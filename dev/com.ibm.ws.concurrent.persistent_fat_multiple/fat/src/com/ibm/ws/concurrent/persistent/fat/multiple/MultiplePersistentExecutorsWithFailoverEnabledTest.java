/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.multiple;

import java.util.Collections;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests for multiple persistent scheduled executor instances sharing the same database
 */
@RunWith(FATRunner.class)
public class MultiplePersistentExecutorsWithFailoverEnabledTest extends FATServletClient {
	private static final String APP_NAME = "persistmultitest";
    private static final Set<String> appNames = Collections.singleton(APP_NAME);
    
    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;
    
    private static ServerConfiguration originalConfig, failoverEnabledConfig;
    
    private static final LibertyServer server = FATSuite.server;
    
    private static final String TASK_ID_SEARCH_TEXT = "Task ids: ";
    
    @BeforeClass
    public static void setUp() throws Exception {
    	//Get driver type
    	server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());
		
    	//Setup server DataSource properties
    	DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);
        
    	//Add application to server
    	ShrinkHelper.defaultDropinApp(server, APP_NAME, "web");
    	
    	//Edit original config to enable failover
    	originalConfig = server.getServerConfiguration();
        failoverEnabledConfig = originalConfig.clone();
        ConfigElementList<PersistentExecutor> executors = failoverEnabledConfig.getPersistentExecutors();
        executors.getById("executor1").setMissedTaskThreshold("15s");
        executors.getById("executor2").setMissedTaskThreshold("16s");
        executors.getById("executor1").setExtraAttribute("ignore.minimum.for.test.use.only", "true");
        executors.getById("executor2").setExtraAttribute("ignore.minimum.for.test.use.only", "true");
        server.updateServerConfiguration(failoverEnabledConfig);

    	//Start server
        server.startServer();
    }

    /**
     * After completing all tests, stop the server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null)
            try {
                if (server.isStarted())
                    server.stopServer("DSRA0174W");
            } finally {
                if (originalConfig != null)
                    server.updateServerConfiguration(originalConfig);
            }
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
    public void testFailoverBetweenTwoInstancesFE() throws Exception {
        StringBuilder output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=2000&invokedBy=testFailoverBetweenTwoInstancesFE-1");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        try {
            // Simulate failover by removing the instance (executor1) to which the task was scheduled
            ServerConfiguration config = failoverEnabledConfig.clone();
            PersistentExecutor executor1 = config.getPersistentExecutors().removeById("executor1");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("testTaskIsRunning&jndiName=concurrent/executor2&taskId=" + taskId + "&invokedBy=testFailoverBetweenTwoInstancesFE-2");

            // Simulate the first instance coming back up
            config.getPersistentExecutors().add(executor1);

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("testTaskIsRunning&jndiName=concurrent/executor1&taskId=" + taskId + "&invokedBy=testFailoverBetweenTwoInstancesFE-3");

            // Simulate the second instance going down (fail back to first instance)
            config.getPersistentExecutors().removeById("executor2");

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("testTaskIsRunning&jndiName=concurrent/executor1&taskId=" + taskId + "&invokedBy=testFailoverBetweenTwoInstancesFE-4");

            runInServlet("testRemoveTask&jndiName=concurrent/executor1&taskId=" + taskId + "&invokedBy=testFailoverBetweenTwoInstancesFE-5");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(failoverEnabledConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Schedule 5 tasks from two persistentExecutor instances.
     * Remove both instances, and verify that all of the tasks continue running on third and fourth instances.
     */
    @Test
    public void testFailoverWithFourInstancesFE() throws Exception {
        StringBuilder output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor1&taskName=FW4I-A&initialDelay=0&interval=2500&invokedBy=testFailoverWithFourInstancesFE-1");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdA = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor1&taskName=FW4I-B&initialDelay=0&interval=2400&invokedBy=testFailoverWithFourInstancesFE-2");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdB = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor1&taskName=FW4I-C&initialDelay=0&interval=2300&invokedBy=testFailoverWithFourInstancesFE-3");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdC = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor2&taskName=FW4I-D&initialDelay=0&interval=2200&invokedBy=testFailoverWithFourInstancesFE-4");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdD = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor1&taskName=FW4I-E&initialDelay=0&interval=2100&invokedBy=testFailoverWithFourInstancesFE-5");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdE = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        try {
            // Simulate fail over by removing the instances (executor1, executor2) to which the tasks were scheduled
            ServerConfiguration config = failoverEnabledConfig.clone();
            PersistentExecutor executor1 = config.getPersistentExecutors().removeById("executor1");

            PersistentExecutor executor3 = (PersistentExecutor) executor1.clone();
            executor3.setId("executor3");
            executor3.setJndiName("concurrent/executor3");
            config.getPersistentExecutors().add(executor3);

            PersistentExecutor executor2 = config.getPersistentExecutors().removeById("executor2");

            PersistentExecutor executor4 = (PersistentExecutor) executor2.clone();
            executor4.setId("executor4");
            executor4.setJndiName("concurrent/executor4");
            config.getPersistentExecutors().add(executor4);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("testTasksAreRunning&jndiName=concurrent/executor3&taskId="
                         + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC + "&taskId=" + taskIdD + "&taskId=" + taskIdE +
                         "&invokedBy=testFailoverWithFourInstancesFE-6");

            runInServlet("testCancelTasks&jndiName=concurrent/executor4&pattern=FW4I-_&state=ENDED&inState=false&numCancelsExpected=5&invokedBy=testFailoverWithFourInstancesFE-7");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(failoverEnabledConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Verify that one instance can schedule a task, and another instance can query and remove it.
     */
    @Test
    public void testTaskVisibleToBothExecutorInstancesFE() throws Exception {
        StringBuilder output = runInServlet("testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=1000&invokedBy=testTaskVisibleToBothExecutorInstancesFE-1");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        runInServlet("testTaskIsRunning&jndiName=concurrent/executor2&taskId=" + taskId + "&invokedBy=testTaskVisibleToBothExecutorInstancesFE-2");

        runInServlet("testRemoveTask&jndiName=concurrent/executor2&taskId=" + taskId + "&invokedBy=testTaskVisibleToBothExecutorInstancesFE-3");
    }
}