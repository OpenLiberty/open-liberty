/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import com.ibm.ws.microprofile.faulttolerance.impl.async.AsyncOuterExecutorImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.sync.SynchronousExecutorImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.threading.PolicyExecutorProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

public class ExecutorBuilderImpl<T, R> implements ExecutorBuilder<T, R> {

    private CircuitBreakerPolicy circuitBreakerPolicy = null;
    private RetryPolicy retryPolicy = null;
    private BulkheadPolicy bulkheadPolicy = null;
    private FallbackPolicy fallbackPolicy = null;
    private TimeoutPolicy timeoutPolicy = null;
    private final WSContextService contextService;
    private final PolicyExecutorProvider policyExecutorProvider;
    private final ScheduledExecutorService scheduledExecutorService;

    public ExecutorBuilderImpl(WSContextService contextService, PolicyExecutorProvider policyExecutorProvider, ScheduledExecutorService scheduledExecutorService) {
        this.contextService = contextService;
        this.policyExecutorProvider = policyExecutorProvider;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorBuilder<T, R> setRetryPolicy(RetryPolicy retry) {
        this.retryPolicy = retry;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorBuilder<T, R> setCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreaker) {
        this.circuitBreakerPolicy = circuitBreaker;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorBuilder<T, R> setBulkheadPolicy(BulkheadPolicy bulkhead) {
        this.bulkheadPolicy = bulkhead;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorBuilder<T, R> setFallbackPolicy(FallbackPolicy fallback) {
        this.fallbackPolicy = fallback;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorBuilder<T, R> setTimeoutPolicy(TimeoutPolicy timeout) {
        this.timeoutPolicy = timeout;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Executor<R> build() {
        Executor<R> executor = new SynchronousExecutorImpl<R>(this.retryPolicy, this.circuitBreakerPolicy, this.timeoutPolicy, this.bulkheadPolicy, this.fallbackPolicy, this.scheduledExecutorService);

        return executor;
    }

    /** {@inheritDoc} */
    @Override
    public Executor<Future<R>> buildAsync() {
        Executor<Future<R>> executor = new AsyncOuterExecutorImpl<R>(this.retryPolicy, this.circuitBreakerPolicy, this.timeoutPolicy, this.bulkheadPolicy, this.fallbackPolicy, this.contextService, this.policyExecutorProvider, this.scheduledExecutorService);

        return executor;
    }

}
