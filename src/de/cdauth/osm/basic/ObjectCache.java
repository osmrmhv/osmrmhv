/*
    This file is part of OSM Route Manager.

    OSM Route Manager is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OSM Route Manager is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with OSM Route Manager.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.cdauth.osm.basic;

import java.util.Hashtable;
import java.util.TreeMap;

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

public class ObjectCache <T extends Object>
{
	private Hashtable<String,T> m_newest = new Hashtable<String,T>();
	private Hashtable<String,TreeMap<Long,T>> m_history = new Hashtable<String,TreeMap<Long,T>>();
	private String m_type;
	
	/**
	 * Pass as type a string like “node”, “way”, “relation” or “changeset”. It will be used in the HTTP URL of the API requests.
	 * @param a_type
	 */
	public ObjectCache(String a_type)
	{
		m_type = a_type;
	}

	/**
	 * Returns the type string you passed to the constructor. (For example “node”, “relation” or “changeset”.)
	 * @return
	 */
	public String getType()
	{
		return m_type;
	}
	
	/**
	 * Returns the most current version of the object with the ID a_id.
	 * @param a_id
	 * @return null if the object is not cached yet
	 */
	public T getCurrent(String a_id)
	{
		return m_newest.get(a_id);
	}
	
	/**
	 * Returns a specific version of the object with the ID a_id.
	 * @param a_id
	 * @param a_version
	 * @return null if the object is not cached yet
	 */
	public T getVersion(String a_id, Long a_version)
	{
		TreeMap<Long,T> history = getHistory(a_id);
		if(history == null)
			return null;
		return history.get(a_version);
	}
	
	/**
	 * Returns a specific version of the object with the ID a_id.
	 * @param a_id
	 * @param a_version
	 * @return null if the object is not cached yet
	 */
	public T getVersion(String a_id, String a_version)
	{
		return getVersion(a_id, new Long(a_version));
	}
	
	/**
	 * Returns the whole history of the object. The history is considered to be cached when all versions of it
	 * are definitely in the cache (which is the case when the current version is saved and all versions from 1 
	 * to the current one’s version number are existant).
	 * @param a_id
	 * @return null if the history of the object is not cached yet
	 */
	public TreeMap<Long,T> getHistory(String a_id)
	{
		TreeMap<Long,T> history = m_history.get(a_id);
		if(history == null)
			return null;
		
		// Check if all versions have been fetched into history
		T current = getCurrent(a_id);
		if(current == null)
			return null;
		long currentVersion = Long.parseLong(current.getDOM().getAttribute("version"));
		
		for(long i=1; i<=currentVersion; i++)
		{
			if(!history.containsKey(new Long(i)))
				return null;
		}
		return history;
	}
	
	/**
	 * Caches a version of an object and marks it as the current one.
	 * @param a_object
	 */
	public void cacheCurrent(T a_object)
	{
		m_newest.put(a_object.getDOM().getAttribute("id"), a_object);
		cacheVersion(a_object);
	}
	
	/**
	 * Caches a version of an object that is not (or not for sure) the current one.
	 * @param a_object
	 */
	public void cacheVersion(T a_object)
	{
		if(a_object.getDOM().getAttribute("version").equals(""))
			return;
		TreeMap<Long,T> history = getHistory(a_object.getDOM().getAttribute("id"));
		if(history == null)
		{
			history = new TreeMap<Long,T>();
			m_history.put(a_object.getDOM().getAttribute("id"), history);
		}
		history.put(new Long(a_object.getDOM().getAttribute("version")), a_object);
	}
	
	/**
	 * Caches the whole history of an object. The last entry is considered to be the current version.
	 * @param a_history
	 */
	public void cacheHistory(TreeMap<Long,T> a_history)
	{
		if(a_history.size() < 1)
			return;
		T current = a_history.lastEntry().getValue();
		cacheCurrent(current);
		m_history.put(current.getDOM().getAttribute("id"), a_history);
	}
}
