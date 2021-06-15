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

/**
 * Helper class for Derby.
 * A more specific subclass covers the Derby Network Client driver.
 */
public class DerbyHelper extends DatabaseHelper {
    @SuppressWarnings("deprecation")
    protected static final com.ibm.ejs.ras.TraceComponent derbyTc = com.ibm.ejs.ras.Tr.register("com.ibm.ws.derby.logwriter", "WAS.database", null); // rename 

    /**
     * Construct a helper class for Derby.
     * A more specific subclass covers the Derby Network Client driver.
     *  
     * @param mcf managed connection factory
     */
    DerbyHelper(WSManagedConnectionFactoryImpl mcf) {
        super(mcf);

        dataStoreHelperClassName = "com.ibm.websphere.rsadapter.DerbyDataStoreHelper";

        mcf.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ;
        mcf.supportsGetTypeMap = false;
        
        Collections.addAll(staleConCodes,
                           40000,
                           45000,
                           50000);

        Collections.addAll(staleStmtCodes,
                        "XCL10");
    }

    @Override
    public void doStatementCleanup(PreparedStatement stmt) throws SQLException {
        if (dataStoreHelper != null) {
            doStatementCleanupLegacy(stmt);
            return;
        }

        stmt.setCursorName(null);
        stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
        stmt.setMaxFieldSize(0);
        stmt.setMaxRows(0);

        Integer queryTimeout = mcf.dsConfig.get().queryTimeout;
        if (queryTimeout == null)
            queryTimeout = defaultQueryTimeout;
        stmt.setQueryTimeout(queryTimeout);
    }

    /**
     * Returns a trace component for supplemental JDBC driver level trace.
     * 
     * @return the trace component for supplemental trace.
     */
    @Override
    public com.ibm.ejs.ras.TraceComponent getTracer() {
        return derbyTc;
    }

    @Override
    public PrintWriter getPrintWriter() throws ResourceException {
        //not synchronizing here since there will be one helper
        // and most likely the setting will be serially, even if its not, 
        // it shouldn't matter here (tracing).
        if (genPw == null) {
            genPw = new java.io.PrintWriter(new TraceWriter(derbyTc), true);
        }
        Tr.debug(derbyTc, "returning", genPw);
        return genPw;
    }

    /**
     * Determine if the top level exception is an authorization exception.
     * Chained exceptions are not checked.
     * 
     * Look for the JDBC 4.0 exception subclass
     * or an SQLState in (08004, 04501)
     * 
     * @param x the exception to check.
     * @return true or false to indicate if the exception is an authorization error.
     * @throws NullPointerException if a NULL exception parameter is supplied.
     */
    @Override
    boolean isAuthException(SQLException x) {
        return x instanceof SQLInvalidAuthorizationSpecException
               || "08004".equals(x.getSQLState()) // user authorization error
               || "04501".equals(x.getSQLState()); // user authentication error (no permission to access database)
    }

    @Override
    public boolean shouldTraceBeEnabled(WSManagedConnectionFactoryImpl mcf) {
        // here will base this on the mcf since, the value is enabled for the system
        // as a whole
        if (TraceComponent.isAnyTracingEnabled() && derbyTc.isDebugEnabled() && !mcf.loggingEnabled) 
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
        if (TraceComponent.isAnyTracingEnabled() && !derbyTc.isDebugEnabled() && mc.mcf.loggingEnabled) 
            return true;

        return false;
    }

}
