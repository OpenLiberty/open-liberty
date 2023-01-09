/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This test class ensures that stack traces are written as single events
 */
@RunWith(FATRunner.class)
public class StackJoinerTest {

    @Server("com.ibm.ws.logging.stackjoiner.eventSizeThreshold")
    public static LibertyServer eventSizeThresholdServer;

    @Server("com.ibm.ws.logging.stackjoiner.bootStrapConfig")
    public static LibertyServer bootStrapConfiguredServer;

    @Server("com.ibm.ws.logging.stackjoiner.envConfig")
    public static LibertyServer envConfiguredServer;

    @Server("com.ibm.ws.logging.stackjoiner.serverXMLConfig")
    public static LibertyServer serverXMLConfiguredServer;

    @Server("com.ibm.ws.logging.stackjoiner.envAndBootStrapConfig")
    public static LibertyServer envAndBootStrapConfiguredServer;

    @Server("com.ibm.ws.logging.stackjoiner.dynamicConfig")
    public static LibertyServer dynamicConfigurationServer;

    public static LibertyServer currentServer;

    protected static RemoteFile messageLog;
    protected static RemoteFile traceLog;
    protected static RemoteFile consoleLog;

    static final int CONN_TIMEOUT = 10;
    static final int THREADS_TO_RUN = 5;
    static final int EXPECTED_ERR_OUT = 2500;
    static final int PAGE_HITS = 500;

    protected static void hitWebPage(String contextRoot, String servletName, boolean failureAllowed) throws MalformedURLException, IOException, ProtocolException {
        try {
            URL url = new URL("http://" + currentServer.getHostname() + ":" + currentServer.getHttpDefaultPort() + "/" + contextRoot + "/" + servletName);
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
        ShrinkHelper.defaultDropinApp(bootStrapConfiguredServer, "broken-servlet", "com.ibm.ws.logging.fat.broken.servlet");
        ShrinkHelper.defaultDropinApp(envConfiguredServer, "broken-servlet", "com.ibm.ws.logging.fat.broken.servlet");
        ShrinkHelper.defaultDropinApp(serverXMLConfiguredServer, "broken-servlet", "com.ibm.ws.logging.fat.broken.servlet");
        ShrinkHelper.defaultDropinApp(envAndBootStrapConfiguredServer, "broken-servlet", "com.ibm.ws.logging.fat.broken.servlet");
        ShrinkHelper.defaultDropinApp(dynamicConfigurationServer, "broken-servlet", "com.ibm.ws.logging.fat.broken.servlet");
        ShrinkHelper.defaultDropinApp(eventSizeThresholdServer, "broken-servlet", "com.ibm.ws.logging.fat.broken.servlet");
    }

    @After
    public void tearDown() throws Exception {

        if (currentServer != null && currentServer.isStarted()) {
            currentServer.stopServer();
        }

        messageLog = null;
        traceLog = null;
        consoleLog = null;
        currentServer = null;

    }

    /**
     * Acquires all three log files and sets mark.
     *
     * @throws Exception
     */
    private static void logSetup() throws Exception {
        consoleLog = currentServer.getConsoleLogFile();
        messageLog = currentServer.getDefaultLogFile();
        traceLog = currentServer.getMostRecentTraceFile();

        currentServer.setMarkToEndOfLog(consoleLog, messageLog, traceLog);
    }

    @Test
    /**
     * Test that bootstrap configuration works
     * where com.ibm.ws.logging.stackJoin=true
     *
     * @throws Exception
     */
    public void testBootStrapConfig() throws Exception {
        currentServer = bootStrapConfiguredServer;
        currentServer.startServer();

        logSetup();

        hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);

        List<String> consoleErrors = currentServer.findStringsInLogsUsingMark("err.*", consoleLog);
        List<String> messageErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", messageLog);
        List<String> traceErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", traceLog);

        assertTrue("Expected (Console) stack trace as a single event but found " + consoleErrors.size() + " messages. => " + consoleErrors.toString(), consoleErrors.size() == 1);
        assertTrue("Expected (Message) stack trace as a single event but found " + messageErrors.size() + " messages. => " + messageErrors.toString(), messageErrors.size() == 1);
        assertTrue("Expected (Trace) stack trace as a single event but found " + traceErrors.size() + " messages. => " + traceErrors.toString(), traceErrors.size() == 1);
    }

    @Test
    /**
     * Test that environment variable configuration works
     * where WLP_LOGGING_STACK_JOIN=true
     *
     * @throws Exception
     */
    public void testEnvConfig() throws Exception {
        currentServer = envConfiguredServer;
        currentServer.startServer();

        logSetup();

        hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);

        List<String> consoleErrors = currentServer.findStringsInLogsUsingMark("err.*", consoleLog);
        List<String> messageErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", messageLog);
        List<String> traceErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", traceLog);

