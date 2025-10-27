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
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.HealthFileUtils;

@RunWith(FATRunner.class)
@AllowedFFDC({ "javax.management.InstanceNotFoundException", "java.lang.IllegalStateException" })
public class SimpleFileBasedHealthCheckTest extends BaseHealthFilesTest {

    private static final String FAIL_START_APP = "FailStartApp";
    private static final String FAIL_START_APP_WAR = FAIL_START_APP + ".war";

    private static final String FAIL_LIVE_APP = "FailLiveApp";
    private static final String FAIL_LIVE_APP_WAR = FAIL_LIVE_APP + ".war";

    private static final String FAIL_READY_APP = "FailReadyApp";
    private static final String FAIL_READY_APP_WAR = FAIL_READY_APP + ".war";

    private static final String TOGGLE_APP = "ToggleApp";
    private static final String TOGGLE_APP_WAR = TOGGLE_APP + ".war";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(
                                                             FeatureReplacementAction.ALL_SERVERS,
                                                             MicroProfileActions.MP61, // EE9 mpHealth-4.0
                                                             MicroProfileActions.MP70_EE10, // EE10
                                                             MicroProfileActions.MP70_EE11, // EE11
                                                             io.openliberty.microprofile.health.internal_fat.shared.HealthActions.MP14_MPHEALTH40, // EE7
                                                             io.openliberty.microprofile.health.internal_fat.shared.HealthActions.MP41_MPHEALTH40 // EE8
    );

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final String[] IGNORED_FAILURES = { "CWMMH0052W", "CWMMH0054W", "CWMMH0053W", "CWMMH0050E" };

    @Before
    public void before() throws Exception {
        if (server != null) {
            server.removeAllInstalledAppsForValidation();
            server.deleteAllDropinApplications();
            if (server.isStarted()) {
                server.stopServer(IGNORED_FAILURES);
            }
        }
    }

