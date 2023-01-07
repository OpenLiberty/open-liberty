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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test simple re-arrangement of the applications.
 *
 * The test uses the no-IDs configuration.
 *
 * The first configured application is removed from the server
 * configuration then is added back as the second configured application
 */
@RunWith(FATRunner.class)
public class CacheIdRearrangeTest extends CacheIdTest {
    private static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        CacheIdTest.setupApps();

        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.cacheid.rearrange");

        ShrinkHelper.exportAppToServer(server, cacheId0App, DeployOptions.DISABLE_VALIDATION);
        ShrinkHelper.exportAppToServer(server, cacheId1App, DeployOptions.DISABLE_VALIDATION);
    }

    @Test
    public void testCacheIds_Rearrange() throws Exception {
        String testName = "testCacheIds_Rearrange";
        String logName = "testcacheids_rearrange";

        String configPath = getConfigPath(server);

        verifyCacheIds(server, testName, "prestart-0", CacheIdTest.unsetIds);

        noidsCopyActor.run(server);
        server.startServer(logName, CLEAN_START, !PRECLEAN_START);

        try {
            waitForFeatureUpdate(server);
            waitForApp0(server);
            waitForApp1(server);
            verifyApp0(server);
            verifyApp1(server);
            verifyCacheIds(server, testName, "start-0", defaultIds);

            // Remove app 'cacheid0'

            server.setMarkToEndOfLog();
            FileRewriter.update(configPath, WAR0_DEFAULT_ID, CacheIdTest.REMOVE_MATCH0);
            Thread.sleep(200);
            // TODO: How to tell when the removal happened?
            verifyApp1(server);
            // TODO: How to verify that an app is not available?
            verifyCacheIds(server, testName, "update-0", defaultIds);

            // Add app 'cacheid0' back, but after app 'cacheid1'.
            // Curiously, the application is not assigned a new ID.
            // Conceivably, the application could be assigned the '2' ID.

            server.setMarkToEndOfLog();
            FileRewriter.update(configPath, WAR1_DEFAULT_ID, WAR0_DEFAULT_ID, ADD_AFTER_MATCH0);
            Thread.sleep(200);
            waitForApp0(server);
            verifyApp0(server);
            verifyApp1(server);
            verifyCacheIds(server, testName, "update-1", defaultIds);

        } finally {
            server.stopServer();
        }
        verifyCacheIds(server, testName, "stop-0", defaultIds);

        server.startServer(logName, !CLEAN_START, !PRECLEAN_START);
        try {
            waitForFeatureUpdate(server);
            waitForApp0(server);
            waitForApp1(server);
            verifyApp0(server);
            verifyApp1(server);
            verifyCacheIds(server, testName, "start-1", defaultIds);
        } finally {
            server.stopServer();
        }
        verifyCacheIds(server, testName, "stop-1", defaultIds);
    }
}
//@formatter:on