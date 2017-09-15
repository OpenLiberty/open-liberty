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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A slight enhancement over simple Object.wait() and notify(), this class allows us to check
 * that something significant actually happened to occasion a wake-up. This helps avoid the
 * spurious wake-up phenomenon for which Java's Object.wait() is so famous.
 */
class EventLock implements Serializable {
    private static final long serialVersionUID = 1L;

    private final AtomicInteger eventCount = new AtomicInteger(0);

    final int getEventCount() {
        return eventCount.get();
    }

    /**
     * Determine whether any new events have been posted.
     * 
     * @param oldEventCount the number of events at the start time, for comparison
     * @return <code>true</code> iff a new event has been posted.
     */
    final boolean eventPosted(int oldEventCount) {
        return eventCount.get() != oldEventCount;
    }

    /** Post an event. */
    void postEvent() {
        // note that a new event has been posted
        eventCount.incrementAndGet();
        synchronized (this) {
            this.notifyAll();
        }
    }

    /**
     * Wait for an event to be posted.
     * 
     * @param oldEventCount keep the old eventCount on the stack for later comparison
     * @return <code>true</code> if an event was posted, and
     *         <code>false</code> if the wait timed out.
     * @throws InterruptedException if the thread was interrupted while waiting.
     */
    synchronized boolean wait(int oldEventCount) throws InterruptedException {
        while (!!!eventPosted(oldEventCount)) {
            this.wait();
        }
        return true;
    }

    boolean canTimeOut() {
        return false;
    }

    boolean hasTimedOut() {
        return false;
    }
}