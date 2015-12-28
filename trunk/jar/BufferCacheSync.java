/**
 * class BufferCacheSync
 *
 * A memory cache for disk blocks.  This buffer cache assumes a
 * synchronous interface to the disk (requests wait for the disk). 
 * 
 * @author Jungwoo Ha, Emmett Witchel
 *
 * Copyright (c) 2004-09 University of Texas at Austin
 */

public class BufferCacheSync extends BufferCache {

   protected BufferCacheSync(BlockDriver _bd, MemPage[] _pages, 
                             IScheduler _sched) {
      super(_bd, _pages, _sched);
   }

   
  /**
    * Read from buffer cache synchronously
    * if cache miss, read from disk
    * 
    * @param block block number to read
    * @param ev event object
    * @return the result page that read block
    *         null, on error
    */
   public MemPage read(int block, OSEvent ev) {
      MemPage page = null;
      // Write me
      return page;
   }

}
