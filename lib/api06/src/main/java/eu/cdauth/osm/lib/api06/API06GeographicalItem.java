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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import eu.cdauth.osm.lib.*;
import org.w3c.dom.Element;

import eu.cdauth.osm.lib.GeographicalItem;
import java.util.Arrays;
import java.util.SimpleTimeZone;

abstract public class API06GeographicalItem extends API06Item implements VersionedItem, GeographicalItem
{
	private static final SimpleDateFormat sm_dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	static {
		sm_dateFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
	}
	
	private boolean m_current = false; // FIXME: Serialize?

	private Date m_timestamp = null;
	private Version m_version = null;
	private ID m_changeset = null;
	private ID[] m_containingRelations = null;
	
	protected static SimpleDateFormat getDateFormat()
	{
		return sm_dateFormat;
	}
	
	protected API06GeographicalItem(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);

		try
		{
			m_timestamp = getDateFormat().parse(a_dom.getAttribute("timestamp"));
		}
		catch(ParseException e)
		{
		}

		String version = a_dom.getAttribute("version");
		if(!version.equals(""))
			m_version = new Version(version);

		m_changeset = new ID(a_dom.getAttribute("changeset"));
	}
	
	/**
	 * Marks this object to be the current version.
	 */
	protected void markAsCurrent()
	{
		m_current = true;
	}
	
	@Override
	public int compareTo(Item o)
	{
		int c1 = super.compareTo(o);
		if(c1 != 0 || !(o instanceof VersionedItem))
			return c1;
		return getVersion().compareTo(((VersionedItem)o).getVersion());
	}
	
	@Override
	public Date getTimestamp()
	{
		return m_timestamp;
	}
	
	@Override
	public boolean isCurrent()
	{
		return m_current;
	}
	
	/**
	 * Returns the version of this object.
	 * @return The version of this object. {@link Long#MIN_VALUE} if no version attribute is present.
	 */
	@Override
	public Version getVersion()
	{
		return m_version;
	}
	
	@Override
	public ID getChangeset()
	{
		return m_changeset;
	}

	@Override
	public ID[] getContainingRelations() throws APIError
	{
		if(m_containingRelations == null)
		{
			String urlPart;
			if(this instanceof Node)
				urlPart = "node";
			else if(this instanceof Way)
				urlPart = "way";
			else if(this instanceof Relation)
				urlPart = "relation";
			else
				throw new RuntimeException("Unknown data type.");

			Item[] relations = getAPI().get("/"+urlPart+"/"+getID()+"/relations");
			m_containingRelations = new ID[relations.length];
			VersionedItemCache<Relation> cache = getAPI().getRelationFactory().getCache();
			for(int i=0; i<relations.length; i++)
			{
				m_containingRelations[i] = relations[i].getID();
				((API06Relation)relations[i]).markAsCurrent();
				cache.cacheObject((Relation)relations[i]);
			}
		}

		return Arrays.copyOf(m_containingRelations, m_containingRelations.length);
	}
}
