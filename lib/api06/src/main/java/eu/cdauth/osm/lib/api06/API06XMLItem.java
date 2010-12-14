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

import org.w3c.dom.Element;

import java.io.*;

/**
 * Abstract class for all objects whose information is saved in an XML DOM element.
 */
abstract public class API06XMLItem implements Serializable
{
	private transient API06API m_api;
	
	/**
	 * @param a_dom The DOM element.
	 * @param a_api The API that creates this object.
	*/

	protected API06XMLItem(Element a_dom, API06API a_api)
	{
		m_api = a_api;
	}

	protected void setAPI(API06API a_api)
	{
		m_api = a_api;
	}
	
	protected API06API getAPI()
	{
		return m_api;
	}
}
