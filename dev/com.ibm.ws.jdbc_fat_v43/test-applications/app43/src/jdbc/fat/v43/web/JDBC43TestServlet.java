/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.v43.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.ShardingKey;
import java.sql.ShardingKeyBuilder;
import java.sql.Statement;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.tx.jta.TransactionManagerFactory;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JDBC43TestServlet")
public class JDBC43TestServlet extends FATServlet {
    /**
     * Array indices for beginRequest/endRequest tracking.
     */
    private static final int BEGIN = 0, END = 1;

    /**
     * Maximum amount of time to wait for an asynchronous operation to complete.
     */
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(90);

    @Resource
    DataSource defaultDataSource;

    @Resource(authenticationType = AuthenticationType.APPLICATION)
    DataSource defaultDataSourceWithAppAuth;

    @Resource(lookup = "jdbc/ds", authenticationType = AuthenticationType.APPLICATION)
    DataSource dataSourceWithAppAuth;

    @Resource(lookup = "jdbc/ds", authenticationType = AuthenticationType.CONTAINER, name = "java:module/env/jdbc/ds-with-container-auth")
    DataSource dataSourceWithContainerAuth;

    @Resource(lookup = "jdbc/defaultShardingMatchCurrentState")
    DataSource dataSourceWithDefaultSharding; // shardingKey=CHAR:DefaultShardingKey; superShardingKey=VARCHAR:DefaultSuperKey;

    @Resource(lookup = "jdbc/xaDerby42")
    DataSource derbyJDBC42XADataSource;

    @Resource(lookup = "jdbc/matchCurrentState", authenticationType = AuthenticationType.APPLICATION)
    DataSource sharablePoolDataSourceMatchCurrentState;

    @Resource(lookup = "jdbc/poolOf1", authenticationType = AuthenticationType.APPLICATION)
    DataSource sharablePool1DataSourceWithAppAuth;

    @Resource(lookup = "jdbc/xa", shareable = true)
    DataSource sharableXADataSource;

