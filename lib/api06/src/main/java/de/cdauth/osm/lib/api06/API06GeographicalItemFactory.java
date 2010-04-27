/*
	Copyright © 2010 Candid Dauth

	Permission is hereby granted, free of charge, to any person obtaining
	a copy of this software and associated documentation files (the “Software”),
	to deal in the Software without restriction, including without limitation
	the rights to use, copy, modify, merge, publish, distribute, sublicense,
	and/or sell copies of the Software, and to permit persons to whom the Software
	is furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
	INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
	PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
	HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
	OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
	SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package de.cdauth.osm.lib.api06;

import java.util.*;

import de.cdauth.osm.lib.*;
import de.cdauth.osm.lib.VersionedItem;

abstract public class API06GeographicalItemFactory<T extends VersionedItem> extends API06ItemFactory<T> implements VersionedItemFactory<T>
{
	private final VersionedItemCache<T> m_cache;

	protected API06GeographicalItemFactory(API06API a_api, String a_type)
	{
		super(a_api, a_type);

		m_cache = new VersionedItemCache<T>(getAPI().getDatabaseCache(), getType());
	}
	
	@Override
	protected VersionedItemCache<T> getCache()
	{
		return m_cache;
	}

	@Override
	public T fetch(ID a_id, Date a_date) throws APIError
	{
		NavigableMap<Version,T> history = fetchHistory(a_id);
		for(T historyEntry : history.descendingMap().values())
		{
			Date historyDate = historyEntry.getTimestamp();
			if(historyDate.compareTo(a_date) <= 0)
				return historyEntry;
		}
		return null;
	}

	@Override
	public T fetch(ID a_id) throws APIError
	{
		T ret = super.fetch(a_id);
		if(ret instanceof API06GeographicalItem) // Should always be true
			((API06GeographicalItem)ret).markAsCurrent();
		getCache().cacheObject(ret);
		return ret;
	}

	@Override
	public Map<ID, T> fetch(ID[] a_ids) throws APIError
	{
		Map<ID,T> ret = super.fetch(a_ids);
		for(Map.Entry<ID,T> entry : ret.entrySet())
		{
			T it = entry.getValue();
			if(it instanceof API06GeographicalItem) // Should always be true
				((API06GeographicalItem)it).markAsCurrent();
			getCache().cacheObject(it);
		}
		return ret;
	}

	@Override
	public Set<T> search(Map<String,String> a_tags) throws APIError
	{
		Set<T> ret = super.search(a_tags);
		for(T it : ret)
		{
			if(it instanceof API06GeographicalItem) // Should always be true
				((API06GeographicalItem)it).markAsCurrent();
			getCache().cacheObject(it);
		}
		return ret;
	}

	@Override
	public T fetch(ID a_id, Changeset a_changeset) throws APIError
	{
		ID changeset = a_changeset.getID();
		NavigableMap<Version,T> history = fetchHistory(a_id);
		for(T historyEntry : history.descendingMap().values())
		{
			ID entryChangeset = historyEntry.getChangeset();
			if(entryChangeset.compareTo(changeset) < 0)
				return historyEntry;
		}
		return null;
	}
	
	@Override
	public NavigableMap<Version,T> fetchHistory(ID a_id) throws APIError
	{
		TreeMap<Version,T> cached = getCache().getHistory(a_id);
		if(cached != null)
		{
			for(T cachedEnt : cached.values())
			{
				if(cachedEnt instanceof API06XMLObject)
					((API06XMLObject)cachedEnt).setAPI(getAPI());
			}
			return cached;
		}

		Object[] historyElements = getAPI().get("/"+getType()+"/"+a_id+"/history");
		TreeMap<Version,T> ordered = new TreeMap<Version,T>();
		for(Object element : historyElements)
			ordered.put(((VersionedItem)element).getVersion(), (T)element);
		
		T lastEntry = ordered.lastEntry().getValue();
		if(lastEntry instanceof API06GeographicalItem) // Should always be true
			((API06GeographicalItem)lastEntry).markAsCurrent();

		getCache().cacheHistory(ordered);
		return ordered;
	}

	@Override
	public T fetch(ID a_id, Version a_version) throws APIError
	{
		T cached = getCache().getObject(a_id, a_version);
		if(cached != null)
		{
			if(cached instanceof API06XMLObject)
				((API06XMLObject)cached).setAPI(getAPI());
			return cached;
		}
		
		Object[] fetched = getAPI().get("/"+getType()+"/"+a_id+"/"+a_version);
		if(fetched.length < 1)
			throw new APIError("Server sent no data.");
		getCache().cacheObject((T) fetched[0]);
		return (T) fetched[0];
	}
}
