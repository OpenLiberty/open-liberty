/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transaction.web.HeuristicsServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class HeuristicsTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/heuristics";

    @Server("com.ibm.ws.transaction")
    @TestServlet(servlet = HeuristicsServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transaction.web.*");

        server.setServerStartTimeout(300000);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                server.stopServer("WTRN0017W", "WTRN0075W", "WTRN0076W");
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }

}
