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
package com.ibm.ws.microprofile.faulttolerance.utils;

import java.util.function.LongSupplier;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory;

/**
 * A {@link MetricRecorder} which does nothing
 * <p>
 * For use when metrics is not enabled.
 */
@Trivial
public final class DummyMetricRecorder implements MetricRecorder {

    private static final DummyMetricRecorder instance = new DummyMetricRecorder();

    public static DummyMetricRecorder get() {
        return instance;
    }

    // Use instance instead
    private DummyMetricRecorder() {}

    /** {@inheritDoc} */
    @Override
    public void incrementInvocationSuccessCount(FallbackOccurred fallbackOccurred) {}

    /** {@inheritDoc} */
    @Override
    public void incrementInvocationFailedCount(FallbackOccurred fallbackOccurred) {}

    /** {@inheritDoc} */
    @Override
    public void setBulkheadConcurentExecutionCountSupplier(LongSupplier concurrentExecutionCountSupplier) {}

    /** {@inheritDoc} */
    @Override
    public void recordBulkheadExecutionTime(long executionTime) {}

    /** {@inheritDoc} */
    @Override
    public void incrementRetryCalls(RetryResultCategory resultCategory, RetriesOccurred retriesOccurred) {}

    /** {@inheritDoc} */
    @Override
    public void incrementRetriesCount() {}

    /** {@inheritDoc} */
    @Override
    public void recordTimeoutExecutionTime(long executionNanos) {}

    /** {@inheritDoc} */
    @Override
    public void incrementTimeoutTrueCount() {}

    /** {@inheritDoc} */
    @Override
    public void incrementTimeoutFalseCount() {}

    /** {@inheritDoc} */
    @Override
    public void incrementCircuitBreakerCallsCircuitOpenCount() {}

    /** {@inheritDoc} */
    @Override
    public void incrementCircuitBreakerCallsSuccessCount() {}

    /** {@inheritDoc} */
    @Override
    public void incrementCircuitBreakerCallsFailureCount() {}

    /** {@inheritDoc} */
    @Override
    public void reportCircuitOpen() {}

    /** {@inheritDoc} */
    @Override
    public void reportCircuitHalfOpen() {}

    /** {@inheritDoc} */
    @Override
    public void reportCircuitClosed() {}

    /** {@inheritDoc} */
    @Override
    public void incrementBulkheadRejectedCount() {}

    /** {@inheritDoc} */
    @Override
    public void incrementBulkeadAcceptedCount() {}

    /** {@inheritDoc} */
    @Override
    public void reportQueueWaitTime(long queueWaitNanos) {}

    /** {@inheritDoc} */
    @Override
    public void setBulkheadQueuePopulationSupplier(LongSupplier concurrentExecutionCountSupplier) {}

}
