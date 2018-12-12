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
import java.util.concurrent.CompletionStage;
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
 * Executor for asynchronous methods which return a {@link CompletionStage}{@code<R>}
 *
 * @param <R> the type which the CompletionStage wraps
 */
public class AsyncCompletionStageExecutor<R> extends AsyncExecutor<CompletionStage<R>> {

    private static final TraceComponent tc = Tr.register(AsyncCompletionStageExecutor.class);

    public AsyncCompletionStageExecutor(RetryPolicy retry, CircuitBreakerPolicy cbPolicy, TimeoutPolicy timeoutPolicy, FallbackPolicy fallbackPolicy, BulkheadPolicy bulkheadPolicy,
                                        ScheduledExecutorService executorService, PolicyExecutorProvider policyExecutorProvider) {
        super(retry, cbPolicy, timeoutPolicy, fallbackPolicy, bulkheadPolicy, executorService, policyExecutorProvider);
    }

    @Override
    protected CompletionStage<R> createReturnWrapper(AsyncExecutionContextImpl<CompletionStage<R>> executionContext) {
        return new CompletableFuture<R>();
    }

    @Override
    protected void commitResult(AsyncExecutionContextImpl<CompletionStage<R>> executionContext, MethodResult<CompletionStage<R>> result) {
        CompletableFuture<R> returnWrapper = (CompletableFuture<R>) executionContext.getReturnWrapper();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Method {0} final fault tolerance result: {1}", executionContext.getMethod(), result);
        }

        if (result.isFailure()) {
            returnWrapper.completeExceptionally(result.getFailure());
        } else {
            result.getResult().thenAccept(returnWrapper::complete);
            result.getResult().exceptionally((ex) -> {
                returnWrapper.completeExceptionally(ex);
                return null;
            });
        }

    }

}
