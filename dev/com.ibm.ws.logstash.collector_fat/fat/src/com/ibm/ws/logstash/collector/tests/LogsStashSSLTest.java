/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logstash.collector.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.impl.Logstash;
import componenttest.topology.utils.FileUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@MinimumJavaLevel(javaLevel = 8)
@MaximumJavaLevel(javaLevel = 8)
public class LogsStashSSLTest extends LogstashCollectorTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("LogstashServer");
    protected static Machine machine = null;
    private static boolean connected = false;

    private String testName = "";
    private static Class<?> c = LogsStashSSLTest.class;
    private static String JVMSecurity = System.getProperty("Djava.security.properties");
    public static String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/";
    private static String os = "";

    private static Logstash logstash = new Logstash(server.getMachine());

    protected static boolean runTest;

    @BeforeClass
    public static void setUp() throws Exception {
        os = System.getProperty("os.name").toLowerCase();
        Log.info(c, "setUp", "os.name = " + os);

        runTest = logstash.isSupportedPlatform();
        Log.info(c, "setUp", "runTest = " + runTest);

        if (!runTest) {
            return;
        }

        // Change the logstash config file so that the SSL tests create their own output file.
        Logstash.CONFIG_FILENAME = "logstash.conf";
        Logstash.OUTPUT_FILENAME = "logstash_output.txt";

        logstash.start();

        Log.info(c, "setUp", "---> Setting default logstash configuration.");
        server.setServerConfigurationFile("server_logs_all.xml");
        String extendedPath = "usr/servers/LogstashServer/jvm.options";
        if (server.getServerRoot().contains(server.getInstallRoot())) {
            extendedPath = server.getServerRoot().replaceAll(server.getInstallRoot(), "").substring(1);
        }
        server.copyFileToLibertyInstallRoot(extendedPath, "jvm.options");
        server.copyFileToLibertyInstallRoot(extendedPath.replace("jvm.options", "java.security"), "java.security");
        ShrinkHelper.defaultDropinApp(server, "LogstashApp", "com.ibm.logs");

        serverStart();
    }

    @Before
    public void setUpTest() throws Exception {
        Assume.assumeTrue(runTest); // runTest must be true to run test

        testName = "setUpTest";
        if (!server.isStarted()) {
            serverStart();
        }
    }

    @Test
    //@Ignore("Ignoring testLogstashDefaultConfig for now, need to rewrite the logic later")
    public void testLogstashDefaultConfig() throws Exception {
        testName = "testLogstashDefaultConfig";
        server.setMarkToEndOfLog();
        logstash.setMarkToEndOfLog();

        setConfig("server_default_conf.xml");

        // Run App to generate events
        Log.info(c, testName, "---> Running the application.. ");
        for (int i = 1; i <= 10; i++) {
            createMessageEvent(testName + " " + i);
        }

        assertNotNull("Cannot find TRAS0218I from messages.log", server.waitForStringInLogUsingMark("TRAS0218I", 10000));
        assertNotNull("Cannot find message " + testName + " from Logstash output", logstash.waitForStringInLogUsingMark(testName));
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

        server.setMarkToEndOfLog();
        setConfig("server_logs_all.xml");

        logstash.setMarkToEndOfLog();
        int lastLine = logstash.parseOutputFile().size();
        Log.info(c, testName, "lastLine=" + lastLine);

        createMessageEvent(testName);
        createTraceEvent();
        logstash.waitForStringInLogUsingMark("liberty_trace");
        createFFDCEvent(1);
        logstash.waitForStringInLogUsingMark("liberty_ffdc");

        // Check results
        List<JSONObject> jObjList = logstash.parseOutputFile(lastLine + 1);
        Log.info(c, testName, "jObjList.size()=" + jObjList.size());
        boolean foundMessage = false;
        boolean foundFFDC = false;
        boolean foundTrace = false;
        boolean foundAccessLog = false;
        boolean foundGC = false;

        String type;
        for (JSONObject jObj : jObjList) {
            type = jObj.getString("type");
            if (type.equals(LIBERTY_MESSAGE)) {
                foundMessage = true;
            } else if (type.equals(LIBERTY_TRACE)) {
                foundTrace = true;
            } else if (type.equals(LIBERTY_FFDC)) {
                foundFFDC = true;
            } else if (type.equals(LIBERTY_ACCESSLOG)) {
                foundAccessLog = true;
            } else if (type.equals(LIBERTY_GC)) {
                foundGC = true;
            } else {
                fail("Invalid event type found: " + type);
            }
            if (foundMessage && foundTrace && foundFFDC && foundAccessLog && foundGC) {
                Log.info(c, testName, "All 5 event types found");
                return;
            }
        }
        assertTrue(LIBERTY_MESSAGE + " not found", foundMessage);
        assertTrue(LIBERTY_TRACE + " not found", foundTrace);
        assertTrue(LIBERTY_FFDC + " not found", foundFFDC);
        assertTrue(LIBERTY_ACCESSLOG + " not found", foundAccessLog);
        if (!checkGcSpecialCase()) {
            assertTrue(LIBERTY_GC + " not found", foundFFDC);
        }
    }

    @Test
    public void testLogstashForMessageEvent() throws Exception {
        testName = "testLogstashForMessageEvent";
        server.setMarkToEndOfLog();
        setConfig("server_logs_msg.xml");
        createMessageEvent(testName);
        assertNotNull("Cannot find TRAS0218I from messages.log", server.waitForStringInLogUsingMark("TRAS0218I", 10000));

        boolean found = false;
        int timeout = 0;
        try {
            while (!(found = lookForStringInLogstashOutput(LIBERTY_MESSAGE, testName)) && timeout < 120000) {
                timeout += 1000;
                Thread.sleep(1000);
            }
            Log.info(c, testName, "------> found message event types : " + found);
        } catch (Exception e) {
            Log.info(c, testName, "------>Exception occured while reading logstash output file : \n" + e.getMessage());
        }
        assertTrue("Did not find message log events..", found);
    }

    @Test
    public void testLogstashForAccessEvent() throws Exception {
        testName = "testLogstashForAccessEvent";
        server.setMarkToEndOfLog();
        setConfig("server_logs_access.xml");
        createAccessLogEvent(testName);
        assertNotNull("Cannot find TRAS0218I from messages.log", server.waitForStringInLogUsingMark("TRAS0218I", 10000));

        boolean found = false;
        found = logstash.waitForStringInLogUsingMark(testName, 10000) != null;
        assertTrue("Did not find access log events..", found);
    }

    @Test
    public void testLogstashForGCEvent() throws Exception {
        testName = "testLogstashForGCEvent";
        server.setMarkToEndOfLog();
        setConfig("server_logs_gc.xml");

        // Do some work and hopefully some GC events will be created
        for (int i = 1; i <= 10; i++) {
            createMessageEvent(testName + " " + i);
        }

        boolean found = true;
        try {
            if (!checkGcSpecialCase()) {
                int timeout = 0;
                while (!(found = lookForStringInLogstashOutput(LIBERTY_GC, null)) && timeout < 120000) {
                    timeout += 1000;
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            Log.info(c, testName, "------>Exception occured while reading logstash output file : \n" + e.getMessage());
        }
        assertTrue("Did not find gc log events..", found);
    }

    @Test
    @AllowedFFDC({ "java.lang.ArithmeticException", "java.lang.ArrayIndexOutOfBoundsException" })
    public void testLogstashForFFDCEvent() throws Exception {
        testName = "testLogstashForFFDCEvent";

        server.setMarkToEndOfLog();
        setConfig("server_logs_ffdc.xml");
        Log.info(c, testName, "------> starting ffdc2(ArithmeticException), "
                              + "ffdc3(ArrayIndexOutOfBoundsException)");

        List<String> exceptions = new ArrayList<String>();
        createFFDCEvent(2);
        Log.info(c, testName, "------> finished ffdc2(ArithmeticException)");
        exceptions.add("ArithmeticException");
        createFFDCEvent(3);
        Log.info(c, testName, "------> finished ffdc3(ArrayIndexOutOfBoundsException)");
        exceptions.add("ArrayIndexOutOfBoundsException");
        assertNotNull("Cannot find TRAS0218I from messages.log", server.waitForStringInLogUsingMark("TRAS0218I", 10000));

        boolean found = false;
        try {
            int timeout;
            for (String exception : exceptions) {
                timeout = 0;
                while (!(found = lookForStringInLogstashOutput(LIBERTY_FFDC, exception)) && timeout < 120000) {
                    Thread.sleep(1000);
                    timeout += 1000;
                }
                Log.info(c, testName, "------> " + exception + " : " + found);
                if (!found) {
                    break;
                }
            }
            Log.info(c, testName, "------> found ffdc event types : " + found);
        } catch (Exception e) {
            Log.info(c, testName, "------>Exception occured while reading logstash output file : \n" + e.getMessage());
            found = false;
        }
        assertTrue("Did not find some or all ffdc log events..", found);
    }

    @Test
    public void testLogstashForTraceEvent() throws Exception {
        testName = "testLogstashForTraceEvent";
        server.setMarkToEndOfLog();
        logstash.setMarkToEndOfLog();
        setConfig("server_logs_trace.xml");
        createTraceEvent(testName);

        boolean found = false;
        try {
            found = logstash.waitForStringInLogUsingMark(LIBERTY_TRACE, 10000) != null;
            Log.info(c, testName, "------> found trace event types : " + found);
        } catch (Exception e) {
            Log.info(c, testName, "------>Exception occured while reading logstash output file : \n" + e.getMessage());
        }
        assertTrue("Did not find trace log events..", found);
    }

    @Test
    public void testLogstashForAuditEvent() throws Exception {
        testName = "testLogstashForAuditEvent";
        server.setMarkToEndOfLog();
        logstash.setMarkToEndOfLog();
        setConfig("server_logs_audit.xml");
        createTraceEvent(testName);

        boolean found = false;
        try {
            found = logstash.waitForStringInLogUsingMark(LIBERTY_AUDIT, 10000) != null;
            Log.info(c, testName, "------> found audit event types : " + found);
        } catch (Exception e) {
            Log.info(c, testName, "------>Exception occured while reading logstash output file : \n" + e.getMessage());
        }
        assertTrue("Did not find audit log events..", found);
    }

    @Test
    public void testLogstashEntryExitEvents() throws Exception {
        testName = "testLogstashEntryExitEvents";
        server.setMarkToEndOfLog();
        setConfig("server_logs_trace.xml");
        logstash.setMarkToEndOfLog();
        createTraceEvent(testName);

        boolean entry = (logstash.waitForStringInLogUsingMark(ENTRY, 10000) != null);
        boolean exit = (logstash.waitForStringInLogUsingMark(EXIT, 10000) != null);
        server.setMarkToEndOfLog();
        if (entry && !exit) {
            assertTrue("Exit Events are missing..", exit);
        } else if (!entry && exit) {
            assertTrue("Entry Events are missing..", entry);
        }
        assertTrue("Entry and Exit Events are missing..", entry && exit);
    }

    @Test
    public void testLogstashDynamicDisableFeature() throws Exception {
        testName = "testLogstashDynamicDisableFeature";
        server.setMarkToEndOfLog();
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
        server.setMarkToEndOfLog();
        logstash.setMarkToEndOfLog();
        int lastLine = logstash.parseOutputFile().size();

        setConfig("server_logs_msg.xml");
        createMessageEvent(testName + " 1 - should appear in logstash output");
        assertNotNull("Did not find " + LIBERTY_MESSAGE + ":" + testName, logstash.waitForStringInLogUsingMark(testName));

        setConfig("server_logs_trace.xml");
        logstash.setMarkToEndOfLog();
        createMessageEvent(testName + " 2 - should NOT appear in logstash output");
        createTraceEvent(testName + " 3 - should appear in logstash output");

        logstash.waitForStringInLogUsingMark(testName + " 3");

        List<JSONObject> jObjs = logstash.parseOutputFile(lastLine + 1);

        boolean found1 = false;
        boolean found2 = false;
        boolean found3 = false;
        String msg = null;
        for (JSONObject jObj : jObjs) {
            msg = jObj.getString(KEY_MESSAGE);
            if (msg.contains(testName + " 1")) {
                found1 = true;
            } else if (msg.contains(testName + " 2")) {
                found2 = true;
            } else if (msg.contains(testName + " 3")) {
                found3 = true;
            }
        }
        assertTrue(testName + " 1 is not found", found1);
        assertFalse(testName + " 2 should not appear in logstash output", found2);
        assertTrue(testName + " 3 is not found", found3);
    }

    /*
     * This test determines whether source subsriptions are kept when they are present both
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

    @AfterClass
    public static void completeTest() throws Exception {
        if (!runTest) {
            return;
        }

        logstash.stop();

        String outputFileDirectory = logstash.getLogFilename();
        if (new File(outputFileDirectory).exists()) {
            Log.info(c, "completeTest", "copying logstash output file to server directory");
            try {
                String destPath = System.getProperty("user.dir") + "/output/servers/" + Logstash.OUTPUT_FILENAME;
                FileUtils.copyFile(new File(outputFileDirectory), new File(destPath));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        try {
            if (server.isStarted()) {
                Log.info(c, "completeTest", "---> Stopping server..");
                server.stopServer();
                resetServerSecurity();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void resetServerSecurity() {
        //Reset JVM security to its original value
        System.setProperty("Djava.security.properties", JVMSecurity);
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
        serverSecurityOverwrite();
        Log.info(c, "serverStart", "--->  Starting Server.. ");
        server.startServer();

        Log.info(c, "serverStart", "---> Wait for feature to start ");
        assertNotNull("Cannot find CWWKZ0001I from messages.log", server.waitForStringInLogUsingMark("CWWKZ0001I", 15000));

        Log.info(c, "serverStart", "---> Wait for application to start ");
        assertNotNull("Cannot find CWWKT0016I from messages.log", server.waitForStringInLogUsingMark("CWWKT0016I", 10000));
    }

    private static void serverSecurityOverwrite() throws Exception {
        //Logstash does not work with newest IBM JDK
        //Overwrite JVM security setting to enable logstash Collector as a temporary fix
        System.setProperty("Djava.security.properties", logstash.getJavaSecuritySettingFilePath());
        System.setProperty("Djvm.options.properties", server.getServerRoot() + "/jvm.options");
    }

    private BufferedReader getLogtsashOutputFile() {
        BufferedReader br = null;

        try {
            File f = new File(logstash.getLogFilename());
            if (f.exists()) {
                Log.info(c, testName, " ---> found : " + f.getName());
                br = new BufferedReader(new FileReader(f));
            }
        } catch (Exception e) {
            Log.info(c, testName, " ---> e : " + e.getMessage());
        }

        return br;
    }

    private boolean lookForStringInLogstashOutput(String eventType, String stringToSearch) throws Exception {

        BufferedReader br = null;

        br = getLogtsashOutputFile();

        assertNotNull("Logstash output file not generated..", br);

        String sCurrentLine;
        boolean found = false;

        while ((sCurrentLine = br.readLine()) != null && !found) {
            if (eventType.equalsIgnoreCase(LIBERTY_GC) || (stringToSearch != null && sCurrentLine.contains(stringToSearch))) {
                if (sCurrentLine.contains(eventType)) {
                    Log.info(c, testName, "------> msg type found.. \n" + sCurrentLine);
                    found = true;
                }
            } else if ((eventType.equalsIgnoreCase(EXIT) && sCurrentLine.contains("message\":\"Exit"))
                       || (eventType.equalsIgnoreCase(ENTRY) && sCurrentLine.contains("message\":\"Entry"))) {
                found = true;
                Log.info(c, testName, "------> " + eventType + " type found.. \n" + sCurrentLine);
            }
        }
        return found;
    }

    private boolean checkGcSpecialCase() {
        Log.info(c, testName, "Cannot find event type liberty_gc in logstash output file");
        /**
         * Check if if belongs to the special case where build machine does not have Health centre installed, which prevents gc event to be produced
         * by checking 1. whether the operating system is Mac or linux 2. whether the machine is running IBM JDK
         * if both checks pass, this is the case
         **/
        Log.info(c, testName, "os_name: " + os.toLowerCase() + "\t java_jdk: " + System.getProperty("java.vendor"));
        String JAVA_HOME = System.getenv("JAVA_HOME");
        Log.info(c, testName, "JAVA_HOME: " + JAVA_HOME);
        boolean healthCenterInstalled = false;
        if (JAVA_HOME == null) {
            Log.info(c, testName, " unable to find JAVA_HOME variable");
        } else if (JAVA_HOME.endsWith("jre")) {
            if (new File(JAVA_HOME + "/lib/ext/healthcenter.jar").exists()) {
                healthCenterInstalled = true;
                Log.info(c, testName, " jar file for health center under path " + JAVA_HOME + "/lib/ext/healthcenter.jar");
            }
            Log.info(c, testName, " jar file for health center under path " + JAVA_HOME + "/lib/ext/healthcenter.jar exist:"
                                  + new File(JAVA_HOME + "/lib/ext/healthcenter.jar").exists());
        } else if (JAVA_HOME.endsWith("bin")) {
            healthCenterInstalled = findHealthCenterDirecotry(JAVA_HOME.substring(0, JAVA_HOME.indexOf("bin") + 1));
            if (!healthCenterInstalled) {
                Log.info(c, testName, " unable to find heathcenter.jar, thus unable to produce gc events. Thus, this check will be by-passed");
            }
        } else {
            healthCenterInstalled = findHealthCenterDirecotry(JAVA_HOME);
            if (!healthCenterInstalled) {
                Log.info(c, testName, " unable to find heathcenter.jar, thus unable to produce gc events. Thus, this check will be by-passed");
            }
        }
        if (os.toLowerCase().contains("mac") || !System.getProperty("java.vendor").toLowerCase().contains("ibm")
            || System.getProperty("java.vendor.url").toLowerCase().contains("sun") || !healthCenterInstalled) {
            return true;
        }
        return false;
    }

    private boolean findHealthCenterDirecotry(String directoryPath) {
        boolean jarFileExist = false;
        File[] files = new File(directoryPath).listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                jarFileExist = findHealthCenterDirecotry(file.getAbsolutePath());
                if (jarFileExist == true) {
                    return true;
                }
            } else {
                if (file.getAbsolutePath().contains("healthcenter.jar")) {
                    Log.info(c, testName, " healthcetner.jar is found under path " + file.getAbsolutePath());
                    return true;
                }
            }
        }
        return jarFileExist;
    }

    /** {@inheritDoc} */
    @Override
    protected LibertyServer getServer() {
        return server;
    }

}
