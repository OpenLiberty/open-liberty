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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.tool.dynamicOnlyToolApp.DynamicOnlyStartupBean;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Verifies that a web module whose tools are registered exclusively via the
 * programmatic {@link io.openliberty.mcp.tools.ToolManager} API (with no
 * {@code @Tool}-annotated methods) still gets an MCP servlet endpoint.
 *
 * <p>The fix introduced a deferred {@code @Startup} observer with high priority
 * in {@code McpCdiExtension} that re-checks the tool registry after all user
 * {@code Startup} observers have run. This test exercises that path.
 *
 * @see <a href="https://github.com/OpenLiberty/open-liberty/issues/36103">Issue 36103</a>
 */
@RunWith(FATRunner.class)
public class DynamicOnlyToolManagerTest {

    private static final String APP_NAME = "dynamicOnlyToolApp";

    // Server is managed by McpMainServerSuite — do NOT add @Server here.

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(DynamicOnlyStartupBean.class.getPackage());

        serverLifecycle.deployDropinApp(war);

        // The MCP endpoint should be registered even though no @Tool methods exist.
        assertNotNull("MCP endpoint URL was not logged — dynamic-only tool app did not initialize the MCP servlet",
                      serverLifecycle.getServer().waitForStringInLogUsingMark("CWMCM0008I.*/" + APP_NAME + "/mcp"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    /**
     * Calls the tool registered dynamically via {@code ToolManager} during Startup
     * and verifies a well-formed response is returned, proving the MCP endpoint is live.
     */
    @Test
    public void testDynamicOnlyToolIsCallable() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/call",
                          "params": {
                            "name": "dynamic-echo",
                            "arguments": { "input": "hello" }
                          }
                        }
                        """;

        String response = client.callMCP(request);

        JSONAssert.assertEquals("""
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "result": {
                            "content": [{ "type": "text", "text": "hello" }],
                            "isError": false
                          }
                        }
                        """, response, JSONCompareMode.STRICT);
    }

    /**
     * Calls {@code tools/list} and verifies the dynamically registered tool appears.
     */
    @Test
    public void testDynamicToolAppearsInList() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/list"
                        }
                        """;

        String response = client.callMCP(request);

        JSONAssert.assertEquals("""
                        {
                          "result": {
                            "tools": [
                              { "name": "dynamic-echo" }
                            ]
                          }
                        }
                        """, response, JSONCompareMode.LENIENT);
    }
}
