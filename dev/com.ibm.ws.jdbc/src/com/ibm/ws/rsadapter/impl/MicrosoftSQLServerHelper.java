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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException; 
import java.util.Collections;
import java.util.Properties;

import javax.resource.ResourceException;
import javax.transaction.xa.XAResource;

import com.ibm.ejs.cm.logger.TraceWriter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.jdbc.WSJdbcTracer;

/**
 * Helper for the Microsoft SQL Server JDBC driver.
 */
public class MicrosoftSQLServerHelper extends DatabaseHelper {
    private static final TraceComponent tc = Tr.register(MicrosoftSQLServerHelper.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    @SuppressWarnings("deprecation")
    private transient com.ibm.ejs.ras.TraceComponent jdbcTC = com.ibm.ejs.ras.Tr.register("com.ibm.ws.sqlserver.logwriter", "WAS.database", null);

    /**
     * Cached copy (per data store helper) of stmt.setResponseBuffering
     */
    private Method methodSetResponseBuffering;

    private static final String RESPONSE_BUFFERING = "responseBuffering";

    /**
     * ResponseBuffering configured on the data source.
     */
    private String responseBuffering;

    /**
     * Construct a helper class for the Microsoft SQL Server JDBC driver.
     *  
     * @param mcf managed connection factory
     */
    MicrosoftSQLServerHelper(WSManagedConnectionFactoryImpl mcf) {
        super(mcf);

        dataStoreHelperClassName = "com.ibm.websphere.rsadapter.MicrosoftSQLServerDataStoreHelper";

        mcf.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ;
        mcf.supportsGetTypeMap = false;
        mcf.supportsIsReadOnly = false;

        // Default value for the statement property ResponseBuffering is
        // configurable as a data source property. This data source property is supplied to
        // the data store helper so that we can reset the statement properties to the default
        // value when caching statements.
        Properties props = mcf.dsConfig.get().vendorProps;
        responseBuffering = props.containsKey(RESPONSE_BUFFERING) ?
                            props.getProperty(RESPONSE_BUFFERING) :
                            "adaptive"; // default

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Default responseBuffering = " + responseBuffering);
        
        Collections.addAll(staleConCodes,
                           230,
                           6001,
                           6002,
                           6005,
                           6006);
    }

    /**
     * Returns the XA start flag for loose or tight branch coupling
     *
     * @param couplingType branch coupling type
     * @return XA start flag value for the specified coupling type
     */
    @Override
    public int branchCouplingSupported(int couplingType) {
        // TODO remove this check at GA
        if (!mcf.dsConfig.get().enableBranchCouplingExtension)
            return super.branchCouplingSupported(couplingType);

        if (couplingType == ResourceRefInfo.BRANCH_COUPLING_TIGHT)
            return 0x8000; // value of SQLServerXAResource.SSTRANSTIGHTLYCPLD (32768)

        // Loose branch coupling is default for Microsoft SQL Server
        return XAResource.TMNOFLAGS;
    }

    @Override
    public void doStatementCleanup(PreparedStatement stmt) throws SQLException {
        if (dataStoreHelper != null) {
            doStatementCleanupLegacy(stmt);
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "doStatementCleanup", AdapterUtil.toString(stmt));

        stmt.setCursorName(null); // ignored by the JDBC driver
        stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
        stmt.setMaxFieldSize(2147483647); // the JDBC driver's default value
        stmt.setMaxRows(0);

        Integer queryTimeout = mcf.dsConfig.get().queryTimeout;
        if (queryTimeout == null)
            queryTimeout = defaultQueryTimeout;
        stmt.setQueryTimeout(queryTimeout);

        stmt = (PreparedStatement) WSJdbcTracer.getImpl(stmt);

        // Invoke via reflection:
        //   stmt.setResponseBuffering(responseBuffering);

        if (responseBuffering != null && methodSetResponseBuffering == null)
            try {
                methodSetResponseBuffering =
                                stmt.getClass().getMethod("setResponseBuffering", new Class[] { String.class });
            } catch (NoSuchMethodException x) {
                // No FFDC code needed. The JDBC driver version used does not have the
                // setResponseBuffering method.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "ResponseBuffering not supported.");
                responseBuffering = null;
            }

        if (responseBuffering != null)
            try {
                methodSetResponseBuffering.invoke(stmt, new Object[] { responseBuffering });
            } catch (InvocationTargetException x) {
                // setResponseBuffering raised an error when run.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(this, tc, "doStatementCleanup", x.getCause()); 
                throw (SQLException) x.getCause();
            } catch (IllegalAccessException x) {
                // No FFDC code needed. The JDBC driver version used does not permit access to
                // the setResponseBuffering method.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "ResponseBuffering not supported."); 
                responseBuffering = null;
            }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "doStatementCleanup"); 
    }

