/**
 * class OS
 * 
 * An overall manager class that contains instances
 * of major system components that do most of the work
 * 
 * @author Jungwoo Ha, Emmett Witchel, Pankaj Adhikari
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */

import java.text.NumberFormat;

public class OS implements OSInterface {

   public static final int INIT_PID = 0;
	
   public OS(MemPage[] _pages, IScheduler _sched) {

      bd = new BlockDriver();
      da = new DiskAddress(0);
      sched = _sched;
      boolean sync = ProcessTrace.getEnv("sync_disks").equals("true");
      bc = BufferCache.BufferCacheFactory(sync, bd, _pages, sched);
      vfs = VFS.VFSFactory(sync, bc, this);

      sched.setVFS(vfs);
      first_booting = true;
      // bd - block driver
      // bdB - block driver binary data output
      // bc - buffer cache
      // bcB - buffer cache binary data output
      // pt - process trace
      // os - OS
      // sc - scheduler
      // Dbg.addLabel("bd");
      // Dbg.addLabel("bdB");
      // Dbg.addLabel("bc");
      // Dbg.addLabel("bcB");
      // Dbg.addLabel("pt");
      // Dbg.addLabel("os");
      // Dbg.addLabel("vfs");
      // Dbg.addLabel("sc");
		

      Dbg.println("os", "Operating System Initialized, booting");
   }
	
   void add_disk(Disk d) throws BlockDriver.MultipleDisks {

      bd.init_disk(d);
   }
   public void startup() {
      // Start and schedule the INIT process

      PCB newPCB = new PCB(new Integer(INIT_PID));
      newPCB.started(INIT_PID);
      sched.processStarted(newPCB);
      sched.schedule();
      vfs.startup();
      return;
   }
   // While any of the events we scheduled during startup remain, then
   // we are still booting. 
   public boolean booting() {
      if(CallbackManager.isEmpty() && first_booting) {
         Dbg.println("os", "Operating System boot complete");
         first_booting = false;
      }
      return first_booting;
   }
   public void shutdown() {
      vfs.shutdown();
   }
   public int running() {
      Integer curr_pid = sched.getCurrentPID();
      if (curr_pid != null) 
         return curr_pid.intValue();
      else return -1;	// XXX Useful for the end where there are only interrupts left
   }
   public void block(int pid) {
      getPCB(new Integer(pid)).block(); // Just sets the flag so that the process can be blocked by run() below
   }
   public void unblock(int pid) {
      sched.unblocked(new Integer(pid)); // Unblocks the process
   }
   public PCB getPCB(Integer Pid) {
      return sched.getPCB(Pid);
   }
   static int system_time() {
      Throwable t = new Throwable();
      StackTraceElement[] ste = t.getStackTrace();
      String cname = ste[1].getClassName();
      if(cname.equals("OSEvent")|| cname.equals("OS")) {
         return now;
      } else {
         System.err.println(cname + ", a class other than OSEvent or OS called ProcessTrace.system_time.\nThis is not allowed");
         t.printStackTrace();
      }
      return 0;
   }
   static int system_latency(int latency) {
      Throwable t = new Throwable();
      StackTraceElement[] ste = t.getStackTrace();
      String cname = ste[1].getClassName();
      if(cname.equals("Disk")) {
         now += latency;
      } else {
         System.err.println(cname + ", a class other than Disk called ProcessTrace.system_latency.\nThis is not allowed");
         t.printStackTrace();
      }
      return 0;
   }

