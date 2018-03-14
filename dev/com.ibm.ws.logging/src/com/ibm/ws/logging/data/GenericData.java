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
import java.util.logging.Level;

public class GenericData {

    //Marker class, genericdatamemeber interface

    private final ArrayList<Pair> pairs;

    private String sourceType;

    private Level logRecordLevel = null;

    private String loggerName = null;

    private String jsonMessage = null;

    public GenericData() {
        pairs = new ArrayList<Pair>();
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

    public void addPairs(KeyValuePairList kvps) {
        pairs.add(kvps);
    }

    public ArrayList<Pair> getPairs() {
        return pairs;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public void setLogRecordLevel(Level logRecordLevel) {
        this.logRecordLevel = logRecordLevel;
    }

    public Level getLogRecordLevel() {
        return logRecordLevel;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getLoggerName() {
        return loggerName;
    }

    //Method created to accomodate some tests, must remove down the line
    public String getMessageID() {
        for (Pair p : pairs) {
            if (p instanceof KeyValuePair) {
                KeyValuePair kvp = (KeyValuePair) p;
                if (kvp.getKey().equals("ibm_messageId")) {
                    return kvp.getStringValue();
                }
            }
        }
        return "";
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
        for (Pair p : pairs) {
            if (p instanceof KeyValuePair) {
                kvp = (KeyValuePair) p;
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