    @After
    public void after() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(IGNORED_FAILURES);
        }
    }

    // ----- Tests that use only the default server -----

    @Test
    public void emptyServerCheck() throws Exception {
        final String METHOD_NAME = "emptyServerCheck";

        server.startServer();
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());
        triggerHealthEndpoints(server); // ensure /health is initialized

        File serverRootDirFile = new File(server.getServerRoot());
        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        awaitAllHealthFiles(serverRootDirFile);

        long ready0 = HealthFileUtils.getLastModifiedTime(HealthFileUtils.getReadyFile(serverRootDirFile));
        long live0 = HealthFileUtils.getLastModifiedTime(HealthFileUtils.getLiveFile(serverRootDirFile));

        Assert.assertTrue("ready should update shortly",
                          HealthFileUtils.waitForUpdateSince(HealthFileUtils.getReadyFile(serverRootDirFile), ready0, 20, 200));
        Assert.assertTrue("live should update shortly",
                          HealthFileUtils.waitForUpdateSince(HealthFileUtils.getLiveFile(serverRootDirFile), live0, 20, 200));

        Assert.assertTrue(Constants.READY_SHOULD_HAVE_UPDATED,
                          HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getReadyFile(serverRootDirFile), Duration.ofSeconds(8)));
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_UPDATED,
                          HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));
    }

    @Test
    public void failedStartedHealthCheckTest() throws Exception {
        final String METHOD_NAME = "failedStartedHealthCheckTest";

        WebArchive testWAR = ShrinkWrap.create(WebArchive.class, FAIL_START_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app")
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app.start.fail");

        ShrinkHelper.exportDropinAppToServer(server, testWAR, DeployOptions.SERVER_ONLY);

        server.startServer();
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());
        triggerHealthEndpoints(server);

        File serverRootDirFile = new File(server.getServerRoot());
        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        awaitHealthDir(serverRootDirFile);
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        Assert.assertTrue(Constants.STARTED_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getStartFile(serverRootDirFile), 8, 200));
        Assert.assertTrue(Constants.READY_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getReadyFile(serverRootDirFile), 8, 200));
        Assert.assertTrue(Constants.LIVE_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getLiveFile(serverRootDirFile), 8, 200));
    }

    @Test
    public void failedLivenessHealthCheckTest() throws Exception {
        final String METHOD_NAME = "failedLivenessHealthCheckTest";

        WebArchive testWAR = ShrinkWrap.create(WebArchive.class, FAIL_LIVE_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app")
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app.live.fail");

        ShrinkHelper.exportDropinAppToServer(server, testWAR, DeployOptions.SERVER_ONLY);

        server.startServer();
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());
        triggerHealthEndpoints(server);

        File serverRootDirFile = new File(server.getServerRoot());
        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        awaitHealthDir(serverRootDirFile);
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        Assert.assertTrue(Constants.STARTED_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getStartFile(serverRootDirFile), 8, 200));
        Assert.assertTrue(Constants.READY_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getReadyFile(serverRootDirFile), 8, 200));
        Assert.assertTrue(Constants.LIVE_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getLiveFile(serverRootDirFile), 8, 200));
    }

    @Test
    public void failedReadinessHealthCheckTest() throws Exception {
        final String METHOD_NAME = "failedReadinessHealthCheckTest";

        WebArchive app = ShrinkHelper.buildDefaultApp(
                                                      FAIL_READY_APP,
                                                      "io.openliberty.microprofile.health.file.healthcheck.app",
                                                      "io.openliberty.microprofile.health.file.healthcheck.app.ready.fail");

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        server.startServer();
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());
        triggerHealthEndpoints(server);

        File serverRootDirFile = new File(server.getServerRoot());
        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        awaitHealthDir(serverRootDirFile);
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        Assert.assertTrue(Constants.STARTED_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getStartFile(serverRootDirFile), 8, 200));
        Assert.assertTrue(Constants.READY_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getReadyFile(serverRootDirFile), 8, 200));
        Assert.assertTrue(Constants.LIVE_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getLiveFile(serverRootDirFile), 8, 200));
    }

    @Test
    public void toggleReadinessFailTest() throws Exception {
        final String METHOD_NAME = "toggleReadinessFailTest";

        WebArchive app = ShrinkWrap.create(WebArchive.class, TOGGLE_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app");

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        server.startServer();
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());
        triggerHealthEndpoints(server);

        File root = new File(server.getServerRoot());
        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + root.getAbsolutePath());

        // ensure initial files exist
        awaitAllHealthFiles(root);

        File startedFile = HealthFileUtils.getStartFile(root);
        File readyFile = HealthFileUtils.getReadyFile(root);
        File liveFile = HealthFileUtils.getLiveFile(root);

        long live0 = HealthFileUtils.getLastModifiedTime(liveFile);
        long ready0 = HealthFileUtils.getLastModifiedTime(readyFile);

        // ready=false
        URL url = HttpUtils.createURL(server, "/" + TOGGLE_APP + "/HealthAppServlet?ready=false");
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
        con.connect();
        Assert.assertEquals(200, con.getResponseCode());

        Assert.assertTrue("live should update quickly after toggle",
                          HealthFileUtils.waitForUpdateSince(liveFile, live0, 15, 200));
        Assert.assertTrue(Constants.READY_SHOULD_NOT_HAVE_UPDATED,
                          HealthFileUtils.waitForNoUpdateSince(readyFile, ready0, 8, 200));

        // ready=true
        ready0 = HealthFileUtils.getLastModifiedTime(readyFile);
        live0 = HealthFileUtils.getLastModifiedTime(liveFile);

        url = HttpUtils.createURL(server, "/" + TOGGLE_APP + "/HealthAppServlet?ready=true");
        con = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
        con.connect();
        Assert.assertEquals(200, con.getResponseCode());

        Assert.assertTrue("ready should update quickly after toggle",
                          HealthFileUtils.waitForUpdateSince(readyFile, ready0, 15, 200));
        Assert.assertTrue("live should update again",
                          HealthFileUtils.waitForUpdateSince(liveFile, live0, 15, 200));
    }
}
