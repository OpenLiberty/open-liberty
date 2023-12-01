/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.threading.policy;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@MinimumJavaLevel(javaLevel = 21)
@RunWith(FATRunner.class)
public class PolicyExecutorVirtualThreadTest extends FATServletClient {

    @Server("com.ibm.ws.threading.policy.fat.vt")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.copyFileToLibertyInstallRoot("lib/features/", "features/policyExecutorUser-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib/", "bundles/test.policyexecutor.bundle_fat.jar");

        WebArchive app = ShrinkWrap.create(WebArchive.class, "vtpolicyapp.war")//
                        .addPackages(true, "web.vt");
        ShrinkHelper.exportAppToServer(server, app);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
        server.deleteFileFromLibertyInstallRoot("lib/features/policyExecutorUser-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("lib/test.policyexecutor.bundle_fat.jar");
    }

    @Test
    public void testMaxConcurrencyWithVirtualThreads() throws Exception {
        runTest(server, "vtpolicyapp/PolicyVirtualThreadServlet", "testMaxConcurrencyWithVirtualThreads");
    }

    @Test
    public void testMaxPolicyStrictWithVirtualThreads() throws Exception {
        runTest(server, "vtpolicyapp/PolicyVirtualThreadServlet", "testMaxPolicyStrictWithVirtualThreads");
    }
}
