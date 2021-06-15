/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jdbc.heritage.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransactionRollbackException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executor;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ce.cm.DuplicateKeyException;
import com.ibm.websphere.ce.cm.ObjectClosedException;
import com.ibm.websphere.ce.cm.StaleConnectionException;
import com.ibm.websphere.rsadapter.JDBCConnectionSpec;
import com.ibm.websphere.rsadapter.WSDataSource;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;
import test.jdbc.heritage.driver.HeritageDBConnection;
import test.jdbc.heritage.driver.helper.HeritageDBDuplicateKeyException;
import test.jdbc.heritage.driver.helper.HeritageDBFeatureUnavailableException;
import test.jdbc.heritage.driver.helper.HeritageDBStaleConnectionException;

@SuppressWarnings({ "serial", "restriction" })
@WebServlet(urlPatterns = "/JDBCHeritageTestServlet")
public class JDBCHeritageTestServlet extends FATServlet {
    @Resource
    private DataSource defaultDataSource;

    @Resource(name = "java:comp/jdbc/env/unsharable-ds-xa-loosely-coupled", shareable = false)
    private DataSource defaultDataSource_unsharable_loosely_coupled;

    @Resource(name = "java:comp/jdbc/env/unsharable-ds-xa-tightly-coupled", shareable = false)
    private DataSource defaultDataSource_unsharable_tightly_coupled;

    @Resource(lookup = "jdbc/one-phase", shareable = true)
    private DataSource dsOnePhaseSharable;

    @Resource(lookup = "jdbc/one-phase", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    private DataSource dsOnePhaseSharableWithAppAuth;

    @Resource(lookup = "jdbc/one-phase", shareable = false)
    private DataSource dsOnePhaseUnsharable;

    @Resource(lookup = "jdbc/helperDefaulted", shareable = false)
    private DataSource dsWithHelperDefaulted;

    @Resource
    private UserTransaction tx;

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try (Connection con = defaultDataSource.getConnection()) {
            Statement stmt = con.createStatement();
            try {
                stmt.execute("DROP TABLE MYTABLE");
            } catch (SQLException x) {
                // probably didn't exist
            }
            stmt.execute("CREATE TABLE MYTABLE (ID INT NOT NULL PRIMARY KEY, STRVAL VARCHAR(40))");
            stmt.executeUpdate("INSERT INTO MYTABLE VALUES (0, 'zero')");
        } catch (SQLException x) {
            throw new ServletException(x);
        }
    }

    /**
     * Utility method that accesses the internal field WSJdbcConnection.connImpl.
     * This is useful for determining if we got the same connection from the connection pool.
     *
     * @param con connection wrapper (WSJdbcConnection).
     * @return connection implementation.
     */
    private static Connection connImpl(Connection con) throws PrivilegedActionException {
        return AccessController.doPrivileged((PrivilegedExceptionAction<Connection>) () -> {
            for (Class<?> c = con.getClass(); c != null; c = c.getSuperclass())
                try {
                    Field connImpl = c.getDeclaredField("connImpl");
                    connImpl.setAccessible(true);
                    return (Connection) connImpl.get(con);
                } catch (NoSuchFieldException x) {
                    // ignore, try the super class
                }
            throw new NoSuchFieldException("Unable to find conImpl on the connection wrapper. If the field has been renamed, you will need to update this test.");
        });
    }

    /**
     * Utility method that accesses the internal field WSJdbcPreparedStatement.pstmtImpl.
     * This is useful for determining if we got the same statement from the statement cache.
     *
     * @param pstmt prepared statement wrapper (WSJdbcPreparedStatement).
     * @return prepared statement implementation.
     */
    private static PreparedStatement pstmtImpl(PreparedStatement pstmt) throws PrivilegedActionException {
        return AccessController.doPrivileged((PrivilegedExceptionAction<PreparedStatement>) () -> {
            for (Class<?> c = pstmt.getClass(); c != null; c = c.getSuperclass())
                try {
                    Field pstmtImpl = c.getDeclaredField("pstmtImpl");
                    pstmtImpl.setAccessible(true);
                    return (PreparedStatement) pstmtImpl.get(pstmt);
                } catch (NoSuchFieldException x) {
                    // ignore, try the super class
                }
            throw new NoSuchFieldException("Unable to find pstmtImpl on prepared statement wrapper. If the field has been renamed, you will need to update this test.");
        });
    }

    /**
     * Verifies that when an authorization error occurs, only the connection on which the
     * error occurred is removed from the pool.
     */
    @Test
    public void testAuthorizationError() throws Exception {
        Connection connImpl;

        try (Connection con1 = dsWithHelperDefaulted.getConnection("testAuthorizationError", "testAuthorizationError")) {
            connImpl = connImpl(con1);

            try (Connection con2 = dsWithHelperDefaulted.getConnection("testAuthorizationError", "testAuthorizationError")) {
                // put the first connection into the pool
                con1.close();

                // cause an authorization error on the second connection
                try {
                    con2.prepareCall("CALL TEST.FORCE_EXCEPTION(08001,28,java.sql.SQLRecoverableException)").executeQuery();
                    fail("Test case did not force an error with SQL state 08001 and error code 28");
                } catch (SQLRecoverableException x) {
                    // Expected. Ensure the SQL state, error code, and exception class match.
                    assertEquals(SQLRecoverableException.class.getName(), x.getClass().getName());
                    assertEquals("08001", x.getSQLState());
                    assertEquals(28, x.getErrorCode());
                }
            }
        }

        // Verify that the first connection is reused form the pool, unimpacted by con2's authorization error
        try (Connection con3 = dsWithHelperDefaulted.getConnection("testAuthorizationError", "testAuthorizationError")) {
            assertSame(connImpl, connImpl(con3));
        }
    }

