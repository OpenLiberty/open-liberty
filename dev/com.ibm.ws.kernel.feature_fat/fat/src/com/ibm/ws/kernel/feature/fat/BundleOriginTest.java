/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class BundleOriginTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.origin.bundle");
    private static final String TEST_MARKER = "BundleInstallOriginTest:";

    @Test
    public void testBundleOrigin() throws Exception {

        server.startServer();
        String result = server.waitForStringInLogUsingMark(TEST_MARKER);
        assertTrue("Test failed: " + result, result.contains("PASSED"));

        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("usr:test.origin.user-1.0"));
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        result = server.waitForStringInLogUsingMark(TEST_MARKER);
        assertTrue("Test failed: " + result, result.contains("PASSED"));
    }

    @BeforeClass
    public static void installFeatures() throws Exception {
        server.installSystemFeature("test.origin.system-1.0");
        server.installSystemBundle("test.origin.bundle.system");

        server.installUserFeature("test.origin.user-1.0");
        server.installUserBundle("test.origin.bundle.user");
    }

    @AfterClass
    public static void uninstallFeatures() throws Exception {
        server.uninstallSystemFeature("test.origin.system-1.0");
        server.uninstallSystemBundle("test.origin.bundle.system");

        server.uninstallUserFeature("test.origin.user-1.0");
        server.uninstallUserBundle("test.origin.bundle.user");
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}