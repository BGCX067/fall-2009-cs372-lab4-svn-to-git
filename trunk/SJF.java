import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.lang.Math;

public abstract class SJF implements IScheduler {
	
	private List ready_q = new ArrayList();
	private Integer current_pid = null;
	private HashMap all_pcb = new HashMap();
	private static double first_quantum = 5.0;
	private VFS vfs;
	
    class History {
	    private double expected_burst;
        private int ticks_used;
        History(double next_expected_burst, int ticks_used_in_current_burst) {
	        expected_burst = next_expected_burst;
	        ticks_used = ticks_used_in_current_burst;
	    }
        
        public void recalc(Integer pid, int ticks_used_in_current_burst){
        	ticks_used = ticks_used_in_current_burst;
        	//Stat.printMessage("recalc", pid, Integer.valueOf(ticks_used), Double.valueOf(expected_burst));
        	Stat.print();
            expected_burst = ((.4 * expected_burst) + (.6 * ticks_used));
            //Stat.printMessage("expected", Double.valueOf(expected_burst));
            Stat.print();
        }
        
        public void incrementTicksUsed(int n_ticks){
        	ticks_used += n_ticks;
        }
        
        public double getNextBurst(){
        	return expected_burst;
        }
        
        public int getNextQuantum(){
        	return (int)Math.ceil(expected_burst);
        }
        
        public int getTicksUsed(){
        	return ticks_used;
        }
        
        public void resetTicksUsed(){
        	ticks_used = 0;
        }
	}
    
    
	public void setVFS(VFS _vfs) {
		vfs = _vfs;
	}
	
 
	public PCB getPCB(Integer pid) {
		return (PCB)all_pcb.get(pid);
	}

 
	public Integer getCurrentPID() {
		return current_pid;
	}

 
	public void processStarted(PCB pcb) {
		// add the pcb to the read_q and the all_pcb map
		ready_q.add(pcb.getPID());
		all_pcb.put(pcb.getPID(), pcb);
		// associate process with a history
		pcb.setObject(new History(first_quantum, 0));
	}

 
	public void quantumExpired(Integer pid, int ticks_given) {
		ready_q.remove(pid);
		ready_q.add(pid);
		((History)((PCB)all_pcb.get(pid)).getObject()).incrementTicksUsed(ticks_given);
	}

	
	public Integer schedule() {
		// check ready_q for shortest expected job
		if(ready_q.size() > 0){
			current_pid = (Integer)ready_q.get(0);
			PCB pcb = (PCB)all_pcb.get(current_pid);
			History pcb_history = (History)pcb.getObject();
			double expected_burst = pcb_history.getNextBurst();
			
			for(int i = 1; i < ready_q.size(); i++){
				Integer new_pid = (Integer)ready_q.get(i);
				pcb = (PCB)all_pcb.get(new_pid);
				pcb_history = (History)pcb.getObject();
				double new_expected_burst = pcb_history.getNextBurst();
				if(new_expected_burst < expected_burst){
					expected_burst = new_expected_burst;
					current_pid = new_pid;
				}
			}
			return current_pid;
		}
		return null;
	}

 
	public void processExited(Integer pid) {
		ready_q.remove(pid);
		all_pcb.remove(pid);
	}

 
	public void processBlocked(Integer pid, int burst_given) {
		ready_q.remove(pid);
		PCB pcb = (PCB)all_pcb.get(pid);
		History pcb_history = (History)pcb.getObject();
		pcb_history.incrementTicksUsed(burst_given);
		pcb_history.recalc(pid, pcb_history.getTicksUsed());
		pcb.setQuantum(pcb_history.getNextQuantum());
	}

 
	public void unblocked(Integer pid) {
		PCB pcb = (PCB)all_pcb.get(pid);
		History pcb_history = (History)pcb.getObject();
		ready_q.add(pid);
		pcb_history.resetTicksUsed();
	}

 
	public void quantumInterrupted(Integer pid, int ticks_given) {
		System.out.println("Warning: Preimption has occured in a non-preimptive scheduler!");
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
