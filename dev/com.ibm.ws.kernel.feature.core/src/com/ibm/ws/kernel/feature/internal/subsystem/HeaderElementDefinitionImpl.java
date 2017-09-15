/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.subsystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.ws.kernel.feature.provisioning.HeaderElementDefinition;

public class HeaderElementDefinitionImpl implements HeaderElementDefinition {
    private final String _symbolicName;
    private final Map<String, String> _rawAttributes;
    private final AtomicReference<Map<String, String>> _attributes = new AtomicReference<Map<String, String>>();
    private final AtomicReference<Map<String, String>> _directives = new AtomicReference<Map<String, String>>();

    public HeaderElementDefinitionImpl(String newName, Map<String, String> attributes) {
        _symbolicName = newName;
        _rawAttributes = attributes;
    }

    @Override
    public String getSymbolicName() {
        return _symbolicName;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> result = _attributes.get();

        if (result == null) {
            result = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : _rawAttributes.entrySet()) {
                String key = entry.getKey();
                if (!!!key.endsWith(":")) {
                    result.put(key, entry.getValue());
                }
            }

            result = Collections.unmodifiableMap(result);

            if (!!!_attributes.compareAndSet(null, result)) {
                result = _attributes.get();
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getDirectives() {
        Map<String, String> result = _directives.get();

        if (result == null) {
            result = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : _rawAttributes.entrySet()) {
                String key = entry.getKey();
                if (key.endsWith(":")) {
                    result.put(key.substring(0, key.length() - 1), entry.getValue());
                }
            }

            result = Collections.unmodifiableMap(result);

            if (!!!_directives.compareAndSet(null, result)) {
                result = _directives.get();
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return _symbolicName + '=' + _rawAttributes.toString();
    }
}
