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
import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasKind;
import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.isSpan;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.function.Function;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
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
@WebServlet("/testJaxrsServerAsync")
public class JaxRsServerAsyncTestServlet extends FATServlet {

    private static final String TEST_VALUE = "test.value";

    @Inject
    private Tracer tracer;

    @Inject
    private InMemorySpanExporter spanExporter;

    @Inject
    private HttpServletRequest request;

    @Inject
    private TestSpans testSpans;

    @Test
    public void testJaxRsServerAsyncCompletionStage() {
        doAsyncTest(JaxRsServerAsyncTestEndpointClient::getCompletionStage);
    }

    @Test
    public void testJaxRsServerAsyncSuspend() {
        doAsyncTest(JaxRsServerAsyncTestEndpointClient::getSuspend);
    }

    private void doAsyncTest(Function<JaxRsServerAsyncTestEndpointClient, String> requestFunction) {
        Span span = testSpans.withTestSpan(() -> {
            Baggage baggage = Baggage.builder()
                            .put(JaxRsServerAsyncTestEndpoint.BAGGAGE_KEY, TEST_VALUE)
                            .build();

            try (Scope s = baggage.makeCurrent()) {
                // Make the request to the test endpoint
                JaxRsServerAsyncTestEndpointClient client = RestClientBuilder.newBuilder()
                                .baseUri(JaxRsServerAsyncTestEndpoint.getBaseUri(request))
                                .build(JaxRsServerAsyncTestEndpointClient.class);
                String response = requestFunction.apply(client);
                assertEquals("OK", response);
            }
        });

        List<SpanData> spanData = spanExporter.getFinishedSpanItems(4, span.getSpanContext().getTraceId());

        // Assert correct parent-child links
        // Shows that propagation occurred
        TestSpans.assertLinearParentage(spanData);

        SpanData testSpan = spanData.get(0);
        SpanData clientSpan = spanData.get(1);
        SpanData serverSpan = spanData.get(2);
        SpanData subtaskSpan = spanData.get(3);

        assertThat(testSpan, hasKind(INTERNAL));
        assertThat(clientSpan, hasKind(CLIENT));

        // Assert baggage propagated on server span
        assertThat(serverSpan, isSpan()
                        .withKind(SERVER)
                        .withAttribute(BAGGAGE_VALUE_ATTR, TEST_VALUE));
        // Assert baggage propagated on subtask span
        assertThat(subtaskSpan, isSpan()
                        .withKind(INTERNAL)
                        .withAttribute(BAGGAGE_VALUE_ATTR, TEST_VALUE));

        // Assert that the server span finished after the subtask span
        // Even though the resource method returned quickly, the span should not end until the response is actually returned
        assertThat("Server span should finish after subtask span", serverSpan.getEndEpochNanos(), greaterThan(subtaskSpan.getEndEpochNanos()));
    }
}
