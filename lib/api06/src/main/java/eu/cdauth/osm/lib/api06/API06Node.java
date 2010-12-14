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

import org.w3c.dom.Element;

import eu.cdauth.osm.lib.APIError;
import eu.cdauth.osm.lib.ID;
import eu.cdauth.osm.lib.Item;
import eu.cdauth.osm.lib.LonLat;
import eu.cdauth.osm.lib.Node;
import eu.cdauth.osm.lib.VersionedItemCache;
import eu.cdauth.osm.lib.Way;
import java.util.Arrays;

/**
 * Represents a Node in OpenStreetMap.
 */

public class API06Node extends API06GeographicalItem implements Node
{
	private LonLat m_lonlat = null;
	private ID[] m_containingWays = null;

	protected API06Node(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);

		m_lonlat = new LonLat(Double.parseDouble(a_dom.getAttribute("lon")), Double.parseDouble(a_dom.getAttribute("lat")));
	}

	@Override
	public LonLat getLonLat()
	{
		return m_lonlat;
	}

	@Override
	public ID[] getContainingWays() throws APIError
	{
		if(m_containingWays == null)
		{
			Item[] ways = getAPI().get("/node/"+getID()+"/ways");
			VersionedItemCache<Way> cache = getAPI().getWayFactory().getCache();
			synchronized(this)
			{
				m_containingWays = new ID[ways.length];
				for(int i=0; i<ways.length; i++)
				{
					((API06GeographicalItem)ways[i]).markAsCurrent();
					cache.cacheObject((Way)ways[i]);
					m_containingWays[i] = ways[i].getID();
				}
			}
		}
		
		return Arrays.copyOf(m_containingWays, m_containingWays.length);
	}
}
