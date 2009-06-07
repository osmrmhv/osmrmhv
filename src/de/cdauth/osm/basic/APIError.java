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

import java.io.IOException;
import java.net.HttpURLConnection;

public class APIError extends Exception
{
	private static final long serialVersionUID = 437977542958386370L;
	private String m_message;

	public APIError(HttpURLConnection a_connection)
	{
		m_message = "URL: "+a_connection.getURL().toString();
		try
		{
			m_message += "\nStatus: "+a_connection.getResponseCode();
		}
		catch(IOException e)
		{
		}
	}
	
	public APIError(String a_message)
	{
		m_message = a_message;
	}
	
	public String toString()
	{
		return m_message;
	}
}