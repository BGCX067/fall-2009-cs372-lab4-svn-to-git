/**
 * class DiskDumper
 * 
 * A little utility to help you read your disk
 * od -x (the unix utility) is also useful
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.Iterator;
import java.nio.ByteBuffer;

public class DiskDumper {
	
   RandomAccessFile disk;
	
   public static void main(String[] args) {
      if (args.length < 2) {
         System.out.println("Usage: java DiskDumper filename command [args...]");
         System.out.println("commands");
         System.out.println("\tbyte [block number ...]");
         System.out.println("\tmd5 [block number ...]");
         System.out.println("\tcontents (prints block-independent disk contents)");
         System.out.println("\tcont-md5 (prints block-independent disk contents + md5sum)");
         System.exit(-1);
      }
      if (args[1].equals("byte")) {
         byteCommand(args);
      }
      else if (args[1].equals("md5")) {
         md5Command(args);
      }
      else if (args[1].equals("contents")) {
         dumpContents(args);
      } else if(args[1].equals("cont-md5")) {
         dumpContents(args); 
         md5Command(args);
      }
   }
	
   private static RandomAccessFile open(String fileName) {
      try {
         return new RandomAccessFile(fileName, "r");	
      }
      catch (FileNotFoundException e) {
         System.out.println(e.getMessage());
         System.exit(-1);
      }
      return null;
   }
	
   private static byte[] readSector(RandomAccessFile file, int sector) {
      byte[] barray = new byte[Disk.BYTES_PER_SECTOR];
      try {
         file.seek(sector * Disk.BYTES_PER_SECTOR);
         file.read(barray);
      }
      catch (Exception e) {
         System.out.println(e.getMessage());
         System.exit(-1);
      }		
      return barray;
   }
	
   private static String toHexString(int i, int radix) {
      if (i < 0) i = i + (1 << (radix*8));
      String str = Integer.toHexString(i);
      if (str.length() > radix)
         str = str.substring(str.length()-radix);
      while (str.length() < radix)
         str = "0" + str;
      return str;
   }
	
   private static void printBytes(byte[] buffer) {
      printBytes(buffer, 0, buffer.length);
   }
	
   private static void printBytes(byte[] buffer, 
                                  int offset, int size) {
      String line = "";
      size = size + offset % 32;
      offset = offset - offset % 32;
      size = size + ((32 - size % 32) % 32);
		
      for (int i = offset; i < offset+size; i+= 32) {
         line = toHexString(i, 4).toUpperCase();
         for (int j = i; j < i+32; j++) {				
            line = line + " " + toHexString((int)buffer[j], 2);
         }
         line = line + "  ";
         System.out.println(line);
      }
   }
	
   private static void byteCommand(String[] args) {
      RandomAccessFile file = open(args[0]);
      for (int i = 2; i < args.length; i++) {
         int sector = Integer.parseInt(args[i]);
         System.out.println("Sector: " + Integer.toHexString(sector));
         printBytes(readSector(file, sector));
      }
   }
   
   private static void md5Command(String[] args) {
      try {
         RandomAccessFile file = open(args[0]);
         MessageDigest md = MessageDigest.getInstance("MD5");
		 if (args.length == 2) {
			byte [] buf = new byte[(int)file.length()];	
			file.readFully(buf);
			byte [] digest = md.digest(buf);
            for (int j = 0; j < digest.length; j++)
               System.out.print(toHexString((int)digest[j], 2));
			System.out.println("");
		 }

         for (int i = 2; i < args.length; i++) {
            int sector = Integer.parseInt(args[i]);
            byte[] buf = readSector(file, sector);            
            byte[] digest = md.digest(buf);
            for (int j = 0; j < digest.length; j++)
               System.out.print(toHexString((int)digest[j], 2));
            System.out.println("");
         }
      }
      catch (Exception e) {
         System.err.println("Error");
      }
   }

   private static DirectoryBlock readDirB(RandomAccessFile raf) {
      return new DirectoryBlock(readSector(raf, VFS.BLK_DIRDATABLOCK));
   }
   private static SuperBlock getSuperBlock(RandomAccessFile raf) {
      return new SuperBlock(readSector(raf, VFS.BLK_SUPERBLOCK));
   }
   private static void dumpContents(String[] args) {
      try {
         RandomAccessFile file = open(args[0]);
         SuperBlock sb = getSuperBlock(file);
         DirectoryBlock db = readDirB(file);
         Dentry[] dentries = db.getDentries();
         int i;
         Arrays.sort(dentries);
         for(i = 0; i < dentries.length; ++i) {
            if(dentries[i].getIno() == Inode.EMPTY) continue;
            System.out.println(dentries[i].getFileName());
            int ino = dentries[i].getIno();
            Inode inode = new Inode(sb);
            int blkNum = VFS.BLK_DIRINODE 
               + (sb.getInodeBlockRatio()+1) * (ino / sb.getInodesPerBlock());
            byte [] block = readSector(file, blkNum);
            inode.setBytes(block, ino % sb.getInodesPerBlock());
            MessageDigest md = MessageDigest.getInstance("MD5");
            ByteBuffer [] bbArr = new ByteBuffer[inode.getDataBlocksUsed()];
            for(int j = 0; j < inode.getDataBlocksUsed(); ++j) {
               blkNum = inode.getDataBlock(j);
               block = readSector(file, blkNum);
               md.reset();
               bbArr[j] = ByteBuffer.wrap(md.digest(block));
            }
            Arrays.sort(bbArr);
            for(int j = 0; j < inode.getDataBlocksUsed(); ++j) {
               byte[] digest = bbArr[j].array();
               for (int k = 0; k < digest.length; k++)
                  System.out.print(toHexString((int)digest[k], 2));
               System.out.println("");
            }
         }
      }
      catch (Exception e) {
         System.err.println("Error");
      }
   }
}
