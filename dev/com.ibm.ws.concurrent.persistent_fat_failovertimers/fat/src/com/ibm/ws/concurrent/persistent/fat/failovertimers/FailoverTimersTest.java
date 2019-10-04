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
import java.util.stream.Collectors;

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
        ShrinkHelper.defaultApp(serverA, APP_NAME, "failovertimers.web", "failovertimers.ejb.autotimer", "failovertimers.ejb.stateless");

        serverB.useSecondaryHTTPPort();
        originalConfigB = serverB.getServerConfiguration();
        ShrinkHelper.defaultApp(serverB, APP_NAME, "failovertimers.web", "failovertimers.ejb.autotimer", "failovertimers.ejb.stateless");

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
     * On one of the servers, programmatically start a persistent timer which runs outside of a global transaction.
     * Stop the application on that server (but not the server itself)
     * and verify that the timer starts running on the same application on a different server.
     */
    @Test
    public void testProgrammaticTimerWithTxSuspendedFailsOverWhenAppStops() throws Exception {
        runTest(serverB, APP_NAME + "/FailoverTimersTestServlet",
                "testScheduleStatelessTxSuspendedTimer&timer=StatelessTxSuspendedTimer_1_2&initialDelayMS=1000&intervalMS=2000&test=testProgrammaticTimerWithTxSuspendedFailsOverWhenAppStops[1]");
        try {
            ServerConfiguration newConfig = originalConfigB.clone();
            newConfig.getApplications().removeBy("location", "failoverTimersApp.war");
            serverB.setMarkToEndOfLog();
            serverB.updateServerConfiguration(newConfig);
            try {
                runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                        "testTimerFailover&timer=StatelessTxSuspendedTimer_1_2&server=" + SERVER_A_NAME + "&test=testProgrammaticTimerWithTxSuspendedFailsOverWhenAppStops[2]");
            } finally {
                Set<String> remainingApps = APP_NAMES.stream().filter(s -> !s.equals(APP_NAME)).collect(Collectors.toSet());
                serverB.waitForConfigUpdateInLogUsingMark(remainingApps);
            }
        } finally {
            serverB.setMarkToEndOfLog();
            serverB.updateServerConfiguration(originalConfigB);
            serverB.waitForConfigUpdateInLogUsingMark(APP_NAMES);

            // The server upon which the application was stopped will try to run the task again
            // upon seeing that the application has become available.
            // However, the task now belongs to a different member (paritionId). It should be skipped silently without errors.
            Thread.sleep(2000);

            // Also restart the server. This allows us to process any expected warning messages that are logged in response
            // to the application going away while its scheduled tasks remain.
            serverB.stopServer("CWWKC1556W"); // Execution of tasks from application failoverTimersApp is deferred until the application and modules that scheduled the tasks are available.
            serverB.startServer("after-testProgrammaticTimerWithTxSuspendedFailsOverWhenAppStops");

            runTest(serverB, APP_NAME + "/FailoverTimersTestServlet", "testCancelStatelessTxSuspendedTimers&test=testProgrammaticTimerWithTxSuspendedFailsOverWhenAppStops[3]");
        }
    }

    /**
     * Determine which server an automatic persistent timer, which is a Singleton EJB, is running on.
     * Stop the application on that server (but not the server itself)
     * and verify that the timer starts running on the same application on a different server.
     */
    @Test
    public void testSingletonTimerFailsOverWhenAppStops() throws Exception {
        StringBuilder sb = runTestWithResponse(serverA, APP_NAME + "/FailoverTimersTestServlet",
                "findServerWhereTimerRuns&timer=AutomaticCountingSingletonTimer&test=testSingletonTimerFailsOverWhenAppStops[1]");
        assertEquals(sb.toString(), 0, sb.indexOf(TIMER_LAST_RAN_ON));
        String serverName = sb.substring(TIMER_LAST_RAN_ON.length(), sb.lastIndexOf("."));
        assertTrue(serverName, SERVER_A_NAME.equals(serverName) || SERVER_B_NAME.equals(serverName));

        LibertyServer serverOnWhichToStopApp = SERVER_A_NAME.equals(serverName) ? serverA : serverB;
        ServerConfiguration originalConfig = serverOnWhichToStopApp == serverA ? originalConfigA : originalConfigB;
        try {
            ServerConfiguration newConfig = originalConfig.clone();
            newConfig.getApplications().removeBy("location", "failoverTimersApp.war");
            serverOnWhichToStopApp.setMarkToEndOfLog();
            serverOnWhichToStopApp.updateServerConfiguration(newConfig);
            try {
                String nameOfServerForFailover = serverOnWhichToStopApp == serverA ? SERVER_B_NAME : SERVER_A_NAME;
                LibertyServer serverForFailover = serverOnWhichToStopApp == serverA ? serverB : serverA;

                runTest(serverForFailover, APP_NAME + "/FailoverTimersTestServlet",
                        "testTimerFailover&timer=AutomaticCountingSingletonTimer&server=" + nameOfServerForFailover + "&test=testSingletonTimerFailsOverWhenAppStops[2]");
            } finally {
                Set<String> remainingApps = APP_NAMES.stream().filter(s -> !s.equals(APP_NAME)).collect(Collectors.toSet());
                serverOnWhichToStopApp.waitForConfigUpdateInLogUsingMark(remainingApps);
            }
        } finally {
            serverOnWhichToStopApp.setMarkToEndOfLog();
            serverOnWhichToStopApp.updateServerConfiguration(originalConfig);
            serverOnWhichToStopApp.waitForConfigUpdateInLogUsingMark(APP_NAMES);

            // The server upon which the application was stopped will try to run the task again
            // upon seeing that the application has become available.
            // However, the task now belongs to a different member (paritionId). It should be skipped silently without errors.
            Thread.sleep(2000);

            // Also restart the server. This allows us to process any expected warning messages that are logged in response
            // to the application going away while its scheduled tasks remain.
            serverOnWhichToStopApp.stopServer("CWWKC1556W"); // Execution of tasks from application failoverTimersApp is deferred until the application and modules that scheduled the tasks are available.
            serverOnWhichToStopApp.startServer("after-testSingletonTimerFailsOverWhenAppStops");
        }
    }

    /**
     * Determine which server an automatic persistent timer is running on.
     * Stop that server and verify that the timer starts running on a different server.
     */
    @Test
    public void testTimerFailsOverWhenServerStops() throws Exception {
        StringBuilder sb = runTestWithResponse(serverA, APP_NAME + "/FailoverTimersTestServlet",
                "findServerWhereTimerRuns&timer=AutomaticCountingSingletonTimer&test=testTimerFailsOverWhenServerStops[1]");
        assertEquals(sb.toString(), 0, sb.indexOf(TIMER_LAST_RAN_ON));
        String serverName = sb.substring(TIMER_LAST_RAN_ON.length(), sb.lastIndexOf("."));
        assertTrue(serverName, SERVER_A_NAME.equals(serverName) || SERVER_B_NAME.equals(serverName));

        LibertyServer serverToStop = SERVER_A_NAME.equals(serverName) ? serverA : serverB;
        try {
            serverToStop.stopServer();

            String nameOfServerForFailover = serverToStop == serverA ? SERVER_B_NAME : SERVER_A_NAME;
            LibertyServer serverForFailover = serverToStop == serverA ? serverB : serverA;

            runTest(serverForFailover, APP_NAME + "/FailoverTimersTestServlet",
                    "testTimerFailover&timer=AutomaticCountingSingletonTimer&server=" + nameOfServerForFailover + "&test=testTimerFailsOverWhenServerStops[2]");
        } finally {
            serverToStop.startServer();
        }
    }
}