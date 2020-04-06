/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basicfat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLNonTransientException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.UserTransaction;

import org.apache.derby.iapi.jdbc.EngineConnection;
import org.apache.derby.iapi.jdbc.EngineResultSet;

import componenttest.app.FATServlet;

@DataSourceDefinition(
                      name = "jdbc/dsfat6",
                      className = "org.apache.derby.jdbc.EmbeddedDataSource40",
                      databaseName = "${shared.resource.dir}/data/derbyfat",
                      description = "description #6",
                      initialPoolSize = 0,
                      isolationLevel = Connection.TRANSACTION_READ_COMMITTED,
                      loginTimeout = 600,
                      maxIdleTime = 60,
                      maxPoolSize = 6,
                      maxStatements = 26, // converts to statementCacheSize per connection = 26/6 = 4
                      minPoolSize = 1,
                      transactional = true,
                      user = "dbuser1",
                      password = "{xor}Oz0vKDtu",
                      properties = {
                                     "agedTimeout=3s",
                                     "connectionSharing=MatchCurrentState",
                                     "connectionTimeout=0",
                                     "createDatabase=create",
                                     "queryTimeout=1m", // 60 seconds
                                     "reapTime=2", // default unit is seconds
                                     "syncQueryTimeoutWithTransactionTimeout=true"
                      })
@DataSourceDefinitions(value = {
                                 @DataSourceDefinition(
                                                       name = "java:module/env/jdbc/dsfat7",
                                                       className = "org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource40",
                                                       isolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED,
                                                       loginTimeout = 77, // web.xml overrides with 70
                                                       user = "dbuser7", // web.xml overrides with dbuser1
                                                       password = "{xor}Oz0vKDtu",
                                                       properties = {
                                                                      "shutdownDatabase=shutdown" // web.xml overrides with false.
                                                       }),

                                 @DataSourceDefinition(
                                                       name = "java:comp/env/jdbc/dsfat8",
                                                       className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                                                       databaseName = "${shared.resource.dir}/data/derbyfat",
                                                       loginTimeout = 80,
                                                       maxIdleTime = 3,
                                                       user = "dbuser1",
                                                       password = "{xor}Oz0vKDtu",
                                                       properties = {
                                                                      "createDatabase=create",
                                                                      "purgePolicy=ValidateAllConnections",
                                                                      "reapTime=2s",
                                                                      "recoveryAuthDataRef=derbyAuth1"
                                                       }),
                                 @DataSourceDefinition(
                                                       name = "java:comp/env/jdbc/dsValTderbyAnn",
                                                       className = "org.apache.derby.jdbc.EmbeddedDataSource40",
                                                       databaseName = "memory:dsValTderbyAnn",
                                                       user = "dbuser1",
                                                       password = "{xor}Oz0vKDtu",
                                                       properties = {
                                                                      "createDatabase=create",
                                                                      "validationTimeout=10s"
                                                       })

})
@SuppressWarnings("serial")
public class DataSourceTestServlet extends FATServlet {
    private static final String className = "DataSourceTestServlet";

    @Resource(name = "jdbc/dsfat0")
    DataSource ds0; // one-phase, default isolation, MatchOriginalRequest

    @Resource(name = "jdbc/dsfat1", shareable = true)
    DataSource ds1; // one-phase, default isolation, MatchCurrentState

    @Resource(lookup = "jdbc/dsfat1", shareable = false)
    DataSource ds1u; // one-phase, default isolation, unsharable

    @Resource(name = "jdbc/dsfat2", authenticationType = Resource.AuthenticationType.APPLICATION)
    DataSource ds2; // two-phase, TRANSACTION_READ_COMMITTED, MatchOriginalRequest

    @Resource(name = "jdbc/dsfat3", authenticationType = Resource.AuthenticationType.CONTAINER, description = "a non-transactional data source")
    DataSource ds3; // non-transactional, default isolation, MatchOriginalRequest

    @Resource(name = "jdbc/dsfat4", type = DataSource.class)
    DataSource ds4; // two-phase, default isolation, MatchCurrentState

    @Resource(name = "jdbc/dsfat4ref2", lookup = "jdbc/dsfat4", shareable = false, type = DataSource.class)
    DataSource ds4u_2; // two-phase, isolation=2, unsharable

    @Resource(name = "jdbc/dsfat4ref8", lookup = "jdbc/dsfat4", shareable = false, type = DataSource.class)
    DataSource ds4u_8; // two-phase, isolation=8, unsharable

    @Resource(lookup = "jdbc/dsfat5", shareable = true)
    DataSource ds5; // one-phase, default isolation, MatchOriginalRequest, sharable, Derby only

    @Resource(lookup = "jdbc/dsfat5", shareable = false)
    DataSource ds5u; // one-phase, default isolation, unsharable, Derby only

    @Resource(name = "jdbc/dsfat5ref1", lookup = "jdbc/dsfat5")
    DataSource ds5_1; //one-phase, default isolation, Derby Only, derbyAuth1 set as authentication alias

    @Resource(lookup = "jdbc/dsfatmca", type = DataSource.class)
    DataSource dsmca; // one-phase, default isolation, MatchOriginalRequest, sharable, mapping config alias

    @Resource(name = "jdbc/dsfat6ref", lookup = "java:comp/env/jdbc/dsfat6")
    DataSource ds6; // one-phase, Derby only, see @DataSourceDefinition above

    // one-phase, Derby only, see @DataSourceDefinition above
    // ibm-web-ext.xml overrides isolation to 1, ibm-web-bnd.xml sets authentication-alias to derbyAuth1
    @Resource(name = "jdbc/dsfat6ref1", lookup = "java:comp/env/jdbc/dsfat6")
    DataSource ds6_1;

    @Resource(name = "jdbc/dsfat7ref", lookup = "java:module/env/jdbc/dsfat7")
    DataSource ds7; // one-phase, Derby only

    @Resource(lookup = "java:comp/env/jdbc/dsfat8")
    DataSource ds8; // two-phase, Derby only

    @Resource(lookup = "java:app/env/jdbc/dsfat9")
    DataSource ds9; // one-phase, default isolation, MatchOriginalRequest, sharable, Derby only

    // web.xml: java:comp/env/jdbc/dsfat10ref2  two-phase, isolation=2, unsharable, container auth=dbuser2

    // server.xml: jdbc/dsfat11 // one-phase, TRANSACTION_READ_UNCOMMITTED, contains invalid configuration

    // web.xml: java:comp/env/jdbc/dsfatclcref  two-phase, isolation=2, unsharable, custom-login-configuration=myJAASLoginEntry, container auth=dbuser1

    @Resource(lookup = "jdbc/dsfat20")
    DataSource ds20; // two-phase, TRANSACTION_READ_COMMITTED, MatchOriginalRequest

    @Resource(lookup = "jdbc/dsfat22", shareable = false)
    DataSource ds22;

    @Resource(lookup = "java:comp/env/jdbc/dsValTderbyAnn")
    DataSource dsValTderbyAnn;

    /**
     * This state is used by certain tests to determine whether a Servlet instance,
     * and thus the application as a whole, survives a config update.
     */
    private String state = "NEW";

    @Resource
    private UserTransaction tran;

    @Resource
    private ExecutorService executor;

    /**
     * Standard isolation level values.
     */
    private static final int[] ISOLATION_LEVELS = new int[] {
                                                              Connection.TRANSACTION_READ_COMMITTED,
                                                              Connection.TRANSACTION_REPEATABLE_READ,
                                                              Connection.TRANSACTION_SERIALIZABLE,
                                                              Connection.TRANSACTION_READ_UNCOMMITTED
    };

