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
package test.jdbc.h2.web;

import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Referenceable;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.junit.Test;

import componenttest.annotation.SkipIfSysProp;
import componenttest.app.FATServlet;

@DataSourceDefinition(name = "java:app/jdbc/H2JdbcDataSource",
                      className = "org.h2.jdbcx.JdbcDataSource",
                      url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                      user = "dbuser1",
                      password = "dbpwd1")
@DataSourceDefinition(name = "java:app/jdbc/H2ConnectionPoolDataSource",
                      className = "javax.sql.ConnectionPoolDataSource",
                      url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                      user = "dbuser1",
                      password = "dbpwd1")
@DataSourceDefinition(name = "java:comp/jdbc/H2DataSource",
                      className = "javax.sql.DataSource",
                      url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                      user = "dbuser1",
                      password = "dbpwd1")
@DataSourceDefinition(name = "java:global/jdbc/H2Driver",
                      className = "java.sql.Driver",
                      url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                      user = "dbuser1",
                      password = "dbpwd1")
@DataSourceDefinition(name = "java:module/jdbc/H2XADataSource",
                      className = "javax.sql.XADataSource",
                      url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                      user = "dbuser1",
                      password = "dbpwd1")
@DataSourceDefinition(name = "java:module/jdbc/H2MySQLDataSource",
                      className = "org.h2.jdbcx.JdbcDataSource",
                      url = "jdbc:h2:mem:testdb_mysql;DB_CLOSE_DELAY=-1;MODE=MySQL",
                      user = "dbuser1",
                      password = "dbpwd1")
@DataSourceDefinition(name = "java:module/jdbc/H2PostgreSQLDataSource",
                      className = "org.h2.jdbcx.JdbcDataSource",
                      url = "jdbc:h2:mem:testdb_pgsql;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                      user = "dbuser1",
                      password = "dbpwd1")
@DataSourceDefinition(name = "java:module/jdbc/H2RunScriptDataSource",
                      className = "org.h2.jdbcx.JdbcDataSource",
                      url = "jdbc:h2:mem:testdb_runscript;DB_CLOSE_DELAY=-1",
                      user = "dbuser1",
                      password = "dbpwd1")
@DataSourceDefinition(name = "java:module/jdbc/H2SessionDataSource",
                      className = "org.h2.jdbcx.JdbcDataSource",
                      url = "jdbc:h2:mem:testdb_session;DB_CLOSE_DELAY=-1",
                      user = "dbuser1",
                      password = "dbpwd1")
@DataSourceDefinition(name = "java:module/jdbc/H2ListenerDataSource",
                      className = "org.h2.jdbcx.JdbcDataSource",
                      url = "jdbc:h2:mem:testdb_listener;DB_CLOSE_DELAY=-1;DATABASE_EVENT_LISTENER='test.jdbc.h2.web.H2TestServlet$TestDatabaseEventListener'",
                      user = "dbuser1",
                      password = "dbpwd1")
@SuppressWarnings("serial")
@WebServlet("/*")
public class H2TestServlet extends FATServlet {
    @Resource(lookup = "java:app/jdbc/H2JdbcDataSource")
    DataSource h2initDataSource;

    @Resource(lookup = "java:app/jdbc/H2ConnectionPoolDataSource")
    DataSource h2cpDataSource;

    @Resource(lookup = "java:comp/jdbc/H2DataSource")
    DataSource h2DataSource;

    @Resource(lookup = "java:global/jdbc/H2Driver")
    DataSource h2driverDataSource;

    @Resource(lookup = "java:module/jdbc/H2XADataSource",
              shareable = false)
    DataSource h2xaDataSource;

    @Resource(lookup = "java:module/jdbc/H2MySQLDataSource")
    DataSource h2mysqlDataSource;

    @Resource(lookup = "java:module/jdbc/H2PostgreSQLDataSource")
    DataSource h2pgsqlDataSource;

    @Resource(lookup = "java:module/jdbc/H2RunScriptDataSource")
    DataSource h2runscriptDataSource;

    @Resource(lookup = "java:module/jdbc/H2SessionDataSource",
              shareable = false)
    DataSource h2sessionDataSource;

    @Resource(lookup = "java:module/jdbc/H2ListenerDataSource")
    DataSource h2listenerDataSource;

    @Resource
    UserTransaction tx;

    private static final int TRANSACTION_SNAPSHOT = 6; // H2-specific, org.h2.engine.Constants.TRANSACTION_SNAPSHOT

