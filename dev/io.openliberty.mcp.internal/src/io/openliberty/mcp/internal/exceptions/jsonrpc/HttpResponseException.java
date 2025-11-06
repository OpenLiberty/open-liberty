/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.exceptions.jsonrpc;

import java.util.HashMap;
import java.util.Map;

/**
 * Signals that a specific HTTP status code should be used as the response to this request.
 * <p>
 * The message of this exception will be included in the body of the response to the client as plain text.
 * <p>
 * If required, additional headers can be specified.
 */
public class HttpResponseException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private Map<String, String> headers = new HashMap<>();
    private int statusCode;

    public HttpResponseException(int statusCode) {
        this.statusCode = statusCode;
    }

    public HttpResponseException(int statusCode, String msg) {
        super(msg);
        this.statusCode = statusCode;
    }

    /**
     * Adds a header to the response.
     *
     * @param name the name of the header
     * @param value the value of the header
     * @return this exception instance for method chaining
     */
    public HttpResponseException withHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    /**
     * @return the headers as a map. If none are set then an empty map is returned.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Returns the HTTP status code associated with this exception.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

}
