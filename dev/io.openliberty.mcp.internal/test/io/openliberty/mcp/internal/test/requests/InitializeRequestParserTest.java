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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mcpjava.server.Icon;
import org.mcpjava.server.Icon.Theme;

import io.openliberty.mcp.internal.McpRequestTracker;
import io.openliberty.mcp.internal.config.McpConfig;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.ImplementationInfoImpl;
import io.openliberty.mcp.internal.requests.InitializeRequest;
import io.openliberty.mcp.internal.requests.McpOperationRequest;
import io.openliberty.mcp.internal.requests.parsing.InitializeRequestParser;
import io.openliberty.mcp.internal.requests.parsing.McpOperationRequestParser;
import io.openliberty.mcp.internal.sessions.McpSessionStore;
import io.openliberty.mcp.internal.testutils.MockHttpServletRequest;
import io.openliberty.mcp.internal.testutils.MockMcpConfig;
import io.openliberty.mcp.internal.testutils.TestUtils;
import jakarta.json.bind.Jsonb;

public class InitializeRequestParserTest {

    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_VALUE = "application/json,text/event-stream";
    private static final Jsonb jsonb = TestUtils.createJsonb();

    private McpOperationRequestParser requestParser;
    private InitializeRequestParser initializeParser;

    @Before
    public void setup() {
        McpConfig config = MockMcpConfig.createStateless();
        McpRequestTracker requestTracker = new McpRequestTracker(config);
        McpSessionStore sessionStore = new McpSessionStore(requestTracker, config);
        requestParser = new McpOperationRequestParser(sessionStore, config, jsonb);
        initializeParser = new InitializeRequestParser(jsonb);
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
     * An initialize request containing protocol version, capabilities, and complete clientInfo
     * must parse all fields correctly.
     */
    @Test
    public void testParseFullInitializeRequest() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "capabilities": {
                              "roots": {
                                "listChanged": true
                              }
                            },
                            "clientInfo": {
                              "name": "example-client",
                              "title": "Example Client",
                              "version": "1.0.0",
                              "description": "An example MCP client",
                              "websiteUrl": "https://example.com",
                              "icons": [
                                {
                                  "src": "https://example.com/icon.png",
                                  "mimeType": "image/png",
                                  "sizes": ["32x32"],
                                  "theme": "light"
                                }
                              ]
                            }
                          }
                        }
                        """);
        InitializeRequest initRequest = initializeParser.parse(opRequest);
        assertEquals("protocolVersion", V_2025_11_25, initRequest.protocolVersion());

        assertNotNull("clientCapabilities", initRequest.clientCapabilities());
        assertTrue("clientCapabilities contains roots", initRequest.clientCapabilities().containsKey("roots"));

        ImplementationInfoImpl clientInfo = initRequest.clientInfo();
        assertNotNull("clientInfo", clientInfo);
        assertEquals("clientInfo name", "example-client", clientInfo.name());
        assertEquals("clientInfo title", "Example Client", clientInfo.title());
        assertEquals("clientInfo version", "1.0.0", clientInfo.version());
        assertEquals("clientInfo description", Optional.of("An example MCP client"), clientInfo.description());
        assertEquals("clientInfo websiteUrl", Optional.of("https://example.com"), clientInfo.websiteUrl());

        List<Icon> expectedIcons = List.of(Icon.builder("https://example.com/icon.png")
                                               .setMimeType("image/png")
                                               .addSize(32, 32)
                                               .setTheme(Theme.LIGHT)
                                               .build());
        assertEquals("clientInfo icons", expectedIcons, clientInfo.icons());
    }

    /**
     * An initialize request with minimal clientInfo (only required name and version).
     */
    @Test
    public void testParseMinimalClientInfo() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "capabilities": {},
                            "clientInfo": {
                              "name": "minimal-client",
                              "version": "2.0"
                            }
                          }
                        }
                        """);
        InitializeRequest initRequest = initializeParser.parse(opRequest);
        assertEquals("protocolVersion", V_2025_11_25, initRequest.protocolVersion());
        assertEquals("clientCapabilities", Collections.emptyMap(), initRequest.clientCapabilities());

        ImplementationInfoImpl clientInfo = initRequest.clientInfo();
        assertNotNull("clientInfo", clientInfo);
        assertEquals("clientInfo name", "minimal-client", clientInfo.name());
        assertEquals("clientInfo title defaults to name", "minimal-client", clientInfo.title());
        assertEquals("clientInfo version", "2.0", clientInfo.version());
        assertEquals("clientInfo description", Optional.empty(), clientInfo.description());
        assertEquals("clientInfo websiteUrl", Optional.empty(), clientInfo.websiteUrl());
        assertEquals("clientInfo icons", Collections.emptyList(), clientInfo.icons());
    }

    /**
     * When the client specifies an unrecognized or unsupported protocol version,
     * protocolVersion must be parsed as null without failing the parse step.
     */
    @Test
    public void testParseUnrecognizedProtocolVersion() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 3,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2099-01-01",
                            "capabilities": {},
                            "clientInfo": {
                              "name": "future-client",
                              "version": "1.0"
                            }
                          }
                        }
                        """);
        InitializeRequest initRequest = initializeParser.parse(opRequest);
        assertNull("protocolVersion should be null for unrecognized version", initRequest.protocolVersion());
        assertEquals("clientCapabilities", Collections.emptyMap(), initRequest.clientCapabilities());
        assertEquals("clientInfo name", "future-client", initRequest.clientInfo().name());
    }

    /**
     * When protocolVersion is omitted from params, protocolVersion must be null.
     */
    @Test
    public void testParseMissingProtocolVersion() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 4,
                          "method": "initialize",
                          "params": {
                            "capabilities": {},
                            "clientInfo": {
                              "name": "test-client",
                              "version": "1.0"
                            }
                          }
                        }
                        """);
        InitializeRequest initRequest = initializeParser.parse(opRequest);
        assertNull("protocolVersion should be null when omitted", initRequest.protocolVersion());
    }

    /**
     * When protocolVersion is explicitly null, protocolVersion must be null.
     */
    @Test
    public void testParseNullProtocolVersion() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 5,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": null,
                            "capabilities": {},
                            "clientInfo": {
                              "name": "test-client",
                              "version": "1.0"
                            }
                          }
                        }
                        """);
        InitializeRequest initRequest = initializeParser.parse(opRequest);
        assertNull("protocolVersion should be null when explicitly null", initRequest.protocolVersion());
    }

    /**
     * When capabilities is omitted from params, clientCapabilities must default to an empty map.
     */
    @Test
    public void testParseMissingCapabilities() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 6,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "clientInfo": {
                              "name": "test-client",
                              "version": "1.0"
                            }
                          }
                        }
                        """);
        InitializeRequest initRequest = initializeParser.parse(opRequest);
        assertEquals("protocolVersion", V_2025_11_25, initRequest.protocolVersion());
        assertEquals("clientCapabilities should default to empty map", Collections.emptyMap(), initRequest.clientCapabilities());
    }

    /**
     * When capabilities is explicitly null, clientCapabilities must default to an empty map.
     */
    @Test
    public void testParseNullCapabilities() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 7,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "capabilities": null,
                            "clientInfo": {
                              "name": "test-client",
                              "version": "1.0"
                            }
                          }
                        }
                        """);
        InitializeRequest initRequest = initializeParser.parse(opRequest);
        assertEquals("clientCapabilities should default to empty map when null", Collections.emptyMap(), initRequest.clientCapabilities());
    }

    /**
     * When clientInfo is omitted from params, clientInfo must default to ImplementationInfoImpl.UNKNOWN.
     */
    @Test
    public void testParseMissingClientInfo() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 8,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "capabilities": {}
                          }
                        }
                        """);
        InitializeRequest initRequest = initializeParser.parse(opRequest);
        assertEquals("protocolVersion", V_2025_11_25, initRequest.protocolVersion());
        assertEquals("clientInfo should default to UNKNOWN", ImplementationInfoImpl.UNKNOWN, initRequest.clientInfo());
    }

    /**
     * An initialize request with empty params object must parse with null protocolVersion,
     * empty capabilities, and UNKNOWN clientInfo.
     */
    @Test
    public void testParseEmptyParams() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 9,
                          "method": "initialize",
                          "params": {}
                        }
                        """);
        InitializeRequest initRequest = initializeParser.parse(opRequest);
        assertNull("protocolVersion should be null", initRequest.protocolVersion());
        assertEquals("clientCapabilities should be empty map", Collections.emptyMap(), initRequest.clientCapabilities());
        assertEquals("clientInfo should be UNKNOWN", ImplementationInfoImpl.UNKNOWN, initRequest.clientInfo());
    }

    /**
     * An initialize request with no params field must parse with null protocolVersion,
     * empty capabilities, and UNKNOWN clientInfo.
     */
    @Test
    public void testParseNoParams() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 10,
                          "method": "initialize"
                        }
                        """);
        InitializeRequest initRequest = initializeParser.parse(opRequest);
        assertNull("protocolVersion should be null", initRequest.protocolVersion());
        assertEquals("clientCapabilities should be empty map", Collections.emptyMap(), initRequest.clientCapabilities());
        assertEquals("clientInfo should be UNKNOWN", ImplementationInfoImpl.UNKNOWN, initRequest.clientInfo());
    }

    /**
     * Invalid clientInfo (e.g. missing required name) must result in an INVALID_PARAMS JSON-RPC error.
     */
    @Test
    public void testParseInvalidClientInfoMissingName() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 11,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "clientInfo": {
                              "version": "1.0"
                            }
                          }
                        }
                        """);
        assertThrows(() -> initializeParser.parse(opRequest),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS))));
    }

    /**
     * Invalid clientInfo (e.g. missing required version) must result in an INVALID_PARAMS JSON-RPC error.
     */
    @Test
    public void testParseInvalidClientInfoMissingVersion() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 12,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "clientInfo": {
                              "name": "test-client"
                            }
                          }
                        }
                        """);
        assertThrows(() -> initializeParser.parse(opRequest),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS))));
    }

    /**
     * When clientInfo is explicitly null in JSON, deserialization fails because name and version
     * are required by ImplementationInfoImpl, resulting in an INVALID_PARAMS JSON-RPC error.
     */
    @Test
    public void testParseNullClientInfo() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 13,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "clientInfo": null
                          }
                        }
                        """);
        assertThrows(() -> initializeParser.parse(opRequest),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS))));
    }

    /**
     * Non-object clientInfo must result in an INVALID_PARAMS JSON-RPC error.
     */
    @Test
    public void testParseNonObjectClientInfo() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 14,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "clientInfo": "invalid"
                          }
                        }
                        """);
        assertThrows(() -> initializeParser.parse(opRequest),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS))));
    }

    /**
     * Non-object capabilities must result in an INVALID_PARAMS JSON-RPC error.
     */
    @Test
    public void testParseNonObjectCapabilities() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 15,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "capabilities": "invalid"
                          }
                        }
                        """);
        assertThrows(() -> initializeParser.parse(opRequest),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS))));
    }

    /**
     * Non-string protocolVersion must result in an INVALID_PARAMS JSON-RPC error.
     */
    @Test
    public void testParseNonStringProtocolVersion() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 16,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": {},
                            "capabilities": {},
                            "clientInfo": {
                              "name": "test-client",
                              "version": "1.0"
                            }
                          }
                        }
                        """);
        assertThrows(() -> initializeParser.parse(opRequest),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.INVALID_PARAMS))));
    }

}
