/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jaxrspropagation.methods;

import static io.opentelemetry.semconv.SemanticAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.SemanticAttributes.URL_FULL;
import static javax.ws.rs.client.Entity.text;
import static jaxrspropagation.spanexporter.SpanDataMatcher.isSpan;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
//In MpTelemetry-2.1 SemanticAttributes moved to their relative classes
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import jaxrspropagation.spanexporter.InMemorySpanExporter;
import jaxrspropagation.spanexporter.TestSpans;

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

    @Inject
    @ConfigProperty(name = "feature.version")
    private String featureVersion;

    @Test
    public void testGet() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient()
                            .target(testUri)
                            .request()
                            .build("GET")
                            .invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("get"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        if (featureVersion.equals("2.1")) { //SemanticAttributes moved to their relative classes organised by root namespaces
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        } else if (featureVersion.equals("2.0")) { //SemanticAttributes moved to a new package. HTTP_URL has changed to URL_FULL
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L));
        } else {
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
    }

    @Test
    public void testPost() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient()
                            .target(testUri)
                            .request()
                            .buildPost(text("test"))
                            .invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("post"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        if (featureVersion.equals("2.0")) {
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HTTP_REQUEST_METHOD, "POST")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HTTP_REQUEST_METHOD, "POST")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L));
        } else if (featureVersion.equals("2.1")) {
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "POST")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "POST")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        } else {
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
    }

    @Test
    public void testPut() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient()
                            .target(testUri)
                            .request()
                            .buildPut(text("test"))
                            .invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("put"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        if (featureVersion.equals("2.1")) {
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "PUT")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "PUT")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        } else if (featureVersion.equals("2.0")) {
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HTTP_REQUEST_METHOD, "PUT")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HTTP_REQUEST_METHOD, "PUT")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L));
        } else {
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
    }

    @Test
    public void testHead() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient()
                            .target(testUri)
                            .request()
                            .build("HEAD")
                            .invoke();
            assertThat(response.getStatus(), equalTo(204));
            assertThat(response.hasEntity(), is(false));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        if (featureVersion.equals("2.1")) {
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "HEAD")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 204L)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "HEAD")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 204L));
        } else if (featureVersion.equals("2.0")) {
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HTTP_REQUEST_METHOD, "HEAD")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 204L)
                            .withAttribute(URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HTTP_REQUEST_METHOD, "HEAD")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 204L));
        } else {
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

    }

    @Test
    public void testDelete() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient()
                            .target(testUri)
                            .request()
                            .build("DELETE")
                            .invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("delete"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        if (featureVersion.equals("2.1")) {
            System.out.println("featureVersion 2.1 in method");
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "DELETE")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "DELETE")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));

            System.out.println("Finished 2.1 in method");
        } else if (featureVersion.equals("2.0")) {
            System.out.println("featureVersion 2.0 in method");
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HTTP_REQUEST_METHOD, "DELETE")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HTTP_REQUEST_METHOD, "DELETE")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L));
        } else {
            System.out.println("featureVersion 1.1 in method");
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