    public static class TestDatabaseEventListener implements org.h2.api.DatabaseEventListener {
        public static volatile String initUrl;
        public static volatile boolean openedCalled;
        public static volatile boolean closingDatabaseCalled;
        public static volatile boolean setProgressCalled;
        public static volatile boolean exceptionThrownCalled;
        public static volatile String lastExceptionSql;

        public static void reset() {
            initUrl = null;
            openedCalled = false;
            closingDatabaseCalled = false;
            setProgressCalled = false;
            exceptionThrownCalled = false;
            lastExceptionSql = null;
        }

        public TestDatabaseEventListener() {
        }

        @Override
        public void init(String url) {
            initUrl = url;
        }

        @Override
        public void opened() {
            openedCalled = true;
        }

        @Override
        public void closingDatabase() {
            closingDatabaseCalled = true;
        }

        @Override
        public void setProgress(int state, String name, long current, long max) {
            setProgressCalled = true;
        }

        @Override
        public void exceptionThrown(SQLException e, String sql) {
            exceptionThrownCalled = true;
            lastExceptionSql = sql;
        }
    }

    public static int tripleInt(int n) {
        return n * 3;
    }

    static record Compound(
                    String formula,
                    String name,
                    int calcium,
                    int carbon,
                    int chlorine,
                    int fluorine,
                    int hydrogen,
                    int nitrogen,
                    int oxygen,
                    int sodium,
                    int sulfur) {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try (Connection con = h2initDataSource.getConnection()) {
            Statement stmt = con.createStatement();
            stmt.execute("""
                            CREATE TABLE COMPOUNDS (
                                FORMULA VARCHAR(50) NOT NULL PRIMARY KEY,
                                NAME VARCHAR(100) NOT NULL,
                                CALCIUM SMALLINT CHECK (CALCIUM >= 0),
                                CARBON SMALLINT CHECK (CARBON >= 0),
                                CHLORINE SMALLINT CHECK (CHLORINE >= 0),
                                FLUORINE SMALLINT CHECK (FLUORINE >= 0),
                                HYDROGEN SMALLINT CHECK (HYDROGEN >= 0),
                                NITROGEN SMALLINT CHECK (NITROGEN >= 0),
                                OXYGEN SMALLINT CHECK (OXYGEN >= 0),
                                SODIUM SMALLINT CHECK (SODIUM >= 0),
                                SULFUR SMALLINT CHECK (SULFUR >= 0)
                            )
                            """);
            String addCompound = "INSERT INTO COMPOUNDS VALUES (?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(addCompound);
            //                      Ca C  Cl F  H  N  O  Na S
            List.of(new Compound("C3H8", "Propane", //
                            /*   */ 0, 3, 0, 0, 8, 0, 0, 0, 0),
                    new Compound("CH4", "Methane", //
                                    0, 1, 0, 0, 4, 0, 0, 0, 0),
                    new Compound("CO2", "Carbon dioxide", //
                                    0, 1, 0, 0, 0, 0, 2, 0, 0),
                    new Compound("CaCO3", "Calcium carbonate", //
                                    1, 1, 0, 0, 0, 0, 3, 0, 0),
                    new Compound("CaCl2", "Calcium chloride", //
                                    1, 0, 2, 0, 0, 0, 0, 0, 0),
                    new Compound("CaO", "Calcium oxide", //
                                    1, 0, 0, 0, 0, 0, 1, 0, 0),
                    new Compound("H2", "Hydrogen", //
                                    0, 0, 0, 0, 2, 0, 0, 0, 0),
                    new Compound("H2O", "Water", //
                                    0, 0, 0, 0, 2, 0, 1, 0, 0),
                    new Compound("H2O2", "Hydrogen peroxide", //
                                    0, 0, 0, 0, 2, 0, 2, 0, 0),
                    new Compound("H2S", "Hydrogen sulfide", //
                                    0, 0, 0, 0, 2, 0, 0, 0, 1),
                    new Compound("H2SO4", "Sulfuric acid", //
                                    0, 0, 0, 0, 2, 0, 4, 0, 1),
                    new Compound("HCl", "Hydrochloric acid", //
                                    0, 0, 1, 0, 1, 0, 0, 0, 0),
                    new Compound("HF", "Hydrofluoric acid", //
                                    0, 0, 0, 1, 1, 0, 0, 0, 0),
                    new Compound("N2H4", "Hydrazine", //
                                    0, 0, 0, 0, 4, 2, 0, 0, 0),
                    new Compound("NH3", "Ammonia", //
                                    0, 0, 0, 0, 3, 1, 0, 0, 0),
                    new Compound("NaCl", "Sodium chloride", //
                                    0, 0, 1, 0, 0, 0, 0, 1, 0),
                    new Compound("NaOH", "Sodium hydroxide", //
                                    0, 0, 0, 0, 1, 0, 1, 1, 0))
                            .forEach(c -> {
                                try {
                                    ps.setString(1, c.formula);
                                    ps.setString(2, c.name);
                                    ps.setInt(3, c.calcium);
                                    ps.setInt(4, c.carbon);
                                    ps.setInt(5, c.chlorine);
                                    ps.setInt(6, c.fluorine);
                                    ps.setInt(7, c.hydrogen);
                                    ps.setInt(8, c.nitrogen);
                                    ps.setInt(9, c.oxygen);
                                    ps.setInt(10, c.sodium);
                                    ps.setInt(11, c.sulfur);
                                    ps.addBatch();
                                } catch (SQLException x) {
                                    throw new RuntimeException(x);
                                }
                            });
            ps.executeBatch();
        } catch (SQLException x) {
            throw new ServletException(x.getMessage(), x);
        }
    }

