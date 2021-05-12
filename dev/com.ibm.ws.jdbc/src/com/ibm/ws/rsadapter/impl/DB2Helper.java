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
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException; 
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

import com.ibm.ejs.cm.logger.TraceWriter;

import javax.resource.ResourceException;
import javax.transaction.Transaction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.jca.cm.AbstractConnectionFactoryService;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.DSConfig;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

/**
 * Helper class for common DB2 behavior. Do not instantiate this class directly.
 * Always use one of the subclasses which are specific to the JDBC driver that connects to DB2.
 */
public class DB2Helper extends DatabaseHelper {
    private static TraceComponent tc = Tr.register(DB2Helper.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);
    private static final String ZOS_CURRENT_SQLID = "currentSQLID";

    @SuppressWarnings("deprecation")
    protected static final TraceComponent db2Tc = com.ibm.ejs.ras.Tr.register("com.ibm.ws.db2.logwriter", "WAS.database", null);

    HashMap<Object, Class<?>> db2ErrorMap = new HashMap<Object, Class<?>>(37); // this number should change when adding new values

    private String currentSQLid = null;
    String osType;
    boolean isRRSTransaction = false;
    int threadIdentitySupport = AbstractConnectionFactoryService.THREAD_IDENTITY_NOT_ALLOWED;
    boolean threadSecurity = false;
    boolean localZOS = false;
    String productName = null;
    // : the connection type. 1 = jdbc driver; 2 = jcc driver
    static int JDBC = 1; 
    static int SQLJ = 2; 
    int connType = 0; 

    /**
     * Construct a helper class for common DB2 behavior. Do not instantiate this class directly.
     * Always use one of the subclasses which are specific to the JDBC driver that connects to DB2.
     * 
     * @param mcf managed connection factory
     */
    DB2Helper(WSManagedConnectionFactoryImpl mcf) throws Exception {
        super(mcf);

        mcf.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ;
        mcf.doesStatementCacheIsoLevel = true;
        mcf.supportsGetTypeMap = false;

        Properties props = mcf.dsConfig.get().vendorProps;
        currentSQLid = (String) props.get(ZOS_CURRENT_SQLID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "ZOS_CURRENT_SQLID is", currentSQLid);

        if (AdapterUtil.isZOS()) 
            localZOS = true;
        // flag. therefore, it is saved to use this flag to indicate that this is a zOS database.
        if (localZOS) {
            isRRSTransaction = true;
            threadIdentitySupport = AbstractConnectionFactoryService.THREAD_IDENTITY_ALLOWED;
            threadSecurity = true;
        }

        Collections.addAll(staleConCodes,
                           -30108,
                           -30081
                           -30080,
                           -6036,
                           -1229,
                           -1224,
                           -1035,
                           -1034,
                           -1015,
                           -924,
                           -923,
                           -906,
                           "58004");

        staleConCodes.remove("S1000"); // DB2 sometimes uses this SQL state for non-stale. In those cases, rely on error code to detect.

        Collections.addAll(staleStmtCodes,
                           -518,
                           -514);
    }

    /**
     * <p>This method configures a connection before first use. This method is invoked only when a new
     * connection to the database is created. It is not invoked when connections are reused
     * from the connection pool.</p>
     * <p> This class will set a variable db2ZOS to <code>FALSE<code> as default value. This method
     * sets to <code>TRUE<code> if the backend system is zOS.
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

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "doConnectionSetup");

        //set the currrentSQLid on the connection if its not null
        Statement stmt = null;
        Transaction suspendedTx = null; 
        EmbeddableWebSphereTransactionManager tm = mcf.connectorSvc.getTransactionManager();

        try {
            if (currentSQLid != null && !currentSQLid.equals("")) 
            {
                // If the work below is happening under a global transaction scope, we must suspend the 
                // global transaction before we perform the action. That is because if we are to perform an
                // action that implies that a local transaction should on the thread, we need to make sure that
                // the action will take place under a local transaction scope. 
                // If we do not do this, others that are aware of the transaction type currently on
                // the thread (i.e. DB2 T2 jdbc driver) may react in a way that is inconsistent with
                // the actions and expectations below.
                UOWCurrent uow = (UOWCurrent) tm;
                UOWCoordinator coord = uow == null ? null : uow.getUOWCoord();
                boolean inGlobalTransaction = coord != null && coord.isGlobal();
                if (inGlobalTransaction) { 
                    try {
                        suspendedTx = tm.suspend();
                    } catch (Throwable t) {
                        throw new SQLException(t);
                    }
                }

                if (isTraceOn && tc.isDebugEnabled()) 
                {
                    Tr.debug(this, tc, "Setting currentSQLID : " 
                                       + currentSQLid);
                }
                stmt = conn.createStatement();
                String sql = "set current sqlid = '" + currentSQLid + "'";
                stmt.executeUpdate(sql);
            }

        } finally {
            // close the statement
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException e) { 
                com.ibm.ws.ffdc.FFDCFilter.processException(e, getClass().getName(), "231", this);
                if (isTraceOn && tc.isDebugEnabled()) 
                    Tr.debug(this, tc, "SQLException occured in closing the statement ", e);
            } finally { 
                // If there is a suspended transaction, resume it.
                if (suspendedTx != null) {
                    try {
                        tm.resume(suspendedTx);
                    } catch (Throwable t) {
                        throw new SQLException(t);
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "doConnectionSetup");
    }

    /**
     * Feature WS14621 
     * 
     * @return an indicator of the DataSource is RRS-enabled.
     */
    @Override
    public boolean getRRSTransactional() {
        return isRRSTransaction; 
    }

