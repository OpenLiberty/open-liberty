
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

import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.assertThrows;
import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.exception;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;

import java.io.StringReader;

import org.junit.Test;

import io.openliberty.mcp.internal.HeaderValidation;
import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.McpOperationPartialRequest;
import io.openliberty.mcp.internal.requests.parsing.JsonRpcRequestTO;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/**
 * Tests that the MCP-Protocol-Version header is validated against the protocol version in the request body's
 * {@code _meta} for protocol version 2026-07-28.
 */
public class HeaderProtocolVersionValidationTest {

    private static McpOperationPartialRequest partialRequest(String json) {

        try (JsonReader reader = Json.createReader(new StringReader(json))) {

            JsonObject body = reader.readObject();

            JsonRpcRequestTO requestTo = new JsonRpcRequestTO(
                                                              body.getString("jsonrpc"),
                                                              body.getString("method"),
                                                              body.get("id"),
                                                              body.getJsonObject("params"));

            return new McpOperationPartialRequest(
                                                  null,
                                                  McpProtocolVersion.V_2026_07_28,
                                                  true,
                                                  null,
                                                  requestTo,
                                                  null,
                                                  null,
                                                  null);
        }
    }

    private static McpOperationPartialRequest request(String idJson, String paramsJson) {

        String id = idJson == null ? "" : "\"id\": " + idJson + ",";

        return partialRequest("""
                        {
                          "jsonrpc": "2.0",
                          %s
                          "method": "tools/call",
                          "params": %s
                        }
                        """.formatted(id, paramsJson));
    }

    private static McpOperationPartialRequest requestWithMeta(String metaJson) {

        return request("1", "{\"name\": \"echo\", \"_meta\": " + metaJson + "}");
    }

    private static void assertHeaderMismatch(McpProtocolVersion header, McpOperationPartialRequest request) {

        assertThrows(() -> HeaderValidation.validateProtocolVersionMatchesBody(header, request),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(JSONRPCErrorCode.HEADER_MISMATCH))));
    }

    @Test
    public void testHeaderMismatchErrorCode() {

        assertEquals(-32020, JSONRPCErrorCode.HEADER_MISMATCH.getCode());
    }

    @Test
    public void testMatchingVersionAccepted() {

        McpOperationPartialRequest request = requestWithMeta(
                                                             "{\"io.modelcontextprotocol/protocolVersion\": \"2026-07-28\"}");

        HeaderValidation.validateProtocolVersionMatchesBody(
                                                            McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testMismatchedVersionRejected() {

        McpOperationPartialRequest request = requestWithMeta(
                                                             "{\"io.modelcontextprotocol/protocolVersion\": \"2025-11-25\"}");

        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testMismatchRejectedWithBadRequestStatus() {

        McpOperationPartialRequest request = requestWithMeta(
                                                             "{\"io.modelcontextprotocol/protocolVersion\": \"2025-11-25\"}");

        assertThrows(() -> HeaderValidation.validateProtocolVersionMatchesBody(McpProtocolVersion.V_2026_07_28, request),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("statusCode", equalTo(400))));
    }

    @Test
    public void testMissingMetaRejected() {

        McpOperationPartialRequest request = request(
                                                     "1", "{\"name\": \"echo\"}");

        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testMissingParamsRejected() {

        McpOperationPartialRequest request = partialRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "ping"
                        }
                        """);

        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testMetaWithoutProtocolVersionRejected() {

        McpOperationPartialRequest request = requestWithMeta(
                                                             "{\"other/key\": \"value\"}");

        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testNonStringProtocolVersionRejected() {

        McpOperationPartialRequest request = requestWithMeta(
                                                             "{\"io.modelcontextprotocol/protocolVersion\": 20260728}");

        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testNonObjectMetaRejected() {

        McpOperationPartialRequest request = requestWithMeta(
                                                             "\"2026-07-28\"");

        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, request);
    }

    @Test
    public void testNotificationMismatchRejected() {

        McpOperationPartialRequest notification = request(null, "{}");

        assertHeaderMismatch(McpProtocolVersion.V_2026_07_28, notification);
    }

    @Test
    public void testNotificationMatchingVersionAccepted() {

        McpOperationPartialRequest notification = request(
                                                          null,
                                                          "{\"_meta\": {\"io.modelcontextprotocol/protocolVersion\": \"2026-07-28\"}}");

        HeaderValidation.validateProtocolVersionMatchesBody(
                                                            McpProtocolVersion.V_2026_07_28, notification);
    }

    private static final McpProtocolVersion[] GA_VERSIONS = {
                                                              McpProtocolVersion.V_2025_11_25,
                                                              McpProtocolVersion.V_2025_06_18,
                                                              McpProtocolVersion.V_2025_03_26
    };

    /**
     * The body is the canonical source of the version, so a version in the body must match the header even
     * for versions before 2026-07-28.
     */
    @Test
    public void testGaVersionsMismatchRejected() {

        McpOperationPartialRequest mismatched = requestWithMeta(
                                                                "{\"io.modelcontextprotocol/protocolVersion\": \"2026-07-28\"}");

        for (McpProtocolVersion version : GA_VERSIONS) {
            assertHeaderMismatch(version, mismatched);
        }
    }

    @Test
    public void testGaVersionsMatchingVersionAccepted() {

        for (McpProtocolVersion version : GA_VERSIONS) {
            McpOperationPartialRequest matching = requestWithMeta(
                                                                  "{\"io.modelcontextprotocol/protocolVersion\": \"" + version.getVersion() + "\"}");

            HeaderValidation.validateProtocolVersionMatchesBody(version, matching);
        }
    }

    /**
     * Versions before 2026-07-28 do not have to carry the version in the body.
     */
    @Test
    public void testGaVersionsWithoutVersionInBodyAccepted() {

        McpOperationPartialRequest noMeta = request(
                                                    "1", "{\"name\": \"echo\"}");

        for (McpProtocolVersion version : GA_VERSIONS) {
            HeaderValidation.validateProtocolVersionMatchesBody(version, noMeta);
        }
    }
}
