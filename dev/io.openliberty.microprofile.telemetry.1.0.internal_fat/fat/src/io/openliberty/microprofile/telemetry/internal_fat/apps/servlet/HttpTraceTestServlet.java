/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.servlet;

import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasKind;
import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.isSpan;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

@SuppressWarnings("serial")
@WebServlet("/testHttpTrace")
public class HttpTraceTestServlet extends FATServlet {

    public static final String APP_NAME = "TelemetryServletTestApp";
    public static final String SIMPLE_SERVLET = "simple";
    public static final String SIMPLE_ASYNC_SERVLET = "simpleAsync";
    public static final String CONTEXT_ASYNC_SERVLET = "contextAsync";
    public static final String HELLO_HTML = "hello.html";
    public static final String DICE_JSP = "dice.jsp";

    @Inject
    private InMemorySpanExporter spanExporter;

    @Inject
    private HttpServletRequest request;

    @Inject
    private TestSpans utils;

    @Test
    public void testSimpleServlet() throws Exception {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        URL url = new URL(scheme + "://" + serverName + ":" + serverPort + "/" + APP_NAME + "/" + SIMPLE_SERVLET);

        String traceId = httpGet(url); // The servlet outputs the traceId

        // The simple servlet will return the traceId
        List<SpanData> spanDataList = spanExporter.getFinishedSpanItems(1, traceId);
        SpanData servletSpan = spanDataList.get(0);
        assertThat(servletSpan, isSpan()
                        .withNoParent()
                        .withKind(SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_ROUTE, "/" + APP_NAME + "/" + SIMPLE_SERVLET)
                        .withAttribute(SemanticAttributes.HTTP_TARGET, "/" + APP_NAME + "/" + SIMPLE_SERVLET)
                        .withAttribute(SemanticAttributes.HTTP_SCHEME, scheme)
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.NET_HOST_NAME, serverName)
                        .withAttribute(SemanticAttributes.NET_HOST_PORT, Long.valueOf(serverPort)));
    }

    @Test
    public void testSimpleAsyncServlet() throws Exception {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        URI uri = new URI(scheme + "://" + serverName + ":" + serverPort + "/" + APP_NAME + "/" + SIMPLE_ASYNC_SERVLET);

        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(uri).request()
                            .build("GET").invoke();
            assertThat(response.getStatus(), equalTo(200));
        });

        // The simple servlet will return the traceId
        List<SpanData> spanDataList = spanExporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        SpanData servletSpan = spanDataList.get(2);
        assertThat(servletSpan, isSpan()
                        .withKind(SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_ROUTE, "/" + APP_NAME + "/" + SIMPLE_ASYNC_SERVLET)
                        .withAttribute(SemanticAttributes.HTTP_TARGET, "/" + APP_NAME + "/" + SIMPLE_ASYNC_SERVLET)
                        .withAttribute(SemanticAttributes.HTTP_SCHEME, scheme)
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.NET_HOST_NAME, serverName)
                        .withAttribute(SemanticAttributes.NET_HOST_PORT, Long.valueOf(serverPort))

        );
        // Make sure the duration is longer than 2 seconds as there is a sleep for 2 seconds in the servlet
        long duration = servletSpan.getEndEpochNanos() - servletSpan.getStartEpochNanos();
        assertTrue(duration > TimeUnit.SECONDS.toNanos(2));

    }

    @Test
    public void testContextAsyncServlet() throws Exception {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        URI uri = new URI(scheme + "://" + serverName + ":" + serverPort + "/" + APP_NAME + "/" + CONTEXT_ASYNC_SERVLET);

        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(uri).request()
                            .build("GET").invoke();
            assertThat(response.getStatus(), equalTo(200));
        });

        // The simple servlet will return the traceId
        List<SpanData> spanDataList = spanExporter.getFinishedSpanItems(4, span.getSpanContext().getTraceId());

        TestSpans.assertLinearParentage(spanDataList);

        SpanData testSpan = spanDataList.get(0);
        assertThat(testSpan, hasKind(INTERNAL));

        SpanData clientSpan = spanDataList.get(1);
        assertThat(clientSpan, hasKind(CLIENT));

        SpanData servletSpan = spanDataList.get(2);
        assertThat(servletSpan, isSpan()
                        .withKind(SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_ROUTE, "/" + APP_NAME + "/" + CONTEXT_ASYNC_SERVLET)
                        .withAttribute(SemanticAttributes.HTTP_TARGET, "/" + APP_NAME + "/" + CONTEXT_ASYNC_SERVLET)
                        .withAttribute(SemanticAttributes.HTTP_SCHEME, scheme)
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.NET_HOST_NAME, serverName)
                        .withAttribute(SemanticAttributes.NET_HOST_PORT, Long.valueOf(serverPort))

        );
        // Make sure the duration is longer than 2 seconds as there is a sleep for 2 seconds in the servlet
        long duration = servletSpan.getEndEpochNanos() - servletSpan.getStartEpochNanos();
        assertTrue(duration > TimeUnit.SECONDS.toNanos(2));

        SpanData taskSpan = spanDataList.get(3);
        assertThat(taskSpan, isSpan()
                        .withKind(INTERNAL));
        assertThat("Servlet span should finish after subtask span", servletSpan.getEndEpochNanos(), greaterThan(taskSpan.getEndEpochNanos()));

    }

    @Test
    public void testStaticFile() throws Exception {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        URI uri = new URI(scheme + "://" + serverName + ":" + serverPort + "/" + APP_NAME + "/" + HELLO_HTML);
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(uri).request()
                            .build("GET").invoke();
            assertThat(response.getStatus(), equalTo(200));
        });
        List<SpanData> spanDataList = spanExporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        SpanData testSpan = spanDataList.get(0);
        assertThat(testSpan, hasKind(INTERNAL));

        SpanData clientSpan = spanDataList.get(1);
        assertThat(clientSpan, hasKind(CLIENT));

        SpanData serverSpan = spanDataList.get(2);
        assertThat(serverSpan, isSpan()
                        .withKind(SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_ROUTE, "/" + APP_NAME + "/" + HELLO_HTML)
                        .withAttribute(SemanticAttributes.HTTP_SCHEME, scheme)
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.NET_HOST_NAME, serverName)
                        .withAttribute(SemanticAttributes.NET_HOST_PORT, Long.valueOf(serverPort)));
    }

    @Test
    public void testJsp() throws Exception {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        URI uri = new URI(scheme + "://" + serverName + ":" + serverPort + "/" + APP_NAME + "/" + DICE_JSP);
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(uri).request()
                            .build("GET").invoke();
            assertThat(response.getStatus(), equalTo(200));
        });
        List<SpanData> spanDataList = spanExporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        SpanData testSpan = spanDataList.get(0);
        assertThat(testSpan, hasKind(INTERNAL));

        SpanData clientSpan = spanDataList.get(1);
        assertThat(clientSpan, hasKind(CLIENT));

        SpanData serverSpan = spanDataList.get(2);
        assertThat(serverSpan, isSpan()
                        .withKind(SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_ROUTE, "/" + APP_NAME + "/" + DICE_JSP)
                        .withAttribute(SemanticAttributes.HTTP_SCHEME, scheme)
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.NET_HOST_NAME, serverName)
                        .withAttribute(SemanticAttributes.NET_HOST_PORT, Long.valueOf(serverPort)));
    }

    private String httpGet(URL url) throws IOException {
        HttpURLConnection connection = null;
        StringBuffer content = new StringBuffer();
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            assertEquals(200, connection.getResponseCode());
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return content.toString();
    }

}