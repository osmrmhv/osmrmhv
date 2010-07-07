/*
	Copyright © 2010 Candid Dauth

	Permission is hereby granted, free of charge, to any person obtaining
	a copy of this software and associated documentation files (the “Software”),
	to deal in the Software without restriction, including without limitation
	the rights to use, copy, modify, merge, publish, distribute, sublicense,
	and/or sell copies of the Software, and to permit persons to whom the Software
	is furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
	INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
	PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
	HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
	OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
	SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package eu.cdauth.osm.lib.api06;

import eu.cdauth.osm.lib.Item;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLReadTest
{
	public static final API06API sm_api = new API06API();

	public static API06Item makeItem(String a_xml) throws Exception
	{
		Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(a_xml.getBytes("UTF-8")));
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
			throw new IOException("No root element.");

		List<Item> items = sm_api.makeObjects(root);
		if(items.size() < 1)
			throw new IOException("No items.");

		return (API06Item)items.get(0);
	}

	public static API06Node makeNode() throws Exception
	{
		return (API06Node)makeItem("<osm version=\"0.6\" generator=\"OpenStreetMap server\">" +
			"<node id=\"123456\" lat=\"51.2153688\" lon=\"4.089274\" version=\"8\" changeset=\"359445\" user=\"Benjamiini\" uid=\"65172\" visible=\"true\" timestamp=\"2008-12-13T15:05:11Z\">" +
			"<tag k=\"tourism\" v=\"camp_site\"/>" +
			"<tag k=\"name\" v=\"Bariş Camping\"/>" +
			"</node>" +
			"</osm>");
	}

	@Test
	public void node() throws Exception
	{
		API06Node node = makeNode();

		assertEquals(node.getID().asLong().longValue(), 123456L);
		assertEquals(node.getLonLat().getLon(), 4.089274D, 0.00000005D);
		assertEquals(node.getLonLat().getLat(), 51.2153688D, 0.00000005D);
		assertEquals(node.getVersion().asLong().longValue(), 8L);
		assertEquals(node.getChangeset().asLong().longValue(), 359445L);
		assertEquals(node.getTimestamp().getTime(), 1229180711000L);
		assertEquals(node.getTags().size(), 2);
		assertEquals(node.getTag("tourism"), "camp_site");
		assertEquals(node.getTag("name"), "Bariş Camping");
	}
}
