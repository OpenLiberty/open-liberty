/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.web;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.annotation.Trivial;

public class JwtRequest {
    @Trivial
    public static enum EndpointType {
        jwk
    };

    protected String jwtConfigId;
    protected EndpointType type;
    protected HttpServletRequest request;

    protected JwtRequest(String jwtConfigId, EndpointType type, HttpServletRequest request) {
        this.jwtConfigId = jwtConfigId;
        this.type = type;
        this.request = request;
    }

    public EndpointType getType() {
        return type;
    }

    public String getJwtConfigId() {
        return jwtConfigId;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JwtRequest [jwtConfigId:").append(jwtConfigId).append(" type:").append(type).append(" request:").append(request).append("]");
        return sb.toString();
    }
}
