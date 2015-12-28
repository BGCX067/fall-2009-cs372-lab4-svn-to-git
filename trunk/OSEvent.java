/**
 * class OSEvent
 * 
 * A class to holds callback state.
 * 
 * @author Jungwoo Ha, Emmett Witchel, Pankaj Adhikari 
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.util.HashMap;

public class OSEvent implements Comparable {
   static final int NOT_AN_EVENT   = 0;
   static final int INT_TIMER      = 1;    
   static final int INT_DISK_READ  = 2;
   static final int INT_DISK_WRITE = 3;
   static final int INT_MAX        = 4;

   static final int SYSCALL_OPEN  = 5;
   static final int SYSCALL_CLOSE = 6;
   static final int SYSCALL_READ  = 7;
   static final int SYSCALL_WRITE = 8;
   static final int SYSCALL_SEEK  = 9;
   static final int SYSCALL_CREATE  = 10;
   static final int SYSCALL_PROC_CREATE  = 11;
   static final int SYSCALL_PROC_KILL  = 12;
   static final int SYSCALL_COMPUTE = 13;
   static final int SYSCALL_MAX  = 14;
   static final String[] sys_names = {"NOT_AN_EVENT",
                                      "INT_TIMER",
                                      "INT_DISK_READ",
                                      "INT_DISK_WRITE",
                                      "NOT_AN_EVENT_4",
                                      "sys_open", "sys_close",
                                      "sys_read", "sys_write",
                                      "sys_seek", "sys_create",
                                      "sys_proc_create",
                                      "sys_proc_kill", "sys_compute"};

   static final Integer I_INT_TIMER = new Integer(INT_TIMER);
   static final Integer I_INT_DISK_READ  = new Integer(INT_DISK_READ);
   static final Integer I_INT_DISK_WRITE = new Integer(INT_DISK_WRITE);

   static final Integer I_SYSCALL_OPEN  = new Integer(SYSCALL_OPEN);
   static final Integer I_SYSCALL_CLOSE = new Integer(SYSCALL_CLOSE);
   static final Integer I_SYSCALL_READ  = new Integer(SYSCALL_READ);
   static final Integer I_SYSCALL_WRITE = new Integer(SYSCALL_WRITE);
   static final Integer I_SYSCALL_SEEK  = new Integer(SYSCALL_SEEK);
   static final Integer I_SYSCALL_CREATE  = new Integer(SYSCALL_CREATE);
   static final Integer I_SYSCALL_PROC_CREATE = new Integer(SYSCALL_PROC_CREATE);
   static final Integer I_SYSCALL_PROC_KILL  = new Integer(SYSCALL_PROC_KILL);
   static final Integer I_SYSCALL_COMPUTE = new Integer(SYSCALL_COMPUTE);

   public OSEvent(OSEvent _ev) {
      this.time = _ev.time;
      this.pid = _ev.pid;
      this.event_number = _ev.event_number;
      this.sched_time = _ev.sched_time;
      this.hm = new HashMap(_ev.hm); // Need a new instance
   }

   // Note, the OSEvent "owns" the HashMap, assuming the user drops
   // its reference after creating the OSEvent. 
   OSEvent(HashMap _hm) {
      // Read time, pid and event number out of hash map
      // and then delete them
      try {
         this.time = OS.system_time();
         if(!_hm.containsKey("pid"))
            this.pid = ProcessTrace.system_pid();
         else {
            this.pid = ((Integer)_hm.get("pid")).intValue();
            _hm.remove("pid");
         }
         this.event_number = ((Integer)_hm.get("event_number")).intValue();
         // The truth for pid and event_number is now cached in the
         // object, not in the hash map.
      } catch(Exception e) {
         System.err.println("Bad OSEvent created without all of time, pid and event_number" + e);
      }
      hm = _hm; // Don't new HashMap(_hm);
   }

   public int compareTo(Object o) {
      if(time < ((OSEvent)o).time) return -1;
      if(time > ((OSEvent)o).time) return 1;
      if(sched_time < ((OSEvent)o).sched_time) return -1;
      if(sched_time > ((OSEvent)o).sched_time) return 1;
      return 0;	
   }

   public boolean equals(Object o) {
      return time == ((OSEvent)o).time;
   }

   public String toString() {
      String s = new String();
      boolean syscall = false;
      if(event_number > NOT_AN_EVENT
         && event_number < SYSCALL_MAX) {
         s += sys_names[event_number];
      } else {
         s += "EVENT_OUT_OF_BOUNDS_" + event_number;
      }

      s += " pid:" + pid + " time:" + time;
      s += " " + hm.toString();

      return s;
   }

   int time() { return this.time; }
   void set_time(int _time) {
      // Check the class of our caller do to access control
      Throwable t = new Throwable();
      StackTraceElement[] ste = t.getStackTrace();
      String cname = ste[1].getClassName();
      if(cname.equals("CallbackManager")
         || cname.equals("Disk")
         || cname.equals("ProcessTrace")
		 || cname.equals("OS")) {
         this.time = _time; 
      } else {
         System.err.println(cname + ", a class other than CallbackManager, ProcessTrace or Disk called OSEvent.set_time.\nThis is not allowed");
         t.printStackTrace();
      }
   }
   void set_sched_time(int _time) {
//    Check the class of our caller do to access control
      Throwable t = new Throwable();
      StackTraceElement[] ste = t.getStackTrace();
      String cname = ste[1].getClassName();
      if(cname.equals("CallbackManager")
         || cname.equals("Disk")
         || cname.equals("ProcessTrace")) {
         this.sched_time = _time; 
      } else {
         System.err.println(cname + ", a class other than CallbackManager, ProcessTrace or Disk called OSEvent.set_sched_time.\nThis is not allowed");
         t.printStackTrace();
      }   
   }
   int pid() { return pid; }
   Object get(String s) { return hm.get(s); }
   int getInt(String s) { 
      try {
         return ((Integer)hm.get(s)).intValue(); 
      }
      catch (NullPointerException e) {
         return -1;
      }
   }
   
   boolean getBoolean(String s) { 
      try {
         return ((Boolean)hm.get(s)).booleanValue(); 
      }
      catch (NullPointerException e) {
         return false;
      }
   }

   void put(Object o0, Object o1) { 
	    //p.p("--- OSEvent.put(Object o0, Object 01): o0 = " +o0+ " and o1 = " +o1);
	   hm.put(o0, o1); 
	   // p.p("--- OSEvent.put(): method DONE!!!");   
   }
   void putInt(Object o, int i) { 

		hm.put(o, new Integer(i)); }
   void putBoolean(Object o, boolean b) { hm.put(o, new Boolean(b)); }
   
   int getState() { return getInt("state"); }
   void setState(int s) { putInt("state", s); } 
   void setNextState() { setState(getState()+1); }

   void happen(OSInterface os) {

      boolean bad = true;
      if(event_number > NOT_AN_EVENT) {
         if(event_number < INT_MAX) {
            os.interrupt(event_number, this);
            bad = false;
         } else if(event_number < SYSCALL_MAX) {
            os.syscall(event_number, this);
            bad = false;
         }
      }
      if(bad) {
         System.err.println("Corrupt trace, we have an event that is neither an interrupt nor a system call " + event_number);
      }
   }

   private int sched_time;
   private int time;
   private int event_number;
   private int pid;
   private HashMap hm;
}
