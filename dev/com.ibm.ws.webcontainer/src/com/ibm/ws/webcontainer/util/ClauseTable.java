/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClauseTable {
    private final Map<String, ClauseNode> hashTable = new ConcurrentHashMap<String, ClauseNode>(16, .90f, 16);; //PK17266 changed variable name for clarity

    public ClauseTable() {}

    // PM06111 Start: Add methods to work with String kets 
    public ClauseNode get(String key) {
        return hashTable.get(key);
    }

    public void remove(String key) {
        hashTable.remove(key);
    }

    public void add(String key, ClauseNode item) {
        hashTable.put(key, item);
    }

    // PM06111 Start: Add methods to work with String kets 

    public ArrayList<ClauseNode> getList() {
        return new ArrayList<ClauseNode>(hashTable.values());
    }

    public int size() {
        return hashTable.size();
    }

}