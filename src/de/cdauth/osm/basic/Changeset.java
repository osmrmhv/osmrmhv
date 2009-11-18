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
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Represents an OpenStreetMap changeset. This only includes the tags and other information
 * about the changeset, the content (the modified elements) are represented by a ChangesetContent
 * object.
 */

public class Changeset extends Object
{
	static private ObjectCache<Changeset> sm_cache = new ObjectCache<Changeset>("changeset");
	
	protected Changeset(Element a_dom)
	{
		super(a_dom);
	}
	
	public static Changeset fetch(String a_id) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		return fetchWithCache(a_id, sm_cache);
	}
	
	public static ObjectCache<Changeset> getCache()
	{
		return sm_cache;
	}

	/**
	 * Returns the ChangesetContent object for this changeset.
	 * @return A ChangesetContent object
	 * @throws IOException
	 * @throws APIError
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public ChangesetContent getContent() throws IOException, APIError, SAXException, ParserConfigurationException
	{
		return ChangesetContent.fetch(getDOM().getAttribute("id"));
	}
}
