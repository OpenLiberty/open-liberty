/*******************************************************************************
 * Copyright (c) 2009, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.netty.handler.codec.http.cookie.Cookie;

/**
 * HTTP cookie object, similar to the J2EE servlet cookie object.
 */
public class HttpCookie {
    private String myName = null;
    private String myValue = null;
    private int myVersion = 0;
    private String myComment = null;
    private String myDomain = null;
    private String myPath = null;
    private boolean mySecureFlag = false;
    private int myMaxAge = -1;
    private boolean myHttpOnlyFlag = false;
    private boolean myDiscardFlag = false;
    private Map<String, String> myAttrs = new HashMap<String, String>();

    /**
     * Constructor.
     *
     * @param name
     * @param value
     */
    public HttpCookie(String name, String value) {
        this.myName = name;
        this.myValue = value;
    }

    public HttpCookie from(Cookie nettyCookie){
        HttpCookie cookie = new HttpCookie(nettyCookie.name(), nettyCookie.value());
        
        if (nettyCookie.domain() != null) {
            cookie.setDomain(nettyCookie.domain());
        }
        if (nettyCookie.path() != null) {
            cookie.setPath(nettyCookie.path());
        }
        if (nettyCookie.maxAge() != Long.MIN_VALUE) {
            long maxAgeLong = nettyCookie.maxAge();
            if (maxAgeLong > Integer.MAX_VALUE) {
                cookie.setMaxAge(Integer.MAX_VALUE);
            } else if (maxAgeLong < Integer.MIN_VALUE) {
                cookie.setMaxAge(Integer.MIN_VALUE);
            } else {
                cookie.setMaxAge((int) maxAgeLong);
            }
        }
        cookie.setHttpOnly(nettyCookie.isHttpOnly());
        cookie.setSecure(nettyCookie.isSecure());

        return cookie;
    }

    /**
     * Query the name of this cookie.
     *
     * @return String
     */
    public String getName() {
        return this.myName;
    }

    /**
     * Query the value of this cookie. This might be null, an empty string, or a
     * full valid string.
     *
     * @return String
     */
    public String getValue() {
        return this.myValue;
    }

    /**
     * Query the comment attribute of this cookie.
     *
     * @return String
     */
    public String getComment() {
        return this.myComment;
    }

    /**
     * Query the path attribute of this cookie.
     *
     * @return String
     */
    public String getPath() {
        return this.myPath;
    }

    /**
     * Query the domain attribute of this cookie.
     *
     * @return String
     */
    public String getDomain() {
        return this.myDomain;
    }

    /**
     * Query the secure-flag attribute of this cookie.
     *
     * @return String
     */
    public boolean isSecure() {
        return this.mySecureFlag;
    }

    /**
     * Query the max-age attribute of this cookie.
     *
     * @return String
     */
    public int getMaxAge() {
        return this.myMaxAge;
    }

    /**
     * Set a generic attribute on this cookie.
     *
     * @param name
     * @param value
     */
    public void setAttribute(String name, String value) {
        this.myAttrs.put(name.toLowerCase(), value);
    }

    /**
     * Query a generic attribute of this cookie.
     *
     * @param name
     * @return String
     */
    public String getAttribute(String name) {
        return this.myAttrs.get(name.toLowerCase());
    }

    /**
     * Query the version attribute of this cookie.
     *
     * @return int
     */
    public int getVersion() {
        return this.myVersion;
    }

    /**
     * Set the version attribute of this cookie to the input value. Valid options
     * include 0 and 1 only.
     *
     * @param version
     */
    public void setVersion(int version) {
        if (0 != version && 1 != version) {
            throw new IllegalArgumentException("Incorrect version; " + version);
        }
        this.myVersion = version;
    }

    /**
     * Set the comment attribute of this cookie.
     *
     * @param comment
     */
    public void setComment(String comment) {
        this.myComment = comment;
    }

    /**
     * Set the domain attribute of this cookie.
     *
     * @param domain
     */
    public void setDomain(String domain) {
        this.myDomain = domain;
    }

    /**
     * Set the path attribute of this cookie.
     *
     * @param path
     */
    public void setPath(String path) {
        this.myPath = path;
    }

    /**
     * Set the secure-flag attribute of this cookie.
     *
     * @param flag
     */
    public void setSecure(boolean flag) {
        this.mySecureFlag = flag;
    }

    /**
     * Set the max-age attribute of this cookie.
     *
     * @param age
     */
    public void setMaxAge(int age) {
        this.myMaxAge = age;
    }

    /**
     * Query if the HttpOnly attribute is set.
     *
     * @return boolean
     */
    public boolean isHttpOnly() {
        return this.myHttpOnlyFlag;
    }

    /**
     * Set the HttpOnly special attribute flag.
     *
     * @param flag
     */
    public void setHttpOnly(boolean flag) {
        this.myHttpOnlyFlag = flag;
    }

    /**
     * Query if the Discard attribute is set.
     *
     * @return boolean
     */
    public boolean isDiscard() {
        return this.myDiscardFlag;
    }

    /**
     * Set the Discard attribute flag.
     *
     * @param flag
     */
    public void setDiscard(boolean flag) {
        this.myDiscardFlag = flag;
    }

    /*
     * @see java.lang.Object#clone()
     */
    @Override
    public HttpCookie clone() {
        HttpCookie rc = new HttpCookie(this.myName, this.myValue);
        rc.setVersion(this.myVersion);
        rc.setPath(this.myPath);
        rc.setComment(this.myComment);
        rc.setDomain(this.myDomain);
        rc.setMaxAge(this.myMaxAge);
        rc.setSecure(this.mySecureFlag);
        rc.setHttpOnly(this.myHttpOnlyFlag);
        rc.setDiscard(this.myDiscardFlag);
        for (Entry<String, String> entry : this.myAttrs.entrySet()) {
            rc.setAttribute(entry.getKey(), entry.getValue());
        }
        return rc;
    }

    //Servlet 6.0 - Support the new Cookie setAttribute
    public Map<String, String> getAttributes() {
        return myAttrs;
    }
    
}
