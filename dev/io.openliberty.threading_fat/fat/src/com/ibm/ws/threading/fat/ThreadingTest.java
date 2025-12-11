/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.threading.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class ThreadingTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.threading.hangtest");
    private static final Class<?> c = ThreadingTest.class;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final String method = "beforeClass";
        Log.entering(c, method);

        boolean serverWasStarted = false;

        if (server != null && !server.isStarted()) {
            server.startServer();
            serverWasStarted = true;
        }

        Log.exiting(c, method, serverWasStarted);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        final String method = "afterClass";
        Log.entering(c, method);

        boolean serverWasStopped = false;

        if (server != null && server.isStarted()) {
            server.stopServer();
            serverWasStopped = true;
        }

        Log.exiting(c, method, serverWasStopped);
    }

    @Test
    public void testExecutorHang() throws Exception {
        final String method = "testExecutorHang";
        Log.entering(c, method);

        assertNotNull("Expected message indicating the test passed on the server was not found.", server.waitForStringInLog("runExecutorHangTest PASSED"));

        Log.exiting(c, method);
    }

}
