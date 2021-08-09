/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Non-blocking (except when take or poll with timeout are requested) queue that is backed by
 * two ConcurrentLinkedQueue instances, where offer can be made to either, but one is preferred
 * over the other when polling/removing items.
 *
 * @param <T>
 */
public class DoubleQueue<T> extends AbstractQueue<T> implements BlockingQueue<T> {
    /**
     * Queue for expedited offer/add, which is preferred for polling/removal.
     */
    private final ConcurrentLinkedQueue<T> q1 = new ConcurrentLinkedQueue<T>();

    /**
     * Queue for normal offer/add.
     */
    private final ConcurrentLinkedQueue<T> q2 = new ConcurrentLinkedQueue<T>();

    /**
     * Count of items available for poll/removal.
     */
    private final ReduceableSemaphore size = new ReduceableSemaphore(0, false);

    @Override
    public boolean contains(Object item) {
        return q2.contains(item) || q1.contains(item);
    }

    @Override
    public int drainTo(Collection<? super T> col) {
        int count = 0;
        for (T item; (item = poll()) != null; count++)
            col.add(item);
        return count;
    }

    @Override
    public int drainTo(Collection<? super T> col, int maxElements) {
        int count = 0;
        for (T item; count < maxElements && (item = poll()) != null; count++)
            col.add(item);
        return count;
    }

    @Override
    public boolean isEmpty() {
        return size.availablePermits() <= 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new QueueIterator();
    }

    // Iterate the first queue, then once exhausted, switch to the second queue.
    private class QueueIterator implements Iterator<T> {
        private boolean is1 = true;
        private Iterator<T> it = q1.iterator();
        private boolean skipCheck; // after .hasNext returns true, the subsequent .next must return a value, so we should not check it again

        @Override
        public boolean hasNext() {
            if (is1) {
                if (it.hasNext())
                    return skipCheck = true;
                it = q2.iterator();
                is1 = false;
            }
            return it.hasNext();
        }

        @Override
        public T next() {
            if (is1 && !skipCheck && !it.hasNext()) {
                it = q2.iterator();
                is1 = false;
            }
            skipCheck = false;
            return it.next();
        }

        @Override
        public void remove() {
            // Cannot be implemented because a delegated it.remove() does not indicate whether or not
            // the item was actually removed, which makes it impossible to reliably update the semaphore.
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean offer(T item) {
        if (item instanceof QueueItem && ((QueueItem) item).isExpedited())
            q1.offer(item);
        else
            q2.offer(item);

        size.release();
        return true;
    }

    @Override
    public boolean offer(T item, long time, TimeUnit timeout) throws InterruptedException {
        return offer(item); // size is unlimited so all adds are non-blocking
    }

    @Override
    public T peek() {
        T t = q1.peek();
        return t == null ? q2.peek() : t;
    }

    @Override
    public T poll() {
        while (size.tryAcquire()) {
            T t = q1.poll();
            t = t == null ? q2.poll() : t;
            if (t == null) {
                size.release(); // another thread is removing, put the permit back
                Thread.yield();
            } else
                return t;
        }
        return null;
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        for (long start = System.nanoTime(), remain = timeout = unit.toNanos(timeout); //
                        remain >= 0 && size.tryAcquire(remain, TimeUnit.NANOSECONDS); //
                        remain = timeout - (System.nanoTime() - start)) {
            T t = q1.poll();
            t = t == null ? q2.poll() : t;
            if (t == null) {
                size.release(); // another thread is removing, put the permit back
                Thread.yield();
            } else
                return t;
        }
        return null;
    }

    @Override
    public void put(T item) throws InterruptedException {
        offer(item);
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean remove(Object item) {
        if (q1.remove(item) || q2.remove(item)) {
            size.reducePermits(1);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> items) {
        boolean modified = false;
        // Using remove instead of iterator.remove might seem inefficient, but we cannot reliably use iterator.remove
        // because it does not indicate whether or not it actually removed anything.
        for (Object item : items) {
            while (q1.remove(item)) {
                size.reducePermits(1);
                modified = true;
            }
            while (q2.remove(item)) {
                size.reducePermits(1);
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> items) {
        boolean modified = false;
        // Using remove instead of iterator.remove might seem inefficient, but we cannot reliably use iterator.remove
        // because it does not indicate whether or not it actually removed anything.
        for (Iterator<T> it = q1.iterator(); it.hasNext();) {
            T t = it.next();
            if (!items.contains(t))
                while (q1.remove(t)) {
                    size.reducePermits(1);
                    modified = true;
                }
        }
        for (Iterator<T> it = q2.iterator(); it.hasNext();) {
            T t = it.next();
            if (!items.contains(t))
                while (q2.remove(t)) {
                    size.reducePermits(1);
                    modified = true;
                }
        }
        return modified;
    }

    @Override
    public final int size() {
        int s = size.availablePermits();
        return s < 0 ? 0 : s;
    }

    @Override
    public T take() throws InterruptedException {
        while (true) {
            size.acquire();

            T t = q1.poll();
            t = t == null ? q2.poll() : t;
            if (t == null) {
                size.release(); // another thread is removing, put the permit back
                Thread.yield();
            } else
                return t;
        }
    }

    /**
     * If less than 100 elements, represents the queue in the form:
     *
     * <pre>
     * SIZE [A, B] [C, D, E]
     * </pre>
     *
     * Otherwise, represents the queue as its total size followed by the size of each of the sub-queues.
     *
     * <p>The string value generated by this method is only meaningful when no modifications are being made for
     * the duration of the method.</p>
     *
     * @return string representing this data structure.
     */
    @Override
    public String toString() {
        int s = size.availablePermits();
        StringBuilder b = new StringBuilder().append(size.availablePermits());
        if (s < 100)
            b.append(' ').append(q1).append(' ').append(q2);
        else
            b.append(' ').append(q1.size()).append(' ').append(q2.size());
        return b.toString();
    }
}