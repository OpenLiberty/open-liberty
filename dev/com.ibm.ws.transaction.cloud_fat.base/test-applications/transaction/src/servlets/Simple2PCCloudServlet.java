/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package servlets;

import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.TxTestUtils;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

@SuppressWarnings("serial")
@WebServlet("/Simple2PCCloudServlet")
public class Simple2PCCloudServlet extends Base2PCCloudServlet {

    @Resource(name = "jdbc/derby", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds;
    @Resource(name = "jdbc/tranlogDataSource", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource dsTranLog;

    public void testLeaseTableAccess(HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {

        try (Connection con = getConnection(ds)) {
            con.setAutoCommit(false);

            // Statement used to drop table
            try (Statement stmt = con.createStatement()) {
                String selForUpdateString = "SELECT LEASE_OWNER" +
                                            " FROM WAS_LEASES_LOG" +
                                            " WHERE SERVER_IDENTITY='cloud0011' FOR UPDATE OF LEASE_OWNER";
                System.out.println("testLeaseTableAccess: " + selForUpdateString);
                ResultSet rs = stmt.executeQuery(selForUpdateString);
                String owner = null;
                while (rs.next()) {
                    owner = rs.getString("LEASE_OWNER");
                    System.out.println("testLeaseTableAccess: owner is - " + owner);
                }

                rs.close();

                if (owner == null) {
                    throw new Exception("No rows were returned for " + selForUpdateString);
                }

                String updateString = "UPDATE WAS_LEASES_LOG" +
                                      " SET LEASE_OWNER = 'cloud0011'" +
                                      " WHERE SERVER_IDENTITY='cloud0011'";
                System.out.println("testLeaseTableAccess: " + updateString);
                stmt.executeUpdate(updateString);
            } catch (Exception x) {
                System.out.println("testLeaseTableAccess: caught exception - " + x);
            }

            System.out.println("testLeaseTableAccess: commit changes to database");
            con.commit();
        } catch (Exception ex) {
            System.out.println("testLeaseTableAccess: caught exception in testSetup: " + ex);
        }
    }

    public void modifyLeaseOwner(HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        try (Connection con = getConnection(dsTranLog)) {
            con.setAutoCommit(false);
            final DatabaseMetaData mdata = con.getMetaData();
            final String dbName = mdata.getDatabaseProductName();
            final boolean isPostgreSQL = dbName.toLowerCase().contains("postgresql");
            final boolean isSQLServer = dbName.toLowerCase().contains("microsoft sql");

            // Statement used to drop table
            final String selForUpdateString = "SELECT LEASE_OWNER" +
                                              " FROM WAS_LEASES_LOG" +
                                              (isSQLServer ? " WITH (ROWLOCK, UPDLOCK, HOLDLOCK)" : "") +
                                              " WHERE SERVER_IDENTITY='cloud0011'" +
                                              ((isSQLServer) ? "" : " FOR UPDATE") +
                                              ((isPostgreSQL || isSQLServer) ? "" : " OF LEASE_TIME");
            System.out.println("modifyLeaseOwner: " + selForUpdateString);
            try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(selForUpdateString)) {

                String owner = null;
                while (rs.next()) {
                    owner = rs.getString("LEASE_OWNER");
                    System.out.println("modifyLeaseOwner: owner is - " + owner);
                    break;
                }

                if (owner == null) {
                    throw new Exception("No rows were returned for " + selForUpdateString);
                }

                final String updateString = "UPDATE WAS_LEASES_LOG" +
                                            " SET LEASE_OWNER = 'cloud0021'" +
                                            " WHERE SERVER_IDENTITY='cloud0011'";
                System.out.println("modifyLeaseOwner: " + updateString);
                final int ret = stmt.executeUpdate(updateString);
                System.out.println("modifyLeaseOwner: update returned " + ret);
            }

            System.out.println("modifyLeaseOwner: commit changes to database");
            con.commit();
        }
    }

    public void setLatch(HttpServletRequest request,
                         HttpServletResponse response) throws Exception {

        try (Connection con = getConnection(dsTranLog)) {
            // Statement used to drop table
            try (Statement stmt = con.createStatement()) {

                long latch = 255L;
                String updateString = "UPDATE " + "WAS_PARTNER_LOGcloud0011" +
                                      " SET RUSECTION_ID = " + latch +
                                      " WHERE RU_ID = -1";
                stmt.executeUpdate(updateString);
            } catch (SQLException x) {
                System.out.println("setLatch: caught exception - " + x);
            }

            System.out.println("setLatch: commit changes to database");
            con.commit();
        } catch (Exception ex) {
            System.out.println("setLatch: caught exception in testSetup: " + ex);
        }
    }

    public void setPeerOwnership(HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        try (Connection con = getConnection(dsTranLog)) {
            // Statement used to drop table
            try (Statement stmt = con.createStatement()) {
                String updateString = "UPDATE " + "WAS_PARTNER_LOGcloud0011" +
                                      " SET SERVER_NAME = 'cloud0021'" +
                                      " WHERE RU_ID = -1";
                stmt.executeUpdate(updateString);
            } catch (SQLException x) {
                System.out.println("setPeerOwnership: caught exception - " + x);
            }

            System.out.println("setPeerOwnership: commit changes to database");
            con.commit();
        } catch (Exception ex) {
            System.out.println("setPeerOwnership: caught exception in testSetup: " + ex);
        }
    }

    // Check our logs are still here and peer leases have gone
    public void testTranlogTableAccess(HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {

        try (Connection con = getConnection(dsTranLog)) {
            con.setAutoCommit(false);

            final Set<String> ourTables = new HashSet<String>();

            // Need to be a bit careful here, some RDBMS store upper case versions of the name and some lower.
            for (String wasPrefix : Arrays.asList("WAS_%", "was_%")) {
                try (ResultSet tables = con.getMetaData().getTables(null, null, wasPrefix, null)) {
                    while (tables.next()) {
                        final String tableName = tables.getString("Table_NAME");
                        System.out.println("Found table: " + tableName);
                        ourTables.add(tableName.toUpperCase());
                    }
                }
            }

            con.commit();

            if (ourTables.contains("WAS_TRAN_LOGCLOUD0011") || ourTables.contains("WAS_PARTNER_LOGCLOUD0011")) {
                throw new Exception("cloud0011 logs still exist");
            }

            if (!ourTables.contains("WAS_TRAN_LOGCLOUD0021") || !ourTables.contains("WAS_PARTNER_LOGCLOUD0021")) {
                throw new Exception("cloud0021 logs don't exist");
            }
        }
    }

    public void setupV1LeaseLog(HttpServletRequest request,
                                HttpServletResponse response) throws Exception {

        try (Connection con = getConnection(dsTranLog)) {
            con.setAutoCommit(false);
            DatabaseMetaData mdata = con.getMetaData();
            String dbName = mdata.getDatabaseProductName();
            boolean isPostgreSQL = dbName.toLowerCase().contains("postgresql");
            boolean isSQLServer = dbName.toLowerCase().contains("microsoft sql");

            // Statement used to drop table
            try (Statement stmt = con.createStatement()) {
                String selForUpdateString = "SELECT LEASE_OWNER" +
                                            " FROM WAS_LEASES_LOG" +
                                            (isSQLServer ? " WITH (ROWLOCK, UPDLOCK, HOLDLOCK)" : "") +
                                            " WHERE SERVER_IDENTITY='cloud0011'" +
                                            ((isSQLServer) ? "" : " FOR UPDATE") +
                                            ((isPostgreSQL || isSQLServer) ? "" : " OF LEASE_OWNER");
                System.out.println("setupV1LeaseLog: " + selForUpdateString);
                ResultSet rs = stmt.executeQuery(selForUpdateString);
                String owner = null;
                while (rs.next()) {
                    owner = rs.getString("LEASE_OWNER");
                    System.out.println("setupV1LeaseLog: owner is - " + owner);
                }
                rs.close();

                if (owner == null) {
                    throw new Exception("No rows were returned for " + selForUpdateString);
                }

                String updateString = "UPDATE WAS_LEASES_LOG" +
                                      " SET LEASE_OWNER = 'cloud0011'" +
                                      " WHERE SERVER_IDENTITY='cloud0011'";
                System.out.println("setupV1LeaseLog: " + updateString);
                stmt.executeUpdate(updateString);

                //Insert a couple of artificial server references into the lease log.
                String insertString = "INSERT INTO WAS_LEASES_LOG" +
                                      " (SERVER_IDENTITY, RECOVERY_GROUP, LEASE_OWNER, LEASE_TIME)" +
                                      " VALUES (?,?,?,?)";

                PreparedStatement specStatement = null;
                long fir1 = 0;

                System.out.println("setupV1LeaseLog: setup new row using - " + insertString);

                specStatement = con.prepareStatement(insertString);
                specStatement.setString(1, "cloud0022");
                specStatement.setString(2, "defaultGroup");
                specStatement.setString(3, "cloud0022");
                specStatement.setLong(4, fir1);

                specStatement.executeUpdate();

                specStatement = con.prepareStatement(insertString);
                specStatement.setString(1, "cloud0033");
                specStatement.setString(2, "defaultGroup");
                specStatement.setString(3, "cloud0033");
                specStatement.setLong(4, fir1);

                specStatement.executeUpdate();

                System.out.println("setupV1LeaseLog: commit changes to database");
                con.commit();
            } catch (Exception ex) {
                System.out.println("setupV1LeaseLog: caught exception in testSetup: " + ex);
            }
        }
    }

    /**
     * This method supports a retry when a connection is required.
     *
     * @param dSource
     * @return
     * @throws Exception
     */
    private Connection getConnection(DataSource dSource) throws SQLException {
        Connection conn = null;
        int retries = 0;

        SQLException excToThrow = null;
        while (retries < 2 && conn == null) {
            try {
                System.out.println("Simple2PCCloudServlet: getConnection called against resource - " + dSource);
                conn = dSource.getConnection();
            } catch (SQLException ex) {
                System.out.println("Simple2PCCloudServlet: getConnection caught exception - " + ex);
                excToThrow = ex;
                retries++;
            }
        }

        if (conn == null && excToThrow != null) {
            throw excToThrow;
        }

        System.out.println("Simple2PCCloudServlet: getConnection returned connection - " + conn);
        return conn;
    }

    public void insertOrphanLease() throws Exception {

        try (Connection con = getConnection(dsTranLog)) {
            con.setAutoCommit(false);
            DatabaseMetaData mdata = con.getMetaData();
            String dbName = mdata.getDatabaseProductName();
            System.out.println("insertOrphanLease with cleanup");
            // Access the Database
            boolean isPostgreSQL = false;
            boolean isSQLServer = false;
            if (dbName.toLowerCase().contains("postgresql")) {
                // we are PostgreSQL
                isPostgreSQL = true;
                System.out.println("insertOrphanLease: This is a PostgreSQL Database");
            } else if (dbName.toLowerCase().contains("microsoft sql")) {
                // we are MS SQL Server
                isSQLServer = true;
                System.out.println("insertOrphanLease: This is an MS SQL Server Database");
            }

            String queryString = "SELECT LEASE_TIME" +
                                 " FROM WAS_LEASES_LOG" +
                                 (isSQLServer ? " WITH (UPDLOCK)" : "") +
                                 " WHERE SERVER_IDENTITY='nonexistant'" +
                                 (isSQLServer ? "" : " FOR UPDATE") +
                                 (isSQLServer || isPostgreSQL ? "" : " OF LEASE_TIME");
            System.out.println("insertOrphanLease: Attempt to select the row for UPDATE using - " + queryString);

            try (Statement claimPeerlockingStmt = con.createStatement(); ResultSet claimPeerLockingRS = claimPeerlockingStmt.executeQuery(queryString)) {

                final long fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES).toEpochMilli();

                // see if we acquired the row
                if (claimPeerLockingRS.next()) {
                    // We found an existing lease row
                    long storedLease = claimPeerLockingRS.getLong(1);
                    System.out.println("insertOrphanLease: Acquired server row, stored lease value is: " + storedLease);

                    // Construct the UPDATE string
                    String updateString = "UPDATE WAS_LEASES_LOG" +
                                          " SET LEASE_OWNER = ?, LEASE_TIME = ?" +
                                          " WHERE SERVER_IDENTITY='nonexistant'";

                    System.out.println("insertOrphanLease: update lease for nonexistant");

                    try (PreparedStatement claimPeerUpdateStmt = con.prepareStatement(updateString)) {

                        // Set the Lease_time
                        claimPeerUpdateStmt.setString(1, "nonexistant");
                        claimPeerUpdateStmt.setLong(2, fiveMinutesAgo);

                        System.out.println("insertOrphanLease: Ready to UPDATE using string - " + updateString + " and time: " + TxTestUtils.traceTime(fiveMinutesAgo));

                        int ret = claimPeerUpdateStmt.executeUpdate();

                        System.out.println("insertOrphanLease: Have updated server row with return: " + ret);
                        con.commit();
                    } catch (Exception ex) {
                        System.out.println("insertOrphanLease: caught exception in testSetup: " + ex);
                        // attempt rollback
                        con.rollback();
                    }
                } else {
                    // We didn't find the row in the table
                    System.out.println("insertOrphanLease: Could not find row");

                    String insertString = "INSERT INTO WAS_LEASES_LOG" +
                                          " (SERVER_IDENTITY, RECOVERY_GROUP, LEASE_OWNER, LEASE_TIME)" +
                                          " VALUES (?,?,?,?)";

                    System.out.println("insertOrphanLease: Using - " + insertString + ", and time: " + TxTestUtils.traceTime(fiveMinutesAgo));

                    try (PreparedStatement specStatement = con.prepareStatement(insertString)) {
                        specStatement.setString(1, "nonexistant");
                        specStatement.setString(2, "defaultGroup");
                        specStatement.setString(3, "nonexistant");
                        specStatement.setLong(4, fiveMinutesAgo);

                        int ret = specStatement.executeUpdate();

                        System.out.println("insertOrphanLease: Have inserted Server row with return: " + ret);
                        con.commit();
                    } catch (Exception ex) {
                        System.out.println("insertOrphanLease: caught exception in testSetup: " + ex);
                        // attempt rollback
                        con.rollback();
                    }
                }
            }
        }
    }

    public void setupNonUniqueLeaseLog(HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {

        final String genericTableString = "CREATE TABLE WAS_LEASES_LOG" +
                                          "( SERVER_IDENTITY VARCHAR(128), RECOVERY_GROUP VARCHAR(128), LEASE_OWNER VARCHAR(128), " +
                                          "LEASE_TIME BIGINT) ";

        final String oracleTableString = "CREATE TABLE WAS_LEASES_LOG" +
                                         "( SERVER_IDENTITY VARCHAR(128), RECOVERY_GROUP VARCHAR(128), LEASE_OWNER VARCHAR(128), " +
                                         "LEASE_TIME NUMBER(19)) ";

        final String postgreSQLTableString = "CREATE TABLE WAS_LEASES_LOG" +
                                             "( SERVER_IDENTITY VARCHAR (128) UNIQUE NOT NULL, RECOVERY_GROUP VARCHAR (128) NOT NULL, LEASE_OWNER VARCHAR (128) NOT NULL, "
                                             +
                                             "LEASE_TIME BIGINT);";

        try (Connection con = getConnection(dsTranLog)) {
            con.setAutoCommit(false);
            DatabaseMetaData mdata = con.getMetaData();
            String dbName = mdata.getDatabaseProductName();
            boolean isPostgreSQL = dbName.toLowerCase().contains("postgresql");
            boolean isOracle = dbName.toLowerCase().contains("oracle");
            boolean isSQLServer = dbName.toLowerCase().contains("microsoft sql");

            try (Statement stmt = con.createStatement()) {
                String dropTableString = "DROP TABLE WAS_LEASES_LOG";
                System.out.println("setupNonIndexedLeaseLog: Drop table using: " + dropTableString);
                int dropReturn = stmt.executeUpdate(dropTableString);
                con.commit();
            } catch (Exception ex) {
                System.out.println("setupNonIndexedLeaseLog: caught exception in testSetup: " + ex);
            }

            // Now set up old school WAS_LEASES_LOG table
            try (Statement stmt = con.createStatement()) {
                if (isOracle) {
                    System.out.println("setupNonIndexedLeaseLog: Create Oracle Table using: " + oracleTableString);
                    stmt.executeUpdate(oracleTableString);
                    String oracleIndexString = "CREATE INDEX IXWS_LEASE ON WAS_LEASES_LOG( \"SERVER_IDENTITY\" ASC) ";

                    System.out.println("setupNonIndexedLeaseLog: Create SQL Server index using: " + oracleIndexString);

                    // Create index on the new table
                    stmt.execute(oracleIndexString);
                } else if (isPostgreSQL) {
                    System.out.println("setupNonIndexedLeaseLog: Create PostgreSQL Table using: " + postgreSQLTableString);
                    stmt.execute(postgreSQLTableString);
                    String postgresqlIndexString = "CREATE INDEX IXWS_LEASE ON WAS_LEASES_LOG( SERVER_IDENTITY ASC) ";

                    System.out.println("setupNonIndexedLeaseLog: Create SQL Server index using: " + postgresqlIndexString);

                    // Create index on the new table
                    stmt.execute(postgresqlIndexString);
                } else {
                    System.out.println("setupNonIndexedLeaseLog: Create Generic Table using: " + genericTableString);
                    stmt.executeUpdate(genericTableString);
                    String genericIndexString = "CREATE INDEX IXWS_LEASE ON WAS_LEASES_LOG( \"SERVER_IDENTITY\" ASC) ";

                    System.out.println("setupNonIndexedLeaseLog: Create SQL Server index using: " + genericIndexString);

                    // Create index on the new table
                    stmt.execute(genericIndexString);
                }
                con.commit();
                System.out.println("setupNonIndexedLeaseLog: new table created");

            } catch (Exception ex) {
                System.out.println("setupNonIndexedLeaseLog: caught exception in testSetup: " + ex);
            }
        }
    }

    public void dropServer2Tables(HttpServletRequest request,
                                  HttpServletResponse response) throws Exception {

        try (Connection con = getConnection(dsTranLog)) {
            con.setAutoCommit(false);
            try (Statement stmt = con.createStatement()) {
                String dropTableString = "DROP TABLE WAS_TRAN_LOGCLOUD0021";
                System.out.println("dropServer2Tables: Drop table using: " + dropTableString);
                int dropReturn = stmt.executeUpdate(dropTableString);
                dropTableString = "DROP TABLE WAS_PARTNER_LOGCLOUD0021";
                System.out.println("dropServer2Tables: Drop table using: " + dropTableString);
                dropReturn = stmt.executeUpdate(dropTableString);
                con.commit();
            } catch (Exception ex) {
                System.out.println("dropServer2Tables: caught exception in testSetup: " + ex);
            }
        }
    }

    // Run a tran that is expected to fail
    public void doomedTran(HttpServletRequest request,
                           HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);

        boolean tranFailed = false;
        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1);
            int recoveryId1 = tm.registerResourceInfo(XAResourceInfoFactory.filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResource(xaResInfo2);
            int recoveryId2 = tm.registerResourceInfo(XAResourceInfoFactory.filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.commit();
        } catch (Exception e) {
            // This is what we expect
            e.printStackTrace();
            tranFailed = true;
        }

        assertTrue("Transaction should have failed", tranFailed);
    }

    public void modifyLeaseOwnerAndDie(HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {
        modifyLeaseOwner(request, response);
        // Give the test something to search for
        XAResourceImpl.dumpState();
        Runtime.getRuntime().halt(XAResourceImpl.DIE);
    }

    public void checkOrphanLeaseAbsence() throws Exception {

        try (Connection con = getConnection(dsTranLog)) {
            con.setAutoCommit(false);
            DatabaseMetaData mdata = con.getMetaData();
            String dbName = mdata.getDatabaseProductName();

            // Access the Database
            boolean isPostgreSQL = false;
            boolean isSQLServer = false;
            if (dbName.toLowerCase().contains("postgresql")) {
                // we are PostgreSQL
                isPostgreSQL = true;
            } else if (dbName.toLowerCase().contains("microsoft sql")) {
                // we are MS SQL Server
                isSQLServer = true;
            }

            String queryString = "SELECT LEASE_TIME" +
                                 " FROM WAS_LEASES_LOG" +
                                 (isSQLServer ? " WITH (UPDLOCK)" : "") +
                                 " WHERE SERVER_IDENTITY='nonexistant'" +
                                 (isSQLServer ? "" : " FOR UPDATE") +
                                 (isSQLServer || isPostgreSQL ? "" : " OF LEASE_TIME");

            try (Statement claimPeerlockingStmt = con.createStatement(); ResultSet claimPeerLockingRS = claimPeerlockingStmt.executeQuery(queryString)) {

                // see if we acquired the row
                if (claimPeerLockingRS.next()) {
                    throw new Exception();
                    // We found an existing lease row
                }
            }
        }
    }

    public void setupBatchOfOrphanLeases(int lower, int upper) throws Exception {

        try (Connection con = getConnection(dsTranLog); Statement claimPeerlockingStmt = con.createStatement()) {

            con.setAutoCommit(false);

            for (int i = lower; i < upper; i++) {
                String insertString = "INSERT INTO WAS_LEASES_LOG" +
                                      " (SERVER_IDENTITY, RECOVERY_GROUP, LEASE_OWNER, LEASE_TIME)" +
                                      " VALUES (?,?,?,?)";

                final long fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES).toEpochMilli();
                String serverid = UUID.randomUUID().toString().replaceAll("\\W", "");
                System.out.println("setupBatchOfOrphanLeases: Using - " + insertString + ", and time: " + TxTestUtils.traceTime(fiveMinutesAgo));

                try (PreparedStatement specStatement = con.prepareStatement(insertString)) {
                    specStatement.setString(1, serverid);
                    specStatement.setString(2, "defaultGroup");
                    specStatement.setString(3, serverid);
                    specStatement.setLong(4, fiveMinutesAgo);

                    int ret = specStatement.executeUpdate();

                    System.out.println("setupBatchOfOrphanLeases: Have inserted Server row with return: " + ret);
                }
            }

            con.commit();
        }
    }

    public void setupBatchOfOrphanLeases1() throws Exception {

        setupBatchOfOrphanLeases(0, 10);
    }

    public void setupBatchOfOrphanLeases2() throws Exception {

        setupBatchOfOrphanLeases(10, 20);
    }
}