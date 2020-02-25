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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.webcontainer.security.internal.URLHandler;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;

/**
 * Contains all WASReqURL Cookie related functions required by WAS.security.
 * Unless a method is explicitly needed by a class outside of this package,
 * methods should be left as default scope.
 */
public class ReferrerURLCookieHandler extends URLHandler {
    private static final TraceComponent tc = Tr.register(ReferrerURLCookieHandler.class);
    public static final String REFERRER_URL_COOKIENAME = "WASReqURL";
    public static final String CUSTOM_RELOGIN_URL_COOKIENAME = "WASReLoginURL";

    public ReferrerURLCookieHandler(WebAppSecurityConfig webAppSecConfig) {
        super(webAppSecConfig);
    }

    /**
     * Retrieve the referrer URL from the HttpServletRequest's cookies.
     * This will decode the URL and restore the host name if it was removed.
     *
     * @param req
     * @return referrerURL
     */
    @Sensitive
    public String getReferrerURLFromCookies(HttpServletRequest req, String cookieName) {
        Cookie[] cookies = req.getCookies();
        String referrerURL = CookieHelper.getCookieValue(cookies, cookieName);
        if (referrerURL != null) {
            StringBuffer URL = req.getRequestURL();
            referrerURL = decodeURL(referrerURL);
            referrerURL = restoreHostNameToURL(referrerURL, URL.toString());
        }
        return referrerURL;
    }

    /**
     * Create the referrer URL cookie.
     * This cookie should be session length (age == -1).
     *
     * @param authResult
     * @param url
     */
    public Cookie createReferrerURLCookie(String cookieName, @Sensitive String url, HttpServletRequest req) {
        if (!webAppSecConfig.getPreserveFullyQualifiedReferrerUrl())
            url = removeHostNameFromURL(url);

        return createCookie(cookieName, url, req);
    }

    /**
     * @param cookieName
     * @param value
     * @return
     */
    public Cookie createCookie(String cookieName, @Sensitive String value, HttpServletRequest req) {
        value = encodeURL(value);
        return createCookie(cookieName, value, true, req);
    }

    /**
     * @param cookieName
     * @param value
     * @return
     */
    public Cookie createCookie(String cookieName, @Sensitive String value, boolean enableHttpOnly, HttpServletRequest req) {
        Cookie c = new Cookie(cookieName, value);
        if (cookieName.equals(REFERRER_URL_COOKIENAME) || cookieName.startsWith("WASOidcStateKey")) {
            c.setPath(getPathName(req));
        } else {
            c.setPath("/");
        }
        c.setMaxAge(-1);
        if (enableHttpOnly && webAppSecConfig.getHttpOnlyCookies()) {
            c.setHttpOnly(true);
        }
        if (webAppSecConfig.getSSORequiresSSL()) {
            c.setSecure(true);
        }

        String sameSite = webAppSecConfig.getSameSiteCookie();
        if (sameSite != null && !sameSite.equals("Disabled")) {
            WebContainerRequestState requestState = WebContainerRequestState.getInstance(true);
            requestState.setCookieAttributes(cookieName, "SameSite=" + sameSite);

            if (sameSite.equals("None")) {
                c.setSecure(true);
            }
        }

        return c;
    }

    public void invalidateReferrerURLCookies(HttpServletRequest req, HttpServletResponse res, String[] cookieNames) {
        for (String cookieName : cookieNames) {
            invalidateReferrerURLCookie(req, res, cookieName);
        }
    }

    /**
     * Invalidate (clear) the referrer URL cookie in the HttpServletResponse.
     * Setting age to 0 invalidates it.
     *
     * @param res
     */
    public void invalidateReferrerURLCookie(HttpServletRequest req, HttpServletResponse res, String cookieName) {
        invalidateCookie(req, res, cookieName, true);
    }

    /**
     * Invalidate (clear) the referrer URL cookie in the HttpServletResponse.
     * Setting age to 0 invalidates it.
     *
     * @param res
     */
    public void invalidateCookie(HttpServletRequest req, HttpServletResponse res, String cookieName, boolean enableHttpOnly) {
        Cookie c = new Cookie(cookieName, "");
        if (cookieName.equals(REFERRER_URL_COOKIENAME)) {
            c.setPath(getPathName(req));
        } else {
            c.setPath("/");
        }
        c.setMaxAge(0);
        if (enableHttpOnly && webAppSecConfig.getHttpOnlyCookies()) {
            c.setHttpOnly(true);
        }
        if (webAppSecConfig.getSSORequiresSSL()) {
            c.setSecure(true);
        }
        res.addCookie(c);
    }

    /**
     * Removes the referrer URL cookie from the HttpServletResponse if set in the
     * HttpServletRequest.
     *
     * @param req
     * @param res
     */
    public void clearReferrerURLCookie(HttpServletRequest req, HttpServletResponse res, String cookieName) {

        String url = CookieHelper.getCookieValue(req.getCookies(), cookieName);
        if (url != null && url.length() > 0) {
            invalidateReferrerURLCookie(req, res, cookieName);
        }
    }

