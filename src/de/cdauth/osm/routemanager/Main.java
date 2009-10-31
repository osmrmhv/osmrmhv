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

package de.cdauth.osm.routemanager;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import de.cdauth.cmdargs.Argument;
import de.cdauth.cmdargs.ArgumentParser;
import de.cdauth.cmdargs.ArgumentParserException;
import de.cdauth.osm.basic.API;
import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.Relation;
import de.cdauth.osm.basic.RelationSegment;

public class Main
{
	private static Cache sm_cache = null;

	public static void main(String[] a_args) throws ArgumentParserException, ClassNotFoundException, SQLException, IOException, APIError, SAXException, ParserConfigurationException, ParseException
	{
		API.setUserAgent("OSM Route Manager");

		ArgumentParser arguments = new ArgumentParser("OSM Route Manager");
		
		Argument token_relation = new Argument('r', "relation");
		token_relation.setDescription("The relation ID to be parsed.");
		token_relation.setParameter(Argument.PARAMETER_REQUIRED);
		token_relation.setRequired(true);
		arguments.addArgument(token_relation);
		
		Argument token_cache = new Argument('c', "cache");
		token_cache.setDescription("The SQLite file to be used as cache.");
		token_cache.setParameter(Argument.PARAMETER_REQUIRED);
		token_cache.setRequired(true);
		arguments.addArgument(token_cache);
		
		arguments.parseArguments(a_args);
		
		System.out.println("This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.");
		System.out.println("This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.");
		System.out.println("You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.");
		System.out.println("");
		
		sm_cache = new Cache(token_cache.parameter());
		
		Calendar downloadTime = Calendar.getInstance();
		Relation.downloadFull(token_relation.parameter());
		Relation relation = Relation.fetch(token_relation.parameter()); 
		RelationSegment[] segmentated = relation.segmentate();
		
		System.out.println("Successfully parsed relation "+relation.getDOM().getAttribute("id")+".");
		double totalLength = 0;
		for(int i=0; i<segmentated.length; i++)
		{
			System.out.println("Segment "+i+": "+Math.round(segmentated[i].getDistance()*1000)+" m, "+segmentated[i].getNodes().length+" nodes, first node "+segmentated[i].getEnd1()+", last node "+segmentated[i].getEnd2());
			totalLength += segmentated[i].getDistance();
		}
		System.out.println("----");
		System.out.println("Total length: "+Math.round(totalLength*1000)+" m");
		
		sm_cache.cacheRelationSegments(segmentated, relation, downloadTime);
	}
	
	public static Cache getCache()
	{
		return sm_cache;
	}
}
