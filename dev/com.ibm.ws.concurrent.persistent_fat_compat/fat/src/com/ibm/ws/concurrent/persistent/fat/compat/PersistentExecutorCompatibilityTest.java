/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.compat;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for persistent scheduled executor with task execution disabled
 */
@RunWith(FATRunner.class)
public class PersistentExecutorCompatibilityTest {
    private static final LibertyServer server = FATSuite.server;
    
    public static final String APP_NAME = "persistentcompattest";

    @Rule
    public TestName testName = new TestName();

    /**
     * Runs a test in the servlet.
     * 
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    protected static StringBuilder runInServlet(String test) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/persistentcompattest?test=" + test);
        Log.info(PersistentExecutorCompatibilityTest.class, "runInServlet", "URL is " + url);
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
                Log.info(PersistentExecutorCompatibilityTest.class, "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                Log.info(PersistentExecutorCompatibilityTest.class, "runInServlet", "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }

            return lines;
        } finally {
            con.disconnect();
            Log.info(PersistentExecutorCompatibilityTest.class, "runInServlet", "disconnected from servlet");
        }
    }

    /**
     * Before running any tests, start the server
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
    	ShrinkHelper.defaultDropinApp(server, APP_NAME, "web", "ejb");
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
            server.stopServer("CNTR0333W","CWWKC1511W");
    }

    @Test
    public void testExecuteCallableTrigger_8_5_5_6() throws Exception {
        runInServlet("testExecuteCallableTrigger_8_5_5_6");
    }

    @Test
    public void testExecuteCallableWithTrigger_8_5_5_6() throws Exception {
        runInServlet("testExecuteCallableWithTrigger_8_5_5_6");
    }

    @Test
    public void testExecuteEJBTimer_8_5_5_6() throws Exception {
        runInServlet("testExecuteEJBTimer_8_5_5_6");
    }

    @Test
    public void testExecuteFixedDelayTask_8_5_5_6() throws Exception {
        runInServlet("testExecuteFixedDelayTask_8_5_5_6");
    }

    @Test
    public void testExecuteFixedRateTask_8_5_5_6() throws Exception {
        runInServlet("testExecuteFixedRateTask_8_5_5_6");
    }

    @Test
    public void testExecuteOneShotCallable_8_5_5_6() throws Exception {
        runInServlet("testExecuteOneShotCallable_8_5_5_6");
    }

    @Test
    public void testGetFailingRunnable_8_5_5_6() throws Exception {
        runInServlet("testGetFailingRunnable_8_5_5_6");
    }

    @Test
    public void testGetSkippedCallable_8_5_5_6() throws Exception {
        runInServlet("testGetSkippedCallable_8_5_5_6");
    }

    @Test
    public void testGetSkipRunFailure_8_5_5_6() throws Exception {
        runInServlet("testGetSkipRunFailure_8_5_5_6");
    }

    @Test
    public void testGetTaskWithNonSerializableResult_8_5_5_6() throws Exception {
        runInServlet("testGetTaskWithNonSerializableResult_8_5_5_6");
    }

    @Test
    public void testPersistFailingRunnable() throws Exception {
        runInServlet("testPersistFailingRunnable");
    }

    @Test
    public void testPersistSkippedCallable() throws Exception {
        runInServlet("testPersistSkippedCallable");
    }

    @Test
    public void testPersistSkipRunFailure() throws Exception {
        runInServlet("testPersistSkipRunFailure");
    }

    @Test
    public void testPersistTaskWithNonSerializableResult() throws Exception {
        runInServlet("testPersistTaskWithNonSerializableResult");
    }

    @Test
    public void testScheduleCallableTrigger() throws Exception {
        runInServlet("testScheduleCallableTrigger");
    }

    @Test
    public void testScheduleCallableWithTrigger() throws Exception {
        runInServlet("testScheduleCallableWithTrigger");
    }

    @Test
    public void testScheduleEJBTimer() throws Exception {
        runInServlet("testScheduleEJBTimer");
    }

    @Test
    public void testScheduleFixedDelayTask() throws Exception {
        runInServlet("testScheduleFixedDelayTask");
    }

    @Test
    public void testScheduleFixedRateTask() throws Exception {
        runInServlet("testScheduleFixedRateTask");
    }

    @Test
    public void testScheduleOneShotCallable() throws Exception {
        runInServlet("testScheduleOneShotCallable");
    }

    @Test
    public void testSequenceCreated() throws Exception {
        runInServlet("testSequenceCreated");
    }
}