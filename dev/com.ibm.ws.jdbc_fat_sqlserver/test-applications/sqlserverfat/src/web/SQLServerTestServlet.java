/*******************************************************************************
 * Copyright (c) 2019,2023 IBM Corporation and others.
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
package web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.microsoft.sqlserver.jdbc.ISQLServerConnection;
import com.microsoft.sqlserver.jdbc.ISQLServerDataSource;
import com.microsoft.sqlserver.jdbc.ISQLServerStatement;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipIfSysProp;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SQLServerTestServlet")
public class SQLServerTestServlet extends FATServlet {
    @Resource
    private DataSource ds;

    @Resource(lookup = "jdbc/ss", authenticationType = Resource.AuthenticationType.APPLICATION)
    private DataSource ds_ss;

    @Resource(lookup = "jdbc/ss-using-driver-type")
    private DataSource ss_using_driver_type;

    @Resource(name = "java:comp/jdbc/env/unsharable-ds-xa-loosely-coupled", shareable = false)
    private DataSource unsharable_ds_xa_loosely_coupled;

    @Resource(name = "java:comp/jdbc/env/unsharable-ds-xa-tightly-coupled", shareable = false)
    private DataSource unsharable_ds_xa_tightly_coupled;

    @Resource(lookup = "jdbc/ntlm")
    private DataSource ds_ntlm;

    @Resource(lookup = "jdbc/driver-property-preferred")
    private DataSource driver_property_perferred;

    @Resource(lookup = "jdbc/ds-property-preferred")
    private DataSource ds_property_perferred;

    @Resource(lookup = "jdbc/driver-no-override")
    private DataSource driver_no_override;

    @Resource(lookup = "jdbc/ds-no-override")
    private DataSource ds_no_override;

    @Resource(lookup = "jdbc/ds-no-url-defaults")
    private DataSource ds_no_url_defaults;

    @Resource
    private ExecutorService executor;

    @Resource
    private UserTransaction tran;

    // Maximum amount of time the test will wait for an operation to complete
    private static final long TIMEOUT = TimeUnit.MINUTES.toNanos(2);

    // Verify that the responseBuffering attribute of cached statements is reset to the default from the data source
    @Test
    public void testResponseBuffering() throws Exception {
        String originalResponseBuffering;
        originalResponseBuffering = ds_ss.unwrap(ISQLServerDataSource.class).getResponseBuffering();
        Connection con = ds_ss.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");
            ISQLServerStatement sstmt = pstmt.unwrap(ISQLServerStatement.class);
            assertEquals(originalResponseBuffering, sstmt.getResponseBuffering());
            // change the value
            sstmt.setResponseBuffering("adaptive".equals(originalResponseBuffering) ? "full" : "adaptive");
            pstmt.close();

            // Reuse the cached statement and verify the response buffering was reset.
            pstmt = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");
            sstmt = pstmt.unwrap(ISQLServerStatement.class);
            assertEquals(originalResponseBuffering, sstmt.getResponseBuffering());
        } finally {
            con.close();
        }
    }

    // Test the transaction snapshot isolation level
    @Test
    public void testTransactionSnapshot() throws Exception {
        List<Future<Integer>> futures = new LinkedList<Future<Integer>>();

        Connection con = ds_ss.getConnection();
        try {
            assertEquals(ISQLServerConnection.TRANSACTION_SNAPSHOT, con.getTransactionIsolation());

            PreparedStatement ps = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");
            ps.setInt(1, 1);
            ps.setNString(2, "one");
            assertEquals(1, ps.executeUpdate());
            ps.close();

            con.setAutoCommit(false);

            ps = con.prepareStatement("SELECT STRVAL FROM MYTABLE WHERE ID=?");
            ps.setInt(1, 1);
            ResultSet result = ps.executeQuery();
            assertTrue(result.next());
            assertEquals("one", result.getNString(1));
            result.close();

            final Phaser phaser = new Phaser(1);
            final int Phase0_ExecThreadUpdateExecuted = 0;
            final int Phase1_MainThreadQueriedAfterUpdate = 1;
            final int Phase2_ExecThreadUpdateCommitted = 2;

            futures.add(executor.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws InterruptedException, SQLException, TimeoutException {
                    Connection con = ds_ss.getConnection();
                    try {
                        con.setAutoCommit(false);
                        PreparedStatement ps = con.prepareStatement("UPDATE MYTABLE SET STRVAL=? WHERE ID=?");
                        ps.setNString(1, "uno");
                        ps.setInt(2, 1);
                        int updateCount = ps.executeUpdate();

                        System.out.println("Executor thread update made, waiting before commit...");
                        phaser.arrive();
                        phaser.awaitAdvanceInterruptibly(Phase1_MainThreadQueriedAfterUpdate, TIMEOUT, TimeUnit.NANOSECONDS);

                        System.out.println("Executor thread committing and ending");
                        con.commit();
                        phaser.arrive();

                        return updateCount;
                    } finally {
                        con.close();
                    }
                }
            }));

            System.out.println("Main thread initial read completed, waiting for executor thread to update...");
            phaser.awaitAdvanceInterruptibly(Phase0_ExecThreadUpdateExecuted, TIMEOUT, TimeUnit.NANOSECONDS);

            // With snapshot isolation, should be able to read the old value even though
            // the executor thread has updated it in another transaction.
            result = ps.executeQuery();
            assertTrue(result.next());
            assertEquals("one", result.getNString(1));
            result.close();

            System.out.println("Main thread second read completed, waiting for executor thread to commit its changes...");
            phaser.arrive();
            phaser.awaitAdvanceInterruptibly(Phase2_ExecThreadUpdateCommitted, TIMEOUT, TimeUnit.NANOSECONDS);

            // With snapshot isolation, should be able to read the old value even though
            // the executor thread has updated it and committed in another transaction.
            result = ps.executeQuery();
            assertTrue(result.next());
            assertEquals("one", result.getNString(1));
            result.close();

            con.commit();

            System.out.println("Main thread third read completed, waiting for executor thread to complete...");

            // Allow the executor thread to complete
            int executorThreadUpdateCount = futures.remove(0).get(TIMEOUT, TimeUnit.NANOSECONDS);
            assertEquals(1, executorThreadUpdateCount);

            // Validate the executor thread committed successfully
            result = ps.executeQuery();
            assertTrue(result.next());
            assertEquals("uno", result.getNString(1));
            result.close();

            con.commit();

            System.out.println("Done");
        } finally {
            con.close();

            for (Future<Integer> future : futures)
                future.cancel(true);
        }
    }

    // Test XA transaction timeout
    // Expected XAER_NOTA (-4) when transaction manager tries to roll back XAResource that already rolled back in the database upon transaction timeout
    @Test
    @Mode(TestMode.FULL)
    @AllowedFFDC("javax.transaction.xa.XAException")
    public void testTransactionTimeout() throws Exception {
        boolean committed = false;
        tran.setTransactionTimeout(8);
        tran.begin();
        long start = System.nanoTime();
        try {
            try (Connection con = ds.getConnection()) {
                con.createStatement().executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID=0").close(); //perform db operation
                con.createStatement().execute("WAITFOR DELAY '00:00:16'"); // Wait for 16 seconds

                /*
                 * FIXME currently executing the WAITFOR statement above DOES causes a transaction timeout,
                 * and the connection to abort on the liberty side. The abort does not seem to go down to the database.
                 * Therefore, on exit no SQLException is returned from the database.
                 *
                 * This statement does throw an exception when run against a bare metal server using the same JDBC driver.
                 * Possible issues:
                 * 1. docker image defect
                 * 2. connection issue
                 */
