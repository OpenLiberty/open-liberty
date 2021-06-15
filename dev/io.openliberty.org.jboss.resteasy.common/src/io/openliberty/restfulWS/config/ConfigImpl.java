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
package io.openliberty.restfulWS.config;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

public class ConfigImpl implements Config {
    private final Set<ConfigSource> configSources = Collections.singleton(new ConfigSourceImpl(this));
    private final Map<String, String> configProperties = new HashMap<>();
    private Set<Object> myPropertiesSet = Collections.synchronizedSet(new HashSet<>());

    ConfigImpl() {
        if (ConfigProviderResolverImpl.java2SecurityEnabled) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                myPropertiesSet.addAll(System.getProperties().keySet());
                for (Object key : myPropertiesSet) {
                    configProperties.put((String) key, System.getProperty((String)key));
                }
                return null;
            });
        } else {
            myPropertiesSet.addAll(System.getProperties().keySet());
            for (Object key : myPropertiesSet) {
                 configProperties.put((String) key, System.getProperty((String)key));
            }
        }
        // Add EJBException to the list of wrapped exceptions that are processed by RESTEasy
        configProperties.merge("resteasy.unwrapped.exceptions", "jakarta.ejb.EJBException", (oldVal, newVal) -> {
            return (oldVal.contains("jakarta.ejb.EJBException") ? oldVal : oldVal + "," + newVal); });
        
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return configSources;
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propName, Class<T> type) {
        return Optional.ofNullable(getValue(propName, type));
    }

    @Override
    public synchronized Iterable<String> getPropertyNames() {
        return configProperties.keySet();
    }

    @Override
    public synchronized <T> T getValue(String propName, Class<T> propType) {
        String value = configProperties.get(propName);
        if (value == null || String.class.equals(propType)) {
            return (T) value;
        }
        return propType.cast(value);
    }

    public synchronized void updateProperties(Map<String, String> map) {
        configProperties.putAll(map);
    }
}