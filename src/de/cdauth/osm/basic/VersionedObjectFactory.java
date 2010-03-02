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

import java.util.Date;
import java.util.NavigableMap;

public interface VersionedObjectFactory<T extends VersionedObject> extends ObjectFactory<T>
{
	/**
	 * Fetches the version of an object that was the current one at the given point of time.
	 * @param <T>
	 * @param a_id
	 * @param a_date
	 * @return null if the element did not exist at the given point of time 
	 * @throws APIError
	 */
	public T fetch(ID a_id, Date a_date) throws APIError;
	
	/**
	 * Fetches the version of an object that has been changed in a changeset with a number smaller than that of a_changeset.
	 * @param <T>
	 * @param a_id
	 * @param a_changeset
	 * @return null if the element did not exist before this changeset
	 * @throws APIError
	 */
	public T fetch(ID a_id, Changeset a_changeset) throws APIError;
	
	public T fetch(ID a_id, Version a_version) throws APIError;

	/**
	 * Returns a TreeMap of all versions of the element. The versions are ordered from the oldest to the newest. The indexes of the TreeMap match the version number.
	 * @param <T>
	 * @param a_id
	 * @return
	 * @throws APIError  
	 */
	public NavigableMap<Version,T> fetchHistory(ID a_id) throws APIError;
}