    /**
     *
     * @param req
     * @param res
     * @param securityConfig
     * @return
     */
    public AuthenticationResult shouldRedirectToReferrerURL(HttpServletRequest req,
                                                            HttpServletResponse res, String cookieName) {
        AuthenticationResult result = null;
        //TODO ALWAYS_REDIRECT_TO_REFERRER_URL
        boolean restore = false;
        if (!restore) {
            return result;
        }

        String WasReqURLValue = getReferrerURLFromCookies(req, cookieName);
        if (WasReqURLValue != null && WasReqURLValue.trim().length() > 0) {
            StringBuffer reqURL = req.getRequestURL();
            if (req.getQueryString() != null) {
                reqURL.append("?");
                reqURL.append(req.getQueryString());
            }
            String currentURL = reqURL.toString();
            String currentURI = getServletURI(req);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "\nCurrentURL: " + currentURL + "\nCurrentURI: " + currentURI + "\nWasReqURL: " + WasReqURLValue);
            }
            // If URI are identical but not URL (which means some mismatch in parameter, then check custom property.
            if (currentURL != null && currentURI != null
                && (WasReqURLValue.toLowerCase().indexOf(currentURI.toLowerCase()) > 0)
                && !WasReqURLValue.equalsIgnoreCase(currentURL)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Redirect the request to the original URL: " + WasReqURLValue);
                }
                result = new AuthenticationResult(AuthResult.REDIRECT, WasReqURLValue);
                invalidateReferrerURLCookie(req, res, cookieName);
            }
        }
        return result;
    }

    public String getPathName(HttpServletRequest req) {
        String pathName = "/";
        if (webAppSecConfig.isIncludePathInWASReqURL()) {
            pathName = req.getContextPath();
            if (pathName == null || pathName.isEmpty()) {
                pathName = "/";
            }
        }
        return pathName;
    }

    /**
     * Sets the referrer URL cookie into the AuthenticationResult. If
     * PRESERVE_FULLY_QUALIFIED_REFERRER_URL is not set, or set to false,
     * then the host name of the referrer URL is removed.
     *
     * @param authResult AuthenticationResult instance
     * @param url non-null URL String
     * @param securityConfig SecurityConfig instance
     */
    public void setReferrerURLCookie(HttpServletRequest req, AuthenticationResult authResult, String url) {
        //PM81345: If the URL contains favicon, then we will not update the value of the ReffererURL. The only way
        //we will do it, is if the value of the cookie is null. This will solve the Error 500.
        if (url.contains("/favicon.ico") && CookieHelper.getCookieValue(req.getCookies(), REFERRER_URL_COOKIENAME) != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Will not update the WASReqURL cookie");
        } else {
            if (!webAppSecConfig.getPreserveFullyQualifiedReferrerUrl()) {
                url = removeHostNameFromURL(url);
            }
            url = encodeURL(url);
            authResult.setCookie(createReferrerUrlCookie(req, url));
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "set " + REFERRER_URL_COOKIENAME + " cookie into AuthenticationResult.");
                Tr.debug(tc, "setReferrerURLCookie", "Referrer URL cookie set " + url);
            }
        }
    }

    private Cookie createReferrerUrlCookie(HttpServletRequest req, final String url) {
        Cookie referrerUrlCookie;
        if (System.getSecurityManager() == null) {
            referrerUrlCookie = new Cookie(REFERRER_URL_COOKIENAME, url);
        } else {
            referrerUrlCookie = AccessController.doPrivileged(new PrivilegedAction<Cookie>() {

                @Override
                public Cookie run() {
                    return new Cookie(REFERRER_URL_COOKIENAME, url);
                }
            });
        }

        referrerUrlCookie.setPath(getPathName(req));
        referrerUrlCookie.setMaxAge(-1);
        referrerUrlCookie.setSecure(webAppSecConfig.getSSORequiresSSL());
        return referrerUrlCookie;
    }

    /*
     * Goals:
     * 1: MalformedException: indicate which URL failed in the exception text.
     * 2: RuntimeException: If this was a MalformedException, RuntimeException should include the MalformedException.
     */
    public static void isReferrerHostValid(final String currentReqURL, final String storedReq, final List<String> domainList) throws RuntimeException {
        Boolean isValid = false;
        isValid = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                Boolean _isValid = false;
                URL referrerURL = null;
                URL currentURL = null;
                try {
                    referrerURL = new URL(storedReq);
                    currentURL = new URL(currentReqURL);
                    if (referrerURL != null && currentURL != null) {
                        String referrerHost = referrerURL.getHost();
                        String currentReqHost = currentURL.getHost();
                        if (referrerHost != null && currentReqHost != null
                            && (referrerHost.equalsIgnoreCase(currentReqHost) || isReferrerHostMatchDomainNameList(referrerHost, domainList))) {
                            _isValid = true;
                        }
                    }
                } catch (MalformedURLException me) {

                    //if referrerURL==null then storedReq is not a valid URL. Otherwise, currentURL is invalid
                    if (referrerURL == null) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "WASReqURL:" + storedReq + " is a MalformedURL.");
                        }
                        RuntimeException e = new RuntimeException("WASReqURL:" + "'" + storedReq + "'" + " is not a valid URL.", me);
                        throw e;
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "currentURL:" + currentReqURL + " is a MalformedURL.");
                        }
                        RuntimeException e = new RuntimeException("The request URL:" + "'" + currentReqURL + "'"
                                                                  + " is not a valid URL.", me);
                        throw e;
                    }
                }
                return _isValid;
            }
        });
        if (!isValid) {
            RuntimeException e = new RuntimeException("WASReqURL:" + "'" + storedReq + "'"
                                                      + " hostname does not match current request hostname: " + "'" + currentReqURL
                                                      + "'");
            throw e;
        }
        return;
    }

    static boolean isReferrerHostMatchDomainNameList(String referrerHost, List<String> _wasReqURLRedirectDomainNames) {
        boolean acceptURL = false;
        if (_wasReqURLRedirectDomainNames != null && !_wasReqURLRedirectDomainNames.isEmpty()) {
            for (Iterator<String> itr = _wasReqURLRedirectDomainNames.iterator(); itr.hasNext();) {
                String domain = itr.next();
                if (referrerHost.endsWith(domain)) {
                    acceptURL = true;
                    break;
                }
            }
        }
        return acceptURL;
    }

}
