/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.util.Date;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper for the ScheduledFuture returned on overridden scheduleWithFixedRate and scheduleWithFixedDelay methods.
 * It's used to present a consistent state of the submitted Runnable as it progresses through the underlying
 * ScheduledThreadPoolExecutor and its redirection into the Default ExecutorService.
 * 
 * @param <V>
 */
class SchedulingRunnableFixedHelper<V> implements ScheduledFuture<Object>, Runnable {

    ScheduledExecutorImpl m_scheduledExecutor;

    /**
     * Wrapped Runnable related to this future instance
     */
    protected Runnable m_runnable;

    /**
     * Done state of submitted Runnable
     */
    volatile boolean m_isDone = false;

    /**
     * Result from first cancel() call.
     */
    volatile boolean m_cancelResult = false;

    /**
     * ScheduleFuture from invoking our schedule(Runnable command, long delay, TimeUnit unit)
     */
    ScheduledFuture<?> m_schedFuture;

    /**
     * Used to provide a consistent state.
     */
    CountDownLatch m_coordinationLatch;

    /**
     * Exception to throw at completion for a get() call.
     * 
     * get() -- Waits until cancelled or executor ends or exception one Task, then Throws??
     * 
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException we encountered a problem on our way to dispatching the Task
     */
    Exception m_pendingException = null;

    /**
     * The period between successive executions or the delay between the termination of one execution and
     * the commencement of the next. In nanoseconds. It depends on if scheduledAtFixedRate or scheduledWithDelay.
     * (m_scheduledWithDelay).
     */
    private final long m_periodInterval;

    /**
     * True if scheduledWithFixedDelay.
     */
    private final boolean m_scheduledWithDelay;

    /**
     * Time when this Task should be in execution next. Only Applicable if ScheduleAtFixedRate.
     */
    private long m_myNextExecutionTime;

    /**
     * Initialize to manage the supplied Runnable for dispatch on the Default Executor threadpool according to
     * the schedule parms.
     * 
     * @param scheduledWithDelay boolean indicating if the runnable was scheduled with delay or fixed interval.
     * @param inRunnable Runnable to manage.
     * @param inExecutor ScheduledExecutorImpl for scheduling.
     * @param initialDelay initialDelay the time to delay first execution.
     * @param period the period between successive executions.
     * @param unit the time unit of the initialDelay and period parameters.
     */
    public SchedulingRunnableFixedHelper(boolean scheduledWithDelay, Runnable inRunnable, ScheduledExecutorImpl inExecutor, long initialDelay, long period, TimeUnit unit) {
        this.m_scheduledExecutor = inExecutor;
        this.m_runnable = inRunnable;
        this.m_coordinationLatch = new CountDownLatch(1);
        this.m_schedFuture = null;

        this.m_pendingException = null;

        this.m_scheduledWithDelay = scheduledWithDelay;

        this.m_periodInterval = TimeUnit.NANOSECONDS.convert(period, unit);
        if (!this.m_scheduledWithDelay) {
            long currentTime = System.nanoTime();
            this.m_myNextExecutionTime = currentTime + TimeUnit.NANOSECONDS.convert(initialDelay, unit);
        }
    }

    /**
     * @param schedFuture
     */
    protected void setScheduledFuture(ScheduledFuture<?> schedFuture) {
        this.m_schedFuture = schedFuture;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("SchedulingRunnableFixedHelper: ");
        sb.append("m_runnable: " + (m_runnable != null ? m_runnable.toString() : "null"));
        sb.append(", m_isDone: " + m_isDone);
        sb.append(", m_cancelResult: " + m_cancelResult);
        sb.append(", m_coordinationLatch: " + (m_coordinationLatch != null ? m_coordinationLatch.toString() : "null"));
        sb.append(", m_pendingException: " + m_pendingException);
        sb.append(", m_periodInterval(ns): " + m_periodInterval);
        sb.append(", m_scheduledWithDelay: " + m_scheduledWithDelay);
        sb.append(", m_myNextExecutionTime(ns): " + m_myNextExecutionTime + " (" +
                  ((m_myNextExecutionTime != 0) ? new Date((m_myNextExecutionTime + 999999) / 1000000) : "NA") + ")");

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#cancel(boolean)
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {

        if (m_isDone == true) {
            return false;
        }
        m_isDone = true;

        try {
            m_cancelResult = m_schedFuture.cancel(mayInterruptIfRunning);

            m_pendingException = new CancellationException();
        } finally {
            // Let get() finish
            this.m_coordinationLatch.countDown();
        }

        return m_cancelResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#isDone()
     */
    @Override
    public boolean isDone() {
        // For scheduleAtFixedRate and scheduleWithFixedDelay, the Task is NOT done until cancel() is driven.
        // Regardless of the cancel() result, isDone needs to return true after a cancel() was issued.
        // OR, if we encountered an exception managing the Task.
        return (m_isDone || (m_pendingException != null));
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#get()
     */
    @Override
    public V get() throws InterruptedException, ExecutionException {
        this.m_coordinationLatch.await();

        if (m_pendingException != null) {
            if (m_pendingException instanceof ExecutionException) {
                throw (ExecutionException) m_pendingException;
            } else {
                throw (RuntimeException) m_pendingException;
            }
        }

        throw new CancellationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (this.m_coordinationLatch.await(timeout, unit) == false) {
            throw new TimeoutException();
        }

        if (m_pendingException != null) {
            if (m_pendingException instanceof ExecutionException) {
                throw (ExecutionException) m_pendingException;
            } else {
                throw (RuntimeException) m_pendingException;
            }
        }

        throw new CancellationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return this.m_schedFuture.getDelay(unit);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Delayed o) {
        return this.m_schedFuture.compareTo(o);
    }

    @Override
    public boolean equals(Object o) {
        return this.m_schedFuture.equals(o);
    }

    @Override
    public int hashCode() {
        return this.m_schedFuture.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#isCancelled()
     */
    @Override
    public boolean isCancelled() {
        return m_cancelResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        // Check for a pending exception.  A CancellationException could be set if a cancel() was driven while this Runnable
        // was being re-scheduled.
        if (this.m_pendingException != null) {
            return;
        }

        // NOTE: Already running in Default Executor thread.
        try {
            this.m_runnable.run();
        } catch (Exception e) {
            this.m_pendingException = new ExecutionException(e);
            this.m_coordinationLatch.countDown();
            return;
        }

        try {
            long scheduleTime = this.m_periodInterval;
            if (!this.m_scheduledWithDelay) {
                this.m_myNextExecutionTime = this.m_myNextExecutionTime + this.m_periodInterval;
                long currentTime = System.nanoTime();

                scheduleTime = (this.m_myNextExecutionTime > currentTime) ? (this.m_myNextExecutionTime - currentTime) : 0;
            }

            m_schedFuture = this.m_scheduledExecutor.schedule(this, scheduleTime, TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            this.m_pendingException = e;
            this.m_coordinationLatch.countDown();
        }
    }
}
