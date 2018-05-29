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
public class KeyValueLongPair implements KeyValuePair {

    private final String key;
    private final long value;

    public KeyValueLongPair(String key, long value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean isList() {
        return false;
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
        return true;
    }

    @Override
    public boolean isFloat() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public String getStringValue() {
        throw new UnsupportedOperationException("Cannot call getStringValue method on KeyValueLongPair class");
    }

    @Override
    public int getIntValue() {
        throw new UnsupportedOperationException("Cannot call getIntValue method on KeyValueLongPair class");
    }

    @Override
    public float getFloatValue() {
        throw new UnsupportedOperationException("Cannot call getFloatValue method on KeyValueLongPair class");
    }

    @Override
    public boolean getBooleanValue() {
        throw new UnsupportedOperationException("Cannot call getBooleanValue method on KeyValueLongPair class");
    }

    @Override
    public long getLongValue() {
        return value;
    }

    @Override
    public ValueTypes getType() {
        return ValueTypes.LONG;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public ArrayList<KeyValuePair> getList() {
        throw new UnsupportedOperationException("Cannot call getList method on KeyValueLongPair class");
    }
}
