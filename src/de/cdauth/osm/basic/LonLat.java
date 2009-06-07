

package de.cdauth.osm.basic;

public class LonLat
{
	private double m_lon;
	private double m_lat;
	
	private static final short EARTH_RADIUS = 6367;
	
	public LonLat(double a_lon, double a_lat)
	{
		m_lon = a_lon;
		m_lat = a_lat;
	}
	
	public double getLon()
	{
		return m_lon;
	}
	
	public double getLat()
	{
		return m_lat;
	}
	
	public boolean equals(java.lang.Object a_other)
	{
		if(a_other == null)
			return false;
		if(a_other instanceof LonLat)
		{
			LonLat other = (LonLat) a_other;
			return (getLon() == other.getLon() && getLat() == other.getLat());
		}
		return false;
	}

	public int hashCode()
	{
		return (((int)Math.round(getLat()*128)) << 16) | ((int)Math.round(getLon()*128)); 
	}
	
	/**
	 * Calculate the distance from this point to another.
	 * Formula from {@link http://mathforum.org/library/drmath/view/51879.html}.
	 * @param a_to
	 * @return
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
	
	public String toString()
	{
		//return "http://osm.cdauth.de/map/#mlat[0]="+getLat()+";mlon[0]="+getLon()+";zoom=15;lat="+getLat()+";lon="+getLon();
		return getLat()+","+getLon();
	}
}
