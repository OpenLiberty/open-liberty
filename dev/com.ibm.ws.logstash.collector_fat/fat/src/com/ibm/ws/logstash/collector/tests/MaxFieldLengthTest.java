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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

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
public class MaxFieldLengthTest extends LogstashCollectorTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("LogstashServer");
    private static boolean connected = false;

    private String testName = "";
    private static Class<?> c = MaxFieldLengthTest.class;

    private static final String DOTS = "...";

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

        String zip = System.getProperty("user.dir");
        Log.info(c, "setUp", "Current directory is " + zip);
        Log.info(c, "setUp", "---> Setting default logstash configuration.");
        Log.info(c, "setUp", "installed liberty root is at: " + server.getInstallRoot());
        Log.info(c, "setUp", "server root is at: " + server.getServerRoot());

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
        String testId = "TMFS";
        String filler = " - make this message longer and longer.";
        boolean feature = this.isConnected();

        Log.info(c, testName, "---> Did LogstashCollector feature START ? : " + feature);
        assertTrue("logstashCollector-1.0 did not show as started..", feature);

        // Set maxFieldLength to 30 characters
        int max = 30;
        setConfig("server_maxField_thirty.xml");

        // Need a second delay for the old log entries appear in Logstash container
        Thread.sleep(1000);
        clearContainerOutput();

        // maxFieldLength only limit the message field of message and trace, stackTrace from FFDC
        createMessageEvent(testId + "1" + filler);
        createTraceEvent(testId + "2" + filler);
        assertNotNull(waitForStringInContainerOutput(testId + "1"));
        assertNotNull(waitForStringInContainerOutput(testId + "2"));

        int counter = 0;
        List<JSONObject> jobjs = parseJsonInContainerOutput();
        for (JSONObject jobj : jobjs) {
            String type = jobj.getString(KEY_TYPE);
            if (type.equals(LIBERTY_MESSAGE)) {
                String message = jobj.getString(KEY_MESSAGE);
                if (message.endsWith(DOTS)) {
                    message = message.substring(0, message.length() - 3);
                }
                if (message.contains(testId)) {
                    counter++;
                    Log.info(c, testName, message);
                    assertFalse("message field exceeded " + max + " characters", message.length() > max);
                }
            } else if (type.equals(LIBERTY_TRACE)) {
                String message = jobj.getString(KEY_MESSAGE);
                if (message.endsWith(DOTS)) {
                    message = message.substring(0, message.length() - 3);
                }
                if (message.contains(testId)) {
                    counter++;
                    Log.info(c, testName, message);
                    assertFalse("message field exceeded " + max + " characters", message.length() > max);
                }
            }
        }
        assertEquals("Did not find all events", 2, counter);
    }

    @AllowedFFDC({ "java.lang.NullPointerException", "java.lang.ArithmeticException", "java.lang.ArrayIndexOutOfBoundsException" })
    @Test
    public void testNoLimitField() throws Exception {
        testName = "testNoLimitField";
        String testId = "TNLF";
        // No limit on message/stackTrace field
        setConfig("server_maxField_nolimit.xml");

        // Need a second delay for the old log entries appear in Logstash container
        Thread.sleep(1000);
        clearContainerOutput();

        // Create some more log records to be verified below
        createMessageEvent(testId + "1");
        createTraceEvent(testId + "2");

        assertNotNull(waitForStringInContainerOutput(testId + "1"));
        assertNotNull(waitForStringInContainerOutput(testId + "2"));

        List<JSONObject> jobjs = parseJsonInContainerOutput();
        int counter = 0;
        for (JSONObject jobj : jobjs) {
            String type = jobj.getString(KEY_TYPE);
            if (type.equals(LIBERTY_MESSAGE) || type.equals(LIBERTY_TRACE)) {
                String message = jobj.getString(KEY_MESSAGE);
                if (message.contains(testId)) {
                    counter++;
                    assertFalse("message field contains ... ", message.endsWith(DOTS));
                }
            }
        }
        assertEquals("Did not find all events", 2, counter);
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

        try {
            if (server.isStarted()) {
                Log.info(c, "competeTest", "---> Stopping server..");
                server.stopServer("TRAS4301W");
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

    /** {@inheritDoc} */
    @Override
    protected LibertyServer getServer() {
        return server;
    }

}
