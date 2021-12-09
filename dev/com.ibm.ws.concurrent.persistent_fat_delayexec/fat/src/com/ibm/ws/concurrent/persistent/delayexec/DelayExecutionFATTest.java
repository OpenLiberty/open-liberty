/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.delayexec;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;

@AllowedFFDC // various FFDCs can occur when task execution overlaps a config update
@RunWith(FATRunner.class)
public class DelayExecutionFATTest {

    private static final Set<String> appNames = Collections.singleton("DelayExecution");
    
    private static final String APP_NAME = "DelayExecution";

    private static ServerConfiguration originalConfig;

    private static final LibertyServer server = FATSuite.server;

    private static final String TASK_ID_SEARCH_TEXT = "Task id is ";

    private static boolean createTablesDriven = false;

    private static final String CONFIG_TIMEOUT_MESSAGE_REGX = "CWWKG0027W:.*";

    private static final String SERVER_QUIESCE_WAIT_MESSAGE_REGX = "CWWKE1102W:.*";

    final String SERVICE_ACTIVATION = "test.concurrent.persistent.delayexec.internal.TestServiceImpl.activate.Entry";

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
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/DelayExecution?" + queryString);
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
    }

    /**
     * After completing all tests, stop the server.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted())
            server.stopServer(true, SERVER_QUIESCE_WAIT_MESSAGE_REGX, CONFIG_TIMEOUT_MESSAGE_REGX);
    }

    @Test
    //@Mode(TestMode.LITE)
    @Mode(TestMode.QUARANTINE)
    public void testRescheduleUnderConfigUpdateRemove_Lite() throws Exception {
        // Make sure createTables() is driven outside of test.  
        submitAndRemoveTask();

        // Drive the target test 5 times.  The number of 5 iterations was determined based on failure detection
        // from previous versions of this FAT based on different RuntimeUpdateListener notifications.
        testRescheduleUnderConfigUpdateRemove(5);
    }

    @Test
    @Mode(TestMode.LITE)
    public void testRescheduleUnderConfigUpdateRun_Lite() throws Exception {
        // Make sure createTables() is driven outside of test.  
        submitAndRemoveTask();

        // Drive the target test 5 times.  The number of 5 iterations was determined based on failure detection
        // from previous versions of this FAT based on different RuntimeUpdateListener notifications.
        testRescheduleUnderConfigUpdateRun(5);
    }

    @Test
    @Mode(TestMode.FULL)
    public void testRescheduleUnderConfigUpdateRemove_Full() throws Exception {
        // Make sure createTables() is driven outside of test.  
        submitAndRemoveTask();

        // Drive the target test 20 times.
        testRescheduleUnderConfigUpdateRemove(20);
    }

    @Test
    @Mode(TestMode.FULL)
    public void testRescheduleUnderConfigUpdateRun_Full() throws Exception {
        // Make sure createTables() is driven outside of test.  
        submitAndRemoveTask();

        // Drive the target test 20 times.
        testRescheduleUnderConfigUpdateRun(20);
    }

    /**
     * Schedules Tasks to run while under a configuration update. The configuration update
     * is hung-up by sleeping within the notification call from a RuntimeUpdateListener.
     * The scheduled Tasks are removed during the configuration update hang.
     * 
     * @param numberOfIterations
     * @throws Exception
     */
    public void testRescheduleUnderConfigUpdateRemove(int numberOfIterations) throws Exception {

        String taskIdA;
        String taskIdB;
        String taskIdC;

        for (int i = 1; i <= numberOfIterations; i++) {
            // This server's state may become bound-up after we hang the configuration updates.  Recycle the 
            // server to ensure a clean start to our test.
            recycleServer("testRescheduleUnderConfigUpdateRemove_" + i + ".log");

            // Update configuration with user feature that will sleep in its com.ibm.ws.runtime.update.RuntimeUpdateListener#notificationCreated method to cause a 
            // configuration update delay for use to submit task(s) in the this window.
            ServerConfiguration config = originalConfig.clone();
            config.getFeatureManager().getFeatures().add("testFeature-1.0");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);
            Log.info(getClass(), "testRescheduleUnderConfigUpdateRemove", "added testFeature-1.0 to feature list");

            // Wait for evidence that the TestService has activated
            List<String> activation = server.findStringsInLogs(SERVICE_ACTIVATION);
            if (activation == null || activation.isEmpty()) {
                String activated = server.waitForStringInLog(SERVICE_ACTIVATION);
                if (activated == null) {
                    fail("The TestService did not activate.");
                }
            }
            // Update the configuration for the test feature to cause delay	   
            if (config.getFeatureManager().getFeatures().remove("osgiConsole-1.0") == false) {
                config.getFeatureManager().getFeatures().add("osgiConsole-1.0");
            }
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);

            taskIdA = taskIdB = taskIdC = null;
            try {
                Thread.sleep(1000); // Give time for configuration update to hang...

                long startTime = System.nanoTime();

                // Schedule tasks to run in executorservice1...hopefully they don't make it to their run()/call() methods...
                StringBuilder output = runInServlet(
                                "test=scheduleASimpleTaskNoDatabaseExecution&jndiName=concurrent/executorservice1&initialDelay=1500&interval=100&invokedBy=testRescheduleUnderConfigUpdateRemove-"
                                                + i + "A");
                int start = output.indexOf(TASK_ID_SEARCH_TEXT);
                if (start < 0)
                    throw new Exception("Task id of scheduled task not found in servlet output: " + output);
                taskIdA = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

                output = runInServlet(
                                "test=scheduleASimpleTaskNoDatabaseExecution&jndiName=concurrent/executorservice1&initialDelay=1500&interval=100&invokedBy=testRescheduleUnderConfigUpdateRemove-"
                                                + i + "B");
                start = output.indexOf(TASK_ID_SEARCH_TEXT);
                if (start < 0)
                    throw new Exception("Task id of scheduled task not found in servlet output: " + output);
                taskIdB = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

                output = runInServlet(
                                "test=scheduleASimpleTaskNoDatabaseExecution&jndiName=concurrent/executorservice1&initialDelay=1500&interval=100&invokedBy=testRescheduleUnderConfigUpdateRemove-"
                                                + i + "C");
                start = output.indexOf(TASK_ID_SEARCH_TEXT);
                if (start < 0)
                    throw new Exception("Task id of scheduled task not found in servlet output: " + output);
                taskIdC = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

                // Don't know how to get the user feature configuration value for activateDelay...currently config'ed to 30sec
                long duration = System.nanoTime() - startTime;
                long sleep = 20000 - TimeUnit.NANOSECONDS.toMillis(duration);
                if (sleep >= 0)
                    Thread.sleep(sleep); // wait 20 seconds...
                else {
                    Log.info(getClass(), "testRescheduleUnderConfigUpdateRemove", "not valid to run this iteration of the test because it took too long to schedule tasks: "
                                                                                  + duration + "ns");
                    break; // not valid to run the test
                }

                // Verify that the submitted tasks didn't run while the configuration update related to the user feature was in progress.
                runInServlet("test=testTasksHaveNotRun&jndiName=concurrent/executorservice1&taskId="
                             + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC +
                             "&invokedBy=testRescheduleUnderConfigUpdateRemove-" + i + "D");

            } finally {
                runInServlet("test=removeTask&jndiName=concurrent/executorservice1&taskId=" + taskIdA + "&invokedBy=testRescheduleUnderConfigUpdateRemove-" + i + "E");
                runInServlet("test=removeTask&jndiName=concurrent/executorservice1&taskId=" + taskIdB + "&invokedBy=testRescheduleUnderConfigUpdateRemove-" + i + "F");
                runInServlet("test=removeTask&jndiName=concurrent/executorservice1&taskId=" + taskIdC + "&invokedBy=testRescheduleUnderConfigUpdateRemove-" + i + "G");
            }
        } // end, for
    }

    /**
     * Schedules Tasks to run while under a configuration update. The configuration update
     * is hung-up by sleeping within the notification call from a RuntimeUpdateListener.
     * The scheduled Tasks are allowed to run after the configuration update hang is over.
     * 
     * @param numberOfIterations
     * @throws Exception
     */
    public void testRescheduleUnderConfigUpdateRun(int numberOfIterations) throws Exception {

        String taskIdA;
        String taskIdB;
        String taskIdC;

        for (int i = 1; i <= numberOfIterations; i++) {
            // This server's state may become bound-up after we hang the configuration updates.  Recycle the 
            // server to ensure a clean start to our test.
            recycleServer("testRescheduleUnderConfigUpdateRun_" + i + ".log");

            // Update configuration with user feature that will sleep in its com.ibm.ws.runtime.update.RuntimeUpdateListener#notificationCreated method to cause a 
            // configuration update delay for use to submit task(s) in the this window.
            ServerConfiguration config = originalConfig.clone();
            config.getFeatureManager().getFeatures().add("testFeature-1.0");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);
            Log.info(getClass(), "testRescheduleUnderConfigUpdateRun", "added testFeature-1.0 to feature list");

            // Wait for evidence that the TestService has activated
            List<String> activation = server.findStringsInLogs(SERVICE_ACTIVATION);
            if (activation == null || activation.isEmpty()) {
                String activated = server.waitForStringInLog(SERVICE_ACTIVATION);
                if (activated == null) {
                	// Generate a server dump for debug if the service does not activate
                	server.dumpServer("testRescheduleUnderConfigUpdateRun");
                    fail("The TestService did not activate.");
                }
            }

            // Update the configuration for the test feature to cause delay	   
            if (config.getFeatureManager().getFeatures().remove("osgiConsole-1.0") == false) {
                config.getFeatureManager().getFeatures().add("osgiConsole-1.0");
            }
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);

            Thread.sleep(1000); // Give time for configuration update to hang...

            long startTime = System.nanoTime();

            // Schedule tasks to run in executorservice1...hopefully they don't make it to their run()/call() methods...
            StringBuilder output = runInServlet(
                            "test=scheduleASimpleTaskNoDatabaseExecution&jndiName=concurrent/executorservice1&initialDelay=1500&interval=100&invokedBy=testRescheduleUnderConfigUpdateRun-"
                                            + i + "A");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdA = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            output = runInServlet(
                            "test=scheduleASimpleTaskNoDatabaseExecution&jndiName=concurrent/executorservice1&initialDelay=1500&interval=100&invokedBy=testRescheduleUnderConfigUpdateRun-"
                                            + i + "B");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdB = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            output = runInServlet(
                            "test=scheduleASimpleTaskNoDatabaseExecution&jndiName=concurrent/executorservice1&initialDelay=1500&interval=100&invokedBy=testRescheduleUnderConfigUpdateRun-"
                                            + i + "C");
            start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskIdC = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

            // Don't know how to get the user feature configuration value for activateDelay...currently config'ed to 30sec
            long duration = System.nanoTime() - startTime;
            long sleep = 20000 - TimeUnit.NANOSECONDS.toMillis(duration);
            if (sleep >= 0)
                Thread.sleep(sleep); // wait 20 seconds...
            else {
                Log.info(getClass(), "testRescheduleUnderConfigUpdateRun", "not valid to run this iteration of the test because it took too long to schedule tasks: "
                                                                           + duration + "ns");
                break; // not valid to run the test
            }

            // Verify that the submitted tasks didn't run while the configuration update related to the user feature was in progress.
            runInServlet("test=testTasksHaveNotRun&jndiName=concurrent/executorservice1&taskId="
                         + taskIdA + "&taskId=" + taskIdB + "&taskId=" + taskIdC +
                         "&invokedBy=testRescheduleUnderConfigUpdateRun-" + i + "D");

            // Verify that the scheduled Tasks that were delayed from the configuration update hang have now run    			
            runInServlet("test=testTaskDoesRun&jndiName=concurrent/executorservice1&taskId=" + taskIdA + "&invokedBy=testRescheduleUnderConfigUpdateRun-" + i + "E");
            runInServlet("test=testTaskDoesRun&jndiName=concurrent/executorservice1&taskId=" + taskIdB + "&invokedBy=testRescheduleUnderConfigUpdateRun-" + i + "F");
            runInServlet("test=testTaskDoesRun&jndiName=concurrent/executorservice1&taskId=" + taskIdC + "&invokedBy=testRescheduleUnderConfigUpdateRun-" + i + "G");
        } // end, for
    }

    /**
     * Use to cause the initialization of the PersistentExecutorService to drive the createTables()
     * call.
     * 
     * @throws Exception
     */
    public void submitAndRemoveTask() throws Exception {

        String taskIdA;

        if (createTablesDriven == true)
            return;
        createTablesDriven = true;

        startServerIfNotStarted("submitAndRemoveTask.log", false);

        // Schedule task to run in executorservice1...hopefully it doesn't make it to the run()/call() method...
        StringBuilder output = runInServlet(
                        "test=scheduleASimpleTaskNoDatabaseExecution&jndiName=concurrent/executorservice1&initialDelay=50000&interval=100&invokedBy=submitAndRemoveTask");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of scheduled task not found in servlet output: " + output);
        taskIdA = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        runInServlet("test=testRemoveTask&jndiName=concurrent/executorservice1&taskId=" + taskIdA + "&invokedBy=submitAndRemoveTask");
    }

    /**
     * Starts the server if not started.
     * 
     * @param forceRestart True to force a server to restart.
     * 
     * @throws Exception
     */
    public void startServerIfNotStarted(String logName, boolean forceRestart) throws Exception {
        if (!server.isStarted()) {
            server.startServer(logName);
        } else {
            if (forceRestart) {
                server.stopServer();
                server.startServer(logName);
            }
        }
    }

    /**
     * This method is called from tests that could induce hangs in the configuration update process and
     * thus expect some fail-out messages.
     * 
     * The CWWKG0027W is the timeout message issued by ConfigRefresher after
     * a 1 minute delay of waiting in its futures related to the Notifications sent out.
     * 
     * The CWWKE1102W message is observed when the quiesce server command stumbles with hung activities.
     *
     * The CWWKC1501W message indicates a task failed (in this case due to overlapping a config update)
     * and will be retried.
     * 
     * @throws Exception LibertyServer Exceptions
     */
    public void recycleServer(String logName) throws Exception {
        if (server.isStarted()) {
            server.stopServer(true, SERVER_QUIESCE_WAIT_MESSAGE_REGX, CONFIG_TIMEOUT_MESSAGE_REGX, "CWWKC1501W");
        }
        server.updateServerConfiguration(originalConfig);
        server.startServer(logName);
    }
}