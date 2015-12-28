/**
 * class SuperBlock
 * 
 * representation of superblock in a filesystem.  It holds
 * the metadata for the entire file system
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class SuperBlock {
	// re-calculate the max dir inode size if any changes made on the
	// superblock fields.
	/**
	 * the number of inodes in one block
	 */
	private int inodesPerBlock;
	
	/**
	 * the ratio of inode block to the data block
	 */
	private int inodeBlockRatio;
	private int dataBlocksPerInode;
	
   /** number of blocks of the file system */
	private int numBlocks;
		
	/**
	 * pointer to the directory inodes
	 */
	private int dirInode[];
	private int dirInodeUsed;

	/**
	 * This constructor is only used for creating a image
	 * The numBlocks property is not present, so you should set it before using it
	 */
	public SuperBlock(int _numBlocks) {
		dirInodeUsed = 0;
      inodesPerBlock = Integer.parseInt(ProcessTrace.getEnv("inodes_per_block"));
      inodeBlockRatio = Integer.parseInt(ProcessTrace.getEnv("inode_block_ratio"));
      dataBlocksPerInode = Integer.parseInt(ProcessTrace.getEnv("datablocks_per_inode"));
		numBlocks = _numBlocks;
		dirInode = new int[getMaxDirInode()];
		for (int i = 0; i < dirInode.length; i++)
			dirInode[i] = -1;
      // We fix this value
      dirInode[0] = 0;
      dirInodeUsed = 1;
	}
	
	public SuperBlock(byte[] buf) {
		this.setBytes(buf);
	}
	
	/**
	 * convert byte array into superblock class
	 * 
	 * @param buf byte array to change
	 * @return true, if successful, otherwise false
	 */
	public boolean setBytes(byte[] buf) {

		try {
			if (buf.length != Disk.BYTES_PER_SECTOR){

				return false;
			}

			ByteBuffer buffer = ByteBuffer.wrap(buf);
			this.inodesPerBlock = buffer.getInt();
			this.inodeBlockRatio = buffer.getInt();
			this.dirInodeUsed = buffer.getInt();
			this.numBlocks = buffer.getInt();
			this.dataBlocksPerInode = buffer.getInt();
			dirInode = new int[getMaxDirInode()];
			buffer.asIntBuffer().get(dirInode);

			return true;
		}
		catch (Exception e) {
			System.err.println("SuperBlock.set error : " + e.getMessage());

			return false;
		}		
	}
	
	/**
	 * convert superblock class into byte array
	 * 
	 * @return byte array that represents this superblock
	 */
	public byte[] getBytes() {
		ByteBuffer buffer = ByteBuffer.allocate(Disk.BYTES_PER_SECTOR);
		buffer.putInt(inodesPerBlock);
		buffer.putInt(inodeBlockRatio);
		buffer.putInt(dirInodeUsed);
		buffer.putInt(numBlocks);
		buffer.putInt(dataBlocksPerInode);
		IntBuffer ibuffer = buffer.asIntBuffer();
		ibuffer.put(dirInode);
		return buffer.array();
	}

	public int getInodeBlockRatio() {
		return inodeBlockRatio;
	}

	public int getInodesPerBlock() {
		return inodesPerBlock;
	}

	public void setInodeBlockRatio(int i) {
		inodeBlockRatio = i;
	}

	public void setInodesPerBlock(int i) {
		inodesPerBlock = i;
	}

	public int[] getDirInodes() {
		return dirInode;
	}
	
	public int getDirInode(int i) {
		return dirInode[i];
	}

	public int getDirInodeUsed() {
		return dirInodeUsed;
	}

	/**
	 * Calculate absolute block number that contains inode
	 */
	public int calcBlockNum(int ino) {
		return 3 + ((getInodeBlockRatio()+1) * (ino / getInodesPerBlock()));
	}
	
	public int addDirInode(int ino) {
		for (int i = 0; i < dirInode.length; i++) {
			if (dirInode[i] == -1) {
				dirInode[i] = ino;
				dirInodeUsed++;
				return 0;
			}
		}
		return -1;
	}
	
	private int getMaxDirInode() {
		return 1;
	}
	
	public int getNumBlocks() {
		return numBlocks;
	}

	public void setNumBlocks(int i) {
		numBlocks = i;
	}

   public int getDataBlocksPerInode() {
      return dataBlocksPerInode;
   }
   
   public void setDataBlocksPerInode(int i) {
      dataBlocksPerInode = i;
   }

   public void p(String s) {
      //System.out.println(s);
   }

}
