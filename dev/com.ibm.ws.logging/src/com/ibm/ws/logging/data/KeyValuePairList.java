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

public class KeyValuePairList implements KeyValuePair {

    ArrayList<KeyValuePair> keyValuePairs;
    private final String key;

    public KeyValuePairList(String key) {
        this.key = key;
        keyValuePairs = new ArrayList<KeyValuePair>();
    }

    public void addKeyValuePair(String key, String value) {
        KeyValuePair kvp = new KeyValueStringPair(key, value);
        keyValuePairs.add(kvp);
    }

    public void addKeyValuePair(String key, int value) {
        KeyValuePair kvp = new KeyValueIntegerPair(key, value);
        keyValuePairs.add(kvp);
    }

    public void addKeyValuePair(String key, long value) {
        KeyValuePair kvp = new KeyValueLongPair(key, value);
        keyValuePairs.add(kvp);
    }

    public void addKeyValuePair(String key, boolean value) {
        KeyValuePair kvp = new KeyValueBooleanPair(key, value);
        keyValuePairs.add(kvp);
    }

    public void addKeyValuePair(String key, float value) {
        KeyValuePair kvp = new KeyValueFloatPair(key, value);
        keyValuePairs.add(kvp);
    }

    @Override
    public ArrayList<KeyValuePair> getList() {
        return keyValuePairs;
    }

    @Override
    public boolean isList() {
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
    public boolean isString() {
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
        throw new UnsupportedOperationException("Cannot call getStringValue method on KeyValueListPairList class");
    }

    @Override
    public int getIntValue() {
        throw new UnsupportedOperationException("Cannot call getIntValue method on KeyValueListPairList class");
    }

    @Override
    public long getLongValue() {
        throw new UnsupportedOperationException("Cannot call getLongValue method on KeyValueListPairList class");
    }

    @Override
    public float getFloatValue() {
        throw new UnsupportedOperationException("Cannot call getFloatValue method on KeyValueListPairList class");
    }

    @Override
    public boolean getBooleanValue() {
        throw new UnsupportedOperationException("Cannot call getFloatValue method on KeyValueListPairList class");
    }

    @Override
    public ValueTypes getType() {
        return ValueTypes.LIST;
    }

    @Override
    public String getKey() {
        return key;
    }
}
