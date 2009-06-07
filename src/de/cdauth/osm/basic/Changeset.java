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
import java.util.Hashtable;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class Changeset extends Object
{
	static private Hashtable<String,Changeset> sm_cache = new Hashtable<String,Changeset>();
	
	protected Changeset(Element a_dom)
	{
		super(a_dom);
	}
	
	public static Changeset fetch(String a_id) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		return fetchWithCache(a_id, sm_cache, "changeset");
	}
	
	protected static boolean isCached(String a_id)
	{
		return sm_cache.containsKey(a_id);
	}

	public static void cache(Changeset a_object)
	{
		sm_cache.put(a_object.getDOM().getAttribute("id"), a_object);
	}
	
	public ChangesetContent getContent() throws IOException, APIError, SAXException, ParserConfigurationException
	{
		return ChangesetContent.fetch(getDOM().getAttribute("id"));
	}
}
