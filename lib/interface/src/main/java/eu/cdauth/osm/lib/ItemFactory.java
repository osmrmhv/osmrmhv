/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package eu.cdauth.osm.lib;

import java.util.Map;
import java.util.Set;

/**
 * The methods of this type are common to all factories that fetch {@link Item}s using an {@link API} implementation.
 * The sub-types of this interface define which type of object is to be referenced by the IDs used in these methods.
 * @author cdauth
 */
public interface ItemFactory<T extends Item>
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

	/**
	 * Searches for objects having the specified tags.
	 * @param a_tags The tags the objects have to have.
	 * @return The objects containing the tags.
	 * @throws APIError The search could not be performed.
	 */
	public Set<T> search(Map<String,String> a_tags) throws APIError;
}
