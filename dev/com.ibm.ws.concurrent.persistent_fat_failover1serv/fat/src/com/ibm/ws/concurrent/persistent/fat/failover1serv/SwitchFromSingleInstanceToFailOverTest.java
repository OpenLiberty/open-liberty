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

import java.util.Collections;
import java.util.Set;

import org.junit.After;
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
                    "testTaskCompleted&taskId=" + taskIdA + "&expectedResult=1&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsRunning[8]");
        } finally {
            if (server.isStarted()) {
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
            server.startServer();
            
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
                    "testTaskCompleted&taskId=" + taskIdA + "&expectedResult=1&jndiName=persistent/exec1&test=testEnableFailOverWhileServerIsStopped[8]");
        } finally {
            if (server.isStarted()) {
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
            }
        }
    }
}