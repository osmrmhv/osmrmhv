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
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.Changeset;
import de.cdauth.osm.basic.ChangesetContent;
import de.cdauth.osm.basic.ID;
import de.cdauth.osm.basic.Node;
import de.cdauth.osm.basic.Object;
import de.cdauth.osm.basic.Relation;
import de.cdauth.osm.basic.Segment;
import de.cdauth.osm.basic.Version;
import de.cdauth.osm.basic.VersionedObject;
import de.cdauth.osm.basic.Way;

/**
 * Represents the content (the modified elements) of a changeset.
 */

public class API06ChangesetContent extends API06XMLObject implements Object,ChangesetContent
{
	private Changeset m_changeset;
	
	private Hashtable<ChangeType,VersionedObject[]> m_content = null;
	
	protected API06ChangesetContent(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);
	}
	
	protected void setChangeset(Changeset a_changeset)
	{
		m_changeset = a_changeset;
	}
	
	/**
	 * Returns an array of all objects that are part of one ChangeType of this changeset. The returned values are
	 * clean of double entries, see fixMemberObjects().
	 * @param a_type
	 * @return For created and modified objects, their new version. For deleted objects, their old version. 
	 */
	@Override
	public VersionedObject[] getMemberObjects(ChangeType a_type)
	{
		if(m_content == null)
			fixMemberObjects();
		return m_content.get(a_type);
	}
	
	/**
	 * Returns an array of all objects that are part of one ChangeType of this changeset. The returned values are
	 * _not_ clean of double entries.
	 * @param a_type
	 * @return
	 */
	private VersionedObject[] getMemberObjectsUnfixed(ChangeType a_type)
	{
		ArrayList<Object> ret = new ArrayList<Object>();
		NodeList nodes = getDOM().getElementsByTagName(a_type.toString());
		for(int i=0; i<nodes.getLength(); i++)
			ret.addAll(getAPI().makeObjects((Element) nodes.item(i)));
		VersionedObject[] retArr = ret.toArray(new VersionedObject[0]);
		for(VersionedObject object : retArr)
		{
			if(object instanceof Node)
				((API06NodeFactory)getAPI().getNodeFactory()).getCache().cacheVersion((Node)object);
			else if(object instanceof Way)
				((API06WayFactory)getAPI().getWayFactory()).getCache().cacheVersion((Way)object);
			else if(object instanceof Relation)
				((API06RelationFactory)getAPI().getRelationFactory()).getCache().cacheVersion((Relation)object);
		}
		return retArr;
	}
	
	/**
	 * Removes double entries in this Changeset.
	 * You can do very funny things in a changeset. You can create an object, modify it multiple times and then remove
	 * it again, so that basically, you haven’t created nor modified nor removed anything in the changeset, because
	 * afterwards everything is as it was.
	 * This function cleans up such multiple entries considering the same object by doing the following things: 
	 * 1. If an object was modified multiple times in one changeset, keep only the newest modification in the “modify” block
	 * 2. If an object has been created and later modified in one changeset, move the newest modification to the “create” block
	 * 3. If an object has been modified and later removed in one changeset, remove the part from the “modify” block
	 * 4. If an object has been created and later removed in one changset, remove it from both the “create” and the “delete” part
	 */
	private void fixMemberObjects()
	{
		Hashtable<Long,VersionedObject> created = new Hashtable<Long,VersionedObject>();
		Hashtable<Long,VersionedObject> modified = new Hashtable<Long,VersionedObject>();
		Hashtable<Long,VersionedObject> deleted = new Hashtable<Long,VersionedObject>();
		
		for(VersionedObject it : getMemberObjectsUnfixed(ChangeType.create))
		{
			long id = it.getID().asLong() * 4;
			if(it instanceof Node)
				id += 1;
			else if(it instanceof Way)
				id += 2;
			else if(it instanceof Relation)
				id += 3;
			created.put(id, it);
		}
		
		for(VersionedObject it : getMemberObjectsUnfixed(ChangeType.modify))
		{
			long id = it.getID().asLong() * 4;
			if(it instanceof Node)
				id += 1;
			else if(it instanceof Way)
				id += 2;
			else if(it instanceof Relation)
				id += 3;
			Long idObj = new Long(id);
			
			// If an object has been created and then modified in one changeset, move the modified one to the “create” block
			if(created.containsKey(idObj) && it.getVersion().compareTo(created.get(idObj).getVersion()) > 0)
			{
				created.put(idObj, it);
				continue;
			}

			// If an object has been modified multiple times in one changeset, only keep the newest one
			if(modified.containsKey(idObj) && it.getVersion().compareTo(modified.get(idObj).getVersion()) <= 0)
				continue;
			
			modified.put(idObj, it);
		}
		
		for(VersionedObject it : getMemberObjectsUnfixed(ChangeType.delete))
		{
			long id = it.getID().asLong() * 4;
			if(it instanceof Node)
				id += 1;
			else if(it instanceof Way)
				id += 2;
			else if(it instanceof Relation)
				id += 3;
			Long idObj = new Long(id);
			
			// If an object has been modified and then deleted in one changeset, remove it from the “modify” block
			if(modified.containsKey(idObj))
				modified.remove(idObj);
			
			// If an object has been created and then deleted in one changeset, remove it from both blocks
			if(created.containsKey(idObj))
			{
				created.remove(idObj);
				continue;
			}

			deleted.put(new Long(id), it);
		}
		
		Hashtable<ChangeType,VersionedObject[]> ret = new Hashtable<ChangeType,VersionedObject[]>();
		ret.put(ChangeType.create, created.values().toArray(new VersionedObject[0]));
		ret.put(ChangeType.modify, modified.values().toArray(new VersionedObject[0]));
		ret.put(ChangeType.delete, deleted.values().toArray(new VersionedObject[0]));
		
		m_content = ret;
	}
	
	/**
	 * Returns all objects that are part of this changeset.
	 * @return For created and modified objects, their new version. For deleted objects, their old version. 
	 */
	@Override
	public VersionedObject[] getMemberObjects()
	{
		ArrayList<VersionedObject> ret = new ArrayList<VersionedObject>();
		ret.addAll(Arrays.asList(getMemberObjects(ChangeType.create)));
		ret.addAll(Arrays.asList(getMemberObjects(ChangeType.modify)));
		ret.addAll(Arrays.asList(getMemberObjects(ChangeType.delete)));
		return ret.toArray(new VersionedObject[0]);
	}
	
	/**
	 * Fetches the previous version of all objects that were modified in this changeset.
	 * @param a_onlyWithTagChanges If true, only objects will be returned whose tags have changed
	 * @return A hashtable with the new version of an object in the key and the old version in the value
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws APIError
	 */
	@Override
	public Map<VersionedObject,VersionedObject> getPreviousVersions(boolean a_onlyWithTagChanges) throws APIError
	{
		VersionedObject[] newVersions = getMemberObjects(ChangeType.modify);
		Hashtable<VersionedObject,VersionedObject> ret = new Hashtable<VersionedObject,VersionedObject>();
		for(int i=0; i<newVersions.length; i++)
		{
			VersionedObject last = null;
			long version = newVersions[i].getVersion().asLong()-1;
			do
			{
				if(newVersions[i] instanceof Node)
					last = getAPI().getNodeFactory().fetch(newVersions[i].getID(), new Version(version));
				else if(newVersions[i] instanceof Way)
					last = getAPI().getWayFactory().fetch(newVersions[i].getID(), new Version(version));
				else if(newVersions[i] instanceof Relation)
					last = getAPI().getRelationFactory().fetch(newVersions[i].getID(), new Version(version));
				version--;
			}
			while(last.getChangeset() == getChangeset().getID() && version >= 1);

			if(last != null && (!a_onlyWithTagChanges || !last.getTags().equals(newVersions[i].getTags())))
				ret.put(newVersions[i], last);
		}
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
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws APIError
	 * @throws ParseException
	 */
	
	public Segment[][] getNodeChanges() throws IOException, SAXException, ParserConfigurationException, APIError, ParseException
	{
		Map<VersionedObject,VersionedObject> old = getPreviousVersions(false);
		
		Hashtable<ID,Node> nodesRemoved = new Hashtable<ID,Node>(); // All removed nodes and the old versions of all moved nodes
		Hashtable<ID,Node> nodesAdded = new Hashtable<ID,Node>(); // All created nodes and the new versions of all moved nodes
		Hashtable<ID,Node> nodesChanged = new Hashtable<ID,Node>(); // Only the new versions of all moved nodes
		
		for(VersionedObject obj : getMemberObjects(ChangeType.delete))
		{
			if(obj instanceof Node)
				nodesRemoved.put(obj.getID(), (Node) obj);
		}
		
		for(VersionedObject obj : getMemberObjects(ChangeType.create))
		{
			if(obj instanceof Node)
				nodesAdded.put(obj.getID(), (Node) obj);
		}
		
		for(VersionedObject obj : getMemberObjects(ChangeType.modify))
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
		for(VersionedObject obj : getMemberObjects(ChangeType.modify))
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
		Date changesetDate = getChangeset().getCreationDate();
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
				NavigableMap<Version,Way> history = getAPI().getWayFactory().fetchHistory(obj.getID());
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
		
		for(VersionedObject obj : getMemberObjects(ChangeType.create))
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
						nodesCache.put(id, getAPI().getNodeFactory().fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}
				
				if(lastNode != null)
					segmentsNew.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}
		}
		
		for(VersionedObject obj : getMemberObjects(ChangeType.delete))
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
						nodesCache.put(id, getAPI().getNodeFactory().fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}
				
				if(lastNode != null)
					segmentsOld.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}
		}
		
		for(Object obj : getMemberObjects(ChangeType.modify))
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
						nodesCache.put(id, getAPI().getNodeFactory().fetch(id, changesetDate));
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
						nodesCache.put(id, getAPI().getNodeFactory().fetch(id, changesetDate));
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
						nodesCache.put(id, getAPI().getNodeFactory().fetch(id, changesetDate));
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
		
		Segment[][] ret = { segmentsOld.toArray(new Segment[0]), segmentsNew.toArray(new Segment[0]), segmentsUnchanged.toArray(new Segment[0]) };
		return ret;
	}

	@Override
	public ID getID()
	{
		return getChangeset().getID();
	}

	@Override
	public String getTag(String a_tagName)
	{
		return null;
	}

	@Override
	public Map<String, String> getTags()
	{
		return new Hashtable<String,String>();
	}

	@Override
	public int compareTo(Object a_o)
	{
		return getID().compareTo(a_o.getID());
	}

	@Override
	public Changeset getChangeset()
	{
		return m_changeset;
	}
}