    /**
     * Verifies that doConnectionCleanup is invoked on a data store helper that uses this notification to
     * restore the default list of client info properties.
     */
    @Test
    public void testConnectionCleanup() throws Exception {
        try (Connection con = defaultDataSource.getConnection()) {
            HeritageDBConnection hcon = con.unwrap(HeritageDBConnection.class);
            Set<String> defaultClientInfoKeys = hcon.getClientInfoKeys();

            // Set non-default keys
            hcon.setClientInfoKeys("testConCleanupKey1", "testConCleanupKey2");

            // Prove the fake JDBC driver API returns what we set because the test will rely on this later
            Set<String> validKeys = hcon.getClientInfoKeys();
            assertTrue(validKeys.toString(), validKeys.contains("testConCleanupKey1")
                                             && validKeys.contains("testConCleanupKey2")
                                             && validKeys.size() == 2);

            tx.begin();
            try {
                con.createStatement()
                                .executeQuery("VALUES ('testConnectionCleanup is an excellent test')")
                                .getStatement()
                                .close();

                // doConnectionCleanup resets the default client info keys value when the connection handle is
                // reassociated across the transaction boundary

                validKeys = hcon.getClientInfoKeys();
                assertEquals(defaultClientInfoKeys, validKeys);

                // Set more non-default keys
                hcon.setClientInfoKeys("testConCleanupKey3", "testConCleanupKey4", "testConCleanupKey5");

            } finally {
                tx.commit();
            }

            // doConnectionCleanup again resets the default client info keys value when the connection handle is
            // reassociated across the transaction boundary

            validKeys = hcon.getClientInfoKeys();
            assertEquals(defaultClientInfoKeys, validKeys);
        }
    }

    /**
     * Verifies that doConnectionSetup was performed on a new connection.
     * Verifies that doConnectionSetupPerGetConnection is invoked for each first connection handle.
     * Verifies that doConnectionCleanupPerCloseConnection is invoked upon closing or dissociating the last connection handle.
     */
    @Test
    public void testConnectionSetup() throws Exception {
        try (Connection con = defaultDataSource.getConnection("testConnectionSetupUser", "PASSWORD")) {

            // Read the value that was set by doConnectionSetup
            Statement stmt = con.createStatement();
            ResultSet result = stmt.executeQuery("VALUES SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS()");
            assertTrue(result.next());
            assertNotNull(result.getString(1));
            stmt.close();

            // Query the doConnectionSetupPerGetConnection count, expecting 1
            result = con.prepareCall("CALL TEST.GET_SETUP_COUNT()").executeQuery();
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
            result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
            result.getStatement().close();

            // Query the doConnectionCleanupPerCloseConnection count, expecting 0
            result = con.prepareCall("CALL TEST.GET_CLEANUP_COUNT()").executeQuery();
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
            result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
            result.getStatement().close();
        }

        Connection con;
        ResultSet result;

        tx.begin();
        try {
            con = defaultDataSource.getConnection("testConnectionSetupUser", "PASSWORD");

            // Query the doConnectionSetupPerGetConnection count, expecting 2
            result = con.prepareCall("CALL TEST.GET_SETUP_COUNT()").executeQuery();
            assertTrue(result.next());
            assertEquals(2, result.getInt(1));
            result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
            result.getStatement().close();

            // Query the doConnectionCleanupPerCloseConnection count, expecting 1
            result = con.prepareCall("CALL TEST.GET_CLEANUP_COUNT()").executeQuery();
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
            result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
            result.getStatement().close();

            Connection con2 = defaultDataSource.getConnection("testConnectionSetupUser", "PASSWORD");

            // Query the doConnectionSetupPerGetConnection count, still expecting 2
            result = con.prepareCall("CALL TEST.GET_SETUP_COUNT()").executeQuery();
            assertTrue(result.next());
            assertEquals(2, result.getInt(1));
            result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
            result.getStatement().close();

            // Query the doConnectionCleanupPerCloseConnection count, still expecting 1
            result = con.prepareCall("CALL TEST.GET_CLEANUP_COUNT()").executeQuery();
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
            result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
            result.getStatement().close();
        } finally {
            tx.commit();
        }

        // Query the doConnectionSetupPerGetConnection count
        result = con.prepareCall("CALL TEST.GET_SETUP_COUNT()").executeQuery();
        assertTrue(result.next());
        // assertEquals(3, result.getInt(1)); // legacy behavior does not invoke doConnectionSetupPerGetConnection on reassociate
        result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
        result.getStatement().close();

        // Query the doConnectionCleanupPerCloseConnection count, expecting 2
        result = con.prepareCall("CALL TEST.GET_CLEANUP_COUNT()").executeQuery();
        assertTrue(result.next());
        assertEquals(2, result.getInt(1));
        result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
        result.getStatement().close();

        con.close();
    }

