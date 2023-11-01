/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jaxrspropagation.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jaxrspropagation.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageEntry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * This endpoint is used to test propagation.
 *
 * It does the following:
 * <ul>
 * <li>Creates a span
 * <li>Collects the names of all known propagation headers which are present in the HTTP request and sets that as a span attribute with key {@link #PROPAGATION_HEADERS_ATTR}
 * <li>Reads the baggage entry with key {@link #BAGGAGE_KEY} and if present sets
 * <ul><li>span attribute {@link #BAGGAGE_VALUE_ATTR} to the entry value
 * <li>span attribute {@link #BAGGAGE_METADATA_ATTR} to the entry metadata value
 * </ul></ul>
 *
 * Tests using this endpoint should generally do the following:
 * <ul>
 * <li>Start a span
 * <li>Set the baggage entry {@link #BAGGAGE_KEY} to a known test value
 * <li>Request this endpoint
 * <li>End their span
 * <li>Retrieve the created spans (e.g. via {@link InMemorySpanExporter})
 * <li>Verify that their span has a child CLIENT span which has a child SERVER span (which will be the span created by this endpoint) (if traceIds should be propagated)
 * <li>Verify the value of the {@link #BAGGAGE_VALUE_ATTR} attribute in the SERVER span is value of the test baggage entry (if baggage should be propagated)
 * <li>Verify the value of the {@link #BAGGAGE_METADATA_ATTR} attribute in the SERVER span (if baggage metadata should be propagated)
 * <li>Verify the value of the {@link #PROPAGATION_HEADERS_ATTR} attribute in the SERVER span is the list of headers expected to be used for propagation
 * </ul>
 */
@ApplicationPath("/")
@Path("/propagationHeaderEndpoint")
public class PropagationHeaderEndpoint extends Application {

    public static final String BAGGAGE_KEY = "test.baggage.key";
    public static final AttributeKey<String> BAGGAGE_METADATA_ATTR = AttributeKey.stringKey("test.baggage.metadata");
    public static final AttributeKey<String> BAGGAGE_VALUE_ATTR = AttributeKey.stringKey("test.baggage");
    public static final AttributeKey<List<String>> PROPAGATION_HEADERS_ATTR = AttributeKey.stringArrayKey("test.propagation.headers");

    private static final List<String> PROPAGATION_HEADER_NAMES = Arrays.asList("baggage", "traceparent",
                                                                               "b3",
                                                                               "X-B3-TraceId", "X-B3-SpanId", "X-B3-ParentSpanId", "X-B3-Sampled",
                                                                               "uber-trace-id");

    @GET
    public String get(@Context HttpHeaders headers) {
        Span span = Span.current();

        // Extract the propagation headers and store in the span
        List<String> propagationHeaders = headers.getRequestHeaders().keySet().stream()
                        .filter(PropagationHeaderEndpoint::isPropagationHeader)
                        .collect(Collectors.toList());
        span.setAttribute(PropagationHeaderEndpoint.PROPAGATION_HEADERS_ATTR, propagationHeaders);

        // Extract the test baggage value (if present) and store in the span
        BaggageEntry baggageEntry = Baggage.current().asMap().get(BAGGAGE_KEY);
        if (baggageEntry != null) {
            span.setAttribute(BAGGAGE_VALUE_ATTR, baggageEntry.getValue());
            span.setAttribute(BAGGAGE_METADATA_ATTR, baggageEntry.getMetadata().getValue());
        }

        return "OK";
    }

    public static boolean isPropagationHeader(String header) {
        return PROPAGATION_HEADER_NAMES.contains(header)
               || header.startsWith("uberctx-"); // Jaeger baggage headers use keys as part of the header but have a common prefix
    }

    public static URI getBaseUri(HttpServletRequest request) {
        try {
            URI originalUri = URI.create(request.getRequestURL().toString());
            URI targetUri = new URI(originalUri.getScheme(), originalUri.getAuthority(), request.getContextPath(), null, null);
            System.out.println("Using URI: " + targetUri);
            return targetUri;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}