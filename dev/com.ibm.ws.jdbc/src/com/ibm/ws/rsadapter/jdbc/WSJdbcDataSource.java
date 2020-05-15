/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.sql.DataSource;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.jca.adapter.WSConnectionManager;
import com.ibm.ws.jdbc.WSDataSource;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.DSConfig;
import com.ibm.ws.rsadapter.impl.WSConnectionRequestInfoImpl;
import com.ibm.ws.rsadapter.impl.WSManagedConnectionFactoryImpl;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.resource.ResourceFactory;

/**
 * This class wraps a JDBC DataSource. It is used as a Connection Factory.
 */
public class WSJdbcDataSource extends WSJdbcWrapper implements DataSource, FFDCSelfIntrospectable, WSDataSource {
    private static final TraceComponent tc =
                    Tr.register(
                                WSJdbcDataSource.class,
                                AdapterUtil.TRACE_GROUP,
                                AdapterUtil.NLS_FILE);

    protected WSConnectionManager cm;
    private ResourceRefInfo resRefInfo;

    /**
     * Cached list of interfaces implemented by the JDBC vendor connection implementation. Null means the list is not initialized yet.
     */
    private final transient AtomicReference<Class<?>[]> vendorConnectionInterfaces = new AtomicReference<Class<?>[]>();