    /**
     * Use a DataSource that accesses H2 via the
     * javax.sql.ConnectionPoolDataSource interface.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testConnectionPoolDataSource() throws Exception {
        try (Connection con = h2cpDataSource.getConnection()) {
            assertEquals(true, con.getAutoCommit());
            assertEquals("TESTDB", con.getCatalog());
            assertEquals("PUBLIC", con.getSchema());
            assertEquals(TRANSACTION_READ_COMMITTED,
                         con.getTransactionIsolation());

            DatabaseMetaData mdata = con.getMetaData();
            assertEquals("DBUSER1", mdata.getUserName());
            assertEquals("H2", mdata.getDatabaseProductName());
            assertEquals("H2 JDBC Driver", mdata.getDriverName());
            assertEquals(4, mdata.getJDBCMajorVersion());
            assertEquals(3, mdata.getJDBCMinorVersion());

            assertEquals(TRANSACTION_READ_COMMITTED,
                         mdata.getDefaultTransactionIsolation());
            assertEquals(false,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_NONE));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_READ_UNCOMMITTED));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_READ_COMMITTED));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_REPEATABLE_READ));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_SERIALIZABLE));

            assertEquals(true,
                         h2cpDataSource.isWrapperFor(ConnectionPoolDataSource.class));
            ConnectionPoolDataSource cpds = //
                            h2cpDataSource.unwrap(ConnectionPoolDataSource.class);

            // disallowed usage
            try {
                PooledConnection pcon = cpds.getPooledConnection();
                fail("Unsafe getPooledConnection operation on unwrapped" +
                     " instance should have been blocked. Instead: " + pcon);
            } catch (SQLFeatureNotSupportedException x) {
                if (x.getMessage() == null ||
                    !x.getMessage().contains("DSRA9130E"))
                    throw x;
            }

            // valid usage
            PreparedStatement pstmt = con.prepareStatement("""
                            SELECT OXYGEN
                              FROM COMPOUNDS
                             WHERE FORMULA=?
                            """);
            pstmt.setString(1, "CO2");
            ResultSet result = pstmt.executeQuery();
            assertEquals(true, result.next());
            assertEquals(2, result.getInt(1));
            assertEquals(false, result.next());
        }
    }

    /**
     * Use a DataSource that accesses H2 via the
     * javax.sql.DataSource interface.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testDataSource() throws Exception {
        try (Connection con = h2DataSource.getConnection()) {
            assertEquals(true, con.getAutoCommit());
            assertEquals("TESTDB", con.getCatalog());
            assertEquals("PUBLIC", con.getSchema());
            assertEquals(TRANSACTION_READ_COMMITTED,
                         con.getTransactionIsolation());

            DatabaseMetaData mdata = con.getMetaData();
            assertEquals("DBUSER1", mdata.getUserName());
            assertEquals("H2", mdata.getDatabaseProductName());
            assertEquals("H2 JDBC Driver", mdata.getDriverName());
            assertEquals(4, mdata.getJDBCMajorVersion());
            assertEquals(3, mdata.getJDBCMinorVersion());

            assertEquals(TRANSACTION_READ_COMMITTED,
                         mdata.getDefaultTransactionIsolation());
            assertEquals(false,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_NONE));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_READ_UNCOMMITTED));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_READ_COMMITTED));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_REPEATABLE_READ));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_SERIALIZABLE));

            assertEquals(true,
                         h2DataSource.isWrapperFor(Referenceable.class));
            Referenceable r = h2DataSource.unwrap(Referenceable.class);
            r.getReference();

            // valid usage
            PreparedStatement pstmt = con.prepareStatement("""
                            SELECT COUNT(*)
                              FROM COMPOUNDS
                             WHERE HYDROGEN > ?
                            """);
            pstmt.setInt(1, 2);
            ResultSet result = pstmt.executeQuery();
            assertEquals(true, result.next());
            assertEquals(4, result.getInt(1));
            assertEquals(false, result.next());
        }
    }

    /**
     * Use a DataSource that accesses H2 via the
     * java.sql.Driver interface.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testDriver() throws Exception {
        try (Connection con = h2driverDataSource.getConnection()) {
            assertEquals(true, con.getAutoCommit());
            assertEquals("TESTDB", con.getCatalog());
            assertEquals("PUBLIC", con.getSchema());
            assertEquals(TRANSACTION_READ_COMMITTED,
                         con.getTransactionIsolation());

            DatabaseMetaData mdata = con.getMetaData();
            assertEquals("DBUSER1", mdata.getUserName());
            assertEquals("H2", mdata.getDatabaseProductName());
            assertEquals("H2 JDBC Driver", mdata.getDriverName());
            assertEquals(4, mdata.getJDBCMajorVersion());
            assertEquals(3, mdata.getJDBCMinorVersion());

            assertEquals(TRANSACTION_READ_COMMITTED,
                         mdata.getDefaultTransactionIsolation());
            assertEquals(false,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_NONE));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_READ_UNCOMMITTED));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_READ_COMMITTED));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_REPEATABLE_READ));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_SERIALIZABLE));

            PreparedStatement pstmt = con.prepareStatement("""
                            SELECT FORMULA
                              FROM COMPOUNDS
                             WHERE HYDROGEN=?
                             ORDER BY FORMULA DESC
                            """);
            pstmt.setInt(1, 4);
            ResultSet result = pstmt.executeQuery();
            assertEquals(true, result.next());
            assertEquals("N2H4", result.getString(1));
            assertEquals(true, result.next());
            assertEquals("CH4", result.getString(1));
            assertEquals(false, result.next());
        }
    }

    /**
     * Use a DataSource that accesses H2 via the
     * javax.sql.XADataSource interface.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testXADataSource() throws Exception {
        try (Connection con = h2xaDataSource.getConnection()) {
            assertEquals(true, con.getAutoCommit());
            assertEquals("TESTDB", con.getCatalog());
            assertEquals("PUBLIC", con.getSchema());
            assertEquals(TRANSACTION_READ_COMMITTED,
                         con.getTransactionIsolation());

            DatabaseMetaData mdata = con.getMetaData();
            assertEquals("DBUSER1", mdata.getUserName());
            assertEquals("H2", mdata.getDatabaseProductName());
            assertEquals("H2 JDBC Driver", mdata.getDriverName());
            assertEquals(4, mdata.getJDBCMajorVersion());
            assertEquals(3, mdata.getJDBCMinorVersion());

            assertEquals(TRANSACTION_READ_COMMITTED,
                         mdata.getDefaultTransactionIsolation());
            assertEquals(false,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_NONE));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_READ_UNCOMMITTED));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_READ_COMMITTED));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_REPEATABLE_READ));
            assertEquals(true,
                         mdata.supportsTransactionIsolationLevel(TRANSACTION_SERIALIZABLE));

            assertEquals(true,
                         h2xaDataSource.isWrapperFor(XADataSource.class));
            XADataSource xads = h2xaDataSource.unwrap(XADataSource.class);

            // disallowed usage
            try {
                XAConnection xacon = xads.getXAConnection();
                fail("Unsafe getXAConnection operation on unwrapped" +
                     " instance should have been blocked. Instead: " + xacon);
            } catch (SQLFeatureNotSupportedException x) {
                if (x.getMessage() == null ||
                    !x.getMessage().contains("DSRA9130E"))
                    throw x;
            }

            // valid usage
            boolean successful = false;
            tx.begin();
            try (Connection con2 = h2cpDataSource.getConnection()) {
                PreparedStatement pstmt = con.prepareStatement("""
                                UPDATE COMPOUNDS
                                   SET NAME=?
                                 WHERE FORMULA=?
                                                """);
                pstmt.setString(1, "Dihydrogen monoxide");
                pstmt.setString(2, "H2O");
                assertEquals(1, pstmt.executeUpdate());

                ResultSet result = con2
                                .createStatement(ResultSet.TYPE_FORWARD_ONLY,
                                                 ResultSet.CONCUR_UPDATABLE)
                                .executeQuery("""
                                                SELECT *
                                                  FROM COMPOUNDS
                                                 WHERE FORMULA='NaCl'
                                                   FOR UPDATE WAIT 30
                                                """);
                assertEquals(true, result.next());
                assertEquals("NaCl", result.getString(1));
                assertEquals("Sodium chloride", result.getString(2));
                assertEquals(1, result.getInt(10)); // Na
                assertEquals(1, result.getInt(5)); // Cl
                result.updateString(2, "Salt");
                result.updateRow();
                assertEquals(false, result.next());
                successful = true;
            } finally {
                if (successful)
                    tx.commit();
                else
                    tx.rollback();
            }

            // restore values that were updated by the transaction
            con.setAutoCommit(false);
            PreparedStatement pstmt = con.prepareStatement("""
                            UPDATE COMPOUNDS
                               SET NAME=?
                             WHERE NAME=?
                                            """);
            pstmt.setString(1, "Water");
            pstmt.setString(2, "Dihydrogen monoxide");
            assertEquals(1, pstmt.executeUpdate());
            con.commit();

            con.setAutoCommit(true);
            pstmt.setString(1, "Sodium chloride");
            pstmt.setString(2, "Salt");
            assertEquals(1, pstmt.executeUpdate());
        }
    }

    /**
     * Test that a datasource with password attribute (not in URL) works correctly.
     */
    public void testValidPasswordAttribute() throws Exception {
        DataSource ds = (DataSource) new InitialContext().lookup("jdbc/h2ds-valid-password");
        assertNotNull("DataSource should be successfully created", ds);
    }

