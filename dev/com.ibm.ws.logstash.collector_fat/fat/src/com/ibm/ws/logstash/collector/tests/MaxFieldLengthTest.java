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

import java.io.File;
import java.io.IOException;
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
public class MaxFieldLengthTest extends LogstashCollectorTest {
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
    private static Class<?> c = MaxFieldLengthTest.class;
    private static Logstash logstash = new Logstash(server.getMachine());
    private static String JVMSecurity = System.getProperty("Djava.security.properties");

    private static final String DOTS = "...";

    protected static boolean runTest;

    @BeforeClass
    public static void setUp() throws Exception {
        runTest = logstash.isSupportedPlatform();
        Log.info(c, "setUp", "runTest = " + runTest);

        if (!runTest) {
            return;
        }

        String zip = System.getProperty("user.dir");
        Log.info(c, "setUp", "Current directory is " + zip);

        // Change the logstash config file so that the Tag tests create their own output file.
        Logstash.CONFIG_FILENAME = "../logstash_maxFieldLength.conf";
        Logstash.OUTPUT_FILENAME = "logstash_maxFieldoutput.txt";

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
    public void testMaxFieldSetLength() throws Exception {
        testName = "testMaxFieldSetLength";

        server.setMarkToEndOfLog();
        boolean feature = this.isConnected();

        Log.info(c, testName, "---> Did LogstashCollector feature START ? : " + feature);
        assertTrue("logstashCollector-1.0 did not show as started..", feature);

        logstash.setMarkToEndOfLog();
        Log.info(c, testName, "First offset for logstash output is " + logstash.getMarkOffset());

        // Set maxFieldLength to 30 characters
        int max = 30;
        setConfig("server_maxField_thirty.xml");

        logstash.setMarkToEndOfLog();
        int lastLine = logstash.parseOutputFile().size();
        Log.info(c, testName, "lastLine=" + lastLine);

        // maxFieldLength only limit the message field of message and trace, stackTrace from FFDC
        createMessageEvent("testMaxFieldSetLength");
        createTraceEvent();
        createFFDCEvent(1);
        logstash.waitForStringInLogUsingMark(NPE);

        // Check results
        List<JSONObject> jObjList = logstash.parseOutputFile(lastLine + 1);
        Log.info(c, testName, "jObjList.size()=" + jObjList.size());
        boolean foundMessage = false;
        boolean foundFFDC = false;
        boolean foundTrace = false;

        String type;
        String msg;
        for (JSONObject jObj : jObjList) {
            type = jObj.getString("type");
            if (type.equals(LIBERTY_MESSAGE)) {
                if (!foundMessage) {
                    msg = jObj.getString(KEY_MESSAGE);
                    if (msg.contains("Test Logstash Message testMaxF")) {
                        Log.info(c, testName, "found " + LIBERTY_MESSAGE + " message=" + msg);
                        if (msg.endsWith(DOTS)) {
                            msg = msg.substring(0, msg.length() - 3);
                        }
                        assertTrue(LIBERTY_MESSAGE + " exceeded " + max + " msg=" + msg, msg.length() <= max);
                        foundMessage = true;
                    }
                }
            } else if (type.equals(LIBERTY_TRACE)) {
                if (!foundTrace) {
                    msg = jObj.getString(KEY_MESSAGE);
                    if (msg.startsWith("com.ibm.ws.logstash.collector_")) {
                        Log.info(c, testName, "found " + LIBERTY_TRACE + " message=" + msg);
                        if (msg.endsWith(DOTS)) {
                            msg = msg.substring(0, msg.length() - 3);
                        }
                        assertTrue(LIBERTY_TRACE + " exceeded " + max + " msg=" + msg, msg.length() <= max);
                        foundTrace = true;
                    }
                }
            } else if (type.equals(LIBERTY_FFDC)) {
                if (!foundFFDC) {
                    msg = jObj.getString(KEY_STACKTRACE);
                    if (msg.startsWith("java.lang.NullPointerException")) {
                        Log.info(c, testName, "found " + LIBERTY_FFDC + " " + KEY_OBJECTDETAILS + "=" + msg);
                        if (msg.endsWith(DOTS)) {
                            msg = msg.substring(0, msg.length() - 3);
                        }
                        assertTrue(LIBERTY_FFDC + " exceeded " + max + " stackTrace=" + msg, msg.length() <= max);
                        foundFFDC = true;
                    }
                }
            } else if (type.equals(LIBERTY_ACCESSLOG)) {
                // ignore
            } else if (type.equals(LIBERTY_GC)) {
                // ignore
            } else {
                fail("Invalid event type found: " + type);
            }
            if (foundMessage && foundTrace && foundFFDC) {
                Log.info(c, testName, "All 3 event types found");
                return;
            }
        }
        assertTrue(LIBERTY_MESSAGE + " not found", foundMessage);
        assertTrue(LIBERTY_TRACE + " not found", foundTrace);
        assertTrue(LIBERTY_FFDC + " not found", foundFFDC);
    }

    @AllowedFFDC({ "java.lang.NullPointerException", "java.lang.ArithmeticException", "java.lang.ArrayIndexOutOfBoundsException" })
    @Test
    public void testInfiniteMaxField() throws Exception {
        testName = "testInfiniteMaxField";

        server.setMarkToEndOfLog();
        boolean feature = this.isConnected();
        Log.info(c, testName, "---> Did LogstashCollector feature START ? : " + feature);
        assertTrue("logstashCollector-1.0 did not show as started..", feature);

        logstash.setMarkToEndOfLog();
        Log.info(c, testName, "First offset for logstash output is " + logstash.getMarkOffset());

        // No limit on message/stackTrace field
        setConfig("server_maxField_infinite.xml");

        logstash.setMarkToEndOfLog();
        int lastLine = logstash.parseOutputFile().size();
        Log.info(c, testName, "lastLine=" + lastLine);

        // Create some more log records to be verified below
        createMessageEvent(testName);
        logstash.waitForStringInLogUsingMark(testName);
        createTraceEvent();
        createFFDCEvent(3);

        logstash.waitForStringInLogUsingMark(AIOB);

        // Check results
        List<JSONObject> jObjList = logstash.parseOutputFile(lastLine + 1);
        boolean foundMessage = false;
        boolean foundFFDC = false;
        boolean foundTrace = false;

        String type;
        String msg;
        for (JSONObject jObj : jObjList) {
            type = jObj.getString("type");
            if (type.equals(LIBERTY_MESSAGE)) {
                if (!foundMessage) {
                    msg = jObj.getString(KEY_MESSAGE);
                    if (msg.contains(testName)) {
                        Log.info(c, testName, "found " + LIBERTY_MESSAGE + " message=" + msg);
                        assertFalse(LIBERTY_MESSAGE + " contains ... msg=" + msg, msg.endsWith(DOTS));
                        foundMessage = true;
                    }
                }
            } else if (type.equals(LIBERTY_TRACE)) {
                if (!foundTrace) {
                    msg = jObj.getString(KEY_MESSAGE);
                    Log.info(c, testName, "found " + LIBERTY_TRACE + " message=" + msg);
                    assertFalse(LIBERTY_TRACE + " contains ... msg=" + msg, msg.endsWith(DOTS));
                    foundTrace = true;
                }
            } else if (type.equals(LIBERTY_FFDC)) {
                if (!foundFFDC) {
                    msg = jObj.getString(KEY_STACKTRACE);
                    Log.info(c, testName, "found " + LIBERTY_FFDC + " " + KEY_OBJECTDETAILS + "=" + msg);
                    assertFalse(LIBERTY_FFDC + " contains ... stackTrace=" + msg, msg.endsWith(DOTS));
                    foundFFDC = true;
                }
            } else if (type.equals(LIBERTY_ACCESSLOG)) {
                // ignore
            } else if (type.equals(LIBERTY_GC)) {
                // ignore
            } else {
                fail("Invalid event type found: " + type);
            }
            if (foundMessage && foundTrace && foundFFDC) {
                Log.info(c, testName, "All 3 event types found");
                return;
            }
        }
        assertTrue(LIBERTY_MESSAGE + " not found", foundMessage);
        assertTrue(LIBERTY_TRACE + " not found", foundTrace);
        assertTrue(LIBERTY_FFDC + " not found", foundFFDC);
    }

    @After
    public void tearDown() {
        testName = "tearDown";
    }

    //TODO add other methods to validate posted trace datait

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

    /** {@inheritDoc} */
    @Override
    protected LibertyServer getServer() {
        return server;
    }

}
