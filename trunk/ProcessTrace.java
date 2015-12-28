/**
 * class ProcessTrace
 * 
 * Root class to manage reading system call trace.
 * Reads and parses trace file, supports callbacks.
 * 
 * @author Jungwoo Ha, Emmett Witchel, Pankaj Adhikari 
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.zip.*;
import java.util.Enumeration;
import java.io.SequenceInputStream;

public class ProcessTrace {
	
   // Each pid has an TreeSet of pending events.
   static private HashMap pending;
   private boolean trace_exhausted;
   static private Properties props = null;
   static private OS os;
   LinkedList proc_list;
	
   // The trace file
   BufferedReader tfile = null;
   private int trace_index = 0;
	
   ProcessTrace(String fname) {
      init(fname);
   }
		
   //////////////////////////////////////////////////////
   // Environment, properties file
   public static String getEnv(String key) {
      if (props == null) {
         props = new Properties();
         // put default setting here
         props.setProperty("trace_file", "simple0.tr");
         props.setProperty("sched_class","RR");
         props.setProperty("disk_file", "test_disk");
         props.setProperty("datablocks_per_inode", "8");
         props.setProperty("inodes_per_block", "8");
         props.setProperty("inode_block_ratio", "8");
         props.setProperty("sync_disks", "true");
         // Percentage of disk to use as memory
         props.setProperty("mem_percentage", "100");
         props.setProperty("read_check", "rc");
         try {
            // Let user override defaults
            props.load(new FileInputStream("system.properties"));
         }
         catch (Exception e) {
            Dbg.println("pt", "Using default properties");
         }
      }
      return props.getProperty(key);
   }
	
   static public HashMap getPending() {
      return pending;
   }
	
   static int system_pid() {
      Throwable t = new Throwable();
      StackTraceElement[] ste = t.getStackTrace();
      String cname = ste[1].getClassName();
      if(cname.equals("OSEvent")) {
         return os.running();
      } else {
         System.err.println(cname + ", a class other than OSEvent called ProcessTrace.system_pid.\nThis is not allowed");
         t.printStackTrace();
      }
      return 0;
   }

   public static void main(String[] args) {

      ProcessTrace pt = null;
      MemPage[] pages = null;
      String trace_file_name = null;
      // Initialize props
      getEnv("trace_file");
      if(args.length > 0)
         props.setProperty("trace_file", args[0]);
      props.list(System.out);
      try {
         pt = new ProcessTrace(getEnv("trace_file"));
         File disk_file = new File(getEnv("disk_file"));
         int len = (int)disk_file.length();

         int NPAGES = (new Integer(getEnv("mem_percentage")).intValue()
                       * (len/MemPage.BYTES_PER_PAGE)) / 100;
         if(NPAGES < 6) {
            throw new Exception("mem_percentage is too low, need at least 6 pages of memory\n");
         }
         System.out.println("Number of memory pages: " + NPAGES);
         pages = new MemPage[NPAGES];
         int i;
         for(i = 0; i < NPAGES; ++i) {
            pages[i] = new MemPage();
         }
      }
      catch(Exception e) {
         System.err.println("Error in ProcessTrace initialization");
         e.printStackTrace();
      }
		
      String fname = getEnv("read_check");
      File rcfile = new File(fname);
      if (rcfile.exists() && rcfile.isFile()) {
         try { rcfile.delete(); }
         catch(Exception e) {
            System.err.println("Error removing Read Check file");
            e.printStackTrace();
         }
      }
		
      try {

         os = new OS(pages, (IScheduler)do_GetClassInstance(props,"sched_class"));
      } 
      catch (Exception e) {
         System.err.println("Scheduler Class not found!");
         e.printStackTrace();
      }
      os.run();
      pt.cat_rc();
   }
	
   private void init(String fname) {

      pending = new HashMap();
      proc_list = new LinkedList();
      trace_exhausted = false;
      init_events(fname);
      while (!trace_exhausted)
         pending_put(readTraceFileLine());
      trace_exhausted = false;
   }
	
   private HashMap parseTrace(String line) {
		HashMap hm = new HashMap();

      // alist[1] : pid
      while (line.startsWith(" ")) line = line.substring(1);
      String s_pid = line.substring(0, line.indexOf(' '));
      line = line.substring(line.indexOf(' ')+1);
      hm.put("pid", new Integer(s_pid));
		
      // alist[2] : syscall num
      String syscall = line.substring(0, line.indexOf('(')).toLowerCase();
      String args = line.substring(line.indexOf('(')+1, line.indexOf(')'));
      try {
         if (syscall.equals("proc_create")) {
            hm.put("event_number", OSEvent.I_SYSCALL_PROC_CREATE);
            hm.put("new_pid", new Integer(args));
            proc_list.add(new Integer(args));
         }
         else if (syscall.equals("compute")) {
            hm.put("event_number", OSEvent.I_SYSCALL_COMPUTE);
            if (args.indexOf(',')>=0) {
               hm.put("data", new Integer(args.substring(0, args.indexOf(',')).trim()));
               args = args.substring(args.indexOf(',')+1);
               while (args.startsWith(" ")) args = args.substring(1);
               if ((new Integer(args)).intValue() < 1) 
                  throw (new Exception("Compute directive must have a positive value for ticks"));
               hm.put("ticks", new Integer(args));
            }
            else {
               throw (new Exception("Compute directive must have valid values for data & ticks"));
            }
         }
         else if (syscall.equals("proc_kill")) {
            hm.put("event_number",OSEvent.I_SYSCALL_PROC_KILL);
            hm.put("kill_pid", new Integer(args));
            if (((Integer)hm.get("pid")).intValue()!=((Integer)hm.get("kill_pid")).intValue())
               throw (new Exception("A process can only kill itself!"));
         }
         else if (syscall.equals("create")) {
            hm.put("event_number", OSEvent.I_SYSCALL_CREATE);
            if (args.startsWith("\"")) args = args.substring(1);
            if (args.endsWith("\"")) args = args.substring(0, args.length()-1);
            hm.put("file_name", new String(args));
         }
         else if (syscall.equals("open")) {
            hm.put("event_number", OSEvent.I_SYSCALL_OPEN);
            line = line.substring(line.indexOf('=')+1).trim();
            while (line.startsWith(" "))
               line = line.substring(1);
            String fileName = args.substring(0, args.indexOf(','));
            if (fileName.startsWith("\"")) fileName = fileName.substring(1);
            args = args.substring(args.indexOf(',')+1) + "|";
            int mode = 0;
            while (args.indexOf('|') > 0) {
               String modeStr = args.substring(0, args.indexOf('|')).trim();
               while (modeStr.startsWith(" ")) modeStr = modeStr.substring(1);
               if (modeStr.equals("O_RDONLY"))
                  mode = mode | VFS.O_RDONLY;
               else if (modeStr.equals("O_WRONLY"))
                  mode = mode | VFS.O_WRONLY;
               else if (modeStr.equals("O_RDWR"))
                  mode = mode | VFS.O_RDWR;
               else if (modeStr.equals("O_APPEND"))
                  mode = mode | VFS.O_APPEND;
               args = args.substring(args.indexOf('|')+1);
            }
            hm.put("file_name", fileName);
            hm.put("mode", new Integer(mode));
            hm.put("fd", new Integer(line)); // New file descriptor
         }
         else if (syscall.equals("close")) {
            hm.put("event_number", OSEvent.I_SYSCALL_CLOSE);
            while (args.startsWith(" ")) args = args.substring(1);
            hm.put("fd", new Integer(args.trim()));
         }
         else if (syscall.equals("write")) {
            hm.put("event_number", OSEvent.I_SYSCALL_WRITE);
            line = line.substring(line.indexOf("=") + 1).trim();
            if (args.indexOf(',')>=0) {
               hm.put("fd", new Integer(args.substring(0, args.indexOf(',')).trim()));
               args = args.substring(args.indexOf(',')+1);
               while (args.startsWith(" ")) args = args.substring(1);
               Integer length = new Integer(args.trim());
               if (length.intValue() < 1) 
                  throw (new Exception("Write directive must have a positive value for length"));
               hm.put("length", length);
               if ((!line.startsWith("0x") && !line.startsWith("0X"))) {
                  throw (new Exception("Write directive must have a valid HEX data starting with 0x"));
               }
               line = line.substring(2);
               hm.put("data", new Integer(Integer.parseInt(line.trim(), 16)));
            }
            else {
               throw (new Exception("Write directive must have valid values for fd & length"));
            }
         }
         else if (syscall.equals("read")) {
            hm.put("event_number", OSEvent.I_SYSCALL_READ);
            if (args.indexOf(',')>=0) {
               hm.put("fd", new Integer(args.substring(0, args.indexOf(',')).trim()));
               args = args.substring(args.indexOf(',')+1);
               while (args.startsWith(" ")) args = args.substring(1);
               if ((new Integer(args.trim())).intValue() < 1) 
                  throw (new Exception("Read directive must have a positive value for length"));
               hm.put("length", new Integer(args.trim()));
            }
            else {
               throw (new Exception("Read directive must have valid values for fd & length"));
            }
         }
         else if (syscall.equals("seek")) {
            hm.put("event_number", OSEvent.I_SYSCALL_SEEK);
            hm.put("fd", new Integer(args.substring(0, args.indexOf(',')).trim()));
            args = args.substring(args.indexOf(',')+1);
            while (args.startsWith(" ")) args = args.substring(1);
            String whence = args.substring(0, args.indexOf(',')).trim();
            if (whence.equals("SEEK_SET"))
               hm.put("whence", new Integer(VFS.SEEK_SET));
            else if (whence.equals("SEEK_CUR"))
               hm.put("whence", new Integer(VFS.SEEK_CUR));
            else if (whence.equals("SEEK_END"))
               hm.put("whence", new Integer(VFS.SEEK_END));
            args = args.substring(args.indexOf(',')+1);
            while (args.startsWith(" ")) args = args.substring(1);
            hm.put("offset", new Integer(args));
         }
         else 
            return null;
         if (!hm.containsKey("ticks")) {
            hm.put("ticks", new Integer(1));
         }
         return hm;
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      return null;
   }
	
   public OSEvent readTraceFileLine() {
      String line = null;
      try {
         while (tfile.ready()) {
            line = tfile.readLine();
            if (line.startsWith("#")) continue;
            HashMap hm = parseTrace(line);
            if (hm == null)
               continue;
            return new OSEvent(hm);
         }
         tfile.close();
         trace_exhausted = true;
      } catch (FileNotFoundException e) {
         System.err.println(e.getMessage());
         e.printStackTrace();
      } catch (IOException e) {
         System.err.println("IOException: " + e.getMessage());
         e.printStackTrace();
      }
      return null;
   }
	
   private void init_events(String fname) {

      OSEvent ev = null;
      if(fname != null && fname.length() > 0) {
         try {
            tfile = new BufferedReader(new FileReader(fname), 512);
         }
         catch(Exception e) {
            System.err.println("Error opening file " + fname);
            e.printStackTrace();
            Dbg.ASSERT(false);
         }
      }
   }
	
	
   static public OSEvent read_next_event(OS os) {
      // Don't start reading trace file until boot process finishes
      if(os.booting()) return null;
      Integer pid = new Integer(os.running());
      OSEvent ev = pending_get(pid);
      return ev;
   }
	
   private void pending_put(OSEvent ev) {
      /* Process Create */
      if (ev == null) return;
      Integer pid = new Integer(ev.pid());
      if(pending.containsKey(pid) == false) {
         //Dbg.println("pt", "pending_put creating set for pid " + pid);
         //Dbg.println("pt", "\tputting - " + ev.toString());
         pending.put(pid, new TreeSet());
      }
      ev.set_time(trace_index++);
      ((TreeSet)pending.get(pid)).add(ev);
   }
	
   static private OSEvent pending_get(Integer pid) {
      TreeSet ts = (TreeSet)pending.get(pid);
      if(ts != null
         && !ts.isEmpty()) {
         OSEvent ev = (OSEvent)ts.first();
         ts.remove(ev);
         if(ts.isEmpty()) {
            /* Kill Process */
            pending.remove(pid);
         }
         return ev;
      }
      return null;
   }
	
   String getName(LinkedList l, int current) {
      return getEnv("read_check" )+"."+((Integer)l.get(current)).toString();
   }
	
   // suppporting methods for setting the scheduler class
   private static Class do_GetClass(Properties props, String name) throws ClassNotFoundException {
      String cname = props.getProperty(name).trim();
      if (cname == null) {
         // log("Property " + name + " not found.  Exiting.");
         throw new IllegalArgumentException("Property " + name + " not found.  Exiting.");
      }
      Class c;
      try {
         c = Class.forName(cname);
      } catch (ClassNotFoundException e) {
         // log("Could not locate class " + cname);
         throw e;
      }
      return c;
   }
	
   private static Object do_GetClassInstance(Properties props, String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
      Class c = do_GetClass(props, name);
      Object o = c.newInstance();
      return o;
   }
	
   // concatenate all the read check files into one big file and remove rc.pid
   void cat_rc() {
      FileOutputStream file = null;
      ListOfFiles rcs;
      int current=0;
		
      File f;
      LinkedList tmp_list = new LinkedList();
		
      // find the rc files that acually exists
      while (current < proc_list.size()) {
         f = new File(getName(proc_list, current));
         if (f.exists())
            tmp_list.add(proc_list.get(current));
         current++;
      }
      proc_list = tmp_list;
		
      // no read check files, exit
      if (proc_list.size() == 0)
         return;
		
		
      rcs = new ListOfFiles(proc_list);
		
      SequenceInputStream s = new SequenceInputStream(rcs);
      int c;
		
      try {
         file = new FileOutputStream(getEnv("read_check"),false);
         while ((c = s.read()) != -1)
            file.write(c);
         file.close();
      } catch (Exception e) {
         System.err.println("Error in cat_rc(): " + e.getMessage());
      }
		
		
      try {
         s.close();
      } catch (Exception e) {
         System.err.println("Error closing file in cat_rc");
      }
		
		
      try {
         for (int i=0; i < proc_list.size(); i++) {
            f = new File(getEnv("read_check")+"."+((Integer)proc_list.get(i)).toString());
				
            f.delete();
         }
      } catch (Exception e) {
         System.err.println("Can't remove read check file");
      }
   }
	
   // supporting class for cat_rc()
   public class ListOfFiles implements Enumeration {
		
      private LinkedList listOfFiles;
      private int current = 0;
		
      public ListOfFiles(LinkedList listf) {
         listOfFiles = listf;
      }
		
      public boolean hasMoreElements() {
         if (current < listOfFiles.size())
            return true;
         else
            return false;
      }
		
		
      public Object nextElement() {
         FileInputStream in = null;
			
         File f = new File(getName(listOfFiles,current));
         while (!f.exists() && ++current<listOfFiles.size()) 
            f = new File(getName(listOfFiles,current));
			
         if (hasMoreElements()){
            String nextElement = getEnv("read_check")+"."+((Integer)listOfFiles.get(current)).toString();
            try {
               in = new FileInputStream(nextElement);
            } catch (FileNotFoundException e) {
               System.err.println("ListOfFiles: Can't open " + nextElement);
            }
         }
         current++;
         return in;
      }
   }
	
}
