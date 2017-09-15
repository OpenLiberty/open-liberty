/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * Utility class that will create an Enumeration wrapper.
 * 
 * @param <T>
 * 
 */
public class GenericEnumWrap<T> implements Enumeration<T> {

    /** The wrapped iterator object */
    private Iterator<T> myIterator = null;
    /** Singleton object instead of an iterator */
    private T singleton = null;

    /**
     * Constructor that wraps an Iterator object.
     * 
     * @param iter
     */
    public GenericEnumWrap(Iterator<T> iter) {
        this.myIterator = iter;
    }

    /**
     * Provide an enumeration wrapper for a single object.
     * 
     * @param item
     */
    public GenericEnumWrap(T item) {
        this.singleton = item;
    }

    /*
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements() {
        if (null == this.myIterator) {
            return (null == this.singleton);
        }
        return this.myIterator.hasNext();
    }

    /*
     * @see java.util.Enumeration#nextElement()
     */
    public T nextElement() {
        T rc = null;
        if (null == this.myIterator) {
            rc = this.singleton;
            this.singleton = null;
        } else {
            rc = this.myIterator.next();
        }
        return rc;
    }
}