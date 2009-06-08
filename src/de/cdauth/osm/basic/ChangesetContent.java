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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ChangesetContent extends XMLObject
{
	static private Hashtable<String,ChangesetContent> sm_cache = new Hashtable<String,ChangesetContent>();
	
	private String m_id;
	
	public enum ChangeType { create, modify, delete };
	
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
			ret.addAll(API.makeObjects((Element) nodes.item(i), false));
		return ret.toArray(new Object[0]);
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
			ret.put(newVersions[i], last);
		}
		return ret;
	}
	
	/**
	 * Checks which nodes and ways were changed on a per-node basis. If a node is moved, this
	 * influences all the ways that it belongs to, but they don’t necessarily appear in the
	 * changeset. This method gets all ways that were changed by adding, removing of moving  
	 * their nodes. Thus, created and removed ways are also returned. Nodes that are part of the
	 * changeset but that weren’t moved are ignored. Moved nodes are returned as well, represented
	 * by an array of one node (whereas ways are repesented by an array of multiple nodes). An
	 * empty array indicates that the way or node did not exist before or afterwards.
	 * @return An array of two ArrayLists of ArrayLists of Nodes. For each index, the value in the
	 * first ArrayList represents the nodes of the way before the changeset was commited, the
	 * value in the second one represents the nodes after the commit. 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws APIError
	 * @throws ParseException
	 */
	
	public Node[][][] getNodeChanges() throws IOException, SAXException, ParserConfigurationException, APIError, ParseException
	{
		Hashtable<Object,Object> old = getPreviousVersions();
		
		Hashtable<String,Node> nodesRemoved = new Hashtable<String,Node>(); // All removed nodes and the old versions of all moved nodes
		Hashtable<String,Node> nodesAdded = new Hashtable<String,Node>(); // All created nodes and the new versions of all moved nodes
		Hashtable<String,Node> nodesChanged = new Hashtable<String,Node>(); // Only the new versions of all moved nodes
		
		Hashtable<String,Node> nodePosition = new Hashtable<String,Node>(); // Nodes that are part of the changeset but whose position isn’t changed
		
		for(Object obj : getMemberObjects(ChangeType.delete))
		{
			if(obj.getDOM().getTagName().equals("node"))
			{
				nodesRemoved.put(obj.getDOM().getAttribute("id"), (Node) obj);
				nodePosition.put(obj.getDOM().getAttribute("id"), (Node) obj);
			}
		}
		
		for(Object obj : getMemberObjects(ChangeType.create))
		{
			if(obj.getDOM().getTagName().equals("node"))
			{
				nodesAdded.put(obj.getDOM().getAttribute("id"), (Node) obj);
				nodePosition.put(obj.getDOM().getAttribute("id"), (Node) obj);
			}
		}
		
		for(Object obj : getMemberObjects(ChangeType.modify))
		{
			if(!obj.getDOM().getTagName().equals("node"))
				continue;
			Node newVersion = (Node)obj;
			nodePosition.put(newVersion.getDOM().getAttribute("id"), newVersion);
			
			Node oldVersion = (Node)old.get(obj);
			if(oldVersion == null)
				continue;
			if(oldVersion.getLonLat().equals(newVersion.getLonLat()))
				continue;
			
			nodesRemoved.put(oldVersion.getDOM().getAttribute("id"), oldVersion);
			nodesAdded.put(newVersion.getDOM().getAttribute("id"), newVersion);
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
			for(Map.Entry<Way,List<String>> entry : containedWays.entrySet())
			{
				if(entry.getValue().contains(nodeID))
				{
					String id = entry.getKey().getDOM().getAttribute("id");
					waysChanged.put(id, previousWays.get(id));
				}
			}
			
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
		ArrayList<ArrayList<String>> wayNodes1 = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> wayNodes2 = new ArrayList<ArrayList<String>>();
		HashSet<String> fetchNodes = new HashSet<String>();
		
		for(Object obj : getMemberObjects(ChangeType.create))
		{
			if(!obj.getDOM().getTagName().equals("way"))
				continue;
			
			ArrayList<String> oldVersion = new ArrayList<String>();
			ArrayList<String> newVersion = new ArrayList<String>();
			newVersion.addAll(Arrays.asList(((Way)obj).getMembers()));
			
			for(String id : newVersion)
			{
				if(!nodePosition.containsKey(id))
					fetchNodes.add(id);
			}
			
			wayNodes1.add(oldVersion);
			wayNodes2.add(newVersion);
		}
		
		for(Object obj : getMemberObjects(ChangeType.delete))
		{
			if(!obj.getDOM().getTagName().equals("way"))
				continue;
			
			ArrayList<String> oldVersion = new ArrayList<String>();
			ArrayList<String> newVersion = new ArrayList<String>();
			oldVersion.addAll(Arrays.asList(((Way)obj).getMembers()));
			
			for(String id : oldVersion)
			{
				if(!nodePosition.containsKey(id))
					fetchNodes.add(id);
			}
			
			wayNodes1.add(oldVersion);
			wayNodes2.add(newVersion);
		}
		
		for(Object obj : getMemberObjects(ChangeType.modify))
		{
			if(!obj.getDOM().getTagName().equals("way"))
				continue;
			
			ArrayList<String> oldVersion = new ArrayList<String>();
			ArrayList<String> newVersion = new ArrayList<String>();
			
			oldVersion.addAll(Arrays.asList(((Way)old.get(obj)).getMembers()));
			newVersion.addAll(Arrays.asList(((Way)obj).getMembers()));
			
			if(oldVersion.equals(newVersion))
			{
				boolean changed = false;
				for(String id : newVersion)
				{
					if(nodesChanged.containsKey(id))
					{
						changed = true;
						break;
					}
				}
				if(!changed)
					continue;
			}
			
			for(String id : newVersion)
			{
				if(!nodePosition.containsKey(id))
					fetchNodes.add(id);
			}
			
			wayNodes1.add(oldVersion);
			wayNodes2.add(newVersion);
		}
		
		for(Way way : waysChanged.values())
		{
			ArrayList<String> members = new ArrayList<String>();
			members.addAll(Arrays.asList(way.getMembers()));
			
			for(String id : members)
			{
				if(!nodePosition.containsKey(id))
					fetchNodes.add(id);
			}
			
			wayNodes1.add(members);
			wayNodes2.add(members);
		}
		
		// Download all needed nodes in the right version
		for(String id : fetchNodes)
		{
			TreeMap<Long,Node> history = Node.getHistory(id);
			for(Node historyEntry : history.descendingMap().values())
			{
				Date historyDate = dateFormat.parse(historyEntry.getDOM().getAttribute("timestamp"));
				if(historyDate.compareTo(changesetDate) < 0)
				{
					nodePosition.put(id, historyEntry);
					break;
				}
			}
		}
		
		// Make arrays of nodes
		ArrayList<ArrayList<Node>> wayPoints1 = new ArrayList<ArrayList<Node>>();
		ArrayList<ArrayList<Node>> wayPoints2 = new ArrayList<ArrayList<Node>>();
		for(int i=0; i<wayNodes1.size(); i++)
		{
			ArrayList<Node> oldVersion = new ArrayList<Node>();
			ArrayList<Node> newVersion = new ArrayList<Node>();
			
			for(String id : wayNodes1.get(i))
			{
				if(nodesRemoved.containsKey(id))
					oldVersion.add(nodesRemoved.get(id));
				else
					oldVersion.add(nodePosition.get(id));
			}
			
			for(String id : wayNodes2.get(i))
			{
				if(nodesAdded.containsKey(id))
					newVersion.add(nodesAdded.get(id));
				else
					newVersion.add(nodePosition.get(id));
			}
			wayPoints1.add(oldVersion);
			wayPoints2.add(newVersion);
		}
		
		// Create one-node entries for node changes
		ArrayList<String> changedNodes = new ArrayList<String>();
		changedNodes.addAll(nodesAdded.keySet());
		changedNodes.addAll(nodesRemoved.keySet());
		for(String id : changedNodes)
		{
			ArrayList<Node> oldVersion = new ArrayList<Node>();
			ArrayList<Node> newVersion = new ArrayList<Node>();
			
			if(nodesRemoved.containsKey(id))
				oldVersion.add(nodesRemoved.get(id));
			if(nodesAdded.containsKey(id))
				newVersion.add(nodesAdded.get(id));
			
			wayPoints1.add(oldVersion);
			wayPoints2.add(newVersion);
		}
		
		Node[][][] ret = new Node[2][wayPoints1.size()][];
		for(int i=0; i<wayPoints1.size(); i++)
		{
			ret[0][i] = wayPoints1.get(i).toArray(new Node[0]);
			ret[1][i] = wayPoints2.get(i).toArray(new Node[0]);
		}
		return ret;
	}
}
