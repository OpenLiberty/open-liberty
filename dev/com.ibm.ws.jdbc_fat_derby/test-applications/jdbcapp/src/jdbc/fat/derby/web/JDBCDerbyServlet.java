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
package jdbc.fat.derby.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/JDBCDerbyServlet")
public class JDBCDerbyServlet extends FATServlet {

    @Resource(name = "jdbc/dsfat0ref", lookup = "jdbc/dsfat0")
    DataSource ds0; //DSConfig (TRAN_SERIALIZABLE) + Res-ref (TRAN_NONE) + Normal JDBC Driver

    @Resource(lookup = "jdbc/dsfat1")
    DataSource ds1; //DSConfig (TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver

    @Resource(name = "jdbc/dsfat2ref", lookup = "jdbc/dsfat2")
    DataSource ds2; //DSConfig (no iso lvl) + Res-ref (TRAN_NONE) + Normal JDBC Driver

    @Resource(name = "jdbc/dsfat3ref", lookup = "jdbc/dsfat3")
    DataSource ds3; //DSConfig (TRAN_NONE) + Res-ref (TRAN_NONE) + TRAN_NONE JDBC Driver

    @Resource
    private UserTransaction tran;

    /**
     * Create the default table used by the tests.
     */
    private void createTable(DataSource ds) throws SQLException {
        Connection con = ds.getConnection();
        try {
            Statement st = con.createStatement();
            try {
                st.executeUpdate("drop table cities");
            } catch (SQLException x) {
            }
            String dbProductName = con.getMetaData().getDatabaseProductName().toUpperCase();
            if (dbProductName.contains("IDS") || dbProductName.contains("INFORMIX")) // Informix JCC and JDBC
                st.executeUpdate("create table cities (name varchar(50) not null primary key, population int, county varchar(30)) LOCK MODE ROW");
            else
                st.executeUpdate("create table cities (name varchar(50) not null primary key, population int, county varchar(30))");
        } finally {
            con.close();
        }
    }

    /**
     * Data Source - ds1 = DSConfig(TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     *
     * Ensure that if we try to change Transaction Isolation that we throw an exception.
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException" })
    public void testRejectIsolationChange() throws Throwable {
        Connection con = null;
        try {
            con = ds1.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            fail("Exception should have been thrown when swiching isolation levels on a driver configured with TRANSACTION_NONE.");
        } catch (SQLException sql) {
            assertTrue("Exception message should have contained", sql.getMessage().contains("DSRA4011E"));
        } finally {
            con.close();
        }
    }

    /**
     * Data Source - ds4 = DSConfig(TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     *
     * This data source is not configured with "Transactional = false" which should cause
     * the creation of the data source to fail
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException" })
    public void testTransactionalSetting() throws Throwable {
        InitialContext ctx = new InitialContext();

        try {
            DataSource ds4 = (DataSource) ctx.lookup("jdbc/dsfat4");
            fail("Lookup should have failed due to bad config.");
        } catch (Exception e) {
            assertTrue("Exception message should have contained", e.getMessage().contains("CWWKN0008E"));
        }
    }

    /**
     * Data Sources - ds1 = DSConfig(TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     *
     * Ensure connection is not enlisted in transaction
     * Begin global transaction, do work, roll-back transaction, ensure work was not rolled-back.
     */
    @Test
    public void testTransationEnlistment() throws Throwable {
        createTable(ds1);
        Connection con = null;

        try {
            con = ds1.getConnection();
            tran.begin();
            Statement stmt = con.createStatement();
            stmt.executeUpdate("insert into cities values ('Rochester', 106769, 'Olmsted')");
            ResultSet result = stmt.executeQuery("select county from cities where name='Rochester'");
            if (!result.next())
                throw new Exception("Entry missing from database");
            String value = result.getString(1);
            if (!"Olmsted".equals(value))
                throw new Exception("Incorrect value: " + value);
        } finally {
            tran.rollback();
            con.close();
        }

        try {
            con = ds1.getConnection();
            Statement stmt = con.createStatement();
            ResultSet result = stmt.executeQuery("select county from cities where name='Rochester'");
            if (!result.next()) {
                throw new Exception("Entry missing from database after rollback. Connection should not have been enlisted in global transation.");
            }
            String value = result.getString(1);
            if (!"Olmsted".equals(value))
                throw new Exception("Incorrect value: " + value);
        } finally {
            con.close();
        }
    }

    /**
     * Data Sources - ds1 - DSConfig(TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     *
     * Ensure we reject setAutoCommit(false)
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException" })
    public void testRejectAutoCommit() throws Throwable {
        createTable(ds1);
        Connection con = null;

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

            Statement stmt = con.createStatement();
            stmt.executeUpdate("insert into cities values ('Rochester', 106769, 'Olmsted')");
            ResultSet result = stmt.executeQuery("select county from cities where name='Rochester'");
            if (!result.next())
                throw new Exception("Entry missing from database");
            String value = result.getString(1);
            if (!"Olmsted".equals(value))
                throw new Exception("Incorrect value: " + value);

            fail("Exception should have been thrown when setting AutoCommit to false.");

        } catch (SQLException sql) {
            assertTrue("Exception message should have contained", sql.getMessage().contains("DSRA4010E"));
        } finally {
            tran.rollback();
            con.commit();
            con.close();
        }
    }

    /**
     * Data Source - ds1 - DSConfig(TRAN_NONE) + No Res-ref + TRAN_NONE JDBC Driver
     * Data Source - ds3 = DSConfig(TRAN_NONE) + Res-ref (TRAN_NONE) + TRAN_NONE JDBC Driver
     *
     * Test with and without res-ref that conn.getTransactionIsolation() returns
     * Driver's isolation level when server config is set to TRAN_NONE
     */
    @Test
    public void testResourceRefNone() throws Throwable {
        Connection con = null;
        int expected = Connection.TRANSACTION_NONE;

        try {
            con = ds1.getConnection();
            assertEquals("Connection with dsConfig = TRAN_NONE and no res-ref should use driver default iso lvl: ", expected, con.getTransactionIsolation());
        } finally {
            con.close();
        }

        try {
            con = ds3.getConnection();
            assertEquals("Connection with dsConfig = TRAN_NONE and res-ref = TRAN_NONE should use driver default iso lvl: ", expected, con.getTransactionIsolation());
        } finally {
            con.close();
        }

    }

    /**
     * Data Source - ds2 - dsConfig (no iso lvl) + Res-ref (TRAN_NONE) + Normal JDBC Driver
     * Data Source - ds0 - dsConfig (TRAN_SERIALIZABLE) + Res-ref (TRAN_NONE) + Normal JDBC Driver
     *
     * Test that when res-ref isolation level is TRAN_NONE that our behavior is unchanged if the
     * data source config isolation level is unspecified or is something other than TRAN_NONE
     */
    @Test
    public void testResourceRefBehavior() throws Throwable {
        Connection con = null;

        try {
            int expected = Connection.TRANSACTION_REPEATABLE_READ; //WAS default
            con = ds2.getConnection();
            assertEquals("Connection with no dsConfig and res-ref = TRAN_NONE should use WAS default iso lvl: ", expected, con.getTransactionIsolation());
        } finally {
            con.close();
        }

        try {
            int expected = Connection.TRANSACTION_SERIALIZABLE; //dsConfig
            con = ds0.getConnection();
            assertEquals("Connection with dsConfig = TRAN_SERIALIZABLE and res-ref = TRAN_NONE should use dsConfig iso lvl: ", expected, con.getTransactionIsolation());
        } finally {
            con.close();
        }
    }

}
