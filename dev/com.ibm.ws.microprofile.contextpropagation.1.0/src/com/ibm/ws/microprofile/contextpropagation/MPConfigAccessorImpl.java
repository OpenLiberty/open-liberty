/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.contextpropagation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

/**
 * This class abstracts away usage of MicroProfile Config so that values can be
 * conditionally read from MicroProfile Config if available. The only data types
 * needed are String[] and Integer.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class MPConfigAccessorImpl implements MPConfigAccessor {
    @Reference
    protected ConfigProviderResolver configProviderResolver;

    /**
     * Reads a String[] or Integer property value from MicroProfile Config.
     *
     * @param config instance of org.eclipse.microprofile.config.Config.
     * @param name config property name.
     * @param defaultValue value to use if a config property with the specified name is not found.
     * @return value from MicroProfile Config. Otherwise the default value.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object config, String name, T defaultValue) {
        Class<?> cl = defaultValue == null || defaultValue instanceof Set ? String[].class : defaultValue.getClass();
        Optional<T> configuredValue = (Optional<T>) ((Config) config).getOptionalValue(name, cl);
        T value = configuredValue.orElse(defaultValue);

        if (value instanceof String[]) {
            // MicroProfile Config is unclear about whether empty value for String[] results in
            // an empty String array or a size 1 String array where the element is the empty string.
            // Allow for both possibilities,
            String[] arr = ((String[]) value);
            if (arr.length == 1 && (arr[0].length() == 0 || "None".equals(arr[0]))) {
                if (defaultValue instanceof Set)
                    value = (T) Collections.EMPTY_SET;
                else // TODO remove if config annotations are removed
                    value = (T) new String[0];
            } else if (defaultValue instanceof Set) {
                Set<String> set = new LinkedHashSet<String>();
                Collections.addAll(set, arr);
                value = (T) set;
            }
        }

        return value;
    }

    /**
     * Resolve instance of org.eclipse.microprofile.config.Config.
     *
     * @return instance of org.eclipse.microprofile.config.Config.
     */
    @Override
    public Object getConfig() {
        return configProviderResolver.getConfig();
    }
}
