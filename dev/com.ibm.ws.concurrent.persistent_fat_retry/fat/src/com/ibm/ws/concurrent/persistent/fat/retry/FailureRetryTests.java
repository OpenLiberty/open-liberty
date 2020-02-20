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
package com.ibm.ws.concurrent.persistent.fat.retry;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.FeatureManager;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.database.DerbyEmbeddedUtilities;
import componenttest.topology.impl.LibertyServer;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Tests for error paths in persistent scheduled executor
 */
@RunWith(FATRunner.class)
public class FailureRetryTests {

    private static final LibertyServer server = FATSuite.server;

    private static final String APP_NAME = "retrytest";

    @Rule
    public TestName testName = new TestName();

    private static ServerConfiguration savedConfig;

    /**
     * Updates the configuration. Each test should call this method. Note that the
     * "default" retryCount is set to 1 and the "default" retryInterval is set to
     * 10ms. The actual product defaults are much larger, and we will not be testing
     * those.
     */
    private static ServerConfiguration updateConfiguration(String retryLimit, String retryInterval) throws Exception {
        ServerConfiguration config = savedConfig.clone();
        ConfigElementList<PersistentExecutor> peList = config.getPersistentExecutors();
        if ((peList == null) || (peList.size() != 1)) {
            throw new Exception("PersistentExecutor configuration is invalid");
        }

        PersistentExecutor pe = peList.get(0);
        if (retryLimit != null)
            pe.setRetryLimit(retryLimit);
        if (retryInterval != null)
            pe.setRetryInterval(retryInterval);

        server.updateServerConfiguration(config);
        return config;
    }

    /**
     * Runs a test in the servlet.
     *
     * @param test
     *            Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException
     *             if an error occurs
     */
    protected StringBuilder runInServlet(String test) throws Exception {
        return runInServlet(test, null, null);
    }

