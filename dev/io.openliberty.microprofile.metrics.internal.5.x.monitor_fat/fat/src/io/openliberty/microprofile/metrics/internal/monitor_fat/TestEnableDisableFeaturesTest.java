/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@AllowedFFDC("javax.management.InstanceNotFoundException")
public class TestEnableDisableFeaturesTest {

    private static Class<?> c = TestEnableDisableFeaturesTest.class;

    @Server("TestEDF2")
    public static LibertyServer serverEDF2;

    @Server("TestEDF3")
    public static LibertyServer serverEDF3;

    @Server("TestEDF4")
    public static LibertyServer serverEDF4;

    @Server("TestEDF5")
    public static LibertyServer serverEDF5;

    @Server("TestEDF6")
    public static LibertyServer serverEDF6;

    @Server("TestEDF8")
    public static LibertyServer serverEDF8;

    @Server("TestEDF9")
    public static LibertyServer serverEDF9;

    @Server("TestEDF10")
    public static LibertyServer serverEDF10;

    @Server("TestEDF11")
    public static LibertyServer serverEDF11;

    private static LibertyServer currentServ;

    private static final String SERVER_NAME = "MetricsMonitorServer";
    
    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(FeatureReplacementAction.ALL_SERVERS,
                                                             MicroProfileActions.MP70_EE10, //MP61 is the same features here
                                                             MicroProfileActions.MP70_EE11,
                                                             MicroProfileActions.MP60);
    
    private static boolean serverEDF6FirstUse = true;

    @BeforeClass
    public static void setUp() throws Exception {
        trustAll();
        add3Apps(serverEDF6);
        add3Apps(serverEDF8);
        add3Apps(serverEDF9);
        add3Apps(serverEDF10);
        add2Apps(serverEDF11);
        add2Apps(serverEDF5);
    }

