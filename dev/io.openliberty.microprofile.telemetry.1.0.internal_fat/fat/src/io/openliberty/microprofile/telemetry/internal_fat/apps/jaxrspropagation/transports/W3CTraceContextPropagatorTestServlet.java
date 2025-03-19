/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

@SuppressWarnings("serial")
@WebServlet("/testw3cTraceContextPropagator")
public class W3CTraceContextPropagatorTestServlet extends FATServlet {

    private static final String TRACE_ID_BASE16 = "ff000000000000000000000000000041";
    private static final String SPAN_ID_BASE16 = "ff00000000000041";
    private static final String TRACE_PARENT = "traceparent";
    private static final TraceState TRACE_STATE = TraceState.builder().put("foo", "bar").put("bar", "baz").build();
    private static final String TRACEPARENT_HEADER_SAMPLED = "00-" + TRACE_ID_BASE16 + "-" + SPAN_ID_BASE16 + "-01";

    private static final TextMapSetter<Map<String, String>> setter = Map::put;

    private static final TextMapGetter<Map<String, String>> getter = new TextMapGetter<Map<String, String>>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    };
    private final TextMapPropagator w3cTraceContextPropagator = W3CTraceContextPropagator.getInstance();

    @Test
    void inject_Nothing() {
        Map<String, String> carrier = new LinkedHashMap<>();
        w3cTraceContextPropagator.inject(Context.current(), carrier, setter);
        assertEquals(carrier.size(), 0);
    }

    @Test
    public void inject_SampledContext() throws URISyntaxException {
        Map<String, String> carrier = new LinkedHashMap<>();
        Context context = withSpanContext(
                                          SpanContext.create(
                                                             TRACE_ID_BASE16, SPAN_ID_BASE16, TraceFlags.getSampled(), TraceState.getDefault()),
                                          Context.current());
        w3cTraceContextPropagator.inject(context, carrier, setter);
        System.out.println("context " + context);
        System.out.println("carrier " + carrier);
        assertThat(carrier.keySet(), contains(TRACE_PARENT));
        assertThat(carrier.get(TRACE_PARENT), containsString(TRACEPARENT_HEADER_SAMPLED));

    }

    @Test
    void inject_invalidContext() {
        Map<String, String> carrier = new LinkedHashMap<>();
        w3cTraceContextPropagator.inject(
                                         withSpanContext(
                                                         SpanContext.create(
                                                                            TraceId.getInvalid(),
                                                                            SpanId.getInvalid(),
                                                                            TraceFlags.getSampled(),
                                                                            TraceState.builder().put("foo", "bar").build()),
                                                         Context.current()),
                                         carrier,
                                         setter);
        assertEquals(carrier.size(), 0);
    }

    private static Context withSpanContext(SpanContext spanContext, Context context) {
        return context.with(Span.wrap(spanContext));
    }

    @Test
    void extract_SampledContext() {
        Map<String, String> carrier = new LinkedHashMap<>();
        carrier.put(TRACE_PARENT, TRACEPARENT_HEADER_SAMPLED);
        assertThat(
                   getSpanContext(w3cTraceContextPropagator.extract(Context.current(), carrier, getter)), equalTo(
                                                                                                                  SpanContext.createFromRemoteParent(
                                                                                                                                                     TRACE_ID_BASE16,
                                                                                                                                                     SPAN_ID_BASE16,
                                                                                                                                                     TraceFlags.getSampled(),
                                                                                                                                                     TraceState.getDefault())));
    }

    private static SpanContext getSpanContext(Context context) {
        return Span.fromContext(context).getSpanContext();
    }

}