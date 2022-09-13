/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
import java.util.HashMap;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;

import org.eclipse.microprofile.config.Config;

@Provider
public class TelemetryContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String INSTRUMENTATION_NAME = "io.openliberty.microprofile.telemetry";

    private static final String REST_RESOURCE_CLASS = "rest.resource.class";
    private static final String REST_RESOURCE_METHOD = "rest.resource.method";
    private static final String REST_RESOURCE_ROUTE = "rest.resource.route";

    private static final String SPAN_CONTEXT = "otel.span.server.context";
    private static final String SPAN_PARENT_CONTEXT = "otel.span.server.parentContext";
    private static final String SPAN_SCOPE = "otel.span.server.scope";

    private Instrumenter<ContainerRequestContext, ContainerResponseContext> instrumenter;

    @jakarta.ws.rs.core.Context
    ResourceInfo resourceInfo;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    public TelemetryContainerFilter() {
    }

    @Inject
    public TelemetryContainerFilter(final OpenTelemetry openTelemetry) {
        ServerAttributesExtractor serverAttributesExtractor = new ServerAttributesExtractor();

        InstrumenterBuilder<ContainerRequestContext, ContainerResponseContext> builder = Instrumenter.builder(
            openTelemetry,
            INSTRUMENTATION_NAME,
            HttpSpanNameExtractor.create(serverAttributesExtractor));

        this.instrumenter = builder
            .setSpanStatusExtractor(HttpSpanStatusExtractor.create(serverAttributesExtractor))
            .addAttributesExtractor(HttpServerAttributesExtractor.create(serverAttributesExtractor))
            .newServerInstrumenter(new ContainerRequestContextTextMapGetter());
    }

    @Override
    public void filter(final ContainerRequestContext request) {
        Context parentContext = Context.current();
        if (instrumenter.shouldStart(parentContext, request)) {
            request.setProperty(REST_RESOURCE_CLASS, resourceInfo.getResourceClass());
            request.setProperty(REST_RESOURCE_METHOD, resourceInfo.getResourceMethod());

            Context spanContext = instrumenter.start(parentContext, request);
            Scope scope = spanContext.makeCurrent();
            request.setProperty(SPAN_CONTEXT, spanContext);
            request.setProperty(SPAN_PARENT_CONTEXT, parentContext);
            request.setProperty(SPAN_SCOPE, scope);
        }
    }

    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {
        Scope scope = (Scope) request.getProperty(SPAN_SCOPE);
        if (scope == null) {
            return;
        }

        Context spanContext = (Context) request.getProperty(SPAN_CONTEXT);
        try {
            instrumenter.end(spanContext, request, response, null);
        } finally {
            scope.close();

            request.removeProperty(REST_RESOURCE_CLASS);
            request.removeProperty(REST_RESOURCE_METHOD);
            request.removeProperty(SPAN_CONTEXT);
            request.removeProperty(SPAN_PARENT_CONTEXT);
            request.removeProperty(SPAN_SCOPE);
        }
    }

    private static class ContainerRequestContextTextMapGetter implements TextMapGetter<ContainerRequestContext> {
        
        @Override
        public Iterable<String> keys(final ContainerRequestContext carrier) {
            return carrier.getHeaders().keySet();
        }

        @Override
        public String get(final ContainerRequestContext carrier, final String key) {
            if (carrier == null) {
                return null;
            }

            return carrier.getHeaders().getOrDefault(key, singletonList(null)).get(0);
        }
    }

    private static class ServerAttributesExtractor
            implements HttpServerAttributesGetter<ContainerRequestContext, ContainerResponseContext> {

        @Override
        public String flavor(final ContainerRequestContext request) {
            return null;
        }

        @Override
        public String route(final ContainerRequestContext request) {

            String route = (String) request.getProperty(REST_RESOURCE_ROUTE);

            if (route == null) {
                Class<?> resourceClass = (Class<?>) request.getProperty(REST_RESOURCE_CLASS);
                Method method = (Method) request.getProperty(REST_RESOURCE_METHOD);

                String contextRoot = request.getUriInfo().getBaseUri().getPath();
                UriBuilder template = UriBuilder.fromPath(contextRoot);

                template.path(resourceClass);

                if (method.isAnnotationPresent(Path.class)) {
                    template.path(method);
                }

                route = template.toTemplate();
                request.setProperty(REST_RESOURCE_ROUTE, route);
            }

            return route;
        }
        //required
        @Override
        public String method(final ContainerRequestContext request) {
            return request.getMethod();
        }

        @Override
        public String target(final ContainerRequestContext request) {
            URI requestUri = request.getUriInfo().getRequestUri();
            String path = requestUri.getPath();
            String query = requestUri.getQuery();
            if (path != null && query != null && !query.isEmpty()) {
                return path + "?" + query;
            }
            return path;
        }

        @Override
        public String scheme(final ContainerRequestContext request) {
            return request.getUriInfo().getRequestUri().getScheme();
        }

        //If one was sent
        @Override
        public Integer statusCode(final ContainerRequestContext request, final ContainerResponseContext response) {
            return response.getStatus();
        }

        @Override
        public String serverName(final ContainerRequestContext request) {
            return request.getUriInfo().getRequestUri().getHost();
        }

        @Override
        public List<String> requestHeader(final ContainerRequestContext request, final String name) {
            return request.getHeaders().getOrDefault(name, emptyList());
        }

        @Override
        public Long requestContentLength(final ContainerRequestContext request, final ContainerResponseContext response) {
            return null;
        }

        @Override
        public Long requestContentLengthUncompressed(final ContainerRequestContext request,
                final ContainerResponseContext response) {
            return null;
        }

        @Override
        public Long responseContentLength(final ContainerRequestContext request, final ContainerResponseContext response) {
            return null;
        }

        @Override
        public Long responseContentLengthUncompressed(final ContainerRequestContext request,
                final ContainerResponseContext response) {
            return null;
        }

        @Override
        public List<String> responseHeader(final ContainerRequestContext request, final ContainerResponseContext response,
                final String name) {
            return response.getStringHeaders().getOrDefault(name, emptyList());
        }
    }
} 