    /**
     * Verifies that doConnectionCleanupPerTransaction is invoked exactly once for each global transaction
     * or Local Transaction Containment (which is not actually a transaction).
     */
    @Test
    public void testConnectionSetupPerTransaction() throws Exception {
        // Use in Local Transaction Containment:
        try (Connection con = defaultDataSource.getConnection("testConnectionSetupPerTxUser", "PASSWORD")) {

            // Query the doConnectionSetupPerTransaction count, expecting 1
            ResultSet result = con.prepareCall("CALL TEST.GET_TRANSACTION_COUNT()").executeQuery();
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
            result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
            result.getStatement().close();
        }

        Connection con;
        ResultSet result;

        // Use in global transaction:
        tx.begin();
        try {
            con = defaultDataSource.getConnection("testConnectionSetupPerTxUser", "PASSWORD");

            // Query the doConnectionSetupPerTransaction count, expecting 2
            result = con.prepareCall("CALL TEST.GET_TRANSACTION_COUNT()").executeQuery();
            assertTrue(result.next());
            assertEquals(2, result.getInt(1));
            result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
            result.getStatement().close();

            Connection con2 = defaultDataSource.getConnection("testConnectionSetupPerTxUser", "PASSWORD");

            // Query the doConnectionSetupPerTransaction count, still expecting 2
            result = con.prepareCall("CALL TEST.GET_TRANSACTION_COUNT()").executeQuery();
            assertTrue(result.next());
            assertEquals(2, result.getInt(1));
            result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
            result.getStatement().close();

            con.close();
            con2.close();

            con = defaultDataSource.getConnection("testConnectionSetupPerTxUser", "PASSWORD");

            // Query the doConnectionSetupPerTransaction count, still expecting 2 because it's the same transaction
            result = con.prepareCall("CALL TEST.GET_TRANSACTION_COUNT()").executeQuery();
            assertTrue(result.next());
            assertEquals(2, result.getInt(1));
            result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
            result.getStatement().close();
        } finally {
            tx.commit();
        }

        // Use again in global transaction:
        tx.begin();
        try {
            con = defaultDataSource.getConnection("testConnectionSetupPerTxUser", "PASSWORD");

            // Query the doConnectionSetupPerTransaction count, expecting 3
            result = con.prepareCall("CALL TEST.GET_TRANSACTION_COUNT()").executeQuery();
            assertTrue(result.next());
            assertEquals(3, result.getInt(1));
            result.getStatement().setPoolable(false); // statement caching interferes with test replacing sql
            result.getStatement().close();

            con.close();
        } finally {
            tx.commit();
        }
    }

    /**
     * It should be possible to configure exception replacement without specifying a data store helper,
     * in which case a default is assigned.
     */
    @Test
    public void testDataStoreHelperUnspecified() throws Exception {
        assertNotNull(dsWithHelperDefaulted);

        try (Connection con = dsWithHelperDefaulted.getConnection()) {
            // exception replacement from identifyException
            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(2300H,0,java.sql.SQLException)").executeQuery();
                fail("Test case did not force an error with SQL state 2300H");
            } catch (HeritageDBDuplicateKeyException x) {
                // pass
            }

            // exception replacement from identifyException
            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(0A00H,0,java.sql.SQLFeatureNotSupportedException)").executeQuery();
                fail("Test case did not force an error with SQL state 0A00H");
            } catch (HeritageDBFeatureUnavailableException x) {
                // pass
            }

