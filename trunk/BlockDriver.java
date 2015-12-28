/**
 * class BlockDriver
 * 
 * A block driver manages the low level details
 * of the disk device, providing a simple store
 * at this sector number abstraction to the higher
 * levels of the system.
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */

import java.util.ArrayList;

public class BlockDriver {

   private Disk disk; // Only controls a single disk

   public class MultipleDisks extends Exception {
      MultipleDisks() { super(); }
      MultipleDisks(String s) { super(s); }
   }

   BlockDriver() {

      disk = null;
   }
   void init_disk(Disk _disk) {

      disk = _disk;
   }

   /** ev contains the DiskAddress under key "DiskAddress"
    * and contains the MemPage under the key "MemPage"
    */
   void read(OSEvent ev) throws Disk.BadDisk {
      Disk d = disk;
      DiskAddress sa = (DiskAddress)ev.get("DiskAddress");
      MemPage iop = (MemPage)ev.get("MemPage");
      ev.put("Disk", d);
      disk.read(sa, iop, ev);
      Dbg.println("bd", "BlockDriver.read(block="+sa.sector_number()+",..)");
      Dbg.printBytes("bdB", iop.get_bytes(), 0, 64);
   }

   /** ev contains the DiskAddress under key "DiskAddress"
    * and contains the MemPage under the key "MemPage"
    */
   void write(OSEvent ev) throws Disk.BadDisk {
      Disk d = disk;
      DiskAddress sa = (DiskAddress)ev.get("DiskAddress");

      MemPage iop = (MemPage)ev.get("MemPage");

      Dbg.println("bd", "BlockDriver.write(block="+sa.sector_number()+",..)");
      Dbg.printBytes("bdB", iop.get_bytes(), 0, 64);
      ev.put("Disk", d);
      disk.write(sa, iop, ev);
   }
   
}
