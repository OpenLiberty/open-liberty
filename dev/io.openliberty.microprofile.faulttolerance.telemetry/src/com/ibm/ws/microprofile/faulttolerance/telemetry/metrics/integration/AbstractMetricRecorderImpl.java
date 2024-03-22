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

import java.util.function.LongSupplier;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorderProvider.AsyncType;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;

/**
 *
 */
public abstract class AbstractMetricRecorderImpl implements MetricRecorder {

    private final LongCounter invocationCounter;
    private final LongCounter invocationFailedCounter;
    private final LongCounter retryCallsSuccessImmediateCounter;
    private final LongCounter retryCallsSuccessRetryCounter;
    private final LongCounter retryCallsFailureCounter;
    private final LongCounter retryRetriesCounter;
    private final LongHistogram timeoutDurationHistogram;
    private final LongCounter timeoutTrueCalls;
    private final LongCounter timeoutFalseCalls;
    private final LongCounter circuitBreakerCallsSuccessCounter;
    private final LongCounter circuitBreakerCallsFailureCounter;
    private final LongCounter circuitBreakerCallsOpenCounter;
    @SuppressWarnings("unused")
    private final ObservableLongGauge circuitBreakerOpenTime;
    @SuppressWarnings("unused")
    private final ObservableLongGauge circuitBreakerHalfOpenTime;
    @SuppressWarnings("unused")
    private final ObservableLongGauge circuitBreakerClosedTime;
    private final LongCounter circuitBreakerTimesOpenedCounter;
    @SuppressWarnings("unused")
    private final ObservableLongGauge bulkheadConcurrentExecutions;
    private final LongCounter bulkheadRejectionsCounter;
    private final LongCounter bulkheadAcceptedCounter;
    private final LongHistogram bulkheadExecutionDuration;
    @SuppressWarnings("unused")
    private final ObservableLongGauge bulkheadQueuePopulation;
    private final LongHistogram bulkheadQueueWaitTimeHistogram;
    private final LongCounter fallbackCalls;
    private LongSupplier concurrentExecutionCountSupplier = null;
    private LongSupplier queuePopulationSupplier = null;
    private long openNanos;
    private long halfOpenNanos;
    private long closedNanos;

    private enum CircuitBreakerState {
        CLOSED,
        HALF_OPEN,
        OPEN
    }

