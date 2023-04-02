/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.impl;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.ws.microprofile.config.interfaces.SourcedValue;

public class TypeCache {
    private final String key;
    private final SourcedValue rawValue;
    private final Map<Type, SourcedValue> convertedValues = new ConcurrentHashMap<>();

    public TypeCache(SourcedValue rawValue) {
        this.key = rawValue.getKey();
        this.rawValue = rawValue;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the rawValue
     */
    public SourcedValue getRawValue() {
        return rawValue;
    }

    /**
     * @return the convertedValues
     */
    public Map<Type, SourcedValue> getConvertedValues() {
        return convertedValues;
    }
}