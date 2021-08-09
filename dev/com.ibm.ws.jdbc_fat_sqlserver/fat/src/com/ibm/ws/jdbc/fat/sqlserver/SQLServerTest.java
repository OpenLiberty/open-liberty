/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.sqlserver;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MSSQLServerContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.containers.SimpleLogConsumer;
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

    @ClassRule
    public static MSSQLServerContainer<?> sqlserver = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2019-CU10-ubuntu-16.04") //
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "sqlserver")) //
                    .acceptLicense();

    @BeforeClass
    public static void setUp() throws Exception {
        FATSuite.setupDatabase(sqlserver, false);

        server.addEnvVar("DBNAME", FATSuite.DB_NAME);
        server.addEnvVar("HOST", sqlserver.getContainerIpAddress());
        server.addEnvVar("PORT", Integer.toString(sqlserver.getFirstMappedPort()));
        server.addEnvVar("USER", sqlserver.getUsername());
        server.addEnvVar("PASSWORD", sqlserver.getPassword());

        // Create a normal Java EE application and export to server
        ShrinkHelper.defaultApp(server, APP_NAME, "web");

        server.startServer();
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
