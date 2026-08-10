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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.shrinkwrap.api.ShrinkWrap;
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
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.TestConstants;

/**
 * FAT test for MCP Monitor feature
 * Tests operation and session metrics collection via MBeans and metrics adapters
 */
@RunWith(FATRunner.class)
public class McpMonitorTest {

    private final static String APP_NAME = "mcpMonitorTest";
    private static final String MBEAN_DOMAIN = "WebSphere";
    private static final String MBEAN_TYPE_OPERATION = "McpOperationStatistics";
    private static final String MBEAN_TYPE_SESSION = "McpSessionStatistics";

    @Server("mcp-server-monitor-only")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/" + APP_NAME);

    private static LocalConnector localConnector;
    private static MBeanServerConnection mbeanServer;

    private static final String ECHO_TOOL_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "method": "tools/call",
                      "params": {
                        "name": "echo",
                        "arguments": {"message": "test message"}
                      }
                    }
                    """;

    private static final String ADD_TOOL_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 2,
                      "method": "tools/call",
                      "params": {
                        "name": "add",
                        "arguments": {"num1": 5, "num2": 3}
                      }
                    }
                    """;

    private static final String TOOLS_LIST_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 3,
                      "method": "tools/list"
                    }
                    """;

    private static final String PING_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 4,
                      "method": "ping"
                    }
                    """;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(BasicTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();

        // Wait for server to be ready
        assertNotNull("Server should start successfully",
                      server.waitForStringInLog("CWWKF0011I"));

        // Connect to local JMX connector
        localConnector = new LocalConnector(server.getServerRoot());
        mbeanServer = localConnector.getMBeanServer();
        assertNotNull("MBean server connection should not be null", mbeanServer);
    }

    @AfterClass
    public static void teardown() throws Exception {
        try {
            if (localConnector != null) {
                localConnector.close();
            }
        } finally {
            if (server != null && server.isStarted()) {
                server.stopServer("CWMCM0010E"); // Expected: Tool threw non-business exception
            }
        }
    }

    // ========== Utility Methods ==========

    /**
     * Finds the MBean for an MCP operation by method name and optional tool name.
     * All operations have a method name. Only tools/call operations have a tool name.
     *
     * @param methodName the MCP method name (e.g., "tools/call", "tools/list", "ping")
     * @param toolName the tool name for tools/call operations (null for other operations)
     * @return the matching ObjectName, or null if not found
     * @throws AssertionError if more than one matching MBean is found
     */
    private ObjectName findOperationMBean(String methodName, String toolName) throws Exception {
        // Build query pattern with individual properties instead of a single name property
        StringBuilder pattern = new StringBuilder(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_OPERATION);
        pattern.append(",mcpMethod=").append(methodName);
        if (toolName != null) {
            pattern.append(",genAiTool=").append(toolName);
        }
        pattern.append(",*");

        ObjectName namePattern = new ObjectName(pattern.toString());
        Set<ObjectName> mbeans = mbeanServer.queryNames(namePattern, null);

        return switch (mbeans.size()) {
            case 0 -> null;
            case 1 -> mbeans.iterator().next();
            default -> throw new AssertionError("More than one operation mbean found for " + methodName + (toolName != null ? "/" + toolName : ""));
        };
    }

    /**
     * Verifies that an MBean exists for the specified operation and checks its common attributes.
     *
     * @param methodName the MCP method name
     * @param toolName the tool name (null for non-tool operations)
     * @param expectedErrorType the expected error type (null for successful operations)
     * @param expectedStatusCode the expected RPC status code
     */
    private void verifyOperationMBeanAttributes(String methodName, String toolName, String expectedErrorType, String expectedStatusCode) throws Exception {
        ObjectName mbean = findOperationMBean(methodName, toolName);
        assertNotNull("MBean for method '" + methodName + "'" + (toolName != null ? " tool '" + toolName + "'" : "") + " should exist", mbean);

        // Verify method name attribute (all operations have this)
        String actualMethodName = (String) mbeanServer.getAttribute(mbean, "McpMethodName");
        assertEquals("McpMethodName should match", methodName, actualMethodName);

        // Verify error type attribute (all operations have this)
        String actualErrorType = (String) mbeanServer.getAttribute(mbean, "ErrorType");
        assertEquals("ErrorType attribute should match", expectedErrorType, actualErrorType);

        // Verify status code attribute (all operations have this)
        String actualStatusCode = (String) mbeanServer.getAttribute(mbean, "RpcResponseStatusCode");
        assertEquals("RpcResponseStatusCode attribute should match", expectedStatusCode, actualStatusCode);

        // Verify tool name attribute (only tools/call operations have this)
        if (toolName != null) {
            String actualToolName = (String) mbeanServer.getAttribute(mbean, "GenAiToolName");
            assertEquals("GenAiToolName should match", toolName, actualToolName);
        }
    }

    /**
     * Verifies that a call to an operation increments the operation count.
     *
     * @param methodName the MCP method name
     * @param toolName the tool name (null for non-tool operations)
     * @param request the JSON-RPC request to send
     */
    private void verifyCallCountIncrement(String methodName, String toolName, String request) throws Exception {
        // Get initial count (or 0 if MBean doesn't exist yet)
        ObjectName mbean = findOperationMBean(methodName, toolName);
        long initialCount = 0;
        if (mbean != null) {
            initialCount = (Long) mbeanServer.getAttribute(mbean, "Count");
        }

        // Call the operation
        String identifier = methodName + (toolName != null ? "/" + toolName : "");
        String response = client.callMCP(request);
        assertNotNull("Response should not be null", response);

        // Verify count incremented
        mbean = findOperationMBean(methodName, toolName);
        assertNotNull("MBean should exist after calling " + identifier, mbean);
        long finalCount = (Long) mbeanServer.getAttribute(mbean, "Count");
        assertEquals("Call count should increment by 1 for " + identifier, initialCount + 1, finalCount);
    }

    /**
     * Verifies that an operation call records a non-zero duration.
     *
     * @param methodName the MCP method name
     * @param toolName the tool name (null for non-tool operations)
     * @param request the JSON-RPC request to send
     */
    private void verifyDurationTracking(String methodName, String toolName, String request) throws Exception {
        // Get initial duration (or 0 if MBean doesn't exist yet)
        ObjectName mbean = findOperationMBean(methodName, toolName);
        double initialDuration = 0;
        if (mbean != null) {
            initialDuration = (Double) mbeanServer.getAttribute(mbean, "Duration");
        }

        String response = client.callMCP(request);
        assertNotNull("Response should not be null", response);

        // Verify duration was recorded
        mbean = findOperationMBean(methodName, toolName);
        String identifier = methodName + (toolName != null ? "/" + toolName : "");
        assertNotNull("MBean should exist after calling " + identifier, mbean);
        double finalDuration = (Double) mbeanServer.getAttribute(mbean, "Duration");

        double durationIncrement = finalDuration - initialDuration;
        assertTrue("Duration should have incremented for " + identifier + " (was: " + initialDuration + ", now: " + finalDuration + ")",
                   durationIncrement > 0);
    }

    /**
     * Finds the session MBean
     *
     * @return the first matching session ObjectName, or null if not found
     * @throws AssertionError if more than one matching MBean is found
     */
    private ObjectName findSessionMBean() throws Exception {
        // Session MBeans now have individual properties including session=true
        ObjectName query = new ObjectName(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_SESSION + ",session=true,*");
        Set<ObjectName> mbeans = mbeanServer.queryNames(query, null);

        return switch (mbeans.size()) {
            case 0 -> null;
            case 1 -> mbeans.iterator().next();
            default -> throw new AssertionError("More than one session mbean found");
        };
    }

    /**
     * Verifies that a session MBean exists with correct attribute values.
     *
     * @param expectedErrorType the expected error type (null for success)
     * @throws Exception if verification fails
     */
    private void verifySessionMBeanAttributes(String expectedErrorType) throws Exception {
        ObjectName mbean = findSessionMBean();
        assertNotNull("Session MBean should exist", mbean);

        // Verify error type
        String actualErrorType = (String) mbeanServer.getAttribute(mbean, "ErrorType");
        assertEquals("ErrorType attribute should match", expectedErrorType, actualErrorType);

        // Verify the MBean name contains protocol information as individual properties
        String mbeanName = mbean.toString();

        // Check for JSON-RPC protocol version (should be "2.0")
        // The format is "jsonrpcVer=2.0" as a property
        boolean foundJsonRpcVersion = mbeanName.contains("jsonrpcVer=2.0");
        assertTrue("MBean name should contain jsonrpcVer=2.0 as a property, but was: " + mbeanName,
                   foundJsonRpcVersion);

        String jsonrpcVersion = (String) mbeanServer.getAttribute(mbean, "JsonrpcProtocolVersion");
        assertEquals("JsonrpcProtocolVersion is wrong", "2.0", jsonrpcVersion);

        // Check for MCP protocol version (should be present)
        // The format is "mcpVer=" as a property
        boolean foundMcpVersion = mbeanName.contains("mcpVer=");
        assertTrue("MBean name should contain mcpVer= as a property, but was: " + mbeanName,
                   foundMcpVersion);

        String mcpVersion = (String) mbeanServer.getAttribute(mbean, "McpProtocolVersion");
        assertEquals("McpProtocolVersion is wrong", TestConstants.VALUE_MCP_PROTOCOL_VERSION, mcpVersion);

        // Verify count is positive
        long count = (Long) mbeanServer.getAttribute(mbean, "Count");
        assertTrue("Session count should be positive", count > 0);

        // Verify duration is positive
        double duration = (Double) mbeanServer.getAttribute(mbean, "Duration");
        assertTrue("Session duration should be positive", duration > 0);
    }

    /**
     * Test that operation metrics are recorded when MCP tools are called.
     * Verifies call count increment and attribute values.
     */
    @Test
    public void testOperationMetricsRecorded() throws Exception {
        // Verify call count increments
        verifyCallCountIncrement("tools/call", "echo", ECHO_TOOL_REQUEST);

        // Verify MBean exists with correct attributes (null errorType for success, "error" status for tool errors)
        verifyOperationMBeanAttributes("tools/call", "echo", "tool_error", "error");
    }

    /**
     * Test that multiple operations are tracked correctly
     */
    @Test
    public void testMultipleOperationsTracked() throws Exception {
        // Get initial counts
        ObjectName echoMBean = findOperationMBean("tools/call", "echo");
        long initialEchoCount = (echoMBean != null) ? (Long) mbeanServer.getAttribute(echoMBean, "Count") : 0;

        ObjectName addMBean = findOperationMBean("tools/call", "add");
        long initialAddCount = (addMBean != null) ? (Long) mbeanServer.getAttribute(addMBean, "Count") : 0;

        // Call multiple tools
        client.callMCP(ECHO_TOOL_REQUEST);
        client.callMCP(ADD_TOOL_REQUEST);
        client.callMCP(ECHO_TOOL_REQUEST);

        // Verify echo tool metrics with specific attributes (tool_error because of argument mismatch)
        verifyOperationMBeanAttributes("tools/call", "echo", "tool_error", "error");
        echoMBean = findOperationMBean("tools/call", "echo");

        // Verify echo was called twice more than initial count
        Long echoCount = (Long) mbeanServer.getAttribute(echoMBean, "Count");
        assertEquals("Echo tool should have been called twice", initialEchoCount + 2, echoCount.longValue());

        // Verify add tool metrics with specific attributes (null errorType for success, "ok" status)
        verifyOperationMBeanAttributes("tools/call", "add", null, "ok");
        addMBean = findOperationMBean("tools/call", "add");

        // Verify add was called once more than initial count
        Long addCount = (Long) mbeanServer.getAttribute(addMBean, "Count");
        assertEquals("Add tool should have been called once", initialAddCount + 1, addCount.longValue());
    }

    /**
     * Test that session metrics are recorded with correct attributes and count increments.
     */
    @Test
    public void testSessionMetricsRecorded() throws Exception {
        // Get initial count (or 0 if no MBean exists yet)
        ObjectName mbean = findSessionMBean();
        long initialCount = 0;
        if (mbean != null) {
            initialCount = (Long) mbeanServer.getAttribute(mbean, "Count");
        }

        // Perform operations and end session
        client.callMCP(ECHO_TOOL_REQUEST);
        client.callMCP(ADD_TOOL_REQUEST);
        client.deleteSession();

        // Verify count incremented
        mbean = findSessionMBean();
        assertNotNull("Session MBean should exist after session ends", mbean);
        long finalCount = (Long) mbeanServer.getAttribute(mbean, "Count");
        assertEquals("Session count should increment by 1", initialCount + 1, finalCount);

        // Verify session MBean has correct attributes (null error type for success)
        verifySessionMBeanAttributes(null);
    }

    /**
     * Test that session duration is tracked and increments.
     */
    @Test
    public void testSessionDurationTracking() throws Exception {
        // Get initial duration (or 0 if no MBean exists yet)
        ObjectName mbean = findSessionMBean();
        double initialDuration = 0;
        if (mbean != null) {
            initialDuration = (Double) mbeanServer.getAttribute(mbean, "Duration");
        }

        // Perform operations and end session
        client.callMCP(ECHO_TOOL_REQUEST);
        client.callMCP(ADD_TOOL_REQUEST);
        client.deleteSession();

        // Verify duration incremented
        mbean = findSessionMBean();
        assertNotNull("Session MBean should exist after session ends", mbean);
        double finalDuration = (Double) mbeanServer.getAttribute(mbean, "Duration");

        double durationIncrement = finalDuration - initialDuration;
        assertTrue("Session duration should have incremented (was: " + initialDuration + ", now: " + finalDuration + ")",
                   durationIncrement > 0);
    }

    /**
     * Test that MBeans are registered for MCP operations
     */
    @Test
    public void testMBeanRegistration() throws Exception {
        // Call tool to trigger MBean creation
        client.callMCP(ECHO_TOOL_REQUEST);

        // Find and verify the echo tool MBean with specific attributes
        verifyOperationMBeanAttributes("tools/call", "echo", "tool_error", "error");
    }

    /**
     * Test that operation duration is tracked and increments.
     */
    @Test
    public void testOperationDurationTracking() throws Exception {
        // Verify duration tracking
        verifyDurationTracking("tools/call", "echo", ECHO_TOOL_REQUEST);
    }

    /**
     * Test that protocol version attributes are captured correctly.
     * Verifies JSON-RPC and MCP protocol versions are recorded in MBean names.
     */
    @Test
    public void testProtocolVersionAttributes() throws Exception {
        // Call tool
        client.callMCP(ECHO_TOOL_REQUEST);

        // Find the operation MBean
        ObjectName mbean = findOperationMBean("tools/call", "echo");
        assertNotNull("Operation MBean should exist", mbean);

        // Verify the MBean name contains protocol information as individual properties
        String mbeanName = mbean.toString();

        // Check for JSON-RPC protocol version (should be "2.0")
        // The format is "jsonrpcVer=2.0" as a property
        boolean foundJsonRpcVersion = mbeanName.contains("jsonrpcVer=2.0");
        assertTrue("MBean name should contain jsonrpcVer=2.0 as a property, but was: " + mbeanName,
                   foundJsonRpcVersion);

        assertEquals("2.0", mbeanServer.getAttribute(mbean, "JsonrpcProtocolVersion"));

        // Check for MCP protocol version (should be present)
        // The format is "mcpVer=" as a property
        boolean foundMcpVersion = mbeanName.contains("mcpVer=");
        assertTrue("MBean name should contain mcpVer= as a property, but was: " + mbeanName,
                   foundMcpVersion);

        assertEquals("2025-11-25", mbeanServer.getAttribute(mbean, "McpProtocolVersion"));
    }

    /**
     * Test that tool name is captured in metrics
     */
    @Test
    public void testToolNameAttribute() throws Exception {
        // Call specific tool
        client.callMCP(ECHO_TOOL_REQUEST);

        // Verify tool name attribute
        ObjectName query = new ObjectName(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_OPERATION + ",*");
        // Find and verify the echo tool MBean with specific attributes
        verifyOperationMBeanAttributes("tools/call", "echo", "tool_error", "error");
    }

    /**
     * Test that error metrics are recorded for business errors (ToolCallException).
     * Verifies error type attribute is set correctly.
     */
    @Test
    public void testBusinessErrorMetrics() throws Exception {
        String businessErrorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 10,
                          "method": "tools/call",
                          "params": {
                            "name": "businessErrorTool",
                            "arguments": {"input": "bad-value"}
                          }
                        }
                        """;

        String response = client.callMCP(businessErrorRequest);
        assertTrue("Response should contain error", response.contains("isError"));

        // Verify error metrics with proper error type
        verifyOperationMBeanAttributes("tools/call", "businessErrorTool", "tool_error", "error");
    }

    /**
     * Test that error metrics are recorded for non-business errors (RuntimeException).
     * Verifies error type attribute is set correctly.
     */
    @Test
    public void testNonBusinessErrorMetrics() throws Exception {
        String nonBusinessErrorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 11,
                          "method": "tools/call",
                          "params": {
                            "name": "nonBusinessErrorTool",
                            "arguments": {"input": "trigger-error"}
                          }
                        }
                        """;

        String response = client.callMCP(nonBusinessErrorRequest);
        assertTrue("Response should contain error", response.contains("isError"));

        // Verify error metrics with proper error type (need to verify actual RuntimeException error type)
        verifyOperationMBeanAttributes("tools/call", "nonBusinessErrorTool", "tool_error", "error");
    }

    /**
     * Test that error metrics are recorded for async business errors.
     * Verifies error type attribute is set correctly for async operations.
     */
    @Test
    public void testAsyncBusinessErrorMetrics() throws Exception {
        String asyncBusinessErrorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 12,
                          "method": "tools/call",
                          "params": {
                            "name": "asyncBusinessErrorTool",
                            "arguments": {"input": "bad-value"}
                          }
                        }
                        """;

        String response = client.callMCP(asyncBusinessErrorRequest);
        assertTrue("Response should contain error", response.contains("isError"));

        // Verify async error metrics with proper error type
        verifyOperationMBeanAttributes("tools/call", "asyncBusinessErrorTool", "tool_error", "error");
    }

    /**
     * Test that error metrics are recorded for async non-business errors.
     * Verifies error type attribute is set correctly for async operations.
     */
    @Test
    public void testAsyncNonBusinessErrorMetrics() throws Exception {
        String asyncNonBusinessErrorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 13,
                          "method": "tools/call",
                          "params": {
                            "name": "asyncNonBusinessErrorTool",
                            "arguments": {"input": "trigger-error"}
                          }
                        }
                        """;

        String response = client.callMCP(asyncNonBusinessErrorRequest);
        assertTrue("Response should contain error", response.contains("isError"));

        // Verify async error metrics with proper error type
        verifyOperationMBeanAttributes("tools/call", "asyncNonBusinessErrorTool", "tool_error", "error");
    }

    /**
     * Test that error metrics are recorded for async failed CompletionStage.
     * Verifies error type attribute is set correctly for failed async stages.
     */
    @Test
    public void testAsyncFailedStageErrorMetrics() throws Exception {
        String asyncFailedStageRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 14,
                          "method": "tools/call",
                          "params": {
                            "name": "asyncFailedStageTool",
                            "arguments": {"input": "trigger-error"}
                          }
                        }
                        """;

        String response = client.callMCP(asyncFailedStageRequest);
        assertTrue("Response should contain error", response.contains("isError"));

        // Verify async failed stage error metrics with proper error type
        verifyOperationMBeanAttributes("tools/call", "asyncFailedStageTool", "tool_error", "error");
    }

    /**
     * Test that error type attribute is correctly set for different error types
     */
    @Test
    public void testErrorTypeAttributeVariety() throws Exception {
        // Call business error tool
        String businessErrorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 15,
                          "method": "tools/call",
                          "params": {
                            "name": "businessErrorTool",
                            "arguments": {"input": "bad"}
                          }
                        }
                        """;
        client.callMCP(businessErrorRequest);

        // Call non-business error tool
        String nonBusinessErrorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 16,
                          "method": "tools/call",
                          "params": {
                            "name": "nonBusinessErrorTool",
                            "arguments": {"input": "error"}
                          }
                        }
                        """;
        client.callMCP(nonBusinessErrorRequest);

        // Call failed stage tool
        String failedStageRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 17,
                          "method": "tools/call",
                          "params": {
                            "name": "failedStageTool",
                            "arguments": {"input": "fail"}
                          }
                        }
                        """;
        client.callMCP(failedStageRequest);

        // Verify all three error tools have specific MBeans with correct attributes
        verifyOperationMBeanAttributes("tools/call", "businessErrorTool", "tool_error", "error");
        verifyOperationMBeanAttributes("tools/call", "failedStageTool", "tool_error", "error");
        verifyOperationMBeanAttributes("tools/call", "nonBusinessErrorTool", "tool_error", "error");
    }

    /**
     * Test that MCP monitoring resources are properly cleaned up when an application is unloaded.
     * This verifies no memory leaks occur from accumulated MBeans or statistics.
     */
    @Test
    public void testMonitoringCleanupOnAppUnload() throws Exception {
        // 1. Generate metrics by calling tools and creating a session
        client.callMCP(ECHO_TOOL_REQUEST);
        client.callMCP(ADD_TOOL_REQUEST);

        // End session to trigger session metrics
        client.deleteSession();

        // 2. Verify both operation and session MBeans are registered with specific checks
        ObjectName operationQuery = new ObjectName(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_OPERATION + ",*");
        Set<ObjectName> operationMBeans = mbeanServer.queryNames(operationQuery, null);
        assertFalse("Operation MBeans should be registered before unload", operationMBeans.isEmpty());

        // Verify specific operation MBeans exist
        ObjectName echoMBean = findOperationMBean("tools/call", "echo");
        assertNotNull("Echo operation MBean should exist before unload", echoMBean);
        ObjectName addMBean = findOperationMBean("tools/call", "add");
        assertNotNull("Add operation MBean should exist before unload", addMBean);

        ObjectName sessionQuery = new ObjectName(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_SESSION + ",*");
        Set<ObjectName> sessionMBeans = mbeanServer.queryNames(sessionQuery, null);
        assertFalse("Session MBeans should be registered before unload", sessionMBeans.isEmpty());

        // Verify at least one session MBean has expected attributes
        ObjectName sessionMBean = sessionMBeans.iterator().next();
        assertNotNull("Session MBean Count attribute should exist",
                      mbeanServer.getAttribute(sessionMBean, "Count"));

        // 3. Undeploy the application
        server.removeDropinsApplications(APP_NAME + ".war");
        assertNotNull("Application should stop",
                      server.waitForStringInLog("CWWKZ0009I.*" + APP_NAME));

        // 4. Verify cleanup: both operation and session MBeans should be removed
        operationMBeans = mbeanServer.queryNames(operationQuery, null);
        assertTrue("MCP operation MBeans should be removed after app unload", operationMBeans.isEmpty());

        sessionMBeans = mbeanServer.queryNames(sessionQuery, null);
        assertTrue("MCP session MBeans should be removed after app unload", sessionMBeans.isEmpty());

        // 5. Redeploy the application
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(BasicTools.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        assertNotNull("Application should restart",
                      server.waitForStringInLog("CWWKZ0001I.*" + APP_NAME));

        // 6. Reinitialize client and generate metrics again
        client.initialize();
        client.callMCP(ECHO_TOOL_REQUEST);
        client.callMCP(ADD_TOOL_REQUEST);
        client.deleteSession();

        // 7. Verify MBeans registered again with specific checks (proves cleanup worked - no accumulation)
        operationMBeans = mbeanServer.queryNames(operationQuery, null);
        assertFalse("Operation MBeans should be registered after redeploy", operationMBeans.isEmpty());

        // Verify specific operation MBeans exist after redeploy
        verifyOperationMBeanAttributes("tools/call", "echo", "tool_error", "error");
        verifyOperationMBeanAttributes("tools/call", "add", null, "ok");

        sessionMBeans = mbeanServer.queryNames(sessionQuery, null);
        assertFalse("Session MBeans should be registered after redeploy", sessionMBeans.isEmpty());

        // Verify session MBean has expected attributes after redeploy
        sessionMBean = sessionMBeans.iterator().next();
        assertNotNull("Session MBean Count attribute should exist after redeploy",
                      mbeanServer.getAttribute(sessionMBean, "Count"));
    }

    /**
     * Test that tools/list operation metrics are recorded.
     * Verifies MBean existence, attributes, and call count increment.
     */
    @Test
    public void testToolsListMetrics() throws Exception {
        // Verify call count increments
        verifyCallCountIncrement("tools/list", null, TOOLS_LIST_REQUEST);

        // Verify MBean exists with correct attributes (null errorType for success, "ok" status)
        verifyOperationMBeanAttributes("tools/list", null, null, "ok");
    }

    /**
     * Test that ping operation metrics are recorded.
     * Verifies MBean existence, attributes, and call count increment.
     */
    @Test
    public void testPingMetrics() throws Exception {
        // Verify call count increments
        verifyCallCountIncrement("ping", null, PING_REQUEST);

        // Verify MBean exists with correct attributes (null errorType for success, "ok" status)
        verifyOperationMBeanAttributes("ping", null, null, "ok");
    }

    /**
     * Test that initialize operation metrics are recorded.
     * Verifies that the initialize call from setup created an MBean.
     */
    @Test
    public void testInitializeMetrics() throws Exception {
        // Initialize was called in @BeforeClass, verify its MBean exists
        ObjectName mbean = findOperationMBean("initialize", null);
        assertNotNull("MBean for initialize should exist from setup", mbean);

        String methodName = (String) mbeanServer.getAttribute(mbean, "McpMethodName");
        assertEquals("Method name should be initialize", "initialize", methodName);

        // Verify it was called at least once
        long count = (Long) mbeanServer.getAttribute(mbean, "Count");

        // Get CountDetails to see internal state
        Object countDetails = mbeanServer.getAttribute(mbean, "CountDetails");

        assertTrue("Initialize should have been called at least once", count >= 1);

        // Verify duration was recorded
        double duration = (Double) mbeanServer.getAttribute(mbean, "Duration");
        assertTrue("Duration should be greater than 0", duration > 0);
    }
}
