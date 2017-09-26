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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.LITE)
public class CDICircuitBreakerTest extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("CDIFaultTolerance");

    @Test
    public void testCBFailureThresholdWithTimeout() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBFailureThresholdWithTimeout",
                                         "SUCCESS");
    }

    @Test
    public void testCBFailureThresholdWithException() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBFailureThresholdWithException",
                                         "SUCCESS");
    }

    @Test
    public void testCBAsync() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBAsync",
                                         "SUCCESS");
    }

    @Test
    public void testCBAsyncFallback() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBAsyncFallback",
                                         "SUCCESS");
    }

    @Test
    public void testCBSyncFallback() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBSyncFallback",
                                         "SUCCESS");
    }

    @Test
    public void testCBSyncRetryCircuitOpens() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBSyncRetryCircuitOpens",
                                         "SUCCESS");
    }

    @Test
    public void testCBSyncRetryCircuitClosed() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBSyncRetryCircuitClosed",
                                         "SUCCESS");
    }

    @Test
    public void testCBAsyncRetryCircuitOpens() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBAsyncRetryCircuitOpens",
                                         "SUCCESS");
    }

    @Test
    public void testCBAsyncRetryCircuitClosed() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBAsyncRetryCircuitClosed",
                                         "SUCCESS");
    }

    @Test
    public void testCBFailureThresholdWithRoll() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBFailureThresholdWithRoll",
                                         "SUCCESS");
    }

    @Test
    public void testCBFailureThresholdConfig() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBFailureThresholdConfig",
                                         "SUCCESS");
    }

    @Test
    public void testCBFailureThresholdClassScopeConfig() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBFailureThresholdClassScopeConfig",
                                         "SUCCESS");
    }

    @Test
    public void testCBDelayConfig() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/circuitbreaker?testMethod=testCBDelayConfig",
                                         "SUCCESS");
    }

    /** {@inheritDoc} */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        if (!SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().startServer();
        }

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
