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
import mpRestClient10.basic.BasicClientTestServlet;

@RunWith(FATRunner.class)
public class BasicTest extends FATServletClient {

    final static String SERVER_NAME = "mpRestClient10.basic";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
        .andWith(new FeatureReplacementAction()
                 .withID("mpRestClient-1.1")
                 .addFeature("mpRestClient-1.1")
                 .removeFeature("mpRestClient-1.0")
                 .removeFeature("mpRestClient-1.2")
                 .forServers(SERVER_NAME))
        .andWith(new FeatureReplacementAction()
                 .withID("mpRestClient-1.2")
                 .addFeature("mpRestClient-1.2")
                 .removeFeature("mpRestClient-1.0")
                 .removeFeature("mpRestClient-1.1")
                 .forServers(SERVER_NAME));

    private static final String appName = "basicClientApp";

    /*
     * We need two servers to clearly distinguish that the "client" server
     * only has the client features enabled - it includes mpRestClient-1.0
     * which includes the jaxrsClient-2.0 feature, but not the jaxrs-2.0
     * feature that contains server code. The client should be able to
     * work on its own - by splitting out the "server" server into it's
     * own server, we can verify this.
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

        ShrinkHelper.defaultDropinApp(server, appName, "mpRestClient10.basic");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
        remoteAppServer.stopServer("CWWKO0801E");
    }
}