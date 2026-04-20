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
import io.openliberty.mcp.internal.fat.convertertools.customConverterModule.CustomConverterModuleTools;
import io.openliberty.mcp.internal.fat.convertertools.sharedConvertersModule.SharedConvertersModuleTools;
import io.openliberty.mcp.internal.fat.encodertools.defaultEncoderTest1.DefaultEncoderModule;
import io.openliberty.mcp.internal.fat.encodertools.ejbjarEncoder.EarToolBean;
import io.openliberty.mcp.internal.fat.encodertools.moduleLevelEncoder.EncoderModuleTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Tests tool and encoder isolation across WAR modules in an EAR.
 *
 * <p>Verifies:
 * <ul>
 * <li>Tools registered in one WAR are not visible in other WARs
 * <li>Module-specific encoders override shared encoders from EAR/lib
 * <li>EJB JAR tools and encoders are not accessible to WARs
 * </ul>
 */
@RunWith(FATRunner.class)
public class MultiModuleToolTestToolManager extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient encoderModuleMCPClient = new McpClient(server, "/encoderModule");

    @Rule
    public McpClient defaultEncoderModuleMCPClient = new McpClient(server, "/defaultEncoderModule");

    @Rule
    public McpClient customConverterModuleClient = new McpClient(server, "/customConverterModule");

    @Rule
    public McpClient sharedConvertersModuleClient = new McpClient(server, "/sharedConvertersModule");

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive encoderModule = ShrinkWrap.create(WebArchive.class, "encoderModule.war")
                                             .addPackage(EncoderModuleTools.class.getPackage());
        WebArchive defaultEncoderModule = ShrinkWrap.create(WebArchive.class, "defaultEncoderModule.war")
                                                    .addPackage(DefaultEncoderModule.class.getPackage());

        // Converter test modules
        WebArchive customConverterModule = ShrinkWrap.create(WebArchive.class, "customConverterModule.war")
                                                .addPackage(CustomConverterModuleTools.class.getPackage());
        WebArchive sharedConvertersModule = ShrinkWrap.create(WebArchive.class, "sharedConvertersModule.war")
                                                .addPackage(SharedConvertersModuleTools.class.getPackage());

        // 1) EarToolBean cannot share its tools across WARs (they need module context)
        // 2) EarToolBean also has a Person Encoder with a high priority (5000), but it correctly is never used.
        // 3) EarToolBean also has a Company Converter that should NOT be accessible to WAR modules
        // This is by design as EJB JAR classes are NOT visible to WAR modules
        JavaArchive ejbJavaArchive = ShrinkWrap.create(JavaArchive.class, "mcpToolsEjb.jar")
                                               .addClass(EarToolBean.class)
                                               .addPackage("io.openliberty.mcp.internal.fat.convertertools.ejbjarConverter");

        JavaArchive sharedEncodersLib = ShrinkWrap.create(JavaArchive.class, "shared-encoders.jar")
                                                  .addPackage("io.openliberty.mcp.internal.fat.encodertools.sharedEncoders");

        JavaArchive sharedConvertersLib = ShrinkWrap.create(JavaArchive.class, "shared-converters.jar")
                                                    .addPackage("io.openliberty.mcp.internal.fat.convertertools.sharedConverters");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "multi-module.ear")
                                          .addAsModule(encoderModule)
                                          .addAsModule(defaultEncoderModule)
                                          .addAsModule(customConverterModule)
                                          .addAsModule(sharedConvertersModule)
                                          .addAsModule(ejbJavaArchive) // Cannot share classes with @Tool annotations across WARs (they need module context)
                                          .addAsLibrary(sharedEncodersLib) // Shared encoders are accessible to all WARs
                                          .addAsLibrary(sharedConvertersLib); // Shared converters are accessible to all WARs

        ShrinkHelper.exportDropinAppToServer(server, ear, SERVER_ONLY);

        server.startServer();
    }

    private static String toolCallRequest(String id, String toolName) {
        return String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": "%s",
                          "method": "tools/call",
                          "params": {
                            "name": "%s"
                          }
                        }
                        """, id, toolName);
    }

    private static String textResponse(String id, String text) {
        return String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": "%s",
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
                        """, id, text);
    }

    private static String errorResponse(String id, String toolName) {
        return String.format("""
                        {"id":"%s","jsonrpc":"2.0","error":{"code":-32602,"data":["Method %s not found"],"message":"Invalid params"}}
                        """, id, toolName);
    }

    // High-level assertion helpers that combine call + assertion
    private static void assertToolReturnsText(McpClient client, String id, String toolName, String expectedText) throws Exception {
        String response = client.callMCP(toolCallRequest(id, toolName));
        JSONAssert.assertEquals(textResponse(id, expectedText), response, JSONCompareMode.STRICT);
    }

    private static void assertToolNotFound(McpClient client, String id, String toolName) throws Exception {
        String response = client.callMCP(toolCallRequest(id, toolName));
        JSONAssert.assertEquals(errorResponse(id, toolName), response, JSONCompareMode.STRICT);
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
        assertToolReturnsText(encoderModuleMCPClient, "1", "methodTool", "EncoderModule");
        assertToolReturnsText(encoderModuleMCPClient, "2", "apiTool", "EncoderModule");
        assertToolReturnsText(defaultEncoderModuleMCPClient, "3", "methodTool", "DefaultEncoderModule");
        assertToolReturnsText(defaultEncoderModuleMCPClient, "4", "apiTool", "DefaultEncoderModule");
    }

    /**
     * Tests that tools registered in EJB JAR modules are NOT accessible via WAR module endpoints.
     *
     * <p><b>Test Scenario:</b>
     * <ul>
     * <li>EJB JAR module (mcpToolsEjb.jar) contains tools registered via @Tool and ToolManager API</li>
     * <li>WAR module (encoderModule) attempts to invoke these EJB tools</li>
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
        assertToolNotFound(encoderModuleMCPClient, "1", "ejbJarMethodTool");
        assertToolNotFound(encoderModuleMCPClient, "2", "ejbJarApiTool");
    }

    /**
     * Encoders from EAR/lib (SharedEncoders) are accessible to all WAR modules without a custom encoder.
     * Also tests a client that has a custom encoder (PersonContentEncoder in EncoderModule)
     *
     * @throws Exception
     */
    @Test
    public void testSharedEncoderAccessibleToAllModules() throws Exception {
        String expected = "{\\\"age\\\":32,\\\"fistName\\\":\\\"Jon\\\",\\\"lastName\\\":\\\"Encoded by PersonContentEncoder\\\"}";
        assertToolReturnsText(defaultEncoderModuleMCPClient, "1", "testContentEncoderSharing", expected);
        assertToolReturnsText(encoderModuleMCPClient, "1", "testContentEncoderSharing", expected);
    }

    /**
     * Tests that module-specific encoders are properly isolated between WAR modules.
     *
     * <p><b>Test Scenario:</b>
     * <ul>
     * <li>encoderModule has CompanyContentEncoder and CAN encode Company objects</li>
     * <li>war1WithBeanStartupEvent does NOT have CompanyContentEncoder and CANNOT encode Company objects</li>
     * </ul>
     *
     * <p><b>Expected Behavior:</b>
     * <ul>
     * <li>encoderModule: Returns Company with custom encoding: "IBM (encoded by War1)"</li>
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
        assertToolReturnsText(encoderModuleMCPClient, "2", "testWar1SpecificEncoder",
                              "{\\\"employees\\\":350000,\\\"industry\\\":\\\"Technology\\\",\\\"name\\\":\\\"IBM (encoded by War1)\\\"}");
        assertToolReturnsText(defaultEncoderModuleMCPClient, "3", "testWar1SpecificEncoderIsolation",
                              "{\\\"employees\\\":350000,\\\"industry\\\":\\\"Technology\\\",\\\"name\\\":\\\"IBM\\\"}");
    }

    /**
     * Tests that HTTP session IDs are not shared across all WAR modules in the same EAR application.
     *
     */
    @Test
    public void testSharingOfSessionIDs() throws Exception {
        assertToolReturnsText(encoderModuleMCPClient, "1", "methodTool", "EncoderModule");

        boolean exceptionThrown = false;
        try {
            defaultEncoderModuleMCPClient.callMCPWithSessionID(toolCallRequest("1", "methodTool"), encoderModuleMCPClient.getSessionId());
        } catch (Exception e) {
            exceptionThrown = true;
            assertTrue(e.getMessage().contains("Invalid or Expired Session Id"));
        }
        assertTrue("Expected Invalid or Expired Session Id error, but got none", exceptionThrown);
    }

    /**
     * Tests that built-in converters are accessible to all WAR modules.
     */
    @Test
    public void testBuiltInConvertersAccessibleToAllModules() throws Exception {
        assertToolReturnsText(customConverterModuleClient, "1", "testBuiltInStringConverter", "Jupiter");
        assertToolReturnsText(customConverterModuleClient, "2", "testBuiltInIntConverter", "2025");
        assertToolReturnsText(sharedConvertersModuleClient, "3", "testBuiltInStringConverter", "Mars");
        assertToolReturnsText(sharedConvertersModuleClient, "4", "testBuiltInIntConverter", "2026");
    }

    /**
     * Tests that shared converters from EAR/lib are accessible to all WAR modules.
     */
    @Test
    public void testSharedConverterAccessibleToAllModules() throws Exception {
        assertToolReturnsText(customConverterModuleClient, "1", "testPersonConverter", "{\\\"age\\\":30,\\\"name\\\":\\\"John\\\"}");
        assertToolReturnsText(sharedConvertersModuleClient, "2", "testPersonConverter", "{\\\"age\\\":25,\\\"name\\\":\\\"Jane\\\"}");
    }

    /**
     * Tests that module-specific converters override shared converters from EAR/lib.
     */
    @Test
    public void testModuleConverterOverridesSharedConverter() throws Exception {
        assertToolReturnsText(customConverterModuleClient, "1", "testCityConverter",
                              "{\\\"country\\\":\\\"UK\\\",\\\"name\\\":\\\"Module1-London\\\",\\\"population\\\":9000000}");
        assertToolReturnsText(sharedConvertersModuleClient, "2", "testCityConverter",
                              "{\\\"country\\\":\\\"France\\\",\\\"name\\\":\\\"Paris\\\",\\\"population\\\":2000000}");
    }
}
