/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.cdi.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import concurrent.cdi.web.ConcurrentCDIServlet;

@RunWith(FATRunner.class)
public class ConcurrentCDITest extends FATServletClient {

    public static final String APP_NAME = "concurrentCDIApp";

    @Server("concurrent_fat_cdi")
    @TestServlet(servlet = ConcurrentCDIServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "concurrent.cdi.web");
        server.startServer();
        runTest(server, APP_NAME + '/' + ConcurrentCDIServlet.class.getSimpleName(), "initTransactionService");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          "CWWKE1205E" // test case intentionally causes startTimeout to be exceeded
        );
    }
}
