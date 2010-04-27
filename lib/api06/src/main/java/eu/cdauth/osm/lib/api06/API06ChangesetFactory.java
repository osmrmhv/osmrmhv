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

import java.util.Hashtable;
import java.util.Map;

import eu.cdauth.osm.lib.APIError;
import eu.cdauth.osm.lib.Changeset;
import eu.cdauth.osm.lib.ChangesetFactory;
import eu.cdauth.osm.lib.ID;

public class API06ChangesetFactory extends API06ItemFactory<Changeset> implements ChangesetFactory
{
	protected static final String TYPE = "changeset";
	
	protected API06ChangesetFactory(API06API a_api)
	{
		super(a_api, TYPE);
	}

	@Override
	public Map<ID,Changeset> fetch(ID[] a_ids) throws APIError
	{
		// We can only fetch one changeset at a time
		Map<ID,Changeset> ret = new Hashtable<ID,Changeset>();
		for(ID id : a_ids)
			ret.put(id, fetch(id));
		return ret;
	}
	
	@Override
	public Changeset fetch(ID a_id) throws APIError
	{
		return super.fetch(new ID[]{ a_id }).get(a_id);
	}
}
