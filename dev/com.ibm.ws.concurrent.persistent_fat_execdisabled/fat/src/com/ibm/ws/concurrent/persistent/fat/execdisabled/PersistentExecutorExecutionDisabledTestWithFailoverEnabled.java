/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.execdisabled;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for persistent scheduled executor with task execution disabled
 */
@RunWith(FATRunner.class)
public class PersistentExecutorExecutionDisabledTestWithFailoverEnabled {

    private static final LibertyServer server = FATSuite.server;
    
    private static final String APP_NAME = "persistentnoexectest";

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
    protected static StringBuilder runInServlet(String test) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/persistentnoexectest?test=" + test);
        Log.info(PersistentExecutorExecutionDisabledTestWithFailoverEnabled.class, "runInServlet", "URL is " + url);
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
                Log.info(PersistentExecutorExecutionDisabledTestWithFailoverEnabled.class, "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                Log.info(PersistentExecutorExecutionDisabledTestWithFailoverEnabled.class, "runInServlet", "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }

            return lines;
        } finally {
            con.disconnect();
            Log.info(PersistentExecutorExecutionDisabledTestWithFailoverEnabled.class, "runInServlet", "disconnected from servlet");
        }
    }

    /**
     * Before running any tests, enable fail over and start the server
     */
    @BeforeClass
    public static void setUp() throws Exception {
        originalConfig = server.getServerConfiguration();
        ServerConfiguration config = originalConfig.clone();
        PersistentExecutor persistentExecutor = config.getPersistentExecutors().getBy("jndiName", "concurrent/myScheduler");
        persistentExecutor.setMissedTaskThreshold("6s");
        persistentExecutor.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
        server.updateServerConfiguration(config);

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "web");
        server.startServer();
    }

    /**
     * After completing all tests, stop the server and restore its configuration.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null)
            try {
                if (server.isStarted())
                    try {
                        runInServlet("testNoTasksExecuted");
                    } finally {
                        server.stopServer();
                    }
            } finally {
                server.updateServerConfiguration(originalConfig);
            }
    }

    @Test
    public void testCancelAndAutoPurgeFE() throws Exception {
        runInServlet("testCancelAndAutoPurge");
    }

    @Test
    public void testCancelNoAutoPurgeFE() throws Exception {
        runInServlet("testCancelNoAutoPurge");
    }

    @Test
    public void testExecuteFE() throws Exception {
        runInServlet("testExecute");
    }

    @Test
    public void testNameAndTriggerOnInitialTaskStatusFE() throws Exception {
        runInServlet("testNameAndTriggerOnInitialTaskStatus");
    }

    @Test
    public void testRemoveFE() throws Exception {
        runInServlet("testRemove");
    }

    @Test
    public void testScheduleAtFixedRateFE() throws Exception {
        runInServlet("testScheduleAtFixedRate");
    }

    @Test
    public void testScheduleCallableFE() throws Exception {
        runInServlet("testScheduleCallable");
    }

    @Test
    public void testScheduleCallableWithTriggerFE() throws Exception {
        runInServlet("testScheduleCallableWithTrigger");
    }

    @Test
    public void testScheduleRunnableFE() throws Exception {
        runInServlet("testScheduleRunnable");
    }

    @Test
    public void testScheduleRunnableWithTriggerFE() throws Exception {
        runInServlet("testScheduleRunnableWithTrigger");
    }

    @Test
    public void testScheduleWithFixedDelayFE() throws Exception {
        runInServlet("testScheduleWithFixedDelay");
    }

    @Test
    public void testSubmitCallableFE() throws Exception {
        runInServlet("testSubmitCallable");
    }

    @Test
    public void testSubmitRunnableFE() throws Exception {
        runInServlet("testSubmitRunnable");
    }

    @Test
    public void testSubmitRunnableWithResultFE() throws Exception {
        runInServlet("testSubmitRunnableWithResult");
    }
}