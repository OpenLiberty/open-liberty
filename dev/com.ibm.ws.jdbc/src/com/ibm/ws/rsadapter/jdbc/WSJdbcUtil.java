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

import java.sql.SQLException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.impl.WSRdbManagedConnectionImpl;

/**
 * This is a utility class containing static methods for use throughout the JDBC code.
 */
public class WSJdbcUtil
{
    private static final TraceComponent tc = Tr.register(WSJdbcUtil.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Performs special handling for stale statements, such as clearing the statement cache
     * and marking existing statements non-poolable.
     * 
     * @param jdbcWrapper the JDBC wrapper on which the error occurred.
     */
    public static void handleStaleStatement(WSJdbcWrapper jdbcWrapper) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "Encountered a Stale Statement: " + jdbcWrapper);

        if (jdbcWrapper instanceof WSJdbcObject)
            try {
                WSJdbcConnection connWrapper =
                                (WSJdbcConnection) ((WSJdbcObject) jdbcWrapper).getConnectionWrapper(); 

                WSRdbManagedConnectionImpl mc = connWrapper.managedConn;

                // Instead of closing the statements, mark them as
                // not poolable so that they are prevented from being cached again when closed.
                connWrapper.markStmtsAsNotPoolable(); 

                // Clear out the cache.
                if (mc != null)
                    mc.clearStatementCache();
            } catch (NullPointerException nullX) {
                // No FFDC code needed; probably closed by another thread.
                if (!((WSJdbcObject) jdbcWrapper).isClosed())
                    throw nullX; 
            }
    }

    /**
     * Map a SQLException. And, if it's a connection error, send a CONNECTION_ERROR_OCCURRED
     * ConnectionEvent to all listeners of the Managed Connection.
     * 
     * @param jdbcWrapper the WebSphere JDBC wrapper object throwing the exception.
     * @param sqlX the SQLException to map.
     * 
     * @return A mapped SQLException subclass, if the SQLException maps. Otherwise, the
     *         original exception.
     */
    public static SQLException mapException(WSJdbcWrapper jdbcWrapper, SQLException sqlX) {
        Object mapper = null;
        WSJdbcConnection connWrapper = null;

        if (jdbcWrapper instanceof WSJdbcObject) {
            // Use the connection and managed connection.
            connWrapper = (WSJdbcConnection) ((WSJdbcObject) jdbcWrapper).getConnectionWrapper();
            if (connWrapper != null) {
                mapper = connWrapper.isClosed() ? connWrapper.mcf : connWrapper.managedConn;
            }
        } else
            mapper = jdbcWrapper.mcf;

        return (SQLException) AdapterUtil.mapException(sqlX, connWrapper, mapper, true);
    }
}
