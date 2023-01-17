/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import componenttest.app.FATServlet;

import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/ServiceNameServlet")
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
    @Test
    public void testServiceNameConfig() {
        Span span = tracer.spanBuilder("span").startSpan();
        span.end();

        SpanData spanData = exporter.getFinishedSpanItems(1).get(0);
        System.out.println(spanData.toString());
        // Attributes added by TestResourceProvider should have been merged into the default resource
        Resource resource = spanData.getResource();
        assertThat(resource.getAttribute(SERVICE_NAME_KEY), equalTo(APP_NAME));
    }

}