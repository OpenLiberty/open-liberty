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

import java.util.ArrayList;

/**
 *
 */

public class KeyValueBooleanPair implements KeyValuePair {
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
    public boolean isList() {
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
        throw new UnsupportedOperationException("Cannot call getStringValue method on KeyValueBooleanPair class");
    }

    @Override
    public int getIntValue() {
        throw new UnsupportedOperationException("Cannot call getIntValue method on KeyValueBooleanPair class");
    }

    @Override
    public long getLongValue() {
        throw new UnsupportedOperationException("Cannot call getLongValue method on KeyValueBooleanPair class");
    }

    @Override
    public float getFloatValue() {
        throw new UnsupportedOperationException("Cannot call getFloatValue method on KeyValueBooleanPair class");
    }

    @Override
    public boolean getBooleanValue() {
        return value;
    }

    @Override
    public ArrayList<KeyValuePair> getList() {
        throw new UnsupportedOperationException("Cannot call getList method on KeyValueBooleanPair class");
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
