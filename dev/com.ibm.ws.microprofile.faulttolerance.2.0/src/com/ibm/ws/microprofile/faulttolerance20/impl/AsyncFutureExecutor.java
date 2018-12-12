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
package com.ibm.ws.microprofile.faulttolerance20.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.threading.PolicyExecutorProvider;

/**
 * Executor for Asynchronous calls which return a {@link Future}
 *
 * @param R the type wrapped by the returned Future
 */
public class AsyncFutureExecutor<R> extends AsyncExecutor<Future<R>> {

    private static final TraceComponent tc = Tr.register(AsyncFutureExecutor.class);

    public AsyncFutureExecutor(RetryPolicy retry, CircuitBreakerPolicy cbPolicy, TimeoutPolicy timeoutPolicy, FallbackPolicy fallbackPolicy, BulkheadPolicy bulkheadPolicy,
                               ScheduledExecutorService executorService, PolicyExecutorProvider policyExecutorProvider) {
        super(retry, cbPolicy, timeoutPolicy, fallbackPolicy, bulkheadPolicy, executorService, policyExecutorProvider);
    }

    @Override
    protected void commitResult(AsyncExecutionContextImpl<Future<R>> executionContext, MethodResult<Future<R>> result) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Method {0} final fault tolerance result: {1}", executionContext.getMethod(), result);
        }

        FutureShell<R> returnWrapper = (FutureShell<R>) executionContext.getReturnWrapper();
        if (result.isFailure()) {
            CompletableFuture<R> failureResult = new CompletableFuture<R>();
            failureResult.completeExceptionally(result.getFailure());
            returnWrapper.setDelegate(failureResult);
        } else {
            returnWrapper.setDelegate(result.getResult());
        }
    }

    @Override
    protected Future<R> createReturnWrapper(AsyncExecutionContextImpl<Future<R>> executionContext) {
        FutureShell<R> returnWrapper = new FutureShell<>();
        returnWrapper.setCancellationCallback(executionContext::cancel);
        return returnWrapper;
    }

}
