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
import java.util.function.Supplier;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.enterprise.context.ApplicationScoped;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.Config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;

public class OpenTelemetryProducer {

    private String instrumentationName = "io.openliberty.microprofile.telemetry";

    @Inject
    Config config;

    //See https://github.com/open-telemetry/opentelemetry-java-docs/blob/main/otlp/src/main/java/io/opentelemetry/example/otlp/ExampleConfiguration.java
    @Produces
    public OpenTelemetry getOpenTelemetry() {

        SpanExporter exporter = getSpanExporter(getTelemetryProperties());

        Resource serviceNameResource =
            Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "open-liberty"));
        
        SdkTracerProvider tracerProvider =
            SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .setResource(Resource.getDefault().merge(serviceNameResource))
                .build();

        OpenTelemetrySdk openTelemetry =
            OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));

        return openTelemetry;
    }

    @ApplicationScoped
    private SpanExporter getSpanExporter(Map<String,String> oTelConfigs) {
        /*if(oTelConfigs.get("otel.traces.exporter").equals("jaeger")){
            return JaegerGrpcSpanExporter.builder()
                        .setEndpoint("http://localhost:14250")
                        .build();
        }
        else if(oTelConfigs.get("otel.traces.exporter").equals("zipkin")){
           return ZipkinSpanExporter.builder()
                        .setEndpoint("http://localhost:9411/api/v2/spans")
                        .build();
        }*/
        return JaegerGrpcSpanExporter.builder()
                            .setEndpoint("http://localhost:14250")
                            .build();
    }

    private HashMap<String,String> getTelemetryProperties(){
        HashMap<String,String> telemetryProperties = new HashMap<>();
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
        return CDI.current().select(OpenTelemetry.class).get().getTracer(instrumentationName);
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