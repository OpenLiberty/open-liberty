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
    public void testAlphaToolListReturnsAlpaToolsOnly() throws Exception {
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

}
