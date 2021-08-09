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

/**
 * Wait for an event to be posted, allowing a cumulative timeout.
 * Once the timeout is reached, calls to {@link #wait(int)} will
 * no longer block.
 */
final class EventLockCumulativeTimeout extends EventLock {
    private static final long serialVersionUID = 1L;

    /** This is the original wait interval in nanoseconds. */
    private final long timeout;
    /**
     * This is the time waited so far - safe to be non-volatile.
     * The risk is that a thread may see an old value and consider
     * the object not timed out, but once it then calls {@link #wait(int)} the current value will be seen.
     */
    private long timeWaited;
    /** The starting point for the current timer */
    private long startTime;

    EventLockCumulativeTimeout(long nanos) {
        this.timeout = nanos;
    }

    @Override
    synchronized boolean wait(int oldEventCount) throws InterruptedException {
        while (waitNeeded(oldEventCount)) {
            startTimer();
            try {
                waitForEventOrTimeout();
            } finally {
                recordElapsedTime();
            }
        }
        return eventPosted(oldEventCount);
    }

    @Override
    synchronized void postEvent() {
        super.postEvent();
        stopTimer();
    }

    @Override
    boolean canTimeOut() {
        return true;
    }

    @Override
    boolean hasTimedOut() {
        return timeout <= timeWaited;
    }

    /** Start the timer. */
    private synchronized void startTimer() {
        if (startTime == 0)
            startTime = System.nanoTime();
    }

    /** Record the elapsed time and continue timing from now. */
    private synchronized void recordElapsedTime() {
        // do nothing if the timer is not started
        if (startTime == 0)
            return;
        long current = System.nanoTime();
        // It is reported that elapsed time can be negative on some platforms.
        // Use the absolute difference to avoid infinite waits.
        timeWaited += Math.abs(current - startTime);
        // continue timing from now
        startTime = current;
    }

    /** Record the elapsed time and stop the timer. */
    private synchronized void stopTimer() {
        // do nothing if the timer is not started
        if (startTime == 0)
            return;
        // It is reported that elapsed time can be negative on some platforms.
        // Use the absolute difference to avoid infinite waits.
        timeWaited += Math.abs(System.nanoTime() - startTime);
        startTime = 0;
    }

    /** Wait for a notification or a timeout - may wake spuriously. */
    private void waitForEventOrTimeout() throws InterruptedException {
        long remainingWait = timeout - timeWaited;
        long millisToWait = remainingWait / 1000000;
        int nanosToWait = (int) (remainingWait % 1000000);
        wait(millisToWait, nanosToWait);
    }

    private boolean waitNeeded(int oldEventCount) {
        if (eventPosted(oldEventCount))
            return false;
        if (hasTimedOut())
            return false;
        return true;
    }
}