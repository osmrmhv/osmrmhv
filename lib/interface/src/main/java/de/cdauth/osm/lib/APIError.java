/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
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
