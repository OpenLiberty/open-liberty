/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.compatibility;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

@RunWith(FATRunner.class)
public class FatTestConcurrentCompatible extends CommonUtils {
	
    private static final Set<String> appNames = new TreeSet<String>(Arrays.asList("PersistExecComp"));
    
	private static final String APP_NAME = "PersistentExecutor";

    private static LibertyServer server = FATSuite.server;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeSuite() throws Exception {
    	//TODO using ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.test.servlet")
    	//causes time out error waiting for app to start.
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                .addPackage("com.ibm.test.servlet");
        ShrinkHelper.exportToServer(server, "apps", app);
        //No need to start the server.  Each test will start the server when necessary.
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stopServer();
    }

    /**
     * TestDescription:
     * Test that a persisted task will run on restart of the server
     * Set the server up to disable task execution
     * For current configuration this ensures that the task does not run until
     * after server restart.
     * On restart setup server to enable task execution
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void testStartPersistedTaskTAfterServerRestart() throws Exception {
        // Setup Persistent Executor to not execute tasks on this server
        if (!server.isStarted()) {
            server.setServerConfigurationFile("/config/disableTaskExecuteServer.xml");
            server.startServer(testName.getMethodName() + ".log");
        } else {
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/config/disableTaskExecuteServer.xml");
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }

        // set default props
        Map<String, String> props = new HashMap<String, String>();
        // Call servlet to create persisted task
        HttpURLConnection connection = getConnection("/persist/schedtest?test=testScheduledPersistedTaskStart",
                                                     HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);

        // Retrieve ID for persisted task from the HTTP Connection
        String taskId = findTaskIdInHttpConnection(connection, "Scheduled Task ID = ");
        log("Task id is " + taskId);
        // stop server
        log("Stopping server at " + System.nanoTime());
        server.stopServer();
        log("Server is Stopped at " + System.nanoTime());

        //turn on task execution.
        server.setServerConfigurationFile("/config/enableTaskExecuteServer.xml");
        server.startServer("testStartPersistedTaskTAfterServerRestart.log");
        log("Server is restarted");
        log("Completed Server Enable Task Execution " + System.nanoTime());

        // run servlet to check the status of the persisted task scheduled in the previous step
        connection = getConnection("/persist/schedtest?test=testCheckPersistedTaskStatus&taskId=" + taskId,
                                   HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
        // task should return the value "COMPLETED SUCCESSFULLY" otherwise the test fails
        HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");
        // Server should start waiting tasks on restart. Message in console log indicates the task
        // was actually started during this instance of the server.
        List<String> taskRunMsgList = server.findStringsInLogs("SimpleTask call method executing");
        if (taskRunMsgList.isEmpty())
            throw new Exception("Task was not run during this server instance");

    }

    /**
     * TestDescription:
     * Test that a persisted task which is active on the server but is not allowed to complete will
     * start again on the server restart
     * Start task with no delay then shut the server down
     * Start the server up and see if the task restarts.
     * NOTE:!!!Enable this test once long running tasks are supported by PE
     */
//    @Mode(TestMode.FULL)
//    @Test
    public void testRunningTaskatServerRestart() throws Exception {

        // set default props
        Map<String, String> props = new HashMap<String, String>();
        // Call servlet to create persisted task
        HttpURLConnection connection = getConnection("/persist/schedtest?test=testScheduledPersistedLongRunningTask",
                                                     HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);

        // Retrieve ID for persisted task from the HTTP Connection
        String taskId = findTaskIdInHttpConnection(connection, "Scheduled Task ID = ");
        log("Task id is " + taskId);
        String runMsg = server.waitForStringInLog("testScehduledPersistedLongRunningTask Sleeping for");
        if (runMsg == null || runMsg.isEmpty()) {
            throw new Exception("Long running task has not started ");
        }
        // stop server
        server.stopServer();

        // Start server
        server.startServer("testRunningTaskatServerRestart.log");
        log("Server is restarted");

        // run servlet to check the status of the persisted task scheduled in the previous step
        connection = getConnection("/persist/schedtest?test=testCheckPersistedTaskStatus&taskId=" + taskId,
                                   HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
        // task should return the value "COMPLETED SUCCESSFULLY" otherwise the test fails
        HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");
        // Make sure task was restarted by this instance of server.
        // and not just using persisted results.
        List<String> runningTaskMsgList = server.findStringsInLogs("testScehduledPersistedLongRunningTask Sleeping for ");
        if (runningTaskMsgList.isEmpty()) {
            throw new Exception("Long running Task Never started");
        }

    }

    /**
     * TestDescription:
     * Test that results of a persisted task will be available on the restart of the server
     * Set delay on the new task in microseconds.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testPersistedTaskThruServerRestartResults() throws Exception {

        if (!server.isStarted())
            server.startServer(testName.getMethodName() + ".log");

        // set default props
        Map<String, String> props = new HashMap<String, String>();
        // Call servlet to create persisted task
        HttpURLConnection connection = getConnection("/persist/schedtest?test=testScheduledPersistedTaskResults",
                                                     HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);

        // Retrieve ID for persisted task from the HTTP Connection
        String taskId = findTaskIdInHttpConnection(connection, "Scheduled Task ID = ");
        log("Task id is " + taskId);
        // stop server
        server.stopServer();
        log("Server is Stopped");

        // Start server
        server.startServer("testPersistedTaskThruServerRestartResults.log");
        log("Server is restarted");

        // run servlet to check the status of the persisted task scheduled in the previous step
        connection = getConnection("/persist/schedtest?test=testCheckPersistedTaskStatus&taskId=" + taskId,
                                   HTTPRequestMethod.GET, null, props, HttpURLConnection.HTTP_OK);
        // task should return the value "COMPLETED SUCCESSFULLY" otherwise the test fails
        HttpUtils.findStringInHttpConnection(connection, "COMPLETED SUCCESSFULLY");

    }

    @Override
    public LibertyServer getServer() {
        return server;
    }

    /**
     * Method to the task ID for a task in the output of an HttpURLConnection. If the text isn't found an assertion error is thrown.
     *
     * @param con The HttpURLconnection to retrieve the text from
     * @param String containing the TaskId name
     * @throws IOException
     * @throws {@link AssertionError} If the text isn't found
     * @throws IllegalArgumentException If <code>taskIdString</code> or <code>con</code> is <code>null</code> or empty
     */
    public String findTaskIdInHttpConnection(HttpURLConnection con, String taskIdString) throws IOException {

        String taskId = null;
        if (con == null || taskIdString == null || taskIdString.length() == 0)
            throw new IllegalArgumentException("Invalid arguments: taskIdString = " + taskIdString + " Http Connection = " + con);
        try {
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line;
            Boolean taskIdFound = false;

            StringBuilder outputBuilder = new StringBuilder();

            while ((line = br.readLine()) != null) {
                outputBuilder.append(line);
                outputBuilder.append("\n");
                if (line.indexOf(taskIdString) >= 0) {
                    taskId = line.substring(line.indexOf(taskIdString) + taskIdString.length(), line.length());
                    taskId.trim();
                    log("Task ID = " + taskId);
                    taskIdFound = true;
                }

            }
            assertEquals("The response did not contain \"" + taskIdString + "\".  Full output is:\"\n" + outputBuilder.toString() + "\".",
                         true, taskIdFound);
        } catch (IOException e) {
            Log.info(getClass(), "findTaskIdInHttpConnection", "Exception " + e.getClass().getName() + " requesting URL=" + con.getURL().toString(), e);
            throw e;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return taskId;
    }

    private void log(String msg) {
        Log.info(getClass(), testName.getMethodName(), msg);
    }

}
