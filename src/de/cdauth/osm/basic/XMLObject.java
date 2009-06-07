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

import org.w3c.dom.Element;

/**
 * Abstract class for all objects whose information is saved in an XML DOM element.
 */
abstract public class XMLObject
{
	/** The DOM element containing the API XML response for this object. */
	protected Element m_dom;
	
	/**
	 * @param a_dom The DOM element.
	*/

	protected XMLObject(Element a_dom)
	{
		m_dom = a_dom;
	}
	
	/**
	 * @return The DOM element.
	 */

	public Element getDOM()
	{
		return m_dom;
	}
}
