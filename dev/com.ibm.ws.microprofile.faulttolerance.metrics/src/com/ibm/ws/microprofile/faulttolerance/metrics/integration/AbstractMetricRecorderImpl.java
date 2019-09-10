/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorderProvider.AsyncType;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

/**
 *
 */
public abstract class AbstractMetricRecorderImpl implements MetricRecorder {

    protected interface MetaDataFactory {
        Metadata create(String name, MetricType type, String unit);
    }

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
    private final Counter fallbackCalls;
    private LongSupplier concurrentExecutionCountSupplier = null;
    private LongSupplier queuePopulationSupplier = null;
    private long openNanos;
    private long halfOpenNanos;
    private long closedNanos;

    private final MetaDataFactory metadataFactory;

    private enum CircuitBreakerState {
        CLOSED,
        HALF_OPEN,
        OPEN
    }

    public AbstractMetricRecorderImpl(String metricPrefix, MetricRegistry registry, RetryPolicy retryPolicy, CircuitBreakerPolicy circuitBreakerPolicy, TimeoutPolicy timeoutPolicy,
                                      BulkheadPolicy bulkheadPolicy, FallbackPolicy fallbackPolicy, AsyncType isAsync, MetaDataFactory mdFactory) {
        this.metadataFactory = mdFactory;
        if (retryPolicy != null || timeoutPolicy != null || circuitBreakerPolicy != null || bulkheadPolicy != null || fallbackPolicy != null) {
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
            Metadata tdhMetricsMetadata = metadataFactory.create(metricPrefix
                                                                 + ".timeout.executionDuration", MetricType.HISTOGRAM, MetricUnits.NANOSECONDS);
            timeoutDurationHistogram = registry.histogram(tdhMetricsMetadata);
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
            circuitBreakerOpenTime = gauge(registry, metricPrefix + ".circuitbreaker.open.total", MetricUnits.NANOSECONDS, this::getCircuitBreakerAccumulatedOpen);
            circuitBreakerHalfOpenTime = gauge(registry, metricPrefix + ".circuitbreaker.halfOpen.total", MetricUnits.NANOSECONDS, this::getCircuitBreakerAccumulatedHalfOpen);
            circuitBreakerClosedTime = gauge(registry, metricPrefix + ".circuitbreaker.closed.total", MetricUnits.NANOSECONDS, this::getCircuitBreakerAccumulatedClosed);
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
            Metadata bheMetricsMetadata = metadataFactory.create(metricPrefix
                                                                 + ".bulkhead.executionDuration", MetricType.HISTOGRAM, MetricUnits.NANOSECONDS);
            bulkheadConcurrentExecutions = gauge(registry, metricPrefix + ".bulkhead.concurrentExecutions", MetricUnits.NONE, this::getConcurrentExecutions);
            bulkheadRejectionsCounter = registry.counter(metricPrefix + ".bulkhead.callsRejected.total");
            bulkheadAcceptedCounter = registry.counter(metricPrefix + ".bulkhead.callsAccepted.total");
            bulkheadExecutionDuration = registry.histogram(bheMetricsMetadata);
        } else {
            bulkheadConcurrentExecutions = null;
            bulkheadRejectionsCounter = null;
            bulkheadAcceptedCounter = null;
            bulkheadExecutionDuration = null;
        }

        if (bulkheadPolicy != null && isAsync == AsyncType.ASYNC) {
            Metadata bhWaitingTimeMeta = metadataFactory.create(metricPrefix
                                                                + ".bulkhead.waiting.duration", MetricType.HISTOGRAM, MetricUnits.NANOSECONDS);
            bulkheadQueuePopulation = gauge(registry, metricPrefix + ".bulkhead.waitingQueue.population", MetricUnits.NONE, this::getQueuePopulation);
            bulkheadQueueWaitTimeHistogram = registry.histogram(bhWaitingTimeMeta);
        } else {
            bulkheadQueuePopulation = null;
            bulkheadQueueWaitTimeHistogram = null;
        }

        if (fallbackPolicy != null) {
            fallbackCalls = registry.counter(metricPrefix + ".fallback.calls.total");
        } else {
            fallbackCalls = null;
        }

        lastCircuitBreakerTransitionTime = System.nanoTime();
    }

