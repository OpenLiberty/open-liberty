/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.util.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class UtilImpl_EmptyStringSet implements Set<String> {
    public static final UtilImpl_EmptyStringSet INSTANCE = new UtilImpl_EmptyStringSet();
    public static final String[] INSTANCE_ARRAY = new String[] {};

    public static final EmptyStringIterator INSTANCE_ITERATOR = new EmptyStringIterator();

    public static final class EmptyStringIterator implements Iterator<String> {
        public boolean hasNext() {
            return false;
        }

        public String next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public boolean add(String object) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends String> collection) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean contains(Object object) {
        return false;
    }

    public boolean containsAll(Collection<?> collection) {
        return (collection.isEmpty());
    }

    public boolean isEmpty() {
        return true;
    }

    public Iterator<String> iterator() {
        return UtilImpl_EmptyStringSet.INSTANCE_ITERATOR;
    }

    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    public int size() {
        return 0;
    }

    public Object[] toArray() {
        return UtilImpl_EmptyStringSet.INSTANCE_ARRAY;
    }

    public <T> T[] toArray(T[] array) {
        return array;
    }

}
