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
package io.openliberty.microprofile.telemetry11.internal.rest;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import io.openliberty.microprofile.telemetry.internal.common.AgentDetection;
import io.openliberty.microprofile.telemetry.internal.common.OpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.common.rest.AbstractTelemetryClientFilter;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TelemetryClientFilter extends AbstractTelemetryClientFilter implements ClientRequestFilter, ClientResponseFilter {

    private final Instrumenter<ClientRequestContext, ClientResponseContext> instrumenter;

    private final String configString = "otel.span.client.";

    private static final HttpClientAttributesGetterImpl HTTP_CLIENT_ATTRIBUTES_GETTER = new HttpClientAttributesGetterImpl();

    TelemetryClientFilter() {
        final OpenTelemetryInfo openTelemetryInfo = OpenTelemetryAccessor.getOpenTelemetryInfo();
        if (openTelemetryInfo.getEnabled() && !AgentDetection.isAgentActive()) {
            InstrumenterBuilder<ClientRequestContext, ClientResponseContext> builder = Instrumenter.builder(openTelemetryInfo.getOpenTelemetry(),
                                                                                                            "Client filter",
                                                                                                            HttpSpanNameExtractor.create(HTTP_CLIENT_ATTRIBUTES_GETTER));

            this.instrumenter = builder
                            .setSpanStatusExtractor(HttpSpanStatusExtractor.create(HTTP_CLIENT_ATTRIBUTES_GETTER))
                            .addAttributesExtractor(HttpClientAttributesExtractor.create(HTTP_CLIENT_ATTRIBUTES_GETTER))
                            .buildClientInstrumenter(new ClientRequestContextTextMapSetter());
        } else {
            this.instrumenter = null;
        }
    }

    @Override
    public void filter(final ClientRequestContext request) {
        if (instrumenter != null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    Context parentContext = Context.current();
                    if (instrumenter.shouldStart(parentContext, request)) {
                        Context spanContext = instrumenter.start(parentContext, request);
                        request.setProperty(configString + "context", spanContext);
                        request.setProperty(configString + "parentContext", parentContext);
                    }
                    return null;
                }
            });
        }
    }

    @Override
    public void filter(final ClientRequestContext request, final ClientResponseContext response) {
        if (instrumenter != null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    Context spanContext = (Context) request.getProperty(configString + "context");
                    if (spanContext == null) {
                        return null;
                    }
                    try {
                        instrumenter.end(spanContext, request, response, null);
                    } finally {
                        request.removeProperty(configString + "context");
                        request.removeProperty(configString + "parentContext");
                    }
                    return null;
                }
            });
        }
    }

    /**
     * @return false if OpenTelemetry is disabled
     *         Indicated by instrumenter being set to null
     */
    @Override
    public boolean isEnabled() {
        return instrumenter != null;
    }

    private static class ClientRequestContextTextMapSetter implements TextMapSetter<ClientRequestContext> {
        @Override
        public void set(final ClientRequestContext carrier, final String key, final String value) {
            if (carrier != null) {
                carrier.getHeaders().put(key, singletonList(value));
            }
        }
    }

    private static class HttpClientAttributesGetterImpl implements HttpClientAttributesGetter<ClientRequestContext, ClientResponseContext> {

        //Required
        @Override
        public String getHttpRequestMethod(final ClientRequestContext request) {
            return request.getMethod();
        }

        //If one was sent
        @Override
        public Integer getHttpResponseStatusCode(final ClientRequestContext request, final ClientResponseContext response, @Nullable Throwable error) {
            return response.getStatus();
        }

        @Override
        public String getUrlFull(final ClientRequestContext request) {
            return request.getUri().toString();
        }

        @Override
        public List<String> getHttpRequestHeader(final ClientRequestContext request, final String name) {
            return request.getStringHeaders().getOrDefault(name, emptyList());
        }

        @Override
        public List<String> getHttpResponseHeader(final ClientRequestContext request, final ClientResponseContext response,
                                                  final String name) {
            return response.getHeaders().getOrDefault(name, emptyList());
        }

        @Nullable
        @Override
        public String getServerAddress(final ClientRequestContext request) {
            return request.getUri().getHost();
        }

        @Nullable
        @Override
        public Integer getServerPort(final ClientRequestContext request) {
            return request.getUri().getPort();
        }

        @Override
        public String getTransport(ClientRequestContext request, ClientResponseContext response) {
            return SemanticAttributes.NetTransportValues.IP_TCP;
        }
    }
}