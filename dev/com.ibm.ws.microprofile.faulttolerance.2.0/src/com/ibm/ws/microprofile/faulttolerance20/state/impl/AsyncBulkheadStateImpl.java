/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;

import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState;

/**
 * Implements the asynchronous bulkhead
 * <p>
 * The implementation is made of several parts:
 * <ul>
 * <li>an execution semaphore, used to limit the number of tasks scheduled to execute</li>
 * <li>a queuing semaphore, used to limit the number of tasks allowed to queue for execution</li>
 * <li>a concurrent linked queue, to store the executions waiting to be scheduled</li>
 * </ul>
 * <p>
 * This class ensures that the following rules are followed:
 * <ul>
 * <li>Before a execution can be scheduled, it must acquire a permit from the execution semaphore</li>
 * <li>Before an execution can be queued, it must acquire a permit from the queuing semaphore</li>
 * <li>When an execution is removed from the queue, it must release its permit from the queuing semaphore</li>
 * <li>When an execution returns after being executed, it must either release its permit from the execution semaphore, or pass the responsibility for that permit to a newly
 * scheduled execution</li>
 * </li>
 */
public class AsyncBulkheadStateImpl implements AsyncBulkheadState {

    private final ScheduledExecutorService executorService;
    private final ConcurrentLinkedQueue<ExecutionReferenceImpl> queue;
    private final Semaphore executionSemaphore;
    private final Semaphore queuingSemaphore;

    public AsyncBulkheadStateImpl(ScheduledExecutorService executorService, BulkheadPolicy policy) {
        this.executorService = executorService;
        this.executionSemaphore = new Semaphore(policy.getMaxThreads());
        this.queuingSemaphore = new Semaphore(policy.getQueueSize());
        this.queue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public ExecutionReference submit(Runnable runnable) {
        ExecutionReferenceImpl execution = new ExecutionReferenceImpl(runnable);

        if (executionSemaphore.tryAcquire()) {
            enqueueExecution(execution);
            execution.accepted = true;
        } else if (queuingSemaphore.tryAcquire()) {
            queue.offer(execution);
            execution.accepted = true;
        }

        return execution;
    }

    private void enqueueExecution(ExecutionReferenceImpl execution) {
        Future<?> future = executorService.submit(() -> {
            runRunnable(execution.runnable);
        });
        execution.future = future;
    }

    private void runRunnable(Runnable runnable) {
        try {
            runnable.run();
        } finally {
            enqueueNext();
        }
    }

    private void enqueueNext() {
        // Synchronization to ensure that execution cannot be cancelled while it's in the process of being enqueued
        synchronized (this) {
            ExecutionReferenceImpl execution = queue.poll();
            if (execution != null) {
                // If there are queued executions, we schedule one and release its queuing permit
                // Our execution permit passes to that execution, so we do not release it
                enqueueExecution(execution);
                queuingSemaphore.release();
            } else {
                // No queued executions, release our execution permit
                executionSemaphore.release();
            }
        }
    }

    private class ExecutionReferenceImpl implements ExecutionReference {

        public ExecutionReferenceImpl(Runnable runnable) {
            this.runnable = runnable;
        }

        private final Runnable runnable;
        private Future<?> future;
        private boolean accepted = false;

        @Override
        public void abort() {
            // Synchronization to avoid canceling an execution while it's in the process
            // of being moved from queued to running
            synchronized (AsyncBulkheadStateImpl.this) {
                if (future == null) {
                    if (queue.remove(this)) {
                        queuingSemaphore.release();
                    }
                } else {
                    future.cancel(true);
                }
            }
        }

        @Override
        public boolean wasAccepted() {
            return accepted;
        }
    }

}
