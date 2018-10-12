/*******************************************************************************
 * Copyright (c) 2017,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.impl;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.faulttolerance.impl.async.QueuedFuture;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance.utils.FTDebug;

/**
 *
 */
public class TimeoutImpl {

    private static final TraceComponent tc = Tr.register(TimeoutImpl.class);

    private final String id;
    private final TimeoutPolicy timeoutPolicy;
    private final ScheduledExecutorService scheduledExecutorService;

    //lock must be held whenever reading or writing any of the following properties
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    //=========================================
    private Future<?> future; //the future which represents the scheduled timeout task
    private boolean timedout = false; //has the timeout popped?
    private boolean stopped = false; //has the timeout been stopped?
    private long start; //what relative nanoTime was the timeout started?
    private long targetEnd; //what relative nanoTime do we expect the timeout to occur
    private Runnable timeoutTask; //the task which will be run when the timeout does occur
    //=========================================

    /**
     * @param timeoutPolicy
     */
    public TimeoutImpl(String id, TimeoutPolicy timeoutPolicy, ScheduledExecutorService scheduledExecutorService) {
        this.id = id;
        this.timeoutPolicy = timeoutPolicy;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * start timer and interrupt given thread
     */
    public void start(Thread targetThread) {
        Runnable timeoutTask = () -> {
            targetThread.interrupt();
        };

        start(timeoutTask);
    }

    /**
     * start timer and cancel given future
     */
    public void start(QueuedFuture<?> queuedFuture) {
        Runnable timeoutTask = () -> {
            queuedFuture.abort(new TimeoutException());
        };

        start(timeoutTask);
    }

    /**
     * This method is run when the timer pops
     */
    private void timeout() {
        lock.writeLock().lock();
        try {
            //if already stopped, do nothing, otherwise check times and run the timeout task
            if (!this.stopped) {
                long now = System.nanoTime();
                long remaining = this.targetEnd - now;
                this.timedout = remaining <= FTConstants.MIN_TIMEOUT_NANO;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    debugTime("!Start  {0}", this.start);
                    debugTime("!Target {0}", this.targetEnd);
                    debugTime("!Now    {0}", now);
                    debugTime("!Remain {0}", remaining);
                }

                //if we really have timedout then run the timeout task
                if (this.timedout) {
                    debugRelativeTime("Timeout!");
                    this.timeoutTask.run();
                } else {
                    //this shouldn't be possible but if the timer popped too early, restart it
                    debugTime("Premature Timeout!", remaining);
                    start(this.timeoutTask, remaining);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get the timeout from the policy and start the timer
     *
     * @param timeoutTask
     */
    private void start(Runnable timeoutTask) {
        long timeout = timeoutPolicy.getTimeout().toNanos();
        start(timeoutTask, timeout);
    }

    /**
     * This is the method which actually starts the timer
     *
     * WARNING: This method uses System.nanoTime(). nanoTime is a point in time relative to an arbitrary point (fixed at runtime).
     * As a result, it could be positive or negative and will not bare any relation to the actual time ... it's just a relative measure.
     * Also, since it could be massively positive or negative, caution must be used when doing comparisons due to the possibility
     * of numerical overflow e.g. one should use t1 - t0 < 0, not t1 < t0
     * The reason we use this here is that it not affected by changes to the system clock at runtime
     *
     * @param timeoutTask
     * @param remainingNanos
     */
    private void start(Runnable timeoutTask, long remainingNanos) {
        lock.writeLock().lock();
        try {
            this.timeoutTask = timeoutTask;

            this.start = System.nanoTime();
            this.targetEnd = start + remainingNanos;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                debugTime(">Start  {0}", this.start);
                debugTime(">Target {0}", this.targetEnd);
                debugTime(">Now    {0}", this.start);
                debugTime(">Remain {0}", remainingNanos);
            }

            Runnable task = () -> {
                timeout();
            };

            if (remainingNanos > FTConstants.MIN_TIMEOUT_NANO) {
                this.future = scheduledExecutorService.schedule(task, remainingNanos, TimeUnit.NANOSECONDS);
            } else {
                task.run();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Stop the timeout ... mark it as stopped and cancel the scheduled future task if required
     */
    public void stop() {
        lock.writeLock().lock();
        try {
            debugRelativeTime("Stop!");
            this.stopped = true;
            if (this.future != null && !this.future.isDone()) {
                debugRelativeTime("Cancelling");
                this.future.cancel(true);
            }
            this.future = null;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Stop the timeout as above but also optionally check if the timedout flag was previously set. If it was then throw a TimeoutException.
     */
    public void stop(boolean exceptionOnTimeout) {
        stop();
        if (exceptionOnTimeout) {
            check();
        }
    }

    /**
     * Restart the timer ... stop the timer, reset the stopped flag and then start again with the same timeout policy
     */
    public void restart() {
        lock.writeLock().lock();
        try {
            if (this.timeoutTask == null) {
                throw new IllegalStateException(Tr.formatMessage(tc, "internal.error.CWMFT4999E"));
            }
            stop();
            this.stopped = false;
            start(this.timeoutTask);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Restart the timer on a new thread in synchronous mode.
     * <p>
     * In this mode, a timeout only causes the thread to be interrupted, it does not directly set the result of the QueuedFuture.
     * <p>
     * This is needed when doing Retries or Fallback on an async thread. If the result is set directly, then we have no opportunity to handle the exception.
     */
    public void runSyncOnNewThread(Thread newThread) {
        lock.writeLock().lock();
        try {
            if (this.timeoutTask == null) {
                throw new IllegalStateException(Tr.formatMessage(tc, "internal.error.CWMFT4999E"));
            }
            stop();
            this.stopped = false;

            long remaining = check();
            Runnable timeoutTask = () -> {
                newThread.interrupt();
            };

            start(timeoutTask, remaining);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Check if the timedout flag was previously set and throw an exception if it was.
     * Otherwise, return the remaining timeout time, in nanoseconds.
     *
     * @return the time remaining, in nanoseconds
     */
    public long check() {
        long remaining = 0;
        lock.readLock().lock();
        try {
            if (this.timedout) {
                // Note: this clears the interrupted flag if it was set
                // Assumption is that the interruption was caused by the Timeout
                boolean wasInterrupted = Thread.interrupted();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    if (wasInterrupted) {
                        Tr.debug(tc, "{0}: Throwing timeout exception and clearing interrupted flag", getDescriptor());
                    } else {
                        Tr.debug(tc, "{0}: Throwing timeout exception", getDescriptor());
                    }
                }

                throw new TimeoutException(Tr.formatMessage(tc, "timeout.occurred.CWMFT0000E"));
            }
            long now = System.nanoTime();
            remaining = this.targetEnd - now;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                debugTime("?Start ", this.start);
                debugTime("?Target", this.targetEnd);
                debugTime("?Now   ", now);
                debugTime("?Remain", remaining);
            }
        } finally {
            lock.readLock().unlock();
        }
        return remaining;
    }

    @Override
    @Trivial
    public String toString() {
        return getDescriptor();
    }

    @Trivial
    public String getDescriptor() {
        return "Timeout[" + this.id + "]";
    }

    /**
     * Output a debug message showing the time in seconds between this.start and now
     *
     * @param message
     */
    @Trivial
    private void debugRelativeTime(String message) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            FTDebug.debugRelativeTime(tc, getDescriptor(), message, this.start);
        }
    }

    /**
     * Output a debug message showing a given relative time, converted from nanos to seconds
     *
     * @param message
     * @param nanos
     */
    @Trivial
    private void debugTime(String message, long nanos) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            FTDebug.debugTime(tc, getDescriptor(), message, nanos);
        }
    }

    /**
     * Output a debug message showing the current relative time (nanoTime), converted from nanos to seconds
     *
     * @param message
     */
    @Trivial
    private void debugTime(String message) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            debugTime(message, System.nanoTime());
        }
    }

}
