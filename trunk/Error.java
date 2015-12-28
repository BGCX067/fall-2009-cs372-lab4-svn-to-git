/**
 * class Error
 *
 * Error message print utility
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
public class Error {
   // various kinds of error code
   
   /** trying to open a non-existing file */
   public final static int FILE_NOT_FOUND = -1;
   /** trying to create an already existing file */
   public final static int FILE_ALREADY_EXISTS = -2;
   /** attempting to write beyond maximum file size */
   public final static int FILE_SIZE_LIMIT = -3;
   /** attempting to write when there is no free data blocks */ 
   public final static int DISK_FULL = -4;
   /** attempting to write when there is no more inodes left */
   public final static int INODE_FULL = -5;
   /** attempting to create a file when directory entries are full */
   public final static int DENTRY_FULL = -6;
   /** attempting to write to a file that is opened with O_RDONLY */
   public final static int BAD_FILE_DESCRIPTOR = -7;
   /** trying to create a file with name longer than Dentry.MAX_FILENAME */
   public final static int ENAMETOOLONG = -8;

   
   /**
    * Print error message to stderr.
    *
    * @param errno error number
    * @param fileName file name that caused the error
    */
   public static void println(int errno, String fileName) {
      switch (errno) {
         case FILE_NOT_FOUND:
            System.err.println("Err: " + fileName + ": File not found!");
            break;
         case FILE_ALREADY_EXISTS:
            System.err.println("Err: " + fileName + ": File already exists!");
            break;
         case FILE_SIZE_LIMIT:
            System.err.println("Err: " + fileName + ": File size reached the limit!");
            break;
         case DISK_FULL:
            System.err.println("Err: " + fileName + ": Disk Full!");
            break;
         case INODE_FULL:
            System.err.println("Err: " + fileName + ": No more inodes left!");
            break;
         case DENTRY_FULL:
            System.err.println("Err: " + fileName + ": Directory entries full!");
            break;
         case BAD_FILE_DESCRIPTOR:
            System.err.println("Err: " + fileName + ": Bad file descriptor.");
            break;
         case ENAMETOOLONG:
            System.err.println("Err: " + fileName + ": File Name too long.");
            break;
         default:
      }
   }
}
