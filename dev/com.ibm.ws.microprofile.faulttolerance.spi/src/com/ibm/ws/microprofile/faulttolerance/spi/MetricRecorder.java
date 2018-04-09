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
package com.ibm.ws.microprofile.faulttolerance.spi;

import java.util.function.LongSupplier;

import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 * Interface to record metrics for fault tolerance executions
 * <p>
 * A metric recorder is scoped to cover all executions of one particular method.
 */
public interface MetricRecorder {

    /**
     * Called when a method returns
     * <p>
     * This should be called once per invocation, just before the result is returned to the caller
     */
    public void incrementInvocationCount();

    /**
     * Called when a method fails
     * <p>
     * This should be called once per invocation, just before the result is returned to the caller
     */
    public void incrementInvocationFailedCount();

    /**
     * Called when a method with {@code @Retry} returns successfully without retrying
     */
    public void incrementRetryCallsSuccessImmediateCount();

    /**
     * Called when a method with {@code @Retry} returns successfully after one or more retries
     */
    public void incrementRetryCallsSuccessRetriesCount();

    /**
     * Called when a method with {@code @Retry} fails despite retrying
     */
    public void incrementRetryCallsFailureCount();

    /**
     * Called when a method with {@code @Retry} retries
     */
    public void incrementRetriesCount();

    /**
     * Called when a method with {@code Timeout} completes (either successfully or unsuccessfully)
     *
     * @param executionNanos the time taken for the method to execute
     */
    public void recordTimeoutExecutionTime(long executionNanos);

    /**
     * Called when a method with {@code Timeout} completes after timing out
     */
    public void incrementTimeoutTrueCount();

    /**
     * Called when a method with {@code Timeout} completes without timing out
     */
    public void incrementTimeoutFalseCount();

    /**
     * Called when a method with {@code CircuitBreaker} fails because the circuit is open
     */
    public void incrementCircuitBreakerCallsCircuitOpenCount();

    /**
     * Called when a method with {@code CircuitBreaker} succeeds
     */
    public void incrementCircuitBreakerCallsSuccessCount();

    /**
     * Called when a method with {@code CircuitBreaker} runs but fails
     */
    public void incrementCircuitBreakerCallsFailureCount();

    /**
     * Called when the circuit opens
     */
    public void reportCircuitOpen();

    /**
     * Called when the circuit half-opens
     */
    public void reportCircuitHalfOpen();

    /**
     * Called when the circuit closes
     */
    public void reportCircuitClosed();

    /**
     * Called when the bulkhead rejects a call
     */
    public void incrementBulkheadRejectedCount();

    /**
     * Called when the bulkhead accepts a call
     */
    public void incrementBulkeadAcceptedCount();

    /**
     * Report the time an execution spent waiting in the bulkhead queue
     */
    public void reportQueueWaitTime(long queueWaitNanos);

    /**
     * Provide a supplier of the current concurrent execution count for the bulkhead
     */
    public void setBulkheadConcurentExecutionCountSupplier(LongSupplier concurrentExecutionCountSupplier);

    /**
     * Provide a supplier of the current queue population for the bulkhead
     */
    public void setBulkheadQueuePopulationSupplier(LongSupplier concurrentExecutionCountSupplier);

    /**
     * Called when a method with {@link Bulkhead} completes, (either successfully or unsuccessfully)
     *
     * @param executionTime the time in nanoseconds the method took to execute
     */
    public void recordBulkheadExecutionTime(long executionTime);

    /**
     * Called when a fallback method or handler is called
     */
    public void incrementFallbackCalls();

}
