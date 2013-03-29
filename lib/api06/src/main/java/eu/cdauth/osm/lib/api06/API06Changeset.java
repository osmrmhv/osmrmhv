/*
	Copyright © 2010 Candid Dauth

	Permission is hereby granted, free of charge, to any person obtaining
	a copy of this software and associated documentation files (the “Software”),
	to deal in the Software without restriction, including without limitation
	the rights to use, copy, modify, merge, publish, distribute, sublicense,
	and/or sell copies of the Software, and to permit persons to whom the Software
	is furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
	INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
	PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
	HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
	OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
	SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package eu.cdauth.osm.lib.api06;

import eu.cdauth.osm.lib.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

public class API06Changeset extends API06Item implements Changeset
{
	private Map<ChangeType, VersionedItem[]> m_content = null;
	
	private Date m_creation = null;
	private Date m_closing = null;
	private User m_user = null;

	protected API06Changeset(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);

		try
		{
			m_creation = API06GeographicalItem.getDateFormat().parse(a_dom.getAttribute("created_at"));
		}
		catch(ParseException e)
		{
		}

		try
		{
			m_closing = API06GeographicalItem.getDateFormat().parse(a_dom.getAttribute("closed_at"));
		}
		catch(ParseException e)
		{
		}

		m_user = new User(new ID(a_dom.getAttribute("uid")), a_dom.getAttribute("user"));
	}

	@Override
	public Date getCreationDate()
	{
		return m_creation;
	}

	@Override
	public Date getClosingDate()
	{
		return m_closing;
	}

	@Override
	public User getUser()
	{
		return m_user;
	}
	
	/**
	 * Returns an array of all objects that are part of one ChangeType of this changeset. The returned values are
	 * clean of double entries, see fixMemberObjects().
	 * @param a_type The ChangeType to return.
	 * @return For created and modified objects, their new version. For deleted objects, their old version. 
	 * @throws APIError 
	 */
	@Override
	public VersionedItem[] getMemberObjects(ChangeType a_type) throws APIError
	{
		fixMemberObjects();
		return m_content.get(a_type);
	}
	
	/**
	 * Fetches the content of a changeset and removes double entries.
	 * You can do very funny things in a changeset. You can create an object, modify it multiple times and then remove
	 * it again, so that basically, you haven’t created nor modified nor removed anything in the changeset, because
	 * afterwards everything is as it was.
	 * This function cleans up such multiple entries considering the same object by doing the following things: 
	 * 1. If an object was modified multiple times in one changeset, keep only the newest modification in the “modify” block
	 * 2. If an object has been created and later modified in one changeset, move the newest modification to the “create” block
	 * 3. If an object has been modified and later removed in one changeset, remove the part from the “modify” block
	 * 4. If an object has been created and later removed in one changset, remove it from both the “create” and the “delete” part
	 * @throws APIError There was an error communicating with the API
	 */
	private synchronized void fixMemberObjects() throws APIError
	{
		if(m_content != null)
			return;

		Element uncleanedDom = getAPI().fetch("/changeset/"+getID()+"/download");

		NodeList createdNodes = uncleanedDom.getElementsByTagName("create");
		NodeList modifiedNodes = uncleanedDom.getElementsByTagName("modify");
		NodeList deletedNodes = uncleanedDom.getElementsByTagName("delete");

		ArrayList<Item> createdElements = new ArrayList<Item>();
		ArrayList<Item> modifiedElements = new ArrayList<Item>();
		ArrayList<Item> deletedElements = new ArrayList<Item>();

		for(int i=0; i<createdNodes.getLength(); i++)
			createdElements.addAll(getAPI().makeObjects((Element) createdNodes.item(i)));
		for(int i=0; i<modifiedNodes.getLength(); i++)
			modifiedElements.addAll(getAPI().makeObjects((Element) modifiedNodes.item(i)));
		for(int i=0; i<deletedNodes.getLength(); i++)
		{
			// Deleted items are not fully contained in changeset, we need to download them manually
			NodeList nodes = deletedNodes.item(i).getChildNodes();
			for(int j=0; j<nodes.getLength(); j++)
			{
				if(nodes.item(j).getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
					continue;

				Element el = (Element) nodes.item(j);
				String tag = el.getTagName();
				ID id = new ID(el.getAttribute("id"));
				Version version = new Version(new Version(el.getAttribute("version")).asLong()-1);
				if("node".equals(tag))
					deletedElements.add(getAPI().getNodeFactory().fetch(id, version));
				else if("way".equals(tag))
					deletedElements.add(getAPI().getWayFactory().fetch(id, version));
				else if("relation".equals(tag))
					deletedElements.add(getAPI().getRelationFactory().fetch(id, version));
			}
		}

		ArrayList<Item> all = new ArrayList<Item>();
		all.addAll(createdElements);
		all.addAll(modifiedElements);
		all.addAll(deletedElements);

		for(Item object : all)
		{
			if(object instanceof Node)
				(getAPI().getNodeFactory()).getCache().cacheObject((Node)object);
			else if(object instanceof Way)
				(getAPI().getWayFactory()).getCache().cacheObject((Way)object);
			else if(object instanceof Relation)
				(getAPI().getRelationFactory()).getCache().cacheObject((Relation)object);
		}

		Hashtable<Long, Item> created = new Hashtable<Long, Item>();
		Hashtable<Long, Item> modified = new Hashtable<Long, Item>();
		Hashtable<Long, Item> deleted = new Hashtable<Long, Item>();

		for(Item it : createdElements)
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
		
		for(Item it : modifiedElements)
		{
			long id = it.getID().asLong() * 4;
			if(it instanceof Node)
				id += 1;
			else if(it instanceof Way)
				id += 2;
			else if(it instanceof Relation)
				id += 3;
			
			// If an object has been created and then modified in one changeset, move the modified one to the “create” block
			if(created.containsKey(id) && ((VersionedItem)it).getVersion().compareTo(((VersionedItem)created.get(id)).getVersion()) > 0)
			{
				created.put(id, it);
				continue;
			}

			// If an object has been modified multiple times in one changeset, only keep the newest one
			if(modified.containsKey(id) && ((VersionedItem)it).getVersion().compareTo(((VersionedItem)modified.get(id)).getVersion()) <= 0)
				continue;
			
			modified.put(id, it);
		}
		
		for(Item it : deletedElements)
		{
			long id = it.getID().asLong() * 4;
			if(it instanceof Node)
				id += 1;
			else if(it instanceof Way)
				id += 2;
			else if(it instanceof Relation)
				id += 3;
			
			// If an object has been modified and then deleted in one changeset, remove it from the “modify” block
			if(modified.containsKey(id))
				modified.remove(id);
			
			// If an object has been created and then deleted in one changeset, remove it from both blocks
			if(created.containsKey(id))
			{
				created.remove(id);
				continue;
			}

			deleted.put(id, it);
		}
		
		m_content = new Hashtable<ChangeType, VersionedItem[]>();
		m_content.put(ChangeType.create, created.values().toArray(new VersionedItem[created.size()]));
		m_content.put(ChangeType.modify, modified.values().toArray(new VersionedItem[modified.size()]));
		m_content.put(ChangeType.delete, deleted.values().toArray(new VersionedItem[deleted.size()]));
	}
	
	@Override
	public Map<VersionedItem, VersionedItem> getPreviousVersions(boolean a_onlyWithTagChanges) throws APIError
	{
		VersionedItem[] newVersions = getMemberObjects(ChangeType.modify);
		Hashtable<VersionedItem, VersionedItem> ret = new Hashtable<VersionedItem, VersionedItem>();
		for(VersionedItem newVersion : newVersions)
		{
			VersionedItem last = null;
			long version = newVersion.getVersion().asLong()-1;
			do
			{
				try
				{
					if(newVersion instanceof Node)
						last = getAPI().getNodeFactory().fetch(newVersion.getID(), new Version(version));
					else if(newVersion instanceof Way)
						last = getAPI().getWayFactory().fetch(newVersion.getID(), new Version(version));
					else if(newVersion instanceof Relation)
						last = getAPI().getRelationFactory().fetch(newVersion.getID(), new Version(version));
				}
				catch(APIError e)
				{
					// Since the OSMF License Redaction, there are some versions that are inaccessable and return a status code of 403
					if(e.getCause() == null || !(e.getCause() instanceof API06API.StatusCodeError) || (((API06API.StatusCodeError)e.getCause()).getCode() != 403 && ((API06API.StatusCodeError)e.getCause()).getCode() != 404))
						throw e;
				}
				version--;
			}
			while((last == null || last.getChangeset().equals(getID())) && version >= 1);

			if(last != null && (!a_onlyWithTagChanges || !last.getTags().equals(newVersion.getTags())))
				ret.put(newVersion, last);
		}
		return ret;
	}
}
