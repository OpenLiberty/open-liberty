/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.impl;

import java.io.IOException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import io.openliberty.microprofile.config.internal.common.ConfigException;

public abstract class AbstractProviderResolver extends ConfigProviderResolver implements ApplicationStateListener {

    private static final TraceComponent tc = Tr.register(AbstractProviderResolver.class);

    static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private final AtomicServiceReference<ScheduledExecutorService> scheduledExecutorServiceRef = new AtomicServiceReference<>("scheduledExecutorService");

    //NOTE: a lock must be held on the configCache whenever reading or writing to the configCache, the appClassLoaderMap or any of the contained ConfigWrappers.
    //map from classloader to config
    private final Map<ClassLoader, ConfigWrapper> configCache = new HashMap<>();
    //map from app Name to list of ClassLoader in use
    private final Map<String, Set<ClassLoader>> appClassLoaderMap = new HashMap<>();

    /**
     * Save the new ref into the AtomicServiceReference. See {@link AtomicServiceReference}
     *
     * @param ref
     */
    @Reference(name = "scheduledExecutorService", service = ScheduledExecutorService.class, target = "(deferrable=false)")
    protected void setScheduledExecutorService(ServiceReference<ScheduledExecutorService> ref) {
        this.scheduledExecutorServiceRef.setReference(ref);
    }

    /**
     * Unset the ref into the AtomicServiceReference. See {@link AtomicServiceReference}
     *
     * @param ref
     */
    protected void unsetScheduledExecutorService(ServiceReference<ScheduledExecutorService> ref) {
        this.scheduledExecutorServiceRef.unsetReference(ref);
    }

    /**
     * Activate a context and set the instance
     *
     * @param cc
     */
    public void activate(ComponentContext cc) {
        this.scheduledExecutorServiceRef.activate(cc);
        ConfigProviderResolver.setInstance(this);
    }

