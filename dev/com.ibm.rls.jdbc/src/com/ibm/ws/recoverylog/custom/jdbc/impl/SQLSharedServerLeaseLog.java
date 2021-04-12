/*******************************************************************************
 * Copyright (c) 2014,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.custom.jdbc.impl;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.sql.DataSource;

import com.ibm.tx.util.Utils;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.recoverylog.spi.CustomLogProperties;
import com.ibm.ws.recoverylog.spi.InternalLogException;
import com.ibm.ws.recoverylog.spi.LeaseInfo;
import com.ibm.ws.recoverylog.spi.PeerLeaseData;
import com.ibm.ws.recoverylog.spi.PeerLeaseTable;
import com.ibm.ws.recoverylog.spi.RecoveryFailedException;
import com.ibm.ws.recoverylog.spi.SharedServerLeaseLog;
import com.ibm.ws.recoverylog.spi.TraceConstants;

/**
 *
 */
public class SQLSharedServerLeaseLog implements SharedServerLeaseLog {

    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(SQLSharedServerLeaseLog.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    // Reference to the dedicated non-transactional datasource
    private DataSource _theDS;

    /**
     * A reference to the LogProperties object that defines the identity and physical
     * location of the recovery log.
     */
    private final CustomLogProperties _customLogProperties;

    /**
     * Are we working against Oracle, PostgreSQL or Generic (DB2 or SQL Server at least)
     */
    private boolean _isOracle;
    private boolean _isPostgreSQL;

    private int _leaseTimeout;
    private final String _leaseTableName = "WAS_LEASES_LOG";

    /**
     * These strings are used for Database table creation. DDL is
     * different for DB2, MS SQL Server, PostgreSQL and Oracle.
     */
    private final String genericTablePreString = "CREATE TABLE ";
    private final String genericTablePostString = "( SERVER_IDENTITY VARCHAR(128), RECOVERY_GROUP VARCHAR(128), LEASE_OWNER VARCHAR(128), " +
                                                  "LEASE_TIME BIGINT) ";

    private final String oracleTablePreString = "CREATE TABLE ";
    private final String oracleTablePostString = "( SERVER_IDENTITY VARCHAR(128), RECOVERY_GROUP VARCHAR(128), LEASE_OWNER VARCHAR(128), " +
                                                 "LEASE_TIME NUMBER(19)) ";

    private final String postgreSQLTablePreString = "CREATE TABLE ";
    private final String postgreSQLTablePostString = "( SERVER_IDENTITY VARCHAR (128) UNIQUE NOT NULL, RECOVERY_GROUP VARCHAR (128) NOT NULL, LEASE_OWNER VARCHAR (128) NOT NULL, "
                                                     +
                                                     "LEASE_TIME BIGINT);";

    /**
     * We only want one client at a time to attempt to create a new
     * Database table.
     */
    private static final Object _CreateTableLock = new Object();

    /**
     * Flag to indicate whether the server is stopping.
     */
    volatile private boolean _serverStopping;

    public SQLSharedServerLeaseLog(CustomLogProperties logProperties) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "SQLSharedServerStatusLog", new Object[] { logProperties, this });

