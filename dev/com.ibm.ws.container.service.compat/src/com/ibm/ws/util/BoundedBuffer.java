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
package com.ibm.ws.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A fixed size FIFO of Objects. Null objects are not allowed in
 * the buffer.
 */
public class BoundedBuffer {
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //
    // Implementation Note: the buffer is implemented using a circular
    // array. Both a "head" and "tail" pointer (array index) are
    // maintained as well as an overall count of used/empty
    // slots:
    //
    // [TEM]: I do not know why both used and empty slot counts are
    // maintained given that one can be derived from the other
    // and the capacity. I suspect it is so that the two
    // values can vary on independent threads.
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
    // thread put operations and the empty slots counter. The two locks
    // are never held concurrently except in expand().
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
    // the taking threads stuck in wait() awaiting a put and the puting threads
    // stuck in wait() awaiting a notify from a successful take which can never
    // occur as the buffer's empty.
    //
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // PK77809 Number of threads waiting for work
    private int waitingThreads = 0;

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

    // D638088 - modified the set of WAIT_SLICE parameters
    private static final long WAIT_SHORT_SLICE_;
    private static final long WAIT_LONG_SLICE_;

    // D638088 - Added support members for lock splitting
    private static final int SPLIT_THRESH_;
    private static final int SPLIT_FACTOR_;
    private static final int MAX_COUNTER = Integer.MAX_VALUE / 2;

    static {

        // D638088 - modified spinning defaults to adjust to the number
        // of physical processors on host system.
        SPINS_TAKE_ = Integer.getInteger("com.ibm.ws.util.BoundedBuffer.spins_take", Runtime.getRuntime().availableProcessors() - 1).intValue(); // D371967
        SPINS_PUT_ = Integer.getInteger("com.ibm.ws.util.BoundedBuffer.spins_put", SPINS_TAKE_ / 4).intValue(); // D371967

        // D638088 - default split threshold to 50 (default web container thread
        // pool). This is
        // an important factor that threads to park themselves on a group of locks,
        // instead
        // of a single lock.
        SPLIT_THRESH_ = Integer.getInteger("com.ibm.ws.util.BoundedBuffer.split_thresh", 50).intValue();
        SPLIT_FACTOR_ = Integer.getInteger("com.ibm.ws.util.BoundedBuffer.split_factor", SPLIT_THRESH_).intValue();

        YIELD_TAKE_ = Boolean.getBoolean("com.ibm.ws.util.BoundedBuffer.yield_take");
        YIELD_PUT_ = Boolean.getBoolean("com.ibm.ws.util.BoundedBuffer.yield_put");

        // D638088 - created short/long wait periods, based on a multiplication
        // factor. Keeping
        // default behavior the same.
        WAIT_SHORT_SLICE_ = Long.getLong("com.ibm.ws.util.BoundedBuffer.wait", 1000).longValue(); // D371967
        WAIT_LONG_SLICE_ = Long.getLong("com.ibm.ws.util.BoundedBuffer.wait_long", 1000).longValue();
    }

    /*
     * D638088 - This is faster for small systems, and in the current
     * implementation the numUsedSlots field that uses the class
     * is not highly contended. This is mostly written to from within
     * other synchronized sections. numUsedSlots is read from many
     * unsynchronized places, so I've added read-write lock support
     * when for systems with greater than 4 CPUs.
     */
    private static class LiteAtomicInteger {

        // D638088 - changed synchronized to read/write lock since the
        // write case is not contended. The read case will be lighter
        // but still provide concurrency protection.
        private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
        private final Lock readLock = rwl.readLock();
        private final Lock writeLock = rwl.writeLock();
        private final boolean useRwl = Runtime.getRuntime().availableProcessors() > 4;
        private int val_;

        public LiteAtomicInteger(int v) {
            val_ = v;
        }

        final int get() {

            if (useRwl) {
                readLock.lock();
                try {
                    return val_;
                } finally {
                    readLock.unlock();
                }
            } else {
                synchronized (this) {
                    return val_;
                }
            }
        }

