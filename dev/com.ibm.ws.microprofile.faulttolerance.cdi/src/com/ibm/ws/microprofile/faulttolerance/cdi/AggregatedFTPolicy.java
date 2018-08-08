/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

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
 *
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

    /**
     * @param asynchronous
     */
    public void setAsynchronousResultWrapper(Class<?> asyncResultWrapper) {
        this.asyncResultWrapper = asyncResultWrapper;
    }

    /**
     * @param timeoutMillis
     */
    public void setTimeoutPolicy(TimeoutPolicy timeout) {
        this.timeout = timeout;
    }

    /**
     * @param retryPolicy
     */
    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    /**
     * @param circuitBreakerPolicy
     */
    public void setCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy) {
        this.circuitBreakerPolicy = circuitBreakerPolicy;
    }

    /**
     * @param bulkheadPolicy
     */
    public void setBulkheadPolicy(BulkheadPolicy bulkheadPolicy) {
        this.bulkheadPolicy = bulkheadPolicy;
    }

    /**
     * @return the method this policy is applied to
     */
    public Method getMethod() {
        return method;
    }

    /**
     * @return
     */
    public boolean isAsynchronous() {
        return asyncResultWrapper != null;
    }

    /**
     * @return
     */
    public BulkheadPolicy getBulkheadPolicy() {
        return bulkheadPolicy;
    }

    /**
     * @return
     */
    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    /**
     * @return the timeoutMillis
     */
    public TimeoutPolicy getTimeoutPolicy() {
        return timeout;
    }

    /**
     * @return the circuitBreakerPolicy
     */
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
     * @return
     */
    @SuppressWarnings("unchecked")
    public Executor<Object> getExecutor() {
        synchronized (this) {
            if (this.executor == null) {
                ExecutorBuilder<ExecutionContext, ?> builder = newBuilder();

                if (isAsynchronous()) {
                    this.executor = builder.buildAsync(asyncResultWrapper);
                } else {
                    this.executor = builder.build();
                }
            }
            return (Executor<Object>) this.executor;
        }
    }

    private ExecutorBuilder<ExecutionContext, ?> newBuilder() {
        ExecutorBuilder<ExecutionContext, ?> builder = FaultToleranceProvider.newExecutionBuilder();
        builder = updateBuilder(builder);
        return builder;
    }

    private <R> ExecutorBuilder<ExecutionContext, R> updateBuilder(ExecutorBuilder<ExecutionContext, R> builder) {
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
