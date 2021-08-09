/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.configupd.db;

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
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.JdbcDriver;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_embedded;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for configuration updates to databaseStore while the server is running.
 * The persistent executor is configured to have task execution ENABLED.
 */
@RunWith(FATRunner.class)
public class ExecEnabledDBStoreConfigUpdateTest {
	public static final Set<String> appNames = Collections.singleton("persistcfgdbtest");
	
    private static final String APP_NAME = "persistcfgdbtest";

    private static final LibertyServer server = FATSuite.server;

    private static final String TASK_ID_SEARCH_TEXT = "Task id is ";

    private static ServerConfiguration originalConfig;
    private static ServerConfiguration originalConfigForAfterSuite;

    @Rule
    public TestName testName = new TestName();

    @AfterClass
    public static void afterSuite() throws Exception {
        if (server != null) {
            if (server.isStarted())
                server.stopServer("CWWKC1556W");
            server.updateServerConfiguration(originalConfigForAfterSuite);
        }
    }

    @BeforeClass
    public static void beforeSuite() throws Exception {
    	//Setup app
    	System.out.println("Executing before test suite.");
    	
    	ShrinkHelper.defaultDropinApp(server, APP_NAME, "web");
    	
        // Add  id="executor1" enableTaskExecution="true" to the persistentExecutor
        originalConfig = server.getServerConfiguration();
        originalConfigForAfterSuite = originalConfig.clone();
        PersistentExecutor executor1 = originalConfig.getPersistentExecutors().getBy("jndiName", "concurrent/myScheduler");
        executor1.setId("executor1");
        executor1.setEnableTaskExecution("true");
        server.updateServerConfiguration(originalConfig);
        server.startServer("ExecEnabledDBTaskStoreConfigUpdateTest.log");
    }

