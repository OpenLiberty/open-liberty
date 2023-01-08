/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.server.config;

import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test of ID assignment, including a restart which changes the ID
 * assignments.
 *
 * Start with no ID assignments, then replace the configuration with
 * assigned IDs and restart.
 *
 * IDs are verified before the server is started, while the server is running,
 * then after the server stops.
 *
 * IDs become visible when applications are processed, at which point in time
 * a container is created for the application, which includes the creation of
 * cache folders for the application container, in the server workarea.
 *
 * Each application is assigned a cache ID which is used as the cache folder for
 * the application.
 *
 * When an ID is assigned in the application configuration, that ID is used as
 * the cache ID.
 *
 * When no ID is assigned in the application configuration, a default ID is
 * assigned. Default IDs are assigned in numerical order, starting with '0'.
 *
 * If applications are removed, their cache folders are removed from the workarea.
 * This happens during server shutdown: Any cache folder which does not match a
 * configured application is removed.
 *
 * In the restart test, below, the configuration is updated between server starts.
 * The original configuration has no assigned IDs. The updated configuration has
 * assigned IDs.
 *
 * The result is that following the restart, cache folders are present for both the
 * original default IDs, which were assigned to the applications during the first
 * start, and new assigned IDs, as read from the configuration.
 *
 * The original default IDs are removed during the second server stop.
 */
@RunWith(FATRunner.class)
public class CacheIdRestartTest extends CacheIdTest {
    private static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        CacheIdTest.setupApps();

        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.cacheid.restart");

        ShrinkHelper.exportAppToServer(server, cacheId0App, DeployOptions.DISABLE_VALIDATION);
        ShrinkHelper.exportAppToServer(server, cacheId1App, DeployOptions.DISABLE_VALIDATION);
    }

    /**
     * Test cache ID assignment.
     *
     * The first startup assigns default IDs.
     *
     * The second startup has IDs from the first startup, resulting in
     * assigned IDs. The default IDs are removed when the server stops.
     */
    @Test
    public void testCacheIds_Restart() throws Exception {
        testIds("testCacheIds",
                noidsCopyActor,
                "noids",
                CLEAN_START, PRECLEAN_START,
                unsetIds, defaultIds, defaultIds);

        testIds("testCacheId",
                idsCopyActor,
                "ids",
                !CLEAN_START, !PRECLEAN_START,
                defaultIds, allIds, assignedIds);
    }

    protected void testIds(String testName,
                           ServerRunnable initialActor,
                           String logBase,
                           boolean cleanStart, boolean precleanStart,
                           Set<String> expectedIds_prestart,
                           Set<String> expectedIds_running,
                           Set<String> expectedIds_poststop) throws Exception {

        // The first start has IDS as determine from prior startups.
        testIds(testName, logBase + "-initial",
                initialActor,
                testName + '_' + logBase + "_initial.log",
                cleanStart, precleanStart,
                expectedIds_prestart, expectedIds_running, expectedIds_poststop);

        // Restart with no change to the configuration makes no changes
        // to the IDs.
        testIds(testName, logBase + "-noop",
                noopActor,
                testName + '_' + logBase + "_noop.log",
                !CLEAN_START, !PRECLEAN_START,
                expectedIds_poststop, expectedIds_poststop, expectedIds_poststop);

        // Restart after touching the configuration makes no changes
        // to the IDs.
        testIds(testName, logBase + "-touch",
                touchActor,
                testName + '_' + logBase + "_touch.log",
                !CLEAN_START, !PRECLEAN_START,
                expectedIds_poststop, expectedIds_poststop, expectedIds_poststop);
    }

    protected void testIds(String testName, String testPart,
                           ServerRunnable configActor,
                           String logName,
                           boolean cleanStart, boolean precleanStart,
                           Set<String> expectedIds_prestart,
                           Set<String> expectedIds_running,
                           Set<String> expectedIds_poststop) throws Exception {

        verifyCacheIds(server, testName, testPart + " initial", expectedIds_prestart);

        configActor.run(server);
        server.startServer(logName, cleanStart, precleanStart);

        try {
            waitForFeatureUpdate(server);
            waitForApp0(server);
            waitForApp1(server);
            verifyApp0(server);
            verifyApp1(server);

            verifyCacheIds(server, testName, testPart + " running", expectedIds_running);

        } finally {
            server.stopServer();
        }

        verifyCacheIds(server, testName, testPart + " final", expectedIds_poststop);
    }
}
