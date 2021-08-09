/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.utils;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SessionHashSet extends AbstractSet {

    Object[] keys;

    public SessionHashSet(Object[] keys) {
        this.keys = keys;
    }

    public Iterator iterator() {
        return getSessionHashIterator();
    }

    public int size() {
        if (keys == null) {
            return 0;
        }
        return keys.length;
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    private Iterator getSessionHashIterator() {
        if (size() == 0) {
            return sessionEmptyHashIterator;
        } else {
            return new SessionHashIterator();
        }
    }

    private static SessionEmptyHashIterator sessionEmptyHashIterator = new SessionEmptyHashIterator();

    // Internal class

    private static class SessionEmptyHashIterator implements Iterator {

        SessionEmptyHashIterator() {

        }

        public boolean hasNext() {
            return false;
        }

        public Object next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class SessionHashIterator implements Iterator {

        int current = 0;;
        int end = keys.length;

        SessionHashIterator() {}

        // Does hasNext actually consume an entry without next?
        public boolean hasNext() {
            if (current < end) {
                return true;
            }
            return false;
        }

        public Object next() {

            if (current >= end) {
                throw new NoSuchElementException();
            }

            Object theObject = keys[current];
            current++;
            return theObject;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
