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

/**
 * Implementations of this interface provide access to OSM data. Classes implementing this interface are the only
 * ones who you have to instantiate directly, everywhere else use the abstract interfaces.
 * @author cdauth
 */
public interface API
{
	/**
	 * Returns a {@link RelationFactory} implementation.
	 * @return A {@link RelationFactory} to fetch {@link Relation} objects.
	 */
	public RelationFactory getRelationFactory();
	
	/**
	 * Returns a {@link NodeFactory} implementation.
	 * @return A {@link NodeFactory} to fetch {@link Node} objects.
	 */
	public NodeFactory getNodeFactory();
	
	/**
	 * Returns a {@link WayFactory} implementation.
	 * @return A {@link WayFactory} to fetch {@link Way} objects.
	 */
	public WayFactory getWayFactory();
	
	/**
	 * Returns a {@link ChangesetFactory} implementation.
	 * @return A {@link ChangesetFactory} to fetch {@link Changeset} objects.
	 */
	public ChangesetFactory getChangesetFactory();
	
	/**
	 * Returns all geographical objects that exist within the given bounding box.
	 * @param a_boundingBox The bounding box where the objects shall be.
	 * @return A list of the geographical elements inside the bounding box.
	 * @throws APIError An error has occurred, perhaps the API could not process such a large bounding box.
	 */
	public GeographicalObject[] fetchBoundingBox(BoundingBox a_boundingBox) throws APIError;
}
