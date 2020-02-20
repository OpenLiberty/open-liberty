/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.internal;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.CommonDataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.crypto.InvalidPasswordDecodingException;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.cm.AppDefinedResource;
import com.ibm.ws.jca.cm.ConnectorService;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.library.LibraryChangeListener;
import com.ibm.wsspi.library.Library;

/**
 * Provides information about a JDBC driver.
 * 
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 * 
 * OSGi methods (activate/deactivate) should be protected.
 */
public class JDBCDriverService extends Observable implements LibraryChangeListener {
    private static final TraceComponent tc = Tr.register(JDBCDriverService.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Factory persistent identifier for JDBCDriverService.
     */
    public final static String FACTORY_PID = "com.ibm.ws.jdbc.jdbcDriver";

    /**
     * Name of unique identifier property.
     */
    static final String ID = "id";

    /**
     * Name of attribute that refers to the shared library for the JDBC driver.
     */
    public static final String LIBRARY_REF = "libraryRef";

    /**
     * Name of element used for JDBC driver configuration.
     */
    public static final String JDBC_DRIVER = "jdbcDriver";

    /**
     * Name of internal attribute that specifies the target shared library service
     */
    public static final String TARGET_LIBRARY = "sharedLib.target";

    /**
     * Properties that only apply to javax.sql.XADataSource implementations.
     * We will ignore these for ConnectionPoolDataSource/DataSource.
     */
    private static final List<String> PROPS_FOR_XA_ONLY = Arrays.asList("ifxIFX_XASPEC");
    
    /**
     * Properties that should not be set on the JDBC driver.
     */
    private static final List<String> PROPS_NOT_SET_ON_DRIVER = Arrays.asList("isolationLevelSwitchingSupport");

    /**
     * Class loader instance. If null, JDBC driver classes should be loaded from the
     * application's thread context class loader.
     */
    private ClassLoader classloader;

    /**
     * Utility that collects various core services needed by connection management and JDBC
     */
    private ConnectorService connectorSvc;

    /**
     * Derby Embedded only - List Derby Embedded class loaders used by jdbcDrivers
     * with a library defined in server configuration.
     * A class loader instance can appear multiple times in the list,
     * which serves as a reference count.
     * When the reference count reaches 0, the Derby system can be shut down.
     */
    private static ConcurrentLinkedQueue<ClassLoader> embDerbyRefCount = new ConcurrentLinkedQueue<ClassLoader>();

    /**
     * Data source classes that we introspected.
     * We should remove these classes from the Introspector cache
     * when the jdbcDriver is deactivated or modified.
     */
    private final HashSet<Class<? extends CommonDataSource>> introspectedClasses = new HashSet<Class<? extends CommonDataSource>>();

    /**
     * Indicates if the JDBC driver is Derby Embedded.
     */
    private final AtomicBoolean isDerbyEmbedded = new AtomicBoolean();

    /**
     * Indicates if initialization has been performed on this instance.
     */
    private boolean isInitialized;

    /**
     * Lock for reading and updating JDBC driver configuration.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Name that we display in messages. It is based on the id (or config.displayId),
     * but shortened for nested config.
     */
    private String name;

    /**
     * JDBC driver configuration.
     */
    private Dictionary<String, ?> properties;

    /**
     * The shared library for this jdbcDriver.
     */
    private Library sharedLib;

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context for this component instance
     */
    protected void activate(ComponentContext context) {
        Dictionary<String, ?> props = context.getProperties();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activate", props);

        name = (String) props.get("config.displayId");
        lock.writeLock().lock();
        try {
            properties = props;
        } finally {
            lock.writeLock().unlock();
        }

        if ("file".equals(props.get("config.source"))) {
            if (name.startsWith(AppDefinedResource.PREFIX)) // avoid conflicts with application defined data sources
                throw new IllegalArgumentException(ConnectorService.getMessage("UNSUPPORTED_VALUE_J2CA8011", name, ID, JDBC_DRIVER));
        }
        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /**
     * Returns an exception to raise when the data source class is not found.
     *
     * @param interfaceNames names of data source interface(s) or java.sql.Driver for which we could not find an implementation class.
     * @param packagesSearched packages in or beneath which we have searched for data source implementation classes.
     * @param dsId identifier for the data source that is using this JDBC driver.
     * @param cause error that already occurred. Null if not applicable.
     * @return an exception to raise when the data source class is not found.
     */
    private SQLException classNotFound(Object interfaceNames, Set<String> packagesSearched, String dsId, Throwable cause) {
        if (cause instanceof SQLException)
            return (SQLException) cause;

        // TODO need an appropriate message when sharedLib is null and classes are loaded from the application
        String sharedLibId = sharedLib.id();

        // Determine the list of folders that should contain the JDBC driver files.
        Collection<String> driverJARs = getClasspath(sharedLib, false);

        String message = sharedLibId.startsWith("com.ibm.ws.jdbc.jdbcDriver-")
                        ? AdapterUtil.getNLSMessage("DSRA4001.no.suitable.driver.nested", interfaceNames, dsId, driverJARs, packagesSearched)
                        : AdapterUtil.getNLSMessage("DSRA4000.no.suitable.driver", interfaceNames, dsId, sharedLibId, driverJARs, packagesSearched);

        return new SQLNonTransientException(message, cause);
    }

    /**
     * Utility method for creating all types of data sources.
     * Precondition: invoker must have at least a read lock on this JDBC driver service.
     * 
     * @param type data source interface in javax.sql package
     * @param className name of data source class to create
     * @param props typed data source properties
     * @param dsID identifier for the data source
     * @return the data source
     * @throws SQLException if an error occurs
     */
    private <T extends CommonDataSource> T create(final String className, final Hashtable<?, ?> props, String dsID) throws SQLException {
        if (classloader != null && className.startsWith("org.apache.derby.jdbc.Embedded") && isDerbyEmbedded.compareAndSet(false, true)) {
            embDerbyRefCount.add(classloader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "ref count for shutdown", classloader, embDerbyRefCount);
        }

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "create", className, classloader, PropertyService.hidePasswords(props));

        //Add a value for connectionFactoryClassName when using UCP if one is not specified
        if (className.startsWith("oracle.ucp.jdbc") && !props.containsKey("connectionFactoryClassName")) {
            if (className.equals("oracle.ucp.jdbc.PoolDataSourceImpl") && props instanceof PropertyService) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "Setting connectionFactoryClassName property to oracle.jdbc.pool.OracleDataSource");
                ((PropertyService) props).setProperty("connectionFactoryClassName", "oracle.jdbc.pool.OracleDataSource");
            } else if (className.equals("oracle.ucp.jdbc.PoolXADataSourceImpl") && props instanceof PropertyService) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "Setting connectionFactoryClassName property to oracle.jdbc.xa.client.OracleXADataSource");
                ((PropertyService) props).setProperty("connectionFactoryClassName", "oracle.jdbc.xa.client.OracleXADataSource");
            }
        }
        try {
            T ds = AccessController.doPrivileged(new PrivilegedExceptionAction<T>() {
                public T run() throws Exception {
                    ClassLoader loader, origTCCL = Thread.currentThread().getContextClassLoader();
                    try {
                        if (classloader == null)
                            loader = origTCCL; // Use thread context class loader of application in absence of server configured library
                        else
                            Thread.currentThread().setContextClassLoader(loader = classloader);

                        @SuppressWarnings("unchecked")
                        Class<T> dsClass = (Class<T>) loader.loadClass(className);
                        introspectedClasses.add(dsClass);
                        T ds = dsClass.newInstance();

                        // Set all of the JDBC vendor properties
                        Hashtable<?, ?> p = (Hashtable<?, ?>) props.clone();
                        for (PropertyDescriptor descriptor : Introspector.getBeanInfo(dsClass).getPropertyDescriptors()) {
                            String name = descriptor.getName();
                            Object value = p.remove(name);

                            // handle osgi vs non-osgi URL property
                            if (value == null && name.equals("url")) {
                                value = p.remove("URL");
                            }

                            boolean isPassword = PropertyService.isPassword(name);

                            if (value != null)
                                try {
                                    if (value instanceof String) {
                                        String str = (String) value;
                                        // Decode password(s) that are embedded in value of Oracle connectionProperties property
                                        if ("connectionProperties".equals(name) && className.startsWith("oracle.jdbc") && 
                                                        (str.contains("javax.net.ssl.keyStorePassword") || str.contains("javax.net.ssl.trustStorePassword" ))); {
                                            str = decodeOracleConnectionPropertiesPwds(str);
                                        }
                                        // Decode passwords
                                        if (isPassword)
                                            str = PasswordUtil.getCryptoAlgorithm(str) == null ? str : PasswordUtil.decode(str);
                                        setProperty(ds, descriptor, str, !isPassword);
                                    } else {
                                        // Property already has correct non-String type
                                        if (trace && tc.isDebugEnabled())
                                            Tr.debug(tc, "set " + name + " = " + value);
                                        descriptor.getWriteMethod().invoke(ds, value);
                                    }
                                } catch (Throwable x) {
                                    if (x instanceof InvocationTargetException)
                                        x = x.getCause();
                                    FFDCFilter.processException(x, getClass().getName(), "217", this, new Object[] { className, name, value });
                                    boolean isURL = ("URL".equals(name) || "url".equals(name)) && value instanceof String;
                                    SQLException failure = connectorSvc.ignoreWarnOrFail(tc, x, SQLException.class, "PROP_SET_ERROR", name,
                                                                                                     "=" + (isPassword ? "******" : isURL ? PropertyService.filterURL((String) value) : value),
                                                                                                     AdapterUtil.stackTraceToString(x));
                                    if (failure != null)
                                        throw failure;
                                }
                        }

                        // Are there any properties remaining for which we couldn't find setters?
                        if (!p.isEmpty())
                            for (Object propertyName : p.keySet())
                                // Filter out properties that only apply to XA or that shouldn't be set on the driver
                                if ((!PROPS_FOR_XA_ONLY.contains(propertyName) || ds instanceof XADataSource) && !PROPS_NOT_SET_ON_DRIVER.contains(propertyName) ) {
                                    SQLException failure = connectorSvc.ignoreWarnOrFail(tc, null, SQLException.class, "PROP_NOT_FOUND", className, propertyName);
                                    if (failure != null)
                                        throw failure;
                                }

                        return ds;
                    } finally {
                        if (classloader != null)
                            Thread.currentThread().setContextClassLoader(origTCCL);
                    }
                }
            });
            if (trace && tc.isEntryEnabled())
                Tr.exit(tc, "create", ds);
            return ds;
        } catch (PrivilegedActionException privX) {
            Throwable x = privX.getCause();
            FFDCFilter.processException(x, JDBCDriverService.class.getName(), "234");
            int lastDot = className.lastIndexOf('.');
            Set<String> searched = Collections.singleton(lastDot > 0 ? className.substring(0, lastDot) : className);
            SQLException sqlX = x instanceof ClassNotFoundException ? classNotFound(className, searched, dsID, x)
                            : x instanceof SQLException ? (SQLException) x
                                            : new SQLNonTransientException(x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(tc, "create", x);
            throw sqlX;
        }
    }

    /**
     * Create any type of data source or java.sql.Driver - whichever is available, looking for
     * known data source impl classes in the following order,
     * <ul>
     * <li>javax.sql.ConnectionPoolDataSource
     * <li>javax.sql.DataSource
     * <li>javax.sql.XADataSource
     * </ul>
     * 
     * @param props typed data source properties
     * @return the data source or driver instance
     * @throws Exception if an error occurs
     * @deprecated only use this method if the jdbc-4.2 or earlier feature is enabled.
     */
    public Object createAnyPreferLegacyOrder(Properties props, String dataSourceID) throws Exception {
        lock.readLock().lock();
        try {
            if (!isInitialized)
                try {
                    // Switch to write lock for lazy initialization
                    lock.readLock().unlock();
                    lock.writeLock().lock();
                    
                    if (!isInitialized) {
                        if (!loadFromApp())
                            classloader = AdapterUtil.getClassLoaderWithPriv(sharedLib);
                        isInitialized = true;
                    }
                } finally {
                    // Downgrade to read lock for rest of method
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }

            String vendorPropertiesPID = props instanceof PropertyService ? ((PropertyService) props).getFactoryPID() : PropertyService.FACTORY_PID;
            String className;

            if (null != (className = (String) properties.get(ConnectionPoolDataSource.class.getName()))
             || null != (className = JDBCDrivers.getConnectionPoolDataSourceClassName(vendorPropertiesPID))
             || null != (className = JDBCDrivers.getConnectionPoolDataSourceClassName(getClasspath(sharedLib, true)))

             || null != (className = (String) properties.get(DataSource.class.getName()))
             || null != (className = JDBCDrivers.getDataSourceClassName(vendorPropertiesPID))
             || null != (className = JDBCDrivers.getDataSourceClassName(getClasspath(sharedLib, true)))

             || null != (className = (String) properties.get(XADataSource.class.getName()))
             || null != (className = JDBCDrivers.getXADataSourceClassName(vendorPropertiesPID))
             || null != (className = JDBCDrivers.getXADataSourceClassName(getClasspath(sharedLib, true))))
                return create(className, props, dataSourceID);

            String url = props.getProperty("URL", props.getProperty("url"));
            if (url != null) {
                Driver driver = loadDriver(null, url, classloader, props, dataSourceID);
                if (driver != null)
                    return driver;
            }

            Set<String> packagesSearched = new LinkedHashSet<String>();
            SimpleEntry<Integer, String> dsEntry = JDBCDrivers.inferDataSourceClassFromDriver(classloader,
                                                                   packagesSearched,
                                                                   JDBCDrivers.CONNECTION_POOL_DATA_SOURCE,
                                                                   JDBCDrivers.DATA_SOURCE,
                                                                   JDBCDrivers.XA_DATA_SOURCE);
            if (dsEntry == null) {
                List<String> interfaceNames = Arrays.asList(ConnectionPoolDataSource.class.getName(),
                                                            DataSource.class.getName(),
                                                            XADataSource.class.getName(),
                                                            Driver.class.getName());
                throw classNotFound(interfaceNames, packagesSearched, dataSourceID, null);                
            }

            return create(className = dsEntry.getValue(), props, dataSourceID);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Create any type of data source or java.sql.Driver - whichever is available, looking for
     * known data source impl classes in the following order,
     * <ul>
     * <li>javax.sql.XADataSource
     * <li>javax.sql.ConnectionPoolDataSource
     * <li>javax.sql.DataSource
     * </ul>
     * This order is different than the legacy (prior to jdbc-4.3 feature) priority,
     * which prioritizes javax.sql.XADataSource after ConnectionPoolDataSource and DataSource.
     * 
     * @param props typed data source properties
     * @param dataSourceID identifier for the data source config
     * @return the data source or driver instance
     * @throws Exception if an error occurs
     */
    public Object createAnyPreferXADataSource(Properties props, String dataSourceID) throws Exception {
        lock.readLock().lock();
        try {
            if (!isInitialized)
                try {
                    // Switch to write lock for lazy initialization
                    lock.readLock().unlock();
                    lock.writeLock().lock();

                    if (!isInitialized) {
                        if (!loadFromApp())
                            classloader = AdapterUtil.getClassLoaderWithPriv(sharedLib);
                        isInitialized = true;
                    }
                } finally {
                    // Downgrade to read lock for rest of method
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }

            String vendorPropertiesPID = props instanceof PropertyService ? ((PropertyService) props).getFactoryPID() : PropertyService.FACTORY_PID;
            String className;
            
            if (null != (className = (String) properties.get(XADataSource.class.getName()))
             || null != (className = JDBCDrivers.getXADataSourceClassName(vendorPropertiesPID))
             || null != (className = JDBCDrivers.getXADataSourceClassName(getClasspath(sharedLib, true)))

             || null != (className = (String) properties.get(ConnectionPoolDataSource.class.getName()))
             || null != (className = JDBCDrivers.getConnectionPoolDataSourceClassName(vendorPropertiesPID))
             || null != (className = JDBCDrivers.getConnectionPoolDataSourceClassName(getClasspath(sharedLib, true)))

             || null != (className = (String) properties.get(DataSource.class.getName()))
             || null != (className = JDBCDrivers.getDataSourceClassName(vendorPropertiesPID))
             || null != (className = JDBCDrivers.getDataSourceClassName(getClasspath(sharedLib, true))))
                return create(className, props, dataSourceID);

            String url = props.getProperty("URL", props.getProperty("url"));
            if (url != null) {
                Driver driver = loadDriver(null, url, classloader, props, "dataSource[DefaultDataSource]");
                if (driver != null)
                    return driver;
            }

            Set<String> packagesSearched = new LinkedHashSet<String>();
            SimpleEntry<Integer, String> dsEntry = JDBCDrivers.inferDataSourceClassFromDriver(classloader,
                                                                   packagesSearched,
                                                                   JDBCDrivers.XA_DATA_SOURCE,
                                                                   JDBCDrivers.CONNECTION_POOL_DATA_SOURCE,
                                                                   JDBCDrivers.DATA_SOURCE);
            if (dsEntry == null) {
                List<String> interfaceNames = Arrays.asList(XADataSource.class.getName(),
                                                            ConnectionPoolDataSource.class.getName(),
                                                            DataSource.class.getName(),
                                                            Driver.class.getName());
                throw classNotFound(interfaceNames, packagesSearched, dataSourceID, null);
            }

            return create(className = dsEntry.getValue(), props, dataSourceID);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Create a ConnectionPoolDataSource
     * 
     * @param props typed data source properties
     * @param dataSourceID identifier for the data source config
     * @return the data source
     * @throws SQLException if an error occurs
     */
    public ConnectionPoolDataSource createConnectionPoolDataSource(Properties props, String dataSourceID) throws SQLException {
        lock.readLock().lock();
        try {
            if (!isInitialized)
                try {
                    // Switch to write lock for lazy initialization
                    lock.readLock().unlock();
                    lock.writeLock().lock();

                    if (!isInitialized) {
                        if (!loadFromApp())
                            classloader = AdapterUtil.getClassLoaderWithPriv(sharedLib);
                        isInitialized = true;
                    }
                } finally {
                    // Downgrade to read lock for rest of method
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }

            String className = (String) properties.get(ConnectionPoolDataSource.class.getName());
            if (className == null) {
                String vendorPropertiesPID = props instanceof PropertyService ? ((PropertyService) props).getFactoryPID() : PropertyService.FACTORY_PID;
                className = JDBCDrivers.getConnectionPoolDataSourceClassName(vendorPropertiesPID);
                if (className == null) {
                    //if properties.oracle.ucp is configured do not search based on classname or infer because the customer has indicated
                    //they want to use UCP, but this will likely pick up the Oracle driver instead of the UCP driver (since UCP has no ConnectionPoolDataSource)
                    if("com.ibm.ws.jdbc.dataSource.properties.oracle.ucp".equals(vendorPropertiesPID)) {
                        throw new SQLNonTransientException(AdapterUtil.getNLSMessage("DSRA4015.no.ucp.connection.pool.datasource", dataSourceID, ConnectionPoolDataSource.class.getName()));
                    }
                    className = JDBCDrivers.getConnectionPoolDataSourceClassName(getClasspath(sharedLib, true));
                    if (className == null) {
                        Set<String> packagesSearched = new LinkedHashSet<String>();
                        SimpleEntry<Integer, String> dsEntry = JDBCDrivers.inferDataSourceClassFromDriver //
                                        (classloader, packagesSearched, JDBCDrivers.CONNECTION_POOL_DATA_SOURCE); 
                        className = dsEntry == null ? null : dsEntry.getValue();
                        if (className == null)
                            throw classNotFound(ConnectionPoolDataSource.class.getName(), packagesSearched, dataSourceID, null);
                    }
                }
            }

            return create(className, props, dataSourceID);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Create a DataSource
     * 
     * @param props typed data source properties
     * @param dataSourceID identifier for the data source config
     * @return the data source
     * @throws SQLException if an error occurs
     */
    public DataSource createDataSource(Properties props, String dataSourceID) throws SQLException {
        lock.readLock().lock();
        try {
            if (!isInitialized)
                try {
                    // Switch to write lock for lazy initialization
                    lock.readLock().unlock();
                    lock.writeLock().lock();

                    if (!isInitialized) {
                        if (!loadFromApp())
                            classloader = AdapterUtil.getClassLoaderWithPriv(sharedLib);
                        isInitialized = true;
                    }
                } finally {
                    // Downgrade to read lock for rest of method
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }

            String className = (String) properties.get(DataSource.class.getName());
            if (className == null) {
                String vendorPropertiesPID = props instanceof PropertyService ? ((PropertyService) props).getFactoryPID() : PropertyService.FACTORY_PID;
                className = JDBCDrivers.getDataSourceClassName(vendorPropertiesPID);
                if (className == null) {
                    className = JDBCDrivers.getDataSourceClassName(getClasspath(sharedLib, true));
                    if (className == null) {
                        Set<String> packagesSearched = new LinkedHashSet<String>();
                        SimpleEntry<Integer, String> dsEntry = JDBCDrivers.inferDataSourceClassFromDriver //
                                        (classloader, packagesSearched, JDBCDrivers.DATA_SOURCE);
                        className = dsEntry == null ? null : dsEntry.getValue();
                        if (className == null)
                            throw classNotFound(DataSource.class.getName(), packagesSearched, dataSourceID, null);
                    }
                }
            }

            return create(className, props, dataSourceID);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Create an XADataSource
     * 
     * @param props typed data source properties
     * @param dataSourceID identifier for the data source config 
     * @return the data source
     * @throws SQLException if an error occurs
     */
    public XADataSource createXADataSource(Properties props, String dataSourceID) throws SQLException {
        lock.readLock().lock();
        try {
            if (!isInitialized)
                try {
                    // Switch to write lock for lazy initialization
                    lock.readLock().unlock();
                    lock.writeLock().lock();

                    if (!isInitialized) {
                        if (!loadFromApp())
                            classloader = AdapterUtil.getClassLoaderWithPriv(sharedLib);
                        isInitialized = true;
                    }
                } finally {
                    // Downgrade to read lock for rest of method
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }

            String className = (String) properties.get(XADataSource.class.getName());
            if (className == null) {
                String vendorPropertiesPID = props instanceof PropertyService ? ((PropertyService) props).getFactoryPID() : PropertyService.FACTORY_PID;
                className = JDBCDrivers.getXADataSourceClassName(vendorPropertiesPID);
                if (className == null) {
                    className = JDBCDrivers.getXADataSourceClassName(getClasspath(sharedLib, true));
                    if (className == null) {
                        Set<String> packagesSearched = new LinkedHashSet<String>();
                        SimpleEntry<Integer, String> dsEntry = JDBCDrivers.inferDataSourceClassFromDriver //
                                        (classloader, packagesSearched, JDBCDrivers.XA_DATA_SOURCE);
                        className = dsEntry == null ? null : dsEntry.getValue();
                        if (className == null)
                            throw classNotFound(XADataSource.class.getName(), packagesSearched, dataSourceID, null);
                    }
                }
            }

            return create(className, props, dataSourceID);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Load the Driver instance for the specified URL.
     * 
     * @param url JDBC driver URL.
     * @return the driver
     * @throws Exception if an error occurs
     */
    public Object getDriver(String url, Properties props, String dataSourceID) throws Exception {
        lock.readLock().lock();
        try {
            if (!isInitialized)
                try {
                    // Switch to write lock for lazy initialization
                    lock.readLock().unlock();
                    lock.writeLock().lock();

                    if (!isInitialized) {
                        if (!loadFromApp())
                            classloader = AdapterUtil.getClassLoaderWithPriv(sharedLib);
                        isInitialized = true;
                    }
                } finally {
                    // Downgrade to read lock for rest of method
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }

            final String className = (String) properties.get(Driver.class.getName());
            if (className == null) {
                String vendorPropertiesPID = props instanceof PropertyService ? ((PropertyService) props).getFactoryPID() : PropertyService.FACTORY_PID;
                //if properties.oracle.ucp is configured do not search for driver impls because the customer has indicated
                //they want to use UCP, but this will likely pick up the Oracle driver instead of the UCP driver (since UCP has no Driver interface)
                if("com.ibm.ws.jdbc.dataSource.properties.oracle.ucp".equals(vendorPropertiesPID)) {
                    throw new SQLNonTransientException(AdapterUtil.getNLSMessage("DSRA4015.no.ucp.connection.pool.datasource", dataSourceID, Driver.class.getName()));
                }
            }
            Driver driver = loadDriver(className, url, classloader, props, dataSourceID);
            if (driver == null)
               throw classNotFound(Driver.class.getName(), Collections.singleton("META-INF/services/java.sql.Driver"), dataSourceID, null);
            return driver;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context for this component instance
     */
    protected void deactivate(ComponentContext context) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "deactivate");

        lock.writeLock().lock();
        try {
            if (isInitialized) {
                if (classloader != null) {
                    if (isDerbyEmbedded.get())
                        shutdownDerbyEmbedded();
                    classloader = null;
                }
                for (Iterator<Class<? extends CommonDataSource>> it = introspectedClasses.iterator(); it.hasNext(); it.remove())
                    Introspector.flushFromCaches(it.next());
                isInitialized = false;
            }
        } finally {
            lock.writeLock().unlock();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "deactivate");
    }

    /**
     * Returns the class loader for a jdbcDriver with a libraryRef or nested library.
     * 
     * @return the class loader for a jdbcDriver with a libraryRef or nested library.
     *         Null if uninitialized or if configured to load from the application.
     */
    public ClassLoader getClassLoaderForLibraryRef() {
        lock.readLock().lock();
        try {
            return classloader;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a list of file names for the specified library.
     * 
     * @param sharedLib library
     * @param upperCaseFileNamesOnly indicates whether or not to include file names only (not paths) and to convert the names to all upper case.
     * @return list of file names for the library. If the library is null, returns an empty list.
     */
    public static Collection<String> getClasspath(Library sharedLib, boolean upperCaseFileNamesOnly) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "getClasspath", sharedLib);

        Collection<String> classpath = new LinkedList<String>();
        if (sharedLib != null && sharedLib.getFiles() != null)
            for (File file : sharedLib.getFiles())
                classpath.add(upperCaseFileNamesOnly ? file.getName().toUpperCase() : file.getAbsolutePath());
        if (sharedLib != null && sharedLib.getFilesets() != null)
            for (Fileset fileset : sharedLib.getFilesets())
                for (File file : fileset.getFileset())
                    classpath.add(upperCaseFileNamesOnly ? file.getName().toUpperCase() : file.getAbsolutePath());

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, "getClasspath", classpath);
        return classpath;
    }

    /**
     * Load java.sql.Driver implementations available to the specific class loader.
     * If className is specified, return an instance of that class.
     * Otherwise return the first that accepts the URL.
     *
     * @param className pre-computed Driver implementation class name to load. This will only ever be available on the DataSourceDefinition path.
     * @param url the JDBC driver URL.
     * @param classloader class loader from which to load JDBC drivers. NULL to load from the application's thread context class loader.
     * @return Driver instance that accepts the URL. NULL if no such Driver can be loaded.
     * @throws Exception if an error occurs.
     */
    private Driver loadDriver(final String className, String url, final ClassLoader classloader, Properties props, String dataSourceID) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isDebugEnabled())
            Tr.entry(this, tc, "loadDriver", className, PropertyService.filterURL(url), classloader);
        int index = url.toLowerCase().indexOf("logintimeout");
        if(index != -1) {
            int length = url.length();
            index += 13;  //length of logintimeout=
            //check that the value after logintime= is 0
            if(index < length && !url.substring(index, index+1).equals("0")) {
                throw new SQLNonTransientException(AdapterUtil.getNLSMessage("DSRA4005.invalid.logintimeout", dataSourceID));
            }
            if(index + 1 < length && !Character.isDigit(url.charAt(index+1))) { //check that the value after 0 (if there is one) is not numeric
                throw new SQLNonTransientException(AdapterUtil.getNLSMessage("DSRA4005.invalid.logintimeout", dataSourceID));
            }
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Allowing value of 0 for loginTimeout in URL");
        }
        
        Object loginTimeout = props.get("loginTimeout");
        if(loginTimeout != null && !loginTimeout.toString().equals("0")) {
            throw new SQLNonTransientException(AdapterUtil.getNLSMessage("DSRA4005.invalid.logintimeout", dataSourceID));
        }

        Iterable<Driver> drivers;
        if (className == null) {
            if (classloader == null)
                drivers = ServiceLoader.load(Driver.class); // use thread context class loader of application
            else
                drivers = ServiceLoader.load(Driver.class, classloader);
        } else { // load explicitly specified class
            Driver driver = AccessController.doPrivileged(new PrivilegedExceptionAction<Driver>() {
                public Driver run() throws Exception {
                    ClassLoader loader = classloader == null ? Thread.currentThread().getContextClassLoader() : classloader;
                    Class<Driver> driverClass = (Class<Driver>) loader.loadClass(className);
                    return driverClass.newInstance();
                }
            });
            drivers = Collections.singleton(driver);
        }

        SQLException failure = null;
        for (Driver driver : drivers) {
            boolean acceptsURL;
            try {
                acceptsURL = driver.acceptsURL(url);
            } catch (SQLException x) {
                if (failure == null)
                    failure = x;
                acceptsURL = false;
            }
            if (acceptsURL) {
                if (classloader != null && url.startsWith("jdbc:derby:") && isDerbyEmbedded.compareAndSet(false, true)) {
                    embDerbyRefCount.add(classloader);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "ref count for shutdown", classloader, embDerbyRefCount);
                }

                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "loadDriver", driver);
                return driver;
            } else {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, driver + " does not accept url");
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "loadDriver", failure);
        if (failure == null)
            return null;
        else
            throw failure;
    }

    /**
     * Returns true if configured to load the data source class from the application's thread context class loader.
     * Otherwise, false.
     * 
     * @return true if configured to load the data source class from the application's thread context class loader.
     */
    public boolean loadFromApp() {
        return // TODO sharedLib != null
               Boolean.parseBoolean((String) properties.get("ibm.internal.nonship.function"))
               && "ibm.internal.simulate.no.library.do.not.ship".equals(sharedLib.id());
    }

    /**
     * Notification that a shared library has changed (either in configuration or content).
     * 
     * A service should be registered under this interface with a property of
     * library=id, where id is the library id in config
     */
    @Override
    public void libraryNotification() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "libraryNotification");
        modified(null, true);
    }

    /*
     * Called by Declarative Services to modify service config properties
     */
    protected void modified(ComponentContext context) {
        Dictionary<String, ?> newProperties = context.getProperties();
        modified(newProperties, true);
    }

    /**
     * Clears the configuration of this JDBCDriverService so that it can
     * lazily initialize with the new configuration on next use.
     * 
     * @param newProperties new properties to use. Can be null if there are no changes to existing properties.
     * @param logMessage indicates whether or not to log a message about the update.
     */
    private void modified(Dictionary<String, ?> newProperties, boolean logMessage) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "modified", newProperties);

        boolean replaced = false;
        lock.writeLock().lock();
        try {
            if (isInitialized) {
                if (classloader != null) {
                    if (isDerbyEmbedded.compareAndSet(true, false)) // assume false for any future usage until shown otherwise
                        shutdownDerbyEmbedded();
                    classloader = null;
                }
                for (Iterator<Class<? extends CommonDataSource>> it = introspectedClasses.iterator(); it.hasNext(); it.remove())
                    Introspector.flushFromCaches(it.next());

                replaced = true;
                isInitialized = false;
            }

            if (newProperties != null)
                properties = newProperties;
        } finally {
            lock.writeLock().unlock();
        }

        if (replaced)
            try {
                setChanged();
                notifyObservers();
            } catch (Throwable x) {
                FFDCFilter.processException(x, getClass().getName(), "254", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, x.getMessage(), AdapterUtil.stackTraceToString(x));
            }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "modified");
    }

    /**
     * Declarative services method to set the ConnectorService.
     */
    protected void setConnectorService(ConnectorService svc) {
        connectorSvc = svc;
    }

    /**
     * Handles the setting of any property for which a public single-parameter setter exists on
     * the DataSource and for which the property data type is either a primitive or has a
     * single-parameter String constructor.
     * 
     * @param obj the Object to set the property on.
     * @param pd the PropertyDescriptor describing the property to set.
     * @param value a String representing the new value.
     * @param doTraceValue indicates if the value should be traced.
     * 
     * @throws Exception if an error occurs.
     */
    private static void setProperty(Object obj, PropertyDescriptor pd, String value,
                                   boolean doTraceValue) throws Exception {
        Object param = null;
        String propName = pd.getName();

        if (tc.isDebugEnabled()) {
            if("URL".equals(propName) || "url".equals(propName)) {
                Tr.debug(tc, "set " + propName + " = " + PropertyService.filterURL(value));
            } else {
                Tr.debug(tc, "set " + propName + " = " + (doTraceValue ? value : "******"));
            }
        }

        java.lang.reflect.Method setter = pd.getWriteMethod();

        if (setter == null)
            throw new NoSuchMethodException(AdapterUtil.getNLSMessage("NO_SETTER_METHOD", propName));

        Class<?> paramType = setter.getParameterTypes()[0];

        if (!paramType.isPrimitive()) {
            if (paramType.equals(String.class)) // the most common case: String
                param = value;

            else if (paramType.equals(Properties.class)) // special case: Properties
                param = AdapterUtil.toProperties(value);

            else if (paramType.equals(Character.class)) // special case: Character
                param = Character.valueOf(value.charAt(0));

            else // the generic case: any object with a single parameter String constructor
                param = paramType.getConstructor(String.class).newInstance(value);
        }
        else if (paramType.equals(int.class))
            param = Integer.valueOf(value);
        else if (paramType.equals(long.class))
            param = Long.valueOf(value);
        else if (paramType.equals(boolean.class))
            param = Boolean.valueOf(value);
        else if (paramType.equals(double.class))
            param = Double.valueOf(value);
        else if (paramType.equals(float.class))
            param = Float.valueOf(value);
        else if (paramType.equals(short.class))
            param = Short.valueOf(value);
        else if (paramType.equals(byte.class))
            param = Byte.valueOf(value);
        else if (paramType.equals(char.class))
            param = Character.valueOf(value.charAt(0));

        setter.invoke(obj, new Object[] { param });
    }

    /**
     * Declarative Services method for setting the SharedLibrary service
     * 
     * @param lib the service
     */
    protected void setSharedLib(Library lib) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setSharedLib", lib);
        sharedLib = lib;
    }

    /**
     * Shut down the Derby system if the reference count for the class loader drops to 0.
     */
    private void shutdownDerbyEmbedded() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "shutdownDerbyEmbedded", classloader, embDerbyRefCount);

        // Shut down Derby embedded if the reference count drops to 0
        if (embDerbyRefCount.remove(classloader) && !embDerbyRefCount.contains(classloader))
            try {
                Class<?> EmbDS = AdapterUtil.forNameWithPriv("org.apache.derby.jdbc.EmbeddedDataSource40", true, classloader);
                DataSource ds = (DataSource) EmbDS.newInstance();
                EmbDS.getMethod("setShutdownDatabase", String.class).invoke(ds, "shutdown");
                ds.getConnection().close();
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
     * Declarative services method to unset the ConnectorService.
     */
    protected void unsetConnectorService(ConnectorService svc) {
        connectorSvc = null;
    }

    /**
     * Declarative Services method for unsetting the SharedLibrary service
     * 
     * @param lib the service
     */
    protected void unsetSharedLib(Library lib) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "unsetSharedLib", lib);
        modified(null, false);
    }
    
    /** Decode passwords embedded in an Oracle connectionProperties element
     * @ connProp an Oracle connectionProperties string which may contain encoded passwords
     * @return a connectionProperties string with the passwords decoded
     */
    public static String decodeOracleConnectionPropertiesPwds(String connProp) throws InvalidPasswordDecodingException, UnsupportedCryptoAlgorithmException {
        final String regex = "Password\\s*=\\s*(.*?)\\s*(;|$)";
        
        StringBuffer sb = new StringBuffer();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(connProp);

        while (matcher.find()) {
            //group(0) = "Password = {xor}abcd;" group(1) = "{xor}abcd" group(2) = ";"
            String password = matcher.group(1);
            password = PasswordUtil.getCryptoAlgorithm(password) == null ? password : PasswordUtil.decode(matcher.group(1));
            
            //This appends a replacement for group(0), so we want to just replace group(1) with [decoded password]
            matcher.appendReplacement(sb, matcher.group(0).replace(matcher.group(1), password));
        }
        
        //Append any trailing characters after matches
        //If there were no matches this will just append props
        matcher.appendTail(sb);
        
        return sb.toString(); 
    }
}
