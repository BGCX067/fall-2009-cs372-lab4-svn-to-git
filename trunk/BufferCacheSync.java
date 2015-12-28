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

   protected BufferCacheSync(BlockDriver _bd, 
				MemPage[] _pages, 
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
   	public MemPage read(int block, OSEvent ev){
			// Write me
			MemPage page = null;
			Stat.inc("cache_read");
   		BufferCacheKey key = new BufferCacheKey(block);
			if(allocTable.containsKey(key)){
				Stat.inc("cache_hit");
				update(key);
				page = lookup(key);
			}
			else{
				Stat.inc("cache_miss");
				page = alloc(key);
				if( ev.get("DiskAddress") == null ) {
					ev.put("DiskAddress", new DiskAddress(block));
				}
				else {
					((DiskAddress)ev.get("DiskAddress")).set(block);
				} 		
				ev.put("MemPage", page);
				try {
					this.bd.read(ev);
					page = (MemPage)ev.get("MemPage");
				}
				catch (Exception e) {
					return null;
				}
			}
   		ReadCheck.write(((BufferCacheValue)allocTable.get(key)).pid, page.get_bytes()); 
     	return page;
   	}

}
