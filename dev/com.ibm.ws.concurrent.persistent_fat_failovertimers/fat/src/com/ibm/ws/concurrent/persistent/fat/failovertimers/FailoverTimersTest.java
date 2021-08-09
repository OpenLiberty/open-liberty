/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.failovertimers;

import static componenttest.annotation.SkipIfSysProp.DB_Oracle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import failovertimers.web.FailoverTimersTestServlet;

/**
 * This test bucket will cover scenarios where EJB persistent timers are shared across multiple servers,
 * by pointing at the same database. When the instance that has been running the timer goes down, it should
 * fail over to another server and continue running there.
 */
@RunWith(FATRunner.class)
@SkipIfSysProp(DB_Oracle) //TODO investigate running these tests causes an FFDC java.lang.IllegalStateException: Attempting to execute an operation on a closed EntityManagerFactory.
@AllowedFFDC
public class FailoverTimersTest extends FATServletClient {
    private static final Class<FailoverTimersTest> c = FailoverTimersTest.class;
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

    @Rule
    public TestName testName = new TestName();

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

    private static final ExecutorService testThreads = Executors.newFixedThreadPool(3);

    private static final String TIMER_LAST_RAN_ON = "Timer last ran on server: ";

    @BeforeClass
    public static void setUp() throws Exception {
        //Setup datasource properties
        DatabaseContainerUtil.setupDataSourceProperties(serverA, testContainer);
        DatabaseContainerUtil.setupDataSourceProperties(serverB, testContainer);

        //Application uses an XA datasource to perform database access.
        //Oracle restrictions creation/dropping of database tables using transactions with error:
        //  ORA-02089: COMMIT is not allowed in a subordinate session
        //Therefore, we will create the table prior to running tests when running against oracle.
        if (DatabaseContainerType.valueOf(testContainer) == DatabaseContainerType.Oracle) {
            final String createTable = "CREATE TABLE TIMERLOG (TIMERNAME VARCHAR(254) NOT NULL PRIMARY KEY, COUNT INT NOT NULL, SERVERNAME VARCHAR(254) NOT NULL)";

            try (Connection conn = testContainer.createConnection("")) {
                try (PreparedStatement pstmt = conn.prepareStatement(createTable)) {
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                fail(c.getName() + " caught exception when initializing table: " + e.getMessage());
            }
        }

        originalConfigA = serverA.getServerConfiguration();
        ShrinkHelper.defaultApp(serverA, APP_NAME, "failovertimers.web", "failovertimers.ejb.autotimer", "failovertimers.ejb.stateless");

        serverB.useSecondaryHTTPPort();
        originalConfigB = serverB.getServerConfiguration();

        // Transform serverB's app to Jakarta:
        Path javaEEApp = Paths.get("publish", "servers", "com.ibm.ws.concurrent.persistent.fat.failovertimers.serverA", "apps", APP_NAME + ".war");
        Path jakartaEEApp = Paths.get("publish", "servers", "com.ibm.ws.concurrent.persistent.fat.failovertimers.serverB", "apps", APP_NAME + ".war");
        // /apps/ folder must exist for transform to run
        File apps = Paths.get("publish", "servers", "com.ibm.ws.concurrent.persistent.fat.failovertimers.serverB", "apps").toFile();
        apps.mkdir();

        JakartaEE9Action.transformApp(javaEEApp, jakartaEEApp);
        Log.info(c, "setUp", "Transformed app " + javaEEApp + " to " + jakartaEEApp);
        serverB.copyFileToLibertyServerRoot(apps.toString(), "apps", APP_NAME + ".war");
        Log.info(c, "setUp", "Copied from " + apps + " to " + serverB.getServerRoot() + "/apps/");
    }

    /**
     * Ensure both servers are started before running each test.
     */
    @Before
    public void setUpPerTest() throws Exception {
        ArrayList<Callable<ProgramOutput>> startActions = new ArrayList<>();
        if (!serverA.isStarted()) {
            serverA.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());
            startActions.add(() -> serverA.startServer(testName.getMethodName() + ".log"));
        }
        if (!serverB.isStarted()) {
            serverB.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());
            startActions.add(() -> serverB.startServer(testName.getMethodName() + ".log"));
        }

