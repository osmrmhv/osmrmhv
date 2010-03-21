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
import java.util.Date;

import org.w3c.dom.Element;

import de.cdauth.osm.lib.APIError;
import de.cdauth.osm.lib.GeographicalObject;
import de.cdauth.osm.lib.ID;
import de.cdauth.osm.lib.Node;
import de.cdauth.osm.lib.Object;
import de.cdauth.osm.lib.Relation;
import de.cdauth.osm.lib.Version;
import de.cdauth.osm.lib.VersionedObject;
import de.cdauth.osm.lib.VersionedObjectCache;
import de.cdauth.osm.lib.Way;

abstract public class API06GeographicalObject extends API06Object implements VersionedObject,GeographicalObject
{
	private static SimpleDateFormat sm_dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	
	private boolean m_current = false;
	
	protected static SimpleDateFormat getDateFormat()
	{
		return sm_dateFormat;
	}
	
	protected API06GeographicalObject(Element a_dom, API06API a_api)
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
	public int compareTo(Object o)
	{
		int c1 = super.compareTo(o);
		if(c1 != 0 || !(o instanceof VersionedObject))
			return c1;
		return ((VersionedObject)this).getVersion().compareTo(((VersionedObject)o).getVersion());
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
		String urlPart;
		if(this instanceof Node)
			urlPart = "node";
		else if(this instanceof Way)
			urlPart = "way";
		else if(this instanceof Relation)
			urlPart = "relation";
		else
			throw new RuntimeException("Unknown data type.");
		
		Relation[] ret = (Relation[])getAPI().get("/"+urlPart+"/"+getID()+"/relations");
		VersionedObjectCache<Relation> cache = getAPI().getRelationFactory().getCache();
		for(Relation it : ret)
		{
			((API06Relation)it).markAsCurrent();
			cache.cacheObject(it);
		}
		
		// FIXME: Cache this result somehow?
		return ret;
	}
}
