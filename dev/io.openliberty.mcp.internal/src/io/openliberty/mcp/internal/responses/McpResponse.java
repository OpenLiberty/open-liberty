/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.responses;

import io.openliberty.mcp.internal.requests.McpRequestId;
import jakarta.json.bind.annotation.JsonbNillable;

/**
 * An MCP Response message
 */
@JsonbNillable
public abstract class McpResponse {
    private String jsonrpc;
    private McpRequestId id;

    public McpResponse(String jsonrpc, McpRequestId id) {
        if (jsonrpc == null || !jsonrpc.equals("2.0"))
            throw new IllegalArgumentException("jsonrpc field must be present. Only JSONRPC 2.0 is currently supported");
        if (id == null)
            throw new IllegalArgumentException("id must not be null");

        this.jsonrpc = jsonrpc;
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public McpRequestId getId() {
        return id;
    }
}