//                long duration = System.nanoTime() - start;
//                fail("Statement should have been canceled. Instead it completed after " + TimeUnit.NANOSECONDS.toMillis(duration) + " ms");
            } catch (SQLException x) {
                if (x.getErrorCode() != 1206) // distributed transaction cancelled (due to timeout)
                    throw x;
            }
        } finally {
            try {
                /*
                 * Due to timing issues we may found ourselves in a situation where the database has reported a transaction timeout,
                 * and ends the WAITFOR statement early. We continue on to close our connections. At this point we are in a TRANSACTION_ENDING state.
                 * We attempt to call tran.commit(), but at this point the transaction hasn't yet registered the timeout.
                 *
                 * During the commit, the com.ibm.tx.jta.embeddable.impl.EmbeddableTimeoutManager reports the transaction has timed out (~10 seconds after
                 * the timeout has actually occurred), and our state is changed to NO_TRANSACTION_ACTIVE (though I do not see this in the trace).
                 *
                 * The commit continues to process, and tries to update the state to NO_TRANSACTION_ACTIVE, but NO_TRANSACTION_ACTIVE > NO_TRANSACTION_ACTIVE
                 * is invalid and a SystemException is thrown.
                 */

                int delaySeconds = Boolean.getBoolean("fat.test.localrun") ? 1 : 5;
                for (int retry = 0; retry < 3 && tran.getStatus() != Status.STATUS_MARKED_ROLLBACK; retry++) {
                    System.out.println("Preparing to commit.  Retry=" + retry);
                    Thread.sleep(TimeUnit.SECONDS.toMillis(delaySeconds));
                }

                //Debug info in-case our delay timing needs to be updated in the future.
                long duration = System.nanoTime() - start;
                System.out.println("Transaction open for " + TimeUnit.NANOSECONDS.toSeconds(duration) + " seconds");

                tran.commit();
                committed = true;
            } catch (RollbackException x) {
                System.out.println("tran.commit() threw a RollbackException as expected.");
                x.printStackTrace(System.out);
            } finally {
                //Restore default transaction timeout
                tran.setTransactionTimeout(0);
            }
        }

        assertFalse(committed);
    }

    // Test multiple SQL Server resources in a single XA transaction
    @Test
    public void testTwoPhaseCommit() throws Exception {
        Connection con;

        tran.begin();
        try {
            con = ds.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");
            ps.setInt(1, 2);
            ps.setNString(2, "dos");
            assertEquals(1, ps.executeUpdate());
            con.close();

            con = ds_ss.getConnection();
            ps = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");
            ps.setInt(1, 3);
            ps.setNString(2, "tres");
            assertEquals(1, ps.executeUpdate());
        } finally {
            /*
             * On JDBC 4.3 this commit will fail due to the ds_ss datasource having started as XA enabled and switching to snapshot isolation.
             * For JDBC 4.3 the SQL Server driver will throw the following error:
             *
             * com.microsoft.sqlserver.jdbc.SQLServerException: Transaction failed in database 'LIBR0073' because the statement was
             * run under snapshot isolation but the transaction did not start in snapshot isolation. You cannot change the isolation
             * level of the transaction to snapshot after the transaction has started unless the transaction was originally started
             * under snapshot isolation level.
             *
             * This is a change in behavior due to the change in defaulting to XA enabled datasources.
             */
            tran.commit();
        }

        try {
            PreparedStatement ps = con.prepareStatement("SELECT STRVAL FROM MYTABLE WHERE ID>? AND ID<? ORDER BY ID ASC");
            ps.setInt(1, 1);
            ps.setInt(2, 4);
            ResultSet result = ps.executeQuery();

            assertTrue(result.next());
            assertEquals("dos", result.getNString(1));

            assertTrue(result.next());
            assertEquals("tres", result.getNString(1));
        } finally {
            con.close();
        }
    }

    //Test that a datasource backed by Driver can be used with both the generic properties element and properties.microsoft.sqlserver
    //element when type="java.sql.Driver"
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testDSUsingDriver() throws Exception {
        Connection conn = ss_using_driver_type.getConnection();
        assertFalse("ss_using_driver_type should not wrap ISQLServerDataSource", ss_using_driver_type.isWrapperFor(ISQLServerDataSource.class));

        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");
            ps.setInt(1, 20);
            ps.setString(2, "twenty");
            ps.executeUpdate();
            ps.close();
        } finally {
            conn.close();
        }

        DataSource ss_using_driver = InitialContext.doLookup("jdbc/ss-using-driver");
        assertFalse("ss_using_driver should not wrap ISQLServerDataSource", ss_using_driver.isWrapperFor(ISQLServerDataSource.class));
        Connection conn2 = ss_using_driver.getConnection();
        try {
            Statement st = conn2.createStatement();
            ResultSet rs = st.executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID = 20");
            assertTrue("Query should have returned a result", rs.next());
            assertEquals("Unexpected value returned", "twenty", rs.getString(1));
            rs.close();
        } finally {
            conn2.close();
        }
    }

    //Test that the proper implementation classes are used for the various datasources configured in this test bucket
    //since the JDBC Driver used is named so as not to be recognized by the built-in logic
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
    public void testInferSQLServerDataSource() throws Exception {
        //The default datasource should continue to be inferred as an XADataSource, since it has properties.microsoft.sqlserver configured
        assertTrue("default datasource should wrap XADataSource", ds.isWrapperFor(XADataSource.class));

        //ds_ss should continue to be inferred as a ConnectionPoolDataSource, since it has properties.microsoft.sqlserver configured
        assertTrue("ds_ss should wrap ConnectionPoolDataSource", ds_ss.isWrapperFor(ConnectionPoolDataSource.class));

        //ss_using_driver doesn't specify a type.  The presence of URL will result in the DataSource being back by Driver
        DataSource ss_using_driver = InitialContext.doLookup("jdbc/ss-using-driver");
        assertFalse("The presence of the URL should result in ss_using_driver being back by Driver", ss_using_driver.isWrapperFor(ISQLServerDataSource.class));

        //inferred ds does not specify a URL or type. This should result in inferring a datasource class name
        DataSource ss_inferred_ds = InitialContext.doLookup("jdbc/ss-inferred");
        assertTrue("ss_inferred_ds should wrap datasource since it does not have a URL property",
                   ss_inferred_ds.isWrapperFor(ISQLServerDataSource.class));

        //try to use the inferred_ds to ensure it is usable
        Connection conn = ss_inferred_ds.getConnection();
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");
            ps.setInt(1, 30);
            ps.setString(2, "thirty");
            ps.executeUpdate();
            ps.close();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID = 30");
            assertTrue("Query should have returned a result", rs.next());
            assertEquals("Unexpected value returned", "thirty", rs.getString(1));
            rs.close();
        } finally {
            conn.close();
        }
    }

    /**
     * Confirm that locks are not shared between transaction branches that are loosely coupled.
     */
    @Test
    public void testTransactionBranchesLooselyCoupled() throws Exception {
        tran.begin();
        try {
            try (Connection con1 = unsharable_ds_xa_loosely_coupled.getConnection()) {
                con1.setTransactionIsolation(ISQLServerConnection.TRANSACTION_SNAPSHOT);
                con1.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (31, 'thirty-one')");

                // Obtain a second (unshared) connection so that we have 2 transaction branches
                try (Connection con2 = unsharable_ds_xa_loosely_coupled.getConnection()) {
                    con2.setTransactionIsolation(ISQLServerConnection.TRANSACTION_SNAPSHOT);
                    ResultSet result = con2.createStatement().executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID=31");
                    assertFalse(result.next());
                }
            }
        } finally {
            tran.commit();
        }
    }

    /**
     * Confirm that locks are shared between transaction branches that are tightly coupled.
     */
    @AllowedFFDC("javax.transaction.xa.XAException") // TODO remove this once Microsoft bug is fixed
    @Test
    public void testTransactionBranchesTightlyCoupled() throws Exception {
        tran.begin();
        try {
            try (Connection con1 = unsharable_ds_xa_tightly_coupled.getConnection()) {
                con1.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (32, 'thirty-two')");

                // Obtain a second (unshared) connection so that we have 2 transaction branches
                try (Connection con2 = unsharable_ds_xa_tightly_coupled.getConnection()) {
                    assertEquals(1, con2.createStatement().executeUpdate("UPDATE MYTABLE SET STRVAL='XXXII' WHERE ID=32"));
                }
            }
        } finally {
            // TODO switch to commit once Microsoft bug is fixed
            tran.rollback();
        }
    }

    //Verify that the NTLM authentication scheme can be configured on the DataSource.
    //This is not supported by the Database because we cannot run Active Directory in a container.
    //Just ensure that the setting was passed to the driver for now.
    //Without integratedSecurity=true this setting will be ignored and normal UN/PW will be used for authentication
    public void testAuthenticationSchemeNTLM() throws Exception {
        //Try to use NTLM datasource to create a connection, and insert data into database
        tran.begin();
        try (Connection con = ds_ntlm.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");) {
                ps.setInt(1, 40);
                ps.setNString(2, "fourty");
                assertEquals(1, ps.executeUpdate());
            }
            tran.commit();
        } catch (SQLException e) {
            tran.rollback();
            fail("Update should have been committed, but wasn't because of an Exception: " + e.getMessage());
        }

        //Query database using a different datasoruce to ensure the data was committed.
        try (Connection con = ds.getConnection(); Statement stmt = con.createStatement();) {
            ResultSet result = stmt.executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID=40");
            assertTrue("Query should have returned a result", result.next());
            assertEquals("Unexpected value returned", "fourty", result.getString(1));
        }
    }

    @Test
    public void testVerifyConnectionPrecedence() throws Throwable {
        try (Connection con = driver_property_perferred.getConnection(); PreparedStatement stmt = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");) {
            stmt.setInt(1, 41);
            stmt.setString(2, "fourty-one");
            stmt.execute();
        }

        try (Connection con = ds_property_perferred.getConnection(); PreparedStatement stmt = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");) {
            stmt.setInt(1, 42);
            stmt.setString(2, "fourty-two");
            stmt.execute();
        }
    }

    @Test
    public void testVerifyDefaultDoesNotOverride() throws Throwable {
        try (Connection con = driver_no_override.getConnection(); PreparedStatement stmt = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");) {
            stmt.setInt(1, 43);
            stmt.setString(2, "fourty-three");
            stmt.execute();
        }

        try (Connection con = ds_no_override.getConnection(); PreparedStatement stmt = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");) {
            stmt.setInt(1, 44);
            stmt.setString(2, "fourty-four");
            stmt.execute();
        }
    }

    /**
     * If a URL is not set, and no serverName is configured on the DataSource, then the JDBC Driver does default to localhost when attempting a connection.
     *
     * This is where the serverName is queried and defaulted:
     * https://github.com/microsoft/mssql-jdbc/blob/3343f73f0a18ce322d894b5646f3d89a3fbe375c/src/main/java/com/microsoft/sqlserver/jdbc/SQLServerConnection.java#L2090-L2097
     *
     * Here is where you would expect to find the default value (but isn't)
     * https://github.com/microsoft/mssql-jdbc/blob/3343f73f0a18ce322d894b5646f3d89a3fbe375c/src/main/java/com/microsoft/sqlserver/jdbc/SQLServerDriver.java#L577
     *
     * This test will verify this behavior does not change in future releases.
     *
     * @throws Throwable
     */
    @Test
    @ExpectedFFDC({ "com.microsoft.sqlserver.jdbc.SQLServerException",
                    "javax.resource.spi.ResourceAllocationException",
                    "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
    public void testVerifyDefaultWithoutURL() throws Throwable {
        try (Connection con = ds_no_url_defaults.getConnection(); PreparedStatement stmt = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");) {
            stmt.setInt(1, 45);
            stmt.setString(2, "fourty-five");
            stmt.execute();
            fail("Should not have been able to create a connection using default serverName.");
        } catch (SQLException e) {
            //Expect the default to be localhost and for the connection to fail.
            assertTrue(e.getMessage().contains("TCP/IP connection to the host localhost"));
        }
    }
}
