/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc;

import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.cm.AppDefinedResource;
import com.ibm.ws.jca.cm.AppDefinedResourceFactory;
import com.ibm.ws.jca.cm.ConnectionManagerService;
import com.ibm.ws.jca.cm.ConnectorService;
import com.ibm.ws.jdbc.internal.DataSourceDef;
import com.ibm.ws.jdbc.internal.JDBCDriverService;
import com.ibm.ws.jdbc.internal.JDBCDrivers;
import com.ibm.ws.jdbc.internal.PropertyService;
import com.ibm.ws.resource.ResourceFactory;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.DSConfig;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.library.Library;

/**
 * Creates, modifies, and removes data sources that are defined via DataSourceDefinition.
 */
public class DataSourceResourceFactoryBuilder implements ResourceFactoryBuilder {
    /**  */
    private static final String BASE_PROPERTIES_KEY = "properties.0.";

    private static final TraceComponent tc = Tr.register(DataSourceResourceFactoryBuilder.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Name of property used by config service to uniquely identify a component instance.
     */
    private static final String CONFIG_DISPLAY_ID = "config.displayId";

    /**
     * Name of property used by config service to identify where the configuration of a component instance originates.
     */
    private static final String CONFIG_SOURCE = "config.source";

    /**
     * Interface names, including package, for supported data source types. 
     */
    private static final Collection<String> DS_INTERFACE_NAMES = Arrays.asList(XADataSource.class.getName(),
                                                                               ConnectionPoolDataSource.class.getName(),
                                                                               DataSource.class.getName());

    /**
     * Property value that indicates the configuration originated in a configuration file, such as server.xml,
     * rather than being programmatically created via ConfigurationAdmin.
     */
    private static final String FILE = "file";

    /**
     * Unique identifier attribute name.
     */
    private static final String ID = "id";

    /**
     * ConfigAdmin service.
     */
    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>("configAdmin");

    /**
     * Utility that collects various core services needed by connection management and JDBC
     */
    private ConnectorService connectorSvc;

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context context for this component
     */
    protected void activate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(this, tc, "activate", context);
        configAdminRef.activate(context);
    }

