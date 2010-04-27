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

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import eu.cdauth.osm.lib.*;
import eu.cdauth.osm.lib.GeographicalItem;

public class API06RelationFactory extends API06GeographicalItemFactory<Relation> implements RelationFactory
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
		VersionedItemCache<Node> nodeCache = getAPI().getNodeFactory().getCache();
		VersionedItemCache<Way> wayCache = getAPI().getWayFactory().getCache();
		VersionedItemCache<Relation> relationCache = getCache();
		
		boolean downloadNecessary = true;
		if(getCache().getObject(a_id) != null)
		{
			downloadNecessary = false;
			for(RelationMember it : fetch(a_id).getMembers())
			{
				Class<? extends GeographicalItem> type = it.getType();
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
				if(object instanceof API06GeographicalItem)
					((API06GeographicalItem)object).markAsCurrent();

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
					Class<? extends GeographicalItem> type = member.getType();
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
}
