/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.exceptions.jsonrpc;

/**
 * Signals that a JSON-RPC error response should be returned for this request
 */
public sealed class JSONRPCException extends McpResponseException permits MCPRequestValidationException {
    private static final long serialVersionUID = 1L;
    private JSONRPCErrorCode errorCode;
    private Object data;
    private int statusCode;

    public JSONRPCException(JSONRPCErrorCode errorCode, Object data) {
        this(errorCode, data, 0);
    }

    /**
     * @param errorCode the JSON-RPC error code
     * @param data the error data
     * @param statusCode the HTTP status code the response should be sent with, or 0 to use the default status
     */
    public JSONRPCException(JSONRPCErrorCode errorCode, Object data, int statusCode) {
        this.errorCode = errorCode;
        this.data = data;
        this.statusCode = statusCode;
    }

    /**
     * @return the errorCode
     */
    public JSONRPCErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * @return the data
     */
    public Object getData() {
        return data;
    }

    /**
     * @return the HTTP status code the response should be sent with. Only meaningful if {@link #hasStatusCode()}
     *         is true.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return true if this error should be sent with a specific HTTP status code, rather than the default
     */
    public boolean hasStatusCode() {
        return statusCode != 0;
    }

}
