/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.protocol;

import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.suite.BetaRepeatAction;
import io.openliberty.mcp.internal.fat.suite.McpMainServerSuite;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.McpClient.McpDetailedAuthResponse;

/**
 * Tests that, for protocol version 2026-07-28, the MCP-Protocol-Version header is validated against the
 * {@code io.modelcontextprotocol/protocolVersion} value in the request body's {@code _meta}.
 * <p>
 * 2026-07-28 is only available on a beta runtime. {@link McpMainServerSuite} repeats its tests on a beta runtime
 * (see {@link BetaRepeatAction}), and this class is skipped in the repeat which runs on a normal runtime.
 */
@RunWith(FATRunner.class)
@SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
public class ProtocolVersionHeaderValidationBetaTest {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "protocolVersionHeaderValidationBetaTest";

    private static final String[] GA_VERSIONS = { "2025-11-25", "2025-06-18", "2025-03-26" };
    private static final String META_2026_07_28 = "{\"io.modelcontextprotocol/protocolVersion\": \"2026-07-28\"}";

    private static final String ECHO_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "method": "tools/call",
                      "params": {
                        "name": "echo",
                        "arguments": {
                          "input": "Hello"
                        }
                      }
                    }
                    """;

    private static final String ECHO_REQUEST_WITH_META = """
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "method": "tools/call",
                      "params": {
                        "name": "echo",
                        "arguments": {
                          "input": "Hello"
                        },
                        "_meta": %s
                      }
                    }
                    """;

    private static final String ECHO_RESPONSE = """
                    {
                      "id": 1,
                      "jsonrpc": "2.0",
                      "result": {
                        "content": [
                          {
                            "type": "text",
                            "text": "Hello"
                          }
                        ],
                        "isError": false
                      }
                    }""";

    /** The HeaderMismatch error response, with the id to be filled in */
    private static final String HEADER_MISMATCH_RESPONSE = """
                    {
                      "jsonrpc": "2.0",
                      "id": %s,
                      "error": {
                        "code": -32020,
                        "message": "Header mismatch",
                        "data": ["The MCP-Protocol-Version header must match the io.modelcontextprotocol/protocolVersion field in the request _meta object."]
                      }
                    }
                    """;

    private static final String NOTIFICATION_WITH_META = """
                    {
                      "jsonrpc": "2.0",
                      "method": "notifications/initialized",
                      "params": {
                        "_meta": %s
                      }
                    }
                    """;

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addPackage(BasicTools.class.getPackage());
        serverLifecycle.deployDropinApp(war);
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testMatchingHeaderAndBodyMetaIsAccepted() throws Exception {
        String response = client.callMCPRaw(ECHO_REQUEST_WITH_META.formatted(META_2026_07_28))
                                .protocolVersion("2026-07-28")
                                .run()
                                .body();

        JSONAssert.assertEquals(ECHO_RESPONSE, response, true);
    }

    @Test
    public void testHeaderMismatchWithBodyMetaIsRejected() throws Exception {
        String request = ECHO_REQUEST_WITH_META.formatted("{\"io.modelcontextprotocol/protocolVersion\": \"2025-11-25\"}");

        McpDetailedAuthResponse response = client.callMCPRaw(request)
                                                 .protocolVersion("2026-07-28")
                                                 .expectedStatus(400)
                                                 .run();

        assertHeaderMismatch(response.body(), "1");
    }

    @Test
    public void testMissingBodyMetaIsRejected() throws Exception {
        McpDetailedAuthResponse response = client.callMCPRaw(ECHO_REQUEST)
                                                 .protocolVersion("2026-07-28")
                                                 .expectedStatus(400)
                                                 .run();

        assertHeaderMismatch(response.body(), "1");
    }

    @Test
    public void testMetaWithoutProtocolVersionIsRejected() throws Exception {
        McpDetailedAuthResponse response = client.callMCPRaw(ECHO_REQUEST_WITH_META.formatted("{\"other/key\": \"value\"}"))
                                                 .protocolVersion("2026-07-28")
                                                 .expectedStatus(400)
                                                 .run();

        assertHeaderMismatch(response.body(), "1");
    }

    /**
     * A header mismatch is a validation failure which the spec requires to be rejected with HTTP 400, even for a
     * notification. The spec allows the body to be a JSON-RPC error response which has no id.
     */
    @Test
    public void testHeaderMismatchOnNotificationIsRejected() throws Exception {
        String notification = NOTIFICATION_WITH_META.formatted("{\"io.modelcontextprotocol/protocolVersion\": \"2025-11-25\"}");

        McpDetailedAuthResponse response = client.callMCPRaw(notification)
                                                 .protocolVersion("2026-07-28")
                                                 .expectedStatus(400)
                                                 .run();

        // A notification has no id, so the error response carries a null id
        assertHeaderMismatch(response.body(), "null");
    }

    /**
     * A notification whose header and body match must still be accepted with 202 and no body.
     */
    @Test
    public void testMatchingHeaderOnNotificationIsAccepted() throws Exception {
        McpDetailedAuthResponse response = client.callMCPRaw(NOTIFICATION_WITH_META.formatted(META_2026_07_28))
                                                 .protocolVersion("2026-07-28")
                                                 .expectedStatus(202)
                                                 .run();

        assertTrue("Expected no response body for a notification but was: " + response.body(),
                   response.body() == null || response.body().isEmpty());
    }

    /**
     * The body is the canonical source of the protocol version, so a version in the body must match the header
     * even for versions before 2026-07-28.
     */
    @Test
    public void testHeaderMismatchWithBodyMetaIsRejectedForGaVersions() throws Exception {
        String request = ECHO_REQUEST_WITH_META.formatted(META_2026_07_28);
        for (String version : GA_VERSIONS) {
            McpDetailedAuthResponse response = client.callMCPRaw(request)
                                                     .protocolVersion(version)
                                                     .expectedStatus(400)
                                                     .run();

            assertHeaderMismatch(response.body(), "1");
        }
    }

    /**
     * Versions before 2026-07-28 do not have to carry the version in the body.
     */
    @Test
    public void testMissingBodyMetaIsAcceptedForGaVersions() throws Exception {
        for (String version : GA_VERSIONS) {
            String response = client.callMCPRaw(ECHO_REQUEST)
                                    .protocolVersion(version)
                                    .run()
                                    .body();

            JSONAssert.assertEquals("Unexpected response for MCP-Protocol-Version " + version, ECHO_RESPONSE, response, true);
        }
    }

    /**
     * Asserts that the response body is a JSON-RPC HeaderMismatch (-32020) error carrying the request id.
     *
     * @param body the response body
     * @param idJson the expected id, as JSON. {@code 1} for the echo request, {@code null} for a notification
     */
    private static void assertHeaderMismatch(String body, String idJson) throws Exception {
        JSONAssert.assertEquals(HEADER_MISMATCH_RESPONSE.formatted(idJson), body, true);
    }
}
