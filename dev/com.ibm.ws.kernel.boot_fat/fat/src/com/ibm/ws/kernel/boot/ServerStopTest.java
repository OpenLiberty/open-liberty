/*******************************************************************************
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
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This test bucket tests the server stop command with options.
 */
@RunWith(FATRunner.class)
public class ServerStopTest {
    private static final Class<?> c = ServerStopTest.class;

    private static final String SERVER_NAME = "com.ibm.ws.kernel.boot.serverstart.fat";
    private static final String OS = System.getProperty("os.name").toLowerCase();

    private static LibertyServer server;

    @Rule
    public TestName testName = new TestName();

    @Before
    public void before() {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
    }

    @After
    public void after() throws Exception {
        if (server.isStarted()) {
            stopServer();
        }
    }

    /**
     * Test - Normal case. No options Should work.
     */
    @Test
    public void testServerStop() throws Exception {
        final String METHOD_NAME = "testServerStop";
        Log.entering(c, METHOD_NAME);

        startAndStopServer("", "CWWKE0036I", MESSAGES); // CWWKE0036I: The server <SERVER NAME> stopped after ...

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test - Normal case, with valid --timeout option. Should work.
     */
    @Test
    public void testServerStopWithTimeout_GoodArg() throws Exception {
        final String METHOD_NAME = "testServerStopWithTimeout_GoodArg";
        Log.entering(c, METHOD_NAME);

        startAndStopServer("--timeout=60", "CWWKE0036I", MESSAGES); // CWWKE0036I: The server <SERVER NAME> stopped after ...

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test - Provide bad argument to --timeout option. Expect failure.
     */
    @Test
    public void testServerStopWithTimeout_BadArg() throws Exception {
        final String METHOD_NAME = "testServerStopWithTimeout_BadArg";
        Log.entering(c, METHOD_NAME);

        startAndStopServer("--timeout=garbage", "CWWKE0024E", STDOUT);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test - --timeout option on the start command.
     * That should fail, since --timeout is only supported for stop.
     */
    @Test
    public void testServerStartWithTimeoutArg() throws Exception {
        final String METHOD_NAME = "testServerStartWithTimeoutArg";
        Log.entering(c, METHOD_NAME);

        String expectedMessage;

        if (OS.contains("win")) {
            expectedMessage = "start failed.";
        } else {
            expectedMessage = "CWWKE0028E";
        }

        //------------------//
        //  SERVER START    //
        //------------------//
        // Execute the server start command and display stdout and stderr
        String executionDir = server.getInstallRoot() + File.separator + "bin";
        String command = "." + File.separator + "server";
        String[] parms = new String[3];
        parms[0] = "start";
        parms[1] = SERVER_NAME;
        parms[2] = "--timeout=120s";
        ProgramOutput po = server.getMachine().execute(command, parms, executionDir);
        String standardOutput = po.getStdout();
        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        assertTrue("sever start expected to fail, but didn't. Message [ " + expectedMessage + " ] not found in [" + STDOUT + "]",
                   standardOutput.contains(expectedMessage));
        Log.info(c, METHOD_NAME, "PASSED");

        Log.exiting(c, METHOD_NAME);
    }

    static final String STDOUT = "stdout";
    static final String MESSAGES = "messages.log";

    /**
     *
     * @param option
     * @param expectedOutput
     * @param where          - where to search output. "stdout" or "messages.log"
     * @throws Exception
     */
    public void startAndStopServer(String option, String expectedOutput, String where) throws Exception {
        final String METHOD_NAME = "startAndStopServer[" + option + ", " + expectedOutput + "]";
        Log.entering(c, METHOD_NAME);
        Log.info(c, METHOD_NAME, "option [" + option + "]");
        Log.info(c, METHOD_NAME, "expectedOutput [" + expectedOutput + "]");

        //------------------//
        //  SERVER START    //
        //------------------//
        // Execute the server start command and display stdout and stderr
        String executionDir = server.getInstallRoot() + File.separator + "bin";
        String command = "." + File.separator + "server";
        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;
        ProgramOutput po = server.getMachine().execute(command, parms, executionDir);
        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        // Check for server ready
        String serverReady = server.waitForStringInLog("CWWKE0002I");
        if (serverReady == null) {
            //Log.info(c, METHOD_NAME, "Timed out waiting for server ready message, CWWKF0011I");
            Log.info(c, METHOD_NAME, "'The kernel started after' message, CWWKF0011I");
        }

        // Because we didn't start the server using the LibertyServer APIs, we need
        // to have it detect its started state so it will stop and save logs properly
        server.resetStarted();
        assertTrue("the server should have been started", server.isStarted());

        //------------------//
        //  SERVER STOP     //
        //------------------//
        // Execute the server stop command and display stdout and stderr
        executionDir = server.getInstallRoot() + File.separator + "bin";
        command = "." + File.separator + "server";
        parms = new String[3];
        parms[0] = "stop";
        parms[1] = SERVER_NAME;
        parms[2] = option; // for example "--timeout=30"
        po = server.getMachine().execute(command, parms, executionDir);
        String standardOutput = po.getStdout();
        Log.info(c, METHOD_NAME, "server stop stdout = " + standardOutput);
        Log.info(c, METHOD_NAME, "server stop stderr = " + po.getStderr());

        // Check for expected output
        if (where.equals(STDOUT)) {
            assertTrue("sever stop expected to fail, but didn't. Message [ " + expectedOutput + " ] not found in [" + STDOUT + "]",
                       standardOutput.contains(expectedOutput));
            Log.info(c, METHOD_NAME, "PASSED");

        } else {
            String serverStopped = server.waitForStringInLog(expectedOutput);
            assertNotNull("Timed out waiting for message, [ " + expectedOutput + " ]",
                          serverStopped);
            Log.info(c, METHOD_NAME, "PASSED");
        }

        Log.exiting(c, METHOD_NAME);
    }

    public void stopServer() throws Exception {
        final String METHOD_NAME = "startAndStopServer";
        Log.entering(c, METHOD_NAME);

        //------------------//
        //  SERVER STOP     //
        //------------------//
        // Execute the server stop command and display stdout and stderr
        String executionDir = server.getInstallRoot() + File.separator + "bin";
        String command = "." + File.separator + "server";
        String[] parms = new String[2];
        parms[0] = "stop";
        parms[1] = SERVER_NAME;
        ProgramOutput po = server.getMachine().execute(command, parms, executionDir);
        String standardOutput = po.getStdout();
        Log.info(c, METHOD_NAME, "server stop stdout = " + standardOutput);
        Log.info(c, METHOD_NAME, "server stop stderr = " + po.getStderr());

        Log.exiting(c, METHOD_NAME);
    }
}
