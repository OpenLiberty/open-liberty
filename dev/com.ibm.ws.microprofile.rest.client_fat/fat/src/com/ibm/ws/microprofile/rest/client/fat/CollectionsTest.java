/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import mpRestClient10.collections.CollectionsTestServlet;

@RunWith(FATRunner.class)
public class CollectionsTest extends FATServletClient {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
             .andWith(new FeatureReplacementAction("mpRestClient-1.0", "mpRestClient-1.1")
                 .withID("mpRestClient-1.1")
                 .forServers("mpRestClient10.collections"));

    private static final String appName = "collectionsApp";
    public static final String JOHNZON_IMPL = "publish/shared/resources/johnzon/";
    public static final String JSONB_API = "publish/files/jsonbapi/";

    /*
     * We need two servers to clearly distinguish that the "client" server
     * only has the client features enabled - it includes mpRestClient-1.0
     * which includes the jaxrsClient-2.0 feature, but not the jaxrs-2.0
     * feature that contains server code. The client should be able to
     * work on its own - by splitting out the "server" server into it's
     * own server, we can verify this.
     */
    @Server("mpRestClient10.collections")
    @TestServlet(servlet = CollectionsTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @Server("mpRestClient10.remoteServer")
    public static LibertyServer remoteAppServer;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp("basicRemoteApp", "remoteApp.basic");
        app.addAsLibraries(new File(JOHNZON_IMPL).listFiles());
        app.addAsLibraries(new File(JSONB_API).listFiles());
        ShrinkHelper.exportDropinAppToServer(remoteAppServer, app);
        remoteAppServer.startServer();

        ShrinkHelper.defaultDropinApp(server, appName, "mpRestClient10.collections");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
        if (remoteAppServer != null) {
            remoteAppServer.stopServer();
        }
    }
}