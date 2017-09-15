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

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.httpsvc.servlet.internal.RequestMessage;

/**
 * Class that encapsulates the session information provided by a client
 * request message.
 */
public class SessionInfo {
    /** Debug variable */
    private static final TraceComponent tc = Tr.register(SessionInfo.class);

    /** Client provided session id, might be null */
    private String id = null;
    /** If client id existed, was it found in a cookie? */
    private boolean fromCookie = false;
    /** If client id existed, was it found in the url? */
    private boolean fromURL = false;
    /** If client id existed, did it point to a session still valid? */
    private boolean stillValid = false;
    /**
     * Current session for this request, may or may not equal the client
     * provided ID.
     */
    private SessionImpl mySession = null;

    /** Session configuration object */
    private SessionConfig mySessionConfig = null;

    private final RequestMessage request;
    private final SessionManager mgr;

    /**
     * Constructor.
     * 
     * @param request
     */
    public SessionInfo(RequestMessage request, SessionManager mgr) {
        this.request = request;

        if (mgr == null)
            throw new IllegalStateException("No Session manager");

        this.mgr = mgr;
        this.mySessionConfig = mgr.getSessionConfig(this);

        if (getSessionConfig().isURLRewriting()) {
            parseIDFromURL(request);
        }
        if (null == this.id) {
            parseIDFromCookies(request);
        }

        if (null != this.id) {
            // check whether the initial data points to a valid session
            this.mySession = mgr.getSession(this, false);
            this.stillValid = (null != this.mySession);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, this.toString());
        }
    }

    /**
     * Look for the possible session ID in the URL of the input request
     * message.
     * 
     * @param request
     */
    private void parseIDFromURL(RequestMessage request) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Looking for ID in URL");
        }
        String url = request.getRawRequestURI();
        String target = getSessionConfig().getURLRewritingMarker();
        URLParser parser = new URLParser(url, target);
        if (-1 != parser.idMarker) {
            // session id marker was found, try to pull out the value
            int start = parser.idMarker + target.length();
            if (-1 != parser.fragmentMarker) {
                this.id = url.substring(start, parser.fragmentMarker);
            } else if (-1 != parser.queryMarker) {
                this.id = url.substring(start, parser.queryMarker);
            } else {
                this.id = url.substring(start);
            }
            this.fromURL = true;
        }
    }

    /**
     * Encode session information into the provided URL. This will replace
     * any existing session in that URL.
     * 
     * @param url
     * @param info
     * @return String
     */
    public static String encodeURL(String url, SessionInfo info) {
        // could be /path/page#fragment?query
        // could be /page/page;session=existing#fragment?query
        // where fragment and query are both optional

        HttpSession session = info.getSession();
        if (null == session) {
            return url;
        }
        final String id = session.getId();
        final String target = info.getSessionConfig().getURLRewritingMarker();
        URLParser parser = new URLParser(url, target);

        StringBuilder sb = new StringBuilder();
        if (-1 != parser.idMarker) {
            // a session exists in the URL, overlay this ID
            sb.append(url);
            int start = parser.idMarker + target.length();
            if (start + 23 < url.length()) {
                sb.replace(start, start + 23, id);
            } else {
                // invalid length on existing session, just remove that
                // TODO: what if a fragment or query string was after the
                // invalid session data
                sb.setLength(parser.idMarker);
                sb.append(target).append(id);
            }
        } else {
            // add session data to the URL
            if (-1 != parser.fragmentMarker) {
                // prepend it before the uri fragment
                sb.append(url, 0, parser.fragmentMarker);
                sb.append(target).append(id);
                sb.append(url, parser.fragmentMarker, url.length());
            } else if (-1 != parser.queryMarker) {
                // prepend it before the query data
                sb.append(url, 0, parser.queryMarker);
                sb.append(target).append(id);
                sb.append(url, parser.queryMarker, url.length());
            } else {
                // just a uri
                sb.append(url).append(target).append(id);
            }
        }
        return sb.toString();
    }

    /**
     * Strip out any session id information from the input URL.
     * 
     * @param url
     * @param info
     * @return String
     */
    public static String stripURL(String url, SessionInfo info) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Removing any session id from [" + url + "]");
        }
        String target = info.getSessionConfig().getURLRewritingMarker();
        URLParser parser = new URLParser(url, target);
        if (-1 != parser.idMarker) {
            // the parser found an id marker, see if we need to include
            // any trailing fragment or query data
            StringBuilder sb = new StringBuilder(url.substring(0, parser.idMarker));
            if (-1 != parser.fragmentMarker) {
                sb.append(url.substring(parser.fragmentMarker));
            } else if (-1 != parser.queryMarker) {
                sb.append(url.substring(parser.queryMarker));
            }
            return sb.toString();
        }
        return url;
    }

    /**
     * Strip out any session id information from the input URL. This method
     * is provided for the request servlet spec that uses a StringBuffer for
     * the full URL value.
     * 
     * @param url
     * @param info
     * @return String
     */
    public static StringBuffer stripURL(StringBuffer url, SessionInfo info) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Removing any session id from [" + url + "]");
        }
        String target = info.getSessionConfig().getURLRewritingMarker();
        URLParser parser = new URLParser(url, target);
        if (-1 != parser.idMarker) {
            // the parser found an id marker, see if we need to include
            // any trailing fragment or query data
            StringBuffer sb = new StringBuffer(url.substring(0, parser.idMarker));
            if (-1 != parser.fragmentMarker) {
                sb.append(url.substring(parser.fragmentMarker));
            } else if (-1 != parser.queryMarker) {
                sb.append(url.substring(parser.queryMarker));
            }
            return sb;
        }
        return url;
    }

    /**
     * Look for a possible session id in the cookies of the request message.
     * 
     * @param request
     */
    private void parseIDFromCookies(RequestMessage request) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Looking for ID in cookies");
        }
        Enumeration<String> list = request.getHeaders("Cookie");
        while (list.hasMoreElements()) {
            String item = list.nextElement();
            int index = item.indexOf(getSessionConfig().getIDName());
            if (-1 != index) {
                index = item.indexOf('=', index);
                if (-1 != index) {
                    index++;
                    // TODO this is assuming that the full value is valid and
                    // grabbing just the 4 digit id...
                    if (item.length() >= (index + 4)) {
                        this.id = item.substring(index, index + 4);
                        this.fromCookie = true;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Create a proper Cookie for the given session object.
     * 
     * @param info
     * @return Cookie
     */
    public static Cookie encodeCookie(SessionInfo info) {
        // create a cookie using the configuration information
        HttpSession session = info.getSession();
        SessionConfig config = info.getSessionConfig();
        Cookie cookie = new Cookie(
                        config.getIDName(),
                        config.getSessionVersion() + session.getId());
        cookie.setSecure(config.isCookieSecure());
        cookie.setMaxAge(config.getCookieMaxAge());
        cookie.setPath(config.getCookiePath());
        cookie.setDomain(config.getCookieDomain());

        return cookie;
    }

    /**
     * Query the configuration for this session. This could be scoped at the
     * server level, the app level, or an individual module level.
     * 
     * @return SessionConfig
     */
    public SessionConfig getSessionConfig() {
        return this.mySessionConfig;
    }

    /**
     * Query the servlet context object. This may be null.
     * 
     * @return ServletContext
     */
    public ServletContext getContext() {
        return request.getServletContext();
    }

    /**
     * Query the session ID found in the original request message. This may
     * be null if the client did not send one, and may or may not equal the
     * current session for this connection.
     * 
     * @return String
     */
    public String getID() {
        return this.id;
    }

    /**
     * Query whether that optional session ID found in the original request
     * came from a cookie or not.
     * 
     * @return boolean, false if not found or found outside of cookies
     */
    public boolean isFromCookie() {
        return this.fromCookie;
    }

    /**
     * Query whether that optional session ID found in the original request
     * came from the URL or not.
     * 
     * @return boolean, false if not found or found outside the URL
     */
    public boolean isFromURL() {
        return this.fromURL;
    }

    /**
     * Query whether the optional session ID found in the original request
     * pointed to a valid session initially.
     * 
     * @return boolean, false if not found or was invalid
     */
    public boolean isValid() {
        return this.stillValid;
    }

    /**
     * Access any existing session for this connection. It will not create
     * one if no session is found.
     * 
     * @return HttpSession
     */
    public HttpSession getSession() {
        return getSession(false);
    }

    /**
     * Access the current session for this connection. This may return null
     * if one was not found and the create flag was false. If a cached session
     * is found but it reports as invalid, then this will look for a new
     * session if the create flag is true.
     * 
     * @param create
     * @return HttpSession
     */
    public HttpSession getSession(boolean create) {
        if (null != this.mySession) {
            if (this.mySession.isInvalid()) {
                this.mySession = null;
            } else {
                return this.mySession;
            }
        }
        this.mySession = mgr.getSession(this, create);
        return this.mySession;
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (null == this.id) {
            return "SessionInfo: id=null";
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("SessionInfo: id=").append(this.id);
        sb.append(" fromURL=").append(this.fromURL);
        sb.append(" fromCookie=").append(this.fromCookie);
        sb.append(" stillValid=").append(this.stillValid);
        sb.append("\n\tcurrent session=").append(this.mySession);
        return sb.toString();
    }

}
