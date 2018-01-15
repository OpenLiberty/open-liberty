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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;
import com.ibm.ws.rsadapter.jdbc.v41.WSJdbc41Statement;

public class WSJdbc42Statement extends WSJdbc41Statement implements Statement {

    private static final TraceComponent tc = Tr.register(WSJdbc42Statement.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Do not use. Constructor exists only for PreparedStatement wrapper.
     */
    public WSJdbc42Statement() {
        super();
    }

    public WSJdbc42Statement(Statement stmtImplObject, WSJdbcConnection connWrapper, int theHoldability) {
        super(stmtImplObject, connWrapper, theHoldability);
    }

    @Override
    public long getCompatibleUpdateCount() throws SQLException {
        return mcf.jdbcDriverSpecVersion >= 42 ? stmtImpl.getLargeUpdateCount() : stmtImpl.getUpdateCount();
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
}