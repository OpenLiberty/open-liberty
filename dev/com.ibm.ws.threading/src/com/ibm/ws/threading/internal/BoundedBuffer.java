/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.threading.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * A fixed size FIFO (with expedited) of Objects. Null objects are not allowed in
 * the buffer. The buffer contains a expedited FIFO buffer, whose objects
 * will be removed before objects in the main buffer.
 */
public class BoundedBuffer<T> implements BlockingQueue<T> {
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //
    // Implementation Note:  the buffer is implemented using a circular
    //   array.  Both a "head" and "tail" pointer (array index) are
    //   maintained as well as an overall count of used/empty
    //   slots:
    //
    //   [TEM]: I do not know why both used and empty slot counts are
    //          maintained given that one can be derived from the other
    //          and the capacity.  I suspect it is so that the two
    //          values can vary on independent threads.
    //
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //
    // BoundedBuffer's locking model is a little more complicated than it
    // appears at first glance and the reasons for this are somewhat subtle.
    //
    // There are two 'locks' involved: the BoundedBuffer instance itself
    // and the Object instance named lock. Synchronizing on this i.e. the
    // BoundedBuffer instance is used to single thread take operations and
    // the used slots counter. Synchronizing on lock is used to single
    // thread put operations and the empty slots counter.
    //
    // It's easiest to explain the need for two separate locks by explaining
    // a problem that can arise by only using a single lock. Consider a buffer
    // with a capacity of two that is currently full - two consecutive put
    // calls have been made and all threads attempting puts are now in a wait()
    // call until some space is freed up in the buffer by a take. Another thread
    // calls the buffer and performs a take which will trigger a notify. If the
    // thread that is notified is a taking thread another take will occur. This
    // take will empty the buffer and triggering another notify. If the thread
    // that is notified here is another taking thread a hang will occur - the
    // buffer is empty so it and any subsequent take calls will drop into a
    // wait() until a put occurs. No more notify() calls will be made leaving
    // the taking threads stuck in wait() awaiting a put and the putting threads
    // stuck in wait() awaiting a notify from a successful take which can never
    // occur as the buffer's empty.
    //
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // D638088.2
    private static TraceComponent tc = Tr.register(BoundedBuffer.class);

/*
 * Maged:
 * These modifications are for the purpose of:
 * - increasing concurrency
 * - reduce latency
 * - fix a race condition between the expand and put functions
 * Outline:
 * - The buffer size is disassociated from the locks
 * - numberOfUsedSlots is declared AtomicInteger and updated atomically
 * without locking
 * - numberOfEmptySlots is not needed
 * - Wait-notify is replaced with polling
 * - Lock holding periods are minimized
 * - Put/Take operations acquire one lock and update numberOfUsedSlots
 * atomically instead of acquiring two locks
 * - The expand operation acquires both locks
 * Christoph:
 * - queueing putters and getters separately on locks that are different
 * from 'this' and 'lock'
 * - benign races on XXQueueLen_ at notify
 * - spinning with backoff
 */

    // D312598 - begin
    private static final int SPINS_TAKE_;
    private static final int SPINS_PUT_;
    private static final boolean YIELD_TAKE_;
    private static final boolean YIELD_PUT_;

    //D638088 - modified the set of WAIT_SLICE parameters
    private static final long WAIT_SHORT_SLICE_;

    static {

        //D638088 - modified spinning defaults to adjust to the number
        //of physical processors on host system.
        SPINS_TAKE_ = Integer.getInteger("com.ibm.ws.util.BoundedBuffer.spins_take", Runtime.getRuntime().availableProcessors() - 1).intValue(); // D371967
        SPINS_PUT_ = Integer.getInteger("com.ibm.ws.util.BoundedBuffer.spins_put", SPINS_TAKE_ / 4).intValue(); // D371967

        YIELD_TAKE_ = Boolean.getBoolean("com.ibm.ws.util.BoundedBuffer.yield_take");
        YIELD_PUT_ = Boolean.getBoolean("com.ibm.ws.util.BoundedBuffer.yield_put");

        //D638088 - created short/long wait periods, based on a multiplication factor. Keeping
        //default behavior the same.
        WAIT_SHORT_SLICE_ = Long.getLong("com.ibm.ws.util.BoundedBuffer.wait", 1000).longValue(); // D371967
    }

    private static ConcurrentLinkedQueue<GetQueueLock> waitingThreadLocks = new ConcurrentLinkedQueue<GetQueueLock>();

    private static final ThreadLocal<GetQueueLock> threadLocalGetLock = new ThreadLocal<GetQueueLock>() {
        @Override
        protected GetQueueLock initialValue() {
            return new GetQueueLock();
        }
    };

    // D371967: An easily-identified marker class used for locking
    private static class PutQueueLock extends Object {}

    private final PutQueueLock putQueue_ = new PutQueueLock();
    private int putQueueLen_ = 0;

    // D638088: Added two fields to the getQueueLock for book keeping
    // D371967: An easily-identified marker class used for locking
    private static class GetQueueLock extends Object {
        private boolean notified;

        public boolean isNotified() {
            return notified;
        }

        public void setNotified(boolean notified) {
            this.notified = notified;
        }

    }

    private void notifyGet_() {
        // Notify a waiting thread that work has arrived on the queue.  A notification may be lost in some cases - however as none of the
        // threads wait endlessly, a waiting thread will either be notified, or will eventually wake up.  If there are no waiting threads,
        // the new work will be picked up when an active thread completes its task.
        GetQueueLock lock = waitingThreadLocks.poll();
        if (lock != null) {
            synchronized (lock) {
                lock.setNotified(true);
                lock.notify();
            }
        }
    }

