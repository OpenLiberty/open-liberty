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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import io.openliberty.mcp.internal.McpRequestTracker;
import io.openliberty.mcp.internal.config.McpConfig;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.McpOperationRequest;
import io.openliberty.mcp.internal.requests.parsing.CancellationNotificationParser;
import io.openliberty.mcp.internal.requests.parsing.McpOperationRequestParser;
import io.openliberty.mcp.internal.sessions.McpSessionStore;
import io.openliberty.mcp.internal.testutils.MockHttpServletRequest;
import io.openliberty.mcp.internal.testutils.MockMcpConfig;
import io.openliberty.mcp.internal.testutils.TestUtils;
import jakarta.json.bind.Jsonb;

public class CancellationNotificationParserTest {

    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_VALUE = "application/json,text/event-stream";
    private static final Jsonb jsonb = TestUtils.createJsonb();

    private McpOperationRequestParser requestParser;
    private CancellationNotificationParser cancellationParser;

    @Before
    public void setup() {
        McpConfig config = MockMcpConfig.createStateless();
        McpRequestTracker requestTracker = new McpRequestTracker(config);
        McpSessionStore sessionStore = new McpSessionStore(requestTracker, config);
        requestParser = new McpOperationRequestParser(sessionStore, config, jsonb);
        cancellationParser = new CancellationNotificationParser(jsonb);
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

    /**
     * Per the MCP spec, a cancellation notification with a string requestId and a
     * reason must parse both fields correctly.
     */
    @Test
    public void testParseWithStringRequestIdAndReason() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled",
                          "params": {
                            "requestId": "abc-123",
                            "reason": "User cancelled"
                          }
                        }
                        """);
        var notification = cancellationParser.parse(opRequest);
        assertEquals("opRequest", opRequest, notification.request());
        assertEquals("requestId value", "abc-123", notification.idToCancel().asString());
        assertEquals("reason", "User cancelled", notification.reason());
    }

    /**
     * Per the MCP spec, requestId may be an integer.
     */
    @Test
    public void testParseWithNumericRequestId() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled",
                          "params": {
                            "requestId": 42
                          }
                        }
                        """);
        var notification = cancellationParser.parse(opRequest);
        assertEquals("opRequest", opRequest, notification.request());
        assertEquals("requestId value", 42, notification.idToCancel().asInteger().intValue());
        assertNull("reason should be null when absent", notification.reason());
    }

    /**
     * Per the MCP spec, reason is optional. When omitted the parsed reason must be null.
     */
    @Test
    public void testParseWithoutReason() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled",
                          "params": {
                            "requestId": "req-1"
                          }
                        }
                        """);
        var notification = cancellationParser.parse(opRequest);
        assertEquals("opRequest", opRequest, notification.request());
        assertEquals("requestId value", "req-1", notification.idToCancel().asString());
        assertNull("reason should be null when absent", notification.reason());
    }

    /**
     * Per the MCP spec, reason may be explicitly null.
     */
    @Test
    public void testParseWithNullReason() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled",
                          "params": {
                            "requestId": "req-2",
                            "reason": null
                          }
                        }
                        """);
        var notification = cancellationParser.parse(opRequest);
        assertEquals("requestId value", "req-2", notification.idToCancel().asString());
        assertNull("reason should be null when explicitly null", notification.reason());
    }

    /**
     * Per the MCP spec, requestId is required. Omitting it must result in an
     * INVALID_PARAMS JSON-RPC error.
     */
    @Test
    public void testParseMissingRequestId() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled",
                          "params": {
                            "reason": "some reason"
                          }
                        }
                        """);
        assertThrows(() -> cancellationParser.parse(opRequest),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS)))
                                .and(hasProperty("data", contains("requestId missing"))));
    }

    /**
     * Per the MCP spec, requestId is required. Sending an empty params object must
     * result in an INVALID_PARAMS JSON-RPC error.
     */
    @Test
    public void testParseEmptyParams() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled",
                          "params": {}
                        }
                        """);
        assertThrows(() -> cancellationParser.parse(opRequest),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS)))
                                .and(hasProperty("data", contains("requestId missing"))));
    }

    @Test
    public void testParseInvalidId() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled",
                          "params": {
                            "requestId": true
                          }
                        }
                        """);
        assertThrows(() -> cancellationParser.parse(opRequest),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS))));
    }

    @Test
    public void testParseNoParams() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled"
                        }
                        """);
        assertThrows(() -> cancellationParser.parse(opRequest),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS)))
                                .and(hasProperty("data", contains("requestId missing"))));
    }

}
