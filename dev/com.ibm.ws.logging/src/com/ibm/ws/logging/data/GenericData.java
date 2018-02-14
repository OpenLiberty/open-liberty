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

    //Marker class, genericdatamemeber interface

    private final ArrayList<Pair> pairs;

    private String sourceType;

    public GenericData() {
        pairs = new ArrayList<Pair>();
    }

    public void addPair(String key, String value) {
        KeyValuePair kvp = new KeyValuePair(key, value, KeyValuePair.ValueTypes.STRING);
        pairs.add(kvp);
    }

    public void addPair(String key, Number value) {
        KeyValuePair kvp = new KeyValuePair(key, value.toString(), KeyValuePair.ValueTypes.NUMBER);
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

    //Method created to accomodate some tests, must remove down the line
    public String getMessageID() {
        for (Pair p : pairs) {
            if (p instanceof KeyValuePair) {
                KeyValuePair kvp = (KeyValuePair) p;
                if (kvp.getKey().equals("ibm_messageId")) {
                    return kvp.getValue();
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
        String val;
        StringBuilder sb = new StringBuilder();
        String comma = ",";
        sb.append("GenericData [");
        sb.append("type=" + sourceType);
        for (Pair p : pairs) {
            if (p instanceof KeyValuePair) {
                kvp = (KeyValuePair) p;
                key = kvp.getKey();
                val = kvp.getValue();
                sb.append(comma);
                if (sourceType.equals("com.ibm.ws.logging.ffdc.source.ffdcsource") && key.equals("ibm_threadId")) {
                    key = "threadID";
                }
                sb.append(key + "=" + val);
            }
        }
        sb.append("]");
        return sb.toString();
    }

}