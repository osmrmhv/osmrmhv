/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package eu.cdauth.osm.lib;

import java.io.*;

/**
 * A user in OpenStreetMap. Every user has a 64-bit ID number and a user name. Objects of this class are immutable.
 *
 * @author cdauth
 */
public class User implements Comparable<User>, Serializable
{
	private final ID m_id;
	private final String m_name;

	/**
	 * Create a new user object with the specified ID and user name.
	 * @param a_id The ID of the user.
	 * @param a_name The user name.
	 */
	public User(ID a_id, String a_name)
	{
		m_id = a_id;
		m_name = a_name;
	}

	/**
	 * Returns the ID of this user.
	 * @return The ID of this user.
	 */
	public ID getID()
	{
		return m_id;
	}

	/**
	 * Returns the name of this user.
	 * @return The name of this user.
	 */
	public String getName()
	{
		return m_name;
	}

	@Override
	public int compareTo(User a_o)
	{
		return getID().compareTo(a_o.getID());
	}

	@Override
	public boolean equals(Object a_o)
	{
		return (a_o instanceof User) && getID().equals(((User) a_o).getID());
	}

	@Override
	public int hashCode()
	{
		return m_id.hashCode();
	}

	@Override
	public String toString()
	{
		return getName();
	}
}
