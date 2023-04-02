/*
 * =============================================================================
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.jndi.global.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.runner.Description;

import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jndi.global.fat.data.AppName;
import com.ibm.ws.jndi.global.fat.data.ServletName;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Run name-based tests by sending HTTP requests to test methods on the given server and app.
 */
public class ServletMethodRunner implements TestRule {
    /** The time in seconds to wait at a URL before giving up. **/
    private static final int CONN_TIMEOUT = 30;
    private static final Class<?> c = ServletMethodRunner.class;

    private final LibertyServer server;
    private final String appName;
    private final String servletName;

    public ServletMethodRunner(final AppName appName, final ServletName servletName, final LibertyServer server) {
        assertNotNull("servletName should not be null", servletName);
        assertNotNull("server should not be null", server);
        this.appName = appName.getAppName();
        this.servletName = servletName.getServletName();
        this.server = server;
    }

    /**
     * Run the test with the given name.
     *
     * @param testMethod The test to run.
     */
    public final void run(final String testMethod) {
        try {
            final URL url = getUrl(testMethod);
            Log.info(c, testMethod, "Calling application with URL=" + url.toString());
            // Note: Uses local Utils
            final HttpURLConnection con = Util.getHttpConnection(url);
            try {
                final BufferedReader stream = Util.getStream(con);
                String line = stream.readLine();
                Log.info(c, testMethod, line);
                assertNotNull("Should have got a non-empty response from the servlet", line);
                if (line.contains("SUCCESS")) {
                    return;
                }
                final StringBuilder sb = new StringBuilder().append("Received non-success response from the servlet:\n\t").append(line);
                while ((line = stream.readLine()) != null) {
                    sb.append("\n\t").append(line);
                }
                fail(sb.toString());
            } finally {
                con.disconnect();
            }
        } catch (final Throwable t) {
            Util.rethrow(t);
        }
    }

    /**
     * Invoke the specified servlet and wait until the passed in response
     * code is returned.
     *
     * @param testMethod The test to get the response for.
     *
     * @return the message associated with the response
     */
    final String getResponseFor(final String testMethod) throws Exception {
        final URL url = getUrl(testMethod);
        Log.info(c, testMethod, "Calling application with URL=" + url.toString());
        // Note: Uses HttpUtils
        final HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);

        final String response = getAnyResponse(con);
        Log.info(c, testMethod, "Response from " + servletName + ": " + response);

        return response;
    }

    /**
     * @param test
     * @return
     * @throws MalformedURLException
     */
    private URL getUrl(final String test) throws MalformedURLException {
        final URL url = new URL(String.format("http://%s:%d/%s/%s?testMethod=%s", server.getHostname(), server.getHttpDefaultPort(), appName,
                                              servletName, test));
        return url;
    }

    /**
     * This method is used to read the response from either the input stream or
     * error stream.
     *
     * @param con The connection to the HTTP address
     * @return The output or error from the webpage
     * @throws IOException if neither the input stream nor error stream can be read
     */
    private static String getAnyResponse(final HttpURLConnection urlConnection) throws IOException {
        BufferedReader br;
        try {
            br = HttpUtils.getConnectionStream(urlConnection);
        } catch (final IOException ioex) {
            br = HttpUtils.getErrorStream(urlConnection);
        }
        return toSingleString(br);
    }

    /**
     * Read a buffer line-by-line and concatenate into a String.
     *
     * @param br
     * @return
     * @throws IOException
     */
    private static String toSingleString(BufferedReader br) throws IOException {
        String response = "";
        String aLine;

        while ((aLine = br.readLine()) != null) {
            response = response + aLine;
        }
        return response;
    }

    @Override
    public Statement apply(Statement base, Description description) {
       // logInfo("Before test", description);
        Log.info(c, "apply", "Before test" + description.getMethodName());
        try {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    base.evaluate();
                }
            };
        } finally {
            Log.info(c, "apply", "After test" + description.getMethodName());
          //  logInfo("After test", description);
        }
    }
}
