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
package com.ibm.ws.microprofile.faulttolerance20.impl;

import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.FallbackOccurred.NO_FALLBACK;
import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.FallbackOccurred.WITH_FALLBACK;

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
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.FallbackOccurred;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance20.state.CircuitBreakerState;
import com.ibm.ws.microprofile.faulttolerance20.state.FallbackState;
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
    private final FallbackState fallback;
    private final SyncBulkheadState bulkhead;
    private final MetricRecorder metricRecorder;

    public SyncExecutor(RetryPolicy retry, CircuitBreakerPolicy cbPolicy, TimeoutPolicy timeoutPolicy, FallbackPolicy fallbackPolicy, BulkheadPolicy bulkheadPolicy,
                        ScheduledExecutorService executorService, MetricRecorder metricRecorder) {
        retryPolicy = retry;
        circuitBreaker = FaultToleranceStateFactory.INSTANCE.createCircuitBreakerState(cbPolicy, metricRecorder);
        this.timeoutPolicy = timeoutPolicy;
        this.executorService = executorService;
        fallback = FaultToleranceStateFactory.INSTANCE.createFallbackState(fallbackPolicy);
        bulkhead = FaultToleranceStateFactory.INSTANCE.createSyncBulkheadState(bulkheadPolicy, metricRecorder);
        this.metricRecorder = metricRecorder;
    }

    /** {@inheritDoc} */
    @Override
    public R execute(Callable<R> callable, ExecutionContext context) {
        MethodResult<R> result;

        try {
            result = run(callable, context);
        } catch (Exception e) {
            // Handle unexpected exceptions
            // Exceptions from user code should have been caught and wrapped up in the MethodResult
            Tr.error(tc, "internal.error.CWMFT4998E", e);
            throw new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT4998E", e), e);
        }

        if (result.isFailure()) {
            if (result.getFailure() instanceof FaultToleranceException) {
                throw (FaultToleranceException) result.getFailure();
            } else {
                throw new ExecutionException(result.getFailure());
            }
        }

        return result.getResult();
    }

    private MethodResult<R> run(Callable<R> callable, ExecutionContext context) {
        SyncExecutionContextImpl executionContext = (SyncExecutionContextImpl) context;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Execution {0} Fault tolerance execution started for {1}", executionContext.getId(), context.getMethod());
        }

        Thread runningThread = Thread.currentThread();

        MethodResult<R> result = null;
        boolean done = false;
        RetryState retryContext = FaultToleranceStateFactory.INSTANCE.createRetryState(retryPolicy, metricRecorder);
        FallbackOccurred fallbackOccurred = NO_FALLBACK;

        retryContext.start();

        // Each iteration of this loop runs one retry attempt
        while (!done) {
            result = null;

            TimeoutState timeoutState = FaultToleranceStateFactory.INSTANCE.createTimeoutState(executorService, timeoutPolicy, metricRecorder);

            boolean circuitBreakerPermissionGiven = circuitBreaker.requestPermissionToExecute();
            if (!circuitBreakerPermissionGiven) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Execution {0} attempt Circuit Breaker open, not executing", executionContext.getId());
                }

                result = MethodResult.failure(new CircuitBreakerOpenException());
            }

            // If the circuit breaker gave permission to execute the result hasn't yet been set and we should run the execution
            if (result == null) {
                timeoutState.setTimeoutCallback(() -> runningThread.interrupt());
                timeoutState.start();

                // The call to the application code happens here
                result = bulkhead.run(callable);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Execution {0} attempt result: {1}", executionContext.getId(), result);
                }

                timeoutState.stop();
            }

            if (timeoutState.isTimedOut()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Execution {0} finished but has timed out, result changed to TimeoutException", executionContext.getId());
                }

                result = MethodResult.failure(new TimeoutException());
                Thread.interrupted(); // Clear interrupted flag if we were timed out
            }

            if (circuitBreakerPermissionGiven) {
                circuitBreaker.recordResult(result);
            }

            RetryResult retryResult = retryContext.recordResult(result);
            if (!retryResult.shouldRetry()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Execution {0} not retrying: {1}", executionContext.getId(), retryResult);
                }

                done = true;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Execution {0} retrying with delay: {1} {2}", executionContext.getId(), retryResult.getDelay(), retryResult.getDelayUnit());
                }

                try {
                    Thread.sleep(TimeUnit.MILLISECONDS.convert(retryResult.getDelay(), retryResult.getDelayUnit()));
                } catch (InterruptedException ex) {
                    done = true; // Stop retrying if we're interrupted
                }
            }
        }

        if (fallback.shouldApplyFallback(result)) {
            result = fallback.runFallback(result, executionContext);
            fallbackOccurred = WITH_FALLBACK;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Execution {0} final fault tolerance result: {1}", executionContext.getId(), result);
        }

        if (result.isFailure()) {
            metricRecorder.incrementInvocationFailedCount(fallbackOccurred);
        } else {
            metricRecorder.incrementInvocationSuccessCount(fallbackOccurred);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public FTExecutionContext newExecutionContext(String id, Method method, Object... parameters) {
        SyncExecutionContextImpl executionContext = new SyncExecutionContextImpl(id, method, parameters);
        return executionContext;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        // Nothing to close
    }

}
