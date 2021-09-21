/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.microprofile.faulttolerance_fat.suite.FATSuite;
import com.ibm.ws.microprofile.faulttolerance.fat.repeat.RepeatFaultTolerance;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.AnnotationsDisabledServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 * Test the server when the non-Fallback annotations are disabled by setting MP_Fault_Tolerance_NonFallback_Enabled = false
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class CDIAnnotationsDisabledTest extends FATServletClient {

    private final static String SERVER_NAME = "CDIFaultTolerance";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = "CDIFaultTolerance", servlet = AnnotationsDisabledServlet.class)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        FATSuite.exportCDIFaultToleranceAppToServer(server);

        Map<String, String> envVars = new HashMap<>();
        envVars.put("MP_Fault_Tolerance_NonFallback_Enabled", "false");
        server.setAdditionalSystemProperties(envVars);
        server.startServer();
    }

    @Test
    public void testBulkheadSynchronous() throws Exception {

        final long TEST_TWEAK_TIME_UNIT = 100;
        final long TIMEOUT = 5000;
        final long FUTURE_THRESHOLD = 6000;

        // Make an initial request so that everything is initialized
        HttpUtils.getHttpResponseAsString(server, "/CDIFaultTolerance/multi-request-bulkhead");

        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Connect C has a pool size of 2
        // Fire three requests in parallel
        Future<String> future1 = executor.submit(() -> HttpUtils.getHttpResponseAsString(server, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(TEST_TWEAK_TIME_UNIT);

        Future<String> future2 = executor.submit(() -> HttpUtils.getHttpResponseAsString(server, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(TEST_TWEAK_TIME_UNIT);

        Future<String> future3 = executor.submit(() -> HttpUtils.getHttpResponseAsString(server, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(TEST_TWEAK_TIME_UNIT);

        executor.shutdown();

        // First two tasks should succeed
        assertThat("Task One", future1.get(TIMEOUT + FUTURE_THRESHOLD, TimeUnit.MILLISECONDS), containsString("Success"));
        assertThat("Task Two", future2.get(FUTURE_THRESHOLD, TimeUnit.MILLISECONDS), containsString("Success"));

        // Third task should fail with a Bulkhead exception
        assertThat("Task Three", future3.get(FUTURE_THRESHOLD, TimeUnit.MILLISECONDS), containsString("Success"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        /*
         * Ignore following exception as those are expected:
         * CWWKC1101E: The task com.ibm.ws.microprofile.faulttolerance.cdi.FutureTimeoutMonitor@3f76c259, which was submitted to executor service
         * managedScheduledExecutorService[DefaultManagedScheduledExecutorService], failed with the following error:
         * org.eclipse.microprofile.faulttolerance.exceptions.FTTimeoutException: java.util.concurrent.TimeoutException
         */
        server.stopServer("CWWKC1101E");
    }

}
