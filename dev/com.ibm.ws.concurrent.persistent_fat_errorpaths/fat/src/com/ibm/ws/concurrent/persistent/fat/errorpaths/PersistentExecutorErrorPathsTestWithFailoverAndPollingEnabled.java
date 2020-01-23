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
package com.ibm.ws.concurrent.persistent.fat.errorpaths;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for error paths in persistent scheduled executor
 */
@RunWith(FATRunner.class)
public class PersistentExecutorErrorPathsTestWithFailoverAndPollingEnabled {

    private static final LibertyServer server = FATSuite.server;
    
    private static final String APP_NAME = "persistenterrtest";

    private static ServerConfiguration originalConfig;

    @Rule
    public TestName testName = new TestName();

    /**
     * Runs a test in the servlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    protected StringBuilder runInServlet(String test) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/persistenterrtest?test=" + test);
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

    @BeforeClass
    public static void setUp() throws Exception {
        originalConfig = server.getServerConfiguration();
        ServerConfiguration config = originalConfig.clone();

        PersistentExecutor persistentExecutor = config.getPersistentExecutors().getBy("jndiName", "concurrent/myScheduler");
        persistentExecutor.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
        persistentExecutor.setMissedTaskThreshold("5s");
        persistentExecutor.setPollInterval("3s");
        persistentExecutor.setInitialPollDelay("1s");

        PersistentExecutor belowMinMissedTaskThresholdExecutor = new PersistentExecutor();
        belowMinMissedTaskThresholdExecutor.setId("belowMinMissedTaskThresholdExecutor");
        belowMinMissedTaskThresholdExecutor.setJndiName("concurrent/belowMinMissedTaskThreshold");
        belowMinMissedTaskThresholdExecutor.setTaskStoreRef("DBTaskStore");
        belowMinMissedTaskThresholdExecutor.setMissedTaskThreshold("99s");
        belowMinMissedTaskThresholdExecutor.setPollInterval("30m");
        belowMinMissedTaskThresholdExecutor.setInitialPollDelay("-1");
        config.getPersistentExecutors().add(belowMinMissedTaskThresholdExecutor);

        PersistentExecutor belowMinPollIntervalExecutor = new PersistentExecutor();
        belowMinPollIntervalExecutor.setId("belowMinPollIntervalExecutor");
        belowMinPollIntervalExecutor.setJndiName("concurrent/belowMinPollInterval");
        belowMinPollIntervalExecutor.setTaskStoreRef("DBTaskStore");
        belowMinPollIntervalExecutor.setMissedTaskThreshold("100s");
        belowMinPollIntervalExecutor.setPollInterval("1m38s");
        belowMinPollIntervalExecutor.setInitialPollDelay("-1");
        config.getPersistentExecutors().add(belowMinPollIntervalExecutor);

        PersistentExecutor exceedsMaxMissedTaskThresholdExecutor = new PersistentExecutor();
        exceedsMaxMissedTaskThresholdExecutor.setId("exceedsMaxMissedTaskThresholdExecutor");
        exceedsMaxMissedTaskThresholdExecutor.setJndiName("concurrent/exceedsMaxMissedTaskThreshold");
        exceedsMaxMissedTaskThresholdExecutor.setTaskStoreRef("DBTaskStore");
        exceedsMaxMissedTaskThresholdExecutor.setMissedTaskThreshold("2h30m1s");
        exceedsMaxMissedTaskThresholdExecutor.setPollInterval("30m");
        exceedsMaxMissedTaskThresholdExecutor.setInitialPollDelay("-1");
        config.getPersistentExecutors().add(exceedsMaxMissedTaskThresholdExecutor);

        PersistentExecutor exceedsMaxPollIntervalExecutor = new PersistentExecutor();
        exceedsMaxPollIntervalExecutor.setId("exceedsMaxPollIntervalExecutor");
        exceedsMaxPollIntervalExecutor.setJndiName("concurrent/exceedsMaxPollIntervalExecutor");
        exceedsMaxPollIntervalExecutor.setTaskStoreRef("DBTaskStore");
        exceedsMaxPollIntervalExecutor.setMissedTaskThreshold("2h");
        exceedsMaxPollIntervalExecutor.setPollInterval("2h31m");
        exceedsMaxPollIntervalExecutor.setInitialPollDelay("-1");
        config.getPersistentExecutors().add(exceedsMaxPollIntervalExecutor);

        PersistentExecutor retryIntervalAndMissedTaskThresholdBothEnabledExecutor = new PersistentExecutor();
        retryIntervalAndMissedTaskThresholdBothEnabledExecutor.setId("retryIntervalAndMissedTaskThresholdBothEnabled");
        retryIntervalAndMissedTaskThresholdBothEnabledExecutor.setJndiName("concurrent/retryIntervalAndMissedTaskThresholdBothEnabled");
        retryIntervalAndMissedTaskThresholdBothEnabledExecutor.setTaskStoreRef("DBTaskStore");
        retryIntervalAndMissedTaskThresholdBothEnabledExecutor.setMissedTaskThreshold("145s");
        retryIntervalAndMissedTaskThresholdBothEnabledExecutor.setPollInterval("28m");
        retryIntervalAndMissedTaskThresholdBothEnabledExecutor.setRetryInterval("3m14s");
        retryIntervalAndMissedTaskThresholdBothEnabledExecutor.setInitialPollDelay("-1");
        config.getPersistentExecutors().add(retryIntervalAndMissedTaskThresholdBothEnabledExecutor);

        config.getDataSources().getById("SchedDB").getConnectionManagers().get(0).setMaxPoolSize("10");
        server.updateServerConfiguration(config);

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
        if (server != null)
            try {
                if (server.isStarted())
                    server.stopServer(".*");
            } finally {
                server.updateServerConfiguration(originalConfig);
            }
    }

    @Test
    public void testFailingTaskAndAutoPurgeFEWithPolling() throws Exception {
        runInServlet("testFailingTaskAndAutoPurge");
    }

    @Test
    public void testFailingTaskNoAutoPurgeFEWithPolling() throws Exception {
        runInServlet("testFailingTaskNoAutoPurge");
    }

    @Test
    public void testFailOnceAndSkipFirstRetryAndAutoPurgeFEWithPolling() throws Exception {
        runInServlet("testFailOnceAndSkipFirstRetryAndAutoPurge");
    }

    @Test
    public void testFailOnceAndSkipFirstRetryNoAutoPurgeFEWithPolling() throws Exception {
        runInServlet("testFailOnceAndSkipFirstRetryNoAutoPurge");
    }

    /**
     * Verify that pending/active task ids, plus other helpful information, appears in the server dump output.
     */
    @Mode(TestMode.FULL)
    @Test
    public void testIntrospectorWithFailOverEnabled() throws Exception {
        // schedule some tasks that will remain active while the introspector output is recorded
        StringBuilder response = runInServlet("testScheduleDistantIntrospectableTasks");
        int i = response.indexOf("TASKS ARE ");
        if (i < 0)
            throw new Exception("Start of task list not found in output: " + response);
        int end = response.indexOf(".", i);
        if (end < 0)
            throw new Exception("End of task list not found in output: " + response);

        String[] scheduledDistantTasks = response.substring(i + "TASKS ARE ".length(), end).split(",");

        response = runInServlet("testScheduleFrequentIntrospectableTasks");
        i = response.indexOf("TASKS ARE ");
        if (i < 0)
            throw new Exception("Start of task list not found in output: " + response);
        end = response.indexOf(".", i);
        if (end < 0)
            throw new Exception("End of task list not found in output: " + response);

        String[] scheduledFrequentTasks = response.substring(i + "TASKS ARE ".length(), end).split(",");

        boolean successful = false;
        try {
            // Request a dump of the server
            List<String> output = FATSuite.persistentExecutorIntrospectorDump();

            // Validate contents of dump
            String found = null;
            for (String line : output)
                if (line.contains("concurrent/myScheduler")) {
                    found = line;
                    if (line.toLowerCase().contains("deactivated"))
                        throw new Exception("Persistent executor should still be active. " + output);
                    break;
                }
            if (found == null)
                throw new Exception("Persistent executor JNDI name is absent from server dump output: " + output);

            found = null;
            for (String line : output)
                if (line.contains("Partition 0")) {
                    found = line;
                    break;
                }
            if (found == null)
                throw new Exception("Persistent executor partition must show as 0 in server dump output when fail over is enabled " + output);

            found = null;
            for (String line : output)
                if (line.contains("missedTaskThreshold=5s")) {
                    found = line;
                    break;
                }
            if (found == null)
                throw new Exception("Persistent executor config is absent from server dump output " + output);

            found = null;
            for (String line : output)
                if (line.contains("Accessed from [persistenterrtest]")) {
                    found = line;
                    break;
                }
            if (found == null)
                throw new Exception("Application that used persistent executor is absent from server dump output " + output);

            found = null;
            for (String line : output)
                if (line.contains("persistenterrtest is STARTED")) {
                    found = line;
                    break;
                }
            if (found == null)
                throw new Exception("State of application that used persistent executor is absent from server dump output " + output);

            found = null;
            for (String line : output)
                if (line.contains("In-memory")) {
                    found = line;
                    int b1 = line.indexOf('[');
                    int b2 = line.indexOf(']');
                    if (b1 < 0 || b2 < 0 || b2 < b1)
                        throw new Exception("In-memory task list not found in server dump output " + output);
                    String[] foundTasks = line.substring(b1 + 1, b2).split(", ");
                    List<String> foundTasksList = Arrays.asList(foundTasks);
                    for (String taskId : scheduledFrequentTasks)
                        if (!foundTasksList.contains(taskId))
                            throw new Exception("Scheduled tasks " + Arrays.toString(scheduledFrequentTasks) + " are not all found in server dump output: " + output);
                    for (String taskId : scheduledDistantTasks)
                        if (foundTasksList.contains(taskId))
                            throw new Exception("Scheduled task " + taskId + " should not be found in server dump output " +
                                "because it is scheduled for the distant future and not claimed by this instance " + output);
                    break;
                }
            if (found == null)
                throw new Exception("In-memory tasks are absent from server dump output " + output);

            successful = true;
        } finally {
            // Cancel tasks that we created during this test
            try {
                StringBuilder removalRequest = new StringBuilder("testRemoveTasks");
                for (String taskId : scheduledDistantTasks)
                    removalRequest.append("&taskId=").append(taskId);
                for (String taskId : scheduledFrequentTasks)
                    removalRequest.append("&taskId=").append(taskId);
                runInServlet(removalRequest.toString());
            } catch (Exception x) {
                if (successful)
                    throw x;
                // else allow the original failure to be raised
            }
        }
    }