    /**
     * Create a DataSource wrapper. The ManagedConnectionFactory invokes this constructor
     * indirectly via the JDBC##Runtime class in order to create a wrapper for the JDBC spec
     * level that corresponds to the jdbc-#.# feature that is enabled in the server configuration.
     * 
     * @param mcf ManagedConnectionFactory implementation that created this data source.
     * @param connMgr connection manager that manages connections from this data source.
     */
    public WSJdbcDataSource(WSManagedConnectionFactoryImpl mcf, WSConnectionManager connMgr)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "<init>", mcf, connMgr);

        this.mcf = mcf;
        cm = connMgr;
        resRefInfo = cm.getResourceRefInfo(); 
        dsConfig = mcf.dsConfig; 

        if (resRefInfo != null) {
            ComponentMetaDataAccessorImpl cmpMDAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
            ComponentMetaData cmd = cmpMDAccessor.getComponentMetaData();
            J2EEName j2eeName = cmd != null ? cmd.getJ2EEName() : null;
            if (isTraceOn && tc.isDebugEnabled() && j2eeName != null) {
                Tr.debug(this, tc, "the application name is: " + j2eeName.getApplication());
                Tr.debug(this, tc, "the module      name is: " + j2eeName.getModule());
                Tr.debug(this, tc, "the bean        name is: " + j2eeName.getComponent());
                Tr.debug(this, tc, "the res-ref     name is: " + resRefInfo.getName());
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "<init>");
    }

    public final Connection getConnection() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "getConnection");

        // Get the isolation level from the resource reference, or if that is not specified, use the
        // configured isolationLevel value, otherwise use a default that we choose for the database.
        int isolationLevelForCRI = getDefaultIsolationLevel(); 

        final boolean supportIsolvlSwitching = mcf.getHelper().isIsolationLevelSwitchingSupport(); 

        WSConnectionRequestInfoImpl cri = new WSConnectionRequestInfoImpl(null, null, isolationLevelForCRI, mcf.instanceID, supportIsolvlSwitching);
        return getConnection(cri);
    }

    /**
     * This is the common getConnection implementation used by the other getConnection
     * methods. This method handles the connection request to the CM and related exceptions,
     * including the ConnectionWaitTimeoutException. Exceptions thrown by the CM are converted
     * to SQLExceptions.
     * 
     * @param connInfo useful information for requesting a Connection.
     * 
     * @return the Connection
     * 
     * @throws SQLException if an error occurs while obtaining a Connection.
     */
    protected Connection getConnection(ConnectionRequestInfo connInfo) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "getConnection", AdapterUtil.toString(connInfo));

        // Perform wrapper initialization in the common getConnection method.  Check for
        // connection errors on initialization. 
        WSJdbcConnection connWrapper;
        try // get a Connection
        {
            connWrapper = (WSJdbcConnection) cm.allocateConnection(mcf, connInfo);
        }
        // Allow AdapterUtil to convert ConnectionWaitTimeoutExceptions for us. 
        catch (ResourceException resX) {
            FFDCFilter.processException(
                                        resX,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource.getConnection",
                                        "299",
                                        this);
            SQLException x = AdapterUtil.toSQLException(resX);
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "getConnection", "Exception");
            throw x;
        }
        try // to initialize the connection.
        {
            connWrapper.initialize(cm);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource.getConnection",
                                        "280",
                                        this);
            sqlX = WSJdbcUtil.mapException(connWrapper, sqlX);
            if (isTraceOn && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "getConnection", sqlX);
            throw sqlX;
        }

        // create dynamic proxy for vendor interfaces
        Connection c = dsConfig.get().enableConnectionCasting
                        ? connWrapper.getCastableWrapper(vendorConnectionInterfaces)
                        : connWrapper;

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "getConnection", c);
        return c;
    }

    public final Connection getConnection(String user, String pwd)
                    throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "getConnection", user, pwd == null ? null : "******"); 

        // Get the isolation level from the resource reference, or if that is not specified, use the
        // configured isolationLevel value, otherwise use a default that we choose for the database.
        int isolationLevelForCRI = getDefaultIsolationLevel(); 

        WSConnectionRequestInfoImpl _connInf = new WSConnectionRequestInfoImpl(
                        user,
                        pwd,
                        isolationLevelForCRI,
                        mcf.instanceID,
                        mcf.getHelper().isIsolationLevelSwitchingSupport());

        _connInf.markAsChangable(); // this cri can be changed later if needed
        // we are ok with changing the cri here as we haven't called the J2C code with it just yet.
        // special attention needs to be paid to make sure we don't change the cri after it was
        // send to the J2C component. (i.e. after we call allocateConnection on the cm)

        return getConnection(_connInf);
    }

    /**
     * @see com.ibm.ws.jdbc.WSDataSource#getDatabaseProductName()
     */
    public final String getDatabaseProductName() {
        return mcf.getHelper().getDatabaseProductName();
    }

    /**
     * Determine the default isolation level for this data source.
     * 
     * @return the default isolation level for this data source.
     */
    private final int getDefaultIsolationLevel() {
        int defaultIsolationLevel = resRefInfo == null ? Connection.TRANSACTION_NONE : resRefInfo.getIsolationLevel();
        if (defaultIsolationLevel == Connection.TRANSACTION_NONE)
            defaultIsolationLevel = dsConfig.get().isolationLevel;
        if (defaultIsolationLevel == -1)
            defaultIsolationLevel = mcf.getHelper().getDefaultIsolationLevel();
        return defaultIsolationLevel;
    }

    /**
     * @return the underlying JDBC driver's data source implementation object that we wrap.
     */
    protected Object getJDBCImplObject() throws SQLException
    {
        return mcf.getUnderlyingDataSource();
    }

    /**
     * Locate the underlying JDBC driver's implementation of the specified interface.
     * 
     * @param interfaceClass the interface.
     * 
     * @return the underlying JDBC driver's implementation of the specified interface,
     *         or NULL if none is found.
     * @throws SQLException if an error occurs locating or unwrapping the implementation.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getJDBCImplObject(Class<T> interfaceClass) throws SQLException {
        Object jdbcImplObject = getJDBCImplObject();
        if(jdbcImplObject == null) {
            return null;
        }
        Object impl = WSJdbcTracer.getImpl(jdbcImplObject);
        return interfaceClass.isInstance(impl) ? (T) impl : null;
    }

    public final int getLoginTimeout() throws SQLException {
        return mcf.getLoginTimeout();
    }

    public final java.io.PrintWriter getLogWriter() throws SQLException {
        try {
            return mcf.getLogWriter();
        } catch (ResourceException x) {
            throw AdapterUtil.toSQLException(x);
        }
    }

    /**
     * @return the trace component for the JDBC wrapper subclass.
     */
    protected TraceComponent getTracer() {
        return tc;
    }

    /**
     * @return relevant FFDC information for WSJdbcDataSource, formatted as a String array.
     */
    public String[] introspectSelf() {
        com.ibm.ws.rsadapter.FFDCLogger info = new com.ibm.ws.rsadapter.FFDCLogger(this);

        // Introspect on the connection manager. 

        info.append("ResourceRefInfo:", resRefInfo);
        info.introspect("Connection Manager:", cm); 
        info.introspect("ManagedConnectionFactory:", mcf);
        return info.toStringArray();
    }

    /**
     * Invokes a method. Data sources should use the dynamic configuration manager
     * to select the correct configuration, or if the method is a single-parameter
     * setter -- such as setExtProperties(map) -- then create a new configuration entry
     * for it.
     * 
     * @param implObject ignore this parameter; it does not apply to data sources.
     * @param method the method being invoked.
     * @param args the parameters to the method.
     * 
     * @throws IllegalAccessException if the method is inaccessible.
     * @throws IllegalArgumentException if the instance does not have the method or
     *             if the method arguments are not appropriate.
     * @throws InvocationTargetException if the method raises a checked exception.
     * @throws SQLException if unable to invoke the method for other reasons.
     * 
     * @return the result of invoking the method.
     */
    @Override
    Object invokeOperation(Object implObject, Method method, Object[] args)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                    SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "invokeOperation: " + method.getName(), args); 

        Object result;

        // Check for single parameter setter methods
        Class<?>[] types = method.getParameterTypes(); 

        if (types != null && types.length == 1 && method.getName().startsWith("set")
            && void.class.equals(method.getReturnType())) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "invokeOperation: " + method.getName(), "not supported"); 
            throw new SQLFeatureNotSupportedException(method.getName());
        } else // Not modifying the configuration, use the instance for the current config ID.
        {
            implObject = mcf.getUnderlyingDataSource();
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "invoke on " + AdapterUtil.toString(implObject)); 
            result = method.invoke(implObject, args);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "invokeOperation: " + method.getName(), result); 
        return result;
    }

    /**
     * Returns a replacement object that can be serialized instead of WSJdbcDataSource.
     * 
     * @param resRefConfigFactory factory for resource ref config.
     * @return a replacement object that can be serialized instead of WSJdbcDataSource.
     */
    Object replaceObject(ResourceRefConfigFactory resRefConfigFactory) {
        DSConfig config = dsConfig.get();
        String filter = config.jndiName == null || config.jndiName.startsWith("java:")
                        ? FilterUtils.createPropertyFilter("config.displayId", config.id)
                        : FilterUtils.createPropertyFilter(ResourceFactory.JNDI_NAME, config.jndiName);
        ResourceRefConfig resRefConfig = resRefInfo == null ? null : resRefConfigFactory.createResourceRefConfig(DataSource.class.getName());
        if (resRefInfo != null) {
            resRefConfig.setBranchCoupling(resRefInfo.getBranchCoupling());
            resRefConfig.setCommitPriority(resRefInfo.getCommitPriority());
            resRefConfig.setIsolationLevel(resRefInfo.getIsolationLevel());
            resRefConfig.setJNDIName(resRefInfo.getJNDIName());
            resRefConfig.setLoginConfigurationName(resRefInfo.getLoginConfigurationName());
            resRefConfig.setResAuthType(resRefInfo.getAuth());
            resRefConfig.setSharingScope(resRefInfo.getSharingScope());
        }
        return new SerializedDataSourceWrapper(filter, resRefConfig);
    }

    /**
     * @param runtimeX a RuntimeException that occurred, indicating the wrapper might be closed.
     * @return the RuntimeException because data source wrappers do not have a closed state.
     */
    final protected RuntimeException runtimeXIfNotClosed(RuntimeException runtimeX) {
        return runtimeX;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        if (seconds != getLoginTimeout())
            throw new SQLFeatureNotSupportedException();
    }

    public void setLogWriter(final java.io.PrintWriter out) throws SQLException {
        if (!AdapterUtil.match(out, getLogWriter()))
            throw new SQLFeatureNotSupportedException();
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

}