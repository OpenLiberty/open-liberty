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
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

/**
 * Test the server when the non-Fallback annotations are disabled by setting MP_Fault_Tolerance_NonFallback_Enabled = false
 */
public class CDIAnnotationsDisabledTest extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER;
    static {
        SHARED_SERVER = new SharedServer("CDIFaultTolerance");
        Map<String, String> envVars = new HashMap<>();
        envVars.put("MP_Fault_Tolerance_NonFallback_Enabled", "false");
        SHARED_SERVER.getLibertyServer().setAdditionalSystemProperties(envVars);
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testAsync() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/async?testMethod=testAsyncDisabled", "SUCCESS");
    }

    @Test
    public void testCircuitBreaker() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBDisabled", "SUCCESS");
    }

    @Test
    public void testRetry() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/retry?testMethod=testRetryDisabled", "SUCCESS");
    }

    @Test
    public void testTimeout() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/timeout?testMethod=testTimeoutDisabled", "SUCCESS");
    }

    /**
     * Fallback is a special case, it's not disabled by by setting MP_Fault_Tolerance_NonFallback_Enabled = false
     * <p>
     * Here we're calling one of the regular fallback tests which should still work.
     */
    @Test
    public void testFallbackWithoutRetry() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/fallback?testMethod=testFallbackWithoutRetry", "SUCCESS");
    }

    /**
     * Here we're calling a Fallback test which uses Retry, but expecting the retry to have no effect
     */
    @Test
    public void testFallbackRetryDisabled() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/fallback?testMethod=testFallbackRetryDisabled", "SUCCESS");
    }

    @Test
    public void testBulkheadSynchronous() throws Exception {

        WebBrowser browser = createWebBrowserForTestCase();
        // Make an initial request so that everything is initialized
        getSharedServer().getResponse(browser, "/CDIFaultTolerance/multi-request-bulkhead");

        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Connect C has a pool size of 2
        // Fire three requests in parallel
        Future<WebResponse> future1 = executor.submit(() -> getSharedServer().getResponse(browser, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(CDIBulkheadTest.TEST_TWEAK_TIME_UNIT);

        Future<WebResponse> future2 = executor.submit(() -> getSharedServer().getResponse(browser, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(CDIBulkheadTest.TEST_TWEAK_TIME_UNIT);

        Future<WebResponse> future3 = executor.submit(() -> getSharedServer().getResponse(browser, "/CDIFaultTolerance/multi-request-bulkhead"));
        Thread.sleep(CDIBulkheadTest.TEST_TWEAK_TIME_UNIT);

        executor.shutdown();

        // First two tasks should succeed
        assertThat("Task One", future1.get(CDIBulkheadTest.TIMEOUT + CDIBulkheadTest.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS).getResponseBody(), containsString("Success"));
        assertThat("Task Two", future2.get(CDIBulkheadTest.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS).getResponseBody(), containsString("Success"));

        // Third task should fail with a Bulkhead exception
        assertThat("Task Three", future3.get(CDIBulkheadTest.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS).getResponseBody(), containsString("Success"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            /*
             * Ignore following exception as those are expected:
             * CWWKC1101E: The task com.ibm.ws.microprofile.faulttolerance.cdi.FutureTimeoutMonitor@3f76c259, which was submitted to executor service
             * managedScheduledExecutorService[DefaultManagedScheduledExecutorService], failed with the following error:
             * org.eclipse.microprofile.faulttolerance.exceptions.FTTimeoutException: java.util.concurrent.TimeoutException
             */
            SHARED_SERVER.getLibertyServer().stopServer("CWWKC1101E");
        }
    }

}
