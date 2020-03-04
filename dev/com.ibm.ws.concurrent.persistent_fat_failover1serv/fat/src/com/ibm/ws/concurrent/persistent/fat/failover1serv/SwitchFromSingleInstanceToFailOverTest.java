/*******************************************************************************
 * Copyright (c) 2019,2020 IBM Corporation and others.
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

import java.util.Collections;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import failover1serv.web.Failover1ServerTestServlet;

/**
 * This test bucket covers scenarios where the user starts out with fail over disabled and switches to
 * enable fail over. There are a variety of ways they might do this, which are described in more detail
 * in the comments for each individual test.
 */
@RunWith(FATRunner.class)
public class SwitchFromSingleInstanceToFailOverTest extends FATServletClient {
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

        server.deleteDirectoryFromLibertyInstallRoot("usr/shared/resources/data/failover1db");
    }

    @After
    public void tearDownPerTest() throws Exception {
        try {
            if (server.isStarted())
                server.stopServer();
        } finally {
            server.updateServerConfiguration(originalConfig);
        }        
    }

    /**
     * testEnableFailOverWhileServerIsRunning - Schedules tasks on an instance where fail over is not enabled.
     * Enables fail over while the server is still running. Then verifies that the previous, as well as new, tasks run.
     */
    @Test
    public void testEnableFailOverWhileServerIsRunning() throws Exception {
        ServerConfiguration config = originalConfig.clone();
        PersistentExecutor persistentExec1 = config.getPersistentExecutors().getById("persistentExec1");
        persistentExec1.setMissedTaskThreshold(null);
        PersistentExecutor persistentExec2 = config.getPersistentExecutors().getById("persistentExec2");
        persistentExec2.setMissedTaskThreshold(null);

        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + "-1");
        String taskIdA = null, taskIdB = null, taskIdC = null, taskIdD = null;
        boolean successful = false;
        try {
            // Schedule on the instance that cannot run tasks
            StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleOneTimeTask&jndiName=persistent/exec1&initialDelayMS=0&test=testEnableFailOverWhileServerIsRunning[1]");

            int start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of first scheduled task not found in servlet output: " + result);
            taskIdA = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled first task " + taskIdA);

            // Schedule on the instance that can run tasks
            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=600&test=testEnableFailOverWhileServerIsRunning[2]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of second scheduled task not found in servlet output: " + result);
            taskIdB = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled second task " + taskIdB);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=500&test=testEnableFailOverWhileServerIsRunning[3]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of third scheduled task not found in servlet output: " + result);
            taskIdC = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled third task " + taskIdC);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdB + "&taskId=" + taskIdC + "&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsRunning[4]");

            // Enable fail over
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES);

            // Verify that tasks still run
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdB + "&taskId=" + taskIdC + "&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsRunning[5]");

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskCompleted&taskId=" + taskIdA + "&expectedResult=1&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsRunning[6]");

            // Schedule another task and verify that it runs
            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleOneTimeTask&jndiName=persistent/exec2&initialDelayMS=0&test=testEnableFailOverWhileServerIsRunning[7]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of fourth scheduled task not found in servlet output: " + result);
            taskIdD = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled fourth task " + taskIdD);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskCompleted&taskId=" + taskIdD + "&expectedResult=1&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsRunning[8]");
            successful = true;
        } finally {
            if (server.isStarted())
                try {
                    if (taskIdD != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdD + "&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsRunning[9]");

                    if (taskIdC != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdC + "&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsRunning[10]");

                    if (taskIdB != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdB + "&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsRunning[11]");

                    if (taskIdA != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdA + "&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsRunning[12]");
                } catch (Error | Exception x) {
                    if (successful)
                        throw x;
                }
        }
    }

    /**
     * testEnableFailOverWhileServerIsStopped - Schedules tasks on an instance where fail over is not enabled.
     * Stops the server and enables fail over. Starts up the server and verifies that the previous, as well as new, tasks run.
     */
    @Test
    public void testEnableFailOverWhileServerIsStopped() throws Exception {
        ServerConfiguration config = originalConfig.clone();
        PersistentExecutor persistentExec1 = config.getPersistentExecutors().getById("persistentExec1");
        persistentExec1.setMissedTaskThreshold(null);
        PersistentExecutor persistentExec2 = config.getPersistentExecutors().getById("persistentExec2");
        persistentExec2.setMissedTaskThreshold(null);

        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + "-1");
        String taskIdA = null, taskIdB = null, taskIdC = null, taskIdD = null;
        boolean successful = false;
        try {
            // Schedule on the instance that cannot run tasks
            StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleOneTimeTask&jndiName=persistent/exec1&initialDelayMS=0&test=testEnableFailOverWhileServerIsStopped[1]");

            int start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of first scheduled task not found in servlet output: " + result);
            taskIdA = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled first task " + taskIdA);

            // Schedule on the instance that can run tasks
            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=1600&test=testEnableFailOverWhileServerIsStopped[2]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of second scheduled task not found in servlet output: " + result);
            taskIdB = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled second task " + taskIdB);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=1500&test=testEnableFailOverWhileServerIsStopped[3]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of third scheduled task not found in servlet output: " + result);
            taskIdC = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled third task " + taskIdC);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdB + "&taskId=" + taskIdC + "&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsStopped[4]");

            server.stopServer(); // this might need to allow for expected warnings if the server shuts down while a task is running

            // Enable fail over
            server.updateServerConfiguration(originalConfig);
            server.startServer(testName.getMethodName() + "-2");

            // Verify that tasks still run
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdB + "&taskId=" + taskIdC + "&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsStopped[5]");

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskCompleted&taskId=" + taskIdA + "&expectedResult=1&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsStopped[6]");

            // Schedule another task and verify that it runs
            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleOneTimeTask&jndiName=persistent/exec2&initialDelayMS=0&test=testEnableFailOverWhileServerIsStopped[7]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of fourth scheduled task not found in servlet output: " + result);
            taskIdD = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled fourth task " + taskIdD);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskCompleted&taskId=" + taskIdD + "&expectedResult=1&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsStopped[8]");
            successful = true;
        } finally {
            if (server.isStarted())
                try {
                    if (taskIdD != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdD + "&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsStopped[9]");

                    if (taskIdC != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdC + "&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsStopped[10]");

                    if (taskIdB != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdB + "&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsStopped[11]");

                    if (taskIdA != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdA + "&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsStopped[12]");
                } catch (Error | Exception x) {
                    if (successful)
                        throw x;
                }
        }
    }

    /**
     * testNewFailOverEnabledInstance - Schedules tasks on an instance where fail over is not enabled.
     * Stop the server and create a new instance with fail over enabled. Verify that the previous, as well as new, tasks run.
     * Stop the server and remove the old instance. Verify that the previous, as well as new, tasks run.
     * This is not a recommended way of enabling fail over, because it leaves instances with and without fail over running at
     * the same time. But it is being tested here in case anyone tries it.
     */
    @Test
    public void testNewFailOverEnabledInstance() throws Exception {
        ServerConfiguration config = originalConfig.clone();
        config.getPersistentExecutors().removeById("persistentExec1");
        PersistentExecutor persistentExec = config.getPersistentExecutors().getById("persistentExec2");
        String missedTaskThreshold = persistentExec.getMissedTaskThreshold();
        persistentExec.setMissedTaskThreshold(null);

        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + "-1");
        String taskIdA = null, taskIdB = null, taskIdC = null, taskIdD = null;
        boolean successful = false;
        try {
            // Schedule on original instance where fail over is not enabled
            StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=1555&test=testNewFailOverEnabledInstance[1]");

            int start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of first scheduled task not found in servlet output: " + result);
            taskIdA = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled first task " + taskIdA);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=1556&test=testNewFailOverEnabledInstance[2]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of second scheduled task not found in servlet output: " + result);
            taskIdB = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled second task " + taskIdB);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/exec2&test=testNewFailOverEnabledInstance[3]");

            server.stopServer(); // this might need to allow for expected warnings if the server shuts down while a task is running

            // Enable fail over on a new instance
            persistentExec = (PersistentExecutor) persistentExec.clone();
            persistentExec.setId("persistentExec2FE");
            persistentExec.setJndiName("persistent/exec2FE");
            persistentExec.setMissedTaskThreshold(missedTaskThreshold);
            config.getPersistentExecutors().add(persistentExec);
            server.updateServerConfiguration(config);
            server.startServer(testName.getMethodName() + "-2");

            // Verify that tasks still run
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/exec2FE&test=testNewFailOverEnabledInstance[4]");

            // Schedule another task
            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2FE&initialDelayMS=0&delayMS=1557&test=testNewFailOverEnabledInstance[5]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of third scheduled task not found in servlet output: " + result);
            taskIdC = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled third task " + taskIdC);

            server.stopServer(); // this might need to allow for expected warnings if the server shuts down while a task is running

            // Remove the old instance
            config.getPersistentExecutors().removeById("persistentExec2");
            server.updateServerConfiguration(config);
            server.startServer(testName.getMethodName() + "-3");

            // Verify that tasks still run
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC + "&jndiName=persistent/exec2FE&test=testNewFailOverEnabledInstance[6]");

            // Schedule another task and verify that it runs
            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleOneTimeTask&jndiName=persistent/exec2FE&initialDelayMS=0&test=testNewFailOverEnabledInstance[7]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of fourth scheduled task not found in servlet output: " + result);
            taskIdD = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled fourth task " + taskIdD);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskCompleted&taskId=" + taskIdD + "&expectedResult=1&jndiName=persistent/exec2FE&test=testNewFailOverEnabledInstance[8]");
            successful = true;
        } finally {
            if (server.isStarted())
                try {
                    if (taskIdD != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdD + "&jndiName=" + persistentExec.getJndiName() + "&test=testReplaceWithNewFailOverEnabledInstance[9]");

                    if (taskIdC != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdC + "&jndiName=" + persistentExec.getJndiName() + "&test=testReplaceWithNewFailOverEnabledInstance[10]");

                    if (taskIdB != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdB + "&jndiName=" + persistentExec.getJndiName() + "&test=testReplaceWithNewFailOverEnabledInstance[11]");

                    if (taskIdA != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdA + "&jndiName=" + persistentExec.getJndiName() + "&test=testReplaceWithNewFailOverEnabledInstance[12]");
                } catch (Error | Exception x) {
                    if (successful)
                        throw x;
                }
        }
    }

    /**
     * testNewFailOverEnabledInstanceWhileServerIsRunning - Schedules tasks on an instance where fail over is not enabled.
     * While the server is running, create a new instance with fail over enabled. Verify that the previous, as well as new, tasks run.
     * Then remove the old instance. Verify that the previous, as well as new, tasks run.
     * This is not a recommended way of enabling fail over, because it leaves instances with and without fail over running at
     * the same time. But it is being tested here in case anyone tries it.
     */
    @Test
    public void testNewFailOverEnabledInstanceWhileServerIsRunning() throws Exception {
        ServerConfiguration config = originalConfig.clone();
        config.getPersistentExecutors().removeById("persistentExec1");
        PersistentExecutor persistentExec = config.getPersistentExecutors().getById("persistentExec2");
        String missedTaskThreshold = persistentExec.getMissedTaskThreshold();
        persistentExec.setMissedTaskThreshold(null);

        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + "-1");
        String taskIdA = null, taskIdB = null, taskIdC = null, taskIdD = null;
        boolean successful = false;
        try {
            // Schedule on original instance where fail over is not enabled
            StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=1900&test=testNewFailOverEnabledInstanceWhileServerIsRunning[1]");

            int start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of first scheduled task not found in servlet output: " + result);
            taskIdA = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled first task " + taskIdA);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=1925&test=testNewFailOverEnabledInstanceWhileServerIsRunning[2]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of second scheduled task not found in servlet output: " + result);
            taskIdB = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled second task " + taskIdB);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/exec2&test=testNewFailOverEnabledInstanceWhileServerIsRunning[3]");

            // Enable fail over on a new instance
            persistentExec = (PersistentExecutor) persistentExec.clone();
            persistentExec.setId("persistentExec3FE");
            persistentExec.setJndiName("persistent/exec3FE");
            persistentExec.setMissedTaskThreshold(missedTaskThreshold);
            config.getPersistentExecutors().add(persistentExec);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES);

            // Verify that tasks still run
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/exec3FE&test=testNewFailOverEnabledInstanceWhileServerIsRunning[4]");

            // Schedule another task
            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec3FE&initialDelayMS=0&delayMS=1950&test=testNewFailOverEnabledInstanceWhileServerIsRunning[5]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of third scheduled task not found in servlet output: " + result);
            taskIdC = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled third task " + taskIdC);

            // Remove the old instance
            config.getPersistentExecutors().removeById("persistentExec2");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES);

            // Verify that tasks still run
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC + "&jndiName=persistent/exec3FE&test=testNewFailOverEnabledInstanceWhileServerIsRunning[6]");

            // Schedule another task and verify that it runs
            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleOneTimeTask&jndiName=persistent/exec3FE&initialDelayMS=0&test=testNewFailOverEnabledInstanceWhileServerIsRunning[7]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of fourth scheduled task not found in servlet output: " + result);
            taskIdD = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled fourth task " + taskIdD);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskCompleted&taskId=" + taskIdD + "&expectedResult=1&jndiName=persistent/exec3FE&test=testNewFailOverEnabledInstanceWhileServerIsRunning[8]");
            successful = true;
        } finally {
            if (server.isStarted())
                try {
                    if (taskIdD != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdD + "&jndiName=" + persistentExec.getJndiName() + "&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[9]");

                    if (taskIdC != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdC + "&jndiName=" + persistentExec.getJndiName() + "&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[10]");

                    if (taskIdB != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdB + "&jndiName=" + persistentExec.getJndiName() + "&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[11]");

                    if (taskIdA != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdA + "&jndiName=" + persistentExec.getJndiName() + "&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[12]");
                } catch (Error | Exception x) {
                    if (successful)
                        throw x;
                }
        }
    }

    /**
     * testRemoveFailOverEnablementWhileServerIsRunning - Schedules tasks on an instance where fail over is enabled.
     * While the server is still running, disables fail over on the same instance. New tasks can be scheduled
     * on the instance and should run there, but those that were submitted previously will not be recognized unless
     * some operations are performed to manually transfer them. Users should not do this. This test is only written
     * to experiment with what would happen and explore how to cope with it.
     */
    @Test
    public void testRemoveFailOverEnablementWhileServerIsRunning() throws Exception {
        // start with fail over enabled
        ServerConfiguration config = originalConfig.clone();
        ConfigElementList<PersistentExecutor> persistentExecutors = config.getPersistentExecutors();
        PersistentExecutor persistentExecRFR = (PersistentExecutor) persistentExecutors.getById("persistentExec2").clone();
        persistentExecRFR.setId("persistentExecRFR");
        persistentExecRFR.setJndiName("persistent/execRFR");
        persistentExecutors.add(persistentExecRFR);
        persistentExecutors.removeById("persistentExec1");
        persistentExecutors.removeById("persistentExec2");

        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + "-1");
        String taskIdA = null, taskIdB = null, taskIdC = null;
        boolean successful = false;
        try {
            // Schedule on original instance where fail over is enabled
            StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/execRFR&initialDelayMS=0&delayMS=1810&test=testRemoveFailOverEnablementWhileServerIsRunning[1]");

            int start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of first scheduled task not found in servlet output: " + result);
            taskIdA = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled first task " + taskIdA);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/execRFR&initialDelayMS=0&delayMS=1820&test=testRemoveFailOverEnablementWhileServerIsRunning[2]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of second scheduled task not found in servlet output: " + result);
            taskIdB = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled second task " + taskIdB);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/execRFR&test=testRemoveFailOverEnablementWhileServerIsRunning[3]");

            // Disable fail over
            persistentExecRFR.setMissedTaskThreshold(null);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES);

            // Schedule another task and verify that it runs
            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleOneTimeTask&jndiName=persistent/execRFR&initialDelayMS=0&test=testRemoveFailOverEnablementWhileServerIsRunning[4]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of third scheduled task not found in servlet output: " + result);
            taskIdC = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled third task " + taskIdC);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskCompleted&taskId=" + taskIdC + "&expectedResult=1&jndiName=persistent/execRFR&test=testRemoveFailOverEnablementWhileServerIsRunning[5]");

            // In order for the old tasks to run, they will need to be manually assigned to the single instance
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTransferWithoutMBean&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/execRFR&test=testRemoveFailOverEnablementWhileServerIsRunning[6]");

            // Verify that tasks still run
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/execRFR&test=testRemoveFailOverEnablementWhileServerIsRunning[7]");

            successful = true;
        } finally {
            if (server.isStarted())
                try {
                    if (taskIdC != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdC + "&jndiName=persistent/execRFR&test=testRemoveFailOverEnablementWhileServerIsRunning[8]");

                    if (taskIdB != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdB + "&jndiName=persistent/execRFR&test=testRemoveFailOverEnablementWhileServerIsRunning[9]");

                    if (taskIdA != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdA + "&jndiName=persistent/execRFR&test=testRemoveFailOverEnablementWhileServerIsRunning[10]");
                } catch (Error | Exception x) {
                    if (successful)
                        throw x;
                }
        }
    }

    /**
     * testRemoveFailOverEnablementWhileServerIsStopped - Schedules tasks on an instance where fail over is enabled.
     * Stops the server and disables fail over on the same instance. Starts up the server. New tasks can be scheduled
     * on the instance and should run there, but those that were submitted previously will not be recognized unless
     * some MBean operations are performed. Users should not do this. This test is only written to experiment with
     * what would happen and explore how to cope with it.
     */
    @AllowedFFDC({
        // due to transaction timeout:
        "javax.transaction.RollbackException",
        "javax.transaction.xa.XAException",
        "javax.persistence.PersistenceException",
        "java.lang.IllegalStateException"
        })
    @Test
    public void testRemoveFailOverEnablementWhileServerIsStopped() throws Exception {
        // start with fail over enabled
        ServerConfiguration config = originalConfig.clone();
        ConfigElementList<PersistentExecutor> persistentExecutors = config.getPersistentExecutors();
        PersistentExecutor persistentExecRF = (PersistentExecutor) persistentExecutors.getById("persistentExec2").clone();
        persistentExecRF.setId("persistentExecRF");
        persistentExecRF.setJndiName("persistent/execRF");
        persistentExecutors.add(persistentExecRF);
        persistentExecutors.removeById("persistentExec1");
        persistentExecutors.removeById("persistentExec2");

        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + "-1");
        String taskIdA = null, taskIdB = null, taskIdC = null;
        boolean successful = false;
        try {
            // Schedule on original instance where fail over is enabled
            StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/execRF&initialDelayMS=0&delayMS=1710&test=testRemoveFailOverEnablementWhileServerIsStopped[1]");

            int start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of first scheduled task not found in servlet output: " + result);
            taskIdA = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled first task " + taskIdA);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/execRF&initialDelayMS=0&delayMS=1720&test=testRemoveFailOverEnablementWhileServerIsStopped[2]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of second scheduled task not found in servlet output: " + result);
            taskIdB = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled second task " + taskIdB);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/execRF&test=testRemoveFailOverEnablementWhileServerIsStopped[3]");

            server.stopServer(
                    // rollback due to transaction timeout
                    "DSRA0304E.*",
                    "DSRA0302E.*XA_RBROLLBACK",
                    "J2CA0079E",
                    "J2CA0088W",
                    "CWWKC1503W.*IncTask_testRemoveFailOverEnablementWhileServerIsStopped"
                     // might also need to allow for expected warnings if the server shuts down while a task is running
                    );

            // Disable fail over
            persistentExecRF.setMissedTaskThreshold(null);
            server.updateServerConfiguration(config);
            server.startServer(testName.getMethodName() + "-2");

            // Schedule another task and verify that it runs
            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleOneTimeTask&jndiName=persistent/execRF&initialDelayMS=0&test=testRemoveFailOverEnablementWhileServerIsStopped[4]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of third scheduled task not found in servlet output: " + result);
            taskIdC = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled third task " + taskIdC);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskCompleted&taskId=" + taskIdC + "&expectedResult=1&jndiName=persistent/execRF&test=testRemoveFailOverEnablementWhileServerIsStopped[5]");

            // In order for the old tasks to run, they will need to be manually assigned to the single instance
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTransferWithMBean&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/execRF&test=testRemoveFailOverEnablementWhileServerIsStopped[6]");

            // Verify that tasks still run
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/execRF&test=testRemoveFailOverEnablementWhileServerIsStopped[7]");

            successful = true;
        } finally {
            if (server.isStarted())
                try {
                    if (taskIdC != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdC + "&jndiName=persistent/execRF&test=testRemoveFailOverEnablementWhileServerIsStopped[8]");

                    if (taskIdB != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdB + "&jndiName=persistent/execRF&test=testRemoveFailOverEnablementWhileServerIsStopped[9]");

                    if (taskIdA != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdA + "&jndiName=persistent/execRF&test=testRemoveFailOverEnablementWhileServerIsStopped[10]");
                } catch (Error | Exception x) {
                    if (successful)
                        throw x;
                }
        }
    }

    /**
     * testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning - Schedules tasks on an instance where fail over is not enabled.
     * While the server is running, removes the original instance, creating a new one with fail over enabled. Then verifies
     * that the previous, as well as new, tasks run.
     */
    @Test
    public void testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning() throws Exception {
        ServerConfiguration config = originalConfig.clone();
        config.getPersistentExecutors().removeById("persistentExec1");
        PersistentExecutor persistentExec = config.getPersistentExecutors().getById("persistentExec2");
        String missedTaskThreshold = persistentExec.getMissedTaskThreshold();
        persistentExec.setMissedTaskThreshold(null);

        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + "-1");
        String taskIdA = null, taskIdB = null, taskIdC = null;
        boolean successful = false;
        try {
            // Schedule on original instance where fail over is not enabled
            StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=1800&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[1]");

            int start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of first scheduled task not found in servlet output: " + result);
            taskIdA = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled first task " + taskIdA);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=1850&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[2]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of second scheduled task not found in servlet output: " + result);
            taskIdB = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled second task " + taskIdB);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/exec2&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[3]");

            // Enable fail over
            config.getPersistentExecutors().removeById("persistentExec2");
            persistentExec.setId("persistentExec3FE");
            persistentExec.setJndiName("persistent/exec3FE");
            persistentExec.setMissedTaskThreshold(missedTaskThreshold);
            config.getPersistentExecutors().add(persistentExec);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_NAMES);

            // Verify that tasks still run
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/exec3FE&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[4]");

            // Schedule another task and verify that it runs
            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleOneTimeTask&jndiName=persistent/exec3FE&initialDelayMS=0&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[5]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of third scheduled task not found in servlet output: " + result);
            taskIdC = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled third task " + taskIdC);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskCompleted&taskId=" + taskIdC + "&expectedResult=1&jndiName=persistent/exec3FE&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[6]");
            successful = true;
        } finally {
            if (server.isStarted())
                try {
                    if (taskIdC != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdC + "&jndiName=" + persistentExec.getJndiName() + "&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[7]");

                    if (taskIdB != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdB + "&jndiName=" + persistentExec.getJndiName() + "&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[8]");

                    if (taskIdA != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdA + "&jndiName=" + persistentExec.getJndiName() + "&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsRunning[9]");
                } catch (Error | Exception x) {
                    if (successful)
                        throw x;
                }
        }
    }

    /**
     * testReplaceWithNewFailOverEnabledInstanceWhileServerIsStopped - Schedules tasks on an instance where fail over is not enabled.
     * Stops the server and removes the original instance, creating a new one with fail over enabled. Starts up the server and verifies
     * that the previous, as well as new, tasks run.
     */
    @Test
    public void testReplaceWithNewFailOverEnabledInstanceWhileServerIsStopped() throws Exception {
        ServerConfiguration config = originalConfig.clone();
        config.getPersistentExecutors().removeById("persistentExec1");
        PersistentExecutor persistentExec = config.getPersistentExecutors().getById("persistentExec2");
        String missedTaskThreshold = persistentExec.getMissedTaskThreshold();
        persistentExec.setMissedTaskThreshold(null);

        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + "-1");
        String taskIdA = null, taskIdB = null, taskIdC = null;
        boolean successful = false;
        try {
            // Schedule on original instance where fail over is not enabled
            StringBuilder result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=1700&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsStopped[1]");

            int start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of first scheduled task not found in servlet output: " + result);
            taskIdA = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled first task " + taskIdA);

            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleRepeatingTask&jndiName=persistent/exec2&initialDelayMS=0&delayMS=1750&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsStopped[2]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of second scheduled task not found in servlet output: " + result);
            taskIdB = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled second task " + taskIdB);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/exec2&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsStopped[3]");

            server.stopServer(); // this might need to allow for expected warnings if the server shuts down while a task is running

            // Enable fail over
            config.getPersistentExecutors().removeById("persistentExec2");
            persistentExec.setId("persistentExec2FE");
            persistentExec.setMissedTaskThreshold(missedTaskThreshold);
            config.getPersistentExecutors().add(persistentExec);
            server.updateServerConfiguration(config);
            server.startServer(testName.getMethodName() + "-2");

            // Verify that tasks still run
            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTasksAreRunning&taskId=" + taskIdA + "&taskId=" + taskIdB + "&jndiName=persistent/exec2&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsStopped[4]");

            // Schedule another task and verify that it runs
            result = runTestWithResponse(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testScheduleOneTimeTask&jndiName=persistent/exec2&initialDelayMS=0&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsStopped[5]");

            start = result.indexOf(TASK_ID_MESSAGE);
            if (start < 0)
                fail("Task id of third scheduled task not found in servlet output: " + result);
            taskIdC = result.substring(start += TASK_ID_MESSAGE.length(), result.indexOf(".", start));

            System.out.println("Scheduled third task " + taskIdC);

            runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                    "testTaskCompleted&taskId=" + taskIdC + "&expectedResult=1&jndiName=persistent/exec2&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsStopped[6]");
            successful = true;
        } finally {
            if (server.isStarted())
                try {
                    if (taskIdC != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdC + "&jndiName=persistent/exec2&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsStopped[7]");

                    if (taskIdB != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdB + "&jndiName=persistent/exec2&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsStopped[8]");

                    if (taskIdA != null)
                        runTest(server, APP_NAME + "/Failover1ServerTestServlet",
                                "testCancelTask&taskId=" + taskIdA + "&jndiName=persistent/exec2&test=testReplaceWithNewFailOverEnabledInstanceWhileServerIsStopped[9]");
                } catch (Error | Exception x) {
                    if (successful)
                        throw x;
                }
        }
    }
}