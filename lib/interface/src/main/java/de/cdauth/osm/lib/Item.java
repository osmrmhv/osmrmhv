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

import java.util.Map;

/**
 * An object in the OSM database that has an ID and tags.
 * @author cdauth
 */
public interface Item extends Comparable<Item>
{
	/**
	 * Get the ID of this object.
	 * @return The ID of this object.
	 */
	public ID getID();
	
	/**
	 * Get the value of a specific tag of this object.
	 * @param a_tagName The name/key of the tag.
	 * @return The value of the tag or an empty string if it is not set.
	 */
	public String getTag(String a_tagName);
	
	/**
	 * Returns a Map with all the tags of this object.
	 * @return A Map with all the tags of this object, an empty map if there are no tags.
	 */
	public Map<String,String> getTags();
}
