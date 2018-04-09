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
package com.ibm.ws.microprofile.faulttolerance.metrics.integration;

import java.util.function.LongSupplier;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;

import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorderProvider.AsyncType;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

/**
 * Records metrics using MP Metrics API 1.1
 */
public class MetricRecorderImpl implements MetricRecorder {

    private final Counter invocationCounter;
    private final Counter invocationFailedCounter;

    private final Counter retryCallsSuccessImmediateCounter;
    private final Counter retryCallsSuccessRetryCounter;
    private final Counter retryCallsFailureCounter;
    private final Counter retryRetriesCounter;

    private final Histogram timeoutDurationHistogram;
    private final Counter timeoutTrueCalls;
    private final Counter timeoutFalseCalls;

    private final Counter circuitBreakerCallsSuccessCounter;
    private final Counter circuitBreakerCallsFailureCounter;
    private final Counter circuitBreakerCallsOpenCounter;
    @SuppressWarnings("unused")
    private final Gauge<Long> circuitBreakerOpenTime;
    @SuppressWarnings("unused")
    private final Gauge<Long> circuitBreakerHalfOpenTime;
    @SuppressWarnings("unused")
    private final Gauge<Long> circuitBreakerClosedTime;
    private final Counter circuitBreakerTimesOpenedCounter;

    @SuppressWarnings("unused")
    private final Gauge<Long> bulkheadConcurrentExecutions;
    private final Counter bulkheadRejectionsCounter;
    private final Counter bulkheadAcceptedCounter;
    private final Histogram bulkheadExecutionDuration;
    @SuppressWarnings("unused")
    private final Gauge<Long> bulkheadQueuePopulation;
    private final Histogram bulkheadQueueWaitTimeHistogram;

    private long openNanos;
    private long halfOpenNanos;
    private long closedNanos;
    private int state = 0;
    private long lastTransitionTime;

