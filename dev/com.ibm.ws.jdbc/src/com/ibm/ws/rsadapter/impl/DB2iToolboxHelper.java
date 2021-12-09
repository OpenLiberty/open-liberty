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

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jca.cm.AbstractConnectionFactoryService;
import com.ibm.ws.rsadapter.AdapterUtil;

import java.util.Collections;
import java.util.Properties; // 274538
import java.util.concurrent.atomic.AtomicReference;

/**
 * Helper for the iSeries Toolbox DB2 driver.
 */
public class DB2iToolboxHelper extends DB2Helper {
    private static TraceComponent tc = Tr.register(DB2iToolboxHelper.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * DB2 i toolbox classes
     */
    private final AtomicReference<Class<?>> com_ibm_as400_access_AS400JDBCConnectionHandle_class = new AtomicReference<Class<?>>();

    /**
     * DB2 i toolbox methods
     */
    private final AtomicReference<Method> getServerJobIdentifier = new AtomicReference<Method>();

    boolean isolationLevelSwitchingSupport = false; // 274538

    /**
     * Construct a helper class for DB2 iSeries Toolbox JDBC driver.
     * 
     * Now that the DB2 AS400 Toolbox can be used on WAS z/OS to access a remote
     * DB2 AS400 database, make sure that DB2 z/OS flags that are set by the
     * DB2Helper when running on the zOS platform are reset.
     * This is necessary because the DB2Helper when running
     * on the zOS platform assumes that it is working with a local DB2 for zOS
     * JDBC connector.
     * 
     * @param mcf managed connection factory
     */
    DB2iToolboxHelper(WSManagedConnectionFactoryImpl mcf) throws Exception {
        super(mcf);

        dataStoreHelperClassName = "com.ibm.websphere.rsadapter.DB2AS400DataStoreHelper";

        localZOS = false;
        isRRSTransaction = false;
        threadIdentitySupport = AbstractConnectionFactoryService.THREAD_IDENTITY_NOT_ALLOWED;
        threadSecurity = false;

        Properties props = mcf.dsConfig.get().vendorProps;
        Object switchingSupported = props.get("isolationLevelSwitchingSupport");
        if (switchingSupported != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "isolationSwitchingSupported property = " + switchingSupported);

            isolationLevelSwitchingSupport = (switchingSupported instanceof Boolean) ?
                            (Boolean) switchingSupported :
                            Boolean.valueOf((String)switchingSupported);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "isolationSwitchingSupported property not set for this datasource");
        }
        
        // --- The Native driver will return this CLI SQLState (HY017) whenever a connection is no longer available.
        //     This covers the case when an underlying iSeries QSQSRVR prestart job on the iSeries that represents
        //     the jdbc connection in the WAS connection pool is killed and the connection is later used.
        // --- The Toolbox driver will return the SQLState "8S01" when a connection is no longer available
        //
        // **** StaleConnectionException ******
        //     SQLCode   SQLState  Toolbox  Native       DatabaseHelper         DB2Helper        DB2iToolboxHelper
        //     SQ99999   HY017                X                                                       SQLState
        //     SQL0900   08003        X       X           SQLState
        //     SQL0901   58004        X       X                                 SQLState
        //     SQL0906   24514        X       X                                 SQLCode
        //     SQL0858   08S01        X                   SQLState
        //     SQ30080   08001        X       X           SQLState              SQLCode
        // **** DuplicateKeyException *****
        //     SQLCode   SQLState  Toolbox  Native       DatabaseHelper         DB2Helper        DB2iToolboxHelper
        //     SQL0803   23505        X       X           SQLState              SQLCode
        Collections.addAll(staleConCodes,
                "HY017");
    }

    @Override
    public boolean doConnectionCleanup(java.sql.Connection conn) throws SQLException {
        if (dataStoreHelper != null)
            return doConnectionCleanupLegacy(conn);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "doConnectionCleanup");

        SQLWarning warn = conn.getWarnings();
        if (warn == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "doConnectionCleanup(): no warnings to cleanup");
        } else {
            try {
                conn.clearWarnings();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "doConnectionCleanup(): cleanup of warnings done");
            } catch (SQLException se) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "doConnectionCleanup(): cleanup of warnings failed", se);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "doConnectionCleanup");
        return false;
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

    @Override
    public String getCorrelator(final WSRdbManagedConnectionImpl mc) throws SQLException {//202080
        try {
            Class<?> c = com_ibm_as400_access_AS400JDBCConnectionHandle_class.get();
            if (c == null) {
                if(System.getSecurityManager() == null)
                    com_ibm_as400_access_AS400JDBCConnectionHandle_class.set(
                        c = mc.mcf.jdbcDriverLoader.loadClass("com.ibm.as400.access.AS400JDBCConnectionHandle"));
                else 
                    com_ibm_as400_access_AS400JDBCConnectionHandle_class.set(
                        c = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                            @Override
                            public Class<?> run() throws ClassNotFoundException {
                                return mc.mcf.jdbcDriverLoader.loadClass("com.ibm.as400.access.AS400JDBCConnectionHandle");
                            }
                        }));
            }

            if (c.isInstance(mc.sqlConn)) {
                Method m = getServerJobIdentifier.get();
                if (m == null)
                    getServerJobIdentifier.set(m = c.getMethod("getServerJobIdentifier"));
                return (String) m.invoke(mc.sqlConn);
            }
        } catch (NullPointerException npx) {
            // No FFDC code needed.   //162188
            // this means the db2 driver doesn't support the correlator, so will just return null
            Tr.warning(tc, "DSA_GENERIC_MSG", new Object[] { "getServerJobIdentifier()", npx, "method not supported on DB2 driver being used" });
        } catch (NoSuchMethodError nsex) {
            // No FFDC code needed.
            // this means an older toolbos driver is being used, so just log the message and return null.
            // However WAS 5.0x runs back to V5R1 OS400 and this support is in the Toolbox back to V5R1 so this should not happen.
            Tr.warning(tc, "DSA_GENERIC_MSG", new Object[] { "getServerJobIdentifier()", nsex, "method not supported on DB2 driver being used" });
        } catch (IllegalAccessError illegalaccesserror) { // Silber - 
            // It is unfortunate that the JTOpen version and the Toolbox version of the jt400.jar
            // files have defined the AS400JDBCConnectionHandle class in different manners.  
            // It is a protected class in the Toolbox and a public class in the JTOpen version.
            // This catch{} is here to handle the Toolbox exception until they PTF the class to be public
            Tr.warning(tc, "DSA_GENERIC_MSG", new Object[] { "getServerJobIdentifier()", illegalaccesserror,
                                                            ": AS400JDBCConnectionHandle is not a public class in the version of the Toolbox driver being used." });
        } catch (Exception e) { //202080
            // No FFDC code needed.			
            Tr.warning(tc, "DSA_GENERIC_MSG", new Object[] { "getServerJobIdentifier()", e, "method may not be supported on DB2 driver being used" }); //d160887
            // just map the exception, don't rethrow it
            if (e.getCause() instanceof SQLException)
                AdapterUtil.mapSQLException((SQLException) e.getCause(), mc);
        }

        return null;
    }

    @Override
    public boolean isIsolationLevelSwitchingSupport() {// d185974
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) // 274538 Added trace statement   //Defect 736528 
            Tr.debug(this, tc, "isolationSwitchingSupported has a value of " + isolationLevelSwitchingSupport);

        return isolationLevelSwitchingSupport; // d274538  Value set in setProperties()
    }
}
