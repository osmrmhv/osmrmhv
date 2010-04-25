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

/**
 * An object that can be modified by changeset and thus has a version history. Instances of this type do not know
 * if they are the current version. With some API implementations though, it makes a huge difference whether to
 * fetch a specific version of an object or the current version.
 * @author cdauth
 */
public interface VersionedItem extends Item
{
	/**
	 * Returns the time when this version of the object was created.
	 * @return The timestamp of the creation of this object.
	 */
	public Date getTimestamp();
	
	/**
	 * Returns the version number of this object. Versions start at 1 and are always increased by 1, gaps do not
	 * exist.
	 * @return The version number of this object.
	 */
	public Version getVersion();
	
	/**
	 * Returns the ID of the changeset in which this version of the object was created.
	 * @return The ID of the corresponding changeset.
	 */
	public ID getChangeset();
	
	/**
	 * Returns whether this version of the object is known to be the current one. This is determined by the
	 * corresponding {@link VersionedItemFactory} when the object is created.
	 * @return true if this version of the object is the current one for sure.
	 */
	public boolean isCurrent();
}
