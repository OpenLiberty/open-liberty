/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.containers.SimpleLogConsumer;

@RunWith(Suite.class)
@SuiteClasses(SQLServerTest.class)
public class FATSuite {

    public static final String DB_NAME = "test";

    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
    }

    private static final DockerImageName sqlserverImage = DockerImageName.parse("kyleaure/sqlserver-ssl:2019-CU10-ubuntu-16.04")//
                    .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server");

    @ClassRule
    public static MSSQLServerContainer<?> sqlserver = new MSSQLServerContainer<>(sqlserverImage) //
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "sqlserver")) //
                    .acceptLicense();

    /**
     * Create database and tables needed by test servlet.
     * Use a native JDBC connection from the driver so that there aren't any dependencies on the appserver.
     * The SQLServer container already has knowledge of how to create a native JDBC connection.
     *
     * @throws SQLException
     */
    @BeforeClass
    public static void setup() throws SQLException {
        final String TABLE_NAME = "MYTABLE";

        //Setup database and settings
        Log.info(FATSuite.class, "setup", "Attempting to setup database with name: " + DB_NAME + "."
                                          + " With connection URL: " + sqlserver.getJdbcUrl());
        try (Connection conn = sqlserver.createConnection(""); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE [" + DB_NAME + "];");
            stmt.execute("EXEC sp_sqljdbc_xa_install");
            stmt.execute("ALTER DATABASE " + DB_NAME + " SET ALLOW_SNAPSHOT_ISOLATION ON");
        }

        //Create test table
        sqlserver.withUrlParam("databaseName", DB_NAME);
        Log.info(FATSuite.class, "setup", "Attempting to setup database table with name: " + TABLE_NAME + "."
                                          + " With connection URL: " + sqlserver.getJdbcUrl());
        try (Connection conn = sqlserver.createConnection(""); Statement stmt = conn.createStatement()) {
            // Create tables
            int version = conn.getMetaData().getDatabaseMajorVersion();
            try {
                if (version >= 13) // SQLServer 2016 or higher
                    stmt.execute("DROP TABLE IF EXISTS " + TABLE_NAME);
                else
                    stmt.execute("DROP TABLE " + TABLE_NAME);
            } catch (SQLException x) {
                // probably didn't exist
            }
            stmt.execute("CREATE TABLE " + TABLE_NAME + " (ID SMALLINT NOT NULL PRIMARY KEY, STRVAL NVARCHAR(40))");
        }
    }
}