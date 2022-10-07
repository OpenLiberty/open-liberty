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
package io.openliberty.microprofile.telemetry.internal.cdi;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.Config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

public class OpenTelemetryProducer {

    private static final String INSTRUMENTATION_NAME = "io.openliberty.microprofile.telemetry";
    private static final String ENV_ENABLE_PROPERTY = "OTEL_EXPERIMENTAL_SDK_ENABLED";
    private static final String CONFIG_ENABLE_PROPERTY = "otel.experimental.sdk.enabled";

    @Inject
    Config config;

    //See https://github.com/open-telemetry/opentelemetry-java-docs/blob/main/otlp/src/main/java/io/opentelemetry/example/otlp/ExampleConfiguration.java
    @ApplicationScoped
    @Produces
    public OpenTelemetry getOpenTelemetry() {
        HashMap<String, String> telemetryProperties = getTelemetryProperties();
        //Builds tracer provider if user has enabled tracing aspects with config properties
        if (checkEnabled(telemetryProperties)) {

            OpenTelemetry openTelemetry = AutoConfiguredOpenTelemetrySdk.builder()
                            .addPropertiesSupplier(() -> telemetryProperties)
                            .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                            .setResultAsGlobal(false)
                            .build()
                            .getOpenTelemetrySdk();

            if (openTelemetry == null) {
                openTelemetry = OpenTelemetry.noop();
            }

            return openTelemetry;
        }
        //By default, MicroProfile Telemetry tracing is off.
        //The absence of an installed SDK is a “no-op” API
        //Operations on a Tracer, or on Spans have no side effects and do nothing
        return OpenTelemetry.noop();

    }

    private boolean checkEnabled(Map<String, String> oTelConfigs) {
        //In order to enable any of the tracing aspects, the configuration otel.experimental.sdk.enabled=true must be specified in any of the config sources available via MicroProfile Config
        if (oTelConfigs.get(ENV_ENABLE_PROPERTY) != null) {
            if (oTelConfigs.get(ENV_ENABLE_PROPERTY).equals("true")) {
                return true;
            }
        } else if (oTelConfigs.get(CONFIG_ENABLE_PROPERTY) != null) {
            if (oTelConfigs.get(CONFIG_ENABLE_PROPERTY).equals("true")) {
                return true;
            }
        }
        return false;
    }

    private HashMap<String, String> getTelemetryProperties() {
        HashMap<String, String> telemetryProperties = new HashMap<>();
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith("otel.") || propertyName.startsWith("OTEL_")) {
                config.getOptionalValue(propertyName, String.class).ifPresent(
                                                                              value -> telemetryProperties.put(propertyName, value));
            }
        }
        return telemetryProperties;
    }

    @Produces
    public Tracer getTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(INSTRUMENTATION_NAME);
    }

    @Produces
    @RequestScoped
    public Span getSpan() {
        return Span.current();
    }

    @Produces
    @RequestScoped
    public Baggage getBaggage() {
        return Baggage.current();
    }
}
