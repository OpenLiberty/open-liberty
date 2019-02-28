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

public class GenericData {

    private final static int DEFAULT_SIZE = 16;

    private KeyValuePair[] pairs;

    private String sourceName;

    private String jsonMessage = null;

    private Integer lastIndex = -1;

    public GenericData() {
        pairs = new KeyValuePair[DEFAULT_SIZE];
    }

    public GenericData(int size) {
        pairs = new KeyValuePair[size];
    }

    public GenericData(String sourceName) {
        this(sourceName, DEFAULT_SIZE);
    }

    public GenericData(String sourceName, int size) {
        this.sourceName = sourceName;
        pairs = new KeyValuePair[size];
    }

    public void setPair(int index, String key, String value) {
        KeyValueStringPair kvp = new KeyValueStringPair(key, value);
        ensureCapacityAndSetPair(index, kvp);
    }

    public void setPair(int index, String key, int value) {
        KeyValueIntegerPair kvp = new KeyValueIntegerPair(key, value);
        ensureCapacityAndSetPair(index, kvp);
    }

    public void setPair(int index, String key, long value) {
        KeyValueLongPair kvp = new KeyValueLongPair(key, value);
        ensureCapacityAndSetPair(index, kvp);
    }

    public void setPair(int index, KeyValuePairList kvps) {
        ensureCapacityAndSetPair(index, kvps);

    }

    public void addPair(String key, String value) {
        KeyValueStringPair kvp = new KeyValueStringPair(key, value);
        ensureCapacityAndAddPair(kvp);
    }

    public void addPair(String key, int value) {
        KeyValueIntegerPair kvp = new KeyValueIntegerPair(key, value);
        ensureCapacityAndAddPair(kvp);
    }

    public void addPair(String key, long value) {
        KeyValueLongPair kvp = new KeyValueLongPair(key, value);
        ensureCapacityAndAddPair(kvp);
    }

    public void addPair(KeyValuePairList kvps) {
        ensureCapacityAndAddPair(kvps);
    }

    /* Resizes pairs array when full */
    private void ensureCapacityAndAddPair(KeyValuePair kvp) {
        if (lastIndex + 1 < pairs.length) {
            pairs[++lastIndex] = kvp;
        } else {
            pairs = java.util.Arrays.copyOf(pairs, Math.max(pairs.length + (pairs.length >> 1), DEFAULT_SIZE));
            pairs[++lastIndex] = kvp;
        }
    }

    /* Resizes pairs array when full */
    private void ensureCapacityAndSetPair(int index, KeyValuePair kvp) {
        if (index < pairs.length) {
            pairs[index] = kvp;
        } else {
            pairs = java.util.Arrays.copyOf(pairs, Math.max(index + (index >> 1), DEFAULT_SIZE));
            pairs[index] = kvp;
        }
        setLastIndex(index);
    }

    /* Ensures that addPair() will not override values set by setPair() */
    private void setLastIndex(int index) {
        lastIndex = (index > lastIndex) ? index : lastIndex;
    }

    protected String getStringValue(int index) {
        KeyValueStringPair kvp = (KeyValueStringPair) pairs[index];
        return kvp == null ? null : kvp.getStringValue();
    }

    protected int getIntValue(int index) {
        KeyValueIntegerPair kvp = (KeyValueIntegerPair) pairs[index];
        return kvp.getIntValue();
    }

    protected long getLongValue(int index) {
        KeyValueLongPair kvp = (KeyValueLongPair) pairs[index];
        return kvp.getLongValue();
    }

    public KeyValuePair[] getPairs() {
        return pairs;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        KeyValuePair kvp;
        String key;
        StringBuilder sb = new StringBuilder();
        String comma = ",";
        sb.append("GenericData [");
        /*
         * Current FAT tests currently query logs for type=<sourceName>
         *
         * The source name (e.g. com.ibm.ws.logging.source.message) is essentially the "type"
         * Do not confuse this with the "logging event type" (e.g. liberty_message) that is used for JSON logging
         * for the JSON data object
         *
         */
        sb.append("type=" + sourceName);
        for (KeyValuePair p : pairs) {
            if (p != null && !p.isList()) {
                kvp = p;
                key = kvp.getKey();
                sb.append(comma);
                if (sourceName.equals("com.ibm.ws.logging.ffdc.source.ffdcsource") && key.equals("ibm_threadId")) {
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