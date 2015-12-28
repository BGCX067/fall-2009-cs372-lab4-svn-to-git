/**
 * class IScheduler
 *
 * The interface to the scheduler
 * 
 * @author Ron Rockhold, Pankaj Adhikari
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */


public interface IScheduler {
	
   //	 An exception class
   public class SchedErr extends Exception {
      SchedErr() {
         super();
      }
      SchedErr(String s) {
         super(s);
      }
   }  
	
   /**
    * Returns the PCB for the given pid
    * @param pid The process id
    * @return
    */
   PCB getPCB(Integer pid);
	
	
   /**
    * This is called when a new process enters the system
    * and is ready to be scheduled.
    * @param pid  The id of the new process.
    */
   void processStarted(PCB pcb);
	
	
   /**
    * This is called when the process's
    * assigned quantum has expired (i.e. when
    * the process didn't block voluntarily).
    * The process has been "undispatched" and should
    * be added back to the scheduler's ready-queue.
    * The scheduler's schedule() method will be called very soon 
    *   after this call.
    * @param pid The id of the process whose quantum expired
    * @param ticks_given Number of ticks consumed during last dispatch
    */
   void quantumExpired(Integer pid, int ticks_given);
	
   /**
    * Called when a CPU is looking for work.
    * Remember to change the quantum for this process if
    * the default (5) is not appropriate.
    * @return pid of the next process to run, or null if none are ready.
    */
   Integer schedule();
	
   /**
    * Get the pid for the currently running process
    * @return
    */
   Integer getCurrentPID();
	
	
   /**
    * Informational.  This process has left-the-building.
    * @param pid
    */
   void processExited(Integer pid);
	
	
   /**
    * Informational.
    * This process was executing, but has blocked for I/O.
    * The scheudler's schedule() method will soon be called.
    * @param pid
    * @param ticks_given Number of ticks consumed during last dispatch before blocking
    */
   void processBlocked(Integer pid, int burst_given);
	
	
   /**
    * This process is no longer blocked (I/O complete) and should be added
    * to the scheduler's list of ready processes.
    * @param pid
    */
   void unblocked(Integer pid);
	
	
   /**
    * @param pid
    * @param ticks_given Number of ticks consumed during last dispatch
    */
   void quantumInterrupted(Integer pid, int ticks_given);
	
   /**
    * @param _vfs Used by the scheduler to close files of killed processes
    */
   void setVFS(VFS _vfs);
}
