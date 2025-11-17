/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
// https://github.com/resteasy/resteasy/blob/4.7.2.Final/resteasy-client-microprofile-base/src/main/java/org/jboss/resteasy/microprofile/client/header/ClientHeadersRequestFilter.java
// https://repo.maven.apache.org/maven2/org/jboss/resteasy/resteasy-client-microprofile-base/4.7.2.Final/resteasy-client-microprofile-base-4.7.2.Final-sources.jar
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

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;
import org.jboss.resteasy.microprofile.client.impl.MpClientInvocation;
import org.jboss.resteasy.microprofile.client.utils.ClientRequestContextUtils;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jboss.resteasy.microprofile.client.utils.ListCastUtils.castToListOfStrings;


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

    // Liberty change start
    private final ClientHeaderProviders headerProviders;

    public ClientHeadersRequestFilter(ClientHeaderProviders headerProviders) {
        this.headerProviders = headerProviders;
    }
    // Liberty change end

    @Override
    public void filter(ClientRequestContext requestContext) {
        Method method = ClientRequestContextUtils.getMethod(requestContext);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        Optional<ClientHeaderProvider> handler = headerProviders.getProvider(method); // Liberty change
        handler.ifPresent(h -> h.addHeaders(headers));

        Optional<ClientHeadersFactory> factory = headerProviders.getFactory(ClientRequestContextUtils.getDeclaringClass(requestContext)); // Liberty change

        requestContext.getHeaders().forEach(
                (key, values) -> headers.put(key, castToListOfStrings(values))
        );

        @SuppressWarnings("unchecked")
        MultivaluedMap<String,String> containerHeaders = (MultivaluedMap<String, String>) requestContext.getProperty(MpClientInvocation.CONTAINER_HEADERS);
        if(containerHeaders == null)
            containerHeaders = EMPTY_MAP;
        // stupid final rules
        MultivaluedMap<String,String> incomingHeaders = containerHeaders;

        if (!factory.isPresent() || factory.get() instanceof DefaultClientHeadersFactoryImpl) {
            // When using the default factory, pass the proposed outgoing headers onto the request context.
            // Propagation with the default factory will then overwrite any values if required.
            headers.forEach((key, values) -> requestContext.getHeaders().put(key, castToListOfObjects(values)));
        }

        factory.ifPresent(f -> f.update(incomingHeaders, headers)
                .forEach((key, values) -> requestContext.getHeaders().put(key, castToListOfObjects(values)))
        );
    }

    private static List<Object> castToListOfObjects(List<String> values) {
        return new ArrayList<>(values);
    }
}
