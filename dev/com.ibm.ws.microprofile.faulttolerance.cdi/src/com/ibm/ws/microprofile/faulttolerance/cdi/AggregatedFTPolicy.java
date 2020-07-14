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
package com.ibm.ws.microprofile.faulttolerance.cdi;

import java.lang.reflect.Method;

import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorderProvider.AsyncType;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

/**
 * Holds all of the policies for a single method and creates an executor from them.
 * <p>
 * This class stores all of the fault tolerance policies ( {@link RetryPolicy}, {@link CircuitBreakerPolicy} etc.) for a method and is responsible for creating an executor for
 * them.
 * <p>
 * An {@link AggregatedFTPolicy} instance will only create a single executor. Subsequent calls to {@link #getExecutor()} will return the same executor, resulting in a 1-1 mapping
 * between objects of this class an {@link Executor} objects.
 */
public class AggregatedFTPolicy {

    private Method method = null;
    private Class<?> asyncResultWrapper = null;
    private RetryPolicy retryPolicy = null;
    private CircuitBreakerPolicy circuitBreakerPolicy = null;
    private BulkheadPolicy bulkheadPolicy = null;
    private TimeoutPolicy timeout;
    private FallbackPolicy fallbackPolicy;
    private Executor<?> executor;

    /**
     * @return the method this policy will be applied to
     */
    public void setMethod(Method method) {
        this.method = method;
    }

    public void setAsynchronousResultWrapper(Class<?> asyncResultWrapper) {
        this.asyncResultWrapper = asyncResultWrapper;
    }

    /**
     * @param timeoutMillis
     */
    public void setTimeoutPolicy(TimeoutPolicy timeout) {
        this.timeout = timeout;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public void setCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy) {
        this.circuitBreakerPolicy = circuitBreakerPolicy;
    }

    public void setBulkheadPolicy(BulkheadPolicy bulkheadPolicy) {
        this.bulkheadPolicy = bulkheadPolicy;
    }

    /**
     * @return the method this policy is applied to
     */
    public Method getMethod() {
        return method;
    }

    public boolean isAsynchronous() {
        return asyncResultWrapper != null;
    }

    public BulkheadPolicy getBulkheadPolicy() {
        return bulkheadPolicy;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    /**
     * @return timeoutMillis
     */
    public TimeoutPolicy getTimeoutPolicy() {
        return timeout;
    }

    public CircuitBreakerPolicy getCircuitBreakerPolicy() {
        return circuitBreakerPolicy;
    }

    public void setFallbackPolicy(FallbackPolicy fallbackPolicy) {
        this.fallbackPolicy = fallbackPolicy;
    }

    public FallbackPolicy getFallbackPolicy() {
        return this.fallbackPolicy;
    }

    /**
     * Get an executor for this set of policies, creating one if needed.
     * <p>
     * On the first call, this method will build an {@link Executor} for the policies set. Subsequent calls will return the same executor.
     *
     * @param rcInstance an instance of the request context controller
     * @return the {@code Executor} created previously if there is one, or a newly created {@code Executor}
     */
    @SuppressWarnings("unchecked")
    public Executor<Object> getExecutor() {
        synchronized (this) {
            if (this.executor == null) {
                ExecutorBuilder<?> builder = newBuilder();

                if (isAsynchronous()) {
                    this.executor = builder.buildAsync(asyncResultWrapper);
                } else {
                    this.executor = builder.build();
                }
            }
            return (Executor<Object>) this.executor;
        }
    }

    private ExecutorBuilder<?> newBuilder() {
        ExecutorBuilder<?> builder = FaultToleranceProvider.newExecutionBuilder();
        builder = updateBuilder(builder);
        return builder;
    }

    private <R> ExecutorBuilder<R> updateBuilder(ExecutorBuilder<R> builder) {
        TimeoutPolicy timeoutPolicy = getTimeoutPolicy();
        CircuitBreakerPolicy circuitBreakerPolicy = getCircuitBreakerPolicy();
        RetryPolicy retryPolicy = getRetryPolicy();
        FallbackPolicy fallbackPolicy = getFallbackPolicy();
        BulkheadPolicy bulkheadPolicy = getBulkheadPolicy();

        if (timeoutPolicy != null) {
            builder.setTimeoutPolicy(timeoutPolicy);
        }
        if (circuitBreakerPolicy != null) {
            builder.setCircuitBreakerPolicy(circuitBreakerPolicy);
        }
        if (retryPolicy != null) {
            builder.setRetryPolicy(retryPolicy);
        }
        if (fallbackPolicy != null) {
            builder.setFallbackPolicy(fallbackPolicy);
        }
        if (bulkheadPolicy != null) {
            builder.setBulkheadPolicy(bulkheadPolicy);
        }

        builder.setMetricRecorder(FaultToleranceCDIComponent.getMetricProvider().getMetricRecorder(method,
                                                                                                   retryPolicy,
                                                                                                   circuitBreakerPolicy,
                                                                                                   timeoutPolicy,
                                                                                                   bulkheadPolicy,
                                                                                                   fallbackPolicy,
                                                                                                   isAsynchronous() ? AsyncType.ASYNC : AsyncType.SYNC));

        return builder;
    }

    public void close() {
        synchronized (this) {
            if (this.executor != null) {
                this.executor.close();
                this.executor = null;
            }
        }
    }
}
