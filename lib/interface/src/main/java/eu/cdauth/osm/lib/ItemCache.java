/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package eu.cdauth.osm.lib;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * With this class you can easily cache OSM objects you retrieved from the API and that are not versioned. There
 * is a maximum number of cached values {@link #MAX_CACHED_VALUES} and a maximum age {@link #MAX_AGE}. Run
 * {@link #cleanUpAll} regularly to apply these limits.
 * 
 * <p>This class is to be used by the API implementations. These should check if objects are cached before fetching
 * them from the API. As this class looks up cached objects only by their ID, you need multiple instances for different
 * object types (ID scopes).
 * @author Candid Dauth
 */

public class ItemCache<T extends Item>
{
	protected static interface Reference<T>
	{
		public T get();
	}

	protected static class SoftReference<T> extends java.lang.ref.SoftReference<T> implements Reference<T>
	{
		public SoftReference(T referent)
		{
			super(referent);
		}
	}

	protected static class StrongReference<T> implements Reference<T>
	{
		private final T m_referent;

		public StrongReference(T referent)
		{
			m_referent = referent;
		}

		@Override
		public T get()
		{
			return m_referent;
		}
	}

	private final static boolean USE_SOFT_REFERENCES = false; //!System.getProperty("java.vm.name").equals("GNU libgcj");

	/**
	 * How many entries may be in the cache?
	 */
	public static final int MAX_CACHED_VALUES = Integer.MAX_VALUE; //USE_SOFT_REFERENCES ? Integer.MAX_VALUE : 500;
	/**
	 * How old may the entries in the cache be at most? (seconds)
	 */
	public static final int MAX_AGE = Integer.MAX_VALUE;

	private static final Logger sm_logger = Logger.getLogger(ItemCache.class.getName());

	private final Map<ID,Reference<T>> m_cache = new Hashtable<ID,Reference<T>>();

	/**
	 * Caches the time stamp ({@link System#currentTimeMillis()}) when a entry is saved to the cache. Needed for all
	 * clean up methods.
	 */
	private final ValueSortedMap<ID,Long> m_cacheTimes = new ValueSortedMap<ID,Long>();

	static
	{
		if(sm_logger.isLoggable(Level.INFO))
			sm_logger.info((USE_SOFT_REFERENCES ? "Using" : "Not using")+" soft references for caching.");
	}

	/**
	 * Creates a cache that stores several items in the memory.
	 */
	public ItemCache()
	{
	}

	/**
	 * Returns the object with the ID a_id. For versioned objects returns the version that is known to be the current
	 * one.
	 * @param a_id The ID of the object.
	 * @return The object or null if it is not in the cache.
	 */
	public T getObject(ID a_id)
	{
		T ret = null;
		synchronized(m_cache)
		{
			Reference<T> ref = m_cache.get(a_id);
			if(ref != null)
				ret = ref.get();
		}

		return ret;
	}
	
	/**
	 * Caches an object.
	 * @param a_object The object to cache.
	 */
	public void cacheObject(T a_object)
	{
		ID id = a_object.getID();

		synchronized(m_cache)
		{
			synchronized(m_cacheTimes)
			{
				Reference<T> oldRef = m_cache.get(id);
				T old = (oldRef == null ? null : oldRef.get());
				if(old == null || !old.equals(a_object)) // Prevent additionally downloaded data (for example the content of a changeset) from being lost.
					m_cache.put(id, makeReference(a_object));
				m_cacheTimes.put(id, System.currentTimeMillis());
			}
		}
	}
	
	/**
	 * Clean up entries from the memory cache that exceed {@link #MAX_CACHED_VALUES} or {@link #MAX_AGE}. If a database
	 * cache is used, the entries are moved there.
	 * @param a_completely If set to true, the memory cache will be cleared completely, not just up to {@link #MAX_CACHED_VALUES} (useful at shutdown)
	 */
	protected void cleanUpMemory(boolean a_completely)
	{
		sm_logger.info("Cache contains "+m_cache.size()+" entries.");

		int affected = 0;

		while(true)
		{
			T item;
			synchronized(m_cache)
			{
				synchronized(m_cacheTimes)
				{
					if(m_cacheTimes.isEmpty())
						break;
					ID oldest = m_cacheTimes.firstKey();
					long oldestTime = m_cacheTimes.get(oldest);
					if(!a_completely && System.currentTimeMillis()-oldestTime <= MAX_AGE*1000 && m_cacheTimes.size() <= MAX_CACHED_VALUES)
						break;
					m_cacheTimes.remove(oldest);
					item = m_cache.remove(oldest).get();
				}
			}

			affected++;
		}

		sm_logger.info("Removed "+affected+" entries from the memory.");
	}

	protected <T> Reference<T> makeReference(T obj)
	{
		if(USE_SOFT_REFERENCES)
			return new SoftReference<T>(obj);
		else
			return new StrongReference<T>(obj);
	}
}
