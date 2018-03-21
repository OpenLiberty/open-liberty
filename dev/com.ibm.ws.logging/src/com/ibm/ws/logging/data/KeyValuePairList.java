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

public class KeyValuePairList implements Pair {

    ArrayList<KeyValuePair> keyValuePairs;

    public KeyValuePairList() {
        keyValuePairs = new ArrayList<KeyValuePair>();
    }

    public ArrayList<KeyValuePair> getKeyValuePairs() {
        return keyValuePairs;
    }

    public void addPair(String key, String value) {
        KeyValuePair kvp = new KeyValueStringPair(key, value);
        keyValuePairs.add(kvp);
    }

    public void addPair(String key, int value) {
        KeyValuePair kvp = new KeyValueIntegerPair(key, value);
        keyValuePairs.add(kvp);
    }

    public void addPair(String key, long value) {
        KeyValuePair kvp = new KeyValueLongPair(key, value);
        keyValuePairs.add(kvp);
    }
}
