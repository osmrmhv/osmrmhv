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

import java.util.Map;

public interface ObjectFactory<T extends Object>
{
	/**
	 * Internal function to fetch objects of type T with the IDs a_ids if they do not already exist in a_cache.
	 * @param <T>
	 * @param a_ids
	 * @return
	 * @throws APIError
	 */
	public Map<ID,T> fetch(ID[] a_ids) throws APIError;
	
	/**
	 * Returns an OSM Object; fetches it from the API if it isn’t cached already.
	 * @param a_id
	 * @return
	 * @throws APIError
	 */
	public T fetch(ID a_id) throws APIError;
}
