/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jaxrspropagation.responses;

import static io.opentelemetry.semconv.SemanticAttributes.HTTP_REQUEST_METHOD;
//In MpTelemetry-2.0 SemanticAttributes was moved to a new package, so we use import static to allow both versions to coexist
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.SemanticAttributes.URL_FULL;
import static io.opentelemetry.semconv.SemanticAttributes.URL_PATH;
import static jaxrspropagation.spanexporter.SpanDataMatcher.isSpan;
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
import javax.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import jaxrspropagation.spanexporter.InMemorySpanExporter;
import jaxrspropagation.spanexporter.TestSpans;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/testJaxRsResponseCode")
public class JaxRsResponseCodeTestServlet extends FATServlet {

    @Inject
    private InMemorySpanExporter exporter;

    @Inject
    private HttpServletRequest request;

    @Inject
    private TestSpans spans;

    @Inject
    @ConfigProperty(name = "feature.version")
    private String featureVersion;

    @Test
    public void test200() {
        doTestForStatusCode(200);
    }

    @Test
    public void test202() {
        doTestForStatusCode(202);
    }

    @Test
    public void test400() {
        doTestForStatusCode(400);
    }

    @Test
    public void test404() {
        doTestForStatusCode(404);
    }

    @Test
    public void test500() {
        doTestForStatusCode(500);
    }

    @Test
    public void testRedirect() {
        doTestForStatusCode(307);
    }

    @Test
    public void testInvalidJaxRsPath() {
        URI testUri = UriBuilder.fromUri(getBaseUri())
                        .path("responseCodeEndpoints")
                        .path("invalid")
                        .build();
        Span span = spans.withTestSpan(() -> {
            Response response = ClientBuilder.newClient()
                            .target(testUri)
                            .request()
                            .build("GET")
                            .invoke();
            assertThat(response.getStatus(), equalTo(404));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        if (featureVersion.equals("2.0")) {
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 404L)
                            .withAttribute(URL_FULL, testUri.toString()));

            // Assert the span created by HTTP tracing
            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 404L)
                            .withAttribute(URL_PATH, testUri.getPath()));
        } else if (featureVersion.equals("2.1")) {
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 404L)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));

            // Assert the span created by HTTP tracing
            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 404L)
                            .withAttribute(UrlAttributes.URL_PATH, testUri.getPath()));
        } else {
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                            .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 404L)
                            .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString()));

            // Assert the span created by HTTP tracing
            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                            .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 404L)
                            .withAttribute(SemanticAttributes.HTTP_TARGET, testUri.getPath()));
        }
    }

    private void doTestForStatusCode(int statusCode) {
        URI testUri = getTargetUri(statusCode);
        Span span = spans.withTestSpan(() -> {
            Response response = ClientBuilder.newClient()
                            .target(testUri)
                            .request()
                            .build("GET")
                            .invoke();
            assertThat(response.getStatus(), equalTo(statusCode));
            assertThat(response.readEntity(String.class), equalTo("get" + statusCode));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);
        if (featureVersion.equals("2.0")) {
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, (long) statusCode)
                            .withAttribute(URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, (long) statusCode));
        } else if (featureVersion.equals("2.1")) {
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, (long) statusCode)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, (long) statusCode));
        } else {
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                            .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, (long) statusCode)
                            .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                            .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, (long) statusCode));
        }

    }

    private URI getTargetUri(int statusCode) {
        try {
            String path = request.getContextPath() + "/responseCodeEndpoints/" + statusCode;
            URI originalUri = new URI(request.getRequestURL().toString());
            URI result = new URI(originalUri.getScheme(), originalUri.getAuthority(), path, null, null);
            return result;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private URI getBaseUri() {
        try {
            String path = request.getContextPath();
            URI originalUri = new URI(request.getRequestURL().toString());
            URI result = new URI(originalUri.getScheme(), originalUri.getAuthority(), path, null, null);
            return result;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