    /**
     * Deactivate a context and set the instance to null
     *
     * @param cc
     */
    public void deactivate(ComponentContext cc) throws IOException {
        ConfigProviderResolver.setInstance(null);
        shutdown();
        this.scheduledExecutorServiceRef.deactivate(cc);
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        //no-op
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        //no-op
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        // Repeat shutdown logic here because applicationStopping is not called for applications which fail to start
        ExtendedApplicationInfo extendedAppInfo = (ExtendedApplicationInfo) appInfo;
        String applicationName = extendedAppInfo.getMetaData().getJ2EEName().getApplication();

        shutdown(applicationName);
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {

        ExtendedApplicationInfo extendedAppInfo = (ExtendedApplicationInfo) appInfo;
        String applicationName = extendedAppInfo.getMetaData().getJ2EEName().getApplication();

        shutdown(applicationName);

    }

    public String getApplicationName() {
        ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        ComponentMetaData cmd = cmdai.getComponentMetaData();
        String applicationName = null;
        if (cmd != null) {
            J2EEName applicationJEEName = cmd.getModuleMetaData().getApplicationMetaData().getJ2EEName();
            applicationName = applicationJEEName.getApplication();
        }
        if (applicationName == null) {
            //There are cases where the Config is used by a global component or we just can't work out which app it is. Then we fall back to this global name.
            //Configs used "globally" will be shutdown when the server is shutdown
            applicationName = ConfigConstants.GLOBAL_CONFIG_APPLICATION_NAME;
        }
        return applicationName;
    }

    /**
     * Close down all the configs
     */
    public void shutdown() {
        synchronized (this.configCache) {
            Set<ClassLoader> allClassLoaders = new HashSet<>();
            allClassLoaders.addAll(this.configCache.keySet()); //create a copy of the keys so that we avoid a ConcurrentModificationException
            for (ClassLoader classLoader : allClassLoaders) {
                close(classLoader);
            }
            //caches should be empty now but clear them anyway
            this.configCache.clear();
            this.appClassLoaderMap.clear();
        }
    }

    /**
     * Close down all the configs used by a specified application (that are not also still in use by other applications)
     */
    private void shutdown(String applicationName) {
        synchronized (this.configCache) {
            Set<ClassLoader> appClassLoaders = this.appClassLoaderMap.remove(applicationName);
            if (appClassLoaders != null) {
                for (ClassLoader classLoader : appClassLoaders) {
                    shutdown(applicationName, classLoader);
                }
            }
        }
    }

    /**
     * Close down a specific config used by a specified application (that is not also still in use by other applications)
     */
    private void shutdown(String applicationName, ClassLoader classLoader) {
        synchronized (this.configCache) {
            ConfigWrapper configWrapper = this.configCache.get(classLoader);
            boolean close = configWrapper.removeApplication(applicationName);
            if (close) {
                close(classLoader);
            }
        }
    }

    /**
     * Completely close a config for a given classloader
     */
    private void close(ClassLoader classLoader) {
        synchronized (this.configCache) {
            ConfigWrapper config = this.configCache.remove(classLoader);
            if (config != null) {
                Set<String> applicationNames = config.getApplications();
                for (String app : applicationNames) {
                    this.appClassLoaderMap.remove(app);
                }

                config.close();
            }
        }
    }

    /**
     * Close a given config, if it's a WebSphereConfig
     */
    private void closeConfig(Config config) {
        if (config instanceof WebSphereConfig) {
            try {
                ((WebSphereConfig) config).close();
            } catch (IOException e) {
                throw new ConfigException(Tr.formatMessage(tc, "could.not.close.CWMCG0004E", e));
            }
        }
    }

    /**
     * @return the scheduledExecutorServiceRef service
     */
    public ScheduledExecutorService getScheduledExecutorService() {
        return this.scheduledExecutorServiceRef.getService();
    }

    /** {@inheritDoc} */
    @Override
    public Config getConfig() {
        ClassLoader classloader = getThreadContextClassLoader();
        Config config = getConfig(classloader);
        return config;
    }

    private ClassLoader getThreadContextClassLoader() {
        ClassLoader classloader = priv.getContextClassLoader();
        return classloader;
    }

    /** {@inheritDoc} */
    @Override
    public Config getConfig(ClassLoader classLoader) {

        if (classLoader == null) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "null.classloader.CWMCG0002E"));
        }

        Config config = null;
        synchronized (this.configCache) {
            ConfigWrapper configWrapper = this.configCache.get(classLoader);
            if (configWrapper == null) {
                AbstractConfigBuilder builder = newBuilder(classLoader);
                //add all default and discovered sources and converters
                builder.addDefaultSources();
                builder.addDiscoveredSources();
                builder.addDefaultConverters();
                builder.addDiscoveredConverters();
                config = builder.build();

                //add this new config
                configWrapper = newConfigWrapper(config, classLoader);
            } else {
                config = configWrapper.getConfig();
            }

            //register the application
            registerApplication(configWrapper, classLoader);

        }
        return config;
    }

    /** {@inheritDoc} */
    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        synchronized (this.configCache) {
            ConfigWrapper configWrapper = newConfigWrapper(config, classLoader);
            registerApplication(configWrapper, classLoader);
        }
    }

    private ConfigWrapper newConfigWrapper(Config config, ClassLoader classLoader) {
        ConfigWrapper configWrapper = null;
        synchronized (this.configCache) {
            configWrapper = null;
            ConfigWrapper previous = this.configCache.get(classLoader);
            if (previous != null) {
                throw new IllegalStateException(Tr.formatMessage(tc, "config.already.exists.CWMCG0003E"));
            }
            //create a new ConfigWrapper and put it in the cache
            configWrapper = new ConfigWrapper((WebSphereConfig) config);
            this.configCache.put(classLoader, configWrapper);
        }
        return configWrapper;
    }

    /**
     *
     * Register an application's use of a config
     *
     * @param configWrapper
     * @param classLoader
     */
    private void registerApplication(ConfigWrapper configWrapper, ClassLoader classLoader) {
        String applicationName = getApplicationName();
        synchronized (this.configCache) {
            Set<ClassLoader> appClassLoaders = this.appClassLoaderMap.get(applicationName);
            if (appClassLoaders == null) {
                appClassLoaders = new HashSet<>();
                this.appClassLoaderMap.put(applicationName, appClassLoaders);

                appClassLoaders.add(classLoader);
                configWrapper.addApplication(applicationName);
            } else if (!appClassLoaders.contains(classLoader)) {
                appClassLoaders.add(classLoader);
                configWrapper.addApplication(applicationName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void releaseConfig(Config config) {
        synchronized (this.configCache) {
            ClassLoader classloader = findClassloaderForRegisteredConfig(config);
            if (classloader != null) {
                close(classloader);
            } else {
                closeConfig(config);
            }
        }
    }

    private ClassLoader findClassloaderForRegisteredConfig(Config config) {
        synchronized (this.configCache) {
            //look through the cache and find the classloader which corresponds to the specified config
            for (Map.Entry<ClassLoader, ConfigWrapper> entry : this.configCache.entrySet()) {
                ClassLoader classLoader = entry.getKey();
                ConfigWrapper configWrapper = entry.getValue();
                if (configWrapper != null) {
                    Config cachedConfig = configWrapper.getConfig();
                    if ((cachedConfig != null) && (config == cachedConfig)) {
                        return classLoader;
                    }
                }
            }
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public ConfigBuilder getBuilder() {
        ClassLoader classLoader = getThreadContextClassLoader();
        AbstractConfigBuilder builder = newBuilder(classLoader);
        //do not add default sources, discovered sources or discovered converters
        //always add default converters
        builder.addDefaultConverters();
        return builder;
    }

    @Trivial
    public String getConfigCacheDetails() {
        StringBuilder builder = new StringBuilder("[");
        boolean first1 = true;
        synchronized (this.configCache) {
            for (Map.Entry<ClassLoader, ConfigWrapper> entry : this.configCache.entrySet()) {
                if (!first1) {
                    builder.append(", ");
                } else {
                    first1 = false;
                }
                ClassLoader cl = entry.getKey();
                ConfigWrapper wrapper = entry.getValue();
                builder.append(cl.toString());
                builder.append(":{");
                boolean first2 = true;
                for (String app : wrapper.getApplications()) {
                    if (!first2) {
                        builder.append(",");
                    } else {
                        first2 = false;
                    }
                    builder.append(app);
                }
                builder.append("}");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    @Trivial
    public int getConfigCacheSize() {
        int size = 0;
        synchronized (this.configCache) {
            size = this.configCache.size();
        }
        return size;
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getName());
        builder.append(": cache=");
        builder.append(getConfigCacheDetails());
        return builder.toString();
    }

    protected abstract AbstractConfigBuilder newBuilder(ClassLoader classLoader);

}
