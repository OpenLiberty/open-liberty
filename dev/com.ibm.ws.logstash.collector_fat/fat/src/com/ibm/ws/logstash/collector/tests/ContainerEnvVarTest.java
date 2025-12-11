/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class ContainerEnvVarTest extends LogstashCollectorTest {

    @Server("ContainerEnvServer")
    public static LibertyServer server;

    private String testName = "";
    private static Class<?> c = ContainerEnvVarTest.class;

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

    @After
    public void tearDown() {
    }

    @AfterClass
    public static void completeTest() throws Exception {
        try {
            if (server.isStarted()) {
                Log.info(c, "competeTest", "---> Stopping server..");
                server.stopServer("TRAS4301W");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Test to verify CONTAINER_NAME and CONTAINER_HOST env vars are picked up */
    @Test
    public void containerEnvVarTest() throws Exception {

        boolean foundHostEnv = false;
        boolean foundServerEnv = false;

        String stringHost = waitForStringInContainerOutput("TEST_HOST_NAME");
        String stringServer = waitForStringInContainerOutput("TEST_SERVER_NAME");

        Log.info(c, "containerEnvVarTest", "stringHost: " + stringHost);
        Log.info(c, "containerEnvVarTest", "stringServer: " + stringServer);

        if (stringHost != null) {
            foundHostEnv = true;
        }
        if (stringServer != null) {
            foundServerEnv = true;
        }

        assertTrue("Could not find env var CONTAINER_HOST", foundHostEnv);
        assertTrue("Could not find env var CONTAINER_NAME", foundServerEnv);
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

    @Override
    protected LibertyServer getServer() {
        return server;
    }

}
