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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
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
    private static final String SERVER_NAME_XML = "com.ibm.ws.logging.logThrottle";
    private static final String HIGH_MAX_MESSAGES_XML = "server-highMaxMessages.xml";
    private static final String THROTTLING_DISABLED_XML = "server-throttlingDisabled.xml";
    private static final String THROTTLING_INVALID_CONFIG_XML = "server-invalidThrottlingConfig.xml";
    private static final String THROTTLING_FULL_MESSAGE_XML = "server-throttlingFullMessage.xml";

    private static LibertyServer server;

    @BeforeClass
    public static void initialSetup() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME_XML);
        server.saveServerConfiguration();
        ShrinkHelper.defaultDropinApp(server, "quicklogtest", "com.ibm.ws.logging.fat.quick.log.test");

    }

    @Before
    public void setUp() throws Exception {
        if (server != null && !server.isStarted()) {
            // Restore the original server configuration, before starting the server for each test case.
            server.restoreServerConfiguration();
            server.startServer();
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKG0032W", "CWWKG0083W", "TRAS3016W");
        }
    }

    /*
     * Ensure log throttling warning is printed and only printed once.
     */
    @Test
    public void testLogThrottlingWarningTriggered() throws Exception {
        spamServerConfigurationUpdates(6);

        List<String> lines = server.findStringsInLogs("The logs are being throttled due to high volume");
        spamServerConfigurationUpdates(6);

        assertEquals("The throttle log warning was not printed.", lines.size(), 1);
    }

    /*
     * Ensure that log throttling is activated immediately after threshold is met.
     */
    @Test
    public void testLogThrottlingActiveLowOccurrence() throws Exception {
        spamServerConfigurationUpdates(6);
        List<String> lines = server.findStringsInLogs("CWWKG0016I");
        assertEquals("Configuration updated message wasn't printed the correct number of times.", lines.size(), 5);
    }

    /*
     * Ensure that log throttling is activated and remains active with a high volume of logs
     */
    @Test
    public void testLogThrottlingActiveHighOccurrence() throws Exception {
        spamServerConfigurationUpdates(25);
        List<String> lines = server.findStringsInLogs("CWWKG0016I");
        assertEquals("Configuration updated message wasn't printed the correct number of times.", lines.size(), 5);
    }

    /*
     * Ensures that log throtting is not activated when the log threshold is not met.
     */
    @Test
    public void testLogThrottlingHighMaxMessages() throws Exception {
        server.setServerConfigurationFile(HIGH_MAX_MESSAGES_XML);
        spamServerConfigurationUpdates(6);

        RemoteFile messagesLogFile = server.getDefaultLogFile();
        String line = server.waitForStringInLog("The logs are being throttled due to high volume.", 5000, messagesLogFile);
        assertNull("Log throttling incorrectly throttled prematurely.", line);
    }

    /*
     * Ensure that when using the message throttleType, applicable messages are throttled while other are not.
     */
    @Test
    public void testLogThrottlingActiveFullMessage() throws Exception {
        server.setServerConfigurationFile(THROTTLING_FULL_MESSAGE_XML);
        spamServerConfigurationUpdates(8);
        List<String> lines = server.findStringsInLogs("CWWKG0016I");
        List<String> lines2 = server.findStringsInLogs("CWWKG0017I");

        assertEquals("Configuration updated message wasn't printed the correct number of times.", lines.size(), 6);
        assertEquals("Configuration updated message wasn't printed the correct number of times.", lines2.size(), 8); //This message shouldn't be getting throttled due to message variation
    }

    /*
     * Ensure that throttling is not activated whn throttling is disabled.
     */
    @Test
    public void testLogThrottlingDisabled() throws Exception {
        server.setServerConfigurationFile(THROTTLING_DISABLED_XML);
        spamServerConfigurationUpdates(6);

        RemoteFile messagesLogFile = server.getDefaultLogFile();
        String line = server.waitForStringInLog("The logs are being throttled due to high volume.", 5000, messagesLogFile);
        assertNull("Log Throttling was not disabled", line);
    }

    /*
     * Ensure that invalid throttleMaxMessagesPerWindow configuration is caught, set to the default and has a warning printed.
     */
    @Test
    public void testInvalidLogThrottlingMaxMessagesConfig() throws Exception {
        server.setServerConfigurationFile(THROTTLING_INVALID_CONFIG_XML);
        spamServerConfigurationUpdates(6);

        RemoteFile messagesLogFile = server.getDefaultLogFile();
        String line = server.waitForStringInLog("CWWKG0083W:", 5000, messagesLogFile);
        assertTrue("An invalid config attribute was not caught and set to the default.",
                   line.contains("A validation failure occurred while processing the [throttleMaxMessagesPerWindow] property, value = [-1]. Default value in use: [1000]."));
    }

    /*
     * Ensure that invalid throttleType configuration is caught, set to the default and has a warning printed.
     */
    @Test
    public void testInvalidLogThrottlingMessageTypeConfig() throws Exception {
        server.setServerConfigurationFile(THROTTLING_INVALID_CONFIG_XML);
        spamServerConfigurationUpdates(6);

        RemoteFile messagesLogFile = server.getDefaultLogFile();
        String line = server.waitForStringInLog("CWWKG0032W:", 5000, messagesLogFile);
        assertTrue("An invalid config attribute was not caught and set to the default.",
                   line.contains("Unexpected value specified for property [throttleType], value = [messageIDs]. Expected value(s) are: [messageID][message]. Default value in use: messageID."));
    }

    public void spamServerConfigurationUpdates(int numberOfUpdates) throws Exception {
        ServerConfiguration serverConfig = server.getServerConfiguration();
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
            server.updateServerConfiguration(serverConfig);
            Thread.sleep(1000);
        }
    }

}