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

import org.w3c.dom.Element;

import de.cdauth.osm.lib.GeographicalItem;
import de.cdauth.osm.lib.ID;
import de.cdauth.osm.lib.Node;
import de.cdauth.osm.lib.Relation;
import de.cdauth.osm.lib.RelationMember;
import de.cdauth.osm.lib.Way;

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