    /**
     * testMissedTaskThresholdBelowMinimum - attempt to use a persistent executor where the missedTaskThreshold value is less than
     * the minimum allowed. Expect IllegalArgumentException with a translatable message.
     */
    @Test
    public void testMissedTaskThresholdBelowMinimum() throws Exception {
        server.setMarkToEndOfLog();

        runInServlet("testMissedTaskThresholdBelowMinimum");

        List<String> errorMessages = server.findStringsInLogsUsingMark("CWWKE0701E.*99s", server.getConsoleLogFile());
        if (errorMessages.isEmpty())
            throw new Exception("Error message not found in log.");

        String errorMessage = errorMessages.get(0);

        if (!errorMessage.contains("IllegalArgumentException")
                || !errorMessage.contains("CWWKC1520E")
                || !errorMessage.contains("missedTaskThreshold")
                || !errorMessage.contains("100s")
                || !errorMessage.contains("2h30m"))
            throw new Exception("Problem with substitution parameters in message " + errorMessage);
    }

    /**
     * testMissedTaskThresholdExceedsMaximum - attempt to use a persistent executor where the missedTaskThreshold value exceeds
     * the maximum allowed. Expect IllegalArgumentException with a translatable message.
     */
    @Test
    public void testMissedTaskThresholdExceedsMaximum() throws Exception {
        server.setMarkToEndOfLog();

        runInServlet("testMissedTaskThresholdExceedsMaximum");

        List<String> errorMessages = server.findStringsInLogsUsingMark("CWWKE0701E.*9001s", server.getConsoleLogFile());
        if (errorMessages.isEmpty())
            throw new Exception("Error message not found in log.");

        String errorMessage = errorMessages.get(0);

        if (!errorMessage.contains("IllegalArgumentException")
                || !errorMessage.contains("CWWKC1520E")
                || !errorMessage.contains("missedTaskThreshold")
                || !errorMessage.contains("100s")
                || !errorMessage.contains("2h30m"))
            throw new Exception("Problem with substitution parameters in message " + errorMessage);
    }

