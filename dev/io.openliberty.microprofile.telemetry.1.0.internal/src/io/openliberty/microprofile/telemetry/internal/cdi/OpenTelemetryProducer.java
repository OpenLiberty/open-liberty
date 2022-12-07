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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.Config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

public class OpenTelemetryProducer {

    private static final String INSTRUMENTATION_NAME = "io.openliberty.microprofile.telemetry";
    private static final String ENV_DISABLE_PROPERTY = "OTEL_SDK_DISABLED";
    private static final String CONFIG_DISABLE_PROPERTY = "otel.sdk.disabled";

    @Inject
    Config config;

    //See https://github.com/open-telemetry/opentelemetry-java-docs/blob/main/otlp/src/main/java/io/opentelemetry/example/otlp/ExampleConfiguration.java
    @ApplicationScoped
    @Produces
    public OpenTelemetry getOpenTelemetry() {
        HashMap<String, String> telemetryProperties = getTelemetryProperties();
        //Builds tracer provider if user has enabled tracing aspects with config properties
        if (!checkDisabled(telemetryProperties)) {

            OpenTelemetry openTelemetry = AccessController.doPrivileged((PrivilegedAction<OpenTelemetry>) () -> {
                return AutoConfiguredOpenTelemetrySdk.builder()
                                .addPropertiesSupplier(() -> telemetryProperties)
                                .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                                .setResultAsGlobal(false)
                                .registerShutdownHook(false)
                                .build()
                                .getOpenTelemetrySdk();
            });

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

    public void disposeOpenTelemetry(@Disposes OpenTelemetry openTelemetry) {
        if (openTelemetry instanceof OpenTelemetrySdk) {
            OpenTelemetrySdk sdk = (OpenTelemetrySdk) openTelemetry;
            List<CompletableResultCode> results = new ArrayList<>();

            SdkTracerProvider tracerProvider = sdk.getSdkTracerProvider();
            if (tracerProvider != null) {
                results.add(tracerProvider.shutdown());
            }

            SdkMeterProvider meterProvider = sdk.getSdkMeterProvider();
            if (meterProvider != null) {
                results.add(meterProvider.shutdown());
            }

            SdkLoggerProvider loggerProvider = sdk.getSdkLoggerProvider();
            if (loggerProvider != null) {
                results.add(loggerProvider.shutdown());
            }

            CompletableResultCode.ofAll(results).join(10, TimeUnit.SECONDS);
        }
    }

    private boolean checkDisabled(Map<String, String> oTelConfigs) {
        //In order to enable any of the tracing aspects, the configuration otel.sdk.disabled=false must be specified in any of the configuration sources available via MicroProfile Config.
        if (oTelConfigs.get(ENV_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(ENV_DISABLE_PROPERTY));
        } else if (oTelConfigs.get(CONFIG_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(CONFIG_DISABLE_PROPERTY));
        }
        return true;
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
    @ApplicationScoped
    public Span getSpan() {
        return new SpanProxy();
    }

    @Produces
    @ApplicationScoped
    public Baggage getBaggage() {
        return new BaggageProxy();
    }
}
