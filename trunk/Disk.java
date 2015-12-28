/**
 * class Disk
 * 
 * A simulated disk device that reads/writes to a file
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */

/* A disk is a linear sequence of sectors.  Sectors are 
 * groups of bytes that must be read and written as a unit.
 * The disk is a block device, not a character device because
 * it must be read and written in blocks.
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

public class Disk {
   /* A sector is the atomic unit of transfer for a disk */
   // In this lab sectors and blocks are the same size.
   public static final int BYTES_PER_SECTOR = 4096;
   
   public class BadDisk extends Exception {
      BadDisk(int _code) {super(); code = _code; }
      BadDisk(int _code, String s) {super(s); code = _code;}
      int getCode() { return code; }
      int code;
   }
   public class DiskSector {
      DiskSector() {
         data = new byte[BYTES_PER_SECTOR];
      }
      void set_bytes(byte[] _data) {
         System.arraycopy(_data, 0, data, 0, BYTES_PER_SECTOR);
      }
      private byte data[];
   };

   Disk(String file_name, boolean _sync, float prob_of_failure) 
      throws BadDisk {
      file = new File(file_name);
      if(BYTES_PER_SECTOR * (int)(file.length() / BYTES_PER_SECTOR)
         != file.length()) {
         throw new BadDisk(EDISK_BAD_INITIALIZATION,
                           "Cannot open file " + file_name
                           + " as a disc device\nThe file is "
                           + file.length() 
                           + " bytes long, which is is not a\n"
                           + "multiple of the disk sector size which is " 
                           + BYTES_PER_SECTOR);
      }
      if(file.length() == 0) {
         throw new BadDisk(EDISK_BAD_INITIALIZATION,
                           "Disk model does not accept zero length files.\n" + file_name + " is zero length");
      }
      if(file.length() > (long)1<<31) {
         throw new BadDisk(EDISK_BAD_INITIALIZATION,
                           "Disk model does not accept files larger than 2GB");
      }
      try {
         raf = new RandomAccessFile(file, "rw");
      }
      catch(IOException e) {
         throw new BadDisk(EDISK_BAD_INITIALIZATION,
                           "Disk constructor failed " + e.getMessage());
      }
      num_sectors = (int)(file.length() / BYTES_PER_SECTOR);
      rand = new Random(1234321);
      fail_prob = prob_of_failure;
      sync = _sync;
      disk_failed = false;
      head = 0;
   }
   // Return codes
   final int DISK_OK = 0;
   // Disk is broken
   final int EDISK_BUSTED = -1;
   // Unknown possibly transient failure
   final int EDISK_UNKNOWN = -2;
   final int EDISK_BAD_INITIALIZATION = -3;
   final int EDISK_BOUNDS = -4;

   void data_read(DiskAddress sa, MemPage iop) throws BadDisk {
		p.p("DISK READ");
      // Check the class of our caller do to access control
      Throwable t = new Throwable();
      StackTraceElement[] ste = t.getStackTrace();
      String cname = ste[1].getClassName();
      if(!cname.equals("Disk")
         && !cname.equals("OS")) {
         System.err.println(cname + ", a class other than OS or Disk called Disk.data_read.\nThis is not allowed");
         t.printStackTrace();
      }

      check_fail(sa);
      try {
         raf.seek(sa.sector_number() * Disk.BYTES_PER_SECTOR); // modified by habals
         byte bytes[] = new byte[BYTES_PER_SECTOR];
         raf.read(bytes);
         iop.write(bytes, 0, BYTES_PER_SECTOR);

         Stat.add("disk_sectors_travelled", Math.abs(sa.sector_number() - head));
         Stat.inc("num_disk_reads");

         head = sa.sector_number();
      }
      catch(Exception e) {
         disk_failed = true;
         throw new BadDisk(EDISK_BUSTED,
                           "Exception in Disk device read.\n"
                           + "Disk is now broken "
                           + e.getMessage());
      }
   }

   // Given a DiskAddress, return the data at that sector,
   // and an error code
   void read(DiskAddress sa, MemPage iop, OSEvent ev) 
      throws BadDisk {
      check_fail(sa);
      if(sync) {
         data_read(sa, iop);
         OS.system_latency(op_latency(0, sa));	
      } else {
         ev.set_time(op_latency(0, sa));
         CallbackManager.schedule_event(ev);
      }
   }
      
   
   void data_write(DiskAddress sa, MemPage iop) throws BadDisk {

	   p.p("DISK WRITE");

      Throwable t = new Throwable();
      StackTraceElement[] ste = t.getStackTrace();
      String cname = ste[1].getClassName();
      if(!cname.equals("Disk")
         && !cname.equals("OS")) {
         System.err.println(cname + ", a class other than OS or Disk called Disk.data_write.\nThis is not allowed");
         t.printStackTrace();
      }
      check_fail(sa);

      try {

      	raf.seek(sa.sector_number() * Disk.BYTES_PER_SECTOR); // modified by habals

         raf.write(iop.get_bytes());

         Stat.add("disk_sectors_travelled", Math.abs(sa.sector_number() - head));
         Stat.inc("num_disk_writes");

         head = sa.sector_number();
      }
      catch(IOException e) {

         disk_failed = true;
         throw new BadDisk(EDISK_BUSTED,
                           "Exception in Disk device write.\n"
                           + "Disk is now broken "
                           + e.getMessage());
      }
   }
   
   void write(DiskAddress sa, MemPage iop, OSEvent ev)  
      throws BadDisk {
      check_fail(sa);
      if(sync) {
    	 // p.p("Disk.write(): sync");
         data_write(sa, iop);
         OS.system_latency(op_latency(1, sa));
      } else {
         ev.set_time(op_latency(0, sa));
         CallbackManager.schedule_event(ev);
      }
   }

   int op_latency(int op, DiskAddress sa) {
      // op == 0 for read, 1 for write
      return 20;
   }

   private int num_sectors;
   private DiskSector[] sectors;
   private Random rand;
   private float fail_prob;
   private boolean disk_failed;
   private File file;
   private RandomAccessFile raf;
   private int head;
   // Does this disk respond as soon as it is called?
   private boolean sync;

   private void check_fail(DiskAddress sa) throws BadDisk {
      if(disk_failed) {
         throw new BadDisk(EDISK_BUSTED, "Disk failed");
      }
      if(rand.nextFloat() < fail_prob) {
         disk_failed = true;
         throw new BadDisk(EDISK_BUSTED, "Disk failed");
      }
      if(sa.sector_number() >= num_sectors) {
			throw new BadDisk(EDISK_BOUNDS, "Disk indexed with sector "
                           + sa.sector_number()
                           + " but disk only has "
                           + num_sectors + " sectors\n" 
                           + "It looks like your disk is too small for this workload");
      }
   }
}
