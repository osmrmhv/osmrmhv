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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import de.cdauth.osm.lib.*;
import de.cdauth.osm.lib.ItemCache;

import javax.sql.DataSource;

abstract public class API06ItemFactory<T extends Item> implements ItemFactory<T>
{
	private final API06API m_api;
	private final String m_type;
	private final ItemCache<T> m_cache;
	
	protected API06ItemFactory(API06API a_api, String a_type)
	{
		m_api = a_api;
		m_type = a_type;
		m_cache = new ItemCache<T>(getAPI().getDatabaseCache(), getType());
	}
	
	protected ItemCache<T> getCache()
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
			else
			{
				if(cached instanceof API06XMLObject)
					((API06XMLObject)cached).setAPI(getAPI());
				ret.put(id, cached);
			}
		}
		
		if(toFetch.size() > 0)
		{
			Item[] fetched = getAPI().get(makeFetchURL(toFetch.toArray(new ID[0])));
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

	@Override
	public Set<T> search(Map<String,String> a_tags) throws APIError
	{
		try
		{
			if(a_tags.size() < 1)
				throw new APIError("You have to specify at least one tag.");
			Map.Entry<String,String>[] tags = a_tags.entrySet().toArray(new Map.Entry[a_tags.size()]);
			HashSet<T> ret = new HashSet<T>();
			ret.addAll((Collection)Arrays.asList(getAPI().get("/"+getType()+"s/search?type="+URLEncoder.encode(tags[0].getKey(), "UTF-8")+"&value="+URLEncoder.encode(tags[0].getValue(), "UTF-8"))));
			if(tags.length > 1)
			{
				Iterator<T> retIter = ret.iterator();
				retVals: while(retIter.hasNext())
				{
					Map<String,String> it_tags = retIter.next().getTags();
					for(Map.Entry<String,String> tag : tags)
					{
						if(!tag.getValue().equals(it_tags.get(tag.getKey())))
						{
							retIter.remove();
							continue retVals;
						}
					}
				}
			}

			return ret;
		}
		catch(UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}
}
