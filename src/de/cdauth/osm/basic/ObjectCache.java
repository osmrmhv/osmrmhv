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

package de.cdauth.osm.basic;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

/**
 * With this class you can easily cache OSM objects you received from the API, including their history.
 * The properties of each OSM object are determined by reading their “id” and “version” XML attribute.
 * For each ID the single versions are cached. The most current version of an object is specially marked, so you
 * have to use the cacheCurrent() method instead of cacheVersion() to cache it. (Or you pass the whole history
 * using cacheHistory().)
 * @author Candid Dauth
 *
 * @param <T>
 */

public class ObjectCache<T extends Object>
{
	/**
	 * How many entries may be in the cache?
	 */
	public static final int MAX_CACHED_VALUES = 5000;
	/**
	 * How old may the entries in the cache be at most?
	 */
	public static final int MAX_AGE = 3600;
	
	public static final Map<ObjectCache<? extends Object>, java.lang.Object> sm_instances = Collections.synchronizedMap(new WeakHashMap<ObjectCache<? extends Object>, java.lang.Object>());

	private final Hashtable<ID,T> m_newest = new Hashtable<ID,T>();
	
	public ObjectCache()
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
	private final SortedMap<Long,ID> m_newestTimes = Collections.synchronizedSortedMap(new TreeMap<Long,ID>());
	
	/**
	 * Returns the most current version of the object with the ID a_id.
	 * @param a_id
	 * @return null if the object is not cached yet
	 */
	public T getCurrent(ID a_id)
	{
		synchronized(m_newest)
		{
			return m_newest.get(a_id);
		}
	}
	
	/**
	 * Caches a version of an object and marks it as the current one.
	 * @param a_object
	 */
	public void cacheCurrent(T a_object)
	{
		synchronized(m_newest)
		{
			synchronized(m_newestTimes)
			{
				ID id = a_object.getID();
				m_newest.put(id, a_object);
				m_newestTimes.put(System.currentTimeMillis(), id);
			}
		}
	}
	
	/**
	 * Runs {@link #cleanUp()} on all instances of this class.
	 */
	protected static void cleanUpAll(int a_sleepBetween)
	{
		ObjectCache<? extends Object>[] instances;
		synchronized(sm_instances)
		{ // Copy the list of instances to avoid locking the instances list (and thus preventing the creation of new
		  // instances) during the cleanup process.
			instances = sm_instances.keySet().toArray(new ObjectCache[0]);
		}
		for(ObjectCache<? extends Object> instance : instances)
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
			synchronized(m_newest)
			{
				synchronized(m_newestTimes)
				{
					Long oldest = m_newestTimes.firstKey();
					if(oldest == null || (System.currentTimeMillis()-oldest <= MAX_AGE*1000 && m_newestTimes.size() <= MAX_CACHED_VALUES))
						break;
					ID id = m_newestTimes.remove(oldest);
					m_newest.remove(id);
				}
			}
		}
	}
}
