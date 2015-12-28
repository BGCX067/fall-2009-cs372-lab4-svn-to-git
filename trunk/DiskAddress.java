/**
 * class DiskAddress
 * 
 * A disk can be physically laid out as a linear array of
 * sectors, but it is more likely to have a more complicated
 * structure.  But in this lab, it is just an array, and this
 * class is just a wrapper.
 *
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */

public class DiskAddress {
   DiskAddress(int _sector_number) {

      sector_number = _sector_number;
   }
   DiskAddress set(int _sector_number) {

      sector_number = _sector_number;
      return this;
   }
   int sector_number() {
      return sector_number;
   }
   private int sector_number;
};
