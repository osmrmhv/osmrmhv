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

package de.cdauth.osm.lib;

import java.util.Date;
import java.util.Map;

/**
 * Represents an OpenStreetMap changeset. A changeset contains information about the time it was created,
 * tags and the objects that were created, modified and deleted. These objects have versions and thus instances
 * of {@link VersionedItem}.
 *  
 * You can do very funny things in a changeset. You can create an object, modify it multiple times and then remove
 * it again, so that basically, you haven’t created nor modified nor removed anything in the changeset, because
 * afterwards everything is as it was.
 * Implementations of this type have to clean up such multiple entries considering the same object by doing the following things: 
 * 1. If an object was modified multiple times in one changeset, keep only the newest modification in the “modify” block
 * 2. If an object has been created and later modified in one changeset, move the newest modification to the “create” block
 * 3. If an object has been modified and later removed in one changeset, remove the part from the “modify” block
 * 4. If an object has been created and later removed in one changset, remove it from both the “create” and the “delete” part
 */

public interface Changeset extends Item
{
	/**
	 * An osmChange file knows these types of changes.
	 */
	public static enum ChangeType { create, modify, delete };

	/**
	 * Returns the date when this Changeset was created.
	 * @return The creation date of this Changeset.
	 */
	public Date getCreationDate();

	/**
	 * Returns the user that created this changeset.
	 * @return The user that created this changeset.
	 */
	public User getUser();
	
	/**
	 * Returns the created, the modified or the deleted objects of this changeset.
	 * @param a_type The {@link ChangeType} how the objects were changed.
	 * @return A list of the objects with the specified {@link ChangeType}.
	 * @throws APIError There was an error receiving the members.
	 */
	public VersionedItem[] getMemberObjects(ChangeType a_type) throws APIError;
	
	/**
	 * Fetches the previous version of all objects that were {@link ChangeType#modify modified} in this changeset.
	 * All modified objects obviously have to have a previous version. 
	 * @param a_onlyWithTagChanges If true, only objects will be returned whose tags have changed
	 * @return A hashtable with the new version of an object in the key and the old version in the value.
	 * @throws APIError There was an error fetching the previous versions of the objects.
	 */
	public Map<VersionedItem, VersionedItem> getPreviousVersions(boolean a_onlyWithTagChanges) throws APIError;
}
