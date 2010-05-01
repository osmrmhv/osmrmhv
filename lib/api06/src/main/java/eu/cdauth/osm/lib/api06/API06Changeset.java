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
	private Hashtable<ChangeType, VersionedItem[]> m_content = null;
	
	/**
	 * Contains the uncleaned osmChange XML element.
	 */
	private Element m_uncleanedDom = null;

	/**
	 * Only for serialization.
	 */
	@Deprecated
	public API06Changeset()
	{
	}

	protected API06Changeset(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);
	}

	@Override
	public Date getCreationDate()
	{
		try
		{
			return API06GeographicalItem.getDateFormat().parse(getDOM().getAttribute("created_at"));
		}
		catch(ParseException e)
		{
			return null;
		}
	}

	@Override
	public Date getClosingDate()
	{
		try
		{
			return API06GeographicalItem.getDateFormat().parse(getDOM().getAttribute("closed_at"));
		}
		catch(ParseException e)
		{
			return null;
		}
	}

	@Override
	public User getUser()
	{
		return new User(new ID(getDOM().getAttribute("uid")), getDOM().getAttribute("user"));
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
		if(m_content == null)
			fixMemberObjects();
		return m_content.get(a_type);
	}
	
	/**
	 * Returns an array of all objects that are part of one ChangeType of this changeset. The returned values are
	 * _not_ clean of double entries.
	 * @param a_type The ChangeType to return.
	 * @return An array of all objects with the ChangeType
	 * @throws APIError There was an error communicating with the API
	 */
	private VersionedItem[] getMemberObjectsUnfixed(ChangeType a_type) throws APIError
	{
		if(m_uncleanedDom == null)
			m_uncleanedDom = getAPI().fetch("/changeset/"+getID()+"/download");

		ArrayList<Item> ret = new ArrayList<Item>();
		NodeList nodes = m_uncleanedDom.getElementsByTagName(a_type.toString());
		for(int i=0; i<nodes.getLength(); i++)
			ret.addAll(getAPI().makeObjects((Element) nodes.item(i)));
		VersionedItem[] retArr = ret.toArray(new VersionedItem[ret.size()]);
		for(VersionedItem object : retArr)
		{
			if(object instanceof Node)
				(getAPI().getNodeFactory()).getCache().cacheObject((Node)object);
			else if(object instanceof Way)
				(getAPI().getWayFactory()).getCache().cacheObject((Way)object);
			else if(object instanceof Relation)
				(getAPI().getRelationFactory()).getCache().cacheObject((Relation)object);
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
	 * @throws APIError There was an error communicating with the API
	 */
	private void fixMemberObjects() throws APIError
	{
		Hashtable<Long, VersionedItem> created = new Hashtable<Long, VersionedItem>();
		Hashtable<Long, VersionedItem> modified = new Hashtable<Long, VersionedItem>();
		Hashtable<Long, VersionedItem> deleted = new Hashtable<Long, VersionedItem>();
		
		for(VersionedItem it : getMemberObjectsUnfixed(ChangeType.create))
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
		
		for(VersionedItem it : getMemberObjectsUnfixed(ChangeType.modify))
		{
			long id = it.getID().asLong() * 4;
			if(it instanceof Node)
				id += 1;
			else if(it instanceof Way)
				id += 2;
			else if(it instanceof Relation)
				id += 3;
			
			// If an object has been created and then modified in one changeset, move the modified one to the “create” block
			if(created.containsKey(id) && it.getVersion().compareTo(created.get(id).getVersion()) > 0)
			{
				created.put(id, it);
				continue;
			}

			// If an object has been modified multiple times in one changeset, only keep the newest one
			if(modified.containsKey(id) && it.getVersion().compareTo(modified.get(id).getVersion()) <= 0)
				continue;
			
			modified.put(id, it);
		}
		
		for(VersionedItem it : getMemberObjectsUnfixed(ChangeType.delete))
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
		
		Hashtable<ChangeType, VersionedItem[]> ret = new Hashtable<ChangeType, VersionedItem[]>();
		ret.put(ChangeType.create, created.values().toArray(new VersionedItem[created.size()]));
		ret.put(ChangeType.modify, modified.values().toArray(new VersionedItem[modified.size()]));
		ret.put(ChangeType.delete, deleted.values().toArray(new VersionedItem[deleted.size()]));
		
		m_content = ret;
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
				if(newVersion instanceof Node)
					last = getAPI().getNodeFactory().fetch(newVersion.getID(), new Version(version));
				else if(newVersion instanceof Way)
					last = getAPI().getWayFactory().fetch(newVersion.getID(), new Version(version));
				else if(newVersion instanceof Relation)
					last = getAPI().getRelationFactory().fetch(newVersion.getID(), new Version(version));
				version--;
			}
			while(last.getChangeset().equals(getID()) && version >= 1);

			if(last != null && (!a_onlyWithTagChanges || !last.getTags().equals(newVersion.getTags())))
				ret.put(newVersion, last);
		}
		return ret;
	}
}
