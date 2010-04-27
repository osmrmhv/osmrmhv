/*
	This file is part of the OSM History Viewer.

	OSM History Viewer is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	OSM History Viewer is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.

	You should have received a copy of the GNU Affero General Public License
	along with this software. If not, see <http://www.gnu.org/licenses/>.

	Copyright © 2010 Candid Dauth
*/

package eu.cdauth.osm.web.osmhv;

import eu.cdauth.osm.lib.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * @author cdauth
 */

public class HistoryViewer
{
	public static class Changes
	{
		public final Segment[] removed;
		public final Segment[] created;
		public final Segment[] unchanged;

		protected Changes(Segment[] a_removed, Segment[] a_created, Segment[] a_unchanged)
		{
			removed = a_removed;
			created = a_created;
			unchanged = a_unchanged;
		}
	}

	public Map<Segment,Changeset> blame(API a_api, ID a_id) throws APIError
	{
		NodeFactory nodeFactory = a_api.getNodeFactory();
		WayFactory wayFactory = a_api.getWayFactory();
		RelationFactory relationFactory = a_api.getRelationFactory();
		ChangesetFactory changesetFactory = a_api.getChangesetFactory();

		Relation currentRelation = relationFactory.fetchHistory(a_id).lastEntry().getValue();

		Date currentDate = null;
		Date nextDate = null;
		ID currentChangeset = null;
		ID nextChangeset = null;

		List<Segment> currentSegments = Arrays.asList(currentRelation.getSegmentsRecursive(null));
		Hashtable<Segment,ID> blame = new Hashtable<Segment,ID>();

		while(true)
		{
			Relation mainRelation = null;
			Relation mainRelationOlder = null;

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
				mainRelation = relationFactory.fetch(a_id, currentDate);
				mainRelationOlder = relationFactory.fetch(a_id, new Date(currentDate.getTime()-1000));
			}

			if(mainRelation == null)
				break;

			if(mainRelationOlder != null)
			{
				nextDate = mainRelationOlder.getTimestamp();
				nextChangeset = mainRelationOlder.getChangeset();
			}

			Node[] nodeMembers = mainRelation.getNodesRecursive(currentDate);
			for(int i=0; i<nodeMembers.length; i++)
			{
				NavigableMap<Version,Node> nodeHistory = nodeFactory.fetchHistory(nodeMembers[i].getID());
				for(Node node : nodeHistory.values())
				{
					Date nodeDate = node.getTimestamp();
					if((currentDate == null || nodeDate.compareTo(currentDate) < 0) && (nextDate == null || nodeDate.compareTo(nextDate) > 0))
					{
						nextDate = nodeDate;
						nextChangeset = node.getChangeset();
					}
				}
			}

			Way[] wayMembers = mainRelation.getWaysRecursive(currentDate);
			for(int i=0; i<wayMembers.length; i++)
			{
				NavigableMap<Version,Way> wayHistory = wayFactory.fetchHistory(wayMembers[i].getID());
				for(Way way : wayHistory.values())
				{
					Date wayDate = way.getTimestamp();
					if((currentDate == null || wayDate.compareTo(currentDate) < 0) && (nextDate == null || wayDate.compareTo(nextDate) > 0))
					{
						nextDate = wayDate;
						nextChangeset = way.getChangeset();
					}
				}
			}

			Relation[] relationMembers = mainRelation.getRelationsRecursive(currentDate);
			for(int i=0; i<relationMembers.length; i++)
			{
				NavigableMap<Version,Relation> relationHistory = relationFactory.fetchHistory(relationMembers[i].getID());
				for(Relation relation : relationHistory.values())
				{
					Date relationDate = relation.getTimestamp();
					if((currentDate == null || relationDate.compareTo(currentDate) < 0) && (nextDate == null || relationDate.compareTo(nextDate) > 0))
					{
						nextDate = relationDate;
						nextChangeset = relation.getChangeset();
					}
				}
			}

			List<Segment> segments = Arrays.asList(mainRelation.getSegmentsRecursive(currentDate));
			segments.retainAll(currentSegments);

			if(segments.size() == 0)
				break;

			if(currentChangeset != null)
			{
				for(Segment segment : segments)
					blame.put(segment, currentChangeset);
			}
		}

