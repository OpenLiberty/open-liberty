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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.config.Config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;

public class OpenTelemetryProducer {

    private static final String INSTRUMENTATION_NAME = "io.openliberty.microprofile.telemetry";
    private static final String CONFIG_EXPORTER_PROPERTY = "otel.exporter.";
    private static final String ENV_EXPORTER_PROPERTY = "OTEL_EXPORTER_";
    private static final String ENV_TRACE_EXPORTER_PROPERTY = "OTEL_TRACES_EXPORTER";
    private static final String CONFIG_TRACE_EXPORTER_PROPERTY = "otel.traces.exporter";
    private static final String ENV_JAEGER_TIMEOUT_PROPERTY = "JAEGER_TIMEOUT";
    private static final String CONFIG_JAEGER_TIMEOUT_PROPERTY = "jaeger.timeout";

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
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .setResource(Resource.getDefault().merge(serviceNameResource))
                .build();

        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build();

        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));
        return openTelemetry;
    }

    private Resource getServiceName(Map<String,String> oTelConfigs){
        String serviceName = "open-liberty-service";

        if(oTelConfigs.get("otel.service.name") != null || oTelConfigs.get("OTEL_SERVICE_NAME") != null){
            serviceName = oTelConfigs.get("otel.service.name");
        }
        return Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName));
    }
    
    @ApplicationScoped
    private SpanExporter getSpanExporter(Map<String,String> oTelConfigs) {

        //default otlp endpoint
        String endpoint = "http://localhost:4317";
        String timeout = "10000";

        //Environment properties take priority
        if(oTelConfigs.get(ENV_TRACE_EXPORTER_PROPERTY) != null && oTelConfigs.get(ENV_TRACE_EXPORTER_PROPERTY).equals("jaeger") || oTelConfigs.get(CONFIG_TRACE_EXPORTER_PROPERTY) != null && oTelConfigs.get(CONFIG_TRACE_EXPORTER_PROPERTY).equals("jaeger")){
            //default jaeger endpoint
            endpoint = "http://localhost:14250";
            if(oTelConfigs.get(ENV_EXPORTER_PROPERTY + "JAEGER_ENDPOINT")!= null){
                    endpoint = oTelConfigs.get(ENV_EXPORTER_PROPERTY + "JAEGER_ENDPOINT");
                }
                else if(oTelConfigs.get(CONFIG_EXPORTER_PROPERTY + "jaeger.endpoint") != null){
                    endpoint = oTelConfigs.get(CONFIG_EXPORTER_PROPERTY + "jaeger.endpoint");
                }
                if(oTelConfigs.get(ENV_EXPORTER_PROPERTY + ENV_JAEGER_TIMEOUT_PROPERTY)!= null){
                    timeout = oTelConfigs.get(ENV_EXPORTER_PROPERTY + ENV_JAEGER_TIMEOUT_PROPERTY);
                }
                else if(oTelConfigs.get(CONFIG_EXPORTER_PROPERTY + CONFIG_JAEGER_TIMEOUT_PROPERTY) != null){
                    timeout = oTelConfigs.get(CONFIG_EXPORTER_PROPERTY + CONFIG_JAEGER_TIMEOUT_PROPERTY);
                }
                return JaegerGrpcSpanExporter.builder()
                            .setEndpoint(endpoint)
                            .setTimeout(Integer.valueOf(timeout),TimeUnit.MILLISECONDS)
                            .build();
        }
        if(oTelConfigs.get(ENV_TRACE_EXPORTER_PROPERTY) != null && oTelConfigs.get(ENV_TRACE_EXPORTER_PROPERTY).equals("zipkin") || oTelConfigs.get(CONFIG_TRACE_EXPORTER_PROPERTY) != null && oTelConfigs.get(CONFIG_TRACE_EXPORTER_PROPERTY).equals("zipkin")){
            //default zipkin endpoint
            endpoint = "http://localhost:9411/api/v2/spans";
            if(oTelConfigs.get(ENV_EXPORTER_PROPERTY + "ZIPKIN_ENDPOINT")!= null){
                    endpoint = oTelConfigs.get(ENV_EXPORTER_PROPERTY + "ZIPKIN_ENDPOINT");
                }
            else if(oTelConfigs.get(CONFIG_EXPORTER_PROPERTY + "zipkin.endpoint") != null){
                    endpoint = oTelConfigs.get(CONFIG_EXPORTER_PROPERTY + "zipkin.endpoint");
            }
                return ZipkinSpanExporter.builder()
                            .setEndpoint(endpoint)
                            .build();
        }
        else{
            if(oTelConfigs.get(ENV_EXPORTER_PROPERTY + "OTLP_ENDPOINT") != null){
                endpoint = oTelConfigs.get(ENV_EXPORTER_PROPERTY + "OTLP_ENDPOINT");
            }
            else if(oTelConfigs.get(ENV_EXPORTER_PROPERTY + "OTLP_TRACES_ENDPOINT") != null){
                endpoint = oTelConfigs.get(ENV_EXPORTER_PROPERTY + "OTLP_TRACES_ENDPOINT");
            }
            if(oTelConfigs.get(CONFIG_EXPORTER_PROPERTY + "otlp.endpoint") != null){
                endpoint = oTelConfigs.get(CONFIG_EXPORTER_PROPERTY + "otlp.endpoint");
            }
            else if(oTelConfigs.get(CONFIG_EXPORTER_PROPERTY + "otlp.traces.endpoint") != null){
                endpoint = oTelConfigs.get(CONFIG_EXPORTER_PROPERTY + "otlp.traces.endpoint");
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
