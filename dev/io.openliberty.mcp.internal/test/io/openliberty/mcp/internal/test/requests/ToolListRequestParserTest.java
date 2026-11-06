/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test.requests;

import static io.openliberty.mcp.internal.McpProtocolVersion.V_2025_11_25;
import static io.openliberty.mcp.internal.requests.parsing.McpOperationRequestParser.MCP_PROTOCOL_VERSION_HEADER;
import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.assertThrows;
import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.exception;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import io.openliberty.mcp.internal.McpRequestTracker;
import io.openliberty.mcp.internal.config.McpConfig;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.McpOperationRequest;
import io.openliberty.mcp.internal.requests.parsing.McpOperationRequestParser;
import io.openliberty.mcp.internal.sessions.McpSessionStore;
import io.openliberty.mcp.internal.testutils.MockHttpServletRequest;
import io.openliberty.mcp.internal.testutils.MockMcpConfig;
import io.openliberty.mcp.internal.testutils.TestUtils;
import io.openliberty.mcp.internal.tools.parsing.ToolListRequestParser;
import jakarta.json.bind.Jsonb;

public class ToolListRequestParserTest {

    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_VALUE = "application/json,text/event-stream";
    private static final Jsonb jsonb = TestUtils.createJsonb();

    private McpOperationRequestParser requestParser;
    private ToolListRequestParser toolListParser;

    @Before
    public void setup() {
        McpConfig config = MockMcpConfig.createStateless();
        McpRequestTracker requestTracker = new McpRequestTracker(config);
        McpSessionStore sessionStore = new McpSessionStore(requestTracker, config);
        requestParser = new McpOperationRequestParser(sessionStore, config, jsonb);
        toolListParser = new ToolListRequestParser(jsonb);
    }

    private McpOperationRequest parseRequest(String json) {
        MockHttpServletRequest req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, ACCEPT_VALUE);
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, V_2025_11_25.getVersion());
        try {
            return requestParser.parse(requestParser.preParse(req));
        } catch (Exception ex) {
            throw new AssertionError("Unexpected operation parsing exception: " + ex, ex);
        }
    }

    @Test
    public void testParseWithCursor() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/list",
                          "params": {
                            "cursor": "abc123"
                          }
                        }
                        """);
        var toolListRequest = toolListParser.parse(opRequest);
        assertEquals("opRequest", opRequest, toolListRequest.request());
        assertEquals("cursorValue", "abc123", toolListRequest.cursorValue());
        assertEquals("cursor", Optional.of("abc123"), toolListRequest.cursor());
    }

    @Test
    public void testParseWithoutCursor() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/list",
                          "params": {}
                        }
                        """);
        var toolListRequest = toolListParser.parse(opRequest);
        assertEquals("opRequest", opRequest, toolListRequest.request());
        assertNull("cursorValue", toolListRequest.cursorValue());
        assertEquals("cursor", Optional.empty(), toolListRequest.cursor());
    }

    @Test
    public void testParseMissingParams() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/list"
                        }
                        """);
        var toolListRequest = toolListParser.parse(opRequest);
        assertEquals("opRequest", opRequest, toolListRequest.request());
        assertNull("cursorValue", toolListRequest.cursorValue());
        assertEquals("cursor", Optional.empty(), toolListRequest.cursor());
    }

    @Test
    public void testParseEmptyCursor() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/list",
                          "params": {
                            "cursor": ""
                          }
                        }
                        """);
        // An empty cursor is normalized to null
        var toolListRequest = toolListParser.parse(opRequest);
        assertNull("cursorValue", toolListRequest.cursorValue());
        assertEquals("cursor", Optional.empty(), toolListRequest.cursor());
    }

    @Test
    public void testParseNullCursor() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/list",
                          "params": {
                            "cursor": null
                          }
                        }
                        """);
        var toolListRequest = toolListParser.parse(opRequest);
        assertNull("cursorValue", toolListRequest.cursorValue());
        assertEquals("cursor", Optional.empty(), toolListRequest.cursor());
    }

    @Test
    public void testParseNonStringCursor() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/list",
                          "params": {
                            "cursor": {}
                          }
                        }
                        """);
        assertThrows(() -> toolListParser.parse(opRequest),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("data", contains(startsWith("Unable to deserialize property 'cursor'")))));
    }

}
