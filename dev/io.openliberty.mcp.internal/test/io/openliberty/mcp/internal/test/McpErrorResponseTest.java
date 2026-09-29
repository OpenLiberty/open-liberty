/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.RequestId;
import io.openliberty.mcp.internal.responses.McpErrorResponse;

/**
 * Tests for {@link McpErrorResponse} construction, covering both path
 * and the null-id case used when the request id could not be determined (e.g.
 * parse errors and invalid-request errors per JSON-RPC 2.0 spec).
 */
public class McpErrorResponseTest {

    /**
     * A valid request id must produce an error response without error.
     */
    @Test
    public void errorResponseBuildsWithValidId() {
        RequestId id = new RequestId("42");
        McpErrorResponse response = new McpErrorResponse(id,
                                                         new JSONRPCException(JSONRPCErrorCode.INTERNAL_ERROR, "oops"));
        assertNotNull(response.getError());
    }

    /**
     * A null id must be accepted and produce an error response whose id field is null.
     * This is required by the JSON-RPC 2.0 spec for parse errors and invalid-request errors
     * where the id of the original request cannot be determined.
     */
    @Test
    public void errorResponseBuildsWithNullId() {
        McpErrorResponse response = new McpErrorResponse(null,
                                                         new JSONRPCException(JSONRPCErrorCode.INTERNAL_ERROR, "oops"));

        // id must be null per JSON-RPC 2.0 spec when the request id could not be determined
        // (e.g. parse errors, invalid-request errors)
        assertTrue("id must be null when request id cannot be determined", response.getId() == null);

        // response must still carry a well-formed error payload
        McpErrorResponse.Error error = response.getError();
        assertNotNull(error);
        assertEquals("jsonrpc version must be 2.0", "2.0", response.getJsonrpc());
        assertEquals("error code must match INTERNAL_ERROR", JSONRPCErrorCode.INTERNAL_ERROR.getCode(), error.code());
    }
}
