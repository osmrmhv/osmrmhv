/*
	This file is in the public domain, furnished “as is”, without technical
	support, and with no warranty, express or implied, as to its usefulness for
	any purpose.
*/

package eu.cdauth.osm.lib;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * With this class you can easily cache OSM objects you retrieved from the API and that are not versioned. There
 * is a maximum number of cached values {@link #MAX_CACHED_VALUES} and a maximum age {@link #MAX_AGE}. Run
 * {@link #cleanUpAll} regularly to apply these limits.
 * 
 * <p>This class is to be used by the API implementations. These should check if objects are cached before fetching
 * them from the API. As this class looks up cached objects only by their ID, you need multiple instances for different
 * object types (ID scopes).
 * @author Candid Dauth
 */

public class ItemCache<T extends Item>
{
	/**
	 * How many entries may be in the cache?
	 */
	public static final int MAX_CACHED_VALUES = 100;
	/**
	 * How old may the entries in the cache be at most? (seconds)
	 */
	public static final int MAX_AGE = 600;

	/**
	 * How many entries may be in the database cache?
	 */
	public static final int MAX_DATABASE_VALUES = 50000;
	/**
	 * How old may the entries in the database cache be at most? (seconds)
	 */
	public static final int MAX_DATABASE_AGE = 1800;

	private static final Logger sm_logger = Logger.getLogger(ItemCache.class.getName());

	private final String m_persistenceID;
	private DataSource m_dataSource = null;
	private final Set<ID> m_databaseCache = Collections.synchronizedSet(new HashSet<ID>());
	
	private static final Map<ItemCache<? extends Item>, java.lang.Object> sm_instances = Collections.synchronizedMap(new WeakHashMap<ItemCache<? extends Item>, java.lang.Object>());

	private final Map<ID,T> m_cache = new Hashtable<ID,T>();

	/**
	 * Caches the time stamp ({@link System#currentTimeMillis()}) when a entry is saved to the cache. Needed for all
	 * clean up methods.
	 */
	private final ValueSortedMap<ID,Long> m_cacheTimes = new ValueSortedMap<ID,Long>();

	/**
	 * Creates a cache that does not use a database but only stores several items in the memory.
	 */
	public ItemCache()
	{
		this(null, null);
	}

	/**
	 * Creates a cache that uses a database backend to have additional capacity.
	 * @param a_dataSource The database connection to use.
	 * @param a_persistenceID An ID to refer to this cache object in the database. No two caches with the same ID may exist at
	 *                        the same time.
	 */
	public ItemCache(DataSource a_dataSource, String a_persistenceID)
	{
		m_dataSource = a_dataSource;
		m_persistenceID = a_persistenceID;

		if(getPersistenceID() != null)
			updateDatabaseCacheList();

		synchronized(sm_instances)
		{
			sm_instances.put(this, null);
		}
	}

	/**
	 * Returns the object with the ID a_id. For versioned objects returns the version that is known to be the current
	 * one.
	 * @param a_id The ID of the object.
	 * @return The object or null if it is not in the cache.
	 */
	public T getObject(ID a_id)
	{
		T ret = null;
		synchronized(m_cache)
		{
			ret = m_cache.get(a_id);
		}

		if(ret == null)
		{
			synchronized(m_databaseCache)
			{
				if(m_databaseCache.contains(a_id))
				{
					Connection conn = null;
					try
					{
						String persistenceID = getPersistenceID();
						conn = getConnection();
						if(persistenceID != null && conn != null)
						{
							PreparedStatement stmt = conn.prepareStatement("SELECT \"data\" FROM \"osmrmhv_cache\" WHERE \"cache_id\" = ? AND \"object_id\" = ?");
							stmt.setString(1, persistenceID);
							stmt.setLong(2, a_id.asLong());
							ResultSet res = stmt.executeQuery();
							if(res.next())
								ret = (T)getSerializedObjectFromDatabase(res, 1);
							res.close();
						}
					}
					catch(Exception e)
					{
						sm_logger.log(Level.WARNING, "Could not get object from database.", e);
					}
					finally
					{
						if(conn != null)
						{
							try {
								conn.close();
							} catch(SQLException e) {
							}
						}
					}
				}
			}
			if(ret != null)
				cacheObject(ret);
		}

		return ret;
	}
	
	/**
	 * Caches an object.
	 * @param a_object The object to cache.
	 */
	public void cacheObject(T a_object)
	{
		ID id = a_object.getID();

		synchronized(m_cache)
		{
			synchronized(m_cacheTimes)
			{
				T old = m_cache.get(id);
				if(old == null || !old.equals(a_object)) // Prevent additionally downloaded data (for example the content of a changeset) from being lost.
					m_cache.put(id, a_object);
				m_cacheTimes.put(id, System.currentTimeMillis());
			}
		}
	}

	/**
	 * Returns an ID to use in the database cache to identify this cache object.
	 * @return A unique ID for this cache object or null if no database cache is used.
	 */
	protected String getPersistenceID()
	{
		if(m_dataSource == null)
			return null;
		else
			return m_persistenceID;
	}

	/**
	 * Returns the connection to the cache database.
	 * @return A database connection or null if no database cache is used.
	 * @throws SQLException A connection could not be established.
	 */
	protected Connection getConnection() throws SQLException
	{
		if(m_dataSource == null)
			return null;

		Connection ret = m_dataSource.getConnection();
		ret.setAutoCommit(false);
		return ret;
	}
	
	/**
	 * Clean up entries from the memory cache that exceed {@link #MAX_CACHED_VALUES} or {@link #MAX_AGE}. If a database
	 * cache is used, the entries are moved there.
	 * @param a_completely If set to true, the memory cache will be cleared completely, not just up to {@link #MAX_CACHED_VALUES} (useful at shutdown)
	 */
	protected void cleanUpMemory(boolean a_completely)
	{
		String persistenceID = getPersistenceID();
		Connection conn = null;
		try
		{
			try
			{
				conn = getConnection();
			}
			catch(SQLException e)
			{
				sm_logger.log(Level.WARNING, "Could not move memory cache to database, could not open connection.", e);
			}

			sm_logger.info("Cache "+getPersistenceID()+" contains "+m_cache.size()+" entries.");

			int affected = 0;

			while(true)
			{
				T item;
				synchronized(m_cache)
				{
					synchronized(m_cacheTimes)
					{
						if(m_cacheTimes.isEmpty())
							break;
						ID oldest = m_cacheTimes.firstKey();
						long oldestTime = m_cacheTimes.get(oldest);
						if(!a_completely && System.currentTimeMillis()-oldestTime <= MAX_AGE*1000 && m_cacheTimes.size() <= MAX_CACHED_VALUES)
							break;
						m_cacheTimes.remove(oldest);
						item = m_cache.remove(oldest);
					}
				}

				affected++;

				if(persistenceID != null && conn != null)
				{
					try
					{
						synchronized(conn)
						{
							try
							{
								PreparedStatement stmt = conn.prepareStatement("DELETE FROM \"osmrmhv_cache\" WHERE \"cache_id\" = ? AND \"object_id\" = ?");
								stmt.setString(1, persistenceID);
								stmt.setLong(2, item.getID().asLong());
								stmt.execute();

								stmt = conn.prepareStatement("INSERT INTO \"osmrmhv_cache\" ( \"cache_id\", \"object_id\", \"data\", \"date\" ) VALUES ( ?, ?, ?, ? )");
								stmt.setString(1, persistenceID);
								stmt.setLong(2, item.getID().asLong());
								putSerializedObjectInDatabase(stmt, 3, item);
								stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
								stmt.execute();

								conn.commit();

								synchronized(m_databaseCache)
								{
									m_databaseCache.add(item.getID());
								}
							}
							catch(Exception e)
							{
								conn.rollback();
								throw e;
							}
						}
					}
					catch(Exception e)
					{
						sm_logger.log(Level.WARNING, "Could not cache object in database.", e);
					}
				}
			}

			if(persistenceID != null && conn != null)
				sm_logger.info("Moved "+affected+" entries to the database cache.");
			else
				sm_logger.info("Removed "+affected+" entries from the memory.");
		}
		finally
		{
			if(conn != null)
			{
				try {
					conn.close();
				} catch(SQLException e) {
				}
			}
		}
	}

	/**
	 * Removes all entries from the database cache that exceed {@link #MAX_DATABASE_AGE} or {@link #MAX_DATABASE_VALUES}.
	 * If no database cache is used, nothing is done.
	 */
	protected void cleanUpDatabase()
	{
		int affected = 0;
		Connection conn = null;
		try
		{
			String persistenceID = getPersistenceID();
			conn = getConnection();

			if(persistenceID == null || conn == null)
				return;

			synchronized(conn)
			{
				PreparedStatement stmt = conn.prepareStatement("DELETE FROM \"osmrmhv_cache\" WHERE \"cache_id\" = ? AND \"date\" < ?");
				stmt.setString(1, persistenceID);
				stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()-MAX_DATABASE_AGE*1000));
				affected += stmt.executeUpdate();
				conn.commit();

				stmt = conn.prepareStatement("DELETE FROM \"osmrmhv_cache\" WHERE \"cache_id\" = ? AND \"object_id\" IN ( SELECT \"object_id\" FROM \"osmrmhv_cache\" WHERE \"cache_id\" = ? ORDER BY \"date\" DESC OFFSET ? )");
				stmt.setString(1, persistenceID);
				stmt.setString(2, persistenceID);
				stmt.setInt(3, MAX_DATABASE_VALUES);
				affected += stmt.executeUpdate();
				conn.commit();
			}
		}
		catch(SQLException e)
		{
			sm_logger.log(Level.WARNING, "Could not clean up database cache.", e);
		}
		finally
		{
			if(conn != null)
			{
				try {
					conn.close();
				} catch(SQLException e) {
				}
			}
		}

		if(affected > 0)
		{
			sm_logger.info("Removed "+affected+" old cache entries from the database.");
			updateDatabaseCacheList();
		}
	}

	/**
	 * Regenerates the internal cache of all IDs that are in the database cache.
	 */
	private void updateDatabaseCacheList()
	{
		Connection conn = null;
		try
		{
			String persistenceID = getPersistenceID();
			if(persistenceID == null)
				return;
			conn = getConnection();
			if(conn == null)
				return;
			PreparedStatement stmt = conn.prepareStatement("SELECT \"object_id\" FROM \"osmrmhv_cache\" WHERE \"cache_id\" = ?");
			stmt.setString(1, persistenceID);
			ResultSet res = stmt.executeQuery();
			synchronized(m_databaseCache)
			{
				m_databaseCache.clear();
				while(res.next())
					m_databaseCache.add(new ID(res.getLong(1)));
			}
			res.close();
		}
		catch(SQLException e)
		{
			sm_logger.log(Level.WARNING, "Could not initialise database cache.", e);

			m_dataSource = null;
		}
		finally
		{
			if(conn != null)
			{
				try {
					conn.close();
				} catch(SQLException e) {
				}
			}
		}
	}

	/**
	 * Runs {@link #cleanUpMemory()} and {@link #cleanUpDatabase()} on all instances of this class.
	 * @param a_completely If set to true, the memory cache is cleared completely instead of just to {@link #MAX_CACHED_VALUES}.
	 */
	public static void cleanUpAll(boolean a_completely)
	{
		ItemCache<? extends Item>[] instances;
		synchronized(sm_instances)
		{ // Copy the list of instances to avoid locking the instances list (and thus preventing the creation of new
		  // instances) during the cleanup process.
			if(sm_logger.isLoggable(Level.FINER))
				sm_logger.finer("There seem to be "+sm_instances.size()+" ItemCache instances.");

			instances = sm_instances.keySet().toArray(new ItemCache[sm_instances.size()]);
		}
		int number = 0;
		for(ItemCache<? extends Item> instance : instances)
		{
			if(instance == null) // sm_instances.size() can be larger that the actual size in a WeakHashMap()
				continue;
			number++;
			try
			{
				instance.cleanUpMemory(a_completely);
				instance.cleanUpDatabase();
			}
			catch(Exception e)
			{
				sm_logger.log(Level.WARNING, "Could not clean up cache.", e);
			}
		}

		if(sm_logger.isLoggable(Level.FINER))
			sm_logger.finer("There were actually "+number+" ItemCache instances.");
	}

	protected Serializable getSerializedObjectFromDatabase(ResultSet a_res, int a_idx) throws SQLException
	{
		try
		{
			return (Serializable)new ObjectInputStream(new ByteArrayInputStream(a_res.getBytes(a_idx))).readObject();
		}
		catch(Exception e)
		{
			if(e instanceof SQLException)
				throw (SQLException)e;
			else
				throw new SQLException("Could not unserialize object.", e);
		}
	}

	protected void putSerializedObjectInDatabase(PreparedStatement a_stmt, int a_idx, Serializable a_obj) throws SQLException
	{
		try
		{
			ByteArrayOutputStream ser = new ByteArrayOutputStream();
			BufferedOutputStream ser3 = new BufferedOutputStream(ser);
			ObjectOutputStream ser2 = new ObjectOutputStream(ser3);
			ser2.writeObject(a_obj);
			ser2.close();
			a_stmt.setBytes(a_idx, ser.toByteArray());
		}
		catch(IOException e)
		{
			throw new SQLException("Could not serialize object.", e);
		}
	}
}
