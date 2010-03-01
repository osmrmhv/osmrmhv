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

import java.util.Hashtable;

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
	private Hashtable<Long,T> m_newest = new Hashtable<Long,T>();
	
	/**
	 * Returns the most current version of the object with the ID a_id.
	 * @param a_id
	 * @return null if the object is not cached yet
	 */
	public T getCurrent(long a_id)
	{
		return m_newest.get(a_id);
	}
	
	/**
	 * Caches a version of an object and marks it as the current one.
	 * @param a_object
	 */
	public void cacheCurrent(T a_object)
	{
		m_newest.put(a_object.getID(), a_object);
	}
}
