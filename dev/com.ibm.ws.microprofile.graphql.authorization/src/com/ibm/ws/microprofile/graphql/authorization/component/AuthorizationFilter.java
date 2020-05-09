/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.graphql.authorization.component;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;


public class AuthorizationFilter extends HttpFilter {

    private final static TraceComponent tc = Tr.register(AuthorizationFilter.class);
    
    private final static AuthorizationFilter INSTANCE = new AuthorizationFilter();

    private final static ThreadLocal<HttpServletRequest> tlRequest = new ThreadLocal<>();
    private final static ThreadLocal<HttpServletResponse> tlResponse = new ThreadLocal<>();

    static AuthorizationFilter getInstance() {
        return INSTANCE;
    }

    private AuthorizationFilter() {
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) 
            throws IOException, ServletException {
        try {
            tlRequest.set(req);
            tlResponse.set(res);
            chain.doFilter(req, res);
        } finally {
            tlRequest.remove();
            tlResponse.remove();
        }
    }

    boolean authenticate() throws IOException, ServletException {
        return getRequest().authenticate(getResponse());
    }
    Principal getUserPrincipal() {
        return getRequest().getUserPrincipal();
    }

    boolean isUserInRole(String role) {
        return getRequest().isUserInRole(role);
    }

    private HttpServletRequest getRequest() {
        HttpServletRequest req = tlRequest.get();
        if (req == null) {
            throw new IllegalStateException("Not in the context of a servlet request");
        }
        return req;
    }

    private HttpServletResponse getResponse() {
        HttpServletResponse res = tlResponse.get();
        if (res == null) {
            throw new IllegalStateException("Not in the context of a servlet request");
        }
        return res;
    }
}