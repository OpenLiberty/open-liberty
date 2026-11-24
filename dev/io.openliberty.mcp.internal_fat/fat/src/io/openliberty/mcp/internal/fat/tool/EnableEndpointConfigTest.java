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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.config.Mcp;

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.tool.enableEndpointAnnotatedApp.AnnotatedToolsBean;
import io.openliberty.mcp.internal.fat.tool.enableEndpointApp.DynamicOnlyToolsBean;
import io.openliberty.mcp.internal.fat.tool.enableEndpointNoToolsApp.NoToolsBean;
import io.openliberty.mcp.internal.fat.tool.inactiveCdiApp.InactiveCdiTool;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Tests the {@code enableEndpoint} attribute of {@code <mcp>} ({@code true}, {@code auto} and {@code false})
 * using several web modules deployed in a single EAR, each with its own {@code <mcp moduleName="...">} config.
 *
 * The MCP servlet must only be initialized in modules which have tools (or which explicitly set
 * {@code enableEndpoint="true"}).
 *
 * The server is managed by {@link io.openliberty.mcp.internal.fat.suite.McpMainServerSuite} - do NOT add {@code @Server}.
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
    private static final String DYNAMIC_TRUE_SPACES = "dynamicTrueSpaces";
    private static final String ANNOTATED_FALSE_SPACES = "annotatedFalseSpaces";
    private static final String ANNOTATED_INVALID = "annotatedInvalid";
    /** Standalone (non-EAR) WARs with no CDI beans, so that CDI is inactive for them */
    private static final String NO_CDI_FALSE = "noCdiFalseApp";
    private static final String NO_CDI_AUTO = "noCdiAutoApp";

    /** Trace event logged by {@code McpServletInitializer} when {@code ToolRegistry.get()} fails because CDI is inactive */
    private static final String CDI_INACTIVE = "unavailable due to CDI being inactive";

    /** Logged by the web container when a request is made to a path with no registered servlet */
    private static final String FILE_NOT_FOUND = "SRVE0190E: File not found: /mcp";

    /** Logged when {@code enableEndpoint} is not one of {@code true}, {@code auto} or {@code false} */
    private static final String INVALID_VALUE_WARNING = "CWMCM0047W";

    @BeforeClass
    public static void setUp() throws Exception {
        serverLifecycle.getServer().addIgnoredErrors(List.of(FILE_NOT_FOUND, INVALID_VALUE_WARNING));

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME + ".ear")
                                          .addAsModule(dynamicWar(DYNAMIC_TRUE))
                                          .addAsModule(dynamicWar(DYNAMIC_AUTO))
                                          .addAsModule(annotatedWar(ANNOTATED_AUTO))
                                          .addAsModule(annotatedWar(ANNOTATED_DEFAULT))
                                          .addAsModule(annotatedWar(ANNOTATED_FALSE))
                                          .addAsModule(ShrinkWrap.create(WebArchive.class, NO_TOOLS + ".war")
                                                                 .addPackage(NoToolsBean.class.getPackage()))
                                          .addAsModule(dynamicWar(DYNAMIC_TRUE_SPACES))
                                          .addAsModule(annotatedWar(ANNOTATED_FALSE_SPACES))
                                          .addAsModule(annotatedWar(ANNOTATED_INVALID));

        // dynamicAuto, annotatedDefault and noTools have no config, so they use the default ("auto")
        serverLifecycle.deployWithConfiguration(ear, app -> {
            // Dynamic (ToolManager) tools only: endpoint must be explicitly enabled
            app.getMcps().add(mcp(DYNAMIC_TRUE, "true"));
            // @Tool annotated methods: endpoint is opened by "auto"
            app.getMcps().add(mcp(ANNOTATED_AUTO, "auto"));
            // @Tool annotated methods, but endpoint explicitly disabled
            app.getMcps().add(mcp(ANNOTATED_FALSE, "false"));
            // Values with leading/trailing whitespace must behave like their trimmed equivalents
            app.getMcps().add(mcp(DYNAMIC_TRUE_SPACES, " true "));
            app.getMcps().add(mcp(ANNOTATED_FALSE_SPACES, " false "));
            // Not a valid value: a warning is logged and it falls back to "auto"
            app.getMcps().add(mcp(ANNOTATED_INVALID, "ture"));
        });

        // Standalone WARs with no CDI beans: with "false" CDI must not be consulted at all
        serverLifecycle.deployWithConfiguration(noCdiWar(NO_CDI_FALSE), app -> app.getMcps().add(mcp(null, "false")));
        serverLifecycle.deployWithConfiguration(noCdiWar(NO_CDI_AUTO), app -> {
        });
    }

    @AfterClass
    public static void tearDown() throws Exception {
        serverLifecycle.undeployWithConfiguration(NO_CDI_AUTO);
        serverLifecycle.undeployWithConfiguration(NO_CDI_FALSE);
        serverLifecycle.undeployWithConfiguration(EAR_NAME + ".ear");
    }

    private static Mcp mcp(String moduleName, String enableEndpoint) {
        Mcp mcp = new Mcp();
        mcp.setModuleName(moduleName);
        mcp.setEnableEndpoint(enableEndpoint);
        return mcp;
    }

    private static WebArchive dynamicWar(String name) {
        return ShrinkWrap.create(WebArchive.class, name + ".war")
                         .addPackage(DynamicOnlyToolsBean.class.getPackage());
    }

    private static WebArchive annotatedWar(String name) {
        return ShrinkWrap.create(WebArchive.class, name + ".war")
                         .addPackage(AnnotatedToolsBean.class.getPackage());
    }

    /** A module with no CDI beans (the {@code @Tool} class has no bean defining annotation), so CDI is inactive */
    private static WebArchive noCdiWar(String name) {
        return ShrinkWrap.create(WebArchive.class, name + ".war")
                         .addPackage(InactiveCdiTool.class.getPackage());
    }

    private static McpClient client(String contextRoot) {
        return McpClient.builder(serverLifecycle.getServer(), "/" + contextRoot).build();
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

    /** Surrounding whitespace in {@code enableEndpoint=" false "} must not make it fall back to {@code auto}. */
    @Test
    public void testFalseWithSurroundingWhitespaceHasNoEndpoint() throws Exception {
        assertNoEndpoint(ANNOTATED_FALSE_SPACES);
    }

    /** Surrounding whitespace in {@code enableEndpoint=" true "} must still enable the endpoint. */
    @Test
    public void testTrueWithSurroundingWhitespaceHasEndpoint() throws Exception {
        assertToolCall(DYNAMIC_TRUE_SPACES, "dynamic-only-tool", "{}", "dynamic-ok");
    }

    // ---- invalid enableEndpoint value -----------------------------------------------------

    /**
     * A value which is not {@code true}, {@code auto} or {@code false} is reported and treated as {@code auto}.
     * The module has {@code @Tool}-annotated methods, so {@code auto} gives it an endpoint. The warning is logged
     * only for the invalid value, and not for the valid ones (including those with surrounding whitespace).
     */
    @Test
    public void testInvalidValueWarnsAndFallsBackToAuto() throws Exception {
        assertToolCall(ANNOTATED_INVALID, "annotated-echo", "{\"input\":\"hello\"}", "hello");

        List<String> warnings = serverLifecycle.getServer().findStringsInLogs(INVALID_VALUE_WARNING);
        assertTrue("Expected a CWMCM0047W warning for the invalid value but found none", !warnings.isEmpty());
        assertTrue("Expected CWMCM0047W only for the ture value but found: " + warnings,
                   warnings.stream().allMatch(w -> w.contains("The ture value is not valid for the enableEndpoint attribute")));
    }

    /**
     * {@code false} must turn the endpoint off without touching CDI. Two standalone WARs without CDI are
     * deployed: the {@code false} one must not look up the tool registry, so only the default ({@code auto})
     * one reports that CDI is inactive.
     */
    @Test
    public void testFalseWithoutCdiDoesNotResolveCdi() throws Exception {
        assertNoEndpoint(NO_CDI_FALSE);
        assertNoEndpoint(NO_CDI_AUTO);
        List<String> inactive = serverLifecycle.getServer().findStringsInLogsAndTrace(CDI_INACTIVE);
        assertTrue("Expected the default (auto) module " + NO_CDI_AUTO + " to report CDI as inactive but found: " + inactive,
                   inactive.stream().anyMatch(s -> s.contains(NO_CDI_AUTO)));
        assertTrue("The disabled (false) module " + NO_CDI_FALSE + " must not look up CDI but found: " + inactive,
                   inactive.stream().noneMatch(s -> s.contains(NO_CDI_FALSE)));
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
