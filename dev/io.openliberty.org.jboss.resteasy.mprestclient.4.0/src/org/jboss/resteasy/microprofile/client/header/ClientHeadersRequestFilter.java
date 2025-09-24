/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
// https://github.com/resteasy/resteasy-microprofile/blob/3.0.0.Final/rest-client-base/src/main/java/org/jboss/resteasy/microprofile/client/header/ClientHeadersRequestFilter.java
// https://repo.maven.apache.org/maven2/org/jboss/resteasy/microprofile/microprofile-rest-client-base/3.0.0.Final/microprofile-rest-client-base-3.0.0.Final-sources.jar
/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.resteasy.microprofile.client.header;

import static org.jboss.resteasy.microprofile.client.utils.ListCastUtils.castToListOfStrings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.microprofile.client.impl.MpClientInvocation;
import org.jboss.resteasy.microprofile.client.utils.ClientRequestContextUtils;

/**
 * First the headers from `@ClientHeaderParam` annotations are applied,
 * they can be overwritten by JAX-RS `@HeaderParam` (coming in the `requestContext`)
 *
 * Then, if a `ClientHeadersFactory` is defined, all the headers, together with incoming container headers,
 * are passed to it and it can overwrite them.
 */
@Priority(Integer.MIN_VALUE)
public class ClientHeadersRequestFilter implements ClientRequestFilter {

    private static final MultivaluedMap<String, String> EMPTY_MAP = new MultivaluedHashMap<>();

    private final MultivaluedMap<String, Object> defaultHeaders;

    private final ClientHeaderProviders headerProviders; // Liberty change

    /**
     * Creates a new filter which will add each default header by default to the request headers. Note the values of
     * the {@code defaultHeaders} will be appended and header values are not replaced.
     *
     * @param defaultHeaders the default headers to add
     */
    public ClientHeadersRequestFilter(final MultivaluedMap<String, Object> defaultHeaders, ClientHeaderProviders headerProviders) { // Liberty Change
        this.defaultHeaders = new Headers<>();
        this.defaultHeaders.putAll(defaultHeaders);
        this.headerProviders = headerProviders; // Liberty Change
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        Method method = ClientRequestContextUtils.getMethod(requestContext);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        Optional<ClientHeaderProvider> handler = headerProviders.getProvider(method); // Liberty change
        handler.ifPresent(h -> h.addHeaders(headers));

        Optional<ClientHeadersFactory> factory = headerProviders // Liberty change
                .getFactory(ClientRequestContextUtils.getDeclaringClass(requestContext));

        requestContext.getHeaders().forEach(
                (key, values) -> headers.put(key, castToListOfStrings(values)));

        @SuppressWarnings("unchecked")
        MultivaluedMap<String, String> containerHeaders = (MultivaluedMap<String, String>) requestContext
                .getProperty(MpClientInvocation.CONTAINER_HEADERS);
        if (containerHeaders == null)
            containerHeaders = EMPTY_MAP;
        // stupid final rules
        MultivaluedMap<String, String> incomingHeaders = containerHeaders;

        if (!factory.isPresent() || factory.get() instanceof DefaultClientHeadersFactoryImpl) {
            // When using the default factory, pass the proposed outgoing headers onto the request context.
            // Propagation with the default factory will then overwrite any values if required.
            headers.forEach((key, values) -> requestContext.getHeaders().put(key, castToListOfObjects(values)));
        }

        factory.ifPresent(f -> f.update(incomingHeaders, headers)
                .forEach((key, values) -> requestContext.getHeaders().put(key, castToListOfObjects(values))));

        this.defaultHeaders.forEach((name, values) -> requestContext.getHeaders().addAll(name, values));
    }

    private static List<Object> castToListOfObjects(List<String> values) {
        return new ArrayList<>(values);
    }
}
