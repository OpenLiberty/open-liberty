/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.microprofile.faulttolerance.cdi;

import java.util.List;
import java.util.function.LongSupplier;

import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory;

public class CombinedMetricRecorderProxy implements MetricRecorder {

    private final List<MetricRecorder> delegates;

    public CombinedMetricRecorderProxy(List<MetricRecorder> delegates) {
        this.delegates = delegates;
    }

    @Override
    public void incrementInvocationSuccessCount(FallbackOccurred fallbackOccurred) {
        delegates.stream().forEach(deligate -> deligate.incrementInvocationSuccessCount(fallbackOccurred));
    }

    @Override
    public void incrementInvocationFailedCount(FallbackOccurred fallbackOccurred) {
        delegates.stream().forEach(deligate -> deligate.incrementInvocationFailedCount(fallbackOccurred));
    }

    @Override
    public void incrementRetryCalls(RetryResultCategory resultCategory, RetriesOccurred retriesOccurred) {
        delegates.stream().forEach(deligate -> deligate.incrementRetryCalls(resultCategory, retriesOccurred));
    }

    @Override
    public void incrementRetriesCount() {
        delegates.stream().forEach(deligate -> deligate.incrementRetriesCount());
    }

    @Override
    public void recordTimeoutExecutionTime(long executionNanos) {
        delegates.stream().forEach(deligate -> deligate.recordTimeoutExecutionTime(executionNanos));
    }

    @Override
    public void incrementTimeoutTrueCount() {
        delegates.stream().forEach(deligate -> deligate.incrementTimeoutTrueCount());
    }

    @Override
    public void incrementTimeoutFalseCount() {
        delegates.stream().forEach(deligate -> deligate.incrementTimeoutFalseCount());
    }

    @Override
    public void incrementCircuitBreakerCallsCircuitOpenCount() {
        delegates.stream().forEach(deligate -> deligate.incrementCircuitBreakerCallsCircuitOpenCount());
    }

    @Override
    public void incrementCircuitBreakerCallsSuccessCount() {
        delegates.stream().forEach(deligate -> deligate.incrementCircuitBreakerCallsSuccessCount());
    }

    @Override
    public void incrementCircuitBreakerCallsFailureCount() {
        delegates.stream().forEach(deligate -> deligate.incrementCircuitBreakerCallsFailureCount());
    }

    @Override
    public void reportCircuitOpen(long now) {
        delegates.stream().forEach(deligate -> deligate.reportCircuitOpen(now));
    }

    @Override
    public void reportCircuitHalfOpen(long now) {
        delegates.stream().forEach(deligate -> deligate.reportCircuitHalfOpen(now));
    }

    @Override
    public void reportCircuitClosed(long now) {
        delegates.stream().forEach(deligate -> deligate.reportCircuitClosed(now));
    }

    @Override
    public void incrementBulkheadRejectedCount() {
        delegates.stream().forEach(deligate -> deligate.incrementBulkheadRejectedCount());
    }

    @Override
    public void incrementBulkeadAcceptedCount() {
        delegates.stream().forEach(deligate -> deligate.incrementBulkeadAcceptedCount());
    }

    @Override
    public void reportQueueWaitTime(long queueWaitNanos) {
        delegates.stream().forEach(deligate -> deligate.reportQueueWaitTime(queueWaitNanos));
    }

    @Override
    public void setBulkheadConcurentExecutionCountSupplier(LongSupplier concurrentExecutionCountSupplier) {
        delegates.stream().forEach(deligate -> deligate.setBulkheadConcurentExecutionCountSupplier(concurrentExecutionCountSupplier));
    }

    @Override
    public void setBulkheadQueuePopulationSupplier(LongSupplier concurrentExecutionCountSupplier) {
        delegates.stream().forEach(deligate -> deligate.setBulkheadQueuePopulationSupplier(concurrentExecutionCountSupplier));
    }

    @Override
    public void recordBulkheadExecutionTime(long executionTime) {
        delegates.stream().forEach(deligate -> deligate.recordBulkheadExecutionTime(executionTime));
    }

}
