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

public class ObjectCache <T extends Object>
{
	private Hashtable<String,T> m_newest = new Hashtable<String,T>();
	private Hashtable<String,TreeMap<Long,T>> m_history = new Hashtable<String,TreeMap<Long,T>>();
	
	public T getCurrent(String a_id)
	{
		return m_newest.get(a_id);
	}
	
	public T getVersion(String a_id, Long a_version)
	{
		TreeMap<Long,T> history = getHistory(a_id);
		if(history == null)
			return null;
		return history.get(a_version);
	}
	
	public T getVersion(String a_id, String a_version)
	{
		return getVersion(a_id, new Long(a_version));
	}
	
	public TreeMap<Long,T> getHistory(String a_id)
	{
		TreeMap<Long,T> history = m_history.get(a_id);
		if(history == null)
			return null;
		
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
	
	public void cacheCurrent(T a_object)
	{
		m_newest.put(a_object.getDOM().getAttribute("id"), a_object);
		cacheVersion(a_object);
	}
	
	public void cacheVersion(T a_object)
	{
		TreeMap<Long,T> history = getHistory(a_object.getDOM().getAttribute("id"));
		if(history == null)
		{
			history = new TreeMap<Long,T>();
			m_history.put(a_object.getDOM().getAttribute("id"), history);
		}
		history.put(new Long(a_object.getDOM().getAttribute("version")), a_object);
	}
	
	public void cacheHistory(TreeMap<Long,T> a_history)
	{
		if(a_history.size() < 1)
			return;
		T current = a_history.lastEntry().getValue();
		cacheCurrent(current);
		m_history.put(current.getDOM().getAttribute("id"), a_history);
	}
}
