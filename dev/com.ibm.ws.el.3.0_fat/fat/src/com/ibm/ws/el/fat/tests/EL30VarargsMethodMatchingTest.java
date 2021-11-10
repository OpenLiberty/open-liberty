/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
import com.ibm.ws.el30.fat.varargstest.EL30VarargsMethodMatchingServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test EL 3.0 Method Matching when Varargs are used. See BZ 65358 
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EL30VarargsMethodMatchingTest extends FATServletClient {

    @Server("elServer")
    @TestServlet(servlet = EL30VarargsMethodMatchingServlet.class, contextRoot = "TestVarargsMatching")
    public static LibertyServer elServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(elServer, "TestVarargsMatching.war", "com.ibm.ws.el30.fat.varargstest");

        elServer.startServer(EL30VarargsMethodMatchingTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (elServer != null && elServer.isStarted()) {
            elServer.stopServer();
        }
    }
}
