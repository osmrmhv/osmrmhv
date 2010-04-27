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

package eu.cdauth.osm.lib.api06;

import eu.cdauth.osm.lib.APIError;
import eu.cdauth.osm.lib.ID;
import eu.cdauth.osm.lib.Node;
import eu.cdauth.osm.lib.VersionedItemCache;
import eu.cdauth.osm.lib.Way;
import eu.cdauth.osm.lib.WayFactory;

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
