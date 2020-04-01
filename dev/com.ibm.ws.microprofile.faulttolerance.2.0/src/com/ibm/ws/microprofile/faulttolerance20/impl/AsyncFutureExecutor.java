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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.spi.AsyncRequestContextController;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Executor for Asynchronous calls which return a {@link Future}
 *
 * @param R the type wrapped by the returned Future
 */
public class AsyncFutureExecutor<R> extends AsyncExecutor<Future<R>> {

    private static final TraceComponent tc = Tr.register(AsyncFutureExecutor.class);

    public AsyncFutureExecutor(RetryPolicy retry, CircuitBreakerPolicy cbPolicy, TimeoutPolicy timeoutPolicy, FallbackPolicy fallbackPolicy, BulkheadPolicy bulkheadPolicy,
                               ScheduledExecutorService executorService, WSContextService contextService, MetricRecorder metricRecorder,
                               AsyncRequestContextController asyncRequestContext) {
        super(retry, cbPolicy, timeoutPolicy, fallbackPolicy, bulkheadPolicy, executorService, contextService, metricRecorder, asyncRequestContext);
    }

    @Override
    protected void setResult(AsyncExecutionContextImpl<Future<R>> executionContext, MethodResult<Future<R>> result) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Method {0} final fault tolerance result: {1}", executionContext.getMethod(), result);
        }

        FutureShell<R> resultWrapper = (FutureShell<R>) executionContext.getResultWrapper();
        if (result.isFailure()) {
            CompletableFuture<R> failureResult = new CompletableFuture<R>();
            failureResult.completeExceptionally(result.getFailure());
            resultWrapper.setDelegate(failureResult);
        } else {
            resultWrapper.setDelegate(result.getResult());
        }
    }

    @Override
    protected Future<R> createEmptyResultWrapper(AsyncExecutionContextImpl<Future<R>> executionContext) {
        FutureShell<R> resultWrapper = new FutureShell<>();
        resultWrapper.setCancellationCallback(executionContext::cancel);
        return resultWrapper;
    }

}
