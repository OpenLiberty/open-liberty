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
package com.ibm.ws.http.channel.internal.cookies;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 * <code>CookieCacheData</code> represents a set of data to cache cookies with
 * and/or information about the headers they were derived from.
 */
public class CookieCacheData implements FFDCSelfIntrospectable {

    /** The list of parsed cookie objects */
    private List<HttpCookie> parsedList = null;
    /** The last instance of the Cookie header in storage we parsed */
    private int hdrIndex = 0;
    /** The type of header (Cookie, Set-Cookie) this object represents. */
    private HttpHeaderKeys headerType;
    /** A flag that indicates that the parsed cookie list has been modified */
    private boolean isDirty = false;

    /**
     * Private constructor used for duplication.
     * 
     */
    private CookieCacheData() {
        // nothing to do
    }

    /**
     * Create a collection of data representing cookies being cached for the
     * input header name.
     * 
     * @param header
     */
    public CookieCacheData(HttpHeaderKeys header) {
        this.headerType = header;
        this.parsedList = new LinkedList<HttpCookie>();
    }

    /**
     * Return the <code>HttpHeaderKeys</code> object this cache data is supposed
     * to represent.
     * 
     * @return the HttpHeaderKeys object this object represents.
     */
    public HttpHeaderKeys getHeaderType() {
        return this.headerType;
    }

    /**
     * Returns the last header instance parsed.
     * 
     * @return int
     */
    public int getHeaderIndex() {
        return this.hdrIndex;
    }

    /**
     * Increments the current header instance marker.
     */
    public void incrementHeaderIndex() {
        this.hdrIndex++;
    }

    /**
     * Checks to see if the cache of cookies is dirty.
     * 
     * @return boolean Flag that indicates whether the parsed list has been
     *         modified
     */
    public boolean isDirty() {
        return this.isDirty;
    }

    /**
     * Sets the list dirty flag.
     * 
     * @param flag
     */
    public void setIsDirty(boolean flag) {
        this.isDirty = flag;
    }

    /**
     * Search the cache for an existing Cookie that matches the input name.
     * 
     * @param name
     * @return HttpCookie -- null if this cookie is not in the cache
     */
    public HttpCookie getCookie(String name) {
        if (null == name || 0 == this.parsedList.size()) {
            return null;
        }
        for (HttpCookie cookie : this.parsedList) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    /**
     * Find all instances of cookies in the cache with the given name and add
     * clones of those objects to the input list.
     * 
     * @param name
     * @param list
     * @return int - number of cookies added to the list
     */
    public int getAllCookies(String name, List<HttpCookie> list) {
        int added = 0;
        if (0 < this.parsedList.size() && null != name) {
            for (HttpCookie cookie : this.parsedList) {
                if (cookie.getName().equals(name)) {
                    list.add(cookie.clone());
                    added++;
                }
            }
        }
        return added;
    }

    /**
     * Clones all of the cookies in the cache onto the input list.
     * 
     * @param list
     * @return int - number of cookies added to the list
     */
    public int getAllCookies(List<HttpCookie> list) {
        int added = 0;
        if (0 < this.parsedList.size()) {
            for (HttpCookie cookie : this.parsedList) {
                list.add(cookie.clone());
                added++;
            }
        }
        return added;
    }

    /**
     * Find all instances of the input cookie name in the cache and append their
     * values into the input list.
     * 
     * @param name
     * @param list
     * @return int - number of items added
     */
    public int getAllCookieValues(String name, List<String> list) {
        int added = 0;
        if (0 < this.parsedList.size() && null != name) {
            for (HttpCookie cookie : this.parsedList) {
                if (name.equals(cookie.getName())) {
                    list.add(cookie.getValue());
                }
            }
        }
        return added;
    }

    /**
     * Returns the list of Cookie objects parsed out from the storage.
     * 
     * @return List<Cookie> of parsed Cookie objects
     */
    public List<HttpCookie> getParsedList() {
        return this.parsedList;
    }

    /**
     * Add a parsed Cookie object to the internal storage list.
     * 
     * @param cookie
     */
    public void addParsedCookie(HttpCookie cookie) {
        this.parsedList.add(cookie);
    }

    /**
     * Add a list of Cookies to the internal parsed list.
     * 
     * @param list
     */
    public void addParsedCookies(List<HttpCookie> list) {
        this.parsedList.addAll(list);
    }

    /**
     * Add a new cookie to the cache.
     * 
     * @param cookie
     */
    public void addNewCookie(HttpCookie cookie) {
        this.isDirty = true;
        this.parsedList.add(cookie);
    }

    /**
     * Add a list of new cookies to the cache.
     * 
     * @param list
     */
    public void addNewCookies(List<HttpCookie> list) {
        this.isDirty = true;
        this.parsedList.addAll(list);
    }

    /**
     * Remove a cookie from the cache.
     * 
     * @param cookie
     * @return boolean (false means the cookie was not present)
     */
    public boolean removeCookie(HttpCookie cookie) {
        this.isDirty = true;
        return this.parsedList.remove(cookie);
    }

    /**
     * Create a 'cloned', independent, representation of this object.
     * 
     * @return a duplicate of this object where the members are independent of
     *         originator's members (i.e. separate references, duplicated
     *         primatives, etc.).
     */
    public CookieCacheData duplicate() {
        // Create second instance and copy members
        CookieCacheData cData = new CookieCacheData();
        cData.parsedList = copyList(this.parsedList);
        cData.isDirty = this.isDirty;
        cData.hdrIndex = this.hdrIndex;
        cData.headerType = this.headerType;
        return cData;
    }

    /**
     * Makes a <b>fully</b> duplicated <code>List</code> from another
     * <code>List</code>.
     * 
     * <p>
     * Changes in one List will not affect the other.
     * </p>
     * 
     * @param originalList
     *            the original <code>List</code> to duplicate.
     * @return a duplicate <code>List</code>.
     */
    private List<HttpCookie> copyList(List<HttpCookie> originalList) {
        List<HttpCookie> list = new LinkedList<HttpCookie>();
        for (Iterator<HttpCookie> it = originalList.iterator(); it.hasNext();) {
            list.add(it.next().clone());
        }
        return list;
    }

    /**
     * For debugging - display this CookieCacheData and its contents.
     * 
     * @return a <code>String</code> representation of this object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("headerType=").append(this.headerType.getName());
        sb.append("headerIndex=").append(this.hdrIndex);
        sb.append("isDirty =").append(this.isDirty);
        for (HttpCookie cookie : this.parsedList) {
            sb.append(" [").append(cookie.getName());
            sb.append("]=[").append(cookie.getValue());
            sb.append(" Path=").append(cookie.getPath());
            sb.append(" Version=").append(cookie.getVersion());
            sb.append(" Domain=").append(cookie.getDomain());
            sb.append(" Max-Age=").append(cookie.getMaxAge());
            sb.append(" Comment=").append(cookie.getComment());
            sb.append("\r\n");
        }
        return sb.toString();
    }

    /*
     * @see com.ibm.ws.ffdc.FFDCSelfIntrospectable#introspectSelf()
     */
    public String[] introspectSelf() {
        return new String[] { this.toString() };
    }

}
