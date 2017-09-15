/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A read/write lock mechanism with a simple, integrated event mechanism, as
 * on {@link Object}. This class ensures that the reader threads waiting for
 * an event do not hold onto their read locks, since that would deadlock any
 * writer thread attempting to post an event.
 */
class EventReadWriteLock extends ReentrantReadWriteLock {
    private static final long serialVersionUID = 1L;

    private final EventLock eventLock;

    EventReadWriteLock() {
        this.eventLock = new EventLock();
    }

    EventReadWriteLock(long nanoTimeout) {
        this.eventLock = nanoTimeout == 0 ? new EventLock() : new EventLockCumulativeTimeout(nanoTimeout);
    }

    boolean canTimeOut() {
        return eventLock.canTimeOut();
    }

    /**
     * Determine whether this lock has timed out. This method should be
     * overridden by any child class implementing a time-out feature.
     * 
     * @return <code>true</code> because timeout is not implemented
     */
    boolean hasTimedOut() {
        return eventLock.hasTimedOut();
    }

    /**
     * Temporarily suspend read locks and wait for an event to be posted.
     * Block until an event is posted.
     * <p>
     * N.B. this blocking method may wake up spuriously, and should be called from a loop that tests for the exit condition.
     * 
     * @return <code>true</code> if an event was posted, and
     *         <code>false</code> if the wait returned spuriously or timed out.
     * @throws InterruptedException if the thread was interrupted while waiting.
     * @throws IllegalStateException if the current thread does not hold a read lock
     * @throws IllegalStateException if the current thread holds a write lock
     */
    final boolean waitForEvent() throws InterruptedException {
        // must get the event count BEFORE releasing the read locks
        // to avoid a lost update
        final int oldEventCount = eventLock.getEventCount();
        final int readLockCount = releaseReadLocks();
        if (readLockCount == 0)
            throw new IllegalStateException("Must hold read lock");
        if (getWriteHoldCount() > 0)
            throw new IllegalStateException("Must not hold write lock");
        try {
            return eventLock.wait(oldEventCount);
        } finally {
            acquireReadLocks(readLockCount);
        }
    }

    /** Post an event. */
    void postEvent() {
        eventLock.postEvent();
    }

    /**
     * @return number of times read lock had to be unlocked for the calling thread
     */
    private int releaseReadLocks() {
        final int readHoldCount = getReadHoldCount();
        for (int i = 0; i < readHoldCount; i++)
            readLock().unlock();
        return readHoldCount;
    }

    private void acquireReadLocks(final int lockCount) {
        for (int i = 0; i < lockCount; i++)
            readLock().lock();
    }

    /**
     * @return number of times read lock had to be unlocked for the calling thread
     */
    public int releaseWriteLocks() {
        final int writeHoldCount = getWriteHoldCount();
        for (int i = 0; i < writeHoldCount; i++)
            writeLock().unlock();
        return writeHoldCount;
    }

    public void acquireWriteLocks(final int lockCount) {
        for (int i = 0; i < lockCount; i++)
            writeLock().lock();
    }

    public int releaseReadLocksAndAcquireWriteLock() {
        int lockCount = releaseReadLocks();
        writeLock().lock();
        return lockCount;
    }

    public void downgradeWriteLockToReadLocks(int lockCount) {
        acquireReadLocks(lockCount);
        writeLock().unlock();
    }

}
