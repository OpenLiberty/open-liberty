/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat.apps.spi.propagator;

import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasAttribute;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.rules.repeater.MicroProfileActions;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.SpanData;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

/**
 * Test that a custom Propagator can be provided.
 */
@SuppressWarnings("serial")
@WebServlet("/testPropagator")
public class PropagatorTestServlet extends FATServlet {

    public static final AttributeKey<String> TEST_KEY = AttributeKey.stringKey("test-key");
    public static final String TEST_VALUE = "test-value";

    @Inject
    private HttpServletRequest request;

    @Inject
    private InMemorySpanExporter exporter;

    @Inject
    private Baggage baggage;

    @Inject
    private TestSpans testSpans;

    @Test
    @SkipForRepeat({ MicroProfileActions.MP70_EE11_ID, MicroProfileActions.MP70_EE10_ID, TelemetryActions.MP61_MPTEL20_ID, TelemetryActions.MP50_MPTEL20_ID, TelemetryActions.MP50_MPTEL20_JAVA8_ID, TelemetryActions.MP41_MPTEL20_ID,
                     TelemetryActions.MP14_MPTEL20_ID })
    public void testPropagator() {
        Span span = testSpans.withTestSpan(() -> {
            // Add a key to the baggage that we will look for later
            try (Scope s = baggage.toBuilder().put(TEST_KEY.getKey(), TEST_VALUE).build().makeCurrent()) {
                // Call PropagatorTarget (below)
                String result = ClientBuilder.newClient().target(getTargetURI()).request().get(String.class);
                assertEquals("OK", result);
            }
        });

        // Expect three spans (test, client and server)
        List<SpanData> spans = exporter.getFinishedSpanItems(3, span);

        SpanData server = spans.get(2);

        // Check that baggage propagation worked by checking that the baggage entry was copied into a span attribute
        assertThat(server, hasAttribute(TEST_KEY, TEST_VALUE));

        // Check that trace context propagation worked by checking that the parent was set correctly
        TestSpans.assertLinearParentage(spans);
    }

    @ApplicationPath("/")
    @Path("/target")
    public static class PropagatorTarget extends Application {

        @Inject
        private Baggage baggage;

        @Inject
        private Span span;

        @GET
        public String get(@Context HttpHeaders headers) {
            // Check the TestPropagator headers were used
            assertThat(headers.getHeaderString(TestPropagator.TRACE_KEY), notNullValue());
            assertThat(headers.getHeaderString(TestPropagator.BAGGAGE_KEY), notNullValue());

            // Test that the default W3C headers were not used
            assertThat(headers.getHeaderString("traceparent"), nullValue());
            assertThat(headers.getHeaderString("tracestate"), nullValue());
            assertThat(headers.getHeaderString("baggage"), nullValue());

            // Copy TEST_KEY from baggage into a span attribute
            span.setAttribute(TEST_KEY, baggage.getEntryValue(TEST_KEY.getKey()));
            return "OK";
        }
    }

    private URI getTargetURI() {
        try {
            URI originalUri = URI.create(request.getRequestURL().toString());
            URI targetUri = new URI(originalUri.getScheme(), originalUri.getAuthority(), request.getContextPath() + "/target", null, null);
            return targetUri;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}