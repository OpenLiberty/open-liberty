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
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_SESSION_ID;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.suite.McpMainServerSuite;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Tests that the beta MCP protocol version 2026-07-28 is usable when the runtime is a beta runtime.
 * <p>
 * The beta edition flag is a JVM option, so this class restarts the shared {@code mcp-server} with
 * {@code -Dcom.ibm.ws.beta.edition=true} for the duration of the class and restores the original JVM options
 * and restarts it again afterwards. This is an exception to the {@link McpMainServerSuite} rule that tests
 * must not restart the server, so the server must always be returned to its non-beta state in {@link #teardown()}.
 * The equivalent non-beta behaviour is covered in {@link ProtocolVersionTest}.
 */
@RunWith(FATRunner.class)
public class ProtocolVersionBetaTest {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "protocolVersionBetaTest";
    private static final String BETA_OPTION = "-Dcom.ibm.ws.beta.edition";

    /** The JVM options the server had before this class changed them, restored in {@link #teardown()} */
    private static Map<String, String> originalJvmOptions;

    private static final String[] GA_VERSIONS = { "2025-11-25", "2025-06-18", "2025-03-26" };
    private static final String[] INVALID_VERSIONS = { "2026-07-29", "2026-7-28", "not-a-version" };

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

    private static final String INITIALIZE_REQUEST = """
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
                    """;

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();

    @BeforeClass
    public static void setup() throws Exception {
        LibertyServer server = serverLifecycle.getServer();

        // Restart the server as a beta runtime
        server.stopServer();
        originalJvmOptions = server.getJvmOptionsAsMap();
        Map<String, String> betaOptions = server.getJvmOptionsAsMap();
        betaOptions.put(BETA_OPTION, "true");
        server.setJvmOptions(betaOptions);
        server.startServer();

        server.setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addPackage(BasicTools.class.getPackage());
        serverLifecycle.deployDropinApp(war);
    }

    @AfterClass
    public static void teardown() throws Exception {
        LibertyServer server = serverLifecycle.getServer();
        try {
            serverLifecycle.undeployDropinApp(APP_NAME);
        } finally {
            // Always return the shared server to its original, non-beta state for the remaining tests in the suite.
            // originalJvmOptions is null if setup() failed before the options were read, in which case nothing was changed.
            server.stopServer();
            if (originalJvmOptions != null) {
                server.setJvmOptions(originalJvmOptions);
            }
            server.startServer();
            server.setMarkToEndOfLog();
        }
    }

    @Test
    public void testAcceptsBetaProtocolVersionHeaderOnBeta() throws Exception {
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

        String response = new HttpRequest(serverLifecycle.getServer(), "/" + APP_NAME + "/mcp")
                                                                                               .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                               .requestProp(MCP_PROTOCOL_VERSION, "2026-07-28")
                                                                                               .requestProp(MCP_SESSION_ID, client.getSessionId())
                                                                                               .jsonBody(request)
                                                                                               .method("POST")
                                                                                               .expectCode(200)
                                                                                               .run(String.class);

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
    }

    @Test
    public void testInitializeNegotiatesBetaProtocolVersionOnBeta() throws Exception {
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
        assertEquals("Expected the beta version to be negotiated on a beta runtime", "2026-07-28", negotiated);
    }

    @Test
    public void testSupportedValuesIncludeBetaVersionOnBeta() throws Exception {
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

        String response = new HttpRequest(serverLifecycle.getServer(), "/" + APP_NAME + "/mcp")
                                                                                               .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                               .requestProp(MCP_PROTOCOL_VERSION, "2022-02-02")
                                                                                               .requestProp(MCP_SESSION_ID, client.getSessionId())
                                                                                               .jsonBody(request)
                                                                                               .method("POST")
                                                                                               .expectCode(400)
                                                                                               .run(String.class);

        assertThat("Expected error message about invalid protocol version",
                   response, containsString("An unsupported MCP-Protocol-Version header was provided."));
        assertSupportedValuesAreBetaAndGa(response);
    }

    /**
     * Being a beta runtime must not make the server accept arbitrary versions: values which are not exactly a
     * known version are still rejected, and the supported values list is exactly the beta plus GA versions.
     */
    @Test
    public void testRejectsInvalidProtocolVersionsOnBeta() throws Exception {
        for (String version : INVALID_VERSIONS) {
            String response = new HttpRequest(serverLifecycle.getServer(), "/" + APP_NAME + "/mcp")
                                                                                                   .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                                   .requestProp(MCP_PROTOCOL_VERSION, version)
                                                                                                   .requestProp(MCP_SESSION_ID, client.getSessionId())
                                                                                                   .jsonBody(ECHO_REQUEST)
                                                                                                   .method("POST")
                                                                                                   .expectCode(400)
                                                                                                   .run(String.class);

            assertThat("Expected error message about invalid protocol version for " + version,
                       response, containsString("An unsupported MCP-Protocol-Version header was provided."));
            assertSupportedValuesAreBetaAndGa(response);
        }
    }

    /**
     * Every GA version must still be accepted as a header on a beta runtime.
     */
    @Test
    public void testAcceptsEachGaProtocolVersionHeaderOnBeta() throws Exception {
        for (String version : GA_VERSIONS) {
            String response = new HttpRequest(serverLifecycle.getServer(), "/" + APP_NAME + "/mcp")
                                                                                                   .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                                   .requestProp(MCP_PROTOCOL_VERSION, version)
                                                                                                   .requestProp(MCP_SESSION_ID, client.getSessionId())
                                                                                                   .jsonBody(ECHO_REQUEST)
                                                                                                   .method("POST")
                                                                                                   .expectCode(200)
                                                                                                   .run(String.class);

            JSONAssert.assertEquals("Unexpected response for MCP-Protocol-Version " + version, ECHO_RESPONSE, response, true);
        }
    }

    /**
     * Each GA version requested in {@code initialize} must still be negotiated as itself on a beta runtime,
     * rather than being upgraded to the beta version.
     */
    @Test
    public void testInitializeNegotiatesEachGaProtocolVersionOnBeta() throws Exception {
        for (String version : GA_VERSIONS) {
            String request = INITIALIZE_REQUEST.formatted(version);

            String response = new HttpRequest(serverLifecycle.getServer(), "/" + APP_NAME + "/mcp")
                                                                                                   .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                                   .jsonBody(request)
                                                                                                   .method("POST")
                                                                                                   .expectCode(200)
                                                                                                   .run(String.class);

            String negotiated = new JSONObject(response).getJSONObject("result").getString("protocolVersion");
            assertEquals("Expected " + version + " to be negotiated as itself", version, negotiated);
        }
    }

    /**
     * Asserts that the supported values listed in an unsupported-version error response are exactly the beta
     * version followed by the GA versions, in order.
     */
    private static void assertSupportedValuesAreBetaAndGa(String response) {
        String marker = "Supported values: ";
        int start = response.indexOf(marker);
        assertTrue("Expected the response to list the supported values but was: " + response, start >= 0);

        String supportedValues = response.substring(start + marker.length()).trim();
        assertEquals("Expected the supported values to be exactly the beta and GA versions",
                     "2026-07-28, 2025-11-25, 2025-06-18, 2025-03-26", supportedValues);
    }
}
