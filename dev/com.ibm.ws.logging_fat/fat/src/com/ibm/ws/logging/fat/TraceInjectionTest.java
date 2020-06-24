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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
public class TraceInjectionTest {
    protected static LibertyServer server;
    private static final int CONN_TIMEOUT = 10;

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.traceinject");
        server.startServer();

    }

    @Test
    public void testTraceInjection() throws Exception {
        server.setServerConfigurationFile("enableTrMetrics.xml");
        server.waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I");
        server.waitForStringInLogUsingMark("CWWKT0016I.*metrics.*");
        hitWebPage("metrics", false);
        String line = server.waitForStringInTrace(".*com.ibm.ws.microprofile.metrics.BaseMetricsHandler.*> handleRequest Entry.*");
        assertNotNull("Entry log not found", line);
        line = server.waitForStringInTrace(".*com.ibm.ws.microprofile.metrics.BaseMetricsHandler.*< handleRequest Exit.*");
        assertNotNull("Exit log not found", line);

    }

    @Before
    public void setup() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer();
        }
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
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