        assertTrue("Expected (Console) stack trace as a single event but found " + consoleErrors.size() + " messages. => " + consoleErrors.toString(), consoleErrors.size() == 1);
        assertTrue("Expected (Message) stack trace as a single event but found " + messageErrors.size() + " messages. => " + messageErrors.toString(), messageErrors.size() == 1);
        assertTrue("Expected (Trace) stack trace as a single event but found " + traceErrors.size() + " messages. => " + traceErrors.toString(), traceErrors.size() == 1);
    }

    @Test
    /**
     * Test that server XML configuration works
     * where <logging stackJoin="true"/>
     *
     * @throws Exception
     */
    public void testServerXMLConfig() throws Exception {
        currentServer = serverXMLConfiguredServer;
        currentServer.startServer();

        logSetup();

        hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);

        List<String> consoleErrors = currentServer.findStringsInLogsUsingMark("err.*", consoleLog);
        List<String> messageErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", messageLog);
        List<String> traceErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", traceLog);

        assertTrue("Expected (Console) stack trace as a single event but found " + consoleErrors.size() + " messages. => " + consoleErrors.toString(), consoleErrors.size() == 1);
        assertTrue("Expected (Message) stack trace as a single event but found " + messageErrors.size() + " messages. => " + messageErrors.toString(), messageErrors.size() == 1);
        assertTrue("Expected (Trace) stack trace as a single event but found " + traceErrors.size() + " messages. => " + traceErrors.toString(), traceErrors.size() == 1);
    }

    @Test
    /**
     * Both Environment Variable and BootStrap property is configured
     *
     * WLP_LOGGING_STACK_JOIN=true
     * com.ibm.ws.logging.stackJoin=false
     *
     * Since bootstrap property takes precedence. We expect the stack joiner to be disabled.
     *
     * @throws Exception
     */
    public void testEnvAndBootStrapConfig() throws Exception {
        currentServer = envAndBootStrapConfiguredServer;
        currentServer.startServer();

        logSetup();

        hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);

        List<String> consoleErrors = currentServer.findStringsInLogsUsingMark("err.*", consoleLog);
        List<String> messageErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", messageLog);
        List<String> traceErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", traceLog);

        assertTrue("Expected (Console) stack trace to print each line as a new logging event/record.", consoleErrors.size() > 1);
        assertTrue("Expected (Message) stack trace to print each line as a new logging event/record.", messageErrors.size() > 1);
        assertTrue("Expected (Trace) stack trace to print each line as a new logging event/record. ", traceErrors.size() > 1);
    }

    @Test
    /**
     * Load with stack joining function enabled through bootstrap properties.
     *
     * Check stack joiner functionality is enabled.
     *
     * Load configuration change where server.xml disables stack joiner.
     *
     * Check stack joiner functionality is disabled.
     *
     * Load configuration change where server.xml enables stack joiner.
     *
     * Check stack joiner functionality is enabled.
     *
     * @throws Exception
     */
    public void testDynamicConfig() throws Exception {
        currentServer = dynamicConfigurationServer;
        currentServer.startServer();

        logSetup();

        hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);

        List<String> consoleErrors = currentServer.findStringsInLogsUsingMark("err.*", consoleLog);
        List<String> messageErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", messageLog);
        List<String> traceErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", traceLog);

        assertTrue("Expected (Console) stack trace as a single event but found " + consoleErrors.size() + " messages. => " + consoleErrors.toString(), consoleErrors.size() == 1);
        assertTrue("Expected (Message) stack trace as a single event but found " + messageErrors.size() + " messages. => " + messageErrors.toString(), messageErrors.size() == 1);
        assertTrue("Expected (Trace) stack trace as a single event but found " + traceErrors.size() + " messages. => " + traceErrors.toString(), traceErrors.size() == 1);

        currentServer.setServerConfigurationFile("stackJoiner/server-disableStackJoiner.xml");
        Assert.assertNotNull("CWWKG0017I NOT FOUND", currentServer.waitForStringInLogUsingMark("CWWKG0017I"));

        currentServer.setMarkToEndOfLog(consoleLog, messageLog, traceLog);

        hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);

        consoleErrors = currentServer.findStringsInLogsUsingMark("err.*", consoleLog);
        messageErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", messageLog);
        traceErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", traceLog);

        assertTrue("Expected (Console) stack trace to print each line as a new logging event/record.", consoleErrors.size() > 1);
        assertTrue("Expected (Message) stack trace to print each line as a new logging event/record.", messageErrors.size() > 1);
        assertTrue("Expected (Trace) stack trace to print each line as a new logging event/record. ", traceErrors.size() > 1);

        currentServer.setServerConfigurationFile("stackJoiner/server-enableStackJoiner.xml");
        Assert.assertNotNull("CWWKG0017I NOT FOUND", currentServer.waitForStringInLogUsingMark("CWWKG0017I"));

        currentServer.setMarkToEndOfLog(consoleLog, messageLog, traceLog);

        hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);

        consoleErrors = currentServer.findStringsInLogsUsingMark("err.*", consoleLog);
        messageErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", messageLog);
        traceErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", traceLog);

        assertTrue("Expected (Console) stack trace as a single event but found " + consoleErrors.size() + " messages. => " + consoleErrors.toString(), consoleErrors.size() == 1);
        assertTrue("Expected (Message) stack trace as a single event but found " + messageErrors.size() + " messages. => " + messageErrors.toString(), messageErrors.size() == 1);
        assertTrue("Expected (Trace) stack trace as a single event but found " + traceErrors.size() + " messages. => " + traceErrors.toString(), traceErrors.size() == 1);
    }

    @Test
    /**
     * Tests printing of stack joined exception with messages log
     *
     * Generates 5 threads which hit the servlet 500 times each.
     *
     * @throws Exception
     */
    public void testPrintStackTraceMultipleThreads() throws Exception {
        currentServer = serverXMLConfiguredServer;
        currentServer.startServer();

        logSetup();

        Thread[] threads = new Thread[THREADS_TO_RUN];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < PAGE_HITS; j++) {
                            //Try: Numbering hits to see which thread is missing
                            hitWebPage("broken-servlet", "ExceptionPrintingServlet", false);
                        }
                    } catch (Exception e) {
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
        List<String> messageErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", messageLog);
        for (int j = 0; j < 5; j++) {
            if (messageErrors.size() != EXPECTED_ERR_OUT) {
                messageErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", messageLog);
                //Try: print number to check if its growing or not
                Thread.sleep(500);
            }
        }
        assertTrue("Expected " + EXPECTED_ERR_OUT + " events but found " + messageErrors.size() + " messages.", messageErrors.size() == EXPECTED_ERR_OUT);

    }

    @Test
    public void testPrintStackTracePrintStream() throws Exception {
        currentServer = serverXMLConfiguredServer;
        currentServer.startServer();

        logSetup();

        hitWebPage("broken-servlet", "BrokenWithCustomPrintStreamServlet", false);

        List<String> consoleErrors = currentServer.findStringsInLogsUsingMark("err.*", consoleLog);
        List<String> messageErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", messageLog);
        List<String> traceErrors = currentServer.findStringsInLogsUsingMark("SystemErr.*", traceLog);

        assertTrue("(Console) Expected two single-lined stack traced to be found but found " + consoleErrors.size() + "messages. =>" + consoleErrors.toString(),
                   consoleErrors.size() == 2);
        assertTrue("(Message) Expected two single-lined stack traced to be found but found " + messageErrors.size() + "messages. =>" + messageErrors.toString(),
                   messageErrors.size() == 2);
        assertTrue("(Trace) Expected two single-lined stack traced to be found but found " + traceErrors.size() + "messages. =>" + traceErrors.toString(), traceErrors.size() == 2);

    }

    @Test
    public void testPrintStackTraceEventSizeThreshold() throws Exception {

        final String errorMessagePrefix = "SystemErr                                                    R ";

        currentServer = eventSizeThresholdServer;
        currentServer.startServer();

        logSetup();

        // Set default to the maximum system stream print event size threshold
        int maxSystemStreamPrintEventThresholdSize = 256 * 1024;

        // JDK's system stream print event flush size threshold
        final int jdkSystemStreamPrintEventThreshold = 8 * 1024;
        int expectedSystemStreamPrintEventSize = jdkSystemStreamPrintEventThreshold;

        //Get the system stream print event threshold size which was set
        if (currentServer.getServerEnv().getProperty("WLP_LOGGING_MAX_SYSTEM_STREAM_PRINT_EVENT_SIZE") != null) {
            try {
                maxSystemStreamPrintEventThresholdSize = Integer.valueOf(currentServer.getServerEnv().getProperty("WLP_LOGGING_MAX_SYSTEM_STREAM_PRINT_EVENT_SIZE"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        // Find the maximum possible length that the stack trace can be
        while (expectedSystemStreamPrintEventSize < maxSystemStreamPrintEventThresholdSize)
            expectedSystemStreamPrintEventSize += jdkSystemStreamPrintEventThreshold;

        // Specify the length of the error to be longer than maxSystemStreamPrintEventSize
        String sizeRequestParamater = String.valueOf((expectedSystemStreamPrintEventSize + 1) * 2);

        messageLog = currentServer.getDefaultLogFile();
        currentServer.setMarkToEndOfLog(messageLog);

        hitWebPage("broken-servlet", "ExtremelyLongStackTrace?size=" + sizeRequestParamater, false);
        List<String> systemErrOutput = currentServer.findStringsInLogsUsingMark("SystemErr.*", messageLog);

        // Check that each system error output message does not surpass the threshold
        for (int i = 0; i < systemErrOutput.size(); i++) {
            // Get the original message output in the stacktrace
            String currentSystemStreamPrintEvent = systemErrOutput.get(i).substring(systemErrOutput.get(i).indexOf(errorMessagePrefix) + errorMessagePrefix.length());
            assertTrue("Expected system stream print event size to be <= " + expectedSystemStreamPrintEventSize + " but found that the size is "
                       + currentSystemStreamPrintEvent.length() + ".",
                       currentSystemStreamPrintEvent.length() <= expectedSystemStreamPrintEventSize);
            Log.info(getClass(), "testPrintStackTraceEventSizeThreshold",
                     "The current system streamt print event length " + currentSystemStreamPrintEvent.length() + ". The expected system stream print event length "
                                                                          + expectedSystemStreamPrintEventSize);
        }

    }
}
