/*
	This file is part of the OSM Route Manager.

	OSM Route Manager is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	OSM Route Manager is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.

	You should have received a copy of the GNU Affero General Public License
	along with this software. If not, see <http://www.gnu.org/licenses/>.

	Copyright © 2010 Candid Dauth
*/

package eu.cdauth.osm.web.osmrm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import eu.cdauth.osm.lib.APIError;
import eu.cdauth.osm.lib.LonLat;
import eu.cdauth.osm.lib.Node;
import eu.cdauth.osm.lib.Relation;
import eu.cdauth.osm.lib.Way;

public class RouteManager
{
	private static Logger sm_logger = Logger.getLogger(RouteManager.class.getName());
	
	/**
	 * Makes segments out of the ways of this relation and its sub-relations. The return value is an array of segments. The segments consist of a list of coordinates that are connected via ways. The relation structure is irrelevant for the segmentation; a segment ends where a way is connected to two or more ways where it is not connected to any way. 
	 * The segments are sorted from north to south or from west to east (depending in which direction the two most distant ends have the greater distance from each other). The sort algorithm sorts using the smaller distance of the two ends of a segment to the northern/western one of the the two most distant points.
	 * @param a_relation The relation to analyse.
	 * @return The segments of this relation.
	 * @throws APIError There was an error talking to the API
	 */
	
	public static RelationSegment[] segmentate(Relation a_relation) throws APIError
	{
		long startTime = System.currentTimeMillis();

		ArrayList<Way> waysList = new ArrayList<Way>();
		waysList.addAll(Arrays.asList(a_relation.getWaysRecursive(null)));

		if(sm_logger.isLoggable(Level.INFO))
		{
			sm_logger.info("Took "+(System.currentTimeMillis()-startTime)+" ms to fetch ways.");
			startTime = System.currentTimeMillis();
		}
		
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

		if(sm_logger.isLoggable(Level.FINE))
		{
			sm_logger.fine("Took "+(System.currentTimeMillis()-startTime)+" ms to analyse roundabouts.");
			startTime = System.currentTimeMillis();
		}
		
		Way[] ways = waysList.toArray(new Way[waysList.size()]);
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

		if(sm_logger.isLoggable(Level.FINE))
		{
			sm_logger.fine("Took "+(System.currentTimeMillis()-startTime)+" ms to get way ends.");
			startTime = System.currentTimeMillis();
		}
		
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

		if(endsIndexes.isEmpty())
		{ // If the relation is a closed circle, break it up somewhere
			endsIndexes.add(0);
			int other = waysConnections1[0].get(0);
			waysConnections1[0].clear();
			if(other != 0)
			{
				endsIndexes.add(other);
				if(waysConnections1[other].get(0) == 0)
					waysConnections1[other].clear();
				else
					waysConnections2[other].clear();
			}
		}

		if(sm_logger.isLoggable(Level.FINE))
		{
			sm_logger.fine("Took "+(System.currentTimeMillis()-startTime)+" ms to get way connections.");
			startTime = System.currentTimeMillis();
		}
		
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

		if(sm_logger.isLoggable(Level.FINE))
		{
			sm_logger.fine("Took "+(System.currentTimeMillis()-startTime)+" ms to connect ways.");
			startTime = System.currentTimeMillis();
		}
		
		ArrayList<Way>[] segmentsWays = segmentsWaysV.toArray(new ArrayList[segmentsWaysV.size()]);
		
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
		{
			List<LonLat> nodes = segmentsNodesV.get(i);
			segmentsNodes[i] = new RelationSegment(nodes.toArray(new LonLat[nodes.size()]));
		}
		segmentsNodesV = null;

		if(sm_logger.isLoggable(Level.FINE))
		{
			sm_logger.fine("Took "+(System.currentTimeMillis()-startTime)+" ms to convert ways to segments.");
			startTime = System.currentTimeMillis();
		}
		
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
		
		LonLat[] endsCoordinates = endsCoordinatesV.toArray(new LonLat[endsCoordinatesV.size()]);
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

		if(sm_logger.isLoggable(Level.FINE))
		{
			sm_logger.fine("Took "+(System.currentTimeMillis()-startTime)+" ms to sort segments.");
			startTime = System.currentTimeMillis();
		}
		else if(sm_logger.isLoggable(Level.INFO))
			sm_logger.info("Took "+(System.currentTimeMillis()-startTime)+" ms to segmentate.");
		
		return segmentsNodes;
	}
}
