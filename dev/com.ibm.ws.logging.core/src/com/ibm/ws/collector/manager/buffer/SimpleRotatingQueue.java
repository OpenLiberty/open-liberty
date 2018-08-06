/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.manager.buffer;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * Thread-safe, unsynchronized, simple bounded rotating queue.
 * Uses an AtomicInteger to handle concurrency.
 * 
 * Optimized for fast writes.
 * 
 * Reads are slow - the entire queue is copied and returned.
 * 
 * Implements Queue to make it easier to pass a reference to one
 * of these objects around from bundle to bundle without having
 * to share the impl.
 * 
 * Only the Queue.add() and Queue.toArray() methods are implemented
 * (didn't bother with the others since this class is only used internally).
 * Writes are done using add(). Reads are done using toArray().
 */
public class SimpleRotatingQueue<T> implements Queue<T> {

    private final T[] elements;
    private final AtomicInteger tailIndex = new AtomicInteger(0);

    /**
     * CTOR.
     * 
     * @param elements - the array that will contain the queue elements.
     *            the size of the array determines the size of the queue.
     */
    public SimpleRotatingQueue(T[] elements) {
        if (elements == null || elements.length == 0) {
            throw new IllegalArgumentException("elements array must not be null or zero-length");
        }

        this.elements = elements; // (T[]) Array.newInstance(clazz, size); // Need to do it this way bc of generic type.
    }

    /**
     * @param element to be added to the queue
     * @return true
     */
    @Override
    public boolean add(T element) {
        elements[getAndUpdateTail()] = element;
        return true;
    }

    /**
     * Atomically update and return the tailIndex.
     * 
     * Guarantees that two threads won't simultaneously update the same slot
     * in the array.
     */
    private int getAndUpdateTail() {
        int retMe;

        do {
            retMe = tailIndex.get();
        } while (tailIndex.compareAndSet(retMe, getNext(retMe)) == false);

        return retMe;
    }

    /**
     * @return the next index after the given one (wraps)
     */
    private int getNext(int index) {
        return (++index == elements.length) ? 0 : index;
    }

    /**
     * 
     * @return an atomic copy of the queue. The elements in the returned array
     *         are in order from head to tail so it can be iterated normally.
     * 
     */
    @Override
    public <T> T[] toArray(T[] arr) {

        if (arr == null) {
            return null;
        }

        T[] retMe = (arr.length >= elements.length)
                        ? arr
                        : (T[]) Array.newInstance(arr.getClass().getComponentType(), elements.length);

        int currTailIndex;
        do {
            currTailIndex = tailIndex.get(); // start from the tailIndex (technically the first slot *after* the tail)

            int firstCopyLen = elements.length - currTailIndex;

            // copy from elements[currTailIndex -> <end>]  into  retMe[0 -> firstCopyLen]
            System.arraycopy(elements, currTailIndex, retMe, 0, firstCopyLen);

            // copy from elements[0 -> currTailIndex]  into retMe[firstCopyLen -> <end>]
            System.arraycopy(elements, 0, retMe, firstCopyLen, currTailIndex);

            // Make sure tailIndex wasn't updated by another thread during the copy
        } while (tailIndex.compareAndSet(currTailIndex, currTailIndex) == false);

        return retMe;
    }

    /**
     * @return the capacity of the queue (NOT the number of elements in it)
     * @see java.util.Collection#size()
     */
    @Override
    public int size() {
        return elements.length;
    }

    /**
     * @return false
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    // NOT IMPLEMENTED

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
    	return this.toArray(new Object[0]);
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(T e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T poll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T element() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T peek() {
        throw new UnsupportedOperationException();
    }
}