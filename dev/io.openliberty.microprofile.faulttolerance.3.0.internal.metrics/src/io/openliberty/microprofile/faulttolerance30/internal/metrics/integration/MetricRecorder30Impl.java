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
package io.openliberty.microprofile.faulttolerance30.internal.metrics.integration;

import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.FallbackOccurred.NO_FALLBACK;
import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.FallbackOccurred.WITH_FALLBACK;
import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.RetriesOccurred.NO_RETRIES;
import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.RetriesOccurred.WITH_RETRIES;
import static com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory.EXCEPTION_IN_ABORT_ON;
import static com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory.EXCEPTION_NOT_IN_RETRY_ON;
import static com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory.MAX_DURATION_REACHED;
import static com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory.MAX_RETRIES_REACHED;
import static com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory.NO_EXCEPTION;
import static org.eclipse.microprofile.metrics.MetricType.COUNTER;

import java.util.EnumMap;
import java.util.function.LongSupplier;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorderProvider.AsyncType;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

/**
 * Records Fault Tolerance metrics for the FT 3.0 spec
 * <p>
 * The FT 3.0 spec changes from using lots of separate metrics to using fewer metrics but storing multiple values by using tags.
 * From a recording perspective, this introduces very little change as we just treat each combination of name and tag as a separate metric.
 * <p>
 * This class initializes all of the metrics for the method up front in the constructor and stores them in fields so that we don't have to register or look up metrics while the
 * method is running.
 * <p>
 * In some cases, where a metric has tags with lots of values, we use an EnumSet to store the metrics for each tag combination.
 * <p>
 * In other cases, where a metric has few or no possible tag values, we just use a separate field for each tag combination.
 */
public class MetricRecorder30Impl implements MetricRecorder {

    /**
     * Constant map from a {@link RetryResultCategory} to its corresponding metric {@link Tag}
     */
    private static final EnumMap<RetryResultCategory, Tag> RETRY_RESULT_TAGS = new EnumMap<>(RetryResultCategory.class);

    /**
     * Constant map from a {@link RetriesOccurred} to its corresponding metric {@link Tag}
     */
    private static final EnumMap<RetriesOccurred, Tag> RETRIES_OCCURRED_TAGS = new EnumMap<>(RetriesOccurred.class);

    static {
        RETRY_RESULT_TAGS.put(NO_EXCEPTION, new Tag("retryResult", "valueReturned"));
        RETRY_RESULT_TAGS.put(EXCEPTION_IN_ABORT_ON, new Tag("retryResult", "exceptionNotRetryable"));
        RETRY_RESULT_TAGS.put(EXCEPTION_NOT_IN_RETRY_ON, new Tag("retryResult", "exceptionNotRetryable"));
        RETRY_RESULT_TAGS.put(MAX_DURATION_REACHED, new Tag("retryResult", "maxDurationReached"));
        RETRY_RESULT_TAGS.put(MAX_RETRIES_REACHED, new Tag("retryResult", "maxRetriesReached"));

        RETRIES_OCCURRED_TAGS.put(WITH_RETRIES, new Tag("retried", "true"));
        RETRIES_OCCURRED_TAGS.put(NO_RETRIES, new Tag("retried", "false"));
    }

    private final EnumMap<FallbackOccurred, Counter> invocationSuccessCounter;
    private final EnumMap<FallbackOccurred, Counter> invocationFailedCounter;
    private final EnumMap<RetryResultCategory, EnumMap<RetriesOccurred, Counter>> retryCallsCounter;
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

    private LongSupplier concurrentExecutionCountSupplier = null;
    private LongSupplier queuePopulationSupplier = null;

    /*
     * Fields storing required state for circuit breaker metrics
     */
    private long openNanos;
    private long halfOpenNanos;
    private long closedNanos;
    private CircuitBreakerState circuitBreakerState = CircuitBreakerState.CLOSED;
    protected long lastCircuitBreakerTransitionTime;

    private enum CircuitBreakerState {
        CLOSED,
        HALF_OPEN,
        OPEN
    }

