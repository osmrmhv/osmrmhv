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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.junit.Test;
import static org.junit.Assert.*;

public class SerializationTest
{
	public <T extends Serializable> T reconstruct(T a_object) throws Exception
	{
		ByteArrayOutputStream ser = new ByteArrayOutputStream();
		BufferedOutputStream ser2 = new BufferedOutputStream(ser);
		ObjectOutputStream ser3 = new ObjectOutputStream(ser2);
		ser3.writeObject(a_object);
		ser3.close();

		ByteArrayInputStream ser4 = new ByteArrayInputStream(ser.toByteArray());
		ObjectInputStream ser5 = new ObjectInputStream(ser4);
		T ret = (T) ser5.readObject();
		ser5.close();

		return ret;
	}

	@Test
	public void node() throws Exception
	{
		API06Node node = XMLReadTest.makeNode();
		API06Node clone = reconstruct(node);

		assertEquals(node, clone);
		assertEquals(node.getID(), clone.getID());
		assertEquals(node.getLonLat(), clone.getLonLat());
		assertEquals(node.getVersion(), clone.getVersion());
		assertEquals(node.getChangeset(), clone.getChangeset());
		assertEquals(node.getTimestamp(), clone.getTimestamp());
		assertEquals(node.getTags(), clone.getTags());
	}
}