		Map<ID,Changeset> changesets = changesetFactory.fetch(blame.values().toArray(new ID[0]));
		Hashtable<Segment,Changeset> ret = new Hashtable<Segment,Changeset>();
		for(Map.Entry<Segment,ID> e : blame.entrySet())
			ret.put(e.getKey(), changesets.get(e.getValue()));

		return ret;
	}

	/**
	 * Resolves all ways and nodes that were changed in this changeset to segments
	 * (connections of two nodes). Then returns three lists of segments:
	 * 1. Those that have been removed (which is the case even if one of their nodes has been moved)
	 * 2. Those that have been created
	 * 3. Those that haven’t been changed but have somehow been affected by the changeset,
	 *   be it by changing their tags or by changing another part of the way they belong to.
	 * Nodes that have been changed will be represented by a Segment consisting of two
	 * equal Nodes.
	 * @return An array of three Segment arrays. The first one is the removed segments,
	 * the second one the created and the third one the unchanged ones.
	 * @throws java.io.IOException
	 * @throws org.xml.sax.SAXException
	 * @throws javax.xml.parsers.ParserConfigurationException
	 * @throws APIError
	 * @throws java.text.ParseException
	 */

	public static Changes getNodeChanges(API a_api, Changeset a_changeset) throws IOException, SAXException, ParserConfigurationException, APIError, ParseException
	{
		Map<VersionedItem, VersionedItem> old = a_changeset.getPreviousVersions(false);

		Hashtable<ID,Node> nodesRemoved = new Hashtable<ID,Node>(); // All removed nodes and the old versions of all moved nodes
		Hashtable<ID,Node> nodesAdded = new Hashtable<ID,Node>(); // All created nodes and the new versions of all moved nodes
		Hashtable<ID,Node> nodesChanged = new Hashtable<ID,Node>(); // Only the new versions of all moved nodes

		for(VersionedItem obj : a_changeset.getMemberObjects(Changeset.ChangeType.delete))
		{
			if(obj instanceof Node)
				nodesRemoved.put(obj.getID(), (Node) obj);
		}

		for(VersionedItem obj : a_changeset.getMemberObjects(Changeset.ChangeType.create))
		{
			if(obj instanceof Node)
				nodesAdded.put(obj.getID(), (Node) obj);
		}

		for(VersionedItem obj : a_changeset.getMemberObjects(Changeset.ChangeType.modify))
		{
			if(!(obj instanceof Node))
				continue;
			Node newVersion = (Node)obj;

			Node oldVersion = (Node)old.get(obj);
			if(oldVersion == null)
				continue;

			nodesRemoved.put(oldVersion.getID(), oldVersion);
			nodesAdded.put(newVersion.getID(), newVersion);
			if(!oldVersion.getLonLat().equals(newVersion.getLonLat()))
				nodesChanged.put(newVersion.getID(), newVersion);
		}

		Hashtable<Way,List<ID>> containedWays = new Hashtable<Way,List<ID>>();
		Hashtable<ID,Way> previousWays = new Hashtable<ID,Way>();
		for(VersionedItem obj : a_changeset.getMemberObjects(Changeset.ChangeType.modify))
		{
			if(!(obj instanceof Way))
				continue;
			Way way = (Way) obj;
			containedWays.put(way, Arrays.asList(way.getMembers()));
			previousWays.put(way.getID(), (Way)old.get(way));
		}

		// If only one node is moved in a changeset, that node might be a member of one or more
		// ways and thus change these. As there is no real way to find out the parent ways
		// of a node at the time of the changeset, we have to guess them.
		Date changesetDate = a_changeset.getCreationDate();
		changesetDate.setTime(changesetDate.getTime()-1000);;

		Hashtable<ID,Way> waysChanged = new Hashtable<ID,Way>();
		for(Node node : nodesChanged.values())
		{
			ID nodeID = node.getID();
			// First guess: Ways that have been changed in the changeset
			/*for(Map.Entry<Way,List<String>> entry : containedWays.entrySet())
			{
				if(entry.getValue().contains(nodeID))
				{
					String id = entry.getKey().getDOM().getAttribute("id");
					waysChanged.put(id, previousWays.get(id));
				}
			}*/

			// Second guess: Current parent nodes of the node
			for(Way obj : node.getContainingWays())
			{
				if(waysChanged.containsKey(obj.getID()))
					continue;
				if(containedWays.containsKey(obj))
					continue;
				NavigableMap<Version,Way> history = a_api.getWayFactory().fetchHistory(obj.getID());
				for(Way historyEntry : history.descendingMap().values())
				{
					if(historyEntry.getTimestamp().compareTo(changesetDate) < 0)
					{
						if(Arrays.asList(historyEntry.getMembers()).contains(nodeID))
							waysChanged.put(historyEntry.getID(), historyEntry);
						break;
					}
				}
			}
		}

		// Now make an array of node arrays to represent the old and the new form of the changed ways
		Hashtable<ID,Node> nodesCache = new Hashtable<ID,Node>();
		HashSet<Segment> segmentsOld = new HashSet<Segment>();
		HashSet<Segment> segmentsNew = new HashSet<Segment>();

		for(VersionedItem obj : a_changeset.getMemberObjects(Changeset.ChangeType.create))
		{
			if(!(obj instanceof Way))
				continue;
			Node lastNode = null;
			for(ID id : ((Way)obj).getMembers())
			{
				Node thisNode = null;
				if(nodesAdded.containsKey(id))
					thisNode = nodesAdded.get(id);
				else
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, a_api.getNodeFactory().fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}

				if(lastNode != null)
					segmentsNew.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}
		}

		for(VersionedItem obj : a_changeset.getMemberObjects(Changeset.ChangeType.delete))
		{
			if(!(obj instanceof Way))
				continue;
			Node lastNode = null;
			for(ID id : ((Way)obj).getMembers())
			{
				Node thisNode = null;
				if(nodesRemoved.containsKey(id))
					thisNode = nodesRemoved.get(id);
				else
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, a_api.getNodeFactory().fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}

				if(lastNode != null)
					segmentsOld.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}
		}

		for(Item obj : a_changeset.getMemberObjects(Changeset.ChangeType.modify))
		{
			if(!(obj instanceof Way))
				continue;

			Node lastNode = null;
			for(ID id : ((Way)old.get(obj)).getMembers())
			{
				Node thisNode = null;
				if(nodesRemoved.containsKey(id))
					thisNode = nodesRemoved.get(id);
				else
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, a_api.getNodeFactory().fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}

				if(lastNode != null)
					segmentsOld.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}

			lastNode = null;
			for(ID id : ((Way)obj).getMembers())
			{
				Node thisNode = null;
				if(nodesAdded.containsKey(id))
					thisNode = nodesAdded.get(id);
				else
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, a_api.getNodeFactory().fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}

				if(lastNode != null)
					segmentsNew.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}
		}

		for(Way way : waysChanged.values())
		{
			Node lastNodeOld = null;
			Node lastNodeNew = null;
			for(ID id : way.getMembers())
			{
				Node thisNodeOld = null;
				Node thisNodeNew = null;
				if(nodesAdded.containsKey(id))
					thisNodeNew = nodesAdded.get(id);
				if(nodesRemoved.containsKey(id))
					thisNodeOld = nodesRemoved.get(id);

				if(thisNodeOld == null || thisNodeNew == null)
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, a_api.getNodeFactory().fetch(id, changesetDate));
					if(thisNodeOld == null)
						thisNodeOld = nodesCache.get(id);
					if(thisNodeNew == null)
						thisNodeNew = nodesCache.get(id);
				}

				if(lastNodeOld != null)
					segmentsOld.add(new Segment(lastNodeOld, thisNodeOld));
				if(lastNodeNew != null)
					segmentsNew.add(new Segment(lastNodeNew, thisNodeNew));

				lastNodeOld = thisNodeOld;
				lastNodeNew = thisNodeNew;
			}
		}

		// Create one-node entries for node changes
		for(Node node : nodesRemoved.values())
			segmentsOld.add(new Segment(node, node));
		for(Node node : nodesAdded.values())
			segmentsNew.add(new Segment(node, node));

		HashSet<Segment> segmentsUnchanged = new HashSet<Segment>();
		segmentsUnchanged.addAll(segmentsOld);
		segmentsUnchanged.retainAll(segmentsNew);

		segmentsOld.removeAll(segmentsUnchanged);
		segmentsNew.removeAll(segmentsUnchanged);

		return new Changes(segmentsOld.toArray(new Segment[0]), segmentsNew.toArray(new Segment[0]), segmentsUnchanged.toArray(new Segment[0]));
	}
}