    /**
     * testPollIntervalBelowMinimum - attempt to use a persistent executor where the pollInterval value is less than
     * the minimum allowed. Expect IllegalArgumentException with a translatable message.
     */
    @Test
    public void testPollIntervalBelowMinimum() throws Exception {
        server.setMarkToEndOfLog();

        runInServlet("testPollIntervalBelowMinimum");

        List<String> errorMessages = server.findStringsInLogsUsingMark("CWWKE0701E.*98s", server.getConsoleLogFile());
        if (errorMessages.isEmpty())
            throw new Exception("Error message not found in log.");

        String errorMessage = errorMessages.get(0);

        if (!errorMessage.contains("IllegalArgumentException")
                || !errorMessage.contains("CWWKC1520E")
                || !errorMessage.contains("pollInterval")
                || !errorMessage.contains("100s")
                || !errorMessage.contains("2h30m"))
            throw new Exception("Problem with substitution parameters in message " + errorMessage);
    }

    /**
     * testPollIntervalExceedsMaximum - attempt to use a persistent executor where the pollInterval value exceeds
     * the maximum allowed. Expect IllegalArgumentException with a translatable message.
     */
    @Test
    public void testPollIntervalExceedsMaximum() throws Exception {
        server.setMarkToEndOfLog();

        runInServlet("testPollIntervalExceedsMaximum");

        List<String> errorMessages = server.findStringsInLogsUsingMark("CWWKE0701E.*151m", server.getConsoleLogFile());
        if (errorMessages.isEmpty())
            throw new Exception("Error message not found in log.");

        String errorMessage = errorMessages.get(0);

        if (!errorMessage.contains("IllegalArgumentException")
                || !errorMessage.contains("CWWKC1520E")
                || !errorMessage.contains("pollInterval")
                || !errorMessage.contains("100s")
                || !errorMessage.contains("2h30m"))
            throw new Exception("Problem with substitution parameters in message " + errorMessage);
    }

