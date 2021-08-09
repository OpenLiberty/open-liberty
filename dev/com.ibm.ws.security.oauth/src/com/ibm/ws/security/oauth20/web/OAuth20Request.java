/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.web;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.annotation.Trivial;

public class OAuth20Request {
    @Trivial
    public static enum EndpointType {
        authorize, token, introspect, revoke, discovery, userinfo, registration, check_session_iframe, end_session, coverage_map, proxy, jwk, logout, app_password, app_token, personalTokenManagement, usersTokenManagement, clientManagement, clientMetatype;

        public static String app_password_effective_name = app_password.name().replace("_", "-") + "s";
        public static String app_token_effective_name = app_token.name().replace("_", "-") + "s";

    };

    protected OAuth20Request(String providerName, EndpointType type, HttpServletRequest request) {
        this.providerName = providerName;
        this.type = type;
        this.request = request;
    }

    protected EndpointType type;
    protected String providerName;
    protected HttpServletRequest request;

    public EndpointType getType() {
        return type;
    }

    public String getProviderName() {
        return providerName;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OAuth20Request [provider:").append(this.providerName).append(" type:")
                .append(this.type).append(" request:").append(this.request).append("]");
        return sb.toString();
    }

}
