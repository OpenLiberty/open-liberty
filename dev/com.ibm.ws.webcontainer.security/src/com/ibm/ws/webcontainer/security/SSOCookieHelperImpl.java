/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Hashtable;
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
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenHelper;
import com.ibm.ws.security.util.ByteArray;
import com.ibm.ws.webcontainer.security.internal.LoggedOutJwtSsoCookieCache;
import com.ibm.ws.webcontainer.security.internal.SSOAuthenticator;
import com.ibm.ws.webcontainer.security.internal.StringUtil;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServer;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.SingleSignonToken;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;

/**
 * Single sign-on cookie helper class.
 */
public class SSOCookieHelperImpl implements SSOCookieHelper {
    private static final TraceComponent tc = Tr.register(SSOCookieHelperImpl.class);

    private static final String OIDC_BROWSER_STATE_COOKIE = "oidc_bsc";
    private final AtomicServiceReference<OidcServer> oidcServerRef = null;
    private static final String[] disableSsoLtpaCookie = new String[] { AuthenticationConstants.INTERNAL_DISABLE_SSO_LTPA_COOKIE };

    protected static final ConcurrentMap<ByteArray, String> cookieByteStringCache = new ConcurrentHashMap<ByteArray, String>(20);
    private static int MAX_COOKIE_STRING_ENTRIES = 100;
    private String cookieName = null;

    protected final WebAppSecurityConfig config;

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

    /**
     * @param subject
     * @param req
     * @param resp
     * @return true if cookies were added
     */
    @Override
    public boolean addJwtSsoCookiesToResponse(Subject subject, HttpServletRequest req, HttpServletResponse resp) {
        boolean result = false;
        if (JwtSSOTokenHelper.isDisableJwtCookie()) {
            return result;
        }
        String cookieByteString = JwtSSOTokenHelper.getJwtSSOToken(subject);
        if (cookieByteString != null) {
            String testString = getJwtSsoTokenFromCookies(req, getJwtCookieName());
            boolean cookieAlreadySent = testString != null && testString.equals(cookieByteString);
            if (!cookieAlreadySent) {
                result = addJwtCookies(cookieByteString, req, resp);
            }
        }
        return result;
    }

    /**
     * Add the cookie or cookies as needed, depending on size of token.
     * Return true if any cookies were added
     */
    protected boolean addJwtCookies(String cookieByteString, HttpServletRequest req, HttpServletResponse resp) {

        String baseName = getJwtCookieName();
        if (baseName == null) {
            return false;
        }
        if ((!req.isSecure()) && getJwtCookieSecure()) {
            Tr.warning(tc, "JWT_COOKIE_SECURITY_MISMATCH", new Object[] {}); // CWWKS9127W
        }
        String[] chunks = splitString(cookieByteString, 3900);
        String cookieName = baseName;
        for (int i = 0; i < chunks.length; i++) {
            if (i > 98) {
                String eMsg = "Too many jwt cookies created";
                com.ibm.ws.ffdc.FFDCFilter.processException(new Exception(eMsg), this.getClass().getName(), "132");
                break;
            }
            Cookie ssoCookie = createCookie(req, cookieName, chunks[i], getJwtCookieSecure()); //name
            resp.addCookie(ssoCookie);
            cookieName = baseName + (i + 2 < 10 ? "0" : "") + (i + 2); //name02... name99
        }
        return true;
    }

    protected String getJwtCookieName() {
        return JwtSSOTokenHelper.getJwtCookieName();
    }

    protected boolean getJwtCookieSecure() {
        return JwtSSOTokenHelper.isCookieSecured();
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
        return createCookie(req, getSSOCookiename(), cookieValue, config.getSSORequiresSSL());
    }

    public Cookie createCookie(HttpServletRequest req, String cookieName, String cookieValue, boolean isSecure) {
        Cookie ssoCookie = new Cookie(cookieName, cookieValue);
        ssoCookie.setMaxAge(-1);
        //The path has to be "/" so we will not have multiple cookies in the same domain
        ssoCookie.setPath("/");
        ssoCookie.setSecure(isSecure);
        ssoCookie.setHttpOnly(config.getHttpOnlyCookies());

        String domainName = getSSODomainName(req, config.getSSODomainList(), config.getSSOUseDomainFromURL());
        if (domainName != null) {
            ssoCookie.setDomain(domainName);
        }

        String sameSite = config.getSameSiteCookie();
        if (sameSite != null && !sameSite.equals("Disabled")) {
            WebContainerRequestState requestState = WebContainerRequestState.getInstance(true);
            requestState.setCookieAttributes(cookieName, "SameSite=" + sameSite);

            if (sameSite.equals("None")) {
                ssoCookie.setSecure(true);
            }
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
            removeJwtSSOCookies((com.ibm.wsspi.webcontainer.servlet.IExtendedResponse) resp);
        }
    }

