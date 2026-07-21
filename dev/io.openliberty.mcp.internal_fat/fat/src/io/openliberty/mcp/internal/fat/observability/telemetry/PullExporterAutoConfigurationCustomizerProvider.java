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

import java.util.Map;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;

public class PullExporterAutoConfigurationCustomizerProvider implements AutoConfigurationCustomizerProvider {
    public static InMemoryMetricReader exporter = new InMemoryMetricReader();

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        autoConfiguration.addMeterProviderCustomizer(this::registerMeterProvider);
        autoConfiguration.addPropertiesSupplier(this::disableExporters);
    }

    private SdkMeterProviderBuilder registerMeterProvider(SdkMeterProviderBuilder builder,
                                                          ConfigProperties properties) {
        builder.registerMetricReader(exporter);
        return builder;
    }

    private Map<String, String> disableExporters() {
        // Assume that if we're using the InMemoryMetricReader, we don't want any other exporters enabled
        return Map.of("otel.traces.exporter", "none",
                      "otel.metrics.exporter", "none",
                      "otel.logs.exporter", "none");
    }

}