        final int getAndIncrement() {

            if (useRwl) {
                writeLock.lock();
                try {
                    return val_++;
                } finally {
                    writeLock.unlock();
                }
            } else {
                synchronized (this) {
                    return val_++;
                }
            }
        }

        final int getAndDecrement() {

            if (useRwl) {
                writeLock.lock();
                try {
                    return val_--;
                } finally {
                    writeLock.unlock();
                }
            } else {
                synchronized (this) {
                    return val_--;
                }
            }
        }
    }

    // D371967: An easily-identified marker class used for locking
    private static class PutQueueLock extends Object {}

    private final PutQueueLock putQueue_ = new PutQueueLock();
    private int putQueueLen_ = 0;

    // D638088: Added two fields to the getQueueLock for book keeping
    // D371967: An easily-identified marker class used for locking
    private class GetQueueLock extends Object {

        public int threadsWaiting = 0;
        public Thread shortWaiter = null;
    }

    // D638088 - now an array of lock instead of a single object
    private final GetQueueLock[] getQueueLocks_;

    // D638088 - use an atomic counter to maintain balanced load across
    // an array of lock objects. An object that needs to wait on a
    // lock should also increment the lock so that subsequent threads
    // will use a different lock. The notify case is not necessary to
    // increment for two reasons. 1) Increment has more contention with
    // CAS operation compared to a simple get. 2) It's more efficient
    // to start notification loop on the last lock index where a thread
    // has waited.
    private int getQueueIndex(Object[] queue, java.util.concurrent.atomic.AtomicInteger counter, boolean increment) {

        // fast path for single element array
        if (queue.length == 1) {
            return 0;
        } else if (increment) {

            // get the next long value from counter
            int i = counter.incrementAndGet();

            // rollover logic to make sure we stay within
            // the bounds of an integer. Not important that
            // this logic is highly accurate, ballpark is okay.
            if (i > MAX_COUNTER) {
                if (counter.compareAndSet(i, 0)) {
                    System.out.println("BoundedBuffer: reset counter to 0");
                }
            }
            return i % queue.length;
        } else {
            return counter.get() % queue.length;
        }
    }

    // D638088 - implemented split locks for get queue
    private void notifyGet_() {
        // a notification may be lost in some cases - however
        // as none of the threads wait endlessly, a waiting thread
        // will either be notified, or will eventually wakeup
        int lastWaitIndex = getQueueIndex(getQueueLocks_, getQueueCounter_, false);
        int lockIndex = lastWaitIndex;
        for (int i = 0; i < getQueueLocks_.length; i++) {

            // are threads waiting on this queue?
            if (getQueueLocks_[lockIndex].threadsWaiting > 0) {
                synchronized (getQueueLocks_[lockIndex]) {
                    // make sure we are actually notifying somebody
                    // now that the lock is held
                    if (getQueueLocks_[lockIndex].threadsWaiting > 0) {
                        getQueueLocks_[lockIndex].notify();
                        return;
                    } else {
                        // somebody stole my notify, make up for it
                        // by allowing another attempt
                        i--;
                    }
                }
            }

            // check to see whether a new thread has waited
            int checkIndex = getQueueIndex(getQueueLocks_, getQueueCounter_, false);
            if (checkIndex != lastWaitIndex) {
                // restart scan from the new wait index
                lockIndex = checkIndex;
                lastWaitIndex = lockIndex;
                i = 0;
            } else {
                // increment to next element
                lockIndex = ++lockIndex % getQueueLocks_.length;
            }
        }

        // D638088 - if we get here, the entire lock array was scanned and
        // nobody was found to notify.
    }

