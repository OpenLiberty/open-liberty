/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.jasper.expressionLanguage60.fat.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.el60.fat.arraylengthtest.servlets.EL60ArrayLengthTestServlet;

/**
 * Test the Expression Language 6.0 Array length property.
 */
@RunWith(FATRunner.class)
public class EL60ArrayLengthTest extends FATServletClient {

    @Server("expressionLanguage60_arrayLengthTestServer")
    @TestServlet(servlet = EL60ArrayLengthTestServlet.class, contextRoot = "EL60ArrayLengthTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "EL60ArrayLengthTest.war", "io.openliberty.el60.fat.arraylengthtest.servlets");

        server.startServer(EL60ArrayLengthTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
