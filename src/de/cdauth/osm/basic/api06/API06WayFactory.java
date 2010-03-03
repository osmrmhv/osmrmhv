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

import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.ID;
import de.cdauth.osm.basic.Node;
import de.cdauth.osm.basic.VersionedObjectCache;
import de.cdauth.osm.basic.Way;
import de.cdauth.osm.basic.WayFactory;

public class API06WayFactory extends API06GeographicalObjectFactory<Way> implements WayFactory
{
	protected static final String TYPE = "way";

	protected API06WayFactory(API06API a_api)
	{
		super(a_api, TYPE);
	}
	
	/**
	 * Ensures that all nodes of the way are downloaded and cached. This saves a lot of time when accessing them with fetch(), as fetch() makes an API call for each uncached item whereas this method can download all members at once.
	 * @param a_id
	 * @throws APIError
	 */
	protected void downloadFull(ID a_id) throws APIError
	{
		VersionedObjectCache<Way> wayCache = getCache();
		VersionedObjectCache<Node> nodeCache = ((API06NodeFactory)getAPI().getNodeFactory()).getCache();

		boolean downloadNecessary = true;
		if(wayCache.getCurrent(a_id) != null)
		{
			downloadNecessary = false;
			for(ID it : fetch(a_id).getMembers())
			{
				if(nodeCache.getCurrent(it) == null)
				{
					downloadNecessary = true;
					break;
				}
			}
		}
		
		if(downloadNecessary)
		{
			Object[] fetched = getAPI().get("/way/"+a_id+"/full");
			for(Object object : fetched)
			{
				if(object instanceof Way)
					wayCache.cacheCurrent((Way) object);
				else if(object instanceof Node)
					nodeCache.cacheCurrent((Node) object);
			}
		}
	}
}
