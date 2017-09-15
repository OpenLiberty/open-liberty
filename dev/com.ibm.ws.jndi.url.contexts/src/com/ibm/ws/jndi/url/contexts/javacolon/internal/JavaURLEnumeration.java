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
package com.ibm.ws.jndi.url.contexts.javacolon.internal;

import java.util.Iterator;
import java.util.Set;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * @param <T>
 * 
 */
public class JavaURLEnumeration<T extends NameClassPair> implements NamingEnumeration<T> {

    private final Iterator<T> delegate;

    /**
     * @param allInstances
     */
    public JavaURLEnumeration(Set<T> allInstances) {
        this.delegate = allInstances.iterator();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasMoreElements() {
        return delegate.hasNext();
    }

    /** {@inheritDoc} */
    @Override
    public T nextElement() {
        return delegate.next();
    }

    /** {@inheritDoc} */
    @Override
    public T next() throws NamingException {
        return delegate.next();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasMore() throws NamingException {
        return delegate.hasNext();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws NamingException {
        // Ignore, nothing to do

    }

}
