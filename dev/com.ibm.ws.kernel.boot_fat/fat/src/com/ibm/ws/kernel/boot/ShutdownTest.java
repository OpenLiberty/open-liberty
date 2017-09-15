/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class ShutdownTest {
    private static final Class<?> c = ShutdownTest.class;

    @Rule
    public final TestName testName = new TestName();

    LibertyServer server;

    @Before
    public void before() {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.shutdown.fat");
    }

    @After
    public void after() throws Exception {
        // We stop the server by other means, so wait for that stop to finish,
        // and then call stopServer to save logs, reset log offsets, etc.
        server.waitForStringInLog("CWWKE0036I");
        server.stopServer();
    }

    private void runTest(String exitMethodName) throws Exception {
        final String m = testName.getMethodName();
        Log.entering(c, m);
        try {
            server.startServer(m + ".log");

            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/shutdownfat?exit=" + exitMethodName);
            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                Log.info(c, m, "HTTP response: " + con.getResponseCode());

                InputStream in = con.getInputStream();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                    for (String line; (line = reader.readLine()) != null;) {
                        Log.info(c, m, "Output: " + line);
                    }
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        Log.error(c, m, e);
                    }
                }
            } catch (Throwable t) {
                // The server might die before the response can be written.
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                Log.info(ShutdownTest.class, "testSystemExit", "Ignoring " + sw.toString());
            }

            server.waitForStringInLog("CWWKE0084I:.*" + exitMethodName);
        } finally {
            Log.exiting(c, m);
        }
    }

    @Test
    public void testSystemExit() throws Exception {
        runTest("System.exit");
    }

    @Test
    public void testRuntimeExit() throws Exception {
        runTest("Runtime.exit");
    }
}
