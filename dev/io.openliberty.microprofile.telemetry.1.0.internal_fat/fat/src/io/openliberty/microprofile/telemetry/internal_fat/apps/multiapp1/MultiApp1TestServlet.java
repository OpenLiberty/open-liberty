/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat.apps.multiapp1;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.client.ClientBuilder;

@SuppressWarnings("serial")
@WebServlet("/multiapp1")
public class MultiApp1TestServlet extends FATServlet {

    @Inject
    private Tracer tracer;

    @Inject
    private InMemorySpanExporter exporter;

    @Inject
    private HttpServletRequest request;

    @Test
    public void testResource() {
        Span span = tracer.spanBuilder("test").startSpan();
        span.end();

        SpanData spanData = exporter.getFinishedSpanItems(1).get(0);
        Resource resource = spanData.getResource();
        assertThat(resource.getAttribute(ResourceAttributes.SERVICE_NAME), equalTo("multiapp1"));
    }

    @Test
    public void callMultiApp() {
        URI uri = getTargetUri();
        String response = ClientBuilder.newClient().target(uri).request().get(String.class);
        assertThat(response, equalTo("OK"));

        // Note the exporter is static and in a shared library so it will contain traces from both apps
        List<SpanData> spanData = exporter.getFinishedSpanItems(2);
        SpanData app1client = spanData.get(0);
        SpanData app2server = spanData.get(1);

        assertThat(app1client.getKind(), equalTo(SpanKind.CLIENT));
        assertThat(app1client.getResource().getAttribute(ResourceAttributes.SERVICE_NAME), equalTo("multiapp1"));

        assertThat(app2server.getKind(), equalTo(SpanKind.SERVER));
        assertThat(app2server.getResource().getAttribute(ResourceAttributes.SERVICE_NAME), equalTo("multiapp2"));
    }

    /**
     * Get the URI for the "target" resource in multiapp2
     *
     * @return the URI for the "target" resource
     */
    private URI getTargetUri() {
        try {
            URI originalUri = URI.create(request.getRequestURL().toString());
            URI targetUri = new URI(originalUri.getScheme(), originalUri.getAuthority(), "/multiapp2/target", null, null);
            return targetUri;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void before() throws Exception {
        exporter.reset();
    }

}
