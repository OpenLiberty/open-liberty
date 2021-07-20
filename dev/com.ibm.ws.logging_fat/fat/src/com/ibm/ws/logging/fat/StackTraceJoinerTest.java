/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 * This test class ensures that stack traces are written as single events
 */
@RunWith(FATRunner.class)
public class StackTraceJoinerTest {

    protected static LibertyServer server;
    protected static RemoteFile messagesLog;
    protected static RemoteFile consoleLog;

    protected static final int CONN_TIMEOUT = 10;

    protected static void hitWebPage(String contextRoot, String servletName, boolean failureAllowed) throws MalformedURLException, IOException, ProtocolException {
        try {
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot + "/" + servletName);
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

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.logging.stackjoiner", StackTraceJoinerTest.class);
        ShrinkHelper.defaultDropinApp(server, "broken-servlet", "com.ibm.ws.logging.fat.broken.servlet");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testPrintStackTracePrintStream() throws Exception {
        consoleLog = server.getConsoleLogFile();
        server.setMarkToEndOfLog(consoleLog);
        hitWebPage("broken-servlet", "BrokenWithCustomPrintStreamServlet", false);
        List<String> systemErrOutput = server.findStringsInLogsUsingMark("err.*", consoleLog);

        assertTrue("Expected two single-lined stack traces to be found but found " + systemErrOutput.size() + " messages.", systemErrOutput.size() == 2);
        assertTrue("Expected two single-lined stack traces to be identical but they were not.", systemErrOutput.get(0).equals(systemErrOutput.get(1)));
    }

    @Test
    public void testSingleLinedStackTraceConsoleLog() throws Exception {
        consoleLog = server.getConsoleLogFile();
        server.setMarkToEndOfLog(consoleLog);
        hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);
        List<String> systemErrOutput = server.findStringsInLogsUsingMark("err.*", consoleLog);
        assertTrue("Expected stack trace as a single event but found " + systemErrOutput.size() + " messages.", systemErrOutput.size() == 1);
    }

    @Test
    public void testSingleLinedStackTraceMessagesLog() throws Exception {
        messagesLog = server.getDefaultLogFile();
        server.setMarkToEndOfLog(messagesLog);
        hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);
        List<String> systemErrOutput = server.findStringsInLogsUsingMark("SystemErr.*", messagesLog);
        assertTrue("Expected stack trace as a single event but found " + systemErrOutput.size() + " messages.", systemErrOutput.size() == 1);
    }

    @Test
    public void testPrintStackTraceOverrideReflection() throws Exception {
        hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);
        List<String> traceOutput = server.findStringsInTrace("Stack joiner could not be initialized..*");
        assertTrue("Stack joiner could not be initialized message was found in the trace.", traceOutput.size() == 0);
    }

    @Test
    public void testPrintStackTraceMultipleThreads() {
        // TODO: Test printStackTrace in multiple threads and ensure that messages are not leaking from one stack trace to another
    }
}
