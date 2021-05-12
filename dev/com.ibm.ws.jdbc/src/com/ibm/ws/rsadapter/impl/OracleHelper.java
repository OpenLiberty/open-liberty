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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;

import javax.resource.ResourceException;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.kernel.service.util.JavaInfo.Vendor;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.impl.WSManagedConnectionFactoryImpl.KerbUsage;
import com.ibm.ws.rsadapter.jdbc.WSJdbcTracer;

/**
 * Helper for the Oracle driver.
 */
public class OracleHelper extends DatabaseHelper {

    private static TraceComponent tc = Tr.register(OracleHelper.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    private static final String
        ORACLELOG_FILE_SIZE_LIMIT = "oracleLogFileSizeLimit",
        ORACLELOG_FILE_COUNT = "oracleLogFileCount",
        ORACLELOG_FILENAME = "oracleLogFileName",
        ORACLELOG_TRACELEVEL = "oracleLogTraceLevel",
        ORACLELOG_FORMAT = "oracleLogFormat",
        ORACLELOG_PACKAGENAME = "oracleLogPackageName";

    private static final int NOT_CACHED = -99;
    private int driverMajorVersion = NOT_CACHED;
    private int driverMinorVersion = NOT_CACHED;

    /**
     * Oracle class names
     */
    private final static String
                    oracle_jdbc_OracleConnection = "oracle.jdbc.OracleConnection",
                    oracle_jdbc_OraclePreparedStatement = "oracle.jdbc.OraclePreparedStatement",
                    oracle_jdbc_driver_OracleLog = "oracle.jdbc.driver.OracleLog";

    /**
     * Oracle classes
     */
    private final AtomicReference<Class<?>> OracleConnection = new AtomicReference<Class<?>>();

    /**
     * Oracle methods
     */
    private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
    private MethodHandle setConnectionProperties,
                         getConnectionProperties;
    private final AtomicReference<Method>
                    clearDefines = new AtomicReference<Method>(),
                    close = new AtomicReference<Method>(),
                    getDefaultExecuteBatch = new AtomicReference<Method>(),
                    getDefaultRowPrefetch = new AtomicReference<Method>(),
                    getDefaultTimeZone = new AtomicReference<Method>(),
                    getIncludeSynonyms = new AtomicReference<Method>(),
                    getRemarksReporting = new AtomicReference<Method>(),
                    getRestrictGetTables = new AtomicReference<Method>(),
                    getSessionTimeZone = new AtomicReference<Method>(),
                    isProxySession = new AtomicReference<Method>(),
                    setDefaultExecuteBatch = new AtomicReference<Method>(),
                    setDefaultRowPrefetch = new AtomicReference<Method>(),
                    setDefaultTimeZone = new AtomicReference<Method>(),
                    setEndToEndMetrics = new AtomicReference<Method>(),
                    setIncludeSynonyms = new AtomicReference<Method>(),
                    setLobPrefetchSize = new AtomicReference<Method>(),
                    setRemarksReporting = new AtomicReference<Method>(),
                    setRestrictGetTables = new AtomicReference<Method>(),
                    setRowPrefetch = new AtomicReference<Method>(),
                    setSessionTimeZone = new AtomicReference<Method>();

    private String matrix[];

    @SuppressWarnings("deprecation")
    private static final TraceComponent oraTc = com.ibm.ejs.ras.Tr.register("com.ibm.ws.oracle.logwriter", "WAS.database", null); 

    /**
     * Construct a helper class for Oracle.
     * 
     * @param mcf managed connection factory
     */
    OracleHelper(WSManagedConnectionFactoryImpl mcf) throws Exception {
        super(mcf);

        dataStoreHelperClassName = "com.ibm.websphere.rsadapter.Oracle11gDataStoreHelper";

        mcf.supportsIsReadOnly = false;
        xaEndResetsAutoCommit = true;

        matrix = new String[4]; // OracleConnection.END_TO_END_STATE_INDEX_MAX

        int limit = 0; // unlimited
        int count = 1; // only one file to rotate through
        String _oracleLogFileName = null;

        Logger _logger = null;
        Formatter _formatter = null;
        Handler _handler = null;

        String _oraclePackageName = "oracle.jdbc.driver";
        String _oracleLogTraceLevel = "INFO";
        String _oracleLogFormat = "SimpleFormat";
        String holder = null;

        Properties props = mcf.dsConfig.get().vendorProps;
        if (props != null) {
            // get the package name:
            holder = props.getProperty(ORACLELOG_PACKAGENAME);
            if (holder != null && !holder.equals(""))
                _oraclePackageName = holder;

            //now getting the file name  
            holder = props.getProperty(ORACLELOG_FILENAME);
            if (holder != null && !holder.equals(""))
                _oracleLogFileName = holder;

            if (oraTc.isDebugEnabled()) {
                Tr.debug(oraTc, "DSConfigHelper.ORACLELOG_PACKAGENAME is: ", _oraclePackageName); 
                Tr.debug(oraTc, "DSConfigHelper.ORACLELOG_FILENAME is:  ", _oracleLogFileName); 
            }

            //now get the logger for the package name specified above
            _logger = Logger.getLogger(_oraclePackageName);

            //if the file name is not set, then we can't set any other values and we have to use 
            // the WAS logging file and settings. 
            if (_oracleLogFileName != null && (!_oracleLogFileName.equals(""))) {

                // now get the trace level
                holder = props.getProperty(ORACLELOG_TRACELEVEL);
                if (holder != null && !holder.equals(""))
                    _oracleLogTraceLevel = holder;

                // now get the log format needed
                holder = props.getProperty(ORACLELOG_FORMAT);
                if (holder != null && !holder.equals(""))
                    _oracleLogFormat = holder;

                //now set the format of the oracle log output                   
                if (_oracleLogFormat != null && (_oracleLogFormat.charAt(0) == 'S' || _oracleLogFormat.charAt(0) == 's')) {
                    if (oraTc.isDebugEnabled())
                        Tr.debug(oraTc, "SimpleFormatter is used");
                    _formatter = new SimpleFormatter();
                } else // default for everything else
                {
                    if (oraTc.isDebugEnabled())
                        Tr.debug(oraTc, "XMLFormatter is used");
                    _formatter = new XMLFormatter();
                }

                // GET limit setting which is how big each file is in bytes before switching (approximate)
                holder = props.getProperty(ORACLELOG_FILE_SIZE_LIMIT);
                if (holder != null && (!holder.equals("")))
                    limit = Integer.parseInt(holder);

                holder = props.getProperty(ORACLELOG_FILE_COUNT);
                // Get file count Setting which is how many files to rotate through
                if (holder != null && (!holder.equals("")))
                    count = Integer.parseInt(holder);

                if (oraTc.isDebugEnabled()) {
                    Tr.debug(oraTc, "DSConfigHelper.ORACLELOG_FILE_COUNT is: " + count); 
                    Tr.debug(oraTc, "DSConfigHelper.ORACLELOG_FILE_SIZE_LIMIT is: " + limit); 
                    Tr.debug(oraTc, "DSConfigHelper.ORACLELOG_FORMAT is: ", _oracleLogFormat); 
                    Tr.debug(oraTc, "DSConfigHelper.ORACLELOG_TRACELEVEL is: ", _oracleLogTraceLevel); 
                }
                // now setting up the file handlers and the logger since at this point we know the user wanted to 
                // to log/trace to a seperate file                              
                try {
                    _handler = new FileHandler(_oracleLogFileName + "%g.%u", limit, count); // g is generation number, u is unique number
                    _handler.setFormatter(_formatter);
                    _handler.setLevel(Level.ALL); // for the handler, i will set it to all so that only the logger 
                                                  // controls the output
                    _logger.setLevel(AdapterUtil.getLevelBasedOnName(_oracleLogTraceLevel));
                    _logger.setUseParentHandlers(false); // this will make sure that we don't send oracle trace to WAS trace file                                           
                    _logger.addHandler(_handler);
                } catch (IOException iox) {
                    // No FFDC needed
                    Tr.warning(tc, "ORACLE_TRACE_WARNING", _oracleLogFileName, iox);
                }
            } else // merge logs with WAS logs since no file is specified
            {
                if (oraTc.isDebugEnabled())
                    Tr.debug(oraTc, "Oracle trace file is not set, Oracle logging/tracing will be mergned with WAS logging based on WAS logging settings");
            }
        }
        
        Collections.addAll(staleConCodes,
                           20,
                           28,
                           1012,
                           1014,
                           1033,
                           1034,
                           1035,
                           1089,
                           1090,
                           1092,
                           3113,
                           3114,
                           12505,
                           12541,
                           12560,
                           12571,
                           17002,
                           17008,
                           17009,
                           17401,
                           17410,
                           17430,
                           17447,
                           24794,
                           25408);
    }
    
    @Override
    public boolean supportsSubjectDoAsForKerberos() {
        return true;
    }

    /**
     * Indicates if setAutoCommit requests should always be sent to the JDBC driver, even
     * if the same as the current value.
     * When a value of false is returned by this method, the IBM WebSphere Relational
     * Resource Adapter code may choose not to send setAutoCommit requests to the JDBC
     * driver when the current value should already be correct.
     * Returning a value of true is useful only as a workaround for an Oracle 9i data
     * integrity bug.
     * 
     * @return false because Oracle 10g does not require the workaround.
     */
    @Override
    public boolean alwaysSetAutoCommit() {
        return false;
    }

    /**
     * Returns the XA start flag for loose or tight branch coupling
     *
     * @param couplingType branch coupling type
     * @return XA start flag value for the specified coupling type
     */
    public int branchCouplingSupported(int couplingType) {
        // TODO remove this check at GA
        if (!mcf.dsConfig.get().enableBranchCouplingExtension)
            return super.branchCouplingSupported(couplingType);

        if (couplingType == ResourceRefInfo.BRANCH_COUPLING_LOOSE)
            if (dataStoreHelper == null)
                return 0x10000; // value of oracle.jdbc.xa.OracleXAResource.ORATRANSLOOSE
            else
                return modifyXAFlag(XAResource.TMNOFLAGS);

        // Tight branch coupling is default for Oracle
        return XAResource.TMNOFLAGS;
    }

    /**
     * This method is called to cache the default set of properties for a Connection.
     * The properties only need to be cached when applications are invoking a
     * set of specific Vendor APIs, which change properties, that must be returned to
     * default values before pooling a connection.
     * 
     * @return Map of properties or Null if not implemented
     * @param java.sql.Connection
     * @throws SQLException
     */
    @Override
    public Map<String, Object> cacheVendorConnectionProps(Connection sqlConn) throws SQLException {
        try {
            Class<?> c = OracleConnection.get();
            if (c == null)
                OracleConnection.set(c = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, oracle_jdbc_OracleConnection));

            Map<String, Object> tempProps = new HashMap<String, Object>();

            if (driverMajorVersion == NOT_CACHED) {
                driverMajorVersion = sqlConn.getMetaData().getDriverMajorVersion();
            }

            // If tempProps is changed then WSRdbManagedConnectionImpl.VENDOR_PROPERTY_SETTERS must be updated
            Method m = getDefaultExecuteBatch.get();
            if (m == null)
                getDefaultExecuteBatch.set(m = c.getMethod("getDefaultExecuteBatch"));
            tempProps.put("DefaultExecuteBatch", m.invoke(sqlConn));

            m = getDefaultRowPrefetch.get();
            if (m == null)
                getDefaultRowPrefetch.set(m = c.getMethod("getDefaultRowPrefetch"));
            tempProps.put("DefaultRowPrefetch", m.invoke(sqlConn));

            if (driverMajorVersion > 10) {
                m = getDefaultTimeZone.get();
                if (m == null)
                    getDefaultTimeZone.set(m = c.getMethod("getDefaultTimeZone"));
                tempProps.put("DefaultTimeZone", m.invoke(sqlConn));
            }

            m = getIncludeSynonyms.get();
            if (m == null)
                getIncludeSynonyms.set(m = c.getMethod("getIncludeSynonyms"));
            tempProps.put("IncludeSynonyms", m.invoke(sqlConn));

            m = getRemarksReporting.get();
            if (m == null)
                getRemarksReporting.set(m = c.getMethod("getRemarksReporting"));
            tempProps.put("RemarksReporting", m.invoke(sqlConn));

            m = getRestrictGetTables.get();
            if (m == null)
                getRestrictGetTables.set(m = c.getMethod("getRestrictGetTables"));
            tempProps.put("RestrictGetTables", m.invoke(sqlConn));

            m = getSessionTimeZone.get();
            if (m == null)
                getSessionTimeZone.set(m = c.getMethod("getSessionTimeZone"));
            tempProps.put("SessionTimeZone", m.invoke(sqlConn));

            return tempProps;
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw AdapterUtil.toSQLException(x);
        }
    }

