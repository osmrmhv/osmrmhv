/*
    This file is part of the osmrmhv library.

    osmrmhv is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    osmrmhv is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with osmrmhv. If not, see <http://www.gnu.org/licenses/>.
*/

package de.cdauth.osm.basic;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * An error has occurred while talking to the OSM API.
 */
public class APIError extends Exception
{
	public APIError(String a_message, Throwable a_cause)
	{
		super(a_message, a_cause);
	}

	private static final long serialVersionUID = 437977542958386370L;
	private String m_message;

	/**
	 * An error has occurred with the HTTP connection to the API.
	 * @param a_connection
	 */
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