    /**
     * This returns a <code>PrintWriter</code> for a specific
     * backend. The order of printwriter lookup is as follows:
     * first, the returned value from the <code>externalhelper.getPrintWriter() </code>,
     * which also can be overwritten by extending the helper<br>
     * then, based on the trace writer (i.e. the Websphere trace setting) <br>
     * 
     * @return
     *         <CODE>PrintWriter </CODE>
     * @exception ResourceException if something goes wrong.
     */
    @Override
    public PrintWriter getPrintWriter() throws ResourceException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "getPrintWriter"); 

        //not synchronizing here since there will be one helper
        // and most likely the setting will be serially, even if it's not, 
        // it shouldn't matter here (tracing).
        if (genPw == null) {
            genPw = new PrintWriter(new TraceWriter(jdbcTC), true);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "getPrintWriter", genPw);
        return genPw;
    }

    /**
     * Returns a trace component for supplemental JDBC driver level trace.
     * 
     * @return the trace component for supplemental trace.
     */
    @Override
    public com.ibm.ejs.ras.TraceComponent getTracer() {
        return jdbcTC;
    }

    /**
     * Determine if the top level exception is an authorization exception.
     * Chained exceptions are not checked.
     * 
     * For Microsoft SQL Server, look for the JDBC 4.0 exception subclass
     * or the 28xxx SQLState (Invalid authorization specification)
     * or an SQL Server error code in (18450..18452, 18456..18461, 18470, 18483, 18485..18488)
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
               || ec >= 18450 && ec <= 18452
               || ec >= 18456 && ec <= 18461
               || ec == 18470
               || ec == 18483
               || ec >= 18485 && ec <= 18488;

        // 18450: Login failed for login "%.*ls". The login is not defined as a valid login of a trusted SQL Server connection.%.*ls
        // 18451: Login failed for user '%.*ls'. Only administrators may connect at this time.%.*ls
        // 18452: Login failed for user '%.*ls'. The user is not associated with a trusted SQL Server connection.%.*ls
        // 18456: Login failed for user '%.*ls'.%.*ls
        // 18457: Login failed for user '%.*ls'. The user name contains a mapping character or is longer than 30 characters.%.*ls
        // 18458: Login failed. The number of simultaneous users already equals the %d registered licenses for this server...
        // 18459: Login failed. The workstation licensing limit for SQL Server access has already been reached.%.*ls
        // 18460: Login failed. The number of simultaneous users has already reached the limit of %d licenses ...
        // 18461: Login failed for user '%.*ls'. Reason: Server is in single user mode. Only one administrator can connect ...
        // 18470: Login failed for user '%.*ls'. Reason: The account is disabled.%.*ls
        // 18483: Could not connect to server '%.*ls' because '%.*ls' is not defined as a remote login at t...
        // 18485: Could not connect to server '%.*ls' because it is not configured to accept remote logins....
        // 18486: Login failed for user '%.*ls' because the account is currently locked out. The system adm...
        // 18487: Login failed for SQL Server login '%.*ls'. The password for this login has expired.%.*ls
        // 18488: Login failed for user '%.*ls'.  Reason: The password of the account must be changed.%.*ls;
    }

    /**
     * This method checks if enabling jdbc logging is in order(i.e. if a call to enableJDBCLogging is needed).
     * The boundaries by which the method is called: <br>
     * <li> when a new connection is created </li>
     * 
     * @param mcf WSManagedConnectionFactoryImpl
     * @return boolean true if trace should be enabled, false otherwise
     */
    @Override
    public boolean shouldTraceBeEnabled(WSManagedConnectionFactoryImpl mcf) {
        // here will base this on the mcf since, the value is enabled for the system
        // as a whole
        if (TraceComponent.isAnyTracingEnabled() && jdbcTC.isDebugEnabled() && !mcf.loggingEnabled) 
            return true;
        return false;
    }

    /**
     * This method checks if disabling jdbc logging is needed. (i.e. if a call to disableJdbcLogging is needed).
     * The boundaries by which the method is called: <br>
     * <li> before we hand out a connection in mc.getConnection </li>
     * <li> during cleanup of the connection </li>
     * 
     * @param mc WSRdbManagedConnectionImpl
     * @return boolean true if trace should be disabled, false otherwise.
     */
    @Override
    public boolean shouldTraceBeDisabled(WSRdbManagedConnectionImpl mc) {
        if (TraceComponent.isAnyTracingEnabled() && !jdbcTC.isDebugEnabled() && mc.mcf.loggingEnabled) 
            return true;
        return false;
    }

    /**
     * Indicates if the JDBC driver supports propagating the GSS credential for kerberos
     * to the JDBC driver by obtaining the connection within Subject.doAs.
     * 
     * @return true if version 4.0 or higher, or if we don't know the version (because a connection hasn't been established yet).
     */
    @Override
    public boolean supportsSubjectDoAsForKerberos() {
        return driverMajorVersion >= 4 // JavaKerberos feature added in version 4.0 of JDBC driver.
               || driverMajorVersion == 0; // Unknown version, so allow it to be attempted.
    }
}