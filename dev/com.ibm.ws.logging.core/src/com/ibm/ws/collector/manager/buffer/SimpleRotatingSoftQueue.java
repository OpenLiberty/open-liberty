/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.manager.buffer;

import java.lang.ref.SoftReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleRotatingSoftQueue<T> implements Queue<T> {

    private final ArrayList<SoftReference<T>> elements;
    private final int QUEUE_SIZE;
    private final AtomicInteger tailIndex = new AtomicInteger(0);

    /**
     * @param elements
     */
    public SimpleRotatingSoftQueue(T[] elements) {
        if (elements == null || elements.length == 0) {
            throw new IllegalArgumentException("elements array must not be null or zero-length");
        }
        this.QUEUE_SIZE = elements.length;
        this.elements = new ArrayList<SoftReference<T>>(this.QUEUE_SIZE);

        for (T element : elements) {
            SoftReference<T> elementItem = new SoftReference<T>(element);
            this.elements.add(elementItem);
        }
    }

    /**
     * @param element to be added to the queue
     * @return true
     */
    @Override
    public boolean add(T element) {
        SoftReference<T> elementItem = new SoftReference<T>(element);
        elements.set(getAndUpdateTail(), elementItem);
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
        return (++index == elements.size()) ? 0 : index;
    }

    /**
     *
     * @return an atomic copy of the queue. The elements in the returned array
     *         are in order from head to tail so it can be iterated normally.
     *
     */
    @SuppressWarnings("unchecked")
	@Override
    public <X> X[] toArray(X[] arr) {

        if (arr == null) {
            return null;
        }
        X[] retMe;
        int currTailIndex;
        do {
            currTailIndex = tailIndex.get(); // start from the tailIndex (technically the first slot *after* the tail)

            ArrayList<X> returnArrayList = new ArrayList<X>();
            //Go through the SoftReference Buffer and retrieve elements that have not been GCed.
            //Restore these elements into a strong reference and add them into an arraylist
            //which will be used to retrieve an array that will be returned to the caller.
            for (int i = 0, index = currTailIndex; i < elements.size(); i++, index++) {
                index = index % elements.size();
                X element = (X) elements.get(index).get();
                if (element != null) {
                    returnArrayList.add(element);
                }
            }

            retMe = (X[]) Array.newInstance(arr.getClass().getComponentType(), returnArrayList.size());
            returnArrayList.toArray(retMe);

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
        return elements.size();
    }

    /**
     * @return false
     */
    @Override
    public boolean isEmpty() {
        return false;
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public T[] toArray() {
        Object[] messagesList = this.toArray(new Object[elements.size()]);
        return (T[]) messagesList;
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