    protected void removeJwtSSOCookies(com.ibm.wsspi.webcontainer.servlet.IExtendedResponse resp) {
        String cookieName = getJwtCookieName();
        if (cookieName == null)
            return;
        resp.removeCookie(cookieName);
        // unknown how many additional cookies we had, just remove all possible cookies.
        for (int i = 2; i <= 99; i++) {
            String nextCookieName = cookieName + (i < 10 ? "0" : "") + i; //name02... name99
            resp.removeCookie(nextCookieName);
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
     * 3) If jwtsso is active, clean up those cookies too.
     */
    @Override
    public void createLogoutCookies(HttpServletRequest req, HttpServletResponse res, boolean deleteJwtCookies) {
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
            if (deleteJwtCookies) {
                logoutJwtCookies(req, cookies, logoutCookieList);
            }
            //TODO: deal with jwtsso's customizable cookie path.
            for (Cookie cookie : logoutCookieList) {
                res.addCookie(cookie);
            }
        }
    }

    /**
     * @param req
     * @param cookies
     * @param logoutCookieList
     */
    protected void logoutJwtCookies(HttpServletRequest req, Cookie[] cookies, java.util.ArrayList<Cookie> logoutCookieList) {
        String jwtCookieName = getJwtCookieName();
        if (jwtCookieName != null) { // jwtsso is active, expire it's cookies too
            String jwtTokenStr = getJwtSsoTokenFromCookies(req, jwtCookieName);
            if (jwtTokenStr != null) {
                LoggedOutJwtSsoCookieCache.put(jwtTokenStr);
            }

            for (int i = 0; i < cookies.length; i++) {
                if (isJwtCookie(jwtCookieName, cookies[i].getName())) {
                    cookies[i].setValue(null);
                    addLogoutCookieToList(req, cookies[i].getName(), logoutCookieList);
                }
            }

        }
    }

