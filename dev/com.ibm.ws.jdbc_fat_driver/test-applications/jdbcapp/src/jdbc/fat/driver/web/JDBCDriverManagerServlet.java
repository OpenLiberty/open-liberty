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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@DataSourceDefinitions({
                         @DataSourceDefinition(name = "java:comp/env/jdbc/dsd-driver-class",
                                               className = "org.apache.derby.jdbc.AutoloadedDriver",
                                               url = "jdbc:derby:memory:jdbcdriver1",
                                               user = "dbuser1",
                                               password = "{xor}Oz0vKDtu",
                                               properties = "createDatabase=create"),
                         @DataSourceDefinition(name = "java:comp/env/jdbc/dsd-driver-interface",
                                               className = "java.sql.Driver",
                                               url = "jdbc:fatdriver:memory:jdbcdriver1",
                                               user = "dbuser1",
                                               password = "{xor}Oz0vKDtu",
                                               properties = "createDatabase=create"),
                         @DataSourceDefinition(name = "java:module/env/jdbc/dsd-infer-datasource-class",
                                               className = "",
                                               databaseName = "memory:jdbcdriver1;create=true",
                                               user = "dbuser1",
                                               password = "{xor}Oz0vKDtu"),
                         @DataSourceDefinition(name = "java:app/env/jdbc/dsd-with-login-timeout",
                                               className = "",
                                               loginTimeout = 76,
                                               url = "jdbc:fatdriver:memory:jdbcdriver1"),
                         @DataSourceDefinition(name = "java:app/env/jdbc/dsd-with-datasource-interface",
                                               className = "javax.sql.DataSource",
                                               databaseName = "memory:jdbcdriver1;create=true",
                                               user = "dbuser1",
                                               password = "{xor}Oz0vKDtu"),
                         @DataSourceDefinition(name = "java:app/env/jdbc/dsd-with-xadatasource-interface",
                                               className = "javax.sql.XADataSource",
                                               databaseName = "memory:jdbcdriver1;create=true",
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
     * Verify a data source backed by javax.sql.ConnectionPoolDataSource can be discovered based on the
     * Driver package and class name.
     */
    @Test
    public void testConnectionPoolDataSource() throws Exception {
        DataSource proxypoolds = InitialContext.doLookup("jdbc/proxypoolds");

        // Confirm that value configured in server.xml is set on the data source
        assertEquals(200, proxypoolds.getLoginTimeout());

        Connection con = proxypoolds.getConnection("pxuser2", "pxpwd2");
        try {
            DatabaseMetaData mdata = con.getMetaData();
            assertEquals("Proxy Pool Driver", mdata.getDriverName());
            assertEquals("pxuser2", mdata.getUserName());

            // Properties configured in server.xml:
            assertEquals("proxydb", con.getCatalog());
            assertEquals("pxschema2", con.getSchema());
        } finally {
            con.close();
        }
    }

    /**
     * Verify the exception path where ConnectionPoolDataSource is requested but no
     * ConnectionPoolDataSource implementation is available from the driver.
     */
    @ExpectedFFDC("java.sql.SQLNonTransientException")
    @Test
    public void testConnectionPoolDataSourceNotFound() throws Exception {
        try {
            DataSource cpds = InitialContext.doLookup("jdbc/fatConnectionPoolDataSource");
            fail("Should not be able to look up data source configured with type of ConnectionPoolDataSource " +
                 "when the JDBC driver doesn't provide an implementation of one. " + cpds);
        } catch (NamingException x) {
            // expected - unfortunately the cause is not chained
        }
    }

    /**
     * Verify that DataSourceDefinition can specify a java.sql.Driver implementation by its class name,
     * and it will be loaded and used, regardless of whether the driver class is present in
     * META-INF/services/java.sql.Driver
     */
    @Test
    public void testDataSourceDefinitionWithDerbyDriver() throws Exception {
        DataSource derbyds = InitialContext.doLookup("java:comp/env/jdbc/dsd-driver-class");
        Connection con = derbyds.getConnection();
        try {
            con.createStatement().executeUpdate("insert into address values ('Quarry Hill Nature Center', 701, 'Silver Creek Road NE', 'Rochester', 'MN', 55906)");
        } finally {
            con.close();
        }
    }

    /**
     * Verify a data source backed by java.sql.Driver that isn't available on the application class loader,
     * and is only available in the shared library that is supplied to the dataSource/jdbcDriver in server config.
     */
    @Test
    public void testDriverUnvailableToApplicationClassLoader() throws Exception {
        DataSource proxyds = InitialContext.doLookup("jdbc/proxydriver");
        Connection con = proxyds.getConnection("user1", "pwd1");
        try {
            DatabaseMetaData mdata = con.getMetaData();
            assertEquals("Proxy Driver", mdata.getDriverName());
            assertEquals("user1", mdata.getUserName());

            // Properties configured in server.xml:
            assertEquals("proxydb", con.getCatalog());
            assertEquals("pxschema1", con.getSchema());
        } finally {
            con.close();
        }
    }

    /**
     * Verify that DataSourceDefinitions can specify javax.sql.DataSource and javax.sql.XADataSource as the class name,
     * and Liberty will discover a data source implementation class of the specified type. In order to confirm that the
     * correct implementation is chosen, attempt to enlist multiple non-shared connections from the javax.sql.DataSource,
     * requiring the second to fail, but show that enlisting another resource from the javax.sql.XADataSource succeeds.
     */
    @ExpectedFFDC({
                    "java.lang.IllegalStateException", // intentional failure to enlist second resource that isn't two-phase capable
                    "java.sql.SQLException", // intentional failure to enlist second resource that isn't two-phase capable
                    "javax.resource.ResourceException" // intentional failure to enlist second resource that isn't two-phase capable
    })
    @Test
    public void testInterfaceAsClassNameInDataSourceDefinition() throws Exception {
        DataSource dsd_ds = InitialContext.doLookup("java:app/env/jdbc/dsd-with-datasource-interface");

        int txStatus;
        tx.begin();
        try {
            Connection con1 = dsd_ds.getConnection();
            con1.createStatement().executeUpdate("insert into address values ('University of Minnesota Rochester', 111, 'S Broadway #300', 'Rochester', 'MN', 55904)");

            // If dsd_ds is backed by javax.sql.DataSource, we will not be able to enlist a second connection,
            try {
                Connection con2 = dsd_ds.getConnection("newUser", "newPassword");
                con2.createStatement().executeUpdate("insert into address values ('Apache Mall', 52, 'US-14', 'Rochester', 'MN', 55902)");
            } catch (SQLException x) {
                boolean causedByEnlistmentFailure = false;
                for (Throwable c = x; !causedByEnlistmentFailure && c != null; c = c.getCause())
                    causedByEnlistmentFailure |= c instanceof IllegalStateException;
                if (!causedByEnlistmentFailure)
                    throw x;
            }
        } finally {
            txStatus = tx.getStatus();
            tx.rollback();
        }

        assertEquals(Status.STATUS_MARKED_ROLLBACK, txStatus);

        tx.begin();
        try {
            Connection con1 = dsd_ds.getConnection();
            con1.createStatement().executeUpdate("insert into address values ('Mayo Civic Center', 30, 'Civic Center Dr SE', 'Rochester', 'MN', 55904)");

            // If dsd_xa is backed by javax.sql.XADataSource, then another connection can be enlisted,
            DataSource dsd_xa = InitialContext.doLookup("java:app/env/jdbc/dsd-with-xadatasource-interface");
            Connection con2 = dsd_xa.getConnection();
            con2.createStatement().executeUpdate("insert into address values ('Indian Heights Park', 1800, 'Terracewood Dr NW', 'Rochester', 'MN', 55901)");
        } finally {
            tx.commit();
        }
    }

    /**
     * Verify that DataSourceDefinition can specify java.sql.Driver as the class name,
     * and Liberty will use the ServiceLoader to select a driver based on the URL.
     */
    @Test
    public void testJavaSqlDriverInDataSourceDefinition() throws Exception {
        DataSource derbyds = InitialContext.doLookup("java:comp/env/jdbc/dsd-driver-interface");
        Connection con = derbyds.getConnection();
        try {
            con.createStatement().executeUpdate("insert into address values ('Silver Lake Park', 840, '7th St NE', 'Rochester', 'MN', 55906)");
        } finally {
            con.close();
        }
    }

    /**
     * Verify that a JDBC vendor (Derby) that Liberty has built-in knowledge of
     * can also be used as a java.sql.Driver rather than as a data source impl class if so configured.
     */
    @Test
    public void testServerConfiguredDerbyDriver() throws Exception {
        DataSource derbyds = InitialContext.doLookup("jdbc/derby");
        Connection con = derbyds.getConnection();
        try {
            DatabaseMetaData mdata = con.getMetaData();
            String url = mdata.getURL();
            assertTrue(url, url.startsWith("jdbc:derby:"));

            String user = mdata.getUserName();
            assertEquals("dbuser1", user);
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
     * in which case, in the absence of a url property, we infer the data source class from the Driver class,
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

    //Test that setting the LoginTimeout via URL or properties for DataSources using Driver is rejected and that getLoginTimeout always returns 0.
    @Test
    @ExpectedFFDC({ "java.sql.SQLNonTransientException" })
    public void testGetSetLoginTimeout() throws Exception {
        InitialContext ctx = new InitialContext();
        //Ensure URL with loginTimeout specified is not allowed when using Driver
        try {
            ctx.lookup("jdbc/fatDriverInvalidURLLoginTimeout");
            fail("URL containing LoginTimeout should not be allowed when using Driver");
        } catch (Exception e) {
        } //expected
          //Ensure datasource with property loginTimeout is not allowed when using Driver
        try {
            ctx.lookup("java:app/env/jdbc/dsd-with-login-timeout");
            fail("loginTimeout property not allowed when using Driver");
        } catch (Exception e) {
        } //expected
          //Datasource using a driver should return 0 (which means use defaults) for loginTimeout, regardless of value set on DriverManager
        DriverManager.setLoginTimeout(10 * 60); //10 minutes

        assertEquals("Login timeout should be 0 regardless of what is done to DriverManager", 0, fatDriverDS.getLoginTimeout());
    }

    //Test that you are unable to set the logwriter on the DataSource and that the getLogWriter method returns null
    @Test
    public void testGetSetLogWriter() throws Exception {
        try {
            fatDriverDS.setLogWriter(new PrintWriter(System.out));
        } catch (SQLFeatureNotSupportedException ex) {
        } //expected
        assertNull("The getLogWriter method should always return null when using Driver", fatDriverDS.getLogWriter());
    }

    //Test that ensures unwrapping a DataSource backed by Driver to the underlying Driver interface is not possible
    @Test
    @ExpectedFFDC({ "java.sql.SQLException" })
    public void testUnwrapDriver() throws Exception {
        assertFalse("fatDriverDS should not wrap Driver", fatDriverDS.isWrapperFor(Driver.class));
        try {
            fatDriverDS.unwrap(Driver.class);
            fail("Should not be able to unwrap to the Driver interface");
        } catch (SQLException ex) {
            if (!ex.getMessage().contains("DSRA9122E"))
                throw ex;
        }

        //It should however still be able to be unwrapped to an interface impl'd by WsJdbcDataSource
        assertTrue("fatDriverDS should wrap CommonDataSource", fatDriverDS.isWrapperFor(CommonDataSource.class));
        CommonDataSource ds = fatDriverDS.unwrap(CommonDataSource.class);
        assertSame("The WSJdbcDataSource instance should have been returned by the call to unwrap", fatDriverDS, ds);
    }

}
