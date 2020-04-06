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
package com.ibm.ws.concurrent.persistent.fat.simctrl;

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

import componenttest.topology.impl.LibertyServer;
import componenttest.custom.junit.runner.FATRunner;

/**
 * This test bucket attempts to simulate what a controller implementation would do to fail tasks over
 * to another instance when the instance on which they are currently running goes down.
 * For this oversimplified scenario, we don't actually have multiple servers, just multiple
 * persistent executor instances on a single server. We simulate the controller with a user feature
 * that use declaratives services to keep track of all of the active persistent executor instances
 * on the server. We can simulate an instance going down by removing it from the configuration.
 */
@RunWith(FATRunner.class)
public class SimulatedControllerTest {
	private static final String APP_NAME = "persistctrltest";
	
    private static final Set<String> appNames = Collections.singleton(APP_NAME);

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
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/persistctrltest?" + queryString);
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

        // Update the server's configuration file if the bootstrapping.properties was
        // populated with relevant database information.
        originalConfig.updateDatabaseArtifacts();
        server.updateServerConfiguration(originalConfig);
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
            server.stopServer("CWWKC1556W",
                              "CWWKC1101E.*test.concurrent.persistent.fat.simctrl.SimulatedController$FinderTask"); // Not always possible to prevent FinderTask from running during shutdown
    }

    /**
     * Use an execution-disabled persistent executor to schedule a task.
     * Verify that the controller can schedule the task to a different instance, where it runs successfully.
     */
    @Test
    public void testExecutorThatSchedulesTasksButDoesNotRunThem() throws Exception {
        try {
            // Create a persistent executor that may schedule tasks but cannot run them
            // <persistentExecutor id="executor5" jndiName="concurrent/executor5" enableTaskExecution="false" .../>
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor executor5 = new PersistentExecutor();
            executor5.setId("executor5");
            executor5.setJndiName("concurrent/executor5");
            executor5.setTaskStoreRef(config.getPersistentExecutors().get(1).getTaskStoreRef());
            executor5.setEnableTaskExecution("false");
            config.getPersistentExecutors().add(executor5);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            StringBuilder output = runInServlet(
                                                "test=testScheduleRepeatingTask&jndiName=concurrent/executor5&initialDelay=0&interval=50000&invokedBy=testExecutorThatSchedulesTasksButDoesNotRunThem");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            runInServlet("test=testVerifyTaskRunsAndCancel&jndiName=concurrent/executor5&taskId=" + taskId + "&invokedBy=testExecutorThatSchedulesTasksButDoesNotRunThem");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Schedule a task from one persistentExecutor instance.
     * Remove the instance, and verify that it continues running on a second instance.
     * Restore the first instance and remove the second. Verify the task continues running.
     */
    @Test
    public void testFailoverBetweenTwoInstances() throws Exception {
        StringBuilder output = runInServlet(
                                            "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=2000&invokedBy=testFailoverBetweenTwoInstances");
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

            runInServlet("test=testTaskIsRunning&jndiName=concurrent/executor2&taskId=" + taskId + "&invokedBy=testFailoverBetweenTwoInstances");

            // Simulate the first instance coming back up
            config.getPersistentExecutors().add(executor1);

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testTaskIsRunning&jndiName=concurrent/executor1&taskId=" + taskId + "&invokedBy=testFailoverBetweenTwoInstances");

            // Simulate the second instance going down (fail back to first instance)
            config.getPersistentExecutors().removeById("executor2");

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testTaskIsRunning&jndiName=concurrent/executor1&taskId=" + taskId + "&invokedBy=testFailoverBetweenTwoInstances");

            runInServlet("test=testRemoveTask&jndiName=concurrent/executor1&taskId=" + taskId + "&invokedBy=testFailoverBetweenTwoInstances");
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
        StringBuilder output = runInServlet(
        					  "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&taskName=FW4I-A&initialDelay=0&interval=2500&invokedBy=testFailoverWithFourInstances");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdA = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        output = runInServlet(
                              "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&taskName=FW4I-B&initialDelay=0&interval=2400&invokedBy=testFailoverWithFourInstances");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdB = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        output = runInServlet(
                              "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&taskName=FW4I-C&initialDelay=0&interval=2300&invokedBy=testFailoverWithFourInstances");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdC = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        output = runInServlet(
                              "test=testScheduleRepeatingTask&jndiName=concurrent/executor2&taskName=FW4I-D&initialDelay=0&interval=2200&invokedBy=testFailoverWithFourInstances");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskIdD = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        output = runInServlet(
                              "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&taskName=FW4I-E&initialDelay=0&interval=2100&invokedBy=testFailoverWithFourInstances");
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
            PersistentExecutor executor4 = (PersistentExecutor) executor2.clone();
            executor4.setId("executor4");
            executor4.setJndiName("concurrent/executor4");
            config.getPersistentExecutors().add(executor4);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testTasksAreRunning&jndiName=concurrent/executor3&taskId="
                         + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC + "&taskId=" + taskIdD + "&taskId=" + taskIdE +
                         "&invokedBy=testFailoverWithFourInstances");

            runInServlet("test=testCancelTasks&jndiName=concurrent/executor4&pattern=FW4I-_&state=ENDED&inState=false&numCancelsExpected=5&invokedBy=testFailoverWithFourInstances");

            // Query for partition entries
            runInServlet("test=testFindPartitions&jndiName=concurrent/executor3&executorId=executor1&executorId=executor2&executorId=executor3&executorId=executor4&invokedBy=testFailoverWithFourInstances");

            // Remove one of the partition entries
            runInServlet("test=testRemovePartitions&jndiName=concurrent/executor3&executorId=executor3&libertyServer=com.ibm.ws.concurrent.persistent.fat.simctrl&expectedUpdateCount=1&invokedBy=testFailoverWithFourInstances");
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
        StringBuilder output = runInServlet(
                                            "test=testScheduleRepeatingTask&jndiName=concurrent/executor1&initialDelay=0&interval=1000&invokedBy=testTaskVisibleToBothExecutorInstances");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        runInServlet("test=testTaskIsRunning&jndiName=concurrent/executor2&taskId=" + taskId + "&invokedBy=testTaskVisibleToBothExecutorInstances");

        runInServlet("test=testRemoveTask&jndiName=concurrent/executor2&taskId=" + taskId + "&invokedBy=testTaskVisibleToBothExecutorInstances");
    }
}