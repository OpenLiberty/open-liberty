/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.isolation;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.isolation.alpha.AlphaTools;
import io.openliberty.mcp.internal.fat.isolation.beta.BetaTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Multi-application isolation tests for MCP server features
 * Verifies that when 2 applications are deployed, Alpha and Beta,
 * to the same Liberty server, their MCP tools,encoders etc are
 * fully isolated from each other
 */
@RunWith(FATRunner.class)
public class MultiAppIsolationTest extends FATServletClient {
    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient alphaClient = new McpClient(server, "/alphaApp");

    @Rule
    public McpClient betaClient = new McpClient(server, "/betaApp");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive alphaWar = ShrinkWrap.create(WebArchive.class, "alphaApp.war")
                                        .addPackage(AlphaTools.class.getPackage());

        WebArchive betaWar = ShrinkWrap.create(WebArchive.class, "betaApp.war")
                                       .addPackage(BetaTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, alphaWar, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, betaWar, SERVER_ONLY);

        server.startServer();

        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/alphaApp/mcp$"));
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/betaApp/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testAlphaToolListReturnsAlphaToolsOnly() throws Exception {
        String alphaToolCallResponse = alphaClient.listAllTools();
        JSONObject jsonResponse = new JSONObject(alphaToolCallResponse);
        JSONArray tools = jsonResponse.getJSONObject("result").getJSONArray("tools");

        boolean foundAlphaTool = false;
        boolean foundBetaTool = false;
        boolean foundSharedToolName = false;

        for (int i = 0; i < tools.length(); i++) {
            String tooName = tools.getJSONObject(i).getString("name");
            if ("alphaOnlyTool".equals(tooName)) {
                foundAlphaTool = true;
            }
            if ("betaOnlyTool".equals(tooName)) {
                foundBetaTool = true;
            }
            if ("sharedToolName".equals(tooName)) {
                foundSharedToolName = true;
            }
        }

        assertTrue("Expected to find `alphaOnlyTool` in the Aplha app tool list", foundAlphaTool);
        assertFalse("Did NOT expect to find `betaOnlyTool` in the Aplha app tool list", foundBetaTool);
        assertTrue("Expected to find `sharedToolName` in the Aplha app tool list", foundSharedToolName);
    }

    @Test
    public void testBetaToolListReturnsBetaToolsOnly() throws Exception {
        String betaToolCallResponse = betaClient.listAllTools();
        JSONObject jsonResponse = new JSONObject(betaToolCallResponse);
        JSONArray tools = jsonResponse.getJSONObject("result").getJSONArray("tools");

        boolean foundAlphaTool = false;
        boolean foundBetaTool = false;
        boolean foundSharedToolName = false;

        for (int i = 0; i < tools.length(); i++) {
            String tooName = tools.getJSONObject(i).getString("name");
            if ("alphaOnlyTool".equals(tooName)) {
                foundAlphaTool = true;
            }
            if ("betaOnlyTool".equals(tooName)) {
                foundBetaTool = true;
            }
            if ("sharedToolName".equals(tooName)) {
                foundSharedToolName = true;
            }
        }

        assertTrue("Expected to find `betaOnlyTool` in the Beta app tool list", foundBetaTool);
        assertFalse("Did NOT expect to find `alphaOnlyTool` in the Beta app tool list", foundAlphaTool);
        assertTrue("Expected to find `sharedToolName` in the Beta app tool list", foundSharedToolName);
    }

    //Calling a tool that exists in one app from another app should fail with method not found
    @Test
    public void testCallingBetaToolFromAlphaAppFails() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "betaOnlyTool",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;

        String expectedResponseString = """
                        {
                            "id": "2",
                            "jsonrpc": "2.0",
                            "error": {
                                "code": -32602,
                                "data": ["Method betaOnlyTool not found"],
                                "message": "Invalid params"
                            }
                        }
                        """;

        String response = alphaClient.callMCP(request);
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testCallingAlphaToolFromBetaAppFails() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "alphaOnlyTool",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;

