/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry10.internal.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.microprofile.telemetry.internal.common.AgentDetection;
import io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.common.rest.AbstractTelemetryServletFilter;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;
import io.opentelemetry.api.trace.Span;
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
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TelemetryServletFilter extends AbstractTelemetryServletFilter implements Filter {

    private static final TraceComponent tc = Tr.register(TelemetryServletFilter.class);

    private static final String INSTRUMENTATION_NAME = "io.openliberty.microprofile.telemetry";
    private static final HttpServerAttributesGetterImpl HTTP_SERVER_ATTRIBUTES_GETTER = new HttpServerAttributesGetterImpl();
    private static final NetServerAttributesGetterImpl NET_SERVER_ATTRIBUTES_GETTER = new NetServerAttributesGetterImpl();

    private Instrumenter<ServletRequest, ServletResponse> instrumenter;
    private volatile boolean lazyCreate = false;
    private final AtomicReference<Instrumenter<ServletRequest, ServletResponse>> lazyInstrumenter = new AtomicReference<>();

    public TelemetryServletFilter() {
    }

    private final Config config = ConfigProvider.getConfig();

    @Override
    public void init(FilterConfig config) {
        if (!CheckpointPhase.getPhase().restored()) {
            lazyCreate = true;
        } else {
            instrumenter = createInstrumenter();
        }
    }

    private Instrumenter<ServletRequest, ServletResponse> getInstrumenter() {
        if (instrumenter != null) {
            return instrumenter;
        }
        if (lazyCreate) {
            instrumenter = lazyInstrumenter.updateAndGet((i) -> {
                if (i == null) {
                    return createInstrumenter();
                } else {
                    return i;
                }
            });
            lazyCreate = false;
        }
        return instrumenter;
    }

    private Instrumenter<ServletRequest, ServletResponse> createInstrumenter() {
        // Check if the HTTP tracing should be disabled
        boolean httpTracingDisabled = config.getOptionalValue(CONFIG_DISABLE_HTTP_TRACING_PROPERTY, Boolean.class).orElse(false);
        OpenTelemetryInfo otelInfo = OpenTelemetryAccessor.getOpenTelemetryInfo();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, CONFIG_DISABLE_HTTP_TRACING_PROPERTY + "=" + httpTracingDisabled);
            Tr.debug(tc, "otelInfo.getEnabled()=" + otelInfo.getEnabled());
        }
        if (otelInfo != null &&
            otelInfo.getEnabled() &&
            !AgentDetection.isAgentActive() &&
            !httpTracingDisabled) {
            InstrumenterBuilder<ServletRequest, ServletResponse> builder = Instrumenter.builder(
                                                                                                otelInfo.getOpenTelemetry(),
                                                                                                INSTRUMENTATION_NAME,
                                                                                                HttpSpanNameExtractor.create(HTTP_SERVER_ATTRIBUTES_GETTER));

            Instrumenter<ServletRequest, ServletResponse> result = builder.setSpanStatusExtractor(HttpSpanStatusExtractor.create(HTTP_SERVER_ATTRIBUTES_GETTER))
                            .addAttributesExtractor(HttpServerAttributesExtractor.create(HTTP_SERVER_ATTRIBUTES_GETTER))
                            .addAttributesExtractor(NetServerAttributesExtractor.create(NET_SERVER_ATTRIBUTES_GETTER))
                            .buildServerInstrumenter(new ServletRequestContextTextMapGetter());
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "instrumenter is initialized");
            }
            return result;
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "instrumenter is set to null");
            }
            return null;
        }

    }

    /** {@inheritDoc} */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Scope scope = null;
        Instrumenter<ServletRequest, ServletResponse> current = getInstrumenter();
        if (current != null) {
            Context parentContext = Context.current();
            if (current.shouldStart(parentContext, request)) {
                Context spanContext = current.start(parentContext, request);
                scope = spanContext.makeCurrent();
                request.setAttribute(SPAN_CONTEXT, spanContext);
                request.setAttribute(SPAN_PARENT_CONTEXT, parentContext);
                request.setAttribute(SPAN_SCOPE, scope);

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Span traceId=" + Span.current().getSpanContext().getTraceId() + ", spanId=" + Span.current().getSpanContext().getSpanId());
                }
            }
        }

        chain.doFilter(request, response);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "isAsyncStarted=" + request.isAsyncStarted());
        }

        if (request.isAsyncStarted()) {

            AsyncContext asyncContext = request.getAsyncContext();
            asyncContext.addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) throws IOException {
                    endSpan(request, response, null, current);
                }

                @Override
                public void onTimeout(AsyncEvent event) throws IOException {
                    endSpan(request, response, event.getThrowable(), current);
                }

                @Override
                public void onError(AsyncEvent event) throws IOException {
                    endSpan(request, response, event.getThrowable(), current);
                }

                @Override
                public void onStartAsync(AsyncEvent event) throws IOException {
                    // A new async cycle is starting, we need to re-register ourself
                    event.getAsyncContext().addListener(this);
                }
            });
        } else {
            endSpan(request, response, null, current);
        }

        if (scope != null) {
            scope.close();
            request.removeAttribute(SPAN_SCOPE);
        }

    }

    private void endSpan(ServletRequest request, ServletResponse response, Throwable throwable, Instrumenter<ServletRequest, ServletResponse> current) {
        Context spanContext = (Context) request.getAttribute(SPAN_CONTEXT);
        if (spanContext == null) {
            return;
        }

        try {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "End span traceId=" + Span.fromContext(spanContext).getSpanContext().getTraceId() + " spanId="
                             + Span.fromContext(spanContext).getSpanContext().getSpanId());
            }
            current.end(spanContext, request, response, throwable);
        } finally {
            request.removeAttribute(SPAN_CONTEXT);
            request.removeAttribute(SPAN_PARENT_CONTEXT);
        }
    }

    private static class ServletRequestContextTextMapGetter implements TextMapGetter<ServletRequest> {

        @Override
        public Iterable<String> keys(final ServletRequest request) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                return new HashSet<String>(Collections.list(httpServletRequest.getHeaderNames()));
            }
            return Collections.emptyList();
        }

        @Override
        public String get(final ServletRequest request, final String key) {
            if (request == null) {
                return null;
            }

            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                return httpServletRequest.getHeader(key);
            }
            return null;
        }
    }

    private static class NetServerAttributesGetterImpl implements NetServerAttributesGetter<ServletRequest> {

        @Override
        public String transport(ServletRequest request) {
            return SemanticAttributes.NetTransportValues.IP_TCP;
        }

        @Override
        public String hostName(ServletRequest request) {
            return request.getServerName();
        }

        @Override
        public Integer hostPort(ServletRequest request) {
            return request.getServerPort();
        }

    }

    private static class HttpServerAttributesGetterImpl implements HttpServerAttributesGetter<ServletRequest, ServletResponse> {

        @Override
        public String flavor(final ServletRequest request) {
            return null;
        }

        @Override
        public String route(final ServletRequest request) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                return httpServletRequest.getContextPath() + httpServletRequest.getServletPath();
            }
            return null;
        }

        @Override
        public String method(final ServletRequest request) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                return httpServletRequest.getMethod();
            }
            return null;
        }

        @Override
        public String target(final ServletRequest request) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                String path = httpServletRequest.getRequestURI();
                String query = httpServletRequest.getQueryString();
                if (path != null && query != null && !query.isEmpty()) {
                    return path + "?" + query;
                } else {
                    return path;
                }
            }
            return null;
        }

        @Override
        public String scheme(final ServletRequest request) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                return httpServletRequest.getScheme();
            }
            return null;
        }

        @Override
        public Integer statusCode(final ServletRequest request, final ServletResponse response, @Nullable Throwable error) {
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                return httpServletResponse.getStatus();
            }
            return null;
        }

        @Override
        public List<String> requestHeader(final ServletRequest request, final String name) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                return Collections.list(httpServletRequest.getHeaders(name));
            }
            return Collections.emptyList();
        }

        @Override
        public List<String> responseHeader(final ServletRequest request, final ServletResponse response,
                                           final String name) {
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                return new ArrayList<>(httpServletResponse.getHeaders(name));
            }
            return Collections.emptyList();
        }
    }

}
