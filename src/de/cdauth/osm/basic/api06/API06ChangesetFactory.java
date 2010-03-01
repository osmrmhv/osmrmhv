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

import de.cdauth.osm.basic.Changeset;
import de.cdauth.osm.basic.ChangesetFactory;

public class API06ChangesetFactory extends API06ObjectFactory<Changeset> implements ChangesetFactory
{
	protected static final String TYPE = "changeset";
	
	protected API06ChangesetFactory(API06API a_api)
	{
		super(a_api, TYPE);
	}
}
