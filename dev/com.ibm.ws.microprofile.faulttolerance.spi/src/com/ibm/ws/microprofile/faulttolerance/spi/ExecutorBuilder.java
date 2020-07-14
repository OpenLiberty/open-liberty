/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.spi;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

/**
 * Configures and produces {@link Executor}s
 * <p>
 * Instances of this class should be obtained from {@link FaultToleranceProvider#newExecutionBuilder()}
 *
 * @param <R> the type of the result returned by {@code Executor}s created by this builder
 */
public interface ExecutorBuilder<R> {

    /**
     * Set the retry policy for executors created by this builder
     *
     * @param retry the retry policy
     * @return this builder
     */
    public ExecutorBuilder<R> setRetryPolicy(RetryPolicy retry);

    /**
     * Set the circuit breaker policy for executors created by this builder
     *
     * @param circuitBreaker the circuit breaker policy
     * @return this builder
     */
    public ExecutorBuilder<R> setCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreaker);

    /**
     * Set the bulkhead policy for executors created by this builder
     *
     * @param bulkhead the bulkhead policy
     * @return this builder
     */
    public ExecutorBuilder<R> setBulkheadPolicy(BulkheadPolicy bulkhead);

    /**
     * Set the fallback policy for executors created by this builder
     *
     * @param fallback the fallback policy
     * @return this builder
     */
    public ExecutorBuilder<R> setFallbackPolicy(FallbackPolicy fallback);

    /**
     * Set the timeout policy for executors created by this builder
     *
     * @param timeout the timeout policy
     * @return this builder
     */
    public ExecutorBuilder<R> setTimeoutPolicy(TimeoutPolicy timeout);

    /**
     * Set the metric recorder for executors created by this builder
     *
     * @param metricRecorder the metric recorder
     * @return this builder
     */
    public ExecutorBuilder<R> setMetricRecorder(MetricRecorder metricRecorder);

    /**
     * Create a new synchronous executor
     *
     * @return the new executor
     */
    public Executor<R> build();

    /**
     * Create a new asynchronous executor
     * <p>
     * An asynchronous executor returns its result inside a wrapper class. This is to allow an empty wrapper instance to be returned initially, which is later populated with the
     * result once the asynchronous method has completed.
     * <p>
     * The type returned by this method will be an Executor which returns an instance of {@code asyncResultWrapperType} which contains an instance of {@code R}
     * <p>
     * For example, if {@code asyncResultWrapperType == CompletionStage.class} and {@code R == String}, the actual type returned by this method will be
     * {@code Executor<CompletionStage<String>>}. Unfortunately, this isn't encapsulated in the signature itself because it's hard to express properly with Java generics.
     *
     * @param asyncResultWrapperType the wrapper type for executor, either {@link Future} or {@link CompletionStage}
     * @return the new executor
     */
    public <W> Executor<W> buildAsync(Class<?> asyncResultWrapperType);

}
