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

package de.cdauth.osm.routemanager;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import de.cdauth.osm.basic.LonLat;
import de.cdauth.osm.basic.Relation;
import de.cdauth.osm.basic.RelationSegment;
import de.cdauth.osm.basic.SQLite;

public class Cache extends SQLite
{
	public Cache(String a_filename) throws ClassNotFoundException, SQLException
	{
		super(a_filename);
	}

	public void cacheRelationSegments(RelationSegment[] a_segments, Relation a_relation, Calendar a_lastUpdate) throws SQLException
	{
		getConnection().createStatement().execute("CREATE TABLE IF NOT EXISTS relation_updated ( relation LONG, updated INT, timestamp TEXT );");
		getConnection().createStatement().execute("CREATE TABLE IF NOT EXISTS relation_segments ( relation LONG, segment INT, i INT, lat REAL, lon REAL );");
		getConnection().commit();
		
		long relationID = Long.parseLong(a_relation.getDOM().getAttribute("id"));
		
		PreparedStatement clearStatement1 = getConnection().prepareStatement("DELETE FROM relation_updated WHERE relation = ?");
		clearStatement1.setLong(1, relationID);
		clearStatement1.execute();
		PreparedStatement clearStatement2 = getConnection().prepareStatement("DELETE FROM relation_segments WHERE relation = ?");
		clearStatement2.setLong(1, relationID);
		clearStatement2.execute();
		
		try
		{
			PreparedStatement updateStatement = getConnection().prepareStatement("INSERT INTO relation_updated ( relation, updated, timestamp ) VALUES ( ?, ?, ? )");
			updateStatement.setLong(1, relationID);
			updateStatement.setInt(2, (int)Math.round(Math.floor(a_lastUpdate.getTimeInMillis()/1000)));
			updateStatement.setString(3, a_relation.getDOM().getAttribute("timestamp"));
			updateStatement.execute();

			PreparedStatement nodeStatement = getConnection().prepareStatement("INSERT INTO relation_segments ( relation, segment, i, lat, lon ) VALUES ( ?, ?, ?, ?, ? )");
			nodeStatement.setLong(1, relationID);
			for(int i=0; i<a_segments.length; i++)
			{
				nodeStatement.setInt(2, i);
				LonLat[] points = a_segments[i].getNodes();
				for(int j=0; j<points.length; j++)
				{
					nodeStatement.setInt(3, j);
					nodeStatement.setDouble(4, points[j].getLat());
					nodeStatement.setDouble(5, points[j].getLon());
					nodeStatement.execute();
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
				getConnection().commit();
			}
			catch(SQLException e2)
			{
			}
			throw e;
		}
	}
}