    public MetricRecorder30Impl(String methodName, MetricRegistry registry, RetryPolicy retryPolicy, CircuitBreakerPolicy circuitBreakerPolicy, TimeoutPolicy timeoutPolicy,
                                BulkheadPolicy bulkheadPolicy, FallbackPolicy fallbackPolicy, AsyncType isAsync) {

        /*
         * Register all of the metrics required for this method and store them in fields
         */

        // Every metric uses this tag to identify the method it's reporting metrics for
        Tag methodTag = new Tag("method", methodName);

        if (retryPolicy != null || timeoutPolicy != null || circuitBreakerPolicy != null || bulkheadPolicy != null || fallbackPolicy != null) {
            Metadata invocationsMetadata = Metadata.builder().withName("ft.invocations.total").withType(COUNTER).build();
            Tag valueReturnedTag = new Tag("result", "valueReturned");
            Tag exceptionThrownTag = new Tag("result", "exceptionThrown");
            invocationSuccessCounter = new EnumMap<>(FallbackOccurred.class);
            invocationFailedCounter = new EnumMap<>(FallbackOccurred.class);

            if (fallbackPolicy != null) {
                // If there's a fallback policy we need four metrics to cover the combinations of
                // fallback = [applied|notApplied] and result = [valueReturned|exceptionThrown]
                Tag withFallbackTag = new Tag("fallback", "applied");
                Tag withoutFallbackTag = new Tag("fallback", "notApplied");

                invocationSuccessCounter.put(NO_FALLBACK, registry.counter(invocationsMetadata, methodTag, valueReturnedTag, withoutFallbackTag));
                invocationSuccessCounter.put(WITH_FALLBACK, registry.counter(invocationsMetadata, methodTag, valueReturnedTag, withFallbackTag));

                invocationFailedCounter.put(NO_FALLBACK, registry.counter(invocationsMetadata, methodTag, exceptionThrownTag, withoutFallbackTag));
                invocationFailedCounter.put(WITH_FALLBACK, registry.counter(invocationsMetadata, methodTag, exceptionThrownTag, withFallbackTag));
            } else {
                // If there's no fallback, then we only need two metrics to cover the combinations of
                // fallback = [notDefined] and result = [valueReturned|exceptionThrown]
                Tag noFallbackTag = new Tag("fallback", "notDefined");

                Counter invocationSuccess = registry.counter(invocationsMetadata, methodTag, valueReturnedTag, noFallbackTag);
                invocationSuccessCounter.put(NO_FALLBACK, invocationSuccess);
                invocationSuccessCounter.put(WITH_FALLBACK, invocationSuccess);

                Counter invocationFailed = registry.counter(invocationsMetadata, methodTag, exceptionThrownTag, noFallbackTag);
                invocationFailedCounter.put(NO_FALLBACK, invocationFailed);
                invocationFailedCounter.put(WITH_FALLBACK, invocationFailed);
            }
        } else {
            invocationSuccessCounter = null;
            invocationFailedCounter = null;
        }

        if (retryPolicy != null) {
            retryCallsCounter = new EnumMap<>(RetryResultCategory.class);
            // Iterate through the combinations of retry tags, creating each counter
            for (RetryResultCategory resultCategory : RETRY_RESULT_TAGS.keySet()) {
                EnumMap<RetriesOccurred, Counter> submap = new EnumMap<>(RetriesOccurred.class);
                retryCallsCounter.put(resultCategory, submap);
                for (RetriesOccurred retriesOccurred : RETRIES_OCCURRED_TAGS.keySet()) {
                    submap.put(retriesOccurred,
                               registry.counter("ft.retry.calls.total", methodTag, RETRY_RESULT_TAGS.get(resultCategory), RETRIES_OCCURRED_TAGS.get(retriesOccurred)));
                }
            }
            retryRetriesCounter = registry.counter("ft.retry.retries.total", methodTag);
        } else {
            retryCallsCounter = null;
            retryRetriesCounter = null;
        }

        if (timeoutPolicy != null) {
            Metadata timeoutDurationMetadata = Metadata.builder().withName("ft.timeout.executionDuration").withType(MetricType.HISTOGRAM).withUnit(MetricUnits.NANOSECONDS).build();
            timeoutDurationHistogram = registry.histogram(timeoutDurationMetadata, methodTag);

            Metadata timeoutCallsMetadata = Metadata.builder().withName("ft.timeout.calls.total").withType(COUNTER).build();
            timeoutTrueCalls = registry.counter(timeoutCallsMetadata, methodTag, new Tag("timedOut", "true"));
            timeoutFalseCalls = registry.counter(timeoutCallsMetadata, methodTag, new Tag("timedOut", "false"));
        } else {
            timeoutDurationHistogram = null;
            timeoutTrueCalls = null;
            timeoutFalseCalls = null;
        }

        if (circuitBreakerPolicy != null) {
            Metadata cbCallsMetadata = Metadata.builder().withName("ft.circuitbreaker.calls.total").withType(COUNTER).build();
            circuitBreakerCallsFailureCounter = registry.counter(cbCallsMetadata, methodTag, new Tag("circuitBreakerResult", "failure"));
            circuitBreakerCallsSuccessCounter = registry.counter(cbCallsMetadata, methodTag, new Tag("circuitBreakerResult", "success"));
            circuitBreakerCallsOpenCounter = registry.counter(cbCallsMetadata, methodTag, new Tag("circuitBreakerResult", "circuitBreakerOpen"));

            Metadata cbStateTimeMetadata = Metadata.builder().withName("ft.circuitbreaker.state.total").withType(MetricType.GAUGE).withUnit(MetricUnits.NANOSECONDS).build();
            circuitBreakerOpenTime = gauge(registry, cbStateTimeMetadata, this::getCircuitBreakerAccumulatedOpen, methodTag, new Tag("state", "open"));
            circuitBreakerHalfOpenTime = gauge(registry, cbStateTimeMetadata, this::getCircuitBreakerAccumulatedHalfOpen, methodTag, new Tag("state", "halfOpen"));
            circuitBreakerClosedTime = gauge(registry, cbStateTimeMetadata, this::getCircuitBreakerAccumulatedClosed, methodTag, new Tag("state", "closed"));

            circuitBreakerTimesOpenedCounter = registry.counter("ft.circuitbreaker.opened.total", methodTag);
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
            Metadata bulkheadCallsMetadata = Metadata.builder().withName("ft.bulkhead.calls.total").withType(COUNTER).build();
            bulkheadAcceptedCounter = registry.counter(bulkheadCallsMetadata, methodTag, new Tag("bulkheadResult", "accepted"));
            bulkheadRejectionsCounter = registry.counter(bulkheadCallsMetadata, methodTag, new Tag("bulkheadResult", "rejected"));

            Metadata executionsRunningMetadata = Metadata.builder().withName("ft.bulkhead.executionsRunning").withType(MetricType.GAUGE).build();
            bulkheadConcurrentExecutions = gauge(registry, executionsRunningMetadata, this::getConcurrentExecutions, methodTag);

            Metadata runningDurationMetadata = Metadata.builder().withName("ft.bulkhead.runningDuration").withType(MetricType.HISTOGRAM).withUnit(MetricUnits.NANOSECONDS).build();
            bulkheadExecutionDuration = registry.histogram(runningDurationMetadata, methodTag);
        } else {
            bulkheadRejectionsCounter = null;
            bulkheadAcceptedCounter = null;
            bulkheadConcurrentExecutions = null;
            bulkheadExecutionDuration = null;
        }

        if (bulkheadPolicy != null && isAsync == AsyncType.ASYNC) {
            Metadata executionsWaitingMetadata = Metadata.builder().withName("ft.bulkhead.executionsWaiting").withType(MetricType.GAUGE).build();
            bulkheadQueuePopulation = gauge(registry, executionsWaitingMetadata, this::getQueuePopulation, methodTag);

            Metadata waitingDurationMetadata = Metadata.builder().withName("ft.bulkhead.waitingDuration").withType(MetricType.HISTOGRAM).withUnit(MetricUnits.NANOSECONDS).build();
            bulkheadQueueWaitTimeHistogram = registry.histogram(waitingDurationMetadata, methodTag);
        } else {
            bulkheadQueuePopulation = null;
            bulkheadQueueWaitTimeHistogram = null;
        }

        lastCircuitBreakerTransitionTime = System.nanoTime();
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementInvocationSuccessCount(FallbackOccurred fallbackOccurred) {
        if (invocationSuccessCounter != null) {
            invocationSuccessCounter.get(fallbackOccurred).inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementInvocationFailedCount(FallbackOccurred fallbackOccurred) {
        if (invocationFailedCounter != null) {
            invocationFailedCounter.get(fallbackOccurred).inc();
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementRetryCalls(RetryResultCategory resultCategory, RetriesOccurred retriesOccurred) {
        if (retryCallsCounter != null) {
            retryCallsCounter.get(resultCategory).get(retriesOccurred).inc();
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

    @FFDCIgnore(IllegalArgumentException.class)
    private Gauge<Long> gauge(MetricRegistry registry, Metadata gaugeMeta, Gauge<Long> supplier, Tag... tags) {
        Gauge<Long> result = null;
        try {
            result = registry.register(gaugeMeta, supplier, tags);
        } catch (IllegalArgumentException ex) {
            // Thrown if metric already exists
        }
        return result;
    }

}