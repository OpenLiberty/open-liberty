/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.internal;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.oauth20.AuthnContext;

/**
 *
 */
public class AuthnContextImpl implements AuthnContext {
    private final String accessToken;
    private String[] grantedScopes;
    private final long createdAt;
    private final long expiresIn;
    private final String userName;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Map<String, String[]> properties;

    /**
     * @param accessToken
     * @param grantedScopes
     * @param createdAt
     * @param expiresIn
     * @param userName
     * @param request
     * @param response
     * @param properties
     */
    public AuthnContextImpl(HttpServletRequest request, HttpServletResponse response, String accessToken, String[] grantedScopes, long createdAt, long expiresIn, String userName,
            Map<String, String[]> properties) {
        super();
        this.accessToken = accessToken;
        this.grantedScopes = Arrays.copyOf(grantedScopes, grantedScopes.length);
        ;
        this.createdAt = createdAt;
        this.expiresIn = expiresIn;
        this.userName = userName;
        this.request = request;
        this.response = response;
        this.properties = properties;
    }

    /**
     * @return the accessToken
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * @return the grantedScopes
     */
    public String[] getGrantedScopes() {
        if (grantedScopes == null) {
            return null;
        }
        return grantedScopes.clone();
    }

    /**
     * @return the createdAt
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * @return the expiresIn
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return the request
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * @return the response
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * @return the properties
     */
    public Map<String, String[]> getProperties() {
        return properties;
    }

}
