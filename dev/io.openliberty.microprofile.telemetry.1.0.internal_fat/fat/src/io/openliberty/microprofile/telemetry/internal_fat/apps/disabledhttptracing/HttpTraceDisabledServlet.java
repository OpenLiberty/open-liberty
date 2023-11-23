/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.disabledhttptracing;

import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasKind;
import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.isSpan;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

@SuppressWarnings("serial")
@WebServlet("/testDisabledHttpTrace")
public class HttpTraceDisabledServlet extends FATServlet {

    public static final String APP_NAME = "HttpTraceDisabledServletTestApp";
    public static final String HELLO_HTML = "hello.html";

    @Inject
    private HttpServletRequest request;

    @Inject
    private InMemorySpanExporter spanExporter;

    @Inject
    private TestSpans utils;

    @Test
    public void testDisabledStaticFile() throws Exception {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        URI uri = new URI(scheme + "://" + serverName + ":" + serverPort + "/" + APP_NAME + "/" + HELLO_HTML);
        System.out.println(uri.toString());
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(uri).request()
                            .build("GET").invoke();
            assertThat(response.getStatus(), equalTo(200));
        });
        List<SpanData> spanDataList = spanExporter.getFinishedSpanItems(2, span.getSpanContext().getTraceId());
        SpanData testSpan = spanDataList.get(0);
        assertThat(testSpan, hasKind(INTERNAL));

        SpanData clientSpan = spanDataList.get(1);
        assertThat(clientSpan, hasKind(CLIENT));
    }

    @Test
    public void testHttpTracingDisabledWithJaxrs() throws Exception {
        URI testUri = getUri();
        Span span = utils.withTestSpan(() -> {
            Response response = ClientBuilder.newClient().target(testUri).path("/getResource").request()
                            .build("GET").invoke();
            assertThat(response.getStatus(), equalTo(200));
        });

        List<SpanData> spans = spanExporter.getFinishedSpanItems(3, span.getSpanContext().getTraceId());
        TestSpans.assertLinearParentage(spans);

        SpanData clientSpan = spans.get(1);
        SpanData serverSpan = spans.get(2);

        assertThat(clientSpan, isSpan()
                        .withKind(SpanKind.CLIENT)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_URL, testUri.toString() + "/getResource"));

        assertThat(serverSpan, isSpan()
                        .withKind(SpanKind.SERVER)
                        .withAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                        .withAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200L)
                        .withAttribute(SemanticAttributes.HTTP_ROUTE, getPath() + "/getResource")
                        .withAttribute(SemanticAttributes.HTTP_TARGET, getPath() + "/getResource"));

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