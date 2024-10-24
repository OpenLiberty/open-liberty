/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.faulttolerance.telemetry.metrics.integration;

import java.util.List;
import java.util.function.LongSupplier;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorderProvider.AsyncType;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;

/**
 * Records Fault Tolerance metrics for the FT 4.1 spec
 * <p>
 * The 4.1 spec supports both MPMetrics and MPTelemetry, this class only handles MP telemetry. Unlike MPMetrics tags (attributes in MP Telemetry parlance) do not need to be unique
 * to a counter
 * so we initialize one object for every metric, and then attach attributes every time we add a value into that object.
 * <p>
 * This class initializes all of the metrics for the method up front in the constructor and stores them in fields so that we don't have to register or look up metrics while the
 * method is running.
 * <p>
 * In some cases, where a metric has tags with lots of values, we use an EnumSet to store the metrics for each tag combination.
 * <p>
 * In other cases, where a metric has few or no possible tag values, we just use a separate field for each tag combination.
 */
public class MetricRecorderImpl implements MetricRecorder {

    private static final TraceComponent tc = Tr.register(MetricRecorderImpl.class);
    private static final AttributeKey<String> BULKHEAD_RESULT = AttributeKey.stringKey("bulkheadResult");
    private static final AttributeKey<String> CIRCUIT_BREAKER_RESULT = AttributeKey.stringKey("circuitBreakerResult");
    private static final AttributeKey<String> FALLBACK = AttributeKey.stringKey("fallback");
    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");
    private static final AttributeKey<String> RETRIED = AttributeKey.stringKey("retried");
    private static final AttributeKey<String> RETRY_RESULT = AttributeKey.stringKey("retryResult");
    private static final AttributeKey<String> STATE = AttributeKey.stringKey("state");
    private static final AttributeKey<String> TIMED_OUT = AttributeKey.stringKey("timedOut");
    private static final AttributeKey<String> METHOD = AttributeKey.stringKey("method");
    private static final List<Double> HISTOGRAM_BUCKETS = List.of(0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0);

    // Every metric uses this tag to identify the method it's reporting metrics for
    private final Attributes methodAttribute;

    //Fields for spec Metric
    private final LongCounter ftInvocationsTotal;
    private final boolean fallbackDefined;

    //Fields for @Retry Metrics
    private final LongCounter ftRetryCallsTotal;
    private final LongCounter ftRetryRetriesTotal;

    //Fields for @Timeout Metrics
    private final LongCounter ftTimeoutCallsTotal;
    private final DoubleHistogram ftTimeoutExecutionDuration;

    //Fields for @CircuitBreaker Metrics
    private final LongCounter ftCircuitbreakerCallsTotal;
    @SuppressWarnings("unused") //This object will be passed into open telemetry by the builder and used there.
    private final ObservableLongCounter ftCircuitbreakerStateTotal;
    private final LongCounter ftCircuitbreakerOpenedTotal;

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

    //Fields for @Bulkhead Metrics
    private final LongCounter ftBulkheadCallsTotal;
    @SuppressWarnings("unused") //This object will be passed into open telemetry by the builder and used there.
    private final ObservableLongUpDownCounter ftBulkheadExecutionsRunning;
    @SuppressWarnings("unused") //This object will be passed into open telemetry by the builder and used there.
    private final ObservableLongUpDownCounter ftBulkheadExecutionsWaiting;
    private final DoubleHistogram ftBulkheadRunningDuration;
    private final DoubleHistogram ftBulkheadWaitingDuration;

    private LongSupplier queuePopulationSupplier = null;
    private LongSupplier concurrentExecutionCountSupplier = null;

