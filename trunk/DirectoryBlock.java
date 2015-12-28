/**
 * class DirectoryBlock
 *
 * In-memory representation of a directory data block
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.nio.ByteBuffer;

public class DirectoryBlock {
   Dentry [] dentries;
	
   public DirectoryBlock() {
      dentries = new Dentry[Disk.BYTES_PER_SECTOR/Dentry.DENTRY_SIZE];
      for (int i = 0; i < dentries.length; i++) {
         dentries[i] = new Dentry();
      }
   }
   
   public DirectoryBlock(byte[] block) {
      dentries = new Dentry[Disk.BYTES_PER_SECTOR/Dentry.DENTRY_SIZE];
      this.setBytes(block);
   }
	
   public void setBytes(byte[] block) {
      for (int i = 0; i < dentries.length; i++) {
         try {
            dentries[i] = new Dentry();
            dentries[i].setBytes(block, i * Dentry.DENTRY_SIZE);
         }
         catch (Exception e) {
            System.err.println("i = " + i +" , index = " + i/Dentry.DENTRY_SIZE);
            e.printStackTrace();
         }
      }
   }
	
   public byte[] getBytes() {
      ByteBuffer buffer = ByteBuffer.allocate(Disk.BYTES_PER_SECTOR);
      for (int i = 0; i < dentries.length; i++) {
         buffer.put(dentries[i].getBytes());
      }			
      return buffer.array();
   }
	
   public boolean findRemove(String fileName) {
      for (int i = 0; i < dentries.length; i++) {
         if (dentries[i].getFileName().equals(fileName)) {
            dentries[i] = new Dentry();
            return true;
         }
      }
      return false;
   }
	
   public boolean addDentry(Dentry dentry) {
      for (int i = 0; i < dentries.length; i++) {
         if (dentries[i].getIno() == 0) {
            dentries[i] = dentry;
            return true;
         }
         else if (dentries[i].getFileName().equals(dentry.getFileName())) {
            return false;
         }
      }
      return false;		
   }
   
   public Dentry lookupByName(String fileName) {
      for (int i = 0; i < dentries.length; i++) {
         if (dentries[i].getFileName().equals(fileName))
            return dentries[i];
      }
      return null;
   }
   
   public boolean isAvailable() {
      for (int i = 0; i < dentries.length; i++) {
         if (dentries[i].getIno() == 0)
            return true;
      }
      return false;
   }

   public Dentry [] getDentries() {
      return dentries;
   }
}
