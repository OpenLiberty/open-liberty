/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.jsonb.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.jsonptest.JSONPTestServlet;

/**
 * This test for JSON-P is placed in the JSON-B bucket because it is convenient to access the Johnzon library here.
 * Consider if we should move to the JSON-P bucket once that is written.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JSONPContainerTest extends FATServletClient {

    private static final String appName = "jsonpapp";

    @Server("com.ibm.ws.jsonp.container.fat")
    @TestServlet(servlet = JSONPTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(JSONPContainerTest.class, "setUp", "=====> Start JSONPContainerTest");

        FATSuite.configureImpls(server);
        ShrinkHelper.defaultApp(server, appName, "web.jsonptest");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();

        Log.info(JSONPContainerTest.class, "tearDown", "<===== Stop JSONPContainerTest");
    }

    @Test
    public void testJsonpProviderAvailable() throws Exception {
        runTest(server, appName + "/JSONPTestServlet", getTestMethodSimpleName() + "&JsonpProvider=" + FATSuite.getJsonpProviderClassName());
    }
}
