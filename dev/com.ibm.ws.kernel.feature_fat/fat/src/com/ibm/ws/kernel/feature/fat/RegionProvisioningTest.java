/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.fat;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class RegionProvisioningTest {
    private static final Class<?> c = RegionProvisioningTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.region");
    @Rule
    public TestName name = new TestName();
    public String testName = "";

    @Before
    public void beforeTest() throws Exception {
        // set the current test name
        testName = name.getMethodName();
        Log.info(c, testName, "===== Starting test " + testName + " =====");
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testKernelBundlesRegion() throws Exception {
        server.startServer(testName + ".log");
        server.stopServer();
        RemoteFile libDir = server.getFileFromLibertyInstallRoot("/lib");
        for (RemoteFile f : libDir.list(false)) {
            if (f.getName().startsWith("com.ibm.ws.kernel.feature_")) {
                Log.info(c, testName, "==== Updating last modified for: " + f.getAbsolutePath());
                new File(f.getAbsolutePath()).setLastModified(System.currentTimeMillis());
            }
        }

        ServerConfiguration sc = server.getServerConfiguration();
        sc.getFeatureManager().getFeatures().add("servlet-3.1");
        server.updateServerConfiguration(sc);
        server.startServer(testName + ".log", false);
        String error = server.waitForStringInLog("CWWKF0004E");
        Assert.assertNull("Error occurred", error);
        server.stopServer();
        Log.exiting(c, testName);
    }

}
