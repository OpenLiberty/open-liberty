/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.methods;

import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.isSpan;
import static javax.ws.rs.client.Entity.text;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.rules.repeater.MicroProfileActions;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

// In MpTelemetry-2.0 SemanticAttributes was moved to a new package, so we use import static to allow both versions to coexist
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.SemanticAttributes.URL_FULL;
import static io.opentelemetry.semconv.SemanticAttributes.URL_SCHEME;
import static io.opentelemetry.semconv.SemanticAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.SemanticAttributes.SERVER_PORT;

// In MpTelemetry-2.1 SemanticAttributes moved to their relative classes
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UrlAttributes;
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
            Response response = ClientBuilder.newClient().target(testUri).request()
                            .build("GET").invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("get"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        if(featureVersion.equals("2.1")){  //SemanticAttributes moved to their relative classes organised by root namespaces
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));
            
            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        } else if(featureVersion.equals("2.0")){ //SemanticAttributes moved to a new package. HTTP_URL has changed to URL_FULL
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

        if(featureVersion.equals("2.0")){
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HTTP_REQUEST_METHOD, "POST")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(URL_FULL, testUri.toString()));
            
            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HTTP_REQUEST_METHOD, "POST")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L));
        } else if(featureVersion.equals("2.1")){
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

        if(featureVersion.equals("2.1")){
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "PUT")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));
            
            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "PUT")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        } else if(featureVersion.equals("2.0")){
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

        if(featureVersion.equals("2.1")){
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "HEAD")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 204L)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));
            
            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "HEAD")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 204L));
        } else if(featureVersion.equals("2.0")){
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

        if(featureVersion.equals("2.1")){
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
        } else if(featureVersion.equals("2.0")){
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

    @Test
    @SkipForRepeat({TelemetryActions.MP14_MPTEL11_ID, TelemetryActions.MP14_MPTEL20_ID, TelemetryActions.MP14_MPTEL21_ID,})
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
        System.out.println("VERSION NUMBER: " + featureVersion);
        if(featureVersion.equals("2.1")){
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "PATCH")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "PATCH")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        } else if(featureVersion.equals("2.0")){
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HTTP_REQUEST_METHOD, "PATCH")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(URL_FULL, testUri.toString()));

            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HTTP_REQUEST_METHOD, "PATCH")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L));
        } else {
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
    }

    @Test
    @SkipForRepeat({ TelemetryActions.MP50_MPTEL11_ID, MicroProfileActions.MP60_ID, MicroProfileActions.MP61_ID, TelemetryActions.MP50_MPTEL20_ID, TelemetryActions.MP50_MPTEL20_JAVA8_ID, TelemetryActions.MP61_MPTEL20_ID, MicroProfileActions.MP70_EE10_ID, MicroProfileActions.MP70_EE11_ID, TelemetryActions.MP50_MPTEL21_ID, TelemetryActions.MP50_MPTEL21_JAVA8_ID, MicroProfileActions.MP71_EE10_ID, MicroProfileActions.MP71_EE11_ID })
    public void testOptionsBelowEE9() {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(testUri).request()
                            .build("OPTIONS")
                            .invoke();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.readEntity(String.class), equalTo("options"));

            // Added in "get" and "split" due to JAX-RS behaviour when providing the headers in JEE7/MP1.4
            // We manually add PATCH with HttpHeaders.ALLOW so expect the span to contain it in the response header with JaxRs-2.0
            assertThat(Arrays.asList(response.getStringHeaders().get(HttpHeaders.ALLOW).get(0).split(",", -1)),
                       containsInAnyOrder("GET", "HEAD", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        if(featureVersion.equals("2.1")){
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "OPTIONS")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));
            
            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "OPTIONS")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        } else if(featureVersion.equals("2.0")){
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HTTP_REQUEST_METHOD, "OPTIONS")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(URL_FULL, testUri.toString()));
            
            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HTTP_REQUEST_METHOD, "OPTIONS")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L));
        } else {
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
    }

    @Test
    @SkipForRepeat({ TelemetryActions.MP14_MPTEL11_ID, TelemetryActions.MP41_MPTEL11_ID, TelemetryActions.MP14_MPTEL20_ID, TelemetryActions.MP41_MPTEL20_ID, TelemetryActions.MP14_MPTEL21_ID, TelemetryActions.MP41_MPTEL21_ID})
    public void testOptionsAboveEE8() {
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

        if(featureVersion.equals("2.1")){
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "OPTIONS")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(UrlAttributes.URL_FULL, testUri.toString()));
            
            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "OPTIONS")
                            .withAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        } else if(featureVersion.equals("2.0")){
            assertThat(clientSpan, isSpan()
                            .withKind(SpanKind.CLIENT)
                            .withAttribute(HTTP_REQUEST_METHOD, "OPTIONS")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L)
                            .withAttribute(URL_FULL, testUri.toString()));
            
            assertThat(serverSpan, isSpan()
                            .withKind(SpanKind.SERVER)
                            .withAttribute(HTTP_REQUEST_METHOD, "OPTIONS")
                            .withAttribute(HTTP_RESPONSE_STATUS_CODE, 200L));
        } else {
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