    @Override
    public void init(ServletConfig c) throws ServletException {
        //Create table in database container
        try (Connection con = ds1.getConnection()) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("create table cities (name varchar(50) not null primary key, population int, county varchar(30))");
            }
        } catch (SQLException e) {
            //Ignore, server could have been restarted. Assume table is available.
        }

        //Create table in derby database
        try (Connection con = ds5u.getConnection()) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("create table cities (name varchar(50) not null primary key, population int, county varchar(30))");
            }
        } catch (SQLException e) {
            //Ignore, server could have been restarted. Assume table is available.
        }

    }

    /**
     * Utility method to get a different transaction isolation level if supported by the JDBC driver.
     * If not supported, then return the transaction isolation level currently set on the connection.
     *
     * @param con connection to the database.
     * @return isolation level
     * @throws SQLException if an error occurs
     */
    private static final int getDifferentIsolationIfSupported(Connection con) throws SQLException {
        DatabaseMetaData metadata = con.getMetaData();
        int isolation = con.getTransactionIsolation();
        for (int i : ISOLATION_LEVELS)
            if (i != isolation && metadata.supportsTransactionIsolationLevel(i))
                return i;
        return isolation; // in case the driver only supports one isolation level
    }

    /**
     * clears table of all data to ensure fresh start for this test.
     *
     * @param datasource the data source to clear the table for
     */
    public void clearTable(DataSource datasource) throws Exception {
        try (Connection con = datasource.getConnection()) {
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate("delete from cities");
            }
        }

        // End the current LTC and get a new one, so that test methods start from the correct place
        tran.begin();
        tran.commit();
    }

    public void requireNewServletInstance(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        if (!"NEW".equals(state))
            throw new Exception("It appears that the existing servlet instance was used, meaning the app was not restarted. State: " + state);
    }

    public void requireServletInstanceStillActive(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        if (!"SERVLET_INSTANCE_STILL_ACTIVE".equals(state))
            throw new Exception("It appears that a different servlet instance was used, meaning the app was restarted. State: " + state);
    }

    public void resetState(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        state = "NEW";
    }

    public void setServletInstanceStillActive(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        state = "SERVLET_INSTANCE_STILL_ACTIVE";
    }

    /**
     * Run a basic query to the database.
     */
    public void testBasicQuery() throws Exception {
        clearTable(ds1);
        Connection con = ds1.getConnection();
        try {
            Statement stmt = con.createStatement();
            stmt.executeUpdate("insert into cities values ('Rochester', 106769, 'Olmsted')");
            ResultSet result = stmt.executeQuery("select county from cities where name='Rochester'");
            if (!result.next())
                throw new Exception("Entry missing from database");
            String value = result.getString(1);
            if (!"Olmsted".equals(value))
                throw new Exception("Incorrect value: " + value);
        } finally {
            con.close();
        }
    }

    /**
     * Attempts to load the two datasources (jdbc/derbyWithNestedList and jdbc/db2WithNestedList) that contain nested libraries that don't have an id.
     * This is a test for APAR PI23168.
     */
    public void testTwoNestedLibrariesWithNoIds() throws Exception {
        DataSource derbyDs = (DataSource) (new InitialContext()).lookup("jdbc/derbyWithNestedList");
        System.out.println("Success loading derby DS: " + derbyDs.toString());
        DataSource db2Ds = (DataSource) (new InitialContext()).lookup("jdbc/db2WithNestedList");
        System.out.println("Success loading DB2 DS: " + db2Ds.toString());
    }

    /**
     * Run batch updates.
     */
    public void testBatchUpdates() throws Exception {
        clearTable(ds1);
        Connection con = ds1.getConnection();
        try {
            DatabaseMetaData metadata = con.getMetaData();
            if (!metadata.supportsBatchUpdates()) {
                System.out.println("skipped testBatchUpdates<br>");
                return; // can't run this test
            }

            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, 'Olmsted')");

            pstmt.setString(1, "Byron");
            pstmt.setInt(2, 4914);
            pstmt.addBatch();
            pstmt.setString(1, "Chatfield");
            pstmt.setInt(2, 2779);
            pstmt.addBatch();
            pstmt.setString(1, "Dover");
            pstmt.setInt(2, 735);
            pstmt.addBatch();
            pstmt.setString(1, "Eyota");
            pstmt.setInt(2, 1977);
            pstmt.addBatch();
            pstmt.setString(1, "Oronoco");
            pstmt.setInt(2, 1300);
            pstmt.addBatch();
            pstmt.setString(1, "Stewartville");
            pstmt.setInt(2, 5916);
            pstmt.addBatch();

            int[] updateCounts = pstmt.executeBatch();

            if (updateCounts.length != 6)
                throw new Exception("Wrong number of update counts: " + updateCounts.length);

            // JDBC driver can return success with unknown number of updates
            if (updateCounts[0] == Statement.SUCCESS_NO_INFO)
                return;

            for (int i = 0; i < 6; i++)
                if (updateCounts[i] != 1)
                    throw new Exception("Unexpected update count " + updateCounts[i] + " at position " + i);
        } finally {
            con.close();
        }
    }

    /**
     * Verify that a config change to authData is honored.
     * It is only valid to run this method after having changed the user for
     * authData derbyAuth1 to be "updatedUserName"
     */
    public void testConfigChangeAuthData() throws Throwable {

        Connection con = ds5_1.getConnection();
        try {
            DatabaseMetaData metadata = con.getMetaData();
            String user = metadata.getUserName();
            if (!"updatedUserName".equalsIgnoreCase(user))
                throw new Exception("User name from authData ID:derbyAuth1 was not honored. Expected: updatedUserName Instead: " + user);
        } finally {
            con.close();
        }
    }

    /**
     * Verify that a config change that sets authData derbyAuth1 back its original value.
     */
    public void testConfigChangeAuthDataOriginalValue() throws Throwable {

        Connection con = ds5_1.getConnection();
        try {
            DatabaseMetaData metadata = con.getMetaData();
            String user = metadata.getUserName();
            if (!"dbuser1".equalsIgnoreCase(user))
                throw new Exception("User name from authData ID:derbyAuth1 was not honored. Expected: dbuser1 Instead: " + user);
        } finally {
            con.close();
        }
    }

    /**
     * Verify commitOrRollbackOnCleanup=commit
     */
    public void testConfigChangeCommitOnCleanup() throws Throwable {
        clearTable(ds3);

        Connection con = ds3.getConnection();
        try {
            con.setAutoCommit(false);
            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Minnetonka");
            pstmt.setInt(2, 49734);
            pstmt.setString(3, "Hennepin");
            pstmt.executeUpdate();
        } finally {
            con.close();
        }

        con = ds3.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Minnetonka");
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Entry not committed when connection closed.");
        } finally {
            con.close();
        }
    }

    /**
     * Tests that connection can be casted to Derby interfaces when enableConnectionCasting=true.
     */
    public void testConfigChangeConnectionCastingEnabled() throws Exception {
        DataSource ds10 = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/dsfat10ref2");
        Connection con = ds10.getConnection();
        try {
            org.apache.derby.iapi.jdbc.EngineConnection derbyCon = (org.apache.derby.iapi.jdbc.EngineConnection) con;

            String currentSchemaName = derbyCon.getCurrentSchemaName();
            String userName = derbyCon.getMetaData().getUserName();
            if (!userName.equalsIgnoreCase(currentSchemaName))
                throw new Exception("Unexpected value for getCurrentSchemaName: " + currentSchemaName + ". Expecting: " + userName);

            if (!derbyCon.isWrapperFor(Connection.class))
                throw new Exception("Connection handle " + derbyCon + " not a wrapper for Connection.");

            if (!derbyCon.isWrapperFor(org.apache.derby.iapi.jdbc.EngineConnection.class))
                throw new Exception("Connection handle " + derbyCon + " not a wrapper for EngineConnection");

            Connection unwrapped1 = derbyCon.unwrap(Connection.class);
            int iso1 = unwrapped1.getTransactionIsolation();

            EngineConnection unwrapped2 = derbyCon.unwrap(org.apache.derby.iapi.jdbc.EngineConnection.class);
            int iso2 = unwrapped2.getTransactionIsolation();

            if (iso1 != iso2)
                throw new Exception("Isolation level " + iso1 + " from connection handle " + unwrapped1 +
                                    " unwrapped as Connection does not match " +
                                    "isolation level " + iso2 + " from connection handle " + unwrapped2 +
                                    " unwrapped as EngineConnection.");
        } finally {
            con.close();
        }
    }

    /**
     * Tests that connection cannot be casted to Derby interfaces when enableConnectionCasting=false,
     * but it can still be unwrapped to Derby interfaces.
     */
    public void testConfigChangeConnectionCastingDisabled() throws Exception {
        DataSource ds10 = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/dsfat10ref2");
        Connection con = ds10.getConnection();
        try {
            if (con instanceof org.apache.derby.iapi.jdbc.EngineConnection)
                throw new Exception("Connection handle " + con + " should not be castable. Interfaces are: " + Arrays.toString(con.getClass().getInterfaces()));

            if (!con.isWrapperFor(org.apache.derby.iapi.jdbc.EngineConnection.class))
                throw new Exception("Connection handle " + con + " not a wrapper for EngineConnection");

            int prepareIsolation = con.unwrap(org.apache.derby.iapi.jdbc.EngineConnection.class).getPrepareIsolation();
            if (prepareIsolation != 0)
                throw new Exception("Unexpected prepareIsolation: " + prepareIsolation);
        } finally {
            con.close();
        }
    }

    /**
     * Verify configuration changes to various data source properties.
     */
    public void testConfigChangeDataSourceModified() throws Throwable {

        final Connection con1 = ds5.getConnection();
        try {
            // isolationLevel changed to 1
            int isolation = con1.getTransactionIsolation();
            if (isolation != Connection.TRANSACTION_READ_UNCOMMITTED)
                throw new Exception("Expecting isolation=1, not " + isolation);

            // queryTimeout changed to 10
            PreparedStatement pstmt = con1.prepareStatement("values (1)");
            int queryTimeout = pstmt.getQueryTimeout();
            if (queryTimeout != 10)
                throw new Exception("Expecting queryTimeout=10, not " + queryTimeout);
            pstmt.close();

            // Get a dissociated handle with TRANSACTION_READ_COMMITTED (non-default) isolation level.
            Connection con2 = ds5.getConnection();
            try {
                con2.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                tran.begin();
                tran.commit();
            } catch (Throwable t) {
                con2.close();
                throw t;
            }

            tran.setTransactionTimeout(80);
            try {
                tran.begin();
                try {
                    // connectionSharing=MatchCurrentState
                    con1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    con1.prepareStatement("values (2)").close();

                    // con2 will be unable to share and will fail if not MatchCurrentState
                    pstmt = con2.prepareStatement("values (3)");

                    // syncQueryTimeoutWithTransactionTimeout changed to false
                    pstmt.executeQuery();
                    queryTimeout = pstmt.getQueryTimeout();
                    pstmt.close();
                } finally {
                    tran.rollback();
                }
            } finally {
                tran.setTransactionTimeout(0); // restore default
            }
            if (queryTimeout != 10) // tolerate any elapsed time for the query
                throw new Exception("Expecting queryTimeout(during UserTransaction)=10, not " + queryTimeout);

        } finally {
            con1.close();
        }
    }

    /**
     * Verify configuration changes to various data source properties,
     * back to the original values.
     */
    public void testConfigChangeDataSourceOriginalConfig() throws Throwable {

        final Connection con1 = ds5.getConnection();
        try {
            // isolationLevel
            int isolation = con1.getTransactionIsolation();
            if (isolation != Connection.TRANSACTION_REPEATABLE_READ)
                throw new Exception("Expecting isolation=4, not " + isolation);

            // queryTimeout
            PreparedStatement pstmt = con1.prepareStatement("values (1)");
            int queryTimeout = pstmt.getQueryTimeout();
            if (queryTimeout != 30)
                throw new Exception("Expecting queryTimeout=30, not " + queryTimeout);
            pstmt.close();

            tran.setTransactionTimeout(90);
            try {
                tran.begin();
                try {
                    // connectionSharing=MatchOriginalRequest
                    con1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    con1.prepareStatement("values (2)").close();

                    // a second connection request will fail if not MatchOriginalRequest
                    Connection con2 = ds5.getConnection();
                    pstmt = con2.prepareStatement("values (3)");

                    // syncQueryTimeoutWithTransactionTimeout=true
                    pstmt.executeQuery();
                    queryTimeout = pstmt.getQueryTimeout();
                    pstmt.close();
                } finally {
                    tran.rollback();
                }
            } finally {
                tran.setTransactionTimeout(0); // restore default
            }
            if (queryTimeout < 85 || queryTimeout > 90) // tolerate any elapsed time for the query
                throw new Exception("Expecting queryTimeout(sync to tran)=90, not " + queryTimeout);

        } finally {
            con1.close();
        }
    }

    /**
     * Verify a configuration change that has an error in the fileset dir.
     * This test assumes that we have made the update to jdbc/dsfat15.
     */
    public void testConfigChangeFilesetBad() throws Throwable {
        try {
            DataSource ds15 = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/dsfat15ref");
            ds15.getConnection().close();
            throw new Exception("Invalid fileset dir should cause failure that JDBC driver classes cannot be found");
        } catch (NamingException x) {
            Throwable cause = x;
            while (cause.getCause() != null && (!(cause instanceof SQLException)))
                cause = cause.getCause();
            if (!cause.getMessage().contains("DSRA4000E"))
                throw x;
        } catch (SQLNonTransientException x) {
            if (!x.getMessage().contains("DSRA4000E"))
                throw x;
        }
    }

    /**
     * Verify a configuration change that doesn't have an error in the fileset dir.
     * This test assumes that we have made the update to jdbc/dsfat15.
     */
    public void testConfigChangeFilesetGood() throws Throwable {
        DataSource ds15 = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/dsfat15ref");
        ds15.getConnection().close();
    }

    /**
     * Verify configuration change to vendor properties as nested config (loginTimeout=320)
     *
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testConfigChangeLoginTimeout320() throws Throwable {

        int loginTimeout;
        try {
            loginTimeout = ds2.getLoginTimeout();
        } catch (SQLFeatureNotSupportedException x) {
            return; // skip the test if the JDBC driver doesn't support login timeout
        }
        if (loginTimeout != 320)
            throw new Exception("Unexpected value for getLoginTimeout: " + loginTimeout);
    }

    /**
     * Verify configuration change to vendor properties as top level config (loginTimeout=320)
     */
    public void testConfigChangeLoginTimeout550() throws Throwable {

        int loginTimeout;
        try {
            loginTimeout = ds2.getLoginTimeout();
        } catch (SQLFeatureNotSupportedException x) {
            return; // skip the test if the JDBC driver doesn't support login timeout
        }
        if (loginTimeout != 550)
            throw new Exception("Unexpected value for ds2.getLoginTimeout: " + loginTimeout);

        loginTimeout = ds3.getLoginTimeout();
        if (loginTimeout != 550)
            throw new Exception("Unexpected value for ds3.getLoginTimeout: " + loginTimeout);

        loginTimeout = ds4.getLoginTimeout();
        if (loginTimeout != 550)
            throw new Exception("Unexpected value for ds4.getLoginTimeout: " + loginTimeout);
    }

    /**
     * Verify commitOrRollbackOnCleanup=rollback (or defaulted to rollback for transactional=false)
     */
    public void testConfigChangeRollbackOnCleanup() throws Throwable {
        clearTable(ds3);

        Connection con = ds3.getConnection();
        try {
            con.setAutoCommit(false);
            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Lakeville");
            pstmt.setInt(2, 55954);
            pstmt.setString(3, "Dakota");
            pstmt.executeUpdate();
        } finally {
            con.close();
        }

        con = ds3.getConnection();
        try {
            con.setAutoCommit(true);
            PreparedStatement pstmt = con.prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Lakeville");
            ResultSet result = pstmt.executeQuery();
            if (result.next())
                throw new Exception("Entry not rolled back when connection closed.");
        } finally {
            con.close();
        }
    }

    /**
     * Verify configuration change that top level config (transactional=false)
     */
    public void testConfigChangeTransactionalFalse() throws Throwable {
        clearTable(ds1u);
        PreparedStatement pstmt;
        Connection con = ds1u.getConnection();
        con.setAutoCommit(false);
        try {
            tran.begin();
            try {
                pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
                pstmt.setString(1, "Eagan");
                pstmt.setInt(2, 64206);
                pstmt.setString(3, "Dakota");
                pstmt.executeUpdate();

                tran.commit();
            } catch (Throwable x) {
                try {
                    tran.rollback();
                } catch (Throwable t) {
                }
                throw x;
            }
            con.rollback();
            PreparedStatement ps = con.prepareStatement("select name, population, county from cities where name = ?");
            ps.setString(1, "Eagan");
            ResultSet result = ps.executeQuery();
            if (result.next())
                throw new Exception("Entry should not be found in database");

            con.commit();
            tran.begin();
            try {
                pstmt.setString(1, "Woodbury");
                pstmt.setInt(2, 61961);
                pstmt.setString(3, "Washington");
                pstmt.executeUpdate();
            } finally {
                tran.rollback();
            }
            con.commit();
            ps.setString(1, "Woodbury");
            result = ps.executeQuery();
            if (!result.next())
                throw new Exception("Entry is missing from database");
        } finally {
            try {
                con.setAutoCommit(true);
            } finally {
                con.close();
            }
        }
    }

    /**
     * Verify configuration change that sets transactional=true
     */
    public void testConfigChangeTransactionalTrue() throws Throwable {
        clearTable(ds1u);
        PreparedStatement pstmt;
        Connection con = ds1u.getConnection();
        con.setAutoCommit(false);
        try {
            tran.begin();
            try {
                pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
                pstmt.setString(1, "Maple Grove");
                pstmt.setInt(2, 61567);
                pstmt.setString(3, "Hennepin");
                pstmt.executeUpdate();

                tran.commit();
            } catch (Throwable x) {
                try {
                    tran.rollback();
                } catch (Throwable t) {
                }
                throw x;
            }
            con.rollback();
            PreparedStatement ps = con.prepareStatement("select name, population, county from cities where name = ?");
            ps.setString(1, "Maple Grove");
            ResultSet result = ps.executeQuery();
            if (!result.next())
                throw new Exception("Entry is missing from database");

            con.commit();
            tran.begin();
            try {
                pstmt.setString(1, "Coon Rapids");
                pstmt.setInt(2, 61476);
                pstmt.setString(3, "Anoka");
                pstmt.executeUpdate();
            } finally {
                tran.rollback();
            }
            con.commit();
            ps.setString(1, "Coon Rapids");
            result = ps.executeQuery();
            if (result.next())
                throw new Exception("Entry should not be found in database");
        } finally {
            try {
                con.setAutoCommit(true);
            } finally {
                con.close();
            }
        }
    }

    /**
     * Keep a connection open for a few seconds while ConfigTest increases the queryTimeout.
     */
    public void testConfigChangeWithActiveConnections() throws Exception {

        Stack<Integer> results = new Stack<Integer>();
        Connection con = ds5.getConnection();
        try {
            for (int i = 0; i < 40; i++) {
                Thread.sleep(100);
                Statement s = con.createStatement();
                int queryTimeout = s.getQueryTimeout();
                int previous = results.isEmpty() ? 30 : results.peek();
                results.push(queryTimeout);
                if (queryTimeout < previous || queryTimeout > 34)
                    throw new Exception("Unexpected queryTimeout in " + results);
                s.close();
            }
        } finally {
            con.close();
        }

        if (results.peek() == 30) // no updates were made
            throw new Exception("Did not observe any updates to the queryTimeout: " + results);
    }

    /**
     * Use a data source defined with @DataSourceDefinition
     */
    public void testDataSourceDefinition() throws Throwable {
        clearTable(ds6);

        int loginTimeout = ds6.getLoginTimeout();
        if (loginTimeout != 600)
            throw new Exception("Incorrect loginTimeout for ds6: " + loginTimeout);

        Connection con = ds6.getConnection();
        try {
            String user = con.getMetaData().getUserName();
            if (!"dbuser1".equalsIgnoreCase(user))
                throw new Exception("Incorrect user for ds6: " + user);

            int isolation = con.getTransactionIsolation();
            if (isolation != Connection.TRANSACTION_READ_COMMITTED)
                throw new Exception("Incorrect isolationLevel for ds6: " + isolation);

            con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Apple Valley");
            pstmt.setInt(2, 49000);
            pstmt.setString(3, "Dakota");
            pstmt.executeUpdate();

            int queryTimeout = pstmt.getQueryTimeout();
            if (queryTimeout != 60)
                throw new Exception("Incorrect queryTimeout for ds6: " + queryTimeout);

            // Direct lookup of @DataSourceDefinition(name)
            DataSource dsfat6 = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/dsfat6");

            Connection con2 = dsfat6.getConnection();
            try {
                con2.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                con2.setAutoCommit(false);

                tran.setTransactionTimeout(40);
                try {
                    tran.begin();
                    try {
                        boolean isClosed = pstmt.isClosed();
                        if (!isClosed)
                            throw new Exception("Statement from sharable, transactional connection should be closed after tran.begin");

                        pstmt = con.prepareStatement("update cities set population = population + ? where name = ?");
                        pstmt.setInt(1, 80);
                        pstmt.setString(2, "Apple Valley");
                        pstmt.executeUpdate();

                        queryTimeout = pstmt.getQueryTimeout();
                        if (queryTimeout > 40 || queryTimeout < 35) // allow some buffer for really slow machines
                            throw new Exception("Query timeout should be synced to transaction timeout (around 40) not " + queryTimeout);

                        // Share connection based on MatchCurrentState
                        pstmt.close();
                        pstmt = con2.prepareStatement("update cities set population = population + ? where name = ?");
                        pstmt.setInt(1, 4);
                        pstmt.setString(2, "Apple Valley");
                        pstmt.executeUpdate();
                        pstmt.close();

                        isolation = con2.getTransactionIsolation();
                        if (isolation != Connection.TRANSACTION_REPEATABLE_READ)
                            throw new Exception("Expecting isolationLevel = 4 for connection shared on MatchCurrentState, not " + isolation);

                        tran.commit();
                    } catch (Throwable x) {
                        try {
                            tran.rollback();
                        } catch (Throwable t) {
                        }
                        throw x;
                    }
                } finally {
                    tran.setTransactionTimeout(0); // restore to default
                }

                con2.rollback();
                con2.setAutoCommit(true);
                pstmt = con2.prepareStatement("select name, population, county from cities where name = ?");
                pstmt.setString(1, "Apple Valley");
                ResultSet result = pstmt.executeQuery();
                if (!result.next())
                    throw new Exception("Entry not found in database");
                int population = result.getInt(2);
                if (population != 49084)
                    throw new Exception("With updates made during transaction, should be 49084, not: " + population);
            } finally {
                con2.close();
            }
        } finally {
            con.close();
        }
    }

    /**
     * Use data sources defined with @DataSourceDefinitions
     */
    public void testDataSourceDefinitions() throws Throwable {
        clearTable(ds7);

        int loginTimeout;

        loginTimeout = ds7.getLoginTimeout();
        if (loginTimeout != 70)
            throw new Exception("Wrong loginTimeout for ds7: " + loginTimeout);

        loginTimeout = ds8.getLoginTimeout();
        if (loginTimeout != 80)
            throw new Exception("Wrong loginTimeout for ds8: " + loginTimeout);

        loginTimeout = ds9.getLoginTimeout();
        if (loginTimeout != 90)
            throw new Exception("Wrong loginTimeout for ds9: " + loginTimeout);

        Connection con = ds7.getConnection();
        try {
            // isolationLevel = TRANSACTION_READ_UNCOMMITTED (1)
            int isolationLevel = con.getTransactionIsolation();
            if (isolationLevel != Connection.TRANSACTION_READ_UNCOMMITTED)
                throw new Exception("ds7: Expecting isolationLevel = 1, not " + isolationLevel);

            // user = dbuser1
            DatabaseMetaData metadata = con.getMetaData();
            String user = metadata.getUserName();
            if (!"dbuser1".equalsIgnoreCase(user))
                throw new Exception("ds7: Expecting user=dbuser1, not " + user);

            // databaseName = ${shared.resource.dir}/data/derbyfat
            String url = metadata.getURL();
            if (!url.contains("derbyfat"))
                throw new Exception("ds7: Expecting url to point to derbyfat database. Instead: " + url);

            tran.begin();
            try {
                // queryTimeout = 77000ms (77 seconds)
                PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
                pstmt.setString(1, "Shakopee");
                pstmt.setInt(2, 37000);
                pstmt.setString(3, "Scott");
                pstmt.executeUpdate();
                int queryTimeout = pstmt.getQueryTimeout();
                if (queryTimeout != 77)
                    throw new Exception("ds7: Expecting queryTimeout = 77 seconds (77000ms), not " + queryTimeout);
            } finally {
                // transactional = false (should ignore this transaction rollback)
                tran.rollback();
            }
        } finally {
            con.close();
        }

        con = ds8.getConnection();
        try {
            // isolationLevel defaults to TRANSACTION_REPEATABLE_READ (4)
            int isolationLevel = con.getTransactionIsolation();
            if (isolationLevel != Connection.TRANSACTION_REPEATABLE_READ)
                throw new Exception("ds8: The isolationLevel should default to 4, not " + isolationLevel);

            // user = dbuser1
            DatabaseMetaData metadata = con.getMetaData();
            String user = metadata.getUserName();
            if (!"dbuser1".equalsIgnoreCase(user))
                throw new Exception("ds8: Expecting user=dbuser1, not " + user);

            // databaseName = ${shared.resource.dir}/data/derbyfat
            String url = metadata.getURL();
            if (!url.contains("derbyfat"))
                throw new Exception("ds8: Expecting url to point to derbyfat database. Instead: " + url);

            tran.begin();
            try {
                PreparedStatement pstmt = con.prepareStatement("update cities set population = ? where name = ?");
                pstmt.setInt(1, 37076);
                pstmt.setString(2, "Shakopee");
                int updateCount = pstmt.executeUpdate();
                if (updateCount != 1)
                    throw new Exception("Expecting exactly one entry to be updated. Instead: " + updateCount);
                pstmt.close();
                con.close();

                con = ds9.getConnection();

                // isolationLevel = TRANSACTION_READ_COMMITTED (2)
                isolationLevel = con.getTransactionIsolation();
                if (isolationLevel != Connection.TRANSACTION_READ_COMMITTED)
                    throw new Exception("ds9: Expecting isolationLevel = 2, not " + isolationLevel);

                // user = dbuser1
                metadata = con.getMetaData();
                user = metadata.getUserName();
                if (!"dbuser1".equalsIgnoreCase(user))
                    throw new Exception("ds9: Expecting user=dbuser1, not " + user);

                // databaseName = ${shared.resource.dir}/data/derbyfat
                url = metadata.getURL();
                if (!url.contains("derbyfat"))
                    throw new Exception("ds9: Expecting url to point to derbyfat database. Instead: " + url);

                pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
                pstmt.setString(1, "Richfield");
                pstmt.setInt(2, 35228);
                pstmt.setString(3, "Hennepin");
                pstmt.executeUpdate();

                // queryTimeout = 1h4m60s (3900s)
                int queryTimeout = pstmt.getQueryTimeout();
                if (queryTimeout != 3900)
                    throw new Exception("ds9: Expecting queryTimeout = 3900 (1h4m60s). Instead: " + queryTimeout);

                tran.commit();
            } catch (Throwable x) {
                try {
                    tran.rollback();
                } catch (Throwable t) {
                }
                throw x;
            }
        } finally {
            con.close();
        }
    }

    /**
     * Use a data source with a mappingConfigAlias
     */
    public void testDataSourceMappingConfigAlias() throws Throwable {
        clearTable(dsmca);

        Connection con = dsmca.getConnection();
        try {
            String user = con.getMetaData().getUserName();
            if (!"dbuser1".equalsIgnoreCase(user))
                throw new Exception("Incorrect user for dsmca: " + user);
        } finally {
            con.close();
        }
    }

    /**
     * Use a data source with a custom-login-configuration
     */
    public void testDataSourceCustomLoginConfiguration() throws Throwable {
        DataSource dsclc = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/dsfatclcref");
        Connection con = dsclc.getConnection();
        try {
            String user = con.getMetaData().getUserName();
            if (!"dbuser1".equalsIgnoreCase(user))
                throw new Exception("Incorrect user for dsclc: " + user);
        } finally {
            con.close();
        }
    }

    /**
     * Test that we can add a GSSCredential to a Subject with a JAASLogin and then properly pool
     * based on the GSSCredential
     */
    public void testJAASLoginWithGSSCredential() throws Throwable {
        DataSource dsclc = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/dsfatgssref");
        Connection con = dsclc.getConnection();
        con.close();
        con = dsclc.getConnection();
        assertEquals("Should only have one connection due to pooling", 1, getPoolSize("jdbc/dsfatgss"));
        try {
            String user = con.getMetaData().getUserName();
            if (!"dbuser1".equalsIgnoreCase(user))
                throw new Exception("Incorrect user for dsfatgssref: " + user);
        } finally {
            con.close();
        }
    }

    /**
     * Connect to Derby and run a query.
     */
    public void testDerbyJDBCDriver() throws Exception {
        DataSource ds10 = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/dsfat10ref2");
        Connection con = ds10.getConnection();
        try {
            ResultSet result = con.createStatement().executeQuery("values length('abcdefghijklmnopqrstuvwxyz')");
            result.next();
            int length = result.getInt(1);
            if (length != 26)
                throw new Exception("Unexpected length of string: " + length);
        } finally {
            con.close();
        }
    }

    /**
     * Verify that connections are unshared when enableSharingForDirectLookups=false
     */
    public void testEnableSharingForDirectLookupsFalse() throws Exception {
        DataSource ds = (DataSource) new InitialContext().lookup("jdbc/dsfat1");
        clearTable(ds);
        Connection con = null;
        Connection con2 = null;
        Statement stmt = null;
        try {
            tran.begin();
            con = ds.getConnection();
            stmt = con.createStatement();
            stmt.execute("select * from cities");
            stmt.close();
            con2 = ds.getConnection();
            stmt = con2.createStatement();
            stmt.close();
            tran.commit();
            throw new Exception("Connection is shared - enableSharingForDirectLookups property is not being honored. " +
                                "If JDBC 4.3 or above is enabled, this is expected behavior (XA enabled on DS by default) and the test should be updated.");
        } catch (SQLException x) {
            tran.rollback();
        } finally {
            con2.close();
            con.close();
        }
    }

    /**
     * Verify that pooled connections are properly cleaned up.
     */
    public void testConnectionCleanup() throws Exception {
        clearTable(ds1);
        Connection con = ds1.getConnection();
        try {
            DatabaseMetaData metadata = con.getMetaData();

            boolean autocommit0 = con.getAutoCommit();
            int isolation0 = con.getTransactionIsolation();

            for (int i = 1; i <= 8; i *= 2)
                // all isolation level values
                if (i != isolation0 && metadata.supportsTransactionIsolationLevel(i))
                    con.setTransactionIsolation(i);

            con.setAutoCommit(!autocommit0);
            con.close();

            con = ds1.getConnection();
            boolean autocommit1 = con.getAutoCommit();
            int isolation1 = con.getTransactionIsolation();

            if (autocommit0 != autocommit1)
                throw new Exception("AutoCommit " + autocommit1 + " does not match " + autocommit0);

            if (isolation0 != isolation1)
                throw new Exception("Isolation " + isolation1 + " does not match " + isolation0);

        } finally {
            con.close();
        }
    }

    /**
     * Child JDBC resources should be closed implicitly when connection is closed.
     */
    public void testImplicitlyCloseChildren() throws Exception {
        clearTable(ds1);
        Connection con = ds1.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Winona");
            pstmt.setInt(2, 27592);
            pstmt.setString(3, "Winona");
            pstmt.executeUpdate();

            Statement stmt = con.createStatement();

            ResultSet result1 = stmt.executeQuery("select population from cities where name='Winona'");

            DatabaseMetaData metadata = con.getMetaData();
            ResultSet result2 = null;
            try {
                result2 = metadata.getSchemas();
            } catch (SQLFeatureNotSupportedException x) {
            } catch (SQLException s) {
                // Sybase jconn3 returns JZ0SA. This is known problem that is fixed in jconn4
                if (!s.getSQLState().contains("JZ0SA")) {
                    throw s;
                }
            }

            metadata.getConnection().close();

            if (!pstmt.isClosed())
                throw new Exception("PreparedStatement not closed when connection closed.");

            if (!stmt.isClosed())
                throw new Exception("Statement not closed when connection closed.");

            if (!result1.isClosed())
                throw new Exception("ResultSet from Statement not closed when connection closed.");

            if (result2 != null && !result2.isClosed())
                throw new Exception("ResultSet from DatabaseMetaData not closed when connection closed.");

            if (con.isValid(0))
                throw new Exception("Closed connection should not be isValid.");
        } finally {
            con.close();
        }
    }

    /**
     * DataSourceDefinition that uses jdbcDriverRef to use an existing JDBC driver definition.
     * Derby Embedded has a restriction against using the same database from multiple class loaders.
     * So if we don't get the same class loader (same JDBC driver), then this will fail.
     */
    public void testIsolatedSharedLibraries() throws Throwable {
        // ds9 (DataSourceDefinition) and ds5 (a <datasource> in server.xml)
        // both use the same database.
        clearTable(ds5u);

        // Write the value with ds9
        Connection con = ds9.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Blaine");
            pstmt.setInt(2, 57186);
            pstmt.setString(3, "Anoka");
            pstmt.executeUpdate();
        } finally {
            con.close();
        }

        // Read it with ds5
        con = ds5u.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Blaine");
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Entry not found in database.");

            EngineResultSet engineResult = result.unwrap(EngineResultSet.class);
            int length = engineResult.getLength(1);
            if (length <= 0)
                throw new Exception("Incorrect length: " + length);

            int population = result.getInt(2);
            if (population != 57186)
                throw new Exception("Incorrect value: " + population);
        } finally {
            con.close();
        }
    }

    /**
     * Enlist a single one-phase capable resource in a global transaction with a two-phase capable resource.
     */
    public void testLastParticipant() throws Throwable {
        clearTable(ds1);
        // Enlist both resources and commit changes
        tran.begin();
        try {
            Connection con1 = ds1.getConnection();
            Connection con2 = ds2.getConnection();

            PreparedStatement pstmt = con1.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Mazeppa");
            pstmt.setInt(2, 842);
            pstmt.setString(3, "Wabasha");
            pstmt.executeUpdate();

            pstmt = con2.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Zumbrota");
            pstmt.setInt(2, 3252);
            pstmt.setString(3, "Goodhue");
            pstmt.executeUpdate();

            tran.commit();
        } catch (Throwable x) {
            try {
                tran.rollback();
            } catch (Throwable t) {
            }
            throw x;
        }

        // Enlist both resources and roll back changes
        tran.begin();
        try {
            Connection con1 = ds1.getConnection();
            Connection con2 = ds2.getConnection();

            PreparedStatement pstmt = con1.prepareStatement("update cities set population = ? where name = ?");
            pstmt.setInt(1, 1000);
            pstmt.setString(2, "Mazeppa");
            pstmt.executeUpdate();

            pstmt = con2.prepareStatement("update cities set population = ? where name = ?");
            pstmt.setInt(1, 2000);
            pstmt.setString(2, "Zumbrota");
            pstmt.executeUpdate();
        } finally {
            tran.rollback();
        }

        // Verify the results
        Connection con = ds0.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Mazeppa");
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Entry from one-phase connection is missing.");
            int size = result.getInt(2);
            if (size != 842)
                throw new Exception("Incorrect value in first entry: " + size);

            pstmt.setString(1, "Zumbrota");
            result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Entry from two-phase connection is missing.");
            size = result.getInt(2);
            if (size != 3252)
                throw new Exception("Incorrect value in second entry: " + size);
        } finally {
            con.close();
        }
    }

    /**
     * Share connections for which the current state matches.
     */
    public void testMatchCurrentState() throws Throwable {
        clearTable(ds1);
        Connection[] cons = new Connection[2];
        try {
            cons[0] = ds1.getConnection();
            // Set a non-default isolation level
            int defaultIsolation = cons[0].getTransactionIsolation();
            int nonDefaultIsolation = getDifferentIsolationIfSupported(cons[0]);

            // This handle will be dissociated on tran.begin.  When subsequently used within
            // the transaction, the reassociation request will be made with the non-default
            // isolation level that we are setting here, which will only match if matching
            // is done based on the current state rather than the original connection request.
            cons[1] = ds1.getConnection();
            cons[1].setTransactionIsolation(nonDefaultIsolation);

            tran.begin();
            try {
                cons[0].setTransactionIsolation(nonDefaultIsolation);
                PreparedStatement pstmt = cons[0].prepareStatement("insert into cities values (?, ?, ?)");
                pstmt.setString(1, "Red Wing");
                pstmt.setInt(2, 16459);
                pstmt.setString(3, "");
                pstmt.executeUpdate();

                int isolation = cons[0].getTransactionIsolation();
                if (isolation != nonDefaultIsolation)
                    throw new Exception("con0 should have the isolation level that we set previously, " + nonDefaultIsolation + ", not " + isolation);

                // Data source is only one-phase capable so the second connection handle is
                // only possible if both handles share the same connection.
                pstmt = cons[1].prepareStatement("update cities set county=? where name=?");
                pstmt.setString(1, "Goodhue");
                pstmt.setString(2, "Red Wing");
                pstmt.executeUpdate();

                isolation = cons[1].getTransactionIsolation();
                if (isolation != nonDefaultIsolation)
                    throw new Exception("con1 should have the isolation level that we set previously, " + nonDefaultIsolation + ", not " + isolation);

                tran.commit();
            } catch (Throwable x) {
                try {
                    tran.rollback();
                } catch (Throwable t) {
                }
                throw x;
            }

            // TODO: Shouldn't need to set the isolation level back to avoid Oracle issues with subsequent tests
            cons[0].setTransactionIsolation(defaultIsolation);
            cons[1].setTransactionIsolation(defaultIsolation);

            PreparedStatement pstmt = cons[0].prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Red Wing");
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Missing database entry inserted by con0");
            String county = result.getString(3);
            if (!"Goodhue".equals(county))
                throw new Exception("Missing the update made by con1. Instead: " + county);
        } finally {
            for (Connection con : cons)
                if (con != null)
                    try {
                        con.close();
                    } catch (Throwable x) {
                    }
        }
    }

    /**
     * Share connections for which the original connection request matches.
     */
    public void testMatchOriginalRequest() throws Throwable {
        clearTable(ds0);
        int nonDefaultIsolation;
        Connection con = ds0.getConnection();
        try {
            nonDefaultIsolation = getDifferentIsolationIfSupported(con);
        } finally {
            con.close();
        }

        tran.begin();
        try {
            // Change the isolation level so that the current state of the connection
            // no longer matches its original request.
            Connection con1 = ds0.getConnection();
            con1.setTransactionIsolation(nonDefaultIsolation);
            PreparedStatement pstmt1 = con1.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt1.setString(1, "Wabasha");
            pstmt1.setInt(2, 2521);
            pstmt1.setString(3, "");
            pstmt1.executeUpdate();

            // Try to match based on the original request.
            Connection con2 = ds0.getConnection();
            int isolation = con2.getTransactionIsolation();

            DatabaseMetaData metadata = con2.getMetaData();
            if (isolation != nonDefaultIsolation
                && !metadata.getDatabaseProductName().toUpperCase().contains("DB2")) // because of isolation level switching (DB2)
                throw new Exception("Expecting non-default isolation of " + nonDefaultIsolation + " on shared connection, not " + isolation);

            PreparedStatement pstmt2 = con2.prepareStatement("update cities set county = ? where name = ?");
            pstmt2.setString(1, "Wabasha");
            pstmt2.setString(2, "Wabasha");
            int updateCount = pstmt2.executeUpdate();

            if (updateCount != 1)
                throw new Exception("Updated wrong number of entries in database: " + updateCount);

            tran.commit();
        } catch (Throwable x) {
            try {
                tran.rollback();
            } catch (Throwable t) {
            }
            throw x;
        }
    }

    /**
     * Connection request should time out per the connectionTimeout when maxPoolSize of 1 is exceeded.
     */
    public void testMaxPoolSize1() throws Throwable {

        Connection con1 = ds2.getConnection();
        long start = System.nanoTime();
        try {
            Connection con2 = ds2.getConnection();
            con2.close();
            throw new Exception("This connection should not be allowed (exceeds maxPoolSize of 1)");
        } catch (SQLTransientConnectionException x) {
            // Timeout should occur in 1 second, but let's allow lots of buffer for slow machines
            long duration = System.nanoTime() - start;
            if (duration > TimeUnit.SECONDS.toNanos(20))
                throw new Exception("Connection attempt should time out after 1 second, not " + duration + "ns");
        } finally {
            con1.close();
        }
    }

    /**
     * Connection request should time out per the connectionTimeout when maxPoolSize of 2 is exceeded.
     */
    public void testMaxPoolSize2() throws Throwable {

        Connection con1 = ds2.getConnection();
        try {
            Connection con2 = ds2.getConnection();
            long start = System.currentTimeMillis();
            try {
                Connection con3 = ds2.getConnection();
                con3.close();
                throw new Exception("This connection should not be allowed (exceeds maxPoolSize of 2)");
            } catch (SQLTransientConnectionException x) {
                // Timeout should occur in 1 second, but let's allow lots of buffer for slow machines. (Observed 7.4 second delay logging to FFDC)
                long duration = System.currentTimeMillis() - start;
                if (duration > 15000)
                    throw new Exception("Connection attempt should time out after 1 second, not " + duration + "ms");
            } finally {
                con2.close();
            }
        } finally {
            con1.close();
        }
    }

    /**
     * After maxIdleTime elapses, the next pool maintenance cycle
     * should cause the the pool size to drop to minPoolSize.
     * After the agedTimeout, the pool size should drop to 0.
     */
    public void testMinPoolSize() throws Throwable {
        clearTable(ds5u);
        ClassLoader loader;
        String databaseName;
        boolean testWasNotRun = false;

        // Need to empty the pool before running. Sleep for the AgedTimeout + 1.
        // TODO: In the future use JMX to purge the pool once that's available.
        Thread.sleep(6000);

        // Put 3 connections into the pool
        Connection con1 = ds5u.getConnection();
        try {
            // This test can only run against Derby Embedded
            // because we take advantage of shutting down the Derby database
            // in order to cause pooled connections to go bad.
            if (con1.getMetaData().getDriverName().indexOf("Derby Embedded") < 0)
                return;

            // Find the databaseName from the URL.
            String url = con1.getMetaData().getURL();
            databaseName = url.substring("jdbc:derby:".length());

            // Get the class loader used for the Derby driver
            Clob clob = con1.createClob();
            loader = clob.getClass().getClassLoader();
            clob.free();

            Connection con2 = ds5u.getConnection();
            try {
                ds5u.getConnection().close();
                System.out.println(System.currentTimeMillis() + ": first connection closed");
            } finally {
                con2.close();
                System.out.println(System.currentTimeMillis() + ": second connection closed");
            }
        } finally {
            con1.close();
            System.out.println(System.currentTimeMillis() + ": third connection closed");
        }

        long start = System.currentTimeMillis();

        // shut down Derby
        Class<?> EmbDS = Class.forName("org.apache.derby.jdbc.EmbeddedDataSource40", true, loader);
        DataSource ds = (DataSource) EmbDS.newInstance();
        EmbDS.getMethod("setDatabaseName", String.class).invoke(ds, databaseName);
        EmbDS.getMethod("setShutdownDatabase", String.class).invoke(ds, "shutdown");
        EmbDS.getMethod("setUser", String.class).invoke(ds, "dbuser1");
        EmbDS.getMethod("setPassword", String.class).invoke(ds, "{xor}Oz0vKDtu");
        try {
            ds.getConnection().close();
            throw new Exception("Failed to shut down Derby database: " + databaseName);
        } catch (SQLException x) {
            // expected for shutdown
            System.out.println(System.currentTimeMillis() + ": Derby shutdown result: " + x.getMessage());
        }

        // At this point there are 3 bad connections in the pool.
        // We must be careful to remove them if anything goes wrong so that we do not interfere with other test cases.
        try {
            // Wait 3 seconds (from close) for maxIdleTime to remove a connection
            /*
             * We are checking the time and logging the result to know how long its
             * taken to shut down derby. If this shut down takes too long on a slow system,
             * this test is invalid. We are just going to return. Normally derby shuts
             * down very fast.
             *
             * I added the same change for the 7 second shut down.
             */
            long elapsedTime = System.currentTimeMillis() - start;
            if (elapsedTime > 3000l) {
                System.out.println("Can't run test becase it took longer than 3 seconds to shut down derby: " + elapsedTime + "ms");
                /*
                 * Just return since the rest of the test will result in an unpredictable result. This should
                 * not occur during normal test runs. This timing issue should only occur
                 * when our test systems are too slow for various reason.
                 */
                testWasNotRun = true;
                return;
            }
            Thread.sleep(3000l - elapsedTime);
            System.out.println(System.currentTimeMillis() + ": about to attempt connection");

            // 2 bad connections should be left
            try {
                Connection conn = ds5u.getConnection();
                if (conn != null) {
                    try {
                        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO cities VALUES (?, ?, ?");
                        pstmt.setString(1, "Minneapolis - North Loop");
                        pstmt.setInt(2, 4291);
                        pstmt.setString(3, "Hennepin");
                        pstmt.executeUpdate();

                        throw new Exception("Insert #1 should have failed since the connection from the pool should be bad");
                    } finally {
                        conn.close();
                    }
                }
            } catch (SQLException x) {
                System.out.println(System.currentTimeMillis() + ": expected bad connection 1");
            }

            // 1 bad connection should be left
            try {
                Connection conn = ds5u.getConnection();
                if (conn != null) {
                    try {
                        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO cities VALUES (?, ?, ?");
                        pstmt.setString(1, "Minneapolis - North Loop");
                        pstmt.setInt(2, 4291);
                        pstmt.setString(3, "Hennepin");
                        pstmt.executeUpdate();

                        throw new Exception("Insert #2 should have failed since the connection from the pool should be bad");
                    } finally {
                        conn.close();
                    }
                }
            } catch (SQLException x) {
                System.out.println(System.currentTimeMillis() + ": expected bad connection 2");
            }

            // No connections should be left in pool. New request should succeed.
            ds5u.getConnection().close();
            System.out.println(System.currentTimeMillis() + ": good connection obtained");

            start = System.currentTimeMillis();

            // shut down Derby database again
            try {
                ds.getConnection().close();
                throw new Exception("Failed to shut down Derby database: " + databaseName);
            } catch (SQLException x) {
                // expected for shutdown
                System.out.println(System.currentTimeMillis() + ": Derby shutdown result: " + x.getMessage());
            }

            // Wait 7 seconds (from close) for agedTimeout to remove a connection

            elapsedTime = System.currentTimeMillis() - start;
            if (elapsedTime > 7000l) {
                System.out.println("Can't run test becase it took longer than 7 seconds to shut down derby: " + elapsedTime + "ms");
                /*
                 * Just return since the rest of the test will result in an unpredictable result. This should
                 * not occur during normal test runs. This timing issue should only occur
                 * when our test systems are too slow for various reason.
                 */
                testWasNotRun = true;
                return;
            }

            Thread.sleep(7000l - elapsedTime);
            System.out.println(System.currentTimeMillis() + ": about to attempt connection");

            // No connections should be left in pool. New request should succeed.
            ds5u.getConnection().close();
            System.out.println(System.currentTimeMillis() + ": good connection obtained");
        } catch (Throwable x) {
            testWasNotRun = true;
            throw x;
        } finally {
            if (testWasNotRun) {
                // Ensure there are no bad connections left in the pool
                for (int i = 1; i <= 3; i++) {
                    try {
                        ds5u.getConnection().close();
                        System.out.println("clean up connection " + i + " good");
                    } catch (Throwable t) {
                        System.out.println("clean up connection " + i + " bad");
                    }
                }
                // We might have invalidated connections in pools for data sources 6, 7, 8, 9, 10, 11 - all of which
                // use this same database that we have shut down in order to test bad connections for data source 5.
                // Sleep for 10 seconds to allow these bad connections to time out from the pools for those data sources.
                Thread.sleep(10000);
            }
        }
    }

    /**
     * Use a non-transactional data source.
     */
    public void testNonTransactional() throws Throwable {
        clearTable(ds3);
        PreparedStatement pstmt;
        Connection con = ds3.getConnection();
        try {
            // Non-transactional data source should ignore global tran.commit.
            con.setAutoCommit(false);
            tran.begin();
            try {
                pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
                pstmt.setString(1, "Grand Marais");
                pstmt.setInt(2, 1351);
                pstmt.setString(3, "Cook");
                pstmt.executeUpdate();

                tran.commit();
            } catch (Throwable x) {
                try {
                    tran.rollback();
                } catch (Throwable t) {
                }
                throw x;
            }
            con.rollback();
            // If the connection rollback wasn't honored, we will see a duplicate key error when we try to insert again.

            // Non-transactional data source should ignore global tran.rollback
            tran.begin();
            try {
                pstmt.setString(1, "Grand Marais");
                pstmt.setInt(2, 1351);
                pstmt.setString(3, "Cook");
                pstmt.executeUpdate();
            } finally {
                tran.rollback();
            }
            con.commit();

            // Verify the connection commit
            con.setAutoCommit(true);
            pstmt = con.prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Grand Marais");
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Entry is missing from database.");
            String county = result.getString(3);
            if (!"Cook".equals(county))
                throw new Exception("Incorrect data: " + county);
        } finally {
            con.close();
        }
    }

    /**
     * Verify than unresolved database transactions are rolled back by default.
     */
    public void testNonTransactionalCleanup() throws Throwable {
        clearTable(ds3);
        Connection con = ds3.getConnection();
        try {
            con.setAutoCommit(false);
            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Lake City");
            pstmt.setInt(2, 5063);
            pstmt.setString(3, "Wabasha");
            pstmt.executeUpdate();
        } finally {
            con.close();
        }

        // end the LTC and get a new one
        tran.begin();
        tran.commit();

        con = ds3.getConnection();
        try {
            con.setAutoCommit(true);
            PreparedStatement pstmt = con.prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Lake City");
            ResultSet result = pstmt.executeQuery();
            if (result.next())
                throw new Exception("Connection was not rolled back.");
        } finally {
            con.close();
        }
    }

    /**
     * Use multiple connections from a non-transactional data source.
     */
    public void testNonTransactionalMultipleConnections() throws Throwable {
        clearTable(ds3);
        Connection[] cons = new Connection[2];
        try {
            tran.begin();
            try {
                cons[1] = ds3.getConnection();
                cons[1].setAutoCommit(false);
                PreparedStatement pstmt1 = cons[1].prepareStatement("update cities set county = ? where name = ?");

                cons[0] = ds3.getConnection();
                PreparedStatement pstmt0 = cons[0].prepareStatement("insert into cities values (?, ?, ?)");

                int isolation = cons[0].getTransactionIsolation();
                int differentIsolation = getDifferentIsolationIfSupported(cons[0]);
                cons[1].setTransactionIsolation(differentIsolation);
                int value = cons[0].getTransactionIsolation();
                if (value != isolation)
                    throw new Exception("Update to con1 isolation level should not show up on con0.");

                pstmt0.setString(1, "Faribault");
                pstmt0.setInt(2, 23352);
                pstmt0.setString(3, "Rice");
                pstmt0.executeUpdate();

                pstmt1.setString(1, "");
                pstmt1.setString(2, "Faribault");
                pstmt1.executeUpdate();

                cons[1].rollback();
            } finally {
                tran.rollback();
            }

            // Verify the autocommit of con0, and the rollback of con1
            PreparedStatement pstmt = cons[0].prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Faribault");
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Entry is missing from database.");
            String county = result.getString(3);
            if (!"Rice".equals(county))
                throw new Exception("Incorrect data: " + county);
        } finally {
            for (Connection con : cons)
                if (con != null)
                    try {
                        con.close();
                    } catch (SQLException x) {
                    }
        }
    }

    /**
     * Test that validation is turned on, and that validation intervenes when getting an
     * existing bad connection out of the pool.
     *
     * Note that this will not test the actual duration of the validationTimeout,
     * which is more of an effort made by the JDBC driver than a hard limit.
     */
    @SuppressWarnings("deprecation")
    public void testValTimeoutTable1() throws Throwable {
        DataSource ds = InitialContext.doLookup("jdbc/dsValTderby");
        tran.begin();
        try {
            ds.getConnection().close();
        } finally {
            tran.commit();
        }
        // shut down Derby
        org.apache.derby.jdbc.EmbeddedDataSource40 derbyds = new org.apache.derby.jdbc.EmbeddedDataSource40();
        derbyds.setDatabaseName("memory:dsValTderby");
        derbyds.setShutdownDatabase("shutdown");
        try {
            derbyds.getConnection().close();
            throw new Exception("Failed to shut down Derby database: memory:dsValTderby");
        } catch (SQLException x) {
            // expected for shutdown
            System.out.println(System.currentTimeMillis() + ": Derby shutdown result: " + x.getMessage());
        }
        Connection conn = ds.getConnection();
        try {
            // Validation should be turned on, so the connection in the pool will be found to be bad,
            // and will be discarded in favor of a new one; no exception should be thrown
            conn.createStatement().executeQuery("values current_time");
        } finally {
            conn.close();
        }
    }

    /**
     * Test that validation is turned off, and the pool allows a bad connection
     * to be retrieved from the connection pool.
     */
    @SuppressWarnings("deprecation")
    public void testValNoTimeoutTable1() throws Throwable {
        DataSource ds = InitialContext.doLookup("jdbc/dsValTderby");
        tran.begin();
        try {
            ds.getConnection().close();
        } finally {
            tran.commit();
        }
        // shut down Derby
        org.apache.derby.jdbc.EmbeddedDataSource40 derbyds = new org.apache.derby.jdbc.EmbeddedDataSource40();
        derbyds.setDatabaseName("memory:dsValTderby");
        derbyds.setShutdownDatabase("shutdown");
        try {
            derbyds.getConnection().close();
            throw new Exception("Failed to shut down Derby database: memory:dsValTderby");
        } catch (SQLException x) {
            // expected for shutdown
            System.out.println(System.currentTimeMillis() + ": Derby shutdown result: " + x.getMessage());
        }

        try {
            Connection conn = ds.getConnection();
            try {
                conn.createStatement().executeQuery("values current_time");
            } finally {
                conn.close();
            }
            throw new Exception("Expected SQLException because validation is turned off");
        } catch (SQLException e) {
            // Because validation is turned off, the connection pool should allow a bad connection
            // to be handed out to the app. This exception is expected, so do nothing with it.
        }
    }

    /**
     * Test validationTimeout in a datasource defined by the app in annotations
     */
    @SuppressWarnings("deprecation")
    public void testValTimeoutAnnotation() throws Throwable {
        tran.begin();
        try {
            dsValTderbyAnn.getConnection().close();
        } finally {
            tran.commit();
        }

        // shut down Derby
        org.apache.derby.jdbc.EmbeddedDataSource40 derbyds = new org.apache.derby.jdbc.EmbeddedDataSource40();
        derbyds.setDatabaseName("memory:dsValTderbyAnn");
        derbyds.setShutdownDatabase("shutdown");
        try {
            derbyds.getConnection().close();
            throw new Exception("Failed to shut down Derby database: memory:dsValTderbyAnn");
        } catch (SQLException x) {
            // expected for shutdown
            System.out.println(System.currentTimeMillis() + ": Derby shutdown result: " + x.getMessage());
        }

        Connection conn = dsValTderbyAnn.getConnection();
        try {
            // Validation should be turned on, so the connection in the pool will be found to be bad,
            // and will be discarded in favor of a new one; no exception should be thrown
            conn.createStatement().executeQuery("values current_time");
        } finally {
            conn.close();
        }
    }

    /**
     * Test that onConnect attribute creates TEMP1 temporary table.
     */
    public void testOnConnectTable1() throws Throwable {
        Connection con = ds5.getConnection();
        try {
            int updateCount = con.createStatement().executeUpdate("insert into SESSION.TEMP1 values ('TEMP1 table should have been created by onConnect SQL.')");
            if (updateCount != 1)
                throw new Exception("Expected to insert 1 entry into TEMP1 table. Instead: " + updateCount);
        } finally {
            con.close();
        }
    }

    /**
     * Test that onConnect attribute does not create TEMP1 temporary table.
     */
    public void testOnConnectTable1NotFound() throws Throwable {
        Connection con = ds5.getConnection();
        try {
            con.createStatement().executeUpdate("insert into SESSION.TEMP1 values ('TEMP1 table should have been created by onConnect SQL.')");
            throw new Exception("TEMP1 table should not exist");
        } catch (SQLSyntaxErrorException x) {
            if (x.getMessage() == null || !x.getMessage().contains("SESSION.TEMP1"))
                throw x;
        } finally {
            con.close();
        }
    }

    /**
     * Test onConnect attribute that creates TEMP2 temporary table.
     */
    public void testOnConnectTable2() throws Throwable {
        Connection con = ds5.getConnection();
        try {
            int updateCount = con.createStatement().executeUpdate("insert into SESSION.TEMP2 values ('TEMP2 table should have been created by onConnect SQL.')");
            if (updateCount != 1)
                throw new Exception("Expected to insert 1 entry into TEMP2 table. Instead: " + updateCount);
        } finally {
            con.close();
        }
    }

    /**
     * Test that onConnect attribute does not create TEMP2 temporary table.
     */
    public void testOnConnectTable2NotFound() throws Throwable {
        Connection con = ds5.getConnection();
        try {
            con.createStatement().executeUpdate("insert into SESSION.TEMP2 values ('TEMP2 table should have been created by onConnect SQL.')");
            throw new Exception("TEMP2 table should not exist");
        } catch (SQLException x) {
            if (x.getMessage() == null || !x.getMessage().contains("SESSION.TEMP2"))
                throw x;
        } finally {
            con.close();
        }
    }

    /**
     * Enlist a single two-phase capable resource in a transaction.
     * The transaction manager can use the one-phase optimization (skip prepare) when we commit.
     */
    public void testOnePhaseOptimization() throws Throwable {
        clearTable(ds2);
        Connection con;

        tran.begin();
        try {
            con = ds2.getConnection();
            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Zumbro Falls");
            pstmt.setInt(2, 207);
            pstmt.setString(3, "Wabasha");
            pstmt.executeUpdate();
            pstmt.close();

            tran.commit();
        } catch (Throwable x) {
            try {
                tran.rollback();
            } catch (Throwable t) {
            }
            throw x;
        }

        try {
            PreparedStatement pstmt = con.prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Zumbro Falls");
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Entry missing from database.");
        } finally {
            con.close();
        }
    }

    /**
     * Use a data source/connection manager with some configuration errors and verify that the
     * onError=FAIL causes a failure.
     */
    public void testOnErrorFAIL() throws Exception {
        try {
            DataSource ds11 = (DataSource) new InitialContext().lookup("jdbc/dsfat11");
            Connection con = ds11.getConnection();
            con.close();
            throw new Exception("Expecting an error to be raised for bad configuration when onError=FAIL");
        } catch (Exception x) {
            // Pass - we wanted a failure
        }
    }

    /**
     * Use a data source/connection manager with some configuration errors and verify that the
     * onError=IGNORE causes the config error to be ignored.
     */
    public void testOnErrorIGNORE() throws Exception {
        DataSource ds11 = (DataSource) new InitialContext().lookup("jdbc/dsfat11");
        Connection con = ds11.getConnection();
        try {
            int isolationLevel = con.getTransactionIsolation();
            if (isolationLevel != Connection.TRANSACTION_READ_UNCOMMITTED)
                throw new Exception("Expecting isolationLevel=1, not " + isolationLevel);

            Statement stmt = con.createStatement();
            int queryTimeout = stmt.getQueryTimeout();
            if (queryTimeout != 11)
                throw new Exception("Expecting queryTimeout=11, not " + queryTimeout);
        } finally {
            con.close();
        }
    }

    /**
     * Use a data source with some configuration errors and verify that the
     * ignore/warn/fail setting (which defaults to warn) outputs the correct warnings.
     * ConfigTest contains the code to check the logs.
     */
    public void testOnErrorWARN() throws Exception {
        clearTable(ds5); // ensure the other data source (dsfat5) is used first

        tran.setTransactionTimeout(60);
        try {
            tran.begin();
            try {
                DataSource ds11 = (DataSource) new InitialContext().lookup("jdbc/dsfat11");
                Connection con = ds11.getConnection();
                int isolationLevel = con.getTransactionIsolation();
                if (isolationLevel != Connection.TRANSACTION_READ_UNCOMMITTED)
                    throw new Exception("Incorrection isolation level: " + isolationLevel);

                Statement stmt = con.createStatement();
                ResultSet result = stmt.executeQuery("values current_time");
                if (!result.next())
                    throw new Exception("Empty result.");

                int timeout = stmt.getQueryTimeout();
                if (timeout != 11)
                    throw new Exception("Incorrect query timeout: " + timeout);

                con.close();
            } finally {
                tran.commit();
            }
        } finally {
            tran.setTransactionTimeout(0); // restore default
        }
    }

    /**
     * Use the queryTimeout and syncQueryTimeoutWithTransactionTimeout properties.
     */
    public void testQueryTimeout() throws Throwable {

        int timeout;
        Connection con = ds5u.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("values sqrt(?)");

            timeout = pstmt.getQueryTimeout();
            if (timeout != 30)
                throw new Exception("Default query timeout not honored for new statement. Instead: " + timeout);

            pstmt.setQueryTimeout(20);
            timeout = pstmt.getQueryTimeout();
            if (timeout != 20)
                throw new Exception("Update of query timeout not honored. Instead: " + timeout);

            // cache it and get it back
            pstmt.close();
            pstmt = con.prepareStatement("values sqrt(?)");

            timeout = pstmt.getQueryTimeout();
            if (timeout != 30)
                throw new Exception("Default query timeout not honored for cached statement. Instead: " + timeout);

            CallableStatement cstmt = con.prepareCall("values current_date");
            int cstmtQueryTimeout = cstmt.getQueryTimeout();
            if (cstmtQueryTimeout != 30)
                throw new Exception("Default query timeout not honored for callable statement. Instead: " + cstmtQueryTimeout);
            cstmt.setQueryTimeout(40);

            tran.setTransactionTimeout(25);
            try {
                tran.begin();
                try {
                    pstmt.setDouble(1, 2.0);
                    pstmt.executeQuery();

                    timeout = pstmt.getQueryTimeout();
                    if (timeout > 25 || timeout < 20)
                        throw new Exception("Query timeout not properly synced to tran timeout. Instead: " + timeout);

                    cstmt.executeQuery();
                    cstmtQueryTimeout = cstmt.getQueryTimeout();
                    if (cstmtQueryTimeout != 40)
                        throw new Exception("Did not honor query timeout explicitly set by app. Instead: " + cstmtQueryTimeout);

                    Thread.sleep(1100);

                    pstmt.setDouble(1, 5.0);
                    pstmt.executeQuery();

                    int prevTimeout = timeout;
                    timeout = pstmt.getQueryTimeout();
                    if (timeout >= prevTimeout || timeout < 15)
                        throw new Exception("Query timeout not properly synced to tran timeout. Instead: " + timeout + " (previous timeout was: " + prevTimeout + ")");
                } finally {
                    tran.commit();
                }
            } finally {
                tran.setTransactionTimeout(0); // restore default
            }

            pstmt.setDouble(1, 7.0);
            pstmt.executeQuery();

            timeout = pstmt.getQueryTimeout();
            if (timeout != 30)
                throw new Exception("Default query timeout not restored after transaction. Instead: " + timeout);
        } finally {
            con.close();
        }
    }

    /**
     * Verify MatchCurrentState between a connection with isolation level
     * set to TRANSACTION_READ_UNCOMMITTED by the application and a new
     * connection request with TRANSACTION_READ_UNCOMMITTED from the resource ref.
     */
    public void testResourceRefIsolationLevel() throws Throwable {
        clearTable(ds6);

        tran.begin();
        try {
            // Set a non-default isolation level
            Connection con0 = ds6.getConnection();
            con0.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            PreparedStatement pstmt = con0.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Maplewood");
            pstmt.setInt(2, 38000);
            pstmt.setString(3, "Ramsey");
            pstmt.executeUpdate();
            pstmt.close();

            // Data source is only one-phase capable so the second connection handle is
            // only possible if both handles share the same connection.
            Connection con1 = ds6_1.getConnection(); // resource-ref has isolation-level=TRANSACTION_READ_UNCOMMITTED
            pstmt = con1.prepareStatement("update cities set size=? where name=?");
            pstmt.setString(1, "Maplewood");
            pstmt.setInt(2, 38018);
            int updateCount = pstmt.executeUpdate();

            if (updateCount != 1)
                throw new Exception("Should have updated 1 entry in database, not " + updateCount);

            int isolation = con1.getTransactionIsolation();
            if (isolation != Connection.TRANSACTION_READ_UNCOMMITTED)
                throw new Exception("Isolation level from resource ref not honored. Instead: " + isolation);

            tran.commit();
        } catch (Throwable x) {
            try {
                tran.rollback();
            } catch (Throwable t) {
            }
            throw x;
        }
    }

    /**
     * Use ResultSetMetaData.
     */
    public void testResultSetMetaData() throws Exception {
        clearTable(ds2);

        Connection con = ds2.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Pine Island");
            pstmt.setInt(2, 3263);
            pstmt.setString(3, "Goodhue");
            pstmt.executeUpdate();
            pstmt.close();

            pstmt = con.prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Pine Island");
            ResultSet result = pstmt.executeQuery();
            ResultSetMetaData metadata = result.getMetaData();

            int numColumns;
            try {
                numColumns = metadata.getColumnCount();
            } catch (SQLFeatureNotSupportedException x) {
                numColumns = 3;
            }

            String values = "";
            while (result.next())
                for (int i = 1; i <= numColumns; i++)
                    values += result.getObject(i) + " ";

            if (values.indexOf("Pine Island") < 0 || values.indexOf("3263") < 0 || values.indexOf("Goodhue") < 0)
                throw new Exception("Incorrect data (" + values + ") for " + numColumns + " columns of table");
        } finally {
            con.close();
        }
    }

    /**
     * Run a test to check if DataSource de/serializes correctly.
     */
    public void testSerialization() throws Exception {
        clearTable(ds1);

        DataSource testSource = ds1;
        Connection con = testSource.getConnection();
        ObjectOutputStream output = null;
        ByteArrayOutputStream outByteStream = null;
        ResultSet results = null;
        Statement stmt = null;

        try {
            stmt = con.createStatement();
            stmt.executeUpdate("INSERT INTO cities VALUES('Mansfield',1072,'Scott')");

            results = stmt.executeQuery("SElECT county FROM cities WHERE name = 'Mansfield'");

            if (!results.next())
                throw new Exception("Entry not found in database");

            try {
                outByteStream = new ByteArrayOutputStream();
                output = new ObjectOutputStream(outByteStream);
                output.writeObject(testSource);
                throw new Exception("expected NotSerializableException");
            } catch (NotSerializableException e) {
                System.out.println(System.currentTimeMillis() + ": caught expected " + e);
            }
        } finally {
            if (con != null)
                con.close();
        }
    }

    /**
     * Serial reuse of one-phase capable connection in a global transaction.
     */
    public void testSerialReuseInGlobalTran() throws Throwable {
        clearTable(ds0);
        tran.begin();
        try {
            Connection con = ds0.getConnection();
            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Bemidji");
            pstmt.setInt(2, 13431);
            pstmt.setString(3, "");
            pstmt.executeUpdate();
            con.close();

            // Serial reuse of connection
            con = ds0.getConnection();
            pstmt = con.prepareStatement("update cities set county=? where name=?");
            pstmt.setString(1, "Beltrami");
            pstmt.setString(2, "Bemidji");
            pstmt.executeUpdate();
            con.close();

            tran.commit();
        } catch (Throwable x) {
            try {
                tran.rollback();
            } catch (Throwable t) {
            }
            throw x;
        }
    }

    /**
     * Serial reuse of shared connection in an LTC
     */
    public void testSerialReuseInLTC() throws Throwable {
        clearTable(ds1);
        Connection con = ds1.getConnection();
        try {
            con.setAutoCommit(false);
            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Brooklyn Park");
            pstmt.setInt(2, 75781);
            pstmt.setString(3, "Hennepin");
            pstmt.executeUpdate();
            pstmt.close();
            con.close();

            // Serial reuse should happen for sharable connections
            con = ds1.getConnection();
            pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Plymouth");
            pstmt.setInt(2, 70576);
            pstmt.setString(3, "Hennepin");
            pstmt.executeUpdate();
            pstmt.close();
            con.commit();

            pstmt = con.prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Brooklyn Park");
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("First entry not found in database.");

            pstmt.setString(1, "Plymouth");
            result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Second entry not found in database.");

        } finally {
            try {
                con.setAutoCommit(true);
            } catch (Throwable t) {
                throw t;
            } finally {
                con.close();
            }
        }
    }

    /**
     * Verify that sharable handles are reassociated across transaction boundaries.
     */
    public void testSharableHandleReassociation() throws Throwable {
        clearTable(ds1);
        Connection[] cons = new Connection[2];
        try {
            cons[0] = ds1.getConnection();
            PreparedStatement pstmt0 = cons[0].prepareStatement("insert into cities values (?, ?, ?)");
            pstmt0.setString(1, "International Falls");
            pstmt0.setInt(2, 6424);
            pstmt0.setString(3, "");
            pstmt0.executeUpdate();

            cons[1] = ds1.getConnection();
            Statement stmt1 = cons[1].createStatement();
            ResultSet result1 = stmt1.executeQuery("select population from cities where name='International Falls'");

            tran.begin();
            try {
                if (!pstmt0.isClosed())
                    throw new Exception("Sharable connection's PreparedStatement should be closed after tran.begin.");

                if (!stmt1.isClosed())
                    throw new Exception("Sharable connection's Statement should be closed after tran.begin.");

                if (!result1.isClosed())
                    throw new Exception("Sharable connection's ResultSet should be closed after tran.begin.");

                // Inactive connection handle should associate with managed connection upon first use
                pstmt0 = cons[0].prepareStatement("update cities set county=? where name=?");
                pstmt0.setString(1, "Koochiching");
                pstmt0.setString(2, "International Falls");
                pstmt0.executeUpdate();

                // Other inactive connection handle should associate with same managed connection.
                // (If it doesn't then transaction will fail with two one-phase resources)
                stmt1 = cons[1].createStatement();
                result1 = stmt1.executeQuery("select county from cities where name='International Falls'");
                if (!result1.next())
                    throw new Exception("Entry missing from database");
                String county = result1.getString(1);
                if (!"Koochiching".equals(county))
                    throw new Exception("Update not honored. Instead: " + county);

                tran.commit();
            } catch (Throwable x) {
                try {
                    tran.rollback();
                } catch (Throwable t) {
                }
                throw x;
            }

            if (!pstmt0.isClosed())
                throw new Exception("Sharable connection's PreparedStatement should be closed after tran.commit.");

            if (!stmt1.isClosed())
                throw new Exception("Sharable connection's Statement should be closed after tran.commit.");

            if (!result1.isClosed())
                throw new Exception("Sharable connection's ResultSet should be closed after tran.commit.");

            // Inactive connections should be usable again
            pstmt0 = cons[0].prepareStatement("insert into cities values (?, ?, ?)");
            pstmt0.setString(1, "Littlefork");
            pstmt0.setInt(2, 647);
            pstmt0.setString(3, "");
            pstmt0.executeUpdate();

            PreparedStatement pstmt1 = cons[1].prepareStatement("update cities set county=? where name=?");
            pstmt1.setString(1, "Koochiching");
            pstmt1.setString(2, "Littlefork");
            pstmt1.executeUpdate();

            // Reassociate into another global transaction
            tran.begin();
            try {
                if (!pstmt0.isClosed())
                    throw new Exception("Sharable con0's PreparedStatement should be closed after tran.begin.");

                if (!pstmt1.isClosed())
                    throw new Exception("Sharable con1's PreparedStatement should be closed after tran.begin.");

                pstmt0 = cons[0].prepareStatement("insert into cities values (?, ?, ?)");
                pstmt0.setString(1, "Big Falls");
                pstmt0.setInt(2, 236);
                pstmt0.setString(3, "");
                pstmt0.executeUpdate();

                pstmt1 = cons[1].prepareStatement("update cities set county=? where name=?");
                pstmt1.setString(1, "Koochiching");
                pstmt1.setString(2, "Big Falls");
                pstmt1.executeUpdate();

                tran.commit();
            } catch (Throwable x) {
                try {
                    tran.rollback();
                } catch (Throwable t) {
                }
                throw x;
            }

            if (!pstmt0.isClosed())
                throw new Exception("Sharable con0's PreparedStatement should be closed after tran.commit.");

            if (!pstmt1.isClosed())
                throw new Exception("Sharable con1's PreparedStatement should be closed after tran.commit.");

            // Reassociate for the last time
            cons[0].setAutoCommit(false);
            pstmt0 = cons[0].prepareStatement("select name, population, county from cities where name = ?");
            pstmt0.setString(1, "Littlefork");
            ResultSet result0 = pstmt0.executeQuery();
            if (!result0.next())
                throw new Exception("Second entry missing from database.");
            int size = result0.getInt(2);
            if (size != 647)
                throw new Exception("Incorrect value in second entry: " + size);
            cons[0].setAutoCommit(true);

            cons[1].setAutoCommit(false);
            pstmt1 = cons[1].prepareStatement("select county from cities where name = ?");
            pstmt1.setString(1, "Big Falls");
            result1 = pstmt1.executeQuery();
            if (!result1.next())
                throw new Exception("Third entry missing from database.");
            String county = result1.getString(1);
            if (!"Koochiching".equals(county))
                throw new Exception("Incorrect value in third entry: " + county);
            cons[1].commit();
        } finally {
            for (Connection con : cons)
                if (con != null)
                    try {
                        con.close();
                    } catch (SQLException x) {
                    }
        }
    }

    /**
     * Share a connection in a global transaction.
     */
    public void testSharingInGlobalTran() throws Throwable {
        clearTable(ds0);
        tran.begin();
        try {
            Connection con0 = ds0.getConnection();
            PreparedStatement pstmt = con0.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Brainerd");
            pstmt.setInt(2, 13590);
            pstmt.setString(3, "");
            pstmt.executeUpdate();

            // Multiple connection handles are only possible for one-phase resource if
            // they share the same connection.
            Connection con1 = ds0.getConnection();
            pstmt = con1.prepareStatement("update cities set county=? where name=?");
            pstmt.setString(1, "Crow Wing");
            pstmt.setString(2, "Brainerd");
            pstmt.executeUpdate();

            con0.close();
            con1.close();

            tran.commit();
        } catch (Throwable x) {
            try {
                tran.rollback();
            } catch (Throwable t) {
            }
            throw x;
        }
    }

    /**
     * Verify that cached statements are properly cleaned up.
     */
    public void testStatementCleanup() throws Exception {
        clearTable(ds1);
        Connection con = ds1.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Austin");
            pstmt.setInt(2, 24718);
            pstmt.setString(3, "Mower");

            // Cache the statement
            pstmt.setPoolable(true);
            pstmt.close();

            // Retrieve it from the cache
            pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");

            // Parameters should be cleared.
            if (!con.getMetaData().getDatabaseProductName().toUpperCase().contains("ADAPTIVE SERVER")) // skip if Sybase
                try {
                    pstmt.executeUpdate();
                    throw new Exception("Should not be able to use statement from cache without setting any parameters.");
                } catch (SQLException x) {
                }

        } finally {
            con.close();
        }
    }

    /**
     * Unsharable data source should allow statements to remain open across transactions.
     */
    public void testStatementsAcrossTranBoundaries() throws Throwable {
        clearTable(ds1u);
        Connection con = ds1u.getConnection();
        try {
            Statement stmt = con.createStatement();

            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Minneapolis");
            pstmt.setInt(2, 382578);
            pstmt.setString(3, "Hennepin");
            pstmt.executeUpdate();

            tran.begin();
            try {
                pstmt.setString(1, "Saint Paul");
                pstmt.setInt(2, 285068);
                pstmt.setString(3, "Ramsey");
                pstmt.executeUpdate();
                tran.commit();
            } catch (Throwable x) {
                try {
                    tran.rollback();
                } catch (Throwable t) {
                }
                throw x;
            }

            con.setAutoCommit(false);
            pstmt.setString(1, "Madison");
            pstmt.setInt(2, 233209);
            pstmt.setString(3, "Dane");
            pstmt.executeUpdate();
            con.rollback();
            con.setAutoCommit(true);

            if (pstmt.isClosed())
                throw new Exception("Statement from unsharable connection should still be open after rollback.");
            pstmt.clearParameters();
            pstmt.setPoolable(true);
            pstmt.close();

            ResultSet result = stmt.executeQuery("select sum (population) from cities");
            if (!result.next())
                throw new Exception("Missing entries from database.");
            int sum = result.getInt(1);
            if (sum != 667646)
                throw new Exception("Expecting value of 667646, not " + sum);
        } finally {
            con.close();
        }
    }

    public void testTestConnectionTimerNotRunning() throws Exception {
        // create the Derby database
        @SuppressWarnings("deprecation")
        org.apache.derby.jdbc.EmbeddedDataSource40 derbyds = new org.apache.derby.jdbc.EmbeddedDataSource40();
        derbyds.setDatabaseName("memory:dstct");
        derbyds.setCreateDatabase("create");
        derbyds.getConnection().close();

        // prereq: test case needs to dynamically create the data source that this points to in server config
        DataSource ds = InitialContext.doLookup("java:module/env/jdbc/ds-tct");

        // fill the pool
        Connection con1 = ds.getConnection();
        try {
            Connection con2 = ds.getConnection();
            try {
                ds.getConnection().close();

                // with 2 connections still in use and 1 in the free pool, drop the database
                derbyds.setCreateDatabase("false");
                derbyds.setDatabaseName(derbyds.getDatabaseName() + ";drop=true");
                try {
                    derbyds.getConnection().close();
                    throw new Exception("Failed to shut down Derby database");
                } catch (SQLException x) {
                    // expected for shutdown
                    System.out.println("Derby shutdown result: " + x.getMessage());
                }

                // Expect an initial failure after shutting down Derby
                try {
                    con2.createStatement().execute("values ('this should fail due to a bad connection')");
                    throw new Exception("Should not be able to continue to use Derby connection after shutdown.");
                } catch (SQLNonTransientConnectionException x) {
                }
            } finally {
                con2.close();
            }
        } finally {
            con1.close();
        }

        // Expect error indicating the database isn't found
        try {
            ds.getConnection().close();
        } catch (SQLException x) {
            boolean foundXJ004 = false;
            for (SQLException c = x; !foundXJ004 && c != null; c = c.getNextException())
                foundXJ004 |= "XJ004".equals(c.getSQLState());
            if (!foundXJ004)
                throw x;
        }

        // See if the test connection timer is going to run. If it does, errors will be reported in the logs,
        // causing the test to report failures when it checks the logs after shutting down the server.
        Thread.sleep(2000);

        // Expect error indicating the database isn't found
        try {
            ds.getConnection().close();
        } catch (SQLException x) {
            boolean foundXJ004 = false;
            for (SQLException c = x; !foundXJ004 && c != null; c = c.getNextException())
                foundXJ004 |= "XJ004".equals(c.getSQLState());
            if (!foundXJ004)
                throw x;
        }

        // Create the database again, connections should start working after this
        derbyds.setDatabaseName("memory:dstct");
        derbyds.setCreateDatabase("create");
        derbyds.getConnection().close();

        ds.getConnection().close();
    }

    /**
     * Use two-phase commit.
     */
    public void testTwoPhaseCommit() throws Throwable {
        clearTable(ds4);
        Connection con1, con2;

        // Commit some updates to the database
        tran.begin();
        try {
            con1 = ds4.getConnection();
            PreparedStatement pstmt = con1.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Wanamingo");
            pstmt.setInt(2, 1086);
            pstmt.setString(3, "Goodhue");
            pstmt.executeUpdate();
            pstmt.close();

            con2 = ds20.getConnection();
            pstmt = con2.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Waseca");
            pstmt.setInt(2, 9410);
            pstmt.setString(3, "Waseca");
            pstmt.executeUpdate();
            pstmt.close();

            tran.commit();
        } catch (Throwable x) {
            try {
                tran.rollback();
            } catch (Throwable t) {
            }
            throw x;
        }

        // Roll back some updates
        tran.begin();
        try {
            con1 = ds4.getConnection();
            PreparedStatement pstmt = con1.prepareStatement("update cities set population = ? where name = ?");
            pstmt.setInt(1, 1000);
            pstmt.setString(2, "Wanamingo");
            pstmt.executeUpdate();
            pstmt.close();

            con2 = ds2.getConnection();
            pstmt = con2.prepareStatement("update cities set population = ? where name = ?");
            pstmt.setInt(1, 9000);
            pstmt.setString(2, "Waseca");
            pstmt.executeUpdate();
            pstmt.close();
            con2.close();
        } finally {
            tran.rollback();
        }

        try {
            PreparedStatement pstmt = con1.prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Wanamingo");
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("First entry missing from database.");
            int size = result.getInt(2);
            if (size != 1086)
                throw new Exception("Incorrect value in first entry: " + size);

            pstmt.setString(1, "Waseca");
            result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Second entry missing from database.");
            size = result.getInt(2);
            if (size != 9410)
                throw new Exception("Incorrect value in second entry: " + size);
        } finally {
            con1.close();
        }
    }

    /**
     * Have two database local transactions active at the same time.
     * Roll one back and commit the other.
     */
    public void testTwoTransactions() throws Exception {
        clearTable(ds1);
        Connection[] cons = new Connection[2];
        try {
            cons[0] = ds1.getConnection();
            cons[1] = ds1.getConnection();
            Statement stmt0 = cons[0].createStatement();
            Statement stmt1 = cons[1].createStatement();
            stmt0.executeUpdate("insert into cities values ('Mankato', 39309, 'BlueEarth')");
            stmt0.executeUpdate("insert into cities values ('Owatonna', 25599, 'Steele')");

            cons[0].setAutoCommit(false);
            cons[1].setAutoCommit(false);

            stmt0.executeUpdate("update cities set county='Steele County' where name='Owatonna'");
            stmt1.executeUpdate("update cities set county='Blue Earth' where name='Mankato'");

            cons[0].rollback();
            cons[1].commit();

            cons[0].setAutoCommit(true);
            cons[1].setAutoCommit(true);

            PreparedStatement pstmt = cons[1].prepareStatement("select county from cities where name=?");
            pstmt.setPoolable(false);
            pstmt.setString(1, "Owatonna");
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Missing entry for rolled back update");
            String county = result.getString(1);
            if (!"Steele".equals(county))
                throw new Exception("Entry does not reflect rollback: " + county);

            pstmt.setString(1, "Mankato");
            result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Missing entry for committed update");
            county = result.getString(1);
            if (!"Blue Earth".equals(county))
                throw new Exception("Entry does not reflect commit: " + county);
        } finally {
            for (Connection con : cons)
                if (con != null)
                    try {
                        con.close();
                    } catch (SQLException x) {
                    }
        }
    }

    /**
     * Verify that sharable and unsharable connections don't share with eachother.
     */
    public void testUnsharable() throws Throwable {
        clearTable(ds1);
        Connection con = ds1.getConnection(); // sharable
        try {
            con.setAutoCommit(false);
            PreparedStatement pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "St. Cloud");
            pstmt.setInt(2, 65842);
            pstmt.setString(3, "Stearns");
            pstmt.executeUpdate();
            pstmt.close();
            con.close(); // LTC should remain open

            con = ds1u.getConnection(); // unsharable
            con.setAutoCommit(false);
            pstmt = con.prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Bloomington");
            pstmt.setInt(2, 82893);
            pstmt.setString(3, "Hennepin");
            pstmt.executeUpdate();
            pstmt.close();
            con.rollback(); // roll back the unsharable connection only
            con.close();

            con = ds1.getConnection(); // sharable
            con.setAutoCommit(true); // commit the LTC from before

            pstmt = con.prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "St. Cloud");
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Entry missing from database");

            pstmt.setString(1, "Bloomington");
            result = pstmt.executeQuery();
            if (result.next())
                throw new Exception("Second entry should have been rolled back");

        } finally {
            try {
                con.setAutoCommit(true);
            } catch (Throwable x) {
                throw x;
            } finally {
                con.close();
            }
        }
    }

    /**
     * Update a result set.
     */
    public void testUpdatableResult() throws Exception {
        clearTable(ds2);
        Connection con = ds2.getConnection();
        try {
            DatabaseMetaData metadata = con.getMetaData();
            if (!metadata.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
                System.out.println("skipped testUpdatableResult<br>");
                return; // can't run this test
            }

            String query = "select name, population, county from cities";
            PreparedStatement ps;
            ps = con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet result = ps.executeQuery();
            result.moveToInsertRow();
            result.updateString(1, "Duluth");
            result.updateInt(2, 86265);
            result.updateString(3, "");
            result.insertRow();
            ps.close();

            ps = con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            result = ps.executeQuery();
            if (!result.next())
                throw new Exception("Entry missing from database");
            String value = result.getString(1);
            if (!"Duluth".equals(value))
                throw new Exception("Incorrect value: " + value);

            result.updateString(3, "St. Louis");
            result.updateRow();
            ps.close();

            ps = con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            result = ps.executeQuery();
            if (!result.next())
                throw new Exception("Entry disappeared from database");
            value = result.getString(1);
            if (!"Duluth".equals(value))
                throw new Exception("Primary key should not have changed: " + value);
            value = result.getString(3);
            if (!"St. Louis".equals(value))
                throw new Exception("Value not updated correctly: " + value);
        } finally {
            con.close();
        }
    }

    /**
     * Use a the JDBC wrapper pattern.
     */
    public void testWrapperPattern() throws Exception {
        // ds6 is a Derby-only data source
        Connection con = ds6.getConnection();
        try {
            DatabaseMetaData mdata = con.getMetaData();
            String userName = mdata.getUserName();

            if (!con.isWrapperFor(EngineConnection.class))
                throw new Exception("WAS connection should be wrapper for " + EngineConnection.class.getName());

            EngineConnection engineCon = con.unwrap(EngineConnection.class);
            String schemaName = engineCon.getCurrentSchemaName();

            if (!userName.equalsIgnoreCase(schemaName))
                throw new Exception("User name and schema name are not equal: " + userName + ", " + schemaName);
        } finally {
            con.close();
        }
    }

    /**
     * Cause an in-doubt transaction and verify that XA recovery resolves it.
     * The recoveryAuthData should be used for recovery.
     */
    public void testXARecovery() throws Throwable {
        clearTable(ds4u_2);
        Connection[] cons = new Connection[3];
        tran.begin();
        try {
            // Use unsharable connections, so that they all get their own XA resources
            cons[0] = ds4u_2.getConnection();
            cons[1] = ds4u_2.getConnection();
            cons[2] = ds4u_2.getConnection();

            String dbProductName = cons[0].getMetaData().getDatabaseProductName().toUpperCase();
            System.out.println("Product Name is " + dbProductName);

            // Verify isolation-level="TRANSACTION_READ_COMMITTED" from ibm-web-ext.xml
            int isolation = cons[0].getTransactionIsolation();
            if (isolation != Connection.TRANSACTION_READ_COMMITTED)
                throw new Exception("The isolation-level of the resource-ref is not honored, instead: " + isolation);

            PreparedStatement pstmt;
            pstmt = cons[0].prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Edina");
            pstmt.setInt(2, 47941);
            pstmt.setString(3, "Hennepin");
            pstmt.executeUpdate();
            pstmt.close();

            pstmt = cons[1].prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "St. Louis Park");
            pstmt.setInt(2, 45250);
            pstmt.setString(3, "Hennepin");
            pstmt.executeUpdate();
            pstmt.close();

            pstmt = cons[2].prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Moorhead");
            pstmt.setInt(2, 38065);
            pstmt.setString(3, "Clay");
            pstmt.executeUpdate();
            pstmt.close();

            System.out.println("Intentionally causing in-doubt transaction");
            TestXAResource.assignSuccessLimit(1, cons);
            try {
                tran.commit();
                throw new Exception("Commit should not have succeeded because the test infrastructure is supposed to cause an in-doubt transaction.");
            } catch (HeuristicMixedException x) {
                TestXAResource.removeSuccessLimit(cons);
                System.out.println("Caught expected HeuristicMixedException: " + x.getMessage());
            }
        } catch (Throwable x) {
            TestXAResource.removeSuccessLimit(cons);
            try {
                tran.rollback();
            } catch (Throwable t) {
            }
            throw x;
        } finally {
            for (Connection con : cons)
                if (con != null)
                    try {
                        con.close();
                    } catch (Throwable x) {
                    }
        }

        // At this point, the transaction is in-doubt.
        // We won't be able to access the data until the transaction manager recovers
        // the transaction and resolves it.
        //
        // A connection configured with TRANSACTION_SERIALIZABLE is necessary in
        // order to allow the recovery to kick in before using the connection.

        System.out.println("attempting to access data (only possible after recovery)");
        Connection con = ds4u_8.getConnection();

        int isolation = con.getTransactionIsolation();
        if (isolation != Connection.TRANSACTION_SERIALIZABLE)
            throw new Exception("The isolation-level of the resource-ref is not honored, instead: " + isolation);
        try {
            ResultSet result;
            PreparedStatement pstmt = con.prepareStatement("select name, population, county from cities where name = ?");

            /*
             * Poll for results once a second for 5 seconds.
             * Most databases will have XA recovery done by this point
             *
             */
            List<String> cities = new ArrayList<>();
            for (int count = 0; cities.size() < 3 && count < 5; Thread.sleep(1000)) {
                if (!cities.contains("Edina")) {
                    pstmt.setString(1, "Edina");
                    result = pstmt.executeQuery();
                    if (result.next())
                        cities.add(0, "Edina");
                }

                if (!cities.contains("St. Louis Park")) {
                    pstmt.setString(1, "St. Louis Park");
                    result = pstmt.executeQuery();
                    if (result.next())
                        cities.add(1, "St. Louis Park");
                }

                if (!cities.contains("Moorhead")) {
                    pstmt.setString(1, "Moorhead");
                    result = pstmt.executeQuery();
                    if (result.next())
                        cities.add(2, "Moorhead");
                }
                count++;
                System.out.println("Attempt " + count + " to retrieve recovered XA data. Current status: " + cities);
            }

            if (cities.size() < 3)
                throw new Exception("Missing entry in database. Results: " + cities);
            else
                System.out.println("successfully accessed the data");
        } finally {
            con.close();
        }
    }

    /**
     * Cause an in-doubt transaction in Derby and verify that XA recovery resolves it.
     * The container managed auth alias should be used for recovery.
     */
    public void testXARecoveryContainerAuth() throws Throwable {
        DataSource ds10ref2 = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/dsfat10ref2");

        // This is a different user, so we can't use the default table.
        Connection con = ds10ref2.getConnection();
        try {
            Statement stmt = con.createStatement();
            try {
                stmt.executeUpdate("drop table iterations");
            } catch (SQLException x) {
            }
            stmt.executeUpdate("create table iterations (name varchar(10) not null primary key, startDate date, endDate date)");
        } finally {
            con.close();
        }

        Connection[] cons = new Connection[3];
        tran.begin();
        try {
            // Use unsharable connections, so that they all get their own XA resources
            cons[0] = ds10ref2.getConnection();
            cons[1] = ds10ref2.getConnection();
            cons[2] = ds10ref2.getConnection();

            // Verify that the correct user is used.
            String user = cons[0].getMetaData().getUserName();
            if (!"dbuser2".equalsIgnoreCase(user))
                throw new Exception("Should have user dbuser2 from container managed auth alias, not " + user);

            // Verify isolation-level="TRANSACTION_READ_COMMITTED" from ibm-web-ext.xml
            int isolation = cons[0].getTransactionIsolation();
            if (isolation != Connection.TRANSACTION_READ_COMMITTED)
                throw new Exception("The isolation-level of the resource-ref is not honored, instead: " + isolation);

            Statement stmt = cons[0].createStatement();
            stmt.executeUpdate("insert into iterations values ('M4.5', '2012-01-17', '2012-01-30')");
            stmt.close();

            PreparedStatement pstmt = cons[1].prepareStatement("insert into iterations values (?, ?, ?)");
            pstmt.setString(1, "M4.6");
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(2012, 1 - 1, 31);
            pstmt.setDate(2, new Date(cal.getTimeInMillis()));
            cal.set(2012, 2 - 1, 13);
            pstmt.setDate(3, new Date(cal.getTimeInMillis()));
            pstmt.executeUpdate();
            pstmt.setPoolable(false);
            pstmt.close();

            CallableStatement cstmt = cons[2].prepareCall("insert into iterations values (?, ?, ?)");
            cstmt.setString(1, "M5.1");
            cal.set(2012, 2 - 1, 14);
            cstmt.setDate(2, new Date(cal.getTimeInMillis()));
            cal.set(2012, 2 - 1, 27);
            cstmt.setDate(3, new Date(cal.getTimeInMillis()));
            cstmt.executeUpdate();
            cstmt.setPoolable(false);
            cstmt.close();

            System.out.println("Intentionally causing in-doubt transaction");
            TestXAResource.assignSuccessLimit(1, cons);
            try {
                tran.commit();
                throw new Exception("Commit should not have succeeded because the test infrastructure is supposed to cause an in-doubt transaction.");
            } catch (HeuristicMixedException x) {
                TestXAResource.removeSuccessLimit(cons);
                System.out.println("Caught expected HeuristicMixedException: " + x.getMessage());
            }
        } catch (Throwable x) {
            TestXAResource.removeSuccessLimit(cons);
            try {
                tran.rollback();
            } catch (Throwable t) {
            }
            throw x;
        } finally {
            for (Connection c : cons)
                if (c != null)
                    try {
                        c.close();
                    } catch (Throwable x) {
                    }
        }

        // At this point, the transaction is in-doubt.
        // We won't be able to access the data until the transaction manager recovers
        // the transaction and resolves it.

        System.out.println("attempting to access data (only possible after recovery)");
        con = ds10ref2.getConnection();
        try {
            ResultSet result;
            PreparedStatement pstmt = con.prepareStatement("select endDate from iterations where name = ?");

            pstmt.setString(1, "M4.5");
            result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Missing first entry in database.");

            pstmt.setString(1, "M4.6");
            result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Missing second entry in database.");

            pstmt.setString(1, "M5.1");
            result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Missing third entry in database.");
            Date date = result.getDate(1);
            if (!"2012-02-27".equals(date.toString()))
                throw new Exception("Database has wrong end date for last entry: " + date);

            System.out.println("successfully accessed the data");
        } finally {
            con.close();
        }
    }

    /**
     * Use multiple databases in an XA transaction.
     */
    public void testXAWithMultipleDatabases() throws Throwable {
        clearTable(ds5u);
        Connection con1 = ds5u.getConnection();
        try {
            // Commit updates to the database
            tran.begin();
            try {
                // Update Derby
                PreparedStatement pstmt = con1.prepareStatement("insert into cities values (?, ?, ?)");
                pstmt.setString(1, "Burnsville");
                pstmt.setInt(2, 60306);
                pstmt.setString(3, "Dakota");
                pstmt.executeUpdate();
                pstmt.close();

                // Update the other database
                Connection con2 = ds2.getConnection();
                try {
                    pstmt = con2.prepareStatement("insert into cities values (?, ?, ?)");
                    pstmt.setString(1, "Eden Prairie");
                    pstmt.setInt(2, 60797);
                    pstmt.setString(3, "Hennepin");
                    pstmt.executeUpdate();
                    pstmt.close();
                } finally {
                    con2.close();
                }

                tran.commit();
            } catch (Throwable x) {
                try {
                    tran.rollback();
                } catch (Throwable t) {
                }
                throw x;
            }

            // Roll back some updates
            tran.begin();
            try {
                PreparedStatement pstmt = con1.prepareStatement("update cities set population = ? where name = ?");
                pstmt.setInt(1, 60000);
                pstmt.setString(2, "Burnsville");
                pstmt.executeUpdate();
                pstmt.close();

                Connection con2 = ds2.getConnection();
                try {
                    pstmt = con2.prepareStatement("update cities set population = ? where name = ?");
                    pstmt.setInt(1, 60000);
                    pstmt.setString(2, "Eden Prairie");
                    pstmt.executeUpdate();
                    pstmt.close();
                } finally {
                    con2.close();
                }
            } finally {
                tran.rollback();
            }

            PreparedStatement pstmt = con1.prepareStatement("select name, population, county from cities where name = ?");
            pstmt.setString(1, "Burnsville");
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("First entry missing from database.");
            int size = result.getInt(2);
            if (size != 60306)
                throw new Exception("Incorrect value in first entry: " + size);
            pstmt.close();

            Connection con2 = ds2.getConnection();
            try {
                pstmt = con2.prepareStatement("select name, population, county from cities where name = ?");
                pstmt.setString(1, "Eden Prairie");
                result = pstmt.executeQuery();
                if (!result.next())
                    throw new Exception(className + ": Second entry missing from database.");
                size = result.getInt(2);
                if (size != 60797)
                    throw new Exception(className + ": Incorrect value in second entry: " + size);
            } finally {
                con2.close();
            }
        } finally {
            con1.close();
        }
    }

    /**
     * Test if a reap time of 0 is still unsupported.
     */
    public void testReapTimeUnsupportedValue() throws Throwable {
        InitialContext ctx = new InitialContext();

        try {
            DataSource ds = (DataSource) ctx.lookup("jdbc/dsfat21");
            fail("Lookup should have failed due to bad config.");
        } catch (Exception e) {
            assertTrue("Exception message should have contained", e.getMessage().contains("CWWKN0008E"));
        }
    }

    /**
     * Test if a aged timeout value of 0 causes pooling to be disabled.
     */
    public void testAgedTimeoutImmediate() throws Throwable {
        Connection cnt = ds22.getConnection();
        try {
            cnt.getMetaData();
            cnt.close();

            assertEquals("Connection should not be pooled.", 0, getPoolSize("jdbc/dsfat22"));
        } finally {
            cnt.close();
        }
    }

    /**
     * Test if a maxIdleTime of 0 is still unsupported.
     */
    public void testMaxIdleTimeUnsupportedValue() throws Throwable {
        InitialContext ctx = new InitialContext();

        try {
            DataSource ds = (DataSource) ctx.lookup("jdbc/dsfat23");
            fail("Lookup should have failed due to bad config.");

        } catch (Exception e) {
            assertTrue("Exception message should have contained", e.getMessage().contains("CWWKN0008E"));
        }
    }

    /**
     * Test that when connectionTimeout is set to a value of -1 (infinite) the connection
     * request waits for longer than the default timeout (30 seconds)
     */
    public void testConnectionTimeoutInfinite() throws Throwable {
        Connection con1 = ds22.getConnection();
        Connection con2 = null;
        try {
            con2 = ds22.getConnection(); // connection pool should now be full
            //Need to request the third connection async since the getConnection request should hang
            //until there is room in the pool
            Future<Boolean> future = executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    Connection con = ds22.getConnection();
                    try {
                        con.getMetaData();
                        return true;
                    } finally {
                        if (con != null)
                            con.close();
                    }
                }
            });
            //Wait 45 seconds, assume that the task will normally begin execution within 15 seconds
            //and then wait the 30 seconds for the default timeout
            try {
                boolean val = future.get(45, TimeUnit.SECONDS);
                fail("The task should not have completed, instead returned " + val);
            } catch (TimeoutException ex) {
                //expected
            }

            //Now try to close one of the connections, which should allow the other task to complete
            con2.close();

            assertTrue("The task should have returned true", future.get(5, TimeUnit.MINUTES));
        } finally {
            con1.close();
            con2.close();
        }

    }

    public void testInterruptedWaiters() throws Throwable {
        Connection con1 = ds22.getConnection();
        Connection con2 = null;
        try {
            con2 = ds22.getConnection(); // connection pool should now be full
            //Need to request the third connection async since the getConnection request should hang
            //until there is room in the pool
            Future<Boolean> future = executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    Connection con = ds22.getConnection();
                    try {
                        con.getMetaData();
                        return true;
                    } finally {
                        if (con != null)
                            con.close();
                    }
                }
            });
            //Wait 20 seconds to give time for the connection to get into the waiter state
            try {
                boolean val = future.get(20, TimeUnit.SECONDS);
                fail("The task should not have completed, instead returned " + val);
            } catch (TimeoutException ex) {
                //expected
            }

            //cancel the task which will result in the thread being interrupted
            future.cancel(true);

            assertTrue("Future should have been canceled", future.isCancelled());
        } finally {
            con1.close();
            con2.close();
        }
        //Both connections should now be closed (and the third request never actually got a connection).  There should be
        //0 connections in the pool now.  Polling the value in case the async task isn't interrupted and we need to wait for the
        //task to get and close its connection

        long timeout = TimeUnit.MINUTES.toMillis(3);
        int poolsize;
        for (long start = System.currentTimeMillis(); (poolsize = getPoolSize("jdbc/dsfat22")) != 0 && System.currentTimeMillis() - start < timeout; Thread.sleep(500));
        assertEquals("Expected there to be 0 connections in the pool.", 0, poolsize);
    }

    private ObjectInstance getMBeanObjectInstance(String jndiName) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,jndiName=" + jndiName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        if (s.size() != 1) {
            System.out.println("ERROR: Found incorrect number of MBeans (" + s.size() + ")");
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        return s.iterator().next();
    }

    private int getPoolSize(String jndiName) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectInstance bean = getMBeanObjectInstance(jndiName);
        System.out.println("Found " + bean.getObjectName().toString());
        String contents = (String) mbs.invoke(bean.getObjectName(), "showPoolContents", null, null);
        System.out.println("   " + contents.replace("\n", "\n   "));

        return Integer.parseInt((String) mbs.getAttribute(bean.getObjectName(), "size"));
    }
}
