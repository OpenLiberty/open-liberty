/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class TraceInjectionTest {
    protected static LibertyServer server;
    private static final int CONN_TIMEOUT = 10;

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.traceinject");
        server.startServer();
        server.setServerConfigurationFile("server-enableTraceSpec.xml");
        server.waitForStringInLog("CWWKT0016I.*metrics.*");
    }

    @Test
    public void testTraceInjectionMetrics() throws Exception {
        hitWebPage("metrics", false);
        String line = server.waitForStringInTrace(".*com.ibm.ws.microprofile.metrics.BaseMetricsHandler.*> handleRequest Entry.*");
        assertNotNull("Entry log not found", line);
        line = server.waitForStringInTrace(".*com.ibm.ws.microprofile.metrics.BaseMetricsHandler.*< handleRequest Exit.*");
        assertNotNull("Exit log not found", line);
    }

    @Test
    public void testTraceInjectionKernel() throws Exception {
        String line = server.waitForStringInTrace(".*com.ibm.ws.kernel.filemonitor.internal.MonitorHolder.*> scanForUpdates Entry.*");
        assertNotNull("Entry log not found", line);
        line = server.waitForStringInTrace(".*com.ibm.ws.kernel.filemonitor.internal.MonitorHolder.*< scanForUpdates Exit.*");
        assertNotNull("Exit log not found", line);
    }

    @Test
    public void testTraceInjectionWebcontainer() throws Exception {
        hitWebPage("metrics", false);
        String line = server.waitForStringInTrace(".*com.ibm.ws.webcontainer.cors.CorsRequestInterceptor.*> handleRequest Entry.*");
        assertNotNull("Entry log not found", line);
        line = server.waitForStringInTrace(".*com.ibm.ws.webcontainer.cors.CorsRequestInterceptor.*< handleRequest Exit.*");
        assertNotNull("Exit log not found", line);
    }

    @Before
    public void setup() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer();
        }
    }

    protected static void hitWebPage(String contextRoot, boolean failureAllowed) throws MalformedURLException, IOException, ProtocolException {
        try {
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot);
            int expectedResponseCode = failureAllowed ? HttpURLConnection.HTTP_INTERNAL_ERROR : HttpURLConnection.HTTP_OK;
            HttpURLConnection con = HttpUtils.getHttpConnection(url, expectedResponseCode, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            // Make sure the server gave us something back
            assertNotNull(line);
            con.disconnect();
        } catch (IOException e) {
            // A message about a 500 code may be fine
            if (!failureAllowed) {
                throw e;
            }

        }

    }
}
