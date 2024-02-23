/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxws.namespacecheck.servlet.PartInfoNamespaceCorrectionTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * For test description and details see:
 *
 * @see com.ibm.ws.jaxws.namespacecheck.servlet.PartInfoNamespaceCorrectionTestServlet
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class PartInfoNamespaceCorrectionTest extends FATServletClient {

    private static final String APP_NAME = "namespaceCheck";

    @Server("NamespaceCheckServer")
    @TestServlet(servlet = PartInfoNamespaceCorrectionTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.jaxws.namespacecheck.servlet",
                                                      "com.ibm.ws.jaxws.namespacecheck.wsdl.importedserviceschema",
                                                      "com.ibm.ws.jaxws.namespacecheck.wsdl.mainservice",
                                                      "com.ibm.ws.jaxws.namespacecheck.wsdl.mainserviceschema");

        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer("NamespaceCheckServer.log");
        System.out.println("Starting Server");

        assertNotNull("Application " + APP_NAME + " does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
    }

}
