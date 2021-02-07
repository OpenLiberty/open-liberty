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
package com.ibm.ws.cdi.beansxml.implicit.utils;

import java.util.ArrayList;
import java.util.List;

public class ChainableListImpl<E> extends ForwardingList<E> implements ChainableList<E> {

    /**
     * Create a list which forwards calls to a backing {@link ArrayList}.
     */
    public ChainableListImpl() {
        super(new ArrayList<E>());
    }

    /**
     * Create a list which forwards method calls to the given backing list.
     */
    public ChainableListImpl(List<E> backingList) {
        super(backingList);
    }

    @Override
    public ChainableList<E> chainAdd(E item) {
        add(item);
        return this;
    }

}
