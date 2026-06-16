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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.Constants;
import io.openliberty.microprofile.health.internal_fat.shared.HealthFileUtils;
import io.openliberty.microprofile.health.internal_fat.shared.HealthActions;

@RunWith(FATRunner.class)
public class LongIntervalHealthCheckTest {

    final static String SERVER_DUMMY = "DummyServer";
    final static String SERVER_LONG_STARTUP_CHECK_INTERVAL = "HealthServerLongStartupCheckInterval";
    final static String SERVER_LONG_CHECK_INTERVAL = "HealthServerLongCheckInterval";
    final static String FAIL_START_APP = "FailStartApp";
    final static String FAIL_START_APP_WAR = FAIL_START_APP + ".war";
    final static int MAX_ATTEMPTS = 10;

    // Health check timing constants (in milliseconds)
    private static final long MIN_UPDATE_INTERVAL_MS = 29500L;
    private static final long MAX_UPDATE_INTERVAL_MS = 32000L;
    private static final int MIN_UPDATE_INTERVAL = 29;
    private static final int MAX_UPDATE_INTERVAL = 32;

    private static final String[] IGNORED_FAILURES = { "CWMMH0052W", "CWMMH0054W", "CWMMH0053W", "CWMMH0050E" };

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(FeatureReplacementAction.ALL_SERVERS,
                                                             MicroProfileActions.MP61, // mpHealth-4.0 w/ EE10
                                                             MicroProfileActions.MP70_EE10, // mpHealth-4.0 FULL EE10
                                                             MicroProfileActions.MP70_EE11, // mpHealth-4.0 FULL EE11
                                                             HealthActions.MP14_MPHEALTH40, // mpHealth-4.0 FULL EE7
                                                             HealthActions.MP41_MPHEALTH40); //mpHealth-4.0 FULL EE8

    @Server(SERVER_DUMMY)
    public static LibertyServer server;

    @Server(SERVER_LONG_STARTUP_CHECK_INTERVAL)
    public static LibertyServer serverLongStart;

    @Server(SERVER_LONG_CHECK_INTERVAL)
    public static LibertyServer serverLongCheck;

    @BeforeClass
    public static void beforeClass() throws Exception {
        /*
         *
         * The first test/server-start sometimes?/always? needs to generate a fatFeatureList.xml.
         * Sometimes this takes a VERY LONG TIME. This happens on Windows OS the majority of the time.
         * Other OS platforms can also take a long time, but is much less likely.
         *
         * Previously, the StartedhealthCheckTestLongStartupInterval was the first test to run.
         * If this encountered a long featFeatureList generation then the window of time we wanted
         * to test would already be complete (i.e., health check files have already reached their final state).
         * The test would "start" testing after the fact and would fail.
         *
         * This dummy test is put in place to take the brunt of FAT feature generation.
         *
         **/
        //Test infra checks for fatfeatureList as part of start server.
        Log.info(LongIntervalHealthCheckTest.class, "beforeClass", "starting dummy server");
        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");

        server.stopServer();
        Log.info(LongIntervalHealthCheckTest.class, "beforeClass", "stopping/stopped dummy server");
    }

    @Before
    public void before() throws Exception {
        if (serverLongCheck != null) {
            serverLongCheck.removeAllInstalledAppsForValidation();
            serverLongCheck.deleteAllDropinApplications();
        }
        if (serverLongStart != null) {
            serverLongStart.removeAllInstalledAppsForValidation();
            serverLongStart.deleteAllDropinApplications();
        }
    }

    @After
    public void after() throws Exception {
        if (serverLongCheck != null && serverLongCheck.isStarted()) {
            serverLongCheck.stopServer(IGNORED_FAILURES);
        }

        if (serverLongStart != null && serverLongStart.isStarted()) {
            serverLongStart.stopServer(IGNORED_FAILURES);
        }
    }

