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

package io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/opentelemetry")
@ApplicationScoped
public class OpenTelemetryServlet extends FATServlet {

    @Inject
    OpenTelemetry openTelemetry;

    Tracer tracer;

    private static final String SPAN_NAME = "MySpanName";
    private static final SpanContext spanContext = SpanContext.create(
                                                                      "00000000000000000000000000000061",
                                                                      "0000000000000061",
                                                                      TraceFlags.getDefault(),
                                                                      TraceState.getDefault());

    @PostConstruct
    public void postConstruct() {
        this.tracer = openTelemetry.getTracer("instrumentation-test", "1.0.0");
    }

    @Test
    void testSpanContextPropagationExplicitParent() {
        Span span = tracer
                        .spanBuilder(SPAN_NAME)
                        .setParent(Context.root().with(Span.wrap(spanContext)))
                        .startSpan();
        assertEquals(span.getSpanContext(), spanContext);
    }

    @Test
    public void tracerTest() {
        assertNotNull(tracer);
    }

    @Test
    public void testOpenTelemetryNotNull() {
        assertNotNull(openTelemetry);
    }

}
