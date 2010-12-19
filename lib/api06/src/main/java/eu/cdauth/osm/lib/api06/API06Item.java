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

import eu.cdauth.osm.lib.Changeset;
import java.util.Hashtable;
import java.util.Map;

import eu.cdauth.osm.lib.Item;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.cdauth.osm.lib.ID;
import eu.cdauth.osm.lib.Node;
import eu.cdauth.osm.lib.Relation;
import eu.cdauth.osm.lib.VersionedItem;
import eu.cdauth.osm.lib.Way;
import java.util.Collections;

/**
 * Parent class for all geographical objects in OSM, currently Nodes, Ways and Relations.
*/

abstract public class API06Item extends API06XMLItem implements Item
{
	private ID m_id = null;
	private Map<String,String> m_tags = null;

	protected API06Item(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);

		m_id = new ID(a_dom.getAttribute("id"));

		m_tags = new Hashtable<String,String>();
		NodeList tags = a_dom.getElementsByTagName("tag");
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
	
	/**
	 * Two Objects are compared by comparing their tag names, their ids and their versions.
	 */
	@Override
	public boolean equals(java.lang.Object a_other)
	{
		if(!(a_other instanceof Item))
			return false;
		if(!((Item)a_other).getID().equals(getID()))
			return false;
		if(a_other instanceof VersionedItem)
		{
			if(!(this instanceof VersionedItem))
				return false;
			if(!((VersionedItem)a_other).getVersion().equals(((VersionedItem)this).getVersion()))
				return false;
		}

		if(a_other instanceof Node && this instanceof Node)
			return true;
		if(a_other instanceof Way && this instanceof Way)
			return true;
		if(a_other instanceof Relation && this instanceof Relation)
			return true;
		if(a_other instanceof Changeset && this instanceof Changeset)
			return true;

		return false;
	}
	
	@Override
	public int hashCode()
	{
		return getID().hashCode();
	}
	
	@Override
	public int compareTo(Item o)
	{
		return getID().compareTo(o.getID());
	}
	
	@Override
	public ID getID()
	{
		return m_id;
	}
	
	@Override
	public String getTag(String a_tagname)
	{
		String ret = getTags().get(a_tagname);
		return ret == null ? "" : ret;
	}
	
	@Override
	public Map<String,String> getTags()
	{
		return Collections.unmodifiableMap(m_tags);
	}
}
