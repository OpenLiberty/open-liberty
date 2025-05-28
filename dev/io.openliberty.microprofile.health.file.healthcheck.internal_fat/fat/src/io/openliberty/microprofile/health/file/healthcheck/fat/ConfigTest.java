/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.Constants;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.HealthFileUtils;
import io.openliberty.microprofile.health.internal_fat.shared.HealthActions;

/**
 *
 */
@RunWith(FATRunner.class)
@AllowedFFDC({ "javax.management.InstanceNotFoundException", "java.lang.IllegalStateException" })
public class ConfigTest {

    final static String SERVER_NAME_ONLY_START_CONFIG = "OnlyStartupCheckHealthServer";

    final static String SERVER_NAME_BAD_CHECK_CONFIG = "BadCheckIntervalHealthServer";

    final static String SERVER_NAME_BAD_START_CONFIG = "BadStartupCheckIntervalHealthServer";

    private static final String[] IGNORED_AND_EXPECTED_FAILURES = { "CWMMH0052W", "CWMMH0054W", "CWMMH0053W", "CWMMH0050E", "CWMMH01012W", "CWMMH01011W", "CWMMH01010W" };

    public static final int APP_STARTUP_TIMEOUT = 120 * 1000;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(FeatureReplacementAction.ALL_SERVERS,
                                                             MicroProfileActions.MP61, // mpHealth-4.0 w/ EE9
                                                             MicroProfileActions.MP70_EE10, // mpHealth-4.0 FULL EE10
                                                             MicroProfileActions.MP70_EE11, // mpHealth-4.0 FULL EE11
                                                             HealthActions.MP14_MPHEALTH40, // mpHealth-4.0 FULL EE7
                                                             HealthActions.MP41_MPHEALTH40); //mpHealth-4.0 FULL EE8

    @Server(SERVER_NAME_ONLY_START_CONFIG)
    public static LibertyServer onlyStartConfigServer;

    @Server(SERVER_NAME_BAD_START_CONFIG)
    public static LibertyServer badStartupCheckIntervalServer;

    @Server(SERVER_NAME_BAD_CHECK_CONFIG)
    public static LibertyServer badCheckIntervalServer;

    public static LibertyServer server;

    @After
    public void after() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(IGNORED_AND_EXPECTED_FAILURES);
        }
    }

    @Test
    /*
     * Only startupCheckInterval is configured expect CWMMH01012W
     */
    public void onlyStartConfigServerTest() throws Exception {
        final String METHOD_NAME = "onlyStartConfigServerTest";

        server = onlyStartConfigServer;
        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");

        assertTrue("Server is not started", server.isStarted());

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [ ] /health dir
         * [ ] Started
         * [ ] Ready
         * [ ] Live
         *
         * Not Expected:
         * [x] /health dir
         * [X] Started
         * [X] Ready
         * [X] Live
         *
         */

        Assert.assertFalse(Constants.HEALTH_DIR_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        List<String> results = server.findStringsInLogs("CWMMH01012W");

        Assert.assertTrue("Did not find CWMMH01012W", results.size() > 0);

    }

    @Test
    /*
     * Bad configuration used for "startupCheckInterval" expect "CWMMH01011W"
     */
    public void badStartupCheckIntervalConfigTest() throws Exception {
        final String METHOD_NAME = "badStartupCheckIntervalConfigTest";

        server = badStartupCheckIntervalServer;
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
         * [x] Started
         * [x] Ready
         * [x] Live
         *
         */
        /*
         * Checks that require to check that all files are created may encounter a scenario where FAT test is way ahead of the server.
         * This results in the files not existing yet. isFilesCreated() will retry up to 2 seconds (w/ 250ms cycles).
         */
        Assert.assertTrue("Expected all files to be created: Review isAllHealthCheckFilesCreated logs for state of files.", FATSuite.isFilesCreated(serverRootDirFile));

        List<String> results = server.findStringsInLogs("CWMMH01011W");

        Assert.assertTrue("Did not find CWMMH01011W", results.size() > 0);

    }

    @Test
    /*
     * Bad configuration used for "checkInterval" expect "CWMMH0101W"
     */
    public void badCheckIntervalConfigTest() throws Exception {
        final String METHOD_NAME = "badCheckIntervalConfigTest";

        server = badCheckIntervalServer;
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
         * [x] Started
         * [x] Ready
         * [x] Live
         *
         */
        /*
         * Checks that require to check that all files are created may encounter a scenario where FAT test is way ahead of the server.
         * This results in the files not existing yet. isFilesCreated() will retry up to 2 seconds (w/ 250ms cycles).
         */
        Assert.assertTrue("Expected all files to be created: Review isAllHealthCheckFilesCreated logs for state of files.", FATSuite.isFilesCreated(serverRootDirFile));

        List<String> results = server.findStringsInLogs("CWMMH01010W");

        Assert.assertTrue("Did not find CWMMH01010W", results.size() > 0);

    }

}
