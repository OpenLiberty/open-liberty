/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.microprofile.telemetry.internal.config_fat.apps.telemetry;

import static io.openliberty.microprofile.telemetry.internal.config_fat.common.SpanDataMatcher.hasResourceAttribute;
import static org.junit.Assert.assertThat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal.config_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.data.SpanData;

@SuppressWarnings("serial")
@WebServlet("/testServiceName")
@ApplicationScoped
public class ServiceNameServlet extends FATServlet {

    @Inject
    Tracer tracer;

    @Inject
    Span span;

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    private InMemorySpanExporter exporter;

    public static final AttributeKey<String> SERVICE_NAME_KEY = AttributeKey.stringKey("service.name");
    public static final String APP_NAME = "TelemetryApp";

    //Tests if otel.service.name is set to the application name by default
    //On otel 2.0+ this will get the application name because otel is only set
    //to sdk disabled false in the app
    @Test
    public void testServiceNameConfig() {
        Span span = tracer.spanBuilder("span").startSpan();
        span.end();

        SpanData spanData = exporter.getFinishedSpanItems(1, span).get(0);
        System.out.println(spanData.toString());
        // Attributes added by TestResourceProvider should have been merged into the default resource
        assertThat(spanData, hasResourceAttribute(SERVICE_NAME_KEY, APP_NAME));
    }

}