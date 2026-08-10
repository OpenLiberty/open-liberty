/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.observability.telemetry;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import jakarta.enterprise.inject.spi.CDI;

public class InMemoryMetricReader implements MetricReader {

    private CollectionRegistration collectionRegistration;
    boolean isShutdown = false;

    public static InMemoryMetricReader current() {
        return CDI.current().select(InMemoryMetricReader.class).get();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return AggregationTemporality.CUMULATIVE;
    }

    @Override
    public void register(CollectionRegistration registration) {
        if (isShutdown) {
            throw new IllegalStateException("InMemoryMetricReader has been shutdown");
        }

        collectionRegistration = registration;
    }

    @Override
    public CompletableResultCode forceFlush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        collectionRegistration = null;
        isShutdown = true;
        return CompletableResultCode.ofSuccess();
    }

    public List<String> getMetricNames() {
        return collectionRegistration.collectAllMetrics().stream().map(metric -> metric.getName()).collect(toList());

    }

    public Collection<MetricData> getMcpMetricData() {
        Collection<MetricData> allMetrics = collectionRegistration.collectAllMetrics();
        
        Collection<MetricData> mcpMetrics = allMetrics.stream()
                                     .filter(metric -> metric.getName().contains("mcp")) //We are testing mcp metrics, ignore JVM, HTTP, etc metrics
                                     .collect(toList());
        
        return mcpMetrics;
    }
}