        // Cache the supplied information
        _customLogProperties = logProperties;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "SQLSharedServerStatusLog", this);
    }

    @Override
    public synchronized void getLeasesForPeers(final PeerLeaseTable peerLeaseTable, String recoveryGroup) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getLeasesForPeers", new java.lang.Object[] { recoveryGroup, this });

        // The Database Connection
        Connection conn = null;

        // We obtain a connection to the underlying DB
        ResultSet lockingRS = null;
        Statement lockingStmt = null;
        try {
            // Have we acquired the reference to the DataSource yet?
            if (_theDS == null) {
                _theDS = getDataSourceFromProperties();
                // We've looked up the DS, so now we can get a JDBC connection
                if (_theDS != null) {
                    conn = getConnection(_theDS);
                }
            } else {
                // Try and get a new connection
                conn = _theDS.getConnection();
            }

            // If we were unable to get a connection, throw an exception
            if (conn == null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getLeasesForPeers", "Null connection InternalLogException");
                throw new InternalLogException("Failed to get JDBC Connection", null);
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Set autocommit FALSE on the connection");
            conn.setAutoCommit(false);

            // lookup peers and their timestamps in the leases table.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "create a statement");
            lockingStmt = conn.createStatement();
            // If we were unable to get a connection, throw an exception

            // Set a null recovery group to empty string
            if (recoveryGroup == null) {
                recoveryGroup = "";
            }
            boolean newTable = true;

            // Get peers from table
            // Test if they are stale
            // If they are we'll need to recover them
            // If not just return
            // if needed to create new table then just return

            synchronized (_CreateTableLock) // Guard against trying to create a table from multiple threads
            {
                try {
                    // TODO: Use RDBMS SELECT FOR UPDATE to lock table for recovery
                    String queryString = "SELECT SERVER_IDENTITY, LEASE_TIME" +
                                         " FROM " + _leaseTableName + " WHERE RECOVERY_GROUP = '" + recoveryGroup + "'";
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Attempt to select from the lease table - " + queryString);
                    lockingRS = lockingStmt.executeQuery(queryString);

                    newTable = false;
                } catch (Exception e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Query failed with exception: " + e);

                    try {
                        // Perhaps we couldn't find the table ... so attempt to create it
                        createLeaseTable(conn);

                        conn.commit();

                        newTable = true;
                    } catch (Exception ine) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Table Creation failed with exception: " + ine);
                        // Set the current exception to ine
                        throw ine;
                    }

                } // eof Exception e block

            } // eof synchronize block

            // If table creation succeeded then we are done.
            if (newTable) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Table Creation succeeded");
            } else {
//TODO?HEY THIS
                // Now process through the peers we need to handle
                while (lockingRS.next()) {
                    final String recoveryId = lockingRS.getString(1);
                    final long leaseTime = lockingRS.getLong(2);

                    if (tc.isEventEnabled()) {
                        Tr.event(tc, "Lease Table: read recoveryId: " + recoveryId);
                        Tr.event(tc, "Lease Table: read leaseTime: " + Utils.traceTime(leaseTime));
                    }

                    PeerLeaseData pld = new PeerLeaseData(recoveryId, leaseTime, _leaseTimeout);

                    peerLeaseTable.addPeerEntry(pld);
                }
            }

            // commit and return
            conn.commit();
        } finally {
            if (lockingRS != null && !lockingRS.isClosed())
                lockingRS.close();
            if (lockingStmt != null && !lockingStmt.isClosed())
                lockingStmt.close();
            if (conn != null)
                conn.close();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getLeasesForPeers");
    }

    /**
     * Will update the lease for an existing server in the table. Or insert a new row if required.
     *
     * @param recoveryIdentity
     * @param leaseTime
     * @throws Exception
     */
    @Override
    public synchronized void updateServerLease(String recoveryIdentity, String recoveryGroup, boolean isServerStartup) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "updateServerLease", new java.lang.Object[] { recoveryIdentity, recoveryGroup, isServerStartup, this });

        Connection conn = null;
        Exception currentEx = null;
        Statement lockingStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet lockingRS = null;

        // if the server is stopping, we should simply return
        if (_serverStopping) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "updateServerLease", this);
            return;
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Work with recoveryIdentity - ", recoveryIdentity);

        // Reset a null recoveryGroup to an empty string
        if (recoveryGroup == null)
            recoveryGroup = "";

        try {
            // Have we acquired the reference to the DataSource yet?
            if (_theDS == null) {
                _theDS = getDataSourceFromProperties();
                // We've looked up the DS, so now we can get a JDBC connection
                if (_theDS != null) {
                    conn = getConnection(_theDS);
                }
            } else {
                // Try and get a new connection
                conn = _theDS.getConnection();
            }

            // If we were unable to get a connection, throw an exception, but not if we're stopping
            if (conn == null) {
                if (!_serverStopping) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "updateServerLease", "Null connection InternalLogException");
                    throw new InternalLogException("Failed to get JDBC Connection", null);
                } else {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "updateServerLease", "null connection");
                    return;
                }
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Set autocommit FALSE on the connection");
            conn.setAutoCommit(false);

            // We can go ahead and write to the Database
            lockingStmt = conn.createStatement();
            //lockingStmt.setCursorName("UPDATESTATEMENT");
            boolean newTable = true;

            try {
                String queryString = "SELECT LEASE_TIME, LEASE_OWNER" +
                                     " FROM " + _leaseTableName +
                                     " WHERE SERVER_IDENTITY='" + recoveryIdentity + "'" +
                                     (_isPostgreSQL ? "" : " FOR UPDATE OF LEASE_TIME");
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Attempt to select the row for UPDATE using - " + queryString);
                lockingRS = lockingStmt.executeQuery(queryString);

                newTable = false;
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Query failed with exception: " + e);
                currentEx = e;
            } // eof Exception e block

            if (currentEx != null) {
                if (!_serverStopping) {
                    if (isServerStartup) {
                        // Perhaps we couldn't find the table ... so attempt to create it
                        synchronized (_CreateTableLock) // Guard against trying to create a table from multiple threads
                        {
                            try {
                                Tr.audit(tc, "WTRN0108I: Create Shared Lease Table");
                                createLeaseTable(conn);

                                conn.commit();

                                newTable = true;
                            } catch (Exception ine) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Table Creation failed with exception: " + ine);
                                // Set the current exception to ine
                                throw ine;
                            }
                        } // eof synchronize block
                    } // eof isServerStartup
                    else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Lease select update failed with exception: " + currentEx);
                        // Set the current exception to ine
                        throw currentEx;
                    }
                } else { // server is stopping report but exit without throwing exception
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "updateServerLease", "Lease select update failed with exception: " + currentEx);
                    return;
                }
            }

            boolean needInsert = true;
            if (!newTable) {
                // We havent just created the table, see if we have a row to update
                if (lockingRS.next()) {
                    // We found the Server row
                    long storedLease = lockingRS.getLong(1);
                    String storedLeaseOwner = lockingRS.getString(2);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Acquired lock row, stored lease value is: " + Utils.traceTime(storedLease) + ", stored owner is: " + storedLeaseOwner);

                    // If this is startup, check whether lease has expired
                    if (isServerStartup) {
                        PeerLeaseData pld = new PeerLeaseData(recoveryIdentity, storedLease, _leaseTimeout);
                        if (pld.isExpired()) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Lease has expired, we should update lease and recover");
                            // We have acquired the lock. If any other server is also trying to peer recover then
                            // they can fail. To make this happen, we'll update the lease time ourselves.
                        } else {
                            // Lease has not expired, are we the owner or is some other server peer recovering
                            // If the latter, then we should barf
                            if (!storedLeaseOwner.equals(recoveryIdentity)) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "A peer is recovering, we will fail our recovery and exit");
                                RecoveryFailedException rex = new RecoveryFailedException(recoveryIdentity);
                                throw rex;
                            }
                        }
                    }

                    // Construct the UPDATE string
                    String updateString = "UPDATE " + _leaseTableName +
                                          " SET LEASE_TIME = ?, RECOVERY_GROUP = ?, LEASE_OWNER = ?" +
                                          " WHERE SERVER_IDENTITY='" + recoveryIdentity + "'";
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "update lease for " + recoveryIdentity);

                    updateStmt = conn.prepareStatement(updateString);
                    // Set the Lease_time to the current time
                    //TODO:
                    long fir1 = System.currentTimeMillis();
                    updateStmt.setLong(1, fir1);
                    updateStmt.setString(2, recoveryGroup);
                    updateStmt.setString(3, recoveryIdentity);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Ready to UPDATE using string - " + updateString + " and time: " + Utils.traceTime(fir1));

                    int ret = updateStmt.executeUpdate();

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Have updated Server row with return: " + ret);
                    needInsert = false;
                } else {
                    // We didn't find the row in the table
                    //TODO: Should we insert a new one?
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Could not find row");
                }
            }

            // Either a new table or we couldn't find the row for our server. Insert it.
            if (needInsert) {
                if (!_serverStopping) {
                    // Insert a new row into the lease table
                    insertNewLease(recoveryIdentity, recoveryGroup, conn);
                } else { // server is stopping exit without insert
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "updateServerLease", "skip insert server is stopping");
                    return;
                }
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "COMMIT the change");
            conn.commit();
        }
        // Catch and report an SQLException. In the finally block we'll determine whether the condition is transient or not.
        catch (SQLException sqlex) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Lease update failed with exception: " + sqlex);
        } finally {
            if (lockingRS != null && !lockingRS.isClosed())
                lockingRS.close();
            if (lockingStmt != null && !lockingStmt.isClosed())
                lockingStmt.close();
            if (updateStmt != null && !updateStmt.isClosed())
                updateStmt.close();
            if (conn != null)
                conn.close();

        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "updateServerLease");
    }

    /**
     * Insert a new lease in the table
     *
     * @param recoveryIdentity
     * @param recoveryGroup
     * @param conn
     * @throws SQLException
     */
    private void insertNewLease(String recoveryIdentity, String recoveryGroup, Connection conn) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "insertNewLease", this);

        short serviceId = (short) 1;
        String insertString = "INSERT INTO " +
                              _leaseTableName +
                              " (SERVER_IDENTITY, RECOVERY_GROUP, LEASE_OWNER, LEASE_TIME)" +
                              " VALUES (?,?,?,?)";

        PreparedStatement specStatement = null;
        long fir1 = System.currentTimeMillis();

        Tr.audit(tc, "WTRN0108I: Insert New Lease for server with recovery identity " + recoveryIdentity);
        try {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Need to setup new row using - " + insertString + ", and time: " + Utils.traceTime(fir1));
            specStatement = conn.prepareStatement(insertString);
            specStatement.setString(1, recoveryIdentity);
            specStatement.setString(2, recoveryGroup);
            specStatement.setString(3, recoveryIdentity);
            specStatement.setLong(4, fir1);

            int ret = specStatement.executeUpdate();

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Have inserted Server row with return: " + ret);
        } finally {
            if (specStatement != null && !specStatement.isClosed())
                specStatement.close();
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "insertNewLease");
    }

    private DataSource getDataSourceFromProperties() throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getDataSourceFromProperties", this);
        DataSource dataSource = null;
        Properties internalLogProperties = _customLogProperties.properties();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Recovery log internal properties are " + internalLogProperties);

        String fullLogDirectory = internalLogProperties.getProperty("LOG_DIRECTORY");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "fullLogDirectory = " + fullLogDirectory);
        // Parse database properties
        StringTokenizer st = new StringTokenizer(fullLogDirectory, "?");
        String cname = st.nextToken();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "cname = " + cname);

        // Extract the DB related properties
        Properties dbStringProps = new Properties();
        String dbPropertiesString = st.nextToken();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "dbPropertiesString = " + dbPropertiesString);

        dbStringProps.load(new StringReader(dbPropertiesString.replace(',', '\n')));
        if (tc.isDebugEnabled())
            Tr.debug(tc, "dbStringProps = " + dbStringProps);

        // Extract the DSName
        String dsName = dbStringProps.getProperty("datasource");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Extracted Data Source name = " + dsName);

        // We'll look up the DataSource definition in jndi but jndi is initialising in another
        // thread so we need to handle the situation where a first lookup does not succeed. This
        // processing is wrapped up in the SQLNonTransactionalDataSource
        SQLNonTransactionalDataSource sqlNonTranDS = new SQLNonTransactionalDataSource(dsName, _customLogProperties);

        dataSource = sqlNonTranDS.getDataSource();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getDataSourceFromProperties", dataSource);
        return dataSource;
    }

    private Connection getConnection(DataSource dataSource) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getConnection", this);
        Connection conn = null;

        // Get connection to database via first datasource
        conn = dataSource.getConnection();

        if (conn != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Got connection: " + conn);
            DatabaseMetaData mdata = conn.getMetaData();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Got metadata: " + mdata);
            String dbName = mdata.getDatabaseProductName();
            if (dbName.toLowerCase().contains("oracle")) {
                _isOracle = true;
                // We can set the transient error codes to watch for at this point too.
                //TODO: WORRY about failover later
                //_sqlTransientErrorCodes = _oracleTransientErrorCodes;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "This is an Oracle Database");
                // Flag the we can tolerate transient SQL error codes
                //sqlTransientErrorHandlingEnabled = true;
            } else if (dbName.toLowerCase().contains("postgresql")) {
                // we are PostgreSQL
                _isPostgreSQL = true;
                //TODO: WORRY about failover later
                //_sqlTransientErrorCodes = _db2TransientErrorCodes;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "This is a PostgreSQL Database");
                // Flag the we can tolerate transient SQL error codes
                //sqlTransientErrorHandlingEnabled = true;
            } else if (dbName.toLowerCase().contains("db2")) {
                // we are DB2
                //TODO: WORRY about failover later
                //_sqlTransientErrorCodes = _db2TransientErrorCodes;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "This is a DB2 Database");
                // Flag the we can tolerate transient SQL error codes
                //sqlTransientErrorHandlingEnabled = true;
            } else if (dbName.toLowerCase().contains("microsoft sql")) {
                // we are MS SQL Server
                //TODO: WORRY about failover later
                //_sqlTransientErrorCodes = _db2TransientErrorCodes;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "This is a Microsoft SQL Server Database");
                // Flag the we can tolerate transient SQL error codes
                //sqlTransientErrorHandlingEnabled = true;
            } else {
                // Not DB2, PostgreSQL or Oracle, cannot handle transient SQL errors
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "This is neither Oracle, PostgreSQL, MS SQL Server nor DB2, it is " + dbName);
            }

            String dbVersion = mdata.getDatabaseProductVersion();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "You are now connected to " + dbName + ", version " + dbVersion);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getConnection", conn);
        return conn;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.createDBTable
    //------------------------------------------------------------------------------
    /**
     * Creates the database table that is being used for the recovery
     * log.
     *
     * @exception SQLException thrown if a SQLException is
     *                encountered when accessing the
     *                Database.
     */
    private void createLeaseTable(Connection conn) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createLeaseTable", new java.lang.Object[] { conn, this });

        Statement createTableStmt = null;

        try {
            createTableStmt = conn.createStatement();

            if (_isOracle) {
                String oracleTableString = oracleTablePreString + _leaseTableName + oracleTablePostString;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Create Oracle Table using: " + oracleTableString);
                createTableStmt.executeUpdate(oracleTableString);
            } else if (_isPostgreSQL) {
                String postgreSQLTableString = postgreSQLTablePreString + _leaseTableName + postgreSQLTablePostString;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Create PostgreSQL Table using: " + postgreSQLTableString);
                conn.rollback();
                createTableStmt.execute(postgreSQLTableString);
            } else {
                String genericTableString = genericTablePreString + _leaseTableName + genericTablePostString;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Create Generic Table using: " + genericTableString);
                createTableStmt.executeUpdate(genericTableString);
            }

        } finally {
            if (createTableStmt != null && !createTableStmt.isClosed()) {
                createTableStmt.close();
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createLeaseTable");
    }

    /*
     * This method supports the deletion of a server's lease. We have to be a little careful as we may be deleting the lease for a peer
     * and that peer may have restarted. In this case we should test that the lease is
     *
     * that we have recoveredRead lease check if it is still expired. If so, then update lease and proceed to peer recover
     * if not still expired (someone else has grabbed it) then bypass peer recover.
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#deleteServerLease(java.lang.String)
     */
//TODO:HEY ISSUE HERE IS THAT in the case where we are doing peer recovery we will have updated the lease ourselves at the start of peer
//TODO:recovery. How do we know if original server has re-started and needs the lease to NOT be deleted? Or does it matter? ie if the lease
//TODO:is deleted by a peer, could the original server not simply (re)insert its own row?
    @Override
    public synchronized void deleteServerLease(String recoveryIdentity) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "deleteServerLease", new java.lang.Object[] { recoveryIdentity, this });

        Connection conn = null;
        Statement deleteStmt = null;

        try {
            // Have we acquired the reference to the DataSource yet?
            if (_theDS == null) {
                _theDS = getDataSourceFromProperties();
                // We've looked up the DS, so now we can get a JDBC connection
                if (_theDS != null) {
                    conn = getConnection(_theDS);
                }
            } else {
                // Try and get a new connection
                conn = _theDS.getConnection();
            }

            // If we were unable to get a connection, throw an exception
            if (conn == null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deleteServerLease", "Null connection InternalLogException");
                throw new InternalLogException("Failed to get JDBC Connection", null);
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Set autocommit FALSE on the connection");
            conn.setAutoCommit(false);

            // We can go ahead and write to the Database
            deleteStmt = conn.createStatement();

            String deleteString = "DELETE FROM " + _leaseTableName +
                                  " WHERE SERVER_IDENTITY='" + recoveryIdentity + "'";
            if (tc.isDebugEnabled())
                Tr.debug(tc, "delete server lease for " + recoveryIdentity + "using string " + deleteString);

            int ret = deleteStmt.executeUpdate(deleteString);

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Have deleted row with return: " + ret + ", commit the change");

            conn.commit();
        }
        // Catch and report an SQLException. In the finally block we'll determine whether the condition is transient or not.
        catch (SQLException sqlex) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Lease delete failed with exception: " + sqlex);
        } finally {

            if (deleteStmt != null && !deleteStmt.isClosed())
                deleteStmt.close();
            if (conn != null)
                conn.close();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "deleteServerLease");
    }

    /**
     * Read lease check if it is still expired. If so, then update lease and proceed to peer recover
     * if not still expired (someone else has grabbed it) then bypass peer recover.
     *
     * @param recoveryIdentityToRecover
     * @param myRecoveryIdentity
     * @throws Exception
     */
    @Override
    public synchronized boolean claimPeerLeaseForRecovery(String recoveryIdentityToRecover, String myRecoveryIdentity, LeaseInfo leaseInfo) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "claimPeerLeaseForRecovery", new java.lang.Object[] { recoveryIdentityToRecover, myRecoveryIdentity, leaseInfo, this });

        boolean peerClaimed = false;
        Connection conn = null;

        Statement lockingStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet lockingRS = null;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Recovering server with recoveryIdentity - ", recoveryIdentityToRecover);

        try {
            // Have we acquired the reference to the DataSource yet?
            if (_theDS == null) {
                _theDS = getDataSourceFromProperties();
                // We've looked up the DS, so now we can get a JDBC connection
                if (_theDS != null) {
                    conn = getConnection(_theDS);
                }
            } else {
                // Try and get a new connection
                conn = _theDS.getConnection();
            }

            // If we were unable to get a connection, throw an exception
            if (conn == null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "claimPeerLeaseForRecovery", "Null connection InternalLogException");
                throw new InternalLogException("Failed to get JDBC Connection", null);
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Set autocommit FALSE on the connection");
            conn.setAutoCommit(false);

            // We can go ahead and access the Database
            lockingStmt = conn.createStatement();

            try {
                String queryString = "SELECT LEASE_TIME" +
                                     " FROM " + _leaseTableName +
                                     " WHERE SERVER_IDENTITY='" + recoveryIdentityToRecover + "'" +
                                     (_isPostgreSQL ? "" : " FOR UPDATE OF LEASE_TIME");
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Attempt to select the row for UPDATE using - " + queryString);
                lockingRS = lockingStmt.executeQuery(queryString);
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Query failed with exception: " + e);
                throw new InternalLogException("Failed to query the lease time, exc: " + e, null);
            } // eof Exception e block

            // see if we acquired the row
            if (lockingRS.next()) {
                // We found the server row
                long storedLease = lockingRS.getLong(1);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Acquired server row, stored lease value is: " + Utils.traceTime(storedLease));

                // Has the lease expired?
                PeerLeaseData pld = new PeerLeaseData(recoveryIdentityToRecover, storedLease, _leaseTimeout);
                if (pld.isExpired()) {
                    // Lease has expired, this means that this server is attempting peer recovery and
                    // it has acquired the lock. If any other server is also trying to peer recover then
                    // they can fail. To make this happen, we'll update the lease time ourselves.

                    // Construct the UPDATE string
                    String updateString = "UPDATE " + _leaseTableName +
                                          " SET LEASE_TIME = ?, LEASE_OWNER = ?" +
                                          " WHERE SERVER_IDENTITY='" + recoveryIdentityToRecover + "'";
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "update lease for " + recoveryIdentityToRecover);

                    updateStmt = conn.prepareStatement(updateString);

                    // Set the Lease_time to the current time
                    //TODO:
                    long fir1 = System.currentTimeMillis();
                    updateStmt.setLong(1, fir1);
                    updateStmt.setString(2, myRecoveryIdentity);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Ready to UPDATE using string - " + updateString + " and time: " + Utils.traceTime(fir1));

                    int ret = updateStmt.executeUpdate();

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Have updated server row with return: " + ret);
                    peerClaimed = true;
                }
            } else {
                // We didn't find the row in the table
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Could not find row");
                throw new InternalLogException("Could not find lease", null);
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "COMMIT the change");
            conn.commit();
        }
        // Catch and report an SQLException.
        catch (SQLException sqlex) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Lease update failed with exception: " + sqlex);
        } finally {
            if (lockingRS != null && !lockingRS.isClosed())
                lockingRS.close();
            if (lockingStmt != null && !lockingStmt.isClosed())
                lockingStmt.close();
            if (updateStmt != null && !updateStmt.isClosed())
                updateStmt.close();
            if (conn != null)
                conn.close();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "claimPeerLeaseForRecovery");
        return peerClaimed;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#lockPeerLease(java.lang.String)
     */
    @Override
    public boolean lockPeerLease(String recoveryIdentity) {
        // Noop in RDBMS implementation
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#releasePeerLease(java.lang.String)
     */
    @Override
    public boolean releasePeerLease(String recoveryIdentity) throws Exception {
        // Noop in RDBMS implementation
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#lockLocalLease(java.lang.String)
     */
    @Override
    public boolean lockLocalLease(String recoveryIdentity) {
        // Noop in RDBMS implementation
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#releaseLocalLease(java.lang.String)
     */
    @Override
    public boolean releaseLocalLease(String recoveryIdentity) throws Exception {
        // Noop in RDBMS implementation
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#setPeerRecoveryLeaseTimeout(int)
     */
    @Override
    public void setPeerRecoveryLeaseTimeout(int leaseTimeout) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setPeerRecoveryLeaseTimeout", leaseTimeout);

        // Store the Lease Timeout
        _leaseTimeout = leaseTimeout;
    }

    /**
     * Signals to the Lease Log that the server is stopping.
     */
    @Override
    public void serverStopping() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serverStopping ", new Object[] { this });

        _serverStopping = true;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "serverStopping", this);
    }
}
