package de.cdauth.osm.basic;

public interface GeographicalObject extends Object
{
	public Relation[] getContainingRelations() throws APIError;
}
