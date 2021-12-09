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
package com.ibm.ws.microprofile.config14.sources;

import java.security.AccessController;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

import io.openliberty.microprofile.config.internal.common.InternalConfigSource;

/**
 *
 */
public class SystemConfig14Source extends InternalConfigSource implements ExtendedConfigSource {

    private static final TraceComponent tc = Tr.register(SystemConfig14Source.class);
    static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private final String name;

    @Trivial
    public SystemConfig14Source() {
        name = Tr.formatMessage(tc, "system.properties.config.source");
    }

    @Override
    @Trivial
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    protected int getDefaultOrdinal() {
        return ConfigConstants.ORDINAL_SYSTEM_PROPERTIES;
    }

    @Override
    public Map<String, String> getProperties() {
        // Return a copy, removing any entries where either the key or value is not a string
        // This is a bit slow
        // Properties.stringPropertyNames() returns an enumeration over the keys that will not change while we're using it
        Properties props = priv.getProperties();
        return props.stringPropertyNames().stream()
                        .collect(Collectors.toMap(e -> e, e -> props.getProperty(e))) //create a new Map<String, String>
                        .entrySet().stream()
                        .filter(e -> e.getValue() != null) //remove the null values
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

    /*
     * Overridden for performance to avoid calling getProperties which would make a copy of the map
     */
    @Override
    public String getValue(String key) {
        return priv.getProperty(key);
    }

    @Override
    public ConfigString getConfigString(String key) {
        Properties properties = priv.getProperties();
        ConfigString result = null;

        // Synchronized here to ensure that the value isn't removed after checking its existence
        // Individual calls to Properties objects are synchronized anyway so there shouldn't be much overhead
        synchronized (properties) {
            if (properties.containsKey(key)) {
                result = ConfigString.of(properties.getProperty(key));
            }
        }

        if (result == null) {
            result = ConfigString.MISSING;
        }

        return result;
    }

}
