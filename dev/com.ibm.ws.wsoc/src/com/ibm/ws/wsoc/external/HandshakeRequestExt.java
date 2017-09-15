/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.external;

import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.server.HandshakeRequest;

import com.ibm.websphere.ras.annotation.Sensitive;

public class HandshakeRequestExt implements HandshakeRequest {

    private HttpServletRequest request = null;
    private Map<String, List<String>> headers = null;
    private Map<String, List<String>> parameterMap = null;
    private String queryString = null;
    private HttpSession httpSession = null;
    private Principal principal = null;
    private URI requestURI = null;

    public HandshakeRequestExt(HttpServletRequest _request,
                               Map<String, List<String>> _headers,
                               Map<String, List<String>> _parameterMap,
                               URI _requestURI) {

        headers = _headers;
        // if there are no headers, then _headers should be empty list, not null.  Following servlet spec (wsoc javadoc doesn't say)
        if (headers == null) {
            headers = new HashMap<String, List<String>>();
        }

        //as per WebSocket HandshakeRequest API, the headers should be read only
        this.headers = Collections.unmodifiableMap(headers);
        request = _request;
        //Even though webSocket Spec/API doesn't specify this to be readonly, TCK needs this to be readonly because 
        //HttpServletRequest's parameterMap is immutable.
        this.parameterMap = Collections.unmodifiableMap(_parameterMap);
        queryString = request.getQueryString();
        httpSession = request.getSession();
        principal = request.getUserPrincipal();
        requestURI = _requestURI;

    }

    @Override
    @Sensitive
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public Object getHttpSession() {
        return httpSession;
    }

    @Override
    public Map<String, List<String>> getParameterMap() {
        return parameterMap;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public URI getRequestURI() {
        return requestURI;
    }

    @Override
    @Sensitive
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return request.isUserInRole(role);
    }

}
