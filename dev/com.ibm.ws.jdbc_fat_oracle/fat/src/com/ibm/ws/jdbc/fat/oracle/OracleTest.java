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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

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

    @Server("com.ibm.ws.jdbc.fat.oracle")
    @TestServlet(servlet = OracleTestServlet.class, path = JEE_APP + "/" + SERVLET_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
    	// Set server environment variables
        server.addEnvVar("URL", FATSuite.oracle.getJdbcUrl());
        server.addEnvVar("USER", FATSuite.oracle.getUsername());
        server.addEnvVar("PASSWORD", FATSuite.oracle.getPassword());
        server.addEnvVar("DBNAME", "XE");
        server.addEnvVar("PORT", Integer.toString(FATSuite.oracle.getFirstMappedPort()));
        server.addEnvVar("HOST", FATSuite.oracle.getContainerIpAddress());

    	// Create a normal Java EE application and export to server
    	ShrinkHelper.defaultApp(server, JEE_APP, "web");

    	// Start Server
        server.startServer();

        // Create database tables
        initDatabaseTables();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }
    
    private static void initDatabaseTables() throws SQLException {
    	try (Connection conn = FATSuite.oracle.createConnection("")) {
            Statement stmt = conn.createStatement();
            
            //Create MYTABLE for OracleTest.class
            try {
                stmt.execute("DROP TABLE MYTABLE");
            } catch (SQLException x) {
                // probably didn't exist
            }
            stmt.execute("CREATE TABLE MYTABLE (ID NUMBER NOT NULL PRIMARY KEY, STRVAL NVARCHAR2(40))");

            //Create CONCOUNT for OracleTest.class
            try {
                stmt.execute("DROP TABLE CONCOUNT");
            } catch (SQLException x) {
                // probably didn't exist
            }
            stmt.execute("CREATE TABLE CONCOUNT (NUMCONNECTIONS NUMBER NOT NULL)");
            stmt.execute("INSERT INTO CONCOUNT VALUES(0)");

            //Close statement
            stmt.close();
    	}
    }
}