    public MetricRecorderImpl(String metricPrefix, MetricRegistry registry, RetryPolicy retryPolicy, CircuitBreakerPolicy circuitBreakerPolicy, TimeoutPolicy timeoutPolicy,
                              BulkheadPolicy bulkheadPolicy, FallbackPolicy fallbackPolicy, AsyncType isAsync) {

        if (retryPolicy != null || timeoutPolicy != null || circuitBreakerPolicy != null || bulkheadPolicy != null) {
            invocationCounter = registry.counter(metricPrefix + ".invocations.total");
            invocationFailedCounter = registry.counter(metricPrefix + ".invocations.failed.total");
        } else {
            invocationCounter = null;
            invocationFailedCounter = null;
        }

        if (retryPolicy != null) {
            retryCallsSuccessImmediateCounter = registry.counter(metricPrefix + ".retry.callsSucceededNotRetried.total");
            retryCallsSuccessRetryCounter = registry.counter(metricPrefix + ".retry.callsSucceededRetried.total");
            retryCallsFailureCounter = registry.counter(metricPrefix + ".retry.callsFailed.total");
            retryRetriesCounter = registry.counter(metricPrefix + ".retry.retries.total");
        } else {
            retryCallsSuccessImmediateCounter = null;
            retryCallsSuccessRetryCounter = null;
            retryCallsFailureCounter = null;
            retryRetriesCounter = null;
        }

        if (timeoutPolicy != null) {
            timeoutDurationHistogram = registry.histogram(new Metadata(metricPrefix + ".timeout.executionDuration", MetricType.HISTOGRAM, MetricUnits.NANOSECONDS));
            timeoutTrueCalls = registry.counter(metricPrefix + ".timeout.callsTimedOut.total");
            timeoutFalseCalls = registry.counter(metricPrefix + ".timeout.callsNotTimedOut.total");
        } else {
            timeoutDurationHistogram = null;
            timeoutTrueCalls = null;
            timeoutFalseCalls = null;
        }

        if (circuitBreakerPolicy != null) {
            circuitBreakerCallsFailureCounter = registry.counter(metricPrefix + ".circuitbreaker.callsFailed.total");
            circuitBreakerCallsSuccessCounter = registry.counter(metricPrefix + ".circuitbreaker.callsSucceeded.total");
            circuitBreakerCallsOpenCounter = registry.counter(metricPrefix + ".circuitbreaker.callsPrevented.total");
            circuitBreakerOpenTime = registry.<Gauge<Long>> register(metricPrefix + ".circuitbreaker.open.total", this::getCircuitBreakerAccumulatedOpen);
            circuitBreakerHalfOpenTime = registry.<Gauge<Long>> register(metricPrefix + ".circuitbreaker.halfOpen.total", this::getCircuitBreakerAccumulatedHalfOpen);
            circuitBreakerClosedTime = registry.<Gauge<Long>> register(metricPrefix + ".circuitbreaker.closed.total", this::getCircuitBreakerAccumulatedClosed);
            circuitBreakerTimesOpenedCounter = registry.counter(metricPrefix + ".circuitbreaker.opened.total");
        } else {
            circuitBreakerCallsFailureCounter = null;
            circuitBreakerCallsSuccessCounter = null;
            circuitBreakerCallsOpenCounter = null;
            circuitBreakerOpenTime = null;
            circuitBreakerHalfOpenTime = null;
            circuitBreakerClosedTime = null;
            circuitBreakerTimesOpenedCounter = null;
        }

        if (bulkheadPolicy != null) {
            bulkheadConcurrentExecutions = registry.<Gauge<Long>> register(metricPrefix + ".bulkhead.concurrentExecutions", this::getConcurrentExecutions);
            bulkheadRejectionsCounter = registry.counter(metricPrefix + ".bulkhead.callsRejected.total");
            bulkheadAcceptedCounter = registry.counter(metricPrefix + ".bulkhead.callsAccepted.total");
            bulkheadExecutionDuration = registry.histogram(new Metadata(metricPrefix + ".bulkhead.executionDuration", MetricType.HISTOGRAM, MetricUnits.NANOSECONDS));
        } else {
            bulkheadConcurrentExecutions = null;
            bulkheadRejectionsCounter = null;
            bulkheadAcceptedCounter = null;
            bulkheadExecutionDuration = null;
        }

        if (bulkheadPolicy != null && isAsync == AsyncType.ASYNC) {
            bulkheadQueuePopulation = registry.<Gauge<Long>> register(metricPrefix + ".bulkhead.waitingQueue.population", this::getQueuePopulation);
            bulkheadQueueWaitTimeHistogram = registry.histogram(metricPrefix + ".bulkhead.waiting.duration");
        } else {
            bulkheadQueuePopulation = null;
            bulkheadQueueWaitTimeHistogram = null;
        }

        lastTransitionTime = System.nanoTime();
    }

