/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class UlimitTest {

    private static final Class<?> c = UlimitTest.class;

    private final String DEFAULT_ULIMIT = "4096";
    private final String NON_DEFAULT_ULIMIT = "1234";
    private final String WLP_FILE_SOFT_LIMIT = "WLP_FILE_SOFT_LIMIT";
    private final String SERVER_NAME = "com.ibm.ws.kernel.boot.ulimit.fat";

    //@Rule
    //public final TestName testName = new TestName();

    LibertyServer server;

    @Before
    public void before() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        ShrinkHelper.defaultApp(server, "ulimitApp", "com.ibm.ws.kernel.boot.ulimit.fat");
    }

    @After
    public void after() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }

    private String runTest(String exitMethodName) throws Exception {
        final String m = exitMethodName;
        StringBuffer output = new StringBuffer();
        Log.entering(c, m);

        try {

            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/ulimitApp?exit=" + exitMethodName);
            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                Log.info(c, m, "HTTP response: " + con.getResponseCode());

                InputStream in = con.getInputStream();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                    for (String line; (line = reader.readLine()) != null;) {
                        Log.info(c, m, "Output: " + line);
                        output.append(line);
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
                Log.info(UlimitTest.class, "UlimitTest", "Ignoring " + sw.toString());
            }

            return output.toString();
        } finally {
            Log.exiting(c, m);
        }
    }

    public void startServerWithEnvVar(String runType, String ulimit) throws Exception {
        String METHOD_NAME = "startServerWithEnvVar";

        Log.entering(c, METHOD_NAME, "entering startServerWithEnvVar");

        // ulimit only valid for Unix based systems...
        OperatingSystem oS = server.getMachine().getOperatingSystem();
        if (oS != OperatingSystem.WINDOWS) {

            String executionDir = server.getInstallRoot();
            String command = "bin" + File.separator + "server";

            String[] parms = new String[2];
            parms[0] = runType;
            parms[1] = SERVER_NAME;

            Properties envVars = new Properties();
            if (ulimit != null) {
                envVars.put(WLP_FILE_SOFT_LIMIT, ulimit);
            }

            ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
            Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
            Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());
            server.waitForStringInLog("CWWKF0011I");
        } else {
            assumeTrue(false);
            Log.info(c, METHOD_NAME, "Skipping test.  Non-Unix platform.");
        }

        Log.exiting(c, METHOD_NAME, "exiting startServerWithEnvVar");

    }

    @Test
    public void testEnvVarUlimitServerSTART() throws Exception {
        final String METHOD_NAME = "testEnvVarUlimitServerSTART";
        Log.entering(c, METHOD_NAME);

        // Set ulimit to '1234' via env variable
        startServerWithEnvVar("start", "1234");

        String s = runTest(METHOD_NAME);
        assertTrue("The output should contain a value of " + NON_DEFAULT_ULIMIT + " but contains = " + s,
                   s.contains(NON_DEFAULT_ULIMIT));

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();
        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testUlimitDefaultServerSTART() throws Exception {

        String METHOD_NAME = "testUlimitDefaultServerSTART";

        Log.entering(c, METHOD_NAME);

        startServerWithEnvVar("start", null);

        String s = runTest(METHOD_NAME);

        assertTrue("The output should contain a value of " + DEFAULT_ULIMIT + " but contains = " + s,
                   s.contains(DEFAULT_ULIMIT));

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();
        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

}
