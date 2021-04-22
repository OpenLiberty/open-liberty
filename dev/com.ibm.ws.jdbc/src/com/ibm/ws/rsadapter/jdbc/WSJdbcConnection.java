/*******************************************************************************
 * Copyright (c) 2001, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method; 
import java.lang.reflect.Proxy;
import java.sql.Array; 
import java.sql.Blob; 
import java.sql.CallableStatement;
import java.sql.ClientInfoStatus; 
import java.sql.Clob; 
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob; 
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException; 
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLRecoverableException; 
import java.sql.SQLWarning;
import java.sql.SQLXML; 
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct; 
import java.sql.Wrapper; 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap; 
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LazyAssociatableConnectionManager;
import javax.resource.spi.LazyEnlistableConnectionManager;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.SharingViolationException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent; 
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.jca.adapter.WSConnectionManager;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.ConnectionSharing; 
import com.ibm.ws.rsadapter.DSConfig;
import com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException;
import com.ibm.ws.rsadapter.impl.CSCacheKey;
import com.ibm.ws.rsadapter.impl.DB2SQLJCSCacheKey;
import com.ibm.ws.rsadapter.impl.DB2SQLJPSCacheKey;
import com.ibm.ws.rsadapter.impl.PSCacheKey;
import com.ibm.ws.rsadapter.impl.StatementCacheKey;
import com.ibm.ws.rsadapter.impl.WSConnectionRequestInfoImpl;
import com.ibm.ws.rsadapter.impl.WSRdbManagedConnectionImpl;
import com.ibm.ws.rsadapter.impl.WSStateManager;

/**
 * This class wraps a JDBC Connection.
 */
public class WSJdbcConnection extends WSJdbcObject implements Connection {
    private static final TraceComponent tc =
                    Tr.register(
                                WSJdbcConnection.class,
                                AdapterUtil.TRACE_GROUP,
                                AdapterUtil.NLS_FILE);

    /**
     * This non-interface is used as a marker in the ifcToDynamicWrapper map, as the most convenient way
     * to fit enableConnectionCasting with multiple interfaces into the existing implementation.
     */
    private static final Class<?> CASTABLE_CONNECTION_MARKER = Proxy.class;

    /**
     * Indicates whether we have warned the customer (when they do setCatalog)
     * about the changes to how connection matching is done for connection sharing.
     */
    private static final AtomicBoolean warnedAboutCatalogMatching = new AtomicBoolean();

    /**
     * Indicates whether we have warned the customer (when they do setHoldability)
     * about the changes to how connection matching is done for connection sharing.
     */
    private static final AtomicBoolean warnedAboutHoldabilityMatching = new AtomicBoolean();

    /**
     * Indicates whether we have warned the customer (when they do setReadOnly)
     * about the changes to how connection matching is done for connection sharing.
     */
    private static final AtomicBoolean warnedAboutReadOnlyMatching = new AtomicBoolean();

    /**
     * Indicates whether we have warned the customer (when they do setTypeMap)
     * about the changes to how connection matching is done for connection sharing.
     */
    private static final AtomicBoolean warnedAboutTypeMapMatching = new AtomicBoolean();
    
    //client info set on this handle to be used when reassocaition
    private String[] currentClientInfo;
    private boolean clientInfoSetExplicitly;

    private Properties clientProps;

    /** The underlying JDBC Connection implementation object. */
    protected Connection connImpl;
    /** SPI ManagedConnection containing a Connection impl object from the JDBC driver. */
    protected WSRdbManagedConnectionImpl managedConn; 
    /** Connection Manager instance - needed for handle reassociation. */
    private WSConnectionManager cm; 
    /** The same Connection Manager instance, with interfaces for lazy enlistment. */
    private LazyEnlistableConnectionManager lazyEnlistableCM; 
    /** The same Connection Manager instance, with interfaces for lazy association. */
    private LazyAssociatableConnectionManager lazyAssociatableCM;
    /** Indicates whether this handle is reserved for reassociation with its current MC. */
    private boolean isReserved; 
    /** Key from the ManagedConnection used to restrict access to certain public methods. */
    protected Object managedConnKey; 
    /** A read-only copy of the ConnectionRequestInfo, for reassociation. */
    private ConnectionRequestInfo connRequestInfo; 
    /** AutoCommit value, now tracked at all times. */
    protected boolean autoCommit; 

    /** ID of the thread which may access this handle, or null if detection is disabled. */
    protected Object threadID; 

    /**
     * Indicates whether any result set of this connection's statements will be closed or not.
     * This value is set to true if any created statement has cursor holdability as
     * CLOSE_CURSORS_AT_COMMIT.
     */
    protected boolean isResultSetClosedAtCommit; 

    /**
     * current isolation level.. it is set by the ManagedConnection. this value is used for each connection
     * operations to set this isolation level on the connection before doing the work. This only impacts
     * the operation that support isolation level switching.. In addition, the setTranscationLevel on the
     * ManagedConection has been optimized so it only sets on the connection when the isolvl is different.
     */
    protected int currentTransactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED; 

    /**
     * flag to keep track of isolation level switching on connection support or not
     **/
    protected boolean supportIsolvlSwitching = false; 

    /**
     * Create a WebSphere JDBC Connection wrapper. To associate a JDBC Connection with a CCI
     * Connection, the initializeForCCI() method must be called.
     * 
     * @param mc the Managed Connection containing the JDBC Connection implementation class.
     * @param conn the JDBC Connection implementation class to be wrapped.
     * @param key the key used to restrict access to certain public methods.
     * @param currentThreadID thread id used for detecting multithreaded access, or else null.
     */
    public WSJdbcConnection(
                            WSRdbManagedConnectionImpl mc,
                            Connection conn,
                            Object key,
                            Object currentThreadID) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(tc, "<init>", mc, AdapterUtil.toString(conn));
        managedConn = mc;
        connImpl = conn;
        managedConnKey = key;
        // Save the thread id if multithreaded access detection is enabled.  A thread id of
        // null indicates it is not enabled. 
        threadID = currentThreadID;
        mcf = managedConn.getManagedConnectionFactory();
        supportIsolvlSwitching = mcf.getHelper().isIsolationLevelSwitchingSupport(); 
        dsConfig = mcf.dsConfig; 
        freeResourcesOnClose = false; // To enable this, read a configured value from dsConfig

        // Initialize AutoCommit from the ManagedConnection.
        // A new connection handle always has default autocommit value
        // In get/use/noCommit/close/get/use case, the second connection should have
        // autocommit false.

        if (managedConn.getTransactionState() == WSStateManager.LOCAL_TRANSACTION_ACTIVE &&
            !managedConn.inGlobalTransaction())
        {
            autoCommit = false;
        } else {
            autoCommit = managedConn.getDefaultAutoCommit();
        }

        // isReserved = false; // default value is already false
        // Anything less than 8 defaults to 8, so we'll just use 8 here.  Usually, we do not
        // expect this list to even grow that big, since statements are removed as they are
        // closed.
        childWrappers = new ArrayList<Wrapper>(8); 
        currentTransactionIsolation = managedConn.getTransactionIsolation(); 

