/**
 * class Inode
 * 
 * In-memory representation of the on-disk data structure.
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Inode structure
 */
public class Inode {
   public final static int FILE =	0; // This was 0
   public final static int DIR =	1; // This was 1
	
   public final static int EMPTY = 0; // This was 0
	
   /** number of pointer to the data block */	
   private int maxDataBlocks;
   private int maxFileSize;

   /* Metadatas */

   /** inode number */
   private int ino;			// This info is not stored in the disk
   private int dataBlocksUsed;
   private int type;
   
   /** File length in bytes */
   private int size;
   private SuperBlock sb;

   /** block number that this file is using */
   private int[] dataBlocks;


   /**
    * Initialize Inode with its superblock information
    * 
    * @param sb corresponding superblock
    */
   public Inode(SuperBlock sb) {
      this.dataBlocksUsed = 0;
      this.size = 0;
      this.sb = sb;
      this.type = FILE;
    	
      this.maxDataBlocks = getMaxDataBlock();
      this.maxFileSize = maxDataBlocks * Disk.BYTES_PER_SECTOR;
      this.dataBlocks = new int[this.maxDataBlocks];
      for (int i = 0; i < dataBlocks.length; i++)
         dataBlocks[i] = Inode.EMPTY;
   }
    
   /**
    * Initialize inode with its superblock information and 
    * byte[] representation of a inode
    *  
    * @param sb corresponding superblock
    * @param page inode block representation in byte[]
    * @param ino inode number
    */
   public Inode(SuperBlock sb, byte[] page, int ino) {
      this.sb = sb;
      this.ino = ino;
      this.maxDataBlocks = getMaxDataBlock();
      this.maxFileSize = maxDataBlocks * Disk.BYTES_PER_SECTOR;
      this.dataBlocks = new int[this.maxDataBlocks];
      setBytes(page, ino % sb.getInodesPerBlock());
   }
	
   /**
    * Initialize inode
    * Usually used when creating a new inode
    * 
    * @param sb
    * @param ino
    * @param type
    */
   public Inode(SuperBlock sb, int ino, int type) {
      this.sb = sb;
      this.ino = ino;
      this.maxDataBlocks = getMaxDataBlock();
      this.maxFileSize = maxDataBlocks * Disk.BYTES_PER_SECTOR;
      this.dataBlocks = new int[this.maxDataBlocks];
      for (int i = 0; i < dataBlocks.length; i++)
         this.dataBlocks[i] = Inode.EMPTY;
		
      this.type = type;
   }

   /**
    * get byte[] representation of inode
    * 
    * @return inode in byte[]
    */
   public byte[] getBytes() {
      ByteBuffer byteBuffer = ByteBuffer.allocate(getInodeSize());
      IntBuffer buffer = byteBuffer.asIntBuffer();
      buffer.put(dataBlocksUsed);
      buffer.put(size);
      buffer.put(type);
      buffer.put(dataBlocks);
      return byteBuffer.array(); 
   }
	
   /**
    * set the inode from <code>byte[]</code> of block 
    * 
    * @param block A disk block that has a series of inodes
    * @param order zero-based order of inode in <code>block</code>
    */
   public void setBytes(byte[] block, int order) {
      IntBuffer buffer = 
         ByteBuffer.wrap(block, order * getInodeSize(), getInodeSize()).asIntBuffer();
      this.dataBlocksUsed = buffer.get();
      this.size = buffer.get();
      this.type = buffer.get();
      try {
         buffer.get(this.dataBlocks);
      } catch (BufferUnderflowException e) {
         e.printStackTrace();
      } catch (Exception e) {
         System.err.println("Inode.setBytes(...,"+order+"): " + e.getMessage());
         e.printStackTrace();
      }
   }
	
   public int getDataBlock(int i) {
      return dataBlocks[i];
   }

   public int getDataBlocksUsed() {
      return dataBlocksUsed;
   }

   public int getMaxDataBlocks() {
      return maxDataBlocks;
   }

   public int getMaxFileSize() {
      return maxFileSize;
   }

   public int getSize() {
      return size;
   }

   public void setDataBlock(int i, int value) {
      dataBlocks[i] = value;
   }

   public void setDataBlocksUsed(int i) {
      dataBlocksUsed = i;
   }

   public void setSize(int i) {
      size = i;
   }
	
   public int getInodeSize() {
      return Disk.BYTES_PER_SECTOR / sb.getInodesPerBlock();
   }
	
   private int getMaxDataBlock() {
      return sb.getDataBlocksPerInode();
   }

   public int getIno() {
      return ino;
   }

   public int getType() {
      return type;
   }

   public void setType(int i) {
      type = i;
   }

   public boolean addDataBlock(int block) {
		p.p("(Inode.addDataBlock(block)) adding a data block to the inode");
      for (int i = 0; i < dataBlocks.length; i++) {
         if (dataBlocks[i] == Inode.EMPTY) {
            dataBlocks[i] = block;
            dataBlocksUsed++;
            return true;
         }
      }
      return false;
   }
}
