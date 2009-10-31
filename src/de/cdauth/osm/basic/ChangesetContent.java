/*
    This file is part of OSM Route Manager.

    OSM Route Manager is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OSM Route Manager is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with OSM Route Manager.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.cdauth.osm.basic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Represents the content (the modified elements) of a changeset.
 */

public class ChangesetContent extends XMLObject
{
	static private Hashtable<String,ChangesetContent> sm_cache = new Hashtable<String,ChangesetContent>();
	
	private String m_id;
	
	public enum ChangeType { create, modify, delete };
	
	/**
	 * @param a_id The ID of the changeset, as it is not provided with the XML response.
	 * @param a_dom
	 */
	protected ChangesetContent(String a_id, Element a_dom)
	{
		super(a_dom);
		m_id = a_id;
	}

	public static ChangesetContent fetch(String a_id) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		if(isCached(a_id))
			return sm_cache.get(a_id);
		ChangesetContent root = new ChangesetContent(a_id, API.fetch("/changeset/"+a_id+"/download"));
		sm_cache.put(a_id, root);
		return root;
	}
	
	protected static boolean isCached(String a_id)
	{
		return sm_cache.containsKey(a_id);
	}
	
	/**
	 * Returns an array of all objects that are part of one ChangeType of this changeset.
	 * @param a_type
	 * @return For created and modified objects, their new version. For deleted objects, their old version. 
	 */
	
	public Object[] getMemberObjects(ChangeType a_type)
	{
		ArrayList<Object> ret = new ArrayList<Object>();
		NodeList nodes = getDOM().getElementsByTagName(a_type.toString());
		for(int i=0; i<nodes.getLength(); i++)
			ret.addAll(API.makeObjects((Element) nodes.item(i)));
		Object[] retArr = ret.toArray(new Object[0]);
		for(Object object : retArr)
		{
			String type = object.getDOM().getTagName();
			if(type.equals("node"))
				Node.getCache().cacheVersion((Node)object);
			else if(type.equals("way"))
				Way.getCache().cacheVersion((Way)object);
			else if(type.equals("relation"))
				Relation.getCache().cacheVersion((Relation)object);
		}
		return retArr;
	}
	
	/**
	 * Returns all objects that are part of this changeset.
	 * @return For created and modified objects, their new version. For deleted objects, their old version. 
	 */
	
	public Object[] getMemberObjects()
	{
		ArrayList<Object> ret = new ArrayList<Object>();
		ret.addAll(Arrays.asList(getMemberObjects(ChangeType.create)));
		ret.addAll(Arrays.asList(getMemberObjects(ChangeType.modify)));
		ret.addAll(Arrays.asList(getMemberObjects(ChangeType.delete)));
		return ret.toArray(new Object[0]);
	}
	
	/**
	 * Fetches the previous version of all objects that were modified in this changeset.
	 * @return A hashtable with the new version of an object in the key and the old version in the value 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws APIError
	 */
	public Hashtable<Object,Object> getPreviousVersions() throws IOException, SAXException, ParserConfigurationException, APIError
	{
		return getPreviousVersions(false);
	}
	
	/**
	 * Fetches the previous version of all objects that were modified in this changeset.
	 * @param onlyWithTagChanges If true, only objects will be returned whose tags have changed
	 * @return A hashtable with the new version of an object in the key and the old version in the value
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws APIError
	 */
	public Hashtable<Object,Object> getPreviousVersions(boolean onlyWithTagChanges) throws IOException, SAXException, ParserConfigurationException, APIError
	{
		Object[] newVersions = getMemberObjects(ChangeType.modify);
		Hashtable<Object,Object> ret = new Hashtable<Object,Object>();
		for(int i=0; i<newVersions.length; i++)
		{
			Object last = null;
			String tagName = newVersions[i].getDOM().getTagName();
			try
			{
				if(tagName.equals("node"))
					last = Node.fetch(newVersions[i].getDOM().getAttribute("id"), ""+(Long.parseLong(newVersions[i].getDOM().getAttribute("version"))-1));
				else if(tagName.equals("way"))
					last = Way.fetch(newVersions[i].getDOM().getAttribute("id"), ""+(Long.parseLong(newVersions[i].getDOM().getAttribute("version"))-1));
				else if(tagName.equals("relation"))
					last = Relation.fetch(newVersions[i].getDOM().getAttribute("id"), ""+(Long.parseLong(newVersions[i].getDOM().getAttribute("version"))-1));
			}
			catch(APIError e)
			{
			}
			if(!onlyWithTagChanges || !last.getTags().equals(newVersions[i].getTags()))
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
		Hashtable<Object,Object> old = getPreviousVersions();
		
		Hashtable<String,Node> nodesRemoved = new Hashtable<String,Node>(); // All removed nodes and the old versions of all moved nodes
		Hashtable<String,Node> nodesAdded = new Hashtable<String,Node>(); // All created nodes and the new versions of all moved nodes
		Hashtable<String,Node> nodesChanged = new Hashtable<String,Node>(); // Only the new versions of all moved nodes
		
		for(Object obj : getMemberObjects(ChangeType.delete))
		{
			if(obj.getDOM().getTagName().equals("node"))
				nodesRemoved.put(obj.getDOM().getAttribute("id"), (Node) obj);
		}
		
		for(Object obj : getMemberObjects(ChangeType.create))
		{
			if(obj.getDOM().getTagName().equals("node"))
				nodesAdded.put(obj.getDOM().getAttribute("id"), (Node) obj);
		}
		
		for(Object obj : getMemberObjects(ChangeType.modify))
		{
			if(!obj.getDOM().getTagName().equals("node"))
				continue;
			Node newVersion = (Node)obj;
			
			Node oldVersion = (Node)old.get(obj);
			if(oldVersion == null)
				continue;
			
			nodesRemoved.put(oldVersion.getDOM().getAttribute("id"), oldVersion);
			nodesAdded.put(newVersion.getDOM().getAttribute("id"), newVersion);
			if(!oldVersion.getLonLat().equals(newVersion.getLonLat()))
				nodesChanged.put(newVersion.getDOM().getAttribute("id"), newVersion);
		}
		
		Hashtable<Way,List<String>> containedWays = new Hashtable<Way,List<String>>();
		Hashtable<String,Way> previousWays = new Hashtable<String,Way>();
		for(Object obj : getMemberObjects(ChangeType.modify))
		{
			if(!obj.getDOM().getTagName().equals("way"))
				continue;
			Way way = (Way) obj;
			containedWays.put(way, Arrays.asList(way.getMembers()));
			previousWays.put(way.getDOM().getAttribute("id"), (Way)old.get(way));
		}

		// If only one node is moved in a changeset, that node might be a member of one or more
		// ways and thus change these. As there is no real way to find out the parent ways
		// of a node at the time of the changeset, we have to guess them.
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		Date changesetDate = dateFormat.parse(Changeset.fetch(m_id).getDOM().getAttribute("created_at"));

		Hashtable<String,Way> waysChanged = new Hashtable<String,Way>();
		for(Node node : nodesChanged.values())
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
			for(Object obj : API.get("/node/"+node.getDOM().getAttribute("id")+"/ways"))
			{
				if(!obj.getDOM().getTagName().equals("way"))
					continue;
				if(waysChanged.containsKey(obj.getDOM().getAttribute("id")))
					continue;
				if(containedWays.containsKey(obj))
					continue;
				TreeMap<Long,Way> history = Way.getHistory(obj.getDOM().getAttribute("id"));
				for(Way historyEntry : history.descendingMap().values())
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
		Hashtable<String,Node> nodesCache = new Hashtable<String,Node>();
		HashSet<Segment> segmentsOld = new HashSet<Segment>();
		HashSet<Segment> segmentsNew = new HashSet<Segment>();
		
		for(Object obj : getMemberObjects(ChangeType.create))
		{
			if(!obj.getDOM().getTagName().equals("way"))
				continue;
			Node lastNode = null;
			for(String id : ((Way)obj).getMembers())
			{
				Node thisNode = null;
				if(nodesAdded.containsKey(id))
					thisNode = nodesAdded.get(id);
				else
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, Node.fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}
				
				if(lastNode != null)
					segmentsNew.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}
		}
		
		for(Object obj : getMemberObjects(ChangeType.delete))
		{
			if(!obj.getDOM().getTagName().equals("way"))
				continue;
			Node lastNode = null;
			for(String id : ((Way)obj).getMembers())
			{
				Node thisNode = null;
				if(nodesRemoved.containsKey(id))
					thisNode = nodesRemoved.get(id);
				else
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, Node.fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}
				
				if(lastNode != null)
					segmentsOld.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}
		}
		
		for(Object obj : getMemberObjects(ChangeType.modify))
		{
			if(!obj.getDOM().getTagName().equals("way"))
				continue;
			
			Node lastNode = null;
			for(String id : ((Way)old.get(obj)).getMembers())
			{
				Node thisNode = null;
				if(nodesRemoved.containsKey(id))
					thisNode = nodesRemoved.get(id);
				else
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, Node.fetch(id, changesetDate));
					thisNode = nodesCache.get(id);
				}
				
				if(lastNode != null)
					segmentsOld.add(new Segment(lastNode, thisNode));
				lastNode = thisNode;
			}
			
			lastNode = null;
			for(String id : ((Way)obj).getMembers())
			{
				Node thisNode = null;
				if(nodesAdded.containsKey(id))
					thisNode = nodesAdded.get(id);
				else
				{
					if(!nodesCache.containsKey(id))
						nodesCache.put(id, Node.fetch(id, changesetDate));
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
			for(String id : way.getMembers())
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
						nodesCache.put(id, Node.fetch(id, changesetDate));
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
}
