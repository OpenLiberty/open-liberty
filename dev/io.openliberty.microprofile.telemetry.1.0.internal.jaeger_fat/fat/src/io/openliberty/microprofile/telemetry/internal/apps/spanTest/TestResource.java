/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.apps.spanTest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

/**
 * A Restful WS resource which can create spans in various ways which are useful for tests
 * <p>
 * Each method returns the current Trace ID so that tests can query the trace store and ensure the correct spans were created.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class TestResource {

    public static final String TEST_OPERATION_NAME = "TestOp";
    public static final String TEST_EVENT_NAME = "TestEvent";

    public static final AttributeKey<String> TEST_ATTRIBUTE_KEY = AttributeKey.stringKey("test_key_foo");
    public static final String TEST_ATTRIBUTE_VALUE = "test_value_bar";

    @Inject
    private Tracer tracer;

    @GET
    public String basic() {
        // There should be a span active since we've come in via RestfulWS
        Span span = Span.current();
        return span.getSpanContext().getTraceId();
    }

    @GET
    @Path("/eventAdded")
    public String eventAdded() {
        Span span = Span.current();
        span.addEvent(TEST_EVENT_NAME);
        return span.getSpanContext().getTraceId();
    }

    @GET
    @Path("/subspan")
    public String subspan() {
        Span span = Span.current();
        Span subspan = tracer.spanBuilder(TEST_OPERATION_NAME).startSpan();
        subspan.end();
        return span.getSpanContext().getTraceId();
    }

    @GET
    @Path("/exception")
    public String spanException() {
        Span span = Span.current();
        span.recordException(new RuntimeException());
        span.setStatus(StatusCode.ERROR);
        return span.getSpanContext().getTraceId();
    }

    @GET
    @Path("/attributeAdded")
    public String attributeAdded() {
        Span span = Span.current();
        span.setAttribute(TEST_ATTRIBUTE_KEY, TEST_ATTRIBUTE_VALUE);
        return span.getSpanContext().getTraceId();
    }
}
