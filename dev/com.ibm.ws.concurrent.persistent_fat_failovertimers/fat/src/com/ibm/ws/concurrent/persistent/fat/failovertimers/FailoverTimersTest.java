/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.failovertimers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import failovertimers.web.FailoverTimersTestServlet;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;

/**
 * This test bucket will cover scenarios where EJB persistent timers are shared across multiple servers,
 * by pointing at the same database.  When the instance that has been running the timer goes down, it should
 * fail over to another server and continue running there.
 */
@RunWith(FATRunner.class)
public class FailoverTimersTest extends FATServletClient {
	private static final String APP_NAME = "failoverTimersApp";
	private static final Set<String> APP_NAMES = Collections.singleton(APP_NAME);

    private static ServerConfiguration originalConfigA;
    private static ServerConfiguration originalConfigB;

    private static final String SERVER_A_NAME = "com.ibm.ws.concurrent.persistent.fat.failovertimers.serverA";
    private static final String SERVER_B_NAME = "com.ibm.ws.concurrent.persistent.fat.failovertimers.serverB";

    @Server(SERVER_A_NAME)
    @TestServlet(servlet = FailoverTimersTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverA;

    @Server(SERVER_B_NAME)
    @TestServlet(servlet = FailoverTimersTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverB;

    private static final ExecutorService testThreads = Executors.newFixedThreadPool(3);

    private static final String TIMER_LAST_RAN_ON = "Timer last ran on server: ";

    @BeforeClass
    public static void setUp() throws Exception {
        originalConfigA = serverA.getServerConfiguration();
        ShrinkHelper.defaultApp(serverA, APP_NAME, "failovertimers.web", "failovertimers.ejb.autotimer");

        serverB.useSecondaryHTTPPort();
        originalConfigB = serverB.getServerConfiguration();
        ShrinkHelper.defaultApp(serverB, APP_NAME, "failovertimers.web", "failovertimers.ejb.autotimer");

        // TODO Test infrastructure is unable to start multiple servers at once. Intermittent errors occur while processing fatFeatureList.xml
        boolean startInParallel = false;

        if (startInParallel) {
            testThreads.invokeAll(Arrays.asList(
                    () -> serverA.startServer(),
                    () -> serverB.startServer()
            )).forEach(f -> {
                try {
                    f.get();
                } catch (ExecutionException | InterruptedException x) {
                    throw new CompletionException(x);
                }
            });
        } else {
            serverA.startServer();
            serverB.startServer();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (serverA.isStarted())
                serverA.stopServer();
        } finally {
            if (serverB.isStarted())
                serverB.stopServer();
        }
    }

    /**
     * Determine which server an automatic persistent timer is running on.
     * Stop that server and verify that the timer starts running on a different server.
     */
    @Test
    public void testTimerFailsOverWhenServerStops() throws Exception {
        StringBuilder sb = runTestWithResponse(serverA, APP_NAME + "/FailoverTimersTestServlet",
                "findServerWhereTimerRuns&timer=AutomaticCountingTimer&test=testTimerFailsOverWhenServerStops[1]");
        assertEquals(sb.toString(), 0, sb.indexOf(TIMER_LAST_RAN_ON));
        String serverName = sb.substring(TIMER_LAST_RAN_ON.length(), sb.lastIndexOf("."));
        assertTrue(serverName, SERVER_A_NAME.equals(serverName) || SERVER_B_NAME.equals(serverName));

        LibertyServer serverToStop = SERVER_A_NAME.equals(serverName) ? serverA : serverB;
        try {
            serverToStop.stopServer();

            String nameOfServerForFailover = serverToStop == serverA ? SERVER_B_NAME : SERVER_A_NAME;
            LibertyServer serverForFailover = serverToStop == serverA ? serverB : serverA;

            runTest(serverForFailover, APP_NAME + "/FailoverTimersTestServlet",
                    "testTimerFailover&timer=AutomaticCountingTimer&server=" + nameOfServerForFailover + "&test=testTimerFailsOverWhenServerStops[2]");
        } finally {
            serverToStop.startServer();
        }
    }
}