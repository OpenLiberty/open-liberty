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
 * Class representing possible values for the HTTP header "Transfer-Encoding".
 */
public class TransferEncodingValues extends GenericKeys {

    /** Counter of the number of values defined so far */
    private static final AtomicInteger NEXT_ORDINAL = new AtomicInteger(0);
    /** List keeping track of all the values, used by the corresponding matcher */
    private static final List<TransferEncodingValues> allKeys = new ArrayList<TransferEncodingValues>();
    /** Matcher used for these enum objects */
    private static final KeyMatcher myMatcher = new KeyMatcher(false);

    // Note: cannot remove undefined because serialization/binary HTTP requires
    // existing ordinals not to change
    /** Enumerated object for an undefined Transfer-Encoding header value */
    public static final TransferEncodingValues UNDEF = new TransferEncodingValues("Undefined");
    /** Enumerated object for the Transfer-Encoding header value not set */
    public static final TransferEncodingValues NOTSET = new TransferEncodingValues("Not-Set");
    /** Enumerated object for the Transfer-Encoding header value gzip */
    public static final TransferEncodingValues GZIP = new TransferEncodingValues("gzip");
    /** Enumerated object for the Transfer-Encoding header value x-gzip */
    public static final TransferEncodingValues XGZIP = new TransferEncodingValues("x-gzip");
    /** Enumerated object for the Transfer-Encoding header value compress */
    public static final TransferEncodingValues COMPRESS = new TransferEncodingValues("compress");
    /** Enumerated object for the Transfer-Encoding header value x-compress */
    public static final TransferEncodingValues XCOMPRESS = new TransferEncodingValues("x-compress");
    /** Enumerated object for the Transfer-Encoding header value deflate */
    public static final TransferEncodingValues DEFLATE = new TransferEncodingValues("deflate");
    /** Enumerated object for the Transfer-Encoding header value identity */
    public static final TransferEncodingValues IDENTITY = new TransferEncodingValues("identity");
    /** Enumerated object for the Transfer-Encoding header value chunked */
    public static final TransferEncodingValues CHUNKED = new TransferEncodingValues("chunked");

    /** Flag on whether or not this instance is an undefined one */
    private boolean undefined = false;

    /**
     * Default constructor.
     * 
     * @param name
     *            The name of the allowable Transfer-Encoding value.
     */
    public TransferEncodingValues(String name) {
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
     * @return TransferEncodingValues
     * @throws IndexOutOfBoundsException
     */
    public static TransferEncodingValues getByOrdinal(int i) {
        return allKeys.get(i);
    }

    /**
     * Allow a comparison directly against another enum object.
     * 
     * @param val
     * @return int (0 if equal)
     */
    public int compareTo(TransferEncodingValues val) {
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
     * @return TransferEncodingValues
     */
    public static TransferEncodingValues match(String name, int offset, int length) {
        if (null == name)
            return null;
        return (TransferEncodingValues) myMatcher.match(name, offset, length);
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
     * @return TransferEncodingValues - null only if the input name is null
     */
    public static TransferEncodingValues match(byte[] name, int offset, int length) {
        if (null == name)
            return null;
        return (TransferEncodingValues) myMatcher.match(name, offset, length);
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
     * @return TransferEncodingValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static TransferEncodingValues find(byte[] name, int offset, int length) {
        TransferEncodingValues key = (TransferEncodingValues) myMatcher.match(name, offset, length);
        if (null == key) {
            synchronized (TransferEncodingValues.class) {
                // protect against 2 threads getting here on the new value by
                // testing again inside a sync block
                key = (TransferEncodingValues) myMatcher.match(name, offset, length);
                if (null == key) {
                    key = new TransferEncodingValues(new String(name, offset, length));
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
     * @return TransferEncodingValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static TransferEncodingValues find(String name) {
        TransferEncodingValues key = (TransferEncodingValues) myMatcher.match(name, 0, name.length());
        if (null == key) {
            synchronized (TransferEncodingValues.class) {
                // protect against 2 threads getting here on the new value by
                // testing again inside a sync block
                key = (TransferEncodingValues) myMatcher.match(name, 0, name.length());
                if (null == key) {
                    key = new TransferEncodingValues(name);
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
     * @return TransferEncodingValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static TransferEncodingValues find(byte[] name) {
        return find(name, 0, name.length);
    }

}