    /**
     * Test that a datasource with containerAuthData (not in URL) works correctly.
     */
    public void testValidContainerAuthData() throws Exception {
        DataSource ds = (DataSource) new InitialContext().lookup("jdbc/h2ds-valid-authdata");
        assertNotNull("DataSource should be successfully created", ds);
    }

    /**
     * Test that a datasource with PASSWORD in URL (uppercase) is rejected.
     * The JNDI lookup should fail. The DSRA8070E error will be in the server logs.
     */
    public void testPasswordInURL() throws Exception {
        try {
            DataSource ds = (DataSource) new InitialContext().lookup("jdbc/h2ds-invalid-password");
            fail("Expected exception for PASSWORD in URL, but datasource was created: " + ds);
        } catch (NamingException e) {
            String msg = e.getMessage();
            assertTrue("Expected CWWKN0008E in exception message but got: " + msg, msg.contains("CWWKN0008E"));
            assertTrue("Expected datasource name 'jdbc/h2ds-invalid-password' in exception message but got: " + msg, msg.contains("jdbc/h2ds-invalid-password"));
        }
    }

    /**
     * Test that a datasource with password in URL (any case) is rejected,
     * even when mixed with other parameters.
     * The JNDI lookup should fail. The DSRA8070E error will be in the server logs.
     */
    public void testPasswordInURLMixedLowerCase() throws Exception {
        try {
            DataSource ds = (DataSource) new InitialContext().lookup("jdbc/h2ds-invalid-password-mixed");
            fail("Expected exception for password in URL, but datasource was created: " + ds);
        } catch (NamingException e) {
            String msg = e.getMessage();
            assertTrue("Expected CWWKN0008E in exception message but got: " + msg, msg.contains("CWWKN0008E"));
            assertTrue("Expected datasource name 'jdbc/h2ds-invalid-password-mixed' in exception message but got: " + msg, msg.contains("jdbc/h2ds-invalid-password-mixed"));
        }
    }