    /**
     * Creates a resource factory that creates handles of the type specified
     * by the {@link ResourceFactory#CREATES_OBJECT_CLASS} property.
     * 
     * @param props the resource-specific type information
     * @return the resource factory
     * @throws Exception a resource-specific exception
     */
    public ResourceFactory createResourceFactory(Map<String, Object> props) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "createResourceFactory", PropertyService.hidePasswords(props));

        Hashtable<String, Object> cmSvcProps = new Hashtable<String, Object>();
        Hashtable<String, Object> dsSvcProps = new Hashtable<String, Object>();
        Hashtable<String, Object> driverProps = new Hashtable<String, Object>();
        Map<String, Object> vendorProps = new HashMap<String, Object>();

        // Initially copy everything into vendor properties and expand any variables
        VariableRegistry variableRegistry = connectorSvc.getVariableRegistry();
        for (Map.Entry<String, Object> prop : props.entrySet()) {
            Object value = prop.getValue();
            if (value instanceof String)
                value = variableRegistry.resolveRawString((String) value);
            vendorProps.put(prop.getKey(), value);
        }

        String application = (String) vendorProps.remove(AppDefinedResource.APPLICATION);
        String declaringApplication = (String) vendorProps.remove(DataSourceService.DECLARING_APPLICATION);
        String module = (String) vendorProps.remove(AppDefinedResource.MODULE);
        String component = (String) vendorProps.remove(AppDefinedResource.COMPONENT);
        String jndiName = (String) vendorProps.remove(ResourceFactory.JNDI_NAME);

        String dataSourceID = getDataSourceID(application, module, component, jndiName);
        String conManagerID = dataSourceID + '/' + ConnectionManagerService.CONNECTION_MANAGER;
        String jdbcDriverID = dataSourceID + '/' + JDBCDriverService.JDBC_DRIVER;

        String conManagerFilter = FilterUtils.createPropertyFilter(ID, conManagerID);
        String jdbcDriverFilter = FilterUtils.createPropertyFilter(ID, jdbcDriverID);

        StringBuilder filter = new StringBuilder(FilterUtils.createPropertyFilter(ID, dataSourceID));
        filter.insert(filter.length() - 1, '*');
        // Fail if server.xml is already using the id
        if (!removeExistingConfigurations(filter.toString()))
            throw new IllegalArgumentException(dataSourceID); // internal error, shouldn't ever have been permitted in server.xml

        cmSvcProps.put(ID, conManagerID);
        cmSvcProps.put(CONFIG_DISPLAY_ID, conManagerID);
        driverProps.put(ID, jdbcDriverID);
        driverProps.put(CONFIG_DISPLAY_ID, jdbcDriverID);
        dsSvcProps.put(ID, dataSourceID);
        dsSvcProps.put(CONFIG_DISPLAY_ID, dataSourceID);
        dsSvcProps.put(ResourceFactory.JNDI_NAME, jndiName);
        // Use the unique identifier because jndiName is not always unique for app-defined data sources
        dsSvcProps.put(DataSourceService.UNIQUE_JNDI_NAME, dataSourceID);
        dsSvcProps.put(DataSourceService.TARGET_CONNECTION_MANAGER, conManagerFilter);
        dsSvcProps.put(DataSourceService.TARGET_JDBC_DRIVER, jdbcDriverFilter);
        dsSvcProps.put("connectionManager.cardinality.minimum", 1); // require exactly 1 connection manager (the one specified by the target)

        String containerAuthDataRef = (String) vendorProps.remove(DSConfig.CONTAINER_AUTH_DATA_REF);
        if (containerAuthDataRef != null) {
            String authDataFilter = FilterUtils.createPropertyFilter(ID, containerAuthDataRef);
            dsSvcProps.put(DataSourceService.TARGET_CONTAINER_AUTH_DATA, authDataFilter);
            dsSvcProps.put("containerAuthData.cardinality.minimum", 1);
        }

        String recoveryAuthDataRef = (String) vendorProps.remove(DSConfig.RECOVERY_AUTH_DATA_REF);
        if (recoveryAuthDataRef != null) {
            String authDataFilter = FilterUtils.createPropertyFilter(ID, recoveryAuthDataRef);
            dsSvcProps.put(DataSourceService.TARGET_RECOVERY_AUTH_DATA, authDataFilter);
            dsSvcProps.put("recoveryAuthData.cardinality.minimum", 1);
        }

        if (application != null) {
            dsSvcProps.put(AppDefinedResource.APPLICATION, application);
            if (module != null) {
                dsSvcProps.put(AppDefinedResource.MODULE, module);
                if (component != null)
                    dsSvcProps.put(AppDefinedResource.COMPONENT, component);
            }
        }

        Object value;
        for (String name : ConnectionManagerService.CONNECTION_MANAGER_PROPS)
            if ((value = vendorProps.remove(name)) != null)
                cmSvcProps.put(name, value);

        for (String name : DSConfig.DATA_SOURCE_PROPS)
            if ((value = vendorProps.remove(name)) != null)
                dsSvcProps.put(name, value);

        BundleContext bundleContext = DataSourceService.priv.getBundleContext(FrameworkUtil.getBundle(DataSourceResourceFactoryBuilder.class));

        // className
        String className = (String) vendorProps.remove(DataSourceDef.className.name());

        // url should be used if properties like serverName/portNumber/databaseName are unspecified
        String url = (String) vendorProps.remove(DataSourceDef.url.name());
        if (vendorProps.containsKey(DataSourceDef.databaseName.name()) || vendorProps.containsKey(DataSourceDef.portNumber.name()))
            url = null;

        // libraryRef - scan shared libraries from the application
        className = updateWithLibraries(bundleContext, application, declaringApplication, className, url, driverProps, dsSvcProps);

        // initialPoolSize > 0 not supported
        value = vendorProps.remove(DataSourceDef.initialPoolSize.name());
        if (value != null && ((Integer) value) > 0)
            ConnectorService.logMessage(Level.INFO, "IGNORE_FEATURE_J2CA0240", DataSourceDef.initialPoolSize.name(), jndiName);

        // maxStatements per datasource --> statementCacheSize per connection
        value = vendorProps.remove(DataSourceDef.maxStatements.name());
        if (value != null) {
            Integer maxPoolSize = (Integer) cmSvcProps.get(DataSourceDef.maxPoolSize.name());
            int stmtCacheSize = maxPoolSize == null || maxPoolSize <= 0 ? 0 : ((Integer) value / maxPoolSize);
            dsSvcProps.put(DSConfig.STATEMENT_CACHE_SIZE, stmtCacheSize);
        }

        // serverName defaults to "localhost" (unless it's Derby Embedded, which doesn't have serverName)
        if (!vendorProps.containsKey(DataSourceDef.serverName.name()))
            if (url != null)
                vendorProps.put("URL", url);
            else if (!className.startsWith("org.apache.derby.jdbc.Embedded"))
                vendorProps.put(DataSourceDef.serverName.name(), "localhost");

        PropertyService.parseDurationProperties(vendorProps, className, connectorSvc);
        PropertyService.parsePasswordProperties(vendorProps);

        // Add vendor properties to the data source properties as flattened config
        for (Map.Entry<String, Object> entry : vendorProps.entrySet())
            dsSvcProps.put(BASE_PROPERTIES_KEY + entry.getKey(), entry.getValue());

        StringBuilder dsFilter = new StringBuilder(200);
        dsFilter.append("(&").append(FilterUtils.createPropertyFilter(ID, dataSourceID));
        dsFilter.append(FilterUtils.createPropertyFilter(Constants.OBJECTCLASS, DataSourceService.class.getName())).append(")");

        ResourceFactory factory = new AppDefinedResourceFactory(this, bundleContext, dataSourceID, dsFilter.toString(), declaringApplication);
        ConfigurationAdmin configAdmin = null;
        try {
            String bundleLocation = bundleContext.getBundle().getLocation();
            String jcaBundleLocation = DataSourceService.priv.getBundleContext(FrameworkUtil.getBundle(ConnectorService.class)).getBundle().getLocation();

            configAdmin = configAdminRef.getService();

            Configuration conMgrConfig = configAdmin.createFactoryConfiguration(ConnectionManagerService.FACTORY_PID, jcaBundleLocation);
            conMgrConfig.update(cmSvcProps);
            dsSvcProps.put(DSConfig.CONNECTION_MANAGER_REF, new String[] { conMgrConfig.getPid() });

            Configuration driverConfig = configAdmin.createFactoryConfiguration(JDBCDriverService.FACTORY_PID, bundleLocation);
            driverConfig.update(driverProps);
            dsSvcProps.put(DSConfig.JDBC_DRIVER_REF, new String[] { driverConfig.getPid() });

            Configuration dsSvcConfig = configAdmin.createFactoryConfiguration(DataSourceService.FACTORY_PID, bundleLocation);
            dsSvcConfig.update(dsSvcProps);
        } catch (Exception x) {
            factory.destroy();
            throw x;
        } catch (Error x) {
            factory.destroy();
            throw x;
        } finally {
            if(configAdmin != null)
                bundleContext.ungetService(configAdminRef.getReference());
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, "createResourceFactory", factory);
        return factory;
    }

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context context for this component
     */
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(this, tc, "deactivate", context);
        configAdminRef.deactivate(context);
    }

    /**
     * Utility method that creates a unique identifier for an application defined data source.
     * For example,
     * application[MyApp]/module[MyModule]/dataSource[java:module/env/jdbc/ds1]
     * 
     * @param application application name if data source is in java:app, java:module, or java:comp. Otherwise null.
     * @param module module name if data source is in java:module or java:comp. Otherwise null.
     * @param component component name if data source is in java:comp and isn't in web container. Otherwise null.
     * @param jndiName configured JNDI name for the data source. For example, java:module/env/jdbc/ds1
     * @return the unique identifier
     */
    private static final String getDataSourceID(String application, String module, String component, String jndiName) {
        StringBuilder sb = new StringBuilder(jndiName.length() + 80);
        if (application != null) {
            sb.append(AppDefinedResource.APPLICATION).append('[').append(application).append(']').append('/');
            if (module != null) {
                sb.append(AppDefinedResource.MODULE).append('[').append(module).append(']').append('/');
                if (component != null)
                    sb.append(AppDefinedResource.COMPONENT).append('[').append(component).append(']').append('/');
            }
        }
        return sb.append(DataSourceService.DATASOURCE).append('[').append(jndiName).append(']').toString();
    }

    /**
     * This method looks for existing configurations and removes them unless they came
     * from server.xml
     * 
     * We can distinguish by checking for the presence of a property named "config.source"
     * which is set to "file" when the configuration originates from server.xml.
     * 
     * @param filter used to find existing configurations.
     * @return true if all configurations were removed or did not exist to begin with, otherwise false.
     * @throws Exception if an error occurs.
     */
    public final boolean removeExistingConfigurations(String filter) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        BundleContext bundleContext = DataSourceService.priv.getBundleContext(FrameworkUtil.getBundle(DataSourceResourceFactoryBuilder.class));
        ConfigurationAdmin configAdmin = configAdminRef.getService();
        try {
            Configuration[] existingConfigurations = configAdmin.listConfigurations(filter);
            if (existingConfigurations != null)
                for (Configuration config : existingConfigurations) {
                    Dictionary<?, ?> cfgProps = config.getProperties();
                    // Don't remove configuration that came from server.xml
                    if (cfgProps != null && FILE.equals(cfgProps.get(CONFIG_SOURCE))) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(tc, "configuration found in server.xml: ", config.getPid());
                        return false;
                    } else {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(tc, "removing", config.getPid());
                        config.delete();
                    }
                }
        } finally {
            bundleContext.ungetService(configAdminRef.getReference());
        }

        return true;
    }

    /**
     * Declarative Services method for setting the ConfigurationAdmin service reference.
     * 
     * @param ref reference to the service
     */
    protected void setConfigAdmin(ServiceReference<ConfigurationAdmin> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setConfigAdmin", ref);
        configAdminRef.setReference(ref);
    }

    /**
     * Declarative services method to set the ConnectorService.
     */
    protected void setConnectorService(ConnectorService svc) {
        connectorSvc = svc;
    }

    /**
     * Declarative Services method for unsetting the ConfigurationAdmin service reference.
     * 
     * @param ref reference to the service
     */
    protected void unsetConfigAdmin(ServiceReference<ConfigurationAdmin> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unsetConfigAdmin", ref);
        configAdminRef.unsetReference(ref);
    }

    /**
     * Declarative services method to unset the ConnectorService.
     */
    protected void unsetConnectorService(ConnectorService svc) {
        connectorSvc = null;
    }

    /**
     * Scan the shared libraries for the application to determine which library provides the data source class.
     * Update the jdbcDriver and dataSource configuration accordingly. For example,
     * <jdbcDriver libraryRef="{service.pid for libraryRef}" library="{libraryRef}" sharedLib.target="(service.pid={libraryRef})" {type}="{className}" />
     * <dataSource type="{type}"/>
     * 
     * @param applicationName name of the application with the DataSourceDefinition. Is set to null when java:global is specified
     * @param declaringApplication name of the application with the DataSourceDefinition.
     * @param className data source or driver implementation class name, including package. NULL means infer the implementation class.
     * @param url URL with which the Driver will connect to the database. NULL if data source should be used instead of Driver.
     * @param driverProps properties for the jdbcDriver.
     * @param dsSvcProps properties for the dataSource.
     * @return the supplied className unless NULL, in which case the inferred class name is returned. 
     * @throws Exception if an error occurs or unable to locate a library containing the data source class or unable to infer a data source/driver class.
     */
    private final String updateWithLibraries(BundleContext bundleContext,
                                             String applicationName,
                                             String declaringApplication,
                                             String className,
                                             String url,
                                             Hashtable<String, Object> driverProps,
                                             Hashtable<String, Object> dsSvcProps) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "updateWithLibraries", applicationName, className);

        ConfigurationAdmin configAdmin = configAdminRef.getServiceWithException();

        // Find the application
        Configuration[] classloaderConfigs = null;
        ServiceReference<?>[] refs = DataSourceService.priv.getServiceReferences(bundleContext,"com.ibm.wsspi.application.Application", FilterUtils.createPropertyFilter("name", declaringApplication));
        if (refs != null && refs.length > 0) {
            ServiceReference<?> appRef = refs[0];
            String parentPid = (String) appRef.getProperty(Constants.SERVICE_PID);
            String sourcePid = (String) appRef.getProperty("ibm.extends.source.pid");
            if (sourcePid != null) {
                parentPid = sourcePid;
            }
            // Find the classloaders for the application
            StringBuilder classloaderFilter = new StringBuilder(200);
            classloaderFilter.append("(&");
            classloaderFilter.append(FilterUtils.createPropertyFilter("service.factoryPid", "com.ibm.ws.classloading.classloader"));
            classloaderFilter.append(FilterUtils.createPropertyFilter("config.parentPID", parentPid));
            classloaderFilter.append(')');
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "filter for classloaders", classloaderFilter);
            if (classloaderFilter.length() > 3)
                classloaderConfigs = configAdmin.listConfigurations(classloaderFilter.toString());
        }

        // Find the shared libraries for the classloaders
        StringBuilder commonLibraryFilter = new StringBuilder(500);
        StringBuilder libraryFilter = new StringBuilder(500);
        commonLibraryFilter.append("(|");
        libraryFilter.append("(|");
        
        if (classloaderConfigs != null) {
            for (Configuration classloaderConfig : classloaderConfigs) {
                Dictionary<?, ?> classloaderProps = classloaderConfig.getProperties();
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "classloader", classloaderProps);

                // Do not check for privateLibraryRef for java:global data source definitions, applicationName is null when java:global is used
                if (applicationName != null) {
                    Object privateLibraryRef = classloaderProps.get("privateLibraryRef");
                    if (privateLibraryRef != null && privateLibraryRef instanceof String[])
                        for (String pid : (String[]) privateLibraryRef)
                            libraryFilter.append(FilterUtils.createPropertyFilter(Constants.SERVICE_PID, pid));
                }

                Object commonLibraryRef = classloaderProps.get("commonLibraryRef");
                if (commonLibraryRef != null && commonLibraryRef instanceof String[])
                    for (String pid : (String[]) commonLibraryRef)
                        commonLibraryFilter.append(FilterUtils.createPropertyFilter(Constants.SERVICE_PID, pid));
            }
        }

        commonLibraryFilter.append(')');
        libraryFilter.append(')');
        if (trace && tc.isDebugEnabled())
            Tr.debug(tc, "filters for libraries", commonLibraryFilter, libraryFilter);
        // Give commonLibraryRef higher priority than libraryRef by adding it first
        List<ServiceReference<Library>> libraryRefs = new LinkedList<ServiceReference<Library>>();
        if (commonLibraryFilter.length() > 3)
            libraryRefs.addAll(DataSourceService.priv.getServiceReferences(bundleContext,Library.class, commonLibraryFilter.toString()));
        if (libraryFilter.length() > 3)
            libraryRefs.addAll(DataSourceService.priv.getServiceReferences(bundleContext,Library.class, libraryFilter.toString()));
        
        // If no commonLibraryRef or privateLibraryRef was specified, use the global shared lib
        if(libraryRefs.size() == 0){
            if(trace && tc.isDebugEnabled())
                Tr.debug(tc, "No direct library refs found on datasource, using global shared lib");
            StringBuilder globalLibFilter = new StringBuilder("(&");
            globalLibFilter.append(FilterUtils.createPropertyFilter("service.factoryPid", "com.ibm.ws.classloading.sharedlibrary"));
            globalLibFilter.append(FilterUtils.createPropertyFilter("id", "global"));
            globalLibFilter.append(")");
            Collection<ServiceReference<Library>> globalLib = DataSourceService.priv.getServiceReferences(bundleContext, Library.class, globalLibFilter.toString());
            libraryRefs.addAll(globalLib);
        }

        if (trace && tc.isDebugEnabled())
            Tr.debug(tc, "libraries", libraryRefs.toArray());

        // this structure is populated only if className and url properties are absent.
        String[][] dsClassInfo = new String[JDBCDrivers.NUM_DATA_SOURCE_INTERFACES][2];
        final int CLASS_NAME = 0, LIBRARY_PID = 1; // indices for the second dimension

        // determine if we need to search for an implementation class, and if so, of which type(s)
        int[] dsTypeSearchOrder;
        Set<String> searchedLibraryFiles = new TreeSet<String>();
        Set<String> searchedPackages;
        
        boolean hasImplClassName = className != null && className.length() > 0;
        if (hasImplClassName) {
            if (XADataSource.class.getName().equals(className)) {
                dsTypeSearchOrder = new int[] { JDBCDrivers.XA_DATA_SOURCE };
                searchedPackages = new TreeSet<String>();
                hasImplClassName = false;
            } else if (ConnectionPoolDataSource.class.getName().equals(className)) {
                dsTypeSearchOrder = new int[] { JDBCDrivers.CONNECTION_POOL_DATA_SOURCE };
                searchedPackages = new TreeSet<String>();
                hasImplClassName = false;
            } else if (DataSource.class.getName().equals(className)) {
                dsTypeSearchOrder = new int[] { JDBCDrivers.DATA_SOURCE };
                searchedPackages = new TreeSet<String>();
                hasImplClassName = false;
            } else if (Driver.class.getName().equals(className)) {
                dsTypeSearchOrder = null;
                searchedPackages = Collections.singleton("META-INF/services/java.sql.Driver");
                hasImplClassName = false;
            } else {
                dsTypeSearchOrder = null; // if we know the impl class, there is no need to search for one
                searchedPackages = null;
            }
        } else if (url == null) {
            dsTypeSearchOrder = new int[] { JDBCDrivers.XA_DATA_SOURCE, JDBCDrivers.CONNECTION_POOL_DATA_SOURCE, JDBCDrivers.DATA_SOURCE };
            searchedPackages = new TreeSet<String>();
        } else { // assume java.sql.Driver due to presence of URL
            dsTypeSearchOrder = null;
            searchedPackages = Collections.singleton("META-INF/services/java.sql.Driver");
        }

        // Determine which shared library can load className
        for (ServiceReference<Library> libraryRef : libraryRefs) {
            Library library = DataSourceService.priv.getService(bundleContext,libraryRef);
            try {
                String libraryPid = (String) libraryRef.getProperty(Constants.SERVICE_PID);
                if (library == null) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "library not found", libraryPid);
                } else {
                    try {
                        ClassLoader loader = AdapterUtil.getClassLoaderWithPriv(library);

                        String type = null;
                        if (hasImplClassName) {
                            Class<?> cl = DataSourceService.priv.loadClass(loader, className);
                            type = XADataSource.class.isAssignableFrom(cl) ? XADataSource.class.getName()
                                 : ConnectionPoolDataSource.class.isAssignableFrom(cl) ? ConnectionPoolDataSource.class.getName()
                                 : DataSource.class.isAssignableFrom(cl) ? DataSource.class.getName()
                                 : Driver.class.getName();
                        } else {
                            searchedLibraryFiles.addAll(JDBCDriverService.getClasspath(library, false));
                            if (dsTypeSearchOrder == null) {
                                type = Driver.class.getName();
                                for (Iterator<Driver> it = ServiceLoader.load(Driver.class, loader).iterator(); it.hasNext(); ) {
                                    Driver driver = it.next();
                                    if (trace && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "trying driver", driver);
                                    try {
                                        if (driver.acceptsURL(url)) {
                                            if (trace && tc.isDebugEnabled())
                                                Tr.debug(this, tc, driver + " accepts " + PropertyService.filterURL(url));
                                            className = driver.getClass().getName();
                                            break;
                                        } else {
                                            if (trace && tc.isDebugEnabled())
                                                Tr.debug(this, tc, driver + " does not accept " + PropertyService.filterURL(url));
                                        }
                                    } catch (SQLException x) {
                                        if (trace && tc.isDebugEnabled())
                                            Tr.debug(this, tc, driver + " does not accept " + PropertyService.filterURL(url), x);
                                    }
                                }
                            } else {
                                SimpleEntry<Integer, String> dsEntry = JDBCDrivers.inferDataSourceClassFromDriver(loader,
                                                                                                                  searchedPackages,
                                                                                                                  dsTypeSearchOrder);
                                if (dsEntry != null) {
                                    int dsType = dsEntry.getKey();
                                    if (dsClassInfo[dsType][CLASS_NAME] == null) {
                                        dsClassInfo[dsType][CLASS_NAME] = dsEntry.getValue();
                                        dsClassInfo[dsType][LIBRARY_PID] = libraryPid;
                                    }
                                }
                            }
                        }

                        if (className != null && type != null) {
                            driverProps.put(JDBCDriverService.LIBRARY_REF, new String[] { libraryPid });
                            driverProps.put(JDBCDriverService.TARGET_LIBRARY, FilterUtils.createPropertyFilter("service.pid", libraryPid));
                            driverProps.put(type, className);
                            dsSvcProps.put(DSConfig.TYPE, type);

                            if (trace && tc.isEntryEnabled())
                                Tr.exit(tc, "updateWithLibraries", driverProps);
                            return className;
                        }
                    } catch (ClassNotFoundException x) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(tc, className + " not found in", libraryPid);
                        searchedLibraryFiles.addAll(JDBCDriverService.getClasspath(library, false));
                    } catch (Exception x) {
                        FFDCFilter.processException(x, DataSourceResourceFactoryBuilder.class.getName(), "444", new Object[] { libraryRef });
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(tc, libraryRef.toString(), x);
                        searchedLibraryFiles.addAll(JDBCDriverService.getClasspath(library, false));
                        // Continue to try other libraryRefs
                    }
                }
            } finally {
                bundleContext.ungetService(libraryRef);
            }
        }

        // Inferred data source class name when className and url properties are absent 
        for (int dsType : new int[] { JDBCDrivers.XA_DATA_SOURCE, JDBCDrivers.CONNECTION_POOL_DATA_SOURCE, JDBCDrivers.DATA_SOURCE })
            if (dsClassInfo[dsType][CLASS_NAME] != null) {
                String type = dsType == JDBCDrivers.XA_DATA_SOURCE ? XADataSource.class.getName()
                            : dsType == JDBCDrivers.CONNECTION_POOL_DATA_SOURCE ? ConnectionPoolDataSource.class.getName()
                            : DataSource.class.getName();
                driverProps.put(JDBCDriverService.LIBRARY_REF, new String[] { dsClassInfo[dsType][LIBRARY_PID] });
                driverProps.put(JDBCDriverService.TARGET_LIBRARY, FilterUtils.createPropertyFilter("service.pid", dsClassInfo[dsType][LIBRARY_PID]));
                driverProps.put(type, dsClassInfo[dsType][CLASS_NAME]);
                dsSvcProps.put(DSConfig.TYPE, type);

                if (trace && tc.isEntryEnabled())
                    Tr.exit(tc, "updateWithLibraries", driverProps);
                return dsClassInfo[dsType][CLASS_NAME];
            }

        String message;
        if (searchedPackages == null) {
            // className couldn't be found in any of the shared libraries
            message = ConnectorService.getMessage("MISSING_LIBRARY_J2CA8022", declaringApplication, className, DataSourceService.DATASOURCE,
                                                  dsSvcProps.get(DataSourceService.JNDI_NAME));
        } else {
            List<String> types = dsTypeSearchOrder == null ? Collections.singletonList("java.sql.Driver"): new ArrayList<String>(dsTypeSearchOrder.length);
            if (dsTypeSearchOrder != null)
                for (int dsType : dsTypeSearchOrder)
                    switch (dsType) {
                        case JDBCDrivers.DATA_SOURCE: types.add("javax.sql.DataSource"); break;
                        case JDBCDrivers.CONNECTION_POOL_DATA_SOURCE : types.add("javax.sql.ConnectionPoolDataSource"); break;
                        case JDBCDrivers.XA_DATA_SOURCE : types.add("javax.sql.XADataSource"); break;
                    }
            message = AdapterUtil.getNLSMessage("DSRA4001.no.suitable.driver.nested", types, dsSvcProps.get(DataSourceService.JNDI_NAME),
                                                searchedLibraryFiles, searchedPackages);
        }
        SQLNonTransientException x = new SQLNonTransientException(message);

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, "updateWithLibraries", x);
        throw x;
    }
}