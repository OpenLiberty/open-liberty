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
package com.ibm.ws.jaxrs20.fat.securitycontext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.fat.customsecuritycontext.servlet.CustomSecurityContextTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class CustomSecurityContextTest {


    @Server("com.ibm.ws.jaxrs.fat.customSecurityContext")
    @TestServlet(servlet = CustomSecurityContextTestServlet.class, contextRoot = "SecurityContext")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, "SecurityContext", "com.ibm.ws.jaxrs.fat.customsecuritycontext",
                                                                 "com.ibm.ws.jaxrs.fat.customsecuritycontext.servlet");
        ShrinkHelper.defaultApp(server, "CustomSecurityContext", "com.ibm.ws.jaxrs.fat.customsecuritycontext",
                                                                       "com.ibm.ws.jaxrs.fat.customsecuritycontext.filter");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer();
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
