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

package de.cdauth.osm.lib.api06;

import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

import java.io.*;

/**
 * Abstract class for all objects whose information is saved in an XML DOM element.
 */
abstract public class API06XMLObject implements Externalizable
{
	/** The DOM element containing the API XML response for this object. */
	private Element m_dom;
	
	private transient API06API m_api;

	/**
	 * Only for serialization.
	 */
	@Deprecated
	public API06XMLObject()
	{
	}
	
	/**
	 * @param a_dom The DOM element.
	*/

	protected API06XMLObject(Element a_dom, API06API a_api)
	{
		m_dom = a_dom;
		m_api = a_api;
	}

	protected void setAPI(API06API a_api)
	{
		m_api = a_api;
	}
	
	/**
	 * @return The DOM element.
	 */

	protected Element getDOM()
	{
		return m_dom;
	}
	
	protected API06API getAPI()
	{
		return m_api;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException
	{
		String str = "";
		if(m_dom != null)
		{
			try {
				DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
				DOMImplementationLS impl = (DOMImplementationLS)registry.getDOMImplementation("LS");
				LSSerializer writer = impl.createLSSerializer();
				str = writer.writeToString(m_dom);
			} catch(Exception e) {
				if(e instanceof IOException)
					throw (IOException)e;
				else
					throw new IOException(e);
			}
		}
		out.writeObject(str);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		String str = (String)in.readObject();
		if(!str.equals(""))
		{
			try {
				DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
				DOMImplementationLS impl = (DOMImplementationLS)registry.getDOMImplementation("LS");
				LSParser parser = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
				LSInput input = impl.createLSInput();
				input.setStringData(str);
				m_dom = (Element)parser.parse(input).getFirstChild();
			} catch(Exception e) {
				if(e instanceof IOException)
					throw (IOException)e;
				else
					throw new IOException(e);
			}
		}
	}
}
