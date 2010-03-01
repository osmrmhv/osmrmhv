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

import org.w3c.dom.Element;

import de.cdauth.osm.basic.Relation;
import de.cdauth.osm.basic.RelationMember;

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
}
