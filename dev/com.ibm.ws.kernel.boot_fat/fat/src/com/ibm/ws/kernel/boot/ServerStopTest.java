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

    ///////  BEGIN QUIESE TESTS

    /**
     * Test - Quiesce NOT configured on server element - Beta Mode.
     * Ensure default quiesce timeout is used when quiesceTimeout not configured.
     * Starts & Stops the server and verifies that the expected timeout value is in
     * the quiesce message in the logs.
     */
    @Test
    public void testQuiesceTimeDefaultBetaMode() throws Exception {
        final String METHOD_NAME = "testQuiesceTimeDefaultBetaMode()";
        Log.info(c, METHOD_NAME, ENTERING);
        
        // Enable beta mode
        server.setJvmOptions(java.util.Arrays.asList("-Dcom.ibm.ws.beta.edition=true"));
        
        assertTrue("Default quiesce timeout should be 30 seconds in beta mode", runQuiesceTest("30"));
        Log.info(c, METHOD_NAME, EXITING);
    }

    /**
     * Test - Quiesce NOT configured on server element - Non-Beta Mode.
     * Ensure default quiesce timeout is used when quiesceTimeout not configured.
     */
    @Test
    public void testQuiesceTimeDefaultNonBetaMode() throws Exception {
        final String METHOD_NAME = "testQuiesceTimeDefaultNonBetaMode()";
        Log.info(c, METHOD_NAME, ENTERING);
        
        // Disable beta mode
        server.setJvmOptions(java.util.Collections.emptyList());
        
        assertTrue("Default quiesce timeout should be 30 seconds in non-beta mode", runQuiesceTest("30"));
        Log.info(c, METHOD_NAME, EXITING);
    }

    /**
     * Test - Quiesce configured on server element but NOT valid - Beta Mode.
     * Ensure default quiesce timeout is used when quiesceTimeout value is NOT valid.
     */
    @Test
    public void testQuiesceTimeNotValidBetaMode() throws Exception {
        final String METHOD_NAME = "testQuiesceTimeNotValidBetaMode()";
        Log.info(c, METHOD_NAME, ENTERING);

        // Enable beta mode
        server.setJvmOptions(java.util.Arrays.asList("-Dcom.ibm.ws.beta.edition=true"));
        
        Utils.createFile(serverXmlFilePath, getServerXmlContents("XXXXX"));
        assertTrue("Quiesce timeout not valid. Should use default 30 seconds in beta mode", runQuiesceTest("30"));
        Log.info(c, METHOD_NAME, EXITING);
    }

    /**
     * Test - Quiesce configured on server element but LESS than minimum - Beta Mode.
     * Ensure default quiesce timeout is used when quiesceTimeout value is LESS than minimum (30).
     */
    @Test
    public void testQuiesceTimeValueLessThanMinimumBetaMode() throws Exception {
        final String METHOD_NAME = "testQuiesceTimeValueLessThanMinimumBetaMode()";
        Log.info(c, METHOD_NAME, ENTERING);
        
        // Enable beta mode
        server.setJvmOptions(java.util.Arrays.asList("-Dcom.ibm.ws.beta.edition=true"));
        
        Utils.createFile(serverXmlFilePath, getServerXmlContents("15"));
        assertTrue("Quiesce timeout below minimum should use default 30 seconds in beta mode", runQuiesceTest("30"));
        Log.info(c, METHOD_NAME, EXITING);
    }

    /**
     * Test - Quiesce configured on server element and is GREATER than default - Beta Mode.
     * Ensure the configured quiesce timeout is used when quiesceTimeout value is valid and GREATER than default.
     */
    @Test
    public void testQuiesceTimeValueGreaterThanDefaultBetaMode() throws Exception {
        final String METHOD_NAME = "testQuiesceTimeValueGreaterThanDefaultBetaMode()";
        Log.info(c, METHOD_NAME, ENTERING);
        
        // Enable beta mode
        server.setJvmOptions(java.util.Arrays.asList("-Dcom.ibm.ws.beta.edition=true"));
        
        Utils.createFile(serverXmlFilePath, getServerXmlContents("1m30s"));
        assertTrue("Valid quiesce timeout should be used (90 seconds) in beta mode", runQuiesceTest("90"));
        Log.info(c, METHOD_NAME, EXITING);
    }

    /**
     * Test - Quiesce configured on server element and is GREATER than default - Non-Beta Mode.
     * Ensure the quiesceTimeout attribute is ignored when beta mode is disabled.
     */
    @Test
    public void testQuiesceTimeValueGreaterThanDefaultNonBetaMode() throws Exception {
        final String METHOD_NAME = "testQuiesceTimeValueGreaterThanDefaultNonBetaMode()";
        Log.info(c, METHOD_NAME, ENTERING);
        
        // Disable beta mode
        server.setJvmOptions(java.util.Collections.emptyList());
        
        Utils.createFile(serverXmlFilePath, getServerXmlContents("1m30s"));
        assertTrue("Quiesce timeout attribute ignored in non-beta mode should use default 30 seconds", runQuiesceTest("30"));
        Log.info(c, METHOD_NAME, EXITING);
    }

    // -----

    public boolean runQuiesceTest(String expectedResult) throws Exception {
        final String METHOD_NAME = "runQuiesceTest";
        final String quiesceMessage = "CWWKE1100I";

        startServer();
        stopServer();

        RemoteFile consoleLog = server.getConsoleLogFile();

        if (consoleLog == null) {
            Log.info(c, METHOD_NAME, "The consoleLog is null.");
        } else {
            Log.info(c, METHOD_NAME, "consoleLog Path [" + consoleLog.getAbsolutePath() + "]");
        }

        List<String> matches = server.findStringsInLogs(quiesceMessage, consoleLog);
        if (matches == null) {
            Log.info(c, METHOD_NAME, "matches is null");
        }

        String lastMatch = null;
        for (String s : matches) {
            Log.info(c, METHOD_NAME, "matches [" + s + "]");
            lastMatch = s;
        }
        Log.info(c, METHOD_NAME, "lastMatch [" + lastMatch + "]");
        if (lastMatch != null) {
            String actualResult = extractTimeValue(lastMatch);
            if (actualResult != null) {
                Log.info(c, METHOD_NAME, "returning  - actual result is [" + actualResult + "]");
                return actualResult.equals(expectedResult);
            }
            Log.info(c, METHOD_NAME, "Problem extracting time from quiesce message [" + lastMatch + "]");
        } else {
            Log.info(c, METHOD_NAME, "Quiesce message" + "[" + quiesceMessage + "]" + "not found in " + consoleLog.getAbsolutePath());
        }
        Log.info(c, METHOD_NAME, "returning false");
        return false;
    }

    ///////  END SERVER ELEMENT QUIESCE TESTS

    public String getServerXmlContents(String timeout) {
        return "<server quiesceTimeout=\"" + timeout + "\">\n" +
               "    <include location=\"../fatTestPorts.xml\"/>\n" +
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
