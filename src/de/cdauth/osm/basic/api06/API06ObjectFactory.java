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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.ID;
import de.cdauth.osm.basic.Object;
import de.cdauth.osm.basic.ObjectCache;
import de.cdauth.osm.basic.ObjectFactory;

abstract public class API06ObjectFactory<T extends Object> implements ObjectFactory<T>
{
	private final API06API m_api;
	private final String m_type;
	private final ObjectCache<T> m_cache = new ObjectCache<T>();
	
	protected API06ObjectFactory(API06API a_api, String a_type)
	{
		m_api = a_api;
		m_type = a_type;
	}
	
	protected ObjectCache<T> getCache()
	{
		return m_cache;
	}
	
	protected String getType()
	{
		return m_type;
	}
	
	protected API06API getAPI()
	{
		return m_api;
	}
	
	protected String makeFetchURL(ID[] a_ids)
	{
		if(a_ids.length == 1)
			return "/"+getType()+"/"+a_ids[0];
		else
		{
			StringBuilder url = new StringBuilder("/"+getType()+"s/?"+getType()+"s=");
			for(int i=0; i<a_ids.length; i++)
			{
				if(i > 0) url.append(',');
				url.append(a_ids[i]);
			}
			return url.toString();
		}
	}

	@Override
	public Map<ID,T> fetch(ID[] a_ids) throws APIError
	{
		Hashtable<ID,T> ret = new Hashtable<ID,T>();
		ArrayList<ID> toFetch = new ArrayList<ID>();
		for(ID id : a_ids)
		{
			T cached = getCache().getObject(id);
			if(cached == null)
			{
				toFetch.add(id);
				continue;
			}
			ret.put(id, cached);
		}
		
		if(toFetch.size() > 0)
		{
			Object[] fetched = getAPI().get(makeFetchURL(toFetch.toArray(new ID[0])));
			for(int i=0; i<fetched.length; i++)
			{
				ret.put(fetched[i].getID(), (T)fetched[i]);
				getCache().cacheObject((T)fetched[i]);
			}
		}
		
		return ret;
	}

	@Override
	public T fetch(ID a_id) throws APIError
	{
		return fetch(new ID[] { a_id }).get(a_id);
	}
}
