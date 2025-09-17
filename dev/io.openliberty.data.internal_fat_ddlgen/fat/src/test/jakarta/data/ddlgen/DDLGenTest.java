/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
        String preamble = createUserAndSchema(FATSuite.testContainer, "dbuser", "DB!userPassw0rd");

        DatabaseContainerUtil.build(server, FATSuite.testContainer)
                        .withDriverVariable()
                        .withAuthVariables("dbuser", "DB!userPassw0rd")
                        .withDatabaseProperties()
                        .modify();

        WebArchive war = ShrinkHelper.buildDefaultApp("DDLGenTestApp", "test.jakarta.data.ddlgen.web");
        ShrinkHelper.exportAppToServer(server, war);

        server.startServer();

        DDLGenScriptResult result = DDLGenScript.build(server)
                        .execute()
                        .assertSuccessful()
                        .assertDDLFile("application[DDLGenTestApp].module[DDLGenTestApp.war].databaseStore[java.comp.DefaultDataSource]_JakartaData.ddl")
                        .assertDDLFile("application[DDLGenTestApp].databaseStore[TestDataSource]_JakartaData.ddl")
                        .assertDDLFile("application[DDLGenTestApp].databaseStore[jdbc.TestDataSourceJndi]_JakartaData.ddl")
                        .assertDDLFile("application[DDLGenTestApp].databaseStore[java.app.env.jdbc.TestDataSourceResourceRef]_JakartaData.ddl")
                        .assertNoDDLFileLike("java.app.env.persistence.MyPersistenceUnitRef")
                        .assertDDLFile("databaseStore[TestDataStore]_JakartaData.ddl");

        String scripts = String.join(",", result.getFileLocations());

        runTest(server, "DDLGenTestApp/DDLGenTestServlet", "executeDDL" +
                                                           "&scripts=" + scripts +
                                                           "&preamble=" + preamble.replace(" ", "]") +
                                                           "&usingDataSource=jdbc/AdminDataSource");

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
     * @return String preamble necessary for executing DDL statements as admin user in the database
     */
    private static String createUserAndSchema(JdbcDatabaseContainer<?> testContainer, String user, String pass) throws Exception {
        final DatabaseContainerType type = DatabaseContainerType.valueOf(testContainer);

        String preamble = "";

        Log.entering(FATSuite.class, "createUserAndSchema", new Object[] { type, user, pass });
        try {
            switch (type) {
                case DB2: // Working - admin user will set schema to user
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
                    results.add(testContainer.execInContainer("su", "-", testContainer.getUsername(), "-c",
                                                              connectDBPrefix + "db2 grant use of tablespace USERSPACE1 to user " + user + ";"));

                    for (ExecResult result : results) {
                        assertEquals("Unexpected result from command on DB2 container, std.err: " + result.getStderr(), 0, result.getExitCode());
                    }

                    preamble = "set schema " + user;
                    break;
                case Derby: // Working - uses user instead of admin when executing ddl statements
                    break;
                case DerbyClient: // Unnecessary
                    break;
                case Oracle: // Working - admin user will set schema to user
                    // On oracle a user and a schema are one and the same, so just create a user
                    try (Connection con = testContainer.createConnection(""); Statement stmt = con.createStatement()) {
                        stmt.executeUpdate("alter session set \"_ORACLE_SCRIPT\"=true");
                        stmt.executeUpdate("alter user " + testContainer.getUsername() + " quota unlimited on users");
                        stmt.executeUpdate("create user " + user + " identified by \"" + pass + "\"");
                        stmt.executeUpdate("grant connect, create session, create table to " + user);
                        stmt.executeUpdate("grant unlimited tablespace to " + user);
                    }

                    preamble = "alter session set current_schema=" + user;
                    break;
                case Postgres: // Working - admin and user share public schema when none is defined
                    try (Connection con = testContainer.createConnection(""); Statement stmt = con.createStatement()) {
                        stmt.executeUpdate("create user " + user + " with password '" + pass + "'");
                        stmt.executeUpdate("grant connect on database " + testContainer.getDatabaseName() + " to " + user + " with grant option");
                        stmt.executeUpdate("create schema " + user);
                        stmt.executeUpdate("alter default privileges in schema " + user +
                                           " grant all privileges on tables to " + user);
                        stmt.executeUpdate("alter default privileges in schema public " +
                                           " grant all privileges on tables to " + user);
                        stmt.executeUpdate("grant all on schema " + user + " to " + user);
                    }
                    break;
                case SQLServer: // Working - admin and user share dbo schema when none is defined
                    //TODO remove once bug is fixed: https://github.com/testcontainers/testcontainers-java/pull/9463
                    //replace with: testContainer.createConnection(";databaseName=TEST")
                    testContainer.withUrlParam("databaseName", "TEST");

                    try (Connection con = testContainer.createConnection(""); Statement stmt = con.createStatement()) {
                        stmt.executeUpdate("create login " + user + " with password = '" + pass + "'");
                        stmt.executeUpdate("create user " + user + " from login " + user + " with default_schema=dbo");
                        stmt.executeUpdate("grant connect to " + user);
                        stmt.executeUpdate("create schema " + user);
                        stmt.executeUpdate("grant control on schema :: " + user + " to " + user);
                        stmt.executeUpdate("grant control on schema :: dbo to " + user);
                        stmt.executeUpdate("grant create table to " + user);
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown database container type");
            }
        } finally {
            Log.exiting(FATSuite.class, "createUserAndSchema", preamble);
        }
        return preamble;
    }
}
