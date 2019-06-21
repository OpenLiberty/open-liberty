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
package com.ibm.ws.microprofile.faulttolerance.metrics_11;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;

import com.ibm.ws.microprofile.faulttolerance.metrics.integration.AbstractMetricRecorderImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorderProvider.AsyncType;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

/**
 * Records metrics using MP Metrics API 1.1
 */
public class MetricRecorderImpl extends AbstractMetricRecorderImpl {

    /**
     * @param metricPrefix
     * @param registry
     * @param retryPolicy
     * @param circuitBreakerPolicy
     * @param timeoutPolicy
     * @param bulkheadPolicy
     * @param fallbackPolicy
     * @param isAsync
     */
    public MetricRecorderImpl(String metricPrefix, MetricRegistry registry, RetryPolicy retryPolicy, CircuitBreakerPolicy circuitBreakerPolicy, TimeoutPolicy timeoutPolicy,
                              BulkheadPolicy bulkheadPolicy, FallbackPolicy fallbackPolicy, AsyncType isAsync) {
        super(metricPrefix, registry, retryPolicy, circuitBreakerPolicy, timeoutPolicy, bulkheadPolicy, fallbackPolicy, isAsync, MetricRecorderImpl::createMD);
    }

    private static Metadata createMD(String metaDataName, MetricType metaDataMetricType, String metadataUnit) {
        Metadata metaData = new Metadata(metaDataName, metaDataMetricType, metadataUnit);
        return metaData;
    }

}
