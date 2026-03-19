/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
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
    private static final String serverXmlRelativePath = "usr/servers/" + SERVER_NAME + "/server.xml";
    private static String serverXmlFilePath;

    private static LibertyServer server;
    private static final String ENTERING = ">>>>>>>  --------------------- >>>>>>>";
    private static final String EXITING = "<<<<<<< ---------------------  <<<<<<<";

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeClass() {
        Utils.backupFile(serverXmlRelativePath);
    }

    @AfterClass
    public static void afterClass() {
        Utils.restoreFileFromBackup(serverXmlRelativePath);
    }

    @Before
    public void before() {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        serverXmlFilePath = server.getInstallRoot() + "/" + serverXmlRelativePath;
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
        Log.info(c, METHOD_NAME, ENTERING);

        startServer();
        stopServer("", "CWWKE0036I", MESSAGES); // CWWKE0036I: The server <SERVER NAME> stopped after ...

        Log.info(c, METHOD_NAME, EXITING);
    }

    /**
     * Test - Normal case, with valid --timeout option. Should work.
     */
    @Test
    public void testServerStopWithTimeout_GoodArg() throws Exception {
        final String METHOD_NAME = "testServerStopWithTimeout_GoodArg";
        Log.info(c, METHOD_NAME, ENTERING);

        startServer();
        stopServer("--timeout=60", "CWWKE0036I", MESSAGES); // CWWKE0036I: The server <SERVER NAME> stopped after ...

        Log.info(c, METHOD_NAME, EXITING);
    }

    /**
     * Test - Provide bad argument to --timeout option. Expect failure.
     */
    @Test
    public void testServerStopWithTimeout_BadArg() throws Exception {
        final String METHOD_NAME = "testServerStopWithTimeout_BadArg";
        Log.info(c, METHOD_NAME, ENTERING);

        startServer();
        stopServer("--timeout=garbage", "CWWKE0024E", STDOUT);

        Log.info(c, METHOD_NAME, EXITING);
    }

    /**
     * Test - --timeout option on the start command.
     * That should fail, since --timeout is only supported for stop.
     */
    @Test
    public void testServerStartWithTimeoutArg() throws Exception {
        final String METHOD_NAME = "testServerStartWithTimeoutArg";
        Log.info(c, METHOD_NAME, ENTERING);

        String expectedMessage;

        if (OS.contains("win")) {
            expectedMessage = "start failed.";
        } else {
            expectedMessage = "CWWKE0028E";
        }

        startServer("--timeout=120s", expectedMessage, STDOUT, false);

        Log.info(c, METHOD_NAME, EXITING);
    }

    static final String STDOUT = "stdout";
    static final String MESSAGES = "messages.log";

    public void startServer() throws Exception {
        startServer(null, null, null);
    }

    public void startServer(String commandLineOption, String expectedOutput, String locationOfOutput) throws Exception {
        startServer(commandLineOption, expectedOutput, locationOfOutput, true);
    }

    public void startServer(String commandLineOption, String expectedOutput, String locationOfOutput, boolean expectedToStart) throws Exception {
        final String METHOD_NAME = "startServer";
        Log.info(c, METHOD_NAME, ">");

        //------------------//
        //  SERVER START    //
        //------------------//
        // Execute the server start command and display stdout and stderr
        String executionDir = server.getInstallRoot() + File.separator + "bin";
        String command = "." + File.separator + "server";
        String[] parms = new String[3];
        parms[0] = "start";
        parms[1] = SERVER_NAME;
        parms[2] = "";
        if (commandLineOption != null) {
            parms[2] = commandLineOption;
        }
        ProgramOutput po = server.getMachine().execute(command, parms, executionDir);
        String standardOutput = po.getStdout();
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
        if (expectedToStart) {
            assertTrue("the server should have been started", server.isStarted());
        } else {
            assertFalse("Server start should have failed, but did not.", server.isStarted());
        }

        if (expectedOutput != null) {
            String location = STDOUT;
            if (locationOfOutput != null) {
                location = locationOfOutput;
            }
            // Check for expected output
            if (location.equals(STDOUT)) {
                assertTrue("Expected message [ " + expectedOutput + " ] was not found in [" + STDOUT + "]",
                           standardOutput.contains(expectedOutput));

            } else {
                String serverStopped = server.waitForStringInLog(expectedOutput);
                assertNotNull("Timed out waiting for message, [ " + expectedOutput + " ]",
                              serverStopped);
            }
        }
        Log.info(c, METHOD_NAME, "<");
    }

    public void stopServer() throws Exception {
        stopServer(null, null, null);
    }

    public void stopServer(String commandLineOption, String expectedOutput, String locationOfOutput) throws Exception {
        final String METHOD_NAME = "stopServer";
        Log.info(c, METHOD_NAME, ">");

        //------------------//
        //  SERVER STOP     //
        //------------------//
        // Execute the server stop command and display stdout and stderr
        String executionDir = server.getInstallRoot() + File.separator + "bin";
        String command = "." + File.separator + "server";
        String[] parms = new String[3];
        parms[0] = "stop";
        parms[1] = SERVER_NAME;
        parms[2] = "";
        if (commandLineOption != null) {
            parms[2] = commandLineOption;
        }
        ProgramOutput po = server.getMachine().execute(command, parms, executionDir);
        String standardOutput = po.getStdout();
        Log.info(c, METHOD_NAME, "server stop stdout = " + standardOutput);
        Log.info(c, METHOD_NAME, "server stop stderr = " + po.getStderr());

        server.resetStarted();

        if (expectedOutput != null) {
            String location = STDOUT;
            if (locationOfOutput != null) {
                location = locationOfOutput;
            }
            // Check for expected output
            if (location.equals(STDOUT)) {
                assertTrue("Expected message[ " + expectedOutput + " ] was not found in [" + STDOUT + "]",
                           standardOutput.contains(expectedOutput));

            } else {
                String serverStopped = server.waitForStringInLog(expectedOutput);
                assertNotNull("Timed out waiting for expected message, [ " + expectedOutput + " ]",
                              serverStopped);
            }
        }
        Log.info(c, METHOD_NAME, "<");
    }

    public String getServerXmlContents(String timeout) {
        return "<server>\n" +
               "    <include location=\"../fatTestPorts.xml\"/>\n" +
               "    <executor quiesceTimeout=\"" + timeout + "\"/>\n" +
               "</server>";
    }

    private static final Pattern timePattern = Pattern.compile("Waiting for up to (\\d+) seconds");

    public static String extractTimeValue(String logMessage) {
        Matcher matcher = timePattern.matcher(logMessage);

        if (matcher.find()) {
            String timeValueStr = matcher.group(1);
            return timeValueStr;
        } else {
            return null;
        }
    }
}
