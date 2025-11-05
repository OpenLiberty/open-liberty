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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;
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

    @BeforeClass
    public static void initialSetup() throws Exception {
        defaultServer = LibertyServerFactory.getLibertyServer(DEFAULT_SERVER_NAME_XML);
        disabledServer = LibertyServerFactory.getLibertyServer(DISABLED_SERVER_NAME_XML);

        // Preserve the original server configuration
        defaultServer.saveServerConfiguration();
        disabledServer.saveServerConfiguration();

        ShrinkHelper.defaultDropinApp(defaultServer, "quicklogtest", "com.ibm.ws.logging.fat.quick.log.test");

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
            serverInUse.stopServer("CWWKG0032W", "CWWKG0083W", "TRAS3016W");
        }
    }

    /*
     * Ensure log throttling warning is printed and only printed once.
     */
    @Test
    public void testLogThrottlingWarningTriggered() throws Exception {
        setUp(defaultServer, "testLogThrottlingWarningTriggered");
        spamServerConfigurationUpdates(6);

        List<String> lines = serverInUse.findStringsInLogs("The logs are being throttled due to high volume");
        spamServerConfigurationUpdates(6);

        assertEquals("The throttle log warning was not printed.", lines.size(), 1);
    }

    /*
     * Ensure that log throttling is activated immediately after threshold is met.
     */
    @Test
    public void testLogThrottlingActiveLowOccurrence() throws Exception {
        setUp(defaultServer, "testLogThrottlingActiveLowOccurrence");
        spamServerConfigurationUpdates(6);
        List<String> lines = serverInUse.findStringsInLogs("CWWKG0016I");
        assertEquals("Configuration updated message wasn't printed the correct number of times.", lines.size(), 5);
    }

    /*
     * Ensure that log throttling is activated and remains active with a high volume of logs
     */
    @Test
    public void testLogThrottlingActiveHighOccurrence() throws Exception {
        setUp(defaultServer, "testLogThrottlingActiveHighOccurrence");
        spamServerConfigurationUpdates(25);
        List<String> lines = serverInUse.findStringsInLogs("CWWKG0016I");
        assertEquals("Configuration updated message wasn't printed the correct number of times.", lines.size(), 5);
    }

    /*
     * Ensures that log throtting is not activated when the log threshold is not met.
     */
    @Test
    public void testLogThrottlingHighMaxMessages() throws Exception {
        setUp(disabledServer, "testLogThrottlingHighMaxMessages");
        serverInUse.setServerConfigurationFile(HIGH_MAX_MESSAGES_XML);
        spamServerConfigurationUpdates(6);

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
        spamServerConfigurationUpdates(8);
        List<String> lines = serverInUse.findStringsInLogs("CWWKG0016I");
        List<String> lines2 = serverInUse.findStringsInLogs("CWWKG0017I");

        assertEquals("Configuration updated message wasn't printed the correct number of times.", lines.size(), 6);
        assertFalse("Configuration updated message wasn't printed the correct number of times.", lines2.size() == lines.size()); //This message shouldn't be getting throttled due to message variation
    }

    /*
     * Ensure that throttling is not activated whn throttling is disabled.
     */
    @Test
    public void testLogThrottlingDisabled() throws Exception {
        setUp(disabledServer, "testLogThrottlingDisabled");
        spamServerConfigurationUpdates(6);

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
        spamServerConfigurationUpdates(6);

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
        spamServerConfigurationUpdates(6);

        RemoteFile messagesLogFile = serverInUse.getDefaultLogFile();
        String line = serverInUse.waitForStringInLog("CWWKG0032W:", 5000, messagesLogFile);
        assertTrue("An invalid config attribute was not caught and set to the default.",
                   line.contains("Unexpected value specified for property [throttleType], value = [messageIDs]. Expected value(s) are: [messageID][message]. Default value in use: messageID."));
    }

    public void spamServerConfigurationUpdates(int numberOfUpdates) throws Exception {
        ServerConfiguration serverConfig = serverInUse.getServerConfiguration();
        Logging loggingObj = serverConfig.getLogging();
        int lastInt = 1;
        Random rand = new Random();

        for (int i = 0; i < numberOfUpdates; i++) {
            int newInt;
            do {
                newInt = rand.nextInt(10) + 1;
            } while (newInt == lastInt);

            lastInt = newInt;
            loggingObj.setMaxFiles(newInt);
            serverInUse.updateServerConfiguration(serverConfig);
            Thread.sleep(1000);
        }
    }

}