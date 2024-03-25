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
import componenttest.topology.utils.FATServletClient;
import io.openliberty.el60.fat.modulevisibility.servlets.EL60ModuleVisibilityTestServlet;

/**
 * Test the Expression Language 6.0 module visibility.
 *
 * Expression Language 6.0 issue: https://github.com/jakartaee/expression-language/issues/188
 */
@RunWith(FATRunner.class)
public class EL60ModuleVisibilityTest extends FATServletClient {

    @Server("expressionLanguage60_moduleVisibilityServer")
    @TestServlet(servlet = EL60ModuleVisibilityTestServlet.class, contextRoot = "EL60ModuleVisibilityTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "EL60ModuleVisibilityTest.war", "io.openliberty.el60.fat.modulevisibility.servlets");

        server.startServer(EL60ModuleVisibilityTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
