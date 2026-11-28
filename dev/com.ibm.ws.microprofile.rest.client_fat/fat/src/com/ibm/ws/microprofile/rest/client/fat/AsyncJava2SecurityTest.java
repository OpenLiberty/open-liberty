/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient20.asyncJava2Security.AsyncJava2SecurityTestServlet;

/**
 * Test for GitHub issue #26810
 * This test does not use RepeatTests since it tests a specific Java 2 Security configuration.
 */
@RunWith(FATRunner.class)
public class AsyncJava2SecurityTest extends FATServletClient {

    private static final String APP_NAME = "asyncJava2SecurityApp";
    
    @Server("mpRestClient20.asyncJava2Security")
    @TestServlet(servlet = AsyncJava2SecurityTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] {DeployOptions.SERVER_ONLY},
                               "com.ibm.ws.microprofile.rest.client.fat.asyncjava2security",
                               "mpRestClient20.asyncJava2Security");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}