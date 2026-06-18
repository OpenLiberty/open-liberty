/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health.file.healthcheck.fat;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.Constants;
import io.openliberty.microprofile.health.internal_fat.shared.HealthFileUtils;
import io.openliberty.microprofile.health.internal_fat.shared.HealthActions;

/**
 *
 */
@RunWith(FATRunner.class)
@AllowedFFDC({ "javax.management.InstanceNotFoundException", "java.lang.IllegalStateException" })
public class SimpleFileBasedHealthCheckTest {

    final static String SERVER_NAME = "HealthServer";

    final static String FAIL_START_APP = "FailStartApp";
    final static String FAIL_START_APP_WAR = FAIL_START_APP + ".war";

    final static String FAIL_LIVE_APP = "FailLiveApp";
    final static String FAIL_LIVE_APP_WAR = FAIL_LIVE_APP + ".war";

    final static String FAIL_READY_APP = "FailReadyApp";
    final static String FAIL_READY_APP_WAR = FAIL_READY_APP + ".war";
    final static String TOGGLE_APP = "ToggleApp";
    final static String TOGGLE_APP_WAR = TOGGLE_APP + ".war";

    private static final String[] IGNORED_FAILURES = { "CWMMH0052W", "CWMMH0054W", "CWMMH0053W", "CWMMH0050E" };

    public static final int APP_STARTUP_TIMEOUT = 120 * 1000;

    private static enum HealthCheck {
        LIVE, READY, STARTED, HEALTH;
    }

    private static enum Status {
        SUCCESS, FAILURE;
    }

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(FeatureReplacementAction.ALL_SERVERS,
                                                             MicroProfileActions.MP61, // mpHealth-4.0 w/ EE9
                                                             MicroProfileActions.MP70_EE10, // mpHealth-4.0 FULL EE10
                                                             MicroProfileActions.MP70_EE11, // mpHealth-4.0 FULL EE11
                                                             HealthActions.MP14_MPHEALTH40, // mpHealth-4.0 FULL EE7
                                                             HealthActions.MP41_MPHEALTH40); //mpHealth-4.0 FULL EE8

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() {

    }

    @Before
    public void before() throws Exception {
        server.removeAllInstalledAppsForValidation();
        server.deleteAllDropinApplications();
    }

