/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.jaxrs.ejbsubresource.EJBSubResourceClientTestServlet;

@RunWith(FATRunner.class)
public class EJBSubResourceTest extends FATServletClient {

    private static final String appName = "ejbsubresource";

    @Server("io.openliberty.jaxrs.fat.ejbsubresource")
    @TestServlet(servlet = EJBSubResourceClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Build an application and export it to the dropins directory
        ShrinkHelper.defaultDropinApp(server, appName, "io.openliberty.jaxrs.ejbsubresource");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("ejbsubresource.log", true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (server != null) {
            server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
        }
    }
}
