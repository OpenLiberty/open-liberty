/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jdbc.h2.client.web;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Tests for H2 in TCP client/server mode. Exercises behaviors unique to
 * TCP connectivity: remote metadata path and session isolation across
 * independent TCP sessions.
 */
@SuppressWarnings("serial")
@WebServlet("/*")
public class H2ClientTestServlet extends FATServlet {

    /** Backed by H2 ConnectionPoolDataSource via TCP */
    @Resource(lookup = "jdbc/H2ConnectionPoolDataSource")
    DataSource h2cpDataSource;

    /** Backed by H2 DataSource via TCP */
    @Resource(lookup = "jdbc/H2DataSource")
    DataSource h2DataSource;

    /** Backed by H2 Driver via TCP */
    @Resource(lookup = "jdbc/H2Driver")
    DataSource h2driverDataSource;

    /** Backed by H2 XADataSource via TCP */
    @Resource(lookup = "jdbc/H2XADataSource")
    DataSource h2xaDataSource;

    /**
     * Create the PLANETS table and populate initial rows.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        try (Connection con = h2cpDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("""
                            CREATE TABLE PLANETS (
                                NAME       VARCHAR(20)    NOT NULL PRIMARY KEY,
                                RADIUS_KM  INT            NOT NULL,
                                ORBIT_DAYS DECIMAL(10,3)  NOT NULL
                            )
                            """);
            String insert = "INSERT INTO PLANETS VALUES (?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insert)) {
                Object[][] rows = {
                    { "Mercury", 2440,  87.969  },
                    { "Venus",   6052,  224.701 },
                    { "Earth",   6371,  365.256 },
                    { "Mars",    3390,  686.971 },
                };
                for (Object[] row : rows) {
                    ps.setString(1, (String) row[0]);
                    ps.setInt(2, (Integer) row[1]);
                    ps.setDouble(3, (Double) row[2]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException x) {
            throw new ServletException(x.getMessage(), x);
        }
    }

    @Test
    public void testConnectionPoolDataSource() throws Exception {
        try (Connection con = h2cpDataSource.getConnection()) {
            assertEquals("TESTDB", con.getCatalog());
        }
    }

    @Test
    public void testDataSource() throws Exception {
        try (Connection con = h2DataSource.getConnection()) {
            assertEquals("TESTDB", con.getCatalog());
        }
    }

    @Test
    public void testDriver() throws Exception {
        try (Connection con = h2driverDataSource.getConnection()) {
            assertEquals("TESTDB", con.getCatalog());
        }
    }

    @Test
    public void testXADataSource() throws Exception {
        try (Connection con = h2xaDataSource.getConnection()) {
            assertEquals("TESTDB", con.getCatalog());
        }
    }

    /**
     * Verifies the DatabaseMetaRemote TCP path returns correct values for
     * getDatabaseProductName() and getDriverName(). These values are sent over
     * the TCP wire in H2's remote protocol rather than returned from local state.
     */
    @Test
    public void testRemoteMetadata() throws Exception {
        try (Connection con = h2DataSource.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            assertEquals("H2", meta.getDatabaseProductName());
            assertEquals("H2 JDBC Driver", meta.getDriverName());
        }
    }

    /**
     * Verifies session isolation across two independent TCP sessions.
     * Writes in one session (con1, autoCommit off) are not visible to a second
     * session (con2) until con1 commits. This is meaningful only over TCP where
     * each connection is an independent SessionRemote instance on the server.
     */
    @Test
    public void testSessionIsolation() throws Exception {
        try (Connection con1 = h2cpDataSource.getConnection();
             Connection con2 = h2cpDataSource.getConnection()) {

            con1.setAutoCommit(false);

            // Insert a new planet in con1's uncommitted transaction
            try (PreparedStatement ps = con1.prepareStatement(
                         "INSERT INTO PLANETS VALUES (?, ?, ?)")) {
                ps.setString(1, "Jupiter");
                ps.setInt(2, 71492);
                ps.setDouble(3, 4332.589);
                ps.executeUpdate();
            }

            // con2 must not see con1's uncommitted row
            try (PreparedStatement ps = con2.prepareStatement(
                         "SELECT COUNT(*) FROM PLANETS WHERE NAME=?")) {
                ps.setString(1, "Jupiter");
                ResultSet rs = ps.executeQuery();
                rs.next();
                assertEquals("con2 must not see con1's uncommitted INSERT",
                             0, rs.getInt(1));
            }

            con1.commit();

            // After commit, con2 must see the row
            try (PreparedStatement ps = con2.prepareStatement(
                         "SELECT COUNT(*) FROM PLANETS WHERE NAME=?")) {
                ps.setString(1, "Jupiter");
                ResultSet rs = ps.executeQuery();
                rs.next();
                assertEquals("con2 must see con1's committed INSERT",
                             1, rs.getInt(1));
            }

            // Clean up
            try (Statement stmt = con1.createStatement()) {
                stmt.executeUpdate("DELETE FROM PLANETS WHERE NAME='Jupiter'");
            }
            con1.commit();
        }
    }
}
