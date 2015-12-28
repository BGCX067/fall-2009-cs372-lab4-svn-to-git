/**
 * class BitMapBlock
 * 
 * A disk block that holds a bitmap.  Useful for tracking
 * free blocks or free inodes.
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.util.BitSet;

public class BitMapBlock {
   private BitSet bits = null;

   private static final int byteSize = 8;
   private static final int nBits = Disk.BYTES_PER_SECTOR * byteSize;
	
   public BitMapBlock() {
      bits = new BitSet(nBits);
   }
	
   public BitMapBlock(byte[] buf) {
      bits = new BitSet(nBits);
      this.setBytes(buf);
   }

   public void clear() {
      bits.clear();
   }

   public boolean get(int index) {
      return bits.get(index);
   }

   public void set(int index) {
      bits.set(index);
   }

   public void unset(int index) {
      bits.set(index, false);
   }

   public void set(int index, boolean value) {
      bits.set(index, value);
   }

   public int allocate() {
      try {
         int i = bits.nextClearBit(0);
         if (i >= nBits) return -1;
         bits.set(i);
         return i;
      }
      catch (IndexOutOfBoundsException e) {
         return -1;
      }
   }

   public int numFree() {
      int free = 0;
      for (int i = 0; i < bits.size(); i++) {
         if (!bits.get(i))
            free++;
      }
      return free;
   }

   public int numUsed() {
      return bits.size() - numFree();
   }

   public void setBytes(byte[] k) {
      for (int i = 0; i < k.length; i++) {
         for (int j = 0; j < byteSize; j++) {
            if ((k[i] & (1 << byteSize-j-1)) != 0)
               bits.set(i * byteSize + j, true);
            else
               bits.set(i * byteSize + j, false);
         }
      }
   }

   public byte[] getBytes() {
      byte[] buffer = new byte[nBits/byteSize];
      for (int i = 0; i < nBits/byteSize; i++) {
         byte result = 0;
         for (int j = 0; j < byteSize; j++) {
            byte bit = 0;
            if (bits.get(i * byteSize + j)) bit = 1;
            result = (byte)((result << 1) | bit);
         }
         buffer[i] = result; 
      }
      return buffer;
   }
	
   public String toString() {
      return bits.toString();
   }
}
