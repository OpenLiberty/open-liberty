/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.concurrent.persistent.fat.demo.timers;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import ejb.timers.PersistentDemoTimersServlet;

/**
 * This test suite start's an application that has automated timers,
 * and scheduled timers that will perform some sort of in memory data manipulation.
 * This is to simulate the situation where customers use timers to do something
 * like unit conversions, data processing, etc.
 *
 * These timers will run every half second. That sort of frequency is the
 * maximum we would ever expect a customer to run a timer that is doing
 * in memory work.
 */
@RunWith(FATRunner.class)
public class DemoTimerTest extends FATServletClient {
    private static final Class<DemoTimerTest> c = DemoTimerTest.class;

    public static final String APP_NAME = "demotimer";

    @Server("com.ibm.ws.concurrent.persistent.fat.demo.timers")
    @TestServlet(servlet = PersistentDemoTimersServlet.class, path = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        //Install App
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "ejb.timers");

        //Start server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKC1501W", "CWWKC1511W");
    }
}