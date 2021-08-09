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
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.rsadapter.AdapterUtil;

import java.util.StringTokenizer; 
import java.util.concurrent.atomic.AtomicReference;
import java.util.Collections;
import java.util.Properties; 

/**
 * Helper for the iSeries Native DB2 driver.
 */
public class DB2iNativeHelper extends DB2Helper {
    private static TraceComponent tc = Tr.register(DB2iNativeHelper.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * DB2 i native classes
     */
    private final AtomicReference<Class<?>>
                    com_ibm_db2_jdbc_app_DB2ConnectionHandle_class = new AtomicReference<Class<?>>(),
                    com_ibm_db2_jdbc_app_UDBConnectionHandle_class = new AtomicReference<Class<?>>();

    /**
     * DB2 i native methods
     */
    private final AtomicReference<Method>
                    DB2ConnectionHandle_getServerJobName = new AtomicReference<Method>(),
                    UDBConnectionHandle_getServerJobName = new AtomicReference<Method>();

    boolean isolationLevelSwitchingSupport = false; 
    boolean switchingSupportDetermined = false; 
    private String os400Version_; 
    private int os400VersionNum_; 

    /**
     * Construct a helper class for DB2 iSeries Native JDBC driver.
     * 
     * @param mcf managed connection factory
     */
    DB2iNativeHelper(WSManagedConnectionFactoryImpl mcf) throws Exception {
        super(mcf);

        dataStoreHelperClassName = "com.ibm.websphere.rsadapter.DB2AS400DataStoreHelper";

        // For the Native driver (unlike the Toolbox driver) the custom property isolationLevelSwitchingSupport is
        // optional and really is only needed if the target database is a remote one.  If this property is not set
        // local DB2 access is assumed and the os.version is checked to determine if isolation level switching is 
        // supported at the local DB2 level. However, if the property is set it takes precedence over the os.version level.
        Properties props = mcf.dsConfig.get().vendorProps;
        String switchingSupported = (String) props.get("isolationLevelSwitchingSupport");
        if (switchingSupported != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "isolationSwitchingSupported property = " + switchingSupported);

            isolationLevelSwitchingSupport = Boolean.valueOf(switchingSupported);

            switchingSupportDetermined = true;
        }
        
        // --- The Native driver will return this CLI SQLState (HY017) whenever a connection is no longer available.
        //     This covers the case when an underlying iSeries QSQSRVR prestart job on the iSeries that represents
        //     the jdbc connection in the WAS connection pool is killed and the connection is later used.
        // --- The Toolbox driver will return the SQLState "8S01" when a connection is no longer available
        //
        // **** StaleConnectionException ******
        //     SQLCode   SQLState  Toolbox  Native       DatabaseHelper         DB2Helper        DB2iNativeHelper
        //     SQ99999   HY017                X                                                       SQLState
        //     SQL0900   08003        X       X           SQLState
        //     SQL0901   58004        X       X                                 SQLState
        //     SQL0906   24514        X       X                                 SQLCode
        //     SQL0858   08S01        X                   SQLState
        //     SQ30080   08001        X       X           SQLState              SQLCode
        // **** DuplicateKeyException *****
        //     SQLCode   SQLState  Toolbox  Native       DatabaseHelper         DB2Helper        DB2iNativeHelper
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
    public String getCorrelator(final WSRdbManagedConnectionImpl mc) throws SQLException { 
        try {
            Class<?> c = com_ibm_db2_jdbc_app_DB2ConnectionHandle_class.get();
            if (c == null) { 
                if(System.getSecurityManager() == null)
                    com_ibm_db2_jdbc_app_DB2ConnectionHandle_class.set(
                        c = mc.mcf.jdbcDriverLoader.loadClass("com.ibm.db2.jdbc.app.DB2ConnectionHandle"));
                else 
                    com_ibm_db2_jdbc_app_DB2ConnectionHandle_class.set( 
                        c = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                            @Override
                            public Class<?> run() throws ClassNotFoundException {
                                return mc.mcf.jdbcDriverLoader.loadClass("com.ibm.db2.jdbc.app.DB2ConnectionHandle");
                            }
                        }));
            }

            if (c.isInstance(mc.sqlConn)) {
                Method m = DB2ConnectionHandle_getServerJobName.get();
                if (m == null)
                    DB2ConnectionHandle_getServerJobName.set(m = c.getMethod("getServerJobName"));
                return (String) m.invoke(mc.sqlConn);
            }

            c = com_ibm_db2_jdbc_app_UDBConnectionHandle_class.get();
            if (c == null) { 
                if(System.getSecurityManager() == null)
                    com_ibm_db2_jdbc_app_UDBConnectionHandle_class.set(
                        c = mc.mcf.jdbcDriverLoader.loadClass("com.ibm.db2.jdbc.app.UDBConnectionHandle"));               
                else 
                    com_ibm_db2_jdbc_app_DB2ConnectionHandle_class.set( 
                        c = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                            @Override
                            public Class<?> run() throws ClassNotFoundException {
                                return mc.mcf.jdbcDriverLoader.loadClass("com.ibm.db2.jdbc.app.UDBConnectionHandle");
                            }
                        }));
            }

            if (c.isInstance(mc.sqlConn)) {
                Method m = UDBConnectionHandle_getServerJobName.get();
                if (m == null)
                    UDBConnectionHandle_getServerJobName.set(m = c.getMethod("getServerJobName"));
                return (String) m.invoke(mc.sqlConn);
            }
        } catch (NullPointerException npx) {
            // No FFDC code needed.   
            // this means the iSeries native db2 driver doesn't support the correlator, so will just return null
            Tr.warning(tc, "DSA_GENERIC_MSG", new Object[] { "getServerJobName()", npx, "method not supported on DB2 driver being used" }); 
        } catch (NoSuchMethodError nsex) {
            // No FFDC code needed.
            // this means an older native driver is being used, so just log the message and return null
            Tr.warning(tc, "DSA_GENERIC_MSG", new Object[] { "getServerJobName()", nsex, "method not supported on DB2 driver being used" }); 
        } catch (Exception e) {
            // No FFDC code needed.                     
            Tr.warning(tc, "DSA_GENERIC_MSG", new Object[] { "getServerJobName()", e, "method may not be supported on DB2 driver being used" }); 
            // just map the exception, don't rethrow it
            if (e.getCause() instanceof SQLException)
                AdapterUtil.mapSQLException((SQLException) e.getCause(), mc);
        }
        return null;
    }

    /**
     * 274538 -- internal utility method added.
     * This internal utility method is used to convert the os.version string to a integer value
     * so that DB2 support can be determined based on version and release level.
     * 
     * @return int : The numeric value of the os400 VRM such as 530 for the string "V5R3M0"
     * 
     **/
    protected static final int generateVersionNumber(String s) {
        int i = -1;
        StringTokenizer stringtokenizer = new StringTokenizer(s, "VRM", false);
        if (stringtokenizer.countTokens() == 3) {
            String s1 = stringtokenizer.nextToken();
            s1 = s1 + stringtokenizer.nextToken();
            s1 = s1 + stringtokenizer.nextToken();
            i = Integer.parseInt(s1);
        }
        return i;
    }

    /**
     * This method returns the indicator whether the backend supports the isolation level switching on a connection
     * 
     * @return boolean : indicates the backend whether supports the isolation level switching
     **/
    @Override
    public boolean isIsolationLevelSwitchingSupport() {
        // We assume that we are running on the local server so we can determine the os version
        // Check that the OS is V5R3 or higher and set isolationLevelSwitchingSupported to true.
        // Note:  isolationLevelSwitchingSupport is defaulted to false
        if (!switchingSupportDetermined) {
            os400Version_ = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty("os.version");
                }
            });
            os400VersionNum_ = generateVersionNumber(os400Version_);
            if (os400VersionNum_ >= 530) {
                isolationLevelSwitchingSupport = true;
            }
            switchingSupportDetermined = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())   
            Tr.debug(this, tc, "isolationSwitchingSupported has a value of " + isolationLevelSwitchingSupport);
        // The variable 'isolationLevelSwitchingSupport' is initialized to false.  
        // If it was changed, it was done so in one of two places:
        // 1) In the code in this method - ie. switchingSupportDetermined was false and os.version dictates its value.
        // 2) In the setProperties() method of this class. A datasource property (isolationLevelSwitchingSupport) was set 
        //    to true, and that takes precedence over the os.version WAS is running on (remote DB access is being done) 
        //    and switchingSupportDetermined is then set to true.
        //
        return isolationLevelSwitchingSupport; 
    }
}
