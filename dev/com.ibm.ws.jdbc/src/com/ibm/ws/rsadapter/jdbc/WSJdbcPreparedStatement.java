/*******************************************************************************
 * Copyright (c) 2001, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Wrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.impl.StatementCacheKey;

/**
 * This class wraps a PreparedStatement.
 */
public class WSJdbcPreparedStatement extends WSJdbcStatement implements PreparedStatement {
    private static final TraceComponent tc = Tr.register(
                                                         WSJdbcPreparedStatement.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /** The underlying PreparedStatement object. */
    protected PreparedStatement pstmtImpl; 

    /** The key for statement caching. Null if statement caching is disabled. */
    protected StatementCacheKey key; 

    /**
     * Tracks the SQL for this prepared statement solely for the purpose of reporting it to
     * PMI request metrics.
     * 
     */
    protected String sql;
    
    Object sqljSection;

    /**
     * Do not use. Constructor exists only for CallableStatement wrapper.
     */
    public WSJdbcPreparedStatement() 
    {
        poolabilityHint = true; // Default for prepared statements. 
    }

    /**
     * Create a WebSphere PreparedStatement wrapper. This constructor should only be used if
     * statement caching is disabled.
     * 
     * @param pstmtImplObject the JDBC PreparedStatement implementation class to be wrapped.
     * @param connWrapper the WebSphere JDBC Connection wrapper creating this statement.
     * @param theHoldability the cursor holdability value of this statement
     * @param pstmtSQL the SQL for this prepared statement. 
     * 
     * @throws SQLException if an error occurs making the PreparedStatement wrapper.
     */
    public WSJdbcPreparedStatement(PreparedStatement pstmtImplObject, WSJdbcConnection connWrapper,
                                      int theHoldability, String pstmtSQL) 
    throws SQLException 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "<init>", 
                     AdapterUtil.toString(pstmtImplObject), connWrapper, AdapterUtil.getCursorHoldabilityString(theHoldability));

        stmtImpl = pstmtImpl = pstmtImplObject;
        init(connWrapper); 
        sql = pstmtSQL; 
        poolabilityHint = true; // Default for prepared statements. 
        holdability = theHoldability; 

        mcf = parentWrapper.mcf;

        try {
            currentFetchSize = pstmtImpl.getFetchSize();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.<init>", "94", this);

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "<init>", "Exception"); 
            throw WSJdbcUtil.mapException(this, ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>", "current fetchSize is " + currentFetchSize);
    }

