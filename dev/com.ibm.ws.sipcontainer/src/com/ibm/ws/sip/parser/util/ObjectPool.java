/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.parser.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;

/**
 * A pool of objects, which is thread-safe, and never blocks the callers.
 * Null objects are not allowed.
 * 
 * The state of the pool consists of two indexes - a get index and a put
 * index. The get index points to the position of the next element to fetch.
 * The put index points to an empty cell that will accommodate the next object
 * to be returned to the pool.
 * The pool is empty if the two indexes point to the same cell. The actual pool
 * capacity is one less than the capacity parameter passed in the constructor.
 * The index values are not limited to the array boundary. When accessing the
 * array, the index is normalized using (index % array_size). This provides
 * easier comparison between the two indexes, as the put index is always
 * larger than or equal to the get index. In order to prevent negative indexes
 * in the (rare) case where an index value exceeds the 32-bit integer limit,
 * indexes are always truncated to 31 bits. This implies that the next index
 * after 2**31-1 is 0. The only case when the value of the put index can be
 * smaller than the get index is when the put index completes a full cycle and
 * rolls back to 0. In such a case, to make it simple, the pool is drained
 * (returned objects are discarded and garbage-collected) until all pooled
 * items are removed, and both indexes return to the initial state of 0, ready
 * for a new cycle.
 * 
 * The two indexes are atomic integers. A thread that wishes to access a cell
 * (for either get or put) first conquers the cell by incrementing the index.
 * If the thread succeeded in incrementing the index without interference, it
 * owns the cell exclusively, and can then modify the cell contents with no
 * need for locking.
 * 
 * Note that each call to get() or put() is executed in two steps:
 * 1. incrementing the index to conquer a cell
 * 2. modifying the contents of the cell
 * 
 * There is no way to guarantee that a single call performs the 2 steps
 * atomically. It is possible that step 1 wins the race and step 2 loses
 * the race. This causes the operation to change the index without changing
 * a reference in the array. When this happens, the operation is re-iterated,
 * incrementing the index again. The call only returns after both steps have
 * completed successfully.
 * 
 * @author ran
 */
public class ObjectPool
{
	/** array of pooled objects */
	private final AtomicReference[] m_array;

	/** index of the next object to take from the pool */
	private final AtomicInteger m_get;

	/** index of the free cell to accommodate the next object to put back */
	private final AtomicInteger m_put;

	/** cached value of m_array.length() */
	private final int m_arraySize;

	/** cached value of m_array.length()-1 */
	private final int m_capacity;

	/** the types of objects in this pool */
	private final Class m_type;

	/** the listener to be called when objects are returned */
	private ObjectPoolListener m_listener;

	/** largest get/put index */
	private static final int LIMIT = Integer.MAX_VALUE;

	/**
	 * constructor
	 * @param type the types of objects in this pool. this class must provide
	 *  a public no-arg constructor
	 * @param listener the listener to be called when objects are returned
	 * @param arraySize initial and final array size. the actual capacity is
	 *  one less than this value.
	 */
	public ObjectPool(Class type, ObjectPoolListener listener, int arraySize) {
		if (arraySize == -1) {
			arraySize = ApplicationProperties.getProperties().getInt(StackProperties.OBJECT_POOL_SIZE);
		}
		if (arraySize < 2) {
			throw new IllegalArgumentException("pool size is too small");
		}
		m_array = new AtomicReference[arraySize];
		for (int i = 0; i < arraySize; i++) {
			m_array[i] = new AtomicReference(null);
		}
		m_get = new AtomicInteger(0);
		m_put = new AtomicInteger(0);
		m_arraySize = arraySize;
		m_capacity = arraySize - 1;
		m_type = type;
		m_listener = listener;
	}

	/**
	 * constructor
	 * @param type the types of objects in this pool
	 * @param listener the listener to be called when objects are returned
	 */
	public ObjectPool(Class type, ObjectPoolListener listener) {
		this(type, listener, -1);
	}

	/**
	 * constructor
	 * @param type the types of objects in this pool
	 */
	public ObjectPool(Class type) {
		this(type, null);
	}

	/**
	 * gets an object from the pool
	 * @return the pooled object
	 */
	public Object get() {
		Object object;

		while (true) {
			// 1. get the next free object
			int p = m_put.get();
			int g = m_get.get();
			if (g >= p) {
				// get index hits the put index, implying empty pool
				if (p >= LIMIT) {
					// put pointer wrapped around. rewind both indexes.
					m_get.set(0);
					m_put.set(0);
				}
				return allocate();
			}

			// 2. conquer this cell by incrementing the get index
			if (!m_get.compareAndSet(g, g+1)) {
				// some other thread got here first. try again.
				continue;
			}

			// 3. retrieve the object at the get index
			int index = g % m_arraySize;
			AtomicReference reference = m_array[index];
			object = reference.get();
			if (object == null) {
				// getting here is rare. it happens when:
				// 1. put() is called on an empty pool (p==g) 
				// 2. put() increments the put pointer (p==g+1)
				// 3. get() is called and finds g < p
				// 4. get() takes the object at g (which is null)
				// 5. put() puts the object at g
				continue;
			}

			// 4. change the reference to null so no other get() takes the
			//    same element, and no other put() replaces it with another.
			if (reference.compareAndSet(object, null)) {
				return object;
			}

			// getting here is rare. the object that was supposedly conquered
			// locally was either replaced by a different object or replaced
			// with null, in the same cell.
		}
	}

	/**
	 * puts an object back into the pool for recycling
	 * @param object the pooled object
	 */
	public void putBack(Object object) {
		if (m_listener != null) {
			m_listener.objectReturned(object);
		}
		put(object);
	}

	/**
	 * puts an object back into the pool for recycling
	 * @param object the pooled object
	 * @return true on success, false if pool is full
	 */
	private boolean put(Object object) {
		if (object == null) {
			throw new IllegalArgumentException("returning null object to pool");
		}
		if (!m_type.isInstance(object)) {
			throw new IllegalArgumentException("argument type invalid for pool");
		}

		while (true) {
			// 1. get the index of the next free cell
			int g = m_get.get();
			int p = m_put.get();
			if (p >= LIMIT) {
				// this happens very seldom - once every 1**31 calls, the put
				// index completes a full cycle.
				// to handle this simply, we just wait for the pool to
				// drain, and start placing objects back in the pool only when
				// the get index completes its cycle too.
				return false;
			}
			if (p - g >= m_capacity) {
				// put index is far from the get pointer, implying full pool.
				// note that (p >= g + m_capacity) is a bug.
				return false;
			}

			// 2. conquer this cell by incrementing the put index
			if (!m_put.compareAndSet(p, p+1)) {
				// some other thread got here first. try again.
				continue;
			}

			// 3. put the object in the cell
			int index = p % m_arraySize;
			AtomicReference reference = m_array[index];
			if (reference.compareAndSet(null, object)) {
				return true;
			}

			// getting here is rare. the object that was supposedly conquered
			// locally was replaced with a different object in the same cell.
		}
	}

	/**
	 * allocates a new object from the heap
	 * @return the newly allocated object
	 */
	private Object allocate() {
		Object o;
		try {
			o = m_type.newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (o == null) {
			throw new RuntimeException("null object created");
		}
		return o;
	}

	/**
	 * sets the listener to be called when objects are returned
	 * @param listener the listener to be called when objects are returned
	 */
	public void setObjectPoolListener(ObjectPoolListener listener) {
		m_listener = listener;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "ObjectPool-" + System.identityHashCode(this);
	}
}
