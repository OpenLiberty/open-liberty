/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
package jdbc.fat.derby.web;

import static componenttest.app.FATDatabaseServlet.createTable;
import static componenttest.app.FATDatabaseServlet.dropTable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@DataSourceDefinitions(value = {
                                 @DataSourceDefinition(
                                                       name = "java:module/env/jdbc/dsfat5",
                                                       className = "jdbc.tran.none.driver.TranNoneDataSource",
                                                       databaseName = "memory:ds5",
                                                       isolationLevel = Connection.TRANSACTION_NONE,
                                                       transactional = false,
                                                       properties = {
                                                                      "createDatabase=create"
                                                       }),
                                 @DataSourceDefinition(
                                                       name = "java:module/env/jdbc/dsfat6",
                                                       className = "org.apache.derby.jdbc.EmbeddedDataSource40",
                                                       databaseName = "memory:ds6",
                                                       properties = {
                                                                      "createDatabase=create"
                                                       }),
                                 @DataSourceDefinition(
                                                       name = "java:module/env/jdbc/dsfat7",
                                                       className = "org.apache.derby.jdbc.EmbeddedDataSource40",
                                                       databaseName = "memory:ds7",
                                                       isolationLevel = Connection.TRANSACTION_SERIALIZABLE,
                                                       properties = {
                                                                      "createDatabase=create"
                                                       })
})
@SuppressWarnings("serial")
@WebServlet("/JDBCDerbyServlet")
public class JDBCDerbyServlet extends FATServlet {
    private static final String CITYTABLE = "cities";
    private static final String CITYSCHEMA = "name varchar(50) not null primary key, population int, county varchar(30)";

    @Resource(name = "jdbc/dsfat0ref", lookup = "jdbc/dsfat0")
    DataSource ds0ref; //DSConfig (TRAN_SERIALIZABLE) + Res-ref (TRAN_NONE) + Normal JDBC Driver

    @Resource(lookup = "jdbc/dsfat1")
    DataSource ds1; //DSConfig (TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver

    @Resource(name = "jdbc/dsfat1ref", lookup = "jdbc/dsfat1")
    DataSource ds1ref; //DSConfig (TRAN_NONE) + Res-ref (TRAN_NONE) + TRAN_NONE JDBC Driver

    @Resource(name = "jdbc/dsfat2ref", lookup = "jdbc/dsfat2")
    DataSource ds2ref; //DSConfig (no iso lvl) + Res-ref (TRAN_NONE) + Normal JDBC Driver

    @Resource(name = "jdbc/dsfat5", lookup = "java:module/env/jdbc/dsfat5")
    DataSource dsd5; //DataSourceDef (TRAN_NONE) + No Res-ref

    @Resource(name = "jdbc/dsfat5ref", lookup = "java:module/env/jdbc/dsfat5")
    DataSource dsd5ref; //DataSourceDef (TRAN_NONE) + Res-ref (TRAN_NONE)

    @Resource(name = "jdbc/dsfat6ref", lookup = "java:module/env/jdbc/dsfat6")
    DataSource dsd6ref; //DataSourceDef(no iso lvl) + Res-ref (TRAN_NONE)

    @Resource(name = "jdbc/dsfat7ref", lookup = "java:module/env/jdbc/dsfat7")
    DataSource dsd7ref; //DataSourceDef(TRAN_SERIALIZABLE) + Res-ref (TRAN_NONE)

    @Resource(lookup = "jdbc/driver-url-preferred")
    DataSource driver_url_preferred;

    @Resource
    private UserTransaction tran;

