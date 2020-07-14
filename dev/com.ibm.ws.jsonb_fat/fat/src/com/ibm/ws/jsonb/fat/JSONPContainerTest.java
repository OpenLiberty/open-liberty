/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsonb.fat;

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
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
        ShrinkHelper.defaultApp(server, appName, "web.jsonptest");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    @SkipForRepeat(EE9_FEATURES)
    //TODO: Transform the johnzon jars in AUTO_FVT/publish/shared/resources folder, solve the classloader problems with yasson and jonhzon provider impls
    public void testJsonpProviderAvailableJohnzon() throws Exception {
        runTest(server, appName + "/JSONPTestServlet", "testJsonpProviderAvailable&JsonpProvider=" + FATSuite.PROVIDER_JOHNZON_JSONP);
    }
}
