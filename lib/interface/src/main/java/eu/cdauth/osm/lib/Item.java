/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package eu.cdauth.osm.lib;

import java.io.Serializable;
import java.util.Map;

/**
 * An object in the OSM database that has an ID and tags.
 * @author cdauth
 */
public interface Item extends Comparable<Item>, Serializable
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
