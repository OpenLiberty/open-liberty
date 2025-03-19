/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
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

package com.ibm.ws.recoverylog.custom.jdbc.impl;

import java.io.StringReader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.resource.ResourceException;
import javax.resource.spi.ResourceAllocationException;
import javax.sql.DataSource;

import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.util.Utils;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.recoverylog.custom.jdbc.impl.DBUtils.DBProduct;
import com.ibm.ws.recoverylog.spi.Configuration;
import com.ibm.ws.recoverylog.spi.CustomLogProperties;
import com.ibm.ws.recoverylog.spi.DistributedRecoveryLog;
import com.ibm.ws.recoverylog.spi.FailureScope;
import com.ibm.ws.recoverylog.spi.HeartbeatLog;
import com.ibm.ws.recoverylog.spi.HeartbeatLogManager;
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
import com.ibm.ws.recoverylog.spi.LogsUnderlyingTablesMissingException;
import com.ibm.ws.recoverylog.spi.MultiScopeLog;
import com.ibm.ws.recoverylog.spi.PeerLostLogOwnershipException;
import com.ibm.ws.recoverylog.spi.RLSUtils;
import com.ibm.ws.recoverylog.spi.RecoverableUnit;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSectionExistsException;
import com.ibm.ws.recoverylog.spi.RecoveryAgent;
import com.ibm.ws.recoverylog.spi.TraceConstants;
import com.ibm.ws.recoverylog.utils.RecoverableUnitIdTable;

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
public class SQLMultiScopeRecoveryLog implements LogCursorCallback, MultiScopeLog, HeartbeatLog, SQLRetriableLog {

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
     * Which RDBMS are we working against?
     */
    private volatile DBProduct dbProduct;

    private boolean isolationFailureReported;

    /**
     * A map of recoverable units. Each recoverable unit is keyed by its identity.
     */
    private HashMap<Long, SQLRecoverableUnitImpl> _recoverableUnits;

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
     * Name of the DataSource that hosts the db logs.
     */
    private String _dsName;
    /**
     * Flag indicating that the recovery log has suffered an internal error
     * that leaves the recovey log in an undefined or unmanageable state.
     * Once this flag is set, any interface method that could modify the disk
     * state is stopped with an InternalLogException. This protects the on-disk
     * information from damage and allows a subseqent server bounce to recover
     * correctly.
     */
    private boolean _failed;

    private boolean _deleted;

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

    private static final String _recoveryTableName = "WAS_";
    private static final String _recoveryIndexName = "IXWS";
    private String _recoveryTableNameSuffix = "";

    final FailureScope _failureScope;

    /**
     * A flag to indicate whether the recovery log belongs to the home server.
     */
    private final boolean _isHomeServer;

    /**
     * Flag to indicate that underlying SQL Tables have been deleted by a peer
     */
    private boolean _isTableDeleted = false;
    /**
     * A flag to indicate that the log was being recovered by a peer
     * but it has been reclaimed by its "home" server.
     */
    private volatile boolean _peerServerLostLogOwnership;

    /**
     * These strings are used for Database table creation. DDL is
     * different for DB2 and Oracle.
     */
    private static final String genericTableCreatePreString = "CREATE TABLE ";
    private static final String genericTableDropPreString = "DROP TABLE ";
    private static final String genericTablePostString = "( SERVER_NAME VARCHAR(128), " +
                                                         "SERVICE_ID SMALLINT, " +
                                                         "RU_ID BIGINT, " +
                                                         "RUSECTION_ID BIGINT, " +
                                                         "RUSECTION_DATA_INDEX SMALLINT, " +
                                                         "DATA LONG VARCHAR FOR BIT DATA) ";

    private static final String db2TablePostString = "( SERVER_NAME VARCHAR(128), " +
                                                     "SERVICE_ID SMALLINT, " +
                                                     "RU_ID BIGINT, " +
                                                     "RUSECTION_ID BIGINT, " +
                                                     "RUSECTION_DATA_INDEX SMALLINT, " +
                                                     "DATA BLOB) ";

    private static final String oracleTablePostString = "( SERVER_NAME VARCHAR(128), " +
                                                        "SERVICE_ID SMALLINT, " +
                                                        "RU_ID NUMBER(19), " +
                                                        "RUSECTION_ID NUMBER(19), " +
                                                        "RUSECTION_DATA_INDEX SMALLINT, " +
                                                        "DATA BLOB) ";

    private static final String postgreSQLTablePostString = "( SERVER_NAME VARCHAR(128), " +
                                                            "SERVICE_ID SMALLINT, " +
                                                            "RU_ID BIGINT, " +
                                                            "RUSECTION_ID BIGINT, " +
                                                            "RUSECTION_DATA_INDEX SMALLINT, " +
                                                            "DATA BYTEA) ";

    private static final String sqlServerTablePostString = "( SERVER_NAME VARCHAR(128), " +
                                                           "SERVICE_ID SMALLINT, " +
                                                           "RU_ID BIGINT, " +
                                                           "RUSECTION_ID BIGINT, " +
                                                           "RUSECTION_DATA_INDEX SMALLINT, " +
                                                           "DATA VARBINARY(MAX)) ";

    // It is possible to use the same INDEX creation DDL for Oracle and DB2 and Generic
    private static final String indexPreString = "CREATE INDEX ";
    private static final String indexPostString = "( \"RU_ID\" ASC, " +
                                                  "\"SERVICE_ID\" ASC, " +
                                                  "\"SERVER_NAME\" ASC) ";

    private static final String postgreSQLIndexPostString = "( RU_ID ASC, " +
                                                            "SERVICE_ID ASC, " +
                                                            "SERVER_NAME ASC) ";

    // Reference to the dedicated non-transactional datasource
    private DataSource _theDS;

    // Reserved connection for use specifically in shutdown processing
    private Connection _reservedConn;

    /**
     * We only want one client at a time to attempt to create a new
     * Database table.
     */
    private final Object _CreateTableLock = new Object();

    // This is "lock A".  We can lock A then lock B, or just lock A or just lock B but we can't lock B then lock A.  Lock B is 'this'.
    private final Object _DBAccessIntentLock = new Object();
    // Could use separate locks eg private static final Object _CacheLock = new Object();
    // Could replace synchronized this with the above object lock (in case a caller synchs on our reference)
    // We only really care about failed() being accurate when using the database.

    /*
     * PH22988 introduces "throttle" code to improve concurrent access to the cache of inserts, updates and removes at runtime.
     * Greater efficiency can be achieved where multiple cached operations are performed in a batch.
     *
     * _throttleWaiters counts the number of threads attempting to forceSections(). If that number passes a threshold,
     * then throttling is enabled. The default threshold is currently 6. The use of a threshold means the throttle code
     * is only used when contention increases. The throttle is disabled when contention decreases. Methods that access
     * the cache of inserts, updates and removes, ie internalWriteRUSection(), removeRecoverableUnit() and internalUpdateRUSection()
     * check to see if the throttle is enabled. If the throttle is enabled they will wait.
     *
     * When an invocation of forceSections() has completed its work it decrements _throttleWaiters. If the value of _throttleWaiters
     * has reached 0, then all waiting threads are notified to allow new work into the cache and throttling is disabled.
     */
    private final ReentrantLock _throttleLock = new ReentrantLock(true);
    private final Condition _throttleCleared = _throttleLock.newCondition();
    private int _throttleWaiters;
    private final static int _waiterThreshold;

    // Switch on SQL comments for testing
    private static Boolean _tagSQL;

    static {
        int waiterThreshold = 6;
        try {
            waiterThreshold = AccessController.doPrivileged(
                                                            new PrivilegedExceptionAction<Integer>() {
                                                                @Override
                                                                public Integer run() {
                                                                    return Integer.getInteger("com.ibm.ws.recoverylog.custom.jdbc.ThrottleThreshold", 6);
                                                                }
                                                            });
        } catch (PrivilegedActionException e) {
            FFDCFilter.processException(e, "com.ibm.ws.recoverylog.custom.jdbc.impl.SqlMultiScopeRecoveryLog", "345");
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception setting throttle waiter threshold", e);
            waiterThreshold = 6;
        }
        _waiterThreshold = waiterThreshold;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Throttle waiter threshold set to ", _waiterThreshold);

        try {
            _tagSQL = AccessController.doPrivileged(
                                                    new PrivilegedExceptionAction<Boolean>() {
                                                        @Override
                                                        public Boolean run() {
                                                            return Boolean.getBoolean("com.ibm.ws.recoverylog.custom.jdbc.tagSQL");
                                                        }
                                                    });
        } catch (PrivilegedActionException e) {
        }
    }

    private boolean _throttleEnabled;

    /**
     * Maintain a lists of statements that can be replayed if an HA
     * Database fails over.
     */
    private final List<ruForReplay> _cachedInsertsA = new ArrayList<ruForReplay>();
    private final List<ruForReplay> _cachedUpdatesA = new ArrayList<ruForReplay>();
    private final List<ruForReplay> _cachedRemovesA = new ArrayList<ruForReplay>();

    private final List<ruForReplay> _cachedInsertsB = new ArrayList<ruForReplay>();
    private final List<ruForReplay> _cachedUpdatesB = new ArrayList<ruForReplay>();
    private final List<ruForReplay> _cachedRemovesB = new ArrayList<ruForReplay>();

    // If we're writing new data TO A then if we're writing data it's being written FROM B.
    // _writeFromCacheA is only changed in internalForceSections.
    // it is ONLY flipped when BOTH _DBAccessIntentLock is held (for single threaded behaviour) AND this's intrinsic lock is held (for synching cache state).
    private boolean _writeToCacheA = true;

    /**
     * In Liberty, we now rely on the RDBMS throwing SQLTransientExceptions, to signify a transient condition. Retain the codes as documentation.
     * Key Database Transient error and Failover codes that alert us to a transient absence of a database connection. These are
     * pulled from the RDBMS specific Helper classes in SERV1\ws\code\adapter\src\com\ibm\websphere\rsadapter
     *
     * private static final int _db2TransientErrorCodes[] = { -1015, -1034, -1035, -6036, -30081, -30108, -1224, -1229, -518, -514, -30080, -924, -923, -906, -4498, -4499, -1776 };
     * private static final int _oracleTransientErrorCodes[] = { 20, 28, 1012, 1014, 1033, 1034, 1035, 1089, 1090, 1092, 3113, 3114, 12505, 12514, 12541, 12560,
     * 12571, 17002, 17008, 17009, 17410, 17401, 17430, 25408, 24794, 17447, 30006 }; // N.B. POSITIVE - is that correct?
     * private int _sqlTransientErrorCodes[];
     */
    private boolean _sqlTransientErrorHandlingEnabled = true;

    /**
     * Flag to indicate whether the server is stopping.
     */
    private boolean _serverStopping;
    private boolean _associateServerStopping;

    volatile SQLMultiScopeRecoveryLog _associatedLog;
    volatile boolean _failAssociatedLog;

    private final String _currentProcessServerName;

    // This flag is set to "true" when the new HADB locking scheme has been enabled for peer recovery
    private static volatile boolean _useNewLockingScheme;

    private final Duration _logGoneStaleTime = Duration.ofSeconds(ConfigurationProviderManager.getConfigurationProvider().getPeerTimeBeforeStale()); // If a timestamp has not been updated in _logGoneStaleTime seconds, we assume that
    // the owning server has crashed. The default is 10 seconds.
    private final Duration _peerLockTimeBetweenHeartbeats = Duration.ofSeconds(ConfigurationProviderManager.getConfigurationProvider().getTimeBetweenHeartbeats());

    private static final long _reservedConnectionActiveSectionIDSet = 255L;
    private static final long _reservedConnectionActiveSectionIDUnset = 1L;

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
     * @param recoveryAgent     The RecoveryAgent of the associated client service.
     * @param fs                The FailureScope of the associated client service.
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
        _isHomeServer = Configuration.localFailureScope().equals(_failureScope);
        _internalLogProperties = _customLogProperties.properties();
        _currentProcessServerName = Configuration.fqServerName();

        /**
         * Set the parameters that define SQL Error retry behaviour
         *
         * In tWAS this is achieved through system properties, in Liberty through internal
         * server.xml attributes.
         *
         * tWAS
         * ====
         *
         * _transientRetryAttempts = getTransientSQLErrorRetryAttempts().intValue();
         * _transientRetrySleepTime = getTransientSQLErrorRetrySleepTime().intValue();
         */

