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
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@Mode(TestMode.FULL)
public class CDIAsyncTest extends LoggingTest {

    //run against both EE8 and EE7 features
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE7_FEATURES());
    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("CDIFaultTolerance");

    @Test
    public void testAsync() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/async?testMethod=testAsync",
                                         "SUCCESS");
    }

    @Test
    public void testAsyncVoid() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/async?testMethod=testAsyncVoid",
                                         "SUCCESS");
    }

    @Test
    public void testAsyncTimeout() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/async?testMethod=testAsyncTimeout",
                                         "SUCCESS");
    }

    @Test
    public void testAsyncMethodTimeout() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/async?testMethod=testAsyncMethodTimeout",
                                         "SUCCESS");
    }

    @Test
    @Ignore // Known issues, reenable under #937
    public void testAsyncDoubleJump() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/async?testMethod=testAsyncDoubleJump",
                                         "SUCCESS");
    }

    @Test
    public void testAsyncCallable() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/async?testMethod=testAsyncCallable",
                                         "SUCCESS");
    }

    @Test
    public void testAsyncConfig() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/async?testMethod=testAsyncConfig",
                                         "SUCCESS");
    }

    @Test
    public void testAsyncConfigInjected() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/async?testMethod=testAsyncConfigInjected",
                                         "SUCCESS");
    }

    @Test
    public void testAsyncGetCdi() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/async?testMethod=testAsyncGetCdi",
                                         "SUCCESS");
    }

    @Test
    public void testAsyncGetBeanManagerViaJndi() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/CDIFaultTolerance/async?testMethod=testAsyncGetBeanManagerViaJndi",
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
