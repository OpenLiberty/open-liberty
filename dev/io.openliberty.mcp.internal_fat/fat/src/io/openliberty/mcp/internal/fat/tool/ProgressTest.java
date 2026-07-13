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
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import io.openliberty.mcp.internal.fat.tool.progressApp.ProgressTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Integration test that verifies:
 * <ol>
 *   <li>{@code Progress} can be declared as a parameter of a {@code @Tool} method.</li>
 *   <li>The no-op {@code ProgressImpl} is injected when the client does not supply a
 *       progress token, causing {@code progress.token()} to return an empty
 *       {@link java.util.Optional}.</li>
 *   <li>{@code RequestFeatureArguments.progress()} exposes the {@code Progress} instance
 *       that reaches the tool method (transitively confirmed by the tool executing
 *       successfully without error).</li>
 * </ol>
 */
@RunWith(FATRunner.class)
public class ProgressTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/progressTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "progressTest.war")
                                   .addPackage(ProgressTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();

        assertNotNull("MCP server endpoint did not appear in log",
                      server.waitForStringInLog("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    /**
     * Verifies that a {@code @Tool} method with a {@link org.mcpjava.server.progress.Progress}
     * parameter is invoked successfully and that the no-op {@code ProgressImpl} reports no
     * progress token (i.e. {@code progress.token().isPresent() == false}).
     */
    @Test
    public void testProgressInjectedWithNoToken() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "1",
                            "method": "tools/call",
                            "params": {
                              "name": "progressTool",
                              "arguments": {
                                "input": "hello"
                              }
                            }
                          }
                          """;

        String response = client.callMCP(request);

        // The tool returns "progressTokenPresent=false,input=hello" when no progress
        // token is present, confirming the no-op ProgressImpl was injected.
        String expectedResponse = """
                        {
                          "id": "1",
                          "jsonrpc": "2.0",
                          "result": {
                            "content": [
                              {
                                "type": "text",
                                "text": "progressTokenPresent=false,input=hello"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponse, response, true);
    }

    /**
     * Verifies that {@code progressTool} can be listed via {@code tools/list},
     * confirming that the tool with a {@link org.mcpjava.server.progress.Progress}
     * parameter is registered correctly without deployment errors.
     */
    @Test
    public void testProgressToolIsListed() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "2",
                            "method": "tools/list",
                            "params": {}
                          }
                          """;

        String response = client.callMCP(request);

        // Verify the tool was registered (name is present in the tools list)
        String expectedFragment = """
                        {
                          "result": {
                            "tools": [
                              {
                                "name": "progressTool"
                              }
                            ]
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedFragment, response, false);
    }
}
