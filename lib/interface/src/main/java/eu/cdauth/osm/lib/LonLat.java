/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package eu.cdauth.osm.lib;

import java.io.Serializable;

/**
 * Represents a pair of coordinates in WGS 84 / EPSG 4326. This is immutable.
 */

public class LonLat implements Serializable
{
	private final double m_lon;
	private final double m_lat;
	
	private static final short EARTH_RADIUS = 6367;

	/**
	 * Creates a pair of coordinates.
	 * @param a_lon The longitude in degrees, a value greater than or equal -90 and less than or equal +90.
	 * @param a_lat The latitude in degrees, a value greater than -180 and less than or equal 180.
	 */
	public LonLat(double a_lon, double a_lat)
	{
		m_lon = a_lon;
		m_lat = a_lat;
	}
	
	/**
	 * Returns the longitude.
	 * @return The longitude in degrees, greater than or equal -90 and less than or equal 90.
	 */
	public double getLon()
	{
		return m_lon;
	}
	
	/**
	 * Returns the latitude.
	 * @return The latitude in degrees, greater than -180 and less than or equal 180.
	 */
	public double getLat()
	{
		return m_lat;
	}
	
	/**
	 * Two LonLat objects are equal if their coordinates are the same.
	 * The OpenStreetMap database saves 7 decimals for each coordinate. If lower decimals differ, this is a result of computation errors
	 * and is ignored.
	 * 
	 * <p>{@inheritDoc}
	 */
	@Override
	public boolean equals(java.lang.Object a_other)
	{
		if(a_other == null)
			return false;
		if(a_other instanceof LonLat)
		{
			LonLat other = (LonLat) a_other;
			return (Math.abs(getLon()-other.getLon()) < 0.00000005D && Math.abs(getLat()-other.getLat()) < 0.00000005D);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return (((int)Math.round(getLat()*128)) << 16) | ((int)Math.round(getLon()*128)); 
	}
	
	/**
	 * Calculate the distance from this point to another.
	 * Formula from {@link "http://mathforum.org/library/drmath/view/51879.html"}.
	 * @param a_to The reference point to calculate the distance to.
	 * @return The distance in km.
	 */
	public double getDistance(LonLat a_to)
	{
		double lat1 = getLat()*Math.PI/180;
		double lat2 = a_to.getLat()*Math.PI/180;
		double lon1 = getLon()*Math.PI/180;
		double lon2 = a_to.getLon()*Math.PI/180;

		double dlon = lon2 - lon1;
		double dlat = lat2 - lat1;
		double a = Math.pow((Math.sin(dlat/2)),2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow((Math.sin(dlon/2)),2);
		return EARTH_RADIUS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	}
	
	@Override
	public String toString()
	{
		return getLat()+","+getLon();
	}
}
