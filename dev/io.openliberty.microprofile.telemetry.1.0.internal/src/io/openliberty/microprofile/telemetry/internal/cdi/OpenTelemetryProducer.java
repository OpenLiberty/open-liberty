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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.Config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
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
    @ApplicationScoped
    @Produces
    public OpenTelemetry getOpenTelemetry() {

        HashMap<String,String> telemetryProperties = getTelemetryProperties();

        SpanExporter exporter = getSpanExporter(telemetryProperties);

        Resource serviceNameResource = getServiceName(telemetryProperties);
        
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
    private Resource getServiceName(Map<String,String> oTelConfigs){
        String serviceName = "open-liberty-service";
        if(oTelConfigs.get("otel.service.name") != null || oTelConfigs.get("OTEL_SERVICE_NAME") != null){
            serviceName = oTelConfigs.get("otel.service.name");
        }
        return Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName));
    }
    @ApplicationScoped
    private SpanExporter getSpanExporter(Map<String,String> oTelConfigs) {

        //default endpoint
        String endpoint = "http://localhost:14250";
        String timeout = "10000";
        if(oTelConfigs.get("otel.traces.exporter") != null){
            if(oTelConfigs.get("otel.traces.exporter").equals("jaeger") || oTelConfigs.get("OTEL_TRACES_EXPORTER").equals("jaeger")){
                if(oTelConfigs.get("otel.exporter.jaeger.endpoint") != null){
                    endpoint = oTelConfigs.get("otel.exporter.jaeger.endpoint");
                }
                else if(oTelConfigs.get("OTEL_EXPORTER_JAEGER_ENDPOINT")!= null){
                    endpoint = oTelConfigs.get("OTEL_EXPORTER_JAEGER_ENDPOINT");
                }
                if(oTelConfigs.get("otel.exporter.jaeger.timeout") != null){
                    timeout = oTelConfigs.get("otel.exporter.jaeger.timeout");
                }
                else if(oTelConfigs.get("OTEL_EXPORTER_JAEGER_TIMEOUT")!= null){
                    timeout = oTelConfigs.get("OTEL_EXPORTER_JAEGER_TIMEOUT");
                }
                return JaegerGrpcSpanExporter.builder()
                                .setEndpoint(endpoint)
                                .setTimeout(Integer.valueOf(timeout),TimeUnit.MILLISECONDS)
                                .build();
            }
        }
        else{
            endpoint = "http://localhost:4317";
            if(oTelConfigs.get("OTEL_EXPORTER_OTLP_ENDPOINT") != null){
                endpoint = oTelConfigs.get("OTEL_EXPORTER_OTLP_ENDPOINT");
            }
            else if(oTelConfigs.get("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT") != null){
                endpoint = oTelConfigs.get("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT");
            }
            if(oTelConfigs.get("otel.exporter.otlp.endpoint") != null){
                endpoint = oTelConfigs.get("otel.exporter.otlp.endpoint");
            }
            else if(oTelConfigs.get("otel.exporter.otlp.traces.endpoint") != null){
                endpoint = oTelConfigs.get("otel.exporter.otlp.traces.endpoint");
            }
        }
            return OtlpGrpcSpanExporter.builder()
                            .setEndpoint(endpoint)
                            .setTimeout(Integer.valueOf(timeout),TimeUnit.MILLISECONDS)
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
        return openTelemetry.getTracer(instrumentationName);
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