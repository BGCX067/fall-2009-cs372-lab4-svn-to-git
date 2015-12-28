/**
 * class Dbg
 * 
 * Debugging help.  Debug statements have a string
 * key so you can enable and disable them easily
 * By modifying OS.OS
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.util.HashSet;

public class Dbg {
	
   static HashSet labels = new HashSet();
	
   // Lables include "bd", "bc", "pt", "os", "vfs", "sc"
   // This is called from OS.OS
   public static void addLabel(String label) {
      if(labels == null) labels = new HashSet();
      labels.add(label);
   }
	
   
   private static String toHexString(int i, int radix) {
      if (i < 0) i = i + (1 << (radix*8));
      String str = Integer.toHexString(i);
      if (str.length() > radix)
         str = str.substring(str.length()-radix);
      while (str.length() < radix)
         str = "0" + str;
      return str;
   }
	
   public static void printBytes(String label, byte[] buffer) {
      printBytes(label, buffer, 0, buffer.length);
   }
	
   public static void printBytes(String label, byte[] buffer, int offset, int size) {
      if(!labels.contains(label)) return;
      String line = "";
      size = size + offset % 32;
      offset = offset - offset % 32;
      size = size + ((32 - size % 32) % 32);
		
      for (int i = offset; i < offset+size; i+= 32) {
         line = toHexString(i, 4).toUpperCase();
         for (int j = i; j < i+32; j++) {				
            line = line + " " + toHexString((int)buffer[j], 2);
         }
         line = line + "  ";
         System.out.println(line);
      }
   }
	
   public static void println(String label, String msg) {
      if (labels.contains(label)) {
         System.out.println(msg);
      }
   }
   
   public static void ASSERT(boolean bool) {
      if (!bool) {
         (new Exception()).printStackTrace();
         System.exit(-1);
      }
   }
}
