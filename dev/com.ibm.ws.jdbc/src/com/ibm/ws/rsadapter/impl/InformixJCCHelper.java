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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import javax.resource.ResourceException;

import com.ibm.ejs.cm.logger.TraceWriter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;

/**
 * Helper for the Informix JCC driver.
 */
public class InformixJCCHelper extends InformixHelper {
    @SuppressWarnings("deprecation")
    private transient static com.ibm.ejs.ras.TraceComponent ifxTc = com.ibm.ejs.ras.Tr.register("com.ibm.ws.informix.jcclogwriter", "WAS.database", null);
    private transient static volatile PrintWriter pw;

    /**
     * Informix JCC methods
     */
    private final AtomicReference<Method>
                    setJCCLogWriter = new AtomicReference<Method>(),
                    setJCCLogWriter2 = new AtomicReference<Method>();

    /**
     * Informix JCC method signatures
     */
    private static final Class<?>[]
                    TYPES_PrintWriter = new Class<?>[] { PrintWriter.class },
                    TYPES_PrintWriter_int = new Class<?>[] { PrintWriter.class, int.class };

    private transient int configuredTraceLevel = 0;

    private transient Class<?> currClass = InformixJCCHelper.class;

    /**
     * Construct a helper class for the Informix JCC driver.
     *  
     * @param mcf managed connection factory
     */
    InformixJCCHelper(WSManagedConnectionFactoryImpl mcf) throws Exception {
        super(mcf);

        dataStoreHelperClassName = "com.ibm.websphere.rsadapter.InformixJccDataStoreHelper";

        mcf.doesStatementCacheIsoLevel = true;
        mcf.supportsGetTypeMap = false;

        configuredTraceLevel = 0; // value of DB2BaseDataSource.TRACE_NONE
        
        Collections.addAll(staleConCodes,
                           -4499);
    }

    public void doConnectionSetup(Connection conn) throws SQLException {
        // don't inherit from Informix helper because external Informix JCC helper didn't either
        if (dataStoreHelper != null) {
            doConnectionSetupLegacy(conn);
            return;
        }
    }

    @Override
    public void doStatementCleanup(PreparedStatement stmt) throws SQLException {
        if (dataStoreHelper != null) {
            doStatementCleanupLegacy(stmt);
            return;
        }

        try {
            stmt.setCursorName(null);
        } catch (NullPointerException npe) {
            //Ignore NPE as the statement is being closed anyway.
        }
        stmt.setFetchDirection(ResultSet.FETCH_FORWARD);

        // For the JCC driver, the maxFieldSize getter is faster so avoid the setter if possible.
        if (stmt.getMaxFieldSize() != 0)
            stmt.setMaxFieldSize(0);

        stmt.setMaxRows(0);

        Integer queryTimeout = mcf.dsConfig.get().queryTimeout;
        if (queryTimeout == null)
            queryTimeout = defaultQueryTimeout;
        stmt.setQueryTimeout(queryTimeout);
    }

