/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.sqlserver;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
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
    public static JdbcDatabaseContainer<?> sqlserver = DatabaseContainerFactory.createType(DatabaseContainerType.SQLServer);

    @BeforeClass
    public static void setUp() throws Exception {
        FATSuite.setupDatabase(sqlserver);

        server.addEnvVar("SQL_DBNAME", FATSuite.DB_NAME);
        server.addEnvVar("SQL_HOST", sqlserver.getHost());
        server.addEnvVar("SQL_PORT", Integer.toString(sqlserver.getFirstMappedPort()));
        server.addEnvVar("SQL_USER", sqlserver.getUsername());
        server.addEnvVar("SQL_PASSWORD", sqlserver.getPassword());

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

    @Test
    public void testAuthenticationSchemeNTLM() throws Exception {
        server.setTraceMarkToEndOfDefaultTrace();
        runTest(server, APP_NAME + "/SQLServerTestServlet", testName);
        assertTrue(server.findStringsInTrace(".*Found vendor property: authenticationScheme=NTLM.*").size() > 0);
        assertTrue(server.findStringsInTrace(".*set authenticationScheme = NTLM.*").size() > 0);

    }
}
