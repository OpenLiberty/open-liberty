/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.client.threadleak.server;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Base API REST client with common logic for making HTTP requests.
 */
public abstract class ApiRestClient {

    /**
     * Perform a GET request to the specified endpoint.
     */
    public String doGet(String endpointUrl) {
        String resourceUrl = getResourceUrl(endpointUrl);
        System.out.println("ApiRestClient: GET request to URL [" + resourceUrl + "]");

        WebTarget resource = createResource(resourceUrl);
        try (Response result = resource.request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
            String resultJsonString = result.readEntity(String.class);
            checkStatusCode(result.getStatusInfo(), resultJsonString);
            return resultJsonString;
        }
    }

    /**
     * Check HTTP status code and throw appropriate exceptions.
     */
    protected void checkStatusCode(Response.StatusType status, String resultJsonString) {
        if (status.getFamily() != Response.Status.Family.SUCCESSFUL) {
            String message = "Request failed with statuscode=[" + status.getStatusCode() + "]";
            if (resultJsonString != null && !resultJsonString.isEmpty()) {
                message = message + ", response=[" + resultJsonString + "]";
            }

            if (status.getStatusCode() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new RuntimeException("Entity not found: " + status.getReasonPhrase());
            }
            throw new RuntimeException(message);
        }
    }

    /**
     * Create a WebTarget resource for the given URL.
     * This is the key method that calls client.target(url), which creates a new Bus in CXF.
     */
    protected WebTarget createResource(String resourceUrl) {
        return getClient().resource(resourceUrl, MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * Build the full resource URL from service host, context path, and endpoint.
     */
    protected String getResourceUrl(String endpointUrl) {
        return getServiceHost() + getContextPath() + endpointUrl;
    }

    /**
     * Get the service host (e.g., "http://localhost:8080").
     * Subclasses must implement this to provide varying URLs.
     */
    protected abstract String getServiceHost();

    /**
     * Get the context path (e.g., "/api").
     */
    protected abstract String getContextPath();

    /**
     * Get the RestClient instance (singleton JAX-RS Client wrapper).
     */
    protected abstract RestClient getClient();
}