    /**
     * Closes the proxy session.
     */
    @Override
    public boolean doConnectionCleanup(Connection conn) throws SQLException {
        if (dataStoreHelper != null)
            return doConnectionCleanupLegacy(conn);

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "doConnectionCleanup");
        boolean result = false;
        try {
            Class<?> c = OracleConnection.get();
            if (c == null)
                OracleConnection.set(c = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, oracle_jdbc_OracleConnection));

            if (c.isInstance(conn)) {
                try {
                    Method m = isProxySession.get();
                    if (m == null)
                        isProxySession.set(m = c.getMethod("isProxySession"));

                    if ((Boolean) m.invoke(conn)) {
                        m = close.get();
                        if (m == null)
                            close.set(m = c.getMethod("close", int.class));
                        m.invoke(conn, 1); // value of OracleConnection.PROXY_SESSION
                        result = true;
                    }
                } catch (NoSuchMethodException nsme) {
                    // This is expected when older version of the Oracle JDBC Driver
                    // like classes12.zip are being used
                }
            }
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw AdapterUtil.toSQLException(x);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, "doConnectionCleanup", result);
        return result;
    }

    /**
     * <p>This method is used to reset a connection before it is returned to the connection pool for reuse.
     * This method is called only if at least one of the following Oracle connection setter methods is invoked.</p>
     * <ul>
     * <li>setDefaultExecuteBatch
     * <li>setDefaultRowPrefetch
     * <li>setDefaultTimeZone
     * <li>setIncludeSynonyms
     * <li>setRemarksReporting
     * <li>setRestrictGetTables
     * <li>setSessionTimeZone
     * </ul>
     * 
     * @param sqlConn the connection to attempt to cleanup
     * @param props the default properties to be applied to the connection
     * @return true if properties were successfully applied
     * @throws SQLException If an error occurs while cleaning up the connection.
     */
    public boolean doConnectionVendorPropertyReset(Connection sqlConn, Map<String, Object> props) throws SQLException {
        try {
            Class<?> c = OracleConnection.get();
            if (c == null)
                OracleConnection.set(c = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, oracle_jdbc_OracleConnection));

            Method m;
            m = setDefaultExecuteBatch.get();
            if (m == null)
                setDefaultExecuteBatch.set(m = c.getMethod("setDefaultExecuteBatch", int.class));
            m.invoke(sqlConn, props.get("DefaultExecuteBatch"));

            m = setDefaultRowPrefetch.get();
            if (m == null)
                setDefaultRowPrefetch.set(m = c.getMethod("setDefaultRowPrefetch", int.class));
            m.invoke(sqlConn, props.get("DefaultRowPrefetch"));

            if (driverMajorVersion > 10) {
                m = setDefaultTimeZone.get();
                if (m == null)
                    setDefaultTimeZone.set(m = c.getMethod("setDefaultTimeZone", TimeZone.class));
                m.invoke(sqlConn, props.get("DefaultTimeZone"));
            }

            m = setIncludeSynonyms.get();
            if (m == null)
                setIncludeSynonyms.set(m = c.getMethod("setIncludeSynonyms", boolean.class));
            m.invoke(sqlConn, props.get("IncludeSynonyms"));

            m = setRemarksReporting.get();
            if (m == null)
                setRemarksReporting.set(m = c.getMethod("setRemarksReporting", boolean.class));
            m.invoke(sqlConn, props.get("RemarksReporting"));

            m = setRestrictGetTables.get();
            if (m == null)
                setRestrictGetTables.set(m = c.getMethod("setRestrictGetTables", boolean.class));
            m.invoke(sqlConn, props.get("RestrictGetTables"));

            String s = (String) props.get("SessionTimeZone");
            if (s == null) {
                s = TimeZone.getDefault().getID();
            }
            m = setSessionTimeZone.get();
            if (m == null)
                setSessionTimeZone.set(m = c.getMethod("setSessionTimeZone", String.class));
            m.invoke(sqlConn, s);
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw AdapterUtil.toSQLException(x);
        }

        return true;
    }

    @Override
    public void doStatementCleanup(PreparedStatement stmt) throws SQLException {
        if (dataStoreHelper != null) {
            doStatementCleanupLegacy(stmt);
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "doStatementCleanup");

        /*
         * WSJdbcStatement.VENDOR_PROPERTY_SETTERS
         * must be updated if there is a change in the methods that are cleaned up
         */

        // Oracle doesn't support cursorName, so skip it.

        // In Oracle, the fetchSize getter is faster so eliminate the setter if possible.
        if (stmt.getFetchDirection() != ResultSet.FETCH_FORWARD)
            stmt.setFetchDirection(ResultSet.FETCH_FORWARD);

        stmt.setMaxFieldSize(0);
        stmt.setMaxRows(0);

        Integer queryTimeout = mcf.dsConfig.get().queryTimeout;
        if (queryTimeout == null)
            queryTimeout = defaultQueryTimeout;
        stmt.setQueryTimeout(queryTimeout);

        // Reflectively invoke ((OraclePreparedStatement) stmt).clearDefines();
        try {
            Method m = clearDefines.get();
            if (m == null) {
                Class<?> c = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, oracle_jdbc_OraclePreparedStatement);
                clearDefines.set(m = c.getMethod("clearDefines"));
            }

            m.invoke(stmt);
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw AdapterUtil.toSQLException(x);
        }

        try {
            Method m = getDefaultRowPrefetch.get();
            if (m == null) {
                Class<?> c = OracleConnection.get();
                if (c == null)
                    OracleConnection.set(c = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, oracle_jdbc_OracleConnection));
                getDefaultRowPrefetch.set(m = c.getMethod("getDefaultRowPrefetch"));
            }
            Object defaultRowPrefetch = m.invoke(stmt.getConnection());

            m = setRowPrefetch.get();
            if (m == null) {
                Class<?> c = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, oracle_jdbc_OraclePreparedStatement);
                setRowPrefetch.set(m = c.getMethod("setRowPrefetch", int.class));
            }
            m.invoke(stmt, defaultRowPrefetch);
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw AdapterUtil.toSQLException(x);
        }

        if (driverMajorVersion == NOT_CACHED) {
            DatabaseMetaData metadata = stmt.getConnection().getMetaData();
            driverMajorVersion = metadata.getDriverMajorVersion();
            driverMinorVersion = metadata.getDriverMinorVersion();
        }

        if (driverMajorVersion > 11 || (driverMajorVersion == 11 && driverMinorVersion >= 2)) {
            //11.2.0.1 and greater
            // There is no getDefaultLobPrefetchSize for OracleConnection so
            // we have to use the value that an OracleStatement would be initially
            // set to.
            try {
                Method m = setLobPrefetchSize.get();
                if (m == null) {
                    Class<?> c = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, oracle_jdbc_OraclePreparedStatement);
                    setLobPrefetchSize.set(m = c.getMethod("setLobPrefetchSize", int.class));
                }
                m.invoke(stmt, 4000);
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw AdapterUtil.toSQLException(x);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "doStatementCleanup");
    }

    /**
     * Returns a trace component for supplemental JDBC driver level trace.
     * 
     * @return null to permanently disable supplemental trace for Oracle.
     */
    @Override
    public com.ibm.ejs.ras.TraceComponent getTracer() {
        return null;
    }

    /**
     * This method provides a plug-in point for providing meaningful logging information for an
     * <code>XAException</code>. The information can include details of the original
     * exception that caused the <code>XAException</code>, if applicable.
     * WebSphere uses this method to obtain trace information for
     * <code>XAException</code>s to include in WebSphere trace.
     * 
     * @param xae the <code>XAException</code>.
     * @return detailed information about the <code>XAException</code>, for inclusion in WebSphere trace.
     */
    @Override
    public String getXAExceptionContents(XAException xae) {
        // Use the equivalent method on DataStoreHelper if legacy API is available.
        if (dataStoreHelper != null)
            try {
                return AccessController.doPrivileged((PrivilegedExceptionAction<String>) () -> {
                    return (String) dataStoreHelper.getClass()
                                    .getMethod("getXAExceptionContents", XAException.class)
                                    .invoke(dataStoreHelper, xae);
                });
            } catch (PrivilegedActionException x) {
                FFDCFilter.processException(x, getClass().getName(), "616", this);
            }


        StringBuilder xsb = new StringBuilder(350);
        try {
            Class<?> c = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, "oracle.jdbc.xa.OracleXAException");

            if (c.isInstance(xae)) {
                int xaerror = (Integer) c.getMethod("getXAError").invoke(xae);
                int oraerr = (Integer) c.getMethod("getOracleError").invoke(xae);
                Method getXAErrorMessage = c.getMethod("getXAErrorMessage", int.class);
                String EOLN = AdapterUtil.EOLN;
                xsb.append(EOLN).append("The XA Error is            : ").append(xaerror).append(EOLN);
                xsb.append("The XA Error message is    : ").append(getXAErrorMessage.invoke(null, xaerror)).append(EOLN);
                xsb.append("The Oracle Error code is   : ").append(oraerr).append(EOLN);
                xsb.append("The Oracle Error message is: ").append(getXAErrorMessage.invoke(null, oraerr)).append(EOLN);
            }
        } catch (Exception x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getXAExceptionContents", x);
        }

        if (xae.getCause() != null)   
          xsb.append("The cause is               : ").append(xae.getCause());
        return xsb.toString();
    }

    /**
     * Determine if the top level exception is an authorization exception.
     * Chained exceptions are not checked.
     * 
     * Look for the JDBC 4.0 exception subclass
     * or an Oracle error code in (1004, 1005, 1017)
     * 
     * @param x the exception to check.
     * @return true or false to indicate if the exception is an authorization error.
     * @throws NullPointerException if a NULL exception parameter is supplied.
     */
    boolean isAuthException(SQLException x) {
        return x instanceof SQLInvalidAuthorizationSpecException
               || 1004 == x.getErrorCode() // default username feature not supported; logon denied
               || 1005 == x.getErrorCode() // null password given; logon denied
               || 1017 == x.getErrorCode(); // invalid username/password; logon denied
    }

    @Override
    public boolean shouldTraceBeEnabled(WSManagedConnectionFactoryImpl mcf) {
        // here will base this on the mcf since, the value is enabled for the system
        // as a whole
        if (oraTc.isDebugEnabled() && !mcf.loggingEnabled) {
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
        if (!oraTc.isDebugEnabled() && mc.mcf.loggingEnabled)
            return true;

        return false;
    }

    @Override
    public void disableJdbcLogging(WSRdbManagedConnectionImpl mc) throws ResourceException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "Disabling logging on Oracle10g and later");

        try {
            AccessController.doPrivileged(
                            new PrivilegedAction<Void>()
                            {
                                public Void run()
                                {
                                    setTrace(false);
                                    return null;
                                }
                            });
        } catch (Exception x) {
            FFDCFilter.processException(x, getClass().getName(), "236", this);

            if (tc.isDebugEnabled())
                Tr.debug(oraTc, "enableJdbcLogging failed to enable trace in Oracle, execution will continue", x);
        }
        mc.mcf.loggingEnabled = false;
    }

    @Override
    public void enableJdbcLogging(WSManagedConnectionFactoryImpl mcf) throws ResourceException {
        if (tc.isDebugEnabled())
            Tr.debug(oraTc, "Enabling logging on Oracle10g and later");
        try {
            AccessController.doPrivileged(
                            new PrivilegedAction<Void>()
                            {
                                public Void run()
                                {
                                    setTrace(true);
                                    return null;
                                }
                            });
        } catch (Exception x) {
            FFDCFilter.processException(x, getClass().getName(), "236", this);

            if (tc.isDebugEnabled())
                Tr.debug(oraTc, "enableJdbcLogging failed to enable trace in Oracle, execution will continue", x);
        }
        mcf.loggingEnabled = true;
    }

    @Override
    public void enableJdbcLogging(WSRdbManagedConnectionImpl mc) throws ResourceException {
        this.enableJdbcLogging(mc.mcf);
    } 

    @Override
    public PrintWriter getPrintWriter() throws ResourceException 
    {
        return null;
    }

    /**
     * <p>This method is used to do special handling for readOnly when method setReadOnly is called.
     * If readOnly is true, an SQLException is thrown. If readOnly is false, we ignore it
     * and log an informational message.</p>
     * 
     * @param managedConn WSRdbManagedConnectionImpl object
     * @param readOnly The readOnly value going to be set
     * @param externalCall indicates if caller is WAS or user application,
     */
    @Override
    public void setReadOnly(WSRdbManagedConnectionImpl managedConn, boolean readOnly, boolean externalCall) 
    throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "setReadOnly", managedConn, readOnly, externalCall); 

        if (externalCall) 
        {
            if (readOnly) {
                // Fix this message later
                throw new SQLException(AdapterUtil.getNLSMessage("METHOD_UNSUPPORTED", "setReadOnly", Connection.class.getName()));
            } else {
                // ignore it but log an informational message stating that we won't start a transaction
                Tr.info(tc, "ORA_READONLY"); 

            }
        } else 
        {
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "setReadOnly ignored for internal call"); 
        }
    }

    /**
     *  - allow for special handling of Oracle prepared statement setBytes
     * If byte[] > 2000 bytes, use setBinaryStream
     */
    @Override
    public void psSetBytes(PreparedStatement pstmtImpl, int i, byte[] x) throws SQLException {
        int length = (x == null ? 0 : x.length);
        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "psSetBytes: " + length);

        if ((x != null) && (length > 2000)) {
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "ORACLE setBytes byte array length > 2000 workaround.");
            pstmtImpl.setBinaryStream(i, new ByteArrayInputStream(x), length);
        } else
            pstmtImpl.setBytes(i, x);
    }

    /**
     *  - allow for special handling of Oracle prepared statement setString
     */
    @Override
    public void psSetString(PreparedStatement pstmtImpl, int i, String x) throws SQLException {
        int length = (x == null ? 0 : x.getBytes().length);

        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, "string length: " + length);
        }

        if (length > 4000) {
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "Oracle setString length > 4000 bytes workaround.");
            /*
             * length in setCharacterStream is number of character in
             * stream
             */
            pstmtImpl.setCharacterStream(i, new StringReader(x), x.length());
        } else {
            pstmtImpl.setString(i, x);
        }
    }
    
    private void setKerberosDatasourceProperties(CommonDataSource ds) throws ResourceException {
        try {
            if (setConnectionProperties == null) {
                getConnectionProperties = lookup.findVirtual(ds.getClass(), "getConnectionProperties", MethodType.methodType(Properties.class));
                setConnectionProperties = lookup.findVirtual(ds.getClass(), "setConnectionProperties", MethodType.methodType(void.class, Properties.class));
            }

            Properties connProps = (Properties) getConnectionProperties.invoke(ds);
            if (connProps == null)
                connProps = new Properties();
            
            if (!connProps.containsKey("oracle.net.authentication_services")) {
                connProps.put("oracle.net.authentication_services", "( KERBEROS5 )");
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Automatically setting kerberos connectionProperties for Oracle: ", connProps);

                setConnectionProperties.invoke(ds, connProps);
            }
        } catch (Throwable e) {
            throw new ResourceException(e);
        }
    }
    
    @Override
    public Connection getConnectionFromDatasource(DataSource ds, KerbUsage useKerb, Object gssCredential) throws SQLException {
        
        if (useKerb != KerbUsage.NONE) {
            try {
                checkIBMJava8();
                setKerberosDatasourceProperties(ds);
            } catch (ResourceException ex) {
                throw AdapterUtil.toSQLException(ex);
            }
        }
        
        return super.getConnectionFromDatasource(ds, useKerb, gssCredential);
    }

    @Override
    public ConnectionResults getPooledConnection(final CommonDataSource ds, String userName, String password, final boolean is2Phase, 
                                                 WSConnectionRequestInfoImpl cri, KerbUsage useKerberos, Object gssCredential) throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "getPooledConnection", AdapterUtil.toString(ds), userName, "******", is2Phase ? "two-phase" : "one-phase",
                     cri, useKerberos, gssCredential);
        
        if (useKerberos != KerbUsage.NONE) {
            checkIBMJava8();
            setKerberosDatasourceProperties(ds);
        }
        ConnectionResults results = super.getPooledConnection(ds, userName, password, is2Phase, cri, useKerberos, gssCredential);

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "getPooledConnection", results);
        return results;
    }

    @FFDCIgnore(Exception.class)
    private void checkIBMJava8() throws ResourceException {
        if (JavaInfo.majorVersion() == 8 && JavaInfo.vendor() == Vendor.IBM) {
            // The Oracle JDBC driver prior to 21c does not support kerberos authentication on IBM JDK 8 because
            // it has dependencies to the internal Sun security APIs which don't exist in IBM JDK 8
            boolean ibmJdkSupported;
            try {
                Class<?> OracleDatabaseMetaData = mcf.jdbcDriverLoader.loadClass("oracle.jdbc.OracleDatabaseMetaData");
                int majorVersion = (int) OracleDatabaseMetaData.getMethod("getDriverMajorVersionInfo").invoke(null);
                ibmJdkSupported = majorVersion >= 21;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Oracle major version: " + majorVersion);
            } catch (Exception x) {
                // absence of the Oracle class/methods means a newer version beyond 21c, where the IBM JDK is supported
                ibmJdkSupported = true;
            }

            if (!ibmJdkSupported) {
                Tr.error(tc, "KERBEROS_ORACLE_IBMJDK_NOT_SUPPORTED");
                throw new ResourceException(AdapterUtil.getNLSMessage("KERBEROS_ORACLE_IBMJDK_NOT_SUPPORTED"));
            }
        }
    }

    /**
     * <p>This method is used to get the holdability value of the connection object.
     * If there is an exception thrown, we need to check whether the exception is due to
     * the fact that this getHoldability feature is not supported. If that's the case,
     * 0 is returned.</p>
     * 
     * <p>For most datasources, when the getHoldability feature is not supported, an
     * AbstractMethodError will be thrown.</p>
     * 
     * <p>However, for Oracle datasource, when the getHoldability feature is not supported,
     * an SQLException will be thrown with message "Unsupported feature".<p>
     */
    @Override
    public int getHoldability(Connection conn) throws SQLException {
        int holdability = 0;

        if (tc.isDebugEnabled())
            Tr.debug(this, tc, "getHoldability",
                     AdapterUtil.toString(conn));

        try {
            if (holdabilitySupported) 
            {
                holdability = conn.getHoldability();
                return holdability;
            }
            return 0; // return 0 if holdability is not supported 
        } catch (AbstractMethodError ame) {
            // No FFDC needed
            // This JDBC driver doesn't support Connection.getCursorHoldability().
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "getHoldability",
                         "getHoldability is not supported in this JDBC driver. Encounter a java.lang.AbstractMethodError");
            holdabilitySupported = false; // holdability is not supported 
            return 0;
        } catch (SQLException sqle) {
            // No FFDC needed

            // Check the error code
            if (sqle.getErrorCode() == 17023) {
                // SQLException with error code 17023 is the exception 
                // java.sql.SQLException: Unsupported feature
                if (tc.isDebugEnabled())
                    Tr.debug(this, tc, "getHoldability",
                             "getHoldability is not supported in this JDBC driver. Encounter a java.sql.SQLException: " + sqle.getMessage());
                holdabilitySupported = false; // holdability is not supported    
                return 0;
            } else {
                // Something wrong with the connection.
                throw sqle;
            }
        }
    }

    /*
     * order of array is: 0- clientId
     */
    @Override
    public void setClientInformationArray(String[] clientInfoArray, WSRdbManagedConnectionImpl mc, boolean explicitCall)
                    throws SQLException {
        // set the flag here even if the call fails, safer that way.
        if (explicitCall)
            mc.clientInfoExplicitlySet = true;
        else
            mc.clientInfoImplicitlySet = true;

        // set the clientid in the matrix
        matrix[1 /* OracleConnection.END_TO_END_CLIENTID_INDEX*/] = clientInfoArray[0];
        setEndToEndMetrics(mc.sqlConn, matrix, (short) 0); 

    }

    @Override
    public void resetClientInformation(WSRdbManagedConnectionImpl mc) throws SQLException {
        //cleanup only if the clientInfo trace is not on.
        //Reason is that we want to keep the value set while the connection 
        // is in the pool
        //
        if (mc.clientInfoExplicitlySet || mc.clientInfoImplicitlySet) {
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "resetClientInformation is called on: ", mc);

            matrix[1 /*OracleConnection.END_TO_END_CLIENTID_INDEX*/] = null;
            setEndToEndMetrics(mc.sqlConn, matrix, (short) 0); 
            mc.clientInfoExplicitlySet = false;
            mc.clientInfoImplicitlySet = false;
        }

    }

    public void setEndToEndMetrics(Connection sqlConn, String[] matrix, short s) throws SQLException {
        try {
            Method m = setEndToEndMetrics.get();
            if (m == null) {
                Class<?> c = OracleConnection.get();
                if (c == null)
                    OracleConnection.set(c = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, oracle_jdbc_OracleConnection));
                setEndToEndMetrics.set(m = c.getMethod("setEndToEndMetrics", String[].class, short.class));
            }
            m.invoke(WSJdbcTracer.getImpl(sqlConn), matrix, s);
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw AdapterUtil.toSQLException(x);
        }
    }

    private void setTrace(boolean b) {
        try {
            Class<?> c = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, oracle_jdbc_driver_OracleLog);
            c.getMethod("setTrace", boolean.class).invoke(null, b);
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x instanceof InvocationTargetException ? x.getCause() : x);
        }
    }
}
