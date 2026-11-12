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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONObject;
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

        assertHeaderMismatch(response.body());
    }

    @Test
    public void testMissingBodyMetaIsRejected() throws Exception {
        McpDetailedAuthResponse response = client.callMCPRaw(ECHO_REQUEST)
                                                 .protocolVersion("2026-07-28")
                                                 .expectedStatus(400)
                                                 .run();

        assertHeaderMismatch(response.body());
    }

    @Test
    public void testMetaWithoutProtocolVersionIsRejected() throws Exception {
        McpDetailedAuthResponse response = client.callMCPRaw(ECHO_REQUEST_WITH_META.formatted("{\"other/key\": \"value\"}"))
                                                 .protocolVersion("2026-07-28")
                                                 .expectedStatus(400)
                                                 .run();

        assertHeaderMismatch(response.body());
    }

    /**
     * Earlier protocol versions do not carry the version in the body, so a _meta value must not be compared
     * with the header, even on a beta runtime.
     */
    @Test
    public void testBodyMetaIsNotValidatedForGaVersions() throws Exception {
        String request = ECHO_REQUEST_WITH_META.formatted(META_2026_07_28);
        for (String version : GA_VERSIONS) {
            String response = client.callMCPRaw(request)
                                    .protocolVersion(version)
                                    .run()
                                    .body();

            JSONAssert.assertEquals("Unexpected response for MCP-Protocol-Version " + version, ECHO_RESPONSE, response, true);
        }
    }

    /**
     * Asserts that the response body is a JSON-RPC HeaderMismatch (-32020) error carrying the request id.
     */
    private static void assertHeaderMismatch(String body) throws Exception {
        JSONObject json = new JSONObject(body);
        assertEquals(1, json.getInt("id"));
        assertEquals(-32020, json.getJSONObject("error").getInt("code"));
        assertTrue("Expected the error to describe the mismatch but was: " + body,
                   json.getJSONObject("error").toString().contains("io.modelcontextprotocol/protocolVersion"));
    }
}
