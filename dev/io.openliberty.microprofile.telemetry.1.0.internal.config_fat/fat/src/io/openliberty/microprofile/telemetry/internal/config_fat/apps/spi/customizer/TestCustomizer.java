/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.config_fat.apps.spi.customizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;

public class TestCustomizer implements AutoConfigurationCustomizerProvider {

    public static final AttributeKey<String> TEST_KEY = AttributeKey.stringKey("test-key");
    public static final String TEST_VALUE = "test-value";

    public static final List<String> loggedEvents = new ArrayList<>();

    /** {@inheritDoc} */
    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        // Do an actual customization of the resource
        autoConfiguration.addResourceCustomizer((r, c) -> r.toBuilder().put(TEST_KEY, TEST_VALUE).build());

        // Just check that we can add the other customizers that relate to tracing and that they get called
        // Use actual methods so that we know we're really loading every class and not missing some due to generic erasure
        autoConfiguration.addPropagatorCustomizer(this::customizePropagator);
        autoConfiguration.addPropertiesCustomizer(this::customizeProperties);
        autoConfiguration.addSamplerCustomizer(this::customizeSampler);
        autoConfiguration.addSpanExporterCustomizer(this::customizeExporter);
        autoConfiguration.addTracerProviderCustomizer(this::customizeTracer);
    }

    private TextMapPropagator customizePropagator(TextMapPropagator propagator, ConfigProperties config) {
        loggedEvents.add("propagator");
        return propagator;
    }

    private Map<String, String> customizeProperties(ConfigProperties config) {
        loggedEvents.add("properties");
        return Collections.emptyMap();
    }

    private Sampler customizeSampler(Sampler sampler, ConfigProperties config) {
        loggedEvents.add("sampler");
        return sampler;
    }

    private SpanExporter customizeExporter(SpanExporter exporter, ConfigProperties config) {
        loggedEvents.add("exporter");
        return exporter;
    }

    private SdkTracerProviderBuilder customizeTracer(SdkTracerProviderBuilder tracerBuilder, ConfigProperties config) {
        loggedEvents.add("tracer");
        return tracerBuilder;
    }

}
