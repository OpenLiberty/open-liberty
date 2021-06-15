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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.resource.ResourceException;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;

import org.ietf.jgss.GSSCredential;

import com.ibm.ejs.cm.logger.TraceWriter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.adapter.WSConnectionManager;
import com.ibm.ws.jca.cm.AbstractConnectionFactoryService;
import com.ibm.ws.jdbc.internal.PropertyService;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.DSConfig;
import com.ibm.ws.rsadapter.impl.WSManagedConnectionFactoryImpl.KerbUsage;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;

/**
 * Helper for the DB2 JCC driver.
 */
public class DB2JCCHelper extends DB2Helper {
    static TraceComponent tc = Tr.register(
                DB2JCCHelper.class,
                AdapterUtil.TRACE_GROUP,
                AdapterUtil.NLS_FILE);

    private static final String
        DB2_TRACE_LEVEL = "traceLevel",
        DB2_TRACE_FILE = "traceFile",
        DB2_TRACE_FILE_DIR = "traceDirectory",
        DB2_TRACE_FILE_APPEND = "traceFileAppend";

    /**
     * DB2 JCC methods
     */
    private Method getDB2PooledConnection,
                    getDB2XAConnection,
                    getSecurityMechanism,
                    setSecurityMechanism,
                    getConnectionWithGSS;
    private final AtomicReference<Method>
                    getDB2Correlator = new AtomicReference<Method>(),
                    isInDB2UnitOfWork = new AtomicReference<Method>(),
                    reuseDB2Connection = new AtomicReference<Method>(),
                    setDB2ClientUser = new AtomicReference<Method>(),
                    setDB2ClientWorkstation = new AtomicReference<Method>(),
                    setDB2ClientApplicationInformation = new AtomicReference<Method>(),
                    setDB2ClientAccountingInformation = new AtomicReference<Method>(),
                    setJCCLogWriter = new AtomicReference<Method>(),
                    setJCCLogWriter2 = new AtomicReference<Method>();

    /**
     * DB2 JCC method signatures
     */
    private static final Class<?>[]
                    TYPES_PrintWriter = new Class<?>[] { PrintWriter.class },
                    TYPES_PrintWriter_int = new Class<?>[] { PrintWriter.class, int.class },
                    TYPES_GSSCredential_Properties = new Class<?>[] { GSSCredential.class, Properties.class },
                    TYPES_String = new Class<?>[] { String.class };

    private int driverType = 0; 

    /**
     * Major version number for the JCC driver.
     * 
     */
    private int jdbcDriverMajorVersion;

    /**
     * Minor version number for the JCC driver.
     * 
     */
    private int jdbcDriverMinorVersion;

    private boolean tightBranchCouplingSupported; 
    private boolean tightBranchCouplingSupportedbyDB; 
    private transient String traceFile; 
    private transient int configuredTraceLevel; 
    private transient Class<DB2JCCHelper> currClass = DB2JCCHelper.class;

    /**
     * Construct a helper class for DB2 Universal JDBC driver.
     *  
     * @param mcf managed connection factory
     */
    DB2JCCHelper(WSManagedConnectionFactoryImpl mcf) throws Exception {
        super(mcf);

        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        dataStoreHelperClassName = "com.ibm.websphere.rsadapter.DB2UniversalDataStoreHelper";

        configuredTraceLevel = 0; // value of DB2BaseDataSource.TRACE_NONE

        isRRSTransaction = false;
        threadIdentitySupport = AbstractConnectionFactoryService.THREAD_IDENTITY_NOT_ALLOWED;
        threadSecurity = false;
        boolean traceAppend = false;
        String traceDir = null;

        // Make sure a valid driverType was specified
        Object dtype = null;
        boolean validDriverTypeSpecified = false;
        Properties props = mcf.dsConfig.get().vendorProps;
        dtype = props.get("driverType");
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "driverType property = " + dtype);
        if (dtype != null) {
            driverType = dtype instanceof Number ? ((Number) dtype).intValue()
                       : dtype instanceof String ? Integer.valueOf((String) dtype)
                       : 0;
            if (driverType == 2 || driverType == 4) {
                validDriverTypeSpecified = true;
            }
        }

