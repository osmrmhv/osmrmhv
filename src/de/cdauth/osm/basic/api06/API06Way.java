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

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Hashtable;
import java.util.TreeMap;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.LonLat;
import de.cdauth.osm.basic.ObjectCache;
import de.cdauth.osm.basic.Way;

public class API06Way extends API06GeographicalObject implements Way
{
	protected API06Way(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);
	}

	/**
	 * Ensures that all nodes of the way are downloaded and cached. This saves a lot of time when accessing them with fetch(), as fetch() makes an API call for each uncached item whereas this method can download all members at once.
	 * @param a_id
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws APIError 
	 * @throws IOException 
	 */
	
	public static void downloadFull(String a_id) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		boolean downloadNecessary = true;
		if(getCache().getCurrent(a_id) != null)
		{
			downloadNecessary = false;
			for(String it : API06Way.fetch(a_id).getMembers())
			{
				if(API06Node.getCache().getCurrent(it) == null)
				{
					downloadNecessary = true;
					break;
				}
			}
		}
		
		if(downloadNecessary)
		{
			API06Object[] fetched = API06API.get("/way/"+a_id+"/full");
			for(API06Object object : fetched)
			{
				if(object.getDOM().getTagName().equals("way"))
					API06Way.getCache().cacheCurrent((API06Way) object);
				else if(object.getDOM().getTagName().equals("node"))
					API06Node.getCache().cacheCurrent((API06Node) object);
			}
		}
	}
	
	/**
	 * Returns an array of the IDs of all nodes that are part of this way.
	 * @return
	 */
	
	public String[] getMembers()
	{
		NodeList members = getDOM().getElementsByTagName("nd");
		String[] ret = new String[members.getLength()];
		for(int i=0; i<members.getLength(); i++)
			ret[i] = ((Element)members.item(i)).getAttribute("ref");
		return ret;
	}
	
	/**
	 * Downloads all member nodes of this way (if necessary) and returns an array of them.
	 * @return
	 * @throws IOException
	 * @throws APIError
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	
	public Node[] getMemberNodes() throws IOException, APIError, SAXException, ParserConfigurationException
	{
		try
		{
			return getMemberNodes(null);
		}
		catch(ParseException e)
		{ // Cannot occur as no date has to be parsed
			return new API06Node[0];
		}
	}
	
	public Node[] getMemberNodes(Date a_date) throws APIError
	{
		if(a_date != null)
			downloadFull(getDOM().getAttribute("id"));
		String[] members = getMembers();
		API06Node[] ret = new API06Node[members.length];
		for(int i=0; i<members.length; i++)
			ret[i] = (a_date == null ? API06Node.fetch(members[i]) : API06Node.fetch(members[i], a_date));
		return ret;
	}
	
	@Override
	public LonLat getRoundaboutCentre() throws APIError
	{
		API06Node[] nodes = getMemberNodes();
		
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
	
	/**
	 * Gets all relations and ways containing this node.
	 * @return
	 * TODO
	 */

	public API06Object[] getParents()
	{
		return new API06Object[0];
	}
}