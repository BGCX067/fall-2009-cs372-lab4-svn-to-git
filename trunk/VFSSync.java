/**
 * class VFSSync
 *
 * The virtual file system layer.  This is the high
 * level OS code which manages the file system.
 * This version of the code waits for the disk to complete, hence the Sync
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.nio.ByteBuffer;

class VFSSync extends VFS {

	protected VFSSync(BufferCache _bc, OSInterface _os) {
		super(_bc, _os);
	}



/*----------------Inodes to CacheBuffer--------------------
* Notes: still need to write ialloc()
* 
*/


   /**
    * allocate an inode and return an object Synchronously
    * 
    * @param type type of inode (Inode.FILE or Inode.DIR)
    * @return Inode object
    * null, on error
    */
	protected Inode ialloc(int type) {
		Dbg.println("vfs", "VFS.ialloc(type=" + type + ")");
		Inode inode = new Inode(getSuperBlock());  //do i need an "ino"?
		// Write me
		inode.setType(type);

		return inode;
		//remember to return an error if it is null
	}

   /**
    * read inode from buffer cache
    * 
    * @param ino inode number to read
    * @param ev event object
    * @return Inode object
    *         null, on error
    *         //write me
    */
	private Inode readInode(int ino, OSEvent ev) {
		Inode inode = null;
		MemPage theinode = getBufferCache().read(getSuperBlock().calcBlockNum(ino), ev);
		if(theinode != null)
			inode = new Inode(getSuperBlock(), theinode.get_bytes(), ino);
		return inode;
	}

   /**
    * write inode to buffer cache
    * 
    * @param inode
    * @param ev
    * @return 0, on success
    *        -1, on error
    *        //write me
    */
   private int writeInode(Inode inode, OSEvent ev) {
      Dbg.println("vfs", "VFS.writeInode(ino="+inode.getIno()+")");
      int x = getBufferCache().write(getSuperBlock().calcBlockNum(inode.getIno()), inode.getBytes(), ev);
      if(x != 0) { return -1; }  //the write failed
      return 0;
   }


   /**
    * read block from buffer cache to a memory page
    * 
    * @param block block number
    * @param ev event object
    * @return The result MemPage object 
    * write me
    */
   private MemPage readBlock(int block, OSEvent ev) {
      Dbg.println("vfs", "VFS.readBlock(block="+block+")");
      MemPage page = getBufferCache().read(block, ev);
      return page; 
   }