    private void waitGet_(long timeout) throws InterruptedException {
        GetQueueLock getQueueLock = threadLocalGetLock.get();
        try {
            synchronized (getQueueLock) {
                getQueueLock.setNotified(false);
                waitingThreadLocks.add(getQueueLock);
                getQueueLock.wait(timeout == -1 ? 0 : timeout);
            }
        } finally {
            if (!getQueueLock.isNotified()) {
                // we either timed out or were interrupted, so remove ourselves from the queue...  it's okay if a producer already has the
                // lock because we're going to exit and go try to get more work anyway, so it's okay if we don't get the signal
                waitingThreadLocks.remove(getQueueLock);
            }
        }

    }

    private void notifyPut_() {
        // As in notifyGet_, this notification might be lost.
        if (putQueueLen_ > 0) {
            synchronized (putQueue_) {
                putQueue_.notify();
            }
        }
    }

    private void waitPut_(long timeout) throws InterruptedException {
        synchronized (putQueue_) {
            try {
                putQueueLen_++;
                // D497382 - As in waitGet_, try to avoid lost notifications.
                if (numberOfUsedSlots.get() >= buffer.length) {
                    // notification through notifyPut_ may be lost here
                    putQueue_.wait(timeout);
                }
//            } catch (InterruptedException ex) {
//                putQueue_.notify();
//                throw ex;
            } finally {
                putQueueLen_--;
            }
        }
    }

    // D312598 - end

    private T[] buffer; // the circular array (buffer)
    private T[] expeditedBuffer; // the circular expedited array (expedited buffer)
    private int takeIndex = 0; // the beginning of the buffer
    private int expeditedTakeIndex = 0; // the beginning of the expedited buffer
    private int putIndex = 0; // the end of the buffer
    private int expeditedPutIndex = 0; // the end of the expedited buffer
    private final AtomicInteger numberOfUsedSlots = new AtomicInteger(0); // D312598 D638088
    private final AtomicInteger numberOfUsedExpeditedSlots = new AtomicInteger(0);

    /**
     * Helper monitor to handle puts.
     */
    private final BoundedBufferLock lock = new BoundedBufferLock();

    //@awisniew DELETED
    //private ThreadPool threadPool_ = null; // this can be null,  only non null when this threadpool supports thread renewal

