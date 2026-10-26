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
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_MCP_PROTOCOL_VERSION;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import io.openliberty.mcp.internal.fat.tool.dynamicOnlyToolManagerApp.DynamicOnlyStartupBean;
import io.openliberty.mcp.internal.fat.tool.noMcpApp.NonMcpServlet;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * <p>
 * Verifies that within a single EAR containing two WARs:
 * <ul>
 * <li>The WAR that registers tools only via {@code ToolManager} at startup
 * gets the MCP servlet initialized ({@code /mcp} returns responses)</li>
 * <li>The CDI-enabled WAR with no MCP tools and no {@code ToolManager}
 * injection does NOT get the MCP servlet ({@code /mcp} returns 404)</li>
 * </ul>
 */
@RunWith(FATRunner.class)
public class MixedEarModuleTest extends FATServletClient {

    private static final String EAR_NAME = "MixedEarTest";
    private static final String MCP_MODULE = "mcpModule";
    private static final String NON_MCP_MODULE = "nonMcpModule";
    private static final String FILE_NOT_FOUND_ERROR_MESSAGE = "SRVE0190E: File not found: /mcp";

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient mcpClient = McpClient.builder(server, "/" + MCP_MODULE).build();

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive mcpWar = ShrinkWrap.create(WebArchive.class, MCP_MODULE + ".war")
                                      .addPackage(DynamicOnlyStartupBean.class.getPackage());

        WebArchive nonMcpWar = ShrinkWrap.create(WebArchive.class, NON_MCP_MODULE + ".war")
                                         .addPackage(NonMcpServlet.class.getPackage());

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME + ".ear")
                                          .addAsModule(mcpWar)
                                          .addAsModule(nonMcpWar);

        ShrinkHelper.exportDropinAppToServer(server, ear, SERVER_ONLY);

        server.startServer();

        // Confirm MCP endpoint started for the MCP WAR inside the EAR
        assertNotNull("MCP endpoint must be logged for the MCP module in the EAR",
                      server.waitForStringInLog("MCP server endpoint: .*/" + MCP_MODULE + "/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(FILE_NOT_FOUND_ERROR_MESSAGE);
    }

    /**
     * Verifies the MCP WAR inside the EAR correctly initializes its servlet
     * and the dynamic-only tool is callable.
     */
    @Test
    public void testMcpModuleInEarHasEndpoint() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "dynamic-only-tool",
                            "arguments": {}
                          }
                        }
                        """;
        String expected = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "result": {
                            "content": [
                              {
                                "type": "text",
                                "text": "dynamic-only-tool response"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        org.skyscreamer.jsonassert.JSONAssert.assertEquals(expected, mcpClient.callMCP(request),
                                                           org.skyscreamer.jsonassert.JSONCompareMode.STRICT);
    }

    /**
     * Verifies the non-MCP CDI-enabled WAR inside the same EAR does NOT get
     * an MCP servlet — its {@code /mcp} endpoint must return 404.
     */
    @Test
    public void testNonMcpModuleInEarHasNoEndpoint() throws Exception {
        new HttpRequest(server, "/" + NON_MCP_MODULE + "/mcp")
                        .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                        .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                        .method("POST")
                        .expectCode(404)
                        .run(String.class);
        assertNotNull(server.waitForStringInLogUsingMark(FILE_NOT_FOUND_ERROR_MESSAGE, server.getDefaultLogFile()));
    }

    /**
     * Verifies that the MCP endpoint log message was never emitted for the non-MCP module.
     */
    @Test
    public void testNonMcpModuleNotLoggedAsEndpoint() throws Exception {
        assertNull("MCP endpoint must NOT be logged for the non-MCP module in the EAR",
                   server.verifyStringNotInLogUsingMark("MCP server endpoint: .*/" + NON_MCP_MODULE + "/mcp", 3000));
    }
}
