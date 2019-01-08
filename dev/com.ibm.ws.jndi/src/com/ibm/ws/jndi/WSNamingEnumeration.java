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

public final class WSNamingEnumeration<T> implements NamingEnumeration<T> {
    private final Iterator<?> iterator;
    private final Adapter<Object, T> adapter;

    public<F> WSNamingEnumeration(Iterable<F> from, Adapter<F, T> adapter){
        this.iterator = from.iterator();
        this.adapter = (Adapter<Object, T>)adapter; // ugly cast lets us avoid making F a type param on the class
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
            return null;
        }
    }

    @Override
    public T next() throws NamingException {
        return adapter.adapt(iterator.next());
    }

    @Override
    public boolean hasMore() {
        return iterator.hasNext();
    }

    @Override
    public void close() { /* no resources to clean up */ }
}
