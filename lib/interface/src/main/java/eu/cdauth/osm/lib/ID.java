/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package eu.cdauth.osm.lib;

import java.io.*;

/**
 * All OSM objects have an ID that is a 64-bit integer. This class mainly exists to provide type safety, as other
 * data types such as {@link Version} are 64-bit integers as well.
 * @author cdauth
 */
public class ID implements Comparable<ID>, Serializable
{
	private final Long m_id;

	/**
	 * Creates an ID from a long.
	 * @param a_id The ID.
	 */
	public ID(Long a_id)
	{
		m_id = a_id;
	}

	/**
	 * Creates an ID from a String.
	 * @param a_id The ID, usually the value of the XML attribute <code>id</code>.
	 * @see Long#Long(String)
	 */
	public ID(String a_id)
	{
		m_id = new Long(a_id);
	}
	
	/**
	 * Returns the long representation of this ID.
	 * @return The long representation of this ID.
	 */
	public Long asLong()
	{
		return m_id;
	}
	
	@Override
	public int hashCode()
	{
		if(m_id == null)
			return 0;
		return m_id.hashCode();
	}
	
	@Override
	public boolean equals(java.lang.Object a_other)
	{
		if(!(a_other instanceof ID))
			return false;
		
		ID other = (ID) a_other;
		return (m_id == null && other.m_id == null) || (m_id != null && m_id.equals(other.m_id));
	}
	
	@Override
	public int compareTo(ID a_other)
	{
		if(m_id == null && a_other.m_id == null)
			return 0;
		else if(m_id == null)
			return -1;
		else if(a_other.m_id == null)
			return 1;
		else
			return m_id.compareTo(a_other.m_id);
	}
	
	@Override
	public String toString()
	{
		if(m_id == null)
			return "";
		return m_id.toString();
	}
}
