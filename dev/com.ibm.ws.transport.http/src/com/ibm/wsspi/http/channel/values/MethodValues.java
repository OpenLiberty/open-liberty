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
 * Class representing possible values for the HTTP Request method
 */
public class MethodValues extends GenericKeys {

    /** Counter of the number of values defined so far */
    private static final AtomicInteger NEXT_ORDINAL = new AtomicInteger(0);
    /** List keeping track of all the values, used by the corresponding matcher */
    private static final List<MethodValues> allKeys = new ArrayList<MethodValues>();
    /** Matcher used for these enum objects */
    private static final KeyMatcher myMatcher = new KeyMatcher(false);

    // Note: cannot remove undefined because serialization/binary HTTP requires
    // existing ordinals not to change
    /** Enumerated object for an undefined method string */
    public static final MethodValues UNDEF = new MethodValues("Undefined", true);
    /** Enumerated object for the method CONNECT */
    public static final MethodValues CONNECT = new MethodValues("CONNECT", true);
    /** Enumerated object for the method DELETE */
    public static final MethodValues DELETE = new MethodValues("DELETE", true);
    /** Enumerated object for the method GET */
    public static final MethodValues GET = new MethodValues("GET", true);
    /** Enumerated object for the method HEAD */
    public static final MethodValues HEAD = new MethodValues("HEAD", true);
    /** Enumerated object for the method OPTIONS */
    public static final MethodValues OPTIONS = new MethodValues("OPTIONS", true);
    /** Enumerated object for the method POST */
    public static final MethodValues POST = new MethodValues("POST", true);
    /** Enumerated object for the method PUT */
    public static final MethodValues PUT = new MethodValues("PUT", true);
    /** Enumerated object for the method TRACE */
    public static final MethodValues TRACE = new MethodValues("TRACE", false);

    // WebDAV methods below

    /** Enumerated object for the WebDAV method PROPFIND */
    public static final MethodValues PROPFIND = new MethodValues("PROPFIND", true);
    /** Enumerated object for the WebDAV method PROPPATCH */
    public static final MethodValues PROPPATCH = new MethodValues("PROPPATCH", true);
    /** Enumerated object for the WebDAV method MKCOL */
    public static final MethodValues MKCOL = new MethodValues("MKCOL", true);
    /** Enumerated object for the WebDAV method COPY */
    public static final MethodValues COPY = new MethodValues("COPY", true);
    /** Enumerated object for the WebDAV method MOVE */
    public static final MethodValues MOVE = new MethodValues("MOVE", true);
    /** Enumerated object for the WebDAV method LOCK */
    public static final MethodValues LOCK = new MethodValues("LOCK", true);
    /** Enumerated object for the WebDAV method UNLOCK */
    public static final MethodValues UNLOCK = new MethodValues("UNLOCK", true);
    /** Enumerated object for the WebDAV method SEARCH */
    public static final MethodValues SEARCH = new MethodValues("SEARCH", true);
    /** Enumerated object for the WebDAV method BCOPY */
    public static final MethodValues BCOPY = new MethodValues("BCOPY", true);
    /** Enumerated object for the WebDAV method BMOVE */
    public static final MethodValues BMOVE = new MethodValues("BMOVE", true);
    /** Enumerated object for the WebDAV method BDELETE */
    public static final MethodValues BDELETE = new MethodValues("BDELETE", true);
    /** Enumerated object for the WebDAV method BPROPPATCH */
    public static final MethodValues BPROPPATCH = new MethodValues("BPROPPATCH", true);
    /** Enumerated object for the WebDAV method BPROPFIND */
    public static final MethodValues BPROPFIND = new MethodValues("BPROPFIND", true);
    /** Enumerated object for the WebDAV method POLL */
    public static final MethodValues POLL = new MethodValues("POLL", true);
    /** Enumerated object for the WebDAV method NOTIFY */
    public static final MethodValues NOTIFY = new MethodValues("NOTIFY", true);
    /** Enumerated object for the WebDAV method SUBSCRIBE */
    public static final MethodValues SUBSCRIBE = new MethodValues("SUBSCRIBE", true);
    /** Enumerated object for the WebDAV method UNSUBSCRIBE */
    public static final MethodValues UNSUBSCRIBE = new MethodValues("UNSUBSCRIBE", true);
    /** Enumerated object for the WebDAV method ACL */
    public static final MethodValues ACL = new MethodValues("ACL", true);
    /** Enumerated object for the WebDAV method SUBSCRIPTIONS */
    public static final MethodValues SUBSCRIPTIONS = new MethodValues("SUBSCRIPTIONS", true);

    /** Does this method allow a body with the request? */
    private boolean bBodyAllowed = true;
    /** Flag on whether or not this instance is an undefined one */
    private boolean undefined = false;

    /**
     * Constructor that takes as input the name of the value plus a specific
     * ordinal value.
     * 
     * @param name
     * @param isBodyAllowed
     */
    public MethodValues(String name, boolean isBodyAllowed) {
        super(name, NEXT_ORDINAL.getAndIncrement());
        setBodyAllowed(isBodyAllowed);
        allKeys.add(this);
        myMatcher.add(this);
    }

    /**
     * Query whether or not a body is allowed to be sent with this method.
     * 
     * @return boolean
     */
    public boolean isBodyAllowed() {
        return this.bBodyAllowed;
    }

    /**
     * Set the flag on whether or not this method allows a body.
     * 
     * @param flag
     */
    public void setBodyAllowed(boolean flag) {
        this.bBodyAllowed = flag;
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
     * @return MethodValues
     * @throws IndexOutOfBoundsException
     */
    public static MethodValues getByOrdinal(int i) {
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
     * @return MethodValues
     */
    public static MethodValues match(String name, int offset, int length) {
        if (null == name)
            return null;
        return (MethodValues) myMatcher.match(name, offset, length);
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
     * @return MethodValues
     */
    public static MethodValues match(byte[] name, int offset, int length) {
        if (null == name)
            return null;
        return (MethodValues) myMatcher.match(name, offset, length);
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
     * @return MethodValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static MethodValues find(byte[] name, int offset, int length) {
        MethodValues key = (MethodValues) myMatcher.match(name, offset, length);
        if (null == key) {
            synchronized (MethodValues.class) {
                // protect against 2 threads getting here on the new value by
                // testing again inside a sync block
                key = (MethodValues) myMatcher.match(name, offset, length);
                if (null == key) {
                    key = new MethodValues(new String(name, offset, length), true);
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
     * @return MethodValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static MethodValues find(String name) {
        MethodValues key = (MethodValues) myMatcher.match(name, 0, name.length());
        if (null == key) {
            synchronized (MethodValues.class) {
                // protect against 2 threads getting here on the new value by
                // testing again inside a sync block
                key = (MethodValues) myMatcher.match(name, 0, name.length());
                if (null == key) {
                    key = new MethodValues(name, true);
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
     * @return MethodValues
     * @throws NullPointerException
     *             if input name is null
     */
    public static MethodValues find(byte[] name) {
        return find(name, 0, name.length);
    }

}
