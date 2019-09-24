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
package com.ibm.ws.concurrent.persistent.feature;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class ConcurrentPersistentFeatureFATTest {
    
	/** This class' name. */
    private Class<?> c = ConcurrentPersistentFeatureFATTest.class;
    
    /** Liberty server reference. */
    private static final LibertyServer server = FATSuite.server;
    
    /** Web App Name **/
    private static final String APP_NAME = "CPFeatureApp";

    /**
     * Runs a test in the servlet.
     * 
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    protected StringBuilder runInServlet(String servlet, String test) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + servlet + "?test=" + test);
        for (int numRetries = 2;; numRetries--) {
            Log.info(c, "runInServlet", "URL is " + url);
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
                    Log.info(c, "runInServlet", line);
                }

                // Look for success message, otherwise fail test
                if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                    Log.info(c, "runInServlet", "failed to find completed successfully message");
                    fail("Missing success message in output. " + lines);
                }

                return lines;
            } catch (FileNotFoundException x) {
                if (numRetries > 0)
                    try {
                        Log.info(c, "runInServlet", x + " occurred - will retry after 10 seconds");
                        Thread.sleep(10000);
                    } catch (InterruptedException interruption) {
                    }
                else
                    throw x;
            } finally {
                con.disconnect();
                Log.info(c, "runInServlet", "disconnected from servlet");
            }
        }
    }

    /**
     * Pre-execution setup.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
    	ShrinkHelper.defaultDropinApp(server, APP_NAME, "com.ibm.feature");
    }

    /**
     * Post-execution setup.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Validates that an application cannot load classes such as:
     * - javax.enterprise.concurrent.ManagedScheduledExecutorService
     * - com.ibm.websphere.concurrent.persistent.PersistentExecutor
     * because concurrent-1.0 or persistentExecutor-1.0 are not enabled.
     * 
     * @throws Exception
     */
    @Test
    public void testValidateAppInabilityToAccessConcurrentPersistentArtifacts() throws Exception {
    	startServerIfNotStarted(false);
        runInServlet("CPFeatureApp/persistent", "validateAppInabilityToAccessConcurrentPersistentArtifacts");
    }

    /**
     * Schedule a basic callable using the servlet's reference to the ScheduledExecutorService.
     * This reference should point to the configured persistentExecutor.
     * 
     * @throws Exception
     */
    @Test
    public void testCallToFuterGetWithWaitTimeThrowsAUnsupportedOperationException() throws Exception {
    	startServerIfNotStarted(false);
        runInServlet("CPFeatureApp/persistent", "testCallToFuterGetWithWaitTimeThrowsAUnsupportedOperationException");
    }

    /**
     * Have multiple threads invoke TaskStore.findOrCreate at the same time
     */
    @Test
    public void testFindOrCreate() throws Exception {
    	startServerIfNotStarted(false);
    	runInServlet("CPFeatureApp/persistent", "testFindOrCreate");
    }

    /**
     * Schedule a basic Runnable using the servlet's reference to the ScheduledExecutorService. 
     * This reference should point to the configured persistentExecutor.
     * 
     * @throws Exception
     */
    @Test
    public void testScheduleASimpleTaskNoDatabaseExecution() throws Exception {
    	startServerIfNotStarted(false);
        runInServlet("CPFeatureApp/persistent", "scheduleASimpleTaskNoDatabaseExecution");
    }

    /**
     * Starts the server if not started.
     * 
     * @param forceRestart True to force a server to restart.
     * 
     * @throws Exception
     */
    public void startServerIfNotStarted(boolean forceRestart) throws Exception {
        if (!server.isStarted()) {
            server.startServer(true, true);
        } else {
            if (forceRestart) {
                server.restartServer();
            }
        }
    }
}