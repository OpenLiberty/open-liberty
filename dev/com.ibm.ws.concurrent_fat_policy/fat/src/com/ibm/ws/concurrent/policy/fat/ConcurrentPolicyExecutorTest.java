/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package com.ibm.ws.concurrent.policy.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.ConcurrentPolicyFATServlet;

/**
 * Tests for managed executors using policyExecutor.
 */
@RunWith(FATRunner.class)
public class ConcurrentPolicyExecutorTest extends FATServletClient {

    public static final String APP_NAME = "concurrentpolicyfat";

    @Server("com.ibm.ws.concurrent.fat.policy")
    @TestServlet(servlet = ConcurrentPolicyFATServlet.class, path = APP_NAME + "/ConcurrentPolicyFATServlet")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage("web");
        ShrinkHelper.exportDropinAppToServer(server, app);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE1205E:"); // several tests intentionally exceed the startTimeout
    }
}