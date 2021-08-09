/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.timing.hung.fat;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

/**
 *
 * This test class contains test cases that test the Hung Request Timing server configuration attribute enableThreadDumps.
 *
 */
@RunWith(FATRunner.class)
public class HungRequestEnableThreadDumps {

    private static final String MESSAGE_LOG = "logs/messages.log";
    private static final String SERVER_NAME = "HungRequestTimingServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        JavaInfo java = JavaInfo.forCurrentVM();
        ShrinkHelper.defaultDropinApp(server, "TestWebApp", "com.ibm.testwebapp");
        int javaVersion = java.majorVersion();
        if (javaVersion != 8) {
            CommonTasks.writeLogMsg(Level.INFO, " Java version = " + javaVersion + " - It is higher than 8, adding --add-exports...");
            server.copyFileToLibertyServerRoot("add-exports/jvm.options");
        }
        CommonTasks.writeLogMsg(Level.INFO, " Starting server...");
        server.startServer();
    }

    @Before
    public void setupTest() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer();
        }

        // Allow the configuration to change back to the original and ensure the update is finished before starting a test
        server.setServerConfigurationFile("server_original.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);
        server.setMarkToEndOfLog();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("TRAS0114W", "TRAS0115W", "CWWKG0011W", "CWWKG0083W");
        }
    }

    /*
     * Tests when the boolean "enableThreadDumps" attribute is not specified, the thread dumps should be created when the hung request is detected.
     */
    @Test
    public void testEnableThreadDumpsNotSpecified() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "***** Begining testEnableThreadDumpsNotSpecified! *****");

        CommonTasks.writeLogMsg(Level.INFO, "------> Setting hung threshold as 2s");
        server.setServerConfigurationFile("server_hungRequestThreshold2.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        createHungRequest(5000);

        CommonTasks.writeLogMsg(Level.INFO, "------> Verifying if the hung detection warnings appeared...");
        verifyHungRequestWarnings();

        CommonTasks.writeLogMsg(Level.INFO, "------> Checking if any thread dumps are created...");
        checkThreadDumpsCreated(true); // thread dumps are expected.

        CommonTasks.writeLogMsg(Level.INFO, "***** testEnableThreadDumpsNotSpecified has completed! *****");
    }

    /*
     * Tests when the boolean attribute "enableThreadDumps=false", the thread dumps will not be created when the hung request is detected.
     */
    @Test
    public void testThreadDumpsDisabled() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "***** Begining testThreadDumpsDisabled! *****");

        CommonTasks.writeLogMsg(Level.INFO, "------> Setting hung threshold as 2s and enableThreadDumps=false");
        server.setServerConfigurationFile("server_hungRequestEnableThreadDumpsFalse.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        createHungRequest(5000);

        CommonTasks.writeLogMsg(Level.INFO, "------> Verifying if the hung detection warnings appeared...");
        verifyHungRequestWarnings();

        CommonTasks.writeLogMsg(Level.INFO, "------> Checking if any thread dumps are created...");
        checkThreadDumpsCreated(false); // thread dumps are NOT expected.

        CommonTasks.writeLogMsg(Level.INFO, "***** testThreadDumpsDisabled has completed! *****");
    }

    /*
     * Tests when the boolean attribute "enableThreadDumps" is set to false dynamically, after server starts, with no attribute specified.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testDynamicThreadDumpsDisable() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "***** Begining testDynamicThreadDumpsDisable! *****");

        CommonTasks.writeLogMsg(Level.INFO, "------> Setting hung threshold as 2s");
        server.setServerConfigurationFile("server_hungRequestThreshold2.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        createHungRequest(180000); // We must wait this long to see 3 java cores are generated (we expect only 3)

        CommonTasks.writeLogMsg(Level.INFO, "------> Verifying if the hung detection warnings appeared...");
        verifyHungRequestWarnings();

        CommonTasks.writeLogMsg(Level.INFO, "------> Checking if any thread dumps are created...");
        checkThreadDumpsCreated(true); // thread dumps are expected.

        server.setMarkToEndOfLog();

        CommonTasks.writeLogMsg(Level.INFO, "------> Updating server config to add enableThreadDumps=false");
        server.setServerConfigurationFile("server_hungRequestEnableThreadDumpsFalse.xml");
        server.waitForStringInLogUsingMark("CWWKG0017I", 90000);

        createHungRequest(5000);

        CommonTasks.writeLogMsg(Level.INFO, "------> Waiting for hung detection warning...");
        server.waitForStringInLog("TRAS0114W", 30000);

        CommonTasks.writeLogMsg(Level.INFO, "------> Waiting for hung request complete message...");
        server.waitForStringInLog("TRAS0115W", 30000);

        CommonTasks.writeLogMsg(Level.INFO, "------> Checking if any thread dumps are created...");
        checkThreadDumpsCreated(false); // thread dumps are NOT expected.

        CommonTasks.writeLogMsg(Level.INFO, "***** testDynamicThreadDumpsDisable has completed! *****");
    }

    /*
     * Tests when the boolean attribute "enableThreadDumps" is set to true dynamically, after server starts with "enableThreadDumps=false".
     */
    @Test
    @Mode(TestMode.FULL)
    public void testDynamicThreadDumpsEnable() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "***** Begining testDynamicThreadDumpsEnable! *****");

        CommonTasks.writeLogMsg(Level.INFO, "------> Setting hung threshold as 2s and enableThreadDumps=false");
        server.setServerConfigurationFile("server_hungRequestEnableThreadDumpsFalse.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        createHungRequest(180000); // We must wait this long to see 3 java cores are generated (we expect only 3)

        CommonTasks.writeLogMsg(Level.INFO, "------> Verifying if the hung detection warnings appeared...");
        verifyHungRequestWarnings();

        CommonTasks.writeLogMsg(Level.INFO, "------> Checking if any thread dumps are created...");
        checkThreadDumpsCreated(false); // thread dumps are NOT expected.

        server.setMarkToEndOfLog();

        CommonTasks.writeLogMsg(Level.INFO, "------> Updating server config to add enableThreadDumps=true");
        server.setServerConfigurationFile("server_hungRequestThreshold2.xml");
        server.waitForStringInLogUsingMark("CWWKG0017I", 90000);

        createHungRequest(5000);

        CommonTasks.writeLogMsg(Level.INFO, "------> Waiting for hung detection warning...");
        server.waitForStringInLog("TRAS0114W", 30000);

        CommonTasks.writeLogMsg(Level.INFO, "------> Waiting for hung request complete message...");
        server.waitForStringInLog("TRAS0115W", 30000);

        CommonTasks.writeLogMsg(Level.INFO, "------> Checking if any thread dumps are created...");
        checkThreadDumpsCreated(true); // thread dumps are expected.

        CommonTasks.writeLogMsg(Level.INFO, "***** testDynamicThreadDumpsEnable has completed! *****");
    }

    /*
     * Tests when the root requestTiming element has "enableThreadDumps=false" and the sub-element servletTiming has "enableThreadDumps=true".
     * The sub-element configuration should override the root element configuration, hence when a hung request is detected, thread dumps will be created.
     */
    @Test
    public void testGlobalThreadDumpsDisableLocalThreadDumpsEnable() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "***** Begining testGlobalThreadDumpsDisableLocalThreadDumpsEnable! *****");

        CommonTasks.writeLogMsg(Level.INFO, "------> Setting hung threshold as 2s and enableThreadDumps=false in the requestTiming root element.");
        server.setServerConfigurationFile("server_globalThreadDumpsFalse_localThreadDumpsTrue.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        createHungRequest(5000);

        CommonTasks.writeLogMsg(Level.INFO, "------> Verifying if the hung detection warnings appeared...");
        verifyHungRequestWarnings();

        CommonTasks.writeLogMsg(Level.INFO, "------> Checking if any thread dumps are created...");
        checkThreadDumpsCreated(true); // thread dumps are expected.

        CommonTasks.writeLogMsg(Level.INFO, "***** testGlobalThreadDumpsDisableLocalThreadDumpsEnable has completed! *****");
    }

    /*
     * Tests when the root requestTiming element does not have the "enableThreadDumps" attribute specified and the sub-element servletTiming has "enableThreadDumps=false".
     * The sub-element configuration should override the root element configuration, hence when a hung request is detected, thread dumps will not be created.
     */
    @Test
    public void testGlobalThreadDumpsEnableLocalThreadDumpsDisable() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "***** Begining testGlobalThreadDumpsEnableLocalThreadDumpsDisable! *****");

        CommonTasks.writeLogMsg(Level.INFO, "------> Setting hung threshold as 2s and enableThreadDumps=false in the requestTiming sub-element.");
        server.setServerConfigurationFile("server_globalThreadDumpsTrue_localThreadDumpsFalse.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        createHungRequest(5000);

        CommonTasks.writeLogMsg(Level.INFO, "------> Verifying if the hung detection warnings appeared...");
        verifyHungRequestWarnings();

        CommonTasks.writeLogMsg(Level.INFO, "------> Checking if any thread dumps are created...");
        checkThreadDumpsCreated(false); // thread dumps are NOT expected.

        CommonTasks.writeLogMsg(Level.INFO, "***** testGlobalThreadDumpsEnableLocalThreadDumpsDisable has completed! *****");
    }

    /*
     * Tests when the boolean attribute value ("enableThreadDumps=flase") is specified incorrectly in the server configuration.
     * The default configuration will be used ("enableThreadDumps=true"), where thread dumps will be created.
     */
    @Test
    public void testInvalidEnableThreadDumpsAttributeValue() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "***** Begining testInvalidEnableThreadDumpsAttributeValue! *****");

        CommonTasks.writeLogMsg(Level.INFO, "------> Setting hung threshold as 2s and invalid enableThreadDumps=flase attribute");
        server.setServerConfigurationFile("server_hungRequestInvalidEnableThreadDumpsFalse.xml");

        CommonTasks.writeLogMsg(Level.INFO, "------> Waiting for server config validation not succeed warnings...");
        server.waitForStringInLog("CWWKG0011W", 30000);
        server.waitForStringInLog("CWWKG0083W", 30000);
        server.waitForStringInLog("CWWKG0017I", 90000);

        List<String> lines = server.findStringsInLogsUsingMark("CWWKG0011W", MESSAGE_LOG);
        assertTrue("Expected at least one, server config validation did not succeed warning but found : " + lines.size(), (lines.size() > 0));
        lines = server.findStringsInLogsUsingMark("CWWKG0083W", MESSAGE_LOG);
        assertTrue("Expected at least one, server config validation failure and default in use warning but found : " + lines.size(), (lines.size() > 0));

        createHungRequest(5000);

        CommonTasks.writeLogMsg(Level.INFO, "------> Verifying if the hung detection warnings appeared...");
        verifyHungRequestWarnings();

        CommonTasks.writeLogMsg(Level.INFO, "------> Checking if any thread dumps are created...");
        checkThreadDumpsCreated(true); // thread dumps are expected.

        CommonTasks.writeLogMsg(Level.INFO, "***** testInvalidEnableThreadDumpsAttributeValue has completed! *****");
    }

    private void checkThreadDumpsCreated(boolean threadDumpsEnabled) throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "----> Waiting for Thread dump request received message...");
        server.waitForStringInLog("CWWKE0067I", 30000);
        List<String> threadDumpRequestlines = server.findStringsInLogsUsingMark("CWWKE0067I", MESSAGE_LOG);

        CommonTasks.writeLogMsg(Level.INFO, "----> Waiting for Thread dump created message...");
        server.waitForStringInLog("CWWKE0068I", 30000);
        List<String> threadDumpCreationlines = server.findStringsInLogsUsingMark("CWWKE0068I", MESSAGE_LOG);

        CommonTasks.writeLogMsg(Level.INFO, "----> No. of thread dumps requested : " + threadDumpRequestlines.size());
        CommonTasks.writeLogMsg(Level.INFO, "----> No. of thread dumps created : " + threadDumpCreationlines.size());
        for (String line : threadDumpCreationlines) {
            CommonTasks.writeLogMsg(Level.INFO, "------> Created thread dump : " + line);
        }

        if (threadDumpsEnabled) {
            assertTrue("No Thread dump request messages found!", (threadDumpRequestlines.size() > 0));
            assertTrue("No Thread dump generated messages found!", (threadDumpCreationlines.size() > 0));
        } else {
            assertTrue("Thread dump request messages found : " + threadDumpRequestlines.size(), (threadDumpRequestlines.size() == 0));
            assertTrue("Thread dump generated messages found : " + threadDumpCreationlines, (threadDumpCreationlines.size() == 0));
        }
    }

    private void verifyHungRequestWarnings() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "------> Waiting for hung detection warning...");
        server.waitForStringInLog("TRAS0114W", 30000);
        int numOfhungRequestsWarnMsgs = fetchHungRequestWarningsCount("TRAS0114W");
        assertTrue("No hung request warning message was found !", numOfhungRequestsWarnMsgs > 0);

        CommonTasks.writeLogMsg(Level.INFO, "------> Waiting for hung request complete message...");
        server.waitForStringInLog("TRAS0115W", 30000);
        int numOfhungRequestCompletedMsgs = fetchHungRequestWarningsCount("TRAS0115W");
        assertTrue("No hung request completed warning message was found !", numOfhungRequestCompletedMsgs > 0);
    }

    private int fetchHungRequestWarningsCount(String msgID) throws Exception {
        List<String> lines = server.findStringsInLogsUsingMark(msgID, MESSAGE_LOG);
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "------> Hung Request Warning for " + msgID + " : " + line);
        }
        return lines.size();
    }

    private void createHungRequest(int duration) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestWebApp/TestServlet?sleepTime=" + duration);
        CommonTasks.writeLogMsg(Level.INFO, "----> Calling TestWebApp Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        br.readLine();
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
     * This method creates a connection to a webpage and then returns the connection
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
