/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc.v42;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLType;
import java.util.Arrays;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.impl.StatementCacheKey;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;
import com.ibm.ws.rsadapter.jdbc.v41.WSJdbc41CallableStatement;

public class WSJdbc42CallableStatement extends WSJdbc41CallableStatement implements CallableStatement {

    private static final TraceComponent tc = Tr.register(WSJdbc42CallableStatement.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    public WSJdbc42CallableStatement(CallableStatement cstmtImplObject, WSJdbcConnection connWrapper,
                                     int theHoldability, String cstmtSQL) throws SQLException {
        super(cstmtImplObject, connWrapper, theHoldability, cstmtSQL);
    }

    public WSJdbc42CallableStatement(CallableStatement cstmtImplObject, WSJdbcConnection connWrapper,
                                     int theHoldability, String cstmtSQL,
                                     StatementCacheKey cstmtKey) throws SQLException {
        super(cstmtImplObject, connWrapper, theHoldability, cstmtSQL, cstmtKey);
    }

    @Override
    public long getCompatibleUpdateCount() throws SQLException {
        return mcf.jdbcDriverSpecVersion >= 42 ? stmtImpl.getLargeUpdateCount() : stmtImpl.getUpdateCount();
    }

    @Override
    public Object getObject(int i) throws SQLException {
        Object o = super.getObject(i);
        if (o instanceof ResultSet) {
            o = createWrapper((ResultSet) o);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getObject", i, o);
        }
        return o;
    }

    @Override
    public Object getObject(int i, Map<String, Class<?>> map) throws SQLException {
        Object o = super.getObject(i, map);
        if (o instanceof ResultSet) {
            o = createWrapper((ResultSet) o);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getObject", i, o, map);
        }
        return o;
    }

    @Override
    public Object getObject(String parameterName) throws SQLException {
        Object o = super.getObject(parameterName);
        if (o instanceof ResultSet) {
            o = createWrapper((ResultSet) o);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getObject", parameterName, o);
        }
        return o;
    }

    @Override
    public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
        Object o = super.getObject(parameterName, map);
        if (o instanceof ResultSet) {
            o = createWrapper((ResultSet) o);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getObject", parameterName, map, o);
        }
        return o;
    }

