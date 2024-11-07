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
package test.jakarta.data.ddlgen;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.DDLGenScript;
import componenttest.topology.utils.DDLGenScript.DDLGenScriptResult;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.ddlgen.web.DDLGenTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DDLGenTest extends FATServletClient {

    @Server("io.openliberty.data.internal.fat.ddlgen")
    @TestServlet(servlet = DDLGenTestServlet.class, contextRoot = "DDLGenTestApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create an additional user and schema on database (matches authData id="dbuser")
        createUserAndSchema(FATSuite.testContainer, "dbuser", "DBuserPassw0rd");

        // Get driver type
        DatabaseContainerType type = DatabaseContainerType.valueOf(FATSuite.testContainer);
        server.addEnvVar("DB_DRIVER", type.getDriverName());

        // Set up server DataSource properties
        DatabaseContainerUtil.setupDataSourceDatabaseProperties(server, FATSuite.testContainer);

        WebArchive war = ShrinkHelper.buildDefaultApp("DDLGenTestApp", "test.jakarta.data.ddlgen.web");
        ShrinkHelper.exportAppToServer(server, war);

        server.startServer();

        DDLGenScriptResult result = DDLGenScript.build(server)
                        .execute()
                        .assertSuccessful()
                        .assertDDLFile("databaseStore[TestDataStore]_JakartaData.ddl");

        runTest(server, "DDLGenTestApp/DDLGenTestServlet", "executeDDL" +
                                                           "&scripts=" + String.join(",", result.getFileLocations()) +
                                                           "&withDatabaseStore=TestDataStore" +
                                                           "&usingDataSource=java:app/env/adminDataSourceRef");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * TODO if we end up writing more tests that require a separate user/schema
     * consider moving this method to DatabaseContainerUtil {@link DatabaseContainerUtil}
     *
     * @param testContainer the container
     * @param user          the user/schema to create (the schema will be named after the user)
     * @param pass          the password for the user
     * @throws Exception if any database connection or query executions occur
     */
    private static void createUserAndSchema(JdbcDatabaseContainer<?> testContainer, String user, String pass) throws Exception {
        final DatabaseContainerType type = DatabaseContainerType.valueOf(testContainer);

        Log.entering(FATSuite.class, "createUserAndSchema", new Object[] { type, user, pass });
        try {
            switch (type) {
                case DB2: //working
                    final List<ExecResult> results = new ArrayList<>();
                    final String connectDBPrefix = "db2 connect to " + testContainer.getDatabaseName() + "; ";

                    // On DB2 database users and OS users are one and the same, so need to create a user using OS commands.
                    results.add(testContainer.execInContainer("sh", "-c", "useradd -s /bin/bash " + user));
                    results.add(testContainer.execInContainer("sh", "-c", "echo '" + user + ":" + pass + "' | chpasswd"));
                    results.add(testContainer.execInContainer("su", "-", testContainer.getUsername(), "-c",
                                                              connectDBPrefix + "db2 grant createtab, connect on database to user " + user + ";"));
                    results.add(testContainer.execInContainer("su", "-", testContainer.getUsername(), "-c",
                                                              connectDBPrefix + "db2 create schema authorization " + user + ";"));
                    results.add(testContainer.execInContainer("su", "-", testContainer.getUsername(), "-c",
                                                              connectDBPrefix + "db2 grant all privileges on schema " + user + " to user " + user + ";"));

                    for (ExecResult result : results) {
                        assertEquals("Unexpected result from command on DB2 container, std.err: " + result.getStderr(), 0, result.getExitCode());
                    }
                    break;
                case Derby:
                    // Unnecessary
                    break;
                case DerbyClient:
                    // Unnecessary
                    break;
                case Oracle: //working
                    // On oracle a user and a schema are one and the same, so just create a user
                    try (Connection con = testContainer.createConnection(""); Statement stmt = con.createStatement()) {
                        stmt.executeUpdate("alter session set \"_ORACLE_SCRIPT\"=true");
                        stmt.executeUpdate("alter user " + testContainer.getUsername() + " quota unlimited on users");
                        stmt.executeUpdate("create user " + user + " identified by " + pass);
                        stmt.executeUpdate("grant connect, create session to " + user);
                        stmt.executeUpdate("grant unlimited tablespace to " + user);
                    }
                    break;
                case Postgres: //working
                    try (Connection con = testContainer.createConnection(""); Statement stmt = con.createStatement()) {
                        stmt.executeUpdate("create user " + user + " with password '" + pass + "'");
                        stmt.executeUpdate("grant connect on database " + testContainer.getDatabaseName() + " to " + user + " with grant option");
                        stmt.executeUpdate("create schema " + user);
                        stmt.executeUpdate("alter default privileges in schema " + user +
                                           " grant all privileges on tables to " + user);
                        stmt.executeUpdate("grant usage on schema " + user + " to " + user);
                    }
                    break;
                case SQLServer: //working
                    //TODO remove once bug is fixed: https://github.com/testcontainers/testcontainers-java/pull/9463
                    //replace with: testContainer.createConnection(";databaseName=TEST")
                    testContainer.withUrlParam("databaseName", "TEST");

                    try (Connection con = testContainer.createConnection(""); Statement stmt = con.createStatement()) {
                        stmt.executeUpdate("create login " + user + " with password = '" + pass + "'");
                        stmt.executeUpdate("create user " + user + " from login " + user);
                        stmt.executeUpdate("grant connect to " + user);
                        stmt.executeUpdate("create schema " + user);
                        stmt.executeUpdate("grant control on schema :: " + user + " to " + user);
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown database container type");
            }
        } finally {
            Log.exiting(FATSuite.class, "createUserAndSchema");
        }
    }
}
