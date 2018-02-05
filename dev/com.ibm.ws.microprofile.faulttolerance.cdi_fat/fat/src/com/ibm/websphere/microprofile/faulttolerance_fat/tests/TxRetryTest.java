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
public class TxRetryTest extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("TxFaultTolerance");

    @Test
    public void testRetry() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/TxFaultTolerance/retry?testMethod=testRetry",
                                         "SUCCESS");
    }

    @Test
    public void testRetry2() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/TxFaultTolerance/retry?testMethod=testRetry2",
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
            SHARED_SERVER.getLibertyServer().stopServer();
        }
    }
}
