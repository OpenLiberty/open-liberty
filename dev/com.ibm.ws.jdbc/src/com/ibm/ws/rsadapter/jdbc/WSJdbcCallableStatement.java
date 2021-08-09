/*******************************************************************************
 * Copyright (c) 2001, 2017 IBM Corporation and others.
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.impl.StatementCacheKey;

/**
 * This class wraps a CallableStatement.
 */
public class WSJdbcCallableStatement extends WSJdbcPreparedStatement
                implements CallableStatement {
    private static final TraceComponent tc = Tr.register(
                                                         WSJdbcCallableStatement.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /** The underlying CallableStatement object. */
    protected CallableStatement cstmtImpl; 

    /**
     * Create a WebSphere CallableStatement wrapper for a CallableStatement.
     * 
     * @param cstmtImplObject the JDBC CallableStatement implementation class to be wrapped.
     * @param connWrapper the WebSphere JDBC Connection wrapper creating this statement.
     * @param theHoldability the cursor holdability value of this statement
     * @param cstmtSQL the SQL for this callable statement. 
     * 
     * @throws SQLException if an error occurs wrapping the CallableStatement.
     */
    public WSJdbcCallableStatement(CallableStatement cstmtImplObject, WSJdbcConnection connWrapper, 
                                      int theHoldability, String cstmtSQL) 
    throws SQLException 
    {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "<init>",
                     AdapterUtil.toString(cstmtImplObject), connWrapper, AdapterUtil.getCursorHoldabilityString(theHoldability)); 

        stmtImpl = pstmtImpl = cstmtImpl = cstmtImplObject;
        init(connWrapper); 
        sql = cstmtSQL; 

        holdability = theHoldability; 

        mcf = parentWrapper.mcf;

        try {
            currentFetchSize = cstmtImpl.getFetchSize();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.<init>", "66", this);

            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "<init>", "Exception");
            throw WSJdbcUtil.mapException(this, ex);
        }

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "current fetchSize is " + currentFetchSize);

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>");
    }

    /**
     * Create a WebSphere CallableStatement wrapper for a cacheable CallableStatement.
     * 
     * @param cstmtImplObject the JDBC CallableStatement implementation class to wrap.
     * @param connWrapper the WebSphere JDBC Connection wrapper creating this statement.
     * @param theHoldability the cursor holdability value of this statement
     * @param cstmtSQL the SQL for this callable statement. 
     * @param cstmtKey the statement cache key for this CallableStatement.
     * 
     * @throws SQLException if an error occurs wrapping the CallableStatement.
     */
    public WSJdbcCallableStatement(CallableStatement cstmtImplObject, WSJdbcConnection connWrapper,
                                      int theHoldability, String cstmtSQL, 
                                      StatementCacheKey cstmtKey) throws SQLException 
    {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "<init>", AdapterUtil.toString(cstmtImplObject), connWrapper,
                     AdapterUtil.getCursorHoldabilityString(theHoldability), cstmtKey);

        stmtImpl = pstmtImpl = cstmtImpl = cstmtImplObject;
        init(connWrapper); 
        sql = cstmtSQL; 
        key = cstmtKey;

        holdability = theHoldability; 

        mcf = parentWrapper.mcf;

        try {
            currentFetchSize = cstmtImpl.getFetchSize();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.<init>", "111", this);

            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "<init>", "Exception");
            throw WSJdbcUtil.mapException(this, ex);
        }

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "current fetchSize is " + currentFetchSize);

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>");
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
        cstmtImpl = null;

        // The PreparedStatement wrapper contains all of the logic for caching or closing the
        // the underlying statement.  The statement cache key will ensure the statement is
        // cached as a CallableStatement instead of a PreparedStatement.

        return super.closeWrapper(closeWrapperOnly); 
    }

    /**
     * Construct and track a new result set wrapper.
     * 
     * @param rsetImpl result set to wrap.
     * @return wrapper
     */
    protected WSJdbcResultSet createWrapper(ResultSet rsetImpl) {
        // If the childWrapper is null, and the childWrappers is null or 
        // empty, set the result set to childWrapper;
        // Otherwise, add the result set to childWrappers
        WSJdbcResultSet rsetWrapper;
        if (childWrapper == null && (childWrappers == null || childWrappers.isEmpty())) {
            childWrapper = rsetWrapper = mcf.jdbcRuntime.newResultSet(rsetImpl, this);
            if (TraceComponent.isAnyTracingEnabled() &&  tc.isDebugEnabled())
                Tr.debug(tc, "Set the result set to child wrapper"); 
        } else {
            if (childWrappers == null) 
                childWrappers = new ArrayList<Wrapper>(5); 
            rsetWrapper = mcf.jdbcRuntime.newResultSet(rsetImpl, this);
            childWrappers.add(rsetWrapper);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Add the result set to child wrappers list."); 
        }

        return rsetWrapper;
    }

    public Array getArray(int i) throws SQLException {
        try {
            return cstmtImpl.getArray(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getArray", "86", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public java.math.BigDecimal getBigDecimal(int i) throws SQLException {
        try {
            return cstmtImpl.getBigDecimal(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getBigDecimal", "106", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @deprecated
     */
    public java.math.BigDecimal getBigDecimal(int i, int scale) throws SQLException {
        try {
            return cstmtImpl.getBigDecimal(i, scale);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getBigDecimal", "126", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public Blob getBlob(int i) throws SQLException {
        try {
            return cstmtImpl.getBlob(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getBlob", "146", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean getBoolean(int i) throws SQLException {
        try {
            return cstmtImpl.getBoolean(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getBoolean", "166", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public byte getByte(int i) throws SQLException {
        try {
            return cstmtImpl.getByte(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getByte", "186", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public byte[] getBytes(int i) throws SQLException {
        try {
            return cstmtImpl.getBytes(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getBytes", "206", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public Clob getClob(int i) throws SQLException {
        try {
            return cstmtImpl.getClob(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getClob", "226", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Invokes getCursor and wraps the result set.
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
    private Object getCursor(Object implObject, Method method, Object[] args)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                    SQLException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "getCursor", this, args[0]);

        ResultSet rsetImpl = (ResultSet) method.invoke(implObject, args);
        WSJdbcResultSet rsetWrapper = rsetImpl == null ? null : createWrapper(rsetImpl);

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, "getCursor", rsetWrapper);
        return rsetWrapper;
    }

    public Date getDate(int i) throws SQLException {
        try {
            return cstmtImpl.getDate(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getDate", "246", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public Date getDate(int i, Calendar cal) throws SQLException {
        try {
            return cstmtImpl.getDate(i, cal);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getDate", "266", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public double getDouble(int i) throws SQLException {
        try {
            return cstmtImpl.getDouble(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getDouble", "286", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public float getFloat(int i) throws SQLException {
        try {
            return cstmtImpl.getFloat(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getFloat", "306", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getInt(int i) throws SQLException {
        try {
            return cstmtImpl.getInt(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getInt", "326", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public long getLong(int i) throws SQLException {
        try {
            return cstmtImpl.getLong(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getLong", "346", this);
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
    public Reader getNCharacterStream(int i) throws SQLException {
        try {
            return cstmtImpl.getNCharacterStream(i); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getNCharacterStream", "491", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.getNCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getNCharacterStream", "505", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getNCharacterStream", "512", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public Reader getNCharacterStream(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getNCharacterStream(parameterName); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getNCharacterStream", "520", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.getNCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getNCharacterStream", "550", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getNCharacterStream", "557", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public NClob getNClob(int i) throws SQLException {
        try {
            return cstmtImpl.getNClob(i);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getNClob", "552", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.getNClob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getNClob", "568", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getNClob", "575", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public NClob getNClob(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getNClob(parameterName);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getNClob", "595", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.getNClob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getNClob", "611", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getNClob", "618", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public String getNString(int i) throws SQLException {
        try {
            return cstmtImpl.getNString(i); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getNString", "549", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.getNString", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getNString", "681", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNString", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getNString", "688", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNString", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public String getNString(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getNString(parameterName); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getNString", "578", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.getNString", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getNString", "726", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNString", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getNString", "733", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getNString", err);
            throw err;
        }
    }

    public Object getObject(int i) throws SQLException {
        try {
            return cstmtImpl.getObject(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getObject", "366", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public Object getObject(int i, java.util.Map<String, Class<?>> map) throws SQLException {
        try {
            return cstmtImpl.getObject(i, map);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getObject", "386", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public Ref getRef(int i) throws SQLException {
        try {
            return cstmtImpl.getRef(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getRef", "406", this);
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
    public RowId getRowId(int i) throws SQLException {
        try {
            return cstmtImpl.getRowId(i);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getRowId", "753", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.getRowId", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getRowId", "769", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getRowId", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getRowId", "776", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getRowId", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public RowId getRowId(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getRowId(parameterName);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getRowId", "796", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.getRowId", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getRowId", "812", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getRowId", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getRowId", "819", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getRowId", err);
            throw err;
        }
    }

    public short getShort(int i) throws SQLException {
        try {
            return cstmtImpl.getShort(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getShort", "426", this);
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
    public SQLXML getSQLXML(int i) throws SQLException {
        try {
            return cstmtImpl.getSQLXML(i);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getSQLXML", "858", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.getSQLXML", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getSQLXML", "874", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getSQLXML", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getSQLXML", "881", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getSQLXML", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public SQLXML getSQLXML(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getSQLXML(parameterName);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getSQLXML", "901", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.getSQLXML", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getSQLXML", "917", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getSQLXML", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getSQLXML", "924", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getSQLXML", err);
            throw err;
        }
    }

    public String getString(int i) throws SQLException {
        try {
            return cstmtImpl.getString(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getString", "446", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public Time getTime(int i) throws SQLException {
        try {
            return cstmtImpl.getTime(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getTime", "466", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public Time getTime(int i, Calendar cal) throws SQLException {
        try {
            return cstmtImpl.getTime(i, cal);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getTime", "486", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public Timestamp getTimestamp(int i) throws SQLException {
        try {
            return cstmtImpl.getTimestamp(i);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getTimestamp", "506", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public Timestamp getTimestamp(int i, Calendar cal) throws SQLException {
        try {
            return cstmtImpl.getTimestamp(i, cal);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getTimestamp", "526", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @return the trace component for the WSJdbcCallableStatement.
     */
    @Override
    final protected TraceComponent getTracer() 
    {
        return tc;
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
        if (args != null && args.length == 1 && method.getName().equals("getCursor"))
            return getCursor(implObject, method, args);
        return super.invokeOperation(implObject, method, args);
    }

    public void registerOutParameter(int i, int sqlType) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "registerOutParameter", i, AdapterUtil.getSQLTypeString(sqlType));

        try {
            cstmtImpl.registerOutParameter(i, sqlType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.registerOutParameter", "555", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void registerOutParameter(int i, int sqlType, int scale) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "registerOutParameter", i, AdapterUtil.getSQLTypeString(sqlType), scale);

        try {
            cstmtImpl.registerOutParameter(i, sqlType, scale);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.registerOutParameter", "576", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void registerOutParameter(int i, int sqlType, String typeName) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "registerOutParameter", i, AdapterUtil.getSQLTypeString(sqlType), typeName);

        try {
            cstmtImpl.registerOutParameter(i, sqlType, typeName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.registerOutParameter", "597", this);
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
    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setAsciiStream #" + parameterName);

        try {
            cstmtImpl.setAsciiStream(parameterName, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setAsciiStream", "858", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setAsciiStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setAsciiStream", "1194", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setAsciiStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setAsciiStream", "1201", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setAsciiStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setAsciiStream(String parameterName, InputStream x, long length)
                    throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setAsciiStream #" + parameterName,
                     "length = " + length);

        try {
            cstmtImpl.setAsciiStream(parameterName, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setAsciiStream", "891", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setAsciiStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setAsciiStream", "1243", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setAsciiStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setAsciiStream", "1250", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setAsciiStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setBinaryStream(String parameterName, InputStream x)
                    throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setAsciiStream #" + parameterName);

        try {
            cstmtImpl.setBinaryStream(parameterName, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setBinaryStream", "923", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setBinaryStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setBinaryStream", "1291", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setBinaryStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setBinaryStream", "1298", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setBinaryStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setBinaryStream(String parameterName, InputStream x, long length)
                    throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setBinaryStream #" + parameterName, 
                     "length = " + length);

        try {
            cstmtImpl.setBinaryStream(parameterName, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setBinaryStream", "956", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setBinaryStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setBinaryStream", "1340", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setBinaryStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setBinaryStream", "1347", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setBinaryStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setBlob(String parameterName, Blob x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setBlob #" + parameterName);

        try {
            cstmtImpl.setBlob(parameterName, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setBlob", "987", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setBlob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setBlob", "1387", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setBlob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setBlob", "1394", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setBlob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setBlob(String parameterName, InputStream x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setBlob #" + parameterName);

        try {
            cstmtImpl.setBlob(parameterName, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setBlob", "1018", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setBlob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setBlob", "1434", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setBlob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setBlob", "1441", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setBlob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setBlob(String parameterName, InputStream x, long length) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setBlob #" + parameterName,
                     "length = " + length);

        try {
            cstmtImpl.setBlob(parameterName, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setBlob", "1050", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setBlob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setBlob", "1482", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setBlob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setBlob", "1489", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setBlob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setCharacterStream(String parameterName, Reader x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setCharacterStream #" + parameterName);

        try {
            cstmtImpl.setCharacterStream(parameterName, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setCharacterStream", "1081", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setCharacterStream", "1529", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setCharacterStream", "1536", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setCharacterStream(String parameterName, Reader x, long length)
                    throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setCharacterStream #" + parameterName,
                     "length = " + length);

        try {
            cstmtImpl.setCharacterStream(parameterName, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setCharacterStream", "1114", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setCharacterStream", "1578", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setCharacterStream", "1585", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setClob(String parameterName, Clob x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setClob #" + parameterName);

        try {
            cstmtImpl.setClob(parameterName, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setClob", "1145", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setClob", "1625", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setClob", "1632", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setClob(String parameterName, Reader x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setClob #" + parameterName);

        try {
            cstmtImpl.setClob(parameterName, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setClob", "1176", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setClob", "1672", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setClob", "1679", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setClob(String parameterName, Reader x, long length) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setClob #" + parameterName,
                     "length = " + length);

        try {
            cstmtImpl.setClob(parameterName, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setClob", "1208", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setClob", "1720", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setClob", "1727", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setNCharacterStream(String parameterName, Reader x) throws SQLException 
    {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setNCharacterStream #" + parameterName);

        try {
            cstmtImpl.setNCharacterStream(parameterName, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setNCharacterStream", "1239", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setNCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setNCharacterStream", "1767", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setNCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setNCharacterStream", "1774", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setNCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setNCharacterStream(String parameterName, Reader x, long length) 
    throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setNCharacterStream #" + parameterName,
                     "length = " + length);

        try {
            cstmtImpl.setNCharacterStream(parameterName, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setNCharacterStream", "1272", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setNCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setNCharacterStream", "1816", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setNCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setNCharacterStream", "1823", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setNCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setNClob(String parameterName, NClob x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setNClob #" + parameterName);

        try {
            cstmtImpl.setNClob(parameterName, x);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setNClob", "1564", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setNClob", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setNClob", "1580", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setNClob", "1587", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setNClob(String parameterName, Reader x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setNClob #" + parameterName);

        try {
            cstmtImpl.setNClob(parameterName, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setNClob", "1303", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setNClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setNClob", "1908", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setNClob", "1915", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setNClob(String parameterName, Reader x, long length) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setNClob #" + parameterName,
                     "length = " + length);

        try {
            cstmtImpl.setNClob(parameterName, x, length); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setNClob", "1335", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setNClob", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setNClob", "1956", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setNClob", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setNClob", "1963", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setNClob", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setNString(String parameterName, String x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setNString #" + parameterName);

        try {
            cstmtImpl.setNString(parameterName, x); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setNString", "1366", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setNString", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setNString", "2003", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setNString", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setNString", "2010", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setNString", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setRowId(String parameterName, RowId x) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setRowId #" + parameterName);

        try {
            cstmtImpl.setRowId(parameterName, x);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setRowId", "1703", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setRowId", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setRowId", "1719", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setRowId", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setRowId", "1726", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setRowId", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setSQLXML(String parameterName, SQLXML xml) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setSQLXML #" + parameterName);

        try {
            cstmtImpl.setSQLXML(parameterName, xml);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".setSQLXML", "1748", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.setSQLXML", methError);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setSQLXML", "1764", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setSQLXML", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setSQLXML", "1771", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setSQLXML", err);
            throw err;
        }
    }

    public boolean wasNull() throws SQLException {
        try {
            return cstmtImpl.wasNull();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.wasNull", "617", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void registerOutParameter(String parameterName, int sqlType) 
    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "registerOutParameter", parameterName, AdapterUtil.getSQLTypeString(sqlType)); 

        try {
            cstmtImpl.registerOutParameter(parameterName, sqlType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.registerOutParameter(String, int)", "721", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void registerOutParameter(
                                     String parameterName,
                                     int sqlType,
                                     int scale)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "registerOutParameter", parameterName, AdapterUtil.getSQLTypeString(sqlType), scale); 

        try {
            cstmtImpl.registerOutParameter(parameterName, sqlType, scale);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.registerOutParameter(String, int, int)", "744", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void registerOutParameter(
                                     String parameterName,
                                     int sqlType,
                                     String typeName)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "registerOutParameter", parameterName, AdapterUtil.getSQLTypeString(sqlType), typeName); 

        try {
            cstmtImpl.registerOutParameter(parameterName, sqlType, typeName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.registerOutParameter(String, int, String)", "766", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setURL(String parameterName, java.net.URL val) throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setURL #" + parameterName); 

        try {
            cstmtImpl.setURL(parameterName, val);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setURL(String, URL)", "783", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setNull(String parameterName, int sqlType)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setNull #" + parameterName,
                     AdapterUtil.getSQLTypeString(sqlType)); 

        try {
            cstmtImpl.setNull(parameterName, sqlType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setNull(String, int)", "804", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setBoolean(String parameterName, boolean x)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setBoolean #" + parameterName); 

        try {
            cstmtImpl.setBoolean(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setBoolean(String, boolean)", "823", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setByte(String parameterName, byte x) throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setByte #" + parameterName); 

        try {
            cstmtImpl.setByte(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setByte(String, byte)", "841", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setShort(String parameterName, short x) throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setShort #" + parameterName); 

        try {
            cstmtImpl.setShort(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setShort(String, short)", "859", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setInt(String parameterName, int x) throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setInt #" + parameterName); 

        try {
            cstmtImpl.setInt(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setint(String, int)", "877", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setLong(String parameterName, long x) throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setLong #" + parameterName); 

        try {
            cstmtImpl.setLong(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setLong(String, long)", "895", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setFloat(String parameterName, float x) throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setFloat #" + parameterName); 

        try {
            cstmtImpl.setFloat(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setFloat(String, float)", "913", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setDouble(String parameterName, double x) throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setDouble #" + parameterName); 

        try {
            cstmtImpl.setDouble(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setDouble(String, x)", "931", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setBigDecimal(String parameterName, java.math.BigDecimal x)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setBigDecimal #" + parameterName); 

        try {
            cstmtImpl.setBigDecimal(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setBigDecimal(String, BigDecimal)", "950", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setString(String parameterName, String x) throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setString #" + parameterName); 

        try {
            cstmtImpl.setString(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setString(String, String)", "967", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setBytes(String parameterName, byte[] x) throws SQLException { 

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setBytes #" + parameterName,
                     x == null ? null : ("length = " + x.length)); 

        try {
            cstmtImpl.setBytes(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setBytes(String, byte[])", "986", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setDate(String parameterName, java.sql.Date x)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setDate #" + parameterName); 

        try {
            cstmtImpl.setDate(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setDate(String, Date)", "1005", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setTime(String parameterName, java.sql.Time x)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setTime #" + parameterName); 

        try {
            cstmtImpl.setTime(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setTime(String, Time)", "1024", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setTimestamp(String parameterName, java.sql.Timestamp x)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setTimestamp #" + parameterName); 

        try {
            cstmtImpl.setTimestamp(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setTimestamp(String, x)", "1043", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setAsciiStream(
                               String parameterName,
                               java.io.InputStream x,
                               int length)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setAsciiStream #" + parameterName,
                     "length = " + length); 

        try {
            cstmtImpl.setAsciiStream(parameterName, x, length);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setAsciiStream(String, InputStream, int)", "1065", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setBinaryStream(
                                String parameterName,
                                java.io.InputStream x,
                                int length)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setBinaryStream #" + parameterName,
                     "length = " + length); 

        try {
            cstmtImpl.setBinaryStream(parameterName, x, length);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setBinaryStream(String, InputDtream, int)", "1087", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setObject(
                          String parameterName,
                          Object x,
                          int targetSqlType,
                          int scale)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setObject #" + parameterName, AdapterUtil.getSQLTypeString(targetSqlType), scale); 

        try {
            cstmtImpl.setObject(parameterName, x, targetSqlType, scale);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setObject(String, Object, int, int)", "1110", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setObject(String parameterName, Object x, int targetSqlType)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setObject #" + parameterName,
                     AdapterUtil.getSQLTypeString(targetSqlType)); 

        try {
            cstmtImpl.setObject(parameterName, x, targetSqlType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setObject(parameterName, x, targetSqlType)", "1129", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setObject(String parameterName, Object x) throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setObject #" + parameterName); 

        try {
            cstmtImpl.setObject(parameterName, x);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setObject(String, Object)", "1147", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setCharacterStream(
                                   String parameterName,
                                   java.io.Reader reader,
                                   int length)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setCharacterStream #" + parameterName,
                     "length = " + length); 

        try {
            cstmtImpl.setCharacterStream(parameterName, reader, length);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setCharacterStream(String, Reader, int)", "1169", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setDate(
                        String parameterName,
                        java.sql.Date x,
                        java.util.Calendar cal)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setDate with Calendar #" + parameterName); 

        try {
            cstmtImpl.setDate(parameterName, x, cal);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setDate(String, Date, calendar)", "1191", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setTime(
                        String parameterName,
                        java.sql.Time x,
                        java.util.Calendar cal)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setTime with Calendar #" + parameterName); 

        try {
            cstmtImpl.setTime(parameterName, x, cal);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setTime(String, Time, Calendar)", "1213", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setTimestamp(
                             String parameterName,
                             java.sql.Timestamp x,
                             java.util.Calendar cal)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setTimestamp with Calendar #" + parameterName); 

        try {
            cstmtImpl.setTimestamp(parameterName, x, cal);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setTimestamp(String, Timestamp, Calendar)", "1235", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void setNull(String parameterName, int sqlType, String typeName)
                    throws SQLException {

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setNull #" + parameterName, AdapterUtil.getSQLTypeString(sqlType), typeName); 

        try {
            cstmtImpl.setNull(parameterName, sqlType, typeName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.setNull(String, int, String)", "1254", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public Array getArray(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getArray(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getArray(String)", "1273", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public java.math.BigDecimal getBigDecimal(String parameterName)
                    throws SQLException {
        try {
            return cstmtImpl.getBigDecimal(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getBigDecimal(String)", "1293", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public java.sql.Blob getBlob(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getBlob(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getBlob(String)", "1312", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean getBoolean(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getBoolean(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getBoolean(String)", "1327", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public byte getByte(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getByte(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getByte(String)", "1346", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public byte[] getBytes(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getBytes(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getBytes(String)", "1363", this);
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
    public Reader getCharacterStream(int i) throws SQLException {
        try {
            return cstmtImpl.getCharacterStream(i); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getCharacterStream", "2173", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.getCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getCharacterStream", "2916", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getCharacterStream", "2923", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getCharacterStream", err);
            throw err;
        }
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public Reader getCharacterStream(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getCharacterStream(parameterName); 
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".getCharacterStream", "2202", this);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (AbstractMethodError methError) {
            // No FFDC code needed; wrong JDBC level.
            throw AdapterUtil.notSupportedX("CallableStatement.getCharacterStream", methError);
        }
        catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".getCharacterStream", "2961", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getCharacterStream", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".getCharacterStream", "2968", this);
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getCharacterStream", err);
            throw err;
        }
    }

    public java.sql.Clob getClob(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getClob(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getClob", "1381", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public java.sql.Date getDate(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getDate(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getDate(String)", "1400", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public java.sql.Date getDate(String parameterName, Calendar cal)
                    throws SQLException {
        try {
            return cstmtImpl.getDate(parameterName, cal);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getDate(String, Calendar)", "1418", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public double getDouble(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getDouble(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getDouble(String)", "1436", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public float getFloat(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getFloat(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getFloat(String)", "1454", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getInt(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getInt(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getInt(String)", "1472", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public long getLong(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getLong(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getLong(String)", "1489", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public Object getObject(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getObject(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getObject(String)", "1506", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    // enhanced method signature.
    public Object getObject(String parameterName, java.util.Map<String, Class<?>> map)
                    throws SQLException {
        try {
            return cstmtImpl.getObject(parameterName, map);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getObject(String, Map)", "1524", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }
    
    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public java.sql.Ref getRef(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getRef(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getRef(String)", "1542", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public short getShort(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getShort(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getShort(String)", "1561", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public String getString(String s) throws SQLException {
        try {
            return cstmtImpl.getString(s);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getString(String)", "1578", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public java.sql.Time getTime(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getTime(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getTime(String)", "1597", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public java.sql.Time getTime(String parameterName, Calendar cal)
                    throws SQLException {
        try {
            return cstmtImpl.getTime(parameterName, cal);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getTime(String, Calendar)", "1618", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public java.sql.Timestamp getTimestamp(String parameterName)
                    throws SQLException {
        try {
            return cstmtImpl.getTimestamp(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getTimestamp(String)", "1637", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public java.sql.Timestamp getTimestamp(String parameterName, Calendar cal)
                    throws SQLException {
        try {
            return cstmtImpl.getTimestamp(parameterName, cal);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getTimestamp(String, Calendar)", "1656", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public java.net.URL getURL(int parameterIndex) throws SQLException {
        try {
            return cstmtImpl.getURL(parameterIndex);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getURL(int)", "1674", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public java.net.URL getURL(String parameterName) throws SQLException {
        try {
            return cstmtImpl.getURL(parameterName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement.getURL(String)", "1692", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }
}