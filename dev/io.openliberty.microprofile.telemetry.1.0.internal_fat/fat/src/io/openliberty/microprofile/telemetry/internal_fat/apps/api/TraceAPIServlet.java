/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import io.opentelemetry.api.trace.TracerProvider;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/testTrace")
@ApplicationScoped // Make this a bean so that there's one bean in the archive, otherwise CDI gets disabled and @Inject doesn't work
public class TraceAPIServlet extends FATServlet {

    @Inject
    private Tracer tracer;

    @Inject
    private OpenTelemetry openTelemetry;

    /**
     * Very simple test that we can use a SpanBuilder to start a Span and add an event to that
     * {@link SpanBuilder}
     * {@link Span}
     */
    @Test
    public void testSpanBuilder() {
        SpanBuilder builder = tracer.spanBuilder("span1");
        Span span1 = builder.startSpan();
        try {
            span1.addEvent("event1");
        } finally {
            span1.end();
        }
    }

    /**
     * Very simple test that we can create a SpanContext
     * {@link SpanContext}
     * {@link SpanId}
     * {@link TraceId}
     * {@link TraceStateBuilder}
     * {@link TraceState}
     * {@link TraceFlags}
     */
    @Test
    public void testSpanContext() {
        String traceId = TraceId.fromLongs(0, 123);
        String spanId = SpanId.fromLong(123);

        TraceStateBuilder traceStateBuilder = TraceState.builder();
        traceStateBuilder.put("myKey1", "myValue1");
        TraceState traceState = traceStateBuilder.build();

        TraceFlags traceFlags = TraceFlags.fromByte((byte) 1);

        SpanContext context = SpanContext.create(traceId, spanId, traceFlags, traceState);
        assertTrue("SpanContext not valid. TraceId=" + context.getTraceId() + ". SpanId=" + context.getSpanId() + ".", context.isValid()); //a valid context is one which has a non-zero TraceID and a non-zero SpanID
    }

    /**
     * Very simple test that we can get the spanId from the context
     * {@link SpanId}
     */
    @Test
    public void testSpanId() {
        SpanBuilder builder = tracer.spanBuilder("span2");
        Span span2 = builder.startSpan();
        try {
            SpanContext context = span2.getSpanContext();
            String spanId = context.getSpanId();
            assertEquals(16, spanId.length()); //spanID is a 16 character lowercase hex (base16) String.
            assertEquals(SpanId.fromLong(0), spanId); //ID of an invalid span is all zeros
            assertFalse(SpanId.isValid(spanId));
        } finally {
            span2.end();
        }
    }

    /**
     * Very simple test that we can set the SpanKind
     * {@link SpanKind}
     */
    @Test
    public void testSpanKind() {
        SpanBuilder builder = tracer.spanBuilder("span3").setSpanKind(SpanKind.PRODUCER);
        Span span3 = builder.startSpan();
        try {
            span3.addEvent("event3");
        } finally {
            span3.end();
        }
    }

    /**
     * Very simple test that we can set the StatusCode
     * {@link StatusCode}
     */
    @Test
    public void testStatusCode() {
        SpanBuilder builder = tracer.spanBuilder("span4");
        Span span4 = builder.startSpan();
        try {
            span4.addEvent("event4");
            span4.setStatus(StatusCode.ERROR, "This is an error message");
        } finally {
            span4.end();
        }
    }

    /**
     * Very simple test that we can use the TracerBuilder
     * {@link TracerProvider}
     * {@link TracerBuilder}
     * {@link Tracer}
     */
    @Test
    public void testTracerBuilder() {
        TracerProvider tracerProvider = openTelemetry.getTracerProvider();
        TracerBuilder tracerBuilder = tracerProvider.tracerBuilder("scope1");
        Tracer tracer2 = tracerBuilder.build();
        SpanBuilder builder2 = tracer2.spanBuilder("span5");
        Span span5 = builder2.startSpan();
        try {
            span5.addEvent("event5");
        } finally {
            span5.end();
        }
    }

    /**
     * Very simple test that we can NOT use GlobalOpenTelemetry
     * {@link GlobalOpenTelemetry}
     *
     * TODO: Currently GlobalOpenTelemetry manages to configure itself. This may not be what we want.
     * See https://github.com/OpenLiberty/open-liberty/issues/23923
     *
     */
    @Test
    @Ignore
    public void testGlobalOpenTelemetry() {
        OpenTelemetry globalOpenTelemetry = GlobalOpenTelemetry.get();
        assertNull("GlobalOpenTelemetry was not null: " + globalOpenTelemetry, globalOpenTelemetry);

        TracerProvider globalTracerProvider = GlobalOpenTelemetry.getTracerProvider();
        assertNull("GlobalTracerProvider was not null: " + globalTracerProvider, globalTracerProvider);
    }
}
