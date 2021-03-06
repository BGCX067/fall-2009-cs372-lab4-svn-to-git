/**
 * class Mkfs
 * 
 * This class creates a disk image
 * Similar to the mkfs file system utility
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-06 University of Texas at Austin
 */
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.io.IOException;

public class Mkfs {
   private static RandomAccessFile file = null;
   private static int numSectors = 0;
   
   /**
    * create image with multiple of disk sector closest to the given size
    * image size = (given size / Disk.BYTES_PER_SECTOR) * Disk.BYTES_PER_SECTOR
    * 
    * @param fileName file name to create
    * @param size image size
    * @return total number of sector created (size / Disk.BYTES_PER_SECTOR)
    */
   private static int createImage(String fileName, int size) {
		//p.p("### Attempting to create the Image, file name: " +fileName);
		try{
			file = new RandomAccessFile(fileName, "rw");
		    file.setLength(size);
		}
		catch (Exception e) {
			System.out.println(e);
		}
      return size/Disk.BYTES_PER_SECTOR;
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

   private static void printBytes(byte[] buffer, int offset, int size) {
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
	
   /**
    * close <code>file</code> that has been opened at createImage()
    */
   private static void close() {
		//p.p("### Closing the file system / disk image");
      try {file.close();}
      catch (IOException e) {System.out.println(e);}
   }
	
   /**
    * write a sector to <code>file</code>
    * <code>file</code> should be already opened.
    *  
    * @param sector a sector to write
    * @param buffer contents to write to the sector
    */
   public static void writeSector(int sector, byte[] buffer) {
		//p.p("### Attempting to write sector: " +sector);
      if(buffer == null){
        // p.p("### Sector Write Error: byte buffer is null!");
         return;
      }
      if(sector < 0 || sector > numSectors) {
        // p.p("### Sector Write Error: sector out of range!");
         return;
      }
		// **$** changed != to >
      if(buffer.length > Disk.BYTES_PER_SECTOR){
        // p.p("### Sector Write Error: block not correct size!");
         return;
      }
      //for(int i = 0; i < Disk.BYTES_PER_SECTOR; i++){
         try{
            file.seek(sector * Disk.BYTES_PER_SECTOR);
            file.write(buffer);
         }
         catch (IOException e) {
            System.out.println(e);
         }
      //}
   }
	
   /**
    * Make filesystem image
    * 
    * @param fileName image file name to create
    * @param size size of the image file
    * @return 0, on sucess, otherwise -1
    */
   public static int mkfs(String fileName, int size) {
      //p.p("### Starting Mkfs.mkfs()");
		numSectors = createImage(fileName, size);

      SuperBlock superBlock = new SuperBlock(numSectors);
      writeSector(0, superBlock.getBytes());
		BitMapBlock usedBlockBitMap = new BitMapBlock();
      BitMapBlock inodeBitMap = new BitMapBlock();
		inodeBitMap.set(0);
      writeSector(2, inodeBitMap.getBytes());
      DirectoryBlock directoryDataEntryBlock = new DirectoryBlock();
		writeSector(4, directoryDataEntryBlock.getBytes());
      Inode directoryInode = new Inode(superBlock, 0, 1);
      
      usedBlockBitMap.set(0);
      usedBlockBitMap.set(1);
      usedBlockBitMap.set(2);
      usedBlockBitMap.set(3);
      usedBlockBitMap.set(4);

      int i = 3;
      i += superBlock.getInodeBlockRatio() + 1;
      while(i < numSectors){
			usedBlockBitMap.set(i);
         i += superBlock.getInodeBlockRatio() + 1;
      }

		writeSector(1, usedBlockBitMap.getBytes());
		directoryInode.addDataBlock(4);
		writeSector(3, directoryInode.getBytes());
      
      return 0;
   }

   private static void usage() {
      System.out.println("Usage: java Mkfs [image_name] disk_size[M|k]");
      System.out.println("image_name: image file name to create");
      System.out.println("disk_size: size of a disk in Mbytes or Kbytes (default unit: M)");
      System.exit(-1);
   }
	
   public static void main(String[] args) {
		//p.p("### Starting Mkfs.main()");
      String imageName = "";
      String sizeStr = "";
      if (args.length < 1) {
         usage();
      } else if (args.length == 1) {
         imageName = ProcessTrace.getEnv("disk_file");
         sizeStr = args[0];
      } else {
         imageName = args[0];
         sizeStr = args[1];
      }
      String unit = sizeStr.substring(sizeStr.length()-1).toLowerCase();
      int size = 1;
      if (unit.equals("k"))
         size = size * 1024 * Integer.parseInt(sizeStr.substring(0, sizeStr.length()-1));
      else if (unit.equals("m"))
         size = size * 1024 * 1024 * Integer.parseInt(sizeStr.substring(0, sizeStr.length()-1));
			
      else {
         size = size * 1024 * 1024 * Integer.parseInt(sizeStr);
      }
		
      mkfs(imageName, size);
   }
}
