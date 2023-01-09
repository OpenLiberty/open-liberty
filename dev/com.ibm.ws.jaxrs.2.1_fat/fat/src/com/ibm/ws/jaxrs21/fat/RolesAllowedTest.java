/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.fat.rolesallowed.servlet.RolesAllowedTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class RolesAllowedTest {

    private final static String CONTEXT_ROOT = "RolesAllowed";

    @Server("com.ibm.ws.jaxrs.fat.rolesAllowed")
    @TestServlet(servlet = RolesAllowedTestServlet.class, contextRoot = CONTEXT_ROOT)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, CONTEXT_ROOT, "com.ibm.ws.jaxrs.fat.rolesallowed",
                                                      "com.ibm.ws.jaxrs.fat.rolesallowed.servlet");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer();
            assertNotNull("FeatureManager did not report update was complete", server.waitForStringInLog("CWWKF0008I"));
            assertNotNull("LTPA configuration should report it is ready", server.waitForStringInLog("CWWKS4105I"));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }
}
