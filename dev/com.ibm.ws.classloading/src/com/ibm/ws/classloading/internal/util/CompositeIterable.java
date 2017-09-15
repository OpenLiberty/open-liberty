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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Present multiple iterables as a single iterable. This class may be
 * safely used from multiple threads concurrently since it presents
 * an unmodifiable view of its component parts. However it is not
 * designed to be used in the face of concurrent modification of the
 * underlying iterables by other means. If the underlying iterables
 * are concurrent collections this may work but there are no
 * guarantees made about a single, consistent view during any one iteration.
 * 
 * 
 * @param <T>
 */
public class CompositeIterable<T> implements Iterable<T> {
    private final Iterable<Iterable<? extends T>> iterables;

    public CompositeIterable(Iterable<? extends T>... iterables) {
        this.iterables = Arrays.asList(iterables);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Iterator<Iterable<? extends T>> meta = iterables.iterator();
            Iterator<? extends T> current;

            private boolean ensureNextElement(Object... ignored) {
                return (current != null && current.hasNext()) || meta.hasNext() && ensureNextElement(current = meta.next().iterator());
            }

            private T fail() {
                throw new NoSuchElementException();
            }

            @Override
            public boolean hasNext() {
                return ensureNextElement();
            }

            @Override
            public T next() {
                return ensureNextElement() ? current.next() : fail();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Trivial
    @Override
    public String toString() {
        Iterator<?> it = iterables.iterator();
        if (!!!it.hasNext())
            return "()";
        StringBuilder buff = new StringBuilder("(").append(it.next());
        while (it.hasNext())
            buff.append("+").append(it.next());
        return buff.append(")").toString();
    }
}
