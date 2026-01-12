/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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
package com.ibm.ws.ui.servlet.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import java.util.UUID;
import javax.servlet.http.Cookie;
/**
 *
 */
public class SessionFilter implements Filter {

    private FilterConfig filterConfig;
    private static final TraceComponent tc = Tr.register(SessionFilter.class);
    private static final String LOGIN_ERROR_PAGE = "/adminCenter/login.jsp?no_access";
    private static final String COOKIE_NAME = "csrfToken";

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
        this.filterConfig = null;
    }

    public String getCsrfTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null; // Return null if the cookie is not found
    }

    /**
     * Filters out specific requests and takes the appropriate action for each
     * 
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {

        String methodName = "doFilter";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }
        HttpServletRequest httpRequest = (HttpServletRequest) req;
        HttpServletResponse httpResponse = (HttpServletResponse) resp;

        String token = getCsrfTokenFromCookie(httpRequest);
        if(token == null){
            token = UUID.randomUUID().toString();
            Cookie cookie = new Cookie(COOKIE_NAME, token);
            cookie.setPath("/");
            cookie.setHttpOnly(false); // client-side JS needs to read it
            cookie.setSecure(httpRequest.isSecure());
            // optional: cookie.setMaxAge(...);
            httpResponse.addCookie(cookie);
        }
            

        if (req instanceof HttpServletRequest && resp instanceof HttpServletResponse) {

            HttpServletRequest httpServletReq = (HttpServletRequest) req;
            HttpServletResponse httpServletResp = (HttpServletResponse) resp;
            HttpSession session = httpServletReq.getSession(false);
            //HttpSession session = httpServletReq.getSession();
            String requestURI = httpServletReq.getRequestURI();

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, requestURI);
            }

            // We can't allow users to authenticate by navigating directly to a URL like
            // https://localhost:9443/adminCenter/j_security_check?j_username=admin&j_password=adminpwd
            // Doing so is creates a security vulnerability
            if (requestURI.equals("/adminCenter/j_security_check") && httpServletReq.getMethod().equals("GET")) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Session = " + session);
                    Tr.debug(tc, "Redirecting to " + LOGIN_ERROR_PAGE);
                }
                httpServletResp.sendRedirect(LOGIN_ERROR_PAGE);
                if (session != null) {
                    session.invalidate();
                }
            }
        }

        chain.doFilter(req, resp);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName);
        }
    }
}
