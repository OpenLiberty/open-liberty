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
public class KeyValueIntegerPair implements Pair, KeyValuePair {

    private final String key;
    private final int value;

    public KeyValueIntegerPair(String key, int value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public boolean isInteger() {
        return true;
    }

    @Override
    public boolean isLong() {
        return false;
    }

    @Override
    public String getStringValue() {
        throw new UnsupportedOperationException("Cannot call getStringValue method on KeyValueIntegerPair class");
    }

    @Override
    public Integer getIntValue() {
        return value;
    }

    @Override
    public Long getLongValue() {
        throw new UnsupportedOperationException("Cannot call getLongValue method on KeyValueIntegerPair class");
    }

    @Override
    public ValueTypes getType() {
        return ValueTypes.INTEGER;
    }

    @Override
    public String getKey() {
        return key;
    }
}
