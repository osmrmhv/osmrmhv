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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import eu.cdauth.osm.lib.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.cdauth.osm.lib.GeographicalItem;

public class API06Relation extends API06GeographicalItem implements Relation
{
	/**
	 * Only for serialization.
	 */
	@Deprecated
	public API06Relation()
	{
	}

	protected API06Relation(Element a_dom, API06API a_api)
	{
		super(a_dom, a_api);
	}

	@Override
	public API06RelationMember[] getMembers()
	{
		NodeList members = getDOM().getElementsByTagName("member");
		API06RelationMember[] ret = new API06RelationMember[members.getLength()];
		for(int i=0; i<members.getLength(); i++)
			ret[i] = new API06RelationMember((Element) members.item(i), getAPI(), this);
		return ret;
	}
	
	/**
	 * Returns an array of all ways and nodes that are contained in this relation and all of its
	 * sub-relations. You may want to call downloadRecursive() first.
	 * @param a_date The date to use to fetch the members. Set to null to fetch the current member versions (which is a lot faster).
	 * @param a_members Set to null. Is filled with the result and passed along the recursive calls of this function.
	 * @param a_ignoreRelations Set to null. Is passed along the recursive calls to processing a relation twice and thus produce an infinite loop.
	 * @return A set of the members of this relation.
	 * @throws APIError There was an error communicating with the API
	 */
	private HashSet<GeographicalItem> getMembersRecursive(Date a_date, HashSet<GeographicalItem> a_members, HashSet<ID> a_ignoreRelations) throws APIError
	{
		if(a_members == null)
			a_members = new HashSet<GeographicalItem>();
		if(a_ignoreRelations == null)
			a_ignoreRelations = new HashSet<ID>();
		a_ignoreRelations.add(getID());
		
		if(a_date == null)
			getAPI().getRelationFactory().downloadFull(getID());

		for(API06RelationMember it : getMembers())
		{
			Class<? extends GeographicalItem> type = it.getType();
			ID id = it.getReferenceID();
			if(type.equals(Way.class))
			{
				Way obj = (a_date == null ? getAPI().getWayFactory().fetch(id) : getAPI().getWayFactory().fetch(id, a_date));
				a_members.add(obj);
			}
			else if(type.equals(Node.class))
			{
				Node obj = (a_date == null ? getAPI().getNodeFactory().fetch(id) : getAPI().getNodeFactory().fetch(id, a_date));
				a_members.add(obj);
			}
			else if(type.equals(Relation.class) && !a_ignoreRelations.contains(id))
			{
				a_members.add(a_date == null ? getAPI().getRelationFactory().fetch(id) : getAPI().getRelationFactory().fetch(id, a_date));
				if(a_date == null)
					((API06Relation)getAPI().getRelationFactory().fetch(id)).getMembersRecursive(a_date, a_members, a_ignoreRelations);
				else
					((API06Relation)getAPI().getRelationFactory().fetch(id, a_date)).getMembersRecursive(a_date, a_members, a_ignoreRelations);
			}
		}
		return a_members;
	}
	
	@Override
	public GeographicalItem[] getMembersRecursive(Date a_date) throws APIError
	{
		Set<GeographicalItem> ret = getMembersRecursive(a_date, null, null);
		return ret.toArray(new GeographicalItem[ret.size()]);
	}

	@Override
	public Way[] getWaysRecursive(Date a_date) throws APIError
	{
		HashSet<GeographicalItem> members = getMembersRecursive(a_date, null, null);
		ArrayList<Way> ret = new ArrayList<Way>();
		for(GeographicalItem member : members)
		{
			if(member instanceof Way)
				ret.add((Way) member);
		}
		return ret.toArray(new Way[ret.size()]);
	}

	@Override
	public Node[] getNodesRecursive(Date a_date) throws APIError
	{
		HashSet<GeographicalItem> members = getMembersRecursive(a_date, null, null);
		ArrayList<Node> ret = new ArrayList<Node>();
		for(GeographicalItem member : members)
		{
			if(member instanceof Node)
				ret.add((Node) member);
		}
		return ret.toArray(new Node[ret.size()]);
	}
	
	public Relation[] getRelationsRecursive(Date a_date) throws APIError
	{
		HashSet<GeographicalItem> members = getMembersRecursive(a_date, null, null);
		ArrayList<Relation> ret = new ArrayList<Relation>();
		for(GeographicalItem member : members)
		{
			if(member instanceof Relation)
				ret.add((Relation) member);
		}
		return ret.toArray(new Relation[ret.size()]);
	}

	@Override
	public Segment[] getSegmentsRecursive(Date a_date) throws APIError
	{
		HashSet<Segment> ret = new HashSet<Segment>();

		Node[] nodes = getNodesRecursive(a_date);
		for(Node node : nodes)
			ret.add(new Segment(node, node));
		
		Way[] ways = getWaysRecursive(a_date);
		for(Way way : ways)
		{
			Node[] members = way.getMemberNodes(a_date);
			Node lastNode = null;
			for(Node member : members)
			{
				if(lastNode != null)
					ret.add(new Segment(lastNode, member));
				lastNode = member;
			}
		}
		
		return ret.toArray(new Segment[ret.size()]);
	}
}
