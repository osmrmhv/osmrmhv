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

package de.cdauth.osm.basic.api06;

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

import de.cdauth.osm.basic.API;
import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.Object;

/**
 * Provides static methods to communicate with the OSM API. Handles the HTTP connection
 * and parses XML responses.
 */

public class API06API implements API
{
	protected static final String API_SERVER = "api.openstreetmap.org";
	protected static final int API_PORT = 80;
	protected static final String API_PREFIX = "/api/0.6";

	private String m_userAgent = "cdauth’s OSM library";
	
	/**
	 * Sets the User-Agent HTTP request header for all future API requests.
	 * @param a_userAgent
	 */
	public void setUserAgent(String a_userAgent)
	{
		m_userAgent = a_userAgent;
	}
	
	public String getUserAgent()
	{
		return m_userAgent;
	}
	
	protected static String getAPIPrefix()
	{
		return "http://"+API_SERVER+":"+API_PORT+API_PREFIX;
	}

	/**
	 * Makes a HTTP request to the API and returns the XML root element of the response.
	 * @param a_url The URL to be appended to the API prefix, for example "/node/1"
	 * @return The root DOM Element of the XML response.
	 * @throws IOException There was an error communicating with the API server.
	 * @throws APIError The HTTP response code was not 200 or the XML response did not contain a root element.
	 * @throws SAXException The XML response could not be parsed.
	 * @throws ParserConfigurationException There was a problem while initialising the XML parser.
	 */
	protected Element fetch(String a_url) throws APIError
	{
		String url = getAPIPrefix()+a_url;
		try
		{
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", getUserAgent());
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
		catch(IOException e)
		{
			throw new APIError("Error fetching data from URL "+url, e);
		}
		catch(SAXException e)
		{
			throw new APIError("Error fetching data from URL "+url, e);
		}
		catch(ParserConfigurationException e)
		{
			throw new APIError("Error fetching data from URL "+url, e);
		}
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
	protected ArrayList<Object> makeObjects(Element a_root)
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
				API06Node el = new API06Node(element, this);
				ret.add(el);
			}
			else if(element.getTagName().equals("way"))
			{
				API06Way el = new API06Way(element, this);
				ret.add(el);
			}
			else if(element.getTagName().equals("relation"))
			{
				API06Relation el = new API06Relation(element, this);
				ret.add(el);
			}
			else if(element.getTagName().equals("changeset"))
			{
				API06Changeset el = new API06Changeset(element, this);
				ret.add(el);
			}
			else if(element.getTagName().equals("osmChange"))
			{
				API06ChangesetContent el = new API06ChangesetContent(element, this);
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

	protected Object[] get(String a_url) throws APIError
	{
		return makeObjects(fetch(a_url)).toArray(new Object[0]);
	}

	private API06ChangesetFactory m_changesetFactory = null;
	@Override
	public API06ChangesetFactory getChangesetFactory()
	{
		if(m_changesetFactory == null)
			m_changesetFactory = new API06ChangesetFactory(this);
		return m_changesetFactory;
	}

	private API06NodeFactory m_nodeFactory = null;
	@Override
	public API06NodeFactory getNodeFactory()
	{
		if(m_nodeFactory == null)
			m_nodeFactory = new API06NodeFactory(this);
		return m_nodeFactory;
	}

	private API06RelationFactory m_relationFactory = null;
	@Override
	public API06RelationFactory getRelationFactory()
	{
		if(m_relationFactory == null)
			m_relationFactory = new API06RelationFactory(this);
		return m_relationFactory;
	}

	private API06WayFactory m_wayFactory = null;
	@Override
	public API06WayFactory getWayFactory()
	{
		if(m_wayFactory == null)
			m_wayFactory = new API06WayFactory(this);
		return m_wayFactory;
	}
	
	private API06ChangesetContentFactory m_changesetContentFactory = null;
	public API06ChangesetContentFactory getChangesetContentFactory()
	{
		if(m_changesetContentFactory == null)
			m_changesetContentFactory = new API06ChangesetContentFactory(this);
		return m_changesetContentFactory;
	}
}