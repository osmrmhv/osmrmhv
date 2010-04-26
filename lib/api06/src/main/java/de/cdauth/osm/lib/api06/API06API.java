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

package de.cdauth.osm.lib.api06;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.cdauth.osm.lib.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.cdauth.osm.lib.Item;

/**
 * Provides static methods to communicate with the OSM API 0.6. Handles the HTTP connection
 * and parses XML responses.
 */

public class API06API implements API
{
	protected static final String API_SERVER = "api.openstreetmap.org";
	protected static final int API_PORT = 80;
	protected static final String API_PREFIX = "/api/0.6";

	private static Logger sm_logger = Logger.getLogger(API06API.class.getName());

	private String m_userAgent = "cdauth’s OSM library";

	private final DataSource m_databaseCache;

	public API06API()
	{
		m_databaseCache = null;
	}

	/**
	 * Not only caches some Items in the memory, but also uses a database for caching.
	 * @param a_databaseCache The database to use for caching.
	 */
	public API06API(DataSource a_databaseCache)
	{
		m_databaseCache = a_databaseCache;
	}

	/**
	 * Returns the database connection source to use for Item caches.
	 * @return The cache database connection source or null if no such is defined.
	 */
	public DataSource getDatabaseCache()
	{
		return m_databaseCache;
	}
	
	/**
	 * Sets the User-Agent HTTP request header for all future API requests.
	 * @param a_userAgent The user agent to use.
	 */
	public void setUserAgent(String a_userAgent)
	{
		m_userAgent = a_userAgent;
	}
	
	/**
	 * Gets the HTTP User-Agent that is currently being used.
	 * @return The user agent
	 */
	public String getUserAgent()
	{
		return m_userAgent;
	}
	
	/**
	 * Returns the full API URL where a request like <code>/node/1</code> can be appended.
	 * @return The full API URL.
	 */
	protected static String getAPIPrefix()
	{
		return "http://"+API_SERVER+":"+API_PORT+API_PREFIX;
	}

	/**
	 * Makes a HTTP request to the API and returns the XML root element of the response.
	 * @param a_url The URL to be appended to the API prefix, for example "/node/1"
	 * @return The root DOM Element of the XML response.
	 * @throws APIError There was a connection problem or the server sent unexpected data.
	 */
	protected Element fetch(String a_url) throws APIError
	{
		String url = getAPIPrefix()+a_url;
		try
		{
			if(sm_logger.isLoggable(Level.FINE))
				sm_logger.fine(url);

			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", getUserAgent());
			connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			System.out.println("API call "+connection.getURL().toString());
			connection.connect();
	
			if(connection.getResponseCode() != 200)
				throw new APIError("ResponseCode is "+connection.getResponseCode()+" for URL "+url+".");

			InputStream in = connection.getInputStream();
			String encoding = connection.getContentEncoding();
			if("gzip".equalsIgnoreCase(encoding))
				in = new GZIPInputStream(in);
			else if("delfate".equalsIgnoreCase(encoding))
				in = new DeflaterInputStream(in);
	
			Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
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
	 * Makes OSM objects out of an XML element containing &lt;node&gt;, &lt;way&gt;,
	 * &lt;relation&gt; and &lt;changeset&gt; elements. Do not forget to call {@link API06GeographicalItem#markAsCurrent}
	 * afterwards if appropriate.
	 * @param a_root The root element.
	 * @return An ArrayList of OSM Objects.
	 */
	protected ArrayList<Item> makeObjects(Element a_root)
	{
		ArrayList<Item> ret = new ArrayList<Item>();

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
		}

		return ret;
	}
	
	/**
	 * Fetches OSM objects from the given API URL. Do not forget to call {@link API06GeographicalItem#markAsCurrent}
	 * afterwards if appropriate.
	 * @param a_url For example "/node/1"
	 * @return An array of OSM Objects. They can be cast to the sub-types checking the Item.getDOM().getTagName() value.
	 * @throws APIError There was an error communicating with the API.
	 */

	protected Item[] get(String a_url) throws APIError
	{
		return makeObjects(fetch(a_url)).toArray(new Item[0]);
	}

	private transient API06ChangesetFactory m_changesetFactory = null;
	
	@Override
	public API06ChangesetFactory getChangesetFactory()
	{
		if(m_changesetFactory == null)
			m_changesetFactory = new API06ChangesetFactory(this);
		return m_changesetFactory;
	}

	private transient API06NodeFactory m_nodeFactory = null;
	
	@Override
	public API06NodeFactory getNodeFactory()
	{
		if(m_nodeFactory == null)
			m_nodeFactory = new API06NodeFactory(this);
		return m_nodeFactory;
	}

	private transient API06RelationFactory m_relationFactory = null;
	
	@Override
	public API06RelationFactory getRelationFactory()
	{
		if(m_relationFactory == null)
			m_relationFactory = new API06RelationFactory(this);
		return m_relationFactory;
	}

	private transient API06WayFactory m_wayFactory = null;
	
	@Override
	public API06WayFactory getWayFactory()
	{
		if(m_wayFactory == null)
			m_wayFactory = new API06WayFactory(this);
		return m_wayFactory;
	}

	@Override
	public GeographicalItem[] fetchBoundingBox(BoundingBox a_boundingBox)
			throws APIError
	{
		Item[] items = get("/map?bbox="+a_boundingBox.getLeft()+","+a_boundingBox.getBottom()+","+a_boundingBox.getRight()+","+a_boundingBox.getTop());
		for(Item it : items)
		{
			if(it instanceof API06GeographicalItem) // should always be true
				((API06GeographicalItem)it).markAsCurrent();

			if(it instanceof Node)
				getNodeFactory().getCache().cacheObject((Node)it);
			else if(it instanceof Way)
				getWayFactory().getCache().cacheObject((Way)it);
			else if(it instanceof Relation)
				getRelationFactory().getCache().cacheObject((Relation)it);
		}
		
		return (GeographicalItem[]) items;
	}
}