        init(null); // Connection wrapper has no parent. 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "<init>", this);
    }

    /**
     * Initialize the Connection handle. An initialize method must be invoked by the DataSource
     * before the handle is used.
     * Return type changed to WSJdbcConnection. 
     * 
     * @param connectionManager the ConnectionManager associated with the DataSource.
     * 
     * @return this Connection handle, initialized and ready for use.
     * 
     * @throws SQLException if an error occurs initializing the Connection handle.
     */
    protected final Connection initialize(WSConnectionManager connectionManager) 
    throws SQLException {
        cm = connectionManager;
        // JCA 1.5 permits the Application Server to return an inactive handle.  If the handle
        // is inactive (not associated with a ManagedConnection) we need to delay some parts
        // of the initialization until a MC is available.  In this case the only
        // initialization is clearing warnings, which would be done on reactivation anyways.
        if (state != State.INACTIVE)
            connImpl.clearWarnings(); 
        return this;
    }

    /**
     * This method is only used by RRA internally.
     * Initialize the Connection handle. This initialize method must be invoked by the ManagedConnection
     * before this handle is used.
     * Return type changed to WSJdbcConnection.
     * 
     * @param connectionManager the ConnectionManager associated with the DataSource.
     * @param key - the managedConnection key
     * 
     * @return this Connection handle, initialized and ready for use.
     * 
     * @throws SQLException if an error occurs initializing the Connection handle.
     * @ibm-private-in-use
     */
    public Connection initialize(WSConnectionManager connectionManager, Object key)
                    throws ResourceException, SQLException {
        // Verify the caller is allowed to call this method.
        // If the key is nulled out, the Connection must be closed.
        if (managedConnKey == null)
            throw new DataStoreAdapterException("OBJECT_CLOSED", null, WSJdbcConnection.class, "Connection");
        // If the keys do not match then do not continue.  Access to this method is not
        // allowed for non-WebSphere-internal code.
        if (key != managedConnKey)
            throw new DataStoreAdapterException("NOT_A_JDBC_METHOD", null, WSJdbcConnection.class);
        return initialize(connectionManager);

    }

    /**
     * Utility method for the main beginTransactionIfNecessary method. This method implicitly
     * starts a local transaction if AutoCommit is disabled.
     * 
     * @throws SQLException if an error occurs or the current state is not valid.
     * 
     */
    private void beginLocalTransactionIfNecessary() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work on the mc
        if (managedConn.isMCStale()) {
            if (isTraceOn && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "MC is stale", managedConn);
            throw AdapterUtil.staleX();
        }

        // There was no global transaction to enlist in.  The autoCommit value
        // should be used to determine if we need a Local Transaction.
        // If autoCommit is on, we don't want a transaction, just return.
        if (autoCommit)
            return; 

        // Otherwise need to implicity begin a local transaction.
        try {
            managedConn.processLocalTransactionStartedEvent(this);
        } catch (ResourceException ex) {
            FFDCFilter.processException(
                                        ex, WSJdbcConnection.class.getName() + ".beginLocalTransactionIfNecessary",
                                        "343", this);
            throw AdapterUtil.toSQLException(ex);
        }
        if (isTraceOn && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "Local transaction started for " + this);
    }

    /**
     * Call this method before any operation which should implicitly begin a transaction.
     * 
     * This method is mostly a no-op for CCI Connections.
     * 
     * Check the autocommit flag on the connection. If autocommit = off, and there is no global
     * transaction, and a local transaction is not already started, then implicitly start a
     * local transaction (by firing a LOCAL_TRANSACTION_STARTED ConnectionEvent)
     * 
     * @throws SQLException if an error occurs or the current state is not valid.
     */
    @Override
    public void beginTransactionIfNecessary() throws SQLException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "beginTransactionIfNecessary", managedConn.getTransactionStateAsString());

        boolean enforceAutoCommit = false; 

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work on the mc
        if (managedConn.isMCStale()) {
            SQLException x = AdapterUtil.staleX();
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "beginTransactionIfNecessary", "MC is stale: " + managedConn);
            throw x; 

        }

        // Check for multithreaded access on every operation that calls this method. 
        if (threadID != null) 
            detectMultithreadedAccess();

        //  - enforce isolation level on connection first if isolation level switching is
        // supported. This avoids the problem that user tries to use handle 1 to get a statement, then
        // get a different handle with different isolation level, then use the handle 1 again. It will
        // have the wrong isolation level is we do set it each time..
        if (supportIsolvlSwitching) 
        {
            managedConn.setTransactionIsolation(currentTransactionIsolation); 
        }

        // if Enlistment is disabled, then do nothing and simply return
        if (!managedConn.isTransactional()) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "beginTransactionIfNecessary", "no-op enlistment is disabled");
              managedConn.enforceAutoCommit(autoCommit);  // PI90945
            return;
        }
        switch (managedConn.getTransactionState()) {
            case WSStateManager.GLOBAL_TRANSACTION_ACTIVE:
                break;

            case WSStateManager.RRS_GLOBAL_TRANSACTION_ACTIVE: 
                managedConn.setRrsGlobalTransactionReallyActive(true); 
                break; 
            case WSStateManager.LOCAL_TRANSACTION_ACTIVE:
                // Already in a Local or Global Transaction, there is no need to implicitly
                // begin or send a dirty signal for enlisting.  Just return.
                break;
            case WSStateManager.NO_TRANSACTION_ACTIVE:
                // We are not aware of being in any transaction.  If deferred enlistment is
                // enabled, we may still need to enlist in a Global Transaction.  Otherwise,
                // if autoCommit is off, we need to implicitly begin a Local Transaction.
                // The Transaction  Manager can tell us for sure if we're in a Global
                // Transaction or not.

                // Use the LocationRestrictedFunction interface to determine whether there's a
                // global transaction that we can enlist in. On the client, we cannot enlist
                // in global transactions. 

                if (managedConn.isGlobalTransactionActive()) 
                    try {
                        //  - We're in a Global Transaction, but not enlisted yet.
                        // Signal the Application Server for lazy enlistment.
                        //  - Send the event whether autoCommit is on or off.  If
                        // autoCommit is on, it will be turned off implicitly.
                        // We may assume the ConnectionManager supports lazy enlistment here
                        // because we are attempting to do work in a global transaction and are
                        // not enlisted yet. 
                        managedConn.lazyEnlistInGlobalTran(
                                        lazyEnlistableCM == null ? lazyEnlistableCM =
                                                        (LazyEnlistableConnectionManager) cm : lazyEnlistableCM);
                    } catch (ClassCastException castX) 
                    {
                        // No FFDC code needed. Default connection manager does not support
                        // lazy enlistment. Do not enlist in the global transaction.

                        // - not in a global transaction or local transaction,
                        // we need to enforce autocommit
                        enforceAutoCommit = true;

                        beginLocalTransactionIfNecessary(); 
                    } catch (ResourceException resX) {
                        FFDCFilter.processException(
                                                    resX,
                                                    WSJdbcConnection.class.getName()
                                                                    + ".beginTransactionIfNecessary",
                                                    "324",
                                                    this);

                        // An XAException during lazy enlistment indicates a fatal connection
                        // error. 

                        if (resX.getCause() instanceof SystemException) 
                        {
                            SystemException sysX = (SystemException) resX.getCause(); 

                            if (sysX.getCause() != null && sysX.getCause() instanceof XAException) { 

                                XAException xaX = (XAException) sysX.getCause(); 

                                if (xaX.errorCode == XAException.XAER_RMFAIL) {
                                    SQLException badConnX = new SQLRecoverableException(resX.getMessage());
                                    badConnX.initCause(xaX);
                                    if (isTraceOn && tc.isEntryEnabled())
                                        Tr.exit(this, tc, "beginTransactionIfNecessary", badConnX);
                                    throw badConnX;
                                }
                            }
                        }
                        /*
                         * If we made it this far we want to convert resX to a SQLException if possible and throw that.
                         */
                        SQLException x = AdapterUtil.toSQLException(resX);
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(this, tc, "beginTransactionIfNecessary", x);
                        throw x;
                    }
                else // Not in any transaction.
                {
                    // - not in a global transaction or local transaction,
                    // we need to enforce autocommit
                    enforceAutoCommit = true;

                    beginLocalTransactionIfNecessary(); 
                }
                break;
            default:
                // In a transaction state that is not valid. Throw an exception.
                SQLException x = new SQLException(AdapterUtil.getNLSMessage("INVALID_TRAN_STATE", managedConn.getTransactionStateAsString()));
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "beginTransactionIfNecessary", x);
                throw x;
        }
        // AutoCommit is enforced separately from the Connection properties, since any
        // transaction initialization done above may change the value.
        if (enforceAutoCommit) {
            managedConn.enforceAutoCommit(autoCommit);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "beginTransactionIfNecessary");
    }

    public final void clearWarnings() throws SQLException {
        activate(); 
        try {
            connImpl.clearWarnings();
        } catch (SQLException ex) {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.clearWarnings",
                                        "395",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Perform any wrapper-specific close logic. This method is called by the default
     * WSJdbcObject close method.</p>
     * 
     * <p>The Connection close method is responsible for requesting the ManagedConnection to
     * send a CONNECTION CLOSED ConnectionEvent to all listeners.</p>
     * 
     * @param closeWrapperOnly boolean flag to indicate that only wrapper-closure activities
     *            should be performed, but close of the underlying object is unnecessary.
     * 
     * @return SQLException the first error to occur while closing the object.
     */
    protected SQLException closeWrapper(boolean closeWrapperOnly) 
    {
        // Looking for the public close method?  Try WSJdbcObject.
        SQLException sqlX = null;

        if (mcf.getHelper().isCustomHelper && managedConn != null && managedConn.getHandleCount() == 1
         && null == (sqlX = mcf.getHelper().doConnectionCleanupPerCloseConnection(connImpl)))
            managedConn.perCloseCleanupNeeded = false;

        // Send a Connection Closed Event to notify the Managed Connection of the close -- if
        // we are associated with a ManagedConnection to notify.

        if (managedConn != null)
            try {
                managedConn.processConnectionClosedEvent(this);
            } catch (ResourceException resX) {
                FFDCFilter.processException(
                                            resX,
                                            "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.close",
                                            "495",
                                            this);
                sqlX = AdapterUtil.toSQLException(resX);
            }
        else
            ((LazyAssociatableConnectionManager) cm).inactiveConnectionClosed(this, mcf);

        // Destroy the connection wrapper, except for the state, which is needed to detect
        // attempted use of a CLOSED wrapper. 
        connImpl = null;
        childWrappers = null;
        managedConnKey = null;
        connRequestInfo = null;
        managedConn = null; 
        // When JDBC event listeners are enabled, the connection error notification is sent
        // prior to raising the error, which means close is invoked prior to mapException.
        // The reference to the managed connection factory must be kept so that exception mapping can
        // still be performed. 
        cm = null;
        threadID = null;
        clientProps = null;
        return sqlX;
    }

    /**
     * Fires a LOCAL_TRANSACTION_COMMITTED ConnectionEvent, which commits and ends the local
     * transaction.
     * 
     * @throws SQLException if an error occurs during the commit.
     */
    public void commit() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "commit", this);

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work on the mc
        if (managedConn != null && managedConn.isMCStale()) 
        {
            if (isTraceOn && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "MC is stale", managedConn);
            throw AdapterUtil.staleX();
        }
        else if (state == State.CLOSED) {
            SQLException closedX = createClosedException("Connection");
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "commit", closedX);
            throw closedX;
        }

        // Check for multithreaded access of the connection handle. 
        if (threadID != null) 
            detectMultithreadedAccess();
        if (state == State.INACTIVE) {
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "commit", "no-op; state is INACTIVE");
            return;
        }

        try {
            // if the conn has enlistment turned off then just commit
            if (!managedConn.isTransactional()) {
                connImpl.commit();
                // close the ResultSet based on cursor holdability
                // doing here what was done below after the normal commit
                // the same should be done here
                if (isResultSetClosedAtCommit) {
                    // loop through the statement wrappers
                    for (int i = childWrappers.size() - 1; i > -1; i--) {
                        ((WSJdbcStatement) childWrappers.get(i)) 
                        .closeResultSetsIfNecessary();
                    }
                }
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "commit", "Enlistment is disabled");
                return;
            }
            // Commit during a global transaction is an error.
            if (managedConn.inGlobalTransaction()) {
                SQLException sqlX =
                                new SQLException(
                                                AdapterUtil.getNLSMessage(
                                                                          "OP_NOT_VALID_IN_GT",
                                                                          "Connection.commit"));
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "commit", sqlX);
                throw sqlX;
            }
            // If it's an application level local tran, commit it.
            if (managedConn.getTransactionState() == WSStateManager.LOCAL_TRANSACTION_ACTIVE) {
                managedConn.processLocalTransactionCommittedEvent(this);
                //  - close the ResultSet based on cursor holdability
                if (isResultSetClosedAtCommit) {
                    // loop through the statement wrappers
                    for (int i = childWrappers.size() - 1; i > -1; i--) {
                        ((WSJdbcStatement) childWrappers.get(i)) 
                        .closeResultSetsIfNecessary();
                    }
                }
            }
            // No transaction is active.  Leave it up to the underlying JDBC driver to
            // enforce the JDBC spec. 
            else
                connImpl.commit();
        } catch (ResourceException ex) {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.commit",
                                        "776",
                                        this);
            SQLException sqlX = AdapterUtil.toSQLException(ex);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", "Exception");
            throw sqlX;
        } catch (SQLException sqlX)
        {
            FFDCFilter.processException(
                                        sqlX,
                                        getClass().getName() + ".commit",
                                        "587",
                                        this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", "Exception");
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "commit");
    }
    
    /**
     * PostgreSQL only method: 
     * java.sql.Array org.postgresql.PGConnection.createArrayOf(String typeName, Object elements) throws SQLException;
     */
    private Array createArrayOf(Object implObject, Method createArrayOf, Object[] args) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "createArrayOf", args[0]); 
        Array ra;

        try {
            activate(); 
            ra = (Array) createArrayOf.invoke(implObject, args);
            if (freeResourcesOnClose)
                arrays.add(ra); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".createArrayOf", "661", this);
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError | IllegalAccessException | InvocationTargetException methError) {
            // No FFDC code needed; wrong JDBC level or wrong driver
            throw AdapterUtil.notSupportedX("Connection.createArrayOf", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".createArrayOf", "671", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createArrayOf", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".createArrayOf", "677", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createArrayOf", err); 
            throw err;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "createArrayOf", AdapterUtil.toString(ra)); 
        return ra;
    }
    
    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "createArrayOf", typeName); 
        Array ra;

        try {
            activate(); 
            ra = connImpl.createArrayOf(typeName, elements);
            if (freeResourcesOnClose)
                arrays.add(ra); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".createArrayOf", "1014", this);
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("Connection.createArrayOf", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".createArrayOf", "1057", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createArrayOf", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".createArrayOf", "1064", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createArrayOf", err); 
            throw err;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "createArrayOf", AdapterUtil.toString(ra)); 
        return ra;
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public Blob createBlob() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "createBlob"); 
        Blob blob;

        try {
            activate(); 
            blob = connImpl.createBlob();
            if (freeResourcesOnClose)
                blobs.add(blob); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".createBlob", "1040", this);
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("Connection.createBlob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".createBlob", "1108", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createBlob", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".createBlob", "1115", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createBlob", err); 
            throw err;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "createBlob", AdapterUtil.toString(blob)); 
        return blob;
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public Clob createClob() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "createClob"); 
        Clob clob;

        try {
            activate(); 
            clob = connImpl.createClob();
            if (freeResourcesOnClose)
                clobs.add(clob); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".createClob", "1066", this);
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("Connection.createClob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".createClob", "1156", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createClob", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".createClob", "1163", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createClob", err); 
            throw err;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "createClob", AdapterUtil.toString(clob)); 
        return clob;
    }

    /**
     * Creates a wrapper for the supplied Statement object. Use this method only when statement
     * caching is disabled.
     * 
     * @param cstmtImplObject a Statement that needs a wrapper.
     * @param sql the SQL for the callable statement. 
     * 
     * @return the Statement wrapper.
     * 
     * @throws SQLException if there's an error making the wrapper.
     */
    protected CallableStatement createCallableStatementWrapper(
                                                               CallableStatement cstmtImplObject,
                                                               int holdability, 
                                                               String sql) 
    throws SQLException {
        isResultSetClosedAtCommit =
                        isResultSetClosedAtCommit
                                        || holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT;
        return mcf.jdbcRuntime.newCallableStatement(
                        cstmtImplObject, this, holdability, sql); 
    }

    /**
     * Creates a wrapper for the supplied Statement object. Use this method only when statement
     * caching is enabled.
     * 
     * @param cstmtImplObject a Statement that needs a wrapper.
     * @param sql the SQL for the callable statement.
     * @param key the key to use for statement caching.
     * 
     * @return the Statement wrapper.
     * 
     * @throws SQLException if there's an error making the wrapper.
     */
    protected CallableStatement createCallableStatementWrapper(
                                                               CallableStatement cstmtImplObject,
                                                               int holdability,
                                                               String sql, 
                                                               StatementCacheKey key) 
    throws SQLException {
        isResultSetClosedAtCommit =
                        isResultSetClosedAtCommit
                                        || holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT;
        return mcf.jdbcRuntime.newCallableStatement(
                        cstmtImplObject,
                        this,
                        holdability,
                        sql, 
                        key);
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public NClob createNClob() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "createNClob"); 
        NClob clob;

        try {
            activate(); 
            clob = connImpl.createNClob();
            if (freeResourcesOnClose)
                clobs.add(clob); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".createNClob", "1262", this);
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("Connection.createNClob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".createNClob", "1280", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createNClob", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".createNClob", "1287", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createNClob", err); 
            throw err;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "createNClob", AdapterUtil.toString(clob)); 
        return clob;
    }

    //  - Add cursor holdability parameter
    /**
     * Creates a wrapper for the supplied Statement object. Use this method only when statement
     * caching is disabled.
     * 
     * @param pstmtImplObject a Statement that needs a wrapper.
     * @param sql the SQL for the prepared statement. 
     * 
     * @return the Statement wrapper.
     * 
     * @throws SQLException if there's an error making the wrapper.
     */
    protected PreparedStatement createPreparedStatementWrapper(
                                                               PreparedStatement pstmtImplObject,
                                                               int holdability, 
                                                               String sql) 
    throws SQLException {
        isResultSetClosedAtCommit =
                        isResultSetClosedAtCommit
                                        || holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT;
        return mcf.jdbcRuntime.newPreparedStatement(pstmtImplObject, this, holdability, sql); 
    }

    //  - Add cursor holdability parameter
    /**
     * Creates a wrapper for the supplied Statement object. Use this method only when statement
     * caching is enabled.
     * 
     * @param pstmtImplObject a Statement that needs a wrapper.
     * @param sql the SQL for the prepared statement. 
     * @param key the key to use for statement caching.
     * 
     * @return the Statement wrapper.
     * 
     * @throws SQLException if there's an error making the wrapper.
     */
    protected PreparedStatement createPreparedStatementWrapper(
                                                               PreparedStatement pstmtImplObject,
                                                               int holdability,
                                                               String sql, 
                                                               StatementCacheKey key) 
    throws SQLException {
        isResultSetClosedAtCommit =
                        isResultSetClosedAtCommit
                                        || holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT;
        return mcf.jdbcRuntime.newPreparedStatement(
                        pstmtImplObject,
                        this,
                        holdability,
                        sql, 
                        key);
    }

    /**
     * Creates a wrapper for the supplied ResultSet object.
     * 
     * @param rsetImplObject a ResultSet that needs a wrapper.
     * @param rsetParentWrapper the parent wrapper of the ResultSet.
     * 
     * @return the ResultSet wrapper.
     */
    public WSJdbcResultSet createResultSetWrapper(
                                                     ResultSet rsetImplObject,
                                                     WSJdbcObject rsetParentWrapper) 
    {
        return mcf.jdbcRuntime.newResultSet(rsetImplObject, rsetParentWrapper);
    }

    /**
     * Create a SQLException and chained SharingViolationException. The SQLException is needed
     * since the exception will be thrown from JDBC interface methods. Both the SQLException
     * and chained SharingViolationException should contain the error message, indicating the
     * requested operation is not permitted on shareable connections.
     * 
     * @param methodName the name of a method which isn't permitted on shareable connections.
     * 
     * @return the SQLException with a chained SharingViolationException.
     * 
     */
    protected SQLException createSharingException(String methodName) 
    {
        String message =
                        AdapterUtil.getNLSMessage("OP_NOT_SHAREABLE", methodName);
        SQLException sharingX = new SQLException(message);
        sharingX.initCause(new SharingViolationException(message));
        return sharingX;
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public SQLXML createSQLXML() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "createSQLXML", this); 
        SQLXML xml;

        try {
            activate(); 
            xml = connImpl.createSQLXML();
            if (freeResourcesOnClose)
                xmls.add(xml); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".createSQLXML", "1342", this);
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("Connection.createSQLXML", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".createSQLXML", "1358", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createSQLXML", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".createSQLXML", "1365", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createSQLXML", err); 
            throw err;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "createSQLXML", AdapterUtil.toString(xml)); 
        return xml;
    }

    public final Statement createStatement() throws SQLException {
        // Create a statement using the default ResultSet type and concurrency values.
        return createStatement(
                               ResultSet.TYPE_FORWARD_ONLY,
                               ResultSet.CONCUR_READ_ONLY);
    }

    public final Statement createStatement(int type, int concurrency)
                    throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(tc, "createStatement", AdapterUtil.getResultSetTypeString(type), AdapterUtil.getConcurrencyModeString(concurrency));
        Statement stmt = null; 
        try {
            if (state != State.ACTIVE)
                activate(); 
            // Remove synchronization. 
            beginTransactionIfNecessary();
            stmt = connImpl.createStatement(type, concurrency);
            Integer queryTimeout = dsConfig.get().queryTimeout; 
            if (queryTimeout != null) { 
                if(isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                stmt.setQueryTimeout(queryTimeout); 
            }
            // Wrap the Statement.
            stmt =
                            createStatementWrapper(
                                                   stmt,
                                                   managedConn.getCurrentHoldability());
            childWrappers.add(stmt);
        } catch (SQLException ex) {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.createStatement",
                                        "865",
                                        this);
            if (stmt != null)
                try {
                    stmt.close();
                } catch (Throwable x) {
                } 
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "createStatement", "Exception");
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "createStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "createStatement", stmt);
        return stmt;
    }

    //  - Add holdability parameter
    /**
     * Creates a wrapper for the supplied Statement object.
     * 
     * @param stmtImplObject a Statement that needs a wrapper.
     * 
     * @return the Statement wrapper.
     * 
     * @throws SQLException if there's an error making the wrapper.
     */
    protected Statement createStatementWrapper(Statement stmtImplObject, int holdability)
                    throws SQLException 
    {
        isResultSetClosedAtCommit =
                        isResultSetClosedAtCommit
                                        || holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT;
        return mcf.jdbcRuntime.newStatement(stmtImplObject, this, holdability);
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "createStruct", typeName); 
        Struct struct;

        try {
            activate(); 
            struct = connImpl.createStruct(typeName, attributes);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".createStruct", "1341", this);
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("Connection.createStruct", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".createStruct", "1638", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createStruct", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".createStruct", "1645", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createStruct", err); 
            throw err;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "createStruct", AdapterUtil.toString(struct)); 
        return struct;
    }

    /**
     * Detect multithreaded access. This method is called only if detection was enabled at one point,
     * although it might not be enabled anymore.
     * The method ensures that the current thread id matches the saved thread id for this MC.
     * If the MC was just taken out the pool, the thread id may not have been recorded yet.
     * In this case, we save the current thread id. Otherwise, if the thread ids don't match,
     * log a message indicating that multithreaded access was detected. 
     */
    final protected void detectMultithreadedAccess() 
    {
        // If not enabled anymore, then skip this method. 
        if (!dsConfig.get().enableMultithreadedAccessDetection)
            return; 

        Thread currentThreadID = Thread.currentThread();
        if (currentThreadID != threadID) {
            mcf.detectedMultithreadedAccess = true;
            java.io.StringWriter writer = new java.io.StringWriter();
            new Error().printStackTrace(new java.io.PrintWriter(writer));
            Tr.warning(tc, "MULTITHREADED_ACCESS_DETECTED",
                                     Integer.toHexString(threadID.hashCode()) + ' ' + threadID,
                                     Integer.toHexString(currentThreadID.hashCode()) + ' ' + currentThreadID,
                                     writer.getBuffer().delete(0, "java.lang.Error".length()));
        }
    }

    /**
     * <p>This method should be called by the ManagedConnection cleanup to dissociate any
     * remaining ACTIVE Connection handles.</p>
     * 
     * <p>Transitions the Connection handle to the INACTIVE state. Retrieves and stores all
     * information needed for reassociation.</p>
     * 
     * <p>This method is intended only for use by internal WebSphere code, although we have no
     * mechanism in place to prevent applications from invoking it. If they do use it, the
     * only side effects will be dissociating the handle.</p>
     * 
     * @throws ResourceException if the Connection handle is closed or a fatal error occurs on
     *             dissociation.
     */
    public void dissociate() throws ResourceException 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "dissociate", state, isReserved);
        switch (state) {
            case ACTIVE:
                // Only ACTIVE (or INACTIVE reserved) handles may be dissociated.
                break;
            case INACTIVE:
                // A reserved handle may still be dissociated.  This makes the handle available
                // for reassociation with any ManagedConnection.
                if (isReserved) {
                    isReserved = false;
                    break;
                } else {
                    // Dissociating an unreserved INACTIVE handle is just a no-op.
                    if (isTraceOn && tc.isEntryEnabled()) 
                        Tr.exit(this, tc, "dissociate", "Already dissociated.");
                    return;
                }
            case CLOSED:
                // Dissociating a CLOSED handle is an error.
                ResourceException x = new DataStoreAdapterException("OBJECT_CLOSED", null, WSJdbcConnection.class, "Connection");
                if (isTraceOn && tc.isEntryEnabled()) 
                    Tr.exit(this, tc, "dissociate", "OBJECT_CLOSED");
                throw x;
        }

        // When dynamic proxies (produced by Wrapper.unwrap) are reassociated to a new
        // managed connection, the dynamic-wrapper-to-impl map must be updated with the
        // the vendor implementations from new managed connection.
        // The dynamic-wrapper-to-impl map is cleared during dissociation, and later
        // repopulated upon reassociation. All throughout, the interface-to-dynamic-wrapper
        // map remains valid, and is not modified.

        if (dynamicWrapperToImpl != null)
            dynamicWrapperToImpl.clear();


        // Child wrappers, such as Statement or ResultSet, cannot be reassociated to
        // a new ManagedConnection since there is no way to reestablish these objects
        // on the new underlying Connection.  Since child wrappers cannot be used
        // after reassociation, the only option is to close them.
        closeChildWrappers();
        // Track the Subject and ConnectionRequestInfo, which will be needed to
        // reassociate to a new ManagedConnection. 
        // Handles are now responsible for tracking their own autoCommit setting for
        // reassocation. 
        connRequestInfo = managedConn.createConnectionRequestInfo();

        // save the clientInfo is explicitly or implicitly set set too
        if (managedConn.clientInfoExplicitlySet || managedConn.clientInfoImplicitlySet) {
            if (mcf.jdbcDriverSpecVersion >= 40)
                try {
                    clientProps = connImpl.getClientInfo();
                } catch (SQLException sqlX) {
                    FFDCFilter.processException(
                                                sqlX, getClass().getName() + ".dissociate", "1702", this);
                    ResourceException x = new DataStoreAdapterException("DSA_ERROR", proccessSQLException(sqlX), WSJdbcConnection.class);
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(this, tc, "dissociate", sqlX); 
                    throw x;
                }
            else
                currentClientInfo = new String[4]; // this is used to clear client info attributes to null
            clientInfoSetExplicitly = managedConn.clientInfoExplicitlySet;
            if (isTraceOn && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "clientProps saved from mc is:", (Object[]) currentClientInfo);

        }

        // Notify the ManagedConnection of the dissociation so it can remove this
        // handle from its list.
        managedConn.dissociateHandle(this);
        // Null out references to the current ManagedConnection. 
        connImpl = null;
        managedConn = null;
        // Mark the state as INACTIVE
        state = State.INACTIVE;
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "dissociate", "state --> INACTIVE");
    }

    /**
     * Send a CONNECTION_ERROR_OCCURRED ConnectionEvent to all listeners of the Managed
     * Connection.
     * 
     * @param Exception that's causing us to send the event, or null if none.
     * @param logEvent log error event to system out. Useful for preventing expected connection errors from getting logged to system out.
     */
    public void fireConnectionErrorEvent(Exception ex, boolean logEvent) 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        switch (state) {
            case ACTIVE:
                try {
                    // Let the Managed Connection handle any duplicate events.
                    managedConn.processConnectionErrorOccurredEvent(this, ex, logEvent); 
                } catch (NullPointerException nullX) {
                    // No FFDC code needed; we might be closed.
                    if (isTraceOn && tc.isDebugEnabled()) 
                        Tr.debug(this, tc, "Handle CLOSED or INACTIVE. Not sending CONNECTION_ERROR_OCCURRED.");
                }
                break;
            case INACTIVE:
                try {
                    //  - ConnectionError events cannot be sent for INACTIVE handles.
                    if (isTraceOn && tc.isDebugEnabled()) 
                        Tr.debug(this, tc, "Handle is INACTIVE. Not sending CONNECTION_ERROR_OCCURRED.");
                    close();
                } catch (SQLException closeX) {
                    FFDCFilter.processException(
                                                closeX,
                                                "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.FireConnectionErrorEvent",
                                                "965",
                                                this);
                    if (isTraceOn && tc.isDebugEnabled()) 
                        Tr.debug(this, tc, "Error closing connection:", closeX);
                }
                break;
            case CLOSED:
                //  - If a close was already completed, do not send an event.
                if (isTraceOn && tc.isDebugEnabled()) 
                    Tr.debug(this, tc, "Connection already closed. Not sending CONNECTION_ERROR_OCCURRED.");
        }
    }

    public final boolean getAutoCommit() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (state != State.ACTIVE)
            activate(); 

        // for RRS Transactional MCF's, there is no such thing as deferred
        // enlistment, i.e. if a Connection is obtained inside an RRS Global
        // Transaction, it is automatically included in that Transaction
        // and any operations that are normally illegal inside a Global Tran
        // (such as setAutoCommit(true)) will be illegal from the start
        //
        // for this reason, it is appropriate to return an autoCommit value
        // of false any time that an RRS Global Transaction is active, even
        // if SQL has not yet been issued on the Connection
        if (mcf.getHelper().getRRSTransactional()) 
        {
        	if (isTraceOn && tc.isDebugEnabled()) 
            {
                Tr.debug(this, tc, "MCF is rrsTransactional - check for Global"); 
            }
            UOWCurrent uow = (UOWCurrent) mcf.connectorSvc.getTransactionManager();
            UOWCoordinator uowCoord = uow == null ? null : uow.getUOWCoord(); 
            if (uowCoord != null && uowCoord.isGlobal()) 
            {
                if (isTraceOn && tc.isDebugEnabled()) 
                    Tr.debug(this, tc, "RRSGlobalTran active - return autoCommit=false"); 
                return false; 
            } 
        }
          // Handles are now responsible for tracking their own autoCommit. 
        return autoCommit;
    }

    /**
     * Create a dynamic proxy for java.sql.Connection plus the interfaces that the vendor connection implementation implements.
     * If java.sql.Connection is the only interface implemented, just returns this connection wrapper.
     * 
     * Per the design for enableConnectionCasting, the list of vendor interfaces is cached and reused across all connections
     * created by the data source. I have pointed out there is some risk to this - the possibility that a JDBC vendor might vary
     * the interface list across getConnection invocations, making it invalid to cache.  If this becomes a problem, it should
     * be possible to remove the caching by reverting the source code changes under 217741.
     * 
     * @return dynamic proxy or current wrapper.
     */
    Connection getCastableWrapper(AtomicReference<Class<?>[]> vendorConnectionInterfaces) {
        Class<?>[] interfaceList = vendorConnectionInterfaces.get();
        if (interfaceList == null) {
            Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
            interfaces.add(Connection.class); // so that we can validly compare size later
            for (Class<?> c = connImpl.getClass(); c != null; c = c.getSuperclass())
                for (Class<?> i : c.getInterfaces())
                    // exclude sub-interfaces of java.sql.Connection
                    if (!Wrapper.class.equals(i) && !"java.lang.AutoCloseable".equals(i.getName()))
                        interfaces.add(i);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "initVendorConnectionInterfaceList", interfaces);

            interfaceList = interfaces.toArray(new Class<?>[interfaces.size()]);
            vendorConnectionInterfaces.set(interfaceList);
        } 

        if (interfaceList.length <= 1) // only implements java.sql.Connection
            return this;

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "Create castable wrapper for", Arrays.toString(interfaceList));

        Connection wrapper = (Connection) Proxy.newProxyInstance(WSJdbcWrapper.priv.getClassLoader(connImpl.getClass()),
                                                                 interfaceList,
                                                                 this);

        // abusing ifcToDynamicWrapper here with a non-interface as a marker, but it's the most convenient way
        ifcToDynamicWrapper.put(CASTABLE_CONNECTION_MARKER, wrapper); 
        dynamicWrapperToImpl.put(wrapper, connImpl);
        return wrapper;
    }

    /**
     * Retrieve the ManagedConnection this Connection handle is currently associated with.
     * 
     * @param key a special key that must be provided to invoke this method.
     * 
     * @return the ManagedConnection, or null if not associated.
     * 
     * @throws ResourceException if an incorrect key is supplied.
     */
    public ManagedConnection getManagedConnection(Object key)
                    throws ResourceException {
        // Verify the caller is allowed to call this method.
        // If the key is nulled out, the Connection must be closed.
        if (managedConnKey == null)
            throw new DataStoreAdapterException("OBJECT_CLOSED", null, WSJdbcConnection.class, "Connection");
        // If the keys do not match then do not continue.  Access to this method is not
        // allowed for non-WebSphere-internal code.
        if (key != managedConnKey)
            throw new DataStoreAdapterException("NOT_A_JDBC_METHOD", null, WSJdbcConnection.class);
        return managedConn;
    }

    public final String getCatalog() throws SQLException {
        activate(); 

        try {
            return managedConn.getCatalog();
        } catch (SQLException ex) 
        {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.getCatalog",
                                        "917",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public Properties getClientInfo() throws SQLException {
        activate(); 

        try {
            return connImpl.getClientInfo();
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getClientInfo", "1574", this);
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("Connection.getClientInfo", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getClientInfo", "1891", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getClientInfo", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getClientInfo", "1898", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getClientInfo", err); 
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public String getClientInfo(String name) throws SQLException {
        activate(); 

        try {
            return connImpl.getClientInfo(name);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getClientInfo", "1574", this);
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("Connection.getClientInfo", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getClientInfo", "1936", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getClientInfo", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getClientInfo", "1943", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getClientInfo", err); 
            throw err;
        }
    }

    /**
     * @return the Connection wrapper for this object, or null if none is available.
     */
    final protected WSJdbcObject getConnectionWrapper() 
    {
        return this;
    }

    /**
     * Display the javax.transaction.Status constant corresponding to the current transaction
     * status from the Transaction Manager.
     * 
     * @return the name of the constant.
     */
    private String getGlobalTranStatusAsString() {
        int status = javax.transaction.Status.STATUS_NO_TRANSACTION; 
        try {
            TransactionManager tm = mcf.connectorSvc.getTransactionManager();
            if (tm != null)
                status = tm.getStatus(); 
        } catch (javax.transaction.SystemException sysex) //S
        {
            if (tc.isEventEnabled())
                Tr.event(tc,
                         "Recieving SystemException from TransactionManager, ignore it and change to STATUS_UNKNOWN");
            status = javax.transaction.Status.STATUS_UNKNOWN; 

        }

        switch (status) {
            case javax.transaction.Status.STATUS_ACTIVE:
                return "STATUS ACTIVE (" + status + ')'; 

            case javax.transaction.Status.STATUS_COMMITTED:
                return "STATUS COMMITTED (" + status + ')'; 

            case javax.transaction.Status.STATUS_COMMITTING:
                return "STATUS COMMITTING (" + status + ')'; 

            case javax.transaction.Status.STATUS_MARKED_ROLLBACK:
                return "STATUS MARKED ROLLBACK (" + status + ')'; 

            case javax.transaction.Status.STATUS_NO_TRANSACTION:
                return "STATUS NO TRANSACTION (" + status + ')'; 

            case javax.transaction.Status.STATUS_PREPARED:
                return "STATUS PREPARED (" + status + ')'; 

            case javax.transaction.Status.STATUS_PREPARING:
                return "STATUS PREPARING (" + status + ')'; 

            case javax.transaction.Status.STATUS_ROLLEDBACK:
                return "STATUS ROLLEDBACK (" + status + ')'; 

            case javax.transaction.Status.STATUS_ROLLING_BACK:
                return "STATUS ROLLING BACK (" + status + ')'; 

            case javax.transaction.Status.STATUS_UNKNOWN:
                return "STATUS UNKNOWN (" + status + ')'; 
        }

        return "UNKNOWN GLOBAL TRANSACTION STATUS (" + status + ')'; 
    }

    /**
     * @return the underlying JDBC implementation object which we are wrapping.
     */
    final protected Wrapper getJDBCImplObject() 
    {
        return connImpl;
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "getMetaData", this);
        try {
            activate(); 
            //  - Oracle XADataSource needs to be in a transaction to do getMetaData.
            beginTransactionIfNecessary();
            childWrapper =
                            childWrapper == null
                                            ? mcf.jdbcRuntime.newDatabaseMetaData(connImpl.getMetaData(), this)
                                            : childWrapper;
        } catch (SQLException ex) {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.getMetaData",
                                        "922",
                                        this);
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "getMetaData", "Exception");
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "getMetaData", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "getMetaData", childWrapper);
        return (WSJdbcDatabaseMetaData) childWrapper;
    }

    /**
     * @return the trace component for the WSJdbcConnection.
     */
    protected TraceComponent getTracer() 
    {
        return tc;
    }

    public final int getTransactionIsolation() throws SQLException {
        if (state != State.ACTIVE)
            activate(); 

        try {
            return managedConn.getTransactionIsolation();
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public final Map<String, Class<?>> getTypeMap() throws SQLException {
        activate(); 

        try {
            return managedConn.getTypeMap();
        } catch (SQLException ex) 
        {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.getTypeMap",
                                        "927",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public final SQLWarning getWarnings() throws SQLException {
        activate(); 
        try {
            return connImpl.getWarnings();
        } catch (SQLException ex) {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.getWarnings",
                                        "991",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @return relevant FFDC information for the JDBC object, formatted as a String array.
     */
    @Override
    public String[] introspectSelf() {
        com.ibm.ws.rsadapter.FFDCLogger info =
                        new com.ibm.ws.rsadapter.FFDCLogger(this); 

        introspectAll(info);
        info.eoln();
        info.introspect("ConnectionRequestInfo:", connRequestInfo);
        info.introspect("ManagedConnectionFactory:", mcf);
        info.introspect("ManagedConnection:", managedConn); 
        return info.toStringArray();
    }

    /**
     * Collects FFDC information specific to this JDBC wrapper. Formats this information to
     * the provided FFDC logger. This method is used by introspectAll to collect any wrapper
     * specific information.
     * 
     * @param info FFDCLogger on which to record the FFDC information.
     */
    @Override
    protected void introspectWrapperSpecificInfo(com.ibm.ws.rsadapter.FFDCLogger info) 
    {
        info.append(
                    "Transaction Manager global transaction status is",
                    getGlobalTranStatusAsString());
        info.append(
                    "Underlying Connection: " + AdapterUtil.toString(connImpl),
                    connImpl);
        info.append("Key Object:", managedConnKey);
        info.introspect("Connection Manager:", cm); 
        info.append("ConnectionManager supports lazy association?", 
                    cm instanceof LazyAssociatableConnectionManager
                                    ? Boolean.TRUE
                                    : Boolean.FALSE);
        info.append("ConnectionManager supports lazy enlistment?", 
                    cm instanceof LazyEnlistableConnectionManager
                                    ? Boolean.TRUE
                                    : Boolean.FALSE);
        info.append("Handle is reserved? " + isReserved);
        info.append("AutoCommit value tracked by handle: " + autoCommit);
        info.append("Thread id:", threadID); 
    }

    /**
     * Invokes a method on the specified object.
     * The data source must override this method to account for dynamic configuration changes.
     * 
     * @param implObject the instance on which the operation is invoked.
     * @param method the method that is invoked.
     * @param args the parameters to the method.
     * 
     * @throws IllegalAccessException if the method is inaccessible.
     * @throws IllegalArgumentException if the instance does not have the method or
     *             if the method arguments are not appropriate.
     * @throws InvocationTargetException if the method raises a checked exception.
     * @throws SQLException if unable to invoke the method for other reasons.
     * 
     * @return the result of invoking the method.
     */
    Object invokeOperation(Object implObject, Method method, Object[] args)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                    SQLException {
        final int numArgs = args == null ? 0 : args.length;
        final String methodName = method.getName();
        if (numArgs == 0 && methodName.equals("getConnectionContext")) {
            return managedConn.getSQLJConnectionContext(method.getReturnType(),cm);
        } else if (numArgs == 2 && args[0] instanceof String && methodName.equals("createArrayOf")) {
            return createArrayOf(implObject, method, args);
        } else if (numArgs > 10) {
            if (methodName.equals("prepareSQLJStatement"))
                return prepareSQLJStatement(implObject, method, args);
            else if (methodName.equals("prepareSQLJCall"))
                return prepareSQLJCall(implObject, method, args);
        }
        return super.invokeOperation(implObject, method, args);
    }

    public final boolean isReadOnly() throws SQLException {
        activate(); 

        try {
            return managedConn.isReadOnly();
        } catch (SQLException ex) 
        {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.isReadOnly",
                                        "996",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @return true if the handle is reserved for reassociation with its current
     *         ManagedConnection, otherwise false.
     */
    public final boolean isReserved() {
        return isReserved;
    }

    /**
     * Determine if the handle is shareable or unshareable. This information is kept on the
     * CMConfigData. Retrieving the CMConfigData each time will be slow, but we're not
     * concerned with that because this method is only provided for WebSphere test cases,
     * not for customer applications.
     * 
     * @return true if this Connection is shareable; false if it is non-shareable.
     * 
     * @throws SQLException if the Connection is closed.
     */
    public boolean isShareable() throws SQLException {
        try {
            return cm.getResourceRefInfo().getSharingScope() == ResourceRef.SHARING_SCOPE_SHAREABLE;
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public boolean isValid(int timeout) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "isValid", timeout);

        // It is okay to invoke isValid if the connection is closed.
        // In this case, it returns FALSE.

        if (state == State.CLOSED) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "isValid", "FALSE: Connection is CLOSED"); 
            return false;
        }

        boolean valid;

        try {
            if (state != State.ACTIVE)
                activate(); 
            valid = connImpl.isValid(timeout);

            // Notify the connection pool of the bad connection. 
            if (!valid)
                fireConnectionErrorEvent(null, true); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".isValid", "1852", this);
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("Connection.isValid", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".isValid", "2244", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "isValid", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".isValid", "2251", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "isValid", err); 
            throw err;
        }

        // Ideally, we would use a FALSE result to notify the connection pool that the
        // connection is bad. However, since FALSE can also indicate that the validation
        // operation didn't complete within the timeout, we have no guarantee that the
        // connection is actually bad. The JDBC driver might notify us via the
        // connection event listener. Otherwise, it is up to the application to notify
        // us via WSCallHelper.setConnectionError.

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "isValid", valid ? Boolean.TRUE : Boolean.FALSE); 
        return valid;
    }

    public final String nativeSQL(String sql) throws SQLException {
        activate(); 
        try {
            return connImpl.nativeSQL(sql);
        } catch (SQLException ex) {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.nativeSQL",
                                        "1079",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public final CallableStatement prepareCall(String sql) throws SQLException {
        // Use the default ResultSet type and concurrency values.
        return prepareCall(
                           sql,
                           ResultSet.TYPE_FORWARD_ONLY,
                           ResultSet.CONCUR_READ_ONLY);
    }

    public final CallableStatement prepareCall(
                                               String sql,
                                               int type,
                                               int concurrency)
                    throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "prepareCall", sql, AdapterUtil.getResultSetTypeString(type), AdapterUtil.getConcurrencyModeString(concurrency));
        CallableStatement cstmt = null; 
        try {
            if (state != State.ACTIVE)
                activate(); 

            if (managedConn.isStatementCachingEnabled()) {
                CSCacheKey key = new CSCacheKey( 
                    sql,
                    type,
                    concurrency,
                    managedConn.getCurrentHoldability(),
                    mcf.doesStatementCacheIsoLevel ? currentTransactionIsolation : 0,
                    managedConn.getCurrentSchema()); 
                beginTransactionIfNecessary();
                // get a prepared statement from the cache
                Object s = managedConn.getStatement(key);
                if (s == null) // not found in cache, create a new one
                {
                    cstmt = connImpl.prepareCall(sql, type, concurrency);
                    Integer queryTimeout = dsConfig.get().queryTimeout; 
                    if (queryTimeout != null) { 
                        if(isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                        cstmt.setQueryTimeout(queryTimeout); 
                    }
                } else
                    // found in cache
                    cstmt = (CallableStatement) s;
                // Wrap the CallableStatement.
                cstmt =
                                createCallableStatementWrapper(
                                                               cstmt,
                                                               managedConn.getCurrentHoldability(),
                                                               sql, 
                                                               key);
            } else // Statement caching is not enabled.
            {
                beginTransactionIfNecessary();
                cstmt = connImpl.prepareCall(sql, type, concurrency);
                Integer queryTimeout = dsConfig.get().queryTimeout; 
                if (queryTimeout != null) { 
                    if(isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                    cstmt.setQueryTimeout(queryTimeout); 
                }
                // Wrap the CallableStatement.
                cstmt =
                                createCallableStatementWrapper(
                                                               cstmt,
                                                               managedConn.getCurrentHoldability(),
                                                               sql); 
            }
            childWrappers.add(cstmt); 
        } catch (SQLException sqlX) {
            // No FFDC code needed. Might be an application error. 
            if (cstmt != null)
                try {
                    cstmt.close();
                } catch (Throwable x) {
                } 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareCall", sqlX); 
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareCall", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "prepareCall", cstmt); 
        return cstmt; 
    }

    /**
     * Invokes prepareSQLJCall and constructs a callable statement wrapper for the result.
     * 
     * @param implObject the instance on which the operation is invoked.
     * @param method the method that is invoked.
     * @param args the parameters to the method.
     * 
     * @throws IllegalAccessException if the method is inaccessible.
     * @throws IllegalArgumentException if the instance does not have the method or
     *             if the method arguments are not appropriate.
     * @throws InvocationTargetException if the method raises a checked exception.
     * @throws SQLException if unable to invoke the method for other reasons.
     * 
     * @return the result of invoking the method.
     */
    private Object prepareSQLJCall(Object implObject, Method method, Object[] args)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                    SQLException {
        String sql = (String) args[0];
        Object section = args[2];
        int type = (Integer) args[7];
        int concurrency = (Integer) args[8];
        int holdability = (Integer) args[9];
        // There are 2 method signatures for prepareSQLJCall, one of which as 2 additional parameters
        boolean hasCacheKeySuffix = args.length > 11;
        String cacheKeySuffix = hasCacheKeySuffix ? (String) args[11] : null;
        Object[] additionalArgs = args.length > 12 ? (Object[]) args[12] : null;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 
        
        if (isTraceOn && tc.isEntryEnabled()) Tr.entry(tc, "prepareSQLJCall", new Object[] 
        {
            this,
            sql,
            "Statement Role: " + args[1],
            section,
            AdapterUtil.toString(args[3]), // parameter metadata
            AdapterUtil.toString(args[4]), // result set metadata
            "Dynamic Execution? " + args[5],
            "Needs Describe? " + args[6],
            AdapterUtil.getResultSetTypeString(type),
            AdapterUtil.getConcurrencyModeString(concurrency),
            "ResultSet Holdability: " + AdapterUtil.getCursorHoldabilityString(holdability),
            "DB2 SQLJ Statement Type: " + args[10],
            "CacheKeySuffix: " + cacheKeySuffix,
            "AdditionalArgs: " + Arrays.toString(additionalArgs)
        });

        Object sqljWrapper;
        WSJdbcCallableStatement cstmtWrapper;

        try
        {
            if (managedConn.isStatementCachingEnabled() && !(hasCacheKeySuffix && cacheKeySuffix == null))
            {
                StatementCacheKey key = new DB2SQLJCSCacheKey(
                    sql, type, concurrency, holdability, section,
                    mcf.doesStatementCacheIsoLevel ? currentTransactionIsolation : 0,
                    managedConn.getCurrentSchema(),
                    cacheKeySuffix); 

                // get a statement from the cache
                Object s = managedConn.getStatement(key);

                CallableStatement cstmt = s == null ?
                        (CallableStatement) method.invoke(implObject, args) :
                        (CallableStatement) s;

                cstmtWrapper = mcf.jdbcRuntime.newCallableStatement(
                               cstmt, this, holdability, sql, key); 
                cstmtWrapper.sqljSection = section;
            }
            else // Statement caching is not enabled or statement has null cache key suffix.
            {
                CallableStatement cstmt = (CallableStatement) method.invoke(implObject, args);


                // Wrap the CallableStatement.
                cstmtWrapper = mcf.jdbcRuntime.newCallableStatement(
                               cstmt, this, holdability, sql); 
            }

            sqljWrapper = cstmtWrapper.unwrap(method.getReturnType());
            childWrappers.add(cstmtWrapper);
        }
        catch (SQLException sqlX)
        {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled()) Tr.exit(tc, "prepareSQLJCall", sqlX); 
            throw WSJdbcUtil.mapException(this, sqlX);
        }
        catch (NullPointerException nullX)
        {
            // No FFDC code needed; we might be closed.
            if (TraceComponent.isAnyTracingEnabled() &&  tc.isEntryEnabled())  Tr.exit(tc, "prepareSQLJCall", "Exception");  
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled()) Tr.exit(tc, "prepareSQLJCall", sqljWrapper); 
        return sqljWrapper;
    }

    /**
     * Invokes prepareSQLJStatement and constructs a prepared statement wrapper for the result.
     * 
     * @param implObject the instance on which the operation is invoked.
     * @param method the method that is invoked.
     * @param args the parameters to the method.
     * 
     * @throws IllegalAccessException if the method is inaccessible.
     * @throws IllegalArgumentException if the instance does not have the method or
     *             if the method arguments are not appropriate.
     * @throws InvocationTargetException if the method raises a checked exception.
     * @throws SQLException if unable to invoke the method for other reasons.
     * 
     * @return the result of invoking the method.
     */
    private Object prepareSQLJStatement(Object implObject, Method method, Object[] args)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                    SQLException {
        String sql = (String) args[0];
        Object section = args[2];
        int type = (Integer) args[7];
        int concurrency = (Integer) args[8];
        int holdability = (Integer) args[9];
        // There are 2 method signatures for prepareSQLJStatement, one of which has 2 additional parameters
        boolean hasCacheKeySuffix = args.length > 11;
        String cacheKeySuffix = hasCacheKeySuffix ? (String) args[11] : null;
        Object[] additionalArgs = args.length > 12 ? (Object[]) args[12] : null;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) Tr.entry(tc, "prepareSQLJStatement", new Object[] 
        {
            this,
            sql,
            "Statement Role: " + args[1],
            AdapterUtil.toString(section),
            AdapterUtil.toString(args[3]), // parameter metadata
            AdapterUtil.toString(args[4]), // result set metadata
            "Dynamic Execution? " + args[5],
            "Needs Describe? " + args[6],
            AdapterUtil.getResultSetTypeString(type),
            AdapterUtil.getConcurrencyModeString(concurrency),
            "ResultSet Holdability: " + AdapterUtil.getCursorHoldabilityString(holdability),
            "DB2 SQLJ Statement Type: " + args[10],
            "CacheKeySuffix: " + cacheKeySuffix,
            "AdditionalArgs: " + Arrays.toString(additionalArgs)
        });

        Object sqljWrapper;
        WSJdbcPreparedStatement pstmtWrapper;

        try
        {
            // Only create a key if statement caching is enabled. 
            if (managedConn.isStatementCachingEnabled() && !(hasCacheKeySuffix && cacheKeySuffix == null))
            {
                StatementCacheKey key = new DB2SQLJPSCacheKey(
                    sql, type, concurrency, holdability, section,
                    mcf.doesStatementCacheIsoLevel ? currentTransactionIsolation : 0,
                    managedConn.getCurrentSchema(),
                    cacheKeySuffix); 

                // get a prepared statement from the cache
                Object s = managedConn.getStatement(key);

                PreparedStatement pstmt = s == null ?
                        (PreparedStatement) method.invoke(implObject, args) :
                        (PreparedStatement) s;

                pstmtWrapper = mcf.jdbcRuntime.newPreparedStatement(
                               pstmt, this, holdability, sql, key); 
                pstmtWrapper.sqljSection = section;
            }
            else // Statement caching is not enabled or statement has null cache key suffix.
            {
                PreparedStatement pstmt = (PreparedStatement) method.invoke(implObject, args);

                pstmtWrapper = mcf.jdbcRuntime.newPreparedStatement(pstmt, this, holdability, sql);
            }

            sqljWrapper = pstmtWrapper.unwrap(method.getReturnType());
            childWrappers.add(pstmtWrapper);
        }
        catch (SQLException ex)
        {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled()) Tr.exit(tc, "prepareSQLJStatement", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        }
        catch (NullPointerException nullX)
        {
            // No FFDC code needed; we might be closed.
            if (TraceComponent.isAnyTracingEnabled() &&  tc.isEntryEnabled()) Tr.exit(tc, "prepareSQLJStatement", "Exception");  
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled()) Tr.exit(tc, "prepareSQLJStatement", sqljWrapper); 
        return sqljWrapper;
    }

    public final PreparedStatement prepareStatement(String sql)
                    throws SQLException {
        // Use the default ResultSet type and concurrency values.
        return prepareStatement(
                                sql,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
    }

    public final PreparedStatement prepareStatement(
                                                    String sql,
                                                    int type,
                                                    int concurrency)
                    throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "prepareStatement", sql, AdapterUtil.getResultSetTypeString(type), AdapterUtil.getConcurrencyModeString(concurrency));
        PreparedStatement pstmt = null; 
        try {
            if (state != State.ACTIVE)
                activate(); 

            // Only create a PSCacheKey if statement caching is enabled. 
            if (managedConn.isStatementCachingEnabled()) {
                PSCacheKey key = new PSCacheKey( 
                                sql,
                                type,
                                concurrency,
                                managedConn.getCurrentHoldability(),
                                0,
                                mcf.doesStatementCacheIsoLevel ? currentTransactionIsolation : 0,
                                managedConn.getCurrentSchema()); 
                beginTransactionIfNecessary();
                // get a prepared statement from the cache
                Object s = managedConn.getStatement(key);
                if (s == null) // not found in cache, so create a new one
                {
                    pstmt = connImpl.prepareStatement(sql, type, concurrency);
                    Integer queryTimeout = dsConfig.get().queryTimeout; 
                    if (queryTimeout != null) { 
                        if(isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                        pstmt.setQueryTimeout(queryTimeout); 
                    }
                } else
                    pstmt = resetStatement((PreparedStatement) s);
                pstmt =
                                createPreparedStatementWrapper(
                                                               pstmt,
                                                               managedConn.getCurrentHoldability(),
                                                               sql, 
                                                               key);
            } else // Statement caching is not enabled.
            {
                beginTransactionIfNecessary();
                pstmt = connImpl.prepareStatement(sql, type, concurrency);
                Integer queryTimeout = dsConfig.get().queryTimeout; 
                if (queryTimeout != null) { 
                    if(isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                    pstmt.setQueryTimeout(queryTimeout); 
                }
                pstmt =
                                createPreparedStatementWrapper(
                                                               pstmt,
                                                               managedConn.getCurrentHoldability(),
                                                               sql); 
            }
            childWrappers.add(pstmt);
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (Throwable x) {
                } 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareStatement", ex); 
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "prepareStatement", pstmt); 
        return pstmt; 
    }

    /**
     * Returns an INACTIVE connection handle to an ACTIVE state by reassociating it with a new
     * ManagedConnection. If this same handle is first reactivated on another thread then this
     * method becomes a no-op.
     * 
     * @throws SQLException if an error occurs reactivating the handle.
     */
    @Override
    protected final void activate() throws SQLException 
    {
        switch (state) {
            case ACTIVE:
                return;
            case CLOSED:
                throw createClosedException("Connection");
                // else INACTIVE
            default:
                break;
        }

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "activate");
        // Remove the checking of the InactiveConnectionSupport property.  This property was
        // removed from the RRA because there are now standard interfaces for it in JCA 1.5.
        // The RRA will always support lazy connection association (the new standard name
        // for the property) so there is no need to check. 
        try {
            // Request the ConnectionManager to associate this handle with a Managed
            // Connection.  Since we were able to successfully reach the dissociated
            // state we may assume the ConnectionManager supports reassociation.
            lazyAssociatableCM =
                            lazyAssociatableCM == null
                                            ? (LazyAssociatableConnectionManager) cm
                                            : lazyAssociatableCM;
            lazyAssociatableCM.associateConnection(this, mcf, connRequestInfo);
        } catch (ResourceException reassociationX) {
            FFDCFilter.processException(
                                        reassociationX,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.activate", 
                                        "1625",
                                        this);
            Tr.warning(tc, "REASSOCIATION_ERR", reassociationX);
            SQLException sqlX = AdapterUtil.toSQLException(reassociationX);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "activate", reassociationX);
            throw sqlX; 
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /**
     * <p>Reassociates this Connection handle with a new ManagedConnection. This includes
     * reassociating the underlying Connection object, but NOT the child objects of the
     * Connection, which must all be closed at this point. It is an error to reassociate a
     * handle which is not in the inactive state.</p>
     * 
     * @param mc the new ManagedConnection to associate this handle with.
     * @param connImplObject the new underlying JDBC Connection object to associate this handle
     *            with.
     * @param key a special key that must be provided to invoke this method.
     * 
     * @throws ResourceException if an incorrect key is supplied, if the handle is not ready
     *             for reassociation, or if an error occurs during the reassociation.
     */
    public void reassociate(ManagedConnection mc, Connection connImplObject, 
                            Object key) throws ResourceException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(tc, "reassociate", mc, AdapterUtil.toString(connImplObject));
        // Verify the caller is allowed to call this method.
        // If the key is nulled out, the Connection must be closed.  Reassociating closed
        // handles is not valid.
        if (managedConnKey == null) {
            ResourceException x = new DataStoreAdapterException("OBJECT_CLOSED", null, WSJdbcConnection.class, "Connection");
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "reassociate", "Exception");
            throw x;
        }
        // If the keys do not match then do not continue.  Access to this method is not
        // allowed for non-WebSphere-internal code.
        if (key != managedConnKey) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "reassociate", "Exception"); 
            throw new DataStoreAdapterException("NOT_A_JDBC_METHOD", null, WSJdbcConnection.class);
        }
        if (isReserved) {
            // Handle is reserved for reassociation with its current ManagedConnection,
            // so we better have the same ManagedConnection.
            if (mc != managedConn) {
                String message = "Connection handle is reserved for reassociation with a specific ManagedConnection, which does not match the ManagedConnection provided.";
                ResourceException x = new DataStoreAdapterException("WS_INTERNAL_ERROR", null, WSJdbcConnection.class, message, " See trace for more details.");
                if (isTraceOn) 
                {
                    if (tc.isDebugEnabled())
                        Tr.debug(this, tc, message, managedConn, mc);
                    if (tc.isEntryEnabled())
                        Tr.exit(this, tc, "reassociate", "WS_INTERNAL_ERROR");
                } 
                throw x;
            }
            if (isTraceOn && tc.isDebugEnabled()) 
                Tr.debug(tc, "Handle is reserved, reassociating back to original ManagedConnection.");
            // Since the dissociation was never honored, there's not much to do for the
            // reassociation.  Just set the state to ACTIVE and unreserve the handle.
            isReserved = false;
            state = State.ACTIVE;
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "reassociate", "state --> ACTIVE");
            return;
        }
        // ELSE handle is not reserved; continue with reassociation as usual.
        // A Connection handle may only be reassociated from an INACTIVE state.
        if (state != State.INACTIVE) {
            ResourceException x = new DataStoreAdapterException("CANNOT_REASSOCIATE", null, WSJdbcConnection.class, state.name());
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "reassociate", "Exception");
            throw x;
        }
        // Verify that no child objects of the Connection are left open.  It is an error
        // to reassociate a Connection with child objects open.
        if (childWrapper != null
            || (childWrappers != null && childWrappers.size() > 0)) {
            ResourceException x = new DataStoreAdapterException("CHILDREN_STILL_OPEN", null, WSJdbcConnection.class);
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "reassociate", "Exception");
            throw x;
        }
        // Switch to the new ManagedConnection. The ConnectionManager will remain the same.
        managedConn = (WSRdbManagedConnectionImpl) mc;
        connImpl = connImplObject;

        // Dynamic proxies (created by Wrapper.unwrap) are associated with underlying
        // implementation instances. The dynamic-wrapper-to-implementation map was cleared
        // on dissociation. It must be re-established during reassociation based on the
        // entries in the interface-to-dynamic-wrapper map.
        for (Map.Entry<Class<?>, Object> entry : ifcToDynamicWrapper.entrySet())
            try 
            {
                Class<?> ifc = entry.getKey(); 
                Object impl = CASTABLE_CONNECTION_MARKER.equals(ifc) ? connImpl : getJDBCImplObject(ifc);
                dynamicWrapperToImpl.put(entry.getValue(), impl);
            } catch (SQLException implX) {
                FFDCFilter.processException(
                                            implX, getClass().getName() + ".reassociate", "2763", this);

                // We associated to an underlying connection that for some reason doesn't
                // implement the same proprietary interface.  We consider this situation a
                // warning, not a fatal error, because no problems will occur unless the
                // application attempts to use the particular dynamic proxy again.
                // The map entry is left empty, meaning we consider the dynamic proxy closed.
                Class<?> ifc = entry.getKey();
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, 
                             "New underlying connection doesn't implement " + ifc,
                             AdapterUtil.toString(ifcToDynamicWrapper.get(ifc)));

                Tr.warning(tc, "VENDOR_IMPL_NOT_FOUND", ifc, implX);
            }


        // Update the autoCommit value on the underlying ManagedConnection. 
        // Only if no handles are associated with the ManangedConnection yet. 
        //  don't update autocommit at reassociation
        connRequestInfo = null;

        // reset the clientInfo here if it was set
        if (currentClientInfo != null) {
            try {
                // now will pass the saved clientInfoprops to be set on the mc.
                mcf.getHelper().setClientInformationArray(currentClientInfo, managedConn, clientInfoSetExplicitly);
                currentClientInfo = null;
            } catch (SQLException sqle) {
                FFDCFilter.processException(sqle, WSJdbcConnection.class.getName() + ".reassociate()", "1781", this);
                throw new DataStoreAdapterException("DSA_ERROR", sqle, WSJdbcConnection.class);
            }
        }
        else if (clientProps != null)
            try {
                connImpl.setClientInfo(clientProps);
                clientProps = null;
            } catch (SQLException sqle) {
                FFDCFilter.processException(
                                            sqle, getClass().getName() + ".reassociate", "2833", this);
                ResourceException x = new DataStoreAdapterException("DSA_ERROR", proccessSQLException(sqle), WSJdbcConnection.class);
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "reassociate", sqle); 
                throw x;
            }

        // Transition the handle back to the ACTIVE state.
        state = State.ACTIVE;
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "reassociate", "state --> ACTIVE");
    }

    /**
     * <p>Reserve this Connection handle for reassociation only with its current
     * ManagedConnection. This optimization allows child objects of the handle also
     * associated with the ManagedConnection (or associated with underlying objects of the
     * ManagedConnection) to be left open across reassociations. This method should only be
     * used when the guarantee can be made that the handle will always be reassociated with the
     * same ManagedConnection.</p>
     * 
     * <p>The handle remains marked as reserved until either of the following occurs:</p>
     * <ol>
     * <li> A reassociation is requested back to the original handle, or
     * <li> The reserve is overridden with an explicit request to dissociate.
     * </ol>
     * 
     * <p>A handle remains in the ACTIVE state while it is "reserved".</p>
     * 
     * @param key a special key that must be provided to invoke this method.
     * 
     * @throws ResourceException if an incorrect key is supplied or if the handle may not be
     *             reserved from its current state.
     */
    public void reserve(Object key) throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        // Verify the caller is allowed to call this method.
        // If the key is nulled out, the Connection must be closed.
        if (managedConnKey == null)
            throw new DataStoreAdapterException("OBJECT_CLOSED", null, WSJdbcConnection.class, "Connection");
        // If the keys do not match then do not continue.  Access to this method is not
        // allowed for non-WebSphere-internal code.
        if (key != managedConnKey)
            throw new DataStoreAdapterException("NOT_A_JDBC_METHOD", null, WSJdbcConnection.class);
        isReserved = true;

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "Reserving handle", "state --> INACTIVE");
        // A reserved handle is considered dissociated, so state should be INACTIVE.
        state = State.INACTIVE;
    }

    /**
     * Resets a CallableStatement obtained from the statement cache.
     *
     * 
     * @param cstmt the statement.
     * 
     * @return CallableStatement the statement, fully reset and ready for reuse.
     * 
     * @throws SQLException if an error occurs resetting the statement.
     */
    final protected CallableStatement resetStatement(CallableStatement cstmt)
                    throws SQLException 
    {
        // - in order to free up memory, parameters are cleared before caching instead of after
        if (managedConn.resetStmtsInCacheOnRemove)
            mcf.getHelper().doStatementCleanup(cstmt);
        return cstmt;
    }

    /**
     * Resets a PreparedStatement obtained from the statement cache.
     *
     * 
     * @param pstmt the statement.
     * 
     * @return PreparedStatement the statement, fully reset and ready for reuse.
     * 
     * @throws SQLException if an error occurs resetting the statement.
     */
    final protected PreparedStatement resetStatement(PreparedStatement pstmt)
                    throws SQLException 
    {
        // - in order to free up memory, parameters are cleared before caching instead of after
        if (managedConn.resetStmtsInCacheOnRemove)
            mcf.getHelper().doStatementCleanup(pstmt);

        return pstmt;
    }

    /**
     * Fires a LOCAL_TRANSACTION_ROLLEDBACK ConnectionEvent, which rolls back and ends the
     * local transaction.
     * 
     * @throws SQLException if an error occurs during the rollback.
     */
    public void rollback() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "rollback", this);
        try //  - moved try to fix null point problem.
        {

            // if the MC marked Stale, it means the user requested a purge pool with an immediate option
            // so don't allow any work on the mc
            if (managedConn != null && managedConn.isMCStale()) 
            {
                if (isTraceOn && tc.isDebugEnabled()) 
                    Tr.debug(this, tc, "MC is stale", managedConn);
                throw AdapterUtil.staleX();

            }
            else if (state == State.CLOSED) {
                SQLException closedX = createClosedException("Connection");
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "rollback", closedX);
                throw closedX;
            }

            // Check for multithreaded access of the connection handle.  This should not interfere
            // with the transaction timeout case since this is only the application-level local
            // transaction rollback. 
            if (threadID != null)
                detectMultithreadedAccess();
            if (state == State.INACTIVE) {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "rollback", "no-op; state is INACTIVE");
                return;
            }

            // if the conn has enlistment turned off then just rollback
            if (!managedConn.isTransactional()) {
                connImpl.rollback();
                // now closing the children.  i am doing here what is typically done
                // after the normal commit below, but since this is a special case, its
                // done here
                for (int i = childWrappers.size() - 1; i > -1; i--) {
                    ((WSJdbcStatement) childWrappers.get(i))
                                    .closeChildWrappers();
                }
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "rollback", "Enlistment is disabled");
                return;
            }
            // Rollback during a global transaction is an error.
            if (managedConn.inGlobalTransaction()) {
                SQLException sqlX =
                                new SQLException(
                                                AdapterUtil.getNLSMessage(
                                                                          "OP_NOT_VALID_IN_GT",
                                                                          "Connection.rollback"));
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "rollback", sqlX);
                throw sqlX;
            }
            // If it's an application level local tran, roll it back.
            if (managedConn.getTransactionState() == WSStateManager.LOCAL_TRANSACTION_ACTIVE) {
                // Signal the request to roll back with a ConnectionEvent.
                managedConn.processLocalTransactionRolledbackEvent(this);
                //  - loop through the statement wrappers to close resultsets
                for (int i = childWrappers.size() - 1; i > -1; i--) {
                    ((WSJdbcStatement) childWrappers.get(i))
                                    .closeChildWrappers();
                }
            }
            // No transaction is active.  Leave it up to the underlying JDBC driver to
            // enforce the JDBC spec. 
            else
                connImpl.rollback();
        } catch (ResourceException resX) {
            FFDCFilter.processException(
                                        resX,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.rollback",
                                        "2001",
                                        this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", "Exception");
            throw AdapterUtil.toSQLException(resX);
        } catch (SQLException sqlX) // Added catch SQLException. 
        {
            FFDCFilter.processException(
                                        sqlX,
                                        getClass().getName() + ".rollback",
                                        "1860",
                                        this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", "Exception");
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "rollback");
    }

    /**
     * @param runtimeX a RuntimeException which occurred, indicating the wrapper may be closed
     *            or inactive. If the handle is inactive due to a concurrent dissociation on
     *            another thread then we close it and throw an SQLRecoverableException.
     * 
     * @throws SQLRecoverableException if the wrapper is closed or inactive and exception mapping is disabled.
     * 
     * @return the RuntimeException to throw if it isn't.
     */
    final protected RuntimeException runtimeXIfNotClosed(RuntimeException runtimeX)
                    throws SQLException {
        switch (state) {
            case INACTIVE:
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                    Tr.debug(this, tc, "Connection dissociated on another thread while performing an operation. Closing the Connection handle.");
                close();
                throw createClosedException("Connection");
            case CLOSED:
                throw createClosedException("Connection");
            default:
                break; 
        }
        return runtimeX;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "setAutoCommit(" + autoCommit + ") requested by application."); 
        if (state != State.ACTIVE)
            activate(); 
        try {
            // Requests to setAutoCommit(false) must be completely ignored during a global
            // transaction. 
            // Remove synchronization.
            // Setters are not permitted when multiple handles are sharing the same
            // ManagedConnection.   Except we've decided to allow them when
            // the specified value is the same as the current value. 
            if (managedConn.getHandleCount() > 1
                && autoCommit != managedConn.getAutoCommit()) 
                throw createSharingException("setAutoCommit");
            // No transaction active.
            if (managedConn.getTransactionState() == WSStateManager.NO_TRANSACTION_ACTIVE) {
                //  - set autoCommit value after managedConn.setAutoCommit finishes.
                managedConn.setAutoCommit(autoCommit);
                this.autoCommit = autoCommit;
            }
            // Global transaction active.
            else if (managedConn.inGlobalTransaction()) {
                // setAutoCommit(true) is not allowed in a global transaction.
                if (autoCommit)
                    throw new SQLException(
                                    AdapterUtil.getNLSMessage(
                                                              "OP_NOT_VALID_IN_GT",
                                                              "setAutoCommit"));
                else {
                    //  - Do not ignore setAutoCommit(false) in global transaction.
                    if (isTraceOn && tc.isDebugEnabled()) 
                        Tr.debug(this, tc, "setAutoCommit(false) requested during a global transaction.");
                    managedConn.setAutoCommit(autoCommit);
                    this.autoCommit = autoCommit;
                    //  ends
                }
            }
            // Local transaction active.
            else {
                // If setAutoCommit(true) is called during an application level local
                // transaction, our default behavior is to commit and then set
                // autoCommit to true.
                if (autoCommit) {
                    if (isTraceOn && tc.isDebugEnabled()) 
                        Tr.debug(this, tc, "setAutoCommit(true) requested during local transaction; implicitly committing the transaction.");
                    commit();
                }
                //  - set autoCommit value after managedConn.setAutoCommit finishes.
                managedConn.setAutoCommit(autoCommit);
                this.autoCommit = autoCommit;
            }
        } catch (SQLException ex) 
        {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.setAutoCommit",
                                        "2006",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public final void setCatalog(String cLog) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "setCatalog", cLog);

        if (warnedAboutCatalogMatching.compareAndSet(false, true))
            Tr.info(tc, "DEFAULT_MATCH_ORIGINAL", "Catalog", DSConfig.CONNECTION_SHARING);

        activate(); 

        try {
            // Setters are not permitted when multiple handles are sharing the same
            // ManagedConnection.   Except we've decided to allow
            // them when the specified value is the same as the current value. 
            if (managedConn.getHandleCount() > 1
                && !AdapterUtil.match(
                                      cLog,
                                      managedConn.getCatalog())) 
                throw createSharingException("setCatalog");
            managedConn.setCatalog(cLog);

            // Update the connection request information with the new value,
            // so that requests for shared connections will match based on the
            // updated criteria.
            if (managedConn.connectionSharing == ConnectionSharing.MatchCurrentState)
            {
                WSConnectionRequestInfoImpl cri = (WSConnectionRequestInfoImpl) managedConn.getConnectionRequestInfo();
                if (!cri.isCRIChangable()) // only set the cri if its not one of the static ones.
                    managedConn.setCRI(cri = WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                cri.setCatalog(cLog);
            }
        } catch (SQLException ex) 
        {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.setCatalog",
                                        "2011",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * This is for internal SQLJ path only
     * 
     * @param autoCommit new autocommit value
     * @param key key to allow invocation
     * @throws UnsupportedOperationException if customer tries to invoke this method 
     */
    public void setCurrentAutoCommit(boolean autoCommit, Object key) {
        // If the keys do not match then do not continue.  Access to this method is only
        // allowed for WebSphere internal code.
        if (key != managedConnKey)
            throw new UnsupportedOperationException(AdapterUtil.getNLSMessage("NOT_A_JDBC_METHOD"));
        this.autoCommit = autoCommit;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "cached connection autoCommit is set to " + autoCommit);
    }

    /**
     * This is for internal SQLJ path only
     * 
     * @param isolvl new transaction isolation level
     * @param key key to allow invocation
     * @throws UnsupportedOperationException if customer tries to invoke this method
     */
    public void setCurrentTransactionIsolation(int isolvl, Object key) {
        // If the keys do not match then do not continue.  Access to this method only
        // allowed for WebSphere internal code.
        if (key != managedConnKey)
            throw new UnsupportedOperationException(AdapterUtil.getNLSMessage("NOT_A_JDBC_METHOD"));
        this.currentTransactionIsolation = isolvl;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "cached connection isolation level  is set to " + isolvl);
    }

    public final void setReadOnly(boolean readOnly) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled()) 
            Tr.debug(tc, "setReadOnly", readOnly);

        if (warnedAboutReadOnlyMatching.compareAndSet(false, true))
            Tr.info(tc, "DEFAULT_MATCH_ORIGINAL", "ReadOnly", DSConfig.CONNECTION_SHARING);

        activate(); 

        try {
            // Setters are not permitted when multiple handles are sharing the same
            // ManagedConnection.   Except we've decided to allow
            // them when the specified value is the same as the current value. 
            if (managedConn.getHandleCount() > 1
                && readOnly != managedConn.isReadOnly()) 
                throw createSharingException("setReadOnly");
            mcf.getHelper().setReadOnly(managedConn, readOnly, true); 

            //update the CRI Read only  (Its ok to do the update since the ReadOnly is not part of the
            // hashcode.
            if (managedConn.connectionSharing == ConnectionSharing.MatchCurrentState)
            {
                WSConnectionRequestInfoImpl cri = (WSConnectionRequestInfoImpl) managedConn.getConnectionRequestInfo();
                if (!cri.isCRIChangable()) // only set the cri if its not one of the static ones.
                    managedConn.setCRI(cri = WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                cri.setReadOnly(readOnly);
            }

        } catch (SQLException ex) 
        {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.setReadOnly",
                                        "2016",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public final void setTransactionIsolation(int level) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "setTransactionIsolation requested by application.");

        if (state != State.ACTIVE)
            activate(); 

        try {
            // Setters are not permitted when multiple handles are sharing the same
            // ManagedConnection.   Except we've decided to allow them when
            // the specified value is the same as the current value. 
            // It's an error to set the isolation level while in a transaction.  Even though
            // this violates the JDBC spec, we allow the value to be set while in a local
            // transaction.  It is left up to the underlying driver to enforce the spec by
            // throwing an exception.  Apparently DB2 wants to allow this behavior.
            if (managedConn.getHandleCount() > 1
                && level != managedConn.getTransactionIsolation()) 
                throw createSharingException("setTransactionIsolation");
            // - Call the method on the mc not on the physical connection
            managedConn.setTransactionIsolation(level);

            // / - Update the current Transaction Isolation level
            currentTransactionIsolation = level;

            //update the CRI isolation level  (Its ok to do the update since the isolation level is not part of the
            // hashcode.
            if (!supportIsolvlSwitching // in the case if islevel switching we should be ok as we keep a local variable with the value
                && managedConn.connectionSharing == ConnectionSharing.MatchCurrentState)
            {
                WSConnectionRequestInfoImpl cri = (WSConnectionRequestInfoImpl) managedConn.getConnectionRequestInfo();
                if (!cri.isCRIChangable()) // only set the cri if its not one of the static ones.
                    managedConn.setCRI(cri = WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                cri.setTransactionIsolationLevel(level);
            }
        } catch (SQLException ex) 
        {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.setTransactionIsolation",
                                        "2021",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public final void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "setTypeMap", map);

        if (warnedAboutTypeMapMatching.compareAndSet(false, true))
            Tr.info(tc, "DEFAULT_MATCH_ORIGINAL", "TypeMap", DSConfig.CONNECTION_SHARING);

        activate(); 

        try {
            // Setters are not permitted when multiple handles are sharing the same
            // ManagedConnection.   Except we've decided to allow
            // them when the specified value is the same as the current value. 

            //  - only check the handle count.
            // If the application gets the typeMap from the connection, and then add
            // one entry to the map. The typeMap object is still the same, so match method
            // returns true. However, we should not allow to set the typemap

            if (managedConn.getHandleCount() > 1)
                throw createSharingException("setTypeMap");
            managedConn.setTypeMap(map);

            // Update the connection request information with the new value,
            // so that requests for shared connections will match based on the
            // updated criteria.
            if (managedConn.connectionSharing == ConnectionSharing.MatchCurrentState)
            {
                WSConnectionRequestInfoImpl cri = (WSConnectionRequestInfoImpl) managedConn.getConnectionRequestInfo();
                if (!cri.isCRIChangable()) // only set the cri if its not one of the static ones.
                    managedConn.setCRI(cri = WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                cri.setTypeMap(map);
            }
        } catch (SQLException ex) 
        {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.setTypeMap",
                                        "2026",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Get the connection request info object.
     * 
     * @return the connection request info object
     */
    public ConnectionRequestInfo getCRI() {
        return connRequestInfo;
    }

    //  starts
    //  starts
    /**
     * <p>Changes the holdability of ResultSet objects created using this Connection object to
     * the given holdability. </P>
     * 
     * @param holdability a ResultSet holdability constant; either ResultSet.HOLD_CURSORS_OVER_COMMIT
     *            or ResultSet.CLOSE_CURSORS_AT_COMMIT
     * 
     * @exception SQLException If a database access occurs, the given parameter is not a ResultSet
     *                constant indicating holdability, or the given holdability is not supported
     */
    public void setHoldability(int holdability) throws SQLException { 
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "setHoldability requested by application.");

        if (warnedAboutHoldabilityMatching.compareAndSet(false, true))
            Tr.info(tc, "DEFAULT_MATCH_ORIGINAL", "Holdability", DSConfig.CONNECTION_SHARING);

        if (state != State.ACTIVE)
            activate(); 

        try {
            // Setters are not permitted when multiple handles are sharing the same
            // ManagedConnection.   Except we've decided to allow them when
            // the specified value is the same as the current value. 
            if (managedConn.getHandleCount() > 1
                && holdability != managedConn.getHoldability()) 
                throw createSharingException("setHoldability");
            // - Call the method on the mc not on the physical connection
            managedConn.setHoldability(holdability);
            // Update the connection request information with the new value,
            // so that requests for shared connections will match based on the
            // updated criteria.
            if (managedConn.connectionSharing == ConnectionSharing.MatchCurrentState)
            {
                WSConnectionRequestInfoImpl cri = (WSConnectionRequestInfoImpl) managedConn.getConnectionRequestInfo();
                if (!cri.isCRIChangable()) // only set the cri if its not one of the static ones.
                    managedConn.setCRI(cri = WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                cri.setHoldability(holdability);
            }
        } catch (SQLException ex) 
        {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.setHoldability",
                                        "2031",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Retrieves the current holdability of ResultSet objects created using this Connection object.</p>
     * 
     * @return the holdability, one of ResultSet.HOLD_CURSORS_OVER_COMMIT or ResultSet.CLOSE_CURSORS_AT_COMMIT
     * 
     * @exception SQLException If a database access occurs
     */
    public int getHoldability() throws SQLException { 
        if (state != State.ACTIVE)
            activate(); 

        try {
            return managedConn.getHoldability();
        } catch (SQLException ex) {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.getHoldability",
                                        "2055",
                                        this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    //  ends
    //  starts
    /**
     * <p>Creates an unnamed savepoint in the current transaction and returns the new Savepoint object
     * that represents it. <p>
     * 
     * @return the new Savepoint object
     * 
     * @exception SQLException If a database access error occurs or this Connection object is currently
     *                in auto-commit mode.
     */
    public Savepoint setSavepoint() throws SQLException { 
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "setSavepoint", this);

        // Moved the inGlobalTransaction check to the try block to trap the
        // NullPointerException before it gets to the application when this connection handle
        // is closed. 

        if (state != State.ACTIVE)
            activate(); 

        Savepoint sp = null;
        try {
            // The inGlobalTransaction method of the ManagedConnection, not AdapterUtil, must
            // be used in order to ensure we only throw an exception if we are ENLISTED in the
            // global transaction. 

            //  - throw exception if in global transaction
            if (managedConn.inGlobalTransaction()) 
            {
                Tr.error(tc, "OP_NOT_VALID_IN_GT", "setSavepoint");
                SQLException x = new SQLException(AdapterUtil.getNLSMessage("OP_NOT_VALID_IN_GT", "setSavepoint"));
                if (isTraceOn && tc.isEntryEnabled()) 
                    Tr.exit(this, tc, "setSavepoint", "OP_NOT_VALID_IN_GT");
                throw x;
            }

            // - directly call the method on the native connection
            sp = connImpl.setSavepoint();
        } catch (SQLException ex) {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.setSavepoint",
                                        "2082",
                                        this);
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "setSavepoint", "Exception");
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "setSavepoint", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "setSavepoint", sp); 
        return sp; 
    }

    /**
     * <p>Creates a savepoint with the given name in the current transaction and returns the new
     * Savepoint object that represents it. </p>
     * 
     * @param name a String containing the name of the savepoint
     * 
     * @return the new Savepoint object
     * 
     * @exception SQLException f a database access error occurs or this Connection object is
     *                currently in auto-commit mode
     */
    public Savepoint setSavepoint(String name) throws SQLException { 
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "setSavepoint", name);

        // Moved the inGlobalTransaction check to the try block to trap the
        // NullPointerException before it gets to the application when this connection handle
        // is closed. 

        if (state != State.ACTIVE)
            activate(); 

        java.sql.Savepoint sp = null;
        try {
            // The inGlobalTransaction method of the ManagedConnection, not AdapterUtil, must
            // be used in order to ensure we only throw an exception if we are ENLISTED in the
            // global transaction. 

            //  - throw exception if in global transaction
            if (managedConn.inGlobalTransaction()) 
            {
                Tr.error(tc, "OP_NOT_VALID_IN_GT", "setSavepoint");
                SQLException x = new SQLException(AdapterUtil.getNLSMessage("OP_NOT_VALID_IN_GT", "setSavepoint"));
                if (isTraceOn && tc.isEntryEnabled()) 
                    Tr.exit(this, tc, "setSavepoint", "OP_NOT_VALID_IN_GT");
                throw x;
            }

            //  - directly call the method on the native connection
            sp = connImpl.setSavepoint(name);
        } catch (SQLException ex) {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.setSavepoint(String)",
                                        "2116",
                                        this);
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "setSavepoint", "Exception");
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "setSavepoint", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "setSavepoint", sp);
        return sp;
    }

    /**
     * <p>Undoes all changes made after the given Savepoint object was set. This method should be used
     * only when auto-commit has been disabled. </p>
     * 
     * @param savepoint the Savepoint object to roll back to
     * 
     * @exception SQLException If a database access error occurs, the Savepoint object is no longer
     *                valid, or this Connection object is currently in auto-commit mode
     */
    public void rollback(Savepoint savepoint) throws SQLException { 
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "rollback", savepoint);

        // Moved the inGlobalTransaction check to the try block to trap the
        // NullPointerException before it gets to the application when this connection handle
        // is closed. 

        if (state != State.ACTIVE)
            activate(); 

        try {
            // The inGlobalTransaction method of the ManagedConnection, not AdapterUtil, must
            // be used in order to ensure we only throw an exception if we are ENLISTED in the
            // global transaction. 

            //  - throw exception if in global transaction
            if (managedConn.inGlobalTransaction()) 
            {
                Tr.error(tc, "OP_NOT_VALID_IN_GT", "rollback");
                SQLException x = new SQLException(AdapterUtil.getNLSMessage("OP_NOT_VALID_IN_GT", "rollback"));
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "rollback", "OP_NOT_VALID_IN_GT");
                throw x;
            }

            //  - directly call the method on the native connection
            connImpl.rollback(savepoint);
            //  - ResultSets need to be implicitly closed by rollback(savept).
            for (int i = childWrappers.size() - 1; i > -1; i--) {
                ((WSJdbcStatement) childWrappers.get(i)).closeChildWrappers();
            }
        } catch (SQLException ex) {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.rollback(Savepoint)",
                                        "2141",
                                        this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", "Exception");
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", "Exception");
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "rollback");
    }

    /**
     * <p>Removes the given Savepoint object from the current transaction. Any reference to the savepoint
     * after it have been removed will cause an SQLException to be thrown. </p>
     * 
     * @param savepoint the Savepoint object to be removed
     * 
     * @exception SQLException If a database access error occurs or the given Savepoint object is not
     *                a valid savepoint in the current transaction
     */
    public void releaseSavepoint(Savepoint savepoint)
                    throws SQLException { 
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "releaseSavepoint", savepoint);

        // Moved the inGlobalTransaction check to the try block to trap the
        // NullPointerException before it gets to the application when this connection handle
        // is closed. 

        if (state != State.ACTIVE)
            activate(); 

        try {
            // The inGlobalTransaction method of the ManagedConnection, not AdapterUtil, must
            // be used in order to ensure we only throw an exception if we are ENLISTED in the
            // global transaction. 

            //  - throw exception if in global transaction
            if (managedConn.inGlobalTransaction()) 
            {
                Tr.error(tc, "OP_NOT_VALID_IN_GT", "releaseSavepoint");
                SQLException x = new SQLException(AdapterUtil.getNLSMessage("OP_NOT_VALID_IN_GT", "releaseSavepoint"));
                if (isTraceOn && tc.isEntryEnabled()) 
                    Tr.exit(this, tc, "releaseSavepoint", "OP_NOT_VALID_IN_GT");
                throw x;
            }

            //  - directly call the method on the native connection
            connImpl.releaseSavepoint(savepoint);
        } catch (SQLException ex) {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.releaseSavepoint",
                                        "2169",
                                        this);
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "releaseSavepoint", "Exception");
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "releaseSavepoint", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "releaseSavepoint");
    }

    /**
     * <p>Creates a Statement object that will generate ResultSet objects with the given type, concurrency,
     * and holdability. This method is the same as the createStatement method above, but it allows the
     * default result set type, concurrency, and holdability to be overridden. </p>
     * 
     * @param resultSetType one of the following ResultSet constants: ResultSet.TYPE_FORWARD_ONLY,
     *            ResultSet.TYPE_SCROLL_INSENSITIVE, or ResultSet.TYPE_SCROLL_SENSITIVE
     * @param resultSetConcurrency one of the following ResultSet constants: ResultSet.CONCUR_READ_ONLY
     *            or ResultSet.CONCUR_UPDATABLE
     * @param resultSetHoldability one of the following ResultSet constants: ResultSet.HOLD_CURSORS_OVER_COMMIT
     *            or ResultSet.CLOSE_CURSORS_AT_COMMIT
     * 
     * @return a new Statement object that will generate ResultSet objects with the given type, concurrency,
     *         and holdability
     * 
     * @exception SQLException If a database access error occurs or the given parameters are not
     *                ResultSet constants indicating type, concurrency, and holdability
     */
    public java.sql.Statement createStatement(
                                              int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability)
                    throws SQLException { 
                                          // we cannot make createStatement(resultSetType, resultSetConcurrency) to call
                                          // createStatement(resultSetType, resultSetConcurrency, resultSetHoldability) since
                                          // the default resultSetHoldability is different for different backends.
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(tc, "createStatement",
                                   AdapterUtil.getResultSetTypeString(resultSetType),
                                   AdapterUtil.getConcurrencyModeString(resultSetConcurrency),
                                   AdapterUtil.getCursorHoldabilityString(resultSetHoldability));
        Statement stmt = null; 
        try {
            if (state != State.ACTIVE)
                activate(); 
            beginTransactionIfNecessary();
            stmt =
                            connImpl.createStatement(
                                                     resultSetType,
                                                     resultSetConcurrency,
                                                     resultSetHoldability);
            Integer queryTimeout = dsConfig.get().queryTimeout; 
            if (queryTimeout != null) { 
                if(isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                stmt.setQueryTimeout(queryTimeout); 
            }
            // Wrap the Statement.
            stmt = createStatementWrapper(stmt, resultSetHoldability);
            childWrappers.add(stmt);
        } catch (SQLException ex) {
            FFDCFilter.processException(
                                        ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection.createStatement(int, int, int)",
                                        "2268",
                                        this);
            if (stmt != null)
                try {
                    stmt.close();
                } catch (Throwable x) {
                } 
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "createStatement", "Exception");
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "createStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "createStatement", stmt);
        return stmt;
    }

    /**
     * <p>Creates a PreparedStatement object that will generate ResultSet objects with the given type,
     * concurrency, and holdability. </p>
     * 
     * <p>This method is the same as the prepareStatement method above, but it allows the default result
     * set type, concurrency, and holdability to be overridden. </p>
     * 
     * @param sql a String object that is the SQL statement to be sent to the database; may contain one
     *            or more ? IN parameters
     * @param resultSetType one of the following ResultSet constants: ResultSet.TYPE_FORWARD_ONLY,
     *            ResultSet.TYPE_SCROLL_INSENSITIVE, or ResultSet.TYPE_SCROLL_SENSITIVE
     * @param resultSetConcurrency one of the following ResultSet constants: ResultSet.CONCUR_READ_ONLY or
     *            ResultSet.CONCUR_UPDATABLE
     * @param resultSetHoldability one of the following ResultSet constants: ResultSet.HOLD_CURSORS_OVER_COMMIT
     *            or ResultSet.CLOSE_CURSORS_AT_COMMIT
     * 
     * @return a new PreparedStatement object, containing the pre-compiled SQL statement, that will generate
     *         ResultSet objects with the given type, concurrency, and holdability
     * 
     * @exception SQLException If a database access error occurs or the given parameters are not ResultSet
     *                constants indicating type, concurrency, and holdability
     */
    public java.sql.PreparedStatement prepareStatement(
                                                       String sql,
                                                       int type,
                                                       int concurrency,
                                                       int holdability)
                    throws SQLException { 
                                          // we cannot make prepareStatement(resultSetType, resultSetConcurrency) to call
                                          // prepareStatement(resultSetType, resultSetConcurrency, resultSetHoldability) since
                                          // the default resultSetHoldability is different for different backends.
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "prepareStatement", sql,
                     AdapterUtil.getResultSetTypeString(type),
                     AdapterUtil.getConcurrencyModeString(concurrency),
                     AdapterUtil.getCursorHoldabilityString(holdability));
        PreparedStatement pstmt = null; 
        try {
            if (state != State.ACTIVE)
                activate(); 
            // Only create a PSCacheKey if statement caching is enabled. 
            if (managedConn.isStatementCachingEnabled()) {
                PSCacheKey key = new PSCacheKey( 
                                sql, type, concurrency, holdability, 0, 
                                mcf.doesStatementCacheIsoLevel ? currentTransactionIsolation : 0,
                                managedConn.getCurrentSchema()); 

                beginTransactionIfNecessary();
                // get a prepared statement from the cache
                Object s = managedConn.getStatement(key);
                if (s == null) // not found in cache, so create a new one
                {
                    pstmt = connImpl.prepareStatement(sql, type, concurrency, holdability);
                    Integer queryTimeout = dsConfig.get().queryTimeout; 
                    if (queryTimeout != null) {
                        if(isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                        pstmt.setQueryTimeout(queryTimeout); 
                    }
                } else
                    // found in cache
                    pstmt = resetStatement((PreparedStatement) s);
                pstmt = createPreparedStatementWrapper(
                                                       pstmt, holdability, sql, key); 
            } else // Statement caching is not enabled.
            {
                beginTransactionIfNecessary();
                pstmt = connImpl.prepareStatement(sql, type, concurrency, holdability);
                Integer queryTimeout = dsConfig.get().queryTimeout; 
                if (queryTimeout != null) { 
                    if(isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                    pstmt.setQueryTimeout(queryTimeout); 
                }
                pstmt = createPreparedStatementWrapper(
                                                       pstmt, holdability, sql); 
            }
            childWrappers.add(pstmt);
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (Throwable x) {
                } 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareStatement", ex); 
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "prepareStatement", pstmt); 
        return pstmt; 
    }

    /**
     * <p>Creates a CallableStatement object that will generate ResultSet objects with the given type
     * and concurrency.</p>
     * 
     * <p>This method is the same as the prepareCall method above, but it allows the default result
     * set type, result set concurrency type and holdability to be overridden. </p>
     * 
     * @param sql a String object that is the SQL statement to be sent to the database; may contain
     *            on or more ? parameters
     * @param resultSetType one of the following ResultSet constants: ResultSet.TYPE_FORWARD_ONLY,
     *            ResultSet.TYPE_SCROLL_INSENSITIVE, or ResultSet.TYPE_SCROLL_SENSITIVE
     * @param resultSetConcurrency one of the following ResultSet constants: ResultSet.CONCUR_READ_ONLY
     *            or ResultSet.CONCUR_UPDATABLE
     * @param resultSetHoldability one of the following ResultSet constants: ResultSet.HOLD_CURSORS_OVER_COMMIT
     *            or ResultSet.CLOSE_CURSORS_AT_COMMIT
     * 
     * @return a new CallableStatement object, containing the pre-compiled SQL statement, that will
     *         generate ResultSet objects with the given type, concurrency, and holdability
     * 
     * @exception SQLException If a database access error occurs or the given parameters are not
     *                ResultSet constants indicating type, concurrency, and holdability
     */
    public java.sql.CallableStatement prepareCall(
                                                  String sql,
                                                  int type,
                                                  int concurrency,
                                                  int holdability)
                    throws SQLException { 
                                          // we cannot make prepareCall(sql, resultSetType, resultSetConcurrency) to call
                                          // prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability) since
                                          // the default resultSetHoldability is different for different backends.
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "prepareCall", sql,
                     AdapterUtil.getResultSetTypeString(type),
                     AdapterUtil.getConcurrencyModeString(concurrency),
                     AdapterUtil.getCursorHoldabilityString(holdability));
        CallableStatement cstmt = null; 
        try {
            if (state != State.ACTIVE)
                activate(); 
            if (managedConn.isStatementCachingEnabled()) {
                CSCacheKey key = new CSCacheKey( 
                    sql, type, concurrency, holdability, 
                    mcf.doesStatementCacheIsoLevel ? currentTransactionIsolation : 0,
                    managedConn.getCurrentSchema());

                beginTransactionIfNecessary();
                // get a prepared statement from the cache
                Object s = managedConn.getStatement(key);
                if (s == null) // not found in cache, so create a new one
                {
                    cstmt = connImpl.prepareCall(sql, type, concurrency, holdability);
                    Integer queryTimeout = dsConfig.get().queryTimeout; 
                    if (queryTimeout != null) { 
                        if(isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                        cstmt.setQueryTimeout(queryTimeout); 
                    }
                } else
                    // found in cache
                    cstmt = resetStatement((CallableStatement) s);
                // Wrap the CallableStatement.
                cstmt = createCallableStatementWrapper(
                                                       cstmt, holdability, sql, key); 
            } else // Statement caching is not enabled.
            {
                beginTransactionIfNecessary();
                cstmt = connImpl.prepareCall(sql, type, concurrency, holdability);
                Integer queryTimeout = dsConfig.get().queryTimeout; 
                if (queryTimeout != null) { 
                    if(isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                    cstmt.setQueryTimeout(queryTimeout); 
                }
                // Wrap the CallableStatement.
                cstmt = createCallableStatementWrapper(
                                                       cstmt, holdability, sql); 
            }
            childWrappers.add(cstmt); 
        } catch (SQLException sqlX) {
            // No FFDC code needed. Might be an application error. 
            if (cstmt != null)
                try {
                    cstmt.close();
                } catch (Throwable x) {
                } 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareCall", sqlX); 
            throw proccessSQLException(sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareCall", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "prepareCall", cstmt); 
        return cstmt; 
    }

    /**
     * <p>Creates a default PreparedStatement object that has the capability to retrieve auto-generated
     * keys. The given constant tells the driver whether it should make auto-generated keys available
     * for retrieval. This parameter is ignored if the SQL statement is not an INSERT statement. </p>
     * 
     * <p><b>Note</b>: This method is optimized for handling parametric SQL statements that benefit
     * from precompilation. If the driver supports precompilation, the method prepareStatement will
     * send the statement to the database for precompilation. Some drivers may not support precompilation.
     * In this case, the statement may not be sent to the database until the PreparedStatement object is
     * executed. This has no direct effect on users; however, it does affect which methods throw certain
     * SQLExceptions. </p>
     * 
     * <p>Result sets created using the returned PreparedStatement object will by default be type
     * TYPE_FORWARD_ONLY and have a concurrency level of CONCUR_READ_ONLY. </p>
     * 
     * @param sql an SQL statement that may contain one or more '?' IN parameter placeholders
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys should be returned;
     *            one of Statement.RETURN_GENERATED_KEYS or Statement.NO_GENERATED_KEYS
     * 
     * @return a new PreparedStatement object, containing the pre-compiled SQL statement, that will
     *         have the capability of returning auto-generated keys
     * 
     * @exception SQLException If a database access error occurs or the given parameter is not a Statement
     *                constant indicating whether auto-generated keys should be returned
     */
    public java.sql.PreparedStatement prepareStatement(
                                                       String sql,
                                                       int autoGeneratedKeys)
                    throws SQLException { 
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "prepareStatement", sql, AdapterUtil.getAutoGeneratedKeyString(autoGeneratedKeys));
        PreparedStatement pstmt = null; 
        try {
            if (state != State.ACTIVE)
                activate(); 
            // Only create a PSCacheKey if statement caching is enabled. 
            if (managedConn.isStatementCachingEnabled()) {
                // Result sets created using the returned PreparedStatement object will by default be type
                // TYPE_FORWARD_ONLY and have a concurrency level of CONCUR_READ_ONLY
                PSCacheKey key = new PSCacheKey( 
                                sql,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY,
                                managedConn.getCurrentHoldability(),
                                autoGeneratedKeys,
                                mcf.doesStatementCacheIsoLevel ? currentTransactionIsolation : 0,
                                managedConn.getCurrentSchema());
                beginTransactionIfNecessary();
                // get a prepared statement from the cache
                Object s = managedConn.getStatement(key);
                if (s == null) // not found in cache, so create a new one
                {
                    pstmt = connImpl.prepareStatement(sql, autoGeneratedKeys);
                    Integer queryTimeout = dsConfig.get().queryTimeout; 
                    if (queryTimeout != null) { 
                        if(isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                        pstmt.setQueryTimeout(queryTimeout); 
                    }
                } else
                    // found in cache
                    pstmt = resetStatement((PreparedStatement) s);
                pstmt =
                                createPreparedStatementWrapper(
                                                               pstmt,
                                                               managedConn.getCurrentHoldability(),
                                                               sql, 
                                                               key);
            } else // Statement caching is not enabled.
            {
                beginTransactionIfNecessary();
                pstmt = connImpl.prepareStatement(sql, autoGeneratedKeys);
                Integer queryTimeout = dsConfig.get().queryTimeout; 
                if (queryTimeout != null) { 
                    if(isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                    pstmt.setQueryTimeout(queryTimeout); 
                }
                pstmt =
                                createPreparedStatementWrapper(
                                                               pstmt,
                                                               managedConn.getCurrentHoldability(),
                                                               sql); 
            }
            childWrappers.add(pstmt);
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (Throwable x) {
                } 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareStatement", ex); 
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "prepareStatement", pstmt); 
        return pstmt; 
    }

    /**
     * <p>Creates a default PreparedStatement object capable of returning the auto-generated
     * keys designated by the given array. This array contains the indexes of the columns in
     * the target table that contain the auto-generated keys that should be made available.
     * This array is ignored if the SQL statement is not an INSERT statement.</p>
     * 
     * <p>An SQL statement with or without IN parameters can be pre-compiled and stored in a
     * PreparedStatement object. This object can then be used to efficiently execute this
     * statement multiple times. </p>
     * 
     * <p><b>Note</b>: This method is optimized for handling parametric SQL statements that
     * benefit from precompilation. If the driver supports precompilation, the method prepareStatement
     * will send the statement to the database for precompilation. Some drivers may not
     * support precompilation. In this case, the statement may not be sent to the database
     * until the PreparedStatement object is executed. This has no direct effect on users;
     * however, it does affect which methods throw certain SQLExceptions. </p>
     * 
     * <p>Result sets created using the returned PreparedStatement object will by default be
     * type TYPE_FORWARD_ONLY and have a concurrency level of CONCUR_READ_ONLY. </p>
     * 
     * @param sql an SQL statement that may contain one or more '?' IN parameter placeholders
     * @param columnIndexes an array of column indexes indicating the columns that should be
     *            returned from the inserted row or rows
     * 
     * @return a new PreparedStatement object, containing the pre-compiled statement, that is
     *         capable of returning the auto-generated keys designated by the given array of column indexes
     * 
     * @exception SQLException If a database access error occurs
     */
    public java.sql.PreparedStatement prepareStatement(
                                                       String sql,
                                                       int[] columnIndexes)
                    throws SQLException { 
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "prepareStatement", sql, Arrays.toString(columnIndexes));
        PreparedStatement pstmt = null; 
        try {
            if (state != State.ACTIVE)
                activate(); 
            // We don't cache prepareStatement with column indexes.
            beginTransactionIfNecessary();
            pstmt = connImpl.prepareStatement(sql, columnIndexes);
            Integer queryTimeout = dsConfig.get().queryTimeout; 
            if (queryTimeout != null) { 
                if(isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                pstmt.setQueryTimeout(queryTimeout); 
            }
            pstmt = createPreparedStatementWrapper(
                                                   pstmt,
                                                   managedConn.getCurrentHoldability(),
                                                   sql); 
            childWrappers.add(pstmt);
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (Throwable x) {
                } 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareStatement", ex); 
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "prepareStatement", pstmt); 
        return pstmt; 
    }

    /**
     * <p>Creates a default PreparedStatement object capable of returning the auto-generated keys
     * designated by the given array. This array contains the names of the columns in the target
     * table that contain the auto-generated keys that should be returned. This array is ignored
     * if the SQL statement is not an INSERT statement. </p>
     * 
     * 
     * <p>An SQL statement with or without IN parameters can be pre-compiled and stored in a
     * PreparedStatement object. This object can then be used to efficiently execute this
     * statement multiple times. </p>
     * 
     * <p><b>Note</b>: This method is optimized for handling parametric SQL statements that
     * benefit from precompilation. If the driver supports precompilation, the method prepareStatement
     * will send the statement to the database for precompilation. Some drivers may not
     * support precompilation. In this case, the statement may not be sent to the database
     * until the PreparedStatement object is executed. This has no direct effect on users;
     * however, it does affect which methods throw certain SQLExceptions. </p>
     * 
     * <p>Result sets created using the returned PreparedStatement object will by default be
     * type TYPE_FORWARD_ONLY and have a concurrency level of CONCUR_READ_ONLY. </p>
     * 
     * @param sql an SQL statement that may contain one or more '?' IN parameter placeholders
     * @param columnNames an array of column names indicating the columns that should be returned
     *            from the inserted row or rows
     * 
     * @return a new PreparedStatement object, containing the pre-compiled statement, that is
     *         capable of returning the auto-generated keys designated by the given array of column names
     * 
     * @exception SQLException If a database access error occurs
     */
    public java.sql.PreparedStatement prepareStatement(
                                                       String sql,
                                                       String[] columnNames)
                    throws SQLException { 
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "prepareStatement", sql, Arrays.toString(columnNames));
        PreparedStatement pstmt = null; 
        try {
            if (state != State.ACTIVE)
                activate(); 
            // We don't cache PreparedStatement with column names.
            beginTransactionIfNecessary();
            pstmt = connImpl.prepareStatement(sql, columnNames);
            Integer queryTimeout = dsConfig.get().queryTimeout; 
            if (queryTimeout != null) { 
                if(isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "apply default query timeout to " + queryTimeout);
                pstmt.setQueryTimeout(queryTimeout); 
            }
            pstmt = createPreparedStatementWrapper(
                                                   pstmt,
                                                   managedConn.getCurrentHoldability(),
                                                   sql); 
            childWrappers.add(pstmt);
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (Throwable x) {
                } 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareStatement", ex); 
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepareStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "prepareStatement", pstmt); 
        return pstmt; 
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "setClientInfo", properties);

        try {
            activate(); 

            try {
                connImpl.setClientInfo(properties);
                managedConn.clientInfoExplicitlySet = true;
            } catch (NullPointerException nullX) {
                // No FFDC code needed; we might be closed.
                throw runtimeXIfNotClosed(nullX);
            } catch (AbstractMethodError methError) {
                // No FFDC code needed; wrong JDBC level.
                throw AdapterUtil.notSupportedX("Connection.setClientInfo", methError);
            } catch (RuntimeException runX) {
                FFDCFilter.processException(
                                            runX, getClass().getName() + ".setClientInfo", "4578", this);
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, "setClientInfo", runX); 
                throw runX;
            } catch (Error err) {
                FFDCFilter.processException(
                                            err, getClass().getName() + ".setClientInfo", "4585", this);
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, "setClientInfo", err); 
                throw err;
            }
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setClientInfo", "4593", this);

            // If a SQLClientInfoException maps to a generic SQLException, we are only able
            // to throw the original SQLClientInfoException.  Still perform the mapping logic
            // because it might notify the application server of a connection error.

            proccessSQLException(sqlX);

            if (!(sqlX instanceof SQLClientInfoException)) {
                Map<String, ClientInfoStatus> failed = new HashMap<String, ClientInfoStatus>();
                for (Object propName : properties.keySet())
                    failed.put((String) propName, ClientInfoStatus.REASON_UNKNOWN);

                throw new SQLClientInfoException(
                                sqlX.getMessage(), sqlX.getSQLState(), sqlX.getErrorCode(), failed, sqlX);
            }

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "setClientInfo", sqlX); 
            throw (SQLClientInfoException) sqlX;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "setClientInfo"); 
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "setClientInfo", name, value);

        try {
            activate(); 

            try {
                connImpl.setClientInfo(name, value);
                managedConn.clientInfoExplicitlySet = true;
            } catch (NullPointerException nullX) {
                // No FFDC code needed; we might be closed.
                throw runtimeXIfNotClosed(nullX);
            } catch (AbstractMethodError methError) {
                // No FFDC code needed; wrong JDBC level.
                throw AdapterUtil.notSupportedX("Connection.setClientInfo", methError);
            } catch (RuntimeException runX) {
                FFDCFilter.processException(
                                            runX, getClass().getName() + ".setClientInfo", "4414", this);
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, "setClientInfo", runX); 
                throw runX;
            } catch (Error err) {
                FFDCFilter.processException(
                                            err, getClass().getName() + ".setClientInfo", "4421", this);
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, "setClientInfo", err); 
                throw err;
            }
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setClientInfo", "4429", this);

            // If a SQLClientInfoException maps to a generic SQLException, we are only able
            // to throw the original SQLClientInfoException.  Still perform the mapping logic
            // because it might notify the application server of a connection error.

            proccessSQLException(sqlX);

            if (!(sqlX instanceof SQLClientInfoException)) {
                // Indicate that the property was not set.
                Map<String, ClientInfoStatus> fProps = new HashMap<String, ClientInfoStatus>();
                fProps.put(name, ClientInfoStatus.REASON_UNKNOWN);

                throw new SQLClientInfoException(
                                sqlX.getMessage(), sqlX.getSQLState(), sqlX.getErrorCode(), fProps, sqlX);
            }

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "setClientInfo", sqlX); 
            throw (SQLClientInfoException) sqlX;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "setClientInfo"); 
    }

    /**
     * Need to set the mc's threadid to prevent false multi thread detection.
     * 
     * @param mc's thread id.
     * @param special key
     * @throws DataStoreAdapterException
     */
    public void setThreadID(Object threadID, Object key) throws DataStoreAdapterException {
        if (key != managedConnKey)
            throw new DataStoreAdapterException("NOT_A_JDBC_METHOD", null, WSJdbcConnection.class);
        this.threadID = threadID;
    }

    /*
     * This method is called in the context of stale statement handling.
     * It prevents pooling of all open statements of this handle from
     * being pooled.
     */
    void markStmtsAsNotPoolable() {
        TraceComponent tc = getTracer();
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (childWrappers != null && !childWrappers.isEmpty()) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.entry(this, tc, "markStmtsAsNotPoolable");
            WSJdbcObject wrapper = null;
            for (int i = childWrappers.size(); i > 0;)
                try {
                    wrapper = (WSJdbcObject) childWrappers.get(--i);
                    if (wrapper instanceof WSJdbcPreparedStatement)
                        ((WSJdbcPreparedStatement) wrapper).poolabilityHint = false;
                } catch (ArrayIndexOutOfBoundsException ioobX) {
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "ArrayIndexOutOfBoundsException is caught during closeChildWrappers() of the WSJdbcObject");
                        Tr.debug(this, tc, "Possible causes:");
                        Tr.debug(this, tc, "multithreaded access of JDBC objects by the Application");
                        Tr.debug(this, tc, "Application is closing JDBC objects in a finalize()");
                        Tr.debug(this, tc, "Exception is: ", ioobX);
                    }
                    throw ioobX;
                }
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "markStmtsAsNotPoolable");
        }
    }

    /**
     * This method retrieves statement wrapper, given the physical statement object,
     * and sets it's poolability hint to the value supplied as an argument.
     */
    public void setPoolableFlag(PreparedStatement statement, boolean poolable) {
        TraceComponent tc = getTracer();
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "Marking statement as not poolable.", statement, poolable);
        try {
            int size = childWrappers != null ? this.childWrappers.size() : 0;
            for (int i = 0; i < size; i++) {
                Object child = this.childWrappers.get(i);
                if ((child instanceof WSJdbcPreparedStatement)
                    && (statement == WSJdbcTracer.getImpl(((WSJdbcPreparedStatement) child).stmtImpl))) {
                    ((WSJdbcPreparedStatement) child).poolabilityHint = poolable;
                }
            }
        } catch (ArrayIndexOutOfBoundsException ioobX) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "ArrayIndexOutOfBoundsException is caught during closeChildWrappers() of the WSJdbcObject ", this);
                Tr.debug(this, tc, "Possible causes:");
                Tr.debug(this, tc, "multithreaded access of JDBC objects by the Application");
                Tr.debug(this, tc, "Application is closing JDBC objects in a finalize()");
                Tr.debug(this, tc, "Exception is: ", ioobX);
            }
            throw ioobX;
        }
    }

    /**
     * Intercept the proxy handler to detect changes to connection properties
     * that must be reset before pooling.
     * 
     * @param proxy
     *            the dynamic proxy.
     * @param method
     *            the method being invoked.
     * @param args
     *            the parameters to the method.
     * @return the result of invoking the operation on the underlying object.
     * @throws Throwable
     *             if something goes wrong.
     */
    public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
        String methodName = method.getName();

        if(managedConn!=null) {
            /*
             * only need to cache the default properties on the first use of one
             * the cached methods per the life of an MC
             */
            if (!managedConn.haveVendorConnectionPropertiesChanged
                            && WSRdbManagedConnectionImpl.VENDOR_PROPERTY_SETTERS
                            .contains(methodName)) {

                if (managedConn.CONNECTION_VENDOR_DEFAULT_PROPERTIES == null) {

                    managedConn.CONNECTION_VENDOR_DEFAULT_PROPERTIES =
                                    mcf.getHelper()
                                    .cacheVendorConnectionProps(this.connImpl);
                }

                managedConn.haveVendorConnectionPropertiesChanged = true;
            }

            if (managedConn.isStatementCachingEnabled()
                            && !managedConn.resetStmtsInCacheOnRemove) {

                if (WSRdbManagedConnectionImpl.VENDOR_STM_AND_CONNECTION_PROPERTY_SETTERS
                                .contains(methodName)) {

                    managedConn.resetStmtsInCacheOnRemove = true;
                }
            }
        }

        return super.invoke(proxy, method, args);
    }

    // PostgreSQL only
    public Object getLargeObjectAPI() throws SQLException {
        activate();
        return mcf.getHelper().getLargeObjectAPI(this);
    }

    public void abort(Executor executor) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isAborted() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setAborted(boolean aborted) throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    public int getNetworkTimeout() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }
    
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public String getSchema() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setSchema(String schema) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }
    
    /**
     * <li> Checks if the SQLException was caused by a NetworkTimeout.  If it was,
     * our wrapper objects will be closed to reflect the resources that the 
     * JDBC driver must close when a NetworkTimeout occurs.
     * <li> After the first check is complete, the exception gets mapped using <code>WSJdbcUtil</code> 
     */
    protected SQLException proccessSQLException(SQLException ex)
    {
        return WSJdbcUtil.mapException(this, ex);
    }
}