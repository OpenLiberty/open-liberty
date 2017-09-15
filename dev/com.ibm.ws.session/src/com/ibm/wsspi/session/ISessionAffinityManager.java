/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.session;

import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * This interface allows the cookie generator to be customized. Depending on
 * whether
 * the session management depends on the presence or absence of affinity, a
 * different
 * cookie generator can be plugged in.
 * 
 */
public interface ISessionAffinityManager {

    // XD vars
    // Constant for the JSessionID
    public final static String JSESSIONID = "JSESSIONID";
    public final static String SIP_HTTP_COOKIE_NAME = "SIP-HTTP-COOKIE";
    public final static String URL_REWRITE_PREFIX = ";jsessionid=";

    public String encodeURL(ServletRequest request, String url, SessionAffinityContext affinityContext, IProtocolAdapter adapter);

    public void setCookie(ServletRequest request, ServletResponse response, SessionAffinityContext affinityContext, IProtocolAdapter adapter, Object session);

    /**
     * Gets whatever default token this app server uses for http session affinity
     * to
     * this server and this server only
     * 
     * @return
     */
    public String getLocalCloneID();

    /**
     * Sets whether URL encoding is configured
     */
    public void setUseURLEncoding(boolean useURLEncoding);

    /**
     * Sets the name to use for the session cookie
     */
    public void setCookieName(String cookieName);

    // end of XD vars
    /**
     * Analyses the provided request object and extracts all the relevant
     * information
     * about affinity details that are available on the request, and visible by
     * using the
     * standard methods available on the ServletRequest object. A protocol
     * specific affinity manager may also be able to use protocol specific
     * extensions,
     * so in HTTP, methods on the HTTPServletRequest class may also be used.
     * <p>
     * 
     * @param ServletRequest
     *            request
     * @return SessionAffinityContext object that stores the information gleaned
     *         from the request.
     */
    public SessionAffinityContext analyzeRequest(ServletRequest request);

    /**
     * Encodes the URL with affinity information (URL rewriting)
     * <p>
     * 
     * @param ServletRequest
     *            request
     * @param String
     *            url
     * @param SessionAffinityContext
     *            affinityContext
     * @return String encoded URL
     */
    public String encodeURL(ServletRequest request, String url, SessionAffinityContext affinityContext);

    /**
     * Sets a cookie on the response object with the affinity details in the
     * affinityContext
     * object.
     * <p>
     * 
     * @param ServletRequest
     *            Incoming request object.
     * @param ServletResponse
     *            response on which the cookie is set
     * @param SessionAffinityContext
     *            affinityContext object that stores the affinity data of the
     *            request and response.
     */
    public void setCookie(ServletRequest request, ServletResponse response, SessionAffinityContext affinityContext, Object session);

    /**
     * If multiple IDs are recieved (for example, multiple JSESSIONID cookies) on
     * a single request, set the
     * next one in the SessionAffinityContext. This allows the session manager to
     * check all input ids.
     * 
     * @param sac
     * @return boolean that indicates whether another ID was present
     */
    public boolean setNextId(SessionAffinityContext sac);

    /**
     * Returns the session id to use for this request. Maybe the SSL session id,
     * the
     * requested session id, or the response session id. The response session id
     * is used
     * if we've been dispatched and another servlet has already generated the
     * response id.
     * 
     * @param req
     * @param sac
     * @return String
     */
    public String getInUseSessionID(ServletRequest req, SessionAffinityContext sac);

    /**
     * Returns the session version to use for this request. The response session
     * version is used
     * if we've been dispatched and another servlet has already set the response
     * version.
     * 
     * @param req
     * @param sac
     * @return String
     */
    public int getInUseSessionVersion(ServletRequest req, SessionAffinityContext sac);

    /**
     * Returns a list of all the cookie values with the session cookie name.
     * 
     * @param request
     * @return List
     */
    public List getAllCookieValues(ServletRequest request);
}
