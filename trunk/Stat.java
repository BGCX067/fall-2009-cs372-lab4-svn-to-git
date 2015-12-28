/**
 * class Stat
 * 
 * Manage some statistics collection
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;

public class Stat {
   private static HashMap hm = new HashMap();
   
   public static void add(String key, int val) {
      Integer i = (Integer)hm.get(key);
      if (i == null)
         i = new Integer(0);
      hm.put(key, new Integer(i.intValue() + val));
   }

   public static void inc(String key) {
      add(key, 1);
   }
   
   public static int getInt(String key) {
      Integer i = (Integer)hm.get(key);
      if (i == null)
         return 0;
      return i.intValue();
   }
   
   public static void putInt(String key, int v) {
      hm.put(key, new Integer(v));
   }
   
   public static void print(PrintStream out) {
      Iterator i = hm.keySet().iterator();
      for (;i.hasNext();) {
         String key = (String)i.next();
         Object o = hm.get(key);
         out.println(key + ": " + o.toString());
      }
   }
   
   public static void print() {
      print(System.out);      
   }
}
