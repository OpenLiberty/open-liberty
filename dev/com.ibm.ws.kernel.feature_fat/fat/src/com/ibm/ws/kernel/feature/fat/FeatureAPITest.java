package com.ibm.ws.kernel.feature.fat;

/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class FeatureAPITest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.api");
    private final Class<?> c = FeatureAPITest.class;

    @Test
    public void testAddApi() throws Exception {
        final String method = "testAddApi";
        server.startServer();

        assertNotNull("The application did not appear to have been installed.", server.waitForStringInLog("CWWKZ0001I.* test.feature.api.client"));
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/test.feature.api.client/apiClient");
        Log.info(c, method, "Calling Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();
        assertEquals("Wrong response from application", "FAILED", line);

        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("servlet-3.0", "test.feature.api-1.0"));
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        Log.info(c, method, "Calling Application with URL=" + url.toString());
        con = getHttpConnection(url);
        br = getConnectionStream(con);
        line = br.readLine();
        assertEquals("Wrong response from application", "ApiClientTest:SUCCESS", line);
    }

    @BeforeClass
    public static void installSystemFeature() throws Exception {
        server.installSystemFeature("test.feature.api.internal-1.0");
        server.installSystemFeature("test.feature.api-1.0");
        server.installSystemBundle("test.feature.api_1.0.0");
    }

    @AfterClass
    public static void uninstallSystemFeature() throws Exception {
        server.uninstallSystemFeature("test.feature.api.internal-1.0");
        server.uninstallSystemFeature("test.feature.api-1.0");
        server.uninstallSystemBundle("test.feature.api_1.0.0");
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     * 
     * @param con The connection to the HTTP address
     * @return The Output from the webpage
     */
    private BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    /**
     * This method creates a connection to a webpage and then reutrns the connection
     * 
     * @param url The Http Address to connect to
     * @return The connection to the http address
     */
    private HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }
}