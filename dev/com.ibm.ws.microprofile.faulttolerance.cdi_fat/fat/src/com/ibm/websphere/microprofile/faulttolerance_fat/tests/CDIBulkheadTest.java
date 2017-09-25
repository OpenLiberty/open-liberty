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
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.LITE)
public class CDIBulkheadTest extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("CDIFaultTolerance");

    public static final long TEST_TWEAK_TIME_UNIT = 100;
    public static final long TIMEOUT = 5000;
    public static final long FUTURE_THRESHOLD = 2000;

    @Test
    public void testAsyncBulkheadSmall() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/asyncbulkhead?testMethod=testAsyncBulkheadSmall",
                                         "SUCCESS");
    }

    @Test
    public void testAsyncBulkheadQueueFull() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/asyncbulkhead?testMethod=testAsyncBulkheadQueueFull",
                                         "SUCCESS");
    }

    @Test
    public void testAsyncBulkheadTimeout() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/asyncbulkhead?testMethod=testAsyncBulkheadTimeout",
                                         "SUCCESS");
    }

    @Test
    public void testSyncBulkheadSmall() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/bulkhead?testMethod=testSyncBulkheadSmall",
                                         "SUCCESS");
    }

    @Test
    public void testSyncBulkheadCircuitBreaker() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/bulkhead?testMethod=testSyncBulkheadCircuitBreaker",
                                         "SUCCESS");
    }

    @Test
    public void testSyncBulkheadFallback() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/bulkhead?testMethod=testSyncBulkheadFallback",
                                         "SUCCESS");
    }

    /**
     * Test a synchronous bulkhead by firing multiple requests at it
     */
    @Test
    public void testMultiRequestBulkhead() throws Exception {

        WebBrowser browser = createWebBrowserForTestCase();
        // Make an initial request so that everything is initialized
        getSharedServer().getResponse(browser, "/CDIFaultTolerance/multi-request-bulkhead");

        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Connect C has a pool size of 2
        // Fire three requests in parallel
        Future<WebResponse> future1 = executor.submit(() -> getSharedServer().getResponse(browser, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(TEST_TWEAK_TIME_UNIT);

        Future<WebResponse> future2 = executor.submit(() -> getSharedServer().getResponse(browser, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(TEST_TWEAK_TIME_UNIT);

        Future<WebResponse> future3 = executor.submit(() -> getSharedServer().getResponse(browser, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(TEST_TWEAK_TIME_UNIT);

        executor.shutdown();

        // First two tasks should succeed
        assertThat("Task One", future1.get(TIMEOUT + FUTURE_THRESHOLD, TimeUnit.MILLISECONDS).getResponseBody(), containsString("Success"));
        assertThat("Task Two", future2.get(FUTURE_THRESHOLD, TimeUnit.MILLISECONDS).getResponseBody(), containsString("Success"));

        // Third task should fail with a Bulkhead exception
        assertThat("Task Three", future3.get(TIMEOUT + FUTURE_THRESHOLD, TimeUnit.MILLISECONDS).getResponseBody(), containsString("BulkheadException"));
    }

    /** {@inheritDoc} */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer();
        }
    }
}
