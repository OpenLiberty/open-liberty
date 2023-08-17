/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.rest;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.microprofile.telemetry.internal.cdi.OpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.helper.AgentDetection;
import io.opentelemetry.api.GlobalOpenTelemetry;
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
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
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
public class TelemetryServletFilter implements Filter {

    private static final TraceComponent tc = Tr.register(TelemetryServletFilter.class);

    private static final String INSTRUMENTATION_NAME = "io.openliberty.microprofile.telemetry";
    private static final HttpServerAttributesGetterImpl HTTP_SERVER_ATTRIBUTES_GETTER = new HttpServerAttributesGetterImpl();
    private static final NetServerAttributesGetterImpl NET_SERVER_ATTRIBUTES_GETTER = new NetServerAttributesGetterImpl();

    private static final String SPAN_CONTEXT = "otel.span.http.context";
    private static final String SPAN_PARENT_CONTEXT = "otel.span.http.parentContext";
    public static final String SPAN_SCOPE = "otel.span.http.scope";

    private Instrumenter<ServletRequest, ServletResponse> instrumenter;

    private static final String CONFIG_METRICS_EXPORTER_PROPERTY = "otel.metrics.exporter";
    private static final String CONFIG_LOGS_EXPORTER_PROPERTY = "otel.logs.exporter";
    private static final String ENV_METRICS_EXPORTER_PROPERTY = "OTEL_METRICS_EXPORTER";
    private static final String ENV_LOGS_EXPORTER_PROPERTY = "OTEL_LOGS_EXPORTER";
    private static final String ENV_DISABLE_PROPERTY = "OTEL_SDK_DISABLED";
    private static final String CONFIG_DISABLE_PROPERTY = "otel.sdk.disabled";
    private static final String SERVICE_NAME_PROPERTY = "otel.service.name";

    private final Config config = ConfigProvider.getConfig();

    public TelemetryServletFilter() {
        System.out.println("FW TelemetryServletFilter ctor default");
    }

