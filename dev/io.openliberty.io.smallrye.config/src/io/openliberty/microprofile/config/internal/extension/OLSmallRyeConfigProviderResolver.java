/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.extension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
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
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.microprofile.config.internal.common.ConfigIntrospectionProvider;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigProviderResolver;

@Component(service = { ConfigProviderResolver.class, ApplicationStateListener.class, ConfigIntrospectionProvider.class },
           configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" }, immediate = true)
public class OLSmallRyeConfigProviderResolver extends SmallRyeConfigProviderResolver implements ApplicationStateListener, ConfigIntrospectionProvider {

    private static final TraceComponent tc = Tr.register(OLSmallRyeConfigProviderResolver.class);

    //NOTE: a lock must be held on the configCache whenever reading or writing to the configCache or any of the contained ConfigWrappers.
    //map from config to wrapper
    private final Map<Config, ConfigWrapper> configCache = new HashMap<>();

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
        synchronized (this.configCache) {
            super.registerConfig(config, classLoader);
            registerConfig(config, classLoader, getApplicationName());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Config getConfig(ClassLoader classLoader) {
        Config config = null;
        synchronized (this.configCache) {
            config = super.getConfig(classLoader);
            registerConfig(config, classLoader, getApplicationName());
        }
        return config;
    }

    /**
     * Register this config as associated with this classloader and in use by this app
     * Must be synchronized on configCache before calling
     *
     * @param config
     * @param classLoader
     * @param applicationName
     */
    private void registerConfig(Config config, ClassLoader classLoader, String applicationName) {
        ConfigWrapper wrapper = this.configCache.computeIfAbsent(config, (cfg) -> new ConfigWrapper(cfg));
        wrapper.addApplication(applicationName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            traceCache();
        }
    }

    @Override
    public void releaseConfig(Config config) {
        synchronized (this.configCache) {
            deregisterAndReleaseConfig(config);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                traceCache();
            }
        }
    }

    //Must be synchronized on configCache before calling
    private void deregisterAndReleaseConfig(Config config) {
        super.releaseConfig(config);
        this.configCache.remove(config);
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
            Set<Config> configsToRelease = new HashSet<>();
            for (ConfigWrapper wrapper : this.configCache.values()) {
                boolean empty = wrapper.removeApplication(applicationName);
                if (empty) {
                    //If the wrapper is now empty, add the config to the set to be released
                    configsToRelease.add(wrapper.getConfig());
                }
            }
            for (Config config : configsToRelease) {
                deregisterAndReleaseConfig(config);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                traceCache();
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
            applicationName = ConfigWrapper.GLOBAL_CONFIG_APPLICATION_NAME;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to determine application name for Config. Using \"" + applicationName + "\".");
                traceStack();
            }

        }
        return applicationName;
    }

    @Override
    public Map<String, Set<Config>> getConfigsByApplication() {
        Map<String, Set<Config>> appInfos = new HashMap<>();
        synchronized (configCache) {
            for (ConfigWrapper wrapper : configCache.values()) {
                for (String appName : wrapper.listApplications()) {
                    appInfos.computeIfAbsent(appName, x -> new HashSet<>())
                            .add(wrapper.getConfig());
                }
            }
        }
        return appInfos;
    }

    @Trivial
    private static void traceStack() {
        Exception e = new Exception();
        OutputStream os = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(os);
        try {
            e.printStackTrace(pw);
        } finally {
            pw.close();
        }
        Tr.debug(tc, "OLSmallRyeConfigProviderResolver call stack: " + os.toString());
    }

    @Trivial
    private void traceCache() {
        StringBuilder builder = new StringBuilder("\n# OLSmallRyeConfigProviderResolver config cache #\n");
        synchronized (configCache) {
            for (ConfigWrapper wrapper : configCache.values()) {
                builder.append("########################\n");
                builder.append("Config : ");
                builder.append(wrapper.getConfig());
                builder.append("\n");
                for (String appName : wrapper.listApplications()) {
                    builder.append("--> Application: ");
                    builder.append(appName);
                    builder.append("\n");
                }
            }
            builder.append("########################\n");
        }
        Tr.debug(tc, builder.toString());
    }
}
