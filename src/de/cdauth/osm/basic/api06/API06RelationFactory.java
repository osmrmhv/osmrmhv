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

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import de.cdauth.osm.basic.APIError;
import de.cdauth.osm.basic.Changeset;
import de.cdauth.osm.basic.ChangesetFactory;
import de.cdauth.osm.basic.GeographicalObject;
import de.cdauth.osm.basic.ID;
import de.cdauth.osm.basic.Node;
import de.cdauth.osm.basic.NodeFactory;
import de.cdauth.osm.basic.Relation;
import de.cdauth.osm.basic.RelationFactory;
import de.cdauth.osm.basic.RelationMember;
import de.cdauth.osm.basic.Segment;
import de.cdauth.osm.basic.Version;
import de.cdauth.osm.basic.VersionedObjectCache;
import de.cdauth.osm.basic.Way;
import de.cdauth.osm.basic.WayFactory;

public class API06RelationFactory extends API06GeographicalObjectFactory<Relation> implements RelationFactory
{
	protected static final String TYPE = "relation";

	protected API06RelationFactory(API06API a_api)
	{
		super(a_api, TYPE);
	}
	
	/**
	 * Ensures that all members of the relation are downloaded and cached. This saves a lot of time when accessing them with fetch(), as fetch() makes an API call for each uncached item whereas this method can download all members at once.
	 * @param a_id
	 * @throws APIError
	 */
	
	public void downloadFull(ID a_id) throws APIError
	{
		VersionedObjectCache<Node> nodeCache = getAPI().getNodeFactory().getCache();
		VersionedObjectCache<Way> wayCache = getAPI().getWayFactory().getCache();
		VersionedObjectCache<Relation> relationCache = getCache();
		
		boolean downloadNecessary = true;
		if(getCache().getObject(a_id) != null)
		{
			downloadNecessary = false;
			for(RelationMember it : fetch(a_id).getMembers())
			{
				Class<? extends GeographicalObject> type = it.getType();
				ID id = it.getReferenceID();
				boolean isCached = true;
				if(type.equals(Node.class))
					isCached = (nodeCache.getObject(id) != null);
				else if(type.equals(Way.class))
					isCached = (wayCache.getObject(id) != null);
				else if(type.equals(Relation.class))
					isCached = (relationCache.getObject(id) != null);
				if(!isCached)
				{
					downloadNecessary = true;
					break;
				}
			}
		}
		
		if(downloadNecessary)
		{
			Object[] fetched = getAPI().get("/relation/"+a_id+"/full");
			for(Object object : fetched)
			{
				if(object instanceof API06GeographicalObject)
					((API06GeographicalObject)object).markAsCurrent();

				if(object instanceof Node)
					nodeCache.cacheObject((Node) object);
				else if(object instanceof Way)
					wayCache.cacheObject((Way) object);
				else if(object instanceof Relation)
					relationCache.cacheObject((Relation) object);
			}
		}
	}
	
	/**
	 * Downloads all members of this relation and its sub-relations. Acts like a full download, but recursive. The number of API requests is optimised for the case of many sub-relations: as many relations as possible are downloaded in each API request, all ways and all nodes are downloaded in one additional request for both.
	 * As the OSM API seems to be optimised for full fetches and takes quite a time to respond to the Way and Node request, this function may save requests but not time (at least with the current speed of the API).
	 * @param a_id
	 * @throws APIError
	 */
	
