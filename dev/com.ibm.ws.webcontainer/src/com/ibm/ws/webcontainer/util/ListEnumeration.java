/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

public class ListEnumeration<T> implements Enumeration<T> {
    private final List<T> list;
    private final int size;
    private int index = 0;

    /**
     * Constructor for ListEnumeration.
     * 
     * @param list
     */
    public ListEnumeration(List<T> list) {
        this.list = list;
        this.size = list.size();
    }

    /**
     * @return boolean
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements() {
        return index < size;
    }

    /**
     * @return Object
     * @see java.util.Enumeration#nextElement()
     */
    public T nextElement() {
        if (index >= size) {
            throw new NoSuchElementException();
        }
        return list.get(index++);
    }
}
