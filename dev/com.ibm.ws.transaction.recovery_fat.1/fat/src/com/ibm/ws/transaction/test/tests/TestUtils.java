/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
public class TestUtils {

    public static final int LOG_SEARCH_TIMEOUT = 300000;

    public static void recoveryTest(LibertyServer server, String servletName, String id) throws Exception {
        recoveryTest(server, server, servletName, id);
    }

    public static void recoveryTest(LibertyServer crashingServer, LibertyServer recoveringServer, String servletName, String id) throws Exception {
        final String method = "recoveryTest";

        try {
            // We expect this to fail since it is gonna crash the server
            FATServletClient.runTest(crashingServer, servletName, "setupRec" + id);
            restartServer(crashingServer);
            fail(crashingServer.getServerName() + " did not crash as expected");
        } catch (Exception e) {
            Log.info(TestUtils.class, method, "setupRec" + id + " crashed as expected");
            Log.error(TestUtils.class, method, e);
        }

        assertNotNull(crashingServer.getServerName() + " didn't crash properly", crashingServer.waitForStringInLog("Dump State:"));

        ProgramOutput po = recoveringServer.startServerAndValidate(false, true, true);
        if (po.getReturnCode() != 0) {
            Log.info(TestUtils.class, method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(TestUtils.class, method, "Stdout: " + po.getStdout());
            Log.info(TestUtils.class, method, "Stderr: " + po.getStderr());

            // It may be that we attempted to restart the server too soon.
            Log.info(TestUtils.class, method, "start server failed, sleep then retry");
            Thread.sleep(30000); // sleep for 30 seconds
            po = recoveringServer.startServerAndValidate(false, true, true);
            // If it fails again then we'll report the failure
            if (po.getReturnCode() != 0) {
                Log.info(TestUtils.class, method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(TestUtils.class, method, "Stdout: " + po.getStdout());
                Log.info(TestUtils.class, method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not restart the server");
                Log.error(TestUtils.class, method, ex);
                throw ex;
            }
        }

        // Server appears to have started ok
        assertNotNull(recoveringServer.getServerName() + " didn't recover properly", recoveringServer.waitForStringInTrace("Setting state from RECOVERING to ACTIVE"));

        int attempt = 0;
        while (true) {
            Log.info(TestUtils.class, method, "calling checkRec" + id);
            try {
                final StringBuilder sb = runTestWithResponse(recoveringServer, servletName, "checkRec" + id);
                Log.info(TestUtils.class, method, "checkRec" + id + " returned: " + sb);
                break;
            } catch (Exception e) {
                Log.error(TestUtils.class, method, e);
                if (++attempt < 5) {
                    Thread.sleep(10000);
                } else {
                    // Something is seriously wrong with this server instance. Reset so the next test has a chance
                    restartServer(recoveringServer);
                    throw e;
                }
            }
        }
    }

    private static void restartServer(LibertyServer s) {
        try {
            s.stopServer(".*");
            s.startServerAndValidate(false, false, true, false);
        } catch (Exception e) {
            Log.error(TestUtils.class, "restartServer", e);
        }
    }

    /**
     * Runs a test in the servlet and returns the servlet output.
     *
     * @param server      the started server containing the started application
     * @param path        the url path (e.g. myApp/myServlet)
     * @param queryString query string including at least the test name
     *                        (e.g. testName or testname&key=value&key=value)
     * @return output of the servlet
     */
    public static StringBuilder runTestWithResponse(LibertyServer server, String path, String queryString) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + FATServletClient.getPathAndQuery(path, queryString));
        Log.info(TestUtils.class, "runTestWithResponse", "URL is " + url);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(TestUtils.class, "runTestWithResponse", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf(FATServletClient.SUCCESS) < 0) {
                Log.info(TestUtils.class, "runTestWithResponse", "failed to find \"" + FATServletClient.SUCCESS + "\" in response");
                fail("Missing success message in output. " + lines);
            }
            return lines;
        } finally {
            con.disconnect();
        }
    }
}