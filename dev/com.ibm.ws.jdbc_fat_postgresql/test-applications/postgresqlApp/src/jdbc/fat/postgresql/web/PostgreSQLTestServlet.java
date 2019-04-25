/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.postgresql.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/PostgreSQLTestServlet")
public class PostgreSQLTestServlet extends FATServlet {

    @Resource(lookup = "jdbc/anonymous/XADataSource")
    DataSource resRefDS;

    @Resource
    UserTransaction tx;

    // Test that we can obtain a Connection from a datasource that uses the generic <properties> element with PostgreSQL
    @Test
    public void testPostgresGenericProps() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/postgres/genericprops");
        ds.getConnection().close();
    }

    // Test that we can obtain a Connection from a datasource that uses the generic <properties> element with PostgreSQL
    // and a JDBC driver that does not match the jar name heuristic detection. This will confirm that our java.sql.Driver
    // detection mechanism works properly for PostgreSQL
    @Test
    public void testAnonymousPostgresDriver() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/anonymous/Driver");
        ds.getConnection().close();
    }

    // Verify we can auto-detect an XA DataSource implementation using a generically named PostgreSQL JDBC Driver
    @Test
    public void testAnonymousPostgresDS() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/anonymous/XADataSource");
        ds.getConnection().close();
    }

    // Verify that a DataSource using <properties.postgresql> works with the minimum possible configuration
    @Test
    public void testPostgresMinimal() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/postgres/minimal");
        ds.getConnection().close();
    }

    // Verify that we can configure a <properties.postgresql> element by specifying only the 'URL' property
    @Test
    public void testPostgresURLOnly() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/postgres/urlOnly");
        ds.getConnection().close();
    }

    // Verify that basic unwrap patterns work for the 3 DataSource types: reg, CP, and XA
    @Test
    public void testUnwrap() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/anonymous/XADataSource");
        assertTrue("Class " + ds.getClass() + " was not marked as a wrapper for XADataSource",
                   ds.isWrapperFor(XADataSource.class));
        // There isn't any PosgreSQL specific interface we can unwrap to,
        // so just make sure a basic unwrap is possible
        XADataSource unwrapedXA = ds.unwrap(XADataSource.class);
        assertTrue("Expected unwraped XADataSource to be an instance of XADataSource, but was: " + unwrapedXA,
                   unwrapedXA instanceof XADataSource);

        ds = InitialContext.doLookup("jdbc/postgres/ConnectionPoolDataSource");
        assertTrue("Class " + ds.getClass() + " was not marked as a wrapper for ConnectionPoolDataSource",
                   ds.isWrapperFor(ConnectionPoolDataSource.class));
        ConnectionPoolDataSource unwrapedCP = ds.unwrap(ConnectionPoolDataSource.class);
        assertTrue("Expected unwraped ConnectionPoolDataSource to be an instance of ConnectionPoolDataSource, but was: " + unwrapedCP,
                   unwrapedCP instanceof ConnectionPoolDataSource);

        ds = InitialContext.doLookup("jdbc/postgres/DataSource");
        ds.getConnection().close();
        assertTrue("Class " + ds.getClass() + " was not marked as a wrapper for DataSource",
                   ds.isWrapperFor(DataSource.class));
        DataSource unwrapedDS = ds.unwrap(DataSource.class);
        assertTrue("Expected unwraped DataSource to be an instance of DataSource, but was: " + unwrapedDS,
                   unwrapedDS instanceof DataSource);
    }

    // Test that a basic PostgreSQL-only bean property (defaultFetchSize) gets set on a DataSource when configured in server.xml
    @Test
    public void testBaiscPostgreSpecificProp() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/anonymous/XADataSource");

        // Insert 6 rows into the DB. Uses ID's 0, 1, 2, 3, 4, and 5
        try (Connection con = ds.getConnection()) {
            PreparedStatement pStmt = con.prepareStatement("INSERT INTO people(id,name) VALUES(?,?)");
            for (int i = 0; i < 6; i++) {
                pStmt.setInt(1, i);
                pStmt.setString(2, "person-" + i);
                pStmt.addBatch();
            }
            pStmt.executeBatch();
        }

        // Since defaultRowFetchSize=5, it should take 2 fetches to obtain the data
        try (Connection con = ds.getConnection()) {
            ResultSet rs = con.createStatement().executeQuery("SELECT id,name FROM people");
            assertEquals(5, rs.getFetchSize());
            while (rs.next()) {
                System.out.println("Got person: " + rs.getString("name"));
            }
        }
    }

    // Verify behavior of the defaultReadOnly setting on <properties.postgresql>
    @Test
    public void testReadOnly() throws Exception {
        // On a regular DS, should be able to write data
        DataSource regularDS = InitialContext.doLookup("jdbc/anonymous/XADataSource");
        try (Connection con = regularDS.getConnection()) {
            assertFalse("JDBC connection should not be marked read-only by default.", con.isReadOnly());
            Statement stmt = con.createStatement();
            stmt.execute("INSERT INTO people(id,name) VALUES(6,'testReadOnly1')");
            stmt.executeUpdate("INSERT INTO people(id,name) VALUES(7,'testReadOnly2')");
        }

        // On a read-only configured DS, should only be able to read data
        DataSource readOnlyDS = InitialContext.doLookup("jdbc/postgres/readOnly");
        try (Connection con = readOnlyDS.getConnection()) {
            assertTrue("DataSource was configured 'readOnly' but connection was not marked readOnly.", con.isReadOnly());

            Statement stmt = con.createStatement();
            try {
                stmt.execute("INSERT INTO people(id,name) VALUES(8,'testReadOnly1')");
                fail("Should not be able to write data in a read-only ds.");
            } catch (SQLException expected) {
                System.out.println("Got expected SQLException for trying to execute an insert in a read-only tran.");
            }
            try {
                stmt.executeUpdate("INSERT INTO people(id,name) VALUES(9,'testReadOnly')");
                fail("Should not be able to write data in a read-only ds.");
            } catch (SQLException expected) {
                System.out.println("Got expected SQLException for trying to execute an insert in a read-only tran.");
            }
        }
    }

    // Verify that the defaultAutoCommit setting defaults to true, and when it is set to false
    // connections obtained from these DataSources have autoCommit=false when initially obtained
    @Test
    public void testDefaultAutoCommit() throws Exception {
        // On a regular DS, default AC should be true in an LTC, or false in a global tran
        DataSource writingDS = InitialContext.doLookup("jdbc/anonymous/XADataSource");
        DataSource regularDS = InitialContext.doLookup("jdbc/postgres/ConnectionPoolDataSource");

        try (Connection writingConn = writingDS.getConnection();
                        Connection readingConn = regularDS.getConnection()) {
            assertTrue("JDBC connection should have auto-commit true in an LTC", writingConn.getAutoCommit());
            assertTrue("JDBC connection should have auto-commit true in an LTC", readingConn.getAutoCommit());

            // Insert and select some data to confirm the DB behaves as auto-commit=true
            Statement insert = writingConn.createStatement();
            insert.execute("INSERT INTO people(id,name) VALUES(10,'testDefaultAutoCommit')");

            Statement query = readingConn.createStatement();
            ResultSet rs = query.executeQuery("SELECT * FROM people WHERE id=10");
            assertTrue("Did not find expected row in database when autoCommit=true in a separate connection", rs.next());
            assertEquals("testDefaultAutoCommit", rs.getString(2));
        }

        DataSource autoCommitDS = InitialContext.doLookup("jdbc/postgres/defaultAutoCommit");
        try (Connection writingConn = autoCommitDS.getConnection();
                        Connection readingConn = regularDS.getConnection()) {
            assertTrue("JDBC connection should have auto-commit true in an LTC", writingConn.getAutoCommit());
            assertTrue("JDBC connection should have auto-commit true in an LTC", readingConn.getAutoCommit());

            // Insert and select some data to confirm the DB behaves as auto-commit=true
            Statement insert = writingConn.createStatement();
            insert.execute("INSERT INTO people(id,name) VALUES(11,'testDefaultAutoCommit')");

            Statement query = readingConn.createStatement();
            ResultSet rs = query.executeQuery("SELECT * FROM people WHERE id=11");
            assertTrue("Did not find expected row in database when autoCommit=true in a separate connection", rs.next());
            assertEquals("testDefaultAutoCommit", rs.getString(2));
        }

        DataSource autoCommitOffDS = InitialContext.doLookup("jdbc/postgres/defaultAutoCommitOff");
        try (Connection writingConn = autoCommitOffDS.getConnection();
                        Connection readingConn = regularDS.getConnection()) {
            assertFalse("JDBC connection should have auto-commit false when defaultAutoCommit=false is set on the DataSource properties in server.xml",
                        writingConn.getAutoCommit());
            assertTrue("JDBC connection should have auto-commit true in an LTC", readingConn.getAutoCommit());

            // Insert and select some data to confirm the DB behaves as auto-commit=true
            Statement insert = writingConn.createStatement();
            insert.execute("INSERT INTO people(id,name) VALUES(12,'testDefaultAutoCommit')");

            Statement query = readingConn.createStatement();
            ResultSet rs = query.executeQuery("SELECT * FROM people WHERE id=12");
            assertFalse("Data should not be auto-committed to the DB when defaultAutoCommit=false is set on the DataSource properties in server.xml", rs.next());
            writingConn.rollback();
        }
    }

    // Ensure defaultAutoCommit=false behaves properly across global transaction boundaries.
    // Insert/read data with two different DataSources, expect writes to auto-commit
    @Test
    public void testDefaultAutoCommitOffGlobalTran() throws Exception {
        DataSource regularDS = InitialContext.doLookup("jdbc/postgres/xa");
        DataSource autoCommitDS = InitialContext.doLookup("jdbc/postgres/defaultAutoCommitOff");

        // Get a new connection, intentionally leave it open over a transaction boundary
        Connection boundaryPassingConnection = autoCommitDS.getConnection();

        tx.begin();
        try (Connection readingConn = regularDS.getConnection()) {
            // Insert and select some data to confirm the DB behaves as auto-commit=false
            Statement insert = boundaryPassingConnection.createStatement();
            insert.execute("INSERT INTO people(id,name) VALUES(15,'testDefaultAutoCommitOffGlobalTran')");

            Statement query = readingConn.createStatement();
            ResultSet rs = query.executeQuery("SELECT * FROM people WHERE id=15");
            assertFalse("Should not find uncommitted row in database during global transaction", rs.next());
        }
        tx.commit();

        // After global tran is committed, row should be readable in DB
        try (Connection readingConn = regularDS.getConnection()) {
            Statement query = readingConn.createStatement();
            ResultSet rs = query.executeQuery("SELECT * FROM people WHERE id=15");
            assertTrue("Did not find expected row in database after global transaction was committed", rs.next());
            assertEquals("testDefaultAutoCommitOffGlobalTran", rs.getString(2));
        }

        // Connection obtained before tx scope should have same autoCommit value
        assertFalse("DataSource with autoCommit=false set in server.xml should have autoCommit=false", boundaryPassingConnection.getAutoCommit());
        Statement boundaryPassingInsert = boundaryPassingConnection.createStatement();
        boundaryPassingInsert.execute("INSERT INTO people(id,name) VALUES(16,'testDefaultAutoCommitOffGlobalTran')");
        try (Connection readingConn = regularDS.getConnection()) {
            Statement query = readingConn.createStatement();
            ResultSet rs = query.executeQuery("SELECT * FROM people WHERE id=16");
            assertFalse("Should not find uncommitted row in database during global transaction", rs.next());

            boundaryPassingConnection.commit();
            rs = query.executeQuery("SELECT * FROM people WHERE id=16");
            assertTrue("Did not find expected row in database after issuing a local Connection.commit()", rs.next());
            assertEquals("testDefaultAutoCommitOffGlobalTran", rs.getString(2));
        }
        boundaryPassingConnection.close();
    }

    @Test
    public void testPropertyCleanup_readOnly() throws Exception {
        // Use a DataSource with maxPoolSize=1 and shareable=false so that we always reuse the same underlying connection (if possible)
        DataSource ds = InitialContext.doLookup("jdbc/postgres/maxPoolSize1");

        // Verify readOnly can be set and is reset on new connection
        try (Connection con = ds.getConnection()) {
            verifyClean(con);
            con.setReadOnly(true);
            assertTrue(con.isReadOnly());
            assertEquals(con.isReadOnly(), getUnderlyingPGConnection(con).isReadOnly());
        }
        try (Connection con = ds.getConnection()) {
            verifyClean(con);
        }
    }

    // Verify autoCommit can be set and is reset on new connection
    @Test
    public void testPropertyCleanup_autoCommit() throws Exception {
        // Use a DataSource with maxPoolSize=1 and shareable=false so that we always reuse the same underlying connection (if possible)
        DataSource ds = InitialContext.doLookup("jdbc/postgres/maxPoolSize1");

        try (Connection con = ds.getConnection()) {
            verifyClean(con);
            con.setAutoCommit(false);
            assertFalse(con.getAutoCommit());
            assertEquals(con.getAutoCommit(), getUnderlyingPGConnection(con).getAutoCommit());
            con.commit();
        }
        try (Connection con = ds.getConnection()) {
            verifyClean(con);
        }
    }

    // Verify transaction isolation level can be set and is reset on new connection
    @Test
    public void testPropertyCleanup_transactionIsolation() throws Exception {
        // Use a DataSource with maxPoolSize=1 and shareable=false so that we always reuse the same underlying connection (if possible)
        DataSource ds = InitialContext.doLookup("jdbc/postgres/maxPoolSize1");

        try (Connection con = ds.getConnection()) {
            verifyClean(con);
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            assertEquals(Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());
            assertEquals(con.getTransactionIsolation(), getUnderlyingPGConnection(con).getTransactionIsolation());
        }
        try (Connection con = ds.getConnection()) {
            verifyClean(con);
        }
    }

    // Verify ResultSet holdability can be set and is reset on new connection
    @Test
    public void testPropertyCleanup_holdability() throws Exception {
        // Use a DataSource with maxPoolSize=1 and shareable=false so that we always reuse the same underlying connection (if possible)
        DataSource ds = InitialContext.doLookup("jdbc/postgres/maxPoolSize1");

        try (Connection con = ds.getConnection()) {
            verifyClean(con);
            con.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
            assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, con.getHoldability());
            assertEquals(con.getHoldability(), getUnderlyingPGConnection(con).getHoldability());
        }
        try (Connection con = ds.getConnection()) {
            verifyClean(con);
        }
    }

    // Verify network timeout can be set and is reset on new connection
    @Test
    public void testPropertyCleanup_networkTimeout() throws Exception {
        // Use a DataSource with maxPoolSize=1 and shareable=false so that we always reuse the same underlying connection (if possible)
        DataSource ds = InitialContext.doLookup("jdbc/postgres/maxPoolSize1");

        Executor exec = Executors.newSingleThreadExecutor();
        try (Connection con = ds.getConnection()) {
            verifyClean(con);
            con.setNetworkTimeout(exec, 5000);
            assertEquals(5000, con.getNetworkTimeout());
            assertEquals(con.getNetworkTimeout(), getUnderlyingPGConnection(con).getNetworkTimeout());
        }
        try (Connection con = ds.getConnection()) {
            verifyClean(con);
        }
    }

    // Verify schema can be set and is reset on new connection
    @Test
    public void testPropertyCleanup_schema() throws Exception {
        // Use a DataSource with maxPoolSize=1 and shareable=false so that we always reuse the same underlying connection (if possible)
        DataSource ds = InitialContext.doLookup("jdbc/postgres/maxPoolSize1");

        try (Connection con = ds.getConnection()) {
            verifyClean(con);
            con.createStatement().execute("CREATE SCHEMA fooschema");
            con.setSchema("fooschema");
            assertEquals("fooschema", con.getSchema());
            assertEquals(con.getSchema(), getUnderlyingPGConnection(con).getSchema());
        }
        try (Connection con = ds.getConnection()) {
            verifyClean(con);
        }
    }

    // Verifies spec-standard JDBC properties are at their default values originally
    // and that our WSJdbcConnection properties are in sync with the underlying PostgreSQL connection's properties
    private void verifyClean(Connection con) throws Exception {
        // Always "do some work" with the connection before we verify it's underlying state.
        // The JDBC code intentionally lazily resets connection values, so our wrapper may be out of sync with the underlying
        // connection between getting the initial connection and actually driving some work on it
        con.createStatement().close();

        // Verify WSJdbcConnection values are all at the proper initial state
        assertTrue("Default auto-commit value on a connection should be 'true'", con.getAutoCommit());
        assertFalse("Default readOnly value on a connection should be 'false'", con.isReadOnly());
        assertEquals("Default tx isolation level on a connection should be TRANSACTION_READ_COMMITTED (2)", Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
        assertEquals("Default ResultSet holdability on a connection should be CLOSE_CURSORS_AT_COMMIT (2)", ResultSet.CLOSE_CURSORS_AT_COMMIT, con.getHoldability());
        assertEquals("Default network timeout on a connection should be 0", 0, con.getNetworkTimeout());
        assertEquals("Default schema on a connection should be 'public", "public", con.getSchema());

        // Verify the underlying PostgreSQL connection is in a consistent state with our tracking
        assertEquals("Liberty JDBC connection wrapper auto-commit value did not match the underlying PostgreSQL connection value",
                     con.getAutoCommit(), getUnderlyingPGConnection(con).getAutoCommit());
        assertEquals("Liberty JDBC connection wrapper isReadOnly value did not match the underlying PostgreSQL connection value",
                     con.isReadOnly(), getUnderlyingPGConnection(con).isReadOnly());
        assertEquals("Liberty JDBC connection wrapper tx isolation level value did not match the underlying PostgreSQL connection value",
                     con.getTransactionIsolation(), getUnderlyingPGConnection(con).getTransactionIsolation());
        assertEquals("Liberty JDBC connection wrapper ResultSet holdability value did not match the underlying PostgreSQL connection value",
                     con.getHoldability(), getUnderlyingPGConnection(con).getHoldability());
        assertEquals("Liberty JDBC connection wrapper network timeout value did not match the underlying PostgreSQL connection value",
                     con.getNetworkTimeout(), getUnderlyingPGConnection(con).getNetworkTimeout());
        assertEquals("Liberty JDBC connection wrapper schema value did not match the underlying PostgreSQL connection value",
                     con.getSchema(), getUnderlyingPGConnection(con).getSchema());
    }

    private Connection getUnderlyingPGConnection(Connection wsJdbcConnection) throws Exception {
        Field connImplField = null;
        for (Class<?> clazz = wsJdbcConnection.getClass(); connImplField == null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                connImplField = clazz.getDeclaredField("connImpl");
            } catch (Exception ignore) {
            }
        }
        if (connImplField == null)
            fail("Did not find 'connImpl' field on " + wsJdbcConnection.getClass() + " or any of its super classes." +
                 " This is probably not a product issue, but may require a test update if the JDBC classes are being refactored.");
        connImplField.setAccessible(true);
        return (Connection) connImplField.get(wsJdbcConnection);
    }

}
