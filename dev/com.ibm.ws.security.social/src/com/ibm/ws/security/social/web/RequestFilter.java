/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.social.web;

import java.io.IOException;
import java.util.regex.Matcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.internal.utils.RequestUtil;
import com.ibm.ws.security.social.internal.utils.SocialLoginRequest;

/**
 * Servlet Filter implementation class RequestFilter
 */
public class RequestFilter implements Filter {
    private static TraceComponent tc = Tr.register(RequestFilter.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);
    public static final String REDIRECT = "/redirect/";
    public static final int REDIRECT_LEN = REDIRECT.length();
    public static final String WELLKNOWN_CONFIG = "/.well-known/configuration";
    public static final String LOGOUT = "/logout";
    public static final String UNKNOWN = "UNKNOWN";

    /**
     * Default constructor.
     */
    public RequestFilter() {
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

        SocialLoginRequest socialLoginRequest = null;
        String pathInfo = request.getPathInfo();
        if (pathInfo.startsWith(REDIRECT)) {
            String provider = pathInfo.substring(REDIRECT_LEN);
            SocialLoginConfig socialLoginConfig = RequestUtil.getSocialLoginConfig(provider);
            if (socialLoginConfig != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "doFilter redirect providerId:" + socialLoginConfig.getUniqueId());
                }
                socialLoginRequest = new SocialLoginRequest(socialLoginConfig, REDIRECT, request, response);
            } else {
                // note that if we could not get a socialLoginConfig, we will try to dig the error out when we handle the redirect.
                // in EndpointServices.
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to find a social login configuration for ID [" + provider + "]");
                }
                socialLoginRequest = new SocialLoginRequest(REDIRECT, request, response);
            }
        } else if (pathInfo.startsWith(WELLKNOWN_CONFIG)) {
            socialLoginRequest = new SocialLoginRequest(WELLKNOWN_CONFIG, request, response);
        } else if (pathInfo.startsWith(LOGOUT)) {
            socialLoginRequest = new SocialLoginRequest(LOGOUT, request, response);
        } else {
            socialLoginRequest = new SocialLoginRequest(UNKNOWN, request, response);
        }
        request.setAttribute(Constants.ATTRIBUTE_SOCIALMEDIA_REQUEST, socialLoginRequest);
        chain.doFilter(request, response);
    }

    protected String getProviderNameFromUrl(Matcher m) {
        String componentId = m.group(1);
        return componentId;
    }

    /**
     * @see Filter#init(FilterConfig)
     */
    @Override
    public void init(FilterConfig fConfig) throws ServletException {
        // do nothing
    }

}
