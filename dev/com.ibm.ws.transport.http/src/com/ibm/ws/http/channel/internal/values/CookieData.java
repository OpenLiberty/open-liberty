/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.values;

import java.util.ArrayList;
import java.util.List;

import com.ibm.wsspi.genericbnf.GenericKeys;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.genericbnf.KeyMatcher;
import com.ibm.wsspi.http.HttpCookie;

/**
 * This class is extended by the individual Cookie Attribute classes.
 *
 */
public abstract class CookieData extends GenericKeys {

    /** Counter of the number of values defined so far */
    private static int NEXT_ORDINAL = 0;
    /** List keeping track of all the values, used by the corresponding matcher */
    private static final List<CookieData> allKeys = new ArrayList<CookieData>();
    /** Matcher used for these enum objects */
    private static final KeyMatcher myMatcher = new KeyMatcher(false);

    /** Cookie version data object */
    public static final CookieData cookieVersion = new CookieVersionData();
    /** Cookie domain data object */
    public static final CookieData cookieDomain = new CookieDomainData();
    /** Cookie max-age data object */
    public static final CookieData cookieMaxAge = new CookieMaxAgeData();
    /** Cookie path data object */
    public static final CookieData cookiePath = new CookiePathData();
    /** Cookie security data object */
    public static final CookieData cookieSecure = new CookieSecureData();
    /** Cookie expires data object */
    public static final CookieData cookieExpires = new CookieExpiresData();
    /** Cookie comment data object */
    public static final CookieData cookieComment = new CookieCommentData();
    /** Discard attribute of a Set-Cookie2 */
    public static final CookieData cookieDiscard = new CookieDiscard();
    /** Port attribute of a cookie */
    public static final CookieData cookiePort = new CookiePort();
    /** CommentURL attribute of a Set-Cookie2 */
    public static final CookieData cookieCommentURL = new CookieCommentURL();
    /** MS created HttpOnly attribute */
    public static final CookieData cookieHttpOnly = new CookieHttpOnly();
    /** SameSite attribute */
    public static final CookieData cookieSameSite = new CookieSameSiteData();

    /**
     * Constructor for a generic cookie data object.
     *
     * @param name
     */
    public CookieData(String name) {
        super(name, nextOrdinal());
        allKeys.add(this);
        myMatcher.add(this);
    }

    /**
     * Get the next ordinal value.
     *
     * @return int
     */
    private static synchronized int nextOrdinal() {
        return NEXT_ORDINAL++;
    }

    /**
     * Allow access to the list containing all of the enumerated values.
     *
     * @return List<CookieData>
     */
    public static List<CookieData> getAllKeys() {
        return allKeys;
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
     * @return CookieData
     */
    public static CookieData match(String name, int offset, int length) {
        if (null == name)
            return null;
        return (CookieData) myMatcher.match(name, offset, length);
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
     * @return CookieData
     */
    public static CookieData match(byte[] name, int offset, int length) {
        if (null == name)
            return null;
        return (CookieData) myMatcher.match(name, offset, length);
    }

    /**
     * This abstract method is extended by all the specialized cookieData
     * classes that implement their own set method. For instance the
     * CookieVersionData class will implement the set method to set the
     * version on the cookie.
     *
     * @param cookie
     *            The cookie object that will be used to set the value on
     * @param data
     *            The value of the attribute to be set
     * @return Indicates if the status worked or not
     */
    public abstract boolean set(HttpCookie cookie, byte[] data);

    /**
     * Query whether this particular cookie token is a valid attribute of the
     * input header.
     *
     * @param hdr
     * @param includesDollar
     *            (whether the value starts with a dollar symbol)
     * @return boolean
     */
    public abstract boolean validForHeader(HeaderKeys hdr, boolean includesDollar);
}
