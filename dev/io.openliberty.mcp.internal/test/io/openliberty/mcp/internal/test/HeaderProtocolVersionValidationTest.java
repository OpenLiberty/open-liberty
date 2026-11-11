/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;

import org.junit.Test;

import io.openliberty.mcp.internal.HeaderValidation;
import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.McpRequest;

/**
 * Tests that the MCP-Protocol-Version header is validated against the protocol version in the request body's
 * {@code _meta} for protocol version 2026-07-28.
 */
public class HeaderProtocolVersionValidationTest {

    private static McpRequest request(String idJson, String paramsJson) {
        String id = idJson == null ? "" : "\"id\": " + idJson + ",";
        return McpRequest.createValidMCPRequest(new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          %s
                          "method": "tools/call",
                          "params": %s
                        }
                        """.formatted(id, paramsJson)));
    }

    private static McpRequest requestWithMeta(String metaJson) {
        return request("1", "{\"name\": \"echo\", \"_meta\": " + metaJson + "}");
    }

    private static void assertHeaderMismatch(McpProtocolVersion header, McpRequest request) {
        try {
            HeaderValidation.validateProtocolVersionMatchesBody(header, request);
            fail("Expected a HeaderMismatch JSONRPCException");
        } catch (JSONRPCException e) {
            assertEquals(JSONRPCErrorCode.HEADER_MISMATCH, e.getErrorCode());
        }
    }

    @Test
    public void testHeaderMismatchErrorCode() {
        assertEquals(-32020, JSONRPCErrorCode.HEADER_MISMATCH.getCode());
    }

    @Test
    public void testMatchingVersionAccepted() {
        McpRequest request = requestWithMeta("{\"io.modelcontextprotocol/protocolVersion\": \"2026-07-28\"}");
        HeaderValidation.validateProtocolVersionMatchesBody(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testMismatchedVersionRejected() {
        McpRequest request = requestWithMeta("{\"io.modelcontextprotocol/protocolVersion\": \"2025-11-25\"}");
        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testMismatchMessageNamesBothValues() {
        McpRequest request = requestWithMeta("{\"io.modelcontextprotocol/protocolVersion\": \"2025-11-25\"}");
        try {
            HeaderValidation.validateProtocolVersionMatchesBody(McpProtocolVersion.V_2026_07_28, request);
            fail("Expected a HeaderMismatch JSONRPCException");
        } catch (JSONRPCException e) {
            String data = String.valueOf(e.getData());
            assertTrue(data, data.contains("2026-07-28"));
            assertTrue(data, data.contains("2025-11-25"));
        }
    }

    @Test
    public void testMissingMetaRejected() {
        McpRequest request = request("1", "{\"name\": \"echo\"}");
        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testMissingParamsRejected() {
        McpRequest request = McpRequest.createValidMCPRequest(new StringReader("""
                        { "jsonrpc": "2.0", "id": 1, "method": "ping" }
                        """));
        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testMetaWithoutProtocolVersionRejected() {
        McpRequest request = requestWithMeta("{\"other/key\": \"value\"}");
        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testNonStringProtocolVersionRejected() {
        McpRequest request = requestWithMeta("{\"io.modelcontextprotocol/protocolVersion\": 20260728}");
        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testNonObjectMetaRejected() {
        McpRequest request = requestWithMeta("\"2026-07-28\"");
        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testNotificationMismatchRejected() {
        McpRequest notification = request(null, "{}");
        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, notification);
    }

    @Test
    public void testNotificationMatchingVersionAccepted() {
        McpRequest notification = request(null, "{\"_meta\": {\"io.modelcontextprotocol/protocolVersion\": \"2026-07-28\"}}");
        HeaderValidation.validateProtocolVersionMatchesBody(McpProtocolVersion.V_2026_07_28, notification);
    }

    @Test
    public void testGaVersionsNotValidated() {
        McpRequest mismatched = requestWithMeta("{\"io.modelcontextprotocol/protocolVersion\": \"2026-07-28\"}");
        McpRequest noMeta = request("1", "{\"name\": \"echo\"}");
        for (McpProtocolVersion version : new McpProtocolVersion[] { McpProtocolVersion.V_2025_11_25,
                                                                     McpProtocolVersion.V_2025_06_18,
                                                                     McpProtocolVersion.V_2025_03_26 }) {
            HeaderValidation.validateProtocolVersionMatchesBody(version, mismatched);
            HeaderValidation.validateProtocolVersionMatchesBody(version, noMeta);
        }
    }
}
