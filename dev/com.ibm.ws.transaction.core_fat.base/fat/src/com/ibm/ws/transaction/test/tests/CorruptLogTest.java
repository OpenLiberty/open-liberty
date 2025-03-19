/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.transaction.test.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import servlets.CorruptLogServlet;

@RunWith(FATRunner.class)
public class CorruptLogTest extends FATServletClient {

    public static final String APP_NAME = "transaction";

    @Server("com.ibm.ws.transaction_corruptlog")
    @TestServlet(servlet = CorruptLogServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void beforeSuite() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "servlets.*");
    }

    @Before
    public void before() throws Exception {
        server.startServer();
    }

    @After
    public void after() throws Exception {
        server.stopServer("WTRN0111E", "WTRN0000E", "WTRN0112E");
    }
}