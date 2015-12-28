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

   /**
    * allocate an inode and return an object Synchronously
    * 
    * @param type type of inode (Inode.FILE or Inode.DIR)
    * @return Inode object
    * null, on error
    */
   protected Inode ialloc(int type) {
      Dbg.println("vfs", "VFS.ialloc(type=" + type + ")");
      Inode inode = null;
      // Write me
      return inode;
   }

   /**
    * read inode from buffer cache
    * 
    * @param ino inode number to read
    * @param ev event object
    * @return Inode object
    *         null, on error
    */
   private Inode readInode(int ino, OSEvent ev) {
      Inode inode = null;
      // Write me
      return inode;
   }

   /**
    * write inode to buffer cache
    * 
    * @param inode
    * @param ev
    * @return 0, on success
    *        -1, on error
    */
   private int writeInode(Inode inode, OSEvent ev) {
      Dbg.println("vfs", "VFS.writeInode(ino="+inode.getIno()+")");
      // Write me
      return 0;
   }


   /**
    * read block from buffer cache to a memory page
    * 
    * @param block block number
    * @param ev event object
    * @return The result MemPage object 
    */
   private MemPage readBlock(int block, OSEvent ev) {
      Dbg.println("vfs", "VFS.readBlock(block="+block+")");
      MemPage page = null;
      // Write me
      return page; 
   }

   /**
    * Sync open system call
    *
    * @param fileName fileName to open
    * @param mode open mode
    * @param fd file descriptor to assign
    * @return fd, on success
    *           -1, on error
    */
   public int open(String fileName, int mode, int fd) {
      Dbg.println("vfs", "VFS.open(fileName="+fileName+",mode="+mode+") : entered");
      // Write me
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
    * 
    */
   public int write(int fd, int size, byte data) {
      File file = openFiles[fd];
      if (file == null) {
         Error.println(Error.BAD_FILE_DESCRIPTOR, "");
         return -1;
      }
      Dbg.println("vfs", "VFS.write(fd="+fd+",size="+size+"): entering");
      int bytes_written = 0;
      // Write me
      Dbg.println("vfs", "VFS.write(fd="+fd+",size="+size+"): leaving");
      return bytes_written;
   }   

   /**
    * Sync read system call
    * 
    * @param fd file descriptor
    * @param size size in byte to read
    * @return actual bytes read
    */
   public int read(int fd, int size) {
      File file = openFiles[fd];
      if (file == null) {
         Error.println(Error.BAD_FILE_DESCRIPTOR, "");
         return -1;
      }
      Dbg.println("vfs", "VFS.read(fd="+fd+",size="+size+"): entering");
      int bytes_read = 0;
      Dbg.println("vfs", "VFS.read(fd="+fd+",size="+size+"): leaving");
      return bytes_read;
   }
}