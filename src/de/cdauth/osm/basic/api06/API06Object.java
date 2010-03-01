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

import java.util.Hashtable;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.cdauth.osm.basic.Object;

/**
 * Parent class for all geographical objects in OSM, currently Nodes, Ways and Relations.
*/

abstract public class API06Object extends API06XMLObject implements Object
{
	private Hashtable<String,String> m_tags = null;

	protected API06Object(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);
	}
	
	/**
	 * Two Objects are compared by comparing their tag names, their ids and their versions.
	 */
	@Override
	public boolean equals(java.lang.Object a_other)
	{
		if(a_other instanceof API06Object)
		{
			API06Object other = (API06Object) a_other;
			return (getDOM().getTagName().equals(other.getDOM().getTagName()) && !getDOM().getAttribute("id").equals("") && getDOM().getAttribute("id").equals(other.getDOM().getAttribute("id")) && getDOM().getAttribute("version").equals(other.getDOM().getAttribute("version")));
		}
		else
			return false;
	}
	
	@Override
	public int hashCode()
	{
		return new Long(getID()).hashCode();
	}
	
	@Override
	public int compareTo(Object o)
	{
		return getID().compareTo(o.getID());
	}
	
	@Override
	public Long getID()
	{
		return new Long(getDOM().getAttribute("id"));
	}
	
	/**
	 * Returns the value of a tag on this object. If the tag is not set, an empty string is returned. If the tag is set multiple times (should not be possible in API 0.6), the values are joined using a comma.
	 * @param a_tagname
	 * @return
	 */
	@Override
	public String getTag(String a_tagname)
	{
		String ret = getTags().get(a_tagname);
		return ret == null ? "" : ret;
	}
	
	/**
	 * Returns a Hashtable of all tags set on this Object.
	 * @return
	 */
	@Override
	public Map<String,String> getTags()
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