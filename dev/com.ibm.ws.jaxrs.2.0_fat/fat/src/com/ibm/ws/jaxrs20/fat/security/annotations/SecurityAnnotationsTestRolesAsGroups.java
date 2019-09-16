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
package com.ibm.ws.jaxrs20.fat.security.annotations;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.fat.security.servlet.SecurityAnnotationsRolesAsGroupsTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class SecurityAnnotationsTestRolesAsGroups {

    private static final String secwar = "security";
    private static final String secNoWebXmlWar = "securityNoWebXml";

    @Server("com.ibm.ws.jaxrs.fat.securityAnnotations_rolesAsGroups")
    public static LibertyServer server;

    @Server("com.ibm.ws.jaxrs.fat.security.client")
    @TestServlet(servlet = SecurityAnnotationsRolesAsGroupsTestServlet.class, contextRoot = secwar + "TestServlet")
    public static LibertyServer clientServer;

    @BeforeClass
    public static void setupClass() throws Exception {
        ShrinkHelper.defaultApp(server, secwar, "com.ibm.ws.jaxrs21.fat.security.annotations",
                                "com.ibm.ws.jaxrs21.fat.security.ssl",
                                "com.ibm.ws.jaxrs21.fat.securitycontext",
                                "com.ibm.ws.jaxrs21.fat.securitycontext.xml");
        ShrinkHelper.defaultApp(server, secNoWebXmlWar, "com.ibm.ws.jaxrs21.fat.security.annotations",
                                "com.ibm.ws.jaxrs21.fat.security.ssl",
                                "com.ibm.ws.jaxrs21.fat.securitycontext",
                                "com.ibm.ws.jaxrs21.fat.securitycontext.xml");
        ShrinkHelper.defaultDropinApp(clientServer, secwar + "TestServlet", "com.ibm.ws.jaxrs21.fat.security.servlet",
                                      "com.ibm.ws.jaxrs21.fat.securitycontext.xml");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            clientServer.startServer();
            server.useSecondaryHTTPPort();
            server.startServer();
            assertNotNull("The server did not start", server.waitForStringInLog("CWWKF0011I"));
            assertNotNull("The Security Service should be ready", server.waitForStringInLog("CWWKS0008I"));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
        if (clientServer != null) {
            clientServer.stopServer();
        }
    }

}
