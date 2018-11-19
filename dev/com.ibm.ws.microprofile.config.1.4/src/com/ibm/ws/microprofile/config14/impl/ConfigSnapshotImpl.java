/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.ConfigAccessor;
import org.eclipse.microprofile.config.ConfigSnapshot;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.impl.AbstractConfig;

public class ConfigSnapshotImpl implements ConfigSnapshot {

    private static final TraceComponent tc = Tr.register(AbstractConfig.class);

    private final Map<String, String> resolvedValues = new HashMap<>();

    /**
     * @param config14Impl
     * @param configValues
     */
    public ConfigSnapshotImpl(ConfigAccessor<?>... configValues) {
        for (ConfigAccessor<?> accessor : configValues) {
            ConfigAccessorImpl<?> accessorImpl = (ConfigAccessorImpl<?>) accessor;
            String resolvedValue = accessorImpl.getResolved(true);
            if (resolvedValue != null) {
                String name = accessorImpl.getPropertyName();
                resolvedValues.put(name, resolvedValue);
            }
        }
    }

    public String getResolvedValue(String propertyName, boolean optional) {
        String resolved = resolvedValues.get(propertyName);
        if (!optional && resolved == null) { //TODO not sure if this is handling null values properly
            throw new NoSuchElementException(Tr.formatMessage(tc, "no.such.element.CWMCG0015E", propertyName));
        }
        return resolved;
    }

}