    // jwtsso cookie names can be name, name02, 03, etc thru name99
    // see if cookiename is a jwtsso cookie based on the name.
    protected boolean isJwtCookie(String baseName, String cookieName) {
        if (baseName.equalsIgnoreCase(cookieName))
            return true;
        if (!(cookieName.startsWith(baseName))) {
            return false;
        }
        if (cookieName.length() != baseName.length() + 2) {
            return false;
        }
        String lastTwoChars = cookieName.substring(baseName.length());
        return lastTwoChars.matches("\\d\\d");
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

    @Trivial
    protected String[] splitString(String buf, int blockSize) {
        ArrayList<String> al = new ArrayList<String>();
        if (blockSize <= 0 || buf == null || buf.length() == 0) {
            return al.toArray(new String[0]);
        }
        int begin = 0;
        int end = 0;
        int length = buf.length();
        while (true) {
            end = begin + (length - end < blockSize ? length - end : blockSize);
            al.add(buf.substring(begin, end));
            if (end >= length) {
                break;
            }
            begin += (end - begin);
        }

        return al.toArray(new String[0]);
    }

    /**
     * The token can be split across multiple cookies if it is over 3900 chars.
     * Look for subsequent cookies and concatenate them in that case.
     * The counterpart for this method is SSOCookieHelperImpl.addJwtSsoCookiesToResponse.
     *
     * @param req
     * @return the token String or null if nothing found.
     */
    @Override
    public String getJwtSsoTokenFromCookies(HttpServletRequest req, String baseName) {

        StringBuffer tokenStr = new StringBuffer();
        String cookieName = baseName;
        for (int i = 1; i <= 99; i++) {
            if (i > 1) {
                cookieName = baseName + (i < 10 ? "0" : "") + i; //name02... name99
            }
            String cookieValue = getCookieValue(req, cookieName);
            if (cookieValue == null) {
                break;
            }
            if (cookieValue.length() > 0) {
                tokenStr.append(cookieValue);
            }
        }
        return tokenStr.length() > 0 ? tokenStr.toString() : null;
    }

    protected String getCookieValue(HttpServletRequest req, String cookieName) {
        String[] hdrVals = CookieHelper.getCookieValues(getCookies(req), cookieName);
        String result = null;
        if (hdrVals != null) {
            for (int n = 0; n < hdrVals.length; n++) {
                String hdrVal = hdrVals[n];
                if (hdrVal != null && hdrVal.length() > 0) {
                    result = hdrVal;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * @param req
     * @return
     */
    private Cookie[] getCookies(HttpServletRequest req) {
        return (req.getCookies());

    }

    /** {@inheritDoc} */
    /**
     * Set-Cookie: <name>=<value>[; <name>=<value>]...
     * [; expires=<date>][; domain=<domain_name>]
     * [; path=<some_path>][; secure][; httponly]
     **/
    @Override
    public void addSSOCookiesToResponse(Subject subject, HttpServletRequest req, HttpServletResponse resp) {
        if (!allowToAddCookieToResponse(req))
            return;
        addJwtSsoCookiesToResponse(subject, req, resp);

        if (!JwtSSOTokenHelper.shouldAlsoIncludeLtpaCookie()) {
            return;
        }

        if (!isDisableLtpaCookie(subject)) {
            addLtpaSsoCookiesToResponse(subject, req, resp);
        }

        if (oidcServerRef != null && oidcServerRef.getService() != null) {
            // oidc server exists, remove browser state cookie.
            if (isBrowserStateEnabled(req)) {
                removeBrowserStateCookie(req, resp);
            }
        }

    }

    private boolean isDisableLtpaCookie(Subject subject) {
        SubjectHelper subjectHelper = new SubjectHelper();
        Hashtable<String, ?> hashtable = subjectHelper.getHashtableFromSubject(subject, disableSsoLtpaCookie);
        if (hashtable != null && (Boolean) hashtable.get(AuthenticationConstants.INTERNAL_DISABLE_SSO_LTPA_COOKIE))
            return true;
        else
            return false;
    }

    /**
     * @param subject
     * @param req
     * @param resp
     */
    private void addLtpaSsoCookiesToResponse(Subject subject, HttpServletRequest req, HttpServletResponse resp) {
        SingleSignonToken ssoToken = getDefaultSSOTokenFromSubject(subject);
        if (ssoToken != null) {
            byte[] ssoTokenBytes = ssoToken.getBytes();
            if (ssoTokenBytes != null) {
                ByteArray cookieBytes = new ByteArray(ssoTokenBytes);
                String cookieByteString = cookieByteStringCache.get(cookieBytes);
                if (cookieByteString == null) {
                    cookieByteString = StringUtil.toString(Base64Coder.base64Encode(ssoTokenBytes));
                    updateCookieCache(cookieBytes, cookieByteString);
                }

                Cookie ssoCookie = createCookie(req, cookieByteString);
                resp.addCookie(ssoCookie);
            }
        }
    }

    /** {@inheritDoc} */
    /*
     * 1) If we have the custom cookie name, then delete just the custom cookie name
     * 2) If we have the custom cookie name but no cookie found, then will delete the default cookie name LTPAToken2
     * 3) If jwtsso is active, clean up those cookies too.
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
                } else if (cookies[i].getName().equalsIgnoreCase(OIDC_BROWSER_STATE_COOKIE)) {
                    // remove oidc browser state cookie, if it exists.
                    if (oidcServerRef != null && oidcServerRef.getService() != null) {
                        removeBrowserStateCookie(req, res);
                    }
                }
            }

            logoutJwtCookies(req, cookies, logoutCookieList);

            //TODO: deal with jwtsso's customizable cookie path.
            for (Cookie cookie : logoutCookieList) {
                res.addCookie(cookie);
            }
        }
    }

    /**
     * check whether BrowserState cookie exists in a http request.
     *
     * @param req
     * @return true if the cookie exists, false otherwise
     */
    protected boolean isBrowserStateEnabled(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equalsIgnoreCase(OIDC_BROWSER_STATE_COOKIE)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void removeBrowserStateCookie(HttpServletRequest req, HttpServletResponse res) {
        Cookie c = new Cookie(OIDC_BROWSER_STATE_COOKIE, "");
        c.setMaxAge(0);
        c.setPath("/");
        c.setSecure(req.isSecure());
        res.addCookie(c);
    }

}
