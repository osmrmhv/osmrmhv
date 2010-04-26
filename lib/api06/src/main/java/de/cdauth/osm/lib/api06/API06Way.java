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

import java.util.Date;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.cdauth.osm.lib.APIError;
import de.cdauth.osm.lib.ID;
import de.cdauth.osm.lib.LonLat;
import de.cdauth.osm.lib.Node;
import de.cdauth.osm.lib.Way;

public class API06Way extends API06GeographicalItem implements Way
{
	/**
	 * Only for serialization.
	 */
	@Deprecated
	public API06Way()
	{
	}

	protected API06Way(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);
	}
	
	/**
	 * Returns an array of the IDs of all nodes that are part of this way.
	 * @return
	 */
	@Override
	public ID[] getMembers()
	{
		NodeList members = getDOM().getElementsByTagName("nd");
		ID[] ret = new ID[members.getLength()];
		for(int i=0; i<members.getLength(); i++)
			ret[i] = new ID(((Element)members.item(i)).getAttribute("ref"));
		return ret;
	}
	
	public Node[] getMemberNodes(Date a_date) throws APIError
	{
		if(a_date != null)
			getAPI().getWayFactory().downloadFull(new ID(getDOM().getAttribute("id")));
		ID[] members = getMembers();
		Node[] ret = new Node[members.length];
		for(int i=0; i<members.length; i++)
			ret[i] = (a_date == null ? getAPI().getNodeFactory().fetch(members[i]) : getAPI().getNodeFactory().fetch(members[i], a_date));
		return ret;
	}
	
	@Override
	public LonLat getRoundaboutCentre() throws APIError
	{
		Node[] nodes = getMemberNodes(null);
		
		if(nodes.length == 1)
			return nodes[0].getLonLat();

		if(nodes.length < 1 || !nodes[0].equals(nodes[nodes.length-1]))
			return null;
		
		double lon_sum = 0;
		double lat_sum = 0;
		for(int i=0; i<nodes.length-1; i++)
		{
			LonLat lonlat = nodes[i].getLonLat();
			lon_sum += lonlat.getLon();
			lat_sum += lonlat.getLat();
		}
		
		return new LonLat(lon_sum/(nodes.length-1), lat_sum/(nodes.length-1));
	}
}
