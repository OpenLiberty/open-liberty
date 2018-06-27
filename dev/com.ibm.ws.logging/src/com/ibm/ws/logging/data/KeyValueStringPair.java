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

public class KeyValueStringPair implements KeyValuePair {

    private final String key;
    private final String value;

    public KeyValueStringPair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean isList() {
        return false;
    }

    @Override
    public boolean isString() {
        return true;
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
        return false;
    }

    @Override
    public String getStringValue() {
        return value;
    }

    @Override
    public int getIntValue() {
        throw new UnsupportedOperationException("Cannot call getIntValue method on KeyValueStringPair class");
    }

    @Override
    public long getLongValue() {
        throw new UnsupportedOperationException("Cannot call getLongValue method on KeyValueStringPair class");
    }

    @Override
    public float getFloatValue() {
        throw new UnsupportedOperationException("Cannot call getFloatValue method on KeyValueStringPair class");
    }

    @Override
    public boolean getBooleanValue() {
        throw new UnsupportedOperationException("Cannot call getBooleanValue method on KeyValueStringPair class");
    }

    @Override
    public ValueTypes getType() {
        return ValueTypes.STRING;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public ArrayList<KeyValuePair> getList() {
        throw new UnsupportedOperationException("Cannot call getList method on KeyValueStringPair class");
    }
}
