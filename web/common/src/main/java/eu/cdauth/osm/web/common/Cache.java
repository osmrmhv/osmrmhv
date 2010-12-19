/*
	This file is part of the OSM Route Manager and History Viewer.

	OSM Route Manager and History Viewer is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	OSM Route Manager and History Viewer is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.

	You should have received a copy of the GNU Affero General Public License
	along with this software. If not, see <http://www.gnu.org/licenses/>.
*/

package eu.cdauth.osm.web.common;

import eu.cdauth.osm.lib.ValueSortedMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Caches the objects produced by Route Manager and History Viewer. Every entry has an ID (for which the ID of the relation
 * or changeset is probably used) and a content (which is any serializable object containing all the information).
 * @author Candid Dauth
 */
public class Cache<T extends Serializable>
{
	/**
	 * The interval in seconds how often the cleanup thread shall remove old cache entries.
	 */
	protected static final int CLEANUP_INTERVAL = 3600;

	private static final Logger sm_logger = Logger.getLogger(Cache.class.getName());

	protected static final Map<Cache,Object> sm_instances = new WeakHashMap<Cache,Object>();
	protected static CleanupThread sm_cleanupThread = null;

	private final File m_dir;
	private int m_maxAge = 0;
	private long m_maxSize = 50000000;

	public static class Entry<T>
	{
		/**
		 * The ID of the cached item.
		 */
		public final String id;
		/**
		 * The content of the cached item.
		 */
		public final T content;
		/**
		 * The date when the item was cached in Unix milliseconds.
		 */
		public final Date date;

		protected Entry(String a_id, T a_content, Date a_date)
		{
			id = a_id;
			content = a_content;
			date = a_date;
		}
	}

	protected static class CleanupThread extends Thread
	{
		public CleanupThread()
		{
			setDaemon(true);
		}

		@Override
		public void run()
		{
			try
			{
				while(true)
				{
					synchronized(sm_instances)
					{
						for(Map.Entry<Cache,Object> instance : sm_instances.entrySet())
						{
							instance.getKey().clearOldEntries();

							if(isInterrupted())
								return;
						}
					}
					Thread.sleep(CLEANUP_INTERVAL*1000);
				}
			}
			catch(InterruptedException e)
			{
			}
		}
	}

	/**
	 * @param a_dir The directory where to store the files.
	 */
	public Cache(String a_dir)
	{
		m_dir = new File(a_dir);
		if(!m_dir.exists())
			m_dir.mkdirs();

		synchronized(sm_instances)
		{
			sm_instances.put(this, null);
			if(sm_cleanupThread == null)
			{
				sm_cleanupThread = new CleanupThread();
				sm_cleanupThread.start();
			}
		}
	}

	/**
	 * Returns the set maximum total size of this cache before items are deleted.
	 * @return The maximum size in bytes, 0 means no limit
	 */
	public long getMaxSize()
	{
		return m_maxSize;
	}

	/**
	 * Returns the set maximum age of entries in this cache before they are deleted.
	 * @return The maximum age in seconds, 0 means no limit
	 */
	public int getMaxAge()
	{
		return m_maxAge;
	}

	/**
	 * Sets the maximum size of this cache before entries are deleted.
	 * @param a_maxSize The maximum size in bytes, 0 means no limit
	 */
	public void setMaxSize(long a_maxSize)
	{
		m_maxSize = a_maxSize;
	}

	/**
	 * Sets the maximum age of entries in this cache before they are deleted.
	 * @param a_maxAge The maximum age in seconds, 0 means no limit.
	 */
	public void setMaxAge(int a_maxAge)
	{
		m_maxAge = a_maxAge;
	}

	protected File getFileByID(String a_id)
	{
		return new File(m_dir, a_id+".ser.gz");
	}

	/**
	 * Cleans up cached files if the cache is bigger than {@link #getMaxSize} or entries are older than {@link #getMaxAge}.
	 */
	public synchronized void clearOldEntries()
	{
		if(sm_logger.isLoggable(Level.INFO))
			sm_logger.info("Cleaning up cache.");

		long maxAge = getMaxAge()*1000;
		long maxSize = getMaxSize();
		int deleted = 0;

		if(maxAge > 0 || maxSize > 0)
		{
			long now = System.currentTimeMillis();

			long totalSize = 0;
			ValueSortedMap<File,Long> mtimes = null;
			if(maxSize > 0)
				mtimes = new ValueSortedMap<File,Long>();

			for(File entry : m_dir.listFiles())
			{
				if(maxAge > 0 && now-entry.lastModified() > maxAge)
				{
					entry.delete();
					deleted++;
					continue;
				}

				if(maxSize > 0 && entry.isFile())
				{
					totalSize += entry.length();
					mtimes.put(entry, entry.lastModified());
				}
			}

			if(maxAge > 0)
			{
				while(mtimes.size() > 0 && totalSize > maxAge)
				{
					File f = mtimes.firstKey();
					totalSize -= f.length();
					f.delete();
					deleted++;
					mtimes.remove(f);
				}
			}
		}

		if(sm_logger.isLoggable(Level.INFO))
			sm_logger.info("Cleaned up "+deleted+" entries.");
	}

	/**
	 * Gets the content of an entry in the cache.
	 * @param a_id The ID of the requested entry.
	 * @return The entry or null if it isn’t cached yet.
	 * @throws IOException
	 */
	public synchronized Entry<T> getEntry(String a_id) throws IOException
	{
		File file = getFileByID(a_id);
		try
		{
			ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
			T obj = (T)in.readObject();
			in.close();
			return new Entry<T>(a_id, obj, new Date(file.lastModified()));
		}
		catch(FileNotFoundException e)
		{
			return null;
		}
		catch(ClassNotFoundException e)
		{
			throw new IOException(e);
		}
	}

	/**
	 * Stores an entry in the cache. If an entry with the same ID already exists, it will be overwritten.
	 * @param a_id The ID under which to save the entry
	 * @param a_content The content of the entry
	 * @throws IOException
	 */
	public synchronized void saveEntry(String a_id, T a_content) throws IOException
	{
		File file = getFileByID(a_id);
		file.delete();

		ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
		out.writeObject(a_content);
		out.close();
	}
}
