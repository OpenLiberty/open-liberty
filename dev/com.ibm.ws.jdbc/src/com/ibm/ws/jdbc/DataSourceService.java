/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc;

import java.io.Serializable;
import java.security.AccessController;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Observable;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.TransactionSupport.TransactionSupportLevel;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.cm.AbstractConnectionFactoryService;
import com.ibm.ws.jca.cm.AppDefinedResource;
import com.ibm.ws.jca.cm.ConnectionManagerService;
import com.ibm.ws.jca.cm.ConnectorService;
import com.ibm.ws.jdbc.internal.DataSourceDef;
import com.ibm.ws.jdbc.internal.JDBCDriverService;
import com.ibm.ws.jdbc.internal.PropertyService;
import com.ibm.ws.jdbc.osgi.JDBCRuntimeVersion;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.DSConfig;
import com.ibm.ws.rsadapter.SQLStateAndCode;
import com.ibm.ws.rsadapter.impl.DatabaseHelper;
import com.ibm.ws.rsadapter.impl.WSManagedConnectionFactoryImpl;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.kernel.service.utils.PathUtils;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.resource.ResourceFactory;

/**
 * Declarative services component for the <dataSource> element.
 */
public class DataSourceService extends AbstractConnectionFactoryService implements AppDefinedResource, ResourceFactory, ApplicationRecycleComponent {
    private static final TraceComponent tc = Tr.register(DataSourceService.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    /**
     * Name of reference to the ConnectionManagerService
     */
    private static final String CONNECTION_MANAGER = "connectionManager";

    /**
     * Name of element used for data source configuration.
     */
    public static final String DATASOURCE = "dataSource";

    /**
     * Factory persistent identifier for DataSourceService.
     */
    public final static String FACTORY_PID = "com.ibm.ws.jdbc.dataSource";

    /**
     * Name of reference to the JDBCDriverService
     */
    private static final String JDBC_DRIVER = "driver";

    /**
     * Name of property added by config service to element that has nested config. We need to ignore this.
     */
    static final String SUPPORT_EXTENSIONS = "supportExtensions";

    /**
     * Name of internal property that specifies the target connectionManager service
     */
    static final String TARGET_CONNECTION_MANAGER = "connectionManager.target";

    /**
     * Name of internal property that specifies the target container auth data service
     */
    static final String TARGET_CONTAINER_AUTH_DATA = "containerAuthData.target";

    /**
     * Name of internal property that specifies the target jdbcDriver service
     */
    static final String TARGET_JDBC_DRIVER = "driver.target";

    /**
     * Name of internal property that specifies the target recovery auth data service
     */
    static final String TARGET_RECOVERY_AUTH_DATA = "recoveryAuthData.target";

    /**
     * Name of internal property that enforces unique JNDI names.
     */
    static final String UNIQUE_JNDI_NAME = "jndiName.unique";

    /**
     * Name of property that identifies the application for java:global data sources.
     */
    static final String DECLARING_APPLICATION = "declaringApplication";

    /**
     * Properties to skip when parsing configuration.
     */
    private static final HashSet<String> WPROPS_TO_SKIP = new HashSet<String>(Arrays.asList
                    (APPLICATION, MODULE, COMPONENT, DSConfig.CONNECTION_MANAGER_REF, DSConfig.CONTAINER_AUTH_DATA_REF, ID, DSConfig.JDBC_DRIVER_REF,
                     DSConfig.RECOVERY_AUTH_DATA_REF, SUPPORT_EXTENSIONS));

    /**
     * Component context.
     */
    private ComponentContext componentContext;

    /**
     * Utility that collects various core services needed by connection management and JDBC
     */
    private ConnectorService connectorSvc;

    /**
     * Data source configuration.
     */
    private final AtomicReference<DSConfig> dsConfigRef = new AtomicReference<DSConfig>();

    /**
     * Data source or driver implementation class name. Only rely on this value when initialized.
     */
    private String vendorImplClassName;

    /**
     * Indicates that we deferred the destroying that would normally happen because of configuration changes.
     * This happens, for example, when vendor properties are updated, if it isn't Derby Embedded,
     * which needs the immediate destroy to free up the database for other class loaders.
     */
    private boolean destroyWasDeferred;

    /**
     * Derby Embedded only - List of Derby Embedded databaseNames used by dataSources.
     * A databaseName can appear multiple times, which serves as a reference count.
     * When the reference count reaches 0, the database can be shut down.
     */
    private static ConcurrentLinkedQueue<String> embDerbyRefCount = new ConcurrentLinkedQueue<String>();

    /**
     * Unique identifier for this data source configuration, which is suitable for display in messages.
     */
    private String id;

    /**
     * Indicates if the JDBC driver is Derby Embedded.
     */
    private boolean isDerbyEmbedded;
    
    /**
     * Indicates if the JDBC driver is Oracle UCP.
     */
    private boolean isUCP;
    
    private boolean sentUCPConnMgrPropsIgnoredInfoMessage;
    private boolean sentUCPDataSourcePropsIgnoredInfoMessage;

    /**
     * JDBC driver service
     */
    private JDBCDriverService jdbcDriverSvc;

    /**
     * Implementation that varies based on JDBC version.
     */
    private JDBCRuntimeVersion jdbcRuntime;

    /**
     * Managed connection factory for data sources where the jdbcDriver service has a configured library.
     * Null if the application's thread context class loader is used to load the JDBC driver classes instead.
     * Only access the value of this field when holding the lock for this instance that is defined in AbstractConnectionFactoryService.
     */
    private WSManagedConnectionFactoryImpl mcf;

    /**
     * Map of class loader identifier to managed connection factory.
     * Entries are added to this map when the jdbcDriver element lacks a library,
     * causing data source classes to be loaded from the application's thread context class loader.
     * Null if the jdbcDriver is configured with a library.
     */
    private ConcurrentHashMap<String, WSManagedConnectionFactoryImpl> mcfPerClassLoader;

    /**
     * Service properties
     */
    private Map<String, Object> properties;

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activate", PropertyService.hidePasswords(properties));
        
        String jndiName = (String) properties.get(JNDI_NAME);
        id = (String) properties.get("config.displayId");

        // avoid conflicts between server.xml data sources and app-defined data sources
        if (isServerDefined = "file".equals(properties.get("config.source")))
            if (jndiName != null && jndiName.startsWith("java:"))
                throw new IllegalArgumentException(ConnectorService.getMessage("UNSUPPORTED_VALUE_J2CA8011", jndiName, JNDI_NAME, DATASOURCE));
            else {
                String id = (String) properties.get(ID);
                if (id != null && id.startsWith(PREFIX))
                    throw new IllegalArgumentException(ConnectorService.getMessage("UNSUPPORTED_VALUE_J2CA8011", id, ID, DATASOURCE));
            }

        lock.writeLock().lock();
        try {
            componentContext = context;
            this.properties = properties;
        } finally {
            lock.writeLock().unlock();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context context for this component instance.
     */
    protected void deactivate(ComponentContext context) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "deactivate");

        destroyConnectionFactories(true);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "deactivate", id + ' ' + context.getProperties().get(JNDI_NAME));
    }

    @Override
    public ApplicationRecycleContext getContext() {
        return null;
    }

    @Override
    public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>(appsToRecycle);
        appsToRecycle.removeAll(members);
        return members;
    }

    /**
     * Utility method to destroy WAS data source instances.
     *
     * @param destroyImmediately indicates to immediately destroy instead of deferring to later.
     */
    protected void destroyConnectionFactories(boolean destroyImmediately) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "destroyConnectionFactories", destroyImmediately, destroyWasDeferred);

        // Notify the application recycle coordinator of an incompatible config update that requires restarting the application
        if (!appsToRecycle.isEmpty()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "recycle applications", appsToRecycle);
            
            ApplicationRecycleCoordinator appRecycleCoord = null;
            appRecycleCoord = (ApplicationRecycleCoordinator) priv.locateService(componentContext,"appRecycleService");
            
            Set<String> members = new HashSet<String>(appsToRecycle);
            appsToRecycle.removeAll(members);
            appRecycleCoord.recycleApplications(members);
        }

        lock.writeLock().lock();
        try {
            if (isInitialized.get() || destroyImmediately && destroyWasDeferred)
                try {
                    // Mark all traditional WAS data source wrappers as disabled
                    isInitialized.set(false);

                    if (destroyImmediately) {
                        // Destroy the data source (it will only exist if it was looked up)
                        conMgrSvc.destroyConnectionFactories();

                        if (isDerbyEmbedded)
                            shutdownDerbyEmbedded();

                        conMgrSvc.deleteObserver(this);
                        jdbcDriverSvc.deleteObserver(this);
                    }

                    destroyWasDeferred = !destroyImmediately;
                } catch (RuntimeException x) {
                    throw x;
                } catch (Exception x) {
                    throw new RuntimeException(x);
                }
        } finally {
            lock.writeLock().unlock();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "destroyConnectionFactories");
    }

    /**
     * @see com.ibm.ws.jca.cm.AppDefinedResource#getApplication()
     */
    @Override
    public String getApplication() {
        return (String) properties.get(APPLICATION);
    }

    /**
     * @see com.ibm.ws.jca.cm.AppDefinedResource#getComponent()
     */
    @Override
    public String getComponent() {
        return (String) properties.get(COMPONENT);
    }

    /**
     * Returns the name of the config element used to configure this type of connection factory.
     *
     * @return the name of the config element used to configure this type of connection factory.
     */
    @Override
    public final String getConfigElementName() {
        return DATASOURCE;
    }

    @Override
    public final ConnectorService getConnectorService() {
        return connectorSvc;
    }

    public Version getFeatureVersion() {
        return jdbcRuntime.getVersion();
    }

    /**
     * Returns the unique identifier for this connection factory configuration.
     *
     * @return the unique identifier for this connection factory configuration.
     */
    @Override
    public String getID() {
        return id;
    }

    /**
     * Returns the JNDI name.
     *
     * @return the JNDI name.
     */
    @Override
    public final String getJNDIName() {
        return dsConfigRef.get().jndiName;
    }
    /**
     * Returns the managed connection factory.
     * 
     * Prerequisite: the invoker must hold a read or write lock on this instance.
     *
     * @param identifier identifier for the class loader from which to load vendor classes (for XA recovery path). Otherwise, null.
     * @return the managed connection factory.
     * @throws Exception if an error occurs obtaining the managed connection factory.
     */
    @Override
    public final ManagedConnectionFactory getManagedConnectionFactory(String identifier) throws Exception {
        WSManagedConnectionFactoryImpl mcf1;
        if (jdbcDriverSvc.loadFromApp()) {
            final boolean trace = TraceComponent.isAnyTracingEnabled();

            // data source class is loaded from thread context class loader
            if (identifier == null) {
                ClassLoader tccl = priv.getContextClassLoader();
                identifier = connectorSvc.getClassLoaderIdentifierService().getClassLoaderIdentifier(tccl);
                // TODO better error handling when thread context class loader does not have an identifier
            }
            mcf1 = mcfPerClassLoader.get(identifier);

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "getManagedConnectionFactory", identifier, mcf1);

            if (mcf1 == null) {
                PropertyService vProps = new PropertyService();
                NavigableMap<String, Object> wProps = parseConfiguration(properties, vProps);

                // Clone properties so that we can later detect modifications from the current values
                vProps = (PropertyService) vProps.clone();

                String jndiName = (String) wProps.remove(JNDI_NAME);
                String type = (String) wProps.remove(DSConfig.TYPE);

                // Trace some of the most important config settings
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "create new data source", id, jndiName, type);

                Object vendorImpl;
                Class<?> ifc;

                if (type == null){
                    boolean atLeastJDBC43 = jdbcRuntime.getVersion().compareTo(JDBCRuntimeVersion.VERSION_4_3) >= 0;
                    vendorImpl = atLeastJDBC43 || id != null && id.contains("dataSource[DefaultDataSource]")
                               ? jdbcDriverSvc.createAnyPreferXADataSource(vProps, id)
                               : jdbcDriverSvc.createAnyPreferLegacyOrder(vProps, id);
                    ifc = vendorImpl instanceof XADataSource ? XADataSource.class
                        : vendorImpl instanceof ConnectionPoolDataSource ? ConnectionPoolDataSource.class
                        : vendorImpl instanceof DataSource ? DataSource.class
                        : Driver.class;
                } else if (ConnectionPoolDataSource.class.getName().equals(type)) {
                    ifc = ConnectionPoolDataSource.class;
                    vendorImpl = jdbcDriverSvc.createConnectionPoolDataSource(vProps, id);
                } else if (XADataSource.class.getName().equals(type)) {
                    ifc = XADataSource.class;
                    vendorImpl = jdbcDriverSvc.createXADataSource(vProps, id);
                } else if (DataSource.class.getName().equals(type)) {
                    ifc = DataSource.class;
                    vendorImpl = jdbcDriverSvc.createDataSource(vProps, id);
                } else if (Driver.class.getName().equals(type)) {
                    ifc = Driver.class;
                    String url = vProps.getProperty("URL", vProps.getProperty("url"));
                    if (url != null && !"".equals(url)) {
                        vendorImpl = jdbcDriverSvc.getDriver(url, vProps, id);
                    } else 
                        throw new SQLNonTransientException(AdapterUtil.getNLSMessage("DSRA4014.URL.for.Driver.missing", jndiName == null ? id : jndiName));
                } else
                    throw new SQLNonTransientException(ConnectorService.getMessage("MISSING_RESOURCE_J2CA8030", DSConfig.TYPE, type, DATASOURCE, jndiName == null ? id : jndiName));

                mcf1 = new WSManagedConnectionFactoryImpl(dsConfigRef, ifc, vendorImpl, jdbcRuntime);
                WSManagedConnectionFactoryImpl mcf0 = mcfPerClassLoader.putIfAbsent(identifier, mcf1);
                mcf1 = mcf0 == null ? mcf1 : mcf0;

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, mcf0 == null ? "created" : "found", mcf1);
            }
        } else
            mcf1 = mcf;

        return mcf1;
    }

    /**
     * @see com.ibm.ws.jca.cm.AppDefinedResource#getModule()
     */
    @Override
    public String getModule() {
        return (String) properties.get(MODULE);
    }

    /** {@inheritDoc} */
    @Override
    public boolean getReauthenticationSupport() {
        return false;
    }

    /**
     * Indicates whether or not thread identity, sync-to-thread, and RRS transactions are supported.
     * The result is a 3 element array, of which,
     * <ul>
     * <li>The first element indicates support for thread identity. 2=REQUIRED, 1=ALLOWED, 0=NOT ALLOWED.</li>
     * <li>The second element indicates support for "synch to thread" for the
     * allocateConnection, i.e., push an ACEE corresponding to the current java
     * Subject on the native OS thread. 1=supported, 0=not supported.</li>
     * <li>The third element indicates support for RRS transactions. 1=supported, 0=not supported.</li>
     * </ul>
     *
     * Prerequisite: the invoker must hold a read or write lock on this instance.
     *
     * @param identifier identifier for the class loader from which to load vendor classes (for XA recovery path). Otherwise, null.
     * @return boolean array indicating whether or not each of the aforementioned capabilities are supported.
     */
    @Override
    public int[] getThreadIdentitySecurityAndRRSSupport(String identifier) {
        WSManagedConnectionFactoryImpl mcf1;
        if (jdbcDriverSvc.loadFromApp()) {
            final boolean trace = TraceComponent.isAnyTracingEnabled();

            // data source class is loaded from thread context class loader
            if (identifier == null) {
                ClassLoader tccl = priv.getContextClassLoader();
                identifier = connectorSvc.getClassLoaderIdentifierService().getClassLoaderIdentifier(tccl);
                // TODO better error handling when thread context class loader does not have an identifier
            }
            mcf1 = mcfPerClassLoader.get(identifier);
        } else
            mcf1 = mcf;

        DatabaseHelper dbHelper = mcf1.getHelper();
        return new int[] { dbHelper.getThreadIdentitySupport(), dbHelper.getThreadSecurity() ? 1 : 0, dbHelper.getRRSTransactional() ? 1 : 0 };
    }

    /** {@inheritDoc} */
    @Override
    public TransactionSupportLevel getTransactionSupport() {
        return dsConfigRef.get().transactional ? TransactionSupportLevel.XATransaction : TransactionSupportLevel.NoTransaction;
    }

    @Override
    public boolean getValidatingManagedConnectionFactorySupport() {
        return true;
    }

    /**
     * Lazy initialization.
     * Precondition: invoker must have write lock on this DataSourceService
     *
     * @throws Exception if an error occurs
     */
    @Override
    protected void init() throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        try {
            PropertyService vProps = new PropertyService();
            NavigableMap<String, Object> wProps = parseConfiguration(properties, vProps);

            // Clone properties so that we can later detect modifications from the current values
            vProps = (PropertyService) vProps.clone();

            String jndiName = (String) wProps.remove(JNDI_NAME);
            String type = (String) wProps.remove(DSConfig.TYPE);

            // Trace some of the most important config settings
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, DATASOURCE, id, jndiName, type);

            // Get the connection manager service for this data source. If none is configured, then use defaults.
            conMgrSvc = (ConnectionManagerService) priv.locateService(componentContext,CONNECTION_MANAGER);

            boolean createdDefaultConnectionManager = false;
            
            if (conMgrSvc == null) {
                if (wProps.containsKey(DSConfig.CONNECTION_MANAGER_REF)) {
                    SQLNonTransientException failure = connectorSvc.ignoreWarnOrFail(tc, null, SQLNonTransientException.class, "MISSING_RESOURCE_J2CA8030",
                                                                                    DSConfig.CONNECTION_MANAGER_REF, "", DATASOURCE, jndiName == null ? id : jndiName);
                    if (failure != null)
                        throw failure;
                }
                conMgrSvc = ConnectionManagerService.createDefaultService(id);
                createdDefaultConnectionManager = true;

            }
            conMgrSvc.addObserver(this);

            // Obtain the data source from the JDBC driver
            // TODO: Switch to org.osgi.service.jdbc.DataSourceFactory for compatibility with OSGI JDBC drivers
            jdbcDriverSvc = (JDBCDriverService) priv.locateService(componentContext,JDBC_DRIVER);

            if (jdbcDriverSvc == null) {
                Tr.error(tc, "DSRA4003.driver.null", jndiName == null ? id : jndiName);

                throw new SQLNonTransientException(ConnectorService.getMessage("MISSING_RESOURCE_J2CA8030", DSConfig.JDBC_DRIVER_REF,
                                                                               "", DATASOURCE, jndiName == null ? id : jndiName));
            }
            jdbcDriverSvc.addObserver(this);

            Object vendorImpl;
            Class<?> ifc;

            if(type == null){
                boolean atLeastJDBC43 = jdbcRuntime.getVersion().compareTo(JDBCRuntimeVersion.VERSION_4_3) >= 0;
                vendorImpl = atLeastJDBC43 || id != null && id.contains("dataSource[DefaultDataSource]")
                                ? jdbcDriverSvc.createAnyPreferXADataSource(vProps, id)
                                : jdbcDriverSvc.createAnyPreferLegacyOrder(vProps, id);
                ifc = vendorImpl instanceof XADataSource ? XADataSource.class
                    : vendorImpl instanceof ConnectionPoolDataSource ? ConnectionPoolDataSource.class
                    : vendorImpl instanceof DataSource ? DataSource.class
                    : Driver.class;
            } else if (ConnectionPoolDataSource.class.getName().equals(type)) {
                ifc = ConnectionPoolDataSource.class;
                vendorImpl = jdbcDriverSvc.createConnectionPoolDataSource(vProps, id);
            } else if (XADataSource.class.getName().equals(type)) {
                ifc = XADataSource.class;
                vendorImpl = jdbcDriverSvc.createXADataSource(vProps, id);
            } else if (DataSource.class.getName().equals(type)) {
                ifc = DataSource.class;
                vendorImpl = jdbcDriverSvc.createDataSource(vProps, id);
            } else if (Driver.class.getName().equals(type)) {
                ifc = Driver.class;
                String url = vProps.getProperty("URL", vProps.getProperty("url"));
                if (url != null && !"".equals(url)) {
                    vendorImpl = jdbcDriverSvc.getDriver(url, vProps, id);
                } else 
                    throw new SQLNonTransientException(AdapterUtil.getNLSMessage("DSRA4014.URL.for.Driver.missing", jndiName == null ? id : jndiName));
            } else
                throw new SQLNonTransientException(ConnectorService.getMessage("MISSING_RESOURCE_J2CA8030", DSConfig.TYPE, type, DATASOURCE, jndiName == null ? id : jndiName));

            // Convert isolationLevel constant name to integer
            vendorImplClassName = vendorImpl.getClass().getName();
            parseIsolationLevel(wProps, vendorImplClassName);
            
            Object objIsolationLevel = wProps.get(DataSourceDef.isolationLevel.name());
            int wIsolationLevel = objIsolationLevel == null ? -1 : (int) objIsolationLevel;
            
            if(wIsolationLevel == Connection.TRANSACTION_NONE) {
                Object objTransactional = wProps.get(DataSourceDef.transactional.name());
                boolean wTransactional = objTransactional == null ? true : (boolean) objTransactional;
                
                if (wTransactional) {
                    throw new SQLException(AdapterUtil.getNLSMessage("DSRA4009.tran.none.transactional.unsupported", id));
                }
            }

            // Derby Embedded needs a reference count so that we can shutdown databases when no longer used.
            isDerbyEmbedded = vendorImplClassName.startsWith("org.apache.derby.jdbc.Embedded");
            if (isDerbyEmbedded) {
                String dbName = (String) vProps.get(DataSourceDef.databaseName.name());
                if (dbName != null) {
                    // Maintaining compatibility here. Variables are no longer normalized by default, but the ResourceFactoryBuilder is still
                    // using VariableRegistry.resolveString() to resolve variables in data source definitions. 
                    dbName = PathUtils.normalize(dbName);
                    embDerbyRefCount.add(dbName);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "ref count for database shutdown", dbName, embDerbyRefCount);
                }
            }

            isUCP = vendorImplClassName.startsWith("oracle.ucp");
            if (isUCP) {
                if (!createdDefaultConnectionManager && !sentUCPConnMgrPropsIgnoredInfoMessage) {
                    Tr.info(tc, "DSRA4013.ignored.connection.manager.config.used", Arrays.asList("enableSharingForDirectLookups", ConnectionManagerService.ENABLE_CONTAINER_AUTH_FOR_DIRECT_LOOKUPS));
                    sentUCPConnMgrPropsIgnoredInfoMessage = true;
                }
                updateConfigForUCP(wProps);
            }

            dsConfigRef.set(new DSConfig(id, jndiName, wProps, vProps, connectorSvc));

            WSManagedConnectionFactoryImpl mcfImpl = new WSManagedConnectionFactoryImpl(dsConfigRef, ifc, vendorImpl, jdbcRuntime);

            if (jdbcDriverSvc.loadFromApp()) {
                // data source class loaded from thread context class loader
                mcf = null;
                mcfPerClassLoader = new ConcurrentHashMap<String, WSManagedConnectionFactoryImpl>();
                ClassLoader tccl = priv.getContextClassLoader();
                String identifier = connectorSvc.getClassLoaderIdentifierService().getClassLoaderIdentifier(tccl);
                mcfPerClassLoader.put(identifier, mcfImpl);
            } else {
                // data source class loaded from shared library
                mcf = mcfImpl;
                mcfPerClassLoader = null;
            }

            isInitialized.set(true);
        } catch (Exception x) {
            FFDCFilter.processException(x, getClass().getName(), "587", this);
            // undo any partial initialization
            if (conMgrSvc != null)
                conMgrSvc.deleteObserver(this);
            if (jdbcDriverSvc != null)
                jdbcDriverSvc.deleteObserver(this);
            throw x;
        } catch (Error x) {
            FFDCFilter.processException(x, getClass().getName(), "591", this);
            // undo any partial initialization
            if (conMgrSvc != null)
                conMgrSvc.deleteObserver(this);
            if (jdbcDriverSvc != null)
                jdbcDriverSvc.deleteObserver(this);
            throw x;
        }
    }

    /**
     * Utility method that does a shallow match of two objects,
     * or, if both are String[], then a deep match.
     *
     * @param o1 first object
     * @param o2 second object
     * @return true if they match. Otherwise false.
     */
    private static final boolean match(Object o1, Object o2) {
        return o1 instanceof String[] && o2 instanceof String[]
                        ? Arrays.deepEquals((String[]) o1, (String[]) o2)
                        : AdapterUtil.match(o1, o2);
    }

    /**
     * Called by Declarative Services to modify service config properties
     *
     * @param Context the component context
     * @param newProperties the new configuration
     * @throws Exception if unable to complete the modifications
     */
    protected void modified(ComponentContext context, Map<String, Object> newProperties) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "modified", AdapterUtil.toString(newProperties));

        String jndiName = (String) newProperties.get(JNDI_NAME);

        lock.writeLock().lock();
        try {
            componentContext = context;
            if (!AdapterUtil.match(jndiName, properties.get(JNDI_NAME))) {
                // To change JNDI name, must re-run the activate code
                deactivate(context);
                activate(context, newProperties);
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "modified", "jndiName changed");
                return;
            } else if (isInitialized.get()) {
                PropertyService vProps = new PropertyService();
                NavigableMap<String, Object> wProps = parseConfiguration(newProperties, vProps);
                wProps.remove(JNDI_NAME);
                DSConfig config = dsConfigRef.get();
                if (!match(newProperties.get(DSConfig.CONNECTION_MANAGER_REF), properties.get(DSConfig.CONNECTION_MANAGER_REF))
                    || !match(newProperties.get(DSConfig.HELPER_CLASS), properties.get(DSConfig.HELPER_CLASS))
                    || !match(newProperties.get(DSConfig.JDBC_DRIVER_REF), properties.get(DSConfig.JDBC_DRIVER_REF))
                    || !match(newProperties.get(DSConfig.ON_CONNECT), properties.get(DSConfig.ON_CONNECT))
                    || !match(newProperties.get(DataSourceDef.transactional.name()), properties.get(DataSourceDef.transactional.name()))
                    || connectorSvc.isHeritageEnabled() && !config.identifyExceptions.equals(wProps.get(DSConfig.IDENTIFY_EXCEPTION))) {
                    // Destroy everything, and allow lazy initialization to recreate
                    destroyConnectionFactories(true);
                } else if (!AdapterUtil.match(vProps, config.vendorProps)
                           || !AdapterUtil.match(wProps.get(DSConfig.SUPPLEMENTAL_JDBC_TRACE), properties.get(DSConfig.SUPPLEMENTAL_JDBC_TRACE))
                           || !AdapterUtil.match(wProps.remove(DSConfig.TYPE), properties.get(DSConfig.TYPE))) {
                    // Reinitialize with a new MCF and let the old connections go away via agedTimeout or claim victim
                    // Defer the destroy until later, unless we are using Derby Embedded, in which case we need
                    // to issue a shutdown of the Derby database in order to free it up for other class loaders.
                    // Destroy now if switching to/from UCP to ensure that the connection manager is initialized
                    // with the proper properties to disable/enable Liberty connection pooling
                    isUCP = isUCP || "com.ibm.ws.jdbc.dataSource.properties.oracle.ucp".equals(newProperties.get("properties.0.config.referenceType"));
                    destroyConnectionFactories(isDerbyEmbedded || isUCP);
                } else {
                    parseIsolationLevel(wProps, vendorImplClassName);
                    
                    if(isUCP) {
                        updateConfigForUCP(wProps);
                    }
                    
                    // Swap the reference to the configuration - the WAS data source will start honoring it
                    dsConfigRef.set(new DSConfig(config, wProps));
                }
            } // else hasn't been used yet, just swap the properties

            properties = newProperties;
        } finally {
            lock.writeLock().unlock();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "modified");
    }

    /**
     * Utility method that parses out the WAS data source configuration.
     *
     * @param configProps properties from the config service
     * @param vProps empty list of vendor properties to populate as we parse through the list of service properties
     * @return WAS data source properties
     * @throws Exception if an error occurs
     */
    private NavigableMap<String, Object> parseConfiguration(Map<String, Object> configProps, PropertyService vProps) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // WAS data source properties
        NavigableMap<String, Object> wProps = new TreeMap<String, Object>();

        String vPropsPID = null;
        
        boolean recommendAuthAlias = false;
        for (Map.Entry<String, Object> entry : configProps.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Filter out flattened config. For example:
            //  properties.0.databaseName
            //  heritage.0.replaceExceptions
            //  identifyException.2.sqlState
            int dot1 = key.indexOf('.'), length = key.length();
            int dot2 = dot1 > 0 && length > dot1 + 2 ? key.indexOf('.', dot1 + 2) : -1;
            String flatPrefix = dot2 > dot1 + 1 ? key.substring(0, dot1) : null;

            if (flatPrefix == null) {
                if (dot1 == -1 && !WPROPS_TO_SKIP.contains(key))
                    wProps.put(key, value);
            } else if (flatPrefix.equals(PropertyService.PROPERTIES)) {
                key = key.substring(dot2 + 1);
                if (key.equals("config.referenceType")) {
                    if (vPropsPID == null)
                        vProps.setFactoryPID(vPropsPID = (String) value);
                    else
                        throw new SQLFeatureNotSupportedException(ConnectorService.getMessage("CARDINALITY_ERROR_J2CA8040", DATASOURCE, id, PropertyService.PROPERTIES));
                } else {
                    if (value instanceof Long)
                        if (PropertyService.DURATION_MIXED_PROPS.contains(key))
                            value = ((Long) value).toString(); // figure it out later by introspection

                    if (PropertyService.isPassword(key)) {
                        if (value instanceof SerializableProtectedString) {
                            String password = new String(((SerializableProtectedString) value).getChars());
                            value = PasswordUtil.getCryptoAlgorithm(password) == null ? password : PasswordUtil.decode(password);
                        } else if (value instanceof String) {
                            String password = (String) value;
                            value = PasswordUtil.getCryptoAlgorithm(password) == null ? password : PasswordUtil.decode(password);
                        }
                        if (DataSourceDef.password.name().equals(key))
                            recommendAuthAlias = true;
                    } else if (trace && tc.isDebugEnabled()) {
                        if(key.toLowerCase().equals("url")) {
                            if(value instanceof String)
                                Tr.debug(this, tc, "Found vendor property: " + key + '=' + PropertyService.filterURL((String) value));
                        } else {
                            Tr.debug(this, tc, "Found vendor property: " + key + '=' + value);
                        }
                    }

                    vProps.put(key, value);
                }
            } else if (flatPrefix.equals(DSConfig.HERITAGE)) {
                if (DSConfig.HELPER_CLASS.equals(key) || DSConfig.REPLACE_EXCEPTIONS.equals(key))
                    wProps.put(key, value);
            }
        }

        // identifyException, which is a group of
        // (identifyException.#.sqlState, identifyException.#.errorCode, identifyException.#.as)
        // is parsed separately to have a predictable order of precedence when collisions occur
        Map<Object, String> identifications = null;
        String keyFormat = "identifyException.%d.%s";
        String key;
        for (int i = 0; i < 1000; i++) { // cardinality is capped at 1000 in metatype
            key = String.format(keyFormat, i, "as");
            String as = (String) configProps.get(key);
            if (as == null)
                break; // no more nested identifyException elements
            key = String.format(keyFormat, i, "sqlState");
            String sqlState = (String) configProps.get(key);
            key = String.format(keyFormat, i, "errorCode");
            Integer errorCode = (Integer) configProps.get(key);
            if (i == 0)
                identifications = new HashMap<Object, String>();
            if (sqlState == null && errorCode == null) {
                Tr.error(tc, "8067E_IDENTIFY_EXCEPTION_ERRCODE_SQLSTATE");
                throw new IllegalArgumentException(AdapterUtil.getNLSMessage("8067E_IDENTIFY_EXCEPTION_ERRCODE_SQLSTATE"));
            } else if (sqlState == null) {
                identifications.put(errorCode, as);
            } else if (errorCode == null ) {
                identifications.put(sqlState, as);
            } else { // both sqlState and errorCode are supplied
                identifications.put(new SQLStateAndCode(sqlState, errorCode), as);
            }
        }
        wProps.put(DSConfig.IDENTIFY_EXCEPTION, identifications == null ? Collections.EMPTY_MAP : identifications);

        //Don't send out auth alias recommendation message with UCP since it may be required to set the 
        //user and password as ds props
        if(recommendAuthAlias && !"com.ibm.ws.jdbc.dataSource.properties.oracle.ucp".equals(vPropsPID))
            ConnectorService.logMessage(Level.INFO, "RECOMMEND_AUTH_ALIAS_J2CA8050", id);

        if (vPropsPID == null)
            vProps.setFactoryPID(PropertyService.FACTORY_PID);

        return wProps;
    }

    /**
     * Utility method that converts transaction isolation level constant names
     * to the corresponding int value.
     *
     * @param wProps WAS data source properties, including the configured isolationLevel property.
     * @param vendorImplClassName name of the vendor data source or driver implementation class.
     * @return Integer transaction isolation level constant value. If unknown, then the original String value.
     */
    private static final void parseIsolationLevel(NavigableMap<String, Object> wProps, String vendorImplClassName) {
        // Convert isolationLevel constant name to integer
        Object isolationLevel = wProps.get(DataSourceDef.isolationLevel.name());
        if (isolationLevel instanceof String) {
            isolationLevel = "TRANSACTION_READ_COMMITTED".equals(isolationLevel) ? Connection.TRANSACTION_READ_COMMITTED
                            : "TRANSACTION_REPEATABLE_READ".equals(isolationLevel) ? Connection.TRANSACTION_REPEATABLE_READ
                                            : "TRANSACTION_SERIALIZABLE".equals(isolationLevel) ? Connection.TRANSACTION_SERIALIZABLE
                                                            : "TRANSACTION_READ_UNCOMMITTED".equals(isolationLevel) ? Connection.TRANSACTION_READ_UNCOMMITTED
                                                                            : "TRANSACTION_NONE".equals(isolationLevel) ? Connection.TRANSACTION_NONE
                                                                                            : "TRANSACTION_SNAPSHOT".equals(isolationLevel) ? (vendorImplClassName.startsWith("com.microsoft.") ? 4096 : 16)
                                                                                                            : isolationLevel;

            wProps.put(DataSourceDef.isolationLevel.name(), isolationLevel);
        }
    }

    /**
     * Declarative Services method for setting the connection manager service reference
     *
     * @param ref reference to the service
     */
    protected void setConnectionManager(ServiceReference<ConnectionManagerService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setConnectionManager", ref);
    }

    /**
     * Declarative services method to set the ConnectorService.
     */
    protected void setConnectorService(ConnectorService svc) {
        connectorSvc = svc;
    }

    /**
     * Declarative Services method for setting the JDBC driver service reference
     *
     * @param ref reference to the service
     */
    protected void setDriver(ServiceReference<JDBCDriverService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setDriver", ref);
    }
    
    protected void setJdbcRuntimeVersion(JDBCRuntimeVersion ref) {
        jdbcRuntime = ref;
    }

    /**
     * Shuts down the Derby database if we are the last one using it.
     * Note that other threads might increment the reference count and start using the database
     * after we have checked the reference count but before we finish shutting down derby.
     * If that happens, the application will see errors. We are not attempting to prevent that.
     * Precondition: invoker must have at least a read lock on this DataSourceService instance.
     */
    private void shutdownDerbyEmbedded() {
        DSConfig cfg = dsConfigRef.get();
        Hashtable<?, ?> vProps = cfg.vendorProps;
        String dbName = (String) vProps.get(DataSourceDef.databaseName.name());
        if (dbName == null) // Avoid causing null pointer when configuration is bad
            return;

        // Maintaining compatibility here. Variables are no longer normalized by default, but the ResourceFactoryBuilder is still
        // using VariableRegistry.resolveString() to resolve variables in data source definitions. 
        dbName = PathUtils.normalize(dbName);
        
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "shutdownDerbyEmbedded", dbName, embDerbyRefCount);

        if (embDerbyRefCount.remove(dbName) && !embDerbyRefCount.contains(dbName))
            try {
                ClassLoader jdbcDriverLoader = jdbcDriverSvc.getClassLoaderForLibraryRef();
                if (jdbcDriverLoader != null) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "using classloader", jdbcDriverLoader);

                    Class<?> EmbDS = AdapterUtil.forNameWithPriv("org.apache.derby.jdbc.EmbeddedDataSource40", true, jdbcDriverLoader);
                    DataSource ds = (DataSource) EmbDS.newInstance();

                    EmbDS.getMethod("setDatabaseName", String.class).invoke(ds, dbName);
                    EmbDS.getMethod("setShutdownDatabase", String.class).invoke(ds, "shutdown");
                    String user = (String) vProps.get(DataSourceDef.user.name());
                    if (user != null)
                        EmbDS.getMethod("setUser", String.class).invoke(ds, user);
                    String pwd = (String) vProps.get(DataSourceDef.password.name());
                    if (pwd != null)
                        EmbDS.getMethod("setPassword", String.class).invoke(ds, pwd);

                    ds.getConnection().close();
                }
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "shutdownDerbyEmbedded");
            } catch (SQLException x) {
                // expected for shutdown
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "shutdownDerbyEmbedded", x.getSQLState() + ' ' + x.getErrorCode() + ':' + x.getMessage());
            } catch (Throwable x) {
                // Work around Derby issue when the JVM is shutting down while Derby shutdown is requested.
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "shutdownDerbyEmbedded", x);
            }
        else if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "shutdownDerbyEmbedded", false);
    }

    /**
     * Declarative Services method for unsetting the connection manager service reference
     *
     * @param ref reference to the service
     */
    protected void unsetConnectionManager(ServiceReference<ConnectionManagerService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "unsetConnectionManager", ref);
    }

    /**
     * Declarative services method to unset the ConnectorService.
     */
    protected void unsetConnectorService(ConnectorService svc) {
        connectorSvc = null;
    }

    /**
     * Declarative Services method for unsetting the JDBC driver service reference
     *
     * @param ref reference to the service
     */
    protected void unsetDriver(ServiceReference<JDBCDriverService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "unsetDriver", ref);
    }
    
    protected void unsetJdbcRuntimeVersion(JDBCRuntimeVersion ref) {
        jdbcRuntime = null;
    }

    /** {@inheritDoc} */
    @Override
    public void update(Observable observable, Object data) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "service updated", observable);

        destroyConnectionFactories(true);
    }

    /** {@inheritDoc} */
    @Override
    protected void checkAccess() throws ResourceException {}

    @Override
    public void setMQQueueManager(Serializable xaresinfo) throws Exception {
        // no-op, not implemented for data sources        
    }
    
    /**
     * This method contains the common config related tasks that need to be done when the DataSourceService is 
     * initialized or modified. It outputs an informational message for connection manager and datasource properties
     * that will be ignored and sets the proper values for the ignored DataSource properties: statementCacheSize 
     * and ValidationTimeout.
     * @param wProps 
     */
    private void updateConfigForUCP(NavigableMap<String, Object> wProps) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Updating config for UCP");
        
        if (wProps.remove(DSConfig.VALIDATION_TIMEOUT) != null) {
            if(!sentUCPDataSourcePropsIgnoredInfoMessage) {     
                Set<String> dsPropsIgnored = new LinkedHashSet<String>();
                dsPropsIgnored.add("statementCacheSize");
                dsPropsIgnored.add("validationTimeout");
                Tr.info(tc, "DSRA4012.ignored.datasource.config.used", dsPropsIgnored);
                sentUCPDataSourcePropsIgnoredInfoMessage = true;
            }
        }
        
        Object statementCacheSize = wProps.get(DSConfig.STATEMENT_CACHE_SIZE);
        if(statementCacheSize != null) {
            long numericVal = -1;
            if (statementCacheSize instanceof Number)
                numericVal = ((Number) statementCacheSize).longValue();
            else
                try {
                    numericVal = Integer.parseInt((String) statementCacheSize);
                } catch (Exception x) {
                    //don't need to surface this exception since we are ignoring the config anyway
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Caught the following exception parsing statement cache size: " + x);
                }
            if(numericVal != 0) {
                wProps.put(DSConfig.STATEMENT_CACHE_SIZE, 0);
                //To avoid always sending ignored ds config message, don't send it for a value of 10 since that's the default
                if(numericVal != 10) {
                    if(!sentUCPDataSourcePropsIgnoredInfoMessage) {
                        Set<String> dsPropsIgnored = new LinkedHashSet<String>();
                        dsPropsIgnored.add("statementCacheSize");
                        dsPropsIgnored.add("validationTimeout");
                        Tr.info(tc, "DSRA4012.ignored.datasource.config.used", dsPropsIgnored);
                        sentUCPDataSourcePropsIgnoredInfoMessage = true;
                    }
                }
            }
        } else {
            //this shouldn't be possible since statementCacheSize has a default of 10
            wProps.put(DSConfig.STATEMENT_CACHE_SIZE, 0);
        }
    }
    
    @Override
    public boolean isLibertyConnectionPoolingDisabled() {
        return isUCP;
    }
}