    private static String retrieveMPMVersion(LibertyServer server) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        String returnMpMetricsFeature = null;
        for (String feature : features) {
            if (feature.contains("mpMetrics-")) {
                returnMpMetricsFeature = feature.trim();
                break;
            }
        }
        return returnMpMetricsFeature;
    }

    private static void trustAll() throws Exception {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            } }, new SecureRandom());
            SSLContext.setDefault(sslContext);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            Log.error(c, "trustAll", e);
        }
    }

    private static void add3Apps(LibertyServer server) throws Exception {
        String testName = "add3Apps to " + server.toString();
        Log.info(c, testName, "------- Add session application ------");
        ShrinkHelper.defaultDropinApp(server, "testSessionApp",
                "io.openliberty.microprofile.metrics.internal.monitor_fat.session.servlet");
        Log.info(c, testName, "------- added testSessionApp to dropins -----");
        Log.info(c, testName, "------- Add JDBC application ------");
        ShrinkHelper.defaultDropinApp(server, "testJDBCApp",
                "io.openliberty.microprofile.metrics.internal.monitor_fat.jdbc.servlet");
        Log.info(c, testName, "------- added testJDBCApp to dropins -----");
        Log.info(c, testName, "------- Add jax-ws endpoint application ------");
        ShrinkHelper.defaultDropinApp(server, "testJaxWsApp", "io.openliberty.microprofile.metrics.internal.monitor_fat.jaxws",
                "io.openliberty.microprofile.metrics.internal.monitor_fat.jaxws.client");
        Log.info(c, testName, "------- added testJaxWsApp to dropins -----");
    }

    private static void add2Apps(LibertyServer server) throws Exception {
        String testName = "add2Apps to " + server.toString();
        Log.info(c, testName, "------- Add session application ------");
        ShrinkHelper.defaultDropinApp(server, "testSessionApp",
                "io.openliberty.microprofile.metrics.internal.monitor_fat.session.servlet");
        Log.info(c, testName, "------- added testSessionApp to dropins -----");
        Log.info(c, testName, "------- Add JDBC application ------");
        ShrinkHelper.defaultDropinApp(server, "testJDBCApp",
                "io.openliberty.microprofile.metrics.internal.monitor_fat.jdbc.servlet");
        Log.info(c, testName, "------- added testJDBCApp to dropins -----");
    }

    
    @Test
    public void testEDF2() throws Exception {
        currentServ = serverEDF2;
        String metricFeature = retrieveMPMVersion(currentServ);
        String testName = "testEDF2";
        Log.info(c, testName,
                "------- Enable " + metricFeature + "threadpool metrics should be available ------");
        serverEDF2.startServer();
        waitForSecurityPrerequisites(serverEDF2, 60000);
        serverEDF2.setMarkToEndOfLog(serverEDF2.getMostRecentTraceFile());
        Log.info(c, testName, "------- threadpool metrics should be available ------");
        getHttpsServlet("/metrics?scope=vendor", serverEDF2);
        Log.info(c, testName, "------- servlet metrics should be available ------");
        Assert.assertNotNull("CWWKO0219I NOT FOUND",
                serverEDF2.waitForStringInTraceUsingMark("Monitoring MXBean WebSphere:type=ServletStats"));
        checkStrings(getHttpsServlet("/metrics?scope=vendor", serverEDF2), new String[] {
                "threadpool_activeThreads{mp_scope=\"vendor\",pool=\"Default_Executor\",}",
                "threadpool_size{mp_scope=\"vendor\",pool=\"Default_Executor\",}",
                "servlet_responseTime_total_seconds{mp_scope=\"vendor\",servlet=\"io_openliberty_microprofile_metrics_5_0_private_internal_PrivateMetricsRESTProxyServlet\",}",
                "servlet_request_total{mp_scope=\"vendor\",servlet=\"io_openliberty_microprofile_metrics_5_0_private_internal_PrivateMetricsRESTProxyServlet\",}" },
                new String[] {});
    }
    
    @Test
    public void testEDF3() throws Exception {
        currentServ = serverEDF3;
        String testName = "testEDF3";
        serverEDF3.startServer();
        waitForSecurityPrerequisites(serverEDF3, 60000);
        Log.info(c, testName, "------- Add session application and run session servlet ------");
        ShrinkHelper.defaultDropinApp(serverEDF3, "testSessionApp",
                "io.openliberty.microprofile.metrics.internal.monitor_fat.session.servlet");
        Log.info(c, testName, "------- added testSessionApp to dropins -----");
        checkStrings(getHttpServlet("/testSessionApp/testSessionServlet", serverEDF3), new String[] { "Session id:" },
                new String[] {});
        Log.info(c, testName, "------- session metrics should be available ------");
        checkStrings(getHttpsServlet("/metrics?scope=vendor", serverEDF3),
                new String[] { "session_create_total{appname=\"default_host_testSessionApp\",mp_scope=\"vendor\",}",
                        "session_liveSessions{appname=\"default_host_testSessionApp\",mp_scope=\"vendor\",}",
                        "session_activeSessions{appname=\"default_host_testSessionApp\",mp_scope=\"vendor\",}",
                        "session_invalidated_total{appname=\"default_host_testSessionApp\",mp_scope=\"vendor\",}",
                        "session_invalidatedbyTimeout_total{appname=\"default_host_testSessionApp\",mp_scope=\"vendor\",}" },
                new String[] {});
    }
   
    @Test
    public void testEDF4() throws Exception {
        currentServ = serverEDF4;
        String testName = "testEDF4";
        serverEDF4.startServer();
        waitForSecurityPrerequisites(serverEDF4, 60000);
        Log.info(c, testName, "------- Add session application ------");
        ShrinkHelper.defaultDropinApp(serverEDF4, "testSessionApp",
                "io.openliberty.microprofile.metrics.internal.monitor_fat.session.servlet");
        Log.info(c, testName, "------- added testSessionApp to dropins -----");

        Log.info(c, testName, "------- Add JDBC application and run JDBC servlet ------");
        ShrinkHelper.defaultDropinApp(serverEDF4, "testJDBCApp",
                "io.openliberty.microprofile.metrics.internal.monitor_fat.jdbc.servlet");
        Log.info(c, testName, "------- added testJDBCApp to dropins -----");
        checkStrings(getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create", serverEDF4),
                new String[] { "sql: create table cities" }, new String[] {});
        Log.info(c, testName, "------- connectionpool metrics should be available ------");
        checkStrings(getHttpsServlet("/metrics?scope=vendor", serverEDF4),
                new String[] { "connectionpool_connectionHandles{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_freeConnections{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_destroy_total{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_create_total{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_managedConnections{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_waitTime_total_seconds{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_inUseTime_total_seconds{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_queuedRequests_total{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_usedConnections_total{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_connectionHandles{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_freeConnections{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_destroy_total{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_create_total{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_managedConnections{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_waitTime_total_seconds{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_inUseTime_total_seconds{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_queuedRequests_total{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_usedConnections_total{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}" },
                new String[] {});
        
        currentServ.setMarkToEndOfLog();
        // FAT updated to check that connectionpool metric remains after unloading
        // application.
        boolean res = currentServ.removeDropinsApplications("testJDBCApp.war");
        Assert.assertTrue("TestJDBCApp.war was not removed", res);

        currentServ.waitForStringInLog(".*CWWKZ0009I: The application testJDBCApp has stopped successfully.*");
        Log.info(c, testName, "------- Removed JDBC application ------");
        checkStrings(getHttpsServlet("/metrics?scope=vendor", serverEDF4),
                new String[] { "connectionpool_connectionHandles{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_freeConnections{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_destroy_total{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_create_total{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_managedConnections{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_waitTime_total_seconds{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_inUseTime_total_seconds{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_queuedRequests_total{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_usedConnections_total{datasource=\"jdbc_exampleDS1\",mp_scope=\"vendor\",}",
                        "connectionpool_connectionHandles{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_freeConnections{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_destroy_total{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_create_total{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_managedConnections{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_waitTime_total_seconds{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_inUseTime_total_seconds{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_queuedRequests_total{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}",
                        "connectionpool_usedConnections_total{datasource=\"jdbc_exampleDS2\",mp_scope=\"vendor\",}" },
                new String[] {});
    }

    @Test
    public void testEDF5() throws Exception {
        currentServ = serverEDF5;
        String testName = "testEDF5";
        serverEDF5.startServer();
        waitForSecurityPrerequisites(serverEDF5, 60000);
        Log.info(c, testName, "------- Add jax-ws endpoint application and run jax-ws client servlet ------");
        ShrinkHelper.defaultDropinApp(serverEDF5, "testJaxWsApp", "io.openliberty.microprofile.metrics.internal.monitor_fat.jaxws",
                "io.openliberty.microprofile.metrics.internal.monitor_fat.jaxws.client");
        Log.info(c, testName, "------- added testJaxWsApp to dropins -----");
        Assert.assertNotNull("Application testJaxWsApp not started",
                serverEDF5.waitForStringInLogUsingMark("CWWKZ0001I.*testJaxWsApp.*"));
        checkStrings(getHttpServlet("/testJaxWsApp/SimpleStubClientServlet", serverEDF5), new String[] { "Pass" },
                new String[] {});
        Log.info(c, testName, "------- jax-ws metrics should be available ------");
        
        System.out.println(getHttpsServlet("/metrics?scope=vendor", serverEDF5));
        //jaxws_server_uncheckedApplicationFaults_total{endpoint="jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort",mp_scope="vendor",}
        //jaxws_client_uncheckedApplicationFaults_total{endpoint="jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort",mp_scope="vendor",}
        checkStrings(getHttpsServlet("/metrics?scope=vendor", serverEDF5), new String[] {
                "jaxws_client_checkedApplicationFaults_total{endpoint=\"jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort\",mp_scope=\"vendor\",}",
                "jaxws_client_runtimeFaults_total{endpoint=\"jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort\",mp_scope=\"vendor\",}",
                "jaxws_client_responseTime_total_seconds{endpoint=\"jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort\",mp_scope=\"vendor\",}",
                "jaxws_client_invocations_total{endpoint=\"jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort\",mp_scope=\"vendor\",}",
                "jaxws_client_uncheckedApplicationFaults_total{endpoint=\"jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort\",mp_scope=\"vendor\",}",
                "jaxws_client_logicalRuntimeFaults_total{endpoint=\"jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort\",mp_scope=\"vendor\",}",
                "jaxws_server_checkedApplicationFaults_total{endpoint=\"jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort\",mp_scope=\"vendor\",}",
                "jaxws_server_runtimeFaults_total{endpoint=\"jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort\",mp_scope=\"vendor\",}",
                "jaxws_server_responseTime_total_seconds{endpoint=\"jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort\",mp_scope=\"vendor\",}",
                "jaxws_server_invocations_total{endpoint=\"jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort\",mp_scope=\"vendor\",}",
                "jaxws_server_uncheckedApplicationFaults_total{endpoint=\"jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort\",mp_scope=\"vendor\",}",
                "jaxws_server_logicalRuntimeFaults_total{endpoint=\"jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io..SimpleEchoService.SimpleEchoPort\",mp_scope=\"vendor\",}" },
                new String[] {});
    }


    
    @Test
    public void testEDF6() throws Exception {
        currentServ = serverEDF6;
        String testName = "testEDF6";
        serverEDF6.startServer();

        if (serverEDF6FirstUse) {
            // We only need to wait for LTPA keys if this is the first time using this
            // server
            waitForSecurityPrerequisites(serverEDF6, 60000);
        } else {
            Assert.assertNotNull("TCP Channel defaultHttpEndpoint-ssl has not started (CWWKO0219I not found)",
                    serverEDF6.waitForStringInLog("CWWKO0219I.*defaultHttpEndpoint-ssl", 60000));
        }
        serverEDF6FirstUse = false;

        Log.info(c, testName, "------- Monitor filter ThreadPool and WebContainer  ------");
        serverEDF6.setServerConfigurationFile("server_monitorFilter1.xml");
        Assert.assertNotNull("CWWKG0017I NOT FOUND", serverEDF6.waitForStringInLogUsingMark("CWWKG0017I"));
        Log.info(c, testName, "------- Only threadpool and servlet metrics should be available ------");
        getHttpsServlet("/metrics", serverEDF6); // Initialize the metrics endpoint first, to load the mpMetrics servlet
                                                 // metrics.
        checkStrings(getHttpsServlet("/metrics?scope=vendor", serverEDF6),
                new String[] { "threadpool_", "servlet_" },
                new String[] { "session_", "connectionpool_" });
    }

    // This is not a copy/paste error, serverEDF6 can be reused here
    @Test
    public void testEDF7() throws Exception {
        currentServ = serverEDF6;
        String testName = "testEDF7";
        serverEDF6.startServer();

        if (serverEDF6FirstUse) {
            // We only need to wait for LTPA keys if this is the first time using this
            // server
            waitForSecurityPrerequisites(serverEDF6, 60000);
        } else {
            Assert.assertNotNull("TCP Channel defaultHttpEndpoint-ssl has not started (CWWKO0219I not found)",
                    serverEDF6.waitForStringInLog("CWWKO0219I.*defaultHttpEndpoint-ssl", 60000));
        }
        serverEDF6FirstUse = false;

        Log.info(c, testName, "------- Monitor filter WebContainer and Session ------");
        serverEDF6.setMarkToEndOfLog();
        serverEDF6.setServerConfigurationFile("server_monitorFilter2.xml");
        Assert.assertNotNull("CWWKG0017I NOT FOUND", serverEDF6.waitForStringInLogUsingMark("CWWKG0017I"));
        checkStrings(getHttpServlet("/testSessionApp/testSessionServlet", serverEDF6), new String[] { "Session id:" },
                new String[] {});
        Log.info(c, testName, "------- Only servlet and session metrics should be available ------");
        checkStrings(getHttpsServlet("/metrics?scope=vendor", serverEDF6),
                new String[] { "servlet_", "session_" },
                new String[] { "threadpool_", "connectionpool_" });
    }

    @Test
    public void testEDF8() throws Exception {
        currentServ = serverEDF8;
        String testName = "testEDF8";
        serverEDF8.startServer();
        waitForSecurityPrerequisites(serverEDF8, 60000);
        checkStrings(getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create", serverEDF8),
                new String[] { "sql: create table cities" }, new String[] {});
        Log.info(c, testName, "------- Monitor filter Session and ConnectionPool ------");
        serverEDF8.setServerConfigurationFile("server_monitorFilter3.xml");
        Assert.assertNotNull("CWWKG0017I NOT FOUND", serverEDF8.waitForStringInLogUsingMark("CWWKG0017I"));
        checkStrings(getHttpServlet("/testJDBCApp/testJDBCServlet?operation=select&city=city1&id=id1", serverEDF8),
                new String[] { "sql: select" }, new String[] {});
        Log.info(c, testName, "------- Only session and connectionpool metrics should be available ------");
        checkStrings(getHttpsServlet("/metrics?scope=vendor", serverEDF8),
                new String[] { "session_", "connectionpool_" },
                new String[] { "servlet_", "threadpool_" });
    }

    @Test
    public void testEDF9() throws Exception {
        currentServ = serverEDF9;
        String testName = "testEDF9";
        serverEDF9.startServer();
        waitForSecurityPrerequisites(serverEDF9, 60000);
        checkStrings(getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create", serverEDF9),
                new String[] { "sql: create table cities" }, new String[] {});
        Log.info(c, testName, "------- Monitor filter ThreadPool, WebContainer, Session and ConnectionPool ------");
        serverEDF9.setMarkToEndOfLog();
        serverEDF9.setServerConfigurationFile("server_monitorFilter4.xml");
        Assert.assertNotNull("CWWKG0017I NOT FOUND", serverEDF9.waitForStringInLogUsingMark("CWWKG0017I"));
        checkStrings(getHttpServlet("/testSessionApp/testSessionServlet", serverEDF9), new String[] { "Session id:" },
                new String[] {});
        checkStrings(getHttpServlet("/testJDBCApp/testJDBCServlet?operation=select&city=city1&id=id1", serverEDF9),
                new String[] { "sql: select" }, new String[] {});
        Log.info(c, testName, "------- all four vendor metrics should be available ------");
        checkStrings(getHttpsServlet("/metrics?scope=vendor", serverEDF9),
                new String[] { "threadpool_", "servlet_", "session_", "connectionpool_" },
                new String[] {});
    }

    @Test
    public void testEDF10() throws Exception {
        currentServ = serverEDF10;
        String testName = "testEDF10";
        serverEDF10.startServer();
        waitForSecurityPrerequisites(serverEDF10, 60000);
        Log.info(c, testName, "------- Remove JAX-WS application ------");
        boolean rc1 = serverEDF10.removeAndStopDropinsApplications("testJaxWsApp.war");
        Log.info(c, testName,
                "------- " + (rc1 ? "successfully removed" : "failed to remove") + " JAX-WS application ------");
        Assert.assertNotNull("CWWKT0017I NOT FOUND",
                serverEDF10.waitForStringInLogUsingMark("CWWKT0017I.*testJaxWsApp.*"));
        serverEDF10.setMarkToEndOfLog();
        serverEDF10.setServerConfigurationFile("server_noJaxWs.xml");
        Assert.assertNotNull("CWWKF0008I NOT FOUND", serverEDF10.waitForStringInLogUsingMark("CWWKF0008I"));
        Assert.assertNotNull("testJDBCApp not loaded", serverEDF10.waitForStringInLog("CWWKT0016I.*testJDBCApp.*"));
        Assert.assertNotNull("testSessionApp not loaded",
                serverEDF10.waitForStringInLog("CWWKT0016I.*testSessionApp.*"));
        Log.info(c, testName, "------- jax-ws metrics should not be available ------");
        checkStrings(getHttpsServlet("/metrics?scope=vendor", serverEDF10), new String[] { "mp_scope=\"vendor\"" },
                new String[] { "jaxws_client", "jaxws_server" });
    }
    //,mp_scope="vendor"
    @Test
    public void testEDF11() throws Exception {
        currentServ = serverEDF11;
        String testName = "testEDF11";
        serverEDF11.startServer();
        waitForSecurityPrerequisites(serverEDF11, 60000);
        Log.info(c, testName, "------- Remove JDBC application ------");
        boolean rc2 = serverEDF11.removeAndStopDropinsApplications("testJDBCApp.war");
        Log.info(c, testName,
                "------- " + (rc2 ? "successfully removed" : "failed to remove") + " JDBC application ------");
        serverEDF11.setMarkToEndOfLog();
        serverEDF11.setServerConfigurationFile("server_noJDBC.xml");
        Assert.assertNotNull("CWWKF0008I NOT FOUND", serverEDF11.waitForStringInLogUsingMark("CWWKF0008I"));
    	//This message ID indicates the Pub/Priv Rest Handler has been initialized
    	Assert.assertNotNull("SRVE0242I NOT FOUND",serverEDF11.waitForStringInLogUsingMark("SRVE0242I.*(\\[/metrics\\])")); 
        Log.info(c, testName, "------- connectionpool metrics should not be available ------");
        checkStrings(getHttpsServlet("/metrics?scope=vendor", serverEDF11), new String[] { "mp_scope=\"vendor\""},
                new String[] { "connectionpool_", "servlet_", "{mp_scope=\"vendor\",servlet=\"testJDBCApp\"}" });
    }

    private void waitForSecurityPrerequisites(LibertyServer server, int timeout) {
        // Need to ensure LTPA keys and configuration are created before hitting a
        // secure endpoint
        Assert.assertNotNull("LTPA keys are not created within timeout period of " + timeout + "ms.",
                server.waitForStringInLog("CWWKS4104A", timeout));
        Assert.assertNotNull("LTPA configuration is not ready within timeout period of " + timeout + "ms.",
                server.waitForStringInLog("CWWKS4105I", timeout));

        // Ensure defaultHttpEndpoint-ssl TCP Channel is started
        Assert.assertNotNull("TCP Channel defaultHttpEndpoint-ssl has not started (CWWKO0219I not found)",
                server.waitForStringInLog("CWWKO0219I.*defaultHttpEndpoint-ssl", timeout));
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
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

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
            String authorization = "Basic "
                    + Base64.getEncoder().encodeToString(("theUser:thePassword").getBytes(StandardCharsets.UTF_8)); // Java
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

    private void checkStrings(String metricsText, String[] expectedString, String[] unexpectedString) {
        for (String m : expectedString) {
            if (!metricsText.contains(m)) {
                Log.info(c, "checkStrings", "Failed:\n" + metricsText);
                Assert.fail("Did not contain string: " + m);
            }
        }
        for (String m : unexpectedString) {
            if (metricsText.contains(m)) {
                Log.info(c, "checkStrings", "Failed:\n" + metricsText);
                Assert.fail("Contained string: " + m);
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        if (currentServ != null && currentServ.isStarted()) {
            currentServ.stopServer("CWWKS4000E", "CWWKZ0014W", "CWNEN0049W");
            currentServ.removeAllInstalledAppsForValidation();
        }
    }
}
