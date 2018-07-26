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
package jdbc.fat.driver.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/JDBCDriverManagerServlet")
public class JDBCDriverManagerServlet extends FATServlet {

    @Resource(name = "jdbc/fatDataSource", shareable = false, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds;

    @Resource
    DataSource xads;

    /**
     * Test of basic database connectivity
     */
    @Test
    public void testBasicConnection() throws Exception {
        InitialContext context = new InitialContext();
        UserTransaction tran = (UserTransaction) context.lookup("java:comp/UserTransaction");
        Connection con = ds.getConnection();
        try {
            DatabaseMetaData metadata = con.getMetaData();
            String dbName = metadata.getDatabaseProductName();
            System.out.println("Database Name=" + dbName);
            String dbVersion = metadata.getDatabaseProductVersion();
            System.out.println("Database Version=" + dbVersion);

            // Set up table
            Statement stmt = con.createStatement();
            try {
                stmt.executeUpdate("drop table drivertable");
            } catch (SQLException x) {
                // didn't exist
            }
            stmt.executeUpdate("create table drivertable (col1 int not null primary key, col2 varchar(20))");

            // Insert data
            PreparedStatement ps = con.prepareStatement("insert into drivertable values (?, ?)");
            ps.setInt(1, 45);
            ps.setString(2, "XLV");
            ps.executeUpdate();
            ps.setInt(1, 91);
            ps.setString(2, "XCI");
            ps.executeUpdate();
            ps.setInt(1, 13);
            ps.setString(2, "XIII");
            ps.executeUpdate();
            ps.close();

            tran.begin();
            try {
                stmt.executeUpdate("update drivertable set col1=24, col2='XXIV' where col1=13");
            } finally {
                tran.commit();
            }

            // Query for updates
            ResultSet rs = stmt.executeQuery("select col1, col2 from drivertable order by col1 asc");

            assertTrue("Expected another row in the result set.", rs.next());
            assertEquals(24, rs.getInt(1));
            assertEquals("XXIV", rs.getString(2));

            assertTrue("Expected another row in the result set.", rs.next());
            assertEquals(45, rs.getInt(1));
            assertEquals("XLV", rs.getString(2));

            assertTrue("Expected another row in the result set.", rs.next());
            assertEquals(91, rs.getInt(1));
            assertEquals("XCI", rs.getString(2));

            assertFalse("Unexpected row in the result set.", rs.next());

            rs.close();
            stmt.close();

        } finally {
            con.close();
        }
    }

    /**
     * Test enlistment in transactions.
     */
    @Test
    public void testTransactionEnlistment() throws Exception {
        InitialContext context = new InitialContext();
        Connection con = ds.getConnection();
        try {
            // Set up table
            Statement stmt = con.createStatement();

            try {
                stmt.executeUpdate("drop table drivertable");
            } catch (SQLException x) {
                // didn't exist
            }
            stmt.executeUpdate("create table drivertable (col1 int not null primary key, col2 varchar(20))");
            stmt.executeUpdate("insert into drivertable values (1, 'one')");
            stmt.executeUpdate("insert into drivertable values (2, 'two')");
            stmt.executeUpdate("insert into drivertable values (3, 'three')");
            stmt.executeUpdate("insert into drivertable values (4, 'four')");

            // UserTransaction Commit
            con.setAutoCommit(false);
            UserTransaction tran = (UserTransaction) context.lookup("java:comp/UserTransaction");
            tran.begin();
            try {
                stmt.executeUpdate("update drivertable set col2='uno' where col1=1");

                // Enlist second resource (must be two-phase capable)
                Connection con2 = xads.getConnection();
                Statement stmt2 = con2.createStatement();
                stmt2.executeUpdate("insert into drivertable values (5, 'five')");
            } finally {
                tran.commit();
            }
            con.rollback(); // shouldn't have any impact because update was made in UserTransaction

            ResultSet result = stmt.executeQuery("select col2 from drivertable where col1=1");
            assertTrue("entry with col1=1 not found in table", result.next());
            String value = result.getString(1);
            assertEquals("UserTransaction commit not honored. Incorrect value: " + value, "uno", value);

            con.commit();

            // UserTransaction Rollback
            tran.begin();
            try {
                stmt.executeUpdate("update drivertable set col2='dos' where col1=2");
            } finally {
                tran.rollback();
            }
            con.commit(); // shouldn't have any impact because update was made in UserTransaction

            result = stmt.executeQuery("select col2 from drivertable where col1=2");
            assertTrue("entry with col1=2 not found in table", result.next());
            value = result.getString(1);
            assertEquals("UserTransaction rollback not honored. Incorrect value: " + value, "two", value);

            // Connection commit
            stmt.executeUpdate("update drivertable set col2='tres' where col1=3");
            con.commit();
            result = stmt.executeQuery("select col2 from drivertable where col1=3");
            assertTrue("entry with col1=3 not found in table", result.next());
            value = result.getString(1);
            assertEquals("Connection commit not honored. Incorrect value: " + value, "tres", value);

            // Connection rollback
            stmt.executeUpdate("update drivertable set col2='cuatro' where col1=4");
            con.rollback();
            result = stmt.executeQuery("select col2 from drivertable where col1=4");
            assertTrue("entry with col1=4 not found in table", result.next());
            value = result.getString(1);
            assertEquals("Connection rollback not honored. Incorrect value: " + value, "four", value);

        } finally {
            try {
                con.rollback();
            } catch (Throwable x) {
            }
            con.close();
        }
    }

    //TODO this test can be removed once other tests are updated to use the custom FAT driver
    @Test
    public void testFATDriver() throws Exception {
        Class.forName("jdbc.fat.driver.derby.FATDriver");
        Connection conn = DriverManager.getConnection("jdbc:derby:memory:wrappedDerby;create=true");
        try {
            System.out.println("Connected to " + conn.getMetaData().getDatabaseProductName());
            conn.createStatement().executeQuery("VALUES 1");
        } finally {
            conn.close();
        }

    }
}
