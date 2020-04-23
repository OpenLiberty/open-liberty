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
package com.ibm.ws.microprofile.graphql.fat;

import org.jboss.shrinkwrap.api.asset.StringAsset;
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
import mpGraphQL10.basicQuery.BasicQueryTestServlet;

@RunWith(FATRunner.class)
public class BasicQueryWithConfigTest extends FATServletClient {

    private static final String SERVER = "mpGraphQL10.basicQueryWithConfig";
    private static final String APP_NAME = "basicQueryApp";

    @Server(SERVER)
    @TestServlet(servlet = BasicQueryTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp(APP_NAME, "mpGraphQL10.basicQuery")
                                     .addAsManifestResource(new StringAsset(
                                             "mp.graphql.contextpath=hello"),
                                         "microprofile-config.properties");
        ShrinkHelper.exportDropinAppToServer(server, war);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }
}
