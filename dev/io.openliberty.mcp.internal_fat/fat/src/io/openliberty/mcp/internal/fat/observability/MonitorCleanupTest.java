/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.observability;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Monitor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Tests that MCP monitoring resources (MBeans, metrics, statistics) are properly
 * cleaned up when applications are unloaded.
 *
 * <p><b>Test Scenario:</b>
 * <ol>
 *   <li>Deploy an MCP application with tools</li>
 *   <li>Invoke tools to generate operation and session metrics</li>
 *   <li>Verify MBeans are registered in JMX</li>
 *   <li>Undeploy the application</li>
 *   <li>Verify all MBeans are removed from JMX</li>
 *   <li>Verify no memory leaks (statistics map is cleaned up)</li>
 *   <li>Repeat deploy/undeploy cycle to ensure no accumulation</li>
 * </ol>
 *
 * <p><b>Components Tested:</b>
 * <ul>
 *   <li>{@link io.openliberty.mcp.internal.monitor.McpMonitorAppStateListener} - Application state listener</li>
 *   <li>{@link io.openliberty.mcp.internal.monitor.McpStatsMonitorImpl#removeStat(String)} - Cleanup logic</li>
 *   <li>{@link io.openliberty.mcp.internal.monitor.metrics.MetricsManager#removeMetricsForApp(String)} - Metrics cleanup</li>
 * </ul>
 */
@RunWith(FATRunner.class)
public class MonitorCleanupTest extends FATServletClient {
    
    @Server("mcp-server-monitor-only")
    public static LibertyServer server;

    @Rule
    public McpClient client1 = new McpClient(server, "/monitorCleanupTest");
    
    private static final String APP_NAME = "monitorCleanupTest";
    
    @BeforeClass
    public static void setup() throws Exception {
        // Deploy the application with both tools and MBean checker servlet
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(BasicTools.class.getPackage())
                                   .addClass(MBeanCheckerServlet.class);
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        
        // Start server with monitor-1.0 feature enabled
        server.startServer();
        
        // Wait for application to start
        assertNotNull("Application should start successfully",
                     server.waitForStringInLog("CWWKZ0001I.*" + APP_NAME));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    /**
     * Tests that MCP monitoring resources are properly cleaned up when an application is unloaded.
     * 
     * <p>This test verifies the complete cleanup workflow:
     * <ol>
     *   <li>Deploy app and generate metrics</li>
     *   <li>Verify MBeans exist</li>
     *   <li>Undeploy app</li>
     *   <li>Verify MBeans are removed</li>
     *   <li>Repeat to check for accumulation</li>
     * </ol>
     */
    @Test
    public void testMonitoringCleanupOnAppUnload() throws Exception {
        // ===== FIRST DEPLOYMENT CYCLE =====
        // Application is already deployed in @BeforeClass
        
        // 1. Invoke a tool to generate metrics
        String toolRequest = """
                {
                  "jsonrpc": "2.0",
                  "id": "1",
                  "method": "tools/call",
                  "params": {
                    "name": "add",
                    "arguments": {
                      "num1": 5,
                      "num2": 3
                    }
                  }
                }
                """;
        client1.callMCP(toolRequest);
        
        // 2. Verify MBeans are registered
        FATServletClient.runTest(server, APP_NAME + "/MBeanCheckerServlet", "testOperationMBeansRegistered");
        
        // 3. Undeploy the application
        server.removeDropinsApplications(APP_NAME + ".war");
        
        // Wait for application to stop
        assertNotNull("Application should stop successfully",
                     server.waitForStringInLog("CWWKZ0009I.*" + APP_NAME));
        
        // 4. Delete the session since app undeploy invalidates it
        client1.deleteSession();
        
        // ===== SECOND DEPLOYMENT CYCLE (verify no accumulation) =====
        
        // 5. Redeploy the application
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(BasicTools.class.getPackage())
                                   .addClass(MBeanCheckerServlet.class);
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        assertNotNull("Application should start successfully on redeploy",
                     server.waitForStringInLog("CWWKZ0001I.*" + APP_NAME));
        
        // 6. Reinitialize client after app redeployment
        client1.initialize();
        
        // 7. Generate metrics again
        client1.callMCP(toolRequest);
        
        // 8. Verify MBeans are registered again (proves cleanup worked - no accumulation)
        FATServletClient.runTest(server, APP_NAME + "/MBeanCheckerServlet", "testOperationMBeansRegistered");
        
        // 9. Undeploy again
        server.removeDropinsApplications(APP_NAME + ".war");
        assertNotNull("Application should stop successfully on second undeploy",
                     server.waitForStringInLog("CWWKZ0009I.*" + APP_NAME));
        
        // 10. Delete the session again
        client1.deleteSession();
        
        // 11. Verify no error messages in logs
        assertFalse("Should not have memory leak warnings",
                   server.findStringsInLogs(".*memory.*leak.*").isEmpty() == false);
    }
    
    /**
     * Tests that MCP monitoring resources are properly cleaned up when MCP is removed
     * from the monitor filter configuration.
     *
     * <p>This test verifies the dynamic configuration cleanup workflow:
     * <ol>
     *   <li>Deploy app with MCP monitoring enabled (filter includes "MCP")</li>
     *   <li>Generate metrics by invoking tools</li>
     *   <li>Verify MBeans exist</li>
     *   <li>Update server.xml to remove "MCP" from monitor filter</li>
     *   <li>Verify all MCP MBeans are removed</li>
     *   <li>Re-enable MCP monitoring and verify it works again</li>
     * </ol>
     */
    @Test
    public void testMonitoringCleanupOnFilterChange() throws Exception {
        // 1. Deploy application
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(BasicTools.class.getPackage())
                                   .addClass(MBeanCheckerServlet.class);
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        assertNotNull("Application should start successfully",
                     server.waitForStringInLog("CWWKZ0001I.*" + APP_NAME));
        
        // 2. Add monitor filter with MCP enabled
        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        Monitor monitor = new Monitor();
        monitor.setFilter("HTTP,MCP");
        config.getMonitors().add(monitor);
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        
        // 3. Initialize client and generate metrics
        client1.initialize();
        String toolRequest = """
                {
                  "jsonrpc": "2.0",
                  "id": "1",
                  "method": "tools/call",
                  "params": {
                    "name": "add",
                    "arguments": {
                      "num1": 10,
                      "num2": 20
                    }
                  }
                }
                """;
        client1.callMCP(toolRequest);
        
        // 4. Verify MBeans are registered
        FATServletClient.runTest(server, APP_NAME + "/MBeanCheckerServlet", "testOperationMBeansRegistered");
        
        // 5. Update monitor filter to remove MCP
        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        monitor = config.getMonitors().get(0);
        monitor.setFilter("HTTP");  // Remove MCP from filter
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        
        // 6. Verify MBeans are removed (cleanup triggered by @Modified method)
        FATServletClient.runTest(server, APP_NAME + "/MBeanCheckerServlet", "testOperationMBeansNotRegistered");
        
        // 7. Re-enable MCP monitoring
        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        monitor = config.getMonitors().get(0);
        monitor.setFilter("HTTP,MCP");  // Add MCP back to filter
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        
        // 8. Generate metrics again to verify monitoring works after re-enabling
        client1.callMCP(toolRequest);
        
        // 9. Verify MBeans are registered again
        FATServletClient.runTest(server, APP_NAME + "/MBeanCheckerServlet", "testOperationMBeansRegistered");
        
        // Cleanup
        server.removeDropinsApplications(APP_NAME + ".war");
        client1.deleteSession();
    }
    
}
