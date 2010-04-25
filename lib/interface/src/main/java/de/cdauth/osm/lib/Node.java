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
 * A node in OSM is a versioned object that consists of an ID, a position and tags.
 * @author cdauth
 */
public interface Node extends GeographicalItem, VersionedItem
{
	/**
	 * Returns a LonLat object for the coordinates of this node.
	 * @return The coordinates of this node.
	 */
	public LonLat getLonLat();
	
	/**
	 * Returns all ways that currently contain the <strong>current</strong> version of this node.
	 * @return An array of ways that currently contain this node.
	 * @throws APIError The ways could not be fetched.
	 */
	public Way[] getContainingWays() throws APIError;
}