    @Test
    /*
     * Startup check is long at 30 seconds.
     * The `StartupCheckUpAfterSecondQuery` servlet only returns UP after second query.
     * This ensures that first query (i.e. first check of the startup check process will fail).
     *
     */
    public void StartedHealthCheckTestLongStartupInterval() throws Exception {
        final String METHOD_NAME = "StartedHealthCheckTestLongStartupInterval";

        boolean diffInRange = false;
        File serverRootDirFile = null;
        int attempts = 0;

        while (!diffInRange) {

            assertTrue("Maximum number of attempts is exceeded", attempts++ < MAX_ATTEMPTS);
            
            Log.info(getClass(), METHOD_NAME, "Attempt: " + attempts);

            // If the difference is not in range, try again as the CPU may have been busy, so restart/cleanup the servers.
            if (attempts > 1) {
                after();
                before();
            }

            WebArchive testWAR = ShrinkWrap
                            .create(WebArchive.class, FAIL_START_APP_WAR)
                            .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                            .addPackage("io.openliberty.microprofile.health.file.healthcheck.app")
                            .addPackage("io.openliberty.microprofile.health.file.healthcheck.app.start.after");

            ShrinkHelper.exportDropinAppToServer(serverLongStart, testWAR, DeployOptions.SERVER_ONLY);

            serverLongStart.startServer();

            // Read to run a smarter planet
            serverLongStart.waitForStringInLogUsingMark("CWWKF0011I");

            assertTrue("Server is not started", serverLongStart.isStarted());

            String serverRoot = serverLongStart.getServerRoot();
            serverRootDirFile = new File(serverRoot);

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

            /*
             * Finding fist entry trace for StartedFileCreateProcess which checks for started health check.
             * This is when the scheduler invokes it, the timer is based on this execution
             * time.
             */
            String traceEntryStartofFirstStartCheck = serverLongStart.waitForStringInTraceUsingMark(".*HealthCheck40ServiceImpl\\$StartedFileCreateProcess > run Entry.*");
            serverLongStart.setMarkToEndOfLog(serverLongStart.getMostRecentTraceFile());

            Log.info(getClass(), METHOD_NAME, "First `run` entry trace: " + traceEntryStartofFirstStartCheck);

            // Expect to see something like this (ISO date format) : 2026-01-06T22:02:43.886+0300
            String dateTimeString = traceEntryStartofFirstStartCheck.split("]")[0].substring(1);
            Log.info(getClass(), METHOD_NAME, "Debug: first `run` trace's timestamp : " + dateTimeString);

            /*
             * Find the second `run` entry trace
             */
            String traceEntryStartofSecondStartCheck = serverLongStart.waitForStringInTraceUsingMark(".*HealthCheck40ServiceImpl\\$StartedFileCreateProcess > run Entry.*", 35000);
            Log.info(getClass(), METHOD_NAME, "Second `run` entry trace: " + traceEntryStartofSecondStartCheck);

            String dateTimeString2 = traceEntryStartofSecondStartCheck.split("]")[0].substring(1);
            Log.info(getClass(), METHOD_NAME, "Debug: second `run` trace timestamp : " + dateTimeString2);

            /*
             * Time to calculate the difference.
             */
            DateTimeFormatter timeFormatterHH = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

            String time = resolveTime(dateTimeString);
            String date = resolveDate(dateTimeString);
            LocalTime timeOfFirstQuery = LocalTime.parse(time, timeFormatterHH);

            String time2 = resolveTime(dateTimeString2);
            String date2 = resolveDate(dateTimeString2);
            LocalTime timeOfSecondQuery = LocalTime.parse(time2, timeFormatterHH);

            // Get time difference.
            long diff = Duration.between(timeOfFirstQuery, timeOfSecondQuery).getSeconds();

            // If test ends on diff date, add one day's worth of seconds time to prevent negative difference.
            if (!date.equals(date2))
                diff += Duration.ofDays(1).getSeconds();

            /*
             * We start with 29 seconds because the tracing the first trace and second trace may have a difference of 29s999ms.
             * That is because potential slowness may have caused the first trace to be emitted x ms after the timer actually was invoked.
             * The traces are not "truly" exact on timing. And when we Convert the difference in duration into seconds, we get 29 seconds difference.
             */

            diffInRange = (diff >= MIN_UPDATE_INTERVAL && diff <= MAX_UPDATE_INTERVAL);

            // If the difference is not in range, try again as the CPU may have been busy.
            if (!diffInRange) {
                Log.info(getClass(), METHOD_NAME,
                         "The difference in time between the two timestamps is (in seconds): " + diff + " but should be " + MIN_UPDATE_INTERVAL + "s or greater (but no more than "
                                                  + MAX_UPDATE_INTERVAL + "s).");
            }
        }

        if (!diffInRange) {
            Log.info(getClass(), METHOD_NAME, "Failed after " + attempts + " attempts.");
        } else {
            Log.info(getClass(), METHOD_NAME, "Passed after " + attempts + " attempts.");
        }

        assertTrue("The difference expected should be " + MIN_UPDATE_INTERVAL + "s or greater (but no more than " + MAX_UPDATE_INTERVAL
                   + "s). We offer extra 2 seconds for potential slowness.", diffInRange);

        assertNotNull(serverLongStart.waitForStringInTraceUsingMark(".*Startup phase for local health check functionality completed.*"));

        /*
         * Expect:
         * [X] /health dir
         * [X] Started
         * [X] Ready
         * [X] Live
         */
        Assert.assertTrue(Constants.STARTED_SHOULD_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

    }

    /*
     * trace date time should be in ISO:
     * e.g. 2026-01-06T22:02:43.886+0300
     */
    String resolveTime(String traceEntryDateTimeStamp) {
        String dateTime[] = traceEntryDateTimeStamp.split("T");
        assertEquals("Should be split into two parts (split by the `T`), the date and time", 2, dateTime.length);

        //ignore zone offset
        String time = dateTime[1].split("[+-]")[0];

        //time can't be null;
        assertNotNull("Unable to resolve time, the time stamp is: " + traceEntryDateTimeStamp, time);

        Log.info(getClass(), "resolveTime", "Debug: the resolved time is: " + time);
        return time;
    }

    String resolveDate(String traceEntryDateTimeStamp) {
        String dateTime[] = traceEntryDateTimeStamp.split("T");
        assertEquals("Should be split into two parts (split by the `T`), the date and time", 2, dateTime.length);

        //ignore zone offset
        String date = dateTime[0];

        //time can't be null;
        assertNotNull("Unable to resolve date, the time stamp is: " + traceEntryDateTimeStamp, date);

        Log.info(getClass(), "resolveDate", "Debug: the resolved date is: " + date);
        return date;
    }

    @Test
    /*
     * check interval is long at 30 seconds.
     */
    public void HealthCheckTestLongCheckInterval() throws Exception {
        final String METHOD_NAME = "HealthCheckTestLongCheckInterval";

        long readyCreatedTime = 0;
        long readyCreateModifiedTime = 0;
        long readyCreatedModifiedTimeDiff = 0;
        boolean readyCreatedModifiedTimeDiffInRange = false;
        long readyModifiedTime = 0;
        long readyUpdateDiff = 0;
        boolean readyUpdateDiffInRange = false;
        File serverRootDirFile = null;
        int attempts = 0;

        while ((!readyCreatedModifiedTimeDiffInRange || !readyUpdateDiffInRange)) {

            readyCreatedModifiedTimeDiffInRange = false;
            readyUpdateDiffInRange = false;

            assertTrue("Maximum number of attempts is exceeded", attempts++ < MAX_ATTEMPTS);

            Log.info(getClass(), METHOD_NAME, "Attempt: " + attempts);

            // If the difference is not in range, try again as the CPU may have been busy, so restart/cleanup the servers.
            if (attempts > 1) {
                after();
                before();
            }

            WebArchive testWAR = ShrinkWrap
                            .create(WebArchive.class, FAIL_START_APP_WAR)
                            .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                            .addPackage("io.openliberty.microprofile.health.file.healthcheck.app");

            ShrinkHelper.exportDropinAppToServer(serverLongCheck, testWAR, DeployOptions.SERVER_ONLY);

            serverLongCheck.startServer();

            // Read to run a smarter planet
            serverLongCheck.waitForStringInLogUsingMark("CWWKF0011I");
            assertTrue("Server is not started", serverLongCheck.isStarted());

            String serverRoot = serverLongCheck.getServerRoot();
            serverRootDirFile = new File(serverRoot);

            Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

            /*
             * Expect:
             * [X] /health dir
             * [x] Started
             * [x] Ready
             * [x] Live
             *
             * Not Expected:
             * [] Started
             * [] Ready
             * [] Live
             */
            /*
             * Checks that require to check that all files are created may encounter a scenario where FAT test is way ahead of the server.
             * This results in the files not existing yet. isFilesCreated() will retry up to 2 seconds (w/ 250ms cycles).
             */
            Assert.assertTrue("Expected all files to be created: Review isAllHealthCheckFilesCreated logs for state of files.", FATSuite.isFilesCreated(serverRootDirFile));

            /*
             * TO REDUCE ON POTENTIAL TEST SLOWNESS, WE'LL ONLY CHECK THE READY FILE.
             * The underlying logic/mechanism is duplicated for updating the ready and live healht-check files.
             */

            readyCreateModifiedTime = HealthFileUtils.getLastModifiedTime(HealthFileUtils.getReadyFile(serverRootDirFile));
            readyCreatedTime = HealthFileUtils.getCreatedTime(HealthFileUtils.getReadyFile(serverRootDirFile));
            readyCreatedModifiedTimeDiff = readyCreateModifiedTime - readyCreatedTime;

            Log.info(getClass(), METHOD_NAME, "Debug: ready file creation time's modified time(ms): " + readyCreateModifiedTime);
            Log.info(getClass(), METHOD_NAME, "Debug: ready file creation time(ms): " + readyCreatedTime);

            readyCreatedModifiedTimeDiffInRange = readyCreatedModifiedTimeDiff <= 1000;

            if (!readyCreatedModifiedTimeDiffInRange) {
                Log.info(getClass(), METHOD_NAME, "Difference between create time and initial modified time is too great for the ready file.");
                continue;
            }

            long currTime = System.currentTimeMillis();
            long diff = currTime - readyCreatedTime;
            Log.info(getClass(), METHOD_NAME, "Diff from file creation time and current run time(ms): " + diff);
            long timeRemaining = 30000L - diff;
            Log.info(getClass(), METHOD_NAME, "Time remaining in the 30 second interval(ms): " + timeRemaining);

            TimeUnit.MILLISECONDS.sleep(timeRemaining / 2);
            readyModifiedTime = HealthFileUtils.getLastModifiedTime(HealthFileUtils.getReadyFile(serverRootDirFile));
            Log.info(getClass(), METHOD_NAME, "The ready file's `new` modified time(ms): " + readyModifiedTime);

            /*
             * If difference is less than a second, its effectively the "same" time.
             * Encountered instances where there is a different of ~100ms.
             * Perhaps something (i.e. OS) touched it immediately after creating it.
             */
            assertTrue("Ready file should not have been updated. The new modified time is (ms): " + readyModifiedTime, (readyModifiedTime - readyCreateModifiedTime) <= 999);

            /*
             * We've waited half the remaining 30 seconds already, we're waiting the second half now and we need to offset
             * (i.e. 7 / 2 = 3. We'd need to wait (3x2) + 1 to hit the full time. Add extra 1.5 seconds offset.
             */
            TimeUnit.MILLISECONDS.sleep((timeRemaining / 2) + 1500);
            readyModifiedTime = HealthFileUtils.getLastModifiedTime(HealthFileUtils.getReadyFile(serverRootDirFile));

            if (readyModifiedTime == readyCreateModifiedTime) {
                Log.info(getClass(), METHOD_NAME, "The last modified time of ready should have been updated, but was still: " + readyModifiedTime);
                continue;
            }

            Log.info(getClass(), METHOD_NAME, "The ready file's `new` modified time(ms):" + readyModifiedTime);

            readyUpdateDiff = readyModifiedTime - readyCreatedTime;
            readyUpdateDiffInRange = readyUpdateDiff >= MIN_UPDATE_INTERVAL_MS && readyUpdateDiff <= MAX_UPDATE_INTERVAL_MS;
            Log.info(getClass(), METHOD_NAME, "The difference between creation time and the ready update is (ms) : " + readyUpdateDiff);

            // If the difference is not in range, try again as the CPU may have been busy.
            if (!readyUpdateDiffInRange) {
                Log.info(getClass(), METHOD_NAME, "The modified time is out of bounds(ms): " + readyUpdateDiff);
                continue;
            }
        }

        if (!readyUpdateDiffInRange) {
            Log.info(getClass(), METHOD_NAME, "Failed after " + attempts + " attempts.");
        } else {
            Log.info(getClass(), METHOD_NAME, "Passed after " + attempts + " attempts.");
        }

        assertTrue("The last modified time of ready should have been updated, but was still: " + readyModifiedTime, readyModifiedTime != readyCreateModifiedTime);

        assertTrue("Difference between create time and initial modified time is too great for the ready file.", readyCreatedModifiedTimeDiffInRange);

        assertTrue("The modified time is out of bounds(ms): " + readyUpdateDiff, readyUpdateDiffInRange); //Allow for 29.5-32 range to allow for quickness or slowness of system.

    }
}