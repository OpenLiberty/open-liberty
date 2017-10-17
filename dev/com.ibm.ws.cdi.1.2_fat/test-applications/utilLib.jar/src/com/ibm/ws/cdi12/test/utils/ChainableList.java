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

import java.util.List;

/**
 * A list which extra methods for chaining method calls.
 */
public interface ChainableList<E> extends List<E> {

    /**
     * Adds an item to the list and returns the list. Useful for making code more concise. For example:
     * <pre> {@code
     * List<String> list = getList();
     * list.add(item);
     * return list;
     * } </pre>
     * <p>can be replaced by:
     * <p>{@code return list.chainAdd(item);}
     */
    ChainableList<E> chainAdd(E item);
}
