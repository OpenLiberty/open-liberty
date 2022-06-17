/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.rest;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;

@Provider
public class TelemetryClientFilter implements ClientRequestFilter, ClientResponseFilter {

    private Instrumenter<ClientRequestContext, ClientResponseContext> instrumenter;

    private String configString = "otel.span.client.";

    //No arg constructor
    public TelemetryClientFilter() {
    }

    //Constructor injecting openTelemetry object
    @Inject
    public TelemetryClientFilter(final OpenTelemetry openTelemetry) {
        ClientAttributesExtractor clientAttributesExtractor = new ClientAttributesExtractor();

        InstrumenterBuilder<ClientRequestContext, ClientResponseContext> builder = Instrumenter.builder(
                openTelemetry,
                "Client filter",
                HttpSpanNameExtractor.create(clientAttributesExtractor));

        this.instrumenter = builder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(clientAttributesExtractor))
                .addAttributesExtractor(HttpClientAttributesExtractor.create(clientAttributesExtractor))
                .newClientInstrumenter(new ClientRequestContextTextMapSetter());  
    }

    @Override
    public void filter(final ClientRequestContext request) {
        Context parentContext = Context.current();
        if (instrumenter.shouldStart(parentContext, request)) {
            Context spanContext = instrumenter.start(parentContext, request);
            Scope scope = spanContext.makeCurrent();
            request.setProperty(configString + "context", spanContext);
            request.setProperty(configString + "parentContext", parentContext);
            request.setProperty(configString + "scope", scope);
        }
    }

    @Override
    public void filter(final ClientRequestContext request, final ClientResponseContext response) {
        Scope scope = (Scope) request.getProperty(configString + "scope");
        if (scope == null) {
            return;
        }

        Context spanContext = (Context) request.getProperty(configString + "context");
        try {
            instrumenter.end(spanContext, request, response, null);
        } finally {
            scope.close();
            request.removeProperty(configString + "context");
            request.removeProperty(configString + "parentContext");
            request.removeProperty(configString + "scope");
        }
    }

    private static class ClientRequestContextTextMapSetter implements TextMapSetter<ClientRequestContext> {
        @Override
        public void set(final ClientRequestContext carrier, final String key, final String value) {
            if (carrier != null) {
                carrier.getHeaders().put(key, singletonList(value));
            }
        }
    }

    private static class ClientAttributesExtractor
            implements HttpClientAttributesGetter<ClientRequestContext, ClientResponseContext> {

        @Override
        public String flavor(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        //Required
        @Override
        public String method(final ClientRequestContext request) {
            return request.getMethod();
        }

        //If one was sent
        @Override
        public Integer statusCode(final ClientRequestContext request, final ClientResponseContext response) {
            return response.getStatus();
        }

        @Override
        public String url(final ClientRequestContext request) {
            return request.getUri().toString();
        }

        @Override
        public List<String> requestHeader(final ClientRequestContext request, final String name) {
            return request.getStringHeaders().getOrDefault(name, emptyList());
        }

        @Override
        public Long requestContentLength(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        public Long requestContentLengthUncompressed(final ClientRequestContext request,
                final ClientResponseContext response) {
            return null;
        }

        @Override
        public Long responseContentLength(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        public Long responseContentLengthUncompressed(final ClientRequestContext request,
                final ClientResponseContext response) {
            return null;
        }

        @Override
        public List<String> responseHeader(final ClientRequestContext request, final ClientResponseContext response,
                final String name) {
            return response.getHeaders().getOrDefault(name, emptyList());
        }
    }
} 