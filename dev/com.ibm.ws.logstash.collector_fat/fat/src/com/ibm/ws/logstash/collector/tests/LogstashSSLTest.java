/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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
package com.ibm.ws.logstash.collector.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class LogstashSSLTest extends LogstashCollectorTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("LogstashServer");
    protected static Machine machine = null;
    private static boolean connected = false;

    private String testName = "";
    private static Class<?> c = LogstashSSLTest.class;
    public static String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/";
    private static String os = "";
    private static boolean found_liberty_gc_at_startup = false;

    protected static boolean runTest = true;

    @BeforeClass
    public static void setUp() throws Exception {
        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        os = System.getProperty("os.name").toLowerCase();
        if (os != null && (os.contains("os/390") || os.contains("z/os") || os.contains("zos")))
            runTest = false;

        Log.info(c, "setUp", "os.name = " + os);
        Log.info(c, "setUp", "runTest = " + runTest);

        Assume.assumeTrue(runTest); // runTest must be true to run test

        clearContainerOutput();
        String host = logstashContainer.getHost();
        String port = String.valueOf(logstashContainer.getMappedPort(5043));
        Log.info(c, "setUp", "Logstash container: host=" + host + "  port=" + port);
        server.addEnvVar("LOGSTASH_HOST", host);
        server.addEnvVar("LOGSTASH_PORT", port);

        Log.info(c, "setUp", "---> Setting default logstash configuration.");
        server.setServerConfigurationFile("server_logs_all.xml");
        ShrinkHelper.defaultDropinApp(server, "LogstashApp", "com.ibm.logs");

        serverStart();

        // GC events are usually emitted during server startup
        if (!checkGcSpecialCase()) {
            found_liberty_gc_at_startup = waitForStringInContainerOutput(LIBERTY_GC) != null;
        }

        assertNotNull("The application is not ready", server.waitForStringInLogUsingMark("CWWKT0016I", 10000));
        assertNotNull("Cannot find TRAS0218I from Logstash output", waitForStringInContainerOutput("TRAS0218I"));
        clearContainerOutput();
    }

    @Before
    public void setUpTest() throws Exception {
        testName = "setUpTest";
        if (!server.isStarted()) {
            serverStart();
        }
    }

    @Test
    public void testLogstashDefaultConfig() throws Exception {
        testName = "testLogstashDefaultConfig";
        setConfig("server_default_conf.xml");
        assertNotNull("Cannot find TRAS0218I from messages.log", server.waitForStringInLogUsingMark("TRAS0218I"));
        assertNotNull("Cannot find TRAS0218I from Logstash output", waitForStringInContainerOutput("TRAS0218I"));
        clearContainerOutput();

        int numOfMsg = 10;
        Log.info(c, testName, "---> Running the application.. ");
        Set<String> checkSet = new HashSet<String>();
        for (int i = 1; i <= numOfMsg; i++) {
            createMessageEvent(testName + " " + i);
            checkSet.add(MESSAGE_PREFIX + " " + testName + " " + i);
        }

        assertEquals(numOfMsg, waitForContainerOutputSize(numOfMsg));
        assertNotNull("Cannot find message " + testName + " from Logstash output", waitForStringInContainerOutput(testName));

        List<JSONObject> list = parseJsonInContainerOutput();
        for (JSONObject jobj : list) {
            String value = (String) jobj.get(KEY_MESSAGE);
            checkSet.remove(value);
        }
        assertTrue("Did not get all messages from Logstash container. Missing " + checkSet.size(), checkSet.isEmpty());
    }

    @Test
    public void testLogstash() throws Exception {
        testName = "testLogstash";

        //Look for feature started message.
        boolean feature = this.isConnected();

        Log.info(c, testName, "---> Did Logstash feature start ? : " + feature);
        assertTrue("logstashCollector-1.0 did not show as started..", feature);

    }

    @Test
    @AllowedFFDC({ "java.lang.NullPointerException" })
    public void testLogstashEvents() throws Exception {
        testName = "testLogstashEvents";

        setConfig("server_logs_all.xml");
        clearContainerOutput();

        createMessageEvent(testName);
        createTraceEvent();
        createFFDCEvent(1);

        assertNotNull(LIBERTY_MESSAGE + " not found", waitForStringInContainerOutput(LIBERTY_MESSAGE));
        assertNotNull(LIBERTY_TRACE + " not found", waitForStringInContainerOutput(LIBERTY_TRACE));
        assertNotNull(LIBERTY_FFDC + " not found", waitForStringInContainerOutput(LIBERTY_FFDC));
        assertNotNull(LIBERTY_ACCESSLOG + " not found", waitForStringInContainerOutput(LIBERTY_ACCESSLOG));
    }

    @Test
    public void testLogstashForMessageEvent() throws Exception {
        testName = "testLogstashForMessageEvent";
        setConfig("server_logs_msg.xml");
        clearContainerOutput();

        createMessageEvent(testName);

        assertNotNull("Cannot find TRAS0218I from messages.log", server.waitForStringInLogUsingMark("TRAS0218I"));
        assertNotNull("Did not find " + LIBERTY_MESSAGE, waitForStringInContainerOutput(LIBERTY_MESSAGE));
    }

    @Test
    public void testLogstashForMessageWithExceptionEvent() throws Exception {
        testName = "testLogstashForMessageWithExceptionEvent";
        setConfig("server_logs_msg.xml");
        clearContainerOutput();

        createMessageEventWithException(testName);

        assertNotNull("Cannot find TRAS0218I from messages.log", server.waitForStringInLogUsingMark("TRAS0218I"));

        /**
         * The exception servlet emits three exceptions.
         * Given that the current setup of this FAT is using regex to parse the messages
         * and that the JSON fields can be ordered in a varied order, we'll use each message to test an individual field.
         * First assert will just check that the message is there.
         * Second will check that the exceptionName is present.
         * Third will check that the stacktrace field is present.
         *
         */
        assertNotNull("exception message not found", waitForStringInContainerOutput("\"message\":\"exception message\""));

        String line = waitForStringInContainerOutput("\"message\":\"second exception message\"");
        assertNotNull("second exception message not found", line);
        assertTrue(line.contains("\"exceptionName\":\"java.lang.IllegalArgumentException\""));

        line = waitForStringInContainerOutput("\"message\":\"third exception message\"");
        assertNotNull("third exception message not found", line);
        assertTrue(line.contains("\"stackTrace\":\"java.lang.IllegalArgumentException: bad"));
    }

    @Test
    public void testLogstashForAccessEvent() throws Exception {
        testName = "testLogstashForAccessEvent";
        setConfig("server_logs_access.xml");
        clearContainerOutput();

        createAccessLogEvent(testName);

        assertNotNull("Cannot find TRAS0218I from messages.log", server.waitForStringInLogUsingMark("TRAS0218I"));
        assertNotNull("Did not find " + LIBERTY_ACCESSLOG, waitForStringInContainerOutput(testName));
    }

    @Test
    public void testLogstashForGCEvent() throws Exception {
        testName = "testLogstashForGCEvent";
        if (checkGcSpecialCase()) {
            return;
        }
        setConfig("server_logs_gc.xml");
        clearContainerOutput();

        // Do some work and hopefully some GC events will be created
        for (int i = 1; i <= 10; i++) {
            createGCEvent();
        }
        assertTrue(LIBERTY_GC + " not found", waitForStringInContainerOutput(LIBERTY_GC) != null || found_liberty_gc_at_startup);
    }

    @Test
    @AllowedFFDC({ "java.lang.ArithmeticException", "java.lang.ArrayIndexOutOfBoundsException" })
    public void testLogstashForFFDCEvent() throws Exception {
        testName = "testLogstashForFFDCEvent";
        setConfig("server_logs_ffdc.xml");
        clearContainerOutput();

        Log.info(c, testName, "------> starting ffdc2(ArithmeticException), "
                              + "ffdc3(ArrayIndexOutOfBoundsException)");
        List<String> exceptions = new ArrayList<String>();
        createFFDCEvent(2);
        Log.info(c, testName, "------> finished ffdc2(ArithmeticException)");
        exceptions.add("ArithmeticException");
        createFFDCEvent(3);
        Log.info(c, testName, "------> finished ffdc3(ArrayIndexOutOfBoundsException)");
        exceptions.add("ArrayIndexOutOfBoundsException");
        assertNotNull("Cannot find TRAS0218I from messages.log", server.waitForStringInLogUsingMark("TRAS0218I"));

        assertNotNull(LIBERTY_FFDC + " not found", waitForStringInContainerOutput(LIBERTY_FFDC));
        assertNotNull("ArithmeticException not found", waitForStringInContainerOutput("ArithmeticException"));
        assertNotNull("ArrayIndexOutOfBoundsException not found", waitForStringInContainerOutput("ArrayIndexOutOfBoundsException"));
    }

    @Test
    public void testLogstashForTraceEvent() throws Exception {
        testName = "testLogstashForTraceEvent";
        setConfig("server_logs_trace.xml");
        clearContainerOutput();

        createTraceEvent(testName);

        assertNotNull("Did not find " + LIBERTY_TRACE, waitForStringInContainerOutput(LIBERTY_TRACE));
    }

    @Test
    public void testLogstashForAuditEvent() throws Exception {
        testName = "testLogstashForAuditEvent";
        setConfig("server_logs_audit.xml");
        clearContainerOutput();

        createTraceEvent(testName);

        assertNotNull("Did not find " + LIBERTY_AUDIT, waitForStringInContainerOutput(LIBERTY_AUDIT));
    }

    @Test
    public void testLogstashForAudit20Event() throws Exception {
        testName = "testLogstashForAuditEvent";
        setConfig("server_logs_audit20.xml");
        clearContainerOutput();

        createTraceEvent(testName);

        assertNotNull("Did not find " + LIBERTY_AUDIT, waitForStringInContainerOutput(LIBERTY_AUDIT));
    }

    @Test
    public void testLogstashEntryExitEvents() throws Exception {
        testName = "testLogstashEntryExitEvents";
        setConfig("server_logs_trace.xml");
        clearContainerOutput();

        createTraceEvent(testName);

        assertNotNull("Did not find " + ENTRY, waitForStringInContainerOutput(ENTRY));
        assertNotNull("Did not find " + EXIT, waitForStringInContainerOutput(EXIT));
    }

    @Test
    public void testLogstashDynamicDisableFeature() throws Exception {
        testName = "testLogstashDynamicDisableFeature";
        setConfig("server_disable.xml");
        server.waitForStringInLogUsingMark("CWWKF0013I", 10000);

        boolean removed = false;
        List<String> lines = server.findStringsInLogsAndTraceUsingMark("CWWKF0013I");
        assertTrue("Feature not removed..", lines.size() > 0);
        String line = lines.get(lines.size() - 1);
        Log.info(c, testName, "---> line : " + line);
        if (line.contains("logstashCollector-1.0")) {
            removed = true;
        }

        Log.info(c, testName, "---> Did Logstash feature STOP ? : " + removed);
        assertTrue("logstashCollector-1.0 show as started..", removed);

        server.setMarkToEndOfLog();
        setConfig("server_logs_msg.xml");

        boolean feature = this.isConnected();
        Log.info(c, testName, "---> Did LogstashCollector feature START ? : " + feature);
        assertTrue("logstashCollector-1.0 did not show as started..", feature);

    }

    @Test
    public void testLogstashDynamicDisableEventType() throws Exception {
        testName = "testLogstashDynamicDisableEventType";
        setConfig("server_logs_msg.xml");
        clearContainerOutput();

        String msg1 = testName + " 1 - should appear in logstash output";
        createMessageEvent(msg1);

        setConfig("server_logs_trace.xml");

        String msg2 = testName + " 2 - should NOT appear in logstash output";
        createMessageEvent(msg2);
        String msg3 = testName + " 3 - should appear in logstash output";
        createTraceEvent(msg3);

        assertNotNull("Did not find " + msg1, waitForStringInContainerOutput(msg1));
        assertNotNull("Did not find " + msg3, waitForStringInContainerOutput(msg3));
        assertNull("Found " + msg2, findStringInContainerOutput(msg2));
    }

    /*
     * This test determines whether source subscriptions are kept when they are present both
     * before and after a server configuration change, and additionally if source subscriptions
     * are unsubscribed when a source is no longer present in the server configuration
     */
    @Test
    public void testModifiedSourceSubscription() throws Exception {
        RemoteFile traceFile = server.getMostRecentTraceFile();
        testName = "testModifiedSourceSubscription";

        //Clearing all sources
        server.setMarkToEndOfLog(traceFile);
        server.setMarkToEndOfLog();
        Log.info(c, testName, "Initializing: Unsubscribing from all sources");
        setConfig("server_no_sources.xml");

        //Specify two sources: message and trace
        //Check if both sources are subscribed to initially
        server.setMarkToEndOfLog(traceFile);
        server.setMarkToEndOfLog();
        setConfig("server_message_trace.xml");

        //listOfSourcesToSubscribe [com.ibm.ws.logging.source.message|memory, com.ibm.ws.logging.source.trace|memory]
        List<String> lines = server.findStringsInLogsAndTraceUsingMark("listOfSourcesToSubscribe.\\[com\\.ibm\\.ws\\.logging\\.source\\.message\\|memory,.com\\.ibm\\.ws\\.logging\\.source\\.trace\\|memory\\]");
        Log.info(c, testName,
                 "Number of lines containing \"listOfSourcesToSubscribe.\\[com\\.ibm\\.ws\\.logging\\.source\\.message\\|memory,.com\\.ibm\\.ws\\.logging\\.source\\.trace\\|memory\\]\":"
                              + lines.size());
        //Check for both orderings, just in case
        if (lines.size() == 0) {
            lines = server.findStringsInLogsAndTraceUsingMark("listOfSourcesToSubscribe.\\[com\\.ibm\\.ws\\.logging\\.source\\.trace\\|memory,.com\\.ibm\\.ws\\.logging\\.source\\.message\\|memory\\]");
            Log.info(c, testName,
                     "Number of lines containing \"listOfSourcesToSubscribe.\\[com\\.ibm\\.ws\\.logging\\.source\\.trace\\|memory,.com\\.ibm\\.ws\\.logging\\.source\\.message\\|memory\\]\":"
                                  + lines.size());
        }
        assertTrue("Initialization failure: Sources message and/or trace were not subscribed to", lines.size() > 0);

        //Specify two sources: message and accessLog
        //Check if message is kept, trace is unsubscribed, and accessLog is subscribed
        server.setMarkToEndOfLog(traceFile);
        server.setMarkToEndOfLog();
        setConfig("server_message_access.xml");
        Log.info(c, testName, "Checking for unsubscription from trace and subscription to access");

        //Message was present both before and after, so it shouldn't be unsubscribed or resubscribed after this config change
        lines = server.findStringsInLogsAndTraceUsingMark("listOfSourcesToUnsubscribe.\\[com\\.ibm.ws\\.logging\\.source\\.message\\|memory\\]");
        Log.info(c, testName, "Number of lines containing \"listOfSourcesToUnsubscribe.\\[com\\.ibm.ws\\.logging\\.source\\.message\\|memory\\]\":" + lines.size());
        assertTrue("Message was unsubscribed when it was supposed to be kept", lines.size() == 0);
        lines = server.findStringsInLogsAndTraceUsingMark("listOfSourcesToSubscribe.\\[com\\.ibm\\.ws\\.logging\\.source\\.message\\|memory\\]");
        Log.info(c, testName, "Number of lines containing \"listOfSourcesToSubscribe.\\[com\\.ibm\\.ws\\.logging\\.source\\.message\\|memory\\]\":" + lines.size());
        assertTrue("Message was subscribed to again after configuration change ", lines.size() == 0);

        //Trace is no longer present, and should have been unsubscribed
        lines = server.findStringsInLogsAndTraceUsingMark("listOfSourcesToUnsubscribe.\\[com\\.ibm\\.ws\\.logging\\.source\\.trace\\|memory\\]");
        Log.info(c, testName, "Number of lines containing \"listOfSourcesToUnsubscribe.\\[com\\.ibm\\.ws\\.logging\\.source\\.trace\\|memory\\]\":" + lines.size());
        assertTrue("Trace source was not unsubscribed when it was supposed to be unsubscribed", lines.size() > 0);

        //AccessLog is newly present, and should be subscribed
        lines = server.findStringsInLogsAndTraceUsingMark("listOfSourcesToSubscribe.\\[com\\.ibm\\.ws\\.http\\.logging\\.source\\.accesslog\\|memory\\]");
        Log.info(c, testName, "Number of lines containing \"listOfSourcesToSubscribe.\\[com\\.ibm\\.ws\\.http\\.logging\\.source\\.accesslog\\|memory\\]\":" + lines.size());
        assertTrue("AccessLog source was not subscribed when it was supposed to be subscribed", lines.size() > 0);
    }

    @After
    public void tearDown() {
    }

    //TODO add other methods to validate posted trace data

    /**
     * Reset server configurations
     *
     * @throws Exception
     */
    @Before
    public void resetServer() throws Exception {
        setConfig("server_reset.xml");
        clearContainerOutput();
    }

    @AfterClass
    public static void completeTest() throws Exception {
        if (!runTest) {
            return;
        }

        try {
            if (server.isStarted()) {
                Log.info(c, "completeTest", "---> Stopping server..");
                server.stopServer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isConnected() throws Exception {
        if (!connected) {
            List<String> lines = server.findStringsInLogs("CWWKF0012I");
            for (String line : lines) {
                if (line.contains("logstashCollector-1.0")) {
                    Log.info(c, testName, "---> line : " + line);
                    connected = true;
                }
            }
        }
        return connected;
    }

    private static void serverStart() throws Exception {
        Log.info(c, "serverStart", "--->  Starting Server.. ");
        server.startServer();

        Log.info(c, "serverStart", "---> Wait for feature to start ");
        // CWWKZ0001I: Application LogstashApp started in x seconds.
        assertNotNull("Cannot find CWWKZ0001I from messages.log", server.waitForStringInLogUsingMark("CWWKZ0001I", 15000));

        // Comment this to debug a build break.  This check might be redundant as we check the same message ID in the container output in the next step.
        // Log.info(c, "serverStart", "---> Wait for application to start ");
        // // CWWKT0016I: Web application available (default_host): http://localhost:8010/LogstashApp/
        // assertNotNull("Cannot find CWWKT0016I from messages.log", server.waitForStringInLogUsingMark("CWWKT0016I", 10000));

        // Wait for CWWKT0016I in Logstash container output
        waitForStringInContainerOutput("CWWKT0016I");
    }

    private static boolean checkGcSpecialCase() {
        String methodName = "checkGcSpecialCase";
        /**
         * Check if if belongs to the special case where build machine does not have Health centre installed, which prevents gc event to be produced
         * by checking 1. whether the operating system is Mac or linux 2. whether the machine is running IBM JDK
         * if both checks pass, this is the case
         **/
        Log.info(c, methodName, "os_name: " + os + "\t java_jdk: " + System.getProperty("java.vendor"));
        String JAVA_HOME = System.getenv("JAVA_HOME");
        Log.info(c, methodName, "JAVA_HOME: " + JAVA_HOME);
        boolean healthCenterInstalled = false;
        if (JAVA_HOME == null) {
            Log.info(c, methodName, " unable to find JAVA_HOME variable");
        } else if (JAVA_HOME.endsWith("jre")) {
            if (new File(JAVA_HOME + "/lib/ext/healthcenter.jar").exists()) {
                healthCenterInstalled = true;
                Log.info(c, methodName, " jar file for health center under path " + JAVA_HOME + "/lib/ext/healthcenter.jar");
            }
            Log.info(c, methodName, " jar file for health center under path " + JAVA_HOME + "/lib/ext/healthcenter.jar exist:"
                                    + new File(JAVA_HOME + "/lib/ext/healthcenter.jar").exists());
        } else if ((JAVA_HOME.endsWith("/bin")) || (JAVA_HOME.endsWith("\\bin"))) {
            healthCenterInstalled = findHealthCenterDirecotry(JAVA_HOME.substring(0, JAVA_HOME.length() - 4));
            if (!healthCenterInstalled) {
                Log.info(c, methodName, " unable to find heathcenter.jar, thus unable to produce gc events. Thus, this check will be by-passed");
            }
        } else {
            healthCenterInstalled = findHealthCenterDirecotry(JAVA_HOME);
            if (!healthCenterInstalled) {
                Log.info(c, methodName, " unable to find heathcenter.jar, thus unable to produce gc events. Thus, this check will be by-passed");
            }
        }
        if (os.contains("mac") || !System.getProperty("java.vendor").toLowerCase().contains("ibm")
            || System.getProperty("java.vendor.url").toLowerCase().contains("sun") || !healthCenterInstalled) {
            return true;
        }
        // Skip zOS temporary as zOS JDK has a bug that does not return any GC information.
        if (os.contains("os/390") || os.contains("z/os") || os.contains("zos")) {
            return true;
        }
        return false;
    }

    private static boolean findHealthCenterDirecotry(String directoryPath) {
        String methodName = "findHealthCenterDirecotry";
        boolean jarFileExist = false;
        File dirFile = new File(directoryPath);
        if (dirFile.exists()) {
            File[] files = dirFile.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    jarFileExist = findHealthCenterDirecotry(file.getAbsolutePath());
                    if (jarFileExist == true) {
                        return true;
                    }
                } else {
                    if (file.getAbsolutePath().contains("healthcenter.jar")) {
                        Log.info(c, methodName, " healthcetner.jar is found under path " + file.getAbsolutePath());
                        return true;
                    }
                }
            }
        } else {
            Log.info(c, methodName, "directoryPath " + directoryPath + " does not exist");
        }
        return jarFileExist;
    }

    /** {@inheritDoc} */
    @Override
    protected LibertyServer getServer() {
        return server;
    }

}
