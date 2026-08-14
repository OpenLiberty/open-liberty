/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.fat.ejbinjection.servlet.EjbInjectionClientTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class EjbInjectionTest extends FATServletClient {

    private static final String appName = "EjbInjection";

    @Server("com.ibm.ws.jaxrs.fat.ejbinjection")
    @TestServlet(servlet = EjbInjectionClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Build an application and export it to the dropins directory
        ShrinkHelper.defaultDropinApp(server, appName, "com.ibm.ws.jaxrs.fat.ejbinjection",
                                                       "com.ibm.ws.jaxrs.fat.ejbinjection.ejbs",
                                                       "com.ibm.ws.jaxrs.fat.ejbinjection.interfaces",
                                                       "com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.annotated",
                                                       "com.ibm.ws.jaxrs.fat.ejbinjection.servlet",
                                                       "com.ibm.ws.jaxrs.fat.ejbinjection.singleton");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("EjbInjection.log", true);
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
