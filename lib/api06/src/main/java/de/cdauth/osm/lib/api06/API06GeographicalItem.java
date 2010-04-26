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

package de.cdauth.osm.lib.api06;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import de.cdauth.osm.lib.*;
import org.w3c.dom.Element;

import de.cdauth.osm.lib.GeographicalItem;

abstract public class API06GeographicalItem extends API06Item implements VersionedItem, GeographicalItem
{
	private static SimpleDateFormat sm_dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	
	private boolean m_current = false;

	private ID[] m_containingRelations = null;

	/**
	 * Only for serialization.
	 */
	@Deprecated
	public API06GeographicalItem()
	{
	}
	
	protected static SimpleDateFormat getDateFormat()
	{
		return sm_dateFormat;
	}
	
	protected API06GeographicalItem(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);
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
		return ((VersionedItem)this).getVersion().compareTo(((VersionedItem)o).getVersion());
	}
	
	@Override
	public Date getTimestamp()
	{
		try
		{
			return getDateFormat().parse(getDOM().getAttribute("timestamp"));
		}
		catch(ParseException e)
		{
			return null;
		}
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
		String version = getDOM().getAttribute("version");
		if(version.equals(""))
			return null;
		return new Version(version);
	}
	
	@Override
	public ID getChangeset()
	{
		return new ID(getDOM().getAttribute("changeset"));
	}

	@Override
	public Relation[] getContainingRelations() throws APIError
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
			Relation[] ret = new Relation[relations.length];
			for(int i=0; i<relations.length; i++)
				ret[i] = (Relation)relations[i];
			VersionedItemCache<Relation> cache = getAPI().getRelationFactory().getCache();
			for(Relation it : ret)
			{
				((API06Relation)it).markAsCurrent();
				cache.cacheObject(it);
			}

			synchronized(this)
			{// TODO This does not seem to be useful
				m_containingRelations = new ID[ret.length];
				for(int i=0; i<ret.length; i++)
					m_containingRelations[i] = ret[i].getID();
			}

			return ret;
		}
		else
		{
			Collection<Relation> ret = getAPI().getRelationFactory().fetch(m_containingRelations).values();
			return ret.toArray(new Relation[ret.size()]);
		}
	}
}