    /**
     * Runs a test in the servlet.
     *
     * @param queryString query string including at least the test name
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    protected StringBuilder runInServlet(String queryString) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/persistcfgdbtest?" + queryString);
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
     * Start out with createTables=false for a persistent executor. Let it fail.
     * In response, update configuration to set createTables=true.
     * Verify that a task can be scheduled and executes successfully.
     */
    @Test
    public void testCreateMissingTables() throws Exception {
        try {
            // Add a new persistentExecutor with a new databaseStore with createTables=false
            ServerConfiguration config = originalConfig.clone();
            DatabaseStore dbTaskStore = config.getDatabaseStores().getById("DBTaskStore");
            DatabaseStore dbStore2 = new DatabaseStore();
            dbStore2.setId("dbStore2");
            dbStore2.setDataSourceRef(dbTaskStore.getDataSourceRef());
            dbStore2.setAuthDataRef(dbTaskStore.getAuthDataRef());
            dbStore2.setTablePrefix("PX2");
            dbStore2.setCreateTables("false");
            config.getDatabaseStores().add(dbStore2);
            PersistentExecutor executor2 = new PersistentExecutor();
            executor2.setId("executor2");
            executor2.setJndiName("concurrent/executor2");
            executor2.setTaskStoreRef(dbStore2.getId());
            config.getPersistentExecutors().add(executor2);

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testScheduleTaskWithMissingTables&jndiName=concurrent/executor2&invokedBy=testCreateMissingTables[1]");

            // switch createTables=true
            dbStore2.setCreateTables("true");

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testScheduleAndRunTask&jndiName=concurrent/executor2&invokedBy=testCreateMissingTables[2]");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Swap which databaseStore is used by a persistentExecutor
     */
    @Test
    public void testSwapDatabaseStores() throws Exception {
        // Schedule one shot task
        StringBuilder output = runInServlet("test=testScheduledPersistedTaskStart&invokedBy=testSwapDatabaseStores[1]");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of one-shot task not found in servlet output: " + output);
        String taskIdA1 = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        // Schedule repeating task
        output = runInServlet("test=testScheduleRepeatingTask&invokedBy=testSwapDatabaseStores[2]");
        start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of repeating task not found in servlet output: " + output);
        String taskIdA2 = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        // Moved this to avoid trouble with Derby being shut down before it finishes creating tables
        try {
            // Switch taskStoreRef from DBTaskStore to taskStoreB (new) which has a different tablePrefix (B)
            ServerConfiguration config = originalConfig.clone();
            DatabaseStore dbTaskStore = config.getDatabaseStores().getById("DBTaskStore");
            DatabaseStore taskStoreB = new DatabaseStore();
            taskStoreB.setId("taskStoreB");
            taskStoreB.setAuthDataRef(dbTaskStore.getAuthDataRef());
            taskStoreB.setDataSourceRef(dbTaskStore.getDataSourceRef());
            taskStoreB.setTablePrefix("B");
            config.getDatabaseStores().add(taskStoreB);
            PersistentExecutor executor1 = config.getPersistentExecutors().getById("executor1");
            executor1.setTaskStoreRef(taskStoreB.getId());

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Tasks from the original database store should not be visible.
            runInServlet("test=testCannotFindTask&jndiName=concurrent/myScheduler&taskId=" + taskIdA1 + "&invokedBy=testSwapDatabaseStores[3]");
            runInServlet("test=testCannotFindTask&jndiName=concurrent/myScheduler&taskId=" + taskIdA2 + "&invokedBy=testSwapDatabaseStores[4]");

            // Schedule another task, which should go into the new database store
            runInServlet("test=testScheduleNamedTask&name=testSwapDatabaseStores-TaskB1&invokedBy=testSwapDatabaseStores[5]");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }

        // Tasks from the original database store should be visible again
        runInServlet("test=testFindTask&jndiName=concurrent/myScheduler&taskId=" + taskIdA1 + "&invokedBy=testSwapDatabaseStores[6]");
        runInServlet("test=testFindTask&jndiName=concurrent/myScheduler&taskId=" + taskIdA2 + "&invokedBy=testSwapDatabaseStores[7]");

        // Repeating task from the original database store should be running
        runInServlet("test=testTaskIsRunning&jndiName=concurrent/myScheduler&taskId=" + taskIdA2 + "&cancel=true&invokedBy=testSwapDatabaseStores[8]");

        // Verify we cannot find the task from the other database store
        runInServlet("test=testCannotFindNamedTask&name=testSwapDatabaseStores-TaskB1&invokedBy=testSwapDatabaseStores[9]");
    }

    /**
     * Swap which dataSource is used by a databaseStore
     */
    @Test
    public void testSwapDataSources() throws Exception {
        // Schedule repeating task
        StringBuilder output = runInServlet("test=testScheduleRepeatingTask&invokedBy=testSwapDataSources[1]");
        int start = output.indexOf(TASK_ID_SEARCH_TEXT);
        if (start < 0)
            throw new Exception("Task id of repeating task not found in servlet output: " + output);
        String taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));

        try {
            // Replace dataSourceRef with a nested dataSource that points to a different database
            ServerConfiguration config = originalConfig.clone();
            DatabaseStore dbTaskStore = config.getDatabaseStores().getById("DBTaskStore");
            DataSource dataSource = new DataSource();
            JdbcDriver jdbcDriver = new JdbcDriver();
            jdbcDriver.setLibraryRef("FATJDBCLib");
            dataSource.getJdbcDrivers().add(jdbcDriver);
            Properties_derby_embedded properties = new Properties_derby_embedded();
            properties.setCreateDatabase("create");
            properties.setDatabaseName("memory:testdb");
            dataSource.getProperties_derby_embedded().add(properties);
            dbTaskStore.getDataSources().add(dataSource);
            dbTaskStore.setDataSourceRef(null);

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            // Task from the original database should not be visible.
            runInServlet("test=testCannotFindTask&jndiName=concurrent/myScheduler&taskId=" + taskId + "&invokedBy=testSwapDataSources[2]");

            // Schedule another task, which should go into the new database
            runInServlet("test=testScheduleNamedTask&name=testSwapDataSources-Task2&invokedBy=testSwapDataSources[3]");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }

        // Repeating task from the original database store should be running
        runInServlet("test=testTaskIsRunning&jndiName=concurrent/myScheduler&taskId=" + taskId + "&cancel=true&invokedBy=testSwapDataSources[4]");

        // Verify we cannot find the task from the other database store
        runInServlet("test=testCannotFindNamedTask&name=testSwapDataSources-Task2&invokedBy=testSwapDataSources[5]");
    }
}
