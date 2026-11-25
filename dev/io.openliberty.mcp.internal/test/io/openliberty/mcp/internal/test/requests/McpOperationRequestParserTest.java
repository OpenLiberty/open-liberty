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

import static io.openliberty.mcp.internal.McpProtocolVersion.V_2025_03_26;
import static io.openliberty.mcp.internal.McpProtocolVersion.V_2025_06_18;
import static io.openliberty.mcp.internal.McpProtocolVersion.V_2025_11_25;
import static io.openliberty.mcp.internal.McpProtocolVersion.V_2026_07_28;
import static io.openliberty.mcp.internal.RequestMethod.PING;
import static io.openliberty.mcp.internal.RequestMethod.TOOLS_CALL;
import static io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode.PARSE_ERROR;
import static io.openliberty.mcp.internal.requests.parsing.McpOperationRequestParser.MCP_PROTOCOL_VERSION_HEADER;
import static io.openliberty.mcp.internal.requests.parsing.McpOperationRequestParser.MCP_SESSION_ID_HEADER;
import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.assertThrows;
import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.exception;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import org.mcpjava.server.Icon;
import org.mcpjava.server.ImplementationInfo;

import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.McpRequestTracker;
import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.config.McpConfig;
import io.openliberty.mcp.internal.exceptions.jsonrpc.HttpResponseException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.MCPRequestValidationException;
import io.openliberty.mcp.internal.metrics.McpSessionMetrics;
import io.openliberty.mcp.internal.requests.IconImpl;
import io.openliberty.mcp.internal.requests.ImplementationInfoImpl;
import io.openliberty.mcp.internal.requests.RequestId;
import io.openliberty.mcp.internal.requests.parsing.McpOperationRequestParser;
import io.openliberty.mcp.internal.sessions.McpSession;
import io.openliberty.mcp.internal.sessions.McpSessionId;
import io.openliberty.mcp.internal.sessions.McpSessionStore;
import io.openliberty.mcp.internal.testutils.BetaPropertyRule;
import io.openliberty.mcp.internal.testutils.MockHttpServletRequest;
import io.openliberty.mcp.internal.testutils.MockMcpConfig;
import io.openliberty.mcp.internal.testutils.TestUtils;
import jakarta.json.bind.Jsonb;

public class McpOperationRequestParserTest {

    private static final ImplementationInfoImpl TEST_CLIENT_INFO = new ImplementationInfoImpl(List.of(),
                                                                                              "ExampleClient",
                                                                                              "Example Client Display Name",
                                                                                              "1.0.0",
                                                                                              null,
                                                                                              null);
    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_VALUE = "application/json,text/event-stream";
    private static final Jsonb jsonb = TestUtils.createJsonb();

    @Rule
    public BetaPropertyRule betaProperty = new BetaPropertyRule();

    private McpOperationRequestParser parser;
    private McpSessionStore sessionStore;

    @Before
    public void setup() {
        setupStateless();
    }

    private void setupStateless() {
        setupWithConfig(MockMcpConfig.createStateless());
    }

    private void setupStateful() {
        setupWithConfig(MockMcpConfig.createStateful());
    }

    private void setupWithConfig(McpConfig config) {
        McpRequestTracker requestTracker = new McpRequestTracker(config);
        sessionStore = new McpSessionStore(requestTracker, config);
        parser = new McpOperationRequestParser(sessionStore, config, jsonb);
    }

    private static MockHttpServletRequest validRequestFor(String json) {
        var request = new MockHttpServletRequest(json);
        request.addHeader(ACCEPT, ACCEPT_VALUE);
        request.addHeader(MCP_PROTOCOL_VERSION_HEADER, V_2025_11_25.getVersion());
        return request;
    }

    private MockHttpServletRequest validRequestFor(String json, McpProtocolVersion version) {
        if (version == V_2026_07_28) {
            betaProperty.set(true);
        }
        var request = new MockHttpServletRequest(json);
        request.addHeader(ACCEPT, ACCEPT_VALUE);
        request.addHeader(MCP_PROTOCOL_VERSION_HEADER, version.getVersion());
        return request;
    }

