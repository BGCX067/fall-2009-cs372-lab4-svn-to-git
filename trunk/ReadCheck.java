/**
 * class ReadCheck
 * 
 * Writes read check files
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.io.FileOutputStream;
import java.util.HashMap;

public class ReadCheck {
   FileOutputStream file = null;

   // We append to the file to give up-to-date partial results.
   public ReadCheck(int pid) {
      try {
         file = new FileOutputStream(ProcessTrace.getEnv("read_check")+"."+pid ,true);
      }
      catch (Exception e) {
         System.err.println("ReadCheck: can't initialize");
         e.printStackTrace();
      }
   }
   
   public void printBytes(byte[] buffer) {
      try {
         for (int i = 0; i < buffer.length; i++) {
            file.write(String.format(" %02x", buffer[i]).getBytes());
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   private void close() {
      try {
         file.close();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   ////////////////////////////////////////
   // Call this function with the pid of the reading process and the data read
   // You can call it for each block read or for all the data at once.
   public static void write(int pid, byte[] buffer) {
      ReadCheck instance = new ReadCheck(pid);
      instance.printBytes(buffer);
      instance.close();
   }
}
