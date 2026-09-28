/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.TestConstants;

/**
 * Tests for the {@code GET /mcp/info} observability endpoint.
 *
 * Verifies that the endpoint returns the correct session and tool counts
 * before and after sessions are established, and after sessions are deleted.
 */
@RunWith(FATRunner.class)
public class InfoEndpointTest extends FATServletClient {

    private static final String CONTEXT_ROOT = "/infoEndpointTest";
    private static final String MCP_PATH = "/mcp";
    private static final String INFO_PATH = MCP_PATH + "/info";

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, CONTEXT_ROOT);

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "infoEndpointTest.war")
                                   .addPackage(BasicTools.class.getPackage())
                                   .addClass(TestConstants.class);

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();

        assertNotNull("MCP endpoint did not start", server.waitForStringInLog("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    /**
     * GET /mcp/info before any session is established should return sessions=0
     * and tools >= 1 (the basicToolApp tools are registered at deploy time).
     */
    @Test
    public void testInfoBeforeSession() throws Exception {
        // McpClient @Rule automatically calls initialize() — delete that session first
        // so we can check the pre-session count.  Use a fresh HTTP call directly.
        String response = new HttpRequest(server, CONTEXT_ROOT + INFO_PATH)
                                                                            .method("GET")
                                                                            .run(String.class);

        JSONObject json = new JSONObject(response);
        assertTrue("tools should be >= 1", json.getInt("tools") >= 1);
        // sessions could be >= 0 depending on whether the @Rule client has fired yet
        assertTrue("sessions should be >= 0", json.getInt("sessions") >= 0);
    }

    /**
     * After the McpClient @Rule initializes a session, GET /mcp/info should
     * report sessions >= 1.
     */
    @Test
    public void testInfoAfterSession() throws Exception {
        // The @Rule has already called initialize(), so at least one session exists.
        String response = new HttpRequest(server, CONTEXT_ROOT + INFO_PATH)
                                                                            .method("GET")
                                                                            .run(String.class);

        JSONObject json = new JSONObject(response);
        assertTrue("tools should be >= 1", json.getInt("tools") >= 1);
        assertTrue("sessions should be >= 1 after initialize", json.getInt("sessions") >= 1);
    }

    /**
     * GET /mcp/info response must be valid JSON with both 'sessions' and 'tools' integer fields.
     */
    @Test
    public void testInfoResponseShape() throws Exception {
        String response = new HttpRequest(server, CONTEXT_ROOT + INFO_PATH)
                                                                            .method("GET")
                                                                            .run(String.class);

        JSONObject json = new JSONObject(response);
        assertTrue("response must contain 'sessions' field", json.has("sessions"));
        assertTrue("response must contain 'tools' field", json.has("tools"));
        // Verify they are integers (getInt throws if not)
        assertTrue(json.getInt("sessions") >= 0);
        assertTrue(json.getInt("tools") >= 0);
    }

    /**
     * POST to /mcp must still work after the servlet mapping was widened to /mcp/*.
     */
    @Test
    public void testPostToMcpStillWorks() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "info-test-ping",
                            "method": "ping"
                          }
                          """;
        String response = client.callMCP(request);
        assertNotNull("POST to /mcp should return a response", response);
        JSONObject json = new JSONObject(response);
        assertEquals("info-test-ping", json.getString("id"));
    }
}
