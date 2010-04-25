package de.cdauth.osm.lib;

/**
 * A geographical object is an object that actually exists somewhere on the map, so it consists of parts that have
 * coordinates. Currently these are nodes, ways and relations.
 * @author cdauth
 */
public interface GeographicalItem extends Item
{
	/**
	 * Returns all relations that the <strong>current</strong> version of this object is currently contained in.
	 * Relations can only contain geographical objects.
	 * @return The relations that contain this object.
	 * @throws APIError There was an error fetching the relations.
	 */
	public Relation[] getContainingRelations() throws APIError;
}
