/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.faulttolerance30.internal.metrics30.integration;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;

import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorderProvider.AsyncType;

import io.openliberty.microprofile.faulttolerance30.internal.metrics.integration.AbstractMetricRecorder30Impl;
import io.openliberty.microprofile.faulttolerance30.internal.metrics.integration.Type;

import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

/**
 * Metrics recorder for FT 3.0+ with Metrics 3.0+
 */
public class MetricRecorder30Metrics30Impl extends AbstractMetricRecorder30Impl {

    private static final Map<Type, MetricType> metricTypeMapping;

    static {
        metricTypeMapping = new HashMap<>();
        metricTypeMapping.put(Type.COUNTER, MetricType.COUNTER);
        metricTypeMapping.put(Type.GAUGE, MetricType.GAUGE);
        metricTypeMapping.put(Type.HISTOGRAM, MetricType.HISTOGRAM);
    }

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
    public MetricRecorder30Metrics30Impl(String methodName, MetricRegistry registry, RetryPolicy retryPolicy, CircuitBreakerPolicy circuitBreakerPolicy,
                                         TimeoutPolicy timeoutPolicy, BulkheadPolicy bulkheadPolicy, FallbackPolicy fallbackPolicy, AsyncType isAsync) {
        super(methodName, registry, retryPolicy, circuitBreakerPolicy, timeoutPolicy, bulkheadPolicy, fallbackPolicy, isAsync);
    }

    /** {@inheritDoc} */
    @Override
    public Metadata metadata(String name, Type type, String unit) {
        return Metadata.builder().withName(name).withType(metricTypeMapping.get(type)).withUnit(unit).build();
    }

    /** {@inheritDoc} */
    @Override
    public Metadata metadata(String name, Type type) {
        return Metadata.builder().withName(name).withType(metricTypeMapping.get(type)).build();
    }

}
