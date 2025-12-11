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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
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

    private String testName = "";
    private static Class<?> c = ThrottleMaxEventsTest.class;

    protected static boolean runTest = true;

    @BeforeClass
    public static void setUp() throws Exception {
        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        Log.info(c, "setUp", "runTest = " + runTest);

        if (!runTest) {
            return;
        }

        clearContainerOutput();
        String host = logstashContainer.getHost();
        String port = String.valueOf(logstashContainer.getMappedPort(5043));
        Log.info(c, "setUp", "Logstash container: host=" + host + "  port=" + port);
        server.addEnvVar("LOGSTASH_HOST", host);
        server.addEnvVar("LOGSTASH_PORT", port);

        Log.info(c, "setUp", "---> Setting default logstash configuration.");
        Log.info(c, "setUp", "installed liberty root is at: " + server.getInstallRoot());
        Log.info(c, "setUp", "server root is at: " + server.getServerRoot());

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
        clearContainerOutput();
        setConfig("server_maxEvents_ten.xml");
        clearContainerOutput();

        long startTime = System.currentTimeMillis();
        for (int t = 0; t < 100; t++) {
            //generate 100 message events 100 trace events and 200 access log events
            createMessageEvent(Integer.toString(t));
            createTraceEvent(Integer.toString(t));
        }
        waitForContainerOutputSize(400);
        boolean throttled = waitForEventsToProcess(100, 100, 200, startTime, 120000);

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

        try {
            if (server.isStarted()) {
                Log.info(c, "competeTest", "---> Stopping server..");
                server.stopServer("TRAS4301W");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private boolean waitForEventsToProcess(int numMsg, int numTrace, int numAccessLog, long startTime, long timeout) throws JSONException {
        int accessCount = 0;
        int traceCount = 0;
        int messageCount = 0;
        long traceEndTime = 0;
        long messageEndTime = 0;
        long accesslogEndTime = 0;
        while ((accessCount + traceCount + messageCount < numMsg + numTrace + numAccessLog) && (timeout > 0)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            timeout -= 1000;

            List<JSONObject> jobjs = parseJsonInContainerOutput();
            messageCount = 0;
            traceCount = 0;
            accessCount = 0;

            for (JSONObject jobj : jobjs) {
                String type = jobj.getString(KEY_TYPE);
                String message = null;
                if (!type.equals(LIBERTY_ACCESSLOG)) {
                    message = jobj.getString(KEY_MESSAGE);
                }
                if (type.equals(LIBERTY_TRACE) && message.contains("TEST JUL TRACE")) {
                    traceCount++;
                    traceEndTime = System.currentTimeMillis();
                } else if (type.equals(LIBERTY_MESSAGE) && message.contains("Test Logstash Message")) {
                    messageCount++;
                    messageEndTime = System.currentTimeMillis();
                } else if (type.equals(LIBERTY_ACCESSLOG)) {
                    accessCount++;
                    accesslogEndTime = System.currentTimeMillis();
                }
            }
        }

        assertEquals("Number of liberty_message does not match", numMsg, messageCount);
        assertEquals("Number of liberty_trace does not match", numTrace, traceCount);
        assertEquals("Number of liberty_accesslog does not match", numAccessLog, accessCount);
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

    /** {@inheritDoc} */
    @Override
    protected LibertyServer getServer() {
        return server;
    }

}
