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

import de.cdauth.osm.lib.APIError;
import de.cdauth.osm.lib.ID;
import de.cdauth.osm.lib.Node;
import de.cdauth.osm.lib.VersionedItemCache;
import de.cdauth.osm.lib.Way;
import de.cdauth.osm.lib.WayFactory;

public class API06WayFactory extends API06GeographicalItemFactory<Way> implements WayFactory
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
		VersionedItemCache<Way> wayCache = getCache();
		VersionedItemCache<Node> nodeCache = (getAPI().getNodeFactory()).getCache();

		boolean downloadNecessary = true;
		if(wayCache.getObject(a_id) != null)
		{
			downloadNecessary = false;
			for(ID it : fetch(a_id).getMembers())
			{
				if(nodeCache.getObject(it) == null)
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
				if(object instanceof API06GeographicalItem)
					((API06GeographicalItem)object).markAsCurrent();

				if(object instanceof Way)
					wayCache.cacheObject((Way) object);
				else if(object instanceof Node)
					nodeCache.cacheObject((Node) object);
			}
		}
	}
}
