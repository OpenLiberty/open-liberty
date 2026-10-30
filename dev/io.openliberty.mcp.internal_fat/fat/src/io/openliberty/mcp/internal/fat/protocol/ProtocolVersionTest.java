/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.protocol;

import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_SESSION_ID;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_APPLICATION_JSON;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
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
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.suite.BetaRepeatAction;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.TestConstants;

/**
 * Tests the protocol version handling on a normal (non-beta) runtime. This class is skipped in the
 * repeat which runs on a beta runtime, where the equivalent tests are in {@link ProtocolVersionBetaTest}.
 */
@RunWith(FATRunner.class)
@SkipForRepeat(BetaRepeatAction.ID)
public class ProtocolVersionTest {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "protocolVersionTest";

    private static final String[] GA_VERSIONS = { "2025-11-25", "2025-06-18", "2025-03-26" };
    private static final String[] NEAR_MISS_VERSIONS = { "2026-07-29", "2026-7-28", "not-a-version" };

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
    public void testMissingMcpProtocolVersionHeader() throws Exception {
        String request = """
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

        HttpRequest httpRequest = new HttpRequest(serverLifecycle.getServer(), "/" + APP_NAME + "/mcp")
                                                                                     .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                     .requestProp(MCP_SESSION_ID, client.getSessionId())
                                                                                     .jsonBody(request)
                                                                                     .method("POST")
                                                                                     .expectCode(200);
        String response = httpRequest.run(String.class);

        String expectedResponse = """
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

        JSONAssert.assertEquals(expectedResponse, response, true);

        String contentType = httpRequest.getResponseHeader("Content-Type");
        assertThat(contentType, containsString(VALUE_APPLICATION_JSON));
    }

