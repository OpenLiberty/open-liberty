/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry11.internal.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.telemetry.internal.common.AgentDetection;
import io.openliberty.microprofile.telemetry.internal.common.OpenTelemetryInfo;
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

    private Instrumenter<ServletRequest, ServletResponse> instrumenter;

    private final Config config = ConfigProvider.getConfig();

    public TelemetryServletFilter() {
    }

    @Override
    public void init(FilterConfig config) {
        if (instrumenter == null) {
            OpenTelemetryInfo otelInfo = OpenTelemetryAccessor.getOpenTelemetryInfo();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "otelInfo.getEnabled()=" + otelInfo.getEnabled());
            }
            if (otelInfo != null &&
                otelInfo.getEnabled() &&
                !AgentDetection.isAgentActive() &&
                !checkDisabled(getTelemetryProperties())) {
                InstrumenterBuilder<ServletRequest, ServletResponse> builder = Instrumenter.builder(
                                                                                                    otelInfo.getOpenTelemetry(),
                                                                                                    INSTRUMENTATION_NAME,
                                                                                                    HttpSpanNameExtractor.create(HTTP_SERVER_ATTRIBUTES_GETTER));

                this.instrumenter = builder
                                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(HTTP_SERVER_ATTRIBUTES_GETTER))
                                .addAttributesExtractor(HttpServerAttributesExtractor.create(HTTP_SERVER_ATTRIBUTES_GETTER))
                                .buildServerInstrumenter(new ServletRequestContextTextMapGetter());
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "instrumenter is initialized");
                }
            } else {
                instrumenter = null;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "instrumenter is set to null");
                }
            }

        }
    }

    /** {@inheritDoc} */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Scope scope = null;
        if (instrumenter != null) {
            Context parentContext = Context.current();
            if (instrumenter.shouldStart(parentContext, request)) {
                Context spanContext = instrumenter.start(parentContext, request);
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
                    endSpan(request, response, null);
                }

                @Override
                public void onTimeout(AsyncEvent event) throws IOException {
                    endSpan(request, response, event.getThrowable());
                }

                @Override
                public void onError(AsyncEvent event) throws IOException {
                    endSpan(request, response, event.getThrowable());
                }

                @Override
                public void onStartAsync(AsyncEvent event) throws IOException {
                }
            });
        } else {
            endSpan(request, response, null);
        }

        if (scope != null) {
            scope.close();
            request.removeAttribute(SPAN_SCOPE);
        }

    }

    private void endSpan(ServletRequest request, ServletResponse response, Throwable throwable) {
        if (instrumenter != null) {
            Context spanContext = (Context) request.getAttribute(SPAN_CONTEXT);
            if (spanContext == null) {
                return;
            }

            try {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "End span traceId=" + Span.fromContext(spanContext).getSpanContext().getTraceId() + " spanId="
                                 + Span.fromContext(spanContext).getSpanContext().getSpanId());
                }
                instrumenter.end(spanContext, request, response, throwable);
            } finally {
                request.removeAttribute(SPAN_CONTEXT);
                request.removeAttribute(SPAN_PARENT_CONTEXT);
            }
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

    private static class HttpServerAttributesGetterImpl implements HttpServerAttributesGetter<ServletRequest, ServletResponse> {

        @Override
        public String getHttpRoute(final ServletRequest request) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                return httpServletRequest.getRequestURI();
            }
            return null;
        }

        @Override
        public String getHttpRequestMethod(final ServletRequest request) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                return httpServletRequest.getMethod();
            }
            return null;
        }

//        @Override
//        public String target(final ServletRequest request) {
//            if (request instanceof HttpServletRequest) {
//                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
//                return httpServletRequest.getRequestURI();
//            }
//            return null;
//        }

        @Override
        public String getUrlPath(ServletRequest request) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                return httpServletRequest.getRequestURI();
            }
            return null;
        }

        @Override
        public String getUrlQuery(ServletRequest request) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                return httpServletRequest.getQueryString();
            }
            return null;
        }

        @Override
        public String getUrlScheme(final ServletRequest request) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                return httpServletRequest.getScheme();
            }
            return null;
        }

        @Override
        public Integer getHttpResponseStatusCode(final ServletRequest request, final ServletResponse response, @Nullable Throwable error) {
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                return httpServletResponse.getStatus();
            }
            return null;
        }

        @Override
        public List<String> getHttpRequestHeader(final ServletRequest request, final String name) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                return Collections.list(httpServletRequest.getHeaders(name));
            }
            return Collections.emptyList();
        }

        @Override
        public List<String> getHttpResponseHeader(final ServletRequest request, final ServletResponse response,
                                                  final String name) {
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                return httpServletResponse.getHeaders(name).stream().collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

    }

    private HashMap<String, String> getTelemetryProperties() {
        HashMap<String, String> telemetryProperties = new HashMap<>();
        for (String propertyName : config.getPropertyNames()) {

            if (propertyName.startsWith("otel.")) {
                config.getOptionalValue(propertyName, String.class).ifPresent(value -> telemetryProperties.put(propertyName, value));
            }
        }
        return telemetryProperties;
    }

    /**
     * Check if the HTTP tracing should be disabled
     *
     * @param oTelConfigs
     * @return false (default)
     * @return true if either ENV_DISABLE_HTTP_TRACING_PROPERTY or CONFIG_DISABLE_HTTP_TRACING_PROPERTY equal true
     */
    private boolean checkDisabled(Map<String, String> oTelConfigs) {
        //In order to enable any of the tracing aspects, the configuration otel.sdk.disabled=false must be specified in any of the configuration sources available via MicroProfile Config.
        if (oTelConfigs.get(ENV_DISABLE_HTTP_TRACING_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(ENV_DISABLE_HTTP_TRACING_PROPERTY));
        } else if (oTelConfigs.get(CONFIG_DISABLE_HTTP_TRACING_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(CONFIG_DISABLE_HTTP_TRACING_PROPERTY));
        }
        return false;
    }

}
