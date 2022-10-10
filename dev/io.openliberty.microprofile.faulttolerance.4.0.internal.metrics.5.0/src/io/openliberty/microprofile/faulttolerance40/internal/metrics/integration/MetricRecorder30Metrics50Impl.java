/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.faulttolerance40.internal.metrics.integration;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;

import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorderProvider.AsyncType;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

import io.openliberty.microprofile.faulttolerance30.internal.metrics.integration.AbstractMetricRecorder30Impl;
import io.openliberty.microprofile.faulttolerance30.internal.metrics.integration.Type;

/**
 * Metrics recorder for FT 3.0+ with Metrics 5.0+
 */
public class MetricRecorder30Metrics50Impl extends AbstractMetricRecorder30Impl {

    /**
     * @param methodName
     * @param registry
     * @param retryPolicy
     * @param circuitBreakerPolicy
     * @param timeoutPolicy
     * @param bulkheadPolicy
     * @param fallbackPolicy
     * @param isAsync
     */
    public MetricRecorder30Metrics50Impl(String methodName, MetricRegistry registry, RetryPolicy retryPolicy, CircuitBreakerPolicy circuitBreakerPolicy,
                                         TimeoutPolicy timeoutPolicy, BulkheadPolicy bulkheadPolicy, FallbackPolicy fallbackPolicy, AsyncType isAsync) {
        super(methodName, registry, retryPolicy, circuitBreakerPolicy, timeoutPolicy, bulkheadPolicy, fallbackPolicy, isAsync);
    }

    /** {@inheritDoc} */
    @Override
    public Metadata metadata(String name, Type type, String unit) {
        return Metadata.builder().withName(name).withUnit(unit).build();
    }

    /** {@inheritDoc} */
    @Override
    public Metadata metadata(String name, Type type) {
        return Metadata.builder().withName(name).build();
    }

}
