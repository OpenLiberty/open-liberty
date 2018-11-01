/*******************************************************************************
fa * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.impl;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutionException;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.FTExecutionContext;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance20.state.CircuitBreakerState;
import com.ibm.ws.microprofile.faulttolerance20.state.FaultToleranceStateFactory;
import com.ibm.ws.microprofile.faulttolerance20.state.RetryState;
import com.ibm.ws.microprofile.faulttolerance20.state.RetryState.RetryResult;
import com.ibm.ws.microprofile.faulttolerance20.state.SyncBulkheadState;
import com.ibm.ws.microprofile.faulttolerance20.state.TimeoutState;

/**
 * Executor for synchronous calls
 *
 * @param R the return type of the code being executed
 */
public class SyncExecutor<R> implements Executor<R> {

    private static final TraceComponent tc = Tr.register(SyncExecutor.class);

    private final RetryPolicy retryPolicy;
    private final CircuitBreakerState circuitBreaker;
    private final ScheduledExecutorService executorService;
    private final TimeoutPolicy timeoutPolicy;
    private final FallbackPolicy fallbackPolicy;
    private final SyncBulkheadState bulkhead;

    public SyncExecutor(RetryPolicy retry, CircuitBreakerPolicy cbPolicy, TimeoutPolicy timeoutPolicy, FallbackPolicy fallbackPolicy, BulkheadPolicy bulkheadPolicy,
                        ScheduledExecutorService executorService) {
        retryPolicy = retry;
        circuitBreaker = FaultToleranceStateFactory.INSTANCE.createCircuitBreakerState(cbPolicy);
        this.timeoutPolicy = timeoutPolicy;
        this.executorService = executorService;
        this.fallbackPolicy = fallbackPolicy;
        bulkhead = FaultToleranceStateFactory.INSTANCE.createSyncBulkheadState(bulkheadPolicy);
    }

    /** {@inheritDoc} */
    @Override
    public R execute(Callable<R> callable, ExecutionContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Fault tolerance execution started for {0}", context.getMethod());
        }

        SyncExecutionContextImpl executionContext = (SyncExecutionContextImpl) context;
        Thread runningThread = Thread.currentThread();

        MethodResult<R> result = null;
        boolean done = false;
        RetryState retryContext = FaultToleranceStateFactory.INSTANCE.createRetryState(retryPolicy);

        retryContext.start();

        // Each iteration of this loop runs one retry attempt
        while (!done) {
            result = null;

            TimeoutState timeoutState = FaultToleranceStateFactory.INSTANCE.createTimeoutState(executorService, timeoutPolicy);

            if (!circuitBreaker.requestPermissionToExecute()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Method {0} attempt Circuit Breaker open, not executing", context.getMethod());
                }

                result = MethodResult.failure(new CircuitBreakerOpenException());
            }

            // If the circuit breaker gave permission to execute the result hasn't yet been set and we should run the execution
            if (result == null) {
                timeoutState.start(() -> runningThread.interrupt());

                result = bulkhead.run(callable);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Method {0} attempt result: {1}", context.getMethod(), result);
                }

                timeoutState.stop();
            }

            if (timeoutState.isTimedOut()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Method {0} finished but has timed out, result changed to TimeoutException", context.getMethod());
                }

                result = MethodResult.failure(new TimeoutException());
                Thread.interrupted(); // Clear interrupted flag if we were timed out
            }

            circuitBreaker.recordResult(result);

            RetryResult retryResult = retryContext.recordResult(result);
            if (!retryResult.shouldRetry()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Method {0} not retrying", context.getMethod());
                }

                done = true;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Method {0} retrying with delay: {1} {2}", context.getMethod(), retryResult.getDelay(), retryResult.getDelayUnit());
                }

                try {
                    Thread.sleep(TimeUnit.MILLISECONDS.convert(retryResult.getDelay(), retryResult.getDelayUnit()));
                } catch (InterruptedException ex) {
                    done = true; // Stop retrying if we're interrupted
                }
            }
        }

        if (fallbackPolicy != null && result.isFailure()) {
            result = runFallback(result, executionContext);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Method {0} final fault tolerance result: {1}", context.getMethod(), result);
        }

        if (result.getFailure() != null) {
            if (result.getFailure() instanceof FaultToleranceException) {
                throw (FaultToleranceException) result.getFailure();
            } else {
                throw new ExecutionException(result.getFailure());
            }
        }

        return result.getResult();
    }

    @SuppressWarnings("unchecked")
    private MethodResult<R> runFallback(MethodResult<R> result, SyncExecutionContextImpl executionContext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Method {0} calling fallback", executionContext.getMethod());
        }

        executionContext.setFailure(result.getFailure());
        try {
            result = MethodResult.success((R) fallbackPolicy.getFallbackFunction().execute(executionContext));
        } catch (Throwable ex) {
            result = MethodResult.failure(ex);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Method {0} fallback result: {1}", executionContext.getMethod(), result);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public FTExecutionContext newExecutionContext(String id, Method method, Object... parameters) {
        SyncExecutionContextImpl executionContext = new SyncExecutionContextImpl(method, parameters);
        return executionContext;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        // Nothing to close
    }

}
