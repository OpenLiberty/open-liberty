/*******************************************************************************
 * Copyright (c) 2017,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.cdi.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import concurrent.cdi4.web.ConcurrentCDI4Servlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class ConcurrentCDI4Test extends FATServletClient {

    public static final String APP_NAME = "concurrentCDI4App";

    @Server("concurrent_fat_cdi4")
    @TestServlet(servlet = ConcurrentCDI4Servlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME,
                                      "concurrent.cdi4.web",
                                      "concurrent.cu3.web");
        server.startServer();
        runTest(server, APP_NAME + '/' + ConcurrentCDI4Servlet.class.getSimpleName(), "initTransactionService");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          "CWWKE1205E" // test case intentionally causes startTimeout to be exceeded
        );
    }
}
