/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.utils;

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
