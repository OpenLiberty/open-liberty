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
package com.ibm.ws.concurrent.persistent.fat.configupd;

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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.annotation.AllowedFFDC;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for configuration updates to persistent scheduled executors
 */
@RunWith(FATRunner.class)
public class PersistentExecutorConfigUpdateTest {
    private static final Set<String> appNames = Collections.singleton("persistcfgtest");

    public static final String APP_NAME = "persistcfgtest";

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
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/persistcfgtest?" + queryString);
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
        server.startServer();
    }

    /**
     * After completing all tests, stop the server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(
                    "CWNEN1000E",
                    "CWWKC1556W" // Execution of tasks deferred during config update
                    );
        }
    }

    /**
     * Schedule a repeating task. Switch to enableTaskExecution=false, then back to enableTaskExecution=true. Verify the task runs.
     */
    @Test
    public void testDisableAndEnableTaskExecution() throws Exception {
        StringBuilder output = runInServlet(
                                            "test=testScheduleRepeatingTask&jndiName=concurrent/MyExecutor&initialDelay=0&interval=1000&invokedBy=testDisableAndEnableTaskExecution");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        try {
            // change enableTaskExecution=false
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor myExecutor = config.getPersistentExecutors().getById("MyExecutor");
            myExecutor.setEnableTaskExecution("false");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // change enableTaskExecution=true
            myExecutor.setEnableTaskExecution("true");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testTaskIsRunning&jndiName=concurrent/MyExecutor&taskId=" + taskId + "&invokedBy=testDisableAndEnableTaskExecution");

            runInServlet("test=testRemoveTask&jndiName=concurrent/MyExecutor&taskId=" + taskId + "&invokedBy=testDisableAndEnableTaskExecution");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Starting with unlimited retries, schedule a task that always fails. Update config with 0 retries and make sure the task ends with failure.
     */
    @Test
    public void testDisableRetries() throws Exception {
        try {
            // Disable propagation of jeeMetaDataContext
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor myExecutor = config.getPersistentExecutors().getById("MyExecutor");
            myExecutor.setContextServiceRef("ClassloaderContextOnly");
            myExecutor.setRetryLimit("-1"); // unlimited retries
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Schedule a task that will fail when it runs
            StringBuilder output = runInServlet(
                                                "test=testScheduleRepeatingTask&jndiName=concurrent/MyExecutor&initialDelay=0&interval=500&invokedBy=testDisableRetries");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            // Reduce the number of retries to 0
            myExecutor.setRetryLimit("0");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testTaskFailsWithLookupError&jndiName=concurrent/MyExecutor&taskId=" + taskId + "&invokedBy=testDisableRetries");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * With task execution disabled, schedule a task.
     * Re-enable task execution with initialPollDelay=-1 (no polling), pollInterval=-1.
     * Update config with initialPollDelay=0 (immediate). Verify the task runs.
     */
    @Test
    public void testEnablePolling() throws Exception {
        try {
            // Disable execution of tasks
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor myExecutor = config.getPersistentExecutors().getById("MyExecutor");
            myExecutor.setEnableTaskExecution("false");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Schedule a task
            StringBuilder output = runInServlet(
                                                "test=testScheduleRepeatingTask&jndiName=concurrent/MyExecutor&initialDelay=0&interval=500&invokedBy=EnablePolling");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of first scheduled task not found in servlet output: " + output);
            String taskIdA = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            // Schedule another task
            output = runInServlet(
                                  "test=testScheduleRepeatingTask&jndiName=concurrent/MyExecutor&initialDelay=100&interval=200&invokedBy=EnablePolling");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of second scheduled task not found in servlet output: " + output);
            String taskIdB = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            // Disable polling and re-enable task execution.
            myExecutor.setEnableTaskExecution("true");
            myExecutor.setInitialPollDelay("-1");
            myExecutor.setPollInterval("-1");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Reduce the initial poll delay to immediate, set the poll size to 1, and verify the first task runs
            myExecutor.setInitialPollDelay("0");
            myExecutor.setPollSize("1");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testTaskIsRunning&jndiName=concurrent/MyExecutor&taskId=" + taskIdA + "&invokedBy=EnablePolling");
            runInServlet("test=testTaskDidNotRun&jndiName=concurrent/MyExecutor&taskId=" + taskIdB + "&invokedBy=EnablePolling");

            // Increase the poll and verify the second task runs
            myExecutor.setPollSize("20");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testTaskIsRunning&jndiName=concurrent/MyExecutor&taskId=" + taskIdB + "&invokedBy=EnablePolling");

            runInServlet("test=testRemoveTask&jndiName=concurrent/MyExecutor&taskId=" + taskIdA + "&invokedBy=EnablePolling");
            runInServlet("test=testRemoveTask&jndiName=concurrent/MyExecutor&taskId=" + taskIdB + "&invokedBy=EnablePolling");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Schedule a task. Disable the MicroProfile Context Propagation feature. Verify that the task keeps running.
     */
    @Test
    public void testMPContextPropagationDisabled() throws Exception {
        String taskId;
        try {
            // Enable MicroProfile Context Propagation
            ServerConfiguration config = originalConfig.clone();
            config.getFeatureManager().getFeatures().add("mpContextPropagation-1.0");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Schedule a task
            StringBuilder output = runInServlet(
                    "test=testScheduleRepeatingTask&jndiName=concurrent/MyExecutor&initialDelay=0&interval=1300&invokedBy=testMPContextPropagationDisabled");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));
        } finally {
            // Disable MicroProfile Context Propagation by restoring original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }

        // Verify that the task still executes
        runInServlet("test=testTaskIsRunning&jndiName=concurrent/MyExecutor&taskId=" + taskId + "&invokedBy=testMPContextPropagationDisabled");

        runInServlet("test=testRemoveTask&jndiName=concurrent/MyExecutor&taskId=" + taskId + "&invokedBy=testMPContextPropagationDisabled");
    }

    /**
     * With task execution disabled, schedule a task.
     * Re-enable task execution with initialPollDelay=10 days, pollInterval=3 days.
     * Update config with initialPollDelay=100ms. Verify the task runs.
     */
    @Test
    public void testReduceTheInitialPollDelay() throws Exception {
        try {
            // Disable execution of tasks
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor myExecutor = config.getPersistentExecutors().getById("MyExecutor");
            myExecutor.setEnableTaskExecution("false");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Schedule a task
            StringBuilder output = runInServlet(
                                                "test=testScheduleRepeatingTask&jndiName=concurrent/MyExecutor&initialDelay=0&interval=500&invokedBy=testReduceTheInitialPollDelay");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            // Set the initial poll delay to a lengthy amount of time and re-enable task execution.
            myExecutor.setEnableTaskExecution("true");
            myExecutor.setInitialPollDelay("10d");
            myExecutor.setPollInterval("3d");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Reduce the initial poll delay to 100ms and verify the task runs
            myExecutor.setInitialPollDelay("100ms");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testTaskIsRunning&jndiName=concurrent/MyExecutor&taskId=" + taskId + "&invokedBy=testReduceTheInitialPollDelay");

            runInServlet("test=testRemoveTask&jndiName=concurrent/MyExecutor&taskId=" + taskId + "&invokedBy=testReduceTheInitialPollDelay");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Starting with retry limit 10, retrying every 2 seconds, schedule a task that always fails. Update config with 10ms retry interval and make sure the task ends with failure.
     */
    @AllowedFFDC("javax.naming.NamingException")
    @Test
    public void testReduceTheRetryInterval() throws Exception {
        try {
            // Disable propagation of jeeMetaDataContext
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor myExecutor = config.getPersistentExecutors().getById("MyExecutor");
            myExecutor.setContextServiceRef("ClassloaderContextOnly");
            myExecutor.setRetryLimit("10");
            myExecutor.setRetryInterval("2s");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Schedule a task that will fail when it runs
            StringBuilder output = runInServlet(
                                                "test=testScheduleRepeatingTask&jndiName=concurrent/MyExecutor&initialDelay=0&interval=500&invokedBy=testReduceTheRetryInterval");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            // Reduce the retry interval to 10ms
            myExecutor.setRetryInterval("10ms");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testTaskFailsWithLookupError&jndiName=concurrent/MyExecutor&taskId=" + taskId + "&expectedNumAttempts=11&invokedBy=testReduceTheRetryInterval");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Schedule a repeating task that does a java:comp lookup, which should work fine when DefaultContextService is used.
     * Update configuration to specify a context service with only classloaderContext.
     * Verify the task runs successfully, then cancel it.
     * Schedule a task that does a java:comp lookup. Verify it fails.
     */
    @AllowedFFDC("javax.naming.NamingException")
    @Test
    public void testReplaceTheContextService() throws Exception {
        StringBuilder output = runInServlet(
                                            "test=testScheduleRepeatingTask&jndiName=concurrent/MyExecutor&initialDelay=0&interval=1000&invokedBy=testReplaceTheContextService");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        try {
            // Disable propagation of jeeMetaDataContext
            ServerConfiguration config = originalConfig.clone();
            PersistentExecutor myExecutor = config.getPersistentExecutors().getById("MyExecutor");
            myExecutor.setContextServiceRef("ClassloaderContextOnly");
            myExecutor.setRetryLimit("0");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Verify the original task still runs
            runInServlet("test=testTaskIsRunning&jndiName=concurrent/MyExecutor&taskId=" + taskId + "&invokedBy=testReplaceTheContextService");

            // Cancel the original task.
            runInServlet("test=testRemoveTask&jndiName=concurrent/MyExecutor&taskId=" + taskId + "&invokedBy=testReplaceTheContextService");

            // Schedule another task.
            output = runInServlet(
                                  "test=testScheduleRepeatingTask&jndiName=concurrent/MyExecutor&initialDelay=0&interval=3000&invokedBy=testReplaceTheContextService");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of second task not found in servlet output: " + output);
            taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            // Verify it fails because it cannot lookup a resource reference
            runInServlet("test=testTaskFailsWithLookupError&jndiName=concurrent/MyExecutor&taskId=" + taskId + "&invokedBy=testReplaceTheContextService");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }
}
