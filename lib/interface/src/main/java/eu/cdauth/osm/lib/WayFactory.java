/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package eu.cdauth.osm.lib;

/**
 * Implentations of this type fetch {@link Way} objects from the {@link API}.
 * @see {@link API#getWayFactory()}
 * @author cdauth
 */
public interface WayFactory extends VersionedItemFactory<Way>
{

}