        testThreads.invokeAll(startActions).forEach(f -> {
            try {
                f.get();
            } catch (ExecutionException | InterruptedException x) {
                throw new CompletionException(x);
            }
        });
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (serverA.isStarted())
                serverA.stopServer("CWWKC1503W",
                                   "DSRA0302E", // can happen if timer tries to run while the server stops
                                   "DSRA0304E" // can happen if timer tries to run while the server stops
                );
        } finally {
            if (serverB.isStarted())
                serverB.stopServer("CWWKC1503W",
                                   "DSRA0302E", // can happen if timer tries to run while the server stops
                                   "DSRA0304E" // can happen if timer tries to run while the server stops
                );
        }
    }

    /**
     * On one of the servers, programmatically start a persistent timer which runs in the same global transaction
     * as the database operations performed by the EJB Timer Server/persistent executor.
     * Force the timer to start failing on that server (but keep the application and the server up)
     * and verify that the timer starts running on the same application on a different server.
     * This should occur even if a retryInterval is configured on the server where the failure occurs.
     */
    @Test
    public void testProgrammaticTimerFailsOverWhenTimerFailsOnOneServer() throws Exception {
        runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                "testScheduleStatelessTimer&timer=Timer_400_1700&initialDelayMS=400&intervalMS=1700&test=testProgrammaticTimerFailsOverWhenTimerFailsOnOneServer[1]");
        try {
            // Make the timer fail when it runs on the server (A) from which it was scheduled
            runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                    "disallowTimer&timer=Timer_400_1700&test=testProgrammaticTimerFailsOverWhenTimerFailsOnOneServer[2]");

            // Verify that the timer fails over to the other server (B) and runs there
            runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                    "testTimerFailover&timer=Timer_400_1700&server=" + SERVER_B_NAME + "&test=testProgrammaticTimerFailsOverWhenTimerFailsOnOneServer[3]");
        } finally {
            // The server (serverA) upon which the timer failed will initially continue trying to run it.
            // However, the timer was taken over by a different member which is likely continuing to claim executions of it,
            // in which case it should be skipped silently on serverA and then no longer rescheduled there
            // unless the other member doesn't claim it.
            Thread.sleep(2000);

            runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                    "allowTimer&timer=Timer_400_1700&test=testProgrammaticTimerFailsOverWhenTimerFailsOnOneServer[4]");

            runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                    "testCancelStatelessProgrammaticTimers&test=testProgrammaticTimerFailsOverWhenTimerFailsOnOneServer[5]");
        }

        // Also restart the server. This allows us to process any expected warning messages that are logged in response
        // to the intentionally failed task.
        serverA.stopServer(
                           "CNTR0020E", // EJB threw an unexpected (non-declared) exception during invocation of ...
                           "CWWKC1501W", // Persistent executor defaultEJBPersistentTimerExecutor rolled back task ...
                           "CWWKC1503W", // Persistent executor defaultEJBPersistentTimerExecutor rolled back task ... due to failure ...
                           "DSRA.*", "J2CA.*", "WTRN.*" // task running during server shutdown
        );
    }

    /**
     * On one of the servers, programmatically start a persistent timer which runs in the same global transaction
     * as the database operations performed by the EJB Timer Server/persistent executor.
     * Force the timer to start rolling back on that server (but keep the application and the server up)
     * and verify that the timer starts running on the same application on a different server.
     * This should occur even if a retryInterval is configured on the server where the failure occurs.
     */
    @Test
    public void testProgrammaticTimerFailsOverWhenTimerRollsBackOnOneServer() throws Exception {
        runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                "testScheduleStatelessTimer&timer=Timer_300_1800&initialDelayMS=300&intervalMS=1800&test=testProgrammaticTimerFailsOverWhenTimerRollsBackOnOneServer[1]");
        try {
            // Make the timer roll back when it runs on the server (A) from which it was scheduled
            runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                    "forceRollbackForTimer&timer=Timer_300_1800&test=testProgrammaticTimerFailsOverWhenTimerRollsBackOnOneServer[2]");

            // Verify that the timer fails over to the other server (B) and runs there
            runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                    "testTimerFailover&timer=Timer_300_1800&server=" + SERVER_B_NAME + "&test=testProgrammaticTimerFailsOverWhenTimerRollsBackOnOneServer[3]");
        } finally {
            // The server (serverA) upon which the timer rolled back will initially continue trying to run it.
            // However, the timer was taken over by a different member which is likely continuing to claim executions of it,
            // in which case it should be skipped silently on serverA and then no longer rescheduled there
            // unless the other member doesn't claim it.
            Thread.sleep(2000);

            runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                    "allowTimerCommit&timer=Timer_300_1800&test=testProgrammaticTimerFailsOverWhenTimerRollsBackOnOneServer[4]");

            runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                    "testCancelStatelessProgrammaticTimers&test=testProgrammaticTimerFailsOverWhenTimerRollsBackOnOneServer[5]");
        }

        // Also restart the server. This allows us to process any expected warning messages that are logged in response
        // to the intentionally failed task.
        serverA.stopServer(
                           "CWWKC1501W.*StatelessProgrammaticTimersBean", // Persistent executor defaultEJBPersistentTimerExecutor rolled back task due to failure ... The task is scheduled to retry after ...
                           "CWWKC1503W", // Persistent executor defaultEJBPersistentTimerExecutor rolled back task due to failure ... [no retry] OR timer rolls back for trying to run while server is stopping
                           "DSRA.*", "J2CA.*", "WTRN.*" // task running during server shutdown
        );
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
            // However, the timer was taken over by a different member which is likely continuing to claim executions of it.
            // It should be skipped silently on the first member without errors.
            Thread.sleep(2000);

            runTest(serverB, APP_NAME + "/FailoverTimersTestServlet", "testCancelStatelessTxSuspendedTimers&test=testProgrammaticTimerWithTxSuspendedFailsOverWhenAppStops[3]");

            // Also restart the server. This allows us to process any expected warning messages that are logged in response
            // to the application going away while its scheduled tasks remain.
            serverB.stopServer("CWWKC1556W", // Execution of tasks from application failoverTimersApp is deferred until the application and modules that scheduled the tasks are available.
                               "CWWKC1503W",
                               "DSRA.*", "J2CA.*", "WTRN.*" // task running during server shutdown
            );
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
            // However, the timer was taken over by a different member which is likely continuing to claim executions of it.
            // It should be skipped silently on the first member without errors.
            Thread.sleep(2000);

            // Also restart the server. This allows us to process any expected warning messages that are logged in response
            // to the application going away while its scheduled tasks remain.
            serverOnWhichToStopApp.stopServer(
                                              "CWWKC1556W", // Execution of tasks from application failoverTimersApp is deferred until the application and modules that scheduled the tasks are available.
                                              "CWWKC1503W", // timer not invoking due to server stop
                                              "DSRA.*", "J2CA.*", "WTRN.*" // transaction in progress across server stop
            );
        }
    }

    /**
     * Schedule a long-running timer, which exceeds the missedTaskThreshold by a significant amount.
     * Verify that timer is allowed to complete on the same server, and does not cause a failover due to its slowness.
     * This is not a recommended way of running. In general, a user should increase their missedTaskThreshold to
     * accommodate the length of their longest timers. However, if it does happen, this test helps to demonstrate
     * that Liberty deals with it gracefully, apart from the extra locking that is incurred by attempting an
     * unnecessary failover.
     */
    @Test
    public void testLongRunningTimerLooksLikeAMissedTimer() throws Exception {
        runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                "testScheduleStatelessTimer&timer=Long_Running_Timer_200_1200&initialDelayMS=200&intervalMS=1200&test=testLongRunningTimerLooksLikeAMissedTimer[1]");
        try {
            // Verify that the timer execution on server A (where it was scheduled) succeeds
            runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                    "testTimerFailover&timer=Long_Running_Timer_200_1200&server=" + SERVER_A_NAME + "&test=testLongRunningTimerLooksLikeAMissedTimer[2]");
        } finally {
            runTest(serverA, APP_NAME + "/FailoverTimersTestServlet",
                    "testCancelStatelessProgrammaticTimers&test=testLongRunningTimerLooksLikeAMissedTimer[3]");
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
        serverToStop.stopServer("CWWKC1503W",
                                "DSRA0302E", // can happen if timer tries to run while the server stops
                                "DSRA0304E" // can happen if timer tries to run while the server stops
        );

        String nameOfServerForFailover = serverToStop == serverA ? SERVER_B_NAME : SERVER_A_NAME;
        LibertyServer serverForFailover = serverToStop == serverA ? serverB : serverA;

        runTest(serverForFailover, APP_NAME + "/FailoverTimersTestServlet",
                "testTimerFailover&timer=AutomaticCountingSingletonTimer&server=" + nameOfServerForFailover + "&test=testTimerFailsOverWhenServerStops[2]");

        serverForFailover.stopServer("CWWKC1503W",
                                     "DSRA0302E", // can happen if timer tries to run while the server stops
                                     "DSRA0304E" // can happen if timer tries to run while the server stops
        );
    }
}