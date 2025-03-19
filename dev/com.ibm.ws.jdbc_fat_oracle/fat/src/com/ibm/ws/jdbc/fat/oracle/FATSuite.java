/*******************************************************************************
 * Copyright (c) 2016, 2024 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.SimpleLogConsumer;
import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.FATRunner;
import oracle.jdbc.pool.OracleDataSource;

@RunWith(Suite.class)
@SuiteClasses({
                OracleCheckpointTest.class,
                OracleCustomTrace.class,
                OracleTest.class,
                OracleTraceTest.class,
                OracleUCPTest.class,
                OracleSSLTest.class
})
public class FATSuite extends TestContainerSuite {

    public static final DockerImageName ORACLE_IMAGE_NAME = DockerImageName.parse("ghcr.io/gvenzl/oracle-free:23-slim-faststart")
                    .asCompatibleSubstituteFor("gvenzl/oracle-free");

    private static OracleContainer sharedContainer = new OracleContainer(ORACLE_IMAGE_NAME)
                    .usingSid()
                    .withPassword("VeRyUniqu3P@ssw0rd")
                    .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 3 : 25))
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "Oracle"));

    public static OracleContainer getSharedOracleContainer() {
        if (!sharedContainer.isRunning()) {
            sharedContainer.start();
        }
        initDatabaseTables(sharedContainer);
        return sharedContainer;
    }

    @AfterClass
    public static void cleanupContainer() {
        if (sharedContainer.isRunning()) {
            sharedContainer.stop();
        }
    }

    static void initDatabaseTables(OracleContainer container) {
        Properties connProps = new Properties();
        // This property prevents "ORA-01882: timezone region not found" errors due to
        // the Oracle DB not understanding
        // some time zones(specifically those used by our RHEL 6 test systems).
        connProps.put("oracle.jdbc.timezoneAsRegion", "false");

        try {
            OracleDataSource ds = new OracleDataSource();
            ds.setConnectionProperties(connProps);
            ds.setUser(container.getUsername());
            ds.setPassword(container.getPassword());
            ds.setURL(container.getJdbcUrl());

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

                // Create BLOBTABLE for OracleTest.class
                try {
                    stmt.execute("DROP TABLE BLOBTABLE");
                } catch (SQLException x) {
                    // probably didn't exist
                }
                stmt.execute("CREATE TABLE BLOBTABLE (ID NUMBER NOT NULL PRIMARY KEY, MYFILE BLOB)");

                // Close statements
                ps.close();
                stmt.close();
            }
        } catch (SQLException sqle) {
            throw new RuntimeException("Unable to setup test tables");
        }

    }
}
