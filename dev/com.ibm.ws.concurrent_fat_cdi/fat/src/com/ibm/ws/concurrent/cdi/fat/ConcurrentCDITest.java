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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import concurrent.cdi4.web.ConcurrentCDI4Servlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 21)
public class ConcurrentCDITest extends FATServletClient {

    public static final String APP_NAME = "concurrentCDIApp";
    public static final String APP_NAME_EE10 = "concurrentCDI4App";

    @Server("concurrent_fat_cdi")
    @TestServlets({
                    @TestServlet(servlet = ConcurrentCDI4Servlet.class, contextRoot = APP_NAME_EE10)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "concurrent.cdi.web");
        // TODO Adding "concurrent.cu3.web" to the following would cause conflict with app-defined ManagedExecutorService.
        // There is a spec proposal to detect conflict and avoid automatically adding the bean.
        ShrinkHelper.defaultDropinApp(server, APP_NAME_EE10, "concurrent.cdi4.web");
        server.startServer();
        runTest(server, APP_NAME_EE10 + '/' + ConcurrentCDI4Servlet.class.getSimpleName(), "initTransactionService");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          "CWWKC1101E.*scheduled-executor-without-app-context", // tests lack of context from scheduled executor thread
                          "CWWKE1205E" // test case intentionally causes startTimeout to be exceeded
        );
    }

    @Test
    public void testInjectContextServiceDefaultInstance() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectContextServiceQualified() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedExecutorServiceDefaultInstance() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedExecutorServiceQualified() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedScheduledExecutorServiceDefaultInstance() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedScheduledExecutorServiceQualified() throws Exception {
        runTest(server, APP_NAME, testName);
    }
}
