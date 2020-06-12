/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;

/**
 * Servlet Filter implementation class OAuth20RequestFilter
 */
public class OAuth20RequestFilter implements Filter {
    private static TraceComponent tc = Tr.register(OAuth20RequestFilter.class);

    public static final String REGEX_COMPONENT_ID = "/([\\w-]+)/"; // first capture group, /name_of_provider/

    // Matches paths such as /registration/* to extract clientId values
    public static final String REGEX_REGISTRATION = "registration(/[\u0020-\u007E]*)?";
    public static final String PATH_PTM = "personalTokenManagement";
    public static final String PATH_UTM = "usersTokenManagement";
    public static final String PATH_CLIENTMGT = "clientManagement";

    // app-password and token endpoints
    private static final String apwuri = OAuth20Constants.APP_PASSWORD_URI; // match .../blah
    private static final String apwuri2 = apwuri + "/.*"; // match .../blah/moreblah
    private static final String atokuri = OAuth20Constants.APP_TOKEN_URI;
    private static final String atokuri2 = atokuri + "/.*";
    private static final String apwtok = apwuri + "|" + apwuri2 + "|" + atokuri + "|" + atokuri2;

    private static final Pattern PATH_RE = Pattern.compile("^" + REGEX_COMPONENT_ID +
            "(authorize|token|introspect|revoke|.well-known/openid-configuration|userinfo|logout|"
            + REGEX_REGISTRATION + "|check_session_iframe|end_session|coverage_map|proxy|jwk|"
            + apwtok + "|" + PATH_PTM + "|" + PATH_UTM + "|" + PATH_CLIENTMGT + "|" + OAuth20Constants.CLIENT_METATYPE_URI + ")$");
    // Used to map OIDC Discovery well-known path to enum type 'discovery'
    public static final String PATH_DISCOVERY = ".well-known/openid-configuration";
    public static final String PATH_REGISTRATION = "registration";
    public static final String SLASH_PATH_REGISTRATION = "/" + PATH_REGISTRATION;
    public static final String PATH_REGISTRATION_SLASH = PATH_REGISTRATION + "/";
    // map to enum types app_password, app_token

    /**
     * Default constructor.
     */
    public OAuth20RequestFilter() {
    }

    /**
     * @see Filter#destroy()
     */
    @Override
    public void destroy() {
    }

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (response.isCommitted()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "doFilter response.isCommitted() do nothing...");
            }
            // some component has already commit the http servlet response
            // do nothing here
        } else {
            Matcher matcher = endpointRequest(request);
            if (matcher != null) {
                setEndpointRequest(request, response, chain, matcher);
            } else {
                if (tc.isWarningEnabled()) {
                    Tr.warning(tc, "security.oauth20.filter.request.null", request.getPathInfo()); // CWOAU0039W
                }
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    /**
     * @param response
     * @param matcher
     * @throws ServletException
     * @throws IOException
     */
    public void setEndpointRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Matcher matcher) throws IOException, ServletException {
        OAuth20Request oauth20Request = new OAuth20Request(getProviderNameFromUrl(matcher), getEndpointTypeFromUrl(matcher), request);
        request.setAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME, oauth20Request);
        chain.doFilter(request, response);
    }

    protected String getProviderNameFromUrl(Matcher m) {
        String componentId = m.group(1);
        return componentId;
    }

    protected EndpointType getEndpointTypeFromUrl(Matcher m) {
        EndpointType type = OAuth20RequestFilter.getType(m.group(2));
        return type;
    }

    /**
     * @see Filter#init(FilterConfig)
     */
    @Override
    public void init(FilterConfig fConfig) throws ServletException {
        // do nothing
    }

    private Matcher endpointRequest(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null) {
            return null;
        }
        Matcher m = PATH_RE.matcher(path);
        if (m.matches()) {
            return m;
        }
        return null;
    }

    /**
     * Method to determine type of request based on known
     * path values (ex: authorize, token, introspect, revoke,
     * etc...)
     *
     *  Normally the endointType is exactly the same as the pathType,
     *  but there are a few that are not exact matches, for those cases this
     *  method performs the necessary translation
     */
    private static EndpointType getType(String pathType) {
        // Need to do map to type discovery because enum types don't support certain symbols

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "path type is " + pathType);
        }

        if (pathType.equals(PATH_DISCOVERY)) {
            return EndpointType.discovery;
        }

        if (pathType.startsWith(OAuth20Constants.APP_PASSWORD_URI)) {
            return EndpointType.app_password;
        }
        if (pathType.startsWith(OAuth20Constants.APP_TOKEN_URI)) {
            return EndpointType.app_token;
        }

        // Needed to handle paths such as /registration/*
        if (pathType.startsWith(PATH_REGISTRATION_SLASH)) {
            return EndpointType.registration;
        }

        // no translation needed, just return path
        return EndpointType.valueOf(pathType);
    }
}
