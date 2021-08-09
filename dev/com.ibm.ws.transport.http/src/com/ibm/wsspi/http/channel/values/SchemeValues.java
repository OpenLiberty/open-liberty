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
 * Class representing possible values for the HTTP Request scheme.
 */
public class SchemeValues extends GenericKeys {

    /** Counter of the number of values defined so far */
    private static final AtomicInteger NEXT_ORDINAL = new AtomicInteger(0);
    /** List keeping track of all the values, used by the corresponding matcher */
    private static final List<SchemeValues> allKeys = new ArrayList<SchemeValues>();
    /** Matcher used for these enum objects */
    private static final KeyMatcher myMatcher = new KeyMatcher(false);

    // Note: cannot remove undefined because serialization/binary HTTP requires
    // existing ordinals not to change
    /** Enumerated object for an undefined scheme */
    public static final SchemeValues UNDEF = new SchemeValues("Undefined");
    /** Enumerated object for the scheme HTTP */
    public static final SchemeValues HTTP = new SchemeValues("http");
    /** Enumerated object for the scheme HTTPS */
    public static final SchemeValues HTTPS = new SchemeValues("https");
    /** Enumerated object for the scheme FTP */
    public static final SchemeValues FTP = new SchemeValues("ftp");

    /** Flag on whether or not this instance is an undefined one */
    private boolean undefined = false;

    /**
     * Constructor to convert the given String into a SchemeValues object.
     * 
     * @param name
     */
    public SchemeValues(String name) {
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
     * Query the enumerated value that exists with the specified ordinal
     * value.
     * 
     * @param i
     * @return SchemeValues
     * @throws IndexOutOfBoundsException
     */
    public static SchemeValues getByOrdinal(int i) {
        return allKeys.get(i);
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
     * @return SchemeValues
     */
    public static SchemeValues match(String name, int offset, int length) {
        if (null == name)
            return null;
        return (SchemeValues) myMatcher.match(name, offset, length);
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
     * @return SchemeValues - null only if the input name is null
     */
    public static SchemeValues match(byte[] name, int offset, int length) {
        if (null == name)
            return null;
        return (SchemeValues) myMatcher.match(name, offset, length);
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
     * @return SchemeValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static SchemeValues find(byte[] name, int offset, int length) {
        SchemeValues key = (SchemeValues) myMatcher.match(name, offset, length);
        if (null == key) {
            synchronized (SchemeValues.class) {
                // protect against 2 threads getting here on the new value by
                // testing again inside a sync block
                key = (SchemeValues) myMatcher.match(name, offset, length);
                if (null == key) {
                    key = new SchemeValues(new String(name, offset, length));
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
     * @return SchemeValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static SchemeValues find(String name) {
        SchemeValues key = (SchemeValues) myMatcher.match(name, 0, name.length());
        if (null == key) {
            synchronized (SchemeValues.class) {
                // protect against 2 threads getting here on the new value by
                // testing again inside a sync block
                key = (SchemeValues) myMatcher.match(name, 0, name.length());
                if (null == key) {
                    key = new SchemeValues(name);
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
     * @return SchemeValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static SchemeValues find(byte[] name) {
        return find(name, 0, name.length);
    }

}
