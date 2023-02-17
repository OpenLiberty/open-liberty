/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.transports;

import static io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.common.PropagationHeaderEndpoint.BAGGAGE_KEY;
import static io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.common.PropagationHeaderEndpoint.BAGGAGE_METADATA_ATTR;
import static io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.common.PropagationHeaderEndpoint.BAGGAGE_VALUE_ATTR;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.common.PropagationHeaderClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.common.PropagationHeaderEndpoint;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageEntryMetadata;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

@SuppressWarnings("serial")
@WebServlet("/w3cTraceBaggagePropagation")
public class W3CTraceBaggagePropagationTestServlet extends FATServlet {

    private static final String BAGGAGE_VALUE = "test.baggage.value";
    private static final String BAGGAGE_METADATA = "test.baggage.metadata";

    @Inject
    private Tracer tracer;

    @Inject
    private InMemorySpanExporter spanExporter;

    @Inject
    private HttpServletRequest request;

    @Inject
    private TestSpans testSpans;

    @Test
    public void testW3cTraceBaggagePropagation() throws URISyntaxException {
        Span span = testSpans.withTestSpan(() -> {
            Baggage baggage = Baggage.builder().put(BAGGAGE_KEY, BAGGAGE_VALUE, BaggageEntryMetadata.create(BAGGAGE_METADATA)).build();
            try (Scope s = baggage.makeCurrent()) {
                PropagationHeaderClient client = RestClientBuilder.newBuilder().baseUri(PropagationHeaderEndpoint.getBaseUri(request)).build(PropagationHeaderClient.class);
                client.get();
            }
        });

        List<SpanData> spanData = spanExporter.getFinishedSpanItems(3, span);

        // Assert correct parent-child links
        // Shows that propagation occurred
        TestSpans.assertLinearParentage(spanData);

        SpanData serverSpan = spanData.get(2);

        // Assert baggage is propagated
        assertEquals("baggage value propagated", BAGGAGE_VALUE, serverSpan.getAttributes().get(BAGGAGE_VALUE_ATTR));
        assertEquals("baggage metadata propagated", BAGGAGE_METADATA, serverSpan.getAttributes().get(BAGGAGE_METADATA_ATTR));

        // Assert that the expected headers were used
        assertThat(serverSpan.getAttributes().get(PropagationHeaderEndpoint.PROPAGATION_HEADERS_ATTR), containsInAnyOrder("traceparent", "baggage"));
    }
}