	public void downloadRecursive(ID a_id) throws APIError
	{
		HashSet<ID> checkRelations = new HashSet<ID>();
		HashSet<ID> downloadRelations = new HashSet<ID>();
		HashSet<ID> containedRelations = new HashSet<ID>();
		
		HashSet<ID> downloadWays = new HashSet<ID>();
		HashSet<ID> downloadNodes = new HashSet<ID>();

		if(getCache().getObject(a_id) == null)
			downloadFull(a_id);
		
		checkRelations.add(a_id);
		while(checkRelations.size() > 0)
		{
			for(ID id : checkRelations)
			{
				for(RelationMember member : fetch(id).getMembers())
				{
					Class<? extends GeographicalObject> type = member.getType();
					ID refId = member.getReferenceID();
					if(type.equals(Relation.class) && !containedRelations.contains(refId))
						downloadRelations.add(refId);
					else if(type.equals(Way.class))
						downloadWays.add(refId);
					else if(type.equals(Node.class))
						downloadNodes.add(refId);
				}
			}
			if(downloadRelations.size() == 1)
			{
				ID one = downloadRelations.iterator().next();
				if(getCache().getObject(one) == null)
					downloadFull(one);
			}
			else if(downloadRelations.size() > 1)
				fetch(downloadRelations.toArray(new ID[0]));
			
			containedRelations.addAll(downloadRelations);
			checkRelations = downloadRelations;
			downloadRelations = new HashSet<ID>();
		}
		
		WayFactory wayFactory = getAPI().getWayFactory();
		wayFactory.fetch(downloadWays.toArray(new ID[0]));
		
		for(ID id : downloadWays)
		{
			for(ID nodeID : wayFactory.fetch(id).getMembers())
				downloadNodes.add(nodeID);
		}
		
		NodeFactory nodeFactory = getAPI().getNodeFactory();
		nodeFactory.fetch(downloadNodes.toArray(new ID[0]));
	}
	
	public Hashtable<Segment,Changeset> blame(ID a_id) throws APIError
	{
		NodeFactory nodeFactory = getAPI().getNodeFactory();
		WayFactory wayFactory = getAPI().getWayFactory();
		RelationFactory relationFactory = this;
		ChangesetFactory changesetFactory = getAPI().getChangesetFactory();

		Relation currentRelation = relationFactory.fetchHistory(a_id).lastEntry().getValue();
		
		Date currentDate = null;
		Date nextDate = null;
		ID currentChangeset = null;
		ID nextChangeset = null;
		
		List<Segment> currentSegments = Arrays.asList(currentRelation.getSegmentsRecursive(null));
		Hashtable<Segment,ID> blame = new Hashtable<Segment,ID>();
		
		while(true)
		{
			Relation mainRelation = null;
			Relation mainRelationOlder = null;

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
				mainRelation = relationFactory.fetch(a_id, currentDate);
				mainRelationOlder = fetch(a_id, new Date(currentDate.getTime()-1000));
			}
			
			if(mainRelation == null)
				break;
			
			if(mainRelationOlder != null)
			{
				nextDate = mainRelationOlder.getTimestamp();
				nextChangeset = mainRelationOlder.getChangeset();
			}
			
			Node[] nodeMembers = mainRelation.getNodesRecursive(currentDate);
			for(int i=0; i<nodeMembers.length; i++)
			{
				NavigableMap<Version,Node> nodeHistory = nodeFactory.fetchHistory(nodeMembers[i].getID());
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
			for(int i=0; i<wayMembers.length; i++)
			{
				NavigableMap<Version,Way> wayHistory = wayFactory.fetchHistory(wayMembers[i].getID());
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
			for(int i=0; i<relationMembers.length; i++)
			{
				NavigableMap<Version,Relation> relationHistory = relationFactory.fetchHistory(relationMembers[i].getID());
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
			
			List<Segment> segments = Arrays.asList(mainRelation.getSegmentsRecursive(currentDate));
			segments.retainAll(currentSegments);

			if(segments.size() == 0)
				break;
			
			if(currentChangeset != null)
			{
				for(Segment segment : segments)
					blame.put(segment, currentChangeset);
			}
		}
		
		Map<ID,Changeset> changesets = changesetFactory.fetch(blame.values().toArray(new ID[0]));
		Hashtable<Segment,Changeset> ret = new Hashtable<Segment,Changeset>();
		for(Map.Entry<Segment,ID> e : blame.entrySet())
			ret.put(e.getKey(), changesets.get(e.getValue()));
		
		return ret;
	}
}