    //TODO confirm all descriptions are correct
    public AbstractMetricRecorderImpl(String metricPrefix, Meter meter, RetryPolicy retryPolicy, CircuitBreakerPolicy circuitBreakerPolicy,
                                      TimeoutPolicy timeoutPolicy,
                                      BulkheadPolicy bulkheadPolicy, FallbackPolicy fallbackPolicy, AsyncType isAsync) {
        if (retryPolicy != null || timeoutPolicy != null || circuitBreakerPolicy != null || bulkheadPolicy != null || fallbackPolicy != null) {
            invocationCounter = meter.counterBuilder(metricPrefix + ".invocations.total").setDescription("The number of invocations").build();
            invocationFailedCounter = meter.counterBuilder(metricPrefix + ".invocations.failed.total").setDescription("The number of invocations that failed").build();
        } else {
            invocationCounter = null;
            invocationFailedCounter = null;
        }

        if (retryPolicy != null) {
            retryCallsSuccessImmediateCounter = meter.counterBuilder(metricPrefix
                                                                     + ".retry.callsSucceededNotRetried.total").setDescription("The number of calls that succeeded without a retry").build();
            retryCallsSuccessRetryCounter = meter.counterBuilder(metricPrefix
                                                                 + ".retry.callsSucceededRetried.total").setDescription("The number of calls that succeeded with a retry").build();
            retryCallsFailureCounter = meter.counterBuilder(metricPrefix + ".retry.callsFailed.total").setDescription("The number of calls that failed after a retry").build();
            retryRetriesCounter = meter.counterBuilder(metricPrefix + ".retry.retries.total").setDescription("The number of calls that failed after a retry").build();
        } else {
            retryCallsSuccessImmediateCounter = null;
            retryCallsSuccessRetryCounter = null;
            retryCallsFailureCounter = null;
            retryRetriesCounter = null;
        }

        if (timeoutPolicy != null) {
            timeoutDurationHistogram = meter.histogramBuilder(metricPrefix
                                                              + ".timeout.executionDuration").setDescription("Execution Durations").ofLongs().setUnit("nanoseconds").build();
            timeoutTrueCalls = meter.counterBuilder(metricPrefix + ".timeout.callsTimedOut.total").setDescription("The number of calls that timed out").build();
            timeoutFalseCalls = meter.counterBuilder(metricPrefix + ".timeout.callsNotTimedOut.total").setDescription("The number of calls that did not time out").build();
        } else {
            timeoutDurationHistogram = null;
            timeoutTrueCalls = null;
            timeoutFalseCalls = null;
        }

        if (circuitBreakerPolicy != null) {
            circuitBreakerCallsFailureCounter = meter.counterBuilder(metricPrefix
                                                                     + ".circuitbreaker.callsFailed.total").setDescription("The number of calls that failed inside a circuit").build();
            circuitBreakerCallsSuccessCounter = meter.counterBuilder(metricPrefix
                                                                     + ".circuitbreaker.callsSucceeded.total").setDescription("The number of calls that succeeded inside a circuit").build();
            circuitBreakerCallsOpenCounter = meter.counterBuilder(metricPrefix
                                                                  + ".circuitbreaker.callsPrevented.total").setDescription("The number of calls prevented by a circuit break").build();
            circuitBreakerOpenTime = meter.gaugeBuilder(metricPrefix
                                                        + ".circuitbreaker.open.total").ofLongs().setUnit("nanoseconds").setDescription("The time a circuit was open").buildWithCallback(this::getCircuitBreakerAccumulatedOpen);
            circuitBreakerHalfOpenTime = meter.gaugeBuilder(metricPrefix
                                                            + ".circuitbreaker.halfOpen.total").ofLongs().setUnit("nanoseconds").setDescription("The time a circuit was half open").buildWithCallback(this::getCircuitBreakerAccumulatedHalfOpen);
            circuitBreakerClosedTime = meter.gaugeBuilder(metricPrefix
                                                          + ".circuitbreaker.closed.total").ofLongs().setUnit("nanoseconds").setDescription("The time a circuit was closed").buildWithCallback(this::getCircuitBreakerAccumulatedClosed);
            circuitBreakerTimesOpenedCounter = meter.counterBuilder(metricPrefix + ".circuitbreaker.opened.total").setDescription("The number of times a circuit opened").build();
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
            bulkheadConcurrentExecutions = meter.gaugeBuilder(metricPrefix
                                                              + ".bulkhead.concurrentExecutions").ofLongs().setUnit("none").setDescription("The number of concurrent executions").buildWithCallback(this::getConcurrentExecutions);
            bulkheadRejectionsCounter = meter.counterBuilder(metricPrefix + ".bulkhead.callsRejected.total").setDescription("The number of calls rejected by a bulkhead").build();
            bulkheadAcceptedCounter = meter.counterBuilder(metricPrefix + ".bulkhead.callsAccepted.total").setDescription("The number of calls accepted by a bulkhead").build();
            bulkheadExecutionDuration = meter.histogramBuilder(metricPrefix
                                                               + ".bulkhead.executionDuration").setDescription("Execution Duration").ofLongs().setUnit("nanoseconds").build();

        } else {
            bulkheadConcurrentExecutions = null;
            bulkheadRejectionsCounter = null;
            bulkheadAcceptedCounter = null;
            bulkheadExecutionDuration = null;
        }

        if (bulkheadPolicy != null && isAsync == AsyncType.ASYNC) {
            bulkheadQueuePopulation = meter.gaugeBuilder(metricPrefix
                                                         + ".bulkhead.waitingQueue.population").ofLongs().setUnit("nanoseconds").buildWithCallback(this::getQueuePopulation);
            bulkheadQueueWaitTimeHistogram = meter.histogramBuilder(metricPrefix + ".bulkhead.waiting.duration").ofLongs().setUnit("nanoseconds").build();

        } else {
            bulkheadQueuePopulation = null;
            bulkheadQueueWaitTimeHistogram = null;
        }

        if (fallbackPolicy != null) {
            fallbackCalls = meter.counterBuilder(metricPrefix + ".fallback.calls.total").build();
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
    public void incrementInvocationSuccessCount(FallbackOccurred fallbackOccurred) {
        if (invocationCounter != null) {
            invocationCounter.add(1);
        }
        if (fallbackOccurred == FallbackOccurred.WITH_FALLBACK) {
            fallbackCalls.add(1);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementInvocationFailedCount(FallbackOccurred fallbackOccurred) {
        if (invocationCounter != null) {
            invocationCounter.add(1);
        }
        if (invocationFailedCounter != null) {
            invocationFailedCounter.add(1);
        }
        if (fallbackOccurred == FallbackOccurred.WITH_FALLBACK) {
            fallbackCalls.add(1);
        }
    }

    @Override
    public void incrementRetryCalls(RetryResultCategory resultCategory, RetriesOccurred retriesOccurred) {
        if (retryCallsSuccessImmediateCounter != null) {
            switch (resultCategory) {
                case EXCEPTION_IN_ABORT_ON:
                case NO_EXCEPTION:
                case EXCEPTION_NOT_IN_RETRY_ON:
                    // success
                    if (retriesOccurred == RetriesOccurred.WITH_RETRIES) {
                        retryCallsSuccessRetryCounter.add(1);
                    } else {
                        retryCallsSuccessImmediateCounter.add(1);
                    }
                    break;

                case MAX_DURATION_REACHED:
                case MAX_RETRIES_REACHED:
                    // failure
                    retryCallsFailureCounter.add(1);
                    break;

                case EXCEPTION_IN_RETRY_ON:
                    // not valid for final retry attempt
                default:
                    // do nothing
            }
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementRetriesCount() {
        if (retryRetriesCounter != null) {
            retryRetriesCounter.add(1);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void recordTimeoutExecutionTime(long executionNanos) {
        if (timeoutDurationHistogram != null) {
            timeoutDurationHistogram.record(executionNanos);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementTimeoutTrueCount() {
        if (timeoutTrueCalls != null) {
            timeoutTrueCalls.add(1);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementTimeoutFalseCount() {
        if (timeoutFalseCalls != null) {
            timeoutFalseCalls.add(1);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementCircuitBreakerCallsCircuitOpenCount() {
        if (circuitBreakerCallsOpenCounter != null) {
            circuitBreakerCallsOpenCounter.add(1);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementCircuitBreakerCallsSuccessCount() {
        if (circuitBreakerCallsSuccessCounter != null) {
            circuitBreakerCallsSuccessCounter.add(1);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementCircuitBreakerCallsFailureCount() {
        if (circuitBreakerCallsFailureCounter != null) {
            circuitBreakerCallsFailureCounter.add(1);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementBulkheadRejectedCount() {
        if (bulkheadRejectionsCounter != null) {
            bulkheadRejectionsCounter.add(1);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementBulkeadAcceptedCount() {
        if (bulkheadAcceptedCounter != null) {
            bulkheadAcceptedCounter.add(1);
        }
    }

    @Trivial
    @Override
    public synchronized void reportCircuitOpen() {
        if (circuitBreakerState != CircuitBreakerState.OPEN) {
            recordEndOfCircuitBreakerState(circuitBreakerState);
            circuitBreakerState = CircuitBreakerState.OPEN;
            circuitBreakerTimesOpenedCounter.add(1);
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
            bulkheadQueueWaitTimeHistogram.record(queueWaitNanos);
        }
    }

    @Trivial
    private synchronized void getCircuitBreakerAccumulatedOpen(ObservableLongMeasurement measurement) {
        long computedNanos = openNanos;
        if (circuitBreakerState == CircuitBreakerState.OPEN) {
            computedNanos += System.nanoTime() - lastCircuitBreakerTransitionTime;
        }
        measurement.record(computedNanos);
    }

    @Trivial
    private synchronized void getCircuitBreakerAccumulatedHalfOpen(ObservableLongMeasurement measurement) {
        long computedNanos = halfOpenNanos;
        if (circuitBreakerState == CircuitBreakerState.HALF_OPEN) {
            computedNanos += System.nanoTime() - lastCircuitBreakerTransitionTime;
        }
        measurement.record(computedNanos);
    }

    @Trivial
    private synchronized void getCircuitBreakerAccumulatedClosed(ObservableLongMeasurement measurement) {
        long computedNanos = closedNanos;
        if (circuitBreakerState == CircuitBreakerState.CLOSED) {
            computedNanos += System.nanoTime() - lastCircuitBreakerTransitionTime;
        }
        measurement.record(computedNanos);
    }

    @Trivial
    private void getConcurrentExecutions(ObservableLongMeasurement measurement) {
        if (concurrentExecutionCountSupplier != null) {
            measurement.record(concurrentExecutionCountSupplier.getAsLong());
        } else {
            measurement.record(0L);
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void setBulkheadConcurentExecutionCountSupplier(LongSupplier concurrentExecutionCountSupplier) {
        this.concurrentExecutionCountSupplier = concurrentExecutionCountSupplier;
    }

    @Trivial
    private void getQueuePopulation(ObservableLongMeasurement measurement) {
        if (queuePopulationSupplier != null) {
            measurement.record(queuePopulationSupplier.getAsLong());
        } else {
            measurement.record(0L);
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
            bulkheadExecutionDuration.record(executionTime);
        }
    }

}
