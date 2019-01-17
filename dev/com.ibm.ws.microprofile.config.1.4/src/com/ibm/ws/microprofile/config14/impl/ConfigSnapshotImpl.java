/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
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

import org.eclipse.microprofile.config.ConfigAccessor;

import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
import com.ibm.ws.microprofile.config14.interfaces.WebSphereConfigAccessor;
import com.ibm.ws.microprofile.config14.interfaces.WebSphereConfigSnapshot;

public class ConfigSnapshotImpl implements WebSphereConfigSnapshot {

    private final Map<String, SourcedValue> resolvedValues = new HashMap<>();

    /**
     * @param configValues
     */
    public ConfigSnapshotImpl(ConfigAccessor<?>... configValues) {
        for (ConfigAccessor<?> accessor : configValues) {
            WebSphereConfigAccessor<?> wAccessor = (WebSphereConfigAccessor<?>) accessor;
            SourcedValue sourcedValue = wAccessor.getSourcedValue();
            String name = accessor.getPropertyName();
            resolvedValues.put(name, sourcedValue);
        }
    }

    @Override
    public SourcedValue getSourcedValue(String propertyName) {
        SourcedValue resolved = null;

        if (resolvedValues.containsKey(propertyName)) {
            resolved = resolvedValues.get(propertyName);
        } else {
            //TODO NLS
            throw new IllegalArgumentException("Property not found in this snapshot: " + propertyName);
        }

        return resolved;
    }
}
