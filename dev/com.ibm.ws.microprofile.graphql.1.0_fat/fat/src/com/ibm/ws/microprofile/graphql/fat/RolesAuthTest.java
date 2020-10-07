/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.graphql.fat;

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

import mpGraphQL10.rolesAuth.RolesAuthTestServlet;

import org.jboss.shrinkwrap.api.spec.WebArchive;

@RunWith(FATRunner.class)
public class RolesAuthTest extends FATServletClient {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction("appSecurity-3.0", "appSecurity-2.0"));

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
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }
}
