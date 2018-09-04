/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.web.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.web.WebUtils;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

public class SocialWebUtils {

    public static final TraceComponent tc = Tr.register(SocialWebUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    protected ReferrerURLCookieHandler getCookieHandler() {
        WebAppSecurityConfig config = getWebAppSecurityConfig();
        if (config != null) {
            return config.createReferrerURLCookieHandler();
        }
        return new ReferrerURLCookieHandler(config);
    }

    WebAppSecurityConfig getWebAppSecurityConfig() {
        return WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
    }

    public void doClientSideRedirect(HttpServletResponse response, String reqUrlCookieName, String loginURL) throws SocialLoginException {
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter pw = null;
        try {
            pw = response.getWriter();
        } catch (IOException e) {
            // Shouldn't happen, so not going to write a dedicated NLS message for this case
            throw new SocialLoginException(e);
        }
        pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        pw.println("<head>");

        pw.println(createJavaScriptForRedirect(reqUrlCookieName, loginURL));

        pw.println("<title>Redirect To OP</title> ");
        pw.println("</head>");
        pw.println("<body></body>");
        pw.println("</html>");

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private, max-age=0");
        // HTTP 1.0.
        response.setHeader("Pragma", "no-cache");
        // Proxies.
        response.setDateHeader("Expires", 0);
        response.setContentType("text/html; charset=UTF-8");

        pw.close();
    }

    protected String createJavaScriptForRedirect(String reqUrlCookieName, String loginURL) {

        StringBuilder sb = new StringBuilder();

        sb.append("<script type=\"text/javascript\" language=\"javascript\">")
                .append("var loc=window.location.href;")
                .append("document.cookie=\"").append(reqUrlCookieName).append("=\"").append("+encodeURI(loc)+").append("\"; path=/;");

        WebAppSecurityConfig webAppSecurityConfig = getWebAppSecurityConfig();
        if (webAppSecurityConfig != null) {
            if (webAppSecurityConfig.getSSORequiresSSL()) {
                sb.append(" secure;");
            }
        }
        sb.append("\"</script>");

        sb.append("<script type=\"text/javascript\" language=\"javascript\">")
                .append("window.location.replace(\"" + loginURL + "\")")
                .append("</script>");

        String js = sb.toString();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "createJavaScriptForRedirect returns [" + js + "]");
        }
        return js;
    }

    @Sensitive
    public String getAndClearCookie(HttpServletRequest request, HttpServletResponse response, String cookieName) {
        Cookie[] cookies = request.getCookies();
        String value = CookieHelper.getCookieValue(cookies, cookieName);
        CookieHelper.clearCookie(request, response, cookieName, cookies);
        return value;
    }

    public String getRequestUrlWithEncodedQueryString(HttpServletRequest req) {
        StringBuffer reqURL = req.getRequestURL();
        if (req.getQueryString() != null) {
            reqURL.append("?");
            reqURL.append(getUrlEncodedQueryString(req));
        }
        return reqURL.toString();
    }

    public String getUrlEncodedQueryString(HttpServletRequest req) {
        StringBuilder qs = new StringBuilder();
        Map<String, String[]> params = req.getParameterMap();
        if (!params.isEmpty()) {
            qs.append(getUrlEncodedQueryStringFromParameterMap(params));
        }
        return qs.toString();
    }

    public String getUrlEncodedQueryStringFromParameterMap(Map<String, String[]> params) {
        StringBuilder qs = new StringBuilder();
        Iterator<Entry<String, String[]>> iter = params.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, String[]> param = iter.next();
            qs.append(getUrlEncodedParameterAndValues(param.getKey(), param.getValue()));

            if (iter.hasNext() && qs.charAt(qs.length() - 1) != '&') {
                // Append a '&' character if there are more parameters to encode
                qs.append("&");
            }
        }
        return qs.toString();
    }

    public String getUrlEncodedParameterAndValues(String key, String[] values) {
        StringBuilder paramAndValue = new StringBuilder();
        // The parameter may or may not have values, but go ahead and add it to the string since at least the key is present
        paramAndValue.append(WebUtils.urlEncode(key));

        if (values != null && values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
                paramAndValue.append("=" + WebUtils.urlEncode(value));
                if (i < values.length - 1) {
                    // Append a '&' character if there are more parameter values to encode
                    paramAndValue.append("&" + WebUtils.urlEncode(key));
                }
            }
        }
        return paramAndValue.toString();
    }

    public String getLoginHint(HttpServletRequest request) {
        String loginHint = getLoginHintFromHeader(request);
        if (loginHint != null && !loginHint.isEmpty()) {
            return loginHint;
        }
        loginHint = getLoginHintFromCookie(request);
        if (loginHint != null && !loginHint.isEmpty()) {
            return loginHint;
        }
        loginHint = getLoginHintFromParameter(request);
        if (loginHint != null && !loginHint.isEmpty()) {
            return loginHint;
        }
        return loginHint;
    }

    String getLoginHintFromHeader(HttpServletRequest request) {
        String loginHint = request.getHeader(ClientConstants.LOGIN_HINT);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "specifiedService(header) id:" + loginHint);
        }
        return loginHint;
    }

    String getLoginHintFromCookie(HttpServletRequest request) {
        String loginHint = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ClientConstants.LOGIN_HINT.equals(cookie.getName())) {
                    loginHint = cookie.getValue();
                }
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "specifiedService(cookie) id:" + loginHint);
        }
        return loginHint;
    }

    String getLoginHintFromParameter(HttpServletRequest request) {
        String loginHint = request.getParameter(ClientConstants.LOGIN_HINT);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "specifiedService(param) id:" + loginHint);
        }
        return loginHint;
    }

    /**
     * Saves the original request URL and any POST parameters.
     */
    public void saveRequestUrlAndParameters(HttpServletRequest request, HttpServletResponse response) {
        String cookieName = ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME;
        ReferrerURLCookieHandler referrerURLCookieHandler = getCookieHandler();
        Cookie c = referrerURLCookieHandler.createCookie(cookieName, getRequestUrlWithEncodedQueryString(request), request);
        response.addCookie(c);

        savePostParameters(request, response);
    }

    void savePostParameters(HttpServletRequest request, HttpServletResponse response) {
        WebAppSecurityConfig webAppSecConfig = getWebAppSecurityConfig();
        PostParameterHelper pph = new PostParameterHelper(webAppSecConfig);
        pph.save(request, response);
    }

    /**
     * Invalidates the original request URL cookie or removes the same respective session attributes, depending on how the data
     * was saved.
     */
    public void removeRequestUrlAndParameters(HttpServletRequest request, HttpServletResponse response) {
        ReferrerURLCookieHandler referrerURLCookieHandler = getCookieHandler();
        referrerURLCookieHandler.invalidateReferrerURLCookie(request, response, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);

        WebAppSecurityConfig webAppSecConfig = getWebAppSecurityConfig();
        if (isPostDataSavedInCookie(webAppSecConfig)) {
            deleteCookie(request, response, PostParameterHelper.POSTPARAM_COOKIE, webAppSecConfig);
        } else {
            removePostParameterSessionAttributes(request);
        }
    }

    boolean isPostDataSavedInCookie(WebAppSecurityConfig webAppSecConfig) {
        return WebAppSecurityConfig.POST_PARAM_SAVE_TO_COOKIE.equals(webAppSecConfig.getPostParamSaveMethod());
    }

    /**
     * Clears the specified cookie and sets its path to the current request URI.
     */
    public void deleteCookie(HttpServletRequest request, HttpServletResponse response, String cookieName, WebAppSecurityConfig webAppSecConfig) {
        ReferrerURLCookieHandler referrerURLCookieHandler = getCookieHandler();
        referrerURLCookieHandler.clearReferrerURLCookie(request, response, cookieName);

        Cookie paramCookie = createExpiredCookie(request, cookieName, webAppSecConfig);
        response.addCookie(paramCookie);
    }

    Cookie createExpiredCookie(HttpServletRequest request, String cookieName, WebAppSecurityConfig webAppSecConfig) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setPath(request.getRequestURI());
        cookie.setMaxAge(0);

        return setCookieFlagsBasedOnWebAppConfig(cookie, webAppSecConfig);
    }

    Cookie setCookieFlagsBasedOnWebAppConfig(Cookie cookie, WebAppSecurityConfig webAppSecConfig) {
        if (webAppSecConfig.getHttpOnlyCookies()) {
            cookie.setHttpOnly(true);
        }
        if (webAppSecConfig.getSSORequiresSSL()) {
            cookie.setSecure(true);
        }
        return cookie;
    }

    void removePostParameterSessionAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(PostParameterHelper.INITIAL_URL);
            session.removeAttribute(PostParameterHelper.PARAM_VALUES);
            session.removeAttribute(PostParameterHelper.PARAM_NAMES);
        }
    }

}
