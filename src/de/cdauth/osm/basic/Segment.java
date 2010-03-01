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

package de.cdauth.osm.basic;

import de.cdauth.osm.basic.api06.API06Node;

/**
 * Represents a connection between two nodes.
 */

public class Segment
{
	private API06Node m_node1;
	private API06Node m_node2;
	
	public Segment(API06Node a_node1, API06Node a_node2)
	{
		m_node1 = a_node1;
		m_node2 = a_node2;
	}
	
	public boolean equals(java.lang.Object obj)
	{
		if(obj instanceof Segment)
		{
			Segment other = (Segment) obj;
			return (getNode1().equals(other.getNode1()) && getNode2().equals(other.getNode2())) || (getNode1().equals(other.getNode2()) && getNode2().equals(other.getNode1()));
		}
		return false;
	}

	public int hashCode()
	{
		return getNode1().hashCode() ^ getNode2().hashCode();
	}
	
	public API06Node getNode1()
	{
		return m_node1;
	}
	
	public API06Node getNode2()
	{
		return m_node2;
	}
}