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
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.mcp.internal.fat.tool.enableEndpointAnnotatedApp.AnnotatedToolsBean;
import io.openliberty.mcp.internal.fat.tool.enableEndpointApp.DynamicOnlyToolsBean;
import io.openliberty.mcp.internal.fat.tool.enableEndpointNoToolsApp.NoToolsBean;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Tests the {@code enableEndpoint} attribute of {@code <mcp>} ({@code true}, {@code auto} and {@code false})
 * using several web modules deployed in a single EAR, each with its own {@code <mcp moduleName="...">} config.
 *
 * The MCP servlet must only be initialized in modules which have tools (or which explicitly set
 * {@code enableEndpoint="true"}).
 */
@RunWith(FATRunner.class)
public class EnableEndpointConfigTest {

    private static final String EAR_NAME = "enableEndpointTest";

    private static final String DYNAMIC_TRUE = "dynamicTrue";
    private static final String DYNAMIC_AUTO = "dynamicAuto";
    private static final String ANNOTATED_AUTO = "annotatedAuto";
    private static final String ANNOTATED_DEFAULT = "annotatedDefault";
    private static final String ANNOTATED_FALSE = "annotatedFalse";
    private static final String NO_TOOLS = "noTools";

    /** Logged by the web container when a request is made to a path with no registered servlet */
    private static final String FILE_NOT_FOUND = "SRVE0190E";

    @Server("mcp-server-enable-endpoint")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive dynamicTrue = dynamicWar(DYNAMIC_TRUE);
        WebArchive dynamicAuto = dynamicWar(DYNAMIC_AUTO);
        WebArchive annotatedAuto = annotatedWar(ANNOTATED_AUTO);
        WebArchive annotatedDefault = annotatedWar(ANNOTATED_DEFAULT);
        WebArchive annotatedFalse = annotatedWar(ANNOTATED_FALSE);
        WebArchive noTools = ShrinkWrap.create(WebArchive.class, NO_TOOLS + ".war")
                                       .addPackage(NoToolsBean.class.getPackage());

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME + ".ear")
                                          .addAsModule(dynamicTrue)
                                          .addAsModule(dynamicAuto)
                                          .addAsModule(annotatedAuto)
                                          .addAsModule(annotatedDefault)
                                          .addAsModule(annotatedFalse)
                                          .addAsModule(noTools);

        ShrinkHelper.exportAppToServer(server, ear, SERVER_ONLY);
        server.startServer();
        assertNotNull("Web application was not available",
                      server.waitForStringInLogUsingMark("CWWKT0016I:.*" + ANNOTATED_AUTO));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(FILE_NOT_FOUND);
    }

    private static WebArchive dynamicWar(String name) {
        return ShrinkWrap.create(WebArchive.class, name + ".war")
                         .addPackage(DynamicOnlyToolsBean.class.getPackage());
    }

    private static WebArchive annotatedWar(String name) {
        return ShrinkWrap.create(WebArchive.class, name + ".war")
                         .addPackage(AnnotatedToolsBean.class.getPackage());
    }

    private static McpClient client(String contextRoot) {
        return McpClient.builder(server, "/" + contextRoot).build();
    }

    private static final String INITIALIZE_REQUEST = """
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

    /** Asserts the MCP servlet was not initialized in the module, so the MCP path returns 404. */
    private static void assertNoEndpoint(String contextRoot) throws Exception {
        try (McpClient c = client(contextRoot)) {
            c.callMCPRaw(INITIALIZE_REQUEST).expectedStatus(404).run();
        }
    }

    private static void assertToolCall(String contextRoot, String toolName, String arguments, String expectedText) throws Exception {
        String request = String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/call",
                          "params": {
                            "name": "%s",
                            "arguments": %s
                          }
                        }
                        """, toolName, arguments);
        String expected = String.format("""
                        {"id":"1","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"%s"}],"isError":false}}
                        """, expectedText);
        try (McpClient c = client(contextRoot)) {
            c.initialize();
            JSONAssert.assertEquals(expected, c.callMCP(request), false);
        }
    }

    // ---- enableEndpoint="true" ------------------------------------------------------------

    /**
     * With {@code enableEndpoint="true"} the endpoint must be reachable and serve a tool registered
     * only via {@code ToolManager} (no {@code @Tool}-annotated method exists in the module).
     */
    @Test
    public void testTrueWithDynamicOnlyToolsCallsTool() throws Exception {
        assertToolCall(DYNAMIC_TRUE, "dynamic-only-tool", "{}", "dynamic-ok");
    }

    @Test
    public void testTrueWithDynamicOnlyToolsListsTool() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/list",
                          "params": {}
                        }
                        """;
        try (McpClient c = client(DYNAMIC_TRUE)) {
            c.initialize();
            String response = c.callMCP(request);
            assertTrue("Expected 'dynamic-only-tool' in response but got: " + response, response.contains("dynamic-only-tool"));
        }
    }

    // ---- enableEndpoint="auto" (explicit and default) -------------------------------------

    /** With the default (auto) a module which only has dynamic tools does not get an endpoint. */
    @Test
    public void testAutoWithDynamicOnlyToolsHasNoEndpoint() throws Exception {
        assertNoEndpoint(DYNAMIC_AUTO);
    }

    @Test
    public void testExplicitAutoWithAnnotatedToolsHasEndpoint() throws Exception {
        assertToolCall(ANNOTATED_AUTO, "annotated-echo", "{\"input\":\"hello\"}", "hello");
    }

    @Test
    public void testDefaultWithAnnotatedToolsHasEndpoint() throws Exception {
        assertToolCall(ANNOTATED_DEFAULT, "annotated-echo", "{\"input\":\"hello\"}", "hello");
    }

    /** A module with no tools at all must not have its servlet initialized. */
    @Test
    public void testAutoWithNoToolsHasNoEndpoint() throws Exception {
        assertNoEndpoint(NO_TOOLS);
    }

    // ---- enableEndpoint="false" -----------------------------------------------------------

    /** {@code false} disables the endpoint even though the module has {@code @Tool}-annotated methods. */
    @Test
    public void testFalseWithAnnotatedToolsHasNoEndpoint() throws Exception {
        assertNoEndpoint(ANNOTATED_FALSE);
    }

    // ---- multiple modules in one EAR ------------------------------------------------------

    /**
     * Each module in the EAR is configured independently: modules which are enabled keep working
     * while the disabled / tool-less ones stay unavailable.
     */
    @Test
    public void testModulesInEarAreIndependent() throws Exception {
        assertToolCall(DYNAMIC_TRUE, "dynamic-only-tool", "{}", "dynamic-ok");
        assertToolCall(ANNOTATED_AUTO, "annotated-echo", "{\"input\":\"a\"}", "a");
        assertToolCall(ANNOTATED_DEFAULT, "annotated-echo", "{\"input\":\"b\"}", "b");
        assertNoEndpoint(DYNAMIC_AUTO);
        assertNoEndpoint(ANNOTATED_FALSE);
        assertNoEndpoint(NO_TOOLS);
    }
}