    @After
    public void after() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(IGNORED_FAILURES);
        }
    }

    /* 
     * For tests checking for the ready and live file, check periodically instead of waiting for whole 10s.
     */
    public void waitForModifiedTimestamp(File serverRootDirFile) throws Exception {
        File readyFile = HealthFileUtils.getReadyFile(serverRootDirFile);
        File liveFile = HealthFileUtils.getLiveFile(serverRootDirFile);

        long readyFileModifiedTimestamp = HealthFileUtils.getLastModifiedTime(readyFile);
        long liveFileModifiedTimestamp = HealthFileUtils.getLastModifiedTime(liveFile);
        long deadlineTime = System.currentTimeMillis() + 10000;

        boolean readyFileUpdated = false;
        boolean liveFileUpdated = false;

        while (System.currentTimeMillis() < deadlineTime) {
            long newUpdatedReadyFileTimestamp = HealthFileUtils.getLastModifiedTime(readyFile);
            long newUpdatedLiveFileTimestamp = HealthFileUtils.getLastModifiedTime(liveFile);

            if (!liveFileUpdated && newUpdatedLiveFileTimestamp > liveFileModifiedTimestamp) {
                liveFileModifiedTimestamp = newUpdatedLiveFileTimestamp;
                liveFileUpdated = true;
            }
            if (!readyFileUpdated && newUpdatedReadyFileTimestamp > readyFileModifiedTimestamp) {
                readyFileModifiedTimestamp = newUpdatedReadyFileTimestamp;
                readyFileUpdated = true;
            }

            if (readyFileUpdated && liveFileUpdated) return;            

            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

    @Test
    /*
     * No configuration used.
     */
    public void emptyServerCheck() throws Exception {
        final String METHOD_NAME = "emptyServerCheck";

        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");

        assertTrue("Server is not started", server.isStarted());

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [X] /health dir
         * [X] Started
         * [X] Ready
         * [X] Live
         *
         */
        /*
         * Checks that require to check that all files are created may encounter a scenario where FAT test is way ahead of the server.
         * This results in the files not existing yet. isFilesCreated() will retry up to 2 seconds (w/ 250ms cycles).
         */
        Assert.assertTrue("Expected all files to be created: Review isAllHealthCheckFilesCreated logs for state of files.", FATSuite.isFilesCreated(serverRootDirFile));

        waitForModifiedTimestamp(serverRootDirFile);

        //Check that live and ready files have been updating.
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_UPDATED,
                          HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getReadyFile(serverRootDirFile), Duration.ofSeconds(8)));
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_UPDATED, HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));

    }

    @Test
    /*
     * Startup check fails.
     */
    public void failedStartedHealthCheckTest() throws Exception {
        final String METHOD_NAME = "failedStartedHealthCheckTest";

        WebArchive testWAR = ShrinkWrap
                        .create(WebArchive.class, FAIL_START_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app")
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app.start.fail");

        ShrinkHelper.exportDropinAppToServer(server, testWAR, DeployOptions.SERVER_ONLY);

        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [X] /health dir
         * [ ] Started
         * [ ] Ready
         * [ ] Live
         *
         * Not Expected:
         * [X] Started
         * [X] Ready
         * [X] Live
         */
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE_CREATED, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        //Started file should still not be created; consequently no other files are created
        TimeUnit.SECONDS.sleep(10);
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

    }

    @Test
    /*
     * Liveness check fails.
     *
     */
    public void failedLivenessHealthCheckTest() throws Exception {
        final String METHOD_NAME = "failedLivenessHealthCheckTest";

        WebArchive testWAR = ShrinkWrap
                        .create(WebArchive.class, FAIL_LIVE_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app")
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app.live.fail");

        ShrinkHelper.exportDropinAppToServer(server, testWAR, DeployOptions.SERVER_ONLY);

        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [X] /health dir
         * [ ] Started
         * [ ] Ready
         * [ ] Live
         *
         * Not Expected:
         * [X] Started
         * [X] Ready
         * [X] Live
         */
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE_CREATED, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        TimeUnit.SECONDS.sleep(10);

        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE_CREATED, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

    }

    @Test
    /*
     * Readiness check fails.
     */
    public void failedReadinessHealthCheckTest() throws Exception {
        final String METHOD_NAME = "failedReadinessHealthCheckTest";

        WebArchive app = ShrinkHelper.buildDefaultApp(FAIL_READY_APP, "io.openliberty.microprofile.health.file.healthcheck.app",
                                                      "io.openliberty.microprofile.health.file.healthcheck.app.ready.fail");

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [X] /health dir
         * [ ] Started
         * [ ] Ready
         * [ ] Live
         *
         * Not Expected:
         * [X] Started
         * [X] Ready
         * [X] Live
         */
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE_CREATED, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        TimeUnit.SECONDS.sleep(10);

        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE_CREATED, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());
    }

    @Test
    /*
     * Readiness check fails during runtime.
     *
     * And then later is updated.
     */
    public void toggleReadinessFailTest() throws Exception {
        final String METHOD_NAME = "toggleReadinessFailTest";
        final int MAX_ATTEMPTS = 10;
        final long CHECK_INTERVAL_MS = 5000L; // 5 second interval
        final long MIN_UPDATE_INTERVAL_MS = 4500L; // Allow 4.5-6.5 second range
        final long MAX_UPDATE_INTERVAL_MS = 6500L;

        boolean testPassed = false;
        int attempts = 0;

        while (!testPassed && attempts < MAX_ATTEMPTS) {
            attempts++;
            Log.info(getClass(), METHOD_NAME, "Attempt: " + attempts);

            // Clean up from previous attempt if needed
            if (attempts > 1) {
                after();
                before();
            }

            WebArchive app = ShrinkWrap
                            .create(WebArchive.class, TOGGLE_APP_WAR)
                            .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                            .addPackage("io.openliberty.microprofile.health.file.healthcheck.app");

            ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

            server.startServer();

            // Read to run a smarter planet
            server.waitForStringInLogUsingMark("CWWKF0011I");
            assertTrue("Server is not started", server.isStarted());

            String serverRoot = server.getServerRoot();
            File serverRootDirFile = new File(serverRoot);

            Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

            /*
             * Expect:
             * [X] /health dir
             * [X] Started
             * [X] Ready
             * [X] Live
             */
            Assert.assertTrue("Expected all files to be created: Review isAllHealthCheckFilesCreated logs for state of files.",
                            FATSuite.isFilesCreated(serverRootDirFile));

            // Get creation times and initial modified times
            long readyCreatedTime = HealthFileUtils.getCreatedTime(HealthFileUtils.getReadyFile(serverRootDirFile));
            long liveCreatedTime = HealthFileUtils.getCreatedTime(HealthFileUtils.getLiveFile(serverRootDirFile));
            long readyInitialModifiedTime = HealthFileUtils.getLastModifiedTimeNIO(HealthFileUtils.getReadyFile(serverRootDirFile));
            long liveInitialModifiedTime = HealthFileUtils.getLastModifiedTimeNIO(HealthFileUtils.getLiveFile(serverRootDirFile));

            Log.info(getClass(), METHOD_NAME, "Ready file created time (ms): " + readyCreatedTime);
            Log.info(getClass(), METHOD_NAME, "Ready file initial modified time (ms): " + readyInitialModifiedTime);
            Log.info(getClass(), METHOD_NAME, "Live file created time (ms): " + liveCreatedTime);
            Log.info(getClass(), METHOD_NAME, "Live file initial modified time (ms): " + liveInitialModifiedTime);

            // Verify creation and initial modification times are close (within 1 second)
            long readyCreateModDiff = readyInitialModifiedTime - readyCreatedTime;
            long liveCreateModDiff = liveInitialModifiedTime - liveCreatedTime;

            if (readyCreateModDiff > 1000 || liveCreateModDiff > 1000) {
                Log.info(getClass(), METHOD_NAME, "Creation and initial modification times too far apart. Ready diff: "
                        + readyCreateModDiff + "ms, Live diff: " + liveCreateModDiff + "ms. Retrying...");
                continue;
            }

            // Toggle ready to false
            URL url = HttpUtils.createURL(server, "/" + TOGGLE_APP + "/HealthAppServlet?ready=false");
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
            try {
                con.connect();
                Assert.assertTrue("200 Response code expected", con.getResponseCode() == 200);
            } finally {
                con.disconnect();
            }

            // Calculate time remaining in the current 5-second interval for live file
            long currTime = System.currentTimeMillis();
            long timeSinceLiveCreation = currTime - liveCreatedTime;
            long timeRemainingInInterval = CHECK_INTERVAL_MS - (timeSinceLiveCreation % CHECK_INTERVAL_MS);
            
            Log.info(getClass(), METHOD_NAME, "Time since live file creation (ms): " + timeSinceLiveCreation);
            Log.info(getClass(), METHOD_NAME, "Time remaining in current 5s interval (ms): " + timeRemainingInInterval);

            // Wait for the next update cycle plus a bit more to ensure update happens
            TimeUnit.MILLISECONDS.sleep(timeRemainingInInterval + 1500);

            // Check that live was updated but ready was not
            long afterToggleReadyModifiedTime = HealthFileUtils.getLastModifiedTimeNIO(HealthFileUtils.getReadyFile(serverRootDirFile));
            long afterToggleLiveModifiedTime = HealthFileUtils.getLastModifiedTimeNIO(HealthFileUtils.getLiveFile(serverRootDirFile));

            Log.info(getClass(), METHOD_NAME, "After toggle ready modified time (ms): " + afterToggleReadyModifiedTime);
            Log.info(getClass(), METHOD_NAME, "After toggle live modified time (ms): " + afterToggleLiveModifiedTime);

            long liveUpdateDiff = afterToggleLiveModifiedTime - liveCreatedTime;
            long readyUpdateDiff = afterToggleReadyModifiedTime - readyCreatedTime;

            Log.info(getClass(), METHOD_NAME, "Live file update diff from creation (ms): " + liveUpdateDiff);
            Log.info(getClass(), METHOD_NAME, "Ready file update diff from creation (ms): " + readyUpdateDiff);

            // Live should have been updated (difference from creation should be in range)
            if (liveUpdateDiff < MIN_UPDATE_INTERVAL_MS || liveUpdateDiff > MAX_UPDATE_INTERVAL_MS) {
                Log.info(getClass(), METHOD_NAME, "Live file update diff out of range: " + liveUpdateDiff + "ms. Retrying...");
                continue;
            }

            // Ready should NOT have been updated (should still be close to creation time)
            if (readyUpdateDiff > 1000) {
                Log.info(getClass(), METHOD_NAME, "Ready file was updated when it shouldn't have been. Diff: " + readyUpdateDiff + "ms. Retrying...");
                continue;
            }

            // Now toggle ready back to true
            url = HttpUtils.createURL(server, "/" + TOGGLE_APP + "/HealthAppServlet?ready=true");
            con = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
            try {
                con.connect();
                Assert.assertTrue("200 Response code expected", con.getResponseCode() == 200);
            } finally {
                con.disconnect();
            }

            // Wait for both files to update
            waitForModifiedTimestamp(serverRootDirFile);

            long finalReadyModifiedTime = HealthFileUtils.getLastModifiedTimeNIO(HealthFileUtils.getReadyFile(serverRootDirFile));
            long finalLiveModifiedTime = HealthFileUtils.getLastModifiedTimeNIO(HealthFileUtils.getLiveFile(serverRootDirFile));

            Log.info(getClass(), METHOD_NAME, "Final ready modified time (ms): " + finalReadyModifiedTime);
            Log.info(getClass(), METHOD_NAME, "Final live modified time (ms): " + finalLiveModifiedTime);

            // Verify both files were updated (should be different from their previous values)
            // Allow for the fact that ready file may have been updated 1-2 cycles after toggle
            if (finalReadyModifiedTime == afterToggleReadyModifiedTime) {
                Log.info(getClass(), METHOD_NAME, "Ready file was not updated after toggling back to true. Retrying...");
                continue;
            }

            if (finalLiveModifiedTime == afterToggleLiveModifiedTime) {
                Log.info(getClass(), METHOD_NAME, "Live file was not updated after toggling back to true. Retrying...");
                continue;
            }

            // Verify the files are being updated at reasonable intervals (within 1-2 cycles of 5 seconds)
            long readyFinalDiff = finalReadyModifiedTime - afterToggleReadyModifiedTime;
            long liveFinalDiff = finalLiveModifiedTime - afterToggleLiveModifiedTime;

            Log.info(getClass(), METHOD_NAME, "Ready file final update diff (ms): " + readyFinalDiff);
            Log.info(getClass(), METHOD_NAME, "Live file final update diff (ms): " + liveFinalDiff);

            // Allow up to 2 update cycles (10 seconds) for ready file since it was down
            if (readyFinalDiff < MIN_UPDATE_INTERVAL_MS || readyFinalDiff > (MAX_UPDATE_INTERVAL_MS * 2)) {
                Log.info(getClass(), METHOD_NAME, "Ready file final update diff out of range: " + readyFinalDiff + "ms. Expected 4.5-13s. Retrying...");
                continue;
            }

            // Live file should update within one cycle
            if (liveFinalDiff < MIN_UPDATE_INTERVAL_MS || liveFinalDiff > MAX_UPDATE_INTERVAL_MS) {
                Log.info(getClass(), METHOD_NAME, "Live file final update diff out of range: " + liveFinalDiff + "ms. Retrying...");
                continue;
            }

            // All checks passed!
            testPassed = true;
            Log.info(getClass(), METHOD_NAME, "Test passed on attempt " + attempts);
        }

        assertTrue("Test failed after " + attempts + " attempts. Files did not update within expected intervals.", testPassed);
    }

    @Test
    /*
     * Liveness check fails during runtime.
     */
    public void toggleLivenessFailTest() throws Exception {
        final String METHOD_NAME = "toggleLivenessFailTest";

        WebArchive app = ShrinkWrap
                        .create(WebArchive.class, TOGGLE_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app");

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [X] /health dir
         * [X] Started
         * [X] Ready
         * [X] Live
         *
         * Not Expected:
         * [ ] Started
         * [ ] Ready
         * [ ] Live
         */
        /*
         * Checks that require to check that all files are created may encounter a scenario where FAT test is way ahead of the server.
         * This results in the files not existing yet. isFilesCreated() will retry up to 2 seconds (w/ 250ms cycles).
         */
        Assert.assertTrue("Expected all files to be created: Review isAllHealthCheckFilesCreated logs for state of files.", FATSuite.isFilesCreated(serverRootDirFile));

        URL url = HttpUtils.createURL(server, "/" + TOGGLE_APP + "/HealthAppServlet?live=false");
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
        try {
            con.connect();
            Assert.assertTrue("200 Response code expected", con.getResponseCode() == 200);
        } finally {
            con.disconnect();
        }

        TimeUnit.SECONDS.sleep(10);

        /*
         * Now expect live to not be updated. Expect ready to have been updated.
         */

        Assert.assertTrue(Constants.READY_SHOULD_HAVE_UPDATED,
                          HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getReadyFile(serverRootDirFile), Duration.ofSeconds(8)));
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_UPDATED,
                           HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));

        /*
         * Set the live status back to UP; expect both files to have been updated
         */

        url = HttpUtils.createURL(server, "/" + TOGGLE_APP + "/HealthAppServlet?live=true");
        con = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
        try {
            con.connect();
            Assert.assertTrue("200 Response code expected", con.getResponseCode() == 200);
        } finally {
            con.disconnect();
        }

        waitForModifiedTimestamp(serverRootDirFile);
        
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_UPDATED,
                          HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getReadyFile(serverRootDirFile), Duration.ofSeconds(8)));
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_UPDATED,
                          HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));

    }

}
