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
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.ejbjar.EarToolBean;
import io.openliberty.mcp.internal.fat.tool.sharedEncoders.SharedEncoders;
import io.openliberty.mcp.internal.fat.tool.war1init.War1ToolBeanInitializedEvent;
import io.openliberty.mcp.internal.fat.tool.war1startup.War1ToolBeanStartupEvent;
import io.openliberty.mcp.internal.fat.tool.war2init.War2ToolBeanInitializedEvent;
import io.openliberty.mcp.internal.fat.tool.war2startup.War2ToolBeanStartupEvent;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Tests module-scoped tool and encoder isolation in Jakarta EE applications with MCP (Model Context Protocol).
 *
 * <p><b>Purpose:</b> Validates that tools and encoders registered in one web module are properly isolated
 * from other web modules, while shared encoders in EAR/lib are accessible to all modules. Ensures no
 * cross-contamination of tool and encoder registries across module boundaries.
 *
 * <p><b>Test Architecture:</b>
 * <ul>
 * <li>Deploys an EAR containing 4 WAR modules, 1 EJB JAR module, and 1 shared library</li>
 * <li>Each WAR registers tools using different mechanisms (CDI events)</li>
 * <li>Tests verify tools and encoders are scoped per module, not application-wide</li>
 * <li>Shared encoders in EAR/lib are accessible to all WAR modules</li>
 * </ul>
 *
 * <p><b>Module Structure:</b>
 *
 * <pre>
 * multi-module.ear
 * ├── war1WithInitializedEvent.war  - Tools via @Initialized(ApplicationScoped.class)
 * │                                    + Custom PersonContentEncoder (overrides shared)
 * │                                    + Custom CompanyContentEncoder (module-specific)
 * ├── war1WithBeanStartupEvent.war  - Tools via @Startup bean
 * ├── war2WithInitializedEvent.war  - Tools via @Initialized(ApplicationScoped.class)
 * ├── war2WithBeanStartupEvent.war  - Tools via @Startup bean
 * ├── mcpToolsEjb.jar               - EJB-based tools (NOT exposed to WARs)
 * │                                   + EJB encoders (NOT accessible to WARs)
 * └── lib/
 *     └── shared-encoders.jar       - Shared PersonContentEncoder (GLOBAL scope)
 *                                      Accessible to all WAR modules
 * </pre>
 *
 * <p><b>Test Coverage:</b>
 * <ul>
 * <li><b>Tool Isolation:</b>
 * <ul>
 * <li>Tools registered via @Initialized(ApplicationScoped.class) event</li>
 * <li>Tools registered via @Startup bean event</li>
 * <li>Both annotation-based (@Tool) and programmatic (ToolManager API) tool registration</li>
 * <li>EJB JAR tool exclusion from web module endpoints</li>
 * </ul>
 * </li>
 * <li><b>Encoder Isolation and Sharing:</b>
 * <ul>
 * <li>Shared encoders (EAR/lib) accessible to all WAR modules (GLOBAL scope)</li>
 * <li>Module-specific encoders isolated per WAR (MODULE scope)</li>
 * <li>Module encoders override shared encoders when present</li>
 * <li>EJB JAR encoders NOT accessible to WAR modules (class visibility constraint)</li>
 * </ul>
 * </li>
 * <li><b>Session Sharing:</b>
 * <ul>
 * <li>Each MCPSessionStore is ModuleScoped</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <p><b>Encoder Scope Behavior:</b>
 * <ul>
 * <li><b>GLOBAL (EAR/lib):</b> Encoders in shared-encoders.jar are registered to the global
 * EncoderRegistry and accessible to all WAR modules that don't have their own encoder
 * for the same type</li>
 * <li><b>MODULE (WAR):</b> Encoders in WAR modules are registered to that module's
 * EncoderRegistry and take precedence over global encoders</li>
 * <li><b>EJB JAR:</b> Encoders in EJB JAR are NOT accessible to WAR modules due to
 * Jakarta EE class visibility rules (EJB classes not visible to WARs)</li>
 * </ul>
 *
 * <p><b>Related Requirements:</b>
 * <ul>
 * <li>One ToolRegistry per web module (not per application)</li>
 * <li>One EncoderRegistry per web module + one global EncoderRegistry for EAR/lib</li>
 * <li>Module-scoped CDI beans for programmatic tool registration</li>
 * <li>EncoderRegistry and ConverterRegistry module isolation with global fallback</li>
 * </ul>
 *
 * @see io.openliberty.mcp.ToolManager
 * @see io.openliberty.mcp.Tool
 */
