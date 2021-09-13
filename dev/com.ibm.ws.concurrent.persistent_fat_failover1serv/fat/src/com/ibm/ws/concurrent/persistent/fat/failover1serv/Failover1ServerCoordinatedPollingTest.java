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
package com.ibm.ws.concurrent.persistent.fat.failover1serv;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import failover1serv.web.Failover1ServerTestServlet;

/**
 * This test bucket attempts to simulate failover of tasks.
 * For this oversimplified scenario, we don't actually have multiple servers, just multiple
 * persistent executor instances on a single server.
 * We can simulate an instance going down by removing it from the configuration.
 * All of the persistent executors have coordination of polling enabled.
 */
@RunWith(FATRunner.class)
public class Failover1ServerCoordinatedPollingTest extends FATServletClient {
	private static final String APP_NAME = "failover1servApp";
	private static final Set<String> APP_NAMES = Collections.singleton(APP_NAME);

    private static ServerConfiguration originalConfig;
    private static ServerConfiguration savedConfig;

    @Server("com.ibm.ws.concurrent.persistent.fat.failover1serv")
    @TestServlet(servlet = Failover1ServerTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    private static final String TASK_ID_MESSAGE = "Task id is ";

    @BeforeClass
    public static void setUp() throws Exception {
        originalConfig = server.getServerConfiguration();
        savedConfig = originalConfig.clone();
        savedConfig.getPersistentExecutors().getById("persistentExec1").setExtraAttribute("pollingCoordination.for.test.use.only", "true");
        savedConfig.getPersistentExecutors().getById("persistentExec2").setExtraAttribute("pollingCoordination.for.test.use.only", "true");

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "failover1serv.web");

        server.deleteDirectoryFromLibertyInstallRoot("usr/shared/resources/data/failover1db");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer(
                    "DSRA0302E", "DSRA0304E" // transaction times out and rolls back, persistent executor will retry
                    );
        } finally {
            server.updateServerConfiguration(originalConfig);
        }
    }

    /**
     * testMultipleInstancesCompeteToRunManyLateTasksPC - Have 3 instances available that could take over for running
     * several late tasks. Run this test with polling coordination across servers enabled.
     */
    @AllowedFFDC(value = {
            "javax.transaction.RollbackException", // transaction rolled back due to timeout
            "javax.transaction.xa.XAException", // rollback/abort path
            "javax.persistence.PersistenceException", // caused by RollbackException
            "javax.persistence.ResourceException", // connection error event on retry
            "java.lang.IllegalStateException", // for EclipseLink retry after connection has been aborted due to rollback
            "java.lang.NullPointerException", // can happen when task execution overlaps removal of executor
            "java.sql.SQLException", // closed, likely due to config update
            "org.apache.derby.client.am.XaException" // no connection, likely due to config update
    })
    @Mode(TestMode.FULL)
    @Test
    public void testMultipleInstancesCompeteToRunManyLateTasksPC() throws Exception {
        // Schedule on the only instance that is currently able to run tasks, attempting to time
        // all tasks to start at around the same point in time, making it more likely that they all
        // become late around the same point in time.
        final long initialDelayMS = 6000;
        final long initialTimeNS = System.nanoTime();

        List<String> taskIds = new ArrayList<String>();
        try {
            StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=" +
                    initialDelayMS +
                    "&delayMS=1200&test=testMultipleInstancesCompeteToRunManyLateTasksPC[1]");
            int start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task A not found in servlet output: " + result);
            String taskIdA = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));
            taskIds.add(taskIdA);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=" +
                    (initialDelayMS - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialTimeNS)) +
                    "&delayMS=1200&test=testMultipleInstancesCompeteToRunManyLateTasksPC[2]");
            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task B not found in servlet output: " + result);
            String taskIdB = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));
            taskIds.add(taskIdB);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=" +
                    (initialDelayMS - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialTimeNS)) +
                    "&delayMS=1200&test=testMultipleInstancesCompeteToRunManyLateTasksPC[3]");
            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task C not found in servlet output: " + result);
            String taskIdC = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));
            taskIds.add(taskIdC);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=" +
                    (initialDelayMS - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialTimeNS)) +
                    "&delayMS=1200&test=testMultipleInstancesCompeteToRunManyLateTasksPC[4]");
            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task D not found in servlet output: " + result);
            String taskIdD = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));
            taskIds.add(taskIdD);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=" +
                    (initialDelayMS - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialTimeNS)) +
                    "&delayMS=1200&test=testMultipleInstancesCompeteToRunManyLateTasksPC[5]");
            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task E not found in servlet output: " + result);
            String taskIdE = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));
            taskIds.add(taskIdE);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=" +
                    (initialDelayMS - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialTimeNS)) +
                    "&delayMS=1200&test=testMultipleInstancesCompeteToRunManyLateTasksPC[6]");
            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task F not found in servlet output: " + result);
            String taskIdF = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));
            taskIds.add(taskIdF);

            System.out.println("Scheduled tasks A: " + taskIdA + ", B: " + taskIdB + ", C: " + taskIdC + ", D: " + taskIdD + ", E: " + taskIdE + ", F:" + taskIdF);

            // Simulate failover by disabling the instance (persistentExec2) to which the tasks were scheduled
            // and allowing other instances to claim ownership
            ServerConfiguration config = savedConfig.clone();
            PersistentExecutor persistentExec2 = config.getPersistentExecutors().getById("persistentExec2");
            persistentExec2.setEnableTaskExecution("false");

            PersistentExecutor persistentExec3 = new PersistentExecutor();
            persistentExec3.setId("persistentExec3");
            persistentExec3.setExtraAttribute("pollingCoordination.for.test.use.only", "true");
            persistentExec3.setPollInterval("5s");
            persistentExec3.setPollSize("4");
            persistentExec3.setMissedTaskThreshold("8s");
            persistentExec3.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
            config.getPersistentExecutors().add(persistentExec3);

            PersistentExecutor persistentExec4 = new PersistentExecutor();
            persistentExec4.setId("persistentExec4");
            persistentExec4.setExtraAttribute("pollingCoordination.for.test.use.only", "true");
            persistentExec4.setPollInterval("5s");
            persistentExec4.setPollSize("4");
            persistentExec4.setMissedTaskThreshold("8s");
            persistentExec4.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
            config.getPersistentExecutors().add(persistentExec4);

            PersistentExecutor persistentExec5 = new PersistentExecutor();
            persistentExec5.setId("persistentExec5");
            persistentExec5.setExtraAttribute("pollingCoordination.for.test.use.only", "true");
            persistentExec5.setPollInterval("5s");
            persistentExec5.setPollSize("4");
            persistentExec5.setMissedTaskThreshold("8s");
            persistentExec5.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
            config.getPersistentExecutors().add(persistentExec5);

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
            try {
                runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                        "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC +
                        "&taskId=" + taskIdD + "&taskId=" + taskIdE + "&taskId=" + taskIdF +
                        "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunManyLateTasksPC[7]");
            } finally {
                // restore original configuration
                server.setMarkToEndOfLog();
                server.updateServerConfiguration(savedConfig);
                server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
            }

            // Verify one more failover,
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC +
                    "&taskId=" + taskIdD + "&taskId=" + taskIdE + "&taskId=" + taskIdF +
                    "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunManyLateTasksPC[8]");
        } finally {
            try {
                // always cancel the tasks
                int count = 0;
                for (String taskId : taskIds)
                    runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                            "testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunManyLateTasksPC[9." + (++count) + "]");
            } finally {
                // Stop the server here so that expected warnings/errors can be processed by this test
                server.stopServer(
                        "CWWKC1500W.*", // Rolled back task [id or name]. The task is scheduled to retry after ...
                        "CWWKC1501W.*", // Rolled back task [id or name] due to failure ... The task is scheduled to retry after ...
                        "CWWKC1502W.*", // Rolled back task [id or name]
                        "CWWKC1503W.*", // Rolled back task [id or name] due to failure ...
                        "DSRA*", // various errors possible due to rollback or usage during shutdown
                        "J2CA*", // various errors possible due to rollback or usage during shutdown
                        "WTRN.*" // commit fails - no connection after config update
                        );
                server.startServer();
            }
        }
    }

    /**
     * testMultipleInstancesCompeteToRunOneLateTaskPC - Have 3 instances available that could take over for running
     * a single late task. Exactly one of the instances should take over and run it. Run this test with polling coordination
     * enabled for all servers.
     */
    // If scheduled task execution happens to be attempted while persistentExecutors are being removed, it might fail.
    // This is expected. After the configuration update completes, tasks will be able to run again successfully
    // and pass the test.
    @AllowedFFDC({
        "java.lang.IllegalStateException",
        "java.lang.NullPointerException",
        "java.sql.SQLException", // closed, likely due to config update
        "org.apache.derby.client.am.XaException" // no connection, likely due to config update
        })
    @Test
    public void testMultipleInstancesCompeteToRunOneLateTaskPC() throws Exception {
        // Schedule on the only instance that is currently able to run tasks
        StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=1300&test=testMultipleInstancesCompeteToRunOneLateTaskPC[1]");

        int start = result.indexOf(TASK_ID_MESSAGE);
        if (start < 0)
            fail("Task id of scheduled task not found in servlet output: " + result);
        String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

        System.out.println("Scheduled task " + taskId);

        try {
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskStarted&taskId=" + taskId + "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunOneLateTaskPC[2]");

            // Simulate failover by disabling the instance (persistentExec2) to which the task was scheduled
            // and allowing other instances to claim ownership of the task
            ServerConfiguration config = savedConfig.clone();
            PersistentExecutor persistentExec2 = config.getPersistentExecutors().getById("persistentExec2");
            persistentExec2.setEnableTaskExecution("false");

            PersistentExecutor persistentExec3 = new PersistentExecutor();
            persistentExec3.setId("persistentExec3");
            persistentExec3.setExtraAttribute("pollingCoordination.for.test.use.only", "true");
            persistentExec3.setPollInterval("4s500ms");
            persistentExec3.setMissedTaskThreshold("7s");
            persistentExec3.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
            config.getPersistentExecutors().add(persistentExec3);

            PersistentExecutor persistentExec4 = new PersistentExecutor();
            persistentExec4.setId("persistentExec4");
            persistentExec4.setExtraAttribute("pollingCoordination.for.test.use.only", "true");
            persistentExec4.setPollInterval("4s500ms");
            persistentExec4.setMissedTaskThreshold("7s");
            persistentExec4.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
            config.getPersistentExecutors().add(persistentExec4);

            PersistentExecutor persistentExec5 = new PersistentExecutor();
            persistentExec5.setId("persistentExec5");
            persistentExec5.setExtraAttribute("pollingCoordination.for.test.use.only", "true");
            persistentExec5.setPollInterval("4s500ms");
            persistentExec5.setMissedTaskThreshold("7s");
            persistentExec5.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
            config.getPersistentExecutors().add(persistentExec5);

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
            try {
                runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                        "testTaskIsRunning&taskId=" + taskId + "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunOneLateTaskPC[3]");
            } finally {
                // restore original configuration
                server.setMarkToEndOfLog();
                server.updateServerConfiguration(savedConfig);
                server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
            }

            // Verify one more failover,
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskIsRunning&taskId=" + taskId + "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunOneLateTaskPC[4]");
        } finally {
            // always cancel the task
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunOneLateTaskPC[5]");
        }
    }

    /**
     * testScheduleOnOneServerRunOnAnotherPC - Schedule a task on an instance that cannot run tasks. Verify that another instance takes over and runs it.
     * In this test, the missedTaskThreshold is configured on the instance that cannot run tasks. Run this test with coordination of polling enabled across servers.
     */
    @Test
    public void testScheduleOnOneServerRunOnAnotherPC() throws Exception {
        // Schedule on the instance that cannot run tasks
        StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                "testScheduleOneTimeTask&jndiName=persistent/exec1&initialDelayMS=0&test=testScheduleOnOneServerRunOnAnotherPC[1]");

        int start = result.indexOf(TASK_ID_MESSAGE);
        if (start < 0)
            fail("Task id of scheduled task not found in servlet output: " + result);
        String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

        System.out.println("Scheduled task " + taskId);

        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                "testTaskCompleted&taskId=" + taskId + "&expectedResult=1&jndiName=persistent/exec1&test=testScheduleOnOneServerRunOnAnotherPC[2]");
    }

    /**
     * testScheduleOnOneServerRunOnAnotherThenBackToOriginalPC - Schedule a task on an instance that cannot run tasks.
     * Verify that another instance takes over and runs it. Then prevent that instance from running tasks. Enable the ability
     * to run tasks on the first instance and verify it fails over back to the original. Run this test with coordination of polling
     * enabled across servers.
     */
    @Mode(TestMode.FULL)
    @Test
    public void testScheduleOnOneServerRunOnAnotherThenBackToOriginalPC() throws Exception {
        // Schedule on the instance that cannot run tasks
        StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                "testScheduleRepeatingTask&jndiName=persistent/exec1&initialDelayMS=0&delayMS=1000&test=testScheduleOnOneServerRunOnAnotherThenBackToOriginalPC[1]");

        int start = result.indexOf(TASK_ID_MESSAGE);
        if (start < 0)
            fail("Task id of scheduled task not found in servlet output: " + result);
        String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

        System.out.println("Scheduled task " + taskId);

        try {
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskStarted&taskId=" + taskId + "&jndiName=persistent/exec1&test=testScheduleOnOneServerRunOnAnotherThenBackToOriginalPC[2]");

            // Simulate failover by disabling the instance (persistentExec2) to which the task was scheduled
            // and allowing the other instance to run tasks
            ServerConfiguration config = savedConfig.clone();
            PersistentExecutor persistentExec2 = config.getPersistentExecutors().getById("persistentExec2");
            persistentExec2.setEnableTaskExecution("false");

            PersistentExecutor persistentExec1 = config.getPersistentExecutors().getById("persistentExec1");
            persistentExec1.setEnableTaskExecution("true");
            persistentExec1.setInitialPollDelay("200ms");
            persistentExec1.setPollInterval("4s500ms");
            persistentExec1.setMissedTaskThreshold("7s");
            persistentExec1.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
            try {
                runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                        "testTaskIsRunning&taskId=" + taskId + "&jndiName=persistent/exec1&test=testScheduleOnOneServerRunOnAnotherThenBackToOriginalPC[3]");
            } finally {
                // restore original configuration
                server.setMarkToEndOfLog();
                server.updateServerConfiguration(savedConfig);
                server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
            }

            // Verify one more failover,
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskIsRunning&taskId=" + taskId + "&jndiName=persistent/exec1&test=testScheduleOnOneServerRunOnAnotherThenBackToOriginalPC[4]");
        } finally {
            // always cancel the task
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testScheduleOnOneServerRunOnAnotherThenBackToOriginalPC[5]");
        }
    }

    /**
     * testScheduleThenRunOnDifferentServerPC - Schedule a task using an instance that cannot run tasks.
     * It will go ahead and schedule the task without any claim to run it, allowing another instance to take over.
     * In this test, the missedTaskThreshold is not configured on the instance that cannot run tasks.
     * Run this test with coordination of polling enabled across servers.
     */
    @Test
    public void testScheduleThenRunOnDifferentServerPC() throws Exception {
        ServerConfiguration config = savedConfig.clone();
        PersistentExecutor persistentExec1 = config.getPersistentExecutors().getById("persistentExec1");
        persistentExec1.setMissedTaskThreshold(null);
        PersistentExecutor persistentExec2 = config.getPersistentExecutors().getById("persistentExec2");
        persistentExec2.setMissedTaskThreshold("2h"); // even though this value is unreasonably long, it does not impact the ability to take over an unassigned task

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
        try {
            // Schedule on the instance that cannot run tasks
            StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleOneTimeTask&jndiName=persistent/exec1&initialDelayMS=0&test=testScheduleThenRunOnDifferentServerPC[1]");

            int start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task not found in servlet output: " + result);
            String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled task " + taskId);

            boolean completed = false;
            try {
                runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                        "testTaskCompleted&taskId=" + taskId + "&expectedResult=1&jndiName=persistent/exec1&test=testScheduleThenRunOnDifferentServerPC[2]");
                completed = true;
            } finally {
                if (!completed)
                    runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                            "testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testScheduleThenRunOnDifferentServerPC[3]");
            }
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(savedConfig);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
        }
    }
}