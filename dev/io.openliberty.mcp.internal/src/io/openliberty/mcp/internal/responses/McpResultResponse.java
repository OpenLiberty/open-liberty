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

import io.openliberty.mcp.internal.requests.RequestId;

/**
 *
 */
public class McpResultResponse extends McpResponse {

    /**
     * @param id
     * @param result
     */
    private Object result;

    public McpResultResponse(RequestId id, Object result) {
        super("2.0", id);

        // Per JSON-RPC 2.0 spec, a success response MUST echo the request id
        if (id == null) {
            throw new IllegalArgumentException("id must not be null on a success response");
        }

        // Validate ID is NOT an empty string (numbers are always fine)
        if (id.value() instanceof String s && s.isBlank()) {
            throw new IllegalArgumentException("id must not be an empty string");
        }

        if (result == null) {
            throw new IllegalArgumentException("Result field must be present");
        }
        this.result = result;
    }

    public Object getResult() {
        return result;
    }

}
