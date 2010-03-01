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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.Changeset;
import de.cdauth.osm.basic.ChangesetContent;
import de.cdauth.osm.basic.Segment;
import de.cdauth.osm.basic.Object;

/**
 * Represents the content (the modified elements) of a changeset.
 */

public class API06ChangesetContent extends API06XMLObject implements Object,ChangesetContent
{
	private Changeset m_changeset;
	
	private Hashtable<ChangeType,API06Object[]> m_content = null;
	
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
	
	public API06Object[] getMemberObjects(ChangeType a_type)
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
	
	private API06Object[] getMemberObjectsUnfixed(ChangeType a_type)
	{
		ArrayList<API06Object> ret = new ArrayList<API06Object>();
		NodeList nodes = getDOM().getElementsByTagName(a_type.toString());
		for(int i=0; i<nodes.getLength(); i++)
			ret.addAll(API06API.makeObjects((Element) nodes.item(i)));
		API06Object[] retArr = ret.toArray(new API06Object[0]);
		for(API06Object object : retArr)
		{
			String type = object.getDOM().getTagName();
			if(type.equals("node"))
				API06Node.getCache().cacheVersion((API06Node)object);
			else if(type.equals("way"))
				API06Way.getCache().cacheVersion((API06Way)object);
			else if(type.equals("relation"))
				API06Relation.getCache().cacheVersion((API06Relation)object);
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
		Hashtable<Long,API06Object> created = new Hashtable<Long,API06Object>();
		Hashtable<Long,API06Object> modified = new Hashtable<Long,API06Object>();
		Hashtable<Long,API06Object> deleted = new Hashtable<Long,API06Object>();
		
		for(API06Object it : getMemberObjectsUnfixed(ChangeType.create))
		{
			String type = it.getDOM().getTagName();
			long id = Long.parseLong(it.getDOM().getAttribute("id")) * 4;
			if(type.equals("node"))
				id += 1;
			else if(type.equals("way"))
				id += 2;
			else if(type.equals("relation"))
				id += 3;
			created.put(new Long(id), it);
		}
		
		for(API06Object it : getMemberObjectsUnfixed(ChangeType.modify))
		{
			String type = it.getDOM().getTagName();
			long id = Long.parseLong(it.getDOM().getAttribute("id")) * 4;
			if(type.equals("node"))
				id += 1;
			else if(type.equals("way"))
				id += 2;
			else if(type.equals("relation"))
				id += 3;
			Long idObj = new Long(id);
			
			// If an object has been created and then modified in one changeset, move the modified one to the “create” block
			if(created.containsKey(idObj))
			{
				long thisVersion = Long.parseLong(it.getDOM().getAttribute("version"));
				long thatVersion = Long.parseLong(created.get(idObj).getDOM().getAttribute("version"));
				if(thisVersion > thatVersion)
				{
					created.put(idObj, it);
					continue;
				}
			}

			// If an object has been modified multiple times in one changeset, only keep the newest one
			if(modified.containsKey(idObj))
			{
				long thisVersion = Long.parseLong(it.getDOM().getAttribute("version"));
				long thatVersion = Long.parseLong(modified.get(idObj).getDOM().getAttribute("version"));
				if(thisVersion <= thatVersion)
					continue;
			}
			
			modified.put(idObj, it);
		}
		
		for(API06Object it : getMemberObjectsUnfixed(ChangeType.delete))
		{
			String type = it.getDOM().getTagName();
			long id = Long.parseLong(it.getDOM().getAttribute("id")) * 4;
			if(type.equals("node"))
				id += 1;
			else if(type.equals("way"))
				id += 2;
			else if(type.equals("relation"))
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
		
		Hashtable<ChangeType,API06Object[]> ret = new Hashtable<ChangeType,API06Object[]>();
		ret.put(ChangeType.create, created.values().toArray(new API06Object[0]));
		ret.put(ChangeType.modify, modified.values().toArray(new API06Object[0]));
		ret.put(ChangeType.delete, deleted.values().toArray(new API06Object[0]));
		
		m_content = ret;
	}
	
	/**
	 * Returns all objects that are part of this changeset.
	 * @return For created and modified objects, their new version. For deleted objects, their old version. 
	 */
	
	public API06Object[] getMemberObjects()
	{
		ArrayList<API06Object> ret = new ArrayList<API06Object>();
		ret.addAll(Arrays.asList(getMemberObjects(ChangeType.create)));
		ret.addAll(Arrays.asList(getMemberObjects(ChangeType.modify)));
		ret.addAll(Arrays.asList(getMemberObjects(ChangeType.delete)));
		return ret.toArray(new API06Object[0]);
	}
	
	/**
	 * Fetches the previous version of all objects that were modified in this changeset.
	 * @return A hashtable with the new version of an object in the key and the old version in the value 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws APIError
	 */
	public Hashtable<API06Object,API06Object> getPreviousVersions() throws IOException, SAXException, ParserConfigurationException, APIError
	{
		return getPreviousVersions(false);
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
	public Hashtable<API06Object,API06Object> getPreviousVersions(boolean a_onlyWithTagChanges) throws IOException, SAXException, ParserConfigurationException, APIError
	{
		API06Object[] newVersions = getMemberObjects(ChangeType.modify);
		Hashtable<API06Object,API06Object> ret = new Hashtable<API06Object,API06Object>();
		for(int i=0; i<newVersions.length; i++)
		{
			API06Object last = null;
			String tagName = newVersions[i].getDOM().getTagName();
			long version = Long.parseLong(newVersions[i].getDOM().getAttribute("version"))-1;
			do
			{
				if(tagName.equals("node"))
					last = API06Node.fetch(newVersions[i].getDOM().getAttribute("id"), ""+version);
				else if(tagName.equals("way"))
					last = API06Way.fetch(newVersions[i].getDOM().getAttribute("id"), ""+version);
				else if(tagName.equals("relation"))
					last = API06Relation.fetch(newVersions[i].getDOM().getAttribute("id"), ""+version);
				version--;
			}
			while(last.getDOM().getAttribute("changeset").equals(m_id) && version >= 1);

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
		Hashtable<API06Object,API06Object> old = getPreviousVersions();
		
		Hashtable<String,API06Node> nodesRemoved = new Hashtable<String,API06Node>(); // All removed nodes and the old versions of all moved nodes
		Hashtable<String,API06Node> nodesAdded = new Hashtable<String,API06Node>(); // All created nodes and the new versions of all moved nodes
		Hashtable<String,API06Node> nodesChanged = new Hashtable<String,API06Node>(); // Only the new versions of all moved nodes
		
		for(API06Object obj : getMemberObjects(ChangeType.delete))
		{
			if(obj.getDOM().getTagName().equals("node"))
				nodesRemoved.put(obj.getDOM().getAttribute("id"), (API06Node) obj);
		}
		
		for(API06Object obj : getMemberObjects(ChangeType.create))
		{
			if(obj.getDOM().getTagName().equals("node"))
				nodesAdded.put(obj.getDOM().getAttribute("id"), (API06Node) obj);
		}
		
		for(API06Object obj : getMemberObjects(ChangeType.modify))
		{
			if(!obj.getDOM().getTagName().equals("node"))
				continue;
			API06Node newVersion = (API06Node)obj;
			
			API06Node oldVersion = (API06Node)old.get(obj);
			if(oldVersion == null)
				continue;
			
			nodesRemoved.put(oldVersion.getDOM().getAttribute("id"), oldVersion);
			nodesAdded.put(newVersion.getDOM().getAttribute("id"), newVersion);
			if(!oldVersion.getLonLat().equals(newVersion.getLonLat()))
				nodesChanged.put(newVersion.getDOM().getAttribute("id"), newVersion);
		}
		
		Hashtable<API06Way,List<String>> containedWays = new Hashtable<API06Way,List<String>>();
		Hashtable<String,API06Way> previousWays = new Hashtable<String,API06Way>();
		for(API06Object obj : getMemberObjects(ChangeType.modify))
		{
			if(!obj.getDOM().getTagName().equals("way"))
				continue;
			API06Way way = (API06Way) obj;
			containedWays.put(way, Arrays.asList(way.getMembers()));
			previousWays.put(way.getDOM().getAttribute("id"), (API06Way)old.get(way));
		}

		// If only one node is moved in a changeset, that node might be a member of one or more
		// ways and thus change these. As there is no real way to find out the parent ways
		// of a node at the time of the changeset, we have to guess them.
		SimpleDateFormat dateFormat = API06Object.getDateFormat();
		Date changesetDate = new Date(dateFormat.parse(API06Changeset.fetch(m_id).getDOM().getAttribute("created_at")).getTime()-1000);

		Hashtable<String,API06Way> waysChanged = new Hashtable<String,API06Way>();
		for(API06Node node : nodesChanged.values())
		{
			String nodeID = node.getDOM().getAttribute("id");
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
			for(API06Object obj : API06API.get("/node/"+node.getDOM().getAttribute("id")+"/ways"))
			{
				if(!obj.getDOM().getTagName().equals("way"))
					continue;
				if(waysChanged.containsKey(obj.getDOM().getAttribute("id")))
					continue;
				if(containedWays.containsKey(obj))
					continue;
				TreeMap<Long,API06Way> history = API06Way.getHistory(obj.getDOM().getAttribute("id"));
				for(API06Way historyEntry : history.descendingMap().values())
				{
					Date historyDate = dateFormat.parse(historyEntry.getDOM().getAttribute("timestamp"));
					if(historyDate.compareTo(changesetDate) < 0)
					{
						if(Arrays.asList(historyEntry.getMembers()).contains(nodeID))
							waysChanged.put(historyEntry.getDOM().getAttribute("id"), historyEntry);
						break;
					}
				}
			}
		}
		
		// Now make an array of node arrays to represent the old and the new form of the changed ways
		Hashtable<String,API06Node> nodesCache = new Hashtable<String,API06Node>();
		HashSet<Segment> segmentsOld = new HashSet<Segment>();
		HashSet<Segment> segmentsNew = new HashSet<Segment>();
		
		for(API06Object obj : getMemberObjects(ChangeType.create))
		{
			if(!obj.getDOM().getTagName().equals("way"))
				continue;
			API06Node lastNode = null;
			for(String id : ((API06Way)obj).getMembers())
			{
				API06Node thisNode = null;
				if(nodesAdded.containsKey(id))
					thisNode = nodesAdded.get(id);
				else
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, API06Node.fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}
				
				if(lastNode != null)
					segmentsNew.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}
		}
		
		for(API06Object obj : getMemberObjects(ChangeType.delete))
		{
			if(!obj.getDOM().getTagName().equals("way"))
				continue;
			API06Node lastNode = null;
			for(String id : ((API06Way)obj).getMembers())
			{
				API06Node thisNode = null;
				if(nodesRemoved.containsKey(id))
					thisNode = nodesRemoved.get(id);
				else
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, API06Node.fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}
				
				if(lastNode != null)
					segmentsOld.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}
		}
		
		for(API06Object obj : getMemberObjects(ChangeType.modify))
		{
			if(!obj.getDOM().getTagName().equals("way"))
				continue;
			
			API06Node lastNode = null;
			for(String id : ((API06Way)old.get(obj)).getMembers())
			{
				API06Node thisNode = null;
				if(nodesRemoved.containsKey(id))
					thisNode = nodesRemoved.get(id);
				else
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, API06Node.fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}
				
				if(lastNode != null)
					segmentsOld.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}
			
			lastNode = null;
			for(String id : ((API06Way)obj).getMembers())
			{
				API06Node thisNode = null;
				if(nodesAdded.containsKey(id))
					thisNode = nodesAdded.get(id);
				else
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, API06Node.fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}
				
				if(lastNode != null)
					segmentsNew.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}
		}
		
		for(API06Way way : waysChanged.values())
		{
			API06Node lastNodeOld = null;
			API06Node lastNodeNew = null;
			for(String id : way.getMembers())
			{
				API06Node thisNodeOld = null;
				API06Node thisNodeNew = null;
				if(nodesAdded.containsKey(id))
					thisNodeNew = nodesAdded.get(id);
				if(nodesRemoved.containsKey(id))
					thisNodeOld = nodesRemoved.get(id);
				
				if(thisNodeOld == null || thisNodeNew == null)
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, API06Node.fetch(id, changesetDate));
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
		for(API06Node node : nodesRemoved.values())
			segmentsOld.add(new Segment(node, node));
		for(API06Node node : nodesAdded.values())
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
	public Long getID()
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
