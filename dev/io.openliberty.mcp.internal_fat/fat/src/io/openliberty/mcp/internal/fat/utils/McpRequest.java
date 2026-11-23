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

import static io.openliberty.mcp.internal.fat.utils.JsonUtil.get;

import org.json.JSONObject;

/**
 * A JSON-RPC request built by {@link McpClient} but not yet sent.
 *
 * <p>Building a request is separate from sending it so that the same request can be sent
 * through {@link McpClient#call(McpRequest)} (and parsed into a response) or through
 * {@link McpClient#callMCPRaw(String)} when a test needs control over the HTTP exchange, e.g.
 * to expect a 403 response. {@link #toString()} returns the JSON body.
 */
public final class McpRequest {

    private final JSONObject json;

    /** @param json the complete JSON-RPC request body, including its {@code id} */
    McpRequest(JSONObject json) {
        this.json = json;
    }

    /** Returns the JSON-RPC {@code id} of the request, which is the id the response must have. */
    public Object getId() {
        return get(json, Object.class, "id");
    }

    @Override
    public String toString() {
        return json.toString();
    }
}
