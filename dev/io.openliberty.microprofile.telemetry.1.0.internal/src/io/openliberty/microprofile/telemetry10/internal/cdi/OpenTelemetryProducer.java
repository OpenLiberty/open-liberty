/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.telemetry10.internal.cdi;

import java.security.PrivilegedAction;
import java.util.HashMap;

import io.openliberty.microprofile.telemetry.internal.common.cdi.AbstractOpenTelemetryProducer;
import io.openliberty.microprofile.telemetry.internal.common.cdi.OpenTelemetryInfo;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * The main access point for acquiring OpenTelemetry objects, this class functions via CDI.
 */
public class OpenTelemetryProducer extends AbstractOpenTelemetryProducer {

    //CDI producer annotations are not inherited so we annotate methods on the concrete class
    //And delegate to the abstract.
    @Override
    protected PrivilegedAction<OpenTelemetry> getSDKBuilderPrivilegedAction(HashMap<String, String> telemetryProperties) {
        return (PrivilegedAction<OpenTelemetry>) () -> {
            return AutoConfiguredOpenTelemetrySdk.builder()
                            .addPropertiesCustomizer(x -> telemetryProperties) //Overrides OpenTelemetry's property order
                            .addResourceCustomizer(this::customizeResource)//Defaults service name to application name
                            .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                            .setResultAsGlobal(false)
                            .registerShutdownHook(false)
                            .build()
                            .getOpenTelemetrySdk();
        };
    }

    @Override
    @ApplicationScoped
    @Produces
    public OpenTelemetry getOpenTelemetry(OpenTelemetryInfo openTelemetryInfo) {
        return super.getOpenTelemetry(openTelemetryInfo);
    }

    @Override
    @Produces
    public Tracer getTracer(OpenTelemetry openTelemetry) {
        return super.getTracer(openTelemetry);
    }

    @Override
    @Produces
    @ApplicationScoped
    public Span getSpan() {
        return super.getSpan();
    }

    @Override
    @Produces
    @ApplicationScoped
    public Baggage getBaggage() {
        return super.getBaggage();
    }

    @Override
    @ApplicationScoped
    @Produces
    public OpenTelemetryInfo getOpenTelemetryInfo() {
        return super.getOpenTelemetryInfo();
    }

}
