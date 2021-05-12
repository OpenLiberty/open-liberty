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
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException; 
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLInvalidAuthorizationSpecException; 
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.resource.ResourceException;

import com.ibm.ejs.cm.logger.TraceWriter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.DSConfig;
import com.ibm.ws.rsadapter.SQLStateAndCode;
import com.ibm.ws.rsadapter.jdbc.WSJdbcTracer;

/**
 * Helper for the DataDirect Connect for JDBC driver, when connecting to Microsoft SQL Server database.
 */
public class DataDirectConnectSQLServerHelper extends DatabaseHelper {
    private static final TraceComponent tc = Tr.register(DataDirectConnectSQLServerHelper.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    private static final String LONG_DATA_CACHE_SIZE = "longDataCacheSize";

    /**
     * Cached copy of com.ddtek.jdbc.extensions.ExtConnection.getNetworkTimeout
     */
    private final AtomicReference<Method> getNetworkTimeout = new AtomicReference<Method>();

    /**
    *  LongDataCacheSize configured on the data source.
    */
    private int longDataCacheSize = 2048; // default value

    /**
    * Cached copy of com.ddtek.jdbc.extensions.ExtStatement.setLongDataCacheSize
    */
    private final AtomicReference<Method> setLongDataCacheSize = new AtomicReference<Method>();

    /**
     * Cached copy  of com.ddtek.jdbc.extensions.ExtConnection.setNetworkTimeout
     */
    private final AtomicReference<Method> setNetworkTimeout = new AtomicReference<Method>();

    @SuppressWarnings("deprecation")
    private transient com.ibm.ejs.ras.TraceComponent sqlserverTc = com.ibm.ejs.ras.Tr.register("com.ibm.ws.sqlserver.logwriter", "WAS.database", null);

    /**
     * SQLException error codes from the DataDirect Connect JDBC driver that indicate a stale connection.
     */
    final Set<Integer> staleDDErrorCodes = new HashSet<Integer>();

    /**
     * SQLException SQL States from the DataDirect Connect JDBC driver that indicate a stale connection.
     */
    final Set<String> staleDDSQLStates = new HashSet<String>();

    /**
     * SQLException error codes from the Microsoft SQL Server database that indicate a stale connection.
     */
    final Set<Integer> staleMSErrorCodes = new HashSet<Integer>();

    /**
    * Indicates whether the JDBC driver supports the setLongDataCacheSize method.
    * Assume that it does until proven otherwise.
    */
    private final AtomicBoolean supportsSetLongDataCacheSize = new AtomicBoolean(true);

    /**
    * Indicates whether the JDBC driver supports the setNetworkTimeout method.
    * Assume that it does until proven otherwise.
    */
    private final AtomicBoolean supportsNetworkTimeout = new AtomicBoolean(true);

    /**
     * Construct a helper class for the DataDirect Connect for JDBC driver.
     *  
     * @param mcf managed connection factory
     */
    DataDirectConnectSQLServerHelper(WSManagedConnectionFactoryImpl mcf) {
        super(mcf);

        dataStoreHelperClassName = "com.ibm.websphere.rsadapter.ConnectJDBCDataStoreHelper";

        mcf.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ;

        // Default values for the statement properties LongDataCacheSize and QueryTimeout are
        // configurable as data source properties. These data source properties are supplied to
        // the data store helper so that we can reset the statement properties to these default
        // values when caching statements.
        Properties props = mcf.dsConfig.get().vendorProps;
        Object value = props.get(LONG_DATA_CACHE_SIZE);
        try {
            longDataCacheSize = value instanceof Number ? ((Number) value).intValue()
                              : value instanceof String ? Integer.parseInt((String) value)
                              : longDataCacheSize;
        } catch (NumberFormatException x) {
            // error paths already covered by data source properties processing code 
        }

        if (TraceComponent.isAnyTracingEnabled() &&  tc.isDebugEnabled()) 
            Tr.debug(this, tc, "Default longDataCacheSize = " + longDataCacheSize);

        Collections.addAll(staleDDErrorCodes,
                           2217,
                           2251,
                           2306,
                           2310,
                           2311);

        // DataDirect informs us they will use all of the 08*** SQL States to indicate the
        // connection has become unusable. The following states are added here because they
        // are not found (and do not belong) in the super class.
        Collections.addAll(staleDDSQLStates,
                           "08000",
                           "08002",
                           "08004",
                           "08007");

        // DataDirect also informs us they do not use 40003 or S1000 to indicate a
        // dead connection. These need to be removed because they are inherited from the super class.
        staleConCodes.remove("40003");
        staleConCodes.remove("S1000");

        Collections.addAll(staleMSErrorCodes,
                           230,
                           6001,
                           6002,
                           6005,
                           6006);
    }

    @Override
    public boolean doConnectionCleanup(Connection conn) throws  SQLException {
        if (dataStoreHelper != null)
            return doConnectionCleanupLegacy(conn);

        final boolean trace = TraceComponent.isAnyTracingEnabled(); 
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "doConnectionCleanup", AdapterUtil.toString(conn));

        boolean updated = false;

        // Invoke via reflection:
        //   stmt.getNetworkTimeout();

        Method method = getNetworkTimeout.get();
        if (method == null && supportsNetworkTimeout.get())
            try {
                method = conn.getClass().getMethod("getNetworkTimeout");
                getNetworkTimeout.set(method);
            } catch (NoSuchMethodException x) {
                // No FFDC code needed. The JDBC driver version used does not have the getNetworkTimeout method.
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "getNetworkTimeout not supported.");
                supportsNetworkTimeout.set(false);
            }

