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
public class KeyValueLongPair implements Pair, KeyValuePair {

    private final String key;
    private final long value;

    public KeyValueLongPair(String key, long value) {
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
        return true;
    }

    @Override
    public String getStringValue() {
        throw new UnsupportedOperationException("Cannot call getStringValue method on KeyValueLongPair class");
    }

    @Override
    public Integer getIntValue() {
        throw new UnsupportedOperationException("Cannot call getIntValue method on KeyValueLongPair class");
    }

    @Override
    public Long getLongValue() {
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
}
