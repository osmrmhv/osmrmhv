/*
    This file is part of OSM Route Manager.

    OSM Route Manager is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OSM Route Manager is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with OSM Route Manager.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.cdauth.osm.basic;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Hashtable;
import java.util.TreeMap;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Way extends de.cdauth.osm.basic.Object
{
	static private ObjectCache<Way> sm_cache = new ObjectCache<Way>("way");
	
	protected Way(Element a_dom)
	{
		super(a_dom);
	}
	
	public static ObjectCache<Way> getCache()
	{
		return sm_cache;
	}
	
	public static Hashtable<String,Way> fetch(String[] a_ids) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		return fetchWithCache(a_ids, sm_cache);
	}
	
	public static Way fetch(String a_id) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		return fetchWithCache(a_id, sm_cache);
	}
	
	public static Way fetch(String a_id, String a_version) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		return fetchWithCache(a_id, sm_cache, a_version);
	}
	
	public static Way fetch(String a_id, Date a_date) throws ParseException, IOException, SAXException, ParserConfigurationException, APIError
	{
		return fetchWithCache(a_id, sm_cache, a_date);
	}
	
	public static Way fetch(String a_id, Changeset a_changeset) throws ParseException, IOException, SAXException, ParserConfigurationException, APIError
	{
		return fetchWithCache(a_id, sm_cache, a_changeset);
	}
	
	public static TreeMap<Long,Way> getHistory(String a_id) throws IOException, SAXException, ParserConfigurationException, APIError
	{
		return fetchHistory(a_id, sm_cache);
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
			for(String it : Way.fetch(a_id).getMembers())
			{
				if(Node.getCache().getCurrent(it) == null)
				{
					downloadNecessary = true;
					break;
				}
			}
		}
		
		if(downloadNecessary)
		{
			Object[] fetched = API.get("/way/"+a_id+"/full");
			for(Object object : fetched)
			{
				if(object.getDOM().getTagName().equals("way"))
					Way.getCache().cacheCurrent((Way) object);
				else if(object.getDOM().getTagName().equals("node"))
					Node.getCache().cacheCurrent((Node) object);
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
			return new Node[0];
		}
	}
	
	public Node[] getMemberNodes(Date a_date) throws IOException, APIError, SAXException, ParserConfigurationException, ParseException
	{
		if(a_date != null)
			downloadFull(getDOM().getAttribute("id"));
		String[] members = getMembers();
		Node[] ret = new Node[members.length];
		for(int i=0; i<members.length; i++)
			ret[i] = (a_date == null ? Node.fetch(members[i]) : Node.fetch(members[i], a_date));
		return ret;
	}
	
	public LonLat getRoundaboutCentre() throws IOException, APIError, SAXException, ParserConfigurationException
	{
		Node[] nodes = getMemberNodes();
		
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

	public Object[] getParents()
	{
		return new Object[0];
	}
}