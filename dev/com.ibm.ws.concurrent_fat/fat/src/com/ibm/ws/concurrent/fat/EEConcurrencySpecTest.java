/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.fat;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import fat.concurrent.spec.app.EEConcurrencyTestServlet;

/**
 * Tests for EE Concurrency Utilities, including tests that make updates to the server
 * configuration while the server is running.
 * A setUpPerTest method runs before each test to restore to the original configuration,
 * so that tests do not interfere with each other.
 */
@RunWith(FATRunner.class)
public class EEConcurrencySpecTest extends FATServletClient {

    private static final String APP_NAME = "concurrentSpec";

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .withoutModification()
                    .andWith(new JakartaEE9Action());

    @Server("concurrent.spec.fat")
    @TestServlet(servlet = EEConcurrencyTestServlet.class, path = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "fat.concurrent.spec.app");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.resource.testresource.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/concurrenttest-1.0.mf");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKC1101E", "CWWKC1102E", "CWWKC1103E");
    }

    @Test
    public void testDemo() throws Exception {
        FATServletClient.runTest(server, APP_NAME + "/demo", "testOneTimeScheduledTask&interval=1");
        Assert.assertNotNull("Timed out waiting for message: 'One-time task looked up this value: 100'",
                             server.waitForStringInLog("One-time task looked up this value: 100"));
    }
}