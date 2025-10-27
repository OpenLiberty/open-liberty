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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.Constants;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.HealthFileUtils;

@RunWith(FATRunner.class)
public class LongStartupHealthCheckTest extends BaseHealthFilesTest {

    private static final String FAIL_START_APP = "FailStartApp";
    private static final String FAIL_START_APP_WAR = FAIL_START_APP + ".war";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(
                                                             FeatureReplacementAction.ALL_SERVERS,
                                                             MicroProfileActions.MP61,
                                                             MicroProfileActions.MP70_EE10,
                                                             MicroProfileActions.MP70_EE11,
                                                             io.openliberty.microprofile.health.internal_fat.shared.HealthActions.MP14_MPHEALTH40,
                                                             io.openliberty.microprofile.health.internal_fat.shared.HealthActions.MP41_MPHEALTH40);

    @Server(SERVER_LONG_STARTUP_CHECK_INTERVAL)
    public static LibertyServer serverLongStart;

    private static final String[] IGNORED_FAILURES = { "CWMMH0052W", "CWMMH0054W", "CWMMH0053W", "CWMMH0050E" };

    @Before
    public void before() throws Exception {
        if (serverLongStart != null) {
            serverLongStart.removeAllInstalledAppsForValidation();
            serverLongStart.deleteAllDropinApplications();
            if (serverLongStart.isStarted()) {
                serverLongStart.stopServer(IGNORED_FAILURES);
            }
        }
    }

    @After
    public void after() throws Exception {
        if (serverLongStart != null && serverLongStart.isStarted()) {
            serverLongStart.stopServer(IGNORED_FAILURES);
        }
    }

    @Test
    public void StartedHealthCheckTestLongStartupInterval() throws Exception {
        final String METHOD_NAME = "StartedHealthCheckTestLongStartupInterval";

        WebArchive testWAR = ShrinkWrap.create(WebArchive.class, FAIL_START_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app")
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app.start.after");

        ShrinkHelper.exportDropinAppToServer(serverLongStart, testWAR, DeployOptions.SERVER_ONLY);

        serverLongStart.startServer();
        serverLongStart.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", serverLongStart.isStarted());

        File root = new File(serverLongStart.getServerRoot());
        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + root.getAbsolutePath());

        // While startup check is still running, no files should exist yet
        awaitHealthDir(root);
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(root).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(root).exists());
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(root).exists());

        Assert.assertTrue(Constants.STARTED_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getStartFile(root), 8, 200));
        Assert.assertTrue(Constants.READY_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getReadyFile(root), 8, 200));
        Assert.assertTrue(Constants.LIVE_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getLiveFile(root), 8, 200));

        // final small window still absent
        Assert.assertTrue(Constants.STARTED_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getStartFile(root), 5, 200));
        Assert.assertTrue(Constants.READY_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getReadyFile(root), 5, 200));
        Assert.assertTrue(Constants.LIVE_SHOULD_NOT_HAVE_CREATED,
                          HealthFileUtils.waitForFileMissing(HealthFileUtils.getLiveFile(root), 5, 200));

        // Now allow enough time for a 30s startupCheckInterval (use 60s cushion)
        Assert.assertTrue(Constants.STARTED_SHOULD_HAVE_CREATED,
                          HealthFileUtils.waitForFileExists(HealthFileUtils.getStartFile(root), 60, 200));
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_CREATED,
                          HealthFileUtils.waitForFileExists(HealthFileUtils.getReadyFile(root), 60, 200));
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_CREATED,
                          HealthFileUtils.waitForFileExists(HealthFileUtils.getLiveFile(root), 60, 200));
    }
}
