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
package com.ibm.ws.rsadapter.jdbc.v41;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.ConnectionSharing;
import com.ibm.ws.rsadapter.DSConfig;
import com.ibm.ws.rsadapter.impl.WSConnectionRequestInfoImpl;
import com.ibm.ws.rsadapter.impl.WSRdbManagedConnectionImpl;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;

/**
 * This class wraps a JDBC Connection.
 */
public class WSJdbc41Connection extends WSJdbcConnection implements Connection {

    private static final TraceComponent tc = Tr.register(WSJdbc41Connection.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Indicates whether we have warned the customer (when they do setSchema)
     * about the changes to how connection matching is done for connection sharing.
     */
    private static final AtomicBoolean warnedAboutSchemaMatching = new AtomicBoolean();

    /**
     * Indicates whether we have warned the customer (when they do setNetworkTimeout)
     * about the changes to how connection matching is done for connection sharing.
     */
    private static final AtomicBoolean warnedAboutNetworkTimeoutMatching = new AtomicBoolean();

    private boolean aborted = false;

    public WSJdbc41Connection(WSRdbManagedConnectionImpl mc, Connection conn, Object key, Object currentThreadID) {
        super(mc, conn, key, currentThreadID);
    }

    @Override
    public String getSchema() throws SQLException {
        activate();

        try {
            return managedConn.getSchema();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbc41Connection.getSchema", "62", this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        if (warnedAboutSchemaMatching.compareAndSet(false, true))
            Tr.info(tc, "DEFAULT_MATCH_ORIGINAL", "Schema", DSConfig.CONNECTION_SHARING);

        activate();

        try {
            // Setters are not permitted when multiple handles are sharing the same
            // ManagedConnection.   Except we've decided to allow
            // them when the specified value is the same as the current value.
            if (managedConn.getHandleCount() > 1 && !AdapterUtil.match(schema, managedConn.getCurrentSchema()))
                throw createSharingException("setSchema");

            managedConn.setSchema(schema);

            // Update the connection request information with the new value, so that
            // requests for shared connections will match based on the updated criteria.
            if (managedConn.connectionSharing == ConnectionSharing.MatchCurrentState) {
                WSConnectionRequestInfoImpl cri = (WSConnectionRequestInfoImpl) managedConn.getConnectionRequestInfo();
                if (!cri.isCRIChangable()) // only set the cri if its not one of the static ones.
                    managedConn.setCRI(cri = WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                cri.setSchema(schema);
            }
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbc41Connection.setSchema", "96", this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        activate();

        try {
            return managedConn.getNetworkTimeout();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbc41Connection.getSchema", "111", this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (warnedAboutNetworkTimeoutMatching.compareAndSet(false, true))
            Tr.info(tc, "DEFAULT_MATCH_ORIGINAL", "NetworkTimeout", DSConfig.CONNECTION_SHARING);

        activate();

        try {
            // Setters are not permitted when multiple handles are sharing the same
            // ManagedConnection.   Except we've decided to allow
            // them when the specified value is the same as the current value.
            if (managedConn.getHandleCount() > 1 && milliseconds != managedConn.getCurrentNetworkTimeout())
                throw createSharingException("setNetworkTimeout");

            managedConn.setNetworkTimeout(executor, milliseconds);

            // Update the connection request information with the new value, so that
            // requests for shared connections will match based on the updated criteria.
            if (managedConn.connectionSharing == ConnectionSharing.MatchCurrentState) {
                WSConnectionRequestInfoImpl cri = (WSConnectionRequestInfoImpl) managedConn.getConnectionRequestInfo();
                if (!cri.isCRIChangable()) // only set the cri if its not one of the static ones.
                    managedConn.setCRI(cri = WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                cri.setNetworkTimeout(milliseconds);
            }
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbc41Connection.setNetworkTimeout", "145", this);
            throw proccessSQLException(ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        if (!isClosed()) {
            /*
             * Mark this connection as aborted and mark the managed connection.
             */
            setAborted(true);
            managedConn.setAborted(true);
            /*
             * Call abort with the provided exceutor to abort the connection.
             */
            try {
                connImpl.abort(executor);
            } catch (IncompatibleClassChangeError e) {
                // If the JDBC driver was compiled with java 6
                throw new SQLFeatureNotSupportedException();
            }

            fireConnectionErrorEvent(null, false);
        }
    }

    @Override
    public boolean isAborted() throws SQLFeatureNotSupportedException {
        return aborted;
    }

    @Override
    public void setAborted(boolean aborted) throws SQLFeatureNotSupportedException {
        this.aborted = aborted;
    }

    @Override
    protected SQLException proccessSQLException(SQLException ex) {
        // First need to check if a NetworkTimeout occurred
        if (managedConn != null && managedConn.currentNetworkTimeout != 0) { // If NetworkTimeout was changed
            try {
                if (!isClosed() && connImpl != null && connImpl.isClosed()) { // If wrapper not closed, but driver impl is closed
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Network timeout detected, closing wrapper objects.");
                    close(true);
                }
            } catch (SQLException e) {
                // If an exception occurred while trying to check if the connection is closed,
                // log a message and continue normally.
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Exception ocurred while processing SQLException: ", e);
            }
        }

        return super.proccessSQLException(ex);
    }
}