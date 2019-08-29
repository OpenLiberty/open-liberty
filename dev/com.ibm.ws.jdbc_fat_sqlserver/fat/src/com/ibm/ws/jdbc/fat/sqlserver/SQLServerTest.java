/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.sqlserver;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.SQLServerTestServlet;

@RunWith(FATRunner.class)
public class SQLServerTest extends FATServletClient {

    public static final String APP_NAME = "sqlserverfat";
    public static final String SERVLET_NAME = "SQLServerTestServlet";

    @Server("com.ibm.ws.jdbc.fat.sqlserver")
    @TestServlet(servlet = SQLServerTestServlet.class, path = APP_NAME + '/' + SERVLET_NAME)
    public static LibertyServer server;

    //TODO change this to the SQL Server 2019 official image when it is released.  Currently, using a preview image.
    @ClassRule
    public static MSSQLServerContainer<?> sqlserver = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2019-CTP3.1-ubuntu").withLogConsumer(SQLServerTest::log);

    //Private Method: used to setup logging for containers to this class.
    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(sqlserver.getClass(), "output", msg);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        String dbName = "test";
        initDatabase(sqlserver, dbName);

        server.addEnvVar("DBNAME", dbName);
        server.addEnvVar("HOST", sqlserver.getContainerIpAddress());
        server.addEnvVar("PORT", Integer.toString(sqlserver.getFirstMappedPort()));
        server.addEnvVar("USER", sqlserver.getUsername());
        server.addEnvVar("PASSWORD", sqlserver.getPassword());

        // Create a normal Java EE application and export to server
        ShrinkHelper.defaultApp(server, APP_NAME, "web");

        server.startServer();

        runTest(server, APP_NAME + '/' + SERVLET_NAME, "initDatabase");
    }

    //Helper method
    private static void initDatabase(JdbcDatabaseContainer<?> cont, String dbName) throws NoDriverFoundException, SQLException {
        // Create Database
        try (Connection conn = cont.createConnection("")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE DATABASE [" + dbName + "];");
            stmt.close();
        }

        //Setup distributed connection.
        try (Connection conn = cont.createConnection("")) {
            Statement stmt = conn.createStatement();
            stmt.execute("EXEC sp_sqljdbc_xa_install");
            stmt.close();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            ArrayList<String> expectedErrorMessages = new ArrayList<String>();
            // Some config warnings are expected if the JDBC driver version is old
            List<String> jdbcVersionLines = server.findStringsInLogs("DSRA8206I");
            if (!jdbcVersionLines.isEmpty()) {
                //DSRA8206I: JDBC driver version  : 7.4.1.0
                String[] parts = jdbcVersionLines.get(0).split(" |\\x2E"); // space or .
                if (parts.length > 4) {
                    int major = Integer.parseInt(parts[parts.length - 4]);
                    int minor = Integer.parseInt(parts[parts.length - 3]);
                    System.out.println("JDBC driver version " + major + '.' + minor);
                    if (major < 6) {
                        expectedErrorMessages.add("DSRA8020E.*serverNameAsACE");
                        expectedErrorMessages.add("DSRA8020E.*transparentNetworkIPResolution");
                    }
                }
            }
            expectedErrorMessages.add("DSRA0304E"); // From XAException upon rollback of already timed out XAResource
            expectedErrorMessages.add("DSRA0302E.*XAER_NOTA"); // More specific message for rollback of already timed out XAResource
            expectedErrorMessages.add("J2CA0027E.*rollback"); // JCA message for rollback of already timed out XAResource
            expectedErrorMessages.add("J2CA0027E.*commit"); // JCA message for attempted commit of already timed out XAResource
            server.stopServer(expectedErrorMessages.toArray(new String[expectedErrorMessages.size()]));
        }
    }
}
