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
package com.ibm.ws.rsadapter.impl;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference; 

import javax.security.auth.Subject;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.StatementEvent;
import javax.sql.StatementEventListener;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSName;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.DissociatableManagedConnection;
import javax.resource.spi.LazyEnlistableConnectionManager;
import javax.resource.spi.LazyEnlistableManagedConnection;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.LocalTransactionException;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.SecurityException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.jca.adapter.WSConnectionManager;
import com.ibm.ws.jca.adapter.WSManagedConnection;
import com.ibm.ws.jdbc.internal.DataSourceDef;
import com.ibm.ws.jdbc.osgi.JDBCRuntimeVersion;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.CommitOrRollbackOnCleanup; 
import com.ibm.ws.rsadapter.ConnectionSharing;
import com.ibm.ws.rsadapter.DSConfig; 
import com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcTracer;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

/**
 * The WSRdbManagedConnectionImpl implements the javax.resource.spi.ManagedConnection interface.
 * It represents a physical connection to a database. It contains a JDBC physical connection.
 * <p>
 * Multiple Connection handles can be returned by one WSRdbManagedConnection. This is the
 * connector runtime's responsibility to manage Connection handles. The Interaction object
 * is also cached in the connection handle for performance reason.
 * <p>
 * This class also provides a connection notification callback mechanism. When a
 * connection is closed, or a local transaction is started or is ended, this class will
 * send out the notification to the participants to inform the event has occurred.
 * <p>
 * This class caches the prepared statement for a specific transaction. When a transaction
 * is ended, or any exception causes this instance to clean up, all the cached prepared
 * statements will be closed.
 */
