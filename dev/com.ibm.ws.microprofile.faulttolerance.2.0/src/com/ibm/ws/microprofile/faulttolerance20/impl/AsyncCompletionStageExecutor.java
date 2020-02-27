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
package com.ibm.ws.microprofile.faulttolerance20.impl;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance.spi.AsyncRequestContextController;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState.BulkheadReservation;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Executor for asynchronous methods which return a {@link CompletionStage}{@code<R>}
 *
 * @param <R> the type which the CompletionStage wraps
 */
public class AsyncCompletionStageExecutor<R> extends AsyncExecutor<CompletionStage<R>> {

    private static final TraceComponent tc = Tr.register(AsyncCompletionStageExecutor.class);

    private final ScheduledExecutorService executorService;

    public AsyncCompletionStageExecutor(RetryPolicy retry, CircuitBreakerPolicy cbPolicy, TimeoutPolicy timeoutPolicy, FallbackPolicy fallbackPolicy, BulkheadPolicy bulkheadPolicy,
                                        ScheduledExecutorService executorService, WSContextService contextService, MetricRecorder metricRecorder,
                                        AsyncRequestContextController asyncRequestContext) {
        super(retry, cbPolicy, timeoutPolicy, fallbackPolicy, bulkheadPolicy, executorService, contextService, metricRecorder, asyncRequestContext);
        this.executorService = executorService;
    }

    @Override
    protected CompletionStage<R> createEmptyResultWrapper(AsyncExecutionContextImpl<CompletionStage<R>> executionContext) {
        return new CompletableFuture<R>();
    }

    @Override
    protected void setResult(AsyncExecutionContextImpl<CompletionStage<R>> executionContext, MethodResult<CompletionStage<R>> result) {

        if (System.getSecurityManager() != null && Thread.currentThread() instanceof ForkJoinWorkerThread) {
            // Workaround in case the user completes the result on a ForkJoin thread
            // Note: user should _never_ be using ForkJoin threads but we'll try to handle this gracefully
            // If a security manager is present, we won't be able to apply thread context so submit a new task to set the result
            executorService.submit(() -> doSetResult(executionContext, result));
        } else {
            doSetResult(executionContext, result);
        }
    }

    @FFDCIgnore(IllegalStateException.class)
    private void doSetResult(AsyncExecutionContextImpl<CompletionStage<R>> executionContext, MethodResult<CompletionStage<R>> result) {
        CompletableFuture<R> resultWrapper = (CompletableFuture<R>) executionContext.getResultWrapper();

        // Completing the return wrapper may cause user code to run, so set the thread context first as we may be on an async thread
        ThreadContextDescriptor threadContext = executionContext.getThreadContextDescriptor();
        try {
            ArrayList<ThreadContext> contexts = null;
            try {
                contexts = threadContext.taskStarting();
            } catch (IllegalStateException e) {
                // Component for captured context is no longer running
                result = MethodResult.internalFailure(createAppStoppedException(e, executionContext));
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Execution {0} final fault tolerance result: {1}", executionContext.getId(), result);
            }

            try {
                if (result.isFailure()) {
                    resultWrapper.completeExceptionally(result.getFailure());
                } else {
                    result.getResult().thenAccept(resultWrapper::complete);
                    result.getResult().exceptionally((ex) -> {
                        resultWrapper.completeExceptionally(ex);
                        return null;
                    });
                }
            } finally {
                if (contexts != null) {
                    threadContext.taskStopping(contexts);
                }
            }
        } catch (Throwable t) {
            Tr.error(tc, "internal.error.CWMFT4998E", t);
            resultWrapper.completeExceptionally(new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT4998E", t), t));
        }
    }

    @Override
    protected void processMethodResult(AsyncAttemptContextImpl<CompletionStage<R>> attemptContext, MethodResult<CompletionStage<R>> result, BulkheadReservation reservation) {
        // processMethodResult is called after the user's method returns.
        // However, for methods which return CompletionStage, if an exception was not thrown, we want to delay the rest of the fault tolerance processing
        // until the returned CompletionStage completes, and then if it completes exceptionally, we want to use that exception as the result
        if (result.isFailure()) {
            super.processMethodResult(attemptContext, result, reservation);
        } else {
            result.getResult().thenRun(() -> super.processMethodResult(attemptContext, result, reservation));
            result.getResult().exceptionally((t) -> {
                super.processMethodResult(attemptContext, MethodResult.failure(t), reservation);
                return null;
            });
        }
    }

}
