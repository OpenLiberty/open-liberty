/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.oneexec;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for one persistent executor instance running tasks, with multiple instances scheduling tasks.
 */
@RunWith(FATRunner.class)
public class OneExecutorRunsAllTest {
    private static final Set<String> appNames = Collections.singleton("persistoneexectest");
    
    private static final String APP_NAME = "persistoneexectest";

    private static ServerConfiguration originalConfig;

    private static final LibertyServer server = FATSuite.server;

    private static final String TASK_ID_SEARCH_TEXT = "Task id is ";

    @Rule
    public TestName testName = new TestName();

    /**
     * Runs a test in the servlet.
     * 
     * @param queryString query string including at least the test name
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    protected StringBuilder runInServlet(String queryString) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/persistoneexectest?" + queryString);
        Log.info(getClass(), "runInServlet", "URL is " + url);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(getClass(), "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                Log.info(getClass(), "runInServlet", "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }

            return lines;
        } finally {
            con.disconnect();
            Log.info(getClass(), "runInServlet", "disconnected from servlet");
        }
    }

    /**
     * Before running any tests, start the server
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Delete the Derby-only database that is used by the persistent scheduled executor
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/persistoneexecdb");

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "web");
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
            server.stopServer();
    }

    /**
     * Simulate a scenario where we need to prevent deadlocks with EJB singletons
     */
    @Test
    public void testEJBSingletonDeadlockPrevention() throws Exception {
        runInServlet("test=testEJBSingletonDeadlockPrevention");
    }

    /**
     * Test interfaces for find and remove that we provide to EJB container for EJB persistent timers.
     */
    @Test
    public void testEJBTimersFindAndRemove() throws Exception {
        runInServlet("test=testEJBTimersFindAndRemove");
    }

    /**
     * Verify that the interface to EJB Timer Service indicates that fail over is not enabled.
     */
    @Test
    public void testFailOverIsNotEnabled() throws Exception {
        runInServlet("test=testFailOverIsNotEnabled");
    }

    /**
     * Schedule tasks from two persistentExecutor instances with execution disabled.
     * Verify the tasks run on a third instance, which has task execution enabled.
     * Remove the third instance. Schedule another task.
     * Add an instance with execution enabled, and verify that all tasks run.
     */
    @Test
    public void testRunTasksOnDifferentExecutor() throws Exception {
        StringBuilder output;
        int start;

        // Schedule a task using an executor that doesn't run tasks
        output = runInServlet(
                        "test=testScheduleRepeatingTask&jndiName=concurrent/executorA&initialDelay=300&interval=2000&invokedBy=testRunTasksOnDifferentExecutor-1");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdA1 = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        // Schedule a task using another executor that doesn't run tasks
        output = runInServlet(
                        "test=testScheduleRepeatingTask&jndiName=concurrent/executorB&initialDelay=200&interval=2000&invokedBy=testRunTasksOnDifferentExecutor-2");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdB1 = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        // Liberty doesn't the ability to coordinate across server instances yet, so we need to manually request that the tasks be transferred
        runInServlet("test=testTransfer&jndiName=concurrent/executorC&oldExecutorId=executorA&invokedBy=testRunTasksOnDifferentExecutor-3");
        runInServlet("test=testTransfer&jndiName=concurrent/executorC&oldExecutorId=executorB&invokedBy=testRunTasksOnDifferentExecutor-4");

        // Verify both tasks are running
        runInServlet("test=testTasksAreRunning&jndiName=concurrent/executorC&taskId=" + taskIdA1 + "&taskId=" + taskIdB1 + "&invokedBy=testRunTasksOnDifferentExecutor-5");

        String taskIdB2;
        try {
            // Remove the executor (C) that is running tasks.
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor executorC = config.getPersistentExecutors().removeById("executorC");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Schedule a task to executor (B) that can't run tasks, and with no other executors that can run tasks
            output = runInServlet(
                            "test=testScheduleRepeatingTask&jndiName=concurrent/executorB&initialDelay=0&interval=2000&invokedBy=testRunTasksOnDifferentExecutor-6");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdB2 = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            // Make another executor (B) run tasks, but without heart beats.
            PersistentExecutor executorB = config.getPersistentExecutors().getBy("id", "executorB");
            executorB.setEnableTaskExecution("true");
            executorB.setInitialPollDelay("0s");

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testTaskIsRunning&jndiName=concurrent/executorB&taskId=" + taskIdB2 + "&invokedBy=testRunTasksOnDifferentExecutor-7");

            // Add an executor (C) which can run tasks.
            config.getPersistentExecutors().add(executorC);

            // Make executor B stop running tasks, and see if executor C picks up its latest task
            executorB.setEnableTaskExecution("false");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Liberty doesn't have high availability support yet, so we need to manually trigger the failover from executorB
            runInServlet("test=testTransfer&jndiName=concurrent/executorC&oldExecutorId=executorB&maxTaskId=" + Long.MAX_VALUE + "&invokedBy=testRunTasksOnDifferentExecutor-8");

            // Verify that all tasks are running
            runInServlet("test=testTasksAreRunning&jndiName=concurrent/executorC&taskId="
                         + taskIdA1 + "&taskId=" + taskIdB1 + "&taskId=" + taskIdB2
                         + "&invokedBy=testRunTasksOnDifferentExecutor-9");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }

        runInServlet("test=testRemoveTask&jndiName=concurrent/executorA&taskId=" + taskIdA1 + "&invokedBy=testRunTasksOnDifferentExecutor-10");
        runInServlet("test=testRemoveTask&jndiName=concurrent/executorA&taskId=" + taskIdB1 + "&invokedBy=testRunTasksOnDifferentExecutor-11");
        runInServlet("test=testRemoveTask&jndiName=concurrent/executorA&taskId=" + taskIdB2 + "&invokedBy=testRunTasksOnDifferentExecutor-12");
    }
}