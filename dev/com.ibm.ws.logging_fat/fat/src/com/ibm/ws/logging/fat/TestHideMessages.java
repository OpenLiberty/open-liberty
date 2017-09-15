/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class TestHideMessages {

    private static LibertyServer msgServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.hidemessage");
    private static final Class<?> logClass = TestHideMessages.class;
    static String TWO_HIDDEDN_MESSAGE_SERVER = "server-twomessageids.xml";

    static long SMALL_TIMEOUT = 10000;
    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void prepareTest() throws Exception {
        msgServer.startServer();
    }

    @Test
    // No need to wait for message CWWKZ0058I/TRAS3001I since that happens during server startup. So using findStringsInLogs
    public void testHiddenMsgIds() throws Exception {

        assertTrue("Hidden Message CWWKZ0058I should not be seen in messages.log",
                   msgServer.findStringsInLogs("CWWKZ0058I:", msgServer.getMatchingLogFile("messages.log")).isEmpty());
        assertTrue("Hidden Message CWWKZ0058I should not be seen in console.log", msgServer.findStringsInLogs("CWWKZ0058I:", msgServer.getMatchingLogFile("console.log")).isEmpty());
        assertFalse("Hidden Message CWWKZ0058I should be seen in trace", msgServer.findStringsInTrace("CWWKZ0058I:").isEmpty());
        assertFalse("Info message about redirection to trace file should be logged",
                    msgServer.findStringsInLogs("TRAS3001I:", msgServer.getMatchingLogFile("messages.log")).isEmpty());

    }

    @Test
    public void testDynamicAddMessageIds() throws Exception {
        Log.info(logClass, name.getMethodName(), "Entering test " + name.getMethodName());
        // Need to capture CWWKF0012I messages that are in the logs from initial startup
        int initial_messages_size_CWWKF0012I = msgServer.findStringsInLogs("CWWKF0012I:", msgServer.getMatchingLogFile("messages.log")).size();
        int initial_console_size_CWWKF0012I = msgServer.findStringsInLogs("CWWKF0012I:", msgServer.getMatchingLogFile("console.log")).size();
        int initial_trace_size_CWWKF0012I = msgServer.findStringsInTrace("CWWKF0012I:").size();

        msgServer.setServerConfigurationFile(TWO_HIDDEDN_MESSAGE_SERVER);

        assertTrue("Hidden Message CWWKZ0058I should not be seen in messages.log",
                   msgServer.findStringsInLogs("CWWKZ0058I:", msgServer.getMatchingLogFile("messages.log")).isEmpty());
        assertTrue("Hidden Message CWWKZ0058I should not be seen in console.log", msgServer.findStringsInLogs("CWWKZ0058I:", msgServer.getMatchingLogFile("console.log")).isEmpty());
        assertFalse("Hidden Message CWWKZ0058I should be seen in trace", msgServer.findStringsInTrace("CWWKZ0058I:").isEmpty());

        //This will wait for feature update completion message since we are adding a new feature. And CWWKF0012I should be seen before that
        msgServer.waitForConfigUpdateInLogUsingMark(null);
        assertTrue("Hidden Message CWWKF0012I should not be seen in messages.log",
                   msgServer.findStringsInLogs("CWWKF0012I:", msgServer.getMatchingLogFile("messages.log")).size() == initial_messages_size_CWWKF0012I);
        assertTrue("Hidden Message CWWKF0012I should not be seen in console.log",
                   msgServer.findStringsInLogs("CWWKF0012I:", msgServer.getMatchingLogFile("console.log")).size() == initial_console_size_CWWKF0012I);
        assertTrue("Hidden Message CWWKF0012I should be seen in trace",
                   msgServer.findStringsInTrace("CWWKF0012I:").size() == (initial_trace_size_CWWKF0012I + 1));

        Log.info(logClass, name.getMethodName(), "Exiting test " + name.getMethodName());
    }

    @AfterClass
    public static void completeTest() throws Exception {
        msgServer.stopServer();
    }

}
