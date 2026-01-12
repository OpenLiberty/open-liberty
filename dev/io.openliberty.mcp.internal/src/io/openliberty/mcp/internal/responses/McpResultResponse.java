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

import io.openliberty.mcp.request.RequestId;

/**
 *
 */
public class McpResultResponse extends McpResponse {
    private static final TraceComponent tc = Tr.register(McpResultResponse.class);

    /**
     * @param id
     * @param result
     */
    private Object result;

    public McpResultResponse(RequestId id, Object result) {
        super("2.0", id);
        Object rawId = id.value();

        // Validate ID is NOT an empty string (numbers are always fine)
        if (rawId instanceof String s && s.isBlank()) {
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
