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
package com.ibm.ws.logging.json.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Enable JSON logging in messages.log with environment variables in server.env
 */
@RunWith(FATRunner.class)
public class ConsoleFormatTest {

    protected static final Class<?> c = ConsoleFormatTest.class;

    @Server("com.ibm.ws.logging.json.ConsoleFormatServer")
    public static LibertyServer server;

    public static final String APP_NAME = "LogstashApp";

    public static final String SERVER_XML_BASIC_STDOUT = "basicConsoleBasicMessageStdout.xml";
    public static final String SERVER_XML_JSON_STDOUT = "jsonConsoleJsonMessageStdout.xml";
    public static final String[] JSON_MESSAGES = { "\\{.*\"type\":\"liberty_accesslog\".*\\}",
                                                   "\\{.*\"type\":\"liberty_gc\".*\\}",
                                                   "\\{.*\"type\":\"liberty_message\".*\\}",
                                                   "\\{.*\"type\":\"liberty_ffdc\".*\\}",
                                                   "\\{.*\"type\":\"liberty_trace\".*\\}",
                                                   "\\{.*\"type\":\"liberty_recommendations\".*\\}",
                                                   "\\{.*\"type\":\"liberty_audit\".*\\}" };
    public static final String[] BASIC_TRACE = { "\\[.*\\] .* LogstashServl C   config trace",
                                                 "\\[.*\\] .* LogstashServl 1   fine trace",
                                                 "\\[.*\\] .* LogstashServl 2   finer trace",
                                                 "\\[.*\\] .* LogstashServl 3   finest trace" };
    public static final String[] JSON_TRACE = { "\\{.*\"loglevel\":\"ENTRY\".*\\}",
                                                "\\{.*\"loglevel\":\"CONFIG\".*\\}",
                                                "\\{.*\"loglevel\":\"FINE\".*\\}",
                                                "\\{.*\"loglevel\":\"FINER\".*\\}",
                                                "\\{.*\"loglevel\":\"FINEST\".*\\}",
                                                "\\{.*\"loglevel\":\"EXIT\".*\\}" };
    public static final String[] ALL_TRACE = { "\\{.*\"loglevel\":\"ENTRY\".*\\}",
                                               "\\{.*\"loglevel\":\"CONFIG\".*\\}",
                                               "\\{.*\"loglevel\":\"FINE\".*\\}",
                                               "\\{.*\"loglevel\":\"FINER\".*\\}",
                                               "\\{.*\"loglevel\":\"FINEST\".*\\}",
                                               "\\{.*\"loglevel\":\"EXIT\".*\\}",
                                               "\\[.*\\] .* LogstashServl C   config trace",
                                               "\\[.*\\] .* LogstashServl 1   fine trace",
                                               "\\[.*\\] .* LogstashServl 2   finer trace",
                                               "\\[.*\\] .* LogstashServl 3   finest trace" };

    ArrayList<String> ALL_SOURCE_LIST = new ArrayList<String>(Arrays.asList("message", "trace", "accesslog", "ffdc"));

    String line = null;
    String consoleline = null;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.logs");
        server.startServer();
        /* start server */
        Assert.assertNotNull("Test app LogstashApp does not appear to have started.", server.waitForStringInLog("CWWKT0016I:.*LogstashApp"));
    }

    /*
     * check for no json being sent to console
     * <logging traceFileName="stdout" traceFormat="BASIC" messageFormat="basic" consoleFormat="basic" traceSpecification="com.ibm.logs.LogstashServlet=finest" />
     */
    @Test
    public void testBasicConsoleTraceFileNameStdout() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        line = setConfig(SERVER_XML_BASIC_STDOUT, consoleLogFile);
        /* Check that the console log does not contain JSON */
        boolean hasNoJSON = checkStringsNotInLog(JSON_MESSAGES, consoleLogFile);
        assertTrue(hasNoJSON);
        /* Check that the console log does contain basic trace */
        boolean hasNoTrace = checkStringsNotInLog(BASIC_TRACE, consoleLogFile);
        assertFalse(hasNoTrace);
    }

    /*
     * Test switching between JSON and BASIC formatting modes for stdout
     */
    @Test
    public void testBasicConsoleSwitchToJSONConsoleTraceFileNameStdout() throws Exception {
        /*
         * first set config as basic stdout
         * <logging traceFileName="stdout" traceFormat="BASIC" messageFormat="basic" consoleFormat="basic" traceSpecification="com.ibm.logs.LogstashServlet=finest" />
         */
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        line = setConfig(SERVER_XML_BASIC_STDOUT, consoleLogFile);
        /* Check that the console log does not contain JSON */
        boolean hasNoJSON = checkStringsNotInLog(JSON_MESSAGES, consoleLogFile);
        assertTrue(hasNoJSON);
        /*
         * switch config to JSON formatting
         * <logging traceFileName="stdout" consoleLogLevel="INFO" consoleFormat="json" messageFormat="json" traceSpecification="com.ibm.logs.LogstashServlet=finest" />
         */
        line = setConfig(SERVER_XML_JSON_STDOUT, consoleLogFile);
        boolean found = false;
        int count = 0;
        while (!found) {
            /* Check if the console log now contains JSON. Check 10 times before exiting loop */
            hasNoJSON = checkStringsNotInLog(JSON_MESSAGES, consoleLogFile);
            /* if JSON is found, exit loop */
            if (!hasNoJSON || count >= 10) {
                found = true;
            }
            /* Wait 1 second between each check */
            Thread.sleep(1000);
            count++;
        }
        assertFalse(hasNoJSON);
    }

    /*
     * Test for no trace data (only message) being sent to console
     * <logging traceFileName="stdout" consoleLogLevel="INFO" consoleFormat="json" messageFormat="json" traceSpecification="com.ibm.logs.LogstashServlet=finest"
     * consoleSource="message" />
     */
    @Test
    public void testJSONConsoleTraceFileNameStdout() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        line = setConfig(SERVER_XML_JSON_STDOUT, consoleLogFile);
        /* Check that the console log does not contain any trace (basic or JSON) */
        boolean hasNoTrace = checkStringsNotInLog(ALL_TRACE, consoleLogFile);
        assertTrue(hasNoTrace);
        /* Check that the console log contains JSON */
        boolean hasNoJSON = checkStringsNotInLog(JSON_MESSAGES, consoleLogFile);
        assertFalse(hasNoJSON);
    }

    /*
     * searches for strings from the given list in the given logFile
     */
    private Boolean checkStringsNotInLog(String[] messagesList, RemoteFile logFile) throws Exception {
        TestUtils.runApp(server, "logServlet");
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

    private static String setConfig(String fileName, RemoteFile logFile) throws Exception {
        server.setMarkToEndOfLog(logFile);
        server.setServerConfigurationFile(fileName);
        return server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");
    }

    @AfterClass
    public static void tearDownClass() {
        if ((server != null) && (server.isStarted())) {
            try {
                server.stopServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
