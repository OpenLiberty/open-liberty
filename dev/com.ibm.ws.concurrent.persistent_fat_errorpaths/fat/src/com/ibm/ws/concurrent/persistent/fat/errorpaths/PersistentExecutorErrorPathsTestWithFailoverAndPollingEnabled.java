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
package com.ibm.ws.concurrent.persistent.fat.errorpaths;

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
        persistentExecutor.setExtraAttribute("missedTaskThreshold2", "5s"); // TODO rename and use setter
        persistentExecutor.setPollInterval("3s");
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

    @Test
    public void testTransactionTimeoutFEWithPolling() throws Exception {
        runInServlet("testTransactionTimeout");
    }

    @Test
    public void testTransactionTimeoutSuspendedTransactionFEWithPolling() throws Exception {
        runInServlet("testTransactionTimeoutSuspendedTransaction");
    }

    @Test
    public void testTriggerFailsGetNextRunTimeAfterTaskRunsFEWithPolling() throws Exception {
        runInServlet("testTriggerFailsGetNextRunTimeAfterTaskRuns");
    }
}