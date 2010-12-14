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

import eu.cdauth.osm.lib.GeographicalItem;
import eu.cdauth.osm.lib.ID;
import eu.cdauth.osm.lib.Node;
import eu.cdauth.osm.lib.Relation;
import eu.cdauth.osm.lib.RelationMember;
import eu.cdauth.osm.lib.Way;

public class API06RelationMember extends API06XMLItem implements RelationMember
{
	private String m_type = null;
	private ID m_referenceID = null;
	private ID m_relationID = null;
	private String m_role = null;
	
	protected API06RelationMember(Element a_dom, API06API a_api, ID a_relation)
	{
		super(a_dom, a_api);

		m_relationID = a_relation;
		m_type = a_dom.getAttribute("type");
		m_referenceID = new ID(a_dom.getAttribute("ref"));
		m_role = a_dom.getAttribute("role");
	}

	@Override
	public ID getRelation()
	{
		return m_relationID;
	}
	
	@Override
	public Class<? extends GeographicalItem> getType()
	{
		if(m_type.equals("node"))
			return Node.class;
		else if(m_type.equals("way"))
			return Way.class;
		else if(m_type.equals("relation"))
			return Relation.class;
		else
			throw new RuntimeException("Unknown relation member type "+m_type+".");
	}

	@Override
	public ID getReferenceID()
	{
		return m_referenceID;
	}

	@Override
	public String getRole()
	{
		return m_role;
	}
}
