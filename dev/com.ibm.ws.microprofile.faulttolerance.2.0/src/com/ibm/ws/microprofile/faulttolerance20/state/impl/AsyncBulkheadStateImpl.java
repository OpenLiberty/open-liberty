/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState;

/**
 * An implementation of {@link AsyncBulkheadState} which implements a {@link BulkheadPolicy} and reports metrics to a {@link MetricRecorder}
 */
public class AsyncBulkheadStateImpl implements AsyncBulkheadState {

    private final ExecutorService executorService;
    private final MetricRecorder metrics;

    /**
     * The queue to store tasks which have been submitted but there isn't space to run yet
     */
    private final BlockingQueue<ExecutionTask> queue;

    /**
     * The semaphore used to limit the number of concurrently running tasks
     */
    private final Semaphore runningSemaphore;

    public AsyncBulkheadStateImpl(ExecutorService executorService, BulkheadPolicy policy, MetricRecorder metricRecorder) {
        this.executorService = executorService;
        final int queueSize = policy.getQueueSize();
        queue = new LinkedBlockingQueue<>(queueSize);
        final int maxThreads = policy.getMaxThreads();
        runningSemaphore = new Semaphore(maxThreads);
        this.metrics = metricRecorder;
        metrics.setBulkheadConcurentExecutionCountSupplier(() -> maxThreads - runningSemaphore.availablePermits());
        metrics.setBulkheadQueuePopulationSupplier(queue::size);
    }

    /** {@inheritDoc} */
    @Override
    public ExecutionReference submit(AsyncBulkheadTask task, ExceptionHandler exceptionHandler) {
        ExecutionTask execution = new ExecutionTask(task, exceptionHandler);
        execution.enqueue();

        if (execution.wasAccepted()) {
            metrics.incrementBulkeadAcceptedCount();
            execution.startTime = System.nanoTime();
            tryRunNext();
        } else {
            metrics.incrementBulkheadRejectedCount();
        }

        return execution;
    }

    /**
     * Attempt to run any queued executions
     */
    private void tryRunNext() {
        while (runningSemaphore.tryAcquire()) {
            ExecutionTask execution = queue.poll();
            if (execution != null) {
                // Note: we've removed the execution from the queue so we're committed to running it
                // if we fail to do so for any reason, we need to call the exception handler to fail the execution
                try {
                    execution.submit();
                } catch (Throwable e) {
                    // Any exception here is unexpected
                    runningSemaphore.release();
                    execution.exceptionHandler.handle(e);
                }
            } else {
                runningSemaphore.release();
                break;
            }
        }
    }

    /**
     * This class incorporates everything we need to know about a AsyncBulkhedTask in order to run it, cancel it, complete it and report any unexpected exceptions.
     * <p>
     * It is returned from {@link AsyncBulkheadStateImpl#submit(AsyncBulkheadTask, ExceptionHandler)} as the {@link ExecutionReference} as well as being passed into
     * {@link AsyncBulkheadTask#run(BulkheadReservation)} as the {@link BulkheadReservation}.
     */
    private class ExecutionTask implements ExecutionReference, Runnable, BulkheadReservation {

        /**
         * Whether the task has been queued, rejected, submitted, cancelled etc.
         * <p>
         * This is used to ensure that semaphore permits are released at the right time and that we take the correct action if the user calls {@link #abort(boolean)}
         * <p>
         * Most state changes use {@link AtomicReference#compareAndSet(Object, Object)} to avoid race conditions (e.g. starting execution after abort has been called)
         */
        private final AtomicReference<Status> status;

        /**
         * The task to run
         */
        private final AsyncBulkheadTask task;

        /**
         * The {@code Future} which was returned when this {@code ExecutionTask} was submitted for execution
         * <p>
         * This will be {@code null} until {@link #submit()} has been called
         */
        private Future<?> future;

        /**
         * The exception handler, provided by the caller, to use to report any unexpected exceptions
         * <p>
         * E.g. this task is rejected when it's submitted for execution
         */
        private final ExceptionHandler exceptionHandler;

        /**
         * Depending on the current status, this is either the time we were queued, or the time we started running
         */
        private long startTime;

        public ExecutionTask(AsyncBulkheadTask task, ExceptionHandler exceptionHandler) {
            this.task = task;
            this.exceptionHandler = exceptionHandler;
            this.status = new AtomicReference<>(Status.NEW);
        }

        /**
         * Add this task to the back of the bulkhead queue
         */
        public void enqueue() {
            // Ensures enqueuing and setting status doesn't overlap with submission
            synchronized (this) {
                if (queue.offer(this)) {
                    status.set(Status.QUEUED);
                } else {
                    status.set(Status.REJECTED);
                }
            }
        }

        /**
         * Submit this task to the executorService
         * <p>
         * This should only be called after having removed this task from the queue
         */
        public void submit() {
            // Ensures we don't overlap with enqueuing or cancellation
            synchronized (this) {
                if (status.compareAndSet(Status.QUEUED, Status.SUBMITTED)) {
                    future = executorService.submit(this);
                }
            }
        }

        @Override
        public void run() {
            if (status.compareAndSet(Status.SUBMITTED, Status.RUNNING)) {
                try {
                    try {
                        long now = System.nanoTime();
                        metrics.reportQueueWaitTime(now - startTime);
                        startTime = now;
                        task.run(this);
                    } finally {
                        long now = System.nanoTime();
                        metrics.recordBulkheadExecutionTime(now - startTime);
                    }
                } catch (Throwable t) {
                    // Any exception here is unexpected
                    exceptionHandler.handle(t);
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void abort(boolean mayInterrupt) {
            // synchronized block prevents overlap with submission
            synchronized (this) {
                if (status.compareAndSet(Status.QUEUED, Status.CANCELLED)) {
                    queue.remove(this);
                } else if (status.compareAndSet(Status.SUBMITTED, Status.CANCELLED)) {
                    // Task never started running
                    future.cancel(mayInterrupt);
                    runningSemaphore.release();
                    tryRunNext();
                } else if (status.get().equals(Status.RUNNING)) {
                    // Task is running
                    // Running code is responsible for releasing semaphore permit
                    future.cancel(mayInterrupt);
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean wasAccepted() {
            return status.get() != Status.REJECTED;
        }

        /** {@inheritDoc} */
        @Override
        public void release() {
            // Note: the user's task will call this method
            if (status.compareAndSet(Status.RUNNING, Status.COMPLETE)) {
                runningSemaphore.release();
                tryRunNext();
            }
        }
    }

    /**
     * The different states that an {@link ExecutionTask} can be in
     */
    private enum Status {
        /** Task has just been created */
        NEW,
        /** Task has been added to the bulkhead queue */
        QUEUED,
        /** Task has been removed from the bulkhead queue and submitted to the executorService */
        SUBMITTED,
        /** Task has started running */
        RUNNING,
        /** {@link ExecutionTask#release()} has been called while the task was running */
        COMPLETE,
        /** Task was rejected because the bulkhead queue was full and will never run */
        REJECTED,

        /**
         * Task was accepted but then cancelled and never ran.
         * <p>
         * Note: if a task is running when {@link ExecutionTask#abort(boolean)} is called, the running thread is interrupted, but the task remains in the RUNNING state
         */
        CANCELLED
    }

}
