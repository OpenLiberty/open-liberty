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

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@Mode(TestMode.LITE)
public class CDIRetryTest extends LoggingTest {

    //run at least one test class with mpConfig-1.2 as well as 1.1
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction("mpConfig-1.1", "mpConfig-1.2"));

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("CDIFaultTolerance");

    @Test
    public void testRetry() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/retry?testMethod=testRetry",
                                         "SUCCESS");
    }

    @Test
    public void testInheritedAnnotations() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/retry?testMethod=testInheritedAnnotations",
                                         "SUCCESS");
    }

    @Test
    public void testAbortOn() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/retry?testMethod=testRetryAbortOn", "SUCCESS");
    }

    @Test
    public void testAbortOnAsync() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/retry?testMethod=testRetryAbortOnAsync", "SUCCESS");
    }

    @Test
    public void testAbortOnConfig() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/retry?testMethod=testRetryAbortOnConfig", "SUCCESS");
    }

    @Test
    public void testRetryForever() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/retry?testMethod=testRetryForever", "SUCCESS");
    }

    @Test
    public void testRetryDurationZero() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/retry?testMethod=testRetryDurationZero", "SUCCESS");
    }

    @Test
    public void testRetryMaxRetriesConfig() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/retry?testMethod=testRetryMaxRetriesConfig", "SUCCESS");
    }

    @Test
    public void testRetryMaxRetriesClassScopeConfig() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/retry?testMethod=testRetryMaxRetriesClassScopeConfig", "SUCCESS");
    }

    @Test
    public void testRetryMaxRetriesClassLevelConfigForMethodAnnotation() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/retry?testMethod=testRetryMaxRetriesClassLevelConfigForMethodAnnotation", "SUCCESS");
    }

    /**
     * Not really related to retry but it's easiest to test it here
     */
    @Test
    public void testExecutorsClose() throws Exception {

        RemoteFile traceLog = SHARED_SERVER.getLibertyServer().getMostRecentTraceFile();
        SHARED_SERVER.getLibertyServer().setMarkToEndOfLog(traceLog);

        // This calls a RequestScoped bean which only has fault tolerance annotations on the method
        // This should cause executors to get cleaned up
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/retry?testMethod=testRetryAbortOn", "SUCCESS");

        SHARED_SERVER.getLibertyServer().waitForStringInLog("Cleaning up executors", traceLog);
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
