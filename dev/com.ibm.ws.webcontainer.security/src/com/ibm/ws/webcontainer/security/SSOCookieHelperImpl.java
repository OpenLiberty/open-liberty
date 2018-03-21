/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenHelper;
import com.ibm.ws.security.util.ByteArray;
import com.ibm.ws.webcontainer.security.internal.SSOAuthenticator;
import com.ibm.ws.webcontainer.security.internal.StringUtil;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * Single sign-on cookie helper class.
 */
public class SSOCookieHelperImpl implements SSOCookieHelper {
    private static final TraceComponent tc = Tr.register(SSOCookieHelperImpl.class);

    protected static final ConcurrentMap<ByteArray, String> cookieByteStringCache = new ConcurrentHashMap<ByteArray, String>(20);
    private static int MAX_COOKIE_STRING_ENTRIES = 100;
    private String cookieName = null;
    protected boolean isJwtCookie = false;

    private final WebAppSecurityConfig config;

    public SSOCookieHelperImpl(WebAppSecurityConfig config) {
        this(config, (String) null);
    }

    /**
     * Only have a custom cookie name for JASPI session as of now.
     */
    public SSOCookieHelperImpl(WebAppSecurityConfig config, String ssoCookieName) {
        this.config = config;
        cookieName = ssoCookieName;
    }

    /** {@inheritDoc} */
    /**
     * Set-Cookie: <name>=<value>[; <name>=<value>]...
     * [; expires=<date>][; domain=<domain_name>]
     * [; path=<some_path>][; secure][; httponly]
     **/
    @Override
    public void addSSOCookiesToResponse(Subject subject, HttpServletRequest req, HttpServletResponse resp) {
new Exception("Toshi").printStackTrace();
        if (!allowToAddCookieToResponse(req))
            return;
        addJwtSsoCookiesToResponse(subject, req, resp);

        if (!JwtSSOTokenHelper.shouldAlsoIncludeLtpaCookie()) {
            return;
        }

        SingleSignonToken ssoToken = getDefaultSSOTokenFromSubject(subject);
        if (ssoToken == null) {
            return;
        }

        byte[] ssoTokenBytes = ssoToken.getBytes();
        if (ssoTokenBytes == null) {
            return;
        }

        ByteArray cookieBytes = new ByteArray(ssoTokenBytes);
        String cookieByteString = cookieByteStringCache.get(cookieBytes);
        if (cookieByteString == null) {
            cookieByteString = StringUtil.toString(Base64Coder.base64Encode(ssoTokenBytes));
            updateCookieCache(cookieBytes, cookieByteString);
        }

        Cookie ssoCookie = createCookie(req, cookieByteString);
        resp.addCookie(ssoCookie);

    }

    /**
     * @param subject
     * @param req
     * @param resp
     */
    @Override
    public void addJwtSsoCookiesToResponse(Subject subject, HttpServletRequest req, HttpServletResponse resp) {
        String cookieByteString = JwtSSOTokenHelper.getJwtSSOToken(subject);
        if (cookieByteString != null) {
            Cookie ssoCookie = createJwtCookie(req, cookieByteString);
            resp.addCookie(ssoCookie);
            isJwtCookie = true;
        }

    }

    public Cookie createJwtCookie(HttpServletRequest req, String cookieValue) {
        Cookie ssoCookie = new Cookie("jwtToken", cookieValue);
        ssoCookie.setMaxAge(-1);
        ssoCookie.setPath("/");
        ssoCookie.setSecure(config.getSSORequiresSSL());
        ssoCookie.setHttpOnly(config.getHttpOnlyCookies());

        String domainName = getSSODomainName(req, config.getSSODomainList(), config.getSSOUseDomainFromURL());
        if (domainName != null) {
            ssoCookie.setDomain(domainName);
        }
        return ssoCookie;
    }

