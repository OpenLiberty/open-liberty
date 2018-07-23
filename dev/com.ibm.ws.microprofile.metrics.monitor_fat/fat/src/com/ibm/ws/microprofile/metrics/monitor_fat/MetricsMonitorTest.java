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
import java.io.IOException;
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

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.simplicity.ShrinkHelper;

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
            server.stopServer();
            server.removeAllInstalledAppsForValidation();
        }
    }
       
    @Test
    public void testEnableDisableFeatures() throws Exception {
    	
    	Log.info(c, "testEnableDisableFeatures", "------- No monitor-1.0: no vendor metrics should be available ------");
    	server.startServer();
    	server.waitForStringInLog("defaultHttpEndpoint-ssl",5000);
    	Log.info(c, "testEnableDisableFeatures", "------- server started -----");
      	checkStrings(getHttpsServlet("/metrics"), 
          	new String[] { "base:" }, 
          	new String[] { "vendor:" });
    	
    	Log.info(c, "testEnableDisableFeatures", "------- Enable mpMetrics-1.1 and monitor-1.0: threadpool metrics should be available ------");
    	server.setMarkToEndOfLog();
    	server.setServerConfigurationFile("server_monitor.xml");
   		if (server.waitForStringInLogUsingMark("CWPMI2000I") == null) {
   			Assert.fail("No CWPMI2000I was found.");
   		}
       	checkStrings(getHttpsServlet("/metrics/vendor"), new String[] {
       		"vendor:threadpool_default_executor_active_threads",
       		"vendor:threadpool_default_executor_size"
       	}, new String[] {});
    	
       	Log.info(c, "testEnableDisableFeatures", "------- Second run: serlvet metrics should be available ------");
   		server.setMarkToEndOfLog();
   		String s = server.waitForStringInLogUsingMark("CWPMI2000I: Monitoring MXBean WebSphere:type=ServletStats", 5000);
   		if (s == null) {
   			Log.info(c, "testEnableDisableFeatures", "Warning: No CWPMI2000I was found.");
   		} else {
   			Log.info(c, "testEnableDisableFeatures", "Waited: " + s);
   		}
       	checkStrings(getHttpsServlet("/metrics/vendor"), new String[] {
       		"vendor:threadpool_default_executor_active_threads",
       		"vendor:threadpool_default_executor_size",
       		"vendor:servlet_com_ibm_ws_microprofile_metrics_1_1_metrics_rest_proxy_servlet_request_total",
       		"vendor:servlet_com_ibm_ws_microprofile_metrics_1_1_metrics_rest_proxy_servlet_response_time_total_seconds"
       	}, new String[] {});
       	
       	Log.info(c, "testEnableDisableFeatures", "------- Add session application and run session servlet ------");
       	ShrinkHelper.defaultDropinApp(server, "testSessionApp", "com.ibm.ws.microprofile.metrics.monitor_fat.session.servlet");
       	Log.info(c, "testEnableDisableFeatures", "------- added testSessionApp to dropins -----");
    	checkStrings(getHttpServlet("/testSessionApp/testSessionServlet"), new String[] {
       		"Session id:"
       	}, new String[] {});

       	Log.info(c, "testEnableDisableFeatures", "------- Third run: session metrics should be available ------");
       	checkStrings(getHttpsServlet("/metrics/vendor"), new String[] {
       		"vendor:session_default_host_test_session_app_create_total",
       		"vendor:session_default_host_test_session_app_live_sessions",
       		"vendor:session_default_host_test_session_app_active_sessions",
       		"vendor:session_default_host_test_session_app_invalidated_total",
       		"vendor:session_default_host_test_session_app_invalidatedby_timeout_total"
       	}, new String[] {});
       	
       	Log.info(c, "testEnableDisableFeatures", "------- Add JDBC application and run JDBC servlet ------");
       	ShrinkHelper.defaultDropinApp(server, "testJDBCApp", "com.ibm.ws.microprofile.metrics.monitor_fat.jdbc.servlet");
       	Log.info(c, "testEnableDisableFeatures", "------- added testJDBCApp to dropins -----");
      	checkStrings(getHttpServlet("/testJDBCApp/testJDBCServlet?operation=create"), new String[] {
       		"sql: create table cities"
      	}, new String[] {});
      	
       	Log.info(c, "testEnableDisableFeatures", "------- Fourth run: connectionpool metrics should be available ------");
       	checkStrings(getHttpsServlet("/metrics/vendor"), new String[] {
       		"vendor:connectionpool_jdbc_example_ds1_connection_handles",
       		"vendor:connectionpool_jdbc_example_ds1_free_connections",
       		"vendor:connectionpool_jdbc_example_ds1_destroy_total",
       		"vendor:connectionpool_jdbc_example_ds1_create_total",
       		"vendor:connectionpool_jdbc_example_ds1_managed_connections",
       		"vendor:connectionpool_jdbc_example_ds2_connection_handles",
       		"vendor:connectionpool_jdbc_example_ds2_free_connections",
       		"vendor:connectionpool_jdbc_example_ds2_destroy_total",
       		"vendor:connectionpool_jdbc_example_ds2_create_total",
       		"vendor:connectionpool_jdbc_example_ds2_managed_connections"
       	}, new String[] {});
       	
       	Log.info(c, "testEnableDisableFeatures", "------- Remove JDBC application ------");
       	boolean rc1 = server.removeDropinsApplications("testJDBCApp.war");
       	Log.info(c, "testEnableDisableFeatures", "------- " + (rc1 ? "successfully removed" : "failed to remove") + " JDBC application ------");
       	server.setMarkToEndOfLog();
       	server.setServerConfigurationFile("server_noJDBC.xml");
       	Log.info(c, "testEnableDisableFeatures", server.waitForStringInLogUsingMark("CWWKF0007I",5000));
       	Log.info(c, "testEnableDisableFeatures", "------- Fifth run: connectionpool metrics should not be available ------");
      	checkStrings(getHttpsServlet("/metrics/vendor"), 
      		new String[] { "vendor:" }, 
      		new String[] { "vendor:connectionpool", "vendor:servlet_test_jdbc_app" });
      	
       	Log.info(c, "testEnableDisableFeatures", "------- Remove monitor-1.0 ------");
    	server.setMarkToEndOfLog();
    	server.setServerConfigurationFile("server_mpMetric11.xml");
       	Log.info(c, "testEnableDisableFeatures", server.waitForStringInLogUsingMark("CWPMI2002I",5000));
       	Log.info(c, "testEnableDisableFeatures", "------- Sixth run: no vendor metrics should be available ------");
      	checkStrings(getHttpsServlet("/metrics"), 
      		new String[] {}, 
      		new String[] { "vendor:" });
    }

    @Test
    public void testDisableMpMetricsFeature() throws Exception {
    	
    	Log.info(c, "testDisableMpMetricsFeature", "------- Enable mpMetrics-1.1 and monitor-1.0: vendor metrics should be available ------");
    	server.setServerConfigurationFile("server_monitor.xml");
    	server.startServer();
    	server.waitForStringInLog("defaultHttpEndpoint-ssl",5000);
    	Log.info(c, "testDisableMpMetricsFeature", "------- server started -----");
      	checkStrings(getHttpsServlet("/metrics"), 
          	new String[] { "base:", "vendor:" }, 
          	new String[] {});
      	
      	Log.info(c, "testDisableMpMetricsFeature", "------- Remove mpMetrics-1.1: no metrics should be available ------");
      	server.setServerConfigurationFile("server_monitorOnly.xml");
      	String logMsg = server.waitForStringInLogUsingMark("CWPMI2002I",5000);
      	Log.info(c, "testEnableDisableFeatures", logMsg);
      	Assert.assertNotNull("No CWPMI2002I message", logMsg);
      	try {
      		getHttpsServlet("/metrics");
      		Assert.fail("/metrics still can be executed");
      	} catch (Exception e) {
      		// Passed
      	}
    }
    
    @Test
    public void testMpMetrics10Monitor10Feature() throws Exception {
    	
    	Log.info(c, "testMpMetrics10Monitor10Feature", "------- Enable mpMetrics-1.0 and monitor-1.0: vendor metrics should not be available ------");
    	server.setServerConfigurationFile("server_mpMetric10Monitor10.xml");
    	server.startServer();
    	server.waitForStringInLog("defaultHttpEndpoint-ssl",5000);
    	Log.info(c, "testDisableMpMetricsFeature", "------- server started -----");
      	checkStrings(getHttpsServlet("/metrics"), 
          	new String[] { "base:" }, 
          	new String[] { "vendor: "});
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
