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

import java.util.Hashtable;
import java.util.Map;

import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.ID;

public class API06ChangesetContentFactory extends API06ObjectFactory<API06ChangesetContent>
{
	protected static final String TYPE = "changeset";
	
	protected API06ChangesetContentFactory(API06API a_api)
	{
		super(a_api, TYPE);
	}

	@Override
	protected String makeFetchURL(ID[] a_ids)
	{
		if(a_ids.length != 1)
			throw new IllegalArgumentException("Can only fetch one changeset content at a time.");
		return "/"+getType()+"/"+a_ids[0]+"/download";
	}
	
	@Override
	public Map<ID,API06ChangesetContent> fetch(ID[] a_ids) throws APIError
	{
		Map<ID,API06ChangesetContent> ret = new Hashtable<ID,API06ChangesetContent>();
		for(ID id : a_ids)
			ret.put(id, fetch(id));
		return ret;
	}
	
	@Override
	public API06ChangesetContent fetch(ID a_id) throws APIError
	{
		return super.fetch(new ID[]{ a_id }).get(a_id);
	}
}
