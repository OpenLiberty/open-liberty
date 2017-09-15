/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class MemLeakTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.threading.memleak_fat_server");
    private static final Class<?> c = MemLeakTest.class;

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

    /**
     * Starting the server with this configuration memLeakTest=true will enabled the
     * MemLeakChecker class to activate and run tests to see if we are leaking memory.
     * 
     * This test checks to see if we leak memory by scheduling and then canceling
     * a large number of tasks.
     */
    @Test
    public void testScheduleCancel() throws Exception {
        final String method = "testScheduleCancel";
        Log.entering(c, method);

        assertNotNull("Expected message indicating the test passed on the server was not found.", server.waitForStringInLog("runScheduleCancelTest PASSED"));

        Log.exiting(c, method);
    }

    /**
     * Starting the server with this configuration memLeakTest=true will enabled the
     * MemLeakChecker class to activate and run tests to see if we are leaking memory.
     * 
     * This test checks to see if we leak memory by scheduling and then running
     * a large number of tasks.
     */
    @Test
    public void testScheduleExecute() throws Exception {
        final String method = "testScheduleExecute";
        Log.entering(c, method);

        assertNotNull("Expected message indicating the test passed on the server was not found.", server.waitForStringInLog("runScheduleExecuteTest PASSED"));

        Log.exiting(c, method);
    }
}
