/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.impl;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.interfaces.ConfigException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public abstract class ConfigProviderResolverImpl extends ConfigProviderResolver {

    private static final TraceComponent tc = Tr.register(ConfigProviderResolverImpl.class);

    private final AtomicServiceReference<ScheduledExecutorService> scheduledExecutorServiceRef = new AtomicServiceReference<ScheduledExecutorService>("scheduledExecutorService");

    private final WeakHashMap<ClassLoader, WeakReference<Config>> configCache = new WeakHashMap<>();

    /**
     * Save the new ref into the AtomicServiceReference. See {@link AtomicServiceReference}
     *
     * @param ref
     */
    @Reference(name = "scheduledExecutorService", service = ScheduledExecutorService.class, target = "(deferrable=false)")
    protected void setScheduledExecutorService(ServiceReference<ScheduledExecutorService> ref) {
        scheduledExecutorServiceRef.setReference(ref);
    }

    /**
     * Unset the ref into the AtomicServiceReference. See {@link AtomicServiceReference}
     *
     * @param ref
     */
    protected void unsetScheduledExecutorService(ServiceReference<ScheduledExecutorService> ref) {
        scheduledExecutorServiceRef.unsetReference(ref);
    }

    /**
     * Activate a context and set the instance
     *
     * @param cc
     */
    public void activate(ComponentContext cc) {
        scheduledExecutorServiceRef.activate(cc);
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
        scheduledExecutorServiceRef.deactivate(cc);
    }

    /**
     * Close down the configs
     *
     * @throws IOException
     */
    private void shutdown() throws IOException {
        synchronized (configCache) {
            for (Map.Entry<ClassLoader, WeakReference<Config>> entry : configCache.entrySet()) {
                WeakReference<Config> configRef = entry.getValue();
                if (configRef != null) {
                    Config config = configRef.get();
                    if (config != null) {
                        closeConfig(config);
                    }
                }
            }
            configCache.clear();
        }
    }

    /**
     * @return the scheduledExecutorServiceRef service
     */
    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorServiceRef.getService();
    }

    /** {@inheritDoc} */
    @Override
    public Config getConfig() {
        ClassLoader classloader = getThreadContextClassLoader();
        Config config = getConfig(classloader);
        return config;
    }

    private ClassLoader getThreadContextClassLoader() {
        ClassLoader classloader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
        return classloader;
    }

    /** {@inheritDoc} */
    @Override
    public Config getConfig(ClassLoader loader) {

        if (loader == null) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "null.classloader.CWMCG0002E"));
        }

        Config config = null;
        synchronized (configCache) {
            WeakReference<Config> ref = configCache.get(loader);
            config = ref == null ? null : ref.get();
            if (config == null) {
                ConfigBuilderImpl builder = newBuilder(loader);
                //add all default and discovered sources and converters
                builder.addDefaultSources();
                builder.addDiscoveredSources();
                builder.addDefaultConverters();
                builder.addDiscoveredConverters();
                config = builder.build();

                //add this config to the classloader cache
                registerConfig(config, loader);
            }
        }
        return config;
    }

    /** {@inheritDoc} */
    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        synchronized (configCache) {
            WeakReference<Config> previous = configCache.get(classLoader);
            if (previous != null) {
                if (previous.get() != null) {
                    throw new IllegalStateException(Tr.formatMessage(tc, "config.already.exists.CWMCG0003E"));
                } else {
                    configCache.remove(classLoader);
                }
            }
            configCache.put(classLoader, new WeakReference<Config>(config));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void releaseConfig(Config config) {
        synchronized (configCache) {
            for (Map.Entry<ClassLoader, WeakReference<Config>> entry : configCache.entrySet()) {
                ClassLoader classLoader = entry.getKey();
                WeakReference<Config> configRef = entry.getValue();
                if (configRef != null) {
                    Config cachedConfig = configRef.get();
                    if (cachedConfig != null && config == cachedConfig) {
                        configCache.remove(classLoader);
                        break;
                    }
                }
            }
            closeConfig(config);
        }
    }

    private void closeConfig(Config config) {
        //our implementation is Closeable
        if (config instanceof Closeable) {
            try {
                ((Closeable) config).close();
            } catch (IOException e) {
                throw new ConfigException(Tr.formatMessage(tc, "could.not.close.CWMCG0004E", e));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public ConfigBuilder getBuilder() {
        ClassLoader classLoader = getThreadContextClassLoader();
        ConfigBuilderImpl builder = newBuilder(classLoader);
        //do not add default sources, discovered sources or discovered converters
        //always add default converters
        builder.addDefaultConverters();
        return builder;
    }

    protected abstract ConfigBuilderImpl newBuilder(ClassLoader classLoader);

}