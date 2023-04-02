/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
package com.ibm.ws.fat.wc.tests;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Note this is a basic test to ensure newPushBuilder() returns null for non H2 requests.
 * A comprehensive test of the PushBuilder API is included in the Servlet 4.0 unit tests.
 * A comprehensive test of the PushBuilder functionality for H2 requests is included in the transport FAT bucket.
 */
@RunWith(FATRunner.class)
public class WCPushBuilderTest {

    private static final Logger LOG = Logger.getLogger(WCPushBuilderTest.class.getName());

    @Server("servlet40_wcServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestPushBuilderAPI to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, "TestPushBuilderAPI.war", "testpushbuilderapi.servlets");

        LOG.info("Setup : complete, ready for Tests");
        server.startServer(WCPushBuilderTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testPushBuilderAPI() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestPushBuilderAPI/PushBuilderAPIServlet",
                                       "PASS : req.newPushBuilder() returned null");
    }
}
