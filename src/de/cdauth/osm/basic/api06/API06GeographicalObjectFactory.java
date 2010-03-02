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

package de.cdauth.osm.basic.api06;

import java.util.Date;
import java.util.NavigableMap;
import java.util.TreeMap;

import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.Changeset;
import de.cdauth.osm.basic.ID;
import de.cdauth.osm.basic.Version;
import de.cdauth.osm.basic.VersionedObject;
import de.cdauth.osm.basic.VersionedObjectCache;
import de.cdauth.osm.basic.VersionedObjectFactory;

abstract public class API06GeographicalObjectFactory<T extends VersionedObject> extends API06ObjectFactory<T> implements VersionedObjectFactory<T>
{
	private final VersionedObjectCache<T> m_cache = new VersionedObjectCache<T>();

	protected API06GeographicalObjectFactory(API06API a_api, String a_type)
	{
		super(a_api, a_type);
	}
	
	@Override
	protected VersionedObjectCache<T> getCache()
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
			return cached;

		Object[] historyElements = getAPI().get("/"+getType()+"/"+a_id+"/history");
		TreeMap<Version,T> ordered = new TreeMap<Version,T>();
		for(Object element : historyElements)
			ordered.put(((VersionedObject)element).getVersion(), (T)element);
		getCache().cacheHistory(ordered);
		return ordered;
	}

	@Override
	public T fetch(ID a_id, Version a_version) throws APIError
	{
		T cached = getCache().getVersion(a_id, a_version);
		if(cached != null)
			return cached;
		
		Object[] fetched = getAPI().get("/"+getType()+"/"+a_id+"/"+a_version);
		if(fetched.length < 1)
			throw new APIError("Server sent no data.");
		getCache().cacheVersion((T) fetched[0]);
		return (T) fetched[0];
	}
}
