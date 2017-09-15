/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.cache.DistributedObjectCache;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.CacheConfig.ExternalCacheGroup;
import com.ibm.ws.cache.CacheConfig.ExternalCacheGroupMember;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.intf.ObjectCacheUnit;
import com.ibm.ws.cache.intf.ServletCacheUnit;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.cache.CacheProvider;
import com.ibm.wsspi.cache.DistributedObjectCacheFactory;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * All the servlet related initialization is now done in ServletCacheServiceImpl. All object cache initialization is now
 * done in ObjectCacheServiceImpl. This class holds the CacheConfig's for ALL the cache instances. Registers the
 * Dynacache mbean. Provides helpers for Sevlet and Object cache services to add their configs via the
 * addCacheInstanceConfig* methods. Sets up DRS for a particular Cache.
 */

@Component(service = { CacheService.class, ResourceFactory.class, ServletContainerInitializer.class }, configurationPid = "com.ibm.ws.cache",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = {
                       ResourceFactory.CREATES_OBJECT_CLASS + "=com.ibm.websphere.cache.DistributedObjectCache", "service.vendor=IBM" })
public class CacheServiceImpl implements CacheService, ResourceFactory, ServletContextListener, ServletContainerInitializer {
    /**  */
    private static final String DISK_CACHE_ALIAS = "diskCache";

    private static final String CLASS_NAME = CacheServiceImpl.class.getName();

    private static final String DEFAULT_DISTRIBUTED_MAP_ID = "defaultCache";

