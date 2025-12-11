/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package mpTelemetry;

import static org.junit.Assert.assertTrue;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

@SuppressWarnings("serial")
@WebServlet("/MpTelemetryServlet")
@ApplicationScoped
public class MpTelemetryServlet extends FATServlet {

    @Inject
    Tracer tracer;

    @Inject
    Span span;

    @Inject
    OpenTelemetry openTelemetry;

    private static final String INVALID_SPAN_ID = "0000000000000000";

    //Tests otel.service.name is service2 instead of service1
    @Test
    public void testServiceNameConfig() {
        Tracer tracer = openTelemetry.getTracer("config-test", "1.0.0");
        Span span = tracer.spanBuilder("testSpan").startSpan();
        span.end();
        assertTrue("Service name was not updated", openTelemetry.toString().contains("service.name=\"service2\""));
    }

    //Tests otel.sdk.disabled is false
    @Test
    public void testSDKDisabledConfig() {
        Tracer tracer = openTelemetry.getTracer("config-test", "1.0.0");
        Span span = tracer.spanBuilder("testSpan").startSpan();
        assertTrue("Span ID shouldn't be equal", !span.getSpanContext().getSpanId().equals(INVALID_SPAN_ID));
        span.end();
    }

}