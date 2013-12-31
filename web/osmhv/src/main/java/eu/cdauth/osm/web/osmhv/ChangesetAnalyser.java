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

import eu.cdauth.osm.lib.API;
import eu.cdauth.osm.lib.APIError;
import eu.cdauth.osm.lib.Changeset;
import eu.cdauth.osm.lib.GeographicalItem;
import eu.cdauth.osm.lib.ID;
import eu.cdauth.osm.lib.Item;
import eu.cdauth.osm.lib.Node;
import eu.cdauth.osm.lib.Relation;
import eu.cdauth.osm.lib.Segment;
import eu.cdauth.osm.lib.Version;
import eu.cdauth.osm.lib.VersionedItem;
import eu.cdauth.osm.lib.Way;
import eu.cdauth.osm.lib.api06.API06API;
import eu.cdauth.osm.web.common.Cache;
import eu.cdauth.osm.web.common.Queue;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChangesetAnalyser implements Serializable
{
	private static final Logger sm_logger = Logger.getLogger(ChangesetAnalyser.class.getName());
	
	public static Cache<ChangesetAnalyser> cache = null;
	public static final Queue.Worker worker = new Queue.Worker() {
		public void work(ID a_id)
		{
			try
			{
				ChangesetAnalyser route = new ChangesetAnalyser(a_id);
				cache.saveEntry(a_id.toString(), route);
			}
			catch(Exception e)
			{
				sm_logger.log(Level.WARNING, "Could not save analysis of changeset "+a_id+".", e);
			}
		}
	};

	public Segment[] removed = null;
	public Segment[] created = null;
	public Segment[] unchanged = null;

	public Changeset changeset = null;

	public TagChange[] tagChanges = null;

	public Throwable exception = null;

	public static class TagChange implements Serializable
	{
		public final ID id;
		public final Class<? extends GeographicalItem> type;
		public final Map<String,String> oldTags;
		public final Map<String,String> newTags;

		protected TagChange(ID a_id, Class<? extends GeographicalItem> a_type, Map<String,String> a_oldTags, Map<String,String> a_newTags)
		{
			id = a_id;
			type = a_type;
			oldTags = a_oldTags;
			newTags = a_newTags;
		}
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
	 * @param a_api The API to use to fetch the previous versions.
	 * @param a_changesetId The ID of the changeset to analyse.
	 * @throws APIError The API threw an error.
	 */

	public ChangesetAnalyser(ID a_changesetId)
	{
		try
		{
			API api = new API06API();

			changeset = api.getChangesetFactory().fetch(a_changesetId);

			Map<VersionedItem,VersionedItem> previousVersions = changeset.getPreviousVersions(true);
			tagChanges = new TagChange[previousVersions.size()];
			int i = 0;
			for(Map.Entry<VersionedItem,VersionedItem> it : previousVersions.entrySet())
			{
				VersionedItem oldItem = it.getValue();
				VersionedItem newItem = it.getKey();

				Class<? extends GeographicalItem> type;
				if(oldItem instanceof Node)
					type = Node.class;
				else if(oldItem instanceof Way)
					type = Way.class;
				else if(oldItem instanceof Relation)
					type = Relation.class;
				else
					continue;

				tagChanges[i++] = new TagChange(oldItem.getID(), type, oldItem.getTags(), newItem.getTags());
			}

			Map<VersionedItem, VersionedItem> old = changeset.getPreviousVersions(false);

			Hashtable<ID,Node> nodesRemoved = new Hashtable<ID,Node>(); // All removed nodes and the old versions of all moved nodes
			Hashtable<ID,Node> nodesAdded = new Hashtable<ID,Node>(); // All created nodes and the new versions of all moved nodes
			Hashtable<ID,Node> nodesChanged = new Hashtable<ID,Node>(); // Only the new versions of all moved nodes

			for(VersionedItem obj : changeset.getMemberObjects(Changeset.ChangeType.delete))
			{
				if(obj instanceof Node)
					nodesRemoved.put(obj.getID(), (Node) obj);
			}

			for(VersionedItem obj : changeset.getMemberObjects(Changeset.ChangeType.create))
			{
				if(obj instanceof Node)
					nodesAdded.put(obj.getID(), (Node) obj);
			}

			for(VersionedItem obj : changeset.getMemberObjects(Changeset.ChangeType.modify))
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
			for(VersionedItem obj : changeset.getMemberObjects(Changeset.ChangeType.modify))
			{
				if(!(obj instanceof Way))
					continue;
				Way way = (Way) obj;
				containedWays.put(way, Arrays.asList(way.getMembers()));
			}

			// If only one node is moved in a changeset, that node might be a member of one or more
			// ways and thus change these. As there is no real way to find out the parent ways
			// of a node at the time of the changeset, we have to guess them.
			Date changesetDate = changeset.getCreationDate();
			changesetDate.setTime(changesetDate.getTime()-1000);

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
				for(Way obj : api.getWayFactory().fetch(node.getContainingWays()).values())
				{
					if(waysChanged.containsKey(obj.getID()))
						continue;
					if(containedWays.containsKey(obj))
						continue;
					NavigableMap<Version,Way> history = api.getWayFactory().fetchHistory(obj.getID());
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

			for(VersionedItem obj : changeset.getMemberObjects(Changeset.ChangeType.create))
			{
				if(!(obj instanceof Way))
					continue;
				Node lastNode = null;
				for(ID id : ((Way)obj).getMembers())
				{
					Node thisNode;
					if(nodesAdded.containsKey(id))
						thisNode = nodesAdded.get(id);
					else
					{
						if(!nodesCache.containsKey(id))
							nodesCache.put(id, api.getNodeFactory().fetch(id, ((Way)obj).getTimestamp()));
						thisNode = nodesCache.get(id);
					}

					if(lastNode != null)
						segmentsNew.add(new Segment(lastNode, thisNode));
					lastNode = thisNode;
				}
			}

			for(VersionedItem obj : changeset.getMemberObjects(Changeset.ChangeType.delete))
			{
				if(!(obj instanceof Way))
					continue;
				Node lastNode = null;
				for(ID id : ((Way)obj).getMembers())
				{
					Node thisNode;
					if(nodesRemoved.containsKey(id))
						thisNode = nodesRemoved.get(id);
					else
					{
						if(!nodesCache.containsKey(id))
							nodesCache.put(id, api.getNodeFactory().fetch(id, ((Way)obj).getTimestamp()));
						thisNode = nodesCache.get(id);
					}

					if(lastNode != null)
						segmentsOld.add(new Segment(lastNode, thisNode));
					lastNode = thisNode;
				}
			}

			for(Item obj : changeset.getMemberObjects(Changeset.ChangeType.modify))
			{
				if(!(obj instanceof Way))
					continue;

				Node lastNode = null;
				Way oldWay = ((Way)old.get((Way)obj));
				for(ID id : oldWay.getMembers())
				{
					Node thisNode;
					if(nodesRemoved.containsKey(id))
						thisNode = nodesRemoved.get(id);
					else
					{
						if(!nodesCache.containsKey(id))
							nodesCache.put(id, api.getNodeFactory().fetch(id, oldWay.getTimestamp()));
						thisNode = nodesCache.get(id);
					}

					if(lastNode != null)
						segmentsOld.add(new Segment(lastNode, thisNode));
					lastNode = thisNode;
				}

				lastNode = null;
				for(ID id : ((Way)obj).getMembers())
				{
					Node thisNode;
					if(nodesAdded.containsKey(id))
						thisNode = nodesAdded.get(id);
					else
					{
						if(!nodesCache.containsKey(id))
							nodesCache.put(id, api.getNodeFactory().fetch(id, ((Way)obj).getTimestamp()));
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
							nodesCache.put(id, api.getNodeFactory().fetch(id, changesetDate));
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

			removed = segmentsOld.toArray(new Segment[segmentsOld.size()]);
			created = segmentsNew.toArray(new Segment[segmentsNew.size()]);
			unchanged = segmentsUnchanged.toArray(new Segment[segmentsUnchanged.size()]);
		}
		catch(Throwable e)
		{ // Including OutOfMemoryError
			exception = e;
		}
	}
}
