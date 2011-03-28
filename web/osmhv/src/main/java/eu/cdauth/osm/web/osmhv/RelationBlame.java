/*
	This file is part of the OSM History Viewer.

	OSM History Viewer is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	OSM History Viewer is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.

	You should have received a copy of the GNU Affero General Public License
	along with this software. If not, see <http://www.gnu.org/licenses/>.

	Copyright © 2010 Candid Dauth
*/

package eu.cdauth.osm.web.osmhv;

import eu.cdauth.osm.lib.API;
import eu.cdauth.osm.lib.APIError;
import eu.cdauth.osm.lib.Changeset;
import eu.cdauth.osm.lib.ChangesetFactory;
import eu.cdauth.osm.lib.ID;
import eu.cdauth.osm.lib.Node;
import eu.cdauth.osm.lib.NodeFactory;
import eu.cdauth.osm.lib.Relation;
import eu.cdauth.osm.lib.RelationFactory;
import eu.cdauth.osm.lib.Segment;
import eu.cdauth.osm.lib.Version;
import eu.cdauth.osm.lib.Way;
import eu.cdauth.osm.lib.WayFactory;
import eu.cdauth.osm.lib.api06.API06API;
import eu.cdauth.osm.web.common.Cache;
import eu.cdauth.osm.web.common.Queue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author cdauth
 */
public class RelationBlame implements Serializable
{
	private static final Logger sm_logger = Logger.getLogger(RelationBlame.class.getName());

	public static Cache<RelationBlame> cache = null;
	public static final Queue.Worker worker = new Queue.Worker() {
		public void work(ID a_id)
		{
			try
			{
				RelationBlame route = new RelationBlame(a_id);
				cache.saveEntry(a_id.toString(), route);
			}
			catch(Exception e)
			{
				sm_logger.log(Level.WARNING, "Could not save blame of relation "+a_id+".", e);
			}
		}
	};

	public Map<Segment,Changeset> segmentChangeset = null;
	public Date timestamp = null;
	public Throwable exception = null;

	public RelationBlame(ID a_relationId)
	{
		try
		{
			API api = new API06API();
			
			NodeFactory nodeFactory = api.getNodeFactory();
			WayFactory wayFactory = api.getWayFactory();
			RelationFactory relationFactory = api.getRelationFactory();
			ChangesetFactory changesetFactory = api.getChangesetFactory();

			Relation currentRelation = relationFactory.fetchHistory(a_relationId).lastEntry().getValue();

			timestamp = currentRelation.getTimestamp();

			Date currentDate = null;
			Date nextDate = null;
			ID currentChangeset = null;
			ID nextChangeset = null;

			List<Segment> currentSegments = Arrays.asList(currentRelation.getSegmentsRecursive(null));
			Hashtable<Segment,ID> blame = new Hashtable<Segment,ID>();

			while(true)
			{
				Relation mainRelation;
				Relation mainRelationOlder;

				if(nextDate == null)
				{ // First run
					mainRelation = currentRelation;
					mainRelationOlder = currentRelation;
				}
				else
				{
					currentDate = nextDate;
					nextDate = null;
					currentChangeset = nextChangeset;
					nextChangeset = null;
					mainRelation = relationFactory.fetch(a_relationId, currentDate);
					mainRelationOlder = relationFactory.fetch(a_relationId, new Date(currentDate.getTime()-1000));
				}

				if(mainRelation == null)
					break;

				if(mainRelationOlder != null)
				{
					nextDate = mainRelationOlder.getTimestamp();
					nextChangeset = mainRelationOlder.getChangeset();
				}

				Node[] nodeMembers = mainRelation.getNodesRecursive(currentDate);
				for(Node nodeMember : nodeMembers)
				{
					NavigableMap<Version,Node> nodeHistory = nodeFactory.fetchHistory(nodeMember.getID());
					for(Node node : nodeHistory.values())
					{
						Date nodeDate = node.getTimestamp();
						if((currentDate == null || nodeDate.compareTo(currentDate) < 0) && (nextDate == null || nodeDate.compareTo(nextDate) > 0))
						{
							nextDate = nodeDate;
							nextChangeset = node.getChangeset();
						}
					}
				}

				Way[] wayMembers = mainRelation.getWaysRecursive(currentDate);
				for(Way wayMember : wayMembers)
				{
					NavigableMap<Version,Way> wayHistory = wayFactory.fetchHistory(wayMember.getID());
					for(Way way : wayHistory.values())
					{
						Date wayDate = way.getTimestamp();
						if((currentDate == null || wayDate.compareTo(currentDate) < 0) && (nextDate == null || wayDate.compareTo(nextDate) > 0))
						{
							nextDate = wayDate;
							nextChangeset = way.getChangeset();
						}
					}
				}

				Relation[] relationMembers = mainRelation.getRelationsRecursive(currentDate);
				for(Relation relationMember : relationMembers)
				{
					NavigableMap<Version,Relation> relationHistory = relationFactory.fetchHistory(relationMember.getID());
					for(Relation relation : relationHistory.values())
					{
						Date relationDate = relation.getTimestamp();
						if((currentDate == null || relationDate.compareTo(currentDate) < 0) && (nextDate == null || relationDate.compareTo(nextDate) > 0))
						{
							nextDate = relationDate;
							nextChangeset = relation.getChangeset();
						}
					}
				}

				List<Segment> segments = new ArrayList<Segment>();
				segments.addAll(Arrays.asList(mainRelation.getSegmentsRecursive(currentDate)));
				segments.retainAll(currentSegments);

				if(segments.size() == 0)
					break;

				if(currentChangeset != null)
				{
					for(Segment segment : segments)
						blame.put(segment, currentChangeset);
				}
			}

			Map<ID,Changeset> changesets = changesetFactory.fetch(blame.values().toArray(new ID[blame.size()]));
			segmentChangeset = new Hashtable<Segment,Changeset>();
			for(Map.Entry<Segment,ID> e : blame.entrySet())
				segmentChangeset.put(e.getKey(), changesets.get(e.getValue()));
		}
		catch(Throwable e)
		{ // Including OutOfMemoryError
			exception = e;
		}
	}
}
