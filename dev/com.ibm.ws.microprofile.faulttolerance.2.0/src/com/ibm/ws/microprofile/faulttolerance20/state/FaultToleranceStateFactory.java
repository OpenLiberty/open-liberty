/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state;

import java.util.concurrent.ScheduledExecutorService;

import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance20.state.impl.AsyncBulkheadStateImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.impl.AsyncBulkheadStateNullImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.impl.CircuitBreakerStateImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.impl.CircuitBreakerStateNullImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.impl.FallbackStateImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.impl.FallbackStateNullImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.impl.RetryStateImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.impl.RetryStateNullImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.impl.SyncBulkheadStateImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.impl.SyncBulkheadStateNullImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.impl.TimeoutStateImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.impl.TimeoutStateNullImpl;

/**
 * Factory for creating Fault Tolerance State objects, which implement one of the fault tolerance policies.
 * <p>
 * Note that all of the methods may be called with a {@code null} policy which will return a state object which does not implement the fault tolerance policy.
 */
public class FaultToleranceStateFactory {

    /**
     * The singleton instance
     */
    public static final FaultToleranceStateFactory INSTANCE = new FaultToleranceStateFactory();

    // Singleton: no public constructor
    private FaultToleranceStateFactory() {
    }

    /**
     * Create an object implementing Retry
     *
     * @param policy the RetryPolicy, may be {@code null}
     * @return a new RetryState
     */
    public RetryState createRetryState(RetryPolicy policy, MetricRecorder metricRecorder) {
        if (policy == null) {
            return new RetryStateNullImpl();
        } else {
            return new RetryStateImpl(policy, metricRecorder);
        }
    }

    /**
     * Create an object implementing a synchronous Bulkhead
     *
     * @param policy the BulkheadPolicy, may be {@code null}
     * @return a new SyncBulkheadState
     */
    public SyncBulkheadState createSyncBulkheadState(BulkheadPolicy policy, MetricRecorder metricRecorder) {
        if (policy == null) {
            return new SyncBulkheadStateNullImpl();
        } else {
            return new SyncBulkheadStateImpl(policy, metricRecorder);
        }
    }

    /**
     * Create an object implementing Timeout
     *
     * @param executorService the executor to use to schedule the timeout callback
     * @param policy          the TimeoutPolicy, may be {@code null}
     * @return a new TimeoutState
     */
    public TimeoutState createTimeoutState(ScheduledExecutorService executorService, TimeoutPolicy policy, MetricRecorder metricRecorder) {
        if (policy == null) {
            return new TimeoutStateNullImpl();
        } else {
            return new TimeoutStateImpl(executorService, policy, metricRecorder);
        }
    }

    /**
     * Create an object implementing CircuitBreaker
     *
     * @param policy the CircuitBreakerPolicy, may be {@code null}
     * @return a new CircuitBreakerState
     */
    public CircuitBreakerState createCircuitBreakerState(CircuitBreakerPolicy policy, MetricRecorder metricRecorder) {
        if (policy == null) {
            return new CircuitBreakerStateNullImpl();
        } else {
            return new CircuitBreakerStateImpl(policy, metricRecorder);
        }
    }

    /**
     * Create an object implementing an asynchronous Bulkhead
     * <p>
     * If {@code null} is passed for the policy, the returned object will still run submitted tasks asynchronously, but will not apply any bulkhead logic.
     *
     * @param executorProvider the policy executor provider
     * @param executorService  the executor to use to asynchronously run tasks
     * @param policy           the BulkheadPolicy, may be {@code null}
     * @return a new AsyncBulkheadState
     */
    public AsyncBulkheadState createAsyncBulkheadState(ScheduledExecutorService executorService, BulkheadPolicy policy, MetricRecorder metricRecorder) {
        if (policy == null) {
            return new AsyncBulkheadStateNullImpl(executorService);
        } else {
            return new AsyncBulkheadStateImpl(executorService, policy, metricRecorder);
        }
    }

    /**
     * Create an object implementing Fallback
     *
     * @param policy the FallbackPolicy, may be {@code null}
     * @return a new FallbackState
     */
    public FallbackState createFallbackState(FallbackPolicy policy) {
        if (policy == null) {
            return new FallbackStateNullImpl();
        } else {
            return new FallbackStateImpl(policy);
        }
    }

}
