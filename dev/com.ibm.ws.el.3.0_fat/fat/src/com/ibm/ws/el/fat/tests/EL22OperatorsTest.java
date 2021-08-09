/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el.fat.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.el30.fat.servlets.EL22OperatorsServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * A test case to verify that the EL 2.2 operators continue to function correcttly via the EL 3.0 implementation.
 * The tests are defined in the servlet EL22OperatorsServlet. Here we just check to make sure nothing failed.
 */
@RunWith(FATRunner.class)
public class EL22OperatorsTest extends FATServletClient {

    @Server("elServer")
    @TestServlet(servlet = EL22OperatorsServlet.class, contextRoot = "TestEL3.0")
    public static LibertyServer elServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(elServer, "TestEL3.0.war", "com.ibm.ws.el30.fat.beans", "com.ibm.ws.el30.fat.servlets");

        elServer.startServer(EL22OperatorsTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (elServer != null && elServer.isStarted()) {
            elServer.stopServer();
        }
    }
}
