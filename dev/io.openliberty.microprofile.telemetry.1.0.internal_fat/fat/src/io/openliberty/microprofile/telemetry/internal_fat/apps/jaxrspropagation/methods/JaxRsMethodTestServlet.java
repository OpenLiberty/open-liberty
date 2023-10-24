/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.methods;

import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.isSpan;
import static jakarta.ws.rs.client.Entity.text;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/**
 * Test tracing requests of each JAX-RS method type
 */
@SuppressWarnings("serial")
@WebServlet("/testJaxRsMethod")
public class JaxRsMethodTestServlet extends FATServlet {

    @Inject
    private InMemorySpanExporter exporter;

    @Inject
    private HttpServletRequest request;

    @Inject
    private TestSpans utils;

    @Test
    public void testGet() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(testUri).request()
                            .build("GET").invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("get"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        assertThat(clientSpan, isSpan()
                        .withKind(SpanKind.CLIENT)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString()));

        assertThat(serverSpan, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L));
    }

    @Test
    public void testPost() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(testUri).request()
                            .buildPost(text("test"))
                            .invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("post"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        assertThat(clientSpan, isSpan()
                        .withKind(SpanKind.CLIENT)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "POST")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString()));

        assertThat(serverSpan, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "POST")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L));
    }

    @Test
    public void testPut() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(testUri).request()
                            .buildPut(text("test"))
                            .invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("put"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        assertThat(clientSpan, isSpan()
                        .withKind(SpanKind.CLIENT)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "PUT")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString()));

        assertThat(serverSpan, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "PUT")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L));
    }

    @Test
    public void testHead() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(testUri).request()
                            .build("HEAD")
                            .invoke();
            assertThat(response.getStatus(), equalTo(204));
            assertThat(response.hasEntity(), is(false));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        assertThat(clientSpan, isSpan()
                        .withKind(SpanKind.CLIENT)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "HEAD")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 204L)
                        .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString()));

        assertThat(serverSpan, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "HEAD")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 204L));

    }

    @Test
    public void testDelete() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(testUri).request()
                            .build("DELETE")
                            .invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("delete"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        assertThat(clientSpan, isSpan()
                        .withKind(SpanKind.CLIENT)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "DELETE")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString()));

        assertThat(serverSpan, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "DELETE")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L));
    }

    @Test
    public void testPatch() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(testUri).request()
                            .build("PATCH", text("test"))
                            .invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("patch"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        assertThat(clientSpan, isSpan()
                        .withKind(SpanKind.CLIENT)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "PATCH")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString()));

        assertThat(serverSpan, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "PATCH")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L));
    }

    @Test
    public void testOptions() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(testUri).request()
                            .build("OPTIONS")
                            .invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("options"));
            assertThat(response.getStringHeaders().get(HttpHeaders.ALLOW), containsInAnyOrder("GET", "HEAD", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        assertThat(clientSpan, isSpan()
                        .withKind(SpanKind.CLIENT)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "OPTIONS")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString()));

        assertThat(serverSpan, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "OPTIONS")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L));
    }

    private URI getUri() {
        try {
            String path = request.getContextPath() + "/methodTestEndpoints";
            URI originalUri = new URI(request.getRequestURL().toString());
            URI result = new URI(originalUri.getScheme(), originalUri.getAuthority(), path, null, null);
            return result;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
