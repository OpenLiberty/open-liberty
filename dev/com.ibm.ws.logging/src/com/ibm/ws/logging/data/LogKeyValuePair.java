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

public class LogKeyValuePair {

    public enum DataValueTypes {
        STRING, NUMBER
    }

    private final String key;
    private final String value;
    private final DataValueTypes valueType;

    public LogKeyValuePair(String key, String value, DataValueTypes valueType) {
        this.key = key;
        this.value = value;
        this.valueType = valueType;
    }

    public DataValueTypes getValueType() {
        return valueType;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}
