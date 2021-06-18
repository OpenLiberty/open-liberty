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
    protected static final int threadsToRun = 5;
    final int expectedErrOut = 2500;
    protected static final int pageHits = 500;

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
    public void testPrintStackTraceMultipleThreads() throws Exception {
        messagesLog = server.getDefaultLogFile();
        server.setMarkToEndOfLog(messagesLog);
        Thread[] threads = new Thread[threadsToRun];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < pageHits; j++) {
                            //Try: Numbering hits to see which thread is missing
                            hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        //sleep to check, keep redoing multiple times, fail if failure
        List<String> systemErrOutput = server.findStringsInLogsUsingMark("SystemErr.*", messagesLog);
        for (int j = 0; j < 5; j++) {
            if (systemErrOutput.size() != expectedErrOut) {
                systemErrOutput = server.findStringsInLogsUsingMark("SystemErr.*", messagesLog);
                //Try: print number to check if its growing or not
                Thread.sleep(500);
            }
        }
        assertTrue("Expected " + expectedErrOut + " events but found " + systemErrOutput.size() + " messages.", systemErrOutput.size() == expectedErrOut);

    }

    @Test
    public void testPrintStackTraceEventSizeThreshold() throws Exception {
        // Set default to the maximum system stream print event size threshold
        int maxSystemStreamPrintEventThresholdSize = 256 * 1024;

        // JDK's system stream print event flush size threshold
        int jdkSystemStreamPrintEventThreshold = 8 * 1024;
        int expectedSystemStreamPrintEventSize = jdkSystemStreamPrintEventThreshold;

        //Get the system stream print event threshold size which was set
        if (server.getServerEnv().getProperty("WLP_LOGGING_MAX_SYSTEM_STREAM_PRINT_EVENT_SIZE") != null) {
            try {
                maxSystemStreamPrintEventThresholdSize = Integer.valueOf(server.getServerEnv().getProperty("WLP_LOGGING_MAX_SYSTEM_STREAM_PRINT_EVENT_SIZE"));
            } catch (NumberFormatException e) {
            }
        }

        // Find the maximum possible length that the stack trace can be
        while (expectedSystemStreamPrintEventSize < maxSystemStreamPrintEventThresholdSize)
            expectedSystemStreamPrintEventSize += jdkSystemStreamPrintEventThreshold;

        // Specify the length of the error to be longer than maxSystemStreamPrintEventSize
        String sizeRequestParamater = String.valueOf((expectedSystemStreamPrintEventSize + 1) * 2);

        messagesLog = server.getDefaultLogFile();
        server.setMarkToEndOfLog(messagesLog);
        hitWebPage("broken-servlet", "ExtremelyLongStackTrace?size=" + sizeRequestParamater, false);
        List<String> systemErrOutput = server.findStringsInLogsUsingMark("SystemErr.*", messagesLog);

        String errorMessagePrefix = "SystemErr                                                    R ";

        // Check that each system error output message does not surpass the threshold
        for (int i = 0; i < systemErrOutput.size(); i++) {
            // Get the original message output in the stacktrace
            String currentSystemStreamPrintEvent = systemErrOutput.get(i).substring(systemErrOutput.get(i).indexOf(errorMessagePrefix) + errorMessagePrefix.length());
            assertTrue("Expected system stream print event size to be <= " + expectedSystemStreamPrintEventSize + " but found that the size is "
                       + currentSystemStreamPrintEvent.length() + ".",
                       currentSystemStreamPrintEvent.length() <= expectedSystemStreamPrintEventSize);
        }

    }
}