    /**
     * Create a WebSphere PreparedStatement wrapper. This constructor should only be used if
     * statement caching is enabled.
     * 
     * @param pstmtImplObject the JDBC PreparedStatement implementation class to be wrapped.
     * @param connWrapper the WebSphere JDBC Connection wrapper creating this statement.
     * @param theHoldability the cursor holdability value of this statement
     * @param pstmtSQL the SQL for this prepared statement. 
     * @param pstmtKey the PreparedStatement key, for caching PreparedStatements. This
     *            value should never be null.
     * 
     * @throws SQLException if an error occurs making the PreparedStatement wrapper.
     */
    public WSJdbcPreparedStatement(PreparedStatement pstmtImplObject, WSJdbcConnection connWrapper,
                                      int theHoldability, String pstmtSQL, 
                                      StatementCacheKey pstmtKey) throws SQLException 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "<init>", 
                     AdapterUtil.toString(pstmtImplObject), connWrapper, AdapterUtil.getCursorHoldabilityString(theHoldability), pstmtKey);

        stmtImpl = pstmtImpl = pstmtImplObject;
        init(connWrapper); 
        sql = pstmtSQL; 
        key = pstmtKey;
        poolabilityHint = true; // Default for prepared statements. 
        holdability = theHoldability; 

        mcf = parentWrapper.mcf;

        try {
            currentFetchSize = pstmtImpl.getFetchSize();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.<init>", "184", this);

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "<init>", "Exception"); 
            throw WSJdbcUtil.mapException(this, ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>", "current fetchSize is " + currentFetchSize);
    }

    public void addBatch() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "addBatch"); 

        try {
            pstmtImpl.addBatch();
            hasBatchParameters = true; 
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "addBatch", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Invokes addBatch after replacing the parameter with the DB2 impl object if proxied.
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
    private Object addBatch(Object implObject, Method method, Object[] args)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                    SQLException {
        Object sqljPstmt = args[0];
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled()) Tr.debug(tc, "addBatch", new Object[] 
            { this, AdapterUtil.toString(sqljPstmt) });

        // Get underlying instance of SQLJPreparedStatement from dynamic proxy
        // to avoid java.lang.ClassCastException in addBatch()
        if (sqljPstmt != null && Proxy.isProxyClass(sqljPstmt.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(sqljPstmt);
            if (handler instanceof WSJdbcWrapper)
                args = new Object[] { ((WSJdbcWrapper) handler).getJDBCImplObject() }; 
        }

        return method.invoke(implObject, args);
    }

    public void clearParameters() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "clearParameters"); 

        try {
            pstmtImpl.clearParameters();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.clearParameters", "87", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Perform any wrapper-specific close logic. This method is called by the default
     * WSJdbcObject close method.
     * 
     * @param closeWrapperOnly boolean flag to indicate that only wrapper-closure activities
     *            should be performed, but close of the underlying object is unnecessary.
     * 
     * @return SQLException the first error to occur while closing the object.
     */
    @Override
    protected SQLException closeWrapper(boolean closeWrapperOnly) 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        SQLException sqlX = null;

        // Indicate the statement is closed by setting the parent object's statement to
        // null.  This will allow us to be garbage collected.

        try // Connection wrapper can close at any time.
        {
            parentWrapper.childWrappers.remove(this);
        } catch (RuntimeException runtimeX) {
            // No FFDC code needed; parent wrapper might be closed.
            if (parentWrapper.state != State.CLOSED)
                throw runtimeX;
        }

        com.ibm.ws.rsadapter.impl.WSRdbManagedConnectionImpl mc;

        // A null key means statement caching is disabled. Do not cache. Close instead.
        // Also don't cache if the poolability hint is FALSE.   
        if (key == null || !poolabilityHint)
            try 
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, key == null ? 
                    "statement caching is disabled" : 
                    "statement is not poolable"); 

                pstmtImpl.close();
            } catch (SQLException closeX) {
                FFDCFilter.processException(closeX,
                                            getClass().getName() + ".closeWrapper", "264", this);

                Tr.warning(tc, "ERR_CLOSING_OBJECT", pstmtImpl, closeX);
                sqlX = closeX;
            }

        // Do not cache unless we are associated with a ManagedConnection. 
        else if ((mc = ((WSJdbcConnection) parentWrapper).managedConn) == null)
            try {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, 
                             "Not associated with a ManagedConnection. Statement cannot be cached.");

                pstmtImpl.close();
            } catch (SQLException closeX) {
                FFDCFilter.processException(closeX,
                                            getClass().getName() + ".closeWrapper", "281", this);

                Tr.warning(tc, "ERR_CLOSING_OBJECT", pstmtImpl, closeX);
                sqlX = closeX;
            }

        // Attempt to cache the statement.
        else
            try {
                if (!pstmtImpl.getMoreResults() && (mcf.getHelper().getUpdateCount(this) == -1)) { 
                    // Reset any statement properties that have changed. 
                    if (haveStatementPropertiesChanged) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Cleaning up Statement");
                        mcf.getHelper().doStatementCleanup(pstmtImpl);
                        haveStatementPropertiesChanged = false;
                    }

                    // Clear statement parameters before caching to release memory 
                    pstmtImpl.clearParameters(); 

                    // Batch parameters must be cleared before putting the statement in the cache
                    // because this is the only time information is available regarding whether the
                    // clearBatch is needed. There is no need to reset the 'hasBatchParameters' flag
                    // because statement wrappers are not reusable.

                    if (hasBatchParameters) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Clearing batch parameters"); 
                        pstmtImpl.clearBatch();
                    }

                    // Return the statement to the cache.
                    if (tc.isDebugEnabled())
                        Tr.debug(this, tc, "Attempt to cache statement");
                    mc.cacheStatement(pstmtImpl, key);
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(this, tc, "Cannot cache statement as there are unprocessed results");
                    pstmtImpl.close();
                }

            } catch (SQLException cleanupX) {
                if (!mc.isAborted()) {
                    FFDCFilter.processException(cleanupX, getClass().getName() + ".closeWrapper", "310", this);
                    sqlX = cleanupX;
                }

                try {
                    pstmtImpl.close();
                } catch (SQLException closeX) {
                    FFDCFilter.processException(closeX,
                                                getClass().getName() + ".closeWrapper", "321", this);

                    Tr.warning(tc, "ERR_CLOSING_OBJECT", pstmtImpl, closeX);
                }
            }

        stmtImpl = null;
        pstmtImpl = null;
        key = null;

        return sqlX == null ? null : WSJdbcUtil.mapException(this, sqlX); 
    }
    
    @Override
    public void closeOnCompletion() throws SQLException {
        super.closeOnCompletion();
        
        key = null; // disable statement caching
    }

    public boolean execute() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "execute"); 

        boolean result;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            result = pstmtImpl.execute();
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "execute", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "execute", "NullPointerException"); 
            throw runtimeXIfNotClosed(nullX);
        } catch (RuntimeException rte) 
        {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "execute", rte); 
            throw rte;
        } 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "execute", result ? "QUERY" : "UPDATE"); 
        return result;
    }

    /**
     * Invokes executeBatch and after closing any previous result sets and ensuring statement properties are up-to-date.
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
    private Object executeBatch(Object implObject, Method method, Object[] args)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                    SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) Tr.entry(tc, "executeBatch", new Object[] 
            { this, args[0] });

        if (childWrapper != null) {
            closeAndRemoveResultSet();
        }

        if (childWrappers != null && !childWrappers.isEmpty()) {
            closeAndRemoveResultSets();
        }

        enforceStatementProperties();

        Object results = method.invoke(implObject, args);

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(tc, "executeBatch", results instanceof int[] ? Arrays.toString((int[]) results) : results);
        return results;
    }

    public ResultSet executeQuery() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeQuery"); 

        WSJdbcResultSet rsetWrapper = null;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            childWrapper = rsetWrapper = createResultSetWrapper(pstmtImpl.executeQuery());
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeQuery", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeQuery", "NullPointerException"); 
            throw runtimeXIfNotClosed(nullX);
        } catch (RuntimeException rte) 
        {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeQuery", rte); 
            throw rte;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeQuery", childWrapper); 
        return rsetWrapper;
    }

    public int executeUpdate() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeUpdate"); 

        int numUpdates;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            numUpdates = pstmtImpl.executeUpdate();
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error.  
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeUpdate", ex); 

            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeUpdate", "NullPpointerException"); 
            throw runtimeXIfNotClosed(nullX);
        } catch (RuntimeException rte) 
        {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeUpdate", rte); 
            throw rte;
        } 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeUpdate", numUpdates); 
        return numUpdates;
    }

    public ResultSetMetaData getMetaData() throws SQLException 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "getMetaData"); 

        ResultSetMetaData md = null;
        try // get a new meta data
        {
            md = pstmtImpl.getMetaData();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.getMetaData", "621", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getMetaData", "Exception"); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getMetaData", "Exception"); 
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "getMetaData", md); 
        return md;
    }

    /**
     * Invokes getReturnResultSet and wraps the result set.
     * 
     * @param implObject the instance on which the operation is invoked.
     * @param method the method that is invoked.
     * 
     * @throws IllegalAccessException if the method is inaccessible.
     * @throws IllegalArgumentException if the instance does not have the method or
     *             if the method arguments are not appropriate.
     * @throws InvocationTargetException if the method raises a checked exception.
     * @throws SQLException if unable to invoke the method for other reasons.
     * 
     * @return the result of invoking the method.
     */
    private Object getReturnResultSet(Object implObject, Method method)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                    SQLException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "getReturnResultSet", this);

        WSJdbcResultSet rsetWrapper = null;
        ResultSet rsetImpl = (ResultSet) method.invoke(implObject);
        if (rsetImpl != null) {
            // If the childWrapper is null, and the childWrappers is null or 
            // empty, set the result set to childWrapper;
            // Otherwise, add the result set to childWrappers
            if (childWrapper == null && (childWrappers == null || childWrappers.isEmpty())) {
                childWrapper = rsetWrapper = mcf.jdbcRuntime.newResultSet(rsetImpl, this);
                if (trace &&  tc.isDebugEnabled())
                    Tr.debug(tc, "Set the result set to child wrapper"); 
            } else {
                if (childWrappers == null) 
                    childWrappers = new ArrayList<Wrapper>(5); 
                rsetWrapper = mcf.jdbcRuntime.newResultSet(rsetImpl, this);
                childWrappers.add(rsetWrapper);
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "Add the result set to child wrappers list."); 
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, "getReturnResultSet", rsetWrapper);
        return rsetWrapper;
    }

    /**
     * Invokes getSingletonResultSet and wraps the SQLJResultSet.
     * 
     * @param implObject the instance on which the operation is invoked.
     * @param method the method that is invoked.
     * 
     * @throws IllegalAccessException if the method is inaccessible.
     * @throws IllegalArgumentException if the instance does not have the method or
     *             if the method arguments are not appropriate.
     * @throws InvocationTargetException if the method raises a checked exception.
     * @throws SQLException if unable to invoke the method for other reasons.
     * 
     * @return the result of invoking the method.
     */
    private Object getSingletonResultSet(Object implObject, Method method)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                    SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) Tr.entry(tc, "getSingletonResultSet", this); 

        Object sqljWrapper = null;

        // Choose the ResultSet wrapper based on Connection wrapper type.
        // This method can return a null Result Set, which we shouldn't wrap.

        ResultSet rsetImpl = (ResultSet) method.invoke(implObject);
        if (rsetImpl != null) {
            // If the childWrapper is null, and the childWrappers is null or 
            // empty, set the result set to childWrapper;
            // Otherwise, add the result set to childWrappers

            WSJdbcResultSet rsetWrapper;
            if (childWrapper == null && (childWrappers == null || childWrappers.isEmpty())) {
                childWrapper = rsetWrapper = mcf.jdbcRuntime.newResultSet(rsetImpl, this);
                if (TraceComponent.isAnyTracingEnabled() &&  tc.isDebugEnabled()) Tr.debug(tc, "Set the result set to child wrapper");  
            }
            else {
                if (childWrappers == null) 
                    childWrappers = new ArrayList<Wrapper>(5); 
                rsetWrapper = mcf.jdbcRuntime.newResultSet(rsetImpl, this);
                childWrappers.add(rsetWrapper);
                if (isTraceOn && tc.isDebugEnabled()) Tr.debug(tc, 
                    "Add the result set to child wrappers list."); 
            }

            sqljWrapper = rsetWrapper.unwrap(method.getReturnType());
        }

        if (isTraceOn && tc.isEntryEnabled()) Tr.exit(tc, "getSingletonResultSet", sqljWrapper); 
        return sqljWrapper;
    }

    /**
     * @return the trace component for the WSJdbcPreparedStatement.
     */
    @Override
    protected TraceComponent getTracer() 
    {
        return tc;
    }

    /**
     * Collects FFDC information specific to this JDBC wrapper. Formats this information to
     * the provided FFDC logger. This method is used by introspectAll to collect any wrapper
     * specific information.
     * 
     * @param info FFDCLogger on which to record the FFDC information.
     */
    @Override
    protected void introspectWrapperSpecificInfo(com.ibm.ws.rsadapter.FFDCLogger info) {
        super.introspectWrapperSpecificInfo(info);

        info.append("key:", key);
        info.append("sql:", sql); 
    }

    /**
     * Intercept the proxy handler to detect changes to statement properties that must be
     * reset on cached statements.
     * 
     * @param proxy the dynamic proxy.
     * @param method the method being invoked.
     * @param args the parameters to the method.
     * 
     * @return the result of invoking the operation on the underlying object.
     * @throws Throwable if something goes wrong.
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!haveStatementPropertiesChanged && VENDOR_PROPERTY_SETTERS.contains(method.getName())) {
            haveStatementPropertiesChanged = true;
        }
        
        // The SQLJ programming model indicates that getSection should be callable on a
        // closed PreparedStatement.  Therefore we manually cache the section and return it
        if(sqljSection != null && "getSection".equals(method.getName())) {
            return sqljSection;
        }

        return super.invoke(proxy, method, args);
    }

    /**
     * Invokes a method on the specified object.
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
        if (args == null || args.length == 0) {
            String methodName = method.getName();
            if (methodName.equals("getReturnResultSet"))
                return getReturnResultSet(implObject, method);
            else if (methodName.equals("getSingletonResultSet"))
                return getSingletonResultSet(implObject, method);
        } else if (args.length == 1) {
            String methodName = method.getName();
            if (methodName.equals("addBatch") && method.getParameterTypes()[0].getName().equals("com.ibm.db2.jcc.SQLJPreparedStatement"))
                return addBatch(implObject, method, args);
            else if (methodName.equals("executeBatch"))
                return executeBatch(implObject, method, args);
            else if (methodName.equals("setSection") && method.getParameterTypes()[0].getName().equals("com.ibm.db2.jcc.SQLJSection")) {
                // Since we cache the SQLJ section, update our cached value whenever it is updated
                this.sqljSection =  args[0];
            }
        }
        return super.invokeOperation(implObject, method, args);
    }

    public void setArray(int i, Array x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setArray #" + i); 

        try {
            pstmtImpl.setArray(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setArray", "500", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setAsciiStream(int i, InputStream x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setAsciiStream #" + i); 

        try {
            pstmtImpl.setAsciiStream(i, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setAsciiStream", "1057", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setAsciiStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setAsciiStream", "1054", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setAsciiStream", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setAsciiStream", "1061", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setAsciiStream", err); 
            throw err;
        }
    }

    public void setAsciiStream(int i, java.io.InputStream x, int length) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setAsciiStream #" + i, length); 

        try {
            pstmtImpl.setAsciiStream(i, x, length);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setAsciiStream", "522", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setAsciiStream(int i, InputStream x, long length) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setAsciiStream #" + i, "length = " + length); 

        try {
            pstmtImpl.setAsciiStream(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setAsciiStream", "1057", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setAsciiStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setAsciiStream", "1123", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setAsciiStream", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setAsciiStream", "1130", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setAsciiStream", err); 
            throw err;
        }
    }

    public void setBigDecimal(int i, java.math.BigDecimal x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setBigDecimal #" + i); 

        try {
            pstmtImpl.setBigDecimal(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setBigDecimal", "544", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setBinaryStream(int i, InputStream x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setBinaryStream #" + i); 

        try {
            pstmtImpl.setBinaryStream(i, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setBinaryStream", "1127", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setBinaryStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setBinaryStream", "1192", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setBinaryStream", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setBinaryStream", "1199", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setBinaryStream", err); 
            throw err;
        }
    }

    public void setBinaryStream(int i, java.io.InputStream x, int length) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setBinaryStream #" + i, length); 

        try {
            pstmtImpl.setBinaryStream(i, x, length);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setBinaryStream", "566", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setBinaryStream(int i, InputStream x, long length) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setBinaryStream #" + i, "length = " + length); 

        try {
            pstmtImpl.setBinaryStream(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setBinaryStream", "1127", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setBinaryStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setBinaryStream", "1261", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setBinaryStream", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setBinaryStream", "1268", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setBinaryStream", err); 
            throw err;
        }
    }

    public void setBlob(int i, Blob x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setBlob #" + i); 

        try {
            pstmtImpl.setBlob(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setBlob", "588", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setBlob(int i, InputStream x) throws SQLException 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setBlob #" + i); 

        try {
            pstmtImpl.setBlob(i, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setBlob", "1175", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setBlob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setBlob", "1330", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setBlob", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setBlob", "1337", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setBlob", err); 
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setBlob(int i, InputStream x, long length) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setBlob #" + i, "length = " + length); 

        try {
            pstmtImpl.setBlob(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setBlob", "1283", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setBlob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setBlob", "1299", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setBlob", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setBlob", "1306", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setBlob", err); 
            throw err;
        }
    }

    public void setBoolean(int i, boolean x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setBoolean #" + i); 

        try {
            pstmtImpl.setBoolean(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setBoolean", "610", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setByte(int i, byte x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setByte #" + i); 

        try {
            pstmtImpl.setByte(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setByte", "632", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setBytes(int i, byte[] x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setBytes #" + i); 

        try {
            // - Add special handling for Oracle prepared statements
            mcf.getHelper().psSetBytes(pstmtImpl, i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setBytes", "654", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            FFDCFilter.processException(nullX, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setBytes", "949", this);
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setCharacterStream(int i, Reader x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setCharacterStream #" + i); 

        try {
            pstmtImpl.setCharacterStream(i, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setCharacterStream", "1292", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setCharacterStream", "1490", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setCharacterStream", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setCharacterStream", "1497", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setCharacterStream", err); 
            throw err;
        }
    }

    public void setCharacterStream(int i, java.io.Reader x, int length) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setCharacterStream #" + i, length);

        try {
            pstmtImpl.setCharacterStream(i, x, length);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setCharacterStream", "677", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setCharacterStream(int i, Reader x, long length) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setCharacterStream #" + i, "length = " + length); 

        try {
            pstmtImpl.setCharacterStream(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setCharacterStream", "1292", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setCharacterStream", "1560", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setCharacterStream", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setCharacterStream", "1567", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setCharacterStream", err); 
            throw err;
        }
    }

    public void setClob(int i, Clob x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setClob #" + i); 

        try {
            pstmtImpl.setClob(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setClob", "699", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setClob(int i, Reader x) throws SQLException 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setClob #" + i); 

        try {
            pstmtImpl.setClob(i, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setClob", "1340", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setClob", "1629", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setClob", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setClob", "1636", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setClob", err); 
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setClob(int i, Reader x, long length) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setClob #" + i, "length = " + length); 

        try {
            pstmtImpl.setClob(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setClob", "1534", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setClob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setClob", "1550", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setClob", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setClob", "1558", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setClob", err); 
            throw err;
        }
    }

    public void setDate(int i, Date x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setDate #" + i); 

        try {
            pstmtImpl.setDate(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setDate", "721", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setDate(int i, Date x, Calendar cal) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setDate with Calendar #" + i); 

        try {
            pstmtImpl.setDate(i, x, cal);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setDate", "743", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setDouble(int i, double x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setDouble #" + i); 

        try {
            pstmtImpl.setDouble(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setDouble", "765", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setFloat(int i, float x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setFloat #" + i); 

        try {
            pstmtImpl.setFloat(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setFloat", "787", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setInt(int i, int x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setInt #" + i); 

        try {
            pstmtImpl.setInt(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setInt", "809", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setLong(int i, long x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setLong #" + i); 

        try {
            pstmtImpl.setLong(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setLong", "831", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setNCharacterStream(int i, Reader x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setNCharacterStream #" + i); 

        try {
            pstmtImpl.setCharacterStream(i, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setNCharacterStream", "1499", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setNCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setNCharacterStream", "1853", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setNCharacterStream", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setNCharacterStream", "1860", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setNCharacterStream", err); 
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setNCharacterStream(int i, Reader x, long length) throws SQLException 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setNCharacterStream #" + i, 
                     "length = " + length); 

        try {
            pstmtImpl.setNCharacterStream(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setNCharacterStream", "1499", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setNCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setNCharacterStream", "1901", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setNCharacterStream", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setNCharacterStream", "1908", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setNCharacterStream", err); 
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setNClob(int i, NClob x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setNClob #" + i); 

        try {
            pstmtImpl.setNClob(i, x);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setNClob", "1684", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setNClob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setNClob", "1700", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setNClob", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setNClob", "1707", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setNClob", err); 
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setNClob(int i, Reader x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setNClob #" + i); 

        try {
            pstmtImpl.setNClob(i, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setNClob", "1525", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setNClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setNClob", "1993", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setNClob", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setNClob", "2000", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setNClob", err); 
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setNClob(int i, Reader x, long length) throws SQLException 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setNClob #" + i, "length = " + length); 

        try {
            pstmtImpl.setNClob(i, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setNClob", "1525", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setNClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setNClob", "2040", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setNClob", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setNClob", "2047", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setNClob", err); 
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setNString(int i, String x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setNString #" + i); 

        try {
            pstmtImpl.setNString(i, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setNString", "1551", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setNString", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setNString", "2087", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setNString", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setNString", "2094", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setNString", err); 
            throw err;
        }
    }

    public void setNull(int i, int sqlType) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setNull #" + i, 
                     AdapterUtil.getSQLTypeString(sqlType));

        try {
            pstmtImpl.setNull(i, sqlType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setNull", "854", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setNull(int i, int sqlType, String typeName) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setNull #" + i, AdapterUtil.getSQLTypeString(sqlType), typeName);

        try {
            pstmtImpl.setNull(i, sqlType, typeName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setNull", "877", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setObject(int i, Object x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setObject #" + i); 

        try {
            pstmtImpl.setObject(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setObject", "899", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setObject(int i, Object x, int targetSqlType) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setObject #" + i, targetSqlType); 

        try {
            pstmtImpl.setObject(i, x, targetSqlType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setObject", "921", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setObject(int i, Object x, int targetSqlType, int scale) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setObject #" + i, targetSqlType, scale);

        try {
            pstmtImpl.setObject(i, x, targetSqlType, scale);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setObject", "944", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setRef(int i, Ref x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setRef #" + i); 

        try {
            pstmtImpl.setRef(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setRef", "966", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setRowId(int i, RowId x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setRowId #" + i); 

        try {
            pstmtImpl.setRowId(i, x);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setRowId", "1957", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setRowId", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setRowId", "1973", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setRowId", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setRowId", "1980", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setRowId", err); 
            throw err;
        }
    }

    public void setShort(int i, short x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setShort #" + i); 

        try {
            pstmtImpl.setShort(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setShort", "988", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setSQLXML(int i, SQLXML xml) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setSQLXML #" + i); 

        try {
            pstmtImpl.setSQLXML(i, xml);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setSQLXML", "2024", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("PreparedStatement.setSQLXML", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setSQLXML", "2040", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setSQLXML", runX); 
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setSQLXML", "2047", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "setSQLXML", err); 
            throw err;
        }
    }

    public void setString(int i, String x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setString #" + i); 

        try {
            // - Add special handling for Oracle prepared statements
            mcf.getHelper().psSetString(pstmtImpl, i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setString", "1010", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            FFDCFilter.processException(nullX, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setString", "1307", this);
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setTime(int i, Time x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setTime #" + i); 

        try {
            pstmtImpl.setTime(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setTime", "1032", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setTime(int i, Time x, Calendar cal) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setTime with Calendar #" + i); 

        try {
            pstmtImpl.setTime(i, x, cal);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setTime", "1054", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setTimestamp(int i, Timestamp x) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setTimestamp #" + i); 

        try {
            pstmtImpl.setTimestamp(i, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setTimestamp", "1076", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setTimestamp(int i, Timestamp x, Calendar cal) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setTimestamp with Calendar #" + i); 

        try {
            pstmtImpl.setTimestamp(i, x, cal);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setTimestamp", "1098", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @deprecated
     */
    public void setUnicodeStream(int i, java.io.InputStream x, int length) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setUnicodeStream #" + i, length); 

        try {
            pstmtImpl.setUnicodeStream(i, x, length);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setUnicodeStream", "1120", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Sets the designated parameter to the given java.net.URL value. The driver
     * converts this to an SQL DATALINK value when it sends it to the database.</p>
     * 
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param url the java.net.URL object to be set
     * 
     * @exception SQLException If a database access error occurs
     */
    public void setURL(int i, java.net.URL url) throws SQLException { 
        // - don't trace user data url.
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setURL", i); 

        try {
            pstmtImpl.setURL(i, url);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setURL", "1140", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Method getParameterMetaData.
     * <p>Retrieves the number, types and properties of this PreparedStatement object's
     * parameters. </p>
     * 
     * @return a ParameterMetaData object that contains information about the number,
     *         types and properties of this PreparedStatement object's parameters
     * 
     * @throws SQLException If a database access error occurs
     */
    public ParameterMetaData getParameterMetaData() throws SQLException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "getParameterMetaData");
        ParameterMetaData pmd = null;

        try {
            pmd = pstmtImpl.getParameterMetaData();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.getParameterMetaData", "1442", this);
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "getParameterMetaData", "Exception, details in FFDC"); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "getParameterMetaData", "Exception, details in FFDC"); 
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "getParameterMetaData", pmd);
        return pmd;
    }
}