    private CircuitBreakerState circuitBreakerState = CircuitBreakerState.CLOSED;
    protected long lastCircuitBreakerTransitionTime;

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementInvocationCount() {
        if (invocationCounter != null) {
            invocationCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementInvocationFailedCount() {
        if (invocationFailedCounter != null) {
            invocationFailedCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementRetryCallsSuccessImmediateCount() {
        if (retryCallsSuccessImmediateCounter != null) {
            retryCallsSuccessImmediateCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementRetryCallsSuccessRetriesCount() {
        if (retryCallsSuccessRetryCounter != null) {
            retryCallsSuccessRetryCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementRetryCallsFailureCount() {
        if (retryCallsFailureCounter != null) {
            retryCallsFailureCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementRetriesCount() {
        if (retryRetriesCounter != null) {
            retryRetriesCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void recordTimeoutExecutionTime(long executionNanos) {
        if (timeoutDurationHistogram != null) {
            timeoutDurationHistogram.update(executionNanos);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementTimeoutTrueCount() {
        if (timeoutTrueCalls != null) {
            timeoutTrueCalls.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementTimeoutFalseCount() {
        if (timeoutFalseCalls != null) {
            timeoutFalseCalls.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementCircuitBreakerCallsCircuitOpenCount() {
        if (circuitBreakerCallsOpenCounter != null) {
            circuitBreakerCallsOpenCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementCircuitBreakerCallsSuccessCount() {
        if (circuitBreakerCallsSuccessCounter != null) {
            circuitBreakerCallsSuccessCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementCircuitBreakerCallsFailureCount() {
        if (circuitBreakerCallsFailureCounter != null) {
            circuitBreakerCallsFailureCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementBulkheadRejectedCount() {
        if (bulkheadRejectionsCounter != null) {
            bulkheadRejectionsCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementBulkeadAcceptedCount() {
        if (bulkheadAcceptedCounter != null) {
            bulkheadAcceptedCounter.inc();
        }
    }

    @Trivial
    @Override
    public synchronized void reportCircuitOpen() {
        if (circuitBreakerState != CircuitBreakerState.OPEN) {
            recordEndOfCircuitBreakerState(circuitBreakerState);
            circuitBreakerState = CircuitBreakerState.OPEN;
            circuitBreakerTimesOpenedCounter.inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public synchronized void reportCircuitHalfOpen() {
        if (circuitBreakerState != CircuitBreakerState.HALF_OPEN) {
            recordEndOfCircuitBreakerState(circuitBreakerState);
            circuitBreakerState = CircuitBreakerState.HALF_OPEN;
        }
    }

    @Trivial
    @Override
    public synchronized void reportCircuitClosed() {
        if (circuitBreakerState != CircuitBreakerState.CLOSED) {
            recordEndOfCircuitBreakerState(circuitBreakerState);
            circuitBreakerState = CircuitBreakerState.CLOSED;
        }
    }

    @Trivial
    private void recordEndOfCircuitBreakerState(CircuitBreakerState oldState) {
        long now = System.nanoTime();

        switch (oldState) {
            case CLOSED:
                closedNanos += (now - lastCircuitBreakerTransitionTime);
                break;
            case HALF_OPEN:
                halfOpenNanos += (now - lastCircuitBreakerTransitionTime);
                break;
            case OPEN:
                openNanos += (now - lastCircuitBreakerTransitionTime);
                break;
        }

        lastCircuitBreakerTransitionTime = now;
    }

    @Trivial
    @Override
    public void reportQueueWaitTime(long queueWaitNanos) {
        if (bulkheadQueueWaitTimeHistogram != null) {
            bulkheadQueueWaitTimeHistogram.update(queueWaitNanos);
        }
    }

    @Trivial
    private synchronized long getCircuitBreakerAccumulatedOpen() {
        long computedNanos = openNanos;
        if (circuitBreakerState == CircuitBreakerState.OPEN) {
            computedNanos += System.nanoTime() - lastCircuitBreakerTransitionTime;
        }
        return computedNanos;
    }

    @Trivial
    private synchronized long getCircuitBreakerAccumulatedHalfOpen() {
        long computedNanos = halfOpenNanos;
        if (circuitBreakerState == CircuitBreakerState.HALF_OPEN) {
            computedNanos += System.nanoTime() - lastCircuitBreakerTransitionTime;
        }
        return computedNanos;
    }

    @Trivial
    private synchronized long getCircuitBreakerAccumulatedClosed() {
        long computedNanos = closedNanos;
        if (circuitBreakerState == CircuitBreakerState.CLOSED) {
            computedNanos += System.nanoTime() - lastCircuitBreakerTransitionTime;
        }
        return computedNanos;
    }

    @Trivial
    private Long getConcurrentExecutions() {
        if (concurrentExecutionCountSupplier != null) {
            return concurrentExecutionCountSupplier.getAsLong();
        } else {
            return 0L;
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void setBulkheadConcurentExecutionCountSupplier(LongSupplier concurrentExecutionCountSupplier) {
        this.concurrentExecutionCountSupplier = concurrentExecutionCountSupplier;
    }

    @Trivial
    private Long getQueuePopulation() {
        if (queuePopulationSupplier != null) {
            return queuePopulationSupplier.getAsLong();
        } else {
            return 0L;
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void setBulkheadQueuePopulationSupplier(LongSupplier queuePopulationSupplier) {
        this.queuePopulationSupplier = queuePopulationSupplier;
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void recordBulkheadExecutionTime(long executionTime) {
        if (bulkheadExecutionDuration != null) {
            bulkheadExecutionDuration.update(executionTime);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementFallbackCalls() {
        fallbackCalls.inc();
    }

    @FFDCIgnore(IllegalArgumentException.class)
    private Gauge<Long> gauge(MetricRegistry registry, String name, String units, Gauge<Long> supplier) {
        Gauge<Long> result = null;
        try {
            Metadata gaugeMeta = metadataFactory.create(name, MetricType.GAUGE, units);
            result = registry.register(gaugeMeta, supplier);
        } catch (IllegalArgumentException ex) {
            // Thrown if metric already exists
        }
        return result;
    }

}