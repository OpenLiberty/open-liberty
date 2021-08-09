/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A <code>Queue</code> is a doubly-linked list of <code>QueueElements</code>.
 * <p>
 * 
 * Note, a <code>Queue</code> performs no synchronization. The user of the queue
 * is responsible for ensuring that access to the queue is thread-safe.
 */

public class Queue {

	/**
	 * The head and tail of the <code>Queue</code>.
	 */

	protected QueueElement head;
	protected QueueElement tail;

	/**
	 * Number of elements in the <code>Queue</code>.
	 */

	protected int numElements;

	public Queue() {

		head = null;
		tail = null;
		numElements = 0;

	} // Queue

	/**
	 * Perform sanity check on given <code>QueueElement</code> to make sure it
	 * is not currently a member of a <code>Queue</code>.
	 */

	private final static void checkElementFree(QueueElement qe) {

		if ((qe.previous != null) || (qe.next != null)
				|| (qe.queue != null)) {
			throw new RuntimeException("Queue element in use");
		}

	} // checkElementFree

	/**
	 * Add the <code>given</code> QueueElement to the head of this
	 * <code>Queue</code>.
	 * 
	 * @param qe
	 *            the <code>QueueElement</code> to add to the head of this Queue
	 *            <p>
	 */

	public void addToHead(QueueElement qe) {

		checkElementFree(qe);
		if (head == null) {
			head = qe;
			tail = qe;
		} else {
			qe.previous = null;
			qe.next = head;
			head.previous = qe;
			head = qe;
		}
		qe.queue = this;
		numElements++;

	} // addToHead

	/**
	 * Add the <code>given</code> QueueElement to the tail of this
	 * <code>Queue</code>.
	 * 
	 * @param qe
	 *            the <code>QueueElement</code> to add to the tail of this Queue
	 *            <p>
	 */

	public void addToTail(QueueElement qe) {

		checkElementFree(qe);
		if (head == null) {
			head = qe;
			tail = qe;
			qe.queue = this;
		} else {
			qe.previous = tail;
			qe.next = null;
			tail.next = qe;
			tail = qe;
		}

		qe.queue = this;
		numElements++;

	} // addToHead

	/**
	 * Remove the element at the head of this queue and return it.
	 * 
	 * If the queue is empty return null.
	 */

	public QueueElement removeHead() {

		if (head == null) {
			return null;
		}

		QueueElement result = head;
		if (result.next == null) {
			head = null;
			tail = null;
		} else {
			head = result.next;
			head.previous = null;
		}
		result.previous = null;
		result.next = null;
		result.queue = null;

		numElements--;
		return result;

	} // removeHead

	/**
	 * Remove the element at the tail of this queue and return it.
	 * 
	 * If the queue is empty return null.
	 */

	public QueueElement removeTail() {

		if (head == null) {
			return null;
		}

		QueueElement result = tail;
		if (result.previous == null) {
			head = null;
			tail = null;
		} else {
			tail = result.previous;
			tail.next = null;
		}
		result.previous = null;
		result.next = null;
		result.queue = null;

		numElements--;
		return result;

	} // removeTail

	/**
	 * Remove given element from this queue.
	 * <p>
	 */

	public void remove(QueueElement qe) {

		if (head == qe) {
			removeHead();
		} else if (tail == qe) {
			removeTail();
		} else {
			qe.previous.next = qe.next;
			qe.next.previous = qe.previous;
			qe.previous = null;
			qe.next = null;
			qe.queue = null;
			numElements--;
		}

	} // remove

	/**
	 * Return the number of elements in this queue.
	 */

	public int size() {
		return numElements;
	} // size

	/**
	 * Return new enumeration instance that will iterate through elements of
	 * this queue, in order, from head to tail.
	 * <p>
	 * 
	 * Note, the enumeration that is returned is NOT thread safe. If the queue
	 * is modified while the enumeration is iterating it is possible that
	 * inconsistent results will be seen. It is the responsibility of the user
	 * of the enumeration to ensure the queue be enumerated is modified by
	 * another thread.
	 * <p>
	 */

	public Enumeration elements() {
		return new QueueEnumeration(head);
	} // elements

	/**
	 * A <code>QueueEnumeration</code> iterates through the elements in a
	 * <code>Queue</code>.
	 */

	class QueueEnumeration implements Enumeration {
		/**
		 * Current element in queue that is being enumerated.
		 */

		QueueElement current;

		/**
		 * Enumerate a queue starting at the given element.
		 */

		public QueueEnumeration(QueueElement qe) {
			current = qe;
		} // QueueEnumeration

		/**
		 * Return true iff there are more elements to enumerate.
		 */

		public boolean hasMoreElements() {
			return (current != null);
		} // hasMoreElements

		/**
		 * Return the next queue element.
		 */

		public Object nextElement() {
			if (current == null) {
				throw new NoSuchElementException();
			}
			Object result = current;
			current = current.next;
			return result;
		} // nextElement

	} // QueueEnumeration

} // Queue