   void run() {

      try {
         // Add disks to OS
         add_disk(new Disk(ProcessTrace.getEnv("disk_file"), 
                           new Boolean(ProcessTrace.getEnv("sync_disks")).booleanValue(), (float)0.0));
         startup();
         boolean initdone = false;
         now++; // One tick gone for startup
         do{
            if (!initdone)	{ // The first time , let INIT be the process 
               curr_pid = new Integer(INIT_PID);
               if (!booting()) initdone = true;
            }
            else 
               curr_pid = sched.schedule();
            curr_pcb = sched.getPCB(curr_pid);
            OSEvent ev;
            must_do_schedule = false;
            if (curr_pid == null){
               if(!CallbackManager.isEmpty()){
                  if (now<CallbackManager.first().time()) {
                     now = CallbackManager.first().time();
                     Dbg.println("os", "All the processes are blocked. Forwarding time to " + now + ", the time for next interrupt");
                  }
               }
               else if (!ProcessTrace.getPending().isEmpty()) {
                  throw (new IllegalStateException("At least one process is running and has pending events, but no process was scheduled."));
               }
            }
            else {
               current_burst = 0;
               curr_pcb.incrDispatches();
               quantum = curr_pcb.getQuantum();
               Dbg.println("os", "Scheduling process " + curr_pid.intValue()
                           + " at time " + now + " with quantum " + quantum);
               must_do_schedule = false;
               now++;	// One tick goes for scheduling
            }
				
            while(!must_do_schedule) {
               // Check if any events for *any* pid
               // must be done now
               int oldtime = now;
               if(CallbackManager.isEmpty() == false) {
                  ev = CallbackManager.first();
                  while(ev != null && ev.time() <= now) {
                     Dbg.println("pt", "PT.CB  " + ev);
                     CallbackManager.set_now(now);
                     CallbackManager.first().happen(this); // Interrupts are assumed not to consume ticks
                     CallbackManager.remove(ev);
                     ev = null;
                     if (curr_pid == null)
                        must_do_schedule = true;
                     if(CallbackManager.isEmpty() == false)
                        ev = CallbackManager.first();
                  }
                  if (must_do_schedule) break;
               }
               // Now handle the trace event for this pid:
               // If the trace is exhausted
               oldtime = now;
               if (ProcessTrace.getPending().isEmpty() && !CallbackManager.isEmpty()) { 
                  if (now<CallbackManager.first().time()) {
                     now = CallbackManager.first().time();
                     Dbg.println("os", "All the processes are blocked. Forwarding time to " + now + ", the time for next interrupt");
                  }
               }
               // else if the current process has valid ticks from last compute
               else if (curr_pcb.getRemainingCPUBurst()>0) {
                  current_burst++;
                  curr_pcb.incrCPUTime(1);
                  if (current_burst == quantum) {
                     // If preemption was also requested at this tick due to an interrupt unblocking
		     // a process, then let this process expire, rather than preempt it
		     if (preempt_requested)
			     preempt_requested = false;
                     Dbg.println("os", "Quantum Expired for process " + 
                                 curr_pid + " at time " + now  + " (" + current_burst + " ticks in this dispatch)");
                     sched.quantumExpired(curr_pid, current_burst);
                     must_do_schedule = true;
                  }
               }
               // else do the next trace event for this process
               else {
                  ev = ProcessTrace.read_next_event(this);
                  // if there is any more event for this process
                  if(ev != null) {
                     if (!initdone)
                        initdone = true;
                     curr_pcb.setCurrentBurst(ev.getInt("ticks"));
                     ev.set_time(ev.getInt("ticks"));
                     Dbg.println("pt", "PT.ER  " + ev);
                     CallbackManager.set_now(now);
                     current_burst++;
                     curr_pcb.incrCPUTime(1);
                     ev.happen(this);
                     now = CallbackManager.get_now();
                     // If the directive was blocking
                     if (curr_pcb.isBlocked()) {
                        if (preempt_requested)
				preempt_requested = false;
                        sched.processBlocked(curr_pid, current_burst);
                        curr_pcb.setCurrentBurst(0);
                        curr_pcb.unblock();	// The process is still blocked, this just clears the flag in PCB
                        Dbg.println("os", "Process " + curr_pid.intValue() + " blocked at time " + (now - 1) + " (" + current_burst + " ticks in this dispatch)");
                        must_do_schedule = true;
                     }
                     // or the directive killed the process
                     else if (curr_pcb.marked_for_death == true) {
                        if (preempt_requested)
				preempt_requested = false;
                        curr_pcb.setCurrentBurst(0);
                        curr_pcb.completed(now);
                        must_do_schedule = true;
                     }
                     // or the directive was non-blocking
                     else { 
                        if (current_burst == quantum) {
                           if (preempt_requested)
				   preempt_requested = false;
                           Dbg.println("os", "Quantum Expired for process " + 
                                       curr_pid + " at time " + now + " (" + current_burst + " ticks in this dispatch)");
                           sched.quantumExpired(curr_pid, current_burst);
                           must_do_schedule = true;
                        }
                     }
                  }
                  // otherwise this process's quantum can be expired?
                  else if (!preempt_requested && !booting()){ // Won't happen, if each process is killed finally
                     Dbg.println("os", "Quantum Expired for process " + 
                                 curr_pid + ", no more trace events left for this");
                     sched.quantumExpired(curr_pid, current_burst);
                     must_do_schedule = true;	//No more trace events for this pid
                  }
               }
               if (preempt_requested && !booting()) {
                  Dbg.println("os", "Preemption of process " + curr_pid + " requested at time " + now + " (" + current_burst + " ticks in this dispatch)");
                  sched.quantumInterrupted(curr_pid, current_burst);
                  must_do_schedule = true;
                  preempt_requested = false;
               }
               if (oldtime == now) now++;
            }
            // Shutdown when trace is finished and we have completed
            // all events.  shutdown() can create more events.
            if(CallbackManager.isEmpty()
               && ProcessTrace.getPending().isEmpty()
               && os_shutdown == false) {
               shutdown();
               os_shutdown = true;
            }
         } while(!CallbackManager.isEmpty()
                 || !ProcessTrace.getPending().isEmpty()
                 || !os_shutdown);
      }
      catch (Exception e){
         System.err.println("OS exception catcher " +
                            e.getMessage());
         e.printStackTrace();
      }
      System.out.println("\n  STATISTICS");
      NumberFormat f = NumberFormat.getInstance();
      f.setMaximumFractionDigits(2);
		
      System.out.println("Total Time: " + now);
      System.out.println("Buffer cache hit rate: " +
                         f.format(100.0 * Stat.getInt("cache_hit") /
                                  (Stat.getInt("cache_write") +
                                   Stat.getInt("cache_read"))) + "%");
      System.out.println("Disk sectors travelled: " +
                         Stat.getInt("disk_sectors_travelled"));
      System.out.println("Number of disk (reads)(writes): ("
                         + Stat.getInt("num_disk_reads")
                         + ")(" 
                         + Stat.getInt("num_disk_writes")
                         + ")");
      System.out.println("Number of page replacement: " + Stat.getInt("cache_page_replaced"));
   }

