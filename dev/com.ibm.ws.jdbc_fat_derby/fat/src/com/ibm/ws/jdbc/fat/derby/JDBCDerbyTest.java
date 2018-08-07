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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.derby.web.JDBCDerbyServlet;

@RunWith(FATRunner.class)
public class JDBCDerbyTest extends FATServletClient {

    public static final String APP_NAME = "jdbcapp";

    @Server("com.ibm.ws.jdbc.fat.derby")
    @TestServlet(servlet = JDBCDerbyServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "jdbc.fat.derby.web");

        JavaArchive tranNoneDriver = ShrinkWrap.create(JavaArchive.class, "trandriver.jar")//
                        .addPackage("jdbc.tran.none.driver");
        ShrinkHelper.exportToServer(server, "../../shared/resources/derby", tranNoneDriver);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
