/**
 * class BufferCache
 *
 * A memory cache for disk blocks.
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

public abstract class BufferCache {

   class BufferCacheKey {
      int block;
		
      BufferCacheKey(int block) {
         this.block = block;
      }
		
      public boolean equals(Object o) {
         BufferCacheKey key = (BufferCacheKey)o;
         if (this.block == key.block)
            return true;
         else
            return false;				
      }
		
      public int hashCode() {
         return block;
      }
   }
	
   /**
    * class BufferCacheValue
    */
   class BufferCacheValue {
      int pageNum;
      int timestamp;
      int pid;
      boolean pin;     // if pin=true, you should not remove it from buffer cache
      // if io_pending>0, the MemPage of this block is not yet ready.
      // Value of io_pending is number of processes waiting for this page 
      int io_pending;
      
      BufferCacheValue(int pageNum, int timestamp) {
         this.pageNum = pageNum;
         this.timestamp = timestamp;
         this.pid = sched.getCurrentPID().intValue();
         this.io_pending = 0;
      }
		
      public boolean equals(Object o) {
         BufferCacheValue value = (BufferCacheValue)o;
         if (this.pageNum == value.pageNum && 
             this.timestamp == value.timestamp)
            return true;
         else
            return false;
      }
		
      public int hashCode() {
         return (pageNum << 16) | (timestamp & 0xffff);
      }
   }

   protected BlockDriver bd;
   protected Hashtable allocTable;  // Buffer cache entry (Key, Value pair)
   private int time;              // internal time used for timestamp
   private BitSet pageAllocTable; // Memory Page allocation table
   private MemPage[] pages;       // Part of main memory that is dedicated to Buffer Cache
   private IScheduler sched;

   public static BufferCache BufferCacheFactory(
      boolean sync, BlockDriver _bd, MemPage[] _pages, IScheduler _sched) {
      if(sync) {
         return new BufferCacheSync(_bd, _pages, _sched);
      } else {
         assert false : "No async code in this distribution";
         // return new BufferCacheAsync(_bd, _pages, _sched);
         return null;
      }
   }
   
   protected BufferCache(BlockDriver _bd, MemPage[] _pages, IScheduler _sched) {
      this.bd = _bd;
      this.pages = _pages;
      this.sched = _sched;
      this.time = 0;
      this.allocTable = new Hashtable();
      this.pageAllocTable = new BitSet(pages.length);
   }

   private void printTable (Hashtable ht) {
      Enumeration keys = ht.keys();
      while (keys.hasMoreElements ()) {
         BufferCacheKey key = (BufferCacheKey) keys.nextElement();
         BufferCacheValue value = (BufferCacheValue) ht.get(key);
         System.err.println ("{ " + key.block + ", [" + value.pageNum 
                             + " " + value.pid
                             + " " + value.timestamp
                             + " " + value.pin
                             + "]}");
      }
    } 
   /**
    * Write to buffer cache
    * 
    */
   public int write(int block, byte[] buf, OSEvent ev) {
      // Write me
      return 0;
   }
   

   /**
    * Read from buffer cache 
    * 
    * @param block block number to read
    * @param ev event object
    * @return the result page that read block
    *         null, on error
    */
   public abstract MemPage read(int block, OSEvent ev);
	
   /**
    * Flush cache entry to disk
    * 
    */
   public void flush(int block, OSEvent ev) {
      // Write me
   }
	
   /**
    * Flush every entries in the buffer cache to disk
    * 
    */
   public void flushAll(OSEvent ev) {
      for (Iterator i = allocTable.keySet().iterator(); i.hasNext();) {
         BufferCacheKey key = (BufferCacheKey)i.next();
         // Allocate a fresh event for each block
         flush(key.block, new OSEvent(ev));
      }
   }
	
   protected MemPage lookup(BufferCacheKey key) {
      BufferCacheValue value = (BufferCacheValue)allocTable.get(key);
      if (value == null) {
         return null;
      } else {
         return getPage(value.pageNum);
      }
   }
	
   protected void update(BufferCacheKey key) {
      BufferCacheValue value = (BufferCacheValue)allocTable.get(key);
      value.timestamp = this.time++;
   }
   
   protected void incrIOPending(BufferCacheKey key) {
      BufferCacheValue value = (BufferCacheValue)allocTable.get(key);
      value.io_pending++;
   }
   
   public void decrIOPending(int block) {
      BufferCacheKey key = new BufferCacheKey(block);
      BufferCacheValue value = (BufferCacheValue)allocTable.get(key);
      value.io_pending--;
   }
   
   protected MemPage alloc(BufferCacheKey key) {
      int pageNum = pageAlloc();
      if (pageNum < 0)
         return null;
      return alloc(key, pageNum);		
   }
	
   protected MemPage alloc(BufferCacheKey key, int pageNum) {
      allocTable.put(key, new BufferCacheValue(pageNum, time++));
      return getPage(pageNum); 
   }
	
   private void removeEntry(BufferCacheKey key, OSEvent ev) {
      flush(key.block, ev);
      allocTable.remove(key);
   }
	
   /**
    * lookup and remove the least frequently used block
    * NOTE! It doesn't deallocate memory from operating system
    * The caller of this method should deallocate or reuse the page
    *  
    * @return the page number that has been removed from allocTable 
    */
   protected int removeLRU(OSEvent ev) {
      // Write me
      return 0;
   }
   
   public MemPage readSuperBlock() {
      BufferCacheKey key = new BufferCacheKey(VFS.BLK_SUPERBLOCK);
      MemPage page = lookup(key);
      Stat.inc("cache_read");
      Stat.inc("cache_hit");
      return page;
   }
   
   public MemPage readUsedBlockBitmap() {
      MemPage page = null;
      // Write me
      Stat.inc("cache_read");
      Stat.inc("cache_hit");
      return page;
   }
   
   public MemPage readInodeBitmap(int devNum) {
      MemPage page = null;
      // Write me
      Stat.inc("cache_read");
      Stat.inc("cache_hit");
      return page;
   }

   public MemPage readDirInode(int devNum) {
      MemPage page = null;
      // Write me
      Stat.inc("cache_read");
      Stat.inc("cache_hit");
      return page;
   }
   
   public MemPage readDirDataBlock(int devNum) {
      MemPage page = null;
      // Write me
      Stat.inc("cache_read");
      Stat.inc("cache_hit");
      return page;
   }
   
   public void setPin(int block, boolean pin) {
      BufferCacheKey key = new BufferCacheKey(block);
      BufferCacheValue value = (BufferCacheValue)allocTable.get(key);
      value.pin = pin;
   }

   //////////////////////////////////////////////////////
   // Page management
   public int pageAlloc() {
      try {
         int i = pageAllocTable.nextClearBit(0);
         // Memory Full
         if (i >= pages.length)
            return -1;
         pageAllocTable.set(i);
         return i;
      }
      catch (Exception e) {
         return -1;
      }
   }
   
    // XXX maybe eliminate since it isn't used
   public void pageFree(int pageNum) {
      try {
         pageAllocTable.clear(pageNum);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   public MemPage getPage(int pageNum) {
      return pages[pageNum];
   }

   public MemPage[] getPages() {
      return this.pages;
   }
}
