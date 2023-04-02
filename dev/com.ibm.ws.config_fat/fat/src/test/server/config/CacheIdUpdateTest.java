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

import java.util.Collections;
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
 * Test configuration updates which change the ID assignment for
 * configured applications.
 *
 * Update the configuration to add explicit IDs for the two
 * configured applications.
 */
@RunWith(FATRunner.class)
public class CacheIdUpdateTest extends CacheIdTest {

    private static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        CacheIdTest.setupApps();

        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.cacheid.update");

        ShrinkHelper.exportAppToServer(server, cacheId0App, DeployOptions.DISABLE_VALIDATION);
        ShrinkHelper.exportAppToServer(server, cacheId1App, DeployOptions.DISABLE_VALIDATION);
    }

    private void waitForApp(String appName) throws Exception {
        Set<String> appNames = Collections.singleton(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);
    }

    @Test
    public void testCacheIds_Update() throws Exception {
        String testName = "testCacheIds_Update";
        String logName = "testcacheids_update";

        String configPath = getConfigPath(server);

        Set<String> ids_update0 = asSet(id_default0, id_default1, id_assigned0);
        Set<String> ids_update1 = asSet(id_default0, id_default1, id_assigned0, id_assigned1);

        verifyCacheIds(server, testName, "prestart-0", CacheIdTest.unsetIds);

        // Start with neither application configuration having an ID.

        noidsCopyActor.run(server);
        server.startServer(logName, CLEAN_START, !PRECLEAN_START);

        try {
            waitForFeatureUpdate(server);
            waitForApp0(server);
            waitForApp1(server);
            verifyApp0(server);
            verifyApp1(server);
            verifyCacheIds(server, testName, "start-0", defaultIds);

            // Set an ID to WAR0; this might change the cache ID of the
            // server, but it doesn't.

            server.setMarkToEndOfLog();
            FileRewriter.update(configPath, WAR0_DEFAULT_ID, WAR0_ASSIGNED_ID);
            Thread.sleep(200);
            // waitForApp(APP_NAME0); // No reaction from the server!

            verifyApp0(server);
            verifyApp1(server);
            // verifyCacheIds(server, testName, "update-0", ids_update0); // Not updated!
            verifyCacheIds(server, testName, "update-0", defaultIds);

            // Set an ID to WAR1; this might change the cache ID of the
            // server, but it doesn't.

            server.setMarkToEndOfLog();
            FileRewriter.update(configPath, WAR1_DEFAULT_ID, WAR1_ASSIGNED_ID);
            Thread.sleep(200);
            // waitForApp(APP_NAME1); // No reaction from the server!

            verifyApp0(server);
            verifyApp1(server);
            // verifyCacheIds(server, testName, "update-1", ids_update1); // Still not updated!
            verifyCacheIds(server, testName, "update-1", defaultIds);
        } finally {
            server.stopServer();
        }
        verifyCacheIds(server, testName, "stop-0", defaultIds);

        // On a restart, the IDs are changed to the assigned IDs.
        // After starting, both sets of IDs are present.
        // After stopping, just the assigned IDs are present.

        server.startServer(logName, !CLEAN_START, !PRECLEAN_START);
        try {
            waitForFeatureUpdate(server);
            waitForApp0(server);
            waitForApp1(server);
            verifyApp0(server);
            verifyApp1(server);
            verifyCacheIds(server, testName, "start-1", allIds);
        } finally {
            server.stopServer();
        }
        verifyCacheIds(server, testName, "stop-1", assignedIds);
    }


}
