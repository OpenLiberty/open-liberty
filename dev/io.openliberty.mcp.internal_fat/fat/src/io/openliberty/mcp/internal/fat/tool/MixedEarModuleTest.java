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

import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
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
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.tool.mixedEarApp.noToolModule.NoToolModuleBean;
import io.openliberty.mcp.internal.fat.tool.mixedEarApp.toolModule.ToolModuleBean;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Verifies correct MCP endpoint initialization behaviour for an EAR that contains
 * two web modules: one that registers tools and one that does not.
 *
 * <p>Expected outcome:
 * <ul>
 *   <li><b>toolModule</b> — registers a tool dynamically via {@code ToolManager} during
 *       the CDI {@code Startup} event → should receive an MCP endpoint.</li>
 *   <li><b>noToolModule</b> — has no tools at all → should <em>not</em> receive an
 *       MCP endpoint (no {@code CWMCM0008I} log entry, HTTP 404 on {@code /mcp}).</li>
 * </ul>
 *
 * @see <a href="https://github.com/OpenLiberty/open-liberty/issues/36103">Issue 36103</a>
 */
@RunWith(FATRunner.class)
public class MixedEarModuleTest {

    private static final String EAR_NAME = "mixed-ear-module-test";
    private static final String TOOL_MODULE = "toolModule";
    private static final String NO_TOOL_MODULE = "noToolModule";

    // Server is managed by McpMainServerSuite — do NOT add @Server here.

    @Rule
    public McpClient toolModuleClient = McpClient.builder(serverLifecycle.getServer(), "/" + TOOL_MODULE).build();

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();

        WebArchive toolModuleWar = ShrinkWrap.create(WebArchive.class, TOOL_MODULE + ".war")
                                             .addPackage(ToolModuleBean.class.getPackage());

        WebArchive noToolModuleWar = ShrinkWrap.create(WebArchive.class, NO_TOOL_MODULE + ".war")
                                               .addPackage(NoToolModuleBean.class.getPackage());

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME + ".ear")
                                          .addAsModule(toolModuleWar)
                                          .addAsModule(noToolModuleWar);

        serverLifecycle.deployDropinApp(ear);

        // Only the tool module should log the MCP endpoint URL.
        assertNotNull("MCP endpoint was not registered for the module that has tools",
                      serverLifecycle.getServer().waitForStringInLogUsingMark("CWMCM0008I.*/" + TOOL_MODULE + "/mcp"));

        // The no-tool module must NOT log an endpoint URL — wait briefly and assert nothing appeared.
        assertNull("MCP endpoint must not be registered for a module that has no tools",
                   serverLifecycle.getServer().waitForStringInLogUsingMark("CWMCM0008I.*/" + NO_TOOL_MODULE + "/mcp", 3000));
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(EAR_NAME + ".ear");
    }

    /**
     * Calls the dynamically registered tool in the tool module and verifies the response.
     */
    @Test
    public void testToolModuleToolIsCallable() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/call",
                          "params": { "name": "ear-tool" }
                        }
                        """;

        String response = toolModuleClient.callMCP(request);

        JSONAssert.assertEquals("""
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "result": {
                            "content": [{ "type": "text", "text": "from-tool-module" }],
                            "isError": false
                          }
                        }
                        """, response, JSONCompareMode.STRICT);
    }

    /**
     * Verifies that the no-tool module does not expose an MCP endpoint — a POST
     * to {@code /noToolModule/mcp} must return HTTP 404.
     */
    @Test
    public void testNoToolModuleHasNoMcpEndpoint() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/list"
                        }
                        """;

        // We expect a 404 because no MCP servlet was registered for this module.
        McpClient noToolClient = McpClient.builder(serverLifecycle.getServer(), "/" + NO_TOOL_MODULE).build();
        noToolClient.callMCPRaw(request).expectedStatus(404).run();
    }
}