    @Resource(lookup = "jdbc/xa", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource sharableXADataSourceWithAppAuth;

    @Resource(lookup = "jdbc/poolOf1", shareable = false)
    DataSource unsharablePool1DataSource;

    @Resource(lookup = "jdbc/xa", shareable = false)
    DataSource unsharableXADataSource;

    private final ExecutorService singleThreadExecutor = Executors.newFixedThreadPool(1);

    @Resource
    UserTransaction tx;

    private final static TransactionManager txm = TransactionManagerFactory.getTransactionManager();

    @Override
    public void destroy() {
        singleThreadExecutor.shutdown();
    }

    // create a table for tests to use and pre-populate it with some data
    @Override
    public void init() throws ServletException {
        try {
            Connection con = defaultDataSource.getConnection();
            try {
                Statement stmt = con.createStatement();
                stmt.execute("CREATE TABLE STREETS (NAME VARCHAR(50), CITY VARCHAR(50), STATE CHAR(2), PRIMARY KEY (NAME, CITY, STATE))");
                PreparedStatement ps = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps.setString(2, "Rochester");
                ps.setString(3, "MN");
                ps.setString(1, "Civic Center Drive NW");
                ps.executeUpdate();
                ps.setString(1, "East Circle Drive");
                ps.executeUpdate();
                ps.setString(1, "West Circle Drive");
                ps.executeUpdate();
                ps.setString(1, "Valleyhigh Drive NW");
                ps.executeUpdate();
            } finally {
                con.close();
            }
        } catch (SQLException x) {
            throw new ServletException(x);
        }
    }

    /**
     * Test that attempting to use the connection builder methods on an unwrapped connection are blocked.
     */
    @Test
    public void testBuilderMethodsBlocked() throws Exception {
        XADataSource xaDS = unsharableXADataSource.unwrap(XADataSource.class);
        try {
            xaDS.createXAConnectionBuilder();
            fail("Call to createXAConnectionBuilder on XADataSource should result in an exception");
        } catch (SQLFeatureNotSupportedException ex) {
            if (!ex.getMessage().contains("DSRA9130E"))
                throw ex;
        }

        ConnectionPoolDataSource cpDS = unsharablePool1DataSource.unwrap(ConnectionPoolDataSource.class);
        try {
            cpDS.createPooledConnectionBuilder();
            fail("Call to createPooledConnectionBuilder on ConnectionPoolDataSource should result in an exception");
        } catch (SQLFeatureNotSupportedException ex) {
            if (!ex.getMessage().contains("DSRA9130E"))
                throw ex;
        }
    }

    /**
     * Verify that connection builder can be used on a Liberty data source that is backed by a
     * javax.sql.DataSource implementation. This test only does matching on ShardingKey.
     * It does not cover user, password, or super sharding key.
     */
    @Test
    public void testDataSourceConnectionBuilderMatchShardingKey() throws Exception {
        ShardingKeyBuilder keybuilderA = dataSourceWithContainerAuth.createShardingKeyBuilder();
        ShardingKeyBuilder keybuilderB = dataSourceWithContainerAuth.createShardingKeyBuilder();
        ShardingKey keyA = keybuilderA.subkey("DSCBKey", JDBCType.VARCHAR).build();
        ShardingKey keyB = keybuilderB.subkey("DSCBKey", JDBCType.VARCHAR).build();
        ConnectionBuilder conbuilderA = dataSourceWithContainerAuth.createConnectionBuilder().shardingKey(keyA);
        ConnectionBuilder conbuilderB = dataSourceWithContainerAuth.createConnectionBuilder().shardingKey(keyB);

        tx.begin();
        try {
            Connection con1 = conbuilderA.build();
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con1.getTransactionIsolation());
            con1.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            String k1 = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k1, k1.endsWith("|VARCHAR:DSCBKey;"));

            Connection con2 = conbuilderB.build();
            // If connection handle is shared, it will report the isolation level value of con1,
            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con2.getTransactionIsolation());
            String k2 = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k2, k2.endsWith("|VARCHAR:DSCBKey;"));

            con2.close();
            con1.close();
        } finally {
            tx.commit();
        }

        // Specify a different sharding key on the builder,
        ShardingKey key3 = keybuilderA.subkey(3, JDBCType.INTEGER).build();
        Connection con3 = conbuilderA.shardingKey(key3).build();
        try {
            String k3 = con3.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k3, k3.endsWith("|VARCHAR:DSCBKey;INTEGER:3;"));
        } finally {
            con3.close();
        }

        // Clear the sharding key that is specified on the builder,
        Connection con4 = conbuilderB.shardingKey(null).build();
        try {
            String k4 = con4.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertNull(k4, k4);
        } finally {
            con4.close();
        }
    }

    /**
     * Verify that connection builder can be used on a Liberty data source that is backed by a
     * javax.sql.DataSource implementation. This test only does matching on user/password
     * and does not cover sharding.
     */
    @Test
    public void testDataSourceConnectionBuilderMatchUserPassword() throws Exception {
        ConnectionBuilder builderA = dataSourceWithAppAuth.createConnectionBuilder().user("user1").password("pwd1");
        ConnectionBuilder builderB = dataSourceWithAppAuth.createConnectionBuilder().user("user1").password("pwd1");

        tx.begin();
        try {
            Connection con1 = builderA.build();
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con1.getTransactionIsolation());
            con1.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            Connection con2 = builderB.build();
            // If connection handle is shared, it will report the isolation level value of con1,
            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con2.getTransactionIsolation());
            assertEquals("user1", con2.getMetaData().getUserName());

            con2.close();
            con1.close();
        } finally {
            tx.commit();
        }

        // Clear the user/password that were specified on the builder,
        Connection con3 = builderB.user(null).password(null).build();
        try {
            // User name should be vendor-specific default (which is APP for the Derby driver
            // upon which our mock JDBC driver is built)
            assertEquals("APP", con3.getMetaData().getUserName());
        } finally {
            con3.close();
        }
    }

    /**
     * Verify that connection builder can be used on a Liberty data source that is backed by a java.sql.Driver
     * implementation if only the user and password attributes are supplied.
     */
    @Test
    public void testDriverConnectionBuilderMatchUserPassword() throws Exception {
        ConnectionBuilder builder = defaultDataSourceWithAppAuth.createConnectionBuilder();
        builder.user("user1").password("pwd1");

        tx.begin();
        try {
            Connection con1 = builder.build();
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con1.getTransactionIsolation());
            con1.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            Connection con2 = builder.build();
            // If connection handle is shared, it will report the isolation level value of con1,
            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con2.getTransactionIsolation());
            assertEquals("user1", con2.getMetaData().getUserName());

            con2.close();
            con1.close();
        } finally {
            tx.commit();
        }

        tx.begin();
        try {
            // Clear the user/password that were specified on the builder,
            Connection con3 = builder.user(null).password(null).build();
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con3.getTransactionIsolation());
            con3.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

            // User name should be vendor-specific default (which for this data source
            // is specified on the vendor properties element in server config)
            assertEquals("user43", con3.getMetaData().getUserName());

            Connection con4 = defaultDataSourceWithAppAuth.getConnection();
            // If connection handle is shared, it will report the isolation level value of con3,
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, con4.getTransactionIsolation());
            assertEquals("user43", con4.getMetaData().getUserName());

            con4.close();
            con3.close();
        } finally {
            tx.commit();
        }
    }

    /**
     * Tests that the new JDBC 4.3 method enquoteIdentifier is correctly delegated to the driver on Statement,
     * PreparedStatement, and CallableStatement. This is done by ensuring a DSRA9110E error is thrown when
     * we attempt to execute on the closed statement.
     */
    @Test
    public void testEnquoteIdentifier() throws Exception {
        Connection con = defaultDataSource.getConnection();
        try {
            Statement stmt = con.createStatement();
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            CallableStatement cstmt = con.prepareCall("CALL SYSCS_UTIL.SYSCS_EMPTY_STATEMENT_CACHE()");
            stmt.close();
            pstmt.close();
            cstmt.close();

            try {
                stmt.enquoteIdentifier("West River Road", false);
            } catch (SQLException ex) {
                if (!ex.getMessage().contains("DSRA9110E"))
                    throw ex;
            }

            try {
                pstmt.enquoteIdentifier("IBM's Parking Lot", true);
            } catch (SQLException ex) {
                if (!ex.getMessage().contains("DSRA9110E"))
                    throw ex;
            }

            try {
                cstmt.enquoteIdentifier("'41st Street'", true);
            } catch (SQLException ex) {
                if (!ex.getMessage().contains("DSRA9110E"))
                    throw ex;
            }

        } finally {
            con.close();
        }
    }

    /**
     * Tests that the new JDBC 4.3 method enquoteLiteral is correctly delegated to the driver on Statement,
     * PreparedStatement, and CallableStatement. This is done by ensuring a DSRA9110E error is thrown when
     * we attempt to execute on the closed statement.
     */
    @Test
    public void testEnquoteLiteral() throws Exception {
        Connection con = defaultDataSource.getConnection();
        try {
            Statement stmt = con.createStatement();
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            CallableStatement cstmt = con.prepareCall("CALL SYSCS_UTIL.SYSCS_EMPTY_STATEMENT_CACHE()");
            stmt.close();
            pstmt.close();
            cstmt.close();

            try {
                stmt.enquoteLiteral("West River Road");
            } catch (SQLException ex) {
                if (!ex.getMessage().contains("DSRA9110E"))
                    throw ex;
            }

            try {
                pstmt.enquoteLiteral("IBM's Parking Lot");
            } catch (SQLException ex) {
                if (!ex.getMessage().contains("DSRA9110E"))
                    throw ex;
            }

            try {
                stmt.enquoteLiteral("'41st Street'");
            } catch (SQLException ex) {
                if (!ex.getMessage().contains("DSRA9110E"))
                    throw ex;
            }

        } finally {
            con.close();
        }
    }

    /**
     * Tests that the new JDBC 4.3 method enquoteNCharLiteral is correctly delegated to the driver on Statement,
     * PreparedStatement, and CallableStatement. This is done by ensuring a DSRA9110E error is thrown when
     * we attempt to execute on the closed statement.
     */
    @Test
    public void testEnquoteNCharLiteral() throws Exception {
        Connection con = defaultDataSource.getConnection();
        try {
            Statement stmt = con.createStatement();
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            CallableStatement cstmt = con.prepareCall("CALL SYSCS_UTIL.SYSCS_EMPTY_STATEMENT_CACHE()");
            stmt.close();
            pstmt.close();
            cstmt.close();

            try {
                stmt.enquoteNCharLiteral("West River Road");
            } catch (SQLException ex) {
                if (!ex.getMessage().contains("DSRA9110E"))
                    throw ex;
            }

            try {
                pstmt.enquoteNCharLiteral("IBM's Parking Lot");
            } catch (SQLException ex) {
                if (!ex.getMessage().contains("DSRA9110E"))
                    throw ex;
            }

            try {
                cstmt.enquoteNCharLiteral("'41st Street'");
            } catch (SQLException ex) {
                if (!ex.getMessage().contains("DSRA9110E"))
                    throw ex;
            }

        } finally {
            con.close();
        }
    }

    /**
     * Supply invalid sharding key to the setShardingKeyIfValid methods and verify that the
     * method returns false and the sharding keys are not set.
     */
    @Test
    public void testInvalidShardingKey() throws Exception {
        ShardingKeyBuilder keybuilder1 = unsharableXADataSource.createShardingKeyBuilder();
        keybuilder1.subkey("ISK1", JDBCType.LONGVARCHAR);
        ShardingKey validKey1 = keybuilder1.build();

        ShardingKeyBuilder keybuilder2 = unsharableXADataSource.createShardingKeyBuilder();
        keybuilder2.subkey("ISK2", JDBCType.LONGNVARCHAR);
        ShardingKey validKey2 = keybuilder2.build();

        ShardingKeyBuilder keybuilder3 = unsharableXADataSource.createShardingKeyBuilder();
        keybuilder3.subkey("ISK3", JDBCType.OTHER);
        ShardingKey validKey3 = keybuilder3.build();

        keybuilder1.subkey("INVALID", JDBCType.BOOLEAN); // Mock JDBC driver will consider this key invalid due to this subkey
        ShardingKey invalidKey = keybuilder1.build();

        String k;

        ConnectionBuilder conbuilder = unsharableXADataSource.createConnectionBuilder();
        Connection con = conbuilder.shardingKey(validKey1).superShardingKey(validKey2).build();
        try {
            assertFalse(con.setShardingKeyIfValid(invalidKey, 101));

            // values must not change
            k = con.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|LONGVARCHAR:ISK1;"));
            k = con.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|LONGNVARCHAR:ISK2;"));

            assertFalse(con.setShardingKeyIfValid(invalidKey, validKey3, 102));

            // values must not change
            k = con.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|LONGVARCHAR:ISK1;"));
            k = con.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|LONGNVARCHAR:ISK2;"));

            assertFalse(con.setShardingKeyIfValid(validKey3, invalidKey, 103));

            // values must not change
            k = con.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|LONGVARCHAR:ISK1;"));
            k = con.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|LONGNVARCHAR:ISK2;"));

            assertFalse(con.setShardingKeyIfValid(invalidKey, invalidKey, 104));

            // values must not change
            k = con.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|LONGVARCHAR:ISK1;"));
            k = con.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|LONGNVARCHAR:ISK2;"));

            assertFalse(con.setShardingKeyIfValid(null, invalidKey, 105));

            // values must not change
            k = con.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|LONGVARCHAR:ISK1;"));
            k = con.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|LONGNVARCHAR:ISK2;"));

            assertFalse(con.setShardingKeyIfValid(invalidKey, null, 106));

            // values must not change
            k = con.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|LONGVARCHAR:ISK1;"));
            k = con.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|LONGNVARCHAR:ISK2;"));

            assertTrue(con.setShardingKeyIfValid(validKey3, 107));

            k = con.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|OTHER:ISK3;"));
            k = con.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|LONGNVARCHAR:ISK2;"));

            assertTrue(con.setShardingKeyIfValid(validKey2, validKey1, 108));

            k = con.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|LONGNVARCHAR:ISK2;"));
            k = con.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|LONGVARCHAR:ISK1;"));

            assertTrue(con.setShardingKeyIfValid(validKey3, null, 109));

            k = con.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|OTHER:ISK3;"));
            k = con.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertNull(k);

            assertTrue(con.setShardingKeyIfValid(null, 110));
            k = con.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertNull(k);
            k = con.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertNull(k);
        } finally {
            con.close();
        }
    }

    /**
     * Tests that the new JDBC 4.3 method isSimpleIdentifier is functional when using the default implementations.
     */
    @Test
    public void testIsSimpleIdentifier() throws Exception {
        Connection con = defaultDataSource.getConnection();
        try {
            Statement stmt = con.createStatement();
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            CallableStatement cstmt = con.prepareCall("CALL SYSCS_UTIL.SYSCS_EMPTY_STATEMENT_CACHE()");

            assertTrue(stmt.isSimpleIdentifier("ValidSQLIdentifier"));
            assertFalse(pstmt.isSimpleIdentifier("Invalid SQL Identifier"));
            assertFalse(cstmt.isSimpleIdentifier("$Invalid"));
        } finally {
            con.close();
        }
    }

    /**
     * Use a JDBC 4.2 driver with the Liberty JDBC 4.3 feature enabled.
     * It should still be usable, but of course won't have any of the JDBC 4.3 function such as
     * sharding that needs to be implemented by the JDBC driver.
     * JDBC 4.3 methods for which Java SE provides default implementations must be invokable
     * and must return results that are consistent with the default implementation.
     */
    @ExpectedFFDC({ "java.sql.SQLFeatureNotSupportedException", // when attempting JDBC 4.3 methods on the JDBC 4.2 driver
                    "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", // when attempting to use connection builder with sharding
                    "javax.resource.spi.ResourceAllocationException" // when attempting to use connection builder with sharding
    })
    @Test
    public void testJDBC42DriverWithJDBC43FeatureEnabled() throws Exception {
        // use another data source to get a sharding key that we will later attempt to set on the connection
        ShardingKey key = sharableXADataSource.createShardingKeyBuilder().subkey("1", JDBCType.BIT).build();

        // Connection builder is implemented by the Liberty JDBC 4.3 feature,
        // and so is usable, as long as sharding isn't specified
        ConnectionBuilder conbuilder = derbyJDBC42XADataSource.createConnectionBuilder();
        try (Connection con = conbuilder.build()) {
            DatabaseMetaData mdata = con.getMetaData();
            assertEquals(4, mdata.getJDBCMajorVersion());
            assertEquals(2, mdata.getJDBCMinorVersion());
            assertFalse(mdata.supportsSharding());

            // Use a JDBC 4.1 method
            assertEquals("USER43", con.getSchema().toUpperCase());

            // per JavaDoc, the default implementation of beginRequest a no-op, and therefore it should not raise an error
            con.beginRequest();

            // default implementations provided by Java SE for these methods apply even if driver does not support JDBC 4.3,
            PreparedStatement ps = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            assertEquals("\"lessThan4'x8'\"", ps.enquoteIdentifier("lessThan4'x8'", false));
            assertTrue(ps.isSimpleIdentifier("OUT_OF_STOCK"));

            try {
                con.setShardingKey(key);
                fail("Should not be able to set a sharding key on a JDBC 4.2 driver");
            } catch (SQLFeatureNotSupportedException x) {
            }

            try {
                con.setShardingKey(key, key);
                fail("Should not be able to set a sharding key and super sharding key on a JDBC 4.2 driver");
            } catch (SQLFeatureNotSupportedException x) {
            }

            try {
                con.setShardingKeyIfValid(key, 150);
                fail("Should not be able to validate and set a sharding key on a JDBC 4.2 driver");
            } catch (SQLFeatureNotSupportedException x) {
            }

            try {
                con.setShardingKeyIfValid(key, key, 151);
                fail("Should not be able to validate and set a sharding key and super sharding key on a JDBC 4.2 driver");
            } catch (SQLFeatureNotSupportedException x) {
            }

            ps.setString(1, "Prairie Vista Drive NW");
            ps.setString(2, "Rochester");
            ps.setString(3, "MN");

            // use a JDBC 4.2 method
            assertEquals(1, ps.executeLargeUpdate());

            ps.close();

            // per JavaDoc, the default implementation of endRequest a no-op, and therefore it should not raise an error
            con.endRequest();
        }

        try {
            ShardingKeyBuilder keybuilder = derbyJDBC42XADataSource.createShardingKeyBuilder();
            fail("Created sharding key builder " + keybuilder + " for JDBC 4.2 driver");
        } catch (SQLFeatureNotSupportedException x) {
        }

        conbuilder.shardingKey(key);
        try {
            Connection con = conbuilder.build();
            con.close();
            fail("Built a connection with sharding " + con + " for JDBC 4.2 driver");
        } catch (SQLFeatureNotSupportedException x) {
        }
    }

    /**
     * Verify that that DatabaseMetaData indicates spec version 4.3 and that the method supports sharding is functional
     */
    @Test
    public void testMetaData() throws Exception {
        Connection con = defaultDataSource.getConnection();
        try {
            DatabaseMetaData mdata = con.getMetaData();

            assertEquals(4, mdata.getJDBCMajorVersion());
            assertEquals(3, mdata.getJDBCMinorVersion());

            assertTrue(mdata.supportsSharding());
        } finally {
            con.close();
        }
    }

    /**
     * Verify that within a global transaction, a single request is made that covers all shared handles.
     * In this test, the shared handles are open at the same time.
     */
    @Test
    public void testMultipleOpenSharableHandlesInTransaction() throws Exception {
        AtomicInteger[] requests;
        int begins = -1000, ends = -1000;

        tx.begin();
        try {
            Connection con1 = defaultDataSource.getConnection();
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "North Broadway Avenue");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();

            Connection con2 = defaultDataSource.getConnection();
            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps2.setString(1, "South Broadway Avenue");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
            ps2.close();

            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());

            @SuppressWarnings("unused")
            Connection con3 = defaultDataSource.getConnection();
            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());
        } finally {
            tx.commit();
        }

        assertEquals(begins, requests[BEGIN].get());
        assertEquals(ends + 1, requests[END].get());
    }

    /**
     * Verify that within a global transaction, a request is made per unshared connection,
     * and those requests remain open until the connection is closed and the transaction ends,
     * regardless of whether the close happens within the transaction or after it.
     */
    @Test
    public void testMultipleUnsharedConnectionsInTransaction() throws Exception {
        boolean successful = false;
        Connection con1 = null, con2 = null;
        AtomicInteger[] requests1 = null, requests2 = null, requests3 = null;
        int begin1 = -1000, end1 = -1000;
        int begin2 = -2000, end2 = -2000;
        int begin3 = -3000, end3 = -3000;

        tx.begin();
        try {
            con1 = unsharableXADataSource.getConnection();
            requests1 = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begin1 = requests1[BEGIN].get();
            end1 = requests1[END].get();
            assertEquals(end1 + 1, begin1);

            PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "Salem Road SW");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();

            con2 = unsharableXADataSource.getConnection();
            requests2 = (AtomicInteger[]) con2.unwrap(Supplier.class).get();
            begin2 = requests2[BEGIN].get();
            end2 = requests2[END].get();
            assertEquals(end2 + 1, begin2);

            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps2.setString(1, "Assisi Drive NW");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
            ps2.close();

            Connection con3 = unsharableXADataSource.getConnection();
            try {
                requests3 = (AtomicInteger[]) con3.unwrap(Supplier.class).get();
                begin3 = requests3[BEGIN].get();
                end3 = requests3[END].get();
                assertEquals(end3 + 1, begin3);
            } finally {
                con3.close();
            }

            // all requests remain open for the duration of the transaction
            assertEquals(requests3[BEGIN].get(), begin3);
            end3 = requests3[END].get();
            assertEquals(end3 + 1, begin3);

            end2 = requests2[END].get();
            assertEquals(end2 + 1, begin2);

            end1 = requests1[END].get();
            assertEquals(end1 + 1, begin1);

            successful = true;
        } finally {
            tx.commit();

            try {
                if (successful) {
                    successful = false;

                    // third requests ends after commit due to closed connection
                    assertEquals(requests3[BEGIN].get(), begin3);
                    end3 = requests3[END].get();
                    assertEquals(end3, begin3);

                    // other requests remain open across transaction boundary
                    assertEquals(requests1[BEGIN].get(), begin1);
                    end1 = requests1[END].get();
                    assertEquals(end1 + 1, begin1);

                    assertEquals(requests2[BEGIN].get(), begin2);
                    end2 = requests2[END].get();
                    assertEquals(end2 + 1, begin2);

                    successful = true;
                }
            } finally {
                con1.close();

                try {
                    if (successful) {
                        successful = false;

                        // first request ends due to closed connection
                        assertEquals(requests1[BEGIN].get(), begin1);
                        end1 = requests1[END].get();
                        assertEquals(end1, begin1);

                        // second request still open due to connection remaining open
                        assertEquals(requests2[BEGIN].get(), begin2);
                        end2 = requests2[END].get();
                        assertEquals(end2 + 1, begin2);

                        successful = true;
                    }
                } finally {
                    con2.close();

                    if (successful) {
                        // second request ends due to closed connection
                        assertEquals(requests2[BEGIN].get(), begin2);
                        end2 = requests2[END].get();
                        assertEquals(end2, begin2);
                    }
                }
            }
        }
    }

    /**
     * Abort a sharable connection from a thread other than the thread that is using it.
     * Verify that endRequest is invoked on the JDBC driver.
     */
    //@AllowedFFDC("javax.transaction.xa.XAException") // seen by transaction manager for aborted connection
    @Test
    public void testOtherThreadAbortSharable() throws Exception {
        AtomicInteger[] requests;
        int begins, ends;

        Connection con1 = sharableXADataSource.getConnection();
        try {
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            //con1.setAutoCommit(false); // TODO enable once abort path is fixed
            PreparedStatement ps = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps.setString(1, "Overland Drive NW");
            ps.setString(2, "Rochester");
            ps.setString(3, "MN");
            ps.executeUpdate();
        } finally {
            con1.close();
        }

        assertEquals(ends, requests[END].get());

        Connection con2 = sharableXADataSource.getConnection();
        try {
            AtomicInteger[] requests2 = (AtomicInteger[]) con2.unwrap(Supplier.class).get();
            assertSame(requests[BEGIN], requests2[BEGIN]);
            assertSame(requests[END], requests2[END]);

            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps2.setString(1, "Essex Parkway NW");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
        } finally {
            singleThreadExecutor.submit(new Callable<Void>() {
                @Override
                public Void call() throws SQLException {
                    con2.abort(singleThreadExecutor);

                    // Per the JavaDoc, abort (invoked previously) closes a connection
                    // and abort on an already-closed connection must be a no-op
                    con2.abort(singleThreadExecutor);

                    return null;
                }
            }).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        }

        tx.begin();
        try {
            assertEquals(ends + 1, requests[END].get());

            Connection con3 = sharableXADataSource.getConnection();
            requests = (AtomicInteger[]) con3.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            PreparedStatement ps3 = con3.prepareStatement("SELECT CITY, STATE FROM STREETS WHERE NAME = ?");
            ps3.setString(1, "Overland Drive NW");
            ResultSet result = ps3.executeQuery();
            //assertFalse(result.next()); // TODO enable once prior TODOs are removed
            ps3.close();

            Connection con4 = sharableXADataSource.getConnection();
            PreparedStatement ps4 = con4.prepareStatement("SELECT CITY, STATE FROM STREETS WHERE NAME = ?");
            ps4.setString(1, "Essex Parkway NW");
            result = ps4.executeQuery();
            //assertFalse(result.next()); // TODO enable once prior TODOs are removed

            //con4.close(); // TODO replace with the following once the abort path is fixed to invoke ManagedConnection.destroy when the transaction ends.
            //singleThreadExecutor.submit(new Callable<Void>() {
            //    @Override
            //    public Void call() throws SQLException {
            //        con4.abort(singleThreadExecutor);
            //        return null;
            //    }
            //}).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        } finally {
            tx.rollback();
        }

        assertEquals(ends + 1, requests[END].get());
    }

    /**
     * Abort an unsharable connection from a thread other than the thread that is using it.
     * Verify that endRequest is invoked on the JDBC driver in response to the abort.
     */
    //@AllowedFFDC("javax.transaction.xa.XAException") // seen by transaction manager for aborted connection
    @Test
    public void testOtherThreadAbortUnsharable() throws Exception {
        AtomicInteger[] requests;
        int begins, ends;

        final Connection con1 = unsharableXADataSource.getConnection();
        try {
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            //con1.setAutoCommit(false); //TODO enable once abort path is fixed
            PreparedStatement ps = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps.setString(1, "Badger Hills Drive NW");
            ps.setString(2, "Rochester");
            ps.setString(3, "MN");
            ps.executeUpdate();
        } finally {
            singleThreadExecutor.submit(new Callable<Void>() {
                @Override
                public Void call() throws SQLException {
                    con1.abort(singleThreadExecutor);
                    return null;
                }
            }).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        }

        // TODO Why is crossing the LTC boundary needed for ManagedConnection.destroy/Connection.endRequest to be invoked after abort?
        tx.begin();
        try {
            assertEquals(ends + 1, requests[END].get()); // TODO move the assert before tx.begin once abort path is fixed

            final Connection con2 = unsharableXADataSource.getConnection();
            try {
                requests = (AtomicInteger[]) con2.unwrap(Supplier.class).get();
                begins = requests[BEGIN].get();
                ends = requests[END].get();
                assertEquals(ends + 1, begins);

                PreparedStatement ps2 = con2.prepareStatement("SELECT CITY, STATE FROM STREETS WHERE NAME = ?");
                ps2.setString(1, "Badger Hills Drive NW");
                ResultSet result = ps2.executeQuery();
                // assertFalse(result.next()); //TODO enable once prior TODOs are removed
            } finally {
                con2.close(); // TODO replace with the following once the abort path is fixed such that
                // ManagedConnection.destroy is invoked either upon abort or when the transaction ends.
                // Currently, destroy is not invoked until server shutdown!
                //singleThreadExecutor.submit(new Callable<Void>() {
                //    @Override
                //    public Void call() throws SQLException {
                //        con2.abort(singleThreadExecutor);
                //        return null;
                //    }
                //}).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            }
        } finally {
            tx.rollback();
        }

        assertEquals(ends + 1, requests[END].get());
    }

    /**
     * Verify that connection builder can be used on a Liberty data source that is backed by a
     * javax.sql.ConnectionPoolDataSource implementation. This test only does matching on ShardingKey.
     * It does not cover user, password, or super sharding key.
     */
    @Test
    public void testPooledConnectionBuilderMatchShardingKey() throws Exception {
        ShardingKeyBuilder keybuilderA = sharablePool1DataSourceWithAppAuth.createShardingKeyBuilder();
        ShardingKeyBuilder keybuilderB = sharablePool1DataSourceWithAppAuth.createShardingKeyBuilder();
        ShardingKey keyA = keybuilderA.subkey("PCBKey", JDBCType.CLOB).build();
        ShardingKey keyB = keybuilderB.subkey("PCBKey", JDBCType.CLOB).build();
        ConnectionBuilder conbuilderA = sharablePool1DataSourceWithAppAuth.createConnectionBuilder().shardingKey(keyA);
        ConnectionBuilder conbuilderB = sharablePool1DataSourceWithAppAuth.createConnectionBuilder().shardingKey(keyB);

        tx.begin();
        try {
            Connection con1 = conbuilderA.build();
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con1.getTransactionIsolation());
            con1.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            String k1 = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k1, k1.endsWith("|CLOB:PCBKey;"));

            Connection con2 = conbuilderB.build();
            // If connection handle is shared, it will report the isolation level value of con1,
            assertEquals(Connection.TRANSACTION_SERIALIZABLE, con2.getTransactionIsolation());
            String k2 = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k2, k2.endsWith("|CLOB:PCBKey;"));

            con2.close();
            con1.close();
        } finally {
            tx.commit();
        }

        // Specify a different sharding key on the builder,
        ShardingKeyBuilder keybuilderC = sharablePool1DataSourceWithAppAuth.createShardingKeyBuilder();
        ShardingKey key3 = keybuilderC.subkey("PCBKey", JDBCType.NCLOB).build();
        Connection con3 = conbuilderA.shardingKey(key3).build();
        try {
            String k3 = con3.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k3, k3.endsWith("|NCLOB:PCBKey;"));
        } finally {
            con3.close();
        }
    }

    /**
     * Verify that user and password supplied via connection builder are used when sharing a PooledConnection,
     * as well as when requesting a new PooledConnection.
     */
    @Test
    public void testPooledConnectionBuilderMatchUserPassword() throws Exception {
        ConnectionBuilder builderA = sharablePool1DataSourceWithAppAuth.createConnectionBuilder();
        ConnectionBuilder builderA1 = builderA.user("user43").password("pwd43");

        // ensure same instance returned, as required by ConnectionBuilder JavaDoc
        assertSame(builderA, builderA1);

        tx.begin();
        try {
            Connection con1 = builderA.build();
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con1.getTransactionIsolation());
            con1.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            // different builder instance must be returned
            ConnectionBuilder builderB = sharablePool1DataSourceWithAppAuth.createConnectionBuilder();
            assertNotSame(builderA, builderB);

            Connection con2 = builderB.user("user43").password("pwd43").build();
            // If connection handle is shared, it will report the isolation level value of con1,
            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con2.getTransactionIsolation());
            assertEquals("user43", con2.getMetaData().getUserName());

            // Show that the user name is honored by accessing data that was previously written by this user
            PreparedStatement ps2 = con2.prepareStatement("SELECT NAME FROM STREETS WHERE NAME=? AND CITY=? AND STATE=?");
            ps2.setString(1, "Valleyhigh Drive NW");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ResultSet result2 = ps2.executeQuery();
            assertTrue(result2.next());
            ps2.close();

            // Request another matching connection via the same builder
            Connection con3 = builderA.build();
            // If connection handle is shared, it will report the isolation level value of con1,
            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con3.getTransactionIsolation());
            assertEquals("user43", con3.getMetaData().getUserName());

            con3.close();
            con2.close();
            con1.close();
        } finally {
            tx.commit();
        }

        // Clear the user/password that were specified on the builder,
        Connection con5 = builderA.user(null).password(null).build();
        try {
            // User name should be vendor-specific default (which is APP for the Derby driver
            // upon which our mock JDBC driver is built)
            assertEquals("APP", con5.getMetaData().getUserName());
        } finally {
            con5.close();
        }
    }

    /**
     * Have multiple handles crossover sharing scopes, where outside of a global transaction, they do not share
     * a connection, but within a global transaction, they do. Ensure that sharding keys are properly preserved
     * and matched.
     */
    @Test
    public void testReassociationWithShardingKey() throws Exception {
        ShardingKey key1 = dataSourceWithAppAuth.createShardingKeyBuilder().subkey("RWSK1", JDBCType.VARBINARY).build();
        ShardingKey key2 = dataSourceWithAppAuth.createShardingKeyBuilder().subkey("RWSK2", JDBCType.BLOB).build();
        ShardingKey key3 = dataSourceWithAppAuth.createShardingKeyBuilder().subkey("RWSK3", JDBCType.BINARY).build();
        String k;

        Connection con1 = null;
        Connection con2 = null;
        try {
            con1 = dataSourceWithAppAuth.createConnectionBuilder()
                            .user("user43")
                            .password("pwd43")
                            .shardingKey(key1)
                            .superShardingKey(key2)
                            .build();
            con1.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            con2 = dataSourceWithAppAuth.createConnectionBuilder()
                            .user("user43")
                            .password("pwd43")
                            .build();
            assertTrue(con2.setShardingKeyIfValid(key1, key2, 130));
            con2.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            tx.begin();
            try {
                con1.createStatement().close();
                assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con1.getTransactionIsolation());
                k = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
                assertTrue(k, k.endsWith("|VARBINARY:RWSK1;"));
                k = con1.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
                assertTrue(k, k.endsWith("|BLOB:RWSK2;"));

                con2.createStatement().close();
                assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con2.getTransactionIsolation());
                k = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
                assertTrue(k, k.endsWith("|VARBINARY:RWSK1;"));
                k = con2.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
                assertTrue(k, k.endsWith("|BLOB:RWSK2;"));

                assertSame(((Object[]) con1.unwrap(Supplier.class).get())[0],
                           ((Object[]) con2.unwrap(Supplier.class).get())[0]);
            } finally {
                tx.rollback();
            }

            // Outside of global transaction, must use separate connections
            assertNotSame(((Object[]) con1.unwrap(Supplier.class).get())[0],
                          ((Object[]) con2.unwrap(Supplier.class).get())[0]);

            tx.begin();
            try {
                con1.createStatement().close();
                assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con1.getTransactionIsolation());
                con1.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                assertTrue(con1.setShardingKeyIfValid(key3, 131));

                con2.createStatement().close();
                // matching is based on original request, not current state
                assertEquals(Connection.TRANSACTION_REPEATABLE_READ, con2.getTransactionIsolation());
                k = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
                assertTrue(k, k.endsWith("|BINARY:RWSK3;"));
                k = con2.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
                assertTrue(k, k.endsWith("|BLOB:RWSK2;"));

                assertSame(((Object[]) con1.unwrap(Supplier.class).get())[0],
                           ((Object[]) con2.unwrap(Supplier.class).get())[0]);
            } finally {
                tx.rollback();
            }

            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, con2.getTransactionIsolation());
            k = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|BINARY:RWSK3;"));
            k = con2.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|BLOB:RWSK2;"));
        } finally {
            if (con1 != null)
                con1.close();
            if (con2 != null)
                con2.close();
        }
    }

    /**
     * Abort a sharable connection from the same thread that is using it.
     * Verify that endRequest is invoked on the JDBC driver.
     */
    @AllowedFFDC("javax.transaction.xa.XAException") // seen by transaction manager for aborted connection
    @Test
    public void testSameThreadAbortSharable() throws Exception {
        AtomicInteger[] requests;
        int begins, ends;

        Connection con1 = defaultDataSource.getConnection();
        try {
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            // con1.setAutoCommit(false); // TODO enable once abort path is fixed
            PreparedStatement ps = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps.setString(1, "Superior Drive NW");
            ps.setString(2, "Rochester");
            ps.setString(3, "MN");
            ps.executeUpdate();
        } finally {
            con1.close();
        }

        assertEquals(ends, requests[END].get());

        Connection con2 = defaultDataSource.getConnection();
        try {
            AtomicInteger[] requests2 = (AtomicInteger[]) con2.unwrap(Supplier.class).get();
            assertSame(requests[BEGIN], requests2[BEGIN]);
            assertSame(requests[END], requests2[END]);

            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps2.setString(1, "Technology Drive NW");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
        } finally {
            con2.abort(singleThreadExecutor);
        }

        tx.begin();
        try {
            assertEquals(ends + 1, requests[END].get());

            Connection con3 = defaultDataSource.getConnection();
            requests = (AtomicInteger[]) con3.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            PreparedStatement ps3 = con3.prepareStatement("SELECT CITY, STATE FROM STREETS WHERE NAME = ?");
            ps3.setString(1, "Superior Drive NW");
            ResultSet result = ps3.executeQuery();
            // assertFalse(result.next()); // TODO enable once prior TODOs are removed
            ps3.close();

            Connection con4 = defaultDataSource.getConnection();
            PreparedStatement ps4 = con4.prepareStatement("SELECT CITY, STATE FROM STREETS WHERE NAME = ?");
            ps4.setString(1, "Technology Drive NW");
            result = ps4.executeQuery();
            // assertFalse(result.next()); // TODO enable once prior TODOs are removed

            con4.abort(singleThreadExecutor);
        } finally {
            tx.rollback();
        }

        assertEquals(ends + 1, requests[END].get());
    }

    /**
     * Abort an unsharable connection from the same thread that is using it.
     * Verify that endRequest is invoked on the JDBC driver in response to the abort.
     */
    @AllowedFFDC("javax.transaction.xa.XAException") // seen by transaction manager for aborted connection
    @Test
    public void testSameThreadAbortUnsharable() throws Exception {
        AtomicInteger[] requests;
        int begins, ends;

        Connection con1 = unsharablePool1DataSource.getConnection();
        try {
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            // con1.setAutoCommit(false); TODO enable once abort path is fixed
            PreparedStatement ps = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps.setString(1, "Commerce Drive NW");
            ps.setString(2, "Rochester");
            ps.setString(3, "MN");
            ps.executeUpdate();
        } finally {
            con1.abort(singleThreadExecutor);
        }

        // TODO Why is crossing the LTC boundary needed for ManagedConnection.destroy/Connection.endRequest to be invoked after abort?
        tx.begin();
        try {
            assertEquals(ends + 1, requests[END].get()); // TODO move the assert before tx.begin once abort path is fixed

            Connection con2 = unsharablePool1DataSource.getConnection();
            try {
                requests = (AtomicInteger[]) con2.unwrap(Supplier.class).get();
                begins = requests[BEGIN].get();
                ends = requests[END].get();
                assertEquals(ends + 1, begins);

                PreparedStatement ps2 = con2.prepareStatement("SELECT CITY, STATE FROM STREETS WHERE NAME = ?");
                ps2.setString(1, "Commerce Drive NW");
                ResultSet result = ps2.executeQuery();
                // assertFalse(result.next()); TODO enable once prior TODOs are removed
            } finally {
                con2.abort(singleThreadExecutor);
            }
        } finally {
            tx.rollback();
        }

        assertEquals(ends + 1, requests[END].get());
    }

    /**
     * Verify that within a global transaction, a single request is used across get/use/close of all shared handles.
     */
    @Test
    public void testSerialReuseInGlobalTransaction() throws Exception {
        AtomicInteger[] requests;
        int begins = -1000, ends = -1000;

        Connection con3;
        tx.begin();
        try {
            Connection con1 = defaultDataSource.getConnection();
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "East River Road");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();
            con1.close();

            Connection con2 = defaultDataSource.getConnection();
            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps2.setString(1, "West River Road");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
            ps2.close();
            con2.close();

            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());

            con3 = defaultDataSource.getConnection();
            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());
        } finally {
            tx.commit();
        }

        assertEquals(begins, requests[BEGIN].get());
        assertEquals(ends + 1, requests[END].get());

        tx.begin();
        try {
            // might use different managed connection instance
            PreparedStatement ps3 = con3.prepareStatement("UPDATE STREETS SET NAME=? WHERE NAME=? AND CITY=? AND STATE=?");
            requests = (AtomicInteger[]) con3.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            ps3.setString(1, "West River Parkway");
            ps3.setString(2, "West River Road");
            ps3.setString(3, "Rochester");
            ps3.setString(4, "MN");
            ps3.executeUpdate();
            ps3.close();

            assertEquals(ends + 1, begins);
        } finally {
            tx.commit();
        }

        assertEquals(begins, requests[BEGIN].get());
        assertEquals(ends + 1, requests[END].get());
    }

    /**
     * Verify that within an LTC, a single request is used across get/use/close of all shared handles.
     */
    @Test
    public void testSerialReuseInLTC() throws Exception {
        AtomicInteger[] requests;
        int begins = -1000, ends = -1000;

        Connection con1 = defaultDataSource.getConnection();
        try {
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "East Center Street");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();
        } finally {
            con1.close();
        }

        Connection con2 = defaultDataSource.getConnection();
        try {
            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps2.setString(1, "W Center Street");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
            ps2.close();

            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());
        } finally {
            con2.close();
        }

        Connection con3 = defaultDataSource.getConnection();
        try {
            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());

            // end the LTC
            tx.begin();
            tx.commit();

            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends + 1, requests[END].get());

            // new LTC, might use different managed connection instance
            PreparedStatement ps3 = con3.prepareStatement("UPDATE STREETS SET NAME=? WHERE NAME=? AND CITY=? AND STATE=?");
            requests = (AtomicInteger[]) con3.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            ps3.setString(1, "West Center Street");
            ps3.setString(2, "W Center Street");
            ps3.setString(3, "Rochester");
            ps3.setString(4, "MN");
            ps3.executeUpdate();
            ps3.close();
        } finally {
            con3.close();
        }

        // end the second LTC
        tx.begin();
        tx.commit();

        assertEquals(begins, requests[BEGIN].get());
        assertEquals(ends + 1, requests[END].get());
    }

    /**
     * Verify that within a global transaction, separate requests are used for each unshared connection,
     * and that these requests only end when both the connection is closed AND the global transaction has ended.
     */
    @Test
    public void testSerialUnsharedInGlobalTransaction() throws Exception {
        AtomicInteger[] requests1 = null, requests2 = null, requests3 = null;
        int begin1 = -1000, end1 = -1000;
        int begin2 = -2000, end2 = -2000;
        int begin3 = -3000, end3 = -3000;

        boolean successful = false;
        Connection con3 = null;
        tx.begin();
        try {
            Connection con1 = unsharableXADataSource.getConnection();
            try {
                requests1 = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
                begin1 = requests1[BEGIN].get();
                end1 = requests1[END].get();
                assertEquals(end1 + 1, begin1);

                PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps1.setString(1, "Bamber Valley Road SW");
                ps1.setString(2, "Rochester");
                ps1.setString(3, "MN");
                ps1.executeUpdate();
                ps1.close();
            } finally {
                con1.close();
            }

            Connection con2 = unsharableXADataSource.getConnection();
            try {
                PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps2.setString(1, "Mayowood Road SW");
                ps2.setString(2, "Rochester");
                ps2.setString(3, "MN");
                ps2.executeUpdate();
                ps2.close();

                requests2 = (AtomicInteger[]) con2.unwrap(Supplier.class).get();
                begin2 = requests2[BEGIN].get();
                end2 = requests2[END].get();
                assertEquals(end2 + 1, begin2);
            } finally {
                con2.close();
            }

            con3 = unsharableXADataSource.getConnection();

            requests3 = (AtomicInteger[]) con3.unwrap(Supplier.class).get();
            begin3 = requests3[BEGIN].get();
            end3 = requests3[END].get();
            assertEquals(end3 + 1, begin3);

            successful = true;
        } finally {
            tx.commit();

            if (!successful && con3 != null)
                con3.close();
        }

        try {
            // first two requests should end after commit because their connections were closed
            assertEquals(end1 + 1, requests1[END].get());
            assertEquals(end2 + 1, requests2[END].get());

            // third request must not end because the connection remains open
            assertEquals(end3, requests3[END].get());

            tx.begin();
            try {
                // same managed connection instance is used here
                PreparedStatement ps3 = con3.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                assertEquals(begin3, requests3[BEGIN].get());
                assertEquals(end3, requests3[END].get());

                ps3.setString(1, "Collegeview Road SE");
                ps3.setString(2, "Rochester");
                ps3.setString(3, "MN");
                ps3.executeUpdate();
                ps3.close();

                assertEquals(end3, requests3[END].get());
            } finally {
                tx.commit();
            }
        } finally {
            con3.close();
        }
        assertEquals(end3 + 1, requests3[END].get());
    }

    /**
     * Verify that within an LTC, a separate request is used for get/use/close of each unshared handle.
     */
    @Test
    public void testSerialUnsharedInLTC() throws Exception {
        AtomicInteger[] requests;
        int begins = -1000, ends = -1000;

        Connection con1 = unsharablePool1DataSource.createConnectionBuilder().build();
        try {
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "Silver Creek Road NE");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();
        } finally {
            con1.close();
        }
        assertEquals(ends + 1, requests[END].get());

        // same connection must be returned from the pool because pool size is 1
        Connection con2 = unsharablePool1DataSource.getConnection();
        try {
            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps2.setString(1, "Marion Road SE");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
            ps2.close();

            assertEquals(begins + 1, requests[BEGIN].get());
            assertEquals(ends + 1, requests[END].get());
        } finally {
            con2.close();
        }
        assertEquals(ends + 2, requests[END].get());

        // Again, same connection must be returned from the pool because pool size is 1
        Connection con3 = unsharablePool1DataSource.getConnection();
        try {
            assertEquals(begins + 2, requests[BEGIN].get());
            assertEquals(ends + 2, requests[END].get());

            // end the LTC
            tx.begin();
            tx.commit();

            assertEquals(begins + 2, requests[BEGIN].get());
            assertEquals(ends + 2, requests[END].get());

            // new LTC, but same request
            PreparedStatement ps3 = con3.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            assertEquals(begins + 2, requests[BEGIN].get());
            assertEquals(ends + 2, requests[END].get());

            ps3.setString(1, "Pinewood Road SE");
            ps3.setString(2, "Rochester");
            ps3.setString(3, "MN");
            ps3.executeUpdate();
            ps3.close();
        } finally {
            con3.close();
        }
        assertEquals(ends + 3, requests[END].get());

        // end the second LTC
        tx.begin();
        tx.commit();

        assertEquals(begins + 2, requests[BEGIN].get());
        assertEquals(ends + 3, requests[END].get());
    }

    /**
     * When using a data source that is backed by java.sql.Driver, the ShardingKeyBuilder is unavailable,
     * and attempts to create it must result in SQLFeatureNotSupportedException.
     */
    @Test
    public void testShardingKeyBuilderNotAvailableWithDriver() throws Exception {
        try {
            ShardingKeyBuilder keybuilder = defaultDataSource.createShardingKeyBuilder();
            fail("Should not be able to create ShardingKeyBuilder when using java.sql.Driver. " + keybuilder);
        } catch (SQLFeatureNotSupportedException x) {
        }
    }

    /**
     * Test matching of sharding key only (super sharding key is null) based on current connection state.
     * This means that connection handles that are requested with attributes matching the current state
     * of a connection are considered to match and will share that connection, even if the connection
     * state has changed from the original connection request.
     */
    @Test
    public void testShardingKeyMatchCurrentState() throws Exception {
        ShardingKey key1 = sharablePoolDataSourceMatchCurrentState.createShardingKeyBuilder().subkey("1.1", JDBCType.DECIMAL).build();
        ShardingKey key2 = sharablePoolDataSourceMatchCurrentState.createShardingKeyBuilder().subkey("2018-10-02", JDBCType.DATE).build();
        ShardingKey superkey1 = sharablePoolDataSourceMatchCurrentState.createShardingKeyBuilder().subkey("<super>Key1</super>", JDBCType.SQLXML).build();
        String k;

        ConnectionBuilder builder1;

        tx.begin();
        try {
            builder1 = sharablePoolDataSourceMatchCurrentState.createConnectionBuilder();
            Connection con1 = builder1.user("user43").password("pwd43").shardingKey(key1).superShardingKey(superkey1).build();

            k = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|DECIMAL:1.1;"));
            k = con1.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|SQLXML:<super>Key1</super>;"));

            assertTrue(con1.setShardingKeyIfValid(key2, null, 120));
            con1.createStatement().executeQuery("VALUES 1").close();

            // Share the same connection based on current connection state
            ConnectionBuilder builder2 = sharablePoolDataSourceMatchCurrentState.createConnectionBuilder();
            Connection con2 = builder2.user("user43").password("pwd43").shardingKey(key2).build();

            con2.createStatement().executeQuery("VALUES 2").close();

            k = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|DATE:2018-10-02;"));
            k = con2.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertNull(k);

            con2.close();
            con1.close();
        } finally {
            tx.commit();
        }

        // Reuse builder instance
        Connection con3 = builder1.build();
        try {
            k = con3.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|DECIMAL:1.1;"));
            k = con3.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|SQLXML:<super>Key1</super>;"));
        } finally {
            con3.close();
        }
    }

    /**
     * Test matching of sharding key and super sharding key based on current connection state.
     * This means that connection handles that are requested with attributes matching the current state
     * of a connection are considered to match and will share that connection, even if the connection
     * state has changed from the original connection request.
     */
    @ExpectedFFDC("java.sql.SQLException") // for intentional sharing violation error caused by the test
    @Test
    public void testShardingKeysMatchCurrentState() throws Exception {
        ShardingKey key1 = sharablePoolDataSourceMatchCurrentState.createShardingKeyBuilder().subkey("SKMCS1", JDBCType.NCHAR).build();
        ShardingKey key2 = sharablePoolDataSourceMatchCurrentState.createShardingKeyBuilder().subkey("SKMCS2", JDBCType.NCHAR).build();
        ShardingKey key3 = sharablePoolDataSourceMatchCurrentState.createShardingKeyBuilder().subkey("SKMCS3", JDBCType.NCHAR).build();
        ShardingKey superkey1 = sharablePoolDataSourceMatchCurrentState.createShardingKeyBuilder().subkey("SUPER", JDBCType.NCLOB).build();
        ShardingKey superkey2 = sharablePoolDataSourceMatchCurrentState.createShardingKeyBuilder().subkey("SUPER", JDBCType.CHAR).build();
        ShardingKey superkey3 = sharablePoolDataSourceMatchCurrentState.createShardingKeyBuilder().subkey("SUPER", JDBCType.CLOB).build();
        String k;

        Connection con1;
        tx.begin();
        try {
            ConnectionBuilder builder1 = sharablePoolDataSourceMatchCurrentState.createConnectionBuilder();
            con1 = builder1.user("user43").password("pwd43").shardingKey(key1).superShardingKey(superkey1).build();

            k = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NCHAR:SKMCS1;"));
            k = con1.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|NCLOB:SUPER;"));

            con1.setShardingKey(key2, superkey2);
            con1.createStatement().executeQuery("VALUES 1").close();

            // Share the same connection based on current connection state
            ConnectionBuilder builder2 = sharablePoolDataSourceMatchCurrentState.createConnectionBuilder();
            Connection con2 = builder2.user("user43").password("pwd43").shardingKey(key2).superShardingKey(superkey2).build();

            con2.createStatement().executeQuery("VALUES 2").close();

            k = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NCHAR:SKMCS2;"));
            k = con2.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|CHAR:SUPER;"));

            // Cannot change it now because there are 2 handles open
            try {
                con1.setShardingKey(key1);
                fail("Should not be able to alter the sharding key of a shared connection");
            } catch (SQLException x) {
                if (x.getCause() == null || !"javax.resource.spi.SharingViolationException".equals(x.getCause().getClass().getName()))
                    throw x;
            }

            try {
                con1.setShardingKey(key1, superkey2);
                fail("Should not be able to alter the sharding key of a shared connection");
            } catch (SQLException x) {
                if (x.getCause() == null || !"javax.resource.spi.SharingViolationException".equals(x.getCause().getClass().getName()))
                    throw x;
            }

            try {
                con2.setShardingKey(key2, superkey1);
                fail("Should not be able to alter the super sharding key of a shared connection");
            } catch (SQLException x) {
                if (x.getCause() == null || !"javax.resource.spi.SharingViolationException".equals(x.getCause().getClass().getName()))
                    throw x;
            }

            try {
                con2.setShardingKey(key1, superkey1);
                fail("Should not be able to alter the sharding key and super sharding key of a shared connection");
            } catch (SQLException x) {
                if (x.getCause() == null || !"javax.resource.spi.SharingViolationException".equals(x.getCause().getClass().getName()))
                    throw x;
            }

            // set to current value is a no-op
            con2.setShardingKey(key2);
            con2.setShardingKey(key2, superkey2);

            con2.close();

            // only one connection handle remains, can change it now
            con1.setShardingKey(key3);

            k = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NCHAR:SKMCS3;"));
            k = con1.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|CHAR:SUPER;"));

            // can also change the super sharding key
            con1.setShardingKey(key3, superkey3);

            k = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NCHAR:SKMCS3;"));
            k = con1.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|CLOB:SUPER;"));

            // remove the super sharding key, but not the sharding key
            con1.setShardingKey(key3, null);

            k = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NCHAR:SKMCS3;"));
            assertNull(con1.getClientInfo("SUPER_SHARDING_KEY")); // extension by mock JDBC driver to determine the super sharding key

            // Match on current state again
            ConnectionBuilder builder3 = sharablePoolDataSourceMatchCurrentState.createConnectionBuilder();
            Connection con3 = builder3.user("user43").password("pwd43").shardingKey(key3).build();

            con3.createStatement().executeQuery("VALUES 3").close();

            k = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NCHAR:SKMCS3;"));
            assertNull(con1.getClientInfo("SUPER_SHARDING_KEY")); // extension by mock JDBC driver to determine the super sharding key

            k = con3.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NCHAR:SKMCS3;"));
            assertNull(con3.getClientInfo("SUPER_SHARDING_KEY")); // extension by mock JDBC driver to determine the super sharding key

            con3.close();

            con1.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            con1.setShardingKey(key2, superkey2);
        } finally {
            tx.commit();
        }

        // Connection handle reassociation across transaction boundary
        try {
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, con1.getTransactionIsolation());
            k = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NCHAR:SKMCS2;"));
            k = con1.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|CHAR:SUPER;"));
        } finally {
            con1.close();
        }

        // Non-matching request
        ConnectionBuilder builder4 = sharablePoolDataSourceMatchCurrentState.createConnectionBuilder();
        Connection con4 = builder4.user("user43").password("pwd43").build();
        try {
            assertNull(con4.getClientInfo("SHARDING_KEY"));
            assertNull(con4.getClientInfo("SUPER_SHARDING_KEY"));
        } finally {
            con4.close();
        }
    }

    /**
     * Test matching of sharding key and super sharding key based on original connection request.
     * This means that connection handles that were requested with the same attributes within the same
     * sharing scope are considered to match, even if the state of the underlying connection has changed
     * in between the requests.
     */
    @ExpectedFFDC("java.sql.SQLException") // for intentional sharing violation error caused by the test
    @Test
    public void testShardingKeyMatchOriginalRequest() throws Exception {
        ShardingKey key1 = sharablePool1DataSourceWithAppAuth.createShardingKeyBuilder().subkey("SKMOR1", JDBCType.NVARCHAR).build();
        ShardingKey key2 = sharablePool1DataSourceWithAppAuth.createShardingKeyBuilder().subkey("SKMOR2", JDBCType.NVARCHAR).build();
        ShardingKey key3 = sharablePool1DataSourceWithAppAuth.createShardingKeyBuilder().subkey("SKMOR3", JDBCType.NVARCHAR).build();
        ShardingKey superkey1 = sharablePool1DataSourceWithAppAuth.createShardingKeyBuilder().subkey("SUPER1", JDBCType.CHAR).build();
        ShardingKey superkey2 = sharablePool1DataSourceWithAppAuth.createShardingKeyBuilder().subkey("SUPER2", JDBCType.CHAR).build();
        ShardingKey superkey3 = sharablePool1DataSourceWithAppAuth.createShardingKeyBuilder().subkey("SUPER3", JDBCType.CHAR).build();

        AtomicInteger[] requests;
        int begins;

        tx.begin();
        try {
            Connection con1 = sharablePool1DataSourceWithAppAuth.getConnection("user43", "pwd43");
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            assertNull(con1.getClientInfo("SHARDING_KEY"));
            assertNull(con1.getClientInfo("SUPER_SHARDING_KEY"));

            con1.setShardingKey(key1, superkey1);

            String k;
            k = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NVARCHAR:SKMOR1;"));
            k = con1.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|CHAR:SUPER1;"));

            Connection con2 = sharablePool1DataSourceWithAppAuth.getConnection("user43", "pwd43");

            k = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NVARCHAR:SKMOR1;"));
            k = con2.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|CHAR:SUPER1;"));

            // set to current value is a no-op
            con2.setShardingKey(key1);
            con2.setShardingKey(key1, superkey1);

            try {
                con2.setShardingKey(key2);
                fail("Should not be able to alter the sharding key of a shared connection");
            } catch (SQLException x) {
                if (x.getCause() == null || !"javax.resource.spi.SharingViolationException".equals(x.getCause().getClass().getName()))
                    throw x;
            }

            try {
                con2.setShardingKey(key2, superkey1);
                fail("Should not be able to alter the sharding key of a shared connection");
            } catch (SQLException x) {
                if (x.getCause() == null || !"javax.resource.spi.SharingViolationException".equals(x.getCause().getClass().getName()))
                    throw x;
            }

            try {
                con2.setShardingKey(key1, superkey2);
                fail("Should not be able to alter the super sharding key of a shared connection");
            } catch (SQLException x) {
                if (x.getCause() == null || !"javax.resource.spi.SharingViolationException".equals(x.getCause().getClass().getName()))
                    throw x;
            }

            try {
                con2.setShardingKey(key2, superkey2);
                fail("Should not be able to alter the sharding key and super sharding key of a shared connection");
            } catch (SQLException x) {
                if (x.getCause() == null || !"javax.resource.spi.SharingViolationException".equals(x.getCause().getClass().getName()))
                    throw x;
            }

            // sharding key values should not have changed
            k = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NVARCHAR:SKMOR1;"));
            k = con2.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|CHAR:SUPER1;"));

            con1.close();

            // only one connection handle remains, can change it now
            con2.setShardingKey(key2);

            k = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NVARCHAR:SKMOR2;"));
            k = con2.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|CHAR:SUPER1;"));

            // can also change the super sharding key
            con2.setShardingKey(key3, superkey3);

            k = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|NVARCHAR:SKMOR3;"));
            k = con2.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|CHAR:SUPER3;"));

            con2.close();
        } finally {
            tx.commit();
        }

        // outside of sharing scope, the default values (null) apply
        Connection con3 = sharablePool1DataSourceWithAppAuth.getConnection("user43", "pwd43");
        try {
            assertNull(con3.getClientInfo("SHARDING_KEY"));
            assertNull(con3.getClientInfo("SUPER_SHARDING_KEY"));

            // verify that the same connection was found in the pool (which has maxPoolSize=1) and reused
            assertEquals(begins + 1, requests[BEGIN].get());

            con3.commit(); // TODO why is it that without this commit, a Derby error is recorded in FFDC when a
            // subsequent test needs to discard the connection as victim when creating a new matching connection?
            // ERROR 25001: Cannot close a connection while a transaction is still active
        } finally {
            con3.close();
        }
    }

    /**
     * A ShardingKey that is obtained from a data source that supports sharding cannot be supplied
     * to the connection builder for a data source that is backed by java.sql.Driver and therefore
     * does not support sharding. When this is attempted, it must result in SQLFeatureNotSupportedException.
     */
    @Test
    public void testShardingKeyNotUsableWithDriversConnectionBuilder() throws Exception {
        ShardingKeyBuilder keybuilder = sharablePool1DataSourceWithAppAuth.createShardingKeyBuilder();
        ShardingKey key = keybuilder.subkey(Arrays.asList(23, 41, 86, 17, 95), JDBCType.ARRAY).build();

        ConnectionBuilder conbuilder = defaultDataSource.createConnectionBuilder();

        try {
            conbuilder.shardingKey(key);
            fail("Should not be able to set sharding key on connection builder that is backed by java.sql.Driver. " + keybuilder);
        } catch (UnsupportedOperationException x) {
        }

        try {
            conbuilder.superShardingKey(key);
            fail("Should not be able to set super sharding key on connection builder that is backed by java.sql.Driver. " + keybuilder);
        } catch (UnsupportedOperationException x) {
        }

        // Null values are acceptable

        conbuilder.shardingKey(null);
        conbuilder.superShardingKey(null);

        Connection con = conbuilder.build();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps.setString(1, "Northern Hills Drive NE");
            ps.setString(2, "Rochester");
            ps.setString(3, "MN");
            ps.executeUpdate();
            ps.close();
        } finally {
            con.close();
        }
    }

    /**
     * When beginRequest and endRequest are invoked by the application (or an external connection manager)
     * they must be ignored.
     */
    @Test
    public void testSuppressBeginAndEndRequest() throws Exception {
        AtomicInteger[] requests;
        int begins, ends;

        Connection con = unsharablePool1DataSource.getConnection();
        try {
            requests = (AtomicInteger[]) con.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            con.beginRequest();
            // expect no change:
            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());

            PreparedStatement ps = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps.setString(1, "Northern Valley Drive NE");
            ps.setString(2, "Rochester");
            ps.setString(3, "MN");
            ps.executeUpdate();
            ps.close();

            con.endRequest();
            // still no change:
            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());
        } finally {
            con.close();
        }

        assertEquals(begins, requests[BEGIN].get());
        assertEquals(ends + 1, requests[END].get());
    }

    /**
     * Begin a request (using a sharable connection) within one global transaction. Suspend that transaction
     * and use the connection handle within a different global transaction, which must be considered a different
     * request. After committing the second global transaction, resume the first global transaction and perform
     * additional operations which should be under the first request. Commit the first transaction and verify
     * that the first request has ended.
     */
    @ExpectedFFDC("java.lang.IllegalStateException") // TODO remove this once transactions bug is fixed
    @Test
    public void testSuspendRunGlobalTranAndResumeSharable() throws Exception {
        AtomicInteger[] requests1;
        int begin1, end1;

        AtomicInteger[] requests2;
        int begin2, end2;

        // Obtain and use a connection handle within one global transaction
        tx.begin();
        try {
            Connection con = defaultDataSource.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            PreparedStatement ps1 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "Viola Road NE");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();

            requests1 = (AtomicInteger[]) con.unwrap(Supplier.class).get();
            begin1 = requests1[BEGIN].get();
            end1 = requests1[END].get();
            assertEquals(end1 + 1, begin1);

            Transaction suspended = txm.suspend();
            try {
                tx.begin();
                try {
                    assertEquals(end1, requests1[END].get());
                    assertEquals(Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());

                    requests2 = (AtomicInteger[]) con.unwrap(Supplier.class).get();
                    begin2 = requests2[BEGIN].get();
                    end2 = requests2[END].get();
                    assertEquals(end2 + 1, begin2);

                    PreparedStatement ps2 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                    ps2.setString(1, "Elton Hills Drive NW");
                    ps2.setString(2, "Rochester");
                    ps2.setString(3, "MN");
                    ps2.executeUpdate();
                    ps2.close();

                } finally {
                    tx.rollback();
                }
                // TODO bug: It appears the same managed connection remains with the sharable handle across suspend/begin/rollback
                // and therefore it is operating under the original request, rather than being a separate request that can end here!
                //TODO assertEquals(end2 + 1, requests2[END].get());
                System.out.println("At this point, the second request should have end count of " + (end2 + 1) + ". We observed: " + requests2[END].get());
            } finally {
                txm.resume(suspended);
            }

            assertEquals(end1, requests1[END].get());

            try {
                // Continue using the connection handle after the global transaction completes,
                PreparedStatement ps3 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps3.setString(1, "Northern Heights Drive NE");
                ps3.setString(2, "Rochester");
                ps3.setString(3, "MN");
                ps3.executeUpdate();
                ps3.close();

                assertEquals(Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());
            } finally {
                con.close();
            }
        } finally {
            tx.commit();
        }

        assertEquals(end1 + 1, requests1[END].get());

        // Check if the data is consistent. The first and third update must commit. The second must roll back.
        Connection c = defaultDataSource.getConnection();
        try {
            PreparedStatement ps = c.prepareStatement("SELECT NAME FROM STREETS WHERE NAME=? AND CITY=? AND STATE=?");
            ps.setString(3, "MN");
            ps.setString(2, "Rochester");
            ps.setString(1, "Viola Road NE");
            assertTrue(ps.executeQuery().next()); // must be committed

            ps.setString(1, "Elton Hills Drive NW");
            // TODO data integrity bug: either the suspend/resume or the inner begin/rollback are not being honored!
            // assertFalse(ps.executeQuery().next()); // must be rolled back

            ps.setString(1, "Northern Heights Drive NE");
            assertTrue(ps.executeQuery().next()); // must be committed

            ps.close();
        } finally {
            c.close();
        }
    }

    /**
     * Begin a request (using an unsharable connection) within one global transaction. Suspend that transaction
     * and use the connection within a different global transaction, which must be considered the same request
     * because it is the same connection, even though running under a different transaction.
     * After committing the second global transaction, resume the first global transaction and perform additional
     * operations, which should continue be under the first (and only) request. Commit the first transaction and
     * verify that the single request has ended.
     */
    @ExpectedFFDC("java.lang.IllegalStateException") // TODO remove this once transactions bug is fixed
    @Test
    public void testSuspendRunGlobalTranAndResumeUnsharable() throws Exception {
        AtomicInteger[] requests;
        int begins, ends;

        // Obtain and use a connection within one global transaction
        Connection con = null;
        tx.begin();
        try {
            con = unsharableXADataSource.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            PreparedStatement ps1 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "Bandel Road NW");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();

            requests = (AtomicInteger[]) con.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            Transaction suspended = txm.suspend();
            try {
                tx.begin();
                try {
                    assertEquals(ends, requests[END].get());
                    assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con.getTransactionIsolation());

                    PreparedStatement ps2 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                    ps2.setString(1, "Country Club Road SW");
                    ps2.setString(2, "Rochester");
                    ps2.setString(3, "MN");
                    ps2.executeUpdate();
                    ps2.close();

                } finally {
                    tx.rollback();
                }
                assertEquals(ends, requests[END].get());
            } finally {
                txm.resume(suspended);
            }

            assertEquals(ends, requests[END].get());

            // Continue using the connection after the global transaction completes,
            PreparedStatement ps3 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps3.setString(1, "Wilder Road NW");
            ps3.setString(2, "Rochester");
            ps3.setString(3, "MN");
            ps3.executeUpdate();
            ps3.close();

            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con.getTransactionIsolation());
        } finally {
            try {
                if (con != null)
                    con.close();
            } finally {
                tx.commit();
            }
        }

        assertEquals(ends + 1, requests[END].get());

        // Check if the data is consistent. The first and third update must commit. The second must roll back.
        Connection c = unsharableXADataSource.getConnection();
        try {
            PreparedStatement ps = c.prepareStatement("SELECT NAME FROM STREETS WHERE NAME=? AND CITY=? AND STATE=?");
            ps.setString(3, "MN");
            ps.setString(2, "Rochester");
            ps.setString(1, "Bandel Road NW");
            assertTrue(ps.executeQuery().next()); // must be committed

            ps.setString(1, "Country Club Road SW");
            // TODO data integrity bug: either the suspend/resume or the inner begin/rollback are not being honored!
            // assertFalse(ps.executeQuery().next()); // must be rolled back

            ps.setString(1, "Wilder Road NW");
            assertTrue(ps.executeQuery().next()); // must be committed

            ps.close();
        } finally {
            c.close();
        }
    }

    /**
     * Begin a request (using a sharable connection) within one global transaction. Suspend that transaction
     * and use the connection handle within an LTC, which must be considered a different request.
     * After performing commits and rollbacks within the LTC, resume the first global transaction and perform
     * additional operations which should be under the first request. Commit the first transaction and verify
     * that the first request has ended.
     */
    @Test
    public void testSuspendRunLTCAndResumeSharable() throws Exception {
        AtomicInteger[] requests1;
        int begin1, end1;

        AtomicInteger[] requests2;
        int begin2, end2;

        // Obtain and use a connection handle within one global transaction
        tx.begin();
        try {
            Connection con = defaultDataSource.createConnectionBuilder().build();
            con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

            PreparedStatement ps1 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "Simpson Road SE");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();

            requests1 = (AtomicInteger[]) con.unwrap(Supplier.class).get();
            begin1 = requests1[BEGIN].get();
            end1 = requests1[END].get();
            assertEquals(end1 + 1, begin1);

            Transaction suspended = txm.suspend();
            try {
                assertEquals(end1, requests1[END].get());
                assertEquals(Connection.TRANSACTION_REPEATABLE_READ, con.getTransactionIsolation());

                requests2 = (AtomicInteger[]) con.unwrap(Supplier.class).get();
                begin2 = requests2[BEGIN].get();
                end2 = requests2[END].get();
                assertEquals(end2 + 1, begin2);

                con.setAutoCommit(false);
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps2.setString(1, "Eastwood Road SE");
                ps2.setString(2, "Rochester");
                ps2.setString(3, "MN");
                ps2.executeUpdate();
                ps2.close();
                // TODO con.commit(); enable once suspend/resume is fixed

                PreparedStatement ps3 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps3.setString(1, "Park Lane SE");
                ps3.setString(2, "Rochester");
                ps3.setString(3, "MN");
                ps3.executeUpdate();
                ps3.close();
                // TODO con.rollback(); enable once suspend/resume is fixed

                // TODO con.setAutoCommit(true); enable once suspend/resume is fixed

                // TODO bug: It appears the same managed connection remains with the sharable handle across suspend/begin/rollback
                // and therefore it is operating under the original request, rather than being a separate request that can end here!
                // assertEquals(end2 + 1, requests2[END].get());
                System.out.println("At this point, the second request should have end count of " + (end2 + 1) + ". We observed: " + requests2[END].get());
            } finally {
                txm.resume(suspended);
            }

            assertEquals(end1, requests1[END].get());

            try {
                // Continue using the connection handle after the LTC ends,
                PreparedStatement ps4 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps4.setString(1, "St. Bridget's Road SE");
                ps4.setString(2, "Rochester");
                ps4.setString(3, "MN");
                ps4.executeUpdate();
                ps4.close();

                assertEquals(Connection.TRANSACTION_REPEATABLE_READ, con.getTransactionIsolation());
            } finally {
                con.close();
            }
        } finally {
            tx.commit();
        }

        assertEquals(end1 + 1, requests1[END].get());

        // Check if the data is consistent. The first and third update must commit. The second must roll back.
        Connection c = defaultDataSource.getConnection();
        try {
            PreparedStatement ps = c.prepareStatement("SELECT NAME FROM STREETS WHERE NAME=? AND CITY=? AND STATE=?");
            ps.setString(3, "MN");
            ps.setString(2, "Rochester");
            ps.setString(1, "Simpson Road SE");
            assertTrue(ps.executeQuery().next()); // must be committed (from global transaction)

            ps.setString(1, "Eastwood Road SE");
            assertTrue(ps.executeQuery().next()); // must be committed (from commit in LTC)

            ps.setString(1, "Park Lane SE");
            // TODO enable once suspend/resume are fixed
            //assertFalse(ps.executeQuery().next()); // must be rolled back (from rollback in LTC)

            ps.setString(1, "St. Bridget's Road SE");
            assertTrue(ps.executeQuery().next()); // must be committed (from global transaction)

            ps.close();
        } finally {
            c.close();
        }
    }

    /**
     * Begin a request (using an unsharable connection) within one global transaction. Suspend that transaction
     * and use the connection within an LTC, which must be considered the same request because it is the same
     * connection, even though running separate transactions. After committing and rolling back transactions
     * within the LTC, resume the first global transaction and perform additional operations, which should
     * continue be under the first (and only) request. Commit the first transaction and verify that the single
     * request has ended.
     */
    @Test
    public void testSuspendRunLTCAndResumeUnsharable() throws Exception {
        AtomicInteger[] requests;
        int begins, ends;

        // Obtain and use a connection within one global transaction
        Connection con = null;
        tx.begin();
        try {
            con = unsharableXADataSource.createConnectionBuilder().build();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            PreparedStatement ps1 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "Valkyrie Drive NW");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();

            requests = (AtomicInteger[]) con.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            Transaction suspended = txm.suspend();
            try {
                assertEquals(ends, requests[END].get());
                assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con.getTransactionIsolation());

                con.setAutoCommit(false);
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps2.setString(1, "Zumbro Drive NW");
                ps2.setString(2, "Rochester");
                ps2.setString(3, "MN");
                ps2.executeUpdate();
                ps2.close();
                // TODO con.rollback(); enable once suspend/resume is fixed

                PreparedStatement ps3 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps3.setString(1, "Viking Drive NW");
                ps3.setString(2, "Rochester");
                ps3.setString(3, "MN");
                ps3.executeUpdate();
                ps3.close();
                // TODO con.commit(); enable once suspend/resume is fixed

                // TODO con.setAutoCommit(true); enable once suspend/resume is fixed

                assertEquals(ends, requests[END].get());
            } finally {
                txm.resume(suspended);
            }

            assertEquals(ends, requests[END].get());

            // Continue using the connection after the global transaction completes,
            PreparedStatement ps4 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps4.setString(1, "Terracewood Drive NW");
            ps4.setString(2, "Rochester");
            ps4.setString(3, "MN");
            ps4.executeUpdate();
            ps4.close();

            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con.getTransactionIsolation());
        } finally {
            try {
                if (con != null)
                    con.close();
            } finally {
                tx.commit();
            }
        }

        assertEquals(ends + 1, requests[END].get());

        // Check if the data is consistent. The first and third update must commit. The second must roll back.
        Connection c = unsharableXADataSource.getConnection();
        try {
            PreparedStatement ps = c.prepareStatement("SELECT NAME FROM STREETS WHERE NAME=? AND CITY=? AND STATE=?");
            ps.setString(3, "MN");
            ps.setString(2, "Rochester");
            ps.setString(1, "Valkyrie Drive NW");
            assertTrue(ps.executeQuery().next()); // must be committed (from global transaction)

            ps.setString(1, "Zumbro Drive NW");
            // TODO enable once suspend/resume are fixed
            //assertFalse(ps.executeQuery().next()); // must be rolled back (from rollback during LTC)

            ps.setString(1, "Viking Drive NW");
            assertTrue(ps.executeQuery().next()); // must be committed (from commit during LTC)

            ps.setString(1, "Terracewood Drive NW");
            assertTrue(ps.executeQuery().next()); // must be committed

            ps.close();
        } finally {
            c.close();
        }
    }

    @Test
    public void testVendorSpecificDefaultShardingKeys() throws Exception {
        ShardingKey key1 = dataSourceWithDefaultSharding.createShardingKeyBuilder().subkey("VSDSK", JDBCType.LONGVARBINARY).build();
        ConnectionBuilder conbuilder1 = dataSourceWithDefaultSharding.createConnectionBuilder();
        ConnectionBuilder conbuilder2 = dataSourceWithDefaultSharding.createConnectionBuilder().shardingKey(null);
        ConnectionBuilder conbuilder3 = dataSourceWithDefaultSharding.createConnectionBuilder().shardingKey(key1).superShardingKey(null);
        String k;

        tx.begin();
        try {
            Connection con1 = conbuilder1.build();
            // expect default sharding keys from the data source config in server.xml
            k = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|CHAR:DefaultShardingKey;"));
            k = con1.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|VARCHAR:DefaultSuperKey;"));

            // connection request (with null/unspecified keys) that shares the same connection by matching current state
            Connection con2 = conbuilder2.build();
            k = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|CHAR:DefaultShardingKey;"));
            k = con2.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|VARCHAR:DefaultSuperKey;"));
            con2.close();

            con1.setShardingKey(key1);

            // connection request (with key1/null keys) that shares the same connection by matching current state
            Connection con3 = conbuilder3.build();
            k = con3.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|LONGVARBINARY:VSDSK;"));
            k = con3.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|VARCHAR:DefaultSuperKey;"));
            con3.close();

            con1.setShardingKey(key1, key1);
            con1.close();
        } finally {
            tx.rollback();
        }

        // Are the values reset to null/defaults when reusing a connection from the pool?
        Connection con4 = conbuilder2.build();
        try {
            k = con4.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k, k.endsWith("|CHAR:DefaultShardingKey;"));
            k = con4.getClientInfo("SUPER_SHARDING_KEY"); // extension by mock JDBC driver to determine the super sharding key
            assertTrue(k, k.endsWith("|VARCHAR:DefaultSuperKey;"));
        } finally {
            con4.close();
        }
    }

    /**
     * Verify that connection builder can be used on a Liberty data source that is backed by a
     * javax.sql.XADataSource implementation. This test only does matching on ShardingKey.
     * It does not cover user, password, or super sharding key.
     */
    @Test
    public void testXAConnectionBuilderMatchShardingKey() throws Exception {
        ShardingKeyBuilder keybuilderA = sharableXADataSource.createShardingKeyBuilder();
        ShardingKeyBuilder keybuilderB = sharableXADataSource.createShardingKeyBuilder();
        ShardingKey keyA = keybuilderA.subkey("XACBKey", JDBCType.CHAR).subkey(true, JDBCType.BOOLEAN).build();
        ShardingKey keyB = keybuilderB.subkey("XACBKey", JDBCType.CHAR).subkey(true, JDBCType.BOOLEAN).build();
        ConnectionBuilder conbuilderA = sharableXADataSource.createConnectionBuilder().shardingKey(keyA);
        ConnectionBuilder conbuilderB = sharableXADataSource.createConnectionBuilder().shardingKey(keyB);

        AtomicInteger[] requests2;
        int begins2;

        tx.begin();
        try {
            Connection con1 = conbuilderA.build();
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con1.getTransactionIsolation());
            con1.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            String k1 = con1.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k1, k1.endsWith("|CHAR:XACBKey;BOOLEAN:true;"));

            Connection con2 = conbuilderB.build();
            // If connection handle is shared, it will report the isolation level value of con1,
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, con2.getTransactionIsolation());
            String k2 = con2.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k2, k2.endsWith("|CHAR:XACBKey;BOOLEAN:true;"));

            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES (?, ?, ?)");
            ps2.setString(1, "Plummer Circle SW");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
            ps2.close();

            requests2 = (AtomicInteger[]) con2.unwrap(Supplier.class).get();
            begins2 = requests2[BEGIN].get();

            // enlist another resource to prove that the prior connection is two-phase capable
            Connection con3 = sharablePool1DataSourceWithAppAuth.getConnection("user43", "pwd43");
            PreparedStatement ps3 = con3.prepareStatement("INSERT INTO STREETS VALUES (?, ?, ?)");
            ps3.setString(1, "Rocky Creek Drive NE");
            ps3.setString(2, "Rochester");
            ps3.setString(3, "MN");
            ps3.executeUpdate();
            ps3.close();

            con3.close();
            con2.close();
            con1.close();
        } finally {
            tx.commit();
        }

        // Specify a similar, but different (subkeys are reversed) sharding key on the builder,
        ShardingKeyBuilder keybuilderD = sharableXADataSource.createShardingKeyBuilder();
        ShardingKey key4 = keybuilderD.subkey(true, JDBCType.BOOLEAN).subkey("XACBKey", JDBCType.CHAR).build();
        Connection con4 = conbuilderA.shardingKey(key4).build();
        try {
            String k4 = con4.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k4, k4.endsWith("|BOOLEAN:true;CHAR:XACBKey;"));
        } finally {
            con4.close();
        }

        // Specify a match for the first sharding key
        ShardingKey key5 = keybuilderB.build();
        Connection con5 = conbuilderB.shardingKey(key5).build();
        try {
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con5.getTransactionIsolation());
            String k5 = con5.getClientInfo("SHARDING_KEY"); // extension by mock JDBC driver to determine the sharding key
            assertTrue(k5, k5.endsWith("|CHAR:XACBKey;BOOLEAN:true;"));

            // verify this is the same instance, matched and reused from the pool
            AtomicInteger[] requests5 = (AtomicInteger[]) con5.unwrap(Supplier.class).get();
            // yes, we do mean to compare instances here - it demonstrates the same underlying connection was reused from the pool
            assertSame(requests2[BEGIN], requests5[BEGIN]);
            int begins5 = requests5[BEGIN].get();
            assertEquals(begins2 + 1, begins5);
        } finally {
            con5.close();
        }
    }

    /**
     * Verify that user and password supplied via connection builder are used when sharing XA connections,
     * as well as when requesting new XA connections.
     */
    @Test
    public void testXAConnectionBuilderMatchUserPassword() throws Exception {
        tx.begin();
        try {
            Connection con1 = sharableXADataSourceWithAppAuth.getConnection("user43", "pwd43");
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con1.getTransactionIsolation());
            con1.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            ConnectionBuilder builderA = sharableXADataSourceWithAppAuth.createConnectionBuilder();
            Connection con2 = builderA.user("user43").password("pwd43").build();
            // If connection handle is shared, it will report the isolation level value of con1,
            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con2.getTransactionIsolation());
            assertEquals("user43", con2.getMetaData().getUserName());

            // Show that the user name is honored by accessing data that was previously written by this user
            PreparedStatement ps2 = con2.prepareStatement("SELECT NAME FROM STREETS WHERE NAME=? AND CITY=? AND STATE=?");
            ps2.setString(1, "Civic Center Drive NW");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ResultSet result2 = ps2.executeQuery();
            assertTrue(result2.next());
            ps2.close();

            // Request another matching connection via the same builder
            Connection con3 = builderA.build();
            // If connection handle is shared, it will report the isolation level value of con1,
            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con3.getTransactionIsolation());
            assertEquals("user43", con3.getMetaData().getUserName());

            // Request a new non-matching connection via the builder
            Connection con4 = builderA.user("user4").build();
            // If connection handle is not shared with con1/con2/con3, it will report the default isolation level,
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con4.getTransactionIsolation());
            con4.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            assertEquals("user4", con4.getMetaData().getUserName());

            // Request a matching connection via a different builder
            ConnectionBuilder builderB = sharableXADataSourceWithAppAuth.createConnectionBuilder();
            Connection con5 = builderB.user("user43").password("pwd43").build();
            // If connection handle is shared, it will report the isolation level value of con1,
            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con5.getTransactionIsolation());
            assertEquals("user43", con5.getMetaData().getUserName());

            con5.close();
            con4.close();
            con3.close();
            con2.close();
            con1.close();
        } finally {
            tx.commit();
        }
    }
}