    @Test
    public void testInitializeDoesNotNeedMcpProtocolVersionHeader() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "initialize",
                         "params": {
                            "protocolVersion": "2025-11-25",
                            "clientInfo": {
                              "name": "test-client",
                              "version": "0.1"
                            },
                            "rootUri": "file:/test/root"
                          }
                        }
                        """;

        HttpRequest httpRequest = new HttpRequest(serverLifecycle.getServer(), "/" + APP_NAME + "/mcp")
                                                                                     .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                     .jsonBody(request)
                                                                                     .method("POST")
                                                                                     .expectCode(200);
        String response = httpRequest.run(String.class);

        assertTrue("Expected response to contain result", response.contains("\"result\""));
        assertTrue("Expected protocolVersion field in response", response.contains("\"protocolVersion\""));
        assertTrue("Expected serverInfo field in response", response.contains("\"serverInfo\""));

        String contentType = httpRequest.getResponseHeader("Content-Type");
        assertThat(contentType, containsString(TestConstants.VALUE_APPLICATION_JSON));
    }

    @Test
    public void testRejectsUnsupportedProtocolVersion() throws Exception {
        String response = client.callMCPRaw(ECHO_REQUEST)
                                .protocolVersion("2022-02-02")
                                .expectedStatus(400)
                                .run()
                                .body();

        assertThat("Expected error message about invalid protocol version",
                   response, containsString("An unsupported MCP-Protocol-Version header was provided."));
        assertSupportedValuesAreGaOnly(response);
    }

    /**
     * 2026-07-28 is only available on a beta runtime. This server is not beta, so the header must be
     * rejected and the version must not be advertised in the supported values.
     */
    @Test
    public void testRejectsBetaProtocolVersionOnNonBeta() throws Exception {
        String response = client.callMCPRaw(ECHO_REQUEST)
                                .protocolVersion("2026-07-28")
                                .expectedStatus(400)
                                .run()
                                .body();

        assertThat("Expected error message about invalid protocol version",
                   response, containsString("An unsupported MCP-Protocol-Version header was provided."));
        assertSupportedValuesAreGaOnly(response);
    }

    /**
     * A client requesting the beta version in {@code initialize} on a non-beta server is treated like
     * any other unsupported version: the server responds with its preferred version.
     */
    @Test
    public void testInitializeWithBetaProtocolVersionFallsBackOnNonBeta() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2026-07-28",
                            "clientInfo": {
                              "name": "test-client",
                              "version": "0.1"
                            }
                          }
                        }
                        """;

        String response = new HttpRequest(serverLifecycle.getServer(), "/" + APP_NAME + "/mcp")
                                                                             .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                             .jsonBody(request)
                                                                             .method("POST")
                                                                             .expectCode(200)
                                                                             .run(String.class);

        String negotiated = new JSONObject(response).getJSONObject("result").getString("protocolVersion");
        assertEquals("Expected server to respond with its preferred GA version, not the beta version", "2025-11-25", negotiated);
    }

    /**
     * Every GA version must still be accepted as a header on a non-beta runtime, and the response must not
     * be affected by the beta version existing in the enum.
     */
    @Test
    public void testAcceptsEachGaProtocolVersionHeaderOnNonBeta() throws Exception {
        for (String version : GA_VERSIONS) {
            String response = client.callMCPRaw(ECHO_REQUEST)
                                    .protocolVersion(version)
                                    .run()
                                    .body();

            JSONAssert.assertEquals("Unexpected response for MCP-Protocol-Version " + version, ECHO_RESPONSE, response, true);
        }
    }

    /**
     * Each GA version requested in {@code initialize} must be negotiated as itself on a non-beta runtime.
     */
    @Test
    public void testInitializeNegotiatesEachGaProtocolVersionOnNonBeta() throws Exception {
        for (String version : GA_VERSIONS) {
            String response = initialize(version);
            String negotiated = new JSONObject(response).getJSONObject("result").getString("protocolVersion");
            assertEquals("Expected " + version + " to be negotiated as itself", version, negotiated);
        }
    }

    /**
     * Versions which look like, but are not exactly, the beta version must be rejected the same way as any
     * other unsupported version, with only the GA versions listed.
     */
    @Test
    public void testRejectsNearMissesOfBetaProtocolVersionOnNonBeta() throws Exception {
        for (String version : NEAR_MISS_VERSIONS) {
            String response = client.callMCPRaw(ECHO_REQUEST)
                                    .protocolVersion(version)
                                    .expectedStatus(400)
                                    .run()
                                    .body();

            assertThat("Expected error message about invalid protocol version for " + version,
                       response, containsString("An unsupported MCP-Protocol-Version header was provided."));
            assertSupportedValuesAreGaOnly(response);
        }
    }

    private String initialize(String protocolVersion) throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "%s",
                            "clientInfo": {
                              "name": "test-client",
                              "version": "0.1"
                            }
                          }
                        }
                        """.formatted(protocolVersion);

        return new HttpRequest(serverLifecycle.getServer(), "/" + APP_NAME + "/mcp")
                                                                             .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                             .jsonBody(request)
                                                                             .method("POST")
                                                                             .expectCode(200)
                                                                             .run(String.class);
    }

    /**
     * Asserts that the supported values listed in an unsupported-version error response are exactly the GA
     * versions, in order, and that the beta version is not mentioned anywhere in the response.
     * <p>
     * Comparing the whole list, rather than checking that the response contains the expected text, means this
     * also fails if a version is added to the end of the list.
     */
    private static void assertSupportedValuesAreGaOnly(String response) {
        String marker = "Supported values: ";
        int start = response.indexOf(marker);
        assertTrue("Expected the response to list the supported values but was: " + response, start >= 0);

        String supportedValues = response.substring(start + marker.length()).trim();
        assertEquals("Expected the supported values to be exactly the GA versions",
                     "2025-11-25, 2025-06-18, 2025-03-26", supportedValues);
        assertFalse("Expected the beta version not to be mentioned on a non-beta runtime but was: " + response,
                    response.contains("2026-07-28"));
    }

}
