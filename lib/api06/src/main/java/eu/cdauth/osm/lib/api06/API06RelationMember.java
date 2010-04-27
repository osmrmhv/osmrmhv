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

public class API06RelationMember extends API06XMLObject implements RelationMember
{
	private final Relation m_relation;
	
	protected API06RelationMember(Element a_dom, API06API a_api, Relation a_relation)
	{
		super(a_dom, a_api);
		m_relation = a_relation;
	}

	@Override
	public Relation getRelation()
	{
		return m_relation;
	}
	
	@Override
	public Class<? extends GeographicalItem> getType()
	{
		String type = getDOM().getAttribute("type");
		if(type.equals("node"))
			return Node.class;
		else if(type.equals("way"))
			return Way.class;
		else if(type.equals("relation"))
			return Relation.class;
		else
			throw new RuntimeException("Unknown relation member type "+type+".");
	}

	@Override
	public ID getReferenceID()
	{
		return new ID(getDOM().getAttribute("ref"));
	}

	@Override
	public String getRole()
	{
		return getDOM().getAttribute("role");
	}
}
