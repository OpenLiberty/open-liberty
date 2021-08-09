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
package io.openliberty.restfulWS.config;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

public class ConfigProviderResolverImpl extends ConfigProviderResolver {
    
    static final boolean java2SecurityEnabled = System.getSecurityManager() != null;

    static String getSysProp(String propName) {
        if (java2SecurityEnabled) {
            return AccessController.doPrivileged((PrivilegedAction<String>) () ->
                System.getProperty(propName));
        }
        return System.getProperty(propName);
    }

    private static ClassLoader getThreadContextClassLoader() {
        if (java2SecurityEnabled) {
            return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () ->
                Thread.currentThread().getContextClassLoader());
        }
        return Thread.currentThread().getContextClassLoader();
    }

    private static ClassLoader getParentClassLoader(ClassLoader loader) {
        if (java2SecurityEnabled) {
            return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () ->
                loader.getParent());
        }
        return loader.getParent();
    }

    private final Map<ClassLoader, Config> configs = new WeakHashMap<>();

    @Override
    public ConfigBuilder getBuilder() {
        return null;
    }

    @Override
    public Config getConfig() {
        ClassLoader tccl = getThreadContextClassLoader();
        Config config = getConfig(tccl);
        return config != null ? config : getConfig(getParentClassLoader(tccl));
    }

    @Override
    public Config getConfig(ClassLoader loader) {
        return configs.computeIfAbsent(loader, cl -> new ConfigImpl());
    }

    @Override
    public void registerConfig(Config config, ClassLoader loader) {
        configs.put(loader, config);
    }

    @Override
    public void releaseConfig(Config config) {
        if (config == null) {
            return;
        }
        Set<ClassLoader> keysToRemove = new HashSet<>();
        for (Map.Entry<ClassLoader, Config> entry : configs.entrySet()) {
            if (config.equals(entry.getValue())) {
                keysToRemove.add(entry.getKey());
            }
        }
        for (ClassLoader key : keysToRemove) {
            configs.remove(key);
        }
    }
}