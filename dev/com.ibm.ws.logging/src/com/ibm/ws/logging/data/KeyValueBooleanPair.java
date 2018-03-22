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
package com.ibm.ws.logging.data;

/**
 *
 */

public class KeyValueBooleanPair implements Pair, KeyValuePair {
    private final String key;
    private final boolean value;

    public KeyValueBooleanPair(String key, boolean value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public boolean isInteger() {
        return false;
    }

    @Override
    public boolean isLong() {
        return false;
    }

    @Override
    public boolean isFloat() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return true;
    }

    @Override
    public String getStringValue() {
        throw new UnsupportedOperationException("Cannot call getStringValue method on KeyValueFloatPair class");
    }

    @Override
    public Integer getIntValue() {
        throw new UnsupportedOperationException("Cannot call getStringValue method on KeyValueFloatPair class");
    }

    @Override
    public Long getLongValue() {
        throw new UnsupportedOperationException("Cannot call getLongValue method on KeyValueFloatPair class");
    }

    @Override
    public Float getFloatValue() {
        throw new UnsupportedOperationException("Cannot call getLongValue method on KeyValueFloatPair class");
    }

    @Override
    public Boolean getBooleanValue() {
        return value;
    }

    @Override
    public ValueTypes getType() {
        return ValueTypes.BOOLEAN;
    }

    @Override
    public String getKey() {
        return key;
    }
}
