/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.app.manager.module.internal;

import java.util.AbstractMap;
import java.util.Map;

//@formatter:off
public class CollectionUtils {
    public static <K, V> Map.Entry<K, V> createEntry(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<K, V>(key, value);
    }

    public static <K, V> Map.Entry<K, V> anyOf(Map<K, V> map) {
        for ( Map.Entry<K, V> entry : map.entrySet() ) {
            return createEntry( entry.getKey(), entry.getValue() );
        }
        return null;
    }

    public static <K, V> K anyKeyOf(Map<K, V> map) {
        for ( Map.Entry<K, V> entry : map.entrySet() ) {
            return entry.getKey();
        }
        return null;
    }

    public static <K, V> V anyValueOf(Map<K, V> map) {
        for ( Map.Entry<K, V> entry : map.entrySet() ) {
            return entry.getValue();
        }
        return null;
    }
}
//@formatter:on
