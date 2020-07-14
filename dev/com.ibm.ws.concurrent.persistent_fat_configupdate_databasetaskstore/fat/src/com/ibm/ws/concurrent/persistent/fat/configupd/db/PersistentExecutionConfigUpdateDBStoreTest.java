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
import com.ibm.websphere.simplicity.config.AuthData;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/*
 * Tests for checking databaseStore configuration options for Persistent executor
 * Note: All servers are configured with a persistent executor that has task execution disabled.
 */
@RunWith(FATRunner.class)
public class PersistentExecutionConfigUpdateDBStoreTest extends CommonUtils {

    private static final Set<String> appNames = Collections.singleton("persistcfgdbtest");

    private static final String APP_NAME = "persistcfgdbtest";

    private static final LibertyServer server = FATSuite.server;

    private static final String TASK_ID_SEARCH_TEXT = "Task id is ";

    private static ServerConfiguration originalConfig;

    /**
     * Make sure persistent data store is deleted before the test starts
     *
     * @param traceTag The tag String to be used to log info.
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {    
    	System.out.println("Executing before test suite.");
    	
    	ShrinkHelper.defaultDropinApp(server, APP_NAME, "web");

        // clean up previous Db creation
        // RemoteFile f = server.getFileFromLibertySharedDir("/data/scheddb");
        // f.delete();

        originalConfig = server.getServerConfiguration();
        server.updateServerConfiguration(originalConfig);

        server.startServer(PersistentExecutionConfigUpdateDBStoreTest.class.getSimpleName() + ".log");
        server.waitForStringInLog("CWWKF0011I:.*");
        System.out.println("Server started");
    }

    @AfterClass
    public static void stopServer() throws Exception {
        System.out.println("Stopping Server");
        server.stopServer("CWWKD0292E", "CWWKS1300E", "DSRA8100E", "DSRA0010E");
    }

    @Rule
    public TestName testName = new TestName();

    /*
     * testAuthNestedAuthData updates the configuration to one with 2 authData's defined
     * One nested in the databaseStore configuration and one stand alone
     * The nested auth data is configured with a valid userid and password.
     * The stand alone authData is configured with a valid userid and invalid password.
     * If the stand alone authData is used the test will fail.
     */
    @Test
    public void testNestedAuthData() throws Exception {

        Log.info(getClass(), "testNestedAuth", "Starting testNestedAuthData");
        try {
            //Make sure DB has been created
            StringBuilder output = runInServlet("test=testScheduledPersistedTaskStart&invokedBy=testNestedAuth[1]");

            // switch to nested auth data and make the top level auth data invalid
            ServerConfiguration config = originalConfig.clone();
            DatabaseStore dbTaskStore = config.getDatabaseStores().getById("DBTaskStore");
            dbTaskStore.setAuthDataRef(null);
            AuthData authA = config.getAuthDataElements().getById("autha");
            AuthData nestedAuthData = (AuthData) authA.clone();
            nestedAuthData.setId(null);
            dbTaskStore.getAuthDatas().add(nestedAuthData);
            authA.setPassword("invalidPW");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            output = runInServlet("test=testScheduledPersistedTaskStart&invokedBy=testNestedAuth[2]");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            String originalTaskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));
            Log.info(getClass(), "testAuthDataAuthAccess", "Task id is " + originalTaskId);
        } finally {
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /*
     * testAuthDataAuthAccess creates a task using an authorized userid
     * then updates the configuration to set the databaseStore authData
     * to access a userid that has an invalid password therefore is not authorized to connect to the underlying DB
     */
    @Test
    public void testAuthDataAuthAccess() throws Exception {

        Log.info(getClass(), "testAuthDataAccess", "Starting testAuthDataTaskAccess");
        try {
            StringBuilder output = runInServlet("test=testScheduledPersistedTaskStart&invokedBy=testAuthDataAuthAccess[1]");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            String originalTaskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));
            Log.info(getClass(), "testAuthDataAuthAccess", "Task id is " + originalTaskId);
            // Update the configuration so databaseStore is running under an authdata with a
            // userid that does not have connect authority

            ServerConfiguration config = originalConfig.clone();
            DatabaseStore dbTaskStore = config.getDatabaseStores().getById("DBTaskStore");
            AuthData user2Auth = new AuthData();
            user2Auth.setId("user2Auth");
            user2Auth.setUser("user2");
            user2Auth.setPassword("invalidpw"); // pwd2
            config.getAuthDataElements().add(user2Auth);
            dbTaskStore = config.getDatabaseStores().getById("DBTaskStore");
            dbTaskStore.setAuthDataRef(user2Auth.getId());

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testCheckPersistedAvailable&taskId=" + originalTaskId + "&invokedBy=testAuthDataAuthAccess[2]");
        } finally {
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    
    /*
     * testChangeAuthData creates a task using an user1 then attempts to access the task
     * using user2 making sure the taks cannot be seen. Then switch back to user1
     * and make sure user1 can access the task.
     */
    @Test
    public void testChangeAuthData() throws Exception {

        String taskId;
        try {
            StringBuilder output = runInServlet("test=testScheduledPersistedTaskStart&invokedBy=testChangeAuthData[1]");
            int start = output.indexOf(TASK_ID_SEARCH_TEXT);
            if (start < 0)
                throw new Exception("Task id of scheduled task not found in servlet output: " + output);
            taskId = output.substring(start += TASK_ID_SEARCH_TEXT.length(), output.indexOf(".", start));
            Log.info(getClass(), "testChangAuthData", "returned Task id = " + taskId);
            // change to  authDataRef=user2auth
            ServerConfiguration config = originalConfig.clone();
            DatabaseStore dbTaskStore = config.getDatabaseStores().getById("DBTaskStore");
            AuthData user2Auth = new AuthData();
            user2Auth.setId("user2Auth");
            user2Auth.setUser("user2");
            user2Auth.setPassword("password2"); // pwd2
            config.getAuthDataElements().add(user2Auth);
            dbTaskStore = config.getDatabaseStores().getById("DBTaskStore");
            dbTaskStore.setAuthDataRef(user2Auth.getId());

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            runInServlet("test=testCannotFindTask&jndiName=concurrent/myScheduler&taskId=" + taskId + "&invokedBy=testChangeAuthData[2]");
        } finally {
            // restore original configuration
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }

        runInServlet("test=testFindTask&jndiName=concurrent/myScheduler&taskId=" + taskId + "&invokedBy=testChangeAuthData[3]");
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

    @Override
    public LibertyServer getServer() {
        return server;
    }
}
