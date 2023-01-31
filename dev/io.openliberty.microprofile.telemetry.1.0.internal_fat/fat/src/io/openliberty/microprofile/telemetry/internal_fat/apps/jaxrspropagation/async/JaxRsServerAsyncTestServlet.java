/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.async;

import static io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.async.JaxRsServerAsyncTestEndpoint.BAGGAGE_VALUE_ATTR;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

@SuppressWarnings("serial")
@WebServlet("/jaxrsServerAsync")
public class JaxRsServerAsyncTestServlet extends FATServlet {

    private static final String TEST_VALUE = "test.value";

    @Inject
    private Tracer tracer;

    @Inject
    private InMemorySpanExporter spanExporter;

    @Inject
    private HttpServletRequest request;

    @Test
    public void testJaxRsServerAsync() {
        Span span = tracer.spanBuilder("test").startSpan();
        try (Scope scope = span.makeCurrent()) {
            // Add the test value to baggage
            Baggage baggage = Baggage.builder()
                            .put(JaxRsServerAsyncTestEndpoint.BAGGAGE_KEY, TEST_VALUE)
                            .build();
            baggage.makeCurrent();

            // Make the request to the test endpoint
            JaxRsServerAsyncTestEndpointClient client = RestClientBuilder.newBuilder()
                            .baseUri(JaxRsServerAsyncTestEndpoint.getBaseUri(request))
                            .build(JaxRsServerAsyncTestEndpointClient.class);
            String response = client.get();
            assertEquals("OK", response);
        } finally {
            span.end();
        }

        List<SpanData> spanData = spanExporter.getFinishedSpanItems(4);

        SpanData testSpan = spanData.get(0);
        SpanData clientSpan = spanData.get(1);
        SpanData serverSpan = spanData.get(2);
        SpanData subtaskSpan = spanData.get(3);

        // Assert correct parent-child links
        // Shows that propagation occurred
        assertEquals("client parent span id", testSpan.getSpanId(), clientSpan.getParentSpanId());
        assertEquals("server parent span id", clientSpan.getSpanId(), serverSpan.getParentSpanId());
        assertEquals("subtask parent span id", serverSpan.getSpanId(), subtaskSpan.getParentSpanId());

        // Assert baggage propagated on server span
        assertEquals(TEST_VALUE, serverSpan.getAttributes().get(BAGGAGE_VALUE_ATTR));
        // Assert baggage propagated on subtask span
        assertEquals(TEST_VALUE, subtaskSpan.getAttributes().get(BAGGAGE_VALUE_ATTR));

        // Assert that the server span finished after the subtask span
        // Even though the resource method returned quickly, the span should not end until the response is actually returned
        assertThat(serverSpan.getEndEpochNanos(), greaterThan(subtaskSpan.getEndEpochNanos()));
    }
}
