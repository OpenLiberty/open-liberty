/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.impl;

import java.util.concurrent.ScheduledExecutorService;

import com.ibm.ws.microprofile.faulttolerance.spi.AsyncRequestContextController;
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

/**
 *
 * @param <R>
 */
public abstract class AbstractExecutorBuilderImpl<R> implements ExecutorBuilder<R> {

    protected CircuitBreakerPolicy circuitBreakerPolicy = null;

    @Override
    public abstract <W> Executor<W> buildAsync(Class<?> asyncResultWrapperType);

    @Override
    public abstract Executor<R> build();

    protected RetryPolicy retryPolicy = null;
    protected BulkheadPolicy bulkheadPolicy = null;
    protected FallbackPolicy fallbackPolicy = null;
    protected TimeoutPolicy timeoutPolicy = null;
    protected MetricRecorder metricRecorder = DummyMetricRecorder.get();
    protected final WSContextService contextService;
    protected final PolicyExecutorProvider policyExecutorProvider;
    protected final ScheduledExecutorService scheduledExecutorService;
    protected AsyncRequestContextController asyncRequestContext = null;

    public AbstractExecutorBuilderImpl(WSContextService contextService, PolicyExecutorProvider policyExecutorProvider, ScheduledExecutorService scheduledExecutorService) {
        this.contextService = contextService;
        this.policyExecutorProvider = policyExecutorProvider;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorBuilder<R> setRetryPolicy(RetryPolicy retry) {
        this.retryPolicy = retry;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorBuilder<R> setCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreaker) {
        this.circuitBreakerPolicy = circuitBreaker;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorBuilder<R> setBulkheadPolicy(BulkheadPolicy bulkhead) {
        this.bulkheadPolicy = bulkhead;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorBuilder<R> setFallbackPolicy(FallbackPolicy fallback) {
        this.fallbackPolicy = fallback;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorBuilder<R> setTimeoutPolicy(TimeoutPolicy timeout) {
        this.timeoutPolicy = timeout;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorBuilder<R> setMetricRecorder(MetricRecorder metricRecorder) {
        this.metricRecorder = metricRecorder;
        return this;
    }

    @Override
    public void setRequestContextController(AsyncRequestContextController asyncRequestContext) {
        this.asyncRequestContext = asyncRequestContext;
    }

}