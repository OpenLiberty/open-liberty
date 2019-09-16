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
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for error paths in persistent scheduled executor
 */
@RunWith(FATRunner.class)
public class PersistentExecutorErrorPathsTest {

    private static final LibertyServer server = FATSuite.server;
    
    private static final String APP_NAME = "persistenterrtest";

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
        if (server != null && server.isStarted())
            server.stopServer(".*");
    }

    @Test
    public void testFailingTaskAndAutoPurge() throws Exception {
        runInServlet("testFailingTaskAndAutoPurge");
    }

    @Test
    public void testFailingTaskNoAutoPurge() throws Exception {
        runInServlet("testFailingTaskNoAutoPurge");
    }

    @Test
    public void testFailOnceAndSkipFirstRetryAndAutoPurge() throws Exception {
        runInServlet("testFailOnceAndSkipFirstRetryAndAutoPurge");
    }

    @Test
    public void testFailOnceAndSkipFirstRetryNoAutoPurge() throws Exception {
        runInServlet("testFailOnceAndSkipFirstRetryNoAutoPurge");
    }

    @Test
    public void testGetWithTimeout() throws Exception {
        runInServlet("testGetWithTimeout");
    }

    @Test
    public void testLongRunningTask() throws Exception {
        runInServlet("testLongRunningTask");
    }

    @Test
    public void testNegativeTransactionTimeout() throws Exception {
        runInServlet("testNegativeTransactionTimeout");
    }

    @Test
    public void testNoClassloaderContext() throws Exception {
        runInServlet("testNoClassloaderContext");
    }

    @Test
    public void testNoJavaEEMetadataContext() throws Exception {
        runInServlet("testNoJavaEEMetadataContext");
    }

    @Test
    public void testNonIntegerTransactionTimeout() throws Exception {
        runInServlet("testNonIntegerTransactionTimeout");
    }

    @Test
    public void testNonSerializableResult() throws Exception {
        runInServlet("testNonSerializableResult");
    }

    @Test
    public void testNullProperty() throws Exception {
        runInServlet("testNullProperty");
    }

    @Test
    public void testNullTasks() throws Exception {
        runInServlet("testNullTasks");
    }

    @Test
    public void testNullTriggers() throws Exception {
        runInServlet("testNullTriggers");
    }

    @Test
    public void testNullUnits() throws Exception {
        runInServlet("testNullUnits");
    }

    @Test
    public void testPredeterminedResultFailsToSerialize() throws Exception {
        runInServlet("testPredeterminedResultFailsToSerialize");
    }

    @Test
    public void testPredeterminedResultIsNotSerializable() throws Exception {
        runInServlet("testPredeterminedResultIsNotSerializable");
    }

    @Test
    public void testResultFailsToSerialize() throws Exception {
        runInServlet("testResultFailsToSerialize");
    }

    @Test
    public void testResultSerializationFailureFailsToSerialize() throws Exception {
        runInServlet("testResultSerializationFailureFailsToSerialize");
    }

    @Test
    public void testRetryFailedTaskAndAutoPurge() throws Exception {
        runInServlet("testRetryFailedTaskAndAutoPurge");
    }

    @Test
    public void testRetryFailedTaskNoAutoPurge() throws Exception {
        runInServlet("testRetryFailedTaskNoAutoPurge");
    }

    @Test
    public void testShutDownDerbyBeforeTaskExecution() throws Exception {
        runInServlet("testShutDownDerbyBeforeTaskExecution");
    }

    @Test
    public void testShutDownDerbyDuringTaskExecution() throws Exception {
        runInServlet("testShutDownDerbyDuringTaskExecution");
    }

    @Test
    public void testSkipRunFailsOnFirstExecutionAttempt() throws Exception {
        runInServlet("testSkipRunFailsOnFirstExecutionAttempt");
    }

    @Test
    public void testSkipRunFailsOnLastExecutionAttempt() throws Exception {
        runInServlet("testSkipRunFailsOnLastExecutionAttempt");
    }

    @Test
    public void testSkipRunFailsOnLastExecutionAttemptNoAutoPurge() throws Exception {
        runInServlet("testSkipRunFailsOnLastExecutionAttemptNoAutoPurge");
    }

    @Test
    public void testSkipRunFailsOnMiddleExecutionAttempts() throws Exception {
        runInServlet("testSkipRunFailsOnMiddleExecutionAttempts");
    }

    @Test
    public void testSkipRunFailsOnOnlyExecutionAttempt() throws Exception {
        runInServlet("testSkipRunFailsOnOnlyExecutionAttempt");
    }

    @Test
    public void testSkipRunFailsOnOnlyExecutionAttemptNoAutoPurge() throws Exception {
        runInServlet("testSkipRunFailsOnOnlyExecutionAttemptNoAutoPurge");
    }

    @Test
    public void testTaskFailsToSerialize() throws Exception {
        runInServlet("testTaskFailsToSerialize");
    }

    @Test
    public void testTransactionTimeout() throws Exception {
        runInServlet("testTransactionTimeout");
    }

    @Test
    public void testTransactionTimeoutSuspendedTransaction() throws Exception {
        runInServlet("testTransactionTimeoutSuspendedTransaction");
    }

    @Test
    public void testTriggerFailsGetNextRunTimeAfterTaskRuns() throws Exception {
        runInServlet("testTriggerFailsGetNextRunTimeAfterTaskRuns");
    }

    @Test
    public void testTriggerFailsInitialGetNextRunTime() throws Exception {
        runInServlet("testTriggerFailsInitialGetNextRunTime");
    }

    @Test
    public void testTriggerFailsToSerialize() throws Exception {
        runInServlet("testTriggerFailsToSerialize");
    }

    @Test
    public void testTriggerWithNoExecutions() throws Exception {
        runInServlet("testTriggerWithNoExecutions");
    }

    @Test
    public void testUnsupportedOperations() throws Exception {
        runInServlet("testUnsupportedOperations");
    }

    @Test
    public void testZeroOrNegativeIntervals() throws Exception {
        runInServlet("testZeroOrNegativeIntervals");
    }
}