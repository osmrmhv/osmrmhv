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

package de.cdauth.osm.basic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the connection to an SQLite database. Usually, you extend this class and provide
 * methods to cache various time-consuming results of this library. This class uses the
 * org.sqlite.JDBC driver from {@link "http://www.xerial.org/trac/Xerial/wiki/SQLiteJDBC"}.
 */
public class SQLite
{
	private Connection m_connection = null;
	
	/**
	 * Opens a JDBC “connection” with no auto-commit.
	 * @param a_filename
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public SQLite(String a_filename) throws ClassNotFoundException, SQLException
	{
		Class.forName("org.sqlite.JDBC");
		m_connection = DriverManager.getConnection("jdbc:sqlite:"+a_filename);
		m_connection.setAutoCommit(false);
	}
	
	public Connection getConnection()
	{
		return m_connection;
	}
}
