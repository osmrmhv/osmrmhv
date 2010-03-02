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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.GeographicalObject;
import de.cdauth.osm.basic.ID;
import de.cdauth.osm.basic.LonLat;
import de.cdauth.osm.basic.Node;
import de.cdauth.osm.basic.ObjectCache;
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
	 * Ensures that all members of the relation are downloaded and cached. This saves a lot of time when accessing them with fetch(), as fetch() makes an API call for each uncached item whereas this method can download all members at once.
	 * @param a_id
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws APIError 
	 * @throws IOException 
	 */
	
	public static void downloadFull(ID a_id) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		boolean downloadNecessary = true;
		if(getCache().getCurrent(a_id) != null)
		{
			downloadNecessary = false;
			for(API06RelationMember it : API06Relation.fetch(a_id).getMembers())
			{
				String type = it.getDOM().getAttribute("type");
				String id = it.getDOM().getAttribute("ref");
				boolean isCached = true;
				if(type.equals("node"))
					isCached = (API06Node.getCache().getCurrent(id) != null);
				else if(type.equals("way"))
					isCached = (API06Way.getCache().getCurrent(id) != null);
				else if(type.equals("relation"))
					isCached = (API06Relation.getCache().getCurrent(id) != null);
				if(!isCached)
				{
					downloadNecessary = true;
					break;
				}
			}
		}
		
		if(downloadNecessary)
		{
			API06Object[] fetched = API06API.get("/relation/"+a_id+"/full");
			for(API06Object object : fetched)
			{
				String type = object.getDOM().getTagName();
				if(type.equals("node"))
					API06Node.getCache().cacheCurrent((API06Node) object);
				else if(type.equals("way"))
					API06Way.getCache().cacheCurrent((API06Way) object);
				else if(type.equals("relation"))
					API06Relation.getCache().cacheCurrent((API06Relation) object);
			}
		}
	}
	
	/**
	 * Downloads all members of this relation and its sub-relations. Acts like a full download, but recursive. The number of API requests is optimised for the case of many sub-relations: as many relations as possible are downloaded in each API request, all ways and all nodes are downloaded in one additional request for both.
	 * As the OSM API seems to be optimised for full fetches and takes quite a time to respond to the Way and Node request, this function may save requests but not time (at least with the current speed of the API).
	 * @param a_id
	 * @throws APIError
	 */
	
	public static void downloadRecursive(ID a_id) throws APIError
	{
		HashSet<String> checkRelations = new HashSet<String>();
		HashSet<String> downloadRelations = new HashSet<String>();
		HashSet<String> containedRelations = new HashSet<String>();
		
		HashSet<String> downloadWays = new HashSet<String>();
		HashSet<String> downloadNodes = new HashSet<String>();

		if(getCache().getCurrent(a_id) == null)
			downloadFull(a_id);
		
		checkRelations.add(a_id);
		while(checkRelations.size() > 0)
		{
			for(String id : checkRelations)
			{
				for(API06RelationMember member : API06Relation.fetch(id).getMembers())
				{
					if(member.getDOM().getAttribute("type").equals("relation") && !containedRelations.contains(member.getDOM().getAttribute("ref")))
						downloadRelations.add(member.getDOM().getAttribute("ref"));
					else if(member.getDOM().getAttribute("type").equals("way"))
						downloadWays.add(member.getDOM().getAttribute("ref"));
					else if(member.getDOM().getAttribute("type").equals("node"))
						downloadNodes.add(member.getDOM().getAttribute("ref"));
				}
			}
			if(downloadRelations.size() == 1)
			{
				String one = downloadRelations.iterator().next();
				if(getCache().getCurrent(one) == null)
					API06Relation.downloadFull(one);
			}
			else if(downloadRelations.size() > 1)
				API06Relation.fetch(downloadRelations.toArray(new String[0]));
			
			containedRelations.addAll(downloadRelations);
			checkRelations = downloadRelations;
			downloadRelations = new HashSet<String>();
		}
		
		API06Way.fetch(downloadWays.toArray(new String[0]));
		
		for(String id : downloadWays)
		{
			for(String nodeID : API06Way.fetch(id).getMembers())
				downloadNodes.add(nodeID);
		}
		
		API06Node.fetch(downloadNodes.toArray(new String[0]));
	}
	
	/**
	 * Returns an array of all ways and nodes that are contained in this relation and all of its
	 * sub-relations. You may want to call downloadRecursive() first.
	 * @param a_ignore_relations
	 * @return
	 * @throws APIError 
	 */
	private HashSet<GeographicalObject> getMembersRecursive(ArrayList<ID> a_ignoreRelations, Date a_date) throws APIError
	{
		try
		{
			a_ignoreRelations.add(getID());
			
			if(a_date == null)
				downloadFull(getID());
	
			HashSet<GeographicalObject> ret = new HashSet<GeographicalObject>();
			for(API06RelationMember it : getMembers())
			{
				String type = it.getDOM().getAttribute("type");
				ID id = new ID(it.getDOM().getAttribute("ref"));
				if(type.equals("way"))
				{
					Way obj = (a_date == null ? getAPI().getWayFactory().fetch(id) : getAPI().getWayFactory().fetch(id, a_date));
					if(!ret.add(obj))
						System.out.println("Double");
				}
				else if(type.equals("node"))
				{
					Node obj = (a_date == null ? getAPI().getNodeFactory().fetch(id) : getAPI().getNodeFactory().fetch(id, a_date));
					if(!ret.add(obj))
						System.out.println("Double");
				}
				else if(type.equals("relation") && !a_ignoreRelations.contains(id))
				{
					ret.add(a_date == null ? getAPI().getRelationFactory().fetch(id) : getAPI().getRelationFactory().fetch(id, a_date));
					ret.addAll(a_date == null ? ((API06Relation)getAPI().getRelationFactory().fetch(id)).getMembersRecursive(a_ignoreRelations, a_date) : ((API06Relation)getAPI().getRelationFactory().fetch(id, a_date)).getMembersRecursive(a_ignoreRelations, a_date));
				}
			}
			return ret;
		}
		catch(IOException e)
		{
			throw new APIError("Could not download members.", e);
		}
		catch(SAXException e)
		{
			throw new APIError("Could not download members.", e);
		}
		catch(ParserConfigurationException e)
		{
			throw new APIError("Could not download members.", e);
		}
	}
	
	@Override
	public GeographicalObject[] getMembersRecursive(Date a_date) throws APIError
	{
		return getMembersRecursive(new ArrayList<ID>(), a_date).toArray(new GeographicalObject[0]);
	}
	
	/**
	 * Returns an array of all ways that are contained in this relation and all of its sub-relations. You may want to call downloadRecursive() first.
	 * @return
	 * @throws ParseException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws APIError 
	 * @throws IOException
	 */
	public Way[] getWaysRecursive(Date a_date) throws APIError
	{
		HashSet<GeographicalObject> members = getMembersRecursive(new ArrayList<ID>(), a_date);
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
		HashSet<GeographicalObject> members = getMembersRecursive(new ArrayList<ID>(), a_date);
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
		HashSet<GeographicalObject> members = getMembersRecursive(new ArrayList<ID>(), a_date);
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
	 * @throws IOException
	 * @throws APIError
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws ParseException 
	 */
	
	@SuppressWarnings("unchecked")
	public RelationSegment[] segmentate() throws APIError
	{
		ArrayList<API06Way> waysList = new ArrayList<API06Way>(Arrays.asList(getWaysRecursive()));
		
		// Make roundabout resolution table
		Hashtable<LonLat,LonLat> roundaboutReplacement = new Hashtable<LonLat,LonLat>();
		for(int i=0; i<waysList.size(); i++)
		{
			LonLat roundaboutCentre = waysList.get(i).getRoundaboutCentre();
			API06Node[] nodes = waysList.get(i).getMemberNodes();
			if(nodes.length <= 1)
				waysList.remove(i--);
			else if(roundaboutCentre != null)
			{
				for(API06Node it : nodes)
					roundaboutReplacement.put(it.getLonLat(), roundaboutCentre);
				waysList.remove(i--);
			}
		}
		
		API06Way[] ways = waysList.toArray(new API06Way[0]);
		waysList = null;
		
		// Get the first and last node of the ways
		LonLat[] waysEnds1 = new LonLat[ways.length];
		LonLat[] waysEnds2 = new LonLat[ways.length];
		LonLat i_lonlat;
		for(int i=0; i<ways.length; i++)
		{
			API06Node[] nodes = ways[i].getMemberNodes();
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
		
		ArrayList<ArrayList<API06Way>> segmentsWaysV = new ArrayList<ArrayList<API06Way>>();
		
		// Connect the ways and first create segments of ways (maybe later tags of the ways could be useful)
		for(int i=0; i<endsIndexes.size(); i++)
		{
			if(endsIndexes.get(i) == null)
				continue;
			int it = endsIndexes.get(i);
			int prevIt;

			ArrayList<API06Way> segment = new ArrayList<API06Way>();
			
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
		
		ArrayList<API06Way>[] segmentsWays = segmentsWaysV.toArray(new ArrayList[0]);
		
		// Resolve segments into points
		ArrayList<ArrayList<LonLat>> segmentsNodesV = new ArrayList<ArrayList<LonLat>>();
		for(int i=0; i<segmentsWays.length; i++)
		{
			ArrayList<API06Way> segmentWays = segmentsWays[i];
			ArrayList<LonLat> segmentNodes = new ArrayList<LonLat>();
			API06Node lastEndNode = null;
			boolean lastWasRoundabout = false;
			for(int j=0; j<segmentWays.size(); j++)
			{
				API06Node[] nodes = segmentWays.get(j).getMemberNodes();
				boolean reverse = false;
				if(lastEndNode == null)
				{
					if(j+1 < segmentWays.size())
					{
						API06Node[] nextNodes = segmentWays.get(j+1).getMemberNodes();
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
			
			synchronized(RelationSegment.sm_sortingReferenceSynchronized)
			{
				RelationSegment.sm_sortingReference = referencePoint;
				Arrays.sort(segmentsNodes);
				RelationSegment.sm_sortingReference = null;
			}
		}
		
		return segmentsNodes;
	}
	
	public static Hashtable<Segment,API06Changeset> blame(ID a_id) throws IOException, SAXException, ParserConfigurationException, APIError, ParseException
	{
		API06Relation currentRelation = API06Relation.getHistory(a_id).lastEntry().getValue();
		
		Date currentDate = null;
		Date nextDate = null;
		String currentChangeset = null;
		String nextChangeset = null;
		
		HashSet<Segment> currentSegments = currentRelation.getSegmentsRecursive(null);
		Hashtable<Segment,String> blame = new Hashtable<Segment,String>();
		
		while(true)
		{
			API06Relation mainRelation = null;
			API06Relation mainRelationOlder = null;

			if(nextDate == null)
			{ // First run
				mainRelation = currentRelation;
				mainRelationOlder = currentRelation;
			}
			else
			{
				currentDate = nextDate;
				nextDate = null;
				currentChangeset = nextChangeset;
				nextChangeset = null;
				mainRelation = API06Relation.fetch(a_id, currentDate);
				mainRelationOlder = API06Relation.fetch(a_id, new Date(currentDate.getTime()-1000));
			}
			
			if(mainRelation == null)
				break;
			
			if(mainRelationOlder != null)
			{
				nextDate = mainRelationOlder.getTimestamp();
				nextChangeset = mainRelationOlder.getDOM().getAttribute("changeset");
			}
			
			API06Node[] nodeMembers = mainRelation.getNodesRecursive(currentDate);
			for(int i=0; i<nodeMembers.length; i++)
			{
				TreeMap<Long,API06Node> nodeHistory = API06Node.getHistory(nodeMembers[i].getDOM().getAttribute("id"));
				for(API06Node node : nodeHistory.values())
				{
					Date nodeDate = getDateFormat().parse(node.getDOM().getAttribute("timestamp"));
					if((currentDate == null || nodeDate.compareTo(currentDate) < 0) && (nextDate == null || nodeDate.compareTo(nextDate) > 0))
					{
						nextDate = nodeDate;
						nextChangeset = node.getDOM().getAttribute("changeset");
					}
				}
			}
			
			API06Way[] wayMembers = mainRelation.getWaysRecursive(currentDate);
			for(int i=0; i<wayMembers.length; i++)
			{
				TreeMap<Long,API06Way> wayHistory = API06Way.getHistory(wayMembers[i].getDOM().getAttribute("id"));
				for(API06Way way : wayHistory.values())
				{
					Date wayDate = getDateFormat().parse(way.getDOM().getAttribute("timestamp"));
					if((currentDate == null || wayDate.compareTo(currentDate) < 0) && (nextDate == null || wayDate.compareTo(nextDate) > 0))
					{
						nextDate = wayDate;
						nextChangeset = way.getDOM().getAttribute("changeset");
					}
				}
			}
			
			API06Relation[] relationMembers = mainRelation.getRelationsRecursive(currentDate);
			for(int i=0; i<relationMembers.length; i++)
			{
				TreeMap<Long,API06Relation> relationHistory = API06Relation.getHistory(relationMembers[i].getDOM().getAttribute("id"));
				for(API06Relation relation : relationHistory.values())
				{
					Date relationDate = getDateFormat().parse(relation.getDOM().getAttribute("timestamp"));
					if((currentDate == null || relationDate.compareTo(currentDate) < 0) && (nextDate == null || relationDate.compareTo(nextDate) > 0))
					{
						nextDate = relationDate;
						nextChangeset = relation.getDOM().getAttribute("changeset");
					}
				}
			}
			
			HashSet<Segment> segments = mainRelation.getSegmentsRecursive(currentDate);
			segments.retainAll(currentSegments);

			if(segments.size() == 0)
				break;
			
			if(currentChangeset != null)
			{
				for(Segment segment : segments)
					blame.put(segment, currentChangeset);
			}
		}
		
		Hashtable<Segment,API06Changeset> ret = new Hashtable<Segment,API06Changeset>();
		for(Map.Entry<Segment,String> e : blame.entrySet())
			ret.put(e.getKey(), API06Changeset.fetch(e.getValue()));
		
		return ret;
	}
}