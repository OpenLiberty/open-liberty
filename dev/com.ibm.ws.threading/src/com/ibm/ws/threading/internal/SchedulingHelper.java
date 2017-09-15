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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base Wrapper for the Scheduled Runnable/Callable returned on overridden schedule methods. It's used to present a consistent state of the
 * submitted Callable/Runnable as it progresses through the underlying ScheduledThreadPoolExecutor and its redirection into
 * the Default ExecutorService.
 * 
 * @param <V>
 */
class SchedulingHelper<V> implements RunnableScheduledFuture<V> {
    /**
     * Wrapped Runnable/Callable related to this future instance
     */
    private final Callable<V> m_callable;

    /**
     * Done state of submitted Runnable/Callable
     */
    volatile boolean m_isDone = false;

    /**
     * Result from first cancel() call.
     */
    volatile boolean m_cancelResult = false;

    /**
     * JDK's ScheduleFuture...we will defer the getDelay and compareTo methods to this instance.
     */
    RunnableScheduledFuture<V> m_schedFuture;

    /**
     * Used to provide a consistent state.
     */
    CountDownLatch m_coordinationLatch;

    /**
     * Default Executor to push dispatches too
     */
    ExecutorService m_executor;

    /**
     * Future related to submitting to the Default Executor
     */
    Future<V> m_defaultFuture;

    /**
     * Exception to throw at completion for a get() call.
     * 
     * get() -- Waits until cancelled or executor ends or exception one Task, then Throws??
     * 
     * @throws CancellationException if the computation was cancelled
     * @throws RuntimeException we encountered a problem on our way to dispatching the Task
     */
    RuntimeException m_pendingException = null;

    /**
     * The queue from whence this SchedulingHelper instance was originally put.
     * This SchedulingHelper must remove itself from this queue once it has finished running.
     */
    private final BlockingQueue<Runnable> m_scheduledExecutorQueue;

    /**
     * Initialize the helper to manage a new Task.
     * 
     * @param inCallable Task to schedule.
     * @param inTask RunnableScheduledFuture associated with the scheduling of inCallable.
     * @param inExecutor default Executor to direct the dispatch of the inCallable to.
     */
    public SchedulingHelper(Callable<V> inCallable, RunnableScheduledFuture<V> inTask, ExecutorService inExecutor, BlockingQueue<Runnable> scheduledExecutorQueue) {
        this.m_callable = inCallable;
        this.m_coordinationLatch = new CountDownLatch(1);
        this.m_executor = inExecutor;
        this.m_schedFuture = inTask;
        this.m_scheduledExecutorQueue = scheduledExecutorQueue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return m_schedFuture.getDelay(unit);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Delayed o) {
        return m_schedFuture.compareTo(o);
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        return m_schedFuture.equals(o);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode() {
        return m_schedFuture.hashCode();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("SchedulingHelper<V>: ");
        sb.append("m_callable: " + (m_callable != null ? m_callable.toString() : "null"));
        sb.append(", m_isDone: " + m_isDone);
        sb.append(", m_cancelResult: " + m_cancelResult);
        sb.append(", m_schedFuture: " + (m_schedFuture != null ? m_schedFuture.toString() : "null"));
        sb.append(", m_coordinationLatch: " + (m_coordinationLatch != null ? m_coordinationLatch.toString() : "null"));
        sb.append(", m_executor: " + m_executor);
        sb.append(", m_defaultFuture: " + m_defaultFuture);
        sb.append(", m_pendingException: " + m_pendingException);

        return sb.toString();
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
     * @see java.util.concurrent.Future#cancel(boolean)
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (m_isDone == true) {
            return false;
        }
        m_isDone = true;

        if (m_pendingException != null) {
            return false;
        }

        m_cancelResult = m_schedFuture.cancel(false);

        if ((m_cancelResult != true) || (m_defaultFuture != null)) {
            // Push the cancel call to the Default Executor Future if it has progressed that far.
            try {
                this.m_coordinationLatch.await();
            } catch (InterruptedException ie) {
                // Try to proceed.
            }

            if (m_defaultFuture != null) {
                m_cancelResult = m_defaultFuture.cancel(mayInterruptIfRunning);
            }
        }

        if (m_cancelResult == true) {
            m_pendingException = new CancellationException();
            m_scheduledExecutorQueue.remove(this);
        }

        // Let get() finish
        this.m_coordinationLatch.countDown();

        return m_cancelResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#isDone()
     */
    @Override
    public boolean isDone() {
        boolean retVal = false;

        if ((m_isDone == true) || (m_pendingException != null)) {
            retVal = true;
        }

        // If we got as far as submitted the work to the default Executor, then ask it.
        if ((m_defaultFuture != null) &&
            m_defaultFuture.isDone()) {
            this.m_isDone = true;
            retVal = true;
        }

        return retVal;
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
            throw m_pendingException;
        }

        return m_defaultFuture.get();
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
            throw m_pendingException;
        }

        return m_defaultFuture.get(timeout, unit);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.RunnableScheduledFuture#isPeriodic()
     */
    @Override
    public boolean isPeriodic() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        try {
            if (this.isDone()) {
                return;
            }

            this.m_defaultFuture = this.m_executor.submit(this.m_callable);
            this.m_scheduledExecutorQueue.remove(this);
        } catch (Exception e) {
            this.m_pendingException = new RuntimeException(e);
            return;
        } finally {
            // Let blocked ScheduleFuture methods thru now.
            this.m_coordinationLatch.countDown();
        }
    }
}
