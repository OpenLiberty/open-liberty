/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package com.ibm.ws.fat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.interop.InteropStartClientTestServlet;

/*
 * The purpose of this test is to check interoperability between JAX-RS and JAX-WS
 * InteropStartClientTestServlet calls -> JAX-RS jaxwsEP1 calls -> JAX-WS EP (PeopleService) calls
 * -> JAX-RS jaxwsEP2 return a text value back trough stack of web service calls
 *
 */
@RunWith(FATRunner.class)
public class WebServicesInteroperabilityTest extends FATServletClient {

    private static final String appName = "interop";

    @Server("WebServicesInteroperabilityServer")
    @TestServlet(servlet = InteropStartClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Build an application and export it to the dropins directory
        ShrinkHelper.defaultDropinApp(server, appName, "io.openliberty.interop", "io.openliberty.interop.util",
                                      "com.ibm.ws.jaxws.test.wsr.server.stub",
                                      "com.ibm.ws.jaxws.fat.util");

        ShrinkHelper.defaultDropinApp(server, "helloServer", "io.openliberty.interop.util",
                                      "com.ibm.ws.jaxws.test.wsr.server",
                                      "com.ibm.ws.jaxws.test.wsr.server.impl",
                                      "com.ibm.ws.jaxws.fat.util");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("WebServicesInteroperability.log", true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        // Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*helloServer");
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (server != null) {
            server.stopServer("CWWKE1102W"); //ignore server quiesce timeouts due to slow test machines
        }
    }

    @Before
    public void beforeTest() {
    }

    @After
    public void afterTest() {
    }
}
