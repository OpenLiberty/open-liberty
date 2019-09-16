/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package db2.web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.servlet.annotation.WebServlet;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.db2.jcc.DB2JccDataSource;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/DB2TestServlet")
public class DB2TestServlet extends FATServlet {
    @Resource(shareable = false)
    private DataSource ds;

    @Resource(lookup = "jdbc/db2", authenticationType = Resource.AuthenticationType.APPLICATION)
    private DataSource ds_db2;

    @Resource(lookup = "jdbc/db2-inferred")
    private DataSource db2_inferred_ds;

    @Resource(lookup = "jdbc/db2-using-driver")
    private DataSource db2_using_driver;

    @Resource(lookup = "jdbc/db2-using-driver-type")
    private DataSource db2_using_driver_type;

    @Resource
    private UserTransaction tran;

    // One-time initialization of database before tests run
    public void initDatabase() throws SQLException {
        Connection con = ds.getConnection();
        try {
            // Create tables
            Statement stmt = con.createStatement();
            try {
                stmt.execute("DROP TABLE MYTABLE");
            } catch (SQLException x) {
                if (!"42704".equals(x.getSQLState()))
                    throw x;
            }
            stmt.execute("CREATE TABLE MYTABLE (ID SMALLINT NOT NULL PRIMARY KEY, STRVAL NVARCHAR(40))");
            stmt.close();
        } finally {
            con.close();
        }
    }

    // Use an XADataSource with downgradeHoldCursorsUnderXa=true configured. Verify that HOLD_CURSORS_OVER_COMMIT can
    // be set on the connection, but that it is downgraded such that a result set is not usable after xa.commit,
    // but the statement remains usable.
    @ExpectedFFDC("com.ibm.db2.jcc.am.SqlException")
    @Test
    public void testDowngradeHoldCursorsUnderXa() throws Exception {
        Connection con = ds.getConnection();
        try {
            con.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT); // requires downgradeHoldCursorsUnderXa=true
            con.setAutoCommit(false);
            Statement stmt = con.createStatement();
            assertEquals(1, stmt.executeUpdate("INSERT INTO MYTABLE VALUES (2, 'second')"));
            con.commit();

            ResultSet result;
            tran.begin();
            try {
                // ensure two-phase commit
                Connection c2 = ds_db2.getConnection();
                c2.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (3, 'third')");
                c2.close();

                result = stmt.executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID=2");
                assertTrue(result.next());
            } finally {
                tran.commit();
            }

            try {
                result.getString(1);
                fail("Cursor holdability should have been downgraded to close cursor at xa.commit");
            } catch (SQLException x) {
                if (x.getErrorCode() != -4470)
                    throw x;
            }

            // statement should continue to be usable, even though its result set was closed at commit
            assertTrue(stmt.executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID=3").next());
        } finally {
            con.setAutoCommit(true);
            con.close();
        }
    }

    // Verify that properties which represent durations of time are set properly on the data source.
    @Test
    public void testDurationProperties() throws Exception {
        Referenceable referenceable = ds_db2.unwrap(javax.naming.Referenceable.class);
        Reference ref = referenceable.getReference();

        RefAddr affinityFailbackInterval = ref.get("affinityFailbackInterval");
        assertEquals("4830", affinityFailbackInterval.getContent().toString()); // 1h20m30s

        RefAddr commandTimeout = ref.get("commandTimeout");
        assertEquals("150", commandTimeout.getContent().toString()); // 2m30s

        RefAddr connectionTimeout = ref.get("connectionTimeout");
        assertEquals("90", connectionTimeout.getContent().toString()); // 90s

        RefAddr memberConnectTimeout = ref.get("memberConnectTimeout");
        assertEquals("100", memberConnectTimeout.getContent().toString()); // 100000ms
    }

    // When allowNullResultSetForExecuteQuery is configured to 1 (YES), DB2 executeQuery can return a NULL.
    // Verify that WAS JDBC statement wrappers can properly handle the NULL result set.
    @Test
    public void testNullResultOfExecuteQuery() throws Exception {
        Connection con = ds_db2.getConnection();
        try {
            con.createStatement()
                            .execute("CREATE OR REPLACE PROCEDURE insertNewEntry (IN NEWID SMALLINT, IN NEWVAL NVARCHAR(40))"
                                     + " BEGIN"
                                     + " INSERT INTO MYTABLE VALUES(NEWID, NEWVAL);"
                                     + " END");

            CallableStatement cs = con.prepareCall("CALL insertNewEntry(?, ?)");
            cs.setInt(1, 1);
            cs.setString(2, "first");
            ResultSet result = cs.executeQuery();
            assertNull(result);
            cs.close();
        } finally {
            con.close();
        }
    }

    //Test that a datasource backed by Driver can be used with both the generic properties element and properties.db2.jcc
    //element when type="java.sql.Driver"
    @Test
    public void testDSUsingDriver() throws Exception {
        Connection conn = db2_using_driver_type.getConnection();
        assertFalse("db2_using_driver_type should not wrap DB2JccDataSource", db2_using_driver_type.isWrapperFor(DB2JccDataSource.class));

        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");
            ps.setInt(1, 20);
            ps.setString(2, "twenty");
            ps.executeUpdate();
            ps.close();
        } finally {
            conn.close();
        }

        assertFalse("db2_using_driver should not wrap DB2JccDataSource", db2_using_driver.isWrapperFor(DB2JccDataSource.class));
        Connection conn2 = db2_using_driver.getConnection();
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
    public void testInferDB2DataSource() throws Exception {
        //The default datasource should continue to be inferred as an XADataSource, since it has properties.db2.jcc configured
        assertTrue("default datasource should wrap XADataSource", ds.isWrapperFor(XADataSource.class));

        //ds_db2 should continue to be inferred as a ConnectionPoolDataSource, since it has properties.db2.jcc configured
        assertTrue("ds_db2 should wrap ConnectionPoolDataSource", ds_db2.isWrapperFor(ConnectionPoolDataSource.class));

        //db2_using_driver doesn't specify a type.  The presence of URL will result in the DataSource being back by Driver
        assertFalse("The presence of the URL should result in db2_using_driver being back by Driver", db2_using_driver.isWrapperFor(DB2JccDataSource.class));

        //inferred ds does not specify a URL or type. This should result in inferring a datasource class name
        //Expect it to be ConnectionPoolDataSource since that is available in the DB2 driver and it's the first type we search for
        assertTrue("db2_inferred_ds should wrap ConnectionPoolDataSource datasource since it does not have a URL property",
                   db2_inferred_ds.isWrapperFor(ConnectionPoolDataSource.class));

        //try to use the inferred_ds to ensure it is usable
        Connection conn = db2_inferred_ds.getConnection();
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
}
