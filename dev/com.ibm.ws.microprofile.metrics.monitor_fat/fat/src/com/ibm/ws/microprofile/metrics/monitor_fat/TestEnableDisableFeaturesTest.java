/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.monitor_fat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class TestEnableDisableFeaturesTest {
	
    private static Class<?> c = TestEnableDisableFeaturesTest.class;

    @Server("TestEDF1")
    public static LibertyServer serverEDF1;
    
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
    
    @BeforeClass
    public static void setUp() throws Exception {
    	trustAll();
    }
    
    private static void trustAll() throws Exception {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(
            	null, 
            	new TrustManager[] {
            		new X509TrustManager() {
            	    	@Override
            	        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

            	    	@Override
            	        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

            	        public X509Certificate[] getAcceptedIssuers() {
            	            return null;
            	        }
            		}
            	}, 
            	new SecureRandom());
	        SSLContext.setDefault(sslContext);
	        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (Exception e) {
			Log.error(c, "trustAll", e);
		}
    }
    
    public void tearDown(LibertyServer server) throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKS4000E", "CWWKZ0014W");
            server.removeAllInstalledAppsForValidation();
        }
    }
    
    @BeforeClass
    public static void setUpEDF6() throws Exception {
    	
    	String testName = "testEDF6";
    	Log.info(c, testName, "------- Add session application ------");
       	ShrinkHelper.defaultDropinApp(serverEDF6, "testSessionApp", "com.ibm.ws.microprofile.metrics.monitor_fat.session.servlet");
       	Log.info(c, testName, "------- added testSessionApp to dropins -----");
       	Log.info(c, testName, "------- Add JDBC application ------");
       	ShrinkHelper.defaultDropinApp(serverEDF6, "testJDBCApp", "com.ibm.ws.microprofile.metrics.monitor_fat.jdbc.servlet");
       	Log.info(c, testName, "------- added testJDBCApp to dropins -----");
    	Log.info(c, testName, "------- Add jax-ws endpoint application ------");
       	ShrinkHelper.defaultDropinApp(serverEDF6, "testJaxWsApp", "com.ibm.ws.microprofile.metrics.monitor_fat.jaxws","com.ibm.ws.microprofile.metrics.monitor_fat.jaxws.client");
       	Log.info(c, testName, "------- added testJaxWsApp to dropins -----");
    	
    }
    
    @Test
    public void testEDF1() throws Exception {

    	String testName = "testEDF1";
        Log.info(c, testName, "------- No monitor-1.0: no vendor metrics should be available ------");
        serverEDF1.setServerConfigurationFile("server_mpMetric11.xml");
        serverEDF1.startServer();
        Assert.assertNotNull("Web application /metrics not loaded", serverEDF1.waitForStringInLog("CWWKT0016I: Web application available \\(default_host\\): http:\\/\\/.*:.*\\/metrics\\/"));
        Assert.assertNotNull("CWWKO0219I NOT FOUND",serverEDF1.waitForStringInLogUsingMark("defaultHttpEndpoint-ssl"));
        Assert.assertNotNull("SRVE9103I NOT FOUND",serverEDF1.waitForStringInLogUsingMark("SRVE9103I"));
        Log.info(c, testName, "------- server started -----");
        checkStrings(getHttpsServlet("/metrics", serverEDF1), 
           	new String[] { "base:" }, 
           	new String[] { "vendor:" });
        tearDown(serverEDF1);
    }
    
    @Test 
    public void testEDF2() throws Exception {
    	
    	String testName = "testEDF2";
    	Log.info(c, testName, "------- Enable mpMetrics-1.1 and monitor-1.0: threadpool metrics should be available ------");
    	serverEDF2.startServer();
    	serverEDF2.setServerConfigurationFile("server_monitor.xml");
        String logMsg = serverEDF2.waitForStringInLogUsingMark("CWPMI2001I");
        Log.info(c, testName, logMsg);
    	Assert.assertNotNull("No CWPMI2001I was found.", logMsg);
    	serverEDF2.setMarkToEndOfLog(serverEDF2.getMostRecentTraceFile());
       	Log.info(c, testName, "------- threadpool metrics should be available ------");
    	getHttpsServlet("/metrics/vendor", serverEDF2);
    	Log.info(c, testName, "------- servlet metrics should be available ------");
        Assert.assertNotNull("CWWKO0219I NOT FOUND",serverEDF2.waitForStringInTraceUsingMark("Monitoring MXBean WebSphere:type=ServletStats"));
       	checkStrings(getHttpsServlet("/metrics/vendor", serverEDF2), new String[] {
       		"vendor:threadpool_default_executor_active_threads",
       		"vendor:threadpool_default_executor_size",
       		"vendor:servlet_com_ibm_ws_microprofile_metrics"
       	}, new String[] {});
       	tearDown(serverEDF2);
    }
    
    @Test
    public void testEDF3() throws Exception {
    	
    	String testName = "testEDF3";
    	serverEDF3.startServer();
    	Log.info(c, testName, "------- Add session application and run session servlet ------");
       	ShrinkHelper.defaultDropinApp(serverEDF3, "testSessionApp", "com.ibm.ws.microprofile.metrics.monitor_fat.session.servlet");
       	Log.info(c, testName, "------- added testSessionApp to dropins -----");
    	checkStrings(getHttpServlet("/testSessionApp/testSessionServlet",serverEDF3),
    		new String[] { "Session id:" }, new String[] {});
       	Log.info(c, testName, "------- session metrics should be available ------");
       	checkStrings(getHttpsServlet("/metrics/vendor",serverEDF3), new String[] {
       		"vendor:session_default_host_test_session_app_create_total",
       		"vendor:session_default_host_test_session_app_live_sessions",
       		"vendor:session_default_host_test_session_app_active_sessions",
       		"vendor:session_default_host_test_session_app_invalidated_total",
       		"vendor:session_default_host_test_session_app_invalidatedby_timeout_total"
       	}, new String[] {});
       	tearDown(serverEDF3);
    }
    
    @Test 
    public void testEDF4() throws Exception {
    	
    	String testName = "testEDF4";
    	serverEDF4.startServer();
    	Log.info(c, testName, "------- Add session application ------");
       	ShrinkHelper.defaultDropinApp(serverEDF4, "testSessionApp", "com.ibm.ws.microprofile.metrics.monitor_fat.session.servlet");
       	Log.info(c, testName, "------- added testSessionApp to dropins -----");
       	
       	Log.info(c, testName, "------- Add JDBC application and run JDBC servlet ------");
       	ShrinkHelper.defaultDropinApp(serverEDF4, "testJDBCApp", "com.ibm.ws.microprofile.metrics.monitor_fat.jdbc.servlet");
       	Log.info(c, testName, "------- added testJDBCApp to dropins -----");
      	checkStrings(getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create", serverEDF4), 
      		new String[] { "sql: create table cities" }, new String[] {});
       	Log.info(c, testName, "------- connectionpool metrics should be available ------");
       	checkStrings(getHttpsServlet("/metrics/vendor", serverEDF4), new String[] {
       		"vendor:connectionpool_jdbc_example_ds1_connection_handles",
       		"vendor:connectionpool_jdbc_example_ds1_free_connections",
       		"vendor:connectionpool_jdbc_example_ds1_destroy_total",
       		"vendor:connectionpool_jdbc_example_ds1_create_total",
       		"vendor:connectionpool_jdbc_example_ds1_managed_connections",
       		"vendor:connectionpool_jdbc_example_ds1_wait_time_total",
       		"vendor:connectionpool_jdbc_example_ds1_in_use_time_total",
       		"vendor:connectionpool_jdbc_example_ds1_queued_requests_total",
       		"vendor:connectionpool_jdbc_example_ds1_used_connections_total",
       		"vendor:connectionpool_jdbc_example_ds2_connection_handles",
       		"vendor:connectionpool_jdbc_example_ds2_free_connections",
       		"vendor:connectionpool_jdbc_example_ds2_destroy_total",
       		"vendor:connectionpool_jdbc_example_ds2_create_total",
       		"vendor:connectionpool_jdbc_example_ds2_managed_connections",
       		"vendor:connectionpool_jdbc_example_ds2_wait_time_total",
       		"vendor:connectionpool_jdbc_example_ds2_in_use_time_total",
       		"vendor:connectionpool_jdbc_example_ds2_queued_requests_total",
       		"vendor:connectionpool_jdbc_example_ds2_used_connections_total",
       	}, new String[] {});
       	tearDown(serverEDF4);
    }
    
    @Test 
    public void testEDF5() throws Exception {
    	
    	String testName = "testEDF5";
    	serverEDF5.startServer();
    	Log.info(c, testName, "------- Add session application ------");
       	ShrinkHelper.defaultDropinApp(serverEDF5, "testSessionApp", "com.ibm.ws.microprofile.metrics.monitor_fat.session.servlet");
       	Log.info(c, testName, "------- added testSessionApp to dropins -----");
       	Log.info(c, testName, "------- Add JDBC application ------");
       	ShrinkHelper.defaultDropinApp(serverEDF5, "testJDBCApp", "com.ibm.ws.microprofile.metrics.monitor_fat.jdbc.servlet");
       	Log.info(c, testName, "------- added testJDBCApp to dropins -----");
       	
    	Log.info(c, testName, "------- Add jax-ws endpoint application and run jax-ws client servlet ------");
       	ShrinkHelper.defaultDropinApp(serverEDF5, "testJaxWsApp", "com.ibm.ws.microprofile.metrics.monitor_fat.jaxws","com.ibm.ws.microprofile.metrics.monitor_fat.jaxws.client");
       	Log.info(c, testName, "------- added testJaxWsApp to dropins -----");
      	checkStrings(getHttpServlet("/testJaxWsApp/SimpleStubClientServlet",serverEDF5),
       		new String[] { "Pass" }, new String[] {});
       	Log.info(c, testName, "------- jax-ws metrics should be available ------");
       	checkStrings(getHttpsServlet("/metrics/vendor",serverEDF5), new String[] {
       		"vendor:jaxws_client_jaxws_monitor_fat_metrics_microprofile_ws_ibm_com_simple_echo_service_simple_echo_port_checked_application_faults_total",
       		"vendor:jaxws_client_jaxws_monitor_fat_metrics_microprofile_ws_ibm_com_simple_echo_service_simple_echo_port_runtime_faults_total",
      		"vendor:jaxws_client_jaxws_monitor_fat_metrics_microprofile_ws_ibm_com_simple_echo_service_simple_echo_port_response_time_total_seconds",
       		"vendor:jaxws_client_jaxws_monitor_fat_metrics_microprofile_ws_ibm_com_simple_echo_service_simple_echo_port_invocations_total",
       		"vendor:jaxws_client_jaxws_monitor_fat_metrics_microprofile_ws_ibm_com_simple_echo_service_simple_echo_port_unchecked_application_faults_total",
       		"vendor:jaxws_client_jaxws_monitor_fat_metrics_microprofile_ws_ibm_com_simple_echo_service_simple_echo_port_logical_runtime_faults_total",
       		"vendor:jaxws_server_jaxws_monitor_fat_metrics_microprofile_ws_ibm_com_simple_echo_service_simple_echo_port_checked_application_faults_total",
       		"vendor:jaxws_server_jaxws_monitor_fat_metrics_microprofile_ws_ibm_com_simple_echo_service_simple_echo_port_runtime_faults_total",
       		"vendor:jaxws_server_jaxws_monitor_fat_metrics_microprofile_ws_ibm_com_simple_echo_service_simple_echo_port_response_time_total_seconds",
       		"vendor:jaxws_server_jaxws_monitor_fat_metrics_microprofile_ws_ibm_com_simple_echo_service_simple_echo_port_invocations_total",
       		"vendor:jaxws_server_jaxws_monitor_fat_metrics_microprofile_ws_ibm_com_simple_echo_service_simple_echo_port_unchecked_application_faults_total",
       		"vendor:jaxws_server_jaxws_monitor_fat_metrics_microprofile_ws_ibm_com_simple_echo_service_simple_echo_port_logical_runtime_faults_total"	
       	}, new String[] {});
       	tearDown(serverEDF5);
    }
    
    @Test
    public void testEDF6() throws Exception {
    	
    	String testName = "testEDF6";
    	serverEDF6.startServer();
    	Log.info(c, testName, "------- Monitor filter ThreadPool and WebContainer  ------");
    	serverEDF6.setServerConfigurationFile("server_monitorFilter1.xml");
        Assert.assertNotNull("CWWKG0017I NOT FOUND",serverEDF6.waitForStringInLogUsingMark("CWWKG0017I"));
        Log.info(c, testName, "------- Only threadpool and servlet metrics should be available ------");
        getHttpsServlet("/metrics", serverEDF6); // Initialize the metrics endpoint first, to load the mpMetrics servlet metrics.
        checkStrings(getHttpsServlet("/metrics/vendor", serverEDF6), 
        	new String[] { "vendor:threadpool", "vendor:servlet" }, 
        	new String[] { "vendor:session", "vendor:connectionpool" });
        tearDown(serverEDF6);
    }
    
    @Test 
    public void testEDF7() throws Exception {
    	
    	String testName = "testEDF7";
    	serverEDF6.startServer();
    	Log.info(c, testName, "------- Monitor filter WebContainer and Session ------");
       	serverEDF6.setMarkToEndOfLog();
       	serverEDF6.setServerConfigurationFile("server_monitorFilter2.xml");
       	Assert.assertNotNull("CWWKG0017I NOT FOUND",serverEDF6.waitForStringInLogUsingMark("CWWKG0017I"));
    	checkStrings(getHttpServlet("/testSessionApp/testSessionServlet", serverEDF6), 
    		new String[] { "Session id:" }, new String[] {});
       	Log.info(c, testName, "------- Only servlet and session metrics should be available ------");
       	checkStrings(getHttpsServlet("/metrics/vendor", serverEDF6), 
       		new String[] { "vendor:servlet", "vendor:session" }, 
       		new String[] { "vendor:threadpool", "vendor:connectionpool" });
       	tearDown(serverEDF6);
    }
   
    @Test 
    public void testEDF8() throws Exception {
    	
    	String testName = "testEDF8";
    	serverEDF6.startServer();
    	Log.info(c, testName, "------- Monitor filter Session and ConnectionPool ------");
    	serverEDF6.setServerConfigurationFile("server_monitorFilter3.xml");
       	Assert.assertNotNull("CWWKG0017I NOT FOUND",serverEDF6.waitForStringInLogUsingMark("CWWKG0017I"));
      	checkStrings(getHttpServlet("/testJDBCApp/testJDBCServlet?operation=select&city=city1&id=id1", serverEDF6), 
          		new String[] { "sql: select" }, new String[] {});
       	Log.info(c, testName, "------- Only session and connectionpool metrics should be available ------");
      	checkStrings(getHttpsServlet("/metrics/vendor", serverEDF6), 
       		new String[] { "vendor:session", "vendor:connectionpool" }, 
       		new String[] { "vendor:servlet", "vendor:threadpool" });
    }
    
    @Test public void testEDF9() throws Exception {
    	
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
            String authorization = "Basic "+ Base64.getEncoder().encodeToString(("theUser:thePassword").getBytes(StandardCharsets.UTF_8));  //Java 8
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
}