    protected StringBuilder runInServlet(String test, String servletName, String taskIdString) throws Exception {
        StringBuilder urlString = new StringBuilder(
                "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/retrytest");
        if (servletName != null) {
            urlString.append("/" + servletName);
        }
        urlString.append("?test=" + test);
        if (taskIdString != null) {
            urlString.append("&taskid=" + taskIdString);
        }
        URL url = new URL(urlString.toString());
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
        savedConfig = server.getServerConfiguration().clone();
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "web");
        DerbyEmbeddedUtilities.createDB(server, "SchedDB");
    }

    /**
     * After running each test, stop the server. Since each test updates the
     * configuration, if the test runs too quickly, the configuration updates will
     * not be noticed. By restarting the server for each test, we'll ensure we see
     * the correct configuration.
     */
    @After
    public void tearDownPerTest() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKE0701E", "CWWKC1503W", "CWWKE0700W");
        }
        server.updateServerConfiguration(savedConfig);
    }

    /**
     * After completing all tests, stop the server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKC1503W");
            server.updateServerConfiguration(savedConfig);
        }
    }

    /**
     * Verify that a task can fail once and be retried (which also fails).
     *
     * @throws Exception
     */
    @Test
    public void testRetryOnce() throws Exception {
        server.startServer("testRetryOnce.log");
        try {
            runInServlet("testRetryOnce");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1511W", "CWWKC1503W");
        }
    }

    /**
     * Verify that a task can fail once and be retried immediately, ignoring the
     * retry interval.
     *
     * @throws Exception
     */
    @Test
    public void testRetryOnceIgnoreInterval() throws Exception {
        updateConfiguration(null, "60s");
        server.startServer("testRetryOnceIgnoreInterval.log");
        try {
            runInServlet("testRetryOnceIgnoreInterval");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1511W", "CWWKC1503W");
        }
    }

    /**
     * Verify that a task is not retried when the failure limit is set to 0.
     *
     * @throws Exception
     */
    @Test
    public void testRetryZero() throws Exception {
        updateConfiguration("0", null);
        server.startServer("testRetryZero.log");
        try {
            runInServlet("testRetryZero");
        } finally {
            server.stopServer("CWWKC1511W", "CWWKC1503W");
        }
    }

    /**
     * Verify that a task is retried twice when the retry limit is set to 2. Verify
     * that at least 10ms elapses between the 2nd and 3rd task execution.
     *
     * @throws Exception
     */
    @Test
    public void testRetryTwiceDefaultInterval() throws Exception {
        updateConfiguration("2", null);
        server.startServer("testRetryTwiceDefaultInterval.log");
        try {
            runInServlet("testRetryTwiceDefaultInterval");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1511W", "CWWKC1503W");
        }
    }

    /**
     * With fail over enabled, verify that a task is retried twice when the retry limit is set to 2. Verify
     * that at least 10ms elapses between the 2nd and 3rd task execution.
     */
    @Test
    public void testRetryTwiceDefaultIntervalWithFailoverEnabled() throws Exception {
        ServerConfiguration config = savedConfig.clone();
        PersistentExecutor myScheduler = config.getPersistentExecutors().getBy("jndiName", "concurrent/myScheduler");
        myScheduler.setRetryInterval(null);
        myScheduler.setRetryLimit("2");
        myScheduler.setPollInterval("1s30ms"); // polling is needed for retries
        myScheduler.setMissedTaskThreshold("3s");
        myScheduler.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
        server.updateServerConfiguration(config);

        server.startServer("testRetryTwiceDefaultIntervalFE.log");
        try {
            runInServlet("testRetryTwiceDefaultInterval");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1511W", "CWWKC1503W");
        }
    }

    /**
     * Verify that a task is retried twice when the retry limit is set to 2. Verify
     * that at least 5s elapses between the 2nd and 3rd task executions.
     *
     * @throws Exception
     */
    @Test
    public void testRetryTwiceLongInterval() throws Exception {
        updateConfiguration("2", "5s");
        server.startServer("testRetryTwiceLongInterval.log");
        try {
            runInServlet("testRetryTwiceLongInterval");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1511W", "CWWKC1503W");
        }
    }

    /**
     * Verify that a task is retried three times when the retry limit is set to 3.
     * Verify that at least 10ms elapses between the 2nd and 3rd, and 3rd and 4th
     * task executions.
     *
     * @throws Exception
     */
    @Test
    public void testRetryThriceDefaultInterval() throws Exception {
        updateConfiguration("3", null);
        server.startServer("testRetryThriceDefaultInterval.log");
        try {
            runInServlet("testRetryThriceDefaultInterval");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1511W", "CWWKC1503W");
        }
    }

    /**
     * Set a negative retry interval and see what happens!
     *
     * @throws Exception
     */
    @Test
    public void testNegativeInterval() throws Exception {
        updateConfiguration("2", "-1");
        server.startServer("testNegativeInterval.log");
        Assert.assertNotNull(server.waitForStringInLog("java.lang.IllegalArgumentException: retryInterval: -1"));
    }

    /**
     * Verify that a task can fail more than the failure limit number of times if
     * the failures are not consecutive.
     *
     * @throws Exception
     */
    @Test
    public void testRetrySixWithTwoPasses() throws Exception {
        updateConfiguration("3", null);
        server.startServer("testRetrySixWithTwoPasses.log");
        try {
            runInServlet("testRetrySixWithTwoPasses");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1503W");
        }
    }

    /**
     * With fail over enabled, verify that a task can fail more than the failure limit number of times if
     * the failures are not consecutive.
     */
    @Test
    public void testRetrySixWithTwoPassesWithFailoverEnabled() throws Exception {
        ServerConfiguration config = savedConfig.clone();
        PersistentExecutor myScheduler = config.getPersistentExecutors().getBy("jndiName", "concurrent/myScheduler");
        myScheduler.setRetryInterval(null);
        myScheduler.setRetryLimit("3");
        myScheduler.setPollInterval("1s31ms"); // polling is needed for retries
        myScheduler.setMissedTaskThreshold("3s");
        myScheduler.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
        server.updateServerConfiguration(config);

        server.startServer("testRetrySixWithTwoPassesFE.log");
        try {
            runInServlet("testRetrySixWithTwoPasses");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1503W");
        }
    }

    /**
     * Verify that a skip is not counted as a fail.
     *
     * @throws Exception
     */
    @Test
    public void testRetryFourWithOneSkip() throws Exception {
        updateConfiguration("3", null);
        server.startServer("testRetryFourWithOneSkip.log");
        try {
            runInServlet("testRetryFourWithOneSkip");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1503W");
        }
    }

    /**
     * When fail over is enabled, verify that a skip is not counted as a fail.
     */
    @Test
    public void testRetryFourWithOneSkipWithFailoverEnabled() throws Exception {
        ServerConfiguration config = savedConfig.clone();
        PersistentExecutor myScheduler = config.getPersistentExecutors().getBy("jndiName", "concurrent/myScheduler");
        myScheduler.setRetryInterval(null);
        myScheduler.setRetryLimit("3");
        myScheduler.setPollInterval("1s32ms"); // polling is needed for retries
        myScheduler.setMissedTaskThreshold("2s");
        myScheduler.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
        server.updateServerConfiguration(config);

        server.startServer("testRetryFourWithOneSkipFE.log");
        try {
            runInServlet("testRetryFourWithOneSkip");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1503W");
        }
    }

    /**
     * Verify that a skip does not stop the 'consecutive failure' count.
     *
     * @throws Exception
     */
    @Test
    public void testRetryFourWithOneSkipFail() throws Exception {
        updateConfiguration("3", null);
        server.startServer("testRetryFourWithOneSkipFail.log");
        try {
            runInServlet("testRetryFourWithOneSkipFail");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1511W", "CWWKC1503W");
        }
    }

    /**
     * When fail over is enabled, verify that a skip does not stop the 'consecutive failure' count.
     */
    @Test
    public void testRetryFourWithOneSkipFailWithFailoverEnabled() throws Exception {
        ServerConfiguration config = savedConfig.clone();
        PersistentExecutor myScheduler = config.getPersistentExecutors().getBy("jndiName", "concurrent/myScheduler");
        myScheduler.setRetryInterval(null);
        myScheduler.setRetryLimit("3");
        myScheduler.setPollInterval("1s33ms"); // polling is needed for retries
        myScheduler.setMissedTaskThreshold("3s");
        myScheduler.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
        server.updateServerConfiguration(config);

        server.startServer("testRetryFourWithOneSkipFailFE.log");
        try {
            runInServlet("testRetryFourWithOneSkipFail");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1511W", "CWWKC1503W");
        }
    }

    /**
     * Set retry limit to 3. Task with AutoPurge=ALWAYS. First 4 attempts fail.
     * Verify TaskStatus is null.
     *
     * @throws Exception
     */
    @Test
    public void testRetryFourTimesAutoPurgeAlways() throws Exception {
        updateConfiguration("3", null);
        server.startServer("testRetryFourTimesAutoPurgeAlways.log");
        try {
            runInServlet("testRetryFourTimesAutoPurgeAlways");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1511W", "CWWKC1503W");
        }
    }

    /**
     * When fail over is enabled, set the retry limit to 3. Schedule a task with AutoPurge=ALWAYS. The first 4 attempts fail.
     * Verify that TaskStatus is null.
     */
    @Test
    public void testRetryFourTimesAutoPurgeAlwaysWithFailoverEnabled() throws Exception {
        ServerConfiguration config = savedConfig.clone();
        PersistentExecutor myScheduler = config.getPersistentExecutors().getBy("jndiName", "concurrent/myScheduler");
        myScheduler.setRetryInterval(null);
        myScheduler.setRetryLimit("3");
        myScheduler.setPollInterval("1s34ms"); // polling is needed for retries
        myScheduler.setMissedTaskThreshold("4s");
        myScheduler.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
        server.updateServerConfiguration(config);

        server.startServer("testRetryFourTimesAutoPurgeAlwaysFE.log");
        try {
            runInServlet("testRetryFourTimesAutoPurgeAlways");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1511W", "CWWKC1503W");
        }
    }

    /**
     * Verify that failure limit -1 causes unlimited retries (well we'll wait for 5
     * anyway).
     *
     * @throws Exception
     */
    @Test
    public void testRetryUnlimited() throws Exception {
        updateConfiguration("-1", null);
        server.startServer("testRetryUnlimited.log");
        try {
            runInServlet("testRetryUnlimited");
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1503W");
        }
    }

    /**
     * Verify that the retry count does not continue to increment (and wrap) when it
     * reaches its max value. It should remain at the max value for each additional
     * failure. We'll use a failure limit of -1 to test this.
     */
    @Test
    public void testRetryCountWrap() throws Exception {
        updateConfiguration("-1", null);
        server.startServer("testRetryCountWrap-1.log");

        // First we'll create a task that always fails. We'll let it run at least
        // once, and then we'll return its ID to this test case.
        StringBuilder servletOutput = runInServlet("testRetryCountWrap_1");
        int indexOfTaskId = servletOutput.indexOf("TASK_ID=");
        if (indexOfTaskId < 0) {
            throw new Exception("Servlet did not print the task ID into its output.");
        }

        StringTokenizer st = new StringTokenizer(servletOutput.substring(indexOfTaskId));
        String idToken = st.nextToken();
        int indexOfEquals = idToken.indexOf("=");
        String taskIdString = idToken.substring(indexOfEquals + 1);
        Log.info(getClass(), "testRetryCountWrap", "read task ID from Servlet: " + taskIdString);

        // Disable the persistent executor feature.
        server.stopServer("CWWKC1501W", "CWWKC1503W", "J2CA0024E", // extreme test infrastructure slowness (5+ minutes
                                                                    // to log FFDC) allows server shutdown to occur
                                                                    // before operation completes
                "J2CA0081E"); // same reason as above
        ServerConfiguration config = server.getServerConfiguration();
        FeatureManager fm = config.getFeatureManager();
        Set<String> featureList = fm.getFeatures();
        featureList.remove("persistentExecutor-1.0");
        featureList.add("jdbc-4.1");
        server.updateServerConfiguration(config);
        server.startServer("testRetryCountWrap-2.log");

        // Update the row in the backing database to set the retry count to a
        // value close to the max value. This way we don't have to wait for it
        // to fail so many times.
        runInServlet("testRetryCountWrap_2", "UpdateDatabase", taskIdString);

        // Restore the Persistent Executor feature.
        server.stopServer("CWNEN0047W", "CWNEN0049W", "CWWKC1503W");
        config = server.getServerConfiguration();
        fm = config.getFeatureManager();
        featureList = fm.getFeatures();
        featureList.remove("jdbc-4.1");
        featureList.add("persistentExecutor-1.0");
        server.updateServerConfiguration(config);
        server.startServer("testRetryCountWrap-3.log");
        try {
            // Due to poor test infrastructure, one time it took 16.5 minutes to report that the transaction service is "recovering no transactions"
            if (server.waitForStringInLog("WTRN0135I", TimeUnit.MINUTES.toMillis(30)) == null)
                fail("It is taking more than 30 minutes for a simple server to become usable.");

            // Re-start the server and let the task fail some more.
            // We can check the retry count in the app (I think).
            runInServlet("testRetryCountWrap_3", null, taskIdString);
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1503W");
        }
    }

    /**
     * With fail over enabled, verify that the retry count does not continue to increment (and wrap) when it
     * reaches its max value. It should remain at the max value for each additional
     * failure. We'll use a failure limit of -1 to test this.
     */
    @Test
    public void testRetryCountWrapWithFailoverEnabled() throws Exception {
        ServerConfiguration cfg = savedConfig.clone();
        PersistentExecutor myScheduler = cfg.getPersistentExecutors().getBy("jndiName", "concurrent/myScheduler");
        // In this test, polling matters because task execution might still be claimed by server instance 1 when server instance 3 comes up.
        myScheduler.setRetryInterval(null);
        myScheduler.setRetryLimit("-1");
        myScheduler.setPollInterval("3s");
        myScheduler.setMissedTaskThreshold("4s");
        myScheduler.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
        server.updateServerConfiguration(cfg);

        server.startServer("testRetryCountWrapFE-1.log");

        // First we'll create a task that always fails. We'll let it run at least
        // once, and then we'll return its ID to this test case.
        StringBuilder servletOutput = runInServlet("testRetryCountWrap_1");
        int indexOfTaskId = servletOutput.indexOf("TASK_ID=");
        if (indexOfTaskId < 0) {
            throw new Exception("Servlet did not print the task ID into its output.");
        }

        StringTokenizer st = new StringTokenizer(servletOutput.substring(indexOfTaskId));
        String idToken = st.nextToken();
        int indexOfEquals = idToken.indexOf("=");
        String taskIdString = idToken.substring(indexOfEquals + 1);
        Log.info(getClass(), "testRetryCountWrap", "read task ID from Servlet: " + taskIdString);

        // Disable the persistent executor feature.
        server.stopServer("CWWKC1501W", "CWWKC1503W", "J2CA0024E", // extreme test infrastructure slowness (5+ minutes
                                                                    // to log FFDC) allows server shutdown to occur
                                                                    // before operation completes
                "J2CA0081E"); // same reason as above
        ServerConfiguration config = server.getServerConfiguration();
        FeatureManager fm = config.getFeatureManager();
        Set<String> featureList = fm.getFeatures();
        featureList.remove("persistentExecutor-1.0");
        featureList.add("jdbc-4.1");
        server.updateServerConfiguration(config);
        server.startServer("testRetryCountWrapFE-2.log");

        // Update the row in the backing database to set the retry count to a
        // value close to the max value. This way we don't have to wait for it
        // to fail so many times.
        runInServlet("testRetryCountWrap_2", "UpdateDatabase", taskIdString);

        // Restore the Persistent Executor feature.
        server.stopServer("CWNEN0047W", "CWNEN0049W", "CWWKC1503W");
        config = server.getServerConfiguration();
        fm = config.getFeatureManager();
        featureList = fm.getFeatures();
        featureList.remove("jdbc-4.1");
        featureList.add("persistentExecutor-1.0");
        server.updateServerConfiguration(config);
        server.startServer("testRetryCountWrapFE-3.log");
        try {
            // Re-start the server and let the task fail some more.
            // We can check the retry count in the app (I think).
            runInServlet("testRetryCountWrap_3", null, taskIdString);
        } finally {
            server.stopServer("CWWKC1501W", "CWWKC1503W");
        }
    }
}