    public MetricRecorderImpl(String classAndMethod, Meter meter, RetryPolicy retryPolicy, CircuitBreakerPolicy circuitBreakerPolicy,
                              TimeoutPolicy timeoutPolicy,
                              BulkheadPolicy bulkheadPolicy, FallbackPolicy fallbackPolicy, AsyncType isAsync) {

        /*
         * Register all of the metrics required for this method and store them in fields
         */

        methodAttribute = Attributes.builder().put(METHOD, classAndMethod).build();

        if (retryPolicy != null || timeoutPolicy != null || circuitBreakerPolicy != null || bulkheadPolicy != null || fallbackPolicy != null) {
            ftInvocationsTotal = meter.counterBuilder("ft.invocations.total").setDescription(Tr.formatMessage(tc, "ft.invocations.total.description")).build();
            fallbackDefined = (fallbackPolicy != null);
        } else {
            ftInvocationsTotal = null;
            fallbackDefined = false;
        }

        if (retryPolicy != null) {
            ftRetryCallsTotal = meter.counterBuilder("ft.retry.calls.total").setDescription(Tr.formatMessage(tc, "ft.retry.calls.total.description")).build();
            ftRetryRetriesTotal = meter.counterBuilder("ft.retry.retries.total").setDescription(Tr.formatMessage(tc, "ft.retry.retries.total.description")).build();
        } else {
            ftRetryCallsTotal = null;
            ftRetryRetriesTotal = null;
        }

        if (timeoutPolicy != null) {
            ftTimeoutCallsTotal = meter.counterBuilder("ft.timeout.calls.total").setDescription(Tr.formatMessage(tc, "ft.timeout.calls.total.description")).build();
            ftTimeoutExecutionDuration = meter.histogramBuilder("ft.timeout.executionDuration").setDescription(Tr.formatMessage(tc,
                                                                                                                                "ft.timeout.executionDuration.description")).setUnit("seconds").setExplicitBucketBoundariesAdvice(HISTOGRAM_BUCKETS).build();
        } else {
            ftTimeoutCallsTotal = null;
            ftTimeoutExecutionDuration = null;
        }

        if (circuitBreakerPolicy != null) {
            ftCircuitbreakerCallsTotal = meter.counterBuilder("ft.circuitbreaker.calls.total").setDescription(Tr.formatMessage(tc,
                                                                                                                               "ft.circuitbreaker.calls.total.description")).build();
            ftCircuitbreakerStateTotal = meter.counterBuilder("ft.circuitbreaker.state.total").setUnit("nanoseconds").setDescription(Tr.formatMessage(tc,
                                                                                                                                                      "ft.circuitbreaker.state.total.description")).buildWithCallback(this::getCircuitBreakerAccumulatedTimes);
            ftCircuitbreakerOpenedTotal = meter.counterBuilder("ft.circuitbreaker.opened.total").setDescription(Tr.formatMessage(tc,
                                                                                                                                 "ft.circuitbreaker.opened.total.description")).build();
        } else {
            ftCircuitbreakerCallsTotal = null;
            ftCircuitbreakerStateTotal = null;
            ftCircuitbreakerOpenedTotal = null;
        }

        if (bulkheadPolicy != null) {
            ftBulkheadCallsTotal = meter.counterBuilder("ft.bulkhead.calls.total").setDescription(Tr.formatMessage(tc, "ft.bulkhead.calls.total.description")).build();
            ftBulkheadExecutionsRunning = meter.upDownCounterBuilder("ft.bulkhead.executionsRunning").setDescription(Tr.formatMessage(tc,
                                                                                                                                      "ft.bulkhead.executionsRunning.description")).buildWithCallback(this::getConcurrentExecutions);
            ftBulkheadRunningDuration = meter.histogramBuilder("ft.bulkhead.runningDuration").setDescription(Tr.formatMessage(tc,
                                                                                                                              "ft.bulkhead.runningDuration.description")).setUnit("seconds").setExplicitBucketBoundariesAdvice(HISTOGRAM_BUCKETS).build();

            if (isAsync == AsyncType.ASYNC) {
                ftBulkheadExecutionsWaiting = meter.upDownCounterBuilder("ft.bulkhead.executionsWaiting").setDescription(Tr.formatMessage(tc,
                                                                                                                                          "ft.bulkhead.executionsWaiting.description")).buildWithCallback(this::getQueuePopulation);
                ftBulkheadWaitingDuration = meter.histogramBuilder("ft.bulkhead.waitingDuration").setDescription(Tr.formatMessage(tc,
                                                                                                                                  "ft.bulkhead.waitingDuration.description")).setUnit("seconds").setExplicitBucketBoundariesAdvice(HISTOGRAM_BUCKETS).build();
            } else {
                ftBulkheadExecutionsWaiting = null;
                ftBulkheadWaitingDuration = null;
            }
        } else {
            ftBulkheadCallsTotal = null;
            ftBulkheadExecutionsRunning = null;
            ftBulkheadExecutionsWaiting = null;
            ftBulkheadRunningDuration = null;
            ftBulkheadWaitingDuration = null;
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementInvocationSuccessCount(FallbackOccurred fallbackOccurred) {
        incrementFtTotalCount(fallbackOccurred, true);
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementInvocationFailedCount(FallbackOccurred fallbackOccurred) {
        incrementFtTotalCount(fallbackOccurred, false);
    }

    @Trivial
    private void incrementFtTotalCount(FallbackOccurred fallbackOccurred, boolean invocationSucceeded) {
        AttributesBuilder ab = Attributes.builder().putAll(methodAttribute);

        if (!fallbackDefined) {
            ab.put(FALLBACK, "notDefined");
        } else if (fallbackOccurred == FallbackOccurred.WITH_FALLBACK) {
            ab.put(FALLBACK, "applied");
        } else {
            ab.put(FALLBACK, "notApplied");
        }

        if (invocationSucceeded) {
            ab.put(RESULT, "valueReturned");
        } else {
            ab.put(RESULT, "exceptionThrown");
        }

        if (ftInvocationsTotal != null) {
            ftInvocationsTotal.add(1, ab.build());
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementRetryCalls(RetryResultCategory resultCategory, RetriesOccurred retriesOccurred) {
        if (ftRetryCallsTotal != null) {

            AttributesBuilder ab = Attributes.builder().putAll(methodAttribute);

            if (retriesOccurred == RetriesOccurred.WITH_RETRIES) {
                ab.put(RETRIED, "true");
            } else {
                ab.put(RETRIED, "false");
            }

            switch (resultCategory) {
                case NO_EXCEPTION:
                    ab.put(RETRY_RESULT, "valueReturned");
                    break;
                case EXCEPTION_NOT_IN_RETRY_ON:
                    ab.put(RETRY_RESULT, "exceptionNotRetryable");
                    break;
                case EXCEPTION_IN_RETRY_ON:
                    //Unreachable. This method captures the result of the final retry. If there was an exception marked RetryOn another retry will be triggered.
                    break;
                case EXCEPTION_IN_ABORT_ON:
                    ab.put(RETRY_RESULT, "exceptionNotRetryable");
                    break;
                case MAX_RETRIES_REACHED:
                    ab.put(RETRY_RESULT, "maxRetriesReached");
                    break;
                case MAX_DURATION_REACHED:
                    ab.put(RETRY_RESULT, "maxDurationReached");
                    break;
                case NO_RETRY:
                    //Unreachable. This method captures the result of a retried method. Obviously you can't have NO_RETRY in a retried method.
                    break;
            }

            ftRetryCallsTotal.add(1, ab.build());
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementRetriesCount() {
        if (ftRetryRetriesTotal != null) {
            ftRetryRetriesTotal.add(1, methodAttribute);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void recordTimeoutExecutionTime(long executionNanos) {
        if (ftTimeoutExecutionDuration != null) {
            double seconds = executionNanos / 1_000_000_000d;
            ftTimeoutExecutionDuration.record(seconds, methodAttribute);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementTimeoutTrueCount() {
        if (ftTimeoutCallsTotal != null) {
            ftTimeoutCallsTotal.add(1, Attributes.builder().putAll(methodAttribute).put(TIMED_OUT, "true").build());
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementTimeoutFalseCount() {
        if (ftTimeoutCallsTotal != null) {
            ftTimeoutCallsTotal.add(1, Attributes.builder().putAll(methodAttribute).put(TIMED_OUT, "false").build());
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementCircuitBreakerCallsCircuitOpenCount() {
        if (ftCircuitbreakerCallsTotal != null) {
            ftCircuitbreakerCallsTotal.add(1, Attributes.builder().putAll(methodAttribute).put(CIRCUIT_BREAKER_RESULT, "circuitBreakerOpen").build());
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementCircuitBreakerCallsSuccessCount() {
        if (ftCircuitbreakerCallsTotal != null) {
            ftCircuitbreakerCallsTotal.add(1, Attributes.builder().putAll(methodAttribute).put(CIRCUIT_BREAKER_RESULT, "success").build());
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementCircuitBreakerCallsFailureCount() {
        if (ftCircuitbreakerCallsTotal != null) {
            ftCircuitbreakerCallsTotal.add(1, Attributes.builder().putAll(methodAttribute).put(CIRCUIT_BREAKER_RESULT, "failure").build());
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementBulkheadRejectedCount() {
        if (ftBulkheadCallsTotal != null) {
            ftBulkheadCallsTotal.add(1, Attributes.builder().putAll(methodAttribute).put(BULKHEAD_RESULT, "rejected").build());
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementBulkeadAcceptedCount() {
        if (ftBulkheadCallsTotal != null) {
            ftBulkheadCallsTotal.add(1, Attributes.builder().putAll(methodAttribute).put(BULKHEAD_RESULT, "accepted").build());
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public synchronized void reportCircuitOpen(long now) {
        if (circuitBreakerState != CircuitBreakerState.OPEN) {
            recordEndOfCircuitBreakerState(circuitBreakerState, now);
            circuitBreakerState = CircuitBreakerState.OPEN;
            ftCircuitbreakerOpenedTotal.add(1, methodAttribute);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public synchronized void reportCircuitHalfOpen(long now) {
        if (circuitBreakerState != CircuitBreakerState.HALF_OPEN) {
            recordEndOfCircuitBreakerState(circuitBreakerState, now);
            circuitBreakerState = CircuitBreakerState.HALF_OPEN;
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public synchronized void reportCircuitClosed(long now) {
        if (circuitBreakerState != CircuitBreakerState.CLOSED) {
            recordEndOfCircuitBreakerState(circuitBreakerState, now);
            circuitBreakerState = CircuitBreakerState.CLOSED;
        }
    }

    @Trivial
    private void recordEndOfCircuitBreakerState(CircuitBreakerState oldState, long now) {

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
    private void getCircuitBreakerAccumulatedTimes(ObservableLongMeasurement measurement) {
        long now = System.nanoTime();
        long closedNanosLocal = closedNanos;
        long halfOpenNanosLocal = halfOpenNanos;
        long openNanosLocal = openNanos;

        switch (circuitBreakerState) {
            case CLOSED:
                closedNanosLocal += (now - lastCircuitBreakerTransitionTime);
                break;
            case HALF_OPEN:
                halfOpenNanosLocal += (now - lastCircuitBreakerTransitionTime);
                break;
            case OPEN:
                openNanosLocal += (now - lastCircuitBreakerTransitionTime);
                break;
        }

        measurement.record(closedNanosLocal, Attributes.builder().putAll(methodAttribute).put(STATE, "closed").build());
        measurement.record(halfOpenNanosLocal, Attributes.builder().putAll(methodAttribute).put(STATE, "halfOpen").build());
        measurement.record(openNanosLocal, Attributes.builder().putAll(methodAttribute).put(STATE, "open").build());
    }

    @Trivial
    @Override
    public void reportQueueWaitTime(long queueWaitNanos) {
        if (ftBulkheadWaitingDuration != null) {
            double seconds = queueWaitNanos / 1_000_000_000d;
            ftBulkheadWaitingDuration.record(seconds, methodAttribute);
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
    private void getConcurrentExecutions(ObservableLongMeasurement measurement) {
        long executions = 0L;
        if (concurrentExecutionCountSupplier != null) {
            executions = concurrentExecutionCountSupplier.getAsLong();
        }
        measurement.record(executions, Attributes.builder().putAll(methodAttribute).build());
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void setBulkheadConcurentExecutionCountSupplier(LongSupplier concurrentExecutionCountSupplier) {
        this.concurrentExecutionCountSupplier = concurrentExecutionCountSupplier;
    }

    @Trivial
    private void getQueuePopulation(ObservableLongMeasurement measurement) {
        long pop = 0L;
        if (queuePopulationSupplier != null) {
            pop = queuePopulationSupplier.getAsLong();
        }
        measurement.record(pop, Attributes.builder().putAll(methodAttribute).build());
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
        if (ftBulkheadRunningDuration != null) {
            double seconds = executionTime / 1_000_000_000d;
            ftBulkheadRunningDuration.record(seconds, methodAttribute);
        }
    }

}
