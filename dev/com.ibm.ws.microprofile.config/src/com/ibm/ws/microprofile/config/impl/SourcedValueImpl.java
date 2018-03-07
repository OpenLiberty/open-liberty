/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.impl;

import java.lang.reflect.Type;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.interfaces.SourcedValue;

/**
 * A value, the type of the value and the id of its source
 */
public class SourcedValueImpl implements SourcedValue {

    private final Object value;
    private final Type type;
    private final String source;
    private final String key;

    @Trivial
    public SourcedValueImpl(String key, Object value, Type type, String source) {
        this.key = key;
        this.value = value;
        this.type = type;
        this.source = source;
    }

    /**
     * Get the key
     *
     * @return the key
     */
    @Override
    @Trivial
    public String getKey() {
        return key;
    }

    /**
     * Get the actual value
     *
     * @return the value
     */
    @Override
    @Trivial
    public Object getValue() {
        return value;
    }

    /**
     * Get the type of the value
     *
     * @return
     */
    @Override
    @Trivial
    public Type getType() {
        return type;
    }

    /**
     * Get the ID of the source that provided the value
     *
     * @return the originating source ID
     */
    @Override
    @Trivial
    public String getSource() {
        return source;
    }

    @Override
    @Trivial
    public String toString() {
        return "[" + source + "; " + type + "] " + key + "=" + value;
    }
}
