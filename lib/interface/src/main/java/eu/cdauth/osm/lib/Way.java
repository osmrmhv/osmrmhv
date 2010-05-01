/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package eu.cdauth.osm.lib;

import java.util.Date;

/**
 * A way is a versioned geographical item that consists of two or more nodes.
 * @author cdauth
 */
public interface Way extends GeographicalItem, VersionedItem
{
	/**
	 * Returns the IDs of the member nodes of this way.
	 * @return The IDs of the member nodes of this way.
	 * @throws APIError There was an error communicating with the API.
	 */
	public ID[] getMembers() throws APIError;

	/**
	 * Returns the nodes that were members of this way at the specified date.
	 * @param a_date The date to use or null to use the current time.
	 * @return The member nodes of the way.
	 * @throws APIError There was an error communicating with the API.
	 */
	public Node[] getMemberNodes(Date a_date) throws APIError;

	/**
	 * Returns the “centre” of this way (that is the average of the position of its nodes). If the node
	 * is a circle (such as a roundabout), this is useful to get its centre.
	 * @return The centre of this way or null if this way is not a closed circle.
	 * @throws APIError There was an error communicating with the API.
	 */
	public LonLat getRoundaboutCentre() throws APIError;
}