    @Test
    public void testResultFailsToSerializeFEWithPolling() throws Exception {
        runInServlet("testResultFailsToSerialize");
    }

    @Test
    public void testResultSerializationFailureFailsToSerializeFEWithPolling() throws Exception {
        runInServlet("testResultSerializationFailureFailsToSerialize");
    }

    @Test
    public void testRetryFailedTaskAndAutoPurgeFEWithPolling() throws Exception {
        runInServlet("testRetryFailedTaskAndAutoPurge");
    }

    @Test
    public void testRetryFailedTaskNoAutoPurgeFEWithPolling() throws Exception {
        runInServlet("testRetryFailedTaskNoAutoPurge");
    }

    /**
     * testRetryIntervalAndMissedTaskThresholdBothEnabled - attempt to use a persistent executor where the retryInterval and
     * the missedTaskThreshold are both configured. Expect IllegalArgumentException with a translatable message.
     */
    @Test
    public void testRetryIntervalAndMissedTaskThresholdBothEnabled() throws Exception {
        server.setMarkToEndOfLog();

        runInServlet("testRetryIntervalAndMissedTaskThresholdBothEnabled");

        List<String> errorMessages = server.findStringsInLogsUsingMark("CWWKE0701E.*CWWKC1521E", server.getConsoleLogFile());
        if (errorMessages.isEmpty())
            throw new Exception("Error message not found in log.");

        String errorMessage = errorMessages.get(0);

        if (!errorMessage.contains("IllegalArgumentException")
                || !errorMessage.contains("CWWKC1521E")
                || !errorMessage.contains("retryInterval")
                || !errorMessage.contains("missedTaskThreshold"))
            throw new Exception("Problem with substitution parameters in message " + errorMessage);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testShutDownDerbyBeforeTaskExecutionFEWithPolling() throws Exception {
        runInServlet("testShutDownDerbyBeforeTaskExecution");
    }

    @Test
    public void testShutDownDerbyDuringTaskExecutionFEWithPolling() throws Exception {
        runInServlet("testShutDownDerbyDuringTaskExecution");
    }

    @Test
    public void testSkipRunFailsOnFirstExecutionAttemptFEWithPolling() throws Exception {
        runInServlet("testSkipRunFailsOnFirstExecutionAttempt");
    }

    @Test
    public void testSkipRunFailsOnLastExecutionAttemptFEWithPolling() throws Exception {
        runInServlet("testSkipRunFailsOnLastExecutionAttempt");
    }

    @Test
    public void testSkipRunFailsOnLastExecutionAttemptNoAutoPurgeFEWithPolling() throws Exception {
        runInServlet("testSkipRunFailsOnLastExecutionAttemptNoAutoPurge");
    }

    @Test
    public void testSkipRunFailsOnMiddleExecutionAttemptsFEWithPolling() throws Exception {
        runInServlet("testSkipRunFailsOnMiddleExecutionAttempts");
    }

    @Test
    public void testSkipRunFailsOnOnlyExecutionAttemptFEWithPolling() throws Exception {
        runInServlet("testSkipRunFailsOnOnlyExecutionAttempt");
    }

    @Test
    public void testSkipRunFailsOnOnlyExecutionAttemptNoAutoPurgeFEWithPolling() throws Exception {
        runInServlet("testSkipRunFailsOnOnlyExecutionAttemptNoAutoPurge");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTransactionTimeoutFEWithPolling() throws Exception {
        runInServlet("testTransactionTimeout");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTransactionTimeoutSuspendedTransactionFEWithPolling() throws Exception {
        runInServlet("testTransactionTimeoutSuspendedTransaction");
    }

    @Test
    public void testTriggerFailsGetNextRunTimeAfterTaskRunsFEWithPolling() throws Exception {
        runInServlet("testTriggerFailsGetNextRunTimeAfterTaskRuns");
    }
}