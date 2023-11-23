/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics.internal.monitor_fat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@AllowedFFDC("javax.management.InstanceNotFoundException")
public class ComputedMetricsTest {

    private static Class<?> c = ComputedMetricsTest.class;

    private static final String[] ALWAYS_EXPECTED_COMPUTED_METRICS = new String[]{
            "vendor_cpu_processCpuUtilization_percent",
            "vendor_memory_heapUtilization_percent",
            "vendor_gc_time_per_cycle_seconds{name="
    };
    
    private static final String[] ALWAYS_EXPECTED_COMPUTED_METRICS_WITH_APPNAME = new String[]{
            "vendor_cpu_processCpuUtilization_percent{_app=\"myserver\"}",
            "vendor_memory_heapUtilization_percent{_app=\"myserver\"}",
            "vendor_gc_time_per_cycle_seconds{_app=\"myserver\",name="
    };
    
    private static boolean initialServerStart = true;

    @Server("ComputedMetricsServer")
    public static LibertyServer computedMetricsServer;

    @BeforeClass
    public static void setUp() throws Exception {
        trustAll();
        addRequiredApplicationsToServer();
        computedMetricsServer.saveServerConfiguration();
    }
    
    @Before
    public void startTest() throws Exception {
        String testName = "startTest";

        Log.info(c, testName, "------- Starting server ------");
        computedMetricsServer.startServer();
        waitForSecurityPrerequisites(computedMetricsServer, 60000);
        computedMetricsServer.setMarkToEndOfLog(computedMetricsServer.getMostRecentTraceFile());
        
        // Server already started for the first time.
        initialServerStart = false;
    }

    @After
    public void tearDown() throws Exception {
        String testName = "tearDown";
        
        if (computedMetricsServer != null && computedMetricsServer.isStarted()) {
            Log.info(c, testName, "------- Stopping server ------");
            computedMetricsServer.stopServer("");
        }
        
        // Remove the created data sources after each test
        Log.info(c, testName, "------- Removing Data sources. ------");
        computedMetricsServer.deleteDirectoryFromLibertyServerRoot("exampleDS1");
        computedMetricsServer.deleteDirectoryFromLibertyServerRoot("exampleDS2");

        Log.info(c, testName, "------- Restoring server configuration. ------");
        computedMetricsServer.restoreServerConfiguration();
    }

    private static void addRequiredApplicationsToServer() throws Exception {
        String testName = "addRequiredApplicationsToServer";
        Log.info(c, testName, "------- Add JDBC/Connection Pool application ------");
        ShrinkHelper.defaultDropinApp(computedMetricsServer, "testJDBCApp",
                "io.openliberty.microprofile.metrics.internal.monitor_fat.jdbc.servlet");
        Log.info(c, testName, "------- Added testJDBCApp to dropins -----");
        Log.info(c, testName, "------- Add RESTful application ------");
        ShrinkHelper.defaultDropinApp(computedMetricsServer, "testRESTApp",
                "io.openliberty.microprofile.metrics.internal.monitor_fat.rest");
        Log.info(c, testName, "------- Added testRESTapp to dropins -----");
    }

