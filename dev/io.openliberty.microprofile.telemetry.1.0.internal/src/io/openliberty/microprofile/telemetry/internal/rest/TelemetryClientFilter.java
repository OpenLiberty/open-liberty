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
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
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

    // RestEasy sometimes creates and injects client filters using CDI and sometimes doesn't so we need to work around that
    // See: https://github.com/OpenLiberty/open-liberty/issues/23758
    public void init() {
        synchronized (this) {
            if (instrumenter != null) {
                return;
            }

            OpenTelemetry openTelemetry = CDI.current().select(OpenTelemetry.class).get();
            ClientAttributesExtractor clientAttributesExtractor = new ClientAttributesExtractor();

            InstrumenterBuilder<ClientRequestContext, ClientResponseContext> builder = Instrumenter.builder(openTelemetry,
                                                                                                            "Client filter",
                                                                                                            HttpSpanNameExtractor.create(clientAttributesExtractor));

            this.instrumenter = builder
                            .setSpanStatusExtractor(HttpSpanStatusExtractor.create(clientAttributesExtractor))
                            .addAttributesExtractor(HttpClientAttributesExtractor.create(clientAttributesExtractor))
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
                    Scope scope = spanContext.makeCurrent();
                    request.setProperty(configString + "context", spanContext);
                    request.setProperty(configString + "parentContext", parentContext);
                    request.setProperty(configString + "scope", scope);
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
                Scope scope = (Scope) request.getProperty(configString + "scope");
                if (scope == null) {
                    return null;
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

    private static class ClientAttributesExtractor implements HttpClientAttributesGetter<ClientRequestContext, ClientResponseContext> {

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