            // exception ignore from identifyException
            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(S1000,8006,java.sql.SQLException)").executeQuery();
                fail("Test case did not force an error with SQL state S1000 and error code 8006");
            } catch (SQLException x) {
                if (x.getClass().getName().equals("java.sql.SQLException"))
                    ; // pass
                else
                    throw x;
            }

            // exception replacement from identifyException
            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(HY000,8008,java.sql.SQLException)").executeQuery();
                fail("Test case did not force an error with SQL state HY000 and error code 8008");
            } catch (StaleConnectionException x) {
                // pass
            }

            assertTrue(con.isClosed()); // due to stale connection
        }

        PreparedStatement cachedStatement;

        try (Connection con = dsWithHelperDefaulted.getConnection("testDataStoreHelperUnspecified", "StaleStmtPwd")) {
            // Force an error that raises error code (22013) that we mapped to StaleStatementException,
            PreparedStatement pstmt = con.prepareStatement("VALUES SQRT (-9)");
            cachedStatement = pstmtImpl(pstmt);
            try {
                ResultSet result = pstmt.executeQuery();
                result.next();
                fail("The database says that the square root of -9 is " + result.getObject(1));
            } catch (StaleConnectionException x) {
                // The StaleStatementException must be surfaced to the application as StaleConnectionException
                assertEquals(StaleConnectionException.class.getName(), x.getClass().getName());
                assertEquals("22013", x.getSQLState());
            }
        }

        // The statement must not be cached due to the StaleStatementException
        try (Connection con = dsWithHelperDefaulted.getConnection("testDataStoreHelperUnspecified", "StaleStmtPwd")) {
            // Force an error that raises a SQLState (22013) that we mapped to StaleStatementException,
            PreparedStatement pstmt = con.prepareStatement("VALUES SQRT (-9)");
            assertNotSame(cachedStatement, pstmtImpl(pstmt));
        }
    }

    /**
     * Confirm that a dataSource that is configured with heritageSettings can be injected
     * and has the transaction isolation level that is assigned as default by the DataStoreHelper.
     */
    @Test
    public void testDefaultIsolationLevel() throws Exception {
        assertNotNull(defaultDataSource);
        try (Connection con = defaultDataSource.getConnection()) {
            assertEquals(Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());
        }
    }

    /**
     * Confirm that a data store helper's doStatementCleanup is capable of resetting the configured
     * default queryTimeout from the dataSource configuration.
     */
    @Test
    public void testDefaultQueryTimeout() throws Exception {
        try (Connection con = defaultDataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement("VALUES ('testDefaultQueryTimeout', SQRT(196))");
            assertEquals(81, pstmt.getQueryTimeout()); // configured on dataSource as 1m21s
            pstmt.setQueryTimeout(289);
            pstmt.executeQuery().close();
            // return statement to cache
            pstmt.close();

            // reuse from cache
            pstmt = con.prepareStatement("VALUES ('testDefaultQueryTimeout', SQRT(196))");
            assertEquals(81, pstmt.getQueryTimeout()); // doStatementCleanup resets the configured default
            pstmt.executeQuery().close();
            pstmt.close();
        }

    }

    /**
     * Verify that the data store helper is invoked to determine if an exception indicates a connection error,
     * even when exceptions are not replaced.
     */
    @Test
    public void testIsConnectionError() throws Exception {
        Connection connImpl;
        try (Connection con = defaultDataSource_unsharable_loosely_coupled.getConnection("user-of-testIsConnectionError", "password-of-user-of-testIsConnectionError")) {
            con.prepareCall("CALL TEST.FORCE_EXCEPTION_ON_IS_VALID()").executeQuery().getStatement().close();
            connImpl = connImpl(con);
        }

        // Must not reuse the same connection from the connection pool
        try (Connection con = defaultDataSource_unsharable_loosely_coupled.getConnection("user-of-testIsConnectionError", "password-of-user-of-testIsConnectionError")) {
            assertNotSame(connImpl, connImpl(con));
        }
    }

    /**
     * Verifies that the type of sharing indicated by the isShareable legacy operation
     * is consistent with the resource reference.
     */
    @Test
    public void testIsShareable() throws Exception {
        Method isShareable;

        try (Connection con = defaultDataSource.getConnection()) {
            isShareable = con.getClass().getMethod("isShareable");

            assertTrue((Boolean) isShareable.invoke(con));
        }

        try (Connection con = dsWithHelperDefaulted.getConnection()) {
            assertFalse((Boolean) isShareable.invoke(con));
        }
    }

    /**
     * Verifies that the isXADataSource legacy operation returns the correct value
     * based on the dataSource configuration.
     */
    @Test
    public void testIsXADataSource() throws Exception {
        assertTrue(((WSDataSource) defaultDataSource).isXADataSource());
        assertTrue(((WSDataSource) defaultDataSource_unsharable_loosely_coupled).isXADataSource());
        assertTrue(((WSDataSource) defaultDataSource_unsharable_tightly_coupled).isXADataSource());
        assertFalse(((WSDataSource) dsOnePhaseSharable).isXADataSource());
        assertFalse(((WSDataSource) dsOnePhaseUnsharable).isXADataSource());
        assertFalse(((WSDataSource) dsWithHelperDefaulted).isXADataSource());
    }

    /**
     * Verifies that API classes can be loaded from the mock heritage API bundle fragment.
     */
    @Test
    public void testLoadClassesFromHeritageBundle() throws Exception {
        Class.forName("com.ibm.websphere.ce.cm.PortableSQLException");
        Class.forName("com.ibm.websphere.rsadapter.GenericDataStoreHelper");
        Class.forName("com.ibm.websphere.appprofile.accessintent.AccessIntent");
    }

    /**
     * Verifies that the ConnectionWaitTimeoutException class can be loaded from the com.ibm.ws.jdbc bundle
     */
    @Test
    public void testLoadClassFromJDBCBundle() throws Exception {
        Class.forName("com.ibm.websphere.ce.cm.ConnectionWaitTimeoutException");
    }

    /**
     * Verify that the data store helper is invoked to map and replace exceptions.
     */
    @Test
    public void testMapExceptionAndReplaceIt() throws Exception {
        try (Connection con = defaultDataSource.getConnection()) {
            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(0A000,0,java.sql.SQLFeatureNotSupportedException)").executeQuery();
                fail("Test case did not force an error with SQL state 0A000");
            } catch (HeritageDBFeatureUnavailableException x) {
                // pass
            }

            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(08001,22344,java.sql.SQLException)").executeQuery();
                fail("Test case did not force an error with SQL state 08001 and error code 22344");
            } catch (HeritageDBStaleConnectionException x) {
                // pass
            }

            // user-defined exception indicates stale:
            assertTrue(con.isClosed());
        }

        try (Connection con = defaultDataSource.getConnection()) {
            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(08006,4002,java.sql.SQLException)").executeQuery();
                fail("Test case did not force an error with SQL state 08006 and error code 4002");
            } catch (StaleConnectionException x) {
                // pass if not a subclass
                assertEquals(StaleConnectionException.class.getName(), x.getClass().getName());
            }

            // user-defined exception indicates stale:
            assertTrue(con.isClosed());
        }
    }

    /**
     * Verify that WSDataSource.getConnection(conspec) can be used to match and share a connection
     * based on its catalog attribute.
     */
    @Test
    public void testMatchCurrentCatalog() throws Exception {
        WSDataSource wsds = (WSDataSource) dsOnePhaseSharable;

        try (Connection con1 = wsds.getConnection()) {
            con1.setAutoCommit(false);
            con1.setCatalog("CATALOG-17");
            con1.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (17, 'testMatchCurrentCatalog')");
        }

        // connection is not shared with a non-matching request
        JDBCConnectionSpec req2 = new ConnectionRequest();
        req2.setCatalog("CATALOG-18");
        try (Connection con2 = wsds.getConnection(req2)) {
            assertEquals("CATALOG-18", con2.getCatalog());
            con2.setAutoCommit(false);
            con2.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (18, 'testMatchCurrentCatalog')");
            con2.rollback();
        }

        // connection can be shared based on its current state,
        JDBCConnectionSpec req3 = new ConnectionRequest();
        req3.setCatalog("CATALOG-17");
        try (Connection con3 = wsds.getConnection(req3)) {
            assertEquals("CATALOG-17", con3.getCatalog());
            con3.commit();
        }

        // verify that only the first update was committed, whereas the second rolled back,
        tx.begin();
        try (Connection con4 = wsds.getConnection(new ConnectionRequest())) {
            ResultSet result = con4.createStatement().executeQuery("SELECT ID FROM MYTABLE WHERE STRVAL='testMatchCurrentCatalog'");
            assertTrue(result.next());
            assertEquals(17, result.getInt(1));
            assertFalse(result.next());
        } finally {
            tx.commit();
        }
    }

    /**
     * Verify that WSDataSource.getConnection(conspec) can be used to match and share a connection
     * based on its cursor holdability attribute.
     */
    @Test
    public void testMatchCurrentHoldability() throws Exception {
        WSDataSource wsds = (WSDataSource) dsOnePhaseSharable;

        try (Connection con1 = wsds.getConnection()) {
            con1.setAutoCommit(false);
            con1.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
            con1.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (21, 'testMatchCurrentHoldability')");
        }

        // connection is not shared with a non-matching request
        JDBCConnectionSpec req2 = new ConnectionRequest();
        req2.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
        try (Connection con2 = wsds.getConnection(req2)) {
            assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, con2.getHoldability());
            con2.setAutoCommit(false);
            con2.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (22, 'testMatchCurrentHoldability')");
            con2.rollback();
        }

        // connection can be shared based on its current state,
        JDBCConnectionSpec req3 = new ConnectionRequest();
        req3.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
        try (Connection con3 = wsds.getConnection(req3)) {
            assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, con3.getHoldability());
            ResultSet result = con3.createStatement().executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID=21");
            con3.commit();
            assertTrue(result.isClosed());
        }

        // verify that only the first update was committed, whereas the second rolled back,
        tx.begin();
        try (Connection con4 = wsds.getConnection(new ConnectionRequest())) {
            ResultSet result = con4.createStatement().executeQuery("SELECT ID FROM MYTABLE WHERE STRVAL='testMatchCurrentHoldability'");
            assertTrue(result.next());
            assertEquals(21, result.getInt(1));
            assertFalse(result.next());
        } finally {
            tx.commit();
        }
    }

    /**
     * Verify that WSDataSource.getConnection(conspec) can be used to match and share a connection
     * based on its isolation level.
     */
    @AllowedFFDC({
                   "java.lang.IllegalStateException", // test attempts to enlist multiple one-phase resources to prove that sharing does not occur
                   "java.sql.SQLException", // test attempts to enlist multiple one-phase resources to prove that sharing does not occur
                   "javax.resource.ResourceException" // test attempts to enlist multiple one-phase resources to prove that sharing does not occur
    })
    @Test
    public void testMatchCurrentIsolationLevel() throws Exception {
        tx.begin();
        try (Connection con1 = dsOnePhaseSharable.getConnection()) {
            // normal connection request defaults to isolationLevel from the dataSource,
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con1.getTransactionIsolation());

            con1.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con1.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (13, 'testMatchCurrentIsolationLevel')");

            // connection can be shared based on its current state,
            WSDataSource wsds = (WSDataSource) dsOnePhaseSharable;
            JDBCConnectionSpec req2 = new ConnectionRequest();
            req2.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            Connection con2 = wsds.getConnection(req2);
            assertEquals(Connection.TRANSACTION_SERIALIZABLE, con2.getTransactionIsolation());
            int numUpdates = con2.createStatement().executeUpdate("UPDATE MYTABLE SET STRVAL='XIII' where ID=13");
            assertEquals(1, numUpdates);

            // connection cannot be shared based on a non-matching request
            JDBCConnectionSpec req3 = new ConnectionRequest();
            req3.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            Connection con3 = wsds.getConnection(req3);
            try {
                con3.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (14, 'testMatchCurrentIsolationLevel')");
                fail("3rd connection request should not share and thus be unable to enlist in second transactional resource.");
            } catch (SQLException x) {
                boolean multiple1PCResources = false;
                for (Throwable cause = x.getCause(); cause != null && !multiple1PCResources; cause = cause.getCause())
                    multiple1PCResources |= cause instanceof IllegalStateException;
                if (multiple1PCResources) // expected
                    tx.setRollbackOnly();
                else
                    throw x;
            }
        } finally {
            if (tx.getStatus() == Status.STATUS_ACTIVE)
                tx.commit();
            else
                tx.rollback();
        }
    }

    /**
     * Verify that WSDataSource.getConnection(conspec) can be used to match and share a connection
     * based on its network timeout.
     */
    @AllowedFFDC({
                   "java.lang.IllegalStateException", // test attempts to enlist multiple one-phase resources to prove that sharing does not occur
                   "java.sql.SQLException", // test attempts to enlist multiple one-phase resources to prove that sharing does not occur
                   "javax.resource.ResourceException" // test attempts to enlist multiple one-phase resources to prove that sharing does not occur
    })
    @Test
    public void testMatchCurrentNetworkTimeout() throws Exception {
        tx.begin();
        try (Connection con1 = dsOnePhaseSharable.getConnection()) {
            int originalNetworkTimeout = con1.getNetworkTimeout();
            int newNetworkTimeout = originalNetworkTimeout + 30000;
            Executor sameThreadExecutor = r -> r.run();

            con1.setNetworkTimeout(sameThreadExecutor, newNetworkTimeout);
            con1.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (15, 'testMatchCurrentNetworkTimeout')");

            // connection can be shared based on its current state,
            WSDataSource wsds = (WSDataSource) dsOnePhaseSharable;
            JDBCConnectionSpec req2 = new ConnectionRequest();
            req2.setNetworkTimeout(newNetworkTimeout);
            Connection con2 = wsds.getConnection(req2);
            assertEquals(newNetworkTimeout, con2.getNetworkTimeout());
            int numUpdates = con2.createStatement().executeUpdate("UPDATE MYTABLE SET STRVAL='XV' where ID=15");
            assertEquals(1, numUpdates);

            // connection cannot be shared based on a non-matching request
            JDBCConnectionSpec req3 = new ConnectionRequest();
            req3.setNetworkTimeout(originalNetworkTimeout + 40000);
            try (Connection con3 = wsds.getConnection(req3)) {
                con3.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (16, 'testMatchCurrentNetworkTimeout')");
                fail("3rd connection request should not share and thus be unable to enlist in second transactional resource.");
            } catch (SQLException x) {
                boolean multiple1PCResources = false;
                for (Throwable cause = x.getCause(); cause != null && !multiple1PCResources; cause = cause.getCause())
                    multiple1PCResources |= cause instanceof IllegalStateException;
                if (multiple1PCResources) // expected
                    tx.setRollbackOnly();
                else
                    throw x;
            }
        } finally {
            if (tx.getStatus() == Status.STATUS_ACTIVE)
                tx.commit();
            else
                tx.rollback();
        }
    }

    /**
     * Verify that WSDataSource.getConnection(conspec) can be used to match and share a connection
     * based on its read only attribute.
     */
    @Test
    public void testMatchCurrentReadOnly() throws Exception {
        tx.begin();
        try (Connection con1 = dsOnePhaseSharable.getConnection()) {

            assertFalse(con1.isReadOnly());
            con1.setReadOnly(true);

            // connection can be shared based on its current state,
            WSDataSource wsds = (WSDataSource) dsOnePhaseSharable;
            JDBCConnectionSpec req2 = new ConnectionRequest();
            req2.setReadOnly(true);
            Connection con2 = wsds.getConnection(req2);
            assertTrue(con2.isReadOnly());
            ResultSet result = con2.createStatement().executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID=0");
            assertTrue(result.next());
            assertEquals("zero", result.getString(1));
        } finally {
            tx.commit();
        }
    }

    /**
     * Verify that WSDataSource.getConnection(conspec) can be used to match and share a connection
     * based on its schema attribute.
     */
    @Test
    public void testMatchCurrentSchema() throws Exception {
        WSDataSource wsds = (WSDataSource) dsOnePhaseSharable;

        try (Connection con1 = wsds.getConnection()) {
            con1.setAutoCommit(false);
            con1.setSchema("APP");
            con1.createStatement().executeUpdate("INSERT INTO DBUSER.MYTABLE VALUES (23, 'testMatchCurrentSchema')");
        }

        // connection is not shared with a non-matching request
        JDBCConnectionSpec req2 = new ConnectionRequest();
        req2.setSchema("DBUSER");
        try (Connection con2 = wsds.getConnection(req2)) {
            assertEquals("DBUSER", con2.getSchema());
            con2.setAutoCommit(false);
            con2.createStatement().executeUpdate("INSERT INTO DBUSER.MYTABLE VALUES (24, 'testMatchCurrentSchema')");
            con2.rollback();
        }

        // connection can be shared based on its current state,
        JDBCConnectionSpec req3 = new ConnectionRequest();
        req3.setSchema("APP");
        try (Connection con3 = wsds.getConnection(req3)) {
            assertEquals("APP", con3.getSchema());
            con3.commit();
        }

        // verify that only the first update was committed, whereas the second rolled back,
        tx.begin();
        try (Connection con4 = wsds.getConnection(new ConnectionRequest())) {
            ResultSet result = con4.createStatement().executeQuery("SELECT ID FROM DBUSER.MYTABLE WHERE STRVAL='testMatchCurrentSchema'");
            assertTrue(result.next());
            assertEquals(23, result.getInt(1));
            assertFalse(result.next());
        } finally {
            tx.commit();
        }
    }

    /**
     * Verify that WSDataSource.getConnection(conspec) can be used to match and share a connection
     * based on its typeMap attribute.
     */
    @Test
    public void testMatchCurrentTypeMap() throws Exception {
        WSDataSource wsds = (WSDataSource) dsOnePhaseSharable;

        Map<String, Class<?>> typeMap = new LinkedHashMap<String, Class<?>>();
        typeMap.put("BIGINT", BigInteger.class);
        typeMap.put("BIGD", BigDecimal.class);

        Map<String, Class<?>> matchingTypeMap = new TreeMap<String, Class<?>>();
        matchingTypeMap.put("BIGD", BigDecimal.class);
        matchingTypeMap.put("BIGINT", BigInteger.class);

        Map<String, Class<?>> nonMatchingTypeMap = new LinkedHashMap<String, Class<?>>();
        nonMatchingTypeMap.put("BIGINT", BigInteger.class);
        nonMatchingTypeMap.put("BIGD", Number.class);

        try (Connection con1 = wsds.getConnection()) {
            con1.setAutoCommit(false);
            con1.setTypeMap(typeMap);
            con1.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (19, 'testMatchCurrentTypeMap')");
        }

        // connection is not shared with a non-matching request
        JDBCConnectionSpec req2 = new ConnectionRequest();
        req2.setTypeMap(nonMatchingTypeMap);
        try (Connection con2 = wsds.getConnection(req2)) {
            assertEquals(nonMatchingTypeMap, con2.getTypeMap());
            con2.setAutoCommit(false);
            con2.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (20, 'testMatchCurrentTypeMap')");
            con2.rollback();
        }

        // connection can be shared based on its current state,
        JDBCConnectionSpec req3 = new ConnectionRequest();
        req3.setTypeMap(matchingTypeMap);
        try (Connection con3 = wsds.getConnection(req3)) {
            assertEquals(matchingTypeMap, con3.getTypeMap());
            con3.commit();
        }

        // verify that only the first update was committed, whereas the second rolled back,
        tx.begin();
        try (Connection con4 = wsds.getConnection(new ConnectionRequest())) {
            ResultSet result = con4.createStatement().executeQuery("SELECT ID FROM MYTABLE WHERE STRVAL='testMatchCurrentTypeMap'");
            assertTrue(result.next());
            assertEquals(19, result.getInt(1));
            assertFalse(result.next());
        } finally {
            tx.commit();
        }
    }

    /**
     * Verify that WSDataSource.getConnection(conspec) can be used to match and share a connection
     * based on its user name attribute.
     */
    @Test
    public void testMatchCurrentUser() throws Exception {
        WSDataSource wsds = (WSDataSource) dsOnePhaseSharableWithAppAuth;

        JDBCConnectionSpec req1 = new ConnectionRequest();
        req1.setUserName("dbuser");
        req1.setPassword("dbpwd");
        try (Connection con1 = wsds.getConnection(req1)) {
            con1.setAutoCommit(false);
            assertEquals("dbuser", con1.getMetaData().getUserName().toLowerCase());
            con1.createStatement().executeUpdate("INSERT INTO DBUSER.MYTABLE VALUES (25, 'testMatchCurrentUser')");
        }

        // connection is not shared with a non-matching request
        JDBCConnectionSpec req2 = new ConnectionRequest();
        req2.setUserName("otheruser");
        req2.setPassword("dbpwd");
        try (Connection con2 = wsds.getConnection(req2)) {
            assertEquals("otheruser", con2.getMetaData().getUserName().toLowerCase());
            con2.setAutoCommit(false);
            con2.createStatement().executeUpdate("INSERT INTO DBUSER.MYTABLE VALUES (26, 'testMatchCurrentUser')");
            con2.rollback();
        }

        // connection can be shared based on its current state,
        JDBCConnectionSpec req3 = new ConnectionRequest();
        req3.setUserName("dbuser");
        req3.setPassword("dbpwd");
        try (Connection con3 = wsds.getConnection(req3)) {
            assertEquals("dbuser", con3.getMetaData().getUserName().toLowerCase());
            con3.commit();
        }

        // verify that only the first update was committed, whereas the second rolled back,
        tx.begin();
        try (Connection con4 = wsds.getConnection(req2)) {
            ResultSet result = con4.createStatement().executeQuery("SELECT ID FROM DBUSER.MYTABLE WHERE STRVAL='testMatchCurrentUser'");
            assertTrue(result.next());
            assertEquals(25, result.getInt(1));
            assertFalse(result.next());
        } finally {
            tx.commit();
        }
    }

    /**
     * Verify that ObjectClosedException is raised when attempting operations on a closed JDBC artifact.
     */
    @Test
    public void testObjectClosedException() throws Exception {
        Connection con = defaultDataSource.getConnection();
        DatabaseMetaData metadata;
        try {
            metadata = con.getMetaData();
            Statement st = con.createStatement();
            ResultSet result = st.executeQuery("VALUES ('testObjectClosedException')");
            st.close();

            try {
                result.next();
                fail("ReultSet should be closed.");
            } catch (ObjectClosedException x) {
                // expected
            }

            try {
                st.executeQuery("VALUES 'testObjectClosedException II'");
                fail("Statement should be closed.");
            } catch (ObjectClosedException x) {
                // expected
            }

            PreparedStatement ps = con.prepareStatement("VALUES ('testObjectClosedException III')");
            ps.close();
            try {
                ps.executeQuery();
                fail("PreparedStatement should be closed.");
            } catch (ObjectClosedException x) {
                // expected
            }

            CallableStatement cs = con.prepareCall("VALUES ('testObjectClosedException IV')");
            cs.close();
            try {
                cs.clearParameters();
                fail("CallableStatement should be closed.");
            } catch (ObjectClosedException x) {
                // expected
            }
        } finally {
            con.close();
        }

        try {
            con.setAutoCommit(false);
            fail("Connection should be closed.");
        } catch (ObjectClosedException x) {
            // expected
        }

        try {
            metadata.getSchemas();
            fail("Connection should be closed.");
        } catch (ObjectClosedException x) {
            // expected
        }
    }

    /**
     * Cause an error that we have arbitrarily mapped to StaleStatementException in the data store helper.
     * Verify that the statement is not cached and that the error is surface to the application as a
     * StaleConnectionException. This matches the legacy behavior.
     */
    @Test
    public void testStaleStatement() throws Exception {
        PreparedStatement cachedStatement;

        try (Connection con = defaultDataSource.getConnection("testStaleStatement", "StaleStmtPwd")) {
            // Force an error that raises a SQLState (22013) that we mapped to StaleStatementException,
            PreparedStatement pstmt = con.prepareStatement("VALUES SQRT (-1)");
            cachedStatement = pstmtImpl(pstmt);
            try {
                ResultSet result = pstmt.executeQuery();
                result.next();
                fail("The database says that the square root of -1 is " + result.getObject(1));
            } catch (StaleConnectionException x) {
                // The StaleStatementException must be surfaced to the application as StaleConnectionException
                assertEquals(StaleConnectionException.class.getName(), x.getClass().getName());
                assertEquals("22013", x.getSQLState());
            }
        }

        // The statement must not be cached due to the StaleStatementException
        try (Connection con = defaultDataSource.getConnection("testStaleStatement", "StaleStmtPwd")) {
            // Force an error that raises a SQLState (22013) that we mapped to StaleStatementException,
            PreparedStatement pstmt = con.prepareStatement("VALUES SQRT (-1)");
            assertNotSame(cachedStatement, pstmtImpl(pstmt));
        }
    }

    /**
     * Confirm that doesStatementCacheIsoLevel causes prepared statements to be cached based on
     * the isolation level that was present on the connection at the time the statement was
     * created.
     */
    @Test
    public void testStatementCachingBasedOnIsolationLevel() throws Exception {
        try (Connection con = defaultDataSource.getConnection()) {
            String sql = "VALUES('testStatementCachingBasedOnIsolationLevel')";
            PreparedStatement pstmt;

            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            pstmt = con.prepareStatement(sql);
            PreparedStatement pstmtRC = pstmtImpl(pstmt);
            pstmt.close();

            con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            pstmt = con.prepareStatement(sql);
            PreparedStatement pstmtRR = pstmtImpl(pstmt);
            pstmt.close();

            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            pstmt = con.prepareStatement(sql);
            PreparedStatement pstmtRC2 = pstmtImpl(pstmt);
            pstmt.close();

            assertNotSame(pstmtRC, pstmtRR);
            assertSame(pstmtRC, pstmtRC2);
        }
    }

    /**
     * Confirm that doStatementCleanup is invoked when reusing statements from the statement cache.
     */
    @Test
    public void testStatementCleanup() throws Exception {
        try (Connection con = defaultDataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement("VALUES ('testPreparedStatementCleanup', CURRENT_TIME)");
            assertEquals(225, pstmt.getMaxFieldSize()); // default value used by fake JDBC driver
            pstmt.setMaxFieldSize(441);
            pstmt.executeQuery().close();
            // return statement to cache
            pstmt.close();

            // reuse from cache
            pstmt = con.prepareStatement("VALUES ('testPreparedStatementCleanup', CURRENT_TIME)");
            assertEquals(225, pstmt.getMaxFieldSize()); // doStatementCleanup resets the default
            pstmt.executeQuery().close();
            pstmt.close();

            // repeat with CallableStatement
            CallableStatement cstmt = con.prepareCall("VALUES (SUBSTR('testCallableStatementCleanup', 5, 12))");
            assertEquals(225, cstmt.getMaxFieldSize()); // default value used by fake JDBC driver
            cstmt.setMaxFieldSize(576);
            cstmt.executeQuery().close();
            // return statement to cache
            cstmt.close();

            // reuse from cache
            cstmt = con.prepareCall("VALUES (SUBSTR('testCallableStatementCleanup', 5, 12))");
            assertEquals(225, cstmt.getMaxFieldSize()); // doStatementCleanup resets the default
            cstmt.executeQuery().close();
            cstmt.close();
        }
    }

    /**
     * Confirm that setUserDefinedMap is supplied with the identifyException configuration.
     */
    @Test
    public void testUserDefinedExceptionMap() throws SQLException {
        try (Connection con = defaultDataSource.getConnection()) {
            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(HY000,40960,test.jdbc.heritage.driver.HeritageDBDoesNotImplementItException)").executeQuery();
                fail("Test case did not force an error with error code 40960");
            } catch (HeritageDBFeatureUnavailableException x) {
                // pass
            }

            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(08004,0,java.sql.SQLException)").executeQuery();
                fail("Test case did not force an error with SQL state 08004");
            } catch (StaleConnectionException x) {
                // identifyException removes this mapping, so it should not be considered stale
                throw x;
            } catch (SQLException x) {
                // pass
            }

            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(H8006,0,test.jdbc.heritage.driver.HeritageDBConnectionBadException)").executeQuery();
                fail("Test case did not force an error with SQL state H8006");
            } catch (StaleConnectionException x) {
                // pass if not a subclass
                assertEquals(StaleConnectionException.class.getName(), x.getClass().getName());
            }

            // automatically closed due to stale connection
            assertTrue(con.isClosed());
        }

        try (Connection con = defaultDataSource.getConnection()) {
            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(H8000,0,java.sql.SQLRecoverableException)").executeQuery();
                fail("Test case did not force an error with SQL state H8000");
            } catch (StaleConnectionException x) {
                // pass if not a subclass
                assertEquals(StaleConnectionException.class.getName(), x.getClass().getName());
            }

            // automatically closed due to stale connection
            assertTrue(con.isClosed());
        }
    }

    /**
     * Verify that the user-defined error map (configured via identifyException) takes precedence when both
     * the data store helper and the identifyException configuration have a conflicting mapping.
     */
    @Test
    public void testUserDefinedExceptionMapOverridesDefaults() throws SQLException {
        try (Connection con = defaultDataSource.getConnection()) {
            // Default mapping for SQL state 23000:
            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(23000,0,java.sql.SQLIntegrityConstraintViolationException)").executeQuery();
                fail("Test case did not force an error with SQL state 23000 and error code 143360");
            } catch (HeritageDBDuplicateKeyException x) {
                throw x;
            } catch (DuplicateKeyException x) {
                // pass
            }

            // Override by identifyException when error code is 143360
            try {
                con.prepareCall("CALL TEST.FORCE_EXCEPTION(23000,143360,java.sql.SQLIntegrityConstraintViolationException)").executeQuery();
                fail("Test case did not force an error with SQL state 23000 and error code 143360");
            } catch (HeritageDBDuplicateKeyException x) {
                // pass
            }
        }
    }

    /**
     * Confirm that locks are not shared between transaction branches that are loosely coupled.
     */
    @Test
    public void testTransactionBranchesLooselyCoupled() throws Exception {
        tx.begin();
        try {
            try (Connection con1 = defaultDataSource_unsharable_loosely_coupled.getConnection()) {
                con1.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (31, 'thirty-one')");

                // Obtain a second (unshared) connection so that we have 2 transaction branches
                try (Connection con2 = defaultDataSource_unsharable_loosely_coupled.getConnection()) {
                    Statement stmt = con2.createStatement();
                    // Reduce the lock timeout so that this test runs faster,
                    stmt.execute("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.locks.waitTimeout', '2')");
                    try {
                        ResultSet result = stmt.executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID=31");
                        assertFalse(result.next());
                    } catch (SQLTransactionRollbackException x) {
                        // expected due to lock timeout
                        tx.setRollbackOnly();
                    } finally {
                        stmt.execute("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.locks.waitTimeout', '60')");
                    }
                }
            }
        } finally {
            if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tx.rollback();
            else
                tx.commit();
        }
    }

    /**
     * Confirm that locks are shared between transaction branches that are tightly coupled.
     */
    @Test
    public void testTransactionBranchesTightlyCoupled() throws Exception {
        tx.begin();
        try {
            try (Connection con1 = defaultDataSource_unsharable_tightly_coupled.getConnection()) {
                con1.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (32, 'thirty-two')");

                // Obtain a second (unshared) connection so that we have 2 transaction branches
                try (Connection con2 = defaultDataSource_unsharable_tightly_coupled.getConnection()) {
                    ResultSet result = con2.createStatement().executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID=32");
                    assertTrue(result.next());
                    assertEquals("thirty-two", result.getString(1));
                }
            }
        } finally {
            tx.commit();
        }
    }
}