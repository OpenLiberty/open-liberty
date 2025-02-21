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
import static org.junit.Assert.assertNull;
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
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;
import componenttest.annotation.SkipForRepeat;
import componenttest.rules.repeater.MicroProfileActions;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageEntryMetadata;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.SpanData;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

@SuppressWarnings("serial")
@WebServlet("/testB3MultiPropagation")
public class B3MultiPropagationTestServlet extends FATServlet {

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
    //This test is already covered in the MpTelemetry 2.0 TCK
    @SkipForRepeat({MicroProfileActions.MP70_EE11_ID, MicroProfileActions.MP70_EE10_ID, TelemetryActions.MP61_MPTEL20_ID, TelemetryActions.MP50_MPTEL20_ID, TelemetryActions.MP50_MPTEL20_JAVA8_ID, TelemetryActions.MP41_MPTEL20_ID,  TelemetryActions.MP14_MPTEL20_ID})
    public void testB3Propagation() throws URISyntaxException {
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

        // B3 does not support baggage, so baggage should not be propagated
        assertNull("baggage value propagated", serverSpan.getAttributes().get(BAGGAGE_VALUE_ATTR));
        assertNull("baggage metadata propagated", serverSpan.getAttributes().get(BAGGAGE_METADATA_ATTR));

        // Assert that the expected headers were used
        assertThat(serverSpan.getAttributes().get(PropagationHeaderEndpoint.PROPAGATION_HEADERS_ATTR),
                   containsInAnyOrder("X-B3-TraceId", "X-B3-SpanId", "X-B3-Sampled"));
    }
}