    /**
     * Creates the SSO cookie with max age of <code>-1</code>, path set to <code>/</code>,
     * and an optional domain name. The cookie's <code>secure</code> flag depends on the
     * <code>ssoRequiresSSL</code> configuration.
     *
     * @param req the HTTP servlet request.
     * @param cookieValue the value used to create the cookie from.
     * @return ssoCookie the SSO cookie.
     */
    public Cookie createCookie(HttpServletRequest req, String cookieValue) {
        Cookie ssoCookie = new Cookie(getSSOCookiename(), cookieValue);
        ssoCookie.setMaxAge(-1);
        //The path has to be "/" so we will not have multiple cookies in the same domain
        ssoCookie.setPath("/");
        ssoCookie.setSecure(config.getSSORequiresSSL());
        ssoCookie.setHttpOnly(config.getHttpOnlyCookies());

        String domainName = getSSODomainName(req, config.getSSODomainList(), config.getSSOUseDomainFromURL());
        if (domainName != null) {
            ssoCookie.setDomain(domainName);
        }
        return ssoCookie;
    }

    /**
     * This method checks the following conditions:
     * 1) If SSO is NOT enabled, returns false.
     * 2) If SSO requires SSL is true and NOT HTTPs request, returns false.
     * 3) Otherwise returns true.
     *
     * @param req
     * @return
     */
    @Override
    public boolean allowToAddCookieToResponse(HttpServletRequest req) {
        if (!config.isSingleSignonEnabled()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "SSO is not enabled. Not setting the SSO Cookie");
            }
            return false;
        }
        boolean secureRequest = req.isSecure();
        if (config.getSSORequiresSSL() && !secureRequest) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "SSO requires SSL. The cookie will not be sent back because the request is not over https.");
            }
            return false;
        }
        return true;
    }

    /**
     * Remove a cookie from the response
     *
     * @param subject
     * @param resp
     */
    @Override
    public void removeSSOCookieFromResponse(HttpServletResponse resp) {
        if (resp instanceof com.ibm.wsspi.webcontainer.servlet.IExtendedResponse) {
            ((com.ibm.wsspi.webcontainer.servlet.IExtendedResponse) resp).removeCookie(getSSOCookiename());
        }
    }

    /**
     * Perform some cookie cache maintenance. If the cookie cache has grown too
     * large, clear it. Otherwise, store the cookieByteString into the cache
     * based on the cookieBytes.
     *
     * @param cookieBytes
     * @param cookieByteString
     */
    protected synchronized void updateCookieCache(ByteArray cookieBytes, String cookieByteString) {
        if (cookieByteStringCache.size() > MAX_COOKIE_STRING_ENTRIES)
            cookieByteStringCache.clear();
        if (cookieByteString != null)
            cookieByteStringCache.put(cookieBytes, cookieByteString);
    }

    /** {@inheritDoc} */
    /*
     * 1) If we have the custom cookie name, then delete just the custom cookie name
     * 2) If we have the custom cookie name but no cookie found, then will delete the default cookie name LTPAToken2
     */
    @Override
    public void createLogoutCookies(HttpServletRequest req, HttpServletResponse res) {
        Cookie[] cookies = req.getCookies();
        java.util.ArrayList<Cookie> logoutCookieList = new java.util.ArrayList<Cookie>();
        if (cookies != null) {
            String ssoCookieName = resolveCookieName(cookies);
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equalsIgnoreCase(ssoCookieName)) {
                    cookies[i].setValue(null);
                    addLogoutCookieToList(req, ssoCookieName, logoutCookieList);
                }
            }
            for (Cookie cookie : logoutCookieList) {
                res.addCookie(cookie);
            }
        }
    }

    /**
     * 1) If we found the cookie associate with the cookie name, we will use the cookie name
     * 2) If we can not find the cookie associate with the cookie name, we will use the default cookie name LTPAToken2 if isUseOnlyCustomCookieName is false
     */
    protected String resolveCookieName(Cookie[] cookies) {
        boolean foundCookie = false;
        String ssoCookieName = this.getSSOCookiename();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equalsIgnoreCase(ssoCookieName)) {
                    foundCookie = true;
                    break;
                }
            }
        }
        if (!foundCookie && !config.isUseOnlyCustomCookieName())
            return SSOAuthenticator.DEFAULT_SSO_COOKIE_NAME;
        else
            return ssoCookieName;
    }

    protected void addLogoutCookieToList(HttpServletRequest req,
                                         String cookieName,
                                         java.util.ArrayList<Cookie> cookieList) {
        Cookie c = new Cookie(cookieName, "");
        c.setMaxAge(0);
        c.setPath("/");
        c.setSecure(req.isSecure());
        if (config.getHttpOnlyCookies()) {
            c.setHttpOnly(true);
        }
        String domainName = getSSODomainName(req, config.getSSODomainList(), config.getSSOUseDomainFromURL());
        if (domainName != null) {
            c.setDomain(domainName);
        }

        cookieList.add(c);
    }

    /** {@inheritDoc} */
    @Override
    public SingleSignonToken getDefaultSSOTokenFromSubject(final javax.security.auth.Subject subject) {
        if (subject == null)
            return null;
        SingleSignonToken ssoToken = null;
        java.util.Set privateCredentials = null;
        try {
            privateCredentials = (java.util.Set) AccessController.doPrivileged(new java.security.PrivilegedAction() {
                @Override
                public Object run() {
                    return subject.getPrivateCredentials(SingleSignonToken.class);
                }
            });

            java.util.Iterator<?> ssoIterator = privateCredentials.iterator();
            while (ssoIterator.hasNext()) {
                ssoToken = (SingleSignonToken) ssoIterator.next();
                if (ssoToken.getName().equals(this.getSSOCookiename())) {
                    break;
                }
            }
        } catch (Exception e) {
            // Oh well
        }
        return ssoToken;
    }

    /** {@inheritDoc} */
    @Override
    public String getSSOCookiename() {
        if (cookieName != null)
            return cookieName;
        else
            return config.getSSOCookieName();
    }

    /**
     * Returns the SSO domain based. Expected return values are derived from the domain name
     * of the host name in the request which may be matched with the WebAppConfig domain name
     * list. If there is no match with the ssoDomainList and useURLDomain=true, returns the
     * hostname domain. Otherwise it returns null for the following conditions:
     * 1) ssoDomainList is null
     * 2) The hostname is not fully qualify or localhost
     * 3) useURLDomain is false
     * 3) Request URL is an IP address
     *
     * @param req
     * @param ssoDomainList
     * @param useDomainFromURL
     * @return
     */
    @Override
    public String getSSODomainName(HttpServletRequest req, List<String> ssoDomainList, boolean useDomainFromURL) {
        try {
            final String host = getHostNameFromRequestURL(req);
            String ipAddr = AccessController.doPrivileged(new PrivilegedAction<String>() {

                @Override
                public String run() {
                    return getHostIPAddr(host);
                }
            });
            if (host.equals(ipAddr) || host.indexOf(".") == -1) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "URL host is an IP or locahost, no SSO domain will be set.");
                return null;
            }
            String domain = host.substring(host.indexOf("."));
            if (ssoDomainList != null && !ssoDomainList.isEmpty()) {
                for (Iterator<String> itr = ssoDomainList.iterator(); itr.hasNext();) {
                    String dm = itr.next();
                    if (domain.endsWith(dm))
                        return dm;
                }
            }
            if (useDomainFromURL) {
                return domain;
            }
        } catch (MalformedURLException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Unexpected exception getting request SSO domain", new Object[] { e });
        }
        return null;
    }

    /**
     * @param host
     * @return
     */
    @FFDCIgnore(UnknownHostException.class)
    private String getHostIPAddr(String host) {
        String iAddr = "";
        try {
            iAddr = InetAddress.getByName(host).getHostAddress().trim();
        } catch (UnknownHostException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception in getting IP address for URL host, assuming URL host is not an IP", new Object[] { e });
        }
        return iAddr;
    }

    /**
     * @param req
     * @return
     * @throws MalformedURLException
     */
    private String getHostNameFromRequestURL(HttpServletRequest req) throws MalformedURLException {
        String requestUrl = req.getRequestURL().toString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "URL: " + requestUrl);
        URL url = new URL(requestUrl);
        String host = url.getHost().trim();
        return host;
    }
}
