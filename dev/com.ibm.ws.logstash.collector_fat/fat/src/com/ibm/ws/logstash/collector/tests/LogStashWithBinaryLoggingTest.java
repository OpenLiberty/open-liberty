/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
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

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@MinimumJavaLevel(javaLevel = 8)
@MaximumJavaLevel(javaLevel = 8)
public class LogStashWithBinaryLoggingTest extends LogstashCollectorTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("LibertyHPELServer");
    protected static Machine machine = null;
    static boolean msgType = false;
    static boolean ffdcType = false;
    static boolean gcType = false;
    static boolean traceType = false;
    static boolean accessType = false;
    static int mark = 0;
    static boolean newMsgsFound = false;
    private static final String TRACE_LOG = "logs/trace.log";
    private static String JVMSecurity = System.getProperty("Djava.security.properties");

    static String record = "";

    private String testName = "";
    private static Class<?> c = LogsStashSSLTest.class;
    public static String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/";

    protected static boolean runTest;

    private static Logstash logstash = new Logstash(server.getMachine());

    @BeforeClass
    public static void setUp() throws Exception {

        runTest = logstash.isSupportedPlatform();
        Log.info(c, "setUp", "runTest = " + runTest);

        if (!runTest) {
            return;
        }

        // Change the logstash config file so that HPEL tests create their own output file.
        Logstash.CONFIG_FILENAME = "../logstash_hpel.conf";
        Logstash.OUTPUT_FILENAME = "logstash_hpeloutput.txt";

        logstash.start();

        Log.info(c, "setUp", "---> Setting default logstash configuration.");

        if (server.isStarted()) {
            Log.info(c, "setUp", "---> Stopping server..");
            server.stopServer();
        }
        String extendedPath = "usr/servers/LogstashServer/jvm.options";
        if (server.getServerRoot().contains(server.getInstallRoot())) {
            extendedPath = server.getServerRoot().replaceAll(server.getInstallRoot(), "").substring(1);
        }
        server.copyFileToLibertyInstallRoot(extendedPath, "jvm.options");
        server.copyFileToLibertyInstallRoot(extendedPath.replace("jvm.options", "java.security"), "java.security");
        ShrinkHelper.defaultDropinApp(server, "LogstashApp", "com.ibm.logs");

        serverStart();

        // Wait for the message ID in logstash_output.txt
        assertNotNull("Cannot find CWWKF0011I from logstash ouput file", logstash.waitForStringInLogUsingMark("CWWKF0011I", 10000));
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
    @AllowedFFDC({ "java.lang.NullPointerException" })
    public void testBinaryLoggingWithLogstash() throws Exception {
        testName = "testBinaryLoggingWithLogstash";

        try {
            // check if the server is started
            if (!server.isStarted()) {
                try {
                    Log.info(c, testName, "---> Starting Liberty server.");
                    serverStart();
                } catch (Exception e) {
                    Log.info(c, testName, "------>Exception occured while starting server: \n" + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.info(c, testName, "------>Exception occured while starting server again: \n" + e.getMessage());
            fail("Error starting server " + e.getMessage());
        }

        logstash.setMarkToEndOfLog();

        createMessageEvent();

        for (int count = 1; count < 6; count++) {
            createAccessLogEvent();
        }

        createFFDCEvent(1);

        // get number of message events
        int msg = getNumberOfMsgEvents();
        Log.info(c, testName, "Number of message events " + msg);
        assertTrue("Message events not found in Logstash output.", (msg > 0));
    }

    @Test
    public void testLogstashForTraceEvent() throws Exception {
        testName = "testLogstashForTraceEvent";

        try {
            // check if the server is started
            if (!server.isStarted()) {
                try {
                    Log.info(c, testName, "---> Starting Liberty server.");
                    serverStart();
                } catch (Exception e) {
                    Log.info(c, testName, "------>Exception occured while starting server: \n" + e.getMessage());
                }
            }
            logstash.setMarkToEndOfLog();
            //Swap in our new server.xml which contains tracing for our TraceServlet
            setConfig("server_trace_servlet.xml");
            //Run the servlet to produce the traces we need
            createTraceEvent();
            createTraceEvent();
            //Check for the trace in the logstash output file (currently logstash_hpel_output.txt)
            //Give generous time (60s) because logstash may experience delays
            String jultrace = logstash.waitForStringInLogUsingMark("TEST JUL TRACE", 10000);
            assertTrue("Did not find JUL trace events with binary logging", jultrace != null);
            //TODO: add TraceComponent specific test
            //String tctrace = findStringInLogstashOutput("TEST TC TRACE");
            //assertTrue("Did not find TC trace events with binary logging", tctrace != null);
        } catch (Exception e) {
            Log.info(c, testName, "------>Exception occured while reading logstash output file for traces : \n" + e.getMessage());
        }
    }

    @Override
    protected void setConfig(String conf) throws Exception {
        server.setServerConfigurationFile(conf);
        String line = logstash.waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I", 60000);
        assertNotNull("Cannot find CWWKG0017I or CWWKG0018I from " + Logstash.OUTPUT_FILENAME, line);
    }

    @After
    public void tearDown() {
        testName = "tearDown";
    }

    //TODO add other methods to validate posted trace data

    @AfterClass
    public static void completeTest() throws Exception {
        if (!runTest) {
            return;
        }

        Thread.sleep(5000);

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

    private static void serverStart() throws Exception {
        serverSecurityOverwrite();
        Log.info(c, "serverStart", "--->  Starting Server.. ");
        server.startServer();

        Log.info(c, "serverStart", "---> Wait for feature to start ");
        logstash.waitForStringInLogUsingMark("CWWKZ0001I", 15000);

        Log.info(c, "serverStart", "---> Wait for application to start ");
        logstash.waitForStringInLogUsingMark("CWWKT0016I", 10000);

        Log.info(c, "serverStart", "---> Wait for logstash warning ");
        logstash.waitForStringInLogUsingMark("CWWKG0018I", 20000);

    }

    private static void serverSecurityOverwrite() throws Exception {
        //Logstash does not work with newest IBM JDK
        //Overwrite JVM security setting to enable logstash Collector as a temporary fix
        System.setProperty("Djava.security.properties", logstash.getJavaSecuritySettingFilePath());
        System.setProperty("Djvm.options.properties", server.getServerRoot() + "/jvm.options");
    }

    public boolean lookInTrace(String type) throws Exception {
        boolean found = false;

        Thread.sleep(10000);
        List<String> lines = server.findStringsInFileInLibertyServerRoot(type, TRACE_LOG);
        for (String line : lines) {
            Log.info(c, testName, "---> found : " + line);
            found = true;
        }
        return found;
    }

    public int getNumberOfMsgEvents() throws Exception {
        BufferedReader br = getLogtsashOutputFile();

        assertNotNull("Logstash output file not generated..", br);
        int msgCount = 0;

        String sCurrentLine;

        while ((sCurrentLine = br.readLine()) != null) {
            if (sCurrentLine.contains("liberty_message")) {
                Log.info(c, testName, "------> msg ..");
                Log.info(c, testName, sCurrentLine);
                msgCount++;
            }
        }

        return msgCount;

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

    /** {@inheritDoc} */
    @Override
    protected LibertyServer getServer() {
        return server;
    }

}