        // If a valid driverType was not specified, fail processing
        if (!validDriverTypeSpecified) {
            throw new ResourceException(
                            "Required driverType property was not specifed or is invalid. The driverType property is " + dtype);
        }

        //  we need to check whether the impl class has the  correct configuration for zOS. we can't do
        // this checking in the constructor since we don't have the properties at that time.
        if (localZOS && driverType == 2) {
            String dsClassName = mcf.vendorImplClass.getName();
            if (dsClassName.equals("com.ibm.db2.jcc.DB2XADataSource")) {
                throw new ResourceException(AdapterUtil.getNLSMessage("DB2ZOS_TYPE2_ERROR"));
            } else if (dsClassName.equals("com.ibm.db2.jcc.DB2ConnectionPoolDataSource")) {
                isRRSTransaction = true;
                threadIdentitySupport = AbstractConnectionFactoryService.THREAD_IDENTITY_ALLOWED;
                threadSecurity = true;
                Tr.info(tc, "DB2ZOS_CONFIG_INFO");
            }
        }

        Object holder = props.get(DB2_TRACE_LEVEL);
        if (holder != null && (!holder.equals(""))) {
            configuredTraceLevel = holder instanceof Number ? ((Number) holder).intValue()
                                 : holder instanceof String ? Integer.parseInt((String) holder)
                                 : 0;
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "traceLevel is set to", configuredTraceLevel);
        } else {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "traceLevel is not set, using WAS default: TRACE_CONNECTION_CALLS | TRACE_DRIVER_CONFIGURATION | TRACE_CONNECTS");
        }
        // now get the value of the trace file 
        traceFile = props.getProperty(DB2_TRACE_FILE);

        // ============= now read the value for traceDir
        traceDir = props.getProperty(DB2_TRACE_FILE_DIR);
        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "traceDir is set to ", traceDir);
        }

        if (traceDir != null && !traceDir.equals("")) {
            traceDir = traceDir + File.separator;
        } else {
            traceDir = "";
        }

        //============= now read the value for traceAppend
        holder = props.get(DB2_TRACE_FILE_APPEND);
        traceAppend = holder instanceof Boolean ? (Boolean) holder
                    : holder instanceof String ? Boolean.valueOf((String) holder)
                    : false;

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Trace Append is set to ", holder);
        }

        // construct the printWriter for DB2
        if ((traceFile != null) && (!traceFile.equals(""))) { // traceFile is set
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "DB2 JDBC trace was configured to go to a file, Thus no integration with WAS trace.  File name is: ", traceDir + traceFile);
            try {
                final String file = traceDir + traceFile;
                final boolean append = traceAppend;
                genPw = new PrintWriter(AccessController.doPrivileged(new PrivilegedExceptionAction<FileOutputStream>() {
                    public FileOutputStream run() throws FileNotFoundException {
                        return new FileOutputStream(file, append);
                    }
                }), true);
            } catch (PrivilegedActionException privX) {
                Exception x = privX.getException();
                FFDCFilter.processException(x, getClass().getName(), "343", this);
                if (x instanceof FileNotFoundException)
                    // if i get an io exception, log a warning and use integrated logging instead
                    Tr.error(tc, "DB2_FILE_OUTSTREAM_ERROR", traceFile);
                else if (x instanceof RuntimeException)
                    throw (RuntimeException) x;
                else
                    throw new ResourceException(x);
            }
        } else { // means need to integrate
            genPw = new PrintWriter(new TraceWriter(db2Tc), true);
        }
        
        Collections.addAll(staleConCodes,
                           -4499,
                           -4498,
                           -1776);
    }

    @Override
    public void doStatementCleanup(PreparedStatement stmt) throws SQLException {
        if (dataStoreHelper != null) {
            doStatementCleanupLegacy(stmt);
            return;
        }

        try {
            stmt.setCursorName(null);
        } catch (NullPointerException npe) {
            //Ignore NPE as the statement is being closed anyway.
        }
        stmt.setFetchDirection(ResultSet.FETCH_FORWARD);

        // For DB2, the maxFieldSize getter is faster so avoid the setter if possible.
        if (stmt.getMaxFieldSize() != 0)
            stmt.setMaxFieldSize(0);

        stmt.setMaxRows(0);

        Integer queryTimeout = mcf.dsConfig.get().queryTimeout;
        if (queryTimeout == null)
            queryTimeout = defaultQueryTimeout;
        stmt.setQueryTimeout(queryTimeout);
    }

    /**
     * Determines if the exception indicates failover occurred.
     * 
     * @param sqlX an exception that might indicate connection failover.
     * @return true if the exception indicates failover occurred, otherwise false.
     */
    public boolean failoverOccurred(SQLException sqlX) {
        return sqlX.getErrorCode() == -4498;
    }

    @Override
    public int branchCouplingSupported(int couplingType) 
    {
        if (couplingType == ResourceRefInfo.BRANCH_COUPLING_TIGHT) 
        {
            if (tightBranchCouplingSupported) {
                return 8388608; // value of com.ibm.db2.jcc.DB2XAResource.TMLCS 
            }

            DSConfig config = super.mcf.dsConfig.get();
            if (tightBranchCouplingSupportedbyDB) {
                Tr.warning(tc, "TBC_JCC_NOT_SUPPORTED", config.jndiName == null ? config.id : config.jndiName);
            } else {
                Tr.warning(tc, "TBC_DB_NOT_SUPPORTED", config.jndiName == null ? config.id : config.jndiName);
            }
            // As tight is not supported by the DB/JCC just take the default (loose)
        }

        return javax.transaction.xa.XAResource.TMNOFLAGS;
    }

    /**
     * Returns the default type of branch coupling that should be used for BRANCH_COUPLING_UNSET.
     * 
     * @return BRANCH_COUPLING_LOOSE
     */
    public int getDefaultBranchCoupling() {
        return ResourceRefInfo.BRANCH_COUPLING_LOOSE;
    }

    @Override
    public String getCorrelator(WSRdbManagedConnectionImpl mc) throws SQLException 
    {

        try {
            return (String) invokeOnDB2Connection(mc.sqlConn, getDB2Correlator, "getDB2Correlator", null);
        } catch (NullPointerException npx) {
            // No FFDC code needed.   
            // this means the db2 driver doesn't support the correlator, so will just return null
            Tr.warning(tc, "DSA_GENERIC_MSG", "getDB2Correlator()", npx, "method not supported on DB2 driver being used");
            return null;
        } catch (NoSuchMethodError nsex) {
            // No FFDC code needed.
            // this means an older jcc driver is being used, so just log the message and return null
            Tr.warning(tc, "DSA_GENERIC_MSG", "getDB2Correlator()", nsex, "method not supported on DB2 driver being used");
            return null;

        } catch (SQLException sqlex)
        {
            // No FFDC code needed.
            // Since this is a trace function, we will not halt execution of problem,
            // instead, will only map the exception and log a warning
            AdapterUtil.mapSQLException(sqlex, mc); 
            Tr.warning(tc, "DSA_GENERIC_MSG", "getDB2Correlator()", sqlex, "method not supported on DB2 driver being used");

            return null;
        }

    }
    
    /**
     * Utility method that returns the result of
     * ((DB2Connection) con).methName(params)
     * 
     * @param con connection to the database
     * @param methRef reference to a method
     * @param methName name of the method
     * @param paramTypes method parameter types
     * @param params method parameters
     * @return the result
     * @throws SQLException if an error occurs
     */
    private final Object invokeOnDB2Connection(Connection con, AtomicReference<Method> methRef,
                                               String methName, Class<?>[] paramTypes, Object... params) throws SQLException {
        try {
            Method m = methRef.get();
            if (m == null) {
                Class<?> c = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, "com.ibm.db2.jcc.DB2Connection");
                methRef.set(m = c.getMethod(methName, paramTypes));
            }

            return m.invoke(con, params);
        } catch (RuntimeException x) {
            throw x;
        } catch (NoSuchMethodException x) {
            throw (Error) new NoSuchMethodError(methName).initCause(x);
        } catch(InvocationTargetException e){
            throw AdapterUtil.toSQLException(e.getCause());
        } catch (Exception x) {
            throw AdapterUtil.toSQLException(x);
        }
    }

    @Override
    public boolean shouldTraceBeEnabled(WSRdbManagedConnectionImpl mc) {
        // here will base this on the mc since the value is enabled per connection

        if (TraceComponent.isAnyTracingEnabled() && db2Tc.isDebugEnabled() && !mc.loggingEnabled) 
            return true;

        return false;
    }

    @Override
    public boolean shouldTraceBeDisabled(WSRdbManagedConnectionImpl mc) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if ((!isTraceOn || !db2Tc.isDebugEnabled()) && mc.loggingEnabled) 
            return true;

        return false;
    }

    @Override
    public void disableJdbcLogging(WSRdbManagedConnectionImpl mc) throws ResourceException {
        if (TraceComponent.isAnyTracingEnabled() && db2Tc.isDebugEnabled())
            Tr.debug(db2Tc, "Disabling logging on connection: ", mc.sqlConn);

        try {
            invokeOnDB2Connection(mc.sqlConn, setJCCLogWriter, "setJCCLogWriter", TYPES_PrintWriter, new Object[] { null });
        } catch (SQLException e) {
            FFDCFilter.processException(
                                        e, getClass().getName(), "402", this);
            throw AdapterUtil.translateSQLException(e, mc, true, currClass); 
        }
        mc.loggingEnabled = false;
    }

    @Override
    public void enableJdbcLogging(WSRdbManagedConnectionImpl mc) throws ResourceException {
        // note that we don't check if debug is enabled, since we we get here, we knwo debug is enabled
        PrintWriter pw = getPrintWriter(); 
        if (TraceComponent.isAnyTracingEnabled() && db2Tc.isDebugEnabled())
            Tr.debug(this, db2Tc, "Setting printWriter on connection and with level", pw, mc.sqlConn, configuredTraceLevel);
        try {
            invokeOnDB2Connection(mc.sqlConn, setJCCLogWriter2, "setJCCLogWriter", TYPES_PrintWriter_int, pw, configuredTraceLevel);
        } catch (SQLException e) {
            FFDCFilter.processException(e, getClass().getName(), "419", this);
            throw AdapterUtil.translateSQLException(e, mc, true, currClass); 
        }
        mc.loggingEnabled = true;
    }

    @Override
    public PrintWriter getPrintWriter() throws ResourceException 
    {
        //not synchronizing here since there will be one helper
        // and most likely the setting will be serially, even if its not,
        // it shouldn't matter here (tracing).
        if (genPw == null) {
            genPw = new PrintWriter(new TraceWriter(db2Tc), true);
        }
        if (db2Tc.isDebugEnabled())
            Tr.debug(db2Tc, "returning", genPw);
        return genPw;
    }

    /*
     * order of array is: 0- clientId, 1- workstationid, 2- applicationname, 3- accounInfo
     */
    @Override
    public void setClientInformationArray(String[] clientInfoArray, WSRdbManagedConnectionImpl mc, boolean explicitCall) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "setClientInformationArray", clientInfoArray, mc, explicitCall);

        //Note that the clientInfoArray will never be null

        // set the flag here even if the call fails, safer that way.
        if (explicitCall)
            mc.clientInfoExplicitlySet = true;
        else
            mc.clientInfoImplicitlySet = true;

        try {
            invokeOnDB2Connection(mc.sqlConn, setDB2ClientUser, "setDB2ClientUser", TYPES_String, clientInfoArray[0]);
            invokeOnDB2Connection(mc.sqlConn, setDB2ClientWorkstation, "setDB2ClientWorkstation", TYPES_String, clientInfoArray[1]);
            invokeOnDB2Connection(mc.sqlConn, setDB2ClientApplicationInformation, "setDB2ClientApplicationInformation", TYPES_String, clientInfoArray[2]);
            invokeOnDB2Connection(mc.sqlConn, setDB2ClientAccountingInformation, "setDB2ClientAccountingInformation", TYPES_String, clientInfoArray[3]);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, getClass().getName(), "611", this);
            if (isTraceOn && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "setClientInformationArray - Exception", ex);
            throw AdapterUtil.mapSQLException(ex, mc); 
        }
    }

    @Override
    public void resetClientInformation(WSRdbManagedConnectionImpl mc) throws SQLException {

        // note that i am resetting here, however, the values won't be reset til its out of hte pool and
        // after an execute is done on the connection, which is good since we will be
        // able to see the value while its in the pool to see the last time it was used

        if (mc.clientInfoExplicitlySet || mc.clientInfoImplicitlySet) {
            final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

            if (isTraceOn && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "resetClientInformation is called on: ", mc);

            try {
                Properties defaultClientInfo = mc.mcf.defaultClientInfo;
                String clientuser = defaultClientInfo.getProperty("ClientUser");
                String clienthostname = defaultClientInfo.getProperty("ClientHostname");
                String applicationname = defaultClientInfo.getProperty("ApplicationName");
                String clientacccountinginformation = defaultClientInfo.getProperty("ClientAccountingInformation");

                invokeOnDB2Connection(mc.sqlConn, setDB2ClientUser, "setDB2ClientUser", TYPES_String, clientuser == null ? "" : clientuser);
                invokeOnDB2Connection(mc.sqlConn, setDB2ClientWorkstation, "setDB2ClientWorkstation", TYPES_String, clienthostname == null ? "" : clienthostname);
                invokeOnDB2Connection(mc.sqlConn, setDB2ClientApplicationInformation, "setDB2ClientApplicationInformation", TYPES_String, applicationname == null ? "" : applicationname);
                invokeOnDB2Connection(mc.sqlConn, setDB2ClientAccountingInformation, "setDB2ClientAccountingInformation", TYPES_String, clientacccountinginformation == null ? "" : clientacccountinginformation);

                mc.clientInfoExplicitlySet = false;
                mc.clientInfoImplicitlySet = false;
            } catch (SQLException ex) {
                FFDCFilter.processException(ex, getClass().getName(), "677", this);
                if (isTraceOn && tc.isDebugEnabled()) 
                    Tr.debug(this, tc, "resetClientInformation -- Exception", ex); 
                throw AdapterUtil.mapSQLException(ex, mc); 
            }
        }
    }

    /**
     * this method returns the indicator whether the backend supports the isolation level switching on
     * a connection
     * 
     * @return boolean : indicates the backend whether supports the isolation level switching
     **/
    @Override
    public boolean isIsolationLevelSwitchingSupport() {
        return true;
    }

    /**
     * This method returns a sqljConnectContext. It will go to DB2 to get the connection Context.
     * We need to create a new WSJccConnection to get the phsyical sqlj context.So the sqlj runtime
     * will use our WSJccConnection to do the work.
     * 
     * @param a managedConnection
     * @param DefaultContext the sqlj.runtime.ref.DefaultContext class
     * @return a physical sqlj connectionContext -
     * @exception a SQLException if can't get a DefaultContext
     **/
    @Override
    public Object getSQLJContext(WSRdbManagedConnectionImpl mc, Class<?> DefaultContext, WSConnectionManager connMgr) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "getSQLJContext");
        Object rtnctx = null; 
        try {
            if (mc.cachedConnection == null) {
                mc.cachedConnection = mcf.jdbcRuntime.newConnection(mc, mc.sqlConn, WSRdbManagedConnectionImpl.key, mc.threadID);
                mc.cachedConnection.initialize(connMgr, WSRdbManagedConnectionImpl.key);
                mc.cachedConnection.setCurrentAutoCommit(mc.currentAutoCommit, WSRdbManagedConnectionImpl.key);
            } else { 
                /*
                 * Need to set the mc's threadid to prevent false multi thread detection.
                 */
                mc.cachedConnection.setThreadID(mc.threadID, WSRdbManagedConnectionImpl.key); 
            }
        } catch (ResourceException rex) {
            FFDCFilter.processException(rex, getClass().getName(), "550", this);
            SQLException sqlX = AdapterUtil.toSQLException(rex);
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "getSQLJContext", rex);
            throw sqlX; 
        } catch (SQLException sqlx) {
            FFDCFilter.processException(sqlx, getClass().getName(), "1009", this);
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "getSQLJContext", sqlx);
            throw WSJdbcUtil.mapException(mc.cachedConnection, sqlx); 
        }

        try {
            rtnctx = DefaultContext.getConstructor(Connection.class).newInstance(mc.cachedConnection);
        } catch (Exception x) {
            FFDCFilter.processException(x, getClass().getName(), "549", this);
            if (x instanceof InvocationTargetException && x.getCause() instanceof SQLException) {
                SQLException sqlX = WSJdbcUtil.mapException(mc.cachedConnection, (SQLException) x.getCause());
                if (isTraceOn && tc.isEntryEnabled()) 
                    Tr.exit(this, tc, "getSQLJContext", sqlX);
                throw sqlX;
            } else {
                RuntimeException rx = x instanceof RuntimeException ? (RuntimeException) x : new RuntimeException(x);
                if (isTraceOn && tc.isEntryEnabled()) 
                    Tr.exit(this, tc, "getSQLJContext", x);
                throw rx;
            }
        }
        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "getSQLJContext", rtnctx);
        return rtnctx;
    } 

    @Override
    public void gatherAndDisplayMetaDataInfo(Connection conn, WSManagedConnectionFactoryImpl mcf) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        super.gatherAndDisplayMetaDataInfo(conn, mcf);

        try {
            DatabaseMetaData mdata = conn.getMetaData();
            jdbcDriverMajorVersion = mdata.getDriverMajorVersion();
            jdbcDriverMinorVersion = mdata.getDriverMinorVersion();

            //  check DB/JCC for tight branch coupling support
            if (driverType == 4) 
            {
                final String productVersion = mdata.getDatabaseProductVersion().toUpperCase(); 
                if (productVersion.startsWith("DSN") || productVersion.startsWith("SQL")) 
                {
                    int version = Integer.parseInt(productVersion.substring(3, 5)); 
                    tightBranchCouplingSupportedbyDB = (version > 7); 
                }
                if (jdbcDriverMajorVersion > 4 
                    || jdbcDriverMajorVersion == 4 && jdbcDriverMinorVersion >= 1
                    || jdbcDriverMajorVersion == 3 &&
                    (jdbcDriverMinorVersion >= 51 || (jdbcDriverMinorVersion >= 6 && jdbcDriverMinorVersion < 50))) {
                    tightBranchCouplingSupported = tightBranchCouplingSupportedbyDB; 
                }
            }
        } catch (Throwable x) {
            FFDCFilter.processException(x,
                                        getClass().getName() + ".gatherAndDisplayMetaDataInfo", "1633", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc,
                         "Unable to determine JDBC driver major/minor version.", x);
        }

        // now see if supportsUOWDetection works for this driver.
        try {
            isInDatabaseUnitOfWork(conn);
            mcf.supportsUOWDetection = true;
        } catch (Throwable t) {
            //NO FFDC needed
            if (isTraceOn && tc.isDebugEnabled()) 
            {
                Tr.debug(this, tc, "JCC Driver version does not support isInDB2UnitOfWork method");
            }
        }
        if (isTraceOn && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "JCC Driver version supports isInDB2UnitOfWork method");

    }

    @Override
    public boolean isInDatabaseUnitOfWork(Connection conn) throws SQLException {
        boolean flag = (Boolean) invokeOnDB2Connection(conn, isInDB2UnitOfWork, "isInDB2UnitOfWork", null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "isInDatabaseUnitOfWork", flag);

        return flag;
    }
    
    @Override
    public Connection getConnectionFromDatasource(DataSource ds, KerbUsage useKerb, Object gssCredential) throws SQLException {
        if (useKerb != KerbUsage.USE_CREDENTIAL) {
            return super.getConnectionFromDatasource(ds, useKerb, gssCredential);
        }
        
        try {
            configureForKerberos(ds);
            
            if (getConnectionWithGSS == null) {
                getConnectionWithGSS = ds.getClass().getMethod("getConnection", Object.class);
            }
            return (Connection) getConnectionWithGSS.invoke(ds, gssCredential);
        } catch (Throwable t) {
            throw AdapterUtil.toSQLException(t);
        }
    }
    
    private void configureForKerberos(CommonDataSource ds) throws Throwable {
        if (setSecurityMechanism == null) {
            getSecurityMechanism = ds.getClass().getMethod("getSecurityMechanism");
            setSecurityMechanism = ds.getClass().getMethod("setSecurityMechanism", short.class);
        }
        
        // Before getting the connection, ensure securityMechanism is set to 11 (kerberos)
        short secMec = (short) getSecurityMechanism.invoke(ds);
        if (secMec == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Overriding existing securityMechanism of " + secMec + " to 11 (kerberos)");
            setSecurityMechanism.invoke(ds, (short) 11);
        }
    }
    
    private PooledConnection getPooledConnectionUsingKerberos(CommonDataSource ds, Object gssCredential, boolean is2Phase) throws ResourceException {
        try {
            configureForKerberos(ds);
            
            if (is2Phase) {
                if (getDB2XAConnection == null) {
                    getDB2XAConnection = ds.getClass().getMethod("getDB2XAConnection", GSSCredential.class, Properties.class);
                }
                // Method returns DB2XAConnection which also extends PooledConnection
                return (PooledConnection) getDB2XAConnection.invoke(ds, gssCredential, null);
            } else {
                if (getDB2PooledConnection == null) {
                    getDB2PooledConnection = ds.getClass().getMethod("getDB2PooledConnection", GSSCredential.class, Properties.class);
                }
                return (PooledConnection) getDB2PooledConnection.invoke(ds, gssCredential, null);
            }
        } catch (Throwable e) {
            throw new ResourceException(e);
        }
    }

    @Override
    public ConnectionResults getPooledConnection(final CommonDataSource ds, String userName, String password, final boolean is2Phase, 
                                                 WSConnectionRequestInfoImpl cri, final KerbUsage useKerberos, final Object gssCredential) throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "getPooledConnection", AdapterUtil.toString(ds), userName, "******", is2Phase ? "two-phase" : "one-phase",
                     cri, useKerberos, gssCredential);

        ConnectionResults results;
        if (useKerberos == KerbUsage.USE_CREDENTIAL) {
            return new ConnectionResults(
                                         getPooledConnectionUsingKerberos(ds, gssCredential, is2Phase),
                                         null);
        }
        else
            results = super.getPooledConnection(ds, userName, password, is2Phase, cri, useKerberos, gssCredential);

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "getPooledConnection", results);
        return results;
    }

    public void reuseKerbrosConnection(Connection sqlConn, GSSCredential gssCred, Properties props) throws SQLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "reuseKerbrosConnection", sqlConn, gssCred, PropertyService.hidePasswords(props));
        invokeOnDB2Connection(sqlConn, reuseDB2Connection, "reuseDB2Connection", TYPES_GSSCredential_Properties, gssCred, props);
    }
}