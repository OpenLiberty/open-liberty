/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.custom.jdbc.impl;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.sql.DataSource;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.recoverylog.spi.Configuration;
import com.ibm.ws.recoverylog.spi.CustomLogProperties;
import com.ibm.ws.recoverylog.spi.DistributedRecoveryLog;
import com.ibm.ws.recoverylog.spi.FailureScope;
import com.ibm.ws.recoverylog.spi.InternalLogException;
import com.ibm.ws.recoverylog.spi.InvalidRecoverableUnitException;
import com.ibm.ws.recoverylog.spi.Lock;
import com.ibm.ws.recoverylog.spi.LogAllocationException;
import com.ibm.ws.recoverylog.spi.LogClosedException;
import com.ibm.ws.recoverylog.spi.LogCorruptedException;
import com.ibm.ws.recoverylog.spi.LogCursor;
import com.ibm.ws.recoverylog.spi.LogCursorCallback;
import com.ibm.ws.recoverylog.spi.LogCursorImpl;
import com.ibm.ws.recoverylog.spi.LogIncompatibleException;
import com.ibm.ws.recoverylog.spi.LogProperties;
import com.ibm.ws.recoverylog.spi.MultiScopeLog;
import com.ibm.ws.recoverylog.spi.RLSUtils;
import com.ibm.ws.recoverylog.spi.RecoverableUnit;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSectionExistsException;
import com.ibm.ws.recoverylog.spi.RecoveryAgent;
import com.ibm.ws.recoverylog.spi.TraceConstants;
import com.ibm.ws.recoverylog.utils.RecoverableUnitIdTable;

//------------------------------------------------------------------------------
// Class: SQLMultiScopeRecoveryLog
//------------------------------------------------------------------------------
/**
 * <p>
 * The SQLMultiScopeRecoveryLog class implements the DistributedRecoveryLog interface and
 * provides support for controlling a specific recovery log on behalf of a client
 * service.
 * </p>
 * 
 * <p>
 * This class provides facilities for opening and closing the recovery log, as
 * well as access to the underlying RecoverableUnits from which it
 * is comprised.
 * </p>
 * 
 * This class also implements the LogCursorCallback interface. This interface
 * allows an instance of SQLMultiScopeRecoveryLog to be notified when the client service
 * invokes remove on a LogCursor created by this class. This is required in order
 * to allow this class to write corrisponding deletion records to the recovery
 * log.
 * </p>
 */
public class SQLMultiScopeRecoveryLog implements LogCursorCallback, MultiScopeLog {
    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(SQLMultiScopeRecoveryLog.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    /**
     * The RecoveryAgent object supplied by the client service that owns
     * the recovery log.
     */
    private final RecoveryAgent _recoveryAgent;

    /**
     * The name of the client service that owns the recovery log.
     */
    private final String _clientName;

    /**
     * The version number of the client service that owns the recovery log.
     */
    private final int _clientVersion;

    /**
     * The name under which the SQLMultiScopeRecoveryLog was registered.
     */
    private final String _logName;

    /**
     * The identity under which the SQLMultiScopeRecoveryLog was registered.
     */
    private final int _logIdentifier;
    private final String _logIdentifierString;
    /**
     * The name of the application server for which the recovery log has
     * been created.
     */
    private final String _serverName;

    /**
     * These properties carry the key LOG_DIRECTORY property which
     * holds the JDBC config data we'll use.
     */
    private final Properties _internalLogProperties;

    /**
     * The URLs that describe the JDBC driver location
     */
    private java.net.URL[] _urls;

    /**
     * The string that defines the JDBC URL.
     */
    private String _dbURL;

    /**
     * Are we working against an Oracle Database or DB2
     */
    volatile private boolean _isOracle = false;

    volatile private boolean _isDB2 = false;

    private boolean isolationFailureReported = false;

    /**
     * A map of recoverable units. Each recoverable unit is keyed by its identity.
     */
    private HashMap<Long, RecoverableUnit> _recoverableUnits;

    /**
     * Counter to track the number of times the recovery log must be closed by the
     * client service before the underlying log will actually be keypointed and
     * closed. This value is incremented by one each time the log is opened and
     * decremented by one each time the log is closed. When the counter reaches
     * zero during a close call, the underlying log is keypointed and closed. This
     * provides support for multiple open and closed calls to the same recovery
     * log by the client service.
     */
    private int _closesRequired;

    /**
     * A reference to the LogProperties object that defines the identity and physical
     * location of the recovery log.
     */
    private final CustomLogProperties _customLogProperties;

    /**
     * Flag indicating that the recovery log has suffered an internal error
     * that leaves the recovey log in an undefined or unmanageable state.
     * Once this flag is set, any interface method that could modify the disk
     * state is stopped with an InternalLogException. This protects the on-disk
     * information from damage and allows a subseqent server bounce to recover
     * correctly.
     */
    private boolean _failed;

    /**
     * Flag indicating that the recovery log represented by this instance is at
     * an incompatible level to this service. Once this flag is set, any interface
     * method that accesses the recovery log will be stopped with a
     * LogIncompatibleException.
     */
    private boolean _incompatible;

    /**
     * A RecoverableUnitId table used to assign the ID of each newly created
     * recoverable unit when no identity is specified on createRecoverableUnit
     */
    private RecoverableUnitIdTable _recUnitIdTable = new RecoverableUnitIdTable();

    /**
     * The 'traceId' string is output at key trace points to allow easy mapping
     * of recovery log operations to clients logs.
     */
    private final String _traceId;

    /**
     * Flag to indicate if the containment check should be bypassed inside the
     * recoverableUnits() method.
     */
    private final boolean _bypassContainmentCheck;

    private final String _recoveryTableName = "WAS_";
    private final String _recoveryIndexName = "IXWS";
    private String _recoveryTableNameSuffix = "";

    /**
    */
    FailureScope _failureScope;

    /**
     * These strings are used for Database table creation. DDL is
     * different for DB2 and Oracle.
     */
    private final String db2TablePreString = "CREATE TABLE ";
    private final String db2TablePostString = "( SERVER_NAME VARCHAR(128), " +
                                              "SERVICE_ID SMALLINT, " +
                                              "RU_ID BIGINT, " +
                                              "RUSECTION_ID BIGINT, " +
                                              "RUSECTION_DATA_INDEX SMALLINT, " +
                                              "DATA LONG VARCHAR FOR BIT DATA) ";

    private final String oracleTablePreString = "CREATE TABLE ";
    private final String oracleTablePostString = "( SERVER_NAME VARCHAR(128), " +
                                                 "SERVICE_ID SMALLINT, " +
                                                 "RU_ID NUMBER(19), " +
                                                 "RUSECTION_ID NUMBER(19), " +
                                                 "RUSECTION_DATA_INDEX SMALLINT, " +
                                                 "DATA BLOB) ";

    // It is possible to use the same INDEX creation DDL for both Oracle and DB2
    private final String indexPreString = "CREATE INDEX ";
    private final String indexPostString = "( \"RU_ID\" ASC, " +
                                           "\"SERVICE_ID\" ASC, " +
                                           "\"SERVER_NAME\" ASC) ";

    // Reference to the dedicated non-transactional datasource
    private DataSource _theDS = null;

    // Reserved connection for use specifically in shutdown processing
    private Connection _reservedConn = null;

    // counters for debug/info
    private int _inserts,
                    _updates,
                    _removes;
    /**
     * We only want one client at a time to attempt to create a new
     * Database table.
     */
    private static final Object _CreateTableLock = new Object();

    /**
     * Maintain a lists of statements that can be replayed if an HA
     * Database fails over.
     */
    private List<ruForReplay> _cachedInserts = new ArrayList<ruForReplay>();
    private List<ruForReplay> _cachedUpdates = new ArrayList<ruForReplay>();
    private List<ruForReplay> _cachedRemoves = new ArrayList<ruForReplay>();

    /**
     * Key Database Transient error and Failover codes that alert us
     * to a transient absence of a database connection. These are
     * culled from the RDBMS specific Helper classes in
     * SERV1\ws\code\adapter\src\com\ibm\websphere\rsadapter
     */
    private final int _db2TransientErrorCodes[] = { -1015, -1034, -1035, -6036, -30081, -30108, -1224, -1229, -518, -514, -30080, -924, -923, -906, -4498, -4499, -1776 };

    private final int _oracleTransientErrorCodes[] = { 20, 28, 1012, 1014, 1033, 1034, 1035, 1089, 1090, 1092, 3113, 3114, 12505, 12514, 12541, 12560,
                                                      12571, 17002, 17008, 17009, 17410, 17401, 17430, 25408, 24794, 17447, 30006 }; // N.B. POSITIVE - is that correct?
    private int _sqlTransientErrorCodes[];
    private final int DEFAULT_TRANSIENT_RETRY_SLEEP_TIME = 10000; // In milliseconds, ie 10 seconds
    private final int _transientRetrySleepTime;
    private final int DEFAULT_TRANSIENT_RETRY_ATTEMPTS = 180; // We'll keep retrying for 30 minutes. Excessive?
    private final int _transientRetryAttempts;
    private boolean sqlTransientErrorHandlingEnabled = false;

    /**
     * Flag to indicate whether the server is stopping.
     */
    private static boolean _serverStopping;

    /**
     * Used in exception reporting.
     */
    private Throwable _nonTransientExceptionAtOpen = null;
    private Throwable _nonTransientExceptionAtRuntime = null;
    volatile MultiScopeLog _associatedLog = null;
    volatile boolean _failAssociatedLog = false;

    private final String _currentProcessServerName;

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.SQLMultiScopeRecoveryLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Package access constructor for the creation of SQLMultiScopeRecoveryLog objects.
     * </p>
     * 
     * <p>
     * This method should only be called by the RecoveryLogManager class in response
     * to a <code>RecoveryLogManager.getRecoveryLog</code> call.
     * </p>
     * 
     * <p>
     * Additionally, the caller must provide a LogProperties object that defines
     * the identity and physical properties of the recovery log. Since this implementation
     * of the RecoveryLog interface currently only supports "file" based recovery
     * logs, the supplied object must be of type FileLogProperties.
     * </p>
     * 
     * @param fileLogProperties The identity and physical properties of the recovery log.
     * @param recoveryAgent The RecoveryAgent of the associated client service.
     * @param fs The FailureScope of the associated client service.
     */
    public SQLMultiScopeRecoveryLog(CustomLogProperties logProperties, RecoveryAgent recoveryAgent, FailureScope fs) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "SQLMultiScopeRecoveryLog", new Object[] { logProperties, recoveryAgent, fs, this });

        // Cache the supplied information
        _customLogProperties = logProperties;
        _recoveryAgent = recoveryAgent;

        // Extract further information about the identity and physical location of the required
        // recovery log.
        _logName = _customLogProperties.logName();
        _logIdentifier = _customLogProperties.logIdentifier();
        _logIdentifierString = logTypeFromInteger(_logIdentifier);
        _clientName = recoveryAgent.clientName();
        _clientVersion = recoveryAgent.clientVersion();
        _serverName = fs.serverName();
        _failureScope = fs;
        _internalLogProperties = _customLogProperties.properties();
        _urls = null;
        _dbURL = null;
        _currentProcessServerName = Configuration.fqServerName();

        // Set the parameters that define SQL Error retry behaviour
        _transientRetryAttempts = getTransientSQLErrorRetryAttempts().intValue();
        _transientRetrySleepTime = getTransientSQLErrorRetrySleepTime().intValue();

        // Set the counters to 0
        _inserts = 0;
        _updates = 0;
        _removes = 0;

