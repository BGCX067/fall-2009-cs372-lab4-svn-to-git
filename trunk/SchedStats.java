/*
 * Created on Jan 31, 2004
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 * 
 * @author Ronald L. Rockhold, Cara Kenny, Pankaj Adhikari
 */
public class SchedStats {
	
	public static int cpu_bursts = 0;
	public static int cpu_ticks = 0;
	public static int current_time = 0;
	public static int dispatches = 0;
	public static int duration = 0;
	
	public static void finishedData(
			Integer pid, 
			int started_time, 
			int current_time, 
			int executed_ticks, 
			int execution_burst_count, 
			int context_switches) {
		Dbg.println("sc", "Process " + pid + " completed at time " + current_time + 
    			", used " + executed_ticks + " ticks, and required " +
				context_switches + " context switches for its " + execution_burst_count + 
				" CPU bursts.");
    	duration += current_time -started_time;
    	cpu_bursts += execution_burst_count;
    	dispatches += context_switches;
    	cpu_ticks += executed_ticks;
	}
	
}
