/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.v41.web;

import static com.ibm.websphere.simplicity.config.DataSourceProperties.DERBY_EMBEDDED;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.INFORMIX_JDBC;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.MICROSOFT_SQLSERVER;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.SYBASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.simplicity.config.dsprops.testrules.OnlyIfDataSourceProperties;
import com.ibm.websphere.simplicity.config.dsprops.testrules.SkipIfDataSourceProperties;
import com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATDatabaseServlet;

@WebServlet(urlPatterns = "/BasicTestServlet")
public class BasicTestServlet extends FATDatabaseServlet {
    private static final long serialVersionUID = -4499882586720934635L;
    private static final String MBEAN_TYPE = "com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean";
    private static final String colorTable = "JDBC_FAT_v41_COLORS";
    private static final String userTable = "JDBC_FAT_v41_USERS";
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);
    private List<String> globalSchemaList = null;
    private boolean isGetColorRegistered = false;
    private boolean isGetUserRegistered = false;

    @Resource
    private UserTransaction tran;

    @Resource(name = "jdbc/ds1", shareable = false, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds1;

    @Resource(name = "jdbc/ds2", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds2;

    @Resource(name = "jdbc/ds3", shareable = false, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds3;

    @Resource(name = "jdbc/XAds")
    DataSource xads;

    @Override
    public void init() throws ServletException {
        createTable(ds1, colorTable, "id int not null primary key, color varchar(30)");
    }

    @Test
    public void testServletWorking(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.getWriter().println("Got to test servlet method.");
        System.out.println("Got to test servlet method");
        Connection conn = ds1.getConnection();
        try {
            DatabaseMetaData md = conn.getMetaData();
            System.out.println("Using driver: " + md.getDatabaseProductName() + ' ' + md.getDatabaseProductVersion());
            System.out.println("Driver is JDBC version: " + md.getJDBCMajorVersion() + '.' + md.getJDBCMinorVersion());
            System.out.println("other: " + md.getDriverMajorVersion() + "." + md.getDriverMinorVersion());
        } finally {
            conn.close();
        }
    }

    /**
     * Verify that all of the new JDBC 4.1 features throw a SQLFeatureNotSupportedException.
     * NOTE: This test method should ONLY get run by a jdbc-4.0 or lower server, since we are
     * making sure that the proper exceptions are in place for tolerating JDBC 4.1 features.
     */
    public void testJDBC41Tolerance(HttpServletRequest request, HttpServletResponse response) throws Exception {
        /*********** DataSource ************/
        try {
            ds1.getParentLogger();
            throw new Exception("Currently we do not support JDBC41 spec enhancements");
        } catch (SQLFeatureNotSupportedException ex1) {
        }

        /*********** Connection ************/
        Connection con = ds1.getConnection();
        try {
            try {
                con.setSchema("");
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex2) {
            }

            try {
                con.getSchema();
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex2) {
            }

            try {
                con.abort((Executor) null);
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex2) {
            }

            try {
                con.setNetworkTimeout((Executor) null, 0);
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex2) {
            }

            try {
                con.getNetworkTimeout();
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex2) {
            }

            /*********** DatabaseMetaData ************/
            DatabaseMetaData dbmd = con.getMetaData();
            try {
                dbmd.getPseudoColumns("", "", "", "");
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex) {
            }

            try {
                dbmd.generatedKeyAlwaysReturned();
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex) {
            }

            /*********** Statement ************/
            Statement st = con.createStatement();
            try {
                st.closeOnCompletion();
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex2) {
            }

            try {
                st.isCloseOnCompletion();
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex2) {
            }

            /*********** ResultSet ************/
            ResultSet rs = st.executeQuery("select * from " + colorTable);
            try {
                try {
                    rs.getObject(0, (Class<String>) null);
                    throw new Exception("Currently we do not support JDBC41 spec enhancements");
                } catch (SQLFeatureNotSupportedException ex2) {
                }

                try {
                    rs.getObject("", (Class<String>) null);
                    throw new Exception("Currently we do not support JDBC41 spec enhancements");
                } catch (SQLFeatureNotSupportedException ex2) {
                }
            } finally {
                rs.close();
                st.close();
            }

            /*********** CallableStatement ************/
            registerGetColors();
            CallableStatement cstmt = con.prepareCall("{call GET_COLORS(?, ?)}");
            try {
                try {
                    cstmt.getObject(0, (Class<String>) null);
                    throw new Exception("Currently we do not support JDBC41 spec enhancements");
                } catch (SQLFeatureNotSupportedException ex2) {
                }

                try {
                    cstmt.getObject("", (Class<String>) null);
                    throw new Exception("Currently we do not support JDBC41 spec enhancements");
                } catch (SQLFeatureNotSupportedException ex2) {
                }
            } finally {
                cstmt.close();
            }
        } finally {
            con.close();
        }
    }

    /**
     * Verify that all of the new JDBC 4.1 features throw a SQLFeatureNotSupportedException
     * EXCEPT for Statement.closeOnCompletion() and Statement.isCloseOnCompletion, since these
     * are implemented by us, not the driver. This test should therefore be run with a jdbc-4.0
     * compliant driver.
     */
    public void testJDBC41Tolerance40Driver(HttpServletRequest request, HttpServletResponse response) throws Exception {
        /*********** DataSource ************/
        try {
            ds1.getParentLogger();
            throw new Exception("Currently we do not support JDBC41 spec enhancements");
        } catch (SQLFeatureNotSupportedException ex1) {
        }

        /*********** Connection ************/
        Connection con = ds1.getConnection();
        try {
            try {
                con.setSchema("");
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex2) {
            }

            try {
                con.getSchema();
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex2) {
            }

            try {
                con.abort((Executor) null);
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex2) {
            }

            try {
                con.setNetworkTimeout((Executor) null, 0);
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex2) {
            }

            try {
                con.getNetworkTimeout();
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex2) {
            }

            /*********** DatabaseMetaData ************/
            DatabaseMetaData dbmd = con.getMetaData();
            try {
                dbmd.getPseudoColumns("", "", "", "");
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex) {
            }

            try {
                dbmd.generatedKeyAlwaysReturned();
                throw new Exception("Currently we do not support JDBC41 spec enhancements");
            } catch (SQLFeatureNotSupportedException ex) {
            }

            /*********** ResultSet ************/
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("select * from " + colorTable);
            try {
                try {
                    rs.getObject(0, (Class<String>) null);
                    throw new Exception("Currently we do not support JDBC41 spec enhancements");
                } catch (SQLFeatureNotSupportedException ex2) {
                }

                try {
                    rs.getObject("", (Class<String>) null);
                    throw new Exception("Currently we do not support JDBC41 spec enhancements");
                } catch (SQLFeatureNotSupportedException ex2) {
                }
            } finally {
                rs.close();
                st.close();
            }

            /*********** CallableStatement ************/
            registerGetColors();
            CallableStatement cstmt = con.prepareCall("{call GET_COLORS(?, ?)}");
            try {
                try {
                    cstmt.getObject(0, (Class<String>) null);
                    throw new Exception("Currently we do not support JDBC41 spec enhancements");
                } catch (SQLFeatureNotSupportedException ex2) {
                }

                try {
                    cstmt.getObject("", (Class<String>) null);
                    throw new Exception("Currently we do not support JDBC41 spec enhancements");
                } catch (SQLFeatureNotSupportedException ex2) {
                }
            } finally {
                cstmt.close();
            }
        } finally {
            con.close();
        }
    }

    /**
     * Verify the following configuration scenario:
     * <br> - Server.xml is configured with {@literal <jdbc-4.0>} <br> - JDBC driver is greater than JDBC 4.0 compliant. In this case 4.1 compliant.
     * We should be intercepting the call to DatabaseMetaData.getJDBCMajor/MinorVersion and reduce to the feature version
     * if needed. In this case, the version returned should be 4.0.
     * <br>NOTE: This test runs as part of the JDBC41 tolerance test suite, which is jdbc-4.0. NOT jdbc-4.1.
     */
    @SkipIfDataSourceProperties(INFORMIX_JDBC)
    public void testJDBCVersionLimiting(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String expectedVersion = request.getParameter("expectedVersion");
        Connection conn = ds1.getConnection();
        try {
            DatabaseMetaData md = conn.getMetaData();
            String actualVersion = md.getJDBCMajorVersion() + "." + md.getJDBCMinorVersion();
            System.out.println("Got JDBC version: " + actualVersion);
            if (!expectedVersion.equalsIgnoreCase(actualVersion))
                throw new Exception("Expected driver to be JDBC version " + expectedVersion + " but instead it was: " + actualVersion);
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    /**
     * Test behavior of Statement.closeOnCompletion() and Statement.isCloseOnCompletioin()
     * <li> Verify calling closeOnCompletion() multiple times does not toggle the effect on the statement.
     * <li> Verify statement is closed when dependent ResultSets are closed (both current and subsequent ResultSets)
     * <li> Verify an SQLE is thrown when closeOnCompletion() called on an already closed Statement.
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLRecoverableException" })
    public void testCloseOnCompletionSingle() throws Exception {
        Connection conn = ds1.getConnection();
        try {
            Statement stmt1 = conn.createStatement();
            stmt1.execute("select * from " + colorTable);
            ResultSet rs1 = stmt1.getResultSet();

            stmt1.closeOnCompletion();
            // Multiple calls should not toggle the value of closeOnCompletion
            stmt1.closeOnCompletion();

            if (!stmt1.isCloseOnCompletion())
                throw new Exception("Statement should be marked closeOnCompletion");

            rs1.close();
            // now stmt should be closed because dependent result sets have been closed
            if (!stmt1.isClosed())
                throw new Exception("Statement should be closed but it wasn't");

            try {
                stmt1.closeOnCompletion();
                throw new Exception("Calling closeOnCompletion on a closed Statement should throw a SQLE.");
            } catch (SQLException sqle) {
                System.out.println("Caught expected SQLE.");
            }
        } finally {
            conn.close();
        }
    }

    /**
     * Verify behavior of Statement.closeOnCompletion() when there are multiple dependent ResultSets.
     * This test uses Statement.KEEP_CURRENT_RESULT to keep result sets open.
     */
    @Test
    @OnlyIfDataSourceProperties(DERBY_EMBEDDED)
    public void testCloseOnCompletionMultiple1() throws Exception {
        Connection conn = ds1.getConnection();
        try {
            // Register stored procedure and insert some test data
            registerGetColors();
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("insert into " + colorTable + " (id, color) values (1, 'blue')");
            stmt.executeUpdate("insert into " + colorTable + " (id, color) values (2, 'red')");
            stmt.close();

            // Call procedure, which will return 2 result sets
            CallableStatement cstmt = conn.prepareCall("{call GET_COLORS(1, 2)}");

            // Get first result set, verify color is 'blue'
            if (!cstmt.execute())
                throw new Exception("Should be an available result set.");
            ResultSet rs1 = cstmt.getResultSet();
            rs1.next();
            String color1 = rs1.getString("color");
            if (!"blue".equals(color1))
                throw new Exception("Expected result set with 'blue', but instead it was " + color1);

            // Enable closeOnCompletion for the ResultSet
            cstmt.closeOnCompletion();

            // Get the second result set, leave rs1 open, and verify color of rs2 is 'red'
            if (!cstmt.getMoreResults(Statement.KEEP_CURRENT_RESULT))
                throw new Exception("Should be a second available result set.");
            ResultSet rs2 = cstmt.getResultSet();
            rs2.next();
            String color2 = rs2.getString("color");
            if (!"red".equals(color2))
                throw new Exception("Expected result set with 'red', but instead it was " + color2);

            // There should not be a third result set
            if (cstmt.getMoreResults(Statement.KEEP_CURRENT_RESULT))
                throw new Exception("Should not have been 3 result sets returned, only 2.");

            if (cstmt.isClosed())
                throw new Exception("Statement should NOT be closed yet.");

            // Verify Statement is closed once dependent ResultSets are closed
            rs1.close();
            if (cstmt.isClosed())
                throw new Exception("Statement should NOT be closed since rs2 is still open.");
            rs2.close();
            if (!cstmt.isClosed())
                throw new Exception("Statement should be closed since the dependent ResultSets were closed.");
        } finally {
            conn.close();
        }
    }

    /**
     * Verify behavior of Statement.closeOnCompletion() when there are multiple dependent ResultSets.
     * This test uses Statement.CLOSE_CURRENT_RESULT
     */
    @Test
    @OnlyIfDataSourceProperties(DERBY_EMBEDDED)
    public void testCloseOnCompletionMultiple2() throws Exception {
        Connection conn = ds1.getConnection();
        try {
            // Register stored procedure and insert some test data
            registerGetColors();
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("insert into " + colorTable + " (id, color) values (3, 'green')");
            stmt.executeUpdate("insert into " + colorTable + " (id, color) values (4, 'yellow')");
            stmt.close();

            // Call procedure, which will return 2 result sets
            CallableStatement cstmt = conn.prepareCall("{call GET_COLORS(3, 4)}");

            // Get first result set, verify color is 'blue'
            if (!cstmt.execute())
                throw new Exception("Should be an available result set.");
            ResultSet rs1 = cstmt.getResultSet();
            rs1.next();
            String color1 = rs1.getString("color");
            if (!"green".equals(color1))
                throw new Exception("Expected result set with 'green', but instead it was " + color1);

            // Enable closeOnCompletion for the ResultSet
            cstmt.closeOnCompletion();

            // Get the second result set, leave rs1 open, and verify color of rs2 is 'red'
            if (!cstmt.getMoreResults(Statement.CLOSE_CURRENT_RESULT))
                throw new Exception("Should be a second available result set.");
            ResultSet rs2 = cstmt.getResultSet();
            rs2.next();
            String color2 = rs2.getString("color");
            if (!"yellow".equals(color2))
                throw new Exception("Expected result set with 'yellow', but instead it was " + color2);

            // There should not be a third result set
            if (cstmt.getMoreResults(Statement.KEEP_CURRENT_RESULT))
                throw new Exception("Should not have been 3 result sets returned, only 2.");

            // Verify Statement is closed once dependent ResultSets are closed
            rs1.close();
            if (cstmt.isClosed())
                throw new Exception("Statement should NOT be closed since rs2 is still open.");
            rs2.close();
            if (!cstmt.isClosed())
                throw new Exception("Statement should be closed since the dependent ResultSets were closed.");
        } finally {
            conn.close();
        }
    }

    /**
     * Verify basic behavior of set and get schema.
     * Loop through all of the possible schemas available to the connection and verify they are set.
     * NOTE: If there are no schemas on the database, this test will be skipped.
     */
    @Test
    @AllowedFFDC({ "java.sql.SQLSyntaxErrorException", "java.sql.SQLException" })
    @SkipIfDataSourceProperties({ SYBASE, MICROSOFT_SQLSERVER, INFORMIX_JDBC }) //SQLServer doesn't support setSchema()
    public void testSchemaSimple() throws Exception {

        Connection conn = ds1.getConnection();
        try {
            List<String> schemaList = getSchemas(conn);

            if (schemaList.size() == 0) {
                System.out.println("No schemas defined for datasource.  Skipping test.");
                return;
            }

            final String originalSchema = conn.getSchema();
            System.out.println("Original schema is: " + originalSchema);

            // Verify each schema can be get and set
            for (String curSchema : schemaList) {
                System.out.println("Changing schema to " + curSchema);
                conn.setSchema(curSchema);
                String actualSchema = conn.getSchema().trim();
                if (!curSchema.trim().equalsIgnoreCase(actualSchema))
                    throw new Exception("Expected schema " + curSchema + " but instead got " + actualSchema);
            }

            // Try to set a nonexistant schema
            try {
                conn.setSchema("THIS_SCHEMA_DOESNT_EXIST");
            } catch (SQLException sqle) {
            }

            // Restore original schema on conneciton
            conn.setSchema(originalSchema);

            // Verify exceptions when set/getSchema is called on a closed connection
            conn.close();
            try {
                conn.getSchema();
                throw new Exception("Should not be able to call getSchema() on a closed connection.");
            } catch (SQLException e) {
            }
            try {
                conn.setSchema(originalSchema);
                throw new Exception("Should not be able to call setSchema() on a closed connection.");
            } catch (SQLException e) {
            }
        } finally {
            if (conn != null && !conn.isClosed())
                conn.close();
        }
    }

    /**
     * Verify connection schema behavior when the MC's are shared.
     * Get conn1 with schema A, set conn1 schema to B, then close conn1.
     * Get conn2 and verify that it has schema A (proving that conn1 cleaned up its schema change properly).
     * NOTE: If there are less than 2 schemas available to the connection, the test will be skipped.
     */
    @Test
    @SkipIfDataSourceProperties({ SYBASE, MICROSOFT_SQLSERVER, INFORMIX_JDBC }) //SQLServer doesn't support setSchema()
    public void testSchemaSharing() throws Exception {
        String connID1, connID2;
        String originalSchema, differentSchema;
        List<String> schemaList;

        // Get a connection with schema A, set schema B on the connection, then close connection
        Connection conn1 = ds2.getConnection();
        try {
            connID1 = getManagedConnectionID(conn1);

            schemaList = getSchemas(conn1);

            if (schemaList.size() < 2) {
                System.out.println("Must be at least 2 schemas for this test to be valid. Skipping test.");
                return;
            }

            originalSchema = conn1.getSchema();
            System.out.println("Original schema is: " + originalSchema);

            differentSchema = (originalSchema.equalsIgnoreCase(schemaList.get(0))) ? schemaList.get(1) : schemaList.get(0);

            System.out.println("Setting schema to: " + differentSchema);
            conn1.setSchema(differentSchema);
        } finally {
            conn1.close();
        }

        // Get another connection (which should be the same MC as before)
        // This new connection should have schema A on it (proving that schema B from conn1 got reverted)
        Connection conn2 = ds2.getConnection();
        try {
            connID2 = getManagedConnectionID(conn2);

            // A new connection should have the original schema
            String conn2Schema = conn2.getSchema();
            System.out.println("Conn2 schema is: " + conn2Schema);

            if (!conn2Schema.equals(originalSchema))
                throw new Exception("Expected original schema (" + originalSchema + ") but instead got: " + conn2Schema);
        } finally {
            conn2.close();
        }

        // Verify that the connections were shared (same MC id)
        if (connID1.compareTo(connID2) != 0)
            throw new Exception("Connections should be shared but were not: " + connID1 + " : " + connID2);
    }

    /**
     * Verify connection schema behavior when two connections are open simultaneously.
     * Setting a schema on conn1 should not effect the schema on conn2.
     * NOTE: Must be at least 2 schemas available to connection for this test to be valid.
     */
    @Test
    @SkipIfDataSourceProperties({ SYBASE, MICROSOFT_SQLSERVER, INFORMIX_JDBC }) //SQLServer doesn't support setSchema()
    public void testSchemaDouble() throws Exception {
        Connection conn1 = ds1.getConnection(), conn2 = null;
        String originalSchema;
        List<String> schemaList;
        try {
            originalSchema = conn1.getSchema();

            schemaList = getSchemas(conn1);
            if (schemaList.size() < 2) {
                System.out.println("Must be at least 2 schemas on connection for this test to be valid.  Skipping test.");
                return;
            }

            String differentSchema = (originalSchema.equalsIgnoreCase(schemaList.get(0))) ? schemaList.get(1) : schemaList.get(0);
            System.out.println("Setting schema to: " + differentSchema);
            conn1.setSchema(differentSchema);

            conn2 = ds1.getConnection();
            String conn2Schema = conn2.getSchema();
            if (!conn2Schema.equals(originalSchema))
                throw new Exception("Expected conn2 to have schema:" + originalSchema + " but instead it had schema:" + conn2Schema);
        } finally {
            conn1.close();
            if (conn2 != null)
                conn2.close();
        }
    }

    /**
     * Verify that PreparedStatement caching accounts for the schema which the statements were created with.
     * PreparedStatements created under the same schema should have the opportunity to be reused.
     * PreparedStatements created with different schemas will never be shared.
     */
    @Test
    @SkipIfDataSourceProperties({ SYBASE, MICROSOFT_SQLSERVER, INFORMIX_JDBC }) //SQLServer doesn't support setSchema()
    public void testSchemaPStmtCaching() throws Exception {

        Connection conn = ds1.getConnection();
        try {
            // Change to non-default schema and create new table with that schema
            List<String> schemaList = getSchemas(conn);
            if (schemaList.size() < 2) {
                System.out.println("Need at least 2 schemas for test to be valid.  Skipping test.");
                return;
            }
            String originalSchema = conn.getSchema();
            System.out.println("Original schema is: " + originalSchema);
            String differentSchema = (originalSchema.equalsIgnoreCase(schemaList.get(0))) ? schemaList.get(1) : schemaList.get(0);
            conn.setSchema(differentSchema);
            System.out.println("Set schema to " + differentSchema);

            String sql = "values (current_date)";

            // Get a PreparedStatement, locate the cache key, then close the statement.
            PreparedStatement pstmt1 = conn.prepareStatement(sql);
            Object key1 = getCacheKey(pstmt1);
            pstmt1.getUpdateCount(); // Need to process all results in order for statement to be cached.
            pstmt1.close();
            System.out.println("key1=" + key1);

            // Get another PreparedStatement with the same sql.
            // Expect statement to be cached since the schema is still the same.
            PreparedStatement pstmt2 = conn.prepareStatement(sql);
            Object key2 = getCacheKey(pstmt2);
            pstmt2.getUpdateCount(); // Need to process all results in order for statement to be cached.
            pstmt2.close();
            System.out.println("key2=" + key2);

            // Verify that cache keys are the same
            if (key1 == null || key2 == null || !key1.equals(key2))
                throw new Exception("Statement keys did not match.  pstmt1: " + key1 + "   pstmt2: " + key2);

            // Do a schema change, and retrieve another PreparedStatement and check the cache key
            conn.setSchema(originalSchema);
            PreparedStatement pstmt3 = conn.prepareStatement(sql);
            Object key3 = getCacheKey(pstmt3);
            pstmt3.getUpdateCount(); // Need to process all results in order for statement to be cached.
            pstmt3.close();
            System.out.println("key3=" + key3);

            // Verify that pstmt3 cache key is different than the first 2
            if (key3 == null || key3.equals(key1))
                throw new Exception("Statement was cached but it should not have been cached.  Key3=" + key3 + " Key1=" + key1);
        } finally {
            conn.close();
        }
    }

    /**
     * Verify that a cached statement retrieved after a schema swap is still usable.
     */
    @Test
    @SkipIfDataSourceProperties({ SYBASE, MICROSOFT_SQLSERVER, INFORMIX_JDBC }) //SQLServer doesn't support setSchema()
    public void testSchemaPStmtCaching2() throws Exception {
        final String sql = "insert into " + colorTable + " (id, color) values (?, ?)";

        Connection conn = ds1.getConnection();
        try {
            // Create and use a PreparedStatement with original schema
            List<String> schemaList = getSchemas(conn);
            if (schemaList.size() < 2) {
                System.out.println("Need at least 2 schemas for test to be valid.  Skipping test.");
                return;
            }
            String originalSchema = conn.getSchema();
            PreparedStatement pstmt1 = conn.prepareStatement(sql);
            pstmt1.setInt(1, 5);
            pstmt1.setString(2, "black");
            pstmt1.execute();
            Object key1 = getCacheKey(pstmt1);
            pstmt1.getUpdateCount(); // Need to process all results in order for statement to be cached.
            pstmt1.close();
            System.out.println("key1=" + key1);

            // Change the schema to something different, then change the schema back
            String differentSchema = (originalSchema.equalsIgnoreCase(schemaList.get(0))) ? schemaList.get(1) : schemaList.get(0);
            conn.setSchema(differentSchema);
            System.out.println("Set schema to " + differentSchema);
            conn.setSchema(originalSchema);
            System.out.println("Set schema to " + originalSchema);

            // Get a PreparedStatement with the same SQL as before, expect to retrieve the previously cached PStmt
            PreparedStatement pstmt2 = conn.prepareStatement(sql);
            pstmt2.setInt(1, 6);
            pstmt2.setString(2, "white");
            pstmt2.execute();
            Object key2 = getCacheKey(pstmt2);
            pstmt2.getUpdateCount(); // Need to process all results in order for statement to be cached.
            pstmt2.close();
            System.out.println("key2=" + key2);

            // Verify that cache keys are the same
            if (key1 == null || key2 == null || !key1.equals(key2))
                throw new Exception("Statement keys did not match.  pstmt1: " + key1 + "   pstmt2: " + key2);
        } finally {
            conn.close();
        }
    }

    /**
     * Verify that CallableStatement caching accounts for the schema which the statements were created with.
     * CallableStatements created under the same schema should have the opportunity to be reused.
     * CallableStatements created with different schemas will never be shared.
     */
    @Test
    @SkipIfDataSourceProperties({ SYBASE, MICROSOFT_SQLSERVER, INFORMIX_JDBC }) //SQLServer doesn't support setSchema()
    public void testSchemaCStmtCaching() throws Exception {

        Connection conn = ds1.getConnection();
        try {
            // Change to non-default schema and create new table with that schema
            List<String> schemaList = getSchemas(conn);
            if (schemaList.size() < 2) {
                System.out.println("Need at least 2 schemas for test to be valid.  Skipping test.");
                return;
            }
            String originalSchema = conn.getSchema();
            System.out.println("Original schema is: " + originalSchema);
            String differentSchema = (originalSchema.equalsIgnoreCase(schemaList.get(0))) ? schemaList.get(1) : schemaList.get(0);
            conn.setSchema(differentSchema);
            System.out.println("Set schema to " + differentSchema);

            String sql = "values (current_date)";

            // Get a CallableStatement, locate the cache key, then close the statement.
            CallableStatement cstmt1 = conn.prepareCall(sql);
            Object key1 = getCacheKey(cstmt1);
            cstmt1.getUpdateCount(); // Need to process all results in order for statement to be cached.
            cstmt1.close();
            System.out.println("key1=" + key1);

            // Get another CallableStatement with the same sql.
            // Expect statement to be cached since the schema is still the same.
            CallableStatement cstmt2 = conn.prepareCall(sql);
            Object key2 = getCacheKey(cstmt2);
            cstmt2.getUpdateCount(); // Need to process all results in order for statement to be cached.
            cstmt2.close();
            System.out.println("key2=" + key2);

            // Verify that cache keys are the same
            if (key1 == null || key2 == null || !key1.equals(key2))
                throw new Exception("Statement keys did not match.  pstmt1: " + key1 + "   pstmt2: " + key2);

            // Do a schema change, and retrieve another CallableStatement and check the cache key
            conn.setSchema(originalSchema);
            CallableStatement cstmt3 = conn.prepareCall(sql);
            Object key3 = getCacheKey(cstmt3);
            cstmt3.close();
            System.out.println("key3=" + key3);

            // Verify that 3rd cache key is different than the first 2
            if (key3 == null || key3.equals(key1))
                throw new Exception("Statement was cached but it should not have been cached.  Key3=" + key3 + " Key1=" + key1);
        } finally {
            conn.close();
        }
    }

    /**
     * Verify that a cached statement retrieved after a schema swap is still usable.
     */
    @Test
    @SkipIfDataSourceProperties({ SYBASE, MICROSOFT_SQLSERVER, INFORMIX_JDBC }) //SQLServer doesn't support setSchema()
    public void testSchemaCStmtCaching2() throws Exception {
        final String sql = "insert into " + colorTable + " (id, color) values (?, ?)";

        Connection conn = ds1.getConnection();
        try {
            // Create and use a CallableStatement with original schema
            List<String> schemaList = getSchemas(conn);
            if (schemaList.size() < 2) {
                System.out.println("Need at least 2 schemas for test to be valid.  Skipping test.");
                return;
            }
            String originalSchema = conn.getSchema();
            CallableStatement cstmt1 = conn.prepareCall(sql);
            Object key1 = getCacheKey(cstmt1);
            cstmt1.getUpdateCount(); // Need to process all results in order for statement to be cached.
            cstmt1.close();
            System.out.println("key1=" + key1);

            // Change the schema to something different, then change the schema back
            String differentSchema = (originalSchema.equalsIgnoreCase(schemaList.get(0))) ? schemaList.get(1) : schemaList.get(0);
            conn.setSchema(differentSchema);
            System.out.println("Set schema to " + differentSchema);
            conn.setSchema(originalSchema);
            System.out.println("Set schema to " + originalSchema);

            // Get a CallableStatement with the same SQL as before, expect to retrieve the previously cached CStmt
            CallableStatement cstmt2 = conn.prepareCall(sql);
            Object key2 = getCacheKey(cstmt2);
            cstmt2.getUpdateCount(); // Need to process all results in order for statement to be cached.
            cstmt2.close();
            System.out.println("key1=" + key1);
            System.out.println("key2=" + key2);

            // Verify that cache keys are the same
            if (key1 == null || key2 == null || !key1.equals(key2))
                throw new Exception("Statement keys did not match.  cstmt1: " + key1 + "   cstmt2: " + key2);
        } finally {
            conn.close();
        }
    }

    /**
     * Function to get and abort a connection.
     *
     * After running this function, the connection should be immediately removed
     * from the connection pool, and a call to getSingleConnectionAfterAbort
     * should succeed.
     *
     * Uses datasource 2 for shareable connections.
     */
    public void testAbortedConnectionDestroyed() throws Exception {
        Connection c = ds2.getConnection();
        Executor testExecutor = new Executor() {
            @Override
            public void execute(Runnable arg0) {
                arg0.run();
            }
        };
        try {
            c.abort(testExecutor);
        } catch (SQLException e) {
            c.close();
            throw e;
        }
    }

    /**
     * Function to just get and close a connection
     *
     * This function should be called right after testAbortedConnectionDestroyed to
     * ensure that the aborted connection was immediately removed from the pool.
     */
    public void getSingleConnectionAfterAbort() throws Exception {
        int size = getPoolSize("jdbc/ds2");
        if (size != 0)
            throw new Exception("Expected pool to be empty when getSingleConnectionAfterAbort was called, but it wasn't");

        // Aborted connection should have been destroyed, so we should be able to get
        // and close a new connection no problem.
        Connection c = ds2.getConnection();
        c.close();
    }

    @Test
    @SkipIfDataSourceProperties({ SYBASE, INFORMIX_JDBC })
    public void testAbortWithEntirePoolPurge() throws Exception {
        Connection conn1 = null;
        Connection conn2 = null;
        Connection conn3 = null;
        try {
            tran.begin();
            conn1 = ds3.getConnection();
            conn2 = ds3.getConnection();

            int size = getPoolSize("jdbc/ds3");
            if (size != 2)
                throw new Exception("Expected 2 connections in pool, but there were " + size + ".");

            conn1.close();
            conn2.close();
            tran.commit();

            size = getPoolSize("jdbc/ds3");
            if (size != 2)
                throw new Exception("Expected 2 connections still in pool after connections closed, but there were " + size + ".");

            tran.begin();
            conn3 = ds3.getConnection();
            size = getPoolSize("jdbc/ds3");
            if (size != 2)
                throw new Exception("Expected 2 connections still in pool after getting third connection, but there were " + size + ".");

            Executor testExecutor = new Executor() {
                @Override
                public void execute(Runnable arg0) {
                    arg0.run();
                }
            };
            conn3.abort(testExecutor);
            tran.commit();

            size = getPoolSize("jdbc/ds3");
            if (size != 1)
                throw new Exception("Expected 1 connection in pool, but there were " + size + ".");

        } finally {
            if (conn1 != null && !conn1.isClosed())
                conn1.close();
            if (conn2 != null && !conn2.isClosed())
                conn2.close();
            if (conn3 != null && !conn3.isClosed())
                conn3.close();
        }
    }

    /**
     * <pre>
     * Test which inserts and retrieves objects to a table using User Defined Types (UDT).
     * The purpose of this test is to exercise the new JDBC-4.1 methods:
     * &ltT&gt ResultSet.getObject(int columnIndex, Class&ltT&gt type)
     * &ltT&gt ResultSet.getObject(String columnLabel, Class&ltT&gt type)
     * </pre>
     */
    @Test
    @OnlyIfDataSourceProperties(DERBY_EMBEDDED)
    public void testGetObject1() throws Exception {
        // Create some user objects
        MyUser expectedUserA = new MyUser("Alice", "aAddress", "111-1111");
        MyUser expectedUserB = new MyUser("Bob", "bAddress", "222-2222");

        Connection conn = ds1.getConnection();
        try {
            registerMyUser(conn);

            PreparedStatement pstmt = conn.prepareStatement("insert into " + userTable + " values(?,?)");
            // Add user a
            pstmt.setInt(1, 1);
            pstmt.setObject(2, expectedUserA);
            pstmt.addBatch();
            // Add user b
            pstmt.setInt(1, 2);
            pstmt.setObject(2, expectedUserB);
            pstmt.addBatch();
            pstmt.executeBatch(); // Insert the users
            pstmt.close();

            // Now retrieve the users as objects using new getObject methods
            Statement stmt = conn.createStatement();
            stmt.execute("select * from " + userTable + " order by id ASC");
            ResultSet rs = stmt.getResultSet();
            rs.next();
            MyUser actualUserA = rs.getObject(2, MyUser.class);
            rs.next();
            MyUser actualUserB = rs.getObject("userObj", MyUser.class);
            System.out.println("Got userA of " + expectedUserA);
            System.out.println("Got userB of " + expectedUserB);
            rs.close();
            stmt.close();

            // Verify users retrieved from database match the originals
            if (!expectedUserA.equals(actualUserA))
                throw new Exception("Expected user: " + expectedUserA + "  But instead got user: " + actualUserA);
            if (!expectedUserB.equals(actualUserB))
                throw new Exception("Expected user: " + expectedUserB + "  But instead got user: " + actualUserB);
        } finally {
            conn.close();
        }
    }

    /**
     * <pre>
     * Test which inserts and retrieves objects to a table using User Defined Types (UDT).
     * The purpose of this test is to exercise the new JDBC-4.1 methods:
     * &ltT&gt CallableStatement.getObject(int columnIndex, Class&ltT&gt type)
     * &ltT&gt CallableStatement.getObject(String columnLabel, Class&ltT&gt type)
     * </pre>
     */
    @Test
    @OnlyIfDataSourceProperties(DERBY_EMBEDDED)
    public void testGetObject2() throws Exception {
        // Create some user objects
        MyUser expectedUserC = new MyUser("Cathy", "cAddress", "333-3333");
        MyUser expectedUserD = new MyUser("Dave", "dAddress", "444-4444");

        Connection conn = ds1.getConnection();
        try {
            registerMyUser(conn);

            // Insert user object to be retrieved later
            PreparedStatement pstmt = conn.prepareStatement("insert into " + userTable + " values(?,?)");
            // Add user c
            pstmt.setInt(1, 3);
            pstmt.setObject(2, expectedUserC);
            pstmt.addBatch();
            // Add user d
            pstmt.setInt(1, 4);
            pstmt.setObject(2, expectedUserD);
            pstmt.addBatch();
            pstmt.executeBatch(); // Insert the users
            pstmt.close();

            // Now retrieve users C and D using the stored procedure.
            CallableStatement cstmt = conn.prepareCall("{call GET_MYUSER(?, ?)}");
            cstmt.setInt(1, 3);
            cstmt.registerOutParameter(2, Types.JAVA_OBJECT, "jdbc.fat.v41.web.MyUser");
            cstmt.execute();
            MyUser actualUserC = cstmt.getObject(2, MyUser.class);
            System.out.println("Got user C of: " + actualUserC);

            cstmt.setInt(1, 4);
            cstmt.registerOutParameter(2, Types.JAVA_OBJECT, "jdbc.fat.v41.web.MyUser");
            cstmt.execute();
            MyUser actualUserD = cstmt.getObject(2, MyUser.class);
            System.out.println("Got user D of: " + actualUserD);
            cstmt.close();

            // Verify users retrieved from database match the originals
            if (!expectedUserC.equals(actualUserC))
                throw new Exception("Expected user: " + expectedUserC + "  But instead got user: " + actualUserC);
            if (!expectedUserD.equals(actualUserD))
                throw new Exception("Expected user: " + expectedUserD + "  But instead got user: " + actualUserD);
        } finally {
            conn.close();
        }
    }

    /**
     * Simple call to the <code>DatabaseMetaData.getMetaDat()</code> method.
     */
    @Test
    @SkipIfDataSourceProperties({ SYBASE, INFORMIX_JDBC }) // no 4.1 sybase or ifx driver
    public void testGeneratedKeyAlwaysReturned() throws Exception {
        Connection conn = ds1.getConnection();
        try {
            DatabaseMetaData md = conn.getMetaData();
            md.generatedKeyAlwaysReturned();
        } finally {
            conn.close();
        }
    }

    /**
     * Simple call to <code>DatabaseMetaData.getPseudoColumns</code>.
     * Verify contents of the restult set returned by the call (expect 12 columns).
     */
    @Test
    @SkipIfDataSourceProperties({ SYBASE, INFORMIX_JDBC }) // no 4.1 sybase or ifx driver
    public void testGetPseudoColumns() throws Exception {
        Connection conn = ds1.getConnection();
        try {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getPseudoColumns(null, null, colorTable, null);

            // Verify the result set returned by getPseudoColumns
            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();
            System.out.println("There are " + colCount + " attributes of a pseudo column:");
            for (int i = 1; i <= colCount; i++) {
                System.out.println(String.format("%02d  |  %s  |  %s", i, rsmd.getColumnTypeName(i), rsmd.getColumnName(i)));
            }
            if (12 != colCount)
                throw new Exception("Expected 12 attributes of a pseudo column, but instead got " + colCount);
        } finally {
            conn.close();
        }
    }

    // Ensure that we preserve the behavior that DatabaseMetaData.supportsRefCursors always returns false prior to jdbc-4.1 feature
    @Test
    public void testSupportsRefCursors() throws Exception {
        Connection con = xads.getConnection();
        try {
            DatabaseMetaData mdata = con.getMetaData();
            assertEquals(Boolean.FALSE, mdata.getClass().getMethod("supportsRefCursors").invoke(mdata));
        } finally {
            con.close();
        }
    }

    /**
     * When a connection is involved in a transaction which times out, the transaction will call abort()
     * on any XAResource(s). Verify that upon transaction timeout, the connection is aborted.
     */
    @Test
    @SkipIfDataSourceProperties({ SYBASE, INFORMIX_JDBC }) // no 4.1 sybase or ifx driver
    @AllowedFFDC({
                   "javax.resource.ResourceException", // times out before enlistment
                   "javax.transaction.RollbackException", // times out before enlistment
                   "javax.transaction.xa.XAException", "java.lang.NullPointerException", "oracle.jdbc.xa.OracleXAException",
                   "org.apache.derby.shared.common.sanity.AssertFailure" })
    public void testTransactionTimeoutAbort() throws Exception {
        Connection conn = xads.getConnection();
        tran.setTransactionTimeout(3);
        tran.begin();

        try {
            Statement stmt = conn.createStatement();
            stmt.execute("select * from " + colorTable);
            stmt.close();
        } catch (SQLException x) {
            boolean isRollbackException = false;
            for (Throwable cause = x; cause != null && !isRollbackException; cause = cause.getCause())
                isRollbackException = cause instanceof RollbackException;
            if (isRollbackException)
                ; // transaction timed out before we could enlist in it
            else
                throw x;
        }

        System.out.println("Wait up to 2 minutes for transaction to be marked for rollback");
        for (long start = System.nanoTime(); //
                        tran.getStatus() != Status.STATUS_MARKED_ROLLBACK && System.nanoTime() - start < TIMEOUT_NS; //
                        TimeUnit.MILLISECONDS.sleep(200))
            System.out.println("Transaction status: " + tran.getStatus());

        // Connection should now be aborted due to timeout
        System.out.println("Done waiting");

        try {
            tran.commit();
            throw new Exception("Expected transaction to be timed out but it was not.");
        } catch (RollbackException e) {
            // expected
        }

        // Don't need to close connection, it should be aborted by now
        // Now get a new connection to make sure the pool is still usable
        conn = xads.getConnection();
        try {
            tran.begin();
            Statement stmt = conn.createStatement();
            stmt.execute("select * from " + colorTable);
            stmt.close();
            tran.commit();
        } finally {
            conn.close();
        }
    }

    // Ensure that we preserve the behavior that DatabaseMetaData.getMaxLogicalLobSize always returns 0 prior to jdbc-4.2 feature
    @Test
    public void testGetMaxLogicalLobSize() throws Exception {
        Connection con = xads.getConnection();
        try {
            DatabaseMetaData mdata = con.getMetaData();
            assertEquals("Expected default value with JDBC 4.1", (long) 0, mdata.getClass().getMethod("getMaxLogicalLobSize").invoke(mdata));
        } finally {
            con.close();
        }
    }

    @Test
    public void testMBeanShowPoolConents() throws Exception {
        ConnectionManagerMBean cmBean = getConnectionManagerBean("jdbc/ds1");
        String poolContents = cmBean.showPoolContents();
        assertContains(poolContents, "name=WebSphere:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,jndiName=jdbc/ds1,name=dataSource");
        assertContains(poolContents, "jndiName=jdbc/ds1");
        assertContains(poolContents, "maxPoolSize=50");
        assertContains(poolContents, "size=");
        assertContains(poolContents, "waiting=");
        assertContains(poolContents, "unshared=");
        assertContains(poolContents, "shared=");
        assertContains(poolContents, "available=");
    }

    @Test
    public void testMBeanGetSize() throws Exception {
        ConnectionManagerMBean cmBean = getConnectionManagerBean("jdbc/ds1");
        long available = cmBean.getAvailable();
        if (available < 0 || available > 100)
            fail("Expected available connections to be between 0-100 but was: " + available);
    }

    @Test
    public void testMBeanGetMaxSize() throws Exception {
        ConnectionManagerMBean cmBean = getConnectionManagerBean("jdbc/ds1");
        assertEquals(50, cmBean.getMaxSize());
    }

    @Test
    public void testMBeanGetJndiName() throws Exception {
        ConnectionManagerMBean cmBean = getConnectionManagerBean("jdbc/ds1");
        assertEquals("jdbc/ds1", cmBean.getJndiName());
    }

    private static void assertContains(String str, String lookFor) {
        assertTrue("Expected string [" + str + "] to contain the text [" + lookFor + "] but it was not found.",
                   str.contains(lookFor));
    }

    /**
     * Using 2 datasources, gets a configuration where there is 1 shared, 1 free, and 1 unshared connection<br>
     * Invoke purge pool("abort") on each of the two pool manager MBeans.<br>
     * Verify pool size of both pools is 0 after the purgePoolContents call
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException" })
    @SuppressWarnings("unused")
    @SkipIfDataSourceProperties({ SYBASE, INFORMIX_JDBC }) // no 4.1 sybase or ifx driver
    public void testMBeanPurgeAbort() throws Throwable {
        Connection conn1 = ds1.getConnection(); // Create an unshareable connection
        Connection conn2 = ds1.getConnection(); // Create a free connection
        conn2.close();
        Connection conn3 = ds2.getConnection(); // Create a shared connection

        // Purge the connection pool for ds1 and ds2 using abort
        System.out.println("--- About to purge connection pools.");

        getConnectionManagerBean("jdbc/ds1").purgePoolContents("abort");
        getConnectionManagerBean("jdbc/ds2").purgePoolContents("abort");

        System.out.println("--- Pools should now be empty");

        if (getPoolSize("jdbc/ds1") != 0)
            throw new Exception("Not all connections were purged from the ds1 pool.");
        if (getPoolSize("jdbc/ds2") != 0)
            throw new Exception("Not all connections were purged from the ds2 pool.");

        // Connections do not need to be closed since they were aborted
    }

    /**
     * Setup helper method that does 3 things: <ol>
     * <li> Create User Defined Type (UDT) called MYUSER based on the <code>jdbc.fat.v41.web.MyUser</code> class
     * <li> Create table USERS with schema <code>(int id, MYUSER userObj)</code>
     * <li> Create stored procedure <code>GET_MYUSER(IN id, OUT userObj)</code>
     * </ol>
     * NOTE: Will no op if this method has already been run
     */
    private synchronized void registerMyUser(Connection conn) throws SQLException {
        // No-op if we already registered getUsers()
        if (isGetUserRegistered) {
            System.out.println("registerMyUser already registered.");
            return;
        }

        Statement stmt = conn.createStatement();
        // Create data type
        try {
            stmt.execute("CREATE TYPE MYUSER EXTERNAL NAME 'jdbc.fat.v41.web.MyUser' LANGUAGE JAVA");
        } catch (SQLException e) {
            // type already exists
        }

        // Create table
        super.createTable(conn, userTable, "id int not null primary key, userObj MYUSER");

        // Registered getUsers stored procedure
        String regProc = "CREATE PROCEDURE GET_MYUSER(IN id INTEGER, OUT userObj MYUSER) " +
                         "PARAMETER STYLE JAVA " +
                         "LANGUAGE JAVA " +
                         "READS SQL DATA " +
                         "DYNAMIC RESULT SETS 1 " +
                         "EXTERNAL NAME 'jdbc.fat.v41.web.BasicTestServlet.getUsers'";
        try {
            stmt.execute("DROP PROCEDURE GET_MYUSER");
        } catch (SQLException e) {
            // procedure did not exist yet
        }
        stmt.execute(regProc);
        stmt.close();

        isGetUserRegistered = true;
    }

    private synchronized void registerGetColors() throws SQLException {
        // No-op if we already registered getColors()
        if (isGetColorRegistered)
            return;

        Connection conn = ds1.getConnection();
        try {
            String regProc = "CREATE PROCEDURE GET_COLORS(IN color1 INTEGER, IN color2 INTEGER) " +
                             "PARAMETER STYLE JAVA " +
                             "LANGUAGE JAVA " +
                             "READS SQL DATA " +
                             "DYNAMIC RESULT SETS 2 " +
                             "EXTERNAL NAME 'jdbc.fat.v41.web.BasicTestServlet.getColors'";
            Statement stmt = conn.createStatement();
            try {
                stmt.execute("DROP PROCEDURE GET_COLORS");
            } catch (SQLException e) {
                // procedure did not exist yet
            }
            stmt.execute(regProc);
            stmt.close();
            isGetColorRegistered = true;
        } finally {
            conn.close();
        }
    }

    /**
     * Stored procedure which will attach two open result sets to the calling CallableStatement.
     *
     * @param color1 Color to be retrieved into rs1
     * @param color2 Color to be retrieved into rs2
     * @param rs1    Result set of color1 query
     * @param rs2    Result set of color2 query
     */
    public static void getColors(int color1, int color2, ResultSet[] rs1, ResultSet[] rs2) throws SQLException {
        // Get the connection of the calling procedure
        Connection con = DriverManager.getConnection("jdbc:default:connection");
        PreparedStatement ps1 = con.prepareStatement("select color from " + colorTable + " where id=" + color1);
        rs1[0] = ps1.executeQuery();

        PreparedStatement ps2 = con.prepareStatement("select color from " + colorTable + " where id=" + color2);
        rs2[0] = ps2.executeQuery();
    }

    public static void getUsers(int id, MyUser[] myUser, ResultSet[] rs) throws SQLException {
        // Get the connection of the calling procedure
        Connection con = DriverManager.getConnection("jdbc:default:connection");
        PreparedStatement ps1 = con.prepareStatement("select userObj from " + userTable + " where id=" + id);
        rs[0] = ps1.executeQuery();
        rs[0].next();
        myUser[0] = rs[0].getObject("userObj", MyUser.class);
    }

    /**
     * Get the managed connection ID of a given Conneciton.
     */
    private String getManagedConnectionID(java.sql.Connection conn1) {
        for (Class<?> clazz = conn1.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Field f1 = clazz.getDeclaredField("managedConn");
                f1.setAccessible(true);
                String mc1 = String.valueOf(f1.get(conn1));
                f1.setAccessible(false);
                return mc1;
            } catch (Exception ignore) {
            }
        }
        throw new RuntimeException("Did not find field 'managedConn' on " + conn1.getClass());
    }

    private List<String> getSchemas(java.sql.Connection conn) throws Exception {
        if (globalSchemaList != null) {
            System.out.println("Got schemas: " + globalSchemaList);
            return globalSchemaList;
        }

        globalSchemaList = new ArrayList<String>();
        ResultSet schemas = conn.getMetaData().getSchemas();
        while (schemas.next() && globalSchemaList.size() <= 5) {
            String curSchema = schemas.getString("TABLE_SCHEM");
            globalSchemaList.add(curSchema);
        }
        schemas.close();

        // Since schema list gets cut off after 5 schemas,
        // make sure that the current schema is on there
        String currentSchema = conn.getSchema();
        if (!globalSchemaList.contains(currentSchema))
            globalSchemaList.add(currentSchema);

        System.out.println("Got schemas: " + globalSchemaList);

        return globalSchemaList;
    }

    private Object getCacheKey(PreparedStatement stmt) throws Exception {
        for (Class<?> clazz = stmt.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Field field1 = clazz.isAssignableFrom(CallableStatement.class) ? //
                                clazz.getDeclaredField("cstmtImpl") : //
                                clazz.getDeclaredField("pstmtImpl");
                field1.setAccessible(true);
                return field1.get(stmt);
            } catch (Exception ignore) {
            }
        }
        throw new RuntimeException("Did not find field 'cstmtImpl' on " + stmt.getClass());
    }

    private ConnectionManagerMBean getConnectionManagerBean(String jndiName) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + MBEAN_TYPE + ",jndiName=" + jndiName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        if (s.size() != 1) {
            System.out.println("ERROR: Found incorrect number of MBeans (" + s.size() + ")");
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        return JMX.newMBeanProxy(mbs, s.iterator().next().getObjectName(), ConnectionManagerMBean.class);
    }

    private int getPoolSize(String jndiName) throws Exception {
        ConnectionManagerMBean cmBean = getConnectionManagerBean(jndiName);
        return (int) cmBean.getSize();
    }

    /**
     * Invocation handler that delegates all operations to the specified instance.
     */
    private static class DelegatingInvocationHandler implements InvocationHandler {
        private final Object instance;

        private DelegatingInvocationHandler(Object instance) {
            this.instance = instance;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(instance, args);
        }
    }

    /**
     * Ensure it is possible to proxy the list of interfaces implemented by the Liberty JDBC connection,
     * database meta data, statements, and result set classes.
     */
    @Test
    public void testProxyForLibertyJDBCProxies() throws Exception {
        Connection con = xads.getConnection();
        try {
            Connection conProxy = (Connection) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                                                      con.getClass().getInterfaces(),
                                                                      new DelegatingInvocationHandler(con));
            PreparedStatement ps = conProxy.prepareStatement("insert into " + colorTable + " values(?,?)");
            PreparedStatement psProxy = (PreparedStatement) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                                                                   ps.getClass().getInterfaces(),
                                                                                   new DelegatingInvocationHandler(ps));
            psProxy.setInt(1, 7);
            psProxy.setString(2, "orange");
            assertEquals(1, psProxy.executeUpdate());
            psProxy.close();

            ResultSet rs = con.createStatement().executeQuery("select color from " + colorTable + " where id=7");
            ResultSet rsProxy = (ResultSet) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                                                   rs.getClass().getInterfaces(),
                                                                   new DelegatingInvocationHandler(rs));
            assertTrue(rsProxy.next());
            assertEquals("orange", rsProxy.getString(1));
            Statement s = rsProxy.getStatement();
            rsProxy.close();
            s.close();

            DatabaseMetaData mdata = conProxy.getMetaData();
            DatabaseMetaData mdataProxy = (DatabaseMetaData) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                                                                    mdata.getClass().getInterfaces(),
                                                                                    new DelegatingInvocationHandler(mdata));

            CallableStatement cs = mdataProxy.getConnection().prepareCall("update " + colorTable + " set id=? where id=?");
            CallableStatement csProxy = (CallableStatement) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                                                                   cs.getClass().getInterfaces(),
                                                                                   new DelegatingInvocationHandler(cs));
            csProxy.setInt(1, 8);
            csProxy.setInt(2, 7);
            assertEquals(1, csProxy.executeUpdate());
            csProxy.close();

            s = conProxy.createStatement();
            Statement sProxy = (Statement) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                                                  s.getClass().getInterfaces(),
                                                                  new DelegatingInvocationHandler(s));
            assertEquals(1, sProxy.executeUpdate("delete from " + colorTable + " where id=8"));
            sProxy.close();
        } finally {
            con.close();
        }
    }
}
