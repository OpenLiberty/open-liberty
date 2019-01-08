/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class WSNamingEnumeration<T> implements NamingEnumeration<T> {
    private interface Enumerator<T> {
        boolean hasMoreElements();
        T next() throws NamingException;
    }

    public static <K, V, T> WSNamingEnumeration<T> getEnumeration(Map<K, V> entries, final Adapter<Entry<K, V>, T> adapter) {
        final Iterator<Entry<K, V>> iter = entries.entrySet().iterator();
        return new WSNamingEnumeration<T>(new Enumerator<T>() {
            public boolean hasMoreElements() {
                return iter.hasNext();
            }

            public T next() throws NamingException {
                return adapter.adapt(iter.next());
            }
        });
    }

    private final Enumerator<T> enumerator;

    private WSNamingEnumeration(Enumerator<T> enumerator) {
        this.enumerator = enumerator;
    }

    @Override
    public boolean hasMoreElements() {
        return enumerator.hasMoreElements();
    }

    @Override
    public T nextElement() {
        try {
            return enumerator.next();
        } catch (NamingException e) {
            return null;
        }
    }

    @Override
    public T next() throws NamingException {
        return enumerator.next();
    }

    @Override
    public boolean hasMore() {
        return enumerator.hasMoreElements();
    }

    @Override
    public void close() { /* no resources to clean up */ }
}