    @Override
    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        T o = super.getObject(parameterIndex, type);
        if (o instanceof ResultSet) {
            o = type.cast(createWrapper((ResultSet) o));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getObject", parameterIndex, type, o);
        }
        return o;
    }

    @Override
    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        T o = super.getObject(parameterName, type);
        if (o instanceof ResultSet) {
            o = type.cast(createWrapper((ResultSet) o));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getObject", parameterName, type, o);
        }
        return o;
    }

    @Override
    public long getLargeUpdateCount() throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc42Statement, WSJdbc42PreparedStatement,
        // and WSJdbc42CallableStatement because multiple inheritance isn't allowed.
        try {
            return stmtImpl.getLargeUpdateCount();
        } catch (SQLException x) {
            FFDCFilter.processException(x, getClass().getName(), "699", this);
            throw WSJdbcUtil.mapException(this, x);
        } catch (NullPointerException x) {
            throw runtimeXIfNotClosed(x);
        }
    }

    @Override
    public void setLargeMaxRows(long max) throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc42Statement, WSJdbc42PreparedStatement,
        // and WSJdbc42CallableStatement because multiple inheritance isn't allowed.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setLargeMaxRows", max);

        try {
            stmtImpl.setLargeMaxRows(max);
        } catch (SQLException x) {
            FFDCFilter.processException(x, getClass().getName(), "1041", this);
            throw WSJdbcUtil.mapException(this, x);
        } catch (NullPointerException x) {
            throw runtimeXIfNotClosed(x);
        }

        haveStatementPropertiesChanged = true;
    }

    @Override
    public long getLargeMaxRows() throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc42Statement, WSJdbc42PreparedStatement,
        // and WSJdbc42CallableStatement because multiple inheritance isn't allowed.
        try {
            return stmtImpl.getLargeMaxRows();
        } catch (SQLException x) {
            FFDCFilter.processException(x, getClass().getName(), "691", this);
            throw WSJdbcUtil.mapException(this, x);
        } catch (NullPointerException x) {
            throw runtimeXIfNotClosed(x);
        }
    }

    @Override
    public long[] executeLargeBatch() throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc42Statement, WSJdbc42PreparedStatement,
        // and WSJdbc42CallableStatement because multiple inheritance isn't allowed.
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeLargeBatch");

        long[] results;
        try {
            if (childWrapper != null)
                closeAndRemoveResultSet();

            if (childWrappers != null && !childWrappers.isEmpty())
                closeAndRemoveResultSets();

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            results = stmtImpl.executeLargeBatch();

            // Batch parameters are cleared after executing the batch, so reset the batch parameter flag.
            hasBatchParameters = false;
        } catch (SQLException sqlX) {
            // No FFDC code needed. Might be an application error.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeLargeBatch", sqlX);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeLargeBatch", nullX);
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeLargeBatch", Arrays.toString(results));
        return results;
    }

    @Override
    public long executeLargeUpdate(String sql) throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc42Statement, WSJdbc42PreparedStatement,
        // and WSJdbc42CallableStatement because multiple inheritance isn't allowed.
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeLargeUpdate", sql);

        long numUpdates;
        try {
            if (childWrapper != null)
                closeAndRemoveResultSet();

            if (childWrappers != null && !childWrappers.isEmpty())
                closeAndRemoveResultSets();

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            numUpdates = stmtImpl.executeLargeUpdate(sql);
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeLargeUpdate", ex);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeLargeUpdate", nullX);
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeLargeUpdate", numUpdates);
        return numUpdates;
    }

    @Override
    public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc42Statement, WSJdbc42PreparedStatement,
        // and WSJdbc42CallableStatement because multiple inheritance isn't allowed.
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeLargeUpdate", sql, AdapterUtil.getAutoGeneratedKeyString(autoGeneratedKeys));

        long numUpdates;
        try {
            if (childWrapper != null)
                closeAndRemoveResultSet();

            if (childWrappers != null && !childWrappers.isEmpty())
                closeAndRemoveResultSets();

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            numUpdates = stmtImpl.executeLargeUpdate(sql, autoGeneratedKeys);
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeLargeUpdate", ex);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeLargeUpdate", nullX);
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeLargeUpdate", numUpdates);
        return numUpdates;
    }

    @Override
    public long executeLargeUpdate(String sql, int[] columnIndices) throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc42Statement, WSJdbc42PreparedStatement,
        // and WSJdbc42CallableStatement because multiple inheritance isn't allowed.
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeLargeUpdate", sql, Arrays.toString(columnIndices));

        long numUpdates;
        try {
            if (childWrapper != null)
                closeAndRemoveResultSet();

            if (childWrappers != null && !childWrappers.isEmpty())
                closeAndRemoveResultSets();

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            numUpdates = stmtImpl.executeLargeUpdate(sql, columnIndices);
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeLargeUpdate", ex);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeLargeUpdate", nullX);
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeLargeUpdate", numUpdates);
        return numUpdates;
    }

    @Override
    public long executeLargeUpdate(String sql, String[] columnNames) throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc42Statement, WSJdbc42PreparedStatement,
        // and WSJdbc42CallableStatement because multiple inheritance isn't allowed.
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeLargeUpdate", sql, Arrays.toString(columnNames));

        long numUpdates;
        try {
            if (childWrapper != null)
                closeAndRemoveResultSet();

            if (childWrappers != null && !childWrappers.isEmpty())
                closeAndRemoveResultSets();

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            numUpdates = stmtImpl.executeLargeUpdate(sql, columnNames);
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeLargeUpdate", ex);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeLargeUpdate", nullX);
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeLargeUpdate", numUpdates);
        return numUpdates;
    }

    @Override
    public long executeLargeUpdate() throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc42PreparedStatement,
        // and WSJdbc42CallableStatement because multiple inheritance isn't allowed.
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeLargeUpdate");

        long numUpdates;
        try {
            if (childWrapper != null)
                closeAndRemoveResultSet();

            if (childWrappers != null && !childWrappers.isEmpty())
                closeAndRemoveResultSets();

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            numUpdates = pstmtImpl.executeLargeUpdate();
        } catch (SQLException x) {
            // No FFDC code needed. Might be an application error.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeLargeUpdate", x);
            throw WSJdbcUtil.mapException(this, x);
        } catch (RuntimeException x) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeLargeUpdate", x);
            throw x;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeLargeUpdate", numUpdates);
        return numUpdates;
    }

    @Override
    public void setObject(int parameterIndex, Object x, SQLType sqlType) throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc42PreparedStatement,
        // and WSJdbc42CallableStatement because multiple inheritance isn't allowed.
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setObject #" + parameterIndex, sqlType.getName());
        try {
            pstmtImpl.setObject(parameterIndex, x, sqlType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42CallableStatement.setObject(int, Object, SQLType)", "2057", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setObject(int parameterIndex, Object x, SQLType sqlType, int scaleOrLength) throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc42PreparedStatement,
        // and WSJdbc42CallableStatement because multiple inheritance isn't allowed.
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setObject #" + parameterIndex, sqlType.getName(), scaleOrLength);
        try {
            pstmtImpl.setObject(parameterIndex, x, sqlType, scaleOrLength);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42CallableStatement.setObject(int, Object, SQLType, int)", "2070", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void registerOutParameter(int parameterIndex, SQLType sqlType) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "registerOutParameter", parameterIndex, sqlType.getName());
        try {
            cstmtImpl.registerOutParameter(parameterIndex, sqlType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42CallableStatement.registerOutParameter(int, SQLType)", "2612", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void registerOutParameter(int parameterIndex, SQLType sqlType, int scale) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "registerOutParameter", parameterIndex, sqlType.getName(), scale);
        try {
            cstmtImpl.registerOutParameter(parameterIndex, sqlType, scale);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42CallableStatement.registerOutParameter(int, SQLType, int)", "2625", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void registerOutParameter(int parameterIndex, SQLType sqlType, String typeName) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "registerOutParameter", parameterIndex, sqlType.getName(), typeName);
        try {
            cstmtImpl.registerOutParameter(parameterIndex, sqlType, typeName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42CallableStatement.registerOutParameter(int, SQLType, String)", "2638", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void registerOutParameter(String parameterName, SQLType sqlType) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "registerOutParameter", parameterName, sqlType.getName());
        try {
            cstmtImpl.registerOutParameter(parameterName, sqlType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42CallableStatement.registerOutParameter(String, SQLType)", "2651", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void registerOutParameter(String parameterName, SQLType sqlType, int scale) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "registerOutParameter", parameterName, sqlType.getName(), scale);
        try {
            cstmtImpl.registerOutParameter(parameterName, sqlType, scale);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42CallableStatement.registerOutParameter(String, SQLType, int)", "2664", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void registerOutParameter(String parameterName, SQLType sqlType, String typeName) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "registerOutParameter", parameterName, sqlType.getName(), typeName);
        try {
            cstmtImpl.registerOutParameter(parameterName, sqlType, typeName);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42CallableStatement.registerOutParameter(String, SQLType, String)", "2677", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setObject(String parameterName, Object x, SQLType sqlType) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setObject " + parameterName, sqlType.getName());
        try {
            cstmtImpl.setObject(parameterName, x, sqlType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42CallableStatement.setObject(String, Object, SQLType)", "2690", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setObject(String parameterName, Object x, SQLType sqlType, int scaleOrLength) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(this, tc, "setObject " + parameterName, sqlType.getName(), scaleOrLength);
        try {
            cstmtImpl.setObject(parameterName, x, sqlType, scaleOrLength);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42CallableStatement.setObject(String, Object, SQLType, int)", "2703", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }
}