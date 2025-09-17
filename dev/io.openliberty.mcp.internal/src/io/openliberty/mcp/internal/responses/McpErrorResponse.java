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

import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.McpRequestId;

/**
 *
 */
public class McpErrorResponse extends McpResponse {

    /**
     * @param jsonrpc
     * @param id
     */
    private Error error;

    public McpErrorResponse(McpRequestId id, JSONRPCException e) {
        super("2.0", id);
        this.error = new Error(e.getErrorCode().getCode(), e.getErrorCode().getMessage(), e.getData());

    }

    public McpErrorResponse(McpRequestId id, Error e) {
        super("2.0", id);
        this.error = e;

    }

    public Error getError() {
        return error;
    }

    public static record Error(int code, String message, Object data) {

    }

}
