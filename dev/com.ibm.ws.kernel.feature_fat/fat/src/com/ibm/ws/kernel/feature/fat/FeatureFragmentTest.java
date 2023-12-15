/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.fat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class FeatureFragmentTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fragment");
    @Rule
    public TestName testName = new TestName();
    public Class<FeatureFragmentTest> c = FeatureFragmentTest.class;

    @Test
    public void testFeatureFragment() throws Exception {
        server.startServer();

        Log.info(c, testName.getMethodName(), "Adding test.feature.fragment-1.0 feature.");
        server.waitForStringInLog("TEST - HostComponent activated");
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("test.feature.host-1.0", "test.feature.fragment-1.0"));
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        server.waitForStringInLog("TEST - HostComponent deactivated");
        server.waitForStringInLog("TEST - HostComponent activated");
        server.waitForStringInLog("TEST - FragmentComponent activated");
    }

    @BeforeClass
    public static void installSystemFeature() throws Exception {
        server.installSystemFeature("test.feature.host-1.0");
        server.installSystemFeature("test.feature.fragment-1.0");
        server.installSystemBundle("test.feature.host.bundle");
        server.installSystemBundle("test.feature.fragment.bundle");
    }

    @AfterClass
    public static void uninstallSystemFeature() throws Exception {
        server.uninstallSystemFeature("test.feature.host-1.0");
        server.uninstallSystemFeature("test.feature.fragment-1.0");
        server.uninstallSystemBundle("test.feature.host.bundle");
        server.uninstallSystemBundle("test.feature.fragment.bundle");

    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}