        if (method != null)
            try {
                int networkTimeout = (Integer) method.invoke(conn);
                if (networkTimeout != 0) {
                    // Invoke via reflection:
                    //   stmt.setNetworkTimeout(0);

                    method = setNetworkTimeout.get();
                    if (method == null) {
                        method = conn.getClass().getMethod("setNetworkTimeout", int.class);
                        setNetworkTimeout.set(method);
                    }
                    method.invoke(conn, 0);
                    updated = true;
                }
            } catch (InvocationTargetException x) {
                // setNetworkTimeout raised an error when run.
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "doConnectionCleanup", x.getCause());
                // With SQL Server the get/setNetworkTimeout work fine. However with Oracle it throws a generic
                // SQLException instead of a NoSuchMethodException and here we allow the code to proceed by masking the exception.
                supportsNetworkTimeout.set(false);
                getNetworkTimeout.set(null);
                setNetworkTimeout.set(null);
            } catch (IllegalAccessException x) {
                // No FFDC code needed. The JDBC driver version used does not permit access to the get/setNetworkTimeout method.
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "NetworkTimeout not supported.");
                supportsNetworkTimeout.set(false);
                getNetworkTimeout.set(null);
                setNetworkTimeout.set(null);
            } catch (NoSuchMethodException x) {
                FFDCFilter.processException(x, getClass().getName(), "237", this);
                // Should never have a case where getNetworkTimeout is supported, but not setNetworkTimeout
                throw new SQLFeatureNotSupportedException(x);
            }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "doConnectionCleanup");

        return updated;
    }

    @Override
    public void doStatementCleanup(java.sql.PreparedStatement stmt) throws SQLException
    {
       if (dataStoreHelper != null) {
           doStatementCleanupLegacy(stmt);
           return;
       }

       if (TraceComponent.isAnyTracingEnabled() &&  tc.isEntryEnabled()) 
           Tr.entry(this, tc, "doStatementCleanup", AdapterUtil.toString(stmt));  

       // ConnectJDBC does not support cursorName, so skip it.

       stmt.setFetchDirection(java.sql.ResultSet.FETCH_FORWARD);
       stmt.setMaxFieldSize(0);
       stmt.setMaxRows(0);

       Integer queryTimeout = mcf.dsConfig.get().queryTimeout;
       if (queryTimeout == null)
           queryTimeout = defaultQueryTimeout;
       stmt.setQueryTimeout(queryTimeout); 

       stmt = (java.sql.PreparedStatement) WSJdbcTracer.getImpl(stmt);

       // Invoke via reflection:
       //   stmt.setLongDataCacheSize(longDataCacheSize);

       Method method = setLongDataCacheSize.get();
       if (method == null && supportsSetLongDataCacheSize.get()) try
       {
           method = stmt.getClass().getMethod("setLongDataCacheSize", int.class);
           setLongDataCacheSize.set(method);
       }
       catch (NoSuchMethodException x)
       {
           // No FFDC code needed. The JDBC driver version used does not have the
           // setLongDataCacheSize method.
           if (TraceComponent.isAnyTracingEnabled() &&  tc.isDebugEnabled()) 
               Tr.debug(this, tc, "LongDataCacheSize not supported.");  
           supportsSetLongDataCacheSize.set(false);
       }

       if (method != null) try
       {
           method.invoke(stmt, longDataCacheSize);
       }
       catch (InvocationTargetException x)
       {
           // setLongDataCacheSize raised an error when run.
           if (TraceComponent.isAnyTracingEnabled() &&  tc.isEntryEnabled()) 
               Tr.exit(this, tc, "doStatementCleanup", x.getCause());  
           throw (SQLException) x.getCause();
       }
       catch (IllegalAccessException x)
       {
           // No FFDC code needed. The JDBC driver version used does not permit access to
           // the setLongDataCacheSize method.
           if (TraceComponent.isAnyTracingEnabled() &&  tc.isDebugEnabled()) 
               Tr.debug(this, tc, "LongDataCacheSize not supported.");  
           supportsSetLongDataCacheSize.set(false);
           setLongDataCacheSize.set(null);
       }

       if (TraceComponent.isAnyTracingEnabled() &&  tc.isEntryEnabled())  
           Tr.exit(this, tc, "doStatementCleanup");  
    }

    /**
     * @return NULL because the DataDirect Connect JDBC driver provides sufficient trace of its own.
     */
    @Override
    public com.ibm.ejs.ras.TraceComponent getTracer() {
        return null;
    }

    /**
     * Determine if the top level exception is an authorization exception.
     * Chained exceptions are not checked.
     * 
     * For the DataDirect Connect for JDBC driver for Microsoft SQL Server,
     * look for the JDBC 4.0 exception subclass
     * or the 28xxx SQLState (Invalid authorization specification)
     * or an SQL Server error code in (18450..18452, 18456..18461, 18470, 18483, 18485..18488)
     * 
     * @param x the exception to check.
     * @return true or false to indicate if the exception is an authorization error.
     * @throws NullPointerException if a NULL exception parameter is supplied.
     */
    @Override
    boolean isAuthException(SQLException x)
    {
        int ec = x.getErrorCode();
        return x instanceof SQLInvalidAuthorizationSpecException
               || x.getSQLState() != null && x.getSQLState().startsWith("28")
               || ec >= 18450 && ec <= 18452
               || ec >= 18456 && ec <= 18461
               || ec == 18470
               || ec == 18483
               || ec >= 18485 && ec <= 18488;

        // 18450: Login failed for login "%.*ls". The login is not defined as a valid login of a trusted SQL Server connection.%.*ls
        // 18451: Login failed for user '%.*ls'. Only administrators may connect at this time.%.*ls
        // 18452: Login failed for user '%.*ls'. The user is not associated with a trusted SQL Server connection.%.*ls
        // 18456: Login failed for user '%.*ls'.%.*ls
        // 18457: Login failed for user '%.*ls'. The user name contains a mapping character or is longer than 30 characters.%.*ls
        // 18458: Login failed. The number of simultaneous users already equals the %d registered licenses for this server...
        // 18459: Login failed. The workstation licensing limit for SQL Server access has already been reached.%.*ls
        // 18460: Login failed. The number of simultaneous users has already reached the limit of %d licenses ...
        // 18461: Login failed for user '%.*ls'. Reason: Server is in single user mode. Only one administrator can connect ...
        // 18470: Login failed for user '%.*ls'. Reason: The account is disabled.%.*ls
        // 18483: Could not connect to server '%.*ls' because '%.*ls' is not defined as a remote login at t...
        // 18485: Could not connect to server '%.*ls' because it is not configured to accept remote logins....
        // 18486: Login failed for user '%.*ls' because the account is currently locked out. The system adm...
        // 18487: Login failed for SQL Server login '%.*ls'. The password for this login has expired.%.*ls
        // 18488: Login failed for user '%.*ls'.  Reason: The password of the account must be changed.%.*ls;
    }

    @Override
    public PrintWriter getPrintWriter() throws ResourceException {
        //not synchronizing here since there will be one helper
        // and most likely the setting will be serially, even if its not, 
        // it shouldn't matter here (tracing).
        if (genPw == null) {
            genPw = new PrintWriter(new TraceWriter(sqlserverTc), true);
        }
        Tr.debug(sqlserverTc, "returning", genPw);
        return genPw;
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

        // Use the equivalent method on DataStoreHelper if possible.
        if (dataStoreHelper != null)
            try {
                boolean stale = AccessController.doPrivileged((PrivilegedExceptionAction<Boolean>) () -> {
                    return (Boolean) dataStoreHelper.getClass()
                                    .getMethod("isConnectionError", SQLException.class)
                                    .invoke(dataStoreHelper, ex);
                });
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "isConnectionError", stale);
                return stale;
            } catch (PrivilegedActionException x) {
                FFDCFilter.processException(x, getClass().getName(), "673", this);
            }

        DSConfig config = mcf.dsConfig.get();

        // Maintain a set in order to check for cycles
        Set<Throwable> chain = new HashSet<Throwable>();

        boolean stale = super.isConnectionError(ex);
        for (Throwable t = ex; t != null && !stale && chain.add(t); t = t.getCause()) {
            if (t instanceof SQLException) {
                SQLException sqlX = (SQLException) t;
                String sqlState = sqlX.getSQLState();
                int errorCode = sqlX.getErrorCode();
                SQLStateAndCode combo = sqlState == null ? null : new SQLStateAndCode(sqlState, errorCode);

                if ((combo == null || config.identifyExceptions.get(combo) == null)
                                && config.identifyExceptions.get(errorCode) == null
                                && (sqlState == null || config.identifyExceptions.get(sqlState) == null))
                    stale = isDataDirectExp(ex)
                                    ? staleDDErrorCodes.contains(errorCode) || staleDDSQLStates.contains(sqlState)
                                    : staleMSErrorCodes.contains(errorCode);
                // else already checked by super.isConnectionError

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, "isConnectionError? " + sqlState + ' ' + errorCode + ' ' + sqlX.getClass().getName(), stale);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "isConnectionError", stale);

        return stale;
    }

    /**
     * <p>This method checks if the specified exception is a DataDirect JDBC driver-created
     * exception or a database vendor-created (such as MS SQL Server) exception.
     * The result of this method is used to determine how SQLException mapping is performed.
     * If the SQLException was created by the JDBC driver, the default findMappingClass
     * implementation will search through the DataDirect SQLException mappings only.</p>
     * 
     * <p>This method is provided for internal use only. It cannot be overriden.</p>
     * 
     * @param e the SQLException to check.
     * 
     * @return true if the SQLException occurred in a DataDirect JDBC driver.
     *         false if the SQLException originated in the database.
     */
    private boolean isDataDirectExp(SQLException e) {
        /*
         * example of exception message
         * msft = "[Microsoft][SQLServer JDBC Driver][SQLServer]Invalid column name 'something'.";
         */
        String message = e.getMessage();
        int ind = message.indexOf('[', 2); // find the position of the second [
        ind = message.indexOf("][", ind + 10); // look for [] starting after the length of [SQLServer JDBC Driver], 10 is a good number.

        if (ind != -1) { // if none found ===> it is a datadirect one
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "The exception is NOT a DataDirect exception");
            return false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "the exception is a DataDirect exception");
        return true;
    }

    @Override
    public boolean shouldTraceBeEnabled(WSManagedConnectionFactoryImpl mcf) {
        // here will base this on the mcf since, the value is enabled for the system
        // as a whole
        if (TraceComponent.isAnyTracingEnabled() && sqlserverTc.isDebugEnabled() && !mcf.loggingEnabled) 
            return true;
        return false;
    }

    @Override
    public boolean shouldTraceBeEnabled(WSRdbManagedConnectionImpl mc) {
        //the mc.mcf will be passed to the shouldTraceBeEnabled method that handles WSManagedConnectionFactoryImpl and 
        //get the boolean returned
        return (this.shouldTraceBeEnabled(mc.mcf));
    } 

    @Override
    public boolean shouldTraceBeDisabled(WSRdbManagedConnectionImpl mc) {
        if (TraceComponent.isAnyTracingEnabled() && !sqlserverTc.isDebugEnabled() && mc.mcf.loggingEnabled) 
            return true;

        return false;
    }


    /**
     * Indicates if the JDBC driver supports propagating the GSS credential for kerberos
     * to the JDBC driver by obtaining the connection within Subject.doAs.
     * 
     * @return true for the DataDirect Connect for JDBC driver.
     */
    @Override
    public boolean supportsSubjectDoAsForKerberos() {
        return true;
    }
}