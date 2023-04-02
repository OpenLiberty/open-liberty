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
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This test bucket tests the server clean up function during the startup process.
 */
@RunWith(FATRunner.class)
public class ServerCleanTest {
    private static final Class<?> c = ServerCleanTest.class;

    private static final String SERVER_CLEAN = "com.ibm.ws.kernel.boot.serverclean.fat";
    private static final String SERVER_CLEAN_BOOTSTRAP = "com.ibm.ws.kernel.boot.serverclean.bootstrap.fat";

    private static LibertyServer server;

    @After
    public void after() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * This test validates that the server cleans the /workarea folder correctly when the --clean
     * parameter is present at startup or when org.osgi.framework.storage.clean=onFirstInit is
     * present in the bootstrap.properties file.
     *
     * @throws Exception
     */
    @Test
    public void testServerStartWithClean() throws Exception {
        final String METHOD_NAME = "testServerStartWithClean";
        Log.entering(c, METHOD_NAME);

        server = LibertyServerFactory.getLibertyServer(SERVER_CLEAN);

        // Write files to /workarea folder
        server.copyFileToLibertyServerRoot("workarea", "marker");
        boolean markerExists = server.fileExistsInLibertyServerRoot("workarea/marker");
        assertTrue("The workarea marker file did not exist when it should have.", markerExists);

        server.copyFileToLibertyServerRoot("workarea/platform/", "marker");
        markerExists = server.fileExistsInLibertyServerRoot("workarea/platform/marker");
        assertTrue("The workarea marker file did not exist when it should have.", markerExists);

        // Start server with the --clean option
        server.startServer(true); // uses --clean
        server.waitForStringInLog("CWWKF0011I");
        assertTrue("the server should have been started", server.isStarted());

        // Ensure marker file in /workarea folder is removed
        markerExists = server.fileExistsInLibertyServerRoot("workarea/marker");
        assertFalse("The workarea marker file existed when it should not have.", markerExists);

        markerExists = server.fileExistsInLibertyServerRoot("workarea/platform/marker");
        assertFalse("The workarea marker file existed when it should not have.", markerExists);

        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test validates that the server start script functions correctly when the osgi clean parameter
     * is specified in the bootstrap.properties file.
     *
     * @throws Exception
     */
    @Test
    public void testServerStartWithCleanBootstrapProps() throws Exception {
        final String METHOD_NAME = "testServerStartWithCleanBootstrapProps";
        Log.entering(c, METHOD_NAME);

        server = LibertyServerFactory.getLibertyServer(SERVER_CLEAN_BOOTSTRAP);

        // Write files to /workarea folder
        server.copyFileToLibertyServerRoot("workarea", "marker");
        boolean markerExists = server.fileExistsInLibertyServerRoot("workarea/marker");
        assertTrue("The workarea marker file did not exist when it should have.", markerExists);

        server.copyFileToLibertyServerRoot("workarea/platform/", "marker");
        markerExists = server.fileExistsInLibertyServerRoot("workarea/platform/marker");
        assertTrue("The workarea marker file did not exist when it should have.", markerExists);

        // Start server with org.osgi.framework.storage.clean=onFirstInit in bootstrap.properties
        server.startServer(false);
        server.waitForStringInLog("CWWKF0011I");
        assertTrue("the server should have been started", server.isStarted());

        // Ensure marker file in /workarea folder is removed
        markerExists = server.fileExistsInLibertyServerRoot("workarea/marker");
        assertFalse("The workarea marker file existed when it should not have.", markerExists);

        markerExists = server.fileExistsInLibertyServerRoot("workarea/platform/marker");
        assertFalse("The workarea marker file existed when it should not have.", markerExists);

        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }
}
