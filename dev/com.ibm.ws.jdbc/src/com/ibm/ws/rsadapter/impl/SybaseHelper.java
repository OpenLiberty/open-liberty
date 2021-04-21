/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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
import java.sql.SQLWarning;
import java.util.Collections;

import javax.resource.ResourceException;
import javax.sql.XADataSource;

import com.ibm.ejs.cm.logger.TraceWriter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.jdbc.WSJdbcStatement; 

/**
 * Helper for Sybase.
 */
public class SybaseHelper extends DatabaseHelper
{
    private static final TraceComponent tc = Tr.register(SybaseHelper.class, "RRA", AdapterUtil.NLS_FILE); 
    @SuppressWarnings("deprecation")
    private transient com.ibm.ejs.ras.TraceComponent sybaseTc = com.ibm.ejs.ras.Tr.register("com.ibm.ws.sybase.logwriter", "WAS.database", null);

    /**
     * Construct a helper class for Sybase.
     *  
     * @param mcf managed connection factory
     */
    SybaseHelper(WSManagedConnectionFactoryImpl mcf) {
        super(mcf);

        dataStoreHelperClassName = "com.ibm.websphere.rsadapter.Sybase11DataStoreHelper";

        mcf.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ;
        mcf.supportsGetTypeMap = false;
        
        Collections.addAll(staleConCodes,
                           "JZ0C0",
                           "JZ0C1");
    }

