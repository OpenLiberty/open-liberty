/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.rest;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.openliberty.microprofile.telemetry.internal.helper.AgentDetection;
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
import io.opentelemetry.instrumentation.api.instrumenter.net.NetServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetServerAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TelemetryContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String INSTRUMENTATION_NAME = "io.openliberty.microprofile.telemetry";

    private static final String REST_RESOURCE_CLASS = "rest.resource.class";
    private static final String REST_RESOURCE_METHOD = "rest.resource.method";

    private static final String SPAN_CONTEXT = "otel.span.server.context";
    private static final String SPAN_PARENT_CONTEXT = "otel.span.server.parentContext";
    private static final String SPAN_SCOPE = "otel.span.server.scope";

    private static final ServerAttributesExtractor serverAttributesExtractor = new ServerAttributesExtractor();
    private static final NetServerAttributesGetterImpl netServerAttributesGetter = new NetServerAttributesGetterImpl();

    private static final ConcurrentHashMap<RestRouteKey, String> routes = new ConcurrentHashMap<>();

    private static final ReferenceQueue<Class<?>> referenceQueue = new ReferenceQueue<>();

    @SuppressWarnings("unchecked")
    private static void poll() {
        RestRouteKeyWeakReference<Class<?>> key;
        while ((key = (RestRouteKeyWeakReference<Class<?>>) referenceQueue.poll()) != null) {
            routes.remove(key.getOwningKey());
        }
    }

    private static String getRoute(Class<?> restClass, Method restMethod) {
        poll();
        return routes.get(new RestRouteKey(restClass, restMethod));
    }

    /**
     * Add a new route for the specified REST Class and Method.
     *
     * @param restClass
     * @param restMethod
     * @param route
     */
    private static void putRoute(Class<?> restClass, Method restMethod, String route) {
        poll();
        routes.put(new RestRouteKey(referenceQueue, restClass, restMethod), route);
    }

    private static class RestRouteKey {
        private final RestRouteKeyWeakReference<Class<?>> restClassRef;
        private final RestRouteKeyWeakReference<Method> restMethodRef;
        private final int hash;

        RestRouteKey(Class<?> restClass, Method restMethod) {
            this.restClassRef = new RestRouteKeyWeakReference<>(restClass, this);
            this.restMethodRef = new RestRouteKeyWeakReference<>(restMethod, this);
            hash = Objects.hash(restClass, restMethod);
        }

        RestRouteKey(ReferenceQueue<Class<?>> referenceQueue, Class<?> restClass, Method restMethod) {
            this.restClassRef = new RestRouteKeyWeakReference<>(restClass, this, referenceQueue);
            this.restMethodRef = new RestRouteKeyWeakReference<>(restMethod, this);
            hash = Objects.hash(restClass, restMethod);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RestRouteKey other = (RestRouteKey) obj;
            if (!restClassRef.equals(other.restClassRef)) {
                return false;
            }
            if (!restMethodRef.equals(other.restMethodRef)) {
                return false;
            }
            return true;
        }
    }

    private static class RestRouteKeyWeakReference<T> extends WeakReference<T> {
        private final RestRouteKey owningKey;

        RestRouteKeyWeakReference(T referent, RestRouteKey owningKey) {
            super(referent);
            this.owningKey = owningKey;
        }

        RestRouteKeyWeakReference(T referent, RestRouteKey owningKey,
                                  ReferenceQueue<T> referenceQueue) {
            super(referent, referenceQueue);
            this.owningKey = owningKey;
        }

        RestRouteKey getOwningKey() {
            return owningKey;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof RestRouteKeyWeakReference) {
                return get() == ((RestRouteKeyWeakReference) obj).get();
            }

            return false;
        }

        @Override
        public String toString() {
            T referent = get();
            return new StringBuilder("RestRouteKeyWeakReference: ").append(referent).toString();
        }
    }

    private Instrumenter<ContainerRequestContext, ContainerResponseContext> instrumenter;

    @jakarta.ws.rs.core.Context
    ResourceInfo resourceInfo;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    public TelemetryContainerFilter() {
    }

    @Inject
    public TelemetryContainerFilter(final OpenTelemetry openTelemetry) {

        InstrumenterBuilder<ContainerRequestContext, ContainerResponseContext> builder = Instrumenter.builder(
                                                                                                              openTelemetry,
                                                                                                              INSTRUMENTATION_NAME,
                                                                                                              HttpSpanNameExtractor.create(serverAttributesExtractor));

        this.instrumenter = builder
                        .setSpanStatusExtractor(HttpSpanStatusExtractor.create(serverAttributesExtractor))
                        .addAttributesExtractor(HttpServerAttributesExtractor.create(serverAttributesExtractor))
                        .addAttributesExtractor(NetServerAttributesExtractor.create(netServerAttributesGetter))
                        .buildServerInstrumenter(new ContainerRequestContextTextMapGetter());
    }

    @Override
    public void filter(final ContainerRequestContext request) {
        Context parentContext = Context.current();
        if ((!AgentDetection.isAgentActive()) && instrumenter.shouldStart(parentContext, request)) {
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

    private static class NetServerAttributesGetterImpl implements NetServerAttributesGetter<ContainerRequestContext> {

        @Override
        public String transport(ContainerRequestContext request) {
            return SemanticAttributes.NetTransportValues.IP_TCP;
        }

        @Override
        public String hostName(ContainerRequestContext request) {
            return request.getUriInfo().getBaseUri().getHost();
        }

        @Override
        public Integer hostPort(ContainerRequestContext request) {
            return request.getUriInfo().getBaseUri().getPort();
        }

    }

    private static class ServerAttributesExtractor implements HttpServerAttributesGetter<ContainerRequestContext, ContainerResponseContext> {

        @Override
        public String flavor(final ContainerRequestContext request) {
            return null;
        }

        @Override
        public String route(final ContainerRequestContext request) {

            Class<?> resourceClass = (Class<?>) request.getProperty(REST_RESOURCE_CLASS);
            Method resourceMethod = (Method) request.getProperty(REST_RESOURCE_METHOD);

            String route = getRoute(resourceClass, resourceMethod);

            if (route == null) {

                String contextRoot = request.getUriInfo().getBaseUri().getPath();
                UriBuilder template = UriBuilder.fromPath(contextRoot);

                template.path(resourceClass);

                if (resourceMethod.isAnnotationPresent(Path.class)) {
                    template.path(resourceMethod);
                }

                route = template.toTemplate();
                putRoute(resourceClass, resourceMethod, route);
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
        public Integer statusCode(final ContainerRequestContext request, final ContainerResponseContext response, @Nullable Throwable error) {
            return response.getStatus();
        }

        @Override
        public List<String> requestHeader(final ContainerRequestContext request, final String name) {
            return request.getHeaders().getOrDefault(name, emptyList());
        }

        @Override
        public List<String> responseHeader(final ContainerRequestContext request, final ContainerResponseContext response,
                                           final String name) {
            return response.getStringHeaders().getOrDefault(name, emptyList());
        }
    }
}
