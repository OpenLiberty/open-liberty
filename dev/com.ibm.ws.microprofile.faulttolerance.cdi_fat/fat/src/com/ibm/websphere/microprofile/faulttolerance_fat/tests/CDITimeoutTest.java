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
import org.junit.runner.RunWith;

import com.ibm.ws.microprofile.faulttolerance.fat.repeat.RepeatFaultTolerance;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TimeoutServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class CDITimeoutTest extends FATServletClient {

    private static final String SERVER_NAME = "CDIFaultTolerance";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = "CDIFaultTolerance", servlet = TimeoutServlet.class)
    public static LibertyServer server;

    //run against both EE8 and EE7 features
    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeatAll(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
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
