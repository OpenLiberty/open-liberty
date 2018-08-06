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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@DataSourceDefinitions({
                         @DataSourceDefinition(name = "java:app/env/jdbc/dsd-infer-driver-class",
                                               className = "",
                                               isolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED,
                                               // loginTimeout = 76, // TODO decide whether/how to support for driver path
                                               maxPoolSize = 2,
                                               url = "jdbc:fatdriver:memory:jdbcdriver1",
                                               user = "dbuser1",
                                               password = "{xor}Oz0vKDtu",
                                               properties = {
                                                              "internal.nonship.function=This is for internal development only. Never use this in production",
                                                              "createDatabase=create",
                                                              "onConnect=insert into address values ('Rochester International Airport', 7600, 'Helgerson Dr SW', 'Rochester', 'MN', 55902)",
                                                              "queryTimeout=1m16s"
                                               }),
                         @DataSourceDefinition(name = "java:module/env/jdbc/dsd-infer-datasource-class",
                                               className = "",
                                               databaseName = "memory:jdbcdriver1;create=true",
                                               properties = {
                                                              "internal.nonship.function=This is for internal development only. Never use this in production",
                                               },
                                               user = "dbuser1",
                                               password = "{xor}Oz0vKDtu")

})

@SuppressWarnings("serial")
@WebServlet("/JDBCDriverManagerServlet")
public class JDBCDriverManagerServlet extends FATServlet {

    @Resource(name = "jdbc/fatDataSource", shareable = false, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds;

    @Resource
    DataSource xads;

    @Resource(name = "jdbc/fatDriver")
    DataSource fatDriverDS;

    @Resource
    UserTransaction tx;

    @Override
    public void init() throws ServletException {
        try {
            DataSource dds = InitialContext.doLookup("jdbc/fatDriver");
            // match the user/password specified for container auth data in server.xml
            Connection con = dds.getConnection("dbuser1", "dbpwd1");
            try {
                Statement s = con.createStatement();
                s.execute("create table address (name varchar(50) not null primary key, num int, street varchar(80), city varchar(40), state varchar(2), zip int)");
                s.executeUpdate("insert into address values ('IBM Rochester Building 050-2 H215-1', 3605, 'Hwy 52 N', 'Rochester', 'MN', 55901)");
                s.close();
            } finally {
                con.close();
            }
        } catch (NamingException x) {
            throw new ServletException(x);
        } catch (SQLException x) {
            throw new ServletException(x);
        }
    }

    /**
     * Verify that it is possible to establish a second connection to a data source that is backed by a Driver,
     * which demonstrates that the URL and other properties are not lost upon the initial connection.
     * Also verifies that the user/password from the data source vendor properties are supplied to the driver
     * in the case of application authentication (which is default when looked up without a resource reference).
     */
    @Test
    public void testAnotherConnection() throws Exception {
        DataSource dds = InitialContext.doLookup("jdbc/fatDriver");
        Connection con = dds.getConnection();
        try {
            DatabaseMetaData mdata = con.getMetaData();
            String user = mdata.getUserName();
            assertEquals("dbuser2", user.toLowerCase());
        } finally {
            con.close();
        }
    }

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

    /**
     * Verify that className is optional in DataSourceDefinition (can be assigned to empty string),
     * in which case, in the absence of a url propety, we infer the data source class from the Driver class,
     * giving highest precedence to XADataSource.
     */
    @Test
    public void testUnspecifiedClassNameInDataSourceDefinitionWithoutURL() throws Exception {
        DataSource dsd = InitialContext.doLookup("java:module/env/jdbc/dsd-infer-datasource-class");

        // Prove it is two-phase capable by using multiple connections that cannot be shared in a transaction
        tx.begin();
        try {
            Connection con1 = dsd.getConnection();
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con1.getTransactionIsolation());

            DatabaseMetaData mdata1 = con1.getMetaData();
            String user1 = mdata1.getUserName();
            assertEquals("dbuser1", user1.toLowerCase());

            Statement s1 = con1.createStatement();
            s1.executeUpdate("insert into address values ('Mayo Clinic', 200, '1st St SW', 'Rochester', 'MN', 55902)");

            // Only possible to use this second, non-matching connection when the data source is capable of two-phase commit
            Connection con2 = dsd.getConnection("dbuser2", "dbpwd2");

            DatabaseMetaData mdata2 = con2.getMetaData();
            String user2 = mdata2.getUserName();
            assertEquals("dbuser2", user2.toLowerCase());

            ResultSet result = con2.createStatement().executeQuery("values 2");
            assertTrue(result.next());
            assertEquals(2, result.getInt(1));
            assertFalse(result.next());
            result.getStatement().getConnection().close();
        } finally {
            tx.commit();
        }
    }

    /**
     * Verify that className is optional in DataSourceDefinition (can be assigned to empty string),
     * in which case we choose the Driver class based on the URL.
     */
    @Test
    public void testUnspecifiedClassNameInDataSourceDefinitionWithURL() throws Exception {
        DataSource dsd = InitialContext.doLookup("java:app/env/jdbc/dsd-infer-driver-class");

        // assertEquals(76, dsd.getLoginTimeout()); // TODO if we find a way to support loginTimeout

        Connection con = dsd.getConnection();
        try {
            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con.getTransactionIsolation());

            DatabaseMetaData mdata = con.getMetaData();
            String user = mdata.getUserName();
            assertEquals("dbuser1", user.toLowerCase());

            String url = mdata.getURL(); // Due to delegation, Derby URL will be returned. It will include the database name.
            assertTrue(url, url.contains("memory:jdbcdriver1"));

            Statement s = con.createStatement();
            assertEquals(76, s.getQueryTimeout());

            ResultSet result = s.executeQuery("select num, street, zip from address where name = 'Rochester International Airport'");
            assertTrue(result.next());
            assertEquals(7600, result.getInt(1));
            assertEquals("Helgerson Dr SW", result.getString(2));
            assertEquals(55902, result.getInt(3));
            assertFalse(result.next());
        } finally {
            con.close();
        }
    }

    /**
     * Very basic test using a connection established via the Driver.
     * This test validates that the user name from the container authentication data is used for resource reference lookup with
     * auth=CONTAINER rather than the default user/password that are specified in the data source vendor properties.
     * It also verifies that we can read an entry that was previously written by the same data source when accessed via
     * a direct lookup, which equates to auth=APPLICATION where the same user/password as the container auth data were
     * explicitly requested.
     */
    @Test
    public void testUserForContainerAuth() throws Exception {
        Connection conn = fatDriverDS.getConnection();
        try {
            DatabaseMetaData mdata = conn.getMetaData();

            String user = mdata.getUserName();
            assertEquals("dbuser1", user.toLowerCase());

            System.out.println("Connected to " + mdata.getDatabaseProductName());
            ResultSet result = conn.createStatement().executeQuery("select city, zip from address where name = 'IBM Rochester Building 050-2 H215-1'");
            assertTrue(result.next());
            assertEquals("Rochester", result.getString(1));
            assertEquals(55901, result.getInt(2));
            assertFalse(result.next());
        } finally {
            conn.close();
        }
    }
}
