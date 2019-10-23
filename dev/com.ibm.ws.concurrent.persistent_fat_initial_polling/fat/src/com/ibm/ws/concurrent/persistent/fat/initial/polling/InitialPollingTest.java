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
package com.ibm.ws.concurrent.persistent.fat.initial.polling;

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

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.database.DerbyEmbeddedUtilities;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for initial polling for persistent scheduled executor instance
 */
@RunWith(FATRunner.class)
public class InitialPollingTest {
    private static final Set<String> appNames = Collections.singleton("initialpollingtest");
    
    private static final String APP_NAME = "initialpollingtest";

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
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/initialpollingtest?" + queryString);
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
        originalConfig = server.getServerConfiguration();
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "web");
        DerbyEmbeddedUtilities.createDB(server, "DefaultDataSource");
        server.startServer("InitialPollingTest.log");
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
     * Schedule 4 tasks with executor unable to execute task. Restart the server to test the initialPolling schedules and executes
     * the tasks.
     *
     * @throws Exception
     */
    @Test
    public void testRestartWithFourTasks() throws Exception {

        String taskIdA;
        String taskIdB;
        String taskIdC;
        String taskIdD;

        try {
            ServerConfiguration config = originalConfig.clone();

            config.getPersistentExecutors().getBy("id", "executor1").setInitialPollDelay("20s");
            config.getPersistentExecutors().getBy("id", "executor1").setEnableTaskExecution("false");

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Schedule tasks
            StringBuilder output = runInServlet(
                                                "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=2500&invokedBy=testRestartWithFourTasks-1");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdA = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            output = runInServlet(
                                  "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=2400&invokedBy=testRestartWithFourTasks-2");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdB = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            output = runInServlet(
                                  "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=2300&invokedBy=testRestartWithFourTasks-3");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdC = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            output = runInServlet(
                                  "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=2200&invokedBy=testRestartWithFourTasks-4");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdD = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            config.getPersistentExecutors().getBy("id", "executor1").setInitialPollDelay("20s");
            config.getPersistentExecutors().getBy("id", "executor1").setEnableTaskExecution("true");

            server.stopServer();
            server.updateServerConfiguration(config);
            server.startServer("testRestartWithFourTasks.log");

            runInServlet("test=testTasksAreRunning&jndiName=concurrent/executor1&taskId="
                         + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC + "&taskId=" + taskIdD +
                         "&invokedBy=testRestartWithFourTasks-5");

            runInServlet("test=testRemoveTask&jndiName=concurrent/executor1&taskId=" + taskIdA + "&invokedBy=testRestartWithFourTasks-6");
            runInServlet("test=testRemoveTask&jndiName=concurrent/executor1&taskId=" + taskIdB + "&invokedBy=testRestartWithFourTasks-7");
            runInServlet("test=testRemoveTask&jndiName=concurrent/executor1&taskId=" + taskIdC + "&invokedBy=testRestartWithFourTasks-8");
            runInServlet("test=testRemoveTask&jndiName=concurrent/executor1&taskId=" + taskIdD + "&invokedBy=testRestartWithFourTasks-9");
        } finally {
            // restore original configuration
            if (server.isStarted())
                server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            if (server.isStarted())
                server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Schedule x repeating tasks with executor. Update the config of the server to test for reschedules and executes
     * the tasks.
     *
     * @throws Exception
     */
    @Test
    public void testRescheduleUnderConfigUpdate() throws Exception {

        String taskIdA;
        String taskIdB;
        String taskIdC;
        String taskIdD;
        String taskIdE;

        try {
            // Remove the second executor to cut down on noise
            ServerConfiguration config = originalConfig.clone();
            config.getPersistentExecutors().removeById("executor2");

            // Schedule tasks
            StringBuilder output = runInServlet(
                                                "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=100&invokedBy=testRescheduleUnderConfigUpdate-1");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdA = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            output = runInServlet(
                                  "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=100&invokedBy=testRescheduleUnderConfigUpdate-2");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdB = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            output = runInServlet(
                                  "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=100&invokedBy=testRescheduleUnderConfigUpdate-3");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdC = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            output = runInServlet(
                                  "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=100&invokedBy=testRescheduleUnderConfigUpdate-4");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdD = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            output = runInServlet(
                                  "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=100&invokedBy=testRescheduleUnderConfigUpdate-5");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdE = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            // Do config updates while the Tasks are running.
            for (int xx = 1; xx <= 10; xx++) {
                //String newtime = Integer.toString(xx + 20) + 's';
                //config.getPersistentExecutors().getBy("id", "executor2").setInitialPollDelay(newtime);

                if ((xx % 2) == 0) {
                    config.getFeatureManager().getFeatures().remove("osgiConsole-1.0");
                } else {
                    config.getFeatureManager().getFeatures().add("osgiConsole-1.0");
                }

                server.setMarkToEndOfLog();
                server.updateServerConfiguration(config);
                server.waitForConfigUpdateInLogUsingMark(appNames);

                Thread.sleep(500);
            }

            runInServlet("test=testTasksAreRunning&jndiName=concurrent/executor1&taskId="
                         + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC + "&taskId=" + taskIdD + "&taskId=" + taskIdE +
                         "&invokedBy=testRescheduleUnderConfigUpdate-6");

            runInServlet("test=testRemoveTask&jndiName=concurrent/executor1&taskId=" + taskIdA + "&invokedBy=testRescheduleUnderConfigUpdate-7");
            runInServlet("test=testRemoveTask&jndiName=concurrent/executor1&taskId=" + taskIdB + "&invokedBy=testRescheduleUnderConfigUpdate-8");
            runInServlet("test=testRemoveTask&jndiName=concurrent/executor1&taskId=" + taskIdC + "&invokedBy=testRescheduleUnderConfigUpdate-9");
            runInServlet("test=testRemoveTask&jndiName=concurrent/executor1&taskId=" + taskIdD + "&invokedBy=testRescheduleUnderConfigUpdate-10");
            runInServlet("test=testRemoveTask&jndiName=concurrent/executor1&taskId=" + taskIdE + "&invokedBy=testRescheduleUnderConfigUpdate-11");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Schedule a task where execution is disabled for the executor.
     * Switch to execution enabled, but with initialPollInterval set to -1,
     * which means polling will not happen until the persistent executor is signaled.
     * Signal the persistent executor to start polling.
     * Verify the task runs.
     *
     * @throws Exception
     */
    @Test
    public void testStartPolling() throws Exception {
        ServerConfiguration config = originalConfig.clone();
        PersistentExecutor executor1 = config.getPersistentExecutors().getBy("id", "executor1");
        executor1.setEnableTaskExecution("false");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);
        try {
            // schedule a one-shot task, which won't be able to run yet because execution is disabled
            StringBuilder output = runInServlet(
                                                "test=testScheduleOneShotTask&jndiName=concurrent/executor1&initialDelay=0&invokedBy=testStartPolling");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            // enable task execution, but with polling disabled until signaled
            executor1.setEnableTaskExecution("true");
            executor1.setInitialPollDelay("-1");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // verify the task did not run yet
            runInServlet("test=testTaskCompletesAfterPollingStarted&jndiName=concurrent/executor1&taskId=" + taskId + "&invokedBy=testStartPolling");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }
}