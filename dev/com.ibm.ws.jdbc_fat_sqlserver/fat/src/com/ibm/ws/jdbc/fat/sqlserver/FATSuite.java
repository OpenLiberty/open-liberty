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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                SQLServerTest.class,
                SQLServerSSLTest.class
})
public class FATSuite extends TestContainerSuite {

    public static final String DB_NAME = "test";
    public static final String TABLE_NAME = "MYTABLE";

    /**
     * Create database and tables needed by test servlet.
     * Use a native JDBC connection from the driver so that there aren't any dependencies on the appserver.
     * The SQLServer container already has knowledge of how to create a native JDBC connection.
     *
     * @throws SQLException
     */
    public static void setupDatabase(JdbcDatabaseContainer<?> sqlserver) throws SQLException {

        //Create test table
        sqlserver.withUrlParam("databaseName", DB_NAME);
        Log.info(FATSuite.class, "setupDatabase", "Attempting to setup database table with name: " + TABLE_NAME + "."
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