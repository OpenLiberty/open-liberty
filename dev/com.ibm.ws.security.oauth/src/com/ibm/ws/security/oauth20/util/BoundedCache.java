/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class extends LinkedHashMap and implements a FIFO cache
 * controlled by the max capacity. The enforcement of the max
 * capacity constraint is provided by the @Override of
 * {@link #removeEldestEntry(java.util.Map.Entry)}, a control-point
 * provided by the class implementation for doing FICO caches.
 */
@SuppressWarnings("serial")
public class BoundedCache<K, V> extends LinkedHashMap<K, V> {

    // Default max capacity is 1000 entries
    private int maxCapacity = 1000;

    public BoundedCache(int maxCapacity) {
        super();
        this.maxCapacity = maxCapacity;
    }

    public BoundedCache() {
        super();
    }

    /**
     * {@inheritDoc}
     * If we're placing a new key and we're above capacity
     * then we need to purge the oldest entry (FIFO)
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity;
    }

}
