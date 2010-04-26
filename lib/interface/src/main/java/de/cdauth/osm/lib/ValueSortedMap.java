package de.cdauth.osm.lib;

import java.util.*;

/**
 * A bit like a {@link java.util.SortedMap}, but sorted by its values.
 * @author cdauth
 */
public class ValueSortedMap<K,V> extends Hashtable<K,V>
{
	private final TreeMap<V,K> m_order;

	public ValueSortedMap()
	{
		m_order = new TreeMap<V,K>();
	}

	public ValueSortedMap(Comparator<V> a_comparator)
	{
		m_order = new TreeMap<V,K>(a_comparator);
	}

	@Override
	public synchronized void clear()
	{
		super.clear();
		m_order.clear();
	}

	@Override
	public synchronized V remove(Object key)
	{
		V value = super.remove(key);

		updateRemovedValue(value);

		return value;
	}

	protected void updateRemovedValue(V a_value)
	{
		if(a_value == null)
			return;
		for(Map.Entry<K,V> it : entrySet())
		{
			if(a_value.equals(it.getValue()))
			{
				m_order.put(a_value, it.getKey());
				return;
			}
		}
		m_order.remove(a_value);
	}

	@Override
	public synchronized void putAll(Map<? extends K, ? extends V> m)
	{
		for(Map.Entry<? extends K,? extends V> it : m.entrySet())
			put(it.getKey(), it.getValue());
	}

	@Override
	public synchronized V put(K key, V value)
	{
		V old = super.put(key, value);
		updateRemovedValue(old);
		m_order.put(value, key);

		return old;
	}

	public synchronized K firstKey() throws NoSuchElementException
	{
		if(isEmpty())
			throw new NoSuchElementException();
		return m_order.firstEntry().getValue();
	}

	public synchronized K lastKey() throws NoSuchElementException
	{
		if(isEmpty())
			throw new NoSuchElementException();
		return m_order.lastEntry().getValue();
	}
}