   ////////////////////////////////////////////////////////////
   // Two main entry points, interrupts and syscalls
   public void interrupt(int interrupt_type, OSEvent ev) {

      Disk d = (Disk)ev.get("Disk");
      DiskAddress sa = (DiskAddress)ev.get("DiskAddress");
      MemPage iop = (MemPage)ev.get("MemPage");
      int syscall_number = ev.getInt("syscall_number");
      try {
         switch (interrupt_type) {
         case OSEvent.INT_DISK_READ :

			if (ev.getInt("NoDiskRead") != 1) 
               d.data_read(sa, iop);
			vfs.pageComplete(sa.sector_number());
			break;
         case OSEvent.INT_DISK_WRITE :

            d.data_write(sa, iop);
			break;
         default :
         }
         ev.put("state", ev.get("next_state"));
			
         if (ev.get("state") == null) return;
			
         switch (syscall_number) {
         case OSEvent.SYSCALL_OPEN:
            vfs.openCallback(ev);
			break;
         case OSEvent.SYSCALL_WRITE:
            vfs.writeCallback(ev);
			break;
         case OSEvent.SYSCALL_READ:
            vfs.readCallback(ev);
			break;
         case OSEvent.SYSCALL_CLOSE:
            break;
         case OSEvent.SYSCALL_SEEK:
            break;
         case OSEvent.NOT_AN_EVENT:
            switch (ev.getInt("state")) {
            case VFS.ST_INODE_WRITE:
               vfs.writeInodeCallback(ev);
               break;
            case VFS.ST_LAST_INITIALIZE:
               Dbg.println("vfs", "BufferCache is initialized! Activating the scheduler.");
               break;
            }
			break;
         default:
         }
      }         
      catch (Disk.BadDisk bad) {
         System.err.println(bad.toString());
         bad.printStackTrace();
         System.err.println("Exception on disk interrupt indicates something has gone seriously wrong");
      }
   }
	
   public int syscall(int syscall_number, OSEvent ev) {
      try {
         switch (syscall_number) {
         case OSEvent.SYSCALL_CREATE:
            vfs.create((String)ev.get("file_name"));
			break;
         case OSEvent.SYSCALL_OPEN:
            vfs.open((String)ev.get("file_name"), ev.getInt("mode"), 
                     ev.getInt("fd"));
			break;
         case OSEvent.SYSCALL_CLOSE :
            vfs.close(ev.getInt("fd"));
			break;
         case OSEvent.SYSCALL_READ :
            vfs.read(ev.getInt("fd"), ev.getInt("length"));
			break;
         case OSEvent.SYSCALL_WRITE :
            vfs.write(ev.getInt("fd"), ev.getInt("length"), (byte)ev.getInt("data"));
			break;
         case OSEvent.SYSCALL_SEEK :
            vfs.seek(ev.getInt("fd"), ev.getInt("whence"), ev.getInt("offset"));
			break;
         case OSEvent.SYSCALL_PROC_CREATE:
				p.p("CREATE PROCESS - "+ev.get("new_pid"));
            PCB pcb = new PCB((Integer)ev.get("new_pid"));
				sched.processStarted(pcb);
				pcb.started(system_time());
			break;
         case OSEvent.SYSCALL_PROC_KILL:
				p.p("KILL PROCESS - "+ev.get("kill_pid"));
            sched.getPCB(sched.getCurrentPID()).marked_for_death = true;
			sched.processExited((Integer)ev.get("kill_pid"));
			break;
         case OSEvent.SYSCALL_COMPUTE:
            sched.getPCB(sched.getCurrentPID()).setAppData((byte)ev.getInt("data"));
            break;
         default :
            System.err.println("Syscall with bad number " + syscall_number);
         }
			
      }
      catch (Exception e) {
         System.err.println("OS.syscall - " + e.getMessage());
         e.printStackTrace();
      }
      return 0;
   }
	
   static void setPreemptRequest() {
      preempt_requested = true;
   }
	
   static private int now;
   private BlockDriver bd;
   private DiskAddress da;
   private VFS vfs;
   private IScheduler sched;
   private BufferCache bc;
   // Just a flag so we know when to print that we are done booting.
   private boolean first_booting;
   private boolean os_shutdown = false;
   // Process data
   private PCB curr_pcb;
   private Integer curr_pid;
   private boolean must_do_schedule;
   static private boolean preempt_requested = false;
   private int current_burst;
   private int burst;
   private int quantum;
}
