/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
import io.openliberty.el60.fat.recordelresolver.servlets.EL60RecordELResolverTestServlet;

/**
 * Test the Expression Language 6.0 RecordELResolver.
 */
@RunWith(FATRunner.class)
public class EL60RecordELResolverTest {

    @Server("expressionLanguage60_recordELResolverServer")
    @TestServlet(servlet = EL60RecordELResolverTestServlet.class, contextRoot = "EL60RecordELResolverTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "EL60RecordELResolverTest.war", "io.openliberty.el60.fat.recordelresolver.servlets",
                                      "io.openliberty.el60.fat.recordelresolver.records");

        server.startServer(EL60RecordELResolverTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
