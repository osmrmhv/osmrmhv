/*
    This file is part of the osmrmhv library.

    osmrmhv is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    osmrmhv is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with osmrmhv. If not, see <http://www.gnu.org/licenses/>.
*/

package de.cdauth.osm.lib;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

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
	/**
	 * How many entries may be in the cache?
	 */
	public static final int MAX_CACHED_VALUES = 5000;
	/**
	 * How old may the entries in the cache be at most?
	 */
	public static final int MAX_AGE = 3600;
	
	private static final Map<ItemCache<? extends Item>, java.lang.Object> sm_instances = Collections.synchronizedMap(new WeakHashMap<ItemCache<? extends Item>, java.lang.Object>());

	private final Hashtable<ID,T> m_cache = new Hashtable<ID,T>();
	
	public ItemCache()
	{
		synchronized(sm_instances)
		{
			sm_instances.put(this, null);
		}
	}
	
	/**
	 * Caches the time stamp ({@link System#currentTimeMillis()}) when an entry is saved to the cache. Needed for
	 * {@link #cleanUp()}.
	 */
	private final SortedMap<Long,ID> m_cacheTimes = Collections.synchronizedSortedMap(new TreeMap<Long,ID>());

	/**
	 * Returns the object with the ID a_id. For versioned objects returns the version that is known to be the current
	 * one.
	 * @param a_id The ID of the object.
	 * @return The object or null if it is not in the cache.
	 */
	public T getObject(ID a_id)
	{
		synchronized(m_cache)
		{
			return m_cache.get(a_id);
		}
	}
	
	/**
	 * Caches an object.
	 * @param a_object The object to cache.
	 */
	public void cacheObject(T a_object)
	{
		synchronized(m_cache)
		{
			synchronized(m_cacheTimes)
			{
				ID id = a_object.getID();
				m_cache.put(id, a_object);
				m_cacheTimes.put(System.currentTimeMillis(), id);
			}
		}
	}
	
	/**
	 * Runs {@link #cleanUp()} on all instances of this class.
	 */
	protected static void cleanUpAll()
	{
		ItemCache<? extends Item>[] instances;
		synchronized(sm_instances)
		{ // Copy the list of instances to avoid locking the instances list (and thus preventing the creation of new
		  // instances) during the cleanup process.
			instances = sm_instances.keySet().toArray(new ItemCache[sm_instances.size()]);
		}
		for(ItemCache<? extends Item> instance : instances)
		{
			instance.cleanUp();
		}
	}
	
	/**
	 * Clean up entries from the cache that exceed {@link #MAX_CACHED_VALUES} or {@link #MAX_AGE}.
	 */
	protected void cleanUp()
	{
		while(true)
		{
			synchronized(m_cache)
			{
				synchronized(m_cacheTimes)
				{
					Long oldest = m_cacheTimes.firstKey();
					if(oldest == null || (System.currentTimeMillis()-oldest <= MAX_AGE*1000 && m_cacheTimes.size() <= MAX_CACHED_VALUES))
						break;
					ID id = m_cacheTimes.remove(oldest);
					m_cache.remove(id);
				}
			}
		}
	}
}