    @Test
    public void testParseToolCallRequest() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "tools/call",
                            "params": {
                                "name": "echo",
                                "arguments": {
                                    "value": "hello"
                                }
                            }
                        }
                        """;
        var req = validRequestFor(json);
        var partialRequest = parser.preParse(req);
        assertEquals("acceptHeader", ACCEPT_VALUE, partialRequest.acceptHeader());
        assertEquals("protocolVersion", V_2025_11_25, partialRequest.protocolVersion());
        assertTrue("protocolVersionHeaderPresent", partialRequest.protocolVersionHeaderPresent());
        assertNull("sessionId", partialRequest.sessionId());
        JSONAssert.assertEquals("requestTo", json, jsonb.toJson(partialRequest.requestTo()), STRICT);
        assertNull("parseException", partialRequest.parseException());
        assertEquals("requestId", new RequestId("5"), partialRequest.id());
        assertEquals("method", RequestMethod.TOOLS_CALL, partialRequest.method());

        var request = parser.parse(partialRequest);
        assertEquals("method", TOOLS_CALL, request.method());
        assertEquals("protocolVersion", V_2025_11_25, request.protocolVersion());
        assertEquals("requestId", new RequestId("5"), request.id());
        assertNull("session", request.session());
        JSONAssert.assertEquals("""
                        {
                            "name": "echo",
                            "arguments": {
                                "value": "hello"
                            }
                        }
                        """,
                                request.args().toString(),
                                STRICT);

    }

    @Test
    public void testParseInitializeRequest() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "initialize",
                            "params": {
                                "protocolVersion": "2026-12-12",
                                "capabilities": {
                                    "elicitation": {
                                        "form": {},
                                        "url": {}
                                    }
                                },
                                "clientInfo": {
                                    "name": "testClient",
                                    "version": "0.1"
                                }
                            }
                        }
                        """;
        var req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, ACCEPT_VALUE); // No protocol version header for initialize
        var partialRequest = parser.preParse(req);
        assertEquals("acceptHeader", ACCEPT_VALUE, partialRequest.acceptHeader());
        assertEquals("protocolVersion", V_2025_03_26, partialRequest.protocolVersion());
        assertNull("sessionId", partialRequest.sessionId());
        JSONAssert.assertEquals("requestTo", json, jsonb.toJson(partialRequest.requestTo()), STRICT);
        assertNull("parseException", partialRequest.parseException());
        assertEquals("requestId", new RequestId("5"), partialRequest.id());
        assertEquals("requestMethod", RequestMethod.INITIALIZE, partialRequest.method());

        var request = parser.parse(partialRequest);
        assertEquals("method", RequestMethod.INITIALIZE, request.method());
        assertEquals("protocolVersion", V_2025_03_26, request.protocolVersion());
        assertEquals("requestId", new RequestId("5"), request.id());
        assertNull("session", request.session());
        JSONAssert.assertEquals("""
                        {
                             "protocolVersion": "2026-12-12",
                             "capabilities": {
                                 "elicitation": {
                                     "form": {},
                                     "url": {}
                                 }
                             },
                             "clientInfo": {
                                 "name": "testClient",
                                 "version": "0.1"
                             }
                         }
                         """,
                                request.args().toString(),
                                STRICT);
    }

    @Test
    public void testPreParseInvalidHeaders() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "1",
                            "method": "ping"
                        }
                        """;
        var req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, "badValue1");
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, "badValue2");
        req.addHeader(MCP_SESSION_ID_HEADER, "badValue3");

        var partialRequest = parser.preParse(req);
        assertEquals("acceptHeader", "badValue1", partialRequest.acceptHeader());
        assertEquals("protocolVersion", null, partialRequest.protocolVersion());
        assertTrue("protocolVersionHeaderPresent", partialRequest.protocolVersionHeaderPresent());
        assertEquals("sessionId", new McpSessionId("badValue3"), partialRequest.sessionId());
        JSONAssert.assertEquals("TO", json, jsonb.toJson(partialRequest.requestTo()), STRICT);
        assertNull("parseException", partialRequest.parseException());
        assertEquals("requestId", new RequestId("1"), partialRequest.id());
        assertEquals("method", PING, partialRequest.method());
    }

    @Test
    public void testPreParseMissingProtocolHeader() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "1",
                            "method": "ping"
                        }
                        """;
        var req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, ACCEPT_VALUE);

        var partialRequest = parser.preParse(req);
        assertEquals("acceptHeader", ACCEPT_VALUE, partialRequest.acceptHeader());
        assertEquals("protocolVersion", V_2025_03_26, partialRequest.protocolVersion());
        assertFalse("protocolVersionHeaderPresent", partialRequest.protocolVersionHeaderPresent());
        assertNull("sessionId", partialRequest.sessionId());
        JSONAssert.assertEquals("TO", json, jsonb.toJson(partialRequest.requestTo()), STRICT);
        assertNull("parseException", partialRequest.parseException());
        assertEquals("requestId", new RequestId("1"), partialRequest.id());
        assertEquals("method", PING, partialRequest.method());
    }

    @Test
    public void testPreParseInvalidRequestId() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": true,
                            "method": "ping"
                        }
                        """;
        var req = validRequestFor(json);
        var partialRequest = parser.preParse(req);
        assertEquals("acceptHeader", ACCEPT_VALUE, partialRequest.acceptHeader());
        assertEquals("protocolVersion", V_2025_11_25, partialRequest.protocolVersion());
        assertTrue("protocolVersionHeaderPresent", partialRequest.protocolVersionHeaderPresent());
        assertNull("sessionId", partialRequest.sessionId());
        assertNull("TO", partialRequest.requestTo());
        assertThat("parseException", partialRequest.parseException(), exception().ofType(JSONRPCException.class));
        assertEquals("requestId", RequestId.UNPARSABLE, partialRequest.id());
        assertNull("method", partialRequest.method());
    }

    @Test
    public void testPreParseBlankRequestId() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "",
                            "method": "ping"
                        }
                        """;
        var req = validRequestFor(json);
        var partialRequest = parser.preParse(req);
        assertEquals("acceptHeader", ACCEPT_VALUE, partialRequest.acceptHeader());
        assertEquals("protocolVersion", V_2025_11_25, partialRequest.protocolVersion());
        assertTrue("protocolVersionHeaderPresent", partialRequest.protocolVersionHeaderPresent());
        assertNull("sessionId", partialRequest.sessionId());
        assertNull("TO", partialRequest.requestTo());
        assertThat("parseException", partialRequest.parseException(), exception().ofType(JSONRPCException.class));
        assertEquals("requestId", RequestId.UNPARSABLE, partialRequest.id());
        assertNull("method", partialRequest.method());
    }

    @Test
    public void testPreParseMissingRequestId() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "method": "ping"
                        }
                        """;
        var req = validRequestFor(json);
        var partialRequest = parser.preParse(req);
        assertEquals("acceptHeader", ACCEPT_VALUE, partialRequest.acceptHeader());
        assertEquals("protocolVersion", V_2025_11_25, partialRequest.protocolVersion());
        assertTrue("protocolVersionHeaderPresent", partialRequest.protocolVersionHeaderPresent());
        assertNull("sessionId", partialRequest.sessionId());
        JSONAssert.assertEquals("TO", json, jsonb.toJson(partialRequest.requestTo()), STRICT);
        assertNull("parseException", partialRequest.parseException());
        assertNull("requestId", partialRequest.id());
        assertEquals("method", RequestMethod.PING, partialRequest.method());
    }

    @Test
    public void testPreParseInvalidJsonRpcVersion() throws IOException {
        String json = """
                        {
                            "jsonrpc": "1.0",
                            "id": "1",
                            "method": "ping"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertEquals("acceptHeader", ACCEPT_VALUE, partialRequest.acceptHeader());
        assertEquals("protocolVersion", V_2025_11_25, partialRequest.protocolVersion());
        assertTrue("protocolVersionHeaderPresent", partialRequest.protocolVersionHeaderPresent());
        assertNull("sessionId", partialRequest.sessionId());
        assertNull("TO", partialRequest.requestTo());
        assertThat("parseException", partialRequest.parseException(), exception().ofType(JSONRPCException.class));
        assertEquals("requestId", RequestId.UNPARSABLE, partialRequest.id());
        assertNull("method", partialRequest.method());
    }

    @Test
    public void testPreParseMissingMethod() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "1"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertEquals("acceptHeader", ACCEPT_VALUE, partialRequest.acceptHeader());
        assertEquals("protocolVersion", V_2025_11_25, partialRequest.protocolVersion());
        assertTrue("protocolVersionHeaderPresent", partialRequest.protocolVersionHeaderPresent());
        assertNull("sessionId", partialRequest.sessionId());
        assertNull("TO", partialRequest.requestTo());
        assertThat("parseException", partialRequest.parseException(), exception().ofType(JSONRPCException.class));
        assertEquals("requestId", RequestId.UNPARSABLE, partialRequest.id());
        assertNull("method", partialRequest.method());
    }

    @Test
    public void testPreParseBlankMethod() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "1",
                            "method": ""
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertEquals("acceptHeader", ACCEPT_VALUE, partialRequest.acceptHeader());
        assertEquals("protocolVersion", V_2025_11_25, partialRequest.protocolVersion());
        assertTrue("protocolVersionHeaderPresent", partialRequest.protocolVersionHeaderPresent());
        assertNull("sessionId", partialRequest.sessionId());
        assertNull("TO", partialRequest.requestTo());
        assertThat("parseException", partialRequest.parseException(), exception().ofType(JSONRPCException.class));
        assertEquals("requestId", RequestId.UNPARSABLE, partialRequest.id());
        assertNull("method", partialRequest.method());
    }

    @Test
    public void testPreParseInvalidMethod() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "1",
                            "method": "not/a/method"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertEquals("acceptHeader", ACCEPT_VALUE, partialRequest.acceptHeader());
        assertEquals("protocolVersion", V_2025_11_25, partialRequest.protocolVersion());
        assertTrue("protocolVersionHeaderPresent", partialRequest.protocolVersionHeaderPresent());
        assertNull("sessionId", partialRequest.sessionId());
        JSONAssert.assertEquals("TO", json, jsonb.toJson(partialRequest.requestTo()), STRICT);
        assertNull("parseException", partialRequest.parseException());
        assertEquals("requestId", new RequestId("1"), partialRequest.id());
        assertNull("method", partialRequest.method());
    }

    @Test
    public void testPreParseNullRequestId() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": null,
                            "method": "ping"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertEquals("acceptHeader", ACCEPT_VALUE, partialRequest.acceptHeader());
        assertEquals("protocolVersion", V_2025_11_25, partialRequest.protocolVersion());
        assertTrue("protocolVersionHeaderPresent", partialRequest.protocolVersionHeaderPresent());
        assertNull("sessionId", partialRequest.sessionId());
        assertNull("TO", partialRequest.requestTo());

        // Presence of parseException here shows that id was treated as invalid rather than missing
        assertThat("parseException",
                   partialRequest.parseException(),
                   exception().ofType(JSONRPCException.class)
                              .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_REQUEST))));

        assertEquals("requestId", RequestId.UNPARSABLE, partialRequest.id());
        assertNull("method", partialRequest.method());
    }

    @Test
    public void testParseInvalidAcceptHeader() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "ping"
                        }
                        """;
        var req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, "text/html");
        var partialRequest = parser.preParse(req);
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(HttpResponseException.class)
                                                                    .and(hasProperty("statusCode", equalTo(SC_NOT_ACCEPTABLE))));
    }

    @Test
    public void testParseMissingAcceptHeader() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "ping"
                        }
                        """;
        var req = new MockHttpServletRequest(json);
        var partialRequest = parser.preParse(req);
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(HttpResponseException.class)
                                                                    .and(hasProperty("statusCode", equalTo(SC_NOT_ACCEPTABLE))));
    }

    @Test
    public void testParseInvalidProtocolVersionHeader() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "ping"
                        }
                        """;
        var req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, ACCEPT_VALUE);
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, "badValue1");
        var partialRequest = parser.preParse(req);
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(HttpResponseException.class)
                                                                    .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testParseMissingProtocolVersionHeader() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "ping"
                        }
                        """;
        var req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, ACCEPT_VALUE);
        var partialRequest = parser.preParse(req);
        var request = parser.parse(partialRequest);
        assertEquals("protocolVersion", V_2025_03_26, request.protocolVersion());
        assertEquals("method", PING, request.method());
    }

    @Test
    public void testParseInvalidJsonrpcVersion() throws IOException {
        String json = """
                        {
                            "jsonrpc": "1.0",
                            "id": "5",
                            "method": "ping"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(MCPRequestValidationException.class)
                                                                    .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_REQUEST))));
    }

    @Test
    public void testParseMissingJsonrpcVersion() throws IOException {
        String json = """
                        {
                            "id": "5",
                            "method": "ping"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(MCPRequestValidationException.class)
                                                                    .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_REQUEST))));
    }

    @Test
    public void testParseInvalidMethod() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "not/a/method"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(JSONRPCException.class)
                                                                    .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.METHOD_NOT_FOUND))));
    }

    @Test
    public void testParseMissingMethod() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(MCPRequestValidationException.class)
                                                                    .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_REQUEST))));
    }

    @Test
    public void testParseNumericId() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": 5,
                            "method": "ping"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertEquals("partial requestId", new RequestId(new BigDecimal(5)), partialRequest.id());
        var request = parser.parse(partialRequest);
        assertEquals("requestId", new RequestId(new BigDecimal(5)), request.id());
        assertEquals("method", PING, request.method());
    }

    @Test
    public void testParseStringId() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "abc",
                            "method": "ping"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertEquals("partial requestId", new RequestId("abc"), partialRequest.id());
        var request = parser.parse(partialRequest);
        assertEquals("requestId", new RequestId("abc"), request.id());
        assertEquals("method", PING, request.method());
    }

    @Test
    public void testParseMissingId() throws IOException {
        // A request without an id is a notification, which is valid
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "method": "ping"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertNull("partial requestId", partialRequest.id());
        var request = parser.parse(partialRequest);
        assertNull("requestId", request.id());
        assertEquals("method", PING, request.method());
    }

    @Test
    public void testParseInvalidId() throws IOException {
        // Use boolean true as the invalid value
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": true,
                            "method": "ping"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertNull("requestTo", partialRequest.requestTo());
        assertThat("parseException", partialRequest.parseException(), exception().ofType(JSONRPCException.class));
        assertEquals("partial requestId", RequestId.UNPARSABLE, partialRequest.id());
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(MCPRequestValidationException.class)
                                                                    .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_REQUEST))));
    }

    @Test
    public void testParseEmptyStringId() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "",
                            "method": "ping"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertNull("requestTo", partialRequest.requestTo());
        assertThat("parseException", partialRequest.parseException(), exception().ofType(JSONRPCException.class));
        assertEquals("partial requestId", RequestId.UNPARSABLE, partialRequest.id());
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(MCPRequestValidationException.class)
                                                                    .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_REQUEST))));
    }

    @Test
    public void testParseEmptyMethod() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": ""
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(MCPRequestValidationException.class)
                                                                    .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_REQUEST))));
    }

    @Test
    public void testParseBlankMethod() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "   "
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(MCPRequestValidationException.class)
                                                                    .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_REQUEST))));
    }

    @Test
    public void testParseMultipleValidationErrors() throws IOException {
        // Invalid jsonrpc version, missing method and invalid id type should all be reported together
        String json = """
                        {
                            "jsonrpc": "1.0",
                            "id": true
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(MCPRequestValidationException.class)
                                                                    .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_REQUEST)))
                                                                    .and(hasProperty("data", hasSize(3))));
    }

    @Test
    public void testPreParseMalformedJson() throws IOException {
        var partialRequest = parser.preParse(validRequestFor("{ this is not json"));
        assertEquals("acceptHeader", ACCEPT_VALUE, partialRequest.acceptHeader());
        assertEquals("protocolVersion", V_2025_11_25, partialRequest.protocolVersion());
        assertNull("requestTo", partialRequest.requestTo());
        assertThat(partialRequest.parseException(), exception().ofType(JSONRPCException.class)
                                                               .and(hasProperty("errorCode", equalTo(PARSE_ERROR))));
        assertEquals("requestId", RequestId.UNPARSABLE, partialRequest.id());
        assertNull("method", partialRequest.method());
    }

    @Test
    public void testParseMalformedJson() throws IOException {
        var partialRequest = parser.preParse(validRequestFor("{ this is not json"));
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(JSONRPCException.class)
                                                                    .and(hasProperty("errorCode", equalTo(PARSE_ERROR))));
    }

    @Test
    public void testParseNullJsonBody() throws IOException {
        var partialRequest = parser.preParse(validRequestFor("null"));
        assertNull("requestTo", partialRequest.requestTo());
        assertThat("parseException", partialRequest.parseException(), exception().ofType(JSONRPCException.class));
        assertEquals("requestId", RequestId.UNPARSABLE, partialRequest.id());
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(JSONRPCException.class)
                                                                    .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_REQUEST))));
    }

    @Test
    public void testParseMalformedJsonWithBadAcceptHeader() throws IOException {
        // Header problems are reported before body problems
        var req = new MockHttpServletRequest("{ this is not json");
        req.addHeader(ACCEPT, "text/html");
        var partialRequest = parser.preParse(req);
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(HttpResponseException.class)
                                                                    .and(hasProperty("statusCode", equalTo(SC_NOT_ACCEPTABLE))));
    }

    @Test
    public void testParseMalformedJsonWithBadProtocolVersionHeader() throws IOException {
        // Header problems are reported before body problems
        var req = new MockHttpServletRequest("{ this is not json");
        req.addHeader(ACCEPT, ACCEPT_VALUE);
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, "badValue");
        var partialRequest = parser.preParse(req);
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(HttpResponseException.class)
                                                                    .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testParseStatefulRequest() throws IOException {
        setupStateful();
        McpSession testSession = sessionStore.createSession(null, new McpSessionMetrics(), TEST_CLIENT_INFO, Map.of());

        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "tools/call",
                            "params": {
                                "name": "echo",
                                "arguments": {
                                    "value": "hello"
                                }
                            }
                        }
                        """;

        var req = validRequestFor(json);
        req.addHeader(MCP_SESSION_ID_HEADER, testSession.getSessionId().value());
        var partialRequest = parser.preParse(req);
        var request = parser.parse(partialRequest);
        assertEquals("protocolVersion", V_2025_11_25, request.protocolVersion());
        assertEquals("method", TOOLS_CALL, request.method());
        assertEquals("session", testSession, request.session());
    }

    @Test
    public void testParseInvalidSessionId() throws IOException {
        setupStateful();
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "tools/call",
                            "params": {
                                "name": "echo"
                            }
                        }
                        """;
        var req = validRequestFor(json);
        req.addHeader(MCP_SESSION_ID_HEADER, "nonExistentSession");
        var partialRequest = parser.preParse(req);
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(HttpResponseException.class)
                                                                    .and(hasProperty("statusCode", equalTo(SC_NOT_FOUND))));
    }

    @Test
    public void testParseMissingSessionId() throws IOException {
        setupStateful();
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "tools/call",
                            "params": {
                                "name": "echo"
                            }
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(HttpResponseException.class)
                                                                    .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testParseStatefulPingWithoutSessionId() throws IOException {
        setupStateful();
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "ping"
                        }
                        """;
        var partialRequest = parser.preParse(validRequestFor(json));
        var request = parser.parse(partialRequest);
        assertEquals("method", PING, request.method());
        assertNull("session", request.session());
    }

    @Test
    public void testParseStatefulInitializeWithoutSessionId() throws IOException {
        setupStateful();
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "initialize",
                            "params": {
                                "protocolVersion": "2025-11-25",
                                "capabilities": {},
                                "clientInfo": {
                                    "name": "testClient",
                                    "version": "0.1"
                                }
                            }
                        }
                        """;
        var req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, ACCEPT_VALUE);
        var partialRequest = parser.preParse(req);
        var request = parser.parse(partialRequest);
        assertEquals("method", RequestMethod.INITIALIZE, request.method());
        assertNull("session", request.session());
    }

    @Test
    public void testParseStatefulInitializeWith20250326Disallowed() throws IOException {
        setupWithConfig(MockMcpConfig.createStateful().setMinimumProtocolVersion(V_2025_11_25));
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "initialize",
                            "params": {
                                "protocolVersion": "2025-11-25",
                                "capabilities": {},
                                "clientInfo": {
                                    "name": "testClient",
                                    "version": "0.1"
                                }
                            }
                        }
                        """;
        var req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, ACCEPT_VALUE);
        var partialRequest = parser.preParse(req);
        var request = parser.parse(partialRequest);
        assertEquals("method", RequestMethod.INITIALIZE, request.method());
        assertNull("session", request.session());
    }

    @Test
    public void testParseToolCallWithoutProtocolHeaderWith20250326Disallowed() throws IOException {
        setupWithConfig(MockMcpConfig.createStateless().setMinimumProtocolVersion(V_2025_11_25));

        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "tools/call",
                            "params": {
                                "name": "echo",
                                "arguments": {
                                    "value": "hello"
                                }
                            }
                        }
                        """;
        // No MCP-Protocol-Version header, so the request is interpreted as 2025-03-26, which is below the configured minimum
        var req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, ACCEPT_VALUE);
        var partialRequest = parser.preParse(req);
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(HttpResponseException.class)
                                                                    .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testParsePingWithoutProtocolHeaderWith20250326Disallowed() throws IOException {
        setupWithConfig(MockMcpConfig.createStateless().setMinimumProtocolVersion(V_2025_11_25));

        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "ping"
                        }
                        """;
        // No MCP-Protocol-Version header, so the request is interpreted as 2025-03-26, which is below the configured minimum
        var req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, ACCEPT_VALUE);
        var partialRequest = parser.preParse(req);

        // Ping is allowed before initialize completes, so this should be accepted
        var request = parser.parse(partialRequest);
        assertEquals("method", RequestMethod.PING, request.method());
    }

    @Test
    public void testParseInvalidJsonWithoutProtocolHeaderWith20250326Disallowed() throws IOException {
        setupWithConfig(MockMcpConfig.createStateless().setMinimumProtocolVersion(V_2025_11_25));

        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "tools/call",
                            "params": true
                        }
                        """;
        // No MCP-Protocol-Version header, so the request is interpreted as 2025-03-26, which is below the configured minimum
        var req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, ACCEPT_VALUE);
        var partialRequest = parser.preParse(req);

        // Where the body is unparsable, the protocol version header is missing and 2025-03-06 is disallowed, we want the parse error, not the bad protocol version error.
        // This is because the intention may have been to call "initialize" which shouldn't have the protocol version header
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(JSONRPCException.class)
                                                                    .and(hasProperty("errorCode", equalTo(PARSE_ERROR))));
    }

    @Test
    public void testParseInvalidMethodWithoutProtocolHeaderWith20250326Disallowed() throws IOException {
        setupWithConfig(MockMcpConfig.createStateless().setMinimumProtocolVersion(V_2025_11_25));

        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "unknownMethod"
                        }
                        """;
        // No MCP-Protocol-Version header, so the request is interpreted as 2025-03-26, which is below the configured minimum
        var req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, ACCEPT_VALUE);
        var partialRequest = parser.preParse(req);

        // Expect the protocol version to be validated before the method here
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(HttpResponseException.class)
                                                                    .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testParseStatelessInvalidSessionId() throws IOException {
        // Session lookup is performed whenever the header is present, even when stateless
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "ping"
                        }
                        """;
        var req = validRequestFor(json);
        req.addHeader(MCP_SESSION_ID_HEADER, "nonExistentSession");
        var partialRequest = parser.preParse(req);
        assertThrows(() -> parser.parse(partialRequest), exception().ofType(HttpResponseException.class)
                                                                    .and(hasProperty("statusCode", equalTo(SC_NOT_FOUND))));
    }

    @Test
    public void testValidateProtocolVersionHeaderForDeleteMissingHeader() {
        // A missing header is allowed
        var req = new MockHttpServletRequest("");
        parser.validateProtocolVersionHeaderForDelete(req);
    }

    @Test
    public void testValidateProtocolVersionHeaderForDeleteValidHeader() {
        var req = new MockHttpServletRequest("");
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, V_2025_11_25.getVersion());
        parser.validateProtocolVersionHeaderForDelete(req);
    }

    @Test
    public void testValidateProtocolVersionHeaderForDeleteOldestVersion() {
        var req = new MockHttpServletRequest("");
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, V_2025_03_26.getVersion());
        parser.validateProtocolVersionHeaderForDelete(req);
    }

    @Test
    public void testValidateProtocolVersionHeaderForDeleteUnknownVersion() {
        var req = new MockHttpServletRequest("");
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, "badValue1");
        assertThrows(() -> parser.validateProtocolVersionHeaderForDelete(req),
                     exception().ofType(HttpResponseException.class)
                                .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testValidateProtocolVersionHeaderForDeleteEmptyHeader() {
        var req = new MockHttpServletRequest("");
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, "");
        assertThrows(() -> parser.validateProtocolVersionHeaderForDelete(req),
                     exception().ofType(HttpResponseException.class)
                                .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testValidateProtocolVersionHeaderForDeleteBelowMinimum() {
        setupWithConfig(MockMcpConfig.createStateful().setMinimumProtocolVersion(V_2025_11_25));
        var req = new MockHttpServletRequest("");
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, V_2025_06_18.getVersion());
        assertThrows(() -> parser.validateProtocolVersionHeaderForDelete(req),
                     exception().ofType(HttpResponseException.class)
                                .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testValidateProtocolVersionHeaderForDeleteAtMinimum() {
        setupWithConfig(MockMcpConfig.createStateful().setMinimumProtocolVersion(V_2025_11_25));
        var req = new MockHttpServletRequest("");
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, V_2025_11_25.getVersion());
        parser.validateProtocolVersionHeaderForDelete(req);
    }

    @Test
    public void testValidateProtocolVersionHeaderForDeleteAboveMaximum() {
        setupWithConfig(MockMcpConfig.createStateful().setMaximumProtocolVersion(V_2025_06_18));
        var req = new MockHttpServletRequest("");
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, V_2025_11_25.getVersion());
        assertThrows(() -> parser.validateProtocolVersionHeaderForDelete(req),
                     exception().ofType(HttpResponseException.class)
                                .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testValidateProtocolVersionHeaderForDeleteAtMaximum() {
        setupWithConfig(MockMcpConfig.createStateful().setMaximumProtocolVersion(V_2025_06_18));
        var req = new MockHttpServletRequest("");
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, V_2025_06_18.getVersion());
        parser.validateProtocolVersionHeaderForDelete(req);
    }

    private static String toolCallWithMeta(String metaEntries) {
        return """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "tools/call",
                            "params": {
                                "name": "echo",
                                "_meta": {
                                    %s
                                }
                            }
                        }
                        """.formatted(metaEntries);
    }

    @Test
    public void testParseClientInfoFromMetaFor20260728() throws IOException {
        String json = toolCallWithMeta("""
                        "io.modelcontextprotocol/protocolVersion": "2026-07-28",
                        "io.modelcontextprotocol/clientInfo": {
                            "name": "meta-client",
                            "title": "Meta Client Title",
                            "version": "3.0.0",
                            "description": "Description in meta",
                            "websiteUrl": "https://example.org",
                            "icons": [ { "src": "https://example.org/icon.png", "mimeType": "image/png", "sizes": ["48x48"], "theme": "light" } ]
                        },
                        "io.modelcontextprotocol/clientCapabilities": {
                            "roots": { "listChanged": true }
                        },
                        "customProp": "customVal"
                        """);
        var request = parser.parse(parser.preParse(validRequestFor(json, V_2026_07_28)));

        ImplementationInfo expectedInfo = new ImplementationInfoImpl(
            List.of(new IconImpl("https://example.org/icon.png", "image/png", List.of("48x48"), Icon.Theme.LIGHT)),
            "meta-client",
            "Meta Client Title",
            "3.0.0",
            "Description in meta",
            "https://example.org"
        );
        assertEquals("clientInfo", expectedInfo, request.clientInfo());
        assertEquals("capabilities", Map.of("roots", Map.of("listChanged", true)), request.clientCapabilities());
    }

    @Test
    public void testParseUnprefixedMetaKeysIgnoredFor20260728() throws IOException {
        String json = toolCallWithMeta("""
                        "io.modelcontextprotocol/protocolVersion": "2026-07-28",
                        "io.modelcontextprotocol/clientCapabilities": {},
                        "clientInfo": { "name": "app-defined", "version": "1.5" },
                        "clientCapabilities": { "appCap": true },
                        "capabilities": { "appCap2": true }
                        """);
        var request = parser.parse(parser.preParse(validRequestFor(json, V_2026_07_28)));

        assertEquals("clientInfo", ImplementationInfoImpl.UNKNOWN, request.clientInfo());
        assertEquals("capabilities", Map.of(), request.clientCapabilities());
    }

    @Test
    public void testParseEmptyClientCapabilitiesFor20260728() throws IOException {
        String json = toolCallWithMeta("""
                        "io.modelcontextprotocol/protocolVersion": "2026-07-28",
                        "io.modelcontextprotocol/clientCapabilities": {}
                        """);
        var request = parser.parse(parser.preParse(validRequestFor(json, V_2026_07_28)));

        assertEquals("clientInfo", ImplementationInfoImpl.UNKNOWN, request.clientInfo());
        assertEquals("capabilities", Map.of(), request.clientCapabilities());
    }

    @Test
    public void testParseWithoutMetaFor20260728() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "tools/call",
                            "params": {
                                "name": "echo"
                            }
                        }
                        """;
        assertThrows(() -> parser.parse(parser.preParse(validRequestFor(json, V_2026_07_28))),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.HEADER_MISMATCH))));
    }

    @Test
    public void testParseWithoutParamsFor20260728() throws IOException {
        String json = """
                        {
                            "jsonrpc": "2.0",
                            "id": "5",
                            "method": "ping"
                        }
                        """;
        assertThrows(() -> parser.parse(parser.preParse(validRequestFor(json, V_2026_07_28))),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.HEADER_MISMATCH))));
    }

    @Test
    public void testParseMissingClientCapabilitiesFor20260728() throws IOException {
        String json = toolCallWithMeta("""
                        "io.modelcontextprotocol/protocolVersion": "2026-07-28",
                        "io.modelcontextprotocol/clientInfo": { "name": "meta-client", "version": "1.0" }
                        """);
        assertThrows(() -> parser.parse(parser.preParse(validRequestFor(json, V_2026_07_28))),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS)))
                                .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testParseInvalidClientInfoFor20260728() throws IOException {
        String json = toolCallWithMeta("""
                        "io.modelcontextprotocol/protocolVersion": "2026-07-28",
                        "io.modelcontextprotocol/clientInfo": "not-an-object",
                        "io.modelcontextprotocol/clientCapabilities": {}
                        """);
        assertThrows(() -> parser.parse(parser.preParse(validRequestFor(json, V_2026_07_28))),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS)))
                                .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testParseClientInfoMissingRequiredFieldsFor20260728() throws IOException {
        String json = toolCallWithMeta("""
                        "io.modelcontextprotocol/protocolVersion": "2026-07-28",
                        "io.modelcontextprotocol/clientInfo": { "title": "Missing Name and Version" },
                        "io.modelcontextprotocol/clientCapabilities": {}
                        """);
        assertThrows(() -> parser.parse(parser.preParse(validRequestFor(json, V_2026_07_28))),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS)))
                                .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testParseInvalidClientCapabilitiesFor20260728() throws IOException {
        String json = toolCallWithMeta("""
                        "io.modelcontextprotocol/protocolVersion": "2026-07-28",
                        "io.modelcontextprotocol/clientCapabilities": 12345
                        """);
        assertThrows(() -> parser.parse(parser.preParse(validRequestFor(json, V_2026_07_28))),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS)))
                                .and(hasProperty("statusCode", equalTo(SC_BAD_REQUEST))));
    }

    @Test
    public void testParseClientCapabilitiesAreUnmodifiableFor20260728() throws IOException {
        String json = toolCallWithMeta("""
                        "io.modelcontextprotocol/protocolVersion": "2026-07-28",
                        "io.modelcontextprotocol/clientCapabilities": { "sampling": {} }
                        """);
        var request = parser.parse(parser.preParse(validRequestFor(json, V_2026_07_28)));

        assertEquals("capabilities", Map.of("sampling", Map.of()), request.clientCapabilities());
        assertThrows(() -> request.clientCapabilities().put("extra", true), exception().ofType(UnsupportedOperationException.class));
    }

    @Test
    public void testParseClientInfoFromSessionForOlderProtocolVersion() throws IOException {
        setupStateful();
        Map<String, Object> sessionCaps = Map.of("sessionFeature", true);
        McpSession testSession = sessionStore.createSession(null, new McpSessionMetrics(), TEST_CLIENT_INFO, sessionCaps);

        // Even if the request carries _meta, an older protocol version must read from the session
        String json = toolCallWithMeta("""
                        "io.modelcontextprotocol/clientInfo": { "name": "ignored-request-client", "version": "9.9" },
                        "io.modelcontextprotocol/clientCapabilities": { "ignoredCaps": false }
                        """);
        var req = validRequestFor(json, V_2025_11_25);
        req.addHeader(MCP_SESSION_ID_HEADER, testSession.getSessionId().value());
        var request = parser.parse(parser.preParse(req));

        assertEquals("clientInfo", TEST_CLIENT_INFO, request.clientInfo());
        assertEquals("capabilities", sessionCaps, request.clientCapabilities());
    }

    @Test
    public void testParseClientInfoStatelessForOlderProtocolVersion() throws IOException {
        String json = toolCallWithMeta("""
                        "io.modelcontextprotocol/clientInfo": { "name": "ignored-request-client", "version": "9.9" }
                        """);
        var request = parser.parse(parser.preParse(validRequestFor(json, V_2025_11_25)));

        assertEquals("clientInfo", ImplementationInfoImpl.UNKNOWN, request.clientInfo());
        assertEquals("capabilities", Map.of(), request.clientCapabilities());
    }

}
