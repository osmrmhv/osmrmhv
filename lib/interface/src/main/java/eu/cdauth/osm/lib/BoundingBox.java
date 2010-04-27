/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package eu.cdauth.osm.lib;

import java.io.*;

/**
 * A bounding box consisting of a top (nothern), right, bottom and left coordinate in WGS 84 / EPSG 4326.
 * @author cdauth
 */
public class BoundingBox implements Externalizable
{
	private double m_top = Double.MAX_VALUE;
	private double m_right = Double.MAX_VALUE;
	private double m_bottom = Double.MIN_VALUE;
	private double m_left = Double.MIN_VALUE;

	/**
	 * Only used for serialization.
	 */
	@Deprecated
	public BoundingBox()
	{
	}
	
	/**
	 * Creates a bounding box using two longitude and two latitude borders.
	 * @param a_left The left longitude border. 
	 * @param a_bottom The bottom latitude border.
	 * @param a_right The right longitude border.
	 * @param a_top The top latitude border.
	 */
	public BoundingBox(double a_left, double a_bottom, double a_right, double a_top)
	{
		setTop(a_top);
		setRight(a_right);
		setBottom(a_bottom);
		setLeft(a_left);
	}
	
	/**
	 * Creates a bounding box using two edge points.
	 * @param a_edge1 The first edge point.
	 * @param a_edge2 The second edge point.
	 */
	public BoundingBox(LonLat a_edge1, LonLat a_edge2)
	{
		setTop(Math.max(a_edge1.getLat(), a_edge2.getLat()));
		setRight(Math.max(a_edge1.getLon(), a_edge2.getLon()));
		setBottom(Math.min(a_edge1.getLat(), a_edge2.getLat()));
		setLeft(Math.min(a_edge1.getLon(), a_edge2.getLon()));
	}

	public double getTop()
	{
		return m_top;
	}

	public void setTop(double a_top)
	{
		if(a_top < m_bottom)
		{
			m_top = m_bottom;
			m_bottom = a_top;
		}
		else
			m_top = a_top;
	}

	public double getRight()
	{
		return m_right;
	}

	public void setRight(double a_right)
	{
		if(a_right < m_left)
		{
			m_right = m_left;
			m_left = a_right;
		}
		else
			m_right = a_right;
	}

	public double getBottom()
	{
		return m_bottom;
	}

	public void setBottom(double a_bottom)
	{
		if(a_bottom > m_top)
		{
			m_bottom = m_top;
			m_top = a_bottom;
		}
		else
			m_bottom = a_bottom;
	}

	public double getLeft()
	{
		return m_left;
	}

	public void setLeft(double a_left)
	{
		if(a_left > m_right)
		{
			m_left = m_right;
			m_right = a_left;
		}
		else
			m_left = a_left;
	}
	
	/**
	 * Expands the bounding box so that both the old bounding box as well as the passed bounding box are contained
	 * in the new one.
	 * @param a_boundingBox The bounding box that is to be contained in the current bounding box after the expansion.
	 */
	public void expand(BoundingBox a_boundingBox)
	{
		setTop(Math.max(getTop(), a_boundingBox.getTop()));
		setTop(Math.max(getRight(), a_boundingBox.getRight()));
		setTop(Math.min(getBottom(), a_boundingBox.getBottom()));
		setTop(Math.min(getLeft(), a_boundingBox.getLeft()));
	}
	
	/**
	 * Expands the bounding box so that the given point is inside it or on its border.
	 * @param a_lonLat The point to expand the bounding box to.
	 */
	public void expand(LonLat a_lonLat)
	{
		setTop(Math.max(getTop(), a_lonLat.getLat()));
		setRight(Math.max(getRight(), a_lonLat.getLon()));
		setBottom(Math.min(getBottom(), a_lonLat.getLat()));
		setLeft(Math.min(getLeft(), a_lonLat.getLon()));
	}
	
	public LonLat getTopLeft()
	{
		return new LonLat(getLeft(), getTop());
	}
	
	public LonLat getTopRight()
	{
		return new LonLat(getRight(), getTop());
	}
	
	public LonLat getBottomLeft()
	{
		return new LonLat(getLeft(), getBottom());
	}
	
	public LonLat getBottomRight()
	{
		return new LonLat(getRight(), getBottom());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeDouble(m_top);
		out.writeDouble(m_right);
		out.writeDouble(m_bottom);
		out.writeDouble(m_left);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		m_top = in.readDouble();
		m_right = in.readDouble();
		m_bottom = in.readDouble();
		m_left = in.readDouble();
	}
}
