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

import java.util.Date;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.cdauth.osm.lib.APIError;
import eu.cdauth.osm.lib.ID;
import eu.cdauth.osm.lib.LonLat;
import eu.cdauth.osm.lib.Node;
import eu.cdauth.osm.lib.Way;
import java.util.Arrays;

public class API06Way extends API06GeographicalItem implements Way
{
	private ID[] m_members = null;

	protected API06Way(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);

		NodeList members = a_dom.getElementsByTagName("nd");
		m_members = new ID[members.getLength()];
		for(int i=0; i<members.getLength(); i++)
			m_members[i] = new ID(((Element)members.item(i)).getAttribute("ref"));
	}
	
	/**
	 * Returns an array of the IDs of all nodes that are part of this way.
	 * @return An array of the IDs of the member nodes
	 */
	@Override
	public ID[] getMembers()
	{
		return Arrays.copyOf(m_members, m_members.length);
	}

	@Override
	public Node[] getMemberNodes(Date a_date) throws APIError
	{
		if(a_date != null)
			getAPI().getWayFactory().downloadFull(getID());
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
