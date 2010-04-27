/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package de.cdauth.osm.lib;

import java.util.Date;
import java.util.NavigableMap;

public interface VersionedItemFactory<T extends VersionedItem> extends ItemFactory<T>
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
	
	/**
	 * Fetches the given version of an object.
	 * @param a_id
	 * @param a_version
	 * @return
	 * @throws APIError
	 */
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
