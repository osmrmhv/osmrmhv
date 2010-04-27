/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package eu.cdauth.osm.lib;

import java.io.Serializable;

/**
 * Represents a member of a {@link Relation} in the OSM database. Relation members are references to
 * {@link GeographicalItem}s and have a role that is an arbitrary text. Relation members can be
 * contained multiple times in the same relation, even with the same role.
 * 
 * RelationMember objects are created by their corresponding {@link Relation} object. None of the
 * method throw an {@link APIError} because all information is filled in upon creation.
 * 
 * @author cdauth
 */
public interface RelationMember extends Serializable
{
	/**
	 * Returns the corresponding {@link Relation} that this member belongs to.
	 * @return The relation that contains this member.
	 */
	public Relation getRelation();
	
	/**
	 * Returns the type of the {@link GeographicalItem} that this member refers to.
	 * @return The class representing the type of {@link GeographicalItem} of this member.
	 */
	public Class<? extends GeographicalItem> getType();
	
	/**
	 * Returns the ID of the {@link GeographicalItem} that this member refers to.
	 * @return The ID this member refers to.
	 */
	public ID getReferenceID();
	
	/**
	 * Returns the role of this relation member.
	 * @return The role of this relation member, which might be an empty string.
	 */
	public String getRole();
}