@RunWith(FATRunner.class)
public class MultiModuleToolTestToolManager extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient war1WithInitializedEvent = new McpClient(server, "/war1WithInitializedEvent");

    @Rule
    public McpClient war1WithBeanStartupEvent = new McpClient(server, "/war1WithBeanStartupEvent");

    @Rule
    public McpClient war2WithInitializedEvent = new McpClient(server, "/war2WithInitializedEvent");

    @Rule
    public McpClient war2WithBeanStartupEvent = new McpClient(server, "/war2WithBeanStartupEvent");

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive war1WithInitializedEvent = ShrinkWrap.create(WebArchive.class, "war1WithInitializedEvent.war")
                                                        .addClass(War1ToolBeanInitializedEvent.class);
        WebArchive war1WithBeanStartupEvent = ShrinkWrap.create(WebArchive.class, "war1WithBeanStartupEvent.war")
                                                        .addClass(War1ToolBeanStartupEvent.class);
        WebArchive war2WithInitializedEvent = ShrinkWrap.create(WebArchive.class, "war2WithInitializedEvent.war")
                                                        .addClass(War2ToolBeanInitializedEvent.class);
        WebArchive war2WithBeanStartupEvent = ShrinkWrap.create(WebArchive.class, "war2WithBeanStartupEvent.war")
                                                        .addClass(War2ToolBeanStartupEvent.class);

        // 1) EarToolBean cannot share its tools across WARs (they need module context)
        // 2) EarToolBean also has a Person Encoder with a high priority (5000), but it correctly is never used.
        // This is by design as EJB JAR classes are NOT visible to WAR modules
        JavaArchive ejbJavaArchive = ShrinkWrap.create(JavaArchive.class, "mcpToolsEjb.jar")
                                               .addClass(EarToolBean.class);

        JavaArchive sharedEncodersLib = ShrinkWrap.create(JavaArchive.class, "shared-encoders.jar")
                                                  .addClass(SharedEncoders.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "multi-module.ear")
                                          .addAsModule(war1WithInitializedEvent)
                                          .addAsModule(war1WithBeanStartupEvent)
                                          .addAsModule(war2WithInitializedEvent)
                                          .addAsModule(war2WithBeanStartupEvent)
                                          .addAsModule(ejbJavaArchive) // Cannot share classes with @Tool annotations across WARs (they need module context)
                                          .addAsLibrary(sharedEncodersLib); // Shared encoders are accessible to all WARs

        ShrinkHelper.exportDropinAppToServer(server, ear, SERVER_ONLY);

        server.startServer();
    }

    /**
     * Tests that tools are properly isolated per web module.
     *
     * <p>Validates that:
     * <ul>
     * <li>Each WAR module maintains its own tool registry</li>
     * <li>Tools with the same name in different modules return module-specific responses</li>
     * <li>Both annotation-based (@Tool) and API-based (ToolManager) tools work correctly</li>
     * <li>Tools registered via @Initialized and @Startup events are both accessible</li>
     * </ul>
     *
     * <p>Each module registers two tools:
     * <ul>
     * <li><b>methodTool</b> - Registered via @Tool annotation on a method</li>
     * <li><b>apiTool</b> - Registered programmatically via ToolManager API</li>
     * </ul>
     *
     * <p>Both tools return the module's class name as response, allowing verification
     * that the correct module's tool was invoked.
     *
     * @throws Exception if MCP communication fails
     */
    @Test
    public void testAllWarTools() throws Exception {
        testTools("methodTool", "apiTool", "War1ToolBeanInitializedEvent", war1WithInitializedEvent);
        testTools("methodTool", "apiTool", "War1ToolBeanStartupEvent", war1WithBeanStartupEvent);
        testTools("methodTool", "apiTool", "War2ToolBeanInitializedEvent", war2WithInitializedEvent);
        testTools("methodTool", "apiTool", "War2ToolBeanStartupEvent", war2WithBeanStartupEvent);
    }

    /**
     * Helper method to test tool registration and invocation for a specific WAR module.
     *
     * <p>Tests both annotation-based (@Tool) and programmatic (ToolManager API) tool registration
     * by invoking two tools and verifying their responses match expected values.
     *
     * @param methodToolName Name of the annotation-based tool (registered via @Tool annotation)
     * @param apiToolName Name of the programmatic tool (registered via ToolManager.newTool() API)
     * @param toolResponse Expected response text from both tools
     * @param client McpClient configured for the specific WAR module endpoint
     * @throws Exception if MCP communication fails or assertions fail
     */
    private void testTools(String methodToolName, String apiToolName, String toolResponse, McpClient client) throws Exception {
        // Test annotation-based tool (@Tool on method)
        String response1 = client.callMCP(String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "%s"
                          }
                        }
                        """, methodToolName));

        String expectedResponse1 = String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "result": {
                            "content": [
                              {
                                "type": "text",
                                "text": "%s"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """, toolResponse);

        JSONAssert.assertEquals(expectedResponse1, response1, JSONCompareMode.STRICT);

        // Test programmatic tool (ToolManager API)
        String response2 = client.callMCP(String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "%s"
                          }
                        }
                        """, apiToolName));

        String expectedResponse2 = String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "result": {
                            "content": [
                              {
                                "type": "text",
                                "text": "%s"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """, toolResponse);

        JSONAssert.assertEquals(expectedResponse2, response2, JSONCompareMode.STRICT);
    }

    /**
     * Tests that tools registered in EJB JAR modules are NOT accessible via WAR module endpoints.
     *
     * <p><b>Test Scenario:</b>
     * <ul>
     * <li>EJB JAR module (mcpToolsEjb.jar) contains tools registered via @Tool and ToolManager API</li>
     * <li>WAR module (war1WithInitializedEvent) attempts to invoke these EJB tools</li>
     * </ul>
     *
     * <p><b>Expected Behavior:</b>
     * <ul>
     * <li>Both ejbJarMethodTool and ejbJarApiTool return "Method not found" errors</li>
     * <li>EJB tools are isolated and not exposed through WAR endpoints</li>
     * </ul>
     *
     * <p><b>Rationale:</b> MCP tools should only be accessible within their defining module.
     * EJB JAR tools should not leak into WAR module tool registries, maintaining proper
     * module isolation and security boundaries.
     *
     * @throws Exception if MCP communication fails or assertions fail
     */
    @Test
    public void testEJBJar() throws Exception {
        String response1 = war1WithInitializedEvent.callMCP("""
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "ejbJarMethodTool"
                          }
                        }
                        """);

        String expectedResponseString1 = """
                        {"id":1,"jsonrpc":"2.0","error":{"code":-32602,"data":["Method ejbJarMethodTool not found"],"message":"Invalid params"}}
                                                """;

        JSONAssert.assertEquals(expectedResponseString1, response1, true);

        String response2 = war1WithInitializedEvent.callMCP("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "ejbJarApiTool"
                          }
                        }
                        """);

        String expectedResponseString2 = """
                        {"id":2,"jsonrpc":"2.0","error":{"code":-32602,"data":["Method ejbJarApiTool not found"],"message":"Invalid params"}}
                        """;

        JSONAssert.assertEquals(expectedResponseString2, response2, true);
    }

    /**
     * Encoders from EAR/lib (SharedEncoders) are accessible to all WAR modules without a custom encoder.
     * Also tests a client that has a custom encoder (War1ToolBeanInitializedEvent)
     *
     * @throws Exception
     */
    @Test
    public void testSharedEncoderAccessibleToAllModules() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/call",
                          "params": {
                            "name": "testContentEncoderSharing",
                            "arguments": {}
                          }
                        }
                        """;

        String expectedResponseForSharedEncoder = """
                        {
                            "id": "1",
                            "jsonrpc": "2.0",
                            "result": {
                                "content": [
                                    {
                                        "text": "{\\"age\\":32,\\"fistName\\":\\"Jon\\",\\"lastName\\":\\"Encoded by PersonContentEncoder\\"}",
                                        "type": "text"
                                    }
                                ],
                                "isError": false
                            }
                        }
                        """;

        // 3 modules (wars) should successfully encode Person using the shared encoder
        String response1 = war1WithBeanStartupEvent.callMCP(request);
        JSONAssert.assertEquals(expectedResponseForSharedEncoder, response1, JSONCompareMode.NON_EXTENSIBLE);

        String response2 = war1WithBeanStartupEvent.callMCP(request);
        JSONAssert.assertEquals(expectedResponseForSharedEncoder, response2, JSONCompareMode.NON_EXTENSIBLE);

        String response3 = war2WithInitializedEvent.callMCP(request);
        JSONAssert.assertEquals(expectedResponseForSharedEncoder, response3, JSONCompareMode.NON_EXTENSIBLE);

        //War1ToolBeanInitializedEvent has its own custom PersonContentEncoder
        String response4 = war1WithInitializedEvent.callMCP(request);
        String expectedResponseForCustomEncoder = """
                        {
                            "id": "1",
                            "jsonrpc": "2.0",
                            "result": {
                                "content": [
                                    {
                                        "text": "{\\"age\\":32,\\"fistName\\":\\"Jon\\",\\"lastName\\":\\"Encoded by PersonContentEncoder in War1ToolBeanInitializedEvent\\"}",
                                        "type": "text"
                                    }
                                ],
                                "isError": false
                            }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseForCustomEncoder, response4, JSONCompareMode.NON_EXTENSIBLE);

    }

    /**
     * Tests that module-specific encoders are properly isolated between WAR modules.
     * - war1WithInitializedEvent has CompanyContentEncoder and can encode Company objects
     * - war1WithBeanStartupEvent does NOT have CompanyContentEncoder and should fail to encode Company objects
     *
     * @throws Exception
     */
    /**
     * Tests that module-specific encoders are properly isolated between WAR modules.
     *
     * <p><b>Test Scenario:</b>
     * <ul>
     * <li>war1WithInitializedEvent has CompanyContentEncoder and CAN encode Company objects</li>
     * <li>war1WithBeanStartupEvent does NOT have CompanyContentEncoder and CANNOT encode Company objects</li>
     * </ul>
     *
     * <p><b>Expected Behavior:</b>
     * <ul>
     * <li>war1WithInitializedEvent: Returns Company with custom encoding: "IBM (encoded by War1)"</li>
     * <li>war1WithBeanStartupEvent: Returns Company with default JSON encoding: "IBM"</li>
     * </ul>
     *
     * <p>This validates that encoders registered in one module's EncoderRegistry are NOT
     * accessible to other modules, ensuring proper encoder isolation.
     *
     * @throws Exception if MCP communication fails or assertions fail
     */
    @Test
    public void testModuleSpecificEncoderIsolation() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testWar1SpecificEncoder",
                            "arguments": {}
                          }
                        }
                        """;

        // war1WithInitializedEvent should successfully encode Company
        String response1 = war1WithInitializedEvent.callMCP(request);
        String expected1 = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"text":"{\\"employees\\":350000,\\"industry\\":\\"Technology\\",\\"name\\":\\"IBM (encoded by War1)\\"}","type":"text"}],"isError":false}}
                        """;
        JSONAssert.assertEquals(expected1, response1, JSONCompareMode.NON_EXTENSIBLE);

        // war1WithBeanStartupEvent should fail - no Company encoder available
        String request2 = """
                          {
                          "jsonrpc": "2.0",
                          "id": "3",
                          "method": "tools/call",
                          "params": {
                            "name": "testWar1SpecificEncoderIsolation",
                            "arguments": {}
                          }
                        }
                        """;

        String response2 = war1WithBeanStartupEvent.callMCP(request2);
        // Should use default JSON encoder since CompanyContentEncoder is not available
        String expected2 = """
                        {"id":"3","jsonrpc":"2.0","result":{"content":[{"text":"{\\"employees\\":350000,\\"industry\\":\\"Technology\\",\\"name\\":\\"IBM\\"}","type":"text"}],"isError":false}}
                        """;
        JSONAssert.assertEquals(expected2, response2, JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     * Tests that HTTP session IDs are not shared across all WAR modules in the same EAR application.
     *
     */
    @Test
    public void testSharingOfSessionIDs() throws Exception {
        String jsonRequestBody = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "methodTool"
                          }
                        }
                        """;
        String response1 = war1WithInitializedEvent.callMCP(jsonRequestBody);

        String expectedResponse1 = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "result": {
                            "content": [
                              {
                                "type": "text",
                                "text": "War1ToolBeanInitializedEvent"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponse1, response1, JSONCompareMode.NON_EXTENSIBLE);

        boolean exceptionThrown = false;
        try {
            war1WithBeanStartupEvent.callMCPWithSessionID(jsonRequestBody, war1WithInitializedEvent.getSessionId());
        } catch (Exception e) {
            exceptionThrown = true;
            assertTrue(e.getMessage().contains("Invalid or Expired Session Id"));
        }
        assertTrue("Expected Invalid or Expired Session Id error, but got none", exceptionThrown);

    }
}
