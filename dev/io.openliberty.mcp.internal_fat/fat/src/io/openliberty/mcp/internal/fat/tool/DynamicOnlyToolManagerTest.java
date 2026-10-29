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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import com.ibm.websphere.simplicity.ShrinkHelper;
import io.openliberty.mcp.internal.fat.tool.dynamicToolApp.CdiSelectStartupBean;
import io.openliberty.mcp.internal.fat.tool.dynamicToolApp.InjectStartupBean;
import io.openliberty.mcp.internal.fat.tool.dynamicToolApp.InstanceStartupBean;
import io.openliberty.mcp.internal.fat.tool.dynamicToolApp.ObservesInitializedBean;
import io.openliberty.mcp.internal.fat.tool.dynamicToolApp.ObservesParamStartupBean;
import io.openliberty.mcp.internal.fat.tool.dynamicToolApp.PlainCdiBean;
import io.openliberty.mcp.internal.fat.tool.dynamicToolApp.PlainServlet;
import io.openliberty.mcp.internal.fat.tool.dynamicToolApp.ProducerInitializedBean;
import io.openliberty.mcp.internal.fat.tool.dynamicToolApp.ProducerStartupBean;
import io.openliberty.mcp.internal.fat.tool.dynamicToolApp.ReadToolManagerBean;
import io.openliberty.mcp.internal.fat.tool.dynamicToolApp.UnrelatedStartupBean;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Tests MCP servlet initialization when tools are registered exclusively via ToolManager
 * (no @Tool annotated methods in the application).
 */
