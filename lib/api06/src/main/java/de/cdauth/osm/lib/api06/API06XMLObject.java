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

import java.io.Serializable;

/**
 * Abstract class for all objects whose information is saved in an XML DOM element.
 */
abstract public class API06XMLObject implements Serializable
{
	/** The DOM element containing the API XML response for this object. */
	private final Element m_dom;
	
	private transient API06API m_api;
	
	/**
	 * @param a_dom The DOM element.
	*/

	protected API06XMLObject(Element a_dom, API06API a_api)
	{
		m_dom = a_dom;
		m_api = a_api;
	}

	protected void setAPI(API06API a_api)
	{
		m_api = a_api;
	}
	
	/**
	 * @return The DOM element.
	 */

	protected Element getDOM()
	{
		return m_dom;
	}
	
	protected API06API getAPI()
	{
		return m_api;
	}
}
