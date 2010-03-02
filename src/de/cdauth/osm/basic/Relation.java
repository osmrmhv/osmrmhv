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

package de.cdauth.osm.basic;

import java.util.Date;

public interface Relation extends GeographicalObject,VersionedObject
{
	public RelationMember<GeographicalObject>[] getMembers() throws APIError;
	
	public GeographicalObject[] getMembersRecursive(Date a_date) throws APIError;
	
	public Node[] getNodesRecursive(Date a_date) throws APIError;
	
	public Way[] getWaysRecursive(Date a_date) throws APIError;
	
	public Relation[] getRelationsRecursive(Date a_date) throws APIError;
	
	public Segment[] getSegmentsRecursive(Date a_date) throws APIError;
}
