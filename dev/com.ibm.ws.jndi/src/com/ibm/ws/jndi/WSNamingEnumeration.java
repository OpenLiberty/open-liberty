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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

public class WSNamingEnumeration<T> implements NamingEnumeration<T> {
    private final Iterator<T> iterator;

    private NamingException exception;

    public static <K, V, T> WSNamingEnumeration<T> getEnumeration(Map<K, V> entries, Adapter<Entry<K, V>, T> adapter) {
        return new WSNamingEnumeration<T>(entries.entrySet().iterator(), adapter);
    }

    private <F> WSNamingEnumeration(final Iterator<F> fromIterator, final Adapter<? super F, ? extends T> adapter) {
        this.iterator = new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return fromIterator.hasNext();
            }

            @Override
            public T next() {
                try {
                    return adapter.adapt(fromIterator.next());
                } catch (NamingException e) { // cannot re-throw, so just suppress this exception
                    if (exception == null)
                        exception = e;
                    return null;
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean hasMoreElements() {
        return iterator.hasNext();
    }

    @Override
    public T nextElement() {
        try {
            return next();
        } catch (NamingException e) {
            // cannot re-throw, so just suppress this exception
            if (exception == null)
                exception = e;
            return null;
        }
    }

    @Override
    public T next() throws NamingException {
        return iterator.next();
    }

    @Override
    public boolean hasMore() throws NamingException {
        if (iterator.hasNext())
            return true;
        if (exception != null)
            throw exception;
        return false;
    }

    @Override
    public void close() throws NamingException {} // ignore calls to close because we have no resources to clean up
}