/*--------------------Syncing Section---------------------
* Notes: still have to fix read()
*
*/



   /**
    * Sync open system call
    *
    * @param fileName fileName to open
    * @param mode open mode
    * @param fd file descriptor to assign
    * @return fd, on success
    *           -1, on error
    *           //write me
    */
   public int open(String fileName, int mode, int fd) {
	   Dbg.println("vfs", "VFS.open(fileName="+fileName+",mode="+mode+") : entered");
	   if(openFiles[fd] != null)
	   {
		   Error.println(Error.BAD_FILE_DESCRIPTOR, fileName);
		   return -1;
	   }
	   DirectoryBlock db = getDirDataBlock();
	   Dentry dentry = db.lookupByName(fileName);
	   if(dentry == null)
	   {
		   Error.println(Error.FILE_NOT_FOUND, fileName);
		   return -1;
	   }
	   int ino = dentry.getIno();
	   Inode inode = new Inode(getSuperBlock(), ino, Inode.FILE);
	   File openme = new File(fd, mode, fileName, inode, getCurrentPid());
	   openFiles[fd] = openme;
	   Dbg.println("vfs", "VFS.open(fileName="+fileName+",mode="+mode+") : leaving (fd="+fd+")");
	   return fd;
   	}

   /** 
    * Sync write system call
    *
    * @param fd file descriptor to write
    * @param size size in byte to write
    * @param data data to be written in the file
    * @return actual bytes written
    * 	//write me
    */
   public int write(int fd, int size, byte data) { p.p("VFS.write(fd, size, data) = "+fd+", "+size+", "+data);
		// get the file and inode, these do not change
 	  	File file = openFiles[fd]; p.p("~~~ the file is "+file.getFileName());
		Inode inode = file.getInode();
		// prepare loop metadata
		Dbg.println("vfs", "VFS.write(fd="+fd+",size="+size+"): entering");
		int bytesWritten = 0;
		int currentDataBlock = file.getPos() / Disk.BYTES_PER_SECTOR; p.p("~~~ current data block is "+currentDataBlock);
		// while not all bytes have been written or inode is full
		while (bytesWritten < size && currentDataBlock < inode.getMaxDataBlocks()){
			// prepare conditional metadata
			int currentInodeBlocksUsed = inode.getDataBlocksUsed(); p.p("~~~ current data blocks used is "+currentInodeBlocksUsed);
			// allocate a inode block if necessary
			if (currentInodeBlocksUsed <= currentDataBlock){
				inode.addDataBlock(balloc()); p.p("~~~ current data blocks used is "+inode.getDataBlocksUsed());
			}
			// set current offset, get page from cache and get the byte array from the page
			int currentOffset = file.getPos() % Disk.BYTES_PER_SECTOR; p.p("~~~ current offset is "+currentOffset);
			int permenantOffset = file.getPos() % Disk.BYTES_PER_SECTOR;				
			MemPage page = bufferCache.read(inode.getDataBlock(currentDataBlock), createEvent(OSEvent.SYSCALL_READ));
			byte[] byteBuffer = page.get_bytes();
			//write data to byteBuffer size times or until block is full
			while(bytesWritten < size && currentOffset < Disk.BYTES_PER_SECTOR){
				byteBuffer[currentOffset] = data;
				bytesWritten++;
				currentOffset++;
			}
			// write byte array back to page and page back to cache
			try{
				page.write(byteBuffer, permenantOffset, (currentOffset - permenantOffset));
				bufferCache.write(inode.getDataBlock(currentDataBlock), page.get_bytes(), createEvent(OSEvent.SYSCALL_WRITE));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			// update file.setPos(int) and block number
			if(currentOffset == Disk.BYTES_PER_SECTOR){
				file.setPos((currentDataBlock + 1) * Disk.BYTES_PER_SECTOR);
				currentDataBlock++;
			}
			else {
				file.setPos(currentDataBlock*Disk.BYTES_PER_SECTOR + currentOffset);
			}
		}
		Dbg.println("vfs", "VFS.write(fd="+fd+",size="+size+"): leaving");

		return bytesWritten;
   }   

   /**
    * Sync read system call
    * 
    * @param fd file descriptor
    * @param size size in byte to read
    * @return actual bytes read
    */
   public int read(int fd, int size) {
	   p.p("VFSSync.read(): beginning");
  		p.p("VFSSync.read(): fd = " +fd + ", size = " +size);

	   	File file = openFiles[fd];
		if (file == null) {
   			//p.p("VFSSynce.read(): ERROR!  bad fd");
			Error.println(Error.BAD_FILE_DESCRIPTOR, "");
			return -1;
		}
		Dbg.println("vfs", "VFS.read(fd="+fd+",size="+size+"): entering");
		//Write me
   		//p.p("VFSSync.read(): filename = " +file.getFileName());
		int bytes_read = 0;
		if(size > 0)
			ReadCheck.write(getCurrentPid(), file.getInode().getBytes());
		Inode inode = file.getInode();
		int currentDataBlock = file.getPos()/Disk.BYTES_PER_SECTOR;
		while(bytes_read < size && currentDataBlock < inode.getMaxDataBlocks())
		{
			int currentOffset = file.getPos() % Disk.BYTES_PER_SECTOR;
			MemPage page = bufferCache.read(inode.getDataBlock(currentDataBlock), createEvent(OSEvent.SYSCALL_READ));
			byte[] byteBuffer = page.get_bytes();
			while(bytes_read < size && currentOffset < Disk.BYTES_PER_SECTOR)
			{
				//byteBuffer[currentOffset];
				bytes_read++;
				currentOffset++;
			}
			if(currentOffset == Disk.BYTES_PER_SECTOR)
			{
				file.setPos((currentDataBlock + 1) * Disk.BYTES_PER_SECTOR);
				currentDataBlock++;
			}
			else
			{
				file.setPos(currentDataBlock * Disk.BYTES_PER_SECTOR + currentOffset);
			}	
		}
		
		Dbg.println("vfs", "VFS.read(fd="+fd+",size="+size+"): leaving");
		p.p("VFSSync.read(): bytes_read = " + bytes_read);
		  p.p("VFSSync.read(): end");

		return bytes_read;

   	}
}
