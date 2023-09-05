/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
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

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import concurrent.cdi3.web.ConcurrentCDIServlet;

@RunWith(FATRunner.class)
public class ConcurrentCDI3Test extends FATServletClient {

    public static final String APP_NAME = "concurrentCDI3App";

    @Server("concurrent_fat_cdi3")
    @TestServlet(servlet = ConcurrentCDIServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "concurrent.cdi3.web");
        server.startServer();
        runTest(server, APP_NAME + '/' + ConcurrentCDIServlet.class.getSimpleName(), "initTransactionService");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
