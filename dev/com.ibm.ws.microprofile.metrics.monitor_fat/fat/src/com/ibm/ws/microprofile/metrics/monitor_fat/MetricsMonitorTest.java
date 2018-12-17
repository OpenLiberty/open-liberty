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
public class MetricsMonitorTest {
	
    private static Class<?> c = MetricsMonitorTest.class;

    @Server("MetricsMonitorServer")
    public static LibertyServer server;
    
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
    
    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKS4000E", "CWWKZ0014W");
            server.removeAllInstalledAppsForValidation();
        }
    }
       
    @Test
    public void testEnableDisableFeatures() throws Exception {

    	String testName = "testEnableDisableFeatures";
    	
    	Log.info(c, testName, "------- No monitor-1.0: no vendor metrics should be available ------");
    	server.setServerConfigurationFile("server_mpMetric11.xml");
    	server.startServer();
    	Assert.assertNotNull("Web application /metrics not loaded", server.waitForStringInLog("CWWKT0016I: Web application available \\(default_host\\): http:\\/\\/.*:.*\\/metrics\\/"));
       	Assert.assertNotNull("CWWKO0219I NOT FOUND",server.waitForStringInLogUsingMark("defaultHttpEndpoint-ssl"));
    	Log.info(c, testName, "------- server started -----");
      	checkStrings(getHttpsServlet("/metrics"), 
          	new String[] { "base:" }, 
          	new String[] { "vendor:" });
    	
    	Log.info(c, testName, "------- Enable mpMetrics-1.1 and monitor-1.0: threadpool metrics should be available ------");
    	server.setMarkToEndOfLog();
    	server.setServerConfigurationFile("server_monitor.xml");
    	String logMsg = server.waitForStringInLogUsingMark("CWPMI2001I");
    	Log.info(c, testName, logMsg);
		Assert.assertNotNull("No CWPMI2001I was found.", logMsg);
       	server.setMarkToEndOfLog(server.getMostRecentTraceFile());
       	
       	Log.info(c, testName, "------- threadpool metrics should be available ------");
		getHttpsServlet("/metrics/vendor");
		
       	Log.info(c, testName, "------- servlet metrics should be available ------");
       	Assert.assertNotNull("CWWKO0219I NOT FOUND",server.waitForStringInTraceUsingMark("Monitoring MXBean WebSphere:type=ServletStats"));
       	checkStrings(getHttpsServlet("/metrics/vendor"), new String[] {
       		"vendor:threadpool_default_executor_active_threads",
       		"vendor:threadpool_default_executor_size",
       		"vendor:servlet_com_ibm_ws_microprofile_metrics"
       	}, new String[] {});
       	       	
       	Log.info(c, testName, "------- Add session application and run session servlet ------");
       	ShrinkHelper.defaultDropinApp(server, "testSessionApp", "com.ibm.ws.microprofile.metrics.monitor_fat.session.servlet");
       	Log.info(c, testName, "------- added testSessionApp to dropins -----");
    	checkStrings(getHttpServlet("/testSessionApp/testSessionServlet"),
    		new String[] { "Session id:" }, new String[] {});
       	Log.info(c, testName, "------- session metrics should be available ------");
       	checkStrings(getHttpsServlet("/metrics/vendor"), new String[] {
       		"vendor:session_default_host_test_session_app_create_total",
       		"vendor:session_default_host_test_session_app_live_sessions",
       		"vendor:session_default_host_test_session_app_active_sessions",
       		"vendor:session_default_host_test_session_app_invalidated_total",
       		"vendor:session_default_host_test_session_app_invalidatedby_timeout_total"
       	}, new String[] {});
       	
       	Log.info(c, testName, "------- Add JDBC application and run JDBC servlet ------");
       	ShrinkHelper.defaultDropinApp(server, "testJDBCApp", "com.ibm.ws.microprofile.metrics.monitor_fat.jdbc.servlet");
       	Log.info(c, testName, "------- added testJDBCApp to dropins -----");
      	checkStrings(getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create"), 
      		new String[] { "sql: create table cities" }, new String[] {});
       	Log.info(c, testName, "------- connectionpool metrics should be available ------");
       	checkStrings(getHttpsServlet("/metrics/vendor"), new String[] {
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
       	
       	Log.info(c, testName, "------- Add jax-ws endpoint application and run jax-ws client servlet ------");
       	ShrinkHelper.defaultDropinApp(server, "testJaxWsApp", "com.ibm.ws.microprofile.metrics.monitor_fat.jaxws","com.ibm.ws.microprofile.metrics.monitor_fat.jaxws.client");
       	Log.info(c, testName, "------- added testJaxWsApp to dropins -----");
    	checkStrings(getHttpServlet("/testJaxWsApp/SimpleStubClientServlet"),
    		new String[] { "Pass" }, new String[] {});
       	Log.info(c, testName, "------- jax-ws metrics should be available ------");
       	checkStrings(getHttpsServlet("/metrics/vendor"), new String[] {
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
       	
       	Log.info(c, testName, "------- Monitor filter ThreadPool and WebContainer  ------");
       	server.setMarkToEndOfLog();
       	server.setServerConfigurationFile("server_monitorFilter1.xml");
       	Assert.assertNotNull("CWWKG0017I NOT FOUND",server.waitForStringInLogUsingMark("CWWKG0017I"));
       	Log.info(c, testName, "------- Only threadpool and servlet metrics should be available ------");
       	getHttpsServlet("/metrics"); // Initialize the metrics endpoint first, to load the mpMetrics servlet metrics.
       	checkStrings(getHttpsServlet("/metrics/vendor"), 
       		new String[] { "vendor:threadpool", "vendor:servlet" }, 
       		new String[] { "vendor:session", "vendor:connectionpool" });
       	
       	Log.info(c, testName, "------- Monitor filter WebContainer and Session ------");
       	server.setMarkToEndOfLog();
       	server.setServerConfigurationFile("server_monitorFilter2.xml");
       	Assert.assertNotNull("CWWKG0017I NOT FOUND",server.waitForStringInLogUsingMark("CWWKG0017I"));
    	checkStrings(getHttpServlet("/testSessionApp/testSessionServlet"), 
    		new String[] { "Session id:" }, new String[] {});
       	Log.info(c, testName, "------- Only servlet and session metrics should be available ------");
       	checkStrings(getHttpsServlet("/metrics/vendor"), 
       		new String[] { "vendor:servlet", "vendor:session" }, 
       		new String[] { "vendor:threadpool", "vendor:connectionpool" });
       	
       	Log.info(c, testName, "------- Monitor filter Session and ConnectionPool ------");
       	server.setMarkToEndOfLog();
       	server.setServerConfigurationFile("server_monitorFilter3.xml");
       	Assert.assertNotNull("CWWKG0017I NOT FOUND",server.waitForStringInLogUsingMark("CWWKG0017I"));
      	checkStrings(getHttpServlet("/testJDBCApp/testJDBCServlet?operation=select&city=city1&id=id1"), 
          		new String[] { "sql: select" }, new String[] {});
       	Log.info(c, testName, "------- Only session and connectionpool metrics should be available ------");
       	checkStrings(getHttpsServlet("/metrics/vendor"), 
       		new String[] { "vendor:session", "vendor:connectionpool" }, 
       		new String[] { "vendor:servlet", "vendor:threadpool" });
       	
       	Log.info(c, testName, "------- Monitor filter ThreadPool, WebContainer, Session and ConnectionPool ------");
       	server.setMarkToEndOfLog();
       	server.setServerConfigurationFile("server_monitorFilter4.xml");
       	Assert.assertNotNull("CWWKG0017I NOT FOUND",server.waitForStringInLogUsingMark("CWWKG0017I"));
    	checkStrings(getHttpServlet("/testSessionApp/testSessionServlet"), 
        		new String[] { "Session id:" }, new String[] {});
      	checkStrings(getHttpServlet("/testJDBCApp/testJDBCServlet?operation=select&city=city1&id=id1"), 
          		new String[] { "sql: select" }, new String[] {});
       	Log.info(c, testName, "------- all four vendor metrics should be available ------");
       	checkStrings(getHttpsServlet("/metrics/vendor"), 
       		new String[] {"vendor:threadpool", "vendor:servlet", "vendor:session", "vendor:connectionpool" }, 
       		new String[] {});
       	
       	Log.info(c, testName, "------- Remove JAX-WS application ------");
       	boolean rc1 = server.removeAndStopDropinsApplications("testJaxWsApp.war");
       	Log.info(c, testName, "------- " + (rc1 ? "successfully removed" : "failed to remove") + " JAX-WS application ------");
       	server.setMarkToEndOfLog();
       	server.setServerConfigurationFile("server_noJaxWs.xml");
       	Assert.assertNotNull("CWWKG0017I NOT FOUND",server.waitForStringInLogUsingMark("CWWKG0017I"));
       	Assert.assertNotNull("CWWKT0016I NOT FOUND",server.waitForStringInLogUsingMark("CWWKT0016I"));
       	Assert.assertNotNull("CWWKZ0009I NOT FOUND",server.waitForStringInLogUsingMark("CWWKZ0009I"));
       	Log.info(c, testName, "------- jax-ws metrics should not be available ------");
      	checkStrings(getHttpsServlet("/metrics/vendor"), 
      		new String[] { "vendor:" }, 
      		new String[] { "vendor:jaxws_client", "vendor:jaxws_server"});
       	
       	Log.info(c, testName, "------- Remove JDBC application ------");
       	boolean rc2 = server.removeAndStopDropinsApplications("testJDBCApp.war");
       	Log.info(c, testName, "------- " + (rc2 ? "successfully removed" : "failed to remove") + " JDBC application ------");
       	server.setMarkToEndOfLog();
       	server.setServerConfigurationFile("server_noJDBC.xml");
       	Assert.assertNotNull("CWWKG0017I NOT FOUND",server.waitForStringInLogUsingMark("CWWKG0017I"));
       	Assert.assertNotNull("CWWKT0016I NOT FOUND",server.waitForStringInLogUsingMark("CWWKT0016I"));
       	Assert.assertNotNull("CWWKZ0009I NOT FOUND",server.waitForStringInLogUsingMark("CWWKZ0009I"));
       	Log.info(c, testName, "------- connectionpool metrics should not be available ------");
      	checkStrings(getHttpsServlet("/metrics/vendor"), 
      		new String[] { "vendor:" }, 
      		new String[] { "vendor:connectionpool", "vendor:servlet_test_jdbc_app" });
      	      	
       	Log.info(c, testName, "------- Remove monitor-1.0 ------");
    	server.setMarkToEndOfLog();
    	server.setServerConfigurationFile("server_noJDBCMonitor.xml");
       	Assert.assertNotNull("CWWKG0016I NOT FOUND",server.waitForStringInLogUsingMark("CWWKG0016I"));
       	Assert.assertNotNull("CWWKZ0003I NOT FOUND",server.waitForStringInLogUsingMark("CWWKF0008I"));
       	Assert.assertNotNull("CWPMI2002I NOT FOUND",server.waitForStringInLogUsingMark("CWPMI2002I"));
       	Log.info(c, testName, "------- no vendor metrics should be available ------");
      	checkStrings(getHttpsServlet("/metrics"), 
      		new String[] {}, 
      		new String[] { "vendor:" });
    }

    @Test
    public void testDisableMpMetricsFeature() throws Exception {
    	
    	String testName = "testDisableMpMetricsFeature";
    	
    	Log.info(c, testName, "------- Enable mpMetrics-1.1 and monitor-1.0: vendor metrics should be available ------");
    	server.setServerConfigurationFile("server_monitor.xml");
    	server.startServer();
    	Log.info(c, testName, server.waitForStringInLog("defaultHttpEndpoint-ssl",60000));
    	Log.info(c, testName, "------- server started -----");
      	checkStrings(getHttpsServlet("/metrics"), 
          	new String[] { "base:", "vendor:" }, 
          	new String[] {});
      	
      	Log.info(c, testName, "------- Remove mpMetrics-1.1: no metrics should be available ------");
      	server.setServerConfigurationFile("server_monitorOnly.xml");
      	String logMsg = server.waitForStringInLogUsingMark("CWPMI2002I");
      	Log.info(c, testName, logMsg);
      	Assert.assertNotNull("No CWPMI2002I message", logMsg);
    }
    
    @Test
    public void testMpMetrics10Monitor10Feature() throws Exception {
    	
    	String testName = "testMpMetrics10Monitor10Feature";
    	
    	Log.info(c, testName, "------- Enable mpMetrics-1.0 and monitor-1.0: vendor metrics should not be available ------");
    	server.setServerConfigurationFile("server_mpMetric10Monitor10.xml");
    	server.startServer();
    	Log.info(c, testName, server.waitForStringInLog("defaultHttpEndpoint-ssl",60000));
    	Log.info(c, testName, "------- server started -----");
      	checkStrings(getHttpsServlet("/metrics"), 
          	new String[] { "base:" }, 
          	new String[] { "vendor: "});
    }
    
    @Test
    public void testMicroProfileMonitor10Feature() throws Exception {
    	
    	String testName = "testMicroProfileMonitor10Feature";
    	
    	Log.info(c, testName, "------- Enable microProfile-1.3 and monitor-1.0: vendor metrics should be available ------");
    	server.setServerConfigurationFile("server_microProfile13Monitor10.xml");
    	server.startServer();
    	Log.info(c, testName, server.waitForStringInLog("CWWKS5500I",60000));
    	Log.info(c, testName, "------- server started -----");
      	checkStrings(getHttpsServlet("/metrics"),
          	new String[] { "vendor:" }, 
          	new String[] {});
      	checkStrings(getHttpsServlet("/metrics/vendor/threadpool.Default_Executor.activeThreads"),
              	new String[] { "vendor:threadpool_default_executor_active_threads" }, 
              	new String[] { "vendor:threadpool_default_executor_size" });
    }
   
    private String getHttpServlet(String servletPath) throws Exception {
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
    
    private String getHttpsServlet(String servletPath) throws Exception {
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
