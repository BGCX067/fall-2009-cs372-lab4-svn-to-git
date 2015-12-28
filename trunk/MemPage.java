/**
 * class MemPage
 * 
 * A class to represent a page in memory.
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */

public class MemPage {
   static final int BYTES_PER_PAGE = 4096;
   MemPage() {
      data = new byte[BYTES_PER_PAGE];
   }

   void read(byte[] buf, int offset, int length) throws Exception {
      try {
         System.arraycopy(data, offset, buf, 0, length);
      }
      catch(Exception e) {
         throw new Exception("MemPage read error\n"
                             + "buf=" + buf
                             + " buf.length=" + buf.length
                             + " offset=" + offset
                             + " length=" + length
                             + " offset+length=" + (offset + length)
            );
      }
   }
   
   void write(byte[] buf, int offset, int length) throws Exception {
      try {
         System.arraycopy(buf, 0, data, offset, length);
      }
      catch(Exception e) {
         throw new Exception("MemPage write error\n"
                             + "buf=" + buf
                             + " buf.length=" + buf.length
                             + " offset=" + offset
                             + " length=" + length
                             + " offset+length=" + (offset + length)
            );
      }
   }

   byte[] get_bytes() {
      return data;
   }

   void set_bytes(byte[] k) {



      //if (k.length != data.length) {

      if (k.length > data.length) {
         System.err.println("set_bytes error!");

         System.exit(-1);
      }
      System.arraycopy(k, 0, data, 0, k.length);
   }

   protected byte data[];
}
