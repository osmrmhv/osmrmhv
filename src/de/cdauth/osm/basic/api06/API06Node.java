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

import org.w3c.dom.Element;

import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.LonLat;
import de.cdauth.osm.basic.Node;
import de.cdauth.osm.basic.VersionedObjectCache;
import de.cdauth.osm.basic.Way;

/**
 * Represents a Node in OpenStreetMap.
 */

public class API06Node extends API06GeographicalObject implements Node
{
	protected API06Node(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);
	}

	@Override
	public LonLat getLonLat()
	{
		return new LonLat(Float.parseFloat(getDOM().getAttribute("lon")), Float.parseFloat(getDOM().getAttribute("lat")));
	}

	@Override
	public Way[] getContainingWays() throws APIError
	{
		Way[] ret = (Way[])getAPI().get("/node/"+getID()+"/ways");
		VersionedObjectCache<Way> cache = getAPI().getWayFactory().getCache();
		for(Way it : ret)
		{
			((API06Way)it).markAsCurrent();
			cache.cacheObject(it);
		}
		
		// FIXME: Cache this result somehow?
		return ret;
	}
}