    /**
     * Utility method that returns the result of
     * ((DB2Connection) con).methName(params)
     * 
     * @param con connection to the database
     * @param methRef reference to a method
     * @param methName name of the method
     * @param paramTypes method parameter types
     * @param params method parameters
     * @return the result
     * @throws SQLException if an error occurs
     */
    private final Object invokeOnDB2Connection(Connection con, AtomicReference<Method> methRef,
                                               String methName, Class<?>[] paramTypes, Object... params) throws SQLException {
        try {
            Method m = methRef.get();
            if (m == null) {
                Class<?> c = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, "com.ibm.db2.jcc.DB2Connection");
                methRef.set(m = c.getMethod(methName, paramTypes));
            }

            return m.invoke(con, params);
        } catch (RuntimeException x) {
            throw x;
        } catch (NoSuchMethodException x) {
            throw (Error) new NoSuchMethodError(methName).initCause(x);
        } catch(InvocationTargetException e){
            throw AdapterUtil.toSQLException(e.getCause());
        } catch (Exception x) {
            throw AdapterUtil.toSQLException(x);
        }
    }

    @Override
    public boolean shouldTraceBeEnabled(WSRdbManagedConnectionImpl mc) {
        // here will base this on the mc since the value is enabled per connection
        if (TraceComponent.isAnyTracingEnabled() && ifxTc.isDebugEnabled() && !mc.loggingEnabled) 
            return true;

        return false;
    }

    /**
     * @see com.ibm.ws.rsadapter.spi.DatabaseHelper#shouldTraceBeDisabled(com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl)
     */
    @Override
    public boolean shouldTraceBeDisabled(WSRdbManagedConnectionImpl mc) {
        if (TraceComponent.isAnyTracingEnabled() && !ifxTc.isDebugEnabled() && mc.loggingEnabled) 
            return true;

        return false;
    }

    /**
     * @see com.ibm.ws.rsadapter.spi.DatabaseHelper#disableJdbcLogging(com.ibm.ws.rsadapter.spi.WSRdbManagedConnectionImpl)
     */
    @Override
    public void disableJdbcLogging(WSRdbManagedConnectionImpl mc) throws ResourceException {
        //note that we don't check if debug is enabled, since we we get here, we knwo debug is enabled
        if (TraceComponent.isAnyTracingEnabled() && ifxTc.isDebugEnabled())
            Tr.debug(ifxTc, "Disabling logging on connection: ", mc.sqlConn);

        try {
            invokeOnDB2Connection(mc.sqlConn, setJCCLogWriter, "setJCCLogWriter", TYPES_PrintWriter, new Object[] { null });
        } catch (SQLException e) {
            FFDCFilter.processException(e, getClass().getName(), "86", this);
            throw AdapterUtil.translateSQLException(e, mc, true, currClass); 
        }
        mc.loggingEnabled = false;
    }

    @Override
    public void enableJdbcLogging(WSRdbManagedConnectionImpl mc) throws ResourceException {
        // note that we don't check if debug is enabled, since we we get here, we knwo debug is enabled
        PrintWriter pw = getPrintWriter(); 
        if (TraceComponent.isAnyTracingEnabled() && ifxTc.isDebugEnabled())
            Tr.debug(ifxTc, "Setting printWriter on connection and with level: ", new Object[] { pw, mc.sqlConn, configuredTraceLevel }); 
        try {
            invokeOnDB2Connection(mc.sqlConn, setJCCLogWriter2, "setJCCLogWriter", TYPES_PrintWriter_int, pw, configuredTraceLevel);
        } catch (SQLException e) {
            FFDCFilter.processException(e, getClass().getName(), "108", this);
            throw AdapterUtil.translateSQLException(e, mc, true, currClass); 
        }
        mc.loggingEnabled = true;
    }

    /**
     * Determine if the top level exception is an authorization exception.
     * Chained exceptions are not checked.
     * 
     * Look for the JDBC 4.0 exception subclass
     * or the 28xxx SQLState (Invalid authorization specification)
     * or an Informix error code in (-951, -952, -956, -1782, -4214, -11018, -11033, -25590, -29007)
     * 
     * @param x the exception to check.
     * @return true or false to indicate if the exception is an authorization error.
     * @throws NullPointerException if a NULL exception parameter is supplied.
     */
    @Override
    boolean isAuthException(SQLException x) {
        return x.getErrorCode() == -4214 || super.isAuthException(x);
    }

    @Override
    public PrintWriter getPrintWriter() throws ResourceException { 
        //not synchronizing here since there will be one helper
        // and most likely the setting will be serially, even if its not,
        // it shouldn't matter here (tracing).
        if (pw == null) {
            pw = new java.io.PrintWriter(new TraceWriter(ifxTc), true);
        }
        Tr.debug(ifxTc, "returning", pw);
        return pw;
    }

    // return null: do not support supplemental tracing.
    @Override
    public com.ibm.ejs.ras.TraceComponent getTracer() {
        return null;
    }
}
