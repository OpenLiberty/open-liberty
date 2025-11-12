/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class LogThrottleTest {
    private static final String DEFAULT_SERVER_NAME_XML = "com.ibm.ws.logging.logThrottle";
    private static final String DISABLED_SERVER_NAME_XML = "com.ibm.ws.logging.logThrottleDisabled"; //Server starts with log throttling disabled due to some SOE builds spamming certain logs on startup

    private static final String HIGH_MAX_MESSAGES_XML = "server-highMaxMessages.xml";
    private static final String THROTTLING_DISABLED_XML = "server-throttlingDisabled.xml";
    private static final String THROTTLING_INVALID_CONFIG_XML = "server-invalidThrottlingConfig.xml";
    private static final String THROTTLING_FULL_MESSAGE_XML = "server-throttlingFullMessage.xml";

    private static final Logger LOG = Logger.getLogger(LogThrottleTest.class.getName());
    private static final String CLASS_NAME = LogThrottleTest.class.getName();
    private static final String TEST_SEPARATOR = "*******************";

    private static LibertyServer defaultServer;
    private static LibertyServer disabledServer;

    private static LibertyServer serverInUse; // hold on to the server currently used so cleanUp knows which server to stop

    private static final int CONN_TIMEOUT = 10;

    @BeforeClass
    public static void initialSetup() throws Exception {
        defaultServer = LibertyServerFactory.getLibertyServer(DEFAULT_SERVER_NAME_XML);
        disabledServer = LibertyServerFactory.getLibertyServer(DISABLED_SERVER_NAME_XML);

        // Preserve the original server configuration
        defaultServer.saveServerConfiguration();
        disabledServer.saveServerConfiguration();

        ShrinkHelper.defaultDropinApp(defaultServer, "logger-servlet", "com.ibm.ws.logging.fat.logger.servlet");
        ShrinkHelper.defaultDropinApp(disabledServer, "logger-servlet", "com.ibm.ws.logging.fat.logger.servlet");

    }

    public void setUp(LibertyServer server, String method) throws Exception {
        LOG.logp(Level.INFO, CLASS_NAME, method, TEST_SEPARATOR + " TEST: " + method + " " + TEST_SEPARATOR);
        serverInUse = server;
        if (server != null && !serverInUse.isStarted()) {
            // Restore the original server configuration, before starting the server for each test case.
            serverInUse.restoreServerConfiguration();
            serverInUse.startServer();
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (serverInUse != null && serverInUse.isStarted()) {
            serverInUse.stopServer("CWWKG0032W", "CWWKG0083W", "TRAS3016W", "TESTA0001W", "TESTA0002W");
        }
    }

    /*
     * Ensure log throttling warning is printed and only printed once.
     */
    @Test
    public void testLogThrottlingWarningTriggered() throws Exception {
        setUp(defaultServer, "testLogThrottlingWarningTriggered");
        ServerConfiguration serverConfig = serverInUse.getServerConfiguration();
        Logging loggingObj = serverConfig.getLogging();
        loggingObj.setThrottleMaxMessagesPerWindow("5");
        hitWebPage("logger-servlet", "LoggerServlet", false, "numMessages=6");

        List<String> lines = serverInUse.findStringsInLogs("The logs are being throttled due to high volume");
        hitWebPage("logger-servlet", "LoggerServlet", false, "numMessages=6");

        assertEquals("The throttle log warning was not printed.", lines.size(), 1);
    }

    /*
     * Ensure that log throttling is activated immediately after threshold is met.
     */
    @Test
    public void testLogThrottlingActiveLowOccurrence() throws Exception {
        setUp(defaultServer, "testLogThrottlingActiveLowOccurrence");
        hitWebPage("logger-servlet", "LoggerServlet", false, "numMessages=6");

        List<String> lines = serverInUse.findStringsInLogs("TESTA0001W");
        assertEquals("Test message TESTA0001W wasn't printed the correct number of times", lines.size(), 5);
    }

    /*
     * Ensure that log throttling is activated and remains active with a high volume of logs
     */
    @Test
    public void testLogThrottlingActiveHighOccurrence() throws Exception {
        setUp(defaultServer, "testLogThrottlingActiveHighOccurrence");
        hitWebPage("logger-servlet", "LoggerServlet", false, "numMessages=25");

        List<String> lines = serverInUse.findStringsInLogs("TESTA0001W");
        assertEquals("Test message TESTA0001W wasn't printed the correct number of times", lines.size(), 5);
    }

    /*
     * Ensures that log throtting is not activated when the log threshold is not met.
     */
    @Test
    public void testLogThrottlingHighMaxMessages() throws Exception {
        setUp(disabledServer, "testLogThrottlingHighMaxMessages");
        serverInUse.setServerConfigurationFile(HIGH_MAX_MESSAGES_XML);
        hitWebPage("logger-servlet", "LoggerServlet", false, "numMessages=6");

        RemoteFile messagesLogFile = serverInUse.getDefaultLogFile();
        String line = serverInUse.waitForStringInLog("The logs are being throttled due to high volume.", 5000, messagesLogFile);
        assertNull("Log throttling incorrectly throttled prematurely.", line);
    }

    /*
     * Ensure that when using the message throttleType, applicable messages are throttled while other are not.
     */
    @Test
    public void testLogThrottlingActiveFullMessage() throws Exception {
        setUp(defaultServer, "testLogThrottlingActiveFullMessage");
        serverInUse.setServerConfigurationFile(THROTTLING_FULL_MESSAGE_XML);
        ServerConfiguration serverConfig = serverInUse.getServerConfiguration();
        Logging loggingObj = serverConfig.getLogging();
        loggingObj.setThrottleMaxMessagesPerWindow("5");
        Thread.sleep(5000);

        hitWebPage("logger-servlet", "LoggerServlet", false, "numMessages=8");

        List<String> lines = serverInUse.findStringsInLogs("TESTA0001W");
        List<String> lines2 = serverInUse.findStringsInLogs("TESTA0002W");

        assertEquals("Test message TESTA0001W wasn't printed the correct number of times", lines.size(), 5);
        assertFalse("Full message configuration is not functioning correctly.", lines2.size() == lines.size()); //This message shouldn't be getting throttled due to message variation
    }

    /*
     * Ensure that throttling is not activated whn throttling is disabled.
     */
    @Test
    public void testLogThrottlingDisabled() throws Exception {
        setUp(disabledServer, "testLogThrottlingDisabled");
        hitWebPage("logger-servlet", "LoggerServlet", false, "numMessages=6");

        RemoteFile messagesLogFile = serverInUse.getDefaultLogFile();
        String line = serverInUse.waitForStringInLog("The logs are being throttled due to high volume.", 5000, messagesLogFile);
        assertNull("Log Throttling was not disabled", line);
    }

    /*
     * Ensure that invalid throttleMaxMessagesPerWindow configuration is caught, set to the default and has a warning printed.
     */
    @Test
    public void testInvalidLogThrottlingMaxMessagesConfig() throws Exception {
        setUp(defaultServer, "testInvalidLogThrottlingMaxMessagesConfig");
        serverInUse.setServerConfigurationFile(THROTTLING_INVALID_CONFIG_XML);
        hitWebPage("logger-servlet", "LoggerServlet", false, "numMessages=6");

        RemoteFile messagesLogFile = serverInUse.getDefaultLogFile();
        String line = serverInUse.waitForStringInLog("CWWKG0083W:", 5000, messagesLogFile);
        assertTrue("An invalid config attribute was not caught and set to the default.",
                   line.contains("A validation failure occurred while processing the [throttleMaxMessagesPerWindow] property, value = [-1]. Default value in use: [1000]."));
    }

    /*
     * Ensure that invalid throttleType configuration is caught, set to the default and has a warning printed.
     */
    @Test
    public void testInvalidLogThrottlingMessageTypeConfig() throws Exception {
        setUp(defaultServer, "testInvalidLogThrottlingMessageTypeConfig");
        serverInUse.setServerConfigurationFile(THROTTLING_INVALID_CONFIG_XML);
        hitWebPage("logger-servlet", "LoggerServlet", false, "numMessages=6");

        RemoteFile messagesLogFile = serverInUse.getDefaultLogFile();
        String line = serverInUse.waitForStringInLog("CWWKG0032W:", 5000, messagesLogFile);
        assertTrue("An invalid config attribute was not caught and set to the default.",
                   line.contains("Unexpected value specified for property [throttleType], value = [messageIDs]. Expected value(s) are: [messageID][message]. Default value in use: messageID."));
    }

    private static void hitWebPage(String contextRoot, String servletName, boolean failureAllowed, String params) throws MalformedURLException, IOException, ProtocolException {
        try {
            String urlStr = "http://" + serverInUse.getHostname() + ":" + serverInUse.getHttpDefaultPort() + "/" + contextRoot + "/" + servletName;
            urlStr = params != null ? urlStr + params : urlStr;
            URL url = new URL(urlStr);
            int expectedResponseCode = failureAllowed ? HttpURLConnection.HTTP_INTERNAL_ERROR : HttpURLConnection.HTTP_OK;
            HttpURLConnection con = HttpUtils.getHttpConnection(url, expectedResponseCode, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            // Make sure the server gave us something back
            assertNotNull(line);
            con.disconnect();
        } catch (IOException e) {
            // A message about a 500 code may be fine
            if (!failureAllowed) {
                throw e;
            }
        }
    }

}