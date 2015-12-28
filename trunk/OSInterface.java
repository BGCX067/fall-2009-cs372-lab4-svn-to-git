/**
 * class OSInterface
 * 
 * An abstract interface to the OS.
 * 
 * @author Jungwoo Ha, Emmett Witchel, Pankaj Adhikari
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */

public interface OSInterface {
   abstract void interrupt(int interrupt_type, OSEvent ev);
   abstract int syscall(int syscall_number, OSEvent ev);
   /** return the currently running pid.  Only used by ProcessTrace */
   abstract int running();
   /** Called when the process trace routine is shutting down */
   abstract void shutdown();
   /** Called by VFS to block the current process **/
   abstract void block(int pid);
   /** Called by VFS to unblock a process when its I/O is completed **/
   abstract void unblock(int pid);
   /** Called by VFS to obtain the PCB of a particular process **/
   abstract PCB getPCB(Integer Pid);
}
