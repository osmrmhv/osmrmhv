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

import de.cdauth.osm.basic.WayFactory;
import de.cdauth.osm.basic.Way;

public class API06WayFactory extends API06GeographicalObjectFactory<Way> implements WayFactory
{
	protected static final String TYPE = "way";

	protected API06WayFactory(API06API a_api)
	{
		super(a_api, TYPE);
	}
}
