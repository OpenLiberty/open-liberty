/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException; 
import java.sql.SQLInvalidAuthorizationSpecException; 
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.sql.ResultSet; 
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList; 
import java.util.Map; 
import java.util.Properties;
import java.util.Set;
import java.sql.PreparedStatement; 

import javax.naming.Context; 
import javax.sql.CommonDataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;

import org.ietf.jgss.GSSCredential;

import com.ibm.ejs.cm.logger.TraceWriter;

import javax.resource.ResourceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.adapter.WSConnectionManager;
import com.ibm.ws.jca.cm.AbstractConnectionFactoryService;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.DSConfig;
import com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException;
import com.ibm.ws.rsadapter.jdbc.WSJdbcStatement;

/**
 * Helper for generic relational databases, coded to the most common cases.
 * This class may be subclassed as needed for databases requiring different behavior.
 */
public class DatabaseHelper {
    // register the generic database trace needed for enabling database jdbc logging/tracing
    @SuppressWarnings("deprecation")
    private static final com.ibm.ejs.ras.TraceComponent databaseTc = com.ibm.ejs.ras.Tr.register("com.ibm.ws.database.logwriter", "WAS.database", null); 
    private static final TraceComponent tc = Tr.register(DatabaseHelper.class, "RRA", AdapterUtil.NLS_FILE); 
    private transient PrintWriter genPw = null; 

    /**
     * Default query timeout configured on the data source.
     */
    protected int defaultQueryTimeout;

    private String databaseProductName = null; 
    private String driverName; 
    int driverMajorVersion; 

    /**
     * The managed connection factory associated with this internal data store helper.
     */
    WSManagedConnectionFactoryImpl mcf;

    /**
     * This flag will cache the knowledge of whether holdability supported or not.
     * at the beginning we assume holdability is supported, if we get an exception when calling the getHolidablity
     * then we will mark the flag as false so that getHoldability is not called all the time
     */
    protected boolean holdabilitySupported = true; 

    private boolean setCursorNameSupported = true;

    /**
     * SQLException error codes that indicate a stale connection.
     */
    final Set<Integer> staleErrorCodes = new HashSet<Integer>();

    /**
     * SQLException SQL States that indicate a stale connection.
     */
    final Set<String> staleSQLStates = new HashSet<String>();

    /**
     * Indicates if the JDBC driver alters the autocommit value upon XAResource.end.
     */
    boolean xaEndResetsAutoCommit;

    /**
     * Construct a helper for generic relational databases.
     * 
     * @param mcf managed connection factory
     */
    DatabaseHelper(WSManagedConnectionFactoryImpl mcf) {
        this.mcf = mcf;

        Properties props = mcf.dsConfig.get().vendorProps;
        Object queryTimeout = props.get(DSConfig.QUERY_TIMEOUT);
        try {
            defaultQueryTimeout = queryTimeout instanceof Number ? ((Number) queryTimeout).intValue() :
                                  queryTimeout instanceof String ? Integer.parseInt((String) queryTimeout) :
                                  0;
        } catch (NumberFormatException x) {
            // error paths already covered by data source properties processing code
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "init", "Default query timeout=" + defaultQueryTimeout);

        // X/OPEN standard SQLSTATE mappings
        Collections.addAll(staleSQLStates,
                           "08001",
                           "08003",
                           "08006",
                           "08S01",
                           "40003",
                           "55032",
                           "S1000");
    }

    /**
     * Indicates if setAutoCommit requests should always be sent to the JDBC driver, even
     * if the same as the current value.
     * When a value of false is returned by this method,
     * we might choose not to send setAutoCommit requests to the JDBC
     * driver when the current value should already be correct.
     * Returning a value of true is useful only as a workaround for an Oracle 9i data
     * integrity bug.
     * 
     * @return true or false, indicating whether or not to always propagate
     *         setAutoCommit requests to the JDBC driver.
     */
    public boolean alwaysSetAutoCommit() {
        return false;
    }

