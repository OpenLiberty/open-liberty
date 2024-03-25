/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.jasper.expressionLanguage50.fat.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.el50.fat.defaultmethods.servlet.EL50DefaultMethodsServlet;

/**
 * Tests to ensure default methods are resolved by BeanELResolver in expressionLanguage-5.0.
 * https://github.com/jakartaee/expression-language/issues/43
 */
@RunWith(FATRunner.class)
public class EL50DefaultMethodsTest extends FATServletClient {

    @Server("expressionLanguage50_defaultMethodsServer")
    @TestServlet(servlet = EL50DefaultMethodsServlet.class, contextRoot = "EL50DefaultMethodBeanELResolverTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "EL50DefaultMethodBeanELResolverTest.war", "io.openliberty.el50.fat.defaultmethods.servlet");

        server.startServer(EL50DefaultMethodsTest.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