    @Override
    public void init(FilterConfig config) {
        if (instrumenter == null) {
//            OpenTelemetryInfo otelInfo = CDI.current().select(OpenTelemetryInfo.class).get();
            OpenTelemetryInfo otelInfo = getOpenTelemetryInfo();
            System.out.println("FW TelemetryServletFilter init instance=" + System.identityHashCode(this));
            System.out.println("FW TelemetryServletFilter init otelInfo=" + otelInfo);
            System.out.println("FW TelemetryServletFilter init otelInfo.getEnabled()=" + otelInfo.getEnabled());

            if (otelInfo != null && otelInfo.getEnabled() && !AgentDetection.isAgentActive()) {
                InstrumenterBuilder<ServletRequest, ServletResponse> builder = Instrumenter.builder(
                                                                                                    otelInfo.getOpenTelemetry(),
                                                                                                    INSTRUMENTATION_NAME,
                                                                                                    HttpSpanNameExtractor.create(HTTP_SERVER_ATTRIBUTES_GETTER));

                instrumenter = builder
                                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(HTTP_SERVER_ATTRIBUTES_GETTER))
                                .addAttributesExtractor(HttpServerAttributesExtractor.create(HTTP_SERVER_ATTRIBUTES_GETTER))
                                .addAttributesExtractor(NetServerAttributesExtractor.create(NET_SERVER_ATTRIBUTES_GETTER))
                                .buildServerInstrumenter(new ContainerRequestContextTextMapGetter());
                System.out.println("FW TelemetryServletFilter init instrumenter is set");

            } else {
                instrumenter = null;
                System.out.println("FW TelemetryServletFilter init instrumenter is NULL");
            }

        }
    }

    /** {@inheritDoc} */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        System.out.println("FW TelemetryServletFilter doFilter instance=" + System.identityHashCode(this));

        System.out.println("FW isAsyncStarted=" + request.isAsyncStarted());

        System.out.println("FW instrumenter=" + instrumenter);
        if (instrumenter != null) {
            Context parentContext = Context.current();
            if (instrumenter.shouldStart(parentContext, request)) {
                System.out.println("FW TelemetryServletFilter doFilter start span");
                Context spanContext = instrumenter.start(parentContext, request);
                Scope scope = spanContext.makeCurrent();
                request.setAttribute(SPAN_CONTEXT, spanContext);
                request.setAttribute(SPAN_PARENT_CONTEXT, parentContext);
                request.setAttribute(SPAN_SCOPE, scope);
            }
        }
        System.out.println("FW TelemetryServletFilter doFilter before filter");

        chain.doFilter(request, response);

        System.out.println("FW TelemetryServletFilter doFilter after filter");

        System.out.println("FW isAsyncStarted=" + request.isAsyncStarted());

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
    }

    private void endSpan(ServletRequest request, ServletResponse response, Throwable throwable) {
        if (instrumenter != null) {
            Context spanContext = (Context) request.getAttribute(SPAN_CONTEXT);
            if (spanContext == null) {
                return;
            }

            try {
                System.out.println("FW TelemetryServletFilter doFilter end span");
                instrumenter.end(spanContext, request, response, throwable);
            } finally {
                request.removeAttribute(SPAN_CONTEXT);
                request.removeAttribute(SPAN_PARENT_CONTEXT);
            }
        }
    }

    private static class ContainerRequestContextTextMapGetter implements TextMapGetter<ServletRequest> {

        @Override
        public Iterable<String> keys(final ServletRequest request) {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                System.out.println("FW ContainerRequestContextTextMapGetter keys=" + httpServletRequest.getHeaderNames());
                return new HashSet<String>(Collections.list(httpServletRequest.getHeaderNames()));
            }
            System.out.println("FW ContainerRequestContextTextMapGetter keys=EMPTY");
            return Collections.emptyList();
        }

        @Override
        public String get(final ServletRequest request, final String key) {
            if (request == null) {
                System.out.println("FW ContainerRequestContextTextMapGetter get key=" + key + " value=NULL");
                return null;
            }

            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                System.out.println("FW ContainerRequestContextTextMapGetter get key=" + key + " value=" + httpServletRequest.getHeader(key));
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
                return "FW:" + httpServletRequest.getRequestURI();
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
                return httpServletRequest.getRequestURI();
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
                return httpServletResponse.getHeaders(name).stream().collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }

    private OpenTelemetryInfo getOpenTelemetryInfo() {
        if (AgentDetection.isAgentActive()) {
            // If we're using the agent, it will have set GlobalOpenTelemetry and we must use its instance
            // all config is handled by the agent in this case
            return new OpenTelemetryInfo(true, GlobalOpenTelemetry.get());
        }

        HashMap<String, String> telemetryProperties = getTelemetryProperties();

        //Builds tracer provider if user has enabled tracing aspects with config properties
        if (!checkDisabled(telemetryProperties)) {
            OpenTelemetry openTelemetry = AccessController.doPrivileged((PrivilegedAction<OpenTelemetry>) () -> {
                return AutoConfiguredOpenTelemetrySdk.builder()
                                .addPropertiesCustomizer(x -> telemetryProperties) //Overrides OpenTelemetry's property order
                                .addResourceCustomizer(this::customizeResource)//Defaults service name to application name
                                .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                                .setResultAsGlobal(false)
                                .registerShutdownHook(false)
                                .build()
                                .getOpenTelemetrySdk();
            });

            if (openTelemetry != null) {
                return new OpenTelemetryInfo(true, openTelemetry);
            }
        }
        //By default, MicroProfile Telemetry tracing is off.
        //The absence of an installed SDK is a “no-op” API
        //Operations on a Tracer, or on Spans have no side effects and do nothing
        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        String applicationName = cData.getJ2EEName().getApplication();
        Tr.info(tc, "CWMOT5100.tracing.is.disabled", applicationName);

        return new OpenTelemetryInfo(false, OpenTelemetry.noop());
    }

    private HashMap<String, String> getTelemetryProperties() {
        HashMap<String, String> telemetryProperties = new HashMap<>();
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith("otel.")) {
                config.getOptionalValue(propertyName, String.class).ifPresent(
                                                                              value -> telemetryProperties.put(propertyName, value));
            }
        }
        //Metrics and logs are disabled by default
        telemetryProperties.put(CONFIG_METRICS_EXPORTER_PROPERTY, "none");
        telemetryProperties.put(CONFIG_LOGS_EXPORTER_PROPERTY, "none");
        telemetryProperties.put(ENV_METRICS_EXPORTER_PROPERTY, "none");
        telemetryProperties.put(ENV_LOGS_EXPORTER_PROPERTY, "none");
        return telemetryProperties;
    }

    private boolean checkDisabled(Map<String, String> oTelConfigs) {
        //In order to enable any of the tracing aspects, the configuration otel.sdk.disabled=false must be specified in any of the configuration sources available via MicroProfile Config.
        if (oTelConfigs.get(ENV_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(ENV_DISABLE_PROPERTY));
        } else if (oTelConfigs.get(CONFIG_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(CONFIG_DISABLE_PROPERTY));
        }
        return true;
    }

    //Adds the service name to the resource attributes
    private Resource customizeResource(Resource resource, ConfigProperties c) {
        ResourceBuilder builder = resource.toBuilder();
        builder.put(ResourceAttributes.SERVICE_NAME, getServiceName(c));
        return builder.build();
    }

    //Uses application name if the user has not given configured service.name resource attribute
    private String getServiceName(ConfigProperties c) {
        String appName = c.getString(SERVICE_NAME_PROPERTY);
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (appName == null) {
            if (cmd != null) {
                appName = cmd.getModuleMetaData().getApplicationMetaData().getName();
            }
        }

        return appName;
    }
}