    private static TraceComponent tc = Tr.register(CacheServiceImpl.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    public static final String FACTORY_PID = "com.ibm.ws.cache";
    private static final String PROPS_JNDI_NAME = "jndiName";
    private static final String PROPS_GENERATED = "generated";
    private static final String PROPS_CACHE_PROVIDER_NAME = "cacheProviderName";

    private static CacheConfig commonCacheConfig = null;
    private static ConcurrentMap<String, CacheConfig> cacheConfigs = new ConcurrentHashMap<String, CacheConfig>();

    public static Map<String, CacheConfig> getCacheConfigs() {
        return cacheConfigs;
    }

    // Inject various Liberty services which are needed
    private final AtomicReference<ConfigurationAdmin> configAdminRef = new AtomicReference<ConfigurationAdmin>(null);
    private final AtomicReference<Scheduler> schedulerRef = new AtomicReference<Scheduler>(null);
    private final AtomicReference<Library> sharedLibRef = new AtomicReference<Library>();

    private String cacheName = null;
    private CacheConfig config;
    private CacheProvider cacheProvider;

    // Object cache variables
    public static final String CACHE_INSTANCES_PROPERTIES = "cacheinstances.properties";
    public static final String DISTRIBUTED_MAP_PROPERTIES = "distributedmap.properties";
    public static final String WEB_INF_CACHE_INSTANCES_PROPERTIES = "WEB-INF/cacheinstances.properties";
    public static final String WEB_INF_DISTRIBUTED_MAP_PROPERTIES = "WEB-INF/distributedmap.properties";

    private final Set<URL> processedUrls = new CopyOnWriteArraySet<URL>();
    private static ObjectCacheUnit objectCacheUnit = new ObjectCacheUnitImpl();
    static {
        ServerCache.cacheUnit.setObjectCacheUnit(objectCacheUnit);
    }

    private final AtomicReference<VariableRegistry> variableRegistryRef = new AtomicReference<VariableRegistry>(null);

    // --------------------------------------------------------------
    // The passed config is a base cache config object from WAS ;L runtime
    // --------------------------------------------------------------
    @Activate
    protected void start(ComponentContext context, Map<String, Object> properties) {

        final String methodName = "start()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, methodName, context, properties);

        try {
            // need to set ref on ServerCache class before DistributedObjectCacheService can operate
            ServerCache.setCacheServiceEarly(this);
            ServerCache.coreCacheEnabled = true;

            config = parsePropertiesFromOSGiConfigAdmin(properties);
            addCacheInstanceConfig(config, false);
            CacheProviderLoaderImpl.getInstance().addCacheProvider(this);
            if (config.createCacheAtServerStartup) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Creating cache instance \"" + config.cacheName + "\" during startup because the cache is configured to.");
                }
                ServerCache.createCache(config.cacheName, config);
            }

        } catch (Exception e) {
            FFDCFilter.processException(e, CLASS_NAME, "start", this);
            Tr.error(tc, "DYNA1062E", new Object[] { e.getCause() });
        }

        try {
            // ----------------------------------------------
            // Wrap passed config object for our use - Cache.DEFAULT_DISTRIBUTED_MAP_NAME ( default )
            // ----------------------------------------------
            ServerCache.objectCacheEnabled = true;

            // ----------------------------------------
            // load CacheConfig from all distributedmap.properties and cacheinstances.properties files
            // ----------------------------------------
            initializeConfigPropertiesFiles(getClass().getClassLoader(), DISTRIBUTED_MAP_PROPERTIES);
            initializeConfigPropertiesFiles(getClass().getClassLoader(), CACHE_INSTANCES_PROPERTIES);

            // ----------------------------------------
            // Create object cache instance if createCacheAtServerStartup in the CacheConfig is true
            // ----------------------------------------
            ArrayList instanceList = getObjectCacheInstanceNames();
            Iterator it = instanceList.iterator();
            while (it.hasNext()) {
                String instanceName = (String) it.next();
                CacheConfig cacheConfig = getCacheInstanceConfig(instanceName);
                if (cacheConfig != null && cacheConfig.isCreateCacheAtServerStartup()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Create object cache instance \"" + instanceName + "\" during startup because the cache is configured to.");
                    }
                    ServerCache.createCache(cacheConfig.getCacheName(), cacheConfig);
                }
            }
            DistributedObjectCacheFactory.setCacheService(this);

            // ----------------------------------------
            // DYNA1056I=DYNA1056I: WebSphere Dynamic Cache (object cache) initialized successfully.
            // DYNA1056I.explanation=This message indicates that the WebSphere Dynamic Cache (object cache) was
            // successfully initialized.
            // DYNA1056I.useraction=None.
            // ----------------------------------------
            Tr.info(tc, "DYNA1056I");

        } catch (IllegalStateException ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ObjectCacheServiceImpl.start", "232", this);
            Tr.error(tc, "dynacache.cacheInitFailed", new Object[] { config.getServerServerName(), ex.getMessage() });
        } catch (Throwable ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ObjectCacheServiceImpl.start", "237", this);
            Tr.error(tc, "dynacache.cacheInitFailed", new Object[] { config.getServerServerName(), ex.getMessage() });
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName + " objectCacheEnabled=" + ServerCache.objectCacheEnabled);

    }

    /*
     * Create a Cache Configuration based on the server.xml configuration received
     */
    public CacheConfig parsePropertiesFromOSGiConfigAdmin(Map<String, Object> properties) {

        CacheConfig config = new CacheConfig(properties);

        // Basic memory cache configuration
        String cacheName = (String) properties.get("id");
        if (cacheName.equals(DEFAULT_DISTRIBUTED_MAP_ID)) {
            cacheName = DCacheBase.DEFAULT_DISTRIBUTED_MAP_NAME;
        }
        config.cacheName = cacheName;
        this.cacheName = config.cacheName;

        String jndiName = (String) properties.get(PROPS_JNDI_NAME);
        config.jndiName = jndiName;

        Integer memorySizeInEntries = (Integer) properties.get("memorySizeInEntries");
        if (null != memorySizeInEntries) {
            config.cacheSize = memorySizeInEntries;
        }

        Integer memorySizeInMB = (Integer) properties.get("memorySizeInMB");
        if (null != memorySizeInMB) {
            config.memoryCacheSizeInMB = memorySizeInMB;
        }

        Integer highThreshold = (Integer) properties.get("highThreshold");
        if (null != highThreshold) {
            config.memoryCacheHighThreshold = highThreshold;
        }

        Integer lowThreshold = (Integer) properties.get("lowThreshold");
        if (null != lowThreshold) {
            config.memoryCacheHighThreshold = lowThreshold;
        }

        String cacheProviderName = (String) properties.get("cacheProviderName");
        if (cacheProviderName != null) {
            config.cacheProviderName = cacheProviderName;
        }

        String libraryRef = (String) properties.get("libraryRef");
        config.libraryRef = libraryRef;

        parseDiskConfiguration(properties, config);
        parseExternalCacheGroupConfiguration(properties, config);

        return config;

    }

    private void parseExternalCacheGroupConfiguration(Map<String, Object> properties, CacheConfig config) {

        Dictionary<String, Object> cacheGroupProperties = null;
        String[] cacheGroupPIDs = (String[]) properties.get("cacheGroup");

        if (null != cacheGroupPIDs) {
            for (String cacheGroupPID : cacheGroupPIDs) {
                Configuration cacheGroupMetaTypeConfig;
                ExternalCacheGroup ecg = new CacheConfig.ExternalCacheGroup();

                try {
                    cacheGroupMetaTypeConfig = configAdminRef.get().getConfiguration(cacheGroupPID);
                    cacheGroupProperties = cacheGroupMetaTypeConfig.getProperties();
                    if (null != cacheGroupProperties) {
                        if (cacheGroupProperties.get("member") instanceof String[]) {
                            String[] pids = (String[]) cacheGroupProperties.get("member");
                            if (null != pids) {
                                for (String pid : pids) {
                                    try {
                                        Configuration memberConfig = configAdminRef.get().getConfiguration(pid);
                                        Dictionary<String, Object> memberProperties = memberConfig.getProperties();

                                        ExternalCacheGroupMember ecgm = new CacheConfig.ExternalCacheGroupMember();
                                        String host = (String) memberProperties.get("host");
                                        Integer port = (Integer) memberProperties.get("port");
                                        String[] adapterBeanName = (String[]) memberProperties.get("adapterBeanName");

                                        ecgm.address = host + ":" + port;
                                        ecgm.beanName = adapterBeanName[0];
                                        ecg.members.add(ecgm);
                                    } catch (IOException e) {
                                    }
                                }
                            }
                        }

                        Enumeration<String> keysEnum = cacheGroupProperties.keys();
                        while (keysEnum.hasMoreElements()) {
                            String key = keysEnum.nextElement();
                            Object value = cacheGroupProperties.get(key);
                            if (key.equals("name")) {
                                ecg.name = (String) value;
                            }
                        }
                    }
                } catch (IOException e) {
                }
                config.externalGroups.add(ecg);
            }
        }

    }

    private void parseDiskConfiguration(Map<String, Object> properties, CacheConfig config) {
        Dictionary<String, Object> diskProperties = null;
        String[] diskPIDs = (String[]) properties.get(DISK_CACHE_ALIAS);
        if (null != diskPIDs) {
            for (String pid : diskPIDs) {
                try {
                    config.enableDiskOffload = true;
                    Configuration diskMetaTypeConfig = configAdminRef.get().getConfiguration(pid);
                    diskProperties = diskMetaTypeConfig.getProperties();
                    if (null != diskProperties) {
                        Enumeration<String> keysEnum = diskProperties.keys();
                        while (keysEnum.hasMoreElements()) {
                            String key = keysEnum.nextElement();
                            Object value = diskProperties.get(key);
                            if (key.equals("sizeInEntries")) {
                                if (null != value)
                                    config.diskCacheSize = (Integer) value;
                            }
                            if (key.equals("sizeInGB")) {
                                if (null != value)
                                    config.diskCacheSizeInGB = (Integer) value;
                            }
                            if (key.equals("evictionPolicy")) {
                                int evictionPolicy;
                                if (value.equals("RANDOM")) {
                                    evictionPolicy = 1;
                                } else if (value.equals("SIZE")) {
                                    evictionPolicy = 2;
                                } else {
                                    evictionPolicy = 0;
                                }
                                config.diskCacheEvictionPolicy = evictionPolicy;
                            }
                            if (key.equals("highThreshold")) {
                                if (null != value)
                                    config.diskCacheHighThreshold = (Integer) value;
                            }
                            if (key.equals("lowThreshold")) {
                                if (null != value)
                                    config.diskCacheLowThreshold = (Integer) value;
                            }
                            if (key.equals("location")) {
                                config.diskOffloadLocation = (String) value;
                            }
                            if (key.equals("flushToDiskOnStopEnabled")) {
                                config.flushToDiskOnStop = (Boolean) value;
                            }
                        }
                    }
                } catch (IOException e) {
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // Stop the cache instance and destroy the cachConfig - called by the OSGI runtime
    // ---------------------------------------------------------------------------------
    @Deactivate
    protected void stop() {
        final String methodName = "stop()";

        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName, cacheName);

        CacheConfig cc = cacheConfigs.remove(cacheName);
        CacheProviderLoaderImpl.getInstance().removeCacheProvider(this);

        DCache cache = (DCache) ServerCache.getCacheInstances().remove(cacheName);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Dropping cache ", cache);
        }
        if (null != cache) {
            cache.stop();
            cache.clearMemory(false);
        }

        DistributedObjectCache dob = DistributedObjectCacheFactory.removeMap(cacheName);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Dropping object cache ", dob);
        }

        if (null != cc) {
            cc.cache = null;
            cc.distributedObjectCache = null;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Dropping CacheConfig ", cc);
        }

        if (cacheName.equalsIgnoreCase(DCacheBase.DEFAULT_CACHE_NAME)) {
            commonCacheConfig = null;
            ServerCache.cacheUnit.initialize(null);
            ServletCacheUnit scu = ServerCache.cacheUnit.getServletCacheUnit();
            if (scu != null) {
                scu.purgeState(cacheName);
            }
            ServerCache.cache = null;
            ServerCache.jspCache = null;
            ServerCache.commandCache = null;
            ServerCache.unsetCacheService(this);
        }

        deleteOSGiConfiguration();

        // Stop object cache
        ServerCache.objectCacheEnabled = false;
        ServerCache.coreCacheEnabled = false;

        unInitializeConfigPropertiesFiles(getClass().getClassLoader(), DISTRIBUTED_MAP_PROPERTIES);
        unInitializeConfigPropertiesFiles(getClass().getClassLoader(), CACHE_INSTANCES_PROPERTIES);

        Collection<DistributedObjectCache> distributedMaps = DistributedObjectCacheFactory.distributedMaps.values();
        for (DistributedObjectCache distributedObjectCache : distributedMaps) {
            DCache distCache = ((DistributedObjectCacheAdapter) distributedObjectCache).getCache();
            ServerCache.getCacheInstances().remove(distCache);
            distCache.stop();
            deleteOSGiConfiguration(distCache.getCacheName());
        }
        DistributedObjectCacheFactory.distributedMaps.clear();
        DistributedObjectCacheFactory.unsetCacheService(this);

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    private void deleteOSGiConfiguration() {
        deleteOSGiConfiguration(this.cacheName);
    }

    private void deleteOSGiConfiguration(String cacheName) {
        final String methodName = "deleteOSGiConfiguration()";

        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName, cacheName);

        try { // DELETE CACHE OSGI CONFIGURATION THAT IS GENERATED
            Configuration[] osgiConfigs = getOSGiConfiguration(cacheName);
            if (null != osgiConfigs) {
                for (Configuration configuration : osgiConfigs) {
                    String generated = ((String) configuration.getProperties().get(PROPS_GENERATED));
                    if (generated == null || !generated.equals("true")) {
                        // skip anything that isn't generated
                        continue;
                    }

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Deleting OSGi Configuration ", configuration.getProperties());

                    String[] diskPIDs = (String[]) configuration.getProperties().get("disk");
                    if (null != diskPIDs) {
                        for (String diskPID : diskPIDs) {
                            Configuration diskConfig = configAdminRef.get().getConfiguration(diskPID);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "\t Deleting OSGi Configuration ", diskConfig.getProperties());
                            diskConfig.delete();
                        }
                    }

                    String[] cacheGroupPIDs = (String[]) configuration.getProperties().get("cacheGroup");
                    if (null != cacheGroupPIDs) {
                        for (String cacheGroupPID : cacheGroupPIDs) {
                            Configuration cacheGroupMetaTypeConfig = configAdminRef.get().getConfiguration(cacheGroupPID);
                            Dictionary<String, Object> cacheGroupProperties = cacheGroupMetaTypeConfig.getProperties();
                            if (null != cacheGroupProperties) {
                                if (cacheGroupProperties.get("member") instanceof String[]) {
                                    String[] pids = (String[]) cacheGroupProperties.get("member");
                                    if (null != pids) {
                                        for (String pid : pids) {
                                            Configuration memberConfig = configAdminRef.get().getConfiguration(pid);
                                            if (tc.isDebugEnabled())
                                                Tr.debug(tc, "\t Deleting OSGi Configuration ", memberConfig.getProperties());
                                            memberConfig.delete();
                                        }
                                    }
                                }
                            }
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "\t Deleting OSGi Configuration ", cacheGroupMetaTypeConfig.getProperties());
                            cacheGroupMetaTypeConfig.delete();
                        }
                    }

                    configuration.delete();
                }
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + " no configuration for: ", cacheName);
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, e);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName, cacheName);
    }

    // --------------------------------------------------------------
    // Config is from FQ properties built from a file
    // --------------------------------------------------------------
    @Override
    public CacheConfig addCacheInstanceConfig(Properties properties) {

        CacheConfig config = null;
        final String methodName = "addCacheInstanceConfig(Properties)";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName + " properties=" + properties);

        try {
            config = new CacheConfig(properties, commonCacheConfig);
            postProcessAndAddConfig(config, true);
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.cache.CacheServiceImpl.addCacheInstanceConfig(Properties)", "396", this);
            if (tc.isWarningEnabled()) {
                Tr.error(tc, "DYNA1091E", new Object[] { properties.toString(), ex.getCause() });
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName + " ALL CONFIGS " + cacheConfigs);

        return config;
    }

    // --------------------------------------------------------------
    // Config is from the factory and server.xml
    // --------------------------------------------------------------
    @Override
    public void addCacheInstanceConfig(CacheConfig config, boolean create) throws Exception {
        final String methodName = "addCacheInstanceConfig(CacheConfig)";

        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName + " reference=" + config.cacheName + " cacheName=" + config.cacheName);

        if (config.cacheName.equalsIgnoreCase(DCacheBase.DEFAULT_CACHE_NAME)) {
            commonCacheConfig = config;
            ServerCache.cacheUnit.initialize(commonCacheConfig);
        }
        postProcessAndAddConfig(config, create);

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName + " ALL CONFIGS " + cacheConfigs);
    }

    private void postProcessAndAddConfig(CacheConfig config, boolean create) throws IOException, InvalidSyntaxException {
        CacheConfig prevConfig = cacheConfigs.get(config.cacheName);
        if (prevConfig == null) {
            cacheConfigs.put(config.cacheName, config);
            if (create) {
                findOrCreateOSGiConfiguration(config);
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Saved the dynacache internal config object as  ", new Object[] { config.cacheName, config });
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Config object already exist ", new Object[] { prevConfig.cacheName, prevConfig });
            }
        }
    }

    private void findOrCreateOSGiConfiguration(CacheConfig config) throws IOException, InvalidSyntaxException {

        StringBuilder filter = new StringBuilder("(&").append(FilterUtils.createPropertyFilter(ConfigurationAdmin.SERVICE_FACTORYPID, FACTORY_PID));
        if (null != config.jndiName) {
            filter.append(FilterUtils.createPropertyFilter(PROPS_JNDI_NAME, config.jndiName));
        } else {
            filter.append(FilterUtils.createPropertyFilter("id", config.cacheName));
            config.jndiName = config.cacheName;
        }
        filter.append(')');

        ConfigurationAdmin configAdmin = configAdminRef.get();
        if (null != configAdmin) {
            Configuration[] configs = configAdmin.listConfigurations(filter.toString());

            if (null != configs && configs.length > 0) { // For configurations from server.xml
                Configuration osgiCacheConfig = configs[0];
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found OSGI Configuration", osgiCacheConfig.getProperties());
                }
            } else { // For configurations that come through the properties file
                Hashtable<String, Object> props = new Hashtable<String, Object>();
                props.put("id", config.cacheName);
                props.put(PROPS_JNDI_NAME, config.jndiName);
                props.put(PROPS_GENERATED, Boolean.TRUE.toString());
                props.put(PROPS_CACHE_PROVIDER_NAME, config.cacheProviderName);
                if (!config.isDefaultCacheProvider()) {
                    props.put("cacheProvider.target", "(name=" + config.cacheProviderName + ")");
                }
                if (null != config.libraryRef) {
                    props.put("libraryRef", config.libraryRef);
                    props.put("sharedLib.target", "(service.pid=" + config.libraryRef + ")");
                }
                Configuration osgiCacheConfig = configAdmin.createFactoryConfiguration(FACTORY_PID);
                osgiCacheConfig.update(props);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Created OSGI Configuration", osgiCacheConfig.getProperties());
                }
            }
        }
    }

    public Configuration[] getOSGiConfiguration(String cacheName) throws IOException, InvalidSyntaxException {
        Configuration[] configs = null;
        ConfigurationAdmin configAdmin = configAdminRef.get();
        if (null != configAdmin) {
            String filter = "(&" + FilterUtils.createPropertyFilter(ConfigurationAdmin.SERVICE_FACTORYPID, FACTORY_PID)
                            + FilterUtils.createPropertyFilter("id", cacheName) + ")";
            configs = configAdmin.listConfigurations(filter);
            if (null != configs && configs.length > 0) {
                for (Configuration configuration : configs) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found OSGI Configuration/s", configuration.getProperties());
                    }
                }
            }
        }
        return configs;
    }

    // --------------------------------------------------------------
    // Get the named cache instance cacheConfig from the table
    // --------------------------------------------------------------
    @Override
    public CacheConfig getCacheInstanceConfig(String reference) {
        final String methodName = "getCacheInstanceConfig()";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName + " reference=" + reference);

        CacheConfig config = cacheConfigs.get(reference);
        if (config == null) {
            if (reference.equalsIgnoreCase(DCacheBase.DEFAULT_BASE_JNDI_NAME)) {
                reference = DCacheBase.DEFAULT_CACHE_NAME;
            } else if (reference.equalsIgnoreCase(DCacheBase.DEFAULT_DMAP_JNDI_NAME)) {
                reference = DCacheBase.DEFAULT_DISTRIBUTED_MAP_NAME;
            }
            config = cacheConfigs.get(reference);
        }

        if (null == config && tc.isDebugEnabled()) {
            Tr.debug(tc, "ALL CONFIGS", cacheConfigs);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName + " config=" + (config != null ? "" + config.hashCode() : "null") + " reference=" + reference);
        return config;
    }

    // --------------------------------------------------------------
    // Stop the named Cache instance and remove the cacheConfig from table
    // --------------------------------------------------------------
    @Override
    public void destroyCacheInstance(String reference) {
        final String methodName = "destroyCacheInstance()";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName + " reference=" + reference);

        CacheConfig config = null;
        String ref = reference;

        if (reference != null) {
            // if cache name is "services/cache/basecache", change to "basecache".
            // if cache name is "services/cache/distributedmap". change to "default"
            if (reference.equalsIgnoreCase(DCacheBase.DEFAULT_BASE_JNDI_NAME)) {
                ref = DCacheBase.DEFAULT_CACHE_NAME;
            } else if (reference.equalsIgnoreCase(DCacheBase.DEFAULT_DMAP_JNDI_NAME)) {
                ref = DCacheBase.DEFAULT_DISTRIBUTED_MAP_NAME;
            }
            config = cacheConfigs.remove(ref);
        }
        if (config != null) {
            if (config.cache != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + " Stopping named cache: " + config.cache.getCacheName());
                config.cache.stop();
            }
            config.reset();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    // --------------------------------------------------------------

    /**
     * gets a reference to this cache's internal config object
     */
    @Override
    public CacheConfig getCacheConfig() {
        return config;
    }

    // -----------------------------------------------------------------
    // For CacheMonitor ( via ServerCache.getServletCacheInstanceNames() )
    // -----------------------------------------------------------------
    /**
     * Get the list of cache config for servlet cache instances
     * 
     * @return the list of cache config
     */
    @Override
    public ArrayList<String> getServletCacheInstanceNames() {
        ArrayList<String> list = new ArrayList<String>();
        if (cacheConfigs != null) {
            for (String cacheName : cacheConfigs.keySet()) {
                CacheConfig cacheConfig = cacheConfigs.get(cacheName);
                if (cacheConfig.enableServletSupport) {
                    list.add(cacheName);
                }
            }
        }
        return list;
    }

    /**
     * Get the list of cache config for object cache instances
     * 
     * @return the list of cache config
     */
    @Override
    public ArrayList<String> getObjectCacheInstanceNames() {
        ArrayList<String> list = new ArrayList<String>();
        if (cacheConfigs != null) {
            for (String cacheName : cacheConfigs.keySet()) {
                CacheConfig cacheConfig = cacheConfigs.get(cacheName);
                if (!cacheConfig.enableServletSupport) {
                    list.add(cacheName);
                }
            }
        }
        return list;
    }

    // -----------------------------------------------------------------
    // For CacheMonitor ( via ServerCache.getServletCacheInstanceNames() )
    // -----------------------------------------------------------------
    @Override
    public CacheInstanceInfo[] getCacheInstanceInfo() {
        CacheInstanceInfo[] info = CacheInstanceInfo.getCacheInstanceInfo(this);
        return info;
    }

    // -----------------------------------------------------------------

    // -----------------------------------------------------------------
    // Called by CacheInstanceInfo - max entry rate is once in 3 sec
    // -----------------------------------------------------------------
    protected void populateCacheInstanceInfo(CacheInstanceInfo info) {
        final String methodName = "populateCacheInstanceInfo()";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName);

        if (cacheConfigs != null) {
            Iterator<String> cacheNames = cacheConfigs.keySet().iterator();
            while (cacheNames.hasNext()) {
                String cacheName = cacheNames.next();
                CacheConfig wccmCacheConfig = cacheConfigs.get(cacheName);
                CacheInstanceInfo.allConfigured.add(cacheName);
                CacheInstanceInfo.allFactory.add(cacheName);
                CacheInstanceInfo.allFile.add(cacheName);

                if (wccmCacheConfig.cache != null) {
                    CacheInstanceInfo.allActive.add(cacheName);
                }
                if (wccmCacheConfig.enableServletSupport) {
                    CacheInstanceInfo.servletConfigured.add(cacheName);
                } else {
                    CacheInstanceInfo.objectConfigured.add(cacheName);
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    public Scheduler getScheduler() {
        return schedulerRef.get();
    }

    @Reference
    protected void setScheduler(Scheduler s) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setScheduler ", s);
        }
        schedulerRef.set(s);
    }

    protected void unsetScheduler(Scheduler s) {
        schedulerRef.compareAndSet(s, null);
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configAdminRef.get();
    }

    @Reference
    protected void setConfigurationAdmin(ConfigurationAdmin ca) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "ConfigurationAdmin ", ca);
        }
        configAdminRef.set(ca);
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin ca) {
        configAdminRef.compareAndSet(ca, null);
    }

    @Override
    public Object createResource(ResourceInfo ref) throws Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "createResource", ref);
        }

        DistributedObjectCache dmap = null;
        if (ServerCache.coreCacheEnabled) {
            CacheConfig config = getCacheInstanceConfig(cacheName);
            if (null != config) {
                dmap = DistributedObjectCacheFactory.distributedMaps.get(config.cacheName);
                if (null == dmap) {
                    dmap = DistributedObjectCacheFactory.getMap(config.cacheName);
                }
            }
            if (null == config) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "all cache names", cacheConfigs.keySet());
                }
            }
        } else {
            throw new IllegalStateException("Core Cache Service is disabled");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "createResource", dmap);
        }
        return dmap;
    }

    @Override
    public Library getSharedLibrary() {
        return sharedLibRef.get();
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    @Reference(name = "cacheProvider", service = CacheProvider.class, cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setCacheProvider(CacheProvider provider) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setCacheProvider", provider);
        cacheProvider = provider;
    }

    protected void unsetCacheProvider(CacheProvider provider) {}

    @Reference(name = "sharedLib", service = Library.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setSharedLib(Library ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setSharedLib", ref);
        sharedLibRef.set(ref);
    }

    /**
     * Declarative Services method for unsetting the shared library service reference
     * 
     * @param ref
     *            reference to the service
     */
    protected void unsetSharedLib(Library ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "unsetSharedLib", ref);
        sharedLibRef.set(null);
    }

    @Override
    public String getCacheName() {
        return cacheName;
    }

    public CacheProvider getCacheProvider() {
        return cacheProvider;
    }

    @Override
    public void onStartup(java.util.Set<java.lang.Class<?>> c, ServletContext ctx) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "onStartup");

        if (tc.isDebugEnabled())
            Tr.debug(tc, "created listener to add to servletContext.  listener is: " + this);

        ctx.addListener(this);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "onStartup");
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final String methodName = "contextInitialized";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName, new Object[] { sce.getServletContext().getContextPath() });
        try {
            initializeConfigPropertiesFiles(sce.getServletContext(), "/" + DISTRIBUTED_MAP_PROPERTIES);
            initializeConfigPropertiesFiles(sce.getServletContext(), "/" + CACHE_INSTANCES_PROPERTIES);
            initializeConfigPropertiesFiles(sce.getServletContext(), "/" + WEB_INF_DISTRIBUTED_MAP_PROPERTIES);
            initializeConfigPropertiesFiles(sce.getServletContext(), "/" + WEB_INF_CACHE_INSTANCES_PROPERTIES);
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ObjectCacheServiceImpl.contextInitialized(ServletContextEvent)", "169",
                                                        this);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        final String methodName = "contextDestroyed";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName, new Object[] { sce.getServletContext().getContextPath() });
        try {
            unInitializeConfigPropertiesFiles(sce.getServletContext(), "/" + DISTRIBUTED_MAP_PROPERTIES);
            unInitializeConfigPropertiesFiles(sce.getServletContext(), "/" + CACHE_INSTANCES_PROPERTIES);
            unInitializeConfigPropertiesFiles(sce.getServletContext(), "/" + WEB_INF_DISTRIBUTED_MAP_PROPERTIES);
            unInitializeConfigPropertiesFiles(sce.getServletContext(), "/" + WEB_INF_CACHE_INSTANCES_PROPERTIES);

        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ObjectCacheServiceImpl.contextDestroyed(ServletContextEvent)", "184",
                                                        this);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    @Reference(service = VariableRegistry.class)
    protected void setVariableRegistry(VariableRegistry vr) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setVariableRegistry ", vr);
        }
        variableRegistryRef.set(vr);
    }

    protected void unsetVariableRegistry(VariableRegistry vr) {
        variableRegistryRef.compareAndSet(vr, null);
    }

    // --------------------------------------------------------------
    // Called during start() and stateChanged().
    // Reads all the config properties files and
    // creates and binds the maps.
    // --------------------------------------------------------------
    private void initializeConfigPropertiesFiles(ServletContext sc, String fileName) {
        final String methodName = "initializeConfigPropertiesFiles()";

        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName + "servletcontext=" + sc + " fileName=" + fileName);
        try {
            URL u = sc.getResource(fileName);
            /*
             * Set<String> paths = sc.getResourcePaths(fileName);
             * Iterator<String> it = paths.iterator();
             * Vector<URL> urls = new Vector<URL>();
             * while (it.hasNext()) {
             * String file = it.next();
             * URL url = new URL(file);
             * urls.add(url);
             * }
             * 
             * for (Enumeration<URL> resources = urls.elements(); resources.hasMoreElements();) {
             * URL url = resources.nextElement();
             * if (!processedUrls.contains(url))
             * processConfigUrl(url);
             * }
             */
            if (u != null && !processedUrls.contains(u))
                processConfigUrl(u);
        } catch (Exception ex) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "dynacache config error", ex);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    public void initializeConfigPropertiesFiles(ClassLoader cl, String fileName) {
        final String methodName = "initializeConfigPropertiesFiles()";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName + " classLoader=" + cl + " fileName=" + fileName);
        try {
            Enumeration<URL> resources = cl.getResources(fileName);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (!processedUrls.contains(url))
                    processConfigUrl(url);
            }
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.CacheServiceImpl.initializeConfigPropertiesFiles", "638", this);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    private void unInitializeConfigPropertiesFiles(ServletContext sc, String fileName) {
        try {
            URL u = sc.getResource(fileName);
            processedUrls.remove(u);
            /*
             * Enumeration<URL> resources = classLoader.getResources(fileName);
             * while (resources.hasMoreElements()) {
             * URL url = resources.nextElement();
             * processedUrls.remove(url);
             * }
             */
        } catch (Exception ex) {

        }
    }

    private void unInitializeConfigPropertiesFiles(ClassLoader classLoader, String fileName) {
        try {

            Enumeration<URL> resources = classLoader.getResources(fileName);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                processedUrls.remove(url);
            }

        } catch (Exception ex) {

        }
    }

    // --------------------------------------------------------------
    // Create cache instances and associated DistributedObjectCache wrapper from a properties file.
    // --------------------------------------------------------------
    private void processConfigUrl(URL url) throws Exception {
        final String methodName = "processConfigUrl()";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName + " configUrl=" + url);

        processedUrls.add(url);
        Properties props = new Properties();
        InputStream is = url.openStream();
        try {
            props.load(is);
        } finally {
            is.close();
        }

        int i = 0;
        VariableRegistry vr = variableRegistryRef.get();
        boolean found = false;
        do {
            // Look for instance name ( JNDI name == cacheName )
            String instancekey = "cache.instance." + i;
            String instanceName = props.getProperty(instancekey);

            // Remove leading forward slash
            if (instanceName != null && instanceName.charAt(0) == '/')
                instanceName = instanceName.substring(1);

            // Do we have an instance name?
            if (instanceName != null) {
                instanceName = instanceName.trim();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + " Adding new cache instance. instanceName=" + instanceName);
                found = true;

                // Build a vMap converted properties object
                Iterator<Object> it = props.keySet().iterator();
                Properties ccProps = new Properties();
                ccProps.put(CacheConfig.CACHE_NAME, instanceName);
                while (it.hasNext()) {
                    String s = (String) it.next();
                    if (s.startsWith(instancekey + ".")) {
                        String value = vr.resolveString(props.getProperty(s)).trim();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName + " Adding property: " + s + " value:" + value);
                        ccProps.put("com.ibm.ws.cache.CacheConfig" + s.substring(instancekey.length()), value);
                        // for external cache provider access
                        String unmodifiedKey = s.substring(instancekey.length() + 1);
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, methodName + " Adding property: " + unmodifiedKey + " value:" + value);
                        }
                        ccProps.put(unmodifiedKey, value);
                    }
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + " Final props=" + ccProps);

                // ------------------------------------------------------------
                // Bind to JNDI if the cache & the config do not exist
                // ------------------------------------------------------------
                DCache c = ServerCache.getCache(instanceName);

                CacheConfig cacheConfig = getCacheInstanceConfig(instanceName);
                if (c == null && cacheConfig == null) {
                    cacheConfig = addCacheInstanceConfig(ccProps);
                    if (cacheConfig.isCreateCacheAtServerStartup()) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Create cache instance \"" + instanceName + "\" during startup because the cache is configured to.");
                        }
                        ServerCache.createCache(cacheConfig.getCacheName(), cacheConfig);
                    }
                } else {
                    // In this case, the instanceName already exists, so output
                    // a
                    // warning message for the user
                    // DYNA1057W=DYNA1057W: Cache instance \"{0}\" defined in
                    // the \"{1}\" is not added because a cache with this name
                    // already exists.
                    // DYNA1057W.explanation=The cache instance is not added
                    // because a cache with this name already exists.
                    // DYNA1057W.useraction=Ensure that the cache instance does
                    // not define more than once in the properties file.
                    Tr.warning(tc, "DYNA1057W", new Object[] { instanceName, url.toString() });
                }
            } else {
                found = false;
            }
            i++;
        } while (found);

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName + " configUrl=" + url);
    }

}