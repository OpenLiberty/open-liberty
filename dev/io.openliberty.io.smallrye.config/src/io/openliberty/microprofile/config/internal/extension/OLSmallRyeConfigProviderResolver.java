/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.extension;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigProviderResolver;

@Component(service = ConfigProviderResolver.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" }, immediate = true)
public class OLSmallRyeConfigProviderResolver extends SmallRyeConfigProviderResolver implements ApplicationStateListener {

    //We try to keep track of which application is using which Config. There are cases where the Config is used by a global component
    //or we just can't work out which app it is. Then we fall back to this global name.
    public static final String GLOBAL_CONFIG_APPLICATION_NAME = "!GLOBAL_CONFIG_APPLICATION_NAME!";

    //NOTE: a lock must be held on the configCache whenever reading or writing to the configCache, the appClassLoaderMap or any of the contained ConfigWrappers.
    //map from classloader to wrapper
    private final Map<ClassLoader, ConfigWrapper> classLoaderCache = new HashMap<>();
    //map from config to wrapper
    private final Map<Config, ConfigWrapper> configCache = new HashMap<>();
    //map from app Name to list of ClassLoader in use
    private final Map<String, Set<ConfigWrapper>> applicationCache = new HashMap<>();

    /**
     * Activate a context and set the instance
     *
     * @param cc
     */
    public void activate(ComponentContext cc) {
        ConfigProviderResolver.setInstance(this);
    }

    /**
     * Deactivate a context and set the instance to null
     *
     * @param cc
     */
    public void deactivate(ComponentContext cc) throws IOException {
        ConfigProviderResolver.setInstance(null);
    }

    @Override
    public SmallRyeConfigBuilder getBuilder() {
        return new OLSmallRyeConfigBuilder().addDefaultInterceptors();
    }

    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        super.registerConfig(config, classLoader);
        registerConfig(config, classLoader, getApplicationName());
    }

    /** {@inheritDoc} */
    @Override
    public Config getConfig(ClassLoader classLoader) {
        Config config = super.getConfig(classLoader);
        registerConfig(config, classLoader, getApplicationName());

        return config;
    }

    /**
     * Register this config as associated with this classloader and in use by this app
     *
     * @param config
     * @param classLoader
     * @param applicationName
     */
    private void registerConfig(Config config, ClassLoader classLoader, String applicationName) {
        synchronized (this.configCache) {
            ConfigWrapper wrapper = this.classLoaderCache.get(classLoader);
            if (wrapper != null) {
                if (wrapper.getConfig() != config) {
                    //TODO do we want to do anything with the old config?
                    //update the wrapper with the new config
                    wrapper.updateConfig(config);
                }
            } else {
                //create a new ConfigWrapper and put it in the cache
                wrapper = new ConfigWrapper(config, classLoader);
                this.classLoaderCache.put(classLoader, wrapper);
                this.configCache.put(config, wrapper);
            }

            Set<ConfigWrapper> appConfigWrappers = this.applicationCache.get(applicationName);
            if (appConfigWrappers == null) {
                appConfigWrappers = new HashSet<>();
                this.applicationCache.put(applicationName, appConfigWrappers);
            }

            appConfigWrappers.add(wrapper);
            wrapper.addApplication(applicationName);
        }
    }

    @Override
    public void releaseConfig(Config config) {
        super.releaseConfig(config);
        synchronized (this.configCache) {
            ConfigWrapper wrapper = this.configCache.remove(config);
            if (wrapper != null) {
                ClassLoader classLoader = wrapper.getClassLoader();
                this.classLoaderCache.remove(classLoader);

                Set<String> applications = wrapper.getApplications();
                for (String app : applications) {
                    Set<ConfigWrapper> appWrappers = applicationCache.get(app);
                    if (appWrappers != null) {
                        appWrappers.remove(wrapper);
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        //NOOP
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        //NOOP
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {

        ExtendedApplicationInfo extendedAppInfo = (ExtendedApplicationInfo) appInfo;
        String applicationName = extendedAppInfo.getMetaData().getJ2EEName().getApplication();

        synchronized (this.configCache) {
            Set<ConfigWrapper> appWrappers = applicationCache.remove(applicationName);
            if (appWrappers != null) {
                for (ConfigWrapper wrapper : appWrappers) {
                    boolean release = wrapper.removeApplication(applicationName);
                    if (release) {
                        releaseConfig(wrapper.getConfig()); //this might be overkill, could simplify?
                    }
                }
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        // Repeat applicationStopping logic here because applicationStopping is not called for applications which fail to start
        applicationStopping(appInfo);
    }

    private static String getApplicationName() {
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
            applicationName = GLOBAL_CONFIG_APPLICATION_NAME;
        }
        return applicationName;
    }

}
