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
package com.ibm.ws.jdbc.fat.oracle;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jdbc.fat.oracle.containters.OracleSSLContainer;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import ssl.web.OracleSSLTestServlet;

/**
 * This test class test's connections using SSL.
 * This test class requires the use of a separate test container from the other test classes.
 * Therefore, only run this class in FULL mode.
 */
@RunWith(FATRunner.class)
@Mode(FULL)
public class OracleSSLTest extends FATServletClient {

    public static final String JEE_APP = "oraclesslfat";
    public static final String SERVLET_NAME = "OracleSSLTestServlet";

    @Server("com.ibm.ws.jdbc.fat.oracle.ssl")
    @TestServlet(servlet = OracleSSLTestServlet.class, path = JEE_APP + "/" + SERVLET_NAME)
    public static LibertyServer server;

    @ClassRule
    public static OracleSSLContainer oracle = new OracleSSLContainer();

    @BeforeClass
    public static void setUp() throws Exception {

        // Set server environment variables
        server.addEnvVar("BASIC_URL", oracle.getJdbcUrl());
        server.addEnvVar("SSL_URL", oracle.getJdbcSSLUrl());
        server.addEnvVar("USER", oracle.getUsername());
        server.addEnvVar("PASS", oracle.getPassword());
        server.addEnvVar("WALLET_PASS", oracle.getWalletPassword());

        // Create a normal Java EE application and export to server
        ShrinkHelper.defaultApp(server, JEE_APP, "ssl.web");

        // Start Server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

}
