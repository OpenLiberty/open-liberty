/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.monitor_fat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.junit.Assert;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

/**
 *
 */
public abstract class BaseTestClass {

    protected Class<?> c = this.getClass();

    protected static final String PATH_TO_AUTOFVT_TESTFILES = "lib/LibertyFATTestFiles/";

    protected static final String IMAGE_NAME = ImageNameSubstitutor.instance() //
                    .apply(DockerImageName.parse("otel/opentelemetry-collector-contrib:0.103.0")).asCanonicalNameString();

    
    protected String requestContainerHttpServlet(String servletPath, String host, int port, String requestMethod, String query) {
        HttpURLConnection con = null;
        try {
            String sURL = "http://" + host + ":"
                          + port + servletPath
                          + ((query != null) ? ("?" + query) : "");

            Log.info(c, "requestContainerHttpServlet", sURL);

            URL checkerServletURL = new URL(sURL);
            con = (HttpURLConnection) checkerServletURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod(requestMethod);
            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line).append(sep);
            }
            return lines.toString();
        } catch (IOException e) {
            Log.info(c, "requestContainerHttpServlet", "Encountered IO exception " + e);
            return null;
        } catch (Exception e) {
            Log.info(c, "requestContainerHttpServlet", "Encountered an exception " + e);
            return null;
        } finally {
            if (con != null)
                con.disconnect();
        }

    }

    protected String getContainerCollectorMetrics(GenericContainer<?> container) throws Exception {
        String containerCollectorMetrics = requestContainerHttpServlet("/metrics", container.getHost(), container.getMappedPort(8889), HttpMethod.GET, null);
        Log.info(c, "getContainerCollectorMetrics", containerCollectorMetrics);
        return containerCollectorMetrics;
    }

    
    
    protected void checkStrings(String metricsText, String[] expectedString) {
      for (String m : expectedString) {
          if (!metricsText.contains(m)) {
              Log.info(c, "checkStrings", "Failed:\n" + metricsText);
              Assert.fail("Did not contain string: " + m);
          }
      }
     
  }
    
    
    protected void matchStrings(String metricsText, String[] expectedString) {
        
		for (String m : expectedString) {
			try (Scanner sc = new Scanner(metricsText)) {
				boolean isFound = false;
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					if (line.matches(m)) {
						isFound=true;
						break;
					}
				}//while
				if (!isFound) {
					Log.info(c, "checkStrings", "Failed:\n" + metricsText);
					Assert.fail("Did not contain string: " + m);
				}
			}//try
		}//for
    }
   
    
    protected String requestHttpServlet(String servletPath, LibertyServer server) {
        return requestHttpServlet(servletPath, server, HttpMethod.GET);
    }
    
    protected String requestHttpServlet(String servletPath, LibertyServer server, String requestMethod) {
        return requestHttpServlet(servletPath, server, requestMethod, null);
    }

    protected String requestHttpServlet(String servletPath, LibertyServer server, String requestMethod, String query) {
        HttpURLConnection con = null;
        try {
            String sURL = "http://" + server.getHostname() + ":"
                          + server.getHttpDefaultPort() + servletPath
                          + ((query != null) ? ("?" + query) : "");
            URL checkerServletURL = new URL(sURL);
            con = (HttpURLConnection) checkerServletURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod(requestMethod);
            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line).append(sep);
            }
            Log.info(c, "requestHttpServlet", sURL);
            return lines.toString();
        } catch (IOException e) {
            Log.info(c, "requestHttpServlet", "Encountered IO exception " + e);
            return null;
        } catch (Exception e) {
            Log.info(c, "requestHttpServlet", "Encountered an exception " + e);
            return null;
        } finally {
            if (con != null)
                con.disconnect();
        }

    }
    
    protected void addSessionsJdbcJaxwsApps(LibertyServer server) throws Exception {
        String testName = "add3Apps to " + server.toString();
        // Sessions App
        Log.info(c, testName, "------- Add session application ------");
        ShrinkHelper.defaultDropinApp(server, "testSessionApp",
                "io.openliberty.microprofile.telemetry.internal.monitor_fat.session.servlet");
        Log.info(c, testName, "------- added testSessionApp to dropins -----");
        // JDBC App
        Log.info(c, testName, "------- Add JDBC application ------");
        ShrinkHelper.defaultDropinApp(server, "testJDBCApp",
                "io.openliberty.microprofile.telemetry.internal.monitor_fat.jdbc.servlet");
        Log.info(c, testName, "------- added testJDBCApp to dropins -----");
        // JAX-WS App
        Log.info(c, testName, "------- Add jax-ws endpoint application ------");
        ShrinkHelper.defaultDropinApp(server, "testJaxWsApp", "io.openliberty.microprofile.telemetry.internal.monitor_fat.jaxws",
                "io.openliberty.microprofile.telemetry.internal.monitor_fat.jaxws.client");
        Log.info(c, testName, "------- added testJaxWsApp to dropins -----");
    }

    protected void addSessionsJdbcApps(LibertyServer server) throws Exception {
        String testName = "add2Apps to " + server.toString();
        //Sessions app
        Log.info(c, testName, "------- Add session application ------");
        ShrinkHelper.defaultDropinApp(server, "testSessionApp",
                "io.openliberty.microprofile.telemetry.internal.monitor_fat.session.servlet");
        Log.info(c, testName, "------- added testSessionApp to dropins -----");
        //JDBC app
        Log.info(c, testName, "------- Add JDBC application ------");
        ShrinkHelper.defaultDropinApp(server, "testJDBCApp",
                "io.openliberty.microprofile.telemetry.internal.monitor_fat.jdbc.servlet");
        Log.info(c, testName, "------- added testJDBCApp to dropins -----");
    }
}
