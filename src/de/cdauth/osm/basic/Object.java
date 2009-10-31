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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.cdauth.osm.basic.API;

/**
 * Parent class for all geographical objects in OSM, currently Nodes, Ways and Relations.
*/

abstract public class Object extends XMLObject implements Comparable<Object>
{
	private Hashtable<String,String> m_tags = null;

	protected Object(Element a_dom)
	{
		super(a_dom);
	}
	
	/**
	 * Two Objects are compared by comparing their tag names, their ids and their versions.
	 */
	public boolean equals(java.lang.Object a_other)
	{
		if(a_other instanceof Object)
		{
			Object other = (Object) a_other;
			return (getDOM().getTagName().equals(other.getDOM().getTagName()) && !getDOM().getAttribute("id").equals("") && getDOM().getAttribute("id").equals(other.getDOM().getAttribute("id")) && getDOM().getAttribute("version").equals(other.getDOM().getAttribute("version")));
		}
		else
			return false;
	}
	
	public int hashCode()
	{
		return new Long(getDOM().getAttribute("id")).hashCode();
	}
	
	public int compareTo(Object o)
	{
		int c1 = new Long(getDOM().getAttribute("id")).compareTo(new Long(o.getDOM().getAttribute("id")));
		if(c1 != 0)
			return c1;
		return new Long(getDOM().getAttribute("version")).compareTo(new Long(o.getDOM().getAttribute("version")));
	}
	
	@SuppressWarnings("unchecked")
	protected static <T extends Object> T fetchWithCache(String a_id, ObjectCache<T> a_cache, String a_version) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		T cached = a_cache.getVersion(a_id, a_version);
		if(cached != null)
			return cached;
		
		Object[] fetched = API.get("/"+a_cache.getType()+"/"+a_id+"/"+a_version);
		if(fetched.length < 1)
			throw new APIError("Server sent no data.");
		a_cache.cacheVersion((T) fetched[0]);
		return (T) fetched[0];
	}
	
	/**
	 * Fetches the version of an object that was the current one at the given point of time.
	 * @param <T>
	 * @param a_id
	 * @param a_cache
	 * @param a_type
	 * @param a_date
	 * @return null if the element did not exist at the given point of time
	 * @throws ParseException 
	 * @throws APIError 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws IOException 
	 */
	protected static <T extends Object> T fetchWithCache(String a_id, ObjectCache<T> a_cache, Date a_date) throws ParseException, IOException, SAXException, ParserConfigurationException, APIError
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		TreeMap<Long,T> history = fetchHistory(a_id, a_cache);
		for(T historyEntry : history.descendingMap().values())
		{
			Date historyDate = dateFormat.parse(historyEntry.getDOM().getAttribute("timestamp"));
			if(historyDate.compareTo(a_date) < 0)
				return historyEntry;
		}
		return null;
	}
	
	/**
	 * Fetches the version of an object that was the current before the given changeset was committed.
	 * @param <T>
	 * @param a_id
	 * @param a_cache
	 * @param a_type
	 * @param a_changeset
	 * @return null if the element did not exist before this changeset
	 * @throws ParseException
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws APIError
	 */
	
	protected static <T extends Object> T fetchWithCache(String a_id, ObjectCache<T> a_cache, Changeset a_changeset) throws ParseException, IOException, SAXException, ParserConfigurationException, APIError
	{
		long changeset = Long.parseLong(a_changeset.getDOM().getAttribute("id"));
		TreeMap<Long,T> history = fetchHistory(a_id, a_cache);
		for(T historyEntry : history.descendingMap().values())
		{
			long entryChangeset = Long.parseLong(historyEntry.getDOM().getAttribute("changeset"));
			if(entryChangeset < changeset)
				return historyEntry;
		}
		return null;
	}

	/**
	 * Internal function to fetch objects of type T with the IDs a_ids if they do not already exist in a_cache.
	 * @param <T>
	 * @param a_ids
	 * @param a_cache
	 * @return
	 * @throws IOException
	 * @throws APIError
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	
	@SuppressWarnings("unchecked")
	protected static <T extends Object> Hashtable<String,T> fetchWithCache(String[] a_ids, ObjectCache<T> a_cache) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		Hashtable<String,T> ret = new Hashtable<String,T>();
		ArrayList<String> ids = new ArrayList<String>(Arrays.asList(a_ids));
		for(int i=0; i<ids.size(); i++)
		{
			T cached = a_cache.getCurrent(ids.get(i));
			if(cached == null)
				continue;
			ret.put(ids.get(i), cached);
			ids.remove(i--);
		}
		
		if(ids.size() > 0)
		{
			Object[] fetched;
			if(ids.size() == 1)
				fetched = API.get("/"+a_cache.getType()+"/"+ids.get(0)); // URLEncoder.encode(, "UTF-8");
			else
				fetched = API.get("/"+a_cache.getType()+"s/?"+a_cache.getType()+"s="+API.joinStringArray(",", ids.toArray(new String[0])));
			for(int i=0; i<fetched.length; i++)
			{
				ret.put(fetched[i].getDOM().getAttribute("id"), (T)fetched[i]);
				a_cache.cacheCurrent((T)fetched[i]);
			}
		}
		
		return ret;
	}
	
	/**
	 * Returns an OSM Object; fetches it from the API if it isn’t cached already.
	 * @param a_id
	 * @param a_cache
	 * @return
	 * @throws IOException
	 * @throws APIError
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	
	protected static <T extends Object> T fetchWithCache(String a_id, ObjectCache<T> a_cache) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		String[] ids = { a_id };
		return fetchWithCache(ids, a_cache).get(a_id);
	}
	
	/**
	 * Returns a TreeMap of all versions of the element. The versions are ordered from the oldest to the newest. The indexes of the TreeMap match the version number.
	 * @param <T>
	 * @param a_id
	 * @param a_type
	 * @return
	 * @throws APIError 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends Object> TreeMap<Long,T> fetchHistory(String a_id, ObjectCache<T> a_cache) throws IOException, SAXException, ParserConfigurationException, APIError
	{
		TreeMap<Long,T> cached = a_cache.getHistory(a_id);
		if(cached != null)
			return cached;

		Object[] historyElements = API.get("/"+a_cache.getType()+"/"+a_id+"/history");
		TreeMap<Long,T> ordered = new TreeMap<Long,T>();
		for(Object element : historyElements)
			ordered.put(Long.parseLong(element.getDOM().getAttribute("version")), (T)element);
		a_cache.cacheHistory(ordered);
		return ordered;
	}
	
	/**
	 * Returns the value of a tag on this object. If the tag is not set, an empty string is returned. If the tag is set multiple times (should not be possible in API 0.6), the values are joined using a comma.
	 * @param a_tagname
	 * @return
	 */

	public String getTag(String a_tagname)
	{
		String ret = getTags().get(a_tagname);
		return ret == null ? "" : ret;
	}
	
	/**
	 * Returns a Hashtable of all tags set on this Object.
	 * @return
	 */
	public Hashtable<String,String> getTags()
	{
		if(m_tags == null)
		{
			m_tags = new Hashtable<String,String>();
			NodeList tags = getDOM().getElementsByTagName("tag");
			for(int i=0; i<tags.getLength(); i++)
			{
				if(tags.item(i).getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
					continue;
				Element item = (Element) tags.item(i);
				String key = item.getAttribute("k");
				if(m_tags.containsKey(key))
					m_tags.put(key, m_tags.get(key)+","+item.getAttribute("v"));
				else
					m_tags.put(key, item.getAttribute("v"));
			}
		}
		return m_tags;
	}
}