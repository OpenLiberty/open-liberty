/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.httpsvc.session.internal;

import java.util.Dictionary;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Configuration wrapper for session information.
 */
public class SessionConfig {

    /** Debug variable */
    private static final TraceComponent tc = Tr.register(SessionConfig.class);

    /** Property name for the session id name */
    private static final String PROP_IDNAME = "name";
    /** Property name on whether to use URL rewriting */
    private static final String PROP_USE_URLS = "url.rewriting.enabled";
    /** Property name on whether to use session cookies or not */
    private static final String PROP_USE_COOKIES = "cookies.enabled";
    /** Property name for whether the session cookies are secure or not */
    private static final String PROP_COOKIE_SECURE = "cookie.secure";
    /**
     * Property name for a max cookie age in seconds. Negative values
     * means it will be discarded when the client quits. A zero means the
     * client should immediately discard the cookie, while a positive value
     * is that many seconds the cookie is considered valid.
     */
    private static final String PROP_COOKIE_MAXAGE = "cookie.maxage";
    /** Property name for the session cookie domain value */
    private static final String PROP_COOKIE_DOMAIN = "cookie.domain";
    /** Property name for the session cookie path value */
    private static final String PROP_COOKIE_PATH = "cookie.path";

    private static final String DEFAULT_IDNAME = "jsessionid";
    private static final String DEFAULT_COOKIEPATH = "/";
    private static final String DEFAULT_COOKIEDOMAIN = "";
    private static final String VERSION_0 = "0000";

    private String idName = DEFAULT_IDNAME;
    private String urlRewritingMarker = null;
    private boolean enableCookies = true;
    private boolean cookieSecure = false;
    private int cookieMaxAge = -1;
    private String cookiePath = DEFAULT_COOKIEPATH;
    private String cookieDomain = DEFAULT_COOKIEDOMAIN;

    /**
     * Constructor.
     */
    public SessionConfig() {
        // nothing to do until config is provided
    }

    /**
     * Session configuration has been updated with the provided properties.
     * 
     * @param props
     */
    public void updated(Dictionary<?, ?> props) {
        String value = (String) props.get(PROP_IDNAME);
        if (null != value) {
            this.idName = value.trim();
        }
        value = (String) props.get(PROP_USE_URLS);
        if (null != value && Boolean.parseBoolean(value.trim())) {
            this.urlRewritingMarker = ";" + getIDName() + "=" + getSessionVersion();
        }

        value = (String) props.get(PROP_USE_COOKIES);
        if (null != value) {
            this.enableCookies = Boolean.parseBoolean(value.trim());
        }
        if (this.enableCookies) {
            // we're using cookies for session information
            value = (String) props.get(PROP_COOKIE_SECURE);
            if (null != value) {
                this.cookieSecure = Boolean.parseBoolean(value.trim());
            }
            value = (String) props.get(PROP_COOKIE_PATH);
            if (null != value) {
                this.cookiePath = value.trim();
            }
            value = (String) props.get(PROP_COOKIE_DOMAIN);
            if (null != value) {
                this.cookieDomain = value.trim();
            }
            value = (String) props.get(PROP_COOKIE_MAXAGE);
            if (null != value) {
                try {
                    this.cookieMaxAge = Integer.parseInt(value.trim());
                } catch (NumberFormatException nfe) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Ignoring incorrect max-age [" + value + "]", nfe.getMessage());
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Config: id name [" + this.idName + "]");
            if (isURLRewriting()) {
                Tr.event(tc, "Config: use URL rewriting [" + this.urlRewritingMarker + "]");
            }
            if (this.enableCookies) {
                Tr.event(tc, "Config: cookie max-age [" + this.cookieMaxAge + "]");
                Tr.event(tc, "Config: cookie secure [" + this.cookieSecure + "]");
                Tr.event(tc, "Config: cookie domain [" + this.cookieDomain + "]");
                Tr.event(tc, "Config: cookie path [" + this.cookiePath + "]");
            } else {
                Tr.event(tc, "Config: cookies disabled");
            }
        }
    }

    /**
     * Query the version of this running session manager.
     * 
     * @return String
     */
    public String getSessionVersion() {
        return VERSION_0;
    }

    /**
     * Query whether session cookies are enabled or not.
     * 
     * @return boolean
     */
    public boolean usingCookies() {
        return this.enableCookies;
    }

    /**
     * Query whether session has been configured to use URL rewriting
     * instead of cookies.
     * 
     * @return boolean
     */
    public boolean isURLRewriting() {
        return null != this.urlRewritingMarker;
    }

    /**
     * Query the marker found in the URL when URL rewriting is enabled. The
     * URL would look similar to /path;jsessionid=ID?querydata, where the
     * ";jsessionid=" is the marker.
     * 
     * @return String, null if URL rewriting is not enabled
     */
    public String getURLRewritingMarker() {
        return this.urlRewritingMarker;
    }

    /**
     * Query the ID name for sessions (i.e. jsessionid).
     * 
     * @return String
     */
    public String getIDName() {
        return this.idName;
    }

    /**
     * Query whether the session cookies are configured to have the secure
     * flag or not.
     * 
     * @return boolean
     */
    public boolean isCookieSecure() {
        return this.cookieSecure;
    }

    /**
     * Query the configured max-age setting for session cookies.
     * 
     * @return int
     */
    public int getCookieMaxAge() {
        return this.cookieMaxAge;
    }

    /**
     * Query the configured path value for session cookies.
     * 
     * @return String
     */
    public String getCookiePath() {
        return this.cookiePath;
    }

    /**
     * Query the configured domain value for session cookies.
     * 
     * @return String
     */
    public String getCookieDomain() {
        return this.cookieDomain;
    }

}
