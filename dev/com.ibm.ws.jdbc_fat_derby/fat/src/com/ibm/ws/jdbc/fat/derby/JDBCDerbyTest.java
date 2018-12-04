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
package com.ibm.ws.jdbc.fat.derby;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.derby.web.JDBCDerbyServlet;

@RunWith(FATRunner.class)
public class JDBCDerbyTest extends FATServletClient {

    @Server(FATSuite.SERVER)
    @TestServlet(servlet = JDBCDerbyServlet.class, contextRoot = FATSuite.jdbcapp)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0701E", //expected by testTransactionalSetting
                          "DSRA4011E", //expected by testTNConfigIsoLvlReverse
                          "SRVE0319E"); //expected by testTNConfigTnsl
    }
}
