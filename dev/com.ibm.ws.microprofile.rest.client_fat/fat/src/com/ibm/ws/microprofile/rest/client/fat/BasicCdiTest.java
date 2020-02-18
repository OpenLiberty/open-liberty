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
package com.ibm.ws.microprofile.rest.client.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient10.basicCdi.BasicClientTestServlet;

@RunWith(FATRunner.class)
public class BasicCdiTest extends FATServletClient {

    final static String SERVER_NAME = "mpRestClient10.basic.cdi";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
        .andWith(FATSuite.MP_REST_CLIENT_WITH_CONFIG("1.1", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT_WITH_CONFIG(FeatureReplacementAction.EE8_FEATURES(), "1.2", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT_WITH_CONFIG(FeatureReplacementAction.EE8_FEATURES(), "1.3", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT_WITH_CONFIG(FeatureReplacementAction.EE8_FEATURES(), "1.4", SERVER_NAME));

    private static final String appName = "basicCdiClientApp";

    /*
     * We need two servers to clearly distinguish that the "client" server
     * only has the client features enabled - it includes mpRestClient-1.0
     * (which includes the jaxrsClient-2.0 feature) and mpConfig-1.1 (which
     * includes cdi-1.2), but not the jaxrs-2.0 feature that contains server
     * code. The client should be able to work on its own - by splitting out
     * the "server" server into it's own server, we can verify this.
     */
    @Server(SERVER_NAME)
    @TestServlet(servlet = BasicClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @Server("mpRestClient10.remoteServer")
    public static LibertyServer remoteAppServer;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(remoteAppServer, "basicRemoteApp", "remoteApp.basic");
        remoteAppServer.startServer();

        ShrinkHelper.defaultDropinApp(server, appName, "mpRestClient10.basicCdi");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
        remoteAppServer.stopServer();
    }
}