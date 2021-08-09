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
package com.ibm.wsspi.kernel.service.utils;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Simple class to wrap several enumerations and make them look like one, as
 * opposed to iterating them all up front and putting them into a new list.
 * <p>
 * Only use this class if you need to work with Enumerations, e.g. because you
 * are working with an old API.
 */
public class CompositeEnumeration<T> implements Enumeration<T> {
    private final List<Enumeration<T>> enumerations = new ArrayList<Enumeration<T>>();
    private int index = 0;

    /**
     * Create the enumeration wrapping a single enumeration.
     */
    public CompositeEnumeration(Enumeration<T> first) {
        enumerations.add(first);
    }

    public CompositeEnumeration() {}

    /**
     * Fluent method for chaining additions of subsequent enumerations.
     */
    public CompositeEnumeration<T> add(Enumeration<T> enumeration) {
        // optimise out empty enumerations up front
        if (enumeration.hasMoreElements())
            enumerations.add(enumeration);
        return this;
    }

    /*
     * @see java.util.Enumeration#hasMoreElements()
     */
    @Override
    public boolean hasMoreElements() {
        // wind forward to the next non-empty enumeration
        while (index < enumerations.size() && !!!enumerations.get(index).hasMoreElements())
            index++;
        // return true iff there was a non-empty enumeration
        return index < enumerations.size();
    }

    @Override
    public T nextElement() {
        // use exceptional path here because we are not expecting to 
        // get a NoSuchElementException
        if (index >= enumerations.size())
            throw new NoSuchElementException();
        try {
            return enumerations.get(index).nextElement();
        } catch (NoSuchElementException e) {
            // increment and recurse
            index++;
            return nextElement();
        }
    }
}
