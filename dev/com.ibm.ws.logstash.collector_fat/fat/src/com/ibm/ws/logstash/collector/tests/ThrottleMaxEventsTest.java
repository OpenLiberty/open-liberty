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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
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
public class ThrottleMaxEventsTest extends LogstashCollectorTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("LogstashServer");
    protected static Machine machine = null;
    static boolean msgType = false;
    static boolean ffdcType = false;
    static boolean gcType = false;
    static boolean traceType = false;
    static boolean accessType = false;
    static int mark = 0;
    static boolean newMsgsFound = false;
    private static boolean connected = false;

    private String testName = "";
    private static Class<?> c = ThrottleMaxEventsTest.class;
    private static Logstash logstash = new Logstash(server.getMachine());
    private static String JVMSecurity = System.getProperty("Djava.security.properties");

    protected static boolean runTest;

    @BeforeClass
    public static void setUp() throws Exception {
        runTest = logstash.isSupportedPlatform();
        Log.info(c, "setUp", "runTest = " + runTest);

        if (!runTest) {
            return;
        }

        // Change the logstash config file so that the Tag tests create their own output file.
        Logstash.CONFIG_FILENAME = "../logstash_maxEvents.conf";
        Logstash.OUTPUT_FILENAME = "logstash_maxEvents.txt";

        logstash.start();

        Log.info(c, "setUp", "---> Setting default logstash configuration.");
        /**
         * All tests within this file will use the same server configuration file
         */
        Log.info(c, "setUp", "installed liberty root is at: " + server.getInstallRoot());

        Log.info(c, "setUp", "server root is at: " + server.getServerRoot());

        String extendedPath = "usr/servers/LogstashServer/jvm.options";
        if (server.getServerRoot().contains(server.getInstallRoot())) {
            extendedPath = server.getServerRoot().replaceAll(server.getInstallRoot(), "").substring(1);
        }
        server.copyFileToLibertyInstallRoot(extendedPath, "jvm.options");
        server.copyFileToLibertyInstallRoot(extendedPath.replace("jvm.options", "java.security"), "java.security");

        ShrinkHelper.defaultDropinApp(server, "LogstashApp", "com.ibm.logs");
        serverStart();

        Log.info(c, "setUp", "server root is at: " + server.getServerRoot());

    }

    @Before
    public void setUpTest() throws Exception {
        Log.info(c, testName, "runTest = " + runTest);
        Assume.assumeTrue(runTest); // runTest must be true to run test

        testName = "setUpTest";
        if (!server.isStarted()) {
            serverStart();
        }
    }

    @AllowedFFDC({ "java.lang.NullPointerException", "java.lang.ArithmeticException", "java.lang.ArrayIndexOutOfBoundsException" })
    @Test
    public void testSetMaxEvents() throws Exception {
        testName = "testSetMaxEvents";
        Log.info(c, testName, "Entry");

        server.setMarkToEndOfLog();
        boolean feature = this.isConnected();

        Log.info(c, testName, "---> Did LogstashCollector feature START ? : " + feature);
        assertTrue("logstashCollector-1.0 did not show as started..", feature);
        logstash.setMarkToEndOfLog();
        Log.info(c, testName, "First offset for logstash output is " + logstash.getMarkOffset());

        setConfig("server_maxEvents_ten.xml");

        long startTime = System.currentTimeMillis();
        for (int t = 0; t < 100; t++) {
            //generate 100 message events 100 trace events and 200 access events
            createMessageEvent(Integer.toString(t));
            createTraceEvent(Integer.toString(t));
        }
        //wait for the message events to process total of 400 events
        boolean throttled = waitForEventsToProcess(100, 100, 200, startTime, 120000);

        logstash.setMarkToEndOfLog();
        int lastLine = logstash.parseOutputFile().size();
        Log.info(c, testName, "lastLine=" + lastLine);
        Log.info(c, testName, "Throttling test=" + throttled);
        Log.info(c, testName, "Exit");
        assertTrue("Events were processed faster than 10 per second", throttled);
    }

    @After
    public void tearDown() {
        testName = "tearDown";
    }

    @AfterClass
    public static void completeTest() throws Exception {
        if (!runTest) {
            return;
        }

        logstash.stop();
        String outputFileDirectory = logstash.getLogFilename();
        if (new File(outputFileDirectory).exists()) {
            Log.info(c, "competeTest", "copying logstash output file to server directory");
            try {
                String destPath = System.getProperty("user.dir") + "/output/servers/" + Logstash.OUTPUT_FILENAME;
                FileUtils.copyFile(new File(outputFileDirectory), new File(destPath));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        try {
            if (server.isStarted()) {
                Log.info(c, "competeTest", "---> Stopping server..");
                server.stopServer("TRAS4301W");
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

    private boolean waitForEventsToProcess(int numMsg, int numTrace, int numAccessLog, long startTime, long timeout) throws Exception {
        BufferedReader br = getLogtsashOutputFile();
        assertNotNull("Logstash output file" + logstash.getLogFilename() + "not generated..", br);
        String sCurrentLine;
        int count = 0;
        int accessCount = 0;
        int traceCount = 0;
        int messageCount = 0;
        int numEvents = numMsg + numTrace + numAccessLog;
        long traceEndTime = 0;
        long messageEndTime = 0;
        long accesslogEndTime = 0;
        long stopTime = System.currentTimeMillis() + timeout;
        while ((count != numEvents) && (System.currentTimeMillis() < stopTime)) {
            sCurrentLine = br.readLine();
            if (sCurrentLine == null) {
                //wait for logstash output
                Thread.sleep(1000);
            } else if (sCurrentLine.contains("TEST JUL TRACE") && sCurrentLine.contains(LIBERTY_TRACE)) {
                count++;
                traceCount++;
                //test should generate 100 trace events, get time of last event sent
                if (traceCount == numTrace) {
                    traceEndTime = System.currentTimeMillis();
                }

            } else if (sCurrentLine.contains("Test Logstash Message") && sCurrentLine.contains(LIBERTY_MESSAGE)) {
                count++;
                messageCount++;
                //test should generate 100 message events, get time of last event sent
                if (messageCount == numMsg) {
                    messageEndTime = System.currentTimeMillis();
                }

            } else if (sCurrentLine.contains(LIBERTY_ACCESSLOG)) {
                count++;
                accessCount++;
                //test should generate 200 access events, get time of last event sent
                if (accessCount == numAccessLog) {
                    accesslogEndTime = System.currentTimeMillis();
                }
            }
        }
        if (System.currentTimeMillis() >= stopTime) {
            fail("Timeout: Waited " + timeout + " ms and only " + count + " out of " + numEvents + " were found");
        }
        //check if 100 events generated in at least 9 seconds
        boolean isTracePassed = checkTimeTaken(startTime, traceEndTime, 9000, 60000);
        //check if 100 events generated in at least 9 seconds
        boolean isMessagePassed = checkTimeTaken(startTime, messageEndTime, 9000, 60000);
        //check if 200 events generated in at least 19 seconds
        boolean isAccesslogPassed = checkTimeTaken(startTime, accesslogEndTime, 19000, 60000);
        return isTracePassed && isMessagePassed && isAccesslogPassed;
    }

    private boolean checkTimeTaken(long startTime, long endTime, long minTimeTaken, long maxTimeTaken) {
        long timeTaken = endTime - startTime;
        Log.info(c, testName, "timeTaken=" + timeTaken);
        return timeTaken >= minTimeTaken && timeTaken <= maxTimeTaken;
    }

    public BufferedReader getLogtsashOutputFile() {
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

    /** {@inheritDoc} */
    @Override
    protected LibertyServer getServer() {
        return server;
    }

}
