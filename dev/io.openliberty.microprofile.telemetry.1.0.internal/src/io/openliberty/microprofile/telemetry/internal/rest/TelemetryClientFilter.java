/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import static io.openliberty.microprofile.telemetry.internal.helper.AgentDetection.isAgentActive;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import jakarta.annotation.Nullable;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TelemetryClientFilter implements ClientRequestFilter, ClientResponseFilter {

    private Instrumenter<ClientRequestContext, ClientResponseContext> instrumenter;

    private final String configString = "otel.span.client.";

    private static final NetClientAttributesGetterImpl NET_CLIENT_ATTRIBUTES_GETTER = new NetClientAttributesGetterImpl();
    private static final HttpClientAttributesGetterImpl HTTP_CLIENT_ATTRIBUTES_GETTER = new HttpClientAttributesGetterImpl();

    /**
     * Retrieve the TelemetryClientFilter for the current application using CDI
     * <p>
     * Implementation note: It's important that there's a class which is registered as a CDI bean on the stack from this bundle when {@code CDI.current()} is called so that CDI
     * finds the correct BDA and bean manager.
     * <p>
     * Calling it from this static method ensures that {@code TelemetryClientFilter} is the first thing on the stack and CDI will find the right BDA.
     *
     * @return the TelemetryClientFilter for the current application
     */
    public static TelemetryClientFilter getCurrent() {
        return CDI.current().select(TelemetryClientFilter.class).get();
    }

    // RestEasy sometimes creates and injects client filters using CDI and sometimes doesn't so we need to work around that
    // See: https://github.com/OpenLiberty/open-liberty/issues/23758
    public void init() {
        synchronized (this) {
            if (instrumenter != null) {
                return;
            }

            OpenTelemetry openTelemetry = CDI.current().select(OpenTelemetry.class).get();
            InstrumenterBuilder<ClientRequestContext, ClientResponseContext> builder = Instrumenter.builder(openTelemetry,
                                                                                                            "Client filter",
                                                                                                            HttpSpanNameExtractor.create(HTTP_CLIENT_ATTRIBUTES_GETTER));

            this.instrumenter = builder
                            .setSpanStatusExtractor(HttpSpanStatusExtractor.create(HTTP_CLIENT_ATTRIBUTES_GETTER))
                            .addAttributesExtractor(HttpClientAttributesExtractor.create(HTTP_CLIENT_ATTRIBUTES_GETTER))
                            .addAttributesExtractor(NetClientAttributesExtractor.create(NET_CLIENT_ATTRIBUTES_GETTER))
                            .buildClientInstrumenter(new ClientRequestContextTextMapSetter());
        }
    }

    @Override
    public void filter(final ClientRequestContext request) {
        init();

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Context parentContext = Context.current();
                if ((!isAgentActive()) && instrumenter.shouldStart(parentContext, request)) {
                    Context spanContext = instrumenter.start(parentContext, request);
                    request.setProperty(configString + "context", spanContext);
                    request.setProperty(configString + "parentContext", parentContext);
                }
                return null;
            }
        });
    }

    @Override
    public void filter(final ClientRequestContext request, final ClientResponseContext response) {
        init();

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

    private static class ClientRequestContextTextMapSetter implements TextMapSetter<ClientRequestContext> {
        @Override
        public void set(final ClientRequestContext carrier, final String key, final String value) {
            if (carrier != null) {
                carrier.getHeaders().put(key, singletonList(value));
            }
        }
    }

    private static class NetClientAttributesGetterImpl implements NetClientAttributesGetter<ClientRequestContext, ClientResponseContext> {

        @Override
        public String transport(ClientRequestContext request, ClientResponseContext response) {
            return SemanticAttributes.NetTransportValues.IP_TCP;
        }

        @Override
        public String peerName(ClientRequestContext request) {
            return request.getUri().getHost();
        }

        @Override
        public Integer peerPort(ClientRequestContext request) {
            return request.getUri().getPort();
        }
    }

    private static class HttpClientAttributesGetterImpl implements HttpClientAttributesGetter<ClientRequestContext, ClientResponseContext> {

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
        public Integer statusCode(final ClientRequestContext request, final ClientResponseContext response, @Nullable Throwable error) {
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
        public List<String> responseHeader(final ClientRequestContext request, final ClientResponseContext response,
                                           final String name) {
            return response.getHeaders().getOrDefault(name, emptyList());
        }
    }
}
