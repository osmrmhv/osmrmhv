/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package de.cdauth.osm.lib;

import java.io.*;

/**
 * Represents a connection between two nodes.
 */

public class Segment implements Externalizable
{
	private Node m_node1;
	private Node m_node2;

	/**
	 * Only used for serialization.
	 */
	@Deprecated
	public Segment()
	{
	}
	
	public Segment(Node a_node1, Node a_node2)
	{
		m_node1 = a_node1;
		m_node2 = a_node2;
	}
	
	@Override
	public boolean equals(java.lang.Object obj)
	{
		if(obj instanceof Segment)
		{
			Segment other = (Segment) obj;
			return (getNode1().equals(other.getNode1()) && getNode2().equals(other.getNode2())) || (getNode1().equals(other.getNode2()) && getNode2().equals(other.getNode1()));
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return getNode1().hashCode() ^ getNode2().hashCode();
	}
	
	public Node getNode1()
	{
		return m_node1;
	}
	
	public Node getNode2()
	{
		return m_node2;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeObject(m_node1);
		out.writeObject(m_node2);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		m_node1 = (Node)in.readObject();
		m_node2 = (Node)in.readObject();
	}
}
