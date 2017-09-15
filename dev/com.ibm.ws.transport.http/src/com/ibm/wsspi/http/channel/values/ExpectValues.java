/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel.values;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.wsspi.genericbnf.GenericKeys;
import com.ibm.wsspi.genericbnf.KeyMatcher;

/**
 * Enumerated object class for the HTTP Expect header.
 */
public class ExpectValues extends GenericKeys {

    /** Counter of the number of values defined so far */
    private static final AtomicInteger NEXT_ORDINAL = new AtomicInteger(0);
    /** List keeping track of all the values, used by the corresponding matcher */
    private static final List<ExpectValues> allKeys = new ArrayList<ExpectValues>();
    /** Matcher used for these enum objects */
    private static final KeyMatcher myMatcher = new KeyMatcher(false);

    // Note: cannot remove undefined because serialization/binary HTTP requires
    // existing ordinals not to change
    /** Enumerated object for an undefined Expect header value */
    public static final ExpectValues UNDEF = new ExpectValues("Undefined");
    /** Enumerated object for the 100-continue Expect header value */
    public static final ExpectValues CONTINUE = new ExpectValues("100-continue");

    /** Flag on whether or not this instance is an undefined one */
    private boolean undefined = false;

    /**
     * Constructor that takes in the given name.
     * 
     * @param name
     */
    public ExpectValues(String name) {
        super(name, NEXT_ORDINAL.getAndIncrement());
        allKeys.add(this);
        myMatcher.add(this);
    }

    /**
     * Query whether this class instance is an undefined value or not.
     * 
     * @return boolean
     */
    public boolean isUndefined() {
        return this.undefined;
    }

    /**
     * Query the enumerated value that exists with the specified ordinal.
     * value.
     * 
     * @param i
     * @return ExpectValues
     */
    public static ExpectValues getByOrdinal(int i) {
        return allKeys.get(i);
    }

    /**
     * Allow a comparison directly against another enum object.
     * 
     * @param val
     * @return int (0 if equal)
     */
    public int compareTo(ExpectValues val) {
        return (null == val) ? -1 : getOrdinal() - val.getOrdinal();
    }

    /**
     * Find the enumerated object that matchs the input name using the given
     * offset and length into that name. If none exist, then a null value is
     * returned.
     * 
     * @param name
     * @param offset
     *            - starting point in that name
     * @param length
     *            - length to use from that starting point
     * @return ExpectValues
     */
    public static ExpectValues match(String name, int offset, int length) {
        if (null == name)
            return null;
        return (ExpectValues) myMatcher.match(name, offset, length);
    }

    /**
     * Find the enumerated object that matchs the input name using the given
     * offset and length into that name. If none exist, then a null value is
     * returned.
     * 
     * @param name
     * @param offset
     *            - starting point in that name
     * @param length
     *            - length to use from that offset
     * @return ExpectValues
     */
    public static ExpectValues match(byte[] name, int offset, int length) {
        if (null == name)
            return null;
        return (ExpectValues) myMatcher.match(name, offset, length);
    }

    /**
     * Find the enumerated object matching the input name. If this name has
     * never been seen prior, then a new object is created by this call.
     * 
     * @param name
     * @param offset
     *            - starting point in that input name
     * @param length
     *            - length to use from that offset
     * @return ExpectValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static ExpectValues find(byte[] name, int offset, int length) {
        ExpectValues key = (ExpectValues) myMatcher.match(name, offset, length);
        if (null == key) {
            synchronized (ExpectValues.class) {
                // protect against 2 threads getting here on the new value by
                // testing again inside a sync block
                key = (ExpectValues) myMatcher.match(name, offset, length);
                if (null == key) {
                    key = new ExpectValues(new String(name, offset, length));
                    key.undefined = true;
                }
            } // end-sync
        }
        return key;
    }

    /**
     * Find the enumerated object matching the input name. If this name has
     * never been seen prior, then a new object is created by this call.
     * 
     * @param name
     * @return ExpectValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static ExpectValues find(String name) {
        ExpectValues key = (ExpectValues) myMatcher.match(name, 0, name.length());
        if (null == key) {
            synchronized (ExpectValues.class) {
                // protect against 2 threads getting here on the new value by
                // testing again inside a sync block
                key = (ExpectValues) myMatcher.match(name, 0, name.length());
                if (null == key) {
                    key = new ExpectValues(name);
                    key.undefined = true;
                }
            } // end-sync
        }
        return key;
    }

    /**
     * Find the enumerated object matching the input name. If this name has
     * never been seen prior, then a new object is created by this call.
     * 
     * @param name
     * @return ExpectValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static ExpectValues find(byte[] name) {
        return find(name, 0, name.length);
    }

}
