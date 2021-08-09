/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/** A weak reference that remembers a key */
class WeakKeyedRef<K, V> extends WeakReference<V> implements KeyedRef<K, V> {
    private final K key;

    WeakKeyedRef(K key, V value, ReferenceQueue<V> q) {
        super(value, q);
        this.key = key;
    }

    @Override
    public K getKey() {
        return key;
    }
}
