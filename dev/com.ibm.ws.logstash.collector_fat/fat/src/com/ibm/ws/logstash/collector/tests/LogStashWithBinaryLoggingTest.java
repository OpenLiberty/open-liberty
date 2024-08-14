/*******************************************************************************
 * Copyright (c) 2016, 2024 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

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

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class LogStashWithBinaryLoggingTest extends LogstashCollectorTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("LibertyHPELServer");
    private String testName;
    private static Class<?> c = LogstashSSLTest.class;
    public static String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/";

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
    @AllowedFFDC({ "java.lang.NullPointerException" })
    public void testBinaryLoggingWithLogstash() throws Exception {
        testName = "testBinaryLoggingWithLogstash";
        setConfig("server_hpel_accesslog.xml");
        clearContainerOutput();

        createMessageEvent();
        createAccessLogEvent();
        createFFDCEvent(1);

        assertNotNull("Cannot find " + LIBERTY_MESSAGE, waitForStringInContainerOutput(LIBERTY_MESSAGE));
        assertNotNull("Cannot find " + LIBERTY_ACCESSLOG, waitForStringInContainerOutput(LIBERTY_ACCESSLOG));
    }

    @Test
    public void testLogstashForTraceEvent() throws Exception {
        testName = "testLogstashForTraceEvent";
        //Swap in our new server.xml which contains tracing for our TraceServlet
        setConfig("server_hpel_trace.xml");
        clearContainerOutput();

        //Run the servlet to produce the traces we need
        for (int i = 0; i < 10; i++) {
            createTraceEvent();
        }

        //Check for the trace in the logstash output file (currently logstash_hpel_output.txt)
        //Give generous time (60s) because logstash may experience delays
        assertNotNull(waitForStringInContainerOutput("TEST JUL TRACE"));
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

        try {
            if (server.isStarted()) {
                Log.info(c, "completeTest", "---> Stopping server..");
                server.stopServer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void serverStart() throws Exception {
        Log.info(c, "serverStart", "--->  Starting Server.. ");
        clearContainerOutput();
        server.startServer();
        // Wait for CWWKT0016I in Logstash container output
        waitForStringInContainerOutput("CWWKT0016I");
    }

    /*
     * Override the setConfig is required as binary logging does not have messages.log
     */
    @Override
    protected void setConfig(String conf) throws Exception {
        clearContainerOutput();
        server.setServerConfigurationFile(conf);
        assertNotNull(waitForStringInContainerOutput("CWWKG0017I|CWWKG0018I"));
    }

    /** {@inheritDoc} */
    @Override
    protected LibertyServer getServer() {
        return server;
    }

}