        String expectedResponseString = """
                        {
                            "id": "2",
                            "jsonrpc": "2.0",
                            "error": {
                                "code": -32602,
                                "data": ["Method alphaOnlyTool not found"],
                                "message": "Invalid params"
                            }
                        }
                        """;

        String response = betaClient.callMCP(request);
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    //Calling a tool with the same name deployed in different apps will return different results

    @Test
    public void testCallingToolsWithTheSameNameInDifferentAops() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "sharedToolName"
                          }
                        }
                        """;

        String expectedAlphaResponseString = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "result": {
                            "isError": false,
                            "content": [
                              {
                                "type": "text",
                                "text": "from-alpha"
                              }
                            ]
                          }
                        }
                        """;

        String expectedBetaResponseString = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "result": {
                            "isError": false,
                            "content": [
                              {
                                "type": "text",
                                "text": "from-beta"
                              }
                            ]
                          }
                        }
                        """;

        String alphaResponse = alphaClient.callMCP(request);
        JSONAssert.assertEquals(expectedAlphaResponseString, alphaResponse, true);

        String betaResponse = betaClient.callMCP(request);
        JSONAssert.assertEquals(expectedBetaResponseString, betaResponse, true);
    }

    //Test for ToolResponseEncoders

    @Test
    public void testAlphaToolUsesAlphaToolResponseEncoderForLocalDate() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "alphaEncodedTool"
                          }
                        }
                        """;

        String response = alphaClient.callMCP(request);
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "isError": false,
                            "content": [
                              {
                                "type":"text",
                                "text":"encoded by AlphaToolResponseEncoder: 2026-03-03"
                              }
                            ]
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    //Testing that the AlphaToolResponseEncoder from the Alpha app is not seen by the Beta app
    @Test
    public void testBetaToolDoesNotUseAlphaToolResponseEncoderForLocalDate() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "betaEncodedTool"
                          }
                        }
                        """;

        String response = betaClient.callMCP(request);
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "isError": false,
                            "content": [
                              {
                                "type":"text",
                                "text":"\\"2026-03-03\\""
                              }
                            ]
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    //A content encoder registered in the Beta app will be visible to only the beta tools deployed in the beta app
    @Test
    public void testBetaToolUsesBetaContentEncoderForLocalDateTime() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "betaContentEncodedTool"
                          }
                        }
                        """;

        String response = betaClient.callMCP(request);
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "isError": false,
                            "content": [
                              {
                                "type":"text",
                                "text":"encoded by BetaContentEncoder: 2026-03-03T03:03"
                              }
                            ]
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    //A content encoder registered in the Beta app will NOT be visible to Alpha tools deployed in the alpha app
    @Test
    public void testAlphaToolDoesNotUseBetaContentEncoderForLocalDateTime() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "alphaContentEncodedTool"
                          }
                        }
                        """;

        String response = alphaClient.callMCP(request);
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "isError": false,
                            "content": [
                              {
                                "type":"text",
                                "text":"\\"2026-03-03T03:03:00\\""
                              }
                            ]
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    // Negative Tests

    // --- Tool Metadata and Schema Generation ---

    /**
     * Negative test: {@code tools/list} on the <em>alpha</em> endpoint must not
     * contain {@code betaOnlyTool}'s schema, and {@code tools/list} on the <em>beta</em>
     * endpoint must not contain {@code alphaOnlyTool}'s schema. Schema isolation must
     * mirror tool-call isolation.
     */
    @Test
    public void testToolListSchemaIsolatedBetweenApps() throws Exception {
        String alphaResponse = alphaClient.listAllTools();
        String betaResponse = betaClient.listAllTools();

        JSONArray alphaTools = new JSONObject(alphaResponse).getJSONObject("result").getJSONArray("tools");
        JSONArray betaTools = new JSONObject(betaResponse).getJSONObject("result").getJSONArray("tools");

        // Alpha must not expose betaOnlyTool's schema
        for (int i = 0; i < alphaTools.length(); i++) {
            assertFalse("Alpha tools/list must not expose betaOnlyTool",
                        "betaOnlyTool".equals(alphaTools.getJSONObject(i).getString("name")));
        }

        // Beta must not expose alphaOnlyTool's schema
        for (int i = 0; i < betaTools.length(); i++) {
            assertFalse("Beta tools/list must not expose alphaOnlyTool",
                        "alphaOnlyTool".equals(betaTools.getJSONObject(i).getString("name")));
        }
    }

    /**
     * Negative test: the {@code sharedToolName} tool exists in both apps but their
     * {@code inputSchema} shapes must not bleed across; each app's listing must only
     * describe its own tool definition (description comes from {@code AlphaTools} vs
     * {@code BetaTools} respectively).
     */
    @Test
    public void testSharedToolNameSchemaIsIsolatedPerApp() throws Exception {
        String alphaResponse = alphaClient.listAllTools();
        String betaResponse = betaClient.listAllTools();

        JSONObject alphaShared = null;
        JSONObject betaShared = null;

        JSONArray alphaTools = new JSONObject(alphaResponse).getJSONObject("result").getJSONArray("tools");
        for (int i = 0; i < alphaTools.length(); i++) {
            JSONObject t = alphaTools.getJSONObject(i);
            if ("sharedToolName".equals(t.getString("name"))) {
                alphaShared = t;
                break;
            }
        }

        JSONArray betaTools = new JSONObject(betaResponse).getJSONObject("result").getJSONArray("tools");
        for (int i = 0; i < betaTools.length(); i++) {
            JSONObject t = betaTools.getJSONObject(i);
            if ("sharedToolName".equals(t.getString("name"))) {
                betaShared = t;
                break;
            }
        }

        assertNotNull("sharedToolName must appear in alpha tools/list", alphaShared);
        assertNotNull("sharedToolName must appear in beta tools/list", betaShared);

        // Each listing must carry its own app-specific title/description, not the other's
        String alphaTitle = alphaShared.optString("title", "");
        String betaTitle = betaShared.optString("title", "");
        assertFalse("Alpha's sharedToolName must not carry Beta's title",
                    "Shared tool name in Beta".equals(alphaTitle));
        assertFalse("Beta's sharedToolName must not carry Alpha's title",
                    "Shared tool name in Alpha".equals(betaTitle));
    }

    // --- Monitoring and Management ---

    /**
     * Negative test: calling {@code alphaOnlyTool} via the <em>alpha</em> client and
     * then calling {@code betaOnlyTool} via the <em>beta</em> client must each succeed
     * independently; the alpha session must not affect the beta session (no
     * cross-app state contamination via shared MBeans or shared bean instances).
     */
    @Test
    public void testCrossAppCallsRemainIndependent() throws Exception {
        String alphaRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": "iso-1",
                          "method": "tools/call",
                          "params": {
                            "name": "alphaOnlyTool",
                            "arguments": {"input": "ping"}
                          }
                        }
                        """;
        String betaRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": "iso-2",
                          "method": "tools/call",
                          "params": {
                            "name": "betaOnlyTool",
                            "arguments": {"input": "ping"}
                          }
                        }
                        """;

        String alphaResponse = alphaClient.callMCP(alphaRequest);
        String betaResponse = betaClient.callMCP(betaRequest);

        // Both must succeed with isError:false
        assertFalse("Alpha response must not indicate an error",
                    new JSONObject(alphaResponse).getJSONObject("result").getBoolean("isError"));
        assertFalse("Beta response must not indicate an error",
                    new JSONObject(betaResponse).getJSONObject("result").getBoolean("isError"));

        // Responses must not cross-contaminate; each must contain its own app prefix
        assertTrue("Alpha response text must come from alphaOnlyTool",
                   alphaResponse.contains("alpha-response"));
        assertTrue("Beta response text must come from betaOnlyTool",
                   betaResponse.contains("beta-response"));
    }
}
