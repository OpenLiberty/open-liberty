/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.graphql.internal.metrics.component;

import java.time.Duration;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;

import io.smallrye.graphql.api.Context;
import io.smallrye.graphql.cdi.config.ConfigKey;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.spi.EventingService;

public class MetricsService implements EventingService {

	private static final String PRE = "mp_graphql_";
    private static final String UNDERSCORE = "_";

    private final MetricRegistry metricRegistry;

    private final Map<Context, Long> startTimes = Collections.synchronizedMap(new IdentityHashMap<>());

    public MetricsService() {
        metricRegistry = MetricsServiceComponent.getSharedMetricRegistries().getOrCreate(MetricRegistry.Type.VENDOR.getName());
    }

    @Override
    public Operation createOperation(Operation operation) {
        final String name = getName(operation);
        final String description = getDescription(operation);

        Metadata metadata = Metadata.builder()
                .withName(name)
                .withType(MetricType.SIMPLE_TIMER)
                .withDescription(description)
                .build();
        metricRegistry.simpleTimer(metadata);
        return operation;
    }

    @Override
    public void beforeDataFetch(Context context) {
        startTimes.put(context, System.nanoTime());
    }

    @Override
    public void afterDataFetch(Context context) {
        Long startTime = startTimes.remove(context);
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            metricRegistry.simpleTimer(getName(context))
            .update(Duration.ofNanos(duration));
        }
    }

    @Override
    public String getConfigKey() {
        return ConfigKey.ENABLE_METRICS;
    }

    private String getName(Context context) {
        return PRE + context.getOperationType().toString() + UNDERSCORE + context.getFieldName();
    }

    private String getName(Operation operation) {
        return PRE + operation.getOperationType().toString() + UNDERSCORE + operation.getName();
    }

    private String getDescription(Operation operation) {
        return "Call statistics for the "
                + operation.getOperationType().toString().toLowerCase()
                + " '"
                + operation.getName()
                + "'";
    }
}