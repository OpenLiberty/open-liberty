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

public class WSNamingEnumeration<T> implements NamingEnumeration<T> {
    private interface Enumerator<T> {
        boolean hasMoreElements();
        T next() throws NamingException;
    }

    private final Enumerator<T> enumerator;

    public<F> WSNamingEnumeration(Iterable<F> from, final Adapter<F, T> adapter){
        final Iterator<F> iter = from.iterator();
        this.enumerator = new Enumerator<T>() {
            public boolean hasMoreElements() {
                return iter.hasNext();
            }

            public T next() throws NamingException {
                return adapter.adapt(iter.next());
            }
        };
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