    /** {@inheritDoc} */
    @Override
    public void incrementInvocationCount() {
        if (invocationCounter != null) {
            invocationCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void incrementInvocationFailedCount() {
        if (invocationFailedCounter != null) {
            invocationFailedCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void incrementRetryCallsSuccessImmediateCount() {
        if (retryCallsSuccessImmediateCounter != null) {
            retryCallsSuccessImmediateCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void incrementRetryCallsSuccessRetriesCount() {
        if (retryCallsSuccessRetryCounter != null) {
            retryCallsSuccessRetryCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void incrementRetryCallsFailureCount() {
        if (retryCallsFailureCounter != null) {
            retryCallsFailureCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void incrementRetriesCount() {
        if (retryRetriesCounter != null) {
            retryRetriesCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void recordTimeoutExecutionTime(long executionNanos) {
        if (timeoutDurationHistogram != null) {
            timeoutDurationHistogram.update(executionNanos);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void incrementTimeoutTrueCount() {
        if (timeoutTrueCalls != null) {
            timeoutTrueCalls.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void incrementTimeoutFalseCount() {
        if (timeoutFalseCalls != null) {
            timeoutFalseCalls.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void incrementCircuitBreakerCallsCircuitOpenCount() {
        if (circuitBreakerCallsOpenCounter != null) {
            circuitBreakerCallsOpenCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void incrementCircuitBreakerCallsSuccessCount() {
        if (circuitBreakerCallsSuccessCounter != null) {
            circuitBreakerCallsSuccessCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void incrementCircuitBreakerCallsFailureCount() {
        if (circuitBreakerCallsFailureCounter != null) {
            circuitBreakerCallsFailureCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void incrementBulkheadRejectedCount() {
        if (bulkheadRejectionsCounter != null) {
            bulkheadRejectionsCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void incrementBulkeadAcceptedCount() {
        if (bulkheadAcceptedCounter != null) {
            bulkheadAcceptedCounter.inc();
        }
    }

    @Override
    public synchronized void reportCircuitOpen() {
        if (state != 2) {
            recordEndOfState(state);
            state = 2;
            circuitBreakerTimesOpenedCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void reportCircuitHalfOpen() {
        if (state != 1) {
            recordEndOfState(state);
            state = 1;
        }
    }

    @Override
    public synchronized void reportCircuitClosed() {
        if (state != 0) {
            recordEndOfState(state);
            state = 0;
        }
    }

    private void recordEndOfState(int oldState) {
        long now = System.nanoTime();

        switch (oldState) {
            case 0:
                closedNanos += (now - lastTransitionTime);
                break;
            case 1:
                halfOpenNanos += (now - lastTransitionTime);
                break;
            case 2:
                openNanos += (now - lastTransitionTime);
                break;
            default:
                throw new IllegalStateException("Invalid state: " + state);
        }

        lastTransitionTime = now;
    }

    @Override
    public void reportQueueWaitTime(long queueWaitNanos) {
        if (bulkheadQueueWaitTimeHistogram != null) {
            bulkheadQueueWaitTimeHistogram.update(queueWaitNanos);
        }
    }

    private synchronized long getCircuitBreakerAccumulatedOpen() {
        long computedNanos = openNanos;
        if (state == 2) {
            computedNanos += System.nanoTime() - lastTransitionTime;
        }
        return computedNanos;
    }

    private synchronized long getCircuitBreakerAccumulatedHalfOpen() {
        long computedNanos = halfOpenNanos;
        if (state == 1) {
            computedNanos += System.nanoTime() - lastTransitionTime;
        }
        return computedNanos;
    }

    private synchronized long getCircuitBreakerAccumulatedClosed() {
        long computedNanos = closedNanos;
        if (state == 0) {
            computedNanos += System.nanoTime() - lastTransitionTime;
        }
        return computedNanos;
    }

    private LongSupplier concurrentExecutionCountSupplier = null;

    private Long getConcurrentExecutions() {
        if (concurrentExecutionCountSupplier != null) {
            return concurrentExecutionCountSupplier.getAsLong();
        } else {
            return 0L;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setBulkheadConcurentExecutionCountSupplier(LongSupplier concurrentExecutionCountSupplier) {
        this.concurrentExecutionCountSupplier = concurrentExecutionCountSupplier;
    }

    private LongSupplier queuePopulationSupplier = null;

    private Long getQueuePopulation() {
        if (queuePopulationSupplier != null) {
            return queuePopulationSupplier.getAsLong();
        } else {
            return 0L;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setBulkheadQueuePopulationSupplier(LongSupplier queuePopulationSupplier) {
        this.queuePopulationSupplier = queuePopulationSupplier;
    }

    /** {@inheritDoc} */
    @Override
    public void recordBulkheadExecutionTime(long executionTime) {
        if (bulkheadExecutionDuration != null) {
            bulkheadExecutionDuration.update(executionTime);
        }
    }

}
