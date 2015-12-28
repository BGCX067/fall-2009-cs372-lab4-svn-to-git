/**
 * class File
 * 
 * In-memory representation of an open file
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */

public class File {	
   private int pid;
   private int fd;
   private int mode;
   private int pos;  //in bytes: The int offset parameter in VFS.seek() is used to update file's pos variable and the documentation says offset is in bytes.
   private String fileName;
   private Inode inode = null;

   public File() {
      fd = 0;
      pos = 0;
      mode = 0;
      fileName = null;
      inode = null;
   }
    
   public File(int fd, int mode, String fileName, Inode inode, int pid) {
      this.fd = fd;
      this.mode = mode;
      this.pos = 0;
      this.fileName = fileName;
      this.inode = inode;
      this.pid = pid;
   }

   public String getFileName() {
      return this.fileName;
   }

   public int getFd() {
      return fd;
   }

   public int getMode() {
      return mode;
   }

   public int getPos() {
      return pos;
   }

   public void setPos(int l) {
      pos = l;
   }

   public Inode getInode() {
      return inode;
   }

   public int getPid() {
      return pid;
   }
}
