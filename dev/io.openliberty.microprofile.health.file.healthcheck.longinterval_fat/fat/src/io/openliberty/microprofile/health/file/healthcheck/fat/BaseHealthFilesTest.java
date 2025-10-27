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

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Assert;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.Constants;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.HealthFileUtils;

/**
 * Pure helper base (no @RunWith, no @Before/@After, no LibertyServer fields).
 * Subclasses own their @Server fields and their cleanup.
 */
public abstract class BaseHealthFilesTest {

    protected static final String SERVER_NAME = "HealthServer";
    protected static final String SERVER_LONG_STARTUP_CHECK_INTERVAL = "HealthServerLongStartupCheckInterval";
    protected static final String SERVER_LONG_CHECK_INTERVAL = "HealthServerLongCheckInterval";

    // Wait settings: up to 3 minutes, 1s polling
    private static final int FILE_WAIT_TRIES = 180;
    private static final int FILE_WAIT_SLEEP_MS = 1000;

    /**
     * Proactively hit the health endpoints once the server is up to
     * force mpHealth to initialize and write the /health directory and files.
     */
    protected static void triggerHealthEndpoints(LibertyServer server) throws Exception {
        // /health
        URL url = HttpUtils.createURL(server, "/health");
        HttpURLConnection con = HttpUtils.getHttpConnection(
                                                            url, HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
        try {
            con.connect();
            con.getResponseCode();
        } catch (Exception ignore) {
            /* 200/204/503 are all fine */ }

        // /health/live
        url = HttpUtils.createURL(server, "/health/live");
        con = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
        try {
            con.connect();
            con.getResponseCode();
        } catch (Exception ignore) {
        }

        // /health/ready
        url = HttpUtils.createURL(server, "/health/ready");
        con = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
        try {
            con.connect();
            con.getResponseCode();
        } catch (Exception ignore) {
        }
    }

    protected static void awaitHealthDir(File serverRootDirFile) {
        final File healthDir = HealthFileUtils.getHealthDirFile(serverRootDirFile);
        Log.info(BaseHealthFilesTest.class, "awaitHealthDir",
                 "Waiting for health dir: " + healthDir.getAbsolutePath());
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE_CREATED,
                          HealthFileUtils.waitForFileExists(healthDir, FILE_WAIT_TRIES, FILE_WAIT_SLEEP_MS));
    }

    protected static void awaitAllHealthFiles(File serverRootDirFile) {
        final File startFile = HealthFileUtils.getStartFile(serverRootDirFile);
        final File readyFile = HealthFileUtils.getReadyFile(serverRootDirFile);
        final File liveFile = HealthFileUtils.getLiveFile(serverRootDirFile);

        Log.info(BaseHealthFilesTest.class, "awaitAllHealthFiles",
                 "Waiting for files:\n  started=" + startFile.getAbsolutePath()
                                                                   + "\n  ready=" + readyFile.getAbsolutePath()
                                                                   + "\n  live=" + liveFile.getAbsolutePath());

        awaitHealthDir(serverRootDirFile);

        Assert.assertTrue(Constants.STARTED_SHOULD_HAVE_CREATED,
                          HealthFileUtils.waitForFileExists(startFile, FILE_WAIT_TRIES, FILE_WAIT_SLEEP_MS));
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_CREATED,
                          HealthFileUtils.waitForFileExists(readyFile, FILE_WAIT_TRIES, FILE_WAIT_SLEEP_MS));
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_CREATED,
                          HealthFileUtils.waitForFileExists(liveFile, FILE_WAIT_TRIES, FILE_WAIT_SLEEP_MS));
    }
}