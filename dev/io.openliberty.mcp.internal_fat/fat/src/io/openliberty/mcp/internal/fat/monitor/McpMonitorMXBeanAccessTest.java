/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.monitor;

import static io.openliberty.mcp.internal.fat.suite.McpMonitorServerSuite.serverLifecycle;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.monitor.mxbeanAccessApp.McpMXBeanAccessServlet;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * FAT test that verifies the MCP monitor MXBean interfaces ({@code McpOperationStatisticsMXBean}
 * and {@code McpSessionStatisticsMXBean}) are accessible from application code running inside
 * a deployed Liberty application.
 *
 * <p>The test deploys a WAR containing both the MCP tool bean and
 * {@link McpMXBeanAccessServlet}, which calls
 * {@link java.lang.management.ManagementFactory#getPlatformMBeanServer()} — exactly as
 * real application code would — to query and read MBean attributes. Assertions run
 * in-process inside the server, so there is no remote JMX connector involved.
 */
@RunWith(FATRunner.class)
public class McpMonitorMXBeanAccessTest extends FATServletClient {

    private static final String APP_NAME = "mcpMXBeanAccessTest";
    private static final String SERVLET = APP_NAME + "/McpMXBeanAccessServlet";

    // Server is managed by McpMonitorServerSuite — do NOT add @Server or copy the field here.
    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();

    /** Calls the {@code add} tool which succeeds (status "ok", no error type). */
    private static final String ADD_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "method": "tools/call",
                      "params": {
                        "name": "add",
                        "arguments": {"num1": 2, "num2": 3}
                      }
                    }
                    """;

    /** Lists all tools — no tool name, should produce status "ok". */
    private static final String TOOLS_LIST_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 2,
                      "method": "tools/list"
                    }
                    """;

    /** Simple ping — no tool name, should produce status "ok". */
    private static final String PING_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 3,
                      "method": "ping"
                    }
                    """;

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        serverLifecycle.getServer().addIgnoredErrors(List.of("CWMCM0010E"));

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(BasicTools.class.getPackage())
                                   .addPackage(McpMXBeanAccessServlet.class.getPackage())
                                   .addAsManifestResource(McpMonitorMXBeanAccessTest.class.getResource("permissions.xml"), "permissions.xml");
        serverLifecycle.deployDropinApp(war);
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    /**
     * Verifies that all String attributes defined by {@code McpOperationStatisticsMXBean} are
     * readable with the correct Java type via {@link java.lang.management.ManagementFactory#getPlatformMBeanServer()}
     * from inside the deployed application.
     */
    @Test
    public void testOperationMXBeanStringAttributesReadable() throws Exception {
        client.callMCP(ADD_REQUEST);
        runTest(serverLifecycle.getServer(), SERVLET, "testOperationMXBeanStringAttributesReadable");
    }

    /**
     * Verifies that the numeric attributes ({@code Count} and {@code Duration})
     * are readable with the correct primitive-wrapper Java types from inside the application.
     */
    @Test
    public void testOperationMXBeanNumericAttributesReadable() throws Exception {
        client.callMCP(ADD_REQUEST);
        client.callMCP(ADD_REQUEST);
        runTest(serverLifecycle.getServer(), SERVLET, "testOperationMXBeanNumericAttributesReadable");
    }

    /**
     * Verifies that {@code CountDetails} and {@code DurationDetails} are exposed as
     * {@link javax.management.openmbean.CompositeData} with the expected named fields,
     * and that the composite count agrees with the scalar {@code Count} attribute.
     */
    @Test
    public void testOperationMXBeanCompositeAttributesReadable() throws Exception {
        client.callMCP(ADD_REQUEST);
        runTest(serverLifecycle.getServer(), SERVLET, "testOperationMXBeanCompositeAttributesReadable");
    }

    /**
     * Verifies that the count increments for each MCP call and that duration grows.
     */
    @Test
    public void testOperationMXBeanCountAccumulates() throws Exception {
        client.callMCP(TOOLS_LIST_REQUEST);
        client.callMCP(TOOLS_LIST_REQUEST);
        runTest(serverLifecycle.getServer(), SERVLET, "testOperationMXBeanCountAccumulates");
    }

    /**
     * Verifies that all String attributes defined by {@code McpSessionStatisticsMXBean} are
     * readable from inside the application after a session is ended.
     */
    @Test
    public void testSessionMXBeanStringAttributesReadable() throws Exception {
        client.callMCP(ADD_REQUEST);
        client.deleteSession();
        runTest(serverLifecycle.getServer(), SERVLET, "testSessionMXBeanStringAttributesReadable");
    }

    /**
     * Verifies that the numeric attributes of {@code McpSessionStatisticsMXBean} carry the
     * correct types and positive values after a real session has completed.
     */
    @Test
    public void testSessionMXBeanNumericAttributesReadable() throws Exception {
        client.callMCP(ADD_REQUEST);
        client.deleteSession();
        runTest(serverLifecycle.getServer(), SERVLET, "testSessionMXBeanNumericAttributesReadable");
    }

    /**
     * Verifies that {@code CountDetails} and {@code DurationDetails} on a session MBean
     * are exposed as {@link javax.management.openmbean.CompositeData} with the expected fields.
     */
    @Test
    public void testSessionMXBeanCompositeAttributesReadable() throws Exception {
        client.callMCP(ADD_REQUEST);
        client.deleteSession();
        runTest(serverLifecycle.getServer(), SERVLET, "testSessionMXBeanCompositeAttributesReadable");
    }

    /**
     * Verifies that the operation MBean ObjectName for a {@code tools/call} operation
     * contains the key properties ({@code mcpMethod}, {@code genAiTool}, {@code jsonrpcVer},
     * {@code mcpVer}) that the public API documents.
     */
    @Test
    public void testOperationMBeanObjectNameKeyProperties() throws Exception {
        client.callMCP(ADD_REQUEST);
        runTest(serverLifecycle.getServer(), SERVLET, "testOperationMBeanObjectNameKeyProperties");
    }

    /**
     * Verifies that a {@code tools/list} operation MBean does NOT have a {@code genAiTool}
     * key property.
     */
    @Test
    public void testNonToolCallMBeanHasNoToolNameProperty() throws Exception {
        client.callMCP(TOOLS_LIST_REQUEST);
        runTest(serverLifecycle.getServer(), SERVLET, "testNonToolCallMBeanHasNoToolNameProperty");
    }

    /**
     * Verifies that the session MBean ObjectName contains the {@code session=true} property
     * and the protocol-version key properties.
     */
    @Test
    public void testSessionMBeanObjectNameKeyProperties() throws Exception {
        client.callMCP(PING_REQUEST);
        client.deleteSession();
        runTest(serverLifecycle.getServer(), SERVLET, "testSessionMBeanObjectNameKeyProperties");
    }

    /**
     * Verifies via {@link javax.management.MBeanInfo} that every attribute declared in
     * {@code McpOperationStatisticsMXBean} is exposed in the registered MBean.
     */
    @Test
    public void testOperationMBeanInfoContainsAllInterfaceAttributes() throws Exception {
        client.callMCP(ADD_REQUEST);
        runTest(serverLifecycle.getServer(), SERVLET, "testOperationMBeanInfoContainsAllInterfaceAttributes");
    }

    /**
     * Verifies via {@link javax.management.MBeanInfo} that every attribute declared in
     * {@code McpSessionStatisticsMXBean} is exposed in the registered session MBean.
     */
    @Test
    public void testSessionMBeanInfoContainsAllInterfaceAttributes() throws Exception {
        client.callMCP(PING_REQUEST);
        client.deleteSession();
        runTest(serverLifecycle.getServer(), SERVLET, "testSessionMBeanInfoContainsAllInterfaceAttributes");
    }
}