    /**
     * Data Source - ds4 = dsConfig (TRAN_NONE) + No Res-ref + Normal JDBC Driver
     *
     * Ensure that when a data source is configured with TRANSACTION_NONE that
     * a JDBC driver that does not support this configuration throws an error.
     */
    @Test
    @ExpectedFFDC({ "javax.resource.spi.ResourceAllocationException", "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
    public void testTNUnsupported() throws Throwable {
        InitialContext ctx = new InitialContext();
        try {
            DataSource ds4 = (DataSource) ctx.lookup("jdbc/dsfat4");
            @SuppressWarnings("unused")
            Connection con = ds4.getConnection();
            fail("Connection should have thrown an exception since the JDBC driver does not support an isolation level of TRANSACTION_NONE.");
        } catch (SQLException sql) {
            assertTrue("Exception message should have contained", sql.getMessage().contains("DSRA4008E"));
        }
    }

    /**
     * Data Source - ds0ref = DSConfig (TRAN_SERIALIZABLE) + Res-ref (TRAN_NONE) + Normal JDBC Driver
     *
     * Ensure that if we try to change Transaction Isolation to TRANSACTION_NONE that we throw an exception
     * and prevent isolation level from being changed.
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException" })
    public void testTNRejectIsolationChange() throws Throwable {
        Connection con = null;
        int expected = Connection.TRANSACTION_SERIALIZABLE;

        try {
            con = ds0ref.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_NONE);
            fail("Exception should have been thrown when switching isolation level to TRANSACTION_NONE.");
        } catch (SQLException sql) {
            assertTrue("Exception message should have contained", sql.getMessage().contains("DSRA4011E"));

            //ensure desired behavior
            assertEquals("Transaction isolation level should not have been changed: ", expected, con.getTransactionIsolation());
        } finally {
            if (con != null) {
                con.close();
            }
        }
    }

    /**
     * Data Source - ds3 = DSConfig(TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     *
     * Ensure that a data source configured with an isolation level of TRANSACTION_NONE
     * and transactional = true fails during creation.
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException" })
    public void testTNTransactionalSetting() throws Throwable {
        InitialContext ctx = new InitialContext();
        try {
            @SuppressWarnings("unused")
            DataSource ds3 = (DataSource) ctx.lookup("jdbc/dsfat3");
            fail("Lookup should have failed due to bad config.");
        } catch (Exception e) {
            assertTrue("Exception message should have contained", e.getMessage().contains("CWWKN0008E"));
        }
    }

    /**
     * Called by testTNConfigTnsl in JDBCDerbyTest but also useful as a stand-alone test
     * Data Sources - ds8 = DSConfig(TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     *
     * Ensure connection is not enlisted in transaction by beginning a global transaction and doing work.
     * Then roll-back the transaction and ensure work was not rolled-back.
     */
    @Test
    public void testTNTransationEnlistment() throws Throwable {
        Connection con = null;
        ResultSet result = null;
        InitialContext ctx = new InitialContext();

        DataSource ds8 = (DataSource) ctx.lookup("jdbc/dsfat8");
        createTable(ds8, CITYTABLE, CITYSCHEMA);

        try {
            con = ds8.getConnection();

            tran.begin();
            Statement stmt = con.createStatement();
            stmt.executeUpdate("insert into cities values ('Rochester', 106769, 'Olmsted')");
            result = stmt.executeQuery("select county from cities where name='Rochester'");
            if (!result.next())
                throw new Exception("Entry missing from database");
            String value = result.getString(1);
            if (!"Olmsted".equals(value))
                throw new Exception("Incorrect value: " + value);
        } finally {
            if (con != null) {
                tran.rollback();
                con.close();
                con = null;
            }
        }

        try {
            con = ds8.getConnection();
            Statement stmt = con.createStatement();
            result = stmt.executeQuery("select county from cities where name='Rochester'");
            if (!result.next()) {
                throw new Exception("Entry missing from database after rollback. Connection should not have been enlisted in global transation.");
            }
            String value = result.getString(1);
            if (!"Olmsted".equals(value))
                throw new Exception("Incorrect value: " + value);
        } finally {
            if (con != null) {
                result.close();
                dropTable(con, CITYTABLE);
                con.close();
	    }
        }
    }

    /**
     * Called by testTNConfigTnsl in JDBCDerbyTest
     * Data Sources - ds8 = DSConfig(TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     *
     * Data source has been updated to Transactional = true in JDBCDerbyTest
     * ensure that the configuration of TRANSACTION_NONE prevents the data source
     * from being created.
     */
    public void testTNTransationEnlistmentModified() throws Throwable {
        InitialContext ctx = new InitialContext();
        try {
            @SuppressWarnings("unused")
            DataSource ds8 = (DataSource) ctx.lookup("jdbc/dsfat8");
            fail("Creation of data source should have thrown an exception since a config with Isolation Level = TRANSACTION_NONE and Transactional = true should be rejected.");
        } catch (Exception e) {
            assertTrue("Exception message should have contained", e.getMessage().contains("CWWKN0008E"));
        }
    }

    /**
     * Data Sources - ds1 = DSConfig(TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     *
     * Ensure we reject setAutoCommit(false)
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException" })
    public void testTNRejectAutoCommit() throws Throwable {
        Connection con = null;
        boolean expected = true;

        try {
            con = ds1.getConnection();
            tran.begin();
            /*
             * This should throw an exception because we are not enlisted in a global
             * transaction (see previous testTransationEnlistment) so autoCommit(false)
             * would prevent changes from being committed overall.
             *
             * If this behavior is undesired in the future consider a requirement to set
             * CommitOrRollbackOnCleanup on dataSource.
             */
            con.setAutoCommit(false);
            fail("Exception should have been thrown when setting AutoCommit to false.");

        } catch (SQLException sql) {
            assertTrue("Exception message should have contained", sql.getMessage().contains("DSRA4010E"));

            //ensure desired behavior
            assertEquals("Auto commit should not have been changed", expected, con.getAutoCommit());
        } finally {
            if (con != null) {
                tran.rollback();
                con.commit();
                con.close();
            }
        }
    }

