/**
 * class VFS
 *
 * The virtual file system layer.  This is the high
 * level OS code which manages the file system.
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.nio.ByteBuffer;
import java.util.HashMap;

public abstract class VFS {
   /** fixed block location */
   public final static int BLK_SUPERBLOCK = 0;
   public final static int BLK_USEDBLOCKBITMAP = 1; 
   public final static int BLK_INODEBITMAP = 2;
   public final static int BLK_DIRINODE = 3;
   public final static int BLK_DIRDATABLOCK = 4;
	
   /** constants for read */
   public final static int O_RDONLY = 0x00000004;
   public final static int O_WRONLY = 0x00000001;
   public final static int O_RDWR = 0x00000002;
   public final static int O_APPEND = 0x00002000;
	
   /** constant for lseek */
   public final static int SEEK_SET = 0;
   public final static int SEEK_CUR = 1;
   public final static int SEEK_END = 2;
	
   // maximum number of open files
   public final static int MAX_OPEN_FILE = 1024;
	
   // States for async mode
   public final static int ST_OPEN_INIT = 20;
   public final static int ST_OPEN_READINODE = 21;
	
   public final static int ST_READ_READBLOCK = 30;
   public final static int ST_READ_READBLOCK_DONE = 31;
   public final static int ST_WRITE_READBLOCK = 40;
   public final static int ST_WRITE_READBLOCK_DONE = 41;
	
   public final static int ST_INODE_WRITE = 50;
   public final static int ST_INITIALIZE = 60;
   public final static int ST_LAST_INITIALIZE = 61;
	
   protected OSInterface os;
   protected BufferCache bufferCache;
	
   /** 
    * Array of openfiles per each device
    * The index of the openFile is the file descriptor.
    * e.g. openFiles[fd]
    */
   public File[] openFiles;

   static public VFS VFSFactory(boolean sync, BufferCache _bc, OSInterface _os) {
      if(sync) {
         return new VFSSync(_bc, _os);
      } else {
         // We do not distribute the async version
         // return new VFSAsync(_bc, _os);
         return null;
      }
   }
   protected VFS(BufferCache _bc, OSInterface _os) {
      os = _os;
      bufferCache = _bc;
		
      openFiles = new File[MAX_OPEN_FILE];
      for (int i = 0; i < openFiles.length; i++)
         openFiles[i] = null;
		
   }
	
   /*-------------------------------------------------------------------------*
    * Common methods for both sync and async mode
    */
	
   /**
    * read superblock from buffer cache
    * 
    * @return superblock object
    */
   public SuperBlock getSuperBlock() {
      SuperBlock sb = null;
      // Write me
      return sb;
   }

   /**
    * read used block bitmap from buffer cache
    * 
    * @return used block bitmap object
    */
   public BitMapBlock getUsedBlockBitmap() {
      BitMapBlock bmb = null;
      // Write me
      return bmb;
   }

   /**
    * read inode bitmap from buffer cache
    * 
    * @return inode bitmap object
    */
   public BitMapBlock getInodeBitmap() {
      BitMapBlock bmb = null;
      // Write me
      return bmb;
   }

   /**
    * read directory inode from buffer cache
    * 
    * @return Inode object
    */
   public Inode getDirInode() {
      Inode inode = null;
      // Write me
      return inode;
   }

   /**
    * read directory data block from buffer cache
    * 
    * @return DirectoryBlock objecta
    */
   public DirectoryBlock getDirDataBlock(){
      DirectoryBlock db = null;
      // Write me
      return db;
   }

   /**
    * write superblock to buffer cache
    * 
    * @param sb superblock object to write
    */
   private void writeSuperBlock(SuperBlock sb) {
      // Write me
   }

   /**
    * write inode bitmap to buffer cache
    * 
    * @param ib inode bitmap object to write
    */
   protected void writeInodeBitmap(BitMapBlock ib) {
      // Write me
   }
	
   /**
    * write used block bitmap to buffer cache
    * 
    * @param ubb used block bitmap object to write
    */
   private void writeUsedBlockBitmap(BitMapBlock ubb) {
      // Write me
   }
	
   /**
    * write inode which type is Inode.DIR to buffer cache
    * (there is only one directory inode)
    * 
    * @param dirInode inode objet to write
    */
   private void writeDirInode(Inode dirInode) {
      // Write me
   }
	
   /**
    * write directory data block to buffer cache
    * 
    * @param db directory block object
    */
   private void writeDirDataBlock(DirectoryBlock db) {
      // Write me
   }

   /**
    * Allocate a disk block and return the block number
    * @return block number that has been allocated,
    *         -1, on error
    */
   protected int balloc(){
      Dbg.println("vfs", "VFS.balloc()");
      // Write me
      return 0;
   }

   /**
    * add directory entry with given file name and file inode
    * 
    * @param fileName file name
    * @param fileInode file inode of the file name
    * @return true, on success
    *         false, on error
    */
   private boolean addDentry(String fileName, Inode fileInode) {
      // Write me
      return false;
   }

   /**
    * Resolve the inode number from a given file name
    *  
    * @param fileName file name to resolve
    * @return inode number of the fileName
    *         -1, if file not found
    */
   protected int namei(String fileName) {
      // Write me
      return 0;
   }

   /*-------------------------------------------------------------------------*
    * methods which need different implementation for sync / async mode
    */

   /**
    * A wrapper to allocate an inode and return an object
    * 
    * @param type type of inode (Inode.FILE or Inode.DIR)
    * @return Inode object
    *         null, on error
    */    
   abstract protected Inode ialloc(int type);


   /**
    * write a block to buffer cache
    * 
    * @param block block number
    * @param buf data of the block
    */
   protected void writeBlock(int block, byte[] buf) {
      OSEvent ev = createWritebackEvent();
      bufferCache.write(block, buf, ev);
   }

   /*-------------------------------------------------------------------------*
    *  SYSCALL implementation 
    *-------------------------------------------------------------------------*/

   /**
    * create system call
    * 
    * @param fileName file name to create
    * @return 0, on success
    *        -1, on error
    */
   public int create(String fileName) {
      Dbg.println("vfs", "VFS.create(fileName=" + fileName + ")");
      if (fileName.getBytes().length > Dentry.MAX_FILENAME) {
         Error.println(Error.ENAMETOOLONG, fileName);
         return -1;
      }
      DirectoryBlock db = getDirDataBlock();
      if (!db.isAvailable()) {
         Error.println(Error.DENTRY_FULL, fileName);
         return -1;
      }
      Dentry dentry = db.lookupByName(fileName);
      if (dentry != null) {
         Error.println(Error.FILE_ALREADY_EXISTS, fileName);
         return -1;
      }
      Inode inode = ialloc(Inode.FILE);
      if (inode == null) {
         Error.println(Error.INODE_FULL, fileName);
         return -1;
      }      
      if (addDentry(fileName, inode)) {
         return 0;
      }
      Dbg.ASSERT(false);
      return -1;
   }


   /**
    * open system call wrapper
    * 
    * @param fileName fileName to open
    * @param mode open mode
    * @param fd file descriptor to assign
    * @return fd, on success
    *        -1, on error
    */
   public abstract int open(String fileName, int mode, int fd);



   /**
    * close a file with given file descriptor
    * 
    * 1. You must flush all the blocks being used by this file
    * in buffercache
    * 2. You must flush the inode block that has the file's inode
    * 
    * You should stick to the order of flush as above
    * 
    * @param fd file descriptor to close
    * 
    * @return 0, on success
    *        -1, on error
    */
   public int close(int fd) {
      Dbg.println("vfs", "VFS.close(fd=" + fd + "): entering");
      if (openFiles[fd] == null)
         return -1;
      // Write me
      openFiles[fd] = null;
      Dbg.println("vfs", "VFS.close(fd=" + fd + "): leaving");
      return 0;
   }

	
   /**
    * write system call wrapper
    * 
    * @param fd file descriptor to write
    * @param size size in byte to write
    * @param data data value to write
    * @return actual bytes written
    */
   public abstract int write(int fd, int size, byte data);
	
	
	
   /**
    * read system call wrapper
    * 
    * @param fd file descriptor
    * @param size size in byte to read
    * @return actual bytes read
    */
   public abstract int read(int fd, int size);

   //////////////////////////////////////////////////
   // These functions are only used in the Async version, but we
   // define them in the base class because they are publically 
   // accessed by OS, and we don't want to have to cast OS's VFS object 
   // to the more specific VFSAsync.  This is an instance of the 
   // "fat base class" problem.  Illogical functionality makes its way
   // into the base class to provide a simple interface.
   public void openCallback(OSEvent ev)
      { assert false : "openCallback not implemented"; }
   public void writeCallback(OSEvent ev)
      { assert false : "writeCallback not implemented"; }
   public void writeInodeCallback(OSEvent ev)
      { assert false : "writeInodeCallback not implemented"; }
   public void readCallback(OSEvent ev)
      { assert false : "readCallback not implemented"; }
	
   /**
    * seek system call
    * 
    * @param fd file descriptor
    * @param whence SEEK_SET - offset from beginning of a file
    *               SEEK_CUR - offset from current file position
    *               SEEK_END - end of file
    * @param offset number of bytes to seek
    * @return file position after seek
    */   
   public int seek(int fd, int whence, int offset) {
      File file = openFiles[fd];
      if (file == null) {
         Error.println(Error.BAD_FILE_DESCRIPTOR, "");
         return -1;
      } else if (file.getPid() != getCurrentPid()) {
         Error.println(Error.BAD_FILE_DESCRIPTOR, file.getFileName());
         return -1;
      }
      Inode inode = file.getInode();
      int curPos = file.getPos();
      switch (whence) {
      case VFS.SEEK_CUR :
         if (curPos + offset > inode.getSize())
            file.setPos(inode.getSize());
         else
            file.setPos(curPos + offset);
         break;
      case VFS.SEEK_END :
         if (inode.getSize() - offset < 0)
            file.setPos(0);
         else
            file.setPos(inode.getSize() - offset);
         break;
      case VFS.SEEK_SET :
         if (offset < 0)
            file.setPos(0);
         else if (offset > inode.getSize())
            file.setPos(inode.getSize());
         else
            file.setPos(offset);
         break;
      }
      return file.getPos();
   }
	
	
   /*------------------------------------------------------------------------*
    * Getter, Setter and misc methods
    *------------------------------------------------------------------------*/
	
   /**
    * Read in the first 5 blocks (superblock, used block bitmap, inode bitmap,
    * inode block and directory data block) from each device.
    */
   public void startup() {
      for (int j = 0; j < 5; j++) {
         OSEvent ev = createEvent(OSEvent.NOT_AN_EVENT);
         if (j != 4)
            ev.putInt("next_state", ST_INITIALIZE);
         else
            ev.putInt("next_state", ST_LAST_INITIALIZE);
         bufferCache.read(j, ev);
         bufferCache.setPin(j, true);
      }
   }
	
   /**
    * Flush the buffer cache to disk. Should be called before the
    * system terminates.
    */
   public void shutdown() {
      // This should be called before the system terminates
      System.out.println("Flushing BufferCache to disk");
      OSEvent ev = createWritebackEvent();
      bufferCache.flushAll(ev);
   }
	
   /**
    * Get the pid of the current process
    * 
    * @return pid
    */
   public int getCurrentPid() {
      return os.running();
   }
	
   /**
    * Create an event for syscall
    * Only disk read event is created
    * 
    * @param syscall_number
    * @return OSevent
    */
   protected OSEvent createEvent(int syscall_number) {
      HashMap map = new HashMap();
      map.put("syscall_number", new Integer(syscall_number));
      map.put("event_number", OSEvent.I_INT_DISK_READ);
      OSEvent ev = new OSEvent(map);
      map = null; // Give up reference explicitly
      return ev;
   }
   /**
    * Create an event for a disk write back, not directly tied to a syscall
    * 
    * @return OSevent
    */
   private OSEvent createWritebackEvent() {
      HashMap map = new HashMap();
      map.put("syscall_number", new Integer(OSEvent.NOT_AN_EVENT));
      map.put("event_number", OSEvent.I_INT_DISK_WRITE);
      OSEvent ev = new OSEvent(map);
      map = null; // Give up reference explicitly
      return ev;
   }
	
   /**
    * @return buffer cache object
    */
   public BufferCache getBufferCache() {
      return bufferCache;
   }
	
   /**
    * In async mode, when I/O is finished, you should check io_pending bit to true
    */
   public void pageComplete(int block) {
      bufferCache.decrIOPending(block);
   }
}
