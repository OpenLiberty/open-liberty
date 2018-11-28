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
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.AsyncBulkheadServlet;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.SyncBulkheadServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class CDIBulkheadTest extends FATServletClient {

    private static final String SERVER_NAME = "CDIFaultTolerance";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(contextRoot = "CDIFaultTolerance", servlet = AsyncBulkheadServlet.class),
                    @TestServlet(contextRoot = "CDIFaultTolerance", servlet = SyncBulkheadServlet.class),
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeatDefault(SERVER_NAME);

    public static final long TEST_TWEAK_TIME_UNIT = 100;
    public static final long TIMEOUT = 5000;
    public static final long FUTURE_THRESHOLD = 2000;

    @BeforeClass
    public static void setup() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Test a synchronous bulkhead by firing multiple requests at it
     */
    @Test
    public void testMultiRequestBulkhead() throws Exception {

        // Make an initial request so that everything is initialized
        HttpUtils.findStringInReadyUrl(server, "/CDIFaultTolerance/multi-request-bulkhead", "Success");

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
        assertThat("Task Three", future3.get(TIMEOUT + FUTURE_THRESHOLD, TimeUnit.MILLISECONDS), containsString("BulkheadException"));
    }

}
