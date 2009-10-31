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
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Provides static methods to communicate with the OSM API. Handles the HTTP connection
 * and parses XML responses.
 */

public class API
{
	private static final String API_SERVER = "api.openstreetmap.org";
	private static final int API_PORT = 80;
	private static final String API_PREFIX = "/api/0.6";
	private static String sm_userAgent = "cdauth’s OSM library";
	
	/**
	 * Sets the User-Agent HTTP request header for all future API requests.
	 * @param a_userAgent
	 */
	public static void setUserAgent(String a_userAgent)
	{
		sm_userAgent = a_userAgent;
	}

	/**
	 * Makes a HTTP request to the API and returns the XML root element of the response.
	 * @param a_url The URL to be appended to the API prefix, for example "/node/1"
	 * @param a_server The API hostname, for example "api.openstreetmap.org"
	 * @param a_port The API port, usually 80.
	 * @param a_prefix The API prefix, for example "/api/0.6"
	 * @return The root DOM Element of the XML response.
	 * @throws IOException There was an error communicating with the API server.
	 * @throws APIError The HTTP response code was not 200 or the XML response did not contain a root element.
	 * @throws SAXException The XML response could not be parsed.
	 * @throws ParserConfigurationException There was a problem while initialising the XML parser.
	 */
	public static Element fetch(String a_url, String a_server, int a_port, String a_prefix) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		HttpURLConnection connection = (HttpURLConnection) new URL("http://"+a_server+":"+a_port+a_prefix+a_url).openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-Agent", sm_userAgent);
		System.out.println("API call "+connection.getURL().toString());
		connection.connect();

		if(connection.getResponseCode() != 200)
			throw new APIError(connection);

		Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.getInputStream());
		Element root = null;
		NodeList nodes = dom.getChildNodes();
		for(int i=0; i<nodes.getLength(); i++)
		{
			if(nodes.item(i).getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
				continue;
			root = (Element) nodes.item(i);
			break;
		}
		
		if(root == null)
			throw new APIError("The API server sent no data.");
		
		return root;
	}
	
	/**
	 * Makes a HTTP request to the API (default URL) and returns the XML root element of the response.
	 * @param a_url
	 * @return
	 * @throws IOException
	 * @throws APIError
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	protected static Element fetch(String a_url) throws IOException, APIError, SAXException, ParserConfigurationException
	{
		return fetch(a_url, API_SERVER, API_PORT, API_PREFIX);
	}
	
	/**
	 * Makes OSM objects out of an XML element containing &lt;node&gt;, &lt;way&gt; and
	 * &lt;relation&gt; elements.
	 * @param a_root The root element.
	 * Way.cache() and Relation.cache() methods. You should use this when you aren’t
	 * working with old versions of objects.
	 * @return An ArrayList of OSM Objects. They can be cast to the sub-types by checking
	 * Object.getDOM().getTagName().
	 */
	public static ArrayList<Object> makeObjects(Element a_root)
	{
		ArrayList<Object> ret = new ArrayList<Object>();

		NodeList nodes = a_root.getChildNodes();
		for(int i=0; i<nodes.getLength(); i++)
		{
			if(nodes.item(i).getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
				continue;
			Element element = (Element)nodes.item(i);
			if(element.getTagName().equals("node"))
			{
				Node el = new Node(element);
				ret.add(el);
			}
			else if(element.getTagName().equals("way"))
			{
				Way el = new Way(element);
				ret.add(el);
			}
			else if(element.getTagName().equals("relation"))
			{
				Relation el = new Relation(element);
				ret.add(el);
			}
			else if(element.getTagName().equals("changeset"))
			{
				Changeset el = new Changeset(element);
				ret.add(el);
			}
		}

		return ret;
	}
	
	/**
	 * Fetches OSM objects from the given API URL.
	 * @param a_url For example "/node/1"
	 * @return An array of OSM Objects. They can be cast to the sub-types checking the Object.getDOM().getTagName() value.
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws APIError
	 */

	public static Object[] get(String a_url) throws IOException, SAXException, ParserConfigurationException, APIError
	{
		return makeObjects(fetch(a_url)).toArray(new Object[0]);
	}
	
	/**
	 * Joins the values of an array using a specified delimiter.
	 * @param a_delim
	 * @param a_array
	 * @return
	 */

	public static String joinStringArray(String a_delim, String[] a_array)
	{
		StringBuffer ret = new StringBuffer();
		boolean first = true;
		for(int i=0; i<a_array.length; i++)
		{
			if(a_array[i] == null)
				continue;
			if(first)
				first = false;
			else
				ret.append(a_delim);
			ret.append(a_array[i]);
		}
		return ret.toString();
	}
}