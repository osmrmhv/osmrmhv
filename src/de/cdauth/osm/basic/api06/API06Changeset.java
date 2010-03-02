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

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.Changeset;
import de.cdauth.osm.basic.ChangesetContent;

/**
 * Represents an OpenStreetMap changeset. This only includes the tags and other information
 * about the changeset, the content (the modified elements) are represented by a ChangesetContent
 * object.
 */

public class API06Changeset extends API06Object implements Changeset
{
	protected API06Changeset(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);
	}

	/**
	 * Returns the ChangesetContent object for this changeset.
	 * @return A ChangesetContent object
	 * @throws IOException
	 * @throws APIError
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	@Override
	public ChangesetContent getContent() throws APIError
	{
		API06ChangesetContent ret = getAPI().getChangesetContentFactory().fetch(getID());
		ret.setChangeset(this);
		return ret;
	}

	@Override
	public Date getCreationDate()
	{
		try
		{
			return API06GeographicalObject.getDateFormat().parse(getDOM().getAttribute("created_at"));
		}
		catch(ParseException e)
		{
			return null;
		}
	}
}
