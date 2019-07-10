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
package com.ibm.ws.concurrent.persistent.fat.multiple;

import static componenttest.annotation.SkipIfSysProp.DB_Informix;

import java.util.Collections;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests for multiple persistent scheduled executor instances sharing the same database
 */
@RunWith(FATRunner.class)
@SkipIfSysProp(DB_Informix) // persistent executor is not support on Informix
public class MultiplePersistentExecutorsTest extends FATServletClient {

    private static final String APP_NAME = "persistmultitest";
    private static final Set<String> appNames = Collections.singleton(APP_NAME);

    private static ServerConfiguration originalConfig;

    private static final LibertyServer server = FATSuite.server;

    private static final String TASK_ID_SEARCH_TEXT = "Task ids: ";

    @BeforeClass
    public static void setUp() throws Exception {
        server.addInstalledAppForValidation(APP_NAME);
        server.configureForAnyDatabase();
        originalConfig = server.getServerConfiguration();
        server.startServer();
    }

    /**
     * After completing all tests, stop the server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted())
            server.stopServer("DSRA0174W",
                              "DSRA1300E" /* Sybase does not implement Connection.getClientInfo */);
    }

    private StringBuilder runInServlet(String queryString) throws Exception {
        return runTestWithResponse(server, APP_NAME, queryString);
    }

    @Test
    public void testConnectionSharing() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    /**
     * Schedule a task from one persistentExecutor instance.
     * Remove the instance, and verify that it continues running on a second instance.
     * Restore the first instance and remove the second. Verify the task continues running.
     */
    @Test
    public void testFailoverBetweenTwoInstances() throws Exception {
        StringBuilder output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=2000&invokedBy=testFailoverBetweenTwoInstances-1");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        try {
            // Simulate failover by removing the instance (executor1) to which the task was scheduled
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor executor1 = config.getPersistentExecutors().removeById("executor1");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Liberty doesn't have high availability support yet, so we need to manually trigger the failover
            runInServlet("testTransfer&jndiName=concurrent/executor2&oldExecutorId=executor1&invokedBy=testFailoverBetweenTwoInstances-2");

            runInServlet("testTaskIsRunning&jndiName=concurrent/executor2&taskId=" + taskId + "&invokedBy=testFailoverBetweenTwoInstances-3");

            // Simulate the first instance coming back up
            config.getPersistentExecutors().add(executor1);

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("testTaskIsRunning&jndiName=concurrent/executor1&taskId=" + taskId + "&invokedBy=testFailoverBetweenTwoInstances-4");

            // Simulate the second instance going down (fail back to first instance)
            config.getPersistentExecutors().removeById("executor2");

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Liberty doesn't have high availability support yet, so we need to manually trigger the failover
            runInServlet("testTransfer&jndiName=concurrent/executor1&oldExecutorId=executor2&invokedBy=testFailoverBetweenTwoInstances-5");

            runInServlet("testTaskIsRunning&jndiName=concurrent/executor1&taskId=" + taskId + "&invokedBy=testFailoverBetweenTwoInstances-6");

            runInServlet("testRemoveTask&jndiName=concurrent/executor1&taskId=" + taskId + "&invokedBy=testFailoverBetweenTwoInstances-7");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Schedule 5 tasks from two persistentExecutor instances.
     * Remove both instances, and verify that all of the tasks continue running on third and fourth instances.
     */
    @Test
    public void testFailoverWithFourInstances() throws Exception {
        StringBuilder output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor1&taskName=FW4I-A&initialDelay=0&interval=2500&invokedBy=testFailoverWithFourInstances-1");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdA = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor1&taskName=FW4I-B&initialDelay=0&interval=2400&invokedBy=testFailoverWithFourInstances-2");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdB = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor1&taskName=FW4I-C&initialDelay=0&interval=2300&invokedBy=testFailoverWithFourInstances-3");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdC = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor2&taskName=FW4I-D&initialDelay=0&interval=2200&invokedBy=testFailoverWithFourInstances-4");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdD = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor1&taskName=FW4I-E&initialDelay=0&interval=2100&invokedBy=testFailoverWithFourInstances-5");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdE = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        try {
            // Simulate failover by removing the instances (executor1, executor2) to which the tasks were scheduled
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor executor1 = config.getPersistentExecutors().removeById("executor1");
            PersistentExecutor executor3 = (PersistentExecutor) executor1.clone();
            executor3.setId("executor3");
            executor3.setJndiName("concurrent/executor3");
            config.getPersistentExecutors().add(executor3);
            PersistentExecutor executor2 = config.getPersistentExecutors().removeById("executor2");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Liberty doesn't have high availability support yet, so we need to manually cause the failover.

            // Preemptively switch the executor2 partition info to executor4, which we are about to create
            runInServlet("testUpdatePartitions&jndiName=concurrent/executor3&executorId=executor2&newExecutorId=executor4&expectedUpdateCount=1&invokedBy=testFailoverWithFourInstances-6");

            PersistentExecutor executor4 = (PersistentExecutor) executor2.clone();
            executor4.setId("executor4");
            executor4.setJndiName("concurrent/executor4");
            config.getPersistentExecutors().add(executor4);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Determine which tasks assigned to executor1 haven't ended yet and split them into two groups
            output = runInServlet("testFindTaskIds&jndiName=concurrent/executor3&executorId=executor1&invokedBy=testFailoverWithFourInstances-7");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task ids not found in servlet output: " + output);
            String idsString = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));
            String[] ids = idsString.split(" ");
            int index = ids.length / 2 - 1;
            String maxTaskIdToAssignToExecutor3 = ids[index];

            // Transfer tasks from executor1 to executor3 and executor4
            runInServlet("testTransfer&jndiName=concurrent/executor3&oldExecutorId=executor1&maxTaskId=" + maxTaskIdToAssignToExecutor3
                         + "&invokedBy=testFailoverWithFourInstances-8");
            runInServlet("testTransfer&jndiName=concurrent/executor4&oldExecutorId=executor1&invokedBy=testFailoverWithFourInstances-9");

            // Remove the partition entry for executor1 which doesn't exist anymore
            runInServlet("testRemovePartitions&jndiName=concurrent/executor3&executorId=executor1&libertyServer=com.ibm.ws.concurrent.persistent.fat.multiple&expectedUpdateCount=1&invokedBy=testFailoverWithFourInstances-10");

            runInServlet("testTasksAreRunning&jndiName=concurrent/executor3&taskId="
                         + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC + "&taskId=" + taskIdD + "&taskId=" + taskIdE +
                         "&invokedBy=testFailoverWithFourInstances-11");

            runInServlet("testCancelTasks&jndiName=concurrent/executor4&pattern=FW4I-_&state=ENDED&inState=false&numCancelsExpected=5&invokedBy=testFailoverWithFourInstances-12");

            // Query for partition entries
            runInServlet("testFindPartitions&jndiName=concurrent/executor3&executorId=executor3&executorId=executor4&invokedBy=testFailoverWithFourInstances-13");

            // Schedule a task in the distant future
            runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor4&taskName=FW4I-F&initialDelay=36000000&interval=3200&invokedBy=testFailoverWithFourInstances-14");

            // Make executor4's partition info look like the executor in the PersistentExecutorMBean JavaDoc code example
            runInServlet("testUpdatePartitions&jndiName=concurrent/executor4&executorId=executor4&newExecutorId=defaultEJBPersistentTimerExecutor&newHostName=hostA.rchland.ibm.com&newLibertyServer=myServer1&expectedUpdateCount=1&invokedBy=testFailoverWithFourInstances-15");

            // Run the the PersistentExecutorMBean JavaDoc code example
            runInServlet("testMBeanCodeExample&invokedBy=testFailoverWithFourInstances-16");

            // Update one of the partition entries
            runInServlet("testUpdatePartitions&jndiName=concurrent/executor3&executorId=executor3&newHostName=abcdefg.rchland.ibm.com&expectedUpdateCount=1&invokedBy=testFailoverWithFourInstances-17");

            // Remove one of the partition entries
            runInServlet("testRemovePartitions&jndiName=concurrent/executor3&hostName=abcdefg.rchland.ibm.com&libertyServer=com.ibm.ws.concurrent.persistent.fat.multiple&expectedUpdateCount=1&invokedBy=testFailoverWithFourInstances-18");

            // Attempt an update to the removed partition entry
            runInServlet("testUpdatePartitions&jndiName=concurrent/executor3&executorId=executor3&newHostName=xyz@rchland.ibm.com&expectedUpdateCount=0&invokedBy=testFailoverWithFourInstances-19");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Verify that one instance can schedule a task, and another instance can query and remove it.
     */
    @Test
    public void testTaskVisibleToBothExecutorInstances() throws Exception {
        StringBuilder output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=1000&invokedBy=testTaskVisibleToBothExecutorInstances-1");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        runInServlet("testTaskIsRunning&jndiName=concurrent/executor2&taskId=" + taskId + "&invokedBy=testTaskVisibleToBothExecutorInstances-2");

        runInServlet("testRemoveTask&jndiName=concurrent/executor2&taskId=" + taskId + "&invokedBy=testTaskVisibleToBothExecutorInstances-3");
    }
}