    /**
     * Data Source - ds1 = DSConfig(TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     * Data Source - ds1ref = DSConfig(TRAN_NONE) + Res-ref (TRAN_NONE) + TRAN_NONE JDBC Driver
     *
     * Test with and without resource references that con.getTransactionIsolation() returns
     * TRANSACTION_NONE when using a TRAN_NONE JDBC Driver and a data source set to TRAN_NONE.
     */
    @Test
    public void testTNResRefBehavior() throws Throwable {
        int expected = Connection.TRANSACTION_NONE;
        try (Connection con = ds1.getConnection()) {
            assertEquals("Connection with dsConfig = TRAN_NONE and no res-ref should use driver default iso lvl: ", expected, con.getTransactionIsolation());
        }

        try (Connection con = ds1ref.getConnection()) {
            assertEquals("Connection with dsConfig = TRAN_NONE and res-ref = TRAN_NONE should use driver default iso lvl: ", expected, con.getTransactionIsolation());
        }
    }

    /**
     * Data Source - dsd5 = DataSourceDef (TRAN_NONE) + No Res-ref
     * Data Source - dsd5ref = DataSourceDef (TRAN_NONE) + Res-ref (TRAN_NONE)
     *
     * Test with and without a resource reference that conn.getTransactionIsolation() returns
     * TRANSACTION_NONE when using a TRAN_NONE JDBC Driver and a data source definition set to TRAN_NONE.
     */
    @Test
    public void testTNResRefBehaviorDSDef() throws Throwable {
        int expected = Connection.TRANSACTION_NONE;
        try (Connection con = dsd5.getConnection()) {
            assertEquals("Connection with dsConfig = TRAN_NONE and no res-ref should use driver default iso lvl: ", expected, con.getTransactionIsolation());
        }
        try (Connection con = dsd5ref.getConnection()) {
            assertEquals("Connection with dsConfig = TRAN_NONE and res-ref = TRAN_NONE should use driver default iso lvl: ", expected, con.getTransactionIsolation());
        }
    }

    /**
     * Data Source - ds2ref = dsConfig (no iso lvl) + Res-ref (TRAN_NONE) + Normal JDBC Driver
     * Data Source - ds0ref = dsConfig (TRAN_SERIALIZABLE) + Res-ref (TRAN_NONE) + Normal JDBC Driver
     *
     * Test that when resource reference isolation level is TRANSACTION_NONE that our behavior is unchanged if the
     * data source config isolation level is unspecified or is something other than TRANSACTAION_NONE.
     */
    @Test
    public void testTNResRefNoneBehavior() throws Throwable {
        try (Connection con = ds2ref.getConnection()) {
            int expected = Connection.TRANSACTION_REPEATABLE_READ; //WAS default
            assertEquals("Connection with no dsConfig and res-ref = TRAN_NONE should use WAS default iso lvl: ", expected, con.getTransactionIsolation());
        }

        try (Connection con = ds0ref.getConnection()) {
            int expected = Connection.TRANSACTION_SERIALIZABLE; //dsConfig
            assertEquals("Connection with dsConfig = TRAN_SERIALIZABLE and res-ref = TRAN_NONE should use dsConfig iso lvl: ", expected, con.getTransactionIsolation());
        }
    }

