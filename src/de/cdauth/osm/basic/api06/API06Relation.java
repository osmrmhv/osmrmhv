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

package de.cdauth.osm.basic.api06;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.GeographicalObject;
import de.cdauth.osm.basic.ID;
import de.cdauth.osm.basic.LonLat;
import de.cdauth.osm.basic.Node;
import de.cdauth.osm.basic.Relation;
import de.cdauth.osm.basic.RelationSegment;
import de.cdauth.osm.basic.Segment;
import de.cdauth.osm.basic.Way;

public class API06Relation extends API06GeographicalObject implements Relation
{
	protected API06Relation(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);
	}

	/**
	 * Returns an array of all members of this relation.
	 * @return
	 */
	
	public API06RelationMember[] getMembers()
	{
		NodeList members = getDOM().getElementsByTagName("member");
		API06RelationMember[] ret = new API06RelationMember[members.getLength()];
		for(int i=0; i<members.getLength(); i++)
			ret[i] = new API06RelationMember((Element) members.item(i), getAPI(), this);
		return ret;
	}
	
	/**
	 * Returns an array of all ways and nodes that are contained in this relation and all of its
	 * sub-relations. You may want to call downloadRecursive() first.
	 * @param a_date The date to use to fetch the members. Set to null to fetch the current member versions (which is a lot faster).
	 * @param a_members Set to null. Is filled with the result and passed along the recursive calls of this function.
	 * @param a_ignoreRelations Set to null. Is passed along the recursive calls to processing a relation twice and thus produce an infinite loop.
	 * @return A set of the members of this relation.
	 * @throws APIError 
	 */
	private HashSet<GeographicalObject> getMembersRecursive(Date a_date, HashSet<GeographicalObject> a_members, HashSet<ID> a_ignoreRelations) throws APIError
	{
		if(a_members == null)
			a_members = new HashSet<GeographicalObject>();
		if(a_ignoreRelations == null)
			a_ignoreRelations = new HashSet<ID>();
		a_ignoreRelations.add(getID());
		
		if(a_date == null)
			getAPI().getRelationFactory().downloadFull(getID());

		for(API06RelationMember it : getMembers())
		{
			Class<? extends GeographicalObject> type = it.getType();
			ID id = it.getReferenceID();
			if(type.equals(Way.class))
			{
				Way obj = (a_date == null ? getAPI().getWayFactory().fetch(id) : getAPI().getWayFactory().fetch(id, a_date));
				a_members.add(obj);
			}
			else if(type.equals(Node.class))
			{
				Node obj = (a_date == null ? getAPI().getNodeFactory().fetch(id) : getAPI().getNodeFactory().fetch(id, a_date));
				a_members.add(obj);
			}
			else if(type.equals(Relation.class) && !a_ignoreRelations.contains(id))
			{
				a_members.add(a_date == null ? getAPI().getRelationFactory().fetch(id) : getAPI().getRelationFactory().fetch(id, a_date));
				if(a_date == null)
					((API06Relation)getAPI().getRelationFactory().fetch(id)).getMembersRecursive(a_date, a_members, a_ignoreRelations);
				else
					((API06Relation)getAPI().getRelationFactory().fetch(id, a_date)).getMembersRecursive(a_date, a_members, a_ignoreRelations);
			}
		}
		return a_members;
	}
	
	@Override
	public GeographicalObject[] getMembersRecursive(Date a_date) throws APIError
	{
		return getMembersRecursive(a_date, null, null).toArray(new GeographicalObject[0]);
	}
	
	/**
	 * Returns an array of all ways that are contained in this relation and all of its sub-relations. You may want to call downloadRecursive() first.
	 * @return
	 * @throws APIError
	 */
	public Way[] getWaysRecursive(Date a_date) throws APIError
	{
		HashSet<GeographicalObject> members = getMembersRecursive(a_date, null, null);
		ArrayList<Way> ret = new ArrayList<Way>();
		for(GeographicalObject member : members)
		{
			if(member instanceof Way)
				ret.add((Way) member);
		}
		return ret.toArray(new Way[0]);
	}
	
	/**
	 * Returns an array of all nodes that are contained in this relation and all of its sub-relations. You may want to call downloadRecursive() first.
	 * @return
	 * @throws APIError 
	 */
	@Override
	public Node[] getNodesRecursive(Date a_date) throws APIError
	{
		HashSet<GeographicalObject> members = getMembersRecursive(a_date, null, null);
		ArrayList<Node> ret = new ArrayList<Node>();
		for(GeographicalObject member : members)
		{
			if(member instanceof Node)
				ret.add((Node) member);
		}
		return ret.toArray(new Node[0]);
	}
	
	public Relation[] getRelationsRecursive(Date a_date) throws APIError
	{
		HashSet<GeographicalObject> members = getMembersRecursive(a_date, null, null);
		ArrayList<Relation> ret = new ArrayList<Relation>();
		for(GeographicalObject member : members)
		{
			if(member instanceof Relation)
				ret.add((Relation) member);
		}
		return ret.toArray(new Relation[0]);
	}

	@Override
	public Segment[] getSegmentsRecursive(Date a_date) throws APIError
	{
		HashSet<Segment> ret = new HashSet<Segment>();

		Node[] nodes = getNodesRecursive(a_date);
		for(int i=0; i<nodes.length; i++)
			ret.add(new Segment(nodes[i], nodes[i]));
		
		Way[] ways = getWaysRecursive(a_date);
		for(int i=0; i<ways.length; i++)
		{
			Node[] members = ways[i].getMemberNodes(a_date);
			Node lastNode = null;
			for(int j=0; j<members.length; j++)
			{
				if(lastNode != null)
					ret.add(new Segment(lastNode, members[j]));
				lastNode = members[j];
			}
		}
		
		return ret.toArray(new Segment[0]);
	}
	
	/**
	 * Makes segments out of the ways of this relation and its sub-relations. The return value is an array of segments. The segments consist of a list of coordinates that are connected via ways. The relation structure is irrelevant for the segmentation; a segment ends where a way is connected to two or more ways where it is not connected to any way. 
	 * The segments are sorted from north to south or from west to east (depending in which direction the two most distant ends have the greater distance from each other). The sort algorithm sorts using the smaller distance of the two ends of a segment to the northern/western one of the the two most distant points.
	 * @throws APIError
	 */
	
	@SuppressWarnings("unchecked")
	public RelationSegment[] segmentate() throws APIError
	{
		List<Way> waysList = Arrays.asList(getWaysRecursive(null));
		
		// Make roundabout resolution table
		Hashtable<LonLat,LonLat> roundaboutReplacement = new Hashtable<LonLat,LonLat>();
		for(int i=0; i<waysList.size(); i++)
		{
			LonLat roundaboutCentre = waysList.get(i).getRoundaboutCentre();
			Node[] nodes = waysList.get(i).getMemberNodes(null);
			if(nodes.length <= 1)
				waysList.remove(i--);
			else if(roundaboutCentre != null)
			{
				for(Node it : nodes)
					roundaboutReplacement.put(it.getLonLat(), roundaboutCentre);
				waysList.remove(i--);
			}
		}
		
		Way[] ways = waysList.toArray(new Way[0]);
		waysList = null;
		
		// Get the first and last node of the ways
		LonLat[] waysEnds1 = new LonLat[ways.length];
		LonLat[] waysEnds2 = new LonLat[ways.length];
		LonLat i_lonlat;
		for(int i=0; i<ways.length; i++)
		{
			Node[] nodes = ways[i].getMemberNodes(null);
			i_lonlat = nodes[0].getLonLat();
			if(roundaboutReplacement.containsKey(i_lonlat))
				waysEnds1[i] = roundaboutReplacement.get(i_lonlat);
			else
				waysEnds1[i] = i_lonlat;
			i_lonlat = nodes[nodes.length-1].getLonLat();
			if(roundaboutReplacement.containsKey(i_lonlat))
				waysEnds2[i] = roundaboutReplacement.get(i_lonlat);
			else
				waysEnds2[i] = i_lonlat;
		}
		i_lonlat = null;
		
		// Look which ways are connected
		ArrayList<Integer> endsIndexes = new ArrayList<Integer>(); // Contains the indexes of all ways that are on one end of a segment (thus connected to more or less than 1 other way)
		ArrayList<Integer>[] waysConnections1 = new ArrayList[ways.length];
		ArrayList<Integer>[] waysConnections2 = new ArrayList[ways.length];
		for(int i=0; i<ways.length; i++)
		{
			waysConnections1[i] = new ArrayList<Integer>();
			waysConnections2[i] = new ArrayList<Integer>();
		}
		
		for(int i=0; i<ways.length; i++)
		{
			for(int j=i+1; j<ways.length; j++)
			{
				if(waysEnds1[i].equals(waysEnds1[j]))
				{
					waysConnections1[i].add(j);
					waysConnections1[j].add(i);
				}
				else if(waysEnds1[i].equals(waysEnds2[j]))
				{
					waysConnections1[i].add(j);
					waysConnections2[j].add(i);
				}
				else if(waysEnds2[i].equals(waysEnds1[j]))
				{
					waysConnections2[i].add(j);
					waysConnections1[j].add(i);
				}
				else if(waysEnds2[i].equals(waysEnds2[j]))
				{
					waysConnections2[i].add(j);
					waysConnections2[j].add(i);
				}
			}
			if(waysConnections1[i].size() != 1 || waysConnections2[i].size() != 1)
				endsIndexes.add(i);
		}
		
		waysEnds1 = null;
		waysEnds2 = null;
		
		ArrayList<ArrayList<Way>> segmentsWaysV = new ArrayList<ArrayList<Way>>();
		
		// Connect the ways and first create segments of ways (maybe later tags of the ways could be useful)
		for(int i=0; i<endsIndexes.size(); i++)
		{
			if(endsIndexes.get(i) == null)
				continue;
			int it = endsIndexes.get(i);
			int prevIt;

			ArrayList<Way> segment = new ArrayList<Way>();
			
			segment.add(ways[it]);
			ArrayList<Integer>[] connectionArray = (waysConnections1[it].size() != 1 ? waysConnections2 : waysConnections1);
			while(connectionArray[it].size() == 1)
			{
				prevIt = it;
				it = connectionArray[it].get(0);
				segment.add(ways[it]);
				connectionArray = (waysConnections1[it].size() > 0 && waysConnections1[it].get(0) == prevIt ? waysConnections2 : waysConnections1);
			}
			
			segmentsWaysV.add(segment);
			
			int indexOf = endsIndexes.indexOf(new Integer(it));
			if(indexOf > -1)
				endsIndexes.set(indexOf, null);
		}
		
		waysConnections1 = null;
		waysConnections2 = null;
		endsIndexes = null;
		
		ArrayList<Way>[] segmentsWays = segmentsWaysV.toArray(new ArrayList[0]);
		
		// Resolve segments into points
		ArrayList<ArrayList<LonLat>> segmentsNodesV = new ArrayList<ArrayList<LonLat>>();
		for(int i=0; i<segmentsWays.length; i++)
		{
			ArrayList<Way> segmentWays = segmentsWays[i];
			ArrayList<LonLat> segmentNodes = new ArrayList<LonLat>();
			Node lastEndNode = null;
			boolean lastWasRoundabout = false;
			for(int j=0; j<segmentWays.size(); j++)
			{
				Node[] nodes = segmentWays.get(j).getMemberNodes(null);
				boolean reverse = false;
				if(lastEndNode == null)
				{
					if(j+1 < segmentWays.size())
					{
						Node[] nextNodes = segmentWays.get(j+1).getMemberNodes(null);
						reverse = (nodes[0].equals(nextNodes[0]) || nodes[0].equals(nextNodes[nextNodes.length-1]));
					}
					if(roundaboutReplacement.containsKey(nodes[0].getLonLat()))
						segmentNodes.add(roundaboutReplacement.get(nodes[reverse ? nodes.length-1 : 0].getLonLat()));
				}
				else
					reverse = (nodes[nodes.length-1].equals(lastEndNode) || (lastWasRoundabout && roundaboutReplacement.get(lastEndNode.getLonLat()).equals(roundaboutReplacement.get(nodes[nodes.length-1].getLonLat()))));
				
				for(int k = (reverse ? nodes.length-1 : 0) + (lastEndNode == null || lastWasRoundabout ? 0 : (reverse ? -1 : 1)); (reverse) ? (k >= 0) : (k < nodes.length); k += (reverse ? -1 : 1))
					segmentNodes.add((lastEndNode = nodes[k]).getLonLat());
				if(lastWasRoundabout = roundaboutReplacement.containsKey(lastEndNode.getLonLat()))
					segmentNodes.add(roundaboutReplacement.get(lastEndNode.getLonLat()));
			}
			segmentsNodesV.add(segmentNodes);
		}
		
		segmentsWays = null;
		RelationSegment[] segmentsNodes = new RelationSegment[segmentsNodesV.size()];
		for(int i=0; i<segmentsNodesV.size(); i++)
			segmentsNodes[i] = new RelationSegment(segmentsNodesV.get(i).toArray(new LonLat[0]));
		segmentsNodesV = null;
		
		// Sort segments
		// Calculate the distance between all segment ends that aren’t connected to any other segment and find the greatest distance
		ArrayList<LonLat> endsCoordinatesV = new ArrayList<LonLat>();
		
		for(int i=0; i<segmentsNodes.length; i++)
		{
			boolean end1 = true;
			boolean end2 = true;
			for(int j=0; j<segmentsNodes.length; j++)
			{
				if(i == j) continue;
				
				if(end1 && (segmentsNodes[i].getEnd1().equals(segmentsNodes[j].getEnd1()) || segmentsNodes[i].getEnd1().equals(segmentsNodes[j].getEnd2())))
					end1 = false;
				if(end2 && (segmentsNodes[i].getEnd2().equals(segmentsNodes[j].getEnd1()) || segmentsNodes[i].getEnd2().equals(segmentsNodes[j].getEnd2())))
					end2 = false;
				if(end1 && end2)
					break;
			}
			if(end1)
				endsCoordinatesV.add(segmentsNodes[i].getEnd1());
			if(end2)
				endsCoordinatesV.add(segmentsNodes[i].getEnd2());
		}
		
		LonLat[] endsCoordinates = endsCoordinatesV.toArray(new LonLat[0]);
		endsCoordinatesV = null;
		LonLat[] greatestDistancePoints = new LonLat[2];
		double greatestDistance = -1;
		for(int i=0; i<endsCoordinates.length; i++)
		{
			for(int j=i+1; j<endsCoordinates.length; j++)
			{
				double distance = endsCoordinates[i].getDistance(endsCoordinates[j]);
				if(distance > greatestDistance)
				{
					greatestDistance = distance;
					greatestDistancePoints[0] = endsCoordinates[i];
					greatestDistancePoints[1] = endsCoordinates[j];
				}
			}
		}
		
		if(greatestDistance != -1)
		{
			double distanceLat = Math.abs(greatestDistancePoints[0].getLat()-greatestDistancePoints[1].getLat());
			double distanceLon = Math.abs(greatestDistancePoints[0].getLon()-greatestDistancePoints[1].getLon());
			LonLat referencePoint;
			if(distanceLat > distanceLon)
				referencePoint = greatestDistancePoints[greatestDistancePoints[0].getLat() < greatestDistancePoints[1].getLat() ? 0 : 1];
			else
				referencePoint = greatestDistancePoints[greatestDistancePoints[0].getLon() < greatestDistancePoints[1].getLon() ? 0 : 1];
			
			synchronized(RelationSegment.class)
			{
				RelationSegment.setSortingReference(referencePoint);
				Arrays.sort(segmentsNodes);
				RelationSegment.setSortingReference(null);
			}
		}
		
		return segmentsNodes;
	}
}