@RunWith(FATRunner.class)
public class DynamicOnlyToolManagerTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    private static final List<String> EXPECTED_MCP_APPS = List.of(
        "injectStartupApp",
        "producerStartupApp",
        "observesParamStartupApp",
        "instanceStartupApp",
        "observesInitializedApp",
        "producerInitializedApp",
        "readToolManagerApp"
    );

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive injectStartupWar = ShrinkWrap.create(WebArchive.class, "injectStartupApp.war")
                                                .addClass(InjectStartupBean.class);
        WebArchive producerStartupWar = ShrinkWrap.create(WebArchive.class, "producerStartupApp.war")
                                                  .addClass(ProducerStartupBean.class);
        WebArchive observesParamStartupWar = ShrinkWrap.create(WebArchive.class, "observesParamStartupApp.war")
                                                        .addClass(ObservesParamStartupBean.class);
        WebArchive instanceStartupWar = ShrinkWrap.create(WebArchive.class, "instanceStartupApp.war")
                                                  .addClass(InstanceStartupBean.class);
        WebArchive cdiSelectStartupWar = ShrinkWrap.create(WebArchive.class, "cdiSelectStartupApp.war")
                                                   .addClass(CdiSelectStartupBean.class);
        WebArchive observesInitializedWar = ShrinkWrap.create(WebArchive.class, "observesInitializedApp.war")
                                                      .addClass(ObservesInitializedBean.class);
        WebArchive producerInitializedWar = ShrinkWrap.create(WebArchive.class, "producerInitializedApp.war")
                                                      .addClass(ProducerInitializedBean.class);
        WebArchive plainCdiWar = ShrinkWrap.create(WebArchive.class, "plainCdiApp.war")
                                           .addClasses(PlainCdiBean.class, PlainServlet.class);
        WebArchive unrelatedStartupWar = ShrinkWrap.create(WebArchive.class, "unrelatedStartupApp.war")
                                                    .addClass(UnrelatedStartupBean.class);
        WebArchive readToolManagerWar = ShrinkWrap.create(WebArchive.class, "readToolManagerApp.war")
                                                  .addClass(ReadToolManagerBean.class);

        ShrinkHelper.exportDropinAppToServer(server, injectStartupWar, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, producerStartupWar, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, observesParamStartupWar, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, instanceStartupWar, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, cdiSelectStartupWar, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, observesInitializedWar, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, producerInitializedWar, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, plainCdiWar, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, unrelatedStartupWar, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, readToolManagerWar, SERVER_ONLY);

        server.startServer();

        List<String> missingEndpoints = new ArrayList<>();
        for (String app : EXPECTED_MCP_APPS) {
            String log = server.waitForStringInLogUsingMark("MCP server endpoint: .*" + app + "/mcp$");
            if (log == null) {
                missingEndpoints.add(app);
            }
        }
        assertTrue("Expected MCP server endpoints were not logged for: " + missingEndpoints, missingEndpoints.isEmpty());
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("SRVE0190E");
    }

    private void assertToolRegisteredAndCallable(String appName, String toolName) throws Exception {
        try (McpClient client = McpClient.builder(server, "/" + appName).build()) {
            client.initialize();
            String toolsList = client.listAllTools();
            assertTrue("tools/list in " + appName + " should contain " + toolName + " but was: " + toolsList,
                       toolsList.contains(toolName));

            String toolCall = String.format("""
                            {
                              "jsonrpc": "2.0",
                              "id": "call-1",
                              "method": "tools/call",
                              "params": {
                                "name": "%s"
                              }
                            }
                            """, toolName);
            String response = client.callMCP(toolCall);
            String expectedText = toolName + " response";
            assertTrue("tools/call in " + appName + " should contain expected text '" + expectedText + "' but was: " + response,
                       response.contains(expectedText));
        }
    }

    private void assertNoMcpEndpoint(String appName) throws Exception {
        try (McpClient client = McpClient.builder(server, "/" + appName).build()) {
            String initRequest = """
                            {
                              "jsonrpc": "2.0",
                              "id": "1",
                              "method": "initialize",
                              "params": {
                                "protocolVersion": "2025-11-25",
                                "capabilities": {},
                                "clientInfo": {
                                  "name": "fat-test-client",
                                  "version": "1.0.0"
                                }
                              }
                            }
                            """;
            client.callMCPRaw(initRequest).expectedStatus(404).run();
        }
    }

    @Test
    public void testInjectStartupApp() throws Exception {
        assertToolRegisteredAndCallable("injectStartupApp", "injectStartupTool");
    }

    @Test
    public void testProducerStartupApp() throws Exception {
        assertToolRegisteredAndCallable("producerStartupApp", "producerStartupTool");
    }

    @Test
    public void testObservesParamStartupApp() throws Exception {
        assertToolRegisteredAndCallable("observesParamStartupApp", "observesParamStartupTool");
    }

    @Test
    public void testInstanceStartupApp() throws Exception {
        assertToolRegisteredAndCallable("instanceStartupApp", "instanceStartupTool");
    }

    @Test
    public void testCdiSelectStartupApp() throws Exception {
        assertNoMcpEndpoint("cdiSelectStartupApp");
    }

    @Test
    public void testObservesInitializedApp() throws Exception {
        assertToolRegisteredAndCallable("observesInitializedApp", "observesInitializedTool");
    }

    @Test
    public void testProducerInitializedApp() throws Exception {
        assertToolRegisteredAndCallable("producerInitializedApp", "producerInitializedTool");
    }

    @Test
    public void testPlainCdiApp() throws Exception {
        assertNoMcpEndpoint("plainCdiApp");
    }

    @Test
    public void testUnrelatedStartupApp() throws Exception {
        assertNoMcpEndpoint("unrelatedStartupApp");
    }

    @Test
    public void testReadToolManagerApp() throws Exception {
        try (McpClient client = McpClient.builder(server, "/readToolManagerApp").build()) {
            client.initialize();
            String toolsList = client.listAllTools();
            assertTrue("tools/list in readToolManagerApp should be empty but was: " + toolsList,
                       toolsList.contains("\"tools\":[]") || toolsList.contains("[]") || !toolsList.contains("nonExistentTool"));
        }
    }
}
