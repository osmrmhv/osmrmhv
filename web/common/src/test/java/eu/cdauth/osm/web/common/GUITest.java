/*
	This file is part of the OSM Route Manager and History Viewer.

	OSM Route Manager and History Viewer is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	OSM Route Manager and History Viewer is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.

	You should have received a copy of the GNU Affero General Public License
	along with this software. If not, see <http://www.gnu.org/licenses/>.
*/

package eu.cdauth.osm.web.common;

import java.util.Hashtable;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class GUITest
{
	GUI gui = new GUI(null, null) {
		@Override
		protected String getResourceName()
		{
			return "test";
		}

		@Override
		protected Map<String, String> getLocales()
		{
			return new Hashtable();
		}
	};

	public GUITest()
	{
	}

	@Test
	public void numberFormatTest()
	{
		assertEquals("0.616", gui.formatNumber(0.616, 3));
		assertEquals("1", gui.formatNumber(0.616, 0));
		assertEquals("0.62", gui.formatNumber(0.616, 2));
		assertEquals("0.61600", gui.formatNumber(0.616, 5));

		assertEquals("0.612", gui.formatNumber(0.612, 3));
		assertEquals("1", gui.formatNumber(0.612, 0));
		assertEquals("0.61", gui.formatNumber(0.612, 2));
		assertEquals("0.61200", gui.formatNumber(0.612, 5));

		assertEquals("143.616", gui.formatNumber(143.616, 3));
		assertEquals("144", gui.formatNumber(143.616, 0));
		assertEquals("143.62", gui.formatNumber(143.616, 2));
		assertEquals("143.61600", gui.formatNumber(143.616, 5));

		assertEquals("143.612", gui.formatNumber(143.612, 3));
		assertEquals("144", gui.formatNumber(143.612, 0));
		assertEquals("143.61", gui.formatNumber(143.612, 2));
		assertEquals("143.61200", gui.formatNumber(143.612, 5));

		assertEquals("−0.616", gui.formatNumber(-0.616, 3));
		assertEquals("−1", gui.formatNumber(-0.616, 0));
		assertEquals("−0.62", gui.formatNumber(-0.616, 2));
		assertEquals("−0.61600", gui.formatNumber(-0.616, 5));

		assertEquals("−0.612", gui.formatNumber(-0.612, 3));
		assertEquals("−1", gui.formatNumber(-0.612, 0));
		assertEquals("−0.61", gui.formatNumber(-0.612, 2));
		assertEquals("−0.61200", gui.formatNumber(-0.612, 5));

		assertEquals("−143.616", gui.formatNumber(-143.616, 3));
		assertEquals("−144", gui.formatNumber(-143.616, 0));
		assertEquals("−143.62", gui.formatNumber(-143.616, 2));
		assertEquals("−143.61600", gui.formatNumber(-143.616, 5));

		assertEquals("−143.612", gui.formatNumber(-143.612, 3));
		assertEquals("−144", gui.formatNumber(-143.612, 0));
		assertEquals("−143.61", gui.formatNumber(-143.612, 2));
		assertEquals("−143.61200", gui.formatNumber(-143.612, 5));
	}
}