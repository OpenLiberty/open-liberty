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
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.McpTransport;
import io.openliberty.mcp.internal.config.McpConfig;
import io.openliberty.mcp.internal.config.McpServerConfigProps;
import io.openliberty.mcp.internal.exceptions.jsonrpc.HttpResponseException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.testutils.TestUtils;
import jakarta.json.bind.Jsonb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tests the response to a request which uses an unsupported MCP protocol version.
 * <p>
 * On a beta runtime this is the JSON-RPC {@code UnsupportedProtocolVersionError} (-32022) defined by the 2026-07-28
 * specification. Otherwise it is the plain text HTTP 400 which has always been sent, so the response does not change
 * until the feature is complete.
 * <p>
 * The request and response are minimal {@link Proxy} implementations of the servlet interfaces, so these tests
 * run {@link McpTransport#init} and {@link McpTransport#sendJsonRpcException} for real.
 */
public class McpTransportUnsupportedProtocolVersionTest {

    private static final String BETA_PROPERTY = "com.ibm.ws.beta.edition";
    private static final String ACCEPT_VALUE = "application/json,text/event-stream";
    private static final Jsonb jsonb = TestUtils.createJsonb();

    private static final String PING = """
                    {"jsonrpc": "2.0", "id": 7, "method": "ping"}
                    """;
    private static final String NOTIFICATION = """
                    {"jsonrpc": "2.0", "method": "notifications/initialized"}
                    """;
    private static final String TOOL_CALL = """
                    {"jsonrpc": "2.0", "id": 7, "method": "tools/call", "params": {"name": "echo"}}
                    """;

    private String originalBetaValue;

    @Before
    public void saveBetaProperty() {
        originalBetaValue = System.getProperty(BETA_PROPERTY);
    }

    @After
    public void restoreBetaProperty() {
        if (originalBetaValue == null) {
            System.clearProperty(BETA_PROPERTY);
        } else {
            System.setProperty(BETA_PROPERTY, originalBetaValue);
        }
    }

    /** The parts of the servlet response which the tests check */
    private static class CapturedResponse {
        int status = HttpServletResponse.SC_OK;
        String contentType;
        final StringWriter body = new StringWriter();
    }

    private static HttpServletRequest request(String body, String protocolVersion) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", ACCEPT_VALUE);
        if (protocolVersion != null) {
            headers.put("MCP-Protocol-Version", protocolVersion);
        }
        return (HttpServletRequest) Proxy.newProxyInstance(
                                                           McpTransportUnsupportedProtocolVersionTest.class.getClassLoader(),
                                                           new Class<?>[] { HttpServletRequest.class },
                                                           (proxy, method, args) -> switch (method.getName()) {
                                                               case "getHeader" -> headers.get(args[0]);
                                                               case "getReader" -> new BufferedReader(new StringReader(body));
                                                               case "getMethod" -> "POST";
                                                               default -> null; // setCharacterEncoding and anything else which is not needed
                                                           });
    }

    private static HttpServletResponse response(CapturedResponse captured) {
        PrintWriter writer = new PrintWriter(captured.body);
        return (HttpServletResponse) Proxy.newProxyInstance(
                                                            McpTransportUnsupportedProtocolVersionTest.class.getClassLoader(),
                                                            new Class<?>[] { HttpServletResponse.class },
                                                            (proxy, method, args) -> {
                                                                switch (method.getName()) {
                                                                    case "getWriter" -> {
                                                                        return writer;
                                                                    }
                                                                    case "setStatus" -> captured.status = (Integer) args[0];
                                                                    case "setContentType" -> captured.contentType = (String) args[0];
                                                                    default -> {
                                                                        // not needed
                                                                    }
                                                                }
                                                                return null;
                                                            });
    }

    private static McpConfig config(McpProtocolVersion min, McpProtocolVersion max) {
        return new McpServerConfigProps(true, null, McpServerConfigProps.FALLBACK_PATH, null, Duration.ofMinutes(1), null,
                                        McpServerConfigProps.DEFAULT_ASYNC_TIMEOUT_MS, min, max);
    }

    /** Runs init for the request, expects it to throw a JSON-RPC exception, sends it and returns what the client would see */
    private static CapturedResponse sendRejection(String body, String protocolVersion, McpConfig config) throws IOException {
        CapturedResponse captured = new CapturedResponse();
        McpTransport transport = new McpTransport(request(body, protocolVersion), response(captured), jsonb, 1000);
        try {
            transport.init(null, config);
            throw new AssertionError("Expected the request to be rejected");
        } catch (JSONRPCException e) {
            assertEquals(JSONRPCErrorCode.UNSUPPORTED_PROTOCOL_VERSION, e.getErrorCode());
            transport.sendJsonRpcException(e);
        }
        captured.body.flush();
        return captured;
    }

    @Test
    public void testErrorCode() {
        assertEquals(-32022, JSONRPCErrorCode.UNSUPPORTED_PROTOCOL_VERSION.getCode());
        assertEquals("Unsupported protocol version", JSONRPCErrorCode.UNSUPPORTED_PROTOCOL_VERSION.getMessage());
    }

    /** The response matches the example in the 2026-07-28 specification, with HTTP 400 */
    @Test
    public void testUnknownVersionOnBetaIsSpecError() throws IOException {
        System.setProperty(BETA_PROPERTY, "true");
        CapturedResponse r = sendRejection(PING, "1900-01-01", config(null, null));

        assertEquals(400, r.status);
        assertEquals("application/json", r.contentType);
        JSONAssert.assertEquals("""
                        {
                          "jsonrpc": "2.0",
                          "id": 7,
                          "error": {
                            "code": -32022,
                            "message": "Unsupported protocol version",
                            "data": {
                              "supported": ["2026-07-28", "2025-11-25", "2025-06-18", "2025-03-26"],
                              "requested": "1900-01-01"
                            }
                          }
                        }
                        """, r.body.toString(), STRICT);
    }

    /** A version which is known but not accepted is rejected in the same way, and only accepted versions are listed */
    @Test
    public void testKnownButNotAcceptedVersionOnBetaIsSpecError() throws IOException {
        System.setProperty(BETA_PROPERTY, "true");
        CapturedResponse r = sendRejection(PING, "2025-03-26",
                                           config(McpProtocolVersion.V_2025_06_18, McpProtocolVersion.V_2025_11_25));

        assertEquals(400, r.status);
        JSONAssert.assertEquals("""
                        {
                          "jsonrpc": "2.0",
                          "id": 7,
                          "error": {
                            "code": -32022,
                            "message": "Unsupported protocol version",
                            "data": {
                              "supported": ["2025-11-25", "2025-06-18"],
                              "requested": "2025-03-26"
                            }
                          }
                        }
                        """, r.body.toString(), STRICT);
    }

    /** A missing header is interpreted as 2025-03-26, so that is what is reported as requested */
    @Test
    public void testMissingHeaderOutsideRangeOnBetaReportsDefaultVersion() throws IOException {
        System.setProperty(BETA_PROPERTY, "true");
        CapturedResponse r = sendRejection(TOOL_CALL, null,
                                           config(McpProtocolVersion.V_2025_11_25, null));

        assertEquals(400, r.status);
        JSONAssert.assertEquals("""
                        {
                          "jsonrpc": "2.0",
                          "id": 7,
                          "error": {
                            "code": -32022,
                            "message": "Unsupported protocol version",
                            "data": {
                              "supported": ["2026-07-28", "2025-11-25"],
                              "requested": "2025-03-26"
                            }
                          }
                        }
                        """, r.body.toString(), STRICT);
    }

    /** A notification has no id, but a message rejected at the HTTP level still gets the 400 and the error body */
    @Test
    public void testNotificationOnBetaIsRejectedWith400() throws IOException {
        System.setProperty(BETA_PROPERTY, "true");
        CapturedResponse r = sendRejection(NOTIFICATION, "1900-01-01", config(null, null));

        assertEquals(400, r.status);
        JSONAssert.assertEquals("""
                        {
                          "jsonrpc": "2.0",
                          "id": null,
                          "error": {
                            "code": -32022,
                            "message": "Unsupported protocol version",
                            "data": {
                              "supported": ["2026-07-28", "2025-11-25", "2025-06-18", "2025-03-26"],
                              "requested": "1900-01-01"
                            }
                          }
                        }
                        """, r.body.toString(), STRICT);
    }

    /** A body which cannot be read does not prevent the error, which then has a null id */
    @Test
    public void testUnreadableBodyOnBetaStillGivesSpecError() throws IOException {
        System.setProperty(BETA_PROPERTY, "true");
        CapturedResponse r = sendRejection("{ this is not json", "1900-01-01", config(null, null));

        assertEquals(400, r.status);
        assertTrue(r.body.toString(), r.body.toString().contains("-32022"));
    }

    /** The beta version is not available on a non-beta runtime, so the response must not change from the plain text 400 */
    @Test
    public void testBetaVersionOnNonBetaIsStillPlainTextError() throws IOException {
        System.setProperty(BETA_PROPERTY, "false");
        assertPlainTextRejection("2026-07-28", "Supported values: 2025-11-25, 2025-06-18, 2025-03-26");
    }

    @Test
    public void testUnknownVersionOnNonBetaIsStillPlainTextError() throws IOException {
        System.setProperty(BETA_PROPERTY, "false");
        assertPlainTextRejection("1900-01-01", "Supported values: 2025-11-25, 2025-06-18, 2025-03-26");
    }

    private static void assertPlainTextRejection(String protocolVersion, String expectedMessagePart) throws IOException {
        CapturedResponse captured = new CapturedResponse();
        McpTransport transport = new McpTransport(request(PING, protocolVersion), response(captured), jsonb, 1000);
        try {
            transport.init(null, config(null, null));
            fail("Expected the request to be rejected");
        } catch (HttpResponseException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getStatusCode());
            assertTrue("Expected message to include '" + expectedMessagePart + "' but was: " + e.getMessage(),
                       e.getMessage().contains(expectedMessagePart));
        }
    }

    /** The exception carries no HTTP status unless one is requested, so existing JSON-RPC errors are unchanged */
    @Test
    public void testExceptionHasNoStatusByDefault() {
        JSONRPCException plain = new JSONRPCException(JSONRPCErrorCode.INTERNAL_ERROR, List.of("x"));
        assertEquals(false, plain.hasStatusCode());

        JSONRPCException withStatus = new JSONRPCException(JSONRPCErrorCode.UNSUPPORTED_PROTOCOL_VERSION, null, 400);
        assertEquals(true, withStatus.hasStatusCode());
        assertEquals(400, withStatus.getStatusCode());
    }
}
