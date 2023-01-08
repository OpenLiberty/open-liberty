/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package com.ibm.ws.microprofile.graphql.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

import mpGraphQL10.rolesAuth.RolesAuthTestServlet;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.spec.WebArchive;

@RunWith(FATRunner.class)
public class RolesAuthTest extends FATServletClient {

    private static final String SERVER = "mpGraphQL10.rolesAuth";
    private static final String APP_NAME = "rolesAuthApp";

    @Server(SERVER)
    @TestServlet(servlet = RolesAuthTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive webArchive = ShrinkHelper.buildDefaultApp(APP_NAME, "mpGraphQL10.rolesAuth");
        ShrinkHelper.exportAppToServer(server, webArchive);
        server.startServer();

        // wait for LTPA key to be available to avoid CWWKS4000E
        assertNotNull("CWWKS4105I.* not received on server",
                      server.waitForStringInLog("CWWKS4105I.*"));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }
}