    // D638088 - implemented split locks for get queue
    private void waitGet_(long timeout) throws InterruptedException {

        // wait on a member of the lock array
        int lockIndex = getQueueIndex(getQueueLocks_, getQueueCounter_, true);
        synchronized (getQueueLocks_[lockIndex]) {
            try {

                // increment waiting threads
                getQueueLocks_[lockIndex].threadsWaiting++;

                // D497382 - Now that we have the getQueue_ lock, recheck the
                // condition from take/poll to try to minimize the number of
                // threads that wait but don't get notified.
                if (numberOfUsedSlots.get() <= 0) {

                    // determine timeout if unspecified
                    if (timeout < 0) {
                        if (WAIT_SHORT_SLICE_ != WAIT_LONG_SLICE_ && getQueueLocks_[lockIndex].shortWaiter == null) {

                            // use default timeout if not specified
                            timeout = WAIT_SHORT_SLICE_;

                            // there are no threads on waiting with short timeout,
                            // so this thread will use timeout value and set flag.
                            getQueueLocks_[lockIndex].shortWaiter = Thread.currentThread();
                        } else {

                            // there is already a thread on this queue waiting with
                            // short timeout set, so all other threads will wait a
                            // longer time to avoid contention of threads looking for
                            // work without being notified.
                            timeout = WAIT_LONG_SLICE_;
                        }
                    }

                    // notification through notifyGet_ may be lost here
                    getQueueLocks_[lockIndex].wait(timeout);
                }
            } catch (InterruptedException ex) {
                getQueueLocks_[lockIndex].notify();
                throw ex;
            } finally {

                // decrement waiting threads
                getQueueLocks_[lockIndex].threadsWaiting--;

                // check whether the current thread was waiting with a timeout.
                // if so, need to clear the flag to indicate that no thread is
                // waiting with timeout on this queue.
                if (WAIT_SHORT_SLICE_ != WAIT_LONG_SLICE_)
                    if (getQueueLocks_[lockIndex].shortWaiter == Thread.currentThread())
                        getQueueLocks_[lockIndex].shortWaiter = null;
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
            } catch (InterruptedException ex) {
                putQueue_.notify();
                throw ex;
            } finally {
                putQueueLen_--;
            }
        }
    }

    // D312598 - end

    private Object[] buffer; // the circular array (buffer)
    private int takeIndex = 0; // the beginning of the buffer
    private int putIndex = 0; // the end of the buffer
    private final LiteAtomicInteger numberOfUsedSlots = new LiteAtomicInteger(0); // D312598
    // D638088

    /**
     * Helper monitor to handle puts.
     */
    private final BoundedBufferLock lock = new BoundedBufferLock();
    private final AtomicInteger getQueueCounter_ = new AtomicInteger(0); // D638088

    /**
     * Create a BoundedBuffer with the given capacity.
     * 
     * @exception IllegalArgumentException
     *                if the requested capacity
     *                is less or equal to zero.
     */
    public BoundedBuffer(int capacity) throws IllegalArgumentException {

        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }

        // intitialize buffer array
        buffer = new Object[capacity];

        // initialize all the sub pools
        int subPoolLength = 1;
        if (capacity > SPLIT_THRESH_)
            subPoolLength = (int) Math.ceil((double) capacity / (double) SPLIT_FACTOR_);
        getQueueLocks_ = new GetQueueLock[subPoolLength];
        for (int i = 0; i < getQueueLocks_.length; i++) {
            getQueueLocks_[i] = new GetQueueLock();
        }

