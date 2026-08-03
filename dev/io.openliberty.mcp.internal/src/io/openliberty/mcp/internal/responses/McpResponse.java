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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.requests.RequestId;
import jakarta.json.bind.annotation.JsonbNillable;

/**
 * An MCP Response message
 */
@JsonbNillable
public abstract class McpResponse {
    private String jsonrpc;
    private RequestId id;
    private static final TraceComponent tc = Tr.register(McpResponse.class);

    public McpResponse(String jsonrpc, RequestId id) {
        if (!"2.0".equals(jsonrpc))
            throw new IllegalArgumentException(Tr.formatMessage(tc, "jsonrpc field must be present. Only JSONRPC 2.0 is currently supported\"", jsonrpc));

        this.jsonrpc = jsonrpc;
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public RequestId getId() {
        return id;
    }
}
