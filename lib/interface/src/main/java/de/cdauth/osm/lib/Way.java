/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package de.cdauth.osm.lib;

import java.util.Date;

public interface Way extends GeographicalItem, VersionedItem
{
	public ID[] getMembers() throws APIError;
	
	public Node[] getMemberNodes(Date a_date) throws APIError;
	
	public LonLat getRoundaboutCentre() throws APIError;
}
