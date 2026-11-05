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
import static io.openliberty.mcp.internal.fat.utils.JsonRpcErrorCode.INVALID_PARAMS;
import static io.openliberty.mcp.internal.fat.utils.JsonRpcResponseMatcher.jsonRpcResponse;
import static io.openliberty.mcp.internal.fat.utils.ToolResponseMatcher.toolResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
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

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String EAR_NAME = "multi-module";

    @Rule
    public McpClient encoderModuleMCPClient = McpClient.builder(serverLifecycle.getServer(), "/encoderModule").build();

    @Rule
    public McpClient defaultEncoderModuleMCPClient = McpClient.builder(serverLifecycle.getServer(), "/defaultEncoderModule").build();

    @Rule
    public McpClient customConverterModuleClient = McpClient.builder(serverLifecycle.getServer(), "/customConverterModule").build();

    @Rule
    public McpClient sharedConvertersModuleClient = McpClient.builder(serverLifecycle.getServer(), "/sharedConvertersModule").build();

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();

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

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME + ".ear")
                                          .addAsModule(encoderModule)
                                          .addAsModule(defaultEncoderModule)
                                          .addAsModule(customConverterModule)
                                          .addAsModule(sharedConvertersModule)
                                          .addAsModule(ejbJavaArchive) // Cannot share classes with @Tool annotations across WARs (they need module context)
                                          .addAsLibrary(sharedEncodersLib) // Shared encoders are accessible to all WARs
                                          .addAsLibrary(sharedConvertersLib); // Shared converters are accessible to all WARs

        serverLifecycle.deployDropinApp(ear);
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(EAR_NAME + ".ear");
    }

    // High-level assertion helpers that combine call + assertion
    private static void assertToolReturnsText(McpClient client, String toolName, String expectedText) throws Exception {
        assertThat(client.callTool(toolName), toolResponse().withTextContent(expectedText).withNoStructuredContent());
    }

    private static void assertToolNotFound(McpClient client, String toolName) throws Exception {
        assertThat(client.call(client.toolCallRequest(toolName)),
                   jsonRpcResponse().withErrorCode(INVALID_PARAMS)
                                    .withErrorMessage("Invalid params")
                                    .withErrorDataItems("Method " + toolName + " not found"));
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
        assertToolReturnsText(encoderModuleMCPClient, "methodTool", "EncoderModule");
        assertToolReturnsText(encoderModuleMCPClient, "apiTool", "EncoderModule");
        assertToolReturnsText(defaultEncoderModuleMCPClient, "methodTool", "DefaultEncoderModule");
        assertToolReturnsText(defaultEncoderModuleMCPClient, "apiTool", "DefaultEncoderModule");
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
        assertToolNotFound(encoderModuleMCPClient, "ejbJarMethodTool");
        assertToolNotFound(encoderModuleMCPClient, "ejbJarApiTool");
    }

    /**
     * Encoders from EAR/lib (SharedEncoders) are accessible to all WAR modules without a custom encoder.
     * Also tests a client that has a custom encoder (PersonContentEncoder in EncoderModule)
     *
     * @throws Exception
     */
    @Test
    public void testSharedEncoderAccessibleToAllModules() throws Exception {
        String expected = "{\"age\":32,\"fistName\":\"Jon\",\"lastName\":\"Encoded by PersonContentEncoder\"}";
        assertToolReturnsText(defaultEncoderModuleMCPClient, "testContentEncoderSharing", expected);
        assertToolReturnsText(encoderModuleMCPClient, "testContentEncoderSharing", expected);
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
        assertToolReturnsText(encoderModuleMCPClient, "testWar1SpecificEncoder",
                              "{\"employees\":350000,\"industry\":\"Technology\",\"name\":\"IBM (encoded by War1)\"}");
        assertToolReturnsText(defaultEncoderModuleMCPClient, "testWar1SpecificEncoderIsolation",
                              "{\"employees\":350000,\"industry\":\"Technology\",\"name\":\"IBM\"}");
    }

    /**
     * Tests that HTTP session IDs are not shared across all WAR modules in the same EAR application.
     *
     */
    @Test
    public void testSharingOfSessionIDs() throws Exception {
        assertToolReturnsText(encoderModuleMCPClient, "methodTool", "EncoderModule");

        boolean exceptionThrown = false;
        try {
            defaultEncoderModuleMCPClient.callMCPRaw(defaultEncoderModuleMCPClient.toolCallRequest("methodTool").toString())
                                         .sessionId(encoderModuleMCPClient.getSessionId()).run();
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
        assertToolReturnsText(customConverterModuleClient, "testBuiltInStringConverter", "Jupiter");
        assertToolReturnsText(customConverterModuleClient, "testBuiltInIntConverter", "2025");
        assertToolReturnsText(sharedConvertersModuleClient, "testBuiltInStringConverter", "Mars");
        assertToolReturnsText(sharedConvertersModuleClient, "testBuiltInIntConverter", "2026");
    }

    /**
     * Tests that shared converters from EAR/lib are accessible to all WAR modules.
     */
    @Test
    public void testSharedConverterAccessibleToAllModules() throws Exception {
        assertToolReturnsText(customConverterModuleClient, "testPersonConverter", "{\"age\":30,\"name\":\"John\"}");
        assertToolReturnsText(sharedConvertersModuleClient, "testPersonConverter", "{\"age\":25,\"name\":\"Jane\"}");
    }

    /**
     * Tests that module-specific converters override shared converters from EAR/lib.
     */
    @Test
    public void testModuleConverterOverridesSharedConverter() throws Exception {
        assertToolReturnsText(customConverterModuleClient, "testCityConverter",
                              "{\"country\":\"UK\",\"name\":\"Module1-London\",\"population\":9000000}");
        assertToolReturnsText(sharedConvertersModuleClient, "testCityConverter",
                              "{\"country\":\"France\",\"name\":\"Paris\",\"population\":2000000}");
    }
}