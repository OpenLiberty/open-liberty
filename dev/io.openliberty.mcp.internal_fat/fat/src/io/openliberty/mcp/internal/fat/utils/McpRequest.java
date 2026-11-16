/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

import org.json.JSONObject;

/**
 * A JSON-RPC request built by {@link McpClient} but not yet sent.
 *
 * <p>Building a request is separate from sending it so that the same request can be sent
 * through {@link McpClient#send(McpRequest)} (and parsed into a response) or through
 * {@link McpClient#callMCPRaw(String)} when a test needs control over the HTTP exchange, e.g.
 * to expect a 403 response. {@link #toString()} returns the JSON body.
 */
public final class McpRequest {

    private final Object id;
    private final String method;
    private final JSONObject json;

    McpRequest(Object id, String method, JSONObject json) {
        this.id = id;
        this.method = method;
        this.json = json;
    }

    /** Returns the JSON-RPC {@code id} of the request. */
    public Object getId() {
        return id;
    }

    /** Returns the JSON-RPC {@code method} of the request. */
    public String getMethod() {
        return method;
    }

    /** Returns the JSON-RPC request body. */
    public JSONObject getJson() {
        return json;
    }

    @Override
    public String toString() {
        return json.toString();
    }
}
