package de.cdauth.osm.lib;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class extends the {@link ItemCache} class to be able to additionally cache old versions of {@link VersionedItem}s.
 * As a VersionedItem does not know if it is the current one, you have to use special methods if you fetch
 * current object versions; if you deal with old versions or whole history trees, use the other methods of this class.
 * @author cdauth
 */
public class VersionedItemCache<T extends VersionedItem> extends ItemCache<T>
{
	private static final Logger sm_logger = Logger.getLogger(ItemCache.class.getName());

	private final Hashtable<ID,TreeMap<Version,T>> m_history = new Hashtable<ID,TreeMap<Version,T>>();
	private final ValueSortedMap<ID,Long> m_historyTimes = new ValueSortedMap<ID,Long>();

	private final Set<ID> m_databaseCache = Collections.synchronizedSet(new HashSet<ID>());

	public VersionedItemCache()
	{
		super();
	}

	public VersionedItemCache(DataSource a_dataSource, String a_persistenceID)
	{
		super(a_dataSource, a_persistenceID);
	}

	/**
	 * Returns a specific version of the object with the ID a_id.
	 * @param a_id The ID of the object.
	 * @param a_version The version to look up in the cache.
	 * @return The object of the specified version or null if is not in the cache.
	 */
	public T getObject(ID a_id, Version a_version)
	{
		TreeMap<Version,T> history = getIncompleteHistory(a_id);
		if(history == null)
			return null;
		synchronized(history)
		{
			return history.get(a_version);
		}
	}

