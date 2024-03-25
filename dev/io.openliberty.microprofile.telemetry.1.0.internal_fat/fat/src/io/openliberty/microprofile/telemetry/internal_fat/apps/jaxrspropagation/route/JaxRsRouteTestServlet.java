/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.route;

import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasName;
import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.isSpan;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

/**
 * Test that the HTTP_ROUTE attribute is set correctly for a method with placeholders
 */
@SuppressWarnings("serial")
@WebServlet("/testJaxRsRoute")
public class JaxRsRouteTestServlet extends FATServlet {

    @Inject
    private InMemorySpanExporter exporter;

    @Inject
    private HttpServletRequest request;

    @Inject
    private TestSpans utils;

    @Inject
    @ConfigProperty(name = "feature.version")
    private String featureVersion;

    @Test
    public void testRouteWithId() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(testUri).path("getWithId/myIdForTesting").request()
                            .build("GET").invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("myIdForTesting"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        assertThat(clientSpan, isSpan()
                        .withKind(SpanKind.CLIENT)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString() + "/getWithId/myIdForTesting"));

        assertThat(serverSpan, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_ROUTE, getPath() + "/getWithId/{id}")
                        .withAttribute(SemanticAttributes.HTTP_TARGET, getPath() + "/getWithId/myIdForTesting"));

        if (featureVersion.equals("1.1")) {
            assertThat(serverSpan, hasName("GET " + getPath() + "/getWithId/{id}"));
        } else {
            assertThat(serverSpan, hasName(getPath() + "/getWithId/{id}"));
        }
    }

    @Test
    public void testRouteWithQueryParam() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient()
                            .target(testUri)
                            .path("getWithQueryParam")
                            .queryParam("id", "myIdForTesting")
                            .request()
                            .build("GET").invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("myIdForTesting"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        assertThat(clientSpan, isSpan()
                        .withKind(SpanKind.CLIENT)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString() + "/getWithQueryParam?id=myIdForTesting"));

        assertThat(serverSpan, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_ROUTE, getPath() + "/getWithQueryParam")
                        .withAttribute(SemanticAttributes.HTTP_TARGET, getPath() + "/getWithQueryParam?id=myIdForTesting"));

        if (featureVersion.equals("1.1")) {
            assertThat(serverSpan, hasName("GET " + getPath() + "/getWithQueryParam"));
        } else {
            assertThat(serverSpan, hasName(getPath() + "/getWithQueryParam"));
        }
    }

    /*
     * Ideal behaviour for this test would be HHTP_ROUTE = /getSubResourceWithPathParam/{id}/details
     * Due to the current behaviour when sub resources are used is that only the context root is returned
     */
    @Test
    public void testRouteWithSubResourceWithPathParam() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(testUri).path("getSubResourceWithPathParam/myIdForTesting/details").request()
                            .build("GET").invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("myIdForTesting"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        assertThat(clientSpan, isSpan()
                        .withKind(SpanKind.CLIENT)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString() + "/getSubResourceWithPathParam/myIdForTesting/details"));

        assertThat(serverSpan, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_ROUTE, request.getContextPath())
                        .withAttribute(SemanticAttributes.HTTP_TARGET, getPath() + "/getSubResourceWithPathParam/myIdForTesting/details"));
    }

    /*
     * Ideal behaviour for this test would be HHTP_ROUTE = /getSubResourceWithQueryParam/details
     * Due to the current behaviour when sub resources are used is that only the context root is returned
     */
    @Test
    public void testRouteWithSubResourceWithQueryParam() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient()
                            .target(testUri)
                            .path("getSubResourceWithQueryParam/details")
                            .queryParam("id", "myIdForTesting")
                            .request()
                            .build("GET").invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("myIdForTesting"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        assertThat(clientSpan, isSpan()
                        .withKind(SpanKind.CLIENT)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString() + "/getSubResourceWithQueryParam/details?id=myIdForTesting"));

        assertThat(serverSpan, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_ROUTE, request.getContextPath()));
//                        .withAttribute(SemanticAttributes.HTTP_TARGET, getPath() + "/getSubResourceWithQueryParam/details?id=myIdForTesting"));
    }

    private URI getUri() {
        try {
            String path = getPath();
            URI originalUri = new URI(request.getRequestURL().toString());
            URI result = new URI(originalUri.getScheme(), originalUri.getAuthority(), path, null, null);
            return result;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String getPath() {
        return request.getContextPath() + "/routeTestEndpoints";
    }

}
