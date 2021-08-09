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
 * Class representing possible values for the HTTP header "Connection".
 */
public class ConnectionValues extends GenericKeys {

    /** Counter of the number of values defined so far */
    private static final AtomicInteger NEXT_ORDINAL = new AtomicInteger(0);
    /** List keeping track of all the values, used by the corresponding matcher */
    private static final List<ConnectionValues> allKeys = new ArrayList<ConnectionValues>();
    /** Matcher used for these enum objects */
    private static final KeyMatcher myMatcher = new KeyMatcher(false);

    // Note: cannot remove undefined because serialization/binary HTTP requires
    // existing ordinals not to change
    /** Enumerated object for an undefined Connection header value */
    public static final ConnectionValues UNDEF = new ConnectionValues("Undefined");
    /** Enumerated object for the Connection header not being set yet */
    public static final ConnectionValues NOTSET = new ConnectionValues("Not-Set");
    /** Enumerated object for the Connection header value Close */
    public static final ConnectionValues CLOSE = new ConnectionValues("Close");
    /** Enumerated object for the Connection header value Keep-Alive */
    public static final ConnectionValues KEEPALIVE = new ConnectionValues("Keep-Alive");
    /** Enumerated object for the Connection header value TE */
    public static final ConnectionValues TE = new ConnectionValues("TE");

    /** Flag on whether or not this instance is an undefined one */
    private boolean undefined = false;

    /**
     * Constructor that takes in the given String.
     * 
     * @param name
     */
    public ConnectionValues(String name) {
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
     * @return ConnectionValues
     */
    public static ConnectionValues getByOrdinal(int i) {
        return allKeys.get(i);
    }

    /**
     * Allow a comparison directly against another enum object.
     * 
     * @param val
     * @return int (0 if equal)
     */
    public int compareTo(ConnectionValues val) {
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
     * @return ConnectionValues
     */
    public static ConnectionValues match(String name, int offset, int length) {
        if (null == name)
            return null;
        return (ConnectionValues) myMatcher.match(name, offset, length);
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
     * @return ConnectionValues
     */
    public static ConnectionValues match(byte[] name, int offset, int length) {
        if (null == name)
            return null;
        return (ConnectionValues) myMatcher.match(name, offset, length);
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
     * @return ConnectionValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static ConnectionValues find(byte[] name, int offset, int length) {
        ConnectionValues key = (ConnectionValues) myMatcher.match(name, offset, length);
        if (null == key) {
            synchronized (ConnectionValues.class) {
                // protect against 2 threads getting here on the new value by
                // testing again inside a sync block
                key = (ConnectionValues) myMatcher.match(name, offset, length);
                if (null == key) {
                    key = new ConnectionValues(new String(name, offset, length));
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
     * @return ConnectionValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static ConnectionValues find(String name) {
        ConnectionValues key = (ConnectionValues) myMatcher.match(name, 0, name.length());
        if (null == key) {
            synchronized (ConnectionValues.class) {
                // protect against 2 threads getting here on the new value by
                // testing again inside a sync block
                key = (ConnectionValues) myMatcher.match(name, 0, name.length());
                if (null == key) {
                    key = new ConnectionValues(name);
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
     * @return ConnectionValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static ConnectionValues find(byte[] name) {
        return find(name, 0, name.length);
    }

}
