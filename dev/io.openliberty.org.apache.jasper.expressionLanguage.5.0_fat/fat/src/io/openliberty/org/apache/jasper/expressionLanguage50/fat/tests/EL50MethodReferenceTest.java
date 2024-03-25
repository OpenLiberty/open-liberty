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
import io.openliberty.el50.fat.method.reference.servlet.EL50MethodReferenceServlet;

/**
 * A set of tests to test the new Expression Language 5.0
 * jakarta.el.MethodReference.
 */
@RunWith(FATRunner.class)
public class EL50MethodReferenceTest extends FATServletClient {

    @Server("expressionLanguage50_methodReferenceServer")
    @TestServlet(servlet = EL50MethodReferenceServlet.class, contextRoot = "EL50MethodReferenceTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "EL50MethodReferenceTest.war", "io.openliberty.el50.fat.method.reference.servlet");

        server.startServer(EL50MethodReferenceTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