    /**
     * <p>This method is used to clean up a connection before it is returned to the connection
     * pool for later reuse. WebSphere automatically resets all standard connection
     * properties (fields for which getters and setters are defined on
     * <code>java.sql.Connection</code>).
     * This method can be used to reset other properties proprietary to a specific
     * JDBC driver/database, preparing the connection for
     * reuse.</p>
     * 
     * <p>Use the provided connection to create
     * and execute statements for the purpose of cleaning up the connection. Any statements
     * created within the <code>doConnectionCleanup</code> method must be explicitly closed
     * within the <code>doConnectionCleanup</code> method. The
     * <code>doConnectionCleanup</code> method must never close the
     * connection being cleaned up.</p>
     * 
     * <p>If <i>any</i> standard connection properties are modified in this method, a value
     * of true must be returned, indicating to WebSphere that at least one standard
     * connection property was modified. A value of false should be returned only if
     * <i>no</i> standard connection properties were modified.</p>
     * 
     * @param conn the connection to attempt to cleanup.
     * @return true if <i>any</i> standard connection property was modified, otherwise false.
     * @exception SQLException if an error occurs while cleaning up the connection.
     */
    public boolean doConnectionCleanup(Connection conn) throws SQLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "doConnectionCleanup: no cleanup is done return false");

        return false;
    }

    /**
     * <p>This method configures a connection before first use. This method is invoked only
     * when a new connection to the database is created. It is not invoked when connections
     * are reused from the connection pool.</p>
     * 
     * <p>The following actions are prohibited in this method:</p>
     * <ol>
     * <li>Changing any standard connection properties found on the
     * <code>java.sql.Connection</code> API. This includes TransactionIsolationLevel,
     * AutoCommit, Catalog, ReadOnly, TypeMap, and Holdability.</li>
     * <li>Closing the connection.</li>
     * </ol>
     * 
     * @param conn the connection to set up.
     * @exception SQLException if connection setup cannot be completed successfully.
     */
    public void doConnectionSetup(Connection conn) throws SQLException {
    }

    /**
     * Indicates whether or not the JDBC vendor statement implementation caches a copy of the transaction isolation level.
     * 
     * @return true if statements cache the isolation level, otherwise false.
     */
    public boolean doesStatementCacheIsoLevel() {
        return false;
    }

    /**
     * <p>This method cleans up a statement before the statement is placed in the statement
     * cache. This method is called only
     * for statements being cached. It is called when at least one of the
     * following statement properties has changed,</p>
     * 
     * <ul>
     * <li>cursorName</li>
     * <li>fetchDirection</li>
     * <li>maxFieldSize</li>
     * <li>maxRows</li>
     * <li>queryTimeout</li>
     * </ul>
     * 
     * <p>The generic implementation for this method resets all five of the
     * properties listed above.</p>
     * 
     * <p>The following operations do not need to be included in the statement cleanup because
     * they are automatically performed by WebSphere when caching statements:</p>
     * 
     * <ul>
     * <li><code>setFetchSize(0)</code></li>
     * <li><code>clearParameters()</code></li>
     * <li><code>clearWarnings()</code></li>
     * </ul>
     * 
     * <p>A helper class implementing this method can choose to do additional cleanup for
     * the statement. However, this should never include closing the statement because the
     * statement is intended to be cached.</p>
     * 
     * @param stmt the PreparedStatement.
     * 
     * @exception SQLException if an error occurs cleaning up the statement.
     */
    public void doStatementCleanup(PreparedStatement stmt) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (setCursorNameSupported) {
            try{        
                stmt.setCursorName(null);            
            } catch (SQLFeatureNotSupportedException supportX) {
                setCursorNameSupported = false;
                if (isTraceOn && tc.isDebugEnabled()) Tr.debug(tc, "Statement.setCursorName() is not supported.", supportX);
            }
        }
        
        stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
        stmt.setMaxFieldSize(0);
        stmt.setMaxRows(0);

        Integer queryTimeout = mcf.dsConfig.get().queryTimeout;
        if (queryTimeout == null)
            queryTimeout = defaultQueryTimeout;
        stmt.setQueryTimeout(queryTimeout);
    }

    /**
     * Determines if the exception indicates failover occurred.
     * 
     * @param sqlX an exception that might indicate connection failover.
     * @return false because there are no generic SQL states or error codes for failover.
     */
    public boolean failoverOccurred(SQLException sqlX) {
        return false;
    }

    /**
     * This method returns a default isolation level based on the database backend.
     * 
     * @return default isolation level
     */
    public int getDefaultIsolationLevel() {
        return Connection.TRANSACTION_READ_COMMITTED;
    }

    /**
     * @return FALSE, assuming a generic DataSource is not RRS-enabled.
     */
    public boolean getRRSTransactional() {
        return false;
    }

    /**
     * @return "NOTALLOWED", assuming a generic DataSource does not allow Thread Identity
     *         Support.
     */
    public int getThreadIdentitySupport() {
        return AbstractConnectionFactoryService.THREAD_IDENTITY_NOT_ALLOWED;
    }

    /**
     * @return FALSE, indicating a generic DataSource does not support "synch to thread"
     *         for the allocateConnection, i.e., push an ACEE corresponding to the current
     *         java Subject on the native OS thread.
     */
    public boolean getThreadSecurity() {
        return false;
    }

    /**
     * Returns a trace component for supplemental JDBC driver level trace.
     * If supplemental JDBC driver level trace is not wanted, this method should return NULL.
     * 
     * @return the trace component for supplemental trace, or NULL if unwanted.
     */
    public com.ibm.ejs.ras.TraceComponent getTracer() {
        return databaseTc;
    }

    /**
     * Object the update count as a long value, using the JDBC 4.2 getLargeUpdateCount if possible,
     * otherwise using getUpdateCount.
     * Copied from the JavaDoc API for java.sql.Statement.getUpdateCount/getLargeUpdateCount:
     * Retrieves the current result as an update count; if the result is a ResultSet object
     * or there are no more results, -1 is returned. This method should be called only once
     * per result.
     * 
     * @param stmt the statement.
     * @return the current result as an update count; -1 if the current result is a ResultSet
     *         object or there are no more results.
     * @throws SQLException if a database access error occurs or this method is called on a closed Statement.
     */
    public long getUpdateCount(WSJdbcStatement stmt) throws SQLException {
        return stmt.getCompatibleUpdateCount();
    }

    /**
     * This method is used to get a correlator from the database to log in the websphere tracing
     * currently only DB2 supports this method.
     * 
     * @return byte[]
     */
    public String getCorrelator(WSRdbManagedConnectionImpl mc) throws SQLException { 
        // no-op here
        return null;
    }

    /**
     * This method provides a plug-in point for providing meaningful logging information for an
     * <code>XAException</code>. The information can include details of the original
     * exception that caused the <code>XAException</code>, if applicable.
     * We use this method to obtain trace information for <code>XAException</code>.
     * 
     * @param xae the <code>XAException</code>.
     * @return detailed information about the <code>XAException</code>, for inclusion in trace.
     */
    public String getXAExceptionContents(XAException x) {
        StringBuilder xsb = new StringBuilder(200);
        Throwable cause = x.getCause();
        if (cause != null) {
            String EOLN = AdapterUtil.EOLN;
            xsb.append(EOLN).append("Caused by ").append(cause.getClass().getName()).append(": ").append(cause.getMessage()).append(EOLN);
            if (cause instanceof SQLException) {
                SQLException sqlX = (SQLException) cause;
                xsb.append("The error code is: ").append(sqlX.getErrorCode()).append(EOLN);
                xsb.append("The SQL State is: ").append(sqlX.getSQLState()).append(EOLN);
            }
        }
        return (xsb.toString());
    }

    /**
     * This method determines whether a <code>SQLException</code> indicates a stale connection error.
     * 
     * @param ex the <code>SQLException</code> to check.
     * @return true if the exception indicates a stale connection error, otherwise false.
     */
    public boolean isConnectionError(SQLException ex) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "isConnectionError", ex);

        // Maintain a set in order to check for cycles
        Set<Throwable> chain = new HashSet<Throwable>();

        boolean stale = false;
        for (Throwable t = ex; t != null && !stale && chain.add(t); t = t.getCause()) {
            SQLException sqlX = t instanceof SQLException ? (SQLException) t : null;
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "checking " + t,
                         sqlX == null ? null : sqlX.getSQLState(),
                         sqlX == null ? null : sqlX.getErrorCode());
            if (sqlX != null)
                stale |= sqlX instanceof SQLRecoverableException ||
                         sqlX instanceof SQLNonTransientConnectionException ||
                         sqlX instanceof SQLTransientConnectionException && failoverOccurred(sqlX) ||
                         staleErrorCodes.contains(sqlX.getErrorCode()) ||
                         staleSQLStates.contains(sqlX.getSQLState());
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "isConnectionError", stale);

        return stale;
    }

    /**
     * @return true if the exception or a cause exception in the chain is known to indicate a stale statement. Otherwise false.
     */
    public boolean isStaleStatement(SQLException x) {
        return false;
    }

    /**
     * This method checks if enabling jdbc logging is in order(i.e. if a call to enableJDBCLogging is needed).
     * The boundaries by which the method is called: <br>
     * <li> when a new connection is created </li>
     * 
     * @param mcf WSManagedConnectionFactoryImpl
     * @return boolean true if trace should be enabled, false otherwise
     */
    public boolean shouldTraceBeEnabled(WSManagedConnectionFactoryImpl mcf) {
        // here will base this on the mcf since, the value is enabled for the system
        // as a whole
        if (databaseTc.isDebugEnabled() && !mcf.loggingEnabled) {
            return true;
        }
        return false;
    }

    /**
     * @see com.ibm.ws.rsadapter.spi.DatabaseHelper#shouldTraceBeEnabled(com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl)
     */
    public boolean shouldTraceBeEnabled(WSRdbManagedConnectionImpl mc) {
        //the mc.mcf will be passed to the shouldTraceBeEnabled method that handles WSManagedConnectionFactoryImpl and 
        //get the boolean returned
        return (this.shouldTraceBeEnabled(mc.mcf));
    }

    /**
     * This method checks if disabling jdbc logging is needed. (i.e. if a call to disableJdbcLogging is needed).
     * The boundaries by which the method is called: <br>
     * <li> before we hand out a connection in mc.getConnection </li>
     * <li> during cleanup of the connection </li>
     * 
     * @param mc WSRdbManagedConnectionImpl
     * @return boolean true if trace should be disabled, false otherwise.
     */
    public boolean shouldTraceBeDisabled(WSRdbManagedConnectionImpl mc) {
        if (!databaseTc.isDebugEnabled() && mc.mcf.loggingEnabled)
            return true;

        return false;
    }

    /**
     * This method is used to disable jdbc logging. The boundaries by which the method may be called: <br>
     * <li> before we hand out a connection in mc.getConnection </li>
     * <li> during cleanup of the connection </li>
     * The way jdbc logging is disabled will vary from one backend to another; some will be
     * on the connection, some will be on the system as a whole, and others will be on the datasource.
     * 
     * 
     * @param mc WSRdbManagedConnectionImpl
     * @exception ResourceException
     */
    public void disableJdbcLogging(WSRdbManagedConnectionImpl mc) throws ResourceException {
        mc.mcf.reallySetLogWriter(null);
        mc.mcf.loggingEnabled = false;
    }

    /**
     * This method is used to enable jdbc logging. The boundaries by which the method may be called: <br>
     * <li> when creating the connection </li>
     * The way jdbc logging is enabled will vary from one backend to another; some will be enabled
     * on the connection, some will be enabled on the system as a whole, and others will be on the datasource.
     * 
     * @param mc WSRdbManagedConnectionImpl
     * @exception ResourceException
     */
    public void enableJdbcLogging(WSManagedConnectionFactoryImpl mcf) throws ResourceException {
        // in the Generic case (internalGeneric) just pass the pw

        PrintWriter pw = getPrintWriter(); 

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "enabling logging", mcf, pw);
        mcf.reallySetLogWriter(pw); 
        mcf.loggingEnabled = true;
    }

    /**
     * This method is used to enable jdbc logging. The boundaries by which the method may be called: <br>
     * <li> before we hand out a connection in mc.getConnection </li>
     * <li> during cleanup of the connection </li>
     * The way jdbc logging is enabled will vary from one backend to another; some will be enabled
     * on the connection, some will be enabled on the system as a whole, and others will be on the datasource.
     * 
     * @param mc WSRdbManagedConnectionImpl
     * @exception ResourceException
     */
    public void enableJdbcLogging(WSRdbManagedConnectionImpl mc) throws ResourceException {
        this.enableJdbcLogging(mc.mcf);
    }

    /**
     * This returns a <code>PrintWriter</code> for a specific backend, based on the trace settings
     * 
     * @return PrintWriter
     * @exception ResourceException if something goes wrong.
     */
    public PrintWriter getPrintWriter() throws ResourceException { 
        //not synchronizing here since there will be one helper
        // and most likely the setting will be serially, even if its not, 
        // it shouldn't matter here (tracing).
        if (genPw == null) {
            genPw = new PrintWriter(new TraceWriter(databaseTc), true);
        }
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "returning", genPw);
        return genPw;
    }

    /**
     * <p>This method is used to do special handling for readOnly when method setReadOnly is called.</p>
     * 
     * @param managedConn WSRdbManagedConnectionImpl object
     * @param readOnly The readOnly value going to be set
     * @param externalCall indicates if the call is done by WAS, or by the user application.
     */
    public void setReadOnly(WSRdbManagedConnectionImpl managedConn, boolean readOnly, boolean externalCall)
                    throws SQLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setReadOnly", managedConn, readOnly, externalCall);
        managedConn.setReadOnly(readOnly);
    }

    /**
     * <p>This method is used to get the holdability value of the connection object.
     * If there is an exception thrown, we need to check whether the exception is due to
     * the fact that this getHoldability feature is not supported. If that's the case,
     * 0 is returned.</p>
     * 
     * <p>For most datasources, when the getHoldability feature is not supported, an
     * AbstractMethodError will be thrown.</p>
     */
    public int getHoldability(Connection conn) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "getHoldability", AdapterUtil.toString(conn));

        int holdability = 0;

        try {
            if (holdabilitySupported) 
            {
                holdability = conn.getHoldability();
                return holdability;
            }
            return 0; // holdability is not supported
        } catch (AbstractMethodError ame) {
            // No FFDC needed
            // This JDBC driver doesn't support Connection.getCursorHoldability().

            //  - Change Tr.info to Tr.debug in getHoldability
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getHoldability",
                         "getHoldability is not supported in this JDBC driver. Encounter a java.lang.AbstractMethodError");
            holdabilitySupported = false; 
            return 0;
        }
        catch (UnsupportedOperationException uex) //temporary fix
        {
            // No FFDC needed
            // This JDBC driver doesn't support Connection.getCursorHoldability().

            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getHoldability",
                         "getHoldability is not supported in this JDBC driver. Encounter a java.lang.UnsupportedOperationException");
            holdabilitySupported = false;
            return 0;
        }
        catch (SQLException sqe) {
            if ((isConnectionError(sqe))) {
                throw sqe; // if this is a stale we need to throw exception here.
            }

            // Some DB drivers throw SQLException when the JDBC version is 2.x.
            // Holdability is not supported in 2.0
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "getHoldability is not supported in this JDBC driver. Encounter a java.sql.SQLException", sqe);
            }
            holdabilitySupported = false;
            return 0;
        }

    }

    /**
     * this method returns the indicator whether the backend supports the isolation level switching on
     * a connection
     * 
     * @return boolean : indicates the backend whether supports the isolation level switching
     */
    public boolean isIsolationLevelSwitchingSupport() {
        return false;
    }

    /**
     * Allow for special handling of Oracle prepared statement setBytes
     * This method just does the normal setBytes call, Oracle helper overrides it
     */
    public void psSetBytes(PreparedStatement pstmtImpl, int i, byte[] x) throws SQLException {
        pstmtImpl.setBytes(i, x);
    }

    /**
     * Allow for special handling of Oracle prepared statement setString
     * This method just does the normal setString call, Oracle helper overrides it
     */
    public void psSetString(PreparedStatement pstmtImpl, int i, String x) throws SQLException {
        pstmtImpl.setString(i, x);
    }

    /**
     * Set the database product name
     * 
     * @param databaseProductName
     */
    public void setDatabaseProductName(String databaseProductName) { 
        this.databaseProductName = databaseProductName;
    }

    /**
     * Set the major version number of the JDBC driver.
     * 
     * @param version the version.
     */
    void setDriverMajorVersion(int version) {
        driverMajorVersion = version;
    }

    /**
     * Set the JDBC driver name.
     * 
     * @param name the name.
     */
    void setDriverName(String name) {
        driverName = name;
    }

    /**
     * Get the database product name
     * 
     * @return String the database product name
     */
    public String getDatabaseProductName() { 
        return databaseProductName;
    }

    /**
     * Get the major version number of the JDBC driver.
     * 
     * @return the version.
     */
    int getDriverMajorVersion() {
        return driverMajorVersion;
    }

    /**
     * Get the name of the JDBC driver.
     * 
     * @return the name.
     */
    String getDriverName() {
        return driverName;
    }

    /**
     * This method will be used to set the client information on the mc. This method will only
     * be called when reassociating the handle, not using the regular method, because we don't
     * deal with properties object (expensive) and don't want to go through the other logic in the
     * methods, this should be a straight forward set.
     * The clientInfoArray entries will have the following order:<br>
     * 0- clientId, 1- clientLocation, 2- applicationname, 3- accounInfo <br> 
     * 
     * @param clientInfoArray String[]
     * @param mc WSRdbManagedConnectionImpl
     * @param explicitcall boolean indicates if the clientInfo was explicitly set
     * @exception SQLException
     */
    public void setClientInformationArray(String[] clientInfoArray, WSRdbManagedConnectionImpl mc, boolean explicitCall) throws SQLException {
        // no op                
    }

    /**
     * This method is used to reset the client information on the backend database
     * connection. Information will be reset only if it has been set.
     * 
     * @param mc WSRdbManagedConnectionImpl to reset the client information on
     * @exception SQLException
     *                java.sql.SQLException if a problem happens during resetting the client information on the database connection
     */
    public void resetClientInformation(WSRdbManagedConnectionImpl mc) throws SQLException {
        if (mc.mcf.jdbcDriverSpecVersion >= 40 && (mc.clientInfoExplicitlySet || mc.clientInfoImplicitlySet)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.entry(this, tc, "resetClientInformation", mc);

            try {
                mc.sqlConn.setClientInfo(mc.mcf.defaultClientInfo);
                mc.clientInfoExplicitlySet = false;
                mc.clientInfoImplicitlySet = false;
            } catch (SQLException ex) {
                FFDCFilter.processException(
                                            ex, getClass().getName() + "resetClientInformation", "780", this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(this, tc, "resetClientInformation", ex);

                throw AdapterUtil.mapSQLException(ex, mc);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "resetClientInformation");
        }
    }

    /**
     * This method returns a sqljConnectContext. It will go to DB2 to get the connection Context.
     * We need to create a new WSJccConnection to get the phsyical sqlj context.So the sqlj runtime
     * will use our WSJccConnection to do the work.
     * 
     * @param a managedConnection
     * @param DefaultContext the sqlj.runtime.ref.DefaultContext class
     * @return a physical sqlj connectionContext for DB2 only or return null for other database
     * @exception a SQLException if can't get a DefaultContext
     **/
    public Object getSQLJContext(WSRdbManagedConnectionImpl mc, Class<?> DefaultContext, WSConnectionManager connMgr) throws SQLException {
        return null;
    }

    /**
     * This method is used to get the metadata information from the connection and displaying it along with any other
     * connection related information
     * 
     * @param conn java.sql.Connection
     * @param mcf managed connection factory
     * @throws java.sql.SQLException
     */
    public void gatherAndDisplayMetaDataInfo(Connection conn, WSManagedConnectionFactoryImpl mcf) throws SQLException
    {
        java.sql.DatabaseMetaData mData = conn.getMetaData();

        String databaseProductName = mData.getDatabaseProductName();
        String driverName = mData.getDriverName(); 

        String driverVersion = null;
        String databaseProductVersion = null;
        try {
            driverVersion = mData.getDriverVersion();
            databaseProductVersion = mData.getDatabaseProductVersion();
        } catch (Exception e) {
            // Ignore any Runtime errors as the info collected are solely for FFDC purpose.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Exception occurred while getting metaData info. Exception is: ", e);
            }
        }

        setDatabaseProductName(databaseProductName);
        setDriverName(driverName); 
        setDriverMajorVersion(mData.getDriverMajorVersion()); 

        Tr.info(tc, "DB_PRODUCT_NAME", databaseProductName);
        Tr.info(tc, "DB_PRODUCT_VERSION", databaseProductVersion); 
        Tr.info(tc, "JDBC_DRIVER_NAME", driverName); 
        Tr.info(tc, "JDBC_DRIVER_VERSION", driverVersion); 

        try {
            if (mData.supportsResultSetHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT) &&
                mData.supportsResultSetHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Holdabiliy is supported");
                }
                holdabilitySupported = true;
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Holdability not supported");
                }
                holdabilitySupported = false;
            }

        } catch (Throwable x) {
            if (x instanceof SQLException) {
                SQLException sqe = (SQLException) x;
                if ((isConnectionError(sqe))) {
                    throw sqe; // if this is a stale we need to throw exception here.
                }

                // Some DB drivers throw SQLException when the JDBC version is 2.x.
                // Holdability is not supported in 2.0

                holdabilitySupported = false;
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "getHoldability is not supported in this JDBC driver. Encounter a java.sql.SQLException", sqe);
                }
            } else 
            {
                //NO FFDC needed
                // just log it and do nothing
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "caught an exception when testing the holdability from metadata, will call the holdability itself to know if supported", x);
                }

            }
        }
        try {
            mcf.jdbcDriverSpecVersion = mData.getJDBCMajorVersion() * 10;
        } catch (AbstractMethodError methError) {
            // No FFDC code needed. JDBC driver version is less than 3.0.
            mcf.jdbcDriverSpecVersion = 20;
        }
        catch (UnsupportedOperationException supportX) // raised by old Sybase driver
        {
            // No FFDC code needed. Driver is not JDBC 3.0 compliant because it does not
            // support getJDBCMajorVersion,
            mcf.jdbcDriverSpecVersion = 20;
        }
        catch (SQLException sqlX) {
            // No FFDC code needed.
            // Informix driver throws SQLException when the JDBC version is 2.x.

            mcf.jdbcDriverSpecVersion = 20;

            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "metadata.getJDBCMajorVersion", sqlX);

            // Need to fail if it was a connection error.
            if (isConnectionError(sqlX))
                throw sqlX;
        }

        if (mcf.jdbcDriverSpecVersion > 30)
            try {
                mcf.jdbcDriverSpecVersion += mData.getJDBCMinorVersion();
            } catch (SQLException x) {
            } catch (UnsupportedOperationException x) {
            }

        if (mcf.jdbcDriverSpecVersion >= 40)
            try 
            {
                // Reset to the default client info of the data source (based on an initial connection),
                // instead of using the default values for the JDBC driver--which could differ from the
                // defaults specified for the data source.
                Properties clientInfo = conn.getClientInfo();
                if (clientInfo != null)
                    mcf.defaultClientInfo.putAll(clientInfo);
            }
            catch (SQLFeatureNotSupportedException featureX) {
                // No FFDC code needed.
                // The JDBC 4.0 driver ought to implement the getClientInfOProperties method,
                // but doesn't.  Issue a warning, but tolerate the error so that WAS can still
                // interoperate with the JDBC driver.
                Tr.warning(tc, "FEATURE_NOT_IMPLEMENTED", "java.sql.Connection.getClientInfo");
            }
            catch (AbstractMethodError methErr) {
                // No FFDC code needed.
                // Work around a bug in Oracle where the driver reports "10" instead
                // of the JDBC specification level.
                // If the JDBC 4.0 metadata operation cannot be found on the driver,
                // then it must really be a JDBC 3.0 driver.
                if (tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "JDBC spec level, " + mcf.jdbcDriverSpecVersion +
                                             ", reported by the driver is not valid. Using 30 (3.0) instead.");
                mcf.jdbcDriverSpecVersion = 30;
            }
            catch (UnsupportedOperationException operationX) 
            {
                // No FFDC code needed.
                // Returned by Sybase jconn4.jar versions that are older than jConnect-7.07.
                // The JDBC 4.0 driver ought to throw SQLFeatureNotSupportedException,
                // but doesn't.  Issue a warning, but tolerate the error so that WAS can still
                // interoperate with the JDBC driver.
                Tr.warning(tc, "FEATURE_NOT_IMPLEMENTED", "java.sql.Connection.getClientInfo");
            }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "JDBC spec version implemented by driver", mcf.jdbcDriverSpecVersion);
    }

    /**
     * This method is used to check if the connection is involved in a transaction or not. This method checks
     * the database directly and doesn't use the WebSphere internal tracking mechanism. This is used to catch
     * cases where the database will start a transaction implicitly.
     * 
     * @param conn
     * @return boolean
     * @throws SQLException
     */
    public boolean isInDatabaseUnitOfWork(Connection conn) throws SQLException {
        Tr.info(tc, "UNSUPPORTED_METHOD", "isInDatabaseUnitOfWork");
        throw new SQLException("method not supported for this backend database");
    }

    /**
     * Get a Pooled or XA Connection from the specified DataSource.
     * A null userName indicates that no user name or password should be provided.
     *
     * @param dataSource XADataSource or ConnectionPoolDataSource
     * @param String userName to get the pooled connection
     * @param String password to get the pooled connection
     * @param is2Phase indicates what type of Connection to retrieve (one-phase or two-phase).
     * @param WSConnectionRequestInfoImpl connection request information, possibly including sharding keys
     * @param useKerberos a boolean that specifies if kerberos should be used when getting a connection to the database.
     * @param gssCredential the kerberose Credential to be used if useKerberos is true.
     * 
     * @return Object[] that contains Pooled or XA Connection for its first element and cookie for its second
     * @throws ResourceException if an error occurs obtaining the is2PhaseEnabled value does
     *             not match the DataSource type.
     */
    public ConnectionResults getPooledConnection(final CommonDataSource ds, String userName, String password, final boolean is2Phase, 
                                                 final WSConnectionRequestInfoImpl cri, boolean useKerberos, Object gssCredential) throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "getPooledConnection",
                     AdapterUtil.toString(ds), userName, "******", is2Phase ? "two-phase" : "one-phase", cri, useKerberos, gssCredential);

        // if kerberose is set then issue a warning that no special APIs are used instead, 
        // a getConnection() without username/password will be used.
        // to get a connection.
        if (useKerberos) {
            Tr.warning(tc, "KERBEROS_NOT_SUPPORTED_WARNING");
        }

        try {
            final String user = userName == null ? null : userName.trim();
            final String pwd = password == null ? null : password.trim();

            PooledConnection pConn = AccessController.doPrivileged(new PrivilegedExceptionAction<PooledConnection>() {
                public PooledConnection run() throws SQLException {
                    boolean buildConnection = cri.ivShardingKey != null || cri.ivSuperShardingKey != null;
                    if (is2Phase)
                        if (buildConnection)
                            return mcf.jdbcRuntime.buildXAConnection((XADataSource) ds, user, pwd, cri);
                        else if (user == null)
                            return ((XADataSource) ds).getXAConnection();
                        else
                            return ((XADataSource) ds).getXAConnection(user, pwd);
                    else
                        if (buildConnection)
                            return mcf.jdbcRuntime.buildPooledConnection((ConnectionPoolDataSource) ds, user, pwd, cri);
                        else if (user == null)
                            return ((ConnectionPoolDataSource) ds).getPooledConnection();
                        else
                            return ((ConnectionPoolDataSource) ds).getPooledConnection(user, pwd);
                }
            });

            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "getPooledConnection", AdapterUtil.toString(pConn));

            return new ConnectionResults(pConn, null); 
        } catch (PrivilegedActionException pae) {
            FFDCFilter.processException(pae.getException(), getClass().getName(), "1298");

            ResourceException resX = new DataStoreAdapterException("JAVAX_CONN_ERR", pae.getException(), DatabaseHelper.class, is2Phase ? "XAConnection" : "PooledConnection");

            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "getPooledConnection", "Exception");
            throw resX;
        } catch (ClassCastException castX) {
            // There's a possibility this occurred because of an error in the JDBC driver
            // itself.  The trace should allow us to determine this.
            FFDCFilter.processException(castX, getClass().getName(), "1312");

            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "Caught ClassCastException", castX);

            ResourceException resX = new DataStoreAdapterException(castX.getMessage(), null, DatabaseHelper.class, is2Phase ? "NOT_A_2_PHASE_DS" : "NOT_A_1_PHASE_DS");

            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "getPooledConnection", "Exception");
            throw resX;
        }
    }

    /**
     * This method is used to set the client reroute options on the datasoruce Object.
     * This method will be a no-op for all but the DB2 universal driver.
     * 
     * @param dataSource : Datasource Object
     * @param cRJNDIName : Client Reroute JNDI name to be set on the DataSource Object.
     * @param cRAlternateServer : Client Reroute Alternate Server list
     * @param cRAlternatePort : Client Reroute Alternate Port list
     * @param cRPrimeServer : Client Reroute Primary Server
     * @param cRPrimePort : Client Reroute Primary Port
     * @param jndiContext : JNDI Context to be used to do the lookup
     * @param driverType : type2 (2) or type4 (4)
     * @throws Throwable : exception is bind failed or setting of the CRJNDIName fails
     *             precondition : all parameters wiht the exception of CRIJNDIName can not be null.
     */
    public void setClientRerouteData(Object dataSource, String cRJNDIName, String cRAlternateServer, String cRAlternatePort,
                                     String cRPrimeServer, String cRPrimePort, Context jndiContext, String driverType) throws Throwable // add driverType
    {
        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Client reroute is not supported on non-DB2 JCC driver.");
        }
    }

    /**
     * Method is used to see if the exception passed is an authorization exception or not.
     * 
     * @param SQLException the exception to check.
     * @return boolean true if determined to be an authorization exception, otherwise false.
     */
    public boolean isAnAuthorizationException(SQLException x) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "isAnAuthorizationException", x);

        boolean isAuthError = false;
        LinkedList<SQLException> stack = new LinkedList<SQLException>();
        if (x != null) 
            stack.push(x);

        // Limit the chain depth so that poorly written exceptions don't cause an infinite loop.
        for (int depth = 0; depth < 20 && !isAuthError && !stack.isEmpty(); depth++) {
            x = stack.pop();

            isAuthError |= isAuthException(x);

            // Add the chained exceptions to the stack.

            if (x.getNextException() != null)
                stack.push(x.getNextException());
            if (x.getCause() instanceof SQLException && x.getCause() != x.getNextException())
                stack.push((SQLException) x.getCause());
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "isAnAuthorizationException", isAuthError);
        return isAuthError;
    }

    /**
     * Determine if the top level exception is an authorization exception.
     * Chained exceptions are not checked.
     * 
     * In the generic case, look for the JDBC 4.0 exception subclass
     * or the 28xxx SQLState (Invalid authorization specification)
     * 
     * @param x the exception to check.
     * @return true or false to indicate if the exception is an authorization error.
     * @throws NullPointerException if a NULL exception parameter is supplied.
     */
    boolean isAuthException(SQLException x) {
        return x instanceof SQLInvalidAuthorizationSpecException
               || x.getSQLState() != null && x.getSQLState().startsWith("28");
    }

    /**
     * Method used to reuse a connection using kerberos.
     * This method will reset all connection properties, thus, after a reuse
     * is called, connection should be treated as if it was a newly created connection
     * 
     * @param java.sql.Connection conn
     * @param GSSCredential gssCred
     * @param Properties props
     * @throws SQLException
     */
    public void reuseKerbrosConnection(Connection sqlConn, GSSCredential gssCred, Properties props) throws SQLException {
        // an exception would have been thrown earlier than this point, so adding the trace just in case.
        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Kerberos reuse is not supported when using generic helper.  No-op operation.");
        }
    }

    /**
     * This method checks if the connection supports loose or tight branch coupling
     * 
     * @param couplingType
     * @return xa_start flag value
     */
    public int branchCouplingSupported(int couplingType) 
    {
        // Return -1 as we have no support for resref branch coupling
        if (couplingType == ResourceRefInfo.BRANCH_COUPLING_LOOSE || couplingType == ResourceRefInfo.BRANCH_COUPLING_TIGHT) 
        {
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Specified branch coupling type not supported");
            }
            return -1;
        }

        // If resref branch coupling unset then return default xa_start flags
        return javax.transaction.xa.XAResource.TMNOFLAGS;
    }

    /**
     * Returns the default type of branch coupling that should be used for BRANCH_COUPLING_UNSET.
     * 
     * @return the default type of branch coupling: BRANCH_COUPLING_LOOSE or BRANCH_COUPLING_TIGHT.
     *         If branch coupling is not supported or it is uncertain which type of branch coupling is default,
     *         then BRANCH_COUPLING_UNSET may be returned.
     * @see ResourceRefInfo
     */
    public int getDefaultBranchCoupling() {
        return ResourceRefInfo.BRANCH_COUPLING_UNSET;
    }

    /**
     * This method is called to cache the default set of properties for a Connection.
     * The properties only need to be cached when applications are invoking a
     * set of specific Vendor APIs, which change properties, that must be returned to
     * default values before pooling a connection. 
     * 
     * @return Map of properties or Null if not implemented
     * @param java.sql.Connection
     * @throws SQLException
     */
    public Map<String, Object> cacheVendorConnectionProps(Connection sqlConn) throws SQLException {
        return null;
    }

    /**
     * <p>This method is used to reset a connection before it is returned to the connection
     * pool. This method is called only when vendor specific connection setter methods are invoked.</p>
     * 
     * Currently the Oracle helper is the only helper that implements this method
     * 
     * @param conn the connection to attempt to cleanup.
     * @param props the default properties to be applied to the connection
     * 
     * @return true if properties were successfully applied.
     * 
     * @exception SQLException
     *                If an error occurs while cleaning up the connection.
     * 
     * 
     */
    public boolean doConnectionVendorPropertyReset(Connection sqlConn, Map<String, Object> props) throws SQLException {
        return false;
    }

    /**
     * Indicates if the JDBC driver supports propagating the GSS credential for kerberos
     * to the JDBC driver by obtaining the connection within Subject.doAs.
     * 
     * @return false because we cannot assume the support for the general case.
     */
    public boolean supportsSubjectDoAsForKerberos() {
        return false;
    }
}