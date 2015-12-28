/*
 * class RR
 *   A simple round robin scheduler
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 *
 * @author Ronald L. Rockhold, Cara Kenny, Pankaj Adhikari
 */
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class RR implements IScheduler {

	private VFS vfs;
	private List run_q = new ArrayList();
	private Integer current_pid = null;
	private HashMap all_pcb = new HashMap();
	
	public PCB getPCB(Integer pid) {
		return (PCB) all_pcb.get(pid);
	}

	
	public Integer getCurrentPID() {
		return current_pid;
	}
	
	
	public void setVFS(VFS _vfs) {
		vfs = _vfs;
	}
 
	
	public void processStarted(PCB pcb) {
		Integer pid = pcb.getPID();
		all_pcb.put(pid, pcb);
		pcb.setQuantum(5);
		run_q.add(pid);
	}

 
	public void quantumExpired(Integer pid, int ticks_given) {
		Dbg.println("sc", pid.intValue() + " " + ticks_given);
		run_q.add(pid);
	} 

 
	public Integer schedule() {
		if (run_q.size() > 0) {
			Integer pid = (Integer) run_q.remove(0);
			// Uncomment the next line to see the burst history, which
			//  could be a good mechanism for predicting priority and future burst sizes.
			// PCB pcb = (PCB) all_pcb.get(pid);
			// ProcessTrace.info("Dispatching " + pid + " with burst history " + pcb.getBurstHistory());
			current_pid = pid;

			return pid;
		}
		current_pid = null;
		return null;
	}

 
	public void processExited(Integer pid) {
		clear_fd(pid);
		return;
	}

 
	public void processBlocked(Integer pid, int burst_given) {
		Dbg.println("sc", pid.intValue() + " " + burst_given);
		return;
	}

 
	public void unblocked(Integer pid) {
		run_q.add(pid);
	}

 
	public void quantumInterrupted(Integer pid, int ticks_given) {
		Dbg.println("sc", pid.intValue() + " " + ticks_given);
		return;
	}
	
	
	private void clear_fd(Integer pid) {
		   int p = pid.intValue();
		   for (int i=0; i < vfs.openFiles.length; i++) {
			   if (vfs.openFiles[i] != null && vfs.openFiles[i].getPid() == p) {
				   vfs.close(i);
			   }
		   }
	   }
}
