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
import io.openliberty.el60.fat.simpletest.servlets.SimpleTestServlet;

/**
 * This class doesn't actually test new Expression Language 6.0 function but rather is just
 * a single test to ensure that we run at least one test for this FAT bucket.
 *
 * This test class can be removed once we add more Expression Language 6.0 tests.
 */
@RunWith(FATRunner.class)
public class SimpleTest extends FATServletClient {

    @Server("expressionLanguage60_simpleTestServer")
    @TestServlet(servlet = SimpleTestServlet.class, contextRoot = "EL60SimpleTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "EL60SimpleTest.war", "io.openliberty.el60.fat.simpletest.servlets");

        server.startServer(SimpleTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
