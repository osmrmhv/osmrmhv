/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package eu.cdauth.osm.lib;

import java.util.Date;

/**
 * A relation in OSM can contain nodes, ways and other relations. An object contained in a relation has additional
 * relation-specific properties (its position in the list of members and a role). An object can be contained in a
 * relation multiple times, but these properties are different for all relation members.
 * @author cdauth
 */
public interface Relation extends GeographicalItem, VersionedItem
{
	/**
	 * Gets all objects that are contained in this relation. This list can be empty. The elements are ordered.
	 * An object can be contained in a relation multiple times, this would be represented by different
	 * {@link RelationMember} objects that refer ({@link RelationMember#getReferenceID()}, {@link RelationMember#getType()})
	 * to the same object.
	 * @return An ordered list of the members of this relation.
	 * @throws APIError The members could not be fetched.
	 */
	public RelationMember[] getMembers() throws APIError;
	
	/**
	 * Returns a list of all relations, ways and nodes that this relation or any of its sub-relations contains.
	 * This list contains this relation object itself only if it is member of itself or of any of its sub-relations.
	 * Every object is unique in the returned list, objects being member multiple times or in multiple sub-relations
	 * are only added once. Thus circular relation memberships will not break the functionality of this method.
	 * @param a_date Use the version of this relation at a specified date, null to use the current version.
	 * @return A list of all members and sub-members of this relation.
	 * @throws APIError There was an error communicating with the API.
	 */
	public GeographicalItem[] getMembersRecursive(Date a_date) throws APIError;

	/**
	 * Like {@link #getMembersRecursive(Date)}, but returns only {@link Node}s.
	 * @see #getMembersRecursive(Date)
	 */
	public Node[] getNodesRecursive(Date a_date) throws APIError;

	/**
	 * Like {@link #getMembersRecursive(Date)}, but returns only {@link Way}s.
	 * @see #getMembersRecursive(Date)
	 */
	public Way[] getWaysRecursive(Date a_date) throws APIError;

	/**
	 * Like {@link #getMembersRecursive(Date)}, but returns only {@link Relation}s.
	 * @see #getMembersRecursive(Date)
	 */
	public Relation[] getRelationsRecursive(Date a_date) throws APIError;

	/**
	 * Returns all segments that this relation and all its sub-relations contained at the given date.
	 * Actually returns the ways from {@link #getWaysRecursive} converted to segments and the nodes
	 * from {@link #getNodesRecursive} (as segments with two identical points).
	 * @param a_date The date to use or null for the current version of the relation.
	 * @return A list of all segments contained in the relation.
	 * @throws APIError There was an error communicating with the API.
	 */
	public Segment[] getSegmentsRecursive(Date a_date) throws APIError;
}
