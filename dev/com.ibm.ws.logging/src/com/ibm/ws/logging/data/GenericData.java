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

public class GenericData {

    private final ArrayList<KeyValuePair> pairs;

    private String sourceType;

    private String jsonMessage = null;

    public GenericData() {
        pairs = new ArrayList<KeyValuePair>();
    }

    public GenericData(int size) {
        pairs = new ArrayList<KeyValuePair>(size);
    }

    public void modifyPair(int index, String key, String value) {
        KeyValueStringPair kvp = new KeyValueStringPair(key, value);
        pairs.set(index, kvp);
    }

    public void setPair(int index, String key, String value) {
        KeyValueStringPair kvp = new KeyValueStringPair(key, value);
        pairs.add(index, kvp);
    }

    public void setPair(int index, String key, int value) {
        KeyValueIntegerPair kvp = new KeyValueIntegerPair(key, value);
        pairs.add(index, kvp);
    }

    public void setPair(int index, String key, long value) {
        KeyValueLongPair kvp = new KeyValueLongPair(key, value);
        pairs.add(index, kvp);
    }

    public void setPair(int index, KeyValuePairList kvps) {
        pairs.add(index, kvps);
    }

    public void addPair(String key, String value) {
        KeyValueStringPair kvp = new KeyValueStringPair(key, value);
        pairs.add(kvp);
    }

    public void addPair(String key, int value) {
        KeyValueIntegerPair kvp = new KeyValueIntegerPair(key, value);
        pairs.add(kvp);
    }

    public void addPair(String key, long value) {
        KeyValueLongPair kvp = new KeyValueLongPair(key, value);
        pairs.add(kvp);
    }

    public void addPair(KeyValuePairList kvps) {
        pairs.add(kvps);
    }

    public ArrayList<KeyValuePair> getPairs() {
        return pairs;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        KeyValuePair kvp;
        String key;
        StringBuilder sb = new StringBuilder();
        String comma = ",";
        sb.append("GenericData [");
        sb.append("type=" + sourceType);
        for (KeyValuePair p : pairs) {
            if (p != null && !p.isList()) {
                kvp = p;
                key = kvp.getKey();
                sb.append(comma);
                if (sourceType.equals("com.ibm.ws.logging.ffdc.source.ffdcsource") && key.equals("ibm_threadId")) {
                    key = "threadID";
                }
                if (kvp.isInteger()) {
                    sb.append(key + "=" + kvp.getIntValue());
                } else if (kvp.isLong()) {
                    sb.append(key + "=" + kvp.getLongValue());
                } else {
                    sb.append(key + "=" + kvp.getStringValue());

                }
            }
        }

        sb.append("]");
        return sb.toString();
    }

    public String getJsonMessage() {
        return jsonMessage;
    }

    public void setJsonMessage(String jsonMessage) {
        this.jsonMessage = jsonMessage;
    }
}