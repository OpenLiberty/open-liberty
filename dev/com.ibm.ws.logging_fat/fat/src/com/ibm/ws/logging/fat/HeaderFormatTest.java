/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

import java.util.List;

import org.junit.After;
import org.junit.Test;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class HeaderFormatTest {
    public static final String[] JSON_MESSAGES = { "\\{.*\"type\":\"liberty_accesslog\".*\\}",
                                                   "\\{.*\"type\":\"liberty_gc\".*\\}",
                                                   "\\{.*\"type\":\"liberty_message\".*\\}",
                                                   "\\{.*\"type\":\"liberty_ffdc\".*\\}",
                                                   "\\{.*\"type\":\"liberty_trace\".*\\}",
                                                   "\\{.*\"type\":\"liberty_recommendations\".*\\}",
                                                   "\\{.*\"type\":\"liberty_audit\".*\\}" };
    public static final String[] BASIC_MESSSAGE = { "\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*" };
    public static final String[] JSON_CONSOLE = { "\\{\".*Launching.*\"\\}" };

    private static LibertyServer server;

    @After
    public void tearDownClass() {
        if ((server != null) && (server.isStarted())) {
            try {
                server.stopServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * This tests if valid json headers are produced when message and console format is "json"
     */
    @Test
    public void jsonHeaderTest() throws Exception {
        //start server
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.headerformatjson");
        System.out.println("Starting server...");
        server.startServer();
        System.out.println("Started server.");

        //retrieve log files
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        RemoteFile messagesLogFile = server.getDefaultLogFile();
        /* Check that the messages log does not contain Basic header */
        boolean hasNoBasic = checkStringsNotInLog(BASIC_MESSSAGE, messagesLogFile);
        assertTrue("There is a basic header in messages.log", hasNoBasic);
        /* Check that the console log does not contain basic header */
        boolean hasNoJSONHeader = checkStringsNotInLog(JSON_CONSOLE, consoleLogFile);
        assertFalse("There is no json header in console.log", hasNoJSONHeader);
    }

    /*
     * This tests if valid json headers are produced when message and console format is "json"
     */
    @Test
    public void basicHeaderTest() throws Exception {
        //start server
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.headerformatbasic");
        System.out.println("Starting server...");
        server.startServer();
        System.out.println("Started server.");
        //retrieve log files
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        RemoteFile messagesLogFile = server.getDefaultLogFile();
        /* Check that the messages log does not contain json */
        boolean hasNoJSON = checkStringsNotInLog(JSON_MESSAGES, messagesLogFile);
        assertTrue("There is a json header in messages.log", hasNoJSON);
        /* Check that the console log does contain json */
        hasNoJSON = checkStringsNotInLog(JSON_MESSAGES, consoleLogFile);
        assertTrue("There is a json header in console.log", hasNoJSON);
    }

    /*
     * searches for strings from the given list in the given logFile
     */
    private Boolean checkStringsNotInLog(String[] messagesList, RemoteFile logFile) throws Exception {
        List<String> results;
        for (String message : messagesList) {
            results = server.findStringsInLogsUsingMark(message, logFile);
            /* if able to find strings, return false */
            if (!results.isEmpty()) {
                return false;
            }
        }
        return true;
    }

}