        // Now output consolidated trace information regarding the configuration of this object.
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Recovery log belongs to server " + _serverName);
            Tr.debug(tc, "Recovery log created by client service " + _clientName + " at version " + _clientVersion);
            Tr.debug(tc, "Recovery log name is " + _logName);
            Tr.debug(tc, "Recovery log identifier is " + _logIdentifier);
            Tr.debug(tc, "Recovery log internal properties are " + _internalLogProperties);
            Tr.debug(tc, "FIS 114950");
        }

        Tr.audit(tc, "WTRN0108I: " +
                     "Use SQL RecoveryLog for " + _logName + " on server " + _serverName);

        // Set up the lists to hold values for INSERTs, UPDATEs, DELETEs should a DB transient error occur
        _cachedInserts = new ArrayList<ruForReplay>();
        _cachedUpdates = new ArrayList<ruForReplay>();
        _cachedRemoves = new ArrayList<ruForReplay>();

        // Now establish a 'traceId' string. This is output at key trace points to allow
        // easy mapping of recovery log operations to clients logs.
        _traceId = "SQLMultiScopeRecoveryLog:" + "serverName=" + _serverName + ":"
                   + "clientName=" + _clientName + ":"
                   + "clientVersion=" + _clientVersion + ":"
                   + "logName=" + _logName + ":"
                   + "logIdentifier=" + _logIdentifier + " @"
                   + System.identityHashCode(this);

        // Now we require a temporary solution to support the manual movement of a
        // recovery log file set from server a to server b on distributed. Since
        // the multi-scope recovery log can actually support the storage of records
        // from any number of servers there would be no logical owner of a given log
        // if we made use of this facility on distributed (currently only
        // do on z/OS). Since we make no use of this facility, we can assume that
        // the 'creator server name' in the header of the log is the log owner
        // and that there is no real need to enforce the failure scope check
        // in the recoverableUnits method. We must disable these checks in this
        // environment to ensure that this form of peer recovery is can operate.
        _bypassContainmentCheck = (!Configuration.HAEnabled() && (!Configuration.isZOS()));

        if (tc.isDebugEnabled())
            Tr.debug(tc, "_bypassContainmentCheck = " + _bypassContainmentCheck);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "SQLMultiScopeRecoveryLog", this);
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.openLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Open the recovery log. Before a recovery log may be used, it must be opened by
     * the client service. The first time a recovery log is opened, any data stored
     * in the DB will be used to reconstruct the RecoverableUnits and
     * RecoverableUnitSections that were active when the log was closed or the server
     * failed.
     * </p>
     * 
     * <p>
     * The client service may issue any number of openLog calls, but each must be
     * paired with a corresponding closeLog call. This allows common logic to be
     * executed on independent threads without any need to determine if a
     * recovery log is already open. For example, a recovery process may be in
     * progress on one thread whilst forward processing is being performed on
     * another. Both pieces of logic may issue their own openLog and closeLog
     * calls independently.
     * </p>
     * 
     * @exception LogCorruptedException The recovery log has become corrupted and
     *                cannot be opened.
     * @exception LogAllocationException The recovery log could not be created.
     * @exception InternalLogException An unexpected failure has occured.
     */
    @Override
    public synchronized void openLog() throws LogCorruptedException, LogAllocationException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "openLog", this);

        // If this recovery log instance has been marked as incompatible then throw an exception
        // accordingly.
        if (incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "InternalLogException");
            throw new InternalLogException(null);
        }

        // We need to throw an exception if an attempt is made to open the log when the server is stopping
        if (_serverStopping) {
            Tr.audit(tc, "WTRN0100E: " +
                         "Cannot open SQL RecoveryLog " + _logName + " for server " + _serverName + " as the server is stopping");
            InternalLogException ile = new InternalLogException("Cannot open the log as the server is stopping", null);
            markFailed(ile);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "InternalLogException");
            throw ile;
        }

        // The Database Connection
        Connection conn = null;

        // If this is the first time the recovery log has been opened during the current server run, then
        // obtain a connection to the underlying DB
        if (_closesRequired == 0) {
            // Allocate the map which holds the RecoverableUnit objects
            _recoverableUnits = new HashMap<Long, RecoverableUnit>();

            try {
                String fullLogDirectory = _internalLogProperties.getProperty("LOG_DIRECTORY");
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "fullLogDirectory = " + fullLogDirectory);

                if (!fullLogDirectory.contains("datasource")) {
                    // The absence of this string indicates a fully specified JDBC connection string has been specified
                    // by the administrator ** THIS IS RETAINED FOR TESTING PURPOSES ONLY **
                    conn = getConnFromTranLogDirString(fullLogDirectory);
                } else {
                    // The presence of this string means that a Data Source has been specified
                    conn = getConnection(fullLogDirectory); // DB2 = ny01DS, Oracle = nyoraDS
                }

                // If we were unable to get a connection, throw an exception
                if (conn == null) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "openLog", "Null connection InternalLogException");
                    throw new InternalLogException("Failed to get JDBC Connection", null);
                }

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Set autocommit FALSE on the connection");
                conn.setAutoCommit(false);

                //
                // FIRST PHASE DB PROCESSING: Take HA Lock if possible, create table + HA Lock row if necessary
                //
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "create a statement");
                Statement lockingStmt = conn.createStatement();

                boolean newTable = true;
                ResultSet lockingRS = null;
                boolean handleFailover = false;
                synchronized (_CreateTableLock) // Guard against trying to create a table from multiple threads
                {
                    int queryRetries = 0;
                    boolean tableCreated = false;
                    Exception excToCheck = null;
                    while (queryRetries < 3) {
                        try {
                            // If this flag is true, then we are assuming an HA DB may have failed over and so we need a new connection
                            if (handleFailover) {
                                // Try and get a new connection
                                conn = _theDS.getConnection();
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Acquired connection in Database retry scenario");
                                conn.setAutoCommit(false);
                                lockingStmt = conn.createStatement();
                            }

                            // Use RDBMS SELECT FOR UPDATE to lock table for recovery
                            String queryString = "SELECT SERVER_NAME" +
                                                 " FROM " + _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                                                 " WHERE RU_ID=" + "-1 FOR UPDATE";
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Attempt to select the HA LOCKING ROW for UPDATE - " + queryString);
                            lockingRS = lockingStmt.executeQuery(queryString);

                            // We can do recovery on this rs
                            newTable = false;
                            break;
                        } catch (Exception e) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Query failed with exception: " + e);

                            // Set the current exception to e
                            excToCheck = e;

                            try {
                                // Perhaps we couldn't find the table ... so attempt to create it
                                createDBTable(conn);

                                conn.commit();

                                tableCreated = true;
                            } catch (Exception ine) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Table Creation failed with exception: " + ine);
                                // Set the current exception to ine
                                excToCheck = ine;
                            }

                            // If table creation succeeded then we are done, otherwise retry both the query and the table creation step.
                            if (tableCreated) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Table Creation succeeded");
                                break;
                            } else {
                                // Attempt a rollback. If it fails, trace the failure but allow processing to continue
                                try {
                                    conn.rollback();
                                } catch (Throwable exc) {
                                    // Trace the exception
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Rollback Failed, after table creation failure, got exception: " + exc);
                                }
                                // The query failed. We'll retry as another process may have been in the process of creating the table.
                                queryRetries++;
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Table Creation failed, query retries: " + queryRetries);
                                if (queryRetries >= 2) {
                                    boolean allowHARetry = false;

                                    // We'll go into this block in the case where we are failing but have yet to check for failover
                                    if (!handleFailover) {
                                        if (tc.isDebugEnabled())
                                            Tr.debug(tc, "Have we encounted a DB HA FAilover?");
                                        // We may have encountered a DB HA failover
                                        if (excToCheck instanceof SQLException) {
                                            SQLException sqlex = (SQLException) excToCheck;
                                            if (sqlTransientErrorHandlingEnabled && isSQLErrorTransient(sqlex)) {
                                                allowHARetry = true;
                                                // We may have encountered a failover, so reset counters and retry
                                                queryRetries = 0;
                                                handleFailover = true;
                                            }
                                        }
                                    }

                                    if (!allowHARetry) {
                                        // We are really struggling, we retried in case we were failing to create a table and we have retried
                                        // in case the Database is HA and was failing over. So log an FFDC and rethrow the exception to allow
                                        // the server to "fail to start"
                                        FFDCFilter.processException(excToCheck, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.openLog", "48", this);
                                        throw excToCheck;
                                    }
                                } else
                                    Thread.sleep(1000);
                            }
                        }
                    }
                }

                // If we have not just created the table, then we need to update the HA Lock if we are operating against
                // another server's recovery logs.
                if (!newTable) {
                    boolean secondPhaseSuccess = false;
                    SQLException currentSqlEx = null;
                    try {
                        //
                        // SECOND PHASE DB PROCESSING: Update HA Lock row if necessary
                        //
                        updateHADBLock(conn, lockingStmt, lockingRS);

                        // We are ready to drive recovery
                        recover(conn);

                        conn.commit();
                        secondPhaseSuccess = true;
                    }
                    //NEW CODE FOR DB SWITCH GOES HERE
                    catch (SQLException sqlex) {
                        Tr.audit(tc, "WTRN0107W: " +
                                     "Caught SQLException when opening SQL RecoveryLog " + _logName + " for server " + _serverName + " SQLException: " + sqlex);
                        // Set the exception that will be reported
                        currentSqlEx = sqlex;
                    } finally {
                        if (conn != null) {
                            if (secondPhaseSuccess) {
                                // Close the connection
                                conn.close();
                            } else {
                                // Tidy up connection before dropping into handleOpenLogSQLException method
                                // Attempt a rollback. If it fails, trace the failure but allow processing to continue
                                try {
                                    conn.rollback();
                                } catch (Throwable exc) {
                                    // Trace the exception
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Rollback Failed, in second phase open log, got exception: " + exc);
                                }
                                // Attempt a close. If it fails, trace the failure but allow processing to continue
                                try {
                                    conn.close();
                                } catch (Throwable exc) {
                                    // Trace the exception
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Close Failed, in second phase open log, got exception: " + exc);
                                }

                                // The following method will set "_nonTransientExceptionAtOpen" if it cannot recover
                                boolean failAndReport = handleOpenLogSQLException(currentSqlEx);
                                // We've been through the while loop
                                if (failAndReport) {
                                    Tr.audit(tc, "WTRN0100E: " +
                                                 "Cannot recover from SQLException when opening SQL RecoveryLog " + _logName + " for server " + _serverName + " Exception: "
                                                 + _nonTransientExceptionAtOpen);
                                    markFailed(_nonTransientExceptionAtOpen);
                                    if (tc.isEntryEnabled())
                                        Tr.exit(tc, "openLog", "InternalLogException");
                                    throw new InternalLogException(_nonTransientExceptionAtOpen);
                                } else {
                                    Tr.audit(tc, "WTRN0108I: " +
                                                 "Have recovered from SQLException when opening SQL RecoveryLog " + _logName + " for server " + _serverName);
                                }

                            }
                        }
                    }
                }

                // Ensure that we have closed the DB connection
                if (conn != null) {
                    if (conn.isClosed()) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Connection is already closed");
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Closing DB Connection");
                        if (_reservedConn == null)
                            conn.close();
                    }
                } else if (tc.isDebugEnabled())
                    Tr.debug(tc, "Connection was NULL");
            } catch (SQLException exc) {
                // The code above should have checked if the log is open or not fron the point of view of this class.
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.openLog", "464", this);
                markFailed(exc); /* @MD19484C */
                _recoverableUnits = null;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "openLog", "InternalLogException");
                throw new InternalLogException(exc);
            } catch (Throwable exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.openLog", "500", this);
                if (tc.isEventEnabled())
                    Tr.event(tc, "Unexpected exception caught in openLog", exc);
                markFailed(exc); /* @MD19484C */
                _recoverableUnits = null;

                // Output full stack
                for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, " " + ste);
                }

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "openLog", "InternalLogException");
                throw new InternalLogException(exc);
            }
        }

        // In light of this open operation, the caller will need to issue an additional close operation to fully close
        // the recovey log.
        _closesRequired++;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Closes required: " + _closesRequired);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "openLog");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.getConnFromTranLogDirString
    //------------------------------------------------------------------------------
    /**
     * Extracts the config from the Transaction Log Directory String
     * where database parameters have been specified in name/value
     * pairs. Establish a jdbc connection using the parameters.
     * 
     * @return The Connection.
     * 
     * @exception
     */
    public Connection getConnFromTranLogDirString(String fullLogDirectory) throws LogCorruptedException, LogAllocationException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getConnFromTranLogDirString", new java.lang.Object[] { fullLogDirectory, this });

        Connection conn = null;

        try {
            // Parse the _internalLogProperties in order to extract the config parameters to connect to the database
            // The LOG_DIRECTORY will be of the form,
            //    DB2:  custom://com.ibm.rls.jdbc.SQLRecoveryLog?url=jdbc:db2://localhost:50000/SAMPLE,user=db2admin,
            //                                                  password=db2admin,dbdir=C:\\SQLLIB\\java
            // ORACLE:  custom://com.ibm.rls.jdbc.SQLRecoveryLog?url=jdbc:oracle:thin:@localhost:1521:orcl,user=scott,
            //                                                  password=tiger,dbdir=C:\\Oracle\\app\\Administrator\\product\\11.2.0\\dbhome_1\\jdbc\\lib
            //
            // Other DB2 URL specs might be "jdbc:db2\:WSTEST" or "jdbc:db2://moondb05.rtp.raleigh.ibm.com:50000/BFTESTDB"
            //
            // The dbdir property enables the location of the db2 jars so the driver can be found
            // On WINDOWS
            //          java.io.File file1 = new java.io.File("C:\\SQLLIB\\java\\db2jcc.jar");
            //          java.io.File file2 = new java.io.File("C:\\SQLLIB\\java\\db2jcc_license_cu.jar");
            //          java.io.File file3 = new java.io.File("C:\\SQLLIB\\java\\");
            //On UNIX
            //        java.io.File file1 = new java.io.File("/test/db2jcc.jar");
            //        java.io.File file2 = new java.io.File("/test/db2jcc_license_cu.jar");
            //        java.io.File file3 = new java.io.File("/test");

            // Set up RDBMS properties to be used by JDBC
            Properties dbProps = new Properties();

            // Parse the string we've been given
            parseLogDirectoryString(fullLogDirectory, dbProps);

            // Set up the class for the specific RDBMS implementation's JDBC driver
            Class cls = null;
            // Create a new class loader with the directory
            ClassLoader sqlDriverClassLoader = null;

            try {
                sqlDriverClassLoader = (ClassLoader) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() {
                        ClassLoader cl = new java.net.URLClassLoader(_urls);
                        return cl;
                    }
                });
            } catch (PrivilegedActionException e) {
                Tr.error(tc, e.getMessage());
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, e.getMessage(), e);
                }
            }

            if (_isOracle) {
                cls = sqlDriverClassLoader.loadClass("oracle.jdbc.OracleDriver");
            } else {
                cls = sqlDriverClassLoader.loadClass("com.ibm.db2.jcc.DB2Driver");
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "instantiate jdbc driver class = " + cls);
            // Class cls = Class.forName("com.ibm.db2.jcc.DB2Driver") //.newInstance();
            java.sql.Driver d = (java.sql.Driver) cls.newInstance();

            if (tc.isDebugEnabled())
                Tr.debug(tc, " **** Does this driver accept my URL? " + d.acceptsURL(_dbURL));
            DriverManager.registerDriver(d);

            // Connect to the Database
            conn = d.connect(_dbURL, dbProps);
        } catch (Throwable exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.openLog", "500", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "Unexpected exception caught in openLog", exc);
            markFailed(exc); /* @MD19484C */
            _recoverableUnits = null;

            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, " " + ste);
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getConnFromTranLogDirString", conn);
        return conn;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.parseLogDirectoryString
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Extract the information needed from the LogDirectoryString in
     * order to make a connection to the database via JDBC.
     * Specifically, we need to retrieve the appropriate database
     * properties and to derive the appropriate JDBC driver class.
     * </p>
     */
    public void parseLogDirectoryString(String fullLogDirectory, Properties dbProps) throws MalformedURLException, IOException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "parseLogDirectoryString", new java.lang.Object[] { fullLogDirectory, this });

        StringTokenizer st = new StringTokenizer(fullLogDirectory, "?");
        String cname = st.nextToken();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "cname = " + cname);

        // Extract the DB related properties
        String dbPropertiesString = st.nextToken();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "dbPropertiesString = " + dbPropertiesString);

        //Determine whether we are dealing with Oracle or DB2
        if (dbPropertiesString.contains("oracle")) {
            _isOracle = true;
            // We can set the transient error codes to watch for at this point too.
            _sqlTransientErrorCodes = _oracleTransientErrorCodes;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Configure a connection to an ORACLE database");
        } else if (dbPropertiesString.contains("DB2")) {
            // we are DB2
            _isDB2 = true;
            _sqlTransientErrorCodes = _db2TransientErrorCodes;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Configure a connection to a DB2 database");
            // Flag the we can tolerate transient SQL error codes
            sqlTransientErrorHandlingEnabled = true;
        } else {
            // Not DB2 or Oracle, cannot handle transient SQL errors
            if (tc.isDebugEnabled())
                Tr.debug(tc, "This is neither Oracle nor DB2");
        }

        dbProps.load(new StringReader(dbPropertiesString.replace(',', '\n')));
        if (tc.isDebugEnabled())
            Tr.debug(tc, "dbProps = " + dbProps);
        _dbURL = dbProps.getProperty("url");
        dbProps.remove("url");
        String dbDir = dbProps.getProperty("dbdir");
        dbProps.remove("dbdir");

        // Set us the 3 files
        String file3String = dbDir + "\\";
        String file1String = "";
        String file2String = "";
        if (_isOracle) {
            file1String = file3String + "ojdbc6.jar";
        } else {
            file1String = file3String + "db2jcc.jar";
            file2String = file3String + "db2jcc_license_cu.jar";
        }

        // Output DB related trace information
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "DB URL: " + _dbURL);
            Tr.debug(tc, "DB props: " + dbProps);
            Tr.debug(tc, "DB file1String: " + file1String);
            Tr.debug(tc, "DB file2String: " + file2String);
            Tr.debug(tc, "DB file3String: " + file3String);
        }

        // Set up files
        java.io.File file1 = new java.io.File(file1String);
        java.io.File file2 = new java.io.File(file2String);
        java.io.File file3 = new java.io.File(file3String);

        // Convert File to a URL
        java.net.URL url1 = file1.toURL();
        java.net.URL url2 = file2.toURL();
        java.net.URL url3 = file3.toURL();

        // Set up the urls
        if (_isOracle) {
            _urls = new java.net.URL[] { url1 };
        } else {
            _urls = new java.net.URL[] { url1, url2, url3 };
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "parseLogDirectoryString");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.getConnection
    //------------------------------------------------------------------------------
    /**
     * Locates a DataSource in config and establish a managed
     * connection
     * 
     * @return The Connection.
     * 
     * @exception
     */
    private Connection getConnection(String fullLogDirectory) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getConnection", fullLogDirectory);
        Connection conn = null;

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

        // Extract the optional tablesuffix
        String tableSuffix = dbStringProps.getProperty("tablesuffix");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Extracted Table Suffix = " + tableSuffix);

        if (tableSuffix != null && !tableSuffix.equals("")) {
            _recoveryTableNameSuffix = tableSuffix;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Full RecoveryTableName = " + _recoveryTableName + _recoveryTableNameSuffix);
        }

        // We'll look up the DataSource definition in jndi but jndi is initialising in another
        // thread so we need to handle the situation where a first lookup does not succeed. This
        // processing is wrapped up in the SQLNonTransactionalDataSource
        SQLNonTransactionalDataSource sqlNonTranDS = new SQLNonTransactionalDataSource(dsName, _customLogProperties);

        _theDS = sqlNonTranDS.getDataSource();

        // We've looked up the DS, so now we can get a JDBC connection
        if (_theDS != null) {
            // Get connection to database via first datasource
            conn = _theDS.getConnection();

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Got connection: " + conn);
            DatabaseMetaData mdata = conn.getMetaData();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Got metadata: " + mdata);
            String dbName = mdata.getDatabaseProductName();
            if (dbName.toLowerCase().contains("oracle")) {
                _isOracle = true;
                // We can set the transient error codes to watch for at this point too.
                _sqlTransientErrorCodes = _oracleTransientErrorCodes;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "This is an Oracle Database");
                // Flag the we can tolerate transient SQL error codes
                sqlTransientErrorHandlingEnabled = true;
            } else if (dbName.toLowerCase().contains("db2")) {
                // we are DB2
                _isDB2 = true;
                _sqlTransientErrorCodes = _db2TransientErrorCodes;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "This is a DB2 Database");
                // Flag the we can tolerate transient SQL error codes
                sqlTransientErrorHandlingEnabled = true;
            } else {
                // Not DB2 or Oracle, cannot handle transient SQL errors
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "This is neither Oracle nor DB2, it is " + dbName);
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
    // Method: SQLMultiScopeRecoveryLog.recover
    //------------------------------------------------------------------------------
    /**
     * Retrieves log records from the database ready for recovery
     * processing.
     * 
     * @exception SQLException thrown if a SQLException is
     *                encountered when accessing the
     *                Database.
     * @exception InternalLogException Thrown if an
     *                unexpected error has occured.
     */
    private void recover(Connection conn) throws SQLException, RecoverableUnitSectionExistsException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recover", conn);

        Statement recoveryStmt = null;
        ResultSet recoveryRS = null;

        try {
            recoveryStmt = conn.createStatement();
            String queryString = "SELECT RU_ID, RUSECTION_ID, RUSECTION_DATA_INDEX, DATA" +
                                 " FROM " + _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                                 " WHERE SERVER_NAME='" + _serverName +
                                 "' AND SERVICE_ID=" + _recoveryAgent.clientIdentifier();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Retrieve all rows from table using - " + queryString);

            recoveryRS = recoveryStmt.executeQuery(queryString);

            while (recoveryRS.next()) {
                final long ruId = recoveryRS.getLong(1);
                if (ruId != -1) {
                    SQLRecoverableUnitImpl ru = (SQLRecoverableUnitImpl) _recoverableUnits.get(ruId);
                    if (ru == null) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Creating ru with id: " + ruId);
                        ru = new SQLRecoverableUnitImpl(this, ruId, _failureScope, true);
                    }

                    final long sectId = recoveryRS.getLong(2);
                    final int index = recoveryRS.getInt(3);
                    final byte[] data = recoveryRS.getBytes(4);

                    if (tc.isEventEnabled()) {
                        Tr.event(tc, "sql tranlog: read ruId: " + ruId);
                        Tr.event(tc, "sql tranlog: read sectionId: " + sectId);
                        Tr.event(tc, "sql tranlog: read item: " + index);
                        Tr.event(tc, "sql tranlog: read data: " + RLSUtils.toHexString(data, RLSUtils.MAX_DISPLAY_BYTES));
                    }

                    SQLRecoverableUnitSectionImpl sect = (SQLRecoverableUnitSectionImpl) ru.lookupSection((int) sectId);
                    if (sect == null) {
                        sect = (SQLRecoverableUnitSectionImpl) ru.createSection((int) sectId, index == 0);
                    }

                    sect.addData(index, data);
                } else if (tc.isDebugEnabled())
                    Tr.debug(tc, "Bypass locking row with id: " + ruId);
            }
        } finally {
            if (recoveryRS != null && !recoveryRS.isClosed())
                recoveryRS.close();
            if (recoveryStmt != null && !recoveryStmt.isClosed())
                recoveryStmt.close();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recover");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.serviceData
    //------------------------------------------------------------------------------
    /**
     * Returns a copy of the service data or null if there is none defined. Changes to
     * the copy will have no affect on the service data stored by the RLS.
     * 
     * @return The service data.
     * 
     * @exception LogClosedException Thrown if the recovery log is closed and must
     *                be opened before this call can be issued.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    @Override
    public byte[] serviceData() throws LogClosedException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serviceData", this);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "serviceData", null);
        return null;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.recoveryComplete
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Informs the RLS that any outstanding recovery process for the recovery log is
     * complete. Client services may issue this call to give the RLS an opportunity
     * to optomize log access by performing a keypoint operation. Client services do
     * not have to issue this call.
     * </p>
     * 
     * <p>
     * This call is separate from the <code>RecoveryDirector.recoveryComplete</code>
     * method which must be invoked by a client service in response to a recovery
     * request. The RecoveryDirector callback indicates that sufficient recovery
     * processing has been performed to allow the request to be passed to the next
     * client service. The recovery process may however still execute on a separate
     * thread and call <code>RecoveryLog.recoveryComplete</code> when it has
     * finished.
     * </p>
     * 
     * @exception LogClosedException Thrown if the recovery log is closed and must
     *                be opened before this call can be issued.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                log that is not compatible with this version
     *                of the service.
     * 
     */
    @Override
    public void recoveryComplete() throws LogClosedException, InternalLogException, LogIncompatibleException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoveryComplete", this);

        // If this recovery log instance has been marked as incompatible then throw an exception
        // accordingly.
        if (incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", this);
            throw new InternalLogException(null);
        }

        // Check that the log is open.
        if (_closesRequired == 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", "LogClosedException");
            throw new LogClosedException(null);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoveryComplete");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.recoveryComplete
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Informs the RLS that any outstanding recovery process for the recovery log is
     * complete. Client services may issue this call to give the RLS an opportunity
     * to optomize log access by performing a keypoint operation. Client services do
     * not have to issue this call.
     * </p>
     * 
     * <p>
     * This call is separate from the <code>RecoveryDirector.recoveryComplete</code>
     * method which must be invoked by a client service in response to a recovery
     * request. The RecoveryDirector callback indicates that sufficient recovery
     * processing has been performed to allow the request to be passed to the next
     * client service. The recovery process may however still execute on a separate
     * thread and call <code>RecoveryLog.recoveryComplete</code> when it has
     * finished.
     * </p>
     * 
     * <p>
     * This extended version of the <code>RecoveryLog.recoveryCompelte()</code> method
     * allows the service data to be updated.
     * </p>
     * 
     * @param serviceData The updated service data.
     * 
     * @exception LogClosedException Thrown if the recovery log is closed and must
     *                be opened before this call can be issued.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                log that is not compatible with this version
     *                of the service.
     */
    @Override
    public void recoveryComplete(byte[] serviceData) throws LogClosedException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoveryComplete", new java.lang.Object[] { RLSUtils.toHexString(serviceData, RLSUtils.MAX_DISPLAY_BYTES), this });

        // If this recovery log instance has been marked as incompatible then throw an exception
        // accordingly.
        if (incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", this);
            throw new InternalLogException(null);
        }

        // Check that the log is open.
        if (_closesRequired == 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", "LogClosedException");
            throw new LogClosedException(null);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoveryComplete");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.closeLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Close the recovery log. The RLS will ensure that all active RecoverableUnits
     * and RecoverableUnitSections are stored persistently and, if possible that out
     * of date information is purged from the recovery log. The recovery log should
     * be opened again before further access.
     * </p>
     * 
     * <p>
     * Since the client service may issue any number of openLog calls, each must be
     * paired with a corrisponding closeLog call. This allows common logic to be
     * executed on independent threads without any need to determine if a
     * recovery log is already open. This model would typically be used when
     * different threads obtain the same RecoveryLog object through independant
     * calls to <p>RecoveryLogDirector.getRecoveryLog</p>. For example, a recovery
     * process may be in progress on one thread whilst forward processing is being
     * performed on another. Both pieces of logic may issue their own openLog and
     * closeLog calls independently.
     * </p>
     * 
     * <p>
     * Alternativly, the reference to a RecoveryLog may be shared directly around
     * the program logic or between threads. Using this model, a single openLog and
     * closeLog pair are required at well defined initialziation and shutdown points
     * in the client service.
     * </p>
     * 
     * <p>
     * This implementation of the RecoveryLog interface uses a simple counter
     * '_closesRequired' to track the number of times openLog has been called
     * and consiquentially the number of times closeLog must be invoked to
     * 'fully' close the recovery log.
     * <p>
     * 
     * <p>
     * This extended version of the <code>RecoveryLog.closeLog()</code> method
     * allows the service data to be updated prior to the close operation being
     * performed.
     * </p>
     * 
     * @param serviceData The updated service data.
     * 
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    @Override
    public void closeLog(byte[] serviceData) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "closeLog", new java.lang.Object[] { RLSUtils.toHexString(serviceData, RLSUtils.MAX_DISPLAY_BYTES), this });

        // Close the log.
        closeLog();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "closeLog");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.closeLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Close the recovery log. The RLS will ensure that all active RecoverableUnits
     * and RecoverableUnitSections are stored persistently and, if possible that out
     * of date information is purged from the recovery log. The recovery log should
     * be opened again before further access.
     * </p>
     * 
     * <p>
     * Since the client service may issue any number of openLog calls, each must be
     * paired with a corrisponding closeLog call. This allows common logic to be
     * executed on independent threads without any need to determine if a
     * recovery log is already open. This model would typically be used when
     * different threads obtain the same RecoveryLog object through independant
     * calls to <p>RecoveryLogDirector.getRecoveryLog</p>. For example, a recovery
     * process may be in progress on one thread whilst forward processing is being
     * performed on another. Both pieces of logic may issue their own openLog and
     * closeLog calls independently.
     * </p>
     * 
     * <p>
     * Alternativly, the reference to a RecoveryLog may be shared directly around
     * the program logic or between threads. Using this model, a single openLog and
     * closeLog pair are required at well defined initialziation and shutdown points
     * in the client service.
     * </p>
     * 
     * <p>
     * This implementation of the RecoveryLog interface uses a simple counter
     * '_closesRequired' to track the number of times openLog has been called
     * and consiquentially the number of times closeLog must be invoked to
     * 'fully' close the recovery log.
     * </p>
     * 
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    @Override
    public synchronized void closeLog() throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "closeLog", new Object[] { _reservedConn, this });
        boolean connAlreadyClosed = false;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Closes required: " + _closesRequired);

        if (_closesRequired > 0) {
            try {
                // The reserved connection should not be null. Attempt to open a connection now and throw an exception if
                // that fails.
                if (_reservedConn == null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Reserved Connection is NULL, attempt to get new DataSource connection");
                    if (!_serverStopping)
                        _reservedConn = _theDS.getConnection();
                    else {
                        Tr.audit(tc, "WTRN0100E: " +
                                     "Server stopping but no reserved connection when closing SQL RecoveryLog " + _logName + " for server " + _serverName);
                        InternalLogException ile = new InternalLogException("Server stopping, no reserved connection", null);
                        throw ile;
                    }
                }

                // Test whether the reserved connection is closed already.
                if (_reservedConn.isClosed()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Reserved Connection is already closed");
                    connAlreadyClosed = true;
                }
            } catch (Throwable exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.closeLog", "550", this);
                if (tc.isEventEnabled())
                    Tr.event(tc, "Unexpected exception caught in closeLog", exc);
                markFailed(exc); // @MD19484C
                _recoverableUnits = null;

                for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, " " + ste);
                }

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "closeLog", "InternalLogException");
                throw new InternalLogException(exc);
            }

            if (!connAlreadyClosed) {
                try {
                    internalKeypoint();
                } catch (LogClosedException exc) {
                    // The log is already closed so absorb the exception.
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.custom.jdbc.impl.SQLMultiScopeRecoveryLog.closeLog", "944", this);
                } catch (InternalLogException exc) {
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.custom.jdbc.impl.SQLMultiScopeRecoveryLog.closeLog", "948", this);
                    markFailed(exc); // @MD19484C
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "closeLog", exc);
                    throw exc;
                } catch (Throwable exc) {
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.custom.jdbc.impl.SQLMultiScopeRecoveryLog.closeLog", "955", this);
                    markFailed(exc); // @MD19484C

                    for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, " " + ste);
                    }

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "closeLog", "InternalLogException");
                    throw new InternalLogException(exc);
                }

                // Decrement the number of close operations required
                _closesRequired--;

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Closes required: " + _closesRequired);
                try {
                    if (_closesRequired <= 0) {
                        // Reset the internal state so that a subsequent open operation does not
                        // occurs with a "clean" environment.
                        _recoverableUnits = null;
                        _closesRequired = 0;
                        _failed = false;

                        _reservedConn.rollback();

                    }
                    // Close the reserved connection
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Close the Reserved Connection");

                    _reservedConn.close();
                } catch (Throwable exc) {
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.closeLog", "550", this);
                    if (tc.isEventEnabled())
                        Tr.event(tc, "Unexpected exception caught in closeLog", exc);
                    markFailed(exc); // @MD19484C
                    _recoverableUnits = null;

                    for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, " " + ste);
                    }

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "closeLog", "InternalLogException");
                    throw new InternalLogException(exc);
                }

                // Set the reserved connection to null
                _reservedConn = null;
            }
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Closes required: " + _closesRequired);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "closeLog");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.closeLogImmediate
    //------------------------------------------------------------------------------
    /**
    */
    @Override
    public void closeLogImmediate() throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "closeLogImmediate", this);

        closeLog();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "closeLogImmediate");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.createRecoverableUnit
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Create a new RecoverableUnit under which to write information to the recovery
     * log.
     * </p>
     * 
     * <p>
     * Information written to the recovery log is grouped by the service into a number
     * of RecoverableUnit objects, each of which is then subdivided into a number of
     * RecoverableUnitSection objects. Information to be logged is passed to a
     * RecoverableUnitSection in the form of a byte array.
     * </p>
     * 
     * <p>The identity of the recoverable unit will be allocated by the recovery log.
     * Use of this method <b>must not</b> be mixed with createRecoverableUnit(long)</p>
     * 
     * @return The new RecoverableUnit.
     * 
     * @exception LogClosedException Thrown if the recovery log is closed and must be
     *                opened before this call can be issued.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                log that is not compatible with this version
     *                of the service.
     */
    @Override
    public RecoverableUnit createRecoverableUnit(FailureScope failureScope) throws LogClosedException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoverableUnit", new Object[] { failureScope, this });

        // If this recovery log instance has been marked as incompatible then throw an exception
        // accordingly.
        if (incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createRecoverableUnit", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createRecoverableUnit", "InternalLogException");
            throw new InternalLogException(null);
        }

        // Check that the log is actually open.
        if (_closesRequired == 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createRecoverableUnit", "LogClosedException");
            throw new LogClosedException(null);
        }

        SQLRecoverableUnitImpl recoverableUnit = null;

        synchronized (this) {
            long identity = _recUnitIdTable.nextId(this);
            recoverableUnit = new SQLRecoverableUnitImpl(this, identity, failureScope);
            if (tc.isEventEnabled())
                Tr.event(tc, "SQLMultiScopeRecoveryLog '" + _logName + "' created a new RecoverableUnit with id '" + identity + "'");
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoverableUnit", recoverableUnit);
        return recoverableUnit;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.removeRecoverableUnit
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Remove a RecoverableUnit from the recovery logs set of active RecoverableUnits.
     * </p>
     * 
     * <p>
     * The RecoverableUnit and its associated RecoverableUnitSections are no longer
     * considered valid after this call. The client service must not invoke any further
     * methods on them.
     * </p>
     * 
     * <p>
     * The RLS will remove these objects from its "in memory" copy of the recovery
     * log and write (but not force) a record of this deletion to persistent storage.
     * This means that in the event of a server failure, removed objects may be still
     * be reconstructed during recovery processing and client services must be able
     * to cope with this. Any subsequent force operation will ensure that this
     * deletion record is persisted to disk and any subsequent keypoint operation
     * will remove all reference to the recoverable unit from the recovery log.
     * </p>
     * 
     * <p>
     * This method must not be invoked whilst an unclosed LogCursor is held (for either
     * all RecoverableUnits or this RecoverableUnits RecoverableUnitSections.) The
     * <code>LogCursor.remove</code> method should be used instead.
     * </p>
     * 
     * @param identity Identity of the RecoverableUnit to be removed.
     * 
     * @exception LogClosedException Thrown if the recovery log is closed and must be
     *                opened before this call can be issued.
     * @exception InvalidRecoverableUnitException Thrown if the RecoverableUnit does not exist.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                log that is not compatible with this version
     *                of the service.
     */
    @Override
    public void removeRecoverableUnit(long identity) throws LogClosedException, InvalidRecoverableUnitException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeRecoverableUnit", new Object[] { identity, this });

        // If this recovery log instance has been marked as incompatible then throw an exception
        // accordingly.
        if (incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", this);
            throw new InternalLogException(null);
        }

        // Ensure the log is actually open.
        if (_closesRequired == 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", "LogClosedException");
            throw new LogClosedException(null);
        }

        SQLRecoverableUnitImpl recoverableUnit = null;

        synchronized (this) {
            // REQD Thread safety
            recoverableUnit = removeRecoverableUnitMapEntries(identity);
        }

        // If the RecoverableUnit corresponding to 'identity' was not found in the map then throw an exception
        if (recoverableUnit == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", "InvalidRecoverableUnitException");
            throw new InvalidRecoverableUnitException(null);
        }

        // Inform the recoverable unit that it is being deleted. This enables it to write a "DELETED" entry in the
        // recovery log (unforced) to ensure that a previous instance of this recoverable unit does not get confused with
        // a newer instance (otherwise the recovey logic would have no way of distinguising between older, deleted information
        // and newer recoverable information)
        try {
            recoverableUnit.remove();

            // Store data in case a DB transient error occurs
            synchronized (this) {
                ruForReplay deleteRU = new ruForReplay(identity, 0, 0, null);
                _cachedRemoves.add(deleteRU);
                _removes++;
            }
        } catch (InternalLogException exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.removeRecoverableUnit", "1182", this);

            markFailed(exc); /* @MD19484C */

            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", exc);
            throw exc;
        } catch (Exception e) {
            FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.removeRecoverableUnit", "1186", this);

            markFailed(e); /* @MD19484C */

            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", e);
            throw new InternalLogException(e);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeRecoverableUnit");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.recoverableUnits
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Returns a LogCursor that can be used to itterate through all active
     * RecoverableUnits. The order in which they are returned is not defined.
     * 
     * z/OS requires that this method be synchronized because, in this environment,
     * there could be multiple servants accessing the log at the same time. @MD19706.
     * </p>
     * 
     * <p>
     * The LogCursor must be closed when it is no longer needed or its itteration
     * is complete. (See the LogCursor class for more information)
     * </p>
     * 
     * <p>
     * Objects returned by <code>LogCursor.next</code> or <code>LogCursor.last</code>
     * must be cast to type RecoverableUnit.
     * </p>
     * 
     * <p>
     * Care must be taken not remove or add recoverable units whilst the resulting
     * LogCursor is open. Doing so will result in a ConcurrentModificationException
     * being thrown.
     * </p>
     * 
     * @return A LogCursor that can be used to itterate through all active
     *         RecoverableUnits.
     * 
     * @exception LogClosedException Thrown if the recovery log is closed and must be
     *                opened before this call can be issued.
     */
    @Override
    public synchronized LogCursor recoverableUnits(FailureScope failureScope) throws LogClosedException /* @MD19706C */
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoverableUnits", new Object[] { failureScope, this });

        // Check that the log is actually open
        if (_closesRequired == 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoverableUnits", "LogClosedException");
            throw new LogClosedException(null);
        }

        final List<SQLRecoverableUnitImpl> recoverableUnits = new ArrayList<SQLRecoverableUnitImpl>();

        // No need to access this inside a sync block as the caller is required to
        // hold off from changing the underlying structures whilst the cursor is open.
        final Iterator iterator = _recoverableUnits.values().iterator();

        while (iterator.hasNext()) {
            final SQLRecoverableUnitImpl recoverableUnit = (SQLRecoverableUnitImpl) iterator.next();

            if (_bypassContainmentCheck || (recoverableUnit.failureScope().isContainedBy(failureScope))) {
                recoverableUnits.add(recoverableUnit);
            }
        }

        final LogCursor cursor = new LogCursorImpl((Lock) null, recoverableUnits, true, this);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoverableUnits", cursor);

        return cursor;
    }

    // Added to support RecoveryLog interface so that RecoveryLogImpl can store all types of RecoveryLog (eg new SQL log)
    @Override
    public LogCursor recoverableUnits() throws LogClosedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoverableUnits", this);

        final LogCursor cursor = recoverableUnits(_failureScope);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoverableUnits", cursor);

        return cursor;
    }

    //TODO: remove the getRecoverableUnit method ... this is the 'interface' method
    @Override
    public RecoverableUnit lookupRecoverableUnit(long identity) throws LogClosedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "lookupRecoverableUnit", new Object[] { Long.valueOf(identity), this });

        RecoverableUnit runit = getRecoverableUnit(identity);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "lookupRecoverableUnit", runit);
        return runit;
    }

    @Override
    public RecoverableUnit createRecoverableUnit() throws LogClosedException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoverableUnit", this);

        final RecoverableUnit runit = createRecoverableUnit(_failureScope);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoverableUnit", runit);
        return runit;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.logProperties
    //------------------------------------------------------------------------------
    /**
     * Returns the LogProperties object that defines the physical nature and identity
     * of the associated recovery log.
     * 
     * @return The LogProperties object.
     */
    @Override
    public LogProperties logProperties() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "logProperties", _customLogProperties);
        return _customLogProperties;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.keypoint
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Instructs the recovery log to perfom a keypoint operation. Any redundant
     * information will be removed and all cached information will be forced to disk.
     * </p>
     * 
     * @exception LogClosedException Thrown if the log is closed.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                log that is not compatible with this version
     *                of the service.
     */
    @Override
    public void keypoint() throws LogClosedException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "keypoint", this);

        internalKeypoint();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "keypoint");
    }

    public void internalKeypoint() throws LogClosedException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "internalKeypoint", new java.lang.Object[] { this, _reservedConn });

        // If this recovery log instance has been marked as incompatible then throw an exception
        // accordingly.
        if (incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "internalKeypoint", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "internalKeypoint", this);
            throw new InternalLogException(null);
        }

        // Check that the log is open.
        if (_closesRequired == 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "internalKeypoint", "LogClosedException");
            throw new LogClosedException(null);
        }

        try {
            internalForceSections();
        } catch (Throwable exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.internalKeypoint", "537", this);

            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, " " + ste);
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "internalKeypoint", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "internalKeypoint");
    }

    public void writeRUSection(long ruId, long sectionId, int index, byte[] data) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writeRUSection ", new Object[] { ruId, sectionId, index, data, this });

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing. The log should not be able to get to this point if its not compatible (as this method is only
        // called as a result of a remove on the LogCursor provided by the recoverableUnits method and this method
        // will generate a LogIncompatibleException in that event to prevent the cursor from being provided to the
        // caller. As a result, an incompatible log at this point is actually a code error of some form so
        // treat it in the same way as failure.
        if (failed() || incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeRUSection", this);
            throw new InternalLogException(null);
        }

        try {
            internalWriteRUSection(ruId, sectionId, index, data, false);
        } catch (Exception exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.writeRUSection", "1581", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "An unexpected error occured whilst writing data");
            markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeRUSection", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "writeRUSection");
    }

    public synchronized void internalWriteRUSection(long ruId, long sectionId, int index, byte[] data, boolean replaying) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "internalWriteRUSection ", new Object[] { ruId, sectionId, index, data, replaying, this });

        if (tc.isEventEnabled()) {
            Tr.event(tc, "sql tranlog: writing ruId: " + ruId);
            Tr.event(tc, "sql tranlog: writing sectionId: " + sectionId);
            Tr.event(tc, "sql tranlog: writing item: " + index);
            Tr.event(tc, "sql tranlog: writing data: " + RLSUtils.toHexString(data, RLSUtils.MAX_DISPLAY_BYTES));
        }

        ruForReplay insertRU = new ruForReplay(ruId, sectionId, index, data);
        _cachedInserts.add(insertRU);
        _inserts++;
        if (tc.isEntryEnabled())
            Tr.exit(tc, "internalWriteRUSection");
    }

    public void updateRUSection(long ruId, long sectionId, byte[] data) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "updateRUSection ", new Object[] { ruId, sectionId, data, this });

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing. The log should not be able to get to this point if its not compatible (as this method is only
        // called as a result of a remove on the LogCursor provided by the recoverableUnits method and this method
        // will generate a LogIncompatibleException in that event to prevent the cursor from being provided to the
        // caller. As a result, an incompatible log at this point is actually a code error of some form so
        // treat it in the same way as failure.
        if (failed() || incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "updateRUSection", this);
            throw new InternalLogException(null);
        }

        if (tc.isEventEnabled()) {
            Tr.event(tc, "sql tranlog: writing ruId: " + ruId);
            Tr.event(tc, "sql tranlog: writing sectionId: " + sectionId);
            Tr.event(tc, "sql tranlog: writing data: " + RLSUtils.toHexString(data, RLSUtils.MAX_DISPLAY_BYTES));
        }

        try {
            internalUpdateRUSection(ruId, sectionId, data, false);
        } catch (Exception exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.updateRUSection", "1581", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "An unexpected error occured whilst updating a RecoverableUnit");
            markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "updateRUSection", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "updateRUSection");
    }

    public synchronized void internalUpdateRUSection(long ruId, long sectionId, byte[] data, boolean replaying) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "internalUpdateRUSection ", new Object[] { ruId, sectionId, data, replaying, this });

        if (tc.isEventEnabled()) {
            Tr.event(tc, "sql tranlog: updating ruId: " + ruId);
            Tr.event(tc, "sql tranlog: updating sectionId: " + sectionId);
            Tr.event(tc, "sql tranlog: updating data: " + RLSUtils.toHexString(data, RLSUtils.MAX_DISPLAY_BYTES));
        }

        ruForReplay updateRU = new ruForReplay(ruId, sectionId, 0, data);
        _cachedUpdates.add(updateRU);
        _updates++;
        if (tc.isEntryEnabled())
            Tr.exit(tc, "internalUpdateRUSection");
    }

    public synchronized void forceSections() throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "forceSections", new java.lang.Object[] { this });

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "forceSections", this);
            throw new InternalLogException(null);
        }

        try {
            internalForceSections();
        } catch (Throwable exc) {

            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.forceSections", "537", this);

            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, " " + ste);
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "forceSections", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "forceSections");
    }

    @FFDCIgnore({ SQLException.class, SQLRecoverableException.class })
    void internalForceSections() throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "internalForceSections", new java.lang.Object[] { this, _reservedConn });

        Connection conn = null;
        boolean sqlSuccess = false;
        SQLException currentSqlEx = null;
        int initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;
        try {
            // Get a connection to database via its datasource
            if (_reservedConn == null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Reserved Connection is NULL, attempt to get new DataSource connection");
                if (!_serverStopping)
                    conn = _theDS.getConnection();
                else {
                    Tr.audit(tc, "WTRN0100E: " +
                                 "Server stopping but no reserved connection when forcing SQL RecoveryLog " + _logName + " for server " + _serverName);
                    InternalLogException ile = new InternalLogException("Server stopping, no reserved connection", null);
                    markFailed(ile);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "forceSections", "InternalLogException");
                    throw ile;
                }
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Drive SQL using reserved connection");
                conn = _reservedConn;
            }

            // This next piece of logic uses the HA Lock scheme to detect whether another process has taken over this server's
            // logs. Use RDBMS SELECT FOR UPDATE to lock table and access the HA Lock row in the table.
            initialIsolation = prepareConnectionForBatch(conn);
            takeHADBLock(conn);

            // We can go ahead and write to the Database
            executeBatchStatements(conn);

            conn.commit();
            sqlSuccess = true;
        }
        // Catch and report an SQLException. In the finally block we'll determine whether the condition is transient or not.
        catch (SQLException sqlex) {
            Tr.audit(tc, "WTRN0107W: " +
                         "Caught SQLException when forcing SQL RecoveryLog " + _logName + " for server " + _serverName + " SQLException: " + sqlex);
            // Set the exception that will be reported
            currentSqlEx = sqlex;
            if (conn == null)
                _nonTransientExceptionAtRuntime = sqlex;
        } catch (Throwable exc) {
            Tr.audit(tc, "WTRN0107W: " +
                         "Caught non-SQLException Throwable when forcing SQL RecoveryLog " + _logName + " for server " + _serverName + " Throwable: " + exc);
            _nonTransientExceptionAtRuntime = exc;
        } finally {
            if (conn != null) {
                if (sqlSuccess) {
                    // Don't want to close the reserved connection
                    if (_reservedConn == null)
                        closeConnectionAfterBatch(conn, initialIsolation);
                } else {
                    // Tidy up current connection before dropping into handleForceSectionsSQLException method
                    // Attempt a rollback. If it fails, trace the failure but allow processing to continue
                    try {
                        conn.rollback();
                    } catch (SQLRecoverableException sqlrecexc) {
                        // Trace the exception
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Rollback Failed, after force sections failure, got SQL Recoverable Exception: " + sqlrecexc);
                    } catch (Throwable exc) {
                        // Trace the exception
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Rollback Failed, after force sections failure, got exception: " + exc);
                    }

                    // Attempt a close. If it fails, trace the failure but allow processing to continue
                    // Don't want to close the reserved connection
                    if (_reservedConn == null) {
                        try {
                            closeConnectionAfterBatch(conn, initialIsolation);
                        } catch (SQLRecoverableException sqlrecexc) {
                            // Trace the exception
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Close Failed, after force sections failure, got SQL Recoverable Exception: " + sqlrecexc);
                        } catch (Throwable exc) {
                            // Trace the exception
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Close Failed, after force sections failure, got exception: " + exc);
                        }
                    }

                    // The following method will set "_nonTransientExceptionAtRuntime" if it cannot recover
                    boolean failAndReport = true;
                    if (currentSqlEx != null)
                        failAndReport = handleForceSectionsSQLException(currentSqlEx);

                    // We've been through the while loop
                    if (failAndReport) {
                        Tr.audit(tc, "WTRN0100E: " +
                                     "Cannot recover from SQLException when forcing SQL RecoveryLog " + _logName + " for server " + _serverName + " Exception: "
                                     + _nonTransientExceptionAtRuntime);
                        markFailed(_nonTransientExceptionAtRuntime);
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "forceSections", "InternalLogException");
                        throw new InternalLogException(_nonTransientExceptionAtRuntime);
                    } else
                        Tr.audit(tc, "WTRN0108I: " +
                                     "Have recovered from SQLException when forcing SQL RecoveryLog " + _logName + " for server " + _serverName);
                }
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Connection was NULL");
                Tr.audit(tc, "WTRN0100E: " +
                             "Cannot recover from SQLException when forcing SQL RecoveryLog " + _logName + " for server " + _serverName + " Exception: "
                             + _nonTransientExceptionAtRuntime);
                markFailed(_nonTransientExceptionAtRuntime);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "forceSections", "InternalLogException");
                throw new InternalLogException(_nonTransientExceptionAtRuntime);
            }

            // Ensure that we have cleared the replayable caches
            _cachedInserts.clear();
            _cachedUpdates.clear();
            _cachedRemoves.clear();
            _inserts = 0;
            _updates = 0;
            _removes = 0;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "internalForceSections");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.isSQLErrorTransient
    //------------------------------------------------------------------------------
    /**
     * Determine whether we have encountered a potentially transient
     * SQL error condition. If so we can retry the SQL.
     * 
     * @return true if the error is transient.
     */
    private boolean isSQLErrorTransient(SQLException sqlex) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isSQLErrorTransient ", new Object[] { sqlex, this });
        boolean retryBatch = false;
        int sqlErrorCode = sqlex.getErrorCode();

        if (tc.isEventEnabled()) {
            Tr.event(tc, " SQL exception:");
            Tr.event(tc, " Message: " + sqlex.getMessage());
            Tr.event(tc, " SQLSTATE: " + sqlex.getSQLState());
            Tr.event(tc, " Error code: " + sqlErrorCode);
        }

        for (int transientCode : _sqlTransientErrorCodes) {
            Tr.event(tc, "Test against stored code: " + transientCode);
            if (transientCode == sqlErrorCode) {
                Tr.event(tc, "TRANSIENT: A connection failed but could be reestablished, retry.");
                retryBatch = true;
                break;
            }
        }

        if (!retryBatch && sqlex instanceof BatchUpdateException) {
            BatchUpdateException buex = (BatchUpdateException) sqlex;
            Tr.event(tc, "BatchUpdateException: Update Counts - ");
            int[] updateCounts = buex.getUpdateCounts();
            for (int i = 0; i < updateCounts.length; i++) {
                Tr.event(tc, "   Statement " + i + ":" + updateCounts[i]);
            }
            SQLException nextex = buex.getNextException();
            while (nextex != null) {
                sqlErrorCode = nextex.getErrorCode();
                if (tc.isEventEnabled()) {
                    Tr.event(tc, " SQL exception:");
                    Tr.event(tc, " Message: " + nextex.getMessage());
                    Tr.event(tc, " SQLSTATE: " + nextex.getSQLState());

                    Tr.event(tc, " Error code: " + sqlErrorCode);
                }

                for (int transientCode : _sqlTransientErrorCodes) {
                    Tr.event(tc, "Test against stored code: " + transientCode);
                    if (transientCode == sqlErrorCode) {
                        Tr.event(tc, "TRANSIENT: A connection failed but could be reestablished, retry.");
                        retryBatch = true;
                        break;
                    }
                }

                nextex = nextex.getNextException();
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isSQLErrorTransient", retryBatch);
        return retryBatch;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.executeBatchStatements
    //------------------------------------------------------------------------------
    /**
     * Drives the execution of the cached up database work.
     * 
     * @exception SQLException thrown if a SQLException is
     *                encountered when accessing the
     *                Database.
     */
    private void executeBatchStatements(Connection conn) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "executeBatchStatements", new java.lang.Object[] { conn, this });

        PreparedStatement insertStatement = null;
        PreparedStatement updateStatement = null;
        PreparedStatement removeStatement = null;

        try {
            // Prepare the statements
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Prepare the INSERT statement for " + _inserts + " inserts");

            if (_inserts > 0) {
                String insertString = "INSERT INTO " + _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                                      " (SERVER_NAME, SERVICE_ID, RU_ID, RUSECTION_ID, RUSECTION_DATA_INDEX, DATA)" +
                                      " VALUES (?,?,?,?,?,?)";
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "INSERT string - " + insertString);
                insertStatement = conn.prepareStatement(insertString);
                insertStatement.setString(1, _serverName);
                insertStatement.setShort(2, (short) _recoveryAgent.clientIdentifier());
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Prepare the UPDATE statement for " + _updates + " updates");

            if (_updates > 0) {
                String updateString = "UPDATE " + _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                                      " SET DATA = ? WHERE " +
                                      "SERVER_NAME = ? AND SERVICE_ID = ? AND RU_ID = ? AND RUSECTION_ID = ? AND RUSECTION_DATA_INDEX = 0";
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "UPDATE string - " + updateString);
                updateStatement = conn.prepareStatement(updateString);
                updateStatement.setString(2, _serverName);
                updateStatement.setShort(3, (short) _recoveryAgent.clientIdentifier());
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Prepare the DELETE statement for " + _removes + " removes");

            if (_removes > 0) {
                String removeString = "DELETE FROM " + _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix + " WHERE " +
                                      "SERVER_NAME = ? AND SERVICE_ID = ? AND RU_ID = ? ";
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "DELETE string - " + removeString);
                removeStatement = conn.prepareStatement(removeString);
                removeStatement.setString(1, _serverName);
                removeStatement.setShort(2, (short) _recoveryAgent.clientIdentifier());
            }

            // Batch the INSERT statements
            if (_inserts > 0) {
                for (ruForReplay element : _cachedInserts) {
                    insertStatement.setLong(3, element.getRuId());
                    insertStatement.setLong(4, element.getSectionId());
                    insertStatement.setShort(5, (short) element.getIndex());
                    insertStatement.setBytes(6, element.getData());

                    insertStatement.addBatch();
                }
            }

            // Batch the UPDATE statements
            if (_updates > 0) {
                for (ruForReplay element : _cachedUpdates) {
                    updateStatement.setLong(4, element.getRuId());
                    updateStatement.setLong(5, element.getSectionId());
                    updateStatement.setBytes(1, element.getData());

                    updateStatement.addBatch();
                }
            }

            // Batch the DELETE statements
            if (_removes > 0) {
                for (ruForReplay element : _cachedRemoves) {
                    removeStatement.setLong(3, element.getRuId());
                    removeStatement.addBatch();
                }
            }

            // Execute the statements
            if (_inserts > 0) {
                int[] numUpdates = insertStatement.executeBatch();
                if (tc.isDebugEnabled()) {
                    for (int i = 0; i < numUpdates.length; i++) {
                        if (numUpdates[i] == Statement.SUCCESS_NO_INFO)
                            Tr.debug(tc, "Execution " + i + ": unknown number of rows updated");
                        else
                            Tr.debug(tc, "Execution " + i + "successful: " + numUpdates[i] + " rows updated");
                    }
                }
            }
            if (tc.isEventEnabled())
                Tr.event(tc, "sql tranlog: batch inserts: " + _inserts);

            if (_updates > 0) {
                int[] numUpdates = updateStatement.executeBatch();
                if (tc.isDebugEnabled()) {
                    for (int i = 0; i < numUpdates.length; i++) {
                        if (numUpdates[i] == Statement.SUCCESS_NO_INFO)
                            Tr.debug(tc, "Execution " + i + ": unknown number of rows updated");
                        else
                            Tr.debug(tc, "Execution " + i + "successful: " + numUpdates[i] + " rows updated");
                    }
                }
            }
            if (tc.isEventEnabled())
                Tr.event(tc, "sql tranlog: batch updates: " + _updates);

            if (_removes > 0) {
                int[] numUpdates = removeStatement.executeBatch();
                if (tc.isDebugEnabled()) {
                    for (int i = 0; i < numUpdates.length; i++) {
                        if (numUpdates[i] == Statement.SUCCESS_NO_INFO)
                            Tr.debug(tc, "Execution " + i + ": unknown number of rows updated");
                        else
                            Tr.debug(tc, "Execution " + i + "successful: " + numUpdates[i] + " rows updated");
                    }
                }
            }
            if (tc.isEventEnabled())
                Tr.event(tc, "sql tranlog: batch deletes: " + _removes + ", for obj: " + this);
        } finally {
            if (insertStatement != null && !insertStatement.isClosed())
                insertStatement.close();
            if (updateStatement != null && !updateStatement.isClosed())
                updateStatement.close();
            if (removeStatement != null && !removeStatement.isClosed())
                removeStatement.close();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "executeBatchStatements");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.handleForceSectionsSQLException
    //------------------------------------------------------------------------------
    /**
     * Attempts to replay the cached up SQL work if an error is
     * encountered during force processing and if the error is
     * determined to be transient.
     * 
     * @return true if the error cannot be handled and should be
     *         reported.
     */
    private boolean handleForceSectionsSQLException(SQLException sqlex) throws InterruptedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "handleForceSectionsSQLException", new java.lang.Object[] { sqlex, this });

        boolean retryBatch = true;
        boolean failAndReport = false;
        int batchRetries = 0;

        // Set the exception that will be reported
        _nonTransientExceptionAtRuntime = sqlex;

        while (retryBatch && !failAndReport && batchRetries < _transientRetryAttempts) {
            // Should we attempt to reconnect? This method works through the set of SQL exceptions and will
            // return TRUE if we determine that a transient DB error has occurred
            retryBatch = sqlTransientErrorHandlingEnabled && isSQLErrorTransient(sqlex);
            batchRetries++;
            if (retryBatch) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Try to reexecute the SQL using connection from DS: " + _theDS + ", attempt number: " + batchRetries);
                if (_theDS != null) {
                    Connection conn = null;
                    int initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;
                    // Re-execute the SQL
                    try {
                        // Get a connection to database via its datasource
                        conn = _theDS.getConnection();

                        // Take the HA DB lock and then reexecute the batch
                        initialIsolation = prepareConnectionForBatch(conn);
                        takeHADBLock(conn);
                        executeBatchStatements(conn);

                        conn.commit();
                        // The Batch has executed successfully and we can continue processing
                        retryBatch = false;
                    } catch (SQLException sqlex2) {
                        // We've caught another SQLException. Assume that we've retried the connection too soon.
                        // Make sure we inspect the latest exception
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "reset the sqlex to " + sqlex2);
                        sqlex = sqlex2;
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "sleeping for " + _transientRetrySleepTime + " millisecs");
                        Thread.sleep(_transientRetrySleepTime);
                    } catch (Throwable exc) {
                        // Not a SQLException, break out of the loop and report the exception
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Failed got exception: " + exc);
                        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, " " + ste);
                        }

                        failAndReport = true;
                        _nonTransientExceptionAtRuntime = exc;
                    } finally {
                        if (conn != null) {
                            if (retryBatch) {
                                // Attempt a rollback. If it fails, trace the failure but allow processing to continue
                                try {
                                    conn.rollback();
                                } catch (Throwable exc) {
                                    // Trace the exception
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Rollback Failed, when handling ForceSections SQLException, got exception: " + exc);
                                }
                            }
                            // Attempt a close. If it fails, trace the failure but allow processing to continue
                            try {
                                closeConnectionAfterBatch(conn, initialIsolation);
                            } catch (Throwable exc) {
                                // Trace the exception
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Close Failed, when handling ForceSections SQLException, got exception: " + exc);
                            }
                        } else if (tc.isDebugEnabled())
                            Tr.debug(tc, "Connection was NULL");
                    }
                } else {
                    // This is unexpected and catastrophic, the reference to the DataSource is null
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "NULL DataSource reference");
                    failAndReport = true;
                }
            } else
                failAndReport = true;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "handleForceSectionsSQLException", failAndReport);
        return failAndReport;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.handleOpenLogSQLException
    //------------------------------------------------------------------------------
    /**
     * Attempts to replay the cached up SQL work if an error is
     * encountered during open log processing and if the error is
     * determined to be transient.
     * 
     * @return true if the error cannot be handled and should be
     *         reported.
     */
    private boolean handleOpenLogSQLException(SQLException sqlex) throws InterruptedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "handleOpenLogSQLException", new java.lang.Object[] { sqlex, this });

        boolean retryBatch = true;
        boolean failAndReport = false;
        int batchRetries = 0;

        // Set the exception that will be reported
        _nonTransientExceptionAtOpen = sqlex;

        Connection conn = null;

        while (retryBatch && !failAndReport && batchRetries < _transientRetryAttempts) {
            // Should we attempt to reconnect? This method works through the set of SQL exceptions and will
            // return TRUE if we determine that a transient DB error has occurred
            retryBatch = sqlTransientErrorHandlingEnabled && isSQLErrorTransient(sqlex);
            batchRetries++;
            if (retryBatch) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Try to reexecute the SQL using connection from DS: " + _theDS + ", attempt number: " + batchRetries);
                if (_theDS != null) {
                    // Re-execute the SQL
                    try {
                        // Get a connection to database via its datasource
                        conn = _theDS.getConnection();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Acquired connection in Database retry scenario");
                        conn.setAutoCommit(false);
                        Statement lockingStmt = conn.createStatement();

                        // Use RDBMS SELECT FOR UPDATE to lock table for recovery
                        String queryString = "SELECT SERVER_NAME" +
                                             " FROM " + _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                                             " WHERE RU_ID=" + "-1 FOR UPDATE";
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Attempt to select the HA LOCKING ROW for UPDATE - " + queryString);
                        ResultSet lockingRS = lockingStmt.executeQuery(queryString);
                        // Update the HA DB lock and then recover
                        updateHADBLock(conn, lockingStmt, lockingRS);

                        // Clear out recovery caches in case they were partially filled before the failure.
                        _recoverableUnits.clear();
                        _recUnitIdTable = new RecoverableUnitIdTable();

                        // Drive recover again
                        recover(conn);

                        conn.commit();
                        // The Batch has executed successfully and we can continue processing
                        retryBatch = false;
                    } catch (SQLException sqlex2) {
                        // We've caught another SQLException. Assume that we've retried the connection too soon.
                        // Make sure we inspect the latest exception
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "reset the sqlex to " + sqlex2);
                        sqlex = sqlex2;
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "sleeping for " + _transientRetrySleepTime + " millisecs");
                        Thread.sleep(_transientRetrySleepTime);
                    } catch (Throwable exc) {
                        // Not a SQLException, break out of the loop and report the exception
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Failed got exception: " + exc);
                        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, " " + ste);
                        }

                        failAndReport = true;
                        _nonTransientExceptionAtOpen = exc;
                    } finally {
                        // Tidy up the connection and its artefacets, if we can but allow
                        // processing to continue on the pre-determined path if we cannot.
                        if (conn != null) {
                            if (retryBatch) {
                                // Attempt a rollback. If it fails, trace the failure but allow processing to continue
                                try {
                                    conn.rollback();
                                } catch (Throwable exc) {
                                    // Trace the exception
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Rollback Failed, when handling OpenLog SQLException, got exception: " + exc);
                                }
                            }
                            // Attempt a close. If it fails, trace the failure but allow processing to continue
                            try {
                                conn.close();
                            } catch (Throwable exc) {
                                // Trace the exception
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Close Failed, when handling OpenLog SQLException, got exception: " + exc);
                            }
                        }
                    }
                } else {
                    // This is unexpected and catastrophic, the reference to the DataSource is null
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "NULL DataSource reference");
                    failAndReport = true;
                }
            } else
                failAndReport = true;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "handleOpenLogSQLException", failAndReport);
        return failAndReport;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.takeHADBLock
    //------------------------------------------------------------------------------
    /**
     * Takes a row lock against the database table that is being used
     * for the recovery log. This fulfils the same role as the file
     * locking scheme in conventional transaction logging to a
     * filesystem. The intent is that only one server should be
     * logging to a specific table at a time. There is sometimes a
     * lag in peer recovery where an old server is closing down while
     * a new server is opening the same log for peer recovery.
     * 
     * @exception SQLException thrown if a SQLException is
     *                encountered when accessing the
     *                Database.
     * @exception InternalLogException Thrown if an
     *                unexpected error has occured.
     */
    private void takeHADBLock(Connection conn) throws SQLException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "takeHADBLock", new java.lang.Object[] { conn, this });

        Statement lockingStmt = null;
        ResultSet lockingRS = null;

        try {
            lockingStmt = conn.createStatement();
            String queryString = "SELECT SERVER_NAME" +
                                 " FROM " + _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                                 " WHERE RU_ID=" + "-1 FOR UPDATE";
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Attempt to select the HA LOCKING ROW for UPDATE using - " + queryString);
            lockingRS = lockingStmt.executeQuery(queryString);

            if (lockingRS.next()) {
                // We found the HA Lock row
                String storedServerName = lockingRS.getString(1);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Acquired lock on HA Lock row, stored server value is: " + storedServerName);
                if (_currentProcessServerName.equalsIgnoreCase(storedServerName)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "This server OWNS the HA lock row as expected");
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "ANOTHER server OWNS the lock row - we need to mark the log as failed");
                    Tr.audit(tc, "WTRN0100E: " +
                                 "Another server owns the log cannot force SQL RecoveryLog " + _logName + " for server " + _serverName);
                    InternalLogException ile = new InternalLogException("Another server has locked the HA lock row", null);
                    markFailed(ile);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "takeHADBLock", "InternalLogException");
                    throw ile;
                }
            } else {
                // We didn't find the HA Lock row in the table, mark the log as failed
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Could not find HA Lock row");
                InternalLogException ile = new InternalLogException("Could not find the HA lock row", null);
                Tr.audit(tc, "WTRN0100E: " +
                             "Could not find HA lock row when forcing SQL RecoveryLog " + _logName + " for server " + _serverName);
                markFailed(ile);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "takeHADBLock", "InternalLogException");
                throw ile;
            }
        } finally {
            if (lockingRS != null && !lockingRS.isClosed())
                lockingRS.close();
            if (lockingStmt != null && !lockingStmt.isClosed())
                lockingStmt.close();
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "takeHADBLock");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.updateHADBLock
    //------------------------------------------------------------------------------
    /**
     * Acquires ownership of the special row used in the HA locking
     * scheme. There is sometimes a lag in peer recovery where an old
     * server is closing down while a new server is opening the same
     * log for peer recovery.
     * 
     * @exception SQLException thrown if a SQLException is
     *                encountered when accessing the
     *                Database.
     * @exception InternalLogException Thrown if an
     *                unexpected error has occured.
     */
    private void updateHADBLock(Connection conn, Statement lockingStmt, ResultSet lockingRS) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "updateHADBLock", new java.lang.Object[] { conn, lockingStmt, lockingRS, this });

        if (lockingRS.next()) {
            // We found the HA Lock row
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Acquired lock on HA Lock row");
            String storedServerName = lockingRS.getString(1);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Stored server value is: " + storedServerName);
            if (_currentProcessServerName.equalsIgnoreCase(storedServerName)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "This server ALREADY OWNS the HA lock row");
            } else {
                String updateString = "UPDATE " +
                                      _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                                      " SET SERVER_NAME = '" + _currentProcessServerName +
                                      "' WHERE RU_ID = -1";
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "ANOTHER server OWNS the lock, lets update it using - " + updateString);
                int ret = lockingStmt.executeUpdate(updateString);

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Have updated HA Lock row with return: " + ret);
            }

        } else {
            // Is this entirely necessary? We didn't find the HA Lock row in the table, perhaps we should barf
            short serviceId = (short) 1;
            String insertString = "INSERT INTO " +
                                  _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                                  " (SERVER_NAME, SERVICE_ID, RU_ID, RUSECTION_ID, RUSECTION_DATA_INDEX, DATA)" +
                                  " VALUES (?,?,?,?,?,?)";

            PreparedStatement specStatement = null;

            try {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Need to setup HA Lock row using - " + insertString);
                specStatement = conn.prepareStatement(insertString);
                specStatement.setString(1, _currentProcessServerName);
                specStatement.setShort(2, serviceId);
                specStatement.setLong(3, -1); // NOTE RU_ID SET TO -1
                specStatement.setLong(4, 1);
                specStatement.setShort(5, (short) 1);
                byte buf[] = new byte[2];
                specStatement.setBytes(6, buf);
                int ret = specStatement.executeUpdate();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Have inserted HA Lock row with return: " + ret);
            } finally {
                if (specStatement != null && !specStatement.isClosed())
                    specStatement.close();
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "updateHADBLock");
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
    private void createDBTable(Connection conn) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createDBTable", new java.lang.Object[] { conn, this });

        Statement createTableStmt = null;
        PreparedStatement specStatement = null;

        try {
            createTableStmt = conn.createStatement();

            if (_isOracle) {
                String oracleFullTableName = _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix;
                String oracleTableString = oracleTablePreString + oracleFullTableName + oracleTablePostString;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Create Oracle Table using: " + oracleTableString);

                String oracleIndexString = indexPreString + _recoveryIndexName + _logIdentifierString + _recoveryTableNameSuffix +
                                           " ON " + oracleFullTableName + indexPostString;

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Create Oracle Index using: " + oracleIndexString);
                // Create the Oracle table
                createTableStmt.executeUpdate(oracleTableString);
                // Create index on the new table
                createTableStmt.executeUpdate(oracleIndexString);
            } else {
                String db2FullTableName = _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix;
                String db2TableString = db2TablePreString + db2FullTableName + db2TablePostString;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Create DB2 Table using: " + db2TableString);

                String db2IndexString = indexPreString + _recoveryIndexName + _logIdentifierString + _recoveryTableNameSuffix +
                                        " ON " + db2FullTableName + indexPostString;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Create DB2 Index using: " + db2IndexString);
                // Create the DB2 table
                createTableStmt.executeUpdate(db2TableString);
                // Create index on the new table
                createTableStmt.executeUpdate(db2IndexString);
            }

            short serviceId = (short) _recoveryAgent.clientIdentifier();
            String insertString = "INSERT INTO " +
                                  _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                                  " (SERVER_NAME, SERVICE_ID, RU_ID, RUSECTION_ID, RUSECTION_DATA_INDEX, DATA)" +
                                  " VALUES (?,?,?,?,?,?)";
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Have created the table, insert special HA LOCKING row using - " + insertString);

            specStatement = conn.prepareStatement(insertString);
            specStatement.setString(1, _currentProcessServerName);
            specStatement.setShort(2, serviceId);
            specStatement.setLong(3, -1); // NOTE RU_ID SET TO -1
            specStatement.setLong(4, 1);
            specStatement.setShort(5, (short) 1);
            byte buf[] = new byte[2];
            specStatement.setBytes(6, buf);
            int ret = specStatement.executeUpdate();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Have inserted HA LOCKING ROW with return: " + ret);

        } finally {
            if (createTableStmt != null && !createTableStmt.isClosed()) {
                createTableStmt.close();
            }
            if (specStatement != null && !specStatement.isClosed()) {
                specStatement.close();
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createDBTable");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.removing
    //------------------------------------------------------------------------------
    /**
     * This method is defined by the LogCursorCallback interface. When the client
     * service calls <code>SQLMultiScopeRecoveryLog.recoverableUnits</code>, a LogCursor is
     * created passing the current instance of this class as the LogCursorCallback
     * argument. Whenever the <code>LogCursor.remove</code> method is invoked, the
     * <code>SQLMultiScopeRecoveryLog.removing</code> method is driven automatically. This
     * gives the SQLMultiScopeRecoveryLog class an opportunity to write a corrisponding
     * deletion record to the recovery log.
     * 
     * @param target The RecoverableUnit that is being removed. Typed as an Object.
     * 
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    @Override
    public void removing(Object target) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removing", new Object[] { target, this });

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing. The log should not be able to get to this point if its not compatible (as this method is only
        // called as a result of a remove on the LogCursor provided by the recoverableUnits method and this method
        // will generate a LogIncompatibleException in that event to prevent the cursor from being provided to the
        // caller. As a result, an incompatible log at this point is actually a code error of some form so
        // treat it in the same way as failure.
        if (failed() || incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removing", this);
            throw new InternalLogException(null);
        }

        try {
            removeRecoverableUnit(((SQLRecoverableUnitImpl) target).identity());
        } catch (InternalLogException exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.removing", "1573", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "An unexpected error occured whilst removing a RecoverableUnit");
            markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removing", exc);
            throw exc;
        } catch (Exception exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.removing", "1581", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "An unexpected error occured whilst removing a RecoverableUnit");
            markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removing", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removing");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.failed
    //------------------------------------------------------------------------------
    /**
     * Accessor method to read the recovery log failure state.
     * 
     * The _failed flag is set to true if the recovery log has suffered an
     * internal error that leaves it in an undefined or unmanageable state.
     * Once this flag is set, any interface method that could modify the disk
     * state is stopped with an InternalLogException. This protects the on-disk
     * information from damage and allows a subseqent server bounce to recover
     * correctly.
     * 
     * To avoid confusion in the trace, we have no entry/exit trace in this
     * method, but do put out a debug trace if the it has been marked as failed.
     * 
     * @return true if a serious internal error has occured, otherwise false.
     */
    protected boolean failed() {
        if (tc.isDebugEnabled() && _failed)
            Tr.debug(tc, "failed: RecoveryLog has been marked as failed. [" + this + "]");
        return _failed;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.incompatible
    //------------------------------------------------------------------------------
    /**
     * Accessor method to read the recovery log compatibility state.
     * 
     * The _incompatible flag is set to true if the recovery log has previously been
     * opened and found to be at a level not compatible with this version of the RLS.
     * 
     * To avoid confusion in the trace, we have no entry/exit trace in this
     * method, but do put out a debug trace if the it has been marked as incompatible.
     * 
     * @return true if the recovery log has been marked as incompatible otherwise false.
     */
    protected boolean incompatible() {
        if (tc.isDebugEnabled() && _incompatible)
            Tr.debug(tc, "incompatible: RecoveryLog has been marked as incompatible. [" + this + "]");
        return _incompatible;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.markFailed
    //------------------------------------------------------------------------------
    /**
     * Marks the recovery log as failed.
     * 
     * Set the flag indicating that the recovery log has suffered an internal error
     * that leaves the recovey log in an undefined or unmanageable state.
     * Once this flag is set, any interface method that could modify the disk
     * state is stopped with an InternalLogException. This protects the on-disk
     * information from damage and allows a subseqent server bounce to recover
     * correctly.
     * 
     * d453958: For HA configuration, this condition should force failover as the
     * server is now unusable.
     * 
     * To avoid confusion in the trace, we have no entry/exit trace in this
     * method, but do put out a debug trace if this is the first time the call has been
     * made.
     */
    protected void markFailed(Throwable t) /* @MD19484C */
    {
        markFailed(t, true);
    }

    protected void markFailed(Throwable t, boolean report) {

        boolean newFailure = false;
        synchronized (this) {
            if (tc.isDebugEnabled() && _failed)
                Tr.debug(tc, "markFailed: RecoveryLog has been marked as failed. [" + this + "]");

            if (!_failed) {
                newFailure = true;
                _failed = true;

                if (report) {
                    // On z/OS, the Tr.audit will go to hardcopy and the Tr.info
                    // will go to sysout and sysprint.  We really want the audit
                    // to go to the glass (wto) but can't because there is no Tr.wto
                    // in the 'common' Tr class.  This is the next best thing.
                    Object[] errorObject = new Object[] {
                                                         _logIdentifier, _clientName };
                    Tr.audit(tc, "CWRLS0008_RECOVERY_LOG_FAILED",
                             errorObject);
                    Tr.info(tc, "CWRLS0009_RECOVERY_LOG_FAILED_DETAIL", t);
                }

                // If this is a local recovery process then direct the server to terminate
                if (Configuration.HAEnabled()) {
                    if (Configuration.localFailureScope().equals(_failureScope)) {
                        // d254326 - output a message as to why we are terminating the server as in
                        // this case we never drop back to log any messages as for peer recovery.
                        Tr.error(tc, "CWRLS0024_EXC_DURING_RECOVERY", t);
                        Configuration.getRecoveryLogComponent().terminateServer();
                    } else {
                        Configuration.getRecoveryLogComponent().leaveGroup(_failureScope);
                    }
                }

            }
        }
        if (newFailure && _associatedLog != null) {
            if (_failAssociatedLog) {
                if (tc.isDebugEnabled() && _failed)
                    Tr.debug(tc, "associated log will be marked as failed", _associatedLog);
                _associatedLog.markFailedByAssociation();
            } else {
                _associatedLog.provideServiceability();
            }
        }
    }

    @Override
    public synchronized void markFailedByAssociation() {
        if (!_failed) {
            _failed = true;
            if (tc.isDebugEnabled() && _failed)
                Tr.debug(tc, "markFailedByAssociation: RecoveryLog has been marked as failed by association. [" + this + "]");
            provideServiceability();
        } else if (tc.isDebugEnabled() && _failed)
            Tr.debug(tc, "markFailedByAssociation: RecoveryLog was already failed when marked as failed by association. [" + this + "]");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.markIncompatible
    //------------------------------------------------------------------------------
    /**
     * Marks the recovery log as incompatible.
     * 
     * Set the flag indicating that the recovery log represented by this object is
     * at an incompatible level.
     * 
     * To avoid confusion in the trace, we have no entry/exit trace in this
     * method, but do put out a debug trace if this is the first time the call has been
     * made.
     */
    protected synchronized void markIncompatible() {
        if (tc.isDebugEnabled() && _incompatible)
            Tr.debug(tc, "markIncompatible: RecoveryLog has been marked as incompatible. [" + this + "]");
        _incompatible = true;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.addRecoverableUnit
    //------------------------------------------------------------------------------
    /**
     * Adds a new RecoverableUnitImpl object, keyed from its identity to this
     * classes collection of such objects.
     * 
     * @param recoverableUnit The RecoverableUnit to be added
     * @param recovered Flag to indicate if this instances have been created during
     *            recovery (true) or normal running (false). If its been created
     *            during recovery we need to reserve the associated id so that
     *            it can't be allocated to an independent RecoverableUnit.
     */
    protected void addRecoverableUnit(RecoverableUnit recoverableUnit, boolean recovered) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addRecoverableUnit", new Object[] { recoverableUnit, recovered, this });

        final long identity = recoverableUnit.identity();

        _recoverableUnits.put(identity, recoverableUnit);

        if (recovered) {
            _recUnitIdTable.reserveId(identity, recoverableUnit);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addRecoverableUnit");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.removeRecoverableUnitMapEntries
    //------------------------------------------------------------------------------
    /**
     * Removes a RecoverableUnitImpl object, keyed from its identity from this
     * classes collection of such objects.
     * 
     * @param identity The identity of the RecoverableUnitImpl to be removed
     * 
     * @return RecoverableUnitImpl The RecoverableUnitImpl thats no longer associated
     *         with the SQLMultiScopeRecoveryLog.
     */
    protected SQLRecoverableUnitImpl removeRecoverableUnitMapEntries(long identity) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeRecoverableUnitMapEntries", new Object[] { identity, this });

        final SQLRecoverableUnitImpl recoverableUnit = (SQLRecoverableUnitImpl) _recoverableUnits.remove(identity);

        if (recoverableUnit != null) {
            _recUnitIdTable.removeId(identity);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeRecoverableUnitMapEntries", recoverableUnit);
        return recoverableUnit;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.getRecoverableUnit
    //------------------------------------------------------------------------------
    /**
     * Retrieves a RecoverableUnitImpl object, keyed from its identity from this
     * classes collection of such objects.
     * 
     * @param identity The identity of the RecoverableUnitImpl to be retrieved
     * 
     * @return RecoverableUnitImpl The required RecoverableUnitImpl
     */
    protected SQLRecoverableUnitImpl getRecoverableUnit(long identity) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getRecoverableUnit", new Object[] { identity, this });

        SQLRecoverableUnitImpl recoverableUnit = null;

        // Only attempt to resolve the recoverable unit if the log is compatible and valid.
        if (!incompatible() && !failed()) {
            recoverableUnit = (SQLRecoverableUnitImpl) _recoverableUnits.get(identity);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getRecoverableUnit", recoverableUnit);
        return recoverableUnit;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.serverName
    //------------------------------------------------------------------------------
    /**
     * Returns the name of the server that owns this object instance. This may not
     * necessarly be the local server as this object may represent a recovery log
     * owned by a peer server.
     * 
     * @return String The server name
     */
    String serverName() {
        return _serverName;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.clientName
    //------------------------------------------------------------------------------
    /**
     * Returns the name of the client service that owns this object instance.
     * 
     * @return String The client name.
     */
    String clientName() {
        return _clientName;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.clientVersion
    //------------------------------------------------------------------------------
    /**
     * Returns the version number of the client service that owns this object instance.
     * 
     * @return int The client version number
     */
    public int clientVersion() {
        return _clientVersion;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.logName
    //------------------------------------------------------------------------------
    /**
     * Returns the log name.
     * 
     * @return String The log name
     */
    public String logName() {
        return _logName;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.logIdentifier
    //------------------------------------------------------------------------------
    /**
     * Returns the log identifier.
     * 
     * @return int The log identifier
     */
    public int logIdentifier() {
        return _logIdentifier;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.serverStopping
    //------------------------------------------------------------------------------
    /**
     * Signals to the Recovery Log that the server is stopping.
     */
    public synchronized void serverStopping() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serverStopping ", new Object[] { this });

        _serverStopping = true;

        Statement lockingStmt = null;
        String queryString = "";
        ResultSet nonLockingRS = null;
        // Need to establish a reserved connection at this point that can be used in log close processing.
        try {
            _reservedConn = _theDS.getConnection();

            // The BPM Test team found a case where this connection could be stale - a failover had been performed but no recovery log
            // work had been done up to the point where the server stopped. So we'll just check that it isn't stale.
            lockingStmt = _reservedConn.createStatement();
            // Use RDBMS SELECT FOR UPDATE to lock table for recovery
            queryString = "SELECT SERVER_NAME" +
                          " FROM " + _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                          " WHERE RU_ID=" + "-1";
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Attempt to select the HA LOCKING ROW BUT NOT for UPDATE - " + queryString);
            nonLockingRS = lockingStmt.executeQuery(queryString);
            if (!nonLockingRS.next())
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "HA Locking row was NOT found");
                else if (tc.isDebugEnabled())
                    Tr.debug(tc, "HA Locking row was found");
            lockingStmt.close();
            _reservedConn.commit();
        } catch (SQLException sqlex) {
            Throwable theNonTransientException = sqlex;
            Tr.audit(tc, "WTRN0107W: " +
                         "Caught SQLException when server stopping for SQL RecoveryLog " + _logName + " for server " + _serverName + " SQLException: " + sqlex);
            // Should we attempt to reconnect? This method works through the set of SQL exceptions and will
            // return TRUE if we determine that an transient DB error has occurred
            boolean retryBatch = sqlTransientErrorHandlingEnabled && isSQLErrorTransient(sqlex);
            boolean failAndReport = true;
            if (retryBatch) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Try to reexecute the SQL using connection from DS: " + _theDS);

                // Re-execute the SQL
                try {
                    // Get a connection to database via its datasource
                    _reservedConn = _theDS.getConnection();
                    lockingStmt = _reservedConn.createStatement();
                    nonLockingRS = lockingStmt.executeQuery(queryString);
                    if (!nonLockingRS.next())
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "HA Locking row was NOT found");
                        else if (tc.isDebugEnabled())
                            Tr.debug(tc, "HA Locking row was found");
                    lockingStmt.close();
                    _reservedConn.commit();
                    failAndReport = false;
                } catch (SQLException sqlex2) {
                    // We've caught another SQLException. Assume that we've retried the connection too soon.
                    // Make sure we inspect the latest exception
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "reset the sqlex to " + sqlex2);
                    theNonTransientException = sqlex2;
                } catch (Throwable exc) {
                    // Not a SQLException, break out of the loop and report the exception
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Failed got exception: " + exc);

                    for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, " " + ste);
                    }

                    theNonTransientException = exc;
                }
            }

            if (failAndReport) {
                // Throw an FFDC, but allow processing to continue
                FFDCFilter.processException(theNonTransientException, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.serverStopping", "464", this);

                _reservedConn = null;

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "serverStopping", theNonTransientException);
            }
        } finally {
            try {
                if (nonLockingRS != null && !nonLockingRS.isClosed())
                    nonLockingRS.close();
                if (lockingStmt != null && !lockingStmt.isClosed())
                    lockingStmt.close();
            } catch (SQLException e) {
                // Log close failure but allow processing to continue
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "on closing SQL resources caught exception: " + e);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "serverStopping");
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.toString
    //------------------------------------------------------------------------------
    /**
     * Returns the string representation of this object instance.
     * 
     * @return String The string representation of this object instance.
     */
    @Override
    public String toString() {
        return _traceId;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.logTypeFromInteger
    //------------------------------------------------------------------------------
    /**
     * Returns the string representation of a log identifier.
     * 
     * @return String The string representation of the log type.
     */
    private String logTypeFromInteger(int x) {
        switch (x) {
            case 1:
                return "TRAN_LOG";
            case 2:
                return "PARTNER_LOG";
            case 3:
                return "COMP_LOG";
        }
        return "";
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.getTransientSQLErrorRetryAttempts
    //------------------------------------------------------------------------------
    /**
     * This method retrieves a system property named
     * com.ibm.ws.recoverylog.custom.jdbc.impl.TransientRetryAttempts
     * which allows a value to be specified for the number of times
     * that we should try to get a connection and retry SQL work in
     * the face of transient sql error conditions.
     */
    private Integer getTransientSQLErrorRetryAttempts() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTransientSQLErrorRetryAttempts");

        Integer transientSqlRetryAttempts = null;

        try {
            transientSqlRetryAttempts = AccessController.doPrivileged(
                            new PrivilegedExceptionAction<Integer>() {
                                @Override
                                public Integer run() {
                                    return Integer.getInteger("com.ibm.ws.recoverylog.custom.jdbc.impl.TransientRetryAttempts",
                                                              DEFAULT_TRANSIENT_RETRY_ATTEMPTS);
                                }
                            });
        } catch (PrivilegedActionException e) {
            FFDCFilter.processException(e, "com.ibm.ws.recoverylog.custom.jdbc.impl.SqlMultiScopeRecoveryLog.getTransientSQLErrorRetryAttempts", "132");
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception setting transient SQL retry attempts", e);
            transientSqlRetryAttempts = null;
        }

        if (transientSqlRetryAttempts == null)
            transientSqlRetryAttempts = Integer.valueOf(DEFAULT_TRANSIENT_RETRY_ATTEMPTS);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getTransientSQLErrorRetryAttempts", transientSqlRetryAttempts);
        return transientSqlRetryAttempts;
    }

    //------------------------------------------------------------------------------
    // Method: SQLMultiScopeRecoveryLog.getTransientSQLErrorRetrySleepTime
    //------------------------------------------------------------------------------
    /**
     * This method retrieves a system property named
     * com.ibm.ws.recoverylog.custom.jdbc.impl.TransientRetrySleepTime
     * which allows a value to be specified for the time we should
     * sleep between attempts to get a connection and retry SQL work
     * in the face of transient sql error conditions.
     */
    private Integer getTransientSQLErrorRetrySleepTime() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTransientSQLErrorRetrySleepTime");

        Integer transientSqlRetrySleepTime = null;

        try {
            transientSqlRetrySleepTime = AccessController.doPrivileged(
                            new PrivilegedExceptionAction<Integer>() {
                                @Override
                                public Integer run() {
                                    return Integer.getInteger("com.ibm.ws.recoverylog.custom.jdbc.impl.TransientRetrySleepTime",
                                                              DEFAULT_TRANSIENT_RETRY_SLEEP_TIME);
                                }
                            });
        } catch (PrivilegedActionException e) {
            FFDCFilter.processException(e, "com.ibm.ws.recoverylog.custom.jdbc.impl.SqlMultiScopeRecoveryLog.getTransientSQLErrorRetrySleepTime", "132");
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception setting transient SQL retry sleep time", e);
            transientSqlRetrySleepTime = null;
        }

        if (transientSqlRetrySleepTime == null)
            transientSqlRetrySleepTime = Integer.valueOf(DEFAULT_TRANSIENT_RETRY_SLEEP_TIME);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getTransientSQLErrorRetrySleepTime", transientSqlRetrySleepTime);
        return transientSqlRetrySleepTime;
    }

    //------------------------------------------------------------------------------
    // Class: ruForReplay
    //------------------------------------------------------------------------------
    /**
     * This class is used to represent the cached up work that will
     * be committed to the database when the log is forced.
     */
    private static class ruForReplay {
        private final long _ruId;
        private final long _sectionId;
        private final int _index;
        private byte[] _data = null;

        // Constructor
        public ruForReplay(long ruId, long sectionId, int index, byte[] data) {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "ruForReplay", new Object[] { ruId, sectionId, index, data });
            _ruId = ruId;
            _sectionId = sectionId;
            _index = index;
            _data = data;

            if (tc.isEntryEnabled())
                Tr.exit(tc, "ruForReplay", this);
        }

        public long getRuId() {
            return _ruId;
        }

        public long getSectionId() {
            return _sectionId;
        }

        public int getIndex() {
            return _index;
        }

        private byte[] getData() {
            return _data;
        }
    }

    //------------------------------------------------------------------------------
    // Method: DistributedRecoveryLog.associateLog
    //------------------------------------------------------------------------------
    /**
     * Associates another log with this one. PI45254.
     * The code is protects against infinite recursion since associated logs are only marked as failed if
     * the log isn't already mark as failed.
     * The code does NOT protect against deadlock due to synchronization for logA->logB and logB->logA
     * - this is not an issue since failAssociated is only set to true for tranLog and not partnerLog
     * - this could be fixed for general use by delegating to an 'AssociatedLogGroup' object shared between associated logs.
     */
    @Override
    public void associateLog(DistributedRecoveryLog otherLog, boolean failAssociatedLog) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "associateLog", new Object[] { otherLog, failAssociatedLog, this });

        if (otherLog instanceof MultiScopeLog) {
            _associatedLog = (MultiScopeLog) otherLog;
            _failAssociatedLog = failAssociatedLog;
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "associateLog");
    }

    /**
     * Called when logs fail. Provides more comprehensive FFDC - PI45254.
     * this is NOT synchronized to avoid deadlocks.
     */
    @Override
    public void provideServiceability() {
        Exception e = new Exception();
        try {
            FFDCFilter.processException(e, "com.ibm.ws.recoverylog.custom.jdbc.impl.SQLMultiScopeRecoveryLog.provideServiceability", "3624", this);
            HashMap<Long, RecoverableUnit> rus = _recoverableUnits;
            if (rus != null)
                FFDCFilter.processException(e, "com.ibm.ws.recoverylog.custom.jdbc.impl.SQLMultiScopeRecoveryLog.provideServiceability", "3628", rus);
        } catch (Exception ex) {
            // Do nothing
        }
    }

    // Sets AutoCommit and Isolation level for the connection 
    private int prepareConnectionForBatch(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
        int initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;
        if (_isDB2)
        {
            try
            {
                initialIsolation = conn.getTransactionIsolation();
                if (Connection.TRANSACTION_REPEATABLE_READ != initialIsolation && Connection.TRANSACTION_SERIALIZABLE != initialIsolation)
                {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Transaction isolation level was " + initialIsolation + " , setting to TRANSACTION_REPEATABLE_READ");
                    conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                }
            } catch (Exception e)
            {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "setTransactionIsolation to RR threw Exception. Transaction isolation level was " + initialIsolation + " ", e);
                FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.prepareConnectionForBatch", "3668", this);
                if (!isolationFailureReported)
                {
                    isolationFailureReported = true;
                    Tr.warning(tc, "CWRLS0024_EXC_DURING_RECOVERY", e);
                }
                // returning RR will prevent closeConnectionAfterBatch resetting isolation level
                initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;
            }
        }
        return initialIsolation;
    }

    // closes the connection and resets the isolation level if required
    private void closeConnectionAfterBatch(Connection conn, int initialIsolation) throws SQLException {
        if (_isDB2)
        {
            if (Connection.TRANSACTION_REPEATABLE_READ != initialIsolation && Connection.TRANSACTION_SERIALIZABLE != initialIsolation)
                try
                {
                    conn.setTransactionIsolation(initialIsolation);
                } catch (Exception e)
                {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "setTransactionIsolation threw Exception. Specified transaction isolation level was " + initialIsolation + " ", e);
                    FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.closeConnectionAfterBatch", "3696", this);
                    if (!isolationFailureReported)
                    {
                        isolationFailureReported = true;
                        Tr.warning(tc, "CWRLS0024_EXC_DURING_RECOVERY", e);
                    }
                }
        }
        conn.close();
    }

}