        // Now output consolidated trace information regarding the configuration of this object.
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Recovery log belongs to server " + _serverName);
            Tr.debug(tc, "Recovery log belongs to home server " + _isHomeServer);
            Tr.debug(tc, "Recovery log created by client service " + _clientName + " at version " + _clientVersion);
            Tr.debug(tc, "Recovery log name is " + _logName);
            Tr.debug(tc, "Recovery log identifier is " + _logIdentifier);
            Tr.debug(tc, "Recovery log internal properties are " + _internalLogProperties);
            Tr.debug(tc, "FIS 114950");
        }

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
        _bypassContainmentCheck = !Configuration.HAEnabled();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "_bypassContainmentCheck = " + _bypassContainmentCheck);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "SQLMultiScopeRecoveryLog", this);
    }

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
     * @exception LogCorruptedException  The recovery log has become corrupted and
     *                                       cannot be opened.
     * @exception LogAllocationException The recovery log could not be created.
     * @exception InternalLogException   An unexpected failure has occured.
     */

    @Override
    @FFDCIgnore({ PeerLostLogOwnershipException.class })
    public void openLog(boolean localRecovery) throws LogCorruptedException, LogAllocationException, InternalLogException {

        synchronized (_DBAccessIntentLock) {
            synchronized (this) {
                if (tc.isEntryEnabled())
                    Tr.entry(tc, "openLog", localRecovery, this);

                // If this recovery log instance has experienced a serious internal error then prevent this operation from
                // executing.
                if (failed()) {
                    // The exception to the rule is where the ownership of the log had previously been aggressively reclaimed
                    // by its home server. In this case we'll reset the failure related flags.
                    if (_peerServerLostLogOwnership) {
                        // Re-set the flag to indicate that the log was being recovered by a peer.
                        _peerServerLostLogOwnership = false;
                        _failed = false;
                    } else {
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "openLog", "InternalLogException");
                        throw new InternalLogException("Peer server did not lose log ownership");
                    }
                }

                // We need to throw an exception if an attempt is made to open the log when the server is stopping
                if (_serverStopping) {
                    InternalLogException ile = reportOperationAttemptWhenStopping("open");

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "openLog", "InternalLogException");
                    throw ile;
                }

                // The Database Connection
                Connection conn = null;

                // If this is the first time the recovery log has been opened during the current server run, then
                // obtain a connection to the underlying DB
                if (_closesRequired == 0) {
                    // the logs get cached in the RecoveryLogManager so a peer may pick up an old instance of this object and reuse it on a multiple failover case
                    // I'd prefer to stop that for non-zOS to be honest (in which case we could junk this resetting below) though getRecoveryLog is used all over
                    // so there'd need to be a new method for the initial get(meaning create it and clean up whatever may still be cached) - or junk it somewhere
                    // in the remote FS shutdown code.
                    // While a non-failed close SHOULD always clear these down the reasoning is convoluted (it should have called internalForceSections successully
                    // but it's not clear if that resets everything) so instead let's make sure everything is clean.
                    // NOTE if the logs is marked failed or incompatible from a previous failover we (rather stupidly) will always fail on subsequent failovers
                    // (the close won't reset markFailed in that case).

                    if (_recoverableUnits != null)
                        _recoverableUnits.clear();
                    _recoverableUnits = new HashMap<Long, SQLRecoverableUnitImpl>();

                    _cachedInsertsA.clear();
                    _cachedInsertsB.clear();
                    _cachedUpdatesA.clear();
                    _cachedUpdatesB.clear();
                    _cachedRemovesA.clear();
                    _cachedRemovesB.clear();

                    _reservedConn = null;
                    _recUnitIdTable = new RecoverableUnitIdTable();
                    // Initialise the throttle variables introduced by PH22988
                    _throttleWaiters = 0;
                    _throttleEnabled = false;

                    // For exception handling
                    Throwable nonTransientException = null;

                    try {
                        int initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;
                        boolean openSuccess = false;
                        SQLException currentSqlEx = null;

                        try {
                            // Get a Connection
                            if (_dsName == null) {
                                configureConnectionParameters();
                                conn = getFirstConnection();
                            } else {
                                conn = getConnection();
                            }

                            if (conn == null) {
                                if (tc.isEntryEnabled())
                                    Tr.exit(tc, "openLog", "Null connection InternalLogException");
                                throw new InternalLogException("Failed to get JDBC Connection", null);
                            }

                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Set autocommit FALSE and RR isolation on the connection"); // don't really need RR since updateHADB lock ALWAYS UPDATEs or INSERTs
                            initialIsolation = prepareConnectionForBatch(conn);

                            // Touch the table and create table if necessary
                            assertDBTableExists(conn, _logIdentifierString);

                            // if we've got here without an exception we've been able to touch the table or create it with the locking record in
                            // we've got no locks nor active transaction but conn is non-null and not closed
                            //
                            // Now get the HA Lock row again, update it if necessary and hold it over the recover

                            if (_useNewLockingScheme) {
                                // Under the new locking scheme the locking row is changed in a "claim" not here (open log processing)
                                assertLogOwnershipAtOpenPeerLocking(conn);
                            } else {
                                // Update the ownership of the recovery log to the running server
                                assertLogOwnershipAtOpenWithLatching(conn);
                            }

                            // We are ready to drive recovery
                            recover(conn);

                            conn.commit();

                            openSuccess = true;
                        } catch (SQLException sqlex) {
                            // Set the exception that will be reported
                            currentSqlEx = sqlex;
                            if (_useNewLockingScheme && !_isHomeServer && !ConfigurationProviderManager.getConfigurationProvider().peerRecoveryPrecedence()) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Not the home server and the underlying table may have been deleted");
                                if (isTableDeleted(sqlex)) {
                                    PeerLostLogOwnershipException ple = new PeerLostLogOwnershipException("Underlying table is missing", null);
                                    nonTransientException = ple;
                                    currentSqlEx = null;
                                }
                            }
                            if (currentSqlEx != null) {
                                Tr.audit(tc, "WTRN0107W: " +
                                             "Caught SQLException when opening SQL RecoveryLog " + _logName + " for server " + _serverName + " SQLException: " + currentSqlEx);
                            }
                        } catch (PeerLostLogOwnershipException ple) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Caught PeerLostLogOwnershipException: " + ple);
                            nonTransientException = ple;
                        } finally {
                            if (openSuccess) {
                                // Attempt a close. If it fails, trace the failure but allow processing to continue
                                try {
                                    closeConnectionAfterBatch(conn, initialIsolation);
                                } catch (Throwable exc) {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Close Failed after initial success", exc);
                                    openSuccess = false;
                                }
                            }

                            if (!openSuccess) {
                                // Tidy up connection before dropping into handleOpenLogSQLException method
                                // Attempt a rollback. If it fails, trace the failure but allow processing to continue
                                try {
                                    if (conn != null)
                                        conn.rollback();
                                } catch (Throwable exc) {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Rolling back on NOT openSuccess", exc);
                                }
                                // Attempt a close. If it fails, trace the failure but allow processing to continue
                                try {
                                    if (conn != null)
                                        closeConnectionAfterBatch(conn, initialIsolation);
                                } catch (Throwable exc) {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Close Failed on NOT sopenSuccess", exc);
                                }

                                // if the server is stopping, return without driving any retry logic
                                if (_serverStopping) {
                                    InternalLogException ile = reportOperationAttemptWhenStopping("open");

                                    if (tc.isEntryEnabled())
                                        Tr.exit(tc, "openLog", "InternalLogException");
                                    throw ile;
                                }

                                boolean failAndReport = true;
                                // If currentSqlEx is non-null, then we potentially have a condition that may be retried. If it is not null, then
                                // the nonTransientException will have been set.
                                if (currentSqlEx != null) {
                                    // Set the exception that will be reported
                                    nonTransientException = currentSqlEx;

                                    final OpenLogRetry openLogRetry = new OpenLogRetry();
                                    openLogRetry.setNonTransientException(currentSqlEx);
                                    // The following method will reset "nonTransientException" if it cannot recover
                                    if (_sqlTransientErrorHandlingEnabled) {
                                        failAndReport = openLogRetry.retryAfterSQLException(this, currentSqlEx);

                                        if (failAndReport)
                                            nonTransientException = openLogRetry.getNonTransientException();
                                    }
                                }

                                // We've been through the while loop
                                if (failAndReport) {
                                    // In the case where peer log ownership has been lost, either on first execution or retry, then re-throw the exception
                                    // without auditing.
                                    if (nonTransientException instanceof PeerLostLogOwnershipException) {
                                        PeerLostLogOwnershipException ple = new PeerLostLogOwnershipException(nonTransientException);
                                        throw ple;
                                    } else {
                                        Tr.audit(tc, "WTRN0100E: " +
                                                     "Cannot recover from SQLException when opening SQL RecoveryLog " + _logName + " for server " + _serverName + " Exception: "
                                                     + nonTransientException);
                                        markFailed(nonTransientException);
                                        if (tc.isEntryEnabled())
                                            Tr.exit(tc, "openLog", "InternalLogException");
                                        throw new InternalLogException(nonTransientException);
                                    }
                                } else {
                                    Tr.audit(tc, "WTRN0108I: Have recovered from SQLException when opening SQL RecoveryLog " + _logName + " for server " + _serverName);
                                }

                            }
                        } // end finally
                    } catch (PeerLostLogOwnershipException ple) {
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "openLog", ple);
                        throw ple;
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
                } // end if _closesRequired==0

                // In light of this open operation, the caller will need to issue an additional close operation to fully close
                // the recovey log.
                _closesRequired++;

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Closes required: " + _closesRequired);
            } // end this synch
        } // end _DBAccessIntentLock synch
        if (tc.isEntryEnabled())
            Tr.exit(tc, "openLog");
    }

    private void configureConnectionParameters() throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "configureConnectionParameters");

        String fullLogDirectory = _internalLogProperties.getProperty("LOG_DIRECTORY");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "fullLogDirectory = " + fullLogDirectory);

        // If the enableLogRetries attribute has been set to "true" in the transaction stanza of the server.xml, then we will retry
        // SQL operations when general SQLExceptions are encountered
        if (ConfigurationProviderManager.getConfigurationProvider().enableLogRetries()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Set the logRetriesEnabled flag to true");
            SQLRetry.setLogRetriesEnabled(true);
        }
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
        _dsName = dbStringProps.getProperty("datasource");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Extracted Data Source name = " + _dsName);
        if (_dsName != null && !_dsName.trim().isEmpty()) {
            _dsName = _dsName.trim();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Trimmed Data Source name to = " + _dsName);
        }

        // Extract the optional tablesuffix
        String tableSuffix = dbStringProps.getProperty("tablesuffix");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Extracted Table Suffix = " + tableSuffix);

        if (tableSuffix != null && !tableSuffix.equals("")) {
            _recoveryTableNameSuffix = tableSuffix;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Full RecoveryTableName = " + _recoveryTableName + _recoveryTableNameSuffix);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "configureConnectionParameters");
    }

    /**
     * Locates a DataSource in config and establish a managed connection. Retrieve connection
     * attributes from metadata.
     *
     * @return The Connection.
     * @throws InternalLogException
     * @throws SQLException
     *
     * @exception
     */
    private Connection getFirstConnection() throws InternalLogException, SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getFirstConnection");
        Connection conn = null;

        // We'll look up the DataSource definition in jndi but jndi is initialising in another
        // thread so we need to handle the situation where a first lookup does not succeed. This
        // processing is wrapped up in the SQLNonTransactionalDataSource
        SQLNonTransactionalDataSource sqlNonTranDS = new SQLNonTransactionalDataSource(_dsName, _customLogProperties);

        _theDS = sqlNonTranDS.getDataSource();

        // We've looked up the DS, so now we can get a JDBC connection
        if (_theDS != null) {
            // Get connection to database via first datasource
            conn = _theDS.getConnection();

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Got connection: " + conn);

            dbProduct = DBUtils.identifyDB(conn);

            if (DBProduct.Unknown == dbProduct && !SQLRetry.isLogRetriesEnabled()) {
                // We're not working with the standard set of databases. The "default" behaviour is not to retry for such non-standard, untested databases,
                // even if the exception is a SQLTransientException. But if the logRetriesEnabled flag has been explicitly set, then we will retry SQL
                // operations on all databases.
                _sqlTransientErrorHandlingEnabled = false;
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getFirstConnection", conn);
        return conn;
    }

    /**
     * In general a DataSource object will have been retrieved and stored for the lifetime of the server. If for some reason the
     * DataSource definition has been changed, then it will be necessary to look up the DataSource definition again, in order to
     * get a connection.
     *
     * @return Connection
     * @throws SQLException
     */
    @Override
    public Connection getConnection() throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getConnection");
        Connection conn = null;
        boolean lookupDS = false;

        // If the enableLogRetries attribute has been set to "true" in the transaction stanza of the server.xml, then we will retry
        // SQL operations when general SQLExceptions are encountered
        if (ConfigurationProviderManager.getConfigurationProvider().enableLogRetries()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Set the logRetriesEnabled flag to true");
            SQLRetry.setLogRetriesEnabled(true);
            // If the logRetriesEnabled flag has been explicitly set, then we will retry SQL operations on all databases.
            _sqlTransientErrorHandlingEnabled = true;
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Set the logRetriesEnabled flag to false");
            // If _logRetriesEnabled has been reset (config change) and if the database is non-standard, then we will
            // no longer retry SQLExceptions
            if (SQLRetry.isLogRetriesEnabled() && (dbProduct == null || DBProduct.Unknown == dbProduct))
                _sqlTransientErrorHandlingEnabled = false;

            SQLRetry.setLogRetriesEnabled(false);
        }

        // Get connection to database via datasource
        try {
            if (_theDS != null) {
                conn = _theDS.getConnection();
            } else
                lookupDS = true;
        } catch (SQLException sqlex) {
            // Handle the special case where the DataSource has been refreshed and the Connection Pool has shut down
            Throwable cause = sqlex.getCause();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Caught SQLException with cause: " + cause);

            // Check for exceptions that signify that the prerequisite DataSource
            // component may have been refreshed, so that we attempt a new DS lookup.
            if (cause instanceof ResourceAllocationException
                || cause instanceof ResourceException) {
                // Look up the DataSource definition again
                lookupDS = true;
            } else {
                // Not a ResourceAllocationException, rethrow exception
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getConnection", "SQLException");
                throw sqlex;
            }
        }

        // Look up the DataSource definition again in order to get a Connection
        if (lookupDS) {
            SQLNonTransactionalDataSource sqlNonTranDS = new SQLNonTransactionalDataSource(_dsName, _customLogProperties);

            try {
                _theDS = sqlNonTranDS.getDataSource();
                conn = _theDS.getConnection();
                Tr.audit(tc, "WTRN0108I: " +
                             "Have recovered from ResourceAllocationException in SQL RecoveryLog " + _logName + " for server " + _serverName);
            } catch (Throwable exc) {
                SQLException newsqlex;
                if (exc instanceof SQLException) {
                    newsqlex = (SQLException) exc;

                } else {
                    // Wrap in a SQLException
                    newsqlex = new SQLException(exc);
                }
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getConnection", "new SQLException");
                throw newsqlex;
            }
        }

        if (dbProduct == null) {
            dbProduct = DBUtils.identifyDB(conn);

            if (DBProduct.Unknown == dbProduct && !SQLRetry.isLogRetriesEnabled()) {
                // We're not working with the standard set of databases. The "default" behaviour is not to retry for such non-standard, untested databases,
                // even if the exception is a SQLTransientException. But if the logRetriesEnabled flag has been explicitly set, then we will retry SQL
                // operations on all databases.
                _sqlTransientErrorHandlingEnabled = false;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getConnection", conn);
        return conn;
    }

    /**
     * Retrieves log records from the database ready for recovery
     * processing.
     *
     * @exception SQLException         thrown if a SQLException is
     *                                     encountered when accessing the
     *                                     Database.
     * @exception InternalLogException Thrown if an
     *                                     unexpected error has occured.
     */
    private void recover(Connection conn) throws SQLException, RecoverableUnitSectionExistsException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recover", conn);

        String queryString = "SELECT RU_ID, RUSECTION_ID, RUSECTION_DATA_INDEX, DATA" +
                             " FROM " + _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                             " WHERE SERVER_NAME='" + _serverName +
                             "' AND SERVICE_ID=" + _recoveryAgent.clientIdentifier();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Retrieve all rows from table using - " + queryString);

        try (Statement recoveryStmt = conn.createStatement(); ResultSet recoveryRS = recoveryStmt.executeQuery(queryString)) {
            while (recoveryRS.next()) {
                final long ruId = recoveryRS.getLong(1);
                if (ruId != -1) {
                    SQLRecoverableUnitImpl ru = _recoverableUnits.get(ruId);
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
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recover");
    }

    /**
     * Returns a copy of the service data or null if there is none defined. Changes to
     * the copy will have no affect on the service data stored by the RLS.
     *
     * @return The service data.
     *
     * @exception LogClosedException   Thrown if the recovery log is closed and must
     *                                     be opened before this call can be issued.
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
     * @exception LogClosedException       Thrown if the recovery log is closed and must
     *                                         be opened before this call can be issued.
     * @exception InternalLogException     Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                                         log that is not compatible with this version
     *                                         of the service.
     *
     */
    @Override
    public synchronized void recoveryComplete() throws LogClosedException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoveryComplete", this);

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", "InternalLogException");
            throw getFailureException();
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
     * @exception LogClosedException       Thrown if the recovery log is closed and must
     *                                         be opened before this call can be issued.
     * @exception InternalLogException     Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                                         log that is not compatible with this version
     *                                         of the service.
     */
    @Override
    public synchronized void recoveryComplete(byte[] serviceData) throws LogClosedException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoveryComplete", new java.lang.Object[] { RLSUtils.toHexString(serviceData, RLSUtils.MAX_DISPLAY_BYTES), this });

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", "InternalLogException");
            throw getFailureException();
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
     * and consequentially the number of times closeLog must be invoked to
     * 'fully' close the recovery log.
     * </p>
     *
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    @Override
    public void closeLog() throws InternalLogException {
        boolean successfulSQL = false;
        boolean successfulConnection = true;

        synchronized (_DBAccessIntentLock) {
            synchronized (this) {
                if (tc.isEntryEnabled())
                    Tr.entry(tc, "closeLog", new Object[] { _reservedConn, Integer.valueOf(_closesRequired), this });

                try {
                    if (_closesRequired > 0) {
                        // checks for failed, incompatible etc are delegated down to the internalKeypoint method
                        boolean initialReservedConnection = false;
                        try {
                            // The reserved connection must not be null if the server is stopping.
                            if (_reservedConn == null) {
                                if (_serverStopping) {
                                    if (_isTableDeleted) {
                                        if (tc.isDebugEnabled())
                                            Tr.debug(tc, "Reserved Connection is NULL but the underlying tables have been deleted");
                                    } else {
                                        Tr.audit(tc, "WTRN0100E: " +
                                                     "Server stopping but no reserved connection when closing SQL RecoveryLog " + _logName + " for server " + _serverName);
                                    }
                                    InternalLogException ile = new InternalLogException();
                                    throw ile;
                                }
                            } else {
                                initialReservedConnection = true;
                                // Test whether the reserved connection is closed already.
                                if (_reservedConn.isClosed()) {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Reserved Connection is already closed");
                                    // The caller has a reasonable expectation that the logs have been keypointed on a real or logical close
                                    // if that hasn't happened we really need to let them know - throw InternalLogException
                                    // we'll let the caller determin whether this is worth FFDC'ing
                                    InternalLogException ile = new InternalLogException();
                                    markFailed(ile);
                                    if (tc.isEntryEnabled())
                                        Tr.exit(tc, "closeLog called when _closesRequired is " + _closesRequired, ile);
                                    throw ile;
                                }
                            }

                            if (successfulConnection) {
                                forceSections(false); // we are holding _DBAccessIntentLock and intrinsic lock - must not block on throttle.
                                successfulSQL = true;
                                _closesRequired--;
                            }
                        } catch (PeerLostLogOwnershipException ple) {
                            // No FFDC in this case
                            if (tc.isEntryEnabled())
                                Tr.exit(tc, "closeLog", ple);
                            throw ple;
                        } catch (LogsUnderlyingTablesMissingException lutme) {
                            // No FFDC in this case
                            if (tc.isEntryEnabled())
                                Tr.exit(tc, "closeLog", lutme);
                            throw lutme;
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

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Closes required: " + _closesRequired);
                        try {
                            if (_closesRequired <= 0) // 0 indicates real, not just logical, close (it can't be <0 because it's properly synched)
                            {
                                if (initialReservedConnection && !_useNewLockingScheme) // No need to unlatch if new locking scheme in play
                                    unlatchHADBLock();
                                // Reset the internal state so that a subsequent open operation does not
                                // occurs with a "clean" environment.
                                _recoverableUnits = null;
                                _closesRequired = 0;
                            }
                        } catch (Throwable exc) {
                            // This shouldn't ever happen - collect some FFDC for service but functionally we don't care if we fail to unset the latch
                            // DON'T set success to false or markFailed etc.
                            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.closeLog", "550", this);
                            if (tc.isEventEnabled())
                                Tr.event(tc, "Unexpected exception caught in closeLog while unsetting the latch", exc);
                        }
                    } else {
                        // The caller has a reasonable expectation that the logs have been keypointed on a real or logical close
                        // if that hasn't happened we really need to let them know - throw InternalLogException
                        // we'll let the caller determin whether this is worth FFDC'ing
                        InternalLogException ile = new InternalLogException();
                        markFailed(ile);
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "closeLog called when _closesRequired is " + _closesRequired, ile);
                        throw ile;
                    }

                } finally {
                    if (!successfulSQL) {
                        if (_closesRequired > 0)
                            _closesRequired--;
                        if (_reservedConn != null)
                            try {
                                _reservedConn.close();
                            } catch (Exception e) {
                            }
                        _reservedConn = null;
                        _recoverableUnits = null;
                    }
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Closes required: " + _closesRequired);
            } // end synchronized(this)
        } // end synchronized(_DBAccessIntentLock);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "closeLog");
    }

    /**
    */
    @Override
    public void closeLogImmediate() throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "closeLogImmediate", this);

        synchronized (_DBAccessIntentLock) {
            synchronized (this) {
                _closesRequired = 0;
                if (_reservedConn != null)
                    try {
                        _reservedConn.close();
                    } catch (Exception e) {
                    }
                _reservedConn = null;
                _recoverableUnits = null;
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "closeLogImmediate");
    }

    @Override
    public boolean delete() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "delete", _closesRequired, this);

        boolean succeeded = false;
        if (_deleted) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "This log has already been deleted");
            succeeded = true;
        } else if (_isTableDeleted) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The tables are missing");
            _deleted = true;
            if (_associatedLog != null)
                _associatedLog.markDeletedByAssociation();
            succeeded = true;
        } else {
            int initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;
            synchronized (_DBAccessIntentLock) {
                synchronized (this) {
                    if (failed() || _closesRequired > 0) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Do not delete logs as failed state is " + failed() + " or closesRequired is " + _closesRequired);
                        }
                    } else { // the log is in the right state, we can proceed
                        Connection conn = null;
                        String tableToDrop = "PARTNER_LOG";
                        try {
                            conn = getConnection();
                            // Set autocommit FALSE and RR isolation on the connection
                            initialIsolation = prepareConnectionForBatch(conn);
                            succeeded = dropDBTable(conn, tableToDrop);
                            if (succeeded) {
                                tableToDrop = "TRAN_LOG";
                                succeeded = dropDBTable(conn, tableToDrop);
                            }
                            if (succeeded) {
                                succeeded = false;
                                conn.commit();
                                succeeded = true;
                                _deleted = true;
                                if (_associatedLog != null)
                                    _associatedLog.markDeletedByAssociation();
                            } else {
                                conn.rollback();
                            }
                        } catch (Exception e) {
                            // Issue a warning but allow processing to continue
                            Tr.audit(tc,
                                     "WTRN0107W: Caught exception {0} when attempting to drop table {1} during deletion of recovery log {2} by server {3}",
                                     e, tableToDrop, _logName, _serverName);
                        } finally {
                            // Attempt a close. If it fails, trace the failure but allow processing to continue
                            try {
                                if (conn != null)
                                    closeConnectionAfterBatch(conn, initialIsolation);
                            } catch (Throwable exc) {
                                // Trace the exception
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Close Failed, after delete, got exception: " + exc);
                            }
                        }
                    }
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "delete", succeeded);
        return succeeded;

    }

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
     * @exception LogClosedException   Thrown if the recovery log is closed and must be
     *                                     opened before this call can be issued.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    @Override
    public synchronized RecoverableUnit createRecoverableUnit(FailureScope failureScope) throws LogClosedException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoverableUnit", new Object[] { failureScope, this });

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createRecoverableUnit", "InternalLogException");
            throw getFailureException();
        }

        // Check that the log is actually open.
        if (_closesRequired == 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createRecoverableUnit", "LogClosedException");
            throw new LogClosedException(null);
        }

        SQLRecoverableUnitImpl recoverableUnit = null;

        long identity = _recUnitIdTable.nextId(this);
        recoverableUnit = new SQLRecoverableUnitImpl(this, identity, failureScope);
        if (tc.isEventEnabled())
            Tr.event(tc, "SQLMultiScopeRecoveryLog '" + _logName + "' created a new RecoverableUnit with id '" + identity + "'");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoverableUnit", recoverableUnit);
        return recoverableUnit;
    }

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
     * @exception LogClosedException              Thrown if the recovery log is closed and must be
     *                                                opened before this call can be issued.
     * @exception InvalidRecoverableUnitException Thrown if the RecoverableUnit does not exist.
     * @exception InternalLogException            Thrown if an unexpected error has occured.
     */
    @Override
    public void removeRecoverableUnit(long identity) throws LogClosedException, InvalidRecoverableUnitException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeRecoverableUnit", new Object[] { identity, this });

        // PH22988 introduces "throttle" code to improve concurrent access to the cache of inserts, updates and removes at runtime.
        // If the throttle has been enabled, then this method will wait to be notified before adding more work to the cache.
        _throttleLock.lock();
        try {
            while (_throttleEnabled) {
                _throttleCleared.awaitUninterruptibly();
            }
        } finally {
            _throttleLock.unlock();
        }

        synchronized (this) {

            // If this recovery log instance has experienced a serious internal error then prevent this operation from
            // executing.
            if (failed()) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "removeRecoverableUnit", "InternalLogException");
                throw getFailureException();
            }

            // Ensure the log is actually open.
            if (_closesRequired == 0) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "removeRecoverableUnit", "LogClosedException");
                throw new LogClosedException(null);
            }

            SQLRecoverableUnitImpl recoverableUnit = null;

            recoverableUnit = removeRecoverableUnitMapEntries(identity);

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
                ruForReplay deleteRU = new ruForReplay(identity, 0, 0, null);

                if (_writeToCacheA)
                    _cachedRemovesA.add(deleteRU);
                else
                    _cachedRemovesB.add(deleteRU);
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
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeRecoverableUnit");
    }

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
     *                                   opened before this call can be issued.
     */
    @Override
    public synchronized LogCursor recoverableUnits(FailureScope failureScope) throws LogClosedException /* @MD19706C */
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoverableUnits", new Object[] { failureScope, this });

        // Check that the log is actually open
        if (_closesRequired <= 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoverableUnits", "LogClosedException");
            throw new LogClosedException(null);
        }

        final List<SQLRecoverableUnitImpl> recoverableUnits = new ArrayList<SQLRecoverableUnitImpl>();

        // No need to access this inside a sync block as the caller is required to
        // hold off from changing the underlying structures whilst the cursor is open.
        for (SQLRecoverableUnitImpl recoverableUnit : _recoverableUnits.values()) {
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

    @Override
    public synchronized RecoverableUnit lookupRecoverableUnit(long identity) throws LogClosedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "lookupRecoverableUnit", new Object[] { Long.valueOf(identity), this });

        RecoverableUnit runit = getRecoverableUnit(identity);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "lookupRecoverableUnit", runit);
        return runit;
    }

    @Override
    public RecoverableUnit createRecoverableUnit() throws LogClosedException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoverableUnit", this);

        final RecoverableUnit runit = createRecoverableUnit(_failureScope);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoverableUnit", runit);
        return runit;
    }

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

    /**
     * <p>
     * Instructs the recovery log to perfom a keypoint operation. Any redundant
     * information will be removed and all cached information will be forced to disk.
     * </p>
     *
     * @exception LogClosedException       Thrown if the log is closed.
     * @exception InternalLogException     Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                                         log that is not compatible with this version
     *                                         of the service.
     */
    @Override
    public void keypoint() throws InternalLogException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "keypoint", this);
        forceSections(false);
    }

    // Added this method to be used by the SQLRecoverableUnitImpl.writeSections so that all the sections can be written in synchronized block without that class synching on object ref.
    void writeSections(Iterator<SQLRecoverableUnitSectionImpl> sections) throws InternalLogException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "writeSections", sections);

        if (sections != null) {
            // PH22988 introduces "throttle" code to improve concurrent access to the cache of inserts, updates and removes at runtime.
            // If the throttle has been enabled, then this method will wait to be notified before adding more work to the cache.
            _throttleLock.lock();
            try {
                while (_throttleEnabled) {
                    _throttleCleared.awaitUninterruptibly();
                }
            } finally {
                _throttleLock.unlock();
            }
            synchronized (this) { // Make sure all sections are written to same cache/occur in same force
                while (sections.hasNext()) {
                    SQLRecoverableUnitSectionImpl section = sections.next();

                    // Now direct the recoverable unit section to write its content. If the recoverable unit
                    // section has no data to write then this will be a no-op.
                    section.write(false);
                }
            }
        }
    }

    public void writeRUSection(long ruId, long sectionId, int index, byte[] data, boolean throttle) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writeRUSection ", new Object[] { ruId, sectionId, index, data, Boolean.valueOf(throttle), this });

        // PH22988 introduces "throttle" code to improve concurrent access to the cache of inserts, updates and removes at runtime.
        // If the throttle has been enabled, then this method will wait to be notified before adding more work to the cache.
        if (throttle) {
            _throttleLock.lock();
            try {
                while (_throttleEnabled) {
                    _throttleCleared.awaitUninterruptibly();
                }
            } finally {
                _throttleLock.unlock();
            }
        }

        synchronized (this) {
            // If this recovery log instance has experienced a serious internal error then prevent this operation from
            // executing.

            if (failed()) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "writeRUSection", "InternalLogException");
                throw getFailureException();
            }
            if (_closesRequired <= 0) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "writeRUSection", this);
                throw new InternalLogException("Already closed");
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
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "writeRUSection");
    }

    public void internalWriteRUSection(long ruId, long sectionId, int index, byte[] data, boolean replaying) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "internalWriteRUSection ", new Object[] { ruId, sectionId, index, data, replaying, this });

        if (tc.isEventEnabled()) {
            Tr.event(tc, "sql tranlog: writing ruId: " + ruId);
            Tr.event(tc, "sql tranlog: writing sectionId: " + sectionId);
            Tr.event(tc, "sql tranlog: writing item: " + index);
            Tr.event(tc, "sql tranlog: writing data: " + RLSUtils.toHexString(data, RLSUtils.MAX_DISPLAY_BYTES));
        }

        ruForReplay insertRU = new ruForReplay(ruId, sectionId, index, data);
        if (_writeToCacheA)
            _cachedInsertsA.add(insertRU);
        else
            _cachedInsertsB.add(insertRU);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "internalWriteRUSection");
    }

    public void updateRUSection(long ruId, long sectionId, byte[] data, boolean throttle) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "updateRUSection ", new Object[] { ruId, sectionId, data, Boolean.valueOf(throttle), this });

        // PH22988 introduces "throttle" code to improve concurrent access to the cache of inserts, updates and removes at runtime.
        // If the throttle has been enabled, then this method will wait to be notified before adding more work to the cache.
        if (throttle) {
            _throttleLock.lock();
            try {
                while (_throttleEnabled) {
                    _throttleCleared.awaitUninterruptibly();
                }
            } finally {
                _throttleLock.unlock();
            }
        }

        synchronized (this) {
            // If this recovery log instance has experienced a serious internal error then prevent this operation from
            // executing.
            if (failed()) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "updateRUSection", "InternalLogException");
                throw getFailureException();
            }
            if (_closesRequired <= 0) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "updateRUSection", this);
                throw new InternalLogException("Already closed");
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
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "updateRUSection");
    }

    public void internalUpdateRUSection(long ruId, long sectionId, byte[] data, boolean replaying) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "internalUpdateRUSection ", new Object[] { ruId, sectionId, data, replaying, this });

        if (tc.isEventEnabled()) {
            Tr.event(tc, "sql tranlog: updating ruId: " + ruId);
            Tr.event(tc, "sql tranlog: updating sectionId: " + sectionId);
            Tr.event(tc, "sql tranlog: updating data: " + RLSUtils.toHexString(data, RLSUtils.MAX_DISPLAY_BYTES));
        }

        ruForReplay updateRU = new ruForReplay(ruId, sectionId, 0, data);
        if (_writeToCacheA)
            _cachedUpdatesA.add(updateRU);
        else
            _cachedUpdatesB.add(updateRU);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "internalUpdateRUSection");
    }

    public void forceSections() throws InternalLogException {
        forceSections(true);
    }

    public void forceSections(boolean throttle) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "forceSections", this, throttle);

        // PH22988 introduces "throttle" code to improve concurrent access to the cache of inserts, updates and removes at runtime.
        // _throttleWaiters counts the number of threads attempting to forceSections(). If that number passes a threshold, then throttling is enabled.
        if (throttle) {
            _throttleLock.lock();
            _throttleWaiters++;
            if (_throttleWaiters >= _waiterThreshold) {
                _throttleEnabled = true;
            }
            _throttleLock.unlock();

            try {
                internalForceSections();
            } finally {
                _throttleLock.lock();
                try {
                    // Decrement the number of _throttleWaiters. If that number has returned to 0, then disable throttling and notify all
                    // threads that are waiting to add work to the cache of inserts, updates and removes.
                    _throttleWaiters--;
                    if (_throttleWaiters == 0) {
                        _throttleEnabled = false;
                        _throttleCleared.signalAll();
                    }
                } finally {
                    _throttleLock.unlock();
                }
            }
        } else {
            internalForceSections();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "forceSections");
    }

    // Must NOT be called when holding intrinsic lock unless already holding _DBAccessIntentLock
    @FFDCIgnore({ SQLException.class, SQLRecoverableException.class })
    void internalForceSections() throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "internalForceSections", new java.lang.Object[] { this, _reservedConn });

        Connection conn = null;
        boolean sqlSuccess = false;
        boolean successfulConnection = true;
        SQLException currentSqlEx = null;
        Throwable nonTransientException = null;
        int initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;

        // NOTE must still go into synch even if no work for us to to since this threads must not return until after the thread that updated the db has finished the update.
        // The outer sync prevents two threads writing at the same time AND prevents more than one flip of which cache is in use.
        // This allows other threads to write into one cache while one thread is writing the current cache.
        synchronized (_DBAccessIntentLock) {
            boolean somethingToDo = false;
            List<ruForReplay> cachedInserts, cachedUpdates, cachedRemoves;
            synchronized (this) {

                if (failed()) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "internalForceSections", new Object[] { this, "Already Failed" });
                    throw getFailureException();
                }

                if (_closesRequired <= 0) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "internalForceSections", new Object[] { this, "Already Closed" });
                    throw new InternalLogException("Already Closed");
                }

                cachedInserts = _writeToCacheA ? _cachedInsertsA : _cachedInsertsB;
                cachedUpdates = _writeToCacheA ? _cachedUpdatesA : _cachedUpdatesB;
                cachedRemoves = _writeToCacheA ? _cachedRemovesA : _cachedRemovesB;
                if (cachedRemoves.size() != 0 || cachedInserts.size() != 0 || cachedUpdates.size() != 0) {
                    _writeToCacheA = !_writeToCacheA;
                    somethingToDo = true;
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "SOMETHING to do Remove size: " + cachedRemoves.size() + ", Insert size: " + cachedInserts.size() + ", Updates size: " + cachedUpdates.size());
                } else if (tc.isDebugEnabled())
                    Tr.debug(tc, "NOTHING to do");
            } // end synch this

            // we've dropped this object's intrinsic lock since we check failed/_closesRequired but that's OK because
            // we still hold the _DBAccessIntentLock which protects _closesRequired and...
            // ultimately we only care about changes to failed made while holding the _DBAccessIntentLock too

            if (somethingToDo) {
                try {
                    // Get a connection to database via its datasource
                    if (_reservedConn == null) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Reserved Connection is NULL, attempt to get new DataSource connection");
                        if (!_serverStopping) {
                            conn = getConnection();
                        } else {
                            InternalLogException ile = reportOperationAttemptWhenStopping("forcing");

                            if (tc.isEntryEnabled())
                                Tr.exit(tc, "internalForceSections", "InternalLogException");
                            throw ile;
                        }
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Drive SQL using reserved connection");
                        conn = _reservedConn;
                    }

                    // This next piece of logic uses the HA Lock scheme to detect whether another process has taken over this server's
                    // logs. Use RDBMS SELECT FOR UPDATE to lock table and access the HA Lock row in the table.
                    if (successfulConnection) {
                        initialIsolation = prepareConnectionForBatch(conn);

                        // This will confirm that this server owns this log and will invalidate the log if not.
                        boolean lockSuccess = assertLogOwnershipAtRuntime(conn);

                        if (lockSuccess) {
                            // We can go ahead and write to the Database
                            executeBatchStatements(conn);
                        }

                        conn.commit();
                        sqlSuccess = true;
                    }
                }
                // Catch and report an SQLException. In the finally block we'll determine whether the condition is transient or not.
                catch (SQLException sqlex) {
                    // Set the exception that will be reported
                    currentSqlEx = sqlex;
                    if (_useNewLockingScheme) {
                        if (_isHomeServer) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Home server and the underlying table may have been deleted");
                            if (isTableDeleted(sqlex)) {
                                // The underlying table has been deleted, set exception variables to NOT retry
                                LogsUnderlyingTablesMissingException lutme = new LogsUnderlyingTablesMissingException("Underlying table is missing", null);
                                nonTransientException = lutme;
                                currentSqlEx = null;
                                _isTableDeleted = true;
                                if (_associatedLog != null)
                                    _associatedLog.markTablesDeletedByAssociation();
                                Tr.audit(tc, "WTRN0107W: " +
                                             "Underlying SQL tables missing when forcing SQL RecoveryLog " + _logName + " for server " + _serverName);
                            }
                        } else {
                            if (!ConfigurationProviderManager.getConfigurationProvider().peerRecoveryPrecedence()) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Not the home server and the underlying table may have been deleted");
                                if (isTableDeleted(sqlex)) {
                                    PeerLostLogOwnershipException ple = new PeerLostLogOwnershipException("Underlying table is missing", null);
                                    // Set exception variables to NOT retry
                                    nonTransientException = ple;
                                    currentSqlEx = null;
                                }
                            }
                        }
                    }
                    if (currentSqlEx != null) {
                        Tr.audit(tc, "WTRN0107W: " +
                                     "Caught SQLException when forcing SQL RecoveryLog " + _logName + " for server " + _serverName + " SQLException: " + currentSqlEx);
                    }
                } catch (PeerLostLogOwnershipException ple) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Caught PeerLostLogOwnershipException: " + ple);
                    nonTransientException = ple;
                } catch (LogsUnderlyingTablesMissingException lutme) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Caught LogsUnderlyingTablesMissingException: " + lutme);
                    nonTransientException = lutme;
                } catch (Throwable exc) {
                    Tr.audit(tc, "WTRN0107W: " +
                                 "Caught non-SQLException Throwable when forcing SQL RecoveryLog " + _logName + " for server " + _serverName + " Throwable: " + exc);
                    nonTransientException = exc;
                } finally {
                    if (conn != null) {
                        if (sqlSuccess) {
                            // Don't want to close the reserved connection
                            if (_reservedConn == null) {
                                try {
                                    closeConnectionAfterBatch(conn, initialIsolation);
                                } catch (Throwable exc) {
                                    // Trace the exception
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Close Failed, after force sections success, got exception: " + exc);
                                }
                            }
                        } else {
                            // Tidy up current connection before dropping into retry code.
                            // Attempt a rollback. If it fails, trace the failure but allow processing to continue
                            try {
                                if (conn != null)
                                    conn.rollback();
                            } catch (Throwable exc) {
                                // Trace the exception
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Rollback Failed, after force sections failure, got exception: " + exc);
                            }

                            // Attempt a close. If it fails, trace the failure but allow processing to continue
                            // Don't want to close the reserved connection
                            if (_reservedConn == null) {
                                try {
                                    if (conn != null)
                                        closeConnectionAfterBatch(conn, initialIsolation);
                                } catch (Throwable exc) {
                                    // Trace the exception
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Close Failed, after force sections failure, got exception: " + exc);
                                }
                            }

                            // if the server is stopping, return without driving any retry logic
                            if (_serverStopping) {
                                InternalLogException ile = reportOperationAttemptWhenStopping("forcing");

                                if (tc.isEntryEnabled())
                                    Tr.exit(tc, "internalForceSections", "InternalLogException");
                                throw ile;
                            }

                            boolean failAndReport = true;
                            // If currentSqlEx is non-null, then we potentially have a condition that may be retried. If it is not null, then
                            // the nonTransientException will have been set.
                            if (currentSqlEx != null) {
                                // Set the exception that will be reported
                                nonTransientException = currentSqlEx;

                                final ForceSectionsRetry forceSectionsRetry = new ForceSectionsRetry();
                                forceSectionsRetry.setNonTransientException(currentSqlEx);
                                // The following method will reset "nonTransientException" if it cannot recover
                                if (_sqlTransientErrorHandlingEnabled) {
                                    failAndReport = forceSectionsRetry.retryAfterSQLException(this, currentSqlEx);
                                    if (failAndReport)
                                        nonTransientException = forceSectionsRetry.getNonTransientException();
                                }
                            }

                            // We've either been through the while loop or we'd encountered a non-transient exception on initial execution
                            if (failAndReport) {
                                // In the case where peer log ownership has been lost, either on first execution or retry, then re-throw the exception
                                // without auditing.
                                if (nonTransientException instanceof PeerLostLogOwnershipException) {
                                    if (tc.isEntryEnabled())
                                        Tr.exit(tc, "internalForceSections", "PeerLostLogOwnershipException");
                                    PeerLostLogOwnershipException ple = new PeerLostLogOwnershipException(nonTransientException);
                                    throw ple;
                                } else if (nonTransientException instanceof LogsUnderlyingTablesMissingException) {
                                    markFailed(nonTransientException, false, false); // second parameter "false" as we do not wish to fire out error messages
                                    if (tc.isEntryEnabled())
                                        Tr.exit(tc, "internalForceSections", "LogsUnderlyingTablesMissingException");
                                    LogsUnderlyingTablesMissingException lutme = new LogsUnderlyingTablesMissingException(nonTransientException);
                                    throw lutme;
                                } else {
                                    Tr.audit(tc, "WTRN0100E: " +
                                                 "Cannot recover from SQLException when forcing SQL RecoveryLog " + _logName + " for server " + _serverName + " Exception: "
                                                 + nonTransientException);
                                    markFailed(nonTransientException);

                                    InternalLogException ile = new InternalLogException(nonTransientException);
                                    if (tc.isEntryEnabled())
                                        Tr.exit(tc, "internalForceSections", ile);
                                    throw ile;
                                }
                            } else
                                Tr.audit(tc, "WTRN0108I: " +
                                             "Have recovered from SQLException when forcing SQL RecoveryLog " + _logName + " for server " + _serverName);
                        }
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Connection was NULL");

                        Tr.audit(tc, "WTRN0100E: " +
                                     "Cannot recover from SQLException when forcing SQL RecoveryLog " + _logName + " for server " + _serverName + " Exception: "
                                     + nonTransientException);
                        markFailed(nonTransientException);
                        InternalLogException ile = new InternalLogException(nonTransientException);
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "internalForceSections", ile);
                        throw ile;
                    }

                    // Ensure that we have cleared the replayable caches
                    synchronized (this) {
                        cachedInserts.clear();
                        cachedUpdates.clear();
                        cachedRemoves.clear();
                    }
                }
            } else { // there was no work to force
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "No inserts, updates or deletes");
            }
        } // end outer sync (_DBAccessIntentLock)

        if (tc.isEntryEnabled())
            Tr.exit(tc, "internalForceSections");
    }

    /**
     * Drives the execution of the cached up database work.
     *
     * @exception SQLException thrown if a SQLException is
     *                             encountered when accessing the
     *                             Database.
     */
    private void executeBatchStatements(Connection conn) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "executeBatchStatements", new java.lang.Object[] { conn, this });

        PreparedStatement insertStatement = null;
        PreparedStatement updateStatement = null;
        PreparedStatement removeStatement = null;

        // If we're writing new data TO A we must be writing existing data FROM B and vice-versa.
        boolean _writeFromCacheA = !_writeToCacheA;

        // So we still hold the _DBAccessIntentLock but NOT this's intrinisic lock so how can we access the _writeToCacheA and _cached* state???
        // _writeToCacheA is only ever modified under the _DBAccessIntentLock (as well as this's intrinisic lock).
        // whichever _cached* is going to be used to write FROM has not been modified since we dropped this's intrinsic lock
        // since any other threads will be modifying the other set of _cached* maps currently being written TO
        final List<ruForReplay> cachedInserts = _writeFromCacheA ? _cachedInsertsA : _cachedInsertsB;
        final List<ruForReplay> cachedUpdates = _writeFromCacheA ? _cachedUpdatesA : _cachedUpdatesB;
        final List<ruForReplay> cachedRemoves = _writeFromCacheA ? _cachedRemovesA : _cachedRemovesB;
        int inserts = cachedInserts.size();
        int updates = cachedUpdates.size();
        int removes = cachedRemoves.size();
        try {
            // Prepare the statements
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Prepare the INSERT statement for " + inserts + " inserts");

            if (inserts > 0) {
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
                Tr.debug(tc, "Prepare the UPDATE statement for " + updates + " updates");

            if (updates > 0) {
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
                Tr.debug(tc, "Prepare the DELETE statement for " + removes + " removes");

            if (removes > 0) {
                String removeString = "DELETE FROM " + _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix + " WHERE " +
                                      "SERVER_NAME = ? AND SERVICE_ID = ? AND RU_ID = ? ";
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "DELETE string - " + removeString);
                removeStatement = conn.prepareStatement(removeString);
                removeStatement.setString(1, _serverName);
                removeStatement.setShort(2, (short) _recoveryAgent.clientIdentifier());
            }

            // Batch the INSERT statements
            if (inserts > 0) {
                for (ruForReplay element : cachedInserts) {
                    insertStatement.setLong(3, element.getRuId());
                    insertStatement.setLong(4, element.getSectionId());
                    insertStatement.setShort(5, (short) element.getIndex());
                    insertStatement.setBytes(6, element.getData());
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Insert row, serverName: " + _serverName + " clientId: " + (short) _recoveryAgent.clientIdentifier()
                                     + " ruId: " + element.getRuId() + " sectionId: " + element.getSectionId() + " item: " + (short) element.getIndex());
                    insertStatement.addBatch();
                }
            }

            // Batch the UPDATE statements
            if (updates > 0) {
                for (ruForReplay element : cachedUpdates) {
                    updateStatement.setLong(4, element.getRuId());
                    updateStatement.setLong(5, element.getSectionId());
                    updateStatement.setBytes(1, element.getData());
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Update row, serverName: " + _serverName + " clientId: " + (short) _recoveryAgent.clientIdentifier()
                                     + " ruId: " + element.getRuId() + " sectionId: " + element.getSectionId());
                    updateStatement.addBatch();
                }
            }

            // Batch the DELETE statements
            if (removes > 0) {
                for (ruForReplay element : cachedRemoves) {
                    removeStatement.setLong(3, element.getRuId());
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Delete row, serverName: " + _serverName + " clientId: " + (short) _recoveryAgent.clientIdentifier()
                                     + " ruId: " + element.getRuId());
                    removeStatement.addBatch();
                }
            }

            // Execute the statements
            if (inserts > 0) {
                int[] numUpdates = insertStatement.executeBatch();
                if (tc.isDebugEnabled()) {
                    for (int i = 0; i < numUpdates.length; i++) {
                        if (numUpdates[i] == Statement.SUCCESS_NO_INFO)
                            Tr.debug(tc, "Execution " + i + ": unknown number of rows updated");
                        else
                            Tr.debug(tc, "Execution " + i + " successful: " + numUpdates[i] + " rows updated");
                    }
                }
            }
            if (tc.isEventEnabled())
                Tr.event(tc, "sql tranlog: batch inserts: " + inserts);

            if (updates > 0) {
                int[] numUpdates = updateStatement.executeBatch();
                if (tc.isDebugEnabled()) {
                    for (int i = 0; i < numUpdates.length; i++) {
                        if (numUpdates[i] == Statement.SUCCESS_NO_INFO)
                            Tr.debug(tc, "Execution " + i + ": unknown number of rows updated");
                        else
                            Tr.debug(tc, "Execution " + i + " successful: " + numUpdates[i] + " rows updated");
                    }
                }
            }
            if (tc.isEventEnabled())
                Tr.event(tc, "sql tranlog: batch updates: " + updates);

            if (removes > 0) {
                int[] numUpdates = removeStatement.executeBatch();
                if (tc.isDebugEnabled()) {
                    for (int i = 0; i < numUpdates.length; i++) {
                        if (numUpdates[i] == Statement.SUCCESS_NO_INFO)
                            Tr.debug(tc, "Execution " + i + ": unknown number of rows updated");
                        else
                            Tr.debug(tc, "Execution " + i + " successful: " + numUpdates[i] + " rows updated");
                    }
                }
            }
            if (tc.isEventEnabled())
                Tr.event(tc, "sql tranlog: batch deletes: " + removes + ", for obj: " + this);
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

    /**
     * Takes a row lock against the database table that is being used
     * for the recovery log. This fulfils the same role as the file
     * locking scheme in conventional transaction logging to a
     * filesystem. The intent is that only one server should be
     * logging to a specific table at a time. There is sometimes a
     * lag in peer recovery where an old server is closing down while
     * a new server is opening the same log for peer recovery.
     *
     * @exception SQLException         thrown if a SQLException is
     *                                     encountered when accessing the
     *                                     Database.
     * @exception InternalLogException Thrown if an
     *                                     unexpected error has occured.
     */
    private boolean assertLogOwnershipAtRuntime(Connection conn) throws SQLException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "assertLogOwnershipAtRuntime", new java.lang.Object[] { conn, this });

        boolean lockSuccess = false;

        final String queryString = "SELECT SERVER_NAME" +
                                   " FROM " + _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                                   (DBProduct.Sqlserver == dbProduct ? " WITH (ROWLOCK, UPDLOCK, HOLDLOCK)" : "") +
                                   " WHERE RU_ID=-1" +
                                   (DBProduct.Sqlserver == dbProduct ? "" : " FOR UPDATE");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Attempt to select the HA LOCKING ROW for UPDATE using - " + queryString);

        try (Statement lockingStmt = conn.createStatement(); ResultSet lockingRS = lockingStmt.executeQuery(queryString)) {
            if (lockingRS.next()) {
                // We found the HA Lock row
                String storedServerName = lockingRS.getString(1);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Acquired lock on HA Lock row, stored server value is: " + storedServerName);
                if (_currentProcessServerName.equalsIgnoreCase(storedServerName)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "This server OWNS the HA lock row as expected");
                    lockSuccess = true;
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "ANOTHER server OWNS the lock row - we need to mark the log as failed");

                    // How we react depends on whether we are closing the home server or a peer. In the peer case a home
                    // server may have re-acquired its logs
                    if (_useNewLockingScheme && !_isHomeServer) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Not the home server, failurescope is " + _failureScope);
                        // Instantiate a PeerLostLogOwnershipException which is less "noisy" than its parent InternalLogException
                        final PeerLostLogOwnershipException ple = new PeerLostLogOwnershipException("Another server (" + storedServerName + ") has locked the HA lock row", null);
                        markFailed(ple, false, true); // second parameter "false" as we do not wish to fire out error messages
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "assertLogOwnershipAtRuntime", ple);
                        throw ple;
                    } else {
                        Tr.audit(tc, "WTRN0100E: " +
                                     "Another server (" + storedServerName + ") owns the log cannot force SQL RecoveryLog " + _logName + " for server " + _serverName);

                        final InternalLogException ile = new InternalLogException("Another server (" + storedServerName + ") has locked the HA lock row", null);
                        markFailed(ile);
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "assertLogOwnershipAtRuntime", ile);
                        throw ile;
                    }
                }
            } else {
                // We didn't find the HA Lock row in the table, mark the log as failed
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Could not find HA Lock row");
                final InternalLogException ile = new InternalLogException("Could not find the HA lock row", null);
                Tr.audit(tc, "WTRN0100E: " +
                             "Could not find HA lock row when forcing SQL RecoveryLog " + _logName + " for server " + _serverName);
                markFailed(ile);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "assertLogOwnershipAtRuntime", ile);
                throw ile;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "assertLogOwnershipAtRuntime", lockSuccess);
        return lockSuccess;
    }

    private boolean isTableDeleted(SQLException sqlex) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isTableDeleted ", sqlex);
        boolean noTable = false;
        final int sqlErrorCode = sqlex.getErrorCode();
        final String sqlState = sqlex.getSQLState();
        final String sqlMessage = sqlex.getMessage();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Message: {0}", sqlMessage);
            Tr.debug(tc, "SQL State: {0}", sqlState);
            Tr.debug(tc, "Error code: {0}", sqlErrorCode);
        }

        switch (dbProduct) {
            case DB2:
                if (sqlErrorCode == -204)
                    noTable = true;
                break;
            case Derby:
                if ("42X05".equals(sqlState))
                    noTable = true;
                break;
            case Oracle:
                if (sqlErrorCode == 942)
                    noTable = true;
                break;
            case Postgresql:
                if (sqlMessage.contains("relation") && sqlMessage.contains("does not exist"))
                    noTable = true;
                break;
            case Sqlserver:
                if (sqlErrorCode == 208)
                    noTable = true;
                break;
            default:
                break;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isTableDeleted", noTable);
        return noTable;
    }

    /**
     * Acquires ownership of the special row used in the HA locking
     * scheme. There is sometimes a lag in peer recovery where an old
     * server is closing down while a new server is opening the same
     * log for peer recovery.
     * This method ALWAYS calls either INSERT or UPDATE IF it doesn't throw an exception
     * consequently it can close statements in lower isolation levels that RR and still hold the lock
     * until the transaction is completed.
     *
     * @exception SQLException         thrown if a SQLException is
     *                                     encountered when accessing the
     *                                     Database.
     * @exception InternalLogException Thrown if an
     *                                     unexpected error has occured.
     */
    private void assertLogOwnershipAtOpenWithLatching(Connection conn) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "assertLogOwnershipAtOpenWithLatching", new java.lang.Object[] { conn, this });

        boolean lockingRecordExists = false;

        int latchRetryCount = 0;
        boolean needToRetryLatch = false;
        do {
            needToRetryLatch = false;
            try (Statement readForUpdateStmt = conn.createStatement(); ResultSet readForUpdateRS = readHADBLock(readForUpdateStmt, _logIdentifierString)) {
                if (readForUpdateRS.next()) {
                    // We found the HA Lock row
                    lockingRecordExists = true;
                    String storedServerName = readForUpdateRS.getString(1);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Acquired lock on HA Lock row. Stored server value is: " + storedServerName);
                    if (_currentProcessServerName.equalsIgnoreCase(storedServerName)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "This server ALREADY OWNS the HA lock row");
                        // may need to reset RUSECTION_ID which we use as a latch
                        Long latch = readForUpdateRS.getLong(2);
                        if (_reservedConnectionActiveSectionIDSet == latch)
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "latch is set in HA lock row - it will be unset when we perform the UPDATE");
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "ANOTHER server OWNS the lock");
                        // may need to wait for latch if it's set and we're a peer
                        Long latch = readForUpdateRS.getLong(2);
                        if ((_reservedConnectionActiveSectionIDSet == latch) && !(_isHomeServer)) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "latch is set in HA lock row for remote failurescope, latchRetryCount = " + latchRetryCount);
                            // hardcoded retry twice
                            if (++latchRetryCount < 3) {
                                // set for retry,  cleanup JDBC, drop locks and sleep to allow root server to complete
                                needToRetryLatch = true;
                                conn.rollback();
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ie) {
                                }
                            }
                        }
                    }
                } else {
                    lockingRecordExists = false;
                }
            }
        } while (needToRetryLatch);

        if (lockingRecordExists) {
            try (Statement updateStmt = conn.createStatement()) {
                final String updateString = "UPDATE " +
                                            _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                                            " SET SERVER_NAME = '" + _currentProcessServerName +
                                            "', RUSECTION_ID = " + _reservedConnectionActiveSectionIDUnset + " WHERE RU_ID = -1";

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Updating HA Lock using update string - " + updateString);
                final int ret = updateStmt.executeUpdate(updateString);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Have updated HA Lock row with return: " + ret);
            }
        } else {
            // Is this entirely necessary? We didn't find the HA Lock row in the table, perhaps we should barf
            // YES IT IS NECESSARY - we may be running against a table created before the locking row was added (which we now INSERT if/when
            // we create the table... and here (if is already exists without the locking row)
            insertLockingRow(conn, _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix, (short) 1, _reservedConnectionActiveSectionIDUnset);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "assertLogOwnershipAtOpenWithLatching");
    }

    /**
     * This method is called at the time that the log is opened where the new locking scheme has been configured. The method
     * reports the contents of the control, or HA Locking, row.
     *
     * @param conn
     * @throws SQLException
     */
    private void assertLogOwnershipAtOpenPeerLocking(Connection conn) throws SQLException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "assertLogOwnershipAtOpenPeerLocking", conn, this);

        boolean takeLock = false;

        try (Statement readForUpdateStmt = conn.createStatement();
                        ResultSet readForUpdateRS = readHADBLock(readForUpdateStmt, _logIdentifierString, "--assertLogOwnershipAtOpenPeerLocking")) {
            if (readForUpdateRS.next()) {
                // We found the HA Lock row
                String storedServerName = readForUpdateRS.getString(1);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Acquired lock on HA Lock row. Stored server value is: " + storedServerName);
                if (_currentProcessServerName.equalsIgnoreCase(storedServerName)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "This server OWNS the HA lock row");
                    takeLock = true;
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "ANOTHER server OWNS the lock");
                    // Report the cases under the new locking scheme.
                    if (_isHomeServer) {
                        // This is unexpected under the new locking scheme, we will assert our ownership of the log
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "The Home Server DOES NOT OWN the HA lock row");
                        takeLock = true;
                    } else {
                        // Handle peer server cases
                        if (ConfigurationProviderManager.getConfigurationProvider().peerRecoveryPrecedence()) {
                            // This is not expected under the new locking scheme, we will assert our ownership of the log
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "The Peer Server should OWN the HA lock row");
                            takeLock = true;
                        } else {
                            // The assumption is that a home server has claimed the logs aggressively
                            // and is in the process of recovering them.
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "The Peer Server NO LONGER OWNS the HA lock row");
                            // Instantiate a PeerLostLogOwnershipException which is less "noisy" than its parent InternalLogException
                            PeerLostLogOwnershipException ple = new PeerLostLogOwnershipException("Another server (" + storedServerName + ") has locked the HA lock row", null);
                            markFailed(ple, false, true); // second parameter "false" as we do not wish to fire out error messages
                            if (tc.isEntryEnabled())
                                Tr.exit(tc, "assertLogOwnershipAtOpenPeerLocking", ple);
                            throw ple;
                        }
                    }
                }

                if (takeLock) {
                    try (Statement updateStmt = conn.createStatement()) {
                        // Claim the logs by updating the server name and timestamp.
                        long fir1 = System.currentTimeMillis();
                        String updateString = "UPDATE " +
                                              _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                                              " SET SERVER_NAME = '" + _currentProcessServerName +
                                              "', RUSECTION_ID = " + fir1 +
                                              " WHERE RU_ID = -1";

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Updating HA Lock using update string - " + updateString);
                        int ret = updateStmt.executeUpdate(updateString);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Have updated HA Lock row with return: " + ret);
                    }
                }
            } else {
                // This is unexpected under the new locking scheme
                if (_isHomeServer || ConfigurationProviderManager.getConfigurationProvider().peerRecoveryPrecedence()) {
                    insertLockingRow(conn, _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix, (short) 1, Instant.now().toEpochMilli());
                } else {
                    // In this case instantiate a PeerLostLogOwnershipException which is less "noisy" than its parent InternalLogException
                    final PeerLostLogOwnershipException ple = new PeerLostLogOwnershipException("No lock row but this is peer recovery", null);
                    markFailed(ple, false, true); // second parameter "false" as we do not wish to fire out error messages
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "assertLogOwnershipAtOpenPeerLocking", ple);
                    throw ple;
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "assertLogOwnershipAtOpenPeerLocking");
    }

    /**
     * Creates the database table that is being used for the recovery
     * log.
     *
     * @exception SQLException thrown if a SQLException is
     *                             encountered when accessing the
     *                             Database.
     */
    private void createDBTable(Connection conn, String logIdentifierString) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createDBTable", conn, this);

        final String fullTableName = _recoveryTableName + logIdentifierString + _recoveryTableNameSuffix;

        try (Statement stmt = conn.createStatement()) {

            final String tableDDL;
            final String indexDDL;

            switch (dbProduct) {
                case Oracle:
                    tableDDL = genericTableCreatePreString + fullTableName + oracleTablePostString;
                    indexDDL = indexPreString + _recoveryIndexName + logIdentifierString + _recoveryTableNameSuffix +
                               " ON " + fullTableName + indexPostString;
                    createTableAndIndex(conn, stmt, tableDDL, indexDDL, true);
                    break;

                case DB2:
                    final String dbName = ConfigurationProviderManager.getConfigurationProvider().getTransactionLogDBName();
                    tableDDL = genericTableCreatePreString + fullTableName + db2TablePostString + (dbName.isEmpty() ? "" : (" IN DATABASE " + dbName));
                    indexDDL = indexPreString + _recoveryIndexName + logIdentifierString + _recoveryTableNameSuffix +
                               " ON " + fullTableName + indexPostString;
                    createTableAndIndex(conn, stmt, tableDDL, indexDDL, true);
                    break;

                case Postgresql:
                    tableDDL = genericTableCreatePreString + fullTableName + postgreSQLTablePostString;
                    indexDDL = indexPreString + _recoveryIndexName + logIdentifierString + _recoveryTableNameSuffix +
                               " ON " + fullTableName + postgreSQLIndexPostString;
                    createTableAndIndex(conn, stmt, tableDDL, indexDDL, false);
                    break;

                case Sqlserver:
                    tableDDL = genericTableCreatePreString + fullTableName + sqlServerTablePostString;
                    indexDDL = indexPreString + _recoveryIndexName + logIdentifierString + _recoveryTableNameSuffix +
                               " ON " + fullTableName + indexPostString;
                    createTableAndIndex(conn, stmt, tableDDL, indexDDL, false);
                    break;

                case Derby:
                case Unknown:
                    tableDDL = genericTableCreatePreString + fullTableName + genericTablePostString;
                    indexDDL = indexPreString + _recoveryIndexName + logIdentifierString + _recoveryTableNameSuffix +
                               " ON " + fullTableName + indexPostString;
                    createTableAndIndex(conn, stmt, tableDDL, indexDDL, true);
                    break;
            }
        } catch (SQLException e) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createDBTable", e);
            throw e;
        }

        // Insert the HA Locking row
        insertLockingRow(conn, fullTableName, (short) _recoveryAgent.clientIdentifier(), _useNewLockingScheme ? Instant.now().toEpochMilli() : 1);

        // The table and index creation may not be transactional but the INSERT of the locking row IS - commit
        conn.commit(); // the table and index creation may not be transactional but the INSERT of the locking row IS - commit

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createDBTable");
    }

    private void createTableAndIndex(Connection conn, Statement stmt, String tableDDL, String indexDDL, boolean useExecuteUpdate) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createTableAndIndex", conn, stmt, tableDDL, indexDDL, useExecuteUpdate);
        if (useExecuteUpdate) {
            stmt.executeUpdate(tableDDL);
            stmt.executeUpdate(indexDDL);
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "First, rollback connection.");
            conn.rollback();
            stmt.execute(tableDDL);
            stmt.execute(indexDDL);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "createTableAndIndex");
    }

    /**
     * Insert the control, or HA Locking, row containing metadata into the recovery log table
     */
    private void insertLockingRow(Connection conn, String fullTableName, short serviceId, long sectionId) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "insertLockingRow", conn, fullTableName, serviceId, sectionId, this);

        String insertString = "INSERT INTO " +
                              fullTableName +
                              " (SERVER_NAME, SERVICE_ID, RU_ID, RUSECTION_ID, RUSECTION_DATA_INDEX, DATA)" +
                              " VALUES (?,?,?,?,?,?)";

        final int ret;
        try (PreparedStatement specStatement = conn.prepareStatement(insertString)) {
            specStatement.setString(1, _currentProcessServerName);
            specStatement.setShort(2, serviceId);
            specStatement.setLong(3, -1); // NOTE RU_ID SET TO -1
            specStatement.setLong(4, sectionId);
            specStatement.setShort(5, (short) 1);
            byte buf[] = new byte[2];
            specStatement.setBytes(6, buf);
            ret = specStatement.executeUpdate();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "insertLockingRow", ret);
    }

    /**
     * Drops the database table that is being used for the recovery
     * log.
     *
     * @exception SQLException thrown if a SQLException is
     *                             encountered when accessing the
     *                             Database.
     */
    private boolean dropDBTable(Connection conn, String logIdentifierString) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "dropDBTable", new java.lang.Object[] { conn, logIdentifierString, this });

        boolean dropSuccess = false;
        String queryString = "SELECT SERVER_NAME" +
                             " FROM " + _recoveryTableName + logIdentifierString + _recoveryTableNameSuffix +
                             (DBProduct.Sqlserver == dbProduct ? " WITH (ROWLOCK, UPDLOCK, HOLDLOCK)" : "") +
                             " WHERE RU_ID=-1" +
                             (DBProduct.Sqlserver == dbProduct ? "" : " FOR UPDATE");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Attempt to select the HA LOCKING ROW using - " + queryString);

        try (Statement lockingStmt = conn.createStatement(); ResultSet lockingRS = lockingStmt.executeQuery(queryString)) {
            if (lockingRS.next()) {
                // We found the HA Lock row
                String storedServerName = lockingRS.getString(1);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Acquired lock row, stored server value is: " + storedServerName);
                if (_currentProcessServerName.equalsIgnoreCase(storedServerName)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "This server OWNS the lock row as expected");
                    dropSuccess = true;
                } else {
                    Tr.audit(tc, "WTRN0107W: " +
                                 "Another server (" + storedServerName + ") owns the log cannot delete SQL RecoveryLog " + _logName + " for server " + _serverName);
                }
            } else {
                Tr.audit(tc, "WTRN0107W: " +
                             "Could not find the lock row, so cannot delete SQL RecoveryLog " + _logName + " for server " + _serverName);
            }
        }

        // Proceed if we found the locking row and this server is the owner
        if (dropSuccess) {
            try (Statement dropTableStmt = conn.createStatement()) {

                String fullTableName = _recoveryTableName + logIdentifierString + _recoveryTableNameSuffix;

                String dropTableString = genericTableDropPreString + fullTableName;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "drop Table using: " + dropTableString);

                // Drop the table
                dropTableStmt.executeUpdate(dropTableString);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "dropDBTable", dropSuccess);
        return dropSuccess;
    }

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
    public synchronized void removing(Object target) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removing", new Object[] { target, this });

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removing", "InternalLogException");
            throw getFailureException();
        }
        if (_closesRequired <= 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removing", this);
            throw new InternalLogException("Already closed");
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
    @Override
    public boolean failed() {
        if (tc.isDebugEnabled() && _failed)
            Tr.debug(tc, "failed: RecoveryLog has been marked as failed. [" + this + "]");
        return _failed;
    }

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
        markFailed(t, true, false);
    }

    private void markFailed(Throwable t, boolean report, boolean peerServerLostLogOwnership) {
        boolean newFailure = false;
        synchronized (this) {
            if (!_failed) {
                if (tc.isDebugEnabled()) Tr.debug(tc, "markFailed: RecoveryLog has been marked as failed. [" + this + "]");
                newFailure = true;
                _failed = true;
            }

            if (peerServerLostLogOwnership) {
                // Set the variable that signals the loss of peer ownership
                _peerServerLostLogOwnership = true;
            } else {
                if (_peerServerLostLogOwnership) {
                    // Log has failed in a more severe fashion, reset the _peerServerLostLogOwnership
                    // parameter so that InternalLogExceptions are thrown rather than the quieter
                    // PeerLostLogOwnershipExceptions and set the newFailure parameter to ensure
                    // that auditing etc is enabled in this method.
                    newFailure = true;
                    _peerServerLostLogOwnership = false;
                }
            }

            if (newFailure) {
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

                // If this is a local recovery process and the server is not already stopping, then direct the server to terminate
                if (!_serverStopping && !_associateServerStopping) {
                    if (Configuration.HAEnabled() && ConfigurationProviderManager.getConfigurationProvider().isShutdownOnLogFailure()) {
                        if (Configuration.localFailureScope().equals(_failureScope)) {
                            // d254326 - output a message as to why we are terminating the server as in
                            // this case we never drop back to log any messages as for peer recovery.
                            Tr.error(tc, "CWRLS0024_EXC_DURING_RECOVERY", t);
                            if (ConfigurationProviderManager.getConfigurationProvider().isShutdownOnLogFailure()) {
                                _recoveryAgent.terminateServer();
                            }
                        } else {
                            Configuration.getRecoveryLogComponent().leaveGroup(_failureScope);
                        }
                    }
                }
            }
        }
        if (newFailure && _associatedLog != null) {
            if (_failAssociatedLog) {
                if (tc.isDebugEnabled() && _failed)
                    Tr.debug(tc, "associated log will be marked as failed", _associatedLog);
                _associatedLog.markFailedByAssociation(_peerServerLostLogOwnership);
            } else {
                if (!_peerServerLostLogOwnership)
                    _associatedLog.provideServiceability();
            }
        }
    }

    @Override
    public synchronized void markFailedByAssociation() {
        markFailedByAssociation(false);
    }

    private synchronized void markFailedByAssociation(boolean peerServerLostLogOwnership) {
        if (!_failed) {
            _failed = true;
            if (tc.isDebugEnabled() && _failed)
                Tr.debug(tc, "markFailedByAssociation: RecoveryLog has been marked as failed by association. [" + this + "]");
            if (peerServerLostLogOwnership)
                _peerServerLostLogOwnership = true;
            else if (!_isTableDeleted)
                provideServiceability();
        } else if (tc.isDebugEnabled() && _failed)
            Tr.debug(tc, "markFailedByAssociation: RecoveryLog was already failed when marked as failed by association. [" + this + "]");
    }

    /**
     * Logs are deleted together, so we can save work by marking an associated log as deleted.
     */
    public synchronized void markDeletedByAssociation() {
        if (!_deleted) {
            _deleted = true;
            if (tc.isDebugEnabled() && _deleted)
                Tr.debug(tc, "markDeletedByAssociation: RecoveryLog has been marked as deleted by association. [" + this + "]");
        } else if (tc.isDebugEnabled() && _deleted)
            Tr.debug(tc, "markDeletedByAssociation: RecoveryLog was already deleted when marked as deleted by association. [" + this + "]");
    }

    /**
     * Careful, this method is different from the other mark<operation>ByAssociation methods. In this case we are seeking to avoid attempts to
     * halt a server, when the server is already coming down.
     */
    public synchronized void markStoppingByAssociation() {
        if (!_associateServerStopping) {
            _associateServerStopping = true;
            if (tc.isDebugEnabled() && _associateServerStopping)
                Tr.debug(tc, "markStoppingByAssociation: Associate log has been marked as stopping. [" + this + "]");
        } else if (tc.isDebugEnabled() && _associateServerStopping)
            Tr.debug(tc, "markStoppingByAssociation: Associate log has already been marked as stopping. [" + this + "]");
    }

    public synchronized void markTablesDeletedByAssociation() {
        if (!_isTableDeleted) {
            _isTableDeleted = true;
            if (tc.isDebugEnabled() && _isTableDeleted)
                Tr.debug(tc, "markTablesDeletedByAssociation: RecoveryLog has been marked with tables deleted by association. [" + this + "]");
        } else if (tc.isDebugEnabled() && _isTableDeleted)
            Tr.debug(tc, "markTablesDeletedByAssociation: RecoveryLog was already marked with tables deleted. [" + this + "]");
    }

    /**
     * Returns an appropriate type of InternalLogException depending on whether the log belongs to a peer that has aggressively reclaimed its logs.
     *
     * @return InternalLogException
     */
    protected InternalLogException getFailureException() {
        if (_peerServerLostLogOwnership) {
            return new PeerLostLogOwnershipException(null);
        } else {
            return new InternalLogException("Peer server did not lose log ownership");
        }
    }

    /**
     * Adds a new RecoverableUnitImpl object, keyed from its identity to this
     * classes collection of such objects.
     *
     * @param recoverableUnit The RecoverableUnit to be added
     * @param recovered       Flag to indicate if this instances have been created during
     *                            recovery (true) or normal running (false). If its been created
     *                            during recovery we need to reserve the associated id so that
     *                            it can't be allocated to an independent RecoverableUnit.
     */
    protected void addRecoverableUnit(SQLRecoverableUnitImpl recoverableUnit, boolean recovered) {
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

    /**
     * Removes a RecoverableUnitImpl object, keyed from its identity from this
     * classes collection of such objects.
     * Not synchronized but it's only called by methods in this class that are (should be private)
     *
     * @param identity The identity of the RecoverableUnitImpl to be removed
     *
     * @return RecoverableUnitImpl The RecoverableUnitImpl thats no longer associated
     *         with the SQLMultiScopeRecoveryLog.
     */
    protected SQLRecoverableUnitImpl removeRecoverableUnitMapEntries(long identity) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeRecoverableUnitMapEntries", new Object[] { identity, this });

        final SQLRecoverableUnitImpl recoverableUnit = _recoverableUnits.remove(identity);

        // PI88168 - reusing recovered RU ids is more trouble than it's worth
        if (recoverableUnit != null && !recoverableUnit._recovered) {
            _recUnitIdTable.removeId(identity);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeRecoverableUnitMapEntries", recoverableUnit);
        return recoverableUnit;
    }

    /**
     * Retrieves a RecoverableUnitImpl object, keyed from its identity from this
     * classes collection of such objects.
     * Not synchronized but it's only called by methods in this class that are (should be private)
     *
     * @param identity The identity of the RecoverableUnitImpl to be retrieved
     *
     * @return RecoverableUnitImpl The required RecoverableUnitImpl
     */
    private SQLRecoverableUnitImpl getRecoverableUnit(long identity) throws LogClosedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getRecoverableUnit", new Object[] { identity, this });

        SQLRecoverableUnitImpl recoverableUnit = null;

        // Only attempt to resolve the recoverable unit if the log is valid and not closed.
        if (failed() || _closesRequired <= 0) {
            LogClosedException lce = new LogClosedException();
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getRecoverableUnit", lce);
            throw lce;
        }
        recoverableUnit = _recoverableUnits.get(identity);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getRecoverableUnit", recoverableUnit);
        return recoverableUnit;
    }

    /**
     * Returns the name of the server that owns this object instance. This may not
     * necessarly be the local server as this object may represent a recovery log
     * owned by a peer server.
     */
    String serverName() {
        return _serverName;
    }

    /**
     * Returns the name of the client service that owns this object instance.
     */
    String clientName() {
        return _clientName;
    }

    /**
     * Returns the version number of the client service that owns this object instance.
     */
    public int clientVersion() {
        return _clientVersion;
    }

    public String logName() {
        return _logName;
    }

    public int logIdentifier() {
        return _logIdentifier;
    }

    /**
     * Signals to the Recovery Log that the server is stopping.
     */
    @Override
    public void serverStopping() {
        SQLException transientException = null;
        synchronized (_DBAccessIntentLock) {
            synchronized (this) {
                if (tc.isEntryEnabled())
                    Tr.entry(tc, "serverStopping ", new Object[] { this });

                _serverStopping = true;
                if (_associatedLog != null)
                    _associatedLog.markStoppingByAssociation();

                // Stop Heartbeat Log alarm popping when server is on its way down
                if (_useNewLockingScheme) {
                    HeartbeatLogManager.stopTimeout();
                }

                // if the logs are gone we're done - a later close will fail.
                if (failed() || _closesRequired <= 0) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "serverStopping", this);
                    return;
                }

                // Need to establish a reserved connection at this point that can be used in log close processing.
                try {
                    // The BPM Test team found a case where this connection could be stale - a failover had been performed but no recovery log
                    // work had been done up to the point where the server stopped. We also need to try and stop a peer from snatching our logs so
                    // a) get a connection b) check we still own the lock and c) if necessary set the latch - do that in a retry loop
                    transientException = reserveConnection();
                } catch (Exception e) {
                    // swallow any exceptions - the lack of a reserved connection will be detected later and we should allow the serverStopping process to continue
                    FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.serverStopping", "3513", this);
                }

                if (transientException != null) {
                    Tr.audit(tc, "WTRN0107W: " +
                                 "Caught SQLException when server stopping for SQL RecoveryLog " + _logName + " for server " + _serverName + " SQLException: "
                                 + transientException);
                    // Should we attempt to reconnect? This method works through the set of SQL exceptions and will
                    // return the non-null transient exception if we determine that a transient DB error has occurred
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Try to reexecute the SQL using connection from DS: " + _theDS);

                    try {
                        transientException = reserveConnection();
                    } catch (Exception ex) {
                        // We've caught another Exception - give up.
                        // swallow any exceptions - the lack of a reserved connection will be detected later and we should allow the serverStopping process to continue
                        FFDCFilter.processException(ex, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.serverStopping", "3513", this);
                    }

                    // FFDC the case where we have retried (once) but a transient condition persists.
                    if (transientException != null) {
                        // Generate FFDC, but allow processing to continue
                        FFDCFilter.processException(transientException, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.serverStopping", "3507", this);
                    }
                }

            } // end synch this
        } // end synch _DBAccessIntentLock
        if (tc.isEntryEnabled())
            Tr.exit(tc, "serverStopping");
    }

    /**
     * Returns the string representation of this object instance.
     */
    @Override
    public String toString() {
        return _traceId;
    }

    /**
     * Returns the string representation of a log identifier.
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

    /**
     * This class is used to represent the cached up work that will
     * be committed to the database when the log is forced.
     */
    private class ruForReplay {
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

        if (otherLog instanceof SQLMultiScopeRecoveryLog) {
            _associatedLog = (SQLMultiScopeRecoveryLog) otherLog;
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
            HashMap<Long, SQLRecoverableUnitImpl> rus = _recoverableUnits;
            if (rus != null)
                FFDCFilter.processException(e, "com.ibm.ws.recoverylog.custom.jdbc.impl.SQLMultiScopeRecoveryLog.provideServiceability", "3628", rus);
        } catch (Exception ex) {
            // Do nothing
        }
    }

    // Sets AutoCommit and Isolation level for the connection
    @Override
    public int prepareConnectionForBatch(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
        int initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;

        if (DBProduct.DB2 == dbProduct) {
            try {
                initialIsolation = conn.getTransactionIsolation();
                if (Connection.TRANSACTION_REPEATABLE_READ != initialIsolation && Connection.TRANSACTION_SERIALIZABLE != initialIsolation) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Transaction isolation level was " + initialIsolation + " , setting to TRANSACTION_REPEATABLE_READ");
                    conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                }
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "setTransactionIsolation to RR threw Exception. Transaction isolation level was " + initialIsolation + " ", e);
                FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.prepareConnectionForBatch", "3668", this);
                if (!isolationFailureReported) {
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
    @Override
    public void closeConnectionAfterBatch(Connection conn, int initialIsolation) throws SQLException {
        if (DBProduct.DB2 == dbProduct) {
            if (Connection.TRANSACTION_REPEATABLE_READ != initialIsolation && Connection.TRANSACTION_SERIALIZABLE != initialIsolation)
                try {
                    conn.setTransactionIsolation(initialIsolation);
                } catch (Exception e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "setTransactionIsolation threw Exception. Specified transaction isolation level was " + initialIsolation + " ", e);
                    FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.SQLMultiScopeRecoveryLog.closeConnectionAfterBatch", "3696", this);
                    if (!isolationFailureReported) {
                        isolationFailureReported = true;
                        Tr.warning(tc, "CWRLS0024_EXC_DURING_RECOVERY", e);
                    }
                }
        }
        conn.setAutoCommit(true);
        conn.close();
    }

    // helper method
    private ResultSet readHADBLock(Statement lockingStmt, String logIdentifierString) throws SQLException {
        return readHADBLock(lockingStmt, logIdentifierString, "");
    }

    // helper method
    private ResultSet readHADBLock(Statement lockingStmt, String logIdentifierString, String sqltag) throws SQLException {
        // Use RDBMS SELECT FOR UPDATE to lock table for recovery
        String queryString = "SELECT SERVER_NAME, RUSECTION_ID" +
                             " FROM " + _recoveryTableName + logIdentifierString + _recoveryTableNameSuffix +
                             (DBProduct.Sqlserver == dbProduct ? " WITH (ROWLOCK, UPDLOCK, HOLDLOCK)" : "") +
                             " WHERE RU_ID=-1" +
                             ((DBProduct.Sqlserver == dbProduct ? "" : " FOR UPDATE") +
                              (_tagSQL ? sqltag : ""));
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Attempt to select the HA LOCKING ROW for UPDATE - " + queryString);
        return lockingStmt.executeQuery(queryString);
        // NOTE don't close the Statement - that will close the ResultSet (so we need to pass it in and close it in the caller)
    }

    // helper method
    private int setHADBLockLatch(Statement lockingStmt, boolean setLatch) throws SQLException {
        long latch = setLatch ? _reservedConnectionActiveSectionIDSet : _reservedConnectionActiveSectionIDUnset;

        // Use RDBMS SELECT FOR UPDATE to lock table for recovery
        String updateString = "UPDATE " +
                              _recoveryTableName + _logIdentifierString + _recoveryTableNameSuffix +
                              " SET RUSECTION_ID = " + latch +
                              " WHERE RU_ID = -1";
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Attempt to UPDATE the HA LOCKING ROW - " + updateString);
        return lockingStmt.executeUpdate(updateString);
        // NOTE don't close the Statement - caller owns it
    }

    /**
     * acquires a reserved connection for later use by the shutdown thread and
     * sets a latch (sectionID parameter) to indicate how far the local shutdown has progressed
     * because...
     * There is sometimes a lag in peer recovery where an old
     * server is closing down while a new server is opening the same
     * log for peer recovery.
     *
     * @return SQLException where the exception has been determined to be transient.
     */
    private SQLException reserveConnection() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "reserveConnection", new java.lang.Object[] { _reservedConn, this });

        boolean success = false;
        Statement lockingStmt = null;
        ResultSet lockingRS = null;
        SQLException transientException = null;

        try {
            _reservedConn = getConnection();
            prepareConnectionForBatch(_reservedConn);
            lockingStmt = _reservedConn.createStatement();
            lockingRS = readHADBLock(lockingStmt, _logIdentifierString);

            if (lockingRS.next()) {
                // We found the HA Lock row
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Acquired lock on HA Lock row");
                String storedServerName = lockingRS.getString(1);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Stored server value is: " + storedServerName);
                if (_currentProcessServerName.equalsIgnoreCase(storedServerName)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "This server OWNS the HA lock row");
                    // If the new locking scheme is in play, we bypass this code fragment
                    if (_isHomeServer && !_useNewLockingScheme) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "it's the local server's failure scope - set the latch");
                        int ret = setHADBLockLatch(lockingStmt, true);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Have updated HA Lock row's latch return value: " + ret);
                    }
                    success = true;
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Too late, another server has already got the lock: ");
                }
            } else {
                // there really should be a locking row
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "There's no locking row!!!");
            }
        } catch (SQLException sqlex) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "SQLException creating reservedConnection or reading the lock", sqlex);
            // let transient errors be handled by the caller (no FFDC here)
            if (_sqlTransientErrorHandlingEnabled && SQLRetry.isSQLErrorTransient(sqlex))
                transientException = sqlex;
            else if (isTableDeleted(sqlex)) {
                // The underlying table has been deleted, set exception variables to NOT retry
                transientException = null;
                _isTableDeleted = true;
                if (_associatedLog != null)
                    _associatedLog.markTablesDeletedByAssociation();
                Tr.audit(tc, "WTRN0107W: " +
                             "Underlying SQL tables missing when stopping SQL RecoveryLog " + _logName + " for server " + _serverName);
            } else {
                FFDCFilter.processException(sqlex, "com.ibm.ws.recoverylog.custom.jdbc.impl.SQLMultiScopeRecoveryLog.reserveConnection", "3901", this);
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception selecting/updating the HADBLock row ", e);
            FFDCFilter.processException(e, "com.ibm.ws.recoverylog.custom.jdbc.impl.SQLMultiScopeRecoveryLog.reserveConnection", "3906", this);
        } finally {
            // cleanup JDBC
            if (lockingRS != null) {
                try {
                    lockingRS.close();
                } catch (Exception e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Exception closing ResultsSet", e);
                }
            }
            if (lockingStmt != null) {
                try {
                    lockingStmt.close();
                } catch (Exception e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Exception closing Statement", e);
                }
            }

            // end the tran based on success
            try {
                if (success)
                    _reservedConn.commit();
                else {
                    if (_reservedConn != null)
                        _reservedConn.rollback();
                }
            } catch (SQLException sqlex) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "SQLException ending transaction on connection (commit is " + success + "): ", sqlex);

                // if we're not already throwing a transient then only FFDC if this is not transient - otherwise throw it up to caller for possible retry
                if (success && transientException == null) {
                    if (_sqlTransientErrorHandlingEnabled && SQLRetry.isSQLErrorTransient(sqlex)) {
                        transientException = sqlex;
                    } else {
                        FFDCFilter.processException(sqlex, "com.ibm.ws.recoverylog.custom.jdbc.impl.SQLMultiScopeRecoveryLog.reserveConnection", "3937", this);
                    }
                } // else - no FFDC since this may be a knock-on effect of the original transient condition
                success = false;
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Exception ending transaction on connection (commit is " + success + "): ", e);
                success = false;
            } finally {
                // if we didn't succeed make sure we try to close the connection and forget about it
                if (!success && _reservedConn != null) {
                    try {
                        _reservedConn.close();
                    } catch (Exception e) {
                    } finally {
                        _reservedConn = null;
                    }
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "reserveConnection exiting with connection " + _reservedConn, transientException);
        return transientException;
    }

    /**
     * tries to set the latch back down. If we can't we won't throw an Exception since
     * this isn't necessary for a proper close (though something else has probably already failed).
     * Latch is only set/unset in local failurescope shutdown on local server stop.
     */
    private boolean unlatchHADBLock() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "unlatchHADBLock", new java.lang.Object[] { _reservedConn, this });

        boolean success = false;
        Statement lockingStmt = null;
        ResultSet lockingRS = null;

        if (_isHomeServer && _reservedConn != null) {
            try {
                lockingStmt = _reservedConn.createStatement();
                lockingRS = readHADBLock(lockingStmt, _logIdentifierString);

                if (lockingRS.next()) {
                    // We found the HA Lock row
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Acquired lock on HA Lock row");
                    String storedServerName = lockingRS.getString(1);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Stored server value is: " + storedServerName);
                    if (_currentProcessServerName.equalsIgnoreCase(storedServerName)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "This server OWNS the HA lock row");
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "it's the local server's failure scope - unset the latch");
                        int ret = setHADBLockLatch(lockingStmt, false);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Have updated HA Lock row's latch return value: " + ret);
                        success = true;
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Too late, another server has already got the lock");
                    }
                } else {
                    // there really should be a locking row
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "There's no locking row!!!");
                }
            } catch (SQLException sqlex) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "SQLException selecting/updating the HADBLock row", sqlex);
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Exception selecting/updating the HADBLock row ", e);
            } finally {
                if (lockingRS != null) {
                    try {
                        lockingRS.close();
                    } catch (Exception e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Exception closing ResultsSet", e);
                    }
                }
                if (lockingStmt != null) {
                    try {
                        lockingStmt.close();
                    } catch (Exception e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Exception closing Statement", e);
                    }
                }
                if (success) {
                    try {
                        _reservedConn.commit();
                    } catch (Exception e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Exception committing connection: ", e);
                        success = false;
                    }
                } else {
                    try {
                        _reservedConn.rollback();
                    } catch (Exception e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Exception rolling back connection: ", e);
                    }
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "unlatchHADBLock", Boolean.valueOf(success));
        return success;
    }

    // Checks the table exists and if not tries to create it and insert the locking row.
    // Throws Exception if it cannot achieve this.  If it can it may have switched connections so return the valid connection in this case.
    private void assertDBTableExists(Connection conn, String logIdentifierString) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "assertDBTableExists", new Object[] { conn, logIdentifierString });
        ResultSet touchRS = null;
        Statement touchStmt = null;

        synchronized (_CreateTableLock) // Guard against trying to create a table from multiple threads (this really isn't necessary now we don't support sharing tables)
        {
            try {
                touchStmt = conn.createStatement();
                // This is just a touch test to see if we need to create the table (surely we could use  DatabaseMetaData.getTables)
                touchRS = readHADBLock(touchStmt, logIdentifierString, "--assertDBTableExists");

                if (touchRS != null)
                    try {
                        touchRS.close();
                    } catch (Exception e) {
                    }
                if (touchStmt != null)
                    try {
                        touchStmt.close();
                    } catch (Exception e) {
                    }

                try {
                    conn.rollback();
                } catch (Exception e) {
                } // just a touch test so no need to commit
            } catch (SQLException sqlex) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Query failed with exception: " + sqlex);

                // cleanup after failed attempt
                if (touchRS != null)
                    try {
                        touchRS.close();
                    } catch (Exception e) {
                    }
                if (touchStmt != null)
                    try {
                        touchStmt.close();
                    } catch (Exception e) {
                    }
                if (conn != null)
                    try {
                        conn.rollback();
                    } catch (Exception e) {
                    }

                try {
                    if (conn == null) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Table Creation failed, null connection");
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "assertDBTableExists", sqlex);
                        throw sqlex;
                    }
                    if (_useNewLockingScheme && !_isHomeServer && !ConfigurationProviderManager.getConfigurationProvider().peerRecoveryPrecedence()) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Not the home server and the underlying table may have been deleted");

                        if (isTableDeleted(sqlex)) {
                            PeerLostLogOwnershipException ple = new PeerLostLogOwnershipException("Underlying table is missing", null);
                            throw ple;
                        } else
                            throw sqlex;
                    }
                    // Perhaps we couldn't find the table, if so, attempt to create it
                    createDBTable(conn, logIdentifierString);
                } catch (PeerLostLogOwnershipException ple) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "assertDBTableExists", ple);
                    throw ple;
                } catch (Exception createException) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Table Creation failed with exception: " + createException + " throw, the original exception" + sqlex);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "assertDBTableExists", sqlex);
                    throw sqlex;
                } // end catch create failed
            } // end catch read failed

        } // end unnecessary synch block

        if (tc.isEntryEnabled())
            Tr.exit(tc, "assertDBTableExists");
    }

    @Override
    @FFDCIgnore({ LogClosedException.class, SQLException.class, SQLRecoverableException.class })
    public void heartBeat() throws LogClosedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "heartBeat", new Object[] { Integer.valueOf(_closesRequired), this });

        boolean sqlSuccess = false;
        Throwable nonTransientException = null;
        int initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;

        // Bypass the update if the log is not open or the server is stopping or the log is not in a fit state
        if (_closesRequired > 0 && !_serverStopping && !failed()) {
            SQLException currentSqlEx = null;
            Connection conn = null;
            try {
                // Get a Connection
                synchronized (this) {
                    if (_dsName == null) {
                        configureConnectionParameters();
                        conn = getFirstConnection();
                    } else {
                        conn = getConnection();
                    }
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Acquired connection for heartbeat - " + conn);
                }

                if (!_serverStopping) {
                    synchronized (this) {
                        // Set autocommit FALSE and RR isolation on the connection
                        initialIsolation = prepareConnectionForBatch(conn);
                        internalHeartBeat(conn);

                        // commit the work
                        conn.commit();
                        sqlSuccess = true;
                    }
                }
            } catch (SQLException sqlex) {
                if (_serverStopping || failed()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Peer locking heartbeat failed, the server is stopping or the log is marked failed, got: " + sqlex);
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "The underlying table may have been deleted");
                    if (isTableDeleted(sqlex)) {
                        // The underlying table has been deleted
                        Tr.audit(tc, "WTRN0107W: " +
                                     "Underlying SQL tables missing when heartbeating SQL RecoveryLog " + _logName + " for server " + _serverName);
                        // Set exception variables to NOT retry
                        currentSqlEx = null;
                        nonTransientException = sqlex;
                    } else
                        Tr.audit(tc, "WTRN0107W: " +
                                     "Peer locking heartbeat failed with SQL exception: " + sqlex);
                }
                currentSqlEx = sqlex;
            } catch (Exception ex) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Peer locking heartbeat failed, the server is stopping or the log is marked failed, got: " + ex);

                nonTransientException = ex;
            }

            if (sqlSuccess) {
                // Close the connection and reset autocommit
                try {
                    closeConnectionAfterBatch(conn, initialIsolation);
                } catch (SQLException sqlex) {
                    if (_serverStopping || failed()) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Close failed after heartbeat success, the server is stopping or the log is marked failed, got exception: " + sqlex);
                    } else {
                        Tr.audit(tc, "WTRN0107W: " +
                                     "Caught SQLException when closing connection after heartbeat success, exc: " + sqlex);
                    }
                } catch (Throwable exc) {
                    // Trace the exception
                    if (_serverStopping || failed()) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Close failed after heartbeat success, the server is stopping or the log is marked failed, got exception: " + exc);
                    } else {
                        Tr.audit(tc, "WTRN0107W: " +
                                     "Caught general exception when closing connection after heartbeat success, exc: " + exc);
                    }
                }
            } else { // !sqlSuccess
                if (!_serverStopping) {
                    // Tidy up current connection before dropping into retry code
                    // Attempt a rollback. If it fails, trace the failure but allow processing to continue
                    try {
                        if (conn != null)
                            conn.rollback();
                    } catch (SQLException sqlex) {
                        if (_serverStopping || failed()) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Rollback Failed, after heartbeat failure, the server is stopping or the log is marked failed, got exception: " + sqlex);
                        } else {
                            Tr.audit(tc, "WTRN0107W: " +
                                         "Rollback Failed, after heartbeat failure, SQLException: " + sqlex);
                        }
                    } catch (Throwable exc) {
                        // Trace the exception
                        if (_serverStopping || failed()) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Rollback Failed, after heartbeat failure, the server is stopping or the log is marked failed, got exception: " + exc);
                        } else {
                            Tr.audit(tc, "WTRN0107W: " +
                                         "Rollback Failed, after heartbeat failure, exc: " + exc);
                        }
                    }

                    // Attempt a close. If it fails, trace the failure but allow processing to continue
                    try {
                        if (conn != null)
                            closeConnectionAfterBatch(conn, initialIsolation);
                    } catch (SQLException sqlex) {
                        if (_serverStopping || failed()) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Close failed after heartbeat failure, the server is stopping or the log is marked failed, got exception: " + sqlex);
                        } else {
                            Tr.audit(tc, "WTRN0107W: " +
                                         "Close Failed, after heartbeat failure, SQLException: " + sqlex);
                        }
                    } catch (Throwable exc) {
                        // Trace the exception
                        if (_serverStopping || failed()) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Close Failed, after heartbeat failure, the server is stopping or the log is marked failed, got exception: " + exc);
                        } else {
                            Tr.audit(tc, "WTRN0107W: " +
                                         "Close Failed, after heartbeat failure, exc: " + exc);
                        }
                    }

                    // Is this an environment in which a retry should be attempted
                    if (_sqlTransientErrorHandlingEnabled) {
                        if (nonTransientException == null) {
                            // In this case we will retry if we are operating in an HA DB environment but not if the server is stopping or the log has failed
                            if (_serverStopping || failed()) {
                                if (tc.isEntryEnabled())
                                    Tr.exit(tc, "heartbeat", "Log is not in a fit state for a heartbeat");
                                throw new LogClosedException();
                            }

                            final HeartbeatRetry heartbeatRetry = new HeartbeatRetry();
                            sqlSuccess = heartbeatRetry.retryAndReport(this, _serverName, currentSqlEx);
                            if (!sqlSuccess)
                                nonTransientException = heartbeatRetry.getNonTransientException();
                        } else {
                            // Exception not able to be retried
                            Tr.debug(tc, "Cannot recover from Exception when heartbeating for server " + _serverName + " Exception: "
                                         + nonTransientException);
                        }
                    } else {
                        // Not an environment in which we can retry
                        if (nonTransientException == null) // Up to this point the exception may have appeared to have been transient
                            nonTransientException = currentSqlEx;
                        Tr.debug(tc, "Encountered Exception when heartbeating for server " + _serverName + " Exception: "
                                     + nonTransientException);
                    }
                }
            }
        } else {
            if (_serverStopping || failed()) {
                // Alert the HeartbeatLogManager that the server is stopping or the log is failed
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "heartbeat", "Log is not in a fit state for a heartbeat");
                throw new LogClosedException();
            }
            // Otherwise, the log is closed (and maybe not even opened yet) allow processing to continue
        }

        if (!sqlSuccess && nonTransientException != null) {
            // Audit the failure (if not closed/failed log) but allow processing to continue
            if (!_serverStopping && !failed()) {
                Tr.audit(tc, "WTRN0107W: " +
                             "Caught Exception when heartbeating SQL RecoveryLog " + _logName + " for server " + _serverName +
                             " Exception: " + nonTransientException);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "heartBeat");

    }

    /**
     * The core of the code to claim the local recovery logs for the home server. This code will be retried where an
     * HA RDBMS is supported and a transient error encountered.
     */
    public void internalHeartBeat(Connection conn) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "internalHeartBeat", new Object[] { conn });

        String queryString = "SELECT RUSECTION_ID" +
                             " FROM " + _recoveryTableName + "PARTNER_LOG" + _recoveryTableNameSuffix +
                             (DBProduct.Sqlserver == dbProduct ? " WITH (ROWLOCK, UPDLOCK, HOLDLOCK)" : "") +
                             " WHERE RU_ID=-1" +
                             (DBProduct.Sqlserver == dbProduct ? "" : " FOR UPDATE");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Attempt to select the HA LOCKING ROW for UPDATE using - " + queryString);

        try (Statement lockingStmt = conn.createStatement(); ResultSet lockingRS = lockingStmt.executeQuery(queryString)) {
            if (lockingRS.next()) {
                // We found the HA Lock row
                long storedTimestamp = lockingRS.getLong(1);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Acquired lock on HA Lock row, stored timestamp is: " + Utils.traceTime(storedTimestamp));
                long fir1 = System.currentTimeMillis();
                String updateString = "UPDATE " +
                                      _recoveryTableName + "PARTNER_LOG" + _recoveryTableNameSuffix +
                                      " SET RUSECTION_ID = " + fir1 +
                                      " WHERE RU_ID = -1";
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Update timestamp using using - " + updateString);

                int ret = lockingStmt.executeUpdate(updateString);

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Have updated HA Lock row with return: " + ret);

            } else {
                // We didn't find the HA Lock row in the table
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Could not find HA Lock row, unable to update timestamp");
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "internalHeartBeat");
    }

    @Override
    public boolean claimLocalRecoveryLogs() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "claimLocalRecoveryLogs", this);

        boolean isClaimed = false;
        boolean sqlSuccess = false;
        Throwable nonTransientException = null;
        int initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;
        // Calling either local or peer claim, means that the new locking scheme is in play. Setting the _useNewLockingScheme flag
        // at claim time, is early enough to disable the peer-aware HADB code that predated this functionality
        _useNewLockingScheme = true;

        Connection conn = null;
        SQLException currentSqlEx = null;

        // Check whether the server is stopping
        if (_serverStopping) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Server is stopping will not claim logs");

            if (tc.isEntryEnabled())
                Tr.exit(tc, "claimLocalRecoveryLogs", "server stopping");
            return isClaimed;
        }

        // Handle a cold start by asserting that the table exists for this log (with HA retry)
        try {
            // Get a Connection
            if (_dsName == null) {
                configureConnectionParameters();
                conn = getFirstConnection();
            } else {
                conn = getConnection();
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Acquired connection for attempt to claim local server logs - " + conn);

            // Set autocommit FALSE and RR isolation on the connection
            initialIsolation = prepareConnectionForBatch(conn);
            assertDBTableExists(conn, "PARTNER_LOG");
            assertDBTableExists(conn, "TRAN_LOG");

            // Now make the internal claim
            isClaimed = internalClaimRecoveryLogs(conn, true);

            // commit the work
            conn.commit();

            sqlSuccess = true;
        } catch (SQLException sqlex) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "claimLocalRecoveryLogs failed with SQLException: " + sqlex);
            // Set the exception that will be reported
            currentSqlEx = sqlex;
        } catch (Exception exc) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "claimLocalRecoveryLogs failed with Exception: " + exc);
            nonTransientException = exc;
        }

        if (sqlSuccess) {
            // Close the connection and reset autocommit
            try {
                closeConnectionAfterBatch(conn, initialIsolation);
            } catch (Throwable exc) {
                // Trace the exception
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Close Failed, after claimLocalRecoveryLogs success, got exception: " + exc);
            }
        } else { // !sqlSuccess
            // Ensure that "isClaimed" is false in this case
            isClaimed = false;
            // Tidy up current connection before dropping into retry code
            // Attempt a rollback. If it fails, trace the failure but allow processing to continue
            try {
                if (conn != null)
                    conn.rollback();
            } catch (Throwable exc) {
                // Trace the exception
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Rollback Failed, after claimLocalRecoveryLogs failure, got exception: " + exc);
            }

            // Attempt a close. If it fails, trace the failure but allow processing to continue
            try {
                if (conn != null)
                    closeConnectionAfterBatch(conn, initialIsolation);
            } catch (Throwable exc) {
                // Trace the exception
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Close Failed, after claimLocalRecoveryLogs failure, got exception: " + exc);
            }

            // Is this an environment in which a retry should be attempted
            if (_sqlTransientErrorHandlingEnabled) {
                if (nonTransientException == null) {
                    // In this case we will retry if we are operating in an HA DB environment, unless the server is stopping
                    if (_serverStopping) {
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "claimLocalRecoveryLogs", "server stopping");
                        return false;
                    }

                    final ClaimLocalRetry claimLocalRetry = new ClaimLocalRetry();
                    sqlSuccess = claimLocalRetry.retryAndReport(this, _serverName, currentSqlEx);
                    // If the retry operation succeeded, retrieve the result of the underlying operation
                    if (sqlSuccess)
                        isClaimed = claimLocalRetry.isClaimed();
                    else
                        nonTransientException = claimLocalRetry.getNonTransientException();
                } else {
                    // Exception not able to be retried
                    Tr.debug(tc, "Cannot recover from Exception when claiming local recovery logs for server " + _serverName + " Exception: "
                                 + nonTransientException);
                }
            } else {
                // Not an environment in which we can retry
                if (nonTransientException == null) // Up to this point the exception may have appeared to have been transient
                    nonTransientException = currentSqlEx;
                Tr.debug(tc, "Encountered Exception when claiming local recovery logs for server " + _serverName + " Exception: "
                             + nonTransientException);
            }
        } // eof !sqlSuccess

        // If the recovery logs have been successfully claimed, spin off a thread to heartbeat,
        // ie to periodically update the timestamp in the control row
        if (isClaimed) {
            HeartbeatLogManager.setTimeout(this, _peerLockTimeBetweenHeartbeats);
        } else {
            markFailed(nonTransientException, true, false);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "claimLocalRecoveryLogs", isClaimed);

        return isClaimed;
    }

    /**
     * The core of the code to claim either the local or peer recovery logs for the home server. This code will be retried where an
     * HA RDBMS is supported and a transient error encountered.
     *
     * @param conn
     * @param isHomeServer
     * @return
     * @throws SQLException
     */
    boolean internalClaimRecoveryLogs(Connection conn, boolean isHomeServer) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "internalClaimRecoveryLogs", new Object[] { conn, isHomeServer });

        boolean isClaimed = false;
        boolean doUpdate = true;
        boolean doPeerClaim = false;
        String updateString = "";
        String reportString = "";

        // Get the current time for comparison with that stored in the database.
        final Instant now = Instant.now();

        // First we process the control record in the partner_log
        String queryString = "SELECT SERVER_NAME, RUSECTION_ID" +
                             " FROM " + _recoveryTableName + "PARTNER_LOG" + _recoveryTableNameSuffix +
                             (DBProduct.Sqlserver == dbProduct ? " WITH (ROWLOCK, UPDLOCK, HOLDLOCK)" : "") +
                             " WHERE RU_ID=-1" +
                             ((DBProduct.Sqlserver == dbProduct ? "" : " FOR UPDATE") +
                              (_tagSQL ? "--internalClaimRecoveryLogs" : ""));
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Attempt to select the HA LOCKING ROW for UPDATE using - " + queryString);

        try (Statement lockingStmt = conn.createStatement(); ResultSet lockingRS = lockingStmt.executeQuery(queryString)) {

            if (lockingRS.next()) {
                // We found the HA Lock row
                String storedServerName = lockingRS.getString(1);
                long storedTimestamp = lockingRS.getLong(2);

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Acquired lock on partner_log HA Lock row, stored server value is: " + storedServerName + ", "
                                 + "timestamp is: " + storedTimestamp + ", stale time is: " + _logGoneStaleTime);

                // There are 4 cases:
                // CASE1 - The Home server is claiming its logs and already owns the logs
                // CASE2 - The Home server is claiming its logs and a peer owns the logs
                // CASE3 - A Peer server is claiming STALE logs
                // CASE4 - A Peer server is attempting to claim logs that are not stale
                //
                // Note the interaction between Lease staleness processing and recovery log staleness processing. If we are
                // attempting peer recovery that will be because a peer's lease has expired. But peer recovery will not
                // be allowed to proceed if the recovery logs themselves are not stale. For example a server might be going
                // slow - it has failed to renew its lease but is still actively working on its recovery logs.
                if (isHomeServer) {
                    // We are claiming the local logs for the home server
                    doUpdate = true; // Update the control record in all cases if this is the home server
                    if (_currentProcessServerName.equalsIgnoreCase(storedServerName)) {
                        // CASE1 - The Home server is claiming its logs and already owns the logs
                        // In this case we will update the timestamp only
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "This server ALREADY OWNS the partner_log HA lock row");

                        updateString = "UPDATE " +
                                       _recoveryTableName + "PARTNER_LOG" + _recoveryTableNameSuffix +
                                       " SET RUSECTION_ID = " + now.toEpochMilli() +
                                       " WHERE RU_ID = -1";
                        reportString = "Claim the partner_log for the local server using - ";
                    } else {
                        // CASE2 - The Home server is claiming its logs and a peer owns the logs
                        // Another Server has ownership, we aggressively claim the logs as we are the home server (no timestamp check)
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Another server owns the partner_log HA lock row, we will aggressively claim it,  "
                                         + _recoveryTableName + "PARTNER_LOG" + _recoveryTableNameSuffix
                                         + ", storedTime: " + Utils.traceTime(storedTimestamp));

                        // Claim the logs by updating the server name and timestamp.
                        updateString = "UPDATE " +
                                       _recoveryTableName + "PARTNER_LOG" + _recoveryTableNameSuffix +
                                       " SET SERVER_NAME = '" + _currentProcessServerName +
                                       "', RUSECTION_ID = " + now.toEpochMilli() +
                                       " WHERE RU_ID = -1";
                        reportString = "Claim the partner_log for the local server from a peer using - ";
                    }

                } else {
                    // In the peer case only claim the recovery records if ownership is stale
                    if (now.isAfter(Instant.ofEpochMilli(storedTimestamp).plusSeconds(_logGoneStaleTime.getSeconds()))) {
                        // CASE3 - A Peer server is claiming STALE logs
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Timestamp is STALE for " + _recoveryTableName + "PARTNER_LOG" + _recoveryTableNameSuffix
                                         + ", storedTime: " + Utils.traceTime(storedTimestamp));
                        doPeerClaim = true;
                        // Ownership is stale, claim the logs by updating the server name and timestamp.
                        updateString = "UPDATE " +
                                       _recoveryTableName + "PARTNER_LOG" + _recoveryTableNameSuffix +
                                       " SET SERVER_NAME = '" + _currentProcessServerName +
                                       "', RUSECTION_ID = " + now.toEpochMilli() +
                                       " WHERE RU_ID = -1";
                        reportString = "Claim peer partner_log from a peer server using - ";
                    } else {
                        // CASE4 - A Peer server is attempting to claim logs that are not stale
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Timestamp is NOT STALE for " + _recoveryTableName + "PARTNER_LOG" + _recoveryTableNameSuffix
                                         + ", storedTime: " + Utils.traceTime(storedTimestamp));
                        // In this case we do not update the control record
                        doUpdate = false;
                    }
                }

                // Update the control record
                if (doUpdate) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, reportString + updateString);
                    int ret = lockingStmt.executeUpdate(updateString);

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Have updated partner_log HA Lock row with return: " + ret);
                    // We have successfully claimed the recovery logs.
                    isClaimed = true;
                }
            } else {
                // We didn't find the partner_log HA Lock row in the table. This is unexpected
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "openLog", "InternalLogException didn't find the partner_log HA Lock row in the table");
                throw new InternalLogException("Could not find partner_log HA Lock row");
            }
        }

        // Now update the ownership of the tran_log. We follow the same pattern as for the partner_log
        if (isClaimed) {
            // reset the isClaimed flag
            isClaimed = false;
            queryString = "SELECT SERVER_NAME" +
                          " FROM " + _recoveryTableName + "TRAN_LOG" + _recoveryTableNameSuffix +
                          (DBProduct.Sqlserver == dbProduct ? " WITH (ROWLOCK, UPDLOCK, HOLDLOCK)" : "") +
                          " WHERE RU_ID=-1" +
                          (DBProduct.Sqlserver == dbProduct ? "" : " FOR UPDATE");
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Attempt to select the HA LOCKING ROW for UPDATE using - " + queryString);

            try (Statement lockingStmt = conn.createStatement(); ResultSet lockingRS = lockingStmt.executeQuery(queryString)) {
                if (lockingRS.next()) {
                    // We found the HA Lock row
                    String storedServerName = lockingRS.getString(1);

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Acquired lock on tran_log HA Lock row, stored server value is: " + storedServerName);

                    if (isHomeServer) {
                        // We are claiming the local logs for the home server
                        if (_currentProcessServerName.equalsIgnoreCase(storedServerName)) {
                            // CASE1 - The Home server is claiming its logs and already owns the logs
                            // In this case trace only
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "This server ALREADY OWNS the tran_log HA lock row");
                            doUpdate = false;
                            isClaimed = true; // we don't need to update the tran_log record but this is a good claim.
                        } else {
                            // CASE2 - The Home server is claiming its logs and a peer owns the logs
                            // Another Server has ownership, we aggressively claim the logs as we are the home server (no timestamp check)
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Another server owns the tran_log HA lock row, we will aggressively claim it,  "
                                             + _recoveryTableName + "TRAN_LOG" + _recoveryTableNameSuffix);

                            // Claim the logs by updating the server name.
                            updateString = "UPDATE " +
                                           _recoveryTableName + "TRAN_LOG" + _recoveryTableNameSuffix +
                                           " SET SERVER_NAME = '" + _currentProcessServerName + "'" +
                                           " WHERE RU_ID = -1";
                            reportString = "Claim the tran_log for the local server from a peer using - ";
                            doUpdate = true;
                        }

                    } else {
                        // In the peer case only claim the recovery records if ownership is stale
                        if (doPeerClaim) {
                            // CASE3 - A Peer server is claiming STALE logs
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Make peer claim for " + _recoveryTableName + "TRAN_LOG" + _recoveryTableNameSuffix);

                            // Ownership is stale, claim the logs by updating the server name .
                            updateString = "UPDATE " +
                                           _recoveryTableName + "TRAN_LOG" + _recoveryTableNameSuffix +
                                           " SET SERVER_NAME = '" + _currentProcessServerName + "'" +
                                           " WHERE RU_ID = -1";
                            reportString = "Claim peer tran_log from a peer server using - ";
                            doUpdate = true;
                        } else {
                            // CASE4 - A Peer server is attempting to claim logs that are not stale
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Do not make peer claim for " + _recoveryTableName + "TRAN_LOG" + _recoveryTableNameSuffix);
                            // In this case we do not update the control record
                            doUpdate = false;
                        }
                    }

                    // Update the control record
                    if (doUpdate) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, reportString + updateString);
                        int ret = lockingStmt.executeUpdate(updateString);

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Have updated HA Lock row in tran_log with return: " + ret);
                        // We have successfully claimed the recovery logs.
                        isClaimed = true;
                    }
                } else {
                    // We didn't find the tran_log HA Lock row in the table. This is unexpected
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "openLog", "InternalLogException didn't find the tran_log HA Lock row in the table");
                    throw new InternalLogException("Could not find tran_log HA Lock row");
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "internalClaimRecoveryLogs", isClaimed);
        return isClaimed;
    }

    @Override
    public boolean claimPeerRecoveryLogs() throws LogsUnderlyingTablesMissingException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "claimPeerRecoveryLogs", this);

        boolean sqlSuccess = false;
        boolean isClaimed = false;
        Throwable nonTransientException = null;
        int initialIsolation = Connection.TRANSACTION_REPEATABLE_READ;

        // Calling either local or peer claim, means that the new locking scheme is in play. Setting the _useNewLockingScheme flag
        // at claim time, is early enough to disable the peer-aware HADB code that predated this functionality
        _useNewLockingScheme = true;

        // Check whether the server is stopping
        if (_serverStopping) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Server is stopping will not claim logs");

            if (tc.isEntryEnabled())
                Tr.exit(tc, "claimPeerRecoveryLogs", "server stopping");
            return isClaimed;
        }

        Connection conn = null;
        SQLException currentSqlEx = null;
        try {
            // Get a Connection
            if (_dsName == null) {
                configureConnectionParameters();
                conn = getFirstConnection();
            } else {
                conn = getConnection();
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Acquired connection for staleness test - " + conn);
            // Set autocommit FALSE and RR isolation on the connection
            initialIsolation = prepareConnectionForBatch(conn);
            isClaimed = internalClaimRecoveryLogs(conn, false);

            // commit the work
            conn.commit();

            sqlSuccess = true;

        } catch (SQLException sqlex) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "claimPeerRecoveryLogs failed with SQLException: " + sqlex);
            // Set the exception that will be reported
            currentSqlEx = sqlex;
            if (!ConfigurationProviderManager.getConfigurationProvider().peerRecoveryPrecedence()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Check whether the underlying table has been deleted");
                if (isTableDeleted(sqlex)) {
                    // The underlying table has been deleted, throw an exception
                    LogsUnderlyingTablesMissingException lutme = new LogsUnderlyingTablesMissingException("Underlying table is missing", null);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "claimPeerRecoveryLogs", lutme);
                    throw lutme;
                }
            }
            if (currentSqlEx != null) {
                Tr.audit(tc, "WTRN0107W: " +
                             "Caught SQLException when opening SQL RecoveryLog " + _logName + " for server " + _serverName + " SQLException: " + currentSqlEx);
            }
        } catch (Exception exc) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "claimPeerRecoveryLogs failed with Exception: " + exc);
            nonTransientException = exc;
        }

        if (sqlSuccess) {
            // Close the connection and reset autocommit
            try {
                closeConnectionAfterBatch(conn, initialIsolation);
            } catch (Throwable exc) {
                // Trace the exception
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Close Failed, after claimPeerRecoveryLogs success, got exception: " + exc);
            }
        } else { // !sqlSuccess
            // Ensure that "isClaimed" is false in this case
            isClaimed = false;

            // Tidy up current connection before dropping into retry code
            // Attempt a rollback. If it fails, trace the failure but allow processing to continue
            try {
                if (conn != null)
                    conn.rollback();
            } catch (Throwable exc) {
                // Trace the exception
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Rollback Failed, after claimPeerRecoveryLogs failure, got exception: " + exc);
            }

            // Attempt a close. If it fails, trace the failure but allow processing to continue
            try {
                if (conn != null)
                    closeConnectionAfterBatch(conn, initialIsolation);
            } catch (Throwable exc) {
                // Trace the exception
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Close Failed, after claimPeerRecoveryLogs failure, got exception: " + exc);
            }

            // Is this an environment in which a retry should be attempted
            if (_sqlTransientErrorHandlingEnabled) {
                if (nonTransientException == null) {
                    // In this case we will retry if we are operating in an HA DB environment, unless the server is stopping
                    if (_serverStopping) {
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "claimPeerRecoveryLogs", "server stopping");
                        return false;
                    }

                    final ClaimPeerRetry claimPeerRetry = new ClaimPeerRetry();
                    sqlSuccess = claimPeerRetry.retryAndReport(this, _serverName, currentSqlEx);
                    // If the retry operation succeeded, retrieve the result of the underlying operation
                    if (sqlSuccess)
                        isClaimed = claimPeerRetry.isClaimed();
                } else {
                    // Exception not able to be retried
                    Tr.debug(tc, "Cannot recover from Exception when claiming peer recovery logs for server " + _serverName + " Exception: "
                                 + nonTransientException);
                }
            } else {
                // Not an environment in which we can retry
                if (nonTransientException == null) // Up to this point the exception may have appeared to have been transient
                    nonTransientException = currentSqlEx;
                Tr.debug(tc, "Encountered Exception when claiming peer recovery logs for server " + _serverName + " Exception: "
                             + nonTransientException);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "claimPeerRecoveryLogs", isClaimed);

        return isClaimed;
    }

    @Override
    public void retainLogsInPeerRecoveryEnv(boolean retainLogs) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "retainLogsInPeerRecoveryEnv", new Object[] { retainLogs, this });
        // a noop in this implementation
        if (tc.isEntryEnabled())
            Tr.exit(tc, "retainLogsInPeerRecoveryEnv", this);
    }

    /**
     * We need to throw an exception if an attempt is made to operate on the log when the server is stopping. The reporting differs, dependent
     * on whether the server is recovering its own or a peer's logs.
     */
    private InternalLogException reportOperationAttemptWhenStopping(String operation) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "reportOperationAttemptWhenStopping", operation);
        InternalLogException ile = new InternalLogException("Cannot " + operation + " the log as the server is stopping", null);
        if (!_isHomeServer) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Not the home server, failurescope is " + _failureScope);
            markFailed(ile, false, false); // second parameter "false" as we do not wish to fire out error messages
        } else {
            Tr.audit(tc, "WTRN0100E: " +
                         "Cannot " + operation + " SQL RecoveryLog " + _logName + " for server " + _serverName + " as the server is stopping");
            markFailed(ile);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "reportOperationAttemptWhenStopping");
        return ile;

    }

    /**
     * This concrete class extends SQLRetry providing the local recovery log claim code to be retried in an HA RDBMS environment.
     */
    private class ClaimLocalRetry extends LogRetry {

        boolean _isClaimed = false;

        @Override
        public void retryCode(Connection conn) throws SQLException, Exception {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "ClaimLocalRetry.retryCode", new Object[] { conn });

            // Check whether the server is stopping
            if (_serverStopping) {
                Tr.debug(tc, "Server is stopping will not claim logs");
            } else {
                prepareConnectionForBatch(conn);
                assertDBTableExists(conn, "PARTNER_LOG");
                assertDBTableExists(conn, "TRAN_LOG");

                _isClaimed = internalClaimRecoveryLogs(conn, true);
            }
            if (tc.isEntryEnabled())
                Tr.exit(tc, "ClaimLocalRetry.retryCode", _isClaimed);
        }

        public boolean isClaimed() {
            return _isClaimed;
        }

        @Override
        public String getOperationDescription() {
            return "claiming local recovery logs";
        }

    }

    /**
     * This concrete class extends SQLRetry providing the peer recovery log claim code to be retried in an HA RDBMS environment.
     */
    private class ClaimPeerRetry extends LightweightLogRetry {

        boolean _isClaimed = false;

        @Override
        public void retryCode(Connection conn) throws SQLException, Exception {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "ClaimPeerRetry.retryCode", new Object[] { conn });

            // Check whether the server is stopping
            if (_serverStopping) {
                Tr.debug(tc, "Server is stopping will not claim logs");
            } else {
                _isClaimed = internalClaimRecoveryLogs(conn, false);
            }
            if (tc.isEntryEnabled())
                Tr.exit(tc, "ClaimPeerRetry.retryCode", _isClaimed);
        }

        @Override
        public String getOperationDescription() {
            return "claiming peer recovery logs";
        }

        public boolean isClaimed() {
            return _isClaimed;
        }

    }

    /**
     * This concrete class extends SQLRetry providing the heartbeat code to be retried in an HA RDBMS environment.
     */
    private class HeartbeatRetry extends LightweightLogRetry {

        @Override
        public void retryCode(Connection conn) throws SQLException, Exception {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "HeartbeatRetry.retryCode", new Object[] { conn });

            // Check whether the server is stopping
            if (_serverStopping) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "HeartbeatRetry.retryCode", "Log is not in a fit state for a heartbeat");
                throw new LogClosedException();
            }

            internalHeartBeat(conn);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "HeartbeatRetry.retryCode");
        }

        @Override
        public String getOperationDescription() {
            return "heartbeating";
        }

    }

    /**
     * This concrete class extends SQLRetry providing the force sections code to be retried in an HA RDBMS environment.
     */
    private class ForceSectionsRetry extends LogRetry {

        @Override
        public void retryCode(Connection conn) throws SQLException, Exception {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "ForceSectionsRetry.retryCode", new Object[] { conn });

            // Check whether the server is stopping
            if (_serverStopping) {
                InternalLogException ile = reportOperationAttemptWhenStopping("forcing");

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "ForceSectionsRetry.retryCode", "InternalLogException");
                throw ile;
            }

            // This will confirm that this server owns this log and will invalidate the log if not.
            boolean lockSuccess = assertLogOwnershipAtRuntime(conn);

            if (lockSuccess) {
                // We can go ahead and write to the Database
                executeBatchStatements(conn);
            }
            if (tc.isEntryEnabled())
                Tr.exit(tc, "ForceSectionsRetry.retryCode");
        }

        @Override
        public String getOperationDescription() {
            return "forcing sections";
        }

    }

    /**
     * This concrete class extends SQLRetry providing the openLog code to be retried in an HA RDBMS environment.
     */
    private class OpenLogRetry extends LogRetry {

        @Override
        public void retryCode(Connection conn) throws SQLException, Exception {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "OpenLogRetry.retryCode", new Object[] { conn });

            // Check whether the server is stopping
            if (_serverStopping) {
                InternalLogException ile = reportOperationAttemptWhenStopping("open");

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "OpenLogRetry.retryCode", "InternalLogException");
                throw ile;
            }

            int initialIsolation = prepareConnectionForBatch(conn);

            // Touch the table and create table if necessary
            assertDBTableExists(conn, _logIdentifierString);
            if (_useNewLockingScheme) {
                // Under the new locking scheme the locking row is changed in a "claim" not here (open log processing)
                assertLogOwnershipAtOpenPeerLocking(conn);
            } else {
                // Update the ownership of the recovery log to the running server
                assertLogOwnershipAtOpenWithLatching(conn);
            }

            // Clear out recovery caches in case they were partially filled before the failure.
            _recoverableUnits.clear();
            _recUnitIdTable = new RecoverableUnitIdTable();

            // Drive recover again
            recover(conn);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "OpenLogRetry.retryCode");
        }

        @Override
        public String getOperationDescription() {
            return "opening recovery log";
        }
    }
}
