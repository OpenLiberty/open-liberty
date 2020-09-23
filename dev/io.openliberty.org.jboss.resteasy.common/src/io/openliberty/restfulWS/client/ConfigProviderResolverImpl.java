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
package io.openliberty.restfulWS.client;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Only intended to be used in Java SE client environments.
 */
public class ConfigProviderResolverImpl extends ConfigProviderResolver {
    
    private final boolean java2SecurityEnabled = System.getSecurityManager() != null;

    private final Config config = new Config() {

        @Override
        public Iterable<ConfigSource> getConfigSources() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <T> Optional<T> getOptionalValue(String propName, Class<T> type) {
            return Optional.ofNullable(getValue(propName, type));
        }

        @SuppressWarnings("unchecked")
        @Override
        public Iterable<String> getPropertyNames() {
            if (java2SecurityEnabled) {
                return AccessController.doPrivileged((PrivilegedAction<Iterable<String>>) () ->
                    (Iterable<String>) System.getProperties().keySet().iterator());
            }
            return (Iterable<String>) System.getProperties().keySet().iterator();
        }

        @Override
        public <T> T getValue(String propName, Class<T> propType) {
            return propType.cast(System.getProperty(propName));
        }};

    @Override
    public ConfigBuilder getBuilder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Config getConfig() {
        return getConfig(getThisClassLoader());
    }

    @Override
    public Config getConfig(ClassLoader loader) {
        if (loader != getThisClassLoader()) {
            return null;
        }
        return config;
    }

    @Override
    public void registerConfig(Config arg0, ClassLoader arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void releaseConfig(Config arg0) {
        // TODO Auto-generated method stub

    }

    private ClassLoader getThisClassLoader() {
        if (java2SecurityEnabled) {
            return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> 
                this.getClass().getClassLoader());
        }
        return this.getClass().getClassLoader();
    }
}
