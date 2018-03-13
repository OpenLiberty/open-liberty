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
    private final Integer value;
    private final ValueTypes valueType;

    public KeyValueIntegerPair(String key, Integer value, ValueTypes valueType) {
        this.key = key;
        this.value = value;
        this.valueType = valueType;
    }

    @Override
    public boolean isString() {
        return (valueType == ValueTypes.STRING);
    }

    @Override
    public boolean isInteger() {
        return (valueType == ValueTypes.INTEGER);
    }

    @Override
    public boolean isLong() {
        return (valueType == ValueTypes.LONG);
    }

    @Override
    public ValueTypes getType() {
        return valueType;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() throws Exception {
        throw new Exception("Value is not String");
    }

    @Override
    public Integer getIntValue() throws Exception {
        return value;
    }

    @Override
    public Long getLongValue() throws Exception {
        throw new Exception("Value is not Long");
    }
}