    /*
     * Tests if the computed base metrics are present in the "/metrics/vendor" output, when the server starts and 
     * verifies that the new computed value is actually a valid Number, and not listed as a NaN.
     */
    @Test
    public void testComputedBaseMetrics() throws Exception {
        String testName = "testComputedBaseMetrics";
        Log.info(c, testName, "Starting Testcase: " + testName);

        Log.info(c, testName, "------- Computed Base Metrics should be available ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), ALWAYS_EXPECTED_COMPUTED_METRICS);
    }

    /*
     * Tests if the computed servlet metrics are present in the "/metrics/vendor" output, after a
     * servlet is hit and verifies that the new computed value is actually a valid Number, and not listed as a NaN.
     */
    @Test
    public void testComputedServletMetrics() throws Exception {
        String testName = "testComputedServletMetrics";
        
        Log.info(c, testName, "Starting Testcase: " + testName);

        String[] expectedMetrics = new String[]{
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"io_openliberty_microprofile_metrics_internal_private_PrivateMetricsRESTProxyServlet\"}"};

        Log.info(c, testName, "------- Hitting the /metrics endpoint for the first time to initialize the Servlet metrics ------");
        getHttpsServlet("/metrics/vendor", computedMetricsServer);

        Log.info(c, testName, "------- Computed Servlet Metrics should be available for the /metrics servlet ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), expectedMetrics);
    }

    /*
     * Tests if the computed Connection Pool metrics are present in the "/metrics/vendor" output, after the corresponding 
     * JDBC application is hit and the corresponding data sources are created, and verifies that the new computed value is actually 
     * a valid Number, and not listed as a NaN. Also, checks if the corresponding computed Servlet metric is present in the endpoint
     * for the JDBC test application.
     */
    @Test
    public void testComputedConnectionPoolMetrics() throws Exception {
        String testName = "testComputedConnectionPoolMetrics";
        
        Log.info(c, testName, "Starting Testcase: " + testName);

        String[] expectedMetrics = new String[]{
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{datasource=\"jdbc_exampleDS2\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{datasource=\"jdbc_exampleDS2\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"testJDBCApp_io_openliberty_microprofile_metrics_internal_monitor_fat_jdbc_servlet_TestJDBCServlet\"}"};

        Log.info(c, testName, "------- Hitting the testJDBC application endpoint to initialize the Connection Pool metrics ------");
        getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create", computedMetricsServer);

        Log.info(c, testName, "------- Computed Connection pool and Servlet Metrics should be available for the testJDBCApp ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), expectedMetrics);
    }

    /*
     * Tests if the computed REST metrics present in the "//metrics/vendor" output, after a
     * REST application is hit and verifies that the new computed value is actually a valid Number, and not listed as a NaN. 
     * Also, checks if the corresponding computed Servlet metric is present in the endpoint for the REST test application.
     */
    @Test
    public void testComputedRESTMetrics() throws Exception {
        String testName = "testComputedRESTMetrics";
        
        Log.info(c, testName, "Starting Testcase: " + testName);

        String[] expectedMetrics = new String[]{
                "vendor_REST_request_elapsedTime_per_request_seconds{class=\"io.openliberty.microprofile.metrics.internal.monitor_fat.rest.TestRESTMetrics\",method=\"simpleGet\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"testRESTApp_io_openliberty_microprofile_metrics_internal_monitor_fat_rest_TestApplication\"}"};

        Log.info(c, testName, "------- Hitting the testREST application endpoint to initialize the REST metrics ------");
        getHttpServlet("/testRESTApp/test/get", computedMetricsServer);

        Log.info(c, testName, "------- Computed REST and Servlet Metrics should be available for the testRESTApp ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), expectedMetrics);
    }

    /*
     * Tests if all the computed metrics are present in "/metrics/vendor" output, and verifies that the new computed value is actually a valid Number,
     * and not listed as a NaN. 
     */
    @Test
    public void testAllComputedMetrics() throws Exception {
        String testName = "testAllComputedMetrics";
        
        Log.info(c, testName, "Starting Testcase: " + testName);

        String[] expectedMetrics = new String[]{
                "vendor_cpu_processCpuUtilization_percent",
                "vendor_memory_heapUtilization_percent",
                "vendor_gc_time_per_cycle_seconds{name=",
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{datasource=\"jdbc_exampleDS2\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{datasource=\"jdbc_exampleDS2\"}",
                "vendor_REST_request_elapsedTime_per_request_seconds{class=\"io.openliberty.microprofile.metrics.internal.monitor_fat.rest.TestRESTMetrics\",method=\"simpleGet\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"io_openliberty_microprofile_metrics_internal_private_PrivateMetricsRESTProxyServlet\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"testJDBCApp_io_openliberty_microprofile_metrics_internal_monitor_fat_jdbc_servlet_TestJDBCServlet\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"testRESTApp_io_openliberty_microprofile_metrics_internal_monitor_fat_rest_TestApplication\"}"};

        Log.info(c, testName,"------- Hitting the /metrics endpoint for the first time to initialize the Servlet metrics ------");
        getHttpsServlet("/metrics/vendor", computedMetricsServer);

        Log.info(c, testName, "------- Hitting the testJDBC application endpoint to initialize the Connection Pool metrics ------");
        getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create", computedMetricsServer);

        Log.info(c, testName, "------- Hitting the testREST application endpoint to initialize the REST metrics ------");
        getHttpServlet("/testRESTApp/test/get", computedMetricsServer);

        Log.info(c, testName, "------- Computed REST and Servlet Metrics should be available for the testRESTApp ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), expectedMetrics);
    }

    /*
     * Tests when dynamically stopping the deployed REST application, the computed REST and corresponding Servlet metrics should NOT be shown.
     */
    @Test
    public void testDynamicStopRESTApplication() throws Exception {
        String testName = "testDynamicStopRESTApplication";
        
        Log.info(c, testName, "Starting Testcase: " + testName);

        String[] computedMetrics = new String[]{
                "vendor_REST_request_elapsedTime_per_request_seconds{class=\"io.openliberty.microprofile.metrics.internal.monitor_fat.rest.TestRESTMetrics\",method=\"simpleGet\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"testRESTApp_io_openliberty_microprofile_metrics_internal_monitor_fat_rest_TestApplication\"}"};

        Log.info(c, testName, "------- Hitting the testREST application endpoint to initialize the REST metrics ------");
        getHttpServlet("/testRESTApp/test/get", computedMetricsServer);

        Log.info(c, testName, "------- Computed REST and Servlet Metrics should be available for the testRESTApp ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), computedMetrics);

        Log.info(c, testName, "------- Stopping test REST application... ------");
        computedMetricsServer.getApplicationMBean("testRESTApp").stop();
        Assert.assertNotNull("CWWKZ0009I was not found, application was stopped.", computedMetricsServer.waitForStringInLog("CWWKZ0009I", 30000));

        Log.info(c, testName, "------- Computed REST and Servlet Metrics should NOT be available in /metrics?scope=vendor ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), ALWAYS_EXPECTED_COMPUTED_METRICS);
        checkForUnexpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), computedMetrics);
    }

    /*
     * Tests when dynamically removing Connection Pool and WebContainer (Servlet) Monitoring components from the Filter attribute, 
     * the "/metrics/vendor" output should not show those metrics.
     */
    @Test
    public void testDynamicNoServletConnectionPoolInMonitorFilter() throws Exception {
        String testName = "testDynamicNoServletConnectionPoolInMonitorFilter";
        
        Log.info(c, testName, "Starting Testcase: " + testName);

        String[] computedMetrics = new String[]{
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{datasource=\"jdbc_exampleDS2\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{datasource=\"jdbc_exampleDS2\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"testJDBCApp_io_openliberty_microprofile_metrics_internal_monitor_fat_jdbc_servlet_TestJDBCServlet\"}"};

        Log.info(c, testName, "------- Hitting the testJDBC application endpoint to initialize the Connection Pool metrics ------");
        getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create", computedMetricsServer);

        Log.info(c, testName, "------- Computed Connection pool and Servlet Metrics should be available for the testJDBCApp ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), computedMetrics);

        Log.info(c, testName, "------- Set Monitor filter to ThreadPool and Session  ------");
        computedMetricsServer.setMarkToEndOfLog();
        computedMetricsServer.setServerConfigurationFile("server_monitorFilter5.xml");
        
        // Ensure the server configuration is completed.
        Assert.assertNotNull("CWWKG0017I was not found, server config did not update properly.", computedMetricsServer.waitForStringInLogUsingMark("CWWKG0017I"));
        
        Log.info(c, testName, "------- Hitting the testJDBC application endpoint again for the Connection Pool metrics ------");
        getHttpServlet("/testJDBCApp/testJDBCServlet?operation=select&city=city1&id=id1", computedMetricsServer);

        Log.info(c, testName,"------- The Connection Pool and corresponding Servlet computed metrics should NOT be available ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), ALWAYS_EXPECTED_COMPUTED_METRICS);
        checkForUnexpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), computedMetrics);
    }

    /*
     * Tests when only configuring the Connection Pool and WebContainer (Servlet) Monitoring components in the Filter attribute, 
     * the "/metrics/vendor" output should show both those metrics.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testOnlyServletConnectionPoolInMonitorFilter() throws Exception {
        String testName = "testOnlyServletConnectionPoolInMonitorFilter";
        
        Log.info(c, testName, "Starting Testcase: " + testName);

        String[] computedMetrics = new String[]{
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{datasource=\"jdbc_exampleDS2\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{datasource=\"jdbc_exampleDS2\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"testJDBCApp_io_openliberty_microprofile_metrics_internal_monitor_fat_jdbc_servlet_TestJDBCServlet\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"io_openliberty_microprofile_metrics_internal_private_PrivateMetricsRESTProxyServlet\"}"};
        
        Log.info(c, testName, "------- Stopping server to update server configuration. ------");
        if (computedMetricsServer != null && computedMetricsServer.isStarted()) {
            computedMetricsServer.stopServer("");
        }
        
        Log.info(c, testName, "------- Set Monitor filter to ConnectionPool and WebContainer ------");
        computedMetricsServer.setServerConfigurationFile("server_monitorFilter6.xml");

        Log.info(c, testName, "------- Starting server after server configuration update. ------");
        computedMetricsServer.startServer();
        waitForSecurityPrerequisites(computedMetricsServer, 60000);
        initialServerStart = false;
        computedMetricsServer.setMarkToEndOfLog(computedMetricsServer.getMostRecentTraceFile());

        Log.info(c, testName, "------- Hitting the testJDBC application endpoint to initialize the Connection Pool metrics ------");
        getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create", computedMetricsServer);

        Log.info(c, testName, "------- Hitting the /metrics endpoint for the first time to initialize the Servlet metrics ------");
        getHttpsServlet("/metrics/vendor", computedMetricsServer);

        Log.info(c, testName, "------- Computed Connection pool and Servlet Metrics should be available for the testJDBCApp ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), computedMetrics);
    }

    /*
     * Tests when only WebContainer (Servlet) is configured in the Filter attribute, the "/metrics/vendor" output should show only 
     * those metrics, not Connection Pool.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testOnlyServletInMonitorFilter() throws Exception {
        String testName = "testOnlyServletInMonitorFilter";
        
        Log.info(c, testName, "Starting Testcase: " + testName);

        String[] expectedMetrics = new String[]{
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"testJDBCApp_io_openliberty_microprofile_metrics_internal_monitor_fat_jdbc_servlet_TestJDBCServlet\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"io_openliberty_microprofile_metrics_internal_private_PrivateMetricsRESTProxyServlet\"}"};

        String[] unexpectedMetrics = new String[]{
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{datasource=\"jdbc_exampleDS2\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{datasource=\"jdbc_exampleDS2\"}"};
        
        Log.info(c, testName, "------- Stopping server to update server configuration. ------");
        if (computedMetricsServer != null && computedMetricsServer.isStarted()) {
            computedMetricsServer.stopServer("");
        }

        Log.info(c, testName, "------- Set Monitor filter to ConnectionPool and WebContainer ------");
        computedMetricsServer.setServerConfigurationFile("server_monitorFilter7.xml");

        Log.info(c, testName, "------- Starting server after server configuration update. ------");
        computedMetricsServer.startServer();
        waitForSecurityPrerequisites(computedMetricsServer, 60000);
        initialServerStart = false;
        computedMetricsServer.setMarkToEndOfLog(computedMetricsServer.getMostRecentTraceFile());

        Log.info(c, testName, "------- Hitting the testJDBC application endpoint to initialize the Connection Pool metrics ------");
        getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create", computedMetricsServer);

        Log.info(c, testName, "------- Hitting the /metrics endpoint for the first time to initialize the Servlet metrics ------");
        getHttpsServlet("/metrics/vendor", computedMetricsServer);

        Log.info(c, testName, "------- Only computed Servlet Metrics should be available for the testJDBCApp ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), expectedMetrics);

        Log.info(c, testName, "------- The Connection Pool metrics should NOT be available ------");
        checkForUnexpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), unexpectedMetrics);
    }

    /*
     * Tests when only ConnectionPool is configured in the Filter attribute, the "/metrics/vendor" output should 
     * show only those metrics, not Servlet.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testOnlyConnectionPoolInMonitorFilter() throws Exception {
        String testName = "testOnlyConnectionPoolInMonitorFilter";
        
        Log.info(c, testName, "Starting Testcase: " + testName);

        String[] expectedMetrics = new String[]{
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{datasource=\"jdbc_exampleDS2\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{datasource=\"jdbc_exampleDS2\"}"};

        String[] unexpectedMetrics = new String[]{
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"testJDBCApp_io_openliberty_microprofile_metrics_internal_monitor_fat_jdbc_servlet_TestJDBCServlet\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{servlet=\"io_openliberty_microprofile_metrics_internal_private_PrivateMetricsRESTProxyServlet\"}"};

        Log.info(c, testName, "------- Stopping server to update server configuration. ------");
        if (computedMetricsServer != null && computedMetricsServer.isStarted()) {
            computedMetricsServer.stopServer("");
        }
        
        Log.info(c, testName,
                "------- Set Monitor filter to ConnectionPool and WebContainer ------");
        computedMetricsServer.setServerConfigurationFile("server_monitorFilter8.xml");

        Log.info(c, testName, "------- Starting server after server configuration update. ------");
        computedMetricsServer.startServer();
        waitForSecurityPrerequisites(computedMetricsServer, 60000);
        initialServerStart = false;
        computedMetricsServer.setMarkToEndOfLog(computedMetricsServer.getMostRecentTraceFile());

        Log.info(c, testName, "------- Hitting the testJDBC application endpoint to initialize the Connection Pool metrics ------");
        getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create", computedMetricsServer);

        Log.info(c, testName, "------- Only computed Connection pool should be available for the testJDBCApp ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), expectedMetrics);

        Log.info(c, testName, "------- The Servlet metrics should NOT be available. ------");
        checkForUnexpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), unexpectedMetrics);
    }
    
    /*
     * Tests when the MP Config property "mp.metrics.appName" is set as an ENV variable,  the "/metrics/vendor" output should show all the computed metrics, 
     * with the correct tags, including the mp config property value.
     */
    @Test
    public void testMPConfigAppNameProperty() throws Exception {
        String testName = "testMPConfigAppNameProperty";
        
        Log.info(c, testName, "Starting Testcase: " + testName);

        String[] expectedMetrics = new String[]{
                "vendor_cpu_processCpuUtilization_percent{_app=\"myserver\"}",
                "vendor_memory_heapUtilization_percent{_app=\"myserver\"}",
                "vendor_gc_time_per_cycle_seconds{_app=\"myserver\",name=",
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{_app=\"myserver\",datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_inUseTime_per_usedConnection_seconds{_app=\"myserver\",datasource=\"jdbc_exampleDS2\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{_app=\"myserver\",datasource=\"jdbc_exampleDS1\"}",
                "vendor_connectionpool_waitTime_per_queuedRequest_seconds{_app=\"myserver\",datasource=\"jdbc_exampleDS2\"}",
                "vendor_REST_request_elapsedTime_per_request_seconds{_app=\"myserver\",class=\"io.openliberty.microprofile.metrics.internal.monitor_fat.rest.TestRESTMetrics\",method=\"simpleGet\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{_app=\"myserver\",servlet=\"io_openliberty_microprofile_metrics_internal_private_PrivateMetricsRESTProxyServlet\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{_app=\"myserver\",servlet=\"testJDBCApp_io_openliberty_microprofile_metrics_internal_monitor_fat_jdbc_servlet_TestJDBCServlet\"}",
                "vendor_servlet_request_elapsedTime_per_request_seconds{_app=\"myserver\",servlet=\"testRESTApp_io_openliberty_microprofile_metrics_internal_monitor_fat_rest_TestApplication\"}"};
        
        Log.info(c, testName, "------- Stopping server to update MP Config property. ------");
        if (computedMetricsServer != null && computedMetricsServer.isStarted()) {
            computedMetricsServer.stopServer("");
        }

        Log.info(c, testName, "------- Setting MP Config ENV variable : mp.metrics.appName=myserver ------");
        computedMetricsServer.addEnvVar("MP_METRICS_APPNAME", "myserver");
        
        Log.info(c, testName, "------- Starting server after MP Config update. ------");
        computedMetricsServer.startServer();
        waitForSecurityPrerequisites(computedMetricsServer, 60000);
        initialServerStart = false;
        computedMetricsServer.setMarkToEndOfLog(computedMetricsServer.getMostRecentTraceFile());

        Log.info(c, testName,"------- Hitting the /metrics endpoint for the first time to initialize the Servlet metrics ------");
        getHttpsServlet("/metrics/vendor", computedMetricsServer);

        Log.info(c, testName, "------- Hitting the testJDBC application endpoint to initialize the Connection Pool metrics ------");
        getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create", computedMetricsServer);

        Log.info(c, testName, "------- Hitting the testREST application endpoint to initialize the REST metrics ------");
        getHttpServlet("/testRESTApp/test/get", computedMetricsServer);

        Log.info(c, testName, "------- Make sure all the expected metrics have the \"_app=myserver\" tag ------");
        checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), expectedMetrics);
    }
    
    /*
     * Tests when dynamically stopping the deployed REST application, the computed REST and corresponding Servlet metrics should NOT be shown, when the MP Config appName property is set.
     */
   @Test
   @Mode(TestMode.FULL)
   public void testDynamicApplicationStopWithMpConfigPropSet() throws Exception {
       String testName = "testDynamicApplicationStopWithMpConfigPropSet";
       
       Log.info(c, testName, "Starting Testcase: " + testName);

       String[] expectedMetrics = new String[]{
               "vendor_cpu_processCpuUtilization_percent{_app=\"myserver\"}",
               "vendor_memory_heapUtilization_percent{_app=\"myserver\"}",
               "vendor_gc_time_per_cycle_seconds{_app=\"myserver\",name=",
               "vendor_REST_request_elapsedTime_per_request_seconds{_app=\"myserver\",class=\"io.openliberty.microprofile.metrics.internal.monitor_fat.rest.TestRESTMetrics\",method=\"simpleGet\"}",
               "vendor_servlet_request_elapsedTime_per_request_seconds{_app=\"myserver\",servlet=\"io_openliberty_microprofile_metrics_internal_private_PrivateMetricsRESTProxyServlet\"}",
               "vendor_servlet_request_elapsedTime_per_request_seconds{_app=\"myserver\",servlet=\"testRESTApp_io_openliberty_microprofile_metrics_internal_monitor_fat_rest_TestApplication\"}"};
       
       String[] unexpectedMetrics = new String[]{
               "vendor_REST_request_elapsedTime_per_request_seconds{_app=\"myserver\",class=\"io.openliberty.microprofile.metrics.internal.monitor_fat.rest.TestRESTMetrics\",method=\"simpleGet\"}",
               "vendor_servlet_request_elapsedTime_per_request_seconds{_app=\"myserver\",servlet=\"testRESTApp_io_openliberty_microprofile_metrics_internal_monitor_fat_rest_TestApplication\"}"};
       
       Log.info(c, testName, "------- Stopping server to update MP Config property. ------");
       if (computedMetricsServer != null && computedMetricsServer.isStarted()) {
           computedMetricsServer.stopServer("");
       }

       Log.info(c, testName, "------- Setting MP Config ENV variable : mp.metrics.appName=myserver ------");
       computedMetricsServer.addEnvVar("MP_METRICS_APPNAME", "myserver");
       
       Log.info(c, testName, "------- Starting server after MP Config update. ------");
       computedMetricsServer.startServer();
       waitForSecurityPrerequisites(computedMetricsServer, 60000);
       initialServerStart = false;
       computedMetricsServer.setMarkToEndOfLog(computedMetricsServer.getMostRecentTraceFile());

       Log.info(c, testName,"------- Hitting the /metrics endpoint for the first time to initialize the Servlet metrics ------");
       getHttpsServlet("/metrics/vendor", computedMetricsServer);

       Log.info(c, testName, "------- Hitting the testREST application endpoint to initialize the REST metrics ------");
       getHttpServlet("/testRESTApp/test/get", computedMetricsServer);
       
       Log.info(c, testName, "------- Wait for the Servlet MBean for the REST test app to get registered in MonitorMetrics, before hitting the /metrics endpoint ------");
       String mbeanRegStr = computedMetricsServer.waitForStringInLogUsingMark("type=ServletStats,name=testRESTApp.io.openliberty.microprofile.metrics.internal.monitor_fat.rest.TestApplication is registered", 60000, computedMetricsServer.getMostRecentTraceFile());

       Log.info(c, testName, "------- Found Servlet MBean for the REST test app successfully registered trace : " + mbeanRegStr);
       
       Log.info(c, testName, "------- Make sure all the expected metrics have the \"_app=myserver\" tag ------");
       checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), expectedMetrics);
       
       Log.info(c, testName, "------- Stopping test application... ------");
       computedMetricsServer.getApplicationMBean("testRESTApp").stop();
       Assert.assertNotNull("CWWKZ0009I was not found, application was stopped.", computedMetricsServer.waitForStringInLog("CWWKZ0009I", 30000));
       
       Log.info(c, testName, "------- The REST and Servlet metrics for the stopped application should NOT be available. ------");
       checkForExpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), ALWAYS_EXPECTED_COMPUTED_METRICS_WITH_APPNAME);
       checkForUnexpectedStrings(getHttpsServlet("/metrics/vendor", computedMetricsServer), unexpectedMetrics);
   }

    private static void trustAll() throws Exception {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }}, new SecureRandom());
            SSLContext.setDefault(sslContext);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            Log.error(c, "trustAll", e);
        }
    }

    private void waitForSecurityPrerequisites(LibertyServer server, int timeout) {
        // We only need to wait for LTPA keys if this is the first time using this server.
       if (initialServerStart) {
            // Need to ensure LTPA keys and configuration are created before hitting a secure endpoint.
            Assert.assertNotNull("LTPA keys are not created within timeout period of " + timeout + "ms.", server.waitForStringInLog("CWWKS4104A", timeout));
            Assert.assertNotNull("LTPA configuration is not ready within timeout period of " + timeout + "ms.", server.waitForStringInLog("CWWKS4105I", timeout));
       }

        // Ensure defaultHttpEndpoint-ssl TCP Channel is started
        Assert.assertNotNull("TCP Channel defaultHttpEndpoint-ssl has not started (CWWKO0219I not found)", server.waitForStringInLog("CWWKO0219I.*defaultHttpEndpoint-ssl", timeout));
    }

    private String getHttpServlet(String servletPath, LibertyServer server) throws Exception {
        HttpURLConnection con = null;
        try {
            String sURL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + servletPath;
            URL checkerServletURL = new URL(sURL);
            con = (HttpURLConnection) checkerServletURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line).append(sep);
            }
            Log.info(c, "getHttpServlet", sURL);
            return lines.toString();
        } finally {
            if (con != null)
                con.disconnect();
        }
    }

    private String getHttpsServlet(String servletPath, LibertyServer server) throws Exception {
        HttpsURLConnection con = null;
        try {
            String sURL = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + servletPath;
            URL checkerServletURL = new URL(sURL);
            con = (HttpsURLConnection) checkerServletURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
            String authorization = "Basic " + Base64.getEncoder().encodeToString(
                    ("theUser:thePassword").getBytes(StandardCharsets.UTF_8)); // Java
            con.setRequestProperty("Authorization", authorization);
            con.setRequestMethod("GET");

            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                if (!line.startsWith("#"))
                    lines.append(line).append(sep);
            }
            Log.info(c, "getHttpsServlet", sURL);
            return lines.toString();
        } finally {
            if (con != null)
                con.disconnect();
        }
    }

    private void checkForExpectedStrings(String metricsText, String[] expectedStrings) {
        Log.info(c, "checkForExpectedStrings", "metricsText:\n" + metricsText);
        String sep = System.getProperty("line.separator");
        String[] metricArray = metricsText.split(sep);

        for (String m : expectedStrings) {
            if (!metricsText.contains(m)) {
                Log.info(c, "checkForExpectedStrings", "Failed:\n" + metricsText);
                Assert.fail("Did not contain expected string: " + m);
            }

            if (!isComputedMetricANumber(metricArray, m)) {
                Assert.fail("The computed metric " + m + " is not numeric.");
            }
        }
    }

    private void checkForUnexpectedStrings(String metricsText, String[] unexpectedStrings) {
        Log.info(c, "checkForUnexpectedStrings", "metricsText:\n" + metricsText);

        for (String m : unexpectedStrings) {
            if (metricsText.contains(m)) {
                Log.info(c, "checkForExpectedStrings", "Failed:\n" + metricsText);
                Assert.fail("Contains unexpected string: " + m);
            }
        }
    }

    private boolean isComputedMetricANumber(String[] metricsArray, String computedMetric) {
        for (String metric : metricsArray) {
            if (metric.contains(computedMetric)) {
                Log.info(c, "isComputedMetricANumber","Found Computed Metric : " + metric);
                // Verifying if  the metricValue from the metricName, is a Number, and not NaN.
                return verifyIfMetricValueIsANumber(metric);
            }
        }
        return false;
    }

    private boolean verifyIfMetricValueIsANumber(String metricString) {
        String metricValue = null;
        if (metricString.contains("} ") ) {
            // If the metrics contain tags.
            metricValue = metricString.substring(metricString.indexOf("} ") + 2).trim();
        }
        else {
            // Pattern to match positive numeric values, that may contain decimals or exponentials.
            Pattern pattern = Pattern.compile("\\d+(\\.\\d*)?([eE][-+]?\\d+)?");
            Matcher matcher = pattern.matcher(metricString);
            
            while (matcher.find()) {
                metricValue = matcher.group();
            }
        }
        Log.info(c, "verifyIfMetricValueIsANumber", "The metric value is : " + metricValue);
        try {
            Double.parseDouble(metricValue);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
