/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.oracle;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.OracleContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.OracleTestServlet;

@RunWith(FATRunner.class)
public class OracleTest extends FATServletClient {

    public static final String JEE_APP = "oraclejdbcfat";
    public static final String SERVLET_NAME = "OracleTestServlet";

    //TODO replace this container with the official oracle-xe container if/when it is available without a license
    @ClassRule
    public static OracleContainer oracle = new OracleContainer("oracleinanutshell/oracle-xe-11g");

    @Server("com.ibm.ws.jdbc.fat.oracle")
    @TestServlet(servlet = OracleTestServlet.class, path = JEE_APP + "/" + SERVLET_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
    	// Set server environment variables
        server.addEnvVar("URL", oracle.getJdbcUrl());
        server.addEnvVar("USER", oracle.getUsername());
        server.addEnvVar("PASSWORD", oracle.getPassword());
        server.addEnvVar("DBNAME", "XE");
        server.addEnvVar("PORT", Integer.toString(oracle.getFirstMappedPort()));
        server.addEnvVar("HOST", oracle.getContainerIpAddress());

    	// Create a normal Java EE application and export to server
    	ShrinkHelper.defaultApp(server, JEE_APP, "web");

    	// Start Server
        server.startServer();

        // Run initDatabseTables
        runTest(server, JEE_APP + '/' + SERVLET_NAME, "initDatabaseTables");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }
}
