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
import java.util.Hashtable;
import java.util.TreeMap;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Way extends de.cdauth.osm.basic.Object
{
	static private Hashtable<String,Way> sm_cache = new Hashtable<String,Way>();
	
	protected Way(Element a_dom)
	{
		super(a_dom);
	}
	
	public static Hashtable<String,Way> fetch(String[] a_ids) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		return fetchWithCache(a_ids, sm_cache, "way");
	}
	
	public static Way fetch(String a_id) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		return fetchWithCache(a_id, sm_cache, "way");
	}
	
	public static TreeMap<Long,Way> getHistory(String a_id) throws IOException, SAXException, ParserConfigurationException, APIError
	{
		return fetchHistory(a_id, sm_cache, "way");
	}
	
	public static Way fetch(String a_id, String a_version) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		return fetchVersion(a_id, sm_cache, "way", a_version);
	}
	
	protected static boolean isCached(String a_id)
	{
		return sm_cache.containsKey(a_id);
	}

	public static void cache(Way a_object)
	{
		sm_cache.put(a_object.getDOM().getAttribute("id"), a_object);
	}
	
	public String[] getMembers()
	{
		NodeList members = getDOM().getElementsByTagName("nd");
		String[] ret = new String[members.getLength()];
		for(int i=0; i<members.getLength(); i++)
			ret[i] = ((Element)members.item(i)).getAttribute("ref");
		return ret;
	}
	
	public Node[] getMemberNodes() throws IOException, APIError, SAXException, ParserConfigurationException
	{
		downloadFull(getDOM().getAttribute("id"));
		String[] members = getMembers();
		Node[] ret = new Node[members.length];
		for(int i=0; i<members.length; i++)
			ret[i] = Node.fetch(members[i]);
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
		if(isCached(a_id))
		{
			downloadNecessary = false;
			for(String it : Way.fetch(a_id).getMembers())
			{
				if(!Node.isCached(it))
				{
					downloadNecessary = true;
					break;
				}
			}
		}
		
		if(downloadNecessary)
			API.get("/way/"+a_id+"/full");
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