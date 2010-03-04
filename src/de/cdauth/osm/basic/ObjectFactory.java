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

/**
 * The methods of this type are common to all factories that fetch {@link Object}s using an {@link API} implementation.
 * The sub-types of this interface define which type of object is to be referenced by the IDs used in these methods.
 * @author cdauth
 */
public interface ObjectFactory<T extends Object>
{
	/**
	 * Fetches multiple objects from the API at once.
	 * @param a_ids The list of IDs to fetch objects for.
	 * @return A map that contains the IDs from a_ids as keys and the fetched objects as values.
	 * @throws APIError At least one object could not be fetched.
	 */
	public Map<ID,T> fetch(ID[] a_ids) throws APIError;
	
	/**
	 * Fetches a single object from the API.
	 * @param a_id The ID of the object to fetch.
	 * @return The object with the specified ID.
	 * @throws APIError The object could not be fetched.
	 */
	public T fetch(ID a_id) throws APIError;
}
