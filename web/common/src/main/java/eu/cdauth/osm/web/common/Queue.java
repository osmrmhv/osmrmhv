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
	public static interface Worker
	{
		public void work(ID a_id);
	}
	
	protected static class ScheduledTask
	{
		public final ID id;
		public final Worker worker;
		public final Object notify = new Object();

		public ScheduledTask(Worker a_worker, ID a_id)
		{
			id = a_id;
			worker = a_worker;
		}
	}

	protected static class ExecutionThread extends Thread
	{
		private final Queue m_queue;

		public ExecutionThread(Queue a_queue)
		{
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
						m_queue.wait();
						task = m_queue.getQueuedTask();
					}

					task.worker.work(task.id);
					m_queue.taskFinished(task.worker, task.id);
					task.notify.notifyAll();

					ItemCache.cleanUpAll(true);
				}
			}
			catch(InterruptedException e)
			{
			}
		}
	}

	private LinkedList<ScheduledTask> m_queue = new LinkedList<ScheduledTask>();
	private Map<Worker,Map<ID,ScheduledTask>> m_ids = new Hashtable<Worker,Map<ID,ScheduledTask>>();

	public Queue()
	{
		new ExecutionThread(this).start();
	}

	/**
	 * Schedule the execution of a worker with the specified ID.
	 * @param a_worker The worker to call
	 * @param a_id The ID to pass to the worker
	 * @return An empty Object that you can {@link Object#wait} for until the worker has been executed
	 */
	public synchronized Object scheduleTask(Worker a_worker, ID a_id)
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
