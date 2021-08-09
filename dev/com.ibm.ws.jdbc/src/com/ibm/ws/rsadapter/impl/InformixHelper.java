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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException; 
import java.sql.SQLInvalidAuthorizationSpecException; 
import java.util.Collections;

import javax.resource.ResourceException;

import com.ibm.ejs.cm.logger.TraceWriter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.jdbc.WSJdbcStatement; 

/**
 * Helper for the Informix JDBC driver.
 */
public class InformixHelper extends DatabaseHelper {
    private static final TraceComponent tc = Tr.register(InformixHelper.class, "RRA", AdapterUtil.NLS_FILE); 
    @SuppressWarnings("deprecation")
    private transient com.ibm.ejs.ras.TraceComponent infxTc = com.ibm.ejs.ras.Tr.register("com.ibm.ws.informix.logwriter", "WAS.database", null);

    /**
     * Construct a helper class for the Informix JDBC driver.
     *  
     * @param mcf managed connection factory
     */
    InformixHelper(WSManagedConnectionFactoryImpl mcf) {
        super(mcf);

        dataStoreHelperClassName = "com.ibm.websphere.rsadapter.InformixDataStoreHelper";

        mcf.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ;

        Collections.addAll(staleConCodes,
                           -79735,
                           -79716,
                           -43207,
                           -27002,
                           -25580,
                           -908,
                           43012);

        Collections.addAll(staleStmtCodes,
                           -710);
    }

    @Override
    public void doStatementCleanup(PreparedStatement stmt) throws SQLException {
        if (dataStoreHelper != null) {
            doStatementCleanupLegacy(stmt);
            return;
        }

        // Informix doesn't support cursorName
        stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
        stmt.setMaxFieldSize(0);
        stmt.setMaxRows(0);

        Integer queryTimeout = mcf.dsConfig.get().queryTimeout;
        if (queryTimeout == null)
            queryTimeout = defaultQueryTimeout;
        stmt.setQueryTimeout(queryTimeout);
    }

    @Override
    public PrintWriter getPrintWriter() throws ResourceException {
        //not synchronizing here since there will be one helper
        // and most likely the setting will be serially, even if its not, 
        // it shouldn't matter here (tracing).
        if (genPw == null) {
            genPw = new java.io.PrintWriter(new TraceWriter(infxTc), true);
        }
        Tr.debug(infxTc, "returning", genPw);
        return genPw;
    }

    /**
     * Returns a trace component for supplemental JDBC driver level trace.
     * 
     * @return the trace component for supplemental trace.
     */
    @Override
    public com.ibm.ejs.ras.TraceComponent getTracer() {
        return infxTc;
    }

    /**
     * @see com.ibm.ws.rsadapter.spi.DatabaseHelper#getUpdateCount(java.sql.Statement)
     */
    @Override
    public long getUpdateCount(WSJdbcStatement stmt) throws SQLException {
        try {
            return super.getUpdateCount(stmt);
        } catch (SQLException x) {
            // Work around an Informix bug where they are raising an error,
            // "No active result" with error code -79733,
            // instead of returning -1.
            if (x.getErrorCode() == -79733) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Returning stmt.getUpdateCount = -1 to work around Informix bug", x.getMessage());
                return -1;
            }
            else
                throw x;
        }
    }

    /**
     * Determine if the top level exception is an authorization exception.
     * Chained exceptions are not checked.
     * 
     * Look for the JDBC 4.0 exception subclass
     * or the 28xxx SQLState (Invalid authorization specification)
     * or an Informix error code in (-951, -952, -956, -1782, -11018, -11033, -25590, -29007)
     * 
     * @param x the exception to check.
     * @return true or false to indicate if the exception is an authorization error.
     * @throws NullPointerException if a NULL exception parameter is supplied.
     */
    @Override
    boolean isAuthException(SQLException x) {
        int ec = x.getErrorCode();
        return x instanceof SQLInvalidAuthorizationSpecException
               || x.getSQLState() != null && x.getSQLState().startsWith("28")
               || -951 == ec // User username is not known on the database server.
               || -952 == ec // User's password is not correct for the database server.
               || -956 == ec // Client client-name or user is not trusted by the database server.
               || -1782 == ec // The database server cannot validate this user.
               || -11018 == ec // The data source rejected the establishment of the connection for implementation-defined reasons. Confirm that the password and user ID are correct.
               || -11033 == ec // Invalid authorization specification. 
               || -25590 == ec // Authentication error.
               || -29007 == ec; // RDB authorization failure. RDB-userID,RDB: RDB-userID,RDB-name. The user is not authorized to access the target RDB. The request is rejected. Contact the DBA of the RDB side if necessary. Correct the authorization problem and rerun the application program. 
    }

    @Override
    public boolean shouldTraceBeEnabled(WSManagedConnectionFactoryImpl mcf) {
        // here will base this on the mcf since, the value is enabled for the system
        // as a whole
        if (TraceComponent.isAnyTracingEnabled() && infxTc.isDebugEnabled() && !mcf.loggingEnabled) 
            return true;
        return false;
    }

    @Override
    public boolean shouldTraceBeEnabled(WSRdbManagedConnectionImpl mc) {
        //the mc.mcf will be passed to the shouldTraceBeEnabled method that handles WSManagedConnectionFactoryImpl and 
        //get the boolean returned
        return this.shouldTraceBeEnabled(mc.mcf);
    } 

    @Override
    public boolean shouldTraceBeDisabled(WSRdbManagedConnectionImpl mc) {
        if (TraceComponent.isAnyTracingEnabled() && !infxTc.isDebugEnabled() && mc.mcf.loggingEnabled) 
            return true;

        return false;
    }


}