	protected TreeMap<Version,T> getIncompleteHistory(ID a_id)
	{
		synchronized(m_history)
		{
			TreeMap<Version,T> history = m_history.get(a_id);
			if(history == null)
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
								PreparedStatement stmt = conn.prepareStatement("SELECT \"version\", \"data\" FROM \"osmrmhv_cache_version\" WHERE \"cache_id\" = ? AND \"object_id\" = ?");
								stmt.setString(1, persistenceID);
								stmt.setLong(2, a_id.asLong());
								ResultSet res = stmt.executeQuery();
								history = new TreeMap<Version,T>();
								while(res.next())
								{
									try {
										history.put(new Version(res.getLong(1)), (T)getSerializedObjectFromDatabase(res, 2));
									} catch(Exception e) {
										sm_logger.log(Level.WARNING, "Could not read version from database.", e);
									}
								}
								res.close();

								if(history.size() == 0)
									history = null;
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
			}
			return history;
		}
	}
	
	/**
	 * Returns the whole history of the object. The history is considered to be cached when all versions of it
	 * are definitely in the cache (which is the case when the current version is saved and all versions from 1 
	 * to the current one’s version number are existant).
	 * @param a_id The ID of the object.
	 * @return The whole history of the object or null if it is not complete in the cache.
	 */
	public TreeMap<Version,T> getHistory(ID a_id)
	{
		synchronized(m_history)
		{
			TreeMap<Version,T> history = getIncompleteHistory(a_id);
			if(history == null)
				return null;
			
			// Check if all versions have been fetched into history
			T current = getObject(a_id);
			if(current == null)
				return null;
			Version currentVersion = current.getVersion();
			
			for(long i=1; i<=currentVersion.asLong(); i++)
			{
				if(!history.containsKey(new Version(i)))
					return null;
			}
			return history;
		}
	}
	
	/**
	 * Caches an specific version ({@link VersionedItem#getVersion}) of an object.
	 * @param a_object The versioned object to cache.
	 */
	@Override
	public void cacheObject(T a_object)
	{
		if(a_object.isCurrent())
			super.cacheObject(a_object);

		Version version = a_object.getVersion();
		if(version == null)
			return;
		ID id = a_object.getID();
		TreeMap<Version,T> history;
		synchronized(m_history)
		{
			history = getHistory(id);
			if(history == null)
			{
				history = new TreeMap<Version,T>();
				m_history.put(id, history);
			}
			
			synchronized(m_historyTimes)
			{
				m_historyTimes.put(id, System.currentTimeMillis());
			}
		}
		synchronized(history)
		{
			history.put(version, a_object);
		}
	}
	
	/**
	 * Caches the whole history of an object.
	 * @param a_history The whole history of an object.
	 */
	public void cacheHistory(TreeMap<Version,T> a_history)
	{
		if(a_history.size() < 1)
			return;
		
		T current = a_history.lastEntry().getValue();
		if(current.isCurrent()) // Should always be true
			super.cacheObject(current);

		synchronized(m_history)
		{
			m_history.put(current.getID(), a_history);
		}
	}

	@Override
	protected void cleanUpMemory()
	{
		super.cleanUpMemory();

		String persistenceID = getPersistenceID();
		Connection conn = null;
		try
		{
			conn = getConnection();
		}
		catch(SQLException e)
		{
			sm_logger.log(Level.WARNING, "Could not move memory cache to database, could not open connection.", e);
		}

		sm_logger.info("The cache "+getPersistenceID()+" contains "+m_history.size()+" entries.");
		int affected = 0;

		while(true)
		{
			ID id;
			TreeMap<Version,T> history;
			synchronized(m_history)
			{
				synchronized(m_historyTimes)
				{
					if(m_historyTimes.size() == 0)
						break;
					ID oldest = m_historyTimes.firstKey();
					long oldestTime = m_historyTimes.get(oldest);
					if(System.currentTimeMillis()-oldestTime <= MAX_AGE*1000 && m_historyTimes.size() <= MAX_CACHED_VALUES)
						break;
					id = oldest;
					m_historyTimes.remove(oldest);
					history = m_history.remove(id);
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
							PreparedStatement stmt = conn.prepareStatement("DELETE FROM \"osmrmhv_cache_version\" WHERE \"cache_id\" = ? AND \"object_id\" = ? AND \"version\" = ?");
							stmt.setString(1, persistenceID);
							stmt.setLong(2, id.asLong());
							for(Version it : history.keySet())
							{
								stmt.setLong(3, it.asLong());
								stmt.execute();
							}

							stmt = conn.prepareStatement("INSERT INTO \"osmrmhv_cache_version\" ( \"cache_id\", \"object_id\", \"version\", \"data\" ) VALUES ( ?, ?, ?, ? )");
							stmt.setString(1, persistenceID);
							stmt.setLong(2, id.asLong());
							for(Map.Entry<Version,T> it : history.entrySet())
							{
								stmt.setLong(3, it.getKey().asLong());
								putSerializedObjectInDatabase(stmt, 4, it.getValue());
								stmt.execute();
							}

							conn.commit();

							synchronized(m_databaseCache)
							{
								m_databaseCache.add(id);
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

		sm_logger.info("Removed "+affected+" cache entries from the memory.");
	}

	@Override
	protected void cleanUpDatabase()
	{
		super.cleanUpDatabase();

		Connection conn = null;
		int affected = 0;
		try
		{
			String persistenceID = getPersistenceID();
			conn = getConnection();

			if(persistenceID == null || conn == null)
				return;

			synchronized(conn)
			{ // TODO: Better selection of entries to remove
				PreparedStatement stmt = conn.prepareStatement("DELETE FROM \"osmrmhv_cache_version\" WHERE \"cache_id\" = ? AND ( \"object_id\", \"version\" ) IN ( SELECT \"object_id\", \"version\" FROM \"osmrmhv_cache_version\" WHERE \"cache_id\" = ? ORDER BY RANDOM() OFFSET ?)");
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
			sm_logger.info("Removed "+affected+" cache entries from the database.");
			updateDatabaseCacheList();
		}
	}

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
			PreparedStatement stmt = conn.prepareStatement("SELECT DISTINCT \"object_id\" FROM \"osmrmhv_cache_version\" WHERE \"cache_id\" = ?");
			stmt.setString(1, persistenceID);
			ResultSet res = stmt.executeQuery();
			synchronized(m_databaseCache)
			{
				m_databaseCache.clear();
				while(res.next())
					m_databaseCache.add(new ID(res.getLong(1)));
			}
			res.close();
			conn.close();
		}
		catch(SQLException e)
		{
			sm_logger.log(Level.WARNING, "Could not initialise database cache.", e);
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
