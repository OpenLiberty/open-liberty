/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient10.headerPropagation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Provider
public class HeaderPropagationFilter implements ContainerRequestFilter, ClientRequestFilter {

    private static ThreadLocal<MultivaluedMap<String, String>> headersMap = new ThreadLocal<>();
    private final List<String> headersToPropagate;

    public HeaderPropagationFilter() {
        headersToPropagate = Arrays.asList((System.getProperty("io.openliberty.propagate.headersToPropagate", "MyHeader").split(",")));
    }

    @Override
    public void filter(ContainerRequestContext reqContext) throws IOException {
        // invoked on incoming request to JAX-RS resource
        // save off the headers we are interested in into a thread local
        MultivaluedMap<String, String> headersFromRequest = reqContext.getHeaders();
        MultivaluedMap<String, String> headerMapToSend = new MultivaluedHashMap<>();
        headersToPropagate.forEach(header -> {
            List<String> valueList = headersFromRequest.get(header);
            if (valueList != null) {
                headerMapToSend.put(header, valueList);
            }
        });

        headersMap.set(headerMapToSend);
    }

    @Override
    public void filter(ClientRequestContext reqContext) throws IOException {
        MultivaluedMap<String, String> headersToSend = headersMap.get();
        if (headersToSend != null && !headersToSend.isEmpty()) {
            MultivaluedMap<String, String> actualHeaders = reqContext.getStringHeaders();
            headersToSend.forEach((header, valueList) -> actualHeaders.put(header, valueList));
        }
        headersMap.remove();
    }
}