    /**
     * Data Source - dsd6ref = DataSourceDef(no iso lvl) + Res-ref (TRAN_NONE)
     * Data Source - dsd7ref = DataSourceDef(TRAN_SERIALIZABLE) + Res-ref (TRAN_NONE)
     *
     * Test that when resource reference isolation level is TRANSACTION_NONE that our behavior is unchanged if the
     * data source definition isolation level is unspecified or is something other than TRANSACTION_NONE
     */
    @Test
    public void testTNResRefNoneBehaviorDSDef() throws Throwable {
        try (Connection con = dsd6ref.getConnection()) {
            int expected = Connection.TRANSACTION_REPEATABLE_READ; //WAS default
            assertEquals("Connection with no dsConfig and res-ref = TRAN_NONE should use WAS default iso lvl: ", expected, con.getTransactionIsolation());
        }

        try (Connection con = dsd7ref.getConnection()) {
            int expected = Connection.TRANSACTION_SERIALIZABLE; //dsConfig
            assertEquals("Connection with dsConfig = TRAN_SERIALIZABLE and res-ref = TRAN_NONE should use dsConfig iso lvl: ", expected, con.getTransactionIsolation());
        }
    }

    /**
     * Called by testTNConfigIsoLvl in JDBCDerbyTest
     * Data Source - dsX = DSConfig (TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     *
     * Ensure that the unmodified data source has the expected isolation level.
     */
    public void testTNOriginalIsoLvl() throws Throwable {
        int expected = Connection.TRANSACTION_NONE;

        InitialContext ctx = new InitialContext();
        DataSource dsX = (DataSource) ctx.lookup("jdbc/dsfatX");

        try (Connection con = dsX.getConnection()) {
            int actual = con.getTransactionIsolation();
            assertEquals("Connection should have had an isolation level of: ", expected, actual);
        }
    }

    /**
     * Called by testTNConfigIsoLvl in JDBCDerbyTest
     * Data Source - dsX = DSConfig (TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     *
     * Ensure that the modified data source has the expected isolation level.
     */
    public void testTNModifiedIsoLvl() throws Throwable {
        int expected = Connection.TRANSACTION_SERIALIZABLE; //Modified isolation level

        InitialContext ctx = new InitialContext();
        DataSource dsX = (DataSource) ctx.lookup("jdbc/dsfatX");

        try (Connection con = dsX.getConnection()) {
            int actual = con.getTransactionIsolation();
            assertEquals("Connection should have had a modified isolation level of: ", expected, actual);
        }
    }

    /**
     * Called by testTNConfigIsoLvl in JDBCDerbyTest
     * Data Source - dsX = DSConfig (TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     *
     * Ensure that the attempt to switch back to TRANSACTION_NONE results in an error when attempting to get connection.
     */
    public void testTNRevertedIsoLvl() throws Throwable {
        try {
            InitialContext ctx = new InitialContext();
            DataSource dsX = (DataSource) ctx.lookup("jdbc/dsfatX");
            @SuppressWarnings("unused")
            final Connection con = dsX.getConnection();
            fail("Connection should have thrown an exception since the JDBC driver does not support setting isolation level to TRANSACTION_NONE.");
        } catch (SQLException sql) {
            assertTrue("Exception message should have contained", sql.getMessage().contains("DSRA4011E"));
        }
    }

    @Test
    public void testVerifyConnectionPrecedence() throws Throwable {
        try (Connection con = driver_url_preferred.getConnection(); Statement stmt = con.createStatement();) {
            stmt.executeUpdate("create table " + CITYTABLE + " (" + CITYSCHEMA + ")");
        }
    }
}
