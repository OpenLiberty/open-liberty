/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import com.ibm.websphere.microprofile.faulttolerance_fat.suite.RepeatFaultTolerance;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.AnnotationsDisabledServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 * Test the server when the non-Fallback annotations are disabled by setting MP_Fault_Tolerance_NonFallback_Enabled = false
 */
@RunWith(FATRunner.class)
public class CDIAnnotationsDisabledTest extends FATServletClient {

    private final static String SERVER_NAME = "CDIFaultTolerance";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = "CDIFaultTolerance", servlet = AnnotationsDisabledServlet.class)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("MP_Fault_Tolerance_NonFallback_Enabled", "false");
        server.setAdditionalSystemProperties(envVars);
        server.startServer();
    }

    @Test
    public void testBulkheadSynchronous() throws Exception {

        // Make an initial request so that everything is initialized
        HttpUtils.getHttpResponseAsString(server, "/CDIFaultTolerance/multi-request-bulkhead");

        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Connect C has a pool size of 2
        // Fire three requests in parallel
        Future<String> future1 = executor.submit(() -> HttpUtils.getHttpResponseAsString(server, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(CDIBulkheadTest.TEST_TWEAK_TIME_UNIT);

        Future<String> future2 = executor.submit(() -> HttpUtils.getHttpResponseAsString(server, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(CDIBulkheadTest.TEST_TWEAK_TIME_UNIT);

        Future<String> future3 = executor.submit(() -> HttpUtils.getHttpResponseAsString(server, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(CDIBulkheadTest.TEST_TWEAK_TIME_UNIT);

        executor.shutdown();

        // First two tasks should succeed
        assertThat("Task One", future1.get(CDIBulkheadTest.TIMEOUT + CDIBulkheadTest.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS), containsString("Success"));
        assertThat("Task Two", future2.get(CDIBulkheadTest.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS), containsString("Success"));

        // Third task should fail with a Bulkhead exception
        assertThat("Task Three", future3.get(CDIBulkheadTest.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS), containsString("Success"));
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
