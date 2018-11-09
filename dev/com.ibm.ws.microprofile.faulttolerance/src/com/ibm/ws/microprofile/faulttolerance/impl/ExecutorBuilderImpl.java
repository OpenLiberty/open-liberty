/*******************************************************************************
 * Copyright (c) 2017,2018 IBM Corporation and others.
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
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance.utils.DummyMetricRecorder;
import com.ibm.ws.threading.PolicyExecutorProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

public class ExecutorBuilderImpl<T, R> implements ExecutorBuilder<T, R> {

    protected CircuitBreakerPolicy circuitBreakerPolicy = null;
    protected RetryPolicy retryPolicy = null;
    protected BulkheadPolicy bulkheadPolicy = null;
    protected FallbackPolicy fallbackPolicy = null;
    protected TimeoutPolicy timeoutPolicy = null;
    protected MetricRecorder metricRecorder = DummyMetricRecorder.get();
    private final WSContextService contextService;
    protected final PolicyExecutorProvider policyExecutorProvider;
    protected final ScheduledExecutorService scheduledExecutorService;

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
    public ExecutorBuilder<T, R> setMetricRecorder(MetricRecorder metricRecorder) {
        this.metricRecorder = metricRecorder;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Executor<R> build() {
        Executor<R> executor = new SynchronousExecutorImpl<R>(this.retryPolicy, this.circuitBreakerPolicy, this.timeoutPolicy, this.bulkheadPolicy, this.fallbackPolicy, this.scheduledExecutorService, this.metricRecorder);

        return executor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <W> Executor<W> buildAsync(Class<?> asyncResultWrapperType) {
        if (asyncResultWrapperType == Future.class) {
            Executor<Future<R>> executor = new AsyncOuterExecutorImpl<R>(this.retryPolicy, this.circuitBreakerPolicy, this.timeoutPolicy, this.bulkheadPolicy, this.fallbackPolicy, this.contextService, this.policyExecutorProvider, this.scheduledExecutorService, this.metricRecorder);
            return (Executor<W>) executor;
        } else {
            throw new IllegalArgumentException("Invalid return type for async execution: " + asyncResultWrapperType);
        }
    }

}