public class WSRdbManagedConnectionImpl extends WSManagedConnection implements
                DissociatableManagedConnection, 
                LazyEnlistableManagedConnection, 
                javax.sql.ConnectionEventListener, 
                StatementEventListener, 
                FFDCSelfIntrospectable {

    private boolean aborted;

    /**
     * Indicates whether any Vendor Specific Connection properties have changed.
     */
    public boolean haveVendorConnectionPropertiesChanged = false; 

    /**
     * Indicates that Vendor Statement and Connection properties are out of sync.
     */
    public boolean resetStmtsInCacheOnRemove = false;

    /**
     * Cache Connections Default Vendor Properties
     */
    public Map<String, Object> CONNECTION_VENDOR_DEFAULT_PROPERTIES = null;

    /**
     * When any of the APIs in this Set object are invoked via the wrapper pattern or WSCallHelper,
     * the haveVendorConnectionPropertiesChanged boolean, should be set so to true. So the RRA code
     * will later invoke the internal data store helper to reset the properties to the default value
     * before the connection is pooled.
     * 
     */
    public static final Set<String> VENDOR_PROPERTY_SETTERS = new HashSet<String>();
    static {
        /*
         * Oracle property setters
         * If this is changed, then OracleUtilityImpl needs to be updated
         */
        VENDOR_PROPERTY_SETTERS.add("setDefaultExecuteBatch");
        VENDOR_PROPERTY_SETTERS.add("setDefaultRowPrefetch");
        VENDOR_PROPERTY_SETTERS.add("setDefaultTimeZone");
        VENDOR_PROPERTY_SETTERS.add("setIncludeSynonyms");
        VENDOR_PROPERTY_SETTERS.add("setRemarksReporting");
        VENDOR_PROPERTY_SETTERS.add("setRestrictGetTables");
        VENDOR_PROPERTY_SETTERS.add("setSessionTimeZone");
        
        /*
         * PostgreSQL property setters
         */
        VENDOR_PROPERTY_SETTERS.add("setDefaultFetchSize");
        VENDOR_PROPERTY_SETTERS.add("setPrepareThreshold");
        VENDOR_PROPERTY_SETTERS.add("setAutosave");
    }

    /**
     * Setter methods for Vendor properties. These properties are exposed on both the
     * statement and the connection. When invoked via the wrapper pattern or
     * WSCallHelper, the ResetStmtsInCacheOnRemove indicator should be set so that
     * we later reset the statements properties to match the connections properties.
     * when the connection is pooled.
     * 
     */
    public static final Set<String> VENDOR_STM_AND_CONNECTION_PROPERTY_SETTERS = new HashSet<String>();
    static {

        VENDOR_STM_AND_CONNECTION_PROPERTY_SETTERS.add("setDefaultExecuteBatch"); 
        VENDOR_STM_AND_CONNECTION_PROPERTY_SETTERS.add("setDefaultRowPrefetch"); 

    }

    /**
     * Copy of DSConfig.connectionSharing setting, which is refreshed each time we
     * get the first connection handle for this managed connection. This allows us to
     * support dynamic update to connectionSharing without interfering with connections
     * that were already in use at the time when the update occurred.
     */
    public ConnectionSharing connectionSharing;

    /**
     * Reference to the data source configuration.
     * 
     */
    final AtomicReference<DSConfig> dsConfig;

    private boolean kerberosConnection; // true if the connecation is a kerberos one.

    GSSCredential mc_gssCredential;
    GSSName mc_gssName;

    /**
     * Cached copy of dsConfig.transactional that is valid until cleanup.
     * This allows for async config updates to be honored on the next time the
     * managed connection is obtained from the pool, without interfering with
     * usage that is currently in progress.
     * 
     */
    private Boolean transactional;

    /**
     * Indicates if doConnectionCleanupPerCloseConnection is needed due to doConnectionSetupPerGetConnection.
     */
    public boolean perCloseCleanupNeeded;

    boolean _claimedVictim;

    private transient boolean rrsGlobalTransactionReallyActive; //new code RRS

    public void setKerberosConnection() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setting this mc to indicate it was gotten using kerberos");

        kerberosConnection = true;
    }


    public void setRrsGlobalTransactionReallyActive(boolean flag) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setRrsGlobalTransactionReallyActive is set to: ", Boolean.valueOf(flag)); 

        rrsGlobalTransactionReallyActive = flag;
    }


    /**
     * indicates if the mc is stale or not. MC is considered stale, if the Purge pool
     * call was initiated by the user.
     */
    boolean _mcStale; // default is false

    /**
     * Marks the managed connection as stale.
     */
    public void markStale() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "mark mc stale");
        _mcStale = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.rsadapter.spi.WSManagedConnection#isMCStale()
     */
    public boolean isMCStale() {
        return _mcStale;
    }


    //Connections
    java.sql.Connection sqlConn;
    private javax.sql.PooledConnection poolConn;

    /**
     * List of all handles associated with this ManagedConnection.
     * 
     */
    private WSJdbcConnection[] handlesInUse = new WSJdbcConnection[maxHandlesInUse];

    /**
     * Counter of handles associated with this ManagedConnection.
     */
    private int numHandlesInUse;

    /**
     * The maximum size to allocate for new handle lists. This value is autonomically adjusted
     * if we find more handles are required.
     * 
     */
    private static int maxHandlesInUse = 15;

    /**
     * Tracks whether the managed connection is currently being used within a request,
     * as defined by JDBC 4.3 Connection.begin/endRequest.
     */
    private boolean inRequest;

    // for holding ConnectionEventListeners
    private ConnectionEventListener[] ivEventListeners;
    private int numListeners;

    // constant for the known number of ConnectionEventListeners
    // (currently the only known event listener is the connection pool)
    private static final int KNOWN_NUMBER_OF_CELS = 1; 
    private static final int CEL_ARRAY_INCREMENT_SIZE = 3; 

    WSManagedConnectionFactoryImpl mcf;
    DatabaseHelper helper;

    /**
     * A dedicated ConnectionEvent instance for this ManagedConnection.
     * 
     */
    private WSConnectionEvent connEvent = new WSConnectionEvent(this);

    //Tracks the current transaction state for this MC
    //NOTE: this is a package variable as the SPILocalTransaction will need access to it.
    WSStateManager stateMgr;

    //Transaction variables
    private LocalTransaction localTran;
    private XAResource xares;

    //Security information
    WSConnectionRequestInfoImpl cri; 
    private Subject subject;

    //Connection properties -  - These need to be kept consistent when pooling
    // connections so new connections retrieved from the pool don't inherit the previous
    // values.  The current values are determined from the underlying Connection.  The
    // requested and initially requested values are determined from the CRI.  When the MC is
    // cleaned up, the requested values should be set back to the initially requested values.
    // Default values are also added in order to account for unspecified values on the CRI.

    // defaultAutoCommit is the default autocommit value from the database.
    // currentAutoCommit is used for performance reason.
    protected boolean defaultAutoCommit, currentAutoCommit; 

    // store whether or not we're RRS transactional because     
    // getAutoCommit() will depend on this                      
    private boolean rrsTransactional = false; 

    // defaultXXX and initialXXX are used for reset the value after we put MC back to the pool
    private String defaultCatalog;
    private Map<String, Class<?>> defaultTypeMap;
    private boolean defaultReadOnly;
    private String defaultSchema, currentSchema = null;
    private Object initialShardingKey, currentShardingKey; 
    private Object initialSuperShardingKey, currentSuperShardingKey;
    private int defaultNetworkTimeout;
    public int currentNetworkTimeout = 0;

    /** Current transaction isolation level */
    private int currentTransactionIsolation;

    /** Indicates whether transaction isolation has been changed. */
    private boolean isolationChanged;

    /** Indicates whether catalog, typeMap, readOnly, schema, sharding key, super sharding key, or networkTimeout has been changed. */
    private boolean connectionPropertyChanged;

    /** current cursor holdability */
    private int currentHoldability; 

    /** Indicates whether the cursor holdability has been changed. */
    private boolean holdabilityChanged; 

    /**
     * The defaultHoldability of the connection.
     * If the JDBC driver doesn't support cursor holdability, the default value is 0.
     */
    private int defaultHoldability; 

    //Cache variables
    private CacheMap statementCache; // Switch to a WebSphere-specific hash map.  

    // Key sent to Connection wrapper to permit access to methods such as reassociate.
    static final Object key = new byte[0];

    //Trace
    private static final TraceComponent tc =
                    Tr.register(WSRdbManagedConnectionImpl.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    boolean loggingEnabled;


    // boolean to indicate if the client information has been set on this connection explictly
    public boolean clientInfoExplicitlySet;
    public boolean clientInfoImplicitlySet;

    private Properties doConnectionSetupPerTranProps;

    // Indicates if the Connection supports two phase commit.
    private boolean is2Phase;

    // Indicates whether we lazily enlisted in the current transaction. 
    boolean wasLazilyEnlistedInGlobalTran;

    /** Indicates whether we have detected a fatal Connection error on this MC. */
    private boolean connectionErrorDetected;

    /** Indicates whether we are currently cleaning up handles. */
    private boolean cleaningUpHandles;

    /** The id of the thread which may access this MC, or null if detection is disabled. */
    Object threadID; 

    /** SQLJ ConnectionContext **/
    private Object sqljContext; // sqlj.runtime.ref.DefaultContext
    protected WSJdbcConnection cachedConnection; 

    /**
     * flag to keep track of isolation level switching on connection support or not
     **/
    boolean supportIsolvlSwitching = false; 

    /**
     * The value of the fatal connection error counter of the parent ManagedConnectionFactory
     * at the time the most recent handle for this ManagedConnection was created.
     * 
     */
    private long fatalErrorCount;

    /**
     * Constructs an instance of WSRdbManagedConnectionImpl.
     * 
     * @param WSManagedConnectionFactoryImpl mcf - the ManagedConnectionFactoryImpl class which
     *            is creating the ManagedConnection.
     * @param poolConn1 the underlying PooledConnection.
     * @param java.sql.Connection conn - the physical connection to the database
     * @param sub security information.
     * @param cxRequestInfo ConnectionRequestInfo for this connection.
     * 
     * @exception ResourceException - Possible causes for this exception are:
     *                1) getAutoCommit on the physical connection failed causing a SQLException
     */

    public WSRdbManagedConnectionImpl(WSManagedConnectionFactoryImpl mcf1, 
                                      PooledConnection poolConn1, 
                                      Connection conn,
                                      Subject sub,
                                      WSConnectionRequestInfoImpl cxRequestInfo
                                      ) throws ResourceException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) 
            //cannot print subject - causes security violation 
            Tr.entry(this, tc, "<init>",
                     mcf1, 
                     AdapterUtil.toString(poolConn1), 
                     AdapterUtil.toString(conn),
                     cxRequestInfo
            );

        this.dsConfig = mcf1.dsConfig; 
        this.sqlConn = conn;
        this.poolConn = poolConn1; 
        this.mcf = mcf1; 
        this.helper = mcf1.helper; 
        this.cri = cxRequestInfo; 

        is2Phase = poolConn1 instanceof javax.sql.XAConnection; 

        if (poolConn1 == null)
        {
            if (isTraceOn && tc.isDebugEnabled()) 
            {
                if (!DataSource.class.equals(mcf.type) && !Driver.class.equals(mcf.type) && !mcf.isUCP)
                    Tr.debug(this, tc, "##### poolConn is null which will cause is2Phase to always be false and that will cause XA to break");
            }
        }
        else if (!DataSource.class.equals(mcf.type) && !Driver.class.equals(mcf.type) && !mcf.isUCP)
        {
            poolConn1.addConnectionEventListener(this); 

            if (mcf.jdbcDriverSpecVersion >= 40)
                try {
                    poolConn1.addStatementEventListener(this);
                } catch (UnsupportedOperationException supportX) {
                    // No FFDC code needed. The JDBC 4.0 driver ought to implement the
                    // addStatementEventListener method, but doesn't.  A warning is not logged because
                    // there is nothing that a customer can do in response.  It is an issue in
                    // the JDBC driver.
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(this, tc,
                                 "JDBC driver does not support addStatementEventListener");
                } catch (AbstractMethodError methErr) {
                    // No FFDC code needed. The JDBC 4.0 driver ought to implement the
                    // addStatementEventListener method, but doesn't.  A warning is not logged because
                    // there is nothing that a customer can do in response.  It is an issue in
                    // the JDBC driver.
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(this, tc,
                                 "JDBC driver does not implement addStatementEventListener");
                }
        }

        //Should also clone the Subject but it doesn't support it
        //Therefore, we will new up a Subject object and place all the properties into the Subject
        // to get a new copy of it

        subject = sub == null ? null : copySubject(sub);

        // create array.  Make room for the 'known number of ConnectionEventListeners
        // Use the standard ConnectionEventListener instead of the J2C interface 
        ivEventListeners = new ConnectionEventListener[KNOWN_NUMBER_OF_CELS];
        numListeners = 0; // Use a separate variable for the count of listeners. 

        //logWriter will not be kept here, it will be accessed directly
        // from the mcf
        //logWriter = mcf.getLogWriter();

        // Record the current thread id, for use in multithreaded access detection. 
        DSConfig config = dsConfig.get();
        threadID = config.enableMultithreadedAccessDetection ? Thread.currentThread() : threadID; 

        rrsTransactional = helper.getRRSTransactional(); 

        //  - set defaultHoldability value

        // - Since different connections of the same datasource could have different default holdability values,
        // we have to get the default holdability value for every connection.

        try {
            defaultHoldability = mcf1.getHelper().getHoldability(sqlConn); 
        } catch (SQLException sqle) {
            FFDCFilter.processException(sqle, getClass().getName() + ".init()", "300", this);
            throw AdapterUtil.translateSQLException(sqle, this, true, getClass()); 
        }

        if (isTraceOn && tc.isDebugEnabled()) 
        {
            Tr.debug(this, tc, "defaultHoldability is " + AdapterUtil.getCursorHoldabilityString(defaultHoldability));
        }

        initializeConnectionProperties();

        if (!cri.isCRIChangable())
            cri = WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri);
        cri.setDefaultValues(defaultCatalog, defaultHoldability, defaultReadOnly, defaultTypeMap, defaultSchema,
                             defaultNetworkTimeout);

        // Check to make sure a datasource configured with 'NONE (0)' is compatible with the driver being used. 
        if(config.isolationLevel == Connection.TRANSACTION_NONE) {
            try {
                if(conn.getTransactionIsolation() == Connection.TRANSACTION_NONE) {
                    //Expected behavior
                    if(isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "DataSource configured with an isolation level of 'NONE (0)' and the driver's isolation level is already 'NONE (0)'."); 
                    }
                } else {
                    throw new SQLException(AdapterUtil.getNLSMessage("DSRA4008.tran.none.unsupported", config.id));
                }
            } catch (SQLException sqle) {
                throw AdapterUtil.translateSQLException(sqle, this, true, getClass());
            }
        }
 
        //  - synchronize the connection properties
        synchronizePropertiesWithCRI();

        //Create the stmt cache if cachesize > 0
        int statementCacheSize = config.statementCacheSize; 
        if (statementCacheSize > 0) 
            statementCache = new CacheMap(statementCacheSize); 

        //create an instance of WSStateManager
        stateMgr = new WSStateManager();

        supportIsolvlSwitching =
                        helper.isIsolationLevelSwitchingSupport();

        // set logwriter for XArecovery path
        if (helper.shouldTraceBeEnabled(this))
            helper.enableJdbcLogging(this);
        else if (helper.shouldTraceBeDisabled(this))
            helper.disableJdbcLogging(this);

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "<init>");
    }

    /**
     * Add a handle to this ManagedConnection's list of handles.
     * Signal the JDBC 4.3+ driver that a request is starting.
     * 
     * @param handle the handle to add.
     * @throws ResourceException if a JDBC 4.3+ driver rejects the beginRequest operation
     */
    private final void addHandle(WSJdbcConnection handle) throws ResourceException {
        (numHandlesInUse < handlesInUse.length - 1 ? handlesInUse : resizeHandleList())[numHandlesInUse++] = handle;
        if (!inRequest && dsConfig.get().enableBeginEndRequest)
            try {
                inRequest = true;
                mcf.jdbcRuntime.beginRequest(sqlConn);
            } catch (SQLException x) {
                FFDCFilter.processException(x, getClass().getName(), "548", this);
                throw new DataStoreAdapterException("DSA_ERROR", x, getClass());
            }
    }

    /**
     * Invoked after completion of a z/OS RRS (Resource Recovery Services) global transaction.
     */
    @Override
    public void afterCompletionRRS() {
        stateMgr.setStateNoValidate(WSStateManager.NO_TRANSACTION_ACTIVE); 
        // now reset the RRA internal flag as its the only place we can set it in the cases of unsharable connections.
        // in the cases of shareable conections, the flag will be set to false twice (one here and another in cleanup, but that is ok)
        setRrsGlobalTransactionReallyActive(false); 
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting transaction state to NO_TRANSACTION_ACTIVE"); 
    }

    /**
     * Invoked by the JDBC driver when the java.sql.Connection is closed.
     * 
     * @param event a data structure containing information about the event.
     */
    public void connectionClosed(javax.sql.ConnectionEvent event) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(this, tc, "connectionClosed", "Notification of connection closed received from the JDBC driver", AdapterUtil.toString(event.getSource()));

        // We have intentionally removed our connection event listener prior to closing the
        // underlying connection. Therefore, if this event ever occurs, it indicates a
        // scenario where the connection has been closed unexpectedly by the JDBC driver
        // or database. We will treat this as a connection error.
        if(isAborted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "The connection was aborted, so this event will not be processed");
        } else {
            processConnectionErrorOccurredEvent(null, event.getSQLException());
        }
    }

    /**
     * Invoked by the JDBC driver when a fatal connection error occurs.
     * 
     * @param event a data structure containing information about the event.
     * 
     */
    public void connectionErrorOccurred(javax.sql.ConnectionEvent event) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(this, tc, "connectionErrorOccurred", "Notification of fatal connection error received from the JDBC driver.",
                     AdapterUtil.toString(event.getSource()), event.getSQLException());

        // Notify all listeners registered with the managed connection.
        if(isAborted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "The connection was aborted, so this error will not be processed");
        } else {
            processConnectionErrorOccurredEvent(null, event.getSQLException());
        }
    }

    /**
     * @return a ConnectionRequestInfo based on the currently requested values.
     */
    public final WSConnectionRequestInfoImpl createConnectionRequestInfo() throws ResourceException {
        // Reuse the original CRI if nothing has changed. Otherwise, create a new one.
        // This is needed for sharing to be done correctly. The isJDBC flag may be ignored
        // in this case since it is not involved in matching CRI objects. 
        // Use the values from the original CRI where values have been changed back to their
        // initial settings. 

        //  - go to the JDBC driver for the properties.
        // If holdabilityChanged is true, then the driver must support cursor holdability.

        // Some clarifications: 
        // *  The 'connectionPropertyChanged' field applies only to the catalog, isReadOnly,
        //    and typeMap properties.  It does not apply to holdability, autoCommit, or
        //    transaction isolation.
        // *  The 'connectionPropertyChanged' field indicates whether any of these properties
        //    have changed from their ORIGINAL, default values. It does NOT mean whether they
        //    have changed from what was requested on the last CRI--despite the fact that this
        //    is how it is being used below. This usage is apparently intended, as the only
        //    side-effects are creating extra CRI instances for less common code paths.

        try {
            WSConnectionRequestInfoImpl _criHold = null; 

            // Since the value of the shareDataSourceWithCMP is set during server startup and never
            // changes, it will be taken under account only if a new CRI is created
            if (isolationChanged || connectionPropertyChanged || holdabilityChanged || 
                cri.isCRIChangable()) // if changable then we new up a new one all the time, we do
            // do this in case more than one handle points to the same mc and after
            // they get reassociated they point to different mcs.
            {
                _criHold = new WSConnectionRequestInfoImpl(
                                cri.ivUserName,
                                cri.ivPassword,
                                isolationChanged ? currentTransactionIsolation : cri.ivIsoLevel,
                                connectionPropertyChanged && mcf.supportsGetCatalog ? getCatalog() : cri.ivCatalog,
                                connectionPropertyChanged && mcf.supportsIsReadOnly ? Boolean.valueOf(isReadOnly()) : cri.ivReadOnly,
                                connectionPropertyChanged ? currentShardingKey : cri.ivShardingKey,
                                connectionPropertyChanged ? currentSuperShardingKey : cri.ivSuperShardingKey,
                                connectionPropertyChanged && mcf.supportsGetTypeMap ? getTypeMap() : cri.ivTypeMap,
                                holdabilityChanged ? getHoldability() : cri.ivHoldability,
                                connectionPropertyChanged && mcf.supportsGetSchema ? getSchemaSafely() : cri.ivSchema,
                                connectionPropertyChanged && mcf.supportsGetNetworkTimeout ? Integer.valueOf(getNetworkTimeoutSafely()) : cri.ivNetworkTimeout,
                                cri.ivConfigID,
                                cri.supportIsolvlSwitching);

                _criHold.setDefaultValues(cri.defaultCatalog, cri.defaultHoldability, 
                                          cri.defaultReadOnly, cri.defaultTypeMap, cri.defaultSchema,
                                          cri.defaultNetworkTimeout); 

                // now set the cri as changable, otherwise, setters to follow will fail
                _criHold.markAsChangable();
            } else {
                _criHold = cri;
            }
            return _criHold; 
        } catch (SQLException sqle) {
            FFDCFilter.processException(sqle, getClass().getName() + ".createConnectionRequestInfo", "379", this);
            throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
        }
    }

    /**
     * Destroy an unwanted statement. This method should close the statement.
     * 
     * @param unwantedStatement a statement we don't want in the cache anymore.
     */
    private void destroyStatement(Object unwantedStatement) {
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, 
                         "Statement cache at capacity. Discarding a statement.",
                         AdapterUtil.toString(unwantedStatement));

            ((Statement) unwantedStatement).close();
        } catch (SQLException closeX) {
            FFDCFilter.processException(
                                        closeX, getClass().getName() + ".discardStatement", "511", this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "Error closing statement", AdapterUtil.toString(unwantedStatement), closeX);
        }
    }

    /**
     * Detect multithreaded access. This method is called only if detection is enabled.
     * The method ensures that the current thread id matches the saved thread id for this MC.
     * If the MC was just taken out the pool, the thread id may not have been recorded yet.
     * In this case, we save the current thread id. Otherwise, if the thread ids don't match,
     * log a message indicating that multithreaded access was detected. 
     */
    final void detectMultithreadedAccess() {
        Thread currentThreadID = Thread.currentThread();

        if (currentThreadID == threadID)
            return;

        if (threadID == null)
            threadID = currentThreadID;
        else {
            mcf.detectedMultithreadedAccess = true;

            java.io.StringWriter writer = new java.io.StringWriter();
            new Error().printStackTrace(new java.io.PrintWriter(writer));

            Tr.warning(tc, "MULTITHREADED_ACCESS_DETECTED",
                       this,
                       Integer.toHexString(threadID.hashCode()) + ' ' + threadID,
                       Integer.toHexString(currentThreadID.hashCode()) + ' ' + currentThreadID,
                       writer.getBuffer().delete(0, "java.lang.Error".length())
                            );
        }
    }

    /**
     * Dissociate all connection handles from this ManagedConnection, transitioning the handles
     * to an inactive state where are not associated with any ManagedConnection. Processing
     * continues when errors occur. All errors are logged, and the first error is saved to be
     * thrown when processing completes. 
     * 
     * @throws ResourceException the first error to occur while dissociating the handles.
     */
    public void dissociateConnections() throws ResourceException 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "dissociateConnections"); 

        // The first exception to occur while dissociating connection handles.
        ResourceException firstX = null;

        // Indicate that we are cleaning up handles, so we know not to send events for
        // operations done in the cleanup. 
        cleaningUpHandles = true;

        for (int i = numHandlesInUse; i > 0;)
            try 
            {
                handlesInUse[--i].dissociate(); 
                handlesInUse[i] = null; 
            } catch (ResourceException dissociationX) {
                // No FFDC code needed because the following method does any FFDC that might
                // be necessary.
                dissociationX = processHandleDissociationError(i, dissociationX);
                if (firstX == null)
                    firstX = dissociationX;
            }

        numHandlesInUse = 0; 
        cleaningUpHandles = false;

        // - need to dissociate the cachedConnection too
        // - we can't dissocate the cachedConnection because DB2 has direct access to our WSJccConnection
        // and the preparedStatement wrapper. They can't follow the normal jdbc model because the sqlj engine
        // was written by Oracle. Therefore, we put the restriction that the BMP must run in a unshareable mode
        // it caches the handle. In this case, we never need to dissociate the cached handle.
        /*
         * try
         * if (cachedConnection != null)
         * ((Reassociateable)cachedConnection).dissociate();
         * }catch (ResourceException dissociationX2)
         * // No FFDC code needed because the following method does any FFDC that might
         * // be necessary.
         * dissociationX2 = processHandleDissociationError2((Reassociateable)cachedConnection, dissociationX2);
         * if (firstX == null) firstX = dissociationX2;
         */
        if (firstX != null) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "dissociateConnections", firstX);
            throw firstX;
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "dissociateConnections");
    }

    /**
     * Enforce the autoCommit setting in the underlying database and update the current value
     * on the MC. This method must be invoked by the Connection handle before doing any work
     * on the database. 
     * 
     * @param autoCommit Indicates if the autoCommit is true or false.
     * 
     * @throws SQLException if an error occurs setting the AutoCommit. This exception
     *             is not mapped. Any mapping required is the caller's responsibility.
     */
    public final void enforceAutoCommit(boolean autoCommit) throws SQLException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "enforceAutoCommit", autoCommit); 

        // Only set values if the requested value is different from the current value, 
        // or if required as a workaround. 
        if (autoCommit != currentAutoCommit || helper.alwaysSetAutoCommit()) 
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, 
                         "currentAutoCommit: " + currentAutoCommit + " --> " + autoCommit);

            sqlConn.setAutoCommit(autoCommit);
            currentAutoCommit = autoCommit;
        }
        if (isTraceOn && tc.isEntryEnabled()) 
        {
            Tr.exit(this, tc, "enforceAutoCommit");
        }
    }

    /**
     * Invoked when enlisting in a z/OS RRS (Resource Recovery Services) global transaction.
     */
    @Override
    public void enlistRRS() {
        stateMgr.setStateNoValidate(WSStateManager.RRS_GLOBAL_TRANSACTION_ACTIVE); 
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting transaction state to RRS_GLOBAL_TRANSACTION_ACTIVE"); 
    }

    /**
     * @return the current value of the catalog property.
     */
    public final String getCatalog() throws SQLException 
    {
        return sqlConn.getCatalog();
    }

    /**
     * @return the current value of the network timeout property. 0 if unsupported.
     */
    public final int getCurrentNetworkTimeout() {
        return currentNetworkTimeout;
    }

    /**
     * @return the current value of the schema property. Null if unsupported.
     */
    public final String getCurrentSchema() {
        return currentSchema;
    }

    /**
     * @return the default autocommit value of the connection.
     */
    public final boolean getDefaultAutoCommit() {
        return defaultAutoCommit;
    }

    /**
     * @return the number of handles associated with this ManagedConnection. 
     */
    public final int getHandleCount() {
        return numHandlesInUse; 
    }

    /**
     * Processes any dynamic updates to the statement cache size
     * and then returns the statement cache.
     * 
     * @return the statement cache. Null if caching is not enabled.
     */
    private CacheMap getStatementCache() {
        int newSize = dsConfig.get().statementCacheSize; 

        // Check if statement cache is dynamically enabled
        if (statementCache == null && newSize > 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc,
                         "enable statement cache with size", newSize);
            statementCache = new CacheMap(newSize);
        }

        // Check if statement cache is dynamically resized or disabled
        else if (statementCache != null && statementCache.getMaxSize() != newSize) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc,
                         "resize statement cache to", newSize);
            CacheMap oldCache = statementCache;
            statementCache = newSize > 0 ? new CacheMap(newSize) : null;
            Object[] discards = newSize > 0 ? statementCache.addAll(oldCache) : oldCache.removeAll();
            for (Object stmt : discards)
                destroyStatement(stmt);
        }

        return statementCache;
    }

    /**
     * @return the current value of the typeMap property.
     */
    public final Map<String, Class<?>> getTypeMap() throws SQLException 
    {
        return sqlConn.getTypeMap();
    }
    
    public Map<String, Class<?>> getTypeMapSafely() throws SQLException {
        if(!mcf.supportsGetTypeMap)
            return defaultTypeMap;
        
        try{
            return getTypeMap();
        } catch (SQLException e) {
            if (helper.isUnsupported(e)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "supportsGetTypeMap false due to " + e);
                mcf.supportsGetTypeMap = false;
                return defaultTypeMap;
            } else {
                throw e;
            }
        }
    }

    /**
     * @return true if the ManagedConnection is enlisted in a global transaction, otherwise
     *         false.
     */
    public final boolean inGlobalTransaction() {
        // If we have a one-phase resource being used in a global transaction, then the
        // ManagedConnection state will be local transaction active.  So we need to check the
        // interaction pending event status, which, when deferred enlistment is used, signals
        // whether a global transaction is active. 

        // This method should be used only in situations where normal Global Trans AND RRS              
        // Global Trans are handled in the same way.  If RRS Global Trans should be handled             
        // differently from a normal Global Tran, the calling method should perform a                   
        // switch on each distinct case.                                                                

        int state = stateMgr.transtate;
        
        boolean inGlobalTran = state == WSStateManager.GLOBAL_TRANSACTION_ACTIVE
                        || state == WSStateManager.RRS_GLOBAL_TRANSACTION_ACTIVE
                        || (wasLazilyEnlistedInGlobalTran && state == WSStateManager.LOCAL_TRANSACTION_ACTIVE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
        {
            Tr.debug(this, tc, "In Global Transaction: " + inGlobalTran + ". Transaction state = " + getTransactionStateAsString() + ", wasLazilyEnlistedInGlobalTran = " + wasLazilyEnlistedInGlobalTran + ", rrsGlobalTransactionReallyActive = " + rrsGlobalTransactionReallyActive);
        }

        return inGlobalTran;
    }

    /**
     * Initialize all Connection properties for the ManagedConnection. The current values (and
     * previous values if applicable) should be retrieved from the underlying connection.
     * Requested values and initially requested values should be taken from the
     * ConnectionRequestInfo.
     * 
     * @throws ResourceException if an error occurs retrieving default values.
     */
    private void initializeConnectionProperties() throws ResourceException {
        try {
            // Retrieve the default values for all Connection properties. 
            // Save the default values for when null is specified in the CRI. 

            // - get the default values from the JDBC driver
            if (rrsTransactional) 
            { 
                currentAutoCommit = defaultAutoCommit = true; 
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                { 
                    Tr.debug(this, tc, "MCF is rrsTransactional:  forcing currentAutoCommit and defaultAutoCommit to true"); 
                } 
            } 
            else 
            { 
                currentAutoCommit = defaultAutoCommit = sqlConn.getAutoCommit();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                { 
                    Tr.debug(this, tc, "MCF is NOT rrsTransactional:  setting currentAutoCommit and defaultAutoCommit to " + defaultAutoCommit + " from underlying Connection"); 
                } 
            } 
            defaultCatalog = mcf.supportsGetCatalog ? sqlConn.getCatalog() : null;
            defaultReadOnly = mcf.supportsIsReadOnly ? sqlConn.isReadOnly() : false;
            defaultTypeMap = getTypeMapSafely();
            currentShardingKey = initialShardingKey = cri.ivShardingKey;
            currentSuperShardingKey = initialSuperShardingKey = cri.ivSuperShardingKey;
            currentSchema = defaultSchema = getSchemaSafely();
            currentNetworkTimeout = defaultNetworkTimeout = getNetworkTimeoutSafely();
            currentHoldability = defaultHoldability; 

            currentTransactionIsolation = sqlConn.getTransactionIsolation(); 

        } catch (SQLException sqlX) {
            FFDCFilter.processException(sqlX,
                                        getClass().getName() + ".initializeConnectionProperties", "381", this);
            throw new DataStoreAdapterException("DSA_ERROR", sqlX, getClass());
        }
    }

    /**
     * @return relevant FFDC information for this class, formatted as a String array.
     */
    public String[] introspectSelf() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "introspectSelf"); 

        com.ibm.ws.rsadapter.FFDCLogger info = new com.ibm.ws.rsadapter.FFDCLogger(this);

        info.append(is2Phase ? "TWO PHASE ENABLED" : "ONE PHASE ENABLED");

        info.append("Connection sharing: ", connectionSharing); 
        info.append("Transaction State:", getTransactionStateAsString()); 
        info.append("Key:", key);
        info.append("Log Writer:", mcf == null ? null : mcf.logWriter); 
        info.append("Subject:", subject == null ? null : "NON-NULL"); 
        info.append("ManagedConnection:", this); 
        info.append("Counter of fatal connection errors for the ManagedConnectionFactory " +
                    "as of the most recent getConnection on this ManagedConnection:",
                    fatalErrorCount); 

        info.append("Default AutoCommit:", defaultAutoCommit); 
        info.append("Current AutoCommit:", currentAutoCommit); 

        info.append("Current Isolation:",
                    AdapterUtil.getIsolationLevelString(currentTransactionIsolation));

        info.append("Isolation level has changed? :", 
                    isolationChanged); 

        info.append("Support isolation level switching: ", supportIsolvlSwitching); 

        info.append("The gssCredential is: ").append(mc_gssCredential == null ? null : mc_gssCredential.toString());
        info.append("The gssName is: ").append(mc_gssName == null ? null : mc_gssName.toString());

        info.append("Catalog, IsReadOnly, TypeMap, Schema, ShardingKey, SuperShardingKey, or NetworkTimeout has changed? :", 
                    connectionPropertyChanged ? Boolean.TRUE : Boolean.FALSE); 

        info.append("Default Holdability:",
                    AdapterUtil.getCursorHoldabilityString(defaultHoldability)); 

        info.append("Current Holdability:", AdapterUtil.getCursorHoldabilityString(currentHoldability));

        info.append("Holdability value has changed? :", 
                    holdabilityChanged ? Boolean.TRUE : Boolean.FALSE); 

        info.append("Thread ID:", threadID);

        info.append("Lazily enlisted in the current transaction? :", 
                    wasLazilyEnlistedInGlobalTran ? Boolean.TRUE : Boolean.FALSE);

        info.append("Underlying Connection Object: " + AdapterUtil.toString(sqlConn),
                    sqlConn);

        info.append("Underlying PooledConnection Object: " + AdapterUtil.toString(poolConn),
                    poolConn);

        info.append("SQLJ Default Context: " + AdapterUtil.toString(sqljContext),
                    sqljContext); 


        info.append("Fatal connection error was detected? :", 
                    connectionErrorDetected ? Boolean.TRUE : Boolean.FALSE); 

        info.append("Currently cleaning up handles? :", 
                    cleaningUpHandles ? Boolean.TRUE : Boolean.FALSE); 

        info.append("Last ConnectionEvent sent for this ManagedConnection:"); 
        info.indent(AdapterUtil.toString(connEvent)); 
        info.indent("Connection Handle: " + connEvent.getConnectionHandle()); 
        info.indent("Event ID: " + AdapterUtil.getConnectionEventString(connEvent.getId())); 
        info.indent("Exception: " + connEvent.getException()); 
        info.eoln(); 

        info.append("Connection Event Listeners:");

        for (int i = 0; i < numListeners; i++)
            try {
                info.indent(ivEventListeners[i]);
            } catch (ArrayIndexOutOfBoundsException arrayX) {
                // No FFDC code needed; multithreaded issue during FFDC; just ignore.
            }

        info.eoln();

        info.append("Maximum Handle List Size: " + maxHandlesInUse); 
        info.append("Handle Count: " + numHandlesInUse); 
        info.append("Handles:"); 

        if (handlesInUse != null)
            try {
                for (int i = 0; i < handlesInUse.length; i++)
                    info.indent(handlesInUse[i]); 
            } catch (Throwable th) {
                // No FFDC code needed; multithreaded issue during FFDC; just ignore.
            }

        info.eoln();

        info.introspect("State Manager:", stateMgr);

        try { // FFDC info for XAResource
            if (xares instanceof WSRdbXaResourceImpl)
                ((WSRdbXaResourceImpl) xares).introspectThisClassOnly(info);

            else if (xares instanceof WSRdbOnePhaseXaResourceImpl)
                ((WSRdbOnePhaseXaResourceImpl) xares).introspectThisClassOnly(info);

            else
                // Not a known XAResource class, or null.
                info.append("XA Resource:", xares);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; multithreaded issue during FFDC; just ignore.
        }

        if (localTran == null)
            info.append("SPI LocalTransaction :", "null");

        else
            try {
                ((WSRdbSpiLocalTransactionImpl) localTran).introspectThisClassOnly(info);
            } catch (NullPointerException nullX) {
                // No FFDC code needed; multithreaded issue during FFDC; just ignore.
            }

        // Switch to a WebSphere-specific hash map for statement caching.
        // The 'display' method should contain all the information for the cache that we need.
        // This method, however, is not synchronized, so it is necessary to trap for
        // NullPointerException/ArrayIndexOutOfBoundsException. 

        if (statementCache == null)
            info.append("Statement Cache:", "null");

        else
            try {
                info.append("Statement Cache:", statementCache.display());
            } catch (Exception ex) {
                // No FFDC code needed; multithreaded issue during FFDC; just ignore.
            }

        info.introspect("ConnectionRequestInfo", cri);
        info.introspect("ManagedConnectionFactory", mcf);

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "introspectSelf"); 

        return info.toStringArray();
    }

    /**
     * Returns true if a global transaction is active on the thread, otherwise false.
     * 
     * @return true if a global transaction is active on the thread, otherwise false.
     */
    public final boolean isGlobalTransactionActive() {
        UOWCurrent uow = (UOWCurrent) mcf.connectorSvc.getTransactionManager();
        UOWCoordinator coord = uow == null ? null : uow.getUOWCoord();
        return coord != null && coord.isGlobal();
    }

    /**
     * This method checks if transaction enlistment is enabled on the MC
     * 
     * @return true if transaction enlistment is enabled and supported, false otherwise.
     */
    public final boolean isTransactional() {
        // Take a snapshot of the value with first use (or reuse from pool) of the managed connection.
        // This value will be cleared when the managed connection is returned to the pool.
        if (transactional == null) {
            transactional = mcf.dsConfig.get().transactional;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "transactional=", transactional);
        }

        return transactional;
    }

    /**
     * @return the current value of the readOnly property.
     */
    public final boolean isReadOnly() throws SQLException 
    {
        return sqlConn.isReadOnly();
    }

    /**
     * Processes any dynamic updates to the statement cache size
     * and then determines if statement caching is enabled.
     * 
     * @return true if statement caching is enabled, otherwise false.
     */
    public final boolean isStatementCachingEnabled() {
        return getStatementCache() != null;
    }

    /**
     * Process request for a CONNECTION_CLOSED event.
     * 
     * @param handle the Connection handle requesting to fire the event.
     * 
     * @throws ResourceException if an error occurs processing the request.
     */
    public void processConnectionClosedEvent(WSJdbcConnection handle) 
    throws ResourceException {
        //A connection handle was closed - must notify the connection manager
        // of the close on the handle.  JDBC connection handles
        // which are closed are not allowed to be reused because there is no
        // guarantee that the user will not try to reuse an already closed JDBC handle.

        // Only send the event if the application is requesting the close. 
        if (cleaningUpHandles)
            return;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        // Fill in the ConnectionEvent only if needed. 
        connEvent.recycle(ConnectionEvent.CONNECTION_CLOSED, null, handle);

        if (isTraceOn && tc.isEventEnabled())
            Tr.event(this, tc, "Firing CONNECTION CLOSED", handle);

        try {
            removeHandle(handle); 

            // - Remove resetting autocommit in connection close time
        } catch (NullPointerException nullX) {
            // No FFDC code needed.  ManagedConnection is already closed.

            // A ConnectionError situation may
            // trigger a ManagedConnection close before the handle sends the ConnectionClosed
            // event.  When the event is sent the ManagedConnection is already closed.  If so,
            // just do a no-op here.
            if (handlesInUse == null) {
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(this, tc, "ManagedConnection already closed"); 
                return;
            } else
                throw nullX;
        }

        if (numHandlesInUse == 0) 
        {
            if (haveVendorConnectionPropertiesChanged) {
                try {

                    helper.doConnectionVendorPropertyReset(this.sqlConn, CONNECTION_VENDOR_DEFAULT_PROPERTIES);
                } catch (SQLException sqle) {
                    FFDCFilter.processException(sqle, getClass().getName(), "1905", this);
                    throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
                }

                haveVendorConnectionPropertiesChanged = false;
            }
        } 

        // loop through the listeners
        // Not synchronized because of contract that listeners will only be changed on
        // ManagedConnection create/destroy. 

        for (int i = 0; i < numListeners; i++) {
            // send Connection Closed event to the current listener
            ivEventListeners[i].connectionClosed(connEvent); 
        }

        // Replace ConnectionEvent caching with a single reusable instance per
        // ManagedConnection. 
    }

    /**
     * This method handles the processing of any errors encountered during handle dissociation.
     * In certain cases (for example, when a handle cannot be dissociated because it is
     * currently in use) the dissociation error should be ignored and other work attempted.
     * This method handles such situations. 
     * 
     * @param handleIndex the index of the handle on which the dissociation error occurred.
     * @param dissociationX the dissociation error.
     * 
     * @return the dissociation exception which should be thrown for this error, or null if no
     *         exception should be thrown.
     */
    private ResourceException processHandleDissociationError(int handleIndex,
                                                             ResourceException dissociationX) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "processHandleDissociationError", handleIndex, dissociationX.getMessage()); 

        WSJdbcConnection handle = handlesInUse[handleIndex]; 

        // If we receive an error for dissociating a handle which is currently in use,
        // just close the handle instead , provided we can still access it.

        String errCode = dissociationX.getErrorCode(); 
        if ((errCode != null && errCode.equals("HANDLE_IN_USE")) && handle != null)
            try 
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, 
                             "Unable to dissociate handle because it is doing work in the database.  " +
                                             "Closing it instead.",
                             handlesInUse[handleIndex]);

                // This is a JDBC specific error, so we can cast to java.sql.Connection.
                ((java.sql.Connection) handle).close();

                // If the handle close is successful, this situation is NOT considered an error.
                dissociationX = null;
            } catch (SQLException closeX) {
                // No FFDC code needed here because we do it below.
                dissociationX = new DataStoreAdapterException("DSA_ERROR", closeX, getClass(), closeX);
            }

        if (dissociationX != null) {
            FFDCFilter.processException(dissociationX,
                                        getClass().getName() + ".processHandleDissociationError", "1024", this);

            // The connection handle already signals a ConnectionError event on any fatal
            // error on dissociation, so there is no need to check if the exception maps to
            // connection error.

            if (isTraceOn && tc.isEventEnabled())
                Tr.event(this, tc, 
                         "Error dissociating handle. Continuing...", handle);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "processHandleDissociationError", 
                    dissociationX == null ? null : dissociationX.getMessage());

        return dissociationX;
    }

    /**
     * Process request for a LOCAL_TRANSACTION_STARTED event.
     * 
     * @param handle the Connection handle requesting the event.
     * 
     * @throws ResourceException if an error occurs starting the local transaction, or if the
     *             state is not valid.
     */
    public void processLocalTransactionStartedEvent(Object handle) throws ResourceException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn) 
        {
            if (tc.isEntryEnabled()) 
                Tr.entry(this, tc, "processLocalTransactionStartedEvent", handle);

            if (tc.isDebugEnabled()) 
            { 
                String cId = null;
                try {
                    cId = mcf.getCorrelator(this); 
                } catch (SQLException x) {
                    // will just log the exception here and ignore it since its in trace
                    Tr.debug(this, tc, "got an exception trying to get the correlator in commit, exception is: ", x);
                }

                if (cId != null) {
                    StringBuffer stbuf = new StringBuffer(200);
                    stbuf.append("Correlator: DB2, ID: ");
                    stbuf.append(cId);
                    if (xares != null) {
                        stbuf.append(" Transaction : ");
                        stbuf.append(xares);
                    }
                    stbuf.append(" BEGIN");

                    Tr.debug(this, tc, stbuf.toString());
                }
            } 
        } 

        // An application level local transaction has been requested started

        //The isValid method returns an exception if it is not valid.  This allows the
        // WSStateManager to create a more detailed message than this class could.

        ResourceException re = stateMgr.isValid(WSStateManager.LT_BEGIN);
        if (re == null) {
            if (currentAutoCommit) {
                try {
                    if (isTraceOn && tc.isDebugEnabled()) 
                        Tr.debug(this, tc, "current autocommit is true, set to false");
                    setAutoCommit(false);
                } catch (SQLException sqle) {
                    FFDCFilter.processException(sqle, getClass().getName() + ".processLocalTransactionStartedEvent",
                                                "550", this);
                    throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
                }
            }
            // Already validated the state so just set it. 
            stateMgr.transtate = WSStateManager.LOCAL_TRANSACTION_ACTIVE;
        } else {
            throw re;
        }

        // Fill in the ConnectionEvent only if needed. 
        connEvent.recycle(ConnectionEvent.LOCAL_TRANSACTION_STARTED, null, handle);

        if (isTraceOn && tc.isEventEnabled())
            Tr.event(this, tc, 
                     "Firing LOCAL TRANSACTION STARTED event for: " + handle, this);

        //Notification of the eventListeners must happen after the state change because if the statechange
        // is illegal, we need to throw an exception.  If this exception occurs, we do not want to
        // notify the cm of the tx started because we are not allowing it to start.
        // loop through the listeners
        for (int i = 0; i < numListeners; i++) {
            // send Local Transaction Started event to the current listener
            ivEventListeners[i].localTransactionStarted(connEvent); 
        }

        // Replace ConnectionEvent caching with a single reusable instance per
        // ManagedConnection. 

        if (isTraceOn && tc.isEntryEnabled()) 
        {
            Tr.exit(this, tc, "processLocalTransactionStartedEvent", handle);
        }

    }

    /**
     * Process request for a LOCAL_TRANSACTION_COMMITTED event.
     * 
     * @param handle the Connection handle requesting to send an event.
     * 
     * @throws ResourceException if an error occurs committing the transaction or the state is
     *             not valid.
     */
    public void processLocalTransactionCommittedEvent(Object handle) throws ResourceException {
        // A application level local transaction has been committed.

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn) 
        {
            if (tc.isEntryEnabled()) 
                Tr.entry(this, tc, "processLocalTransactionCommittedEvent", handle);

            if (tc.isDebugEnabled()) 
            { 
                String cId = null;
                try {
                    cId = mcf.getCorrelator(this); 
                } catch (SQLException x) {
                    // will just log the exception here and ignore it since its in trace
                    Tr.debug(this, tc, "got an exception trying to get the correlator in commit, exception is: ", x);
                }

                if (cId != null) {
                    StringBuffer stbuf = new StringBuffer(200);
                    stbuf.append("Correlator: DB2, ID: ");
                    stbuf.append(cId);
                    if (xares != null) {
                        stbuf.append(" Transaction : ");
                        stbuf.append(xares);
                    }
                    stbuf.append(" COMMIT");
                    Tr.debug(this, tc, stbuf.toString());
                }
            } 
        }

        ResourceException re = stateMgr.isValid(WSStateManager.LT_COMMIT);
        if (re == null) {
            // If no work was done during the transaction, the autoCommit value may still be
            // on.  In this case, just no-op, since some drivers like ConnectJDBC 3.1 don't
            // allow commit/rollback when autoCommit is on.  

            if (!currentAutoCommit)
                try { // autoCommit is off
                    sqlConn.commit();
                } catch (SQLException se) {
                    FFDCFilter.processException(se, "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.processLocalTransactionCommittedEvent", "554", this);
                    throw AdapterUtil.translateSQLException(se, this, true, getClass());
                }

            //Set the state only after the commit has succeeded.  Else, we change the
            // state but it has not yet been committed. - a real mess

            // Already validated the state so just set it. 
            stateMgr.transtate = WSStateManager.NO_TRANSACTION_ACTIVE;
        } else {
            throw re;
        }

        // Fill in the ConnectionEvent only if needed. 
        connEvent.recycle(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED, null, handle);

        if (isTraceOn && tc.isEventEnabled())
            Tr.event(this, tc, 
                     "Firing LOCAL TRANSACTION COMMITTED event for: " + handle,
                     this);

        // loop through the listeners
        for (int i = 0; i < numListeners; i++) {
            // send Local Transaction Committed event to the current listener
            ivEventListeners[i].localTransactionCommitted(connEvent); 
        }

        // Reset the indicator so lazy enlistment will be signaled if we end up in a
        // Global Transaction. 
        wasLazilyEnlistedInGlobalTran = false;

        // Replace ConnectionEvent caching with a single reusable instance per
        // ManagedConnection. 

        if (isTraceOn && tc.isEntryEnabled()) 
        {
            Tr.exit(this, tc, "processLocalTransactionCommittedEvent");
        }
    }

    /**
     * Process request for a LOCAL_TRANSACTION_ROLLEDBACK event.
     * 
     * @param handle the Connection handle requesting to send an event.
     * 
     * @throws ResourceException if an error occurs rolling back the transaction or the state
     *             is not valid.
     */
    public void processLocalTransactionRolledbackEvent(Object handle)
                    throws ResourceException {
        //A CCILocalTransaction has been rolledback

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn) 
        {
            if (tc.isEntryEnabled()) 
                Tr.entry(this, tc, "processLocalTransactionRolledbackEvent");

            if (tc.isDebugEnabled()) 
            { 
                String cId = null;
                try {
                    cId = mcf.getCorrelator(this); 
                } catch (SQLException x) {
                    // will just log the exception here and ignore it since its in trace
                    Tr.debug(this, tc, "got an exception trying to get the correlator in commit, exception is: ", x);
                }

                if (cId != null) {
                    StringBuffer stbuf = new StringBuffer(200);
                    stbuf.append("Correlator: DB2, ID: ");
                    stbuf.append(cId);
                    if (xares != null) {
                        stbuf.append(" Transaction : ");
                        stbuf.append(xares);
                    }
                    stbuf.append(" ROLLBACK");
                    Tr.debug(this, tc, stbuf.toString());
                }
            } 
        }


        ResourceException re = stateMgr.isValid(WSStateManager.LT_ROLLBACK);
        if (re == null) {
            // If no work was done during the transaction, the autoCommit value may still be
            // on.  In this case, just no-op, since some drivers like ConnectJDBC 3.1 don't
            // allow commit/rollback when autoCommit is on.  

            if (!currentAutoCommit)
                try { // autoCommit is off
                    sqlConn.rollback();
                } catch (SQLException se) {
                    FFDCFilter.processException(se, "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.processLocalTransactionRolledbackEvent", "595", this);
                    throw AdapterUtil.translateSQLException(se, this, true, getClass());
                }

            //Set the state only after the rollback has succeeded.  Else, we change the
            // state but it has not be rolledback - a real mess.

            // Already validated the state so just set it. 
            stateMgr.transtate = WSStateManager.NO_TRANSACTION_ACTIVE;
        } else {
            throw re;
        }

        // Fill in the ConnectionEvent only if needed. 
        connEvent.recycle(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK, null, handle);

        if (isTraceOn && tc.isEventEnabled())
            Tr.event(this, tc, 
                     "Firing LOCAL TRANSACTION ROLLEDBACK event for: " + handle, this);

        // loop through the listeners
        for (int i = 0; i < numListeners; i++) {
            // send Local Transaction Rolledback event to the current listener
            ivEventListeners[i].localTransactionRolledback(connEvent); 
        }

        // Reset the indicator so lazy enlistment will be signaled if we end up in a
        // Global Transaction. 
        wasLazilyEnlistedInGlobalTran = false;

        // Replace ConnectionEvent caching with a single reusable instance per
        // ManagedConnection. 

        if (isTraceOn && tc.isEntryEnabled()) 
        {
            Tr.exit(this, tc, "processLocalTransactionRolledbackEvent");
        }
    }

    //  Modified to call processConnectionErrorOccurredEvent with logEvent=true to
    //           maintain default behavior of this method
    /**
     * Process request for a CONNECTION_ERROR_OCCURRED event.
     * 
     * @param event the Connection handle requesting to send the event.
     * @param ex the exception which indicates the connection error, or null if no exception.
     */
    public void processConnectionErrorOccurredEvent(Object handle, Exception ex) {
        processConnectionErrorOccurredEvent(handle, ex, true);
    }

    //  - Added logEvent parm to this method.  This provides a means to specify
    // whether the connection error will be dumped to system out or not.  The trigger
    // is the event ID that is given to the connection manager.
    /**
     * Process request for a CONNECTION_ERROR_OCCURRED event.
     * 
     * @param event the Connection handle requesting to send the event.
     * @param ex the exception which indicates the connection error, or null if no exception.
     * @param logEvent fire a logging or non-logging event to be interpreted by the connection manager.
     */
    public void processConnectionErrorOccurredEvent(Object handle, Exception ex, boolean logEvent) {
        // Method is not synchronized because of the contract that add/remove event
        // listeners will only be used on ManagedConnection create/destroy, when the
        // ManagedConnection is not used by any other threads. 

        // Some object using the physical jdbc connection has received a SQLException that
        // when translated to a ResourceException is determined to be a connection event error.
        // The SQLException is mapped to a StaleConnectionException in the
        // helper.  SCE's will (almost) always be connection errors.

        // Track whether a fatal Connection error was detected. 
        // Technically, the Connection Manager is required to be able to handle duplicate
        // events, but since we already have a flag for the occasion, we'll be nice and skip
        // the unnecessary event when convenient.

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (connectionErrorDetected) {
            if (isTraceOn && tc.isEventEnabled())
                Tr.event(this, tc, "CONNECTION_ERROR_OCCURRED event already fired");

            return;
        }

        if (ex instanceof SQLException && helper.isAnAuthorizationException((SQLException) ex)) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "CONNECTION_ERROR_OCCURRED will fire an event to only purge and destroy this connection");

            connectionErrorDetected = true;

            closeHandles();

            // Create a Connection Error Event with the given SQLException.
            // Reuse a single ConnectionEvent instance.
            //  - Modified to use J2C defined event.
            connEvent.recycle(WSConnectionEvent.SINGLE_CONNECTION_ERROR_OCCURRED, ex, handle);

            if (isTraceOn && tc.isEventEnabled())
                Tr.event(this, tc, "Firing Single CONNECTION_ERROR_OCCURRED", handle);

            // loop through the listeners
            for (int i = 0; i < numListeners; i++) {
                // send Connection Error Occurred event to the current listener
                ivEventListeners[i].connectionErrorOccurred(connEvent); 
            }

            return;
        }

        mcf.fatalErrorCount.incrementAndGet();

        if (mcf.oracleRACXARetryDelay > 0l) 
            mcf.oracleRACLastStale.set(System.currentTimeMillis()); 

        connectionErrorDetected = true;

        // The connectionErrorDetected indicator is no longer required for ManagedConnection
        // cleanup since we are now required to invalidate all handles at that point
        // regardless of whether a connection error has occurred. 

        // Close all active handles for this ManagedConnection, since we cannot rely on the
        // ConnectionManager to request cleanup/destroy immediately.  The ConnectionManager is
        // required to wait until the transaction has ended. 

        closeHandles(); 

        // Create a Connection Error Event with the given SQLException.
        // Reuse a single ConnectionEvent instance. 
        //  - Fire the normal logging event if logEvent == true.  Otherwise, fire the non-logging connection error event.
        connEvent.recycle((logEvent ? ConnectionEvent.CONNECTION_ERROR_OCCURRED : WSConnectionEvent.CONNECTION_ERROR_OCCURRED_NO_EVENT),
                          ex, handle);

        if (isTraceOn && tc.isEventEnabled())
            Tr.event(this, tc, 
                     "Firing " + (logEvent ? "CONNECTION_ERROR_OCCURRED" : "CONNECTION_ERROR_OCCURRED_NO_EVENT"), handle);

        // loop through the listeners
        for (int i = 0; i < numListeners; i++) {
            // send Connection Error Occurred event to the current listener
            ivEventListeners[i].connectionErrorOccurred(connEvent); 
        }

        // Replace ConnectionEvent caching with a single reusable instance per
        // ManagedConnection. 
    }

    /**
     * Invoked by the JDBC driver when a prepared statement is closed.
     * 
     * @param event a data structure containing information about the event.
     * 
     */
    public void statementClosed(javax.sql.StatementEvent event) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(this, tc, "statementClosed",
                      "Notification of statement closed received from the JDBC driver",
                      AdapterUtil.toString(event.getSource()),
                      AdapterUtil.toString(event.getStatement())
                            );

        // Statement.close is used instead of these signals.
    }

    /**
     * Invoked by the JDBC driver when a fatal statement error occurs.
     * 
     * @param event a data structure containing information about the event.
     * 
     */
    public void statementErrorOccurred(StatementEvent event) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "statementErrorOccurred",
                      "Notification of a fatal statement error received from the JDBC driver",
                      AdapterUtil.toString(event.getSource()),
                      AdapterUtil.toString(event.getStatement()),
                      event.getSQLException()
                            );

        for (int i = 0; i < numHandlesInUse; i++)
            ((WSJdbcConnection) handlesInUse[i]).setPoolableFlag(event.getStatement(), false);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "statementErrorOccurred");
    }

    /**
     * Signal the Application Server for lazy enlistment if we aren't already enlisted in a
     * transaction. The lazy enlistment signal should only be sent once for a transaction.
     * Connection handles will always invoke this method when doing work in the database,
     * regardless of whether we are already enlisted. In the case where we are already
     * enlisted, this request should be ignored.
     * 
     * @param lazyEnlistableConnectionManager a ConnectionManager capable of lazy enlistment.
     * 
     * @throws ResourceException if an error occurs signaling for lazy enlistement.
     */
    public void lazyEnlistInGlobalTran(LazyEnlistableConnectionManager lazyEnlistableConnectionManager)
                    throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn) {
            if (tc.isEntryEnabled())
                Tr.entry(this, tc, "lazyEnlist", lazyEnlistableConnectionManager);
            if (tc.isDebugEnabled()) 
            {
                String cId = null;
                try {
                    cId = mcf.getCorrelator(this); 
                } catch (SQLException x) {
                    // will just log the exception here and ignore it since its in trace
                    Tr.debug(this, tc, "got an exception trying to get the correlator in commit, exception is: ", x);
                }

                if (cId != null) {
                    StringBuffer stbuf = new StringBuffer(200);
                    stbuf.append("Correlator: DB2, ID: ");
                    stbuf.append(cId);
                    if (xares != null) {
                        // doing this because otherwise, we will hvae an extra Begin that does't match commit/rollback
                        stbuf.append(" Transaction : ");
                        stbuf.append(xares);
                        stbuf.append(" BEGIN");
                    }
                    Tr.debug(this, tc, stbuf.toString());
                }
            }
        }

        // Signal the ConnectionManager directly to lazily enlist.

        if (wasLazilyEnlistedInGlobalTran) // Already enlisted; don't need to do anything.
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "lazyEnlist", "already enlisted");
        } else {
            lazyEnlistableConnectionManager.lazyEnlist(this);

            // Indicate we lazily enlisted in the current transaction, if so.
            wasLazilyEnlistedInGlobalTran |= stateMgr.transtate != WSStateManager.NO_TRANSACTION_ACTIVE;

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "lazyEnlist", wasLazilyEnlistedInGlobalTran);
        }
    }

    /**
     * After XAResource.end, Oracle resets the autocommit value to whatever it was before the transaction
     * instead of leaving it as the value that the application set during the transaction.
     * Refresh our cached copy of the autocommit value to be consistent with the JDBC driver's behavior.
     */
    void refreshCachedAutoCommit()  {
        try {
            boolean autoCommit = sqlConn.getAutoCommit();
            if (currentAutoCommit != autoCommit) {
                currentAutoCommit = autoCommit;
                for (int i = 0; i < numHandlesInUse; i++)
                    handlesInUse[i].setCurrentAutoCommit(autoCommit, key);
            }
        } catch (SQLException x) {
            // Mark the connection stale and close all handles if we cannot accurately determine the autocommit value.
            processConnectionErrorOccurredEvent(null, x);
        }
    }

    /**
     * Remove a handle from the list of handles associated with this ManagedConnection.
     * 
     * @param handle the handle to remove from the list.
     * 
     * @return true if we removed the requested handle, otherwise false.
     * 
     */
    private final boolean removeHandle(WSJdbcConnection handle) {
        // Find the handle in the list and remove it.

        for (int i = numHandlesInUse; i > 0;)
            if (handle == handlesInUse[--i]) {
                // Once found, the handle is removed by replacing it with the last handle in the
                // list and nulling out the previous entry for the last handle.

                handlesInUse[i] = handlesInUse[--numHandlesInUse];
                handlesInUse[numHandlesInUse] = null;
                return true;
            }

        // The handle wasn't found in the list.

        return false;
    }

    /**
     * Replace the CRI of this ManagedConnection with the new CRI.
     * 
     * @param newCRI the new CRI.
     * 
     * @throws ResourceException if handles already exist on the ManagedConnection or if the
     *             requested CRI contains a different user, password, or DataSource configuration
     *             than the existing CRI.
     */
    private void replaceCRI(WSConnectionRequestInfoImpl newCRI) throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "replaceCRI", "Current:", cri, "New:", newCRI);

        if (numHandlesInUse > 0 || !cri.isReconfigurable(newCRI, false))
        {
            if (numHandlesInUse > 0) 
            {
                ResourceException resX = new DataStoreAdapterException("WS_INTERNAL_ERROR",
                                                                       null,
                                                                       getClass(),
                                                                       "ConnectionRequestInfo cannot be changed on a ManagedConnection with active handles.",
                                                                       AdapterUtil.EOLN + "Existing CRI: " + cri,
                                                                       AdapterUtil.EOLN + "Requested CRI: " + newCRI);

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "replaceCRI", resX); 
                throw resX;
            } else // Users, passwords, or DataSource configurations do not match. 
            {
                ResourceException resX = new DataStoreAdapterException("WS_INTERNAL_ERROR",
                                                                       null,
                                                                       getClass(),
                                                                       "ConnectionRequestInfo cannot be changed because the users, passwords, or DataSource configurations do not match.",
                                                                       AdapterUtil.EOLN + "Existing CRI: " + cri,
                                                                       AdapterUtil.EOLN + "Requested CRI: " + newCRI);

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "replaceCRI", resX); 
                throw resX;
            }
        }

        // The ManagedConnection should use the new CRI value in place of its current CRI.

        if (!newCRI.isCRIChangable())
            newCRI = WSConnectionRequestInfoImpl.createChangableCRIFromNon(newCRI);
        newCRI.setDefaultValues(defaultCatalog, defaultHoldability, defaultReadOnly, defaultTypeMap, defaultSchema,
                                defaultNetworkTimeout);

        cri = newCRI;

        //  -- don't intialize the properties, deplay to synchronizePropertiesWithCRI().

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "replaceCRI"); 
    }

    /**
     * Copy from replaceCRI: Replace the CRI of this ManagedConnection with the new CRI for
     * CCI interface
     * 
     * @param newCRI the new CRI.
     * 
     * @throws ResourceException if handles already exist on the ManagedConnection or if the
     *             requested CRI contains a different user or password than the existing CRI.
     */
    private void replaceCRIForCCI(WSConnectionRequestInfoImpl newCRI) throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "replaceCRIForCCI", "Current:", cri, "New:", newCRI);

        if (numHandlesInUse > 0) {
            if (!supportIsolvlSwitching) {
                ResourceException resX = new DataStoreAdapterException("WS_INTERNAL_ERROR",
                                                                       null,
                                                                       getClass(),
                                                                       "ConnectionRequestInfo cannot be changed on a ManagedConnection with active handles.",
                                                                       AdapterUtil.EOLN + "Existing CRI: " + cri,
                                                                       AdapterUtil.EOLN + "Requested CRI: " + newCRI);

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "replaceCRIForCCI", resX); 
                throw resX;
            }
        }
        // The ManagedConnection should use the new CRI value in place of its current CRI.
        if (!newCRI.isCRIChangable())
            newCRI = WSConnectionRequestInfoImpl.createChangableCRIFromNon(newCRI);
        newCRI.setDefaultValues(defaultCatalog, defaultHoldability, defaultReadOnly, defaultTypeMap, defaultSchema,
                                defaultNetworkTimeout);
        cri = newCRI;

        //  -- don't intialize the properties, deplay to synchronizePropertiesWithCRI().

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "replaceCRIForCCI"); 
    }

    /**
     * Increase the size of the array that keeps track of handles associated with this
     * ManagedConnection.
     * 
     * @return the resized handle list.
     * 
     */
    private WSJdbcConnection[] resizeHandleList() {
        System.arraycopy(handlesInUse,
                         0,
                         handlesInUse = new WSJdbcConnection[
                                         maxHandlesInUse > numHandlesInUse ?
                                                         maxHandlesInUse :
                                                         (maxHandlesInUse = numHandlesInUse * 2)],
                         0,
                         numHandlesInUse);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, 
                     "Handle limit increased to: " + maxHandlesInUse);

        return handlesInUse;
    }

    /**
     * Synchronize the properties of the native connection with those of CRI.
     */
    private void synchronizePropertiesWithCRI() throws ResourceException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        int previousTransactionIsolation = -1; // only used for debugging
        int previousHoldability = -1; //  - only used for debugging

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "synchronizePropertiesWithCRI");

        try {
            isolationChanged = false;
            holdabilityChanged = false;

            if (kerberosConnection) {
                if (!AdapterUtil.matchGSSName(mc_gssName, cri.gssName)) {
                    if (isTraceOn && tc.isDebugEnabled()) 
                    {
                        Tr.debug(this, tc, "synching kerberos gssCredential on this mc and then clearing ps cache");
                    }

                    //  move the clearing of statement to before we issue the reuse.
                    // this will cover for cases where the reuse results in bad stuff. DB2 on Z seem to leak when this happens.
                    // reuse is supported with clean model only

                    // the cache gets created after the synchronizePropertiesWithCRI on newly created mc.
                    // thus, we should only call it if its not null here.  We don't want to have the check
                    // happen at the clearSTatementCache() since it puts a tr that cache is disabled (which si not the case)
                    if (statementCache != null) {
                        clearStatementCache();
                    }

                    // the reuse call won't go down to the DB til the next execution of the.  DB optimizaiton.
                    // if we were in a tran, a connection will have to match as we do compare subject and CRI
                    // the fact that the gssNames don't match, means we are not in a tra.
                    helper.reuseKerbrosConnection(sqlConn, cri.gssCredential, null);

                    //now save the cri props in the mc
                    mc_gssCredential = cri.gssCredential;
                    mc_gssName = cri.gssName;

                    // now that we issued the reuse, we need to purge the PS cache for this mc as
                    // reuse is supported with clean model only

                    handleCleanReuse();
                }
            }

            // Retrieve the default values for all Connection properties. 
            // Save the default values for when null is specified in the CRI. 
            previousTransactionIsolation = getTransactionIsolation();
            previousHoldability = currentHoldability;

            if (currentTransactionIsolation != cri.ivIsoLevel) {
                setTransactionIsolation(cri.ivIsoLevel);
            }

            if (cri.ivCatalog != null && !cri.ivCatalog.equals(defaultCatalog) && mcf.supportsGetCatalog) {
                setCatalog(cri.ivCatalog);
            }


            if (cri.ivReadOnly != null && defaultReadOnly != cri.ivReadOnly.booleanValue() && mcf.supportsIsReadOnly) {
                helper.setReadOnly(this, cri.ivReadOnly.booleanValue(), false); 
            }

            if (!AdapterUtil.match(cri.ivShardingKey, currentShardingKey)
             || !AdapterUtil.match(cri.ivSuperShardingKey, currentSuperShardingKey)) {
                setShardingKeys(cri.ivShardingKey, cri.ivSuperShardingKey);
            }

            if (cri.ivTypeMap != null && defaultTypeMap != cri.ivTypeMap && mcf.supportsGetTypeMap) {
                setTypeMap(cri.ivTypeMap);
            }
            
            // Use the default timeout if cri.ivNetworkTimeout hasn't been initialized.
            final int timeoutToSet = cri.ivNetworkTimeout == null ? defaultNetworkTimeout : cri.ivNetworkTimeout;
            
            if (mcf.supportsGetNetworkTimeout && currentNetworkTimeout != timeoutToSet) {
                final ExecutorService libertyThreadPool = mcf.connectorSvc.getLibertyThreadPool();
                // setNetworkTimeout is the only JDBC Connection property that may perform access checks
                if (System.getSecurityManager() == null) {
                    setNetworkTimeout(libertyThreadPool, timeoutToSet);
                } else {
                    try {
                        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                            @Override
                            public Void run() throws Exception {
                                setNetworkTimeout(libertyThreadPool, timeoutToSet);
                                return null;
                            }
                        });
                    } catch (PrivilegedActionException e) {
                        if (e.getException() instanceof SQLException)
                            throw (SQLException) e.getException();
                        throw (RuntimeException) e.getException();
                    }
                }
            }

            if(mcf.supportsGetSchema && (cri.ivSchema != null || defaultSchema != null)){
                String targetSchema = cri.ivSchema == null ? defaultSchema : cri.ivSchema;
                if(!AdapterUtil.match(currentSchema, targetSchema))
                    setSchema(targetSchema);
            }

            //  - synchronize the holdability value to either cri value or default value
            // if the default value is not 0, which means the JDBC driver supports holdability.
            if (defaultHoldability != 0) {
                int targetHoldability = cri.ivHoldability == 0 ?
                                defaultHoldability : cri.ivHoldability;
                if (currentHoldability != targetHoldability) {
                    setHoldability(targetHoldability);
                }
            }

        } catch (SQLException sqlX) {
            FFDCFilter.processException(sqlX, getClass().getName() + ".synchronizePropertiesWithCRI", "850", this);
            ResourceException x = AdapterUtil.translateSQLException(sqlX, this, true, getClass()); 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "synchronizePropertiesWithCRI", sqlX);
            throw x;
        }

        if (isTraceOn && tc.isDebugEnabled()) 
        {
            Tr.debug(this, tc, "previous/current value:",
                                   "AutoCommit:    " + currentAutoCommit + "/" + currentAutoCommit,
                                   "Isolation:     " + AdapterUtil.getIsolationLevelString(previousTransactionIsolation) +
                                                   "/" + AdapterUtil.getIsolationLevelString(currentTransactionIsolation),
                                   "Catalog:       " + defaultCatalog + "/" + (cri.ivCatalog == null ? defaultCatalog : cri.ivCatalog),
                                   "Schema:        " + defaultSchema + "/" + (cri.ivSchema == null ? defaultSchema : cri.ivSchema),
                                   "ShardingKey:   " + initialShardingKey + "/" + cri.ivShardingKey,
                                   "SuperShardingK:" + initialSuperShardingKey + "/" + cri.ivSuperShardingKey,
                                   "NetworkTimeout:" + defaultNetworkTimeout + "/" + (cri.ivNetworkTimeout == null ? defaultNetworkTimeout : cri.ivNetworkTimeout),
                                   "IsReadOnly:    " + defaultReadOnly + "/" + (cri.ivReadOnly == null ? defaultReadOnly : cri.ivReadOnly), 
                                   "TypeMap:       " + defaultTypeMap + "/" + (cri.ivTypeMap == null ? defaultTypeMap : cri.ivTypeMap),
                                   "Holdability:   " + AdapterUtil.getCursorHoldabilityString(previousHoldability) +
                                                   "/" + AdapterUtil.getCursorHoldabilityString(currentHoldability));
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "synchronizePropertiesWithCRI");

    }


    /**
     * used to do some cleanup after a reuse of connection
     * 
     * @param sqConn
     * @throws ResourceException
     * @throws Exception
     */
    private void handleCleanReuse() throws ResourceException {

        // moved clearing the cache to before we issue the reuse connection

        //Now since a reuse was issued, the connection is restored to its orginal properties, so make sure
        // that you match the cri.
        try {
            currentTransactionIsolation = sqlConn.getTransactionIsolation();
            currentHoldability = defaultHoldability;
            //       get the autoCommit value as it will be reset when reusing the connection
            currentAutoCommit = sqlConn.getAutoCommit();
        } catch (SQLException sqlX) {
            FFDCFilter.processException(sqlX, getClass().getName() + ".handleCleanReuse", "2787", this);
            throw AdapterUtil.translateSQLException(sqlX, this, true, getClass());
        }
        // start: turn out that tracing will be reset once the connection is reused.
        // so will need to reset it on our end too and then enable it if needed.
        loggingEnabled = false; // reset the tracing flag for this mc.
        if (helper.shouldTraceBeEnabled(this)) {
            helper.enableJdbcLogging(this);
        }
    }


    /**
     * Return a statement from the cache matching the key provided. Null is returned if no
     * statement matches. 
     * 
     * @param key the statement cache key.
     * @return a matching statement from the cache or null if none is found.
     * @throws SQLException if an error occurs obtaining a new statement.
     */

    public final Object getStatement(StatementCacheKey key) 
    {
        Object stmt = statementCache.remove(key);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
            if (stmt == null) {
                Tr.debug(this, tc, "No Matching Prepared Statement found in cache");
            } else {
                Tr.debug(this, tc, "Matching Prepared Statement found in cache: " + stmt);
            }
        }
        return stmt;
    }

    /**
     * Returns the statement into the cache. The statement is closed if an error occurs
     * attempting to cache it. This method will only called if statement caching was enabled
     * at some point, although it might not be enabled anymore.
     * 
     * @param statement the statement to return to the cache.
     * @param key the statement cache key.
     */
    public final void cacheStatement(Statement statement, StatementCacheKey key) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(this, tc, "cacheStatement", AdapterUtil.toString(statement), key);

        // Add the statement to the cache.  If there is no room in the cache, a statement from
        // the least recently used bucket will be cast out of the cache.  Any statement cast
        // out of the cache must be closed. 

        CacheMap cache = getStatementCache();
        Object discardedStatement = cache == null ? statement : statementCache.add(key, statement);

        if (discardedStatement != null)
            destroyStatement(discardedStatement);
    }

    /**
     * Returns the ManagedConnectionFactory which created this ManagedConnection
     * 
     * @return a WSManagedConnectionFactory.
     */

    public final WSManagedConnectionFactoryImpl getManagedConnectionFactory() {
        return mcf;
    }

    /**
     * Creates a new connection handle for the underlying physical connection
     * represented by the ManagedConnection instance. The physical connection here
     * is the JDBC connection.
     * <p>
     * This connection handle is used by the application code to refer to the
     * underlying physical connection. A connection handle is tied to its
     * ManagedConnection instance in a resource adapter implementation specific way.
     * The ManagedConnection uses the Subject and additional ConnectionRequest Info
     * (which is specific to resource adapter and opaque to application server) to
     * set the state of the physical connection.
     * <p>
     * This instance manages multiple connection handles. Although the caller must
     * manage the connection handle pool, We still manage a connection handle pool here
     * in order to reuse the CCIConnection handles and reduce the number of WSInteractionImpls
     * created.
     * 
     * @param Subject subject - a caller's security context
     * @param ConnectionRequestInfo cxRequestInfo - ConnectionRequestInfo instance
     * @return an Object that represents a connection handle. There are two types of
     *         connection handles which may be returned from this method:
     *         <ul>
     *         <li>WSJdbcConnection - a JDBC Connection handle</li>
     *         <li>WSRdbConnectionImpl - a CCI Connection handle</li>
     *         </ul>
     * @exception ResourceException - generic exception if operation fails
     * @exception SecurityException - ??????????
     */

    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        //cannot print subject - causes security violation 
        // At least trace whether the Subject is null or not. 
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "getConnection", subject == null ? null : "subject", AdapterUtil.toString(cxRequestInfo));

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any connection handles to be created on this MC, instead throw a SCE
        if (_mcStale) {
            if (isTraceOn && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "MC is stale");
            throw new DataStoreAdapterException("INVALID_CONNECTION", AdapterUtil.staleX(), WSRdbManagedConnectionImpl.class);
        }

        //if you aren't in a valid state when doing getConnection, you can't get a connection
        // from this MC

        int transactionState = stateMgr.transtate;

        if ((transactionState != WSStateManager.LOCAL_TRANSACTION_ACTIVE)
            && (transactionState != WSStateManager.GLOBAL_TRANSACTION_ACTIVE)
            && (transactionState != WSStateManager.RRS_GLOBAL_TRANSACTION_ACTIVE)
            && (transactionState != WSStateManager.NO_TRANSACTION_ACTIVE)) {
            String message =
                            "Operation 'getConnection' is not permitted for transaction state: " +
                                            getTransactionStateAsString();

            ResourceException resX = new DataStoreAdapterException("WS_INTERNAL_ERROR", null, getClass(), message);

            // Use FFDC to log the possible components list. Comment out for now. 
            FFDCFilter.processException(resX,
                                        "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.cleanupTransactions",
                                        "939", this, new Object[] { message, ". Possible components: Connection Manager" });

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getConnection", "bad transaction state " + getTransactionStateAsString());
            throw resX;
        }

        // The Subject must match the existing subject.  User and password of the CRI must
        // match the existing values.  Other CRI properties may be modified if there aren't
        // any handles on this ManagedConnection yet. 
        // Subject matching requires doPrivileged code, which is costly.  We can trust the
        // WebSphere ConnectionManager to always send us a matching Subject.  Until it becomes
        // necessary for the RRA to work with other ConnectionManagers, we will NOT compare
        // Subjects. 

        WSConnectionRequestInfoImpl newCRI = (WSConnectionRequestInfoImpl) cxRequestInfo;

        //  - Before, isolation level is only allowed to switch between CCI connections.
        // Now, the isolation level switching is allowed between JDBC and CCI connections.
        if (!supportIsolvlSwitching) 
        {
            if (!cri.equals(newCRI))
                replaceCRI(newCRI);
        } else //  must be CMP
        {
            replaceCRIForCCI(newCRI); 
        }

        // since the CRI of managed connection doesn't reflect the real
        // connection property values in the managed connection. we need to
        // synchronize the properties.

        // Avoid resetting properties when a handle has already been created. 

        if (numHandlesInUse == 0) 
        {
            // Refresh our copy of the connectionSharing setting with each first connection handle
            connectionSharing = dsConfig.get().connectionSharing; 

            /*
             * 1) setTransactionIsolation OK as it will only happen if no transaction is happening
             * 2) setReadOnly OK as it will only happen if no transaction is happening
             * 3) setTypeMap OK as it will only happen if no transaction is happening
             * 4) setHoldability OK as it will only happen if no transaction is happening
             */
            synchronizePropertiesWithCRI();

            if (stateMgr.getState() == WSStateManager.NO_TRANSACTION_ACTIVE ||
                (stateMgr.getState() == WSStateManager.RRS_GLOBAL_TRANSACTION_ACTIVE 
                && !rrsGlobalTransactionReallyActive)) 
            {
                if (helper.dataStoreHelper != null) {
                    if (doConnectionSetupPerTranProps == null) {
                        doConnectionSetupPerTranProps = new Properties();
                        doConnectionSetupPerTranProps.setProperty("FIRST_TIME_CALLED", "true");
                    } else {
                        doConnectionSetupPerTranProps.setProperty("FIRST_TIME_CALLED", "false");
                    }

                    // if we have a subject, it will take precedence
                    helper.doConnectionSetupPerTransaction(subject, (subject == null ? newCRI.ivUserName : null),
                                                           sqlConn, _claimedVictim, doConnectionSetupPerTranProps);
                }

                // setting the new subject in the managed connection, this may be the same
                // as the existing one, however, in the claimedVictim path it won't. Setting it all the time.
                this.subject = subject;

                // now reset the _claimedVictim status, since at this point, the subject should match
                // the sqlConn
                this._claimedVictim = false;

            }
        } else 
        {
            // - we should allow to change the isolation level is the switching is supported
            // this value is used by the connection handle to save this value. So this isolation
            // level must be set before creating a handle.
            if (supportIsolvlSwitching && currentTransactionIsolation != cri.ivIsoLevel)
                try {
                    setTransactionIsolation(cri.ivIsoLevel); 
                } catch (SQLException sqlX) 
                {
                    FFDCFilter.processException(sqlX, 
                                                getClass().getName() + ".getConnection", "1867", this);

                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(this, tc, "getConnection", sqlX);
                    throw AdapterUtil.translateSQLException(sqlX, this, true, getClass()); 
                }

        }

        //  - Subject and CRI are required for handle reassociation.
        // These values must be forwarded to the CM to reassociate with a new MC.
        // The JDBC handle will not modify the CRI or Subject.
        // The Connection handle will request the CRI and Subject only
        // when dissociated, to take a snapshot of the current Connection properties the
        // handle wishes to be reassociated to.
        // If the ManagedConnection was just taken out the pool, the thread id may not
        // be recorded yet. In this case, use the current thread id. 
        // Use the already-casted CRI here. 

        WSJdbcConnection handle = mcf.jdbcRuntime.newConnection(this, sqlConn, key, threadID);
        addHandle(handle);

        //here is one of two boundaries to enable/disable tracing
        // if logwriter was enabled dynamicall, then we will check every time since we don't know then
        // if the user enabled it or diabled it dynamically, (i.e. perfomance won't be as good)
        if (helper.shouldTraceBeEnabled(this))
            helper.enableJdbcLogging(this);
        else if (helper.shouldTraceBeDisabled(this))
            helper.disableJdbcLogging(this);

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "numHandlesInUse", numHandlesInUse);

        if (helper.isCustomHelper && numHandlesInUse == 1) {
            helper.doConnectionSetupPerGetConnection(sqlConn, subject);
            // indicate that we need to undo in case cleanup is called before close
            perCloseCleanupNeeded = true;
        }

        // Record the number of fatal connection errors found on connections created by the
        // parent ManagedConnectionFactory at the time the last handle was created for this
        // ManagedConnection.  This allows us to determine whether it's safe to return this
        // ManagedConnection to the pool.  If the ManagedConnectionFactory's fatal error count
        // is greater than that of the ManagedConnection, it is not safe to pool the
        // ManagedConnection, as this shows fatal errors may have occurred on other
        // connections between the time the connection was last used and when it was closed.
        // This indicator is used to the implement the purge policy of "all open connections"
        // used by the default connection manager. 
        fatalErrorCount = mcf.fatalErrorCount.get();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "getConnection", handle);
        return handle;
    }

    /**
     * Destroys the physical connection to the underlying resource manager.
     * <p>
     * To manage the size of the connection pool, an application server can
     * explictly call ManagedConnection.destroy to destroy a physical connection.
     * A resource adapter should destroy all allocated system resources for this
     * ManagedConnection instance when the method destroy is called.
     * 
     * @exception ResourceException - Possible causes for this exception are:
     *                1) calling close on the physical connection failed
     *                2) calling close on the physical xaConnection failed
     *                3) WSRdbConnection.cleanup threw an exception
     *                4) this.cleanup() threw an exception
     */

    public void destroy() throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "destroy");

        // Save the first exception to occur and raise it after all other destroy processing is complete.
        // Don't map exceptions and fire ConnectionError event from destroy because the
        // ManagedConnection is already being destroyed and there is no further action to take.

        ResourceException dsae = null;

        if (inRequest || isAborted())
            try {
                inRequest = false;
                mcf.jdbcRuntime.endRequest(sqlConn);
            } catch (SQLException x) {
                if (!isAborted()) {
                    FFDCFilter.processException(x, getClass().getName(), "2447", this);
                    dsae = new DataStoreAdapterException("DSA_ERROR", x, getClass());
                }
                Tr.debug(tc, "Error during end request in destroy.", x);
            }

        if(isAborted()){
            if(isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "destroy", "ManagedConnection is aborted -- skipping destroy");
            return;
        }

        try {
            //  - We can't use the normal cleanup here because it dissociates
            // handles and we don't want that.  Instead we do it in pieces...

            cleanupTransactions(false); 
            // ManagedConnection is being destroyed. Do not reset for pooling. 
        } catch (ResourceException re) {
            FFDCFilter.processException(re,
                                        "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.destroy", "957", this);

            Tr.warning(tc, "DSTRY_ERROR_EX", re); 
            if (dsae == null)
                dsae = re; 
        }

        //  Always clean out the jdbc connection list since they can't be reused beyond
        //  the sharing boundary
        // Synchronization is not needed here since the ConnectionManager should not be
        // calling destroy concurrently with other operations on the same ManagedConnection.

        ResourceException closeX = closeHandles();
        dsae = dsae == null ? closeX : dsae;

        // Remove the event listeners prior to closing the underlying connection and
        // destroying the statement cache. In addition to eliminating unnecessary events
        // from the JDBC driver, this also make it possible for us to determine--if we
        // ever do see a connection-closed event--that it's an unexpected close from the
        // JDBC driver rather than one we requested.
        if (poolConn != null && !mcf.isUCP)
        {
            poolConn.removeConnectionEventListener(this);

            if (mcf.jdbcDriverSpecVersion >= 40)
                try {
                    poolConn.removeStatementEventListener(this);
                } catch (UnsupportedOperationException supportX) {
                    // No FFDC code needed. The JDBC 4.0 driver ought to implement the
                    // removeStatementEventListener method, but doesn't.  A warning is not logged because
                    // there is nothing that a customer can do in response.  It is an issue in
                    // the JDBC driver.
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(this, tc,
                                 "JDBC driver does not support removeStatementEventListener");
                } catch (AbstractMethodError methErr) {
                    // No FFDC code needed. The JDBC 4.0 driver ought to implement the
                    // removeStatementEventListener method, but doesn't.  A warning is not logged because
                    // there is nothing that a customer can do in response.  It is an issue in
                    // the JDBC driver.
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(this, tc,
                                 "JDBC driver does not implement removeStatementEventListener");
                }
        }

        //destroy the cache
        // make sure to check if the cache is null before destroying it....
        // could be null if there was no statement cache created due to a zero for
        // statement cache size....

        if (statementCache != null)
            clearStatementCache(); 

        // Close the SQLJ default context. 

        if (sqljContext != null)
            try {
                sqljContext.getClass().getMethod("close").invoke(sqljContext);
            } catch (Throwable x) {
                FFDCFilter.processException(x, getClass().getName() + ".destroy", "2596", this);
                if (dsae == null)
                    dsae = new DataStoreAdapterException("DSA_ERROR", x instanceof InvocationTargetException ? x.getCause() : x, getClass());
            }
        if (cachedConnection != null)
            try {
                cachedConnection.close();
            } catch (SQLException sqlex) {
                FFDCFilter.processException(sqlex, "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.destroy", "2607", this);
                DataStoreAdapterException dsex = new DataStoreAdapterException("DSA_ERROR", sqlex, getClass());
                if (dsae == null)
                    dsae = dsex;
            }

        // - need to cleanup the context before closing the connection.. closing connection moves to this
        if (sqlConn != null)
            try {
                sqlConn.close();
            } catch (SQLException se) {
                FFDCFilter.processException(se,
                                            "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.destroy", "1005", this);
                // Creating a new exception logs a warning for the error.
                ResourceException resX = new DataStoreAdapterException("DSA_ERROR", se, getClass());
                if (dsae == null)
                    dsae = resX;
            }

        if (poolConn != null) { 
            try {
                poolConn.close();
            } catch (SQLException se) {
                FFDCFilter.processException(se,
                                            "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.destroy", "1024", this);

                // Creating a new exception logs a warning for the error.
                ResourceException resX = new DataStoreAdapterException("DSA_ERROR", se, getClass());
                if (dsae == null)
                    dsae = resX;
            }
        } 

        //Null out all vars

        defaultCatalog = null; 
        defaultTypeMap = null; 
        defaultSchema = null;
        initialShardingKey = null;
        initialSuperShardingKey = null;

        handlesInUse = null;

        ivEventListeners = null;
        numListeners = 0; 
        localTran = null;
        xares = null;
        cri = null;
        subject = null;
        sqlConn = null;
        poolConn = null;
        statementCache = null; 
        sqljContext = null; 
        cachedConnection = null; 

        //Lastly, if there was a DataStoreAdapterException to throw from above, throw it here
        if (dsae != null) {
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "destroy", dsae);
            throw dsae;
        }

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "destroy");

    }

    /**
     * This method is invoked by the connection handle during dissociation to signal the
     * ManagedConnection to remove all references to the handle. If the ManagedConnection
     * is not associated with the specified handle, this method is a no-op and a warning
     * message is traced.
     * 
     * @param the connection handle.
     */
    public void dissociateHandle(WSJdbcConnection connHandle) 
    {
        if (!cleaningUpHandles && 
            !removeHandle(connHandle))

        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "Unable to dissociate Connection handle with current ManagedConnection because it is not currently associated with the ManagedConnection.",
                         connHandle);
        }
    }

    /**
     * Application server calls this method to force any cleanup on the
     * ManagedConnection instance.
     * <p>
     * The method ManagedConnection.cleanup initiates a cleanup of the any
     * client-specific state as maintained by a ManagedConnection instance.
     * The cleanup should invalidate all jdbc connection handles that had been created
     * using this ManagedConnection instance.
     * Any attempt by an application
     * component to use the connection handle after cleanup of the underlying
     * ManagedConnection should result in an exception. (unless implicit handle reactivation
     * is enabled)
     * <p>
     * The cleanup of ManagedConnection is always driven by an application
     * server. An application server should not invoke ManagedConnection.cleanup
     * when there is an uncompleted transaction (associated with a
     * ManagedConnection instance) in progress.
     * <p>
     * The invocation of ManagedConnection.cleanup method on an already
     * cleaned-up connection should not throw an exception.
     * <p>
     * The cleanup of ManagedConnection instance resets its client specific state
     * and prepares the connection to be put back in to a connection pool. The
     * cleanup method should not cause resource adapter to close the physical
     * pipe and reclaim system resources associated with the physical connection.
     * <p>
     * <p>Contract with ConnectionManager:
     * <ul>
     * <li> for non-sharing case, CM only calls the cleanup when all the handles are closed.</li>
     * <li> for sharing case, CM will call the cleanup when the transaction is ended. It then
     * parks the outstanding handles to a new MC. When the new method is invoked, these handles
     * will be re-associated with the original MC. -- CM must gurantee that the handles are
     * re-associated with the same MC. This is no longer true. We are now required to store
     * connection handle state so it can reassociate with different MCs. However, no child
     * objects of the connection handle are saved. But the child objects of the connection
     * handle ARE saved if the reassociation is done within a global transaction. In this
     * case, the handle is reserved for the original ManagedConnection and can only be
     * reassociated back to that MC.</li>
     * </ul>
     * 
     * @exception ResourceException - Possible causes for this exception are:
     *                1) closing the Connection handle failed
     *                2) rollback on the physical connection failed
     *                3) setAutoCommit() failed
     *                4) cleanup was called from an invalid transaction state
     *                5) cleanup was called while still in an active transaction state.
     */

    public final void cleanup() throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "cleanup");

        // Save the first exception to occur, continue processing, and throw it later.
        // This allows us to ensure all handles are dissociated and transactions are
        // rolled back, even if something fails early on in the cleanup processing.
        ResourceException firstX = null;

        if (inRequest && !isAborted())
            try {
                inRequest = false;
                mcf.jdbcRuntime.endRequest(sqlConn);
            } catch (SQLException x) {
                if (!isAborted()) {
                    FFDCFilter.processException(x, getClass().getName(), "2682", this);
                    firstX = new DataStoreAdapterException("DSA_ERROR", x, getClass());
                }
                Tr.debug(tc, "Error during end request in cleanup.", x);
            }

        // According to the JCA 1.5 spec, all remaining handles must be invalidated on
        // cleanup.  This is achieved by closing the handles.  Dissociating the handles would
        // not be adequate because dissociated handles may be used again.  
        // Skip the closeHandles processing if there are no handles. 

        ResourceException handleX = numHandlesInUse < 1 ? null : closeHandles();
        if (handleX != null && firstX == null)
            firstX = handleX;

        try {
            cleanupTransactions(true); 
        } catch (ResourceException resX) {
            // No FFDC code needed; already done in cleanupTransactions method.
            if (firstX == null)
                firstX = resX;
        }

        // Reset the thread id we are tracking for multithreaded access detection.  When the
        // ManagedConnection is handed out of the pool, it may be given to a different thread.
        threadID = null;

        // At this point, all of the most important stuff has been cleaned up.  Handles are
        // dissociated and transactions are rolled back.  Now we may throw any exceptions
        // which previously occurred.

        if (firstX != null) {
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "cleanup", firstX);

            throw firstX;
        }

        // Cleanup ManagedConnection and Connection states in a separate method. 
        cleanupStates();

        // Throw an exception if finalize is invoked and this ManagedConnection was never
        // destroyed. 

        if (fatalErrorCount == -1) 
        {
            ResourceException x = new DataStoreAdapterException("CONN_NEVER_CLOSED", null, getClass());
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "cleanup", "Exception"); 
            throw x;
        }

        if (perCloseCleanupNeeded) {
            perCloseCleanupNeeded = false;
            SQLException failure = helper.doConnectionCleanupPerCloseConnection(sqlConn);
            if (failure != null)
                throw AdapterUtil.translateSQLException(failure, this, false, getClass());
        }

        // Reset to null so that it gets refreshed on next use.
        transactional = null; 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "cleanup"); 
    }

    /**
     * Resets ManagedConnection and underlying Connection states. This method is used by both
     * cleanup and destroy. 
     * 
     * @throws ResourceException if an error occurs resetting the states.
     */
    private void cleanupStates() throws ResourceException {
        stateMgr.transtate = WSStateManager.NO_TRANSACTION_ACTIVE;

        // set the internal flag that keeps track of RRSGlobalTran to false. since we are cleanning
        //up and we know RRS can't be on by now.
        // now that we cleanup, reset the rrsGlobalTransactionReallyActive flag.
        // not that in the case of RRS and unsharable connections.  Cleanup will not be called after a Global tran is
        // done.  We are ok howevaer, as the rrsGlobalTransactionReallyActive will also be reset in the afterCompletion call
        // by the J2C component.
        rrsGlobalTransactionReallyActive = false;

        if (!connectionErrorDetected && sqljContext != null) // Clean up cached SQLJ context and its execution context
            try {
                Object execCtx = sqljContext.getClass().getMethod("getExecutionContext").invoke(sqljContext);
                Class<?> ExecutionContext = execCtx.getClass();
                ExecutionContext.getMethod("setBatching", boolean.class).invoke(execCtx, false);
                ExecutionContext.getMethod("setBatchLimit", int.class).invoke(execCtx, -7); // sqlj.runtime.ExecutionContext.UNLIMITED_BATCH
                ExecutionContext.getMethod("setFetchDirection", int.class).invoke(execCtx, 1000); // sqlj.runtime.ResultSetIterator.FETCH_FORWARD
                ExecutionContext.getMethod("setFetchSize", int.class).invoke(execCtx, 0);
                ExecutionContext.getMethod("setMaxFieldSize", int.class).invoke(execCtx, 0);
                ExecutionContext.getMethod("setMaxRows", int.class).invoke(execCtx, 0);
                ExecutionContext.getMethod("setQueryTimeout", int.class).invoke(execCtx, 0);
            } catch (Exception x) {
                FFDCFilter.processException(x, getClass().getName(), "3740", this);
                throw new DataStoreAdapterException("DSA_ERROR", x, getClass());
            }

        // Currently only OracleHelper will clean up the connection and returns true.
        // The reason to do that is because Oracle XA driver doesn't allow a xa transaction started
        // when the autoCommit is set to true. Therefore, before we return the Oracle XA connection to the
        // connection pool, we should reset the autocommit of this connection to false.
        //  - SybaseHelper will also return true.

        boolean modifiedByCleanup;

        try {

            if (!connectionErrorDetected) //  only reset the client if the connection is valid
            {
                //   start  cleanup, clean up will be done only if its been set
                helper.resetClientInformation(this);
            }

            modifiedByCleanup = helper.doConnectionCleanup(sqlConn);

            if (!connectionErrorDetected) 
            {
                // here is another boundary to enable/disable tracing
                if (helper.shouldTraceBeEnabled(this))
                    helper.enableJdbcLogging(this);
                else if (helper.shouldTraceBeDisabled(this))
                    helper.disableJdbcLogging(this);
            }
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, getClass().getName() + ".cleanupStates", "1298", this);

            // Don't need to send ConnectionError event when cleanup fails. Throwing an
            // exception on cleanup is enough to signal a connection error. 
            throw new DataStoreAdapterException("DSA_ERROR", ex, WSRdbManagedConnectionImpl.class);
        }


        //  - reset some Connection properties to their defaults so we don't
        // hand out Connections from the pool with unexpected values.
        // The values are updated directly on the underlying JDBC driver. 

        // Since the requested values were reset to the defaults of the underlying JDBC
        // driver, we should consider no properties to have been requested changed.

        // reset readonly, catalog and typeMap  

        // If doConnectionCleanup indicates it modified ANY connection properties (by
        // returning true) we need to reset ALL connection properties to ensure the
        // modifications are accounted for. 

        if (!connectionErrorDetected && 
            (connectionPropertyChanged || modifiedByCleanup)) 
        {
            if (mcf.supportsIsReadOnly) {
                try {
                    helper.setReadOnly(this, defaultReadOnly, false); 

                    // Update the connection request information after switching back to the
                    // default read-only setting.
                    if (connectionSharing == ConnectionSharing.MatchCurrentState)
                    {
                        if (!cri.isCRIChangable()) // create a changable CRI if existing one is not
                            setCRI(WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                        cri.setReadOnly(defaultReadOnly);
                    }
                } catch (SQLException sqle) {
                    FFDCFilter.processException(sqle, getClass().getName() + ".cleanupStates",
                                                "1226", this);
                    throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
                }
            }

            if (mcf.supportsGetCatalog)
            {
                try {
                    setCatalog(defaultCatalog);

                    // Update the connection request information after switching back to the
                    // default catalog.
                    if (connectionSharing == ConnectionSharing.MatchCurrentState)
                    {
                        if (!cri.isCRIChangable()) // create a changable CRI if existing one is not
                            setCRI(WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                        cri.setCatalog(defaultCatalog);
                    }
                } catch (SQLException sqle) {
                    FFDCFilter.processException(sqle, getClass().getName() + ".cleanupStates",
                                                "1227", this);
                    throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
                }
            }

            if (mcf.supportsGetTypeMap) {
                try {
                    setTypeMap(defaultTypeMap);

                    // Update the connection request information after switching back to the
                    // default type map.
                    if (connectionSharing == ConnectionSharing.MatchCurrentState)
                    {
                        if (!cri.isCRIChangable()) // create a changable CRI if existing one is not
                            setCRI(WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                        cri.setTypeMap(defaultTypeMap);
                    }
                } catch(SQLFeatureNotSupportedException nse) {
                    // Ignore since we are only attempting to cleanup
                } catch(UnsupportedOperationException uoe){
                    // Ignore since we are only attempting to cleanup
                } catch (SQLException sqle) {
                    if (helper.isUnsupported(sqle)){
                        // ignore unsupported exception
                    } else {
                        FFDCFilter.processException(sqle, getClass().getName() + ".cleanupStates",
                                                    "1228", this);
                        throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
                    }
                }
            }
            
            if(mcf.supportsGetSchema){
                try{
                    setSchema(defaultSchema);
                    
                    if(connectionSharing == ConnectionSharing.MatchCurrentState){
                        if(!cri.isCRIChangable())
                            setCRI(WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));
                        cri.setSchema(defaultSchema);
                    }
                } catch(SQLFeatureNotSupportedException nse) {
                    // Ignore since we are only attempting to cleanup
                } catch(UnsupportedOperationException uoe){
                    // Ignore since we are only attempting to cleanup
                } catch (SQLException sqle) {
                    if (helper.isUnsupported(sqle)){
                        // ignore unsupported exception
                    } else {
                        FFDCFilter.processException(sqle, getClass().getName() + ".cleanupStates",
                                                    "3644", this);
                        throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
                    }
                }
            }

            if (!AdapterUtil.match(currentShardingKey, initialShardingKey)
             || !AdapterUtil.match(currentSuperShardingKey, initialSuperShardingKey)) {
                try {
                    setShardingKeys(initialShardingKey, initialSuperShardingKey);

                    // Update the connection request information after switching back to the
                    // default sharding keys.
                    if (connectionSharing == ConnectionSharing.MatchCurrentState)
                    {
                        if (!cri.isCRIChangable()) // create a changable CRI if existing one is not
                            setCRI(WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                        cri.setShardingKey(initialShardingKey);
                        cri.setSuperShardingKey(initialSuperShardingKey);
                    }
                } catch (SQLException sqle) {
                    FFDCFilter.processException(sqle, getClass().getName(), "2959", this);
                    throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
                }
            }

            if(mcf.supportsGetNetworkTimeout){
                try{
                    final ExecutorService libertyThreadPool = mcf.connectorSvc.getLibertyThreadPool();
                    // setNetworkTimeout is the only JDBC Connection property that may perform access checks
                    if (System.getSecurityManager() == null) {
                        setNetworkTimeout(libertyThreadPool, defaultNetworkTimeout);
                    } else {
                        try {
                            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                                @Override
                                public Void run() throws Exception {
                                    setNetworkTimeout(libertyThreadPool, defaultNetworkTimeout);
                                    return null;
                                }
                            });
                        } catch (PrivilegedActionException e) {
                            if (e.getException() instanceof SQLException)
                                throw (SQLException) e.getException();
                            throw (RuntimeException) e.getException();
                        }
                    }
                    
                    if(connectionSharing == ConnectionSharing.MatchCurrentState){
                        if(!cri.isCRIChangable())
                            setCRI(WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));
                        cri.setNetworkTimeout(defaultNetworkTimeout);
                    }
                } catch(SQLFeatureNotSupportedException nse) {
                    // Ignore since we are only attempting to cleanup
                } catch(UnsupportedOperationException uoe){
                    // Ignore since we are only attempting to cleanup
                } catch (SQLException sqle) {
                    if (helper.isUnsupported(sqle)){
                        // ignore unsupported exception
                    } else {
                        FFDCFilter.processException(sqle, getClass().getName() + ".cleanupStates",
                                                    "3672", this);
                        throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
                    }
                }
            }

            connectionPropertyChanged = false;

            //  - get the autocommit value, isolation level value and holdability value from the native connection
            if (modifiedByCleanup) {
                try {
                    currentAutoCommit = sqlConn.getAutoCommit();
                    if (cachedConnection != null) 
                        cachedConnection.setCurrentAutoCommit(currentAutoCommit, key);
                } catch (SQLException sqle) {
                    FFDCFilter.processException(sqle, getClass().getName() + ".cleanupStates",
                                                "1308", this);
                    throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
                }

                try {
                    currentTransactionIsolation = sqlConn.getTransactionIsolation();
                    if (cachedConnection != null) 
                        cachedConnection.setCurrentTransactionIsolation(currentTransactionIsolation, key);

                    // in case doConnectionCleanup changed the value, we need to synchup the cri if isoswitching is not supported.
                    if (!supportIsolvlSwitching && connectionSharing == ConnectionSharing.MatchCurrentState)
                    {
                        if (!cri.isCRIChangable()) // only set the cri if its not one of the static ones.
                            setCRI(WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                        cri.setTransactionIsolationLevel(currentTransactionIsolation);
                    }

                } catch (SQLException sqle) {
                    FFDCFilter.processException(sqle, getClass().getName() + ".cleanupStates",
                                                "1318", this);
                    throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
                }

                try {
                    currentHoldability = mcf.getHelper().getHoldability(sqlConn);

                    // Update the connection request information after switching back to the
                    // default holdability.
                    if (connectionSharing == ConnectionSharing.MatchCurrentState)
                    {
                        if (!cri.isCRIChangable()) // create a changable CRI if existing one is not
                            setCRI(WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                        cri.setHoldability(defaultHoldability);
                    }
                } catch (SQLException sqle) {
                    FFDCFilter.processException(sqle, getClass().getName() + ".cleanupStates()", "3626", this); 
                    throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
                }
            }
            //  ends

            //if non transactional datasource then we need to make sure that
            // the autocomit value matches the database default as we would never
            // change it ourselves due to the fact that we don't enlist in a tran
            if (!isTransactional() && (currentAutoCommit != defaultAutoCommit)) 
            {
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "autoCommit on connection doesn't match the database default, setting it to: ",
                                 defaultAutoCommit); 
                    }

                    sqlConn.setAutoCommit(defaultAutoCommit);
                    currentAutoCommit = defaultAutoCommit;

                    if (cachedConnection != null)
                        cachedConnection.setCurrentAutoCommit(defaultAutoCommit, key);
                } catch (SQLException sqle) {
                    FFDCFilter.processException(sqle, getClass().getName() + ".cleanupStates()", "3652", this);
                    throw new DataStoreAdapterException("DSA_ERROR", sqle, getClass());
                }
            }


        } else if (cachedConnection != null && !connectionErrorDetected)
            cachedConnection.setCurrentAutoCommit(defaultAutoCommit, key);

        // Reset the flag here to be safe, but realize we cannot rely on cleanup being called
        // after every global transaction. 
        wasLazilyEnlistedInGlobalTran = false; 
    }

    /**
     * Clean up any outstanding transactions. This method is called from cleanup and destroy.
     * 
     * @param inCleanup indicates whether this method was invoked from the cleanup method.
     * 
     * @throws ResourceException if an error occurs cleaning up transactions.
     */
    private void cleanupTransactions(boolean inCleanup) throws ResourceException 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        //Send connection error event if in transtates: Global transaction, Local Transaction,
        // or Trans ending because cleanup should never be called from these states.  If it is,
        // we rollback and throw the exception

        switch (stateMgr.transtate) {

            case WSStateManager.GLOBAL_TRANSACTION_ACTIVE: {
                try {
                    ((WSRdbXaResourceImpl) xares).end();
                } catch (javax.transaction.xa.XAException xae) {
                    // No FFDC code needed; this is a normal case.
                    // Continue with the rollback if an exception is thrown on end. 
                }
                
                if (aborted) {
                    break;
                }

                try {
                    ((WSRdbXaResourceImpl) xares).rollback();
                } catch (javax.transaction.xa.XAException xae) {
                    FFDCFilter.processException(xae, "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.cleanupTransactions", "1200", this);

                    if (isTraceOn && tc.isEventEnabled())
                        Tr.event(this, tc, 
                                 "Failed to end or rollback XAResource during cleanup from failure " +
                                                 "state. Continuing with cleanup.",
                                 AdapterUtil.getXAExceptionCodeString(xae.errorCode));

                    throw new DataStoreAdapterException("DSA_ERROR", xae, getClass());
                }

                if (inCleanup && !aborted) 
                {
                    String message =
                                    "Cannot call 'cleanup' on a ManagedConnection while it is still in a " +
                                                    "transaction.";

                    ResourceException resX = new DataStoreAdapterException("DSA_ERROR", null, getClass(), message);

                    // Use FFDC to log the possible components list. 
                    FFDCFilter.processException(resX,
                                                "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.cleanupTransactions",
                                                "1562", this, new Object[] { message, " Possible components: Connection Manager, Transactions" });

                    throw resX;
                }
                break; 
            }

            case WSStateManager.LOCAL_TRANSACTION_ACTIVE:
            case WSStateManager.TRANSACTION_ENDING:
            {
                // If no work was done during the transaction, the autoCommit value may still
                // be on.  In this case, just no-op, since some drivers like ConnectJDBC 3.1
                // don't allow commit/rollback when autoCommit is on.  

                if (aborted) {
                    break;
                }
                
                if (!currentAutoCommit)
                    try { // autoCommit is off
                        sqlConn.rollback();
                    } catch (SQLException se) {
                        FFDCFilter.processException(se, "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.cleanupTransactions", "1223", this);

                        if (isTraceOn && tc.isEventEnabled())
                            Tr.event(this, tc, 
                                     "Connection rollback failed. Continuing with cleanup.");

                        throw new DataStoreAdapterException("DSA_ERROR", se, getClass());
                    }

                if (inCleanup) 
                {
                    // Remove inaccurate statement & redundant information. 

                    String message =
                                    "Cannot call 'cleanup' on a ManagedConnection while it is still in a " +
                                                    "transaction.";

                    ResourceException resX = new DataStoreAdapterException("DSA_ERROR", null, getClass(), message);

                    // Use FFDC to log the possible components list. Comment out for now. 
                    FFDCFilter.processException(resX,
                                                "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.cleanupTransactions",
                                                "1592", this, new Object[] { message, " Possible components: Connection Manager" });

                    throw resX;
                }

                break; 
            }

            case WSStateManager.RRS_GLOBAL_TRANSACTION_ACTIVE: 
            { 
              // this case is the same as the previous case except for the rollback, because commit/rollback is illegal during
              // an RRS controlled transaction
                if (inCleanup) 
                { 
                    String message = 
                    "Cannot call 'cleanup' on a ManagedConnection while it is still in an " + 
                                    "RRS managed transaction."; 

                    ResourceException resX = new DataStoreAdapterException("DSA_ERROR", null, getClass(), message);

                    // Use FFDC to log the possible components list. Comment out for now.                                      
                    FFDCFilter.processException(resX, 
                                                "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.cleanupTransactions", 
                                                "3340", this, new Object[] { message, " Possible components: Connection Manager" }); 

                    throw resX; 
                } 
                break;
            } 

                // case WSStateManager.RECOVERY_IN_PROGRESS)
                //     no-op; allow cleanup to set the state back to NO_TRANSACTION_ACTIVE 
        }

        CommitOrRollbackOnCleanup cleanupAction = dsConfig.get().commitOrRollbackOnCleanup; 

        if (!connectionErrorDetected) // do the checking only if the connection is not bad
        {
            if (cleanupAction != null) 
            {
                /*
                 * DSConfigurationHelper.COMMIT_OR_ROLLBACK_ON_CLEANUP was set on the DataSource, so we will honor
                 * that property
                 */
                if (mcf.supportsUOWDetection) {
                    String operation = "none";
                    try {
                        if (helper.isInDatabaseUnitOfWork(sqlConn)) {
                            /*
                             * If the DB supports UOW Detection and we are in a DB UOW we will commit or rollback per
                             * setting on the DataSource.
                             */
                            if (cleanupAction == CommitOrRollbackOnCleanup.commit) 
                            {
                                operation = "commit";
                                if (!mcf.loggedDbUowMessage) {
                                    Tr.info(tc, "RESOLVING_DB_IMPLICIT_TRANSACTIONS", operation);
                                    mcf.loggedDbUowMessage = true;
                                } else {
                                    if (isTraceOn && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "committing implicit transaction");
                                }
                                try{
                                    sqlConn.commit();
                                } catch(SQLException e){
                                    String message = "Error resolving implicitly started transaction on ManagedConnection.";
                                    Tr.info(tc, "ERROR_RESOLVING_DB_IMPLICIT_TRANSACTIONS", operation, e);
                                    FFDCFilter.processException(e,
                                                                "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.cleanupTransactions",
                                                                "3588", this, new Object[] { message });
                                    if (isTraceOn && tc.isDebugEnabled()) 
                                    {
                                        Tr.debug(this, tc, "Error resolving implicitly started transaction on ManagedConnection.  Exception is:", e);
                                    }
                                    
                                    LocalTransactionException lte = (LocalTransactionException) (new LocalTransactionException(e.getMessage()).initCause(e));
                                    
                                    throw lte;
                                }
                            } else if (cleanupAction == CommitOrRollbackOnCleanup.rollback) 
                            {
                                operation = "rollback";
                                if (!mcf.loggedDbUowMessage) {
                                    Tr.info(tc, "RESOLVING_DB_IMPLICIT_TRANSACTIONS", operation);
                                    mcf.loggedDbUowMessage = true;
                                } else {
                                    if (isTraceOn && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "rolling back implicit transaction");
                                }
                                try{
                                    sqlConn.rollback();
                                } catch(SQLException e){
                                    String message = "Error resolving implicitly started transaction on ManagedConnection.";
                                    Tr.info(tc, "ERROR_RESOLVING_DB_IMPLICIT_TRANSACTIONS", operation, e);
                                    FFDCFilter.processException(e,
                                                                "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.cleanupTransactions",
                                                                "3594", this, new Object[] { message });
                                    if (isTraceOn && tc.isDebugEnabled()) 
                                    {
                                        Tr.debug(this, tc, "Error resolving implicitly started transaction on ManagedConnection.  Exception is:", e);
                                    }
                                    
                                    LocalTransactionException lte = (LocalTransactionException) (new LocalTransactionException(e.getMessage()).initCause(e));
                                    
                                    throw lte; 
                                }
                            }
                        }
                    } catch (ResourceException e){
                        throw e;
                    } catch (SQLException e){
                        //This is from mcf.helper.isInDatabaseUnitOfWork(sqlConn), presumably
                        String message = "SQLException created in process of cleaning up implicitly started transaction on ManagedConnection.";
                        FFDCFilter.processException(e,
                                                    "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.cleanupTransactions",
                                                    "3786", this, new Object[] { message });
                        ResourceException res = ((ResourceException) new ResourceException("Unable to complete connection cleanup; we shouldn't continue to use this connection.").initCause(e));
                        throw res;
                    } 
                } else // uow detection is not supported  
                {
                    /*
                     * The DB does not support UOW detection. The xxxOnCleanup action will be done automatically.
                     * No message is logged unless trace is enabled
                     */
                    try {
                        if (cleanupAction == CommitOrRollbackOnCleanup.commit) 
                        {
                            if (isTraceOn && tc.isDebugEnabled()) 
                            {
                                Tr.debug(this, tc, "Commit connection automatically per custom property " + DSConfig.COMMIT_OR_ROLLBACK_ON_CLEANUP);
                            }
                            // in the case were we dont' know if unit of wrk exists or not, we will only do the commit/rollback if AC = false
                            if (!currentAutoCommit) {
                                if (isTraceOn && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "AC is false, so we will issue a commit in case there is an implicit tra.");

                                sqlConn.commit();
                            }
                        } else if (cleanupAction == CommitOrRollbackOnCleanup.rollback) 
                        {
                            if (isTraceOn && tc.isDebugEnabled()) 
                            {
                                Tr.debug(this, tc, "Rollback connection automatically per custom property " + DSConfig.COMMIT_OR_ROLLBACK_ON_CLEANUP);
                            }
                            if (!currentAutoCommit) {
                                if (isTraceOn && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "AC is false, so we will issue a rollback in case there is an implicit tra.");

                                sqlConn.rollback();
                            }
                        }
                    } catch (Throwable t) {
                        String message = "Error resolving implicitly started transaction on ManagedConnection.";
                        FFDCFilter.processException(t,
                                                    "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.cleanupTransactions",
                                                    "3622", this, new Object[] { message });
                        if (isTraceOn && tc.isDebugEnabled()) 
                        {
                            Tr.debug(this, tc, "Error resolving implicitly started transaction on ManagedConnection.  Exception is:", t);
                        }
                    }
                }
            } else // default behaviour, In this case, we only rollback if unit of work is detected.
            {
                /*
                 * DSConfigurationHelper.COMMIT_OR_ROLLBACK_ON_CLEANUP was not specified, so we check for any
                 * implicit transactions. If there is an implicit tran we will rollback. Otherwise we will do nothing.
                 */
                if (mcf.supportsUOWDetection) {
                    try {
                        if (helper.isInDatabaseUnitOfWork(sqlConn)) {

                            if (!mcf.loggedImmplicitTransactionFound) {
                                Tr.info(tc, "IMPLICIT_TRANSACTION_FOUND");
                                mcf.loggedImmplicitTransactionFound = true;
                            }
                            sqlConn.rollback();
                        }
                    } catch (Throwable t) {
                        String message = "Error while attempting to rollback database implicitly started transaction on ManagedConnection.";
                        // we are not issueing warnings here because this is the default adn we just need to log it in trace.
                        FFDCFilter.processException(t,
                                                    "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.cleanupTransactions",
                                                    "3761", this, new Object[] { message });

                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Error while attempting to rollback database implicitly started transaction on ManagedConnection.", t);

                    }
                }
                // if the DB does not support UOW Detection do nothing.
                else {
                    if (!currentAutoCommit
                        && !isTransactional() 
                        && !mcf.warnedAboutNonTransactionalDataSource) {
                        // If AutoCommit=false there might be an unresolved transaction.
                        // We cannot ask the database because it doesn't support unit-of-work detection.
                        // We cannot rely on our internal transaction state because of transactional=false.
                        // CommitOrRollbackOnCleanup is not specified, so we will not be resolving,
                        // the transaction.

                        // Issue a warning that we might be leaving an unresolved transaction in
                        // the database.

                        Tr.warning(tc, "NONTRAN_DATASOURCE_WARNING",
                                   DataSourceDef.transactional.name() + "=false",
                                   DSConfig.COMMIT_OR_ROLLBACK_ON_CLEANUP
                        );
                        mcf.warnedAboutNonTransactionalDataSource = true;
                    }
                }
            }
        } //

    }

    //  - Allow the statement cache to be cleared when a StaleStatementException
    // occurs.
    /**
     * Removes and closes all statements in the statement cache for this ManagedConnection.
     */
    public final void clearStatementCache() {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        // The closing of cached statements is now separated from the removing of statements
        // from the cache to avoid synchronization during the closing of statements.

        if (statementCache == null) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "statement cache is null. caching is disabled"); 
            return;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "clearStatementCache"); 

        Object[] stmts = statementCache.removeAll();

        for (int i = stmts.length; i > 0;)
            try {
                ((Statement) stmts[--i]).close();
            } catch (SQLException closeX) {
                FFDCFilter.processException(
                                            closeX, getClass().getName() + ".clearStatementCache", "2169", this);

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Error closing statement", closeX); 
            }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "clearStatementCache"); 
    }

    /**
     * Closes all handles associated with this ManagedConnection. Processing continues even
     * if close fails on a handle. All errors are logged, and the first error is saved to be
     * returned when processing completes.
     * 
     * @return the first error to occur closing a handle, or null if none.
     */
    private ResourceException closeHandles() {
        ResourceException firstX = null;
        Object conn = null;

        // Indicate that we are cleaning up handles, so we know not to send events for
        // operations done in the cleanup. 
        cleaningUpHandles = true;

        for (int i = numHandlesInUse; i > 0;) 
        {
            conn = handlesInUse[--i]; 
            handlesInUse[i] = null; 

            try {
                ((WSJdbcConnection) conn).close();
            } catch (SQLException closeX) {
                FFDCFilter.processException(closeX,
                                            getClass().getName() + ".closeHandles", "1414", this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(this, tc, 
                             "Error closing handle. Continuing...", conn);

                ResourceException resX = new DataStoreAdapterException(
                                "DSA_ERROR", closeX, getClass());

                if (firstX == null)
                    firstX = resX;
            }
        }

        numHandlesInUse = 0; 
        cleaningUpHandles = false;

        return firstX;
    }

    private Subject copySubject(final Subject sub) throws ResourceException {
        // this code is straight from the javadoc for AccessController
        // using the example that returns a object.  Except we now use the WebSphere
        // AccessController class instead of the standard Java AccessController. 
        return AccessController.doPrivileged(new PrivilegedAction<Subject>() {
            public Subject run()
            {
                // privileged code goes here, for example:
                // can only access outer class final variables from here
                // so had to make readOnly, principals, pubCredentials & subF final.
                return new Subject(sub.isReadOnly(),
                                sub.getPrincipals(),
                                sub.getPublicCredentials(),
                                sub.getPrivateCredentials());
            }
        });
    }

    /**
     * Used by the container to change the association of an application-level
     * connection handle with a ManagedConneciton instance. The container should
     * find the right ManagedConnection instance and call the associateConnection
     * method.
     * <p>
     * The resource adapter is required to implement the associateConnection method.
     * The method implementation for a ManagedConnection should dissociate the
     * connection handle (passed as a parameter) from its currently associated
     * ManagedConnection and associate the new connection handle with itself. In addition
     * the state of the old ManagedConnection needs to be copied into the new ManagedConnection
     * in case the association occurs between methods during a transaction.
     * 
     * @param Object connection - Application-level connection handle
     * @exception ResourceException - Possible causes for this exception are:
     *                1) The connection is not in a valid state for reassociation.
     *                2) A fatal connection error was detected during reassoctiation.
     */

    public void associateConnection(Object connection) throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "associateConnection", connection);

        WSJdbcConnection connHandle = (WSJdbcConnection) connection;

        // JDBC Handles do not support reassociation during a transaction because of the
        // inability to reassociate "child" handles. (Statement, ResultSet, ...)
        // Therefore, we do not reassociate the handle, but instead "reserve" it for token
        // reassociation back to the same MC.  This relies on the guarantee we will always
        // be reassociated back to the original MC for use during the same transaction.

        try {
            WSRdbManagedConnectionImpl oldMC =
                            (WSRdbManagedConnectionImpl) connHandle.getManagedConnection(key);

            int tranState = oldMC == null ?
                            WSStateManager.NO_TRANSACTION_ACTIVE :
                            oldMC.stateMgr.transtate;

            // store the value just in case.
            if (oldMC != null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Old ManagedConnection rrsGlobalTransactionReallyActive :",
                             Boolean.valueOf(oldMC.rrsGlobalTransactionReallyActive)); 

                rrsGlobalTransactionReallyActive = oldMC.rrsGlobalTransactionReallyActive;
            }

            if (isTraceOn && tc.isDebugEnabled()) 
            {
                Tr.debug(this, tc, "Old ManagedConnection transaction state:",
                         oldMC == null ? null : oldMC.getTransactionStateAsString());

                Tr.debug(this, tc, "New ManagedConnection transaction state:",
                         this.getTransactionStateAsString());
            }

            // The CRI must be retrieved from the connection handle when we are certain the
            // handle is inactive since handles only keep track of the CRI when they are
            // inactive. 

            Object newCRI; 

            if ((tranState == WSStateManager.GLOBAL_TRANSACTION_ACTIVE
                 || (tranState == WSStateManager.RRS_GLOBAL_TRANSACTION_ACTIVE) // to be on the safe side, we don't want to check for rrsGlobalTransactionReallyActive here will take the case where its not really started.
                 || tranState == WSStateManager.LOCAL_TRANSACTION_ACTIVE)
                && !connHandle.isReserved()) {
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(this, tc, 
                             "Reassociation requested within a transaction; " +
                                             "handle reassociation will be ignored.");

                // A transaction is active; ignore the reassociation, but mark the handle as
                // reserved for its current ManagedConnection.

                connHandle.reserve(key);

                // Drop the handle from its current ManagedConnection. We may not be able to
                // drop handle's reference to the ManagedConnection, but we can at least drop
                // the ManagedConnection's reference to the handle.

                oldMC.dissociateHandle(connHandle);

                newCRI = connHandle.getCRI(); 
            }

            else {
                // No transaction is active, so a full reassociation is allowed.

                // If the handle is still ACTIVE, dissociate it from its previous MC.

                if (connHandle.getState() == WSJdbcConnection.State.ACTIVE)
                    connHandle.dissociate();

                // Retrieve the CRI while the handle is inactive.

                newCRI = connHandle.getCRI(); 

                //  - The connection handle must be supplied with both the new
                // ManagedConnection and underlying JDBC Connection.  The handle will handle
                // making the state of the new underlying Connection consistent with the
                // previously held one.

                connHandle.reassociate(this, sqlConn, key);
            }

            if (!cri.equals(newCRI))
                replaceCRI((WSConnectionRequestInfoImpl) newCRI); 

            // Avoid resetting properties when a handle has already been created. 

            if (numHandlesInUse == 0) 
            {
                // Refresh our copy of the connectionSharing setting with each first connection handle
                connectionSharing = dsConfig.get().connectionSharing; 

                synchronizePropertiesWithCRI(); //  - Synchronize the connection properties with CRI
            }

            addHandle(connHandle); 
        } catch (ResourceException resX) {
            FFDCFilter.processException(resX,
                                        "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.associateConnection",
                                        "1981", this);

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "associateConnection", "Exception"); 
            throw resX;
        }

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "associateConnection");
    }

    /**
     * Adds a connection event listener to the ManagedConnection instance.
     * <p>
     * The registered ConnectionEventListener instances are notified of connection
     * close and error events, also of local transaction related events on the
     * Managed Connection.
     * 
     * @param listener - a new ConnectionEventListener to be registered
     * 
     * @throws NullPointerException if you try to add a null listener.
     */

    public void addConnectionEventListener(ConnectionEventListener listener) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "addConnectionEventListener", listener);

        if (listener == null)
            throw new NullPointerException(
                            "Cannot add null ConnectionEventListener.");

        // Not synchronized because of the contract that add/remove event listeners will only
        // be used on ManagedConnection create/destroy, when the ManagedConnection is not
        // used by any other threads. 

        // Add the listener to the end of the array -- if the array is full,
        // then need to create a new, bigger one

        // check if the array is already full
        if (numListeners >= ivEventListeners.length) {
            // there is not enough room for the listener in the array
            // create a new, bigger array
            // Use the standard interface for event listeners instead of J2C's. 
            ConnectionEventListener[] tempArray = ivEventListeners;
            ivEventListeners = new ConnectionEventListener[numListeners + CEL_ARRAY_INCREMENT_SIZE];
            // parms: arraycopy(Object source, int srcIndex, Object dest, int destIndex, int length)
            System.arraycopy(tempArray, 0, ivEventListeners, 0, tempArray.length);
            // point out in the trace that we had to do this - consider code changes if there
            // are new CELs to handle (change KNOWN_NUMBER_OF_CELS, new events?, ...)
            if (isTraceOn && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "received more ConnectionEventListeners than expected, " +
                                   "increased array size to " + ivEventListeners.length);
        }

        // add listener to the array, increment listener counter
        ivEventListeners[numListeners++] = listener; 
    }

    /**
     * Removes an already registered connection event listener from the
     * ManagedConnection instance.
     * 
     * @param listener - already registered connection event listener to be removed
     */

    public void removeConnectionEventListener(ConnectionEventListener listener) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "removeConnectionEventListener", listener);

        if (listener == null) {
            NullPointerException nullX = new NullPointerException(
                            "Cannot remove null ConnectionEventListener.");

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "removeConnectionEventListener", nullX); 
            throw nullX; 
        }

        // setting the metrics to null here causes a NPE the metrics cannot be set to null
        // until the end of the destroy method.

        // Not synchronized because of the contract that add/remove event listeners will only
        // be used on ManagedConnection create/destroy, when the ManagedConnection is not
        // used by any other threads. 

        // Find matching listener in the array -- then remove it, adjust entries as
        // necessary, and adjust the counter

        // loop through the listeners
        for (int i = 0; i < numListeners; i++)
            // look for matching listener
            if (listener == ivEventListeners[i]) {
                // remove the matching listener, but don't leave a gap in the array -- the order of
                // the listeners in the array doesn't matter, so move the last listener to fill the
                // gap left by the remove, if necessary
                ivEventListeners[i] = ivEventListeners[--numListeners];
                ivEventListeners[numListeners] = null;

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "removeConnectionEventListener"); 
                return;
            }

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "removeConnectionEventListener", "Listener not found for remove.");
    }

    /**
     * Returns a javax.transaction.xa.XAresource instance. An application server
     * enlists this XAResource instance with the Transaction Manager if the
     * ManagedConnection instance is being used in a JTA transaction that is
     * being coordinated by the Transaction Manager.
     * 
     * @return a XAResource - if the dataSource specified for this ManagedConnection
     *         is of type XADataSource, then an XAResource from the physical connection is returned
     *         wrappered in our WSRdbXaResourceImpl. If the dataSource is of type ConnectionPoolDataSource,
     *         then our wrapper WSRdbOnePhaseXaResourceImpl is returned as the connection will not be
     *         capable of returning an XAResource as it is not two phase capable.
     * 
     * @exception ResourceException - Possible causes for this exception are:
     *                1) failed to get an XAResource from the XAConnection object.
     */

    public XAResource getXAResource() throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "getXAResource");

        if (xares != null) {
            if (isTraceOn && tc.isEventEnabled()) 
                Tr.event(this, tc, "Returning existing XAResource", xares);
        } else if (is2Phase) {
            //this is a JTAEnabled dataSource, so get a real XaResource object
            try {
                XAResource xa = ((javax.sql.XAConnection) poolConn).getXAResource();
                xares = new WSRdbXaResourceImpl(xa, this);

            } catch (SQLException se) {
                FFDCFilter.processException(se, "com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl.getXAResource", "1638", this);
                if (isTraceOn && tc.isEntryEnabled()) 
                    Tr.exit(this, tc, "getXAResource", se);
                throw AdapterUtil.translateSQLException(se, this, true, getClass());
            }
        } else {
            //this is not a JTAEnabled dataSource, so can't get an xaResource
            //instead get a OnePhaseXAResource
            xares = new WSRdbOnePhaseXaResourceImpl(sqlConn, this);
        }

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "getXAResource");
        return xares;

    }

    /**
     * Returns an javax.resource.spi.LocalTransaction instance. The
     * LocalTransaction interface is used by the container to manage local
     * transactions for a RM instance.
     * 
     * @return a LocalTransaction instance
     * @exception ResourceException - There should not be an exception thrown in this
     *                case. We just need to declare it as it is part of the interface.
     */

    public final LocalTransaction getLocalTransaction() throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "getLocalTransaction"); 

        localTran = localTran == null ?
                        new WSRdbSpiLocalTransactionImpl(this, sqlConn) :
                        localTran;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "getLocalTransaction", localTran); 
        return localTran;
    }

    /**
     * Returns the current transaction state for this ManagedConnection as an integer
     * <p>
     * Possible Transaction state values are:
     * <ul>
     * <li>WSStateManager.TRANSACTION_FAIL
     * <li>WSStateManager.GLOBAL_TRANSACTION_ACTIVE
     * <li>WSStateManager.LOCAL_TRANSACTION_ACTIVE
     * <li>WSStateManager.TRANSACTION_ENDING
     * <li>WSStateManager.NO_TRANSACTION_ACTIVE
     * <li>WSStateManager.TRANSACTION_HEURISTIC_END
     * </ul></p>
     * 
     * @return int - indicating the transaction state from WSStateManager
     */

    public final int getTransactionState() {
        return stateMgr.transtate;
    }

    /**
     * Returns the current transaction state for this ManagedConnection as a string. This
     * method is used for printing messages and trace statements.
     * <p>
     * Possible Transaction state strings are:
     * <ul>
     * <li>TRANSACTION_FAIL
     * <li>GLOBAL_TRANSACTION_ACTIVE
     * <li>LOCAL_TRANSACTION_ACTIVE
     * <li>TRANSACTION_ENDING
     * <li>NO_TRANSACTION_ACTIVE
     * 
     * @return String - indicating the transaction state from WSStateManager
     */

    public final String getTransactionStateAsString() {
        return stateMgr.getStateAsString();
    }

    /**
     * Get the metadata information for this connection's underlying database
     * resource manager instance. The ManagedConnectionMetaData interface
     * provides information about the underlying database instance associated
     * with the ManagedConenction instance.
     * 
     * @return a ManagedConnectionMetaData instance
     * @exception ResourceException - Possible causes for this exception are:
     *                1) failure to create the WSManagedConnectionMetaDataImpl instance.
     */

    public final ManagedConnectionMetaData getMetaData() throws ResourceException {
        throw new NotSupportedException();
    }

    /**
     * Sets the log writer for this ManagedConnection instance.
     * <p>
     * The log writer is a character output stream to which all logging and
     * tracing messages for this ManagedConnection instance will be printed.
     * Application Server manages the association of output stream with the
     * ManagedConnection instance based on the connection pooling requirements.
     * <p>
     * When a ManagedConnection object is initially created, the default log
     * writer associated with this instance is obtained from the
     * ManagedConnectionFactory. If the value is set on the MC, it will change
     * that of the mcf.
     * 
     * @param out - Character Output stream to be associated
     * 
     * @throws ResourceAdapterInternalException if something goes wrong.
     */

    public final void setLogWriter(PrintWriter out) throws ResourceException {

        // this method gets called by the J2c component whenever the RRA trace is enabled.
        // We can't have that since we need to seperate RRA trace from backend database tracing.
        // Thus, we are making this method a no-op.  We have a different mechanisim to enabled
        // the database logging using WAS.database=all=enabled.

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "setLogWriter on the mc is a no-op when calling this method");

    }

    /**
     * Gets the log writer for this ManagedConnection instance.
     * <p>
     * The log writer is a character output stream to which all logging and
     * tracing messages for this ManagedConnection instance will be printed.
     * ConnectionManager manages the association of output stream with the
     * ManagedConnection instance based on the connection pooling requirements.
     * <p>
     * The value returned will be that from the the underlying MCF
     * 
     */

    public final PrintWriter getLogWriter() throws ResourceException {
        return mcf.getLogWriter();
    }

    /*
     * @return boolean the requested autoCommit value, which may be different than the actual
     * current value on the underlying connection.
     */
    public final boolean getAutoCommit() 
    {
        return currentAutoCommit;
    }

    /*
     * Sets the requested autocommit value for the underlying connection if different than
     * the currently requested value or if required to always set the autocommit value as
     * a workaround. 346032.2
     * 
     * @param value the newly requested autocommit value.
     */
    public final void setAutoCommit(boolean value) throws SQLException 
    {
        if (value != currentAutoCommit || helper.alwaysSetAutoCommit()) 
        {
            if( (dsConfig.get().isolationLevel == Connection.TRANSACTION_NONE) && (value == false) )
                throw new SQLException(AdapterUtil.getNLSMessage("DSRA4010.tran.none.autocommit.required", dsConfig.get().id));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "Set AutoCommit to " + value); 

            // Don't update values until AFTER the operation completes successfully on the
            // underlying Connection. 

            sqlConn.setAutoCommit(value); 
            currentAutoCommit = value; 
        }
        if (cachedConnection != null) 
            cachedConnection.setCurrentAutoCommit(currentAutoCommit, key);
    }

    /**
     * Set the transactionIsolation level to the requested Isolation Level.
     * If the requested and current are the same, then do not drive it to the database
     * If they are different, drive it all the way to the database.
     **/

    public final void setTransactionIsolation(int isoLevel) throws SQLException 
    {
        if (currentTransactionIsolation != isoLevel) {            
            // Reject switching to an isolation level of TRANSACTION_NONE
            if (isoLevel == Connection.TRANSACTION_NONE) {
                    throw new SQLException(AdapterUtil.getNLSMessage("DSRA4011.tran.none.iso.switch.unsupported", dsConfig.get().id));
            }
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "Set Isolation Level to " + AdapterUtil.getIsolationLevelString(isoLevel));
            
            // Don't update the isolation level until AFTER the operation completes
            // succesfully on the underlying Connection. 
            
            sqlConn.setTransactionIsolation(isoLevel); 
            currentTransactionIsolation = isoLevel; 
            isolationChanged = true; 
        }
        if (cachedConnection != null) 
            cachedConnection.setCurrentTransactionIsolation(currentTransactionIsolation, key);
    }

    /**
     * Get the transactionIsolation level
     **/
    public final int getTransactionIsolation() 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
        {
            try {
                Tr.debug(this, tc, "The current isolation level from our tracking is: ", currentTransactionIsolation);
                Tr.debug(this, tc, "Isolation reported by the JDBC driver: ", sqlConn.getTransactionIsolation());
            } catch (Throwable x) {
                // NO FFDC needed
                // do nothing as we are taking the isolation level here for debugging reasons only
            }
        }

        return currentTransactionIsolation;
    }

    /**
     * Set the cursor holdability value to the request value
     * 
     * @param holdability the cursor holdability
     */
    public final void setHoldability(int holdability) throws SQLException { 
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setHoldability", "Set Holdability to " + 
                                                 AdapterUtil.getCursorHoldabilityString(holdability)); 
        sqlConn.setHoldability(holdability);
        currentHoldability = holdability;
        holdabilityChanged = true;
    }

    /**
     * Get the cursor holdability value
     * 
     * @return the cursor holdability value
     */
    public final int getHoldability() throws SQLException { 
        if (currentHoldability != 0) {
            // currentHoldability is not equal to 0, which means the JDBC driver supports cursor
            // holdability. We can just return the current value.
            return currentHoldability;
        } else {
            // currentHoldability is 0, which means the JDBC driver doesn't support cursor
            // holdability. We directly call this method of the native connection so we can
            // get the exception  from the native connection.
            return sqlConn.getHoldability();
        }
    }

    /**
     * Get the current cursor holdability value
     * 
     * @return the current cursor holdability value
     */
    public final int getCurrentHoldability() { 
        return currentHoldability;
    }

    /**
     * Updates the value of the catalog property.
     * 
     * @param catalog the new catalog.
     */
    public final void setCatalog(String catalog) throws SQLException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Set Catalog to " + catalog); 
        sqlConn.setCatalog(catalog);
        connectionPropertyChanged = true; 
    }

    /**
     * Updates the value of the readOnly property.
     * 
     * @param readOnly the new isReadOnly value.
     */
    public final void setReadOnly(boolean isReadOnly) throws SQLException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Set readOnly to " + isReadOnly); 
        sqlConn.setReadOnly(isReadOnly);
        connectionPropertyChanged = true; 
    }

    public final Object getCurrentShardingKey() {
        return currentShardingKey;
    }

    public final Object getCurrentSuperShardingKey() {
        return currentSuperShardingKey;
    }

    /**
     * Updates the value of the sharding keys.
     * 
     * @param shardingKey the new sharding key.
     * @param superShardingKey the new super sharding key. The 'unchanged' constant can be used to avoid changing it.
     */
    public final void setShardingKeys(Object shardingKey, Object superShardingKey) throws SQLException {
        if (mcf.beforeJDBCVersion(JDBCRuntimeVersion.VERSION_4_3))
            throw new SQLFeatureNotSupportedException();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Set sharding key/super sharding key to " + shardingKey + "/" + superShardingKey); 

        mcf.jdbcRuntime.doSetShardingKeys(sqlConn, shardingKey, superShardingKey);
        currentShardingKey = shardingKey;
        if (superShardingKey != JDBCRuntimeVersion.SUPER_SHARDING_KEY_UNCHANGED)
            currentSuperShardingKey = superShardingKey;
        connectionPropertyChanged = true;
    }

    /**
     * Updates the value of the sharding keys after validating them.
     *
     * @param shardingKey the new sharding key.
     * @param superShardingKey the new super sharding key. The 'unchanged' constant can be used to avoid changing it.
     * @param timeout number of seconds within which validation must be done. 0 indicates no timeout.
     * @return true if the sharding keys are valid and were successfully set on the connection. Otherwise, false.
     */
    public final boolean setShardingKeysIfValid(Object shardingKey, Object superShardingKey, int timeout) throws SQLException {
        if (mcf.beforeJDBCVersion(JDBCRuntimeVersion.VERSION_4_3))
            throw new SQLFeatureNotSupportedException();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Set sharding key/super sharding key to " + shardingKey + "/" + superShardingKey + " within " + timeout + " seconds");

        boolean updated = mcf.jdbcRuntime.doSetShardingKeysIfValid(sqlConn, shardingKey, superShardingKey, timeout);

        if (updated) {
            currentShardingKey = shardingKey;
            if (superShardingKey != JDBCRuntimeVersion.SUPER_SHARDING_KEY_UNCHANGED)
                currentSuperShardingKey = superShardingKey;
            connectionPropertyChanged = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "successful? " + updated);
        return updated;
    }

    /**
     * Returns the connectionRequestInfo passed in during the construction of the object.
     * This ConnectionRequestInfo object contains the updated values for Connection properties
     * due to any modificiations made to the ManagedConnection.
     * 
     * @returns ConnectionRequestInfo
     */
    public final ConnectionRequestInfo getConnectionRequestInfo() {
        return cri;
    }

    public final void setCRI(WSConnectionRequestInfoImpl newCRI)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setCRI to ", newCRI);

        cri = newCRI;
    }

    /**
     * Returns the subject passed in during the construction of this object
     * 
     * @return Subject
     */

    public final Subject getSubject() {
        return subject;
    }

    /**
     * Updates the value of the typeMap property.
     * 
     * @param typeMap the new type map.
     */
    public final void setTypeMap(Map<String, Class<?>> typeMap) throws SQLException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Set TypeMap to " + typeMap); 
        try{
            sqlConn.setTypeMap(typeMap);
        } catch(SQLFeatureNotSupportedException nse){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "supportsGetTypeMap false due to ", nse);
            mcf.supportsGetTypeMap = false;
            throw nse;
        } catch(UnsupportedOperationException uoe){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "supportsGetTypeMap false due to ", uoe);
            mcf.supportsGetTypeMap = false;
            throw uoe;
        }
        connectionPropertyChanged = true; 
    }

    /**
     * This method returns the SQLJ ConnectionContext. This method is only called by
     * the WSJccConnection class. The ConnectionContext caches all the RTStatements
     * 
     * @param DefaultContext the sqlj.runtime.ref.DefaultContext class
     * @return sqlj.runtime.ref.DefaultContext
     * @exception SQLException - failed to create SQLJ Connection Context
     **/
    public final Object getSQLJConnectionContext(Class<?> DefaultContext, WSConnectionManager cm) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "getSQLJConnectionContext");
        if (sqljContext == null) {
            // get the sqljContext if necessary. let the helper handle it
            // the helper does the sqlex mapping
            try {
                sqljContext = helper.getSQLJContext(this, DefaultContext, cm); 
            } catch (SQLException se) {
                // the helper already did the sqlj error mapping and ffdc..
                if (isTraceOn && tc.isEntryEnabled()) 
                    Tr.exit(this, tc, "getSQLJConnectionContext", se);
                throw se;
            }
        }
        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "getSQLJConnectionContext", sqljContext);
        return sqljContext;
    }

    /**
     * <p>This method does a connection test to check whether the connection is good or not.
     * If the connection test is not successful, a ResourceException will be thrown.</p>
     * 
     * @return true if found to be valid, otherwise false.
     */
    public boolean validate() {
        int validationTimeout = dsConfig.get().validationTimeout;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "validate", "timeout: " + validationTimeout);

        try {
            if (validationTimeout >= 0 && mcf.jdbcDriverSpecVersion >= 40) // validation by timeout
            {
                // Validation by timeout is available only on JDBC 4.0 & higher
                if (!sqlConn.isValid(validationTimeout)) 
                {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(this, tc, "validate", false);
                    return false;
                }
            } else // validation by SQL query for JDBC drivers at spec level prior to v4.0
            {
                Statement stmt = sqlConn.createStatement();
                try {
                    stmt.executeQuery("SELECT 1").close(); // if this fails, the SQLException is checked for stale
                } finally {
                    stmt.close();
                }
            }

            //  - if currentAutoCommit is false, rollback the connection.
            if (!currentAutoCommit) {
                if (isTraceOn && tc.isDebugEnabled()) 
                    Tr.debug(this, tc, "AutoCommit is false.  Rolling back the connection after validation");
                sqlConn.rollback();
            }

            // Clean up the connection.
            helper.doConnectionCleanup(sqlConn);

            // Clear the warning.
            sqlConn.clearWarnings();

        } catch (SQLException sqle) {
            // No FFDC coded needed

            // The SQLException could be a StaleConnectionException or a normal SQLException
            // resulted from invalid SQLStatement.

            ResourceException resX = AdapterUtil.translateSQLException(sqle, this, false, getClass()); // no need to fire connection event here, J2C handles on pretest failure

            // Remove this if the helpers ever start tracing exceptions.
            if (isTraceOn && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "validate", AdapterUtil.getStackTraceWithState(sqle));

            if (helper.isConnectionError(sqle)) {
                // Don't do any cleanup
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "validate", false);
                return false;
            } else if (!(resX instanceof SecurityException)) {
                // This exception is due to an invalid SQL statement.

                //  - if currentAutoCommit is false, rollback the connection.
                if (!currentAutoCommit) {
                    try {
                        if (isTraceOn && tc.isDebugEnabled()) 
                            Tr.debug(this, tc, "AutoCommit is false.  Rolling back the connection after validation");
                        sqlConn.rollback();
                    } catch (SQLException rollbackEx) {
                        // No FFDC coded needed

                        // There is a possibility that now the connection is stale.

                        if (helper.isConnectionError(rollbackEx)) {
                            if (isTraceOn && tc.isEntryEnabled())
                                Tr.exit(this, tc, "validate", AdapterUtil.getStackTraceWithState(rollbackEx));
                            return false;
                        }
                    }
                }

                try {
                    helper.doConnectionCleanup(sqlConn);
                } catch (SQLException cleanEx) {
                    // No FFDC coded needed

                    // There is a possibility that now the connection is stale.

                    if (helper.isConnectionError(cleanEx)) {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(this, tc, "validate", AdapterUtil.getStackTraceWithState(cleanEx));
                        return false;
                    }
                }

                try {
                    sqlConn.clearWarnings();
                } catch (SQLException cleanEx) {
                    // No FFDC coded needed

                    // There is a possibility that now the connection is stale.

                    if (helper.isConnectionError(cleanEx)) {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(this, tc, "validate", AdapterUtil.getStackTraceWithState(cleanEx));
                        return false;
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "validate", true);
        return true;
    }


    /**
     * Claim the unused managed connection as a victim connection,
     * which can then be reauthenticated and reused.
     */
    public void setClaimedVictim() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "marking this mc as _claimedVictim");
        _claimedVictim = true;

    }


    /**
     * Method used to return the toString representation of this object
     * 
     */
    @Override
    public String toString() {
        // this is the same as the default toString, but it doesn't contain the package name.
        return new StringBuilder("WSRdbManagedConnectionImpl")
                        .append('@')
                        .append(Integer.toHexString(hashCode()))
                        .toString();

    }
    
    /**
     * Set a schema for this managed connection.
     * @param schema The schema to set on the connection.
     */
    public void setSchema(String schema) throws SQLException {
        Transaction suspendTx = null;

        if (mcf.beforeJDBCVersion(JDBCRuntimeVersion.VERSION_4_1))
            throw new SQLFeatureNotSupportedException();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Set Schema to " + schema);
        // Global trans must be suspended for jdbc-4.1 getters and setters on zOS 
        if (AdapterUtil.isZOS() && isGlobalTransactionActive())
            suspendTx = suspendGlobalTran();

        try {
            mcf.jdbcRuntime.doSetSchema(sqlConn, schema);
            currentSchema = schema;
            connectionPropertyChanged = true;
        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            if (suspendTx != null)
                resumeGlobalTran(suspendTx);
        }
    }
    
    /**
     * Retrieve the schema being used by this managed connection.  
     * @return The schema used by this managed connection. <BR>
     * NOTE: If this method is called below JDBC version 4.1, null will be returned.
     */
    public String getSchema() throws SQLException {
        Transaction suspendTx = null;
        if (mcf.beforeJDBCVersion(JDBCRuntimeVersion.VERSION_4_1))
            return null;
        // Global trans must be suspended for jdbc-4.1 getters and setters on zOS 
        if (AdapterUtil.isZOS() && isGlobalTransactionActive())
            suspendTx = suspendGlobalTran();

        String schema;
        try {
            schema = mcf.jdbcRuntime.doGetSchema(sqlConn);
        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            if (suspendTx != null)
                resumeGlobalTran(suspendTx);
        }
        return schema;
    }
    
    public String getSchemaSafely() throws SQLException {
        if(!mcf.supportsGetSchema){
            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Returning default schema", defaultSchema);
            return defaultSchema;
        }
        Throwable x;
        try{
            return getSchema();
        } catch(AbstractMethodError e){
            // If we are running pre-Java7
            x = e;
        } catch(NoSuchMethodError e){
            // If the driver is pre-4.1
            x = e;
        } catch (SQLException e) {
            // In case the driver is not 4.1 compliant but says it is 
            String sqlMessge = e.getMessage() == null ? "" : e.getMessage();
            if (helper.isUnsupported(e))
                x = e;
            //try to catch any other variation of not supported, does not support, unsupported, etc.
            //this is needed by several JDBC drivers, but one known driver is DataDirect OpenEdge JDBC Driver
            else if(sqlMessge.contains("support"))
                x = e;
            else
                throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getSchema not supported due to " + x);
        mcf.supportsGetSchema = false;
        return defaultSchema;
    }


    @Override
    public void abort(Executor ex) throws Exception {
        if (mcf.beforeJDBCVersion(JDBCRuntimeVersion.VERSION_4_1))
          throw new SQLFeatureNotSupportedException();
        
        mcf.jdbcRuntime.doAbort(sqlConn, ex);
        setAborted(true);
    }
    
    @Override
    public boolean isAborted() {
        if (mcf.beforeJDBCVersion(JDBCRuntimeVersion.VERSION_4_1))
            return false;
        return aborted;
    }
    
    public void setAborted(boolean aborted) throws SQLFeatureNotSupportedException{
        if (mcf.beforeJDBCVersion(JDBCRuntimeVersion.VERSION_4_1))
          throw new SQLFeatureNotSupportedException();
        this.aborted = aborted;
    }
    
    public int getNetworkTimeout() throws SQLException {
        int timeOut;
        Transaction suspendTx = null;
        // Global trans must be suspended for jdbc-4.1 getters and setters on zOS 
        if (AdapterUtil.isZOS() && isGlobalTransactionActive())
            suspendTx = suspendGlobalTran();
        try {
            timeOut = mcf.jdbcRuntime.doGetNetworkTimeout(sqlConn);
        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            if (suspendTx != null)
                resumeGlobalTran(suspendTx);
        }
        return timeOut;
    }
    
    /**
     * Safely gets the NetworkTimeout.  This method differs from the original getNetworkTimeout() method
     * by returning a default value when before JDBC-4.1 or on a driver that does not support getNetworkTimeout(). 
     * @return The networkTimeout, or a default value if getNetworkTimeout is not supported by the driver, or
     * the current JDBC runtime is pre-JDBC-4.1.
     */
    public int getNetworkTimeoutSafely() throws SQLException {
        if(!mcf.supportsGetNetworkTimeout){
            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Returning default network timeout.", defaultNetworkTimeout);
            return defaultNetworkTimeout;
        }
        Throwable x;
        try{
            return getNetworkTimeout();
        } catch(AbstractMethodError e){
            // If we are running pre-Java7
            x = e;
        } catch(NoSuchMethodError e){
            // If the driver is pre-4.1
            x = e;
        } catch (SQLException e) {
            // In case the driver is not 4.1 compliant but says it is 
            String sqlMessge = e.getMessage() == null ? "" : e.getMessage();
            if (helper.isUnsupported(e))
                x = e;
            //try to catch any other variation of not supported, does not support, unsupported, etc.
            //this is needed by several JDBC drivers, but one known driver is DataDirect OpenEdge JDBC Driver
            else if(sqlMessge.contains("support"))
                x = e;
            else
                throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getNetworkTimeout support false due to " + x);
        mcf.supportsGetNetworkTimeout = false;
        return defaultNetworkTimeout;
    }
    
    public void setNetworkTimeout(Executor executor, int milliseconds)
                    throws SQLException {

        if (mcf.beforeJDBCVersion(JDBCRuntimeVersion.VERSION_4_1))
            throw new SQLFeatureNotSupportedException();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Set NetworkTimeout to " + milliseconds);

        Transaction suspendTx = null;
        // Global trans must be suspended for jdbc-4.1 getters and setters on zOS 
        if (AdapterUtil.isZOS() && isGlobalTransactionActive())
            suspendTx = suspendGlobalTran();
        try {
            mcf.jdbcRuntime.doSetNetworkTimeout(sqlConn, executor, milliseconds);
        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            if (suspendTx != null)
                resumeGlobalTran(suspendTx);
        }
        currentNetworkTimeout = milliseconds;
        connectionPropertyChanged = true;
    }

    private Transaction suspendGlobalTran() throws SQLException {
        EmbeddableWebSphereTransactionManager tm = mcf.connectorSvc.getTransactionManager();
        try {
            return tm.suspend();
        } catch (SystemException e) {
            throw new SQLException(e);
        }
    }
    
    private void resumeGlobalTran(Transaction t) throws SQLException {
        EmbeddableWebSphereTransactionManager tm = mcf.connectorSvc.getTransactionManager();
        try{
            tm.resume(t);
        } catch (Exception e){
            throw new SQLException(e);
        }
    }
}