    /**
     * @return string to indicate whether the DataSource allows Thread Identity Support.
     */
    @Override
    public int getThreadIdentitySupport() {
        return threadIdentitySupport; 
    }

    /**
     * Feature WS14621 
     * 
     * @return an boolean to indicate whether the DataSource supports "synch to thread"
     *         for the allocateConnection, i.e., push an ACEE corresponding to the current
     *         java Subject on the native OS thread.
     */
    @Override
    public boolean getThreadSecurity() {
        return threadSecurity; 
    }

    /**
     * @return NULL because DB2 provides sufficient trace of its own.
     */
    @Override
    public com.ibm.ejs.ras.TraceComponent getTracer() {
        return null;
    }

    @Override
    public PrintWriter getPrintWriter() throws ResourceException 
    {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        //not synchronizing here since there will be one helper
        // and most likely the setting will be serially, even if its not,
        // it shouldn't matter here (tracing).

        if (genPw == null) {
            genPw = new PrintWriter(new TraceWriter(db2Tc), true);
        }
        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "returning", genPw);
        return genPw;
    }

    /**
     * Determine if the top level exception is an authorization exception.
     * Chained exceptions are not checked.
     * 
     * Look for the JDBC 4.0 exception subclass
     * or the 28000 SQLState (Invalid authorization specification)
     * or a DB2 error code in (-1403, -4214, -30082)
     * or an exception message containing the text [2013]
     * 
     * @param x the exception to check.
     * @return true or false to indicate if the exception is an authorization error.
     * @throws NullPointerException if a NULL exception parameter is supplied.
     */
    boolean isAuthException(SQLException x) {
        int ec = x.getErrorCode();
        return x instanceof SQLInvalidAuthorizationSpecException
               || "28000".equals(x.getSQLState()) // Authorization name is invalid
               || -1403 == ec // The username and/or password supplied is incorrect.
               || -4214 == ec
               || -30082 == ec // CONNECTION FAILED FOR SECURITY REASON
               // [ibm][db2][jcc][t4][2013][11249] Connection authorization failure occurred.  Reason: User ID or Password invalid.
               || x.getMessage() != null && x.getMessage().indexOf("[2013]") > 0;
    }

    @Override
    public boolean shouldTraceBeEnabled(WSManagedConnectionFactoryImpl mcf) {
        // here will base this on the mcf since, the value is enabled for the system
        // as a whole
        if (db2Tc.isDebugEnabled() && !mcf.loggingEnabled) {
            return true;
        }
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
        if (!db2Tc.isDebugEnabled() && mc.mcf.loggingEnabled)
            return true;

        return false;
    }

    @Override
    public void gatherAndDisplayMetaDataInfo(Connection conn, WSManagedConnectionFactoryImpl mcf) throws SQLException 
    {
        super.gatherAndDisplayMetaDataInfo(conn, mcf);

        if (getDriverName().equalsIgnoreCase("DSNAJDBC")) //means DB2 legacy RRS is being used.
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            {
                Tr.debug(this, tc, "application using <DB2 for zOS Local JDBC Provider (RRS)> which is not supported anymore in WAS6.1");
            }
            Tr.warning(tc, "PROVIDER_NOT_SUPPORTED",
                       "DB2 for zOS Local JDBC Provider (RRS)", "DB2 Universal JDBC Driver Provider Type 2");
            throw new SQLException(AdapterUtil.getNLSMessage("PROVIDER_NOT_SUPPORTED",
                                                             "DB2 for zOS Local JDBC Provider (RRS)", "DB2 Universal JDBC Driver Provider Type 2"));
        }

    }

    public int branchCouplingSupported(int couplingType) 
    {
        // Default is Loose for DB2 in general
        // Tight is not supported by the DB or JDBC driver 
        if (couplingType == ResourceRefInfo.BRANCH_COUPLING_TIGHT) {
            DSConfig config = super.mcf.dsConfig.get();
            Tr.warning(tc, "TBC_NOT_SUPPORTED", config.jndiName == null ? config.id : config.jndiName);
            return -1;
        }

        return javax.transaction.xa.XAResource.TMNOFLAGS;
    }

}