    @Override
    public boolean doConnectionCleanup(Connection conn) throws SQLException {
        if (dataStoreHelper != null)
            return doConnectionCleanupLegacy(conn);

        boolean standardPropModified = false;

        if (XADataSource.class.isAssignableFrom(mcf.vendorImplClass)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "doConnectionCleanup(): calling setAutoCommit(true) and returning true from this method"); 
            conn.setAutoCommit(true);
            standardPropModified = true;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "doConnectionCleanup(): doing nothing and returning false from this method"); 

        }

        conn.clearWarnings();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "clearWarnings: cleanup of warnings done");

        return standardPropModified;
    }

    /**
     * <p>This method configures a connection before first use. This method is invoked only when a new
     * connection to the database is created. It is not invoked when connections are reused
     * from the connection pool.</p>
     * 
     * <p><code>SybaseHelper</code> checks the <code>SQLWarnings</code> on the
     * connection to determine if a connection was made to the requested database.
     * If the requested database does not exist, Sybase's
     * standard behavior is to connect a "default database" and log a <code>SQLWarning</code>.
     * This method scans for this <code>SQLWarning</code> and if found, throws a
     * <code>SQLException</code> based on the contents of the <code>SQLWarning</code>
     * indicating that a connection to the requested database could not be established.</p>
     * 
     * @param conn the connection to set up.
     * @exception SQLException if connection setup cannot be completed successfully.
     */
    @Override
    public void doConnectionSetup(Connection conn) throws SQLException {
        if (dataStoreHelper != null) {
            doConnectionSetupLegacy(conn);
            return;
        }

        SQLWarning warn = conn.getWarnings();

        if (warn != null) {
            SQLException sqlex = null;
            String sqlstate = warn.getSQLState();

            if (sqlstate.equals("010UF")) { // jConnect cannot connect to the database specified in the connection URL.
                sqlex = new SQLException(warn.getMessage(), sqlstate);
                // OK we have the main exception, now chain all of the rest
                SQLException sqlex2;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                    Tr.debug(this, tc, sqlstate + warn.getMessage());

                warn = warn.getNextWarning();
                while (warn != null) {
                    sqlex2 = new SQLException(warn.getMessage(), warn.getSQLState());
                    sqlex.setNextException(sqlex2);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, warn.getSQLState() + warn.getMessage());
                    warn = warn.getNextWarning();
                }
                throw sqlex;
            }
        }
    }

    @Override
    public void doStatementCleanup(PreparedStatement stmt) throws SQLException {
        if (dataStoreHelper != null) {
            doStatementCleanupLegacy(stmt);
            return;
        }

        // Sybase doesn't support cursor name. Cursor name will
        // be reset when the result set is closed.

        stmt.setFetchDirection(ResultSet.FETCH_FORWARD);

        // For Sybase, the maxFieldSize getter is faster so avoid the setter if possible.
        if (stmt.getMaxFieldSize() != 0)
            stmt.setMaxFieldSize(0);

        stmt.setMaxRows(0);

        Integer queryTimeout = mcf.dsConfig.get().queryTimeout;
        if (queryTimeout == null)
            queryTimeout = defaultQueryTimeout;
        stmt.setQueryTimeout(queryTimeout);
    }

    @Override
    public PrintWriter getPrintWriter() throws ResourceException
    {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        //not synchronizing here since there will be one helper
        // and most likely the setting will be serially, even if its not, 
        // it shouldn't matter here (tracing).
        if (genPw == null)
            genPw = new PrintWriter(new TraceWriter(sybaseTc), true);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "returning", genPw);
        return genPw;
    }

    /**
     * Returns a trace component for supplemental JDBC driver level trace.
     * 
     * @return the trace component for supplemental trace.
     */
    @Override
    public com.ibm.ejs.ras.TraceComponent getTracer()
    {
        return sybaseTc;
    }

    /**
     * @see com.ibm.ws.rsadapter.spi.DatabaseHelper#getUpdateCount(java.sql.Statement)
     */
    public long getUpdateCount(WSJdbcStatement stmt) throws SQLException
    {
        try
        {
            return super.getUpdateCount(stmt);
        } catch (SQLException x)
        {
            // Work around a Sybase bug where they are raising an error,
            // "JZ0PA: The query has been cancelled"
            // instead of returning -1.
            if ("JZ0PA".equals(x.getSQLState()))
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "Returning stmt.getUpdateCount = -1 to work around Sybase bug", x.getMessage());
                return -1;
            }
            else
                throw x;
        }
    }

    @Override
    public void gatherAndDisplayMetaDataInfo(Connection conn, WSManagedConnectionFactoryImpl mcf) throws SQLException 
    {
        setDatabaseProductName("Sybase SQL Server"); // we know the value, so setting it here, to 
                                                     //avoid failing if meta data is disabled.
        try
        {
            super.gatherAndDisplayMetaDataInfo(conn, mcf);                 
        } catch (SQLException x)
        {
            if (isConnectionError(x))
                throw x;

            Tr.info(tc, "META_DATA_EXCEPTION", x.getMessage());

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "meta data access caused a non SCE, continuing without displaying metadata: " + x);

        }
    }

    /**
     * Determine if the top level exception is an authorization exception.
     * Chained exceptions are not checked.
     * 
     * Look for the JDBC 4.0 exception subclass
     * or a Sybase SQLState in (JZ00L, JZ001, JZ002)
     * 
     * @param x the exception to check.
     * @return true or false to indicate if the exception is an authorization error.
     * @throws NullPointerException if a NULL exception parameter is supplied.
     */
    boolean isAuthException(SQLException x)
    {
        return x instanceof SQLInvalidAuthorizationSpecException
               || "JZ00L".equals(x.getSQLState()) // Login failed. Examine the SQLWarnings chained to this exception for the reason(s).
               || "JZ001".equals(x.getSQLState()) // User name property '_____' too long. Maximum length is 30.
               || "JZ002".equals(x.getSQLState()); // Password property '_____' too long. Maximum length is 30.
    }

    /**
     * <p>This method is used to do special handling when method setReadOnly is called.
     * If setReadOnly is called, we ignore it and log an informational message.</p>
     * 
     * @param managedConn WSRdbManagedConnectionImpl object
     * @param readOnly The readOnly value going to be set
     * @param externalCall indicates if the call is done by WAS, or by the user application.
     */
    @Override
    public void setReadOnly(WSRdbManagedConnectionImpl managedConn, boolean readOnly, boolean externalCall)
                    throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "setReadOnly", managedConn, readOnly, externalCall);

        if (!externalCall)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "setReadOnly ignored for internal call");
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "Method setReadOnly() is ignored by WebSphere. Sybase does not honor the setReadOnly method.");
        }
    }

    @Override
    public boolean shouldTraceBeEnabled(WSManagedConnectionFactoryImpl mcf)
    {
        // here will base this on the mcf since, the value is enabled for the system
        // as a whole
        if (sybaseTc.isDebugEnabled() && !mcf.loggingEnabled) 
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldTraceBeEnabled(WSRdbManagedConnectionImpl mc)
    {
        //the mc.mcf will be passed to the shouldTraceBeEnabled method that handles WSManagedConnectionFactoryImpl and 
        //get the boolean returned
        return (this.shouldTraceBeEnabled(mc.mcf));
    }

    @Override
    public boolean shouldTraceBeDisabled(WSRdbManagedConnectionImpl mc)
    {
        if (!sybaseTc.isDebugEnabled() && mc.mcf.loggingEnabled) 
            return true;

        return false;
    }


}