    /**
     * Test H2's SNAPSHOT transaction isolation level using concurrent connections.
     *
     * Connection.setTransactionIsolation(6) is used because H2's SNAPSHOT constant
     * value is 6 (org.h2.engine.Constants.TRANSACTION_SNAPSHOT), which is not part
     * of the standard JDBC API. getTransactionIsolation() returns 6 through Liberty
     * to confirm the level was accepted, and INFORMATION_SCHEMA.SESSIONS.ISOLATION_LEVEL
     * is queried to verify the string "SNAPSHOT".
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testSnapshotIsolation() throws Exception {
        try (Connection con = h2initDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("CREATE TABLE SNAPSHOT_TEST (ID INT PRIMARY KEY, VAL VARCHAR(50))");
            stmt.execute("INSERT INTO SNAPSHOT_TEST VALUES (1, 'initial')");
        }

        try (Connection conA = h2xaDataSource.getConnection()) {
            conA.setAutoCommit(false);
            conA.setTransactionIsolation(TRANSACTION_SNAPSHOT);
            assertEquals(TRANSACTION_SNAPSHOT, conA.getTransactionIsolation());

            // Initial read on connection A
            try (Statement stmtA = conA.createStatement();
                 ResultSet rs = stmtA.executeQuery("SELECT COUNT(*), VAL FROM SNAPSHOT_TEST WHERE ID=1 GROUP BY VAL")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
                assertEquals("initial", rs.getString(2));
            }

            // Connection B inserts and updates rows, then commits in a separate transaction
            try (Connection conB = h2cpDataSource.getConnection()) {
                conB.setAutoCommit(false);
                try {
                    try (Statement stmtB = conB.createStatement()) {
                        stmtB.executeUpdate("INSERT INTO SNAPSHOT_TEST VALUES (2, 'concurrent')");
                        stmtB.executeUpdate("UPDATE SNAPSHOT_TEST SET VAL = 'modified' WHERE ID = 1");
                    }
                    conB.commit();
                } finally {
                    conB.setAutoCommit(true);
                }
            }

            // Reader connection A keeps seeing its original snapshot without blocking
            try (Statement stmtA = conA.createStatement()) {
                try (ResultSet rs = stmtA.executeQuery("SELECT COUNT(*) FROM SNAPSHOT_TEST")) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
                try (ResultSet rs = stmtA.executeQuery("SELECT VAL FROM SNAPSHOT_TEST WHERE ID=1")) {
                    assertTrue(rs.next());
                    assertEquals("initial", rs.getString(1));
                }
                try (ResultSet rs = stmtA.executeQuery("SELECT ISOLATION_LEVEL FROM INFORMATION_SCHEMA.SESSIONS WHERE SESSION_ID = SESSION_ID()")) {
                    assertTrue(rs.next());
                    assertEquals("SNAPSHOT", rs.getString(1));
                }
            }

            // Commit connection A transaction and restore isolation level and autocommit
            conA.commit();
            conA.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            conA.setAutoCommit(true);

            // New read after commit sees the committed changes
            try (Statement stmtA = conA.createStatement()) {
                try (ResultSet rs = stmtA.executeQuery("SELECT COUNT(*) FROM SNAPSHOT_TEST")) {
                    assertTrue(rs.next());
                    assertEquals(2, rs.getInt(1));
                }
                try (ResultSet rs = stmtA.executeQuery("SELECT VAL FROM SNAPSHOT_TEST WHERE ID=1")) {
                    assertTrue(rs.next());
                    assertEquals("modified", rs.getString(1));
                }
            }
        }
    }

    /**
     * Test H2's MERGE INTO ... KEY (...) upsert syntax.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testMergeIntoKeyUpsert() throws Exception {
        try (Connection con = h2DataSource.getConnection();
            Statement stmt = con.createStatement()) {
            stmt.execute("CREATE TABLE MERGE_TEST (ID INT PRIMARY KEY, LABEL VARCHAR(50))");
            stmt.execute("INSERT INTO MERGE_TEST VALUES (1, 'original')");

            int updated = stmt.executeUpdate("MERGE INTO MERGE_TEST (ID, LABEL) KEY (ID) VALUES (1, 'updated'), (2, 'inserted')");
            assertEquals(2, updated);

            try (ResultSet rs = stmt.executeQuery("SELECT ID, LABEL FROM MERGE_TEST ORDER BY ID")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
                assertEquals("updated", rs.getString(2));

                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
                assertEquals("inserted", rs.getString(2));

                assertEquals(false, rs.next());
            }
        }
    }

    /**
     * Test H2's SYSTEM_RANGE table function.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testSystemRange() throws Exception {
        try (Connection con = h2DataSource.getConnection();
             Statement stmt = con.createStatement()) {
            // (a) COUNT over range 1 to 10
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM SYSTEM_RANGE(1, 10)")) {
                assertTrue(rs.next());
                assertEquals(10, rs.getInt(1));
            }

            // (b) SUM over range 1 to 5
            try (ResultSet rs = stmt.executeQuery("SELECT SUM(X) FROM SYSTEM_RANGE(1, 5)")) {
                assertTrue(rs.next());
                assertEquals(15L, rs.getLong(1));
            }

            // (c) Ordered selection over range 1 to 3
            List<Long> values = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery("SELECT X FROM SYSTEM_RANGE(1, 3) ORDER BY X")) {
                while (rs.next()) {
                    values.add(rs.getLong(1));
                }
            }
            assertEquals(List.of(1L, 2L, 3L), values);

            // (d) Join SYSTEM_RANGE against COMPOUNDS
            // Returns the number of compounds with 1-3 Hydrogens
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM COMPOUNDS c JOIN SYSTEM_RANGE(1, 3) r ON c.HYDROGEN = r.X")) {
                assertTrue(rs.next());
                assertEquals(9, rs.getInt(1));
            }
        }
    }

    /**
     * Test H2's CREATE ALIAS function backed by a Java static method.
     *
     * FOR 'test.jdbc.h2.web.H2TestServlet.tripleInt' is used because H2's
     * JdbcUtils.loadUserClass falls back to the thread context classloader
     * (the application classloader in Liberty), making the WAR's own classes resolvable.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testCreateAlias() throws Exception {
        try (Connection con = h2DataSource.getConnection();
            Statement stmt = con.createStatement()) {
            
            stmt.execute("CREATE ALIAS IF NOT EXISTS TRIPLE_INT FOR 'test.jdbc.h2.web.H2TestServlet.tripleInt'");
            try (ResultSet rs = stmt.executeQuery("SELECT TRIPLE_INT(7)")) {
                assertTrue(rs.next());
                assertEquals(21, rs.getInt(1));
                assertEquals(false, rs.next());
            }
        }
    }

    /**
     * Test H2 MySQL compatibility mode (MODE=MySQL).
     *
     * Verifies that MySQL-specific syntax such as ON DUPLICATE KEY UPDATE
     * is accepted and executed properly when MODE=MySQL is configured.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testCompatibilityModeMySQL() throws Exception {
        try (Connection con = h2mysqlDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS MYSQL_MODE_TEST");
            stmt.execute("CREATE TABLE MYSQL_MODE_TEST (ID INT PRIMARY KEY, VAL VARCHAR(50))");
            stmt.execute("INSERT INTO MYSQL_MODE_TEST VALUES (1, 'initial')");

            int updated = stmt.executeUpdate(
                "INSERT INTO MYSQL_MODE_TEST (ID, VAL) VALUES (1, 'updated') ON DUPLICATE KEY UPDATE VAL = 'updated'");
            assertTrue("Update count should be positive", updated > 0);

            try (ResultSet rs = stmt.executeQuery("SELECT VAL FROM MYSQL_MODE_TEST WHERE ID = 1")) {
                assertTrue(rs.next());
                assertEquals("updated", rs.getString(1));
                assertEquals(false, rs.next());
            }
        }
    }

    /**
     * Test H2 PostgreSQL compatibility mode (MODE=PostgreSQL).
     *
     * Verifies that the PostgreSQL-specific current_database() function is accepted
     * in PostgreSQL mode.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testCompatibilityModePostgreSQL() throws Exception {
        try (Connection con = h2pgsqlDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT current_database()")) {
                assertTrue(rs.next());
                assertEquals("TESTDB_PGSQL", rs.getString(1));
            }
        }
    }

    /**
     * Test H2's RUNSCRIPT FROM 'classpath:...' feature.
     *
     * H2 resolves classpath: resources through the thread context classloader
     * (which in Liberty is the application classloader), loading scripts packaged
     * in WEB-INF/classes.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testRunScriptFromClasspath() throws Exception {
        try (Connection con = h2runscriptDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS RUNSCRIPT_TEST");
            stmt.execute("RUNSCRIPT FROM 'classpath:h2_runscript_test.sql'");

            try (ResultSet rs = stmt.executeQuery("SELECT ID, NAME FROM RUNSCRIPT_TEST ORDER BY ID")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
                assertEquals("Alpha", rs.getString(2));

                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
                assertEquals("Beta", rs.getString(2));

                assertTrue(rs.next());
                assertEquals(3, rs.getInt(1));
                assertEquals("Gamma", rs.getString(2));
            }
        }
    }

    /**
     * Test H2 session variables (SET @var = ...).
     *
     * Verifies that session variables are scoped to the individual database session
     * and are not visible across distinct physical connections.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testSessionVariables() throws Exception {
        try (Connection conA = h2sessionDataSource.getConnection();
             Connection conB = h2sessionDataSource.getConnection()) {
            int sessionIdA = -1;
            int sessionIdB = -1;

            try (Statement stmtA = conA.createStatement();
                 ResultSet rsA = stmtA.executeQuery("SELECT SESSION_ID()")) {
                assertTrue(rsA.next());
                sessionIdA = rsA.getInt(1);
            }

            try (Statement stmtB = conB.createStatement();
                 ResultSet rsB = stmtB.executeQuery("SELECT SESSION_ID()")) {
                assertTrue(rsB.next());
                sessionIdB = rsB.getInt(1);
            }

            assertTrue("Connection A and B must have different session IDs", sessionIdA != sessionIdB);

            try {
                try (Statement stmtA = conA.createStatement()) {
                    stmtA.execute("SET @test_var = 'session_a_value'");

                    try (ResultSet rs = stmtA.executeQuery("SELECT @test_var")) {
                        assertTrue(rs.next());
                        assertEquals("session_a_value", rs.getString(1));
                    }
                }

                try (Statement stmtB = conB.createStatement()) {
                    try (ResultSet rs = stmtB.executeQuery("SELECT @test_var")) {
                        assertTrue(rs.next());
                        assertNull("Session variable should be NULL on connection B", rs.getString(1));
                    }

                    stmtB.execute("SET @test_var = 'session_b_value'");
                    try (ResultSet rs = stmtB.executeQuery("SELECT @test_var")) {
                        assertTrue(rs.next());
                        assertEquals("session_b_value", rs.getString(1));
                    }
                }

                // Verify connection A's variable is still unaffected
                try (Statement stmtA = conA.createStatement();
                     ResultSet rs = stmtA.executeQuery("SELECT @test_var")) {
                    assertTrue(rs.next());
                    assertEquals("session_a_value", rs.getString(1));
                }
            } finally {
                // Reset the variables so pooled physical connections do not carry state into other runs
                try (Statement stmtA = conA.createStatement();
                     Statement stmtB = conB.createStatement()) {
                    stmtA.execute("SET @test_var = NULL");
                    stmtB.execute("SET @test_var = NULL");
                }
            }
        }
    }

    /**
     * Test H2's DATABASE_EVENT_LISTENER URL setting.
     *
     * Verifies that H2 loads the custom DatabaseEventListener through the application
     * classloader and invokes init and opened callbacks.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testDatabaseEventListener() throws Exception {
        // Do not call reset() here: init() and opened() fire only once per database lifetime.
        // With DB_CLOSE_DELAY=-1 the in-memory database lives for the JVM lifetime, so
        // initUrl and openedCalled remain set from the first invocation; asserting on them
        // is always valid regardless of test ordering.

        try (Connection con = h2listenerDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT 1")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }

            assertNotNull("DatabaseEventListener.init() should have been called with non-null URL",
                          TestDatabaseEventListener.initUrl);
            assertTrue("DatabaseEventListener.init() URL should contain 'testdb_listener' but was: " + TestDatabaseEventListener.initUrl,
                       TestDatabaseEventListener.initUrl.contains("testdb_listener"));
            assertTrue("DatabaseEventListener.opened() should have been called",
                       TestDatabaseEventListener.openedCalled);

            // Trigger an exception via a runtime SQL error; the listener should record it.
            // A constraint violation (duplicate primary key) fails at execution time, not parse
            // time, so H2's Command layer fires exceptionThrown on the DatabaseEventListener.
            stmt.execute("DROP TABLE IF EXISTS LISTENER_DUPE_TEST");
            stmt.execute("CREATE TABLE LISTENER_DUPE_TEST (ID INT PRIMARY KEY)");
            stmt.execute("INSERT INTO LISTENER_DUPE_TEST VALUES (1)");
            TestDatabaseEventListener.exceptionThrownCalled = false;
            TestDatabaseEventListener.lastExceptionSql = null;
            String dupeSql = "INSERT INTO LISTENER_DUPE_TEST VALUES (1)";
            try {
                stmt.executeUpdate(dupeSql);
                fail("Expected SQLException for duplicate primary key");
            } catch (SQLException e) {
                // Expected
            }

            assertTrue("DatabaseEventListener.exceptionThrown() should have been called after duplicate-key insert",
                       TestDatabaseEventListener.exceptionThrownCalled);
            assertNotNull("DatabaseEventListener.exceptionThrown() should have recorded the SQL",
                          TestDatabaseEventListener.lastExceptionSql);
            assertTrue("exceptionThrown SQL should contain 'LISTENER_DUPE_TEST' but was: " + TestDatabaseEventListener.lastExceptionSql,
                       TestDatabaseEventListener.lastExceptionSql.contains("LISTENER_DUPE_TEST"));
        }
    }
}
