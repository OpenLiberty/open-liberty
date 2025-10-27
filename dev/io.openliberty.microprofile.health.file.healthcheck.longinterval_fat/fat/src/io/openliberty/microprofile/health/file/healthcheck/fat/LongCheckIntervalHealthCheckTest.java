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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.Constants;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.HealthFileUtils;

@RunWith(FATRunner.class)
public class LongCheckIntervalHealthCheckTest extends BaseHealthFilesTest {

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

    @Server(SERVER_LONG_CHECK_INTERVAL)
    public static LibertyServer serverLongCheck;

    private static final String[] IGNORED_FAILURES = { "CWMMH0052W", "CWMMH0054W", "CWMMH0053W", "CWMMH0050E" };

    @Before
    public void before() throws Exception {
        if (serverLongCheck != null) {
            serverLongCheck.removeAllInstalledAppsForValidation();
            serverLongCheck.deleteAllDropinApplications();
            if (serverLongCheck.isStarted()) {
                serverLongCheck.stopServer(IGNORED_FAILURES);
            }
        }
    }

    @After
    public void after() throws Exception {
        if (serverLongCheck != null && serverLongCheck.isStarted()) {
            serverLongCheck.stopServer(IGNORED_FAILURES);
        }
    }

    /** sample lastModified twice until it stops moving (short cap) */
    private static void stabilize(File f, long maxSeconds) {
        long deadline = System.nanoTime() + Duration.ofSeconds(maxSeconds).toNanos();
        long prev = HealthFileUtils.getLastModifiedTime(f);
        while (System.nanoTime() < deadline) {
            try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            long now = HealthFileUtils.getLastModifiedTime(f);
            if (now == prev) return;
            prev = now;
        }
    }

    @Test
    public void HealthCheckTestLongCheckInterval() throws Exception {
        final String METHOD = "HealthCheckTestLongCheckInterval";

        WebArchive testWAR = ShrinkWrap.create(WebArchive.class, FAIL_START_APP_WAR)
                .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                .addPackage("io.openliberty.microprofile.health.file.healthcheck.app");

        ShrinkHelper.exportDropinAppToServer(serverLongCheck, testWAR, DeployOptions.SERVER_ONLY);

        serverLongCheck.startServer();
        serverLongCheck.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", serverLongCheck.isStarted());

        File root = new File(serverLongCheck.getServerRoot());
        Log.info(getClass(), METHOD, "Server root: " + root.getAbsolutePath());

        // 1) wait for initial files
        awaitAllHealthFiles(root);

        File ready = HealthFileUtils.getReadyFile(root);
        File live  = HealthFileUtils.getLiveFile(root);

        // 2) STABILIZE: allow the first scheduler tick to land if it's imminent
        stabilize(ready, 6);
        stabilize(live,  6);

        long ready0 = HealthFileUtils.getLastModifiedTime(ready);
        long live0  = HealthFileUtils.getLastModifiedTime(live);

        // 3) short "no update" window to verify it isn't thrashing right away
        Assert.assertTrue(Constants.READY_SHOULD_NOT_HAVE_UPDATED,
                HealthFileUtils.waitForNoUpdateSince(ready, ready0, 8, 200));
        Assert.assertTrue(Constants.LIVE_SHOULD_NOT_HAVE_UPDATED,
                HealthFileUtils.waitForNoUpdateSince(live,  live0,  8, 200));

        // 4) now expect an update within the long interval horizon
        ready0 = HealthFileUtils.getLastModifiedTime(ready);
        live0  = HealthFileUtils.getLastModifiedTime(live);

        Assert.assertTrue("ready should update within ~30s",
                HealthFileUtils.waitForUpdateSince(ready, ready0, 30, 200));
        Assert.assertTrue("live should update within ~30s",
                HealthFileUtils.waitForUpdateSince(live,  live0,  30, 200));

        // sanity: last write times are fresh
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_UPDATED,
                HealthFileUtils.isLastModifiedTimeWithinLast(ready, Duration.ofSeconds(12)));
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_UPDATED,
                HealthFileUtils.isLastModifiedTimeWithinLast(live, Duration.ofSeconds(12)));
    }
}
