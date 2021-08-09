/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.probeExtension;

import java.util.ArrayList;
import java.util.List;

public class FastList<E> {

	private E[] listElements;

	private int addIndex = 0;
	private int count = 0;
	private int maxCount;
	private int capacity;
	private float loadFactor;

	/**
	 * Creates a new FastList with specified capacity and load factor.
	 * 
	 * @param initialCapacity
	 *            size of initial List. If initialCapacity < 10, it will be
	 *            treated as 10.
	 * @param loadFactor
	 *            load factor for list. List will resize when count >= capacity
	 *            * loadFactor. If loadFactor < 0.1, it will be treated as 0.1.
	 *            If loadFactor > 1.0, it will be treated as 0.5.
	 */
	public FastList(int initialCapacity, float loadFactor) {
		if (initialCapacity < 10)
			initialCapacity = 10;
		if (loadFactor < 0.1f)
			loadFactor = 0.1f;
		if (loadFactor > 1.0f)
			loadFactor = 0.5f;

		listElements = (E[]) new Object[initialCapacity];

		maxCount = (int) (initialCapacity * loadFactor);
		capacity = initialCapacity;
		this.loadFactor = loadFactor;
	}

	/**
	 * Creates a new FastList with a capacity of 10, and loadFactor of 0.5.
	 */
	public FastList() {
		this(10, 0.5f);
	}

	/**
	 * Adds specified object to the list and increments size
	 * 
	 * @param object
	 *            the object to add to the list
	 * @throws NullPointerException
	 *             if a null object is supplied
	 */
	public synchronized int add(E object) {
		if (object == null)
			throw new NullPointerException("FastList add called with null");

		if ((count + 1) >= maxCount)
			resize(capacity * 2);

		int initialAddIndex = addIndex;

		// find right spot to add to - start with addIndex and look
		// for first open spot. give up if we get back to initialAddIndex
		while (listElements[addIndex] != null) {
			addIndex++;
			if (addIndex == capacity)
				addIndex = 0;

			if (addIndex == initialAddIndex) {
				// should not happen - we should have resized if we needed more
				// capacity
				throw new RuntimeException("FastList out of space");
			}
		}

		count++;
		listElements[addIndex] = object;

		return addIndex;
	}
	
	/**
	 * Returns the element at the specified position in the list
	 * 
	 * @param index
	 * 			the index of the element to return
	 * 
	 * @throws NullPointerException
	 * 			if an invalid index is supplied
	 * 
	 * @return element at the specified position in the list
	 */
	public synchronized E get(int index){
		return listElements[index];
	}

	/**
	 * Removes element at specified index from the list and decrements size
	 * 
	 * @param index
	 *            the element to remove from the list.
	 * @throws NullPointerException
	 *             if an invalid index is supplied
	 */
	public synchronized void remove(int index) {
		if (listElements[index] != null) {
			count--;
			listElements[index] = null;
		}
	}

	/**
	 * Provides a shallow copy of the list
	 */
	public synchronized List<E> getAll() {
		ArrayList<E> list = new ArrayList<E>();

		for (E element : listElements) {
			if (element != null) {
				list.add(element);
			}
		}

		return list;
	}

	public synchronized int size() {
		return count;
	}

	private synchronized void resize(int newCapacity) {
		E[] newListElements = (E[]) new Object[newCapacity];
		System.arraycopy(listElements, 0, newListElements, 0, capacity);

		listElements = newListElements;
		capacity = newCapacity;
		maxCount = (int) (capacity * loadFactor);
	}

	/*
	 * returns the underlying list -- should be used for testing only
	 */
	synchronized E[] getListElements() {
		return listElements;
	}

	synchronized public void clear() {
		//Resetting the list as making it null will cause problems.
		int initialCapacity = 10;
		float loadFactor = 0.5f;
		listElements = (E[]) new Object[initialCapacity];

		maxCount = (int) (initialCapacity * loadFactor);
		capacity = initialCapacity;
		this.loadFactor = loadFactor;
		addIndex = 0;
		count = 0;
	}
}
