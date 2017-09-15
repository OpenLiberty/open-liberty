/*******************************************************************************
 * Copyright (c) 2014,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsonp.fat;

import static componenttest.topology.utils.HttpUtils.getHttpConnection;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.app.FATServlet;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;
import junit.framework.Assert;

/**
 * Abstract test class. This should be extended when adding a new test class,
 * so that this shared logic for running the actual tests remains in one place.
 *
 * To add new test, simply implement @BeforeClass method that gets and starts
 * the server needed.
 */
public abstract class AbstractTest {
    /** The time in seconds to wait at a URL before giving up. **/
    public static final int CONN_TIMEOUT = 30;

    protected static LibertyServer server;
    private static final Class<?> c = AbstractTest.class;

    @Rule
    public TestName testName = new TestName();

    public String servlet;

    /**
     * Call {@link #runTest} with the configured servlet name and the current
     * test method as the "testMethod" query parameter.
     */
    protected void runTest() throws Exception {
        Assert.assertNotNull(servlet);
        runTest(servlet + "?testMethod=" + testName.getMethodName());
    }

    protected void runTest(String servlet, String method) throws Exception {
        Assert.assertNotNull(servlet);
        String invocationString = servlet + "?testMethod=" + method;
        runTest(invocationString);
    }

    /**
     * Invoke the specified servlet and look for "PASSED" as a response.
     */
    protected void runTest(String servlet) throws Exception {
        final String method = testName.getMethodName();

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + servlet);
        Log.info(c, method, "Calling application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url, HttpURLConnection.HTTP_OK, new int[0], CONN_TIMEOUT, HTTPRequestMethod.GET);

        // Check the response from servlet for "PASSED"
        String response = getAnyResponse(con);
        Log.info(c, method, "Response from " + servlet + ": " + response);
        if (!"PASSED".equals(response) &&
            !response.contains(FATServlet.SUCCESS)) {
            fail(servlet + " failed: " + response);
        }
    }

    /**
     * Invoke the specified servlet and wait until the passed in response
     * code is returned.
     *
     * @return the message associated with the response
     */
    String runTest(String servlet, int responseCode) throws Exception {
        final String method = testName.getMethodName();

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + servlet);
        Log.info(c, method, "Calling application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url, responseCode, CONN_TIMEOUT);

        // Check the response from servlet for "PASSED"
        String response = getAnyResponse(con);
        Log.info(c, method, "Response from " + servlet + ": " + response);

        return response;
    }

    /**
     * This method is used to read the response from either the input stream or
     * error stream.
     *
     * @param con The connection to the HTTP address
     * @return The output or error from the webpage
     * @throws IOException if neither the input stream nor error stream can be read
     */
    public static String getAnyResponse(HttpURLConnection urlConnection) throws IOException {
        BufferedReader br;
        try {
            br = HttpUtils.getConnectionStream(urlConnection);
        } catch (IOException ioex) {
            br = HttpUtils.getErrorStream(urlConnection);
        }

        String response = "";
        String aLine;

        while ((aLine = br.readLine()) != null) {
            response = response + aLine;
        }
        return response;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
