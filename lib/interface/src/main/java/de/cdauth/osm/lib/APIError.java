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

package de.cdauth.osm.lib;

/**
 * An error has occurred while talking to the OSM API.
 */
public class APIError extends Exception
{
	private static final long serialVersionUID = 437977542958386370L;

	/**
	 * Construct an APIError that was caused by another exception.
	 * @param a_message An error message.
	 * @param a_cause The exception that caused the error.
	 */
	public APIError(String a_message, Throwable a_cause)
	{
		super(a_message, a_cause);
	}
	
	/**
	 * A generic error has occurred talking to the API.
	 * @param a_message An error message.
	 */
	public APIError(String a_message)
	{
		super(a_message);
	}
}