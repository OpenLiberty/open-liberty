/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class AsyncClientConnectionTest {

    private static final String APP_NAME = "calculatorAsync";

    @Server("AsyncClientConnectionServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "com.ibm.samples.jaxws.catalog.servlet", "com.ibm.samples.jaxws.catalog.stubs");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
    }

    @Before
    public void beforeEachTest() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
        server.startServer("AsyncClientConnectionServer.log");
        assertNotNull("Application " + APP_NAME + " does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));
    }

    @Test
    public void testAsyncMaxConnections() throws Exception {
        invokeServlet("true"); // isAsync is set to true

        assertNotNull("Max client connections is not set!",
                      server.waitForStringInTrace("SetProperties: org.apache.cxf.transport.http.async.MAX_CONNECTIONS is set to 3"));
        assertNotNull("Max client connections per host is not set!",
                      server.waitForStringInTrace("SetProperties: org.apache.cxf.transport.http.async.MAX_PER_HOST_CONNECTIONS is set to 2"));
        assertNotNull("Max client connections per host is set but not working!", server.waitForStringInTrace("route allocated: 1 of 2"));
        assertNotNull("Max client connections  is set but not working!", server.waitForStringInTrace("total allocated: 1 of 3]"));
    }

    @Test
    public void testSyncMaxConnections() throws Exception {
        invokeServlet("false"); // isAsync is set to false

        assertNotNull("Max client connections is not set!",
                      server.waitForStringInTrace("SetProperties: org.apache.cxf.transport.http.async.MAX_CONNECTIONS is set to 3"));
        assertNotNull("Max client connections per host is not set!",
                      server.waitForStringInTrace("SetProperties: org.apache.cxf.transport.http.async.MAX_PER_HOST_CONNECTIONS is set to 2"));
        assertNotNull("Max client connections per host is set but not working!", server.waitForStringInTrace("route allocated: 1 of 2"));
        assertNotNull("Max client connections  is set but not working!", server.waitForStringInTrace("total allocated: 1 of 3]"));
    }

    public void invokeServlet(String isAsync) throws IOException {
        String url = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/").append(APP_NAME).append("/AsyncClientConnectionServlet?isAsync=").append(isAsync).toString();

        Log.info(AsyncClientConnectionTest.class, "invokeServlet", "Calling Application with URL=" + url);
        try {
            HttpURLConnection con = HttpUtils.getHttpConnection(new URL(url), HttpURLConnection.HTTP_OK, 10);
            StringBuilder sb = new StringBuilder();
            BufferedReader br = null;
            try {
                br = HttpUtils.getConnectionStream(con);
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
            } finally {
                if (br != null) {
                    br.close();
                }
            }
            String responseContent = sb.toString();
            Log.info(AsyncClientConnectionTest.class, "invokeServlet", "responseContent = " + responseContent);

        } catch (IOException e) {
            // Return true if the exception contains the expected response code
            if (!e.getMessage().contains(Integer.toString(HttpURLConnection.HTTP_OK))) {
                throw e;
            }
        }
    }
}