    /**
     * Create a BoundedBuffer with the given capacity.
     *
     * @exception IllegalArgumentException if the requested capacity
     *                is less or equal to zero.
     */
    @SuppressWarnings("unchecked")
    public BoundedBuffer(Class<T> c, int capacity, int expeditedCapacity) throws IllegalArgumentException {

        if (capacity <= 0 || expeditedCapacity <= 0) {
            throw new IllegalArgumentException();
        }

        //Initialize buffer array
        final T[] buffer = (T[]) Array.newInstance(c, capacity);
        this.buffer = buffer;

        //Initialize expedited buffer array
        final T[] expeditedBuffer = (T[]) Array.newInstance(c, expeditedCapacity);
        this.expeditedBuffer = expeditedBuffer;

        //enable debug output
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created bounded buffer: capacity=" + capacity + " expedited capacity=" + expeditedCapacity);
        }
    }

    /**
     * Return the number of elements in the buffer.
     * This is only a snapshot value, that may change
     * immediately after returning.
     */
    // D312598 - remove synchronization and use .get
    @Override
    public int size() {
        return numberOfUsedSlots.get() + numberOfUsedExpeditedSlots.get();
    }

    /**
     * Returns the overall capacity of the buffer.
     * Note that this is how much the buffer can hold,
     * not how much space is unused.
     */
    public int capacity() {
        return buffer.length + expeditedBuffer.length;
    }

    /*
     * @awisniew - ADDED
     *
     * The number of unused slots.
     *
     * @see java.util.concurrent.BlockingQueue#remainingCapacity()
     */
    @Override
    public int remainingCapacity() {
        return capacity() - size();
    }

    /*
     * @awisniew - ADDED
     *
     * @see java.util.Collection#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        if (size() == 0) {
            return true;
        }
        return false;
    }

    /**
     * Peeks at the head of the buffer and returns the first
     * object (or null if empty). The object remains in the
     * buffer.
     */
    @Override
    public T peek() {
        synchronized (this) {
            if (numberOfUsedExpeditedSlots.get() > 0) {
                return expeditedBuffer[expeditedTakeIndex];
            } else if (numberOfUsedSlots.get() > 0) {
                return buffer[takeIndex];
            } else {
                return null;
            }
        }
    }

    /*
     * @awisniew - ADDED
     *
     * Same as peek, only throws exception if queue is empty
     *
     * @see java.util.Queue#element()
     */
    @Override
    public T element() {
        T retrievedElement = peek();

        if (retrievedElement == null) {
            throw new NoSuchElementException();
        }

        return retrievedElement;
    }

    /**
     * Puts an object into the buffer. If the buffer is full,
     * the call will block indefinitely until space is freed up.
     *
     * @param x the object being placed in the buffer.
     * @exception IllegalArgumentException if the object is null.
     */
    @Override
    public void put(T t) throws InterruptedException {
        if (t == null) {
            throw new IllegalArgumentException();
        }

        // D186845      if (Thread.interrupted())
        // D186845 throw new InterruptedException();

        // D312598 - begin

        //TODO: Add expedited put/fix waiting logic
        boolean ret = false;
        while (true) {
            synchronized (lock) {
                if (numberOfUsedSlots.get() < buffer.length) {
                    insert(t);
                    numberOfUsedSlots.getAndIncrement();
                    ret = true;
                }
            }

            if (ret) {
                notifyGet_();
                return;
            }

            int spinctr = SPINS_PUT_;
            while (numberOfUsedSlots.get() >= buffer.length) {
                // busy wait
                if (spinctr > 0) {
                    if (YIELD_PUT_)
                        Thread.yield();
                    spinctr--;
                } else {
                    // block on lock
                    waitPut_(WAIT_SHORT_SLICE_);
                }
            }
        }
        // D312598 - end

    }

    /**
     * Puts an object into the buffer. If the buffer is full,
     * the call will block for up to the specified timeout
     * period.
     *
     * @param x the object being placed in the buffer.
     * @param timeoutInMillis the timeout period in milliseconds.
     * @return the object that was put into the buffer (t) or
     *         null in the event that the request timed out.
     * @exception IllegalArgumentException if the object is null.
     */
    public T put(T t, long timeoutInMillis) throws InterruptedException {
        if (t == null) {
            throw new IllegalArgumentException();
        }

        //TODO: Add expedited put/fix waiting logic

        long start = (timeoutInMillis <= 0) ? 0 : -1;
        long waitTime = timeoutInMillis;

        // D312598 - begin
        T ret = null;
        while (true) {
            synchronized (lock) {
                if (numberOfUsedSlots.get() < buffer.length) {
                    insert(t);
                    numberOfUsedSlots.getAndIncrement();
                    ret = t;
                }
            }
            if (ret != null) {
                notifyGet_();
                return ret;
            }

            if (start == -1)
                start = System.currentTimeMillis();

            int spinctr = SPINS_PUT_;
            while (numberOfUsedSlots.get() >= buffer.length) {
                if (waitTime <= 0) {
                    return null;
                }
                // busy wait
                if (spinctr > 0) {
                    if (YIELD_PUT_)
                        Thread.yield();
                    spinctr--;
                } else {
                    // block on lock
                    waitPut_(timeoutInMillis);
                }
                waitTime = timeoutInMillis - (System.currentTimeMillis() - start);
            }
        }
        // D312598 - end

    }

    /**
     * Puts an object into the buffer. If the buffer is at or above the
     * specified maximum capacity, the call will block for up to the
     * specified timeout period.
     *
     * @param x the object being placed in the buffer.
     * @param timeoutInMillis the timeout period in milliseconds.
     * @param maximumCapacity the desired maximum capacity of the buffer
     * @return the object that was put into the buffer (x) or
     *         null in the event that the request timed out.
     * @exception IllegalArgumentException if the object is null or if the
     *                supplied maximum capacity exceeds the buffer's size.
     */
    public T put(T t, long timeoutInMillis, int maximumCapacity) throws InterruptedException {
        if ((t == null) || (maximumCapacity > buffer.length)) {
            throw new IllegalArgumentException();
        }

        long start = (timeoutInMillis <= 0) ? 0 : -1;
        long waitTime = timeoutInMillis;

        //TODO: Add expedited put/fix waiting logic

        // D312598 - begin
        T ret = null;
        while (true) {
            synchronized (lock) {
                if (numberOfUsedSlots.get() < maximumCapacity) {
                    insert(t);
                    numberOfUsedSlots.getAndIncrement();
                    ret = t;
                }
            }

            if (ret != null) {
                notifyGet_();
                return ret;
            }

            if (start == -1)
                start = System.currentTimeMillis();

            int spinctr = SPINS_PUT_;
            while (numberOfUsedSlots.get() >= buffer.length) {
                if (waitTime <= 0) {
                    return null;
                }
                // busy wait
                if (spinctr > 0) {
                    if (YIELD_PUT_)
                        Thread.yield();
                    spinctr--;
                } else {
                    // block on lock
                    waitPut_(timeoutInMillis);
                }
                waitTime = timeoutInMillis - (System.currentTimeMillis() - start);
            }
        }
        // D312598 - end

    }

    /**
     * Puts an object into the buffer. If the buffer is full,
     * the call will block for up to the specified amount of
     * time, waiting for space to be freed up.
     *
     * @param x the object being placed into the buffer.
     * @param timeout the maximum amount of time
     *            that the caller is willing to wait if the buffer is full.
     * @return true if the object was added to the buffer; false if
     *         it was not added before the timeout expired.
     */
    @Override
    public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {

        if (t == null) {
            throw new IllegalArgumentException();
        }

        //TODO: Add expedited offer/fix waiting logic

        // D220640: Next two lines pulled out of synchronization block:
        long timeoutMS = unit.toMillis(timeout);
        long start = (timeoutMS <= 0) ? 0 : -1;
        long waitTime = timeoutMS;

        // D312598 - begin
        boolean ret = false;
        while (true) {
            synchronized (lock) {
                if (numberOfUsedSlots.get() < buffer.length) {
                    insert(t);
                    numberOfUsedSlots.getAndIncrement();
                    ret = true;
                }
            }

            if (ret) {
                notifyGet_();
                return true;
            }

            if (start == -1)
                start = System.currentTimeMillis();

            int spinctr = SPINS_PUT_;
            while (numberOfUsedSlots.get() >= buffer.length) {
                if (waitTime <= 0) {
                    return false;
                }
                // busy wait
                if (spinctr > 0) {
                    if (YIELD_PUT_)
                        Thread.yield();
                    spinctr--;
                } else {
                    // block on lock
                    waitPut_(waitTime);
                }
                waitTime = timeoutMS - (System.currentTimeMillis() - start);
            }
        }
        // D312598 - end

    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object)
     */
    @Override
    public boolean offer(T t) {
        if (t == null) {
            throw new IllegalArgumentException();
        }

        boolean ret = false;
        synchronized (lock) {
            if (t instanceof QueueItem && ((QueueItem) t).isExpedited()) {

                if (numberOfUsedExpeditedSlots.get() < expeditedBuffer.length) {
                    expeditedInsert(t);
                    numberOfUsedExpeditedSlots.getAndIncrement();
                    ret = true;
                }
            } else {
                if (numberOfUsedSlots.get() < buffer.length) {
                    insert(t);
                    numberOfUsedSlots.getAndIncrement();
                    ret = true;
                }
            }
        }

        if (ret) {
            notifyGet_();
            return true;
        }

        return false;
    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.concurrent.BlockingQueue#add(java.lang.Object)
     */
    @Override
    public boolean add(T t) {
        if (offer(t))
            return true;
        else
            throw new IllegalStateException("Queue full");
    }

    /**
     * Removes an object from the buffer. If the buffer is
     * empty, then the call blocks until something becomes
     * available.
     *
     * @return Object the next object from the buffer.
     */
    @Override
    public T take() throws InterruptedException {
        T old = poll();

        while (old == null) {
            waitGet_(-1);
            old = poll();
        }

        return old;
    }

    /**
     * Removes an object from the buffer. If the buffer is empty, the call blocks for up to
     * a specified amount of time before it gives up.
     *
     * @param timeout -
     *            the amount of time, that the caller is willing to wait
     *            in the event of an empty buffer.
     * @param unit -
     *            the unit of time
     */
    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        T old = poll();
        long endTimeMillis = System.currentTimeMillis() + unit.toMillis(timeout);
        long timeLeftMillis = endTimeMillis - System.currentTimeMillis();
        int spinctr = SPINS_TAKE_;

        while (old == null && timeLeftMillis > 0) {
            while (size() <= 0 && timeLeftMillis > 0) {
                if (spinctr > 0) {
                    // busy wait
                    if (YIELD_TAKE_)
                        Thread.yield();
                    spinctr--;
                } else {
                    // block on lock
                    waitGet_(timeLeftMillis);
                }
                timeLeftMillis = endTimeMillis - System.currentTimeMillis();
            }

            old = poll();
            timeLeftMillis = endTimeMillis - System.currentTimeMillis();
        }

        return old;
    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.Queue#poll()
     */
    @Override
    public T poll() {
        T old = null;

        boolean expedited = false;

        synchronized (this) {
            if (numberOfUsedExpeditedSlots.get() > 0) {
                old = expeditedExtract();
                numberOfUsedExpeditedSlots.getAndDecrement();
                expedited = true;
            } else if (numberOfUsedSlots.get() > 0) {
                old = extract();
                numberOfUsedSlots.getAndDecrement();
            }
        }

        if (old != null) {
            //TODO if expedited is added for put or offer with timeout add notification here
            if (!expedited)
                notifyPut_();
        }

        return old;
    }

    /*
     * @awisniew - ADDED
     *
     * Same as poll, only throws exception if queue is empty
     *
     * @see java.util.Queue#remove()
     */
    @Override
    public T remove() {

        T retrievedElement = poll();

        if (retrievedElement == null) {
            throw new NoSuchElementException();
        }

        return retrievedElement;
    }

    /**
     * Inserts an object into the buffer. Note that
     * since there is no synchronization, it is assumed
     * that this is done outside the scope of this call.
     */
    private final void insert(T t) {
        buffer[putIndex] = t;

        if (++putIndex >= buffer.length) {
            putIndex = 0;
        }
    }

    /**
     * Inserts an object into the expeditedBuffer. Note that
     * since there is no synchronization, it is assumed
     * that this is done outside the scope of this call.
     */
    private final void expeditedInsert(T t) {
        expeditedBuffer[expeditedPutIndex] = t;

        if (++expeditedPutIndex >= expeditedBuffer.length) {
            expeditedPutIndex = 0;
        }
    }

    /**
     * Removes an object from the buffer. Note that
     * since there is no synchronization, it is assumed
     * that this is done outside the scope of this call.
     */
    private final T extract() {
        T old = buffer[takeIndex];
        buffer[takeIndex] = null;

        if (++takeIndex >= buffer.length)
            takeIndex = 0;

        return old;
    }

    /**
     * Removes an object from the expeditedBuffer. Note that
     * since there is no synchronization, it is assumed
     * that this is done outside the scope of this call.
     */
    private final T expeditedExtract() {
        T old = expeditedBuffer[expeditedTakeIndex];
        expeditedBuffer[expeditedTakeIndex] = null;

        if (++expeditedTakeIndex >= expeditedBuffer.length)
            expeditedTakeIndex = 0;

        return old;
    }

    /**
     * Increases the buffer's capacity by the given amount.
     *
     * @param additionalCapacity
     *            The amount by which the buffer's capacity should be increased.
     */
    @SuppressWarnings("unchecked")
    public synchronized void expand(int additionalCapacity) {
        if (additionalCapacity <= 0) {
            throw new IllegalArgumentException();
        }

        int capacityBefore = buffer.length;
        synchronized (lock) { // D312598
            int capacityAfter = buffer.length;
            //Check that no one was expanding while we waited on this lock
            if (capacityAfter == capacityBefore) {
                final Object[] newBuffer = new Object[buffer.length + additionalCapacity];

                // PK53203 - put() acquires two locks in sequence.  First, it acquires
                // the insert lock to update putIndex.  Then, it drops the insert lock
                // and acquires the extract lock to update numberOfUsedSlots.  As a
                // result, there is a window where putIndex has been updated, but
                // numberOfUsedSlots has not.  Consequently, even though we have
                // acquired both locks in this method, we cannot rely on the values in
                // numberOfUsedSlots; we can only rely on putIndex and takeIndex.

                if (putIndex > takeIndex) {
                    // The contents of the buffer do not wrap round
                    // the end of the array. We can move its contents
                    // into the new expanded buffer in one go.

                    int used = putIndex - takeIndex;
                    System.arraycopy(buffer, takeIndex, newBuffer, 0, used);
                    putIndex = used;

                    // PK53203.1 - If putIndex == takeIndex, then the buffer is either
                    // completely full or completely empty.  If it is completely full, then
                    // we need to copy and adjust putIndex.  Otherwise, we need to set
                    // putIndex to 0.
                } else if (putIndex != takeIndex || buffer[takeIndex] != null) {
                    // The contents of the buffer wrap round the end
                    // of the array. We have to perform two copies to
                    // move its contents into the new buffer.

                    int used = buffer.length - takeIndex;
                    System.arraycopy(buffer, takeIndex, newBuffer, 0, used);
                    System.arraycopy(buffer, 0, newBuffer, used, putIndex);
                    putIndex += used;
                } else {
                    putIndex = 0;
                }

                // The contents of the buffer now begin at 0 - update the head pointer.

                takeIndex = 0;

                // The buffer's capacity has been increased so update the count of the
                // empty slots to reflect this.
                buffer = (T[]) newBuffer;
            }
        } // D312598
    }

    /**
     * Increases the expedited buffer's capacity by the given amount.
     *
     * @param additionalCapacity
     *            The amount by which the expedited buffer's capacity should be increased.
     */
    @SuppressWarnings("unchecked")
    public synchronized void expandExpedited(int additionalCapacity) {
        if (additionalCapacity <= 0) {
            throw new IllegalArgumentException();
        }

        int capacityBefore = expeditedBuffer.length;
        synchronized (lock) { // D312598
            int capacityAfter = expeditedBuffer.length;
            //Check that no one was expanding while we waited on this lock
            if (capacityAfter == capacityBefore) {
                final Object[] newBuffer = new Object[expeditedBuffer.length + additionalCapacity];

                // PK53203 - put() acquires two locks in sequence.  First, it acquires
                // the insert lock to update putIndex.  Then, it drops the insert lock
                // and acquires the extract lock to update numberOfUsedSlots.  As a
                // result, there is a window where putIndex has been updated, but
                // numberOfUsedSlots has not.  Consequently, even though we have
                // acquired both locks in this method, we cannot rely on the values in
                // numberOfUsedSlots; we can only rely on putIndex and takeIndex.

                if (expeditedPutIndex > expeditedTakeIndex) {
                    // The contents of the buffer do not wrap round
                    // the end of the array. We can move its contents
                    // into the new expanded buffer in one go.

                    int used = expeditedPutIndex - expeditedTakeIndex;
                    System.arraycopy(expeditedBuffer, expeditedTakeIndex, newBuffer, 0, used);
                    expeditedPutIndex = used;

                    // PK53203.1 - If putIndex == takeIndex, then the buffer is either
                    // completely full or completely empty.  If it is completely full, then
                    // we need to copy and adjust putIndex.  Otherwise, we need to set
                    // putIndex to 0.
                } else if (expeditedPutIndex != expeditedTakeIndex || expeditedBuffer[expeditedTakeIndex] != null) {
                    // The contents of the buffer wrap round the end
                    // of the array. We have to perform two copies to
                    // move its contents into the new buffer.

                    int used = expeditedBuffer.length - expeditedTakeIndex;
                    System.arraycopy(expeditedBuffer, expeditedTakeIndex, newBuffer, 0, used);
                    System.arraycopy(expeditedBuffer, 0, newBuffer, used, expeditedPutIndex);
                    expeditedPutIndex += used;
                } else {
                    expeditedPutIndex = 0;
                }

                // The contents of the buffer now begin at 0 - update the head pointer.

                expeditedTakeIndex = 0;

                // The buffer's capacity has been increased so update the count of the
                // empty slots to reflect this.
                expeditedBuffer = (T[]) newBuffer;
            }
        } // D312598
    }

    private static class BoundedBufferLock extends Object {
        // An easily-identified marker class used for locking
    }

    // F743-11444 - New method
    // F743-12896 - Start
    protected synchronized boolean cancel(Object x) {
        // First check the expedited buffer
        synchronized (lock) {
            if (expeditedPutIndex > expeditedTakeIndex) {
                for (int i = expeditedTakeIndex; i < expeditedPutIndex; i++) {
                    if (expeditedBuffer[i] == x) {
                        System.arraycopy(expeditedBuffer, i + 1, expeditedBuffer, i, expeditedPutIndex - i - 1);
                        expeditedPutIndex--;
                        expeditedBuffer[expeditedPutIndex] = null;
                        numberOfUsedExpeditedSlots.getAndDecrement(); // D615053
                        return true;
                    }
                }
            } else if (expeditedPutIndex != expeditedTakeIndex || expeditedBuffer[expeditedTakeIndex] != null) {
                for (int i = expeditedTakeIndex; i < buffer.length; i++) {
                    if (expeditedBuffer[i] == x) {
                        if (i != expeditedBuffer.length - 1) {
                            System.arraycopy(expeditedBuffer, i + 1, expeditedBuffer, i, expeditedBuffer.length - i - 1);
                        }

                        if (expeditedPutIndex != 0) {
                            expeditedBuffer[expeditedBuffer.length - 1] = expeditedBuffer[0];
                            System.arraycopy(expeditedBuffer, 1, expeditedBuffer, 0, expeditedPutIndex - 1);
                            expeditedPutIndex--;
                        } else {
                            expeditedPutIndex = expeditedBuffer.length - 1;
                        }

                        expeditedBuffer[expeditedPutIndex] = null;
                        numberOfUsedExpeditedSlots.getAndDecrement(); // D615053
                        return true;
                    }
                }

                // D610567 - Scan first section of expedited BoundedBuffer
                for (int i = 0; i < expeditedPutIndex; i++) {
                    if (expeditedBuffer[i] == x) {
                        System.arraycopy(expeditedBuffer, i + 1, expeditedBuffer, i, expeditedPutIndex - i - 1);
                        expeditedPutIndex--;
                        expeditedBuffer[expeditedPutIndex] = null;
                        numberOfUsedExpeditedSlots.getAndDecrement(); // D615053
                        return true;
                    }
                }

            }
            // Next check the main buffer
            if (putIndex > takeIndex) {
                for (int i = takeIndex; i < putIndex; i++) {
                    if (buffer[i] == x) {
                        System.arraycopy(buffer, i + 1, buffer, i, putIndex - i - 1);
                        putIndex--;
                        buffer[putIndex] = null;
                        numberOfUsedSlots.getAndDecrement(); // D615053
                        return true;
                    }
                }
            } else if (putIndex != takeIndex || buffer[takeIndex] != null) {
                for (int i = takeIndex; i < buffer.length; i++) {
                    if (buffer[i] == x) {
                        if (i != buffer.length - 1) {
                            System.arraycopy(buffer, i + 1, buffer, i, buffer.length - i - 1);
                        }

                        if (putIndex != 0) {
                            buffer[buffer.length - 1] = buffer[0];
                            System.arraycopy(buffer, 1, buffer, 0, putIndex - 1);
                            putIndex--;
                        } else {
                            putIndex = buffer.length - 1;
                        }

                        buffer[putIndex] = null;
                        numberOfUsedSlots.getAndDecrement(); // D615053
                        return true;
                    }
                }

                // D610567 - Scan first section of BoundedBuffer
                for (int i = 0; i < putIndex; i++) {
                    if (buffer[i] == x) {
                        System.arraycopy(buffer, i + 1, buffer, i, putIndex - i - 1);
                        putIndex--;
                        buffer[putIndex] = null;
                        numberOfUsedSlots.getAndDecrement(); // D615053
                        return true;
                    }
                }

            }
        }

        return false;
    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.Collection#iterator()
     */
    @Override
    public Iterator<T> iterator() {
        List<T> bufferAsList = new ArrayList<T>();
        synchronized (this) {
            synchronized (lock) {
                //First add the items in the expedited buffer
                //Check if we wrap around the end of the array before iterating
                if (expeditedPutIndex > expeditedTakeIndex) {
                    for (int i = expeditedTakeIndex; i <= expeditedPutIndex; i++) {
                        bufferAsList.add(expeditedBuffer[i]);
                    }
                } else {
                    //We wrap around the array. Loop through in two passes(upper and lower)
                    for (int i = expeditedTakeIndex; i < expeditedBuffer.length; i++) {
                        bufferAsList.add(expeditedBuffer[i]);
                    }
                    for (int i = 0; i < expeditedPutIndex; i++) {
                        bufferAsList.add(expeditedBuffer[i]);
                    }
                }
                //Next add the items in the main buffer
                //Check if we wrap around the end of the array before iterating
                if (putIndex > takeIndex) {
                    for (int i = takeIndex; i <= putIndex; i++) {
                        bufferAsList.add(buffer[i]);
                    }
                } else {
                    //We wrap around the array. Loop through in two passes(upper and lower)
                    for (int i = takeIndex; i < buffer.length; i++) {
                        bufferAsList.add(buffer[i]);
                    }
                    for (int i = 0; i < putIndex; i++) {
                        bufferAsList.add(buffer[i]);
                    }
                }
            }
        }
        return bufferAsList.iterator();
    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.Collection#toArray()
     */
    @Override
    public Object[] toArray() {
        int size = size();
        Object[] retArray;
        if (size < 1) {
            retArray = new Object[] {};
        } else {

            retArray = new Object[size];
            int retArrayIndex = 0;

            synchronized (this) {

                synchronized (lock) {
                    //Add the items in the expedited buffer first
                    //Check if we wrap around the end of the array before iterating
                    if (expeditedPutIndex > expeditedTakeIndex) {
                        for (int i = expeditedTakeIndex; i <= expeditedPutIndex; i++) {
                            retArray[retArrayIndex] = expeditedBuffer[i];
                            retArrayIndex++;
                        }
                    } else {
                        //We wrap around the array. Loop through in two passes(upper and lower)
                        for (int i = expeditedTakeIndex; i < expeditedBuffer.length; i++) {
                            retArray[retArrayIndex] = expeditedBuffer[i];
                            retArrayIndex++;
                        }
                        for (int i = 0; i < expeditedPutIndex; i++) {
                            retArray[retArrayIndex] = expeditedBuffer[i];
                            retArrayIndex++;
                        }
                    }

                    //Next add the items in the main buffer
                    //Check if we wrap around the end of the array before iterating
                    if (putIndex > takeIndex) {
                        for (int i = takeIndex; i <= putIndex; i++) {
                            retArray[retArrayIndex] = buffer[i];
                            retArrayIndex++;
                        }
                    } else {
                        //We wrap around the array. Loop through in two passes(upper and lower)
                        for (int i = takeIndex; i < buffer.length; i++) {
                            retArray[retArrayIndex] = buffer[i];
                            retArrayIndex++;
                        }
                        for (int i = 0; i < putIndex; i++) {
                            retArray[retArrayIndex] = buffer[i];
                            retArrayIndex++;
                        }
                    }
                }
            }
        }
        return retArray;

    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.Collection#toArray(T[])
     */
    @Override
    @SuppressWarnings("unchecked")
    public <E> E[] toArray(E[] a) {

        if (a.length < size()) {
            a = (E[]) Array.newInstance(a.getClass().getComponentType(), size());
        }

        int aIndex = 0;
        synchronized (this) {

            synchronized (lock) {
                //First add anything in the expedited buffer
                //Check if we wrap around the end of the array before iterating
                if (expeditedPutIndex > expeditedTakeIndex) {
                    for (int i = expeditedTakeIndex; i <= expeditedPutIndex; i++) {
                        a[aIndex] = (E) expeditedBuffer[i];
                        aIndex++;
                    }
                } else {
                    //We wrap around the array. Loop through in two passes(upper and lower)
                    for (int i = expeditedTakeIndex; i < expeditedBuffer.length; i++) {
                        a[aIndex] = (E) expeditedBuffer[i];
                        aIndex++;
                    }
                    for (int i = 0; i < expeditedPutIndex; i++) {
                        a[aIndex] = (E) expeditedBuffer[i];
                        aIndex++;
                    }
                }

                //Now add the items in the main buffer
                //Check if we wrap around the end of the array before iterating
                if (putIndex > takeIndex) {
                    for (int i = takeIndex; i <= putIndex; i++) {
                        a[aIndex] = (E) buffer[i];
                        aIndex++;
                    }
                } else {
                    //We wrap around the array. Loop through in two passes(upper and lower)
                    for (int i = takeIndex; i < buffer.length; i++) {
                        a[aIndex] = (E) buffer[i];
                        aIndex++;
                    }
                    for (int i = 0; i < putIndex; i++) {
                        a[aIndex] = (E) buffer[i];
                        aIndex++;
                    }
                }
            }
        }

        if (a.length > size()) {
            a[size()] = null;
        }
        return a;
    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.concurrent.BlockingQueue#contains(java.lang.Object)
     */
    @Override
    public boolean contains(Object o) {
        synchronized (this) {

            synchronized (lock) {
                //First check the expedited buffer
                //Check if we wrap around the end of the array before iterating
                if (expeditedPutIndex > expeditedTakeIndex) {
                    for (int i = expeditedTakeIndex; i <= expeditedPutIndex; i++) {
                        if (o.equals(expeditedBuffer[i])) {
                            return true;
                        }
                    }
                } else {
                    //We wrap around the array. Loop through in two passes(upper and lower)
                    for (int i = expeditedTakeIndex; i < expeditedBuffer.length; i++) {
                        if (o.equals(expeditedBuffer[i])) {
                            return true;
                        }
                    }
                    for (int i = 0; i < expeditedPutIndex; i++) {
                        if (o.equals(expeditedBuffer[i])) {
                            return true;
                        }
                    }
                }

                //Next check the main buffer
                //Check if we wrap around the end of the array before iterating
                if (putIndex > takeIndex) {
                    for (int i = takeIndex; i <= putIndex; i++) {
                        if (o.equals(buffer[i])) {
                            return true;
                        }
                    }
                } else {
                    //We wrap around the array. Loop through in two passes(upper and lower)
                    for (int i = takeIndex; i < buffer.length; i++) {
                        if (o.equals(buffer[i])) {
                            return true;
                        }
                    }
                    for (int i = 0; i < putIndex; i++) {
                        if (o.equals(buffer[i])) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.Collection#containsAll(java.util.Collection)
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object e : c)
            if (!contains(e))
                return false;
        return true;
    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.concurrent.BlockingQueue#remove(java.lang.Object)
     */
    @Override
    public boolean remove(Object o) {

        if (o == null) {
            return false;
        }

        if (size() == 0) {
            return false;
        }

        synchronized (this) {
            //First check the expedited buffer
            synchronized (lock) {
                //Check if we wrap around the end of the array before iterating
                if (expeditedPutIndex > expeditedTakeIndex) {
                    for (int i = expeditedTakeIndex; i <= expeditedPutIndex; i++) {
                        if (o.equals(expeditedBuffer[i])) {
                            //Remove element and shift all remaining elements
                            for (int j = i; j < expeditedPutIndex; j++) {
                                expeditedBuffer[j] = expeditedBuffer[j + 1];
                            }
                            //Null the putIndex
                            expeditedBuffer[expeditedPutIndex] = null;
                            expeditedPutIndex--;

                            //Decrement used slots counter
                            numberOfUsedExpeditedSlots.getAndDecrement();

                            //TODO if expedited is added for put or offer with timeout add notification here

                            return true;
                        }
                    }
                } else {
                    //We wrap around the array. Loop through in two passes(upper and lower)
                    for (int i = expeditedTakeIndex; i < expeditedBuffer.length; i++) {
                        if (o.equals(expeditedBuffer[i])) {
                            //Remove element and shift all remaining elements up
                            for (int j = i; j > expeditedTakeIndex; j--) {
                                expeditedBuffer[j] = expeditedBuffer[j - 1];
                            }
                            //Null the putIndex
                            expeditedBuffer[expeditedTakeIndex] = null;
                            if (expeditedTakeIndex == expeditedBuffer.length - 1) {
                                expeditedTakeIndex = 0;
                            } else {
                                expeditedTakeIndex++;
                            }

                            //Decrement used slots counter
                            numberOfUsedExpeditedSlots.getAndDecrement();

                            //TODO if expedited is added for put or offer with timeout add notification here

                            return true;
                        }
                    }
                    for (int i = 0; i < expeditedPutIndex; i++) {
                        if (o.equals(expeditedBuffer[i])) {
                            //Remove element and shift all remaining elements down
                            for (int j = i; j < expeditedPutIndex; j++) {
                                expeditedBuffer[j] = expeditedBuffer[j + 1];
                            }
                            //Null the putIndex
                            expeditedBuffer[expeditedPutIndex] = null;
                            expeditedPutIndex--;

                            //Decrement used slots counter
                            numberOfUsedExpeditedSlots.getAndDecrement();

                            //TODO if expedited is added for put or offer with timeout add notification here

                            return true;
                        }
                    }
                }

                //Next check the main buffer
                //Check if we wrap around the end of the array before iterating
                if (putIndex > takeIndex) {
                    for (int i = takeIndex; i <= putIndex; i++) {
                        if (o.equals(buffer[i])) {
                            //Remove element and shift all remaining elements
                            for (int j = i; j < putIndex; j++) {
                                buffer[j] = buffer[j + 1];
                            }
                            //Null the putIndex
                            buffer[putIndex] = null;
                            putIndex--;

                            //Decrement used slots counter
                            numberOfUsedSlots.getAndDecrement();

                            //Notify a waiting put thread that space has cleared
                            notifyPut_();

                            return true;
                        }
                    }
                } else {
                    //We wrap around the array. Loop through in two passes(upper and lower)
                    for (int i = takeIndex; i < buffer.length; i++) {
                        if (o.equals(buffer[i])) {
                            //Remove element and shift all remaining elements up
                            for (int j = i; j > takeIndex; j--) {
                                buffer[j] = buffer[j - 1];
                            }
                            //Null the putIndex
                            buffer[takeIndex] = null;
                            if (takeIndex == buffer.length - 1) {
                                takeIndex = 0;
                            } else {
                                takeIndex++;
                            }

                            //Decrement used slots counter
                            numberOfUsedSlots.getAndDecrement();

                            //Notify a waiting put thread that space has cleared
                            notifyPut_();

                            return true;
                        }
                    }
                    for (int i = 0; i < putIndex; i++) {
                        if (o.equals(buffer[i])) {
                            //Remove element and shift all remaining elements down
                            for (int j = i; j < putIndex; j++) {
                                buffer[j] = buffer[j + 1];
                            }
                            //Null the putIndex
                            buffer[putIndex] = null;
                            putIndex--;

                            //Decrement used slots counter
                            numberOfUsedSlots.getAndDecrement();

                            //Notify a waiting put thread that space has cleared
                            notifyPut_();

                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection)
     */
    @Override
    public int drainTo(Collection<? super T> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection, int)
     */
    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {

        //TODO- The elements aren't added to the given collection...

        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();

        int n = Math.min(maxElements, size());

        int numRemaining = n;

        synchronized (this) {
            synchronized (lock) {
                //Retrieve and remove at most n elements from the expedited buffer and add them to the passed collection
                for (int i = 0; i < n; i++) {
                    T retrieved = expeditedExtract();
                    numRemaining = n - i;
                    if (retrieved != null) {
                        numberOfUsedExpeditedSlots.getAndDecrement();
                        //TODO if expedited is added for put or offer with timeout add notification here
                    } else {
                        break; //retrieved is null so nothing left in the expedited buffer, move on to the main buffer
                    }
                }
                //Retrieve and remove at most numRemaining elements and add them to the passed collection
                for (int i = 0; i < numRemaining; i++) {
                    T retrieved = extract();
                    numberOfUsedSlots.getAndDecrement();
                    if (retrieved != null) {
                        notifyPut_();
                    }
                }
            }
        }
        return n;
    }

    //----------------------
    // Unsupported methods
    //----------------------

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll(Collection<? extends T> c) {
        //Optional Collection method
        throw new UnsupportedOperationException();
    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.Collection#removeAll(java.util.Collection)
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        //Optional Collection method
        throw new UnsupportedOperationException();
    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.Collection#retainAll(java.util.Collection)
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        //Optional Collection method
        throw new UnsupportedOperationException();
    }

    /*
     * @awisniew - ADDED
     *
     * (non-Javadoc)
     *
     * @see java.util.Collection#clear()
     */
    @Override
    public void clear() {
        //Optional Collection method
        throw new UnsupportedOperationException();
    }

    //@awisniew - DELETED
    //public void setThreadPool(ThreadPool threadPool) {
    //    threadPool_ = threadPool;
    //}
}
