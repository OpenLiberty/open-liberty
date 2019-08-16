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
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import failover1serv.web.Failover1ServerTestServlet;

/**
 * This test bucket attempts to simulate failover of tasks.
 * For this oversimplified scenario, we don't actually have multiple servers, just multiple
 * persistent executor instances on a single server.
 * We can simulate an instance going down by removing it from the configuration.
 */
@RunWith(FATRunner.class)
public class Failover1ServerTest extends FATServletClient {
	private static final String APP_NAME = "failover1servApp";
	private static final Set<String> APP_NAMES = Collections.singleton(APP_NAME);

    private static ServerConfiguration originalConfig;

    @Server("com.ibm.ws.concurrent.persistent.fat.failover1serv")
    @TestServlet(servlet = Failover1ServerTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    private static final String TASK_ID_MESSAGE = "Task id is ";

    @BeforeClass
    public static void setUp() throws Exception {
        originalConfig = server.getServerConfiguration();
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "failover1serv.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * testMultipleInstancesCompeteToRunManyLateTasks - Have 3 instances available that could take over for running
     * several late tasks.
     */
    @Test
    public void testMultipleInstancesCompeteToRunManyLateTasks() throws Exception {
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
                    "&delayMS=1200&test=testMultipleInstancesCompeteToRunManyLateTasks[1]");
            int start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task A not found in servlet output: " + result);
            String taskIdA = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));
            taskIds.add(taskIdA);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=" +
                    (initialDelayMS - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialTimeNS)) +
                    "&delayMS=1200&test=testMultipleInstancesCompeteToRunManyLateTasks[2]");
            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task B not found in servlet output: " + result);
            String taskIdB = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));
            taskIds.add(taskIdB);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=" +
                    (initialDelayMS - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialTimeNS)) +
                    "&delayMS=1200&test=testMultipleInstancesCompeteToRunManyLateTasks[3]");
            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task C not found in servlet output: " + result);
            String taskIdC = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));
            taskIds.add(taskIdC);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=" +
                    (initialDelayMS - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialTimeNS)) +
                    "&delayMS=1200&test=testMultipleInstancesCompeteToRunManyLateTasks[4]");
            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task D not found in servlet output: " + result);
            String taskIdD = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));
            taskIds.add(taskIdD);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=" +
                    (initialDelayMS - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialTimeNS)) +
                    "&delayMS=1200&test=testMultipleInstancesCompeteToRunManyLateTasks[5]");
            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task E not found in servlet output: " + result);
            String taskIdE = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));
            taskIds.add(taskIdE);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=" +
                    (initialDelayMS - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialTimeNS)) +
                    "&delayMS=1200&test=testMultipleInstancesCompeteToRunManyLateTasks[6]");
            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of scheduled task F not found in servlet output: " + result);
            String taskIdF = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));
            taskIds.add(taskIdF);

            System.out.println("Scheduled tasks A: " + taskIdA + ", B: " + taskIdB + ", C: " + taskIdC + ", D: " + taskIdD + ", E: " + taskIdE + ", F:" + taskIdF);

            // Simulate failover by disabling the instance (persistentExec2) to which the tasks were scheduled
            // and allowing other instances to claim ownership
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor persistentExec2 = config.getPersistentExecutors().getById("persistentExec2");
            persistentExec2.setEnableTaskExecution("false");

            PersistentExecutor persistentExec3 = new PersistentExecutor();
            persistentExec3.setId("persistentExec3");
            persistentExec3.setPollInterval("2s");
            persistentExec3.setPollSize("4");
            persistentExec3.setExtraAttribute("lateTaskThreshold", "3s"); // TODO update simplicity object with proper setter
            config.getPersistentExecutors().add(persistentExec3);

            PersistentExecutor persistentExec4 = new PersistentExecutor();
            persistentExec4.setId("persistentExec4");
            persistentExec4.setPollInterval("2s");
            persistentExec4.setPollSize("4");
            persistentExec4.setExtraAttribute("lateTaskThreshold", "3s"); // TODO update simplicity object with proper setter
            config.getPersistentExecutors().add(persistentExec4);

            PersistentExecutor persistentExec5 = new PersistentExecutor();
            persistentExec5.setId("persistentExec5");
            persistentExec5.setPollInterval("2s");
            persistentExec5.setPollSize("4");
            persistentExec5.setExtraAttribute("lateTaskThreshold", "3s"); // TODO update simplicity object with proper setter
            config.getPersistentExecutors().add(persistentExec5);

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
            try {
                runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                        "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC +
                        "&taskId=" + taskIdD + "&taskId=" + taskIdE + "&taskId=" + taskIdF +
                        "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunManyLateTasks[7]");
            } finally {
                // restore original configuration
                server.setMarkToEndOfLog();
                server.updateServerConfiguration(originalConfig);
                server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
            }

            // Verify one more failover,
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC +
                    "&taskId=" + taskIdD + "&taskId=" + taskIdE + "&taskId=" + taskIdF +
                    "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunManyLateTasks[8]");
        } finally {
            // always cancel the tasks
            int count = 0;
            for (String taskId : taskIds)
                runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                        "testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunManyLateTasks[9." + (++count) + "]");
        }
    }

    /**
     * testMultipleInstancesCompeteToRunOneLateTask - Have 3 instances available that could take over for running
     * a single late task. Exactly one of the instances should take over and run it.
     */
    @Test
    public void testMultipleInstancesCompeteToRunOneLateTask() throws Exception {
        // Schedule on the only instance that is currently able to run tasks
        StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=1300&test=testMultipleInstancesCompeteToRunOneLateTask[1]");

        int start = result.indexOf(TASK_ID_MESSAGE);
        if (start < 0)
            fail("Task id of scheduled task not found in servlet output: " + result);
        String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

        System.out.println("Scheduled task " + taskId);

        try {
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskStarted&taskId=" + taskId + "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunOneLateTask[2]");

            // Simulate failover by disabling the instance (persistentExec2) to which the task was scheduled
            // and allowing other instances to claim ownership of the task
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor persistentExec2 = config.getPersistentExecutors().getById("persistentExec2");
            persistentExec2.setEnableTaskExecution("false");

            PersistentExecutor persistentExec3 = new PersistentExecutor();
            persistentExec3.setId("persistentExec3");
            persistentExec3.setPollInterval("1s500ms");
            persistentExec3.setExtraAttribute("lateTaskThreshold", "2s"); // TODO update simplicity object with proper setter
            config.getPersistentExecutors().add(persistentExec3);

            PersistentExecutor persistentExec4 = new PersistentExecutor();
            persistentExec4.setId("persistentExec4");
            persistentExec4.setPollInterval("1s500ms");
            persistentExec4.setExtraAttribute("lateTaskThreshold", "2s"); // TODO update simplicity object with proper setter
            config.getPersistentExecutors().add(persistentExec4);

            PersistentExecutor persistentExec5 = new PersistentExecutor();
            persistentExec5.setId("persistentExec5");
            persistentExec5.setPollInterval("1s500ms");
            persistentExec5.setExtraAttribute("lateTaskThreshold", "2s"); // TODO update simplicity object with proper setter
            config.getPersistentExecutors().add(persistentExec5);

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
            try {
                runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                        "testTaskIsRunning&taskId=" + taskId + "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunOneLateTask[3]");
            } finally {
                // restore original configuration
                server.setMarkToEndOfLog();
                server.updateServerConfiguration(originalConfig);
                server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
            }

            // Verify one more failover,
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskIsRunning&taskId=" + taskId + "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunOneLateTask[4]");
        } finally {
            // always cancel the task
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testMultipleInstancesCompeteToRunOneLateTask[5]");
        }
    }

    /**
     * testScheduleOnOneServerRunOnAnother - Schedule a task on an instance that cannot run tasks. Verify that another instance takes over and runs it.
     */
    @Test
    public void testScheduleOnOneServerRunOnAnother() throws Exception {
        // Schedule on the instance that cannot run tasks
        StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                "testScheduleOneTimeTask&jndiName=persistent/exec1&initialDelayMS=0&test=testScheduleOnOneServerRunOnAnother[1]");

        int start = result.indexOf(TASK_ID_MESSAGE);
        if (start < 0)
            fail("Task id of scheduled task not found in servlet output: " + result);
        String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

        System.out.println("Scheduled task " + taskId);

        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                "testTaskCompleted&taskId=" + taskId + "&expectedResult=1&jndiName=persistent/exec1&test=testScheduleOnOneServerRunOnAnother[2]");
    }

    /**
     * testScheduleOnOneServerRunOnAnotherThenBackToOriginal - Schedule a task on an instance that cannot run tasks.
     * Verify that another instance takes over and runs it. Then prevent that instance from running tasks. Enable the ability
     * to run tasks on the first instance and verify it fails over back to the original.
     */
    @Test
    public void testScheduleOnOneServerRunOnAnotherThenBackToOriginal() throws Exception {
        // Schedule on the instance that cannot run tasks
        StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                "testScheduleRepeatingTask&jndiName=persistent/exec1&initialDelayMS=0&delayMS=1000&test=testScheduleOnOneServerRunOnAnotherThenBackToOriginal[1]");

        int start = result.indexOf(TASK_ID_MESSAGE);
        if (start < 0)
            fail("Task id of scheduled task not found in servlet output: " + result);
        String taskId = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

        System.out.println("Scheduled task " + taskId);

        try {
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskStarted&taskId=" + taskId + "&jndiName=persistent/exec1&test=testScheduleOnOneServerRunOnAnotherThenBackToOriginal[2]");

            // Simulate failover by disabling the instance (persistentExec2) to which the task was scheduled
            // and allowing the other instance to run tasks
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor persistentExec2 = config.getPersistentExecutors().getById("persistentExec2");
            persistentExec2.setEnableTaskExecution("false");

            PersistentExecutor persistentExec1 = config.getPersistentExecutors().getById("persistentExec1");
            persistentExec1.setEnableTaskExecution("true");
            persistentExec1.setInitialPollDelay("200ms");
            persistentExec1.setPollInterval("1s500ms");
            persistentExec1.setExtraAttribute("lateTaskThreshold", "2s"); // TODO update simplicity object with proper setter
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
            try {
                runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                        "testTaskIsRunning&taskId=" + taskId + "&jndiName=persistent/exec1&test=testScheduleOnOneServerRunOnAnotherThenBackToOriginal[3]");
            } finally {
                // restore original configuration
                server.setMarkToEndOfLog();
                server.updateServerConfiguration(originalConfig);
                server.waitForConfigUpdateInLogUsingMark(APP_NAMES);
            }

            // Verify one more failover,
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskIsRunning&taskId=" + taskId + "&jndiName=persistent/exec1&test=testScheduleOnOneServerRunOnAnotherThenBackToOriginal[4]");
        } finally {
            // always cancel the task
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testCancelTask&taskId=" + taskId + "&jndiName=persistent/exec1&test=testScheduleOnOneServerRunOnAnotherThenBackToOriginal[5]");
        }
    }
}