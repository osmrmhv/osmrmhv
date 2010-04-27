/*
	This file is part of the OSM Route Manager.

	OSM Route Manager is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	OSM Route Manager is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.

	You should have received a copy of the GNU Affero General Public License
	along with this software. If not, see <http://www.gnu.org/licenses/>.

	Copyright © 2010 Candid Dauth
*/

package eu.cdauth.osm.osmrm;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author cdauth
 */
public class GUI extends eu.cdauth.osm.web.common.GUI
{
	public static final Map<String,String> LOCALES = new Hashtable<String,String>() {{
		put("en_GB", "English");
		put("de_DE", "Deutsch");
		put("hu_HU", "Magyar");
	}};

	public static final String RESOURCE = "eu.cdauth.osm.osmrm.locale.osmrm";

	public GUI(HttpServletRequest a_req, HttpServletResponse a_resp)
	{
		super(a_req, a_resp);
	}

	@Override
	protected String getResourceName()
	{
		return RESOURCE;
	}

	@Override
	protected Map<String,String> getLocales()
	{
		return LOCALES;
	}

	@Override
	public String getTitle()
	{
		String title = super.getTitle();
		if(title == null)
			return _("OSM Route Manager");
		else
			return String.format(_("OSM Route Manager: %s"), title);
	}
}
