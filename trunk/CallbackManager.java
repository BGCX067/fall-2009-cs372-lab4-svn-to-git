/**
 * class CallbackManager
 *
 * This manages callbacks.  It is an example of a singleton
 * class beacuse it only allows a single instance -- all of
 * its methods are static and it has not constructor (it
 * could have a constructor that only allowed a single instance,
 * but this implementation is equivalent to that).
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.util.TreeSet;

public class CallbackManager {

   // Time in these events is specified as ticks in the future.
   static public void schedule_event(OSEvent ev) {
   	  if(events == null) {
         events = new TreeSet();
      }
      ev.set_sched_time(now);
      ev.set_time(ev.time() + now);
      boolean result = events.add(ev);
      // If we schedule multiple events in the same click, have
      // them complete in the order in which they are scheduled.
      now++;
   }
   
   static boolean isEmpty() { 
      if(events == null) {
         events = new TreeSet();
      }
      return events.isEmpty(); 
   }
   static OSEvent first() { return (OSEvent)events.first();}
   static void remove(OSEvent ev) { events.remove(ev); }
   // Let ProcessTrace tell us what time it is
   static void set_now(int _now) { now = _now;}
   static int get_now() {return now;}  
   // Outstanding events
   static private TreeSet events;
   static private int now;
}
