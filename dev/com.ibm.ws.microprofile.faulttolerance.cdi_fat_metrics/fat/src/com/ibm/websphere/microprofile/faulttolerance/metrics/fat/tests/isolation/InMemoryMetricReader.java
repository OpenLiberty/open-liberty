/*
 *******************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.isolation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;

@ApplicationScoped
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

    //For this test we don't need more than this
    public boolean areThereNoMetrics() {
        return collectionRegistration.collectAllMetrics().stream()
                        .filter(
                                metric -> !metric.getName().equals("queueSize")) //Filter out a metric that's always present
                        .filter(metric -> !metric.getName().startsWith("jvm")) //We are testing ft metrics, ignore JVM metrics
                        .count() == 0;
    }
}
