/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
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
                                            ((isPostgreSQL || isSQLServer) ? "" : " OF LEASE_TIME");
                System.out.println("modifyLeaseOwner: " + selForUpdateString);
                ResultSet rs = stmt.executeQuery(selForUpdateString);
                String owner = null;
                while (rs.next()) {
                    owner = rs.getString("LEASE_OWNER");
                    System.out.println("modifyLeaseOwner: owner is - " + owner);
                }
                rs.close();

                if (owner == null) {
                    throw new Exception("No rows were returned for " + selForUpdateString);
                }

                String updateString = "UPDATE WAS_LEASES_LOG" +
                                      " SET LEASE_OWNER = 'cloud0021'" +
                                      " WHERE SERVER_IDENTITY='cloud0011'";
                System.out.println("modifyLeaseOwner: " + updateString);
                stmt.executeUpdate(updateString);
            } catch (Exception x) {
                System.out.println("modifyLeaseOwner: caught exception - " + x);
            }

            System.out.println("modifyLeaseOwner: commit changes to database");
            con.commit();
        } catch (Exception ex) {
            System.out.println("modifyLeaseOwner: caught exception in testSetup: " + ex);
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

    public void testTranlogTableAccess(HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {

        try (Connection con = getConnection(dsTranLog)) {
            con.setAutoCommit(false);

            DatabaseMetaData mdata = con.getMetaData();

            System.out.println("testTranlogTableAccess: get metadata tables - " + mdata);

            // Need to be a bit careful here, some RDBMS store uppercase versions of the name and some lower.
            boolean foundUpperTranCloud1 = false; // WAS_TRAN_LOGCLOUD0011 should have been dropped
            boolean foundUpperTranCloud2 = false; // WAS_TRAN_LOGCLOUD0021 should be found
            boolean foundUpperPartnerCloud1 = false; // WAS_PARTNER_LOGCLOUD0011 should have been dropped
            boolean foundUpperPartnerCloud2 = false; // WAS_PARTNER_LOGCLOUD0021 should be found
            //Retrieving the columns in the database
            ResultSet tables = mdata.getTables(null, null, "WAS_%", null); // Just get all the "WAS" prepended tables
            String tableString = "";
            while (tables.next()) {
                tableString = tables.getString("Table_NAME");
                System.out.println("testTranlogTableAccess: Found table name: " + tableString);
                if (tableString.equalsIgnoreCase("WAS_TRAN_LOGCLOUD0011"))
                    foundUpperTranCloud1 = true;
                else if (tableString.equalsIgnoreCase("WAS_TRAN_LOGCLOUD0021"))
                    foundUpperTranCloud2 = true;
                else if (tableString.equalsIgnoreCase("WAS_PARTNER_LOGCLOUD0011"))
                    foundUpperPartnerCloud1 = true;
                else if (tableString.equalsIgnoreCase("WAS_PARTNER_LOGCLOUD0021"))
                    foundUpperPartnerCloud2 = true;
            }

            boolean foundLowerTranCloud1 = false; // WAS_TRAN_LOGCLOUD0011 should have been dropped
            boolean foundLowerTranCloud2 = false; // WAS_TRAN_LOGCLOUD0021 should be found
            boolean foundLowerPartnerCloud1 = false; // WAS_PARTNER_LOGCLOUD0011 should have been dropped
            boolean foundLowerPartnerCloud2 = false; // WAS_PARTNER_LOGCLOUD0021 should be found
            tables = mdata.getTables(null, null, "was%", null); // Just get all the "was" tables
            while (tables.next()) {
                tableString = tables.getString("Table_NAME");
                System.out.println("testTranlogTableAccess: Found table name: " + tableString);
                if (tableString.equalsIgnoreCase("WAS_TRAN_LOGCLOUD0011"))
                    foundLowerTranCloud1 = true;
                else if (tableString.equalsIgnoreCase("WAS_TRAN_LOGCLOUD0021"))
                    foundLowerTranCloud2 = true;
                else if (tableString.equalsIgnoreCase("WAS_PARTNER_LOGCLOUD0011"))
                    foundLowerPartnerCloud1 = true;
                else if (tableString.equalsIgnoreCase("WAS_PARTNER_LOGCLOUD0021"))
                    foundLowerPartnerCloud2 = true;
            }

            // Report unexpected behaviour
            final PrintWriter pw = response.getWriter();
            if (foundUpperTranCloud1 || foundLowerTranCloud1) {
                pw.println("Unexpectedly found tran log table for CLOUD0011");
            }
            if (!foundUpperTranCloud2 && !foundLowerTranCloud2) {
                pw.println("Unexpectedly did not find tran log table for CLOUD0021");
            }
            if (foundUpperPartnerCloud1 || foundLowerPartnerCloud1) {
                pw.println("Unexpectedly found partner log table for CLOUD0011");
            }
            if (!foundUpperPartnerCloud2 && !foundLowerPartnerCloud2) {
                pw.println("Unexpectedly did not find partner log table for CLOUD0021");
            }
            tables.close();
            con.commit();
        } catch (Exception ex) {
            System.out.println("testTranlogTableAccess: caught exception " + ex);
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

    public void tidyupV1LeaseLog(HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        try (Connection con = getConnection(dsTranLog)) {
            con.setAutoCommit(false);
            DatabaseMetaData mdata = con.getMetaData();
            String dbName = mdata.getDatabaseProductName();
            boolean isSQLServer = dbName.toLowerCase().contains("microsoft sql");

            // Statement used to delete from table
            try (Statement stmt = con.createStatement()) {
                String delString = "DELETE FROM WAS_LEASES_LOG" +
                                   (isSQLServer ? " WITH (ROWLOCK, UPDLOCK, HOLDLOCK)" : "") +
                                   " WHERE SERVER_IDENTITY='cloud0022'";
                System.out.println("tidyupV1LeaseLog: " + delString);
                int ret = stmt.executeUpdate(delString);
                System.out.println("tidyupV1LeaseLog: return was " + ret);
                delString = "DELETE FROM WAS_LEASES_LOG" +
                            (isSQLServer ? " WITH (ROWLOCK, UPDLOCK, HOLDLOCK)" : "") +
                            " WHERE SERVER_IDENTITY='cloud0033'";
                System.out.println("tidyupV1LeaseLog: " + delString);
                ret = stmt.executeUpdate(delString);

                System.out.println("tidyupV1LeaseLog: return was " + ret + " commit changes to database");
                con.commit();
            } catch (Exception ex) {
                System.out.println("tidyupV1LeaseLog: caught exception in testSetup: " + ex);
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
            Statement stmt = null;
            // Statement used to drop table
            try {
                stmt = con.createStatement();
                String dropTableString = "DROP TABLE WAS_LEASES_LOG";
                System.out.println("setupNonIndexedLeaseLog: Drop table using: " + dropTableString);
                int dropReturn = stmt.executeUpdate(dropTableString);
                con.commit();
            } catch (Exception ex) {
                System.out.println("setupNonIndexedLeaseLog: caught exception in testSetup: " + ex);
            }

            try {
                // Now set up old school WAS_LEASES_LOG table
                stmt = con.createStatement();
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
            DatabaseMetaData mdata = con.getMetaData();
            String dbName = mdata.getDatabaseProductName();
            boolean isPostgreSQL = dbName.toLowerCase().contains("postgresql");
            boolean isOracle = dbName.toLowerCase().contains("oracle");
            boolean isSQLServer = dbName.toLowerCase().contains("microsoft sql");
            Statement stmt = null;
            // Statement used to drop table
            try {
                stmt = con.createStatement();
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

    public void twoTrans(HttpServletRequest request,
                         HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1);
            int recoveryId1 = tm.registerResourceInfo(XAResourceInfoFactory.filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResource(xaResInfo2);
            int recoveryId2 = tm.registerResourceInfo(XAResourceInfoFactory.filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.commit();

            tm.begin();

            recoveryId1 = tm.registerResourceInfo(XAResourceInfoFactory.filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            recoveryId2 = tm.registerResourceInfo(XAResourceInfoFactory.filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
