/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.cdauth.osm.web.common;

import eu.cdauth.osm.lib.ID;
import eu.cdauth.osm.lib.ItemCache;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A queue of callback functions to be called one at a time.
 *
 * This is used by the web applications (Route Manager and History Viewer) to avoid too many requests running at the same
 * time. Instead of generating the results immediately, they send them to this queue.
 *
 * One creates a class implementing the {@link Worker} interface where inside the {@link Worker#work} method, all the necessary
 * output is calculated (passing the ID of the relation/changeset/whatever to work with). Then the method {@link #scheduleTask}
 * is called, which returns an Object that you can call {@link Object#wait} on to wait for the work function to be executed.
 *
 * This class makes sure that no tasks of the same worker with the same ID are in the queue at the same time.
 * @author cdauth
 */
public class Queue
{
	private static final Logger sm_logger = Logger.getLogger(Queue.class.getName());

	/**
	 * A callback function for the queue.
	 */
	public static interface Worker
	{
		public void work(ID a_id);
	}
	
	protected static class ScheduledTask
	{
		public final ID id;
		public final Worker worker;
		public final Notification notify = new Notification();

		public ScheduledTask(Worker a_worker, ID a_id)
		{
			id = a_id;
			worker = a_worker;
		}
	}

	/**
	 * Enables threads to wait for the execution of a worker.
	 *
	 * Once the worker has finished, the {@link #notified} method will return true and all executions of the {@link #sleep} method
	 * will return.
	 */
	public static class Notification extends Object
	{
		private volatile boolean m_notified = false;
		private final Object m_obj = new Object();

		/**
		 * Waits for the worker to be executed. If {@link #notified} already returns true, returns immediately.
		 * @param a_timeout The maximum time to wait in milliseconds. 0 means no timeout.
		 * @throws InterruptedException The current thread was interrupted while waiting.
		 */
		public void sleep(long a_timeout) throws InterruptedException
		{
			synchronized(m_obj)
			{
				if(m_notified)
					return;
				m_obj.wait(a_timeout);
			}
		}

		/**
		 * Returns true once the worker has been executed.
		 * @return True if the worker has been executed.
		 */
		public boolean notified()
		{
			return m_notified;
		}

		protected void _notify()
		{
			synchronized(m_obj)
			{
				m_notified = true;
				m_obj.notifyAll();
			}
		}
	}

	protected static class ExecutionThread extends Thread
	{
		private final Queue m_queue;

		public ExecutionThread(Queue a_queue)
		{
			super("osmrmhv Queue");
			setDaemon(true);
			m_queue = a_queue;
		}

		@Override
		public void run()
		{
			try
			{
				while(true)
				{
					ScheduledTask task;
					synchronized(m_queue)
					{
						task = m_queue.getQueuedTask();
						while(task == null)
						{
							m_queue.wait();
							task = m_queue.getQueuedTask();
						}
					}

					try
					{
						task.worker.work(task.id);
					}
					catch(Exception e)
					{
						sm_logger.log(Level.WARNING, "Queue Worker aborted with exception.", e);
					}

					m_queue.taskFinished(task.worker, task.id);
					task.notify._notify();
				}
			}
			catch(InterruptedException e)
			{
			}
		}
	}

	private LinkedList<ScheduledTask> m_queue = new LinkedList<ScheduledTask>();
	private Map<Worker,Map<ID,ScheduledTask>> m_ids = new Hashtable<Worker,Map<ID,ScheduledTask>>();
	private static Queue sm_instance = null;

	protected Queue()
	{
		new ExecutionThread(this).start();
	}

	public synchronized static Queue getInstance()
	{
		if(sm_instance == null)
			sm_instance = new Queue();
		return sm_instance;
	}

	/**
	 * Schedule the execution of a worker with the specified ID.
	 * @param a_worker The worker to call
	 * @param a_id The ID to pass to the worker
	 * @return A notification object that enables threads to wait for the execution of the worker.
	 */
	public synchronized Notification scheduleTask(Worker a_worker, ID a_id)
	{
		Map<ID,ScheduledTask> ids = m_ids.get(a_worker);
		if(ids == null)
		{
			ids = new Hashtable<ID,ScheduledTask>();
			m_ids.put(a_worker, ids);
		}

		ScheduledTask task = ids.get(a_id);
		if(task == null)
		{
			task = new ScheduledTask(a_worker, a_id);
			ids.put(a_id, task);
			m_queue.add(task);
		}

		this.notify();

		return task.notify;
	}

	/**
	 * Returns the position of a scheduled task with the given worker and ID in the queue.
	 * @param a_worker The worker to look for
	 * @param a_id The ID to look for
	 * @return The position in the queue or 0 if the task isn’t scheduled. 1 means that the item is currently being processed.
	 */
	public synchronized int getPosition(Worker a_worker, ID a_id)
	{
		Map<ID,ScheduledTask> ids = m_ids.get(a_worker);
		if(ids == null || !ids.containsKey(a_id))
			return 0;

		boolean in = false;
		int pos = 2;
		for(ScheduledTask it : m_queue)
		{
			if(it.worker.equals(a_worker) && it.id.equals(a_id))
			{
				in = true;
				break;
			}
			pos++;
		}
		return in ? pos : 1;
	}

	protected synchronized ScheduledTask getQueuedTask()
	{
		return m_queue.poll();
	}

	protected synchronized void taskFinished(Worker a_worker, ID a_id)
	{
		Map<ID,ScheduledTask> ids = m_ids.get(a_worker);
		if(ids != null)
			ids.remove(a_id);
	}
}
