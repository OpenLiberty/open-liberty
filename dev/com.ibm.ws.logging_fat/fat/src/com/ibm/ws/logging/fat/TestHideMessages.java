/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class TestHideMessages {

    private static LibertyServer msgServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.hidemessage");
    private static final Class<?> logClass = TestHideMessages.class;
    static String TWO_HIDDEN_MESSAGE_SERVER = "server-twoMessageIds.xml";
    static String HIDE_MSG_ATTRIBUTE_REMOVAL = "server-hideMsgAttrbRemoval.xml";
    static String EMPTY_HIDE_MSG = "server-emptyHideMsg.xml";

    static long SMALL_TIMEOUT = 10000;
    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void prepareTest() throws Exception {
        msgServer.saveServerConfiguration();
    }

    @Before
    public void setupTest() throws Exception {
        msgServer.restoreServerConfiguration();
        msgServer.startServer();
    }

    @Test
    public void testHiddenMsgIds() throws Exception {
        Log.info(logClass, name.getMethodName(), "Entering test " + name.getMethodName());

        // No need to wait for message CWWKZ0058I/TRAS3001I since that happens during server startup. So using findStringsInLogs
        assertTrue("Hidden Message CWWKZ0058I should not be seen in messages.log",
                   msgServer.findStringsInLogs("CWWKZ0058I:", msgServer.getMatchingLogFile("messages.log")).isEmpty());
        assertTrue("Hidden Message CWWKZ0058I should not be seen in console.log",
                   msgServer.findStringsInLogs("CWWKZ0058I:", msgServer.getConsoleLogFile()).isEmpty());
        assertFalse("Hidden Message CWWKZ0058I should be seen in trace", msgServer.findStringsInTrace("CWWKZ0058I:").isEmpty());
        assertFalse("Info message about redirection to trace file should be logged",
                    msgServer.findStringsInLogs("TRAS3001I:", msgServer.getMatchingLogFile("messages.log")).isEmpty());

        Log.info(logClass, name.getMethodName(), "Exiting test " + name.getMethodName());
    }

    @Test
    public void testDynamicAddMessageIds() throws Exception {
        Log.info(logClass, name.getMethodName(), "Entering test " + name.getMethodName());
        // Need to capture CWWKF0012I messages that are in the logs from initial startup
        int initial_messages_size_CWWKF0012I = msgServer.findStringsInLogs("CWWKF0012I:", msgServer.getMatchingLogFile("messages.log")).size();
        int initial_console_size_CWWKF0012I = msgServer.findStringsInLogs("CWWKF0012I:", msgServer.getConsoleLogFile()).size();
        int initial_trace_size_CWWKF0012I = msgServer.findStringsInTrace("CWWKF0012I:").size();

        msgServer.setServerConfigurationFile(TWO_HIDDEN_MESSAGE_SERVER);

        assertTrue("Hidden Message CWWKZ0058I should not be seen in messages.log",
                   msgServer.findStringsInLogs("CWWKZ0058I:", msgServer.getMatchingLogFile("messages.log")).isEmpty());
        assertTrue("Hidden Message CWWKZ0058I should not be seen in console.log",
                   msgServer.findStringsInLogs("CWWKZ0058I:", msgServer.getConsoleLogFile()).isEmpty());
        assertFalse("Hidden Message CWWKZ0058I should be seen in trace", msgServer.findStringsInTrace("CWWKZ0058I:").isEmpty());

        //This will wait for feature update completion message since we are adding a new feature. And CWWKF0012I should be seen before that
        msgServer.waitForConfigUpdateInLogUsingMark(null);
        assertTrue("Hidden Message CWWKF0012I should not be seen in messages.log",
                   msgServer.findStringsInLogs("CWWKF0012I:", msgServer.getMatchingLogFile("messages.log")).size() == initial_messages_size_CWWKF0012I);
        assertTrue("Hidden Message CWWKF0012I should not be seen in console.log",
                   msgServer.findStringsInLogs("CWWKF0012I:", msgServer.getConsoleLogFile()).size() == initial_console_size_CWWKF0012I);
        assertTrue("Hidden Message CWWKF0012I should be seen in trace",
                   msgServer.findStringsInTrace("CWWKF0012I:").size() == (initial_trace_size_CWWKF0012I + 1));

        Log.info(logClass, name.getMethodName(), "Exiting test " + name.getMethodName());
    }

    /**
     * Tests when the hideMessage attribute is removed from the server.xml, no messages should be hidden.
     */
    @Test
    public void testRemovalHideMessageAttribute() throws Exception {
        Log.info(logClass, name.getMethodName(), "Entering test " + name.getMethodName());

        // Make sure the feature update message is set to be hidden.
        msgServer.setServerConfigurationFile(TWO_HIDDEN_MESSAGE_SERVER);

        // First, test if the set messageID prefixes are hidden after server startup.
        assertTrue("Hidden Message CWWKF0012I should not be seen in messages.log",
                   msgServer.findStringsInLogs("CWWKZ0058I:", msgServer.getMatchingLogFile("messages.log")).isEmpty());
        assertTrue("Hidden Message CWWKF0012I should not be seen in console.log",
                   msgServer.findStringsInLogs("CWWKZ0058I:", msgServer.getConsoleLogFile()).isEmpty());
        assertFalse("Hidden Message CWWKF0012I should be seen in trace", msgServer.findStringsInTrace("CWWKZ0058I:").isEmpty());
        assertFalse("Info message about hidden messageID prefixes should be logged",
                    msgServer.findStringsInLogs("TRAS3001I:", msgServer.getMatchingLogFile("messages.log")).isEmpty());

        msgServer.setServerConfigurationFile(HIDE_MSG_ATTRIBUTE_REMOVAL);

        // This will wait for feature update completion message since we are adding a new feature.
        msgServer.waitForConfigUpdateInLogUsingMark(null);

        // Ensure the mark is set here for messages.log, so the find methods do not find the previous messages before the mark.
        msgServer.setMarkToEndOfLog(msgServer.getMatchingLogFile("messages.log"));

        // Ensure the mark is set here for console.log, so the find methods do not find the previous messages before the mark.
        msgServer.setMarkToEndOfLog(msgServer.getMatchingLogFile("console.log"));

        // Wait for the CWWKF0012I message. Should return a maximum of 2 instances of the CWWKF0012I:
        // At server startup and another after the hide message attribute removal.
        msgServer.waitForMultipleStringsInLog(2, "CWWKF0012I:");

        // Second, test if the previously hidden messageID (CWWKF0012I) prefixes are showing up in message.log/console.log after server configuration update.
        assertTrue("Hidden Message CWWKF0012I should be seen in messages.log",
                   msgServer.findStringsInLogs("CWWKF0012I:", msgServer.getMatchingLogFile("messages.log")).size() > 0);
        assertTrue("Hidden Message CWWKF0012I should be seen in console.log",
                   msgServer.findStringsInLogs("CWWKF0012I:", msgServer.getConsoleLogFile()).size() > 0);
        assertTrue("Info message about hidden messageID prefixes should not be logged",
                   msgServer.findStringsInLogsUsingMark("TRAS3001I:", msgServer.getMatchingLogFile("messages.log")).isEmpty());

        Log.info(logClass, name.getMethodName(), "Exiting test " + name.getMethodName());

    }

    /**
     * Tests when the hideMessage attribute is set to an empty string from the server.xml, no messages should be hidden.
     */
    @Test
    public void testEmptyHideMessageAttribute() throws Exception {
        Log.info(logClass, name.getMethodName(), "Entering test " + name.getMethodName());

        msgServer.setServerConfigurationFile(EMPTY_HIDE_MSG);

        // Ensure the mark is set here, so the find methods do not find the previous messages before the mark.
        msgServer.setMarkToEndOfLog(msgServer.getMatchingLogFile("messages.log"));

        // The INFO message regarding if any messages are hidden should not be logged.
        assertTrue("Info message about hidden messageID prefixes should not be logged",
                   msgServer.findStringsInLogsUsingMark("TRAS3001I:", msgServer.getMatchingLogFile("messages.log")).isEmpty());

        Log.info(logClass, name.getMethodName(), "Exiting test " + name.getMethodName());

    }

    @After
    public void cleanupTest() throws Exception {
        msgServer.stopServer();
    }

}
