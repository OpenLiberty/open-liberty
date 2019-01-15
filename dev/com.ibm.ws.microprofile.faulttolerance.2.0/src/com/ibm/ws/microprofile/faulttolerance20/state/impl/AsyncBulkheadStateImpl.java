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

import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.PolicyExecutorProvider;
import com.ibm.ws.threading.PolicyTaskCallback;
import com.ibm.ws.threading.PolicyTaskFuture;

/**
 * Implements the asynchronous bulkhead using a {@link PolicyExecutor}
 */
public class AsyncBulkheadStateImpl implements AsyncBulkheadState {

    private final PolicyExecutor executorService;
    private final MetricRecorder metrics;

    public AsyncBulkheadStateImpl(PolicyExecutorProvider executorProvider, BulkheadPolicy policy, MetricRecorder metricRecorder) {
        this.executorService = executorProvider.create("Fault Tolerance-" + UUID.randomUUID());
        final int queueSize = policy.getQueueSize();
        executorService.maxConcurrency(policy.getMaxThreads());
        executorService.maxQueueSize(queueSize);
        this.metrics = metricRecorder;
        metrics.setBulkheadConcurentExecutionCountSupplier(executorService::getRunningTaskCount);
        metrics.setBulkheadQueuePopulationSupplier(() -> queueSize - executorService.queueCapacityRemaining());
        // TODO: add recording of queue and execution times
    }

    @Override
    @FFDCIgnore(RejectedExecutionException.class)
    public ExecutionReference submit(Runnable runnable, ExceptionHandler exceptionHandler) {
        ExecutionReferenceImpl execution = new ExecutionReferenceImpl();
        try {
            execution.future = executorService.submit(runnable, new TaskMonitor(exceptionHandler));
            execution.accepted = true;
            metrics.incrementBulkeadAcceptedCount();
        } catch (RejectedExecutionException e) {
            execution.accepted = false;
            metrics.incrementBulkheadRejectedCount();
        }
        return execution;
    }

    private class ExecutionReferenceImpl implements ExecutionReference {

        private Future<?> future;
        private boolean accepted = false;

        @Override
        public void abort(boolean mayInterrupt) {
            if (future != null) {
                future.cancel(mayInterrupt);
            }
        }

        @Override
        public boolean wasAccepted() {
            return accepted;
        }
    }

    private class TaskMonitor extends PolicyTaskCallback {

        private final ExceptionHandler exceptionHandler;

        public TaskMonitor(ExceptionHandler exceptionHandler) {
            super();
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public void onEnd(Object task, PolicyTaskFuture<?> future, Object startObj, boolean aborted, int pending, Throwable failure) {
            if (failure != null) {
                exceptionHandler.handle(failure);
            }
        }

    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

}