        // System.out.println("Created bounded buffer: capacity=" + capacity +
        // ", locks=" + subPoolLength);
    }

    /**
     * Return the number of elements in the buffer.
     * This is only a snapshot value, that may change
     * immediately after returning.
     */
    // D312598 - remove synchronization and use .get
    public int size() {
        return numberOfUsedSlots.get();
    }

    /**
     * Returns the overall capacity of the buffer.
     * Note that this is how much the buffer can hold,
     * not how much space is unused.
     */
    public int capacity() {
        return buffer.length;
    }

    /**
     * Peeks at the head of the buffer and returns the first
     * object (or null if empty). The object remains in the
     * buffer.
     */
    public Object peek() {
        synchronized (this) {
            if (numberOfUsedSlots.get() > 0) { // D312598
                return buffer[takeIndex];
            } else {
                return null;
            }
        }
    }

    /**
     * Puts an object into the buffer. If the buffer is full,
     * the call will block indefinitely until space is freed up.
     * 
     * @param x
     *            the object being placed in the buffer.
     * @exception IllegalArgumentException
     *                if the object is null.
     */
    public void put(Object x) throws InterruptedException {
        if (x == null) {
            throw new IllegalArgumentException();
        }

        // D186845 if (Thread.interrupted())
        // D186845 throw new InterruptedException();

        // D312598 - begin
        boolean ret = false;
        while (true) {
            synchronized (lock) {
                if (numberOfUsedSlots.get() < buffer.length) {
                    insert(x);
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
     * @param x
     *            the object being placed in the buffer.
     * @param timeoutInMillis
     *            the timeout period in milliseconds.
     * @return the object that was put into the buffer (x) or
     *         null in the event that the request timed out.
     * @exception IllegalArgumentException
     *                if the object is null.
     */
    public Object put(Object x, long timeoutInMillis) throws InterruptedException {
        if (x == null) {
            throw new IllegalArgumentException();
        }

        long start = (timeoutInMillis <= 0) ? 0 : -1;
        long waitTime = timeoutInMillis;

        // D312598 - begin
        Object ret = null;
        while (true) {
            synchronized (lock) {
                if (numberOfUsedSlots.get() < buffer.length) {
                    insert(x);
                    numberOfUsedSlots.getAndIncrement();
                    ret = x;
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
     * @param x
     *            the object being placed in the buffer.
     * @param timeoutInMillis
     *            the timeout period in milliseconds.
     * @param maximumCapacity
     *            the desired maximum capacity of the buffer
     * @return the object that was put into the buffer (x) or
     *         null in the event that the request timed out.
     * @exception IllegalArgumentException
     *                if the object is null or if the
     *                supplied maximum capacity exceeds the buffer's size.
     */
    public Object put(Object x, long timeoutInMillis, int maximumCapacity) throws InterruptedException {
        if ((x == null) || (maximumCapacity > buffer.length)) {
            throw new IllegalArgumentException();
        }

        long start = (timeoutInMillis <= 0) ? 0 : -1;
        long waitTime = timeoutInMillis;

        // D312598 - begin
        Object ret = null;
        while (true) {
            synchronized (lock) {
                if (numberOfUsedSlots.get() < maximumCapacity) {
                    insert(x);
                    numberOfUsedSlots.getAndIncrement();
                    ret = x;
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
     * @param x
     *            the object being placed into the buffer.
     * @param timeout
     *            the maximum amount of time (in milliseconds)
     *            that the caller is willing to wait if the buffer is full.
     * @return true if the object was added to the buffer; false if
     *         it was not added before the timeout expired.
     */
    public boolean offer(Object x, long timeout) throws InterruptedException {

        if (x == null) {
            throw new IllegalArgumentException();
        }

        // D220640: Next two lines pulled out of synchronization block:
        long start = (timeout <= 0) ? 0 : -1;
        long waitTime = timeout;

        // D312598 - begin
        boolean ret = false;
        while (true) {
            synchronized (lock) {
                if (numberOfUsedSlots.get() < buffer.length) {
                    insert(x);
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
                waitTime = timeout - (System.currentTimeMillis() - start);
            }
        }
        // D312598 - end

    }

    /**
     * Removes an object from the buffer. If the buffer is
     * empty, then the call blocks until something becomes
     * available.
     * 
     * @return Object the next object from the buffer.
     */
    public Object take() throws InterruptedException {

        Object old = null;

        // D312598 - begin
        while (true) {
            synchronized (this) {
                if (numberOfUsedSlots.get() > 0) {
                    old = extract();
                    numberOfUsedSlots.getAndDecrement();
                }
                if (old != null) {
                    // PK77809 Decrement the number of waiting threads since we will be
                    // returning a work item to a waiting thread to execute.
                    waitingThreads--;
                }
            }

            if (old != null) {
                notifyPut_();
                return old;
            }

            int spinctr = SPINS_TAKE_;
            while (numberOfUsedSlots.get() <= 0) {
                // busy wait
                if (spinctr > 0) {
                    if (YIELD_TAKE_)
                        Thread.yield();
                    spinctr--;
                } else {
                    // block on lock
                    // D638088 - Allow waitGet_ to determine timeout value by passing
                    // negative value as parameter.
                    waitGet_(-1);
                }
            }
        }
        // D312598 - end

    }

    /**
     * Removes an object from the buffer. If the buffer is empty, the call blocks
     * for up to
     * a specified amount of time before it gives up.
     * 
     * @param timeout
     *            -
     *            the amount of time (in milliseconds), that the caller is willing
     *            to wait
     *            in the event of an empty buffer.
     */
    public Object poll(long timeout) throws InterruptedException {

        Object old = null;

        // D220640: Next two lines pulled out of synchronization block:

        // E2E optimized long start = (timeout <= 0) ? 0 :
        // System.currentTimeMillis();
        long start = (timeout <= 0) ? 0 : -1;
        long waitTime = timeout;

        // D312598 - begin
        while (true) {
            synchronized (this) {
                if (numberOfUsedSlots.get() > 0) {
                    old = extract();
                    numberOfUsedSlots.getAndDecrement();
                }
                if (old != null) {
                    // PK77809 Decrement the number of waiting threads since we will be
                    // returning a work item to a waiting thread to execute.
                    waitingThreads--;
                }
            }

            if (old != null) {
                notifyPut_();
                return old;
            }

            if (start == -1)
                start = System.currentTimeMillis();

            int spinctr = SPINS_TAKE_;
            while (numberOfUsedSlots.get() <= 0) {
                if (waitTime <= 0) {
                    return null;
                }
                // busy wait
                if (spinctr > 0) {
                    if (YIELD_TAKE_)
                        Thread.yield();
                    spinctr--;
                } else {
                    // block on lock
                    waitGet_(waitTime);
                }
                waitTime = timeout - (System.currentTimeMillis() - start);
            }
        }
        // D312598 - end

    }

    // PK77809 methods to increment/decrement the number of waiting threads
    /**
     * Called when a new thread is added to the pool and will start calling
     * take/poll. This is used to increment the
     * waitingThreads var in the same sync block as the addThread in the
     * ThreadPool to prevent the race condition.
     */
    public synchronized void incrementWaitingThreads() {
        waitingThreads++;
    }

    public synchronized void decrementWaitingThreads() {
        waitingThreads--;
    }

    // PK77809 method to return the amount of excess work in the buffer. The
    // return
    // value will be negative if we have more work in the buffer than threads
    // waiting to do work. If there are more threads waiting for work than we
    // have in the buffer, this value will be positive.
    public synchronized int excessWaitingThreads() {
        return waitingThreads - size();
    }

    /**
     * Inserts an object into the buffer. Note that
     * since there is no synchronization, it is assumed
     * that this is done outside the scope of this call.
     */

    private final void insert(Object x) {
        buffer[putIndex] = x;

        if (++putIndex >= buffer.length) {
            putIndex = 0;
        }
    }

    /**
     * Removes an object from the buffer. Note that
     * since there is no synchronization, it is assumed
     * that this is done outside the scope of this call.
     */

    private final Object extract() {
        Object old = buffer[takeIndex];
        buffer[takeIndex] = null;

        if (++takeIndex >= buffer.length)
            takeIndex = 0;

        return old;
    }

    /**
     * Increases the buffer's capacity by the given amount.
     * 
     * @param additionalCapacity
     *            The amount by which the buffer's capacity should be increased.
     */
    public synchronized void expand(int additionalCapacity) {
        if (additionalCapacity <= 0) {
            throw new IllegalArgumentException();
        }

        synchronized (lock) { // D312598
            final Object[] newBuffer = new Object[buffer.length + additionalCapacity];

            // PK53203 - put() acquires two locks in sequence. First, it acquires
            // the insert lock to update putIndex. Then, it drops the insert lock
            // and acquires the extract lock to update numberOfUsedSlots. As a
            // result, there is a window where putIndex has been updated, but
            // numberOfUsedSlots has not. Consequently, even though we have
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
                // completely full or completely empty. If it is completely full, then
                // we need to copy and adjust putIndex. Otherwise, we need to set
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
            buffer = newBuffer;
        } // D312598
    }

    private static class BoundedBufferLock extends Object {
        // An easily-identified marker class used for locking
    }

    // F743-11444 - New method
    // F743-12896 - Start
    protected synchronized boolean cancel(Object x) {
        synchronized (lock) {
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

    // private void dump() {
    // final String nl = System.getProperty("line.separator");
    // StringBuilder sb = new StringBuilder(toString()).append('[');
    // for (int i = 0; i < buffer.length; i++) {
    // sb.append(nl)
    // .append("  ")
    // .append(i == takeIndex ? '<' : ' ')
    // .append(i == putIndex ? '>' : ' ')
    // .append(' ')
    // .append(buffer[i]);
    // }
    // sb.append(']');
    // System.out.println(sb);
    // }
    //
    // public static void main(String[] args) throws Exception {
    // Object[] objects = new Object[10];
    // for (int i = 0; i < objects.length; i++) {
    // objects[i] = "object" + i;
    // }
    //
    // System.out.println("delete empty");
    // BoundedBuffer bb = new BoundedBuffer(4);
    // bb.dump();
    // bb.cancel(objects[0]);
    // bb.dump();
    //
    // System.out.println("delete not found");
    // bb = new BoundedBuffer(4);
    // bb.put(objects[0]);
    // bb.put(objects[1]);
    // bb.dump();
    // bb.cancel(objects[2]);
    // bb.dump();
    //
    // System.out.println("delete current");
    // bb = new BoundedBuffer(4);
    // bb.put(objects[0]);
    // bb.dump();
    // bb.cancel(objects[0]);
    // bb.dump();
    //
    // System.out.println("delete full");
    // bb = new BoundedBuffer(4);
    // bb.put(objects[0]);
    // bb.put(objects[1]);
    // bb.put(objects[2]);
    // bb.put(objects[3]);
    // bb.cancel(objects[0]);
    // bb.dump();
    // bb.cancel(objects[2]);
    // bb.dump();
    //
    // System.out.println("delete wrap");
    // bb = new BoundedBuffer(4);
    // bb.put(objects[0]);
    // bb.put(objects[1]);
    // bb.take();
    // bb.take();
    // bb.dump();
    // bb.put(objects[2]);
    // bb.put(objects[3]);
    // bb.put(objects[4]);
    // bb.put(objects[5]);
    // bb.dump();
    // bb.cancel(objects[2]);
    // bb.dump();
    // bb.cancel(objects[3]);
    // bb.dump();
    // bb.cancel(objects[4]);
    // bb.dump();
    // bb.cancel(objects[5]);
    // bb.dump();
    //
    // System.out.println("delete duplicate");
    // bb = new BoundedBuffer(4);
    // bb.put(objects[0]);
    // bb.put(objects[1]);
    // bb.put(objects[2]);
    // bb.put(objects[0]);
    // bb.dump();
    // bb.cancel(objects[0]);
    // bb.dump();
    //
    // System.out.println("delete wrap reverse");
    // bb = new BoundedBuffer(4);
    // bb.put(objects[0]);
    // bb.put(objects[1]);
    // bb.take();
    // bb.take();
    // bb.dump();
    // bb.put(objects[2]);
    // bb.put(objects[3]);
    // bb.put(objects[4]);
    // bb.put(objects[5]);
    // bb.dump();
    // bb.cancel(objects[5]);
    // bb.dump();
    // bb.cancel(objects[4]);
    // bb.dump();
    // bb.cancel(objects[3]);
    // bb.dump();
    // bb.cancel(objects[2]);
    // bb.dump();
    //
    // }
    // F743-12896 - End

}
