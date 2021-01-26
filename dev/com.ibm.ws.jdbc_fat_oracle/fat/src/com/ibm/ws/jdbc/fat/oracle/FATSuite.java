/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.OracleContainer;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.containers.SimpleLogConsumer;
import oracle.jdbc.pool.OracleDataSource;

@RunWith(Suite.class)
@SuiteClasses({ OracleTest.class, OracleTraceTest.class, OracleUCPTest.class })
public class FATSuite {

    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
    }

    // TODO replace this container with the official oracle-xe container if/when it is available without a license
    @ClassRule
    public static OracleContainer oracle = new OracleContainer("kyleaure/oracle-18.4.0-xe-prebuilt:1.0")
                    .withExposedPorts(1521, 5500, 8080) // need to manually expose ports due to regression in 1.14.0
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "Oracle"));

    public static void initDatabaseTables() throws SQLException {
        Properties connProps = new Properties();
        // This property prevents "ORA-01882: timezone region not found" errors due to
        // the Oracle DB not understanding
        // some time zones(specifically those used by our RHEL 6 test systems).
        connProps.put("oracle.jdbc.timezoneAsRegion", "false");

        OracleDataSource ds = new OracleDataSource();
        ds.setConnectionProperties(connProps);
        ds.setUser(oracle.getUsername());
        ds.setPassword(oracle.getPassword());
        ds.setURL(oracle.getJdbcUrl());

        try (Connection conn = ds.getConnection()) {
            Statement stmt = conn.createStatement();

            // Create MYTABLE for OracleTest.class and OracleTraceTest.class
            try {
                stmt.execute("DROP TABLE MYTABLE");
            } catch (SQLException x) {
                // probably didn't exist
            }
            stmt.execute("CREATE TABLE MYTABLE (ID NUMBER NOT NULL PRIMARY KEY, STRVAL NVARCHAR2(40))");

            // Create CONCOUNT for OracleTest.class
            try {
                stmt.execute("DROP TABLE CONCOUNT");
            } catch (SQLException x) {
                // probably didn't exist
            }
            stmt.execute("CREATE TABLE CONCOUNT (NUMCONNECTIONS NUMBER NOT NULL)");
            stmt.execute("INSERT INTO CONCOUNT VALUES(0)");

            // Create COLORTABLE for OracleUCPTest.class
            try {
                stmt.execute("DROP TABLE COLORTABLE");
            } catch (SQLException x) {
                // probably didn't exist
            }
            stmt.execute("CREATE TABLE COLORTABLE (ID NUMBER NOT NULL PRIMARY KEY, COLOR NVARCHAR2(40))");
            PreparedStatement ps = conn.prepareStatement("INSERT INTO COLORTABLE VALUES(?,?)");
            ps.setInt(1, 1);
            ps.setString(2, "maroon");
            ps.executeUpdate();

            // Close statements
            ps.close();
            stmt.close();
        }
    }
}
