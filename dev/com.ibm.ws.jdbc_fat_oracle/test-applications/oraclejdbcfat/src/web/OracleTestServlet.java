/*******************************************************************************
 * Copyright (c) 2016, 2023 IBM Corporation and others.
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
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipIfSysProp;
import componenttest.app.FATServlet;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleStatement;
import oracle.jdbc.OracleTypes;
import oracle.jdbc.datasource.OracleCommonDataSource;
import oracle.jdbc.datasource.OracleConnectionPoolDataSource;
import oracle.jdbc.datasource.OracleXADataSource;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/OracleTestServlet")
public class OracleTestServlet extends FATServlet {
    @Resource
    private DataSource ds;

    @Resource(lookup = "jdbc/driver-ds")
    private DataSource driver_ds;

    @Resource(lookup = "jdbc/generic-driver-ds")
    private DataSource generic_driver_ds;

    @Resource(name = "java:comp/jdbc/env/unsharable-ds-xa-loosely-coupled", shareable = false)
    private DataSource unsharable_ds_xa_loosely_coupled;

    @Resource(name = "java:comp/jdbc/env/unsharable-ds-xa-tightly-coupled", shareable = false)
    private DataSource unsharable_ds_xa_tightly_coupled;

    @Resource(lookup = "jdbc/driver-url-preferred")
    private DataSource driver_url_perferred;

    @Resource(lookup = "jdbc/ds-url-preferred")
    private DataSource ds_url_perferred;

    @Resource
    private UserTransaction tx;

    // Verify that connections are/are not castable to OracleConnection based on whether enableConnectionCasting=true/false.
    @Test
    public void testConnectionCasting() throws Exception {
        //Lookup instead of resource injection so this datasource is not looked up when running on IBMi
        DataSource ds_casting = InitialContext.doLookup("jdbc/casting-ds");
        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");

        OracleConnection ocon = (OracleConnection) ds_casting.getConnection();
        try {
            assertTrue(ocon.isUsable());

            Connection con = ocon.unwrap(Connection.class);
            assertTrue(con.isWrapperFor(OracleConnection.class));

            ocon = ocon.unwrap(OracleConnection.class);
            assertEquals(OracleConnection.DATABASE_OK, ocon.pingDatabase());

            ocon = con.unwrap(OracleConnection.class);
            String propsUserName = ocon.getProperties().getProperty("user");
            String metadataUserName = con.getMetaData().getUserName();
            assertEquals(metadataUserName, propsUserName);
        } finally {
            ocon.close();
        }

        Connection con = ds.getConnection();
        try {
            assertFalse(con instanceof OracleConnection);

            assertTrue(con.isWrapperFor(OracleConnection.class));

            String conUserName = con.unwrap(OracleConnection.class).getUserName();
            String metadataUserName = con.getMetaData().getUserName();
            assertEquals(metadataUserName, conUserName);
        } finally {
            con.close();
        }
    }

    // Verify that statements are not castable to OracleStatement, but can be unwrapped
    @Test
    public void testStatementCasting() throws Exception {
        //Lookup instead of resource injection so this datasource is not looked up when running on IBMi
        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");

        Connection con = ds.getConnection();
        Statement stmt;
        OracleStatement ostmt;

        //Test statement
        stmt = con.createStatement();
        try {
            ostmt = (OracleStatement) stmt;
            fail("Should not be able to cast to OracleStatement");
        } catch (Exception e) {
            assertTrue(e instanceof ClassCastException);
        }

        assertTrue(stmt.isWrapperFor(OracleStatement.class));
        ostmt = stmt.unwrap(OracleStatement.class);

        //Test prepared statement
        stmt = con.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");
        try {
            ostmt = (OraclePreparedStatement) stmt;
            fail("Should not be able to cast to OraclePreparedStatement");
        } catch (Exception e) {
            assertTrue(e instanceof ClassCastException);
        }

        assertTrue(stmt.isWrapperFor(OraclePreparedStatement.class));
        ostmt = stmt.unwrap(OraclePreparedStatement.class);

        //Test callable statement
        stmt = con.prepareCall("INSERT INTO MYTABLE VALUES(?,?)");
        try {
            ostmt = (OracleCallableStatement) stmt;
            fail("Should not be able to cast to OracleCallableStatement");
        } catch (Exception e) {
            assertTrue(e instanceof ClassCastException);
        }

        assertTrue(stmt.isWrapperFor(OracleCallableStatement.class));
        ostmt = stmt.unwrap(OracleCallableStatement.class);
    }

    // Test for oracle.jdbc.OracleCallableStatement.getCursor.  Should be able to execute a procedure that returns a cursor
    // and use getCursor to obtain a result set.  The parent statement of the result set should be the WAS statement wrapper.
    @Test
    public void testCursor() throws Exception {
        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");
        Connection con = ds.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");
            ps.setInt(1, 2);
            ps.setString(2, "two");
            ps.executeUpdate();
            ps.close();

            CallableStatement cs = con.prepareCall("BEGIN OPEN ? FOR SELECT STRVAL FROM MYTABLE WHERE ID=?; END;");
            cs.registerOutParameter(1, OracleTypes.CURSOR);
            cs.setInt(2, 2);
            cs.execute();

            OracleCallableStatement ocs = cs.unwrap(OracleCallableStatement.class);
            ResultSet result = ocs.getCursor(1);
            assertTrue(result.next());
            assertEquals("two", result.getString(1));
            assertFalse(result.next());

            assertEquals(cs, result.getStatement());
        } finally {
            con.close();
        }
    }

    // Test for the dataSource onConnect attribute that species SQL commands to Liberty to run on each new connection.
    // In this test we cover specifying multiple onConnect SQL commands on a single data source,
    // the use of transactional onConnect commands, as well as the use of Liberty variables in the onConnect SQL.
    @Test
    public void testOnConnectSQL() throws Exception {
        DataSource ds_casting = InitialContext.doLookup("jdbc/casting-ds");
        Connection con = ds_casting.getConnection();
        try {
            con.setAutoCommit(false);
            Statement stmt = con.createStatement();
            ResultSet result = stmt.executeQuery("SELECT CURTIME FROM TEMP1");
            assertTrue(result.next());
            assertNotNull(result.getNString(1));
            result.close();

            // Make sure that onConnect SQL doesn't roll back along with the transaction
            con.rollback();
            con.setAutoCommit(true);
            result = stmt.executeQuery("SELECT NUMCONNECTIONS FROM CONCOUNT");
            assertTrue(result.next());
            int numConnections = result.getInt(1);
            assertTrue("onConnect SQL should have incremented connection count. Instead " + numConnections, numConnections > 0);
        } finally {
            con.close();
        }
    }

    // Ensure that readOnly true throws an exception
    @Test
    @ExpectedFFDC({ "java.sql.SQLException" })
    public void testReadOnlyException() throws Exception {
        try (Connection con = ds.getConnection(); PreparedStatement ps = con.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");) {
            con.setReadOnly(true);
            ps.setInt(1, 4);
            ps.setString(2, "four");
            ps.executeUpdate();

            fail("Should not have been able to executeUpdate with read only set to true");
        } catch (SQLException e) {
            assertTrue("SQLException should have contained DSRA9010E", e.getMessage().toUpperCase().contains("DSRA9010E"));
        }

    }

    // Test for JDBC 4.2 ref cursors.  Should be able to execute a procedure that returns a cursor
    // and use getObject to obtain a result set.  The parent statement of the result set should be the WAS statement wrapper.
    @Test
    public void testRefCursor() throws Exception {
        Connection con = ds.getConnection();
        try {
            assertTrue(con.getMetaData().supportsRefCursors());

            PreparedStatement ps = con.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");
            ps.setInt(1, 3);
            ps.setString(2, "three");
            ps.executeUpdate();
            ps.close();

            CallableStatement cs = con.prepareCall("BEGIN OPEN ? FOR SELECT STRVAL FROM MYTABLE WHERE ID=?; END;");
            cs.registerOutParameter(1, OracleTypes.CURSOR); // Why doesn't Types.REF_CURSOR from JDBC 4.2 work here? Oracle bug?
            cs.setInt(2, 3);
            cs.execute();

            ResultSet result = cs.getObject(1, ResultSet.class);
            assertTrue(result.next());
            assertEquals("three", result.getString(1));
            assertFalse(result.next());

            assertEquals(cs, result.getStatement());
            cs.close();
            assertTrue(result.isClosed());
        } finally {
            con.close();
        }
    }

    // Test for oracle.jdbc.OraclePreparedStatement.getReturnResultSet.  Should be able to execute a DML statement
    // that returns values, obtain the return result set and use it.  The parent statement of the result set should
    // be the WAS statement wrapper.
    @Test
    public void testReturnResultSet() throws Exception {
        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");
        Connection con = ds.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");
            ps.setInt(1, 1);
            ps.setString(2, "one");
            ps.executeUpdate();
            ps.close();

            ps = con.prepareStatement("UPDATE MYTABLE SET STRVAL=STRVAL||' hundred' WHERE ID=? RETURNING STRVAL INTO ?");
            ps.setInt(1, 1);
            OraclePreparedStatement ops = ps.unwrap(OraclePreparedStatement.class);
            ops.registerReturnParameter(2, OracleTypes.NVARCHAR);
            assertEquals(1, ps.executeUpdate());

            ResultSet result = ops.getReturnResultSet();
            assertTrue(result.next());
            assertEquals("one hundred", result.getString(1));
            assertFalse(result.next());

            assertEquals(ps, result.getStatement());
        } finally {
            con.close();
        }
    }

    // Test configured value of newly supported roleName data source property for Oracle.
    @Test
    public void testRoleName() throws Exception {
        // First determine if we can run this test. Oracle driver for JDBC 4.2 is not compatible with Java 9+
        boolean atLeastJava9;
        try {
            Class.forName("java.lang.Runtime$Version"); // added in Java 9
            atLeastJava9 = true;
        } catch (ClassNotFoundException x) {
            atLeastJava9 = false;
        }
        if (atLeastJava9) {
            //Lookup instead of resource injection so this datasource is not looked up when running on IBMi
            DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");
            Connection con = ds.getConnection();
            try {
                DatabaseMetaData metadata = con.getMetaData();
                int jdbcMajor = metadata.getJDBCMajorVersion();
                int jdbcMinor = metadata.getJDBCMinorVersion();
                if (jdbcMajor < 4 || jdbcMajor == 4 && jdbcMinor <= 2) {
                    System.out.println("Skipped testRoleName because JDBC driver spec compliance level of " +
                                       jdbcMajor + '.' + jdbcMinor +
                                       " is not compatible with Java 9 or higher for unwrap operations");
                    return;
                }
            } finally {
                con.close();
            }
        }

        //Lookup instead of resource injection so this datasource is not looked up when running on IBMi
        DataSource ds_casting = InitialContext.doLookup("jdbc/casting-ds");
        OracleCommonDataSource ocds = ds_casting.unwrap(OracleCommonDataSource.class);
        assertEquals("TestRole", ocds.getRoleName());

        try {
            ocds.setRoleName("NewRole");
            fail("Should not be allowed to alter data source properties at run time");
        } catch (SQLFeatureNotSupportedException x) {
            if (!x.getMessage().contains("setRoleName"))
                throw x;
        }

        assertEquals("TestRole", ocds.getRoleName());
    }

    //Test that a datasource backed by Driver can be used with both the generic properties element and properties.oracle
    //element when type="java.sql.Driver"
    @Test
    public void testDSUsingDriver() throws Exception {
        Connection conn = driver_ds.getConnection();
        assertFalse("driver_ds should not wrap OracleCommonDataSource", driver_ds.isWrapperFor(OracleCommonDataSource.class));

        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");
            ps.setInt(1, 20);
            ps.setString(2, "twenty");
            ps.executeUpdate();
            ps.close();
        } finally {
            conn.close();
        }

        assertFalse("generic_driver_ds should not wrap OracleCommonDataSource", generic_driver_ds.isWrapperFor(OracleCommonDataSource.class));
        Connection conn2 = generic_driver_ds.getConnection();
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
    @SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to additional Db2 JDBC driver in JDK
    public void testInferOracleDataSource() throws Exception {
        //Lookup instead of resource injection so this datasource is not looked up when running on IBMi
        DataSource inferred_ds = InitialContext.doLookup("jdbc/inferred-ds");
        DataSource ds_casting = InitialContext.doLookup("jdbc/casting-ds");
        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");

        //The default datasource should continue to be inferred as an XADataSource, since it has properties.oracle configured
        assertTrue("default datasource should wrap OracleXADataSource",
                   ds.isWrapperFor(OracleXADataSource.class));

        //ds_casting should continue to be inferred as a ConnectionPoolDataSource, since it has properties.oracle configured
        assertTrue("ds_casting should wrap OracleConnectionPoolDataSource",
                   ds_casting.isWrapperFor(OracleConnectionPoolDataSource.class));

        //generic-driver-ds doesn't specify a type.  The presence of URL will result in the DataSource being back by Driver
        assertFalse("The presence of the URL should result in generic-driver-ds being back by Driver",
                    generic_driver_ds.isWrapperFor(OracleCommonDataSource.class));

        //inferred ds does not specify a URL or type. This should result in inferring a datasource class name
        //Expect it to be ConnectionPoolDataSource since that is available in the Oracle driver and it's the first type we search for
        assertTrue("inferred-ds should wrap OracleConnectionPoolDataSource datasource since it does not have a URL property",
                   inferred_ds.isWrapperFor(OracleConnectionPoolDataSource.class));

        //try to use the inferred_ds to ensure it is usable
        Connection conn = inferred_ds.getConnection();
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
        tx.begin();
        try {
            try (Connection con1 = unsharable_ds_xa_loosely_coupled.getConnection()) {
                con1.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (31, 'thirty-one')");

                // Obtain a second (unshared) connection so that we have 2 transaction branches
                try (Connection con2 = unsharable_ds_xa_loosely_coupled.getConnection()) {
                    ResultSet result = con2.createStatement().executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID=31");
                    assertFalse(result.next());
                }
            }
        } finally {
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
            try (Connection con1 = unsharable_ds_xa_tightly_coupled.getConnection()) {
                con1.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (32, 'thirty-two')");

                // Obtain a second (unshared) connection so that we have 2 transaction branches
                try (Connection con2 = unsharable_ds_xa_tightly_coupled.getConnection()) {
                    ResultSet result = con2.createStatement().executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID=32");
                    assertTrue(result.next());
                    assertEquals("thirty-two", result.getString(1));
                }
            }
        } finally {
            tx.commit();
        }
    }

    @Test
    public void testBlobCreation() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("/data/myDataFile.txt");) {

            //First try to use setBlob
            try (Connection con1 = ds.getConnection();
                            PreparedStatement ps = con1.prepareStatement("INSERT INTO BLOBTABLE VALUES (?, ?)");) {

                byte[] byteData = new byte[inputStream.available()];
                inputStream.read(byteData);

                Blob blob = con1.createBlob();
                blob.setBytes(1, byteData);

                ps.setInt(1, 1);
                ps.setBlob(2, blob);
                ps.executeUpdate();

                blob.free();
            }

            //Next try to use setBinaryStream
            try (Connection con1 = ds.getConnection();
                            PreparedStatement ps = con1.prepareStatement("INSERT INTO BLOBTABLE VALUES (?, ?)");) {

                ps.setInt(1, 2);
                ps.setBinaryStream(2, inputStream);
                ps.executeUpdate();
            }
        }
    }

    @Test
    public void testVerifyConnectionPrecedence() throws Throwable {
        try (Connection con = driver_url_perferred.getConnection(); PreparedStatement stmt = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");) {
            stmt.setInt(1, 33);
            stmt.setString(2, "thirty-three");
            stmt.execute();
        }

        try (Connection con = ds_url_perferred.getConnection(); PreparedStatement stmt = con.prepareStatement("INSERT INTO MYTABLE VALUES (?, ?)");) {
            stmt.setInt(1, 34);
            stmt.setString(2, "thirty-four");
            stmt.execute();
        }
    }
}
