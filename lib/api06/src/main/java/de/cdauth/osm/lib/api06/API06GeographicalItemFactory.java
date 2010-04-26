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
