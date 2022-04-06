/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.structures;

import java.util.LinkedHashMap;
import java.util.Map;

public class BoundedGenericHashMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 4601639448073802721L;

    private int MAX_ENTRIES = 10000;

    public BoundedGenericHashMap(int maxEntries) {
        super();
        if (maxEntries > 0) {
            MAX_ENTRIES = maxEntries;
        }
    }

    public BoundedGenericHashMap(int initSize, int maxEntries) {
        super(initSize);
        if (maxEntries > 0) {
            MAX_ENTRIES = maxEntries;
        }
    }

    public BoundedGenericHashMap() {
        super();
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > MAX_ENTRIES;
    }

}
