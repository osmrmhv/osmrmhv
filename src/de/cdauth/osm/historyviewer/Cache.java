/*
    This file is part of OSM Route Manager.

    OSM Route Manager is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OSM Route Manager is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with OSM Route Manager.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.cdauth.osm.historyviewer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import de.cdauth.osm.basic.Changeset;
import de.cdauth.osm.basic.SQLite;
import de.cdauth.osm.basic.Segment;
import de.cdauth.osm.basic.Object;

public class Cache extends SQLite
{
	public static final int ACTION_REMOVE = 1;
	public static final int ACTION_CREATE = 2;
	public static final int ACTION_UNCHANGED = 3;
	
	public static final int TYPE_NODE = 1;
	public static final int TYPE_WAY = 2;
	public static final int TYPE_RELATION = 3;

	public Cache(String a_filename) throws ClassNotFoundException, SQLException
	{
		super(a_filename);
	}

	public void cacheChangesetChanges(Changeset a_changeset, Segment[][] a_changes, Calendar a_lastUpdate, Hashtable<Object,Object> a_tagChanges) throws SQLException
	{
		if(a_changes.length != 3)
			throw new IllegalArgumentException("This is not the return value of de.cdauth.osm.basic.RelationSegment.getNodeChanges().");

		getConnection().createStatement().execute("CREATE TABLE IF NOT EXISTS changeset_information ( changeset LONG, created TEXT, closed TEXT, user TEXT, analysed INT );");
		getConnection().createStatement().execute("CREATE TABLE IF NOT EXISTS changeset_tags ( changeset LONG, k TEXT, v TEXT );");
		getConnection().createStatement().execute("CREATE TABLE IF NOT EXISTS changeset_changes ( changeset LONG, i LONG, id1 LONG, lat1 REAL, lon1 REAL, id2 LONG, lat2 REAL, lon2 REAL, action INT );");
		getConnection().createStatement().execute("CREATE TABLE IF NOT EXISTS changeset_tags_objects ( changeset LONG, type INT, id LONG, tagname TEXT, value1 TEXT, value2 TEXT );");
		getConnection().commit();
		
		long changesetID = Long.parseLong(a_changeset.getDOM().getAttribute("id"));
		PreparedStatement clearStatement1 = getConnection().prepareStatement("DELETE FROM changeset_information WHERE changeset = ?");
		clearStatement1.setLong(1, changesetID);
		clearStatement1.execute();
		PreparedStatement clearStatement2 = getConnection().prepareStatement("DELETE FROM changeset_tags WHERE changeset = ?");
		clearStatement2.setLong(1, changesetID);
		clearStatement2.execute();
		PreparedStatement clearStatement3 = getConnection().prepareStatement("DELETE FROM changeset_changes WHERE changeset = ?");
		clearStatement3.setLong(1, changesetID);
		clearStatement3.execute();
		PreparedStatement clearStatement4 = getConnection().prepareStatement("DELETE FROM changeset_tags_objects WHERE changeset = ?");
		clearStatement4.setLong(1, changesetID);
		clearStatement4.execute();
		getConnection().commit();
		
		try
		{
			PreparedStatement updateStatement = getConnection().prepareStatement("INSERT INTO changeset_information ( changeset, created, closed, user, analysed ) VALUES ( ?, ?, ?, ?, ? );");
			updateStatement.setLong(1, changesetID);
			updateStatement.setString(2, a_changeset.getDOM().getAttribute("created_at"));
			updateStatement.setString(3, a_changeset.getDOM().getAttribute("closed_at"));
			updateStatement.setString(4, a_changeset.getDOM().getAttribute("user"));
			updateStatement.setInt(5, (int)Math.round(Math.floor(a_lastUpdate.getTimeInMillis()/1000)));
			updateStatement.execute();
			
			PreparedStatement tagStatement = getConnection().prepareStatement("INSERT INTO changeset_tags ( changeset, k, v ) VALUES ( ?, ?, ? );");
			tagStatement.setLong(1, changesetID);
			for(Map.Entry<String,String> tag : a_changeset.getTags().entrySet())
			{
				tagStatement.setString(2, tag.getKey());
				tagStatement.setString(3, tag.getValue());
				tagStatement.execute();
			}

			PreparedStatement nodeStatement = getConnection().prepareStatement("INSERT INTO changeset_changes ( changeset, id1, lat1, lon1, id2, lat2, lon2, action ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ? );");
			nodeStatement.setLong(1, changesetID);
			
			nodeStatement.setInt(8, ACTION_REMOVE);
			for(Segment segment : a_changes[0])
			{
				nodeStatement.setLong(2, Long.parseLong(segment.getNode1().getDOM().getAttribute("id")));
				nodeStatement.setDouble(3, segment.getNode1().getLonLat().getLat());
				nodeStatement.setDouble(4, segment.getNode1().getLonLat().getLon());
				nodeStatement.setLong(5, Long.parseLong(segment.getNode2().getDOM().getAttribute("id")));
				nodeStatement.setDouble(6, segment.getNode2().getLonLat().getLat());
				nodeStatement.setDouble(7, segment.getNode2().getLonLat().getLon());
				nodeStatement.execute();
			}
			
			nodeStatement.setInt(8, ACTION_CREATE);
			for(Segment segment : a_changes[1])
			{
				nodeStatement.setLong(2, Long.parseLong(segment.getNode1().getDOM().getAttribute("id")));
				nodeStatement.setDouble(3, segment.getNode1().getLonLat().getLat());
				nodeStatement.setDouble(4, segment.getNode1().getLonLat().getLon());
				nodeStatement.setLong(5, Long.parseLong(segment.getNode2().getDOM().getAttribute("id")));
				nodeStatement.setDouble(6, segment.getNode2().getLonLat().getLat());
				nodeStatement.setDouble(7, segment.getNode2().getLonLat().getLon());
				nodeStatement.execute();
			}
			
			nodeStatement.setInt(8, ACTION_UNCHANGED);
			for(Segment segment : a_changes[2])
			{
				nodeStatement.setLong(2, Long.parseLong(segment.getNode1().getDOM().getAttribute("id")));
				nodeStatement.setDouble(3, segment.getNode1().getLonLat().getLat());
				nodeStatement.setDouble(4, segment.getNode1().getLonLat().getLon());
				nodeStatement.setLong(5, Long.parseLong(segment.getNode2().getDOM().getAttribute("id")));
				nodeStatement.setDouble(6, segment.getNode2().getLonLat().getLat());
				nodeStatement.setDouble(7, segment.getNode2().getLonLat().getLon());
				nodeStatement.execute();
			}
			
			PreparedStatement tagObjectsStatement = getConnection().prepareStatement("INSERT INTO changeset_tags_objects ( changeset, type, id, tagname, value1, value2 ) VALUES ( ?, ?, ?, ?, ?, ? );");
			tagObjectsStatement.setLong(1, changesetID);
			Set<Object> tagChangesOld = a_tagChanges.keySet();
			for(Object objNew : tagChangesOld)
			{
				Object objOld = a_tagChanges.get(objNew);
				
				String type = objNew.getDOM().getTagName();
				if(type.equals("node"))
					tagObjectsStatement.setInt(2, TYPE_NODE);
				else if(type.equals("way"))
					tagObjectsStatement.setInt(2, TYPE_WAY);
				else if(type.equals("relation"))
					tagObjectsStatement.setInt(2, TYPE_RELATION);
				tagObjectsStatement.setLong(3, Long.parseLong(objNew.getDOM().getAttribute("id")));

				HashSet<String> tags = new HashSet<String>();
				tags.addAll(objOld.getTags().keySet());
				tags.addAll(objNew.getTags().keySet());
				for(String tag : tags)
				{
					tagObjectsStatement.setString(4, tag);
					tagObjectsStatement.setString(5, objOld.getTag(tag));
					tagObjectsStatement.setString(6, objNew.getTag(tag));
					tagObjectsStatement.execute();
				}
			}
			
			getConnection().commit();
		}
		catch(SQLException e)
		{
			try
			{
				getConnection().rollback();
				clearStatement1.execute();
				clearStatement2.execute();
				clearStatement3.execute();
				getConnection().commit();
			}
			catch(SQLException e2)
			{
			}
			throw e;
		}
	}
}
