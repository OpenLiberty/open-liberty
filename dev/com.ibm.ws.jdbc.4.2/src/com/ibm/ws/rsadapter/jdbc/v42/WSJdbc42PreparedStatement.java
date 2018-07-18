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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLType;
import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.impl.StatementCacheKey;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;
import com.ibm.ws.rsadapter.jdbc.v41.WSJdbc41PreparedStatement;

public class WSJdbc42PreparedStatement extends WSJdbc41PreparedStatement implements PreparedStatement {

    private static final TraceComponent tc = Tr.register(WSJdbc42PreparedStatement.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Do not use. Constructor exists only for CallableStatement wrapper.
     */
    public WSJdbc42PreparedStatement() {
        super();
    }

    public WSJdbc42PreparedStatement(PreparedStatement pstmtImplObject, WSJdbcConnection connWrapper,
                                     int theHoldability, String pstmtSQL) throws SQLException {
        super(pstmtImplObject, connWrapper, theHoldability, pstmtSQL);
    }

    public WSJdbc42PreparedStatement(PreparedStatement pstmtImplObject, WSJdbcConnection connWrapper,
                                     int theHoldability, String pstmtSQL,
                                     StatementCacheKey pstmtKey) throws SQLException {
        super(pstmtImplObject, connWrapper, theHoldability, pstmtSQL, pstmtKey);
    }

    @Override
    public long getCompatibleUpdateCount() throws SQLException {
        if (mcf.jdbcDriverSpecVersion >= 42 && mcf.supportsGetLargeUpdateCount) {
            try {
                return stmtImpl.getLargeUpdateCount();
            } catch (SQLFeatureNotSupportedException | UnsupportedOperationException notSupp) {
                mcf.supportsGetLargeUpdateCount = false;
            }
        }
        return stmtImpl.getUpdateCount();
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
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setObject(int, Object, SQLType)", "2057", this);
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
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setObject(int, Object, SQLType, int)", "2070", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }
}