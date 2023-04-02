/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class CacheHashMap<K, V>
                extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = -5771674458745168836L;

    private final int ivMaxSize;

    public CacheHashMap(int maxSize) {
        this(maxSize, 16, .75f, true);
    }

    public CacheHashMap(int maxSize, int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
        ivMaxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > ivMaxSize;
    }
}
