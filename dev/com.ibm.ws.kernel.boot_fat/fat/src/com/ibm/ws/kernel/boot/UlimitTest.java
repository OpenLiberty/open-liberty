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
    private final String NON_DEFAULT_ULIMIT_LOW = "1234";
    private final String NON_DEFAULT_ULIMIT_HIGH = "5000";
    private final String WLP_FILE_SOFT_LIMIT = "WLP_FILE_SOFT_LIMIT";
    private final String SERVER_NAME = "com.ibm.ws.kernel.boot.ulimit.fat";

    LibertyServer server;

    @Before
    public void before() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        ShrinkHelper.defaultApp(server, "ulimitApp", "com.ibm.ws.kernel.boot.ulimit.fat");
    }

    @After
    public void after() throws Exception {

        unsetWLPEnvVariable();

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

    /**
     * Sets the ulimit then starts the server all in the same shell.
     *
     * @param ulimit
     * @param wlpFileSoftLimit
     * @throws Exception
     */
    public void startServerWithUlimitOnShell(String ulimit, String wlpFileSoftLimit) throws Exception {
        String METHOD_NAME = "startServerWithUlimitOnShell";

        Log.entering(c, METHOD_NAME);

        // ulimit is only valid for Unix based systems...
        OperatingSystem oS = server.getMachine().getOperatingSystem();
        if (oS != OperatingSystem.WINDOWS) {

            ProgramOutput po = null;

            String executionDir = server.getInstallRoot();

            // To set the ulimit, the cmd has to be run under the same shell that starts
            // the server.  Thus we're doing "cmd ; cmd" with one execute() method to
            // accomplish this task.
            String ulimitCommand = "ulimit";
            String[] ulimitParms = new String[6];
            ulimitParms[0] = "-n";
            ulimitParms[1] = ulimit;
            ulimitParms[2] = ";";
            ulimitParms[3] = "bin" + File.separator + "server";
            ulimitParms[4] = "start";
            ulimitParms[5] = SERVER_NAME;

            String cmd = ulimitCommand;
            for (int i = 0; i < ulimitParms.length; i++) {
                cmd = cmd + " " + ulimitParms[i];
            }

            Properties envVars = new Properties();
            if (wlpFileSoftLimit != null) {
                envVars.put(WLP_FILE_SOFT_LIMIT, wlpFileSoftLimit);
                po = server.getMachine().execute(ulimitCommand, ulimitParms, executionDir, envVars);
            } else {
                po = server.getMachine().execute(ulimitCommand, ulimitParms, executionDir);
            }

            Log.info(c, METHOD_NAME, "********************************");
            Log.info(c, METHOD_NAME, "** cmd string = " + cmd);
            Log.info(c, METHOD_NAME, "** stdout = " + po.getStdout());
            Log.info(c, METHOD_NAME, "** stderr = " + po.getStderr());
            Log.info(c, METHOD_NAME, "** rc = " + po.getReturnCode());
            Log.info(c, METHOD_NAME, "********************************");

            server.waitForStringInLog("CWWKF0011I");
        } else {
            assumeTrue(false);
            Log.info(c, METHOD_NAME, "Skipping.  Non-Unix platform.");
        }

        Log.exiting(c, METHOD_NAME);

    }

    public void unsetWLPEnvVariable() throws Exception {
        String METHOD_NAME = "unsetWLPEnvVariable";

        Log.entering(c, METHOD_NAME);

        // unset is only valid for Unix based systems...
        OperatingSystem oS = server.getMachine().getOperatingSystem();
        if (oS != OperatingSystem.WINDOWS) {

            ProgramOutput po = null;

            String executionDir = server.getInstallRoot();

            // To set the ulimit, the cmd has to be run under the same shell that starts
            // the server.  Thus we're doing "cmd ; cmd" with one execute() method to
            // accomplish this task.
            String unsetCommand = "unset";
            String[] unsetParms = new String[1];
            unsetParms[0] = WLP_FILE_SOFT_LIMIT;

            po = server.getMachine().execute(unsetCommand, unsetParms, executionDir);

            Log.info(c, METHOD_NAME, "********************************");
            Log.info(c, METHOD_NAME, "** " + unsetCommand + " " + WLP_FILE_SOFT_LIMIT + " **");
            Log.info(c, METHOD_NAME, "** stdout = " + po.getStdout());
            Log.info(c, METHOD_NAME, "** stderr = " + po.getStderr());
            Log.info(c, METHOD_NAME, "** rc = " + po.getReturnCode());
            Log.info(c, METHOD_NAME, "********************************");

            server.waitForStringInLog("CWWKF0011I");
        } else {
            assumeTrue(false);
            Log.info(c, METHOD_NAME, "Skipping.  Non-Unix platform.");
        }

        Log.exiting(c, METHOD_NAME);

    }

    /**
     * Tests that when WLP_FILE_SOFT_LIMIT is set, it is utilized regardless of other
     * settings. This is the override.
     *
     * @throws Exception
     */
    @Test
    public void testEnvVariableOverride() throws Exception {
        final String METHOD_NAME = "testEnvVariableOverride";
        Log.entering(c, METHOD_NAME);

        // Set ulimit to '1234' via env variable
        startServerWithUlimitOnShell(NON_DEFAULT_ULIMIT_HIGH, NON_DEFAULT_ULIMIT_LOW);

        String s = runTest(METHOD_NAME);
        assertTrue("The output should contain a value of " + NON_DEFAULT_ULIMIT_LOW + " but contains = " + s,
                   s.contains(NON_DEFAULT_ULIMIT_LOW));

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();
        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that when the ulimit -n value is less that 4096 it gets bumped to 4096
     * by default via the server script.
     *
     * @throws Exception
     */
    @Test
    public void testUlimitDefault() throws Exception {

        String METHOD_NAME = "testUlimitDefault";

        Log.entering(c, METHOD_NAME);

        startServerWithUlimitOnShell(NON_DEFAULT_ULIMIT_LOW, null);

        String s = runTest(METHOD_NAME);

        assertTrue("The output should contain a value of " + DEFAULT_ULIMIT + " but contains = " + s,
                   s.contains(DEFAULT_ULIMIT));

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();
        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that when the ulimit -n value is greater than 4096, it remains at the original value
     * picked up from the shell from which the server was started.
     *
     * @throws Exception
     */
    @Test
    public void testLargerUlimitSetByUser() throws Exception {

        String METHOD_NAME = "testLargerUlimitSetByUser";

        Log.entering(c, METHOD_NAME);

        // Start server
        startServerWithUlimitOnShell(NON_DEFAULT_ULIMIT_HIGH, null);

        String s = runTest(METHOD_NAME);

        assertTrue("The output should contain a value of " + NON_DEFAULT_ULIMIT_HIGH + " but contains = " + s,
                   s.contains(NON_DEFAULT_ULIMIT_HIGH));

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();
        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

}
